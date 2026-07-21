# 第5章 ZAB——ZooKeeper 技术核心

> **技术专家重写**
>
> 原书第5章从 Chubby 背景讲起，再到 ZAB 实现、ZooKeeper 源码实战，内容完整但 Chubby 部分占比过大。
> 本重写版聚焦一个核心问题：**ZAB 如何为 ZooKeeper 的主备复制场景优化？它和 Paxos 有什么本质区别？为什么 ZooKeeper 不直接用 Paxos？**

---

## 5.1 问题引入——ZooKeeper 需要什么样的共识？

### 5.1.1 ZooKeeper 的定位

ZooKeeper 是 Apache 旗下的分布式协调服务（coordination service），灵感来自 Google Chubby。它的定位不是数据库，而是**为分布式应用提供配置管理、命名服务、分布式锁、Leader 选举等协调功能**。

典型的 ZooKeeper 使用场景：
- **Kafka**：用 ZooKeeper 管理 Broker 元数据、Controller 选举（2.8 之前；4.0 版本完全移除 ZK 依赖，转向 KRaft 模式）
- **HBase**：用 ZooKeeper 管理 Master 选举、Region 分配
- **Dubbo**：用 ZooKeeper 做服务注册与发现
- **Curator**：基于 ZooKeeper 的分布式锁框架

这些场景的共同特点是：**数据量小、写并发低、读并发高、对一致性要求强**。ZooKeeper 的设计完全围绕这个特点展开。

### 5.1.2 核心需求

ZooKeeper 的共识需求可以归纳为三点：

1. **写顺序一致**：所有 Follower 看到的写操作顺序必须和 Leader 完全相同。如果 Leader 先写 A 再写 B，Follower 不能先看到 B 再看到 A。

2. **高可用**：Leader 故障时能快速选出新 Leader（通常在数百毫秒到几秒内），不影响服务。

3. **低延迟读**：读请求不走共识协议，直接从本地内存读取——保证毫秒级响应。

### 5.1.3 为什么不直接用 Paxos？

第4章我们学了 Paxos。既然 Paxos 已经解决了共识问题，为什么 ZooKeeper 不直接用 Multi-Paxos？原因有三：

**原因一：Paxos 不保证 primary order（主序）**

Multi-Paxos 的每个 log entry 独立运行 Paxos——不同 entry 的值之间没有强顺序保证。虽然 Leader 串行提案会自然产生顺序，但 Paxos 协议本身不强制"Leader 提出的顺序 = Follower 看到的顺序"。

ZooKeeper 需要更强的保证：**Leader 执行写操作的顺序就是 Follower 看到的顺序**——这叫 primary order。ZAB（ZooKeeper Atomic Broadcast，ZooKeeper 原子广播协议）专门为此设计。

**原因二：Paxos 的恢复机制复杂**

Multi-Paxos 的 Leader 切换需要对新 entry 逐一运行 Prepare 恢复——工程实现复杂。ZooKeeper 需要更简单的恢复机制：新 Leader 选出后，直接与所有 Follower 同步日志，确保日志一致后开始广播——这就是 ZAB 的 Synchronization 阶段。

**原因三：Paxos 的工程标准不统一**

第4章提到，Multi-Paxos 论文只给了思路，没有工程标准。每个实现都不同（Chubby/libpaxos/PhxPaxos 各有差异）。ZooKeeper 团队选择设计一个更简单、更适合自身场景的协议——ZAB。

> **跨书对照**：
> - **释慧利（本章）**：从"ZooKeeper 的需求"切入，讲 ZAB 如何满足这些需求
> - **唐伟志《深入理解分布式系统》第4章**：从"Paxos vs Raft vs ZAB 对比"切入，提供横向视角
> - **江峰《分布式高可用算法》第8章**：从"共识的形式化"角度，将 ZAB 归类为"主备复制协议"而非"共识协议"——差异在于"共识保证值相同，主备复制保证顺序相同"

---

## 5.2 ZAB 的设计思路——主备复制而非共识

### 5.2.1 核心区别：共识 vs 原子广播

理解 ZAB 的关键，是理解它和 Paxos 的定位差异：

| 维度 | Paxos（共识协议） | ZAB（原子广播协议） |
|------|-----------------|-------------------|
| **目标** | 就一个值达成一致 | 将 Leader 的操作顺序复制到所有 Follower |
| **保证** | Safety：只有一个值被选定 | Primary order：Follower 看到的顺序 = Leader 的顺序 |
| **编号** | 提案编号 n（全局递增） | zxid（epoch + counter，epoch 对应任期） |
| **角色** | Proposer/Acceptor/Learner | Leader/Follower/Observer |
| **两阶段** | Prepare/Promise → Accept/Accepted | Discovery → Synchronization → Broadcast（三阶段） |
| **恢复** | 新 Leader 对每个 entry 运行 Prepare | 新 Leader 与所有 Follower 同步日志 |

**关键洞察**：Paxos 解决的是"多个节点就一个值达成一致"，ZAB 解决的是"Leader 的操作序列被所有 Follower 原子地复制"。两者有交集（都使用多数派），但目标不同。

### 5.2.2 ZAB 的核心思想——Leader 是唯一的写入入口

ZAB 的设计极其简单：**所有写请求都经过 Leader，Leader 按顺序为每个写操作分配一个全局唯一的事务 ID（zxid），然后通过原子广播将操作复制到所有 Follower。**

```
Client ──写请求──► Leader
                     │
                 分配 zxid
                 写入本地日志
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
    Follower-1  Follower-2  Follower-3
    (按 zxid    (按 zxid    (按 zxid
     顺序应用)   顺序应用)   顺序应用)
```

