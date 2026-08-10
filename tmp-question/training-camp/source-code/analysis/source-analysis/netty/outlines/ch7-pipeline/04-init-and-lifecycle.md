# Ch7 初始化与生命周期 — ChannelInitializer 自移除与动态装配

> Cluster D: 6 KPs | 依赖 §7.3 写缓冲 | §7.3 → §7.4

### 1. ChannelInitializer 自移除模式 — 一次性初始化然后自己清除

场景: `ServerBootstrap.childHandler(new ChannelInitializer<SocketChannel>() { initChannel(ch) { ch.pipeline().addLast(codec); } })`——每个新 Channel 被 accept 后, ChannelInitializer 把 codec 注入 Pipeline, 然后移除自己——不需要永久占据 Pipeline 位置。

源码路径: `ChannelInitializer.java:125-141` — `initChannel(ctx)`: `initMap.add(ctx)` 防重入→`initChannel(C)` 回调→`finally { pipeline.remove(this) }` 自移除。`ChannelInitializer.java:53-59` — `@Sharable` + `ConcurrentHashMap.newKeySet()` initMap——多 Channel 共享同一实例(每个 Channel 独立防重入)。`handlerAdded`: Channel.isRegistered()? → `initChannel(ctx)→removeState`; 否则→等待 `channelRegistered` 再触(ChannelInitializer.java:105-117)。`removeState()` 异步兼容: ctx 已移除→直接 remove; 未移除→`ctx.executor().execute()` 延迟清理(ChannelInitializer.java:143-157)。

关键设计: ChannelInitializer 的 "用完即毁" 模式让 initChannel 回调中的代码可以任意添加 Handler——不需要担心多次添加(initMap 防重入)。`@Sharable` 让一个 ChannelInitializer 实例可以服务多个 Channel(Server 的 child Channel)——ConcurrentHashMap 隔离——每个 Channel 只会被它初始化一次。

数据流: `serverBootstrap.bind()`→client 连接→`accept()`→新 channel→`pipeline.addLast(channelInitializer)`→`channel registered`→`handlerAdded`→`initChannel(channel)`→`channel.pipeline().addLast(codec, handler, ...)`→`finally { pipeline.remove(this) }`→ChannelInitializer 离开。

### 2. PendingHandlerCallback — 延迟回调队列

场景: Channel 刚创建, 还没注册到 EventLoop——`pipeline.addLast(handler)`——`handlerAdded` 应该立即回调吗? 不能——Handler 可能在 `handlerAdded` 中调 `ctx.write()`——但 Channel 还没绑定到 EventLoop——`write` 需要 EventLoop 线程。

源码路径: `DefaultChannelPipeline.java:1141-1154` — `pendingHandlerCallbackHead` 单链表——Channel 未注册时 Handler 添加不立即回调。`callHandlerCallbackLater(newCtx, true)`——尾部追加(遍历到链表尾 append)。`handlerAdded→PendingHandlerAddedTask`, `handlerRemoved→PendingHandlerRemovedTask`。`DefaultChannelPipeline.java:593-601` — `invokeHandlerAddedIfNeeded()`——首次 `channelRegistered` 时批量执行所有 pending 回调。`DefaultChannelPipeline.java:1118-1138` — `callHandlerAddedForAllHandlers()`——在 `synchronized` 外遍历执行——防止 handlerAdded 内调用 `addHandler` 导致死锁。

关键设计: PendingHandlerCallback 是"注册就绪"和"调用就绪"的时间差解决方案——Pipeline 的 add 操作可以在 Channel 创建后、register 前批量执行(如 ChannelInitializer 在 initChannel 中 batch add 5 个 Handler)。`synchronized` 外遍历执行是死锁预防——如果 handlerAdded 回调中又调了 `addHandler`——synchronized 内遍历会死锁——外遍历允许重入。

数据流: `pipeline.addLast(h1)`→Channel 未注册→`callHandlerCallbackLater(h1, true)`→pendingHandlerCallbackHead→h1→`pipeline.addLast(h2)`→`callHandlerCallbackLater(h2, true)`→pendingHead→h1→h2→...→`channelRegistered`→`invokeHandlerAddedIfNeeded()`→`callHandlerAddedForAllHandlers()`→遍历: `callHandlerAdded(h1)`→`callHandlerAdded(h2)`→...→全部 handler 就绪。

### 3. replace(old, new) — 旧 Handler 缓冲数据 forward

场景: Hot 部署——应用修改了某个 Handler 的配置, 需要用新实例替换旧实例。但旧 Handler 上还有正在处理的缓冲数据——不能直接 remove——需要"无缝切换"。

