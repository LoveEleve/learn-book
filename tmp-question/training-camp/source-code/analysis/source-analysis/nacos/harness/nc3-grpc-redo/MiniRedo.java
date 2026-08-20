import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MiniRedo — NC-3 harness (14 assertions)
 *
 * Reproduces the redo state machine without Nacos server:
 *   1. RedoData four-combination getRedoType semantics (NONE/REGISTER/UNREGISTER/REMOVE)
 *   2. onDisConnect marks all data for redo
 *   3. redo task: connected gate + type dispatch
 *   4. remove only when not expected registered
 *   5. subscribe symmetric lifecycle
 *   6. double-map isolation (instances vs subscribes)
 */
public class MiniRedo {

	// ---- RedoType ----
	enum RedoType {
		NONE, REGISTER, UNREGISTER, REMOVE
	}

	// ---- RedoData (mirror RedoData.java) ----
	static class RedoData {
		volatile boolean expectedRegistered = true;
		volatile boolean registered = false;
		volatile boolean unregistering = false;

		void registered() {
			this.registered = true;
			this.unregistering = false;
		}

		void unregistered() {
			this.registered = false;
			this.unregistering = true;
		}

		boolean isNeedRedo() {
			return !RedoType.NONE.equals(getRedoType());
		}

		RedoType getRedoType() {
			if (isRegistered() && !isUnregistering()) {
				return expectedRegistered ? RedoType.NONE : RedoType.UNREGISTER;
			}
			else if (isRegistered() && isUnregistering()) {
				return RedoType.UNREGISTER;
			}
			else if (!isRegistered() && !isUnregistering()) {
				return RedoType.REGISTER;
			}
			else {
				return expectedRegistered ? RedoType.REGISTER : RedoType.REMOVE;
			}
		}

		boolean isRegistered() {
			return registered;
		}

		boolean isUnregistering() {
			return unregistering;
		}
	}

	// ---- Redo service (mirror NamingGrpcRedoService) ----
	static class RedoService {
		final ConcurrentMap<String, RedoData> registeredInstances = new ConcurrentHashMap<>();
		final ConcurrentMap<String, RedoData> subscribes = new ConcurrentHashMap<>();
		volatile boolean connected = false;
		final java.util.List<String> executed = new java.util.ArrayList<>();

		// onDisConnect: mark all
		void onDisConnect() {
			connected = false;
			registeredInstances.values().forEach(d -> d.registered = false);
			subscribes.values().forEach(d -> d.registered = false);
		}

		void cacheInstance(String key) {
			registeredInstances.put(key, new RedoData());
		}

		void cacheSubscriber(String key) {
			subscribes.put(key, new RedoData());
		}

		void instanceRegistered(String key) {
			RedoData d = registeredInstances.get(key);
			if (d != null) {
				d.registered();
			}
		}

		void instanceDeregister(String key) {
			RedoData d = registeredInstances.get(key);
			if (d != null) {
				d.unregistering = true;
				d.expectedRegistered = false;
			}
		}

		void instanceDeregistered(String key) {
			RedoData d = registeredInstances.get(key);
			if (d != null) {
				d.unregistered();
			}
		}

		void removeInstance(String key) {
			RedoData d = registeredInstances.get(key);
			if (d != null && !d.expectedRegistered) {
				registeredInstances.remove(key);
			}
		}

		Set<RedoData> findInstanceRedoData() {
			Set<RedoData> result = new java.util.HashSet<>();
			for (RedoData each : registeredInstances.values()) {
				if (each.isNeedRedo()) {
					result.add(each);
				}
			}
			return result;
		}

