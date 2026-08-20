# 超限之后有四种命运

> S-3 中篇。本文只看 `TrafficShapingController`：同一条流控规则，超限之后为什么可能是拒绝、预热、排队，或优先级借位。

## 悬念

“超过阈值就拒绝”只是最简单的一种流控。Sentinel 还支持预热、匀速排队和预热+匀速排队。它们共享同一个 `FlowSlot`，差别到底藏在哪里？

答案：差别不在槽，而在 `FlowRule` 创建的 `TrafficShapingController`。

## 一、四个字段,四个维度

一条 `FlowRule` 至少要拆成四个相互独立的选择：

- `grade`：按 QPS 还是按并发线程数观察
- `strategy/refResource`：从哪个调用关系或资源节点取数
- `limitApp`：限制哪个调用方
- `controlBehavior`：超限后采取什么行为

`FlowRuleChecker` 负责前三类匹配，controller 负责最后一类行为。不要把“按哪个节点统计”和“超限后怎么排队”混成同一个策略。

## 二、DefaultController: 立即拒绝

默认 controller 先取得当前使用量：

```java
int curCount = avgUsedTokens(node);
if (curCount + acquireCount > count) {
    return false;
}
return true;
```

`avgUsedTokens` 在 thread grade 下读 `node.curThreadNum()`，在 QPS grade 下读 `node.passQps()` (`DefaultController.java:44-75`)。

普通请求超过阈值，直接返回 false，之后由 `FlowSlot` 抛出 `FlowException`。

但 prioritized 的 QPS 请求有一条特殊分支：如果当前窗口容得下未来借位，controller 会：

1. 调 `node.tryOccupyNext(...)`
2. 把请求写入 waiting bucket
3. 增加 occupied pass
4. sleep 到预期时间
5. 抛出 `PriorityWaitException` (`DefaultController.java:49-64`)

这个异常不是“拒绝”，而是通知上游：请求已经完成优先级等待，不能再按普通 pass 重复记账。中篇后面会专门回收这个控制流。

## 三、WarmUpController: 从冷到热逐步放量

Warm-up 不把阈值简单理解成一条固定横线。它用 `storedTokens` 表示系统冷却程度，并根据 `warningToken` 与 `maxToken` 把系统分成不同状态 (`WarmUpController.java:64-107`)。

- 剩余 token 低于 warning 区间：按稳定阈值 `count` 判断
- 进入 warning 区间：根据 token 数和 `slope` 算出动态 `warningQps`
- 当前 passQps 加上本次 acquireCount 不超过动态值：放行
- 超过动态值：拒绝 (`WarmUpController.java:114-143`)

这就是预热的核心：系统越冷，允许的瞬时速率越低；随着请求逐步到来，token 被消耗，允许速率逐渐接近稳定阈值。

## 四、ThrottlingController: 把请求排成时间线

匀速排队不看 node 当前 passQps 来做瞬时比较，而是把每个请求换算成一个时间成本：

```java
costTime = statDurationMs * acquireCount / maxCountPerStat;
expectedTime = costTime + latestPassedTime.get();
```

如果预计等待超过 `maxQueueingTimeMs`，直接拒绝；否则用 CAS 推进 `latestPassedTime`，sleep 到自己的预计时间，再放行 (`ThrottlingController.java:70-152`)。

它维护的是一条“预计通过时间线”：先来的请求占据前面的时间点，后来的请求只能排在后面。CAS 失败/竞争时会回滚本次时间成本，避免把一个最终拒绝的请求留在队列里。

controller 会根据时间精度选择毫秒或纳秒路径：当每请求时间无法用毫秒精确表达，或速率超过每毫秒一个请求时，使用 `System.nanoTime()` 的纳秒路径 (`ThrottlingController.java:45-63, 139-152`)。

## 五、WarmUpRateLimiterController: 预热加排队

`WarmUpRateLimiterController` 继承 `WarmUpController`，复用 token 冷却与 slope 计算；但它不直接用动态 QPS 做“放或拒”，而是把动态 warmingQps 换成每个请求的 `costTime`：

- 冷态：`acquireCount / count`
- 预热态：`acquireCount / warmingQps`
- 再把 costTime 加到 `latestPassedTime` 上形成预计通过时间
- 等待超过 timeout 就拒绝，否则排队后放行 (`WarmUpRateLimiterController.java:43-84`)

因此它是两个机制的组合：warm-up 决定时间成本，rate limiter 决定排队顺序。

## 六、PriorityWaitException: 为什么不是 BlockException

`PriorityWaitException` 继承 `RuntimeException`，不是 `BlockException`。它只在 prioritized + QPS + occupy 成功时由 `DefaultController` 抛出：请求已经等待并占用了未来 pass (`DefaultController.java:54-64`)。

`StatisticSlot` 对它有专门分支：

- 增加 threadNum
- 触发 `onPass`
- 不再 `addPassRequest`
- 不进入 block 统计 (`StatisticSlot.java:81-95`)

如果把它做成 `BlockException`，外层就会把“已经排队通过”当成“规则拒绝”，既会错误暴露给用户，也会把统计写错。

## 悬念回收

四种 controller 的差异可以压缩成四句话：

- Default：现在超了就拒绝
- WarmUp：系统越冷，允许速率越低
- Throttling：每个请求占一段时间，排成时间线
- WarmUpRateLimiter：先算冷/热状态，再把速率转换成排队成本

它们都由 `FlowRule.getRater()` 承载，`FlowSlot` 不需要知道具体算法。

## 锚点

- `DefaultController.java:44-75`
- `DefaultController.java:49-64`
- `WarmUpController.java:64-107`
- `WarmUpController.java:114-143`
- `ThrottlingController.java:45-63`
- `ThrottlingController.java:70-152`
- `WarmUpRateLimiterController.java:43-84`
- `StatisticSlot.java:81-95`
