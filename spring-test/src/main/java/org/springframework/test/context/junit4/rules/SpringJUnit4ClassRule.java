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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.statements.ProfileValueChecker;
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;

/**
 * {@code SpringJUnit4ClassRule} is a custom JUnit {@link TestRule} that provides
 * class-level functionality of the <em>Spring TestContext Framework</em> to
 * standard JUnit tests by means of the {@link TestContextManager} and associated
 * support classes and annotations.
 *
 * <p>In contrast to the {@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * SpringJUnit4ClassRunner}, Spring's rule-based JUnit support has the advantage
 * that it is independent of any {@link org.junit.runner.Runner Runner} and
 * can therefore be combined with existing alternative runners like JUnit's
 * {@code Parameterized} or third-party runners such as the {@code MockitoJUnitRunner}.
 *
 * <p>In order to achieve the same functionality as the {@code SpringJUnit4ClassRunner},
 * however, the {@code SpringJUnit4ClassRule} must be combined with the
 * {@link SpringJUnit4MethodRule}, since {@code SpringJUnit4ClassRule} only provides
 * the class-level features of the {@code SpringJUnit4ClassRunner}.
 *
 * <h3>Example Usage</h3>
 * <pre><code> public class ExampleSpringIntegrationTest {
 *
 *    &#064;ClassRule
 *    public static final SpringJUnit4ClassRule SPRING_CLASS_RULE = new SpringJUnit4ClassRule();
 *
 *    &#064;Rule
 *    public final SpringJUnit4MethodRule springMethodRule = new SpringJUnit4MethodRule(this);
 *
 *    // ...
 * }</code></pre>
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see SpringJUnit4MethodRule
 * @see TestContextManager
 * @see SpringJUnit4ClassRunner
 */
public class SpringJUnit4ClassRule implements TestRule {

	/**
	 * This field is {@code volatile} since a {@code SpringJUnit4MethodRule}
	 * can potentially access it from a different thread, depending on the
	 * type of JUnit runner in use.
	 */
	private volatile TestContextManager testContextManager;


	/**
	 * Create a new {@link TestContextManager} for the supplied test class.
	 * <p>Can be overridden by subclasses.
	 * @param clazz the test class to be managed
	 */
	protected TestContextManager createTestContextManager(Class<?> clazz) {
		return new TestContextManager(clazz);
	}

	/**
	 * Get the {@link TestContextManager} associated with this class rule.
	 * <p>Will be {@code null} until the {@link #apply} method is invoked
	 * by a JUnit runner.
	 */
	protected final TestContextManager getTestContextManager() {
		return this.testContextManager;
	}

	/**
	 * TODO Update Javadoc for apply().
	 *
	 * Create the {@link TestContextManager} and run its before and after
	 * class methods.
	 *
	 * <p>In addition, this method checks whether the test is enabled in
	 * the current execution environment. This prevents classes with a
	 * non-matching {@code @IfProfileValue} annotation from running altogether,
	 * even skipping the execution of {@code beforeTestClass()} methods
	 * in {@code TestExecutionListeners}.
	 *
	 * @param base the base {@link Statement} that this rule should be applied to
	 * @param description a {@link Description} of the current test execution
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
	 * @see org.springframework.test.annotation.IfProfileValue
	 * @see org.springframework.test.context.TestExecutionListener
	 * @see #createTestContextManager(Class)
	 * @see TestContextManager#beforeTestClass()
	 * @see TestContextManager#afterTestClass()
	 */
	@Override
	public Statement apply(final Statement base, final Description description) {
		Class<?> testClass = description.getTestClass();
		validateSpringMethodRuleConfiguration(testClass);

		this.testContextManager = createTestContextManager(testClass);

		Statement statement = base;

		statement = withBeforeTestClassCallbacks(statement);
		statement = withAfterTestClassCallbacks(statement);
		statement = withProfileValueCheck(testClass, statement);

		return statement;
	}

	/**
	 * Wrap the supplied {@code statement} with a {@code RunBeforeTestClassCallbacks} statement.
	 * @see RunBeforeTestClassCallbacks
	 */
	protected Statement withBeforeTestClassCallbacks(Statement statement) {
		return new RunBeforeTestClassCallbacks(statement, getTestContextManager());
	}

	/**
	 * Wrap the supplied {@code statement} with a {@code RunAfterTestClassCallbacks} statement.
	 * @see RunAfterTestClassCallbacks
	 */
	protected Statement withAfterTestClassCallbacks(Statement statement) {
		return new RunAfterTestClassCallbacks(statement, getTestContextManager());
	}

	/**
	 * Wrap the supplied {@code statement} with a {@code ProfileValueChecker} statement.
	 * @see ProfileValueChecker
	 */
	protected Statement withProfileValueCheck(Class<?> testClass, Statement statement) {
		return new ProfileValueChecker(statement, testClass, null);
	}

	private void validateSpringMethodRuleConfiguration(Class<?> testClass) {
		Field ruleField = null;

		for (Field field : testClass.getDeclaredFields()) {
			int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)
					&& (SpringJUnit4MethodRule.class.isAssignableFrom(field.getType()))) {
				ruleField = field;
				break;
			}
		}

		if (ruleField == null) {
			throw new IllegalStateException(String.format(
				"Failed to find 'public SpringJUnit4MethodRule' field in test class [%s]. "
						+ "Consult the Javadoc for @SpringJUnit4ClassRule for details.", testClass.getName()));
		}

		if (!ruleField.isAnnotationPresent(Rule.class)) {
			throw new IllegalStateException(String.format(
				"SpringJUnit4MethodRule field [%s] must be annotated with JUnit's @Rule annotation. "
						+ "Consult the Javadoc for @SpringJUnit4ClassRule for details.", ruleField));
		}
	}

}
