# Pass 2 闭环笔记 XJ-3: 路由策略的真实边界

## 初始假设
- 路由策略就 8 种，执行计划里的名称已经足够准确。

## 验证过程
- `ExecutorRouteStrategyEnum` 实际枚举 10 项：`FIRST`、`LAST`、`ROUND`、`RANDOM`、`CONSISTENT_HASH`、`LEAST_FREQUENTLY_USED(LFU)`、`LEAST_RECENTLY_USED(LRU)`、`FAILOVER`、`BUSYOVER`、`SHARDING_BROADCAST` (`ExecutorRouteStrategyEnum.java:10-33`)。
- 执行计划里的 `LFH` 是笔误，且漏掉了 `LRU` 和 `FAILOVER`。
- 路由策略真正执行发生在 `XxlJobTrigger.processTrigger`：先从 `ExecutorRouteStrategyEnum.match(...)` 取策略，再调用 `executorRouteStrategyEnum.getRouter().route(triggerParam, group.getRegistryList())` (`XxlJobTrigger.java:113-151`)。
- `SHARDING_BROADCAST` 不是普通选一个 executor，而是按 registryList 大小循环触发多次，每个地址拿不同分片参数 (`XxlJobTrigger.java:67-83`)。
- `ExecutorRouteLFU` 的真实语义：按 jobId 维护 address→频次 map，定期清缓存，优先选频率最低地址；首次引入随机值缓解冷启动集中 (`ExecutorRouteLFU.java:15-63`)。

## 结论

XJ-3 不该按旧计划的 8 项抽象写，而应按源码真实的 10 项策略穷举。尤其 `SHARDING_BROADCAST` 是“多次触发”，不是普通路由；`LFU/LRU/FAILOVER` 也必须与 FIRST/LAST/ROUND 区分开。