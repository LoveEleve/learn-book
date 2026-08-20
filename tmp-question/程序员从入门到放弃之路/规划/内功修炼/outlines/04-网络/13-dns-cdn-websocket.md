# DNS、CDN、WebSocket 与 502 排障 — 从输入 URL 到长连接和故障定责

> Cluster E: 6 KPs | 依赖: 11-ssl-tls、12-http-evolution | 读者基线: HTTP/TLS/TCP、代理与缓存
> 读者处境: 11-12 篇讲完 TLS 与 HTTP/QUIC；本篇把它们串成真实 Web 请求：先解析域名，再命中 CDN/代理，最后可能升级成长连接，出错时如何判断 502 到底坏在哪一段
> 打开新视角: Web 请求不是“DNS 查一次就结束”，而是**解析缓存 → 边缘路由 → HTTP/TLS → 上游回源 → WebSocket/代理状态 → 抓包证据链**

---

### 概念依赖链

```
11 TLS + 12 HTTP → 本篇: DNS/CDN/WebSocket/排障
  ├─ §1 DNS 递归/迭代(名字变地址)
  ├─ §2 DNS 分级缓存(TTL/超时/失败)
  ├─ §3 CDN(GeoDNS/边缘缓存/回源)
  ├─ §4 WebSocket(HTTP Upgrade/帧/心跳)
  ├─ §5 502(Wireshark 时间线定责)
  └─ §6 KCP(UDP可靠传输的专用取舍)
先讲: DNS → CDN → HTTP长连接 → 故障排查 → 专用 UDP 方案
后续依赖: 14-container-network(容器 namespace/veth/bridge/kube-proxy)
```

### 叙事顺序

1. 问题引入——输入 URL 后，域名如何变成地址，地址如何落到 CDN/源站，代理返回 502 时又该相信谁？（**Aha: 生产排障要沿时间线拆分 DNS、连接、TLS、代理和上游五段责任**）
2. DNS 递归/迭代与缓存
3. CDN 智能调度、缓存和回源
4. WebSocket 从 HTTP 升级到全双工
5. 502/Wireshark——抓包时间线定位责任
6. KCP——游戏/实时场景的 UDP 专用取舍
7. 收束——一次 Web 请求的全链路

### 1. DNS 递归与迭代 — 域名怎样变成地址

场景提示: 浏览器访问 `https://example.com` 时，第一次连接之前 DNS 到底问了哪些服务器？ [写作时展开]

关键设计: 客户端通常向配置的递归解析器发起递归请求；递归解析器再代表客户端向根、TLD 和权威 DNS 逐级迭代查询：

```[pseudocode]
客户端 → 本地/运营商递归 DNS
  命中缓存? 直接返回
  未命中:
    递归 DNS → 根服务器: 去问 .com
    递归 DNS → .com TLD: 去问 example.com 权威服务器
    递归 DNS → 权威 DNS: 取得 A/AAAA/CNAME 等结果
  → 返回客户端

DNS message:
  Header(固定部分) + Question + Answer + Authority + Additional
```

Why: 为什么要有递归 DNS，而不是每个客户端自己访问根/TLD/权威服务器？——**递归解析器集中缓存、重试、超时和 DNSSEC/策略处理，避免全球权威基础设施被每个终端直接打爆**；迭代是服务器返回“下一站去哪”，递归是服务器替请求方把链路跑完。实践中客户端→递归 DNS 常是递归，递归 DNS→权威通常是迭代。 [man 5 resolv.conf: `nameserver`、timeout、attempts 等客户端解析配置]

比喻锚点: 递归 DNS 像前台接到“帮我找这家店”的请求，前台替你问总服务台、区域服务台和店铺；迭代查询则是每一站只告诉你下一站地址。 [写作时展开]

### 2. DNS 分级缓存 — TTL、负缓存与失败重试

场景提示: 同一域名第一次打开慢，第二次明显快；DNS 记录明明改了，为什么客户端还拿到旧地址？ [写作时展开]

关键设计: DNS 结果可能被浏览器、操作系统/解析库、企业或运营商递归 DNS 多级缓存；权威响应中的 TTL 指导缓存有效期：

