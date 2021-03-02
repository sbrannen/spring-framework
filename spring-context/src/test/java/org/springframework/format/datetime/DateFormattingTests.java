/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.format.datetime;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class DateFormattingTests {

	private final FormattingConversionService conversionService = new FormattingConversionService();

	private DataBinder binder;


	@BeforeEach
	void setup() {
		DateFormatterRegistrar registrar = new DateFormatterRegistrar();
		setup(registrar);
	}

	private void setup(DateFormatterRegistrar registrar) {
		DefaultConversionService.addDefaultConverters(conversionService);
		registrar.registerFormatters(conversionService);

		SimpleDateBean bean = new SimpleDateBean();
		bean.getChildren().add(new SimpleDateBean());
		binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		LocaleContextHolder.setLocale(Locale.US);
	}

	@AfterEach
	void tearDown() {
		LocaleContextHolder.setLocale(null);
	}


	@Test
	void testBindLong() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("millis", "1256961600");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("millis")).isEqualTo("1256961600");
	}

	@Test
	void testBindLongAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("millisAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("millisAnnotated")).isEqualTo("10/31/09");
	}

	@Test
	void testBindCalendarAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("calendarAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("calendarAnnotated")).isEqualTo("10/31/09");
	}

	@Test
	void testBindDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("dateAnnotated")).isEqualTo("10/31/09");
	}

	@Test
	void testBindDateArray() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateAnnotated", new String[]{"10/31/09 12:00 PM"});
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
	}

	@Test
	void testBindDateAnnotatedWithError() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateAnnotated", "Oct X31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getFieldErrorCount("dateAnnotated")).isEqualTo(1);
		assertThat(binder.getBindingResult().getFieldValue("dateAnnotated")).isEqualTo("Oct X31, 2009");
	}

	@Test
	@Disabled
	void testBindDateAnnotatedWithFallbackError() {
		// TODO This currently passes because of the Date(String) constructor fallback is used
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateAnnotated", "Oct 031, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getFieldErrorCount("dateAnnotated")).isEqualTo(1);
		assertThat(binder.getBindingResult().getFieldValue("dateAnnotated")).isEqualTo("Oct 031, 2009");
	}

	@Test
	void testBindDateAnnotatedPattern() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateAnnotatedPattern", "10/31/09 1:05");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("dateAnnotatedPattern")).isEqualTo("10/31/09 1:05");
	}

	@Test
	void testBindDateAnnotatedPatternWithGlobalFormat() {
		DateFormatterRegistrar registrar = new DateFormatterRegistrar();
		DateFormatter dateFormatter = new DateFormatter();
		dateFormatter.setIso(ISO.DATE_TIME);
		registrar.setFormatter(dateFormatter);
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateAnnotatedPattern", "10/31/09 1:05");
		binder.bind(propertyValues);
		BindingResult bindingResult = binder.getBindingResult();
		assertThat(bindingResult.getErrorCount()).isEqualTo(0);
		assertThat(bindingResult.getFieldValue("dateAnnotatedPattern")).isEqualTo("10/31/09 1:05");
	}

	@Test
	void testBindDateTimeOverflow() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateAnnotatedPattern", "02/29/09 12:00 PM");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(1);
	}

	@Test
	void testBindISODate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDate", "2009-10-31");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoDate")).isEqualTo("2009-10-31");
	}

	@Test
	void testBindISOTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoTime", "12:00:00.000-05:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoTime")).isEqualTo("17:00:00.000Z");
	}

	@Test
	void testBindISODateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDateTime", "2009-10-31T12:00:00.000-08:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoDateTime")).isEqualTo("2009-10-31T20:00:00.000Z");
	}

	@Test
	void testBindNestedDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("children[0].dateAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("children[0].dateAnnotated")).isEqualTo("10/31/09");
	}

	@Test
	void dateToStringWithoutGlobalFormat() {
		Date date = new Date();
		Object actual = this.conversionService.convert(date, TypeDescriptor.valueOf(Date.class), TypeDescriptor.valueOf(String.class));
		String expected = date.toString();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void dateToStringWithGlobalFormat() {
		DateFormatterRegistrar registrar = new DateFormatterRegistrar();
		registrar.setFormatter(new DateFormatter());
		setup(registrar);
		Date date = new Date();
		Object actual = this.conversionService.convert(date, TypeDescriptor.valueOf(Date.class), TypeDescriptor.valueOf(String.class));
		String expected = new DateFormatter().print(date, Locale.US);
		assertThat(actual).isEqualTo(expected);
	}

	@Test  // SPR-10105
	@SuppressWarnings("deprecation")
	void stringToDateWithoutGlobalFormat() {
		String string = "Sat, 12 Aug 1995 13:30:00 GM";
		Date date = this.conversionService.convert(string, Date.class);
		assertThat(date).isEqualTo(new Date(string));
	}

	@Test  // SPR-10105
	void stringToDateWithGlobalFormat() {
		DateFormatterRegistrar registrar = new DateFormatterRegistrar();
		DateFormatter dateFormatter = new DateFormatter();
		dateFormatter.setIso(ISO.DATE_TIME);
		registrar.setFormatter(dateFormatter);
		setup(registrar);
		// This is a format that cannot be parsed by new Date(String)
		String string = "2009-06-01T14:23:05.003+00:00";
		Date date = this.conversionService.convert(string, Date.class);
		assertThat(date).isNotNull();
	}


	@Nested
	class FallbackPatternTests {

		@ParameterizedTest
		@ValueSource(strings = {"2021-03-02", "3/2/21", "20210302", "2021.03.02"})
		void annotatedDate(String propertyValue) {
			String propertyName = "dateWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(0);
			assertThat(bindingResult.getFieldValue(propertyName)).isEqualTo("2021-03-02");
		}

		@Test
		void annotatedDateWithUnsupportedPattern() {
			String propertyValue = "210302";
			String propertyName = "dateWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(1);
			FieldError fieldError = bindingResult.getFieldError(propertyName);
			assertThat(fieldError.unwrap(TypeMismatchException.class))
				.hasMessageContaining("for property 'dateWithFallbackPatterns'")
				.hasCauseInstanceOf(ConversionFailedException.class).getCause()
					.hasMessageContaining("for value '210302'")
					.hasCauseInstanceOf(IllegalArgumentException.class).getCause()
						.hasMessageContaining("Parse attempt failed for value [210302]")
						.hasCauseInstanceOf(ParseException.class).getCause()
							// Unable to parse date "210302" using configuration from
							// @org.springframework.format.annotation.DateTimeFormat(
							// pattern=yyyy-MM-dd, style=SS, iso=NONE, fallbackPatterns=[M/d/yy, yyyyMMdd, yyyy.MM.dd])
							.hasMessageContainingAll(
								"Unable to parse date \"210302\" using configuration from",
								"@org.springframework.format.annotation.DateTimeFormat",
								"yyyy-MM-dd", "M/d/yy", "yyyyMMdd", "yyyy.MM.dd");
		}
	}


	@SuppressWarnings("unused")
	private static class SimpleDateBean {

		private Long millis;

		private Long millisAnnotated;

		@DateTimeFormat(style="S-")
		private Calendar calendarAnnotated;

		@DateTimeFormat(style="S-")
		private Date dateAnnotated;

		@DateTimeFormat(pattern="M/d/yy h:mm")
		private Date dateAnnotatedPattern;

		@DateTimeFormat(pattern = "yyyy-MM-dd", fallbackPatterns = { "M/d/yy", "yyyyMMdd", "yyyy.MM.dd" })
		private Date dateWithFallbackPatterns;

		@DateTimeFormat(iso=ISO.DATE)
		private Date isoDate;

		@DateTimeFormat(iso=ISO.TIME)
		private Date isoTime;

		@DateTimeFormat(iso=ISO.DATE_TIME)
		private Date isoDateTime;

		private final List<SimpleDateBean> children = new ArrayList<>();

		public Long getMillis() {
			return millis;
		}

		public void setMillis(Long millis) {
			this.millis = millis;
		}

		@DateTimeFormat(style="S-")
		public Long getMillisAnnotated() {
			return millisAnnotated;
		}

		public void setMillisAnnotated(@DateTimeFormat(style="S-") Long millisAnnotated) {
			this.millisAnnotated = millisAnnotated;
		}

		public Calendar getCalendarAnnotated() {
			return calendarAnnotated;
		}

		public void setCalendarAnnotated(Calendar calendarAnnotated) {
			this.calendarAnnotated = calendarAnnotated;
		}

		public Date getDateAnnotated() {
			return dateAnnotated;
		}

		public void setDateAnnotated(Date dateAnnotated) {
			this.dateAnnotated = dateAnnotated;
		}

		public Date getDateAnnotatedPattern() {
			return dateAnnotatedPattern;
		}

		public void setDateAnnotatedPattern(Date dateAnnotatedPattern) {
			this.dateAnnotatedPattern = dateAnnotatedPattern;
		}

		public Date getDateWithFallbackPatterns() {
			return dateWithFallbackPatterns;
		}

		public void setDateWithFallbackPatterns(Date dateWithFallbackPatterns) {
			this.dateWithFallbackPatterns = dateWithFallbackPatterns;
		}

		public Date getIsoDate() {
			return isoDate;
		}

		public void setIsoDate(Date isoDate) {
			this.isoDate = isoDate;
		}

		public Date getIsoTime() {
			return isoTime;
		}

		public void setIsoTime(Date isoTime) {
			this.isoTime = isoTime;
		}

		public Date getIsoDateTime() {
			return isoDateTime;
		}

		public void setIsoDateTime(Date isoDateTime) {
			this.isoDateTime = isoDateTime;
		}

		public List<SimpleDateBean> getChildren() {
			return children;
		}
	}

}
