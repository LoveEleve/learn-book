# Ch12 Netty HTTP/2 — 知识规划

> 来源: codec-http2/ 203 文件/~59K 行 | 聚焦 4 核心机制
> 跨框架依赖: gRPC-Java (GrpcHttp2ConnectionHandler) + Dubbo Triple 协议
> 基线: Ch11 HTTP/1.1 编解码 — Ch12 回答 "二进制帧如何替代文本流"

---

## 01 提取 — 逐源映射

### Http2FrameTypes.java (31 行 — 帧类型常量定义)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Http2FrameTypes.java:22-31 | 10 种帧类型: DATA(0x0)/HEADERS(0x1)/PRIORITY(0x2)/RST_STREAM(0x3)/SETTINGS(0x4)/PUSH_PROMISE(0x5)/PING(0x6)/GO_AWAY(0x7)/WINDOW_UPDATE(0x8)/CONTINUATION(0x9) — 对应 RFC 7540 §6 | High |
| Http2FrameTypes.java:22-31 | 帧类型决定帧处理逻辑 — readFrame() 按 frameType switch 分发到不同 readXXXFrame() | High |

### DefaultHttp2FrameReader.java (629 行 — 二进制帧读取与分发)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| DefaultHttp2FrameReader.java:188-204 | **preProcessFrame()**: 读 9 字节帧头 — readUnsignedMedium()→payloadLength(3B)/readByte()→frameType(1B)/readUnsignedByte()→flags(1B)/readUnsignedInt(in)→streamId(4B) | High |
| DefaultHttp2FrameReader.java:148-150 | **readFrame()**: 入口 — 检查 readError → 调用 preProcessFrame → 等待 payload 可读 → verifyFrameState → switch(frameType) 分发 | High |
| DefaultHttp2FrameReader.java:254-278 | **帧分发**: DATA→readDataFrame / HEADERS→readHeadersFrame / PRIORITY→readPriorityFrame / RST_STREAM→readRstStreamFrame / SETTINGS→readSettingsFrame / GOAWAY→readGoAwayFrame / WINDOW_UPDATE→readWindowUpdateFrame — 每种帧有独立 read 方法 | High |
| DefaultHttp2FrameReader.java:196-198 | **maxFrameSize 校验**: payloadLength > maxFrameSize → FRAME_SIZE_ERROR → 连接关闭 | Medium |

### DefaultHttp2Connection.java (776 行 — Stream 管理与生命周期)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| DefaultHttp2Connection.java:81 | **activeStreams**: ActiveStreams 管理所有活跃 Stream — IntObjectMap 以 streamId 为 key | High |
| DefaultHttp2Connection.java:381 | **DefaultStream 内部类**: 维护 Stream 四态 — OPEN / HALF_CLOSED_LOCAL / HALF_CLOSED_REMOTE / CLOSED | High |
| DefaultHttp2Connection.java:776 | **createStream(streamId, halfClosed)**: 创建 DefaultStream → activeStreams.activate() → listener.onStreamAdded() | High |
| DefaultHttp2Connection.java:525-566 | **closeLocalSide() / closeRemoteSide()**: 半关闭语义 — 一方 close 后对方仍可发送 → 双方都 close → CLOSED | High |
| DefaultHttp2Connection.java:142-155 | **forEachActiveStream()**: 遍历所有活跃 Stream — 支持并发安全 (incrementPendingIterations → iterate → decrement) | Medium |

### HpackStaticTable.java (201 行 — RFC 7541 附录 A 静态表)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| HpackStaticTable.java:52-80 | **STATIC_TABLE**: 61 项 — index 1 `:authority` / 2 `:method GET` / 3 `:method POST` / 4 `:path /` / 5 `:path /index.html` / 6-7 `:scheme http/https` / 8-14 `:status 200-500` / 15-61 `accept-charset/encoding/language`... | High |
| HpackStaticTable.java:154-177 | **getIndexInsensitive(name, value)**: 大小写不敏感查找静态表 — 用于编码端快速匹配 | High |
| HpackStaticTable.java:200-201 | **getEntry(index)**: 1-indexed 查表 — `STATIC_TABLE.get(index - 1)` | High |

### HpackEncoder.java (文件级 — HPACK 编码器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| HpackEncoder.java:119-124 | **encodeHeaders()**: 入口 — 根据 ignoreMaxHeaderListSize 分两路 → encodeHeadersIgnoreMaxHeaderListSize / encodeHeadersEnforceMaxHeaderListSize | High |
| HpackEncoder.java:171-176 | **静态表查找**: getIndexInsensitive(name, value) → 命中 → encodeInteger(out, 0x80, 7, staticTableIndex) 写 indexed header 格式 | High |
| HpackEncoder.java:165-174 | **literal 编码**: 静态表未命中 → encodeLiteral(out, name, value, IndexType.NONE, nameIndex) — NONE=不索引动态表 / NEVER=永不索引(安全敏感) | High |
| HpackEncoder.java:111 | **huffCodeThreshold**: Huffman 编码阈值 — 字符串长度 ≥ threshold → Huffman 编码 | Medium |

### HpackDecoder.java (文件级 — HPACK 解码器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| HpackDecoder.java:126-132 | **decode()**: 入口 — decodeDynamicTableSizeUpdates() → decode(in, sink) — 两阶段: 处理动态表大小更新 + 解码 header | High |
| HpackDecoder.java:82-89 | **8 态解码状态机**: READ_INDEXED_HEADER / READ_INDEXED_HEADER_NAME / READ_LITERAL_HEADER_NAME_LENGTH_PREFIX / ... — 解析 HPACK 二进制流的状态 | High |
| HpackDecoder.java:57-80 | **HARD_SHUTDOWN 错误码**: 6 种致命解码错误 → HARD_SHUTDOWN → ctx.close() — HPACK 解码失败不可恢复 | High |

