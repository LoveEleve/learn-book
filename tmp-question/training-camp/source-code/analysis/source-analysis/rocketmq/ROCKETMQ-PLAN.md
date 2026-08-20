# RocketMQ — 知识网络化规划 (R-1~R-16, 09 怀疑审计后 v2)

> **日期**: 2026-08-14 | **依据**: issue/源码分析执行计划.md 阶段4.1 (10 域) + **09 对既有规划保持怀疑 全量重审** (顶层模块扫描 + 数字穷举 + 依赖方向 + 拓扑重排)
> **源码**: `/data/workspace/source-code/code/spring/rocketmq` (**Apache RocketMQ 5.3.1**, pom.xml:31 实证; 19 Maven 模块)
> **定位**: 阶段 4.1 — 消息队列 (消息与事务阶段首仓库)
> **⚠️ 关键**: 既有规划基于 **4.x 时代认知**, 5.3.1 有重大架构差异 (proxy/controller/tieredstore 新面 + ScheduleMessageService 移入 broker) — 本文为唯一规划入口

---

## 〇、09 怀疑审计表 (RocketMQ, 2026-08-14) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| **域清单 10 个** | 顶层模块扫描 (19 模块) + 行数穷举 | **未覆盖 3 个定义特征级模块**: `proxy` (162 文件/20375 行 — 5.0 gRPC 协议层, grpc/ 目录实证 GrpcServer.java; proxy→broker 依赖)、`controller` (45 文件/6347 行 — 5.0 自动故障转移, namesrv→controller 依赖实证)、`tieredstore` (44 文件/7378 行 — 5.1 分层存储); 均过设计决策测试 (协议面/高可用面/存储面) | **修正: 10 → 13 域** |
| RM-1 Producer 发送 | client 模块扫描 | DefaultMQProducer 面存在 (client 150 文件/31234 行) | **接受** ✅ |
| RM-2 "hashSlotSize=4" | grep IndexFile.java | `hashSlotSize = 4` 是**每桶槽大小静态常量** (IndexFile.java:32); **实际配置 maxHashSlotNum 默认 500 万** (MessageStoreConfig.java:199) | **接受+精确化**: 4 是桶内槽常数非配置 |
| RM-3~RM-6 拉取/过滤/顺序/事务 | client+filter 模块 | 面存在 (filter 28 文件/6135 行; 事务消息 client TransactionMQProducer) | **接受** ✅ |
| RM-7 "18 级延迟队列" | grep ScheduleMessageService | **5.x 位置变化: store/schedule → broker/schedule**; delayLevelTable 为 ConcurrentSkipListMap — **5.x 支持自定义延迟等级** (默认 18 但非硬编码) | **修正+精确化**: 位置移入 broker + 等级可配置 |
| RM-8 Broker 启动 | broker 模块 | BrokerController 存在 (broker 126 文件/33961 行); DLedgerRoleChangeHandler 实证 (L797-798) | **接受** ✅ |
| RM-9 Namesrv 路由 | namesrv 模块 | **5.x 弱化**: namesrv 11 文件/3149 行 (最小模块); **namesrv→controller 依赖** (5.0 路由重心迁移); proxy 时代客户端可走 gRPC 代理 | **接受+修正**: 路由面 5.x 已迁移, 域需补 controller 协同 |
| RM-10 HA/DLedger | store DLedger | DLedgerRoleChangeHandler (broker) + store 面存在 | **接受** ✅ |
| 顺序 (Producer → CommitLog → ...) | 依赖方向 (pom 实证) | 拓扑: remoting→common (最底层); store→common+remoting; broker 枢纽 (→store/client/filter/acl/auth/tieredstore); namesrv→client+controller; controller 独立; proxy→broker | **部分重排** (见 §三) |
| 未声明顺序依据 | 执行计划全文 | 无拓扑理由说明 — 违反 09 铁律 (教学序/拓扑序混排须声明) | 记录 (本 PLAN 补) |
| RM-3 "消息拉取" (LitePull/PullMessageService/ProcessQueue) | client 子面量化 (find 行数) | **consumer 面 14212 行远超单域**: 漏 **DefaultMQPushConsumer** (使用率最高的消费方式, impl 1592 行) + **RebalanceImpl 853 行** (再平衡三实现) + **MQClientInstance 1391 行** (客户端骨架) + OffsetStore (Local/Remote 双实现); "PullMessageService" 实为 **Push 内部机制** (长轮询) 非 LitePull — 概念归错 | **修正: 消费面拆 2 域 (Push / Rebalance+offset+LitePull)** |
| ~~remoting 未列域~~ (执行计划无) | remoting 模块扫描 | **28 行文件/28217 行未成域**: RemotingCommand 协议 + NettyDecoder/Encoder (帧编解码) + RemotingClient/Server — 协议层为消息队列定义特征; store/broker/client/namesrv 全部依赖 (pom 实证) | **修正: 新增 RM-4 remoting 协议层** (拓扑最前) |
| ~~auth 未列域~~ (执行计划无) | auth+acl 模块扫描 | **auth 7232 行** (authentication/authorization/config/migration — 5.x 认证: SignAuthentication+ACL 双认证, AuthenticationEvaluator/chain) + acl 3028 行 | **修正: 新增 RM-12 安全面** (5.x 定义特征) |

