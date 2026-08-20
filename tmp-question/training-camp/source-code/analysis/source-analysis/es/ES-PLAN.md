# Elasticsearch — 知识网络化规划 (E-1~E-12, 09 域重审后 v1)

> **日期**: 2026-08-14 | 依据: issue/Elasticsearch源码学习范围规划.md (11 域) + issue/源码分析执行计划.md 阶段3.7 (E-1~11) + **09 对既有规划保持怀疑 全量域重审** (顶层包扫描+数字穷举+依赖方向+拓扑重排)
> **源码**: `/data/workspace/source-code/code/spring/elasticsearch` (**v8.12.2**, Lucene 9.9.2, server 4023 文件, 35 顶层包)
> **定位**: 阶段3.7 — 数据与存储最后一环. **搜索引擎内核 = Lucene 集成 + 写入路径 + 查询路径 + 集群路由 + 分片复制**. 与阶段3.5 Redis (服务端内存引擎) 互为对照: Redis 讲"怎么存", ES 讲"怎么搜"
> **与 Spring Boot 关系**: 阶段2 已产出 `s88-boot-elasticsearch` (S-24, ElasticsearchRestClientAutoConfiguration) — 只讲客户端自动装配接线, **明写"连接/协议深入在阶段3 ES"** — ES 域必须承接 (本规划 §五 承接点表)
> **知识网络**: 本文含 前置/复用/对照/引出 双链; 与 Redis 33 域 + Spring Boot (s88) + Redisson (rd*) 互联
> **并行**: 阶段3.5 Redis 已 25/33 完成, Redisson 9/9 完成 — 交叉引用目标真实存在 (redis/outlines 27 目录已核验)

---

## 〇、09 怀疑审计表 (Elasticsearch, 2026-08-14) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| **域清单 11 个** | 顶层包扫描 (35 包 4023 文件) | **search/aggregations/ = 516 文件** (bucket 257 + metrics 143 + pipeline 46 + 顶包 43 + support) — 规划淘汰理由 "modules/aggregations 46 文件, 面试低频" 双重错误: ① 聚合核心在 **server 包** 非 modules (modules/aggregations 仅 83 文件且多为测试) ② 聚合 (terms/date_histogram/cardinality/composite) 是 ES 定义特征 (搜索+聚合双支柱), 面试高频 | **修正: 新增 E-12 Aggregations** 🔴 |
| 阅读顺序 E-2 Search 在 E-7 Mapping 后 | 依赖方向 import 统计 | **search/ 83 文件 import org.elasticsearch.index.mapper**; SearchService import QueryBuilder/MapperService (L54-61); QueryPhase 挂载 AggregationPhase (L133,195) — Search 必须先懂字段类型 + 聚合挂载点 | **修正: E-7 Mapping 提前, E-12 Aggregations 在 E-2 后** |
| E-11 FieldData 未在阅读顺序 | 规划 §六 对照 | 域清单有 E-11, **阅读顺序遗漏** (规划 §六 只列 10 域) | **修正: 插入 E-5 与 E-2 之间** (依赖 Mapping) |
| "FieldData 67 文件" | find 穷举 | index/fielddata = **103 文件** (fieldcomparator/ordinals/plain 3 子目录) | **修正: 103** |
| "flush 默认 30min" | grep IndexSettings | **index.translog.flush_threshold_age 默认 1 分钟** (IndexSettings.java:358); 512MB (L373) 对 | **修正: 1min + 512MB** |
| "modules 27 / libs 17" | ls 穷举 | modules 27 ✅ / libs **18** (规划 17, 漏 preallocate) | 部分修正 |
| "libs/core 32 / x-content 45" | find 穷举 | libs/core **46** / libs/x-content **80** | 修正 |
| BM25 k1=1.2, b=0.75 | grep | SimilarityProviders.java:258-259 (`getAsFloat("k1", 1.2f)` / `("b", 0.75f)`) | **接受** ✅ |
| Translog.newSnapshot() Translog.java:657 | grep | 精确命中 L657 | **接受** ✅ |
| IndexShard 5 状态 | grep IndexShardState | CREATED(12)/RECOVERING(13)/POST_RECOVERY(14)/STARTED(15)/CLOSED(17) | **接受** ✅ |
| InternalEngine IndexingStrategy | grep | planIndexingAsPrimary L1314 / canOptimizeAddDocument L1059 / indexIntoLucene L1384 / IndexingStrategy L1483 | **接受** ✅ |
| LiveVersionMap 刷新 | grep | LiveVersionMap.java:25 (implements RefreshListener), maybePutIndexUnderLock L337 | **接受** ✅ |
| refresh 默认 1s | grep IndexSettings | DEFAULT_REFRESH_INTERVAL L279 = 1s | **接受** ✅ |
| durability 默认 request | grep | IndexSettings.java:104 (REQUEST) | **接受** ✅ |
| CoordinationState Raft 风格 | grep | CoordinationState.java:32 (656 行) + PersistedState 内嵌接口 L533 + InMemoryPersistedState + ElectionStrategy L20 | **接受** ✅ |
| RoutingTable 版本递增 | grep | RoutingTable.java:59 withIncrementedVersion | **接受** ✅ |
| ReplicationGroup | grep | ReplicationGroup.java:24-25 (inSyncAllocationIds/trackedAllocationIds) | **接受** ✅ |
| GlobalCheckpoint | grep | ReplicationTracker.java:147 (volatile globalCheckpoint) | **接受** ✅ |
| ReplicationOperation | grep | action/support/replication/ReplicationOperation.java:48 (698 行) | **接受** ✅ |
| 分片分配决策器 | find | cluster/routing/allocation/decider = **19 个 Decider** (规划 "10+") | 补全 ✅ |
| OperationRouting 位置 | find | **cluster/routing/OperationRouting.java** (规划未注路径) | 补位置 ✅ |
| 时空溯源可行性 | git log/tag | 原 shallow clone 仅 1 commit → **已 fetch --unshallow: 74277 commits + 43 tags (v0.4.0~v8.12.2)** | **可行** ✅ |
| s88-boot-elasticsearch 承接 | 读 s88 大纲 | s88 明写 "连接/协议, 不展开 — 深入在阶段3 ES"; ES 规划淘汰 transport/http 通信层 — 承接点落在 E-4 (节点间路由) + E-10 (集群发布) 的连接面 | 承接点标注 (§五) |

