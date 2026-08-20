# NC-3 gRPC 通信 + Redo 重做 — 六层深审

> 深审标准: 缺陷档案 #1~#15

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 gRPC 内核在 "client/naming/remote/gprc + client-basic/remote + api/remote" | **09 审计: RpcClient/RpcClientFactory/GrpcClient/GrpcConnection/GrpcClientConfig 全在 common/remote/client** (4368 行); client-basic/remote 只有 HttpClientManager (100 行) — 路径大修正 |
| 2 | 规划 "NamingGrpcRedoService implements ConnectionEventListener (onConnected 清空重做队列)" | 实测: **onConnected 只置 connected=true; 清空标记在 onDisConnect (全部 setRegistered(false))** — 语义修正 (断线标记而非连接清空) |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "redo 队列断线后重建" | 实测: 队列常驻 (cacheInstanceForRedo 先登记), 断线只翻转 registered 标志, 重连后定时任务按 RedoType 补发 — 无重建 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "InstanceRedoData 在 client/redo" | 实测在 **naming/remote/gprc/redo/data/** (命名专属); client/redo/data 只有 RedoData 基类 |

## 审 4: 跨项目概念转移 — 0

## 审 5: 覆盖率 — 0 缺漏 (内核/状态机/四态/双 Map/定时/基类 7 面全覆盖)

## 审 6: 跨层一致性 — 0 (harness 18/18)

## 深审自抓缺陷 (harness)

| 缺陷 | 本质 |
|:--|:--|
| 第 5 步 removeInstance 断言需两段 (expected 不删 / 注销后删) | 源码 removeInstanceForRedo 条件 `!isExpectedRegistered()` — harness 两段实证 |

## 结论: 4 项修正, 全部落盘大纲/harness; 锚点重 grep 验证通过
