import java.util.concurrent.*;

/**
 * MiniEngineTest — E-1 harness 验证入口
 *
 * 跑法: javac MiniEngine.java MiniEngineTest.java && java MiniEngineTest
 * 全部 PASS = Engine 计划-执行/append-only 幂等/NRT 三级可见性理解到位
 * (对照 InternalEngine.java:1305-1383 / 1059-1090 / 865-924)。
 */
public class MiniEngineTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. 计划-执行: 版本冲突在计划阶段判定
    static void testPlanExecute() {
        MiniEngine.Engine e = new MiniEngine.Engine();
        // 首次写入
        MiniEngine.IndexOp first = new MiniEngine.IndexOp("doc1", "v1", MiniEngine.UNSET_AUTO_GEN_TS, false, 0, false);
        String plan = e.plan(first);
        check("A1 新文档计划 UPDATE (无 append 优化)", plan.equals("UPDATE"));
        MiniEngine.IndexResult r = e.execute(first, plan);
        check("A2 执行成功 version=1", r.success && r.version == 1);

        // 版本冲突: 期望 version=1 但带 version=5
        MiniEngine.IndexOp conflict = new MiniEngine.IndexOp("doc1", "v5", MiniEngine.UNSET_AUTO_GEN_TS, false, 5, false);
        check("A3 版本冲突在计划阶段被拒", e.plan(conflict).equals("CONFLICT"));
        check("A4 冲突不落盘", e.committed.get("doc1").equals("v1"));

        // 正确版本更新
        MiniEngine.IndexOp ok = new MiniEngine.IndexOp("doc1", "v2", MiniEngine.UNSET_AUTO_GEN_TS, false, 1, false);
        MiniEngine.IndexResult r2 = e.execute(ok, e.plan(ok));
        check("A5 版本匹配更新 version=2", r2.success && r2.version == 2);
    }

    // B. append-only 优化: 时间戳水位防重试重复
    static void testAppendOnly() {
        MiniEngine.Engine e = new MiniEngine.Engine();
        // 首次: autoGenTs=10, 非重试 → APPEND
        MiniEngine.IndexOp a = new MiniEngine.IndexOp("d", "a", 10, false, 0, false);
        check("B1 新文档可 APPEND", e.plan(a).equals("APPEND"));
        e.execute(a, e.plan(a));
        check("B2 maxUnsafeAutoIdTimestamp=10", e.maxUnsafeAutoIdTimestamp == 10);

        // 重试: autoGenTs=10 (同水位), isRetry → UPDATE 降级
        MiniEngine.IndexOp aRetry = new MiniEngine.IndexOp("d", "a", 10, true, 0, false);
        check("B3 重试降级 UPDATE (防重复)", e.plan(aRetry).equals("UPDATE"));

        // 旧时间戳: autoGenTs=9 < 水位 → UPDATE
        MiniEngine.IndexOp b = new MiniEngine.IndexOp("d", "b", 9, false, 0, false);
        check("B4 旧时间戳降级 UPDATE", e.plan(b).equals("UPDATE"));

        // 新时间戳: autoGenTs=11 > 水位 → APPEND
        MiniEngine.IndexOp c = new MiniEngine.IndexOp("d", "c", 11, false, 0, false);
        check("B5 新时间戳 APPEND", e.plan(c).equals("APPEND"));
    }

    // C. NRT 三级可见性
    static void testNrtVisibility() {
        MiniEngine.Engine e = new MiniEngine.Engine();
        MiniEngine.IndexOp op = new MiniEngine.IndexOp("nrt", "x", MiniEngine.UNSET_AUTO_GEN_TS, false, 0, false);
        e.execute(op, e.plan(op));

        check("C1 写入后 realtime get 立即可见", e.realtimeGet("nrt") != null);
        check("C2 未 refresh 非 realtime 不可见", e.committedGet("nrt") == null);
        e.refresh();
        check("C3 refresh 后非 realtime 可见", e.committedGet("nrt") != null);

        // 删除 → 版本表标记 deleted → realtime 不可见
        e.versionMap.put("nrt", new MiniEngine.VersionValue(e.versionMap.get("nrt").version + 1, true));
        check("C4 删除后 realtime get 返回 null", e.realtimeGet("nrt") == null);
    }

    // C2. 并发: 同文档并发写最终 version 正确
    static void testConcurrentSameDoc() throws Exception {
        MiniEngine.Engine e = new MiniEngine.Engine();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            final int n = i;
            pool.submit(() -> {
                try {
                    start.await();
                    // 模拟 uid 锁串行化: synchronized 同文档 (对照 versionMap.acquireLock)
                    synchronized (e) {
                        MiniEngine.IndexOp op = new MiniEngine.IndexOp("c", n, MiniEngine.UNSET_AUTO_GEN_TS, false, 0, false);
                        MiniEngine.IndexResult r = e.execute(op, e.plan(op));
                        assert r.success;
                    }
                } catch (Exception ignored) {}
            });
        }
        start.countDown();
        pool.shutdown(); pool.awaitTermination(5, TimeUnit.SECONDS);
        check("C5 8 次并发写最终 version=8", e.versionMap.get("c").version == 8);
    }

    public static void main(String[] args) throws Exception {
        testPlanExecute();
        testAppendOnly();
        testNrtVisibility();
        testConcurrentSameDoc();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}
