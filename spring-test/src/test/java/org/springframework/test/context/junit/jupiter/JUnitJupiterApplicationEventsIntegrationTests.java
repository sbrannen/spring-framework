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

package org.springframework.test.context.junit.jupiter;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.AfterTestExecutionEvent;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.BeforeTestExecutionEvent;
import org.springframework.test.context.event.PrepareTestInstanceEvent;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationEvents} in conjunction with JUnit Jupiter.
 *
 * @author Sam Brannen
 * @since 5.3.2
 */
@SpringJUnitConfig
@RecordApplicationEvents
// @TestInstance(Lifecycle.PER_CLASS)
class JUnitJupiterApplicationEventsIntegrationTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	ApplicationEvents applicationEvents;


	// TODO Consider supporting dependency injection of ApplicationEvents in constructors.
	// @Autowired
	// JUnitJupiterApplicationEventsIntegrationTests(ApplicationEvents applicationEvents) {
	//     this.applicationEvents = applicationEvents;
	// }


	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		assertThat(applicationEvents).isNotNull();
		assertThat(applicationEvents.stream()).hasSize(2);
		assertThat(applicationEvents.stream(PrepareTestInstanceEvent.class)).hasSize(1);
		context.publishEvent(new CustomEvent(testInfo, "beforeEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(1);
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach");
		assertThat(applicationEvents.stream()).hasSize(3);
	}

	@Test
	void test1(@Autowired ApplicationEvents events, TestInfo testInfo) {
		assertThat(events).isNotNull();
		assertThat(events.stream()).hasSize(4);
		assertThat(events.stream(BeforeTestExecutionEvent.class)).hasSize(1);
		assertThat(events.stream(CustomEvent.class)).hasSize(1);
		context.publishEvent(new CustomEvent(testInfo, "test"));
		assertThat(events.stream(CustomEvent.class)).hasSize(2);
		assertThat(events.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", "test");
		assertThat(events.stream()).hasSize(5);
	}

	@Test
	void test2(@Autowired ApplicationEvents events, TestInfo testInfo) {
		assertThat(events).isNotNull();
		assertThat(events.stream()).hasSize(4);
		assertThat(events.stream(BeforeTestExecutionEvent.class)).hasSize(1);
		assertThat(events.stream(CustomEvent.class)).hasSize(1);
		context.publishEvent(new CustomEvent(testInfo, "test"));
		assertThat(events.stream(CustomEvent.class)).hasSize(2);
		assertThat(events.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", "test");
		assertThat(events.stream()).hasSize(5);
	}

	@AfterEach
	void afterEach(@Autowired ApplicationEvents events, TestInfo testInfo) {
		assertThat(events).isNotNull();
		assertThat(events.stream()).hasSize(6);
		assertThat(events.stream(AfterTestExecutionEvent.class)).hasSize(1);
		assertThat(events.stream(CustomEvent.class)).hasSize(2);
		context.publishEvent(new CustomEvent(testInfo, "afterEach"));
		assertThat(events.stream(CustomEvent.class)).hasSize(3);
		assertThat(events.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", "test", "afterEach");
		assertThat(events.stream()).hasSize(7);
	}


	@Configuration
	static class Config {
	}

	@SuppressWarnings("serial")
	static class CustomEvent extends ApplicationEvent {

		private final Method testMethod;
		private final String message;


		CustomEvent(TestInfo testInfo, String message) {
			super(testInfo.getTestClass().get());
			this.testMethod = testInfo.getTestMethod().get();
			this.message = message;
		}

		String getTestName() {
			return this.testMethod.getName();
		}

		String getMessage() {
			return message;
		}
	}

}
