# 04-网络 — 多视角完备性提问

> 6 视角 × 5 题 = 30 题 | 2026-08-08

---

## 视角1: 开发者 (写代码的人)

1. 一个 send(buf, 1500) 调用, 内核在 tcp_sendmsg 中如何决定是发一个包还是分成两段? MSS 是如何从 MTU 推算出来的?
2. epoll_create1→epoll_ctl(ADD)→epoll_wait 三者交互中, ep_poll_callback 在哪个内核上下文执行? 为什么不能在回调中做耗时操作?
3. TFO(TCP Fast Open) 需要客户端和服务端同时开启还是任一方开启即可? 首次连接和后续连接的 RTT 差异是多少?
4. SO_REUSEPORT 和 SO_REUSEADDR 的区别是什么? 在生产环境为什么 SO_REUSEPORT 对百万并发是必需的?
5. TLS 1.3 的 0-RTT 数据如果被重放, 应用层如何防御? 什么类型的请求不应该在 0-RTT 阶段执行?

---

## 视角2: 性能工程师 (优化延迟和吞吐)

1. 内核收包路径中, NAPI 的 weight=64 对性能和延迟有何影响? 增大 weight 会减少中断但增加延迟 — 这个 tradeoff 的拐点在哪里?
2. GRO 合并率受什么因素影响? 如何通过 /proc 或 ethtool 观察 GRO 合并效率?
3. Nagle+延迟ACK 互相等待 200ms 的经典场景中, 为什么内核不能自动检测并突破? tcp_nagle_check 的逻辑是什么?
4. HTTP/2 多路复用的 Stream 流控窗口和 TCP 流控窗口(cwnd/rwnd)是如何叠加的? 两层流控是否产生级联背压?
5. 100 万 TCP 连接占 ~3.5GB 内存 — 这个数是怎么计算出来的? tcp_sock/sk_buff/sk_rcvbuf 各自占比多少?

---

## 视角3: SRE/排错 (线上故障处理)

1. 服务端出现大量 CLOSE_WAIT — 排查命令是什么? CLOSE_WAIT 状态下应用层还能读数据吗? 为什么必须应用层 close 才能释放?
2. `nstat -az | grep TcpExt` 看到 ListenOverflows 增长 — 你如何区分是 syn queue 满还是 accept queue 满? 各自调哪个内核参数?
3. 用户反馈 HTTP 偶尔 502 — Wireshark 抓包发现代理→上游 TCP SYN 后无 ACK — 可能的根因有哪三类? 如何进一步区分?
4. conntrack 表满导致丢包 — 如何确认是 conntrack 问题? 临时和永久修复方案各是什么?
5. kube-proxy iptables 相比 IPVS 模式, 在 100 个 Service × 50 个 Endpoint 场景下, 每个新连接匹配 iptables 规则需要遍历多少条?

---

## 视角4: 架构师 (设计决策与方案选择)

1. 一个内部 RPC 框架需要在 HTTP/1.1、HTTP/2、gRPC(HTTP/2+Protobuf) 之间选择 — 在延迟/吞吐/调试便利性/生态兼容性四个维度上如何权衡?
2. 选型时需不需要上 QUIC/HTTP/3? 什么场景下 QUIC 的连接迁移是刚需(非锦上添花), 什么场景下 QUIC 的反向代理生态不成熟会是阻塞点?
3. CDN 缓存策略中, 静态资源如何平衡缓存命中率和更新及时性? Cache-Control + Purge + 版本化URL 三种方案各有什么代价?
4. k8s 网络选型: flannel(VXLAN) vs calico(BGP) vs cilium(eBPF) — 在生产集群 1000 Node 级别各有什么优缺点?
5. 微服务间通信: 选长连接(连接池)还是短连接? TIME_WAIT 在短连接场景的影响该如何量化评估?

---

## 视角5: 研究者/深层追问 (为什么这样设计)

1. TCP 为什么选择 seq=初始随机值而非从 0 开始? 如果 seq 从 0 开始, 会引发哪三类安全问题?
2. ARP 协议自 1982 年(RFC 826)至今未大改 — 为什么在现代数据中心 SDN/VXLAN 架构下它仍是必需? ARP over VXLAN 的额外开销是多少?
3. CUBIC 和 BBR 对 Bufferbloat 的处理理念完全不同 — 一个"填满即丢", 一个"感知即退" — 这是否意味着 BBR 彻底淘汰了基于丢包的拥塞控制?
4. epoll 的红黑树+就绪链表设计是教科书级优化 — 但为什么 io_uring 要用共享内存环形缓冲区而非沿用事件通知模型? 这什么? 是对 epoll 设计模式的彻底否定还是补充?
5. DNS 从 1983 年(RFC 882)至今 — 为什么它仍是互联网最脆弱的单点? DNSSEC 和 DoH(基于HTTPS的DNS)分别解决了哪些问题, 又带来了哪些新问题?

---

## 视角6: 学生 (从零开始理解网络)

1. TCP 为什么需要三次握手而不是两次? 如果只握手两次, 半连接和服务端状态有什么问题? (参考 02)
2. epoll 比 select 快在哪里? 不用"红黑树"术语, 用"你负责一个前台, select 是每1秒对所有客人按顺序问一遍, epoll 是客人到了自己按铃"这个比喻解释 (参考 07-08)
3. HTTP/2 多路复用解决了 HTTP/1.1 的什么问题? 为什么一个网页打开时, HTTP/1.1 需要6个TCP连接但 HTTP/2 只需要1个? (参考 12)
4. Docker 容器里 curl google.com, 经历了哪些网络步骤? 从容器 veth → 宿主机 bridge → SNAT → 物理网卡 → 互联网, 写出一条完整的包路径 (参考 14/10)
5. ping 不通但 curl 能成功, 为什么可能? ICMP 和 TCP 在网络栈中是两条不同的路径, 防火墙可能只拦截其中一个 (参考 01/10)

---

## 覆盖映射

| 视角 | 题数 | 覆盖域 |
|------|:----:|--------|
| 开发者 | 5 | 01-04 TCP/EPOLL/TFO/SO_/TLS |
| 性能工程师 | 5 | 03-06 NAPI/GRO/Nagle/HTTP2/内存 |
| SRE/排错 | 5 | 02/04/09/13/14 CLOSE_WAIT/队列溢出/502/conntrack/kube-proxy |
| 架构师 | 5 | 12-14 QUIC/CDN/CNI/连接池 |
| 研究者 | 5 | TCP设计/DNS历史/BBR vs CUBIC/ARP演进/io_uring vs epoll |
| 学生 | 5 | 02/07-08/10/12/14 握手/epoll/HTTP2/Docker/ICMP |

**总计: 30 题, 14 篇全覆盖**
