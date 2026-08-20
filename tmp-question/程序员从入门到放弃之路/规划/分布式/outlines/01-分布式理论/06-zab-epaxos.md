# ZAB 与 EPaxos — Leader 全序和冲突依赖的两种共识路线

> Cluster B: 14 KPs | 依赖: 04-paxos-core、05-multi-paxos-raft | 读者基线: Paxos/Raft、quorum、日志、Leader
> 读者处境: Paxos/Raft 都把 Leader 放在核心位置；本篇回答 ZooKeeper 为什么采用 ZAB，以及 EPaxos 如何在冲突较少时减少中心 Leader 依赖
> 打开新视角: 共识协议的差异不是“谁更先进”，而是**全序广播、协调服务语义、命令冲突关系和跨地域延迟**的不同取舍

---

### 概念依赖链

```
04 Paxos + 05 Raft → 本篇: ZAB/EPaxos
  ├─ §1 ZAB阶段与zxid(选主/发现/同步/广播)
  ├─ §2 ZooKeeper请求处理链(协议与状态机结合)
  ├─ §3 EPaxos(PreAccept/依赖图/冲突命令)
  └─ §4 选择边界(ZAB/Raft/EPaxos)
先讲: ZAB 全序 → 请求落地 → EPaxos 冲突依赖 → 选型
后续依赖: 07-paxos-variants(Paxos 家族变种)
```

### 叙事顺序

1. 问题引入——ZooKeeper 为什么需要自己的原子广播，EPaxos 又为什么想减少 Leader 依赖？
2. ZAB——选主、历史同步和 zxid 全序
3. ZooKeeper 请求链——从 create 到 DataTree
4. EPaxos——PreAccept 与冲突依赖图
5. 三种路线选型
6. 收束

### 1. ZAB — 为协调服务优化的原子广播

场景提示: ZooKeeper 多个节点都要维护 znode 树，写请求如何保证每个节点按相同顺序应用？ [写作时展开]

关键设计: ZAB 把 Leader 选举、历史发现、同步和正常广播组织成协调服务需要的阶段；zxid 为事务提供全序坐标：

```[pseudocode]
Leader Election
  → 选出当前 epoch 的 Leader

Discovery
  → 收集成员历史/事务状态
  → 确定新 epoch 的历史边界

Synchronization
  → Follower 追上 Leader 可接受的历史
  → 达成服务启动条件

Broadcast
  → Leader 给事务分配 zxid
  → Proposal → 多数 ACK
  → Commit/按 zxid 顺序 apply
```

Why: 为什么 ZooKeeper 需要全序事务，而不是只对冲突 znode 做依赖排序？——**协调服务的顺序节点、锁、watch 和状态机语义常常依赖一个清晰的全局事务序列**；ZAB 与 ZooKeeper 的 DataTree、watch 和持久化请求链紧密结合。不能简单把 ZAB 说成“Paxos 加 zxid”，它还包含历史同步和服务恢复阶段。 [分布式理论: ZAB 的协议语义与 ZooKeeper 状态机/会话模型绑定]

比喻锚点: ZAB 像一个有总账编号的协调办公室：所有变更先由总负责人编号，再按同一顺序发给各分支，分支不能自行换序。 [写作时展开]

### 2. ZooKeeper 请求处理链 — 共识如何落到状态机

场景提示: 客户端调用 `create /path data`，从请求进入到内存树和事务日志，经过了哪些处理器？ [写作时展开]

关键设计: ZooKeeper 把请求预处理、日志、提议、提交和状态机应用拆成处理器链：

```[pseudocode]
客户端 create/setData
  → PrepRequestProcessor
      校验/生成事务记录
  → SyncRequestProcessor
      写事务日志/处理持久化队列
  → ProposalRequestProcessor
      向 Followers 广播 Proposal
  → CommitProcessor
      收集多数 ACK
      → committedRequests
  → FinalRequestProcessor
      Apply 到 DataTree
      → 触发相关 watch
```

