# Ch7 出站与写缓冲区 — ChannelOutboundBuffer 的三指针链表

> Cluster C: 8 KPs | 依赖 §7.2 Handler | §7.2 → §7.3

### 1. ChannelOutboundBuffer 三指针 — flushed→unflushed→tail

场景: `ctx.write(msg1); ctx.write(msg2); ctx.flush()`——两次 write 后一次 flush——两次 write 的数据去哪了? 在 ChannelOutboundBuffer 中累积——`flushedEntry`(已 flush 等待写入 Socket), `unflushedEntry`(已 write 但未 flush), `tailEntry`(链表尾哨兵)。

源码路径: `ChannelOutboundBuffer.java:76-85` — 三指针: `flushedEntry`(已 flush), `unflushedEntry`(已 write 未 flush), `tailEntry`(尾哨兵)。`addMessage(Entry entry)`: 分配到 tail→`touch(msg)` 泄漏追踪→`incrementPendingOutboundBytes`(ChannelOutboundBuffer.java:114-140)。`addFlush()`: `unflushed→flushed` 迁移——unflushed 段标记为 flushed, `setUncancellable+ cancel` 处理(ChannelOutboundBuffer.java:146-170)。`Entry` 内部类使用 `Recycler` 对象池——`pendingSize` 含 96B 对象头开销, 精确内存追踪(ChannelOutboundBuffer.java:826-857)。

关键设计: write 和 flush 的分离——write 只创建 Entry 并放到 unflushed 段——不触发实际 I/O。flush 把整个 unflushed 段迁移到 flushed 段,+调用 `nioBuffers()` 收集 ByteBuffer[], 最后 `doWrite()` 写入 Socket。这个"攒批"设计的核心——TCP_NODELAY 关闭时的 Nagle 算法也会攒包——但 Netty 不依赖 Nagle——它自己在用户态攒包——更可控。

数据流: `write(msg1)`→`addMessage(entry1)`→tail←entry1→`write(msg2)`→`addMessage(entry2)`→tail←entry2→`flush()`→`addFlush()`→unflushed→flushed(entry1+entry2 现在都是 flushed)→`nioBuffers()`→遍历 flushed→收集 ByteBuffer[]→`doWrite(byteBufs)`→writev(fd, iovecs)→`removeBytes(nWritten)`→通知 promise。

### 2. nioBuffers() 零拷贝聚集写 — FastThreadLocal 数组复用

场景: 10 个 write 积攒了 10 个 Entry——每个 Entry 底层是一个 ByteBuf——需要合成一次 `writev` 系统调用。`nioBuffers()` 遍历 flushed 段, 提取每个 Entry 的 `internalNioBuffer()` →填充到 `ByteBuffer[]`→聚合为一个 `iovec` 数组。

源码路径: `ChannelOutboundBuffer.java:414-496` — `nioBuffers(maxCount, maxBytes)`: 遍历 `flushedEntry`→每个 Entry 调用 `internalNioBuffer()`→填充 `ByteBuffer[]`。`NIO_BUFFERS` FastThreadLocal——初始 1024 数组, 不足时 `expandNioBufferArray()` 倍增(ChannelOutboundBuffer.java:67-72,517-534)。`nioBufferCount()` 缓存: count==1→单 buffer 缓存; count>1→多 buffer 数组缓存(ChannelOutboundBuffer.java:471-515)。`progress(amount)`: 调用 `ProgressivePromise.tryProgress(current, total)`——promiseClass 快速路径避免 `instanceof` 类型污染(ChannelOutboundBuffer.java:250-268)。

关键设计: `nioBuffers()` 是"零拷贝"的极限体现——不拷贝数据——只收集各个 Entry 内部的 ByteBuffer 引用——`writev` 系统调用一次发送多个非连续内存块的 `iovec`。`FastThreadLocal` 数组复用让高频写操作(WebSocket 聊天——每秒 10000 次 write)零 GC 压力。

数据流: `flush()`→`nioBuffers(maxCount=16, maxBytes=64KB)`→遍历 flushed 段→每个 Entry→`entry.buf.nioBuffer(index, readable)`→`nioBuffers[i]`→返回 ByteBuffer[3]→`channel.doWrite(iovec[3])`→`writev(fd, [ptr1,len1], [ptr2,len2], [ptr3,len3])`→返回 N 字节已写。

### 3. 高低水位流控 — 32 位 unwritable 掩码

场景: 写操作比读操作快——客户端正在处理上一批数据, TCP 发送缓冲区满了——`channel.write(buf)` 返回 0(非阻塞)——后续 write 应该被暂停, 直到缓冲区重新可写。

源码路径: `ChannelOutboundBuffer.java:180-208` — 高低水位: `totalPendingSize >= highWaterMark`→`setUnwritable(bit0)`; `totalPendingSize < lowWaterMark`→`setWritable(bit0)`。32 位 `unwritable` 掩码: bit0=水位线, bit1-31=31 个用户自定义可写性标志(ChannelOutboundBuffer.java:92-102,631-641)。`setUserDefinedWritability(int index, boolean writable)`——1~31 bit 掩码 CAS(ChannelOutboundBuffer.java:568-616)。`fireChannelWritabilityChanged`: invokeLater 异步 vs 直接触发——task 惰性创建+缓存(ChannelOutboundBuffer.java:644-660)。

关键设计: 水位线不是"超了就停, 低了就写"——中间有个滞后区: highWaterMark > lowWaterMark。这个 hysteresis 防止"写→超水位→停→一字节被消费→低于低水位→写→超水位→停"的震荡——类似于恒温器的温差区间。`setUserDefinedWritability` 让业务层可以定义 31 个独立的可写性通道——如"数据库可写"/"Redis 可写"/"MQ 可写"——通过 `channel.isWritable()&& channel.DB_WRITABLE` 判断。

数据流: `write(largeMsg)`→totalPendingSize 6MB→highWaterMark=64KB→`setUnwritable(0)`→`fireChannelWritabilityChanged(false)`→Handler 停止 write→Socket 写出 3MB→totalPendingSize 3MB→`lowWaterMark=32KB`≤pending→still unwritable→Socket 写出 5.97MB→totalPendingSize 30KB < 32KB→`setWritable(0)`→`fireChannelWritabilityChanged(true)`→Handler 恢复 write。

### 核心悬念

**"ChannelOutboundBuffer 的 write→flush→nioBuffers→doWrite 让数据从 ByteBuf 写到 Socket 的全流程可见。但 write 的 msg 是一个 `ByteBuf`——§7.4 的 ChannelInitializer 在 Channel 创建时, 把 '需要内置的 Handler'(如 HttpServerCodec) 注入 Pipeline 后自动移除自己。而 PendingHandlerCallback 延迟队列解决 'Channel 还没注册到 EventLoop, handlerAdded 不能立即回调' 的异步初始化问题。"**

→ 引出 §7.4 初始化与生命周期 — ChannelInitializer 的 initChannel→finally pipeline.remove(this) 自移除、PendingHandlerCallback 延迟队列、replace(old, new) 的 oldCtx.prev=oldCtx.next=newCtx 缓冲数据 forward——Channel 的生命周期与 Handler 的生命周期的完整交汇。
