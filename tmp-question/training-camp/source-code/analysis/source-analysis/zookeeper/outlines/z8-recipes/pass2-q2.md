# 闭环笔记 q2: LeaderElectionSupport — 事件驱动状态机

## 假设
选举 = 状态机 + 全量快照判定 + 前驱 watch 触发重跑。

## 验证过程
- **状态机** (L461-469): **7 State** (START/OFFER/DETERMINE/ELECTED/READY/FAILED/STOP) + **12 EventType** (L443-456: START/OFFER_START/OFFER_COMPLETE/DETERMINE_START/DETERMINE_COMPLETE/ELECTED_START/ELECTED_COMPLETE/READY_START/READY_COMPLETE/FAILED/STOP_START/STOP_COMPLETE) — 每状态进出都有事件
- **makeOffer** (L165-182): create rootNodeName+"/n_" EPHEMERAL_SEQUENTIAL — 每个竞争者一个 offer; hostName 为 data
- **determineElectionStatus** (L188-225): 拆路径取 id (Integer, L197) → getChildren(false) 全量快照 → toLeaderOffers (逐个 getData + IdComparator 排序, L300-324) → **找自己下标 i: i==0 → becomeLeader; 否则 becomeReady(前驱)**
- **becomeReady** (L227-259): **exists(前驱, this)** — 显式传 watcher (ZooKeeper 实例可能共享, L236-238) — stat!=null → READY; stat==null (**前驱消失竞态**) → **determineElectionStatus() 递归重读** (L256)
- **process** (L327-341): **只处理 NodeDeleted** (L328) + **排除自身路径** (L329) + **排除 STOP 态** (L330) → determineElectionStatus 重跑
- **getLeaderHostName** (L289-298): 每次全量读 + 排序 (无缓存, O(N))
- **事件时序实证** (testReadyOffer): [START, OFFER_START, OFFER_COMPLETE, DETERMINE_START, DETERMINE_COMPLETE, (前驱停) DETERMINE_START, DETERMINE_COMPLETE, ELECTED_START, ELECTED_COMPLETE] — 停 leader 触发重选

## 代码类型
Implementation (状态机 + 事件)

## 跨域关联
- Z-1: 对照服务端 FastLeaderElection — 客户端选举无轮次/无投票, 靠 ZK 服务端全序 (ZK 自身已有 quorum 共识)
- Z-6: watch 语义 (NodeDeleted 触发面)
- Z-5: ephemeral 会话绑定 (断开 offer 消失)

## 结论
选举 = 状态机 (7/12) + 全量快照排序判定 + 前驱 watch 事件驱动重跑; 前驱消失竞态用**递归重读**处理。
源码位置: LeaderElectionSupport.java:119-341; LeaderOffer.java:80-88
