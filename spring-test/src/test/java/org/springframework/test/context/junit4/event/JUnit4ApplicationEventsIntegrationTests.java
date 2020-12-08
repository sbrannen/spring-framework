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

package org.springframework.test.context.junit4.event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.AfterTestExecutionEvent;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.BeforeTestExecutionEvent;
import org.springframework.test.context.event.PrepareTestInstanceEvent;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationEvents} in conjunction with JUnit 4.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
@RunWith(SpringRunner.class)
@RecordApplicationEvents
public class JUnit4ApplicationEventsIntegrationTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	ApplicationEvents applicationEvents;


	@Before
	public void beforeEach() {
		assertThat(applicationEvents).isNotNull();
		assertThat(applicationEvents.stream()).hasSize(2);
		assertThat(applicationEvents.stream(PrepareTestInstanceEvent.class)).hasSize(1);
		context.publishEvent(new CustomEvent("beforeEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(1);
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach");
		assertThat(applicationEvents.stream()).hasSize(3);
	}

	@Test
	public void test1() {
		assertThat(applicationEvents).isNotNull();
		assertThat(applicationEvents.stream()).hasSize(4);
		assertThat(applicationEvents.stream(BeforeTestExecutionEvent.class)).hasSize(1);
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(1);
		context.publishEvent(new CustomEvent("test"));
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(2);
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", "test");
		assertThat(applicationEvents.stream()).hasSize(5);
	}

	@Test
	public void test2() {
		assertThat(applicationEvents).isNotNull();
		assertThat(applicationEvents.stream()).hasSize(4);
		assertThat(applicationEvents.stream(BeforeTestExecutionEvent.class)).hasSize(1);
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(1);
		context.publishEvent(new CustomEvent("test"));
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(2);
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", "test");
		assertThat(applicationEvents.stream()).hasSize(5);
	}

	@After
	public void afterEach() {
		assertThat(applicationEvents).isNotNull();
		assertThat(applicationEvents.stream()).hasSize(6);
		assertThat(applicationEvents.stream(AfterTestExecutionEvent.class)).hasSize(1);
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(2);
		context.publishEvent(new CustomEvent("afterEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).hasSize(3);
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", "test", "afterEach");
		assertThat(applicationEvents.stream()).hasSize(7);
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
