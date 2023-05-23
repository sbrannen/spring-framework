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

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 */
class BeanLiteModeTests {

	@Test
	void test() {
		try (var context = new AnnotationConfigApplicationContext(Config.class)) {
			var bean1 = context.getBean("bean1", String.class);
			var bean2 = context.getBean("bean2", String.class);

			assertThat(bean1).isEqualTo("bean1");
			assertThat(bean2).isEqualTo("bean2");
		}
	}

	static class Config extends BaseConfig {
	}

	static class BaseConfig {
		@Bean
		String bean1() {
			return "bean1";
		}

		@Bean
		String bean2() {
			return "bean2";
		}
	}

}
