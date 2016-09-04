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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.concurrency.model.Client;
import org.springframework.test.context.concurrency.model.RequestStorage;
import org.springframework.test.context.concurrency.model.SessionStorage;
import org.springframework.test.context.concurrency.model.Storage1Impl;
import org.springframework.test.context.concurrency.model.Storage2Impl;
import org.springframework.test.context.concurrency.model.SubBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.annotation.SessionScope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.context.annotation.ScopedProxyMode.INTERFACES;

/**
 * When run stand-alone as part of the standard test suite, this test class
 * verifies a number of basic assumptions that should hold true for tests
 * against the test application.
 *
 * <p>{@link TestContextFrameworkConcurrencyTests} runs this test class a number
 * of times in parallel.
 *
 * <p>If the parallel tests fail, you should first verify that this test class passes
 * on its own.
 *
 * @author Kristian Rosenvold
 * @author Sam Brannen
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
// @DirtiesContext
public class SpringRunnerEnvironmentAssumptionsTests implements ClientTester {

	@Autowired
	Client client;

	@Autowired
	SubBean subBean1;

	AtomicInteger runCount = new AtomicInteger();

	String threadInfo = getThreadInfo();


	private String getThreadInfo() {
		return this.toString() + "-thread-" + Thread.currentThread();
	}

	@Test
	public void testThatWeReNotRunningInTheSameInstance() {
		assertEquals(1, runCount.incrementAndGet());
	}

	@Test
	public void testThatWeAreCreatedOnTheSameThreadAsWeRun() {
		assertEquals(getThreadInfo(), this.threadInfo);
	}

	@Test
	public void testThatScopeProxiedInstancesAreServedByDifferentInstances() {
		assertNotEquals("Expect different instance for differnet bean.", client.getSessionStorage().id(),
			client.getSessionStorageLazy().id());
		assertEquals("Expect equal instance for alias.", client.getSessionStorage().id(),
			client.getSessionStorageAlias().id());
	}

	@Test
	public void testSubBean1() {
		assertNotNull(subBean1.getService());
		assertEquals(SubBean.class, subBean1.getClass());
	}

	@Test
	@Override
	public void testClient() {
		assertNotNull("client.getSessionStorage()", client.getSessionStorage());
		assertNotNull("client.getSessionStorageAlias()", client.getSessionStorageAlias());
		assertNotNull("client.getSessionStorageLazy()", client.getSessionStorageLazy());
		assertNotNull("client.getRequestStorage()", client.getRequestStorage());
		assertNotNull("subBean1.getService()", subBean1.getService());
		assertEquals("subBean1.getClass()", SubBean.class, subBean1.getClass());
	}


	@Configuration
	@ComponentScan(basePackageClasses = SessionStorage.class)
	static class Config {

		@Bean(name = "default")
		@SessionScope(proxyMode = INTERFACES)
		SessionStorage storage1() {
			return new Storage1Impl();
		}

		@Bean
		@SessionScope(proxyMode = INTERFACES)
		@Lazy
		SessionStorage lazyStorage1() {
			return new Storage1Impl();
		}

		@Bean
		@SessionScope(proxyMode = INTERFACES)
		RequestStorage storage2() {
			return new Storage2Impl();
		}
	}

}
