# Paxos 变种 — Fast、Disk、Generalized 与 Flexible 分别放宽了什么

> Cluster B: 12 KPs | 依赖: 04-paxos-core、05-multi-paxos-raft、06-zab-epaxos | 读者基线: quorum、Leader、日志、冲突依赖
> 读者处境: 前三篇已经覆盖 Basic/Multi-Paxos、Raft、ZAB、EPaxos；本篇不再背论文列表，而是按“减少哪种成本、引入什么新约束”理解变种
> 打开新视角: Paxos 变种不是版本升级，而是**用更大 quorum、共享磁盘、可交换命令或不对称 quorum 换取特定场景的延迟/硬件/并发收益**

---

### 概念依赖链

```
04-06 共识协议 → 本篇: Paxos 变种族
  ├─ §1 Fast Paxos(减少一轮消息)
  ├─ §2 Disk Paxos(共享存储替代消息)
  ├─ §3 Cheap/Generalized/Flexible/Stoppable(特化约束)
  └─ §4 选择矩阵(收益与代价)
先讲: 低延迟 → 共享磁盘 → 低硬件/可交换操作/quorum → 选型
后续依赖: 08-2pc-3pc-tcc(共识与原子提交的差异)
```

### 叙事顺序

1. 问题引入——Basic/Multi-Paxos 已能工作，为什么还要发明一堆变种？
2. Fast Paxos——用更大 quorum 换一轮延迟
3. Disk Paxos——用共享稳定存储替代消息状态
4. Cheap/Generalized/Flexible/Stoppable——分别放宽硬件、命令顺序、quorum 或运行状态
5. 选择矩阵——没有通用最优
6. 收束

### 1. Fast Paxos — 没有冲突时尝试一轮决定

场景提示: 客户端提议直接到 Acceptor，能否跳过 Prepare，减少共识延迟？ [写作时展开]

关键设计: Fast Paxos 允许 Fast Round，但必须使用不同于 Classic Round 的 quorum 和冲突回退路径：

```[pseudocode]
Fast Round:
  client/proposer 直接发送 value
  → Fast quorum 收到/接受
  → 无冲突且满足交集条件 → value chosen

冲突:
  多个 value 同时到达
  → 无法唯一确定
  → 回退 Classic Round
  → Prepare/Promise/Accept 解决

核心:
  Fast quorum 比普通多数派更大
  → 用更大交集换跳过一轮的机会
```

Why: 为什么 Fast Paxos 不是“少一轮、所有场景都更快”？——**跳过 Prepare 就少了发现历史/冲突的机会，必须用更大 quorum 保证安全；一旦客户端并发提议冲突，还要回退经典路径，最坏情况可能更复杂**。Fast quorum 的确切公式和容错条件取决于协议参数，不能直接把某个 N 的数字当通用配置。 [分布式理论: 低冲突是收益前提，更大 quorum 是安全代价]

比喻锚点: Fast Paxos 像熟客直接把订单送进多个厨房窗口；没有撞单时很快，撞单后必须叫总协调员重新核账。 [写作时展开]

### 2. Disk Paxos — 把 Acceptor 状态放进共享存储

场景提示: 节点网络不可靠，但多个处理器能访问共享磁盘；共识状态能否通过磁盘块交换？ [写作时展开]

关键设计: Disk Paxos 用共享存储记录 Acceptor 状态，Proposer 通过读写磁盘观察协议进度：

```[pseudocode]
Acceptor state:
  accepted ballot/value
  → 写入为其分配的共享磁盘区域

Proposer:
  读取多个区域
  → 计算 quorum/历史状态
  → 写入更高 ballot/value

安全前提:
  共享存储读写/故障模型满足协议假设
  → 仍需要 quorum 与原子性/可见性条件
```

Why: 为什么共享磁盘不能简单说成“写成功就比网络可靠”？——**磁盘设备、路径、缓存、写入原子性、共享存储分区同样可能故障**；Disk Paxos 把通信和状态成本换到了共享存储，不是消灭故障。适用场景还要求多个节点能可靠访问同一存储系统。 [分布式理论: 任何替代通信介质都必须重新声明故障和原子写假设]

