/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.io.buffer;

import java.util.stream.Stream;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
public class PooledDataBufferDynamicTests {

	@TestFactory
	Stream<DynamicContainer> tests() {
		return Stream.of(
			createContainer("UnpooledByteBufAllocator(true)", new UnpooledByteBufAllocator(true)),
			createContainer("UnpooledByteBufAllocator(false)", new UnpooledByteBufAllocator(false)),
			createContainer("PooledByteBufAllocator(true)", new PooledByteBufAllocator(true)),
			createContainer("PooledByteBufAllocator(false)", new PooledByteBufAllocator(false)));
	}

	private DynamicContainer createContainer(String displayName, ByteBufAllocator byteBufAllocator) {
		NettyDataBufferFactory factory = new NettyDataBufferFactory(byteBufAllocator);
		return dynamicContainer(displayName,
			Stream.of(
				dynamicTest("retain and release", () -> retainAndRelease(factory)),
				dynamicTest("too many releases", () -> tooManyReleases(factory)))
		);
	}

	private void retainAndRelease(DataBufferFactory dataBufferFactory) {
		PooledDataBuffer buffer = createDataBuffer(dataBufferFactory, 1);
		buffer.write((byte) 'a');

		buffer.retain();
		assertThat(buffer.release()).isFalse();
		assertThat(buffer.release()).isTrue();
	}

	private void tooManyReleases(DataBufferFactory dataBufferFactory) {
		PooledDataBuffer buffer = createDataBuffer(dataBufferFactory, 1);
		buffer.write((byte) 'a');

		buffer.release();
		assertThatIllegalStateException().isThrownBy(buffer::release);
	}

	private PooledDataBuffer createDataBuffer(DataBufferFactory dataBufferFactory, int capacity) {
		return (PooledDataBuffer) dataBufferFactory.allocateBuffer(capacity);
	}

}
