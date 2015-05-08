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

package org.springframework.test.context.junit4.rules;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.EnabledAndIgnoredSpringRunnerTests;

import static org.junit.Assert.*;

/**
 * This class is a copy of {@link EnabledAndIgnoredSpringRunnerTests} that
 * has been modified for rule-based tests.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see EnabledAndIgnoredSpringRunnerTests
 */
@TestExecutionListeners({})
public class EnabledAndIgnoredSpringRuleTests {

	protected static final String NAME = "EnabledAndIgnoredSpringRuleTests.profile_value.name";

	protected static final String VALUE = "enigma";

	protected static int numTestsExecuted = 0;

	@ClassRule
	public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule(this);


	@BeforeClass
	public static void setProfileValue() {
		numTestsExecuted = 0;
		System.setProperty(NAME, VALUE);
	}

	@AfterClass
	public static void verifyNumTestsExecuted() {
		assertEquals("Verifying the number of tests executed.", 3, numTestsExecuted);
	}

	@Test
	@IfProfileValue(name = NAME, value = VALUE + "X")
	public void testIfProfileValueDisabled() {
		numTestsExecuted++;
		fail("The body of a disabled test should never be executed!");
	}

	@Test
	@IfProfileValue(name = NAME, value = VALUE)
	public void testIfProfileValueEnabledViaSingleValue() {
		numTestsExecuted++;
	}

	@Test
	@IfProfileValue(name = NAME, values = { "foo", VALUE, "bar" })
	public void testIfProfileValueEnabledViaMultipleValues() {
		numTestsExecuted++;
	}

	@Test
	public void testIfProfileValueNotConfigured() {
		numTestsExecuted++;
	}

	@Test
	@Ignore
	public void testJUnitIgnoreAnnotation() {
		numTestsExecuted++;
		fail("The body of an ignored test should never be executed!");
	}

}
