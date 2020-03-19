/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.DynamicPropertyValues;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DynamicPropertiesContextCustomizer}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class DynamicPropertiesContextCustomizerTests {

	@Test
	void createWhenNonStaticDynamicPropertiesMethodThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> customizerFor("nonStatic"))
			.withMessage("@DynamicPropertySource method 'nonStatic' must be static");
	}

	@Test
	void createWhenBadDynamicPropertiesSignatureThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> customizerFor("badArgs"))
			.withMessage("@DynamicPropertySource method 'badArgs' must accept a single DynamicPropertyValues argument");
	}

	@Test
	void nullPropertyNameResultsInException() throws Exception {
		DynamicPropertiesContextCustomizer customizer = customizerFor("nullName");
		ConfigurableApplicationContext context = new StaticApplicationContext();
		assertThatIllegalArgumentException()
			.isThrownBy(() -> customizer.customizeContext(context, mock(MergedContextConfiguration.class)))
			.withMessage("'name' must not be null or blank");
	}

	@Test
	void emptyPropertyNameResultsInException() throws Exception {
		DynamicPropertiesContextCustomizer customizer = customizerFor("emptyName");
		ConfigurableApplicationContext context = new StaticApplicationContext();
		assertThatIllegalArgumentException()
			.isThrownBy(() -> customizer.customizeContext(context, mock(MergedContextConfiguration.class)))
			.withMessage("'name' must not be null or blank");
	}

	@Test
	void nullValueSupplierResultsInException() throws Exception {
		DynamicPropertiesContextCustomizer customizer = customizerFor("nullValueSupplier");
		ConfigurableApplicationContext context = new StaticApplicationContext();
		assertThatIllegalArgumentException()
			.isThrownBy(() -> customizer.customizeContext(context, mock(MergedContextConfiguration.class)))
			.withMessage("'valueSupplier' must not be null");
	}

	@Test
	void customizeContextAddsPropertySource() throws Exception {
		ConfigurableApplicationContext context = new StaticApplicationContext();
		DynamicPropertiesContextCustomizer customizer = customizerFor("valid1", "valid2");
		customizer.customizeContext(context, mock(MergedContextConfiguration.class));
		ConfigurableEnvironment environment = context.getEnvironment();
		assertThat(environment.getRequiredProperty("p1a")).isEqualTo("v1a");
		assertThat(environment.getRequiredProperty("p1b")).isEqualTo("v1b");
		assertThat(environment.getRequiredProperty("p2a")).isEqualTo("v2a");
		assertThat(environment.getRequiredProperty("p2b")).isEqualTo("v2b");
	}

	@Test
	void equalsAndHashCode() {
		DynamicPropertiesContextCustomizer c1 = customizerFor("valid1", "valid2");
		DynamicPropertiesContextCustomizer c2 = customizerFor("valid1", "valid2");
		DynamicPropertiesContextCustomizer c3 = customizerFor("valid1");
		assertThat(c1.hashCode()).isEqualTo(c1.hashCode()).isEqualTo(c2.hashCode());
		assertThat(c1).isEqualTo(c1).isEqualTo(c2).isNotEqualTo(c3);
	}


	private static DynamicPropertiesContextCustomizer customizerFor(String...methods) {
		return new DynamicPropertiesContextCustomizer(findMethods(methods));
	}

	private static Set<Method> findMethods(String... names) {
		Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(DynamicPropertySourceTestCase.class,
				method -> ObjectUtils.containsElement(names, method.getName()));
		return new LinkedHashSet<>(Arrays.asList(methods));
	}


	static class DynamicPropertySourceTestCase {

		void nonStatic(DynamicPropertyValues values) {
		}

		static void badArgs(String bad) {
		}

		static void nullName(DynamicPropertyValues values) {
			values.add(null, () -> "A");
		}

		static void emptyName(DynamicPropertyValues values) {
			values.add("   ", () -> "A");
		}

		static void nullValueSupplier(DynamicPropertyValues values) {
			values.add("name", null);
		}

		static void valid1(DynamicPropertyValues values) {
			values.add("p1a", () -> "v1a");
			values.add("p1b", () -> "v1b");
		}

		static void valid2(DynamicPropertyValues values) {
			values.add("p2a", () -> "v2a");
			values.add("p2b", () -> "v2b");
		}

	}

}
