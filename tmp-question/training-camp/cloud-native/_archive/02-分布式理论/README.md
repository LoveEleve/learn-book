# 02 分布式理论 · 详细学习指南

## 学完能干什么
能跟面试官讨论 Paxos/Raft/ZAB 的区别，能手写 SOFAJRaft Node 配置，能说清 Nacos CP/AP 切换原理。

## Week 3 · Day 1（全天）

### 上午：CAP + BASE（2小时）

📖 `stage-2/docs/01. 第一节：CAP 与 BASE 理论.md`（全文）

**三个命题刻在脑子里**：
- C（一致性）：所有节点同一时刻看到相同数据
- A（可用性）：每个请求都能获得非错误响应
- P（分区容错）：网络分区时系统继续工作

**P 必选**（网络一定会出问题）→ 只能在 CP 和 AP 之间选。

| 选型 | 代表 | 行为 |
|------|------|------|
| CP | ZK、etcd | 分区时放弃 A——Leader 所在的少数派拒绝写请求 |
| AP | Eureka、Nacos Distro | 分区时放弃 C——两边都能读写，但数据可能不一致 |

✏️ **动手**：画一张图——两个机房通过 WAN 连接，机房间网络断了。分别画出 CP 和 AP 的行为。
- CP：用户请求到机房 B → 失败（Leader 在机房 A）
- AP：用户请求到机房 B → 成功（机房 B 独立提供服务），但两边数据可能不一致

✅ **算过关**：Nacos 为什么能同时支持 CP 和 AP？不是说 CAP 不能三者兼得吗？——**给不同的数据用不同的协议**（配置用 Raft CP，临时实例用 Distro AP）

---

### 下午：Paxos 算法（2小时）

📖 `stage-2/docs/02. 第二节：分布式共识算法 - Paxos.md`（全文）
📖 `stage-2/papers/paxos-simple-Copy.pdf`（读 Phase 1 和 Phase 2）

**Paxos 两阶段消息流**：
```
Phase 1 (Prepare/Promise):
  Proposer → Acceptors: "Prepare(n)"  // n 为提案编号
  Acceptor → Proposer: "Promise(n, v')"  // 承诺不接受 <n 的提案，如果有已接受的 v' 一并返回

Phase 2 (Accept/Accepted):
  Proposer → Acceptors: "Accept(n, v)"  // 如果有 v' 就用 v'，否则用自己的 v
  Acceptor → Proposer: "Accepted(n, v)"
```

**三个硬约束**：
1. Quorum: N = 2F + 1（F 个节点故障时仍能达成共识）
2. Proposal Number 单调递增（防止旧提案覆盖新提案）
3. Phase 2 的值必须沿用被 Promise 过的最新值（保证安全性）

✏️ **动手**：模拟一个活锁场景——P1 提议到 A1/A2（Prepare(1)），P2 提议到 A2/A3（Prepare(2)）。P1 的 Accept(1) 被 A2 拒绝（A2 已经 Promise 给 P2 了）。P1 重新 Prepare(3)...... 画出完整的消息流。

**Multi-Paxos 的优化**：选出一个 Leader，只有它能发起 Prepare，避免活锁。

---

## Week 3 · Day 2（全天）

### 上午：Raft 算法（3小时）

📖 `stage-2/docs/03.` + `04.`
📖 `stage-2/papers/raft.pdf`（重点：Section 5 Leader Election + Figure 2）

**Raft 状态机**（手画三遍）：
```
        超时，发起选举
  Follower ──────────────→ Candidate
     ↑                        │
     │ 发现更高 Term 的 Leader │ 获得多数票
     │←───────────────────────┘
     │
     │             收到更高 Term 的消息
     │←──────────────────────────────┘ Leader
```

**Leader 选举关键**：
1. Follower 在 election timeout（150-300ms 随机）后转为 Candidate
2. Candidate 增加自己的 term，投票给自己，并发 RequestVote 给所有节点
3. 获得多数票 → 成为 Leader → 立即发心跳（AppendEntries）阻止其他节点超时

**日志复制关键**：
1. Leader 收到客户端请求 → 追加到本地日志（uncommitted）
2. 并发 AppendEntries RPC 发送给所有 Follower
3. 多数 Follower 确认 → Leader commit → 响应客户端
4. 下一次心跳时告诉 Follower commitIndex 可以 advance 了

**Raft vs Paxos**：
- Raft 强制单一 Leader，Paxos 允许多 Proposer（因此有活锁问题）
- Raft 日志连续不空洞，Paxos 允许空洞
- Raft 用随机超时避免脑裂，Paxos 没有规定具体机制

