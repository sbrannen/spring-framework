/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.support;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ControlFlowPointcut}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
class ControlFlowPointcutTests {

	@Test
	void matchesExactMethodName() {
		TestBean target = new TestBean("Jane", 27);
		ControlFlowPointcut cflow = pointcut("getAge");
		NopInterceptor nop = new NopInterceptor();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(new DefaultPointcutAdvisor(cflow, nop));
		ITestBean proxy = (ITestBean) pf.getProxy();

		// Not advised, not under MyComponent
		assertThat(proxy.getAge()).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(0);
		assertThat(cflow.getEvaluations()).isEqualTo(1);

		// Will be advised
		assertThat(new MyComponent().getAge(proxy)).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(1);
		assertThat(cflow.getEvaluations()).isEqualTo(2);

		// Won't be advised
		assertThat(new MyComponent().nomatch(proxy)).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(1);
		assertThat(cflow.getEvaluations()).isEqualTo(3);
	}

	@Test
	void matchesMethodNamePatterns() {
		TestBean target = new TestBean("Jane", 27);
		ControlFlowPointcut cflow = pointcut("foo", "get*", "bar", "*se*", "baz");
		NopInterceptor nop = new NopInterceptor();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(new DefaultPointcutAdvisor(cflow, nop));
		ITestBean proxy = (ITestBean) pf.getProxy();

		// Not advised, not under MyComponent
		assertThat(proxy.getAge()).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(0);
		assertThat(cflow.getEvaluations()).isEqualTo(1);

		// Will be advised
		assertThat(new MyComponent().getAge(proxy)).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(1);
		assertThat(cflow.getEvaluations()).isEqualTo(2);

		// Will be advised
		new MyComponent().set(proxy);
		assertThat(proxy.getAge()).isEqualTo(5);
		assertThat(nop.getCount()).isEqualTo(2);
		assertThat(cflow.getEvaluations()).isEqualTo(4);

		// Won't be advised
		assertThat(new MyComponent().nomatch(proxy)).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(2);
		assertThat(cflow.getEvaluations()).isEqualTo(5);
	}

	@Test
	void controlFlowPointcutIsExtensible() {
		@SuppressWarnings("serial")
		class CustomControlFlowPointcut extends ControlFlowPointcut {

			CustomControlFlowPointcut(Class<?> clazz, String... methodNamePatterns) {
				super(clazz, methodNamePatterns);
			}

			@Override
			public boolean matches(Method method, Class<?> targetClass, Object... args) {
				super.incrementEvaluationCount();
				return super.matches(method, targetClass, args);
			}

			Class<?> trackedClass() {
				return super.clazz;
			}

			List<String> trackedMethodNamePatterns() {
				return super.methodNamePatterns;
			}
		}

		CustomControlFlowPointcut cflow = new CustomControlFlowPointcut(MyComponent.class, "set*", "getAge", "set*", "set*");

		assertThat(cflow.trackedClass()).isEqualTo(MyComponent.class);
		assertThat(cflow.trackedMethodNamePatterns()).containsExactly("set*", "getAge");

		TestBean target = new TestBean("Jane", 27);
		NopInterceptor nop = new NopInterceptor();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(new DefaultPointcutAdvisor(cflow, nop));
		ITestBean proxy = (ITestBean) pf.getProxy();

		// Not advised: the proxy is not invoked under MyComponent#getAge
		assertThat(proxy.getAge()).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(0);
		assertThat(cflow.getEvaluations()).isEqualTo(2); // intentional double increment

		// Will be advised: the proxy is invoked under MyComponent#getAge
		assertThat(new MyComponent().getAge(proxy)).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(1);
		assertThat(cflow.getEvaluations()).isEqualTo(4); // intentional double increment

		// Won't be advised: the proxy is not invoked under MyComponent#getAge
		assertThat(new MyComponent().nomatch(proxy)).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(1);
		assertThat(cflow.getEvaluations()).isEqualTo(6); // intentional double increment
	}

