# Ch12 HTTP/2 编解码 — 二进制帧 + Stream 多路复用 + HPACK + Flow Control

> 依赖 Ch11 HTTP/1.1 | 🟡B 1 篇 | 跨框架桥: gRPC + Dubbo Triple

**背景**: HTTP/1.1 的 6 个并行连接限制 + 队头阻塞（一个请求慢 → 后续请求全部被堵）。HTTP/2 用一条 TCP 连接承载 N 个并发 Stream — 没有连接的 6 个限制、没有队头阻塞。Netty `codec-http2/` 模块 203 文件/~59K 行，核心入口: `Http2FrameCodec:56` → `DefaultHttp2ConnectionHandler:29` → `DefaultHttp2Connection:60`。

### 1. 二进制帧格式 — 10 种帧类型 + 9 字节帧头

场景: HTTP/1.1 是文本格式（`GET / HTTP/1.1\r\n`），解析需要逐字符扫描。HTTP/2 是二进制格式 — RFC 7540 定义 10 种帧类型，每种帧有固定格式的 9 字节帧头。

源码路径:
- `Http2FrameTypes.java:22-31` — 10 种帧类型常量: `DATA(0x0)` `HEADERS(0x1)` `PRIORITY(0x2)` `RST_STREAM(0x3)` `SETTINGS(0x4)` `PUSH_PROMISE(0x5)` `PING(0x6)` `GO_AWAY(0x7)` `WINDOW_UPDATE(0x8)` `CONTINUATION(0x9)`
- `DefaultHttp2FrameReader.java:188-204` — `preProcessFrame(ByteBuf in)`: 读 3 字节 `readUnsignedMedium()` → `payloadLength`(L195)，读 1 字节 `readByte()` → `frameType`(L200)，读 1 字节 `readUnsignedByte()` → `Http2Flags`(L201)，读 4 字节 `readUnsignedInt(in)` → `streamId`(L202)

关键设计: 帧头的 streamId 字段决定了帧属于哪个 Stream — 同一个 TCP 连接上的 HEADERS 和 DATA 可以通过不同的 streamId 区分 — 多路复用的基础。网络字节序(Big Endian) — `WebSocketRsv` 也用 4 字节 `readInt()` 读帧头 — 这是网络协议的标准约定。

数据流: 发送端构建帧 — `writeMedium(payloadLength)` → `writeByte(frameType)` → `writeByte(flags)` → `writeInt(streamId)` → `writeBytes(payload)` → TCP → 接收端 `readFrame():148` → `preProcessFrame():188` → `readUnsignedMedium()` 读长度 → 长度 > `maxFrameSize` → `FRAME_SIZE_ERROR` (L196-198) → 合法 → `verifyFrameState():207` → `processPayloadState():246` 分发: HEADERS → `readHeadersFrame():427` / DATA → `readDataFrame():415` / SETTINGS → `readSettingsFrame():525` / PUSH_PROMISE → `readPushPromiseFrame():549` / GO_AWAY → `readGoAwayFrame():588` / ... → 回调 `Http2FrameListener`。

### 2. Stream 多路复用 — 一条 TCP 连接 N 个交错帧

场景: gRPC 客户端发送 10 个并发 RPC — 10 个 Stream（streamId=1/3/5/...，客户端用奇数）— 同一个 TCP 连接上 HEADERS 和 DATA 帧交错发送。服务端回复的 HEADERS 和 DATA 帧也按对应 streamId 分拣。

源码路径:
- `DefaultHttp2Connection.java:776` — `createStream(int streamId, boolean halfClosed)` → 创建 `DefaultStream` 存储到 `activeStreams` Map
- `DefaultHttp2Connection.java:381` — `DefaultStream` 内部类: 维护自身状态（OPEN/HALF_CLOSED_LOCAL/HALF_CLOSED_REMOTE/CLOSED）
- `Http2StreamVisitor` — 遍历所有活跃 Stream（如 GOAWAY 时通知每个 Stream）
- `RST_STREAM` 帧 — 取消一个 Stream 而不关闭整个连接

关键设计: 每个 Stream 有独立的流控窗口 — 一个慢消费的 Stream 不会阻塞其他 Stream（`WINDOW_UPDATE` 帧解决应用层队头阻塞）。TCP 层的队头阻塞仍存在 — 丢包重传影响同连接所有 Stream。QUIC(HTTP/3) 在 UDP 上实现才真正消除传输层队头阻塞 — 这是 HTTP/2 的核心设计边界。

数据流: 连接上的帧流 — 帧1 `streamId=1, type=HEADERS` → 帧2 `streamId=3, type=HEADERS` → 帧3 `streamId=1, type=DATA` → 帧4 `streamId=5, type=HEADERS` → `DefaultHttp2FrameReader` 解析每帧 → `readFrame():148` → `preProcessFrame():188` 提取 streamId → `verifyFrameState():207` → `listener.onHeadersRead(streamId, ...)` / `listener.onDataRead(streamId, ...)` → `DefaultStream(streamId=1)` 收到 HEADERS+DATA → 完成后 `RST_STREAM` 关闭 streamId=1 → streamId=3/5 完全不受影响。

### 3. HPACK 头压缩 — 静态表(61项) + 动态表 + Huffman

场景: HTTP/1.1 每次请求都重复发送 `User-Agent`、`Accept-Encoding` 等 header — 几百字节的冗余。HPACK 用 61 项的静态表（RFC 7541 预定义常用 header）+ 动态表（连接级别运行中学习）+ Huffman 编码 — 大幅减少 header 大小。

