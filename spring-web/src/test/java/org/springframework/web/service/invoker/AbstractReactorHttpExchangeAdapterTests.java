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

package org.springframework.web.service.invoker;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AbstractReactorHttpExchangeAdapter}.
 *
 * @author Sam Brannen
 */
class AbstractReactorHttpExchangeAdapterTests {

	private final TestAdapter adapter = new TestAdapter();


	@Test
	void blockTimeout() {
		this.adapter.setBlockTimeout(Duration.ofSeconds(5));
		assertThat(this.adapter.getBlockTimeout()).isEqualTo(Duration.ofSeconds(5));
		assertThat(exchangeForBody()).isEqualTo("body");
	}

	@Test
	void blockTimeoutNull() {
		this.adapter.setBlockTimeout(null);
		assertThat(this.adapter.getBlockTimeout()).isNull();
		assertThat(exchangeForBody()).isEqualTo("body");
	}

	@Test  // gh-37425
	void blockTimeoutZeroSpecifiesInfiniteTimeout() {
		this.adapter.setBlockTimeout(Duration.ZERO);
		assertThat(this.adapter.getBlockTimeout()).isNull();
		assertThat(exchangeForBody()).isEqualTo("body");
	}

	@Test  // gh-37425
	void negativeBlockTimeoutIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.adapter.setBlockTimeout(Duration.ofMillis(-1)))
				.withMessage("Timeout must be a non-negative value");
	}

	private String exchangeForBody() {
		return this.adapter.exchangeForBody(HttpRequestValues.builder().build(),
				new ParameterizedTypeReference<String>() {});
	}


	@SuppressWarnings("unchecked")
	private static class TestAdapter extends AbstractReactorHttpExchangeAdapter {

		@Override
		public boolean supportsRequestAttributes() {
			return false;
		}

		@Override
		public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
			return Mono.empty();
		}

		@Override
		public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
			return Mono.just(new HttpHeaders());
		}

		@Override
		public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
			// Delay the response so that a block timeout of 0 would fail immediately.
			return (Mono<T>) Mono.just("body").delayElement(Duration.ofMillis(50));
		}

		@Override
		public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
			return Flux.empty();
		}

		@Override
		public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
			return Mono.just(ResponseEntity.ok().build());
		}

		@Override
		public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(
				HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

			return Mono.empty();
		}

		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(
				HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

			return Mono.empty();
		}
	}

}
