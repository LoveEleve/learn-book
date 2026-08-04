# 中间件学习总体规划

> 创建时间：2026-07-27
> 状态：Phase B（规划阶段）
> 对应 MinerU 目录：`10-中间件-消息队列/`

---

## 一、学习架构

采用 **分层递进模型**：网络基石 → 消息队列 → 分布式协调 → 存储与搜索 → RPC与分库

```
Phase 1: 网络通信基础          (1书, 1-2周)  Netty — 所有 Java 网络中间件的基石
Phase 2: 消息队列核心          (3书, 3-4周)  Kafka + RocketMQ 双轨对比
Phase 3: 分布式协调与共识      (1书+待补, 3周)  Etcd源码 + ZooKeeper源码
Phase 4: 分布式存储            (1书, 2周)  OceanBase案例 + 存储引擎原理
Phase 5: 缓存与搜索引擎        (1书+待补, 2周)  ES搜索 + Redis缓存
Phase 6: RPC与数据库中间件     (待补2书, 2周)  Dubbo + ShardingSphere
```

**核心逻辑**：

- Phase 1 是基础设施——EventLoop/Reactor模式/Pipeline/零拷贝/Epoll原理，不懂 Netty 就无法理解 RocketMQ/Nacos gRPC/Dubbo 的底层通信
- Phase 2 是核心能力——Kafka 代表"日志即存储"流处理模型，RocketMQ 代表"消息驱动"业务解耦模型，两个模型覆盖 80% 的 MQ 场景
- Phase 3 是分布式理论的工程实现——Etcd(Raft)/ZK(ZAB) 是两个最经典的共识算法落地方案，必须源码级对比
- Phase 4 从中间件上升到分布式存储系统设计——B+树/LSM/分区/复制/一致性，Phase 2 的存储设计在这里获得理论支撑
- Phase 5 补齐搜索和缓存——ES 倒排索引/Lucene/分片路由 + Redis 内存数据结构/持久化/集群
- Phase 6 补齐 RPC 通信和数据库分片——Dubbo 服务发现/协议/负载均衡 + ShardingSphere 分库分表/读写分离/编排

**Phase 依赖关系**：

```
P1(Netty网络基础)
 ├──→ P2(Kafka/RocketMQ MQ)
 ├──→ P3(Etcd/ZK 协调)
 ├──→ P6(Dubbo RPC)
 │
P4(分布式存储) ← 与 P2 互补(MQ存储 vs 数据库存储)
P5(缓存+搜索)  ← 独立但接入 P2(ES作为MQ消费端)
P6(RPC+分库)   ← 独立但接入 P2(Dubbo可通过MQ解耦)
```

---

## 二、书单与 Phase 映射

### 2.1 已有书单（MinerU 已提取）

| Phase | 编号 | 书名 | MinerU 质量 | 产物 |
|-------|------|------|-------------|------|
| P1 | 01 | Netty 原理剖析与实战 | ❌ 626行,大量HTML span | plan/P01-Netty网络基础/PLAN.md |
| P2 | 02 | Kafka权威指南(第2版) | ✅ 2942行,干净Markdown | plan/P02-消息队列核心/PLAN.md |
| P2 | 03 | RocketMQ消息中间件实战派(上册) | ⚠️ 4956行,轻OCR乱码 | plan/P02-消息队列核心/PLAN.md |
| P2 | 04 | RocketMQ消息中间件实战派(下册) | ⚠️ 5407行,轻OCR乱码 | plan/P02-消息队列核心/PLAN.md |
| P2 | 05 | Apache RocketMQ进阶之路 | ⚠️ 1874行,轻OCR乱码 | plan/P02-消息队列核心/PLAN.md |
| P3 | 06 | Etcd源码解析 | ✅ 6003行,干净Markdown | plan/P03-分布式协调/PLAN.md |
| P4 | 07 | 大规模分布式存储系统 | ⚠️ 4014行,OCR乱码偏多 | plan/P04-分布式存储/PLAN.md |
| P5 | 08 | 深入理解Elasticsearch(第2版) | ⚠️ 5520行,HTML span | plan/P05-缓存与搜索/PLAN.md |

### 2.2 需补充书单（无 MinerU 源，需自行获取）

| Phase | 编号 | 推荐书 | Tier | 理由 |
|-------|------|--------|------|------|
| P3 | ZK-01 | 《从Paxos到ZooKeeper》倪超 | **T1 源码+使用** | ZAB协议工程实现，分布式锁、选主、配置中心的经典方案 |
| P5 | Redis-01 | 《Redis设计与实现》黄健宏 | **T1 源码+使用** | SDS/ziplist/skiplist/ae事件循环/RDB+AOF/集群gossip——所有中间件的缓存层 |
| P5 | Redis-02 | 《Redis核心原理与实战》（可选参考） | T1 | 补充Redis 6.x/7.x新特性（多线程I/O、ACL、RESP3） |
| P6 | Dubbo-01 | 《Apache Dubbo 微服务开发实战》（GitChat） | **T2 使用** | Java RPC标准，sca-lab已通过Nacos掌握服务发现，需补RPC协议/负载均衡 |
| P6 | Shard-01 | 《ShardingSphere核心原理精讲》（电子书/Peng Wei） | **T2 使用** | 分库分表连接/代理/编排3层架构，sca-lab的my-xhs有counter分表可对照 |

### 2.3 Tier 全景（3级掌握标准）

**Tier 1：源码+使用（6个 = 中间件专家标志）**

| 中间件 | 现有书覆盖率 | 缺口 |
|--------|------------|------|
| Netty | ⚠️ 书有但提取质量差 | 需补充源码阅读指引 |
| Kafka | ✅ 完整 | — |
| RocketMQ | ✅ 完整(3本) | — |
| Etcd | ✅ 完整 | — |
| ZooKeeper | ❌ | 需补书+源码 |
| Redis | ❌ | 需补书+源码 |

