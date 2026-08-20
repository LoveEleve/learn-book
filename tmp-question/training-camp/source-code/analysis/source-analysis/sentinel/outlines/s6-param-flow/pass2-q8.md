# Pass 2 闭环笔记 Q8: 热点参数限流的集群协同

## 验证过程

- `ParamFlowChecker.passCheck` 仅在 `clusterMode && grade == FLOW_GRADE_QPS` 时走 `passClusterCheck`，线程级热点限流不做集群 (`ParamFlowChecker.java:69-74`)。
- `passClusterCheck` 把参数值转成 Collection 后调用 `clusterService.requestParamToken(flowId, count, params)`，`flowId` 来自 `rule.getClusterConfig().getFlowId()` (`ParamFlowChecker.java:263-283`)。
- 状态处理与 FlowRuleChecker 的 cluster 分支同构：
  - OK → 放行
  - BLOCKED → 拒绝
  - 其他状态 → `fallbackToLocalOrPass`
- `fallbackToLocalOrPass` 按 `clusterConfig.isFallbackToLocalWhenFail()` 决定回退本地或直接放行 (`ParamFlowChecker.java:296-305`)。
- `pickClusterService` 依据 `ClusterStateManager` 是 client 还是 server 选择对应 provider，否则返回 null。

## 结论

热点参数限流的集群模式与普通流控集群模式共用一个 TokenService 体系，只是用 `requestParamToken` 携带参数值维度。回退策略一致：失败时要么回退本地判定，要么直接放行。