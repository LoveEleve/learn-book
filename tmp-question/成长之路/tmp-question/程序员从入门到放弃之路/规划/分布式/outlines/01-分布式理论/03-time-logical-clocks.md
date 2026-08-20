# 物理时钟、逻辑时钟与分布式快照 — 没有共享时钟，系统如何定义先后

> Cluster A: 12 KPs | 依赖: 02-system-failure-models | 读者基线: Unix timestamp、网络延迟、故障/时间模型
> 读者处境: 02 篇说明时钟不可信；本篇回答：不同服务器时间不同、消息延迟不确定时，怎样定义因果顺序、检测并发、给运行中系统拍一致快照
> 打开新视角: 分布式系统中的“时间”有四种用途——**显示时间、因果排序、并发检测、外部一致时间**，不能用一个物理 timestamp 解决全部问题

---

### 概念依赖链

```
02 故障/网络/时间模型 → 本篇: 物理时间/逻辑时间/快照
  ├─ §1 物理时钟/NTP/PTP(误差与单调性)
  ├─ §2 Lamport 时钟(happens-before/可排序)
  ├─ §3 向量时钟(并发检测/版本冲突)
  ├─ §4 Chandy-Lamport 快照(运行中一致状态)
  └─ §5 TrueTime/HLC(外部时间与务实折中)
先讲: 物理误差 → 因果序 → 并发检测 → 全局快照 → 外部一致时间
后续依赖: 04-paxos-core(逻辑顺序与共识)
```

### 叙事顺序

1. 问题引入——北京和纽约服务器都记录 15:00，谁先发生？
2. 物理时钟——NTP/PTP、漂移、跳变
3. Lamport——不靠绝对时间排序因果
4. 向量时钟——进一步检测并发
5. Chandy-Lamport——给运行中的系统拍一致快照
6. TrueTime/HLC——外部一致与工程折中
7. 收束——时间工具的选择

### 1. 物理时钟 — NTP 同步不了“绝对同时”

场景提示: 两地日志显示同一秒，但网络延迟和时钟漂移让事件顺序不确定；NTP 如何估计偏移？ [写作时展开]

关键设计: 物理时钟同步是估计，不是共享完美时钟：

```[pseudocode]
NTP exchange:
  t1 client send
  t2 server receive
  t3 server send
  t4 client receive

offset/delay:
  根据四个时间戳估计时钟偏移和往返延迟
  → 上下行延迟不对称会引入误差

现实约束:
  oscillator drift
  network jitter
  NTP step/slew
  PTP/GPS/硬件时间戳可在特定环境提高精度
```

Why: 为什么日志 timestamp 不能直接决定跨节点事件先后？——**时钟偏差、网络不对称、校时跳变和精度边界会让 timestamp 倒退或交叉**；展示时间和超时计时应区分 `CLOCK_REALTIME` 与单调时钟，分布式排序不能盲信墙钟。 [系统编程: `CLOCK_MONOTONIC` 适合 elapsed time，但不提供跨节点全局顺序]

比喻锚点: 多台钟像不同城市的手表，校时能让它们接近，但不能保证每次指针同时跳动。 [写作时展开]

### 2. Lamport 逻辑时钟 — 用因果关系而非绝对时间排序

场景提示: 两个节点没有同步物理时钟，但消息发送和接收仍有因果关系，怎样表达？ [写作时展开]

关键设计: happens-before 定义偏序，Lamport 时钟保证因果事件的逻辑数值递增：

```[pseudocode]
happens-before:
  同进程 a 先于 b → a → b
  send(m) → receive(m)
  传递性: a→b 且 b→c → a→c

Lamport clock C:
  本地事件: C = C + 1
  发送: 消息携带 C
  接收: C = max(C_local, C_message) + 1

保证:
  a → b ⇒ C(a) < C(b)
```

Why: 为什么 Lamport 时间戳不能反推出所有并发关系？——**`C(a)<C(b)` 只是必要条件，不是充分条件；两个并发事件也可能拿到大小不同的数字**。用 `(C, node_id)` 可构造确定性全序，但并发事件的先后只是人为 tie-break，不代表真实因果。 [分布式理论: 逻辑时钟描述的是可观察因果，不是物理时间]

比喻锚点: Lamport 时钟像给每封公文盖递增编号：能证明回复晚于来文，但两条互不相干的部门公文谁先盖章只是编号规则，不是因果事实。 [写作时展开]

### 3. 向量时钟 — 区分“先后”与“真正并发”

场景提示: 两个用户同时编辑同一文档，Lamport 数字无法判断是否冲突；向量时钟怎样表达？ [写作时展开]

关键设计: 每个节点维护一组计数，逐位比较可以判断因果或并发：

