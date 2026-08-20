# Linux 内核收包全链路 — 从 NIC DMA 到 `recv()`/`epoll_wait()` 被唤醒

> Cluster C: 12 KPs | 依赖: 01-tcpip-model-arp、02-tcp-state-machine | 读者基线: TCP、NAPI、socket、epoll 基础
> 读者处境: 04 篇已经用 `tcpdump/ss` 讲线上症状；本篇回答“一个包从网线进来，到用户态 `recv()` 真正读到数据，中间经过了哪些队列和上下文？”
> 打开新视角: 收包不是一个函数调用，而是**硬件 DMA → NAPI 批处理 → GRO → IP/Netfilter → TCP 查 socket → 接收队列 → epoll 唤醒**的流水线

---

### 概念依赖链

```
01 分层/ARP + 02 TCP 状态机 → 本篇: Linux 收包路径
  ├─ §1 NIC DMA/RX queue(数据进入内存)
  ├─ §2 IRQ/NAPI/softirq(从中断切到批处理)
  ├─ §3 GRO/netif_receive_skb(进入协议栈)
  ├─ §4 IP/Netfilter/路由(决定本机还是转发)
  └─ §5 TCP/socket/epoll(从报文到用户等待队列)
先讲: 硬件 → 中断批处理 → 协议栈 → IP → TCP → socket
后续依赖: 06-kernel-send-path(反向的用户态到网卡路径)
```

### 叙事顺序

1. 问题引入——网卡收到一个 Ethernet frame 后，用户态阻塞在 `recv()` 的进程是怎么被叫醒的？（**Aha: DMA 只把包放进内存，真正让 `recv()` 返回的是 TCP 把数据挂进 socket 接收队列后的唤醒链**）
2. NIC DMA 与 RX queue——硬件把数据放到哪里
3. IRQ → NAPI → NET_RX_SOFTIRQ——为什么不在硬中断里处理完整协议栈
4. GRO 与 `netif_receive_skb`——批量合并后进入协议栈
5. IP/Netfilter/路由——本机交付还是转发
6. `tcp_v4_rcv` → socket queue → epoll——用户态终于被唤醒
7. 收束——收包 6 阶段与性能瓶颈

### 1. NIC DMA 与 RX queue — 数据先进入内存，不是直接进入进程

场景提示: 网卡收到光电信号后，CPU 还没执行 TCP 代码；数据究竟怎样从 PCIe 设备进入内核可见的内存？ [写作时展开]

关键设计: 网卡驱动启动时准备 RX queue/descriptor，设备收到帧后通过 DMA 写入预先准备的内存 buffer，并更新完成状态：

```[pseudocode]
设备打开:
  driver ndo_open
  → 分配 RX descriptors / page buffers
  → 把 buffer DMA 地址交给 NIC

收到 Ethernet frame:
  PHY/MAC 解码并做硬件级检查
  → NIC DMA 把帧数据写入 RX buffer
  → 更新 descriptor 状态/长度
  → 触发 MSI-X/IRQ 或中断合并通知 CPU
```

Why: 为什么网卡要 DMA，而不是每个字节都让 CPU 搬？——**网络包到达频率太高，CPU 逐字节搬运会把吞吐浪费在复制上**：DMA 让设备直接写内存，CPU 只处理 descriptor、协议和调度。RX ring 的大小、buffer 回收速度、驱动分配失败都会影响丢包；`ethtool -g` 可查看设备支持/当前 ring 参数，但具体默认值取决于网卡驱动。 [内核: 驱动通过 NAPI poll 回收完成的 RX descriptor；“ring 里存指针”不是所有驱动的统一内部布局]

比喻锚点: RX queue 像码头的卸货托盘，网卡把货箱放到指定托盘并贴完成标签，CPU 不必站在码头边亲手搬每个包裹。 [写作时展开]

### 2. IRQ → NAPI → NET_RX_SOFTIRQ — 从硬中断切到批处理

