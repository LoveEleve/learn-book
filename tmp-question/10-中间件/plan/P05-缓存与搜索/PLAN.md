# P05：缓存与搜索引擎 — 详细规划

> Phase: P05 | 周期: 2-3周 | 书: 1本(MinerU) + 2本(需补)
> 依赖: P01（Netty，ES Transport 层基于 Netty） + P04（LSM Tree 存储引擎）
> 定位: 缓存层（所有中间件的性能核心）+ 检索层（非结构化数据的中间件）

---

## 一、为什么缓存和搜索合并为一站

二者都是"数据加速层"：

- **Redis**：加速结构化数据访问——缓存热点数据、分布式锁、排行榜——所有中间件系统都依赖它做性能优化
- **Elasticsearch**：加速非结构化数据检索——倒排索引是搜索引擎的存储引擎，与 B+树/LSM Tree 并列

**Redis 在中间件全景中是 Tier 1 源码级必学**——你的 sca-lab my-xhs counter 模块只到了使用层，离源码级差距还很大。

---

## 二、书单

### 2.1 《深入理解 Elasticsearch（原书第2版）》

| 维度 | 信息 |
|------|------|
| MinerU 路径 | `10-中间件-消息队列/深入理解 Elasticsearch（原书第2版）/` |
| MinerU 质量 | ⚠️ 5520行，HTML span标签，内容完整但格式差 |
| 处理策略 | 内容完整可用，写文档时需手动清理格式 |

| 章 | 核心知识点 | 学习深度 |
|----|-----------|---------|
| 第1章 ES简介 | ES架构、**倒排索引原理**、分片/副本、近实时搜索 | **精读** |
| 第2章 查询DSL进阶 | 查询/过滤上下文、复合查询、**相关性评分(BM25)** | **精读** |
| 第3章 不只是文本搜索 | 地理位置搜索、聚合分析、父子关系/嵌套对象 | 使用层 |
| 第4章 改善用户搜索体验 | 拼写纠错（编辑距离）、自动补全（FST）、同义词 | **精读** |
| 第5章 分布式索引架构 | **分片路由（routing）**、**索引生命周期**、**段合并（Segment Merge）** | **深度精读** |
| 第6章 底层索引控制 | 分析器（分词/过滤）、映射（mapping）、**索引模板** | **精读** |
| 第7章 管理ES | 集群健康、备份恢复、索引别名 | 使用层 |
| 第9章 开发ES插件 | 插件架构、自定义分析器/评分 | 了解 |

**精读章（5章）**：Ch1 倒排索引、Ch2 查询DSL、Ch4 搜索体验、Ch5 分布式索引架构、Ch6 底层索引控制

### 2.2 Redis 书单（需补，Tier 1 源码级）

| 编号 | 书名 | 定位 | 状态 |
|------|------|------|------|
| Redis-01 | 《Redis 设计与实现》黄健宏 | **核心**——覆盖数据结构、单机数据库、多机数据库、独立功能 4 部分 | ❌ 需补 |
| Redis-02 | Redis 7.x 源码（GitHub redis/redis） | 源码级验证——每个数据结构的 C 实现 | ✅ 开源 |

**预期学习要点（Redis 源码线）**：

| 模块 | 源码文件 | 关键知识点 |
|------|---------|-----------|
| 数据结构 | `sds.c` | SDS 动态字符串（len/free/buf，杜绝缓冲区溢出） |
| | `ziplist.c` | 压缩列表——连续内存、双向遍历、级联更新 |
| | `quicklist.c` | 快速列表——ziplist + linkedlist 混合（List 类型底层实现） |
| | `dict.c` | 哈希表——渐进式 rehash（`ht[0]` → `ht[1]`，分步迁移） |
| | `intset.c` | 整数集合——升级不支持降级 |
| | `skiplist.c` | 跳表——ZSet 底层，多层索引节点 |
| | `listpack.c` | 7.0 替代 ziplist——消除级联更新 |
| 事件驱动 | `ae.c` | 文件事件 + 时间事件��—epoll/kqueue/select 统一封装 |
| 持久化 | `rdb.c` | RDB 快照——fork + COW + 二进制格式 |
| | `aof.c` | AOF 日志——命令追加 + 后台重写（BGREWRITEAOF） |
| 复制 | `replication.c` | 主从复制——PSYNC + 复制积压缓冲区 |
| 哨兵 | `sentinel.c` | 哨兵——主观下线/客观下线/故障转移/Leader 选举 |
| 集群 | `cluster.c` | Cluster——16384 槽位 + gossip 协议 + MOVED/ASK 重定向 |
| 过期 | `expire.c` | 惰性删除 + 定期删除（activeExpireCycle） |

