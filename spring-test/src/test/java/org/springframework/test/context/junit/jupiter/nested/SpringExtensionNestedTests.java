package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class SpringExtensionNestedTests {

	@Test
	void test(@Autowired String foo) {
		assertThat(foo).isEqualTo("bar");
	}

	@Nested
	class InnerTests {

		@Test
		void test(@Autowired String foo) {
			assertThat(foo).isEqualTo("bar");
		}

	}

	@Configuration
	static class TestConfig {

		@Bean
		String foo() {
			return "bar";
		}

	}

}