**覆盖率报告**: 执行计划 11 域 → 重审后 **12 域** (+1 Aggregations — 516 文件定义特征级被错误淘汰, 09 反模式 6 案例)。数字断言 20 项: 8 修正 (40%) / 12 接受。与 Redis 18→33 / MyBatis 5→7 相比, ES 规划域清单错误率较低 (9%), 但聚合遗漏是**定义特征级**错误。

---

## 一、入口点与主线

ES 是**服务端中间件**, 无单一用户入口 (不是库/框架) — 入口 = 两条业务路径:

```
写入: REST IndexRequest → TransportBulkAction → TransportShardBulkAction → IndexShard.applyIndexOperationOnPrimary (L894)
      → InternalEngine.index (L1131): planIndexingAsPrimary (L1314) → ①Translog.add (WAL 顺序写)
      → ②indexIntoLucene (L1384, IndexWriter 内存 buffer) → ③IndexResult(version, seqNo, translogLocation)
      → ReplicationOperation (698 行) 复制到副本 → refresh (1s) 后可见

查询: SearchRequest → SearchService (1825 行) → QueryPhase.execute (L61): CollectorManager → Lucene IndexSearcher.search
      → 分片打分 (BM25 k1=1.2 b=0.75) → 聚合挂载 (AggregationPhase, L133) → FetchPhase (378 行) 拉 _source
```

旁路: Translog 持久化 (durability=REQUEST fsync) + 段合并 (TieredMergePolicy + ConcurrentMergeScheduler) + 集群协调 (CoordinationState 两阶段发布) + 路由 (RoutingTable versioned + ShardRouting 4 状态)。

---

## 二、入口展开追踪 (00 §2)