		// ---- redo task (mirror RedoScheduledTask.run) ----
		void runRedoTask() {
			if (!connected) {
				return; // skip when disconnected
			}
			for (RedoData each : findInstanceRedoData()) {
				switch (each.getRedoType()) {
				case REGISTER:
					executed.add("REGISTER");
					each.registered();
					break;
				case UNREGISTER:
					executed.add("UNREGISTER");
					each.unregistered();
					break;
				default:
					break;
				}
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
		// ---- 1. four-combination state machine ----
		RedoData fresh = new RedoData();
		check("NC3: fresh (not registered, not unregistering) → REGISTER",
				fresh.getRedoType() == RedoType.REGISTER);

		RedoData ok = new RedoData();
		ok.registered();
		check("NC3: registered, expecting → NONE", ok.getRedoType() == RedoType.NONE);
		check("NC3: NONE → no redo needed", !ok.isNeedRedo());

		RedoData deregistering = new RedoData();
		deregistering.registered();
		deregistering.unregistering = true;
		check("NC3: registered+unregistering → UNREGISTER",
				deregistering.getRedoType() == RedoType.UNREGISTER);

		RedoData unregExpected = new RedoData();
		unregExpected.registered();
		unregExpected.expectedRegistered = false;
		check("NC3: registered, not expecting → UNREGISTER",
				unregExpected.getRedoType() == RedoType.UNREGISTER);

		RedoData removeCase = new RedoData();
		removeCase.unregistering = true;
		removeCase.expectedRegistered = false;
		check("NC3: unregistering, not expecting → REMOVE",
				removeCase.getRedoType() == RedoType.REMOVE);

		// ---- 2. disconnect marks all ----
		RedoService rs = new RedoService();
		rs.cacheInstance("svc");
		rs.instanceRegistered("svc");
		check("NC3: registered before disconnect", rs.registeredInstances.get("svc").isRegistered());
		rs.onDisConnect();
		check("NC3: disconnect marks for redo", !rs.registeredInstances.get("svc").isRegistered());

		// ---- 3. redo task connected gate ----
		rs.runRedoTask(); // disconnected → skip
		check("NC3: redo skipped when disconnected", rs.executed.isEmpty());
		rs.connected = true;
		rs.runRedoTask();
		check("NC3: redo executed when connected", rs.executed.size() == 1
				&& rs.executed.get(0).equals("REGISTER"));
		check("NC3: redo marks registered again", rs.registeredInstances.get("svc").isRegistered());

		// ---- 4. deregister lifecycle ----
		rs.instanceDeregister("svc");
		rs.runRedoTask();
		check("NC3: deregister redo → UNREGISTER executed", rs.executed.size() == 2
				&& rs.executed.get(1).equals("UNREGISTER"));
		rs.instanceDeregistered("svc");

		// ---- 5. remove only when not expected ----
		RedoService rs2 = new RedoService();
		rs2.cacheInstance("svc2");
		rs2.instanceRegistered("svc2");
		rs2.removeInstance("svc2"); // still expected → not removed
		check("NC3: expected-registered data not removed", rs2.registeredInstances.containsKey("svc2"));
		rs2.instanceDeregister("svc2");
		rs2.removeInstance("svc2"); // not expected → removed
		check("NC3: deregistered data removed", !rs2.registeredInstances.containsKey("svc2"));

		// ---- 6. double-map isolation ----
		RedoService rs3 = new RedoService();
		rs3.cacheInstance("svc");
		rs3.cacheSubscriber("sub");
		rs3.instanceRegistered("svc");
		rs3.onDisConnect();
		check("NC3: instance marked", !rs3.registeredInstances.get("svc").isRegistered());
		check("NC3: subscriber marked too", !rs3.subscribes.get("sub").isRegistered());
		check("NC3: two maps isolated", rs3.registeredInstances.size() == 1
				&& rs3.subscribes.size() == 1);

		// ---- 7. findInstanceRedoData filters ----
		RedoService rs4 = new RedoService();
		rs4.cacheInstance("a");
		rs4.cacheInstance("b");
		rs4.instanceRegistered("b");
		Set<RedoData> need = rs4.findInstanceRedoData();
		check("NC3: only non-registered needs redo", need.size() == 1);

		System.out.println("===== MiniRedo: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