```[pseudocode]
Node i local event:
  VC[i]++

send:
  message carries VC

receive VC_m:
  VC[j] = max(VC[j], VC_m[j]) for all j
  VC[receiver]++

compare(V1,V2):
  V1 < V2(逐位不大且至少一位小)
    → V1 happens-before V2
  互不可比
    → concurrent
```

Why: 为什么向量时钟能检测并发，却不适合无限扩展的节点集群？——**向量大小通常随参与者数量增长，节点加入/离开、版本修剪和多副本冲突都会增加管理成本**；Dynamo/Riak 类系统可采用版本向量、裁剪和客户端合并，但这不是自动消灭冲突。 [分布式理论: 向量时钟给出因果信息，冲突合并仍是业务/存储策略]

比喻锚点: 向量时钟像每个部门都维护一列公文计数；比较两份公文时，逐部门看谁都不落后，才能判断是否存在因果关系或真正并发。 [写作时展开]

### 4. Chandy-Lamport 快照 — 给运行中的系统拍一致状态

场景提示: 交易消息仍在飞，怎样统计某一时刻所有节点余额和通道中未到达的消息？ [写作时展开]

关键设计: 分布式快照记录节点本地状态，并把 channel 中的 in-flight 消息作为快照边界的一部分：

```[pseudocode]
initiator:
  记录自身状态
  → 向所有出 channel 发送 marker
  → 开始记录各入 channel 的消息

节点第一次收到 marker:
  记录自身状态
  → 在该 channel 停止继续记录
  → 向所有出 channel 转发 marker

后续收到 marker:
  → 停止记录对应 channel

所有 channel marker 收齐:
  → 节点状态 + channel 状态 = 一致快照
```

Why: 为什么不能让所有节点同时按本地时钟拍照？——**没有共享瞬间，消息可能在路上；marker 定义了每条 channel 的边界，把飞行中的消息归入快照状态**。算法还依赖可靠、有序通信等模型条件，Kafka/Flink checkpoint 等工程机制可以借鉴思想，但不等同于直接实现 Chandy-Lamport。 [分布式理论: 快照解决的是全局状态观测，不是自动完成事务提交]

比喻锚点: 快照像给多列火车拍照：每个站收到“拍照标记”后冻结本站状态，同时把尚未通过区间的列车记录下来。 [写作时展开]

### 5. TrueTime 与 HLC — 外部一致时间的硬件方案与务实方案

场景提示: 全球事务需要按外部时间顺序提交，普通 NTP 误差不够小；TrueTime 和 HLC 分别怎样解决？ [写作时展开]

关键设计: TrueTime 返回时间区间而不是假装给出精确时刻，HLC 则把物理时间和逻辑计数结合：

```[pseudocode]
TrueTime.now()
  → [earliest, latest]
  → 真实时间被认为位于区间内

外部一致提交:
  分配 commit timestamp
  → 等待不确定性窗口过去
  → 再让提交对外可见
  → 保证外部观察顺序

HLC:
  (physical component, logical counter)
  → 物理时间接近现实
  → 逻辑计数处理同一时刻/消息因果
  → 不要求 GPS/原子钟, 但语义不同于 TrueTime
```

Why: 为什么 TrueTime 的“等待”是设计的一部分，而不是实现低效？——**它用不确定性窗口换取外部一致性保证**；等待越大，提交延迟越高。HLC 更易部署，但不能无条件提供 TrueTime 的绝对时间区间保证，必须明确系统所需的是因果序、逻辑全序还是外部一致性。 [分布式理论: 时间 API 的精度、误差和一致性保证必须写进协议语义]

### 6. 收束

时间工具选择：

```[pseudocode]
展示/超时:
  物理墙钟 + 单调时钟

因果排序:
  Lamport clock

并发检测/冲突版本:
  Vector clock

运行中全局状态:
  Chandy-Lamport snapshot

外部一致时间:
  TrueTime 类区间时钟 / HLC 等工程折中
```

**Aha Moment**: "分布式系统没有一个万能时间戳：**物理时钟适合显示，Lamport 适合因果排序，向量时钟识别并发，快照记录全局状态，TrueTime/HLC 服务不同强度的全局顺序需求**。"
**回答读者三问**: ①Lamport 能判断并发吗=不能，只保证因果单向性；②向量时钟为什么更强=不可比关系能表示并发；③TrueTime 为什么要等=用不确定性窗口换外部一致性。

---

### 核心悬念

**"时间只给出了事件顺序，三个节点如何通过 Prepare/Accept 和多数派，把一个值真正决定下来？"**

→ 引出 04-paxos-core — Paxos Prepare/Accept、多数派与安全性。