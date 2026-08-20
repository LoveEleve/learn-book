# Pass 2 闭环笔记 Q5: server 侧流控判定(ClusterFlowChecker)

## 验证过程

- `acquireClusterToken(rule, acquireCount, prioritized)` 是核心判定：
  1. `allowProceed(id)`：用 `GlobalRequestLimiter.tryPass(namespace)` 做全局请求限流，失败返回 `TOO_MANY_REQUEST` (`ClusterFlowChecker.java:52-55`)。
  2. 取 `ClusterMetricStatistics.getMetric(id)`，拿不到返回 `FAIL` (`ClusterFlowChecker.java:57-61`)。
  3. 算全局阈值：`calcGlobalThreshold(rule)`（GLOBAL 取规则 count，AVG_LOCAL 乘连接数）再乘 `getExceedCount()` (`ClusterFlowChecker.java:34-46, 63`)。
  4. `nextRemaining = globalThreshold - latestQps - acquireCount`：
     - `>= 0` → 记 PASS 事件，prioritized 记 OCCUPIED_PASS，返回 OK + remaining (`ClusterFlowChecker.java:65-81`)
     - `< 0` → prioritized 时尝试 `metric.tryOccupyNext` 借未来窗口，成功返回 SHOULD_WAIT + waitInMs，失败记 BLOCK 返回 BLOCKED (`ClusterFlowChecker.java:82-112`)
- `calcGlobalThreshold` 支持两种集群阈值：GLOBAL（全局总量）和 AVG_LOCAL（按连接数均摊）。

## 结论

server 侧流控是 server 本地聚合判定：用全局阈值减去当前 PASS 事件均值得到剩余额度，够则记 PASS 放行，不够则 prioritized 尝试借未来窗口或 BLOCK。阈值类型（GLOBAL/AVG_LOCAL）决定是否按连接数放大，全局请求限流先行拦截过载。