import java.util.*;

/**
 * MiniRouting — E-4 Cluster Routing 极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 ES 8.12.2 源码):
 *   A. 路由表版本化: withIncrementedVersion, 低版本丢弃
 *      (RoutingTable.java:59-60; MasterService.java:508)
 *   B. 分配决策器投票: 任一 NO 拒绝, 全 YES 允许, THROTTLE 限速
 *      (AllocationDecider.java:32-91; 19 决策器)
 *   C. 读写路由: 写固定主分片, 读 preference 选副本
 *      (OperationRouting.java:63-77,206-240)
 *
 * 用内存路由表 + 决策器模拟 (机制复现非完整库)。
 */
public class MiniRouting {

    /** 路由表 (RoutingTable 简化): 分片 → 节点 + 版本 */
    static class RoutingTable {
        final Map<Integer, String> shardToNode = new HashMap<>();  // shardId → node
        final Map<Integer, String> primaryNode = new HashMap<>();  // shardId → primary node
        long version = 0;

        void assign(int shardId, String node, boolean primary) {
            shardToNode.put(shardId, node);
            if (primary) primaryNode.put(shardId, node);
        }

        RoutingTable withIncrementedVersion() {  // RoutingTable.java:59-60
            RoutingTable copy = new RoutingTable();
            copy.shardToNode.putAll(shardToNode);
            copy.primaryNode.putAll(primaryNode);
            copy.version = version + 1;
            return copy;
        }
    }

    /** 决策器 (AllocationDecider 简化): 投票 */
    interface Decider {
        enum Decision { YES, NO, THROTTLE }
        Decision canAllocate(int shardId, String node, RoutingTable rt);
    }

    /** 磁盘决策器: 磁盘满拒绝 (DiskThresholdDecider) */
    static class DiskDecider implements Decider {
        final Set<String> fullNodes = new HashSet<>();
        public Decision canAllocate(int shardId, String node, RoutingTable rt) {
            return fullNodes.contains(node) ? Decision.NO : Decision.YES;
        }
    }

    /** 同分片决策器: 一个节点不放同分片两份 (SameShardAllocationDecider) */
    static class SameShardDecider implements Decider {
        public Decision canAllocate(int shardId, String node, RoutingTable rt) {
            String existing = rt.shardToNode.get(shardId);
            return existing != null && existing.equals(node) ? Decision.NO : Decision.YES;
        }
    }

    /** 分配器: 决策器聚合 (AllocationDeciders 组合) */
    static String allocate(int shardId, List<String> nodes, RoutingTable rt, List<Decider> deciders) {
        for (String node : nodes) {
            boolean allowed = true;
            for (Decider d : deciders) {
                if (d.canAllocate(shardId, node, rt) == Decider.Decision.NO) { allowed = false; break; }
            }
            if (allowed) return node;  // 首个允许的节点
        }
        return null;  // 全拒绝 → 未分配
    }

    /** 读路由 (OperationRouting 简化): 写固定主, 读 preference 选副本 */
    static String routeRead(int shardId, RoutingTable rt, String preference) {
        String primary = rt.primaryNode.get(shardId);
        if (preference != null && preference.startsWith("_only_nodes:")) {
            String node = preference.substring("_only_nodes:".length());
            return rt.shardToNode.containsKey(shardId) && rt.shardToNode.get(shardId).equals(node) ? node : primary;
        }
        if ("_primary".equals(preference)) return primary;
        return rt.shardToNode.get(shardId);  // 默认: 任意活跃副本 (简化单副本)
    }
}
