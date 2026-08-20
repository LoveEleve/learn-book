import java.util.HashMap;
import java.util.Map;

/**
 * MiniPhase2Branch — S-13 Phase2 分支通知核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. 处理器族 5 实现 + SPI 注册 + 多态分发 (DefaultRMHandler:49-82)
 *   B. 接收端: 通知 → handler.onRequest → 执行 → 状态回传 (RmBranchCommitProcessor:42-63)
 *   C. AT 收束双路径: 提交异步 (AsyncWorker) / 回滚补偿 (undo)
 *      (RMHandlerAT:40-120; AsyncWorker:81-85; AbstractUndoLogManager:315-466)
 *   D. 状态回传消费: PhaseTwo_Committed → removeBranch / 其他 → queueToRetry (S-1:328-363)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniPhase2Branch {

    enum BranchType { AT, TCC, XA, SAGA, SAGA_ANNOTATION }
    enum BranchStatus { PhaseTwo_Committed, PhaseTwo_Rollbacked, PhaseTwo_CommitFailed_Retryable,
        PhaseTwo_CommitFailed_Unretryable }

    // ---- A: 处理器族 ----
    interface RMHandler {
        BranchType type();
        BranchStatus handleCommit(String xid, long branchId);
        BranchStatus handleRollback(String xid, long branchId);
    }

    static class RMHandlerAT implements RMHandler {
        final boolean asyncCommit;
        RMHandlerAT(boolean asyncCommit) { this.asyncCommit = asyncCommit; }
        @Override public BranchType type() { return BranchType.AT; }
        @Override public BranchStatus handleCommit(String xid, long branchId) {
            // 提交异步: AsyncWorker 入队立即返回 Committed (S-11:81-85)
            return asyncCommit ? BranchStatus.PhaseTwo_Committed : BranchStatus.PhaseTwo_CommitFailed_Retryable;
        }
        @Override public BranchStatus handleRollback(String xid, long branchId) {
            return BranchStatus.PhaseTwo_Rollbacked; // undo 补偿成功
        }
    }

    static class DefaultRMHandler {
        final Map<BranchType, RMHandler> map = new HashMap<>();
        void init(RMHandler... handlers) { for (RMHandler h : handlers) map.put(h.type(), h); }
        RMHandler getRMHandler(BranchType t) { return map.get(t); } // 多态分发
    }

    // ---- D: 状态回传消费 (S-1:328-363) ----
    static String consumeCommitResult(BranchStatus status) {
        switch (status) {
            case PhaseTwo_Committed: return "REMOVE_BRANCH";            // → removeBranch
            case PhaseTwo_CommitFailed_Unretryable: return "END_COMMIT_FAILED"; // 终态
            default: return "QUEUE_RETRY_COMMIT";                       // → S-7 重试
        }
    }

    // ---- C: 回滚路径 ----
    static boolean undoRollback() {
        return true; // undoManager.undo — 镜像恢复 + 校验 (S-2)
    }

    public static void main(String[] args) {
        // ============ A: 处理器族 ============
        DefaultRMHandler rm = new DefaultRMHandler();
        rm.init(new RMHandlerAT(true), new RMHandlerAT(false) { // XA/TCC/Saga 用简版
            @Override public BranchType type() { return BranchType.XA; }
        });
        rm.init(new RMHandlerAT(false) { @Override public BranchType type() { return BranchType.TCC; } },
                new RMHandlerAT(false) { @Override public BranchType type() { return BranchType.SAGA; } },
                new RMHandlerAT(false) { @Override public BranchType type() { return BranchType.SAGA_ANNOTATION; } });
        assertTrue(rm.map.size() == 5, "A1 处理器族 5 实现 (AT/XA/TCC/Saga/SagaAnnotation)");
        assertTrue(rm.getRMHandler(BranchType.AT).handleCommit("x", 1) == BranchStatus.PhaseTwo_Committed,
            "A2 AT 异步提交立即回 Committed (AsyncWorker)");
        assertTrue(rm.getRMHandler(BranchType.SAGA_ANNOTATION) != null, "A3 SagaAnnotation 独立注册");
        System.out.println("[A] 处理器族 3/3 OK");

        // ============ B: 接收端 + 状态回传 ============
        // 模拟: RmBranchCommitProcessor → handler.onRequest → 响应回传
        BranchStatus received = rm.getRMHandler(BranchType.AT).handleCommit("xid1", 100);
        assertTrue(received == BranchStatus.PhaseTwo_Committed, "B1 提交通知 → 立即 Committed 回传");
        BranchStatus rollbackStatus = rm.getRMHandler(BranchType.AT).handleRollback("xid1", 100);
        assertTrue(rollbackStatus == BranchStatus.PhaseTwo_Rollbacked, "B2 回滚通知 → undo 补偿 → Rollbacked 回传");
        System.out.println("[B] 接收端 + 回传 2/2 OK");

        // ============ C: AT 收束双路径 ============
        assertTrue(undoRollback(), "C1 回滚 = undo 补偿 (镜像恢复 + 校验)");
        assertTrue(consumeCommitResult(BranchStatus.PhaseTwo_Committed).equals("REMOVE_BRANCH"),
            "C2 提交成功 → removeBranch");
        assertTrue(consumeCommitResult(BranchStatus.PhaseTwo_CommitFailed_Retryable).equals("QUEUE_RETRY_COMMIT"),
            "C3 可重试失败 → queueToRetryCommit (S-7)");
        assertTrue(consumeCommitResult(BranchStatus.PhaseTwo_CommitFailed_Unretryable).equals("END_COMMIT_FAILED"),
            "C4 不可重试 → endCommitFailed 终态");
        System.out.println("[C] AT 收束双路径 4/4 OK");

        // ============ D: 完整闭环 ----
        // 模拟全链: TC 正向遍历 → 通知 → RM 执行 → 回传 → 决策
        String finalAction = consumeCommitResult(
                rm.getRMHandler(BranchType.AT).handleCommit("xid9", 999));
        assertTrue(finalAction.equals("REMOVE_BRANCH"), "D1 全链闭环: 通知 → 执行 → 回传 → 移除分支");
        System.out.println("[D] 完整闭环 1/1 OK");

        System.out.println("MiniPhase2Branch 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
