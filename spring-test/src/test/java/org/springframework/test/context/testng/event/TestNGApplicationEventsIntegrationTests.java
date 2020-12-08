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

package org.springframework.test.context.testng.event;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.AfterTestExecutionEvent;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.BeforeTestExecutionEvent;
import org.springframework.test.context.event.BeforeTestMethodEvent;
import org.springframework.test.context.event.PrepareTestInstanceEvent;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationEvents} in conjunction with TestNG.
 *
 * @author Sam Brannen
 * @since 5.3.2
 */
@RecordApplicationEvents
class TestNGApplicationEventsIntegrationTests extends AbstractTestNGSpringContextTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	ApplicationEvents applicationEvents;

	private boolean testAlreadyExecuted = false;
	private int count = 0;


	@BeforeMethod
	void beforeEach() {
		assertThat(applicationEvents).isNotNull();

		if (!testAlreadyExecuted) {
			assertThat(applicationEvents.stream(PrepareTestInstanceEvent.class)).hasSize(1);
			assertThat(applicationEvents.stream(BeforeTestMethodEvent.class)).hasSize(1);
			assertThat(applicationEvents.stream()).hasSize(count += 2);
		}
		else {
			assertThat(applicationEvents.stream(BeforeTestMethodEvent.class)).hasSize(1);
			assertThat(applicationEvents.stream()).hasSize(count += 1);
		}

		context.publishEvent(new CustomEvent("beforeEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(1);
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach");
		assertThat(applicationEvents.stream()).hasSize(count += 1);
	}

	@Test
	void test1() {
		assertTestExpectations();
	}

	@Test
	void test2() {
		assertTestExpectations();
	}

	private void assertTestExpectations() {
		assertThat(applicationEvents).isNotNull();
		assertThat(applicationEvents.stream()).hasSize(count += 1);
		assertThat(applicationEvents.stream(BeforeTestExecutionEvent.class)).hasSize(1);
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(1);
		context.publishEvent(new CustomEvent("test"));
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(2);
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", "test");
		assertThat(applicationEvents.stream()).hasSize(count += 1);
	}

	@AfterMethod
	void afterEach() {
		assertThat(applicationEvents).isNotNull();
		assertThat(applicationEvents.stream()).hasSize(count += 1);
		assertThat(applicationEvents.stream(AfterTestExecutionEvent.class)).hasSize(1);
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(2);
		context.publishEvent(new CustomEvent("afterEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(3);
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", "test", "afterEach");
		assertThat(applicationEvents.stream()).hasSize(count += 1);

		testAlreadyExecuted = true;
		count = 0;
	}


	@Configuration
	static class Config {
	}

	@SuppressWarnings("serial")
	static class CustomEvent extends ApplicationEvent {

		private final String message;


		CustomEvent(String message) {
			super(message);
			this.message = message;
		}

		String getMessage() {
			return message;
		}
	}

}
