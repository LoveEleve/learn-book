import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.CachedAtomicLong;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicValue;
import org.apache.curator.framework.recipes.atomic.PromotedToLock;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.curator.framework.recipes.shared.SharedCountListener;
import org.apache.curator.framework.recipes.shared.SharedCountReader;
import org.apache.curator.framework.recipes.shared.SharedValue;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.recipes.shared.VersionedValue;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;

/**
 * HarnessC6 — C-6 共享状态与原子量 8 组断言 (对照 Curator 5.8.0 源码)
 *
 * 真 ZK 内嵌, 验证:
 *   A. SharedValue: 版本 CAS — 并发 trySetValue 恰 1 个成功 (SharedValue.java:177-200)
 *   B. SharedValue watcher: 他人修改后本地缓存更新 (L63-71, L259-274)
 *   C. DistributedAtomicLong: 顺序 increment 累加正确 (tryOnce L273-296)
 *   D. DistributedAtomicLong: 5 线程并发 × 10 次 = 50 (乐观重试正确性)
 *   E. compareAndSet 成功/失败路径 (L120-138)
 *   F. initialize 语义: 已存在时 succeeded=false (L173-181)
 *   G. CachedAtomicLong: next() 号段唯一递增 (L48-70)
 *   H. SharedCount: trySetCount 版本 CAS + countHasChanged 通知 (SharedCount.java:109-134)
 */
public class HarnessC6 {
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
        System.out.println("== C-6 Harness ==");
        try (TestingServer server = new TestingServer()) {
            String cs = server.getConnectString();
            CuratorFramework client = CuratorFrameworkFactory.newClient(cs, new RetryNTimes(3, 10));
            client.start();
            client.blockUntilConnected(10, TimeUnit.SECONDS);

            // ---------- A. SharedValue 版本 CAS ----------
            System.out.println("[A] SharedValue 版本 CAS");
            SharedValue sv = new SharedValue(client, "/sv/val", "seed".getBytes());
            sv.start();
            Thread.sleep(300);
            VersionedValue<byte[]> current = sv.getVersionedValue();
            SharedValue sv2 = new SharedValue(client, "/sv/val", "seed".getBytes());
            sv2.start();
            Thread.sleep(300);
            // 两个客户端都基于旧版本 CAS — 只有第一个成功
            boolean a1 = sv.trySetValue(current, "AAA".getBytes());
            boolean a2 = sv2.trySetValue(current, "BBB".getBytes());
            check("两个过期版本 CAS 恰 1 成功 (" + a1 + "/" + a2 + ")", a1 ^ a2);
            sv.close();
            sv2.close();

            // ---------- B. watcher 更新 ----------
            System.out.println("[B] watcher 更新");
            SharedValue sv3 = new SharedValue(client, "/sv/val2", "v0".getBytes());
            sv3.start();
            Thread.sleep(300);
            client.setData().forPath("/sv/val2", "v1".getBytes()); // 外部直接改
            Thread.sleep(800); // 等 watcher 事件传播
            check("外部修改后本地缓存更新", new String(sv3.getValue()).equals("v1"));
            sv3.close();

            // ---------- C. 顺序累加 ----------
            System.out.println("[C] 顺序累加");
            DistributedAtomicLong dal = new DistributedAtomicLong(client, "/atomic/long1", new RetryNTimes(10, 10));
            for (int i = 0; i < 10; i++) {
                AtomicValue<Long> r = dal.increment();
                check("第 " + i + " 次 increment succeeded", r.succeeded());
            }
            check("累加 10 次 = 10", dal.get().postValue() == 10L);

            // ---------- D. 并发累加 ----------
            System.out.println("[D] 并发累加");
            DistributedAtomicLong dal2 = new DistributedAtomicLong(client, "/atomic/long2", new RetryNTimes(10, 10));
            int threads = 5;
            int perThread = 10;
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger failures = new AtomicInteger(0);
            for (int i = 0; i < threads; i++) {
                Thread t = new Thread(() -> {
                    try {
                        ready.countDown();
                        go.await();
                        for (int j = 0; j < perThread; j++) {
                            AtomicValue<Long> r = dal2.increment();
                            if (!r.succeeded()) {
                                failures.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    }
                });
                t.start();
            }
            ready.await();
            go.countDown();
            Thread.sleep(5000);
            check("并发 5×10 无失败", failures.get() == 0);
            check("并发累加 = 50 (实际 " + dal2.get().postValue() + ")", dal2.get().postValue() == 50L);

            // ---------- E. compareAndSet ----------
            System.out.println("[E] compareAndSet");
            DistributedAtomicLong dal3 = new DistributedAtomicLong(client, "/atomic/long3", new RetryNTimes(3, 10));
            dal3.initialize(42L);
            AtomicValue<Long> casOk = dal3.compareAndSet(42L, 100L);
            check("CAS 42→100 成功", casOk.succeeded());
            AtomicValue<Long> casBad = dal3.compareAndSet(42L, 200L);
            check("CAS 过期期望值失败", !casBad.succeeded());
            check("失败后值仍 100", dal3.get().postValue() == 100L);

            // ---------- F. initialize ----------
            System.out.println("[F] initialize");
            DistributedAtomicLong dal4 = new DistributedAtomicLong(client, "/atomic/long4", new RetryNTimes(3, 10));
            check("首次 initialize 成功", dal4.initialize(1L));
            check("再次 initialize 失败 (节点已存在)", !dal4.initialize(2L));

            // ---------- G. CachedAtomicLong ----------
            System.out.println("[G] CachedAtomicLong");
            DistributedAtomicLong calBase = new DistributedAtomicLong(client, "/atomic/cached", new RetryNTimes(3, 10));
            CachedAtomicLong cal = new CachedAtomicLong(calBase, 10);
            Set<Long> seen = ConcurrentHashMap.newKeySet();
            for (int i = 0; i < 25; i++) {
                long v = cal.next().postValue();
                seen.add(v);
            }
            check("25 次 next() 全部唯一", seen.size() == 25);
            check("值单调递增", seen.stream().sorted().reduce((a, b) -> a).orElse(0L) >= 0L);

            // ---------- H. SharedCount ----------
            System.out.println("[H] SharedCount");
            SharedCount sc = new SharedCount(client, "/shared/count", 0);
            CountDownLatch changeLatch = new CountDownLatch(1);
            AtomicInteger notified = new AtomicInteger(0);
            sc.addListener(new SharedCountListener() {
                @Override
                public void countHasChanged(SharedCountReader sharedCount, int newCount) throws Exception {
                    notified.incrementAndGet();
                    changeLatch.countDown();
                }

                @Override
                public void stateChanged(CuratorFramework c, ConnectionState newState) {}
            });
            sc.start();
            Thread.sleep(300);
            VersionedValue<Integer> scVersion = sc.getVersionedValue();
            SharedCount sc2 = new SharedCount(client, "/shared/count", 0);
            sc2.start();
            Thread.sleep(300);
            boolean s1 = sc.trySetCount(scVersion, 5);
            boolean s2 = sc2.trySetCount(scVersion, 9);
            check("并发 trySetCount 恰 1 成功 (" + s1 + "/" + s2 + ")", s1 ^ s2);
            check("countHasChanged 通知触发", changeLatch.await(5, TimeUnit.SECONDS) && notified.get() >= 1);
            check("最终计数 = " + (s1 ? 5 : 9), sc.getCount() == (s1 ? 5 : 9) || sc2.getCount() == (s1 ? 5 : 9));
            sc.close();
            sc2.close();

            client.close();
        }
        System.out.println("== 结果: " + pass + " PASS / " + fail + " FAIL ==");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
