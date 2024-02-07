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

package org.springframework.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DynamicPropertyRegistry} bean support.
 *
 * @author Sam Brannen
 * @since 6.2
 * @see DynamicPropertySourceIntegrationTests
 */
@SpringJUnitConfig
@TestPropertySource(properties = "api.url: https://example.com/test")
class DynamicPropertyRegistryIntegrationTests {

	private static final String API_URL = "api.url";


	@Test
	void dynamicPropertySourceOverridesTestPropertySource(@Autowired ConfigurableEnvironment env) {
		MutablePropertySources propertySources = env.getPropertySources();
		assertThat(propertySources.size()).isGreaterThanOrEqualTo(4);
		assertThat(propertySources.contains("Inlined Test Properties")).isTrue();
		assertThat(propertySources.contains("Dynamic Test Properties")).isTrue();
		assertThat(propertySources.get("Inlined Test Properties").getProperty(API_URL)).isEqualTo("https://example.com/test");
		assertThat(propertySources.get("Dynamic Test Properties").getProperty(API_URL)).isEqualTo("https://example.com/dynamic");
		assertThat(env.getProperty(API_URL)).isEqualTo("https://example.com/dynamic");
	}

	@Test
	void environmentInjectedServiceCanRetrieveDynamicProperty(@Autowired EnvironmentInjectedService service) {
		assertThat(service.getApiUrl()).isEqualTo("https://example.com/dynamic");
	}

	@Test
	void dependentServiceReceivesDynamicProperty(@Autowired DependentService service) {
		assertThat(service.getApiUrl()).isEqualTo("https://example.com/dynamic");
	}

	@Test
	void valueInjectedServiceDoesNotReceiveDynamicProperty(@Autowired ValueInjectedService service) {
		assertThat(service.getApiUrl()).isEqualTo("https://example.com/test");
	}


	@Configuration
	@Import({ EnvironmentInjectedService.class, ValueInjectedService.class })
	static class Config {

		@Bean
		// DependentService#setApiUrl requires that the apiServer @Bean method has
		// already been invoked so that the dynamic "api.url" property is available.
		@DependsOn("apiServer")
		DependentService dependentService() {
			return new DependentService();
		}

		@Bean
		ApiServer apiServer(DynamicPropertyRegistry registry) {
			ApiServer apiServer = new ApiServer();
			registry.add("api.url", apiServer::getUrl);
			return apiServer;
		}

	}

	static class EnvironmentInjectedService {

		private final Environment env;

		EnvironmentInjectedService(Environment env) {
			this.env = env;
		}

		String getApiUrl() {
			return this.env.getProperty(API_URL);
		}
	}

	static class DependentService {

		private String apiUrl;


		@Autowired
		void setApiUrl(@Value("${api.url}") String apiUrl) {
			this.apiUrl = apiUrl;
		}

		String getApiUrl() {
			return this.apiUrl;
		}
	}

	static class ValueInjectedService {

		private final String apiUrl;


		ValueInjectedService(@Value("${api.url}") String apiUrl) {
			this.apiUrl = apiUrl;
		}

		String getApiUrl() {
			return this.apiUrl;
		}
	}

	static class ApiServer {

		String getUrl() {
			return "https://example.com/dynamic";
		}
	}

}
