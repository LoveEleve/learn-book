# 三种判定路径

> S-6 下篇。本文讲热点参数限流的判定层：默认令牌桶、匀速排队、线程计数，以及集群模式与回退。

## 悬念

热点参数限流拿到参数值后，怎么决定放行还是拦截？它不是一种算法，而是按 `grade` 和 `controlBehavior` 分派到三种路径。

## 一、判定入口:先取参数值

`ParamFlowChecker.passCheck` 是判定入口：

```java
int paramIdx = rule.getParamIdx();
if (args.length <= paramIdx) {
    return true;
}
Object value = args[paramIdx];
if (value instanceof ParamFlowArgument) {
    value = ((ParamFlowArgument) value).paramFlowKey();
}
if (value == null) {
    return true;
}
```

- 参数个数不足 → 放行
- 参数值为 null → 放行
- 参数值实现 `ParamFlowArgument` → 用 `paramFlowKey()` 作为统计 key

然后按 `clusterMode && grade == QPS` 决定走集群还是本地。

## 二、本地判定:三种路径

`passSingleValueCheck` 按 grade 分派：

```java
if (rule.getGrade() == FLOW_GRADE_QPS) {
    if (rule.getControlBehavior() == CONTROL_BEHAVIOR_RATE_LIMITER) {
        return passThrottleLocalCheck(...);   // 匀速排队
    } else {
        return passDefaultLocalCheck(...);     // 简化令牌桶
    }
} else if (rule.getGrade() == FLOW_GRADE_THREAD) {
    // 线程计数
}
```

### 1. 默认令牌桶

`passDefaultLocalCheck` 用 `ruleTokenCounter` 里的 `TokenUpdateStatus` 维护每个参数值的令牌状态。它只在统计窗口(`durationInSec`)过去后才补充令牌，是一个简化令牌桶：

- 首次访问：直接消耗 `acquireCount` 放行
- 窗口内：剩余令牌减 `acquireCount`，够则放行，不够则拒绝
- 窗口过后：按 `passTime * tokenCount / durationInSec` 补充令牌

`burstCount` 允许在令牌桶基础上额外放行一部分突发流量。

### 2. 匀速排队

`passThrottleLocalCheck` 用 `ruleTimeCounter` 维护每个参数值的“上次通过时间”。它把每个请求换算成时间成本：

```java
long costTime = Math.round(1.0 * 1000 * acquireCount * durationInSec / tokenCount);
```

然后按 `expectedTime = lastPassTime + costTime` 排队，等待超过 `maxQueueingTimeMs` 就拒绝，否则 sleep 到自己的时间点再放行。这和 S-3 的 `ThrottlingController` 是同一套匀速排队思想，只是按参数值维度拆开。

### 3. 线程计数

线程级热点限流直接比较并发线程数：

```java
long threadCount = getParameterMetric(resourceWrapper).getThreadCount(rule.getParamIdx(), value);
if (exclusionItems.contains(value)) {
    int itemThreshold = rule.getParsedHotItems().get(value);
    return ++threadCount <= itemThreshold;
}
long threshold = (long) rule.getCount();
return ++threadCount <= threshold;
```

线程计数由 entry/exit callback 维护（中篇已述），这里只读当前值比较阈值。

## 三、特殊热点项:独立阈值

`paramFlowItemList` 允许给特定参数值单独设阈值。解析后存到 `hotItems` map。

三种路径都检查 `exclusionItems.contains(value)`：

- 命中特殊项 → 用该项的独立阈值
- 未命中 → 用规则的默认 `count`

例如“默认所有参数值 QPS 100，但 `admin` 这个值 QPS 1000”。这是热点限流“按值差异化”的核心能力。

## 四、集群模式与回退

集群模式只在 `clusterMode && grade == QPS` 时启用。`passClusterCheck` 把参数值转成 Collection，调用：

```java
clusterService.requestParamToken(rule.getClusterConfig().getFlowId(), count, params);
```

`flowId` 来自 `rule.getClusterConfig()`。状态处理：

- OK → 放行
- BLOCKED → 拒绝
- 其他 → `fallbackToLocalOrPass`

`fallbackToLocalOrPass` 按 `clusterConfig.isFallbackToLocalWhenFail()` 决定回退本地判定，还是直接放行。`pickClusterService` 依据 `ClusterStateManager` 是 client 还是 server 选择对应 provider。

## 五、遗留:HotParamSlotChainBuilder

`HotParamSlotChainBuilder` 已 `@Deprecated`，注释明确说明：1.7.2 起可以用 `@Spi(order = -3000)` 调整 `ParamFlowSlot` 顺序，这个类只为老版本兼容保留。

它继承 `DefaultSlotChainBuilder`，没有任何额外代码。它佐证了 S-1 的结论：槽顺序权威是 `@Spi order`，而不是专门的 builder。

## 悬念回收

热点参数限流的判定是“按 grade + controlBehavior 分派”：

- QPS + 默认 → 简化令牌桶
- QPS + 匀速排队 → 时间线排队
- 线程 → 并发计数
- 集群 + QPS → 集群 token

特殊热点项用独立阈值，集群失败可回退本地或放行。判定层和统计层分离，判定只读统计结果，不自己维护窗口。

## 锚点

- `ParamFlowChecker.java:45-72`
- `ParamFlowChecker.java:102-122`
- `ParamFlowChecker.java:124-194`
- `ParamFlowChecker.java:196-268`
- `ParamFlowChecker.java:270-295`
- `HotParamSlotChainBuilder.java:30-42`
