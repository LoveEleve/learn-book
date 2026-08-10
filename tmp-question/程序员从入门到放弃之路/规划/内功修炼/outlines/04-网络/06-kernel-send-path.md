# 内核发包全链路: tcp_sendmsg → ip_queue_xmit → 邻居子系统 → dev_queue_xmit → TSO/GSO

> Cluster C: 12 KPs | 依赖: 05-收包路径 | 读者基线: 内核收包链 + sk_buff

---

### 1. tcp_sendmsg — 用户态数据进入内核
  - 系统调用: send/sendto/write→tcp_sendmsg, 将用户buf拷贝到内核sk_buff (net/ipv4/tcp.c → tcp_sendmsg)
  - send缓冲区: 数据先拷贝到sk_sndbuf(默认16KB-4MB, 受tcp_wmem控制), 若满→阻塞/返回EAGAIN (sysctl net.ipv4.tcp_wmem)
  - 分段: 按MSS切成合适大小的skb, 每个skb挂到sk_write_queue, TCP头由tcp_transmit_skb加上 (net/ipv4/tcp_output.c → tcp_write_xmit)
  - writev/scatter-gather: sendmsg可一次性传入多段iov, 内核迭代写入 (net/ipv4/tcp.c → tcp_sendmsg_locked)
  - TCP选项设置: TCP_NODELAY→禁用Nagle, TCP_CORK→合并小包(类似Nagle反转), 在发送前flush (net/ipv4/tcp.c → tcp_push)

### 2. tcp_transmit_skb → ip_queue_xmit — TCP发包→IP层
  - 构造TCP头: TCP Header + Options(MSS/Timestamp/SACK) + payload, checksum计算 (net/ipv4/tcp_output.c → tcp_transmit_skb)
  - 拥塞控制过滤: tcp_write_xmit循环中检查cwnd/snd_wnd, 决定可否发送 (net/ipv4/tcp_output.c → tcp_write_xmit)
  - 丢包队列: 已发送未ACK的skb保留在rtx_queue, ACK到达时从队列移除 (net/ipv4/tcp_output.c → tcp_rearm_rto)
  - 进入IP层: ip_queue_xmit→__ip_queue_xmit, 查找路由缓存(rtable)确定出口设备和下一跳 (net/ipv4/ip_output.c → ip_queue_xmit)
  - IP头构造: 填充Source IP, Dest IP, TTL, Protocol(TCP=6), 调用NF_INET_LOCAL_OUT钩子 (net/ipv4/ip_output.c → ip_output)

### 3. 邻居子系统 — IP→MAC的ARP转换
  - 邻居表: neighbour table存储IP↔MAC映射, 状态机: NUD_INCOMPLETE→REACHABLE→STALE→DELAY→PROBE (include/net/neighbour.h → neigh_parms)
  - ARP解析: neigh_resolve_output→状态为NUD_INCOMPLETE时调用arp_solicit发ARP请求 (net/core/neighbour.c → __neigh_event_send)
  - 缓存队列: ARP未完成时, skb暂存到neigh->arp_queue, 收到ARP应答后neigh_event_ns→发送队列中全部skb (net/core/neighbour.c → neigh_probe → neigh_event_ns)
  - 路由缓存: 路由表查找结果缓存到dst_entry, 包含出口设备、下一跳MAC获取方式 (net/ipv4/route.c → rt_dst_alloc)
  - ARP缓存管理: gc定时清理STALE条目, 可配置gc_stale_time等 (/proc/sys/net/ipv4/neigh/default/)

### 4. dev_queue_xmit → 网卡发送 — 进入驱动层
  - Qdisc排队: dev_queue_xmit→__dev_queue_xmit, 先过tc egress qdisc(fq_codel/fq_pie等)做流控 (net/core/dev.c → __dev_queue_xmit)
  - Qdisc类型: pfifo_fast(默认三优先队列), fq(公平队列每流一队列), fq_codel(BBR好搭档) (net/sched/)
  - 网卡驱动发送: dev_hard_start_xmit→ndo_start_xmit, 驱动将skb写入TX Descriptor→DMA完成 (drivers/net/ → ndo_start_xmit)
  - RingBuffer回收: TX完成中断→NAPI处理→释放skb→更新TX Ring位置→通过BQL(Byte Queue Limit)控制排队字节数 (net/core/dev.c → netdev_tx_completed_queue)

### 5. TSO/GSO — 分段卸载
  - TSO(TCP Segmentation Offload): 网卡硬件将大skb(≤64KB)切分成MSS小包, 减少CPU开销 (net/ipv4/tcp_output.c → tcp_tso_autosize)
  - 原理: tcp_write_xmit构建超大skb(≤65535的GSO_MAX_SIZE), 带上MSS信息→驱动层→网卡硬件切分 (net/core/dev.c → validate_xmit_skb)
  - GSO(Generic Segmentation Offload): TSO的软件版 — 网卡不支持TSO时, 软件在dev_queue_xmit前分段 (net/core/dev.c → dev_gso_segment)
  - GSO更通用: 支持UFO(UDP Fragmentation Offload), 不依赖硬件 — 这也是现在默认的路径 (net/core/dev.c → netif_needs_gso)
  - TSO vs GSO: TSO是硬件优化(性能更高), GSO是软件回退(通用性更好), 通常混合使用

### 6. 零拷贝发送 — sendfile/splice/mmap+write
  - sendfile: 文件fd→socket fd, DMA→Page Cache→socket缓冲区(不经过用户态) (fs/read_write.c → do_sendfile → kernel_sendpage)
  - splice: 两个fd间pipe传输, 零拷贝管道 (fs/splice.c → do_splice → splice_to_pipe)
  - mmap+write: 文件映射到用户态→直接写映射区域→内核仅拷贝到skb, 减少一次拷贝 (mm/mmap.c → do_mmap)
  - 对比: 传统read+write=4次拷贝(磁盘→page cache→用户→socket→网卡), sendfile=2次(page cache→socket→网卡) (net/ipv4/tcp.c → tcp_sendpage)

### 7. 收束
  - 发包路径5阶段: 用户态拷贝→TCP拥塞控制→路由+ARP→Qdisc排队→驱动发送, 每阶段都是性能优化点
  - 邻居子系统是IP→MAC的桥梁, ARP缓存命中率直接影响首包延迟
  - TSO/GSO/零拷贝是高吞吐场景的标准技术栈: TSO解决CPU开销, sendfile解决内存拷贝

---

### 核心悬念
**"理解内核收发路径后, 用户态怎么高效处理成千上万个连接? select处理1000个fd就扛不住了"**

→ 引出 IO模型与多路复用(07-io-models)
