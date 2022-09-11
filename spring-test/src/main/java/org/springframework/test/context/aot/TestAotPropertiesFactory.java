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
 * Factory for {@link TestAotProperties}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
final class TestAotPropertiesFactory {

	@Nullable
	private static volatile Map<String, String> properties;


	private TestAotPropertiesFactory() {
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
			synchronized (TestAotPropertiesFactory.class) {
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
		synchronized (TestAotPropertiesFactory.class) {
			properties = null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map<String, String> loadPropertiesMap() {
		String className = TestAotPropertiesCodeGenerator.GENERATED_PROPERTIES_CLASS_NAME;
		String methodName = TestAotPropertiesCodeGenerator.GENERATED_PROPERTIES_METHOD_NAME;
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
