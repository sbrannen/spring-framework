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

import org.testng.SkipException;

import org.springframework.aot.AotDetector;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Abstract base class for a {@link TestExecutionListener} that aborts TestNG-based
 * test classes and test methods when running in AOT mode.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class AbstractAbortInAotModeTestExecutionListener extends AbstractTestExecutionListener {

	protected static void abortInAotMode(TestContext testContext) {
		if (AotDetector.useGeneratedArtifacts()) {
			throw new SkipException("Test class [%s] is not supported in AOT mode"
					.formatted(testContext.getTestClass().getName()));
		}
	}

}
