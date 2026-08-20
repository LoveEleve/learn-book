public class MiniServiceRefer {

    // ================= URL (简化) =================
    static class URL {
        final String protocol, address, iface;
        final java.util.Map<String, String> params = new java.util.HashMap<>();
        URL(String protocol, String address, String iface) { this.protocol = protocol; this.address = address; this.iface = iface; }
        boolean isRegistry() { return "registry".equals(protocol) || "zookeeper".equals(protocol) || "nacos".equals(protocol); }
        String get(String k, String dft) { return params.getOrDefault(k, dft); }
        public String toString() { return protocol + "://" + address + "/" + iface; }
    }

    // ---- A: 装配面 (ReferenceConfig.get → init → createProxy 双路径 + injvm 兜底) ----
    static class ReferenceConfig {
        java.util.List<URL> urls = new java.util.ArrayList<>();
        URL invokerUrl;
        boolean initialized;
        final String iface;
        ReferenceConfig(String iface) { this.iface = iface; }

        String get(boolean check) {
            if (!initialized) {
                init();
                if (check) checkInvokerAvailable();   // fail-fast
            }
            return "PROXY(" + invokerUrl + ")";
        }

        void init() {
            // createProxy 双路径: 有 url → 直连 parseUrl; 否则 → 注册中心聚合
            if (!urls.isEmpty() && urls.get(0) != null) {
                // 直连路径 (用户指定 URL)
            } else {
                aggregateUrlFromRegistry(); // 注册中心聚合
            }
            createInvoker();  // 三分支
            initialized = true;
        }

        void aggregateUrlFromRegistry() {
            // loadRegistries → 注册中心 URL; 空 → injvm 兜底 (本地始终可引)
            urls.add(new URL("registry", "zookeeper://192.168.1.10:2181", iface));
            if (urls.isEmpty()) urls.add(new URL("injvm", "localhost:0", iface)); // shouldJvmRefer
        }

        void createInvoker() {
            if (urls.size() == 1) {
                invokerUrl = urls.get(0);
            } else {
                // 多注册中心 → 默认 ZoneAwareCluster (CLUSTER_KEY)
                invokerUrl = urls.get(0);
            }
        }

        void checkInvokerAvailable() {
            if (invokerUrl == null) throw new IllegalStateException("No provider available for the service " + iface);
        }
    }

    // ---- B: 注册中心引用面 (RegistryProtocol.refer → doRefer → MigrationInvoker 三态) ----
    static class RegistryProtocol {
        static Object refer(ReferenceConfig cfg, URL registryUrl) {
            // group="a,b"/"*" → mergeable 集群
            if (registryUrl.get("group", "").contains(",") || "*".equals(registryUrl.get("group", ""))) {
                return "MERGEABLE_CLUSTER_INVOKER(" + registryUrl + ")";
            }
            return doRefer(cfg, registryUrl);
        }

        static Object doRefer(ReferenceConfig cfg, URL registryUrl) {
            MigrationInvoker mi = new MigrationInvoker();
            return mi.decideInvoker(); // 3.x 迁移: 按规则选接口级/应用级
        }
    }

    enum MigrationStep { FORCE_INTERFACE, APPLICATION_FIRST, FORCE_APPLICATION }

    static class MigrationInvoker {
        // 双 invoker: interfaceInvoker (接口级订阅) + applicationInvoker (应用级发现)
        MigrationStep step = MigrationStep.APPLICATION_FIRST; // 默认规则
        String decideInvoker() {
            switch (step) {
                case FORCE_INTERFACE:   return "INTERFACE_INVOKER(RegistryDirectory-subscribe)";
                case FORCE_APPLICATION: return "APPLICATION_INVOKER(ServiceDiscovery)";
                default:                return "MIGRATION_INVOKER(双订阅共存, 规则切换)";
            }
        }
    }

    // ---- C: 集群接入面 (Cluster.join + StaticDirectory) ----
    static class StaticDirectory {
        final java.util.List<Object> invokers;
        StaticDirectory(java.util.List<Object> invokers) { this.invokers = invokers; }
        Object get(int i) { return invokers.get(i); }
    }

    static class Cluster {
        static final String DEFAULT = "failover";
        static final String ZONE_AWARE = "zone-aware";
        static Object join(StaticDirectory dir, boolean buildFilterChain) {
            return "CLUSTER_INVOKER[" + dir.get(0) + (buildFilterChain ? ",filterChain" : "") + "]";
        }
    }

    // ---- D: 代理面 (InvokerInvocationHandler → RpcInvocation) ----
    static class RpcInvocation {
        final String method, iface;
        RpcInvocation(String method, String iface) { this.method = method; this.iface = iface; }
        public String toString() { return "RpcInvocation(" + iface + "." + method + ")"; }
    }

    static class InvokerInvocationHandler {
        final Object invoker; // ClusterInvoker
        InvokerInvocationHandler(Object invoker) { this.invoker = invoker; }
        Object invoke(Object proxy, String method) {
            if ("toString".equals(method)) return invoker.toString(); // Object 方法特判
            RpcInvocation rpc = new RpcInvocation(method, "HelloService");
            return "INVOKE_CHAIN(" + invoker + " → " + rpc + ")"; // InvocationUtil.invoke → D-4 起点
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 装配面 — 双路径 + injvm 兜底 ============
        ReferenceConfig ref = new ReferenceConfig("HelloService");
        ref.urls.add(new URL("dubbo", "192.168.1.20:20880", "HelloService")); // 直连
        System.out.println("A1 direct  : " + ref.get(true));
        ReferenceConfig ref2 = new ReferenceConfig("HelloService"); // 注册中心路径
        System.out.println("A2 registry: " + ref2.get(true) + " | " + ref2.urls.get(0).isRegistry());

        // ============ B: 注册中心引用面 — group 合并 + 迁移三态 ============
        URL registryUrl = new URL("registry", "zookeeper://192.168.1.10:2181", "HelloService");
        registryUrl.params.put("group", "g1,g2");
        System.out.println("B1 group   : " + RegistryProtocol.refer(ref, registryUrl));
        registryUrl.params.put("group", "");
        System.out.println("B2 migrate : " + RegistryProtocol.refer(ref, registryUrl));

        // ============ C: 集群接入面 — StaticDirectory + join ============
        java.util.List<Object> invokers = new java.util.ArrayList<>();
        invokers.add("Invoker(provider-1)");
        StaticDirectory dir = new StaticDirectory(invokers);
        System.out.println("C1 join    : " + Cluster.join(dir, true));
        System.out.println("C2 zone    : default-cluster=" + (invokers.size() > 1 ? Cluster.ZONE_AWARE : Cluster.DEFAULT));

        // ============ D: 代理面 — handler → RpcInvocation ============
        InvokerInvocationHandler handler = new InvokerInvocationHandler(Cluster.join(dir, true));
        System.out.println("D1 proxy   : " + handler.invoke(null, "sayHello"));
        System.out.println("D2 toString: " + handler.invoke(null, "toString"));

        // 断言
        pass += ref.get(true).startsWith("PROXY(") ? 1 : 0;
        pass += ref2.urls.get(0).isRegistry() ? 1 : 0;
        pass += RegistryProtocol.refer(ref, registryUrl).toString().contains("MIGRATION") ? 1 : 0;
        pass += Cluster.join(dir, true).toString().contains("filterChain") ? 1 : 0;
        pass += handler.invoke(null, "sayHello").toString().contains("RpcInvocation") ? 1 : 0;
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
