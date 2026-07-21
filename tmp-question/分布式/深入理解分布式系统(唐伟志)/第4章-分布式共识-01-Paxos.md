# 第4章 分布式共识 · 01 — Paxos

> **本篇定位**：第 4 章核心章节的第一篇，深入 Paxos 算法的形式化推导与工程实现。
>
> 第 2 章证明了 FLP 不可能性（异步 + 崩溃 → 共识不可解），第 3 章建立了 Quorum 数学基础（`W+R>N` 保证强一致）。**Paxos 是这两个理论框架下的第一个工业级共识算法**——Lamport 1990 年完成初稿，1998 年正式发表于 TOCS，是所有强一致分布式系统的算法源头。
>
> 本篇不照搬书，而是从"为什么需要 Paxos"出发，按"直觉方案 → 缺陷 → 改进 → Paxos 形式化 → Multi-Paxos → 工业实现"的顺序展开。读完本篇，你将理解：
> 1. Paxos 不是某人的灵感，而是 FLP + Quorum 约束下的**必然数学解**
> 2. Basic Paxos 的两阶段为什么是**最小**设计（少一阶段不行，多一阶段浪费）
> 3. Multi-Paxos 如何从单值共识扩展为日志复制（SMR 的基础）
> 4. Paxos 的工业实现（Chubby / Spanner / MySQL MGR）如何在工程上做妥协
>
> **跨书视角声明**：
> - **释慧利《深入理解分布式共识算法》第 4 章 Paxos** 视角为主：算法形式化推导，本篇主线
> - **唐伟志《深入理解分布式系统》第 4 章** 视角次之：理论 + Go 实现简化版
> - **江峰《分布式高可用算法》第 8 章 共识 + 第 3.9 节多数派定理** 视角给形式化基础
> - **陈东明《分布式系统与一致性》第 7-9 章 ZK/Spanner/CockroachDB** 视角给工业案例
>
> **写作顺序的依赖关系**：
> 1. 为什么需要 Paxos（动机） → 2. 共识问题形式化（回顾） → 3. Basic Paxos 直觉推导 → 4. Basic Paxos 形式化 → 5. 活锁与解决 → 6. Multi-Paxos → 7. 工业实现 → 8. 总结与 Raft 演进动机

---

## 1. 为什么需要 Paxos？

### 1.1 从 FLP + Quorum 到 Paxos 的必然性

第 2 章证明了 FLP 不可能性：**异步网络 + 一个节点崩溃 → 不存在确定性共识算法**。第 3 章建立了 Quorum 数学基础：`W+R>N` 保证强一致，多数派是最优配置。

**这两个理论框架留下了一个工程问题**：能否在"部分同步假设"（绕过 FLP）+ "多数派 Quorum"（保证一致性）的约束下，设计一个**可证明正确**的共识算法？

答案是 Paxos。Lamport 1990 年完成初稿，1998 年正式发表于 TOCS。**Paxos 不是灵感，而是上述约束下的代表性工程解**——多数派 Quorum 框架下的多数工业级共识算法最终都采用了类似"两阶段 + 多数派 + Leader 优化"的核心结构（**注**：Ben-Or 1983 的纯异步+随机化算法不依赖 Quorum，比 Paxos 早，是另一条绕过 FLP 的路径；Fast Paxos 在 Leader 稳定时可简化为一阶段——这些例外说明"Paxos 是唯一解"的表述过于绝对，但 Paxos 是工业界主流选择的算法基础）。

### 1.2 共识问题的工程动机

考虑一个具体工程问题：**3 节点集群就某个值达成一致**。

场景：分布式锁服务选主。3 个节点中要选出一个 Leader，所有节点必须就"谁是 Leader"达成一致。如果两个节点认为 A 是 Leader，一个节点认为 B 是 Leader，就会脑裂。

**为什么需要共识算法**：
- 单点决定（如指定 A 为 Leader）→ A 故障则整个系统不可用
- 多点并行决定 → 可能产生冲突（不同节点决定不同值）
- 需要"多数派一致"才能保证：① 不会冲突 ② 容忍少数故障

### 1.3 Paxos 的设计目标

Paxos 算法解决以下问题：

> n 个节点（其中最多 f 个可能崩溃，n ≥ 2f+1），每个节点可能提议一个值，算法必须保证：
> 1. **Agreement**（一致性）：所有非崩溃节点**最终决定同一个值**
> 2. **Validity**（有效性）：决定的值必须是某个节点提议过的
> 3. **Termination**（终止性）：在部分同步假设下，所有非崩溃节点**最终**决定一个值

注意：
- Agreement 和 Validity 是 safety 性质（"坏事永不发生"）
- Termination 是 liveness 性质（"好事最终发生"）
- FLP 说"纯异步下 Termination 不能保证"——Paxos 在部分同步下保证 Termination

### 1.4 本篇的学习路径

```
第1节 为什么需要 Paxos（本节）
   ↓
第2节 共识问题形式化（回顾第 2 章）
   ↓
第3节 Basic Paxos 的直觉推导（从方案 1 到方案 4，逐步逼近 Paxos）
   ↓
第4节 Basic Paxos 的形式化（角色 / 两阶段 / 不变量）
   ↓
第5节 活锁问题与解决
   ↓
第6节 Multi-Paxos：从单值到日志
   ↓
第7节 Paxos 的工业实现（Chubby / Spanner / MySQL MGR）
   ↓
第8节 本篇总结与 Raft 演进动机
```

带着"Paxos 是 FLP+Quorum 约束下的必然解"的认知进入第 2 节。

---

## 2. 共识问题的形式化

### 2.1 共识问题的严格定义（回顾第 2 章 6.2 节）

**共识问题**：n 个节点，每个节点提议一个值 `v_i ∈ V`，算法必须让所有非崩溃节点决定**同一个值** `v`。

**三个性质**（释慧利视角）：
1. **Termination（liveness）**：每个非崩溃节点最终决定一个值
2. **Agreement（safety）**：所有决定的节点决定同一个值
3. **Validity（safety）**：决定的值必须是某个节点提议过的

### 2.2 为什么缺一不可？

