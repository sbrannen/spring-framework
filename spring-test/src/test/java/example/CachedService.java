
package example;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig("cachedService")
public class CachedService {

	private int parent;
	private int child;
	private int standalone;


	@Cacheable(key = "T(example.ParentCacheKeyGenerator).getCacheKey()")
	public String getParentCacheKey() {
		return "Parent " + ++parent;
	}

	@Cacheable(key = "T(example.ChildCacheKeyGenerator).getCacheKey()")
	public String getChildCacheKey() {
		return "Child " + ++child;
	}

	@Cacheable(key = "T(example.StandaloneCacheKeyGenerator).getCacheKey()")
	public String getStandaloneCacheKey() {
		return "Standalone " + ++standalone;
	}

}
