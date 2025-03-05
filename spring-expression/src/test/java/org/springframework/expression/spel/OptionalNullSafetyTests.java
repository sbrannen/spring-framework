/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.expression.spel;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests which verify support for using {@link Optional} with the null-safe and
 * Elvis operators in SpEL expressions.
 *
 * @author Sam Brannen
 * @since 7.0
 */
class OptionalNullSafetyTests {

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final StandardEvaluationContext context = new StandardEvaluationContext();


	@BeforeEach
	void setUpContext() {
		context.setVariable("service", new Service());
	}


	@Test
	void accessPropertyDirectlyOnEmptyOptional() {
		assertPropertyAccessFails("#service.findJediByName('').name");
	}

	@Test
	void accessPropertyDirectlyOnNonEmptyOptional() {
		assertPropertyAccessFails("#service.findJediByName('Yoda').name");
	}

	private void assertPropertyAccessFails(String expression) {
		Expression expr = parser.parseExpression(expression);

		assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> expr.getValue(context))
				.satisfies(ex -> {
					assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE);
					assertThat(ex).hasMessageContaining("Property or field 'name' cannot be found on object of type 'java.util.Optional'");
				});
	}


	@Nested
	class NullSafeTests {

		@Test
		void accessIndexOnEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('blue')?.[1]");

			assertThat(expr.getValue(context)).isNull();
		}

		@Test
		void accessIndexOnNonEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.[1]");

			assertThat(expr.getValue(context)).isEqualTo("lemon");
		}

		@Test
		void accessPropertyOnEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findJediByName('')?.name");

			assertThat(expr.getValue(context)).isNull();
		}

		@Test
		void accessPropertyOnNonEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findJediByName('Yoda')?.name");

			assertThat(expr.getValue(context)).isEqualTo("Yoda");
		}

		@Test
		void invokeMethodOnEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findJediByName('')?.salutation('Master')");

			assertThat(expr.getValue(context)).isNull();
		}

		@Test
		void invokeMethodOnNonEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findJediByName('Yoda')?.salutation('Master')");

			assertThat(expr.getValue(context)).isEqualTo("Master Yoda");
		}

		@Test
		void projectionOnEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('blue')?.![#this.length]");

			assertThat(expr.getValue(context)).isNull();
		}

		@Test
		@SuppressWarnings("unchecked")
		void projectionOnNonEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.![#this.length]");

			assertThat(expr.getValue(context, List.class)).containsExactly(6, 5, 5, 9);
		}

		@Test
		void selectionOnEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('blue')?.?[#this.length > 5]");

			assertThat(expr.getValue(context)).isNull();
		}

		@Test
		@SuppressWarnings("unchecked")
		void selectionOnNonEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.?[#this.length > 5]");

			assertThat(expr.getValue(context, List.class)).containsExactly("banana", "pineapple");
		}

		@Test
		void selectFirstOnEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('blue')?.^[#this.length > 5]");

			assertThat(expr.getValue(context)).isNull();
		}

		@Test
		@SuppressWarnings("unchecked")
		void selectFirstOnNonEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.^[#this.length > 5]");

			assertThat(expr.getValue(context, List.class)).containsExactly("banana");
		}

		@Test
		void selectLastOnEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('blue')?.$[#this.length > 5]");

			assertThat(expr.getValue(context)).isNull();
		}

		@Test
		@SuppressWarnings("unchecked")
		void selectLastOnNonEmptyOptionalViaNullSafeOperator() {
			Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.$[#this.length > 5]");

			assertThat(expr.getValue(context, List.class)).containsExactly("pineapple");
		}

	}

	@Nested
	class ElvisTests {

		@Test
		void elvisOperatorOnEmptyOptional() {
			Expression expr = parser.parseExpression("#service.findJediByName('') ?: 'unknown'");

			assertThat(expr.getValue(context)).isEqualTo("unknown");
		}

		@Test
		void elvisOperatorOnNonEmptyOptional() {
			Expression expr = parser.parseExpression("#service.findJediByName('Yoda') ?: 'unknown'");

			assertThat(expr.getValue(context)).isEqualTo(new Jedi("Yoda"));
		}

	}


	record Jedi(String name) {

		public String salutation(String salutation) {
			return salutation + " " + this.name;
		}
	}

	static class Service {

		public Optional<Jedi> findJediByName(String name) {
			return (!name.isEmpty() ? Optional.of(new Jedi(name)) : Optional.empty());
		}

		public Optional<List<String>> findFruitsByColor(String color) {
			return (color.equals("yellow") ? Optional.of(List.of("banana", "lemon", "mango", "pineapple")) :
					Optional.empty());
		}

	}

}
