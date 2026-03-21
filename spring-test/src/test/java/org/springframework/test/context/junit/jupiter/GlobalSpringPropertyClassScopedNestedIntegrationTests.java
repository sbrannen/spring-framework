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

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests for {@link SpringExtension#EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME}
 * set to {@code test_class} (JUnit configuration parameter or {@code SpringProperties}).
 *
 * @author Sam Brannen
 * @since 7.0.7
 * @see SpringExtensionExtensionContextScopeTests
 */
@Disabled("Launched programmatically with DisabledCondition deactivated — see SpringExtensionExtensionContextScopeTests")
@SpringJUnitConfig
@TestPropertySource(properties = "p1 = v1")
@NestedTestConfiguration(OVERRIDE)
class GlobalSpringPropertyClassScopedNestedIntegrationTests {

	@Autowired
	Environment env1;

	@Test
	void propertiesInEnvironment() {
		assertThat(env1.getProperty("p1")).isEqualTo("v1");
	}

	@Nested
	@SpringJUnitConfig(Config.class)
	@TestPropertySource(properties = "p2 = v2")
	class ConfigOverriddenByDefaultTests {

		@Autowired
		Environment env2;

		@Test
		void propertiesInEnvironment() {
			assertThat(env1.getProperty("p1")).isEqualTo("v1");
			assertThat(env1).isNotSameAs(env2);
			assertThat(env2.getProperty("p1")).isNull();
			assertThat(env2.getProperty("p2")).isEqualTo("v2");
		}
	}

	@Configuration
	static class Config {
	}
}
