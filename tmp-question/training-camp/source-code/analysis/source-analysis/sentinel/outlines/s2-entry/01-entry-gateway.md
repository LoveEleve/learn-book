# 从一行 entry 说起:入口的门面与闸门

> 本文是 Sentinel 源码分析之"入口域"的上篇。中篇讲 entry 的生命周期(调用栈的起落),下篇讲异步与统计入口。
> 所有行号引用版本: Sentinel 1.8.9。

## 悬念:一行 `SphU.entry()` 背后有几层?

业务代码最常见的写法是:

```java
Entry entry = SphU.entry("doSomething");   // 一行调用
// ... 业务逻辑 ...
entry.exit();
```

这行 `entry()` 会:生成资源包装 → 找或建一条槽链 → 依次跑完链上全部槽的检查(内置 8 槽 + 扩展槽) → 记录统计 → 返回一个 entry 对象。期间如果上下文超限、全局开关关闭、链超限,它还会**静默放行**。本文把这行调用的完整路程走一遍。

## 一、两条触达 API:异常型与布尔型

用户接触 Sentinel 只有两个门:SphU 和 SphO。

**SphU**(368 行)是主力门面,以 `throws BlockException` 的方式把"被拦"显性化:

```
SphU.entry(name)                          SphU.entry(name, EntryType.IN)      // 声明入站
SphU.entry(name, count)                   SphU.entry(name, type, count)       // 批量计数
SphU.entry(Method method, ...)            // 方法资源包装
SphU.entryWithPriority(name[, type])      // 抢占型入口(配合 OccupyTimeout)
SphU.entry(name, resourceType, type[, args])  // 1.7.0 起的分类标签入口
SphU.asyncEntry(name, resourceType, type) // 异步入口
```

20 个公开入口 = 12 个 entry + 6 个 asyncEntry + 2 个 entryWithPriority。参数差异只在:资源表示(name 字符串或 Method)、流量类型(IN/OUT)、批量计数、分类标签、可变参数(args,供热点规则和自定义槽使用)。

**SphO**(226 行)是布尔型门面,把"被拦"折叠成 true/false,专门给"不想写 try/catch"的调用方:

```java
if (SphO.entry("resource")) {
    // 业务代码
    SphO.exit();
} else {
    // 被拦
}
```

它的转译逻辑只有几行(节选自 SphO.java:180-188):

```java
try {
    Env.sph.entry(name, trafficType, batchCount, args);
} catch (BlockException e) {
    return false;          // 被规则拦 → false
} catch (Throwable e) {
    RecordLog.warn("SphO fatal error", e);
    return true;           // 内部意外错误 → 放行!
}
return true;
```

注意最后这个 `catch Throwable → return true`:Sentinel 内部出了意外错误,宁可放行业务,也不把异常吞进业务代码。这与 CtSph 里 `catch Throwable → RecordLog.info`(不抛出)是同一哲学——**框架错误绝不向业务传播**。

## 二、汇聚点:20 个入口 → 两个私有方法

无论从哪个门进来,最终都汇聚到 CtSph 的两个私有方法:

```
SphU.entry(...) ──┐
SphO.entry(...) ──┼──→ CtSph.entryWithPriority(resourceWrapper, count, prioritized, args)    (同步)
                  └──→ CtSph.asyncEntryWithPriorityInternal(resourceWrapper, count, prioritized, args)  (异步)
```

SphU 只是转发的壳(节选自 SphU.java:84-86):

```java
public static Entry entry(String name) throws BlockException {
    return Env.sph.entry(name, 1, OBJECTS0);
}
```

`Env.sph` 是静态单例 `new CtSph()`(Env.java:32,`public static final`——**运行时不可替换**)。Sph 接口的价值在于架构抽象:入口与实现解耦,测试与扩展可以整体替换 Env 类。

CtSph 这一层负责把字符串/方法包装成统一结构(节选自 CtSph.java:313-316 与 344-348):

