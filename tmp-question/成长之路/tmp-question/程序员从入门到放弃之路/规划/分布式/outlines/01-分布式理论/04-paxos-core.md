# Basic Paxos — 三个角色如何在故障与竞争提案中决定同一个值

> Cluster B: 15 KPs | 依赖: 01-cap-consistency-model、02-system-failure-models、03-time-logical-clocks | 读者基线: 共识、quorum、故障模型、逻辑顺序
> 读者处境: 前三篇说明了为什么需要共识、故障/网络为什么不可靠、逻辑时间如何排序；本篇把“多数派如何决定一个值”推导成 Basic Paxos
> 打开新视角: Paxos 的核心不是“投票选队长”，而是**Prepare 获取历史约束，Accept 继承已知值，多数派交集保证安全性**

---

### 概念依赖链

```
01 CAP + 02 故障模型 + 03 逻辑时间 → 本篇: Basic Paxos
  ├─ §1 共识目标(Agreement/Validity/Termination)
  ├─ §2 Prepare/Promise(读取历史与拒绝旧编号)
  ├─ §3 Accept/Chosen(继承值与多数派)
  └─ §4 Multi-Paxos(稳定 Leader 优化连续实例)
先讲: 需要保证什么 → Prepare → Accept → Leader/日志优化
后续依赖: 05-multi-paxos-raft(选举、日志和安全性比较)
```

### 叙事顺序

1. 问题引入——三个节点收到不同提案，如何保证两个正确节点不会决定不同值？
2. 共识安全/活性——先定义“正确”
3. Prepare——新提议者先获取历史约束
4. Accept——为什么必须继承已有值
5. Multi-Paxos——稳定 Leader 后减少 Prepare
6. 收束——多数派交集与两阶段协议

### 1. 共识问题 — Agreement、Validity 与 Termination

场景提示: 三个节点分别提议不同值，网络还可能延迟消息；最终结果怎样才算共识成功？ [写作时展开]

关键设计: 共识协议通常分别描述安全性与活性：

```[pseudocode]
Agreement:
  两个正确节点不能决定不同值

Validity:
  决定的值必须来自某个合法提案

Termination:
  在假设满足时, 正确节点最终决定

Paxos核心:
  安全性在任意消息延迟/节点故障下保持
  活性需要多数派可达、竞争收敛等额外条件
```

Why: 为什么 Paxos 不能承诺所有情况下都终止？——**FLP 说明异步系统中即使只有一个故障，也不能同时保证确定性共识的安全与无条件终止**；Paxos 优先守住 Safety，实际系统通过稳定 Leader、超时、随机退避和部分同步提高 Liveness。 [分布式理论: §02 的 FLP 是本篇“安全与活性分离”的前置]

比喻锚点: 会议可以保证“不通过互相矛盾的两份决议”，但在主席失联、成员掉线和消息延迟时，会议可能暂时无法通过任何决议。 [写作时展开]

### 2. Prepare / Promise — 先获取历史，再提出新值

场景提示: 新 Proposer 不能直接把自己的值发给多数派吗？为什么要先发一个编号？ [写作时展开]

关键设计: Prepare 阶段用唯一递增的 ballot/proposal number 建立承诺，并让 Proposer 看见 Acceptor 已接受的历史：

```[pseudocode]
Proposer:
  选择唯一 proposal number N
  → 向 Acceptor 发送 Prepare(N)

Acceptor 收到:
  若 N 高于已承诺编号
    → Promise: 不再接受低于 N 的提案
    → 返回自己已接受的最高编号/value(如有)
  否则拒绝/忽略

Proposer:
  收集多数派 Promise
  → 得到历史约束
```

Why: 为什么 Promise 不只是“投票支持 N”？——**它包含两件事：拒绝未来低编号提案，以及把本地已接受历史告诉新 Proposer**；第二部分让新值不能覆盖已经可能被多数派接受的旧值。Proposal number 需要全局唯一/可比较，生成方式由实现负责。 [分布式理论: 多数派交集使新 Proposer 有机会看到已被选择路径约束的历史]

比喻锚点: Prepare 像新任书记先查旧决议档案，并宣布“编号更低的草案不再受理”，而不是直接把自己的草案盖章。 [写作时展开]

### 3. Accept / Chosen — 继承已有值是安全性的核心

场景提示: Proposer 收到多数 Promise 后，究竟提交自己的值，还是提交某个 Acceptor 返回的旧值？ [写作时展开]

关键设计: 如果 Promise 中出现已接受提案，Proposer 必须选择其中编号最高的 value；只有没有历史约束时，才能选择自己的 value：