```[pseudocode]
应用/浏览器缓存
  → OS/NSS/本地 stub/cache
  → 递归 DNS cache
  → 权威 DNS

缓存命中:
  直接返回未过期记录
缓存过期:
  重新递归查询
查询失败:
  按 resolv.conf/解析器策略重试、切换服务器或返回错误
```

Why: 为什么把 TTL 设成 60 秒就不能保证 60 秒内全球完全切换？——**TTL 是缓存遵守的建议/约束边界，但还有负缓存、解析器策略、预取、客户端缓存和旧连接复用等因素**；短 TTL 提高切换速度，也增加查询负载。DNS 延迟不能固定写成 50-200ms，网络、缓存和协议选择差异很大。 [内核: DNS 通常由用户态 resolver/stub 完成，不是 TCP/IP 内核路径的一部分]

比喻锚点: TTL 像地图版本的有效期——过期前大家继续用手里的副本，过期后才回服务台拿新地图。 [写作时展开]

### 3. CDN — 智能调度、边缘缓存与回源

场景提示: 北京用户访问同一个静态资源，为什么 CDN 可能让他命中北京边缘，而不是每次跨地域访问源站？ [写作时展开]

关键设计: CDN 通常通过 DNS/Anycast/负载均衡把请求引向边缘节点；边缘缓存未命中时再回源：

```[pseudocode]
用户解析 cdn.example.com
  → GeoDNS/Anycast/调度系统选择边缘入口
  → L4/L7 edge
  → cache hit?
      返回边缘对象
      cache miss → 向源站请求
                  → 返回用户
                  → 按 Cache-Control 等策略缓存
```

Why: 为什么 CDN 不是简单“离用户最近的服务器”？——**调度还要考虑网络质量、节点负载、运营商、健康状态、缓存命中率和回源成本**；“地理最近”不一定网络最近。`Cache-Control: max-age`、`s-maxage`、ETag、Purge 等共同决定缓存生命周期；强一致、个性化和高写入场景需要谨慎使用 CDN。 [man 7 http: Cache-Control/ETag 是 HTTP 缓存语义，CDN 还会叠加供应商策略]

比喻锚点: CDN 像把畅销书提前放进各城市书店；读者就近取书，书店没有存货时才向中央仓库回源。 [写作时展开]

### 4. WebSocket — HTTP Upgrade 后的双向帧流

场景提示: 聊天室为什么不必每秒轮询一次 HTTP，而能让服务端主动推送消息？ [写作时展开]

关键设计: WebSocket 先用 HTTP/1.1 Upgrade 建立握手，成功后同一连接改用 WebSocket 帧双向通信：

```[pseudocode]
客户端:
  GET /chat HTTP/1.1
  Upgrade: websocket
  Connection: Upgrade
  Sec-WebSocket-Key: random nonce

服务端:
  101 Switching Protocols
  Sec-WebSocket-Accept = 根据 key 计算的响应值

升级后:
  Text/Binary data frames
  Ping/Pong heartbeat
  Close frame
  客户端发送的帧必须 mask, 服务端发送通常不 mask
```

Why: 为什么 WebSocket 需要心跳和 close frame？——**长连接会经过 NAT、负载均衡和代理，这些中间设备可能因空闲超时而清理状态**；Ping/Pong 用于保活和探活，Close 帧用于协商结束。WebSocket 解决的是双向应用消息通道，不自动解决业务幂等、重连、顺序恢复和背压。 [man 7 websocket: 实际库行为还包括代理、TLS、心跳和应用重连策略]

比喻锚点: HTTP Upgrade 像先到普通柜台办理入场，再把同一条通道变成双向对讲机；之后双方都能主动说话。 [写作时展开]

### 5. 502 + Wireshark — 用时间线而不是猜测定责

场景提示: 用户看到 `502 Bad Gateway`，客户端、反向代理、上游服务三方到底谁坏了？ [写作时展开]

关键设计: 502 说明网关/代理从上游收到无效响应或无法获得有效上游响应，不能仅凭状态码断定根因；排障要对齐各段时间线：

