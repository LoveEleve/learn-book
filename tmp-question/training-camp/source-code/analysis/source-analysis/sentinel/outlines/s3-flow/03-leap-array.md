# 一秒不是一个数字

> S-3 下篇。本文把流控最终依赖的时间底座拆开：一个时间片是什么，环形数组如何推进，future bucket 如何兑现借位。

## 悬念

流控规则说“每秒最多 N 次”，但代码里并没有一个每秒清零的总计数器。Sentinel 把一秒切成多个 bucket，用访问时推进的环形数组维护最近窗口。

## 一、WindowWrap 是时间壳

`WindowWrap<T>` 只关心三件事：窗口长度、窗口起点、窗口值：

```java
private final long windowLengthInMs;
private long windowStart;
private T value;
```

它不认识 pass、block、rt；`isTimeInWindow(...)` 只判断时间戳是否落在 `[windowStart, windowStart + windowLength)` 区间 (`WindowWrap.java:24-39, 66-75`)。

真正的统计内容由 `MetricBucket` 承载。它内部是按 `MetricEvent` 索引的 `LongAdder[]`，另有 `minRt` (`MetricBucket.java:29-47`)。因此：

- `WindowWrap` = 这个数据属于哪个时间片
- `MetricBucket` = 这个时间片里记了什么

## 二、LeapArray: 时间除以桶长,再对数组取模

构造时，`LeapArray` 要求总区间能被 bucket 数整除：

```java
windowLengthInMs = intervalInMs / sampleCount;
```

索引算法非常直接：

```java
long timeId = timeMillis / windowLengthInMs;
return (int)(timeId % array.length());
```

对应 `LeapArray.java:55-70, 100-104`。例如总窗口 1000ms、10 个 bucket 时，每个 bucket 100ms；时间 1234ms 会落到 `timeId=12`，环形下标 `12 % 10 = 2`。

下标会复用，所以不能只看数组位置，还必须比较 `windowStart`。这就是 `currentWindow(...)` 同时计算 index 和窗口起点的原因。

## 三、currentWindow: 四种状态

`currentWindow(timeMillis)` 是整个推进算法的核心 (`LeapArray.java:121-208`)。

### 1. 位置为空: CAS 建桶

数组位置为 null 时，先创建：

```java
WindowWrap<T> window = new WindowWrap<>(
    windowLengthInMs, windowStart, newEmptyBucket(timeMillis));
```

然后用 `compareAndSet(idx, null, window)` 发布。多个线程同时创建时，只有一个成功，其余线程让出 CPU 后重试。

### 2. 起点相同: 直接复用

如果 `old.windowStart() == windowStart`，说明当前时间仍在这个 bucket 内，直接返回旧 bucket，不加锁。

### 3. 当前时间领先: 短锁重置

如果 `windowStart > old.windowStart()`，旧 bucket 已落后，需要用 `updateLock.tryLock()` 获取更新锁，再调用子类的 `resetWindowTo(old, windowStart)`。锁只在 bucket 过期时竞争，正常读路径不持锁。

### 4. 请求时间落后: 返回临时窗口

如果 `windowStart < old.windowStart()`，说明传入时间落在数组已有窗口之前。代码不改数组，而是返回一个临时 `WindowWrap`，避免旧数据被倒退覆盖。

这四态共同实现了“访问时推进”：没有定时清理线程，只有真正访问某个时间片时才创建或重置它。

## 四、有效窗口与过期窗口

`LeapArray.isWindowDeprecated(...)` 的默认判断是：

```java
return time - windowWrap.windowStart() > intervalInMs;
```

也就是只有当 bucket 起点已经落后整个统计区间后，才算完全过期 (`LeapArray.java:266-273`)。

`values(timeMillis)` 遍历所有环形位置，只收集没有过期的 bucket (`LeapArray.java:329-343`)。因此上层的 `passQps()` 等聚合操作不需要逐桶手动清空旧值。

## 五、普通滑窗与 future bucket

普通 bucket 记录已经发生的 pass/block/success/rt。优先级 occupy 则多了一种“未来已经被预占”的状态，所以 `OccupiableBucketLeapArray` 内部维护：

```java
private final FutureBucketLeapArray borrowArray;
```

请求借位时，`addWaiting(time, acquireCount)` 把 pass 写入 future 数组；主数组创建或重置 bucket 时，会读取同一时间片的借位值并折算进去 (`OccupiableBucketLeapArray.java:31-58, 79-81`)。

`FutureBucketLeapArray.isWindowDeprecated(...)` 返回：

```java
return time >= windowWrap.windowStart();
```

它只保留尚未到达的未来窗口；一旦时间追上窗口起点，future 记录就应该被主数组消费，而不是继续作为未来借条存在 (`FutureBucketLeapArray.java:49-51`)。

这也解释了 `StatisticSlot` 的特殊分支：`PriorityWaitException` 只增加 thread，不再 `addPassRequest`。pass 已经在 future bucket 中预占，时间推进时会兑现；再记一次就会重复计算。

## 六、StatisticNode 如何消费窗口

`StatisticNode` 在窗口之上提供业务语义：

- `passQps()`、`blockQps()`、`exceptionQps()` 读取 second-level metric
- `totalPass()`、`totalException()`、`metrics()` 读取 minute-level metric
- `curThreadNum` 单独用 `LongAdder` 维护 (`StatisticNode.java:96-108, 165-206, 241-242`)

因此“每秒 N 次”并不是直接读取一个变量，而是：

```text
FlowRule controller
  -> Node.passQps()/curThreadNum()
  -> StatisticNode
  -> ArrayMetric
  -> LeapArray.values/currentWindow
  -> MetricBucket counters
```

## 七、两个旁枝不要混入主路径

- `tokenbucket/` 下的四个类在 1.8.9 主代码中没有生产消费者，不能写成当前 FlowSlot 执行路径。
- `NodeBuilder` 已 `@Deprecated`，现行节点创建直接位于 NodeSelectorSlot/ClusterBuilderSlot。

## 悬念回收

Sentinel 不需要每秒启动一个清零任务。它把时间离散成 bucket，访问时用“时间除桶长 + 环形取模”定位，用窗口起点防止下标复用污染，再用 CAS 和短锁完成并发建桶/重置。

Flow occupy 只是进一步利用同一套底座，把未来额度先写入 future bucket，再在时间推进时兑现。

## 锚点

- `WindowWrap.java:24-39`
- `WindowWrap.java:66-75`
- `MetricBucket.java:29-47`
- `LeapArray.java:55-70`
- `LeapArray.java:100-104`
- `LeapArray.java:121-208`
- `LeapArray.java:266-273`
- `LeapArray.java:329-343`
- `OccupiableBucketLeapArray.java:31-58`
- `OccupiableBucketLeapArray.java:79-81`
- `FutureBucketLeapArray.java:49-51`
- `StatisticNode.java:96-108`
