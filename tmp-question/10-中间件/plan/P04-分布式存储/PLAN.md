# P04：分布式存储 — 详细规划

> Phase: P04 | 周期: 2周 | 书: 1本 (MinerU)
> 依赖: P02（MQ 存储层） + P03（共识算法）
> 定位: 从中间件上升到分布式存储系统设计

---

## 一、为什么需要这一站

P02 学了 CommitLog/PartitionLog 的存储设计，但它们是 MQ 特化的存储——消息写入后会被消费删除。

P04 学的是**通用的分布式存储系统设计**——B+树/LSM Tree 存储引擎、数据分区与复制、CAP 理论在工程中的取舍、一致性协议的应用。这是从"会用中间件"到"会设计存储系统"的升级。

核心案例：OceanBase——阿里自研分布式数据库，与 RocketMQ 同为阿里中间件生态。

---

## 二、书单

### 《大规模分布式存储系统 原理解析与架构实战》（杨传辉）

| 维度 | 信息 |
|------|------|
| MinerU 路径 | `10-中间件-消息队列/大规模分布式存储系统.../大规模分布式存储系统：原理解析与架构实战.md` |
| MinerU 质量 | ⚠️ 4014行，OCR乱码偏多 |
| 定位 | 分布式存储系统从理论到 OceanBase 实践 |

| 篇 | 章 | 核心知识点 | 学习深度 |
|----|----|-----------|---------|
| **基础篇** | 第1章 概述 | 分布式存储概念与分类 | 概念层 |
| | 第2章 单机存储系统 | **存储引擎（哈希/B+/LSM树）、数据模型、事务与并发控制、故障恢复、数据压缩** | **深度精读** |
| | 第3章 分布式系统 | 性能分析、**数据分布（Hash/范围）**、复制（主从/多主/无主）、容错、**分布式协议（2PC/Paxos）**、跨机房部署 | **精读** |
| **范型篇** | 第4章 分布式文件系统 | GFS 架构、Taobao File System、Facebook Haystack、CDN | **精读** |
| | 第5章 分布式键值系统 | **Amazon Dynamo（去中心化、一致性哈希、向量时钟、Hinted Handoff）**、淘宝 Tair | **深度精读** |
| | 第6章 分布式表格系统 | **Google Bigtable 架构、Google Megastore、Windows Azure Storage** | **精读** |
| | 第7章 分布式数据库 | 数据库中间层（分库分表）、Microsoft SQL Azure、**Google Spanner（TrueTime API）** | **精读** |
| **实践篇** | 第8章 OceanBase架构初探 | 背景、**设计思路、系统架构（RootServer/MergeServer/ChunkServer/UpdateServer）** | **深度精读** |
| | 第9章 分布式存储引擎 | 公共模块、**RootServer/UpdateServer/ChunkServer 实现机制、消除更新瓶颈** | **深度精读** |
| | 第10章 数据库功能 | 整体结构、只读/写事务、OLAP 支持 | **精读** |
| **专题篇** | 第12章 云存储 | 云存储概念/技术/安全 | 了解 |
| | 第13章 大数据 | MapReduce、流式计算 | 了解 |

**精读章（8章）**：Ch2 存储引擎、Ch3 分布式协议、Ch5 Dynamo、Ch6 Bigtable、Ch7 Spanner、Ch8-10 OceanBase

---

## 三、源码阅读清单

本 Phase 以"阅读经典论文 + 架构分析"为主，OceanBase 不开源旧版（仅分析书中描述的架构）。

| 资料类型 | 内容 | 阅读目的 |
|---------|------|---------|
| 论文 | Google GFS (2003) | 分布式文件系统设计范本，Ch4 对照学习 |
| 论文 | Google Bigtable (2006) | 分布式表格系统原型，Ch6 对照学习 |
| 论文 | Amazon Dynamo (2007) | 去中心化 KV 设计，一致性哈希+向量时钟，Ch5 对照学习 |
| 论文 | Google Spanner (2012) | TrueTime + 全球分布式数据库，Ch7 对照学习 |
| 架构 | OceanBase 0.4 版本（书中描述的架构） | Ch8-10，学习阶段性架构演化决策 |

---

## 四、关键设计哲学

| 设计决策 | 来源 | 反事实假设 | 为什么选这个 |
|---------|------|-----------|-------------|
| **B+树为什么是数据库存储引擎标准？** | Ch2 | LSM Tree | 读多写少场景——B+树随机读效率 O(log N)，叶子链表支持范围扫描；LSM 适合写多读少（LevelDB/RocksDB） |
| **GFS 为什么一个文件多副本但只有一个 chunkserver 写？** | Ch4/GFS论文 | 多个 chunkserver 同时写 | 简化一致性模型——原子追加(atomic append)由主 chunkserver 串行化 |
| **Dynamo 为什么用向量时钟？** | Ch5/Dynamo论文 | 最后写入胜出(LWW) | 去中心化没有全局时钟——向量时钟可以检测并发冲突并协��解决（而非静默丢弃） |
| **OceanBase 为什么用 UpdateServer 单点写？** | Ch8 | 对等写入架构 | 业务场景 90% 读 10% 写——单点写简化事务、并发控制、一致性检查（代价是单点瓶颈） |
| **Spanner TrueTime 为什么用原子钟+GPS？** | Ch7/Spanner论文 | NTP 同步 | 全球部署下 NTP 误差太大（100ms级），TrueTime 将不确定性缩小到 ε≈7ms——这是外部一致性的硬件基础 |

