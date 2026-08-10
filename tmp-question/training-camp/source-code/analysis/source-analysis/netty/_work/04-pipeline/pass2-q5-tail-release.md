## Loop Note: Q5 — TailContext 自动释放

**Hypothesis**: TailContext 是 Inbound 事件传播的终点——所有未被处理的 Inbound 消息到达这里被 ReferenceCountUtil.release() 释放。TailContext 只实现 ChannelInboundHandler。

**Verification** (`DefaultChannelPipeline.java`):
- `TailContext` (`line 1264`): `implements ChannelInboundHandler` — 只处理入站
- `channelRead()` (`line 1314-1316`): `onUnhandledInboundMessage(ctx, msg)`
- `onUnhandledInboundMessage(msg)` (`line 1201-1208`): `logger.debug → ReferenceCountUtil.release(msg)` — 总是 finally 释放
- `exceptionCaught()` (`line 1309-1311`): `onUnhandledInboundException(cause)` → `logger.warn → ReferenceCountUtil.release(cause)` — 未处理的异常也释放
- `channelActive/channelInactive` (`line 1283-1290`): `onUnhandledInbound*()` — 各事件终点
- 强制规则: 每个到达 TailContext 的消息都被 finally-release —— 无论 handler 是否消费数据

**Code type**: Implementation (pipeline terminator)

**设计意图**: TailContext = Pipeline 的垃圾收集器。如果用户 handler 没有调 release() → TailContext 兜底释放。防止内存泄漏。ByteBuf 篇一的引用计数部分已讲了这个机制——此处展开实现层。HeadContext.readIfIsAutoRead 在 channelReadComplete 中触发下一次 read —— Head 是新一轮 I/O 循环的起点，Tail 是上一轮数据流的终点。

**结论**: TailContext = finally-release 保证——无论 handler 是否消费数据，ByteBuf 全被释放。不需要用户显式管理 Pipeline 末端的资源释放。source: DefaultChannelPipeline.java:1201-1208,1264-1316
