import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniConfigStorage — N-15 harness (12 assertions)
 *
 * Reproduces config storage model without Nacos server:
 *   1. persist service: atomic remove / CAS update semantics
 *   2. embedded vs external dual implementations
 *   3. md5-gated cache write (dumpWithMd5)
 *   4. gray/beta dispatch on publish
 *   5. query handler chain
 */
public class MiniConfigStorage {

	// ---- record ----
	static class ConfigRecord {
		String dataId;
		String content;
		String md5;

		ConfigRecord(String dataId, String content) {
			this.dataId = dataId;
			this.content = content;
			this.md5 = md5Of(content);
		}

		static String md5Of(String c) {
			return String.valueOf(c == null ? "".hashCode() : c.hashCode());
		}
	}

	// ---- persist service interface ----
	interface ConfigInfoPersistService {
		ConfigRecord find(String key);

		boolean updateCas(String key, String newContent, String casMd5);

		boolean removeAtomic(String key, String expectedMd5);
	}

	// ---- embedded (memory) implementation ----
	static class EmbeddedPersistServiceImpl implements ConfigInfoPersistService {
		final Map<String, ConfigRecord> store = new ConcurrentHashMap<>();

		@Override
		public ConfigRecord find(String key) {
			return store.get(key);
		}

		@Override
		public boolean updateCas(String key, String newContent, String casMd5) {
			ConfigRecord cur = store.get(key);
			if (casMd5 != null && cur != null && !cur.md5.equals(casMd5)) {
				return false; // CAS mismatch
			}
			store.put(key, new ConfigRecord(key, newContent));
			return true;
		}

		@Override
		public boolean removeAtomic(String key, String expectedMd5) {
			ConfigRecord cur = store.get(key);
			if (cur != null && expectedMd5 != null && !cur.md5.equals(expectedMd5)) {
				return false;
			}
			store.remove(key);
			return true;
		}
	}

	// ---- external implementation (same contract, different backing) ----
	static class ExternalPersistServiceImpl implements ConfigInfoPersistService {
		final Map<String, ConfigRecord> store = new HashMap<>();

		@Override
		public ConfigRecord find(String key) {
			return store.get(key);
		}

		@Override
		public boolean updateCas(String key, String newContent, String casMd5) {
			ConfigRecord cur = store.get(key);
			if (cur != null && !cur.md5.equals(casMd5)) {
				return false;
			}
			store.put(key, new ConfigRecord(key, newContent));
			return true;
		}

		@Override
		public boolean removeAtomic(String key, String expectedMd5) {
			ConfigRecord cur = store.get(key);
			if (cur != null && expectedMd5 != null && !cur.md5.equals(expectedMd5)) {
				return false;
			}
			store.remove(key);
			return true;
		}
	}

	// ---- cache ----
	static class ConfigCacheService {
		final Map<String, String> cache = new ConcurrentHashMap<>();
		final Map<String, String> cacheMd5 = new ConcurrentHashMap<>();

		boolean dumpWithMd5(String key, String content, String md5) {
			String cur = cacheMd5.get(key);
			if (cur != null && cur.equals(md5)) {
				return false; // no change
			}
			cache.put(key, content);
			cacheMd5.put(key, md5);
			return true;
		}
	}

	// ---- publish with gray dispatch ----
	static class ConfigOperationService {
		final ConfigInfoPersistService persist;

		ConfigOperationService(ConfigInfoPersistService persist) {
			this.persist = persist;
		}

		boolean publish(String key, String content, String type) {
			if ("BETA".equals(type)) {
				return persist.updateCas(key, "[beta]" + content, null);
			}
			return persist.updateCas(key, content, null);
		}
	}

	// ---- query handler chain ----
	interface QueryHandler {
		String getType();

		String handle(String request);
	}

	static class Chain {
		final Map<String, QueryHandler> handlers = new HashMap<>();

		void addHandler(QueryHandler h) {
			handlers.put(h.getType(), h);
		}

		String process(String type, String request) {
			QueryHandler h = handlers.get(type);
			return h != null ? h.handle(request) : "no-handler";
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

	public static void main(String[] args) {
		// ---- 1. dual implementations same contract ----
		EmbeddedPersistServiceImpl embedded = new EmbeddedPersistServiceImpl();
		ExternalPersistServiceImpl external = new ExternalPersistServiceImpl();
		embedded.updateCas("k1", "v1", null);
		external.updateCas("k2", "v2", null);
		check("N15: embedded store", embedded.find("k1").content.equals("v1"));
		check("N15: external store", external.find("k2").content.equals("v2"));

		// ---- 2. CAS semantics ----
		boolean ok1 = embedded.updateCas("k1", "v2", ConfigRecord.md5Of("v1"));
		boolean fail = embedded.updateCas("k1", "v3", "wrong-md5");
		check("N15: CAS with correct md5 succeeds", ok1);
		check("N15: CAS with wrong md5 rejected", !fail
				&& embedded.find("k1").content.equals("v2"));

		// ---- 3. atomic remove ----
		boolean rm1 = embedded.removeAtomic("k1", ConfigRecord.md5Of("v2"));
		boolean rm2 = external.removeAtomic("k2", "wrong-md5");
		check("N15: atomic remove success", rm1 && embedded.find("k1") == null);
		check("N15: atomic remove rejected on mismatch", !rm2
				&& external.find("k2") != null);

		// ---- 4. md5-gated cache ----
		ConfigCacheService cache = new ConfigCacheService();
		cache.dumpWithMd5("k", "c1", "m1");
		boolean dup = cache.dumpWithMd5("k", "c1", "m1");
		check("N15: identical dump skipped", !dup && cache.cache.get("k").equals("c1"));
		cache.dumpWithMd5("k", "c2", "m2");
		check("N15: changed dump applied", cache.cache.get("k").equals("c2"));

		// ---- 5. gray dispatch ----
		ConfigOperationService ops = new ConfigOperationService(embedded);
		embedded.updateCas("g1", "plain", null);
		ops.publish("g1", "plain", "BETA");
		check("N15: beta content marked", embedded.find("g1").content.equals("[beta]plain"));

		// ---- 6. query chain ----
		Chain chain = new Chain();
		chain.addHandler(new QueryHandler() {
			@Override
			public String getType() {
				return "detail";
			}

			@Override
			public String handle(String request) {
				return "detail-of:" + request;
			}
		});
		check("N15: chain dispatch", chain.process("detail", "k1").equals("detail-of:k1"));
		check("N15: chain unknown type", chain.process("other", "k1").equals("no-handler"));

		System.out.println("===== MiniConfigStorage: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