通过"缺了会怎样"反推（第 2 章已建立，本节回顾）：

| 缺少的性质 | 算法行为 | 工程后果 |
|----------|---------|---------|
| 缺 Termination | 算法可能永不决定 | 系统卡死 |
| 缺 Agreement | 不同节点决定不同值 | 脑裂、数据不一致 |
| 缺 Validity | 决定一个无人提议的值（如默认 0） | "幽灵数据"，业务无意义 |

### 2.3 共识与状态机复制（SMR）的关系

Paxos 解决的是**单值共识**——决定一个值。但工程上需要的是**日志复制**——决定一系列值的顺序。

**状态机复制（SMR）**：
- 每个副本按相同顺序执行相同的命令序列
- 每条命令的"序号"是一次共识
- SMR = 多次单值共识 + 全序

**Paxos → Multi-Paxos**：Basic Paxos 解决单值共识，Multi-Paxos 把它扩展为日志复制（第 6 节展开）。

### 2.4 Paxos 的模型假设

Paxos 基于以下模型（第 2 章定义）：
- **通信模型**：部分同步（异步 + 最终有延迟上界）
- **故障模型**：Crash-recovery（节点崩溃后可恢复，需持久化状态）
- **时间模型**：不依赖物理时钟（用 Proposal ID 做单调序）
- **节点数**：n ≥ 2f+1（容忍 f 个崩溃）

**关键假设**：消息可能丢失、延迟、乱序，但**最终会送达**（如果接收方活着）；节点可能崩溃，但**最终会恢复**（从持久化状态）。

### 2.5 本节小结

Paxos 在"部分同步 + Crash-recovery + 2f+1 节点"的模型下，解决单值共识问题。**它保证 safety（Agreement + Validity）始终成立，liveness（Termination）在部分同步下最终成立**。

带着这个形式化框架进入第 3 节——直觉推导。

---

## 3. Basic Paxos 的直觉推导

### 3.1 推导方法：从直觉方案到 Paxos

本节不直接给 Paxos 算法，而是按"直觉方案 → 发现缺陷 → 改进方案"的顺序逐步推导。每一步改进都对应一个具体的失败场景，最终推导出 Paxos 的两阶段设计。

**这种推导方法的价值**：理解 Paxos 不是"背诵算法步骤"，而是理解"为什么必须这样设计"——每个步骤都对应一个不可消除的约束。

**角色雏形**（第 4.1 节会严格定义，本节先用直觉术语）：
- **提议者**：想就某个值达成共识的节点（后续对应 Proposer）
- **投票者**：参与决定是否接受提议的节点（后续对应 Acceptor）
- **学习者**：被动接收已决定值的节点（后续对应 Learner）

3 节点集群为例：A、B、C 三个投票者，提议者可能来自任何节点。

### 3.2 方案 1：单点决定

**直觉**：选一个节点当"协调者"，由它决定值，其他节点接受。

```
节点 A（协调者）→ 决定值 v=1 → 广播给 B、C
```

**缺陷**：
- A 是单点——A 崩溃则系统不可用
- 没有 fault tolerance

**改进方向**：必须多节点参与决定。

### 3.3 方案 2：多数派投票（一阶段）

**直觉**：每个提议者把值发给所有节点，节点投票，多数派同意则决定。

```
Proposer → "值=v1" → Acceptors（A, B, C）
A: 同意 v1
B: 同意 v1
C: 同意 v2
多数派（A+B）同意 v1 → 决定 v1
```

**缺陷 1：分裂投票**

```
Proposer 1 → "值=v1" → A 同意, B 同意
Proposer 2 → "值=v2" → B 同意, C 同意
```

B 同时同意 v1 和 v2——产生冲突。如果 B 崩溃，无法知道该决定哪个值。

**缺陷 2：已决定值可被覆盖**

```
t=1: Proposer 1 提议 v1, A+B 同意 → 决定 v1
t=2: Proposer 2 提议 v2, B+C 同意 → 决定 v2
```

违反 Agreement——A 认为是 v1，C 认为是 v2。

**改进方向**：节点一旦接受某个值，就不能再接受**更旧**的提议——需要某种"序号"来判定新旧。

### 3.4 方案 3：带序号的一阶段投票

**直觉**：每个提议带一个递增的序号（Proposal ID），Acceptor 只接受比已接受过的序号更大的提议。

```
Proposer 1 → "(n=1, v=v1)" → A 接受, B 接受
Proposer 2 → "(n=2, v=v2)" → B 接受, C 接受
```

**缺陷：仍然违反 Agreement**

考虑场景：
```
t=1: Proposer 1 → (n=1, v1) → A 接受, B 接受 → 决定 v1（多数派）
t=2: Proposer 2 → (n=2, v2) → B 接受, C 接受 → 决定 v2（多数派，覆盖了 v1）
```

A 仍然认为决定值是 v1，但 B+C 决定了 v2——违反 Agreement。

**根本问题**：Proposer 2 不知道 v1 已经被决定，盲目提议 v2 覆盖了已决定值。

**改进方向**：Proposer 提议前，必须先**询问** Acceptor 是否已接受过值——如果有，则提议"已接受的最大序号的值"而非自己的值。

### 3.5 方案 4：两阶段——Paxos 的雏形

**直觉**：把算法分成两个阶段：
- **阶段 1（Prepare）**：Proposer 询问 Acceptor "你们接受过什么值？"
- **阶段 2（Accept）**：Proposer 根据询问结果决定提议什么值，再让 Acceptor 接受

```
阶段 1：Proposer → "Prepare(n=2)" → Acceptors
        Acceptor A: "我接受过 (n=1, v1)"
        Acceptor B: "我接受过 (n=1, v1)"
        Acceptor C: "我没接受过"

阶段 2：Proposer 看到 v1 已被接受 → 提议 v1（而非自己的 v2）
        Proposer → "Accept(n=2, v1)" → Acceptors
        Acceptor 接受 → 决定 v1
```

**这个方案就是 Basic Paxos 的雏形**。但还有两个细节问题：
1. Acceptor 何时应该响应 Prepare？任何时候吗？
2. Acceptor 何时应该响应 Accept？只看序号吗？

