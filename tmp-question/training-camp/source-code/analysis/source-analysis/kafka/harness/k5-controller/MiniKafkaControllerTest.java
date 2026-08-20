import java.util.List;
import java.util.Map;

public class MiniKafkaControllerTest {

    static int failures = 0;

    static void check(String name, boolean cond) {
        if (cond) System.out.println("PASS " + name);
        else { System.out.println("FAIL " + name); failures++; }
    }

    public static void main(String[] args) {
        testWriteEventQueue();
        testStandbyReplay();
        testActiveNoReplay();
        testHeartbeatFenced();
        testElectionPreferred();
        testElectionIsr();
        testElectionUnclean();
        testElectionOffline();
        testMetadataImage();
        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** A. 写事件队列: 操作 → 记录 → Raft log (QuorumController L931-954) */
    static void testWriteEventQueue() {
        MiniKafkaController c = new MiniKafkaController();
        int b = c.registerBroker(0);
        check("写事件入队", c.writeQueue.size() == 1);
        check("记录进 Raft log", c.raftLog.records.size() == 1);
        check("记录内容", c.raftLog.records.get(0).key.equals("broker-" + b));
    }

    /** B. standby 回放重建状态 (handleCommit L979-985) */
    static void testStandbyReplay() {
        MiniKafkaController c = new MiniKafkaController();
        c.isActive = false;
        c.registerBroker(0);
        c.handleCommit(0);
        check("standby 回放重建状态", c.state.containsKey("broker-1"));
    }

    /** B2. active 不重复回放 (L970-971 注释) */
    static void testActiveNoReplay() {
        MiniKafkaController c = new MiniKafkaController();
        c.registerBroker(0);
        c.handleCommit(0);
        check("active 不重建 (记录已 replay)", c.state.isEmpty());
    }

    /** C. 心跳超时 → fenced (BrokerHeartbeatManager L66-68) */
    static void testHeartbeatFenced() {
        MiniKafkaController c = new MiniKafkaController();
        int b = c.registerBroker(0);
        c.heartbeat(b, 100);
        c.checkLiveness(5000, 1000);   // 超时
        check("心跳超时 fenced", c.brokers.get(b).fenced);
        c.heartbeat(b, 6000);
        c.checkLiveness(7000, 1000);   // 恢复心跳
        check("心跳恢复", !c.brokers.get(b).fenced);
    }

    /** D. 选举①: preferred 优先 (PartitionChangeBuilder L70) */
    static void testElectionPreferred() {
        MiniKafkaController c = new MiniKafkaController();
        int b1 = c.registerBroker(0);
        int b2 = c.registerBroker(0);
        int leader = c.electLeader(List.of(b1, b2), List.of(b1, b2), true);
        check("preferred 当选", leader == b1);
    }

    /** D2. 选举②: preferred fenced → ISR 内选 (L74) */
    static void testElectionIsr() {
        MiniKafkaController c = new MiniKafkaController();
        int b1 = c.registerBroker(0);
        int b2 = c.registerBroker(0);
        c.brokers.get(b1).fenced = true;
        int leader = c.electLeader(List.of(b1, b2), List.of(b1, b2), false);
        check("ISR 内选", leader == b2);
    }

    /** D3. 选举③: ISR 全挂 → 出 ISR 保在线 (L78, unclean) */
    static void testElectionUnclean() {
        MiniKafkaController c = new MiniKafkaController();
        int b1 = c.registerBroker(0);
        int b2 = c.registerBroker(0);
        int b3 = c.registerBroker(0);
        c.brokers.get(b1).fenced = true;
        c.brokers.get(b2).fenced = true;
        int leader = c.electLeader(List.of(b1, b2, b3), List.of(b1, b2), true);
        check("出 ISR 选 (unclean)", leader == b3);
    }

    /** D4. ISR 全挂 + unclean 关闭 → 离线 */
    static void testElectionOffline() {
        MiniKafkaController c = new MiniKafkaController();
        int b1 = c.registerBroker(0);
        int b2 = c.registerBroker(0);
        c.brokers.get(b1).fenced = true;
        c.brokers.get(b2).fenced = true;
        int leader = c.electLeader(List.of(b1, b2), List.of(b1, b2), false);
        check("unclean 关闭 → 分区离线", leader == -1);
    }

    /** E. MetadataImage 快照 (MetadataImage.java:33-50) */
    static void testMetadataImage() {
        MiniKafkaController c = new MiniKafkaController();
        c.isActive = false;
        c.registerBroker(0);
        c.handleCommit(0);
        Map<String, String> image = c.buildImage();
        check("快照包含元数据", image.containsKey("broker-1") && image.get("broker-1").equals("registered"));
    }
}
