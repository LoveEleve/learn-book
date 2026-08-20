# C-3 分布式锁 — completeness-questions (全视角提问验证)

## 开发者视角

1. acquire() 抛 IOException("Lost connection...") 什么时候发生?
2. 同一线程 acquire 两次再 release 一次, 锁还持有吗?
3. release 时 lockCount 变负会怎样?
4. acquire(time, unit) 返回 false 时, 自己创建的节点被删了吗?
5. getParticipantNodes() 返回什么? 按什么排序?
6. 读锁和写锁的节点名前缀分别是什么?
7. 写锁降级读锁怎么操作?
8. 信号量 acquire(2) 超时, 已拿到的那 1 个租约怎么办?
9. MultiLock 部分成功时怎么回滚?

## 架构师视角

10. 为什么判定互斥用"序号位置"而不是"锁标记"?
11. 每个候选为什么只 watch 前驱? 负载是 O(1) 吗?
12. 为什么用 getData 而不用 exists 挂 watch?
13. 可重入为什么是本地计数? 跨进程可重入为什么不可能?
14. 读锁为什么 maxLeases=Integer.MAX_VALUE? 这个数字怎么参与判定?
15. 读写前缀等长为什么重要? 不等长会怎样?
16. 读→写升级为什么"永远不可能"? 升级死锁场景?
17. 会话过期 vs 显式 release 的锁释放路径差别?
18. guaranteed 删除解决什么问题? FailedDeleteManager 在哪兜底?
19. 信号量动态上限 (SharedCount) 的实现耦合?
20. 与 Redisson RLock 对比: watchdog 续期 vs 会话绑定, 哪个更好?

## 学生视角

21. 什么是临时顺序节点? 临时和顺序分别解决什么?
22. 什么是前驱? 为什么 watch 前驱就能知道轮到自己?
23. 什么是可重入锁? 为什么分布式锁也可重入?
24. 什么是读写锁? 读和写为什么不对称?
25. 什么是信号量? 和锁有什么区别?
