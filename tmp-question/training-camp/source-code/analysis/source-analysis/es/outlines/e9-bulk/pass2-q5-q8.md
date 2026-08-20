# E-9 闭环笔记 Q5-Q8: 背压/失败语义/Redis 对照/复制

## Q5: BulkProcessor — 客户端背压

假设: 客户端批量封装: 按操作数/大小/并发数触发批量, 失败退避重试。

验证过程:
- Read BulkProcessor (BulkProcessor.java:44-138): Builder 三参数 — setConcurrentRequests (BulkProcessor.java:112) / setBulkActions (BulkProcessor.java:121) / setBulkSize (BulkProcessor.java:130)
- BackoffPolicy (BackoffPolicy.java:34-79): constantBackoff (BackoffPolicy.java:55) / exponentialBackoff 默认 50ms×8 (BackoffPolicy.java:66-67) — 重试退避
- awaitClose (BulkProcessor.java:354-368): 优雅关闭 (等剩余批量完成)
- 语义: 客户端背压 = 按量触发 (actions/size) + 并发限制 + 退避重试

代码类型: Implementation (客户端封装)

结论: **BulkProcessor = 客户端批量三参数 (并发数 L112 / 操作数 L121 / 大小 L130) + 退避重试 (exponentialBackoff 50ms×8 L66-67) + 优雅关闭 (L354) — 生产环境写入的标准封装**。BulkProcessor.java:44-138 + BackoffPolicy.java:34-79

## Q6: 失败语义 — partial success

假设: 批量中单条失败不影响其他 — 响应标记 hasFailures。

验证过程:
- Read BulkResponse (BulkResponse.java:31-139): 每 item 独立结果 (BulkResponse.java:31) + hasFailures (BulkResponse.java:88)
- 语义: 批量 = 多条独立操作 (Q2), 部分失败正常 — 调用方按 item 处理
- 与事务区别: 无原子性 (非 MULTI/EXEC)

代码类型: Interface (响应契约)

结论: **批量响应按 item 独立 (BulkResponse L31), hasFailures 标记整体 (BulkResponse.java:88) — 部分成功部分失败, 调用方逐条处理; 非事务语义**。BulkResponse.java:31,88

## Q7: 与 Redis pipeline 对照 — 命令批 vs 操作批

假设: Redis pipeline (命令批) 与 ES bulk (操作批) 都是"一次往返多操作", 但语义不同。

验证过程:
- Redis: pipeline = 客户端命令队列一次发送 (multi.c MULTI/EXEC 是事务, pipeline 是批); EXEC 原子
- ES: bulk = 服务端按 shard 分组逐条执行 (Q1/Q2), 非原子
- 对照维度:
  - 原子性: Redis MULTI/EXEC 原子 / ES bulk 部分成功
  - 分组: Redis 无分组 (单实例) / ES 按 shard
  - 失败: Redis 命令级 / ES item 级
- 面试记忆点: "pipeline 是'一次发很多', bulk 是'一次处理很多'"

代码类型: 对照分析

结论: **Redis pipeline 客户端批 (一次往返) vs ES bulk 服务端批 (分组+逐条): 原子性不同 (MULTI/EXEC 原子 vs bulk 部分成功), 分组不同 (无 vs 按 shard)**。对照锚点: multi.c:11-38 + TransportBulkAction.java:605

## Q8: 复制面 — BulkShardRequest 走复制

假设: BulkShardRequest 作为复制单元, 主执行后整个批量请求复制到副本。

验证过程:
- Read TransportShardBulkAction (TransportShardBulkAction.java:74): extends TransportWriteAction<BulkShardRequest,...> — **复制框架的写动作**
- import TransportReplicationAction (TransportShardBulkAction.java:26) — 复制基类 (E-6 衔接)
- 特殊处理 (TransportShardBulkAction.java:584-601): 无 seqNo / noop 跳过复制
- RetryOnReplicaException (TransportShardBulkAction.java:661): 副本重试
- 语义: 主分片执行整个 BulkShardRequest, 复制到副本作为整体 (E-6 ReplicationOperation)

代码类型: 衔接分析

结论: **BulkShardRequest 是复制单元: TransportWriteAction 框架 (L74) → 主执行 (Q2) → 整体复制到副本 (E-6) — 批量复制省副本侧 RTT**。TransportShardBulkAction.java:74,661

跨域关联: E-6 ReplicationOperation (复制框架) — 批量写入闭环
