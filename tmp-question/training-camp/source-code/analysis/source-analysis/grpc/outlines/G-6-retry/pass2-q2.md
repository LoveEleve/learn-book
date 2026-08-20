# 闭环笔记 Q2 — 缓冲记账: perRPC + channel 双层限额, 超限即定案

假设: 重试需要持有已发消息引用, 缓冲内存有双层上限 (单 RPC + 整通道), 超限的代价是"放弃重试能力" (commit)。

验证过程:
- **缓冲触发** (RetriableStream.java:1394-1410): BufferSizeTracer (每 substream 一个) — `outboundWireSize` 时记账; 注释: "A message is sent to the wire, so its reference would be released if no retry or hedging were involved. So at this point we have to hold the reference of the message longer for retry" — **重试的代价: 消息不能被流释放**
- **双层限额** (L1429-1441): `bufferNeeded > perRpcBufferLimit → substream.bufferLimitExceeded = true` (L1429-1430); 否则 **channelBufferUsed.addAndGet** (L1437, ChannelBufferMeter L1456+, 整通道共享 AtomicLong) → `savedChannelBufferUsed > channelBufferLimit → bufferLimitExceeded` (L1437-1438)
- **超限即 commit** (L1442-1446): `if (substream.bufferLimitExceeded) postCommitTask = commit(substream)` — 缓冲超限 = 立即定案当前流 (失去重试机会, 保住内存)
- commit 时释放: `channelBufferUsed.addAndGet(-perRpcBufferUsed)` (L168)
- 字段: perRpcBufferLimit/channelBufferLimit (L92-93, 构造参数 L129)

代码类型: Algorithmic (内存记账)

结论: 缓冲是**有成本的透明性**: 每字节缓冲都有记账 (outboundWireSize 钩子), 双层上限 (perRpc 防单调用爆炸 / channel 防全体重试调用拖垮内存), 超限策略是**立即 commit** (放弃重试换内存安全) — 极端情况下的优雅降级。**被放弃的方案: 无限缓冲** — 恶意/大量重试调用可 OOM; 逐消息丢弃 (不 commit)** — 重放时会丢消息产生错误语义。 [跨域: G-3 ClientCallImpl 内嵌, 缓冲生命周期] [算法: 记账式内存上限] (RetriableStream.java:1394-1458, L92-93,168)
