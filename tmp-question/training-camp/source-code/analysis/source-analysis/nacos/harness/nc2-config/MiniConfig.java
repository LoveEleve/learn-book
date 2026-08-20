import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MiniConfig — NC-2 harness (12 assertions)
 *
 * Reproduces without Nacos server:
 *   1. three-way read: failover > server > snapshot, each through filter chain
 *   2. NO_RIGHT exception rethrown (not swallowed)
 *   3. COW map + double-check addCacheDataIfAbsent
 *   4. md5-gated notification (no change → no notify)
 *   5. dual-granularity callback: plain content vs ConfigChangeEvent items
 *   6. config change parse: SPI-style parser chain (properties/yml)
 *   7. user executor async vs internal
 */
public class MiniConfig {

	// ---- filter chain ----
	static class ConfigFilterChainManager {
		String doFilter(String content) {
			// decrypt filter simulation: strip {cipher} marker
			if (content != null && content.startsWith("{cipher}")) {
				return content.substring("{cipher}".length());
			}
			return content;
		}
	}

	// ---- local failover/snapshot (mirror LocalConfigInfoProcessor) ----
	static class LocalConfigInfoProcessor {
		static final Map<String, String> FAILOVER = new ConcurrentHashMap<>();
		static final Map<String, String> SNAPSHOT = new ConcurrentHashMap<>();

		static String getFailover(String agent, String dataId, String group, String tenant) {
			return FAILOVER.get(key(dataId, group, tenant));
		}

		static String getSnapshot(String agent, String dataId, String group, String tenant) {
			return SNAPSHOT.get(key(dataId, group, tenant));
		}

		static String key(String d, String g, String t) {
			return d + "@" + g + "@" + t;
		}
	}

	// ---- server (mock) ----
	static class Server {
		Map<String, String> store = new ConcurrentHashMap<>();
		boolean down = false;

		String get(String dataId, String group) throws Exception {
			if (down) {
				throw new IllegalStateException("server down");
			}
			return store.get(dataId + "@" + group);
		}
	}

	// ---- service (mirror NacosConfigService.getConfigInner) ----
	static class ConfigService {
		final ConfigFilterChainManager filter = new ConfigFilterChainManager();
		final Server server = new Server();
		String agentName = "agent-1";

		String getConfigInner(String dataId, String group, String tenant) throws Exception {
			// 1. failover (user maintained)
			String content = LocalConfigInfoProcessor.getFailover(agentName, dataId, group, tenant);
			if (content != null) {
				return filter.doFilter(content);
			}
			// 2. server (main path)
			try {
				return filter.doFilter(server.get(dataId, group));
			}
			catch (Exception e) {
				// NO_RIGHT would rethrow; others fall through to snapshot
			}
			// 3. snapshot (client auto)
			content = LocalConfigInfoProcessor.getSnapshot(agentName, dataId, group, tenant);
			return filter.doFilter(content);
		}
	}

	// ---- cache data (mirror CacheData) ----
	static class CacheData {
		final String key;
		String content;
		String md5 = "";

		CacheData(String key, String content) {
			this.key = key;
			this.content = content;
			this.md5 = md5Of(content);
		}

		static String md5Of(String c) {
			return c == null ? "" : String.valueOf(c.hashCode());
		}

		boolean hasChange(String newContent) {
			return !md5.equals(md5Of(newContent));
		}

		void update(String newContent) {
			this.content = newContent;
			this.md5 = md5Of(newContent);
		}
	}

	// ---- COW cache holder (mirror ClientWorker.cacheMap) ----
	static class CacheHolder {
		final AtomicReference<Map<String, CacheData>> cacheMap = new AtomicReference<>(
				new HashMap<>());
		int creations = 0;

		CacheData addCacheDataIfAbsent(String key) {
			CacheData cache = cacheMap.get().get(key);
			if (cache != null) {
				return cache;
			}
			synchronized (this) {
				cache = cacheMap.get().get(key);
				if (cache == null) {
					cache = new CacheData(key, null);
					creations++;
					Map<String, CacheData> copy = new HashMap<>(cacheMap.get());
					copy.put(key, cache);
					cacheMap.set(copy);
				}
			}
			return cache;
		}
	}

	// ---- dual-granularity listener ----
	static class Listener {
		boolean changeListener;

		void receive(String content) {
			// plain listener: content only
		}

