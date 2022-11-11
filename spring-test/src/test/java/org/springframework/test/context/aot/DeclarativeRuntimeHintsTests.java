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

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.test.context.aot.samples.hints.DeclarativeRuntimeHintsSpringJupiterTests;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.resource;

/**
 * Tests for declarative support for registering run-time hints for tests, tested
 * via the {@link TestContextAotGenerator}
 *
 * @author Sam Brannen
 * @since 6.0
 */
class DeclarativeRuntimeHintsTests extends AbstractAotTests {

	@Test
	void declarativeRuntimeHints() {
		Set<Class<?>> testClasses = Set.of(DeclarativeRuntimeHintsSpringJupiterTests.class);
		TestContextAotGenerator generator = new TestContextAotGenerator(new InMemoryGeneratedFiles());
		RuntimeHints runtimeHints = generator.getRuntimeHints();

		generator.processAheadOfTime(testClasses.stream().sorted(comparing(Class::getName)));

		// @Reflective
		assertReflectionRegistered(runtimeHints, DeclarativeRuntimeHintsSpringJupiterTests.class);

		// @ImportRuntimeHints
		assertThat(resource().forResource("org/example/config/enigma.txt")).accepts(runtimeHints);
		assertThat(resource().forResource("org/example/config/level2/foo.txt")).accepts(runtimeHints);
	}

	private static void assertReflectionRegistered(RuntimeHints runtimeHints, Class<?> type) {
		assertThat(reflection().onType(type))
			.as("Reflection hint for %s", type.getSimpleName())
			.accepts(runtimeHints);
	}

	private static void assertReflectionRegistered(RuntimeHints runtimeHints, Class<?> type, MemberCategory memberCategory) {
		assertThat(reflection().onType(type).withMemberCategory(memberCategory))
			.as("Reflection hint for %s with category %s", type.getSimpleName(), memberCategory)
			.accepts(runtimeHints);
	}

}
