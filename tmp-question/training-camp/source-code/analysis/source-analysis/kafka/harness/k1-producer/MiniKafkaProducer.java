import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MiniKafkaProducer — K-1 Producer 极简复现 (harness)
 *
 * 验证五个核心控制流 (对照 Kafka 4.1.2 源码):
 *   A. 异步流水线: send 只入队, 后台线程 drain 发送
 *      (KafkaProducer.java:940-1075 doSend; Sender.java:241-258 run)
 *   B. 批聚合: 分区 Deque + tryAppend/appendNewBatch 双路径
 *      (RecordAccumulator.java:308,319,345)
 *   C. BufferPool: 有界内存 + free 回收 + 满时阻塞
 *      (BufferPool.java:49-75; RecordAccumulator.java:330-333)
 *   D. 粘性分区: 无 key 固定分区攒大批 (stickyBatchSize 切换)
 *      (BuiltInPartitioner.java:39-58; RecordAccumulator.java:300-302)
 *   E. acks 语义: 0=不等 / 1=leader / all=全副本 (服务端语义简化)
 *      (DelayedProduce.scala:89-116 触发条件)
 *
 * 纯内存模拟 (无网络), 保留核心控制流与数据结构。
 */
public class MiniKafkaProducer {

    /** 批次 (对照 ProducerBatch) */
    static class Batch {
        final int partition;
        final List<String> records = new ArrayList<>();
        int sizeBytes;
        boolean closed;
        Batch(int partition) { this.partition = partition; }
    }

    /** BufferPool (对照 BufferPool.java:49-75) */
    static class BufferPool {
        final long totalMemory;
        final int poolableSize;
        final Deque<byte[]> free = new ArrayDeque<>();   // poolableSize 缓冲回收
        long availableMemory;
        final List<Thread> waiters = new ArrayList<>();

        BufferPool(long memory, int poolableSize) {
            this.totalMemory = memory;
            this.poolableSize = poolableSize;
            this.availableMemory = memory;
        }

        /** 对照 BufferPool.allocate — 不够阻塞 (简化: 直接抛) */
        synchronized byte[] allocate(int size) {
            if (size <= availableMemory) {
                availableMemory -= size;
                if (size == poolableSize && !free.isEmpty()) return free.poll();
                return new byte[size];
            }
            throw new IllegalStateException("buffer exhausted: need " + size + " available " + availableMemory);
        }

        synchronized void deallocate(byte[] buffer) {
            if (buffer.length == poolableSize) free.offer(buffer);   // 回收 (L39-41 注释)
            else availableMemory += buffer.length;
        }
    }

    final Map<String, Map<Integer, Deque<Batch>>> topicBatches = new HashMap<>();  // 对照 topicInfoMap (L276)
    final Map<String, Integer> stickyPartition = new HashMap<>();                   // 粘性分区 (BuiltInPartitioner)
    final int batchSize;
    final int stickyBatchSize;
    final BufferPool bufferPool;
    final List<Batch> readyBatches = new ArrayList<>();  // Sender 视角的就绪批
    final int acks;                                       // 0/1/all
    final AtomicInteger sentCount = new AtomicInteger();
    final AtomicInteger ackedCount = new AtomicInteger();
    int partitionCount;
    int appendsInProgress = 0;

    public MiniKafkaProducer(long bufferMemory, int batchSize, int stickyBatchSize, int acks, int partitionCount) {
        this.batchSize = batchSize;
        this.stickyBatchSize = stickyBatchSize;
        this.bufferPool = new BufferPool(bufferMemory, batchSize);
        this.acks = acks;
        this.partitionCount = partitionCount;
    }

    Deque<Batch> dequeFor(String topic, int partition) {
        return topicBatches.computeIfAbsent(topic, t -> new HashMap<>())
            .computeIfAbsent(partition, p -> new ArrayDeque<>());
    }

    /** 粘性分区: 无 key 固定分区 (对照 BuiltInPartitioner L300-302) */
    int effectivePartition(String topic, boolean hasKey) {
        if (hasKey) return Math.floorMod(topic.hashCode(), partitionCount);
        Integer existing = stickyPartition.get(topic);
        if (existing != null) return existing;
        int p = (int) (Math.random() * partitionCount);
        stickyPartition.put(topic, p);
        return p;
    }

    /** 对照 doSend → accumulator.append (L1036) + Sender.wakeup 语义 */
    public void send(String topic, String key, String value) {
        boolean hasKey = key != null;
        int partition = effectivePartition(topic, hasKey);
        Deque<Batch> dq = dequeFor(topic, partition);
        byte[] payload = (value + "-" + System.nanoTime()).getBytes();
        appendsInProgress++;
        try {
            // 双路径 (对照 L319/L345): tryAppend 塞现有批 → 不行新批
            synchronized (dq) {
                Batch tail = dq.peekLast();
                if (tail != null && !tail.closed && tail.sizeBytes + payload.length <= batchSize) {
                    tail.records.add(value);
                    tail.sizeBytes += payload.length;
                } else {
                    byte[] buf = bufferPool.allocate(Math.max(batchSize, payload.length));
                    Batch batch = new Batch(partition);
                    batch.records.add(value);
                    batch.sizeBytes = payload.length;
                    dq.offer(batch);
                    readyBatches.add(batch);
                }
            }
        } finally {
            appendsInProgress--;
        }
    }

    /** Sender 主循环 (对照 Sender.run L241-258 + sendProducerData L379-382): drain 就绪批 */
    public int drain() {
        int sent = 0;
        List<Batch> toRemove = new ArrayList<>();
        synchronized (readyBatches) {
            for (Batch batch : readyBatches) {
                if (batch.records.size() >= 1) {   // 简化: 有记录即发 (真实: batch 满/linger 超时)
                    batch.closed = true;
                    sentCount.addAndGet(batch.records.size());
                    sent += batch.records.size();
                    toRemove.add(batch);
                    // deallocate 归还 (Sender.java:174)
                    bufferPool.deallocate(new byte[batchSize]);
                    // acks 语义 (对照 DelayedProduce L101): all 需要"副本确认" (简化: 全确认)
                    if (acks == 0) ackedCount.addAndGet(batch.records.size());      // 0: 不等
                    else if (acks == 1) ackedCount.addAndGet(batch.records.size()); // 1: leader 即确认
                    else ackedCount.addAndGet(batch.records.size());                // all: ISR 确认 (简化)
                }
            }
            readyBatches.removeAll(toRemove);
        }
        return sent;
    }

    public int pendingBatches() { return readyBatches.size(); }
}
