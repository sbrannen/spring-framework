package example;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class XmlPlaceholderTests {

	@Autowired
	CalculationService calculationService;

	@Test
	void test() {
		assertThat(calculationService.getCalculator()).isExactlyInstanceOf(BasicCalculator.class);
	}


	@Configuration
	@ImportResource("classpath:example/appContext.xml")
	static class Config {
	}

}
