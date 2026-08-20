# GFS、ZooKeeper、Bigtable、Dynamo 与 Spanner — 分布式理论如何落到真实系统

> Cluster D: 15 KPs | 依赖: 01-11 分布式理论全链路 | 读者基线: CAP、共识、故障/时间模型、事务与复制
> 读者处境: 前 11 篇讲的是抽象模型；本篇把模型放回五个经典系统，观察它们如何在一致性、可用性、存储、时间和运维之间做取舍
> 打开新视角: 经典系统不是理论的“标准答案”，而是**针对工作负载和故障模型做出的具体工程折中**

---

### 概念依赖链

```
01-11 CAP/共识/时间/事务 → 本篇: GFS/ZK/Bigtable/Dynamo/Spanner
  ├─ §1 GFS(大文件/租约/追加)
  ├─ §2 ZooKeeper(ZAB/会话/watch/协调)
  ├─ §3 Bigtable(有序行键/Tablet/LSM)
  ├─ §4 Dynamo/Cassandra(AP/向量时钟/修复)
  └─ §5 Spanner(Paxos/2PC/TrueTime)
先讲: 文件 → 协调 → 结构化大数据 → 高可用 KV → 全球 SQL
后续依赖: 分布式架构与微服务(Spring Cloud/K8s/Istio)
```

### 叙事顺序

1. 问题引入——同样是“分布式存储”，为什么 GFS、ZooKeeper、Bigtable、Dynamo 和 Spanner 会选择完全不同的模型？
2. GFS——大文件、Chunk、Master 和追加语义
3. ZooKeeper——小数据协调、ZAB、Session 与 Watch
4. Bigtable——行键、Tablet 与 LSM
5. Dynamo/Cassandra——高可用与最终一致
6. Spanner——全球 SQL、Paxos 与 TrueTime
7. 收束——理论到产品的映射

### 1. GFS — 大文件工作负载如何改变存储设计

场景提示: 需要存储 PB 级日志文件，文件少但巨大、读写以追加为主，为什么不直接用传统小块文件系统？ [写作时展开]

关键设计: GFS 用 Master 管理元数据和 chunk 位置，ChunkServer 承载大块数据，客户端拿到位置后直接访问数据节点：

```[pseudocode]
client read/write
  → ask Master for chunk/replica location
  → client directly reads/writes ChunkServer

写入:
  Master lease/primary ordering
  → primary determines mutation order
  → forwards to secondary replicas
  → acknowledgements

适配:
  large chunks / append-heavy workload
  → 降低元数据/寻道成本
  → 牺牲通用小文件/强细粒度更新语义
```

Why: 为什么 GFS 的追加写并不等同于普通文件系统的精确 append？——**复制、并发追加、padding、重复记录和异常恢复会影响最终文件内容布局**；GFS 针对 Google 的大文件和追加场景优化，不能把它的 chunk 大小、租约时间和一致性语义当作所有分布式存储通用答案。 [分布式存储: Master 是元数据瓶颈/故障恢复对象，数据路径与控制路径分离]

比喻锚点: GFS 像超大档案仓库：总目录只负责告诉你哪座仓库存哪一箱，真正搬运文件不经过总目录办公室。 [写作时展开]

### 2. ZooKeeper — 小数据协调服务如何保持顺序和会话语义

场景提示: 50 个服务要共享配置、选主和临时锁，为什么不用数据库表直接轮询？ [写作时展开]

关键设计: ZooKeeper 用类文件系统 znode 树、ZAB 全序广播、会话和 watch 提供协调原语：

```[pseudocode]
znode:
  path + small data + version/stat

client session:
  heartbeat/timeout
  → session expire
  → ephemeral nodes disappear

write:
  client request → ZAB ordered transaction
  → DataTree apply
  → relevant watch notification

watch:
  one-shot notification
  → client receives event
  → client must re-register / re-read state
```

Why: 为什么 Watch 不能当作“可靠变更日志”？——**Watch 是一次性通知/提示，可能合并、断连重连或需要重新读取当前状态**；真正正确的做法是收到通知后重新读取 znode，并设计会话失效、重复通知和丢失窗口。ZooKeeper 适合小元数据与协调，不适合大数据文件存储。 [分布式理论: ZAB 全序和会话/临时节点共同组成协调语义]

比喻锚点: ZooKeeper 像分布式值班台：保存小型公告和临时值班牌，并在变化时提醒订阅者，但提醒后仍要回公告栏确认当前内容。 [写作时展开]

### 3. Bigtable — 有序行键、Tablet 与 LSM 如何支撑大数据

场景提示: 需要按 URL 前缀扫描十亿条记录，关系型单机 B+Tree 为什么不够？ [写作时展开]

关键设计: Bigtable 把数据建模为按 row key 排序的稀疏多维 Map，再按范围切成 Tablet：

