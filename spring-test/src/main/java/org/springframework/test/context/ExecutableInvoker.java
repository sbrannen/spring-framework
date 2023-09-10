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

package org.springframework.test.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * {@code ExecutableInvoker} defines a generic API for invoking
 * {@link java.lang.reflect.Executable Executables} (a {@link Constructor} or a
 * {@link Method}) within the <em>Spring TestContext Framework</em>.
 *
 * <p>Specifically, an {@code ExecutableInvoker} is made available to a
 * {@link TestExecutionListener} via {@link TestContext#getExecutableInvoker()},
 * and a {@code TestExecutionListener} can use the invoker to transparently
 * benefit from any special constructor/method invocation features of the
 * underlying testing framework. For example, when the underlying testing
 * framework is JUnit Jupiter, a {@code TestExecutionListener} can use an
 * {@code ExecutableInvoker} to invoke constructors or methods with JUnit Jupiter's
 * parameter resolution mechanism. For other testing frameworks, the {@link #DEFAULT}
 * invoker will be used.
 *
 * @author Sam Brannen
 * @since 6.1
 */
public interface ExecutableInvoker {

	/**
	 * Shared instance of the default {@link ExecutableInvoker}.
	 * <p>This invoker never provides arguments to an
	 * {@link java.lang.reflect.Executable Executable}.
	 */
	static final ExecutableInvoker DEFAULT = new DefaultExecutableInvoker();


	/**
	 * Invoke the supplied {@link Constructor}.
	 * @param <T> the type of object to instantiate
	 * @param constructor the constructor to invoke
	 * @return a new instance of the constructor's declaring class
	 * @throws Exception if any error occurs
	 */
	<T> T invoke(Constructor<T> constructor) throws Exception;

	/**
	 * Invoke the supplied {@link Method} on the supplied {@code target}.
	 * @param method the method to invoke
	 * @param target the object on which to invoke the method, may be {@code null}
	 * if the supplied method is {@code static}
	 * @return the value returned from the method invocation
	 * @throws Exception if any error occurs
	 */
	@Nullable
	Object invoke(Method method, @Nullable Object target) throws Exception;

}
