# 滑动窗口与借未来

> S-5 下篇。本文只讲统计底座：`StatisticNode`、`LeapArray`、`ArrayMetric`，以及抢占式流控为什么要多出一套 future bucket。

## 悬念

为什么 Sentinel 不直接在一个计数器上累加 qps/rt/block，而要引入 `StatisticNode -> ArrayMetric -> LeapArray -> MetricBucket` 这么长一层链？因为它要回答的不是“总共发生了多少”，而是“最近一段时间里，滑动着看，发生了多少”。

## 一、StatisticNode 其实维护了两套窗口

`StatisticNode` 不是一个简单计数器。它内部至少有三类实时状态：

- 秒级滑窗 `rollingCounterInSecond`
- 分钟级滑窗 `rollingCounterInMinute`
- 当前线程数 `curThreadNum` (`StatisticNode.java:96-108`)

其中最容易忽略的是“双窗口”设计：

- second 窗口用于规则判断，精度高
- minute 窗口用于累计查询、历史拉取、metrics 输出

比如：

- `passQps()` / `blockQps()` / `exceptionQps()` 都读 second (`StatisticNode.java:165-206`)
- `totalPass()` / `totalException()` / `metrics()` 都读 minute (`StatisticNode.java:116-137, 195-206`)

这说明 `StatisticNode` 自己不做复杂时间推进，它更像一个“面向外部的指标门面”；真正的滑窗推进和 bucket 轮转，都在下面一层。

## 二、ArrayMetric 与 LeapArray 的分层

`StatisticNode` 持有的是 `Metric` 接口，默认实现是 `ArrayMetric`。而 `ArrayMetric` 再包一层 `LeapArray`。

职责拆分可以理解成：

- `StatisticNode`：对外暴露 passQps/avgRt/totalPass 等语义 API
- `ArrayMetric`：把这些语义映射到窗口读取/写入操作
- `LeapArray`：真正管理时间片数组、窗口滚动与过期判断
- `MetricBucket`：单个 bucket 里的原始计数槽

也就是说，真正“滑”的不是 `StatisticNode`，而是 `LeapArray`。

## 三、为什么抢占式流控需要 future bucket

普通滑窗只需要看“现在”和“过去”。但优先级流控(occupy)要把当前请求借到未来窗口，因此还必须有一套“未来时间片”。

`ArrayMetric` 默认就使用 `OccupiableBucketLeapArray` (`ArrayMetric.java:41-48`)。而 `OccupiableBucketLeapArray` 内部又持有一个 `FutureBucketLeapArray borrowArray`：

```java
private final FutureBucketLeapArray borrowArray;
```

这两套数组分工是：

- 主数组：记录当前/过去窗口里真实已经发生的统计
- future 数组：记录借到未来窗口的预占 pass

当新窗口创建或老窗口滚到新时间片时，`OccupiableBucketLeapArray` 会先去 future 数组里找“这个时间片以前有没有被预借过”，如果有，就把那部分 pass 折算进当前 bucket (`OccupiableBucketLeapArray.java:35-58`)。

所以 occupy 不是“当前窗口先透支，之后再补”，而是“直接在未来窗口记账，等时间真正走到那里时再兑现”。

## 四、FutureBucketLeapArray 的关键语义: 只算未来

`FutureBucketLeapArray` 最关键的一行是：

```java
return time >= windowWrap.windowStart();
```

这是它覆写的 `isWindowDeprecated(...)` (`FutureBucketLeapArray.java:47-50`)。语义非常反直觉：一旦当前时间已经追上或超过这个窗口起点，它对 future 数组来说就“过期”了。

为什么？因为 future 数组本来就不是给“当前时间”用的，它只负责保留未来预借值；当未来真正变成现在，这部分值应该转移到主数组，而不是继续留在 future 数组里。

所以 future bucket 不是第二份普通滑窗，而是一张“未来借条簿”。

## 五、为什么 `PriorityWaitException` 不再加 pass

这和中篇正好闭环。

一旦请求通过 occupy 机制借到了未来窗口，它的 pass 已经通过 `addWaiting(...)` 写入 future 数组 (`OccupiableBucketLeapArray.java:69-74`)。等时间片滚到位，主数组会在建新 bucket 时把这部分借位数据 reset 进来 (`OccupiableBucketLeapArray.java:35-58`)。

所以 `StatisticSlot` 在 `PriorityWaitException` 分支里只加 threadNum、不再 `addPassRequest(count)`，否则会双记。这不是漏统计，而是统计已经提前写进 future bucket 了。

## 六、两条容易误入主线的旁枝

### 1. NodeBuilder

`NodeBuilder` 已经 `@Deprecated`，生产代码里没有主链消费者。现行 1.8.9 的真实构建路径已经内联进 `NodeSelectorSlot` 和 `ClusterBuilderSlot`。它属于遗留接口，不是 S-5 主线。

### 2. eagleeye

`eagleeye/` 与 `node/`、`slots/statistic/`、`slots/nodeselector/`、`slots/clusterbuilder/` 零直接依赖。它是并行日志子系统，不直接消费节点树。S-5 主线讲统计树与滑窗即可，`eagleeye` 只需要作为旁枝点到为止。

## 悬念回收

Sentinel 之所以要铺出 `StatisticNode -> ArrayMetric -> LeapArray -> MetricBucket` 这条链，不是为了抽象而抽象，而是因为它必须同时处理三件事：

- 现在这一个时间片里发生了什么
- 最近一段滑动窗口里累计发生了什么
- 如果请求借到了未来时间片，那未来该怎么兑现

这三件事只靠一个总计数器根本表达不出来。

## 锚点

- `StatisticNode.java:96`
- `StatisticNode.java:116`
- `StatisticNode.java:165`
- `ArrayMetric.java:41`
- `OccupiableBucketLeapArray.java:31`
- `OccupiableBucketLeapArray.java:40`
- `OccupiableBucketLeapArray.java:68`
- `FutureBucketLeapArray.java:49`
