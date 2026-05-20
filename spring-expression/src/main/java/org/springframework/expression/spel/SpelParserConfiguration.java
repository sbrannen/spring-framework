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

package org.springframework.expression.spel;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.core.SpringProperties;
import org.springframework.util.Assert;

/**
 * Configuration object for the SpEL expression parser.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.expression.spel.standard.SpelExpressionParser#SpelExpressionParser(SpelParserConfiguration)
 */
public class SpelParserConfiguration {

	/**
	 * Default maximum length permitted for a SpEL expression: {@value}.
	 * @since 5.2.24
	 */
	public static final int DEFAULT_MAX_EXPRESSION_LENGTH = 10_000;

	/**
	 * Default maximum nesting depth permitted within a SpEL expression: {@value}.
	 * @since 7.1
	 */
	public static final int DEFAULT_MAX_EXPRESSION_NESTING_DEPTH = 1_000;

	/** System property to configure the default compiler mode for SpEL expression parsers: {@value}. */
	public static final String SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME = "spring.expression.compiler.mode";


	private static final SpelCompilerMode defaultCompilerMode;

	static {
		String compilerMode = SpringProperties.getProperty(SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME);
		defaultCompilerMode = (compilerMode != null ?
				SpelCompilerMode.valueOf(compilerMode.toUpperCase(Locale.ROOT)) : SpelCompilerMode.OFF);
	}


	private final SpelCompilerMode compilerMode;

	private final @Nullable ClassLoader compilerClassLoader;

	private final boolean autoGrowNullReferences;

	private final boolean autoGrowCollections;

	private final int maximumAutoGrowSize;

	private final int maximumExpressionLength;

	private final int maximumNestingDepth;



	/**
	 * Create a new {@code SpelParserConfiguration} instance with default settings.
	 */
	public SpelParserConfiguration() {
		this(null, null, false, false, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * @param compilerMode the compiler mode for the parser
	 * @param compilerClassLoader the ClassLoader to use as the basis for expression compilation
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader) {
		this(compilerMode, compilerClassLoader, false, false, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @see #SpelParserConfiguration(boolean, boolean, int)
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size that the collection can auto grow
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * @param compilerMode the compiler mode that parsers using this configuration object should use
	 * @param compilerClassLoader the ClassLoader to use as the basis for expression compilation
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size that the collection can auto grow
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {

		this(compilerMode, compilerClassLoader, autoGrowNullReferences, autoGrowCollections,
				maximumAutoGrowSize, DEFAULT_MAX_EXPRESSION_LENGTH);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * @param compilerMode the compiler mode that parsers using this configuration object should use
	 * @param compilerClassLoader the ClassLoader to use as the basis for expression compilation
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size that a collection can auto grow
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @since 5.2.25
	 * @see #DEFAULT_MAX_EXPRESSION_NESTING_DEPTH
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength) {

		this((compilerMode != null ? compilerMode : defaultCompilerMode), compilerClassLoader, autoGrowNullReferences,
				autoGrowCollections, maximumAutoGrowSize, maximumExpressionLength, DEFAULT_MAX_EXPRESSION_NESTING_DEPTH);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; must not be {@code null}
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @param maximumNestingDepth the maximum nesting depth permitted within a SpEL
	 * expression; must be a positive number
	 * @since 7.1
	 */
	public SpelParserConfiguration(SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength,
			int maximumNestingDepth) {

		Assert.notNull(compilerMode, "'compilerMode' must not be null");
		Assert.isTrue(maximumExpressionLength > 0, "'maximumExpressionLength' must be a positive number");
		Assert.isTrue(maximumNestingDepth > 0, "'maximumNestingDepth' must be a positive number");

		this.compilerMode = compilerMode;
		this.compilerClassLoader = compilerClassLoader;
		this.autoGrowNullReferences = autoGrowNullReferences;
		this.autoGrowCollections = autoGrowCollections;
		this.maximumAutoGrowSize = maximumAutoGrowSize;
		this.maximumExpressionLength = maximumExpressionLength;
		this.maximumNestingDepth = maximumNestingDepth;
	}


	/**
	 * Return the compiler mode for parsers using this configuration object.
	 */
	public SpelCompilerMode getCompilerMode() {
		return this.compilerMode;
	}

	/**
	 * Return the ClassLoader to use as the basis for expression compilation.
	 */
	public @Nullable ClassLoader getCompilerClassLoader() {
		return this.compilerClassLoader;
	}

	/**
	 * Return {@code true} if {@code null} references should be automatically grown.
	 */
	public boolean isAutoGrowNullReferences() {
		return this.autoGrowNullReferences;
	}

	/**
	 * Return {@code true} if collections should be automatically grown.
	 */
	public boolean isAutoGrowCollections() {
		return this.autoGrowCollections;
	}

	/**
	 * Return the maximum size that a collection can auto grow.
	 */
	public int getMaximumAutoGrowSize() {
		return this.maximumAutoGrowSize;
	}

	/**
	 * Return the maximum number of characters that a SpEL expression can contain.
	 * @since 5.2.25
	 */
	public int getMaximumExpressionLength() {
		return this.maximumExpressionLength;
	}

	/**
	 * Return the maximum nesting depth permitted within a SpEL expression.
	 * @since 7.1
	 */
	public int getMaximumNestingDepth() {
		return this.maximumNestingDepth;
	}

}
