# S-3 Flow 流控域 — 大纲

## 上篇: 规则如何变成一次判定 — `01-flow-check.md`

1. `FlowSlot` 为什么只是入口壳
2. `FlowRuleManager` 的 property/listener 更新链
3. `FlowRuleChecker` 全量遍历规则，任一命中即拒绝
4. `limitApp` / `strategy` / `refResource` 如何选统计节点
5. local 与 cluster 分支的边界

## 中篇: 超限之后有四种命运 — `02-traffic-shaping.md`

1. `grade` / `strategy` / `controlBehavior` 四维规则语义
2. Default：立即拒绝
3. WarmUp：从冷到热逐步放量
4. Throttling：固定时间成本与匀速排队
5. WarmUpRateLimiter：预热与排队组合
6. `PriorityWaitException` 为什么是内部控制流而非 block

## 下篇: 一秒不是一个数字 — `03-leap-array.md`

1. `WindowWrap` / `MetricBucket` 的时间壳与数据体
2. `LeapArray` 的索引、CAS 建桶、短锁重置、过期判断
3. `StatisticNode` 如何消费 second/minute 两套窗口
4. occupy 的 future bucket 借位模型
5. `tokenbucket/` 与 `NodeBuilder`：现行路径之外的源码旁枝
