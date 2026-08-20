# S-3 Flow 流控域 — Pass 1 轮廓记录

> 日期: 2026-08-17 | 范围: `slots/block/flow/` + 滑窗底层 `LeapArray/WindowWrap/MetricBucket/BucketLeapArray/OccupiableBucketLeapArray/FutureBucketLeapArray`

## 核心骨架

- 判定入口: `FlowSlot` (183)
- 规则检查: `FlowRuleChecker` (209)
- 规则模型: `FlowRule` (240) / `ClusterFlowConfig` (233) / `FlowRuleUtil` (276) / `FlowRuleComparator` (57)
- 控制器接口: `TrafficShapingController` (45)
- 四实现: `DefaultController` / `WarmUpController` / `ThrottlingController` / `WarmUpRateLimiterController`
- 规则管理: `FlowRuleManager` (164)
- 例外信号: `PriorityWaitException` / `FlowException`
- 旁枝: `tokenbucket/` 4 文件(执行计划已判定无消费者,预留代码)

## Pass 1 观察

- S-3 不是单一算法，而是“两层分工”：
  - 上层决定“该用哪条规则/哪个 controller”
  - 下层滑窗决定“最近一段时间到底用了多少额度”
- 四类控制器意味着至少四种行为语义：
  - 直接拒绝
  - 预热
  - 匀速排队
  - 预热 + 匀速排队
- `PriorityWaitException` 大概率是“排队成功但需要等待”的信号，而不是 block
- `FlowRuleManager` 应该是标准 property listener 模式(待 Pass 2 精确验证)
- `LeapArray` 已在 S-5 看过外围，但本域需要下潜到索引/重置/过期算法级

## 测试地图

- `FlowSlotTest.java`
- `FlowRuleCheckerTest.java`
- `FlowRuleManagerTest.java`
- `FlowPartialIntegrationTest.java`
- `WarmUpControllerTest.java`
- `ThrottlingControllerTest.java`
- `WarmUpRateLimiterControllerTest.java`
- `LeapArrayTest.java`
- `BucketLeapArrayTest.java`
- `FutureBucketLeapArrayTest.java`
- `OccupiableBucketLeapArrayTest.java`

## 标记问题

1. `FlowSlot` 自己做了什么，真正的限流判定下沉到了哪里?
2. `FlowRuleChecker` 是怎么在多条规则里选规则并调用 controller 的?
3. `TrafficShapingController` 四实现的语义差异各是什么?
4. `PriorityWaitException` 为什么不是 `BlockException`?
5. `FlowRule` 的 grade(strategy by qps / thread) 与 controlBehavior 如何组合?
6. `FlowRuleManager` 的规则更新模式是不是和 `AuthorityRuleManager`/`SystemRuleManager` 一样?
7. `LeapArray` 的 `calculateTimeIdx` / `currentWindow` / `resetWindowTo` / `isWindowDeprecated` 怎么配合完成滑窗推进?
8. `WindowWrap` 和 `MetricBucket` 分别解决什么问题?
9. `tokenbucket/` 预留代码为何无消费者，和当前 controller 体系是什么关系?
