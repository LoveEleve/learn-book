import java.util.*;

/**
 * MiniRoutingTest — E-4 harness 验证入口
 *
 * 跑法: javac MiniRouting.java MiniRoutingTest.java && java MiniRoutingTest
 * 全部 PASS = 路由表版本化/决策器投票/读写路由理解到位
 * (对照 RoutingTable.java:59-60 / AllocationDecider.java:32-91 / OperationRouting.java:63-77)。
 */
public class MiniRoutingTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. 路由表版本化
    static void testVersioning() {
        MiniRouting.RoutingTable rt = new MiniRouting.RoutingTable();
        check("A1 初始 version=0", rt.version == 0);
        rt.assign(0, "node1", true);
        MiniRouting.RoutingTable v1 = rt.withIncrementedVersion();
        check("A2 递增后 version=1", v1.version == 1);
        check("A3 副本保留数据", v1.shardToNode.get(0).equals("node1"));
        check("A4 原表不变 (不可变)", rt.version == 0);
    }

    // B. 决策器投票
    static void testDeciders() {
        MiniRouting.RoutingTable rt = new MiniRouting.RoutingTable();
        rt.assign(0, "node1", true);
        MiniRouting.DiskDecider disk = new MiniRouting.DiskDecider();
        disk.fullNodes.add("node2");
        MiniRouting.SameShardDecider same = new MiniRouting.SameShardDecider();
        List<MiniRouting.Decider> deciders = List.of(disk, same);

        // 分片 0 的副本: node1 被 SameShard 拒 (同分片), node2 被 Disk 拒 → 未分配
        String r = MiniRouting.allocate(0, List.of("node1", "node2"), rt, deciders);
        check("B1 全拒 → 未分配", r == null);

        // 新分片 1: node1 允许
        r = MiniRouting.allocate(1, List.of("node1", "node2"), rt, deciders);
        check("B2 新分片放 node1", r.equals("node1"));

        // node2 磁盘恢复
        disk.fullNodes.clear();
        r = MiniRouting.allocate(0, List.of("node1", "node2"), rt, deciders);
        check("B3 磁盘恢复后放 node2 (SameShard 拒 node1)", r.equals("node2"));
    }

    // C. 读写路由
    static void testReadWriteRouting() {
        MiniRouting.RoutingTable rt = new MiniRouting.RoutingTable();
        rt.assign(0, "node1", true);   // 主在 node1
        rt.assign(0, "node2", false);  // 副本在 node2

        // 写固定主 (简化: primaryNode)
        check("C1 写路由 → 主分片 node1", rt.primaryNode.get(0).equals("node1"));

        // 读 preference
        check("C2 _primary → node1", MiniRouting.routeRead(0, rt, "_primary").equals("node1"));
        check("C3 _only_nodes:node2 → node2", MiniRouting.routeRead(0, rt, "_only_nodes:node2").equals("node2"));
        check("C4 默认 → 任意副本", MiniRouting.routeRead(0, rt, null).equals("node1") || MiniRouting.routeRead(0, rt, null).equals("node2"));
    }

    // C2. 路由表变化后旧版本丢弃 (节点按版本判新旧)
    static void testStaleVersion() {
        MiniRouting.RoutingTable v0 = new MiniRouting.RoutingTable();
        v0.assign(0, "node1", true);
        MiniRouting.RoutingTable v1 = v0.withIncrementedVersion();
        v1.assign(0, "node3", true);  // 分片迁移到 node3

        // 收到 v0 (旧) → 丢弃 (version 低)
        check("C5 旧版本被识别 (v0 < v1)", v0.version < v1.version);
        check("C6 应用 v1 后分片在 node3", v1.shardToNode.get(0).equals("node3"));
    }

    public static void main(String[] args) {
        testVersioning();
        testDeciders();
        testReadWriteRouting();
        testStaleVersion();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}
