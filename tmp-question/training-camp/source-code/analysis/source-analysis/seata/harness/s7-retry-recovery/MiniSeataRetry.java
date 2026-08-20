import java.util.ArrayList;
import java.util.List;

/**
 * MiniSeataRetry — S-7 重试故障恢复核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. 重试状态族: 三系 14 态分组 + 入口/终态迁移 (GlobalStatus.java:41-137; GlobalSession:871-893)
 *   B. 重试链: 失败 → queueToRetry → 定时消费 → 成功/超时终态 (DefaultCore+DefaultCoordinator)
 *   C. 死阈值数学: timeToDeadSession 70s/10s + isRetryTimeout -1 永不 + 动态延迟
 *      (DefaultValues:383-390; DefaultCoordinator:572-574)
 *   D. 终态迁移: endRollbackFailed 三态分支 (SessionHelper:249-263)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniSeataRetry {

    enum GS {
        Begin(1), Committing(2), CommitRetrying(3), Rollbacking(4), RollbackRetrying(5),
        TimeoutRollbacking(6), TimeoutRollbackRetrying(7), AsyncCommitting(8), Committed(9), CommitFailed(10),
        Rollbacked(11), RollbackFailed(12), TimeoutRollbacked(13), TimeoutRollbackFailed(14), Finished(15),
        CommitRetryTimeout(16), RollbackRetryTimeout(17);
        final int code;
        GS(int code) { this.code = code; }
    }

    // ---- A: 状态族分组 ----
    static boolean isCommitRetryFamily(GS s) {
        return s == GS.CommitRetrying || s == GS.AsyncCommitting || s == GS.CommitRetryTimeout;
    }
    static boolean isRollbackRetryFamily(GS s) {
        return s == GS.RollbackRetrying || s == GS.RollbackRetryTimeout || s == GS.TimeoutRollbackRetrying;
    }
    static boolean isTimeoutFamily(GS s) {
        return s == GS.TimeoutRollbacking || s == GS.TimeoutRollbackRetrying
                || s == GS.TimeoutRollbacked || s == GS.TimeoutRollbackFailed;
    }

    // ---- C: 阈值数学 ----
    static long timeToDeadSession(boolean active, long beginTime, long now, long endThreshold, long retryThreshold) {
        // GlobalSession:222-228: active → 70s, 已结束 → 10s
        long threshold = active ? retryThreshold : endThreshold;
        return threshold - (now - beginTime);
    }
    static boolean isRetryTimeout(long now, long timeout, long beginTime) {
        return timeout >= 0 && now - beginTime > timeout; // DefaultCoordinator:572-574
    }
    static long dynamicDelay(List<Long> beginTimes, long now, long period, long endThreshold, long retryThreshold,
                             boolean active) {
        if (beginTimes.isEmpty()) return retryThreshold; // 无会话 → 70s
        long delay = period;
        List<Long> sorted = new ArrayList<>(beginTimes);
        java.util.Collections.sort(sorted);
        for (long begin : sorted) {
            long remain = timeToDeadSession(active, begin, now, endThreshold, retryThreshold);
            if (remain > 0) {
                delay = Math.max(remain, period);
                break;
            }
        }
        return delay;
    }

    // ---- D: 终态三态 ----
    static GS endRollbackFailed(GS currentStatus, boolean isRetryTimeout, boolean isTimeoutRollbacking) {
        // SessionHelper:249-263
        if (isRetryTimeout) return GS.RollbackRetryTimeout;
        if (isTimeoutRollbacking) return GS.TimeoutRollbackFailed;
        return GS.RollbackFailed;
    }

    // ---- B: 重试链简化 (返回事件序列) ----
    static List<String> retryChain(boolean firstAttemptFails, boolean retrySucceeds) {
        List<String> events = new ArrayList<>();
        if (firstAttemptFails) {
            events.add("QUEUE_RETRY");              // queueToRetryCommit → CommitRetrying
            events.add("SCHEDULED_CONSUME");        // retry 线程池
            if (retrySucceeds) {
                events.add("BRANCH_REMOVED");
                events.add("END_COMMITTED");
            } else {
                events.add("END_COMMIT_FAILED");    // isRetryTimeout → CommitRetryTimeout
            }
        }
        return events;
    }

    public static void main(String[] args) {
        // ============ A: 状态族 ============
        assertTrue(GS.values().length == 17, "A1 枚举 17 态 (重试相关子集)");
        assertTrue(isCommitRetryFamily(GS.CommitRetrying) && isCommitRetryFamily(GS.CommitRetryTimeout),
            "A2 Commit 重试族 (Retrying/RetryTimeout/Async)");
        assertTrue(isRollbackRetryFamily(GS.RollbackRetrying) && isRollbackRetryFamily(GS.RollbackRetryTimeout),
            "A3 Rollback 重试族");
        assertTrue(isTimeoutFamily(GS.TimeoutRollbacking) && isTimeoutFamily(GS.TimeoutRollbackFailed),
            "A4 Timeout 族 4 态");
        assertTrue(!isRollbackRetryFamily(GS.Rollbacking), "A5 Rollbacking 本身不在重试族 (定时器消费 Retrying 族)");
        System.out.println("[A] 状态族 5/5 OK");

        // ============ B: 重试链 ============
        List<String> success = retryChain(true, true);
        assertTrue(success.equals(java.util.Arrays.asList(
                "QUEUE_RETRY", "SCHEDULED_CONSUME", "BRANCH_REMOVED", "END_COMMITTED")),
            "B1 失败→入队→消费→成功终态");
        List<String> fail = retryChain(true, false);
        assertTrue(fail.equals(java.util.Arrays.asList(
                "QUEUE_RETRY", "SCHEDULED_CONSUME", "END_COMMIT_FAILED")),
            "B2 失败→入队→消费→超时终态");
        assertTrue(retryChain(false, true).isEmpty(), "B3 首次成功无重试");
        System.out.println("[B] 重试链 3/3 OK");

        // ============ C: 死阈值数学 ============
        long now = 100_000;
        assertTrue(timeToDeadSession(true, now - 60_000, now, 10_000, 70_000) == 10_000,
            "C1 active 会话 70s 阈值, 已流逝 60s → 剩余 10s");
        assertTrue(timeToDeadSession(false, now - 9_000, now, 10_000, 70_000) == 1_000,
            "C2 已结束会话 10s 阈值 → 剩余 1s");
        assertTrue(!isRetryTimeout(now + 999_999, -1, now), "C3 MAX=-1 → 永不超时 (默认)");
        assertTrue(dynamicDelay(java.util.Arrays.asList(now - 60_000L), now, 1000, 10_000, 70_000, true) == 10_000,
            "C4 动态延迟 = max(剩余 10s, period 1s)");
        assertTrue(dynamicDelay(new ArrayList<>(), now, 1000, 10_000, 70_000, true) == 70_000,
            "C5 无会话 → 70s");
        System.out.println("[C] 死阈值数学 5/5 OK");

        // ============ D: 终态三态 ============
        assertTrue(endRollbackFailed(GS.Rollbacking, true, false) == GS.RollbackRetryTimeout,
            "D1 isRetryTimeout → RollbackRetryTimeout");
        assertTrue(endRollbackFailed(GS.TimeoutRollbacking, false, true) == GS.TimeoutRollbackFailed,
            "D2 Timeout 系 → TimeoutRollbackFailed");
        assertTrue(endRollbackFailed(GS.Rollbacking, false, false) == GS.RollbackFailed,
            "D3 普通失败 → RollbackFailed");
        System.out.println("[D] 终态三态 3/3 OK");

        System.out.println("MiniSeataRetry 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