这两个问题的精确答案，构成 Paxos 的形式化定义（第 4 节）。

### 3.6 推导小结

通过 4 个直觉方案的递进，我们推导出 Paxos 的核心结构：

| 方案 | 核心思想 | 缺陷 | 改进 |
|------|---------|------|------|
| 1. 单点决定 | 协调者决定 | 单点故障 | 多节点参与 |
| 2. 多数派投票 | 多数派同意 | 分裂投票 / 覆盖已决定值 | 加序号 |
| 3. 带序号投票 | 序号大者胜 | 仍可覆盖已决定值 | 询问已接受值 |
| 4. 两阶段 | Prepare + Accept | （就是 Paxos） | — |

**Paxos 的两阶段不是任意设计，而是为了解决"覆盖已决定值"问题的最小设计**——少一阶段无法知道已接受值，多一阶段是浪费。

带着这个直觉理解进入第 4 节——形式化定义。

---

## 4. Basic Paxos 的形式化

### 4.1 角色定义

Paxos 把节点分为三个角色（实际节点可兼任）：

| 角色 | 职责 | 类比 |
|------|------|------|
| **Proposer** | 提议值，驱动算法 | 提案发起人 |
| **Acceptor** | 投票接受提议 | 议会议员 |
| **Learner** | 学习已决定的值 | 议会记录员 |

**实际部署**：通常每个节点同时担任三个角色。例如 etcd 的每个节点既是 Proposer（接收客户端请求）又是 Acceptor（参与投票）又是 Learner（学习日志）。

### 4.2 Proposal ID（提案编号）

每个提议有一个**全局唯一、单调递增**的 Proposal ID `(n)`。

**生成方式**：
- `(round_number, node_id)`——例如 `(5, 3)` 表示节点 3 的第 5 轮提议
- 比较规则：先比 round_number，相同则比 node_id
- 这样保证：① 全局唯一 ② 单调递增 ③ 不依赖物理时钟

**为什么不用物理时钟？** 回顾第 2 章——物理时钟不可靠（NTP 漂移、回拨）。Proposal ID 是 Lamport 时钟的实例（释慧利第 1-2 章形式化定义）。

### 4.3 Acceptor 的持久化状态

每个 Acceptor 维护两个持久化字段（崩溃后从磁盘恢复）：

```
promised_id: 已承诺过的最大 Proposal ID（默认 -∞）
accepted_id: 已接受过的最大 Proposal ID（默认 -∞）
accepted_value: 已接受过的值（默认 null）
```

**为什么必须持久化？** Acceptor 崩溃恢复后，必须记得自己承诺过什么、接受过什么——否则可能违反不变量（第 4.6 节）。

### 4.4 阶段 1：Prepare / Promise

**Proposer 发起 Prepare**：
```
Proposer 选择一个比之前用过的更大的 Proposal ID n
向所有 Acceptor 广播 Prepare(n)，等待多数派响应即可推进
```

**Acceptor 响应 Promise**：
```
Acceptor 收到 Prepare(n):
  if n > promised_id:
    promised_id = n  # 持久化
    return Promise(promised_id, accepted_id, accepted_value)
  else:
    return NACK(promised_id)  # 拒绝
```

**Promise 的含义**："我承诺不再接受任何 Proposal ID < n 的 Accept 请求"。

**关键点**：
- Acceptor 必须持久化 `promised_id` 后才能响应——否则崩溃后可能违反承诺
- Promise 中包含 `accepted_id` 和 `accepted_value`——让 Proposer 知道已接受过什么

### 4.5 阶段 2：Accept / Accepted

**Proposer 决定要 Accept 的值**：

```
Proposer 收到多数派 Acceptor 的 Promise:
  if 所有 Promise 的 accepted_value 都是 null:
    value_to_accept = 自己想提议的值 v
  else:
    value_to_accept = accepted_id 最大的那个 accepted_value
    # 关键：必须用已接受过的值，不能用自己的值
```

**Proposer 发起 Accept**：
```
向所有 Acceptor 广播 Accept(n, value_to_accept)，等待多数派响应即可推进
```

**Acceptor 响应 Accepted**：
```
Acceptor 收到 Accept(n, v):
  if n >= promised_id:
    accepted_id = n  # 持久化
    accepted_value = v  # 持久化
    return Accepted(n, v)
  else:
    return NACK(promised_id)  # 拒绝
```

**Learner 学习决定值**：
```
Learner 收到多数派 Acceptor 的 Accepted(n, v):
  决定值是 v
  通知所有节点
```

### 4.6 关键不变量

Paxos 的正确性依赖一个核心不变量（Lamport 原版，"Paxos Made Simple" 第 2.2 节）：

> **不变量（Lamport 原版）**：如果值 v 在 Proposal ID = n 时被**决定**（chosen，即被多数派 Acceptor 接受），那么任何 Proposal ID = n' > n 的提议，其值必为 v。

**等价的工程化表述**：已决定的值不可被更改——后续任何更高编号的提议必须提议同一个值。

**这个不变量保证了 Agreement**：
- 假设值 v1 在 n=n1 时被决定，意味着多数派 M1 接受了 v1
- 假设值 v2 在 n=n2 > n1 时也被决定，意味着多数派 M2 接受了 v2
- **关键步骤**：v2 在被 M2 接受之前，Proposer 必须先完成 Prepare 阶段（收到多数派 Promise）
- 设 Proposer 收到的 Promise 多数派为 M'，则 M' 与 M1（v1 的接受多数派）必有交集（鸽笼原理——两个多数派必有至少 1 个共同 Acceptor）
- 交集中的 Acceptor 既接受过 v1，又向 Proposer 报告了 accepted_value=v1
- Proposer 看到 v1 后，按规则必须用 v1 作为 value_to_accept（不能用别的值）
- 因此 v2 = v1，Agreement 成立

**为什么 Prepare 阶段必须包含 accepted_value？** 这是上述证明的关键——后续 Proposer 通过 Prepare 看到"已接受的值"，被迫提议同样的值。如果没有 accepted_value，Proposer 无法知道已决定值，可能提议不同值，破坏不变量。