场景提示: 每个包都触发一次完整中断会发生什么？为什么 Linux 收包要先响 IRQ，再由 NAPI 批量轮询？ [写作时展开]

关键设计: 硬中断只做快速调度，实际批量取包放到软中断/NAPI poll 上：

```[pseudocode]
NIC IRQ/top-half
  → 确认设备事件
  → 暂时抑制/屏蔽对应 RX 中断
  → napi_schedule
  → 标记 NET_RX_SOFTIRQ pending
  → 尽快退出硬中断

NET_RX_SOFTIRQ
  → net_rx_action
  → 调驱动 poll(budget)
  → 批量回收多个 RX descriptor
  → 预算用尽? 继续下一轮
  → ring 处理完? napi_complete, 恢复中断
```

Why: 为什么不在硬中断里直接跑 `tcp_v4_rcv`？——**硬中断上下文不能睡眠，且处理时间越长越容易阻塞其他设备中断**；NAPI 将“通知”和“批量处理”分开，既减少中断风暴，又能把多个包摊平处理。`ksoftirqd` 可能在软中断处理压力很高时参与执行，但不能简单说“所有 NAPI 都在 ksoftirqd 中运行”。 [内核: `__napi_schedule`、`net_rx_action`、驱动 poll 构成 NAPI 的核心调度链]

比喻锚点: IRQ 像码头电话铃，只负责喊“有货到了”；NAPI 是仓库批处理班组，接到通知后一次拉一车货，而不是每来一箱就重新启动整套流程。 [写作时展开]

### 3. `netif_receive_skb` 与 GRO — 协议栈前先做批量合并

场景提示: 高速网卡每秒收到大量小 TCP 段，如果每个段都独立跑一遍协议栈，CPU 会不会耗在重复处理头部上？ [写作时展开]

关键设计: 驱动交给网络核心后，GRO 尝试把属于同一流、序列连续且可合并的包合成较大的 skb，再送入上层：

```[pseudocode]
NAPI poll
  → napi_gro_receive / napi_gro_frags
  → dev_gro_receive
      找到可合并的 GRO flow?
      → 合并 payload/更新长度与校验信息
      → 后续小 skb 被吸收
      不可合并?
      → 保持独立 skb
  → GRO flush
  → netif_receive_skb / __netif_receive_skb_core
```

Why: 为什么 GRO 能提速，却不能把它理解成“TCP 已经收到一个大包”？——**GRO 只是接收路径的批处理优化，协议语义仍然是原来的 TCP 段/序列空间**：它减少重复的协议栈开销，之后仍会按正确的 checksum、Seq/Ack 和 socket 语义处理。LRO 是硬件/驱动侧的另一类聚合，不应和 GRO 混为一谈。 [内核: `napi_gro_receive` 在 NAPI 上下文尝试合并，`netif_receive_skb` 之后才进入更完整的协议栈处理]

比喻锚点: GRO 像把同一趟车送来的十个小包裹先装进一个大周转箱，仓库只处理一次外箱流程，但里面每个包裹的编号仍然保留。 [写作时展开]

### 4. `ip_rcv` → Netfilter → 路由 — 本机交付还是继续转发

场景提示: Ethernet 帧已经进入内核，内核怎样判断它是给本机 socket 的，还是要转发到另一块网卡？ [写作时展开]

关键设计: IPv4 接收路径先做 IP 层检查和 Netfilter PREROUTING，再依据 FIB/路由决定本地交付或转发：

```[pseudocode]
netif_receive_skb
  → ip_rcv
      检查 IPv4 版本/头长度/校验和等
  → NF_INET_PRE_ROUTING
      可能做 conntrack / DNAT / 丢弃
  → 路由查找(FIB)
      目标是本机 → ip_local_deliver
      目标不是本机且允许转发 → ip_forward
  → 本机路径: NF_INET_LOCAL_IN
  → ip_local_deliver_finish
      按 IP protocol 分发给 TCP/UDP/ICMP
```

