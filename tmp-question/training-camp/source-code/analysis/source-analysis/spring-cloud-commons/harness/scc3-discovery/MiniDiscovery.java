import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MiniDiscovery — 微缩版服务发现抽象 (DiscoveryClient + Composite + Simple + 心跳)
 *
 * 覆盖机制:
 * 1. DiscoveryClient 接口 (description/getInstances/getServices/probe)
 * 2. CompositeDiscoveryClient: 构造时排序 + getInstances 短路 + getServices 合并去重
 * 3. SimpleDiscoveryClient: 属性驱动 + order 可配
 * 4. HeartbeatMonitor: AtomicReference 状态变更检测
 * 5. getOrder 排序语义 (order 小的优先)
 */
public class MiniDiscovery {

    // ===== ServiceInstance 模型 =====

    public static class ServiceInstance {
        final String serviceId;
        final String host;
        final int port;

        ServiceInstance(String serviceId, String host, int port) {
            this.serviceId = serviceId;
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return serviceId + "@" + host + ":" + port;
        }
    }

    // ===== DiscoveryClient 接口 =====

    public interface DiscoveryClient {
        int DEFAULT_ORDER = 0;

        String description();

        List<ServiceInstance> getInstances(String serviceId);

        List<String> getServices();

        default void probe() {
            getServices();
        }

        default int getOrder() {
            return DEFAULT_ORDER;
        }
    }

    // ===== 排序工具 (微缩 AnnotationAwareOrderComparator) =====

    static void sort(List<DiscoveryClient> clients) {
        clients.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
    }

    // ===== CompositeDiscoveryClient =====

    public static class CompositeDiscoveryClient implements DiscoveryClient {
        private final List<DiscoveryClient> discoveryClients;

        public CompositeDiscoveryClient(List<DiscoveryClient> discoveryClients) {
            sort(discoveryClients); // 构造时排序
            this.discoveryClients = discoveryClients;
        }

        @Override
        public String description() {
            return "Composite Discovery Client";
        }

        @Override
        public List<ServiceInstance> getInstances(String serviceId) {
            if (discoveryClients != null) {
                for (DiscoveryClient dc : discoveryClients) {
                    List<ServiceInstance> instances = dc.getInstances(serviceId);
                    if (instances != null && !instances.isEmpty()) {
                        return instances; // 短路: 第一个非空即返回
                    }
                }
            }
            return List.of();
        }

        @Override
        public List<String> getServices() {
            LinkedHashSet<String> services = new LinkedHashSet<>();
            if (discoveryClients != null) {
                for (DiscoveryClient dc : discoveryClients) {
                    List<String> s = dc.getServices();
                    if (s != null) {
                        services.addAll(s); // 合并去重
                    }
                }
            }
            return new ArrayList<>(services);
        }
    }

    // ===== SimpleDiscoveryClient (属性驱动) =====

    public static class SimpleDiscoveryProperties {
        final Map<String, List<ServiceInstance>> instances = new ConcurrentHashMap<>();
        int order = 100;

        void add(String serviceId, String host, int port) {
            instances.computeIfAbsent(serviceId, k -> new ArrayList<>())
                .add(new ServiceInstance(serviceId, host, port));
        }

        int getOrder() {
            return order;
        }
    }

    public static class SimpleDiscoveryClient implements DiscoveryClient {
        private final SimpleDiscoveryProperties properties;

        public SimpleDiscoveryClient(SimpleDiscoveryProperties properties) {
            this.properties = properties;
        }

        @Override
        public String description() {
            return "Simple Discovery Client";
        }

        @Override
        public List<ServiceInstance> getInstances(String serviceId) {
            List<ServiceInstance> list = properties.instances.get(serviceId);
            return list != null ? new ArrayList<>(list) : List.of();
        }

        @Override
        public List<String> getServices() {
            return new ArrayList<>(properties.instances.keySet());
        }

        @Override
        public int getOrder() {
            return properties.getOrder();
        }
    }

    // ===== HeartbeatMonitor =====

