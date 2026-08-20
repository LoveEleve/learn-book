# E-9 Bulk 篇 2/2 — 批量边界: 背压/失败/对照

> 前置: [[E-9-bulk-01]] [[E-6-seqno-02]] (复制) | 复用: — | 对照: [[rd1-connection]] (连接批) [[r16-multi]] (Redis MULTI) | 引出: [[E-10-clusterstate]]
> 🟡 B | 来源: BulkProcessor.java:44-138 + BackoffPolicy.java:34-79 + BulkResponse.java:31,88 + multi.c:11-38 + TransportShardBulkAction.java:74,661
> 定位: Bulk 卷收尾 — 回答"客户端怎么背压? 和 Redis pipeline 差在哪?"

**读者处境**: 面试官问 "生产环境怎么批量写 ES? 批量失败怎么办?" 你答 "BulkProcessor" — 但再问 "bulk 和 Redis pipeline 什么区别? 复制怎么走?" 你答不上来。这篇是批量边界的完整答案, 收束 Bulk 域。

### 1. 问题引入 — 批量之外的四件事

场景: 批量写入要管四件事: 背压/失败/对照/复制
- 客户端背压 (Q5) / 失败语义 (Q6) / Redis 对照 (Q7) / 复制面 (Q8)

### 2. 客户端背压 — BulkProcessor

场景: 生产环境怎么批量写?
- 三参数 (BulkProcessor.java:112-130): setConcurrentRequests (BulkProcessor.java:112) / setBulkActions (BulkProcessor.java:121) / setBulkSize (BulkProcessor.java:130)
- **经验值**: 常见配置 bulkActions=1000 条 / bulkSize=5MB 量级 (可配, 按文档大小调整)
- 退避重试: exponentialBackoff 默认 50ms×8 (BackoffPolicy.java:66-67)
- 优雅关闭: awaitClose (BulkProcessor.java:354)

### 3. 失败语义 — partial success

场景: 批量中一条失败怎么办?
- BulkResponse (BulkResponse.java:31): 每 item 独立结果
- hasFailures (BulkResponse.java:88): 整体标记
- 语义: 非事务, 部分成功正常 — 调用方逐条处理

### 4. 与 Redis pipeline 对照 — 命令批 vs 操作批

场景: bulk 和 pipeline 都"一次发很多", 差在哪?
- Redis: pipeline 客户端批 + MULTI/EXEC 事务 (multi.c:11-38) — 原子
- ES: bulk 服务端分组 + 逐条 (Q1/Q2) — 部分成功
- 对照: 原子性 (MULTI 原子 vs bulk 部分) / 分组 (无 vs 按 shard) / 失败 (命令级 vs item 级)

### 5. 复制面 — BulkShardRequest 整体复制

场景: 批量怎么复制到副本?
- TransportShardBulkAction extends TransportWriteAction (TransportShardBulkAction.java:74) — 复制框架
- 主执行整个 BulkShardRequest → 整体复制 (E-6)
- 特殊: 无 seqNo/noop 跳过复制 (TransportShardBulkAction.java:584-601); RetryOnReplicaException (TransportShardBulkAction.java:661)
- 语义: 批量复制省副本侧 RTT

### 核心悬念
"bulk 和 Redis MULTI/EXEC 都是批量, 为什么一个原子一个不原子?" — 因为场景不同: Redis 是单机内存原子操作, ES 是分布式多分片逐条执行 — 分布式下原子性代价太高, 用部分成功换取吞吐。

### 概念依赖链
Q5 背压 → Q6 失败 → Q7 对照 → Q8 复制 → (E-6 衔接)

### 源码锚点清单
- BulkProcessor.java:44 (类) / 112 (setConcurrentRequests) / 121 (setBulkActions) / 130 (setBulkSize) / 354 (awaitClose)
- BackoffPolicy.java:34-79 / 66-67 (exponentialBackoff 默认)
- BulkResponse.java:31 (item 独立) / 88 (hasFailures)
- multi.c:11-38 (Redis MULTI/EXEC 对照)
- TransportShardBulkAction.java:74 (extends TransportWriteAction) / 584-601 (跳过复制) / 661 (RetryOnReplicaException)
