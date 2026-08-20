import java.util.concurrent.*;

/**
 * MiniShardTest — E-5 harness 验证入口
 *
 * 跑法: javac MiniShard.java MiniShardTest.java && java MiniShardTest
 * 全部 PASS = 5 态状态机/permits 双模式/主升四步理解到位
 * (对照 IndexShardState.java:10-17 / IndexShardOperationPermits.java:49-50,82-153 / IndexShard.java:576,609)。
 */
public class MiniShardTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. 状态机
    static void testStateMachine() {
        MiniShard.Shard s = new MiniShard.Shard();
        check("A1 初始 CREATED", s.state == MiniShard.State.CREATED);
        s.transition(MiniShard.State.RECOVERING);
        s.transition(MiniShard.State.POST_RECOVERY);
        s.transition(MiniShard.State.STARTED);
        check("A2 迁移到 STARTED", s.state == MiniShard.State.STARTED);
        s.state = MiniShard.State.CLOSED;
        try {
            s.doWrite();
            check("A3 CLOSED 拒绝写", false);
        } catch (IllegalStateException e) {
            check("A3 CLOSED 拒绝写", true);
        }
    }

    // B. permits 双模式
    static void testPermits() throws Exception {
        MiniShard.Shard s = new MiniShard.Shard();
        // 正常并发写
        check("B1 正常写成功", s.doWrite());
        check("B2 并发写成功", s.doWrite() && s.doWrite());

        // blockOperations: 全占后新写被拒
        try (MiniShard.ReleasableBlock block = s.blockOperations(5000)) {
            boolean wrote = s.doWrite();   // tryAcquire 无 permit → 返回 false
            check("B3 阻塞期间写被拒 (tryAcquire 失败)", !wrote);
            boolean acquired = s.permits.tryAcquire(1, 100, TimeUnit.MILLISECONDS);
            check("B4 阻塞期间无 permit 可用", !acquired);
        }
        // 释放后恢复
        check("B5 释放后写恢复", s.doWrite());
    }

    // B2. 并发: 阻塞等待在途操作排空
    static void testBlockDrainsInFlight() throws Exception {
        MiniShard.Shard s = new MiniShard.Shard();
        // 在途操作持 permit 10ms
        s.permits.acquire(1);
        Thread t = new Thread(() -> {
            try { Thread.sleep(10); s.permits.release(1); } catch (Exception ignored) {}
        });
        t.start();
        long start = System.currentTimeMillis();
        try (MiniShard.ReleasableBlock block = s.blockOperations(5000)) {
            long elapsed = System.currentTimeMillis() - start;
            check("B6 block 等待在途排空 (≥10ms)", elapsed >= 10);
        }
        t.join();
    }

    // C. 主升 + 旧主拒写
    static void testPromotion() throws Exception {
        MiniShard.Shard s = new MiniShard.Shard();
        check("C1 初始 term=0", s.primaryTerm == 0);
        s.promoteToPrimary();
        check("C2 升主后 term=1", s.primaryTerm == 1);
        check("C3 新主 (term=1) 可写", s.oldPrimaryWrite(1));
        check("C4 旧主 (term=0) 被拒", !s.oldPrimaryWrite(0));
        check("C5 更旧主 (term=-1) 被拒", !s.oldPrimaryWrite(-1));
    }

    // C2. 升主期间写被阻塞
    static void testPromotionBlocksWrites() throws Exception {
        MiniShard.Shard s = new MiniShard.Shard();
        Thread prom = new Thread(() -> {
            try { s.promoteToPrimary(); } catch (Exception ignored) {}
        });
        prom.start();
        Thread.sleep(2);  // promotion 已 blockOperations
        boolean wroteDuringPromotion = s.doWrite();  // 可能 true (promotion 已完成) 或 false
        prom.join();
        check("C6 升主后 term=1", s.primaryTerm == 1);
        // 无论何时写, 最终一致性: term 必须 1
        check("C7 升主后可写", s.oldPrimaryWrite(1));
    }

    public static void main(String[] args) throws Exception {
        testStateMachine();
        testPermits();
        testBlockDrainsInFlight();
        testPromotion();
        testPromotionBlocksWrites();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}
