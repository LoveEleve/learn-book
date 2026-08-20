# Pass 2 闭环笔记 XJ-4: 分片参数如何传播

## 初始假设
- `ShardingUtil` 是分片执行的核心运行时容器，分片逻辑主要发生在 executor 本地。

## 验证过程
- 当前源码中的 `ShardingUtil.java` 整个文件是**注释态残留**，并未参与运行时主链。
- 真正的分片参数发生在 Admin 侧 `XxlJobTrigger.trigger/processTrigger`：
  - 如果路由策略是 `SHARDING_BROADCAST` 且未手工指定分片参数，则按 `group.getRegistryList().size()` 循环触发多次，每次传入不同的 `index/total` (`XxlJobTrigger.java:67-83`)。
  - 否则使用外部传入的 `executorShardingParam`，或默认 `0/1`。
- `TriggerParam` 上实际承载的是 `broadcastIndex` 和 `broadcastTotal`，executor 侧 handler 再从 `XxlJobContext` 读取分片序号与总数。
- `ScriptJobHandler` 也直接把 `shardIndex/shardTotal` 作为脚本参数 1、2 传入 (`ScriptJobHandler.java:60-68`)。

## 结论

XJ-4 的主线不是 `ShardingUtil`，而是 **Admin 侧构造分片 trigger 参数 → TriggerParam 传到 executor → handler/XxlJobContext 消费**。`ShardingUtil` 只能作为历史残留/概念补充，不能误写成现行主链。