### 2.3 《Redis高手心法》—— 已有 PDF 无法使用

| 维度 | 信息 |
|------|------|
| 位置 | `05-MySQL-数据库/redis高手心法.pdf`（仅原始 PDF，无 MinerU full.md） |
| 定位 | 使用层书（缓存穿透/击穿/雪崩、分布式锁、延时队列等实战场景） |
| 处理 | 暂不纳入 Phase C 深度文档线，作为使用层参考 |

---

## 三、Elasticsearch 源码阅读清单

ES 基于 Lucene，源码以"理解索引/搜索链路"为主：

| 源码模块 | 关键类 | 阅读目的 |
|---------|--------|---------|
| 索引写入 | `InternalEngine.java`, `IndexingMemoryController.java` | 文档 index → Lucene IndexWriter → translog |
| 索引刷新 | `InternalEngine.refresh()` | refresh → 新建 segment → 文档可搜索（近实时） |
| 段合并 | `MergeScheduler.java`, `TieredMergePolicy.java` | Segment Merge 策略与执行 |
| 搜索 | `SearchService.java`, `QueryPhase.java`, `FetchPhase.java` | Query → Fetch 两阶段搜索 |
| 分片路由 | `OperationRouting.java` | `_routing` → 分片定位 |
| 集群状态 | `ClusterState.java`, `MasterService.java` | 集群元数据管理 |
| 副本 | `IndexShard.java`, `ReplicationTracker.java` | 主分片 → 副本同步 |

---

## 四、关键设计哲学

### ES/Lucene

| 设计决策 | 反事实假设 | 为什么选这个 |
|---------|-----------|-------------|
| **为什么用不可变的 Segment？** | 可变索引文件 | 不可变 → 不需要锁 → 读并发无冲突；新数据写入新 Segment → refresh 后可见（近实时） |
| **为什么段合并（Segment Merge）是必需的？** | 不合并 | 每 refresh 产生一个 Segment → Segment 数量无限增长 → 搜索需遍历所有 Segment → 性能退化 |
| **为什么 refresh 默认 1 秒？** | 实时可见（每次写入 refresh） | 频繁 refresh → 频繁创建 Segment → 大量小文件 + 频繁 merge → 写入性能下降；1 秒是近实时的工程平衡 |

### Redis

| 设计决策 | 反事实假设 | 为什么选这个 |
|---------|-----------|-------------|
| **为什么 Redis 是单线程的？** | 多线程并发读写 | 内存操作极快（微秒级），单线程消除锁竞争 + 原子性天然保证——瓶颈在网络 IO 而非 CPU |
| **为什么渐进式 rehash？** | 一次性 rehash | 大哈希表（百万 key）一次性迁移会阻塞服务——分步迁移（每次增删改查迁移一个桶） |
| **为什么 Cluster 用 16384 个槽？** | 65536 或更多 | CRC16 取模的合理上限（心跳包大小可控 + 集群节点数不会超过 1000） |
| **为什么 RDB 用 fork + COW？** | 直接阻塞保存 | fork 子进程共享父进程内存（COW），只在父进程写时才复制——快照过程不阻塞主进程 |

---

## 五、生产陷阱

### ES 陷阱

| # | 陷阱 | 场景 | 修复 |
|---|------|------|------|
| 1 | **Z次合并耗尽 IO** | Segment 堆积过多 → 大量 merge → 磁盘 IO 满载 → 搜索超时 | 控制 refresh_interval + 监控 merge 线程 |
| 2 | **fielddata 内存溢出** | 对 text 字段做聚合/排序 → 加载全部 term 到堆内存 → OOM | 使用 `keyword` 类型代替 text 聚合，或启用 `doc_values` |
| 3 | **脑裂（split-brain）** | `discovery.zen.minimum_master_nodes` 配错 → 网络分区产生双 Master | 设置为 `N/2+1`（7.x 后自动配置） |
| 4 | **mapping 爆炸** | 动态 mapping（`dynamic: true`）→ 字段数量失控 → cluster state 膨胀 | 预定义 mapping + `dynamic: strict` |

### Redis 陷阱

