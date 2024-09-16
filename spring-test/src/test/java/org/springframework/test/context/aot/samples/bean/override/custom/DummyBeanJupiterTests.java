/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.aot.samples.bean.override.custom;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.DummyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
public class DummyBeanJupiterTests {

	@DummyBean
	String magicString;

	@DummyBean
	Integer magicNumber;

	@Test
	void test() {
		assertThat(magicString).isEqualTo("overridden");
		assertThat(magicNumber).isEqualTo(42);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		String magicString() {
			return "enigma";
		}

		@Bean
		Integer magicNumber() {
			return -1;
		}

	}

}
