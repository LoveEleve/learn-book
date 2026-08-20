import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * MiniReplicator — J-2 日志复制核心逻辑极简复现 (harness)
 *
 * 纯内存模拟, 对照 SOFAJRaft 1.4.1 源码验证:
 *   A. 冲突回退两段式: 落后→批量跳 (Replicator.java:1495-1499) / 冲突→逐条减 (L1501-1504)
 *   B. 流水线乱序: seq 跳号等待, 旧响应不推进新状态 (Replicator.java:1309-1323)
 *   C. BallotBox 连续计票: 缺中间票不提交 (BallotBox.java:117-123)
 *   D. learner 不参与计票: isFollower 才 commitAt (Replicator.java:1532-1535)
 *   E. joint 双 quorum: 新老配置都多数才 granted (Ballot.java:80,89,144-146)
 */
public class MiniReplicator {

    /** A. 冲突回退 (onAppendEntriesReturned 核心逻辑 Replicator.java:1493-1509) */
    static long conflictBackoff(long nextIndex, long followerLastLogIndex, long responseLastLogIndex) {
        // response.getLastLogIndex() + 1 < nextIndex → 批量跳到 follower 末尾+1
        if (responseLastLogIndex + 1 < nextIndex) {
            return responseLastLogIndex + 1; // L1498
        }
        return nextIndex - 1; // L1504 逐条减 (有旧 term 日志)
    }

    /** B. 流水线 seq 消费 (pendingResponses 按序处理) */
    static class Pipeline {
        long requiredNextSeq = 1;
        final PriorityQueue<Long> pending = new PriorityQueue<>();

        boolean consume(long seq) {
            if (seq < requiredNextSeq) {
                return false; // 旧响应丢弃
            }
            pending.add(seq);
            // 从 requiredNextSeq 开始连续消费
            while (!pending.isEmpty() && pending.peek() == requiredNextSeq) {
                pending.poll();
                requiredNextSeq++;
            }
            return true;
        }

        long lastConsumed() {
            return requiredNextSeq - 1;
        }
    }

    /** C/D/E. 计票器 (BallotBox + Ballot 核心语义) */
    static class BallotBox {
        final List<Long> committed = new ArrayList<>();
        final Map<Long, Ballot> ballots = new HashMap<>();
        long pendingIndex = 1;
        long lastCommitted = 0;

        void appendPending(long index, List<String> peers, List<String> oldPeers) {
            Ballot b = new Ballot();
            b.init(peers, oldPeers);
            ballots.put(index, b);
        }

        /** commitAt: 逐条 grant, 仅连续达 quorum 才推进 (BallotBox.java:99-143) */
        void commitAt(long index, String peer, boolean isFollower) {
            if (!isFollower) {
                return; // D. learner 不参与计票 (Replicator.java:1532-1535)
            }
            Ballot b = ballots.get(index);
            if (b == null) {
                return;
            }
            if (!b.grant(peer)) {
                return;
            }
            // 连续推进: 从 pendingIndex 起
            while (true) {
                Ballot next = ballots.get(pendingIndex);
                if (next == null || !next.isGranted()) {
                    break;
                }
                lastCommitted = pendingIndex;
                committed.add(pendingIndex);
                pendingIndex++;
            }
        }
    }

    static class Ballot {
        final Map<String, Boolean> granted = new HashMap<>();
        int quorum = 0;
        int oldQuorum = 0;

        void init(List<String> peers, List<String> oldPeers) {
            quorum = peers.size() / 2 + 1; // Ballot.java:80
            if (oldPeers != null && !oldPeers.isEmpty()) {
                oldQuorum = oldPeers.size() / 2 + 1; // L89
            }
        }

        boolean grant(String peer) {
            Boolean g = granted.putIfAbsent(peer, true);
            if (g != null) {
                return false; // 同 peer 只计一票
            }
            quorum--;
            if (oldQuorum > 0) {
                oldQuorum--; // 简化: 假设 peer 在新老配置都出现
            }
            return true;
        }

