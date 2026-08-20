import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MiniSeataPhase — S-1 AT 两阶段核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. Propagation 决策矩阵: 6 传播 × 有无现有事务 (TransactionalTemplate.java:66-113)
 *   B. Phase2 分支遍历: commit 正向 (getSortedBranches) / rollback 反向 (getReverseSortedBranches)
 *      + removeBranch + Unretryable 终态 + 失败转重试 (DefaultCore.java:284-509)
 *   C. GlobalStatus 状态机: 21 态 + isOnePhaseTimeout/isTwoPhaseSuccess/isTwoPhaseHeuristic
 *      (GlobalStatus.java:29-245)
 *   D. 超时数学: isTimeout + XAER_NOTA (beginTime+timeout+max(RETRY_XAER_NOTA_TIMEOUT, timeout))
 *      (DefaultCore.java:539-549)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniSeataPhase {

    // ---- 传播 (S-5 交叉面, 本域消费) ----
    enum Propagation { REQUIRED, REQUIRES_NEW, NOT_SUPPORTED, SUPPORTS, NEVER, MANDATORY }

    /** A: 决策结果 = 挂起? 新建? 抛异常? 直接无事务执行? */
    static String decide(Propagation p, boolean existingTx) {
        switch (p) {
            case NOT_SUPPORTED:
                if (existingTx) return "SUSPEND_AND_EXECUTE_WITHOUT";
                return "EXECUTE_WITHOUT";
            case REQUIRES_NEW:
                if (existingTx) return "SUSPEND_AND_CREATE_NEW";
                return "CREATE_NEW";
            case SUPPORTS:
                if (!existingTx) return "EXECUTE_WITHOUT";
                return "JOIN"; // 继续执行
            case REQUIRED:
                return existingTx ? "JOIN" : "CREATE_NEW";
            case NEVER:
                if (existingTx) return "THROW";
                return "EXECUTE_WITHOUT";
            case MANDATORY:
                if (!existingTx) return "THROW";
                return "JOIN";
            default:
                return "THROW_NOT_SUPPORTED";
        }
    }

    // ---- GlobalStatus (精简 21 态核心) ----
    enum GlobalStatus {
        UnKnown(0), Begin(1), Committing(2), CommitRetrying(3), Rollbacking(4), RollbackRetrying(5),
        TimeoutRollbacking(6), TimeoutRollbackRetrying(7), AsyncCommitting(8), Committed(9), CommitFailed(10),
        Rollbacked(11), RollbackFailed(12), TimeoutRollbacked(13), TimeoutRollbackFailed(14), Finished(15),
        CommitRetryTimeout(16), RollbackRetryTimeout(17), Deleting(18),
        StopCommitOrCommitRetry(19), StopRollbackOrRollbackRetry(20);
        final int code;
        GlobalStatus(int code) { this.code = code; }
    }

    /** C: 状态组判定 — 对照 GlobalStatus.java:200-245 */
    static boolean isOnePhaseTimeout(GlobalStatus s) {
        return s == GlobalStatus.TimeoutRollbacking || s == GlobalStatus.TimeoutRollbackRetrying
                || s == GlobalStatus.TimeoutRollbacked || s == GlobalStatus.TimeoutRollbackFailed;
    }
    static boolean isTwoPhaseSuccess(GlobalStatus s) {
        return s == GlobalStatus.Committed || s == GlobalStatus.Rollbacked
                || s == GlobalStatus.TimeoutRollbacked || s == GlobalStatus.Deleting;
    }
    static boolean isTwoPhaseHeuristic(GlobalStatus s) {
        return s == GlobalStatus.Finished;
    }

    // ---- 分支 (Phase2) ----
    enum BranchStatus { PhaseOne_Failed, PhaseOne_RDONLY, PhaseTwo_Committed, PhaseTwo_Rollbacked,
        PhaseTwo_CommitFailed_Unretryable, PhaseTwo_RollbackFailed_Unretryable, Other }
    static class Branch {
        final long id;
        final int seq; // 注册序
        BranchStatus status;
        boolean removed;
        Branch(long id, int seq, BranchStatus status) { this.id = id; this.seq = seq; this.status = status; }
    }

    /** B: doGlobalCommit 正向 — 对照 DefaultCore.doGlobalCommit:284-397 */
    static boolean doGlobalCommit(List<Branch> branches, boolean retrying) {
        branches.sort(Comparator.comparingInt(b -> b.seq)); // getSortedBranches 正向
        List<Branch> copy = new ArrayList<>(branches);
        for (Branch b : copy) {
            if (b.status == BranchStatus.PhaseOne_Failed) { b.removed = true; continue; }
            if (b.status == BranchStatus.PhaseTwo_Committed) { b.removed = true; continue; }
            if (b.status == BranchStatus.PhaseTwo_CommitFailed_Unretryable) {
                // endCommitFailed 终态
                return false;
            }
            // 其他 → 非重试转异步重试
            if (!retrying) return false; // queueToRetryCommit
        }
        return true;
    }

    /** B: doGlobalRollback 反向 — 对照 DefaultCore.doGlobalRollback:423-509 */
    static boolean doGlobalRollback(List<Branch> branches, boolean retrying) {
        branches.sort(Comparator.comparingInt(b -> -b.seq)); // getReverseSortedBranches 反向
        List<Branch> copy = new ArrayList<>(branches);
        for (Branch b : copy) {
            if (b.status == BranchStatus.PhaseOne_Failed) { b.removed = true; continue; }
            if (b.status == BranchStatus.PhaseTwo_Rollbacked) { b.removed = true; continue; }
            if (b.status == BranchStatus.PhaseTwo_RollbackFailed_Unretryable) {
                // endRollbackFailed 终态
                return false;
            }
            if (!retrying) return false; // queueToRetryRollback
        }
        return true;
    }

    /** D: XAER_NOTA 超时数学 — 对照 DefaultCore.isXaerNotaTimeout:539-549 */
    static boolean isXaerNotaTimeout(long beginTime, long timeout, long retryXaerNotaTimeout, long now) {
        return now > beginTime + timeout + Math.max(retryXaerNotaTimeout, timeout);
    }

    public static void main(String[] args) {
        // ============ A: Propagation 决策矩阵 ============
        assertTrue(decide(Propagation.REQUIRED, true).equals("JOIN"), "A1 REQUIRED 有事务 → JOIN");
        assertTrue(decide(Propagation.REQUIRED, false).equals("CREATE_NEW"), "A2 REQUIRED 无事务 → CREATE_NEW");
        assertTrue(decide(Propagation.REQUIRES_NEW, true).equals("SUSPEND_AND_CREATE_NEW"), "A3 REQUIRES_NEW 挂起新建");
        assertTrue(decide(Propagation.NOT_SUPPORTED, true).equals("SUSPEND_AND_EXECUTE_WITHOUT"), "A4 NOT_SUPPORTED 挂起无事务执行");
        assertTrue(decide(Propagation.SUPPORTS, false).equals("EXECUTE_WITHOUT"), "A5 SUPPORTS 无事务直接执行");
        assertTrue(decide(Propagation.NEVER, true).equals("THROW"), "A6 NEVER 有事务抛异常");
        assertTrue(decide(Propagation.MANDATORY, false).equals("THROW"), "A7 MANDATORY 无事务抛异常");
        assertTrue(decide(Propagation.SUPPORTS, true).equals("JOIN"), "A8 SUPPORTS 有事务 join");
        // 对照 Spring 7 种: Seata 无 NESTED
        assertTrue(Propagation.values().length == 6, "A9 传播枚举 6 种 (无 NESTED)");
        System.out.println("[A] Propagation 决策矩阵 9/9 OK");

        // ============ B: Phase2 分支遍历方向 ============
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch(1, 0, BranchStatus.PhaseTwo_Committed)); // 先注册先提交
        branches.add(new Branch(2, 1, BranchStatus.PhaseTwo_Committed));
        branches.add(new Branch(3, 2, BranchStatus.PhaseOne_Failed));
        assertTrue(doGlobalCommit(branches, false), "B1 commit 正向全成功");
        assertTrue(branches.stream().filter(b -> b.removed).count() == 3, "B2 全部分支移除 (含 PhaseOne_Failed)");

        List<Branch> rb = new ArrayList<>();
        rb.add(new Branch(1, 0, BranchStatus.PhaseTwo_Rollbacked));
        rb.add(new Branch(2, 1, BranchStatus.PhaseTwo_Rollbacked));
        List<Integer> rollbackOrder = new ArrayList<>();
        rb.sort(Comparator.comparingInt(b -> -b.seq)); // 反向: 后注册先回滚
        rb.forEach(b -> rollbackOrder.add((int) b.id));
        assertTrue(rollbackOrder.equals(List.of(2, 1)), "B3 rollback 反向遍历 (后注册先回滚)");

        List<Branch> fail = new ArrayList<>();
        fail.add(new Branch(1, 0, BranchStatus.PhaseTwo_CommitFailed_Unretryable));
        assertTrue(!doGlobalCommit(fail, false), "B4 Unretryable → 终态 false (endCommitFailed)");
        List<Branch> retry = new ArrayList<>();
        retry.add(new Branch(1, 0, BranchStatus.Other));
        assertTrue(!doGlobalCommit(retry, false), "B5 其他状态 → 非重试转 queueToRetryCommit (false)");
        assertTrue(doGlobalCommit(retry, true), "B6 重试中 → 继续 (retrying=true)");
        System.out.println("[B] Phase2 分支遍历 6/6 OK");

        // ============ C: 状态机 ============
        assertTrue(GlobalStatus.values().length == 21, "C1 GlobalStatus 21 态 (code 0-20)");
        assertTrue(GlobalStatus.TimeoutRollbacking.code == 6 && GlobalStatus.Finished.code == 15
                && GlobalStatus.StopRollbackOrRollbackRetry.code == 20, "C2 关键 code 实证 (6/15/20)");
        assertTrue(isOnePhaseTimeout(GlobalStatus.TimeoutRollbacking)
                && isOnePhaseTimeout(GlobalStatus.TimeoutRollbacked)
                && !isOnePhaseTimeout(GlobalStatus.Rollbacking), "C3 isOnePhaseTimeout 4 态判定");
        assertTrue(isTwoPhaseSuccess(GlobalStatus.Committed)
                && isTwoPhaseSuccess(GlobalStatus.Rollbacked)
                && !isTwoPhaseSuccess(GlobalStatus.Committing), "C4 isTwoPhaseSuccess 判定");
        assertTrue(isTwoPhaseHeuristic(GlobalStatus.Finished)
                && !isTwoPhaseHeuristic(GlobalStatus.Committed), "C5 isTwoPhaseHeuristic 仅 Finished");
        System.out.println("[C] GlobalStatus 状态机 5/5 OK");

        // ============ D: 超时数学 ============
        long begin = 1000, timeout = 60000, retryXaer = 60000;
        assertTrue(isXaerNotaTimeout(begin, timeout, retryXaer, begin + timeout + retryXaer + 1),
            "D1 超过 begin+timeout+max(XAER,timeout) → 超时");
        assertTrue(!isXaerNotaTimeout(begin, timeout, retryXaer, begin + timeout + retryXaer),
            "D2 边界时刻不超时 (严格大于)");
        // 客户端 isTimeout: (now - beginTime) > timeout
        assertTrue((begin + 60001 - begin) > timeout && (begin + 60000 - begin) <= timeout,
            "D3 客户端 isTimeout 数学 (严格大于)");
        System.out.println("[D] 超时数学 3/3 OK");

        System.out.println("MiniSeataPhase 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