---

## 五、生产陷阱

| # | 陷阱 | 场景 | 修复 |
|---|------|------|------|
| 1 | **一致性哈希数据倾斜** | 节点数少时，虚拟节点的 hash ring 上分布不均匀 → 某节点承载过多数据 | 增加虚拟节点数（如每物理节点 150 个虚拟节点） |
| 2 | **向量时钟无限增长** | Dynamo 风格系统中长期不合并 → 向量时钟条目膨胀 → 每次比对开销增大 | 定期裁剪（当某节点时钟差超过阈值，合并旧版本） |
| 3 | **GFS 单 Master 瓶颈** | Master 处理所有元数据操作（创建/删除/重命名）→ 操作量随文件数线性增长 | Google 通过大文件 + 客户端缓存元数据缓解；Colossus 后继系统改用分布式元数据 |
| 4 | **OceanBase UpdateServer 单点写入过载** | 写流量突增 → UpdateServer 内存用满 → 回刷 ChunkServer 跟不上 | 垂直扩容 UpdateServer + 业务分流（写量大的表迁出） |
| 5 | **分布式事务超时→数据不一致** | 2PC 的 Coordinator 超时后 Participant 不确定 commit/abort | 引入 3PC pre-commit 阶段（代价是延迟增加）或使用 TCC 补偿模式 |

---

## 六、面试 Q&A

| 问题 | 面试官意图 | 回答主线 |
|------|----------|---------|
| "B+树和 LSM Tree 的适用场景？" | 考察存储引擎选择能力 | B+树读优(O(log N))写需随机IO；LSM写优(顺序写)读需合并多文件；MySQL InnoDB选B+树，RocksDB选LSM |
| "一致性哈希如何解决节点增删的数据迁移问题？" | 考察分布式数据分布理解 | 只有相邻节点受影响——新增节点从后一个节点接管部分数据，删除节点把数据交给后一个——比简单取模少迁移 |
| "GFS 为什么选择原子追加(atomic append)而不是随机写？" | 考察是否理解论文设计 | 追加写是幂等的——失败重试安全；GFS 场景是日志/流式数据写入，不需要随机修改 |
| "Amazon Dynamo 是如何处理并发冲突的？" | 考察去中心化设计理解 | 向量时钟检测冲突 → 兄弟版本都返回给客户端 → 客户端提供冲突解决逻辑（如购物车合并）→ 读修复传播 |
| "OceanBase 为什么从单点 UpdateServer 演进到 Paxos 多副本？" | 考察架构演化思维 | 业务从 90%读→读写均衡；单点不可高可用；1.0 版本引入 Paxos 多副本解决 |

---

## 七、章节依赖

```
上游：
  P02 MQ存储：Kafka PartitionLog、RocketMQ CommitLog → Ch2 存储引擎的对比
  P03 共识算法：Raft/ZAB → Ch3 分布式协议（2PC/Paxos）的深入理解

下游：
  无直接下游（P04 是整个中间件梯队中存储理论的天花板）
  间接：
    P06 ShardingSphere：分库分表涉及的 CAP/BASE 理论 → 来自 Ch3
```

---

## 八、产出物（Phase C）

```
md/P04-分布式存储/
├── D-01-存储引擎B-plus与LSM树.md        # Ch2 核心——B+树 vs LSM Tree 的 10 维度对比
├── D-02-数据分布与复制策略.md            # Ch3——Hash/范围分区 + 主从/多主/无主复制 + 一致性模型
├── D-03-FGS论文精读.md                  # GFS 架构 + 原子追加 + 快照 + 容错
├── D-04-Bigtable论文精读.md             # Tablet/SSTable/MemTable + 列族 + 时间戳多版本
├── D-05-Dynamo论文精读.md               # 一致性哈希 + 向量时钟 + Hinted Handoff + 读修复 + Merkle树
├── D-06-OceanBase架构深度分析.md         # Ch8-10——RootServer/UpdateServer/ChunkServer/MergeServer 全链路
└── D-07-分布式存储面试Q&A.md             # 5道题 + 面试官意图
```

## 九、学习检查清单

- [ ] 能画出 B+树的结构并说明插入/查找/范围扫描的复杂度
- [ ] 能画出 LSM Tree 的写放大和读放大过程（MemTable → SSTable Level 0 → Level N → Compaction）
- [ ] 能写出 GFS Write（非原子）和 Record Append（原子）的区别
- [ ] 能解释 Dynamo 一致性哈希为什么需要虚拟节点
- [ ] 能对比 OceanBase 的 UpdateServer 模型和 Spanner 的 TrueTime + Paxos 模型的适用场景差异
