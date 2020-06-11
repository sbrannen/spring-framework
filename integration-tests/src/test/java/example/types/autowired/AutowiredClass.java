
package example.types.autowired;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AutowiredClass {

	@Autowired
	public AutowiredClass(AutowiredChild autowiredChild) {
	}

	@Component
	public static class AutowiredChild {
	}
}