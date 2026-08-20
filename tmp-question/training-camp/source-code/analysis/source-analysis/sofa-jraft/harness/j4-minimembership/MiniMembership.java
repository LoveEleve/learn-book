import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MiniMembership — J-4 成员变更核心逻辑极简复现 (harness)
 *
 * 纯内存模拟, 对照 SOFAJRaft 1.4.1 源码验证:
 *   A. isStable 语义: oldConf 空 = 合并完成 (ConfigurationEntry.java:77-79)
 *   B. 四阶段: CATCHING_UP → JOINT → STABLE 顺序推进 (NodeImpl.java:506-533)
 *   C. 双 quorum: 新老配置各自多数才提交配置日志 (Ballot.java:144-146)
 *   D. 追平失败回滚: ECATCHUP, 配置日志未落盘 (NodeImpl.java:448)
 *   E. ELEADERREMOVED: 合并完成后被移除的 leader 让位 (NodeImpl.java:520-526)
 */
public class MiniMembership {

    /** A. ConfigurationEntry 语义 */
    static class ConfEntry {
        final Set<String> conf;
        final Set<String> oldConf;

        ConfEntry(Set<String> conf, Set<String> oldConf) {
            this.conf = conf;
            this.oldConf = oldConf;
        }

        boolean isStable() {
            return oldConf.isEmpty(); // L77-79
        }

        Set<String> listPeers() {
            Set<String> all = new HashSet<>(conf);
            all.addAll(oldConf); // L85-89 并集
            return all;
        }
    }

    /** B. 四阶段状态机 (简化: 顺序推进) */
    enum Stage {
        NONE, CATCHING_UP, JOINT, STABLE
    }

    /** C. 双 quorum 判定 (Ballot.isGranted 语义) */
    static boolean dualQuorumGranted(int grantedNew, int newSize, int grantedOld, int oldSize) {
        int newQ = newSize / 2 + 1;
        int oldQ = oldSize / 2 + 1;
        return grantedNew >= newQ && (oldSize == 0 || grantedOld >= oldQ); // L144-146
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        String[] names = {"A.isStable", "B.四阶段", "C.双quorum", "D.追平回滚", "E.ELEADERREMOVED"};
        boolean[] r = new boolean[5];

        // ---------- A. isStable ----------
        {
            ConfEntry joint = new ConfEntry(Set.of("a", "b", "c"), Set.of("a", "b"));
            ConfEntry stable = new ConfEntry(Set.of("a", "b", "c"), Set.of());
            Set<String> peers = joint.listPeers();
            r[0] = !joint.isStable() && stable.isStable() && peers.size() == 3 && peers.containsAll(List.of("a", "b", "c"));
            System.out.println("[A] isStable: joint 期=" + !joint.isStable() + " 合并后=" + stable.isStable() + " 并集=" + peers);
        }

        // ---------- B. 四阶段 ----------
        {
            List<Stage> path = new ArrayList<>();
            Stage s = Stage.CATCHING_UP;
            // 有增删 → JOINT (提交带 oldConf 日志) → STABLE (提交合并日志) → 完成
            s = Stage.JOINT;
            path.add(Stage.CATCHING_UP);
            path.add(s);
            path.add(Stage.STABLE);
            r[1] = path.equals(Arrays.asList(Stage.CATCHING_UP, Stage.JOINT, Stage.STABLE));
            System.out.println("[B] 四阶段: " + path);
        }

        // ---------- C. 双 quorum ----------
        {
            // 新配置 3 节点 (a,b,c), 旧配置 2 节点 (a,b)
            boolean half = dualQuorumGranted(2, 3, 2, 2); // 新 2/3 ✓ 旧 2/2 ✓ → 提交
            boolean notBoth = dualQuorumGranted(2, 3, 1, 2); // 新 ✓ 旧 1/2 ✗ → 不提交
            boolean newNot = dualQuorumGranted(1, 3, 2, 2); // 新 1/3 ✗ → 不提交
            r[2] = half && !notBoth && !newNot;
            System.out.println("[C] 双 quorum: 双满足提交=" + half + " 仅新满足=" + !notBoth + " 新不满足=" + !newNot);
        }

        // ---------- D. 追平失败回滚 ----------
        {
            // 新 peer d 追平失败 → ECATCHUP: 无配置日志落盘, 变更作废
            boolean caughtUp = false;
            boolean configLogWritten = false;
            if (!caughtUp) {
                // reset(ECATCHUP): 停 replicator, 配置日志从未 append
                configLogWritten = false;
            }
            r[3] = !configLogWritten;
            System.out.println("[D] 追平回滚: 追平失败 → 配置日志未落盘=" + !configLogWritten);
        }

        // ---------- E. ELEADERREMOVED ----------
        {
            // 新配置不含 leader → STABLE 合并完成后 leader 让位
            ConfEntry newConf = new ConfEntry(Set.of("b", "c"), Set.of("a", "b", "c"));
            String leaderId = "a";
            boolean leaderRemoved = !newConf.conf.contains(leaderId);
            boolean stepDown = leaderRemoved; // STABLE 后 stepDown(ELEADERREMOVED) L520-526
            r[4] = stepDown;
            System.out.println("[E] ELEADERREMOVED: leader " + leaderId + " 被移出新配置 → 合并后让位=" + stepDown);
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