**Primary order 的保证**：因为所有写操作都经过 Leader，Leader 按顺序分配 zxid，Follower 按 zxid 顺序应用——顺序自然一致。这比 Paxos 的"每个 entry 独立共识"简单得多。

### 5.2.3 原子广播——两阶段提交的简化

ZAB 的广播阶段本质上是一个简化的两阶段提交：

**阶段一：Leader 发送 PROPOSAL**

```
Leader ──PROPOSAL(zxid, data)──► 所有 Follower
Follower: 写入本地日志，返回 ACK
Leader: 收到多数派 ACK → 进入阶段二
```

**阶段二：Leader 发送 COMMIT**

```
Leader ──COMMIT(zxid)──► 所有 Follower
Follower: 提交本地日志（应用到状态机）
```

**和 2PC 的关键区别**：
- 2PC 要求**所有**参与者 ACK，ZAB 只要求**多数派** ACK
- 2PC 的协调者崩溃导致阻塞，ZAB 的 Leader 崩溃会触发快速选举 + 日志同步
- ZAB 的 COMMIT 按 zxid 顺序发送——保证了 primary order

> **注意**：这里的"两阶段"是广播阶段内部的两阶段（PROPOSAL → COMMIT），和 Paxos 的两阶段（Prepare → Accept）不同。ZAB 的完整生命周期是三阶段（Discovery → Synchronization → Broadcast），广播阶段只是其中之一。

---

## 5.3 ZAB 的三阶段——完整生命周期

ZAB 协议的运行分为三个阶段，构成 Leader 的完整生命周期：

### 5.3.1 阶段一：Discovery（发现/选举阶段）

**目标**：选出新的 Leader，并确定集群中最大的 zxid。

**触发时机**：Leader 崩溃、网络分区导致 Follower 与 Leader 失联、集群启动。

**选举流程**：

每个节点最初都处于 LOOKING（寻找 Leader）状态，广播自己的投票信息：

```
Node-1 (zxid=100): "我投 Node-1，我的最大 zxid 是 100"
Node-2 (zxid=105): "我投 Node-2，我的最大 zxid 是 105"
Node-3 (zxid=103): "我投 Node-3，我的最大 zxid 是 103"
```

收到其他节点的投票后，每个节点比较：
1. **先比 zxid**：zxid 大的优先（数据更新的节点更适合当 Leader）
2. **zxid 相同比比 node_id**：node_id 大的优先（约定俗成的 tiebreaker）

```
Node-1 收到 Node-2 的投票(zxid=105) > 自己的(100)
→ Node-1 更新投票："我投 Node-2，zxid=105"

Node-3 收到 Node-2 的投票(zxid=105) > 自己的(103)
→ Node-3 更新投票："我投 Node-2，zxid=105"

→ Node-2 获得多数派投票，成为新 Leader
```

**选举完成后**：
- 获胜节点进入 LEADING 状态，成为 Leader
- 其他节点进入 FOLLOWING 状态，成为 Follower
- 新 epoch 通过收集所有 Follower 的 acceptedEpoch 确定：new_epoch = max(acceptedEpoch) + 1（不是自己 +1）

**关键点**：选举优先选 zxid 最大的节点——这保证了数据最新的节点成为 Leader，减少日志同步的工作量。

### 5.3.2 阶段二：Synchronization（同步阶段）

**目标**：确保新 Leader 和所有 Follower 的日志一致，为广播阶段做准备。

**问题**：新 Leader 选出后，各个 Follower 的日志可能不一致：
- 有些 Follower 可能落后（缺少部分已提交的事务）
- 有些 Follower 可能领先（包含 Leader 未提交的事务——"幽灵事务"）

**同步流程**：

```
1. Leader 发送 NEWLEADER(epoch, last_zxid) 给所有 Follower
   - epoch：新 Leader 的任期号
   - last_zxid：Leader 本地最新的 zxid

2. Follower 收到 NEWLEADER 后：
   a. 对比自己的日志和 Leader 的日志
   b. 如果 Follower 有 Leader 没有的事务（幽灵事务）→ 丢弃
   c. 如果 Follower 缺少 Leader 有的事务 → 请求补齐
   d. 日志对齐后，回复 ACKNEWLEADER

3. Leader 收到多数派 ACKNEWLEADER → 同步完成
4. Leader 发送 UPTODATE 通知所有 Follower：可以开始正常服务
```

**幽灵事务处理**：如果旧 Leader 在崩溃前发出了 PROPOSAL 但未发出 COMMIT，这些事务可能被部分 Follower 记录但未被多数派确认。新 Leader 不会继承这些事务——直接让 Follower 丢弃。这和 Paxos 的 Prepare 探测机制不同——ZAB 通过"zxid 比较 + 直接丢弃"来处理，更简单但需要 Leader 选举时选择 zxid 最大的节点。

### 5.3.3 阶段三：Broadcast（广播阶段）

**目标**：Leader 正常处理写请求，通过原子广播复制到所有 Follower。

**正常流程**：

```
T0: Client 发送写请求（如 create /node "data"）给 Leader
T1: Leader 分配 zxid（如 zxid = epoch(2) + counter(106) = 0x20000006A）
T2: Leader 写入本地事务日志
T3: Leader 发送 PROPOSAL(zxid, data) 给所有 Follower
T4: Follower 写入本地事务日志，回复 ACK
T5: Leader 收到多数派 ACK（不等待所有 Follower）
T6: Leader 发送 COMMIT(zxid) 给所有 Follower
T7: Leader 自己提交（应用到内存中的 znode 树）
T8: Follower 收到 COMMIT，提交到本地 znode 树
T9: Leader 返回"操作成功"给 Client
```

