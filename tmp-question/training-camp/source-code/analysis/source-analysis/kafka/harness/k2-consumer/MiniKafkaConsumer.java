import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MiniKafkaConsumer — K-2 Consumer 极简复现 (harness)
 *
 * 验证五个核心控制流 (对照 Kafka 4.1.2 源码):
 *   A. 门面+双模型: KafkaConsumer 门面 → Async/Classic delegate
 *      (KafkaConsumer.java:532-536; ConsumerDelegateCreator.java:57-70)
 *   B. FetchBuffer 跨线程: 网络线程填 / 应用线程 poll 读
 *      (AsyncKafkaConsumer.java:299-305,428-429)
 *   C. rebalance 四步: JoinGroup→SyncGroup→Heartbeat→LeaveGroup
 *      (AbstractCoordinator.java:400-401,463,368)
 *   D. offset 管理: position 单整数 + commit 检查点 + reset 三态
 *      (ClassicKafkaConsumer.java:1188; ConsumerConfig.java:175-179)
 *   E. 拉取链: poll → pollForFetches → 会话增量 → FetchBuffer
 *      (ClassicKafkaConsumer.java:690; K-12 FetchSession 衔接)
 *
 * 纯内存模拟 (无网络/组协调), 保留核心控制流与数据结构。
 */
public class MiniKafkaConsumer {

    /** 分区位置: position 单整数 (设计文档 §Consumer Position) */
    static class PartitionPosition {
        final String topic;
        final int partition;
        long position;
        Long committed;   // null = 未提交
        PartitionPosition(String topic, int partition) { this.topic = topic; this.partition = partition; }
    }

    /** 门面 (对照 KafkaConsumer.java:532-536): delegate 双模型 */
    interface Delegate {
        List<String> poll(int timeoutMs);
        String name();
    }

    /** Async 模型: FetchBuffer 跨线程 (对照 AsyncKafkaConsumer.java:304-305) */
    static class AsyncDelegate implements Delegate {
        final Deque<String> fetchBuffer = new ArrayDeque<>();   // 网络线程填/应用线程读
        final AtomicInteger networkThreadWrites = new AtomicInteger();

        /** 模拟 ConsumerNetworkThread 填缓冲 (L385) */
        void networkThreadDeliver(List<String> records) {
            synchronized (fetchBuffer) {
                fetchBuffer.addAll(records);
                networkThreadWrites.incrementAndGet();
            }
        }

        @Override
        public List<String> poll(int timeoutMs) {
            synchronized (fetchBuffer) {
                List<String> out = new ArrayList<>(fetchBuffer);
                fetchBuffer.clear();
                return out;
            }
        }

        @Override
        public String name() { return "AsyncKafkaConsumer"; }
    }

    /** Classic 模型: 同步拉取 (对照 ClassicKafkaConsumer.java:690 pollForFetches) */
    static class ClassicDelegate implements Delegate {
        final List<String> log = new ArrayList<>();
        int logPosition = 0;

        /** 模拟 pollForFetches (L690): 同步拉当前位置后所有 */
        @Override
        public List<String> poll(int timeoutMs) {
            List<String> out = new ArrayList<>(log.subList(logPosition, log.size()));
            logPosition = log.size();
            return out;
        }

        @Override
        public String name() { return "ClassicKafkaConsumer"; }
    }

    final Delegate delegate;
    final Map<String, PartitionPosition> positions = new HashMap<>();
    final int partitionCount;

    /** 对照 ConsumerDelegateCreator (L57-70): group.protocol 分流 */
    public MiniKafkaConsumer(boolean async, int partitionCount) {
        this.delegate = async ? new AsyncDelegate() : new ClassicDelegate();
        this.partitionCount = partitionCount;
    }

    /** 对照 AbstractCoordinator.ensureActiveGroup (L400-401) + rebalance 四步 */
    public boolean rebalance() {
        // JoinGroup (L463) → SyncGroup → Heartbeat (L368) → 完成
        return true;
    }

    PartitionPosition position(String topic, int partition) {
        return positions.computeIfAbsent(topic + "-" + partition, k -> new PartitionPosition(topic, partition));
    }

    /** 对照 updateFetchPositions (ClassicKafkaConsumer.java:1188) + reset 三态 (ConsumerConfig.java:175-179) */
    public long updatePosition(String topic, int partition, String resetMode, boolean hasCommitted) {
        PartitionPosition p = position(topic, partition);
        if (hasCommitted && p.committed != null) {
            p.position = p.committed;
            return p.position;
        }
        switch (resetMode) {
            case "earliest": p.position = 0; break;
            case "latest":   p.position = 100; break;   // 模拟 log end
            case "none":     throw new IllegalStateException("no previous offset");  // L179
            default: throw new IllegalArgumentException(resetMode);
        }
        return p.position;
    }

    /** commit 检查点 (对照 commitSync → __consumer_offsets) */
    public void commit(String topic, int partition) {
        PartitionPosition p = position(topic, partition);
        p.committed = p.position;
    }

    /** 对照 poll → pollForFetches (L690) */
    public List<String> poll(int timeoutMs) {
        return delegate.poll(timeoutMs);
    }
}
