# J-4 成员变更 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. CliServiceImpl.addPeer 的完整流程? getLeader 怎么找?
2. ConfigurationEntry.isStable() 的语义? oldConf 什么时候清空?
3. ConfigurationCtx 四个 stage 分别做什么? nextStage 转移条件?
4. waitCaughtUp 的 dueTime 是多少? 超时后怎么办?
5. unsafeRegisterConfChange 的防护有哪些? 相等配置为什么直接成功?
6. reset 时 done 回调什么错误? version++ 的意义?
7. learner2Follower 做了哪两步? 为什么要两步?

## 架构师视角

8. 为什么新节点必须先追平? 落后节点投票的危险?
9. 两条日志 (JOINT+STABLE) 完成变更的设计? 为什么不是一条?
10. 双配置的并集成员视图对选举的影响? 旧节点何时失去投票权?
11. 为什么"回滚 = 不落盘"? 已提交一半怎么收敛 (flush)?
12. 变更中为什么不能 transferLeader? 旧配置候选的危险?
13. 追平期限 = 一个选举超时的取舍? 太短/太长会怎样?
14. 与 etcd learner 提升流程对照? 差异?
15. ELEADERREMOVED 让位的时机? 为什么在 STABLE 后?
16. ConfigurationManager 的配置历史怎么与日志/快照联动?

## SRE/运维视角

17. 加节点失败 (ECATCHUP) 怎么排查? 日志特征?
18. 变更中 leader 挂了, 集群怎么办? 新 leader 怎么收敛?
19. 节点被 remove 后日志里有什么? 已提交的配置变更历史在哪?
20. resetPeer 什么时候用? 风险?
21. 集群缩容到 2 节点安全吗? quorum 变化要注意什么?

## 研究者视角

22. vs Raft 论文 §6: 单节点变更 vs joint consensus, 为什么 SOFAJRaft 用 joint?
23. vs etcd: learner 机制对比?
24. vs ZAB: 成员变更协议差异?
25. 双配置日志的提交安全性与普通日志有什么不同?

## 学生视角

26. 什么是成员变更? 为什么危险?
27. 什么是 joint consensus? 为什么叫"联合共识"?
28. 什么是追平? 日志怎么追上?
29. 什么是 quorum? 配置变了 quorum 怎么变?
30. 什么是领导权转移? 和选举有什么区别?
