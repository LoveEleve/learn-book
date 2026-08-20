# Pass 2 闭环笔记 Q9: tokenbucket 为什么是预留旁枝

## 验证过程

- `slots/block/flow/tokenbucket/` 有 `TokenBucket`、`AbstractTokenBucket`、`DefaultTokenBucket`、`StrictTokenBucket` 四个文件。
- 在 Sentinel 1.8.9 主代码中检索这些类的 import/实例化，未发现生产消费者。
- 当前生效的限流行为来自 `TrafficShapingController` 四实现，以及 `FlowRule.getRater()` 创建的 controller，不经过 `tokenbucket/`。
- 因此不能因为包名叫 tokenbucket，就把它当作当前 FlowSlot 的实现；它属于未接线/预留代码，和 `eagleeye/TokenBucket` 也不是同一个类。

## 结论
S-3 主线应以 `FlowRuleChecker → TrafficShapingController → Node/LeapArray` 为准。`tokenbucket/` 只能作为源码考古中的旁枝说明，不能写成现行执行路径。