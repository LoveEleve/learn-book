# Ch10 ByteToMessageDecoder — 积攒与解码循环

> §10.1 → §10.2 | 依赖 Ch9 Bootstrap

### 1. Cumulator — 半包积攒的两种策略

场景: TCP 只传 512B 窗口——一个 1K 的 HTTP 请求被切成两半。第一次 `channelRead(512B)` 只收到一半——积攒到 buffer 等下一半到达再 decode。

源码路径: `ByteToMessageDecoder.java:83-116` — `MERGE_CUMULATOR`: `cumulation.writeBytes(in)` → `finally in.release()`——拷贝积攒, cumulation 空且 in 连续→替换优化(直接返回 in, 免拷贝)。`ByteToMessageDecoder.java:118-161` — `COMPOSITE_CUMULATOR`: `CompositeByteBuf.addComponent(in)`——零拷贝积攒, cumulation 空且 in 是 CompositeByteBuf 且 refCnt==1→复用。`expandCumulation(ctx, alloc, cumulation, in.readableBytes())`: cumulation 空间不足→`alloc.buffer(newCapacity)`→`setBytes` 拷贝→释放旧 cumulation。

关键设计: MERGE vs COMPOSITE——MERGE 适合小消息(HTTP header < 1KB), 拷贝成本低→简单可靠。COMPOSITE 适合大消息(文件传输 > 10MB), 零拷贝但 Composite 的找组件(findComponent)有 O(log N) 开销。`cumulation==in` 替换优化——cumulation 空且 in 满足连续+refCnt==1 条件→直接返回 in 取代 cumulation——零拷贝且零 Composite 开销。

数据流: `channelRead(512B)`→`cumulator.cumulate(cumulation, in)`→MERGE: `cumulation.writeBytes(in)`→`in.release()`→callDecode 循环→读完 512B 不够→return→`channelRead(512B again)`→cumulator 追加→callDecode→1024B 够了→decode→`fireChannelRead(msg)`→out.recycle()。

### 2. callDecode 循环 + 三级状态机防重入

场景: decode 可能一次消费 4096B 产生 10 个 message——每次 decode 后检查仍可读→继续 decode→直到可读空间消耗完毕。

源码路径: `ByteToMessageDecoder.java:464-517` — `callDecode(ctx, cumulation, out)`: `while(cumulation.isReadable())`: 1)`out` 非空→`fireChannelRead(ctx, out, out.size())`→`out.clear()`。2)`decodeRemovalReentryProtection(ctx, cumulation, out)`→subclass decode。3)`out.isEmpty() && oldInputLength == cumulation.readableBytes()`→break(等更多数据)。4)`out` 非空但 input 未变→抛 DecoderException(解码了消息但没消费数据)。三级状态机: `INIT→CALLING_CHILD_DECODE→HANDLER_REMOVED_PENDING`——重入时消息入 `inputMessages` ArrayDeque 队列等待原调用完成(ByteToMessageDecoder.java:163-165)。

关键设计: `callDecode` 的循环终止条件基于"数据是否被消费"——不是"返回 null"——decode 可能产生消息 + 部分消费数据 + return null(仍需更多)→callDecode 继续循环→直到数据不变。`CodecOutputList` Recycler 对象池——`newInstance()`→decode 写入→fireChannelRead→`recycle()`——减少 GC。

数据流: `callDecode`→while→`decode(ctx, in, out)`→out.size()=3→`fireChannelRead(ctx, out, 3)`→out.clear()→in 仍可读→`decode(...)`→out.size()=2→fireChannelRead→out.clear()→in 空→break→`channelRead` 返回。

### 3. discardAfterReads=16 — 防 OOM

场景: 接收了一个 100MB 文件——cumulation 不断增长——readerIndex 推进到 50MB, 但前面 50MB 的弃用数据仍然占着内存。

源码路径: `ByteToMessageDecoder.java:316-321` — `numReads >= discardAfterReads(16)`→`numReads=0`→`discardSomeReadBytes()`。仅当 `cumulation.refCnt()==1` 执行——slice.retain() 场景跳过(派生视图不能让父 buffer release 底层内存)。

关键设计: 16 次 read 的门槛——不是每次都 discard——避免 O(N) arraycopy 频繁触发(见 Ch4 §4.1 discardReadBytes 成本)。refCnt 检查保证安全性——派生视图的 buffer 不能 discard。

数据流: channelRead#1-#16→numReads=16→`discardSomeReadBytes()`→readerIndex=50MB→`readerIndex>=capacity/2`→discard→readerIndex=0, writerIndex=capacity-50MB→numReads=0。

### 核心悬念

**"ByteToMessageDecoder 的 cumulation+callDecode 是 Codec 框架的通用解码骨架——但具体怎么 decode? Ch10 §10.2 的四种拆包器(FixedLength/DelimiterBased/LengthFieldBased/LineBased) 重写 decode() 填充这个骨架——所有拆包器数据不足时 return null → callDecode 自动 break 等更多数据。MessageToByteEncoder 走相反方向——类型匹配→allocate → encode → release 原始 msg → write buf。"**

→ 引出 §10.2 编码器与四种拆包器 — LengthFieldBasedFrameDecoder 的四参数(offset+length+adjustment+strip) + ReplayingDecoder 的 checkpoint(S) + REPLAY Signal 非局部控制流。