**关键点**：
- Leader 在 T5 收到多数派 ACK 后就可以 COMMIT——不等所有 Follower
- COMMIT 按 zxid 顺序发送——保证 Follower 按顺序应用
- Leader 在 T7 提交后立即返回 Client——不等 Follower 提交

### 5.3.4 完整生命周期的时间线

从集群启动到正常运行，ZAB 的状态流转：

```
集群启动
  │
  ▼
Discovery（选举）
  │  选出 Leader，epoch+1
  ▼
Synchronization（同步）
  │  Leader 与 Follower 对齐日志
  │  处理幽灵事务
  ▼
Broadcast（广播）◄────────────────┐
  │  正常处理写请求               │
  │  PROPOSAL → ACK → COMMIT     │
  │                                │
  │  Leader 崩溃或网络分区         │
  └────────────────────────────────┘
  │  重新触发选举
  ▼
Discovery（重新选举）
  │  ...循环...
```

**与 Paxos 的对比**：
- Paxos 的 Leader 切换需要对新 entry 逐一运行 Prepare——复杂
- ZAB 的 Leader 切换只需要"选举 + 同步"两步——简单，但需要丢弃幽灵事务

> **生产对照**：ZooKeeper 的默认配置中，`tickTime=2000ms`（心跳间隔），`initLimit=10`（初始连接超时 = 10 个 tick = 20 秒），`syncLimit=5`（同步超时 = 5 个 tick = 10 秒）。选举通常在几百毫秒到几秒内完成。

---

## 5.4 zxid——ZAB 的核心概念

### 5.4.1 zxid 的结构

zxid（ZooKeeper Transaction ID，ZooKeeper 事务 ID）是 ZAB 协议的核心——它既是事务的唯一标识，也编码了顺序信息。

**zxid 是一个 64 位整数，由两部分组成**：

```
┌─────────────────┬─────────────────────────────┐
│    epoch (32位)  │       counter (32位)        │
└─────────────────┴─────────────────────────────┘
```

- **epoch（任期号）**：高 32 位，标识 Leader 的任期。每次 Leader 切换，epoch 递增。类似于 Paxos 的提案编号 n 和 Raft 的 term。
- **counter（计数器）**：低 32 位，标识当前任期内的事务序号。Leader 每处理一个写请求，counter 递增。

**zxid 的全局唯一性**：因为 epoch 和 counter 都是单调递增的，且 epoch 在 Leader 切换时递增，所以 zxid 全局唯一递增——不会有两个事务有相同的 zxid。

### 5.4.2 zxid 的比较规则

zxid 的比较是**先比 epoch，再比 counter**：

```
zxid_1 = epoch=2, counter=100 → 0x200000064
zxid_2 = epoch=2, counter=105 → 0x200000069
zxid_3 = epoch=3, counter=1   → 0x300000001

比较结果：zxid_3 > zxid_2 > zxid_1
（epoch=3 的任何 zxid 都大于 epoch=2 的任何 zxid）
```

**这个比较规则在 Leader 选举中的作用**：5.3.1 节提到选举优先选 zxid 最大的节点——实际上就是先选 epoch 最大的（最新任期的），epoch 相同时选 counter 最大的（数据最全的）。

### 5.4.3 zxid 与 primary order

zxid 的设计保证了 primary order：

1. Leader 按顺序分配 zxid——zxid 递增
2. Follower 按 zxid 顺序应用事务——顺序与 Leader 一致
3. Leader 切换时 epoch 递增——新 Leader 的 zxid 必然大于旧 Leader 的所有 zxid

**对比 Paxos**：Paxos 的提案编号 n 不编码顺序信息——不同 entry 的 n 可能相同（如果 Leader 没变）。Paxos 的顺序由 entry index（日志位置）保证，不是由提案编号保证。ZAB 把"任期 + 顺序"编码在 zxid 中——更紧凑也更直观。

---

## 5.5 ZAB vs Paxos——本质区别

### 5.5.1 对比表

| 维度 | Paxos | ZAB |
|------|-------|-----|
| **协议类型** | 共识协议（Consensus） | 原子广播协议（Atomic Broadcast） |
| **核心目标** | 多节点就一个值达成一致 | Leader 的操作序列原子复制到 Follower |
| **顺序保证** | 不保证 primary order | 保证 primary order |
| **角色** | Proposer/Acceptor/Learner | Leader/Follower/Observer |
| **编号** | 提案编号 n（全局唯一递增） | zxid = epoch(32) + counter(32) |
| **阶段数** | 2（Prepare → Accept） | 3（Discovery → Synchronization → Broadcast） |
| **Leader 恢复** | 对未确认 entry 逐一 Prepare | 选举 zxid 最大的节点 + 日志同步 |
| **幽灵事务** | Prepare 探测并继承 | 直接丢弃（不继承未提交的事务） |
| **多数派要求** | 2F+1 容忍 F 个故障 | 2F+1 容忍 F 个故障（相同） |
| **适用场景** | 通用共识 | 主备复制（ZooKeeper 场景） |

### 5.5.2 为什么 ZAB 不继承幽灵事务？

Paxos 的 Prepare 阶段会探测已接受的值并继承——即使旧 Proposer 未完成 Accept，新 Proposer 也会"接棒"完成。这保证了"一旦值被部分 Acceptor 接受，它最终会被选定"。

