# Ch7 Handler 类型体系 — Inbound/Outbound/Duplex 与 @Sharable

> Cluster B: 10 KPs | 依赖 §7.1 链表 | §7.1 → §7.2

### 1. @Sharable + ThreadLocal 缓存 — 多 Channel 共享的 Handler

场景: 10 个 Channel 都需要同样的日志 Handler——为每个 Channel 新建一个 Handler 实例浪费内存。`@Sharable` 标记的 Handler 可以被多个 Channel 的 Pipeline 共享。

源码路径: `ChannelHandler.java:@Sharable` — 注解标记, `ChannelHandlerAdapter.java` — `isSharable()` 用 `ThreadLocal<WeakHashMap<Class<?>, Boolean>>` 缓存反射检查结果(#2289)——避免每次 `addLast` 都走反射。`DefaultChannelPipeline.java:544-553` — `checkMultiplicity()`: 非 `@Sharable` 且已 added→抛 `ChannelPipelineException`。`ensureNotSharable()`: 子类在构造中调用——标记"我这个类不能共享"(ChannelHandlerAdapter.java)。

关键设计: `@Sharable` 不是 JVM 层面的约束——它只是一个注解+运行时检查。如果两个 Handler 在 `handlerAdded` 中维护了 mutable state(如 `private int counter`), 标记 `@Sharable` 后两个 Channel 共享一个实例会 corrupt ——Netty 不阻止你这么做——责任在开发者自己。正确的 `@Sharable` Handler 是无状态的(如 `LengthFieldBasedFrameDecoder`)。

数据流: `@Sharable codec = new HttpServerCodec()` → `pipeline1.addLast(codec)` → checkMultiplicity→`@Sharable`→通过→handlerAdded → `pipeline2.addLast(codec)` → checkMultiplicity→`@Sharable`→通过→handlerAdded(同一个实例, 第二个 Channel 的 pipeline)。

### 2. ChannelInboundHandler — 9 个入站回调 + READ_AUTO 联动

场景: TCP 字节流到达——Channel 变成可读→`channelRead(msg)` 触发→你的 Handler 解码→解码完需要的字节不够→不消费数据, return→下一个 channelRead→继续积累。

源码路径: `ChannelInboundHandler.java` — 9 个回调生命周期: `channelRegistered→channelActive→channelRead→channelReadComplete→channelInactive→channelUnregistered`(主链)。外加 `exceptionCaught`(异常)、`userEventTriggered`(自定义事件)、`channelWritabilityChanged`(可写性变化)。AUTO_READ 联动: `channelReadComplete` 中如果 autoRead=true→`ctx.read()` 自动请求下一次读——不依赖应用代码显式 read。

关键设计: `channelRead` 是最关键的回调——消息不自动释放——Handler 要么处理并释放( `release()` ), 要么转发给下一个 Handler(不释放)。`channelReadComplete` 是 batch 通知——一次 select 可能返回多个就绪 Channel——全部处理完后调用一次 readComplete。

数据流: `select()` 返回→`processSelectedKey()`→`HeadContext.fireChannelRead(msg)`→遍历 inbound Handler→`yourHandler.channelRead(ctx, msg)`→解码→`ctx.fireChannelRead(decoded)`→下一个 Handler→...→`tail.channelRead(unhandled)`→`ReferenceCountUtil.release(unhandled)`(释放)。

### 3. ChannelOutboundHandler — 8 个出站 + write 不 flush

场景: `ctx.write(response)`——写响应给客户端。但 write 只是把数据写入 ChannelOutboundBuffer——不触发 select 写。需要 `ctx.flush()`——把 `ChannelOutboundBuffer` 中累积的数据实际写入 Socket。

源码路径: `ChannelOutboundHandler.java` — 8 个出站操作: `bind/connect/disconnect/close/deregister/read/write/flush`。`read` 是出站——"请求读"——驱动底层 Socket 读, 非"接收数据"。`write` 不 flush——攒批设计——多次 write 后一次 flush 减少 syscall。`ChannelOutboundHandlerAdapter.java` — 8 个方法全部 `@Skip` 转发——子类只需覆盖关心的方法。

关键设计: `read` 是出站操作——概念上"读"和"接收数据"不是一个层面——`read()` = "请求操作系统从 Socket 缓冲区中读数据"——对应的入站回调 `channelRead` = "接收到的数据通知"。`write` 不 flush——10 次 `write(1KB)` → 一次 `flush()` → 一次 `writev(fd, iovec[10])`——Data 没有反复 syscall。

数据流: `ctx.write(msg)` → findContextOutbound(ctx.prev, MASK_WRITE) → 遍历 outbound Handler→编码(msg→bytes)→`ctx.write(encodedMsg)`→HeadContext→Unsafe→`ChannelOutboundBuffer.addMessage(msg)`→等待 flush。`ctx.flush()` → findContextOutbound→`HeadContext.write(buf)`→`ChannelOutboundBuffer.addFlush()`→`doWrite()`→Socket。

### 4. ChannelDuplexHandler + CombinedChannelDuplexHandler

场景: 一些 Handler 需要同时处理入站和出站——如 `HttpServerCodec`——收到请求时解码(入站), 返回响应时编码(出站)。两个独立的 Handler(Inbound 和 Outbound)可以通过 `CombinedChannelDuplexHandler` 伪装为一个 Handler。

源码路径: `ChannelDuplexHandler.java` — `extends ChannelInboundHandlerAdapter implements ChannelOutboundHandler`——入站+出站双向, `isSharable()` 始终 true。`CombinedChannelDuplexHandler.java` — `DelegatingChannelHandlerContext` 双向代理——`inboundCtx` 持有 `inboundHandler` 引用, `outboundCtx` 持有 `outboundHandler` 引用——全量方法代理: 入站事件→delegate→inboundCtx→inboundHandler; 出站事件→delegate→outboundCtx→outboundHandler。

关键设计: CombinedChannelDuplexHandler 让两个独立 Handler 在 Pipeline 中占一个位置——而不是两个位置。`handlerAdded` 调用顺序: 先 `inbound.handlerAdded(inboundCtx)` 再 `outbound.handlerAdded(outboundCtx)`(创建 Ctx 对)。`handlerRemoved` 反过来: 先 outbound 再 inbound——对称。

数据流: `pipeline.addLast(new CombinedChannelDuplexHandler(new HttpRequestDecoder(), new HttpResponseEncoder()))` → 在 Pipeline 中占一个位置 → inbound 事件→delegate→inboundCtx→`HttpRequestDecoder.channelRead(ctx, msg)`; outbound 事件→delegate→outboundCtx→`HttpResponseEncoder.write(ctx, msg, promise)`。

### 核心悬念

**"Handler 的 Inbound/Outbound/Duplex 分类+Adapter 的 @Skip 默认转发让子类只需覆盖关心的方法。但 ChannelHandlerMask 的 17 位掩码预计算让 skipContext 可以跳过不匹配的 Handler 而无需 instanceof 检查。§7.3 的 ChannelOutboundBuffer 三指针链表(flushed→unflushed→tail)和 nioBuffers() 零拷贝聚集写是 write→flush→Socket 的全流程——但 ChannelInitializer 的 initChannel→finally pipeline.remove(this) 自移除模式——是 Pipeline 动态装配的核心: 一次性初始化后自动移除。"**

→ 引出 §7.3 出站与写缓冲区 — ChannelOutboundBuffer 的 flushed→unflushed→tail 三指针链表让 write 不 flush + flush 批量写成为可能。高低水位流控(highWaterMark/lowWaterMark)是 EventLoop 的 ChannelOutboundBuffer 写的反压机制。
