# J-1 temporal-trace — 时空溯源

> 本地 git 为 release 单提交浅克隆 (cb1a0c7 1.4.1); 溯源基于代码内注释 + braft 对照 (SOFAJRaft 移植自百度 braft C++)。

## 版本线 (代码内证据)

| 版本 | 事件 | 证据 |
|---|---|---|
| 1.0~1.1 | 基础 Raft: 选举/复制/快照/成员变更 移植自 braft | README "从百度的 braft 移植而来" |
| 1.2 | preVote 完善 + Learner 只读成员 | newLearners/oldLearners 结构 (RaftOutter.java:350-385) |
| 1.3 | 优先级选举 (electionPriority) + 半确定性选主 | NodeOptions.java:50 + NodeImpl.java:662-710 decayTargetPriority |
| 1.3.x | 流水线复制优化 | J-2 展开 |
| 1.4.x | LeaseRead 默认关闭? (readOnlyOptions 默认 ReadOnlySafe, RaftOptions.java:89) — 稳字当头 | RaftOptions.java:89 |

## issue 痕迹 (代码注释实证)

- **issue/1049**: 单节点 lastCommittedIndex 初始化 = max(snapshot, lastLogIndex) (NodeImpl.java:1119-1121)
- **双 leader 冲突处理**: 收到不同 leaderId 声明 → stepDown(term+1) 两败俱伤式收敛 (NodeImpl.java:2059-2070)
- **ABA 防御**: 解锁取 lastLogId 后重锁校验 term (L1193-1196) — 选举路径的经典竞态

## 设计代际

```
Raft 论文 (2014, 原教旨: 无 preVote/无 lease)
  → braft (C++, 工程化: preVote + 快照并发 + 流水线)
  → SOFAJRaft (Java: 优先级选举 + Disruptor 无锁队列 + 丰富 metrics)
  → 对照 Kafka KRaft (元数据 Raft) / ZAB (ZK 自研协议)
```

## 教训沉淀 (写作引用)

- "先持久化后拉票" — 崩溃安全优先于响应速度 (NodeImpl.java:1190-1208)
- "本任期提交能力" — 安全性的最小充分条件 (论文 5.4.2 的工程表达)
- lease(900ms) + clockDrift < electionTimeout(1000ms) — 参数推导保证误判不可能 (NodeOptions.java:57-61)
