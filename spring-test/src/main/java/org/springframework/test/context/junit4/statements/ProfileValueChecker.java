/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4.statements;

import java.lang.reflect.Method;

import org.junit.Assume;
import org.junit.runners.model.Statement;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.util.Assert;

/**
 * TODO Document ProfileValueChecker.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 */
public class ProfileValueChecker extends Statement {

	private final Statement next;
	private final Class<?> testClass;
	private final Method testMethod;


	public ProfileValueChecker(Statement next, Class<?> testClass, Method testMethod) {
		Assert.notNull(next, "The next statement must not be null");
		Assert.notNull(testClass, "The test class must not be null");
		this.next = next;
		this.testClass = testClass;
		this.testMethod = testMethod;
	}

	@Override
	public void evaluate() throws Throwable {
		if (this.testMethod == null) {
			boolean enabled = ProfileValueUtils.isTestEnabledInThisEnvironment(testClass);
			Assume.assumeTrue(String.format(
				"Profile configured via [%s] is not enabled in this environment for test class [%s].",
				AnnotationUtils.findAnnotation(testClass, IfProfileValue.class), testClass.getName()), enabled);
		}
		else {
			boolean enabled = ProfileValueUtils.isTestEnabledInThisEnvironment(testMethod, testClass);
			Assume.assumeTrue(String.format(
				"Profile configured via @IfProfileValue is not enabled in this environment for test method [%s].",
				testMethod), enabled);
		}

		this.next.evaluate();
	}

}
