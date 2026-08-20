# 百万连接容量规划 — TCP 内存、队列、fd 与多核分发如何一起算

> Cluster C: 6 KPs | 依赖: 07-io-models、08-epoll-reactor | 读者基线: epoll/Reactor、TCP 状态与内核收发路径
> 读者处境: 08 篇已经解决“怎样高效等待大量 fd”；本篇进一步问：100 万连接真的只需要 100 万个 epoll item 吗？内核内存、backlog、fd、CPU 和应用 buffer 哪个先成为瓶颈？
> 打开新视角: 百万并发不是一个 `ulimit` 参数，而是**连接状态内存 + 排队内存 + fd/epoll 对象 + accept 队列 + CPU/网卡分发 + 应用状态**的总账

---

### 概念依赖链

```
07 I/O模型 + 08 epoll/Reactor → 本篇: 大规模连接容量规划
  ├─ §1 单连接内存账本(协议对象/缓冲区/应用状态)
  ├─ §2 SYN/accept 队列(建连阶段资源)
  ├─ §3 溢出观测(ss/nstat)
  ├─ §4 fd/epoll/SO_REUSEPORT/RSS(横向扩展)
  └─ §5 多线程共享 socket 的并发边界
先讲: 一条连接多少钱 → 建连队列 → 如何观测 → 如何扩展 → 并发安全
后续依赖: 10-netfilter-nat(容器网络/NAT 进一步增加状态)
```

### 叙事顺序

1. 问题引入——“百万连接”到底是 1 百万个整数 fd，还是 1 百万个带内核/用户态状态的对象？（**Aha: 并发上限是多本账的最小值，不是某个神奇内核参数**）
2. 单连接内存——TCP 控制块、socket、fd、epoll item、收发 buffer
3. 半连接/全连接队列——listen 背后的建连压力
4. 观测溢出——`ss`、`nstat`、系统 fd 统计
5. fd 限制与多核分发——SO_REUSEPORT、RSS、CPU affinity
6. socket 并发安全——内核安全不等于应用消息安全
7. 收束——容量公式与瓶颈定位

### 1. 单条 TCP 连接的内存账本 — 不要只乘一个“每连接字节数”

场景提示: 100 万条 ESTABLISHED 连接都空闲时，除了 fd 还会占哪些内核和用户态资源？ [写作时展开]

关键设计: 连接成本至少拆成控制对象、fd/epoll、缓冲区和应用状态：

```[pseudocode]
每连接可能包含:
  struct sock / inet_sock / tcp_sock 控制状态
  file 与 fd 表项
  epoll 注册对象/用户 data
  TCP 发送/接收 buffer 的记账与实际页
  排队的 skb、定时器、路由/邻居引用
  应用自己的连接对象、协议解析 buffer、业务状态

总内存 ≈
  N × (固定控制开销 + fd/epoll开销)
  + 实际配置/占用的收发 buffer
  + 排队 skb
  + N × 应用状态
```

Why: 为什么不能把“一条 TCP 连接约 3.5KB”当成通用答案？——**内核结构体大小、内核版本、架构、slab 对齐、空闲/有数据排队、自动调节 buffer 和应用状态都会改变结果**：空闲连接与满接收缓冲连接不是同一种成本。容量规划必须用目标内核和 workload 实测 `/proc/meminfo`、slab、cgroup memory 与 socket 统计，而不是照抄一个常数。 [内核: `tcp_sock`/`sock` 只是一部分成本，`sk_rmem_alloc/sk_wmem_alloc` 反映实际排队压力]

比喻锚点: 连接像长期入住酒店的客人，身份证、房间钥匙、前台登记、行李和服务订单都要占资源；只按身份证大小算成本一定会漏账。 [写作时展开]

### 2. SYN 队列与 accept 队列 — listen 背后的两个阶段

场景提示: 服务端收到大量 SYN，但应用还没来得及 `accept()`；连接此时已经“建立”了吗？ [写作时展开]

关键设计: TCP 建连至少经历未完成握手与已完成握手等待应用取出的不同阶段：

```[pseudocode]
SYN 阶段:
  收 SYN → 回 SYN+ACK → 等第三次 ACK
  → request_sock / SYN backlog 相关状态

完成握手:
  收最终 ACK → 连接进入 established
  → 放入监听 socket 的完成连接队列

accept():
  从完成队列取出已建立 socket
  不是三次握手本身的完成动作
```

Why: 为什么“握手完成”和“应用拿到连接”是两个时间点？——**内核可以先完成协议握手，再等待用户态 accept 消费连接**；因此 backlog 溢出可能发生在不同阶段，SYN cookies 也只针对特定的半连接资源压力，不能当作 accept 队列的万能修复。`listen(backlog)` 的实际效果还受内核、协议栈和 `somaxconn` 等配置共同影响。 [内核: `tcp_v4_conn_request`、`tcp_v4_syn_recv_sock` 与监听队列代码共同完成建连分阶段]

比喻锚点: SYN 队列像预约登记，accept 队列像已经办好入住但还没被前台交给服务员的房卡；两种队列满，含义不同。 [写作时展开]

### 3. 队列溢出诊断 — 先证明是哪条队列在丢

场景提示: 客户端说偶尔连不上，服务端日志却没有报错；怎样确认是 backlog/accept 处理不及时？ [写作时展开]

关键设计: 把 socket 状态、TCP 扩展统计、抓包和应用 accept 延迟放在同一条时间线上：

