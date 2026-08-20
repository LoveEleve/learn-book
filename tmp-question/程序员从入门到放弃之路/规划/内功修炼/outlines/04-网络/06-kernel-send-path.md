# Linux 内核发包全链路 — 从 `send()` 到 TCP/IP、qdisc、TX ring 与网卡

> Cluster C: 12 KPs | 依赖: 05-kernel-recv-path | 读者基线: 内核收包链、sk_buff、TCP 窗口
> 读者处境: 05 篇沿收包方向走完了 DMA → socket；本篇反向追踪：用户态 `send()` 的字节如何经过 TCP 排队、路由/ARP、qdisc 和驱动 TX queue，最终变成网线上的信号
> 打开新视角: 发包不是一次 copy，而是**用户数据进入发送队列 → TCP 按窗口放行 → IP/邻居决定下一跳 → qdisc 排队 → 驱动/硬件卸载与 DMA**

---

### 概念依赖链

```
05-kernel-recv-path(DMA/NAPI/sk_buff) → 本篇: 内核发包路径
  ├─ §1 tcp_sendmsg(用户缓冲区 → TCP 发送队列)
  ├─ §2 tcp_write_xmit/ip_queue_xmit(TCP 窗口 → IP 路由)
  ├─ §3 邻居子系统(下一跳 IP → MAC)
  ├─ §4 dev_queue_xmit/qdisc/TX ring(排队 → 驱动)
  ├─ §5 TSO/GSO(大 skb → 线速小段)
  └─ §6 sendfile/splice(减少用户态拷贝)
先讲: 用户数据 → TCP → IP/ARP → qdisc/驱动 → 卸载/零拷贝
后续依赖: 07-io-models(用户态如何高效管理大量 socket)
```

### 叙事顺序

1. 问题引入——`send(fd, buf, len)` 返回后，数据为什么还可能在内核队列里，尚未到网卡？（**Aha: send 成功通常意味着数据进入发送路径并被接管，不等于已经在线路上传输完成**）
2. `tcp_sendmsg`——用户缓冲区进入 TCP 发送队列
3. `tcp_write_xmit → ip_queue_xmit`——拥塞窗口放行并完成 IP 路由
4. 邻居子系统——ARP 把下一跳 IP 变成 MAC
5. `dev_queue_xmit → qdisc → ndo_start_xmit`——进入设备发送队列
6. TSO/GSO 与零拷贝——减少 CPU 分段和内存复制
7. 收束——用户态到网卡的完整流水线

### 1. `tcp_sendmsg` — 用户数据进入 TCP 发送队列

场景提示: 用户调用 `send()` 时，内核是直接把数据交给网卡，还是先在 socket 里排队？发送缓冲区满了又会怎样？ [写作时展开]

关键设计: TCP 先把用户数据纳入 socket 发送缓冲区，按写入策略组织 skb，之后由发送路径决定何时真正发出（net/ipv4/tcp.c + net/ipv4/tcp_output.c）：

```[pseudocode]
send/write/writev
  → tcp_sendmsg / tcp_sendmsg_locked
  → 从用户 iov 读取数据
  → 写入 TCP write queue / sk_wmem_alloc 记账
  → 根据 MSS、push、TCP_NODELAY、TCP_CORK 等决定组织方式
  → tcp_push / tcp_write_xmit 尝试发送

发送缓冲区不足:
  阻塞 socket → 等待可用空间
  非阻塞 socket → 返回 EAGAIN/EWOULDBLOCK
```

Why: 为什么 `send()` 返回成功不等于对端收到？——**系统调用只需要把数据交给本机 TCP 栈**：后面还有拥塞窗口、路由、ARP、qdisc、网卡队列和对端 ACK。发送缓冲区把用户线程与网络时钟解耦，让应用可以先写，TCP 再按窗口和重传机制慢慢发。 [内核: 05 篇的 `sk_receive_queue` 是收包终点；这里对应 `sk_write_queue` 是发包起点]

比喻锚点: `send()` 像把包裹交给快递站，不是交给收件人；快递站先把包裹放进待发货架，再根据车辆容量和道路情况安排出发。 [写作时展开]

### 2. `tcp_write_xmit` → `ip_queue_xmit` — 窗口放行后进入 IP 层

场景提示: 发送队列里已经有很多数据，TCP 为什么不一次性全发？谁决定这一刻最多允许哪些 skb 出队？ [写作时展开]

关键设计: TCP 用 `min(snd_wnd, cwnd)` 约束 inflight 数据，放行的 skb 再进入 IP 路由与 Netfilter：

