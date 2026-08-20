# entry 的生命周期:调用栈的起落

> 本文是 Sentinel 源码分析之"入口域"的中篇。上篇讲了入口的门面与闸门,下篇讲异步与统计入口。
> 所有行号引用版本: Sentinel 1.8.9。

## 悬念:entry 与 exit 怎么保证配对?释放错了会怎样?

Sentinel 要求 entry 与 exit 严格配对,但调用方是人——总会写错。框架用什么机制强制配对?写错了是崩溃还是自愈?本文拆开 entry 对象的诞生与销毁。

## 一、Context:没有栈,只有一个指针

先说 Context(上下文)的数据结构。它只有五个字段(节选自 Context.java:62-79):

```java
private final String name;          // context 名(如 "web-context")
private DefaultNode entranceNode;   // 入口节点(挂在全局树 ROOT 下)
private Entry curEntry;             // 当前 entry(栈顶!)
private String origin = "";         // 调用来源(如消费方应用名)
private final boolean async;        // 是否异步 context
```

**没有栈结构,只有一个 curEntry 指针。** 那"调用栈"在哪?在 CtEntry 的 parent 链上——entry 对象自己连成链,Context 只握栈顶。

为什么这样设计?因为栈操作全部发生在**当前线程**(ThreadLocal 里的 context),每次 entry/exit 都只动栈顶,链表 + 单指针就是最省的操作:entry 是"新节点挂到栈顶",exit 是"栈顶回退"。不用数组不用容量管理。

Context 的节点访问也沿这条链(节选自 Context.java:111-127):

```java
public Node getCurNode() {
    return curEntry == null ? null : curEntry.getCurNode();
}
```

`getLastNode()` 则回退一步:取父 entry 的当前节点,父不存在则退化到 entranceNode(节选自 Context.java:179-186)——这正是"上一个经过的节点"语义,统计槽要它来定位父级统计对象。

## 二、入栈:parent/child 双向挂接

entry 构造时把自己挂到栈顶(节选自 CtEntry.java:56-68):

```java
private void setUpEntryFor(Context context) {
    // The entry should not be associated to NullContext.
    if (context instanceof NullContext) {
        return;
    }
    this.parent = context.getCurEntry();
    if (parent != null) {
        ((CtEntry) parent).child = this;
    }
    context.setCurEntry(this);
}
```

三个动作:记下旧栈顶当 parent → 告诉旧栈顶"你有一个 child 了" → 自己成为新栈顶。嵌套调用:

```
ContextUtil.enter("ctx")  → Context{curEntry=null}
SphU.entry("A")           → A{parent=null, 栈顶=A}
SphU.entry("B")           → B{parent=A, A.child=B, 栈顶=B}
entry("B").exit()         → 栈顶回到 A
entry("A").exit()         → 栈顶回到 null
```

注意 NullContext 的 entry 不挂栈(第一行 return)——降级路径的 entry 是"幽灵 entry",退出时也不需要清理,对应下节的免清理分支。

## 三、出栈:exit 的三态

exit 的入口链是 `exit(count, args) → trueExit → exitForContext`(CtEntry.java:73-74, 139-143)。真正干活的是 exitForContext(节选自 CtEntry.java:90-127),按调用栈状态分三种:

**态 1:NullContext——免清理。** context 是 NullContext 直接 return。幽灵 entry 无栈可退。

**态 2:错序——先扯平,再报错。** 这是最精彩的部分(节选自 CtEntry.java:96-109):

```java
if (context.getCurEntry() != this) {
    String curEntryNameInContext = context.getCurEntry() == null ? null
        : context.getCurEntry().getResourceWrapper().getName();
    // Clean previous call stack.
    CtEntry e = (CtEntry) context.getCurEntry();
    while (e != null) {
        e.exit(count, args);
        e = (CtEntry) e.parent;
    }
    String errorMessage = String.format("The order of entry exit can't be paired with the order of entry"
            + ", current entry in context: <%s>, but expected: <%s>", curEntryNameInContext,
        resourceWrapper.getName());
    throw new ErrorEntryFreeException(errorMessage);
}
```

如果业务代码这样写:

```java
Entry a = SphU.entry("A");
Entry b = SphU.entry("B");
a.exit();   // 错!栈顶是 B,却退了 A
```

框架不会让栈"悬着":先把 B 强制 exit,把调用栈扯平,再抛 ErrorEntryFreeException 通知调用方"你的退出顺序错了,但栈我已经救回来了"。**这是一个自愈型错误**——异常抛给业务代码(不捕获就会在日志里留一条栈),但调用栈已被拉平,后续 entry/exit 照常工作。

