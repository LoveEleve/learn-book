# 参数值维度的统计

> S-6 中篇。本文讲热点参数限流的统计层：参数值如何被聚合、容量如何限制、统计如何织入主链。

## 悬念

普通统计看资源维度，一个 `StatisticNode` 就够了。但热点限流要按参数值拆维度，一个资源可能有成千上万个参数值，统计结构怎么设计才不会撑爆内存？

## 一、三层统计结构

热点参数统计由三个类分层协作：

- `ParameterMetricStorage`：`ConcurrentHashMap<String, ParameterMetric>`，按资源名存 metric
- `ParameterMetric`：一个资源下的规则维度索引 + 线程计数 + 令牌/时间计数
- `ParamMapBucket`：单个时间窗口内的参数值事件计数

`ParameterMetric` 内部有三张 map：

```java
Map<ParamFlowRule, CacheMap<Object, AtomicLong>> ruleTimeCounters;
Map<ParamFlowRule, CacheMap<Object, AtomicReference<TokenUpdateStatus>>> ruleTokenCounter;
Map<Integer, CacheMap<Object, AtomicInteger>> threadCountMap;
```

- `ruleTimeCounters`：匀速排队用的时间记录，key 是参数值
- `ruleTokenCounter`：令牌桶用的令牌状态，key 是参数值
- `threadCountMap`：线程级限流的并发计数，key 是参数下标 → 参数值

这是一个“规则 → 参数值 → 计数”的三层结构，而不是把参数值塞进一个巨大的全局 map。

## 二、容量上限与 LRU 淘汰

参数值是不可控的——理论上一个资源可以有无限种参数值。所以统计 map 必须有容量上限。

`CacheMap` 的实现 `ConcurrentLinkedHashMapWrapper` 底层用 Google 的 `ConcurrentLinkedHashMap`，通过 `maximumWeightedCapacity` 限容，`Weighers.singleton()` 表示每个 entry 权重为 1。

`ParameterMetric` 里的容量是刻意设的：

```java
private static final int THREAD_COUNT_MAX_CAPACITY = 4000;
private static final int BASE_PARAM_MAX_CAPACITY = 4000;
private static final int TOTAL_MAX_CAPACITY = 20_0000;
```

超出容量的冷门参数值会被 LRU 淘汰。这就是热点限流在海量参数值下不爆内存的关键：它只精确统计“最近的、最热的”参数值，冷门值可以接受统计不精确或被淘汰。

## 三、统计织入主链

热点参数统计不自己起一个统计线程，而是织入 `StatisticSlot` 的生命周期。

`ParamFlowStatisticSlotCallbackInit` 是一个 `InitFunc`，启动时注册两个 callback：

```java
StatisticSlotCallbackRegistry.addEntryCallback(..., new ParamFlowStatisticEntryCallback());
StatisticSlotCallbackRegistry.addExitCallback(..., new ParamFlowStatisticExitCallback());
```

entry callback 在 `StatisticSlot` 放行后调用 `parameterMetric.addThreadCount(args)`，给对应参数值 +1 并发线程；exit callback 在完成时调用 `decreaseThreadCount(args)`，-1 并发线程。

注意 exit callback 只在 entry 没有 blockError 时才减：

```java
if (context.getCurEntry().getBlockError() == null) {
    parameterMetric.decreaseThreadCount(args);
}
```

这是配对语义：只有真正执行过的请求才加过线程，所以也只有真正完成的请求才减。

## 四、规则驱动的生命周期

`ParameterMetricStorage` 是惰性初始化的。`ParamFlowSlot.checkFlow` 在遍历规则时调用 `initParamMetricsFor`，双检锁创建 metric 并 `initialize(rule)`。

当规则更新时，`ParamFlowRuleManager` 的 listener 会做反向清理：

- 没有规则了 → 清空全部 metric
- 某个资源被删除 → 清该资源的 metric
- 某条规则被删除 → `parameterMetric.clearForRule(rule)`

也就是说，热点参数统计是“规则存在就存在，规则消失就回收”。它不会像核心统计那样一直存在。

## 五、为什么不用滑窗做线程计数

`ParameterMetric` 的线程计数用 `AtomicInteger`，而不是滑窗。因为线程计数是“当前值”，不是“最近 N 秒的总和”——它需要精确的 +1/-1 配对，而不是窗口聚合。

而 QPS 类的令牌桶/匀速排队，才需要用时间窗口和令牌状态（下篇详述）。这是“并发度”和“速率”两种度量在数据结构上的自然分工。

## 悬念回收

热点参数统计的关键设计是：

1. 三层结构：storage → metric → 参数值计数
2. LRU 限容：`ConcurrentLinkedHashMap` 淘汰冷门参数值
3. callback 织入：复用 `StatisticSlot` 的 entry/exit 生命周期
4. 规则驱动：有规则才统计，规则删了就回收

它没有另起炉灶，而是把“参数值维度”这个新需求，通过扩展点优雅地挂到了现有统计主链上。

## 锚点

- `ParameterMetric.java:38-40`
- `ParameterMetric.java:49-58`
- `ParameterMetric.java:128`
- `ParameterMetric.java:187`
- `ConcurrentLinkedHashMapWrapper.java:33-49`
- `ParamFlowStatisticSlotCallbackInit.java:33-35`
- `ParamFlowStatisticEntryCallback.java:38`
- `ParamFlowStatisticExitCallback.java:32-36`
- `ParameterMetricStorage.java:45-54`
- `ParamFlowRuleManager.java:114-132`
