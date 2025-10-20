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

package example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringJUnitWebConfig
class CachedControllerAndServiceTests {

	MockMvc mockMvc;


	@BeforeEach
	void setUpMockMvc(WebApplicationContext wac) {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}


	@Test
	void parentChildStandaloneControllerMethods() throws Exception {
		mockMvc.perform(get("/parent")).andExpect(content().string("Parent 1"));
		mockMvc.perform(get("/parent")).andExpect(content().string("Parent 1"));
		mockMvc.perform(get("/parent")).andExpect(content().string("Parent 1"));

		mockMvc.perform(get("/child")).andExpect(content().string("Child 1"));
		mockMvc.perform(get("/child")).andExpect(content().string("Child 1"));
		mockMvc.perform(get("/child")).andExpect(content().string("Child 1"));

		mockMvc.perform(get("/standalone")).andExpect(content().string("Standalone 1"));
		mockMvc.perform(get("/standalone")).andExpect(content().string("Standalone 1"));
		mockMvc.perform(get("/standalone")).andExpect(content().string("Standalone 1"));
	}

	@Test
	void childParentStandaloneControllerMethods() throws Exception {
		mockMvc.perform(get("/child")).andExpect(content().string("Child 1"));
		mockMvc.perform(get("/child")).andExpect(content().string("Child 1"));
		mockMvc.perform(get("/child")).andExpect(content().string("Child 1"));

		mockMvc.perform(get("/parent")).andExpect(content().string("Parent 1"));
		mockMvc.perform(get("/parent")).andExpect(content().string("Parent 1"));
		mockMvc.perform(get("/parent")).andExpect(content().string("Parent 1"));

		mockMvc.perform(get("/standalone")).andExpect(content().string("Standalone 1"));
		mockMvc.perform(get("/standalone")).andExpect(content().string("Standalone 1"));
		mockMvc.perform(get("/standalone")).andExpect(content().string("Standalone 1"));
	}

	@Test
	void parentChildStandaloneServiceMethods(@Autowired CachedService cachedService) {
		assertThat(cachedService.getParentCacheKey()).isEqualTo("Parent 1");
		assertThat(cachedService.getParentCacheKey()).isEqualTo("Parent 1");
		assertThat(cachedService.getParentCacheKey()).isEqualTo("Parent 1");

		assertThat(cachedService.getChildCacheKey()).isEqualTo("Child 1");
		assertThat(cachedService.getChildCacheKey()).isEqualTo("Child 1");
		assertThat(cachedService.getChildCacheKey()).isEqualTo("Child 1");

		assertThat(cachedService.getStandaloneCacheKey()).isEqualTo("Standalone 1");
		assertThat(cachedService.getStandaloneCacheKey()).isEqualTo("Standalone 1");
		assertThat(cachedService.getStandaloneCacheKey()).isEqualTo("Standalone 1");
	}

	@Test
	void childParentStandaloneServiceMethods(@Autowired CachedService cachedService) {
		assertThat(cachedService.getChildCacheKey()).isEqualTo("Child 1");
		assertThat(cachedService.getChildCacheKey()).isEqualTo("Child 1");
		assertThat(cachedService.getChildCacheKey()).isEqualTo("Child 1");

		assertThat(cachedService.getParentCacheKey()).isEqualTo("Parent 1");
		assertThat(cachedService.getParentCacheKey()).isEqualTo("Parent 1");
		assertThat(cachedService.getParentCacheKey()).isEqualTo("Parent 1");

		assertThat(cachedService.getStandaloneCacheKey()).isEqualTo("Standalone 1");
		assertThat(cachedService.getStandaloneCacheKey()).isEqualTo("Standalone 1");
		assertThat(cachedService.getStandaloneCacheKey()).isEqualTo("Standalone 1");
	}


	@Configuration
	@EnableWebMvc
	@EnableCaching
	@Import({CachedController.class, CachedService.class})
	static class Config {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}
	}

}
