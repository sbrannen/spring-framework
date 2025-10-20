
package example;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@CacheConfig("cachedController")
public class CachedController {

	private int parent;
	private int child;
	private int standalone;


	@GetMapping("parent")
	@Cacheable(key = "T(example.ParentCacheKeyGenerator).getCacheKey()")
	public String getParentCacheKey() {
		return "Parent " + ++parent;
	}

	@GetMapping("child")
	@Cacheable(key = "T(example.ChildCacheKeyGenerator).getCacheKey()")
	public String getChildCacheKey() {
		return "Child " + ++child;
	}

	@GetMapping("standalone")
	@Cacheable(key = "T(example.StandaloneCacheKeyGenerator).getCacheKey()")
	public String getStandaloneCacheKey() {
		return "Standalone " + ++standalone;
	}

}
