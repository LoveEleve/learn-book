import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MiniReactorLB — 微缩版 ReactorLoadBalancer 策略 (RoundRobin + Random)
 *
 * 覆盖机制:
 * 1. ReactorLoadBalancer 接口 (choose)
 * 2. RoundRobin: AtomicInteger position + seedPosition 随机种子 + & MAX_VALUE 循环
 * 3. 三态: 空列表 EmptyResponse / 单实例不转位置 / 多实例取模
 * 4. Random: ThreadLocalRandom 随机
 * 5. SelectedInstanceCallback 回调 (选中后通知 supplier 链)
 * 6. 每服务 LoadBalancer 隔离 (LoadBalancerClientFactory 微缩)
 */
public class MiniReactorLB {

    // ===== ServiceInstance + Response =====

    public static class ServiceInstance {
        final String serviceId;
        final String instanceId;

        ServiceInstance(String serviceId, String instanceId) {
            this.serviceId = serviceId;
            this.instanceId = instanceId;
        }

        @Override
        public String toString() {
            return instanceId;
        }
    }

    public static class Response<T> {
        final T server;
        final boolean hasServer;

        Response(T server, boolean hasServer) {
            this.server = server;
            this.hasServer = hasServer;
        }

        T getServer() {
            return server;
        }

        boolean hasServer() {
            return hasServer;
        }
    }

    public static class DefaultResponse<T> extends Response<T> {
        DefaultResponse(T server) {
            super(server, true);
        }
    }

    public static class EmptyResponse<T> extends Response<T> {
        EmptyResponse() {
            super(null, false);
        }
    }

    // ===== Supplier (微缩 SCC-6) =====

    public interface ServiceInstanceListSupplier {
        List<ServiceInstance> get();
    }

    public static class StaticSupplier implements ServiceInstanceListSupplier, SelectedInstanceCallback {
        private final List<ServiceInstance> instances;
        private final List<ServiceInstance> selected = new ArrayList<>();

        StaticSupplier(List<ServiceInstance> instances) {
            this.instances = instances;
        }

        @Override
        public List<ServiceInstance> get() {
            return instances;
        }

        @Override
        public void selectedServiceInstance(ServiceInstance serviceInstance) {
            selected.add(serviceInstance);
        }
    }

    public interface SelectedInstanceCallback {
        void selectedServiceInstance(ServiceInstance serviceInstance);
    }

    // ===== ReactorLoadBalancer 接口 =====

    public interface ReactorLoadBalancer<T> {
        Response<T> choose();
    }

    // ===== RoundRobinLoadBalancer =====

    public static class RoundRobinLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {
        final AtomicInteger position;
        private final ServiceInstanceListSupplier supplier;

        public RoundRobinLoadBalancer(ServiceInstanceListSupplier supplier) {
            this(supplier, new Random().nextInt(1000)); // 随机种子
        }

        public RoundRobinLoadBalancer(ServiceInstanceListSupplier supplier, int seedPosition) {
            this.supplier = supplier;
            this.position = new AtomicInteger(seedPosition);
        }

        @Override
        public Response<ServiceInstance> choose() {
            List<ServiceInstance> instances = supplier.get();
            return processInstanceResponse(instances);
        }