```[pseudocode]
tcp_write_xmit
  → 检查 snd_wnd / cwnd / packets_in_flight
  → 允许发送的 skb:
      tcp_transmit_skb
        构造 TCP header/options/checksum
        → ip_queue_xmit
            路由查找: 目的 IP → rtable/出口设备/下一跳
            → LOCAL_OUT Netfilter
            → ip_output / ip_finish_output
```

Why: 为什么 TCP 发送队列和 qdisc 还要分两层？——**TCP 关心端到端可靠性和拥塞窗口，qdisc 关心本机设备出口的排队与调度**：TCP 不能替代设备队列，qdisc 也不知道 ACK、重传和字节序列。两层都可能积压数据，排障时要区分 socket send queue、TCP retransmission queue 和设备 qdisc backlog。 [内核: `tcp_write_xmit` 决定哪些数据可以出 TCP，`ip_queue_xmit` 才把它交给三层路由]

比喻锚点: TCP 是总调度员，确认远端道路还能承受多少车；qdisc 是本地收费站，决定几辆车先进入这条出口车道。 [写作时展开]

### 3. 邻居子系统 — 下一跳 IP 到 MAC 的最后一座桥

场景提示: 路由已经决定从 eth0 发给网关 `192.168.1.1`，但 Ethernet 帧还缺目的 MAC；ARP 缓存过期时，发送路径会怎样暂停？ [写作时展开]

关键设计: 邻居表维护 IP→链路层地址映射及其状态，未解析完成的 skb 会进入邻居等待机制（net/core/neighbour.c + net/ipv4/arp.c）：

```[pseudocode]
ip_finish_output
  → neigh_resolve_output
      NUD_REACHABLE/VALID:
        取得 MAC → dev_queue_xmit(skb)
      NUD_INCOMPLETE:
        __neigh_event_send
        → arp_solicit 广播 ARP Request
        → 暂存待发 skb
        → 收到 ARP Reply
        → 更新邻居状态
        → 重新发送等待队列
```

Why: 为什么 ARP 解析不由 TCP 自己完成？——**下一跳寻址是所有 IP 负载共享的链路问题**：TCP、UDP、ICMP 都要用，邻居子系统统一处理；IPv6 则使用 Neighbor Discovery。首包延迟常来自邻居解析，但“ARP 未命中”通常意味着排队等待，不等于 skb 立即丢弃；队列、重试和超时才决定最终结果。 [内核: 01 篇介绍的 ARP 请求在发包方向落到 `neigh_resolve_output`/`arp_solicit`]

比喻锚点: 路由给了城市和街道地址，ARP 像到小区门口查具体门牌；门牌没查到前，快递先放在待派送架上。 [写作时展开]

### 4. `dev_queue_xmit` → qdisc → `ndo_start_xmit` — 从协议栈进入设备

场景提示: TCP/IP 已经准备好完整 skb，为什么还不能直接调用网卡？网卡前面的 qdisc 到底解决什么问题？ [写作时展开]

关键设计: 设备发送路径先经过 qdisc 调度，再由驱动把 skb 放入 TX descriptor：

```[pseudocode]
dev_queue_xmit(skb)
  → __dev_queue_xmit
      有 qdisc?
        入队 → qdisc dequeue 按调度策略取出
        直接发送 → 进入驱动
  → dev_hard_start_xmit
  → ndo_start_xmit
      驱动填充 TX descriptor
      → NIC DMA 读取 skb 数据
      → PHY/MAC 发出电/光信号
  → TX completion
      → 驱动回收 descriptor/skb
      → BQL 等机制限制设备前排队量
```

Why: 为什么需要 qdisc，不能让 TCP 谁准备好谁直接发？——**多 socket、多流、多优先级会同时争用一个设备出口**：qdisc 可以做 FIFO、按流公平、优先级和整形；它也是 bufferbloat、队列延迟与丢包排查的重要位置。具体默认 qdisc 和网卡 offload 由内核版本、发行版和设备配置决定，不能把某一个 qdisc 写成所有系统默认。 [内核: `__dev_queue_xmit` 连接网络核心与设备队列，驱动 `ndo_start_xmit` 负责硬件提交]

比喻锚点: qdisc 像机场登机口：飞机已经装好货（skb），但仍要按航班、优先级和跑道容量排队，最后才交给地勤送上跑道。 [写作时展开]

### 5. TSO/GSO — 让“大 skb”与“线上小段”分工

场景提示: TCP 要按 MSS 发送很多小段，为什么 CPU 不一定真的为每个小段都构造一次完整 skb？ [写作时展开]

