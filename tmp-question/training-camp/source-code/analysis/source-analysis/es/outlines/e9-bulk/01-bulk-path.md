# E-9 Bulk 篇 1/2 — 批量路径: 分组/执行/映射等待

> 前置: [[E-1-engine-01]] (Engine 写入) [[E-7-mapping-03]] (动态映射) | 复用: — | 对照: [[rd4-command]] (命令批) | 引出: [[E-9-bulk-02]]
> 🟡 B | 来源: TransportBulkAction.java:605-690 + TransportShardBulkAction.java:223-260,360-385 + BulkRequestParser.java:46
> 定位: Bulk 卷开篇 — 回答"bulk 怎么分组? 分片内怎么执行?"

**读者处境**: 面试官问 "ES bulk 写入怎么工作? 为什么比单条快?" 你答 "批量" — 但再问 "怎么按分片分组? 动态映射冲突怎么处理?" 你答不上来。这篇是批量路径的完整答案。

### 1. 问题引入 — 一次 bulk 的旅程

场景: 1000 条操作一次 bulk — 服务端怎么处理?
- 两阶段: 协调分组 (TransportBulkAction L605) → 分片执行 (TransportShardBulkAction L223)
- 本篇问题: 分组 (Q1) / 主循环 (Q2) / 映射等待 (Q3) / 性能 (Q4)

### 2. 按 shard 分组 — 同分片合并

场景: 1000 条怎么变成分片请求?
- requestsByShard (TransportBulkAction.java:605-649): shardId = route (TransportBulkAction.java:647) → computeIfAbsent (TransportBulkAction.java:648)
- 分派: 逐组 → BulkShardRequest (TransportBulkAction.java:675-677)
- 分组键: ShardId (index+shardId) — 同分片一次往返

### 3. 主循环执行 — 逐条 + 失败隔离

场景: 分片内怎么执行?
- while hasMoreOperationsToExecute (TransportShardBulkAction.java:223) → executeBulkItemRequest (TransportShardBulkAction.java:224)
- 失败隔离: markOperationAsExecuted (TransportShardBulkAction.java:320) — 单条失败继续
- 语义: 批量 = 多条独立, 非事务 (partial success)

### 4. 映射更新等待 — 动态映射重试

场景: 新字段触发动态映射怎么办?
- MAPPING_UPDATE_REQUIRED (TransportShardBulkAction.java:370) → mapperService.merge (TransportShardBulkAction.java:373-377, E-7)
- break 主循环 (TransportShardBulkAction.java:233-235) → 映射完成后重新执行
- 语义: 动态映射不阻塞其他操作

### 5. 批量优势 — 三层省

- 省 RTT: 分组 (Q1)
- 共享 fsync: translog 双缓冲批量落盘 (E-3)
- 减少锁竞争: 同分片连续执行
- 所以 bulk 是写入首选

### 核心悬念
"bulk 1000 条为什么比 1000 次单条快?" — 三层: 分组省 RTT (同分片一次往返) + translog 批量 fsync (E-3 双缓冲) + 同分片连续执行省锁 — 网络/磁盘/CPU 三面都省。

### 概念依赖链
Q1 分组 → Q2 主循环 → Q3 映射等待 → Q4 性能 → (E-1/E-3/E-7 衔接)

### 源码锚点清单
- TransportBulkAction.java:605 (requestsByShard) / 647 (route) / 648-649 (computeIfAbsent) / 675-677 (BulkShardRequest)
- TransportShardBulkAction.java:223 (主循环) / 224 (executeBulkItemRequest) / 233-235 (break 等映射) / 320 (markOperationAsExecuted) / 360-368 (applyIndexOperationOnPrimary) / 370 (MAPPING_UPDATE_REQUIRED) / 373-377 (mapperService.merge)
- BulkRequestParser.java:46 (NDJSON 解析)
