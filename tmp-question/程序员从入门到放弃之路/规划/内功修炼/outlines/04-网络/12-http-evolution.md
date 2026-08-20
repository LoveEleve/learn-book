# HTTP 演进 — 从 keep-alive 到多路复用，再到 QUIC 连接迁移

> Cluster E: 6 KPs | 依赖: 08-epoll-reactor、10-netfilter-nat、11-ssl-tls | 读者基线: TCP、TLS、应用协议
> 读者处境: 11 篇讲完 TLS 握手与安全边界；本篇回答 HTTP 为什么从文本长连接演进到 HTTP/2，再把传输层交给 QUIC
> 打开新视角: HTTP 演进不是“版本越高越快”，而是逐层拆除瓶颈——**连接复用、应用帧复用、头部压缩、TCP 队头阻塞、网络切换重连**

---

### 概念依赖链

```
08 epoll + 10 NAT + 11 TLS → 本篇: HTTP/1.1 → HTTP/2 → HTTP/3/QUIC
  ├─ §1 HTTP/1.1(持久连接/文本/应用层 HOL)
  ├─ §2 HTTP/2(二进制帧/Stream 多路复用)
  ├─ §3 HPACK(头部压缩与动态状态)
  ├─ §4 HTTP/3/QUIC(UDP之上的可靠多路复用 + TLS 1.3)
  └─ §5 Connection ID/迁移(网络变化不断连接)
先讲: 少建连接 → 一连接多流 → 少传头 → 绕开 TCP HOL → 路径迁移
后续依赖: 13-dns-cdn-websocket(DNS/CDN/WebSocket 与排障)
```

### 叙事顺序

1. 问题引入——一个网页包含几十个资源，为什么 HTTP/1.1 会开多个连接，HTTP/2 又想把它们合成一条？（**Aha: 每一代 HTTP 都是在消除上一代某一层的等待与重复**）
2. HTTP/1.1——keep-alive、文本、pipeline 与应用层 HOL
3. HTTP/2——二进制帧、Stream、多路复用与 TCP HOL
4. HPACK——为什么请求头也值得压缩
5. HTTP/3/QUIC——UDP 上重新实现可靠传输与多路复用
6. Connection ID/迁移——Wi-Fi/4G 切换为何不必重连
7. 收束——三代协议解决的瓶颈对应关系

### 1. HTTP/1.1 — 少建连接，却仍被顺序和文本拖住

场景提示: 一个页面要加载 HTML、CSS、JS、图片，HTTP/1.1 为什么既想复用连接，又常常需要并行连接？ [写作时展开]

关键设计: HTTP/1.1 用持久连接减少 TCP/TLS 建连，但单条连接上的请求/响应顺序和应用解析仍有限制：

```[pseudocode]
HTTP/1.1 keep-alive
  一个 TCP 连接承载多个请求/响应
  → 少一次 TCP/TLS 握手

文本消息:
  request-line + headers + blank line + optional body

pipeline(规范支持, 实际部署很少用)
  客户端连续发多个请求
  → 服务端仍需按序返回响应
  → 前面的慢响应阻塞后面的响应

chunked transfer
  不预先知道 body 总长度时
  → headers + chunk size/data + ... + terminating chunk
```

