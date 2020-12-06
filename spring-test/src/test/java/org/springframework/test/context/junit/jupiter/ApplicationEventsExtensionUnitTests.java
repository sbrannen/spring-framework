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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ApplicationEventsExtension}.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 5.3.2
 */
class ApplicationEventsExtensionUnitTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private final ApplicationEventsExtension extension = new ApplicationEventsExtension(__ -> context);


	@Test
	void supportsApplicationEventsType() {
		assertThat(extension.supportsParameter(createParameterContext(ApplicationEvents.class), null)).isTrue();
		assertThat(extension.supportsParameter(createParameterContext(Object.class), null)).isFalse();
	}

	@Test
	@Disabled("Unit testing no longer possible with null ExtensionContext due to reliance on the ExtensionContext.Store")
	void createsThreadBoundApplicationEvents() throws Exception {
		context.refresh();

		// extension.beforeAll(null);

		Map<String, ApplicationEvents> allEvents = new ConcurrentHashMap<>();
		List<String> keys = Arrays.asList("first", "second", "third");
		CountDownLatch latch = new CountDownLatch(3);

		for (String key : keys) {
			new Thread(() -> {
				ApplicationEvents events = (ApplicationEvents) extension.resolveParameter(null, null);
				context.publishEvent(key);
				allEvents.put(key, events);

				// extension.afterEach(null);

				latch.countDown();
			}).start();
		}

		latch.await(50, TimeUnit.MILLISECONDS);

		keys.forEach(key -> assertThat(allEvents.get(key).stream(String.class)).containsExactly(key));
	}

	private static ParameterContext createParameterContext(Class<?> type) {
		Method method = ReflectionUtils.findMethod(Methods.class, "with", type);
		ParameterContext context = mock(ParameterContext.class);
		doReturn(method.getParameters()[0]).when(context).getParameter();
		return context;
	}

	interface Methods {

		void with(ApplicationEvents events);

		void with(Object object);
	}

}
