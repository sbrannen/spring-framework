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
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.Assert;

/**
 * {@code TestExecutionListener} which provides support for {@link ApplicationEvents}.
 *
 * @author Sam Brannen
 * @since 5.3.2
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
			getThreadBoundApplicationListenerAdapter(testContext).registerDelegate(new DefaultApplicationEvents());
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (recordingApplicationEvents(testContext)) {
			getThreadBoundApplicationListenerAdapter(testContext).unregisterDelegate();
		}
	}

	private boolean recordingApplicationEvents(TestContext testContext) {
		return AnnotatedElementUtils.hasAnnotation(testContext.getTestClass(), RecordApplicationEvents.class);
	}

	@SuppressWarnings("resource")
	private ThreadBoundApplicationListenerAdapter getThreadBoundApplicationListenerAdapter(TestContext testContext) {
		ApplicationContext applicationContext = testContext.getApplicationContext();
		String beanName = ThreadBoundApplicationListenerAdapter.class.getName();

		if (applicationContext.containsBean(beanName)) {
			return applicationContext.getBean(beanName, ThreadBoundApplicationListenerAdapter.class);
		}

		// Else create and register a new ThreadBoundApplicationListenerAdapter.
		Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
			"The ApplicationContext for the test must be a ConfigurableApplicationContext");
		ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
		ThreadBoundApplicationListenerAdapter listener = new ThreadBoundApplicationListenerAdapter();
		ConfigurableListableBeanFactory beanFactory = cac.getBeanFactory();
		beanFactory.registerSingleton(beanName, listener);
		cac.addApplicationListener(listener);

		// Register ApplicationEvents as a resolvable dependency for @Autowired support in test classes.
		beanFactory.registerResolvableDependency(ApplicationEvents.class, new ApplicationEventsObjectFactory(listener));

		return listener;
	}

	/**
	 * Factory that exposes the current {@link ApplicationEvents} object on demand.
	 */
	@SuppressWarnings("serial")
	private static class ApplicationEventsObjectFactory implements ObjectFactory<ApplicationEvents>, Serializable {

		private final ThreadBoundApplicationListenerAdapter listener;


		ApplicationEventsObjectFactory(ThreadBoundApplicationListenerAdapter listener) {
			this.listener = listener;
		}

		@Override
		public ApplicationEvents getObject() {
			ApplicationListener<ApplicationEvent> applicationListener = this.listener.getDelegate();
			if (applicationListener instanceof ApplicationEvents) {
				return (ApplicationEvents) applicationListener;
			}
			throw new IllegalStateException("Failed to retrieve ApplicationEvents for the current test");
		}

		@Override
		public String toString() {
			return "Current ApplicationEvents";
		}
	}

}
