import java.util.List;

public class MiniKafkaGroupTest {

    static int failures = 0;

    static void check(String name, boolean cond) {
        if (cond) System.out.println("PASS " + name);
        else { System.out.println("FAIL " + name); failures++; }
    }

    public static void main(String[] args) {
        testKip848Heartbeat();
        testKip848LeaveByEpoch();
        testIncrementalSubscription();
        testClassicFourSteps();
        testClassicLeave();
        testUniformAssignment();
        testOffsetCommit();
        testOffsetExpiration();
        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** A/B. KIP-848 heartbeat 即状态上报 (GroupMetadataManager L4691-4724) */
    static void testKip848Heartbeat() {
        MiniKafkaGroup g = new MiniKafkaGroup(4);
        g.heartbeat("g1", "m1", List.of("t1", "t2"), 1);
        g.heartbeat("g1", "m2", List.of("t1"), 1);
        check("heartbeat 创建成员", g.group("g1", true).members.size() == 2);
        check("订阅上报", g.group("g1", true).members.get("m1").subscribedTopics.size() == 2);
        check("epoch 记录", g.group("g1", true).members.get("m2").epoch == 1);
    }

    /** E. epoch=-1 离组 (GroupMetadataManager L4697-4701) */
    static void testKip848LeaveByEpoch() {
        MiniKafkaGroup g = new MiniKafkaGroup(4);
        g.heartbeat("g1", "m1", List.of("t1"), 1);
        boolean left = g.heartbeat("g1", "m1", List.of(), -1);
        check("epoch=-1 离组", left && g.group("g1", true).members.isEmpty());
    }

    /** B2. 增量: 二次 heartbeat 更新订阅 (L4715-4720) */
    static void testIncrementalSubscription() {
        MiniKafkaGroup g = new MiniKafkaGroup(4);
        g.heartbeat("g1", "m1", List.of("t1"), 1);
        g.heartbeat("g1", "m1", List.of("t1", "t3"), 2);
        check("增量更新订阅", g.group("g1", true).members.get("m1").subscribedTopics.size() == 2);
        check("epoch 推进", g.group("g1", true).members.get("m1").epoch == 2);
    }

    /** C. Classic 四步 (GroupCoordinatorShard L549-970) */
    static void testClassicFourSteps() {
        MiniKafkaGroup g = new MiniKafkaGroup(4);
        check("join", g.classicJoin("gc", "m1"));
        check("sync 分配", g.classicSync("gc", "m1") && g.group("gc", false).members.get("m1").assignment.size() >= 1);
        check("heartbeat", g.classicHeartbeat("gc", "m1"));
        check("leave", g.classicLeave("gc", "m1"));
        check("leave 后心跳失败", !g.classicHeartbeat("gc", "m1"));
    }

    /** C2. Classic leave (Shard L970) */
    static void testClassicLeave() {
        MiniKafkaGroup g = new MiniKafkaGroup(4);
        g.classicJoin("gc", "m1");
        g.classicJoin("gc", "m2");
        g.classicLeave("gc", "m1");
        check("成员移除", g.group("gc", false).members.size() == 1);
    }

    /** C3. Uniform 分配 (GroupCoordinatorConfig L187-193 默认) */
    static void testUniformAssignment() {
        MiniKafkaGroup g = new MiniKafkaGroup(8);
        g.heartbeat("g1", "m1", List.of("t"), 1);
        g.heartbeat("g1", "m2", List.of("t"), 1);
        check("每成员分满", g.group("g1", true).members.get("m1").assignment.size() == 4);
        check("分配不重叠", !g.group("g1", true).members.get("m1").assignment.equals(g.group("g1", true).members.get("m2").assignment));
    }

    /** D. offset commit (OffsetMetadataManager L600) */
    static void testOffsetCommit() {
        MiniKafkaGroup g = new MiniKafkaGroup(4);
        g.commitOffset("g1-t-0", 42, 1000);
        check("commit 保存", g.offsets.get("g1-t-0").offset == 42);
    }

    /** D2. 过期 (expireTimestampMs L583: now+retention) */
    static void testOffsetExpiration() {
        MiniKafkaGroup g = new MiniKafkaGroup(4);
        g.commitOffset("g1-t-0", 42, 1000);
        check("未过期", !g.isExpired("g1-t-0", 1500, 1000));
        check("已过期", g.isExpired("g1-t-0", 2500, 1000));
        check("无记录视为过期", g.isExpired("g1-t-9", 1000, 1000));
    }
}
