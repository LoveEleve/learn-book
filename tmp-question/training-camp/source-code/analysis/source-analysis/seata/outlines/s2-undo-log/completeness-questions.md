# S-2 undo_log 机制 — completeness-questions (全视角提问验证)

## 开发者视角

1. undo_log 何时写? (Phase1 本地事务 commit 时 flush)
2. 表结构? (branch_id/xid/rollback_info/log_status/log_created/log_modified)
3. log_status 两值? (Normal 0 / GlobalFinished 1)
4. 回滚流程? (行锁 → 校验 → 逆序反向执行 → 删除)
5. 反向 SQL 怎么生成? (DELETE→INSERT / INSERT→DELETE / UPDATE→UPDATE SET)
6. 压缩规则? (enable && >64k → zip)
7. 校验三步? (before==after 跳过 / after==current 通过 / before==current 跳过)
8. dirty 怎么办? (Unretriable + 人工校准)

## 架构师视角

9. 为什么镜像对而非 SQL 记录? (前后对比才能校验脏数据)
10. 为什么行锁? (Phase1 期间锁目标行 — 防并发改破坏补偿)
11. 为什么逆序回滚? (sqlUndoLogs reverse — 依赖顺序: 后执行的先补偿)
12. 为什么无限重试? (for(;;) + 行锁 — 最终一致; 约束冲突重试, 脏数据停)
13. 为什么 GlobalFinished? (#489: 防 Phase1 提交窗口 — 先标记后提交防护)
14. 为什么 UPDATE 主键守卫? (PK 改变 → 镜像行数不等 → 无法定位)
15. 对照 binlog? (物理反向 vs 逻辑补偿)
16. 为什么方言独立? (SQL 语法差异 — SPI 13 方言)

## 学生视角

17. 什么是 undo_log? (回滚用的补偿数据表)
18. 什么是 beforeImage? (操作前的数据快照)
19. 什么是反向 SQL? (把数据改回去的 SQL)
20. 什么是脏数据? (当前数据与镜像不一致)