Why: 为什么 HTTP/1.1 会开多个并行连接？——**因为 keep-alive 解决的是建连成本，不解决单连接内的队头阻塞**：多个连接能让资源并行传输，但会增加连接、TLS、内核 socket 和拥塞控制开销。HTTP/1.1 的文本格式便于调试，但解析、重复头部和严格消息边界也带来成本。 [man 7 http: HTTP 请求/响应语义；[内核: 连接复用最终仍落到 TCP socket 与 epoll 事件循环]

比喻锚点: keep-alive 像同一辆货车多趟送货，省掉反复叫车；但如果一趟货卡在车门口，后面的货仍要等。 [写作时展开]

### 2. HTTP/2 — 用帧和 Stream 拆开应用层等待

场景提示: 如果一条连接上有 HTML、CSS、JS 多个请求，能不能让它们的字节交错发送，而不必等前一个响应完整结束？ [写作时展开]

关键设计: HTTP/2 把消息拆成二进制帧，并用 Stream ID 把不同请求/响应复用到一条 TCP 连接：

```[pseudocode]
HTTP/2 connection
  → 多个独立 Stream

每个 frame:
  length + type + flags + stream_id + payload

典型帧:
  HEADERS / DATA / SETTINGS / RST_STREAM
  WINDOW_UPDATE / PING / GOAWAY

发送:
  Stream 1 HEADERS
  Stream 3 HEADERS
  Stream 1 DATA
  Stream 3 DATA
  → 帧在一个 TCP 连接上交错
```

Why: 为什么 HTTP/2 能解决 HTTP/1.1 的应用层 HOL，却仍可能整体卡住？——**Stream 在 HTTP 层独立，但它们共享一个 TCP 字节流**：某个 TCP 段丢失时，TCP 必须按序交付，所有 Stream 的新数据都可能等待重传。HTTP/2 的 per-stream/connection flow control 解决的是应用发送公平性，不是 TCP 丢包队头阻塞。 [内核: HTTP/2 多路复用最终仍由 TCP socket 的顺序字节流承载]

比喻锚点: HTTP/2 像把多个货物订单装进同一辆车的不同编号箱子，装卸可以交错；但车辆在路上爆胎，所有箱子仍一起等。 [写作时展开]

### 3. HPACK — 头部压缩也需要状态同步

场景提示: 浏览器每个请求都重复发送 Cookie、Host、User-Agent，为什么 HTTP/2 不只压缩 body？ [写作时展开]

关键设计: HPACK 使用静态表、动态表和 Huffman 编码减少重复头部：

```[pseudocode]
静态表:
  预定义常见 header name/value

动态表:
  连接两端维护近期出现的 header
  首次发送完整/部分字段
  后续可发送表索引

Huffman:
  对字符串表示进一步编码

约束:
  动态表大小由 SETTINGS 控制
  两端必须保持解码状态同步
```

Why: 为什么头部压缩不能简单使用普通 gzip？——**请求头是交互式、有状态、包含高敏感 Cookie 的结构**：HPACK 用索引和受控动态表减少重复，同时需要防止压缩上下文被错误利用；压缩上下文和安全策略是一个整体，不能把“压缩率”单独最大化。HTTP/2 的 server push 另有生命周期和缓存一致性问题，不能把它视作 HPACK 的组成部分。 [内核: HPACK 通常在用户态 HTTP/2 实现中完成，不属于 TCP/IP 内核路径]

比喻锚点: HPACK 像双方共用一本近期词典，第二次说“第 17 个词”就够了；但两边词典必须同步，不能各自随意改页码。 [写作时展开]

### 4. HTTP/3 / QUIC — 把多路复用从 TCP 字节流中解放出来

场景提示: HTTP/2 已经多路复用，为什么还要在 UDP 上重新实现可靠传输？ [写作时展开]

关键设计: QUIC 在 UDP 数据报之上提供连接、可靠交付、拥塞控制、Stream 和 TLS 1.3 集成：

```[pseudocode]
HTTP/3
  → HTTP/3 frames
  → QUIC streams
  → QUIC packets
  → UDP datagrams
  → IP

QUIC 丢包:
  某个 stream 的数据等待重传
  → 其他 stream 不必等待同一个 TCP 有序字节流

QUIC 安全:
  TLS 1.3 负责握手/密钥
  QUIC 负责传输状态、ACK、重传、拥塞和 stream
```

Why: 为什么 QUIC 选择 UDP，而不是继续修改 TCP？——**用户态协议可以更快演进，并能把可靠多路复用设计成按 Stream 隔离的丢包影响**；UDP 只提供数据报承载，QUIC 必须自己承担拥塞控制、可靠性、连接状态和安全集成。它解决的是 TCP 层 HOL，不是让网络从此没有丢包。 [内核: UDP 只提供报文承载；QUIC 的大部分传输状态由用户态实现维护]

比喻锚点: HTTP/2 是一辆共享货车上的多个箱子，HTTP/3/QUIC 像多条可独立重发的传送带；一条带子卡住，不必把其他带子全部停掉。 [写作时展开]

### 5. Connection ID 与连接迁移 — 网络换了，逻辑连接不必换

场景提示: 手机从 Wi-Fi 切到 4G，IP 和端口变了，为什么 QUIC 可以尝试保持同一个逻辑连接？ [写作时展开]

关键设计: QUIC 用 Connection ID 标识逻辑连接，并通过路径验证确认新网络路径可用：

```[pseudocode]
旧路径:
  Connection ID = C
  client IP:port = A

网络切换:
  client IP:port = B
  仍携带/协商 Connection ID = C

服务端:
  → 检查新路径
  → PATH_CHALLENGE
  → PATH_RESPONSE
  → 验证通过后迁移发送路径
```

Why: 为什么 Connection ID 能帮助迁移，却不能保证“切网永不掉线”？——**新路径仍要通过 NAT、防火墙、路由和路径验证，连接状态也可能因超时或网络不可达而丢失**；Connection ID 只是把连接身份从五元组中分离出来，不能消除网络故障。 HTTP 语义仍保持方法、状态码和 header 的应用层语义，但传输行为不同。 [内核: NAT 可能改变 UDP 五元组；QUIC 用 Connection ID 在协议层维持逻辑身份]

比喻锚点: Connection ID 像订单号，配送车辆和道路可以更换，但仓库用订单号知道还是同一件货；新道路仍要检查能否通行。 [写作时展开]

### 6. 收束

三代 HTTP 的瓶颈对应关系：

```[pseudocode]
HTTP/1.1:
  keep-alive 减少建连
  但单连接应用层顺序与文本重复明显

HTTP/2:
  二进制帧 + Stream 多路复用 + HPACK
  解决应用层 HOL
  仍受 TCP 丢包 HOL 影响

HTTP/3:
  HTTP/3 frames + QUIC streams + UDP
  按 stream 隔离丢包影响
  Connection ID 支持路径迁移
```

**Aha Moment**: "HTTP 的演进不是简单从‘文本’升级到‘二进制’，而是逐层拆瓶颈：**HTTP/1.1 减少建连，HTTP/2 减少应用层串行等待，HTTP/3/QUIC 再绕开 TCP 字节流层面的队头阻塞，并把连接身份从五元组中分离出来**。"
**回答读者三问**: ①HTTP/2 为什么仍会被丢包拖住=共享 TCP 字节流；②HTTP/3 为什么基于 UDP=在用户态重建按 Stream 隔离的可靠传输；③网络切换为什么可能不断连接=Connection ID 与路径验证分离逻辑连接和路径。

---

### 核心悬念

**"输入 URL 后，DNS 如何找到目标地址，CDN 如何把请求引到最近节点，WebSocket 又如何把 HTTP 升级成长连接？如何用 Wireshark 把这一整条链路抓出来？"**

→ 引出 13-dns-cdn-websocket — DNS/CDN/WebSocket 与 Wireshark 排障——从 HTTP 传输演进进入真实 Web 请求全链路。