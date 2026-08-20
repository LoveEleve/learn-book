import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MiniRLock — RD-2 RLock+Watchdog 极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 Redisson 4.6.2 源码):
 *   A. 加锁 Lua 可重入语义: exists/hexists → hincrby+1+pexpire; else pttl
 *      (RedissonLock.java:214-224)
 *   B. 解锁 Lua owner 校验: hexists 校验 → hincrby-1 → counter>0 续期 / =0 删除+publish
 *      (RedissonLock.java:336-360)
 *   C. Watchdog 周期: lease/3 固定心跳; 锁尽则停 (RenewalTask.java:62-70,97-134)
 *
 * 用"哈希存储"模拟锁 (lockKey → {field=threadId, count}), 机制复现非完整库。
 */
public class MiniRLock {

    // 锁存储: lockKey → HashMap<holder, count> (Redis hash)
    static class LockStore {
        final Map<String, Map<String, Integer>> data = new ConcurrentHashMap<>();
        volatile long currentTime = 0; // 模拟服务端时间

        // 加锁 Lua (RedissonLock:214-224): 锁不存在或已是本线程 → 计数+1+续命; 否则返回剩余 ttl
        // synchronized 模拟 Lua 原子性 (真实 Redis eval 单线程执行, 无双线程竞态)
        synchronized Long tryLock(String lockKey, String threadId, long leaseMs) {
            Map<String, Integer> holders = data.computeIfAbsent(lockKey, k -> new ConcurrentHashMap<>());
            if (holders.isEmpty() || holders.containsKey(threadId)) {
                holders.put(threadId, holders.getOrDefault(threadId, 0) + 1);
                return null; // 获取成功
            }
            return leaseMs; // 已被他人持有, return pttl (简化 = leaseMs)
        }
        // 注意: synchronized Long 语法合法 — synchronized 修饰方法, Long 是返回类型

        // 解锁 Lua (RedissonLock:348-360): owner 校验 + 递减 + counter>0 续期 / =0 删+发布
        boolean unlock(String lockKey, String threadId, long leaseMs) {
            Map<String, Integer> holders = data.get(lockKey);
            if (holders == null || !holders.containsKey(threadId)) {
                return false; // 非 owner, 拒绝 (L349-351)
            }
            int counter = holders.get(threadId) - 1;
            if (counter > 0) {
                holders.put(threadId, counter); // 重入未完, 续期不删
                return true;
            }
            holders.remove(threadId);
            if (holders.isEmpty()) {
                data.remove(lockKey); // 全释放 → 删除锁 + (真实场景 publish)
            }
            return true;
        }

        // forceUnlock (L336-346): 无条件 del
        boolean forceUnlock(String lockKey) {
            return data.remove(lockKey) != null;
        }
    }

    // Watchdog 调度器 (RenewalTask:62-70 + CAS 单例)
    static class Watchdog {
        final LockStore store;
        final long leaseMs;
        final AtomicLong renewCount = new AtomicLong();
        volatile boolean running;
        final Object monitor = new Object();

        Watchdog(LockStore store, long leaseMs) {
            this.store = store;
            this.leaseMs = leaseMs;
        }

        // 线程持有期间的心跳
        void heartbeat(String lockKey, String threadId) {
            // 每 lease/3 续期一次 (用计数器模拟周期)
            renewCount.incrementAndGet();
            Map<String, Integer> holders = store.data.get(lockKey);
            if (holders == null || !holders.containsKey(threadId)) {
                stop(); // 锁已不存在 → 停止心跳 (RenewalTask:97-134)
            }
        }

        void stop() { running = false; }

        void startNewHeartbeatIfIdle(String lockKey, String threadId) {
            synchronized (monitor) {
                if (!running) {
                    running = true; // tryRun (AtomicBoolean CAS 语义)
                    heartbeat(lockKey, threadId);
                }
            }
        }
    }
}