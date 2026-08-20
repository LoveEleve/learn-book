import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MiniNacosRefresh — ALI-A2 harness (12 assertions)
 *
 * Reproduces without Nacos server:
 *   1. NacosContextRefresher: ready CAS once-only (multiple contexts)
 *   2. listenerMap computeIfAbsent dedup
 *   3. old-track innerReceive: refreshCountIncrement + history + snapshot + event
 *   4. NacosPropertySourceRefreshListener: ready gating + containsBean arbitration
 *   5. new track: NacosConfigRefreshEvent → RefreshEvent forwarding
 *   6. Smart rebinder: SPECIFIC_BEAN prefix matching + refreshedSet dedup
 *   7. RefreshHistory: MAX_SIZE 20 ring + MD5
 *   8. @NacosConfig annotation: refreshed=false → no listener
 */
public class MiniNacosRefresh {

	// ---- old-track refresher (mirror NacosContextRefresher) ----
	static class Refresher {
		static final AtomicLong REFRESH_COUNT = new AtomicLong(0);
		final AtomicBoolean ready = new AtomicBoolean(false);
		final Map<String, Listener> listenerMap = new ConcurrentHashMap<>(16);
		final java.util.List<String> publishedEvents = new java.util.ArrayList<>();
		final History history = new History();

		void onApplicationEvent() {
			if (this.ready.compareAndSet(false, true)) {
				registerNacosListeners();
			}
		}

		void registerNacosListeners() {
			for (String dataId : java.util.List.of("cfg-a", "cfg-b")) {
				registerListener("DEFAULT_GROUP", dataId);
			}
		}

		void registerListener(String group, String dataId) {
			String key = dataId + "," + group;
			Listener listener = listenerMap.computeIfAbsent(key, k -> new Listener() {
				@Override
				public void innerReceive(String dataId, String group, String configInfo) {
					REFRESH_COUNT.incrementAndGet();
					history.addRefreshRecord(dataId, group, configInfo);
					publishedEvents.add(dataId + "@" + group);
				}
			});
		}

		interface Listener {
			void innerReceive(String d, String g, String c);
		}

		static long getRefreshCount() {
			return REFRESH_COUNT.get();
		}
	}

	// ---- history (mirror NacosRefreshHistory) ----
	static class History {
		static final int MAX_SIZE = 20;
		final java.util.LinkedList<String> records = new java.util.LinkedList<>();

		void addRefreshRecord(String dataId, String group, String data) {
			records.addFirst(dataId + "@" + group + ":" + md5(data));
			if (records.size() > MAX_SIZE) {
				records.removeLast();
			}
		}

		static String md5(String data) {
			if (data == null || data.isEmpty()) {
				return null;
			}
			try {
				java.security.MessageDigest md = java.security.MessageDigest
						.getInstance("MD5");
				return new java.math.BigInteger(1,
						md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
						.toString(16);
			}
			catch (Exception e) {
				return "md5-fail";
			}
		}
	}

	// ---- new-track event listener (mirror NacosConfigRefreshEventListener) ----
	static class EventChain {
		final java.util.List<String> forwarded = new java.util.ArrayList<>();

		void onNacosConfigRefreshEvent(String dataId) {
			forwarded.add("RefreshEvent:" + dataId); // publish RefreshEvent
		}
	}

	// ---- old-track listener w/ arbitration (mirror NacosPropertySourceRefreshListener) ----
	static class RefreshListener {
		final AtomicBoolean ready = new AtomicBoolean(false);
		final Map<String, String> beans = new ConcurrentHashMap<>();

		boolean handle(String dataId, String group, boolean hasNewTrackListener) {
			if (!ready.get()) {
				return false; // don't handle before ready
			}
			if (hasNewTrackListener) {
				return false; // arbitration: new track takes over, no env replace
			}
			// replace source
			beans.put(dataId + "," + group, "replaced");
			return true;
		}
	}

	// ---- smart rebinder (mirror SmartConfigurationPropertiesRebinder SPECIFIC_BEAN) ----
	static class SmartRebinder {
		final Map<String, String> beanPrefix = new ConcurrentHashMap<>();
		final java.util.List<String> rebound = new java.util.ArrayList<>();

		void register(String beanName, String prefix) {
			beanPrefix.put(beanName, prefix);
		}

		void onEnvironmentChange(java.util.List<String> changeKeys) {
			Set<String> refreshedSet = new java.util.HashSet<>();
			beanPrefix.forEach((name, prefix) -> changeKeys.forEach(key -> {
				if (key.startsWith(prefix) && refreshedSet.add(name)) {
					rebound.add(name);
				}
			}));
		}
	}

