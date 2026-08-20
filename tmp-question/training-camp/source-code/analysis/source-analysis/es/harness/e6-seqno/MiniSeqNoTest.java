import java.util.concurrent.*;

/**
 * MiniSeqNoTest — E-6 harness 验证入口
 *
 * 跑法: javac MiniSeqNo.java MiniSeqNoTest.java && java MiniSeqNoTest
 * 全部 PASS = seqNo 分配/双 checkpoint/乱序推进/globalCheckpoint 聚合理解到位
 * (对照 LocalCheckpointTracker.java:83-127,191-218 / ReplicationTracker.java:1349-1370)。
 */
public class MiniSeqNoTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. seqNo 分配 + 双 checkpoint
    static void testSeqNoAllocation() {
        MiniSeqNo.CheckpointTracker t = new MiniSeqNo.CheckpointTracker();
        check("A1 首个 seqNo=0", t.generateSeqNo() == 0);
        check("A2 递增", t.generateSeqNo() == 1 && t.generateSeqNo() == 2);
        t.advanceMaxSeqNo(100);   // 副本/恢复推进
        check("A3 advanceMaxSeqNo 后分配 101", t.generateSeqNo() == 101);

        // processed 与 persisted 分离
        t.markSeqNoAsProcessed(0);
        check("A4 processed=0", t.processedCheckpoint.get() == 0);
        check("A5 persisted 仍 -1", t.persistedCheckpoint.get() == -1);
        t.markSeqNoAsPersisted(0);
        check("A6 persisted=0", t.persistedCheckpoint.get() == 0);
    }

    // B. 乱序水位推进 (LocalCheckpointTrackerTests L45-67 语义)
    static void testOutOfOrder() {
        MiniSeqNo.CheckpointTracker t = new MiniSeqNo.CheckpointTracker();
        t.markSeqNoAsProcessed(2);            // 先标 2
        check("B1 乱序: checkpoint 不动", t.processedCheckpoint.get() == -1);
        t.markSeqNoAsProcessed(0);            // 补齐 0
        check("B2 补齐 0 → checkpoint=0", t.processedCheckpoint.get() == 0);
        t.markSeqNoAsProcessed(1);            // 补齐 1 → 连续跳到 2
        check("B3 补齐 1 → 连续跳 2", t.processedCheckpoint.get() == 2);
        check("B4 位图已清 (≤checkpoint)", t.processedBits.isEmpty());

        // 幂等: 重复标记已推进的
        t.markSeqNoAsProcessed(0);
        check("B5 重复标记幂等", t.processedCheckpoint.get() == 2);
    }

    // C. globalCheckpoint min 聚合
    static void testGlobalCheckpoint() {
        MiniSeqNo.ReplicationGroup g = new MiniSeqNo.ReplicationGroup();
        g.copies.put("p", new MiniSeqNo.ReplicationGroup.CopyState(10, true));
        g.copies.put("r1", new MiniSeqNo.ReplicationGroup.CopyState(7, true));
        g.copies.put("r2", new MiniSeqNo.ReplicationGroup.CopyState(3, true));
        check("C1 min of in-sync = 3", g.computeGlobalCheckpoint(0) == 3);

        // 慢副本移出 in-sync → min 提升
        g.copies.put("r2", new MiniSeqNo.ReplicationGroup.CopyState(3, false));
        check("C2 非 in-sync 不计入 min = 7", g.computeGlobalCheckpoint(0) == 7);

        // pendingInSync → fallback (不可推进)
        g.pendingInSync = true;
        check("C3 pendingInSync 回退 fallback=0", g.computeGlobalCheckpoint(0) == 0);
        g.pendingInSync = false;

        // UNASSIGNED 副本 → fallback
        g.copies.put("r3", new MiniSeqNo.ReplicationGroup.CopyState(-1, true));
        check("C4 UNASSIGNED in-sync 回退", g.computeGlobalCheckpoint(0) == 0);
    }

    // C2. 并发: 乱序完成最终水位正确
    static void testConcurrent() throws Exception {
        MiniSeqNo.CheckpointTracker t = new MiniSeqNo.CheckpointTracker();
        int n = 100;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < n; i++) {
            final long seq = i;
            pool.submit(() -> {
                try { start.await(); t.markSeqNoAsProcessed(seq); } catch (Exception ignored) {}
            });
        }
        start.countDown();
        pool.shutdown(); pool.awaitTermination(5, TimeUnit.SECONDS);
        check("C5 100 乱序完成最终 checkpoint=99", t.processedCheckpoint.get() == 99);
    }

    public static void main(String[] args) throws Exception {
        testSeqNoAllocation();
        testOutOfOrder();
        testGlobalCheckpoint();
        testConcurrent();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}
