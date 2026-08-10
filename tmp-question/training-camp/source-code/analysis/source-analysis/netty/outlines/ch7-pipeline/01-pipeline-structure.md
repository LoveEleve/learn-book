# Ch7 双向链表与事件传播 — Pipeline 的骨架

> Cluster A: 12 KPs | 依赖 Ch6 Promise | Ch7 → §7.2

### 1. HeadContext + TailContext — 哨兵节点的双向环

场景: Ch4-Ch6 的所有基础设施(ByteBuf/EventLoop/Promise)都就绪了——现在要把十余个 Handler 串成一个处理链。空链(没有任何 Handler)也需要能工作——Head 和 Tail 作为哨兵节点保证访问永远不会 NPE。

源码路径: `DefaultChannelPipeline.java:63-101` — 构造时创建 `HeadContext` 和 `TailContext`, `head.next=tail, tail.prev=head`——空链的闭环。`HeadContext`: 出站终点——`bind/connect/close/write/flush→Unsafe` (DefaultChannelPipeline.java:1324-1454)。`HeadContext`: 入站起点——`channelRegistered→invokeHandlerAddedIfNeeded+fireChannelRegistered` (DefaultChannelPipeline.java:1400-1403)。`TailContext`: 入站终点——`channelRead→onUnhandledInboundMessage`(释放) (DefaultChannelPipeline.java:1264-1322)。

关键设计: 双向链表 + 哨兵节点让边界条件统一——head 是出站操作的终点(最终由 Unsafe 执行到 OS), tail 是入站事件的终点(未被任何 Handler 消费的消息在 tail 释放)。没有哨兵, 每次事件传播都要检查"下一个 Handler 是 null 吗?"——null check 在 10 个 Handler 的链上就是 20 次——哨兵把 null check 变成一次 `ctx.next == tail` 比较。

数据流: `pipeline.fireChannelRead(msg)` → `AbstractChannelHandlerContext.invokeChannelRead(head, msg)` → `findContextInbound(head.next, mask)` → 找到第一个 `ChannelInboundHandler` → 调用 `handler.channelRead(ctx, msg)` → handler 处理完后 `ctx.fireChannelRead(msg)` → 继续传播 → 直到 `ChannelInboundHandler`→ `tail`(释放未处理 msg)。

### 2. internalAdd + AddStrategy — 四种插入统一入口 + 自动命名

场景: `pipeline.addFirst("encoder", new HttpServerCodec())`——精确控制 Handler 在链中的位置。Builder 模式: `addLast`(附加到末尾), `addFirst`(插入头部), `addBefore/addAfter`(在某个 Handler 前后插入)。

源码路径: `DefaultChannelPipeline.java:154-205` — `internalAdd(AddStrategy, String name, ChannelHandler, EventExecutorGroup)`——四种插入统一入口, `AddStrategy` 枚举(ADD_FIRST/ADD_LAST/ADD_BEFORE/ADD_AFTER) 统一操作, `synchronized` 保护 Pipeline 结构修改。`addFirst0`: `head↔newCtx↔oldHead.next`——在 head 之后插入 (DefaultChannelPipeline.java:212-217)。`addLast0`: `tail.prev↔newCtx↔tail`——在 tail 之前插入 (DefaultChannelPipeline.java:230-236)。`generateName`: 未指定 name→自动生成 `"ClassName#0"`, FastThreadLocal+WeakHashMap 缓存类名映射——冲突时追加 #1、#2 (DefaultChannelPipeline.java:336-362)。`checkMultiplicity`: 非 `@Sharable` 且已 added 的 Handler→抛 ChannelPipelineException (DefaultChannelPipeline.java:544-553)。

关键设计: `synchronized` 保护 Pipeline 结构修改——但事件传播(不修改结构)是无锁的。`addFirst0/addLast0` 只重连 2-4 个指针——O(1)。自动命名的 `FastThreadLocal` 缓存让 `Class.getSimpleName()` 不走反射——`WeakHashMap` 的 key 是 WeakReference——Class 被卸载时自动清理。

数据流: `pipeline.addLast("codec", codec)` → `generateName("codec")` → new DefaultChannelHandlerContext(codec, "codec") → `synchronized(this) { internalAdd(ADD_LAST, codecCtx) }` → `addLast0(codecCtx)` → tail.prev=codecCtx, codecCtx.next=tail, codecCtx.prev=oldTailPrev → `callHandlerAdded(codecCtx)` → Channel 已注册→`handlerAdded(this)`; 未注册→`callHandlerCallbackLater`(§7.4)。

### 3. findContextInbound/Outbound + skipContext — 事件传播的方向

