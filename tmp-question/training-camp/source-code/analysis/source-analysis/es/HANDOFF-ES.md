# Elasticsearch 源码分析 — 交接文档 v2 (阶段3.7, 12/12 域交付)

> **日期**: 2026-08-14 | 阶段3.7 Elasticsearch (v8.12.2, Lucene 9.9.2, server 4023 文件, 35 顶层包)
> **⚠️ 总入口**: 阶段3 总交接见 `../HANDOFF-STAGE3.md` — 本文为 ES **分域唯一入口** (自包含, 无需回溯历史会话)。
> **规划权威**: `ES-PLAN.md` (12 域 v1: 7🔴 + 5🟡, 含 09 怀疑审计表 — 8 修正/12 接受)。
> **方法论权威**: `talk-method/source-code-analysis/methodology/zh/` (01-09; **07 五维度全量审查必读** — 每域收官轮用)。
> **给新 AI**: 阅读顺序 §零~§一 → §二~§十三 (各域机制速查) → §十四 (REVIEW 教训与铁律) → §十五 (知识网络) → 收尾工作 (Obsidian 转换等全局待办)。

---

## §零 当前状态速查 (2026-08-14, **12/12 域交付**)

| 域 | 目录 | 类型 | 状态 | 大纲 | harness | 问 | 深审 |
|:--:|---|:--:|:--:|:--:|:--:|:--:|:--:|
| E-3 Translog | outlines/e3-translog | 🔴 A | ✅ | 3 篇 | 21/21 | 32 | 两轮 (25+5) |
| E-7 Mapping | outlines/e7-mapping | 🔴 A | ✅ | 3 篇 | 21/21 | 28 | 两轮 (2+4) |
| E-1 Index Engine | outlines/e1-engine | 🔴 A | ✅ | 3 篇 | 15/15 | 25 | 两轮 (7+10) |
| E-6 SeqNo | outlines/e6-seqno | 🔴 A | ✅ | 3 篇 | 16/16 | 24 | 两轮 (2+5) |
| E-5 Shard | outlines/e5-shard | 🔴 A | ✅ | 3 篇 | 16/16 | 24 | 两轮 (1+8) |
| E-11 FieldData | outlines/e11-fielddata | 🟡 B | ✅ | 2 篇 | — | 18 | 两轮 (3+3) |
| E-2 Search | outlines/e2-search | 🔴 A | ✅ | 3 篇 | 12/12 | 18 | 两轮 (1+7) |
| E-12 Aggregations | outlines/e12-aggregations | 🔴 A | ✅ | 3 篇 | 13/13 | 18 | 两轮 (1+2) |
| E-4 Cluster Routing | outlines/e4-routing | 🔴 A | ✅ | 3 篇 | 13/13 | 18 | 两轮 (1+2) |
| E-8 Merge Policy | outlines/e8-merge | 🟡 B | ✅ | 2 篇 | — | 18 | 两轮 (3+1) |
| E-9 Bulk | outlines/e9-bulk | 🟡 B | ✅ | 2 篇 | — | 18 | 两轮 (0+0) |
| E-10 ClusterState | outlines/e10-clusterstate | 🟡 B | ✅ | 2 篇 | — | 18 | 两轮 (0+0) |

**执行序**: E-3 → E-7 → E-1 → E-6 → E-5 → E-11 → E-2 → E-12 → E-4 → E-8 → E-9 → E-10 (全部完成)

**📊 项目总量**: **12 域交付** (7🔴 + 5🟡) | **32 篇大纲** / **96 闭环** (29 文件) / **12 份 completeness (259 问)** / **8 个 harness (127/127 PASS)** / **168 文件** / 全域裸行号零残留

---

## §一 方法论铁律 (ES 阶段实战验证, 新 AI 必读)

1. **09 怀疑审计每域必走**: ES-PLAN §〇 抓 8 项修正 (聚合 516 文件被淘汰/flush 30min→1min/FieldData 67→103/拓扑反排 E-7 等)
2. **行号必须 awk/sed 验证**: 每行号独立 `(File.java:line)` 格式; 域完成跑 `grep -rnE '\(L[0-9]+' *.md | grep -v java:`
3. **跨域引用只引真实存在域**: 引用前 `[ -d ]` 核验; 路径层级必须算对 (e7-mapping 在 es/outlines/ 下需 ../../../)
4. **双链四行格式**: 每篇 header 必须 `前置/复用/对照/引出`; 对照声明必须"同词+一句摘要"
5. **逆向承接**: 阶段2 s88-boot-elasticsearch 明写 "连接/协议深入在阶段3 ES" — ES 大纲显式回应 (E-1/E-4/E-10)
6. **harness 实证**: 🔴 域强制 (8 个 harness 127/127) — 每个都抓到过自身实现缺陷
7. **禁止引未分析域**: 未产出域不得引用; 收官轮重扫补链
8. **编号漂移警惕**: 引用一律真实目录名 (outlines/e3-translog 而非 E-3)
9. **裸行号根治**: 写后立即扫描 + 修正文必须同步锚点清单
10. **六层深审必真找问题**: 每域两轮深审, 零发现=不合格

