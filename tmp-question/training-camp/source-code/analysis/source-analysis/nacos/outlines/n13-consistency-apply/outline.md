# N-13 一致性落点 — Distro 客户端数据处理与 JRaft 快照

> 前置: [[NC-5-一致性]] (协议面) + [[N-10-客户端管理]] (ClientSyncData) | 对照: 协议与数据的桥接面
> 🟡 B | 方案 B (重要域) | 闭环: q1(Distro 数据面) q2(快照面)

**读者处境**: 一致性协议的数据处理怎么挂到 naming? 集群同步的数据格式?

### 1. Distro 客户端数据面 — v2 四件

场景: 临时实例的 Distro 同步数据怎么处理?
源码路径:
- **DistroClientDataProcessor** (ephemeral/distro/v2/DistroClientDataProcessor.java:58): 数据处理器 (NC-5 onReceive 的 resourceType 实现) — 客户端数据同步
- **DistroClientComponentRegistry** (v2/DistroClientComponentRegistry.java:37): 组件注册 (processor/storage/agent)
- **DistroClientTransportAgent** (v2/DistroClientTransportAgent.java:49): 传输代理
- **DistroClientTaskFailedHandler** (v2/DistroClientTaskFailedHandler.java:31): 失败处理
- **DistroClientVerifyInfo** (v2/DistroClientVerifyInfo.java:26): 校验信息 (verify 任务)
关键设计 (q1): **"组件五件套 = NC-5 注册面的落地"** — processor/storage/agent/失败/校验按资源类型注册; 数据载体 ClientSyncData (N-10)。 [模式: 组件落地]

### 2. 持久快照面 — persistent/impl

场景: 持久实例的 JRaft 快照?
源码路径:
- **AbstractSnapshotOperation** (persistent/impl/AbstractSnapshotOperation.java:35): 快照操作基类
- **BatchReadResponse/BatchWriteRequest** (persistent/impl/): 批量读写协议
- **OldDataOperation** (persistent/impl/OldDataOperation.java:24): 旧数据兼容
- 与 NC-5 JSnapshotOperation 桥接
关键设计 (q2): **"快照基类 = 批量读写协议"** — JRaft 快照落盘走批量请求。 [模式: 快照批量]

### 3. 操作代理 — ClientOperationServiceProxy

场景: 临时/持久操作怎么分派?
源码路径:
- **ClientOperationService** (v2/service/ClientOperationService.java:36) + **ClientOperationServiceProxy** (v2/service/ClientOperationServiceProxy.java:42): 代理
- **EphemeralClientOperationServiceImpl** / **PersistentClientOperationServiceImpl**: 双实现 (N-10 已见)
关键设计 (q3): **"代理分派 = 临时/持久路由"** — 与一致性双落点对应。 [模式: 代理路由]

### 4. 测试与行为锚

场景: 落点边界?
源码路径:
- 测试: DistroClientDataProcessorTest (naming test)
- 锚: Datum/KeyBuilder (consistency/Datum.java:29 + KeyBuilder.java:27)
关键设计 (q1): **"键构造 = 数据寻址"** — KeyBuilder 定义一致性命名的键。 [模式: 键契约]
