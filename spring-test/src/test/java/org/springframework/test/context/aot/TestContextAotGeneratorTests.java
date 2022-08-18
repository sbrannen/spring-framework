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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterSharedConfigTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringTestNGTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringVintageTests;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.aot.samples.web.WebSpringJupiterTests;
import org.springframework.test.context.aot.samples.web.WebSpringTestNGTests;
import org.springframework.test.context.aot.samples.web.WebSpringVintageTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.function.ThrowingConsumer;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Tests for {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
@CompileWithTargetClassAccess
class TestContextAotGeneratorTests extends AbstractAotTests {

	/**
	 * @see AotSmokeTests#scanClassPathThenGenerateSourceFilesAndCompileThem()
	 */
	@Test
	void generate() {
		Stream<Class<?>> testClasses = Stream.of(
				BasicSpringJupiterSharedConfigTests.class,
				BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class,
				BasicSpringTestNGTests.class,
				BasicSpringVintageTests.class);

		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);

		generator.processAheadOfTime(testClasses);

		List<String> sourceFiles = generatedFiles.getGeneratedFiles(Kind.SOURCE).keySet().stream().toList();
		assertThat(sourceFiles).containsExactlyInAnyOrder(expectedSourceFilesForBasicSpringTests);

		TestCompiler.forSystem().withFiles(generatedFiles).compile(compiled -> {
			// just make sure compilation completes without errors
		});
	}

	@Test
	// We cannot parameterize with the test classes, since @CompileWithTargetClassAccess
	// cannot support @ParameterizedTest methods.
	void processAheadOfTimeWithBasicTests() {
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
		Set<Class<?>> testClasses = Set.of(
				BasicSpringJupiterSharedConfigTests.class,
				BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class,
				BasicSpringTestNGTests.class,
				BasicSpringVintageTests.class);
		List<Mapping> mappings = processAheadOfTime(generator, testClasses);

		compile(generatedFiles, mappings, context -> {
			MessageService messageService = context.getBean(MessageService.class);
			assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
			assertThat(context.getEnvironment().getProperty("test.engine"))
				.as("@TestPropertySource").isNotNull();
		});
	}

	@Test
	void processAheadOfTimeWithWebTests() {
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
		Set<Class<?>> testClasses = Set.of(
				WebSpringJupiterTests.class,
				WebSpringTestNGTests.class,
				WebSpringVintageTests.class);
		List<Mapping> mappings = processAheadOfTime(generator, testClasses);

		compile(generatedFiles, mappings, context -> {
			assertThat(context.getEnvironment().getProperty("test.engine"))
				.as("@TestPropertySource").isNotNull();

			MockMvc mockMvc = webAppContextSetup((WebApplicationContext) context).build();
			mockMvc.perform(get("/hello"))
				.andExpectAll(status().isOk(), content().string("Hello, AOT!"));
		});
	}


	private List<Mapping> processAheadOfTime(TestContextAotGenerator generator, Set<Class<?>> testClasses) {
		List<Mapping> mappings = new ArrayList<>();
		testClasses.forEach(testClass -> {
			DefaultGenerationContext generationContext = generator.createGenerationContext(testClass);
			MergedContextConfiguration mergedConfig = generator.buildMergedContextConfiguration(testClass);
			ClassName className = generator.processAheadOfTime(mergedConfig, generationContext);
			assertThat(className).isNotNull();
			mappings.add(new Mapping(mergedConfig, className));
			generationContext.writeGeneratedContent();
		});
		return mappings;
	}

	@SuppressWarnings("unchecked")
	private void compile(InMemoryGeneratedFiles generatedFiles, List<Mapping> mappings, ThrowingConsumer<ApplicationContext> result) {
		TestCompiler.forSystem().withFiles(generatedFiles).compile(compiled -> {
			mappings.forEach(mapping -> {
				MergedContextConfiguration mergedConfig = mapping.mergedConfig();
				ApplicationContextInitializer<GenericApplicationContext> contextInitializer =
						compiled.getInstance(ApplicationContextInitializer.class, mapping.className().reflectionName());
				AotRuntimeContextLoader aotRuntimeContextLoader = new AotRuntimeContextLoader();
				GenericApplicationContext context = aotRuntimeContextLoader.loadContext(mergedConfig, contextInitializer);
				result.accept(context);
			});
		});
	}


	record Mapping(MergedContextConfiguration mergedConfig, ClassName className) {
	}

}
