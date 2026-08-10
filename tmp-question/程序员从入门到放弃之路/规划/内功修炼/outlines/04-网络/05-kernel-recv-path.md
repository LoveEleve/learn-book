# 内核收包全链路: DMA→RingBuffer→硬中断→NAPI→软中断→tcp_v4_rcv→socket

> Cluster C: 12 KPs | 依赖: 01-协议基础, 02-TCP状态机 | 读者基线: TCP机制 + 内核基础

---

### 1. 网卡DMA与RingBuffer — 数据进入内存的第一步
  - 网卡收包: PHY芯片将电/光信号→数字信号, MAC控制器提取以太帧, 校验CRC (drivers/net/ → 网卡驱动 ndo_open)
  - DMA写入: 网卡通过PCIe DMA直接把帧写入内核预先分配的RingBuffer(环形缓冲区) (drivers/net/ → dma_alloc_coherent)
  - RingBuffer结构: N个sk_buff指针的环形数组, 生产者(网卡DMA)写→消费者(内核NAPI)读 (include/linux/skbuff.h -> sk_buff)
  - 队列深度: ethtool -g eth0 查看RX Ring大小(默认256-4096), 过小导致丢包 (/proc/net/dev → rx_dropped)
  - NAPI开关: 现代驱动默认NAPI模式, 非NAPI(旧)每次中断处理一个包效率极低 (drivers/net/ → netif_napi_add)

### 2. 硬中断 → NAPI → 软中断 — 中断处理两阶段
  - 硬中断(top-half): 网卡发IRQ→CPU响应→NAPI禁用网卡中断→触发软中断NET_RX_SOFTIRQ→硬中断退出 (net/core/dev.c → __napi_schedule → raise_softirq_irqoff)
  - 为什么两阶段: 硬中断中不能sleep/占用太长, 软中断在ksoftirqd上下文做实际包处理 (kernel/softirq.c → __do_softirq)
  - NAPI轮询: ksoftirqd调用net_rx_action→driver poll函数→批量处理NAPI weight个包(默认64) (net/core/dev.c → net_rx_action)
  - 中断合并: 网卡收到多个包仅发一次中断(NAPI poll一次处理多个), 避免中断风暴 (drivers/net/ → 驱动NAPI poll实现)
  - 软中断调度: napi_schedule→__raise_softirq_irqoff(NET_RX_SOFTIRQ) 标记pending, 在softirq上下文执行 (include/linux/netdevice.h → napi_schedule)

### 3. netif_receive_skb → GRO — 进入协议栈前
  - 流量控制: netif_receive_skb过tc ingress qdisc(入向限速), 再到协议栈 (net/core/dev.c → __netif_receive_skb_core)
  - tap转发: 如果AF_PACKET socket监听该设备(BPF过滤), 拷贝一份skb到socket (net/packet/af_packet.c → packet_rcv)
  - GRO(Generic Receive Offload): 将同五元组的连续小包合并成大skb再上送协议栈, 减少CPU开销 (net/core/dev.c → napi_gro_receive)
  - GRO原理: 遍历gro_list, 五元组相同+seq连续→合并payload到第一个skb, 后续skb free (net/core/dev.c → dev_gro_receive)
  - GRO vs LRO: GRO在NAPI层(软件/硬件无关), LRO是硬件卸载(已淘汰, 破坏TCP端到端语义)

### 4. ip_rcv → Netfilter → ip_local_deliver — IP层处理
  - ip_rcv校验: IP头校验和检查, 版本号验证(IPv4=4), 头长度≥20 (net/ipv4/ip_input.c → ip_rcv)
  - Netfilter PREROUTING: skb经NF_INET_PRE_ROUTING链, 可能被DNAT修改目的IP (net/ipv4/netfilter/nf_defrag_ipv4.c)
  - 路由查找: ip_rcv_finish→fib_lookup(查FIB表)→ip_local_deliver(本机)或ip_forward(转发) (net/ipv4/route.c → ip_route_input_noref)
  - 分片重组: IP分片的包在ip_defrag中重组, 完整后才上送 (net/ipv4/ip_fragment.c → ip_defrag)
  - ip_local_deliver → Netfilter LOCAL_IN → ip_local_deliver_finish → 协议处理 (net/ipv4/ip_input.c → ip_local_deliver_finish)

### 5. tcp_v4_rcv → socket查找 → 用户态wakeup
  - TCP入口: ip_local_deliver_finish根据协议号(IPPROTO_TCP=6)调用tcp_v4_rcv (net/ipv4/tcp_ipv4.c → tcp_v4_rcv)
  - 根据四元组找sock: __inet_lookup_skb(srcIP,srcPort,dstIP,dstPort)→从ehash(已建立)或listening_hash查找 (net/ipv4/inet_hashtables.c → __inet_lookup_skb)
  - 数据放入receive queue: tcp_v4_do_rcv→tcp_rcv_established→tcp_data_queue→skb加入sk_receive_queue (net/ipv4/tcp_input.c → tcp_data_queue)
  - 唤醒用户态: sk->sk_data_ready→sock_def_readable→wake_up_interruptible(sk_sleep(sk)) 唤醒阻塞在recv/read的进程 (net/core/sock.c → sock_def_readable)
  - epoll联动: epoll监听的socket在sk_data_ready时回调ep_poll_callback, 将epitem加入就绪队列 (fs/eventpoll.c → ep_poll_callback)

### 6. 收束
  - 收包路径6阶段: DMA写入→硬中断→软中断NAPI轮询→GRO合并→IP层处理→TCP状态机→socket就绪队列
  - 性能瓶颈点: RingBuffer过小(丢包)/中断过频(软中断占100%CPU)/GRO合并率低(小包过多)
  - epoll唤醒链条: sk_data_ready→ep_poll_callback→就绪队列→用户态epoll_wait返回 — 是高性能网络的基础

---

### 核心悬念
**"收包是DMA自动推进的, 那发包呢? send调用后数据怎么从用户态送到网卡?"**

→ 引出 内核发包全链路(06-kernel-send-path)
