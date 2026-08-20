# NC-2 ConfigService 配置客户端 — 时空溯源 (3.0.3 实证)

> 仓库为单提交 d14ae0ca (3.0.3) — 演进证据取自代码注释/issue 号/架构对照

## 演化主线: HTTP 长轮询 → gRPC 唤醒式监听

| 时代 | 机制 | 证据 (3.0.3 源码) | 演进信号 |
|:--|:--|:--|:--|
| 1.x | HTTP 长轮询 (ClientLongPolling 服务端) | **09 审计: 客户端已无 HTTP 长轮询**; 执行计划 5.8 的 ClientLongPolling 不存在 | 客户端通道换 gRPC |
| 3.x | gRPC 唤醒式监听 | ConfigRpcTransportClient.getConnectionType = GRPC (ClientWorker:660) + listenExecutebell 信号量 (L644-646) | 主线 |
| 兜底 | 3 分钟全量同步 | ALL_SYNC_INTERNAL (L648-650) | 防推送丢失 |
| 兼容 | 三路容灾 (failover/server/snapshot) | NacosConfigService:206-261 | 保留并强化 |

## 关键事件锚

- **issue#7039** (NacosConfigService.java:110): getConfigAndSignListener 解密内容修复 — "get a decryptContent, fix #7039"
- **failover 语义注释** (L216-219): "A config content for failover is not created by client program automatically, but is maintained by user" — 用户维护的应急覆盖
- **3 分钟同步注释** (ClientWorker:648-649): "3 minutes to check all listen cache keys"
- **日志锚**: [notify-context] (CacheData:439) / [notify-ok] (L464) / [notify-error] (L478) — 通知链全可观测
- **shutdown 对称** (ClientWorker:666-701): RpcClientFactory uuid 前缀清理 + consistentWithServer=false 标记

## 架构递进逻辑

```
1.x: HTTP 长轮询 (客户端轮询拉取)
3.x: gRPC 长连接 + 信号量唤醒 + 3 分钟兜底同步
容灾: failover(用户) > server(主) > snapshot(自动)
```
→ 演进方向: 传输从"轮询"到"唤醒", 容灾三路保留 (应急场景靠 failover 文件强制覆盖), 通知从"新内容"到"变更明细" (AbstractConfigChangeListener + SPI 解析器)。
