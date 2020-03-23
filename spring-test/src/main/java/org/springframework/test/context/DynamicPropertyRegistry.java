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

package org.springframework.test.context;

/**
 * Functional interface used with {@link DynamicPropertySource @DynamicPropertySource}
 * annotated methods so that they can register properties in the {@code Environment} that
 * have dynamically resolved values.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 * @see DynamicPropertySource
 */
@FunctionalInterface
public interface DynamicPropertyRegistry {

	/**
	 * Register a {@link DynamicPropertyResolver} for the given property name.
	 * @param name the name of the property for which the resolver should be registered
	 * @param dynamicPropertyResolver a {@code DynamicPropertyResolver} that will
	 * resolve the property on demand
	 */
	void register(String name, DynamicPropertyResolver dynamicPropertyResolver);


	@FunctionalInterface
	interface DynamicPropertyResolver {

		Object resolve() throws Exception;

	}

}
