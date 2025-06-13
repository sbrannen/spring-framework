/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core.retry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Max attempts tests for {@link DefaultRetryPolicy} and its {@link RetryExecution}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 */
class MaxAttemptsDefaultRetryPolicyTests {

	@Test
	void withMaxAttempts() {
		var retryPolicy = RetryPolicy.withMaxAttempts(2);
		var retryExecution = retryPolicy.start();
		var throwable = mock(Throwable.class);

		assertThat(retryExecution.shouldRetry(throwable)).isTrue();
		assertThat(retryExecution.shouldRetry(throwable)).isTrue();

		assertThat(retryExecution.shouldRetry(throwable)).isFalse();
		assertThat(retryExecution.shouldRetry(throwable)).isFalse();
	}

	@Test
	void maxAttemptsAndPredicate() {
		var retryPolicy = RetryPolicy.builder()
				.maxAttempts(4)
				.predicate(NumberFormatException.class::isInstance)
				.build();

		var retryExecution = retryPolicy.start();

		// 4 retries
		assertThat(retryExecution.shouldRetry(new NumberFormatException())).isTrue();
		assertThat(retryExecution.shouldRetry(new IllegalStateException())).isFalse();
		assertThat(retryExecution.shouldRetry(new IllegalStateException())).isFalse();
		assertThat(retryExecution.shouldRetry(new NumberFormatException())).isTrue();

		// After policy exhaustion
		assertThat(retryExecution.shouldRetry(new NumberFormatException())).isFalse();
		assertThat(retryExecution.shouldRetry(new IllegalStateException())).isFalse();
	}

	@Test
	void invalidMaxAttempts() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RetryPolicy.withMaxAttempts(0))
				.withMessage("Max attempts must be greater than zero");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RetryPolicy.withMaxAttempts(-1))
				.withMessage("Max attempts must be greater than zero");
	}

	@Test
	void toStringImplementations() {
		var policy = RetryPolicy.builder()
				.maxAttempts(3)
				.includes(RuntimeException.class, IOException.class)
				.excludes(FileNotFoundException.class, FileSystemException.class)
				.build();

		assertThat(policy).asString().isEqualTo("""
				DefaultRetryPolicy[\
				maxAttempts=3, \
				includes=[java.lang.RuntimeException, java.io.IOException], \
				excludes=[java.io.FileNotFoundException, java.nio.file.FileSystemException]]""");

		var template = """
				DefaultRetryPolicyExecution[\
				maxAttempts=3, \
				retryCount=%d, \
				includes=[java.lang.RuntimeException, java.io.IOException], \
				excludes=[java.io.FileNotFoundException, java.nio.file.FileSystemException]]""";
		var retryExecution = policy.start();

		assertThat(retryExecution).asString().isEqualTo(template, 0);

		assertThat(retryExecution.shouldRetry(new IOException())).isTrue();
		assertThat(retryExecution).asString().isEqualTo(template, 1);

		assertThat(retryExecution.shouldRetry(new FileNotFoundException())).isFalse();
		assertThat(retryExecution).asString().isEqualTo(template, 2);

		assertThat(retryExecution.shouldRetry(new IOException())).isTrue();
		assertThat(retryExecution).asString().isEqualTo(template, 3);

		assertThat(retryExecution.shouldRetry(new IOException())).isFalse();
		assertThat(retryExecution).asString().isEqualTo(template, 4);
	}

	@Test
	void toStringImplementationsWithPredicateAsClass() {
		var policy = RetryPolicy.builder()
				.maxAttempts(1)
				.predicate(new NumberFormatExceptionMatcher())
				.build();
		assertThat(policy).asString()
				.isEqualTo("DefaultRetryPolicy[maxAttempts=1, predicate=NumberFormatExceptionMatcher]");

		var retryExecution = policy.start();
		assertThat(retryExecution).asString()
				.isEqualTo("DefaultRetryPolicyExecution[maxAttempts=1, retryCount=0, predicate=NumberFormatExceptionMatcher]");
	}

	@Test
	void toStringImplementationsWithPredicateAsLambda() {
		var policy = RetryPolicy.builder()
				.maxAttempts(2)
				.predicate(NumberFormatException.class::isInstance)
				.build();
		assertThat(policy).asString()
				.matches("DefaultRetryPolicy\\[maxAttempts=2, predicate=MaxAttemptsDefaultRetryPolicyTests.+?Lambda.+?]");

		var retryExecution = policy.start();
		assertThat(retryExecution).asString()
				.matches("DefaultRetryPolicyExecution\\[maxAttempts=2, retryCount=0, predicate=MaxAttemptsDefaultRetryPolicyTests.+?Lambda.+?]");

		assertThat(retryExecution.shouldRetry(new NumberFormatException())).isTrue();
		assertThat(retryExecution).asString()
				.matches("DefaultRetryPolicyExecution\\[maxAttempts=2, retryCount=1, predicate=MaxAttemptsDefaultRetryPolicyTests.+?Lambda.+?]");

		assertThat(retryExecution.shouldRetry(new IllegalStateException())).isFalse();
		assertThat(retryExecution).asString()
				.matches("DefaultRetryPolicyExecution\\[maxAttempts=2, retryCount=2, predicate=MaxAttemptsDefaultRetryPolicyTests.+?Lambda.+?]");

		assertThat(retryExecution.shouldRetry(new NumberFormatException())).isFalse();
		assertThat(retryExecution).asString()
				.matches("DefaultRetryPolicyExecution\\[maxAttempts=2, retryCount=3, predicate=MaxAttemptsDefaultRetryPolicyTests.+?Lambda.+?]");
	}

}