✏️ **动手**：手画状态转换图 + 日志复制时序图（Leader/S1/S2 三个节点，Leader 收到两个请求，S1 确认了第一个，S2 确认了第二个）

✅ **算过关**：如果网络分区，旧 Leader（在少数派）收到了客户端的写请求，能 commit 吗？——不能。因为 AppendEntries 得不到多数派确认。

---

### 下午：ZAB 协议（1小时）

📖 `stage-2/docs/05. 第四节：原子广播算法 - ZAB.md`
📖 `stage-2/papers/zab.pdf`

**ZAB 三阶段**：
1. Discovery：选出 Leader，确定 epoch
2. Synchronization：Follower 和 Leader 同步到一致状态
3. Broadcast：Leader 接收写请求，两阶段提交（Propose → Commit）

**ZXID 结构**：高 32 位 = epoch，低 32 位 = 事务序号

**与 Paxos 的区别**：ZAB 在崩溃恢复时更高效——Leader 宕机后，新 Leader 只需要和 Follower 同步未提交的事务，不需要重新执行 Paxos 的两阶段。

---

## Week 4 · Day 1-2（2天）

### 课时 09：SOFAJRaft 源码走读（4小时）

📖 `stage-2/docs/06.` + `07.`（全文）

🔍 **看代码**：
```bash
cd stage-2/src/middleware-projects && mvn compile -pl rpc-project
```
重点文件：`ServiceDiscoveryServer.java`, `ServiceDiscoveryStateMachine.java`, `RegistrationRpcProcessor.java`

**JRaft 核心组件**：
- Node：Raft 节点的主接口
- LogStorage(RocksDB)：持久化日志
- StateMachine(onApply)：业务状态机，onApply 在日志被 commit 到状态机后被调用
- Replicator：日志复制器，负责向 Follower 推送日志
- FSMCaller：回调业务状态机的组件
- BallotBox：投票箱，统计 AppendEntries 的确认数量
- AppendBatcher：批量写盘优化

**心跳机制**（rpc-project 实现）：
- 客户端每 5s HeartBeat → HeartBeatRpcProcessor → **不写 Raft 日志**，直接在 Leader 内存中更新心跳时间
- ServiceInstanceBeatThread 每 5s checkBeat() → 检查 30s 内是否有心跳 → 无心跳则提交 DEREGISTRATION 到 Raft 日志

✅ **算过关**：onApply 收到的日志一定是已提交的吗？——不一定。Leader 上的日志在 committed 之前也会被应用到状态机。但 Follower 收到的日志一定是 Leader 确认提交过的。

---

### 课时 10：Nacos 双协议 + ZK 选举（4小时）

📖 `stage-2/docs/08.` + `09.` + `10.` + `11.` + `12.` + `13.`

**Nacos 的双模式选择**：
- 配置中心 → Raft CP：配置错了系统可能出大问题，必须强一致
- 临时实例 → Distro AP：Leader-Less 全对称架构，每个节点都能独立处理注册请求

**Distro 协议关键机制**：
- DistroMapper：一致性 Hash 路由，决定哪个节点负责哪个实例
- DistroVerifyTimedTask：定期校验数据一致性
- MemberInfoReportTask：心跳上报成员信息

**ZK FastLeaderElection 完整流程**：
1. 递增 logicalclock，updateProposal() 设定自己是 Leader
2. sendNotifications() 广播选票（含 proposedLeader, proposedZxid, logicalclock, state=LOOKING）
3. 主循环接收通知 → totalOrderPredicate 三层比较：**epoch > zxid > sid**
4. 收到多数票 → state = LEADING

---

### 补充：EventDispatcher 事件模式

**microsphere-java** 的纯 Java SPI 事件框架：
```java
// DirectEventDispatcher：调用线程同步执行
new DirectEventDispatcher().dispatch(event);

// ParallelEventDispatcher：默认 ForkJoinPool 并行执行
new ParallelEventDispatcher().dispatch(event);
```

**EventListener 泛型推断**：继承 `EventListener<E extends Event>`，通过 `getAllParameterizedTypes()` 反射推断 E 的类型。

对比 segfault-lessons Spring Cloud lesson-1 的 `ApplicationEvent` + `ApplicationListener`——一个是通用 SPI 框架，一个是 Spring 内置机制。理解两者的设计差异。

---

## 本阶段自检清单
- [ ] 能手画 Raft 状态转换图 + 日志复制时序图
- [ ] 能解释 Nacos Raft CP vs Distro AP 的选择逻辑
- [ ] 理解 ZK totalOrderPredicate 三层比较规则
- [ ] 能说清 SOFAJRaft Node 的 7 个核心组件各自负责什么
- [ ] 读过至少 3 篇共识算法论文原文
