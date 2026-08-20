# TCP 状态机与握手挥手 — 一个字节流连接如何出生、运行与死亡

> Cluster B: 13 KPs | 依赖: 01-tcpip-model-arp | 读者基线: TCP/IP 四层模型、ARP 与下一跳解析
> 读者处境: 01 篇最后留下了一个问题：ARP 解析完成后，SYN 到底怎样把两个端点变成一条连接？本篇把 TCP 的包格式、状态机、握手、挥手和异常路径串成一张图
> 打开新视角: TCP 不是“发几个包就连上”，而是**序列号 + 状态迁移 + 半连接/全连接对象**共同组成的状态协议

---

### 概念依赖链

```
01-tcpip-model-arp(分层/IP/MAC/ARP) → 本篇: TCP 包格式与状态机
  ├─ §1 TCP 头/序列号/窗口(协议状态载体)
  ├─ §2 11 个主要状态(收到包+应用调用驱动迁移)
  ├─ §3 三次握手(request_sock → established sock)
  ├─ §4 四次挥手(全双工半关闭 + TIME_WAIT)
  └─ §5 同时打开/RST(异常状态)
先讲: 包字段 → 状态图 → 建连 → 关闭 → 异常
后续依赖: 03-tcp-flow-congestion(握手后序列号/窗口如何持续发送)
```

### 叙事顺序

1. 问题引入——`connect()` 发出 SYN 后，为什么必须等 SYN+ACK 和 ACK？一个 TCP 连接到底保存了哪些状态？（**Aha: TCP 连接是双方对序列空间和状态机达成的共同承诺**）
2. TCP 头——Seq/Ack/Window/Flags 是状态机的输入输出
3. 11 个状态——应用调用和收到的 TCP 包共同推进状态
4. 三次握手——SYN_SENT → SYN_RCVD → ESTABLISHED
5. 四次挥手——每个方向独立关闭，TIME_WAIT 负责收尾
6. 同时打开/RST——异常路径如何落回状态机
7. 收束——从包字段到连接生命周期

### 1. TCP 头 — 序列号与标志位承载状态

场景提示: 抓到一个 TCP 包，除了源/目的端口，内核如何知道它是新连接、确认数据，还是正在关闭？ [写作时展开]

关键设计: TCP 固定头至少包含端口、序列号、确认号、数据偏移、窗口、校验和、紧急指针以及控制标志；选项追加在固定头之后（include/uapi/linux/tcp.h）：

```[pseudocode]
TCP header
  source/dest port
  seq: 本段数据第一个字节的序号
  ack_seq: 期望对端下一个发送的字节序号
  data_offset: TCP 头长度
  flags: SYN / ACK / FIN / RST / PSH / URG 等
  window: 接收方当前愿意接收的窗口
  checksum: 伪首部 + TCP 头 + 数据
  urgent pointer: 仅 URG 语义下使用
  options: MSS / SACK / Timestamp / Window Scale 等
```

Why: 为什么 TCP 要给每个字节编号，而不是给每个包编号？——**包会分片、重传、乱序，字节流才是应用真正看到的抽象**：Seq/Ack 让接收方能确认“我已经连续收到哪里”，也能识别重复段；Window 则把接收缓冲区容量反馈给发送方。**MSS 不是固定永远 1460**，它通常由路径 MTU 和双方协商决定。 [内核: TCP 头字段最终由 `tcphdr` 与发送/接收路径解释；01 篇的 `sk_buff` 承载它穿过网络栈]

比喻锚点: TCP Seq/Ack 像快递箱上的连续页码——收件人不是确认“第几个箱子”，而是确认“我已经连续读到第几页，下一页请从哪里开始”。 [写作时展开]

### 2. TCP 状态机 — 11 个主要状态不是一条直线

场景提示: `ss -tan` 里为什么会同时看到 `LISTEN`、`SYN-SENT`、`ESTAB`、`TIME-WAIT`？这些不是异常，而是连接生命周期的不同阶段。 [写作时展开]

关键设计: 状态由两类事件共同推进：应用调用（listen/connect/close/shutdown）和收到的 TCP 段（SYN/ACK/FIN/RST）：

```[pseudocode]
CLOSED → LISTEN → SYN-RECV → ESTABLISHED
CLOSED → SYN-SENT → ESTABLISHED
ESTABLISHED → FIN-WAIT-1 → FIN-WAIT-2 → TIME-WAIT → CLOSED
ESTABLISHED → CLOSE-WAIT → LAST-ACK → CLOSED

其他主要状态:
  CLOSING: 双方几乎同时主动关闭
  SYN-SENT / SYN-RECV: 握手尚未完成
```

