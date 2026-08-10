# HTTP协议演进: HTTP/1.1 → HTTP/2 → HTTP/3 QUIC

> Cluster E: 6 KPs | 依赖: 08-epoll, 10-netfilter, 11-ssl-tls | 读者基线: TCP + TLS + 应用协议

---

### 1. HTTP/1.1 — keep-alive + 队头阻塞 + pipeline
  - 持久连接(keep-alive): 一个TCP连接发送多个请求, 避免每次请求都三次握手 — 但请求/响应严格顺序(队头阻塞) (RFC 7230 §6.3)
  - 队头阻塞(HOL): 请求A响应慢→后续B/C/D全被阻塞等 — 浏览器打开6个并行连接缓解(但浪费资源) (RFC 7230 §6.3)
  - Pipeline: 连续发多个请求不等响应→服务端按序返回 — 但FIFO语义+实现复杂, 几乎被废弃 (RFC 7230 §6.3.2)
  - 文本协议: 请求行(method URI version)+Headers(每行key:value)+空行+Body — 人类可读但解析低效
  - 分块传输(Chunked): Transfer-Encoding: chunked 动态内容发送(先发头, body分块发送) — 不需要Content-Length (RFC 7230 §4.1)

### 2. HTTP/2 — 多路复用的革命
  - 二进制分帧: 将所有通信转为二进制帧(frame header 9字节+payload), 替代HTTP/1.1文本解析 (RFC 7540 §4)
  - Stream多路复用: 一个TCP连接含多个Stream(逻辑流), 每Stream有ID, 帧交错发送不互相阻塞 — 解决HTTP/1.1队头阻塞 (RFC 7540 §5.1)
  - 帧类型: HEADERS(头), DATA(体), PRIORITY(优先级), RST_STREAM(取消流), SETTINGS(连接配置), PUSH_PROMISE(服务端推送) (RFC 7540 §6)
  - 流量控制: 每Stream独立窗口 — 防止一个Stream占用所有带宽 (RFC 7540 §5.2)
  - 限制: 仍基于TCP → TCP队头阻塞未解决(单个TCP包的丢失导致所有Stream阻塞)

### 3. HPACK — HTTP/2头部压缩
  - 核心: 静态表(61项常用头)+动态表(每连接维护的请求/响应头), 后续请求只发索引 (RFC 7541 §2.3)
  - Huffman编码: 头部值用Huffman编码进一步压缩 (RFC 7541 §5.2)
  - CRIME攻击防护: HPACK引入HPACK Dynamic Table Size限制+编码隔离 (RFC 7541 §4)
  - Server Push: 服务端主动PUSH_PROMISE推送资源(如HTML需要的CSS) — 1个请求触发多个响应 (RFC 7540 §8.2)

### 4. HTTP/3 QUIC — 基于UDP的下一代
  - 为什么用UDP: 绕过TCP队头阻塞 — QUIC在UDP上实现可靠传输+拥塞控制+多路复用 (RFC 9000 §3)
  - 连接ID: 用64位Connection ID标识连接(非五元组) — 网络切换IP/端口变化不影响连接 (RFC 9000 §5.1)
  - 多路复用: Stream复用类似HTTP/2, 但基于UDP — 丢包仅影响该Stream, 不会阻塞其他Stream (RFC 9000 §2.2)
  - 0-RTT建连: 首次1-RTT握手, 再次连接0-RTT发送数据 — 比HTTP/2+TLS 1.3的1-RTT更快 (RFC 9001 §4.6)
  - 内置TLS 1.3: QUIC的加密层是TLS 1.3(非叠加) — 建连=加密+传输统一完成 (RFC 9001)

### 5. QUIC连接迁移 — 改变网络不重连
  - 场景: 手机从Wi-Fi切到4G → IP+PORT全变 → TCP连接全断需重连 → QUIC用Connection ID保持连接
  - 路径验证: 新路径发PATH_CHALLENGE帧→收到PATH_RESPONSE→确认新路径可达 (RFC 9000 §8.2)
  - 对比HTTP/2: HTTP/2断网=所有Stream全灭+重连(新TCP+新TLS)=等待2-3秒, QUIC=切换路径=几乎无感
  - HTTP语义不变: HTTP/3只是传输层从TCP换为QUIC(基于UDP), HTTP方法/Header/StatusCode语义完全不变

### 6. 收束
  - HTTP三条演进线: 持久化(keep-alive)→多路复用(stream)→无连接迁移(Connection ID)
  - HTTP/1.1→HTTP/2解决应用层队头阻塞(Stream), HTTP/2→HTTP/3解决TCP层队头阻塞(UDP)
  - HTTP/3 QUIC是互联网的未来: Google/YouTube/CDN已大规模部署, 内核也加入QUIC支持

---

### 核心悬念
**"HTTP/3解决了传输层协议问题, 但 你输入URL到看到页面, 第一步的DNS解析是怎么工作的?"**

→ 引出 DNS + CDN + WebSocket + Wireshark排查(13-dns-cdn-websocket)