**时空溯源注记**: 2026-08-14 已 `git fetch --unshallow` + `--tags` — **509 tags (v0.4.0~v9.x) + 74277 commits**; 关键断代锚点: 6.0 (soft deletes 2018-04-19)/7.0 (ReplicationTracker)/8.0 (RELOCATED 移除 2018-03-28)。

---

## §二 E-3 Translog (✅ 🔴 A, 3 篇 + harness 21/21)

**目录**: outlines/e3-translog/ | 核心文件: Translog.java:575,657,1628 + TranslogWriter.java:227,465 + Checkpoint.java + TranslogDeletionPolicy

| 机制 | 源码锚点 |
|:--|:--|
| 双缓冲写路径 | add 只写 buffer (TranslogWriter.java:227-267) → syncUpTo 批量落盘+force (L465-534, force L508) — 1MB buffer/4MB 阻塞 (TranslogConfig.java:27) |
| durability 双模式 | REQUEST (IndexSettings.java:99-104, 每请求持久化) vs ASYNC (sync_interval 5s, L87) |
| checkpoint 水位 | 8 字段单盘块 <512B 原子写 (Checkpoint.java:35-42,228-229) — 崩溃恢复按水位回放 |
| generation 轮转 | rollGeneration (Translog.java:1628-1652): 封存→copyCheckpointTo→新代; 空代跳过 (L1630); 64MB 阈值 (IndexSettings.java:385) |
| 删除双约束 | getMinReferencedGen (L1710-1721): 引用计数 (TranslogDeletionPolicy.java:66) + safeCommit 水位 |
| tragedy 机制 | 任何 IO 异常 → 首因冻结 → 整体关闭 (TragicExceptionHolder + Translog.java:869) |
| 时空溯源 | v0.90 TranslogService 定时轮询 → v5.0 同步决策 (75e816400c2 2015-09-23) → v8.12 双缓冲+trim |

**harness**: MiniTranslog 21/21 (双缓冲/断电恢复/半写截断/轮转/tragedy — 抓 3 处自身缺陷)
**REVIEW 教训**: 行号偏差 33 处 (凭记忆) → 根治: 全锚点 grep 定位; 时空溯源编造 1 处 (TranslogDeletionPolicy 实为 6.x 引入)

---

## §三 E-7 Mapping (✅ 🔴 A, 3 篇 + harness 21/21)

**目录**: outlines/e7-mapping/ | 核心文件: FieldMapper.java:57,595,1168 + MapperService.java:52,376 + DocumentParser.java:77,263 + DynamicFieldsBuilder.java:47-152

| 机制 | 源码锚点 |
|:--|:--|
| 三级类型体系 | Mapper (L23) / FieldMapper (L57, parseCreateField 抽象 L250) / ObjectMapper (L35) / MetadataFieldMapper |
| Parameter 声明式 | 五元组 (FieldMapper.java:595-660): name/default/parser/serializer + mergeValidator (L641: updateable?覆盖:相等) |
| 合并冲突 | Conflicts 聚合 (L1168-1186) + checkIncomingMergeType (L402-412 类型不可改) |
| 解析五步 | DocumentParser.parseDocument (L77) → internalParseDocument (L131): metadata preParse→parseObjectOrNested (L263)→postParse |
| parseCreateField 四变体 | Text (L1243) / Keyword ignoreAbove (L898) / Number coerce (L1830) / Date 三路索引 (L924-937) |
| coerce 遮蔽坑 | "index.mapping.coerce" 三处定义 — FieldMapper=false (L67) / Number=true (L84) / Range=true (L59); Builder (L136) 静态遮蔽用本类默认 |
| dynamic 四态 | TRUE/FALSE/STRICT/RUNTIME (ObjectMapper.java:45-58) — 09 审计"三态"遗漏 RUNTIME |
| 类型推断 | Long→Double→Date→String (DynamicFieldsBuilder.java:51-91); 纯数字拒绝 date (L75-78) |
| 时空溯源 | v0.90 手写 Builder → 2020-08-12 参数化 (c81dc2b8b7d) → 2020-11-02 合并 (a5168572d5b) |

