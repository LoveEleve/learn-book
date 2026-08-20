import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniKafkaGroup — K-6 Consumer Group 极简复现 (harness)
 *
 * 验证五个核心控制流 (对照 Kafka 4.1.2 源码):
 *   A. 双协议: KIP-848 heartbeat / 旧四步 join-sync-heartbeat-leave
 *      (GroupCoordinatorShard.java:457,549,570,591,970)
 *   B. KIP-848 增量: heartbeat 携带订阅 → 服务端 reconcile → epoch 推进
 *      (GroupMetadataManager.java:4691-4724,4715-4720)
 *   C. Assignor: Uniform (默认) / Range — 目标分配计算
 *      (GroupCoordinatorConfig.java:187-193; assignor/)
 *   D. Offset: commit + 过期 (OffsetMetadataManager.java:600,583)
 *   E. epoch 语义: 递增版本, -1/-2 离组 (GroupMetadataManager.java:4697-4701)
 *
 * 纯内存模拟 (无记录持久化), 保留核心控制流与数据结构。
 */
public class MiniKafkaGroup {

    /** 成员 (对照 ConsumerGroupMember.java:45) */
    static class Member {
        final String memberId;
        final List<String> subscribedTopics = new ArrayList<>();
        List<String> assignment = new ArrayList<>();   // 分配的分区
        int epoch;
        boolean active = true;
        Member(String memberId) { this.memberId = memberId; }
    }

    /** 组 (对照 ModernGroup/ClassicGroup) */
    static class Group {
        final String groupId;
        final Map<String, Member> members = new HashMap<>();
        final boolean modern;   // KIP-848 or Classic
        Group(String groupId, boolean modern) { this.groupId = groupId; this.modern = modern; }
    }

    final Map<String, Group> groups = new HashMap<>();
    int partitionCount;

    public MiniKafkaGroup(int partitionCount) { this.partitionCount = partitionCount; }

    Group group(String groupId, boolean modern) {
        return groups.computeIfAbsent(groupId, g -> new Group(g, modern));
    }

    /** 对照 consumerGroupHeartbeat (GroupMetadataManager.java:4691): epoch=-1 离组 */
    public boolean heartbeat(String groupId, String memberId, List<String> topics, int epoch) {
        Group g = group(groupId, true);
        if (epoch == -1 || epoch == -2) {   // LEAVE_GROUP_MEMBER_EPOCH (L4697-4701)
            Member m = g.members.remove(memberId);
            return m != null;
        }
        Member m = g.members.computeIfAbsent(memberId, id -> new Member(id));
        m.subscribedTopics.clear();
        m.subscribedTopics.addAll(topics);   // 增量上报 (L4715-4720)
        m.epoch = epoch;
        // KIP-848: 服务端计算目标分配 (serverAssignor L4719) — 全组 reconcile
        for (Member mm : g.members.values()) mm.assignment = computeAssignment(mm, g.members.size());
        return true;
    }

    /** Assignor: Uniform (KIP-848 默认, GroupCoordinatorConfig.java:187-193) — 全局均分 */
    List<String> computeAssignment(Member m, int memberCount) {
        List<String> out = new ArrayList<>();
        int perMember = partitionCount / memberCount;
        int start = (int) (Math.abs(m.memberId.hashCode()) % memberCount) * perMember;
        for (int i = start; i < start + perMember; i++) out.add("p" + (i % partitionCount));
        return out;
    }

    /** 旧四步: join → sync → heartbeat → leave (GroupCoordinatorShard.java:549-970) */
    public boolean classicJoin(String groupId, String memberId) {
        Group g = group(groupId, false);
        Member m = g.members.computeIfAbsent(memberId, id -> new Member(id));
        return g.members.size() >= 1;   // join 完成
    }

    public boolean classicSync(String groupId, String memberId) {
        Group g = group(groupId, false);
        Member m = g.members.get(memberId);
        if (m == null) return false;
        m.assignment = computeAssignment(m, g.members.size());
        return true;
    }

    public boolean classicHeartbeat(String groupId, String memberId) {
        Member m = group(groupId, false).members.get(memberId);
        return m != null && m.active;
    }

    public boolean classicLeave(String groupId, String memberId) {
        return group(groupId, false).members.remove(memberId) != null;
    }

    /** Offset commit (对照 OffsetMetadataManager.commitOffset L600) */
    static class OffsetEntry {
        long offset;
        long commitTimeMs;
        OffsetEntry(long offset, long now) { this.offset = offset; this.commitTimeMs = now; }
    }

    final Map<String, OffsetEntry> offsets = new HashMap<>();   // key=group-topic-partition

    public void commitOffset(String key, long offset, long now) {
        offsets.put(key, new OffsetEntry(offset, now));
    }

    /** 过期 (对照 expireTimestampMs L583: now+retention) */
    public boolean isExpired(String key, long now, long retentionMs) {
        OffsetEntry e = offsets.get(key);
        return e == null || now - e.commitTimeMs > retentionMs;
    }
}