| 候选 | 源码位置 | 设计决策测试 | 结论 |
|---|---|---|---|
| `InternalEngine.index/IndexingStrategy` | InternalEngine.java:1131,1314 (3378 行) | NRT 写入核心 — 定义特征 | **E-1** |
| `QueryPhase/Collector 体系` | QueryPhase.java:61 + QueryPhaseCollectorManager (701 行) | 两阶段查询 — 定义特征 | **E-2** |
| `Translog/TranslogWriter` | Translog.java:657 (1941 行) | WAL 持久化 — 定义特征 | **E-3** |
| `RoutingTable/ShardRouting/OperationRouting` | RoutingTable.java:45 (654 行) + ShardRouting (992 行) + OperationRouting | 分布式路由 — 定义特征 | **E-4** |
| `IndexShard/ReplicationGroup/OperationPermits` | IndexShard.java (4242 行) + ReplicationGroup (146) + Permits (284) | 分片生命周期 — 定义特征 | **E-5** |
| `SequenceNumbers/ReplicationOperation` | seqno/ 16 文件 + ReplicationOperation (698) | seqNo+primaryTerm 复制协议 — 定义特征 | **E-6** |
| `DocumentMapper/MapperService/FieldMapper 体系` | index/mapper/ 125 文件 (DocumentMapper 141, MapperService 725, DocumentParser 944, TextFieldMapper 1454) | 文档→Lucene 映射 — 支撑 | **E-7** |
| `MergePolicyConfig/TieredMergePolicy/ConcurrentMergeScheduler` | index/MergePolicyConfig.java (368) + index/merge/ | 段合并优化 — 支撑 | **E-8** |
| `TransportBulkAction/BulkShardRequest` | action/bulk/ 24 文件 (TransportBulkAction 1059, TransportShardBulkAction 668) | 批量写入面 — 支撑 | **E-9** |
| `ClusterState/Metadata/CoordinationState` | ClusterState (1195) + Metadata (2857) + CoordinationState (656) + Coordinator (2158) | 集群状态+共识 — 支撑 | **E-10** |
| `IndexFieldData/GlobalOrdinals/CircuitBreaker` | index/fielddata/ **103 文件** | docValues 列式加载 — 支撑 (重审数字修正) | **E-11** |
| `Aggregator 体系/桶遍历/管道聚合` | search/aggregations/ **516 文件** (MultiBucketConsumerService 144, GlobalOrdinalsStringTermsAggregator 974, CompositeAggregator 633) | **聚合分析 — 定义特征, 重审新增** | **E-12** |

---

## 三、域清单 (12 域: 7🔴 + 5🟡, 09 重审修正 v1)

| # | 域 | 文件 (行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| E-3 | **Translog WAL** | index/translog/ 20 文件 (Translog 1941, TranslogWriter, TranslogDeletionPolicy, Checkpoint) | 双缓冲写入/FileChannel.force/durability REQUEST/ckp 文件/Generation 轮转/newSnapshot (L657) 恢复 | 🔴 A |
| E-7 | **Mapping 映射** | index/mapper/ 125 文件 | 字段类型体系 (text/keyword/numeric/date 1454/1070/1947/987 行)/DocumentParser (944)/DynamicMapping/FieldMapper→Lucene 四索引面 | 🔴 A |
| E-1 | **Index Engine 写入路径** | InternalEngine (3378) + LiveVersionMap + SoftDeletesPolicy | IndexingStrategy 计划-执行 (L1314)/append-only 优化 (L1059)/LiveVersionMap 内存版本/refresh 1s (IndexSettings:279)/flush 1min+512MB (L358,373)/readLock 并发写 | 🔴 A |
| E-6 | **SeqNo 复制协议** | index/seqno/ 16 文件 + action/support/replication/ReplicationOperation (698) | seqNo 递增 (UNASSIGNED=-2)/primaryTerm/ReplicationTracker globalCheckpoint (L147)/LocalCheckpointTracker/两阶段复制 | 🔴 A |
| E-5 | **Shard 生命周期** | IndexShard (4242) + ReplicationGroup (146) + IndexShardOperationPermits (284) + GlobalCheckpointSyncer | 5 状态 (IndexShardState L12-17)/Engine+Translog+Store 三组件/ReplicationGroup inSync 集合/许可信号量/GlobalCheckpoint 推进 | 🔴 A |
| E-11 | **FieldData Cache** | index/fielddata/ **103 文件** (GlobalOrdinalsBuilder/Ordinals/SortedBinaryDocValues) | docValues 列式加载/GlobalOrdinals 跨段去重/fielddata.breaker.limit 40% heap/CircuitBreakingException/ES5 前 OOM 史 | 🟡 B |
| E-2 | **Search 查询路径** | search/ (809) + QueryPhase (270) + QueryPhaseCollectorManager (701) + FetchPhase (378) | CollectorManager 并行/BM25 (k1=1.2 b=0.75)/BooleanQuery 重写/scorer 迭代/两阶段 Query+Fetch/DFS 三阶段/聚合挂载点 (L133) | 🔴 A |
| E-12 | **Aggregations 聚合** (新增) | search/aggregations/ **516 文件** | Aggregator 生命周期/AggregationPhase 挂载/bucket 遍历策略 (深度优先 vs 广度优先)/延迟桶 BestBucketsDeferringCollector/terms GlobalOrdinals 加速 (974)/composite (633)/管道聚合 (46)/maxBuckets=65536 保护 | 🔴 A |
| E-4 | **Cluster Routing** | cluster/routing/ 99 文件 + allocation/decider 19 个 | RoutingTable versioned (L59)/ShardRouting 4 状态/AllocationDeciders 19 决策器/BalancedShardsAllocator/OperationRouting 读写路由 | 🔴 A |
| E-8 | **Merge Policy** | MergePolicyConfig (368) + index/merge/ + ElasticsearchConcurrentMergeScheduler | TieredMergePolicy 分层合并/maxMergeCount 限流/forceMerge 冷索引 | 🟡 B |
| E-9 | **Bulk 批量** | action/bulk/ 24 文件 (TransportBulkAction 1059) | NDJSON/按 shard 分组 BulkShardRequest/TransportShardBulkAction 主分片执行 (L360)/pipeline 预处理 | 🟡 B |
| E-10 | **Cluster State** | ClusterState (1195) + Metadata (2857) + coordination/ 62 文件 | ClusterState 三层/CoordinationState Raft 风格 (L32)/PersistedState (L533)/两阶段发布/Coordinator (2158)/ElectionStrategy | 🟡 B |

