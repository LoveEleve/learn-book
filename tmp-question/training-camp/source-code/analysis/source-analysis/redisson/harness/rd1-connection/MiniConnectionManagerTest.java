import java.util.concurrent.*;

/**
 * MiniConnectionManagerTest — RD-1 harness 验证入口
 *
 * 跑法: javac MiniConnectionManager.java MiniConnectionManagerTest.java && java MiniConnectionManagerTest
 * 全部 PASS = 机制理解到位 (对照 Redisson 4.6.2 源码协议)。
 */
public class MiniConnectionManagerTest {

    static int passed = 0, failed = 0;

    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // ---- A. 模式工厂 (ConnectionManager.create 5 分支) ----
    static void testModeFactory() {
        boolean[] ms = {true,false,false,false,false};
        check("master_slave 优先", MiniConnectionManager.detectMode(ms) == MiniConnectionManager.Mode.MASTER_SLAVE);
        boolean[] single = {false,true,false,false,false};
        check("single 检测", MiniConnectionManager.detectMode(single) == MiniConnectionManager.Mode.SINGLE);
        boolean[] none = {false,false,false,false,false};
        try {
            MiniConnectionManager.detectMode(none);
            check("无模式抛异常", false);
        } catch (IllegalArgumentException e) {
            check("无模式抛异常 (server(s) address(es) not defined!)", true);
        }
    }

    // ---- B. lazyConnect 单飞 (L190-227) ----
    static void testSingleFlight() throws Exception {
        MiniConnectionManager m = new MiniConnectionManager(false);

        // 100 线程并发触发, 只应 connect 一次
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            pool.submit(() -> {
                try { start.await(); m.lazyConnect(); } catch (Exception ignored) {}
                finally { done.countDown(); }
            });
        }
        start.countDown();
        done.await(5, TimeUnit.SECONDS);
        check("100 线程并发 connect 恰 1 次", m.connectCalls.get() == 1);
        check("并发后 initialized", m.initialized);
        pool.shutdownNow();

        // 重入防护: 持有 latch 线程再调 lazyConnect 不阻塞 (L197-199)
        MiniConnectionManager m2 = new MiniConnectionManager(false);
        Thread t = new Thread(() -> {
            m2.lazyConnect(); // 首次, 成为 connectingThread
            m2.lazyConnect(); // 重入 → 应立即返回 (不死锁)
        });
        t.setName("owner-thread");
        t.start();
        t.join(3000);
        check("owners 重入不死锁", !t.isAlive());
        check("重入后 connect 仍 1 次", m2.connectCalls.get() == 1);
    }

    static void testRetryAfterFailure() {
        // 首次失败 → latch completeExceptionally → 后续可重试 (L204)
        MiniConnectionManager m = new MiniConnectionManager(true);
        try { m.lazyConnect(); } catch (RuntimeException expected) {}
        check("首次失败未初始化", !m.initialized);
        m.lazyConnect(); // 第二次应成功 (isCompletedExceptionally → 替换 latch)
        check("失败后二次连接成功", m.initialized && m.connectCalls.get() == 2);
    }

    // ---- C. permit 池容量 (ConnectionsHolder.java:44-59) ----
    static void testPoolPermitExactly() {
        MiniConnectionManager.ConnectionsHolder holder = new MiniConnectionManager.ConnectionsHolder(2);
        check("池初始 permit=2", holder.freePermits() == 2);

        // init 2 成功 → permit 归零
        check("init 2 成功", holder.initConnections(2));
        check("init 后 permit=0", holder.freePermits() == 0);

        // 借 3 次: free 有 2 → 第3次等 permit (池满 → null)
        holder.acquire();
        holder.acquire();
        check("池满 acquire 返回 null (permit=0)", holder.acquire() == null);
        check("借后 permit 仍 0", holder.freePermits() == 0);

        // 归还 1 → permit=1 可再借
        holder.release("conn-0");
        check("归还后 permit=1", holder.freePermits() == 1);
        check("归还后可借新", holder.acquire() != null);
    }

    static void testInitFailureRollback() {
        // init 失败必须归还已消费 permit (ConnectionsHolder.java:141, 4.6.1 竞态教训)
        MiniConnectionManager.ConnectionsHolder holder = new MiniConnectionManager.ConnectionsHolder(3);
        holder.acquire(); // 占 1, 剩 2
        // 尝试 init 3 个 → 第 3 个无 permit → 回滚前 2 个, permit 回到 2
        check("init 超容量失败", !holder.initConnections(3));
        check("失败回滚 permit 恰 2 (uc 精确)", holder.freePermits() == 2);

        // 4.6.1 教训: 若回滚多 release 一次 → permit=3 > max → 池退化 (永不排空)
        // 正确协议断言: 不会超过 poolMax
        MiniConnectionManager.ConnectionsHolder h2 = new MiniConnectionManager.ConnectionsHolder(3);
        h2.acquire(); h2.acquire(); h2.acquire(); // 3 全占
        check("3/3 占用 permit=0", h2.freePermits() == 0);
        check("满池 acquire null", h2.acquire() == null);
    }

    public static void main(String[] args) throws Exception {
        testModeFactory();
        testSingleFlight();
        testRetryAfterFailure();
        testPoolPermitExactly();
        testInitFailureRollback();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}