```java
public Entry entry(String name, EntryType type, int count, Object... args) throws BlockException {
    StringResourceWrapper resource = new StringResourceWrapper(name, type);
    return entryWithPriority(resource, count, false, args);
}

public Entry entryWithType(String name, int resourceType, EntryType entryType, int count, boolean prioritized,
                           Object[] args) throws BlockException {
    StringResourceWrapper resource = new StringResourceWrapper(name, entryType, resourceType);
    return entryWithPriority(resource, count, prioritized, args);
}
```

这里有两个容易被忽略的事实:

**第一,resourceType 只是标签。** 1.7.0 起的 `entry(name, resourceType, ...)` 把业务资源分个类(Web/RPC 等),但这个 int 值既不参与链的查找,也不参与规则匹配——因为 ResourceWrapper 的 equals/hashCode 只按名字(节选自 ResourceWrapper.java:82-94):

```java
public int hashCode() {
    return getName().hashCode();
}

public boolean equals(Object obj) {
    if (obj instanceof ResourceWrapper) {
        ResourceWrapper rw = (ResourceWrapper) obj;
        return rw.getName().equals(getName());
    }
    return false;
}
```

也就是说:**同名资源无论用 String 包装还是 Method 包装、无论标 IN 还是 OUT、无论分类标签是什么,共享同一条槽链、同一套规则。** 这是刻意为之——规则绑的是资源名(FlowRule.resource 是字符串),链绑的也必须是名字,否则"同名规则"和"同名链"会对不上。

**第二,EntryType(IN/OUT)不进 equals,但影响两件事**——见下篇第 4 节。

## 三、默认 context:没有 enter 的调用怎么活?

Sentinel 的完整用法是先 `ContextUtil.enter("contextName")` 再 `SphU.entry(...)`。但很多业务只调用 entry——这时 entry 内部会静默补一个"默认 context":

```java
// 节选自 CtSph.java:125-129
Context context = ContextUtil.getContext();
if (context == null) {
    // Using default context.
    context = InternalContextUtil.internalEnter(Constants.CONTEXT_DEFAULT_NAME);
}
```

默认 context 的名字是 `sentinel_default_context`(Constants.java:40)。两个细节:

1. **为什么用子类 InternalContextUtil 而不是直接调 enter?** 因为公开的 `ContextUtil.enter(name)` 明确拒绝把默认名当自定义名使用(节选自 ContextUtil.java:113-116):

```java
if (Constants.CONTEXT_DEFAULT_NAME.equals(name)) {
    throw new ContextNameDefineException(
        "The " + Constants.CONTEXT_DEFAULT_NAME + " can't be permit to defined!");
}
```

框架自己"走后门":`internalEnter` 直接调 protected 的 `trueEnter`(CtSph.java:248-254),绕过校验。所以 InternalContextUtil 的整个存在意义,就是"框架内部合法地进入默认 context"。

2. **默认 context 是全局共享的。** 第一个线程进入时创建它的 EntranceNode(挂在根节点 ROOT 下),之后所有线程的 entry 都走这同一棵树。没有显式 enter 的调用,都在这棵共享树下统计。

退出时还有个联动:默认 context 会在最后一个 entry 退出时自动被清出 ThreadLocal(中篇第 4 节详述)。

## 四、三道放行闸门:什么时候"不检查"?

现在看 entryWithPriority 的核心(节选自 CtSph.java:117-151):

