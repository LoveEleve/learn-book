# NC-1 NamingService 注册发现 — 时空溯源 (3.0.3 实证)

> 仓库为单提交 d14ae0ca (3.0.3) — 演进证据取自代码注释/@Deprecated/1.x↔3.x 对照 (规划文档 3.x 变化表 + 09 审计)

## 演化主线: 从 HTTP 短连接到 gRPC 长连接

| 时代 | 机制 | 证据 (3.0.3 源码) | 演进信号 |
|:--|:--|:--|:--|
| 1.x | HTTP 心跳 (BeatReactor) + ServerListManager 轮询 | **09 审计: 两文件均不存在于 3.0.3** | 完全替代 |
| 3.x | gRPC 长连接 + Redo 重做 | NamingGrpcClientProxy:90 / RpcClientFactory.createClient (L122) | 主线 |
| 兼容 | 双代理 HTTP 回退 | NamingClientProxyDelegate:197-203 isAbilitySupportedByServer | 老服务端兼容 |
| 增强 | 双代理 + 批量 + 模糊订阅 | batchRegisterService (L102) / NamingFuzzyWatch | 3.x 新能力 |

## 关键事件锚

- **NamingClientProxyDelegate 双代理** (L82-83): httpClientProxy + grpcClientProxy 并存 — 升级过渡期设计
- **@Deprecated logName** (NacosNamingService:88-89): 1.x 遗留字段
- **redo 前置** (NamingGrpcClientProxy:392-406): subscribe → cacheSubscriberForRedo → doSubscribe → subscriberRegistered — 断线重做是新架构核心
- **getServiceInfoBySubscribe** (NacosNamingService:366-376): tryToSubscribe 补订阅 — 订阅模式的自动修复
- **pushEmptyProtection** (ServiceInfoHolder:87-95): NAMING_PUSH_EMPTY_PROTECTION 开关 — 防空推送清空列表的加固
- **loadCacheAtStart** (ServiceInfoHolder:65-66): NAMING_LOAD_CACHE_AT_START — 启动加载磁盘缓存

## 架构递进逻辑

```
1.x: HTTP 短连接 + 心跳 (BeatReactor) + 轮询
3.x: gRPC 长连接 + 服务端推送 + Redo 重做 + 双代理兼容
```
→ 演进方向: 通信从"轮询"走向"推送", 可靠性从"客户端重试"走向"重做队列 (断线自动补发)", 兼容性靠双代理能力路由。