	// ---- annotation processor (mirror NacosAnnotationProcessor refreshed=false) ----
	static class AnnotationProcessor {
		int listeners = 0;
		final Map<String, String> cache = new ConcurrentHashMap<>();

		String getContent(String dataId, String group, boolean refreshed) {
			String key = dataId + "@" + group;
			if (cache.containsKey(key)) {
				return cache.get(key);
			}
			synchronized (this) {
				if (!cache.containsKey(key)) {
					String content = "content-of-" + dataId; // getConfig
					cache.put(key, content);
					if (refreshed) {
						listeners++;
					}
				}
				return cache.get(key);
			}
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
		// ---- 1. ready CAS once-only ----
		Refresher r = new Refresher();
		r.onApplicationEvent();
		r.onApplicationEvent(); // second ready event ignored
		check("A2: ready CAS registers listeners once", r.listenerMap.size() == 2);

		// ---- 2. listener dedup ----
		r.registerListener("DEFAULT_GROUP", "cfg-a"); // duplicate
		check("A2: computeIfAbsent dedups listener", r.listenerMap.size() == 2);

		// ---- 3. old-track callback chain ----
		Refresher.REFRESH_COUNT.set(0);
		r.listenerMap.get("cfg-a,DEFAULT_GROUP").innerReceive("cfg-a", "DEFAULT_GROUP", "v2");
		check("A2: refreshCount incremented", Refresher.getRefreshCount() == 1);
		check("A2: history recorded", r.history.records.size() == 1);
		check("A2: event published", r.publishedEvents.get(0).equals("cfg-a@DEFAULT_GROUP"));

		// ---- 4. refresh throttling linkage (A1) ----
		long before = Refresher.getRefreshCount();
		r.listenerMap.get("cfg-b,DEFAULT_GROUP").innerReceive("cfg-b", "DEFAULT_GROUP", "vb");
		check("A2: refreshCount drives A1 throttling gate", Refresher.getRefreshCount() == before + 1);

		// ---- 5. ready gating in old-track listener ----
		RefreshListener rl = new RefreshListener();
		check("A2: event before ready ignored", !rl.handle("a", "G", false));
		rl.ready.set(true);
		check("A2: event after ready handled", rl.handle("a", "G", false)
				&& rl.beans.get("a,G").equals("replaced"));

		// ---- 6. arbitration: new track takes over ----
		RefreshListener rl2 = new RefreshListener();
		rl2.ready.set(true);
		boolean replaced = rl2.handle("a", "G", true);
		check("A2: arbitration defers to new track (no env replace)", !replaced
				&& rl2.beans.isEmpty());

		// ---- 7. new track event chain ----
		EventChain ec = new EventChain();
		ec.onNacosConfigRefreshEvent("cfg-a");
		check("A2: NacosConfigRefreshEvent → RefreshEvent forwarded",
				ec.forwarded.get(0).equals("RefreshEvent:cfg-a"));

		// ---- 8. SPECIFIC_BEAN prefix matching + dedup ----
		SmartRebinder sr = new SmartRebinder();
		sr.register("orderProps", "order.");
		sr.register("userProps", "user.");
		sr.onEnvironmentChange(java.util.List.of("order.timeout=3", "order.timeout=5"));
		check("A2: prefix matched only affected bean", sr.rebound.equals(
				java.util.List.of("orderProps")));
		check("A2: same bean not rebound twice", sr.rebound.size() == 1);
		sr.onEnvironmentChange(java.util.List.of("user.name=x"));
		check("A2: second key hits its own bean", sr.rebound.equals(
				java.util.List.of("orderProps", "userProps")));

		// ---- 9. history ring + MD5 ----
		History h = new History();
		for (int i = 0; i < 25; i++) {
			h.addRefreshRecord("d" + i, "G", "data" + i);
		}
		check("A2: history capped at 20", h.records.size() == 20);
		check("A2: history keeps newest first", h.records.getFirst().startsWith("d24@G:"));
		check("A2: MD5 is 32 hex chars",
				History.md5("hello").matches("[0-9a-f]{32}"));

		// ---- 10. annotation refreshed=false → no listener ----
		AnnotationProcessor ap = new AnnotationProcessor();
		ap.getContent("a", "G", false);
		ap.getContent("b", "G", true);
		check("A2: refreshed=false registers no listener, true does", ap.listeners == 1);
		check("A2: groupKeyCache double-check returns cached", ap.getContent("a", "G", true)
				.equals("content-of-a") && ap.listeners == 1);

		System.out.println("===== MiniNacosRefresh: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
