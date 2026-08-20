import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.ServiceProviderBuilder;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceCacheBuilder;
import org.apache.curator.x.discovery.DownInstancePolicy;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;

/**
 * HarnessC8 — C-8 服务发现 7 组断言 (对照 Curator 5.8.0 curator-x-discovery)
 *
 * 真 ZK 内嵌, 验证:
 *   A. registerService → 临时节点注册 (ServiceDiscoveryImpl.java:178-190)
 *   B. ServiceCache: 注册后缓存可见 (ServiceCacheImpl 基于 CuratorCache)
 *   C. ServiceProvider.getInstance: 轮询策略拿到实例 (ServiceProviderImpl.java:125-127)
 *   D. unregisterService → 缓存中消失 (临时节点删除)
 *   E. updateService → 新值生效
 *   F. noteError 降级: 报错实例被暂时排除 (DownInstanceManager L56-60)
 *   G. 多实例轮询: 2 个实例交替返回
 */
public class HarnessC8 {
    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) {
            pass++;
            System.out.println("  PASS " + name);
        } else {
            fail++;
            System.out.println("  FAIL " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== C-8 Harness ==");
        try (TestingServer server = new TestingServer()) {
            String cs = server.getConnectString();
            CuratorFramework client = CuratorFrameworkFactory.newClient(cs, new RetryNTimes(3, 10));
            client.start();
            client.blockUntilConnected(10, TimeUnit.SECONDS);

            ServiceDiscovery<Void> discovery = ServiceDiscoveryBuilder.builder(Void.class)
                    .client(client)
                    .basePath("/discovery")
                    .build();
            discovery.start();
            Thread.sleep(300);

            // ---------- A. 注册 ----------
            System.out.println("[A] 注册");
            ServiceInstance<Void> i1 = ServiceInstance.<Void>builder()
                    .name("svc-a").id("a-1").address("10.0.0.1").port(8080).build();
            discovery.registerService(i1);
            Thread.sleep(500);
            check("注册节点存在", client.checkExists().forPath("/discovery/svc-a/a-1") != null);
            check("节点是临时节点", client.checkExists().forPath("/discovery/svc-a/a-1").getEphemeralOwner() != 0);

            // ---------- B. ServiceCache ----------
            System.out.println("[B] ServiceCache");
            ServiceCache<Void> cache = discovery.serviceCacheBuilder()
                    .name("svc-a").threadFactory(r -> new Thread(r, "svc-cache")).build();
            cache.start();
            Thread.sleep(500);
            Collection<ServiceInstance<Void>> instances = cache.getInstances();
            check("缓存可见注册实例 (共 " + instances.size() + ")", instances.size() == 1);

            // ---------- E. updateService ----------
            System.out.println("[E] updateService");
            ServiceInstance<Void> i1b = ServiceInstance.<Void>builder()
                    .name("svc-a").id("a-1").address("10.0.0.1").port(9090).build();
            discovery.updateService(i1b);
            Thread.sleep(500);
            check("端口更新生效", cache.getInstances().iterator().next().getPort() == 9090);

            // ---------- G. 多实例轮询 ----------
            System.out.println("[G] 多实例轮询");
            ServiceInstance<Void> i2 = ServiceInstance.<Void>builder()
                    .name("svc-a").id("a-2").address("10.0.0.2").port(8081).build();
            discovery.registerService(i2);
            Thread.sleep(800);
            ServiceProvider<Void> provider = discovery.serviceProviderBuilder()
                    .serviceName("svc-a")
                    .providerStrategy(new RoundRobinStrategy<>())
                    .downInstancePolicy(new DownInstancePolicy(30000, TimeUnit.MILLISECONDS, 3))
                    .threadFactory(r -> new Thread(r, "svc-provider"))
                    .build();
            provider.start();
            Thread.sleep(500);
            String first = provider.getInstance().getId();
            String second = provider.getInstance().getId();
            check("轮询两次拿到不同实例 (" + first + "/" + second + ")", !first.equals(second));
            check("getAllInstances 返回 2 个", provider.getAllInstances().size() == 2);

            // ---------- F. noteError 降级 ----------
            System.out.println("[F] noteError 降级");
            // 对 a-1 连续报错 3 次 → 触发 DownInstancePolicy(errorThreshold=3)
            ServiceInstance<Void> target = cache.getInstances().stream()
                    .filter(i -> i.getId().equals("a-1")).findFirst().orElseThrow();
            for (int i = 0; i < 3; i++) {
                provider.noteError(target);
            }
            boolean downExcluded = false;
            for (int i = 0; i < 20; i++) {
                Thread.sleep(200);
                Collection<ServiceInstance<Void>> all = provider.getAllInstances();
                if (all.size() == 1) { // 只剩 1 个 → 另一个被 down 排除
                    downExcluded = true;
                    break;
                }
            }
            check("报错实例被降级排除 (可用集 " + (downExcluded ? "1" : "2") + ")", downExcluded);
            provider.close();

            // ---------- D. 注销 ----------
            System.out.println("[D] 注销");
            discovery.unregisterService(i1b);
            discovery.unregisterService(i2);
            Thread.sleep(800);
            check("注销后缓存为空", cache.getInstances().isEmpty());
            cache.close();
            discovery.close();
            client.close();
        }
        System.out.println("== 结果: " + pass + " PASS / " + fail + " FAIL ==");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