| # | 陷阱 | 场景 | 修复 |
|---|------|------|------|
| 1 | **BigKey 阻塞** | DEL 一个百万元素的 Set → 单线程阻塞 → 所有请求超时 | UNLINK（异步删除）替代 DEL |
| 2 | **AOF 重写期间内存翻倍** | BGREWRITEAOF fork 子进程 → COW 导致两份数据 | 监控 `aof_rewrite_in_progress` + 控制重写触发阈值 |
| 3 | **主从复制风暴** | 多个 Slave 同时全量同步 → Master RDB 生成 + 网络传输 → CPU/带宽满载 | 树状复制（slave-of-slave）减少 Master 压力 |
| 4 | **Cluster 节点间 gossip 超时** | 节点数过多（100+）→ gossip 消息指数增长 → 带宽占满 | 控制集群规模 + 使用 Proxy（如 Redis Cluster Proxy） |

---

## 六、面试 Q&A

| 问题 | 面试官意图 | 回答主线 |
|------|----------|---------|
| "ES 的写入流程？" | 考察是否理解近实时搜索 | 写入内存buffer → refresh(1s)创建Segment→可搜索；translog持久化→flush→segment写入磁盘 |
| "ES 为什么近实时？" | 考察 refresh 机制理解 | refresh 创建新 Segment 并打开，使文档可搜索——但 Segment 还在 OS cache，未 fsync 到磁盘 |
| "Redis 为什么快？" | 考察是否只知"单线程"（肤浅） | 内存操作(纳秒级) + 单线程无锁 + IO多路复用(epoll) + 高效数据结构(ziplist/skiplist) + RESP简单协议 |
| "Redis Cluster 的 MOVED 和 ASK 有什么区别？" | 考察集群路由理解 | MOVED=槽已迁移（永久重定向，客户端更新路由表）；ASK=槽正在迁移中（临时重定向，仅本次查询） |
| "Redis 主从复制的 PSYNC 和全量同步流程？" | 考察复制机制 | PSYNC(部分同步，从复制积压缓冲区读取差异) / 全量同步(RDB生成→传输→加载→积压缓冲区回放) |
| "倒排索引的原理？" | 考察搜索引擎基础 | 文档ID→词→建立"词→文档ID列表"的倒排表，查询时通过词典(FST/B+树)定位到倒排表 |

---

## 七、章节依赖

```
上游：
  P01 Netty：ES Transport 层基于 Netty（节点间通信）
  P04 分布式存储：Lucene Segment 文件的存储设计可以与 P04 Ch2(B+树/LSM) 对比

下游：
  无直接下游（P05 缓存在所有后续业务系统中被引用）
```

---

## 八、产出物（Phase C）

```
md/P05-Elasticsearch/
├── D-01-倒排索引与查询DSL.md            # FST词典 + 倒排表 + BM25评分
├── D-02-分布式索引架构.md               # 分片路由 + refresh/flush/merge 生命周期
├── D-03-搜索体验优化.md                 # 纠错/补全/同义词（编辑距离 + FST + 同义词过滤器）
└── D-04-ES集群管理.md                   # 脑裂/备份/监控

md/P05-Redis/（待补书后产出）
├── D-01-数据结构SDS与ziplist.md         # SDS动态串/ziplist/quicklist/dict/skiplist C源码
├── D-02-事件驱动ae.md                   # epoll封装 + 文件事件 + 时间事件
├── D-03-持久化RDB与AOF.md               # fork+COW快照 + AOF重写 + 混合持久化
├── D-04-主从复制Sentinel与Cluster.md    # PSYNC + 哨兵故障转移 + 16384槽 + gossip
└── D-05-生产陷阱与面试Q&A.md            # 8个陷阱 + 6个面试题
```

## 九、学习检查清单

- [ ] 能手写 ES refresh → flush → merge 的完整时间线（含各阶段涉及的文件和内存结构）
- [ ] 能画出倒排索引的结构：Term Dictionary(FST) → Posting List(SkipList) → DocID → Term Frequency/Position
- [ ] 能画出 Redis SDS 的内存布局（len/free/buf）并与 C 字符串对比
- [ ] 能解释 Redis dict 渐进式 rehash 的分步过程（`rehashidx` 从 -1 → 0 → N → -1）
- [ ] 能画出 Redis Cluster MOVED/ASK 重定向的完整交互时序
- [ ] 能写出 PSYNC 部分同步时复制积压缓冲区的作用（环形缓冲区 + `repl_offset` + `repl_backlog_histlen`）
