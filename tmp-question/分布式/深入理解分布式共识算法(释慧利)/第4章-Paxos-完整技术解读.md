# 第4章 Paxos——分布式共识算法

> **技术专家重写**
>
> 原书第4章按"诞生 → 初探 → 详解 → 推导 → Multi-Paxos → PhxPaxos"组织，内容完整但 PhxPaxos 部分过长（占全章 40%），且缺乏"为什么 Paxos 能保证安全"的直觉推导。
> 本重写版聚焦一个核心问题：**Paxos 如何用"多数派"解决 2PC 的"全员同意"困境？两阶段为什么能保证安全？Multi-Paxos 如何优化？** 用具体编号推演每一步，用交集属性直觉证明安全性。

---

## 4.1 Paxos 的诞生——从 2PC 困境到共识突破

### 4.1.1 第3章的结论：2PC 的根本局限

第3章我们论证了 2PC/3PC 的根本局限——**它们要求"全员同意"才能推进**。在分布式系统中，节点可能宕机、网络可能分区，"全员同意"是不可行的——任一节点故障 = 阻塞。

2PC 要求所有参与者对 Prepare 阶段返回 Yes 才能进入 Commit 阶段——任一参与者故障或网络分区，协调者必须阻塞等待，无法推进。

### 4.1.2 Paxos 的核心突破：多数派替代全员

1990 年，Leslie Lamport 在论文《The Part-Time Parliament》（兼职议会，1990 年提交 TOCS，1998 年正式发表）中提出了 Paxos 算法。核心突破只有一句话：

> **用"多数派同意"替代"全员同意"。**

第1章 1.2.6 节已用鸽笼原理证明：任意两个多数派必有交集。Paxos 正是利用这一性质——**只要一个值被多数派接受，后续任何多数派必然包含至少一个"知道这个值"的节点，从而保证已选定的值不会被覆盖**。

这就是 Paxos 解决 2PC 困境的钥匙：
- 2PC：所有参与者必须 Yes → 任一故障 = 阻塞
- Paxos：多数派 Yes 即可 → 少数故障不影响

### 4.1.3 为什么叫"Paxos"——一个被遗忘的希腊小岛

Lamport 在论文中虚构了一个叫 Paxos 的希腊小岛，岛上的议会通过"多数派投票"立法——这就是 Paxos 算法的隐喻。论文的文风像考古论文，导致审稿人没看懂，论文在 TOCS 审稿过程中被搁置了 8 年（1990 提交 → 1998 发表）。

2001 年，Lamport 发表了《Paxos Made Simple》（让 Paxos 变简单），用通俗语言重新阐述——核心逻辑其实只有 3 页。Lamport 在文中承认，Paxos 的"难理解"不是因为算法复杂，而是因为原始论文的叙事方式过于晦涩。

> **跨书对照**：
> - **唐伟志《深入理解分布式系统》第4章**：从"Paxos 理论 + Go 实现"切入，提供代码级视角——差异在于"理论推导 vs 代码实现"
> - **江峰《分布式高可用算法》第8章**：从"共识的形式化定义"切入，将 Paxos 视为 FLP 不可能定理的"工程绕行"——FLP 说异步系统中共识不可能，Paxos 通过引入部分同步假设（最终稳定）绕过了这一限制

---

## 4.2 Basic Paxos 的角色与问题模型

### 4.2.1 三个角色

Paxos 定义了三个角色：

| 角色 | 职责 |
|------|------|
| **Proposer（提案者）** | 提出提案（编号 + 值），驱动两阶段协议 |
| **Acceptor（接受者）** | 对提案投票（Promise/Accept），持久化已接受的提案 |
| **Learner（学习者）** | 学习已选定的值（不参与投票） |

**角色可以合并**：在实际工程中，一个节点通常同时充当 Proposer 和 Acceptor（甚至 Learner）。例如 3 节点 etcd 集群中，每个节点都是 Proposer + Acceptor——Leader 是 Proposer，所有节点都是 Acceptor。各角色在 Raft/ZAB 中的对应关系将在第5、6章详述。

### 4.2.2 问题模型

**输入**：多个 Proposer 可能同时提出不同的值（v₁, v₂, ...）

**目标**：所有 Acceptor 最终就**唯一一个值**达成一致

**约束**：
1. **Safety（安全性）**：
   - 只有一个值会被选中（被多数派 Acceptor 接受）
   - Acceptor 不会"反悔"——一旦接受了某个值，不会改为接受更旧的值
2. **Liveness（活性）**：
   - 如果有 Proposer 提出提案，最终某个值会被选中（在部分同步假设下）

### 4.2.3 提案编号——Paxos 的核心机制

每个提案都有一个**全局唯一的递增编号 n**。编号是 Paxos 的"时间戳"——它决定了提案的"新旧"。

**编号规则**：不同 Proposer 使用不同的编号段，保证全局唯一递增。例如 3 个节点：
- Node-1 的编号：1, 4, 7, 10, ...
- Node-2 的编号：2, 5, 8, 11, ...
- Node-3 的编号：3, 6, 9, 12, ...

或者更常见的方式：`n = round * N + node_id`，其中 N 是节点总数。这保证了不同节点的编号不冲突，且全局递增。

**编号的作用**：Acceptor 只接受编号**大于或等于**已见过的最大编号的提案——这保证了"新提案可以覆盖旧提案，但旧提案不能覆盖新提案"。

---

## 4.3 Basic Paxos 两阶段——完整推导

### 4.3.1 为什么需要两阶段？

**直觉方案**：Proposer 直接向所有 Acceptor 发送"请接受值 v"——如果多数派接受，值就选定了。

**问题**：如果两个 Proposer 同时提出不同的值，各自获得部分 Acceptor 的接受——可能两个值都没有达到多数派，或者在不同时间点各自达到多数派——数据不一致。

**Paxos 的解决方案**：增加一个"准备阶段"——Proposer 先"探测"Acceptor 的状态，确保没有更早的值被选定后，再提交自己的值。这就是两阶段的由来。

### 4.3.2 阶段一：Prepare / Promise（准备 / 承诺）

**Proposer 的行为**：
1. 选择一个提案编号 n（比之前用过的都大）
2. 向所有（或多数派）Acceptor 发送 `Prepare(n)`

**Acceptor 的行为**（收到 Prepare(n)）：
- 如果 n **大于** Acceptor 已见过的最大编号 max_n：
  - 承诺不再接受编号 < n 的提案
  - 回复 `Promise(n, accepted_proposal)`，其中 accepted_proposal 是 Acceptor 之前已接受的最高编号提案（如果有）