ZAB 选择了不同的策略——**直接丢弃幽灵事务**（旧 Leader 发出 PROPOSAL 但未 COMMIT 的事务）。为什么？

**原因：ZooKeeper 的语义允许丢弃未提交的事务**

ZooKeeper 的写请求是客户端发起的——如果客户端没收到"操作成功"的响应，它会重试。所以丢弃未提交的事务不会违反客户端语义——客户端会重发。

相比之下，Paxos 的 Prepare 继承机制更通用（适用于"一旦提案就不能丢弃"的场景），但更复杂。ZAB 选择了更简单的方案——这符合 ZooKeeper "协调服务而非数据库"的定位。

### 5.5.3 ZAB 是 Paxos 变体吗？

学术界对 ZAB 与 Paxos 的关系有不同看法：

- **ZAB 论文**（Junqueira et al., DSN 2011）明确将 ZAB 与 Paxos 区分——ZAB 有独立的阶段设计，保证 primary order
- **部分学者**认为 ZAB 是 Paxos 的"特化"——因为 ZAB 也使用多数派 + Leader + 日志复制
- **实践视角**：ZAB 和 Multi-Paxos 在工程层面非常相似，但 ZAB 的设计更简单、更适合 ZooKeeper 的场景

**本章的定位**：ZAB 是与 Paxos 同族但独立设计的协议——它借鉴了 Paxos 的多数派思想，但有独立的目标（primary order）、独立的阶段（三阶段）、独立的恢复机制（丢弃幽灵事务）。

> **跨书对照**：江峰《分布式高可用算法》第8章给出通用共识的形式化定义（Agreement/Validity/Termination）——读者可将 ZAB 套入该框架验证（非江峰原文讨论 ZAB）。唐伟志《深入理解分布式系统》第4章将 ZAB、Paxos、Raft 放在同一框架下对比，提供横向视角。

### 5.5.4 VR 传承——ZAB epoch、Raft term、Paxos 编号的共同祖先

一个常见误解是"ZAB 的 epoch 影响了 Raft 的 term"——实际上两者都源自更早的 **VR（Viewstamped Replication）** 协议。

**VR（Viewstamped Replication）** 由 Brian Oki 和 Barbara Liskov 在 1988 年提出（比 Paxos 1990 年还早两年）。VR 使用 **view-number**（视图编号）标识 Leader 的任期——每次 Leader 切换，view-number 递增。

三个协议的"任期编号"对比：

| 协议 | 任期编号 | 提出年份 | 原始论文 |
|------|---------|---------|---------|
| **VR** | view-number | 1988 | Oki & Liskov, "Viewstamped Replication: A New Primary Copy Method to Support Highly-Available Distributed Systems" |
| **Paxos** | 提案编号 n | 1990 | Lamport, "The Part-Time Parliament" |
| **ZAB** | epoch | 2008 | Junqueira et al., "Zab: High-performance broadcast for primary-backup systems" (DSN 2011) |
| **Raft** | term | 2014 | Ongaro & Ousterhout, "In Search of an Understandable Consensus Algorithm" |

**关键事实**：Raft 论文明确引用了 VR（不是 ZAB）作为 term 的灵感来源。ZAB 的 epoch 和 Raft 的 term 是**同源概念**（都来自 VR 的 view-number），不是直接因果关系。

**为什么这很重要？** 理解 VR 传承关系可以避免"哪个协议影响了哪个"的无谓争论——VR（1988）是共同祖先，Paxos（1990）、ZAB（2008）、Raft（2014）是三条平行的工程化路径，各自针对不同场景优化。

### 5.5.5 ZAB 的持久化状态——什么必须落盘？

和 Paxos 一样（第4章 4.8.1 节），ZAB 的安全性依赖 Acceptor 的持久化状态。如果节点重启后"忘记"了关键信息，安全性会被破坏。

**必须持久化的四个状态**：

| 状态 | 含义 | 持久化要求 | 重启后丢失的后果 |
|------|------|-----------|----------------|
| `acceptedEpoch` | 已接受的最大 epoch | 必须 fsync | 重启后可能接受旧 epoch 的 Leader → 破坏 Safety |
| `lastZxid` | 本地最新的事务 zxid | 必须 fsync | 重启后不知道自己有哪些事务 → 同步异常 |
| `dataTree` | 内存中的 znode 树（完整状态） | 通过快照 + 日志恢复 | 数据丢失 |
| `transactions` | 事务日志（WAL） | 每次 PROPOSAL 必须 fsync | 已提交事务丢失 → 数据不一致 |

**事务日志就是 WAL**：ZooKeeper 的事务日志（transaction log）本身就是 WAL（Write-Ahead Log，预写日志）——不是两个独立的文件。写流程是：先写事务日志（磁盘顺序写 + fsync），再改内存（znode 树）。

**快照 + 日志的恢复流程**：
```
1. 加载最新的快照文件 → 恢复到快照时刻的状态
2. 重放快照之后的事务日志 → 追赶到崩溃前的状态
3. 向 Leader 上报 lastZxid → 进入 Synchronization 阶段
```

**与 Paxos 持久化的对比**：
- Paxos 持久化：`promised_n`（承诺编号）+ `accepted_proposal`（已接受提案）
- ZAB 持久化：`acceptedEpoch`（已接受任期）+ `lastZxid`（最新事务）+ 事务日志
- 两者本质上都是"承诺不退让 + 已接受不遗忘"——只是术语和实现不同

---

## 5.6 ZooKeeper 的工程实现

