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

package org.springframework.test.context.event;

import java.util.stream.Stream;

import org.springframework.context.ApplicationEvent;

/**
 * {@code ApplicationEvents} encapsulates all {@linkplain ApplicationEvent
 * application events} that were fired during the execution of a single test
 * method.
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 5.3.3
 * @see org.springframework.test.context.junit.jupiter.ApplicationEventsExtension
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.ApplicationListener
 */
public interface ApplicationEvents {

	/**
	 * Stream all application events that were fired during test execution.
	 * @return a stream of all application events
	 * @see #stream(Class)
	 * @see #clear()
	 */
	Stream<ApplicationEvent> stream();

	/**
	 * Stream all application events or event payloads of the given type that
	 * were fired during test execution.
	 * @param <T> the event type
	 * @param type the type of events or payloads to stream; never {@code null}
	 * @return a stream of all application events or event payloads of the
	 * specified type
	 * @see #stream()
	 * @see #clear()
	 */
	<T> Stream<T> stream(Class<T> type);

	/**
	 * Clear all application events recorded by this {@code ApplicationEvents} instance.
	 * <p>Subsequent calls to {@link #stream()} or {@link #stream(Class)} will
	 * only include events recorded since this method was invoked.
	 * @see #stream()
	 * @see #stream(Class)
	 */
	void clear();

}