源码路径:
- `HpackStaticTable.java:52` — `STATIC_TABLE`: 61 项，index 1 `:authority`, index 2 `:method GET`, index 3 `:method POST`, index 4 `:path /`, index 5 `:path /index.html`, index 6-7 `:scheme http/https`, index 8-14 `:status 200/204/206/304/400/404/500`
- `HpackEncoder.java:171-174` — `encodeHeadersIgnoreMaxHeaderListSize()`: 先 `HpackStaticTable.getIndexInsensitive(name, value)` 查静态表 → 命中 `staticTableIndex` → 写简短索引字节 → 未命中 → `encodeLiteral(out, name, value, IndexType.NONE, nameIndex)` 文字编码
- `HpackDecoder.java:126-132` — `decode(int streamId, ByteBuf in, ...)`: `decodeDynamicTableSizeUpdates(in)` → `decode(in, sink)` → 读首字节: 0x8x=Indexed Header(static table) / 0x0x/0x4x=Literal Header

关键设计: 动态表是连接级别共享 — 当前 Stream 的 header 减少字节后，同一连接的新 Stream 可以引用动态表的新条目 — 连接的 header 越过越省。但坏处: 压缩上下文是连接的 — 解码错误会导致整条连接不可用 — `Http2Exception.ShutdownHint.HARD_SHUTDOWN` 直接 `ctx.close()` (L57-80 hard shutdown 常量)。静态表 index 1-14 覆盖 HTTP 伪头 — 大部分请求只需要一个字节就能表达 `:method GET`。

数据流: 发送端 — `Http2Headers {":method": "GET", ":path": "/api/users"}` → `HpackEncoder.encodeHeaders():119` → 查静态表: `":method GET"` → index 2 → 写 `0x82`(1字节) → `":path /api/users"` 不在静态表 → `encodeLiteral():174` → HuffmanEncode("/api/users") + nameIndex=4(引用 `:path`) → 接收端 — `HpackDecoder.decode():126` → 读 `0x82` → `decodeULE128` 出 index=2 → `HpackStaticTable.getEntry(2)` → `HpackHeaderField(":method", "GET")` → 动态表更新: 新增 `":path /api/users"` → index 62(动态表首条) → 下次请求同一个 path → 只需 `0xBE`(index 62)。

### 4. Flow Control — 连接级+流级双层 WINDOW_UPDATE

场景: 客户端处理慢（CPU 满载）— 服务端不断发送 DATA 帧 — 客户端内存溢出。WINDOW_UPDATE 让接收方通知发送方 "我可以再接收 N 字节" — 反压流控。

源码路径:
- `DefaultHttp2LocalFlowController.java:176-196` — `consumeBytes(Http2Stream stream, int numBytes)`: `connectionState().consumeBytes(numBytes)` + `state.consumeBytes(numBytes)` → 流窗口 + 连接窗口都减 → 窗口消耗过半(`windowUpdateRatio=0.5`) → `writeFrame(WINDOW_UPDATE)` 仅恢复流窗口(L217-220)
- `DefaultHttp2RemoteFlowController.java:170-194` — `isWritable(stream)`: 双条件 — `isWritableConnection()`(连接窗口>0) + `state.isWritable()`(流窗口>0) → 任一不满足 → 暂停发送(L606-607)
- `DefaultHttp2RemoteFlowController.java:175` — `channelWritabilityChanged()`: Netty Channel `isWritable()` 变化 → 重新分配流窗口 → 唤醒被流控阻塞的 Stream
- `Http2CodecUtil.java` — `DEFAULT_WINDOW_SIZE=65535`(初始)，可通过 `SETTINGS_INITIAL_WINDOW_SIZE` 提升

关键设计: 双层窗口 — 连接级窗口限制总发送速率，流级窗口限制每个 Stream 额度 — 一个 Stream 不能吃掉全部连接带宽。`windowUpdateRatio=0.5` — 窗口消耗过半才发 WINDOW_UPDATE — 减少 WINDOW_UPDATE 帧的开销。发送方检查双窗口 — 流窗口>0 但连接窗口=0 → 不发送 → 全局反压 — 避免"一个 Stream 狂发吃满连接"。

数据流: 接收端 `consumeBytes(stream, 4096):176` → `connectionWindow -= 4096` + `streamWindow -= 4096` → `streamWindow < initialWindow * 0.5` → `writeFrame(WINDOW_UPDATE(streamId, initialWindow-streamWindow))` → TCP → 发送端收到 WINDOW_UPDATE → `incrementWindowSize(stream, delta)` → 窗口恢复 → `isWritable(stream):170` 重新为 true → 帧发送循环 `writePendingBytes()` 恢复 → 发送下一个 DATA 帧 → 接收端再次 consume。

### 核心悬念 + 跨框架桥

**"HTTP/2 的二进制帧+多路复用+HPACK+流控是 gRPC 和 Dubbo Triple 的传输层基石。gRPC 的 `GrpcHttp2ConnectionHandler` 在 Netty HTTP/2 之上构建了 RPC 语义(请求→HEADERS 帧, body→DATA 帧, 完成→END_STREAM flag)。Dubbo 3.0 的 Triple 协议用 HTTP/2 替代了自定义的 Dubbo 协议 — header 走 HEADERS、payload 走 DATA — JSON/Protobuf 序列化全兼容。Stage 5 做 gRPC/Dubbo 源码分析时，不再需要从零解释 HTTP/2 — 本域是 Stage 1→Stage 5 的桥梁域。"**

→ 引出 Tomcat源码分析 — Netty 的 HTTP 编解码链(Ch10 Codec→Ch11 HTTP→Ch12 HTTP/2)是框架级协议的实现。Tomcat 作为 Servlet 容器，有完全不同的架构范式 — 容器层次(Engine→Host→Context→Wrapper)、Pipeline-Valve 责任链、CoyoteAdapter 协议适配 — 从"框架如何设计扩展点"切换到"规范如何驱动实现"。
