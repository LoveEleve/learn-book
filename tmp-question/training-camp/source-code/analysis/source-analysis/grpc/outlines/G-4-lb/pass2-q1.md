# 闭环笔记 Q1 — LoadBalancer API 三方协作: Helper/Picker/状态上报

假设: LoadBalancer 通过三个抽象与通道协作: Helper (请求资源), SubchannelPicker (每 RPC 选址), updateBalancingState (状态上报)。

验证过程:
- **Helper.createSubchannel** (LoadBalancer.java:1050-1060): "a logical connection to the given group of addresses which are considered equivalent" — **等效地址组 (EAG) 概念**: 一组地址视为同一个逻辑连接; LB 负责关闭 (L1057-1059); **"All methods on the LoadBalancer interface are called from a Synchronization Context"** (LoadBalancer.java:58-63) — G-3 syncContext 模型贯穿
- **SubchannelPicker.pickSubchannel** (L461-463): "Make a balancing decision for a new RPC" — **每 RPC 一次**; 返回 PickResult (连接或失败/缓冲)
- **updateBalancingState** (L1183): 抽象 — LB 通过它上报 (ConnectivityState + 新 Picker) 给通道
- Helper 还有: createOobChannel (L1072+, 带外通道, 如 xds 控制面 RPC)
- Factory (L1558): LoadBalancerProvider 的工厂面

代码类型: Interface (SPI 契约)

结论: 三方协作协议: LB 收到解析结果 (handleResolvedAddresses) → createSubchannel (从 syncContext, G-3 关联) → 连接状态变化 → **updateBalancingState(状态, 新 Picker)** → 通道存 Picker → 每 RPC 调 pickSubchannel 选连接。**被放弃的方案: LB 直接持有并返回连接** — Picker 解耦"状态变更"与"每 RPC 决策", 通道侧无锁读 Picker (G-3 AtomicReference 语义)。 [跨域: G-3 syncContext/ConfigSelector 同模式; G-7 xds LB 家族实现同一 SPI] [模式: 策略模式] (LoadBalancer.java:453-463,1036-1072,1183,1558)
