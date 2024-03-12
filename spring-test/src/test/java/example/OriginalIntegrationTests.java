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

package example;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class OriginalIntegrationTests {

	private final ParentString.Child child;

	OriginalIntegrationTests(@Qualifier("childString") Parent<String>.Child child) {
		this.child = child;
	}

	@Test
	void test() {
		// java.lang.ClassCastException: class java.lang.Integer cannot be cast
		// to class java.lang.String
		assertThat(this.child.getValue()).startsWith("a");
	}

	@Configuration
	@Import({ ParentString.class, ParentInt.class })
	static class Config {
	}

	static abstract class Parent<T> {

		public class Child {

			public Child(T value) {
				this.value = value;
			}

			private final T value;

			public T getValue() {
				return this.value;
			}
		}
	}

	static class ParentInt extends Parent<Integer> {
		@Bean
		public Child childInt() {
			return new Child(123);
		}
	}

	static class ParentString extends Parent<String> {
		@Bean
		public Child childString() {
			return new Child("abc");
		}
	}

}