**harness**: MiniMapping 21/21 (推断/四态/四变体/兜底链)
**REVIEW 教训**: 行数≠行号 (ObjectMapper L717 是 wc -l 输出被当类行号, 实为 L35) → 上限检查; dynamic 四态修正规划"三态"

---

## §四 E-1 Index Engine (✅ 🔴 A, 3 篇 + harness 15/15)

**目录**: outlines/e1-engine/ | 核心文件: InternalEngine.java:1131,1314,1384,1483 + LiveVersionMap.java:112-175 + Engine.java

| 机制 | 源码锚点 |
|:--|:--|
| 计划-执行两阶段 | indexingStrategyForOperation (L1305) → planIndexingAsPrimary 五分支 (L1314-1383: append/冲突×3/超限/正常) → 执行按 plan 分派 (L1169-1225) |
| append-only 幂等 | canOptimizeAddDocument (L1059-1090) + maxUnsafeAutoIdTimestamp 水位 (注释 L1143-1173) — 重试降级 updateDocument |
| indexIntoLucene 三分派 | addStaleDocs (L1472) / updateDocs (L1584) / addDocs (L1463) — 定义行 |
| LiveVersionMap 双 map | Maps(current, old) (L112-175): beforeRefresh 建 transition (L149) / afterRefresh 清 old (L167) — volatile 引用无锁读 |
| NRT 三级可见性 | realtime get (L865-924: 版本表+translog) / refresh 1s (L2022, IndexSettings:279) / flush (L2173: rollGeneration L2212 + trim L2225) |
| 失败分级 | 主分片文档失败单请求 (L1428) vs 副本 tragic (L1435-1443) — 不对称根因 (L1438-1441) |
| 三级并发锁 | readLock (L1134) + uid 锁 (L1139) + throttle (L1140) |
| 时空溯源 | v0.90 RobinEngine 单 map 876 行 → v8.12 双 map + 计划-执行 (8247e4beaee 2014-01-13 更名) |

**harness**: MiniEngine 15/15 (计划-执行/append-only 幂等/NRT/并发)
**REVIEW 教训**: 调用点 vs 定义行 (addDocs L1410 调用 vs L1463 定义); 批量 sed 默认文件选错 → CollectorManager 行号全标错 (E-2 教训源头)

---

## §五 E-6 SeqNo (✅ 🔴 A, 3 篇 + harness 16/16)

**目录**: outlines/e6-seqno/ | 核心文件: LocalCheckpointTracker.java:83-127,191-218 + ReplicationTracker.java:147,1349-1370 + ReplicationOperation.java:107-280 + SequenceNumbers.java:23-32

| 机制 | 源码锚点 |
|:--|:--|
| seqNo 分配 | generateSeqNo (L83-85): nextSeqNo.getAndIncrement; advanceMaxSeqNo (L90-92) |
| 双 checkpoint | processed (L99, 复制可用) vs persisted (L108, translog fsync 推进 — InternalEngine.java:1243-1248,255) |
| 乱序水位推进 | markSeqNo (L112-127) 位图乱序标记 + updateCheckpoint (L191-218) 连续前缀跳跃; BIT_SET_SIZE=1024 (L25) |
| globalCheckpoint | computeGlobalCheckpoint (L1349-1370): min(in-sync localCheckpoint); pendingInSync/UNASSIGNED → fallback (L1356-1362) |
| 复制两阶段 | ReplicationOperation.execute (L107-126) → handlePrimaryResult (L129-176) → performOnReplicas (L210) → onFailure 分流 (L251-280: 重试 vs stale) |
| primaryTerm 脑裂 | IndexShard.java:231 (pendingPrimaryTerm) + E-1 term 冲突拒绝 (InternalEngine.java:1358-1367) — fencing token 语义 |
| RetentionLease | 保留 seqNo≥N 历史防 merge 清理 (RetentionLease.java:24-27,31-65; ReplicationTracker.java:302,390,488) |
| 时空溯源 | v2.0 无 seqNo → 2015 三连发 (term 10-21/seqNo 11-19/local checkpoint 12-15) → v6.0 GlobalCheckpointTracker → v7.0 ReplicationTracker |

**harness**: MiniSeqNo 16/16 (分配/双 checkpoint/乱序跳跃/gcp 聚合+回退/并发)
**REVIEW 教训**: 行号上限抓跨文件错误 (LocalCheckpointTracker.java:1247 实为 InternalEngine); 时空溯源 3 commit 日期实证

