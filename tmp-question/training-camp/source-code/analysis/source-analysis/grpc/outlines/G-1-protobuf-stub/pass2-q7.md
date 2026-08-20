# 闭环笔记 Q7 — 消息帧格式: 5 字节头 + 压缩标志 + 双重尺寸校验

假设: gRPC 消息帧 = [1B 压缩标志][4B 长度][消息体], 压缩经 Compressor SPI 协商, 尺寸限制在解压前后双重校验。

验证过程:
- grep `HEADER_LENGTH` → MessageDeframer.java:44 `HEADER_LENGTH = 5`; 帧头解析: `readUnsignedByte()` (L384) → `compressedFlag = (type & COMPRESSED_FLAG_MASK) != 0` (L390) → `readInt()` 长度 (L393)
- grep `COMPRESSED` → MessageFramer.java:71-72 `UNCOMPRESSED = 0 / COMPRESSED = 1`; 写头: `headerScratch.put(UNCOMPRESSED).putInt(messageLength)` (L226) / `put(compressed ? COMPRESSED : UNCOMPRESSED)` (L247)
- **双重尺寸校验**: ① 压缩帧长度 > maxMessageSize → "gRPC message exceeds maximum size" (MessageDeframer.java:396) ② 解压后超限 → "Decompressed gRPC message exceeds maximum size" (L529) — 压缩炸弹防护
- **压缩协商**: Compressor SPI (默认 Codec.Identity.NONE, MessageFramer.java:82), setCompressor (L110); 压缩标志位由 `compressed = messageCompression && compressor != Identity.NONE` (L139) 决定; grpc-encoding header 协商 (AbstractClientStream L52 附近, G-3 验证)
- 无可靠方法预知解压后大小 → 边解压边校验 (L412-413 注释: "There is no reliable way to get the uncompressed size per message when it's compressed")
- 上层尺寸: ProtoLiteUtils setSizeLimit(Integer.MAX_VALUE) (q4) — 尺寸限制全权由 MessageDeframer 执行 (maxMessageSize 来自 ClientCall maxInboundMessageSize)

代码类型: Algorithmic (帧协议)

结论: 帧格式 5 字节 (1B 压缩标志 + 4B 长度) 是 gRPC-over-HTTP/2 的消息封装层 — 压缩标志指示消息体是否经 grpc-encoding 协商的压缩器处理; 双重校验 (压缩前/解压后) 防压缩炸弹 (zip bomb); 尺寸限制的唯一执行点在此 (protobuf 层已放开)。 (MessageDeframer.java:44-45,384-425,529; MessageFramer.java:70-82,226-247)
