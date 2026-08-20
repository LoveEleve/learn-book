# J-1 RAFT 核心循环 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. Node 有哪些状态? 状态转移的触发点分别在哪 (handleElectionTimeout/stepDown/becomeLeader)?
2. electSelf 里为什么先持久化 votedFor 再广播? 顺序反了会怎样?
3. handleRequestVoteRequest 的投票三个条件是什么? 顺序重要吗?
4. stepDown 时 ballotBox.clearPendingTasks 清的是什么? 谁受影响?
5. ReadIndex 的 ReadOnlySafe 和 ReadOnlyLeaseBased 怎么选? 请求里怎么标?
6. FSMCaller 的任务类型有哪些? onApply 在哪个线程跑?
7. isRunningOnFSMThread() 有什么用? 用户 FSM 里能自己开线程回调吗?

## 架构师视角

8. 为什么 preVote 不增加任期? 分区节点怎么自动放弃?
9. 随机选举超时 [1000,2000) 的意义? 固定 1000ms 会怎样?
10. "本任期提交能力"为什么是安全性的开关? 旧任期日志为什么不能直接提交?
11. 为什么新 leader 第一条日志必须是配置日志?
12. checkQuorum 和 read lease 为什么共用 lastLeaderTimestamp? 参数怎么保证不误判?
13. TimeoutNow 传位的价值? 与等选举超时差多少?
14. ReadIndex 的 pendingNotifyStatus 为什么用 TreeMap 按 logIndex 排?
15. FSM 单线程 + Disruptor 攒批的设计权衡? 并行 apply 为什么不行?
16. 优先级选举 (electionPriority) 的设计意图? 衰减机制防什么?
17. 与 ZAB 对比: 任期 vs epoch, 预投票 vs 无预投票?

## SRE/运维视角

18. 线上节点 CPU 抖动导致选举风暴, 怎么调? 哪些参数 (electionTimeoutMs/maxElectionDelayMs)?
19. 半数节点宕机, leader 什么时候让位? checkQuorum 周期是多少?
20. 节点重启后怎么恢复? term/votedFor 在哪持久化 (metaStorage)?
21. 日志落后节点追上需要什么条件? 日志被覆盖会发生什么?
22. ReadIndex EAGAIN 错误什么时候出现? 怎么排查?
23. 集群出现双 leader, 日志里有什么特征? 怎么收敛?

## 研究者视角

24. vs Raft 论文: SOFAJRaft 做了哪些工程化增强 (preVote/lease/优先级/流水线)?
25. vs braft (C++): 移植时改了哪些核心语义?
26. vs Kafka KRaft: 元数据集群的 Raft 用在哪一层?
27. LeaseRead 的理论边界: 时钟漂移多少会破坏线性一致?
28. vs Raft 论文 §9.6: 对称/非对称分区容忍如何实现?

## 学生视角

29. 什么是任期? 为什么叫 term?
30. 什么是预投票? 为什么叫"预"?
31. 什么是 quorum? 3 节点要几票?
32. 什么是线性一致读? 和普通读的区别?
33. 什么是状态机? 日志为什么能变成状态?
