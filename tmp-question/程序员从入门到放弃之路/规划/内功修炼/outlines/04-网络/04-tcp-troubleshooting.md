# TCP 生产排障 — TIME_WAIT、半关闭、Nagle、粘包与 SYN 异常如何定位

> Cluster B: 13 KPs | 依赖: 02-tcp-state-machine、03-tcp-flow-congestion | 读者基线: TCP 包、状态机、窗口与拥塞控制
> 读者处境: 02-03 篇讲的是“协议应该怎样工作”；本篇面对线上症状：端口被 TIME_WAIT 占满、CLOSE_WAIT 堆积、延迟突然升高、recv 读到半条消息、SYN Flood 让服务无法接新连接
> 打开新视角: TCP 排障不是背参数，而是**把症状映射回状态机、计时器、字节流语义和内核队列**

---

### 概念依赖链

```
02 状态机 + 03 窗口/拥塞 → 本篇: TCP 生产排障
  ├─ §1 TIME_WAIT/CLOSE_WAIT(连接生命周期与资源)
  ├─ §2 close/shutdown/linger(优雅关闭与半关闭)
  ├─ §3 Nagle/延迟 ACK(小包延迟)
  ├─ §4 TCP 粘包(字节流与消息边界)
  ├─ §5 SYN Cookies/TFO/RST/backlog(建连异常)
  └─ §6 丢包诊断(ss/nstat/tcpdump/RTO)
先讲: 状态资源 → 关闭语义 → 延迟 → 消息边界 → 建连攻击 → 证据链
后续依赖: 05-kernel-recv-path(从网卡到 recv 的内核收包路径)
```

### 叙事顺序

1. 问题引入——线上看到大量 `TIME_WAIT`、`CLOSE_WAIT`，客户端说“TCP 很慢”，服务端说“偶尔收不到完整消息”；这些现象其实属于同一个协议吗？（**Aha: 排障要先识别状态/语义，再谈参数调优**）
2. TIME_WAIT/CLOSE_WAIT——连接死后还为什么留在内核
3. close/shutdown/linger——优雅关闭与 RST
4. Nagle/延迟 ACK——小包为何可能产生额外等待
5. TCP 粘包——应用必须自己定义消息边界
6. SYN Cookies/TFO/RST/backlog——建连异常与攻击面
7. 丢包诊断——从 `ss`、计数器、抓包建立证据链
8. 收束——症状到机制的映射

### 1. TIME_WAIT 与 CLOSE_WAIT — 两种“连接没消失”含义完全不同

场景提示: `ss -tan` 里满屏 `TIME-WAIT` 或 `CLOSE-WAIT`，这两者都表示连接关闭了吗？ [写作时展开]

关键设计: TIME_WAIT 属于主动关闭方的协议收尾，CLOSE_WAIT 属于被动收到 FIN 但应用尚未关闭本地发送方向：

```[pseudocode]
主动关闭方:
  FIN-WAIT-1 → FIN-WAIT-2 → 收到 FIN → TIME-WAIT → CLOSED
  TIME-WAIT 需等待约 2MSL 的协议时间

被动关闭方:
  收到 FIN → CLOSE-WAIT
  应用 close/shutdown 写方向 → LAST-ACK → CLOSED

TIME-WAIT 的目的:
  1) 允许重发最终 ACK
  2) 让旧连接的延迟段离开网络
```

Why: 为什么 TIME_WAIT 不能简单关掉？——**旧四元组的延迟报文可能晚到**，过早复用会让旧包污染新连接；而 CLOSE_WAIT 堆积通常不是内核“没清理”，而是应用收到 FIN 后没有及时 close。Linux 的 TIME_WAIT 持续时间由实现定时器控制，常见默认约 60 秒；“2MSL”是协议语义，不能机械等同于所有系统上的固定秒数。 [内核: `tcp_timewait_sock` 是比完整 `tcp_sock` 更轻的收尾对象] [man 8 ss: 用状态过滤定位 TIME-WAIT/CLOSE-WAIT]

比喻锚点: TIME_WAIT 像快递站保留旧单号一段时间，CLOSE_WAIT 像对方已经退场但本地仓库还没办理出库手续。 [写作时展开]

### 2. close、shutdown 与 SO_LINGER — 优雅关闭不是一个 API 调用

场景提示: 服务端想“告诉客户端我不再发送，但还要继续读完客户端数据”，应该调用 `close()` 还是 `shutdown()`？ [写作时展开]

关键设计: close 处理文件描述符引用，shutdown 精确关闭 socket 的一个方向，SO_LINGER 改变 close 等待/复位行为：

