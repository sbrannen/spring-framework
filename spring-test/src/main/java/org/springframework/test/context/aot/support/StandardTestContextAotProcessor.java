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

package org.springframework.test.context.aot.support;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.log.LogMessage;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.aot.TestContextAotProcessor;
import org.springframework.util.ResourceUtils;

/**
 * {@link TestContextAotProcessor} that supports standard features of the
 * <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class StandardTestContextAotProcessor implements TestContextAotProcessor {

	private static final Log logger = LogFactory.getLog(StandardTestContextAotProcessor.class);


	@Override
	public void processAheadOfTime(MergedContextConfiguration mergedConfig, List<Class<?>> testClasses,
			GenerationContext generationContext) {

		logger.info(LogMessage.format("Processing test classes %s", testClasses.stream().map(Class::getName).toList()));

		processContextConfiguration(mergedConfig, generationContext.getRuntimeHints());
	}

	/**
	 * Process {@code @ContextConfiguration} attributes.
	 * @see MergedContextConfiguration#getLocations()
	 */
	private void processContextConfiguration(MergedContextConfiguration mergedConfig, RuntimeHints runtimeHints) {
		for (String location : mergedConfig.getLocations()) {
			if (location.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
				String rawLocation = location.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length());
				System.err.println("Registering pattern for location: " + rawLocation);
				runtimeHints.resources().registerPattern(rawLocation);
			}
		}
	}

}
