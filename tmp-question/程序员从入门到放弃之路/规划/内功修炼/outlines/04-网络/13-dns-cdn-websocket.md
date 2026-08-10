# DNS + CDN + WebSocket + Wireshark生产排查

> Cluster E: 6 KPs | 依赖: 11-ssl-tls, 12-http-evolution | 读者基线: HTTP/TLS/TCP

---

### 1. DNS迭代 vs 递归查询 — 域名解析的两条路径
  - 递归查询: 客户端→本地DNS→根DNS→顶级DNS→权威DNS, 本地DNS代劳全路径 — 客户端只收最终结果 (RFC 1034 §4.3.1)
  - 迭代查询: 客户端→本地DNS, 本地DNS依次返回"去问根"/"去问.com"/"去问example.com" — 客户端(或本地DNS)自己做全链路查询 (RFC 1034 §4.3.2)
  - 实践: 用户→本地DNS(递归), 本地DNS→根/顶级/权威(迭代逐级) — 混合模式最常见
  - DNS报文: 12字节Header+Question(Body)+Answer(Body)+Authority(Body)+Additional(Body) (RFC 1035 §4.1)
  - 解析记录类型: A(IPv4), AAAA(IPv6), CNAME(别名), MX(邮件), NS(权威DNS), TXT(文本) (RFC 1035 §3.2)

### 2. DNS分级缓存 — 名字解析的加速链
  - 浏览器DNS缓存: chrome://net-internals/#dns, 通常1分钟 — 应用层第一级 (浏览器实现)
  - OS DNS缓存: systemd-resolved/nscd缓存 — 进程间共享 (glibc gethostbyname/getaddrinfo → resolve/nsswitch)
  - 本地DNS服务器: 如路由器/运营商DNS/8.8.8.8 — 多用户共享的大缓存池
  - TTL: 权威DNS设TTL决定每级缓存存活时间 — 短TTL(60s)便于故障切换, 长TTL(3600s)减少延迟 (RFC 1035 §4.1.3)
  - 实际延迟: 首次解析50-200ms(全链路), 缓存命中<1ms — DNS是每次新连接的首个延迟来源
  - 故障处理: DNS超时→重试→换DNS服务器 — `/etc/resolv.conf` 中nameserver的options timeout/attempts

### 3. CDN — 智能DNS + 回源 + 边缘节点
  - 智能DNS(GeoDNS): CDN自建DNS根据客户端IP返回最近边缘节点 — 北京用户→北京节点, 上海用户→上海节点
  - 回源: 边缘缓存未命中→向源站请求(可能跨大区域) — CDN的核心成本(回源率越低越省钱) (RFC 3568)
  - 边缘节点架构: L4负载均衡(DNS/AnyCast)→L7反向代理(Nginx/Varnish)→缓存层→源站 — 多层防御 (nginx反向代理+proxy_cache)
  - Cache-Control: `Cache-Control: max-age=3600, s-maxage=3600` — CDN按此缓存, 配合Purge/过期主动更新
  - 不适用CDN: 动态内容(实时数据), 写密集型(需回源), 强一致性(缓存过期不一致)

### 4. WebSocket — 从HTTP升级到全双工隧道
  - 握手: HTTP Upgrade请求(GET+Upgrade: websocket+Sec-WebSocket-Key)→服务端101 Switching Protocols+Sec-WebSocket-Accept (RFC 6455 §4)
  - 帧格式: 2-14字节帧头(FIN+Opcode+Mask+PayloadLen)+Payload — Text(0x1)/Binary(0x2)/Close(0x8)/Ping(0x9)/Pong(0xA) (RFC 6455 §5.2)
  - 客户端Mask: 客户端发送帧必须Masking key(随机4字节XOR payload) — 防中间缓存伪造攻击 (RFC 6455 §5.3)
  - 全双工: 握手后在TCP连接上双向收发帧 — 服务端可主动推送, 比HTTP轮询高效(N个连接→1个长连接)
  - 心跳: Ping/Pong帧保持连接 — 无应用数据时定期发Ping防中间代理关闭长连接 (RFC 6455 §5.5.2)

### 5. Wireshark 502排查方法论 — 从抓包到定责
  - 502 Bad Gateway: 反向代理收到上游错误响应 — 不是客户端/代理/上游三方哪个的问题, 需要抓包定位 (RFC 7231 §6.6.3)
  - 排查流程: (1)抓客户端/代理/上游三处pcap, (2)对比时间戳找延迟点, (3)看HTTP Response+TCP Flags定责
  - 上游无响应: 客户端→代理HTTP请求→代理→上游TCP SYN→无应答(或超时后RST)→代理返回502
  - 上游返回错误: 代理收到上游的4xx/5xx→透传(非502, 但可能是连接重置导致) — 区分"上游挂了(无响应)"和"上游拒绝了(有响应)"
  - Wireshark过滤: `http.response.code == 502` 快速定位, `tcp.analysis.retransmission` 找丢包, `Follow→HTTP Stream` 看完整请求响应
  - 工具组合: tcpdump抓包+Wireshark分析+ss看socket状态 — 三位一体定位

### 6. KCP — 游戏场景的可靠UDP协议
  - 核心思想: 在UDP上实现ARQ(自动重传), 牺牲部分通用性换取比TCP更低的延迟 — 专为游戏/音视频优化
  - ARQ自动重传: 精确RTO计算+FEC前向纠错 — 丢包不等超时, 冗余编码提前恢复 (KCP spec → ikcp_flush)
  - FEC前向纠错: 发送N个数据包+M个冗余包, 收到任意N个即可恢复全部 — 用带宽换延迟
  - 多路径调度: 同时用Wi-Fi+4G发送, 选最快到达的路径 — 运营商丢包时自动切换 (KCP multipath → smux)
  - KCP vs QUIC: QUIC牺牲设计复杂度换通用性(Web/视频), KCP牺牲通用性换极致延迟(游戏)
  - 原神案例: 动作类游戏需要<50ms延迟 — TCP重传等200ms不可接受, 纯UDP不可靠,KCP是平衡点

### 7. 收束
  - DNS是每次连接的"热身延迟" — 分级缓存(浏览器→OS→本地DNS→权威DNS)是性能关键, CDN通过GeoDNS将用户引到最近节点
  - WebSocket在HTTP/1.1基础上设计, 通过Upgrade协议从半双工切换到全双工 — 帧格式2-14字节头部+Mask/非Mask语义
  - Wireshark+502排查是生产环境必备技能: 客户端/代理/上游三处抓包+对比时间线=快速定责

---

### 核心悬念
**"CDN/反向代理/Nginx都是应用层的数据路径, 那Docker容器和k8s Pod在网络层面是怎么互联的?"**

→ 引出 容器网络: Namespace + veth + bridge + kube-proxy(14-container-network)
