# E-10 闭环笔记 Q5-Q8: 版本化/应用新旧/路由衔接/s88 承接

## Q5: Metadata 版本化 — 与 ClusterState version 什么关系?

假设: Metadata 独立 version 递增, 与 ClusterState version 解耦, 由 master 在 patch 时同步处理。

验证过程:
- Metadata.withIncrementedVersion (Metadata.java:326-353): 全字段复制, 只 version+1 (Metadata.java:330) — 不可变更新模式
- Metadata.version() (Metadata.java:694) — 持有者读取
- 调用者: MasterService.patchVersions (MasterService.java:508-512) — 仅当 routingTable/metadata 引用变化时才各自递增 (MasterService.java:507-511) — 引用比较 (previousState.routingTable() != newClusterState.routingTable())
- ClusterState.builder(state).incrementVersion() (MasterService.java:506,522-524) — ClusterState version 无条件递增
- 语义: 三层版本各自独立 (ClusterState/routingTable/metadata), 但同一次发布同步推进

代码类型: Implementation (不可变版本化)

结论: **Metadata 自带 withIncrementedVersion (Metadata.java:326-353, version+1 Metadata.java:330); master 发布时 patchVersions (MasterService.java:503-520) 对 routingTable/metadata 引用变化各自递增 (MasterService.java:507-511), ClusterState 版本无条件递增 (MasterService.java:506) — 三层版本解耦但同步推进**。Metadata.java:326-353 + MasterService.java:503-524

## Q6: 节点怎么应用新状态 — version 判新旧

假设: 接收方用 term+version 判新旧: 更旧拒绝, 更新的接受; diff 不兼容回退 full state。

验证过程:
- 接受侧拒绝旧: CoordinationState.handlePublishRequest (CoordinationState.java:370-402): term 必须匹配 (CoordinationState.java:372-381) + 同 term 下 version 必须更大 (CoordinationState.java:382-391) — 旧状态直接被拒
- 投票侧拒绝不匹配: handlePublishResponse (CoordinationState.java:413-437): version 必须等于 lastPublishedVersion (CoordinationState.java:428-437) — 乱序响应丢弃
- diff 应用: PublicationTransportHandler.handleIncomingPublishRequest (PublicationTransportHandler.java:126-209): 无本地状态 (PublicationTransportHandler.java:154-157) 或 diff.apply 抛 IncompatibleClusterStateVersionException (PublicationTransportHandler.java:186-188) → 回退请求 full state — diff 必须基于上一状态 (stateUUID 匹配)
- apply 侧串行: ClusterApplierService 单线程队列 (ClusterApplierService.java:336) — ClusterChangedEvent 带 previousState+newState (ClusterApplierService.java:473) — 应用器可对比前后 (E-5 updateShardState 消费)
- 主节点特例: 发布收尾时才 apply (Coordinator.java:406-408)

代码类型: Implementation (版本协商)

结论: **节点判新旧 = term+version 双门槛 (CoordinationState.java:382-391 拒绝旧, CoordinationState.java:428-437 丢弃乱序响应); diff 基于 stateUUID 匹配 (PublicationTransportHandler.java:178,186-188), 不兼容回退 full state (PublicationTransportHandler.java:154-157); apply 侧单线程串行 (ClusterApplierService.java:336,473,539)**。Coordinator.java:432-489

## Q7: 与 E-4 RoutingTable 衔接 — 路由表随集群状态发布

假设: RoutingTable 是 ClusterState 组件, master 计算后整体发布, 节点侧路由查询直接消费。

验证过程:
- ClusterState 持有 routingTable (ClusterState.java:166) + accessor (ClusterState.java:328-330)
- RoutingTable.withIncrementedVersion (RoutingTable.java:59, E-4 已交付) — 版本化已由 E-4 铺垫
- 发布时: patchVersions (MasterService.java:507-508) — routingTable 引用变化才递增版本
- 消费: E-5 updateShardState (IndexShard.java:493) / E-4 OperationRouting 读路由 — 节点 apply 后新 routingTable 立即生效
- 双向闭环: 集群状态驱动分片状态机 (E-5), 分片分配产生新 routingTable (E-4 reroute → 新 ClusterState 发布)

代码类型: 衔接分析

结论: **RoutingTable 是 ClusterState 组件 (ClusterState.java:166), 发布时与 metadata 同步版本化 (MasterService.java:507-508); 节点 apply 后新路由生效 — E-4 的 RoutingTable + E-5 的 updateShardState 都由集群状态驱动 (E-10 收束全链路)**。ClusterState.java:166 + RoutingTable.java:59

## Q8: 与 s88 承接 + diff 增量传输

假设: s88 明写 "连接/协议深入在阶段3 ES" — 节点间发布传输面在 E-10 展开: full vs diff 两种模式。

验证过程:
- s88-boot-elasticsearch 承接点: 客户端连接面 (s88 已讲) vs 服务端节点间连接面 (E-10 发布传输)
- PublicationTransportHandler (PublicationTransportHandler.java:70): newPublicationContext (PublicationTransportHandler.java:217-232) → buildDiffAndSerializeStates (PublicationTransportHandler.java:340-370): 新节点/禁持久化 → full state (PublicationTransportHandler.java:362), 已知节点 → diff (PublicationTransportHandler.java:364-367) — 按 TransportVersion 缓存序列化结果 (PublicationTransportHandler.java:362-367)
- 传输: sendPublishRequest (PublicationTransportHandler.java:372-478): 本地节点直连复用 (PublicationTransportHandler.java:381-394) / 远端 sendFullClusterState (PublicationTransportHandler.java:427) 或 diff (PublicationTransportHandler.java:420) — 失败回退 full (PublicationTransportHandler.java:478)
- 压缩: CompressorFactory (PublicationTransportHandler.java:127) — 传输前压缩
- 版本: TransportVersion 判兼容 (PublicationTransportHandler.java:362) — 混版本集群按最旧节点序列化
- 对照 r28-networking: Redis RESP 文本协议 vs ES 二进制版本化协议

代码类型: 衔接分析 (跨阶段承接)

结论: **s88 承接点落地: 服务端节点间发布传输 = full/diff 双模式 (PublicationTransportHandler.java:340-370), 新节点 full (PublicationTransportHandler.java:362), 已知节点 diff (PublicationTransportHandler.java:364-367), 失败回退 (PublicationTransportHandler.java:478), 压缩 (PublicationTransportHandler.java:127), 按 TransportVersion 兼容 (PublicationTransportHandler.java:362) — 与 r28 RESP 文本协议对照**。PublicationTransportHandler.java:126-209,340-370

跨域关联: s88-boot-elasticsearch (承接) / r28-networking (对照) / E-4 (路由)
