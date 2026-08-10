# Pipeline 入站 — 数据怎么流进来

## 概念依赖链

```
Q1 (双向链表) → Q2 (Inbound 传播) → Q5 (TailContext 释放)

Q1 定义"处理器怎么链在一起" → Q2 回答"事件怎么向前传播" → Q5 回答"走到尽头谁来收拾"
```

## 叙事顺序

1. **问题引入** — 从 Ch6 Promise/Future 过渡
   - Promise/Future 给出了异步结果传递——但中间的每一步是谁在处理数据？
   - Pipeline = 数据流水线: 入站 (read) 从 Head→Tail, 出站 (write) 从 Tail→Head

2. **Q1：双向链表 — 处理器的骨架**
   - `AbstractChannelHandlerContext.next` / `prev` (`AbstractChannelHandlerContext.java:63-64`) — volatile 双向链表
   - `DefaultChannelPipeline` (`DefaultChannelPipeline.java:45`): `head` (HeadContext) + `tail` (TailContext) 两个端点 (`DefaultChannelPipeline.java:63-64`)
   - 构建: `new DefaultChannelPipeline(channel)` (`DefaultChannelPipeline.java:96-97`) → head.next=tail, tail.prev=head — 空管道就是两个端点
   - `addLast()` (`DefaultChannelPipeline.java:221-230`): 插入在 tail.prev 之后 → 更新三个节点的 prev/next 指针 — 线程安全由 EventLoop 保证

3. **Q2：Inbound 传播 — 从 Head 到 Tail**
   - `HeadContext.channelRead()` (`DefaultChannelPipeline.java:1428-1429`): `ctx.fireChannelRead(msg)` — 数据从 head 出发
   - `AbstractChannelHandlerContext.fireChannelRead()` (`AbstractChannelHandlerContext.java:341-366`):
     - `findContextInbound(MASK_CHANNEL_READ)` (`AbstractChannelHandlerContext.java:927-933`) → 找下一个实现了 channelRead 的 handler
     - `skipContext()` (`AbstractChannelHandlerContext.java:945-954`) — 两层判断: (1) handler 不实现任何 Inbound 事件 → skip; (2) 同一 executor 且 handler 不实现此具体事件 → skip (不同 executor 不可跳)
     - 同一 EventLoop → `next.invokeHandler()` → `handler.channelRead(ctx, m)` (`AbstractChannelHandlerContext.java:345-358`, 三种类型分发: HeadContext/DuplexHandler/InboundHandler); 不同 → `execute(Runnable)`
   - ChannelHandlerMask.MASK_CHANNEL_READ (`ChannelHandlerMask.java:44`) = `1<<5`

4. **Q5：TailContext — 最后的收拾者**
   - `TailContext.channelRead()` (`DefaultChannelPipeline.java:1314-1316`): `onUnhandledInboundMessage(ctx, msg)`
   - `onUnhandledInboundMessage()` (`DefaultChannelPipeline.java:1201-1208`): `ReferenceCountUtil.release(msg)` → 释放未消费的 ByteBuf
   - 为什么只在 TailContext 释放？Head→Tail 传播结束后，最后一个 handler 的 channelRead 不调 fireChannelRead → 事件停在原地。只有到达 TailContext 的消息是"没人要的"

5. **收束**: 从 Channel 接收数据 → HeadContext.fireChannelRead → 跳过 Outbound handler → 用户 handler 消费或传递 → TailContext 释放未消费

## 核心悬念

**"Pipeline 不像 List——每个 Handler 不知道下一个是谁。Context 替你找了下一个，你却不知道它是谁。"**