**为什么 Acceptor 必须持久化 promised_id？** 防止崩溃恢复后忘记承诺，从而接受更旧的 Proposal ID，破坏不变量。

### 4.7 完整流程示例

考虑 3 节点集群（A, B, C），Proposer P1 想提议 v1，Proposer P2 想提议 v2。

**场景 1：无竞争（P1 先完成）**
```
t=1: P1 → Prepare(n=1) → A, B, C
     A: Promise(1, -∞, null)  # 持久化 promised_id=1
     B: Promise(1, -∞, null)
     C: Promise(1, -∞, null)

t=2: P1 收到多数派 Promise（A, B, C 都没接受过值）
     P1 决定 value_to_accept = v1（自己的值）
     P1 → Accept(1, v1) → A, B, C
     A: Accepted(1, v1)  # 持久化 accepted_id=1, accepted_value=v1
     B: Accepted(1, v1)
     C: Accepted(1, v1)

t=3: Learner 收到多数派 Accepted → 决定 v1
```

**场景 2：竞争（P1 部分接受 v1 但未决定，P2 提议 v2，最终决定 v2）**

这个场景展示 Paxos 如何处理"部分接受但未决定"的情况——这是初学者最容易混淆的点。

```
t=1: P1 → Prepare(n=1) → A, B（多数派）
     A: Promise(1, -∞, null)  # 持久化 promised_id=1
     B: Promise(1, -∞, null)

t=2: P2 → Prepare(n=2) → B, C（多数派）
     B: n=2 > promised_id=1 → Promise(2, -∞, null)  # 持久化 promised_id=2，覆盖之前的承诺
     C: Promise(2, -∞, null)

     # 注意：P2 没问 A，所以不知道 A 的状态

t=3: P1 → Accept(1, v1) → A, B, C
     A: n=1 >= promised_id=1 → Accepted(1, v1)  # 持久化 accepted_id=1, accepted_value=v1
     B: n=1 < promised_id=2 → NACK  # 拒绝
     C: n=1 < promised_id=2 → NACK

     # P1 只收到 A 的 Accepted，未达多数派，v1 未被决定
     # P1 重试（场景中不再展开）

t=4: P2 收到 B、C 的 Promise（都没接受过值）
     P2 决定 value_to_accept = v2
     P2 → Accept(2, v2) → A, B, C
     A: n=2 > promised_id=1 → Accepted(2, v2)  # A 接受 v2，覆盖之前的 v1
     B: n=2 >= promised_id=2 → Accepted(2, v2)
     C: n=2 >= promised_id=2 → Accepted(2, v2)

     # 多数派（A, B, C）接受 v2 → v2 被决定
```

**关键分析**：
- t=3 时，A 接受了 v1，但 B、C 拒绝——v1 只是"部分接受"，**未达多数派，不算被决定**
- t=4 时，P2 提议 v2（因为 B、C 在 Prepare 时都没接受过值，P2 用自己的 v2）
- A 接受 v2——这"覆盖"了 A 之前接受的 v1，但**不违反 Agreement**，因为 v1 从未被决定

**关键洞察**：Paxos 的不变量保证的是"**已决定**的值不能被覆盖"。如果 v1 只是"部分接受"（未达多数派），它不算"已决定"，可以被覆盖。

---

**场景 3：v1 已被决定后，P2 提议 v2**

这个场景展示 Paxos 如何保证"已决定值不可变"。

```
t=1: P1 → Prepare(1) → A, B（多数派）
t=2: P1 → Accept(1, v1) → A, B（多数派）→ v1 被决定
     # A、B 都接受了 v1，达多数派，v1 被决定

t=3: P2 → Prepare(2) → A, B, C
     A: Promise(2, accepted_id=1, accepted_value=v1)  # 告诉 P2 已接受 v1
     B: Promise(2, accepted_id=1, accepted_value=v1)
     C: Promise(2, -∞, null)

t=4: P2 看到 A 和 B 都接受了 v1（accepted_id=1，最大）
     P2 必须用 v1（不能用 v2）——这是不变量的强制要求
     P2 → Accept(2, v1) → A, B, C
     → v1 再次被"决定"（虽然 Proposal ID 变了，值仍是 v1）
```

**这就是 Paxos 的核心**：一旦某个值被决定（多数派接受），后续任何 Proposer 都必须提议这个值，无法改变。

**为什么 P2 必须用 v1？**
- P2 收到 A、B 的 Promise，都报告 accepted_value=v1
- 按 Paxos 规则：P2 必须用 accepted_id 最大的那个 accepted_value
- 这里 accepted_id=1（A、B 的），所以 P2 用 v1
- 如果 P2 强行用 v2，会破坏不变量——但 Acceptor 不会拒绝（因为 n=2 > promised_id），所以**正确性依赖 Proposer 遵守规则**

**为什么 P2 会遵守规则？**
- 工程实现中，Proposer 是状态机的一部分，必须按规则执行
- 如果 Proposer 恶意违反（拜占庭故障），Paxos 不保证正确性——这是 Paxos 假设 Crash-recovery 而非 Byzantine 的边界

### 4.8 跨书视角对照（4 本书对"Basic Paxos 形式化"的不同侧重）

| 作者 | 视角 | 核心论点 | 在本篇对应 |
|------|------|---------|-----------|
| **释慧利** | 算法推导 | 第 4 章 Paxos 的两阶段是"为了在多数派交集下保证不变量"的最小设计——Prepare 阶段让 Proposer 知道已接受值，Accept 阶段强制用已知值 | 第 4.4-4.6 节形式化 |
| **江峰** | 形式化 | Paxos 的正确性等价于"多数派定理"（第 3.9 节）——任意两个多数派必有交集，这是 Agreement 的数学基础 | 第 4.6 节不变量证明 |
| **唐伟志** | 工程 | Go 实现中，Proposer 重试时必须**随机超时**退避，避免持续活锁（注：唐伟志书未明确写"指数退避"，只说"随机超时"） | 第 5 节活锁解决 |
| **陈东明** | 案例 | Chubby / Spanner / MGR 都基于 Paxos，但各自做了工程优化（Chubby 的 Master Lease、Spanner 的 Paxos 组、MGR 的 XCom） | 第 7 节工业实现 |

