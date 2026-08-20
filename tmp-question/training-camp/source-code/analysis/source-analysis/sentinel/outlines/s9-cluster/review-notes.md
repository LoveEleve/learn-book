# S-9 集群限流域 — 审查记录

## Pass 1 / Pass 2

- 确认集群判定内联在 `FlowRuleChecker.passClusterCheck` / `ParamFlowChecker.passClusterCheck`，无独立 `ClusterFlowSlot`。
- 确认 `ClusterStateManager` 用 volatile 管理 client/server/未启动三态，模式切换互斥并限制频率。
- 确认 client/server 通过 SPI provider(`TokenClientProvider`/`EmbeddedClusterTokenServerProvider`)惰性加载。
- 确认 `TokenResult`/`TokenResultStatus` 结果模型，checker 只认核心三态(OK/SHOULD_WAIT/BLOCKED)。
- 确认 client 侧 `DefaultClusterTokenClient` 封装逻辑，`NettyTransportClient` 传输，requestId 关联响应。
- 确认 server 侧 `DefaultTokenService` 分派三类请求，`ClusterFlowChecker` 全局阈值判定，`ConcurrentClusterFlowChecker` 并发占用/释放。
- 确认 envoy-rls 复用 core 的 `ClusterMetric` 判定，只是换成 gRPC 接入。

## 深审修正

1. 上篇锚点修正：`ClusterStateManager` 常量在 40-44，`setToClient` 在 82，`startClient` 在 92，`setToServer` 在 136，`startServer` 在 146，`TokenService` 接口在 26-62。
2. 中篇锚点修正：`NettyTransportClient` 类在 61，`Bootstrap` 在 90，promise 关联在 222-235。
3. 下篇锚点修正：`acquireClusterToken` 在 55，`allowProceed` 在 50，阈值判定在 68-81。

## 三篇正文

- `01-cluster-state.md`：模式管理、SPI 加载、TokenService 接口与结果模型
- `02-cluster-transport.md`：DefaultClusterTokenClient 封装、NettyTransportClient 传输、requestId 关联
- `03-cluster-checker.md`：DefaultTokenService 分派、ClusterFlowChecker 全局阈值、并发流控、envoy-rls

## 遗留

1. `NettyTransportClient` 的完整连接/重连逻辑未深挖（连接失败的重试次数、断线重连）。
2. `ConcurrentClusterFlowChecker` 的占用/释放计数细节未逐行展开。
3. envoy-rls 的 gRPC 协议编解码与 Envoy RLS 语义映射未深入。
4. `GlobalRequestLimiter` 的 namespace 级限流实现未单独展开。
