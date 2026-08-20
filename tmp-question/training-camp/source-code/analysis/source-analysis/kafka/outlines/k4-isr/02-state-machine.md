# K-4 Partition & ISR 篇 2/4 — 角色转换: makeLeader/makeFollower 状态机

> 前置: [[K-4-isr-01]] (ISR 模型) | 复用: — | 对照: [[r14-sentinel]] (Redis 故障转移) [[rd2-rlock]] (fencing) | 引出: [[K-4-isr-03]]
> 🔴 A | 来源: Partition.scala:328,733-885,909-937
> 定位: K-4 卷中篇 — 回答"副本角色怎么转换? 转换时日志怎么处理?"

**读者处境**: 面试官问 "leader 挂了新 leader 怎么产生? 旧 leader 回来怎么办?" 你答 "选举" — 但再问 "makeLeader 做什么? 为什么记 epoch 起点? makeFollower 为什么清 ISR?" 你答不上来。这篇是状态机的完整答案。

### 1. 问题引入 — LeaderAndIsr 请求到达

场景: controller 下发 LeaderAndIsr (K-5), 一个 broker 要从 follower 变 leader (或反之) — 转换时做什么?
- 全部转换在 leaderIsrUpdateLock 写锁内 (Partition.scala:328,737,843)
- **不做本地选举**: 角色转换完全由 controller 的 LeaderAndIsr 请求驱动 — 副本自身不发起选举 (与 Raft 的候选者自发竞选不同), 这是 ISR 模型"controller 裁定"的集中式设计 (K-5 衔接)
- 本篇问题: makeLeader (Q3a) / makeFollower (Q3b) / 并发保护 (Q3c)

### 2. makeLeader — 上任的仪式

场景: 成为 leader 的完整步骤?
- epoch 防旧: partitionEpoch 检查 (Partition.scala:743-747) — 旧请求直接跳过
- updateAssignmentAndIsr (Partition.scala:L764-771) — 同步副本集合
- **assignEpochStartOffset (Partition.scala:L793)**: 新 leader epoch 起点缓存 — follower 截断查询的依据 (注释 Partition.scala:L788-792: 连续选举时 follower 必须能截断到正确 offset)
- resetReplicaState (Partition.scala:L797-804) — 副本追赶状态重置
- maybeIncrementLeaderHW (Partition.scala:L822) — "ISR could be down to 1" 时 HW 重算
- 锁外 tryCompleteDelayedRequests (Partition.scala:L826-827) — HW 变了, 延迟请求可能完成

### 3. makeFollower — 让位的仪式

场景: 变成 follower 的步骤?
- epoch 检查 (Partition.scala:844-848)
- leader 先更新再清 ISR (Partition.scala:L851-853) — 注释: 防 under-min-isr 误报
- **isr = Set.empty** (Partition.scala:L861) — 新 follower 没有 ISR
- 返回 isNewLeaderEpoch → **重启 fetcher** (Partition.scala:L883) — 注释: "We must restart the fetchers when the leader epoch changed regardless of whether the leader changed"

### 4. 并发保护 — 读写锁与更新路径

场景: 多个请求同时改状态怎么办?
- leaderIsrUpdateLock = ReentrantReadWriteLock (Partition.scala:328)
- 状态变更 (makeLeader/makeFollower/expand/shrink) 写锁; 读路径 (updateFollowerFetchState Partition.scala:L923) 读锁
- updateFollowerFetchState 读锁注释 (Partition.scala:L921-922): "avoid the race between ISR updates and the fetch requests from rebooted follower. It could break the broker epoch checks in the ISR expansion"

### 核心悬念
"makeLeader 为什么要缓存 epoch 起点 (assignEpochStartOffset)?" — 连续快速选举时, follower 可能持有比新 leader 更新的 epoch 数据; 新 leader 必须能回答"我的 epoch N 到哪里结束"让 follower 截断 (K-4 篇 3 的 4 规则依赖它) — 没有它, 旧数据可能被当成新数据。

### 概念依赖链
Q3a makeLeader → Q3b makeFollower → Q3c 锁 → (03 篇: 拉取+截断) → (K-5 衔接)

### 源码锚点清单
- Partition.scala:328 (leaderIsrUpdateLock) / 733-830 (makeLeader) / 743-747 (epoch 防旧) / 764-771 (updateAssignmentAndIsr) / 793 (assignEpochStartOffset) / 797-804 (resetReplicaState) / 822 (HW 重算) / 826-827 (锁外延迟完成) / 839-885 (makeFollower) / 851-853 (leader 先更新) / 861 (ISR 清空) / 883 (fetcher 重启) / 909-937 (updateFollowerFetchState) / 921-923 (读锁防竞态)
