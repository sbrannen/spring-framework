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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Test to verify when {@link org.springframework.test.annotation.DirtiesContext} marks shared parent context dirty.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2.2
 */
public class TestHierarchyDirtiesContextWithSharedParentsTests {

	private static Map<Class<? extends BaseTestCase>, ApplicationContext> map =
			new HashMap<Class<? extends BaseTestCase>, ApplicationContext>();

	@After
	public void cleanUp() {
		map.clear(); // release hard references to the app context
	}

	@Test
	public void testDirtySharedContext() {

		JUnitCore jUnitCore = new JUnitCore();

		// just run one test which doesn't dirty the contexts
		Result result = jUnitCore.run(SharedContextFirstTestCase.class);

		JUnitCoreUtils.verifyResult(result);
		assertThat(map.keySet(), hasItem(SharedContextFirstTestCase.class));
		assertThat(map.size(), is(1));
		ConfigurableApplicationContext onlyContext = (ConfigurableApplicationContext) map.get(SharedContextFirstTestCase.class);
		verifyAppContext(onlyContext, true, true, true);  // no context should be closed

		map.clear();  // reset

		// run multiple tests that share the context. second test dirties one of the shared contexts
		// expects test class runs in this order...
		result = jUnitCore.run(SharedContextFirstTestCase.class, SharedContextSecondTestCase.class, SharedContextThirdTestCase.class);
		JUnitCoreUtils.verifyResult(result);

		assertThat(map.keySet(), hasItem(SharedContextFirstTestCase.class));
		assertThat(map.keySet(), hasItem(SharedContextSecondTestCase.class));
		assertThat(map.keySet(), hasItem(SharedContextThirdTestCase.class));
		assertThat(map.size(), is(3));

		ConfigurableApplicationContext firstContext = (ConfigurableApplicationContext) map.get(SharedContextFirstTestCase.class);
		ConfigurableApplicationContext secondContext = (ConfigurableApplicationContext) map.get(SharedContextSecondTestCase.class);
		ConfigurableApplicationContext thirdContext = (ConfigurableApplicationContext) map.get(SharedContextThirdTestCase.class);

		// first and second shares same context
		assertThat(firstContext, is(sameInstance(secondContext)));
		verifyAppContext(firstContext, true, false, false);
		verifyAppContext(secondContext, true, false, false);

		// second test marked shared context dirty, when third test runs, it creates a new context.
		assertThat(thirdContext, is(not(sameInstance(secondContext))));
		verifyAppContext(thirdContext, true, true, true);
	}

	private void verifyAppContext(ConfigurableApplicationContext context, boolean isFooActive, boolean isBarActive, boolean isBazActive) {

		assertThat(context.getDisplayName(), is("bazContext"));
		assertThat("bazContext#isActive()", context.isActive(), is(isBazActive));

		ConfigurableApplicationContext barContext = (ConfigurableApplicationContext) context.getParent();
		assertThat(barContext, notNullValue());
		assertThat(barContext.getDisplayName(), is("barContext"));
		assertThat("barContext#isActive()", barContext.isActive(), is(isBarActive));

		ConfigurableApplicationContext fooContext = (ConfigurableApplicationContext) barContext.getParent();
		assertThat(fooContext, notNullValue());
		assertThat(fooContext.getDisplayName(), is("fooContext"));
		assertThat("fooContext#isActive()", fooContext.isActive(), is(isFooActive));
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
	 * share the same parent contexts.
	 */
	public static class SharedContextFirstTestCase extends BaseTestCase {
		@Test
		public void testFoo() {
			map.put(getClass(), applicationContext);
		}
	}

	/**
	 * share the same parent contexts and dirties barContext.
	 */
	public static class SharedContextSecondTestCase extends BaseTestCase {
		@Test
		@DirtiesContext("barContext")
		public void testBar() {
			map.put(getClass(), applicationContext);
		}
	}

	/**
	 * share the same parent contexts.
	 */
	public static class SharedContextThirdTestCase extends BaseTestCase {
		@Test
		public void testBaz() {
			map.put(getClass(), applicationContext);
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
