# 百万并发: TCP内存开销 + 连接队列 + fd限制 + socket并发安全

> Cluster C: 6 KPs | 依赖: 07-IO模型, 08-epoll | 读者基线: epoll/Reactor

---

### 1. 单条TCP连接内核内存开销 — 到底吃多少内存
  - tcp_sock: 约2000字节(状态/cwnd/rtx/ts等所有TCP参数) (include/linux/tcp.h → tcp_sock)
  - inet_sock + sock: 约800字节(Sk_buff接收/发送队列+等待队列+socket通用字段) (include/net/sock.h → sock, include/net/inet_sock.h → inet_sock)
  - 收发缓冲区: sk_rcvbuf(默认约87KB, tcp_rmem控制)+sk_sndbuf(默认约16KB, tcp_wmem控制) (sysctl net.ipv4.tcp_rmem/wmem)
  - 页分配: 每个接收skb+payload约2KB(含skb_head+data fragment), 实际内存取决于缓冲区内排队skb数量
  - 总计估算: 一条ESTABLISHED空闲TCP连接≈3.5-5KB, 有数据排队时更高 (net/ipv4/tcp_memcontrol.c → tcp_init_cgroup)
  - 100万连接: 3.5KB×100万≈3.5GB — 仅TCP结构, 不含应用层开销

### 2. 半连接队列 vs 全连接队列 — listen背后的两个队列
  - 半连接队列(syn queue): 收到SYN→发SYN+ACK→等ACK, 存request_sock (net/ipv4/tcp_ipv4.c → tcp_v4_conn_request → inet_csk_reqsk_queue_add)
  - 全连接队列(accept queue): 三次握手完成→ESTABLISHED但未accept, 存完成握手的sock (net/ipv4/tcp_ipv4.c → tcp_v4_syn_recv_sock → inet_csk_reqsk_queue_add)
  - listen backlog: `listen(fd, backlog)` — 实际队列长度=min(backlog, /proc/sys/net/core/somaxconn) (net/socket.c → __sys_listen)
  - 队列满后果: syn queue满→SYN Cookie介入; accept queue满→新完成的连接被丢弃(应用层丢连接) (net/ipv4/tcp_ipv4.c → tcp_v4_conn_request → cookie检查)

### 3. 队列溢出诊断 — ss -s 与 nstat 实战
  - `ss -s`: 显示Total/ESTAB/SYN-RECV/TIME-WAIT状态统计 (net/ipv4/tcp_diag.c → tcp_diag_get_info)
  - 溢出计数器: `nstat -az | grep TcpExt` — ListenOverflows(全连接溢出次数), ListenDrops(半连接溢出次数)
  - 溢出行为: 系统调用accept队列满→新完成的连接直接被丢弃, 无RST/ICMP通知 (net/ipv4/tcp_minisocks.c → tcp_check_req)
  - 调优: 增大somaxconn和tcp_max_syn_backlog, 但更重要的是应用层加速accept (net/ipv4/tcp_ipv4.c → tcp_v4_syn_recv_sock)
  - tcp_abort_on_overflow: 溢出时发RST给客户端(0默认不发=静默丢弃) (/proc/sys/net/ipv4/tcp_abort_on_overflow)

### 4. 百万并发实现 — fd限制/SO_REUSEPORT/内核参数调优
  - fd数量限制: `ulimit -n`(单进程nofile, 默认1024), `/proc/sys/fs/nr_open`(系统最大fd), `/proc/sys/fs/file-max`(全局最大打开文件数) (fs/file.c → expand_files)
  - SO_REUSEPORT: 多进程/线程绑定同IP+Port→内核负载均衡分发新连接 — 绕过单进程fd限制 (net/core/sock_reuseport.c → reuseport_select_sock)
  - 内核参数: tcp_mem(全局TCP内存限制), tcp_rmem/tcp_wmem(每连接缓冲区), somaxconn(全连接队列全局上限)
  - 网卡多队列: RSS(Receive Side Scaling)将收包分发到多CPU的RX队列, 配合应用层多线程epoll (drivers/net/ → 驱动RSS配置)
  - CPU亲和性: 线程绑CPU核, 网卡中断绑CPU核, 应用线程与中断同核避免跨核缓存失效

### 5. socket并发安全 — TCP/UDP的线程安全模型
  - TCP socket并发: 一个fd在多个线程中同时读/写是安全的(内核有sock锁), 但不保证消息边界 — 可能交错 (net/ipv4/tcp.c → tcp_sendmsg_locked / tcp_recvmsg → lock_sock)
  - 内核锁机制: lock_sock(sk)/release_sock(sk) 保护send/recv的并发, 但应用层仍需消息协议 (net/core/sock.c → lock_sock)
  - UDP socket: 无连接状态, sendto/recvfrom并发写是安全的, 但单次sendto是原子的(≤MTU时) (net/ipv4/udp.c → udp_sendmsg)
  - SO_REUSEPORT安全: 内核计算hash(srcIp+srcPort+dstIP+dstPort)选后端, 同五元组始终到同一socket (net/core/sock_reuseport.c → reuseport_select_sock)
  - 最佳实践: 一核一线程一epoll — 避免共享socket fd的锁竞争

### 6. 收束
  - 100万连接≈3.5GB仅TCP结构内存 — 瓶颈通常是用户态数据结构(RingBuffer/应用buf)而非内核
  - 连接队列溢出是"应用层没及时accept"的无声杀手: 计数器在增加但日志没报错
  - SO_REUSEPORT+网卡RSS+CPU亲和性三点组合是百万并发的标准架构

---

### 核心悬念
**"百万连接是有了, 但Docker容器里的网络怎么和宿主机互通? k8s的Service又是怎么做到服务发现的?"**

→ 引出 Netfilter与iptables(10-netfilter-nat) — 容器网络的底层基础设施
