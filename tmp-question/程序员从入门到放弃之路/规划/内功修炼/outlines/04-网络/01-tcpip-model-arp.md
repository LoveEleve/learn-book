# TCP/IP四层模型 + ARP协议 + 网络设备分层

> Cluster A: 9 KPs | 依赖: 无 | 读者基线: 编程基础

---

### 1. TCP/IP四层 vs OSI七层 — 协议栈分层思想
  - TCP/IP四层: 链路层→网络层(IP)→传输层(TCP/UDP)→应用层(HTTP) (RFC 1122 架构定义)
  - OSI七层为何未落地: 会话/表示层在实践中被应用层合并 (RFC 1122 §1.1)
  - 分层价值: 每层独立演进 — 链路层从以太网到Wi-Fi, IP层从v4到v6, 上层无感 (net/ipv4/af_inet.c → inet_init)
  - 关键区别: TCP/IP先有实现后有模型, OSI先有模型后有实现 (RFC 1122 §1.2)
  - 网络字节序 big-endian: `htons/htonl/ntohs/ntohl` 将主机序转换为网络序(大端) — 跨架构移植的基础, x86(小端)与SPARC(大端)通信前必须转换 (glibc <netinet/in.h>)

### 2. PDU与封包/解包 — 数据在各层的形态
  - 应用层: Message → 传输层: Segment(TCP)/Datagram(UDP) → 网络层: Packet → 链路层: Frame (include/linux/skbuff.h → sk_buff 承载所有层头)
  - 封包(add header): 每层加自己的协议头在payload前 (net/ipv4/tcp_output.c → tcp_transmit_skb)
  - 解包(strip header): 收包时每层剥掉对应头, 沿协议栈上行 (net/ipv4/ip_input.c → ip_rcv)
  - 实际载体: sk_buff 是内核中贯穿所有层的统一数据结构 (include/linux/skbuff.h → sk_buff 结构体)
  - 面向连接 vs 无连接: TCP三次握手建连接→有序传输, UDP直接发包→无序可能丢 (net/ipv4/tcp_ipv4.c → tcp_v4_connect)

### 3. 以太网帧格式 + MAC地址 — 链路层基础
  - 以太网帧: 前导码(8)→目的MAC(6)→源MAC(6)→Type(2)→Payload(46-1500)→CRC(4) (RFC 894 / include/uapi/linux/if_ether.h → ETH_HLEN)
  - MAC地址: 48位全球唯一(OUI 24位 + NIC Specific 24位) (include/linux/if_ether.h → ETH_ALEN)
  - 二层交换: 交换机学习MAC→端口映射, 未知目的MAC→泛洪所有端口 (net/bridge/br_fdb.c → br_fdb_update)
  - VLAN: 802.1Q标签(TPID+Priority+CFI+VID)实现二层隔离 (include/uapi/linux/if_vlan.h → vlan_ethhdr)
  - 广播域: 同一VLAN/交换机内广播帧可达所有端口, 路由器隔离广播域

### 4. ARP协议 — IP到MAC的转换桥梁
  - ARP请求: 广播问"谁有192.168.1.5?告诉192.168.1.1" (net/ipv4/arp.c → arp_solicit)
  - ARP应答: 目标单播回复"我有, MAC是aa:bb:cc:dd:ee:ff" (net/ipv4/arp.c → arp_process)
  - ARP缓存表: 内核维护arp_tbl保存最近解析结果, 过期时间可配置 (/proc/sys/net/ipv4/neigh/default/gc_stale_time)
  - Gratuitous ARP: 源IP=目标IP的ARP请求, 用于IP冲突检测和故障切换通告 (net/ipv4/arp.c → arp_process 处理GARP)
  - 代理ARP: 路由器代替目标主机应答, 实现跨子网透明通信 (/proc/sys/net/ipv4/conf/*/proxy_arp)
  - ARP缓存未命中时: 内核缓存待发送skb到neigh->arp_queue, 等ARP应答后继续发送 (net/core/neighbour.c → __neigh_event_send)

### 5. 网络设备分层 — NIC/交换机/路由器/防火墙角色
  - NIC(网卡): 将数字信号转换为物理信号(电/光), DMA直接将数据写入内存环形缓冲区 (drivers/net/ → ndo_start_xmit)
  - 二层交换机: 按MAC地址转发, 核心数据结构是FDB转发表 (net/bridge/br_fdb.c)
  - 三层层路由器: 按IP路由表转发, 查FIB(Longest Prefix Match)决定下一跳 (net/ipv4/fib_trie.c → fib_table_lookup)
  - 防火墙/L4负载均衡: 工作在传输层, 按五元组(源IP+源端口+目的IP+目的端口+协议)处理 (net/netfilter/)
  - L7反向代理: 解析HTTP/HTTPS内容, 按URL/Host/Cookie路由 (应用层)

### 6. ICMP 与 ping — 网络层的诊断协议
  - ICMP 包格式: Type(1B)+Code(1B)+Checksum(2B)+Message Body(可变) — 封装在IP包内(protocol=1) (RFC 792 / net/ipv4/icmp.c → icmp_rcv)
  - 处理入口: icmp_rcv 解析Type→dispatcher分发 — 不是所有ICMP都到用户态, 内核直接处理重定向/时间戳 (net/ipv4/icmp.c → icmp_rcv)
  - ping 原理: 发送 Echo Request(Type=8 Code=0)→目标回复 Echo Reply(Type=0 Code=0)→计算RTT (net/ipv4/ping.c → ping_rcv)
  - 常见 ICMP Type: 3(Destination Unreachable, Code细分端口/主机/协议不可达), 5(Redirect, 告知更好的下一跳), 11(Time Exceeded Code=0, Traceroute依赖) (net/ipv4/icmp.c → icmp_unreach / icmp_redirect)
  - ICMP 不通 ≠ 服务不通: 防火墙/安全组可能仅拦截ICMP而放行TCP/80 — ping不通但curl成功是正常现象 (net/ipv4/netfilter/ 中REJECT规则)

### 7. 收束
  - TCP/IP四层是"分层解耦+逐层封装"的核心思想, sk_buff是贯穿所有层的统一数据结构
  - ARP是IP→MAC的关键转换, 内核通过邻居子系统管理ARP缓存(ntbl/gc/队列机制)
  - 网络设备各层职责分明: NIC负责信号, 交换机MAC转发, 路由器IP路由, 防火墙五元组过滤

---

### 核心悬念
**"TCP三次握手时, ARP请求和SYN包谁先发? 如果ARP缓存为空, 第一个SYN会丢吗?"**

→ 引出 TCP状态机与三次握手(02-tcp-state-machine)
