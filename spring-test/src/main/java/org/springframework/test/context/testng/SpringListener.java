/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.testng;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.testng.IClassListener;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import org.springframework.test.context.TestContextManager;
import org.springframework.util.Assert;

/**
 * TestNG listener for the Spring TestContext Framework.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see org.testng.annotations.Listeners @Listeners
 * @see org.springframework.test.context.ContextConfiguration @ContextConfiguration
 * @see org.springframework.test.context.TestExecutionListeners @TestExecutionListeners
 */
public class SpringListener implements IClassListener, IInvokedMethodListener, IHookable {

	/**
	 * Cache of {@code TestContextManagers} keyed by test class.
	 */
	private static final Map<Class<?>, TestContextManager> testContextManagerCache = new ConcurrentHashMap<>(64);


	@Override
	public void onBeforeClass(ITestClass iTestClass) {
		Class<?> testClass = iTestClass.getRealClass();
		System.err.println("---------------------------------------------------------------");
		System.err.println(">>> Before Class: " + testClass.getSimpleName());

		Object testInstance = Arrays.stream(iTestClass.getTestMethods())
				.map(ITestNGMethod::getInstance)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Failed to find test instance for " + testClass.getName()));
		try {
			TestContextManager testContextManager = getTestContextManager(testClass);
			testContextManager.beforeTestClass();
			testContextManager.prepareTestInstance(testInstance);
		}
		catch (Exception ex) {
			throwAsUncheckedException(ex);
		}
	}

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
		try {
			// if ((method.isConfigurationMethod() && method.getTestMethod().isBeforeMethodConfiguration())) {
			if (method.isTestMethod()) {
				Class<?> testClass = testResult.getTestClass().getRealClass();
				Object testInstance = testResult.getInstance();
				Method testMethod = testResult.getMethod().getConstructorOrMethod().getMethod();
				System.err.println("\t>>> Before Method: " + testMethod.getName());
				getTestContextManager(testClass).beforeTestMethod(testInstance, testMethod);
			}
		}
		catch (Exception ex) {
			throwAsUncheckedException(ex);
		}
	}

	/**
	 * Delegates to the {@linkplain IHookCallBack#runTestMethod(ITestResult)
	 * test method} in the supplied {@code callback} to execute the actual test
	 * and then tracks the exception thrown during test execution, if any.
	 * @see org.testng.IHookable#run(IHookCallBack, ITestResult)
	 */
	@Override
	public void run(IHookCallBack callBack, ITestResult testResult) {
		Class<?> testClass = testResult.getTestClass().getRealClass();
		Object testInstance = testResult.getInstance();
		Method testMethod = testResult.getMethod().getConstructorOrMethod().getMethod();
		TestContextManager testContextManager = getTestContextManager(testClass);
		boolean beforeCallbacksExecuted = false;
		Throwable currentException = null;

		try {
			System.err.println("\t\t>>> Before Execution: " + testMethod.getName());
			testContextManager.beforeTestExecution(testInstance, testMethod);
			beforeCallbacksExecuted = true;
		}
		catch (Throwable ex) {
			currentException = ex;
		}

		if (beforeCallbacksExecuted) {
			System.err.println("\t\t\t>>> Execution: " + testMethod.getName());
			callBack.runTestMethod(testResult);
			currentException = getTestResultException(testResult);
		}

		try {
			System.err.println("\t\t>>> After Execution:  " + testMethod.getName());
			testContextManager.afterTestExecution(testInstance, testMethod, currentException);
		}
		catch (Throwable ex) {
			if (currentException == null) {
				currentException = ex;
			}
			else {
				currentException.addSuppressed(ex);
			}
		}

		if (currentException != null) {
			throwAsUncheckedException(currentException);
		}
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
		try {
			if (method.isTestMethod()) {
				Class<?> testClass = testResult.getTestClass().getRealClass();
				Object testInstance = testResult.getInstance();
				Method testMethod = method.getTestMethod().getConstructorOrMethod().getMethod();
				Throwable exception = testResult.getThrowable();
				System.err.println("\t>>> After Method:  " + testMethod.getName());
				getTestContextManager(testClass).afterTestMethod(testInstance, testMethod, exception);
			}
		}
		catch (Exception ex) {
			throwAsUncheckedException(ex);
		}
	}

	@Override
	public void onAfterClass(ITestClass iTestClass) {
		Class<?> testClass = iTestClass.getRealClass();
		System.err.println(">>> After Class: " + testClass.getSimpleName());
		try {
			getTestContextManager(testClass).afterTestClass();
		}
		catch (Exception ex) {
			throwAsUncheckedException(ex);
		}
		finally {
			testContextManagerCache.remove(testClass);
		}
	}


	/**
	 * Get the {@link TestContextManager} associated with the supplied test class.
	 * @param testClass the test class to be managed; never {@code null}
	 */
	private static TestContextManager getTestContextManager(Class<?> testClass) {
		Assert.notNull(testClass, "Test Class must not be null");
		return testContextManagerCache.computeIfAbsent(testClass, TestContextManager::new);
	}

	private static @Nullable Throwable getTestResultException(ITestResult testResult) {
		Throwable testResultException = testResult.getThrowable();
		if (testResultException instanceof InvocationTargetException) {
			testResultException = testResultException.getCause();
		}
		return testResultException;
	}

	private static RuntimeException throwAsUncheckedException(Throwable t) {
		throwAs(t);
		// Appeasing the compiler: the following line will never be executed.
		throw new IllegalStateException(t);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void throwAs(Throwable t) throws T {
		throw (T) t;
	}

}
