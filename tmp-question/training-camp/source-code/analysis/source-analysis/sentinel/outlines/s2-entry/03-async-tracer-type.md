# 异步与统计入口:不占线程的调用

> 本文是 Sentinel 源码分析之"入口域"的下篇。上篇讲了入口的门面与闸门,中篇讲了 entry 的生命周期。
> 所有行号引用版本: Sentinel 1.8.9。

## 悬念:异步线程没有 ThreadLocal,生命周期怎么挂?

中篇讲的调用栈全部依赖 ThreadLocal:entry 挂到当前线程的 context 上。但异步调用(CompletableFuture、线程池回调)在**另一个线程**结束——它的 entry 怎么退出?业务异常又是怎么进统计的?本文回答这两个问题,顺便看 IN/OUT 标签在统计面的作用。

## 一、AsyncEntry:把 context 从"线程所有"变成"对象所有"

异步入口由 `SphU.asyncEntry(...)` 进入,返回 AsyncEntry(CtEntry 的子类)。核心机制在 CtSph 的 asyncEntryWithPriorityInternal(节选自 CtSph.java:84-110):

```java
AsyncEntry asyncEntry = new AsyncEntry(resourceWrapper, chain, context, count, args);
try {
    chain.entry(context, resourceWrapper, null, count, prioritized, args);
    // Initiate the async context only when the entry successfully passed the slot chain.
    asyncEntry.initAsyncContext();
    // The asynchronous call may take time in background, and current context should not be hanged on it.
    // So we need to remove current async entry from current context.
    asyncEntry.cleanCurrentEntryInLocal();
} catch (BlockException e1) {
    // When blocked, the async entry will be exited on current context.
    asyncEntry.exitForContext(context, count, args);
    throw e1;
}
```

槽链检查通过后,两步关键操作:

**第一步,initAsyncContext——建一个独立的异步 context。** (节选自 AsyncEntry.java:75-83):

```java
void initAsyncContext() {
    if (asyncContext == null) {
        this.asyncContext = Context.newAsyncContext(context.getEntranceNode(), context.getName())
            .setOrigin(context.getOrigin())
            .setCurEntry(this);
    }
}
```

新 context 与调用线程的 context **同名、同入口节点**,但它是独立对象(async=true),curEntry 指向这个 AsyncEntry 自己。

**第二步,cleanCurrentEntryInLocal——从当前线程的栈上摘下来。** (节选自 AsyncEntry.java:38-56):

```java
void cleanCurrentEntryInLocal() {
    ...
    if (curEntry == this) {
        Entry parent = this.parent;
        originalContext.setCurEntry(parent);   // 栈顶回退
        if (parent != null) {
            ((CtEntry) parent).child = null;   // 断开 child
        }
    }
}
```

把 AsyncEntry 从调用线程的 context 栈顶摘除——**当前线程不再被这个异步调用挂住**,可以继续处理下一个请求,而 AsyncEntry 自己带着 asyncContext 交给异步任务。

组合起来就是一句话:**同步 entry 的生命周期绑 ThreadLocal,异步 entry 的生命周期绑 Entry 对象本身。** 异步任务线程结束时调用 `asyncEntry.exit()`,走的是 AsyncEntry 覆写的 trueExit(节选自 AsyncEntry.java:97-98):

```java
protected Entry trueExit(int count, Object... args) throws ErrorEntryFreeException {
    exitForContext(asyncContext, count, args);
    return parent;
}
```

在 asyncContext 上执行完整的五步收尾——槽链 exit 方向照跑(统计 RT、异常都在异步线程侧正确记录),然后栈顶回退。异步任务只需要持有 entry 引用,不依赖任何 ThreadLocal。

被拦(BlockException)时行为不同:不初始化 asyncContext,直接在**当前线程**的 context 上退出(CtSph.java:95-99)——异步调用都没发起,不需要异步 context。

## 二、Tracer:业务异常怎么进统计?

中篇说过 entry.error 字段。它的**主要写入方是 Tracer**——业务代码 catch 到异常后调用:

```java
try {
    // 业务代码
} catch (Exception e) {
    Tracer.trace(e);   // 异常进统计
    throw e;
}
```

trace 的处理链(Tracer.java:67-113):shouldTrace 过滤 → context 非空且非 NullContext → `entry.setError(e)` 挂到当前 entry。此外 StatisticSlot 也会在**被拦**和**内部意外错误**时写入 error(setBlockError / setError,StatisticSlot.java:97, 118)——所以 exit 侧的 `!BlockException` 判断正好把这些"框架自己写的 error"排除在异常统计之外,两头是闭环的。

过滤规则 shouldTrace(节选自 Tracer.java:201-225)是本文最值得看的一段:

