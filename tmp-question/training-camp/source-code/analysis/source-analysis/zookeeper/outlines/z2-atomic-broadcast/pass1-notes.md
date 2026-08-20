# Z-2 原子广播 — Pass 1 探索笔记

> 域: Z-2 原子广播 | 🔴 A 方案 | 2026-08-15
> 源码: Leader (1817) + LearnerHandler (1183) + Learner (待查) + QuorumCnxManager (1487) + QuorumMaj | ZooKeeper 3.9.5

## 调用图

```
QuorumPeer 切 LEADING → Leader.lead():
  DISCOVERY: loadData + LearnerCnxAcceptor → getEpochToPropose + setZxid(epoch,0)
    → newLeaderProposal (NEWLEADER) → waitForEpochAck → waitForNewLeaderAck (多数)
  SYNCHRONIZATION → BROADCAST:
    main loop: 每 tickTime/2 检查 synced learners (quorum 自检)

写路径: propose (zxid++) → outstandingProposals.put → broadcast PROPOSAL
  → LearnerHandler 转发/ack → processAck → p.addAck → tryToCommit
    → 顺序守卫 (zxid-1) + hasAllQuorums → commit (COMMIT 广播) + inform (observer)

LearnerHandler (每 learner 一线程): FOLLOWERINFO → syncFollower 五分支
  → 空DIFF (已同步) / TRUNC (领先) / DIFF (窗口内) / txnlog 补 (窗口外) / SNAP (兜底)
  → 同步后进 forwardingFollowers → 收 PROPOSAL → ACK → 收 COMMIT

QuorumCnxManager: connectOne (全连) + 连接仲裁 (防双连) + queueSendMap/queueRecv
```

## 基本元素分解

1. **lead 状态机**: DISCOVERY → SYNCHRONIZATION → BROADCAST + epoch 提议 + 双 verifier
2. **两阶段提案**: propose → outstandingProposals → processAck → tryToCommit (顺序+quorum 双守卫)
3. **同步五分支**: syncFollower (空DIFF/TRUNC/DIFF/txnlog/SNAP)
4. **连接仲裁**: QuorumCnxManager connectOne + 防双连 + 队列隔离

## 标记问题 (20 问)

1. ZabState 三阶段? (DISCOVERY/SYNCHRONIZATION/BROADCAST)
2. epoch 提议? (getEpochToPropose + makeZxid(epoch,0))
3. NEWLEADER ack? (waitForEpochAck + waitForNewLeaderAck)
4. 主循环干嘛? (每半 tick 检查 synced learners)
5. propose 流程? (zxid++ + outstandingProposals + broadcast)
6. processAck 守卫? (allowedToCommit/lastCommitted/outstanding null)
7. tryToCommit 守卫? (zxid-1 顺序 + hasAllQuorums)
8. commit 动作? (outstanding remove + toBeApplied + COMMIT 广播)
9. reconfig 特例? (designatedLeader + allowedToCommit=false)
10. pendingSyncs? (sync 等待的请求)
11. LearnerHandler 线程模型? (每 learner 一线程)
12. syncFollower 五分支? (空DIFF/TRUNC/DIFF/txnlog/SNAP)
13. TRUNC 什么时候? (peerLastZxid > maxCommittedLog)
14. 新 epoch zxid 不 TRUNC? (isPeerNewEpochZxid)
15. syncThrottler? (SNAP/DIFF 并发限流)
16. QuorumCnxManager 连接? (connectOne 全连)
17. 连接仲裁? (防双向连接)
18. queueSendMap? (每对节点独立队列)
19. observer 路径? (inform 非 ACK)
20. quorum 失守? (leader shutdown)

## 时空溯源 (代码内痕迹)

- ZOOKEEPER-1783: 初始 config version=0 → lastSeenQV 版本协商 (L671-699 长注释)
- forceSnapshotSync: zookeeper.forceSnapshotSync 系统属性 (L258)
- ZOOKEEPER-1277: 低 32 位 rollover 测试钩子 (L744-754)
- syncThrottler: 3.6+ 同步流控 (INFLIGHT_SNAP/DIFF_COUNT)
- learnerMaster: LearnerMaster 接口抽象 (可插拔 learner 侧)

## 大域拆分判断

Z-2 = Leader 广播 + LearnerHandler 同步 + QuorumCnxManager 连接; 单篇 🔴 A (8 闭环 q1-q4 + 验证); Learner 接收面并入本域

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划) | 验证 | 结论 |
|:--|:--|:--|
| "Leader.lead PROPOSAL→ACK→COMMIT 两阶段" | propose (L1288) + processAck (L1047) + tryToCommit (L963) + commit/inform | **接受** ✅ |
| "outstandingProposals+tryToCommit" | 双守卫实证 (zxid-1 顺序 + hasAllQuorums) | **接受+补充** ✅ |
| 数字: 同步模式 | **五分支** (非三模式 — 空DIFF 与 txnlog 分支是细节) | **精确化** ✅ |
| 数字: quorum 自检 | tickTime/2 周期 | **补充** ✅ |
