# Netfilter五链四表 + iptables规则 + NAT与穿透

> Cluster D: 5 KPs | 依赖: 01-协议基础 | 读者基线: TCP/IP基础 + Linux

---

### 1. Netfilter五链 — 包在协议栈中的5个钩子点
  - NF_INET_PRE_ROUTING: 所有入站包第一站 — 适合DNAT(改目的地址) (include/uapi/linux/netfilter.h → NF_INET_PRE_ROUTING)
  - NF_INET_LOCAL_IN: 路由判断发往本机后 — 适合INPUT过滤 (include/uapi/linux/netfilter.h → NF_INET_LOCAL_IN)
  - NF_INET_FORWARD: 路由判断转发(非本机)后 — 适合转发过滤/NAT (include/uapi/linux/netfilter.h → NF_INET_FORWARD)
  - NF_INET_LOCAL_OUT: 本机发出的包 — 适合OUTPUT过滤 (include/uapi/linux/netfilter.h → NF_INET_LOCAL_OUT)
  - NF_INET_POST_ROUTING: 所有出站包最后一站 — 适合SNAT(改源地址) (include/uapi/linux/netfilter.h → NF_INET_POST_ROUTING)
  - 钩子注册: nf_register_net_hook — 每个表在指定链注册回调函数 (net/netfilter/core.c → nf_register_net_hook)

### 2. 四表(raw/mangle/nat/filter) — 表的优先级与职责
  - raw表(优先级最高): 在连接跟踪前, 设置NOTRACK跳过conntrack — 减少CPU开销 (net/netfilter/nf_tables_api.c + net/ipv4/netfilter/iptable_raw.c)
  - mangle表: 修改包头(TOS/TTL/Mark), 在raw之后nat之前 — 用于策略路由(PBR)标记 (net/ipv4/netfilter/iptable_mangle.c)
  - nat表: 仅处理连接的首包(后续包conntrack自动NAT), DNAT在PRE/POST, SNAT在POST (net/netfilter/nf_nat_core.c → nf_nat_packet)
  - filter表(优先级最低): 数据包过滤(ACCEPT/DROP/REJECT), 三链INPUT/FORWARD/OUTPUT各有规则 (net/ipv4/netfilter/iptable_filter.c)
  - 表顺序: raw→mangle→nat→filter (同一链内) — 先跳过conntrack→后改头→再NAT→最后过滤 (net/netfilter/core.c → nf_hook_slow 按优先级遍历)

### 3. iptables规则结构与匹配 — 从用户态到内核
  - 规则组成: matches(匹配条件: src/dst IP, port, interface, state)+target(动作: ACCEPT/DROP/JUMP/DNAT/SNAT) (include/uapi/linux/netfilter/xt_connstate.h)
  - 连接跟踪(conntrack): 记录每个连接的state(NEW/ESTABLISHED/RELATED), 后续包不需重复规则匹配 (net/netfilter/nf_conntrack_core.c → nf_conntrack_in)
  - 实用规则: `-A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT` — 允许已建立连接的回包 (基于conntrack)
  - DROP vs REJECT: DROP(静默丢弃, 对端超时), REJECT(发ICMP unreachable, 对端立即知道) — DROP更安全但排查困难

### 4. NAT四模式 — SNAT/DNAT/NAPT/穿透
  - SNAT(源NAT): 修改出站包源IP — 内网上外网, POST_ROUTING链, nf_nat_setup_info注册 (net/netfilter/nf_nat_core.c → nf_nat_setup_info)
  - DNAT(目的NAT): 修改入站包目的IP — 外部访问内网服务, PRE_ROUTING链 (net/netfilter/nf_nat_core.c → nf_nat_setup_info NF_NAT_MANIP_DST)
  - NAPT(端口NAT/PAT): SNAT+端口映射 — 多内网IP共用一个公网IP, 用端口区分连接 (net/netfilter/nf_nat_proto_tcp.c → nf_nat_ipv4_manip_pkt)
  - NAT穿透问题: P2P通信(如WebRTC/游戏)需要双方都能被对方直接访问 — NAT类型检测(RFC 3489) (cone vs symmetric)
  - 补充 — DHCP: 四阶段 Discover(客户端广播)→Offer(服务器分配IP)→Request(客户端确认)→Ack(服务器最终确认) (UDP 67/68端口, RFC 2131)。DHCP 获取 IP → NAT 转换 IP — 先有地址才能转换, 两者是获取/转换的不同阶段

### 5. NAT穿透 — STUN/TURN/ICE
  - NAT类型: Full Cone(宽松), Restricted Cone(IP限制), Port Restricted Cone(IP+Port限制), Symmetric(最严, 不同目标→不同映射端口)
  - STUN: 客户端向STUN服务器问"我的公网地址是什么?" — 建立映射, 仅在非对称NAT可用 (RFC 5389)
  - TURN: 中继服务器中转所有数据 — 当STUN失败时回退, 成本高(服务器带宽) (RFC 5766)
  - ICE: 收集所有候选地址(本机/STUN/TURN)→连通性检查→选最佳路径 — WebRTC标准 (RFC 8445)

### 6. 收束
  - Netfilter五链四表是Linux网络功能的基础设施: iptables/K8s Service/Docker端口映射全部依赖它
  - 连接跟踪conntrack是NAT/iptables state匹配的核心 — conntrack表满会导致新连接被拒绝(调大nf_conntrack_max)
  - NAT六字真言: DNAT在PRE_ROUTING改目的, SNAT在POST_ROUTING改源

---

### 核心悬念
**"NAT数据包改IP头, 那HTTPS是在之前加密还是之后加密? TLS握手到底干了什么?"**

→ 引出 SSL/TLS握手与证书链(11-ssl-tls)