比喻锚点: 多个办公室不互相打电话，而是共用一本带锁的登记簿；电话问题少了，但登记簿和存储柜成了新的关键基础设施。 [写作时展开]

### 3. Cheap、Generalized、Flexible、Stoppable — 变种各自换什么

场景提示: 除了降低一轮延迟，还有哪些方法能降低硬件成本、冲突等待或 quorum 成本？ [写作时展开]

关键设计: 四种路线放宽的是不同约束：

```[pseudocode]
Cheap Paxos:
  少数主处理器参与正常协议
  → 辅助处理器在故障时补位
  → 降低常态硬件成本, 恢复更复杂

Generalized Paxos:
  可交换/不冲突命令允许不同顺序
  → 只对不可交换命令建立顺序约束
  → 需要定义命令冲突/交换关系

Flexible Paxos:
  Prepare quorum 与 Accept quorum 可不相同
  → 安全交集条件必须满足
  → 可按读/写路径和拓扑优化

Stoppable Paxos:
  进入停止状态, 不再接受新决定
  → 为维护/升级提供安全边界
  → 停止本身也要被一致决定
```

Why: 为什么 Flexible Paxos 不是“任意选两个 quorum 就安全”？——**安全要求的是跨阶段 quorum 的交集条件，而不是每个阶段都简单多数**；减少某阶段参与者可能增加另一阶段成本或降低可用性。Generalized Paxos 也需要可靠的交换性定义，不能把所有不同 key 操作都自动视作安全可交换。 [分布式理论: 变种优化的是 quorum/顺序/硬件假设，安全证明条件必须随之重写]

### 4. 选择矩阵 — 论文收益必须匹配系统约束

场景提示: 一个团队应该因为“理论上低延迟”就选择 Fast Paxos 或 EPaxos 吗？ [写作时展开]

关键设计: 选择先看约束，再看变种：

```[pseudocode]
通用复制状态机:
  Raft/Multi-Paxos
  → 生态、理解和维护优先

低冲突/低延迟:
  Fast Paxos/EPaxos 类方案
  → 先验证冲突率、跨地域延迟和回退频率

共享存储:
  Disk Paxos
  → 先验证共享设备的故障/性能/运维

可交换命令多:
  Generalized Paxos
  → 先定义交换关系与执行语义

成本/维护模式:
  Cheap/Stoppable/Flexible
  → 评估故障时复杂度和成员变更
```

Why: 为什么理论最优不等于生产最优？——**协议复杂度、监控、故障演练、开发能力和回退路径都会进入总成本**；没有代表性 workload 与故障模型验证，变种的纸面收益可能被冲突、网络或运维开销抵消。 [系统性能/分布式理论: 低延迟、吞吐、安全性和可操作性应共同验收]

### 5. 收束

Paxos 家族的共同问题：

```[pseudocode]
Basic/Multi-Paxos:
  经典 quorum + Promise/Accept

Fast:
  更大 quorum 换一轮延迟
Disk:
  共享存储承载 Acceptor 状态
Generalized:
  可交换命令减少全序
Flexible:
  不对称 quorum 调整路径成本
Cheap/Stoppable:
  调整硬件/运行状态与维护边界
```

**Aha Moment**: "Paxos 变种不是把共识变魔法，而是**明确把成本从一个地方搬到另一个地方**：延迟换 quorum，消息换共享存储，全序换依赖分析，硬件成本换恢复复杂度。"
**回答读者三问**: ①Fast Paxos 为什么快=低冲突时跳过 Prepare，但需更大 quorum；②Flexible Paxos 何时安全=跨阶段 quorum 满足交集条件；③如何选变种=先看故障模型、冲突率、拓扑、团队和回退路径。

---

### 核心悬念

**"共识解决的是多节点决定同一个值，事务还要把多个资源的提交绑在一起；2PC、3PC、XA、TCC 为什么各自牺牲了什么？"**

→ 引出 08-2pc-3pc-tcc — 分布式事务协议。