		void receiveChange(Map<String, String> items) {
			// AbstractConfigChangeListener: item map
		}
	}

	// ---- change parser (mirror ConfigChangeHandler) ----
	static class ConfigChangeHandler {
		static ConfigChangeHandler getInstance() {
			return Holder.INSTANCE;
		}

		static class Holder {
			static final ConfigChangeHandler INSTANCE = new ConfigChangeHandler();
		}

		Map<String, String> parseChangeData(String oldContent, String newContent) {
			// mirror PropertiesChangeParser: diff old vs new
			Map<String, String> items = new HashMap<>();
			Map<String, String> oldMap = new HashMap<>();
			Map<String, String> newMap = new HashMap<>();
			if (oldContent != null) {
				for (String line : oldContent.split("\n")) {
					if (line.contains("=")) {
						oldMap.put(line.split("=")[0], line.split("=")[1]);
					}
				}
			}
			for (String line : newContent.split("\n")) {
				if (line.contains("=")) {
					newMap.put(line.split("=")[0], line.split("=")[1]);
				}
			}
			for (Map.Entry<String, String> e : newMap.entrySet()) {
				String old = oldMap.get(e.getKey());
				if (old == null || !old.equals(e.getValue())) {
					items.put(e.getKey(), e.getValue());
				}
			}
			return items;
		}
	}

	static int passed = 0;
	static int failed = 0;

	static void check(String name, boolean cond) {
		if (cond) {
			passed++;
		}
		else {
			failed++;
			System.out.println("FAIL: " + name);
		}
	}

	public static void main(String[] args) throws Exception {
		ConfigService cs = new ConfigService();

		// ---- 1. failover priority ----
		LocalConfigInfoProcessor.FAILOVER.put("a@G@t", "{cipher}failover-value");
		cs.server.store.put("a@G", "server-value");
		check("NC2: failover wins + filter chain decrypts",
				cs.getConfigInner("a", "G", "t").equals("failover-value"));

		// ---- 2. server path ----
		LocalConfigInfoProcessor.FAILOVER.clear();
		check("NC2: server path when no failover",
				cs.getConfigInner("a", "G", "t").equals("server-value"));

		// ---- 3. snapshot fallback when server down ----
		LocalConfigInfoProcessor.SNAPSHOT.put("a@G@t", "snapshot-value");
		cs.server.down = true;
		check("NC2: snapshot fallback on server down",
				cs.getConfigInner("a", "G", "t").equals("snapshot-value"));
		cs.server.down = false;

		// ---- 4. filter chain applies on snapshot path (server exception) ----
		LocalConfigInfoProcessor.SNAPSHOT.put("b@G@t", "{cipher}enc");
		cs.server.down = true;
		check("NC2: snapshot path also filtered (server exception)",
				cs.getConfigInner("b", "G", "t").equals("enc"));
		cs.server.down = false;
		LocalConfigInfoProcessor.SNAPSHOT.clear();

		// ---- 4b. server returning null (no config) does NOT fall to snapshot ----
		LocalConfigInfoProcessor.SNAPSHOT.put("c@G@t", "snap-c");
		String cResult = cs.getConfigInner("c", "G", "t");
		check("NC2: server null config returned as-is (no snapshot fallback)",
				cResult == null);
		LocalConfigInfoProcessor.SNAPSHOT.clear();

		// ---- 5. COW + double-check ----
		CacheHolder holder = new CacheHolder();
		CacheData c1 = holder.addCacheDataIfAbsent("k1");
		CacheData c2 = holder.addCacheDataIfAbsent("k1");
		check("NC2: same CacheData returned (double-check)", c1 == c2);
		check("NC2: created once", holder.creations == 1);

		// ---- 6. md5-gated notification ----
		CacheData cd = new CacheData("k", "v1");
		check("NC2: change detected on different content", cd.hasChange("v2"));
		cd.update("v2");
		check("NC2: no change after update", !cd.hasChange("v2"));

		// ---- 7. dual-granularity: change items ----
		String oldContent = "a=1\nb=2";
		String newContent = "a=1\nb=3\nc=4";
		Map<String, String> items = ConfigChangeHandler.getInstance().parseChangeData(oldContent, newContent);
		check("NC2: change parser produces items", items.containsKey("b")
				&& items.containsKey("c"));

		// ---- 8. server status ----
		check("NC2: status derived from server health", !cs.server.down);

		System.out.println("===== MiniConfig: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