**📋 v2 深度 REVIEW 记录 (2026-08-14, 用户追问"客户端呢?"触发)**: ①执行计划 RM-3 消费面概念错误 (PullMessageService 属 Push 非 LitePull) + 漏 PushConsumer/Rebalance/OffsetStore ②remoting 28K 行协议层无域 ③auth 7K 行 5.x 认证无域 → 13→16 域。
> **📋 v3 定级修正 (2026-08-14, 用户标准: 面试+业务接触面)**: 定义特征级 ≠ 必须展开 (09 反模式 6 只禁"没测就跳过", 04 方案决策允许降级)。安全面/Controller/TieredStore 面试低频+业务不接触 → 取消独立域 (安全并入 RM-13, Controller 并入 RM-12, TieredStore 🟡 C 按需简略); remoting 协议层 (面试常考) 与消费面拆分 (Push 是 90% 业务面) 保留。16 → **13 域**。
>
> **📋 v3.1 决策理由固化 (2026-08-14)**: 降级决策依据三标准 (排序): ①面试频度 × 业务接触面 ②**技术同源度** — 与既有域共享多少核心概念 ③**展开 ROI** — 一句话能答完 vs 需要机制展开。
> - Controller→RM-12: **技术同源** (与 DLedger 共享选举/主从切换/复制偏移/故障转移概念 — 它是 HA 的 5.x 形态, 非独立角色; 对照 Redis 哨兵独立成域是因监控角色独立)。**内容保留**: RM-12 含完整一节"5.0 自动故障转移: DLedger → Controller 演进", 面试答"5.0 变化"所需皆在, 仅省独立域管线成本 (Pass 0-3+闭环+深审+REVIEW)。
> - TieredStore→🟡 C: **存储骨架复用** (RM-2/3 已覆盖 CommitLog 读写路径, 分层存储是存储策略扩展非新机制; 类比 R-18 defrag 之于 R-33 zmalloc)。**展开 ROI 低**: 面试一句话 (冷热分离/历史数据低成本存储/按需加载) 足够。
> - 语义: **"并入"≠"砍内容"** (RM-12 一节 / RM-3 一句话), 仅省独立域管线; 若用户认为某面值得展开可随时升级。

**覆盖率报告**: 既有规划 10 域 → 重审后 13 域 (**130%**, +3 域: remoting 协议层/消费面拆 2/Proxy; 另 3 面 (安全/Controller/TieredStore) 经 v3 定级降级并入或简略 — 均先过设计决策测试再按面试+业务接触面降级, 非"没测就跳过")。

---

## 一、入口点与主线

`BrokerController (broker) → DefaultMessageStore (store) → CommitLog/ConsumeQueue` — 客户端: `DefaultMQProducer (client) → RemotingClient (remoting) → Broker` — 路由: `NamesrvController (namesrv) → RouteInfoManager` — 5.x 新面: `GrpcServer (proxy)` / `DLedgerController (controller)` / `TieredStoreService (tieredstore)`。

## 二、域清单 (13 域: 8🔴 + 5🟡)

> 拓扑序: remoting 最底 → store → broker 枢纽 → client 面 → 路由 → HA。定级标准: 面试频度 + 业务开发接触面 (v3)。