```[pseudocode]
多数 Promise 返回:
  有 accepted proposal?
    → 选其中 proposal number 最高的 value
  无 accepted proposal?
    → 可选自己的 value

发送:
  Accept(N, V) → Acceptors

Acceptor:
  若尚未承诺更高编号
    → 接受(N,V)

Chosen:
  某 value 被足够多数 Acceptors 接受
  → Learner/其他节点学习该决定
```

Why: 为什么“继承最高已接受值”能保护 Agreement？——**任意两个多数派必有交集，新的多数派在 Prepare 阶段会看到交集中的历史约束，不能随意提出冲突 value**；Prepare/Accept 两阶段不是为了形式复杂，而是把“读取已有约束”和“写入新决定”分开。 [分布式理论: quorum intersection 是 Paxos Safety 的核心，而不是三节点特例]

比喻锚点: 新决议起草前必须继承档案中已经进入正式流程的最高版本；否则不同会议都可能给同一事项盖上不同结论。 [写作时展开]

### 4. 多数派与故障 — 三节点为什么能容忍一个故障

场景提示: 三节点集群中两个节点还能互相通信，一个节点宕机，为什么可以继续决定？ [写作时展开]

关键设计: Paxos 需要一个多数派 quorum；多数派相交保证安全，少数派无法单独决定：

```[pseudocode]
N=3:
  quorum=2
  可容忍 1 个节点故障

N=2f+1:
  quorum=f+1
  最多容忍 f 个故障并继续形成多数

网络分区:
  多数侧 → 可能继续决定
  少数侧 → 不能安全提交新决定

注意:
  能继续决定 ≠ 所有请求都立即成功
  还受 Leader、网络和存储状态影响
```

Why: 为什么两个节点都活着却可能无法提交？——**如果它们互相分区，各自只有一个节点，任何一侧都拿不到多数派**；Paxos 用牺牲少数侧可用性换取不分叉的安全性。quorum 公式还假设固定成员/正确成员变更协议，不能无条件套到动态集群。 [分布式理论: CAP 的 CP 取舍在这里具体体现为少数分区停止决定]

### 5. Multi-Paxos — 稳定 Leader 后减少重复 Prepare

场景提示: 每写一个日志 entry 都执行 Prepare/Promise/Accept，为什么生产系统要引入 Leader？ [写作时展开]

关键设计: Multi-Paxos 把多个共识实例组织成日志，在稳定 Leader/ballot 下复用 Prepare 阶段：

```[pseudocode]
初始:
  Leader 先完成一次 Prepare/Promise

稳定期:
  每个 log slot/instance
  → Leader 直接提议 Accept(instance, value)
  → 多数派接受
  → entry Chosen

故障:
  新 Leader 重新获取历史约束
  → 补空洞/恢复日志
  → 继续后续 instance
```

Why: 为什么 Multi-Paxos 能减少 RTT，却不能简单说“Paxos 变成一阶段”？——**只有在稳定 Leader、ballot 和成员条件成立时，Prepare 才能复用；Leader 变化、日志空洞、成员变更和恢复仍需额外协议**。Paxos 日志允许更灵活的实例状态，具体日志匹配和提交规则与 Raft 的强制前缀匹配不同。 [分布式理论: Multi-Paxos 是工程优化族，不是唯一固定实现]

比喻锚点: 议会选出稳定主席后，后续议案不必每次重新确认谁有资格主持；主席换人时，仍要重新查阅旧议案档案。 [写作时展开]

### 6. 收束

Basic Paxos 心智模型：

```[pseudocode]
Prepare(N)
  → Promise(拒绝低编号 + 返回历史)
  → 选择必须继承的 value
  → Accept(N,V)
  → 多数派接受
  → Chosen/Learned

安全性:
  ballot 顺序 + 历史继承 + quorum intersection
活性:
  需要多数派可达 + 竞争收敛 + 稳定 Leader/超时策略
```

**Aha Moment**: "Paxos 的核心不是“多数投票选谁”，而是**Prepare 先读取历史约束，Accept 再写入新值，多数派交集让新提案无法绕过已决定的值**。"
**回答读者三问**: ①Prepare 做什么=承诺编号并获取历史；②Accept 为什么要继承旧值=保护 Agreement；③三节点为何能容忍一个故障=两节点多数，少数分区不能提交。

---

### 核心悬念

**"Paxos 的 Leader 挂了怎么办？Multi-Paxos 和 Raft 都需要领导者，但 Raft 如何用随机超时、日志匹配和任期把选举与复制讲得更工程化？"**

→ 引出 05-multi-paxos-raft — Multi-Paxos 与 Raft 的选举、日志、安全和成员变更。