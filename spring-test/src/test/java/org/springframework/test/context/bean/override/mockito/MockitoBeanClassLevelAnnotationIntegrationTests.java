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

package org.springframework.test.context.bean.override.mockito;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBeanClassLevelAnnotationIntegrationTests.Service3;
import org.springframework.test.context.bean.override.mockito.MockitoBeanClassLevelAnnotationIntegrationTests.SharedMocks;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringJUnitConfig
@SharedMocks
@MockitoBean(types = Service3.class)
class MockitoBeanClassLevelAnnotationIntegrationTests {

	@Autowired
	Service1 service1;

	@Autowired
	Service2 service2;

	@Autowired
	Service3 service3;

	@MockitoBean
	Service4 service4;


	@BeforeEach
	void configureMocks() {
		given(service1.greeting()).willReturn("mock 1");
		given(service2.greeting()).willReturn("mock 2");
		given(service3.greeting()).willReturn("mock 3");
		given(service4.greeting()).willReturn("mock 4");
	}

	@Test
	void checkMocks() {
		assertThat(service1.greeting()).isEqualTo("mock 1");
		assertThat(service2.greeting()).isEqualTo("mock 2");
		assertThat(service3.greeting()).isEqualTo("mock 3");
		assertThat(service4.greeting()).isEqualTo("mock 4");
	}


	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@MockitoBean(types = {Service1.class, Service2.class})
	@interface SharedMocks {
	}

	interface Service1 {
		String greeting();
	}

	interface Service2 {
		String greeting();
	}

	interface Service3 {
		String greeting();
	}

	interface Service4 {
		String greeting();
	}

}
