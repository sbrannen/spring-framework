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
import org.springframework.util.Assert;

/**
 * {@link ApplicationListener} that supports registration of a delegate
 * {@code ApplicationListener} that is held in a {@link ThreadLocal} and
 * used in conjunction with {@link #onApplicationEvent(ApplicationEvent)} if one
 * is registered for the current thread.
 *
 * <p>This allows multiple event listeners to see the events fired in a certain
 * thread in a concurrent execution scenario.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 5.3.2
 */
public class ThreadBoundApplicationListenerAdapter implements ApplicationListener<ApplicationEvent> {

	private final ThreadLocal<ApplicationListener<ApplicationEvent>> delegateHolder = new ThreadLocal<>();


	/**
	 * Register the given delegate {@link ApplicationListener} to be used for the
	 * current thread.
	 * @param listener the listener to register; never {@code null}
	 */
	public void registerDelegate(ApplicationListener<ApplicationEvent> listener) {
		Assert.notNull(listener, "Delegate ApplicationListener must not be null");
		this.delegateHolder.set(listener);
	}

	@Nullable
	public ApplicationListener<ApplicationEvent> getDelegate() {
		return this.delegateHolder.get();
	}

	/**
	 * Remove the registration of the current delegate {@link ApplicationListener}.
	 */
	public void unregisterDelegate() {
		this.delegateHolder.remove();
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		ApplicationListener<ApplicationEvent> delegate = this.delegateHolder.get();
		if (delegate != null) {
			delegate.onApplicationEvent(event);
		}
	}

}
