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

import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@code TestExecutionListener} which provides support for {@link ApplicationEvents}.
 *
 * @author Sam Brannen
 * @since 5.3.2
 */
public class ApplicationEventsTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Returns {@code 15000}.
	 */
	@Override
	public final int getOrder() {
		return 15_000;
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		getThreadBoundApplicationListenerAdapter(testContext).ifPresent(
			listenerAdapter -> listenerAdapter.registerDelegate(new DefaultApplicationEvents()));
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		getThreadBoundApplicationListenerAdapter(testContext).ifPresent(
			listenerAdapter -> listenerAdapter.unregisterDelegate());
	}

	@Nullable
	private Optional<ThreadBoundApplicationListenerAdapter> getThreadBoundApplicationListenerAdapter(
			TestContext testContext) {

		if (testContext.hasApplicationContext()) {
			ApplicationContext applicationContext = testContext.getApplicationContext();
			String beanName = ThreadBoundApplicationListenerAdapter.class.getName();
			if (applicationContext.containsBean(beanName)) {
				return Optional.of(applicationContext.getBean(beanName, ThreadBoundApplicationListenerAdapter.class));
			}
		}
		return Optional.empty();
	}

}