```[pseudocode]
ss -s / ss -tan
  → 看 ESTAB / SYN-RECV / TIME-WAIT / orphan 等状态规模

nstat -az
  → 关注 ListenOverflows / ListenDrops 等 TcpExt 计数

/proc/net/snmp / /proc/net/netstat
  → 查看 TCP/IP 累计统计

tcpdump
  → 对照 SYN、SYN+ACK、ACK、重传与 RST 时间线

排查顺序:
  1) 应用 accept 是否及时
  2) 完成队列/半连接队列是否增长或溢出
  3) 是否启用 SYN cookies/防火墙限速
  4) 客户端重传还是服务端主动 RST
```

Why: 为什么不能看到 `ListenDrops` 增加就直接断定是某一个队列满？——**内核统计命名和计数路径需要结合版本与代码解释，单个计数器不是完整因果链**；必须结合抓包、`ss`、应用 accept 延迟、CPU/软中断和 backlog 配置。`tcp_abort_on_overflow` 也不是“修复溢出”，只是改变部分溢出情形下的对端反馈行为。 [内核: `/proc/net/netstat` 中 TcpExt 统计是观测入口，不等于完整诊断结论]

比喻锚点: 机场航班延误要同时看值机柜台、安检队列、登机口和跑道；只看一个计数器不能知道乘客卡在哪。 [写作时展开]

### 4. fd 限制、SO_REUSEPORT 与 RSS — 把连接分给多个执行单元

场景提示: 单进程 epoll 已经能等待很多 fd，为什么百万连接仍可能卡在 fd 上限、单核 accept 或网卡队列？ [写作时展开]

关键设计: 横向扩展要同时处理进程资源上限、监听分发和网卡多队列：

```[pseudocode]
fd 资源:
  RLIMIT_NOFILE(进程软/硬限制)
  → /proc/sys/fs/nr_open(单进程可设上限)
  → /proc/sys/fs/file-max(系统级文件句柄约束)

SO_REUSEPORT:
  多 socket 绑定相同地址/端口
  → 内核按连接 hash/策略选择监听 socket
  → 每个 worker 可拥有独立 epoll/fd 集合

RSS / RPS / CPU affinity:
  网卡/内核把流分布到多个 RX queue/CPU
  → worker 与中断/队列尽量保持局部性
```

Why: 为什么 SO_REUSEPORT 不是简单“绕过 fd 限制”？——**它主要解决监听分发和多执行单元扩展，不能消除每个进程、系统和 cgroup 的 fd/内存上限**；同一连接通常需要稳定映射到同一 socket，实际选择还受 hash、eBPF reuseport 程序和内核策略影响。CPU 绑核也不是越死越好，NUMA、IRQ、RSS、业务负载都要一起验证。 [内核: `reuseport_select_sock` 选择监听 socket；网卡 RSS 的具体能力由驱动/设备决定]

比喻锚点: SO_REUSEPORT 像多个服务窗口共享同一个门牌，分流器把新客人分到不同窗口；它增加吞吐，但不会让每个窗口的内存和文件夹容量消失。 [写作时展开]

### 5. socket 并发安全 — 内核不乱，不代表消息不会交错

场景提示: 多线程同时对一个 TCP fd `send()`/`recv()`，会不会把内核状态写坏？即使不会，业务消息又会不会被混在一起？ [写作时展开]

关键设计: 内核通过 socket 锁和队列保护内部状态，但 TCP 字节流与应用消息边界仍由应用负责：

```[pseudocode]
多个线程共享 fd
  → 内核 socket/协议状态由锁与引用计数保护
  → 并发 send/recv 通常不会破坏内核数据结构
  → 但 TCP 仍是字节流:
      多次 send 可能合并
      一次 send 可能被拆分
      多线程业务消息需要应用层串行化/帧协议

UDP:
  datagram 边界与 TCP 不同
  并发语义仍需考虑发送顺序、共享 buffer 和应用竞态
```

Why: 为什么“一核一线程一 epoll”只是常见设计，而不是 TCP 的硬性要求？——**共享 socket 在内核层可以被保护，但用户态 handler、连接状态、写缓冲和业务顺序仍可能产生锁竞争与竞态**：架构要在吞吐、局部性、业务串行化和复杂度间权衡。 [内核: `lock_sock` 等保护协议状态；它不替应用定义消息边界]

比喻锚点: 内核像有锁的仓库，多个工人不会把货架撞坏；但如果业务订单没有编号和顺序规则，发出去的货仍可能按不同批次交错。 [写作时展开]

### 6. 收束

百万连接容量公式不是一个固定常数，而是：

```[pseudocode]
总成本 ≈
  连接数 × (TCP/socket/fd/epoll固定开销)
  + 实际收发 buffer 与 skb
  + 连接队列与定时器
  + 应用连接对象/协议 buffer
  + 事件循环、线程、网卡队列、NUMA 迁移成本

可用连接数 =
  min(fd 上限, 内核内存, cgroup 内存, CPU, NIC/RX-TX能力, backlog/accept能力)
```

**Aha Moment**: "百万并发不是把 `ulimit -n` 调到 100 万就完成了，而是要让**fd、epoll、TCP 控制块、缓冲区、队列、CPU、网卡和应用状态**同时不越界；真正的容量上限永远是这些账本中的最小值。"
**回答读者三问**: ①一条连接到底多贵=取决于控制对象、实际 buffer、排队 skb 和应用状态；②backlog 满和 fd 满是一回事吗=不是，分别属于建连队列与进程资源；③SO_REUSEPORT/RSS 解决什么=把连接和包处理分散到多个执行单元，不是无限扩容。

---

### 核心悬念

**"百万连接只是本机容量问题；容器里的网络如何穿过 namespace、veth、bridge、Netfilter 和 NAT，Kubernetes Service 又怎样把一个虚拟地址映射到后端 Pod？"**

→ 引出 10-netfilter-nat — Netfilter、iptables 与容器网络——从连接资源账本进入虚拟网络路径。