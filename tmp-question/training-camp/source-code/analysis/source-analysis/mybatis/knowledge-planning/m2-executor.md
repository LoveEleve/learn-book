# M-2 SqlSession/Executor 链 — 模板方法执行主线 + 装饰链 + 路由

> 项目: MyBatis | 🔴 Deep / 1 篇 | DefaultSqlSession(341)+BaseExecutor(396)+Simple/Reuse/Batch+CachingExecutor(180)+StatementHandler 族(Routing/Prepared/Simple/Callable+Base 100)
> 基线: M-PLAN M-2 — 前置: **M-1 (4 工厂/注册表, 已交付)** — 展开 SqlSession 门面→Executor 模板→StatementHandler 路由

---

## §0.8

- 🔴 Deep，1篇 — 入口(**DefaultSqlSessionFactory.openSessionFromDataSource L88-113: Environment→TransactionFactory.newTransaction→configuration.newExecutor→DefaultSqlSession**) → 门面(**DefaultSqlSession L56-341: selectOne 三态 L73-85[0→null/>1→TooManyResults]; selectList→executor.query L150-160; update/insert/delete→executor.update+dirty=true L193-203; commit/rollback 按 isCommitOrRollbackRequired=!autoCommit&&dirty||force L261-262; close 时强制回滚未提交**) → 模板方法(**BaseExecutor: query 模板 L132-175[getBoundSql→createCacheKey→queryStack 守卫→localCache 命中/queryFromDatabase→deferredLoads→STATEMENT scope 清]; update 模板 L110-118[clearLocalCache→doUpdate]; 4 doXxx 抽象 L276-284; commit/rollback 清缓存+flushStatements L242-266**) → 一级缓存(**localCache PerpetualCache L58/68; createCacheKey L198-235[ms.id+rowBounds+sql+逐参数值+environment.id issue#176]; queryFromDatabase PLACEHOLDER 防递归 L331-355; 失效 5 清点[update/commit/rollback/STATEMENT scope/flushCacheRequired]**) → 子类策略(**SimpleExecutor.doQuery L57-69[newStatementHandler(wrapper)→prepareStatement 三步[getConnection→handler.prepare→parameterize]→handler.query]; Reuse/Batch 差异**) → 路由(**RoutingStatementHandler 按 statementType 三路 L41-56; PreparedStatementHandler.instantiateStatement 三分支 L80-95[Jdbc3KeyGenerator→RETURN_GENERATED_KEYS/KeyColumns; ResultSetType→prepare(sql,type,CONCUR_READ_ONLY)]; update 后 keyGenerator.processAfter L54-57[主键回填时机]; BaseStatementHandler 构造生成 parameterHandler+resultSetHandler L62-70**) → 二级缓存装饰(**CachingExecutor: ms.getCache→flushCacheIfRequired→useCache&&handler==null→tcm.getObject/putObject L96-119; commit/rollback→tcm.commit/rollback L121-140; delegate.setExecutorWrapper(this) L46[wrapper 语义]**)
- 设计模式: [模式: 门面+模板方法+策略+装饰器+代理]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DefaultSqlSession.java:73-85 | selectOne | **三态语义**: 1 条返回/>1 抛 TooManyResultsException/0 返回 null — 内部走 selectList | High |
| DefaultSqlSession.java:150-203 | 门面 | selectList→executor.query(ms, wrapCollection, rowBounds, handler); update/insert/delete→executor.update + dirty=true; dirty |= ms.isDirtySelect()(游标/列表 L124,153) | High |
| DefaultSqlSession.java:216-247,261-262 | 事务 | commit(force)/rollback(force): isCommitOrRollbackRequired=!autoCommit&&dirty\|\|force; 成功后 dirty=false | High |
| DefaultSqlSession.java:263-266 | close | close(): executor.close(isCommitOrRollbackRequired(false)) — 未提交写操作关闭时回滚(测试 shouldUpdateAuthorImplicitRollback) | High |
| BaseExecutor.java:132-175 | query 模板 | query: getBoundSql→createCacheKey→queryStack 守卫(L148,153,163)→localCache.getObject 命中/queryFromDatabase→queryStack==0 时 deferredLoads.load+STATEMENT scope 清(L163-173) | High |
| BaseExecutor.java:198-235 | CacheKey | **createCacheKey**: ms.id+rowBounds.offset/limit+sql+逐 ParameterMapping 参数值(模拟 DefaultParameterHandler L209)+environment.id(L230-233 issue#176) | High |
| BaseExecutor.java:332-355 | queryFromDatabase | **EXECUTION_PLACEHOLDER 防递归**: 执行前 putObject(key, PLACEHOLDER), finally removeObject, 完成后 putObject 真结果; CALLABLE→localOutputParameterCache | High |
| BaseExecutor.java:110-118,242-266 | 清缓存点 | 一级缓存失效 5 清点: update 前/commit/rollback/STATEMENT scope(L169-172 issue#482)/flushCacheRequired(L148-150) | High |
| SimpleExecutor.java:57-93 | 子类策略 | doQuery: newStatementHandler(**wrapper**)→prepareStatement[getConnection(transaction)→handler.prepare→handler.parameterize]→handler.query — 子类只实现差异 | High |
| RoutingStatementHandler.java:41-56 | 路由 | switch ms.getStatementType(): STATEMENT→Simple/PREPARED→Prepared/CALLABLE→Callable — 策略模式 | High |
| PreparedStatementHandler.java:48-57,80-95 | Prepared | update: ps.execute→getUpdateCount→**keyGenerator.processAfter**(回填时机); instantiateStatement 三分支: Jdbc3KeyGenerator 无 keyColumns→RETURN_GENERATED_KEYS/有→prepare(sql,keyColumns); ResultSetType.DEFAULT→普通/否则 prepare(sql,type,CONCUR_READ_ONLY) | High |
| CachingExecutor.java:96-119,121-140 | 二级缓存 | ms.getCache!=null→flushCacheIfRequired→useCache&&resultHandler==null→ensureNoOutParams→tcm.getObject 命中返回/否则 delegate.query→tcm.putObject; commit/rollback→tcm.commit/rollback | High |
| CachingExecutor.java:46 | wrapper | **delegate.setExecutorWrapper(this)** — 装饰器设为被装饰者 wrapper; BaseExecutor.doQuery 用 wrapper 建 StatementHandler(嵌套查询见最外层) | High |
| BaseExecutor.java:347-353 | 连接 | getConnection: transaction.getConnection()+ConnectionLogger 调试包装(queryStack 参数) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 执行链是单主线 — 1篇 (~66行) 按"门面(会话语义)→模板(执行骨架)→一级缓存→子类策略→路由→二级缓存装饰→wrapper"展开; 缓存细节衔接 M-7(导航), 参数/结果处理衔接 M-5(导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | DefaultSqlSession 门面语义 (selectOne 三态/dirty/commit 判定) | 🔴 | **为什么🔴**: 会话行为面 |
| P1-2 | BaseExecutor 模板方法 + 一级缓存 5 清点 | 🔴 | **为什么🔴**: 执行骨架 |
| P1-3 | createCacheKey 结构 (参数值参与) | 🔴 | **为什么🔴**: 缓存正确性 |
| P1-4 | 子类策略 + wrapper 语义 (SimpleExecutor/装饰链) | 🔴 | **为什么🔴**: 差异实现 |
| P1-5 | StatementHandler 路由 + Prepared 三分支 + 主键回填时机 | 🔴 | **为什么🔴**: SQL 执行本质 |
| P2-1 | CachingExecutor 二级缓存查询流程 (导航 M-7) | 🟡 | **为什么🟡**: M-7 前略述 |
| P2-2 | queryFromDatabase PLACEHOLDER 防递归 | 🟡 | **为什么🟡**: 并发正确性 |
| P3-1 | 连接获取/ConnectionLogger/事务超时 (导航 spring-tx) | 🟢 | **为什么🟢**: 边界协作 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **门面与事务语义** | 🔴 | 会话行为 |
| B | **模板+缓存** | 🔴 | 执行骨架 |
| C | **策略+路由+回填** | 🔴 | SQL 执行 |
| D | **装饰与边界** | 🟡 | 扩展面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 模板方法骨架 | BaseExecutor 的 query/update 是模板方法(**无 final 约束**, 清缓存→queryStack/本地缓存→doXxx), 4 个 doXxx 抽象 — Simple/Reuse/Batch 只实现差异; CachingExecutor 覆写 query/commit/rollback(L49+) 实现二级缓存装饰 | BaseExecutor.java:110-118,132-175,276-284; CachingExecutor.java:96-140 |
| q2 | 一级缓存失效 5 清点 | 失效点穷举: update 前(L116)/commit(L247)/rollback(L258)/STATEMENT scope(L169-172 issue#482)/flushCacheRequired(L148-150) — 写操作必经清缓存 | BaseExecutor.java:110-118,148-150,169-172,242-266 |
| q3 | CacheKey 参数值参与 | createCacheKey 逐 ParameterMapping 取值(hasAdditionalParameter/裸值/metaObject.getValue 三路 L215-226) — 同 SQL 不同参数不同 key; environment.id 参与(issue#176) | BaseExecutor.java:198-235 |
| q4 | queryStack+PLACEHOLDER | queryStack 守卫: 外层查询才清 STATEMENT 缓存/加载 deferredLoads(L163-173); EXECUTION_PLACEHOLDER 防同一 key 递归查询读到半成品(L332-355) | BaseExecutor.java:148-173,332-355 |
| q5 | dirty+close 回滚语义 | isCommitOrRollbackRequired = !autoCommit && dirty \|\| force; close 时未提交写操作自动回滚(测试 shouldUpdateAuthorImplicitRollback); commit(true) 空会话可提交(测试 shouldCommitAnUnUsedSqlSession) | DefaultSqlSession.java:216-247,261-266; 测试 SqlSessionTest |
| q6 | wrapper 语义 | CachingExecutor 构造 delegate.setExecutorWrapper(this)(L46); BaseExecutor.doQuery 用 wrapper 建 StatementHandler — 嵌套查询/插件看到最外层包装; 插件代理未拦的方法 invoke 透传(Plugin.java:52-58) | CachingExecutor.java:46; BaseExecutor.java:357,62; Plugin.java:52-58 |
| q7 | StatementHandler 路由 | RoutingStatementHandler switch statementType 三路(Simple/Prepared/Callable) — 委托模式, 无独立逻辑 | RoutingStatementHandler.java:41-56 |
| q8 | Prepared 三分支+回填 | instantiateStatement: Jdbc3KeyGenerator 无 keyColumns→RETURN_GENERATED_KEYS/有→prepare(sql,keyColumns); ResultSetType.DEFAULT→普通/否则 type+CONCUR_READ_ONLY; update 后 keyGenerator.processAfter 回填主键 | PreparedStatementHandler.java:48-57,80-95 |
| q9 | CachingExecutor 流程 | ms.getCache!=null→flushCacheIfRequired→useCache&&handler==null→tcm.getObject 命中返回/否则 delegate.query→tcm.putObject; commit→tcm.commit(延迟提交) | CachingExecutor.java:96-140 |
| q10 | 子类策略差异 | SimpleExecutor: doQuery 三步(prepare→parameterize→query), doFlushStatements 空实现 — 子类只覆写差异点 | SimpleExecutor.java:44-93 |
| q11 | 连接与日志 | getConnection=transaction.getConnection()+ConnectionLogger(queryStack 参数) 调试包装 — 事务连接延迟获取(导航 spring-tx) | BaseExecutor.java:347-353 |

→ 引出 M-6: 动态 SQL — MappedStatement.getBoundSql(parameter) 是模板 query 的第一步, DynamicSqlSource 运行时求值; M-5: ResultSetHandler.handleResultSets 是 query 的收尾。