---

## §六 E-5 Shard (✅ 🔴 A, 3 篇 + harness 16/16)

**目录**: outlines/e5-shard/ | 核心文件: IndexShard.java:493,885,1672 + IndexShardState.java:10-17 + IndexShardOperationPermits.java:49-153 + ReplicationGroup.java:23-80

| 机制 | 源码锚点 |
|:--|:--|
| 5 态状态机 | CREATED/RECOVERING/POST_RECOVERY/STARTED/CLOSED (IndexShardState.java:12-17); RELOCATED 已移除 (IDS[4]=STARTED 兼容 L25-26) |
| 双轨驱动 | 集群 updateShardState (L493: POST_RECOVERY→STARTED L529-536) + 本地 recoverFromStore (L2370→RECOVERING L744) / postRecovery (L1704→POST_RECOVERY L1723) |
| permits 双模式 | Semaphore MAX_VALUE (L49-50): 正常单 permit (L259-260) / blockOperations 全占 (L82-153: delay→wait→acquireAll L145) — 超时抛错 L152 |
| 关闭顺序 | close (L1672-1690): CLOSED (L1676) → flushAndClose (L1683) → IOUtils.close (L1688) → permits.close (L1689) |
| 主升四步 | term+1 (L576 断言) + blockOperations + resync (L609,748) + seqNo 补洞 (testPrimaryFillsSeqNoGaps L592) |
| ReplicationGroup | inSync/tracked (L24-25) → 派生 targets/skipped (L43-80) — 不可变快照 (E-6 消费 L136) |
| 时空溯源 | v0.90 InternalIndexShard 876 行接口分离 → v5.0 6 态 1562 行 → v8.12 5 态 4242 行 (RELOCATED 移除 848c7e4917c 2018-03-28) |

**harness**: MiniShard 16/16 (状态机/permits 双模式/主升/旧主拒写 — 抓 1 测试断言缺陷: Semaphore tryAcquire 返回 false 非抛)
**REVIEW 教训**: 状态驱动双轨是分片状态机核心; 批量替换漏"文件名在前的括号内"格式

---

## §七 E-11 FieldData (✅ 🟡 B, 2 篇)

**目录**: outlines/e11-fielddata/ | 核心文件: IndexFieldData.java:59,250 + GlobalOrdinalsBuilder.java:39-90 + HierarchyCircuitBreakerService.java:81-92 + TextFieldMapper.java:249

| 机制 | 源码锚点 |
|:--|:--|
| load vs loadGlobal | 段内 (L59) vs 跨段 global ordinals (L250-252) — terms 聚合需要全局去重 |
| GlobalOrdinalsBuilder | 逐段 load (L51-53) → FilterTermsEnum 64K 检查断路器 (L65) → OrdinalMap.build (L72) → ramBytesUsed (L73) → addWithoutBreaking (L74) |
| 断路器 | FIELDDATA limit 40% heap (L83) + overhead 1.03 (L89) + addEstimateBytesAndMaybeBreak (CircuitBreaker.java:84) — 超限 CircuitBreakingException |
| 5.0 迁移史 | fielddata (堆内倒排) → docValues (磁盘列式); 现代 text fielddata 默认 false (TextFieldMapper.java:249) — OOM 事故经典题 |
| 排序面 | fieldcomparator 4 源: LongValuesComparatorSource (L40-86) 从 docValues 列式读 |
| 缓存 | IndexFieldDataCache clear (L30) / clear by field (L35) — 与 refresh 无关 (docValues 不可变) |

**REVIEW 教训**: 子目录路径 (ordinals/GlobalOrdinalsBuilder) 上限检查误报 → 路径必须含子目录; 🟡 B 无 harness/时空溯源

---

## §八 E-2 Search (✅ 🔴 A, 3 篇 + harness 12/12)

**目录**: outlines/e2-search/ | 核心文件: QueryPhase.java:61,115,133,251 + QueryPhaseCollectorManager.java:77,111-180 + SearchPhaseController.java:185-229 + SimilarityProviders.java:255-262

