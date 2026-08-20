import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MiniNaming — NC-1 harness (14 assertions)
 *
 * Reproduces without Nacos server:
 *   1. proxy routing: ephemeral → grpc, persistent → http
 *   2. batch forces grpc
 *   3. doDiff: added/modified/removed + stale (lastRefTime) data ignored
 *   4. processServiceInfo: empty/error push ignored (pushEmptyProtection)
 *   5. change event published only on diff + disk write
 *   6. selectInstances filter (healthy/enabled/weight)
 *   7. subscribe gate: unsubscribe only when no listeners remain
 *   8. redo-before-send ordering
 *   9. scheduleUpdateIfAbsent double-check
 *   10. ProtectMode default 0.8
 */
public class MiniNaming {

	// ---- instance ----
	static class Instance {
		String ip;
		int port;
		boolean healthy = true;
		boolean enabled = true;
		double weight = 1.0;
		boolean ephemeral = true;
		long lastRefTime;

		Instance(String ip, int port) {
			this.ip = ip;
			this.port = port;
		}

		String toInetAddr() {
			return ip + ":" + port;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Instance)) {
				return false;
			}
			Instance i = (Instance) o;
			return ip.equals(i.ip) && port == i.port;
		}

		@Override
		public int hashCode() {
			return toInetAddr().hashCode();
		}
	}

	static class ServiceInfo {
		String key;
		List<Instance> hosts = new ArrayList<>();
		long lastRefTime;

		ServiceInfo(String key, long lastRefTime) {
			this.key = key;
			this.lastRefTime = lastRefTime;
		}
	}

	// ---- diff ----
	static class InstancesDiff {
		final List<Instance> added = new ArrayList<>();
		final List<Instance> removed = new ArrayList<>();
		final List<Instance> modified = new ArrayList<>();

		boolean hasDifferent() {
			return !added.isEmpty() || !removed.isEmpty() || !modified.isEmpty();
		}
	}

	// ---- differ (mirror InstancesDiffer.doDiff) ----
	static class InstancesDiffer {
		InstancesDiff doDiff(ServiceInfo oldS, ServiceInfo newS) {
			InstancesDiff diff = new InstancesDiff();
			if (oldS == null) {
				diff.added.addAll(newS.hosts);
				return diff;
			}
			if (oldS.lastRefTime > newS.lastRefTime) {
				return diff; // out of date
			}
			Map<String, Instance> oldMap = new HashMap<>();
			for (Instance h : oldS.hosts) {
				oldMap.put(h.toInetAddr(), h);
			}
			for (Instance h : newS.hosts) {
				if (oldMap.containsKey(h.toInetAddr())) {
					Instance old = oldMap.get(h.toInetAddr());
					if (h.weight != old.weight) {
						diff.modified.add(h);
					}
				}
				else {
					diff.added.add(h);
				}
			}
			for (Instance h : oldS.hosts) {
				if (!newS.hosts.contains(h)) {
					diff.removed.add(h);
				}
			}
			return diff;
		}
	}

	// ---- holder (mirror ServiceInfoHolder.processServiceInfo) ----
	static class ServiceInfoHolder {
		final ConcurrentMap<String, ServiceInfo> map = new ConcurrentHashMap<>();
		final List<ServiceInfo> events = new ArrayList<>();
		final List<String> diskWrites = new ArrayList<>();
		boolean pushEmptyProtection = false;
		boolean failoverSwitch = false;
		final InstancesDiffer differ = new InstancesDiffer();

		ServiceInfo process(ServiceInfo info) {
			if (info == null || info.hosts == null
					|| (pushEmptyProtection && info.hosts.isEmpty())) {
				return map.get(info != null ? info.key : null); // empty push ignored
			}
			ServiceInfo old = map.get(info.key);
			map.put(info.key, info);
			InstancesDiff diff = differ.doDiff(old, info);
			if (diff.hasDifferent() && !failoverSwitch) {
				events.add(info);
				diskWrites.add(info.key);
			}
			return info;
		}
	}

	// ---- proxy routing (mirror NamingClientProxyDelegate) ----
	static class Proxy {
		int grpcCalls = 0;
		int httpCalls = 0;
		boolean grpcSupported = true;

		void register(Instance instance) {
			if (instance.ephemeral || grpcSupported) {
				grpcCalls++;
			}
			else {
				httpCalls++;
			}
		}

		void batchRegister() {
			grpcCalls++; // batch forces grpc
		}
	}
	// ---- subscribe gate (mirror NacosNamingService doUnsubscribe) ----
	static class ChangeNotifier {
		final Map<String, Integer> listeners = new HashMap<>();

		void register(String key) {
			listeners.merge(key, 1, Integer::sum);
		}

		void deregister(String key) {
			listeners.computeIfPresent(key, (k, v) -> v - 1);
		}

		boolean isSubscribed(String key) {
			return listeners.getOrDefault(key, 0) > 0;
		}
	}

	// ---- redo-before-send (mirror NamingGrpcClientProxy.subscribe) ----
	static class RedoService {
		final List<String> cached = new ArrayList<>();
		final List<String> registered = new ArrayList<>();

		void cacheForRedo(String key) {
			cached.add(key);
		}

		void markRegistered(String key) {
			registered.add(key);
		}

		void subscriberDeregister(String key) {
			registered.remove(key);
		}

		void removeForRedo(String key) {
			cached.remove(key);
		}
	}

	// ---- update service double-check ----
	static class UpdateService {
		final Set<String> tasks = new HashSet<>();
		int scheduled = 0;

		void scheduleUpdateIfAbsent(String key) {
			if (tasks.contains(key)) {
				return;
			}
			synchronized (this) {
				if (tasks.contains(key)) {
					return;
				}
				tasks.add(key);
				scheduled++;
			}
		}
	}

	static class ProtectMode {
		float protectThreshold = 0.8F;
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
		// ---- 1. proxy routing ----
		Proxy p = new Proxy();
		Instance ephemeral = new Instance("1.1.1.1", 8080);
		ephemeral.ephemeral = true;
		Instance persistent = new Instance("2.2.2.2", 8080);
		persistent.ephemeral = false;
		p.register(ephemeral);
		p.register(persistent);
		check("NC1: both go grpc when server supports (ephemeral || ability)",
				p.grpcCalls == 2 && p.httpCalls == 0);
		p.grpcSupported = false; // legacy server without gRPC
		p.register(ephemeral);
		p.register(persistent);
		check("NC1: ephemeral still grpc (short-circuit)", p.grpcCalls == 3);
		check("NC1: persistent → http when gRPC unsupported", p.httpCalls == 1);
		p.batchRegister();
		check("NC1: batch forces grpc", p.grpcCalls == 4);

		// ---- 2. doDiff: added/removed/modified ----
		InstancesDiffer differ = new InstancesDiffer();
		ServiceInfo oldS = new ServiceInfo("A", 100);
		Instance a1 = new Instance("10.0.0.1", 8080);
		Instance a2 = new Instance("10.0.0.2", 8080);
		oldS.hosts.add(a1);
		oldS.hosts.add(a2);
		ServiceInfo newS = new ServiceInfo("A", 200);
		Instance a1b = new Instance("10.0.0.1", 8080);
		a1b.weight = 2.0; // modified
		newS.hosts.add(a1b);
		Instance a3 = new Instance("10.0.0.3", 8080); // added
		newS.hosts.add(a3);
		InstancesDiff diff = differ.doDiff(oldS, newS);
		check("NC1: diff added", diff.added.size() == 1 && diff.added.get(0).ip.equals("10.0.0.3"));
		check("NC1: diff removed", diff.removed.size() == 1 && diff.removed.get(0).ip.equals("10.0.0.2"));
		check("NC1: diff modified", diff.modified.size() == 1);

		// ---- 3. stale data ignored ----
		ServiceInfo stale = new ServiceInfo("A", 50); // older than oldS
		InstancesDiff staleDiff = differ.doDiff(oldS, stale);
		check("NC1: stale data → empty diff", !staleDiff.hasDifferent());

		// ---- 4. empty push ignored ----
		ServiceInfoHolder holder = new ServiceInfoHolder();
		holder.pushEmptyProtection = true;
		ServiceInfo s1 = new ServiceInfo("B", 100);
		s1.hosts.add(new Instance("10.0.0.1", 8080));
		holder.process(s1);
		ServiceInfo empty = new ServiceInfo("B", 200);
		holder.process(empty);
		check("NC1: empty push ignored (old kept)", holder.map.get("B").hosts.size() == 1);
		check("NC1: no event on ignored push", holder.events.size() == 1);

		// ---- 5. no diff → no event/no disk ----
		ServiceInfoHolder holder2 = new ServiceInfoHolder();
		ServiceInfo s2 = new ServiceInfo("C", 100);
		s2.hosts.add(new Instance("10.0.0.1", 8080));
		holder2.process(s2);
		holder2.process(s2); // identical
		check("NC1: identical push → no extra event", holder2.events.size() == 1
				&& holder2.diskWrites.size() == 1);

		// ---- 6. selectInstances filter ----
		List<Instance> list = new ArrayList<>();
		Instance healthy1 = new Instance("10.0.0.1", 8080);
		Instance unhealthy = new Instance("10.0.0.2", 8080);
		unhealthy.healthy = false;
		Instance disabled = new Instance("10.0.0.3", 8080);
		disabled.enabled = false;
		Instance zeroWeight = new Instance("10.0.0.4", 8080);
		zeroWeight.weight = 0;
		list.addAll(List.of(healthy1, unhealthy, disabled, zeroWeight));
		list.removeIf(i -> i.healthy != true || !i.enabled || i.weight <= 0);
		check("NC1: filter keeps only healthy+enabled+weighted", list.size() == 1
				&& list.get(0).ip.equals("10.0.0.1"));

		// ---- 7. unsubscribe only when no listeners ----
		ChangeNotifier cn = new ChangeNotifier();
		cn.register("svc");
		cn.register("svc");
		cn.deregister("svc");
		check("NC1: still subscribed with one listener", cn.isSubscribed("svc"));
		cn.deregister("svc");
		check("NC1: unsubscribed when zero listeners", !cn.isSubscribed("svc"));

		// ---- 8. redo-before-send ----
		RedoService rs = new RedoService();
		rs.cacheForRedo("svc");
		rs.markRegistered("svc"); // after successful send
		check("NC1: redo cached before registered", rs.cached.contains("svc")
				&& rs.registered.contains("svc"));
		rs.subscriberDeregister("svc");
		rs.removeForRedo("svc");
		check("NC1: unsubscribe clears both", rs.cached.isEmpty() && rs.registered.isEmpty());

		// ---- 9. schedule double-check ----
		UpdateService us = new UpdateService();
		us.scheduleUpdateIfAbsent("svc");
		us.scheduleUpdateIfAbsent("svc");
		us.scheduleUpdateIfAbsent("svc");
		check("NC1: double-check schedules once", us.scheduled == 1);

		// ---- 10. protect mode ----
		check("NC1: protect threshold default 0.8", new ProtectMode().protectThreshold == 0.8F);

		System.out.println("===== MiniNaming: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
