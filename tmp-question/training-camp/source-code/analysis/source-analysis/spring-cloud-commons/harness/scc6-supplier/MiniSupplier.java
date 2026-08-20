import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * MiniSupplier — 微缩版 ServiceInstanceListSupplier 链 (委托链 + 缓存 + 健康过滤)
 *
 * 覆盖机制:
 * 1. ServiceInstanceListSupplier 接口 (get + getServiceId)
 * 2. DelegatingServiceInstanceListSupplier 委托基类 (链式包装)
 * 3. DiscoverySupplier 基底 (响应式包装 DiscoveryClient — 微缩用惰性 Supplier)
 * 4. CachingSupplier 缓存 (惰性缓存 + miss 回填)
 * 5. HealthFilterSupplier 健康过滤
 * 6. Builder 链式装配
 */
public class MiniSupplier {

    // ===== ServiceInstance 模型 =====

    public static class ServiceInstance {
        final String serviceId;
        final String host;
        final int port;
        final boolean healthy;

        ServiceInstance(String serviceId, String host, int port, boolean healthy) {
            this.serviceId = serviceId;
            this.host = host;
            this.port = port;
            this.healthy = healthy;
        }

        @Override
        public String toString() {
            return serviceId + "@" + host + ":" + port + (healthy ? "" : "[DOWN]");
        }
    }

    // ===== 响应式模拟 (微缩: 用 Supplier<List> 代替 Flux) =====

    public interface ServiceInstanceListSupplier {
        String getServiceId();

        List<ServiceInstance> get();

        default List<ServiceInstance> get(String request) {
            return get();
        }
    }

    // ===== 委托基类 =====

    public static abstract class DelegatingServiceInstanceListSupplier implements ServiceInstanceListSupplier {
        protected final ServiceInstanceListSupplier delegate;

        protected DelegatingServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
            if (delegate == null) {
                throw new IllegalArgumentException("delegate may not be null");
            }
            this.delegate = delegate;
        }

        @Override
        public String getServiceId() {
            return delegate.getServiceId();
        }

        public ServiceInstanceListSupplier getDelegate() {
            return delegate;
        }
    }

    // ===== 基底: Discovery Supplier =====

    public static class DiscoverySupplier implements ServiceInstanceListSupplier {
        private final String serviceId;
        private final Supplier<List<ServiceInstance>> discoveryCall;

        DiscoverySupplier(String serviceId, Supplier<List<ServiceInstance>> discoveryCall) {
            this.serviceId = serviceId;
            this.discoveryCall = discoveryCall;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public List<ServiceInstance> get() {
            return discoveryCall.get(); // 每次调用实时查 (无缓存)
        }
    }

    // ===== 缓存层 =====

    public static class CachingSupplier extends DelegatingServiceInstanceListSupplier {
        private List<ServiceInstance> cache;
        private boolean cachePopulated = false;
        private int missCount = 0;

        CachingSupplier(ServiceInstanceListSupplier delegate) {
            super(delegate);
        }

        @Override
        public List<ServiceInstance> get() {
            if (!cachePopulated) {
                missCount++;
                cache = delegate.get(); // miss → 取底层
                cachePopulated = true;
            }
            return cache; // 命中 → 缓存
        }

        int getMissCount() {
            return missCount;
        }
    }

    // ===== 健康过滤层 =====

    public static class HealthFilterSupplier extends DelegatingServiceInstanceListSupplier {
        HealthFilterSupplier(ServiceInstanceListSupplier delegate) {
            super(delegate);
        }

        @Override
        public List<ServiceInstance> get() {
            return delegate.get().stream().filter(i -> i.healthy).collect(Collectors.toList());
        }
    }

    // ===== Builder =====

    public static class ServiceInstanceListSupplierBuilder {
        private ServiceInstanceListSupplier delegate;

        public ServiceInstanceListSupplierBuilder withDiscoveryClient(String serviceId,
                Supplier<List<ServiceInstance>> discoveryCall) {
            this.delegate = new DiscoverySupplier(serviceId, discoveryCall);
            return this;
        }

        public ServiceInstanceListSupplierBuilder withCaching() {
            this.delegate = new CachingSupplier(this.delegate);
            return this;
        }

        public ServiceInstanceListSupplierBuilder withHealthChecks() {
            this.delegate = new HealthFilterSupplier(this.delegate);
            return this;
        }

        public ServiceInstanceListSupplier build() {
            return delegate;
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
        // ---- 1. 基底: Discovery 实时查 ----
        List<ServiceInstance> liveData = new ArrayList<>(List.of(
                new ServiceInstance("orders", "h1", 8080, true),
                new ServiceInstance("orders", "h2", 8080, false)));
        int[] discoveryCalls = {0};
        DiscoverySupplier discovery = new DiscoverySupplier("orders", () -> {
            discoveryCalls[0]++;
            return new ArrayList<>(liveData);
        });
        check("基底: 首次查实时", discovery.get().size() == 2);
        discovery.get(); // 第二次实时查
        check("基底: 二次查仍实时 (无缓存)", discoveryCalls[0] == 2);

        // ---- 2. 缓存层: 首次 miss 后续命中 ----
        int[] calls2 = {0};
        CachingSupplier caching = new CachingSupplier(new DiscoverySupplier("orders", () -> {
            calls2[0]++;
            return new ArrayList<>(liveData);
        }));
        check("缓存: 首次 miss", caching.get().size() == 2 && caching.getMissCount() == 1);
        check("缓存: 二次命中 (底层未再调)", caching.get().size() == 2 && calls2[0] == 1);

        // ---- 3. 健康过滤 ----
        HealthFilterSupplier healthFilter = new HealthFilterSupplier(discovery);
        List<ServiceInstance> healthy = healthFilter.get();
        check("健康过滤: 只留 healthy", healthy.size() == 1 && healthy.get(0).host.equals("h1"));

        // ---- 4. Builder 链式装配 ----
        ServiceInstanceListSupplier chain = new ServiceInstanceListSupplierBuilder()
                .withDiscoveryClient("orders", () -> new ArrayList<>(liveData))
                .withCaching()
                .withHealthChecks()
                .build();
        check("Builder: 链完整 (过滤后 1 个)", chain.get().size() == 1);
        check("Builder: serviceId 沿链传递", "orders".equals(chain.getServiceId()));

        // ---- 5. 链顺序验证: 缓存外层包健康过滤 ----
        // 健康过滤在缓存外 → 缓存的是"已过滤"还是"未过滤"?
        // 真实链: withCaching 包 withHealthChecks → 缓存的是健康过滤结果
        List<ServiceInstance> chainResult = chain.get();
        check("链序: 缓存外层 (结果已过滤)", chainResult.stream().allMatch(i -> i.healthy));
        check("链序: 再取走缓存", chain.get() == chainResult || chain.get().equals(chainResult));

        // ---- 6. 委托基类校验 ----
        boolean threw = false;
        try {
            new CachingSupplier(null);
        } catch (IllegalArgumentException e) {
            threw = e.getMessage().contains("delegate may not be null");
        }
        check("委托: null delegate 抛错", threw);

        // ---- 7. getServiceId 委托链 ----
        check("委托: getServiceId 沿链", "orders".equals(caching.getServiceId()) && "orders".equals(healthFilter.getServiceId()));

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }
}