        boolean isGranted() {
            return quorum <= 0 && oldQuorum <= 0; // Ballot.java:144-146
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        String[] names = {"A.冲突回退", "B.乱序消费", "C.连续计票", "D.learner不投票", "E.joint双quorum"};
        boolean[] r = new boolean[5];

        // ---------- A. 冲突回退两段式 ----------
        {
            long batched = conflictBackoff(100, 0, 50); // follower 落后: lastLogIndex=50 → 跳 51
            long stepped = conflictBackoff(100, 0, 99); // 索引对齐但 term 冲突 → 逐条减 99
            long step2 = conflictBackoff(99, 0, 98);
            r[0] = batched == 51 && stepped == 99 && step2 == 98;
            System.out.println("[A] 回退: 落后跳至" + batched + " 冲突减至" + stepped + " 再减至" + step2);
        }

        // ---------- B. 乱序消费 ----------
        {
            Pipeline p = new Pipeline();
            p.consume(3); // 乱序到达
            p.consume(1); // 1 到 → 消费 1, 等 2
            long after1 = p.lastConsumed();
            p.consume(2); // 2 到 → 消费 2,3
            long after2 = p.lastConsumed();
            boolean stale = !p.consume(2); // 旧 seq 拒绝
            r[1] = after1 == 1 && after2 == 3 && stale;
            System.out.println("[B] 乱序: 先到3后到1时已消费=" + after1 + " 补齐后=" + after2 + " 旧seq拒绝=" + stale);
        }

        // ---------- C. 连续计票 ----------
        {
            BallotBox bb = new BallotBox();
            List<String> peers = List.of("a", "b", "c");
            bb.appendPending(1, peers, null);
            bb.appendPending(2, peers, null);
            bb.appendPending(3, peers, null);
            bb.commitAt(1, "a", true);
            bb.commitAt(1, "b", true); // 1 达 quorum (2/3)
            bb.commitAt(3, "a", true); // 3 的票先到 — 但不能越过 2
            bb.commitAt(2, "b", true);
            bb.commitAt(2, "a", true); // 2 达 quorum → 推进 2; 3 仍缺票 → 停在 2
            boolean c1 = bb.lastCommitted == 2; // 3 未达 quorum, 正确停住
            // 3 补齐最后一票 → 连续推进 3
            bb.commitAt(3, "b", true);
            boolean c2 = bb.lastCommitted == 3 && bb.committed.equals(List.of(1L, 2L, 3L));
            r[2] = c1 && c2;
            System.out.println("[C] 连续计票: 缺票停住=" + c1 + " 补票后 committed=" + bb.committed);
        }

        // ---------- D. learner 不参与计票 ----------
        {
            BallotBox bb = new BallotBox();
            List<String> peers = List.of("a", "b", "c");
            bb.appendPending(1, peers, null);
            bb.commitAt(1, "a", true);
            bb.commitAt(1, "learner", false); // learner 确认无效
            bb.commitAt(1, "b", true);
            r[3] = bb.lastCommitted == 1 && bb.ballots.get(1L).quorum == 0;
            System.out.println("[D] learner 不投票: learner 确认后 quorum 未减 (仅 a,b 计票)");
        }

        // ---------- E. joint 双 quorum ----------
        {
            BallotBox bb = new BallotBox();
            List<String> newPeers = List.of("a", "b", "c");
            List<String> oldPeers = List.of("a", "b", "c", "d", "e"); // 旧配置 5 节点
            bb.appendPending(1, newPeers, oldPeers);
            bb.commitAt(1, "a", true);
            bb.commitAt(1, "b", true); // 新 quorum 满足 (2/3)
            long before = bb.lastCommitted;
            bb.commitAt(1, "c", true);
            bb.commitAt(1, "d", true); // 旧 quorum 3/5 满足
            r[4] = before == 0 && bb.lastCommitted == 1;
            System.out.println("[E] joint 双 quorum: 新配置 2/3 时未提交=" + (before == 0) + " 旧配置 3/5 补齐后提交=" + (bb.lastCommitted == 1));
        }

        int p = 0, f = 0;
        for (boolean x : r) {
            if (x) {
                p++;
            } else {
                f++;
            }
        }
        System.out.println("== 结果: " + p + " PASS / " + f + " FAIL ==");
        for (int i = 0; i < names.length; i++) {
            System.out.println((r[i] ? "  PASS " : "  FAIL ") + names[i]);
        }
        System.exit(f > 0 ? 1 : 0);
    }
}