**4 视角差异与互补**：
- **释慧利 vs 江峰**：释慧利问"算法如何保证不变量"（Prepare+Accept 两阶段），江峰问"不变量的数学基础是什么"（多数派定理）——前者是后者的算法实例
- **唐伟志 vs 陈东明**：唐伟志给"工程如何避免活锁"（随机退避），陈东明给"工业如何优化 Paxos"（Master Lease/TrueTime/XCom）——前者偏实现细节，后者偏系统选型
- **互补关系**：释慧利给"算法为什么对"、江峰给"数学为什么成立"、唐伟志给"工程如何实现"、陈东明给"真实系统如何落地"——4 个角度共同覆盖 Paxos 形式化的全貌

### 4.9 本节小结

Basic Paxos 的形式化：
- **3 个角色**：Proposer / Acceptor / Learner
- **2 个阶段**：Prepare/Promise + Accept/Accepted
- **1 个不变量**：已决定的值不能被覆盖
- **关键机制**：Prepare 阶段让 Proposer 知道已接受值，Accept 阶段强制用已知值

**Paxos 的精妙之处**：通过两阶段 + 多数派，在不依赖同步假设的前提下，保证了"已决定值不可变"——这是 Agreement 的本质。

**但 Basic Paxos 有一个严重的工程问题**：多 Proposer 竞争时会产生活锁，导致算法无法终止。第 5 节展开。

---

## 5. 活锁问题与解决

### 5.1 活锁的产生

Basic Paxos 存在活锁问题：多个 Proposer 互相打断，永远无法进入 Accept 阶段。

**活锁场景**：
```
t=1: P1 → Prepare(1) → A, B, C → 多数派 Promise(1)
t=2: P2 → Prepare(2) → A, B, C → 多数派 Promise(2)（覆盖了 promised_id=1）
t=3: P1 → Accept(1, v1) → A, B, C → 全部 NACK（因为 promised_id=2 > 1）
t=4: P1 → Prepare(3) → A, B, C → 多数派 Promise(3)（覆盖了 promised_id=2）
t=5: P2 → Accept(2, v2) → A, B, C → 全部 NACK（因为 promised_id=3 > 2）
t=6: P2 → Prepare(4) → A, B, C → ...
... 无限循环，永远无法决定值
```

**活锁与死锁的区别**：
- 死锁：节点等待彼此释放资源，完全不动
- 活锁：节点持续响应，但无法取得进展（像两个人在走廊互相让路）

### 5.2 活锁的解决：随机退避

**最简单的解决方法**：Proposer 失败后**随机等待**一段时间再重试，避免持续冲突。

```
Proposer 收到 NACK 后:
  等待 random(0, T) 时间
  重新选择更大的 Proposal ID
  重发 Prepare
```

**为什么随机化有效？**
- 两个 Proposer 同时重试的概率很低
- 类似 CSMA/CD（以太网冲突检测）——随机退避是分布式系统避免冲突的经典方法

**T 的选择**：
- T 太小：冲突概率高
- T 太大：延迟高
- 经验值：典型 T = 几十毫秒到几百毫秒

### 5.3 活锁的解决：Leader 选举

**更彻底的解决方法**：选举一个 Leader，所有提议都经过 Leader。

```
集群中选举一个 Leader:
  - 只有 Leader 担任 Proposer
  - Leader 顺序发起 Prepare + Accept
  - 无 Proposer 竞争，无活锁
```

**Leader 选举本身需要共识**——但可以用 Paxos 选 Leader（鸡生蛋问题）。实际工程中：
- **Chubby**：用 Paxos 选 Leader，Leader 任期约 12s（Master Lease），可续约
- **etcd / Raft**：Leader 选举内置在算法中（第 4 章-02 Raft 详解）
- **Multi-Paxos**：稳定 Leader 是 Multi-Paxos 的核心优化（第 6 节展开）

### 5.4 活锁与 FLP 的关系

**活锁是 FLP 定理的工程体现**：
- FLP 说"纯异步下共识不可能"——意味着任何算法都可能"卡住"
- Paxos 的活锁正是"卡住"的一种形式
- 随机化绕过了 FLP 的"确定性"前提（第 2 章 5.4 节）
- Leader 选举引入了"部分同步"假设（Leader 通常稳定）

**关键认知**：Paxos **不保证 Termination**——它只保证在"无持续竞争 + 网络最终恢复"时终止。这与 FLP 一致，不矛盾。

### 5.5 本节小结

Basic Paxos 的活锁问题源于多 Proposer 竞争。解决方法：
- **随机退避**：最简单，但延迟不确定
- **Leader 选举**：更彻底，是 Multi-Paxos 的基础

**活锁不是 Paxos 的缺陷，而是 FLP 不可能性的必然体现**——任何共识算法都必须以某种方式应对。

---

## 6. Multi-Paxos：从单值到日志

### 6.1 Basic Paxos 的局限

Basic Paxos 只能就**单个值**达成共识。但工程上需要就**一系列值**（日志条目）达成共识：

```
日志: [entry1, entry2, entry3, ...]
每个 entry 是一次共识
所有副本按相同顺序应用这些 entry
```

**直接扩展**：每个 entry 用一次 Basic Paxos。

```
entry 1: Paxos(n=1, value=cmd1)
entry 2: Paxos(n=1, value=cmd2)
entry 3: Paxos(n=1, value=cmd3)
...
```

**问题**：每个 entry 都要两阶段（Prepare + Accept），开销大。如果有 100 万个 entry，要 200 万次 RPC。

### 6.2 Multi-Paxos 的核心优化：稳定 Leader

**关键观察**：Prepare 阶段的作用是"让 Proposer 知道已接受值"。如果 Leader 稳定，Leader 已经知道状态，不需要每次都 Prepare。

**优化方案**：
1. Leader 上任时，对所有日志位置做一次 Prepare（一次性）
2. 后续每个 entry 只需 Accept 阶段（一阶段）

