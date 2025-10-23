/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @UseTestClassScopedExtensionContext} is a marker annotation which signals
 * that the {@link SpringExtension} should use a test-class scoped
 * {@link org.junit.jupiter.api.extension.ExtensionContext ExtensionContext}
 * within {@link org.junit.jupiter.api.Nested @Nested} test class hierarchies.
 *
 * <p>This annotation should be applied to the top-level (outer most) test
 * class for a {@code @Nested} test class hierarchy.
 *
 * <p>If this annotation is not present, the {@code SpringExtension} will use
 * a test-method scoped {@code ExtensionContext} within the {@code @Nested} test
 * class hierarchy.
 *
 * <p>Note that
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * is not applicable to this annotation: {@code @UseTestClassScopedExtensionContext}
 * will always be detected on a top-level test class, effectively disregarding
 * any {@code @NestedTestConfiguration(OVERRIDE)} declarations.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see SpringExtension#getTestInstantiationExtensionContextScope(org.junit.jupiter.api.extension.ExtensionContext)
 * @see org.junit.jupiter.api.extension.TestInstantiationAwareExtension#getTestInstantiationExtensionContextScope(org.junit.jupiter.api.extension.ExtensionContext)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseTestClassScopedExtensionContext {
}
