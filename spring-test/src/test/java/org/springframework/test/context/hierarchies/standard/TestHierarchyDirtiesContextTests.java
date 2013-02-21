/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context.hierarchies.standard;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.junit4.JUnitCoreUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Test to verify {@link DirtiesContext} for hierarchy test contexts.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2.2
 */
public class TestHierarchyDirtiesContextTests {

	private static ApplicationContext testAppContext;

	@After
	public void cleanUp() {
		testAppContext = null;
	}

	@Test
	public void testClassLevelDirtiesContextWithSharedContext() {
		runAndVerifyHierarchyContexts(ClassLevelDirtiesContextWithSharedContextTestCase.class, true, false, false);
	}

	@Test
	public void testClassLevelDirtiesContext() {
		runAndVerifyHierarchyContexts(ClassLevelDirtiesContextTestCase.class, true, true, false);
	}

	@Test
	public void testMethodLevelDirtiesContextWithSharedContext() {
		runAndVerifyHierarchyContexts(MethodLevelDirtiesContextWithSharedContextTestCase.class, true, false, false);
	}

	@Test
	public void testMethodLevelDirtiesContextTest() {
		runAndVerifyHierarchyContexts(MethodLevelDirtiesContextTestCase.class, true, true, false);
	}

	private void runAndVerifyHierarchyContexts(Class<? extends BaseTestCase> testClass, boolean isFooContextActive,
	                                           boolean isBarContextActive, boolean isBazContextActive) {

		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(testClass);
		JUnitCoreUtils.verifyResult(result);

		assertThat(testAppContext, notNullValue());

		ConfigurableApplicationContext bazContext = (ConfigurableApplicationContext) testAppContext;
		assertThat(bazContext.getDisplayName(), is("bazContext"));
		assertThat("bazContext#isActive()", bazContext.isActive(), is(isBazContextActive));

		ConfigurableApplicationContext barContext = (ConfigurableApplicationContext) bazContext.getParent();
		assertThat(barContext, notNullValue());
		assertThat(barContext.getDisplayName(), is("barContext"));
		assertThat("barContext#isActive()", barContext.isActive(), is(isBarContextActive));

		ConfigurableApplicationContext fooContext = (ConfigurableApplicationContext) barContext.getParent();
		assertThat(fooContext, notNullValue());
		assertThat(fooContext.getDisplayName(), is("fooContext"));
		assertThat("fooContext#isActive()", fooContext.isActive(), is(isFooContextActive));
	}

	@Test
	public void testClassLevelDirtiesContextWithNonExistingContextName() {
		runAndVerifyRuntimeException(ClassLevelDirtiesContextWithNonExistingContextNameTestCase.class);
	}

	@Test
	public void testMethodLevelDirtiesContextWithNonExistingContextName() {
		runAndVerifyRuntimeException(MethodLevelDirtiesContextWithNonExistingContextNameTestCase.class);
	}

	private void runAndVerifyRuntimeException(Class<? extends BaseTestCase> testClass) {
		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(testClass);

		// expects runtime exception to be thrown
		assertThat(result.getFailureCount(), is(1));
		List<Failure> failures = result.getFailures();
		Throwable exception = failures.get(0).getException();
		assertThat(exception, instanceOf(RuntimeException.class));
	}


	@RunWith(SpringJUnit4ClassRunner.class)
	@ContextHierarchy({
			@ContextConfiguration(name = "fooContext", classes = {Config.class}, loader = NameAwareContextLoader.class),
			@ContextConfiguration(name = "barContext", classes = {Config.class}, loader = NameAwareContextLoader.class),
			@ContextConfiguration(name = "bazContext", classes = {Config.class}, loader = NameAwareContextLoader.class)
	})
	public static abstract class BaseTestCase {
		@Autowired
		protected ApplicationContext applicationContext;
	}

	/**
	 * {@link DirtiesContext} is set at class level specifying shared context name.
	 * After running this test class, specified context and its child contexts should be closed.
	 */
	@DirtiesContext("barContext")
	public static class ClassLevelDirtiesContextWithSharedContextTestCase extends BaseTestCase {
		@Test
		public void testClassLevel() {
			testAppContext = applicationContext;
		}
	}

	/**
	 * {@link DirtiesContext} is set at class level but no context name is specified.
	 * After running this test class, only the test context, no parent contexts, should be closed.
	 */
	@DirtiesContext
	public static class ClassLevelDirtiesContextTestCase extends BaseTestCase {
		@Test
		public void testClassLevel() {
			testAppContext = applicationContext;
		}
	}

	/**
	 * {@link DirtiesContext} is set at class level specifying shared context name.
	 * After running this test class, {@link RuntimeException} should be thrown indicating the given context name
	 * doesn't exist in context hierarchy.
	 */
	@DirtiesContext("nonAvailableContext")
	public static class ClassLevelDirtiesContextWithNonExistingContextNameTestCase extends BaseTestCase {
		@Test
		public void testClassLevel() {
			testAppContext = applicationContext;
		}
	}

	/**
	 * {@link DirtiesContext} is set at method level specifying shared context name.
	 * After running this test class, specified context and its child contexts should be closed.
	 */
	public static class MethodLevelDirtiesContextWithSharedContextTestCase extends BaseTestCase {
		@Test
		@DirtiesContext("barContext")
		public void testMethodLevel() {
			testAppContext = applicationContext;
		}
	}

	/**
	 * {@link DirtiesContext} is set at method level but no context name is specified.
	 * After running this test class, only the test context, no parent contexts, should be closed.
	 */
	public static class MethodLevelDirtiesContextTestCase extends BaseTestCase {
		@Test
		@DirtiesContext
		public void testMethodLevel() {
			testAppContext = applicationContext;
		}
	}

	/**
	 * {@link DirtiesContext} is set at method level specifying shared context name.
	 * After running this test class, {@link RuntimeException} should be thrown indicating the given context name
	 * doesn't exist in context hierarchy.
	 */

	public static class MethodLevelDirtiesContextWithNonExistingContextNameTestCase extends BaseTestCase {
		@Test
		@DirtiesContext("nonAvailableContext")
		public void testMethodLevel() {
			testAppContext = applicationContext;
		}
	}

	@Configuration
	public static class Config {
		// no beans
	}

	/**
	 * {@link org.springframework.test.context.SmartContextLoader} to set {@link ContextConfiguration#name()} as
	 * {@link org.springframework.context.ApplicationContext#getApplicationName()}
	 */
	public static class NameAwareContextLoader extends AnnotationConfigContextLoader {
		@Override
		protected void prepareContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			super.prepareContext(context, mergedConfig);
			((AbstractApplicationContext) context).setDisplayName(mergedConfig.getName());
		}
	}
}