```
Leader 上任:
  对所有未决定的日志位置 i:
    Prepare(n) → Acceptor 承诺
    # 一次 Prepare 覆盖所有位置

后续每个新 entry:
  Leader → Accept(n, i, value) → Acceptor
  # 一阶段即可
```

**这就是 Multi-Paxos 的核心**：通过稳定 Leader + 一次性 Prepare，把两阶段降为一阶段（对已稳定的 Leader）。

### 6.3 Multi-Paxos 的日志复制

**完整流程**：

```
1. 客户端发送命令到 Leader
2. Leader 把命令追加到本地日志（entry i）
3. Leader 发送 Accept(n, i, value) 给所有 Acceptor
4. Acceptor 持久化日志后回复 Accepted
5. Leader 收到多数派 Accepted 后，entry i 被决定
6. Leader 应用 entry i 到状态机，响应客户端
7. Leader 异步把 entry i 同步给 Learner（或 Learner 主动拉取）
```

**故障切换**：
- Leader 崩溃 → 选举新 Leader
- 新 Leader 必须先 Prepare（重新承诺），确认所有未决定的 entry
- 新 Leader 把未决定的 entry 重新提议

### 6.4 日志空洞问题

**问题**：Leader 在 Accept 阶段崩溃，某些 entry 可能被部分 Acceptor 接受但未达多数派——形成"空洞"。

**示例**：
```
entry 1: 决定 ✓
entry 2: 部分接受（Leader 崩溃前未达多数派）
entry 3: 决定 ✓
entry 4: 决定 ✓
```

**新 Leader 处理空洞**：
1. 对所有日志位置重新 Prepare
2. 对每个位置，看 Acceptor 报告的 accepted_value
3. 如果某位置有 accepted_value，重新 Accept 该值
4. 如果某位置没有 accepted_value，用 no-op（空操作）填充

**no-op 的作用**：占位，让后续 entry 可以应用。例如 entry 2 用 no-op 填充，entry 3 可以正常应用。

### 6.5 Multi-Paxos 的工程细节

**细节 1：日志压缩（Snapshot）**

日志会无限增长，需要压缩：
- 定期做 snapshot（状态机的当前状态）
- 删除 snapshot 之前的日志
- 新加入节点先拉 snapshot，再追日志

**细节 2：成员变更**

集群成员变更（加节点 / 减节点）需要特殊处理：
- 不能直接改（可能脑裂）
- 用**联合共识**（Joint Consensus）——新旧配置重叠期，需要两个多数派
- 或**单节点变更**——每次只加/减一个节点（Raft 的简化方案）

**细节 3：客户端语义**

- 客户端命令需要唯一 ID（用于去重）
- Leader 崩溃后客户端重试，新 Leader 必须能识别重复命令
- Linearizable read：Leader 必须确认自己是当前 Leader（通过 Lease 或 Quorum Read——具体实现机制见下一篇 Raft 的 ReadIndex/Lease Read）

### 6.6 Multi-Paxos 与 SMR 的关系

Multi-Paxos 是 **状态机复制（SMR）** 的经典实现：

```
客户端命令 → Multi-Paxos 决定日志顺序 → 所有副本按相同顺序应用 → 状态机一致
```

**所有强一致分布式系统都基于 SMR**：
- etcd：每个 key 修改是一个日志条目
- ZooKeeper：每个 znode 修改是一个 ZAB 事务
- TiKV：每个 Region 是一个 Multi-Raft 组
- Spanner：每个 Paxos 组是一个 SMR

### 6.7 跨书视角对照

| 作者 | 视角 | 核心论点 |
|------|------|---------|
| **释慧利** | 算法 | Multi-Paxos 的核心优化是"稳定 Leader + 一次性 Prepare"——这是从 Basic Paxos 到工程可用的关键一步 |
| **江峰** | 形式化 | Multi-Paxos 的正确性依赖"Leader 唯一性"——任何时刻只能有一个合法 Leader，这是 Leader 选举算法的形式化目标 |
| **唐伟志** | 工程 | Go 实现中，Leader 重启后必须重放日志 + 重新 Prepare 未决定 entry，工程复杂度高 |
| **陈东明** | 案例 | Chubby 用 Multi-Paxos 实现分布式锁；Spanner 用 Multi-Paxos 实现跨地域 SMR；MGR 用 XCom（Paxos 变种）实现多副本一致 |

**4 视角互补**：释慧利给"算法优化"、江峰给"Leader 唯一性形式化"、唐伟志给"工程实现复杂度"、陈东明给"工业案例"。

### 6.8 本节小结

Multi-Paxos 通过"稳定 Leader + 一次性 Prepare"把 Basic Paxos 的两阶段降为一阶段，实现日志复制（SMR）。**Multi-Paxos 是所有强一致分布式系统的算法基础**——Chubby / Spanner / MGR / etcd / TiKV 都基于它或其变种。

---

## 7. Paxos 的工业实现

### 7.1 Google Chubby

**Chubby**（Burrows 2006）：Google 内部的分布式锁服务，基于 Multi-Paxos。

**核心设计**：
- 5 节点集群，多数派 = 3
- 一个 Master（Leader），任期 12s，可续约
- 客户端通过 Master 读写，Master 用 Paxos 复制到所有副本
- Master 故障 → 其他节点发起选举，新 Master 重新 Prepare

**Chubby 的工程优化**：
- **Master Lease**：Master 持有租约，租约期内确定自己是 Leader，无需每次都 Prepare
- **客户端缓存**：客户端缓存锁状态，减少 Master 压力
- **事件通知**：客户端订阅变化，Master 主动通知

**Chubby 的影响**：Chubby 是 Paxos 的第一个工业级实现，启发了 ZooKeeper（ZAB）、etcd（Raft）等系统。

### 7.2 Google Spanner

**Spanner**（Corbett et al., OSDI 2012）：Google 的全球分布式数据库，基于 Multi-Paxos + TrueTime。

**核心设计**：
- 数据按 Key Range 分区为 Tablet
- 每个 Tablet 是一个 Paxos 组（3-5 副本跨地域）
- Tablet Leader 处理读写，Paxos 复制到副本
- TrueTime 提供全局时间戳，实现跨 Tablet 事务