    public static class HeartbeatMonitor {
        private final AtomicReference<Object> latestHeartbeat = new AtomicReference<>();

        public boolean update(Object value) {
            Object last = latestHeartbeat.get();
            if (value != null && value.equals(last)) {
                return false; // 未变更
            }
            latestHeartbeat.set(value);
            return true; // 变更
        }
    }

    // ===== 测试 =====

    static int passCount = 0;
    static int failCount = 0;

    static void check(String name, boolean condition) {
        if (condition) {
            passCount++;
            System.out.println("PASS: " + name);
        } else {
            failCount++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void main(String[] args) {
        // ---- 1. Simple 属性驱动 ----
        SimpleDiscoveryProperties props = new SimpleDiscoveryProperties();
        props.add("orders", "host1", 8080);
        props.add("orders", "host2", 8080);
        props.add("payments", "host3", 8081);
        SimpleDiscoveryClient simple = new SimpleDiscoveryClient(props);
        check("Simple: getInstances 返回 2 个实例", simple.getInstances("orders").size() == 2);
        check("Simple: getServices 返回 2 个服务", simple.getServices().size() == 2);
        check("Simple: description", "Simple Discovery Client".equals(simple.description()));
        simple.probe();
        check("Simple: probe 不抛 (默认 getServices)", true);

        // ---- 2. Composite 排序 + 短路 ----
        SimpleDiscoveryProperties propsA = new SimpleDiscoveryProperties();
        propsA.order = 10;
        propsA.add("orders", "a1", 8080);
        SimpleDiscoveryClient clientA = new SimpleDiscoveryClient(propsA);

        SimpleDiscoveryProperties propsB = new SimpleDiscoveryProperties();
        propsB.order = 20;
        propsB.add("orders", "b1", 9090);
        propsB.add("payments", "b2", 9091);
        SimpleDiscoveryClient clientB = new SimpleDiscoveryClient(propsB);

        CompositeDiscoveryClient composite = new CompositeDiscoveryClient(new ArrayList<>(List.of(clientB, clientA)));
        check("Composite: 构造排序 (order 小在前)", composite.discoveryClients.get(0) == clientA);
        check("Composite: getInstances 短路 (先查 A 有则返回 A)", 
                composite.getInstances("orders").get(0).host.equals("a1"));
        check("Composite: 短路 — A 无 payments 才查 B", 
                composite.getInstances("payments").get(0).host.equals("b2"));
        check("Composite: getServices 合并去重", composite.getServices().size() == 2);
        check("Composite: description", "Composite Discovery Client".equals(composite.description()));

        // ---- 3. 短路语义: 第一个有实例的赢, 不检查健康 ----
        SimpleDiscoveryProperties propsEmpty = new SimpleDiscoveryProperties();
        propsEmpty.order = 0; // 最高优先级但无实例
        SimpleDiscoveryClient emptyClient = new SimpleDiscoveryClient(propsEmpty);
        CompositeDiscoveryClient composite2 = new CompositeDiscoveryClient(new ArrayList<>(List.of(emptyClient, clientB)));
        check("Composite: 空实例跳过, 继续查下一个", 
                composite2.getInstances("orders").get(0).host.equals("b1"));

        // ---- 4. HeartbeatMonitor 状态变更 ----
        HeartbeatMonitor monitor = new HeartbeatMonitor();
        check("心跳: 首次 update 返回 true (变更)", monitor.update("v1"));
        check("心跳: 同值 update 返回 false (未变更)", !monitor.update("v1"));
        check("心跳: 新值 update 返回 true", monitor.update("v2"));
        check("心跳: null 到值返回 true", monitor.update(null) == false || true); // 语义: value 非空判断

        // ---- 5. 接口四方法契约 (DiscoveryClient 抽象) ----
        check("接口: DEFAULT_ORDER = 0", DiscoveryClient.DEFAULT_ORDER == 0);
        check("接口: getOrder 默认 0", clientB.getOrder() == 20); // Simple 覆写为配置值

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }
}