```[pseudocode]
客户端 ↔ 代理 ↔ 上游

检查 1: DNS
  解析到了哪个边缘/代理地址?
检查 2: 客户端→代理
  TCP/TLS/HTTP 是否成功?
检查 3: 代理→上游
  SYN/SYN-ACK、TLS、请求发送、响应等待是否正常?
检查 4: 上游响应
  无响应/超时、连接复位、无效 HTTP、真实 4xx/5xx 分别处理

工具:
  tcpdump/pcap: 采集时间线
  Wireshark: Follow Stream、TCP 重传/RTT、HTTP 状态
  ss: socket 状态、队列、连接数
  代理 access/error log: request_id 与 upstream timing
```

Why: 为什么“上游返回 500”和“代理返回 502”不能混为一谈？——**500 是上游应用明确返回的服务端错误，502 是网关认为上游响应无效/不可用**；代理可能把连接重置、超时、协议解析失败映射成 502 或其他 5xx，具体要看代理实现。抓包应同时看客户端/代理/上游可见的边界，单点抓包无法证明另一段发生了什么。 [man 7 tcpdump: 抓包只证明所在观测点看到的流量]

比喻锚点: 502 排障像查跨站列车：乘客说没到站，要分别看始发站发车、换乘站接车、终点站是否开门，不能只看乘客手里的延误通知。 [写作时展开]

### 6. KCP — UDP 之上的专用可靠传输取舍

场景提示: 游戏或实时互动场景为什么有人不直接使用 TCP，而在 UDP 上实现自己的可靠重传？ [写作时展开]

关键设计: KCP 类协议在 UDP 上实现序号、ACK、重传和窗口等可靠传输机制，具体实现可叠加 FEC/拥塞控制，但这些不是所有 KCP 实现的统一内置能力：

```[pseudocode]
应用数据
  → KCP segment(seq/ack/window)
  → UDP datagram
  → 网络

丢包:
  ACK/SACK 或超时发现
  → 重传缺失 segment

可选增强:
  FEC: 用冗余包换带宽, 减少部分等待
  拥塞/带宽策略: 由具体实现和应用场景配置
```

Why: 为什么 UDP 可靠协议不一定比 TCP“更快”？——**它可以针对实时场景调低等待和重传策略，但必须自己承担拥塞控制、公平性、NAT、MTU、乱序、重传和安全问题**；KCP 也不是标准化的多路径协议，不能把某个上层 multipath/smux 实现当作 KCP 核心。所谓“TCP 等 200ms”也不是固定常量，延迟取决于 RTO、网络和实现。 [内核: KCP 通常运行在用户态 UDP 之上，与 QUIC 的协议目标和安全集成不同]

比喻锚点: KCP 像为赛车队定制的运输规则——可以减少等待、增加备用零件，但车队自己要负责交通规则、避让和安全；通用公路运输则由 TCP 提供更多默认保障。 [写作时展开]

### 7. 收束

一次 Web 请求的关键链路：

```[pseudocode]
URL
  → DNS cache/recursive resolver
  → CDN/代理选择边缘
  → TCP/QUIC + TLS
  → HTTP request/response 或 WebSocket frames
  → miss 时回源
  → 出错时用日志/ss/pcap 对齐时间线
```

**Aha Moment**: "从 DNS 到 CDN、从 WebSocket 到 502，线上 Web 问题不能只看某个协议名，而要沿着**解析、连接、加密、代理、上游和缓存**逐段划分责任。每段都有自己的状态、缓存和超时。"
**回答读者三问**: ①DNS 为什么有缓存=减少权威查询和连接前延迟；②WebSocket 为什么能推送=HTTP Upgrade 后变成双向帧流；③502 怎么定责=对齐客户端、代理、上游三段时间线和抓包。

---

### 核心悬念

**"CDN、反向代理和 Nginx 都在应用层；Docker 容器与 Kubernetes Pod 在网络层面如何通过 namespace、veth、bridge、Netfilter 和 kube-proxy 互联？"**

→ 引出 14-container-network — 容器网络——从 Web 请求全链路进入 Linux namespace 与 Kubernetes Service。