/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.support;

import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link EnumerablePropertySource} backed by a map with dynamically supplied
 * values.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 */
class DynamicValuesPropertySource extends EnumerablePropertySource<Map<String, Callable<Object>>>  {

	DynamicValuesPropertySource(String name, Map<String, Callable<Object>> dynamicValuesMap) {
		super(name, dynamicValuesMap);
	}


	@Override
	public Object getProperty(String name) {
		Callable<Object> valueSupplier = this.source.get(name);
		if (valueSupplier != null) {
			try {
				return valueSupplier.call();
			}
			catch (Exception ex) {
				maskAsUncheckedException(ex);
			}
		}
		return null;
	}

	@Override
	public boolean containsProperty(String name) {
		return this.source.containsKey(name);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.keySet());
	}

	private static void maskAsUncheckedException(Exception ex) {
		DynamicValuesPropertySource.throwAsUncheckedException(ex);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void throwAsUncheckedException(Throwable throwable) throws T {
		throw (T) throwable;
	}

}