**态 3:正常——五步收尾。** (节选自 CtEntry.java:111-126):

```java
if (chain != null) {
    chain.exit(context, resourceWrapper, count, args);   // ① 槽链 exit 方向
}
callExitHandlersAndCleanUp(context);                     // ② 终止回调
context.setCurEntry(parent);                             // ③ 栈顶回退
if (parent != null) {
    ((CtEntry) parent).child = null;
}
if (parent == null) {
    if (ContextUtil.isDefaultContext(context)) {         // ④ 默认 context 自动退出
        ContextUtil.exit();
    }
}
clearEntryContext();                                     // ⑤ 防重复 exit:context 置 null
```

①②的顺序值得注意:槽链的 onExit 钩子先跑(统计 RT、清理线程计数都在这里,见下篇),用户注册的 whenTerminate 回调后跑——注释明确写着 "the exit handlers will be called AFTER onExit of slot chain"(CtEntry.java:79)。whenTerminate 的真实消费者是熔断器:AbstractCircuitBreaker 在 entry 上注册回调,调用结束时结算熔断状态(AbstractCircuitBreaker.java:108)。③把栈顶退回 parent 并断开 child 引用。④是默认 context 的联动退出(下节)。⑤把 entry 的 context 引用置 null——**同一个 entry 退出两次,第二次因 context 已空而静默无操作**,防止重复统计。

## 四、默认 context 的联动退出:一条 2018 年的 bug 修复

上篇说过:没有显式 enter 的调用,自动进入默认 context `sentinel_default_context`。那这个 context 什么时候从 ThreadLocal 消失?答案是:**最后一个 entry 退出时**——正是上面态 3 的第 ④ 步。

`ContextUtil.exit()` 本身很克制(节选自 ContextUtil.java:200-205):

```java
public static void exit() {
    Context context = contextHolder.get();
    if (context != null && context.getCurEntry() == null) {
        contextHolder.set(null);
    }
}
```

只有栈空了才清 ThreadLocal。也就是说:**默认 context 的 ThreadLocal 生命周期 = 最外层 entry 的生命周期**,entry 用完自动回收,用户不需要(也不应该)手动 exit。

这里藏着一段演进故事(temporal-trace.md 有完整考证):0.1.0 时代,这个自动退出是**无条件**的——`parent == null` 就 `ContextUtil.exit()`。后果是:用户显式 enter 了自定义 context、最后一个 entry 退出时,ThreadLocal 被**误清**!下一次 entry 发现 context 没了,又走 internalEnter 落到默认 context——用户的显式调用树被破坏,后续 entry 全部跑错统计树。2018 年的 commit cbaacfda("Bug fix for automatic exit of default context")把它收窄为 `isDefaultContext(context)` 才退出。看 1.8.9 的代码,这个判断还在(CtEntry.java:123-125)——判断条件里那个 `isDefaultContext`,就是这次修复留下的指纹。

## 悬念回收:配对靠 curEntry 校验,错误靠自愈

- **配对性**:Context 只有一个 curEntry 指针,exit 时 `curEntry != this` 即认定错序——这是把"配对"从约定变成强校验。
- **错误处理**:错序不崩溃,先强制扯平调用栈,再抛 ErrorEntryFreeException 通知调用方(态 2);重复 exit 被 context=null 静默吸收(态 3 的 ⑤)。
- **生命周期**:默认 context 随最外层 entry 自动回收(cbaacfda 收窄后的正确语义),用户无需管理 ThreadLocal。

## 本节锚点清单

| 事实 | 锚点 |
|---|---|
| Context 五字段,单 curEntry 指针 | Context.java:62-79 |
| getLastNode 回退语义 | Context.java:179-186, CtEntry.java:147-149 |
| 入栈三动作 + NullContext 不挂栈 | CtEntry.java:56-68 |
| exit 三态 | CtEntry.java:90-127 |
| 错序自愈:先扯平再抛 | CtEntry.java:96-109 |
| 槽链 exit 先于 whenTerminate 回调 | CtEntry.java:79, 111-115 |
| 防重复 exit | CtEntry.java:126, 147-149 |
| ContextUtil.exit 条件清空 | ContextUtil.java:200-205 |
| 自动退出仅限默认 context(演进指纹) | CtEntry.java:123-125 + git cbaacfda |