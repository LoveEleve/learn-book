# S-5 节点与统计域 — 全视角提问

1. 为什么同一 resource 要同时维护 `DefaultNode` 和 `ClusterNode` 两套节点?
2. `EntranceNode` 为什么不直接复用 `DefaultNode` 的统计实现，而要重写聚合方法?
3. `NodeSelectorSlot` 为何按 context 名分叉，而不是按 resource 名?
4. `ClusterBuilderSlot` 为何必须是原型槽? 单例会怎么串线?
5. `StatisticSlot` 的 pass / block / rt / exception / threadNum 分别在 entry 侧还是 exit 侧写入?
6. origin 维度统计挂在哪层? 为什么不挂在 `DefaultNode` 上?
7. `DefaultNode` 到 `ClusterNode` 的双写是显式写两次，还是通过覆写透传?
8. `ENTRY_NODE` 与 `EntranceNode` 有什么区别? 一个是“全局入站总量”，一个是“某个 context 入口树”吗?
9. `StatisticNode` 为什么维护 second/minute 两套 rolling counter?
10. 滑动窗口推进是谁负责的: `StatisticNode` 还是 `LeapArray`?
11. `PriorityWaitException` 为什么只加 thread 不加 pass request?
12. callback registry 的时序为什么放在统计写入之后?
13. 热点参数统计为什么能不改 `StatisticSlot` 主逻辑就织入?
14. `OccupiableBucketLeapArray` 为什么要额外套一层 `FutureBucketLeapArray`?
15. `FutureBucketLeapArray.isWindowDeprecated` 为什么是“只算未来”语义?
16. `NodeBuilder` 为什么被废弃? 真实构建路径什么时候内联进槽?
17. `eagleeye` 为什么与节点树零耦合? 它到底输出什么而不是统计什么?
18. `ClusterNode.originCountMap` 为什么也用 COW + lock 而不是 ConcurrentHashMap?
19. `EntranceNode.avgRt()` 为什么要按 passQps 加权?
20. `StatisticSlot.exit` 为什么 callback 在 `fireExit` 之前?
