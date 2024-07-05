/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.format.datetime.standard;

import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Internal {@link DateTimeFormatter} utilities.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.3.5
 */
abstract class DateTimeFormatterUtils {

	/**
	 * Create a {@link DateTimeFormatter} for the supplied pattern, configured with
	 * {@linkplain ResolverStyle#STRICT strict} resolution.
	 * <p>Note that the strict resolution does not affect the parsing.
	 * @param pattern the pattern to use
	 * @return a new {@code DateTimeFormatter}
	 * @see ResolverStyle#STRICT
	 * @see #createLenientDateTimeFormatter(FormatStyle)
	 */
	static DateTimeFormatter createStrictDateTimeFormatter(String pattern) {
		// Using strict resolution to align with Joda-Time and standard DateFormat behavior:
		// otherwise, an overflow like e.g. Feb 29 for a non-leap-year wouldn't get rejected.
		// However, with strict resolution, a year digit needs to be specified as 'u'...
		String patternToUse = StringUtils.replace(pattern, "yy", "uu");
		return DateTimeFormatter.ofPattern(patternToUse).withResolverStyle(ResolverStyle.STRICT);
	}

	/**
	 * Create a {@link DateTimeFormatter} for the supplied date/time style, configured
	 * with {@linkplain DateTimeFormatterBuilder#parseLenient() lenient} parsing.
	 * <p>Note that the lenient parsing does not affect the {@linkplain ResolverStyle
	 * resolution strategy}.
	 * @param dateTimeStyle the date/time style to use
	 * @return a new {@code DateTimeFormatter}
	 * @since 6.2
	 * @see DateTimeFormatterBuilder#parseLenient()
	 * @see #createLenientDateTimeFormatter(FormatStyle, FormatStyle)
	 * @see #createStrictDateTimeFormatter(String)
	 */
	static DateTimeFormatter createLenientDateTimeFormatter(FormatStyle dateTimeStyle) {
		return createLenientDateTimeFormatter(dateTimeStyle, dateTimeStyle);
	}

	/**
	 * Create a {@link DateTimeFormatter} for the supplied date and time styles, configured
	 * with {@linkplain DateTimeFormatterBuilder#parseLenient() lenient} parsing.
	 * <p>Note that the lenient parsing does not affect the {@linkplain ResolverStyle
	 * resolution strategy}.
	 * @param dateStyle the date style to use
	 * @param timeStyle the time style to use
	 * @return a new {@code DateTimeFormatter}
	 * @since 6.2
	 * @see DateTimeFormatterBuilder#parseLenient()
	 * @see #createLenientDateTimeFormatter(FormatStyle)
	 * @see #createStrictDateTimeFormatter(String)
	 */
	static DateTimeFormatter createLenientDateTimeFormatter(
			@Nullable FormatStyle dateStyle, @Nullable FormatStyle timeStyle) {

		return new DateTimeFormatterBuilder()
				.parseLenient()
				.appendLocalized(dateStyle, timeStyle)
				.toFormatter()
				.withChronology(IsoChronology.INSTANCE);
	}

}
