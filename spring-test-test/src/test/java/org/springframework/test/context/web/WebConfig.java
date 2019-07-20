package org.springframework.test.context.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
class WebConfig {

	@Bean
	PersonController personController() {
		return new PersonController();
	}

}
