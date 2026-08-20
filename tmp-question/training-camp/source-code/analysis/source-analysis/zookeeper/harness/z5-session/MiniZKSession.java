import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * MiniZKSession — Z-5 Session 核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 ZooKeeper 3.9.5 源码):
 *   A. 分桶数学: roundToNextInterval 向上取整 (宽限期)
 *      (ExpiryQueue.java:53-55)
 *   B. touch 桶迁移: update 移旧桶入新桶
 *      (ExpiryQueue.java:84-103)
 *   C. 过期循环: 到期桶批量 setSessionClosing + expire
 *      (SessionTrackerImpl.java:158-172)
 *   D. sessionId 位结构: serverId<<56 + 时间戳<<24>>>8; CONTAINER 特值跳过
 *      (SessionTrackerImpl.java:98-108)
 *
 * 纯内存模拟, 保留核心判定数学与控制流。
 */
public class MiniZKSession {

    static final long CONTAINER_EPHEMERAL_OWNER = 0xFFFFFFFFFFFFFFFEL;  // 对照 EphemeralType

    /** A: roundToNextInterval (ExpiryQueue:53-55) */
    public static long roundToNextInterval(long time, long interval) {
        return (time / interval + 1) * interval;
    }

    /** B: 简化 ExpiryQueue — 桶迁移 */
    public static class ExpiryQueue {
        final long interval;
        final Map<Long, Set<String>> expiryMap = new HashMap<>();
        final Map<String, Long> elemMap = new HashMap<>();

        ExpiryQueue(long interval) { this.interval = interval; }

        /** update (touch) — 对照 ExpiryQueue:84-103 */
        public void update(String elem, long now, long timeout) {
            long newExpiry = roundToNextInterval(now + timeout, interval);
            Set<String> set = expiryMap.computeIfAbsent(newExpiry, k -> new HashSet<>());
            set.add(elem);
            Long prev = elemMap.put(elem, newExpiry);
            if (prev != null && !prev.equals(newExpiry)) {
                Set<String> prevSet = expiryMap.get(prev);
                if (prevSet != null) prevSet.remove(elem);   // 旧桶迁移
            }
        }

        /** 到期桶提取 */
        public Set<String> poll(long expiryTime) {
            return expiryMap.remove(expiryTime);
        }
    }

    /** C: 过期循环模拟 — 返回过期会话 (对照 SessionTrackerImpl:158-172) */
    public static Set<String> runExpiration(ExpiryQueue q, long expiryTime) {
        Set<String> expired = q.poll(expiryTime);
        return expired == null ? new HashSet<>() : expired;
    }

    /** D: initializeNextSessionId (SessionTrackerImpl:98-108) */
    public static long initializeNextSessionId(long serverId, long currentTimeMs) {
        long nextSid = (currentTimeMs << 24) >>> 8;
        nextSid = nextSid | (serverId << 56);
        if (nextSid == CONTAINER_EPHEMERAL_OWNER) {
            nextSid++;   // 特值跳过 (L104-106)
        }
        return nextSid;
    }

    public static void main(String[] args) {
        // --- A: 分桶数学 ---
        long interval = 2000;  // tickTime
        assertTrue(roundToNextInterval(0, interval) == 2000, "now=0 → 桶 2000");
        assertTrue(roundToNextInterval(1500, interval) == 2000, "now=1500 → 桶 2000 (宽限期)");
        assertTrue(roundToNextInterval(2000, interval) == 4000, "now=2000 → 桶 4000 (边界下一桶)");
        assertTrue(roundToNextInterval(1500 + 5000, interval) == 8000, "now+timeout=6500 → 桶 8000");
        System.out.println("[A] roundToNextInterval 向上取整 4/4 OK");

        // --- B: touch 桶迁移 ---
        ExpiryQueue q = new ExpiryQueue(2000);
        q.update("s1", 0, 3000);      // 0+3000 → 桶 4000
        assertTrue(q.elemMap.get("s1") == 4000L && q.expiryMap.get(4000L).contains("s1"),
            "首次 touch → 桶 4000");
        q.update("s1", 1000, 3000);   // 1000+3000 → 桶 6000 (迁移)
        assertTrue(q.elemMap.get("s1") == 6000L && !q.expiryMap.get(4000L).contains("s1")
            && q.expiryMap.get(6000L).contains("s1"), "touch 迁移: 旧桶 4000 移除, 新桶 6000");
        System.out.println("[B] touch 桶迁移 2/2 OK");

        // --- C: 过期循环 ---
        ExpiryQueue q2 = new ExpiryQueue(2000);
        q2.update("s1", 0, 3000);   // 桶 4000
        q2.update("s2", 0, 3000);   // 桶 4000 (同桶批量)
        Set<String> expired = runExpiration(q2, 4000);
        assertTrue(expired.size() == 2 && expired.contains("s1") && expired.contains("s2"),
            "到期桶批量过期 2 会话");
        assertTrue(!q2.expiryMap.containsKey(4000L), "到期桶已移除");
        System.out.println("[C] 过期桶批量 2/2 OK");

        // --- D: sessionId 位结构 ---
        long id1 = initializeNextSessionId(1, 1000);
        long id2 = initializeNextSessionId(2, 1000);
        long id3 = initializeNextSessionId(1, 2000);
        assertTrue((id1 >>> 56) == 1 && (id2 >>> 56) == 2, "高 8 位 = serverId");
        assertTrue(id1 != id2, "不同 serverId 不同 id");
        assertTrue(id1 != id3, "不同时间戳不同 id");
        assertTrue(id1 != CONTAINER_EPHEMERAL_OWNER, "不撞容器特值");
        System.out.println("[D] sessionId 位结构 4/4 OK");

        System.out.println("MiniZKSession 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
