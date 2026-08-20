# S-4 DataSource 代理 — completeness-questions (全视角提问验证)

## 开发者视角

1. 三层代理? (DataSourceProxy → ConnectionProxy → StatementProxy)
2. init 检查什么? (方言/undo_log 表/资源注册)
3. 提交拦截? (doCommit 三分支)
4. register 守卫? (无 undo/lockKey 不注册分支)
5. report 语义? (默认不报成功; 失败必报 5 次重试)
6. LockRetryPolicy? (branchRollbackOnConflict 默认 true)
7. 上下文? (xid/branchId/undo/lockKeys/savepoints)
8. reset? (提交/回滚后全清)

## 架构师视角

9. 为什么提交是唯一拦截点? (Seata 无 SQL 解析介入 — 镜像在执行链, 挂钩在提交)
10. 为什么只读不注册分支? (无写操作无分支 — 减少 TC 负担)
11. 为什么 report 成功默认关? (Phase1 完成可省 — TC 从 undo_log 推断?)
12. 为什么 autoCommit 拦截? (JDBC 规范: false→true 隐式提交)
13. 为什么 FailFast 降级? (autoCommit 单语句本地锁已释放 — 需转可重试)
14. 对照 HikariCP? (池代理 vs 事务代理)
15. 为什么 savepoint 记录? (内嵌事务的上下文恢复面)
16. 为什么 fast-fail? (undo 表缺失 → 启动即失败, 不运行期爆雷)

## 学生视角

17. 什么是代理? (包一层拦截原对象行为)
18. 什么是分支注册? (本地事务向 TC 报备)
19. 什么是 PhaseOne_Done? (Phase1 完成状态)
20. 什么是连接上下文? (连接上的事务状态)
