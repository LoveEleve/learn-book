# Ch10 编码器与四种拆包器 — encode→write 与 decode→frame

> §10.1 → §10.2 | 依赖 §10.1

### 1. LengthFieldBasedFrameDecoder — 四参数最复杂拆包

场景: 协议帧 `[2B length][4B header][payload]`——先读 2 字节长度, 计算总帧长, 再读全部 payload。

源码路径: `LengthFieldBasedFrameDecoder.java:189-196` — 四参数: `lengthFieldOffset`(长度字段在帧内的偏移, 默认 0) + `lengthFieldLength`(长度字段的字节数, 1/2/3/4/8) + `lengthAdjustment`(总帧长补偿值, 含 header 长度) + `initialBytesToStrip`(跳过的头部字节数, 让调用方直接拿到 payload)。两级状态: `frameLengthInt==-1` 状态→先读 `lengthFieldEndOffset=offset+length` 字节→`getUnadjustedFrameLength()` 解析长度→计算 `frameLengthInt = length+adjustment+lengthFieldEndOffset`。`frameLengthInt≠-1` 状态→仅检查 `readableBytes >= frameLengthInt`。三种 CorruptedFrameException: 负长度 / 调整后<endOffset / 长度<strip 量。

关键设计: 两级状态避免重复解析长度字段——一级解析(读到长度+计算)→二级等待(数据就绪)。`lengthAdjustment` 是"长度字段的值不包含自身"场景的补偿——如 `[2B len][4B header][payload]`——长度值=payload 长度(4), 但 total frame=2+4+4=10→adjustment=6(2B len + 4B header)。`initialBytesToStrip` 实现"业务 Handler 只看到 payload"——让调用方不需要知道长度字段的存在。

数据流: `decode(in,...)`→第一次: readableBytes<2→return null→第二次: readableBytes≥2→`getUnadjustedFrameLength(0,2)`→100→`frameLengthInt = 0+2+100+0 = 102`→readableBytes<102→return null→第三次: readableBytes≥102→`initialBytesToStrip(2)`→in.skipBytes(2)→`in.retainedSlice(in.readerIndex(), 102-2)`→返回 payload ByteBuf。

### 2. MessageToByteEncoder — 对象→字节 → write

场景: 业务产生了 `HttpResponse` 对象——需要编码成字节流发给客户端。

源码路径: `MessageToByteEncoder.java:99-131` — 五步: 1)`acceptOutboundMessage(msg)` 类型匹配(泛型 I); 2)`allocateBuffer(ctx, msg, preferDirect)`→分配 `ByteBuf`; 3)`encode(ctx, cast, buf)`→子类实现编码; 4)`finally { ReferenceCountUtil.release(cast) }`→释放原始 msg(生命周期结束); 5)`buf.isReadable()`→`ctx.write(buf, promise)`→busy 的路由: 空 buf→释放+写 `EMPTY_BUFFER`(promise 完成)。

关键设计: `preferDirect=true` 默认——`alloc.ioBuffer()` 返回 DirectByteBuf——编码后的字节经过 `write()` 直接到 Socket(零 JNI 拷贝)。Ch1§1.2 的 Heap→JNI 拷贝正是编码器选择 Direct 的原因。

数据流: `write(new DefaultFullHttpResponse(HTTP_1_1, OK, body))`→`acceptOutboundMessage(msg)`∈HttpResponse→accept→`alloc.ioBuffer()`→`encode(ctx, msg, buf)`→HttpResponseEncoder 写 `HTTP/1.1 200 OK\r\n...`→`release(msg)`→`ctx.write(buf)→flush`。

### 3. ReplayingDecoder — 状态机 + REPLAY Signal

场景: 协议帧 `[1B type][4B len][payload]`——先读 type→switch(type) 决定后续解析逻辑。用 if-else 检查 `readableBytes>=1` 冗长且容易漏检查。

源码路径: `ReplayingDecoder.java:293-406` — `checkpoint(S state)`: 保存 `internalBuffer().readerIndex()` + 更新 `state`→后续读失败可回退。`REPLAY Signal`: `ReplayingDecoderByteBuf` 代理包装真实 buffer——读不足→抛 `Signal REPLAY`→`callDecode` catch→`in.readerIndex(checkpoint)` 回退→break。`decode` 只需读——不用检查 `readableBytes`——不够时自动回退。死循环防护: decode 后 `readerIndex` 不变&&`state` 不变→抛 DecoderException。

关键设计: REPLAY Signal 是 Erlang Actor 的非局部控制流——用异常做正常流程——不是 error 而是"稍后再试"的控制信号。和 `ByteToMessageDecoder.callDecode` 的 return null 等价——但 REPLAY 版本让 decode 代码更简洁——不需要在每处 read 前做边界检查。

数据流: `decode(ctx, replayBuf, out)`→`replayBuf.readByte()`→type=0x01→`checkpoint(State.READ_LENGTH)`→`replayBuf.readInt()`→len=4096→`checkpoint(State.READ_PAYLOAD)`→`replayBuf.readBytes(len)`→不够→抛 REPLAY Signal→callDecode catch→readerIndex 回退到 READ_LENGTH position→break→等数据→`channelRead(more data)`→callDecode→从 READ_PAYLOAD 开始→readBytes(len) 成功→`out.add(new Frame(type, payload))`→`checkpoint(State.READ_TYPE)`。

### 四种拆包器共性: return null

所有拆包器重写 `decode(ctx, in, out)`——数据不足时 return null。`callDecode` 检测 `out.isEmpty() && in.readableBytes() 未变`→break 自动等 cumulator 积攒——拆包器不需要管理积攒缓冲区。子类只需: 解析帧边界→够了→`out.add(frame)`; 不够→return null。

→ 引出 Ch11 HTTP — 四种拆包器覆盖了 TCP 流的所有边界问题。HTTP/1.1 在 Codec 框架之上的实现——HttpRequestDecoder/HttpResponseDecoder 继承 ByteToMessageDecoder→HttpObjectAggregator 把分块的 HttpContent 拼成完整消息→HttpContentCompressor 根据 Accept-Encoding 协商压缩。