### 5.6.1 数据模型——树形 znode

ZooKeeper 的数据模型是一棵树，每个节点叫 **znode**（ZooKeeper Node）：

```
/
├── /app1
│   ├── /app1/config       (配置数据)
│   ├── /app1/leader       (当前 Leader 选举结果)
│   └── /app1/members      (成员列表)
│       ├── /app1/members/node-1  (临时节点)
│       └── /app1/members/node-2  (临时节点)
└── /app2
    └── /app2/locks
        └── /app2/locks/lock-001  (分布式锁)
```

**znode 的类型**：
- **持久节点（Persistent Node）**：创建后一直存在，直到被显式删除
- **临时节点（Ephemeral Node）**：与客户端会话绑定，客户端断开后自动删除——用于成员管理和 Leader 选举
- **顺序节点（Sequential Node）**：创建时自动追加递增序号——用于分布式锁和队列

### 5.6.2 读写流程

**写流程**（经过 ZAB 共识）：

```
Client ──写请求──► Leader（或 Follower 转发给 Leader）
Leader:
  1. 分配 zxid
  2. 写本地事务日志（即 WAL，Write-Ahead Log，预写日志——先写日志再改内存，崩溃后用日志恢复）
  3. 发送 PROPOSAL 给所有 Follower
  4. 等待多数派 ACK
  5. 发送 COMMIT
  6. 应用到 znode 树（内存）
  7. 返回 Client "操作成功"
```

**读流程**（本地直接读，不走共识）：

```
Client ──读请求──► 任意 Follower（或 Leader）
Follower:
  1. 直接从本地 znode 树（内存）读取
  2. 返回结果
```

**关键点**：读请求**不走 ZAB 协议**——直接从本地内存读取。这保证了读的低延迟（亚毫秒级），但代价是读可能看到旧数据（Follower 的日志可能落后于 Leader）。

### 5.6.3 Watch 机制——数据变更通知

ZooKeeper 的 Watch 机制允许客户端在 znode 数据变更时收到通知：

```
Client ──getData("/config", watch=true)──► ZooKeeper
ZooKeeper: 返回当前数据 + 注册 Watch

（数据变更）
ZooKeeper ──WatchEvent("/config", NodeDataChanged)──► Client
Client: 收到通知，重新 getData 获取最新数据
```

**Watch 的特点**：
- **一次性触发**：Watch 触发后自动注销——需要重新注册才能监听下一次变更
- **有序性**：Client 收到 Watch 事件的顺序与 ZAB 的 zxid 顺序一致——不会乱序
- **轻量**：Watch 通知只包含事件类型和 znode 路径，不包含变更后的数据——Client 需要重新读取

**生产案例**：Dubbo 使用 ZooKeeper 做服务注册——服务提供者注册临时节点 `/dubbo/com.example.Service/providers/provider-001`，服务消费者 Watch 这个目录——提供者上线/下线时消费者收到通知，更新本地路由表。

### 5.6.4 Observer 角色——读扩展

ZooKeeper 3.3 引入了 Observer 角色——不参与投票（不是 Acceptor），只接收 Leader 的广播并同步状态：

```
Leader ──PROPOSAL/COMMIT──► Follower（参与投票）
Leader ──PROPOSAL/COMMIT──► Observer（不投票，只同步）
```

**Observer 的作用**：
- **读扩展**：Observer 可以处理读请求——跨地域部署 Observer 让远程客户端低延迟读取
- **不增加写延迟**：Observer 不参与多数派投票——增加 Observer 不会增加写请求的 ACK 等待时间
- **不增加选举复杂度**：Observer 不参与 Leader 选举——集群中 Observer 的数量不影响选举速度

**生产案例**：跨地域 ZooKeeper 部署——北京机房部署 3 个 Voting Member（Leader + 2 Followers），上海机房部署 2 个 Observer。上海的客户端从本地 Observer 读取（延迟 <1ms），写请求转发到北京 Leader（延迟 ~30ms）。

### 5.6.5 ZooKeeper 的一致性保证

ZooKeeper 提供的是**顺序一致性**（Sequential Consistency），不是线性一致性：

- **顺序一致性**：所有客户端看到的写操作顺序相同（ZAB 保证），但不保证读到的数据是最新的
- **线性一致性**：读必返回最新写入的值——ZooKeeper **不保证**这一点

**为什么不是线性一致性？** 因为读请求直接从本地 Follower 读取，不走共识——Follower 可能落后于 Leader。如果需要线性一致性读，客户端需要：
1. 先调用 `sync()` 操作——强制 Follower 追上 Leader 的最新状态
2. 然后再读

> **跨书对照**：第1章 1.6.4 节定义了一致性谱系。ZooKeeper 的顺序一致性位于线性一致性之下——比因果一致性强（保证全局顺序），但比线性一致性弱（不保证实时性）。

---

## 5.7 ZAB 完整消息类型表

ZAB 协议涉及 9 种核心消息，按阶段分组：

| 消息类型 | 发送方 → 接收方 | 所属阶段 | 用途 |
|---------|---------------|---------|------|
| **FOLLOWINFO** | Follower → 准 Leader | Discovery | 上报自己的 `acceptedEpoch` 和 `lastZxid` |
| **ACKEPOCH** | Follower → 准 Leader | Discovery | 确认新 epoch，包含 `acceptedEpoch` 和 `peerLastZxid` |
| **NEWLEADER** | Leader → Follower | Synchronization | 通知"我是新 Leader"，包含 `new_epoch` 和 `lastZxid` |
| **DIFF** | Leader → Follower | Synchronization | 发送差异事务（Follower 落后但日志不冲突） |
| **TRUNC** | Leader → Follower | Synchronization | 通知 Follower 截断幽灵事务 |
| **SNAP** | Leader → Follower | Synchronization | 发送完整快照（差异过大时） |
| **PROPOSAL** | Leader → Follower | Broadcast | 提议事务，包含 `zxid` 和数据 |
| **ACK** | Follower → Leader | Broadcast | 确认已收到 PROPOSAL 并写入本地日志 |
| **COMMIT** | Leader → Follower | Broadcast | 通知 Follower 提交事务 |

