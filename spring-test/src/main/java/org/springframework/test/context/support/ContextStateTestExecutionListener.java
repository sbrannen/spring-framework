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

package org.springframework.test.context.support;

import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;

/**
 * {@code TestExecutionListener} which marks a test's application context as
 * {@linkplain TestContext#markApplicationContextInactive() inactive} after
 * execution of the test class has ended.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see #getOrder()
 * @see TestContext#markApplicationContextInactive()
 */
public class ContextStateTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * The {@link #getOrder() order} value for this listener
	 * ({@code Ordered.HIGHEST_PRECEDENCE}): {@value}.
	 */
	public static final int ORDER = Ordered.HIGHEST_PRECEDENCE;


	/**
	 * Returns {@link #ORDER}, which ensures that this listener has the highest
	 * precedence.
	 * <p>Due to the wrapping behavior of listeners, this means that the
	 * {@link #afterTestClass(TestContext)} method in this listener will be
	 * invoked after all other listeners. See
	 * {@linkplain org.springframework.test.context.TestExecutionListener
	 * Wrapping Behavior for Listeners} for further details.
	 */
	@Override
	public final int getOrder() {
		return ORDER;
	}

	/**
	 * Marks the test's application context as
	 * {@linkplain TestContext#markApplicationContextInactive() inactive}
	 * if the application context for the supplied test context is known to be
	 * {@linkplain TestContext#hasApplicationContext() available}.
	 */
	@Override
	public void afterTestClass(TestContext testContext) {
		if (testContext.hasApplicationContext()) {
			testContext.markApplicationContextInactive();
		}
	}

}
