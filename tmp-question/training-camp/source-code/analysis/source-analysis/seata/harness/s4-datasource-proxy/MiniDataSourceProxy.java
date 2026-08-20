import java.util.ArrayList;
import java.util.List;

/**
 * MiniDataSourceProxy — S-4 DataSource 代理核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. doCommit 三分支决策: 全局事务 / 仅全局锁 / 直通
 *      (ConnectionProxy.java:227-235)
 *   B. register 守卫: !hasUndoLog || !hasLockKey → 不注册分支
 *      (ConnectionProxy.java:267-281)
 *   C. LockRetryPolicy: branchRollbackOnConflict 双面语义 + FailFast 降级
 *      (ConnectionProxy.java:338-392)
 *   D. 上下文生命周期: bind → append → buildLockKeys → reset 归零
 *      (ConnectionContext.java:96-373)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniDataSourceProxy {

    // ---- 上下文 (简化 ConnectionContext) ----
    static class Context {
        String xid;
        Long branchId;
        final List<String> undoItems = new ArrayList<>();
        final List<String> lockKeys = new ArrayList<>();
        boolean autoCommitChanged;
        boolean globalLockRequire;

        boolean inGlobalTransaction() { return xid != null; }
        boolean hasUndoLog() { return !undoItems.isEmpty(); }
        boolean hasLockKey() { return !lockKeys.isEmpty(); }
        boolean isBranchRegistered() { return branchId != null; }
        String buildLockKeys() { return String.join(",", lockKeys); }
        void reset() { xid = null; branchId = null; undoItems.clear(); lockKeys.clear(); autoCommitChanged = false; }
    }

    // ---- A: doCommit 三分支 ----
    static String doCommitDecision(Context ctx) {
        if (ctx.inGlobalTransaction()) {
            return "PROCESS_GLOBAL_COMMIT";      // register + flushUndo + commit + report
        }
        if (ctx.globalLockRequire) {
            return "LOCAL_COMMIT_WITH_GLOBAL_LOCKS"; // checkLock + commit
        }
        return "PLAIN_COMMIT";                    // targetConnection.commit
    }

    // ---- B: register 守卫 ----
    static boolean shouldRegister(Context ctx) {
        return ctx.hasUndoLog() && ctx.hasLockKey(); // ConnectionProxy:267-281
    }

    // ---- C: LockRetryPolicy ----
    static boolean lockRetryPolicyDirect(boolean branchRollbackOnConflict, boolean autoCommitChanged) {
        // L355-357: true && autoCommitChanged → 直接执行 (重试在 executeAutoCommitTrue 层)
        return branchRollbackOnConflict && autoCommitChanged;
    }

    static String classifyLockConflict(String code, boolean autoCommitChanged) {
        // L372-376: FailFast 降级 — autoCommit 单语句本地锁已释放
        if ("LockKeyConflictFailFast".equals(code) && autoCommitChanged) {
            return "LockKeyConflict"; // 降级为可重试
        }
        return code;
    }

    // ---- D: 提交流程 (register → flush → commit → report) ----
    static class CommitResult {
        boolean registered;
        boolean reported;
        boolean resetDone;
    }

    static CommitResult processGlobalCommit(Context ctx, boolean reportSuccessEnable) {
        CommitResult r = new CommitResult();
        if (shouldRegister(ctx)) {
            ctx.branchId = 1L; // branchRegister
            r.registered = true;
        }
        // flushUndoLogs (S-2) — 略
        // targetConnection.commit — 略
        if (reportSuccessEnable) {
            r.reported = true; // report(PhaseOne_Done)
        }
        ctx.reset();
        r.resetDone = true;
        return r;
    }

    public static void main(String[] args) {
        // ============ A: doCommit 三分支 ============
        Context gtx = new Context();
        gtx.xid = "xid1";
        assertTrue(doCommitDecision(gtx).equals("PROCESS_GLOBAL_COMMIT"), "A1 全局事务 → 全局提交流程");
        Context lockOnly = new Context();
        lockOnly.globalLockRequire = true;
        assertTrue(doCommitDecision(lockOnly).equals("LOCAL_COMMIT_WITH_GLOBAL_LOCKS"), "A2 仅全局锁 → 锁检查+本地提交");
        Context plain = new Context();
        assertTrue(doCommitDecision(plain).equals("PLAIN_COMMIT"), "A3 普通 → 直通");
        System.out.println("[A] doCommit 三分支 3/3 OK");

        // ============ B: register 守卫 ============
        Context readOnly = new Context();
        readOnly.xid = "xid2";
        assertTrue(!shouldRegister(readOnly), "B1 无 undo 无 lockKey → 不注册 (只读事务)");
        Context writer = new Context();
        writer.xid = "xid3";
        writer.undoItems.add("u1");
        writer.lockKeys.add("t:id=1");
        assertTrue(shouldRegister(writer), "B2 有 undo+lockKey → 注册");
        Context half = new Context();
        half.xid = "xid4";
        half.undoItems.add("u1");
        assertTrue(!shouldRegister(half), "B3 有 undo 无 lockKey → 不注册 (!! 双条件)");
        System.out.println("[B] register 守卫 3/3 OK");

        // ============ C: LockRetryPolicy ============
        assertTrue(lockRetryPolicyDirect(true, true), "C1 true && autoCommitChanged → 直通 (重试在 execute 层)");
        assertTrue(!lockRetryPolicyDirect(true, false), "C2 true && 非 autoCommitChanged → doRetryOnLockConflict");
        assertTrue(!lockRetryPolicyDirect(false, true), "C3 false → doRetryOnLockConflict");
        assertTrue(classifyLockConflict("LockKeyConflictFailFast", true).equals("LockKeyConflict"),
            "C4 FailFast 降级 → LockKeyConflict (本地锁已释放)");
        assertTrue(classifyLockConflict("LockKeyConflictFailFast", false).equals("LockKeyConflictFailFast"),
            "C5 非 autoCommitChanged → 保持 FailFast");
        System.out.println("[C] LockRetryPolicy 5/5 OK");

        // ============ D: 上下文生命周期 ============
        Context ctx = new Context();
        ctx.xid = "xid5";
        ctx.undoItems.add("u1");
        ctx.lockKeys.add("t:id=1");
        assertTrue(ctx.buildLockKeys().equals("t:id=1"), "D1 buildLockKeys 拼接");
        CommitResult cr = processGlobalCommit(ctx, false); // reportSuccessEnable=false 默认
        assertTrue(cr.registered && !cr.reported, "D2 默认不 report 成功 (IS_REPORT_SUCCESS_ENABLE=false)");
        assertTrue(!ctx.inGlobalTransaction() && ctx.undoItems.isEmpty() && ctx.lockKeys.isEmpty(),
            "D3 reset 全清 (xid/branchId/undo/lockKeys)");
        CommitResult cr2 = processGlobalCommit(new Context() {{ xid = "x6"; undoItems.add("u"); lockKeys.add("k"); }}, true);
        assertTrue(cr2.reported, "D4 reportSuccessEnable=true → report");
        System.out.println("[D] 上下文生命周期 4/4 OK");

        System.out.println("MiniDataSourceProxy 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