```java
protected static boolean shouldTrace(Throwable t) {
    if (t == null || t instanceof BlockException) {
        return false;                       // ① 被规则拦的不算业务异常
    }
    if (exceptionPredicate != null) {
        return exceptionPredicate.test(t);  // ② 自定义谓词优先
    }
    if (ignoreClasses != null) { ... }      // ③ 忽略清单(子类匹配)
    if (traceClasses != null) { ... }       // ④ 指定清单(命中才记)
    return true;                            // ⑤ 默认全记
}
```

注意顺序:全局谓词(exceptionPredicate,通过 `setExceptionPredicate` 设置)优先级最高;没设谓词时,忽略清单和指定清单二选一;全没配 → 默认全记。

挂在 entry.error 上的异常,在 exit 时被统计槽消费(节选自 StatisticSlot.java:135-141, 155-164):

```java
Throwable error = context.getCurEntry().getError();
recordCompleteFor(node, count, rt, error);
recordCompleteFor(context.getCurEntry().getOriginNode(), count, rt, error);
if (resourceWrapper.getEntryType() == EntryType.IN) {
    recordCompleteFor(Constants.ENTRY_NODE, count, rt, error);
}

private void recordCompleteFor(Node node, int batchCount, long rt, Throwable error) {
    ...
    node.addRtAndSuccess(rt, batchCount);
    node.decreaseThreadNum();
    if (error != null && !(error instanceof BlockException)) {
        node.increaseExceptionQps(batchCount);   // 异常数 +1
    }
}
```

业务异常的最终形态是节点上的 **exception QPS**——被 Tracer 挂上的 error 在 exit 侧变成统计数字,再由 dashboard 展示、被异常降级等规则消费。注意 `!BlockException` 判断:被规则拦的调用在 exit 侧**不**记异常数(它已经是"预期中的拒绝",不是业务故障)。

## 三、EntryType:入站还是出站?

上篇说 EntryType 不进 equals、不影响链的唯一性,但它影响两件事——都在统计面。

**第一件:全局入口节点。** Constants.ENTRY_NODE 是名字叫 `__total_inbound_traffic__` 的全局 ClusterNode(Constants.java:45, 66),专记**所有入站流量**的总统计。StatisticSlot 在 pass 和 block 两侧都给 IN 流量记这个节点(StatisticSlot.java:71, 88, 140),exit 侧记 RT/异常(StatisticSlot.java:141)。也就是说:标记 IN 的资源(Web 请求这类"进站"流量)会计入全局入口;标记 OUT 的资源(调用下游 RPC 这类"出站"流量)不计——**全局入口节点 = 入站总量**。

**第二件:系统规则的可拦截性。** 系统保护(SystemRule,如全局 CPU/负载超限时打回入站流量)只针对入站流量,实现上就是一行前置判断(节选自 SystemRuleManager.java:300):

```java
if (resourceWrapper.getEntryType() != EntryType.IN) {
    return;   // 出站流量不做系统规则检查
}
```

入站流量在系统不稳时可以被打回(配合上篇的 BlockException),出站流量是"主动访问",系统规则管不到它。

## 悬念回收:对象携带与统计落点

- **异步生命周期**:initAsyncContext 建独立 context + cleanCurrentEntryInLocal 摘除当前线程 = 生命周期从 ThreadLocal 转移到 Entry 对象;异步线程持 entry 引用,exit 在 asyncContext 上跑完整五步收尾(CtSph.java:84-110 + AsyncEntry.java:38-56, 75-83, 97-98)。
- **异常入统计**:Tracer.trace → entry.error → exit 侧 recordCompleteFor → 节点 exception QPS;BlockException 永不 trace、exit 侧也不计(StatisticSlot.java:135-141, 155-164)。
- **IN/OUT 分工**:IN 计入全局入口节点、可被系统规则拦;OUT 两者皆否(SystemRuleManager.java:300)。

## 本节锚点清单

| 事实 | 锚点 |
|---|---|
| 异步两阶段:建 context + 摘除当前线程 | CtSph.java:84-110, AsyncEntry.java:38-56 |
| initAsyncContext 同名同入口节点 | AsyncEntry.java:63-75 |
| 异步 exit 在 asyncContext 上 | AsyncEntry.java:97-98 |
| 被拦不建 asyncContext | CtSph.java:95-99 |
| shouldTrace 五级过滤 | Tracer.java:201-225 |
| trace 落点 entry.error | Tracer.java:105-113, Entry.java:174; StatisticSlot 也在被拦/内部错误时写 |
| 异常计数消费 | StatisticSlot.java:135-141, 155-164 |
| 全局入口节点 | Constants.java:45, 66 + StatisticSlot.java:71, 140-141 |
| 系统规则只拦 IN | SystemRuleManager.java:300 |