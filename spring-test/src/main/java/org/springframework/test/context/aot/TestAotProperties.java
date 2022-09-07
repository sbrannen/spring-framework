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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aot.AotDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Static holder for AOT properties, analogous to
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
public final class TestAotProperties {

	static final String GENERATED_PROPERTIES_CLASS_NAME = TestAotProperties.class.getName() + "__Generated";

	static final String GENERATED_PROPERTIES_METHOD_NAME = "getProperties";

	@Nullable
	private static volatile Map<String, String> properties;


	private TestAotProperties() {
	}


	/**
	 * Set a property for later retrieval during AOT run-time execution.
	 * @param key the property key
	 * @param value the associated property value, or {@code null} to reset it
	 * @throws IllegalStateException if invoked during
	 * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
	 * @throws IllegalArgumentException if an attempt is made to override an
	 * existing key
	 * @see AotDetector#useGeneratedArtifacts()
	 */
	public static void setProperty(String key, @Nullable String value) {
		Assert.state(!AotDetector.useGeneratedArtifacts(),
				() -> "AOT properties cannot be modified during AOT run-time execution");
		Map<String, String> props = getProperties();
		if (value == null) {
			props.remove(key);
		}
		else {
			Assert.isTrue(!props.containsKey(key),
				() -> "AOT properties cannot be overridden. Key '%s' is already in use.".formatted(key));
			props.put(key, value);
		}
	}

	/**
	 * Retrieve the property value for the given key as a {@link String}.
	 * @param key the property key
	 * @return the associated property value, or {@code null} if not found
	 * @see #getBoolean(String)
	 */
	@Nullable
	public static String getString(String key) {
		return getProperties().get(key);
	}

	/**
	 * Retrieve the property value for the given key as a {@code boolean}.
	 * @param key the property key
	 * @return {@code true} if the property is set to "true" (ignoring case),
	 * {@code} false otherwise
	 * @see #getString(String)
	 * @see Boolean#parseBoolean(String)
	 */
	public static boolean getBoolean(String key) {
		return Boolean.parseBoolean(getString(key));
	}


	/**
	 * Get the underlying properties map.
	 * <p>If the map is not already loaded, this method loads the map from the
	 * generated class when running in {@linkplain AotDetector#useGeneratedArtifacts()
	 * AOT execution mode} and otherwise creates a new map for storing properties
	 * during the AOT processing phase.
	 */
	static Map<String, String> getProperties() {
		Map<String, String> props = properties;
		if (props == null) {
			synchronized (TestAotProperties.class) {
				props = properties;
				if (props == null) {
					props = (AotDetector.useGeneratedArtifacts() ? loadPropertiesMap() : new ConcurrentHashMap<>());
					properties = props;
				}
			}
		}
		return props;
	}

	/**
	 * Reset test AOT properties.
	 * <p>Only for internal use.
	 */
	static void reset() {
		synchronized (TestAotProperties.class) {
			properties = null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map<String, String> loadPropertiesMap() {
		String className = GENERATED_PROPERTIES_CLASS_NAME;
		String methodName = GENERATED_PROPERTIES_METHOD_NAME;
		try {
			Class<?> clazz = ClassUtils.forName(className, null);
			Method method = ReflectionUtils.findMethod(clazz, methodName);
			Assert.state(method != null, () -> "No %s() method found in %s".formatted(methodName, clazz.getName()));
			Map<String, String> properties = (Map<String, String>) ReflectionUtils.invokeMethod(method, null);
			return Collections.unmodifiableMap(properties);
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to invoke %s() method on %s".formatted(methodName, className), ex);
		}
	}

}
