# T-9 §1 帧协议解析 — WsFrameBase 的字节 → 帧状态机

> 依赖 T-8 + T-4 | 🟡B | 3 KP | 09 审计新增域

**读者处境**: T-8 讲完 HTTP 报文解析 — WebSocket 握手请求 (Upgrade: websocket) 就是 HTTP 报文, 由 MimeHeaders 解析 — 但握手成功后的字节流完全换了一套协议: 不再是文本行, 而是二进制帧。一条 4KB 的消息可能拆成 3 个帧、分 5 次 TCP 到达 — 解析器怎么处理"只来了 2 个字节"?

### 1. 帧头位运算 — 一字节三字段

场景: WebSocket 帧的头 2 字节: 第 1 字节 = fin(1 位) + rsv(3 位) + opCode(4 位)。C 语言风格的位运算在 Java 里直接用 — 因为帧格式就是位级的, 解析必须位级。

源码路径: `WsFrameBase.java:141-160`。`processInitialHeader()`: 先检查 `inputBuffer.remaining() < 2` — 不足 2 字节直接 return false (半包); 然后 `int b = inputBuffer.get()` — `fin = (b & 0x80) != 0` (最高位) / `rsv = (b & 0x70) >>> 4` (中 3 位) / `opCode = (byte)(b & 0x0F)` (低 4 位)。随后 `transformation.validateRsv(rsv, opCode)` 校验 rsv 位 — 未协商扩展时 rsv 必须为 0。

关键设计: **位运算解析 — 为什么不用 shift 方法封装?** 帧格式是协议的物理事实 — 单字节承载 3 个语义字段 — 位运算最直接、最接近 RFC 6455 的位图定义, 且热路径无方法调用开销。validateRsv 的"未扩展必须为 0"是协议安全面: 客户端置位 rsv 但服务器不支持扩展 — 必须拒绝, 否则扩展协商状态机错乱。 [模式: Bitmask Protocol Parsing]

数据流: `WsFrameBase.processInputBuffer()` (L111) 主循环 → `processInitialHeader()` → 2 字节 → fin/rsv/opCode 三字段 → validateRsv → 控制帧走 §2 校验 → 数据帧继续读载荷长度。

### 2. 控制帧严格校验 — fin=1 + 白名单 + 125 上限

场景: `PING` 帧混在数据流中间到达 — 如果 PING 帧分片了 (fin=0), 接收方无法判断它是"控制帧的分片"还是"数据帧的开始" — 协议状态机就乱了。

源码路径: `WsFrameBase.java:155-165` (控制帧校验) + L55-56 (缓冲预分配)。控制帧三连校验: ① `!fin → controlFragmented` (控制帧禁止分片); ② `opCode ∉ {PING, PONG, CLOSE} → invalidOpCode` (白名单); ③ `controlBufferBinary/Text = ByteBuffer.allocate(125)` — **125 是控制帧载荷上限** (RFC 6455 §5.5: 控制帧载荷必须 ≤125)。

关键设计: **为什么控制帧禁分片 + 限 125?** 控制帧 (PING/PONG/CLOSE) 必须在消息间隙原子地插入 — 分片会导致接收端把控制帧误当数据帧 continuation; 125 上限保证控制帧永远在单次缓冲内完整处理 — 不需要跨帧状态。白名单拒绝未定义 opCode (0x3-0x7, 0xB-0xF) — 协议演进预留位, 未知即错。 [模式: Fail-Fast Protocol Validation]

数据流: `processInitialHeader()` 检出 opCode → `Util.isControl(opCode)` → 三连校验 → 违规抛 `WsIOException(new CloseReason(CloseCodes.PROTOCOL_ERROR, ...))` → 触发关闭握手 → `processDataControl()` (L311) 分派 PING/PONG/CLOSE 处理。

### 3. 半包/粘包状态机 — return false 与缓冲等待

场景: 客户端发来 100 字节的帧, TCP 分 3 次到达: 先到 2 字节 (头), 再到 50 字节 (部分载荷), 最后 48 字节。解析器每次 read 都返回部分数据 — 状态机怎么记住"已经读了头, 正在等载荷"?

源码路径: `WsFrameBase.java:111-130` (主循环) + L141 (头部不足 return false) + L226 (processRemainingHeader 长度扩展)。核心模式: **每个解析阶段检查剩余字节, 不足就 return false** — `processInputBuffer()` 循环调用: `processInitialHeader()` → `processRemainingHeader()` (126→2 字节扩展长度, 127→8 字节) → `processData()` — 任何一步不足都 return false, 主循环 break, **等下次 read 再来**。状态由**实例字段**记忆 (fin/opCode/payloadLength 已解析部分) — 下次从断点继续。

关键设计: **状态机字段记忆 vs 阻塞等待 — 为什么 return false?** NIO 非阻塞模型下 read 返回 0 是常态 — 阻塞等待会占住 Poller 线程; 字段记忆让解析状态跨 read 调用存续 — 这就是"非阻塞协议解析"的标准形态。注意与 T-8 HTTP 解析的对照: HttpParser 也是字节级, 但 WsFrameBase 的状态更多 (fin/opCode/长度状态机), 因为帧是**分片的二进制结构**而 HTTP 报文是文本行。 [模式: Stateful Incremental Parser]

数据流: `Poller` (T-4) 检测可读 → `WsFrameBase.processInputBuffer()` → 循环: 头 2 字节 → 长度扩展 → 载荷 → 不足 return false break → 下次 OPEN_READ 事件再进 → 完整帧 → processData 分派文本/二进制/控制。

→ 引出 §2 会话管理 — 帧解析出来了, 但连接的状态 (打开/关闭中/关闭) 谁管? 10 万个连接谁扫超时? 谁发心跳? WsSession 的生命周期和 WsWebSocketContainer 的后台任务。