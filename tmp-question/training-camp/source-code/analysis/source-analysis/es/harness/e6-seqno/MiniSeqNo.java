import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MiniSeqNo — E-6 SeqNo 复制协议极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 ES 8.12.2 源码):
 *   A. seqNo 分配 + 双 checkpoint: generateSeqNo 单调递增, processed/persisted 分离
 *      (LocalCheckpointTracker.java:83-108 generateSeqNo/markSeqNoAsProcessed/markSeqNoAsPersisted)
 *   B. 乱序水位推进: 位图记录乱序完成, checkpoint 连续前缀补齐时一次性跳跃
 *      (LocalCheckpointTracker.java:112-127 markSeqNo; 191-218 updateCheckpoint)
 *   C. globalCheckpoint: 所有 in-sync 副本 localCheckpoint 的 min; 不完整不推进
 *      (ReplicationTracker.java:1349-1370 computeGlobalCheckpoint)
 *
 * 用位图 + 水位模拟 (机制复现非完整库)。
 */
public class MiniSeqNo {

    /** LocalCheckpointTracker 简化: seqNo 分配 + 位图 + 双 checkpoint */
    static class CheckpointTracker {
        final AtomicLong nextSeqNo = new AtomicLong(0);
        final AtomicLong processedCheckpoint = new AtomicLong(-1);  // NO_OPS_PERFORMED
        final AtomicLong persistedCheckpoint = new AtomicLong(-1);
        final Set<Long> processedBits = new TreeSet<>();   // 乱序完成位图 (简化 TreeSet)
        final Set<Long> persistedBits = new TreeSet<>();

        long generateSeqNo() {
            return nextSeqNo.getAndIncrement();
        }

        void advanceMaxSeqNo(long seqNo) {
            nextSeqNo.accumulateAndGet(seqNo + 1, Math::max);
        }

        synchronized void markSeqNoAsProcessed(long seqNo) {
            markSeqNo(seqNo, processedCheckpoint, processedBits);
        }

        synchronized void markSeqNoAsPersisted(long seqNo) {
            markSeqNo(seqNo, persistedCheckpoint, persistedBits);
        }

        /** markSeqNo (LocalCheckpointTracker.java:112-127): 乱序标记 + 连续前缀跳跃 */
        private void markSeqNo(long seqNo, AtomicLong checkpoint, Set<Long> bits) {
            advanceMaxSeqNo(seqNo);
            if (seqNo <= checkpoint.get()) return;            // 幂等 (L116-119)
            bits.add(seqNo);
            if (seqNo == checkpoint.get() + 1) {
                // updateCheckpoint: 连续跳跃 (L191-218)
                long next = checkpoint.get() + 1;
                while (bits.contains(next)) {
                    checkpoint.set(next);
                    bits.remove(next);
                    next++;
                }
            }
        }
    }

    /** ReplicationTracker 简化: 副本 checkpoint 聚合 */
    static class ReplicationGroup {
        static class CopyState {
            long localCheckpoint;
            boolean inSync;
            CopyState(long lc, boolean inSync) { this.localCheckpoint = lc; this.inSync = inSync; }
        }
        final Map<String, CopyState> copies = new ConcurrentHashMap<>();
        boolean pendingInSync;                                // 有 pending 副本时不可推进

        /** computeGlobalCheckpoint (ReplicationTracker.java:1349-1370) */
        long computeGlobalCheckpoint(long fallback) {
            if (pendingInSync) return fallback;
            long min = Long.MAX_VALUE;
            for (CopyState c : copies.values()) {
                if (c.inSync) {
                    if (c.localCheckpoint == -1) return fallback;  // UNASSIGNED
                    min = Math.min(min, c.localCheckpoint);
                }
            }
            return min == Long.MAX_VALUE ? fallback : min;
        }
    }
}