```[pseudocode]
shutdown(fd, SHUT_WR)
  → 关闭本地发送方向
  → 发 FIN
  → 本地仍可 read 对端剩余数据

close(fd)
  → fd 引用计数减少
  → 最后一个引用关闭时才进入 socket close 语义

SO_LINGER { l_onoff=1, l_linger=0 }
  → close 时倾向于丢弃未发送数据并发送 RST
  → 不是优雅关闭, 对端可能看到 ECONNRESET
```

Why: 为什么生产中常说“CLOSE_WAIT 是应用 bug”？——**内核已经把对端 FIN 交给应用语义，应用却迟迟没有关闭本地引用**；常见原因是异常路径漏 close、线程阻塞、连接池归还失败。`SO_LINGER=0` 可以快速复位，但只是把问题变成数据可能丢失，不是资源泄漏的根治方案。 [man 2 shutdown: 半关闭语义；[man 7 socket: SO_LINGER 行为]]

比喻锚点: shutdown(SHUT_WR) 像说“我不再发言，但还听你说完”；close 像离开会议；linger=0 则像直接掀桌断会。 [写作时展开]

### 3. Nagle + 延迟 ACK — 小消息延迟的经典组合

场景提示: RPC 每次只发送几十字节，带宽很空，延迟却偶尔突然多出几十毫秒，可能发生了什么？ [写作时展开]

关键设计: Nagle 试图合并小段，延迟 ACK 试图减少确认包，两者在特定交互模式下会互相等待：

```[pseudocode]
Nagle:
  前一个小段尚未确认
  → 暂缓新的小段, 尝试合并到 MSS 或等 ACK

Delayed ACK:
  收到数据后不一定立即 ACK
  → 等待短暂时间或等待第二个段/可捎带 ACK

可能结果:
  应用小写 → Nagle 等 ACK
  对端 ACK 延迟 → 双方出现额外等待

常见控制:
  TCP_NODELAY: 禁用 Nagle
  TCP_QUICKACK: 请求更积极 ACK(具体持续行为由内核决定)
```

Why: 为什么不能看到延迟就永久打开 TCP_NODELAY？——**Nagle 减少小包和协议开销，TCP_NODELAY 则用更多包换更低交互延迟**：短请求/响应 RPC 往往重视尾延迟，批量吞吐则可能更在意包效率。经典“约 200ms”是历史实现与场景相关的经验现象，不是 TCP 的固定常量；Linux 延迟 ACK 计时、应用写入模式和网络路径都会影响结果。 [内核: `tcp_nagle_check` 与 `tcp_send_delayed_ack` 分属发送合并和接收确认两侧]

比喻锚点: Nagle 像等购物车装满再发货，延迟 ACK 像收货方等第二件包裹一起签收；双方都节省操作，却可能让第一件货在门口多等一会。 [写作时展开]

### 4. TCP 粘包 — 字节流没有消息边界

场景提示: 发送端两次 `send()`，接收端一次 `recv()` 却读到两次内容拼在一起；或者一次发送被拆成多次 `recv()`，这是 TCP 出错了吗？ [写作时展开]

关键设计: TCP 只保证有序字节流，不保留应用层 send 调用边界：

```[pseudocode]
sender: send("HEAD") + send("BODY")
receiver: recv() → "HEADBODY"

sender: send(100B)
receiver: recv() → 40B, 再 recv() → 60B

应用必须定义协议边界:
  1) 定长消息
  2) 分隔符(例如 CRLF)
  3) 长度前缀 + body
  4) 自描述编码并处理半包/多包
```

Why: 为什么 TCP 不替应用保留 send 边界？——**TCP 优先提供通用可靠字节流，而不是假定所有应用都使用同一种消息格式**：底层可以按 MSS、拥塞窗口、重传和接收情况自由分段/合并。UDP 保留 datagram 边界，但代价是不同的可靠性与大小限制语义。**“粘包”不是 TCP 的 bug，而是应用把字节流误当成消息队列。** [内核: `tcp_recvmsg` 向用户返回的是连续字节，不知道上层消息边界]

比喻锚点: TCP 像水管，发送端倒两杯水不代表接收端一定收到两个独立杯子；应用协议必须自己标记每杯水的长度或分隔线。 [写作时展开]

### 5. SYN Cookies、TFO、RST 与 backlog — 建连异常如何定位

场景提示: SYN Flood 时半连接队列被打满，服务端为什么仍可能响应合法客户端？而“没有调用 accept”时，握手能不能完成？ [写作时展开]

