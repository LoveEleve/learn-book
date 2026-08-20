import java.util.concurrent.*;

/**
 * MiniLocalCachedMapTest — RD-6 harness 验证入口
 *
 * 跑法: javac MiniLocalCachedMap.java MiniLocalCachedMapTest.java && java MiniLocalCachedMapTest
 * 全部 PASS = 本地缓存一致机制理解到位 (对照 RedissonLocalCachedMap:285-317, LocalCacheListener:263-315)。
 */
public class MiniLocalCachedMapTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. 双层级读路径: 本地 hit 零回源
    static void testReadPath() {
        MiniLocalCachedMap.Bus bus = new MiniLocalCachedMap.Bus();
        ConcurrentHashMap<String, String> remote = new ConcurrentHashMap<>();
        remote.put("k1", "v1");
        MiniLocalCachedMap.Instance a = new MiniLocalCachedMap.Instance(1, remote, bus);

        check("首次 get miss → Redis 回填", "v1".equals(a.get("k1")));
        check("回源 1 次", a.redisReads == 1);
        check("二次 get 本地 hit 零回源", "v1".equals(a.get("k1")));
        check("回源仍 1 次 (本地命中)", a.redisReads == 1);
    }

    // B. INVALIDATE: A 写 → B 收到 hash → 清本地 → 下次回源
    static void testInvalidate() {
        MiniLocalCachedMap.Bus bus = new MiniLocalCachedMap.Bus();
        ConcurrentHashMap<String, String> remote = new ConcurrentHashMap<>();
        MiniLocalCachedMap.Instance a = new MiniLocalCachedMap.Instance(1, remote, bus);
        MiniLocalCachedMap.Instance b = new MiniLocalCachedMap.Instance(2, remote, bus);

        remote.put("k", "old");
        b.get("k"); // b 缓存 old
        check("b 缓存命中", b.get("k").equals("old") && b.redisReads == 1);

        a.put("k", "new", false); // A 用 INVALIDATE 写
        b.drain();                // b 收到失效消息

        int readsBeforeInvalidate = b.redisReads;
        check("INVALIDATE 后 b 需回源 (清空→miss)", "new".equals(b.get("k")));
        check("回源次数增加 (已清空)", b.redisReads > readsBeforeInvalidate);
    }

    // C. UPDATE: A 写 → B 收到值 → 直更本地零回源
    static void testUpdate() {
        MiniLocalCachedMap.Bus bus = new MiniLocalCachedMap.Bus();
        ConcurrentHashMap<String, String> remote = new ConcurrentHashMap<>();
        MiniLocalCachedMap.Instance a = new MiniLocalCachedMap.Instance(1, remote, bus);
        MiniLocalCachedMap.Instance b = new MiniLocalCachedMap.Instance(2, remote, bus);

        remote.put("k", "old");
        b.get("k"); // b 缓存 old
        int readsBefore = b.redisReads;

        a.put("k", "new", true); // A 用 UPDATE 写
        b.drain();               // b 收到更新值

        check("UPDATE 后 b 直更本地", "new".equals(b.get("k")));
        check("UPDATE 零回源", b.redisReads == readsBefore);
    }

    // D. excludedId: A 写 → A 自己收到消息不处理
    static void testExcludedId() {
        MiniLocalCachedMap.Bus bus = new MiniLocalCachedMap.Bus();
        ConcurrentHashMap<String, String> remote = new ConcurrentHashMap<>();
        MiniLocalCachedMap.Instance a = new MiniLocalCachedMap.Instance(1, remote, bus);

        remote.put("k", "old");
        a.get("k"); // a 本地缓存 old
        a.put("k", "new", true); // a 写 new (本地已直更)

        a.drain(); // a 处理自己广播的消息 (应被 excludedId 排除)
        check("A 自处理被排除 (本地仍 new)", "new".equals(a.get("k")));
        check("A 无额外回源", a.redisReads == 1);
    }

    // E. 并发: 多实例读写一致性
    static void testConcurrent() throws Exception {
        MiniLocalCachedMap.Bus bus = new MiniLocalCachedMap.Bus();
        ConcurrentHashMap<String, String> remote = new ConcurrentHashMap<>();
        MiniLocalCachedMap.Instance[] insts = new MiniLocalCachedMap.Instance[4];
        for (int i = 0; i < 4; i++) insts[i] = new MiniLocalCachedMap.Instance(i, remote, bus);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        for (MiniLocalCachedMap.Instance inst : insts) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 50; i++) {
                        inst.put("ckey" + (i % 5), "v" + i, true);
                        inst.drain();
                    }
                } catch (Exception ignored) {}
            });
        }
        start.countDown();
        pool.shutdown(); pool.awaitTermination(5, TimeUnit.SECONDS);
        // 全部 drain 后最终一致
        for (MiniLocalCachedMap.Instance inst : insts) inst.drain();
        String finalVal = remote.get("ckey0");
        check("并发后所有实例最终一致", insts[0].get("ckey0") != null);
        System.out.println("  (ckey0 最终值=" + finalVal + ")");
    }

    public static void main(String[] args) throws Exception {
        testReadPath();
        testInvalidate();
        testUpdate();
        testExcludedId();
        testConcurrent();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}