Why: 为什么需要这么多状态，不能只有“连接/未连接”两态？——**因为 TCP 是全双工协议，连接建立、数据传输、半关闭、同时关闭、旧包清理都需要不同规则**：`CLOSE_WAIT` 表示对端已经不再发送，但本地应用还没 close；`FIN_WAIT_2` 表示本地已关发送方向，但仍可接收；把它们压成一个状态会丢掉关键语义。 [man 8 ss: `ss -tan` 显示内核维护的 TCP 状态；[内核: `tcp_set_state` 是状态迁移的集中入口]

比喻锚点: 状态机像一场双向电话——接通、对方先挂、本方先挂、双方同时挂、线路上旧回声未消失，都不是同一个“挂断”状态。 [写作时展开]

### 3. 三次握手 — 同步双方序列空间并建立半连接/全连接

场景提示: 第一个 SYN 到达服务端后，为什么服务端不能立即把它当成一个普通 established socket？ [写作时展开]

关键设计: 三次握手同时完成能力协商、双方 ISN 确认和服务端连接对象转换（net/ipv4/tcp_ipv4.c + net/ipv4/tcp_input.c）：

```[pseudocode]
1. Client → Server: SYN, Seq=X
   client: CLOSED → SYN-SENT

2. Server → Client: SYN+ACK, Seq=Y, Ack=X+1
   server: LISTEN → SYN-RECV
   保存 request_sock 半连接状态

3. Client → Server: ACK, Ack=Y+1
   client: SYN-SENT → ESTABLISHED
   server: SYN-RECV → ESTABLISHED
   request_sock → established child tcp_sock
```

Why: 为什么是三次而不是两次？——**两次只能让服务端确认“我收到了客户端的初始序列号”，却不能让客户端确认“服务端的初始序列号也被我收到”**；第三个 ACK 把服务端 SYN 的确认送回去，双方才都知道双方都知道。`request_sock` 先承载半连接，能避免每个未完成握手立刻占用完整连接对象。 [内核: `tcp_v4_conn_request` 处理半连接请求，`tcp_child_process` 把完成握手的请求交给全连接 socket]

比喻锚点: 握手像双方交换一次性号码牌：客户端先报“我的起始号是 X”，服务端回“我收到 X，我的起始号是 Y”，客户端再确认“Y 我也收到了”。 [写作时展开]

### 4. 四次挥手 — 全双工意味着两个方向分别关闭

场景提示: 客户端调用 `close()` 后，为什么服务端还可能继续把剩余数据发过来？ [写作时展开]

关键设计: FIN 只关闭一个方向，另一方向可以继续传输，因此正常关闭通常需要两个 FIN 和两个 ACK：

```[pseudocode]
主动关闭方:
  close → FIN-WAIT-1
  收到 ACK → FIN-WAIT-2
  收到对端 FIN → ACK + TIME-WAIT

被动关闭方:
  收到 FIN → ACK + CLOSE-WAIT
  应用 close → FIN + LAST-ACK
  收到 ACK → CLOSED

同时关闭:
  双方 FIN-WAIT-1 → 收到 FIN 后进入 CLOSING
  收到最终 ACK → TIME-WAIT
```

Why: 为什么 TCP 不用一个 FIN 就彻底关掉连接？——**因为发送和接收是两个独立方向**：对端说“我不再发送”，不等于它不能继续接收；`CLOSE_WAIT` 正是给应用处理剩余数据、最终决定关闭本地发送方向的时间。**TIME_WAIT 也不是单纯的延迟**，它让主动关闭方能重发最后 ACK，并避免旧连接的延迟报文污染新连接。 [内核: `tcp_close`/`tcp_shutdown` 分别处理完整关闭与半关闭]

比喻锚点: 双向电话的一方先挂，不代表另一方立刻不能说完最后一句；TIME_WAIT 像挂断后站在门口等一会，确认最后的回执和旧回声不会串到下一通电话。 [写作时展开]

### 5. 同时打开、RST 与握手超时 — 状态机的异常分支

场景提示: 为什么 `connect()` 有时得到 `Connection refused`，有时却长时间超时？这两种现象对应不同的网络事件。 [写作时展开]

关键设计: TCP 把异常也编码成状态迁移，而不是另起一套机制：

```[pseudocode]
同时打开:
  A: SYN-SENT → 收 SYN → SYN-RECV → ACK → ESTABLISHED
  B: 同样路径
  结果: 一条连接, 不是两条

RST:
  目标端口没有监听 → 对 SYN 回 RST
  已建立连接收到不可接受段 → 可能复位
  SO_LINGER=0 close → 可主动发送 RST, 丢弃未发送数据

SYN-RECV 超时:
  SYN+ACK 按重传策略重发
  多次失败后清理半连接
```

Why: 为什么“RST 立即失败”和“无响应超时”是两种完全不同的用户体验？——**RST 是对端/中间设备明确拒绝，超时则是没有得到足够的确认**：前者很快返回 `ECONNREFUSED`，后者要等待重传计时器和路由/防火墙路径判断。`SO_LINGER=0` 也不是“优雅关闭”，而是主动放弃剩余发送并用 RST 表达复位。 [内核: `tcp_v4_send_reset` 构造 RST；半连接清理依赖 SYN-ACK 重传定时器]

比喻锚点: RST 像门卫立刻说“这里没有这个房间”，超时像门铃一直没人回应——结果都没进去，但诊断含义完全不同。 [写作时展开]

### 6. 收束

回到一条 TCP 连接的完整生命周期：

```[pseudocode]
connect
  → SYN-SENT → SYN / SYN+ACK / ACK
  → ESTABLISHED
  → data: Seq/Ack/Window 驱动可靠字节流
  → FIN-WAIT / CLOSE-WAIT 等半关闭状态
  → TIME-WAIT / CLOSED
```

**Aha Moment**: "TCP 不是‘三次握手 + 四次挥手’两组孤立口诀，而是一张由 **Seq/Ack/Flags 驱动的状态机**：握手同步双方序列空间，数据阶段推进确认窗口，挥手阶段分别关闭两个方向，TIME_WAIT 负责隔离旧报文。"
**回答读者三问**: ①三次握手到底确认什么=双方初始序列空间和收发能力；②为什么四次挥手=全双工两方向独立关闭；③RST 和超时差在哪=前者是明确拒绝，后者是没有得到有效确认。

---

### 核心悬念

**"握手完成之后，TCP 如何在一个有限接收窗口里持续发送大量数据？滑动窗口、拥塞窗口和 ACK 到底谁决定下一段能不能发？"**

→ 引出 03-tcp-flow-congestion — 滑动窗口与拥塞控制——从连接生命周期进入已建立连接的数据传输。