- 如果 n **小于等于** max_n：
  - 拒绝（或不回复）

```
Proposer                    Acceptor-1    Acceptor-2    Acceptor-3
   │                            │             │             │
   │── Prepare(n=5) ──────────►│             │             │
   │── Prepare(n=5) ───────────────────────►│             │
   │── Prepare(n=5) ─────────────────────────────────────►│
   │                            │             │             │
   │                max_n=3 < 5 │ max_n=4 < 5 │ max_n=2 < 5 │
   │                承诺不接 <5  │ 承诺不接 <5  │ 承诺不接 <5  │
   │                无已接受提案 │ 已接受(3,v_a)│ 无已接受提案 │
   │                            │             │             │
   │◄── Promise(5, null) ──────│             │             │
   │◄── Promise(5, (3,v_a)) ────────────────│             │
   │◄── Promise(5, null) ────────────────────────────────│
   │                            │             │             │
   │ 收到多数派 Promise          │             │             │
   │ 发现有已接受值 v_a          │             │             │
   │ → 自己的提案值改为 v_a      │             │             │
```

**关键规则**：如果 Proposer 收到的 Promise 中有"已接受的提案"，Proposer 必须**采用编号最大的已接受值**作为自己的提案值——不能用自己的原始值。这保证了"如果已有值被选定，后续 Proposer 必须尊重它"。

### 4.3.3 阶段二：Accept / Accepted（接受 / 已接受）

**Proposer 的行为**：
1. 收到**多数派** Promise 后
2. 确定提案值：
   - 如果 Promise 中有已接受的提案 → 采用编号最大的已接受值
   - 如果没有 → 可以用自己的原始值
3. 向所有 Acceptor 发送 `Accept(n, value)`

**Acceptor 的行为**（收到 Accept(n, value)）：
- 如果 n **大于等于** Acceptor 已承诺的最大编号：
  - 接受提案，持久化 `(n, value)`
  - 回复 `Accepted(n)`
- 如果 n **小于** 已承诺的最大编号：
  - 拒绝（或不回复）

```
Proposer                    Acceptor-1    Acceptor-2    Acceptor-3
   │                            │             │             │
   │── Accept(5, v_a) ────────►│             │             │
   │── Accept(5, v_a) ──────────────────────►│             │
   │── Accept(5, v_a) ────────────────────────────────────►│
   │                            │             │             │
   │               5 ≥ max_n=5 │ 5 ≥ max_n=5 │ 5 ≥ max_n=5 │
   │                  接受(5,v_a)│  接受(5,v_a) │  接受(5,v_a) │
   │                  持久化     │  持久化      │  持久化      │
   │                            │             │             │
   │◄── Accepted(5) ───────────│             │             │
   │◄── Accepted(5) ────────────────────────│             │
   │◄── Accepted(5) ────────────────────────────────────│
   │                            │             │             │
   │ 收到多数派 Accepted         │             │             │
   │ → 值 v_a 被选定（Chosen）   │             │             │
   │ → 通知 Learner             │             │             │
```

### 4.3.4 完整时间线推演

**场景**：3 个 Acceptor，Proposer-1 提出值 v₁，Proposer-2 提出值 v₂。

**正常流程（无竞争）**：

```
T0: Proposer-1 选择编号 n=1，发送 Prepare(1) 给 A1, A2, A3
T1: A1, A2, A3 都没有见过提案，Promise(1, null)
T2: Proposer-1 收到 3 个 Promise（多数派），无已接受值
T3: Proposer-1 发送 Accept(1, v₁) 给 A1, A2, A3
T4: A1, A2, A3 接受，持久化 (1, v₁)，回复 Accepted(1)
T5: Proposer-1 收到 3 个 Accepted（多数派）→ v₁ 被选定
T6: Proposer-1 通知 Learner：v₁ 已选定
```

**竞争场景（两个 Proposer 同时提案）**：

```
T0: Proposer-1 选择 n=1，发送 Prepare(1) 给 A1, A2, A3
T1: A1, A2 Promise(1, null)（A3 未响应或延迟）
T2: Proposer-1 收到 2 个 Promise（多数派），准备发送 Accept(1, v₁)
    但此时——
T3: Proposer-2 选择 n=2，发送 Prepare(2) 给 A1, A2, A3
T4: A1, A2 的 max_n 更新为 2，Promise(2, null)（因为还没接受任何值）
T5: Proposer-2 收到多数派 Promise，发送 Accept(2, v₂)
T6: A1, A2 接受 (2, v₂)，回复 Accepted(2)
T7: Proposer-1 发送 Accept(1, v₁)——但 A1, A2 的 max_n=2 > 1
    → A1, A2 拒绝 Accept(1, v₁)
T8: Proposer-1 收到拒绝，知道有更高编号的提案，重新选择 n=3
T9: Proposer-1 发送 Prepare(3) 给 A1, A2, A3
T10: A1, A2 Promise(3, (2, v₂))——因为已接受了 (2, v₂)
T11: Proposer-1 收到 Promise，发现有已接受值 v₂
     → Proposer-1 必须采用 v₂（不能用自己的 v₁）
T12: Proposer-1 发送 Accept(3, v₂)
T13: A1, A2, A3 接受 (3, v₂)
T14: v₂ 被选定
```

**关键洞察**：即使 Proposer-1 原本想提 v₁，但在发现 v₂ 已被部分接受后，它**必须改为提 v₂**。这就是 Paxos 保证"已选定的值不会被覆盖"的核心机制——后来的 Proposer 必须尊重已选定的值。

### 4.3.5 更多故障场景推演

**场景三：Acceptor 在 Promise 后崩溃再恢复**

```
T0: Proposer 发送 Prepare(5) 给 A1, A2, A3
T1: A1, A2 Promise(5, null)——A3 崩溃
T2: Proposer 收到 2 个 Promise（多数派），发送 Accept(5, v₁)
T3: A1, A2 接受 (5, v₁)——v₁ 被选定
T4: A3 恢复——它错过了 Prepare(5) 和 Accept(5, v₁)
T5: 新 Proposer 发送 Prepare(6) 给 A1, A2, A3
T6: A1 Promise(6, (5,v₁)), A2 Promise(6, (5,v₁))
T7: A3 Promise(6, null)——A3 不知道 v₁ 已被选定
T8: 新 Proposer 收到 3 个 Promise，发现编号最大的已接受值是 v₁
T9: 新 Proposer 必须采用 v₁——发送 Accept(6, v₁)
T10: A3 接受 (6, v₁)——A3 的日志被"补齐"了
```

