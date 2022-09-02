/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.aot;

import java.util.List;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * SPI for contributing to the ahead-of-time (AOT) processing of test classes
 * in the <em>Spring TestContext Framework</em>.
 *
 * <p>Each processor registered via a {@code META-INF/spring/aot.factories} file
 * will be given a chance to {@linkplain #processAheadOfTime process} Spring
 * integration test classes to generate AOT-optimized code or contribute run-time
 * hints for use in a native image.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see org.springframework.beans.factory.aot.AotServices
 * @see org.springframework.aot.hint.RuntimeHints
 * @see org.springframework.aot.generate.GenerationContext
 */
public interface TestContextAotProcessor {

	/**
	 * Process the supplied merged context configuration and associated test classes
	 * ahead-of-time using the specified {@link GenerationContext}.
	 * @param mergedConfig the merged context configuration to process
	 * @param testClasses the test classes that share the supplied merged context
	 * configuration
	 * @param generationContext the generation context to use
	 */
	void processAheadOfTime(MergedContextConfiguration mergedConfig, List<Class<?>> testClasses,
			GenerationContext generationContext);

}
