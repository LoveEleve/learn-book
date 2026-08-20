# TCP 流控与拥塞控制 — 接收窗口、拥塞窗口和 MSS 如何共同决定发送速度

> Cluster B: 13 KPs | 依赖: 02-tcp-state-machine | 读者基线: TCP 包格式、握手、Seq/Ack
> 读者处境: 02 篇已经把连接建立起来了；本篇回答"连接建立后，发送方一次到底能发多少、为什么不能无限快、一个大消息又怎样拆成多个包"
> 打开新视角: TCP 实际能发送的数据量不是一个窗口决定的，而是**接收方的承载能力 `rwnd` 与网络路径的承载能力 `cwnd` 共同取最小值**；MSS/MTU 决定每段能装多少，拥塞控制决定能同时飞多少段

---

### 概念依赖链

```
02-tcp-state-machine(Seq/Ack/握手) → 本篇: 窗口/分段/拥塞控制
  ├─ §1 滑动窗口(rwnd/ACK/Window Scale)
  ├─ §2 TCP 分段 vs IP 分片(MSS/MTU/PMTUD)
  ├─ §3 Reno 基线(cwnd/ssthresh/丢包反馈)
  ├─ §4 CUBIC(以丢包为信号的现代曲线)
  └─ §5 BBR(以带宽/RTT 建模的另一种信号)
先讲: 接收方能吃多少 → 一个段能装多少 → 网络能承受多少 → CUBIC/BBR 如何探测
后续依赖: 04-tcp-troubleshooting(窗口、重传、RTT 进入线上排障)
```

### 叙事顺序

1. 问题引入——连接已建立，发送方为什么不能把 1GB 一股脑塞进网络？（**Aha: TCP 发送能力 = `min(rwnd, cwnd)`，一个限制来自接收方，一个限制来自路径拥塞**）
2. 滑动窗口——ACK 到达，发送窗口右移
3. MSS/MTU——一段数据和一个 IP 包不是同一个概念
4. Reno 基线——从慢启动到快速重传
5. CUBIC/BBR——两种现代拥塞探测哲学
6. 收束——窗口、分段、拥塞三条线合流

### 1. 滑动窗口 — 接收方先告诉发送方“我还能吃多少”

场景提示: 接收端应用读得很慢时，发送端为什么最终也会慢下来，即使网络本身很空？ [写作时展开]

关键设计: TCP 接收窗口 `rwnd` 是接收方根据自身缓冲区通告的信用额度；发送方未确认的 inflight 数据不能超过它，也不能超过拥塞窗口：

```[pseudocode]
接收方:
  接收缓冲区还有 N 字节空间
  → TCP Window 字段通告 rwnd=N

发送方:
  可发送上限 = min(rwnd, cwnd)
  → 已发送但未确认的数据 < 上限
  → ACK 到达后窗口右移, 新 Seq 才能继续发送

rwnd=0:
  → 暂停普通发送
  → Zero Window Probe 定期探测窗口是否恢复
```

Why: 为什么必须有接收窗口，不能只靠发送方自己控制速度？——**发送方看不见接收应用是否及时消费缓冲区**：没有 rwnd，慢接收端会被发送端持续灌满，最终丢数据或耗尽内存。Window Scale 选项在握手时协商，扩大 16 位 Window 字段的表达范围；它不是“14 倍”，而是最多左移 14 位，实际窗口还受缓冲区、实现和路径限制。 [内核: `tcp_sock` 中的发送/接收窗口状态与 `tcp_clean_rtx_queue` 的确认推进共同驱动滑动窗口]

比喻锚点: rwnd 像餐厅告诉厨房“桌上还剩几个座位”，厨房不能只看自己有多少菜；ACK 就像服务员收走盘子后，桌面重新空出位置。 [写作时展开]

### 2. TCP 分段 vs IP 分片 — MSS、MTU 与一个大写入

场景提示: 应用写入 1501 字节，网线上到底是一个包、两个 TCP 段，还是多个 IP 分片？ [写作时展开]

关键设计: TCP 分段发生在传输层，IP 分片发生在网络层，两者不是一回事：

```[pseudocode]
TCP segmentation
  用户字节流
  → 按 MSS 拆成多个 TCP segment
  → 每段独立拥有 Seq/TCP header

IP fragmentation
  一个过大的 IP packet
  → 网络层按更小链路 MTU 拆成 fragments
  → 目的端 IP 层重组

典型 Ethernet MTU=1500、无额外 option 时:
  MSS ≈ 1500 - 20(IP header) - 20(TCP header) = 1460
  1501B payload → 1460B + 41B 两个 TCP segment
```

Why: 为什么工程上尽量避免 IP 分片？——**分片把一个完整 IP 包拆成多个相互依赖的片段，任一片丢失都可能让整个上层报文无法重组**；中间防火墙、隧道和 NAT 也可能对分片处理不一致。PMTUD 通过 DF 和 ICMP “Fragmentation Needed”让端点发现路径 MTU；实际部署还要考虑 ICMP 被过滤、隧道开销和 MSS clamping。 [内核: `tcp_mtu_to_mss` 把路径 MTU转换成 TCP 可用 MSS；IP 层的 `ip_fragment` 是另一条分片路径]

比喻锚点: MSS 是快递箱内能装的货量，MTU 是整辆货车允许的总重量；TCP 先把货装成箱，IP 只在整箱仍塞不进车时才被迫拆箱。 [写作时展开]

### 3. Reno 基线 — cwnd 如何从试探性发送增长到遇到丢包

场景提示: 网络一开始看起来很快，为什么 TCP 不直接把带宽占满，而要从小窗口慢慢试？ [写作时展开]

关键设计: `cwnd` 是发送方对路径可承载 inflight 数据的估计，`ssthresh` 决定慢启动何时转为拥塞避免：

