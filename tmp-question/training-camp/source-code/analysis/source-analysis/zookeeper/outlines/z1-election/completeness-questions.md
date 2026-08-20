# Z-1 Leader 选举 — completeness-questions (全视角提问验证)

## 开发者视角

1. 选票包含什么? (leader/zxid/electionEpoch/peerEpoch 四元组)
2. 选举开始怎么发起? (logicalclock++ + 自荐 + sendNotifications)
3. 收到票怎么比较? (totalOrderPredicate: epoch>zxid>sid)
4. 达成多数后立即当选吗? (不 — finalizeWait 200ms 稳定窗口)
5. 收不到回执怎么办? (指数退避重发 200ms→60s)
6. 迟到的节点怎么跟上? (outofelection 学习已存在 leader)
7. observer 参与选举吗? (不 — OBSERVING 忽略)
8. 权重为 0 的节点能当选吗? (不能 — totalOrderPredicate 排除)

## 架构师视角

9. 为什么 epoch 优先于 zxid? (轮次权威 > 数据新鲜 — 旧轮高 zxid 票无效)
10. recvset 和 outofelection 为什么分开? (当前轮裁决 vs 历史学习 — 语义隔离)
11. 稳定窗口的意义? (防瞬间多数后票又变 — 抖动脉冲)
12. 2 节点为什么需要 Oracle? (majority=2, 恢复节点永远达不成 — ZOOKEEPER-3922)
13. 对比 Raft 选举? (无随机超时/无 prevote/无 term 持久化 — 全员自荐广播)
14. 脑裂防护? (多数派 + CP 语义; 分区即不可用)
15. reconfig 时投票? (双 QuorumVerifier + validVoter 双视图)
16. 指数退避为什么 200ms→60s? (网络抖动快速重试 + 长期分区不轰炸)

## 学生视角

17. 什么是 leader 选举? (多节点选一个当主)
18. 为什么数据最新的优先? (zxid 最大 = 数据最新 — 避免丢数据)
19. 什么是投票? (每节点广播自己选谁, 多数一致即当选)
20. epoch 是什么? (选举轮次 — 谁轮次新听谁的)