	/**
	 * Check that we can use a cflow pointcut in conjunction with
	 * a static pointcut: e.g. all setter methods that are invoked under
	 * a particular class. This greatly reduces the number of calls
	 * to the cflow pointcut, meaning that it's not so prohibitively
	 * expensive.
	 */
	@Test
	void selectiveApplication() {
		TestBean target = new TestBean("Jane", 27);
		ControlFlowPointcut cflow = pointcut();
		NopInterceptor nop = new NopInterceptor();
		Pointcut settersUnderMyComponent = Pointcuts.intersection(Pointcuts.SETTERS, cflow);
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(new DefaultPointcutAdvisor(settersUnderMyComponent, nop));
		ITestBean proxy = (ITestBean) pf.getProxy();

		// Not advised, not under MyComponent
		target.setAge(16);
		assertThat(nop.getCount()).isEqualTo(0);

		// Not advised; under MyComponent but not a setter
		assertThat(new MyComponent().getAge(proxy)).isEqualTo(16);
		assertThat(nop.getCount()).isEqualTo(0);

		// Won't be advised
		new MyComponent().set(proxy);
		assertThat(nop.getCount()).isEqualTo(1);

		// We saved most evaluations
		assertThat(cflow.getEvaluations()).isEqualTo(1);
	}

	@Test
	void equalsAndHashCode() {
		assertThat(pointcut()).isEqualTo(pointcut());
		assertThat(pointcut()).hasSameHashCodeAs(pointcut());

		assertThat(pointcut("getAge")).isEqualTo(pointcut("getAge"));
		assertThat(pointcut("getAge")).hasSameHashCodeAs(pointcut("getAge"));

		assertThat(pointcut("getAge")).isNotEqualTo(pointcut());
		assertThat(pointcut("getAge")).doesNotHaveSameHashCodeAs(pointcut());

		assertThat(pointcut("get*", "set*")).isEqualTo(pointcut("get*", "set*"));
		assertThat(pointcut("get*", "set*")).isEqualTo(pointcut("get*", "set*", "set*", "get*"));
		assertThat(pointcut("get*", "set*")).hasSameHashCodeAs(pointcut("get*", "get*", "set*"));

		assertThat(pointcut("get*", "set*")).isNotEqualTo(pointcut("set*", "get*"));
		assertThat(pointcut("get*", "set*")).doesNotHaveSameHashCodeAs(pointcut("set*", "get*"));

		assertThat(pointcut("get*", "set*")).isEqualTo(pointcut(List.of("get*", "set*")));
		assertThat(pointcut("get*", "set*")).isEqualTo(pointcut(List.of("get*", "set*", "set*", "get*")));
		assertThat(pointcut("get*", "set*")).hasSameHashCodeAs(pointcut(List.of("get*", "get*", "set*")));
	}

	@Test
	void testToString() {
		String pointcutType = ControlFlowPointcut.class.getName();
		String componentType = MyComponent.class.getName();

		assertThat(pointcut()).asString()
				.startsWith(pointcutType)
				.contains(componentType)
				.endsWith("[]");

		assertThat(pointcut("getAge")).asString()
				.startsWith(pointcutType)
				.contains(componentType)
				.endsWith("[getAge]");

		assertThat(pointcut("get*", "set*", "get*")).asString()
				.startsWith(pointcutType)
				.contains(componentType)
				.endsWith("[get*, set*]");
	}


	private static ControlFlowPointcut pointcut() {
		return new ControlFlowPointcut(MyComponent.class);
	}

	private static ControlFlowPointcut pointcut(String methodNamePattern) {
		return new ControlFlowPointcut(MyComponent.class, methodNamePattern);
	}

	private static ControlFlowPointcut pointcut(String... methodNamePatterns) {
		return new ControlFlowPointcut(MyComponent.class, methodNamePatterns);
	}

	private static ControlFlowPointcut pointcut(List<String> methodNamePatterns) {
		return new ControlFlowPointcut(MyComponent.class, methodNamePatterns);
	}


	private static class MyComponent {
		int getAge(ITestBean proxy) {
			return proxy.getAge();
		}
		int nomatch(ITestBean proxy) {
			return proxy.getAge();
		}
		void set(ITestBean proxy) {
			proxy.setAge(5);
		}
	}

}