| 机制 | 源码锚点 |
|:--|:--|
| CollectorManager | QueryPhaseCollectorManager (L77): newCollector 每段 (L111-146: topDocs+聚合组合) + reduce 跨段 (L147-180) — 一次遍历喂查询+聚合 |
| hit count 快路径 | trackTotalHitsUpTo (L233-240): 计数模式 (L399); postFilter/minScore 禁用 (QueryPhaseTests L229,276) |
| BM25 | k1=1.2 (L258) / b=0.75 (L259) — 词频饱和+长度归一化; Legacy boost*(1+k1) (LegacyBM25Similarity.java:67) |
| Fetch 按需加载 | StoredFieldsSpec.build (L110) → StoredFieldLoader.fromSpec (L113) — 子阶段声明需求合并一次加载 |
| 协调归并 | sortDocs (L185-229) + getLastEmittedDocPerShard (L319) — 协调不重新打分; 深分页 from+size 放大 |
| DFS 三阶段 | DfsPhase (L53-62): 全局词频收集 (L152-154) → IDF 精确 — 多一轮 RTT |
| 超时取消 | getTimeoutCheck (L251-269) + queryCancellation (L202) + allowPartialSearchResults (L212) |
| 时空溯源 | v0.90 QueryPhase 148 行内联四分支 → v8.12 CollectorManager 701 行组合 |

**harness**: MiniSearch 12/12 (BM25 公式/分片查询/两阶段/深分页)
**REVIEW 教训**: 混合文件批量替换默认文件选错 → CollectorManager 行号全标到 QueryPhase.java (上限检查抓出); 测试行号不得标到主类

---

## §九 E-12 Aggregations (✅ 🔴 A, 3 篇 + harness 13/13)

**目录**: outlines/e12-aggregations/ | 核心文件: AggregationPhase.java:27-39 + AggregatorBase.java:35,219-232 + GlobalOrdinalsStringTermsAggregator.java:55-139 + MultiBucketConsumerService.java:32-60

| 机制 | 源码锚点 |
|:--|:--|
| 挂载 | AggregationPhase.preProcess (L27-39) — E-2 QueryPhase L133 调用, 查询+聚合共享遍历 |
| 递归组合 | getLeafCollector 模板 (AggregatorBase.java:219-232): 子聚合委托链 (L221) + 父包装 (L222); postCollection (L294-298) |
| 桶上限 | DEFAULT_MAX_BUCKETS=65536 (MultiBucketConsumerService.java:32) + TooManyBucketsException (L55-60) |
| terms 加速 | global ordinals 编号计数 (GlobalOrdinalsStringTermsAggregator.java:127-139: advanceExact L128/ordValue L131/collectGlobalOrd L132) — E-11 两域闭环 |
| 深度/广度优先 | BestBucketsDeferringCollector (L37-42): 第一遍全收集+选 top+第二遍重放 |
| composite | 多字段组合键 (L72-80) + after_key 分页 (L203-216) + compareCurrent (L598) |
| reduce 两阶段 | ForPartial/ForFinal (AggregationReduceContext.java:23,77-81) — pipeline 只最终 |
| pipeline | 消费桶结果 (PipelineAggregator.java:117) — 不参与文档收集 |
| cardinality HLL | HyperLogLogPlusPlus 可配 precision (CardinalityAggregator.java:48,71) — Redis PFADD 同源 |
| 时空溯源 | v0.90 facet 109 文件 → 2013-11-24 aggregations (c7f6c5266d1) → 2014-08-21 facet 移除 (ea96359d82a) → v8.12 516 文件 |

**harness**: MiniAggregations 13/13 (递归组合/桶计数/上限保护)
**REVIEW 教训**: 09 审计新增本域被验证正确 (516 文件定义特征级); 方法体内行号 (L128/131/132) 偏差 5 行

---

## §十 E-4 Cluster Routing (✅ 🔴 A, 3 篇 + harness 13/13)

**目录**: outlines/e4-routing/ | 核心文件: RoutingTable.java:45-60 + ShardRouting.java:447-600 + OperationRouting.java:36-240 + AllocationDecider.java:32-91 + BalancedShardsAllocator.java:65-156

| 机制 | 源码锚点 |
|:--|:--|
| 三级结构 | RoutingTable (L45) → IndexRoutingTable → IndexShardRoutingTable (L45-79: primary L50 + replicas L51) → ShardRouting |
| 版本化 | withIncrementedVersion (L59-60) — MasterService.java:508 发布前递增, 节点按版本判新旧 |
| 两层状态 | 路由态 4 态 (ShardRoutingState.java:15-32) vs 分片态 5 态 (E-5) — 决策 vs 执行解耦 |
| 读路由 | getShards (L63-77) → preferenceActiveShardIterator (L206-240: _only_nodes/_local/_shards, Preference.parse L218) + 自适应副本 (L39) |
| 19 决策器 | AllocationDecider 5 类判定 (L32-91) — 任一 NO 拒绝, 全 YES 允许, THROTTLE 限速 |
| 平衡器 | BalancedShardsAllocator: shard/index/threshold 3 参数 (L84-112) + 阈值≥1 (L155-156) |
| 分配时序 | reroute (AllocationService.java:388-410) → 主先副后 (L541-553) |
| 时空溯源 | v0.90 Mutable/Immutable 分离 → 2013-01-17 平衡器 (2eb09e6b1ab) → 2015-06-24 ShardRouting 合并 (c57951780e0) |

