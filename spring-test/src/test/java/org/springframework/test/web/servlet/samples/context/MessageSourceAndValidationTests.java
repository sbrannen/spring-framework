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

package org.springframework.test.web.servlet.samples.context;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.constraints.Max;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Sam Brannen
 */
@SpringJUnitWebConfig
class MessageSourceAndValidationTests {

	MockMvc mockMvc;


	@BeforeEach
	void setup(WebApplicationContext wac) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	@Test
	void person() throws Exception {
		this.mockMvc.perform(post("/form").param("number", "11"))//
				.andDo(print())//
				.andExpect(status().isOk())//
				.andExpect(forwardedUrl("number_exceeded_10"));
	}


	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		// Declaring the following @Bean method as static makes the test pass.
		// static
		MessageSource messageSource() {
			ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
			messageSource.setBasename("messages/example-msg");

			// Retrieving a message within this @Bean method makes the test pass.
			// messageSource.getMessage("custom.errors.max", null, Locale.ENGLISH);

			return messageSource;
		}

		@Override
		public Validator getValidator() {
			LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
			validator.setValidationMessageSource(messageSource());
			return validator;
		}

		@Bean
		FormController formController() {
			return new FormController();
		}

	}

	static class Form {

		@Max(value = 10, message = "{custom.errors.max}")
		private int number;


		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}
	}

	@Controller
	static class FormController {

		@PostMapping("/form")
		String submit(@Valid Form form, BindingResult errors) {
			if (!errors.hasFieldErrors("number")) {
				throw new IllegalStateException("Declarative validation not applied");
			}

			FieldError fieldError = errors.getFieldError("number");
			if (fieldError.contains(ConstraintViolation.class)) {
				return ((ConstraintViolation<?>) fieldError.unwrap(ConstraintViolation.class)).getMessage();
			}

			return "no_ConstraintViolation_found";
		}

	}

}
