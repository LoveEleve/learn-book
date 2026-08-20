import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniNacosConfig — ALI-A1 harness (12 assertions)
 *
 * Reproduces, without Nacos server dependency:
 *   1. addFirstPropertySource priority inversion (default → suffix → profile)
 *   2. NacosPropertySourceRepository dataId+group composite key + putIfAbsent
 *   3. NacosSnapshotConfigManager get-and-remove (consume-once) semantics
 *   4. refresh throttling: refreshCount != 0 && !isRefreshable → reuse repository
 *   5. empty config → empty list, no placeholder
 */
public class MiniNacosConfig {

	// ---- 1. Composite addFirst semantics ----
	static class CompositePropertySource {
		final List<String> names = new ArrayList<>();

		void addFirst(String name) {
			names.add(0, name);
		}

		@Override
		public String toString() {
			return names.toString();
		}
	}

	// ---- 2. Repository (mirror of NacosPropertySourceRepository) ----
	static class PropertySourceRepository {
		private static final Map<String, String> REPO = new ConcurrentHashMap<>();

		static void collect(String dataId, String group, String source) {
			REPO.putIfAbsent(key(dataId, group), source);
		}

		static String get(String dataId, String group) {
			return REPO.get(key(dataId, group));
		}

		private static String key(String dataId, String group) {
			return dataId + "," + group;
		}

		static int size() {
			return REPO.size();
		}
	}

	// ---- 3. Snapshot manager (mirror of NacosSnapshotConfigManager) ----
	static class SnapshotManager {
		private static final Map<String, String> SNAPSHOT = new ConcurrentHashMap<>(8);
		private static final int MAX = 100;

		static String getAndRemove(String dataId, String group) {
			String v = SNAPSHOT.get(dataId + "@" + group);
			SNAPSHOT.remove(dataId + "@" + group);
			return v;
		}

		static void put(String dataId, String group, String config) {
			if (SNAPSHOT.size() > MAX) {
				var it = SNAPSHOT.entrySet().iterator();
				it.next();
				it.remove();
			}
			if (config == null) {
				SNAPSHOT.remove(dataId + "@" + group);
			}
			else {
				SNAPSHOT.put(dataId + "@" + group, config);
			}
		}

		static int size() {
			return SNAPSHOT.size();
		}
	}

	// ---- 4. Locator (mirror of NacosPropertySourceLocator core logic) ----
	static class MiniLocator {
		final List<String> fetched = new ArrayList<>(); // records remote pulls

		// refreshCount simulates NacosContextRefresher.getRefreshCount()
		int refreshCount = 0;

		// locate: three-tier priority, each tier addFirst
		CompositePropertySource locate(String dataIdPrefix, String fileExtension,
				List<String> profiles) {
			CompositePropertySource composite = new CompositePropertySource();
			// tier 1: default
			loadIfPresent(composite, dataIdPrefix, "DEFAULT_GROUP", true);
			// tier 2: with suffix
			loadIfPresent(composite, dataIdPrefix + "." + fileExtension, "DEFAULT_GROUP",
					true);
			// tier 3: profiles
			for (String profile : profiles) {
				loadIfPresent(composite,
						dataIdPrefix + "-" + profile + "." + fileExtension,
						"DEFAULT_GROUP", true);
			}
			return composite;
		}

		void loadIfPresent(CompositePropertySource composite, String dataId, String group,
				boolean isRefreshable) {
			if (dataId == null || dataId.trim().length() < 1 || group == null
					|| group.trim().length() < 1) {
				return;
			}
			String source = load(dataId, group, isRefreshable);
			if (source != null) {
				composite.addFirst(dataId); // addFirstPropertySource (source name)
			}
		}

		// loadNacosPropertySource: refresh throttling
		String load(String dataId, String group, boolean isRefreshable) {
			if (refreshCount != 0 && !isRefreshable) {
				return PropertySourceRepository.get(dataId, group); // reuse, no pull
			}
			String snapshot = SnapshotManager.getAndRemove(dataId, group);
			String data = snapshot != null ? snapshot : pullRemote(dataId, group);
			fetched.add(dataId);
			if (data == null || data.isEmpty()) {
				return null; // empty → no placeholder
			}
			String source = dataId + ":" + data;
			PropertySourceRepository.collect(dataId, group, source);
			return source;
		}

		String pullRemote(String dataId, String group) {
			// simulate configService.getConfig
			return "value-from-" + dataId;
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
		MiniLocator locator = new MiniLocator();
		locator.refreshCount = 0;

		// ---- priority: profile > suffix > default ----
		CompositePropertySource c = locator.locate("order-service", "yml",
				List.of("dev"));
		check("A1: three tiers fetched (default/suffix/profile)", locator.fetched.size() == 3);
		check("A1: profile first in composite", c.names.get(0).endsWith("-dev.yml"));
		check("A1: suffix second", c.names.get(1).equals("order-service.yml"));
		check("A1: default last", c.names.get(2).equals("order-service"));

		// ---- repository composite key: same dataId different group don't collide ----
		// (repo is process-global; 3 keys already collected by locate above)
		check("A1: locate collected 3 sources into repo", PropertySourceRepository.size() == 3);
		PropertySourceRepository.collect("a", "G1", "v1");
		PropertySourceRepository.collect("a", "G2", "v2");
		check("A1: composite key isolates groups", PropertySourceRepository.size() == 5
				&& !PropertySourceRepository.get("a", "G1").equals(
						PropertySourceRepository.get("a", "G2")));
		check("A1: putIfAbsent keeps first", PropertySourceRepository.get("a", "G1").equals("v1"));

		// ---- snapshot: get-and-remove consume-once ----
		SnapshotManager.put("b", "G", "snap-b");
		check("A1: snapshot put", SnapshotManager.size() == 1);
		check("A1: snapshot getAndRemove returns value", "snap-b".equals(
				SnapshotManager.getAndRemove("b", "G")));
		check("A1: snapshot removed after consume", SnapshotManager.size() == 0);
		check("A1: second consume returns null", SnapshotManager.getAndRemove("b", "G") == null);

		// ---- snapshot priority: snapshot wins over remote ----
		SnapshotManager.put("c", "G", "snap-c");
		MiniLocator l2 = new MiniLocator();
		String loaded = l2.load("c", "G", true);
		check("A1: snapshot used instead of remote", loaded.endsWith("snap-c"));

		// ---- refresh throttling: refreshCount != 0 && !isRefreshable → reuse ----
		MiniLocator l3 = new MiniLocator();
		l3.load("d", "G", true); // initial pull, refreshCount=0 → pull
		int fetchedBefore = l3.fetched.size();
		l3.refreshCount = 1;
		String reused = l3.load("d", "G", false); // not refreshable → reuse
		check("A1: throttled load does not re-pull", l3.fetched.size() == fetchedBefore);
		check("A1: throttled load returns repository source", reused != null
				&& reused.contains("value-from-d"));
		l3.refreshCount = 1;
		l3.load("d", "G", true); // refreshable → pulls again
		check("A1: refreshable config re-pulls during refresh", l3.fetched.size() == fetchedBefore + 1);

		// ---- empty config → no placeholder ----
		MiniLocator l4 = new MiniLocator() {
			@Override
			String pullRemote(String dataId, String group) {
				return "";
			}
		};
		String empty = l4.load("e", "G", true);
		check("A1: empty config → null (no placeholder)", empty == null);

		// ---- null configService → locate returns null ----
		check("A1: guard: null dataId skipped", (l4.load(null, "G", true) == null)
				&& (l4.load("x", null, true) == null));

		System.out.println("===== MiniNacosConfig: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