**消息交互的完整时序**：

```
Discovery:  Follower ──FOLLOWINFO──► 准Leader
            Follower ◄──NEWLEADER── 准Leader
            Follower ──ACKEPOCH───► Leader

Synchronization:
            Follower ◄──DIFF/TRUNC/SNAP── Leader
            Follower ──ACKNEWLEADER──► Leader
            Follower ◄──UPTODATE─── Leader

Broadcast:  Follower ◄──PROPOSAL── Leader
            Follower ──ACK───────► Leader
            Follower ◄──COMMIT──── Leader
```

---

## 5.8 Pipeline 流水线优化

### 5.8.1 串行广播的性能瓶颈

如果 Leader 每次 PROPOSAL 都等多数派 ACK 后才发 COMMIT，再发下一个 PROPOSAL——写入吞吐被限制为 `1 / (2 × RTT)`。同机房 RTT 1ms，理论上限约 500 写/秒——远不够 ZooKeeper 的目标。

### 5.8.2 Pipeline 解决方案

**Pipeline（流水线）**：Leader 连续发送多个 PROPOSAL，不必等前一个的 ACK——收到 ACK 后按顺序发对应的 COMMIT。

```
Leader:  PROPOSAL(1) → PROPOSAL(2) → PROPOSAL(3) → ...
                           │              │              │
Follower:                ACK(1)         ACK(2)         ACK(3)
                           │              │              │
Leader:                COMMIT(1)      COMMIT(2)      COMMIT(3)
```

**关键保证**：
- PROPOSAL 按 zxid 顺序发送——Follower 按顺序写入日志
- COMMIT 也按 zxid 顺序发送——Follower 按顺序提交
- 即使 PROPOSAL(2) 的 ACK 先于 PROPOSAL(1) 到达，COMMIT(1) 仍先于 COMMIT(2) 发送

**性能提升**：Pipeline 让写入吞吐从 `1 / (2 × RTT)` 提升到接近网络带宽上限——典型 ZooKeeper 集群写吞吐可达 5,000-20,000 写/秒。

### 5.8.3 Pipeline 带来的挑战——Leader 切换时的未提交事务

Leader 崩溃时，可能有很多已 PROPOSAL 但未 COMMIT 的事务。这些事务中：
- 已被多数派 ACK 的 → 新 Leader 会在 Synchronization 阶段补发 COMMIT
- 未被多数派 ACK 的 → 新 Leader 发送 TRUNC 丢弃

这就是 5.3.2 节 DIFF/TRUNC/SNAP 机制在 Pipeline 场景下的具体应用。

---

## 5.9 ZAB 安全性证明草图

### 5.9.1 要证明什么

**Safety 属性**：
1. **已提交事务不丢**：一旦事务被多数派 Follower ACK 并由 Leader COMMIT，Leader 切换后该事务不会丢失
2. **Primary order**：所有 Follower 看到的事务顺序相同

### 5.9.2 "已提交事务不丢"的证明

**前提**：事务 T 被 Leader-1 COMMIT → T 已被多数派 Follower ACK → T 在多数派 Follower 的日志中。

**推导**：
1. Leader-1 崩溃，新 Leader-2 选出
2. Leader-2 的选举需要多数派投票——这个多数派与 ACK T 的多数派有交集
3. 交集节点拥有 T 的日志
4. Leader-2 选举时选 zxid 最大的节点——T 的 zxid ≤ Leader-2 的 zxid
5. Synchronization 阶段，Leader-2 会发送 DIFF 给落后节点——T 会被同步
6. 因此 T 不会丢失 ✓

### 5.9.3 Primary order 的证明

**前提**：所有事务的 zxid 全局唯一递增（epoch + counter 保证）。

**推导**：
1. Leader 按顺序分配 zxid → PROPOSAL 按 zxid 顺序发送
2. Follower 按 zxid 顺序写入日志 → COMMIT 按 zxid 顺序到达
3. Follower 按 zxid 顺序提交 → 状态机按相同顺序应用
4. Leader 切换时 epoch 递增 → 新 Leader 的 zxid 必然大于旧 Leader 的所有 zxid
5. 因此所有 Follower 看到的事务顺序相同 ✓

> **跨书对照**：江峰《分布式高可用算法》第8章给出通用共识的形式化定义——读者可将 ZAB 套入该框架验证（非江峰原文讨论 ZAB）。唐伟志《深入理解分布式系统》第4章将 ZAB、Paxos、Raft 放在同一框架下对比，提供横向视角——差异在于"本节纵向深入 ZAB 的安全性，唐伟志横向对比三协议"。

---

## 5.10 ZooKeeper 生产实践

### 5.10.1 Chubby vs ZooKeeper——为什么 Google 选 Paxos，Yahoo 选 ZAB

