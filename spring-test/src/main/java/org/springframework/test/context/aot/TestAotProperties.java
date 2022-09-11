/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.aot;

import org.springframework.aot.AotDetector;
import org.springframework.lang.Nullable;

/**
 * Holder for AOT properties, analogous to
 * {@link org.springframework.core.SpringProperties SpringProperties} but specific
 * to ahead-of-time (AOT) support in the <em>Spring TestContext Framework</em>.
 *
 * <p>{@code TestAotProperties} has two modes of operation: build-time and run-time.
 * At build time, test components can {@linkplain #setProperty contribute} properties
 * during the AOT processing phase. At run time, test components can
 * {@linkplain #getString(String) retrieve} properties that were stored at
 * build time. If {@link AotDetector#useGeneratedArtifacts()} returns {@code true},
 * {@code TestAotProperties} operates in run-time mode.
 *
 * <p>For example, if a test component computes something at build time that
 * cannot be computed at run time, the result of the build-time computation can
 * be stored as an AOT property and retrieved at run time without repeating the
 * computation.
 *
 * <p>An {@link AotContextLoader} would typically contribute a property in
 * {@link AotContextLoader#loadContextForAotProcessing loadContextForAotProcessing()};
 * whereas, an {@link AotTestExecutionListener} would typically contribute a property
 * in {@link AotTestExecutionListener#processAheadOfTime processAheadOfTime()}.
 * Any other test component &mdash; such as a
 * {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}
 * &mdash; can choose to contribute a property at any point time. Note that
 * contributing a property during standard JVM test execution will not have any
 * adverse side effect since AOT properties will be ignored in that scenario. In
 * any case, you should use {@link AotDetector#useGeneratedArtifacts()} to determine
 * if invocations of {@link #setProperty(String, String)} are permitted.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public interface TestAotProperties {

	/**
	 * Get the current instance of {@code TestAotProperties} to use.
	 * <p>See the class-level {@link TestAotProperties Javadoc} for details on
	 * the two possible modes.
	 */
	static TestAotProperties getInstance() {
		return new DefaultTestAotProperties(TestAotPropertiesFactory.getProperties());
	}


	/**
	 * Set a {@code String} property for later retrieval during AOT run-time execution.
	 * @param key the property key
	 * @param value the associated property value
	 * @throws UnsupportedOperationException if invoked during
	 * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
	 * @throws IllegalArgumentException if the provided value is {@code null} or
	 * if an attempt is made to override an existing property
	 * @see AotDetector#useGeneratedArtifacts()
	 * @see #setProperty(String, boolean)
	 */
	void setProperty(String key, String value);

	/**
	 * Set a {@code boolean} property for later retrieval during AOT run-time execution.
	 * @param key the property key
	 * @param value the associated property value
	 * @throws UnsupportedOperationException if invoked during
	 * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
	 * @throws IllegalArgumentException if an attempt is made to override an
	 * existing property
	 * @see AotDetector#useGeneratedArtifacts()
	 * @see #setProperty(String, String)
	 * @see Boolean#toString(boolean)
	 */
	default void setProperty(String key, boolean value) {
		setProperty(key, Boolean.toString(value));
	}

	/**
	 * Remove the property stored under the provided key.
	 * @param key the property key
	 * @throws UnsupportedOperationException if invoked during
	 * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
	 * @see AotDetector#useGeneratedArtifacts()
	 */
	void removeProperty(String key);

	/**
	 * Retrieve the property value for the given key as a {@link String}.
	 * @param key the property key
	 * @return the associated property value, or {@code null} if not found
	 * @see #getBoolean(String)
	 */
	@Nullable
	String getString(String key);

	/**
	 * Retrieve the property value for the given key as a {@code boolean}.
	 * @param key the property key
	 * @return {@code true} if the property is set to "true" (ignoring case),
	 * {@code} false otherwise
	 * @see #getString(String)
	 * @see Boolean#parseBoolean(String)
	 */
	default boolean getBoolean(String key) {
		return Boolean.parseBoolean(getString(key));
	}

}
