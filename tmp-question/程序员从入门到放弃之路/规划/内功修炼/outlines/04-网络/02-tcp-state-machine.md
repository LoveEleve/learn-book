# TCP包格式 + 11状态迁移 + 三次握手/四次挥手

> Cluster B: 13 KPs | 依赖: 01-协议基础 | 读者基线: TCP/IP四层模型

---

### 1. TCP包格式 — 20+字段的协议设计
  - 固定头部20字节: SrcPort(16)+DstPort(16)+Seq(32)+AckNum(32)+Offset(4)+Flags(9)+Win(16)+Checksum(16)+UrgPtr(16) (include/uapi/linux/tcp.h → tcphdr)
  - 核心字段: Seq(字节流序号, 每个字节一个序号), AckNum(期望收到的下一个Seq), Window(接收方剩余缓冲区大小) (RFC 793 §3.1)
  - Flags位: SYN(建连)/ACK(确认)/FIN(关闭)/RST(重置)/PSH(推送)/URG(紧急) — 可同时设多位如SYN+ACK (include/uapi/linux/tcp.h → TCPHDR_SYN/ACK/FIN/RST)
  - Options段(可变长): MSS大小协商(1460), SACK Permitted, Timestamp, Window Scale (net/ipv4/tcp_output.c → tcp_syn_options)
  - TCP校验和: 覆盖TCP伪头(IP)+TCP头+数据, 确保端到端完整性 (net/ipv4/tcp_ipv4.c → tcp_v4_checksum_init)
  - 紧急指针: URG=1时UrgPtr有效, 标记紧急数据偏移 (几乎不用, 被带外数据淘汰)

### 2. TCP状态机 — 11种状态的完整迁移
  - 状态枚举: CLOSED/LISTEN/SYN_SENT/SYN_RCVD/ESTABLISHED/FIN_WAIT1/FIN_WAIT2/CLOSING/TIME_WAIT/CLOSE_WAIT/LAST_ACK (include/net/tcp_states.h → TCPF_*)
  - 服务端路径: CLOSED→LISTEN→SYN_RCVD→ESTABLISHED→CLOSE_WAIT→LAST_ACK→CLOSED (net/ipv4/tcp.c → tcp_set_state)
  - 客户端路径: CLOSED→SYN_SENT→ESTABLISHED→FIN_WAIT1→FIN_WAIT2→TIME_WAIT→CLOSED (net/ipv4/tcp.c → tcp_rcv_state_process)
  - 状态迁移触发: 收到对应flags的TCP包 + 应用层系统调用(connect/listen/close) (net/ipv4/tcp_input.c → tcp_rcv_state_process)
  - 调试命令: `ss -tan` 或 `netstat -tan` 查看当前所有连接状态 (socket diag: net/ipv4/tcp_diag.c)

### 3. 三次握手 — 建连的精确时序
  - 第一次: Client→Server SYN (Seq=X, MSS=1460, Win=65535) — 客户端进入SYN_SENT (net/ipv4/tcp_output.c → tcp_connect → tcp_connect_init)
  - 第二次: Server→Client SYN+ACK (Seq=Y, Ack=X+1) — 服务端进入SYN_RCVD, 分配半连接req (net/ipv4/tcp_ipv4.c → tcp_v4_conn_request)
  - 第三次: Client→Server ACK (Ack=Y+1) — 双方进入ESTABLISHED (net/ipv4/tcp_ipv4.c → tcp_v4_do_rcv → tcp_child_process)
  - ISN(初始序列号): 基于时钟+随机Hash生成, 防序列号预测攻击 (net/core/secure_seq.c → secure_tcp_seq)
  - 内核关键数据结构: request_sock (半连接), tcp_sock (全连接) — 握手完成时从req迁移到全连接sock (include/net/request_sock.h)

### 4. 四次挥手 — 优雅关闭的双向FIN
  - 主动关闭方: 调用close→发FIN→进入FIN_WAIT1, 收ACK→进入FIN_WAIT2, 收对端FIN→发ACK→进TIME_WAIT (net/ipv4/tcp.c → tcp_close)
  - 被动关闭方: 收FIN→发ACK→进入CLOSE_WAIT(应用可继续读), 应用调close→发FIN→LAST_ACK, 收ACK→CLOSED (net/ipv4/tcp.c → tcp_close)
  - 为什么四次: TCP是全双工 — 每方向独立关闭, 主动方不发了≠被动方也不发了 (半关闭状态: net/ipv4/tcp.c → tcp_shutdown)
  - 同时关闭: 双方同时发FIN→都进FIN_WAIT1→收FIN→发ACK→都进CLOSING→收ACK→都进TIME_WAIT (net/ipv4/tcp_input.c → tcp_rcv_state_process CLOSING处理)

### 5. 异常状态 — 同时打开与RST
  - 同时打开: 双方同时发SYN→都进SYN_SENT→收SYN+ACK→都发ACK→都进ESTABLISHED — 产生一条连接(非两条) (RFC 793 §3.4 图8)
  - RST触发: 收到无listening端口SYN→RST; ESTABLISHED收到非法ACK→RST; SO_LINGER且linger=0→RST (net/ipv4/tcp_ipv4.c → tcp_v4_send_reset)
  - SYN_RCVD超时: 半连接队列中的SYN_RECV在超时后清理, 默认重试5次(synack重传) (include/net/tcp.h → TCP_SYNACK_RETRIES)

### 6. 收束
  - TCP状态机11态是理解所有TCP行为的基础图, 状态迁移由"收到包+应用调用"联合触发
  - 三次握手的关键是ISN随机化和半连接→全连接的两阶段安全设计
  - 四次挥手是双向关闭的必然结果 — TCP全双工意味着每方向独立关闭

---

### 核心悬念
**"如果主动关闭方直接跳到TIME_WAIT等2MSL, 那这2MSL到底在等谁的包?"**

→ 引出 滑动窗口与拥塞控制(03-tcp-flow-congestion)
