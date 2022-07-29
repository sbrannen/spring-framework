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

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.test.aot.generate.TestGenerationContext;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests;
import org.springframework.test.context.aot.samples.common.MessageService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class TestContextAotGeneratorTests {

	@Test
	@CompileWithTargetClassAccess
	void generateContextInitializer() throws Exception {
		Class<?> testClass = BasicSpringJupiterTests.class;
		TestGenerationContext generationContext = new TestGenerationContext(testClass);
		TestContextAotGenerator generator = new TestContextAotGenerator(generationContext);

		ClassName className = generator.generateContextInitializer(testClass);
		assertThat(className).isNotNull();

		compile(generationContext, className.reflectionName(), context -> {
			MessageService messageService = context.getBean(MessageService.class);
			assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		});
	}

	@SuppressWarnings("unchecked")
	private void compile(TestGenerationContext generationContext, String initializerClassName, Consumer<GenericApplicationContext> result) {
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(generationContext.getGeneratedFiles()).compile(compiled -> {
			GenericApplicationContext gac = new GenericApplicationContext();
			ApplicationContextInitializer<GenericApplicationContext> contextInitializer =
					compiled.getInstance(ApplicationContextInitializer.class, initializerClassName);
			contextInitializer.initialize(gac);
			gac.refresh();
			result.accept(gac);
		});
	}

}
