# Pipeline 出站 — 数据怎么流出去

## 概念依赖链

```
Q3 (Outbound 传播) → Q6 (HeadContext I/O 触发) → Q7 (Pipeline 动态修改)

Q3 定义"写操作怎么反向传播" → Q6 回答"写出去之后读事件怎么自动重启" → Q7 回答"Handler 链在位怎么安全修改"
```

## 叙事顺序

1. **问题引入** — 从篇一过渡
   - 数据从网络进 Channel(入站) — 数据从应用程序写到网络(出站)
   - Inbound 从 Head→Tail，Outbound 从 Tail→Head 反向传播

2. **Q3：Outbound 传播 — 从 Tail 回到 Head**
   - `AbstractChannelHandlerContext.write()` (`AbstractChannelHandlerContext.java:780-804`): `findContextOutbound(flush ? MASK_WRITE|MASK_FLUSH : MASK_WRITE)` — 写+刷合并优化
   - `flush()` (`AbstractChannelHandlerContext.java:742-771`): `findContextOutbound(MASK_FLUSH)` — 独立 flush 传播
   - 反向遍历: `findContextOutbound()` (`AbstractChannelHandlerContext.java:936-942`) — `do ctx = ctx.prev while skipContext`
   - `HeadContext.write()` (`DefaultChannelPipeline.java:1385-1386`): `unsafe.write(msg, promise)` — 终止点
   - `HeadContext.flush()` (`DefaultChannelPipeline.java:1390-1392`): `unsafe.flush()` — 终止点

3. **Q6：HeadContext — I/O 的自动触发器**
   - `HeadContext.channelReadComplete()` (`DefaultChannelPipeline.java:1433-1437`): `readIfIsAutoRead()` → if autoRead → `channel.read()`
   - `read()` → EventLoop 注册新的 read 操作 → 数据到达 → fireChannelRead → 循环
   - autoRead 控制: `ChannelOption.AUTO_READ` — 默认 true，读完后自动触发下一次读
   - 关闭 autoRead → 手动 `ctx.channel().read()` — 背压机制的基础

4. **Q7：Pipeline 动态修改 — Handler 链的安全编辑**
   - `addLast()` (`DefaultChannelPipeline.java:230`): `newCtx.prev = tail.prev; newCtx.next = tail; tail.prev.next = newCtx; tail.prev = newCtx`
   - `addFirst()` (`DefaultChannelPipeline.java:212`): 插入在 head 之后
   - `remove()`: 通过 `AbstractChannelHandlerContext.setRemoved()` → `prev.next = next; next.prev = prev`
   - 线程安全: 所有修改在 EventLoop 线程中执行——单线程保证无竞态
   - `callHandlerAdded` → handler.handlerAdded(ctx) — handler 收到自己的 Context

5. **收束**: 出站 = 反向传播 + 串联变换 + HeadContext 落盘。读写循环由 HeadContext 的 readIfIsAutoRead 自动重启。Pipeline 所有修改都由 EventLoop 单线程保证安全。

## 核心悬念

**"Pipeline 的写是一个反向传播的递推链——每个 Outbound handler 都能篡改数据，直到 HeadContext 把最终版本交给操作系统。"**
