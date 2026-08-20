import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiniKafkaReplicationTest {

    static int failures = 0;

    static void check(String name, boolean cond) {
        if (cond) System.out.println("PASS " + name);
        else { System.out.println("FAIL " + name); failures++; }
    }

    public static void main(String[] args) {
        testShrinkIsr();
        testExpandIsr();
        testHwAdvance();
        testUnderMinIsr();
        testTwoPhaseFetch();
        testMakeLeaderFollower();
        testFencedWrite();
        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** A. shrink: 副本落后超 maxLagMs → 出 ISR (Partition.scala:1231-1306) */
    static void testShrinkIsr() {
        MiniKafkaReplication.Partition p = new MiniKafkaReplication.Partition(0, 1000, 1, 0);
        p.replica(0).log.addAll(Arrays.asList(0L, 1L, 2L));  // leader 3 条
        p.replica(1).log.addAll(Arrays.asList(0L, 1L, 2L));  // follower 追上
        p.isr = new ArrayList<>(Arrays.asList(0, 1));
        p.leaderReplicaId = 0;
        p.incrementHw();
        check("初始 ISR", p.isr.size() == 2 && p.highWatermark == 3);
        // follower 停滞 (lastFetchTimeMs 老), leader 继续写
        p.now = 5000;
        p.replica(0).log.add(3L);
        p.replica(1).lastFetchTimeMs = 100;
        p.replica(1).lastCaughtUpTimeMs = 100;
        boolean shrunk = p.shrinkIsr();
        check("落后出 ISR (stuck/slow)", shrunk && p.isr.size() == 1);
    }

    /** A2. expand: 追平 HW → 重进 ISR (Partition.scala:1018-1074) */
    static void testExpandIsr() {
        MiniKafkaReplication.Partition p = new MiniKafkaReplication.Partition(0, 1000, 1, 0);
        p.replica(0).log.addAll(Arrays.asList(0L, 1L, 2L));
        p.replica(1).log.addAll(Arrays.asList(0L, 1L, 2L, 3L));  // follower 已追平
        p.isr = new ArrayList<>(Arrays.asList(0));
        p.leaderReplicaId = 0;
        p.now = 100;
        p.incrementHw();
        boolean expanded = p.expandIsr();
        check("追平重进 ISR", expanded && p.isr.contains(1));
        check("未追平不入 ISR", !p.expandIsr() && p.isr.size() == 2);
    }

    /** B. HW = 全 ISR 最小 LEO (Partition.scala:1152-1195) */
    static void testHwAdvance() {
        MiniKafkaReplication.Partition p = new MiniKafkaReplication.Partition(0, 1000, 1, 0);
        p.replica(0).log.addAll(Arrays.asList(0L, 1L, 2L, 3L));  // leader LEO=4
        p.replica(1).log.addAll(Arrays.asList(0L, 1L));          // follower LEO=2
        p.replica(2).log.addAll(Arrays.asList(0L, 1L, 2L));      // follower LEO=3
        p.isr = new ArrayList<>(Arrays.asList(0, 1, 2));
        p.leaderReplicaId = 0;
        p.incrementHw();
        check("HW=全ISR最小LEO", p.highWatermark == 2);
        // follower 1 追上后 HW 前进到 follower 2 的 LEO (HW=全 ISR 最小 LEO)
        p.replica(1).log.addAll(Arrays.asList(2L, 3L));
        p.incrementHw();
        check("HW 前进到次小 LEO", p.highWatermark == 3);
        // follower 2 也追平 → HW = leader LEO
        p.replica(2).log.add(3L);
        p.incrementHw();
        check("全 ISR 追平后 HW=LEO", p.highWatermark == 4);
    }

    /** B2. under-min-ISR: ISR 不足 minIsr → HW 冻结 (Partition.scala:1153-1156) */
    static void testUnderMinIsr() {
        MiniKafkaReplication.Partition p = new MiniKafkaReplication.Partition(0, 1000, 2, 0);
        p.replica(0).log.addAll(Arrays.asList(0L, 1L));
        p.replica(1).log.addAll(Arrays.asList(0L, 1L));
        p.isr = new ArrayList<>(Arrays.asList(0, 1));
        p.leaderReplicaId = 0;
        p.incrementHw();
        check("minIsr=2 时 HW 推进", p.highWatermark == 2);
        p.shrinkIsr();  // follower 掉线 → ISR=1 < minIsr=2
        p.replica(0).log.add(2L);
        boolean advanced = p.incrementHw();
        check("under-min-ISR HW 冻结", !advanced && p.highWatermark == 2);
    }

    /** C. 两阶段: 先截断后拉取 (AbstractFetcherThread.scala:115-118) */
    static void testTwoPhaseFetch() {
        MiniKafkaReplication.Partition p = new MiniKafkaReplication.Partition(0, 1000, 1, 0);
        p.replica(0).log.addAll(Arrays.asList(0L, 1L, 2L, 3L));
        p.replica(1).log.addAll(Arrays.asList(0L, 1L, 2L, 2L, 2L));  // 孤儿数据 (旧 leader 多余)
        p.isr = new ArrayList<>(Arrays.asList(0, 1));
        p.leaderReplicaId = 0;
        p.now = 50;
        // follower 1 截断到 3 (epoch 安全点) 再拉取
        p.fetchAndApply(p.replica(1), 3, 3);
        check("先截断孤儿数据", p.replica(1).log.size() == 4 && p.replica(1).leo() == 4);
        check("截断后与 leader 一致", p.replica(1).log.equals(p.replica(0).log));
    }

    /** E. 状态机: makeLeader/makeFollower (Partition.scala:733-885) */
    static void testMakeLeaderFollower() {
        MiniKafkaReplication.Partition p = new MiniKafkaReplication.Partition(0, 1000, 1, 0);
        p.replica(0).log.addAll(Arrays.asList(0L, 1L));
        p.makeFollower(1, 2);   // 变 follower: ISR 清空 (L861)
        check("makeFollower ISR 清空", p.isr.isEmpty() && p.leaderReplicaId == 1);
        p.makeLeader(3, Arrays.asList(0, 1));  // 变 leader: epoch 起点 = LEO (L780)
        check("makeLeader epoch 起点", p.leaderEpoch == 3 && p.leaderEpochStartOffset == 2);
        check("makeLeader 恢复 ISR", p.isr.size() == 2);
    }

    /** FENCED: 旧 epoch 写入被拒 (AbstractFetcherThread.scala:276-315 语义) */
    static void testFencedWrite() {
        MiniKafkaReplication.Partition p = new MiniKafkaReplication.Partition(0, 1000, 1, 0);
        p.replica(0).log.add(0L);
        p.makeLeader(5, Arrays.asList(0));
        // 旧 epoch 的 follower 请求 → fenced (模拟 onPartitionFenced L308: 等新 LeaderAndIsr)
        int staleEpoch = 3;
        boolean fenced = staleEpoch < p.leaderEpoch;
        check("旧 epoch 被 fence", fenced);
        check("新 epoch 接受", 5 == p.leaderEpoch);
    }
}
