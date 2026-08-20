import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * MiniZKChain — Z-4 Processor 链核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 ZooKeeper 3.9.5 源码):
 *   A. 链式流转: Prep 校验+事务生成 → Sync 批量 → Commit 读写分离 → Final 应用
 *      (ZooKeeperServer.setupRequestProcessors + 各 Processor run)
 *   B. outstandingChanges 先行: 未提交变更对后续请求可见 (读-改-写一致性)
 *      (PrepRequestProcessor.java:165-190 getRecordForPath)
 *   C. Commit 读写分离: 读直通 / 写 per-session 等 commit / 提交匹配 (sessionId+cxid)
 *      (CommitProcessor.java:251-349)
 *   D. snapCount 随机快照: logCount > snapCount/2 + randRoll
 *      (SyncRequestProcessor.java:144-151)
 *
 * 纯内存模拟, 保留核心判定数学与控制流。
 */
public class MiniZKChain {

    static class Request {
        final long sessionId;
        final int cxid;
        final boolean isWrite;
        boolean committed;      // 提交标志 (CommitProcessor 放行)
        Request(long sessionId, int cxid, boolean isWrite) {
            this.sessionId = sessionId; this.cxid = cxid; this.isWrite = isWrite;
        }
    }

    /** A+B: 链模拟 — 返回应用的请求序列 (含 outstandingChanges 可见性) */
    public static class Chain {
        final Map<String, Integer> tree = new HashMap<>();          // path → version
        final Map<String, Integer> outstanding = new HashMap<>();   // 未提交变更
        final Queue<Request> applied = new LinkedList<>();
        // Commit 读写分离状态
        final Map<Long, Deque<Request>> pending = new HashMap<>();
        final Queue<Request> committed = new LinkedList<>();

        /** Prep 阶段: 校验 + 变更暂存 */
        public void prep(Request r, String path) {
            if (r.isWrite) {
                // 生成事务: 树版本 (outstanding 先行)
                int version = outstanding.containsKey(path) ? outstanding.get(path) : tree.getOrDefault(path, 0);
                outstanding.put(path, version + 1);
            }
        }

        /** Commit 阶段: 读直通 / 写暂存 */
        public void commit(Request r, String path) {
            if (!r.isWrite) {
                applied.add(r);                    // 读直通
                return;
            }
            pending.computeIfAbsent(r.sessionId, k -> new ArrayDeque<>()).addLast(r);
        }

        /** 提交回包到达: 匹配放行 */
        public void onCommitted(Request localWrite) {
            committed.add(localWrite);
            Deque<Request> q = pending.get(localWrite.sessionId);
            if (q != null && !q.isEmpty() && q.peek() == localWrite) {
                q.poll();
                applied.add(localWrite);
                // Final 应用: outstanding → tree
                for (Map.Entry<String, Integer> e : outstanding.entrySet()) {
                    tree.put(e.getKey(), e.getValue());
                }
                outstanding.clear();
            }
        }

        /** B: 读-改-写一致性验证 — outstanding 先行 */
        public int readVersion(String path) {
            return outstanding.containsKey(path) ? outstanding.get(path) : tree.getOrDefault(path, 0);
        }
    }

    /** C: 提交匹配 (sessionId+cxid) — 对照 CommitProcessor:325-349 */
    public static boolean commitMatches(Request head, Request localWrite) {
        return head != null && head.sessionId == localWrite.sessionId && head.cxid == localWrite.cxid;
    }

    /** D: snapCount 判定 — 对照 SyncRequestProcessor:144-151 */
    public static boolean shouldSnapshot(int logCount, int snapCount, int randRoll) {
        return logCount > (snapCount / 2 + randRoll);
    }

    public static void main(String[] args) {
        // --- A: 链流转 (读写分离) ---
        Chain chain = new Chain();
        Request w1 = new Request(1, 1, true);
        Request r1 = new Request(1, 2, false);    // 同会话读
        Request w2 = new Request(2, 1, true);     // 异会话写
        chain.prep(w1, "/a");
        chain.commit(w1, "/a");
        chain.commit(r1, "/a");                   // 读直通 (不等 commit)
        assertTrue(chain.applied.contains(r1) && !chain.applied.contains(w1),
            "读直通 (r1 应用, w1 未放行)");
        chain.prep(w2, "/b");
        chain.commit(w2, "/b");
        assertTrue(!chain.applied.contains(w2), "异会话写也不放行 (等 commit)");
        chain.onCommitted(w1);
        chain.onCommitted(w2);
        assertTrue(chain.applied.contains(w1) && chain.applied.contains(w2),
            "提交后双写放行");
        System.out.println("[A] 链流转 + 读写分离 3/3 OK");

        // --- B: outstandingChanges 先行 ---
        Chain c2 = new Chain();
        c2.prep(new Request(1, 1, true), "/x");    // 写暂存 (未提交)
        assertTrue(c2.readVersion("/x") == 1, "未提交变更对读可见 (outstanding 先行)");
        assertTrue(c2.tree.getOrDefault("/x", 0) == 0, "树尚未应用 (等 commit)");
        System.out.println("[B] outstanding 读-改-写一致性 2/2 OK");

        // --- C: 提交匹配 ---
        Request local = new Request(3, 7, true);
        assertTrue(commitMatches(new Request(3, 7, true), local), "sessionId+cxid 匹配");
        assertTrue(!commitMatches(new Request(3, 8, true), local), "cxid 不匹配");
        assertTrue(!commitMatches(new Request(4, 7, true), local), "sessionId 不匹配");
        System.out.println("[C] 提交匹配 3/3 OK");

        // --- D: snapCount 随机快照 ---
        assertTrue(!shouldSnapshot(50000, 100000, 50000), "logCount=50000 未超 snapCount/2+randRoll");
        assertTrue(shouldSnapshot(100001, 100000, 0), "logCount>snapCount/2 触发");
        assertTrue(!shouldSnapshot(100000, 100000, 50000), "randRoll 抖动延后快照");
        System.out.println("[D] snapCount 判定 3/3 OK");

        System.out.println("MiniZKChain 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
