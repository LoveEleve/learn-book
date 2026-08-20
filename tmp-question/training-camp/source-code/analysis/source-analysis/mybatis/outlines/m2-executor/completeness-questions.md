# M-2 SqlSession/Executor 链 — completeness-questions

## 开发者视角

1. selectOne 查出 0 条/1 条/多条分别是什么行为?底层实现和 selectList 什么关系?
2. 同一 SqlSession 里连续两次相同 SQL 相同参数, 第二次还查库吗?为什么?什么操作会让缓存失效?
3. 不调 commit 直接 close, 会话里的 update 会怎样?autoCommit 和 dirty 分别起什么作用?
4. 主键回填 (useGeneratedKeys) 发生在哪个时机?RETURN_GENERATED_KEYS 是什么时候用的?
5. statementType=STATEMENT/PREPARED/CALLABLE 分别对应什么 JDBC 执行方式?谁负责路由?
6. BatchExecutor 的 flushStatements 什么时候触发?SimpleExecutor 为什么返回空列表?
7. resultHandler 参数传自定义 Handler 时, 二级缓存还命中吗?为什么?
8. CacheKey 由哪些部分构成?为什么参数值参与 key 很重要?
9. 嵌套查询 (延迟加载) 时, 内层查询会影响一级缓存的 STATEMENT 清理吗?

## 架构师视角

10. BaseExecutor 的模板方法设计: query/update 模板吸收了哪些公共逻辑?4 个 doXxx 抽象让子类只实现什么?
11. 一级缓存的 5 个失效清点分别是哪些?设计上为什么写操作必须先清缓存?
12. wrapper 字段的语义: CachingExecutor 构造时 delegate.setExecutorWrapper(this) 解决什么问题?插件拦截后 StatementHandler 看到的是谁?
13. queryFromDatabase 的 EXECUTION_PLACEHOLDER 机制防的是什么问题?和 queryStack 分工是什么?
14. CachingExecutor 的 tcm (TransactionalCacheManager) 为什么 commit 才真正写入二级缓存?回滚时呢?
15. dirty 标记 + isCommitOrRollbackRequired 的判定式 ( !autoCommit && dirty || force ) 覆盖了哪些场景?为什么 autoCommit 时不强制?
16. 主键回填时机固定在 execute 之后 (processAfter) — 与 JDBC 的 RETURN_GENERATED_KEYS 机制如何配合?

## 学生视角

17. 一次 selectList 从 SqlSession 到 JDBC 的完整调用链 (门面→模板→子类→路由→Prepared) 每一层做什么?
18. SimpleExecutor 的 prepareStatement 三步 (getConnection→handler.prepare→parameterize) 每一步对应什么?
19. PreparedStatementHandler.instantiateStatement 的三分支分别对应什么场景?
20. 一级缓存 (BaseExecutor) 和二级缓存 (CachingExecutor) 的查询顺序和写入时机有什么区别 (导航 M-7)?
21. ConnectionLogger 包装的连接在什么日志级别下生效?queryStack 参数传给它干什么用?