| 维度 | Chubby（Google） | ZooKeeper（Yahoo/Apache） |
|------|-----------------|-------------------------|
| 共识协议 | Multi-Paxos | ZAB |
| 设计目标 | 分布式锁服务（低频写） | 协调服务（高频读 + 中频写） |
| 数据模型 | 文件系统（文件 = 锁） | 树形 znode |
| 一致性 | 线性一致性（Chubby 客户端缓存 + 缓存一致性） | 顺序一致性（读可能落后） |
| 客户端缓存 | 支持（减少读请求） | 不支持（Watch 机制替代） |
| 实现语言 | C++ | Java |

**为什么 Google 选 Paxos？** Chubby 设计于 2003-2004 年，当时 Paxos 是唯一的成熟共识算法。Google 有 Lamport 的理论支持，直接用 Multi-Paxos 是自然选择。

**为什么 Yahoo 选 ZAB？** ZooKeeper 设计于 2007-2008 年。Yahoo 团队认为 Multi-Paxos 的恢复机制复杂，且 ZooKeeper 需要 primary order（保证客户端看到的写顺序一致）——Multi-Paxos 不天然保证这一点。ZAB 专门为"主备复制 + primary order"设计，更简单更适合。

### 5.10.2 Kafka 去 ZK 化——ZAB 的退场

Kafka 2.8 之前使用 ZooKeeper 管理 Broker 元数据和 Controller 选举。但 ZooKeeper 成为 Kafka 扩展的瓶颈：

1. **元数据规模上限**：ZooKeeper 的 znode 数量受限于内存（建议 <100 万），大型 Kafka 集群（>10,000 分区）逼近上限
2. **Controller 选举慢**：Leader 切换需要 ZAB 选举 + 同步，大集群可能耗时数十秒
3. **运维复杂**：需要同时维护 Kafka 和 ZooKeeper 两套集群

**KRaft 模式**（Kafka Raft）：Kafka 2.8 引入，3.3 默认可用，**4.0（2024）完全移除 ZooKeeper 依赖**。KRaft 使用 Raft 变体管理元数据日志——元数据本身作为 Raft 日志条目存储，Controller 节点通过 Raft 选举。

**启示**：ZAB 适合"小数据量 + 高读"的协调场景。当元数据规模增长到百万级以上时，ZAB 的单 Leader 写入瓶颈和 ZK 的内存模型成为限制——这是 Kafka 转向 Raft 的根本原因。

### 5.10.3 ZooKeeper 典型故障案例

**案例一：JVM GC 导致 Session 超时级联**

ZooKeeper 对 JVM 停顿（GC pause）极其敏感。如果 Follower 的 JVM Full GC 耗时 > `tickTime × syncLimit`（默认 10 秒），Leader 会认为 Follower 已宕机，将其踢出集群。大量 Follower 同时 GC 会导致集群可用节点数 < 多数派 → 集群不可用。

**生产建议**：
- 使用 G1 或 ZGC（低停顿 GC）
- JVM 堆大小适中（建议 4-8GB，过大导致 Full GC 停顿长）
- 监控 JVM GC 停顿时间，告警阈值 < `tickTime`

**案例二：网络分区导致脑裂**

网络分区时，少数派 Follower 可能短暂认为 Leader 存活（心跳未超时），而多数派已选出新 Leader。少数派 Follower 的读请求可能返回旧数据——这不是"脑裂"（ZAB 保证不会有两个 Leader 同时接受写），但客户端可能看到不一致的读结果。

**生产建议**：跨机房部署时，确保多数派节点在同一机房——减少跨网络分区的概率。

### 5.10.4 性能数据与容量规划

| 指标 | 典型值 | 说明 |
|------|--------|------|
| 集群规模 | 3 或 5 节点 | 5 节点容忍 2 故障，但写延迟略增 |
| 读 QPS | ~10 万/秒/节点 | 本地内存读，不走共识 |
| 写 QPS | ~1 万/秒/集群 | 受 Leader 单点 + ZAB 广播限制 |
| 写延迟 | 同机房 ~1ms，跨机房 ~30-100ms | 取决于多数派 RTT |
| 最大 znode 数 | <100 万 | 受限于 JVM 堆内存 |
| 单 znode 数据 | <1 MB | `jute.maxbuffer` 默认 1,040,575 字节 |
| Session timeout | 30-60 秒 | 生产常用值，太短易误判超时 |

### 5.10.5 关键生产参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `tickTime` | 2000ms | 基本时间单元 |
| `initLimit` | 10 | 初始连接超时 = 10 × tickTime |
| `syncLimit` | 5 | 同步超时 = 5 × tickTime |
| `maxClientCnxns` | 60 | 单 IP 最大客户端连接数 |
| `autopurge.snapRetainCount` | 3 | 保留的快照数 |
| `autopurge.purgeInterval` | 1 | 自动清理间隔（小时） |
| `snapCount` | 100,000 | 快照触发的事务数阈值 |
| `jute.maxbuffer` | 1,040,575 | 单 znode 最大数据量（字节） |
| `leaderServers` | yes | Leader 是否接受客户端连接 |

### 5.10.6 ZooKeeper vs etcd——协调服务的两大选择

作为 Java 工程师，你最可能接触的两个协调服务就是 ZooKeeper 和 etcd。两者的对比：

