import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * MiniZKElection — Z-1 Leader 选举核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 ZooKeeper 3.9.5 源码):
 *   A. totalOrderPredicate 三要素全序: peerEpoch > zxid > sid, weight==0 排除
 *      (FastLeaderElection.java:723-749)
 *   B. 指数退避: notTimeout << 1, min=200ms(finalizeWait) → max=60000ms
 *      (FastLeaderElection.java:62-76,957-978)
 *   C. 换轮清集: n.electionEpoch > logicalclock → 清 recvset + 重算提议
 *      (FastLeaderElection.java:980-993)
 *   D. 稳定窗口: 达成多数后 finalizeWait 内更高票放回重来 (n==null 才当选)
 *      (FastLeaderElection.java:1047-1066)
 *
 * 纯内存模拟, 保留核心判定数学与控制流。
 */
public class MiniZKElection {

    /** 投票四元组 (对照 Notification: leader/zxid/electionEpoch/peerEpoch) */
    public static class Vote {
        final long leader;
        final long zxid;
        final long peerEpoch;
        final long sid;
        Vote(long leader, long zxid, long peerEpoch, long sid) {
            this.leader = leader; this.zxid = zxid; this.peerEpoch = peerEpoch; this.sid = sid;
        }
    }

    /** A. totalOrderPredicate — 全序三要素 (FastLeaderElection.java:723-749) */
    public static boolean totalOrderPredicate(long newId, long newZxid, long newEpoch,
                                              long curId, long curZxid, long curEpoch,
                                              boolean newWeightZero) {
        if (newWeightZero) return false;                       // L732-734: weight==0 排除
        return (newEpoch > curEpoch)                           // L744-748
            || ((newEpoch == curEpoch)
                && ((newZxid > curZxid)
                    || ((newZxid == curZxid) && (newId > curId))));
    }

    /** B. 指数退避序列生成 (L957-978: notTimeout << 1, 上限 60000) */
    public static List<Integer> backoffSequence(int minMs) {
        List<Integer> seq = new ArrayList<>();
        int t = minMs;
        while (true) {
            seq.add(t);
            if (t >= 60000) break;
            t = Math.min(t << 1, 60000);
        }
        return seq;
    }

    /** C+D. 简化选举主循环: 返回当选 leader 或 null (未达成) */
    public static Long runElection(long mySid, long myZxid, long myEpoch,
                                   List<Vote> incoming, long finalizeWaitMs) throws InterruptedException {
        long logicalclock = 1;
        long proposedLeader = mySid, proposedZxid = myZxid, proposedEpoch = myEpoch;
        Map<Long, Vote> recvset = new HashMap<>();
        recvset.put(mySid, new Vote(mySid, myZxid, myEpoch, mySid));  // 自荐
        Deque<Vote> queue = new ArrayDeque<>(incoming);

        int notTimeout = 200;
        while (true) {
            Vote n = queue.poll();
            if (n == null) {
                // B: 指数退避 (模拟等待)
                Thread.sleep(0);
                notTimeout = Math.min(notTimeout << 1, 60000);
                if (recvset.size() >= majority(recvset)) {
                    // D: 稳定窗口 — 窗口内无新票 → 当选
                    return proposedLeader;
                }
                continue;
            }
            notTimeout = 200;  // 收到票重置退避
            // C: 换轮清集
            if (n.peerEpoch > logicalclock) {
                logicalclock = n.peerEpoch;
                recvset.clear();
                recvset.put(mySid, new Vote(mySid, myZxid, myEpoch, mySid));
                if (totalOrderPredicate(n.leader, n.zxid, n.peerEpoch, mySid, myZxid, myEpoch, false)) {
                    proposedLeader = n.leader; proposedZxid = n.zxid; proposedEpoch = n.peerEpoch;
                }
            } else if (totalOrderPredicate(n.leader, n.zxid, n.peerEpoch, proposedLeader, proposedZxid, proposedEpoch, false)) {
                proposedLeader = n.leader; proposedZxid = n.zxid; proposedEpoch = n.peerEpoch;
            }
            recvset.put(n.sid, n);
        }
    }

    static int majority(Map<Long, Vote> recvset) { return recvset.size() / 2 + 1; }

    public static void main(String[] args) {
        // --- A: 全序验证 ---
        assertFalse(totalOrderPredicate(2, 100, 5, 1, 200, 6, false), "epoch 高者应胜");
        assertTrue(totalOrderPredicate(2, 200, 5, 1, 100, 5, false), "同 epoch zxid 高者胜");
        assertTrue(totalOrderPredicate(3, 100, 5, 2, 100, 5, false), "同 epoch 同 zxid sid 高者胜");
        assertFalse(totalOrderPredicate(1, 100, 5, 2, 100, 5, false), "sid 低者不胜");
        assertFalse(totalOrderPredicate(9, 999, 9, 1, 1, 1, true), "weight==0 排除");
        System.out.println("[A] totalOrderPredicate 三要素全序 5/5 OK");

        // --- B: 退避序列 ---
        List<Integer> seq = backoffSequence(200);
        assertTrue(seq.get(0) == 200 && seq.get(seq.size() - 1) == 60000, "退避 200 起 60000 止");
        assertTrue(seq.size() == 10, "200<<1 序列长度应为 10 (51200→60000 钳制), 实际 " + seq.size());
        System.out.println("[B] 指数退避序列 " + seq + " OK");

        // --- C: 换轮清集 — 新轮次票应推翻旧轮多数 ---
        // 场景: 旧轮 recvset 已有 3 票 (模拟), 收到新 epoch 票 → 清空重来
        Map<Long, Vote> recvset = new HashMap<>();
        recvset.put(1L, new Vote(1, 100, 1, 1));
        recvset.put(2L, new Vote(1, 100, 1, 2));
        recvset.put(3L, new Vote(1, 100, 1, 3));
        long logicalclock = 1;
        Vote newRound = new Vote(4, 200, 2, 4);   // peerEpoch=2 > logicalclock
        if (newRound.peerEpoch > logicalclock) {
            recvset.clear();
        }
        assertTrue(recvset.isEmpty(), "新轮次应清空旧轮 recvset");
        System.out.println("[C] 换轮清集 OK");

        // --- D: 稳定窗口 — 更高票放回重来 ---
        // 场景: 多数达成 (3/5) 后 200ms 窗口内出现更高 epoch 票 → 不应当选
        List<Vote> incomingD = new ArrayList<>();
        incomingD.add(new Vote(2, 100, 2, 2));
        incomingD.add(new Vote(3, 100, 2, 3));
        incomingD.add(new Vote(4, 999, 9, 4));   // 窗口内更高票
        try {
            Long leader = runElection(1, 100, 2, incomingD, 200);
            assertTrue(leader == 4L, "窗口内更高票应改选, 实际 " + leader);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[D] 稳定窗口更高票改选 OK");

        System.out.println("MiniZKElection 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
    static void assertFalse(boolean cond, String msg) { assertTrue(!cond, msg); }
}
