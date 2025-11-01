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

package org.springframework.test.context.aot.samples.basic;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.aot.samples.management.ManagementConfiguration;
import org.springframework.test.context.env.YamlTestProperties;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ParameterizedClass @ParameterizedClass} variant of {@link BasicSpringJupiterTests}.
 *
 * @author Sam Brannen
 * @since 6.2.13
 */
@SpringJUnitConfig({BasicTestConfiguration.class, ManagementConfiguration.class})
@TestPropertySource(properties = "test.engine = jupiter")
@YamlTestProperties({
	"classpath:org/springframework/test/context/aot/samples/basic/test1.yaml",
	"classpath:org/springframework/test/context/aot/samples/basic/test2.yaml"
})
@ParameterizedClass
@ValueSource(strings = {"foo", "bar"})
public class BasicSpringJupiterParameterizedClassTests {

	private final String parameterizedString;

	@Resource
	Integer magicNumber;


	BasicSpringJupiterParameterizedClassTests(String parameterizedString) {
		this.parameterizedString = parameterizedString;
	}


	@Test
	void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
			@Value("${test.engine}") String testEngine) {
		assertThat("foo".equals(parameterizedString) || "bar".equals(parameterizedString)).isTrue();
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		assertThat(testEngine).isEqualTo("jupiter");
		assertThat(magicNumber).isEqualTo(42);
		assertEnvProperties(context);
	}

	@Nested
	@TestPropertySource(properties = "foo=bar")
	@ActiveProfiles(resolver = SpanishActiveProfilesResolver.class)
	public class NestedTests {

		@Test
		void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
				@Value("${test.engine}") String testEngine, @Value("${foo}") String foo) {
			assertThat("foo".equals(parameterizedString) || "bar".equals(parameterizedString)).isTrue();
			assertThat(messageService.generateMessage()).isEqualTo("¡Hola, AOT!");
			assertThat(foo).isEqualTo("bar");
			assertThat(testEngine).isEqualTo("jupiter");
			assertEnvProperties(context);
		}

		@Nested
		@TestPropertySource(properties = "foo=quux")
		public class DoublyNestedTests {

			@Test
			void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
					@Value("${test.engine}") String testEngine, @Value("${foo}") String foo) {
				assertThat("foo".equals(parameterizedString) || "bar".equals(parameterizedString)).isTrue();
				assertThat(messageService.generateMessage()).isEqualTo("¡Hola, AOT!");
				assertThat(foo).isEqualTo("quux");
				assertThat(testEngine).isEqualTo("jupiter");
				assertEnvProperties(context);
			}
		}
	}

	// This is here to ensure that an inner class is only considered a nested test
	// class if it's annotated with @Nested.
	public class NotReallyNestedTests {
	}


	static void assertEnvProperties(ApplicationContext context) {
		Environment env = context.getEnvironment();
		assertThat(env.getProperty("test.engine")).as("@TestPropertySource").isEqualTo("jupiter");
		assertThat(env.getProperty("test1.prop")).as("@TestPropertySource").isEqualTo("yaml");
		assertThat(env.getProperty("test2.prop")).as("@TestPropertySource").isEqualTo("yaml");
	}

}

