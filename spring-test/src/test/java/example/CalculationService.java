package example;

public class CalculationService {

	private final Calculator calculator;

	public CalculationService(Calculator calculator) {
		this.calculator = calculator;
	}

	public Calculator getCalculator() {
		return this.calculator;
	}

}
