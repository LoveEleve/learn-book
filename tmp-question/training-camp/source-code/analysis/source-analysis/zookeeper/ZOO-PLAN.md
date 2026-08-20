# ZooKeeper — 知识网络化规划 (Z-1~Z-9, 09 怀疑审计后 v1)

> **日期**: 2026-08-15 | **依据**: issue/源码分析执行计划.md 阶段4.3 (9 域) + 09 对既有规划保持怀疑 全量重审
> **源码**: `/data/workspace/source-code/code/spring/zookeeper` (**ZooKeeper 3.9.5**, pom.xml:34 实证; zookeeper-server 为主模块, Java 客户端也在 server 模块 — client 模块仅 C 客户端)
> **定位**: 阶段 4.3 — 消息与事务第六环 **分布式协调内核 (共识/广播/状态树/会话)**
> **知识网络**: 与 Kafka (阶段4.2, 元数据协调对照) + RocketMQ RM-12 (Controller 对照) + sofa-jraft (4.6, Raft 对照) + Curator (4.5, recipes 对照) 互联

---

## 〇、09 怀疑审计表 (ZooKeeper, 2026-08-15) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| **域清单 9 个** (Z-1~Z-9) | 顶层包扫描 (zookeeper-server 70 文件/20656 行 + client + recipes) | server/quorum (选举+广播) + DataTree + Processor 链 + Session + watch + persistence + ZooKeeper/ClientCnxn + recipes 三模块 — 9 面全覆盖 | **接受** ✅ |
| Z-1 "FastLeaderElection logicalclock+recvset/outofelection+totalOrderPredicate" | FastLeaderElection.java | 1225 行, 四要素待逐域验证 | **接受** ✅ |
| Z-2 "Leader.lead PROPOSAL→ACK→COMMIT 两阶段" | Leader.java | 1817 行 + QuorumCnxManager 1487 (连接面) | **接受** ✅ |
| Z-4 "Processor 链 Prep(10+OpCode+multi-op)→Sync→Commit→Final" | PrepRequestProcessor 1122 / Sync 281 / Final 680 | 四处理器存在; "10+OpCode" 数字待穷举 | **接受+待验证** ✅ |
| Z-5 "Session ExpiryQueue分桶+touchSession+SessionImpl三态" | SessionTrackerImpl.java | 359 行 | **接受** ✅ |
| Z-6 "Watcher watchTable+watch2Paths双向" | server/watch/ | **WatchManager 376 + WatchManagerOptimized 412 双实现** — 执行计划未提双实现 | **修正+补充** ⚠ |
| **Z-8 "Distributed Recipes: LeaderLatch/InterProcessMutex/DistributedQueue/DistributedBarrier"** | zookeeper-recipes 扫描 | **LeaderLatch/InterProcessMutex 是 Curator 的类 (org.apache.curator)**; ZK 官方 recipes 为 **LeaderElectionSupport (471)/WriteLock (303)/DistributedQueue (302)** — 执行计划类名张冠李戴 | **修正** ⚠ |
| **Z-6 "WatchManagerOptimized 按 session 分桶 (sessionWatches)"** (执行计划隐含断言) | WatchManagerOptimized grep | **无 sessionWatches** — 实际 pathWatches (按路径位集合) + watcherBitIdMap (watcher 位图); 会话清理靠 **deadWatchers 懒清理** | **修正** ⚠ (2026-08-15 Z-6 五次 REVIEW 补录) |
| **"zookeeper-client 模块"** (隐含) | 模块扫描 | zookeeper-client 仅 **C 客户端** (zookeeper-client-c); **Java 客户端 ZooKeeper (3118)/ClientCnxn (1751) 在 zookeeper-server 模块** | **修正** ⚠ |
| 版本 | pom.xml | **3.9.5** (执行计划无版本声明) | **补充** ✅ |
| Curator→ZK 依赖 (STAGE3 注记) | — | 4.5 Curator 依赖 ZK — 拓扑序 ZK 先于 Curator ✅ | **接受** ✅ |

**覆盖率报告**: 既有规划 9 域 → 重审后 **9 域** (100%, 无增删), 修正 **4 处** (Z-8 类名/Java 客户端归属/WatchManager 双实现/sessionWatches 断言)。

---

