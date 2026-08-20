# NC-5 一致性协议 Distro + SOFA-JRaft — AP 与 CP 的双轨实现

> 前置: [[SofaJRaft-4.6]] (JRaft 内核已学) | 引出: [[NC-6-服务端核心]] (消费方) | 对照: ZK 4.3 (ZAB) / SofaJRaft 4.6 (Raft)
> 🟡 B | 方案 B (重要域) | 闭环: q1(统一契约) q2(Distro 组件面) q3(JRaft 集成面)

**读者处境**: Nacos 配置一致性走 CP (Raft), 服务注册走 AP (Distro) — 这个"双轨"在源码上怎么组织? Distro 的同步任务链? JRaft 是"自己实现"还是"集成框架"?

### 1. 统一契约 — ConsistencyProtocol 接口族

场景: AP/CP 双轨怎么抽象?
源码路径:
- **ConsistencyProtocol** (consistency/ConsistencyProtocol.java:40): 统一接口 — **init(config)** + addRequestProcessors + protocolMetaData + getData/write (读写面) + onRequest (服务端入口)
- **APProtocol** (consistency/ap/APProtocol.java:28): extends ConsistencyProtocol — 泛型 RequestProcessor4AP
- **CPProtocol** (consistency/cp/CPProtocol.java:28): extends ConsistencyProtocol — RequestProcessor4CP
- 双轨分叉: AP (最终一致) vs CP (强一致) — 请求处理器类型不同
关键设计 (q1): **"统一契约 = 双轨对外同构"** — 上层 (naming/config) 面向 ConsistencyProtocol 编程, 选 AP/CP 只在装配; 读写接口统一。 [模式: 协议统一接口]

### 2. Distro 组件面 — DistroProtocol 的 Holder 与任务链

场景: AP 协议的同步怎么组织?
源码路径:
- **DistroProtocol** (core/distributed/distro/DistroProtocol.java:44): memberManager + **DistroComponentHolder** (L48) + **DistroTaskEngineHolder** (L50)
- **启动加载** (L84): DistroLoadDataTask — 启动时从其他节点拉数据
- **定时校验** (L88): DistroVerifyTimedTask — 周期校验数据一致性
- **onReceive** (L164-173): **findDataProcessor(resourceType).processData** — 资源类型分派 (naming/config 各自 processor)
- **组件注册**: DistroDataProcessor/DistroDataStorage/DistroTransportAgent/DistroCallback 按 resourceType 注册在 Holder
关键设计 (q2): **"组件 Holder = 资源类型可插拔"** — naming/config 各注册自己的 processor/storage/agent; 协议框架只管分发与任务。 [模式: 组件注册表]

### 3. JRaft 集成面 — JRaftProtocol 不是自研是集成

场景: CP 协议是自研 Raft 吗?
源码路径:
- **JRaftProtocol** (core/distributed/raft/JRaftProtocol.java:93): extends AbstractConsistencyProtocol + implements CPProtocol — 内部 **new JRaftServer()** (L112)
- **init → raftServer.init + start** (L117-122) — RaftConfig → JRaftServer
- **leader 事件监听** (L133-140): RaftEvent → leader 元数据更新 (MapUtil.putIfValNoEmpty LEADER_META_DATA)
- **JRaftServer** (JRaftServer.java:105): **SOFA-JRaft 集成面** — createServer/start (L190) + **commit → applyOperation** (L317/334) + shutdown (L373-389: node/raftGroupService/cliService/cliClientService 全关)
- 类图注释 (L60-90): 完整架构图 (LogProcessor4CP → RaftConfig → JRaftProtocol.init → JRaftServer.start)
关键设计 (q3): **"集成 SOFA-JRaft = 复用成熟 Raft"** — JRaftServer 是框架封装 (node/group/cli 全托管), 不是自研; 对照 SofaJRaft 4.6 域的内核。 [模式: 框架集成]

### 4. 状态机与回执 — NacosStateMachine/NacosClosure

场景: Raft 提交后的应用与回调?
源码路径:
- **NacosStateMachine** (raft/NacosStateMachine.java:69): SOFA-JRaft StateMachine 实现 — 日志应用
- **NacosClosure** (raft/NacosClosure.java:30): Closure — 提交回调 (成功/异常)
- **FailoverClosureImpl** (raft/utils/FailoverClosureImpl.java:31): 失败兜底闭包
- JSnapshotOperation: 快照操作
- NoLeaderException: 无 Leader 异常 (读路径降级)
关键设计 (q4): **"状态机 + 闭包 = 提交语义完整"** — 应用日志 (StateMachine) 与回调 (Closure) 分离; 无 Leader 异常暴露给上层降级。 [模式: 提交语义]

### 5. 读写分派 — NacosRead/WriteRequestProcessor

场景: CP 读写的请求处理?
源码路径:
- **NacosWriteRequestProcessor** (raft/processor/NacosWriteRequestProcessor.java:30): 写请求 → commit → closure
- **NacosReadRequestProcessor**: 读请求 → 状态机直接读
- **AbstractProcessor** (processor/AbstractProcessor.java:35): 公共基类
关键设计 (q5): **"读写分离 = 写走 Raft 读走状态机"** — 强一致写必须提交, 读可本地 (默认线性读由 JRaft 保证)。 [模式: 读写分派]

### 6. 测试与行为锚

场景: 一致性协议的边界行为?
源码路径:
- 测试: DistroProtocolTest / JRaftProtocolTest (各模块 test)
- 架构图注释 (JRaftProtocol:60-90): 官方架构文档
- RaftSysConstants/RaftConfig: 配置面 (选举超时等)
关键设计 (q1): **"注释即架构图"** — 类注释内嵌完整调用链图, 官方文档。 [模式: 注释架构图]
