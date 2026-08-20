# S-3 Flow 流控域 — 审查记录

## Pass 1 / Pass 2

- FlowSlot 确认只是分发壳，算法下沉至 FlowRuleChecker 与 TrafficShapingController。
- 规则组合确认：同一 resource 的规则是顺序全遍历，任一 controller 失败即 `FlowException`，不是挑选一条“最佳规则”。
- `limitApp`、`strategy/refResource`、`grade`、`controlBehavior` 被拆成四个正交维度。
- FlowRuleManager 确认是 `SentinelProperty → PropertyListener → RuleManager` 更新链，而不是直接修改 Map。
- cluster mode 确认在 FlowRuleChecker 内联分支，不存在 `ClusterFlowSlot`。
- 四 controller 的语义逐一对照方法体：立即拒绝、warm-up、匀速排队、warm-up + 匀速排队。
- PriorityWaitException 确认是内部控制流信号，不是 BlockException；其 pass 已提前进入 future bucket。
- LeapArray 确认是访问时推进：时间索引、CAS 建桶、短锁重置、读取时过滤过期窗口。
- tokenbucket/ 四文件确认无主代码消费者，排除出当前执行路径。

## 正文 review

### `01-flow-check.md`

- 首轮：核对 FlowSlot、FlowRuleManager、FlowRuleChecker 的调用路径。
- 二轮：核对全量规则遍历、limitApp/strategy/refResource 节点选择和 cluster fallback。
- 三轮：统一“规则匹配”和“controller 行为”术语，避免把 checker 描述为规则排序器。

### `02-traffic-shaping.md`

- 首轮：逐 controller 对照 `canPass` 方法体，确认四种行为没有混写。
- 二轮：确认 `ThrottlingController` 的 CAS 时间线和回滚语义。
- 三轮：确认 warm-up-rate-limiter 是继承 warm-up 后改为排队成本，不是独立 token bucket。
- 四轮：确认 `PriorityWaitException` 只在 prioritized + QPS + occupy 路径出现，并与 StatisticSlot 专门分支闭环。

### `03-leap-array.md`

- 首轮：逐行核对 `calculateTimeIdx`、`currentWindow` 四态和 `isWindowDeprecated`。
- 二轮：核对 future bucket 的读取、写入、兑现路径，确认没有把它描述成普通滑窗。
- 三轮：运行测试：
  - `LeapArrayTest`
  - `BucketLeapArrayTest`
  - `FutureBucketLeapArrayTest`
  - `OccupiableBucketLeapArrayTest`
  - 结果：通过。
- 四轮：重新校准 `StatisticNode`、`OccupiableBucketLeapArray`、`FutureBucketLeapArray` 行号。

## 诚实遗留

1. `WarmUpController` 的 token 数学公式只解释行为，未逐项推导 slope 的理论来源。
2. cluster token server/client 的传输协议属于 S-9，本域只验证 checker 分支与 fallback。
3. `LeapArray` 的极端时钟回拨只覆盖源码的临时窗口分支，未单独构造并发回拨 harness。
4. tokenbucket/ 无消费者的结论基于主代码 import/实例化检索，未将历史 commit 全部回溯。