## 一、入口点与主线

`QuorumPeer (2711) → FastLeaderElection → Leader.lead → Prep→Sync→Commit→Final 链 → DataTree/ZXID → 持久化 (FileTxnLog/FileSnap)` — 客户端: `ZooKeeper → ClientCnxn → NIOServerCnxn`。

## 二、域清单 (9 域: 7🔴 + 2🟡)

| # | 域 | 模块 (文件行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| Z-1 | **Leader 选举** | server/quorum/FastLeaderElection (1225) + QuorumPeer (2711) | logicalclock/recvset/outofelection/totalOrderPredicate | 🔴 A |
| Z-2 | **原子广播** | server/quorum/Leader (1817) + Learner + QuorumCnxManager (1487) | PROPOSAL→ACK→COMMIT 两阶段/quorum 判定 | 🔴 A |
| Z-3 | **DataTree** | server/DataTree (1971) + ZKDatabase (806) + NodeHashMap | 内存树/Node stat/ZXID 快照/恢复 | 🔴 A |
| Z-4 | **Processor 链** | Prep (1122) + Sync (281) + Commit + Final (680) | 四处理器管线/multi-op/读写分离 | 🔴 A |
| Z-5 | **Session** | server/SessionTrackerImpl (359) | ExpiryQueue 分桶/touchSession/三态 | 🔴 A |
| Z-6 | **Watcher** | server/watch/ WatchManager (376) + **WatchManagerOptimized (412)** | watchTable+watch2Paths 双向/一次性触发 | 🔴 A |
| Z-7 | **Client API** (位置修正) | server 模块 ZooKeeper (3118) + ClientCnxn (1751) | ZooKeeper 门面/ClientCnxn 事件线程/packet 队列 | 🟡 B |
| Z-8 | **Recipes** (类名修正) | zookeeper-recipes (leader 471/lock 303/queue 302) | **LeaderElectionSupport/WriteLock/DistributedQueue** (非 Curator) | 🟡 B |
| Z-9 | **持久化** | server/persistence/ FileTxnLog (860) + FileSnap (288) + FileTxnSnapLog | txnlog checksum+snapshot zxid/PurgeTxnLog | 🔴 A |

## 三、执行顺序 (拓扑: 共识面 → 存储面 → 客户端面)

**Z-1 → Z-2 → Z-3 → Z-4 → Z-5 → Z-6 → Z-7 → Z-8 → Z-9**

> 拓扑理由: 选举 (Z-1) → 广播 (Z-2, 依赖选举产出 leader) → 状态树 (Z-3, 广播落点) → Processor 链 (Z-4, 读写路径) → 会话 (Z-5) → Watcher (Z-6, 客户端交互面) → 客户端 API (Z-7) → Recipes (Z-8, 客户端模式库) → 持久化 (Z-9, 收束落盘面)。

## 四、知识网络图

```
← 复用: Kafka (4.2 KRaft 元数据对照) + RocketMQ RM-12 (Controller/选举对照) + ES (状态机对照)
→ 引出: Curator (4.5, recipes 客户端封装) + SofaJRaft (4.6, Raft vs ZAB)
```

## 五、完成检查单

- [x] 顶层包扫描 ↔ 域清单覆盖矩阵 (9/9)
- [x] 09 审计: 修正 4 处 (Z-8 类名/Java 客户端归属/WatchManager 双实现/sessionWatches 断言)
- [x] 数字断言穷举 (FastLeaderElection 1225/Leader 1817/Prep 1122 等行数实证)
- [x] Z-1 ✅ 2026-08-15 (harness 4/4)
- [x] Z-2 ✅ / Z-3 ✅ / Z-4 ✅ / Z-5 ✅ / Z-6 ✅ / Z-7 ✅ 2026-08-15 (6 harness 4/4)
- [x] Z-8 ✅ 2026-08-15 (21 发现 — 前驱消失悬挂竞态/会话失效静默/序号位宽)
- [x] Z-9 ✅ 2026-08-15 (harness 4/4 11 断言 — CRC 覆盖 Javadoc 不符/len 无保护静默截断/容错二分)
- [x] **阶段 4.3 全量收官 9/9** (2026-08-15): 大纲 566 行/域文件 4173 行/REVIEW 136 处/harness 7/7
