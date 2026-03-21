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

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import org.springframework.core.SpringProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension.ExtensionContextScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * Tests for {@link SpringExtension.ExtensionContextScope} and
 * {@link SpringExtension#EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME}.
 *
 * @author Sam Brannen
 * @since 7.0.7
 */
class SpringExtensionExtensionContextScopeTests {

	private static final String JUNIT_DISABLED_CONDITION_DEACTIVATE_PROPERTY = "junit.jupiter.conditions.deactivate";

	private static final String JUNIT_DISABLED_CONDITION_CLASS_NAME = "org.junit.jupiter.engine.extension.DisabledCondition";

	@Test
	void extensionContextScopeFromString() {
		assertThat(ExtensionContextScope.from(null)).isNull();
		assertThat(ExtensionContextScope.from("")).isNull();
		assertThat(ExtensionContextScope.from("   ")).isNull();
		assertThat(ExtensionContextScope.from("TEST_METHOD")).isEqualTo(ExtensionContextScope.TEST_METHOD);
		assertThat(ExtensionContextScope.from("test_method")).isEqualTo(ExtensionContextScope.TEST_METHOD);
		assertThat(ExtensionContextScope.from("TEST_CLASS")).isEqualTo(ExtensionContextScope.TEST_CLASS);
		assertThat(ExtensionContextScope.from("test_class")).isEqualTo(ExtensionContextScope.TEST_CLASS);
		assertThat(ExtensionContextScope.from("bogus")).isNull();
	}

	@Test
	void executeWithInvalidExtensionContextScopePropertyFails() {
		SpringProperties.setProperty(SpringExtension.EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, "bogus");
		try {
			SummaryGeneratingListener listener = execute(BrokenScopeTestCase.class, false, false);
			assertThat(listener.getSummary().getTestsFailedCount()).isGreaterThan(0);
		}
		finally {
			SpringProperties.setProperty(SpringExtension.EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, null);
		}
	}

	@Test
	void executeWithJUnitConfigurationParameterForTestClassScope() {
		SummaryGeneratingListener listener = execute(GlobalSpringPropertyClassScopedNestedIntegrationTests.class, true, true);
		assertThat(listener.getSummary().getTestsSucceededCount()).isEqualTo(2);
		assertThat(listener.getSummary().getTestsFailedCount()).isZero();
	}

	@Test
	void executeWithSpringPropertiesForTestClassScope() {
		SpringProperties.setProperty(SpringExtension.EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, "test_class");
		try {
			SummaryGeneratingListener listener = execute(GlobalSpringPropertyClassScopedNestedIntegrationTests.class, false, true);
			assertThat(listener.getSummary().getTestsSucceededCount()).isEqualTo(2);
			assertThat(listener.getSummary().getTestsFailedCount()).isZero();
		}
		finally {
			SpringProperties.setProperty(SpringExtension.EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, null);
		}
	}

	@Test
	void springExtensionConfigFalseOverridesGlobalTestClassScope() {
		SummaryGeneratingListener listener = execute(SpringExtensionConfigOverridesGlobalPropertyNestedIntegrationTests.class, true, true);
		assertThat(listener.getSummary().getTestsSucceededCount()).isEqualTo(2);
		assertThat(listener.getSummary().getTestsFailedCount()).isZero();
	}

	private static SummaryGeneratingListener execute(Class<?> testClass, boolean testClassScopeViaJUnitConfig,
			boolean deactivateDisabledCondition) {

		Launcher launcher = LauncherFactory.create();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(listener);
		var builder = request().selectors(selectClass(testClass));
		if (testClassScopeViaJUnitConfig) {
			builder.configurationParameter(SpringExtension.EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, "test_class");
		}
		if (deactivateDisabledCondition) {
			builder.configurationParameter(JUNIT_DISABLED_CONDITION_DEACTIVATE_PROPERTY, JUNIT_DISABLED_CONDITION_CLASS_NAME);
		}
		LauncherDiscoveryRequest request = builder.build();
		launcher.execute(request);
		return listener;
	}


	@SpringJUnitConfig
	static class BrokenScopeTestCase {

		@Test
		void test() {
			// never reached — invalid extension context scope fails the engine
		}
	}

}