Why: 为什么“收到多数 ACK”后还要区分 committed queue 和 applied queue？——**协议提交与状态机应用是不同阶段**：先确保事务已经达成协议，再按顺序应用到内存树和触发观察者；这让状态机不会因为网络/处理器调度乱序而看到不一致状态。实际处理器名称和线程实现随 ZooKeeper 版本变化，应以目标源码为准。 [分布式理论: 共识层决定 chosen/commit，状态机层负责 apply，二者边界要清楚]

比喻锚点: 请求处理链像银行：先验单、写总账、发给分行确认，确认完成后才更新柜台余额并通知客户。 [写作时展开]

### 3. EPaxos — 不按全局日志顺序执行，而按冲突依赖排序

场景提示: 两个客户端分别修改不同 key，为什么必须让它们等待同一个 Leader 和全局日志位置？ [写作时展开]

关键设计: EPaxos 让每个 Replica 都可以提出命令，只为冲突命令建立依赖关系：

```[pseudocode]
Propose(command)
  → PreAccept 发给 quorum
  → Replica 根据本地依赖历史计算 dependency set
  → 返回序号/依赖

强路径:
  quorum 返回兼容依赖
  → 直接 Commit/执行

弱路径:
  依赖集合不一致或发生竞争
  → 额外 Accept/Commit 阶段

执行:
  非冲突命令可并行/乱序
  冲突命令按依赖图顺序执行
```

Why: 为什么去掉中心 Leader 后不是“免费低延迟”？——**每个 Replica 都要计算和传播依赖图，冲突命令还可能触发额外阶段；冲突率高时，去中心化优势会下降**。EPaxos 的正确性依赖命令冲突定义、quorum 和依赖闭包，不是所有数据库写操作都天然可独立。 [分布式理论: EPaxos 把全序成本转成冲突检测与依赖图成本]

比喻锚点: 全局 Leader 像一个总调度员排所有订单，EPaxos 像各仓库先独立处理不冲突订单，只有抢同一货架的订单才互相协调。 [写作时展开]

### 4. ZAB、Raft 与 EPaxos — 选择的是工作负载和语义

场景提示: 新系统应该直接选 Raft，还是使用 ZAB/EPaxos？ [写作时展开]

关键设计: 三者优化目标不同：

```[pseudocode]
Raft:
  Leader + 强日志前缀 + 清晰选举
  → 通用复制状态机/工程可理解性

ZAB:
  Leader + zxid 全序 + ZooKeeper 会话/watch/状态机
  → 协调服务生态深度绑定

EPaxos:
  多点提议 + 冲突依赖图
  → 冲突少/跨地域/并发写场景可能受益
  → 实现与调试复杂度更高
```

Why: 为什么不能用“EPaxos 无 Leader”作为普遍优势？——**去中心化只在冲突低、网络/拓扑适合、团队能承担复杂实现时才可能转化为收益**；Raft/ZAB 的 Leader 让顺序和运维更直观，也可能形成热点。选型必须从冲突率、写入地域、故障模型、读写语义和生态评估。 [分布式理论: 协议选型要同时看 Safety/Liveness、部署拓扑与维护能力]

### 5. 收束

三条路线对照：

```[pseudocode]
Raft:
  稳定 Leader → 强日志复制 → 状态机

ZAB:
  选主/同步 → zxid 全序广播 → ZooKeeper DataTree/watch

EPaxos:
  多点 PreAccept → 依赖图 → 冲突命令排序
```

**Aha Moment**: "ZAB 和 EPaxos 并不是 Paxos/Raft 的简单替代品：**ZAB 为协调服务强化全序和恢复阶段，EPaxos 为低冲突并发写把全序成本转成依赖图成本，Raft 则用强结构换理解和运维简单**。"
**回答读者三问**: ①ZAB 为什么存在=ZooKeeper 需要全序广播和状态机/会话语义；②EPaxos 怎么减少 Leader 依赖=非冲突命令按依赖并行；③谁更好=取决于冲突率、跨地域、语义和运维能力。

---

### 核心悬念

**"Paxos 已经有了多种实现路线；Fast Paxos、Cheap Paxos、Flexible Paxos、Generalized Paxos 等变种，分别在减少什么成本、放宽什么约束？"**

→ 引出 07-paxos-variants — Paxos 变种全览与适用边界。