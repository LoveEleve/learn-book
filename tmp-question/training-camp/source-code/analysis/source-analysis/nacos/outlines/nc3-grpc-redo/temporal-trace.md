# NC-3 gRPC 通信 + Redo 重做 — 时空溯源 (3.0.3 实证)

> 仓库为单提交 d14ae0ca (3.0.3) — 演进证据取自代码注释/TODO/架构对照

## 演化主线: 短连接时代 → 长连接 + 自愈时代

| 时代 | 机制 | 证据 (3.0.3 源码) | 演进信号 |
|:--|:--|:--|:--|
| 1.x | HTTP 短连接 + 客户端重试 | 执行计划 5.8 描述 (BeatReactor) — 09 审计确认不存在 | 被替代 |
| 3.x | gRPC 长连接 | RpcClient (common/remote/client, 1046 行) | 主线 |
| 3.x | Redo 重做 | NamingGrpcRedoService + RedoData 四态机 | 断线自愈 |
| 演进中 | 通用基类 | AbstractRedoService (client/redo/service) + **TODO refactor 注释** (NamingGrpcRedoService:48) | config 面复用铺路 |

## 关键事件锚

- **TODO refactor 注释** (NamingGrpcRedoService.java:48): "TODO refactor to extends from AbstractRedoService" — 命名服务先落地, 基类已泛型化 (RedoData<?>)
- **REDO_THREAD_NAME_PATTERN** (AbstractRedoService:17): `com.alibaba.nacos.client.%s.redo` — 模块化线程名设计
- **四态语义表注释** (RedoData:100-110): 三布尔四组合行为固化 — 状态机规格
- **onDisConnect 日志** (NamingGrpcRedoService:102): "Grpc connection disconnect, mark to redo"
- **健康检查闭环** (RpcClient:270-290): keepAlive 超时 → healthCheck → UNHEALTHY → 重连

## 架构递进逻辑

```
1.x: 每次操作 HTTP 请求 + 客户端失败重试 (无状态)
3.x: gRPC 长连接 + 操作先登记 (redo 队列) + 断线标记 + 定时补发 (有状态自愈)
演进: AbstractRedoService 泛型基类 → config 模块复用
```
→ 演进方向: 从"请求-响应"到"登记-确认-补发"的可靠通信模型; 连接管理从简单重试到状态机 (WAIT_INIT→INITIALIZED→STARTING→RUNNING/UNHEALTHY) + 双队列 (事件/重连)。
