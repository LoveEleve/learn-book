# S-1 AT 两阶段提交 — completeness-questions (全视角提问验证)

## 开发者视角

1. execute 编排顺序? (传播决策→begin→业务→commit/rollback→清理)
2. 传播 6 种语义? (REQUIRED/REQUIRES_NEW/NOT_SUPPORTED/SUPPORTS/NEVER/MANDATORY)
3. Launcher vs Participant? (谁 begin/commit/rollback)
4. begin 做了什么? (createTime+bind+TM.begin)
5. 异常怎么处理? (rollbackOn ? rollback : commit)
6. 超时在哪检查? (客户端 commit 前 + TC commit 前)
7. 钩子有哪些? (7 个)
8. commit 重试几次? (client.tm.commit.retry.count)

## 架构师视角

9. 为什么模板方法? (统一编排 — begin/commit 顺序强制)
10. 为什么角色分离? (嵌套事务只 join 不驱动 — 单发起者语义)
11. 为什么正向 commit 反向 rollback? (依赖顺序: 先提交先建者, 先回滚后建者)
12. 为什么先关会话再提交? (防新分支注册竞态 — close 先于状态迁移)
13. 为什么失败转异步重试? (TC 不阻塞 — queueToRetryCommit/Rollback)
14. 为什么启发式仅 Finished? (事务不存在=状态不确定 — "may be rollbacked")
15. 为什么双时钟超时? (客户端/服务端各自判定, 无全局时钟)
16. 对照 Spring 7 传播? (Seata 无 NESTED — 无 savepoint 语义)

## 学生视角

17. 什么是全局事务? (跨服务的一组本地事务)
18. 什么是 Phase1/Phase2? (本地提交 + 全局协调)
19. 什么是 XID? (全局事务编号 = IP:PORT:事务ID)
20. 什么是 undo_log? (Phase2 回滚的补偿数据, S-2)