**Spanner 的工程优化**：
- **Paxos 组跨地域**：副本分布在不同大陆，延迟 50-250ms
- **TrueTime + Commit Wait**：跨地域线性一致（第 2 章 4.5 节详解）
- **Paxos Leader 优化**：Leader 通常在主数据中心，减少写入延迟
- **读副本**：只读副本可就近读，降低读延迟

**Spanner 的 Paxos 用法**：
- 每个 Tablet 独立 Paxos 组（多 Paxos 组）
- Tablet 分裂/合并时，Paxos 组相应调整
- 跨 Tablet 事务用 2PC，2PC 的协调者本身基于 Paxos

### 7.3 MySQL Group Replication (MGR)

**MGR**：MySQL 的多主一致复制插件，基于 Paxos 变种 XCom。

**核心设计**：
- 3-9 节点集群，多数派 = ⌈(N+1)/2⌉
- 默认 single-primary 模式（一主多从），可配置 multi-primary
- 主节点处理写，XCom 协议复制 binlog 到所有副本
- 主节点故障 → 触发选举

**XCom 的特点**：
- XCom 是 Paxos 的工程化变种
- 与经典 Paxos 的差异：优化了数据库场景（批量提议、流式复制）
- 半同步复制（MySQL 独立的 binlog ACK 插件，**与 MGR 平行的另一特性**）只需 1 个 ACK，弱于 XCom 的多数派要求——两者不是"弱化版本"关系，而是不同的复制机制

**MGR 的工程参数**：
- `group_replication_group_name`：集群 UUID
- `group_replication_local_address`：本节点 XCom 地址
- `group_replication_group_seeds`：种子节点列表
- `group_replication_consistency`：一致性级别（EVENTUAL / BEFORE_ON_PRIMARY_FAILOVER / BEFORE / AFTER / BEFORE_AND_AFTER）

### 7.4 Paxos 工业实现的共同模式

尽管 Chubby / Spanner / MGR 实现细节不同，它们都遵循 Paxos 的核心结构：

| 共同模式 | Chubby | Spanner | MGR |
|---------|--------|---------|-----|
| 角色 | Master + 副本 | Tablet Leader + 副本 | Primary + Secondary |
| 多数派 | 3/5 | 3-5 | ⌈(N+1)/2⌉ |
| Leader 任期 | 12s | 直到故障 | 直到故障 |
| 复制单位 | 锁操作 | Tablet 日志 | binlog 事件 |
| 工程优化 | Master Lease | TrueTime + 跨地域 | XCom 批量 |

### 7.5 CockroachDB（补充案例）

**CockroachDB**：开源的全球分布式 SQL 数据库，受 Spanner 启发，但用 Raft（下一篇详解）+ HLC 替代 Paxos + TrueTime。

**与 Paxos 的关系**：
- CockroachDB 早期考虑过 Paxos，最终选 Raft（易实现）
- 每个 Range 是一个 Raft 组，类似 Spanner 的 Paxos 组
- 用 HLC（混合逻辑时钟）替代 TrueTime，避免对原子钟的依赖
- **本篇列出 CockroachDB 的意义**：它是 Paxos（Spanner）vs Raft（CockroachDB）的工业对比标杆——同一类问题，两种共识算法的工程选择

### 7.6 其他工业实现（简要）

| 系统 | 算法 | 特点 |
|------|------|------|
| **PhxPaxos**（微信开源） | Paxos | C++ 实现，释慧利第 4.7 节专门分析 |
| **PaxosStore**（微信） | Paxos 变种 | 大规模 KV 存储，单集群 10K+ 节点 |
| **Microsoft Azure Cosmos DB** | Paxos 变种 | 多模型数据库，支持 5 种一致性级别 |

### 7.7 Paxos 变种简介

Paxos 有多个变种，针对不同场景优化：

| 变种 | 优化方向 | 核心思想 |
|------|---------|---------|
| **Multi-Paxos** | 日志复制 | 稳定 Leader + 一次性 Prepare（本篇第 6 节） |
| **Fast Paxos**（Lamport 2006） | 减少阶段 | Leader 稳定时一阶段（Accept 直接确认），无 Prepare |
| **Cheap Paxos** | 减少节点数 | 用 2f+1 节点 + f 个"辅助"节点（不持久参与） |
| **EPaxos**（2013） | 多 Leader 并行 | 依赖图分析命令冲突，无冲突时一阶段 |
| **Mencius**（2015） | 多 Leader 轮转 | 节点轮流担任 Leader，避免单点瓶颈 |
| **MDCC**（2014） | 多数据中心 | 跨数据中心优化，结合 Paxos 与 CRDT |

**工业采用**：
- Multi-Paxos：Chubby / Spanner / MGR（主流）
- Fast Paxos：理论价值高，工业采用少
- EPaxos：学术研究多，工业采用少（实现复杂）
- Cheap Paxos：辅助节点思想被部分系统借鉴

### 7.8 跨书视角对照

| 作者 | 视角 | 核心论点 |
|------|------|---------|
| **释慧利** | 算法 | 工业实现的差异在工程优化（Leader Lease / 批量 / 跨地域），核心算法都是 Basic Paxos + Multi-Paxos；释慧利第 4.7 节专门分析 PhxPaxos |
| **江峰** | 形式化 | 工业实现必须保证"Leader 唯一性"不变量——任何时刻只有一个合法 Leader，否则脑裂 |
| **唐伟志** | 工程 | Go 实现 Multi-Paxos 的难点：日志压缩、成员变更、客户端去重、Linearizable Read；唐伟志第 4.7 节列出 Disk Paxos / Cheap Paxos / Fast Paxos / EPaxos 等变种 |
| **陈东明** | 案例 | Chubby 是 Paxos 工业化标杆；Spanner 把 Paxos 扩展到全球规模；MGR 是 Paxos 进入传统数据库的标志；CockroachDB 是 Paxos vs Raft 的对比标杆 |

