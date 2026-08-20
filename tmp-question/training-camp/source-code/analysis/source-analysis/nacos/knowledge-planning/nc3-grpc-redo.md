# NC-3 gRPC 通信 + Redo 重做 — 知识规划 (KP)

> 🔴 A | 模块: common/remote/client (4368) + client/redo + client/naming/remote/gprc/redo | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 连接内核 | RpcClient:78-86 | 状态机 + 双队列 (事件/重连) |
| 2 | 双消费者 | RpcClient:242-310 | 事件消费 + keepAlive 健康检查重连 |
| 3 | 列表变更 | RpcClient:216-231 | 当前服务器不在列表 → switchServerAsync |
| 4 | 监听钩子 | ConnectionEventListener | onConnected/onDisConnect |
| 5 | 四态机 | RedoData:97-113 | 三布尔 → NONE/REGISTER/UNREGISTER/REMOVE |
| 6 | 双 Map 仓库 | NamingGrpcRedoService:60-68 | instances + subscribes 隔离 |
| 7 | 断线标记 | NamingGrpcRedoService:100-113 | 全 setRegistered(false) |
| 8 | 生命周期 API | NamingGrpcRedoService:122-208 | 登记/确认/注销/移除 |
| 9 | 定时补发 | RedoScheduledTask:27-66 | connected 门控 + switch 分派 |
| 10 | 通用基类 | AbstractRedoService:17-45 | 泛型 Map + 模板任务 |

## 02 高频坑

1. gRPC 内核在 **common** 模块 (非 client-basic — 09 审计修正)
2. onDisConnect 才标记重做; onConnected 只置标志
3. RedoData 四组合: registered+unregistering → UNREGISTER (不是 REMOVE)
4. remove 只在 !expectedRegistered 时执行
5. 断线时 redo 任务跳过 (connected 门控)
6. redoDelayTime 可配 (REDO_DELAY_TIME)
7. 批量注册走 BatchInstanceRedoData

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 连接 | 状态机 (WAIT_INIT→RUNNING/UNHEALTHY) / 双队列 / switchServerAsync |
| 自愈 | onDisConnect 标记 / 定时补发 / healthCheck 闭环 |
| 状态机 | 三布尔四组合 / 语义表注释 / registered()/unregistered() |
| 隔离 | 双 Map / Class→Map 泛型基类 |
| 门控 | connected 检查 / isClientDisabled / keepAlive 超时 |
| 演进 | TODO refactor / 模块化线程名 / 配置键 |

## 04 跨域桥接

- ← NC-1: subscribe 三连的 redo 前置 (cacheSubscriberForRedo)
- ← NC-2: ConfigRpcTransportClient 依赖同内核
- → NC-6: 服务端 remote 面 (GrpcRequestAcceptor)
- → 面试: "Nacos 3.x 通信怎么保证可靠" — 长连接 + redo 四态 + 定时补发