**关键洞察**：A3 恢复后不需要立即同步——它会在下一次 Prepare 中被"自然补齐"。Paxos 的自修复能力来自 Prepare 阶段的探测机制。

**场景四：网络丢包导致 Prepare 丢失**

```
T0: Proposer 发送 Prepare(5) 给 A1, A2, A3
T1: 发往 A1 的消息丢失——A2, A3 收到并 Promise(5, null)
T2: Proposer 只收到 2 个 Promise（多数派），继续
T3: Proposer 发送 Accept(5, v₁) 给 A1, A2, A3
T4: A1 收到 Accept(5, v₁)——但它从未 Promise(5)
     → A1 检查：5 > A1 的 max_n(0) → 接受
     （注意：A1 虽然没参与 Prepare，但仍可接受 Accept）
T5: A2, A3 也接受 → v₁ 被多数派选定
```

**关键洞察**：Prepare 阶段的多数派和 Accept 阶段的多数派不必是同一组节点——只要各自都构成多数派即可。Paxos 的容错能力比直觉更强。

### 4.3.6 Proposer 和 Acceptor 的伪代码

将前面两阶段的描述整理为伪代码，帮助理解完整流程：

**Proposer 伪代码**：

```
// 阶段一：Prepare
function propose(value):
    n = generate_unique_number()  // 全局唯一递增编号
    promises = []
    for acceptor in acceptors:
        resp = acceptor.prepare(n)  // 发送 Prepare(n)
        if resp.type == "PROMISE":
            promises.append(resp)
        // NACK 或超时则跳过
    
    if count(promises) < majority:
        retry_with_higher_n()  // 未获多数派，重新选编号重试
        return
    
    // 确定提案值
    accepted_values = [p.accepted_proposal for p in promises if p.accepted_proposal != null]
    if accepted_values is not empty:
        // 有关键约束：必须采用编号最大的已接受值
        chosen_value = max(accepted_values, key=lambda x: x.n).value
    else:
        chosen_value = value  // 没有已接受值，用自己的原始值
    
    // 阶段二：Accept
    accepts = []
    for acceptor in acceptors:
        resp = acceptor.accept(n, chosen_value)  // 发送 Accept(n, value)
        if resp.type == "ACCEPTED":
            accepts.append(resp)
    
    if count(accepts) >= majority:
        broadcast_chosen(chosen_value)  // 通知 Learner
    else:
        retry_with_higher_n()
```

**Acceptor 伪代码**：

```
// 持久化状态（必须 fsync 落盘）
promised_n = 0       // 已承诺的最大编号
accepted_proposal = null  // 已接受的提案 (n, value)

// 处理 Prepare 请求
function on_prepare(n):
    if n <= promised_n:
        return NACK(promised_n)  // 拒绝，返回当前最大编号
    // 关键：先持久化再回复
    promised_n = n
    fsync(promised_n)  // 必须落盘！
    return PROMISE(n, accepted_proposal)

// 处理 Accept 请求
function on_accept(n, value):
    if n < promised_n:
        return NACK(promised_n)  // 拒绝
    // 关键：先持久化再回复
    accepted_proposal = (n, value)
    fsync(accepted_proposal)  // 必须落盘！
    return ACCEPTED(n)
```

**关键观察**：
1. Acceptor 的每次状态变更都**先 fsync 再回复**——这是 Safety 的硬要求
2. Proposer 的"必须采用编号最大的已接受值"是 Safety 的核心约束——对应 4.5 节推导的 P2C
3. NACK 消息携带 `promised_n`，让 Proposer 可以直接跳到更高编号重试——对应 4.8.2 节的 NACK 优化

---

## 4.4 安全性证明——为什么能保证一致？

### 4.4.1 要证明什么？

**Safety 属性**：Paxos 保证**只有一个值被选定**（被多数派 Acceptor 接受）。

换句话说：不可能有两个不同的值 v₁ 和 v₂ 都被多数派接受。

### 4.4.2 关键不变式

Paxos 的安全性依赖于两个不变式：

**不变式 1**：Acceptor 一旦对编号 n 做出 Promise，就不再接受编号 < n 的提案。

**不变式 2**：Proposer 在阶段二发送的值，必须是阶段一收到的 Promise 中编号最大的已接受值（如果有）。

### 4.4.3 交集属性——多数派的数学基础

**第1章 1.2.6 节已证明**：任意两个多数派必有至少一个交集节点。

例如 3 个 Acceptor 中：
- 多数派 1 = {A1, A2}
- 多数派 2 = {A2, A3}
- 交集 = {A2}

### 4.4.4 安全性推导（直觉版）

假设两个不同的值 v₁ 和 v₂ 分别被多数派 M₁ 和 M₂ 接受——我们要证明这是不可能的。

**设 v₁ 被选定的提案编号为 n₁，v₂ 被选定的提案编号为 n₂。不失一般性，假设 n₁ < n₂。**

**推导步骤**：

1. v₁ 被 M₁ 接受 → M₁ 中所有 Acceptor 都接受了 `(n₁, v₁)`
2. v₂ 被 M₂ 接受 → M₂ 中所有 Acceptor 都接受了 `(n₂, v₂)`
3. M₁ 和 M₂ 有交集 → 存在 Acceptor A 同时在 M₁ 和 M₂ 中
4. A 先接受了 `(n₁, v₁)`，后接受了 `(n₂, v₂)`——**为什么是这个顺序？**因为 A 接受 `(n₁, v₁)` 时还没对任何 n > n₁ 的提案做 Promise（否则根据不变式 1 会拒绝 `(n₁, v₁)`）。所以 A 对 n₂ 的 Promise 必然发生在接受 `(n₁, v₁)` 之后——即 A 先接受 `(n₁, v₁)`，后接受 `(n₂, v₂)`
5. A 接受 `(n₂, v₂)` 之前，必然先对 n₂ 做出 Promise
6. A 做 Promise(n₂) 时，n₂ > n₁ → A 会把自己已接受的 `(n₁, v₁)` 包含在 Promise 中返回给 Proposer-2
7. Proposer-2 收到 Promise(n₂, (n₁, v₁)) → 根据不变式 2，Proposer-2 必须采用 v₁ 作为提案值
8. 所以 v₂ = v₁ ← **矛盾！v₂ 不可能不同于 v₁**

**结论**：如果 v₁ 先被选定，后续任何被选定的值必然等于 v₁。Paxos 保证了 Safety。

