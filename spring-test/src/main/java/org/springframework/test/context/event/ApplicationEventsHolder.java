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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Holder class to expose the application events published during the execution
 * of a test in the form of a thread-bound {@link ApplicationEvents} object.
 *
 * <p>{@code ApplicationEvents} are registered in this holder and managed by
 * the {@link ApplicationEventsTestExecutionListener}.
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 5.3.3
 * @see ApplicationEvents
 * @see RecordApplicationEvents
 */
public abstract class ApplicationEventsHolder {

	private static final ThreadLocal<DefaultApplicationEvents> applicationEvents = new ThreadLocal<>();


	private ApplicationEventsHolder() {
		// no-op to prevent instantiation on this holder class
	}


	/**
	 * Get the {@link ApplicationEvents} for the current thread.
	 * @return the current {@code ApplicationEvents}, or {@code null} if not registered
	 */
	@Nullable
	public static ApplicationEvents getApplicationEvents() {
		return applicationEvents.get();
	}

	/**
	 * Get the {@link ApplicationEvents} for the current thread.
	 * @return the current {@code ApplicationEvents}
	 * @throws IllegalStateException if an instance of {@code ApplicationEvents}
	 * has not been registered for the current thread
	 */
	@Nullable
	public static ApplicationEvents getRequiredApplicationEvents() {
		ApplicationEvents events = applicationEvents.get();
		Assert.state(events != null, "Failed to retrieve ApplicationEvents for the current thread. " +
				"Ensure that your test class is annotated with @RecordApplicationEvents " +
				"and that the ApplicationEventsTestExecutionListener is registered.");
		return events;
	}


	/**
	 * Register a new {@link DefaultApplicationEvents} instance to be used for the
	 * current thread, if necessary.
	 * <p>If {@link #registerApplicationEvents()} has already been called for the
	 * current thread, this method does not do anything.
	 */
	static void registerApplicationEventsIfNecessary() {
		if (getApplicationEvents() == null) {
			registerApplicationEvents();
		}
	}

	/**
	 * Register a new {@link DefaultApplicationEvents} instance to be used for the
	 * current thread.
	 */
	static void registerApplicationEvents() {
		applicationEvents.set(new DefaultApplicationEvents());
	}

	/**
	 * Remove the registration of the {@link ApplicationEvents} for the current thread.
	 */
	static void unregisterApplicationEvents() {
		applicationEvents.remove();
	}

}
