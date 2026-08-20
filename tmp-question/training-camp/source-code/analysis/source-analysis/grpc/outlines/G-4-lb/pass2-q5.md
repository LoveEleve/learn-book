# 闭环笔记 Q5 — RoundRobin 与 MultiChild 基类: 轮询 = 子 PickFirst + 轮换

假设: RoundRobin 不直接管连接 — 继承 MultiChildLoadBalancer (每地址组一个子策略, 默认 PickFirst), 自己只做"对 READY 子流轮询"。

验证过程:
- **MultiChildLoadBalancer 基类** (util/MultiChildLoadBalancer.java:55-80): childLbStates 列表 (L60) + `createChildLbState` (L105-106: **默认 PickFirstLoadBalancerProvider** 作子策略!) + createChildAddressesMap (L87, 按 key 拆分地址组) + 抽象 updateOverallBalancingState (L80)
- **RoundRobinLoadBalancer** (RoundRobinLoadBalancer.java:30-37): extends MultiChild; `AtomicInteger sequence = new AtomicInteger(new Random().nextInt())` (L36, 随机起点防同相) + currentPicker 缓存 (L37)
- **updateOverallBalancingState** (RoundRobinLoadBalancer.java:55-72): `getReadyChildren()` (L57) → 空且有人 CONNECTING/IDLE → **CONNECTING + NoResult** (L70-71); 空且全失败 → **TRANSIENT_FAILURE** (L73); 有 READY → **READY + createReadyPicker** (L75-76)
- **createReadyPicker** (L60+): 子 picker 列表 → 轮询 (sequence 自增取模) — 只对 READY 子流轮询
- **状态去重** (L64-67): `state != currentConnectivityState || !picker.equals(currentPicker)` 才上报 — 减少无谓状态通知

代码类型: Implementation (组合策略)

结论: gRPC 的轮询 = **组合设计**: MultiChild 基类管"子策略生命周期" (每个地址组一个子 LB, 默认 PickFirst), RoundRobin 只实现"选哪个 READY 子流" — 一行 sequence.incrementAndGet 的轮询逻辑; 状态上报去重 (picker.equals) 防抖动。**被放弃的方案: RoundRobin 自己实现连接管理** — 与 PickFirst 逻辑重复; 组合让任意"多子策略"复用同一骨架 (xds 的 ClusterManager 也继承 MultiChild, G-7)。 [跨域: G-7 xds 全部多子策略继承同一基类; G-4 PickFirst 为默认子策略] [模式: 组合复用] (RoundRobinLoadBalancer.java:30-67; MultiChildLoadBalancer.java:55-106)
