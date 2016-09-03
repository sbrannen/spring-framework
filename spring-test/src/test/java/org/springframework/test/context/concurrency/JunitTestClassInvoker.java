
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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Encapsulates the threaded running of tests from here.
 * @author <a href="mailto:kristian@zeniorD0Tno">Kristian Rosenvold</a>
 */
public class JunitTestClassInvoker implements Callable<RunNotifier> {

	private final Class<?> classTorun;
	private final RunNotifier runNotifier;


	public JunitTestClassInvoker(Class<?> classtoRun, RunNotifier runNotifier) {
		this.classTorun = classtoRun;
		this.runNotifier = runNotifier;
	}

	public RunNotifier call() throws Exception {
		MyClassRunner myClassRunner = new MyClassRunner(classTorun);
		myClassRunner.run(runNotifier);
		return runNotifier;
	}

	public static List<Future<RunNotifier>> runAll(Collection<Callable<RunNotifier>> invokerEnumerable) {
		final ExecutorService executorService = Executors.newCachedThreadPool();
		try {
			return executorService.invokeAll(invokerEnumerable);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		finally {
			executorService.shutdown();
		}
	}


	class MyClassRunner extends SpringJUnit4ClassRunner {

		MyClassRunner(Class<?> aClass) throws InitializationError {
			super(aClass);
		}

		public TestContextManager getTestContextManager4Test() {

			return super.getTestContextManager();
		}
	}

}