**Tier 2：使用（3个）**

| 中间件 | 现有书覆盖率 | 学习路径 |
|--------|------------|---------|
| Elasticsearch | ✅ 完整 | Phase 5 产出 |
| Dubbo | ❌ | 需补书，sca-lab 已有服务治理基础 |
| ShardingSphere | ❌ | 需补书，sca-lab my-xhs counter 有分表实践 |

**Tier 3：了解（6个，不纳入主学习路线，按需查阅）**

| 中间件 | 了解重点 | 参考资料 |
|--------|---------|---------|
| RabbitMQ | AMQP协议 vs 日志模型差异 | 分布式系统书91第6章 |
| Pulsar | 计算存储分离架构 | 官方文档 |
| Seata | 分布式事务（sca-lab 已深入 B/C 模块） | sca-lab seata 模块 |
| Sentinel | 流量控制（sca-lab 已深入 A 模块） | sca-lab sentinel 模块 |
| Nacos | 服务治理（sca-lab 已手写客户端 D-01~D-07） | sca-lab nacos 模块 |
| Nginx/OpenResty | 反向代理/Lua/限流 | 在线文档 |

> **已掌握（sca-lab 达到源码级）**：Nacos、Sentinel、Seata、Resilience4j——这些不重复规划。

---

## 三、目录结构

```
tmp-question/10-中间件/
├── PLAN.md                          # 本文件
├── plan/                            # 各 Phase 详细规划（Phase B 产出）
│   ├── P01-Netty网络基础/
│   │   └── PLAN.md
│   ├── P02-消息队列核心/
│   │   └── PLAN.md                  # Kafka + RocketMQ 3本合并
│   ├── P03-分布式协调/
│   │   └── PLAN.md                  # Etcd + ZK
│   ├── P04-分布式存储/
│   │   └── PLAN.md
│   ├── P05-缓存与搜索/
│   │   └── PLAN.md                  # Redis + ES
│   └── P06-RPC与分库/
│       └── PLAN.md                  # Dubbo + ShardingSphere
└── md/                              # 深度文档产出目录（Phase C 产出）
    ├── P01-Netty/
    ├── P02-Kafka/
    ├── P02-RocketMQ/
    ├── P03-Etcd/
    ├── P03-ZooKeeper/               # 待补书后产出
    ├── P04-分布式存储/
    ├── P05-Redis/                   # 待补书后产出
    ├── P05-Elasticsearch/
    ├── P06-Dubbo/                   # 待补书后产出
    └── P06-ShardingSphere/          # 待补书后产出
```

---

## 四、质量标准

每份深度文档（Phase C 产出）必须覆盖 6 个维度：

| 维度 | 要求 | 检查方法 |
|------|------|---------|
| 源码阅读清单 | 列出要读的文件+行号+关键方法 | grep 源码仓库验证 |
| 完整调用链 | 函数/消息级别时序图 | 追踪到实现内部（如 Netty pipeline 触发链、Kafka 生产发送完整路径） |
| 设计哲学 | "为什么这样设计" | 反面假设表（为什么不用另一种设计） |
| 生产陷阱 | N 个错误+复现代码 | 可执行验证（如 Kafka 消费者 Rebalance 陷阱、RocketMQ 消息丢失场景） |
| 面试 Q&A | 含面试官意图分析 | 与设计哲学呼应（如"Kafka 为什么用 ISR 而不是 Raft?"） |
| 章节依赖 | 上下游文档引用 | 与 Phase 对齐（如 P2 文档必须引用 P1 Netty 中的 EventLoop 概念） |

---

## 五、实施流程

### 当前阶段：Phase B — 规划

1. ✅ 已完成：顶层 PLAN.md（本文件）
2. 进行中：逐 Phase 编写 `plan/P0X-xxx/PLAN.md`
3. 待做：获取 ZK/Redis/Dubbo/ShardingSphere 的 MinerU 源（或自行采购PDF提取）

### 规划顺序（按依赖拓扑）

```
先写：
  P01-Netty  →  P02-消息队列（Kafka/RocketMQ 可并列）
                                   ↓
                               P03-分布式协调（Etcd/ZK）
再写：
  P04-分布式存储（独立）
  P05-缓存与搜索（独立，Redis缺源暂标TODO）
  P06-RPC与分库（独立，缺源暂标TODO）
```

### 后续阶段
- **Phase C**：深度文档编写（`md/` 目录，逐 Phase 产出）
- **Phase D**：Review & 面试整合

---

## 六、MinerU 质量评估与处理策略

| 质量等级 | 书籍 | 处理策略 |
|---------|------|---------|
| ✅ 干净 | Kafka、Etcd | 直接阅读 full.md，按章节写深度文档 |
| ⚠️ 轻乱码 | RocketMQ×3、分布式存储 | 阅读时对照 PDF 原文修正关键段落 |
| ❌ 差 | Netty（626行+span标签）、ES（span标签） | Netty 严重不足——需重新提取或换源；ES 内容完整但格式差——写文档时需手动清理 |

---

## 七、关联资源

- **sca-lab 已学中间件**（不重复规划）：Nacos(C1~D07), Sentinel(A/B/C模块), Seata(B/C模块), Resilience4j
- **MySQL 学习**（独立 PLAN）：`tmp-question/MySQL-数据库/PLAN.md`
- **分布式系统学习**：MinerU `06-分布式系统-架构/`（独立学习章，共识算法/一致性理论覆盖 P3 的理论部分）
- **Redis 已学**：sca-lab my-xhs counter 模块（使用级，非源码级）