### 4.4.5 这个证明的工程含义

**核心机制**：阶段一的"探测"是安全性的关键——Proposer 在提交自己的值之前，先"探测"Acceptor 是否已接受过其他值。如果有，就必须"继承"那个值。

**这解释了为什么 Paxos 需要两阶段**：
- 如果只有一阶段（直接 Accept），Proposer 不知道其他 Proposer 是否已选定值——可能覆盖
- 两阶段的 Prepare 阶段确保 Proposer "看到"已有的值，从而"继承"它

**2PC 的对比**：2PC 的 Prepare 阶段是"锁定资源"，Paxos 的 Prepare 阶段是"探测已选定的值"——目的完全不同。这就是 2PC 和 Paxos 的根本区别。

> **跨书对照**：江峰《分布式高可用算法》第8章给出了 Paxos 安全性的完整形式化证明（基于 I/O Automata）。这里的直觉推导足以理解"为什么 Paxos 安全"，形式化证明适合深入研究。

---

## 4.5 从第一性原理推导 Paxos——Lamport 是怎么想出来的？

上一节用交集属性证明了"Paxos 安全"。但你可能还有一个更根本的疑问：**Lamport 是怎么从零开始设计出这个两阶段协议的？** 这一节重现 Lamport 在《Paxos Made Simple》中的推导路径——从最简单的方案出发，逐步发现问题、增强约束，直到自然推出两阶段。

### 4.5.1 起点：最简单的方案

**目标**：多个 Acceptor 就一个值达成一致。

**最简单方案**：每个 Acceptor 批准它收到的第一个提案。

这就是 **P1**：
> **P1**：Acceptor 必须批准它接收到的第一个提案。

**P1 的问题——投票瓜分**：如果有 3 个 Acceptor，3 个 Proposer 同时分别向 A1、A2、A3 发送不同的提案——每个 Acceptor 批准了不同的值，没有任何值获得多数派支持——共识失败。

### 4.5.2 允许 Acceptor 批准多个提案

P1 太严格了——Acceptor 只能批准一个提案导致投票瓜分。放松约束：**允许 Acceptor 批准多个提案**，但用提案编号标识新旧。

现在问题是：如何保证"一旦值 v 被选定，后续选定的值也是 v"？这就是 **P2**：
> **P2**：如果一个提案 `[M, V]` 被选择了，那么后续被批准的编号更高的提案所包含的值都是 V。

### 4.5.3 从 P2 到 P2A——约束 Acceptor

P2 说"后续被批准的提案值都是 V"——这约束的是所有 Acceptor。但提案被批准是 Acceptor 的行为，所以增强为 **P2A**：
> **P2A**：如果一个提案 `[M, V]` 被选择了，那么任何 Acceptor 批准的编号更高的提案所包含的值都是 V。

**P2A 的问题**：假设 `[M₁, V₁]` 已被多数派 Q₁ 选定。此时 Proposer-2 提出 `[M₂, V₂]`（M₂ > M₁, V₂ ≠ V₁），发送给 Acceptor C（C ∉ Q₁）。C 从未批准过任何提案——按 P1 的精神，C 应该批准 `[M₂, V₂]`。但这违反了 P2A（因为 V₁ 已被选定，C 不能批准 V₂ ≠ V₁）。

**矛盾**：P1 说"批准第一个提案"，P2A 说"不能批准与已选定值不同的提案"——当 C 不知道 V₁ 已被选定时，两者冲突。

### 4.5.4 从 P2A 到 P2B——约束 Proposer

解决矛盾的思路：**不要让 Proposer 提出错误的值**。如果 Proposer 在提出提案前就知道 V₁ 已被选定，它就不会提 V₂。

增强为 **P2B**：
> **P2B**：如果一个提案 `[M, V]` 被选择了，那么此后任何 Proposer 提出的编号更高的提案包含的值都是 V。

P2B 比 P2A 更强——它约束的是 Proposer 的"提出"行为，而非 Acceptor 的"批准"行为。只要 Proposer 不提出错误的值，Acceptor 自然不会批准错误的值。

### 4.5.5 从 P2B 到 P2C——推导出两阶段

**核心问题**：如何保证 P2B？即——Proposer 在提出 `[Mₙ, Vₙ]` 之前，如何确保 Vₙ = V₁（如果 V₁ 已被选定）？

**Lamport 的洞察**：`[M₁, V₁]` 被选定意味着多数派 Q₁ 批准了它。任意新的多数派 Qₙ 必与 Q₁ 有交集。所以——

> Proposer 在提出 `[Mₙ, Vₙ]` 之前，先询问一个多数派 Qₙ："你们批准过的编号 < Mₙ 的提案中，编号最大的是哪个值？"

如果 V₁ 已被选定，Qₙ 中至少有一个 Acceptor 批准过 `[M₁, V₁]`——Proposer 会发现这个值，并采用它。

这就是 **P2C**：
> **P2C**：对于任意的 M 和 V，如果提案 `[M, V]` 被提出，那么一个由 Acceptor 多数派组成的集合 Q 满足以下任意条件：
> - 条件一：Q 中没有 Acceptor 批准过编号 < M 的提案（说明之前没有值被选定，V 可以任意）
> - 条件二：Q 中编号 < M 的已批准提案里，V 是编号最大的那个值（说明之前有值被选定，V 必须继承它）

### 4.5.6 从 P2C 推导出两阶段

P2C 要求 Proposer 在提出提案前先"询问"多数派——这就是 **Prepare 阶段**的由来：

1. **Prepare 阶段**：Proposer 发送编号 M 给 Acceptor，询问"你批准过的编号 < M 的最大提案是什么？"
2. Acceptor 回复："没批准过任何提案"（条件一）或"批准过的最大提案是 `[Mₖ, Vₖ]`"（条件二）
3. **Accept 阶段**：Proposer 根据回复决定 V——如果有已批准值，采用编号最大的；如果没有，用自己的值

同时，Acceptor 需要一个承诺：**一旦回复了 Prepare(M)，就不再批准编号 < M 的提案**——否则 Proposer 刚问完，Acceptor 就批准了一个旧提案，P2C 就被破坏了。这就是 **P1A**：
> **P1A**：当且仅当 Acceptor 没有通过编号 > M 的 Prepare 请求时，它才可以批准编号为 M 的提案。

### 4.5.7 推导完成——两阶段自然涌现

从 P1 出发，逐步增强约束，最终自然推出了两阶段：

