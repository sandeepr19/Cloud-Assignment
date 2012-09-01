package classes;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class MemCache {
	
	static MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
	
	
	public static void putInCache(String key, String tweetText)
	{
		syncCache.put(key, tweetText);
	}
	
	public static String getFromCache ( String key)
	{
		return (String) syncCache.get(key);
	}

}
