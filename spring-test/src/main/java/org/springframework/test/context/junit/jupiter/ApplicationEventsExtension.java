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

import java.util.function.Function;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.Nullable;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.ThreadBoundApplicationListenerAdapter;

/**
 * Extension for JUnit Jupiter that provides an instance of {@link ApplicationEvents}
 * as an argument to test class constructors, test methods, and test lifecycle methods.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 5.3.2
 */
public class ApplicationEventsExtension implements ParameterResolver {

	private final Function<ExtensionContext, ApplicationContext> contextProvider;


	public ApplicationEventsExtension() {
		this(SpringExtension::getApplicationContext);
	}

	protected ApplicationEventsExtension(Function<ExtensionContext, ApplicationContext> contextProvider) {
		this.contextProvider = contextProvider;
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return ApplicationEvents.class.isAssignableFrom(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return getApplicationEvents(parameterContext, extensionContext);
	}

	/**
	 * Get the instance of {@link ApplicationEvents} from the test's {@link ApplicationContext}.
	 * @param parameterContext the context for the parameter for which an argument
	 * should be resolved; never {@code null}
	 * @param extensionContext the extension context for the {@code Executable}
	 * about to be invoked; never {@code null}
	 * @return the {@code ApplicationEvents} instance; potentially {@code null}
	 */
	@Nullable
	protected ApplicationEvents getApplicationEvents(ParameterContext parameterContext,
			ExtensionContext extensionContext) {

		ApplicationContext applicationContext = this.contextProvider.apply(extensionContext);
		ApplicationListener<ApplicationEvent> applicationListener =
				getThreadBoundApplicationListenerAdapter(applicationContext).getDelegate();
		if (applicationListener instanceof ApplicationEvents) {
			return (ApplicationEvents) applicationListener;
		}
		return null;
	}

	private ThreadBoundApplicationListenerAdapter getThreadBoundApplicationListenerAdapter(
			ApplicationContext applicationContext) {

		return applicationContext.getBean(ThreadBoundApplicationListenerAdapter.class.getName(),
			ThreadBoundApplicationListenerAdapter.class);
	}

}