**harness**: MiniRouting 13/13 (版本化/决策器投票/读写路由/版本判新旧)
**REVIEW 教训**: Preference.parse 在 preferenceActiveShardIterator 内 (L218) 非 getShards (L214) — 方法归属精确; 读写路由不对称 (写一致读均衡)

---

## §十一 E-8 Merge Policy (✅ 🟡 B, 2 篇)

**目录**: outlines/e8-merge/ | 核心文件: MergePolicyConfig.java:104-380 + MergeSchedulerConfig.java:45-72 + ElasticsearchConcurrentMergeScheduler.java:95,211 + CombinedDeletionPolicy.java:34-57

| 机制 | 源码锚点 |
|:--|:--|
| 分层合并 | TieredMergePolicy (L105): 按大小分层, 大段少合并防写放大 |
| 5 参数 | maxMergeAtOnce=10 (L119) / segmentsPerTier=10.0 (L141) / mergeFactor=32 (L149, **仅 timeBased 用, Tiered 忽略 L327**) / deletesPct=20% (L150); 约束 maxMergeAtOnce≤segmentsPerTier (L367-380) |
| 删除清理 | setDeletesPctAllowed (L303→L362-363): 超 20% 才合并物理清理 (soft deletes 保留) |
| 调度限制 | maxThreadCount + maxMergeCount=线程+5 (MergeSchedulerConfig.java:54) — 防合并风暴 |
| forceMerge | 三分支 (InternalEngine.java:2405/2407/2409) + forceMergeUUID (L2410) — 冷索引优化阻塞合并 |
| 租约保护 | CombinedDeletionPolicy (L34-40): commit 保留到 globalCheckpoint + safeCommit (L57) — 合并只清超保留点 |

**REVIEW 教训**: mergeFactor 只作用于 timeBasedMergePolicy 是面试易错点; 三层保护 (deletesPctAllowed+safeCommit+RetentionLease) 是合并边界核心

---

## §十二 E-9 Bulk (✅ 🟡 B, 2 篇)

**目录**: outlines/e9-bulk/ | 核心文件: TransportBulkAction.java:605-690 + TransportShardBulkAction.java:223-385,584-661 + BulkProcessor.java:44-138 + BackoffPolicy.java:34-79

| 机制 | 源码锚点 |
|:--|:--|
| 按 shard 分组 | requestsByShard (L605) → route (L647) → computeIfAbsent (L648) → BulkShardRequest (L675) — 同分片一次往返 |
| 主循环逐条 | while hasMoreOperationsToExecute (L223) → executeBulkItemRequest (L224); 失败隔离 markOperationAsExecuted (L320) — 非事务 |
| 映射等待 | MAPPING_UPDATE_REQUIRED (L370) → mapperService.merge (L373-377, E-7) → break (L233-235) → 重试 |
| 批量优势 | 省 RTT (分组) + 共享 translog fsync (E-3 双缓冲) + 同分片省锁 |
| 客户端背压 | BulkProcessor 三参数 (L112-130) + exponentialBackoff 50ms×8 (BackoffPolicy.java:66-67) |
| 失败语义 | BulkResponse item 独立 (L31) + hasFailures (L88) — partial success |
| 复制面 | TransportWriteAction (L74) 整体复制; 无 seqNo/noop 跳过 (L584-601); RetryOnReplica (L661) |

**REVIEW 教训**: 本域写后即验成效显著 — 0 行号偏差 (三遍验证闭环内化); bulk vs MULTI/EXEC (原子 vs 部分成功) 核心对照

---

## §十三 E-10 ClusterState (✅ 🟡 B, 2 篇)

**目录**: outlines/e10-clusterstate/ | 核心文件: ClusterState.java:110,156-179,897-899 + CoordinationState.java:32,168-294,533-586 + Coordinator.java:108,1498-1586 + Publication.java:30,252-302 + Metadata.java:99,326-353 + ElectionStrategy.java:20-68 + PublicationTransportHandler.java:70,126-209,340-370 + ClusterBootstrapService.java:46,105-106

