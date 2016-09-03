/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.cache.ContextCacheTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests concurrency of the {@link TestContextManager} by executing
 * {@link SpringJUnit4ClassRunnerEnvironmentAssumptionsTests} multiple
 * times in parallel.
 *
 * @author <a href="mailto:kristian@zeniorD0Tno">Kristian Rosenvold</a>
 */
public class TestContextManagerConcurrencyRunnerTests {

	// This instance is shared by several threads in code (used from
	// SpringJUnit4ClassRunner)
	final TestContextManager testContextManager = new TestContextManager(
		SpringJUnit4ClassRunnerEnvironmentAssumptionsTests.class);

	final CountDownLatch startGate = new CountDownLatch(1);


	@Test
	public void testConcurrentPrepareTestInstance() throws Exception {
		List<Future<SpringJUnit4ClassRunnerEnvironmentAssumptionsTests>> futures = new ArrayList<Future<SpringJUnit4ClassRunnerEnvironmentAssumptionsTests>>();
		ExecutorService executorService = Executors.newCachedThreadPool();
		for (int i = 0; i < 1000; i++) { // Fails ~80% of the time on c2d
			futures.add(executorService.submit(new MySpringBeanBuilder()));
		}

		// ContextCleaner.clearTestContext();
		ContextCacheTestUtils.resetContextCache();

		startGate.countDown();
		for (Future<SpringJUnit4ClassRunnerEnvironmentAssumptionsTests> future : futures) {
			future.get().testClient1();
		}
	}


	class MySpringBeanBuilder implements Callable<SpringJUnit4ClassRunnerEnvironmentAssumptionsTests> {

		public SpringJUnit4ClassRunnerEnvironmentAssumptionsTests call() throws Exception {
			SpringJUnit4ClassRunnerEnvironmentAssumptionsTests instance = new SpringJUnit4ClassRunnerEnvironmentAssumptionsTests();
			startGate.await(); // let's all really start at the same time here.
			testContextManager.prepareTestInstance(instance);
			return instance;
		}
	}


	@Test
	public void testRunCounts() throws Exception {
		final int NUMTESTS = 50; // Fails ~95% of the time on c2d
		List<Callable<RunNotifier>> classes = new ArrayList<Callable<RunNotifier>>();

		// ContextCleaner.clearTestContext();
		ContextCacheTestUtils.resetContextCache();

		RunNotifier runNotifier = new RunNotifier();
		CountingRunListener listener = new CountingRunListener();
		runNotifier.addListener(listener);
		for (int i = 0; i < NUMTESTS; i++) {
			classes.add(
				new JunitTestClassInvoker(SpringJUnit4ClassRunnerEnvironmentAssumptionsTests.class, runNotifier));
		}
		List<Future<RunNotifier>> futures = JunitTestClassInvoker.runAll(classes);

		for (Future<RunNotifier> future : futures) {
			assertNotNull(future.get());
		}

		assertEquals("No tests should fail, right ?", 0, listener.getFailures());
		assertEquals("All tests should succeed, right ?", NUMTESTS * 5, listener.getSuccess());
	}


	class CountingRunListener extends RunListener {

		AtomicInteger failures = new AtomicInteger();
		AtomicInteger success = new AtomicInteger();
		AtomicInteger assumptionFailures = new AtomicInteger();


		@Override
		public void testFailure(Failure failure) throws Exception {
			failures.incrementAndGet();
			System.out.println("failure.getMessage() = " + failure.getMessage());
		}

		@Override
		public void testFinished(Description description) throws Exception {
			success.incrementAndGet();
		}

		@Override
		public void testAssumptionFailure(Failure failure) {
			assumptionFailures.incrementAndGet();
		}

		public int getFailures() {
			return failures.intValue();
		}

		public int getSuccess() {
			return success.intValue();
		}

		public int getAssumptionFailures() {
			return assumptionFailures.intValue();
		}
	}

}