源码路径: `DefaultChannelPipeline.java:474-524` — `replace(old, new)`: 先 `callHandlerAdded0(newCtx)` 再 `callHandlerRemoved0(ctx)`——原因: `handlerRemoved` 可能触发 `channelRead/flush` 到新 Handler——新 Handler 必须先就绪。`DefaultChannelPipeline.java:526-541` — `replace0(oldCtx, newName, newHandler)`: prev/next 重连 + `oldCtx.prev=oldCtx.next=newCtx`——缓冲数据 forward——让 `oldCtx` 持有的缓冲数据能正确 forward 到替换后的新 Handler。

关键设计: `oldCtx.prev=oldCtx.next=newCtx` 是最 subtle 的部分——oldCtx 被 remove 但还没被 GC——如果有 event 恰好传播到 oldCtx(prev/next 还连着链), oldCtx 会通过 `next`(现在指 newCtx)forward 到新 Handler——不丢事件。`callHandlerAdded0` 异常保护: 抛异常→`atomicRemoveFromHandlerList+callHandlerRemoved`(DefaultChannelPipeline.java:556-581)。

数据流: `replace(oldCodec, "newCodec", newCodec)`→`callHandlerAdded0(newCtx)`→`callHandlerRemoved0(oldCtx)`→`replace0`→oldCtx.prev.next=newCtx, newCtx.prev=oldCtx.prev, oldCtx.prev=newCtx, oldCtx.next=newCtx→如果此时有 `ctx.fireChannelRead(msg)` 传到 oldCtx→oldCtx.next=newCtx→forward 到 newCtx→新 Handler 处理。

### 4. destroy() 两阶段 — destroyUp + destroyDown

场景: Channel 关闭——Pipeline 上的所有 Handler 需要依次调用 `handlerRemoved()`——但有些 Handler 在 A EventLoop 线程上, 有些在 B EventLoop 线程上——不能在一个线程上逐个同步 wait。

源码路径: `DefaultChannelPipeline.java:800-856` — `destroy()`: `destroyUp(head.next→tail)`——向上遍历, 提交不同 executor 的任务; `destroyDown(tail.prev→head)`——向下 `atomicRemoveFromHandlerList+callHandlerRemoved0`。`childExecutor()` 实现 EventExecutor pinning——`IdentityHashMap<EventExecutorGroup, EventExecutor>` 缓存(DefaultChannelPipeline.java:122-143)。`ChannelUnregistered` 时触发: `channel.isOpen()==false → destroy()`(DefaultChannelPipeline.java:1406-1413)。

关键设计: destroy 两阶段——destroyUp 向各个 EventLoop 提交 remove 任务(异步, 不阻塞当前线程); destroyDown 等待所有任务完成后依次移除。`childExecutor()` 的 IdentityHashMap 缓存——同一 Group 的 Handler 绑定到同一个 EventExecutor——保证 handlerRemoved 的顺序性(同一 Group 的 Handler, handlerRemoved 顺序与 handlerAdded 相反)。

数据流: `channel.close()`→`ChannelUnregistered`→`destroy()`→`destroyUp`: 遍历 ctx 链→不同 executor→`executor.execute(()-> remove(ctx))`→所有任务完成(CountDownLatch)→`destroyDown`: 从 tail 向 head→`atomicRemoveFromHandlerList(ctx)`→`callHandlerRemoved0(ctx)`→所有 handlerRemoved 回调完成→Pipeline 空→GC。

### 核心悬念

**"Pipeline 的动态装配(ChannelInitializer 自移除)、延迟初始化(PendingHandlerCallback)、平滑替换(replace forward)、两阶段清理(destroyUp+destroyDown)——让 10 个 Handler 的添加、初始化和销毁全流程在 EventLoop 的单线程模型下安全完成。但 §7.1-§7.4 的 Pipeline 回答的是 '事件如何传播和 Handler 如何管理'——数据本身是什么? 是 Ch4 ByteBuf(get/put/retain/release 的内存模型)。而 EventLoop(Ch5) 的单线程保证了 Pipeline 传播不需要锁。"**

→ 引出 Ch8 MemoryPool — Pipeline 中的每个 write 的 msg 是一个 ByteBuf。1 万次 write = 1 万个 ByteBuf = 分配/释放 1 万次。Ch8 的 PoolArena(Buddy 分配)+PoolChunk(handle 编码)+PoolThreadCache(thread-local 缓存) 让 ByteBuf 复用——分配→借用→归还→再分配——一次 `UNSAFE.allocateMemory` 服务于千百次 write。
