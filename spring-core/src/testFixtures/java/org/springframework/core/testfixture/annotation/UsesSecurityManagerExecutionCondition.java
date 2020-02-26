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

package org.springframework.core.testfixture.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.core.GraalVmDetector;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

/**
 * {@link ExecutionCondition} for {@link UsesSecurityManager @UsesSecurityManager}.
 *
 * @author Sam Brannen
 * @since 5.2.5
 */
class UsesSecurityManagerExecutionCondition implements ExecutionCondition {

	private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled(
		"@UsesSecurityManager is not present");


	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Optional<AnnotatedElement> element = context.getElement();
		Optional<UsesSecurityManager> usesSecurityManager = findAnnotation(element, UsesSecurityManager.class);
		if (usesSecurityManager.isPresent() && GraalVmDetector.inImageCode()) {
			return ConditionEvaluationResult.disabled(
				element.get() + " is annotated with @UsesSecurityManager and executing within a GraalVM native image");
		}

		return ENABLED;
	}

}
