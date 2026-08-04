# SOFAJRaft 源码学习范围规划

> **版本**: v1.4.1
> **仓库**: `/data/workspace/source-code/code/spring/sofa-jraft/`
> **规模**: core 279 个源文件，4 个子模块
> **日期**: 2026-08-03

---

## 一、仓库概况

SOFAJRaft 是蚂蚁金服开源的 RAFT 共识算法 Java 实现。核心是 **Node(状态机) → Replicator(日志复制) → RaftLogStorage(日志存储) → Snapshot(快照) → RPC(通信)** 五层架构。实现完整的 RAFT Leader 选举 + 日志复制 + 成员变更 + 快照 + 线性一致读。

**core 包结构**：

| 包 | 职责 | 关键类 |
|---|---|---|
| `core/` | Node 核心——`NodeImpl`(RAFT 主循环)、`FSMCaller`(状态机调用)、`BallotBox`(投票箱/commit 确认) | ✅ |
| `storage/` | 存储——`LogManager`(日志管理)、`SnapshotExecutor`(快照)、`RocksDBLogStorage`(RocksDB) | ✅ |
| `rpc/` | RPC——`RaftClientService`(客户端)、`BoltRaftRpcFactory`(Bolt RPC) | ✅ |
| `closure/` | 回调——`Closure`、`LeaderChangeClosure` | ✅ |
| `conf/` | 成员变更——`Configuration`(单次变更 Joint Consensus) | ✅ |
| `entity/` `option/` `error/` | 数据实体/配置/异常 | 淘汰 |
| `jraft-extension/` | 扩展(14) | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| J-1 | **RAFT 核心循环** | NodeImpl, BallotBox, FSMCaller, StateMachine | **三态转换**：Follower→`preVote`(预投票防脑裂)→Candidate(`requestVote`请求投票)→Leader(获得多数) → 收到更高 Term 回退 Follower；**Leader 心跳**：`Replicator.sendHeartbeat()` 每 `electionTimeoutMs/2` 发送→维持 Leader 权威；**线性一致读**：`ReadIndex` 机制——Leader 确认自己仍是 Leader 后(心跳确认多数)→`FSMCaller.onLeaderStart()` 执行读 |
| J-2 | **日志复制** | Replicator, LogManager, AppendEntriesRequest | **日志结构**：`(term, index, command)`——Leader 收到写请求→`LogManager.append()` 本地持久化→`Replicator.sendEntries()` 并行复制到 Follower→`BallotBox.commitAt()` 多数确认(quorum)→`FSMCaller.onApply()` 应用到状态机；**日志复制状态**：`nextIndex`(乐观匹配点)→逐步回退→`matchIndex`(已匹配点)—每个 Follower 独立追踪；**心跳+日志合一**：`AppendEntries` 空闲时携带空 entries 当心跳，有数据时携带 entries 当复制——减少 RPC 次数 |
| J-3 | **快照与日志压缩** | SnapshotExecutor, SnapshotWriter, LogManager | **快照触发**：`logIndex - snapshotIndex > snapshotLogIndexMargin` → `SnapshotExecutor.doSave()` 生成快照→`RaftOutter.SnapshotMeta(lastIncludedIndex, lastIncludedTerm)` 元数据；**InstallSnapshot**：新节点追日志过大→Leader 直接发送快照代替日志复制→Follower 接收快照→加载→跳过已快照的日志；**日志截断**：快照完成后 `LogManager.truncatePrefix(lastSnapshotIndex)` 删除旧日志 |
| J-4 | **成员变更** | ConfigurationEntry, ConfigurationCtx | **单节点变更**：`addPeer()`/`removePeer()` → Joint Consensus 两阶段：`C_old,new` → `C_new`——中间状态同时听取新旧配置的多数——保证任意时刻只有一个 Leader；**Learner**角色：不参与投票，被动接收日志→`Node.addLearner()` → 追上日志后→`changeToFollower()` |

### 🟡 扩展域（1 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| J-5 | **存储与 RPC 适配** | RocksDBLogStorage, BoltRaftRpcFactory, GrpcRaftRpcFactory | **存储层**：`LogStorage` SPI——`RocksDBLogStorage`(RocksDB 生产)、`MemoryLogStorage`(内存测试)；**RPC 层**：`RpcClient/RpcServer` SPI——`BoltRaftRpcFactory`(蚂蚁 Bolt 默认)、`GrpcRaftRpcFactory`(gRPC)——消息序列化 `Protobuf`；**磁盘管理**：Segmented Log(分段日志预分配)、`MappedByteBuffer` mmap 写日志 |

---

## 三、淘汰清单

| 模块/包 | 理由 |
|---|---|
| `jraft-extension/` (14) | 扩展功能——非核心 |
| `jraft-example/` `jraft-rheakv/` | 示例/RocksDB KV |
| `entity/` `option/` `error/` `util/` | 数据实体/工具类 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 4 |
| 🟡 扩展域 | 1 |
| **总域** | **5** |

---

## 五、学习顺序建议

```
J-1 RAFT 核心循环（理解 Follower→Candidate→Leader 状态机）
  → J-2 日志复制（理解 AppendEntries + quorum 确认）
    → J-3 快照与压缩（理解 InstallSnapshot + 日志截断）
      → J-4 成员变更（理解 Joint Consensus）
        → J-5 按需深入
```

以上规划完成，共 **4🔴+1🟡=5 域**。