```[pseudocode]
cell:
  (row key, column family:qualifier, timestamp) → value

存储:
  write buffer/memtable
  → immutable SSTable
  → compaction

扩展:
  row key range
  → Tablet
  → Tablet 迁移/分裂/负载均衡

组件:
  GFS 存 SSTable
  Chubby 提供锁/协调
  Master 管 Tablet 元数据
```

Why: 为什么 Bigtable 强调 row key 设计？——**排序范围决定局部性、Tablet 分布和热点**：连续热门前缀可能把负载集中到少数 Tablet；随机化 key 又会削弱范围扫描。Bigtable 以单行原子等语义为核心，跨行事务和复杂 join 需要上层系统补充。 [分布式存储: Tablet 分裂和 LSM Compaction 同时影响扩展、写放大与查询延迟]

比喻锚点: Bigtable 像按字典序排列的巨大档案柜，再把连续字母区间拆给不同管理员；区间设计决定查找和管理员是否过载。 [写作时展开]

### 4. Dynamo/Cassandra — 在可写性与一致性之间做显式选择

场景提示: 购物车业务宁可暂时读到旧版本，也不能因为部分节点不可达而拒绝写入，系统如何设计？ [写作时展开]

关键设计: Dynamo 类系统用分区/复制、sloppy quorum、hinted handoff 和版本冲突处理维持高可用：

```[pseudocode]
write:
  hash(key) → replica set
  节点不可达?
  → 临时节点接管(hinted handoff)

冲突:
  vector/version metadata 检测并发版本
  → read repair / 客户端合并 / 应用策略

后台:
  anti-entropy/Merkle comparison
  → 发现并修复副本差异

Cassandra-style tunable consistency:
  R/W 与 replication factor 组合
  → 具体保证取决于拓扑、故障和一致性级别
```

Why: 为什么“W+R>RF”不能脱离模型直接等于强一致？——**读写 quorum 的交集、故障节点、时间窗口、冲突版本和实现拓扑都影响实际语义**；最终一致也需要修复、冲突合并和版本裁剪，否则“永远可写”会变成永久多版本。 [分布式理论: AP 选择把不可用/冲突成本转移到读修复、反熵和业务合并]

比喻锚点: Dynamo 像多个分店都允许收订单，暂时不同账没关系，但必须有回访、对账和冲突处理，最终不能只靠“以后会好”。 [写作时展开]

### 5. Spanner — 全球分布式 SQL 如何组合共识、事务与时间

场景提示: 跨地域 SQL 事务既要强一致，又要在多个数据中心可用，Spanner 怎样组合多种技术？ [写作时展开]

关键设计: Spanner 把数据分片到 Paxos group，用分布式事务协调跨分片提交，用 TrueTime 提供外部一致排序辅助：

```[pseudocode]
单分片:
  Paxos group
  → 复制/选主/强一致提交

跨分片事务:
  coordinator
  → 多个 Paxos group 参与
  → 2PC 协调原子提交

时间:
  TrueTime.now() = [earliest, latest]
  → 分配 commit timestamp
  → commit-wait 等不确定性窗口过去
  → 再对外可见
```

Why: 为什么 Spanner 没有“补齐 CAP 三角、打破 CAP”？——**分区发生时仍需在一致性和可用性之间做选择，TrueTime 也不能消除网络分区**；它通过工程硬件、复制和时间区间，在正常/可假设条件下提供强一致与全球部署能力，代价是等待、硬件/运维和跨地域延迟。TrueTime 的外部一致性也不是简单“物理时间戳排序”。 [分布式理论: Spanner 是 CAP 约束下的工程组合，不是 CAP 反例]

比喻锚点: Spanner 像全球银行：每个区域有本地账房和多数派复核，跨区域交易还要统一协调与等待时间不确定性窗口。 [写作时展开]

### 6. 收束

五个系统的设计选择：

```[pseudocode]
GFS:
  大文件/追加/Chunk + lease

ZooKeeper:
  小数据协调/ZAB/会话/watch

Bigtable:
  有序行键/Tablet/LSM

Dynamo:
  高可用写入/最终一致/冲突修复

Spanner:
  分片Paxos + 2PC + TrueTime
```

**Aha Moment**: "经典系统不是 CAP/Paxos/TrueTime 的产品化答案，而是**把理论约束、工作负载、硬件、故障模型和运维能力组合成具体折中**：GFS 优化大文件，ZK 优化协调，Bigtable 优化有序大数据，Dynamo 优化可写性，Spanner 优化全球事务语义。"
**回答读者三问**: ①GFS 为什么数据不经过 Master=控制路径与数据路径分离；②Dynamo 为什么能持续写=接受最终一致与冲突修复成本；③Spanner 是否打破 CAP=没有，仍在分区时做一致性/可用性取舍。

---

### 核心悬念

**"分布式理论 12 篇已经把模型和经典系统串完；下一步如何把这些原则落到 Spring Cloud、Dubbo、K8s 和 Istio 的微服务架构与工程实践？"**

→ 引出分布式专题域 2：架构与微服务。