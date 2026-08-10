## Loop Note: Q1 — Handler 链表结构

**Hypothesis**: AbstractChannelHandlerContext 的双向链表用 volatile next/prev + AtomicIntegerFieldUpdater handlerState 保证可见性——但不加锁，依赖 EventLoop 单线程保证修改安全。

**Verification**:
- `AbstractChannelHandlerContext.java:63-64` — `volatile AbstractChannelHandlerContext next; volatile AbstractChannelHandlerContext prev`
- `line 66-67` — `HANDLER_STATE_UPDATER` (AtomicIntegerFieldUpdater) 管理 handlerState: INIT(0)→ADD_PENDING(1)→ADD_COMPLETE(2)→REMOVE_COMPLETE(3)
- `line 113` — 构造函数: `executionMask = mask(handlerClass)` — 编译期计算掩码，存为 final 字段
- `DefaultChannelPipeline.java:63-64` — `head` + `tail` 两个端点
- `addLast0()` (DefaultChannelPipeline.java:230): `newCtx.prev = tail.prev; newCtx.next = tail; tail.prev.next = newCtx; tail.prev = newCtx`
- `remove()` 后的清理: `setRemoved()` → `prev.next = next; next.prev = prev`

**Code type**: Implementation (lock-free doubly-linked list)

**设计权衡**: volatile 保证 next/prev 修改对所有线程可见——新 handler 添加到链表后，其他 EventLoop 线程 through execute() 能看到更新。不需要全局锁——所有修改操作在 EventLoop 线程上执行（单线程模型无竞态）。AtomicIntegerFieldUpdater 管理 handlerState 生命周期转换——不是 synchronized 块，是 CAS。

**Conclusion**: Pipeline = volatile 双向链表 + CAS 状态机。不加锁——依赖 EventLoop 的单线程模型。source: AbstractChannelHandlerContext.java:63-116