```
P1: 批准第一个提案
  ↓ 问题：投票瓜分
允许批准多个提案 + 编号
  ↓ 问题：如何保证已选定的值不被覆盖？
P2: 已选定的值后续不变
  ↓ 约束 Acceptor
P2A: Acceptor 不能批准不同的值
  ↓ 问题：Acceptor 不知道值已选定
P2B: Proposer 不能提出不同的值
  ↓ 问题：Proposer 如何知道？
P2C: 提出前先询问多数派
  ↓ 推导出
Prepare 阶段（询问）+ Accept 阶段（提交）
  ↓ Acceptor 需要承诺
P1A: 承诺不批准更旧的提案
```

**这就是 Paxos 两阶段的"为什么"**——不是凭空设计的，而是从"保证已选定的值不被覆盖"这个约束出发，逐步推导出来的必然结果。

> **跨书对照**：释慧利原书 4.4 节完整重现了 Lamport 的推导路径。唐伟志《深入理解分布式系统》第4章也包含此推导，但更简洁。本节的推导遵循 Lamport《Paxos Made Simple》的原始路径，是理解 Paxos 最根本的途径——比 4.4 节的交集属性证明更深入。

---

## 4.6 活锁问题——Paxos 的 Liveness 缺陷

### 4.6.1 活锁场景

Paxos 保证了 Safety（一致性），但 Liveness（终止性）存在问题——**活锁（livelock）**。

**活锁场景**：两个 Proposer 互相抢占：

```
T0: Proposer-1 发送 Prepare(1)，收到多数派 Promise
T1: Proposer-2 发送 Prepare(2)，收到多数派 Promise
    → Proposer-1 的 Accept(1) 被拒绝（max_n 已更新为 2）
T2: Proposer-1 发送 Prepare(3)，收到多数派 Promise
    → Proposer-2 的 Accept(2) 被拒绝（max_n 已更新为 3）
T3: Proposer-2 发送 Prepare(4)，收到多数派 Promise
    → Proposer-1 的 Accept(3) 被拒绝
T4: ... 无限循环，没有值被选定
```

**活锁 vs 死锁的区别**：
- **死锁**：线程互相等待对方释放锁——都不在运行
- **活锁**：Proposer 都在运行、不断重试，但每次都被对方抢占——永远无法完成

### 4.6.2 活锁的解决——随机超时 + Leader

FLP 不可能定理（第1章 1.6.1 节）告诉我们：异步系统中不可能保证共识终止。Paxos 的活锁正是 FLP 的体现——在完全异步环境下，两个 Proposer 永远可以互相抢占。

**工程解决方案**：引入部分同步假设——随机超时。

1. **随机退避（Random Backoff——网络冲突时的通用技术，冲突后等待一段随机时间再重试，TCP 重传和以太网 CSMA/CD 都用此机制）**：Proposer 被拒绝后，等待一个**随机**时间再重试。这降低了两个 Proposer 同时重试的概率。
2. **选出一个 Leader**：只让 Leader 当 Proposer——单 Proposer 不会活锁。这就是 Multi-Paxos 的核心优化。

**为什么随机超时能解决活锁？** 因为两个 Proposer 的随机等待时间不同——总有一个先重试并成功。这和 Raft 的随机选举超时（第6章）是同一思想。

---

## 4.7 Multi-Paxos——从单值共识到日志共识

### 4.7.1 Basic Paxos 的局限

Basic Paxos 每次只决定**一个值**。但实际系统（如 etcd、ZooKeeper）需要决定一个**日志序列**（一系列有序的操作命令）。如果每个命令都跑一遍完整的 Basic Paxos（两阶段），性能极差——每次提案都要两次网络往返。

### 4.7.2 Multi-Paxos 的核心优化：Leader + 省略 Prepare

Multi-Paxos 的核心思想是：**选出一个稳定的 Leader，由 Leader 串行化所有提案。Leader 任期开始时运行一次 Prepare（向所有 Acceptor 获取承诺、探测已接受值），之后在任期内省略 Prepare 阶段，直接进入 Accept 阶段。**

**为什么 Leader 可以省略 Prepare？**

Prepare 阶段的作用是"探测已有值 + 获取承诺"。如果 Leader 是稳定的（没有其他 Proposer 竞争），那么：
- Leader 已经知道自己上一轮的 Prepare 结果
- Acceptor 的承诺仍然有效（只要没有更高编号的 Prepare 打断）

所以 Leader 可以直接发送 Accept——**从两阶段变为一阶段**，显著降低网络往返开销（吞吐通常提升 1.5-2x）。

**Leader 选举**：Multi-Paxos 本身不定义 Leader 选举算法（Lamport 在论文中说"选择一个 Leader 是工程问题"）。常见做法：
- 用 Basic Paxos 选 Leader（提案值 = Leader ID）
- Leader 租约（Lease：授权方在一段固定时间内把某权限独占授予被授权方，租约期内其他人无法获得同一权限）机制保证 Leader 的唯一性

### 4.7.3 日志复制——每个 entry 独立 Paxos

Multi-Paxos 把日志看作一系列 entry：`log[0], log[1], log[2], ...`

每个 entry 独立运行一次 Paxos：
```
log[0]: Accept(n=5, v=cmd_0) → 多数派接受 → chosen
log[1]: Accept(n=5, v=cmd_1) → 多数派接受 → chosen
log[2]: Accept(n=5, v=cmd_2) → 多数派接受 → chosen
...
```

Leader 在任期内用同一个编号 n 依次为每个 entry 提案。Acceptor 对每个 entry 独立记录已接受的值。

**关键点**：不同 entry 的值是独立的——`log[0]` 选定 v₀ 不会影响 `log[1]` 选定 v₁。但所有 entry 共享同一个 Leader 和编号序列。

### 4.7.4 Leader 切换时的日志恢复

Leader 崩溃后，新 Leader 上任时不知道之前的日志状态——可能有些 entry 已经被多数派接受（chosen），有些只有少数派接受（未 chosen）。

**新 Leader 的恢复流程**：

```
1. 新 Leader 通过 Basic Paxos 选举产生（提案值 = Leader ID）
2. 新 Leader 选择一个比前任更高的编号 n'
3. 对所有可能未 chosen 的 entry [i..last_index]：
   a. 发送 Prepare(n') 给所有 Acceptor
   b. 收集多数派响应中编号最大的已接受值
   c. 如果有已接受值 → 继承该值（保证 Safety）
   d. 如果没有已接受值 → 用 Noop 填补（使该 entry chosen）
4. Prepare 完成后，新 Leader 获得了所有 entry 的"执行权"
5. 后续提案直接发送 Accept（省略 Prepare）
```

