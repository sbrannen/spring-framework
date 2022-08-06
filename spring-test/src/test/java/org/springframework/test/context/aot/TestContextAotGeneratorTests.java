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
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.test.aot.generate.TestGenerationContext;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringTestNGTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringVintageTests;
import org.springframework.test.context.aot.samples.common.MessageService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
@CompileWithTargetClassAccess
class TestContextAotGeneratorTests {

	/**
	 * @see AotSmokeTests#scanClassPathThenGenerateSourceFilesAndCompileThem()
	 */
	@Test
	void generate() {
		Stream<Class<?>> testClasses = Stream.of(
			BasicSpringJupiterTests.class,
			BasicSpringJupiterTests.NestedTests.class,
			BasicSpringVintageTests.class,
			BasicSpringTestNGTests.class);

		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);

		generator.generate(testClasses);

		List<String> sourceFiles = generatedFiles.getGeneratedFiles(Kind.SOURCE).keySet().stream()
			// .peek(System.out::println)
			.filter(name -> name.startsWith("org/springframework/test/context/aot/samples/basic/"))
			.map(name -> name.substring("org/springframework/test/context/aot/samples/basic/".length()))
			.filter(name -> name.endsWith("__ApplicationContextInitializer.java"))
			.map(name -> name.substring(0, name.length() - "__ApplicationContextInitializer.java".length()))
			.toList();

		assertThat(sourceFiles).containsExactlyInAnyOrder(
			"BasicSpringJupiterTests",
			"BasicSpringJupiterTests_NestedTests",
			"BasicSpringVintageTests",
			"BasicSpringTestNGTests");

		TestCompiler.forSystem().withFiles(generatedFiles).compile(compiled -> {
			// just make sure compilation completes without errors
		});
	}

	@Test
	void generateApplicationContextInitializer() {
		List<ClassName> classNames = new ArrayList<>();
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		// We cannot use @ParameterizedTest, since @CompileWithTargetClassAccess
		// cannot support @ParameterizedTest methods.
		Set.of(BasicSpringTestNGTests.class, BasicSpringVintageTests.class, BasicSpringJupiterTests.class)
			.forEach(testClass -> {
				ClassNameGenerator classNameGenerator = new ClassNameGenerator(testClass);
				TestGenerationContext generationContext = new TestGenerationContext(classNameGenerator, generatedFiles);
				TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);

				ClassName className = generator.generateApplicationContextInitializer(generationContext, testClass);
				assertThat(className).isNotNull();
				classNames.add(className);
				// System.err.println(className);
				generationContext.writeGeneratedContent();

				// generatedFiles.getGeneratedFiles(Kind.SOURCE).keySet().stream().forEach(System.err::println);

				// generatedFiles.getGeneratedFiles(Kind.SOURCE).entrySet().stream()
				// 	.filter(entry -> entry.getKey().endsWith("DefaultEventListenerFactory__BeanDefinitions.java"))
				// 	.forEach(entry -> System.err.println(entry.getValue()));

			});

		compile(generatedFiles, classNames, context -> {
			MessageService messageService = context.getBean(MessageService.class);
			assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		});
	}


	@SuppressWarnings("unchecked")
	private void compile(InMemoryGeneratedFiles generatedFiles, List<ClassName> classNames,
			Consumer<GenericApplicationContext> result) {

		TestCompiler.forSystem().withFiles(generatedFiles).compile(compiled -> {
			classNames.forEach(className -> {
				GenericApplicationContext gac = new GenericApplicationContext();
				ApplicationContextInitializer<GenericApplicationContext> contextInitializer =
						compiled.getInstance(ApplicationContextInitializer.class, className.reflectionName());
				contextInitializer.initialize(gac);
				gac.refresh();
				result.accept(gac);
			});
		});
	}

}