| # | 域 | 模块 (行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| RM-1 | **remoting 协议层** (新增, 面试常考) | remoting (28217) | RemotingCommand/NettyDecoder-Encoder/帧格式/RemotingClient-Server | 🔴 A |
| RM-2 | **存储底层** | store (37119) 部分 | MappedFile/FlushManager/内存映射 | 🔴 A |
| RM-3 | **CommitLog+ConsumeQueue** | store | 主链路/IndexFile (hashSlotSize=4 桶内常数, maxHashSlotNum 默认 500 万)/刷盘策略 | 🔴 A |
| RM-4 | **延迟消息** (位置修正) | broker/schedule | SCHEDULE_TOPIC_XXXX/延迟等级可配置 | 🟡 B |
| RM-5 | **Broker 启动** | broker (33961) | BrokerController 装配/模块注册/定时任务 | 🔴 A |
| RM-6 | **消息过滤** | filter (6135) | ExpressionType/SQL92/过滤链 | 🟡 B |
| RM-7 | **Producer 发送** | client/producer (4980) | DefaultMQProducer/MQFaultStrategy/SYNC/ASYNC/ONEWAY | 🔴 A |
| RM-8 | **消费-Push** (拆分新增) | client/consumer (14212 中主体) | MQClientInstance/PullMessageService (长轮询)/ProcessQueue/DefaultMQPushConsumerImpl | 🔴 A |
| RM-9 | **消费-Rebalance+offset** (拆分新增) | client | RebalanceImpl 三实现/AllocateMessageQueueStrategy/OffsetStore/LitePull | 🔴 A |
| RM-10 | **顺序+事务消息** | client+broker | MessageQueueSelector/TransactionMQProducer/回查 | 🟡 B |
| RM-11 | **Namesrv 路由** | namesrv (3149) | RouteInfoManager/BrokerData/QueueData (5.x 弱化标注) | 🟡 B |
| RM-12 | **HA/DLedger + Controller** (Controller 并入) | store+broker+controller (6347) | HAService/DLedgerCommitLog/主从同步/5.0 自动故障转移 | 🔴 A |
| RM-13 | **Proxy + 安全面** (安全并入) | proxy (20375)+auth 一句话 | 5.0 gRPC 协议层/双协议/认证接入 (SignAuthentication+ACL 一句话) | 🟡 B |

> **降级说明 (v3)**: TieredStore (5.1 分层存储) 面试低频+业务不接触 → 🟡 C 按需简略, 在 RM-3 存储域一句话带过; 不列独立域。
## 三、执行顺序 (拓扑: 叶子先, 09 重排)

**RM-1 → RM-2 → RM-3 → RM-4 → RM-5 → RM-6 → RM-7 → RM-8 → RM-9 → RM-10 → RM-11 → RM-12 → RM-13**

> 状态注记 (2026-08-14): **RM-1~RM-13 全部完成 — 阶段 4.1 收官 ✅** (状态/速查见 HANDOFF-ROCKETMQ.md §零/§一, V14)。

> 拓扑理由: remoting 协议层最底 (store/broker/client 全依赖, pom 实证) → 存储底层 (MappedFile) → CommitLog 主链路 → 延迟消息 → Broker 装配 (枢纽) → 过滤 → Producer (client 薄面先行) → 消费-Push (长轮询) → Rebalance+offset → 顺序+事务 → 路由 (namesrv) → HA/DLedger+Controller (5.0 自动故障转移并入) → Proxy+安全 (gRPC 新协议+认证接入)。
> **与执行计划差异**: ①+remoting 协议层 (面试常考) ②存储拆 2 域 ③**消费面拆 2 域 (Push / Rebalance+offset+LitePull)** — 修正"PullMessageService 属 LitePull"概念错误 ④+Proxy 域 (5.x 客户端接入面) ⑤Controller 并入 HA、安全并入 Proxy、TieredStore 🟡 C 按需简略 (面试低频) ⑥延迟消息域位置随模块迁移修正 ⑦Namesrv 路由标注 5.x 弱化。

## 四、知识网络图

```
← 复用: Redis R-9 复制 (主从/HA 对照) + R-15 Cluster (DLedger 多主对照) + Netty (阶段1, remoting 底层)
→ 引出: Kafka 12 域 (消息队列对照) + Seata (事务消息→分布式事务) + Redisson (发布订阅对照)
```

## 五、完成检查单

- [x] 顶层模块扫描 (19 模块) ↔ 域清单覆盖矩阵
- [x] 全部数字断言穷举 (hashSlotSize/maxHashSlotNum/延迟等级/模块行数)
- [x] 依赖方向 pom 实证 (6 模块依赖链)
- [x] 拓扑重排完成, 与规划差异逐条记录理由
- [x] 新增域全过设计决策测试
- [x] 偏差已同步 (本文为规划唯一入口, 待写 HANDOFF-ROCKETMQ)
- [x] v2 深度 REVIEW (用户"客户端呢?"触发): 消费面/remoting/安全面 3 缺口全发现
- [x] v3 定级修正 (用户标准: 面试+业务接触面): 安全/Controller/TieredStore 降级并入, 13 域定稿