```[pseudocode]
连接开始:
  cwnd 较小
  → slow start: 每收到 ACK 就增长, 每个 RTT 大致指数增长
  → 达到 ssthresh 或出现拥塞信号

拥塞避免:
  → cwnd 近似每 RTT 增长一个 MSS

丢包信号:
  3 个 duplicate ACK → fast retransmit, 不等 RTO
  超时 → 更保守地降低发送速率
  Reno 类算法据此调整 ssthresh/cwnd
```

Why: 为什么慢启动叫“慢”，实际却是指数增长？——**它相对“直接打满链路”更谨慎，但窗口会快速扩大**：TCP 没有先验知道路径带宽和队列容量，只能用 ACK/丢包反馈探测。快速重传利用 duplicate ACK 提供的“中间段已到达”证据，避免每次丢包都等完整 RTO。 [内核: `tcp_slow_start`、`tcp_reno_cong_avoid` 与 `tcp_fastretrans_alert` 分别体现增长、拥塞避免和快速恢复]

比喻锚点: Reno 像试开一条陌生高速路——先少量放车，确认没有堵车后逐步加车；看到连续车辆反馈异常就马上减速，而不是等整条路完全堵死。 [写作时展开]

### 4. CUBIC — 用三次曲线探测历史带宽点

场景提示: 高带宽、高 RTT 的长距离链路上，线性每 RTT 加一点窗口会不会太慢？ [写作时展开]

关键设计: CUBIC 用窗口历史峰值和时间构造三次函数，减少对 RTT 的强依赖：

```[pseudocode]
丢包后记录 W_max
随着时间 t 增长:
  W(t) = C * (t - K)^3 + W_max
  K 与 W_max、算法参数相关

曲线直觉:
  先谨慎恢复
  接近历史 W_max 时放慢探测
  越过历史峰值后继续探测可用容量
```

Why: 为什么 CUBIC 不沿用 Reno 的线性增长？——**长 RTT 路径上，按 RTT 计步的算法恢复太慢**；CUBIC 把时间作为曲线自变量，让不同 RTT 的连接在高带宽路径上更容易恢复和探测。它仍然主要依赖丢包/拥塞反馈，曲线参数和 Linux 版本会影响实际行为。 [内核: `tcp_cubic.c` 的 `bictcp_update`/`bictcp_cong_avoid` 维护 CUBIC 窗口曲线]

比喻锚点: CUBIC 像记住上一次山顶位置的登山者——接近旧山顶时放慢脚步，越过后再继续探路，而不是每走固定一步才增加一点速度。 [写作时展开]

### 5. BBR — 从“丢包了”转向“带宽/RTT 模型”

场景提示: 缓冲区很大的网络可能很久不丢包，但队列已经排得很长、延迟明显升高；只等丢包才降速会发生什么？ [写作时展开]

关键设计: BBR 通过已测带宽与最小 RTT 估计瓶颈，周期性调整 pacing/cwnd，而不是把丢包当作唯一拥塞信号：

```[pseudocode]
估计:
  BtlBw = 一段时间内测得的 delivery rate
  RTprop = 观测到的最小 RTT

模型:
  BDP ≈ BtlBw × RTprop
  pacing_rate / cwnd 围绕模型探测

典型阶段:
  Startup → Drain → ProbeBW → ProbeRTT
```

Why: 为什么 BBR 可能在丢包之前主动降速？——**它关注的是瓶颈带宽和传播 RTT，而不是等队列溢出才发现拥塞**：当吞吐不再增长、RTT 却上升时，继续加速只会制造 bufferbloat。BBR 与 CUBIC 并非简单的“新算法必然更好”，实际公平性、版本、链路队列和竞争流都会影响结果。 [内核: `tcp_bbr.c` 维护带宽/RTT模型与 Startup/Drain/ProbeBW/ProbeRTT 状态]

比喻锚点: CUBIC 像看到堵车后刹车，BBR 像根据车流速度和道路长度估算容量，在车队排长前就控制进入速度。 [写作时展开]

### 6. 收束

回到“一个 TCP 连接到底能发多快”：
- `rwnd`：接收方还有多少缓冲空间
- `cwnd`：发送方估计网络还能承受多少 inflight 数据
- `MSS`：每个 TCP segment 能装多少应用数据
- `MTU`：一条链路允许的最大 IP 包大小
- CUBIC/BBR：发送方如何根据反馈探测路径容量

完整心智模型：

```[pseudocode]
应用字节流
  → 按 MSS 切成 TCP segments
  → 未确认数据受 min(rwnd, cwnd) 限制
  → IP/链路 MTU 决定是否需要分片或调整 MSS
  → ACK / RTT / 丢包 / delivery rate 反馈给拥塞控制
```

**Aha Moment**: "TCP 速度不是一个旋钮，而是四个约束叠加的结果：**接收窗口决定对端吃得下多少，拥塞窗口决定网络撑得住多少，MSS 决定一段装多少，拥塞算法决定窗口如何试探性增长**。"
**回答读者三问**: ①rwnd 和 cwnd 差在哪=前者是接收能力，后者是路径拥塞估计；②MSS 和 MTU 差在哪=MSS 是 TCP 数据上限，MTU 是链路 IP 包上限；③CUBIC 和 BBR 的思路差在哪=CUBIC主要按丢包后的曲线恢复，BBR以带宽/RTT建模探测。

---

### 核心悬念

**"如果窗口与拥塞控制都设计得很好，线上为什么仍会出现重传、RTT 飙升、TIME_WAIT 堆积和‘粘包’误解？如何用 ss、tcpdump、sar 把问题定位出来？"**

→ 引出 04-tcp-troubleshooting — TCP 生产排障——从算法模型进入真实线上症状。