### DefaultHttp2LocalFlowController.java (接收端流控)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| DefaultHttp2LocalFlowController.java:54 | **initialWindowSize = DEFAULT_WINDOW_SIZE** — 初始窗口 65535 字节 | High |
| DefaultHttp2LocalFlowController.java:176-196 | **consumeBytes(stream, numBytes)**: 连接窗口 + 流窗口都减 → 返回是否消耗成功 → 窗口不足返回 false | High |
| DefaultHttp2LocalFlowController.java:108-110 | **unconsumedBytes()**: 应用层未消耗的字节 — 累计 → 可用于批量恢复窗口 | Medium |
| DefaultHttp2LocalFlowController.java:166 | **incrementWindowSize()**: 接收 WINDOW_UPDATE 帧 → 增加窗口 | High |

### DefaultHttp2RemoteFlowController.java (发送端流控)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| DefaultHttp2RemoteFlowController.java:170-171 | **isWritable(stream)**: 检查是否可发送 — monitor.isWritable(state) | High |
| DefaultHttp2RemoteFlowController.java:606-607 | **双条件 writable**: isWritableConnection()(连接窗口>0) + state.isWritable()(流窗口>0) — 任一不满足 → 暂停 | High |
| DefaultHttp2RemoteFlowController.java:264-265 | **writePendingBytes()**: 触发被流控阻塞的 Stream 恢复发送 — channel 可写 + 窗口足够 | High |
| DefaultHttp2RemoteFlowController.java:175-176 | **channelWritabilityChanged()**: Netty Channel writability 变化 → 重新分配流窗口 | Medium |

---

## 02 聚合 — P1/P2/P3 分级

### P1 — 全系统共识（≥5 处引用）
- **streamId 字段** (5 处引用): FrameReader:202 解析、Connection:776 createStream、LocalFlow:176 consumeBytes、RemoteFlow:170 isWritable、FrameTypes 常量表 — 贯穿帧解析→Stream管理→流控全链路
- **帧类型常量** (6 处引用): Http2FrameTypes 定义 → FrameReader dispatch → Connection handle 各帧 → FlowController WINDOW_UPDATE → Encoder/Decoder HEADERS

### P2 — 局部重要（2-4 处引用）
- **HPACK 静态表**: StaticTable:52 定义 → Encoder:171 查找 → Decoder:152 解码 — 编解码两端引用
- **WINDOW_UPDATE 窗口管理**: LocalFlow:176 consume → RemoteFlow:606 writable check → FrameReader:595 readWindowUpdate — 收发两端 + 帧解析
- **Stream 半关闭语义**: Connection:525 closeLocalSide / Connection:541 closeRemoteSide / FrameReader RST_STREAM

### P3 — 独立知识点（1 处独有）
- **HARD_SHUTDOWN**: Decoder:57-80 — HPACK 解码致命错误的关闭语义
- **huffCodeThreshold**: Encoder:111 — Huffman 编码性能调优
- **maxFrameSize**: Reader:196-198 — 帧大小上限校验

---

## 03 深度分类 — 🔴🟡🟢

| KP | 级别 | 判定理由 |
|------|:--:|------|
| 二进制帧格式 (10 种帧 + 9 字节帧头) | 🔴 Deep | 不含帧结构则无法理解多路复用——streamId 嵌入帧头是 HTTP/2 的设计基础 |
| Stream 多路复用 (streamId 分配 + 交错 + 四态) | 🔴 Deep | HTTP/2 的核心创新——理解为"连接上 N 个虚拟通道"才理解为什么要帧级和流级流控 |
| HPACK 头压缩 (静态表 + 动态表 + Huffman) | 🟡 Working | 性能优化——协议即使不带压缩也能工作(RFC 7541 §2.2 允许不压缩), 但对 gRPC 高频 RPC 场景至关重要 |
| Flow Control (双层 WINDOW_UPDATE) | 🟡 Working | 反压机制——协议定义但不强制 precision; 对生产环境多 Stream 竞争场景关键 |

---

## 04 聚类 — 教学顺序

**顺序**: 帧格式(基础) → Stream(多路) → HPACK(压缩) → Flow Control(反压)

**每个决定的原因**:
1. **帧格式第一**: 不先理解 9 字节帧头 + 10 种帧类型 → Stream 多路复用的 "streamId 在帧头里" 无法解释
2. **Stream 第二**: 理解了 "一个连接 N 个虚拟流" → HPACK 的 "动态表是连接级共享" 和 Flow Control 的 "流级+连接级双层窗口" 才有上下文
3. **HPACK 第三**: 不先知道 Stream 是独立的 → 动态表的 "当前 Stream 写入→新 Stream 读取" 无法理解
4. **Flow Control 第四**: 综合——不先知道 Stream 和 WINDOW_UPDATE 帧 → "每个 Stream 独立窗口" 和 "连接级窗口 = 总量限制" 的双层设计无法理解

> → 跨框架桥: "gRPC 的 GrpcHttp2ConnectionHandler 在 Netty HTTP/2 上构建 RPC 语义(请求→HEADERS 帧/body→DATA 帧/完成→END_STREAM flag); Dubbo Triple 用 HTTP/2 替代 Dubbo 私有协议(header→HEADERS/payload→DATA/JSON+Protobuf 序列化)"
