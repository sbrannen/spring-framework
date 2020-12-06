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

import org.springframework.context.ApplicationContext;
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
	 * Returns {@code 1500}.
	 */
	@Override
	public final int getOrder() {
		return 1500;
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
		cac.getBeanFactory().registerSingleton(beanName, listener);
		cac.addApplicationListener(listener);
		return listener;
	}

}
