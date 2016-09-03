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

package org.springframework.test.context.junit4.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.cache.ContextCacheTestUtils;

/**
 * Tests concurrency of the {@link TestContextManager}.
 *
 * @author <a href="mailto:kristian@zeniorD0Tno">Kristian Rosenvold</a>
 */
public class TestContextManagerConcurrencyRunnerTest {

	// This instance is shared by several threads in code (used from
	// SpringJUnit4ClassRunner)
	final TestContextManager testContextManager = new TestContextManager(
		SpringJUnit4ClassRunnerEnvironmentAssumptionsTest.class);


	class MyCallable implements Callable<SpringJUnit4ClassRunnerEnvironmentAssumptionsTest> {

		public SpringJUnit4ClassRunnerEnvironmentAssumptionsTest call() throws Exception {
			SpringJUnit4ClassRunnerEnvironmentAssumptionsTest instance = new SpringJUnit4ClassRunnerEnvironmentAssumptionsTest();
			testContextManager.prepareTestInstance(instance);
			return instance;
		}
	}


	@Test
	public void testConcurrentConstruction() throws Exception {
		Collection<Callable<SpringJUnit4ClassRunnerEnvironmentAssumptionsTest>> callableList = new ArrayList<Callable<SpringJUnit4ClassRunnerEnvironmentAssumptionsTest>>();
		for (int i = 0; i < 1000; i++) {
			callableList.add(new MyCallable());
		}

		ContextCacheTestUtils.resetContextCache();

		List<Future<SpringJUnit4ClassRunnerEnvironmentAssumptionsTest>> futures = Executors.newCachedThreadPool().invokeAll(
			callableList);
		for (Future<SpringJUnit4ClassRunnerEnvironmentAssumptionsTest> future : futures) {
			future.get().testClient1();
		}
	}

}
