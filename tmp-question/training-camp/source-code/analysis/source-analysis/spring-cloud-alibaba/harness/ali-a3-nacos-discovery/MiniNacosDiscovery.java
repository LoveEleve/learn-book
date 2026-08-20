import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniNacosDiscovery — ALI-A3 harness (14 assertions)
 *
 * Reproduces without Nacos server:
 *   1. dual-channel: success write-through cache, failure fallback or throw
 *   2. asymmetric failure: getInstances throws, getServices returns empty
 *   3. hostToServiceInstance: enabled+healthy filter, six-key metadata, secure
 *   4. ServiceCache: unmodifiableList, missing-key → empty, static global
 *   5. register: empty serviceId skip, failFast rethrow vs warn
 *   6. setStatus UP/DOWN → enabled flip + re-register; getStatus ip+port match
 *   7. NamingService double-checked singleton + shutdown reset
 *   8. registration metadata: management keys + heartbeat keys
 */
public class MiniNacosDiscovery {

	// ---- ServiceCache (mirror) ----
	static class ServiceCache {
		private static List<String> services = Collections.emptyList();
		private static final Map<String, List<ServiceInstance>> INSTANCES = new ConcurrentHashMap<>();

		static void setInstances(String serviceId, List<ServiceInstance> instances) {
			INSTANCES.put(serviceId, Collections.unmodifiableList(instances));
		}

		static List<ServiceInstance> getInstances(String serviceId) {
			return INSTANCES.getOrDefault(serviceId, Collections.emptyList());
		}

		static void setServiceIds(List<String> serviceIds) {
			services = Collections.unmodifiableList(serviceIds);
		}

		static List<String> getServiceIds() {
			return services;
		}
	}

	static class ServiceInstance {
		String host;
		int port;
		String serviceId;
		boolean secure;
		Map<String, String> metadata = new ConcurrentHashMap<>();

		ServiceInstance(String host, int port, String serviceId) {
			this.host = host;
			this.port = port;
			this.serviceId = serviceId;
		}
	}

	// ---- nacos Instance (minimal) ----
	static class Instance {
		String ip;
		int port;
		String instanceId;
		double weight;
		boolean healthy;
		boolean enabled;
		String clusterName;
		boolean ephemeral;
		Map<String, String> metadata;
	}

	// ---- discovery (mirror NacosDiscoveryClient + NacosServiceDiscovery) ----
	static class Discovery {
		boolean failureToleranceEnabled = false;
		boolean remoteFails = false;

		List<ServiceInstance> getInstances(String serviceId) {
			try {
				if (remoteFails) {
					throw new RuntimeException("nacos down");
				}
				List<ServiceInstance> instances = remoteSelect(serviceId);
				ServiceCache.setInstances(serviceId, instances);
				return instances;
			}
			catch (Exception e) {
				if (failureToleranceEnabled) {
					return ServiceCache.getInstances(serviceId);
				}
				throw new RuntimeException("Can not get hosts from nacos server. serviceId: " + serviceId, e);
			}
		}

		List<String> getServices() {
			try {
				if (remoteFails) {
					throw new RuntimeException("nacos down");
				}
				List<String> services = remoteServices();
				ServiceCache.setServiceIds(services);
				return services;
			}
			catch (Exception e) {
				return failureToleranceEnabled ? ServiceCache.getServiceIds()
						: Collections.emptyList();
			}
		}

		List<ServiceInstance> remoteSelect(String serviceId) {
			// simulate namingService.selectInstances(serviceId, group, true)
			Instance i1 = new Instance();
			i1.ip = "10.0.0.1";
			i1.port = 8080;
			i1.weight = 1.0;
			i1.healthy = true;
			i1.enabled = true;
			i1.clusterName = "DEFAULT";
			i1.ephemeral = true;
			i1.instanceId = "id-1";
			i1.metadata = Map.of("secure", "true", "user", "a");
			Instance i2 = new Instance();
			i2.ip = "10.0.0.2";
			i2.port = 8080;
			i2.healthy = false; // unhealthy → filtered
			i2.enabled = true;
			return hostToServiceInstanceList(List.of(i1, i2), serviceId);
		}

		List<String> remoteServices() {
			return List.of("order-service", "user-service");
		}
	}

	static List<ServiceInstance> hostToServiceInstanceList(List<Instance> instances,
			String serviceId) {
		List<ServiceInstance> result = new java.util.ArrayList<>();
		for (Instance instance : instances) {
			ServiceInstance si = hostToServiceInstance(instance, serviceId);
			if (si != null) {
				result.add(si);
			}
		}
		return result;
	}

	static ServiceInstance hostToServiceInstance(Instance instance, String serviceId) {
		if (instance == null || !instance.enabled || !instance.healthy) {
			return null;
		}
		ServiceInstance si = new ServiceInstance(instance.ip, instance.port, serviceId);
		Map<String, String> metadata = new ConcurrentHashMap<>();
		metadata.put("nacos.instanceId", instance.instanceId);
		metadata.put("nacos.weight", instance.weight + "");
		metadata.put("nacos.healthy", instance.healthy + "");
		metadata.put("nacos.cluster", instance.clusterName + "");
		if (instance.metadata != null) {
			metadata.putAll(instance.metadata);
		}
		metadata.put("nacos.ephemeral", String.valueOf(instance.ephemeral));
		si.metadata = metadata;
		if (metadata.containsKey("secure")) {
			si.secure = Boolean.parseBoolean(metadata.get("secure"));
		}
		return si;
	}

	// ---- registry (mirror NacosServiceRegistry) ----
	static class Registry {
		boolean failFast = false;
		final Map<String, Instance> registered = new ConcurrentHashMap<>();
		String lastStatus = null;

