import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MiniRLockTest — RD-2 harness 验证入口
 *
 * 跑法: javac MiniRLock.java MiniRLockTest.java && java MiniRLockTest
 * 全部 PASS = 锁可重入 Lua/Watchdog 语义理解到位 (对照 RedissonLock:214-224,336-360, RenewalTask:62-70)。
 */
public class MiniRLockTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. 加锁 Lua 可重入语义 (RedissonLock:214-224)
    static void testLockReentrant() {
        MiniRLock.LockStore store = new MiniRLock.LockStore();
        // 首次加锁 success
        check("首获锁", store.tryLock("lock:cnt", "t1", 30000) == null);
        // 同线程重入 → count+1 success
        check("同线程重入成功", store.tryLock("lock:cnt", "t1", 30000) == null);
        // 他线程 → 返回 pttl (被拒绝)
        check("他人持有返回 ttl", store.tryLock("lock:cnt", "t2", 30000) != null);
        // 存内 hash: t1 count=2
        check("重入计数=2", store.data.get("lock:cnt").get("t1") == 2);
    }

    // B. 解锁 owner 校验 + 重入递减 (RedissonLock:348-360)
    static void testUnlockOwnerCheck() {
        MiniRLock.LockStore store = new MiniRLock.LockStore();
        store.tryLock("lock:cnt", "t1", 30000);
        store.tryLock("lock:cnt", "t1", 30000); // 重入 2

        check("非 owner 解锁拒绝", !store.unlock("lock:cnt", "t2", 30000));
        // t1 解一次 (从 2→1): 锁还在 (重入未完)
        check("解一次 (重入未完)", store.unlock("lock:cnt", "t1", 30000));
        check("重入未完锁还在", store.data.containsKey("lock:cnt"));
        // 再解一次 (1→0): 锁删除
        check("清零后锁删除", store.unlock("lock:cnt", "t1", 30000));
        check("锁已删除", !store.data.containsKey("lock:cnt"));
    }

    // C. forceUnlock 无条件删 (L336-346)
    static void testForce() {
        MiniRLock.LockStore store = new MiniRLock.LockStore();
        store.tryLock("lock:x", "t1", 30000);
        store.tryLock("lock:x", "t1", 30000); // 重入 2
        check("forceUnlock 无条件删", store.forceUnlock("lock:x"));
        check("force 后锁不存在", !store.data.containsKey("lock:x"));
    }

    // D. Watchdog 心跳周期 (RenewalTask:62-70 lease/3) + 锁尽则停 (97-134)
    static void testWatchdog() {
        MiniRLock.LockStore store = new MiniRLock.LockStore();
        WatchdogHelper(store);
    }

    static void WatchdogHelper(MiniRLock.LockStore store) {
        // 模拟: 加锁 → 心跳 lease/3 三次 → 释放 → 心跳停止
        store.tryLock("lock:wd", "t1", 30000);
        MiniRLock.Watchdog wd = new MiniRLock.Watchdog(store, 30000);
        wd.startNewHeartbeatIfIdle("lock:wd", "t1"); // 首跳 (running=false → 启动)
        long afterFirst = wd.renewCount.get();

        // lease/3 = 10000ms 每跳 — 后续 2 次调用在 running=true 时被 tryRun 防重入拒绝
        wd.startNewHeartbeatIfIdle("lock:wd", "t1");
        wd.startNewHeartbeatIfIdle("lock:wd", "t1");
        check("tryRun 防重入: 心跳只启动一次", wd.renewCount.get() == afterFirst);

        // 锁释放 → 下次心跳检测到锁不存在 → stop
        store.unlock("lock:wd", "t1", 30000);
        wd.heartbeat("lock:wd", "t1"); // 锁没了 → stop
        check("锁尽心跳停止", !wd.running);
    }

    // E. 多线程并发加锁互斥 (基础并发)
    static void testConcurrentMutualExclusion() throws Exception {
        MiniRLock.LockStore store = new MiniRLock.LockStore();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicLong concurrentMax = new AtomicLong();

        for (int i = 0; i < threads; i++) {
            final long id = i;
            pool.submit(() -> {
                try {
                    start.await();
                    Long got = store.tryLock("lock:mutex", "t" + id, 30000);
                    if (got == null) {
                        int holders = store.data.get("lock:mutex").size();
                        concurrentMax.accumulateAndGet(holders, Math::max);
                        Thread.sleep(10);
                        store.unlock("lock:mutex", "t" + id, 30000);
                    }
                } catch (Exception ignored) {}
            });
        }
        start.countDown();
        pool.shutdown(); pool.awaitTermination(5, TimeUnit.SECONDS);
        check("互斥: 并发持有≤1", concurrentMax.get() <= 1);
        check("并发放置后锁释放", !store.data.containsKey("lock:mutex") || store.data.get("lock:mutex").isEmpty());
    }

    public static void main(String[] args) throws Exception {
        testLockReentrant();
        testUnlockOwnerCheck();
        testForce();
        testWatchdog();
        testConcurrentMutualExclusion();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}