| 维度 | ZooKeeper | etcd |
|------|-----------|------|
| **共识协议** | ZAB | Raft |
| **数据模型** | 树形 znode（类似文件系统） | 扁平 KV（key-value，支持前缀范围查询） |
| **一致性** | 顺序一致性（读可能落后） | 线性一致性（默认 ReadIndex Read） |
| **Watch** | 一次性触发（3.6+ 支持持久 Watch） | 持续 Watch（Watch 不会自动取消） |
| **API** | 自定义 TCP 协议 + 客户端 | gRPC + RESTful |
| **实现语言** | Java | Go |
| **典型用户** | Kafka(旧)、HBase、Dubbo、Solr | Kubernetes、CoreDNS、Rook |
| **写入性能** | ~1 万/秒 | ~1-3 万/秒 |
| **读性能** | ~10 万/秒（本地读） | ~1-3 万/秒（ReadIndex Read） |
| **数据量上限** | <100 万 znode（内存限制） | ~几个 GB（bbolt 磁盘存储） |

**关键差异分析**：
1. **一致性**：ZooKeeper 的读可能看到旧数据（Follower 本地读），etcd 默认线性一致性读（ReadIndex）。如果业务对"读必返回最新值"有要求，etcd 更安全
2. **Watch**：ZooKeeper 的 Watch 一次性触发——收到通知后必须重新注册，中间可能有遗漏。etcd 的 Watch 是持续性的——一次注册持续监听直到取消
3. **数据模型**：ZooKeeper 的树形结构适合"配置层级 + 临时节点"场景（如服务注册），etcd 的 KV + 前缀查询更适合"配置 + 元数据"场景（如 Kubernetes 的 Pod 信息）
4. **生态**：ZooKeeper 在 Hadoop/HBase/Kafka(旧) 生态中占主导，etcd 在 Kubernetes/Cloud Native 生态中占主导

### 5.10.7 ZooKeeper 的局限性——为什么新系统转向 Raft

ZooKeeper 的局限性是"新系统选择 Raft 而非 ZAB"的根本原因：

**局限一：单 Leader 写入瓶颈**
ZAB 的所有写请求都经过 Leader——Leader 的 CPU 和网络带宽是写入上限。典型 ZooKeeper 集群写吞吐约 1 万/秒，无法通过增加节点提升（更多节点反而增加 Leader 的广播开销）。

对比：TiKV 的 Multi-Raft 架构把数据分片为百万个 Region，每个 Region 有独立的 Leader——写入压力分散到整个集群，吞吐可达数十万/秒。

**局限二：内存模型限制数据量**
ZooKeeper 的所有 znode 数据都在内存中——数据量受限于 JVM 堆大小（建议 <4GB，过大导致 GC 停顿）。生产建议 znode 数 <100 万，单 znode 数据 <1MB。

对比：etcd 使用 bbolt 磁盘存储，数据量可达几 GB，不受内存限制。

**局限三：JVM GC 敏感**
ZooKeeper 运行在 JVM 上——Full GC 停顿可能导致 Session 超时、Leader 选举。这是 ZooKeeper 运维的已知痛点。

对比：etcd 用 Go 编写——没有 GC 停顿问题（Go 的 GC 停顿通常 <1ms）。

**局限四：跨地域延迟**
ZAB 的写请求需要多数派 ACK——跨地域部署时，写延迟 = 跨地域 RTT。5 节点跨 3 个地域的写延迟可能 >100ms。

对比：Spanner 使用 Paxos + TrueTime，通过原子钟减少跨地域写延迟的影响。

**这些局限性解释了为什么 Kafka 4.0 移除 ZooKeeper 依赖、新系统（etcd/TiKV/Consul）选择 Raft 而非 ZAB**——ZAB 适合"小数据量 + 高读 + 中等写"的协调场景，不适合大数据量的元数据管理。

---

## 5.11 本章总结

### 8 个核心要点

1. **ZAB 是原子广播协议，不是共识协议**。Paxos 解决"就一个值达成一致"，ZAB 解决"Leader 的操作序列原子复制到 Follower"。两者有交集（都使用多数派），但目标不同。

2. **ZAB 保证 primary order**——Follower 看到的写操作顺序与 Leader 完全相同。Paxos 不保证这一点——每个 entry 独立共识，顺序由日志位置保证而非协议保证。

3. **ZAB 的三阶段**：Discovery（选举 Leader）→ Synchronization（同步日志）→ Broadcast（原子广播）。Broadcast 内部是两阶段（PROPOSAL → COMMIT），和 Paxos 的两阶段（Prepare → Accept）不同。

4. **zxid = epoch(32位) + counter(32位)**。epoch 标识 Leader 任期（类似 Paxos 的 n 和 Raft 的 term），counter 标识任期内事务序号。zxid 全局唯一递增，编码了顺序信息。

5. **Leader 选举优先选 zxid 最大的节点**——保证数据最新的节点成为 Leader，减少同步工作量。选举时先比 epoch，再比 counter，最后比 node_id。

6. **ZAB 直接丢弃幽灵事务**（未 COMMIT 的 PROPOSAL），而 Paxos 通过 Prepare 继承。ZAB 更简单，但依赖客户端重试未确认的写请求。

7. **ZooKeeper 的读请求不走共识**——直接从本地内存读取，保证低延迟但可能读到旧数据。提供的是顺序一致性（非线性一致性）。需要线性一致性读时，先 `sync()` 再读。

8. **Observer 角色实现读扩展**——不参与投票，只同步状态。跨地域部署 Observer 让远程客户端低延迟读取，不增加写延迟和选举复杂度。

### 第6章预告

第6章将聚焦 Raft——共识算法的"宠儿"。Raft 和 ZAB 一样使用了强 Leader 模型，但 Raft 的设计目标是"可理解性"——用更清晰的角色定义和更强的 Leader 模型，大幅降低共识算法的实现难度。我们将完整推导 Raft 的三个子问题：领导人选举、日志复制、安全性。

---
