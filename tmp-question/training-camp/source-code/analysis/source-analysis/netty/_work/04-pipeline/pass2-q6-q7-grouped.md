## Loop Note: Q6+Q7 — HeadContext I/O 触发 + Pipeline 动态修改

### Q6: HeadContext I/O 自动循环

**Hypothesis**: HeadContext.channelReadComplete 中 readIfIsAutoRead 自动触发下一次数据读取——形成"读→处理→再读"的无限循环，autoRead 关闭即暂停。

**Verification** (`DefaultChannelPipeline.java`):
- `HeadContext.channelReadComplete()` (`line 1433-1437`): `ctx.fireChannelReadComplete() → readIfIsAutoRead()`
- `readIfIsAutoRead()` (`line 1439-1443`): `if (channel.config().isAutoRead()) → channel.read()` — 通过 ChannelOption.AUTO_READ 控制
- autoRead 默认 true — 数据到达→处理→自动触发下一次 read
- autoRead=false — 手动调 `channel.read()` 触发下一轮 → 背压机制基础 (Channel 暂停读取直到上层处理完毕)
- `HeadContext.read()` (`line 708`): `unsafe.beginRead()` — 注册 OP_READ 到 Selector

### Q7: Pipeline 动态修改

**Hypothesis**: addLast/addFirst/remove 全部在 EventLoop 线程执行——单线程保证无竞态。addLast0 修改三个节点的指针链。

**Verification** (`DefaultChannelPipeline.java`):
- `addLast0()` (`line 230`): `newCtx.prev = tail.prev; newCtx.next = tail; tail.prev.next = newCtx; tail.prev = newCtx` — 插入在 tail.prev 之后
- `callHandlerAdded`: handler.handlerAdded(ctx) — handler 收到通知，开始处理事件
- `remove()`: `AbstractChannelHandlerContext.setRemoved()` → `prev.next = next; next.prev = prev` → `callHandlerRemoved` → handler.handlerRemoved(ctx)
- 线程安全: EventLoop 单线程 — addLast/remove 永远在此线程执行。volatile next/prev 保证其他线程通过 execute() 看到可见性

**Code type**: Implementation

**Conclusion**: HeadContext = readIfIsAutoRead 形成 I/O 自动循环。autoRead 开关 = 背压机制。Pipeline 修改 = EventLoop 单线程安全的双向链表操作。source: DefaultChannelPipeline.java:230,1433-1443