**为什么用 Noop 填补空洞？** 如果某个 entry 没有任何 Acceptor 接受过提案，说明它从未被提案过。如果不填补，状态机无法应用后续 entry（必须按顺序应用）。用 Noop（空操作）填补，使该 entry chosen 但不影响状态——后续 entry 可以正常应用。

**这就是 Prepare 阶段在 Multi-Paxos 中仍然需要的原因**——不是每次提案都需要，但 Leader 切换时需要用 Prepare 恢复日志状态。

> **生产对照**：Google Chubby（Mike Burrows 设计，基于 Lamport 的 Paxos 理论，论文《The Chubby Lock Service for Loosely-Coupled Distributed Systems》OSDI 2006）使用 Multi-Paxos。Chubby 的论文描述了 Multi-Paxos 在生产环境的完整工程实现，包括 Leader 租约、日志恢复、成员变更等。Chubby 是 Paxos 工程化的标杆案例。

### 4.7.5 快照与日志压缩

Multi-Paxos 的日志会无限增长——每个操作都被记录为一条 log entry。长期运行后，日志会占用大量磁盘空间，且新 Leader 恢复时需要重放全部日志。

**快照机制**：
1. 状态机定期将当前状态序列化为快照文件（如 etcd 的 snapshot）
2. 记录快照对应的 last_included_index（快照包含到哪个 entry）
3. 删除 last_included_index 之前的所有 log entry
4. 快照本身也通过共识协议复制到多数派节点

**新 Leader 恢复时的流程（含快照）**：
```
1. 新 Leader 先获取最新的快照
2. 从快照的 last_included_index + 1 开始重放日志
3. 对 last_included_index + 1 之后的 entry 执行 Prepare 恢复
4. 不需要处理快照之前的 entry——它们已经反映在快照状态中
```

**快照的触发时机**：
- 日志大小超过阈值（如 etcd 默认 100MB）
- 定时触发（如每小时一次）
- 日志条目数超过阈值（如 Raft 默认 10000 条）

### 4.7.6 Multi-Paxos 的工程挑战

Multi-Paxos 的论文只给出了核心思想，工程实现需要解决大量细节：

1. **日志空洞**：Leader 为 entry[i] 提案后崩溃，entry[i] 未 chosen 但 entry[i+1] 已 chosen——需要填补空洞
2. **成员变更**：集群扩容/缩容时，如何保证新旧配置不冲突（第1章 1.1.5 节谬误 5 已提及）
3. **快照**：日志无限增长需要压缩——快照 + 日志截断
4. **客户端语义**：exactly-once（恰好一次）vs at-least-once（至少一次）

**这些工程挑战是 Paxos"难实现"的主要原因**——理论简单，但工程细节繁多。这也是 Raft 出现的动机（第6章）。

---

## 4.8 工程实现挑战

### 4.8.1 持久化——什么必须落盘？

Paxos 的安全性依赖 Acceptor 的"承诺"和"已接受"记录。如果 Acceptor 重启后"忘记"了这些信息，安全性可能被破坏。

**必须持久化的状态**：
| 状态 | 持久化要求 | 后果 |
|------|-----------|------|
| Acceptor 的 `promised_n`（已承诺的最大编号） | 必须落盘 | 重启后可能接受旧提案 → 破坏 Safety |
| Acceptor 的 `accepted_proposal`（已接受的提案） | 必须落盘 | 重启后"忘记"已选定的值 → 数据丢失 |
| Proposer 的 `max_n`（用过的最大编号） | 建议落盘 | 重启后可能复用旧编号 → 协议混乱 |

**工程实践**：每次 Promise 和 Accept 之前，先 fsync 到持久化存储的 WAL（Write-Ahead Log，预写日志——所有修改先写日志再改内存，崩溃后用日志恢复），再回复。这和 MySQL redo log 的 fsync 是同一思想（第2章 2.1.5 节），但目的不同：Paxos 持久化是协议正确性要求，MySQL redo log 是事务持久性要求。

### 4.8.2 工程优化——加速 Paxos 的 5 种手段

原始 Paxos 协议在工程实现中有多种优化手段，以下是生产系统常用的 5 种：

**1. NACK 消息（Negative Acknowledgment）**

原始 Paxos 中，Acceptor 收到编号过低的 Prepare/Accept 时选择"忽略"——Proposer 只能等超时后重试。NACK 优化让 Acceptor 主动回复"拒绝 + 当前最大编号 MaxNo"，Proposer 可以直接跳到 MaxNo+1 重试，避免逐个递增编号的盲目尝试。

```
Proposer ──Prepare(n=5)──► Acceptor
Acceptor: max_n=10 > 5，拒绝
Acceptor ──NACK(max_n=10)──► Proposer
Proposer: 下次直接用 n=11，跳过 6-10
```

**2. 跳过 Accept 阶段**

如果 Prepare 阶段收到的多数派响应中都显示"已批准了同一个值 v"，说明 v 在过去的某个时刻已经被选定——Proposer 可以直接结束，不需要再执行 Accept 阶段。

**3. Confirm 请求**

Proposer 在 Accept 阶段获得多数派支持后，发送一轮 Confirm 请求给所有 Acceptor，通知"该提案已达成共识"。Acceptor 收到后记录 Confirm 标记。后续如果有新 Proposer 发起 Prepare，Acceptor 可以直接告知"这个值已 chosen"——新 Proposer 可以跳过 Accept 阶段。

**4. 选出稳定的 Leader（Multi-Paxos 核心）**

多个 Proposer 竞争会导致活锁。选出唯一的 Leader 串行化所有提案——这是 Multi-Paxos 的核心优化（4.7.2 节已述）。Leader 任期内省略 Prepare，从两阶段变一阶段。

**5. Prepare 预执行**

Prepare 阶段不携带具体值（只携带编号），可以在客户端请求到来之前提前执行。客户端请求到来后，Leader 直接进入 Accept 阶段——省去了 Prepare 的等待时间。这种"管道化"让 Paxos 的写入延迟接近一阶段。

### 4.8.3 并行协商——Multi-Paxos 的性能加速

Multi-Paxos 中，Leader 需要为一系列 Instance（log entry）依次提案。如果串行执行——Instance[i] 必须等 Instance[i-1] chosen 后才能提案——吞吐极低。

**并行协商**：允许 Leader 同时为多个 Instance 并行执行 Accept 阶段。因为每个 Instance 的 Paxos 是独立的，并行不影响 Safety。

