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

/**
 * Interface to be implemented by test components that wish to be aware of the
 * current {@link TestAotProperties}.
 *
 * <p>Currently only the following types of test components are supported by
 * {@code TestAotPropertiesAware}.
 * <ul>
 * <li>{@link org.springframework.test.context.ContextLoader}</li>
 * <li>{@link org.springframework.test.context.TestContextBootstrapper}</li>
 * <li>{@link org.springframework.test.context.TestExecutionListener}</li>
 * </ul>
 *
 * <p>Note that {@code TestAotPropertiesAware} callbacks will <strong>not</strong>
 * be invoked during standard test runs on the JVM. Thus, any code that relies
 * on {@code TestAotProperties} must perform a not-null check before interacting
 * with an instance of {@code TestAotProperties} that is supplied via
 * {@code TestAotPropertiesAware} callbacks.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public interface TestAotPropertiesAware {

	/**
	 * Callback that supplies the current {@link TestAotProperties} to a test component.
	 * @param testAotProperties the current {@code TestAotProperties}
	 */
	void setTestAotProperties(TestAotProperties testAotProperties);

}
