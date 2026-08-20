# NC-5 一致性协议 Distro + SOFA-JRaft — 知识规划 (KP)

> 🟡 B | 模块: consistency (23) + core/distributed (60+) | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 统一契约 | ConsistencyProtocol | init/读写/onRequest 统一接口 |
| 2 | 双轨分叉 | APProtocol:28 / CPProtocol:28 | RequestProcessor4AP/4CP |
| 3 | Distro 门面 | DistroProtocol:44-54 | memberManager + 双 Holder |
| 4 | 启动加载 | DistroProtocol:84 | DistroLoadDataTask |
| 5 | 定时校验 | DistroProtocol:88 | DistroVerifyTimedTask |
| 6 | 接收分派 | DistroProtocol:164-173 | findDataProcessor.processData |
| 7 | JRaft 集成 | JRaftProtocol:93-122 | init → raftServer.start |
| 8 | leader 元数据 | JRaftProtocol:133-140 | RaftEvent → LEADER_META_DATA |
| 9 | 提交应用 | JRaftServer:317-334 | commit → applyOperation |
| 10 | 关闭对称 | JRaftServer:373-389 | node/group/cli 全关 |

## 02 高频坑

1. JRaft 是 SOFA-JRaft 集成非自研
2. Distro 组件按 resourceType 注册 (naming/config 独立)
3. 写走 Raft 提交, 读走状态机
4. NoLeaderException 暴露给上层降级
5. DistroVerifyTimedTask 定时校验与 onReceive 双向
6. FailoverClosureImpl 兜底闭包
7. 架构图在 JRaftProtocol 类注释 (L60-90)

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 契约 | ConsistencyProtocol / APProtocol / CPProtocol |
| AP | DistroProtocol / Holder / Load/Verify/Execute 任务链 |
| CP | JRaftProtocol / JRaftServer / NacosStateMachine / NacosClosure |
| 读写 | NacosRead/WriteRequestProcessor / AbstractProcessor |
| 配置 | RaftConfig / RaftSysConstants / DistroConfig |
| 异常 | NoLeaderException / FailoverClosureImpl / DuplicateRaftGroupException |

## 04 跨域桥接

- ← SofaJRaft 4.6: JRaft 内核 (集成面消费)
- ← ZK 4.3: ZAB 对照 (CP)
- → NC-6: naming/config 服务端消费方
- → 面试: "Nacos 的 AP 和 CP 怎么选" — Distro vs JRaft 双轨 + 组件注册面