```
Leader:  Instance[51] Accept ──►  等待...
Leader:  Instance[52] Accept ──►  等待...
Leader:  Instance[53] Accept ──►  等待...
（3 个 Instance 并行协商，不必等前一个完成）
```

**并行协商的问题——日志空洞**：如果 Instance[52] 已 chosen 但 Instance[51] 协商失败（未 chosen），就产生了空洞。状态机必须按顺序应用日志——Instance[52] 不能在 Instance[51] 之前应用。

**解决方案**：用 Noop（空操作）填补空洞——Leader 对失败的 Instance 重新提案一个 Noop 值，使其 chosen 后，后续 Instance 可以正常应用。

### 4.8.4 Instance 重确认——Leader 切换后的恢复

新 Leader 上任时，不知道前任 Leader 有哪些 Instance 已经 chosen。如果直接从某个位置开始提案，可能覆盖已 chosen 的值。

**重确认流程**：
1. 新 Leader 对所有**不确定是否 chosen** 的 Instance 重新执行 Prepare
2. Prepare 会探测 Acceptor 已接受的值——如果有已接受值，新 Leader 必须继承
3. 对于没有任何 Acceptor 接受过提案的 Instance（空洞），用 Noop 填补
4. 对于已确认 chosen 的 Instance，Prepare 会自动继承相同的值（不影响结果）

这个流程保证了 Leader 切换后日志的完整性和一致性。

### 4.8.5 幽灵日志问题

**场景**：Leader 提出 Accept(n, v) 给 entry[i]，部分 Acceptor 接受了，但 Leader 崩溃——entry[i] 未 chosen。新 Leader 上任后，用更高的编号 n' 重新为 entry[i] 提案，选定了不同的值 v'。

**问题**：之前接受了 (n, v) 的 Acceptor 重启后，重放日志看到 (n, v)，但全局 chosen 的值是 v'——Acceptor 本地日志与全局状态不一致。

**解决方案**：新 Leader 在恢复时，必须用 Prepare 探测所有 Acceptor 的已接受值，并继承编号最大的已接受值。这样即使有"幽灵日志"，Prepare 阶段也会发现并继承它。同时，Acceptor 重启后应先重放日志到 Prepare 时的状态，不接受任何与已有日志矛盾的提案。

### 4.8.6 成员变更

集群扩容（3→5 节点）或缩容（5→3 节点）时，如果直接切换配置，可能出现"旧配置的多数派"和"新配置的多数派"不重叠——两个多数派各自选定不同的值，脑裂。

**解决方案**：
1. **Joint Consensus（联合一致，Raft 论文术语；Paxos 论文中对应概念为 Reconfiguration）**：先过渡到"旧配置 + 新配置"的联合状态，要求新旧两个多数派都同意才能推进；再过渡到新配置。Raft 使用的就是这种方法（第6章）。
2. **单节点变更**：每次只增减一个节点——数学上可以证明，单节点变更不会导致两个不重叠的多数派。Paxos 的很多工程实现使用这种简化方案。

### 4.8.7 读请求优化

Paxos 的写请求经过共识协议（两阶段），但读请求如果也走共识会太慢。常见优化：

1. **Leader Read**：直接从 Leader 读——但 Leader 可能已被其他节点取代（自己不知道），读到旧值
2. **Read Index**：Leader 先确认自己仍是 Leader（通过多数派心跳），再读——etcd 3.4+ 默认使用这种方式（第1章 1.6.2 节已述）
3. **Lease Read**：Leader 在 lease（租约）期内直接读，不走共识——最快但依赖时钟准确性
4. **Confirm + Local Read**：Acceptor 收到 Confirm 标记后，可以直接从本地状态机读取已 chosen 的值——不需要经过 Leader。这是 PhxPaxos 等系统使用的高性能读方案

> **跨书对照**：释慧利原书第4章 4.6 节详细讨论了 NACK、Confirm、并行协商、Instance 重确认等优化。唐伟志《深入理解分布式系统》第4章从 Go 实现角度展示了这些优化的代码。两者互补：释慧利讲"为什么"，唐伟志讲"怎么做"。

### 4.8.8 工业实践案例

**Cassandra LWT（Lightweight Transaction）**：Cassandra 的 `IF NOT EXISTS` / `IF condition` 语法使用 Basic Paxos 实现 CAS（Compare-And-Swap）。每次 LWT 操作运行一次完整的 4 阶段 Paxos（Prepare + Accept + Read + Commit），延迟约普通写的 3-4 倍。适用于需要强一致性的场景（如用户注册时检查用户名唯一性），但不应大规模使用——LWT 的吞吐受限于 Paxos 的串行化。

**MySQL MGR（Group Replication）**：MySQL 5.7.17+ 的组复制使用 XCom 通信层，XCom 是 Paxos 变体（类 Multi-Paxos），用于在复制组成员间达成事务共识。每个事务在提交前必须经过多数派 XCom 节点同意——这保证了即使 Leader 故障，已提交的事务也不会丢失。MGR 的单主模式（Single-Primary）类似于 Multi-Paxos 的 Leader 模式。

**Google Chubby**：Multi-Paxos 的标杆工程实现。Chubby 使用 Leader 租约 + Multi-Paxos 提供分布式锁服务和低容量元数据存储。Chubby 的工程细节（如 Leader 租约续约、Session 机制、客户端缓存一致性）是 Paxos 工程化的经典参考。

**Google Spanner**：每个 tablet（数据分片）由一个 Paxos Group 管理（Multi-Paxos）。跨 tablet 的事务使用 2PC 协调——这是"Paxos 做副本 + 2PC 做跨分片事务"的经典组合。TrueTime 提供外部一致性保证。

> **跨书对照**：陈东明《分布式系统与一致性》第8章从系统案例角度分析了 Spanner 的 Paxos + TrueTime 设计。释慧利原书第4章 4.7 节以 PhxPaxos（微信开源 Paxos 实现）为例讲解工程落地——本节用 Cassandra LWT 和 MySQL MGR 替代 PhxPaxos，因为它们对 Java 工程师更贴近。

---

## 4.9 从 Paxos 到 Raft——为什么 Raft 更受欢迎

### 4.9.1 Paxos 的"难理解"问题

Lamport 自己在《Paxos Made Simple》开头写道：

> "The Paxos algorithm, when presented in English, is very simple."

但事实是，Paxos 被公认为"分布式系统中最难理解的算法之一"。Diego Ongaro（Raft 作者）在 Raft 论文中做了一个实验：让斯坦福大学的学生分别学习 Paxos 和 Raft——Raft 的学习效果显著好于 Paxos。