## 四、已排除 (00 §3 — 防"存在=域")

| 类/包 | 文件数 | 原因 |
|---|---|---|
| api 门面 (client/rest) | 186 | REST 客户端 — 阶段2 s88 已讲装配, 连接/协议是 Netty 中间件面 (淘汰) |
| transport/ + http/ | 108 | Netty 通信层 — 中间件, 非搜索引擎内核; 节点间连接面在 E-4/E-10 一句话 |
| discovery/ (Zen) | 13 | 节点发现 — 基础设施 (CoordinationState 在 cluster/coordination, E-10 已含) |
| gateway/ | 18 | 集群状态持久化恢复 — 运维层 |
| monitor/ threadpool/ | 35 | JVM/OS 监控、线程池 — JVM 层 |
| script/ + modules/lang-* | 119 | Painless/Expression/Mustache 脚本 — ES 特有, 非通用 |
| modules/analysis-common | 134 | 分词/分析器 — 自然语言处理层 (index/analysis 36 文件注册框架并入 E-7 一句话) |
| modules/ingest-*/ ingest/ | 26 | 管道预处理 — ES 特有 |
| modules/aggregations | 83 | **测试为主** — 聚合核心在 server (E-12) |
| modules/data-streams/parent-join/percolator/rank-eval/reindex 等 | — | ES 高级特性 — 非搜索引擎内核 |
| snapshots/ repositories/ | 41 | 快照/备份 — 运维操作 |
| search/suggest/ | 52 | 建议器 (completion/phrase) — 面试低频, E-2 一句话 |
| action/ 大部分 | 844 | REST action 层 — 仅 Bulk/Replication 取代表类 (E-6/E-9) |
| common/ | 493 | 工具 (BytesReference/UUID/Settings) — 融入各域 |
| indices/ | 90 | 多索引协调 (IndicesService) — E-10 一句话 (ClusterState 消费) |
| index/query/ | 92 | QueryBuilder DSL 构建器 — 并入 E-2 (查询构造面) |
| search/profile/ runtime/ vectors/ | 100+ | 性能剖析/运行时字段/向量搜索 — 8.x 新特性, E-2 一句话 |

## 五、知识网络图 (Obsidian 双链 — 全部真实存在目录核验)

### 双向引用总表 (源头域 → ES 侧)