关键设计: TSO/GSO 允许内核上层暂时保留较大的 skb，在更靠近设备的位置完成分段：

```[pseudocode]
上层:
  tcp_write_xmit 构造带 GSO/MSS 元信息的大 skb

TSO 硬件支持:
  → 驱动把大 skb + 分段信息交给 NIC
  → NIC 按 MSS 切成线上 TCP segments

GSO 软件路径:
  → validate_xmit_skb / dev_gso_segment
  → 内核在发送设备前拆成多个小 skb
  → 再交给驱动

语义:
  线上仍必须满足 MSS/MTU
  大 skb 只是 CPU/设备之间的优化表示
```

Why: 为什么 TSO/GSO 不改变 TCP 的 MSS 语义？——**它们只改变分段发生的位置**：TCP 仍然按 MSS/拥塞窗口计算发送语义，硬件或软件只是延后真正拆包。TSO 依赖硬件能力，GSO 是内核的通用软件回退；实际路径可由网卡 offload、隧道和校验和能力共同决定。 [内核: `validate_xmit_skb`/`dev_gso_segment` 在设备发送前处理卸载兼容性]

比喻锚点: TSO/GSO 像物流中心先把多个小箱子装进一个大周转箱运输，到达分拣口前再按目的地拆开；线上公路最终仍只允许规定尺寸的货车。 [写作时展开]

### 6. sendfile / splice — 减少用户态往返，不等于完全零拷贝

场景提示: 静态文件服务器把磁盘文件发到 socket，为什么 `sendfile()` 往往比 `read()+write()` 少一次用户态拷贝？ [写作时展开]

关键设计: 零拷贝接口尽量让页缓存/内核 buffer 直接参与 socket 发送，但具体是否真正绕过复制取决于文件系统、协议、网卡和 offload：

```[pseudocode]
传统:
  disk → Page Cache → user buffer → socket/TCP → NIC

sendfile:
  file fd → Page Cache / sendpage-like path → socket/TCP → NIC
  不把文件内容复制到用户 buffer

splice:
  fd ↔ pipe buffer ↔ fd
  传递内核 buffer 引用/描述, 具体路径由双方支持情况决定
```

Why: 为什么不能简单宣传“sendfile=零拷贝、read+write=固定四次拷贝”？——**拷贝次数受页缓存命中、文件系统 a_ops、socket 路径、TLS、网卡 DMA 和 fallback 影响**：sendfile 的核心收益是减少用户态往返和一次显式复制，不代表整个系统完全没有内存搬运。 [内核: `sendfile`/`splice` 把文件页与 socket/pipe 的连接交给内核，但具体实现路径需要结合协议与设备能力]

比喻锚点: 传统 read+write 是把货搬到用户仓库再搬回快递站；sendfile 是让两个仓库直接交接货物，少一次中转，但仍要经过分拣和装车。 [写作时展开]

### 7. 收束

完整发包链路：

```[pseudocode]
send()
  → tcp_sendmsg / TCP write queue
  → tcp_write_xmit: snd_wnd/cwnd 放行
  → tcp_transmit_skb / ip_queue_xmit
  → route / Netfilter / neighbor(ARP)
  → dev_queue_xmit / qdisc
  → ndo_start_xmit / TX descriptor / NIC DMA
  → TSO/GSO 在合适位置完成分段
```

关键性能位置：
- socket send buffer 满：应用阻塞或 EAGAIN
- cwnd/rwnd 小：TCP 本身暂不放行
- ARP 未解析：首包等待邻居状态
- qdisc 排队长：本机出口 bufferbloat
- TSO/GSO 不可用：CPU 分段开销上升
- sendfile/splice 可用：减少用户态复制

**Aha Moment**: "`send()` 不是把数据直接交给网卡，而是把它送进一条多级流水线：**TCP 决定现在能发多少，IP 决定往哪走，邻居子系统决定下一跳 MAC，qdisc 决定何时进设备，TSO/GSO 决定在哪一层拆成线上小段**。"
**回答读者三问**: ①send 成功为什么不等于送达=只代表本机 TCP 接管并排队；②ARP 未命中会怎样=邻居子系统暂存并解析；③TSO/GSO 解决什么=把分段工作从 CPU 逐包处理延后到硬件/软件合适位置。

---

### 核心悬念

**"理解内核收发路径后，用户态怎样同时管理成千上万个连接？`select` 为什么会扫描 fd 集合，`epoll` 又怎样把就绪事件变成 O(1) 取出？"**

→ 引出 07-io-models — I/O 模型与多路复用——从数据面进入用户态并发模型。