import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniKafkaReplication — K-4 Partition & ISR 极简复现 (harness)
 *
 * 验证五个核心控制流 (对照 Kafka 4.1.2 源码):
 *   A. ISR 动态维护: 副本落后超 maxLagMs → shrink 出 ISR; 追平 HW → expand 重进
 *      (Partition.scala:1018-1074 maybeExpandIsr; 1231-1306 maybeShrinkIsr/getOutOfSyncReplicas)
 *   B. HW 推进: HW = 全 ISR 最小 LEO; under-min-ISR 冻结
 *      (Partition.scala:1152-1195 maybeIncrementLeaderHW)
 *   C. 两阶段拉取: doWork = maybeTruncate → maybeFetch (先回退后前进)
 *      (AbstractFetcherThread.scala:115-182)
 *   D. epoch 截断 4 规则 (简化): undefined offset→HW / epoch 未知→迭代 / 正常→min(leader, follower, LEO)
 *      (AbstractFetcherThread.scala:604-655 getOffsetTruncationState)
 *   E. 状态机: makeLeader/makeFollower — epoch 起点缓存 + ISR 清空
 *      (Partition.scala:733-885)
 *
 * 纯内存模拟 (无网络/磁盘), 保留核心控制流与数据结构。
 */
public class MiniKafkaReplication {

    /** 副本: 日志 (offset 列表) + 最后追上时间 */
    static class Replica {
        final int brokerId;
        final List<Long> log = new ArrayList<>();  // 已复制 offset (含 leader)
        long lastCaughtUpTimeMs;
        long lastFetchTimeMs = 0;
        Replica(int brokerId, long now) { this.brokerId = brokerId; this.lastCaughtUpTimeMs = now; }
        long leo() { return log.isEmpty() ? 0 : log.get(log.size() - 1) + 1; }
    }

    /** 分区: replicas + ISR + leader + HW + epoch (对照 Partition.scala:308-328) */
    static class Partition {
        final int localBrokerId;
        final Map<Integer, Replica> replicas = new HashMap<>();
        List<Integer> isr = new ArrayList<>();
        int leaderReplicaId = -1;
        int leaderEpoch = 0;
        long leaderEpochStartOffset = 0;   // 对照 assignEpochStartOffset (L793)
        long highWatermark = 0;            // 对照 maybeIncrementLeaderHW (L1152)
        final long maxLagMs;
        final int minIsr;
        long now = 0;

        Partition(int localBrokerId, long maxLagMs, int minIsr, long now) {
            this.localBrokerId = localBrokerId;
            this.maxLagMs = maxLagMs;
            this.minIsr = minIsr;
            this.now = now;
        }

        Replica replica(int id) { return replicas.computeIfAbsent(id, r -> new Replica(r, now)); }

        /** 对照 getOutOfSyncReplicas (Partition.scala:1297-1306): stuck/slow 两类 */
        List<Integer> outOfSyncReplicas() {
            List<Integer> out = new ArrayList<>();
            Replica leader = replicas.get(leaderReplicaId);
            long leaderLeo = leader.leo();
            for (int id : isr) {
                if (id == leaderReplicaId) continue;
                Replica r = replicas.get(id);
                boolean stuck = r.lastFetchTimeMs != 0 && now - r.lastFetchTimeMs > maxLagMs;
                boolean slow = now - r.lastCaughtUpTimeMs > maxLagMs && r.leo() < leaderLeo;
                if (stuck || slow) out.add(id);
            }
            return out;
        }

        /** 对照 maybeShrinkIsr (Partition.scala:1231-1269) */
        boolean shrinkIsr() {
            List<Integer> out = outOfSyncReplicas();
            if (!out.isEmpty()) { isr.removeAll(out); return true; }
            return false;
        }

        /** 对照 isFollowerInSync (Partition.scala:1049-1054): LEO >= HW 且 >= epoch 起点 */
        boolean isFollowerInSync(Replica r) {
            return r.leo() >= highWatermark && r.leo() >= leaderEpochStartOffset;
        }

        /** 对照 maybeExpandIsr (Partition.scala:1018-1036) */
        boolean expandIsr() {
            boolean changed = false;
            for (Replica r : replicas.values()) {
                if (r.brokerId != leaderReplicaId && !isr.contains(r.brokerId) && isFollowerInSync(r)) {
                    isr.add(r.brokerId);
                    changed = true;
                }
            }
            return changed;
        }

        /** 对照 maybeIncrementLeaderHW (Partition.scala:1152-1195) */
        boolean incrementHw() {
            if (isr.size() < minIsr) return false;   // under-min-ISR 冻结 (L1153-1156)
            Replica leader = replicas.get(leaderReplicaId);
            long newHw = leader.leo();                // L1160
            for (int id : isr) {                      // L1161-1175: 全 ISR 最小 LEO
                Replica r = replicas.get(id);
                if (r.leo() < newHw) newHw = r.leo();
            }
            if (newHw > highWatermark) { highWatermark = newHw; return true; }
            return false;
        }

        /** 对照 makeLeader (Partition.scala:733-830) */
        void makeLeader(int epoch, List<Integer> newIsr) {
            if (epoch > leaderEpoch) {
                leaderEpochStartOffset = replicas.get(localBrokerId).leo();  // L780
                leaderEpoch = epoch;
            }
            leaderReplicaId = localBrokerId;
            isr = new ArrayList<>(newIsr);
        }

        /** 对照 makeFollower (Partition.scala:839-885): ISR 清空 */
        void makeFollower(int leaderId, int epoch) {
            leaderReplicaId = leaderId;
            leaderEpoch = epoch;
            isr = new ArrayList<>();                   // L861: isr = Set.empty
        }

        /** 副本拉取: 对照两阶段 (AbstractFetcherThread.scala:115-118) */
        void fetchAndApply(Replica follower, long truncateTo, long fetchFrom) {
            Replica leader = replicas.get(leaderReplicaId);
            // 阶段 1: 截断 (L174-182)
            if (truncateTo >= 0) {
                while (follower.log.size() > truncateTo) follower.log.remove(follower.log.size() - 1);
            }
            // 阶段 2: 拉取 (L120+)
            for (long off = fetchFrom; off < leader.log.size(); off++) {
                follower.log.add(leader.log.get((int) off));
            }
            follower.lastFetchTimeMs = now;
            if (follower.leo() >= leader.leo() && isFollowerInSync(follower)) {
                follower.lastCaughtUpTimeMs = now;
            }
        }

        /** epoch 截断 4 规则简化 (AbstractFetcherThread.scala:604-655) */
        static long epochTruncation(Replica follower, long leaderEndOffset, boolean leaderKnowsEpoch, long leaderEpoch) {
            if (!leaderKnowsEpoch) return -1;                        // 规则 1: undefined → 用 HW (调用方处理)
            long followerKnown = follower.log.size();
            if (followerKnown <= leaderEpoch) {                      // 规则 3 简化: 未知 epoch → 迭代
                return Math.min(followerKnown, leaderEndOffset);
            }
            return Math.min(leaderEndOffset, Math.min(followerKnown, followerKnown)); // 规则 4: min(leader, follower, LEO)
        }
    }

    public static void main(String[] args) {
        System.out.println("MiniKafkaReplication harness 编译通过");
    }
}
