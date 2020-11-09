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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.util.Assert;

/**
 * Extension for JUnit Jupiter that provides an instance of {@link ApplicationEvents}
 * as an argument to test class constructors, test methods, and test lifecycle methods.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 5.3.1
 */
public class ApplicationEventsExtension implements ParameterResolver, BeforeAllCallback, AfterEachCallback {

	private final ThreadBoundApplicationListenerAdapter listener = new ThreadBoundApplicationListenerAdapter();

	private final Function<ExtensionContext, ApplicationContext> contextProvider;


	public ApplicationEventsExtension() {
		this(SpringExtension::getApplicationContext);
	}

	protected ApplicationEventsExtension(Function<ExtensionContext, ApplicationContext> contextProvider) {
		this.contextProvider = contextProvider;
	}

	@Override
	public void beforeAll(ExtensionContext extensionContext) {
		ApplicationContext context = this.contextProvider.apply(extensionContext);
		Assert.isInstanceOf(ConfigurableApplicationContext.class, context, () -> String.format(
				"The ApplicationContext loaded for test class %s must be a ConfigurableApplicationContext.",
				extensionContext.getRequiredTestClass().getName()));
		((ConfigurableApplicationContext) context).addApplicationListener(this.listener);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return ApplicationEvents.class.isAssignableFrom(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return createApplicationEvents(parameterContext, extensionContext);
	}

	/**
	 * Create an instance of {@link ApplicationEvents} and register an
	 * {@link ApplicationListener} for it in the test's {@link ApplicationContext}.
	 * <p>Note that the instance returned by this method may be cached in the
	 * {@link ExtensionContext}'s {@link Store}.
	 * @param parameterContext the context for the parameter for which an argument
	 * should be resolved; never {@code null}
	 * @param extensionContext the extension context for the {@code Executable}
	 * about to be invoked; never {@code null}
	 * @return the newly created {@code ApplicationEvents} instance
	 */
	protected DefaultApplicationEvents createApplicationEvents(ParameterContext parameterContext,
			ExtensionContext extensionContext) {

		return getStore(extensionContext).getOrComputeIfAbsent(DefaultApplicationEvents.class, key -> {
				DefaultApplicationEvents events = new DefaultApplicationEvents();
				this.listener.registerDelegate(events);
				return events;
			}, DefaultApplicationEvents.class);
	}

	private Store getStore(ExtensionContext extensionContext) {
		return extensionContext.getStore(
			Namespace.create(ApplicationEventsExtension.class, extensionContext.getRequiredTestClass()));
	}

	@Override
	public void afterEach(ExtensionContext context) {
		this.listener.unregisterDelegate();
	}


	/**
	 * {@link ApplicationListener} that supports registration of delegate
	 * {@link ApplicationListener}s that are held in a {@link ThreadLocal} and
	 * get used in conjunction with {@link #onApplicationEvent(ApplicationEvent)}
	 * if one is registered for the current thread. This allows multiple event
	 * listeners to see the events fired in a certain thread in a concurrent
	 * execution scenario.
	 */
	private static class ThreadBoundApplicationListenerAdapter implements ApplicationListener<ApplicationEvent> {

		private final ThreadLocal<ApplicationListener<ApplicationEvent>> delegate = new ThreadLocal<>();


		/**
		 * Register the given {@link ApplicationListener} to be used for the
		 * current thread.
		 * @param listener the listener to register; never {@code null}
		 */
		void registerDelegate(ApplicationListener<ApplicationEvent> listener) {
			Assert.notNull(listener, "Delegate ApplicationListener must not be null");
			this.delegate.set(listener);
		}

		/**
		 * Remove the registration of the current delegate {@link ApplicationListener}.
		 */
		void unregisterDelegate() {
			this.delegate.remove();
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			ApplicationListener<ApplicationEvent> listener = this.delegate.get();
			if (listener != null) {
				listener.onApplicationEvent(event);
			}
		}
	}

	/**
	 * Default implementation of {@link ApplicationEvents}.
	 */
	private static class DefaultApplicationEvents implements ApplicationEvents, ApplicationListener<ApplicationEvent> {

		private final List<ApplicationEvent> events = new ArrayList<>();


		@Override
		public Stream<ApplicationEvent> stream() {
			return this.events.stream();
		}

		@Override
		public <T> Stream<T> stream(Class<T> type) {
			return this.events.stream()
					.map(this::unwrapPayloadEvent)
					.filter(type::isInstance)
					.map(type::cast);
		}

		private Object unwrapPayloadEvent(Object source) {
			return (PayloadApplicationEvent.class.isInstance(source) ?
					((PayloadApplicationEvent<?>) source).getPayload() : source);
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			this.events.add(event);
		}
	}

}
