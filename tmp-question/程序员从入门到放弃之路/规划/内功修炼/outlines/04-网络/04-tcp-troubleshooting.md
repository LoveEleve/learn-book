# TCP生产排错全景: TIME_WAIT/优雅关闭/Nagle/粘包/SYN Cookies/TFO/RST

> Cluster B: 13 KPs | 依赖: 02-TCP状态机, 03-流控拥塞 | 读者基线: TCP机制全貌

---

### 1. TIME_WAIT与2MSL — 为什么等2倍最大段寿命
  - TIME_WAIT时长: 2MSL(Maximum Segment Lifetime), Linux默认60秒 (include/net/tcp.h → TCP_TIMEWAIT_LEN)
  - 等什么: (1)确保最后的ACK被对端收到, (2)让旧连接的延迟包在网络中消散 (RFC 793 §3.5)
  - 问题: 短连接高并发时大量TIME_WAIT消耗端口(65535上限) — 用netstat/ss看到满屏TIME_WAIT
  - 内核timewait结构: tcp_timewait_sock, 轻量版tcp_sock只保留必要字段 (include/net/tcp.h → tcp_twsk_unique)
  - 解决方案: SO_REUSEADDR(服务端绑定), SO_LINGER(发RST快速关闭), tcp_tw_reuse(客户端复用) (net/ipv4/tcp_ipv4.c → tcp_v4_connect 端口选择)
  - tcp_tw_recycle 已废弃(4.12移除): 因破坏NAT环境连接, 内核放弃此选项

### 2. 优雅关闭 — close vs shutdown 的本质区别
  - close: 减少fd引用计数, 引用计数=0时发FIN, 无法再读写 (net/ipv4/tcp.c → tcp_close)
  - shutdown(SHUT_WR): 只关闭写方向→发FIN, 读仍可用 — 实现半关闭 (net/ipv4/tcp.c → tcp_shutdown)
  - 优雅关闭流程: 服务端先shutdown(SHUT_WR)发FIN→继续读客户端数据→客户端发FIN→服务端close (net/ipv4/tcp.c → tcp_shutdown + tcp_recvmsg)
  - SO_LINGER: linger.l_onoff=1, linger.l_linger=0 → close时不发FIN直接发RST (net/ipv4/tcp.c → tcp_close → tcp_send_active_reset)
  - 生产陷阱: 客户端close()后未等ACK就退出→服务端CLOSE_WAIT堆积→fd泄漏 (排查: ss -tan state close-wait)

### 3. Nagle算法 + 延迟ACK冲突 — 200ms的经典坑
  - Nagle算法: 前一个数据ACK未到达前不发小于MSS的包→合并小包 (net/ipv4/tcp_output.c → tcp_nagle_check)
  - 延迟ACK: 收到数据后不立即ACK, 等40ms看有没有数据可回带(或等第二个包) (net/ipv4/tcp_input.c → tcp_send_delayed_ack)
  - 冲突场景: 客户端发小包→Nagle等ACK不发→服务端延迟ACK等数据→互相等≈200ms (TCP_NODELAY解Nagle, TCP_QUICKACK解延迟ACK)
  - 内核解决: 第一个未确认段不应用Nagle(避免初始延迟), 且小包阈值动态调整 (net/ipv4/tcp_output.c → tcp_minshall_check)

### 4. TCP粘包 — 流式语义不是bug
  - 根本原因: TCP是字节流, 无消息边界 — 两次send可能被一次recv读出, 或一次send被两次recv读到 (net/ipv4/tcp.c → tcp_recvmsg 流式拼接)
  - 三种解决方案: (1)定长消息, (2)分隔符(如\r\n), (3)消息头+body(前4字节=body长度)
  - 实际发送: send 100B → 可能被TCP拆成50B+50B(受MSS/cwnd影响), 或2次send合并成200B(Nagle) (net/ipv4/tcp.c → tcp_sendmsg_locked)
  - 与UDP对比: UDP是datagram — send一次=收一次完整包, 不存在粘包 (net/ipv4/udp.c → udp_recvmsg)
  - TCP分段≠粘包: 分段是TCP层行为(可组合), 粘包是应用层问题(需要消息边界)

### 5. SYN Cookies + TFO + RST + 无accept建连接
  - SYN Cookie: SYN Flood攻击→半连接队列满→内核用Cookie算法验证SYN合法性, 不在半连接队列存req (net/ipv4/tcp_ipv4.c → tcp_v4_conn_request → cookie_v4_check)
  - Cookie原理: 第一次SYN时用时间戳+MSS+对端信息生成seq(don't store), SYN+ACK带特殊seq, 第三次ACK验证seq合法→建连接 (net/ipv4/syncookies.c → cookie_v4_init_sequence)
  - TCP Fast Open(TFO): 首次SYN带Cookie→后续SYN带Cookie+数据, 0-RTT发数据 (net/ipv4/tcp_fastopen.c → tcp_fastopen_cookie_gen)
  - RST行为: 收到RST不一定断开 — ESTABLISHED收到合法RST(SackOK seq匹配)才断开, 否则丢弃 (net/ipv4/tcp_input.c → tcp_reset_check)
  - 无accept也能建连接: 三次握手在内核完成, backlog+1, accept只是从全连接队列取 (net/ipv4/tcp_ipv4.c → tcp_v4_conn_request → inet_csk_reqsk_queue_add)

### 6. TCP丢包四种原因 + 排错命令
  - (1)网络拥塞→路由器丢包(sack重传), (2)校验和错误→内核丢弃, (3)接收缓冲区满→通告rwnd=0, (4)TCP段乱序→收Duplicate ACK
  - 诊断: `ss -ti` (TCP Info: retrans/cwnd/rtt), `nstat -az | grep TcpExt` (内核统计), `/proc/net/snmp` (计数器), `tcpdump -i eth0 'tcp'` (抓包)
  - 超时重传: RTO超时→重传SYN/FIN/数据, RTO基于RTT动态计算(SRTT+4×RTTVAR) (net/ipv4/tcp_input.c → tcp_set_rto)

### 7. 收束
  - TIME_WAIT/CLOSE_WAIT是高频排错场景: 前者消耗端口, 后者泄漏fd — ss/tcpdump是排错核心工具
  - Nagle+延迟ACK互等200ms是"TCP慢"的经典根因 — TCP_NODELAY解决
  - SYN Cookies/TFO无accept建连证明: 连接建立全在内核完成, accept只是从队列取出

---

### 核心悬念
**"一个包从网线进来, 到用户态recv读到数据, 内核到底经过了哪些步骤?"**

→ 引出 内核收包全链路(05-kernel-recv-path)
