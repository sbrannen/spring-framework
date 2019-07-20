package org.springframework.test.context.web;

import org.springframework.test.context.junit.jupiter.comics.Person;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PersonController {

	@GetMapping("/person/{id}")
	Person getPerson(@PathVariable long id) {
		if (id == 42) {
			return new Person("Dilbert");
		}
		return new Person("Wally");
	}

}
