# Pass 2 闭环笔记 Q3: AsyncEntry — 异步调用如何绕开 ThreadLocal 生命周期

## 初始假设
- 异步入口与同步入口几乎一样,只是返回类型不同。

## 验证过程
- 读 `CtSph.java:64-110` (asyncEntryWithPriorityInternal): 链检查通过后两步关键操作:
  1. `asyncEntry.initAsyncContext()` — 用 `Context.newAsyncContext(entranceNode, name)`(async=true)建**独立异步 Context**,挂 curEntry=this(AsyncEntry.java:63-75)。
  2. `asyncEntry.cleanCurrentEntryInLocal()` — 把 AsyncEntry 从**当前线程** context 栈摘除(curEntry 复原 parent,AsyncEntry.java:38-56),当前线程不被异步调用挂住。
- 读 `AsyncEntry.java:84-88` (trueExit): `exitForContext(asyncContext, ...)` — **在异步 Context 上执行 exit**,槽链 exit 方向照常(统计 RT)。
- 降级路径: `CtSph.java:56-62` (asyncEntryWithNoChain) 三种无链场景同样返回 AsyncEntry。
- Block 路径: `CtSph.java:95-100` — 被拒时 exitForContext(当前线程 context),**asyncContext 不初始化**。
- 语义核心: 同步 Entry 生命周期绑 ThreadLocal;AsyncEntry 把生命周期**挂在 Entry 对象本身**(asyncContext 字段)— 跨线程携带,不依赖调用线程存活。

## 代码类型
- Implementation(线程模型扩展: 对象携带型上下文)

## 跨域关联
- S-2 → S-5: 异步调用的统计在异步线程的 asyncContext 上完成(RT 记录正确归属)
- S-2 → S-8: WebFlux/reactor 适配器依赖 AsyncEntry

## 结论
AsyncEntry = 把 context 从"线程所有"变成"对象所有":entry 通过检查后建独立 asyncContext 并从当前线程栈摘除,异步线程持 Entry 直接 exit(asyncContext 上跑完整槽链 exit)(CtSph.java:64-110 + AsyncEntry.java:38-56, 97-98)。被拒时不建 asyncContext,当前线程正常退出。