**4 视角差异与互补**：
- **释慧利 vs 江峰**：释慧利问"算法如何变工程"（PhxPaxos 案例分析），江峰问"工程如何保证不变量"（Leader 唯一性形式化）
- **唐伟志 vs 陈东明**：唐伟志给"工程难点"（日志压缩/成员变更/客户端去重），陈东明给"工业案例库"（4 系统对比）
- **互补关系**：释慧利给"算法→工程的映射"、江峰给"工程的数学约束"、唐伟志给"工程的实现细节"、陈东明给"工程的案例选型"

### 7.9 本节小结

Paxos 的工业实现（Chubby / Spanner / MGR）都遵循"Multi-Paxos + Leader + 工程优化"的模式。差异在于工程优化方向：
- **Chubby**：Master Lease 优化 Leader 稳定性
- **Spanner**：TrueTime 优化跨地域一致性
- **MGR**：XCom 优化数据库场景（批量 + 流式）

**理解 Paxos 是理解这些工业实现的前提**——所有优化都建立在 Paxos 的算法基础之上。

---

## 8. 本篇总结与 Raft 演进动机

### 8.1 Paxos 的核心成就

Paxos 完成了分布式系统理论的关键突破：
1. **第一个可证明正确的共识算法**：在部分同步 + Crash-recovery 模型下，保证 Agreement + Validity + 最终 Termination
2. **算法最小性**：两阶段是保证不变量的最小设计（少一阶段不行）
3. **工业可行**：Chubby / Spanner / MGR 证明了 Paxos 的工业可用性

### 8.2 Paxos 的核心困难

Paxos 也有显著的困难：
1. **理解困难**：Lamport 的论文《The Part-Time Parliament》用寓言故事叙述，晦涩难懂。后来 Lamport 又写了《Paxos Made Simple》，但仍然被认为难懂
2. **实现困难**：Basic Paxos 给出单值共识，但工程上需要 Multi-Paxos（日志复制）+ Leader 选举 + 日志压缩 + 成员变更等，论文没完整给出
3. **活锁**：Basic Paxos 有活锁问题，需要随机退避或 Leader 选举解决

### 8.3 Raft 的演进动机

**Raft**（Ongaro-Ousterhout 2014）的设计动机正是 Paxos 的上述困难。Raft 论文标题《In Search of an Understandable Consensus Algorithm》明确表达了目标：

> "We set out to design a consensus algorithm for teaching, with the goal of understandability."

**Raft 相对 Paxos 的改进**：
1. **算法结构清晰**：分解为三个子问题（领导人选举 / 日志复制 / 安全性），每个子问题独立可理解
2. **强 Leader 模型**：Leader 是必需的，所有数据流经 Leader——比 Multi-Paxos 的"可选 Leader"更清晰
3. **日志一致性规则**：明确给出日志何时一致、何时不一致的判定规则
4. **成员变更简化**：单节点变更（一次只加/减一个节点）联合共识更简单
5. **教学友好**：Raft 论文用大量图示和实例，比 Paxos 论文易读

**Raft 与 Paxos 的关系**：
- **算法本质相同**：都基于多数派 + Leader + 日志复制
- **设计哲学不同**：Paxos 追求"算法最小性"，Raft 追求"可理解性"
- **能力等价**：Raft 能做的 Paxos 都能做，反之亦然
- **工程选择**：现代系统倾向 Raft（etcd / TiKV / CockroachDB / Nacos），因为易实现易维护

### 8.4 后续篇章路径

```
第4章-01 Paxos（本篇）
   ↓
第4章-02 Raft（下一篇）
   - 三子问题分解
   - 领导人选举
   - 日志复制
   - 安全性
   - 与 Paxos 的对比
   ↓
第4章-03 ZAB
   - 原子广播
   - 与 Paxos/Raft 的差异
   - ZooKeeper 的工程实现
   ↓
第4章-04 对比与工业实现
   - 三大算法对比矩阵
   - etcd / ZooKeeper / TiKV / Spanner 工业实现
   - 选型建议
```

### 8.5 关键认知

带着以下 3 个认知进入下一篇（Raft）：

**认知 1：Paxos 是 FLP + Quorum 约束下的代表性工程解**
- 多数派 Quorum 框架下的多数工业级共识算法都采用类似"两阶段 + 多数派 + Leader 优化"的核心结构
- **注**：Ben-Or 1983 是纯异步+随机化算法，不依赖 Quorum，比 Paxos 早；Fast Paxos 在 Leader 稳定时可简化为一阶段——这些是 Paxos 之外的替代路径
- Basic Paxos 在无 Leader 假设下，两阶段是保证不变量的最小设计

**认知 2：Multi-Paxos 是 SMR 的算法基础**
- 通过稳定 Leader + 一次性 Prepare，把两阶段降为一阶段
- 所有强一致分布式系统（Chubby / Spanner / MGR）都基于 Multi-Paxos

**认知 3：Paxos 的困难催生了 Raft**
- Paxos 难理解、难实现
- Raft 通过"可理解性"重新设计，能力等价但更易实现
- 现代系统倾向 Raft，但 Paxos 仍是理论基石

---

## 结语：从理论到工业的完整链条

回顾全篇，我们完成了 Paxos 的完整推导：

- **第 1 节**：从 FLP + Quorum 推导 Paxos 的必然性
- **第 2 节**：共识问题的形式化定义
- **第 3 节**：从直觉方案逐步逼近 Paxos（4 个方案递进）
- **第 4 节**：Basic Paxos 的形式化（角色 / 两阶段 / 不变量）
- **第 5 节**：活锁问题与解决（随机退避 + Leader）
- **第 6 节**：Multi-Paxos 的日志复制（SMR 基础）
- **第 7 节**：工业实现（Chubby / Spanner / MGR）
- **第 8 节**：总结与 Raft 演进动机

**本篇的核心论断**：Paxos 不是某个天才的灵感，而是 FLP + Quorum 约束下的代表性工程解。Basic Paxos 在无 Leader 假设下两阶段是最小正确性保证，Multi-Paxos 通过稳定 Leader 实现工程可用。理解 Paxos 是理解所有强一致分布式系统的前提。

**带着这一框架进入下一篇**——Raft，我们将看到如何通过"可理解性"重新设计共识算法，能力等价但更易实现。
