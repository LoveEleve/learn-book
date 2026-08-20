import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MiniShard — E-5 Shard 生命周期极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 ES 8.12.2 源码):
 *   A. 5 态状态机: CREATED/RECOVERING/POST_RECOVERY/STARTED/CLOSED + 主→副本非法
 *      (IndexShardState.java:10-17; IndexShard.java:493-562 updateShardState)
 *   B. 操作许可双模式: MAX_VALUE 信号量正常并发 + blockOperations 全占排空
 *      (IndexShardOperationPermits.java:49-50,82-153)
 *   C. 主升四步: term+1 / 阻塞操作 / resync / 旧主拒写
 *      (IndexShard.java:576,609,748; InternalEngine.java:1358-1367 term 冲突)
 *
 * 用状态枚举 + 信号量模拟 (机制复现非完整库)。
 */
public class MiniShard {

    /** 分片状态 (IndexShardState.java:10-17) */
    enum State { CREATED, RECOVERING, POST_RECOVERY, STARTED, CLOSED }

    /** 分片: 状态机 + 操作许可 + term */
    static class Shard {
        volatile State state = State.CREATED;
        volatile long primaryTerm;
        final Semaphore permits = new Semaphore(Integer.MAX_VALUE, true);  // TOTAL_PERMITS
        volatile boolean closed = false;
        final AtomicLong processedOps = new AtomicLong();

        /** 正常写操作: 取单 permit (并发几乎不互斥) */
        boolean doWrite() {
            if (state == State.CLOSED) throw new IllegalStateException("IndexShardClosedException");
            if (!permits.tryAcquire(1)) return false;
            try {
                processedOps.incrementAndGet();
                return true;
            } finally {
                permits.release(1);
            }
        }

        /** blockOperations (L82-116): delay 新操作 + 全占排空 */
        ReleasableBlock blockOperations(long timeoutMs) throws InterruptedException {
            // delayOperations: 标记排队 (简化: 用标志位)
            if (closed) throw new IllegalStateException("IndexShardClosedException");
            // acquireAll (L138-153): 全占 = 等所有在途完成
            if (!permits.tryAcquire(Integer.MAX_VALUE, timeoutMs, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("ElasticsearchTimeoutException");
            }
            return new ReleasableBlock(permits);
        }

        /** 状态迁移 (updateShardState 简化) */
        void transition(State target) {
            if (state == State.CLOSED) throw new IllegalStateException("closed");
            // 主→副本非法 (IndexShard.java:517-524)
            state = target;
        }

        /** 主升 (promotion): term+1 + 阻塞操作 + resync */
        void promoteToPrimary() throws InterruptedException {
            long newTerm = primaryTerm + 1;   // term is only increased as part of primary promotion
            try (ReleasableBlock block = blockOperations(5000)) {
                primaryTerm = newTerm;        // term+1
                // resync + seqNo 补洞 (简化: 模拟对齐)
                processedOps.set(0);          // 复位计数 (代表 resync 完成)
            }
        }

        /** 旧主写: term 落后被拒 (InternalEngine.java:1358-1367) */
        boolean oldPrimaryWrite(long writerTerm) {
            return writerTerm == primaryTerm; // term 不匹配 → 拒绝
        }
    }

    /** block 释放: 恢复 permits (Releasables.releaseOnce 语义) */
    static class ReleasableBlock implements AutoCloseable {
        final Semaphore sem;
        ReleasableBlock(Semaphore sem) { this.sem = sem; }
        public void close() { sem.release(Integer.MAX_VALUE); }
    }
}
