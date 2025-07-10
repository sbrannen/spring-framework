/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextRestartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.TestContextEvent;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.ContextStateTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests for the {@link ContextStateTestExecutionListener} and
 * associated infrastructure.
 *
 * @author Sam Brannen
 * @since 7.0
 */
class InactiveContextsTests {

	private static final List<String> applicationEvents = new ArrayList<>();


	@BeforeEach
	@AfterEach
	void clearApplicationEvents() {
		applicationEvents.clear();
	}

	@Test
	void topLevelTestClassesWithSharedApplicationContext() {
		runTestClasses(5, TestCase1.class, TestCase2.class, TestCase3.class, TestCase4.class, TestCase5.class);

		assertThat(applicationEvents).containsExactly(

				// --- TestCase1 -----------------------------------------------
				// Refreshed instead of Restarted, since this is the first time
				// the context is loaded.
				"ContextRefreshed",
				// No BeforeTestClass, since EventPublishingTestExecutionListener
				// only publishes events for a context that has already been loaded.
				"AfterTestClass:TestCase1",
				"ContextStopped",

				// --- TestCase2 -----------------------------------------------
				"ContextRestarted",
				"BeforeTestClass:TestCase2",
				"AfterTestClass:TestCase2",
				"ContextStopped",

				// --- TestCase3 -----------------------------------------------
				"ContextRestarted",
				"BeforeTestClass:TestCase3",
				"AfterTestClass:TestCase3",
				// Closed instead of Stopped, since TestCase3 uses @DirtiesContext
				"ContextClosed",

				// --- TestCase4 -----------------------------------------------
				// Refreshed instead of Restarted, since TestCase3 uses @DirtiesContext
				"ContextRefreshed",
				// No BeforeTestClass, since EventPublishingTestExecutionListener
				// only publishes events for a context that has already been loaded.
				"AfterTestClass:TestCase4",
				"ContextStopped",

				// --- TestCase5 -----------------------------------------------
				"ContextRestarted",
				"BeforeTestClass:TestCase5",
				"AfterTestClass:TestCase5",
				"ContextStopped"
		);
	}

	@Test
	void testClassesInNestedTestHierarchy() {
		runTestClasses(5, EnclosingTestCase.class);

		assertThat(applicationEvents).containsExactly(

			// --- EnclosingTestCase -------------------------------------------
			"ContextRefreshed",
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.

				// --- NestedTestCase ------------------------------------------
				// No Refreshed or Restarted event, since NestedTestCase shares the
				// active context used by EnclosingTestCase.
				"BeforeTestClass:NestedTestCase",

					// --- OverridingNestedTestCase1 ---------------------------
					"ContextRefreshed",
					// No BeforeTestClass, since EventPublishingTestExecutionListener
					// only publishes events for a context that has already been loaded.

						// --- InheritingNestedTestCase ------------------------
						// No Refreshed or Restarted event, since InheritingNestedTestCase
						// shares the active context used by OverridingNestedTestCase1.
						"BeforeTestClass:InheritingNestedTestCase",
						"AfterTestClass:InheritingNestedTestCase",
						// No Stopped event, since OverridingNestedTestCase1 is still
						// using the context

					"AfterTestClass:OverridingNestedTestCase1",
					"ContextStopped",

					// --- OverridingNestedTestCase2 ---------------------------
					"ContextRestarted",
					"BeforeTestClass:OverridingNestedTestCase2",
					"AfterTestClass:OverridingNestedTestCase2",
					"ContextStopped",

				"AfterTestClass:NestedTestCase",
				// No Stopped event, since EnclosingTestCase is still using the context

			"AfterTestClass:EnclosingTestCase",
			"ContextStopped"
		);
	}


	private static void runTestClasses(int expectedTestCount, Class<?>... classes) {
		EngineTestKit.engine("junit-jupiter")
			.selectors(selectClasses(classes))
			.execute()
			.testEvents()
			.assertStatistics(stats -> stats.started(expectedTestCount).succeeded(expectedTestCount));
	}

