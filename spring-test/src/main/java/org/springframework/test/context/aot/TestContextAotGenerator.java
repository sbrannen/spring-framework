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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.lang.Nullable;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.util.Assert;

/**
 * {@code TestContextAotGenerator} generates AOT artifacts for integration tests
 * that depend on support from the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class TestContextAotGenerator {

	private static final Log logger = LogFactory.getLog(TestClassScanner.class);

	private final ApplicationContextAotGenerator aotGenerator = new ApplicationContextAotGenerator();

	private final GenerationContext generationContext;


	public TestContextAotGenerator(GenerationContext generationContext) {
		this.generationContext = generationContext;
	}

	/**
	 * Generate an {@code ApplicationContextInitializer} for the supplied test class.
	 */
	@Nullable
	public ClassName generateContextInitializer(Class<?> testClass) throws Exception {
		GenericApplicationContext gac = loadApplicationContextWithoutRefresh(testClass);
		return (gac != null ?
				this.aotGenerator.generateApplicationContext(gac, this.generationContext) : null);
	}

	/**
	 * Load the {@code GenericApplicationContext} for the supplied merged context
	 * configuration without refreshing the {@code ApplicationContext}.
	 * <p>Only supports {@link SmartContextLoader SmartContextLoaders} that
	 * create {@link GenericApplicationContext GenericApplicationContexts}.
	 * @throws Exception if an error occurs while loading the application context
	 */
	@Nullable
	private GenericApplicationContext loadApplicationContextWithoutRefresh(Class<?> testClass) throws Exception {
		TestContextBootstrapper testContextBootstrapper = BootstrapUtils.resolveTestContextBootstrapper(testClass);
		MergedContextConfiguration mergedContextConfiguration = testContextBootstrapper.buildMergedContextConfiguration();
		ContextLoader contextLoader = mergedContextConfiguration.getContextLoader();
		Assert.notNull(contextLoader, """
				Cannot load an ApplicationContext with a NULL 'contextLoader'. \
				Consider annotating test class [%s] with @ContextConfiguration or \
				@ContextHierarchy.""".formatted(testClass.getName()));

		if (!(contextLoader instanceof SmartContextLoader smartContextLoader)) {
			if (logger.isInfoEnabled()) {
				logger.info("""
						Cannot generate AOT artifacts for test class [%s]. The configured \
						ContextLoader [%s] must be a SmartContextLoader.""".formatted(
								testClass.getName(), contextLoader.getClass().getName()));
			}
			return null;
		}

		ApplicationContext context =
				smartContextLoader.loadContextForAotProcessing(mergedContextConfiguration);
		if (context instanceof GenericApplicationContext gac) {
			return gac;
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("""
					Cannot generate AOT artifacts for test class [%s]. The configured \
					ContextLoader [%s] must create a GenericApplicationContext.""".formatted(
							testClass.getName(), contextLoader.getClass().getName()));
			}
			return null;
		}
	}

}