```java
private Entry entryWithPriority(ResourceWrapper resourceWrapper, int count, boolean prioritized,
                                Object... args) throws BlockException {
    Context context = ContextUtil.getContext();
    if (context instanceof NullContext) {
        // context 超限:只建 entry,不做任何检查
        return new CtEntry(resourceWrapper, null, context);
    }
    if (context == null) {
        context = InternalContextUtil.internalEnter(Constants.CONTEXT_DEFAULT_NAME);
    }
    if (!Constants.ON) {
        // 全局开关关闭:不做任何检查
        return new CtEntry(resourceWrapper, null, context);
    }
    ProcessorSlot<Object> chain = lookProcessChain(resourceWrapper);
    if (chain == null) {
        // 链超限:不做任何检查
        return new CtEntry(resourceWrapper, null, context);
    }
    Entry e = new CtEntry(resourceWrapper, chain, context, count, args);
    try {
        chain.entry(context, resourceWrapper, null, count, prioritized, args);
    } catch (BlockException e1) {
        e.exit(count, args);   // 被拦:entry 自己退出,再抛给调用方
        throw e1;
    } catch (Throwable e1) {
        // 内部意外错误:只记日志,不抛出!
        RecordLog.info("Sentinel unexpected exception", e1);
    }
    return e;
}
```

三道闸门,降级路径全部返回"无链 CtEntry"——entry 照常创建(调用方拿得到对象),只是没有任何规则检查:

| 闸门 | 条件 | 表现 |
|---|---|---|
| 闸门 1 | context 超限(2000) | 无链 CtEntry,一次 WARN |
| 闸门 2 | 全局开关 `Constants.ON = false` | 无链 CtEntry,静默 |
| 闸门 3 | 槽链超限(6000) | 无链 CtEntry,静默 |

闸门 1 和 3 背后是两道防内存膨胀的闸:上下文名上限 2000 与槽链上限 6000(Constants.java:36-37)。注意两者机制不同——上下文超限后,该线程的 contextHolder 被置为 NULL_CONTEXT 单例(带一次警告,节选自 ContextUtil.java:163-173):

```java
private static void setNullContext() {
    contextHolder.set(NULL_CONTEXT);
    if (shouldWarn) {
        RecordLog.warn("[SentinelStatusChecker] WARN: Amount of context exceeds the threshold "
            + Constants.MAX_CONTEXT_NAME_SIZE + ". Entries in new contexts will NOT take effect!");
        shouldWarn = false;
    }
}
```

而链超限只是 `lookProcessChain` 返回 null,连日志都没有。另外注意 `e.exit(count, args)` 在 BlockException 时被调用——**被拦也要正常退出**,保证统计面的 thread 计数和 entry 栈一致(中篇详述)。

## 悬念回收:一行 entry 的完整路程

```
SphU.entry("doSomething")
  └─ Env.sph.entry(...)                     → CtSph
       └─ entryWithPriority(...)
            ① context 超限?  → 无链 CtEntry(放行)      [闸门 1]
            ② 无 context?    → 自动进入默认 context
            ③ 全局开关关?    → 无链 CtEntry(放行)      [闸门 2]
            ④ lookProcessChain: 找/建槽链, 链超限 → null(放行) [闸门 3]
            ⑤ new CtEntry(..., chain, ...) 挂上调用栈
            ⑥ chain.entry(...) 逐个槽检查:
                NodeSelector → ClusterBuilder → Log → Statistic → Authority
                → System → ParamFlow → Flow  (顺序见链构建)
            ⑦ BlockException → e.exit + 抛出;  意外 Throwable → 记日志继续
            ⑧ 返回 entry, 业务代码继续
```

一行调用,八步路程,三道闸门。被拦时它会先自己 exit 再抛异常——entry/exit 的配对性由中篇要讲的调用栈机制保证。

## 本节锚点清单

| 事实 | 锚点 |
|---|---|
| SphO 的 Throwable→true 放行 | SphO.java:180-188 |
| SphU 只做转发 | SphU.java:84-86 |
| resourceType 不参与链 key | ResourceWrapper.java:82-94 |
| 默认 context 名与内部进入 | Constants.java:40, CtSph.java:125-129, 248-254 |
| 默认名不可自定义 | ContextUtil.java:113-116 |
| 三道闸门 | CtSph.java:120-141 |
| 2000/6000 双上限 | Constants.java:36-37 |
| setNullContext 一次性警告 | ContextUtil.java:163-173 |
| Block 时先 exit 再抛 | CtSph.java:150-153 |