# Z-1 Leader 选举 — Pass 1 探索笔记

> 域: Z-1 Leader 选举 | 🔴 A 方案 | 2026-08-15
> 源码: FastLeaderElection (1225) + QuorumPeer (2711) + QuorumCnxManager (1487, 连接面) + Vote/SyncedLearnerTracker + QuorumVerifier | ZooKeeper 3.9.5

## 调用图

```
QuorumPeer.run (LOOKING 状态) → getElectionAlgorithm → FastLeaderElection
  → lookForLeader:
      logicalclock++ + updateProposal(自荐) + sendNotifications (全员)
      → Messenger.WorkerSender/WorkerReceiver (双线程: sendqueue/recvqueue)
      → recvqueue.poll(notTimeout 指数退避 200ms→60s)
      → LOOKING 通知: epoch 比较 (旧轮忽略/新轮清 recvset 换轮/同轮 totalOrderPredicate)
        → recvset.put + getVoteTracker (双 QuorumVerifier) + hasAllQuorums
        → finalizeWait 200ms 稳定窗口 → setPeerState + leaveInstance (当选)
      → FOLLOWING/LEADING 通知: receivedFollowingNotification
        → 同 epoch: recvset + checkLeader; 异 epoch: outofelection 学习
        → 2 节点: QuorumOracleMaj 裁决
      → OBSERVING: 忽略
```

## 基本元素分解

1. **投票结构**: Notification (leader/zxid/electionEpoch/state/sid/peerEpoch) + ToSend 四类 + 双队列
2. **提议广播**: logicalclock 轮次 + 自荐 + sendNotifications + 指数退避
3. **收票判定**: totalOrderPredicate 三要素 + getVoteTracker + hasAllQuorums + finalizeWait
4. **双集合**: recvset (当前轮) vs outofelection (历史学习) + FOLLOWING/LEADING 分离 + Oracle

## 标记问题 (20 问)

1. Notification 字段? (leader/zxid/electionEpoch/state/sid/peerEpoch)
2. ToSend 四类消息? (crequest/challenge/notification/ack)
3. Messenger 线程模型? (WorkerSender/WorkerReceiver 双线程)
4. 协议版本兼容? (28B/40B)
5. logicalclock 什么时候递增? (每次 lookForLeader)
6. 初始提议? (投自己 initId/initZxid/peerEpoch)
7. 指数退避范围? (200ms→60s)
8. weight==0? (不能当选)
9. totalOrderPredicate 三要素? (epoch>zxid>sid)
10. 新轮次票怎么处理? (清 recvset 换轮)
11. 旧轮次票? (忽略)
12. 稳定窗口? (finalizeWait 200ms)
13. getVoteTracker 双 verifier? (reconfig)
14. recvset 语义? (当前轮裁决)
15. outofelection 语义? (历史学习)
16. FOLLOWING 通知处理? (checkLeader + 多数)
17. 2 节点 Oracle? (QuorumOracleMaj)
18. observer 投票? (忽略)
19. validVoter? (双视图成员)
20. 当选动作? (setPeerState + leaveInstance)

## 时空溯源 (代码内痕迹)

- 3.4.6: Notification CURRENTVERSION=0x2 (版本锚 L117)
- 3.4.10+: weight 加权投票 (totalOrderPredicate weight==0 排除 — 注释 L736-742)
- ZK-107: 40B 协议 (peerEpoch+version, 兼容 28B 注释 L251-257)
- ZOOKEEPER-3922: FOLLOWING/LEADING 分离 + 2 节点 Oracle (L1073-1097 长注释)
- 动态 reconfig: 双 QuorumVerifier (getVoteTracker L761-767)

## 大域拆分判断

Z-1 = FastLeaderElection 单文件域; QuorumCnxManager 连接面归 Z-2 (广播通道共用); 单篇 🔴 A (8 闭环 q1-q4 + 验证)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划) | 验证 | 结论 |
|:--|:--|:--|
| "FastLeaderElection logicalclock+recvset/outofelection+totalOrderPredicate" | 四要素全实证 (L880-908/L849-862/L723-749) | **接受** ✅ |
| "LeaderLatch/InterProcessMutex" (Z-8) | Curator 类 — ZK recipes 为 LeaderElectionSupport/WriteLock | **已修正** (PLAN) |
| 数字: 稳定窗口 | finalizeWait=200ms | **补充** ✅ |
| 数字: 退避范围 | min=200ms → max=60s | **补充** ✅ |
| 数字: 协议版本 | CURRENTVERSION=0x2 / 28B/40B | **补充** ✅ |
