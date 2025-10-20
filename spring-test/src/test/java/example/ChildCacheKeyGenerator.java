package example;

public class ChildCacheKeyGenerator extends ParentCacheKeyGenerator {

	public static String getCacheKey() {
		return "child";
	}

}