        private Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> instances) {
            Response<ServiceInstance> response = getInstanceResponse(instances);
            if (supplier instanceof SelectedInstanceCallback && response.hasServer()) {
                ((SelectedInstanceCallback) supplier).selectedServiceInstance(response.getServer());
            }
            return response;
        }

        Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
            if (instances.isEmpty()) {
                return new EmptyResponse<>();
            }
            if (instances.size() == 1) {
                return new DefaultResponse<>(instances.get(0)); // 单实例不转位置
            }
            int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;
            return new DefaultResponse<>(instances.get(pos % instances.size()));
        }
    }

    // ===== RandomLoadBalancer =====

    public static class RandomLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {
        private final ServiceInstanceListSupplier supplier;
        private final java.util.concurrent.ThreadLocalRandom random;

        public RandomLoadBalancer(ServiceInstanceListSupplier supplier) {
            this.supplier = supplier;
            this.random = java.util.concurrent.ThreadLocalRandom.current();
        }

        @Override
        public Response<ServiceInstance> choose() {
            List<ServiceInstance> instances = supplier.get();
            if (instances.isEmpty()) {
                return new EmptyResponse<>();
            }
            int index = random.nextInt(instances.size());
            return new DefaultResponse<>(instances.get(index));
        }
    }

    // ===== 每服务工厂 (LoadBalancerClientFactory 微缩) =====

    public static class LoadBalancerClientFactory {
        private final java.util.Map<String, ReactorLoadBalancer<ServiceInstance>> balancers = new java.util.concurrent.ConcurrentHashMap<>();

        public ReactorLoadBalancer<ServiceInstance> getInstance(String serviceId) {
            return balancers.get(serviceId);
        }

        public void register(String serviceId, ReactorLoadBalancer<ServiceInstance> lb) {
            balancers.put(serviceId, lb);
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
        List<ServiceInstance> instances = List.of(
                new ServiceInstance("orders", "instance-0"),
                new ServiceInstance("orders", "instance-1"),
                new ServiceInstance("orders", "instance-2"));

        // ---- 1. 轮询顺序 (seed=0 起点) ----
        RoundRobinLoadBalancer rr = new RoundRobinLoadBalancer(new StaticSupplier(instances), 0);
        check("轮询: 第一次选 1 (pos 0→1)", "instance-1".equals(rr.choose().getServer().instanceId));
        check("轮询: 第二次选 2 (pos 1→2)", "instance-2".equals(rr.choose().getServer().instanceId));
        check("轮询: 第三次选 0 (pos 2→3 % 3)", "instance-0".equals(rr.choose().getServer().instanceId));

        // ---- 2. & MAX_VALUE 循环 ----
        // position 接近 MAX 时 & MAX_VALUE 保证非负
        RoundRobinLoadBalancer nearMax = new RoundRobinLoadBalancer(new StaticSupplier(instances), Integer.MAX_VALUE);
        int pos = nearMax.position.incrementAndGet() & Integer.MAX_VALUE;
        check("& MAX: 溢出后非负", pos >= 0);
        // 单实例不转位置
        RoundRobinLoadBalancer single = new RoundRobinLoadBalancer(new StaticSupplier(List.of(instances.get(0))), 5);
        check("单实例: 不转位置", single.position.get() == 5);
        single.choose();
        check("单实例: choose 后仍不转", single.position.get() == 5);

        // ---- 3. 空列表 EmptyResponse ----
        RoundRobinLoadBalancer empty = new RoundRobinLoadBalancer(new StaticSupplier(List.of()), 0);
        Response<ServiceInstance> emptyResp = empty.choose();
        check("空列表: EmptyResponse 无 server", !emptyResp.hasServer());

        // ---- 4. SelectedInstanceCallback 回调 ----
        StaticSupplier callbackSupplier = new StaticSupplier(instances);
        // 微缩: StaticSupplier 实现 callback (真实: Delegating 传递)
        RoundRobinLoadBalancer withCallback = new RoundRobinLoadBalancer(callbackSupplier, 0);
        withCallback.choose();
        check("回调: 选中后通知 supplier", callbackSupplier.selected.size() == 1);

        // ---- 5. Random 随机 ----
        RandomLoadBalancer random = new RandomLoadBalancer(new StaticSupplier(instances));
        java.util.Set<String> chosen = new java.util.HashSet<>();
        for (int i = 0; i < 50; i++) {
            chosen.add(random.choose().getServer().instanceId);
        }
        check("Random: 50 次覆盖多实例", chosen.size() > 1);

        // ---- 6. 每服务隔离 ----
        LoadBalancerClientFactory factory = new LoadBalancerClientFactory();
        factory.register("orders", new RoundRobinLoadBalancer(new StaticSupplier(instances), 0));
        factory.register("payments", new RandomLoadBalancer(new StaticSupplier(instances)));
        check("每服务: orders 是 RoundRobin", factory.getInstance("orders") instanceof RoundRobinLoadBalancer);
        check("每服务: payments 是 Random", factory.getInstance("payments") instanceof RandomLoadBalancer);
        check("每服务: 未知服务返回 null", factory.getInstance("unknown") == null);

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }
}
