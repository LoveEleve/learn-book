# Pass 2 闭环笔记 Q4: server 侧 token 服务与并发流控

## 验证过程

- `DefaultTokenService` 实现 `TokenService`，通过 `TokenServiceProvider`（`SpiLoader.loadFirstInstanceOrDefault`）解析 (`TokenServiceProvider.java:38-44`)。
- `requestToken` → `ClusterFlowChecker.acquireClusterToken(rule, acquireCount, prioritized)` (`DefaultTokenService.java:39-49`)。
- `requestParamToken` → 参数流控判定。
- `requestConcurrentToken` → `ConcurrentClusterFlowChecker.acquireConcurrentToken(clientAddress, rule, acquireCount)` (`DefaultTokenService.java:67-76`)。
- `releaseConcurrentToken(tokenId)` → `ConcurrentClusterFlowChecker.releaseConcurrentToken(tokenId)` (`DefaultTokenService.java:80-85`)。
- `DefaultEmbeddedTokenServer` 实现 `EmbeddedClusterTokenServer`，内部委托 `NettyTransportServer` 的 `start()`/`stop()` (`DefaultEmbeddedTokenServer.java:37-43`)。
- envoy-rls 的 `SimpleClusterFlowChecker` 复用了 core 的 `ClusterMetric`/`ClusterMetricStatistics` 做判定，`acquireClusterToken` 根据 `ClusterFlowEvent` 事件记录 (`SimpleClusterFlowChecker.java:31-50`)。

## 结论

server 侧把三类 token 请求分派给三个专用 checker：
- 普通流控 → `ClusterFlowChecker`
- 参数流控 → 参数 checker
- 并发流控 → `ConcurrentClusterFlowChecker`（带 tokenId 的占用/释放）

`DefaultEmbeddedTokenServer` 只是 Netty server 的薄封装，真正的流控判定在 `cluster/flow` 下的 checker 与 metric 统计层。