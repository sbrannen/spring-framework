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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assume.*;

/**
 * {@code SpringClassRule} is a custom JUnit {@link TestRule} that provides
 * functionality of the <em>Spring TestContext Framework</em> to standard
 * JUnit tests by means of the {@link TestContextManager} and associated
 * support classes and annotations.
 *
 * <p>In contrast to the {@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * SpringJUnit4ClassRunner}, Spring's rule-based JUnit support has the advantage
 * that it is independent of any {@link org.junit.runner.Runner Runner} and
 * can therefore be combined with existing alternative runners like JUnit's
 * {@code Parameterized} or third-party runners such as the {@code MockitoJUnitRunner}.
 *
 * <p>In order to achieve the same functionality as the {@code SpringJUnit4ClassRunner},
 * however, the {@code SpringClassRule} must be combined with the {@link SpringMethodRule},
 * since {@code SpringClassRule} only provides the class-level features of the
 * {@code SpringJUnit4ClassRunner}.
 *
 * <h3>Example Usage</h3>
 * <pre><code>public class ExampleSpringIntegrationTest {
 *
 *    &#064;ClassRule
 *    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
 *
 *    &#064;Rule
 *    public final SpringMethodRule springMethodRule = new SpringMethodRule(this);
 *
 *   // ...
 * }</code></pre>
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see SpringMethodRule
 * @see TestContextManager
 * @see SpringJUnit4ClassRunner
 */
public class SpringClassRule implements TestRule {

	/**
	 * This field is {@code volatile} since a {@code SpringMethodRule} can
	 * potentially access it from a different thread, depending on the type
	 * of JUnit runner in use.
	 */
	private volatile TestContextManager testContextManager;


	/**
	 * Get the {@link TestContextManager} associated with this class rule.
	 * <p>Will be {@code null} until the {@link #apply} method is invoked
	 * by a JUnit runner.
	 */
	protected final TestContextManager getTestContextManager() {
		return this.testContextManager;
	}

	/**
	 * Create a new {@link TestContextManager} for the supplied test class.
	 * <p>Can be overridden by subclasses.
	 * @param clazz the test class to be managed
	 */
	protected TestContextManager createTestContextManager(Class<?> clazz) {
		return new TestContextManager(clazz);
	}

	/**
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
		final Class<?> testClass = description.getTestClass();

		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				boolean enabled = ProfileValueUtils.isTestEnabledInThisEnvironment(testClass);
				assumeTrue(String.format(
					"Profile configured via [%s] is not enabled in this environment for test class [%s].",
					AnnotationUtils.findAnnotation(testClass, IfProfileValue.class), testClass.getName()), enabled);

				SpringClassRule.this.testContextManager = createTestContextManager(testClass);
				SpringClassRule.this.testContextManager.beforeTestClass();
				try {
					base.evaluate();
				}
				finally {
					// TODO Apply RunAfterTestClassCallbacks or incorporate logic here.
					SpringClassRule.this.testContextManager.afterTestClass();

					// Make TestContextManager eligible for garbage collection
					SpringClassRule.this.testContextManager = null;
				}
			}
		};
	}

}
