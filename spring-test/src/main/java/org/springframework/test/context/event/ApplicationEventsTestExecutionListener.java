/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.event;

import java.io.Serializable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.Assert;

/**
 * {@code TestExecutionListener} which provides support for {@link ApplicationEvents}.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
public class ApplicationEventsTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Returns {@code 1800}.
	 */
	@Override
	public final int getOrder() {
		return 1800;
	}

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		if (recordingApplicationEvents(testContext)) {
			registerListenerAndResolvableDependency(testContext);
			ApplicationEventsHolder.registerApplicationEvents();
		}
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (recordingApplicationEvents(testContext)) {
			// Register a new ApplicationEvents instance for the current thread
			// in case the test instance is shared -- for example, in TestNG or
			// JUnit Jupiter with @TestInstance(PER_CLASS) semantics.
			ApplicationEventsHolder.registerApplicationEventsIfNecessary();
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (recordingApplicationEvents(testContext)) {
			ApplicationEventsHolder.unregisterApplicationEvents();
		}
	}

	private boolean recordingApplicationEvents(TestContext testContext) {
		return TestContextAnnotationUtils.hasAnnotation(testContext.getTestClass(), RecordApplicationEvents.class);
	}

	@SuppressWarnings("resource")
	private void registerListenerAndResolvableDependency(TestContext testContext) {
		ApplicationContext applicationContext = testContext.getApplicationContext();
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext,
				"The ApplicationContext for the test must be an AbstractApplicationContext");
		AbstractApplicationContext aac = (AbstractApplicationContext) applicationContext;
		boolean alreadyRegistered = aac.getApplicationListeners().stream()
				.map(Object::getClass)
				.anyMatch(ApplicationEventsApplicationListener.class::equals);
		if (!alreadyRegistered) {
			// Register a new ApplicationEventsApplicationListener.
			aac.addApplicationListener(new ApplicationEventsApplicationListener());

			// Register ApplicationEvents as a resolvable dependency for @Autowired support in test classes.
			ConfigurableListableBeanFactory beanFactory = aac.getBeanFactory();
			beanFactory.registerResolvableDependency(ApplicationEvents.class, new ApplicationEventsObjectFactory());
		}
	}

	/**
	 * Factory that exposes the current {@link ApplicationEvents} object on demand.
	 */
	@SuppressWarnings("serial")
	private static class ApplicationEventsObjectFactory implements ObjectFactory<ApplicationEvents>, Serializable {

		@Override
		public ApplicationEvents getObject() {
			ApplicationEvents applicationEvents = ApplicationEventsHolder.getApplicationEvents();
			Assert.state(applicationEvents != null, "Failed to retrieve ApplicationEvents for the current test");
			return applicationEvents;
		}

		@Override
		public String toString() {
			return "Current ApplicationEvents";
		}
	}

}
