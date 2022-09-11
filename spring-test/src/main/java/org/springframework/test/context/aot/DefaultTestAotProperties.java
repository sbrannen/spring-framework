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

import java.util.Map;

import org.springframework.aot.AotDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link TestAotProperties} backed by a {@link Map}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class DefaultTestAotProperties implements TestAotProperties {

	final Map<String, String> map;


	DefaultTestAotProperties(Map<String, String> map) {
		this.map = map;
	}


	@Override
	public void setProperty(String key, String value) {
		assertNotInAotRuntime();
		Assert.notNull(value, "'value' must not be null");
		Assert.isTrue(!this.map.containsKey(key),
				() -> "AOT properties cannot be overridden. Key '%s' is already in use.".formatted(key));
		this.map.put(key, value);
	}

	@Override
	public void removeProperty(String key) {
		assertNotInAotRuntime();
		this.map.remove(key);
	}

	@Override
	@Nullable
	public String getString(String key) {
		return this.map.get(key);
	}


	private static void assertNotInAotRuntime() {
		if (AotDetector.useGeneratedArtifacts()) {
			throw new UnsupportedOperationException(
				"AOT properties cannot be modified during AOT run-time execution");
		}
	}

}
