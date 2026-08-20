import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniKafkaController — K-5 Controller 极简复现 (harness)
 *
 * 验证五个核心控制流 (对照 Kafka 4.1.2 源码):
 *   A. 写事件队列: 元数据操作转事件 → 记录 → Raft log
 *      (QuorumController.java:931-954 appendWriteEvent)
 *   B. commit 分工: active 推进 / standby 回放
 *      (QuorumController.java:956-985 handleCommit)
 *   C. broker 心跳活性: 心跳超时 → fenced (KRaft 替代 ZK)
 *      (BrokerHeartbeatManager.java:45-68)
 *   D. Leader 选举三档: preferred → ISR 内 → 出 ISR (unclean)
 *      (PartitionChangeBuilder.java:70-78)
 *   E. MetadataImage: 回放产出快照 (ClusterImage/TopicsImage)
 *      (MetadataImage.java:33-50)
 *
 * 纯内存模拟 (无 Raft 网络), 保留核心控制流与数据结构。
 */
public class MiniKafkaController {

    /** 元数据记录 (对照 CoordinatorRecord/ApiMessageAndVersion) */
    static class Record {
        final String key;
        final String value;
        Record(String key, String value) { this.key = key; this.value = value; }
    }

    /** 写事件 (对照 ControllerWriteEvent L944) */
    interface WriteOp {
        List<Record> generate();   // generateRecordsAndResult (L729-831)
    }

    /** Raft log 模拟 (对照 raft/ 层) */
    static class RaftLog {
        final List<Record> records = new ArrayList<>();
        final List<Integer> committedOffsets = new ArrayList<>();
        void append(List<Record> recs) { records.addAll(recs); }
        int commit(int offset) { committedOffsets.add(offset); return offset; }
    }

    final RaftLog raftLog = new RaftLog();
    final Map<String, String> state = new HashMap<>();        // 元数据状态 (MetadataImage 简化)
    final List<Integer> writeQueue = new ArrayList<>();        // 写事件队列 (L946-950)
    boolean isActive = true;
    int nextBrokerId = 1;

    /** 对照 appendWriteEvent (L931-954): 操作入队 */
    public void appendWriteEvent(String name, WriteOp op) {
        writeQueue.add(writeQueue.size());
        List<Record> records = op.generate();   // generateRecordsAndResult (L779-831)
        raftLog.append(records);                // 写入 Raft log
    }

    /** 对照 handleCommit (L956-985): active 推进 / standby 回放 */
    public void handleCommit(int offset) {
        raftLog.commit(offset);
        if (isActive) {
            // active: 记录已 replayed, 只推进水位 (L970-978)
        } else {
            // standby: 回放记录重建状态 (L979-985)
            for (Record r : raftLog.records) state.put(r.key, r.value);
        }
    }

    /** broker 活性 (对照 BrokerHeartbeatManager L58-68) */
    static class Broker {
        final int id;
        long lastHeartbeatMs;
        boolean fenced;
        Broker(int id, long now) { this.id = id; this.lastHeartbeatMs = now; }
    }

    final Map<Integer, Broker> brokers = new HashMap<>();

    public int registerBroker(long now) {
        Broker b = new Broker(nextBrokerId++, now);
        brokers.put(b.id, b);
        appendWriteEvent("registerBroker", () -> List.of(new Record("broker-" + b.id, "registered")));
        return b.id;
    }

    public void heartbeat(int brokerId, long now) {
        Broker b = brokers.get(brokerId);
        if (b != null) { b.lastHeartbeatMs = now; b.fenced = false; }   // 恢复心跳解除 fence
    }

    /** 心跳超时 → fenced (对照 L66-68) */
    public void checkLiveness(long now, long timeoutMs) {
        for (Broker b : brokers.values()) {
            if (now - b.lastHeartbeatMs > timeoutMs) b.fenced = true;
        }
    }

    /** Leader 选举三档 (对照 PartitionChangeBuilder L70-78) */
    public int electLeader(List<Integer> replicas, List<Integer> isr, boolean uncleanEnabled) {
        for (int r : replicas) {
            if (r == replicas.get(0) && isr.contains(r) && !brokers.get(r).fenced) return r;   // ① preferred (L70)
        }
        for (int r : replicas) {
            if (isr.contains(r) && !brokers.get(r).fenced) return r; // ② ISR 内选 (L74)
        }
        if (uncleanEnabled) {                                        // ③ 出 ISR 保在线 (L78)
            for (int r : replicas) {
                if (!brokers.get(r).fenced) return r;
            }
        }
        return -1;   // 无可用 → 分区离线
    }

    /** MetadataImage 快照 (对照 MetadataImage.java:33-50) */
    public Map<String, String> buildImage() {
        return new HashMap<>(state);   // ClusterImage/TopicsImage 简化
    }
}
