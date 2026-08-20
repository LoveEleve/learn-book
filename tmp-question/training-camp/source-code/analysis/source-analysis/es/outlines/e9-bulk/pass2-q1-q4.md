# E-9 闭环笔记 Q1-Q4: 分组/主循环/映射等待/批量优势

## Q1: 按 shard 分组 — 同分片合并

假设: 按 (index, shardId) 分组, 同一分片的操作合并成一个 BulkShardRequest。

验证过程:
- Read TransportBulkAction (TransportBulkAction.java:605-690): `Map<ShardId, List<BulkItemRequest>> requestsByShard` (TransportBulkAction.java:605) → `shardId = docWriteRequest.route(indexRouting)` (TransportBulkAction.java:647) → computeIfAbsent 分组 (TransportBulkAction.java:648-649)
- 分派 (TransportBulkAction.java:672-690): 逐组 → BulkShardRequest (TransportBulkAction.java:675-677)
- 分组键: ShardId (index + shardId) — 同分片操作一次 transport 往返

代码类型: Implementation (分组批)

结论: **批量按 (index, shardId) 分组 (L605-649): 同分片操作合并为一个 BulkShardRequest (L675) — 一次 transport 往返处理多操作, 减少 RTT**。TransportBulkAction.java:605-690

## Q2: 主循环执行 — 逐条 + 失败隔离

假设: 分片内 while 逐条执行, 单条失败不影响其他 (partial success)。

验证过程:
- Read TransportShardBulkAction (TransportShardBulkAction.java:223-260): `while (context.hasMoreOperationsToExecute())` (TransportShardBulkAction.java:223) → executeBulkItemRequest (TransportShardBulkAction.java:224)
- 失败: executeBulkItemRequest 内部捕获异常 → markOperationAsExecuted (TransportShardBulkAction.java:320) — 单条失败记录, 继续下一条
- 复制: BulkPrimaryExecutionContext 逐条执行后复制 (E-6 衔接)
- 语义: 批量 = 多条独立执行, 非事务 (partial success 返回)

代码类型: Implementation (逐条执行)

结论: **分片主循环 = 逐条执行 (L223-260), 单条失败隔离 (markOperationAsExecuted L320) — 批量是"多条独立操作"非事务, 部分成功部分失败正常**。TransportShardBulkAction.java:223-260,320

## Q3: 映射更新等待 — 动态映射重试

假设: 遇到 MAPPING_UPDATE_REQUIRED → 等映射合并完成 → 重新执行该条。

验证过程:
- Read TransportShardBulkAction (TransportShardBulkAction.java:360-385): applyIndexOperationOnPrimary (TransportShardBulkAction.java:360-368) → MAPPING_UPDATE_REQUIRED 检查 (TransportShardBulkAction.java:370)
- 等待 (TransportShardBulkAction.java:371-385): primary.mapperService().merge (TransportShardBulkAction.java:373-377) 合并动态映射 (E-7 衔接)
- 主循环 break (TransportShardBulkAction.java:233-235): "waiting for a mapping update on another thread... break out here" — 等映射线程完成后重新执行
- 语义: 动态映射 (E-7) 首次遇到新字段 → 等映射更新 → 重试

代码类型: Implementation (异步等待)

结论: **MAPPING_UPDATE_REQUIRED → 合并动态映射 (TransportShardBulkAction.java:373-377, E-7) → break 主循环 (TransportShardBulkAction.java:233-235) → 映射完成后重新执行 — 动态映射不阻塞其他操作**。TransportShardBulkAction.java:360-385,233-235

## Q4: 批量 vs 单条 — 性能优势

假设: 批量省 RTT + 共享 translog fsync + 减少锁竞争。

验证过程:
- 省 RTT: Q1 分组一次往返
- 共享 fsync: 多条操作共享一次 translog sync (E-3 双缓冲) — 批量写入的 Location 批量确认
- 减少锁: 同分片操作连续执行, 减少 readLock/uid 锁切换
- 对照: 单条 = 每次完整请求 (RTT+fsync+锁)

代码类型: 对照分析

结论: **批量优势三层: 分组省 RTT (Q1) + 共享 translog fsync (E-3 双缓冲批量落盘) + 同分片连续执行减少锁竞争 — 所以 bulk 是 ES 写入首选**。对照锚点: TransportBulkAction.java:605 + TranslogWriter.java:227 (E-3)

跨域关联: E-3 Translog (批量 fsync) / E-4 路由 (分组)
