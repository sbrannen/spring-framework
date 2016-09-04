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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.runner.notification.RunNotifier;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Encapsulates the multi-threaded invocation of JUnit 4 test classes
 * run via the {@link SpringRunner}.
 *
 * @author Kristian Rosenvold
 * @author Sam Brannen
 * @since 5.0
 */
class JUnit4TestClassInvoker implements Callable<Void> {

	private final Class<?> testClass;
	private final RunNotifier runNotifier;


	JUnit4TestClassInvoker(Class<?> testClass, RunNotifier runNotifier) {
		this.testClass = testClass;
		this.runNotifier = runNotifier;
	}

	public Void call() throws Exception {
		new SpringRunner(this.testClass).run(this.runNotifier);
		return null;
	}

	static List<Future<Void>> invokeAll(Collection<JUnit4TestClassInvoker> testInvokers) {
		final ExecutorService executorService = Executors.newCachedThreadPool();
		try {
			return executorService.invokeAll(testInvokers);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		finally {
			executorService.shutdownNow();
		}
	}

}
