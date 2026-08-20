# 统计真正落在哪

> S-5 中篇。本文只讲 `StatisticSlot`，看 Sentinel 的 pass / block / rt / exception / threadNum 到底在哪一层收口。

## 悬念

很多人第一次看 Sentinel，会以为真正的统计都分散在各种 RuleChecker 或 Node 里。其实不是。真正把一次调用“记账”记完整的，是 `StatisticSlot`。

## 一、entry 侧: 先放行,再记通过

`StatisticSlot.entry(...)` 的顺序非常关键：它先 `fireEntry(...)`，也就是先让后面的 Authority/System/Flow/Degrade 等槽做判断；只有下游全部通过之后，才开始写通过统计 (`StatisticSlot.java:52-74`)。

核心顺序是：

```java
fireEntry(context, resourceWrapper, node, count, prioritized, args);

node.increaseThreadNum();
node.addPassRequest(count);
```

这里的 `node` 是当前 `DefaultNode`。但由于 `DefaultNode` 已经覆写了这些写方法，它会在写自己时同步透传给 `ClusterNode`。所以 `StatisticSlot` 看起来只写了一个节点，实际上局部与全局两层都记上了。

然后它还会再分两叉：

- 如果当前 entry 挂了 origin node，就给 origin node 也加 thread/pass (`StatisticSlot.java:60-64`)
- 如果这是 IN 流量，就给全局 `ENTRY_NODE` 也加 thread/pass (`StatisticSlot.java:66-70`)

也就是说，放行之后一次写入，最终会落到三种视角：

- 当前 `DefaultNode`
- 当前 origin node
- 全局 `ENTRY_NODE`(仅 IN)

## 二、被拦时记什么,不记什么

`StatisticSlot.entry(...)` 有两个异常分支容易混：`PriorityWaitException` 和 `BlockException`。

### 1. PriorityWaitException

这是“借未来窗口成功后的特殊通过”。它会：

- 增加 threadNum
- origin node / `ENTRY_NODE` 也同步增加 threadNum
- 触发 `onPass(...)` callback

但它**不加 `addPassRequest(count)`** (`StatisticSlot.java:75-89`)。原因是这次通过已经在未来 bucket 里预占过 pass 了；如果这里再加一次，会双记。

### 2. BlockException

真正被拦截时，`StatisticSlot` 不会走 exit 侧成功记账，而是直接在 entry 侧写 block：

```java
context.getCurEntry().setBlockError(e);
node.increaseBlockQps(count);
```

并且 origin node / `ENTRY_NODE` 也同步记 block (`StatisticSlot.java:90-110`)。

注意这里没有增加 threadNum，也没有 success/rt 之类的后置统计。因为这次调用根本没有成功进入“执行中”状态。

## 三、exit 侧: rt / success / exception 的总收口

exit 侧的第一句是：

```java
if (context.getCurEntry().getBlockError() == null) {
```

这就是“只给未被 block 的调用做完成记账”的总闸门 (`StatisticSlot.java:121-135`)。

如果通过了，它会先算：

- `completeTimestamp`
- `rt = completeTimestamp - createTimestamp`
- `error = curEntry.getError()`

然后统一调用 `recordCompleteFor(...)`，分别写给：

- 当前 node
- origin node
- `ENTRY_NODE`(仅 IN) (`StatisticSlot.java:130-135`)

而 `recordCompleteFor(...)` 自己只做三件事：

```java
node.addRtAndSuccess(rt, batchCount);
node.decreaseThreadNum();
if (error != null && !(error instanceof BlockException)) {
    node.increaseExceptionQps(batchCount);
}
```

这三句几乎就是 Sentinel 的“完成记账三件套”：

- 记 RT
- 记 success
- threadNum - 1
- 如有业务异常, exception + 1 (`StatisticSlot.java:155-163`)

## 四、为什么异常统计不在 Tracer 里结束

`Tracer` 只是把业务异常挂到 `entry.error` 上；真正把它变成统计数字的，是 `StatisticSlot.exit(...)`。

也就是说：

- `Tracer` 负责“标记这次调用带异常”
- `StatisticSlot` 负责“在调用完成时把异常折算进节点统计”

这也是为什么 `recordCompleteFor(...)` 里要显式排除 `BlockException`：被规则拦截属于框架拒绝，不属于业务故障，不能算 exception qps。

## 五、callback registry 为什么放在这里

`StatisticSlotCallbackRegistry` 不是旁路装饰，而是统计面的正式织入点。

entry 侧：

- pass 后触发 `handler.onPass(...)`
- block 后触发 `handler.onBlocked(...)`

exit 侧：

- `recordCompleteFor(...)` 完成后触发 `handler.onExit(...)`
- 然后才 `fireExit(...)` 继续后续槽 (`StatisticSlot.java:146-152`)

这意味着 callback 看到的是“统计已经写完后的状态”。这正是 S-6 热点参数统计能够通过 callback 注入而不用修改 `StatisticSlot` 主干的原因。

## 悬念回收

`StatisticSlot` 不是“某个普通统计槽”，它就是 Sentinel 一次调用的统一记账员：

- entry 侧记 pass / block / threadNum
- exit 侧记 rt / success / exception
- 再把这些统计同时扇出到 node / origin / `ENTRY_NODE`

规则判断分散在别的槽里，真正的记账收口在这里。

## 锚点

- `StatisticSlot.java:59`
- `StatisticSlot.java:62`
- `StatisticSlot.java:65`
- `StatisticSlot.java:72`
- `StatisticSlot.java:81`
- `StatisticSlot.java:98`
- `StatisticSlot.java:129`
- `StatisticSlot.java:138`
- `StatisticSlot.java:146`
- `StatisticSlot.java:155`
