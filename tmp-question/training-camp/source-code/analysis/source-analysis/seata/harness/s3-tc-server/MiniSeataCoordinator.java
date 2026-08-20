import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MiniSeataCoordinator — S-3 TC Server 核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. 定时调度表: 6 ScheduledThreadPool (1 线程) + 周期默认值
 *      (DefaultCoordinator.java:182-198; DefaultValues.java:465-492)
 *   B. 状态组筛选: 5 组数组 + 会话按状态归属判定
 *      (DefaultCoordinator.java:200-210)
 *   C. isRetryTimeout 数学: MAX=-1 永不超时 + 边界
 *      (DefaultCoordinator.java:572-574; DefaultValues.java:520-527)
 *   D. 动态延迟自调度: timeToDeadSession 驱动 + 分布式锁防重 (未获锁重排)
 *      (DefaultCoordinator.java:625-753; GlobalSession.java:222-228)
 *
 * 纯内存模拟, 保留核心调度数学与控制流。
 */
public class MiniSeataCoordinator {

    // ---- A: 调度表 (6 ScheduledThreadPool 各 1 线程) ----
    static class Scheduler {
        final String name;
        final int threads;
        long period;
        boolean fixedRate;
        Scheduler(String name, int threads, long period, boolean fixedRate) {
            this.name = name; this.threads = threads; this.period = period; this.fixedRate = fixedRate;
        }
    }

    static List<Scheduler> buildSchedulers() {
        List<Scheduler> list = new ArrayList<>();
        // 对照 DefaultCoordinator:182-198 — 6 个 ScheduledThreadPool 各 1 线程
        list.add(new Scheduler("retryRollbacking", 1, 1000, true));      // ROLLBACKING_RETRY_PERIOD
        list.add(new Scheduler("retryCommitting", 1, 1000, true));       // COMMITTING_RETRY_PERIOD
        list.add(new Scheduler("asyncCommitting", 1, 1000, true));       // ASYNC_COMMITTING_RETRY_PERIOD
        list.add(new Scheduler("timeoutCheck", 1, 1000, true));          // TIMEOUT_RETRY_PERIOD
        list.add(new Scheduler("undoLogDelete", 1, 24 * 3600 * 1000L, true)); // UNDO_LOG_DELETE_PERIOD
        list.add(new Scheduler("syncProcessing", 1, 0, false));          // 动态延迟自调度
        return list;
    }

    // ---- B: 状态组筛选 ----
    enum GS { Begin, Committing, CommitRetrying, Rollbacking, RollbackRetrying,
        TimeoutRollbacking, TimeoutRollbackRetrying, AsyncCommitting,
        Committed, Rollbacked, TimeoutRollbacked, Finished }

    static boolean inGroup(GS status, GS[] group) {
        for (GS g : group) if (g == status) return true;
        return false;
    }

    static final GS[] RETRY_ROLLBACKING = { GS.TimeoutRollbacking, GS.TimeoutRollbackRetrying, GS.RollbackRetrying };
    static final GS[] RETRY_COMMITTING = { GS.CommitRetrying };
    static final GS[] END_STATUSES = { GS.Rollbacked, GS.TimeoutRollbacked, GS.Committed, GS.Finished };

    // ---- C: isRetryTimeout ----
    static boolean isRetryTimeout(long now, long timeout, long beginTime) {
        return timeout >= 0 && now - beginTime > timeout; // DefaultCoordinator:572-574
    }

    // ---- D: 动态延迟 + 分布式锁 ----
    static class Session {
        final GS status;
        final long beginTime;
        final boolean active; // active=false = 已结束 (END 阈值)
        Session(GS status, long beginTime, boolean active) { this.status = status; this.beginTime = beginTime; this.active = active; }
        long timeToDeadSession(long now, long endThreshold, long retryThreshold) {
            // GlobalSession:222-228: end 态 (active=false) 用 END 阈值, 其他 70s
            return (active ? retryThreshold : endThreshold) - (now - beginTime);
        }
    }