| 源头域 (已存在, 产出目录) | ES 侧 | 关系 (06 §2.5 复用≠省略 — 内核引用 + 本层视角展开) |
|---|---|---|
| `s88-boot-elasticsearch` (阶段2) | E-1/E-4/E-10 | **核心承接** (反向链接最重要): s88 明写 "连接/协议, 不展开 — 深入在阶段3 ES" — ES 侧在 E-1 承接 "客户端 IndexRequest 怎么进到服务端 (TransportShardBulkAction)", E-4/E-10 展开节点间连接面 (transport 一句话) |
| `r28-networking` (Redis RESP) | E-4/E-10 | **对照**: Redis RESP 文本协议 (简单可手工抓包) vs ES 传输层 (版本化二进制 + TransportVersions 断代) |
| `r8-persistence` (Redis RDB/AOF) | E-3 Translog | **对照**: Redis AOF (append + rewrite) vs ES Translog (generation 轮转 + flush 截断) — 两代 WAL 设计对比 |
| `r9-replication` (Redis 主从) | E-6 SeqNo | **对照**: Redis 复制偏移量 (字节位点) vs ES seqNo+primaryTerm (逻辑位点) — 面试对比点 |
| `r22-expire` / `r23-evict` (Redis 过期/淘汰) | E-8 Merge/E-11 | **对照**: Redis 惰性+主动过期 vs ES 段合并删除标记/soft-deletes — 删除语义两范式 |
| `r21-db` (Redis 键空间) | E-7 Mapping/E-10 | **对照**: Redis db dict (key→value 裸字节) vs ES 索引 (key→解析后字段列) — 无模式 vs 强模式 |
| `rd3-codec` (Redisson 序列化) | E-7 | **对照**: Redisson 客户端 codec (对象↔字节) vs ES Mapping (文档↔字段列) — 两层编码 |
| `rd1-connection` (Redisson 连接池) | E-4/E-10 | **对照**: Redisson Netty 连接池 vs ES 节点连接 (EsConnection 池化) — 客户端 vs 服务端连接管理 |
| `h13-datasource` (HikariCP) | E-1/E-3 | **对照**: Hikari 池生命周期 vs IndexShard 生命周期 — 资源生命周期管理对照 |
| `s19-cacheable` / `m7-cache` | E-11 | **对照**: 本地缓存一致性 vs fielddata 内存缓存 (无一致性, 容量兜底) |
| `rd6-localcachedmap` | E-11 | **对照**: 客户端本地缓存 (一致性协议) vs 服务端 fielddata (无一致性问题, 纯容量问题) |
| `s77-boot-cache` | E-11 | **对照**: Boot CacheManager 抽象 vs ES 聚合缓存 (无统一抽象, 各聚合自管) |

### 本域内部双链 (ES 内)

**E-3 Translog** (叶子) → 前置 E-1 | **E-7 Mapping** (叶子) → 前置 E-1/E-5/E-11 | **E-1 Engine** → 前置 E-6/E-5 | **E-6 SeqNo** → 前置 E-5 | **E-5 Shard** → 前置 E-4/E-9/E-10 | **E-11 FieldData** → 前置 E-2/E-12 | **E-2 Search** → 前置 E-12 (聚合挂载) | **E-12 Aggregations** → 前置 E-4? (独立) | **E-4 Routing** → 前置 E-9/E-10 | **E-8 Merge** → 依赖 E-1 | **E-9 Bulk** → 消费 E-1/E-5 | **E-10 ClusterState** → 消费全部 (收束)

📌 每篇大纲 header 必须含 (06 §6):
```
前置: [[r28-networking]] ...
复用: [[s88-boot-elasticsearch]] ...
对照: [[r8-persistence]] [[rd1-connection]] ...
引出: [[E-5-shard]] ...
```
> ⚠️ 引用规则: **只引已存在目录** (上表左列, 已 [ -d ] 核验); 未完成域不得引用 — 06 §2"不该做"。若并行 AI 后续产出新域, 再补链。

## 六、执行顺序 (拓扑: 叶子先, 09 重排 v1)

**E-3 → E-7 → E-1 → E-6 → E-5 → E-11 → E-2 → E-12 → E-4 → E-8 → E-9 → E-10**