场景: `ctx.fireChannelRead(msg)`——"下一个" Handler 是谁? 不是简单 `ctx.next`——要跳过所有非 `ChannelInboundHandler` (outbound 不应该收到 inbound 事件)。而且如果下一个 handler 的 executor(fixed thread) 和当前不同——不能跳过(必须 submit 过去)。

源码路径: `AbstractChannelHandlerContext.java:927-933` — `findContextInbound(mask)`: 从 `ctx.next` 出发, 循环 `skipContext(ctx.next, mask)`, 找到第一个匹配 mask 的入站 Handler。`AbstractChannelHandlerContext.java:936-943` — `findContextOutbound(mask)`: 从 `ctx.prev` 出发, 循环跳过, 找到出站 Handler。方向: inbound 沿 `next`(底→顶), outbound 沿 `prev`(顶→底) (ChannelPipeline.java:89-123)。`skipContext(ctx, mask)`: 跳过的条件是 `(ctx.executionMask & mask) == 0` 且 `ctx.executor() == currentExecutor`——同 executor 且掩码不匹配才跳过; 不同 executor 不跳过(必须 submit 到另一个线程) (AbstractChannelHandlerContext.java:945-953)。

关键设计: skipContext 的两个条件(掩码匹配 && 同 executor)体现了"性能优化 vs 正确性"的权衡——同 executor 且不需要处理该事件的 Handler 可以直接跳过, 零成本。但不同 executor 不能跳过——因为事件必须被传递到另一个线程, 即使那个 Handler 最终可能也不处理这个事件——线程边界需要显式 submit。

数据流: `ctx.fireChannelRead(msg)` → `findContextInbound(ctx.next, MASK_CHANNEL_READ)` → 跳过 outbound Handler(掩码不匹配 && same executor)→跳过已完成 Handler(executionMask不匹配)→找到下一个 Inbound Handler→`invokeHandler(next, msg)` → `handler.channelRead(nextCtx, msg)`。

### 4. handlerState 4 态 FSM + invokeHandler 保护

场景: Handler 被添加到 Pipeline 但不是立即生效——handlerAdded 回调需要在合适的线程上执行(Channel 注册后)。在 handlerAdded 完成前——Handler 不应该处理事件——INIT→ADD_PENDING→ADD_COMPLETE。

源码路径: `AbstractChannelHandlerContext.java:69-85` — handlerState 4 态: `INIT(0)→ADD_PENDING(1)→ADD_COMPLETE(2)→REMOVE_COMPLETE(3)`——`AtomicIntegerFieldUpdater` 原子更新。`AbstractChannelHandlerContext.java:1013-1016` — `invokeHandler()` 保护: `handlerState==ADD_COMPLETE` 或 `(!ordered && ADD_PENDING)` 才调用 Handler 方法——未就绪时仅转发事件跳过。`AbstractChannelHandlerContext.java:985-991` — `callHandlerAdded()`: 先 `setAddComplete()` 再 `handlerAdded(this)`——保证 handlerAdded 内产生的 Pipeline 事件能被正常处理。

关键设计: 状态机防重入——handlerAdded 时如果 Handler 内部又调了 `pipeline.remove()`——Handler 此时在 ADD_COMPLETE 态, remove 会检查状态→正常执行 handlerRemoved。如果 handlerAdded 抛异常→catch 中 `callHandlerRemoved+fireExceptionCaught`(§7.4)。

数据流: `pipeline.addLast(handler)` → setAddPending() → Channel 未注册 → PendingHandlerCallback 延迟队列。Channel 注册 → `invokeHandlerAddedIfNeeded()` → `callHandlerAdded()` → `setAddComplete()` → `handlerAdded(this)` → handler 可以 `ctx.write(msg)`。`callHandlerRemoved()` → `handlerRemoved(this)` → `setRemoved()`。

### 核心悬念

**"Pipeline 的双向链表+Head/Tail哨兵定义了事件传播的骨架——inbound 沿 next、outbound 沿 prev。但具体的事件处理逻辑——channelRead 做什么、write 做什么——是由 Handler 类型决定的。InboundHandler、OutboundHandler、DuplexHandler 三种类型——Adapter 的 @Skip 默认转发让子类只需覆盖一个方法。§7.2 的 SimpleChannelInboundHandler 用 TypeParameterMatcher + autoRelease 解决了 '消息类型匹配' 和 '内存释放' 两个痛点。"**

→ 引出 §7.2 Handler 类型体系 — ChannelInboundHandler 的 9 个入站回调(lifecycle: reg→active→read→readComplete→inactive→unreg)和 ChannelOutboundHandler 的 8 个出站操作(write 不 flush)的区别——Handler 如何在 Pipeline 中找到自己的角色。
