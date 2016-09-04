/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.notification.RunNotifier;

import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.cache.ContextCacheTestUtils;
import org.springframework.test.context.junit4.TrackingRunListener;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.junit.Assert.assertEquals;

/**
 * Integration tests that verify support for concurrency in the Spring
 * TestContext Framework.
 *
 * @author Kristian Rosenvold
 * @author Sam Brannen
 * @since 5.0
 */
public class TestContextFrameworkConcurrencyTests {

	private static final Class<?> testClass = SpringRunnerEnvironmentAssumptionsTests.class;

	/**
	 * This instance is shared by several threads to simulate real life usage by
	 * the {@code SpringRunner}, {@code SpringClassRule}, etc.
	 */
	final TestContextManager testContextManager = new TestContextManager(testClass);

	final CountDownLatch startGate = new CountDownLatch(1);


	@Before
	public void resetContextCache() {
		ContextCacheTestUtils.resetContextCache();
	}

	/**
	 * Tests support for concurrency in the {@link TestContextManager} by executing
	 * {@link TestContextManager#prepareTestInstance prepareTestInstance()}
	 * multiple times in parallel.
	 */
	@Test
	public void prepareTestInstanceConcurrently() throws Exception {
		final int NUM_RUNS = 1000; // Fails ~80% of the time on c2d

		ExecutorService executorService = Executors.newCachedThreadPool();
		List<Future<ClientTester>> futures = new ArrayList<>();
		for (int i = 0; i < NUM_RUNS; i++) {
			futures.add(executorService.submit(() -> {
				ClientTester instance = new SpringRunnerEnvironmentAssumptionsTests();
				startGate.await(); // let's all really start at the same time here.
				testContextManager.prepareTestInstance(instance);
				return instance;
			}));
		}

		startGate.countDown();

		for (Future<ClientTester> future : futures) {
			future.get(3, TimeUnit.SECONDS).testClient();
		}
	}

	@Test
	public void runTestClassesConcurrently() throws Exception {
		final int NUM_RUNS = 50; // Fails ~95% of the time on c2d
		final int NUM_TESTS = 5;

		RunNotifier runNotifier = new RunNotifier();
		TrackingRunListener listener = new TrackingRunListener();
		runNotifier.addListener(listener);

		List<JUnit4TestClassInvoker> invokers = rangeClosed(1, NUM_RUNS).mapToObj(
			i -> new JUnit4TestClassInvoker(testClass, runNotifier)).collect(toList());

		for (Future<Void> future : JUnit4TestClassInvoker.invokeAll(invokers)) {
			future.get(3, TimeUnit.SECONDS);
		}

		assertEquals("No tests should fail.", 0, listener.getTestFailureCount());
		assertEquals("All tests should finish.", NUM_RUNS * NUM_TESTS, listener.getTestFinishedCount());
	}

}
