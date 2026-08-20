# J-2 日志复制 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. Replicator 有哪 5 个对外状态? Probe 状态到底在干嘛?
2. nextIndex 初始化是多少? 成功/失败分别怎么变?
3. 冲突回退的两段式: 什么时候批量跳, 什么时候逐条减?
4. 在途请求上限是多少? 超过会怎样?
5. 响应乱序怎么处理? reqSeq/requiredNextSeq 怎么用?
6. BallotBox.commitAt 的调用时机和参数?
7. follower 侧 setLastCommittedIndex 的前置条件?
8. 日志 RPC 超时为什么是 -1? 心跳超时是多少?

## 架构师视角

9. 为什么失败后走 Probe 而非直接重发? 探测一次往返的价值?
10. block 重试 vs 指数退避的取舍? 什么场景选哪个?
11. 流水线 256 在途的窗口设计? 与 TCP 窗口类比?
12. 心跳为什么复用 AppendEntries? committedIndex 每次携带的意义?
13. 计票为什么必须连续? 非连续提交会破坏什么?
14. 双写 (内存立即+磁盘异步) 的崩溃窗口在哪? 回调成功=已持久化怎么保证?
15. AppendBatcher 256 条合并刷盘的权衡? fsync 成本?
16. truncatePrefix 为什么不推进 diskId? braft PR#224 的教训?
17. appliedId 为什么是截断底线? 越过会怎样?
18. 配置日志双写 conf CF 的设计意图? 启动恢复成本?

## SRE/运维视角

19. 慢 follower 会拖垮 leader 吗? maxReplicatorInflightMsgs 怎么调?
20. 日志被截断 (truncateSuffix) 什么时候发生? 日志里怎么发现?
21. 快照安装失败会重试吗? 卡在 Snapshot 状态怎么办?
22. EIO (磁盘错误) 后节点怎么表现? 怎么恢复?
23. 升级版本时 v1/v2 日志兼容性怎么保证?

## 研究者视角

24. vs ZAB: 日志复制 vs 广播队列, 本质差异?
25. vs Kafka: 顺序确认 (HW) vs Raft commitIndex, 谁更严格?
26. vs Raft 论文: 流水线/批处理/心跳复用是论文没有的工程化?
27. learner 不参与计票的语义? 与 Kafka follower fetch 的对照?
28. vs braft (C++): 乱序响应排序是 Java 特有的问题吗?

## 学生视角

29. 什么是日志复制? leader 怎么让 follower 同步?
30. 什么是 nextIndex? 怎么知道从哪开始发?
31. 什么是 quorum? 多数派怎么算?
32. 什么是 fsync? 为什么日志必须落盘才确认?
33. 什么是日志冲突? 为什么 follower 日志可能和 leader 不一样?
