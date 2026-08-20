# C-2 Leader 选举 — completeness-questions (全视角提问验证)

## 开发者视角

1. LeaderLatch.start() 后多久能当选? await() 怎么用?
2. await() 抛 EOFException 是什么意思?
3. LeaderLatch 当选后怎么做业务? 业务失败要不要 close?
4. CloseMode.SILENT 和 NOTIFY_LEADER 差在哪?
5. LeaderSelector 的 takeLeadership 返回后会发生什么?
6. autoRequeue(true) 后断连重连会不会重新竞选?
7. getLeader() 返回的 Participant 什么结构?
8. LeaderSelectorListenerAdapter 省了什么代码?

## 架构师视角

9. why ephemeralOwner 二次确认? 什么场景会误判?
10. latch 的 watch 前驱与锁的 watch 前驱有什么异同?
11. why LeaderSelector 复用 InterProcessMutex 而不是 latch?
12. takeLeadership 阻塞 = 任期, 这个设计好在哪?
13. CancelLeadershipException 为什么必须在 stateChanged 抛? "dated leadership" 是什么?
14. SUSPENDED 时领导权保留还是让位? 两种策略怎么选?
15. 会话过期后旧 leader 的节点怎么清理? 新 leader 什么时候当选?
16. CURATOR-724 是什么场景? reset 为什么能自愈?
17. 集群缩容到 0 再扩容, latch 会发生什么?
18. 对比 ZK 官方 LeaderElectionSupport: 状态机 7 态 vs Curator 的 3 态?

## 学生视角

19. 什么是选主? 为什么需要选主?
20. 临时顺序节点怎么选出最小者?
21. 什么是回调式 API? takeLeadership 和监听器有什么区别?
22. 为什么领导权会随会话过期丢失?
23. 什么是观察者 (watcher)? 前驱删除怎么通知我?
