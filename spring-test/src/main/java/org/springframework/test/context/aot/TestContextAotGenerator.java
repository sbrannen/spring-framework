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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.context.ApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
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
 * @see ApplicationContextAotGenerator
 */
class TestContextAotGenerator {

	private static final Log logger = LogFactory.getLog(TestClassScanner.class);

	private final ApplicationContextAotGenerator aotGenerator = new ApplicationContextAotGenerator();

	private final AtomicInteger sequence = new AtomicInteger();

	private final GeneratedFiles generatedFiles;

	private final RuntimeHints runtimeHints;


	/**
	 * Create a new {@link TestContextAotGenerator} that uses the supplied
	 * {@link GeneratedFiles}.
	 * @param generatedFiles the {@code GeneratedFiles} to use
	 */
	public TestContextAotGenerator(GeneratedFiles generatedFiles) {
		this(generatedFiles, new RuntimeHints());
	}

	/**
	 * Create a new {@link TestContextAotGenerator} that uses the supplied
	 * {@link GeneratedFiles} and {@link RuntimeHints}.
	 * @param generatedFiles the {@code GeneratedFiles} to use
	 * @param runtimeHints the {@code RuntimeHints} to use
	 */
	public TestContextAotGenerator(GeneratedFiles generatedFiles, RuntimeHints runtimeHints) {
		this.generatedFiles = generatedFiles;
		this.runtimeHints = runtimeHints;
	}


	/**
	 * Get the {@link RuntimeHints} gathered during {@linkplain #processAheadOfTime(Stream)
	 * AOT processing}.
	 */
	public final RuntimeHints getRuntimeHints() {
		return this.runtimeHints;
	}

	/**
	 * Process each of the supplied Spring integration test classes and generate
	 * AOT artifacts.
	 * @throws TestContextAotException if an error occurs during AOT processing
	 */
	public void processAheadOfTime(Stream<Class<?>> testClasses) throws TestContextAotException {
		testClasses.forEach(testClass -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Generating AOT artifacts for test class [%s]"
						.formatted(testClass.getCanonicalName()));
			}
			try {
				DefaultGenerationContext generationContext = createGenerationContext(testClass);
				generateApplicationContextInitializer(generationContext, testClass);
				generationContext.writeGeneratedContent();
			}
			catch (Exception ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to generate AOT artifacts for test class [%s]"
							.formatted(testClass.getCanonicalName()), ex);
				}
			}
		});
	}

	/**
	 * Generate an {@link org.springframework.context.ApplicationContextInitializer
	 * ApplicationContextInitializer} for the supplied test class.
	 * @param testClass the test class for which the initializer should be generated
	 * @return the {@link ClassName} for the generated {@code ApplicationContextInitializer}
	 * @throws TestContextAotException if an error occurs during AOT processing
	 */
	ClassName generateApplicationContextInitializer(GenerationContext generationContext, Class<?> testClass)
			throws TestContextAotException {

		GenericApplicationContext gac = loadContextForAotProcessing(testClass);
		try {
			return this.aotGenerator.processAheadOfTime(gac, generationContext);
		}
		catch (Throwable ex) {
			throw new TestContextAotException("Failed to process test class [%s] for AOT"
					.formatted(testClass.getCanonicalName()), ex);
		}
	}

	/**
	 * Load the {@code GenericApplicationContext} for the supplied merged context
	 * configuration for AOT processing.
	 * <p>Only supports {@link SmartContextLoader SmartContextLoaders} that
	 * create {@link GenericApplicationContext GenericApplicationContexts}.
	 * @throws TestContextAotException if an error occurs while loading the application
	 * context or if one of the prerequisites is not met
	 * @see SmartContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
	 */
	private GenericApplicationContext loadContextForAotProcessing(Class<?> testClass)
			throws TestContextAotException {

		TestContextBootstrapper testContextBootstrapper =
				BootstrapUtils.resolveTestContextBootstrapper(testClass);
		MergedContextConfiguration mergedContextConfiguration =
				testContextBootstrapper.buildMergedContextConfiguration();
		ContextLoader contextLoader = mergedContextConfiguration.getContextLoader();
		Assert.notNull(contextLoader, """
				Cannot load an ApplicationContext with a NULL 'contextLoader'. \
				Consider annotating test class [%s] with @ContextConfiguration or \
				@ContextHierarchy.""".formatted(testClass.getCanonicalName()));

		if (!(contextLoader instanceof SmartContextLoader smartContextLoader)) {
			throw new TestContextAotException("""
					Cannot generate AOT artifacts for test class [%s]. The configured \
					ContextLoader [%s] must be a SmartContextLoader.""".formatted(
							testClass.getCanonicalName(), contextLoader.getClass().getName()));
		}

		ApplicationContext context;
		try {
			context = smartContextLoader.loadContextForAotProcessing(mergedContextConfiguration);
		}
		catch (Exception ex) {
			throw new TestContextAotException(
					"Failed to load ApplicationContext for AOT processing for test class [%s]"
						.formatted(testClass.getCanonicalName()), ex);
		}

		if (!(context instanceof GenericApplicationContext gac)) {
			throw new TestContextAotException("""
					Cannot generate AOT artifacts for test class [%s]. The configured \
					ContextLoader [%s] must create a GenericApplicationContext.""".formatted(
							testClass.getCanonicalName(), contextLoader.getClass().getName()));
		}
		return gac;
	}

	DefaultGenerationContext createGenerationContext(Class<?> testClass) {
		ClassNameGenerator classNameGenerator = new ClassNameGenerator(testClass);
		DefaultGenerationContext generationContext =
				new DefaultGenerationContext(classNameGenerator, this.generatedFiles, this.runtimeHints);
		return generationContext.withName(nextTestContextId());
	}

	private String nextTestContextId() {
		return "TestContext%03d_".formatted(this.sequence.incrementAndGet());
	}

}