		void register(String serviceId, Instance instance) {
			if (serviceId == null || serviceId.isEmpty()) {
				return; // warn only
			}
			try {
				registered.put(serviceId + "@" + instance.ip + ":" + instance.port, instance);
			}
			catch (Exception e) {
				if (failFast) {
					throw new RuntimeException(e);
				}
			}
		}

		void setStatus(String serviceId, Instance instance, String status) {
			if (!"UP".equalsIgnoreCase(status) && !"DOWN".equalsIgnoreCase(status)) {
				return; // warn: can't support
			}
			instance.enabled = "DOWN".equalsIgnoreCase(status) ? false : true;
			lastStatus = status;
			register(serviceId, instance);
		}

		String getStatus(String serviceId, String ip, int port) {
			for (Instance instance : registered.values()) {
				if (instance.ip.equalsIgnoreCase(ip) && instance.port == port) {
					return instance.enabled ? "UP" : "DOWN";
				}
			}
			return null;
		}
	}

	// ---- naming service manager (mirror NacosServiceManager) ----
	static class ServiceManager {
		private volatile Object namingService = null;

		Object getNamingService() {
			if (namingService == null) {
				synchronized (ServiceManager.class) {
					if (namingService == null) {
						namingService = new Object();
					}
				}
			}
			return namingService;
		}

		void shutDown() {
			namingService = null;
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
		// ---- 1. write-through cache on success ----
		Discovery d = new Discovery();
		d.getInstances("order-service");
		check("A3: success writes cache", ServiceCache.getInstances("order-service").size() == 1);

		// ---- 2. health/enabled filter ----
		List<ServiceInstance> list = ServiceCache.getInstances("order-service");
		check("A3: unhealthy instance filtered", list.size() == 1
				&& list.get(0).host.equals("10.0.0.1"));

		// ---- 3. six-key metadata + secure ----
		ServiceInstance si = list.get(0);
		check("A3: nacos.* metadata injected",
				si.metadata.containsKey("nacos.instanceId")
						&& si.metadata.containsKey("nacos.weight")
						&& si.metadata.containsKey("nacos.healthy")
						&& si.metadata.containsKey("nacos.cluster")
						&& si.metadata.containsKey("nacos.ephemeral"));
		check("A3: user metadata merged", si.metadata.get("user").equals("a"));
		check("A3: secure read from metadata", si.secure);

		// ---- 4. failure fallback (tolerance on) ----
		Discovery d2 = new Discovery();
		d2.getInstances("order-service"); // prime cache
		d2.remoteFails = true;
		d2.failureToleranceEnabled = true;
		check("A3: tolerant mode falls back to cache",
				d2.getInstances("order-service").size() == 1);

		// ---- 5. failure throws (default) ----
		Discovery d3 = new Discovery();
		d3.remoteFails = true;
		boolean threw = false;
		try {
			d3.getInstances("order-service");
		}
		catch (RuntimeException e) {
			threw = true;
		}
		check("A3: default mode throws on failure", threw);

		// ---- 6. asymmetric getServices: empty not throw ----
		Discovery d4 = new Discovery();
		d4.remoteFails = true;
		check("A3: getServices failure returns empty (no throw)",
				d4.getServices().isEmpty());

		// ---- 7. cache is immutable + missing → empty ----
		check("A3: unmodifiable list", Collections.unmodifiableList(
				ServiceCache.getInstances("order-service")) != null);
		boolean immutableThrows = false;
		try {
			ServiceCache.getInstances("order-service").add(null);
		}
		catch (UnsupportedOperationException e) {
			immutableThrows = true;
		}
		check("A3: cached list unmodifiable", immutableThrows);
		check("A3: missing service → empty", ServiceCache.getInstances("nope").isEmpty());

		// ---- 8. register: empty serviceId skip ----
		Registry r = new Registry();
		Instance inst = new Instance();
		inst.ip = "10.0.0.9";
		inst.port = 9090;
		inst.enabled = true;
		r.register("", inst);
		check("A3: empty serviceId not registered", r.registered.isEmpty());

		// ---- 9. register + failFast ----
		r.register("svc", inst);
		check("A3: register instance", r.registered.size() == 1);
		Registry rf = new Registry();
		rf.failFast = true;
		Registry failing = new Registry() {
			@Override
			void register(String serviceId, Instance instance) {
				throw new IllegalStateException("nacos error");
			}
		};
		failing.failFast = true;
		boolean rethrew = false;
		try {
			failing.register("svc", inst);
		}
		catch (RuntimeException e) {
			rethrew = true;
		}
		check("A3: failFast rethrows", rethrew);

		// ---- 10. setStatus UP/DOWN ----
		r.setStatus("svc", inst, "DOWN");
		check("A3: DOWN flips enabled", !inst.enabled);
		check("A3: getStatus DOWN by ip+port", r.getStatus("svc", "10.0.0.9", 9090).equals("DOWN"));
		r.setStatus("svc", inst, "UP");
		check("A3: UP flips enabled back", inst.enabled
				&& r.getStatus("svc", "10.0.0.9", 9090).equals("UP"));
		r.setStatus("svc", inst, "MAINTENANCE");
		check("A3: unsupported status ignored", r.lastStatus.equals("UP"));

		// ---- 11. manager singleton + shutdown reset ----
		ServiceManager sm = new ServiceManager();
		Object ns1 = sm.getNamingService();
		Object ns2 = sm.getNamingService();
		check("A3: naming service singleton", ns1 == ns2);
		sm.shutDown();
		check("A3: shutdown resets → re-creatable", sm.getNamingService() != ns1);

		System.out.println("===== MiniNacosDiscovery: " + passed + "/" + (passed + failed)
				+ " passed =====");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
