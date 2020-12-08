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

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.Nullable;

/**
 * {@link ApplicationListener} that supports registration of a
 * {@link DefaultApplicationEvents} instance that is held in a {@link ThreadLocal}
 * and delegated to in {@link #onApplicationEvent(ApplicationEvent)}.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 5.3.3
 */
class ThreadBoundApplicationListener implements ApplicationListener<ApplicationEvent> {

	private final ThreadLocal<DefaultApplicationEvents> applicationEvents = new ThreadLocal<>();


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		DefaultApplicationEvents applicationEvents = this.applicationEvents.get();
		if (applicationEvents != null) {
			applicationEvents.addEvent(event);
		}
	}

	/**
	 * Register a new {@link DefaultApplicationEvents} instance to be used for the
	 * current thread, if necessary.
	 * <p>If {@link #registerApplicationEvents()} has already been called for the
	 * current thread, this method does not do anything.
	 */
	void registerApplicationEventsIfNecessary() {
		if (getApplicationEvents() == null) {
			registerApplicationEvents();
		}
	}

	/**
	 * Register a new {@link DefaultApplicationEvents} instance to be used for the
	 * current thread.
	 */
	void registerApplicationEvents() {
		this.applicationEvents.set(new DefaultApplicationEvents());
	}

	/**
	 * Get the {@link ApplicationEvents} for the current thread.
	 * @return the current {@code ApplicationEvents}, or {@code null} if not registered
	 */
	@Nullable
	ApplicationEvents getApplicationEvents() {
		return this.applicationEvents.get();
	}

	/**
	 * Remove the registration of the {@link DefaultApplicationEvents} for the
	 * current thread.
	 */
	void unregisterApplicationEvents() {
		this.applicationEvents.remove();
	}

}