| 机制 | 源码锚点 |
|:--|:--|
| 三层结构 | ClusterState = metadata (L175) + routingTable (L166) + blocks (L177) + nodes (L168) + customs (L179); version 单调递增 (incrementVersion L897-899) — 不可变容器 |
| 版本化 | MasterService.patchVersions (L503-520): 仅 master 控制版本 (L505); routingTable/metadata 引用变化各自递增 (L507-511) + Metadata.withIncrementedVersion (L326-353) |
| Raft 风格选举 | CoordinationState 纯函数状态机 (TLA+ 模型 L28-31): term 严格递增 (handleStartJoin L168-194) → join 三重校验 (handleJoin L219-264) → 双配置 quorum (ElectionStrategy L40-60, `n*2>N` CoordinationMetadata.java:347-353) |
| 持久化两字段 | PersistedState (CoordinationState.java:533-557): currentTerm + lastAcceptedState — term 防旧主复活 (fencing) / lastAccepted 保证接续发布 |
| 两阶段发布 | master 计算 (MasterService L230-233) → PublishRequest 广播 (Publication L252) → 接受+持久化 (CoordinationState L370-402) → 双配置投票 (L413-452) → ApplyCommitRequest (Publication L284) → handleApplyCommit (Coordinator L398-416) → ClusterApplierService apply (L306-539); 主节点发布收尾才 apply (L406-408) |
| 判新旧 | term+version 双门槛 (CoordinationState L382-391 拒绝旧, L428-437 丢弃乱序响应); diff 不兼容回退 full (PublicationTransportHandler L154-157,186-188) |
| diff 传输 | full/diff 双模式 (PublicationTransportHandler L340-370): 新节点 full (L362) / 已知节点 diff (L364-367) — 按 TransportVersion 序列化 (L362) |
| 时空溯源 | Zen2 2018-07-20 (384cc5455b8 "Add core coordination algorithm" #32171) → v8.12; 🟡 B 无 harness |

**REVIEW 教训**: 裸锚点 103 处根治 (行级 13 + 混合行内 90) — 混合行内有 java: 锚点也要逐锚点加文件前缀 (E-9 标准); 三遍验证闭环延续零偏差 (R1/R2 均 0 内容性偏差); 收官轮 07 五维度 (REVIEW-3): R2-R5 四轮收敛 + 内容深度轮抓 1 语义偏差 (term 超越 ≠ L1546, 实为 updateMaxTermSeen L506-517) + 1 锚点一致性 (大纲锚点须源自 pass2, E-9 模式)

---

## §十四 REVIEW 教训与铁律汇总 (ES 阶段最痛的点)

### 1. 裸行号 — 全域根治 (每域都有)

- 12 域每域 REVIEW 都先抓裸行号 `(Lxxx)` — 根治: 写完立即 `grep -rnE '\(L[0-9]+' *.md | grep -v java:` 扫描
- **修正文必须同步锚点清单** (E-4/E-12 教训: 正文修了清单漏)
- 混合格式 `(Lxxx, File.java)` 合法; 多行号 `(L1352,1358,1368)` 正则易漏
- **E-10 新增铁律: 混合行内裸锚点也必须根治** — 行内有 `File.java:` 但仍有 `(Lxxx)` 的不算干净 (103 处案例)

### 2. 行号上限检查 (wc -l) — 终检防线

- 每文件锚点必须 ≤ 文件实际行数 — 抓出: ObjectMapper L717→实为 L35 (行数当行号)、LocalCheckpointTracker.java:1247 实为 InternalEngine、QueryPhase.java:276 实为 QueryPhaseTests (测试标主类)、CollectorManager 批量标错
- **子目录文件路径必须含子目录** (ordinals/GlobalOrdinalsBuilder)

### 3. 批量 sed 的默认文件陷阱 (E-2 最痛)

- 混合文件 (一个 md 含多个类行号) 用单一默认文件替换 → 系统性标错
- **根治**: 按方法名归属精确标注, 不用单一默认文件

### 4. 方法体内行号必须 grep (不能偏移推算)

- E-1 addDocs (L1410 调用 vs L1463 定义)、E-12 terms collect (L128/131/132 偏差 5)、E-8 forceMerge (L2405-2410 偏差 1)
- 根治: 方法体内每个关键行 grep 关键词定位

### 5. 调用点 vs 定义行

- updateGlobalCheckpointOnPrimary (定义 L1375 vs 调用 L1098)、awaitClose (主方法 L354 vs 内部 L335)

### 6. 时空溯源断代必须 commit 日期实证

- TranslogDeletionPolicy 实为 6.x (1775e4253eb 2017-06-01) 非 v5.0 — 编造案例
- 每个断代 commit 必须 `git log -1 --format="%ci %s" <hash>`

### 7. 三遍验证闭环 (E-9 达到零偏差)

```
写时 grep → 自查 (方法起始+方法体内) → REVIEW-2 逐锚点复核
R1 抓: E-3 25 / E-7 2 / E-1 7 / E-6 2 / E-5 1 / E-11 3 / E-2 1 / E-12 1 / E-4 1 / E-8 3 / E-9 0
R2 抓: E-3 5 / E-7 4 / E-1 10 / E-6 5 / E-5 8 / E-11 3 / E-2 7 / E-12 2 / E-4 2 / E-8 1 / E-9 0
R2 收敛趋势: E-1 抓 10 → E-6 抓 5 → E-5 抓 8 → E-11 抓 3 → E-2 抓 7 → E-12 抓 2 → E-4 抓 2 → E-8 抓 1 → E-9 抓 0
```

### 8. 09 审计验证

- ES-PLAN 8 修正全部正确: 聚合 516 文件定义特征级 (E-12 已产出 3 篇+harness 验证)、dynamic 四态 (E-7 修正三态)、flush 1min (E-3)、FieldData 103 (E-11)

---

## §十五 知识网络图 (全域双链)

### 全域图谱 (Obsidian 双链, 全部真实存在)

```
E-3 Translog (叶子: WAL) ──前置──→ E-1 Engine
E-7 Mapping (叶子: 字段类型) ──前置──→ E-1/E-2/E-11
E-1 Engine (Hub: 写入核心) ──前置──→ E-6/E-5/E-9
E-6 SeqNo (复制协议) ──前置──→ E-5/E-4
E-5 Shard (分片生命周期) ──前置──→ E-4/E-9/E-10
E-11 FieldData (docValues 面) ──前置──→ E-2/E-12
E-2 Search (查询路径) ──挂载──→ E-12 (聚合同遍历)
E-12 Aggregations (聚合) ──消费──→ E-11 (global ordinals)
E-4 Cluster Routing (路由) ──前置──→ E-10
E-8 Merge (段合并) ──依赖──→ E-1/E-6 (租约)
E-9 Bulk (批量) ──消费──→ E-1/E-5/E-6
E-10 ClusterState (收束) ──驱动──→ 全部
```

### 跨仓库引用 (真实目录, 已核验)

| 来源 | 关系 |
|:--|:--|
| redis/outlines/r8-persistence | 对照 E-3 (AOF vs translog) / E-1 (RDB vs commit) / E-8 |
| redis/outlines/r9-replication | 对照 E-6 (字节偏移 vs seqNo) |
| redis/outlines/r21-db | 对照 E-7 (无模式 vs 强模式) / E-2 / E-4 |
| redis/outlines/r22-expire / r23-evict | 对照 E-5 / E-11 / E-8 (LRU vs 合并) |
| redis/outlines/r24-string / r28-networking / r29-pubsub / r12-hll / r14-sentinel / r16-multi | 对照 E-2 / E-4 / E-12 (HLL) / E-5 (failover) / E-9 (MULTI) |
| redisson/outlines/rd1-connection | 对照 E-4 (连接池 vs 路由) / E-9 |
| redisson/outlines/rd2-rlock | 对照 E-6 (fencing token) |
| redisson/outlines/rd3-codec / rd4-command / rd5-rmap / rd6-localcachedmap | 对照 E-7 / E-2 / E-12 / E-11 |
| spring/outlines/s88-boot-elasticsearch | **承接** (s88 "连接/协议深入在阶段3 ES" — E-1/E-4/E-10 回应) |

---

## §十六 完成检查单

- [x] 阶段3.7 ES 12/12 域交付 (E-3/E-7/E-1/E-6/E-5/E-11/E-2/E-12/E-4/E-8/E-9/**E-10**)
- [x] 32 篇大纲 + 96 闭环 + 259 问 + 8 harness (127/127) + 168 文件
- [x] 每域两轮深审 (六层) + REVIEW 教训汇总 (§十四)
- [x] 全域一致性: 裸行号零残留 / 行号上限检查 / 知识网络成环
- [x] ES-PLAN v1 (09 审计 8 修正全部验证) + HANDOFF-STAGE3 §零 同步
- [x] **E-10 ClusterState (最后 1 域, 🟡 B)** — 已交付 (§十三)
- [ ] 全域 Obsidian vault 转换 (跨域双链图谱, 全局待办)
