# Z-8 Recipes — completeness-questions (全视角提问验证)

## 开发者视角

1. 三个 recipes 分别解决什么? (选举/互斥锁/FIFO 队列)
2. 共同模式? (顺序节点 + 最小序号 + 前驱 watch)
3. 选举怎么判定? (快照排序找自己位置)
4. 锁怎么幂等创建? (sessionId 前缀 findPrefixInChildren)
5. 重试规则? (RETRY_COUNT=10, SessionExpired 直抛 / ConnectionLoss 退避)
6. 队列 FIFO 怎么保证? (TreeMap\<Long\> 序列排序)
7. take 怎么阻塞? (getChildren 注册 LatchChildWatcher + await)
8. unlock 做什么? (delete + lockReleased 回调)

## 架构师视角

9. 为什么序号全序能替代互斥协议? (ZK 服务端串行化创建 → 全序天然互斥)
10. 为什么用前驱 watch 而非轮询? (O(1) 事件驱动 vs O(N) 轮询; 但取 watch 失败无兜底)
11. 选举 vs 锁的同一性? (Javadoc "exclusive write lock or to elect a leader" — 同算法两语义)
12. ephemeral vs persistent 的选择? (会话绑定 vs 数据存续 — 队列数据不可随消费者消失)
13. 前驱消失竞态两种处理? (LES 递归重读 vs WriteLock 只 warn — 一致性面)
14. 会话失效怎么暴露? (ephemeral 消失 → 他人重选; 自身感知靠 SessionExpired 异常)
15. 对照 Curator? (可重入/超时/重试策略/连接状态回调 — 官方最小实现)
16. 队列消费语义? (at-most-once — take 即删无 ack)

## 学生视角

17. 什么是顺序节点? (创建时自动加递增序号)
18. 什么是前驱? (序号比我小的节点)
19. watch 怎么用? (前驱删除时通知)
20. 最小序号为什么胜出? (全序唯一最小 → 无二主)
