package org.springframework.test.context.web;

public class Person {

	private final String name;

	Person(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