关键设计: TCP 把握手保护、低延迟优化和连接队列分成不同机制：

```[pseudocode]
SYN Cookies:
  半连接资源紧张时, 不为每个 SYN 长期保存完整 request
  SYN+ACK 的序列信息带有可验证编码
  第三个 ACK 返回时验证 cookie, 再建立连接状态

TCP Fast Open:
  客户端持有服务端 cookie 后, 后续握手可携带早期数据
  目标是减少应用数据等待, 但要考虑重放/服务端策略

RST:
  只有符合连接当前序列空间/状态检查的 RST 才应复位连接

accept:
  三次握手主要在内核完成
  accept 从已完成连接队列取出 socket
  不调用 accept 不等于握手本身无法完成, 但 backlog 满后会影响新连接
```

Why: 为什么 SYN Cookies 不是“永远打开就更安全”？——**它是资源紧张时的防御性降级，会牺牲部分握手状态/选项能力，并不能替代限速、过滤和扩容**；TFO 也不是无条件 0-RTT，是否携带数据取决于 cookie、协议栈和服务端配置。 [内核: `tcp_v4_conn_request`、`cookie_v4_check`、`tcp_fastopen` 分别落在握手防护与快速打开路径]

比喻锚点: SYN Cookie 像门卫不先为每个访客分配房间，而是给访客一道只有真正来回走完才能验证的算题；TFO 则像熟客凭预约码提前把行李送进房间。 [写作时展开]

### 6. 丢包诊断 — 从症状到证据链

场景提示: 用户只说“接口很慢”，你怎样区分拥塞丢包、接收窗口为零、校验和错误、乱序和应用层阻塞？ [写作时展开]

关键设计: 排障必须把 socket 状态、内核计数器和线上抓包拼成同一条证据链：

```[pseudocode]
第一层: ss -ti / ss -tan
  看 state、rtt、cwnd、retrans、delivery rate、send/receive queue

第二层: nstat -az /proc/net/snmp
  看重传、拥塞、丢弃、RST 等内核累计计数

第三层: tcpdump -i eth0 'tcp'
  对比 Seq/Ack、SACK、dup ACK、RTO、RST、窗口通告

候选原因:
  路径拥塞 → 丢包/重传/RTT上升
  接收缓冲区满 → rwnd 下降到 0
  乱序 → duplicate ACK/SACK
  校验和错误 → 接收路径丢弃, 需结合网卡 offload 排查
```

Why: 为什么只看 `ping` 或单个 `retrans` 指标不够？——**它们无法区分网络路径、接收端、网卡 offload 和应用消费速度**：必须把抓包时间线和 `ss -ti` 的状态、内核累计计数对齐。RTO 也不是固定秒数，而是根据 RTT/RTTVAR 动态估计，并受重传退避影响。 [内核: TCP 重传计时器和 RTT 估计共同决定 RTO；[man 8 tcpdump: 抓包验证线上 Seq/Ack 与窗口行为]]

比喻锚点: 排障像查一条高速公路：`ss` 是收费站仪表，`nstat` 是全路段事故统计，`tcpdump` 是沿线摄像头；只看其中一个无法判断堵在哪一段。 [写作时展开]

### 7. 收束

回到线上四类高频症状：
- TIME_WAIT 多：主动关闭端资源与四元组复用策略
- CLOSE_WAIT 多：应用没有及时处理对端 FIN
- 小包延迟：Nagle/延迟 ACK/应用写入模式组合
- recv 不完整：TCP 字节流，需要应用协议解决边界
- 建连异常：SYN Cookies、backlog、TFO、RST 与防火墙共同影响

**Aha Moment**: "TCP 排障不是看到一个参数就调参数，而是把现象放回协议语义：**状态解决连接生命周期，窗口解决资源边界，字节流解决消息边界，抓包和 socket 统计负责把猜测变成证据**。"
**回答读者三问**: ①TIME_WAIT 与 CLOSE_WAIT 差在哪=前者主动关闭后的协议保护，后者通常提示应用未关闭；②粘包怎么修=协议定义定长/分隔符/长度前缀；③TCP 慢怎么查=状态、统计、抓包三层交叉验证。

---

### 核心悬念

**"一个包从网卡进入内核，到用户态 `recv()` 真正读到数据，中间经过 NAPI、软中断、协议栈、socket 接收队列哪些步骤？"**

→ 引出 05-kernel-recv-path — Linux 内核收包全链路——从线上症状进入数据包在内核里的真实旅程。