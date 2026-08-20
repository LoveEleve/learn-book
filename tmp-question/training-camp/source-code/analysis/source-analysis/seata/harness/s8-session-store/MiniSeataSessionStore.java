import java.util.concurrent.locks.ReentrantLock;

/**
 * MiniSeataSessionStore — S-8 Session 存储核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. SessionMode 4 值 + 加载路由 (SessionMode.java:19-35; SessionHolder:91-154)
 *   B. 生命周期映射: onBegin→ADD / onSuccessEnd→REMOVE / onFailEnd→unlock?
 *      (AbstractSessionManager.java:73-171)
 *   C. lockAndExecute 三模式: FILE 本地锁 / DB 直通 / 分布式锁
 *      (FileSessionManager:184-193; DataBaseSessionManager:160-163)
 *   D. GlobalSessionLock tryLock 2s 数学 (GlobalSession.java:831-850)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniSeataSessionStore {

    enum SessionMode { FILE("file"), DB("db"), REDIS("redis"), RAFT("raft");
        final String name; SessionMode(String name) { this.name = name; } }

    // ---- A: 加载路由 (简化 SPI) ----
    static String loadManager(SessionMode mode) {
        switch (mode) {
            case DB:    return "DataBaseSessionManager";
            case FILE:  return "FileSessionManager";
            case REDIS: return "RedisSessionManager";
            case RAFT:  return "RaftSessionManager";
            default:    return "UNKNOWN";
        }
    }

    // ---- B: 生命周期映射 ----
    enum LogOp { GLOBAL_ADD, GLOBAL_UPDATE, GLOBAL_REMOVE, BRANCH_ADD, BRANCH_UPDATE, BRANCH_REMOVE }

    static String lifecycleMapping(String event, boolean failEndUnlockEnable) {
        switch (event) {
            case "onBegin":       return "GLOBAL_ADD";
            case "onStatusChange":return "GLOBAL_UPDATE";
            case "onSuccessEnd":  return "GLOBAL_REMOVE";
            case "onFailEnd":     return failEndUnlockEnable ? "CLEAN_AND_KEEP" : "KEEP_NO_UNLOCK";
            default:              return "UNKNOWN";
        }
    }

    // ---- C: lockAndExecute 三模式 ----
    static class GlobalSessionLock {
        final ReentrantLock lock = new ReentrantLock();
        boolean tryLock2s() { return lock.tryLock(); } // 简化 (无等待)
        void unlock() { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    static String lockStrategy(SessionMode mode) {
        switch (mode) {
            case FILE:  return "LOCAL_LOCK";   // globalSession.lock() → call → unlock
            case DB:    return "DIRECT_CALL";  // 多节点共享存储, 本地锁无意义
            case REDIS: return "REDIS_LUA_LOCK";
            case RAFT:  return "DISTRIBUTED_LOCK";
            default:    return "UNKNOWN";
        }
    }

    // ---- D: tryLock 数学 ----
    static boolean tryLockWithTimeout(GlobalSessionLock gsl, long timeoutMills, long start, long now) {
        // 简化: 现成锁且超时 → 失败
        if (now - start > timeoutMills && gsl.lock.isLocked()) {
            return false; // FailedLockGlobalTransaction
        }
        return gsl.tryLock2s();
    }

    public static void main(String[] args) {
        // ============ A: 模式 + 路由 ============
        assertTrue(SessionMode.values().length == 4, "A1 SessionMode 4 值");
        assertTrue(loadManager(SessionMode.DB).equals("DataBaseSessionManager")
                && loadManager(SessionMode.FILE).equals("FileSessionManager")
                && loadManager(SessionMode.REDIS).equals("RedisSessionManager")
                && loadManager(SessionMode.RAFT).equals("RaftSessionManager"), "A2 4 模式路由");
        System.out.println("[A] 模式 + 路由 2/2 OK");

        // ============ B: 生命周期映射 ============
        assertTrue(lifecycleMapping("onBegin", false).equals("GLOBAL_ADD"), "B1 onBegin → GLOBAL_ADD");
        assertTrue(lifecycleMapping("onSuccessEnd", false).equals("GLOBAL_REMOVE"), "B2 onSuccessEnd → GLOBAL_REMOVE");
        assertTrue(lifecycleMapping("onFailEnd", false).equals("KEEP_NO_UNLOCK"),
            "B3 onFailEnd 默认不解锁 (rollbackFailedUnlockEnable=false)");
        assertTrue(lifecycleMapping("onFailEnd", true).equals("CLEAN_AND_KEEP"),
            "B4 onFailEnd 开启解锁 → clean (S-7 交叉)");
        System.out.println("[B] 生命周期映射 4/4 OK");

        // ============ C: lockAndExecute 三模式 ============
        assertTrue(lockStrategy(SessionMode.FILE).equals("LOCAL_LOCK"), "C1 FILE → 本地锁 (ReentrantLock)");
        assertTrue(lockStrategy(SessionMode.DB).equals("DIRECT_CALL"), "C2 DB → 直通 (存储条件更新)");
        assertTrue(lockStrategy(SessionMode.REDIS).equals("REDIS_LUA_LOCK"), "C3 REDIS → Lua 锁");
        assertTrue(lockStrategy(SessionMode.RAFT).equals("DISTRIBUTED_LOCK"), "C4 RAFT → 分布式锁");
        System.out.println("[C] lockAndExecute 三模式 4/4 OK");

        // ============ D: tryLock 2s 数学 ============
        GlobalSessionLock gsl = new GlobalSessionLock();
        assertTrue(tryLockWithTimeout(gsl, 2000, 0, 1000), "D1 无竞争 → 获取成功");
        gsl.unlock();
        // 模拟他人持有: 独立线程持锁 (⚠ ReentrantLock 可重入且须持有者解锁 — 同线程无法模拟, harness 自抓)
        GlobalSessionLock gsl2 = new GlobalSessionLock();
        final boolean[] holderDone = {false};
        Thread holder = new Thread(() -> {
            gsl2.lock.lock();
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            gsl2.lock.unlock(); // 持有者线程自己释放
            holderDone[0] = true;
        });
        holder.start();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {} // 等 holder 获锁
        boolean timeout = false;
        try {
            if (gsl2.lock.isLocked() && !gsl2.lock.tryLock()) {
                timeout = true; // 他人持锁 → 获取失败 → FailedLockGlobalTransaction 语义
            }
        } finally {
            try { holder.join(); } catch (InterruptedException ignored) {} // 等 holder 释放完成
        }
        assertTrue(timeout, "D2 他人持锁 → tryLock 失败 → FailedLockGlobalTransaction 语义");
        System.out.println("[D] tryLock 2s 数学 2/2 OK");

        System.out.println("MiniSeataSessionStore 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
