/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.testng.transaction.ejb;

import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * {@link TestExecutionListener} that aborts TestNG-based test classes and test
 * methods when running in AOT mode -- handles "after" callbacks.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class AbortInAotModeAfterCallbacksTestExecutionListener extends AbstractAbortInAotModeTestExecutionListener {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		// Have to implement this method because AbstractTestNGSpringContextTests.springTestContextAfterTestMethod()
		// is annotated with @AfterMethod(alwaysRun = true).
		abortInAotMode(testContext);
	}

	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		abortInAotMode(testContext);
	}

}
