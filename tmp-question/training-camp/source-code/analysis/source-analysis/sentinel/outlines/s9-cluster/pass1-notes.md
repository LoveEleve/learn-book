# S-9 集群限流域 — Pass 1 轮廓记录

> 日期: 2026-08-17 | 范围: `sentinel-core/cluster/` + `sentinel-cluster/`(client-default + server-default) + envoy-rls

## 核心骨架

- 状态管理: `ClusterStateManager`(CLIENT=0 / SERVER=1 / NOT_STARTED=-1)
- 服务接口: `TokenService`(requestToken / requestParamToken / requestConcurrentToken / releaseConcurrentToken)
- 结果模型: `TokenResult` / `TokenResultStatus`(BAD_REQUEST=-4 ... RELEASE_OK=6 / ALREADY_RELEASE=7)
- 客户端接口/提供者: `ClusterTokenClient` / `TokenClientProvider`
- 服务端接口/提供者: `ClusterTokenServer` / `EmbeddedClusterTokenServer` / `EmbeddedClusterTokenServerProvider`
- client-default: `DefaultClusterTokenClient` / `NettyTransportClient` / `TokenClientHandler` / `DefaultClusterClientInitFunc` / codec 族
- server-default: `DefaultEmbeddedTokenServer` / `NettyTransportServer` / `SentinelDefaultTokenServer` / `TokenServiceProvider` / `ServerConstants`

## Pass 1 观察

- 集群判定不在独立 `ClusterFlowSlot`，而是内联在 `FlowRuleChecker.passClusterCheck` 与 `ParamFlowChecker.passClusterCheck`。
- `ClusterStateManager` 通过 `SentinelProperty<Integer>` 监听集群模式切换，模式切换会启停 client/server。
- `TokenResultStatus` 定义丰富的结果状态，OK=0 / BLOCKED=1 / SHOULD_WAIT=2 是核心三态。
- client 与 server 通过 Netty 通信，有独立的 codec 族。
- 集群 token 服务支持流控、参数流控、并发流控三类请求。

## 标记问题

1. `ClusterStateManager` 的模式切换如何触达 client/server 启停？
2. `TokenClientProvider` / `EmbeddedClusterTokenServerProvider` 如何按 SPI 加载？
3. `TokenResult` 的字段与状态如何被 checker 消费？
4. client-default 的 `DefaultClusterTokenClient` 如何请求 token？
5. `NettyTransportClient` 的通信模型？
6. server-default 的 `DefaultEmbeddedTokenServer` 如何监听与处理？
7. 集群并发流控 `requestConcurrentToken` 的语义？
8. 集群 token 服务的核心流控实现（server 侧 token bucket）？
9. envoy-rls 集成(RLS = RateLimitService)如何复用集群判定？
10. 集群模式下的回退策略(fallbackToLocalWhenFail)？