Why: 为什么 Netfilter PREROUTING 要在最终本地/转发决定之前？——**因为 DNAT 可能改变目的地址，conntrack 也需要在早期建立状态**；之后路由查找才能基于处理后的目标做决定。IP 分片还可能先进入分片重组路径，完整报文才适合继续交付。 [内核: `ip_rcv`、`ip_local_deliver`、`ip_forward` 是本机交付和转发分叉的关键节点]

比喻锚点: IP 层像总分拣中心：先检查包裹标签并经过安检（Netfilter），再查路线表决定送本楼收件台，还是转发到下一座城市。 [写作时展开]

### 5. `tcp_v4_rcv` → socket queue → `recv()`/`epoll` 被唤醒

场景提示: TCP 数据已抵达本机，阻塞在 `recv()` 和 `epoll_wait()` 的用户线程分别是怎样得到通知的？ [写作时展开]

关键设计: TCP 先按四元组找到 socket，再把可交付数据放进接收队列，最后触发 socket readiness 回调：

```[pseudocode]
ip_local_deliver_finish
  → tcp_v4_rcv
  → __inet_lookup_skb(src/dst IP + port)
      established hash / listening hash
  → tcp_v4_do_rcv
      → tcp_rcv_established
      → tcp_data_queue
          按 Seq 放入 sk_receive_queue / out-of-order queue
  → sk_data_ready
      → sock_def_readable
      → wake_up_interruptible(sk_sleep(sk))
      → ep_poll_callback(若被 epoll 监听)
      → recv/read 或 epoll_wait 返回
```

Why: 为什么收到包不等于 `recv()` 立刻拿到完整应用消息？——**TCP 只提供字节流，内核可能先把乱序段放在 out-of-order queue，应用还要按当前可读字节数读取**；`epoll` 通知的是“现在有某种 readiness”，不是“你的业务消息已经完整”。这正是 04 篇粘包排障的内核底层原因。 [内核: `tcp_data_queue` 负责把 TCP 段纳入接收队列，`sk_data_ready` 把 socket 状态传播给等待者]

比喻锚点: socket receive queue 像收货仓，TCP 先按包裹编号整理入库；`recv()` 是人工取货，`epoll` 是仓库在“有货可取”时按铃。 [写作时展开]

### 6. 收束

一个收包从硬件到用户态的完整链路：

```[pseudocode]
NIC DMA / RX descriptor
  → IRQ + napi_schedule
  → NET_RX_SOFTIRQ / driver poll
  → GRO / netif_receive_skb
  → ip_rcv / Netfilter / FIB
  → ip_local_deliver / tcp_v4_rcv
  → tcp_data_queue / sk_receive_queue
  → sk_data_ready / epoll callback
  → recv() copy_to_user
```

关键瓶颈：
- RX ring 或 buffer 回收跟不上 → 丢包
- NAPI budget/softirq 压力高 → CPU 软中断占用
- GRO 合并率低、小包极多 → 协议栈每包开销高
- socket receive queue 或应用消费慢 → rwnd 下降、上游被流控

**Aha Moment**: "DMA 只负责把包放进内存，NAPI 只负责把包批量送进协议栈；真正让用户态可读的是**TCP 按四元组找到 socket、把数据挂进接收队列，再触发 `sk_data_ready`/epoll 回调**。"
**回答读者三问**: ①网卡收到包先做什么=DMA 写 RX buffer；②为什么要 NAPI=把中断通知与批量处理分开；③`epoll` 为什么返回=socket 接收队列出现满足 readiness 的数据。

---

### 核心悬念

**"收包是 DMA → 协议栈 → socket，那么 `send()` 调用后的数据又如何从用户态经过 TCP/IP、qdisc、驱动 TX ring，最终变成网卡发出的电/光信号？"**

→ 引出 06-kernel-send-path — Linux 内核发包全链路——下一篇沿相反方向走完整条数据面。