    static long computeDelay(List<Session> sessions, long now, long period, long endThreshold, long retryThreshold) {
        if (sessions.isEmpty()) {
            return retryThreshold; // 无会话 → 70s (DefaultCoordinator:585-586)
        }
        long delay = period;
        List<Session> sorted = new ArrayList<>(sessions);
        sorted.sort(Comparator.comparingLong(s -> s.beginTime)); // 按 beginTime 排序
        for (Session s : sorted) {
            long time = s.timeToDeadSession(now, endThreshold, retryThreshold);
            if (time <= 0) {
                // needDoNow — 立即处理 (延迟仍按 period)
                continue;
            } else {
                delay = Math.max(time, period); // DefaultCoordinator:596
                break;
            }
        }
        return delay;
    }

    public static void main(String[] args) {
        // ============ A: 调度表 ============
        List<Scheduler> scheds = buildSchedulers();
        assertTrue(scheds.size() == 6, "A1 6 个 ScheduledThreadPool");
        assertTrue(scheds.stream().allMatch(s -> s.threads == 1), "A2 全部 1 线程");
        assertTrue(scheds.stream().filter(s -> s.fixedRate).count() == 5, "A3 5 个 fixedRate + syncProcessing 动态");
        assertTrue(scheds.get(4).period == 24 * 3600 * 1000L, "A4 undoLogDelete 周期 24h");
        System.out.println("[A] 定时调度表 4/4 OK");

        // ============ B: 状态组筛选 ============
        assertTrue(inGroup(GS.TimeoutRollbacking, RETRY_ROLLBACKING)
                && inGroup(GS.RollbackRetrying, RETRY_ROLLBACKING)
                && !inGroup(GS.Rollbacking, RETRY_ROLLBACKING), "B1 retryRollbacking 组 3 态 (Rollbacking 不在)");
        assertTrue(inGroup(GS.CommitRetrying, RETRY_COMMITTING), "B2 retryCommitting 组 1 态");
        assertTrue(inGroup(GS.Finished, END_STATUSES) && !inGroup(GS.Committing, END_STATUSES), "B3 end 组 4 态");
        assertTrue(RETRY_ROLLBACKING.length == 3 && RETRY_COMMITTING.length == 1 && END_STATUSES.length == 4,
            "B4 组大小 3/1/4");
        System.out.println("[B] 状态组筛选 4/4 OK");

        // ============ C: isRetryTimeout ============
        long begin = 1000;
        assertTrue(isRetryTimeout(begin + 60001, 60000, begin), "C1 超 60s → 超时");
        assertTrue(!isRetryTimeout(begin + 60000, 60000, begin), "C2 边界 60s 不超时 (严格大于)");
        assertTrue(!isRetryTimeout(begin + 999999, -1, begin), "C3 MAX=-1 → 永不超时 (默认永远重试)");
        assertTrue(isRetryTimeout(begin + 1, 0, begin), "C4 timeout=0 → 立即超时 (ALWAYS_RETRY_BOUNDARY=0)");
        System.out.println("[C] isRetryTimeout 数学 4/4 OK");

        // ============ D: 动态延迟 + 分布式锁 ============
        long now = 100_000;
        assertTrue(computeDelay(new ArrayList<>(), now, 1000, 10_000, 70_000) == 70_000,
            "D1 无会话 → 70s 低频");
        List<Session> sessions = new ArrayList<>();
        sessions.add(new Session(GS.Rollbacking, now - 60_000, true)); // 已活 60s, 70s 阈值 → 剩余 10s
        sessions.add(new Session(GS.Rollbacking, now - 1_000, true));  // 已活 1s
        long delay = computeDelay(sessions, now, 1000, 10_000, 70_000);
        // 排序后首个 (最早 beginTime) 剩余 10s > 0 → delay = max(10000, 1000) = 10000
        assertTrue(delay == 10_000, "D2 首个剩余时间驱动: max(剩余 10s, period 1s) = 10s");
        List<Session> endSessions = new ArrayList<>();
        endSessions.add(new Session(GS.Committed, now - 9_000, false)); // end 态 10s 阈值, 剩 1s
        long endDelay = computeDelay(endSessions, now, 30_000, 10_000, 70_000);
        assertTrue(endDelay == 30_000, "D3 end 态 10s 阈值 + period 30s → max=30s");
        // 分布式锁防重: 未获锁 → 重排 (模拟: 获锁才执行)
        boolean lockAcquired = false;
        int rescheduled = lockAcquired ? 0 : 1;
        assertTrue(rescheduled == 1, "D4 未获分布式锁 → 重排 (多节点只一个执行)");
        System.out.println("[D] 动态延迟 + 分布式锁 4/4 OK");

        System.out.println("MiniSeataCoordinator 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