> 拓扑理由: **E-3 Translog 叶子** (WAL 独立, 无 ES 内部依赖, 先讲持久化底座); **E-7 Mapping 叶子** (字段类型是文档模型, Search/Engine/FieldData 全消费 — 原计划排第 7 位反拓扑); **E-1 Engine** (消费 Translog+Mapping+SeqNo, 写入路径核心); **E-6 SeqNo** (Engine 分配, 复制协议); **E-5 Shard** (聚合 Engine+Translog+SeqNo+ReplicationGroup, 分片是复制/恢复的容器); **E-11 FieldData** (依赖 Mapping, 为聚合/排序供数据 — 原计划漏排); **E-2 Search** (消费 Mapping/FieldData + Lucene collector, 挂载聚合); **E-12 Aggregations** (消费 FieldData + QueryPhase 挂载点, 搜索后半场); **E-4 Routing** (分片路由, 服务端分布式面); **E-8 Merge** (依赖 Engine 段管理, 优化面); **E-9 Bulk** (消费 E-1/E-5 写入路径, 批量面); **E-10 ClusterState** (集群状态收束, 消费 Routing/Metadata, 全书最后)。
> **与原计划差异**: E-7 从第 7 位提前到第 2 位 (反拓扑修正); E-12 新增插 E-2 后; E-11 补入顺序; 写路径先行 (Translog/Mapping/Engine/SeqNo/Shard) → 读路径 (FieldData/Search/Aggregations) → 分布式面 (Routing/Merge/Bulk/ClusterState)。

## 七、深度分类复核 (00 §3.5)

- **7🔴 / 5🟡** (v1: 原 6🔴/5🟡 → 新增聚合 🔴)
- 🔴 = 定义特征: 写入 (Engine)、查询 (Search)、持久化 (Translog)、分布式路由 (Routing)、分片 (Shard)、复制协议 (SeqNo)、**聚合 (Aggregations)**
- 🟡 = 支撑与集成: Mapping/FieldData/Merge/Bulk/ClusterState (有独立设计决策但非"ES 之所以是 ES")
- 巨型域: 无 (最大 InternalEngine 3378 行 / aggregations 516 文件但拆 3 篇内) — 大域拆分: E-1 拆 3 篇 (01-indexing / 02-versioning-nrt / 03-lucene-lifecycle), E-2 拆 3 篇, E-12 拆 3 篇

## 八、与原始执行计划的差异汇总 (v1)

| 原始 | 重审后 | 理由 |
|:--:|:--:|---|
| 11 域 | **12 域** (+1) | search/aggregations 516 文件定义特征级被错误淘汰 ("modules 46 文件" 双重错误) |
| E-2 在 E-7 前 | **E-7 提前到第 2 位** | search 83 文件 import index.mapper — 反拓扑 |
| E-11 漏排 | 插入 E-5 后 E-2 前 | FieldData 是聚合/排序数据底座 |
| FieldData 67 文件 | **103** | find 穷举 (3 子目录) |
| flush 30min | **1min** | IndexSettings.java:358 实证 |
| libs 17 | 18 | 漏 preallocate |
| 时空溯源不可行 | **可行** | fetch --unshallow: 74277 commits + 43 tags |
| s88 承接未处理 | 承接点表 (§五) | s88 "连接/协议深入在阶段3 ES" 必须在 ES 域显式回应 |

## 九、完成检查单 (00 §8)

- [x] 顶层包扫描 (35 包 4023 文件) ↔ 域清单覆盖矩阵
- [x] 全部数字断言穷举验证 (20 项: 8 修正 / 12 接受)
- [x] 依赖方向 import 证据 (search→mapper 83, engine→translog 5, shard→engine 11)
- [x] 拓扑重排完成 (E-7 提前/E-11 补位/E-12 新增), 差异逐条记录理由
- [x] 新增域 (E-12) 全过 00 §3 + §3.5 测试 (516 文件定义特征级, 面试/生产/框架依赖 3 信号全高)
- [x] 怀疑审计表已写入 (§〇)
- [x] 交叉引用核验: redis/outlines 27 目录 + spring/outlines s88 + redisson/outlines rd* 全 [ -d ] 验证
- [x] 偏差待同步 HANDOFF-STAGE3 + HANDOVER.md
