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
import java.lang.reflect.Method;

import org.junit.ClassRule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.ProfileValueChecker;
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunPrepareTestInstanceCallbacks;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;
import org.springframework.test.context.junit4.statements.SpringRepeat;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@code SpringJUnit4MethodRule} is a custom JUnit {@link MethodRule} that
 * provides instance-level and method-level functionality of the
 * <em>Spring TestContext Framework</em> to standard JUnit tests by means
 * of the {@link TestContextManager} and associated support classes and
 * annotations.
 *
 * <p>In contrast to the {@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * SpringJUnit4ClassRunner}, Spring's rule-based JUnit support has the advantage
 * that it is independent of any {@link org.junit.runner.Runner Runner} and
 * can therefore be combined with existing alternative runners like JUnit's
 * {@code Parameterized} or third-party runners such as the {@code MockitoJUnitRunner}.
 *
 * <p>In order to achieve the same functionality as the {@code SpringJUnit4ClassRunner},
 * however, the {@code SpringJUnit4MethodRule} must be combined with the
 * {@link SpringJUnit4ClassRule}, since {@code SpringJUnit4MethodRule} only
 * provides the method-level features of the {@code SpringJUnit4ClassRunner}.
 *
 * <h3>Example Usage</h3>
 * <pre><code>public class ExampleSpringIntegrationTest {
 *
 *    &#064;ClassRule
 *    public static final SpringJUnit4ClassRule SPRING_CLASS_RULE = new SpringJUnit4ClassRule();
 *
 *    &#064;Rule
 *    public final SpringJUnit4MethodRule springMethodRule = new SpringJUnit4MethodRule(this);
 *
 *   // ...
 * }</code></pre>
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see SpringJUnit4ClassRule
 */
public class SpringJUnit4MethodRule implements MethodRule {

	/**
	 * {@link SpringJUnit4MethodRule} retains a reference to the {@code SpringJUnit4ClassRule}
	 * instead of the {@code TestContextManager}, since the class rule <em>owns</em>
	 * the {@code TestContextManager} and can therefore release it when it's
	 * no longer needed.
	 */
	private final SpringJUnit4ClassRule springClassRule;


	private static SpringJUnit4ClassRule retrieveAndValidateClassRule(Class<?> testClass) {
		Field springClassRuleField = null;

		for (Field field : testClass.getDeclaredFields()) {
			if (ReflectionUtils.isPublicStaticFinal(field)
					&& (SpringJUnit4ClassRule.class.isAssignableFrom(field.getType()))) {
				springClassRuleField = field;
				break;
			}
		}

		if (springClassRuleField == null) {
			throw new IllegalStateException(String.format(
				"Failed to find 'public static final SpringJUnit4ClassRule' field in test class [%s]. "
						+ "Consult the Javadoc for @SpringJUnit4ClassRule for details.", testClass.getName()));
		}

		if (!springClassRuleField.isAnnotationPresent(ClassRule.class)) {
			throw new IllegalStateException(String.format(
				"SpringJUnit4ClassRule field [%s] must be annotated with JUnit's @ClassRule annotation. "
						+ "Consult the Javadoc for @SpringJUnit4ClassRule for details.", springClassRuleField));
		}

		return (SpringJUnit4ClassRule) ReflectionUtils.getField(springClassRuleField, null);
	}

	/**
	 * Construct a new {@code SpringJUnit4MethodRule} for the supplied test instance.
	 *
	 * <p>The test class must declare a {@code public static final SpringJUnit4ClassRule}
	 * field (i.e., a <em>constant</em>) that is annotated with JUnit's
	 * {@link ClassRule @ClassRule} &mdash; for example:
	 *
	 * <pre><code> &#064;ClassRule
	 * public static final SpringJUnit4ClassRule SPRING_CLASS_RULE = new SpringJUnit4ClassRule();</code></pre>
	 *
	 * @param testInstance the test instance, never {@code null}
	 * @throws IllegalStateException if the test class does not declare an
	 * appropriate {@code SpringJUnit4ClassRule} constant.
	 */
	public SpringJUnit4MethodRule(Object testInstance) {
		Assert.notNull(testInstance, "testInstance must not be null");
		this.springClassRule = retrieveAndValidateClassRule(testInstance.getClass());
	}

