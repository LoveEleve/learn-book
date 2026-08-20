# 两种统计策略

> S-4 下篇。本文只讲熔断的统计侧：异常熔断与慢调用熔断各自如何计数，CLOSED 和 HALF_OPEN 的判定有何不同。

## 悬念

熔断规则里的“异常比例”“异常数”“慢调用比例”都不是一个简单计数器，而是建立在一个单 bucket 滑窗之上。它们到底怎么算？

## 一、单 bucket 滑窗

两个断路器都使用一个 `LeapArray` 作为统计底座，且 `sampleCount = 1`：

```java
new SimpleErrorCounterLeapArray(1, rule.getStatIntervalMs());
new SlowRequestLeapArray(1, rule.getStatIntervalMs());
```

`sampleCount = 1` 意味着整个统计区间只有一个时间片。`resetStat()` 直接重置当前窗口的值。统计区间长度由规则的 `statIntervalMs` 决定，默认 1000ms。

这里复用了 S-3 讲过的 `LeapArray` 底座，但只用一个 bucket，避免多次窗口聚合的复杂度。

## 二、ExceptionCircuitBreaker: 异常比例与异常数

`ExceptionCircuitBreaker` 支持两种 grade：异常比例和异常数。构造时读取 `minRequestAmount` 和 `threshold`（`rule.count`）。

每次请求完成，`onRequestComplete` 记录：

```java
Throwable error = entry.getError();
SimpleErrorCounter counter = stat.currentWindow().value();
if (error != null) {
    counter.getErrorCount().add(1);
}
counter.getTotalCount().add(1);
```

即每个完成请求总计数 +1，异常请求额外 errorCount +1。

触发熔断的判定在 CLOSED 状态下进行：

```java
List<SimpleErrorCounter> counters = stat.values();
long errCount = 0, totalCount = 0;
for (SimpleErrorCounter counter : counters) {
    errCount += counter.errorCount.sum();
    totalCount += counter.totalCount.sum();
}
if (totalCount < minRequestAmount) {
    return;
}
double curCount = errCount;
if (strategy == DEGRADE_GRADE_EXCEPTION_RATIO) {
    curCount = errCount * 1.0d / totalCount;
}
if (curCount > threshold) {
    transformToOpen(curCount);
}
```

三个要点：

1. `totalCount < minRequestAmount` 时直接返回，防止低流量下被少数异常误触发。
2. 异常数模式直接比较 `errCount`，异常比例模式比较 `errCount / totalCount`。
3. 触发条件是严格 `>`，不是 `>=`。

## 三、ResponseTimeCircuitBreaker: 慢调用比例

`ResponseTimeCircuitBreaker` 只支持 RT 策略。构造时读取 `maxAllowedRt = round(rule.count)`、`maxSlowRequestRatio`、`minRequestAmount`。

请求完成时计算 rt：

```java
long completeTime = entry.getCompleteTimestamp();
if (completeTime <= 0) {
    completeTime = TimeUtil.currentTimeMillis();
}
long rt = completeTime - entry.getCreateTimestamp();
if (rt > maxAllowedRt) {
    counter.slowCount.add(1);
}
counter.totalCount.add(1);
```

慢调用的边界是 `rt > maxAllowedRt`，严格大于。判定时先满足 `totalCount >= minRequestAmount`，再计算 `slowCount / totalCount`，严格超过 `maxSlowRequestRatio` 时熔断。此外代码对“比例恰好等于 1.0 且阈值也是 1.0”的边界做了特判，也会触发熔断。

## 四、HALF_OPEN: 单请求判定

CLOSED 状态下，两个断路器都是聚合整个滑窗的计数再比较阈值。

但进入 HALF_OPEN 后，判定逻辑完全不同——只关注**探测请求本身**：

- `ExceptionCircuitBreaker`：探测请求无异常 → `fromHalfOpenToClose`；有异常 → `fromHalfOpenToOpen`。
- `ResponseTimeCircuitBreaker`：探测请求 rt 不超过阈值 → 回 CLOSED；否则回 OPEN。

这就是“单请求探测”：HALF_OPEN 不再看窗口聚合，只用一次请求的结果决定熔断器的未来。

## 五、触发值是怎么传出去的

熔断触发时会调用 `transformToOpen(triggerValue)`，其中：

- 异常数模式：`triggerValue = errCount`
- 异常比例模式：`triggerValue = errCount / totalCount`
- 慢调用模式：`triggerValue = slowCount / totalCount`

这个值会通过 `notifyObservers(..., snapshotValue)` 传给观察者，用于展示“触发熔断时的指标快照”。而回 CLOSED 或进 HALF_OPEN 时，`snapshotValue` 为 null。

## 悬念回收

熔断的“指标”不是凭空一个数字，而是：

- 统计底座：单 bucket `LeapArray`，长度 `statIntervalMs`
- CLOSED：聚合窗口，比较比例/数量阈值，受 `minRequestAmount` 保护
- HALF_OPEN：只看一个探测请求的结果
- 触发时把指标快照传给观察者

异常熔断和慢调用熔断共用同一套状态机，只在“怎么算指标”和“探测请求怎么判”上分叉。

## 锚点

- `ExceptionCircuitBreaker.java:44-53`
- `ExceptionCircuitBreaker.java:65-77`
- `ExceptionCircuitBreaker.java:80-111`
- `ResponseTimeCircuitBreaker.java:45-54`
- `ResponseTimeCircuitBreaker.java:65-81`
- `ResponseTimeCircuitBreaker.java:84-116`
