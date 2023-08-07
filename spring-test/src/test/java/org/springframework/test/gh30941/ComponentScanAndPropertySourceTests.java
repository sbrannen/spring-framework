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

package org.springframework.test.gh30941;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.gh30941.a.ComponentA;
import org.springframework.test.gh30941.b.ComponentB;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(AppConfig.class)
class ComponentScanAndPropertySourceTests {

	@Autowired
	ComponentA componentA;

	@Autowired
	ComponentB componentB;

	@Autowired
	Environment env;

	@Test
	void test() {
		assertThat(componentA).isNotNull();
		assertThat(componentB).isNotNull();
		assertThat(env.getProperty("A")).isEqualTo("apple");
		assertThat(env.getProperty("B")).isEqualTo("banana");
	}

}
