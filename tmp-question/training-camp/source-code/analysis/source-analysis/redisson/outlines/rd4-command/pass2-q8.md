# 闭环笔记 q8: 批处理 — CommandBatchService 按节点分组执行

## 假设
CommandBatchService extends CommandAsyncService (复用单命令), 累积命令 → executeAsync() 按 NodeSource 分组 → 每节点管道批量发送。

## 验证过程
- 继承: `class CommandBatchService extends CommandAsyncService implements BatchService` (CommandBatchService.java:53) — **批处理是单命令的超集**
- executeAsync (L273-327):
  - L274 `executed.get()` 防重复执行 ("Batch already executed!")
  - L278 空命令 → 返回空 BatchResult
  - L284-286 Redis 队列模式分支
  - commands = `Map<NodeSource, Entry>` (L289 使用) — **命令按 NodeSource 分组**
  - L290-310 skipResult 快路径 (options.skipResult && syncSlaves==0): 只 void transform, 不收集响应
  - L311+: 完整路径收集 responses
  - 失败: 所有命令 tryFailure(ex) (L314-318) — 批量原子失败
- BatchPromise (L25) extends CompletableFuture — 延迟完成对象
- BatchService 接口 (BatchService.java:29) — command batch + batch group (嵌套)
- 与 Redis 协议: 批处理用 **pipeline** (RedisQu黑洞 queued executor) — 非 MULTI/EXEC 事务 (除非 transaction 选项)

## 代码类型
Interface (批处理抽象) — 单命令的聚合视图

## 跨域关联
- Q1 (async 单体) → 批处理组合单体
- RD-2 (锁续期批量) → AsyncChunkProcessor 也是聚合
- Redis R-16 (MULTI/EXEC) → 批 vs 事务边界 (batch≠transaction)

## 结论
批处理 = 单命令超集: 命令按 NodeSource 分组, executeAsync 一次编排, skipResult 快路径, 失败 tryFailure 全灭。BatchPromise 延迟完成。边界: batch 是 pipeline (性能优化) 非 transaction (原子性), 事务语义在 BatchOptions 可选。
源码位置: CommandBatchService.java:53,273-327; BatchPromise.java:25