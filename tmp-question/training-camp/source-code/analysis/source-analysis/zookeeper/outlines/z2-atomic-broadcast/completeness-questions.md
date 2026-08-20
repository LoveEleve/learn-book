# Z-2 原子广播 — completeness-questions (全视角提问验证)

## 开发者视角

1. leader 当选后第一件事? (loadData + LearnerCnxAcceptor + 新 epoch zxid)
2. 写请求怎么广播? (propose → PROPOSAL → 收 ACK → tryToCommit → COMMIT)
3. 提交顺序怎么保证? (outstandingProposals.containsKey(zxid-1) 守卫)
4. follower 加入怎么补数据? (syncFollower 五分支: 空DIFF/TRUNC/DIFF/txnlog/SNAP)
5. follower 比 leader 新怎么办? (TRUNC 截断)
6. observer 和 follower 区别? (observer 不 ACK, 只收 commit)
7. 连接谁连谁? (QuorumCnxManager connectOne + 仲裁)
8. quorum 失守怎么办? (leader shutdown)

## 架构师视角

9. 为什么严格顺序提交? (全序广播语义 — ZAB 核心, 对照 Raft 可乱序)
10. TRUNC 的风险? (follower 可能领先 — 旧 leader 残留; 新 epoch zxid 不 TRUNC)
11. 同步三模式选择? (窗口内 DIFF 优先, 窗口外 SNAP 兜底, 领先 TRUNC)
12. 连接仲裁的意义? (防双向连接死锁)
13. syncThrottler 流控? (SNAP 全量昂贵 — 限并发)
14. reconfig 时提交? (designatedLeader + allowedToCommit=false)
15. 对照 Raft? (ZAB 全序提案 vs Raft 并行日志; 两者都是多数派提交)
16. 为什么每半 tick 检查 quorum? (leader 自检 — 失守快速让位)

## 学生视角

17. 什么是原子广播? (所有节点按相同顺序收到相同数据)
18. 什么是两阶段? (先提案 → 多数确认 → 再提交)
19. 什么是 DIFF/TRUNC/SNAP? (增量补/截断/全量快照)
20. 为什么需要多数? (少数派无法保证数据不丢)
