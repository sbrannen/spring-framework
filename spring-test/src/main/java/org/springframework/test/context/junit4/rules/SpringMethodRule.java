/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assume.*;

/**
 * {@code SpringMethodRule} is a custom JUnit {@link MethodRule} that, in
 * conjunction with the {@link SpringClassRule}, provides functionality of
 * the <em>Spring TestContext Framework</em> to standard JUnit tests by
 * means of the {@link TestContextManager} and associated support classes
 * and annotations.
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see SpringClassRule
 */
public class SpringMethodRule implements MethodRule {

	/**
	 * {@link SpringMethodRule} retains a reference to the {@code SpringClassRule}
	 * instead of the {@code TestContextManager}, since the class rule <em>owns</em>
	 * the {@code TestContextManager} and can therefore release it when it's
	 * no longer needed.
	 */
	private final SpringClassRule springClassRule;


	/**
	 * Construct a new {@code SpringMethodRule} for the supplied test instance.
	 *
	 * <p>The test class must declare a {@code public static final SpringClassRule}
	 * field (i.e., a <em>constant</em>) that is annotated with JUnit's
	 * {@link ClassRule @ClassRule} &mdash; for example:
	 *
	 * <pre><code>
	 *   &#064;ClassRule
	 *   public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
	 * </code></pre>
	 *
	 * @param testInstance the test instance, never {@code null}
	 * @throws IllegalStateException if the test class does not declare an
	 * appropriate {@code SpringClassRule} constant.
	 */
	public SpringMethodRule(Object testInstance) {
		Assert.notNull(testInstance, "testInstance must not be null");
		Class<?> testClass = testInstance.getClass();
		SpringClassRule springClassRule = null;
		Field springClassRuleField = null;

		for (Field field : testClass.getDeclaredFields()) {
			if (ReflectionUtils.isPublicStaticFinal(field) && (SpringClassRule.class.isAssignableFrom(field.getType()))) {
				springClassRule = (SpringClassRule) ReflectionUtils.getField(field, null);
				springClassRuleField = field;
				break;
			}
		}

		if (springClassRule == null) {
			throw new IllegalStateException(String.format(
				"Failed to find 'public static final SpringClassRule' field in test class [%s].", testClass.getName()));
		}
		if (!springClassRuleField.isAnnotationPresent(ClassRule.class)) {
			throw new IllegalStateException(String.format(
				"SpringClassRule field [%s] must be annotated with JUnit's @ClassRule annotation.",
				springClassRuleField));
		}

		this.springClassRule = springClassRule;
	}

	/**
	 * Prepare the test instance and run before and after test methods on
	 * the {@link TestContextManager}.
	 *
	 * <p>In addition, this method checks whether the test is enabled in
	 * the current execution environment. This prevents methods with a
	 * non-matching {@code @IfProfileValue} annotation from running altogether,
	 * even skipping the execution of {@code prepareTestInstance()} methods
	 * in {@code TestExecutionListeners}.
	 *
	 * @see TestContextManager#prepareTestInstance(Object)
	 * @see TestContextManager#beforeTestMethod(Object, Method)
	 * @see TestContextManager#afterTestMethod(Object, Method, Throwable)
	 */
	@Override
	public Statement apply(final Statement base, final FrameworkMethod method, final Object testInstance) {
		final Method testMethod = method.getMethod();

		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				boolean enabled = ProfileValueUtils.isTestEnabledInThisEnvironment(testMethod, testInstance.getClass());
				assumeTrue(String.format(
					"Profile configured via @IfProfileValue is not enabled in this environment for test method [%s].",
					testMethod), enabled);

				TestContextManager testContextManager = SpringMethodRule.this.springClassRule.getTestContextManager();

				testContextManager.prepareTestInstance(testInstance);
				testContextManager.beforeTestMethod(testInstance, testMethod);

				// TODO Apply RunAfterTestMethodCallbacks or incorporate logic here.

				Throwable testException = null;
				List<Throwable> errors = new ArrayList<Throwable>();
				try {
					base.evaluate();
				}
				catch (Throwable e) {
					testException = e;
					errors.add(e);
				}

				try {
					testContextManager.afterTestMethod(testInstance, testMethod, testException);
				}
				catch (Exception e) {
					errors.add(e);
				}

				if (errors.isEmpty()) {
					return;
				}
				if (errors.size() == 1) {
					throw errors.get(0);
				}
				throw new MultipleFailureException(errors);
			}
		};
	}

}
