# 闭环笔记 Q4 — InternalSubchannel: 连接的"单地址"管理者

假设: Subchannel 是"一组等效地址"的逻辑连接; InternalSubchannel 管单地址连接生命周期 (建传输/失败退避重连/地址热更新)。

验证过程:
- **结构** (InternalSubchannel.java:72-79): TransportProvider + **ClientTransportFactory** (注入) + BackoffPolicy.Provider (L77)
- **startNewTransport** (L247-278): `syncContext.throwIfNotInThisSynchronizationContext` (L249, G-3 模型) → `addressIndex.getCurrentAddress()` (L256) → **代理解包** (L260-263, HttpConnectProxiedSocketAddress → G-3 代理面) → EAG authority 覆盖 (L265-267) → `new CallTracingTransport(...)` (L276)
- **失败与重连** (L305-322): 全部地址失败 → `gotoState(TRANSIENT_FAILURE)` (L309) → `reconnectPolicy.nextBackoffNanos()` (L315-319, **G-6 指数退避复用**) → 日志 "Will reconnect after {} ns" (L322)
- **状态上报** (L368): callback.onStateChange(Subchannel, 新状态) → LB 收状态 → updateBalancingState (q1)
- **热更新**: updateAddresses (L373) — 地址列表变更; resetConnectBackoff (L336); shutdown(Status reason) (L438)

代码类型: Implementation (连接状态机)

结论: InternalSubchannel = **单地址连接管理者** (PickFirstLeaf 每地址建一个, q3): 建传输 (代理/authority 注入) → 状态回调 LB → 失败退避重连 (复用 G-6) → 地址热更新。**被放弃的方案: Subchannel 直接持有多个连接** — 每地址一 Subchannel 让"连接粒度 = 地址粒度", LB 层面可精确控制 (G-4 上层策略决定用哪个)。 [跨域: G-3 syncContext/代理/退避全复用; G-4 PickFirstLeaf 每地址实例化] [网络: 代理链] (InternalSubchannel.java:247-322,368-373)
