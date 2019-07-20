
package org.springframework.test.context.web;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = WebConfig.class)
@WebAppConfiguration
public class MockMvcTests {

	@Autowired
	WebApplicationContext wac;

	MockMvc mockMvc;

	@Before
	public void setUpMockMvc() {
		this.mockMvc = webAppContextSetup(wac)//
				// .alwaysDo(print(System.err))//
				.alwaysExpect(status().isOk())//
				.alwaysExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))//
				.build();
	}

	@Test
	public void getPerson42() throws Exception {
		this.mockMvc.perform(get("/person/42").accept(MediaType.APPLICATION_JSON))//
				.andExpect(jsonPath("$.name", is("Dilbert")));
	}

	@Test
	public void fluentAndReturn() throws Exception {
		this.mockMvc.perform(get("/person/42").accept(MediaType.APPLICATION_JSON))//
				.fluent()//
				.andReturn();
	}

	@Test
	public void fluentAndAssertThat() throws Exception {
		this.mockMvc.perform(get("/person/42").accept(MediaType.APPLICATION_JSON))//
				.fluent()
				// .assertThat(new URI("https://spring.io").isAbsolute();
				;
	}

}