	private static ClassSelector[] selectClasses(Class<?>... classes) {
		return Arrays.stream(classes).map(DiscoverySelectors::selectClass).toArray(ClassSelector[]::new);
	}


	@SpringJUnitConfig(EventTracker.class)
	private abstract static class AbstractTestCase {

		@Test
		void test() {
			// no-op
		}
	}

	static class TestCase1 extends AbstractTestCase {
	}

	static class TestCase2 extends AbstractTestCase {
	}

	@DirtiesContext
	static class TestCase3 extends AbstractTestCase {
	}

	static class TestCase4 extends AbstractTestCase {
	}

	static class TestCase5 extends AbstractTestCase {
	}

	@SpringJUnitConfig(EventTracker.class)
	@TestPropertySource(properties = "magicKey = puzzle")
	static class EnclosingTestCase {

		@Test
		void test(@Value("${magicKey}") String magicKey) {
			assertThat(magicKey).isEqualTo("puzzle");
		}

		@Nested
		@TestClassOrder(ClassOrderer.OrderAnnotation.class)
		class NestedTestCase {

			@Test
			void test(@Value("${magicKey}") String magicKey) {
				assertThat(magicKey).isEqualTo("puzzle");
			}

			/**
			 * Duplicates configuration of {@link OverridingNestedTestCase2}.
			 */
			@Nested
			@Order(1)
			@NestedTestConfiguration(OVERRIDE)
			@SpringJUnitConfig(EventTracker.class)
			@TestPropertySource(properties = "magicKey = enigma")
			class OverridingNestedTestCase1 {

				@Test
				void test(@Value("${magicKey}") String magicKey) {
					assertThat(magicKey).isEqualTo("enigma");
				}

				@Nested
				@NestedTestConfiguration(INHERIT)
				class InheritingNestedTestCase {

					@Test
					void test(@Value("${magicKey}") String magicKey) {
						assertThat(magicKey).isEqualTo("enigma");
					}
				}
			}

			/**
			 * Duplicates configuration of {@link OverridingNestedTestCase1}.
			 */
			@Nested
			@Order(2)
			@NestedTestConfiguration(OVERRIDE)
			@SpringJUnitConfig(EventTracker.class)
			@TestPropertySource(properties = "magicKey = enigma")
			class OverridingNestedTestCase2 {

				@Test
				void test(@Value("${magicKey}") String magicKey) {
					assertThat(magicKey).isEqualTo("enigma");
				}
			}
		}
	}

	@Component
	static class EventTracker {

		@EventListener(ContextRefreshedEvent.class)
		void contextRefreshed(ContextRefreshedEvent event) {
			trackApplicationEvent(event);
		}

		@EventListener(ContextRestartedEvent.class)
		void contextRestarted(ContextRestartedEvent event) {
			trackApplicationEvent(event);
		}

		@EventListener(ContextStoppedEvent.class)
		void contextStopped(ContextStoppedEvent event) {
			trackApplicationEvent(event);
		}

		@EventListener(ContextClosedEvent.class)
		void contextClosed(ContextClosedEvent event) {
			trackApplicationEvent(event);
		}

		@BeforeTestClass
		void beforeTestClass(TestContextEvent event) {
			trackTestContextEvent(event);
		}

		@AfterTestClass
		void afterTestClass(TestContextEvent event) {
			trackTestContextEvent(event);
		}

		private static void trackApplicationEvent(ApplicationEvent event) {
			applicationEvents.add(eventName(event));
		}

		private static void trackTestContextEvent(TestContextEvent event) {
			applicationEvents.add(eventName(event) + ":" + event.getTestContext().getTestClass().getSimpleName());
		}

		private static String eventName(ApplicationEvent event) {
			String name = event.getClass().getSimpleName();
			return name.substring(0, name.length() - "Event".length());
		}
	}

}
