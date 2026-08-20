# S-9 集群限流域 — 大纲

## 上篇: 集群状态与组件 — 01-cluster-state.md

1. `ClusterStateManager` 模式管理(client/server/未启动)
2. 模式切换的互斥与启停
3. SPI provider 加载(client/server)
4. `TokenService` 接口与 `TokenResult`/`TokenResultStatus`

## 中篇: client 与 server 通信 — 02-cluster-transport.md

1. `DefaultClusterTokenClient` 请求封装
2. `NettyTransportClient` 通信模型
3. `DefaultEmbeddedTokenServer` / `NettyTransportServer`
4. 请求-响应异步关联(requestId + promise)

## 下篇: server 侧流控判定 — 03-cluster-checker.md

1. `DefaultTokenService` 三类请求分派
2. `ClusterFlowChecker` 全局阈值判定
3. 并发流控 `ConcurrentClusterFlowChecker`
4. envoy-rls 复用集群判定