**Paxos 难理解的原因**：
1. **角色拆分**：Proposer/Acceptor/Learner 三角色，加上 Leader 角色，交互复杂
2. **编号机制**：提案编号的全局唯一递增、Promise 的承诺逻辑，容易混淆
3. **Multi-Paxos 不完整**：Lamport 的论文只给出了 Basic Paxos 的完整描述，Multi-Paxos 只给了思路，工程细节留给读者
4. **缺乏标准实现**：Paxos 没有像 Raft 那样的参考实现，每个工程实现都不同（Chubby/PhxPaxos/libpaxos 各有差异）

### 4.9.2 Raft 的设计哲学：可理解性优先

2014 年，Diego Ongaro 和 John Ousterhout 在 USENIX ATC 发表了 Raft 论文《In Search of an Understandable Consensus Algorithm》。Raft 的核心设计目标不是"更高效"或"更正确"，而是**更易理解**。

**Raft 的关键设计决策**（对比 Paxos）：

| 维度 | Paxos | Raft |
|------|-------|------|
| 角色 | Proposer/Acceptor/Learner | Leader/Follower/Candidate（更直观） |
| 编号 | 全局唯一递增的提案编号 | term（任期号，每个选举周期递增） |
| 日志 | 每个 entry 独立 Paxos | Leader 串行追加日志，统一复制 |
| Leader 选举 | Multi-Paxos 未定义 | 明确定义（随机超时 + RequestVote RPC——Raft 中候选人向其他节点拉票的远程过程调用） |
| 成员变更 | Joint Consensus 或单节点 | Joint Consensus（明确给出算法） |
| 工程指导 | 留给读者 | 论文本身就是工程指南 |

**Raft 不是"更好的 Paxos"，而是"更易理解的 Paxos"**——两者的数学基础相同（多数派 + 日志复制），但 Raft 通过更清晰的角色定义和更强的 Leader 模型，大幅降低了实现难度。

### 4.9.3 工业界的实际选择

| 系统 | 共识算法 | 选择原因 |
|------|---------|---------|
| Google Chubby | Multi-Paxos | 历史最早，Lamport 参与设计 |
| Google Spanner | Multi-Paxos | 跨地域数据库，需要 Paxos 的灵活性 |
| Apache ZooKeeper | ZAB（与 Paxos 同族但独立设计的协议） | 2008 年设计，Raft 尚未出现；ZAB 有独立的 Discovery/Synchronization/Broadcast 三阶段 |
| etcd | Raft | 2013 年设计，Raft 论文同期 |
| TiKV | Raft | Multi-Raft 架构，Raft 更易实现 |
| Consul | Raft | HashiCorp 选择 Raft 降低维护成本 |
| Kafka KRaft（Kafka Raft，Kafka 3.3+ 默认的内置 Raft 共识模块，替代原先依赖 ZooKeeper 的元数据管理） | Raft | 2.8 预览 / 3.3+ 默认 KRaft 模式 |

**趋势**：新系统几乎都选 Raft——因为"可理解性 = 可维护性 = 工程可靠性"。Paxos 仍用于 Google 内部系统（Chubby/Spanner）、Cassandra LWT（Lightweight Transaction，基于 Paxos 实现 CAS）、MySQL MGR（XCom 通信层使用 Paxos 变体）等场景。

> **跨书对照**：
> - **唐伟志《深入理解分布式系统》第4章**：Paxos + Raft 的对比讲解，含 Go 实现代码——差异在于"理论推导 vs 代码实现"
> - **江峰《分布式高可用算法》第8-9章**：从形式化角度对比 Paxos 和 Raft 的数学等价性——差异在于"工程直觉 vs 数学严格"
> - **释慧利第6章**：Raft 的完整推导（本书下一章将详细展开）

---

## 4.10 本章总结

### 8 个核心要点

1. **Paxos 的核心突破**：用"多数派同意"替代 2PC 的"全员同意"——少数节点故障不影响共识。这是从 2PC 到 Paxos 的根本范式转换。

2. **三个角色**：Proposer（提案）、Acceptor（投票）、Learner（学习）。实际工程中节点常兼任多角色——etcd 每个 node 都是 Acceptor，Leader 是 Proposer。

3. **提案编号 n 是 Paxos 的"时间戳"**：全局唯一递增，决定了提案的新旧。Acceptor 只接受编号 ≥ 已承诺最大编号的提案——这保证了"新覆盖旧，旧不能覆盖新"。

4. **两阶段 Prepare/Promise → Accept/Accepted**：Prepare 阶段"探测已有值 + 获取承诺"；Accept 阶段"提交值"。如果 Prepare 发现已接受的值，Proposer 必须继承它——这是 Safety 的核心机制。

5. **安全性证明靠交集属性**：两个多数派必有交集 → 交集节点把已选定的值传递给后续 Proposer → 后续 Proposer 必须继承该值 → 不可能选定两个不同的值。

6. **活锁是 Paxos 的 Liveness 缺陷**：两个 Proposer 互相抢占导致永远无法完成。解决方案是随机超时 + Leader 选举（引出 Multi-Paxos）。

7. **Multi-Paxos = Leader + 省略 Prepare**：选出一个稳定 Leader，由 Leader 直接发送 Accept（省略 Prepare）——从两阶段变一阶段，显著降低网络往返开销。Leader 切换时仍需 Prepare 恢复日志。

8. **Raft 比 Paxos 更受欢迎不是因为"更好"而是"更易理解"**：两者的数学基础相同，但 Raft 通过清晰的角色定义和强 Leader 模型大幅降低了实现难度。新系统（etcd/TiKV/Consul/Kafka KRaft）几乎都选 Raft。

### 生产实践要点

- **必须持久化**：Acceptor 的 promised_n 和 accepted_proposal 必须 fsync 落盘，否则重启破坏 Safety
- **幽灵日志**：Leader 切换时用 Prepare 探测并继承已接受值，解决幽灵日志问题
- **成员变更**：Joint Consensus 或单节点变更，避免两个不重叠的多数派
- **读优化**：Lease Read > Read Index > Leader Read > 走共识

### 第5章预告

第5章将聚焦 ZAB（ZooKeeper Atomic Broadcast，ZooKeeper 原子广播协议）——ZooKeeper 的共识协议。ZAB 与 Paxos 同族但独立设计，专为 ZooKeeper 的主备复制场景优化。我们将对比 ZAB 和 Paxos 的差异，理解"为什么 ZooKeeper 不直接用 Paxos"。

---