	/**
	 * TODO Update Javadoc for apply().
	 *
	 * <p>Prepare the test instance and run before and after test methods on
	 * the {@link TestContextManager}.
	 *
	 * <p>In addition, this method checks whether the test is enabled in
	 * the current execution environment. This prevents methods with a
	 * non-matching {@code @IfProfileValue} annotation from running altogether,
	 * even skipping the execution of {@code prepareTestInstance()} methods
	 * in {@code TestExecutionListeners}.
	 *
	 * @see TestContextManager#prepareTestInstance(Object)
	 * @see TestContextManager#beforeTestMethod(Object, Method)
	 * @see TestContextManager#afterTestMethod(Object, Method, Throwable)
	 */
	@Override
	public Statement apply(final Statement base, final FrameworkMethod frameworkMethod, final Object testInstance) {
		Statement statement = base;

		statement = withTestInstancePreparation(testInstance, statement);
		statement = withBeforeTestMethodCallbacks(frameworkMethod, testInstance, statement);
		statement = withAfterTestMethodCallbacks(frameworkMethod, testInstance, statement);
		statement = withPotentialRepeat(frameworkMethod, testInstance, statement);
		statement = withPotentialTimeout(frameworkMethod, testInstance, statement);
		statement = withProfileValueCheck(frameworkMethod, testInstance, statement);

		return statement;
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@link ProfileValueChecker} statement.
	 * @see ProfileValueChecker
	 */
	protected Statement withProfileValueCheck(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		return new ProfileValueChecker(statement, testInstance.getClass(), frameworkMethod.getMethod());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@link RunPrepareTestInstanceCallbacks} statement.
	 * @see RunPrepareTestInstanceCallbacks
	 */
	protected Statement withTestInstancePreparation(Object testInstance, Statement statement) {
		return new RunPrepareTestInstanceCallbacks(statement, testInstance,
			this.springClassRule.getTestContextManager());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@link RunBeforeTestMethodCallbacks} statement.
	 * @see RunBeforeTestMethodCallbacks
	 */
	protected Statement withBeforeTestMethodCallbacks(FrameworkMethod frameworkMethod, Object testInstance,
			Statement statement) {
		return new RunBeforeTestMethodCallbacks(statement, testInstance, frameworkMethod.getMethod(),
			this.springClassRule.getTestContextManager());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@link RunAfterTestMethodCallbacks} statement.
	 * @see RunAfterTestMethodCallbacks
	 */
	protected Statement withAfterTestMethodCallbacks(FrameworkMethod frameworkMethod, Object testInstance,
			Statement statement) {
		return new RunAfterTestMethodCallbacks(statement, testInstance, frameworkMethod.getMethod(),
			this.springClassRule.getTestContextManager());
	}

	/**
	 * Return a {@link Statement} that potentially repeats the execution of
	 * the {@code next} statement.
	 * <p>Supports Spring's {@link Repeat @Repeat} annotation by returning a
	 * {@link SpringRepeat} statement initialized with the configured repeat
	 * count (if greater than {@code 1}); otherwise, the supplied statement
	 * is returned unmodified.
	 * @return either a {@link SpringRepeat} or the supplied {@link Statement}
	 * as appropriate
	 * @see SpringRepeat
	 */
	protected Statement withPotentialRepeat(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Repeat repeatAnnotation = AnnotationUtils.getAnnotation(frameworkMethod.getMethod(), Repeat.class);
		int repeat = (repeatAnnotation != null ? repeatAnnotation.value() : 1);
		return (repeat > 1 ? new SpringRepeat(next, frameworkMethod.getMethod(), repeat) : next);
	}

	/**
	 * TODO Document withPotentialTimeout().
	 * @see #getSpringTimeout(FrameworkMethod)
	 */
	protected Statement withPotentialTimeout(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		long springTimeout = getSpringTimeout(frameworkMethod);
		return (springTimeout > 0 ? new SpringFailOnTimeout(next, springTimeout) : next);
	}

	/**
	 * Retrieve the configured Spring-specific {@code timeout} from the
	 * {@link Timed @Timed} annotation on the supplied
	 * {@linkplain FrameworkMethod test method}.
	 * @return the timeout, or {@code 0} if none was specified
	 */
	protected long getSpringTimeout(FrameworkMethod frameworkMethod) {
		AnnotationAttributes annAttrs = AnnotatedElementUtils.getAnnotationAttributes(frameworkMethod.getMethod(),
			Timed.class.getName());
		if (annAttrs == null) {
			return 0;
		}
		else {
			long millis = annAttrs.<Long> getNumber("millis").longValue();
			return millis > 0 ? millis : 0;
		}
	}

}
