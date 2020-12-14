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

import java.io.Serializable;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.Assert;

/**
 * {@code TestExecutionListener} which provides support for {@link ApplicationEvents}.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
public class ApplicationEventsTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Returns {@code 1800}.
	 */
	@Override
	public final int getOrder() {
		return 1800;
	}

	@Override
	@SuppressWarnings("resource")
	public void prepareTestInstance(TestContext testContext) throws Exception {
		if (recordingApplicationEvents(testContext)) {
			getThreadBoundApplicationListener(testContext).registerApplicationEvents();
			ApplicationContext applicationContext = testContext.getApplicationContext();
			Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
					"The ApplicationContext for the test must be a ConfigurableApplicationContext");
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
			ConfigurableListableBeanFactory beanFactory = cac.getBeanFactory();
			Scope testScope = beanFactory.getRegisteredScope("test");
			testScope.remove(ScopedProxyUtils.getTargetBeanName(ApplicationEvents.class.getName()));
		}
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (recordingApplicationEvents(testContext)) {
			// Register a new ApplicationEvents instance for the current thread?
			// This is necessary if the test instance is shared -- for example,
			// in TestNG or JUnit Jupiter with @TestInstance(PER_CLASS) semantics.
			// getThreadBoundApplicationListener(testContext).registerApplicationEventsIfNecessary();
		}
	}

	@Override
	@SuppressWarnings("resource")
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (recordingApplicationEvents(testContext)) {
			getThreadBoundApplicationListener(testContext).unregisterApplicationEvents();
			ApplicationContext applicationContext = testContext.getApplicationContext();
			Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
					"The ApplicationContext for the test must be a ConfigurableApplicationContext");
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
			ConfigurableListableBeanFactory beanFactory = cac.getBeanFactory();
			Scope testScope = beanFactory.getRegisteredScope("test");
			testScope.remove(ScopedProxyUtils.getTargetBeanName(ApplicationEvents.class.getName()));
		}
	}

	private boolean recordingApplicationEvents(TestContext testContext) {
		return TestContextAnnotationUtils.hasAnnotation(testContext.getTestClass(), RecordApplicationEvents.class);
	}

	@SuppressWarnings("resource")
	private ThreadBoundApplicationListener getThreadBoundApplicationListener(TestContext testContext) {
		ApplicationContext applicationContext = testContext.getApplicationContext();
		String beanName = ThreadBoundApplicationListener.class.getName();

		if (applicationContext.containsBean(beanName)) {
			return applicationContext.getBean(beanName, ThreadBoundApplicationListener.class);
		}

		// Register custom "test" scope.
		Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
				"The ApplicationContext for the test must be a ConfigurableApplicationContext");
		ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
		ConfigurableListableBeanFactory beanFactory = cac.getBeanFactory();
		beanFactory.registerScope("test", new SimpleThreadScope());

		Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory,
				"The BeanFactory for the test must be a BeanDefinitionRegistry");
		BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(DefaultApplicationEvents.class);
		beanDefinition.setScope("test");

		BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, ApplicationEvents.class.getName());
		beanDefinitionHolder = ScopedProxyUtils.createScopedProxy(beanDefinitionHolder, beanDefinitionRegistry, true);
		BeanDefinitionReaderUtils.registerBeanDefinition(beanDefinitionHolder, beanDefinitionRegistry);

		// Create and register a new ThreadBoundApplicationListener.
		ThreadBoundApplicationListener threadBoundApplicationListener =
				new ThreadBoundApplicationListener(applicationContext.getBean(DefaultApplicationEvents.class));
		beanFactory.registerSingleton(beanName, threadBoundApplicationListener);
		cac.addApplicationListener(threadBoundApplicationListener);

		// Register ApplicationEvents as a resolvable dependency for @Autowired support in test classes.
//		beanFactory.registerResolvableDependency(ApplicationEvents.class,
//				new ApplicationEventsObjectFactory(threadBoundApplicationListener));

		return threadBoundApplicationListener;
	}

	/**
	 * Factory that exposes the current {@link ApplicationEvents} object on demand.
	 */
	@SuppressWarnings("serial")
	private static class ApplicationEventsObjectFactory implements ObjectFactory<ApplicationEvents>, Serializable {

		private final ThreadBoundApplicationListener threadBoundApplicationListener;


		ApplicationEventsObjectFactory(ThreadBoundApplicationListener threadBoundApplicationListener) {
			this.threadBoundApplicationListener = threadBoundApplicationListener;
		}

		@Override
		public ApplicationEvents getObject() {
			ApplicationEvents applicationEvents = this.threadBoundApplicationListener.getApplicationEvents();
			if (applicationEvents != null) {
				return applicationEvents;
			}
			throw new IllegalStateException("Failed to retrieve ApplicationEvents for the current test");
		}

		@Override
		public String toString() {
			return "Current ApplicationEvents";
		}
	}

}
