# M-2 SqlSession/Executor 链 — 模板方法执行主线 + 装饰链 + 路由
> 前置: [[M-1-configuration]] (4 工厂/注册表) | 复用: [[spring-tx]] (事务边界) | 对照: [[D-1-druid]] (借还/事务语义) | 引出: [[M-6-scripting]] [[M-5-mapping]] [[M-7-cache]]
> 🔴 Deep | 8 KP | [模式: 门面+模板方法+策略+装饰器+代理]
> Pass 2 闭环: q1(模板方法骨架) q2(一级缓存 5 清点) q3(CacheKey 参数值参与) q4(queryStack+PLACEHOLDER) q5(dirty+close 回滚) q6(wrapper 语义) q7(StatementHandler 路由) q8(Prepared 三分支+回填) q9(CachingExecutor 流程) q10(子类策略差异) q11(连接与日志)

**读者处境**: `session.selectList()` 一行调用穿过门面、Executor 模板、StatementHandler 路由、PreparedStatement 预编译 — 为什么同 SQL 二次执行不查库?没 commit 直接 close 更新为何被回滚?主键为何 execute 后才回填?

### 1. 门面 — DefaultSqlSession 的会话语义

场景: selectOne 查出 2 条会怎样?没 commit 就 close 会发生什么?
源码路径:
- `DefaultSqlSession.java:73-85` — selectOne 三态(q1): size==1 返回/`>1 抛 TooManyResultsException`/0 返回 null — 内部全走 selectList
- `DefaultSqlSession.java:150-203` — selectList→`executor.query(ms, wrapCollection(parameter), rowBounds, handler)`; update/insert/delete→`executor.update` + **dirty=true**; selectCursor/selectList `dirty |= ms.isDirtySelect()`(L124,153); **wrapCollection→ParamNameResolver.wrapToMapIfCollection: Collection→{"collection":x,"list":x(List)}, 数组→{"array":x}, 否则原样**(M-6 foreach collection="list" 取值的依据)
- `DefaultSqlSession.java:216-247,261-266` — commit(force)/rollback(force): `isCommitOrRollbackRequired = !autoCommit && dirty || force`(L262); **close(): executor.close(isCommitOrRollbackRequired(false))**(L263) — 未提交写操作关闭自动回滚
关键设计: 门面语义(q5): dirty 标记"会话内未提交写", commit/rollback/close 三处判定; autoCommit=true 不强制; 测试 shouldCommitAnUnUsedSqlSession/shouldUpdateAuthorImplicitRollback 双验证。[模式: 门面+脏标记]
数据流: selectList → getMappedStatement(M-1) → dirty |= isDirtySelect → executor.query → finally ErrorContext.reset。

### 2. 模板方法 — BaseExecutor 执行骨架 + 一级缓存

场景: 为什么同 SQL 二次执行不查库?写操作和查询如何共享清缓存逻辑?
源码路径:
- `BaseExecutor.java:132-175` — query 模板(q1): getBoundSql→`createCacheKey`→queryStack 守卫(L148,153,163)→`localCache.getObject(key)` 命中返回/否则 `queryFromDatabase`→queryStack==0 时 `deferredLoads.load`+`STATEMENT scope 清缓存`(L163-173, issue#482)
- `BaseExecutor.java:110-118` — update 模板: `clearLocalCache()`→doUpdate — **写操作必清缓存**
- `BaseExecutor.java:242-266,276-284` — commit/rollback: clearLocalCache→flushStatements→required 时 transaction 提交/回滚; 4 个 doXxx 抽象 — 子类只实现差异
- `BaseExecutor.java:332-355` — queryFromDatabase(q4): `putObject(key, EXECUTION_PLACEHOLDER)`(防递归半成品)→doQuery→removeObject→putObject 真结果; CALLABLE→localOutputParameterCache
关键设计: 模板方法(q1)+缓存失效 5 清点(q2): update 前(L116)/commit(L247)/rollback(L258)/STATEMENT scope(L169-172)/flushCacheRequired(L148-150) — 写操作必经清缓存, 读缓存命中零 SQL。[模式: 模板方法+缓存]
数据流: query → getBoundSql → createCacheKey → queryStack++ → localCache 命中? 返回 : queryFromDatabase(PLACEHOLDER→doQuery→真结果) → queryStack-- → 最外层时 deferredLoads 加载 + STATEMENT 清。

### 3. CacheKey — 参数值参与的缓存键

场景: 相同 SQL 不同参数会错误命中缓存吗?
源码路径:
- `BaseExecutor.java:198-235` — createCacheKey(q3): `cacheKey.update(ms.getId())`+rowBounds.offset/limit+`boundSql.getSql()`+逐 ParameterMapping 参数值(L211-229)+`environment.getId()`(L230-233, issue#176)
- `BaseExecutor.java:215-226` — 参数值三路取值: `boundSql.hasAdditionalParameter`(动态 SQL 附加参数)/parameterObject 裸值(有 TypeHandler)/`metaObject.getValue(propertyName)`(对象属性, 模拟 DefaultParameterHandler L209)
关键设计: 参数值参与 key(q3) — 同 SQL 不同参数绝不互相命中; 环境 id 参与防止多数据源串缓存; 取参逻辑与 DefaultParameterHandler 保持一致(避免 key 与执行参数不同步); **CacheKey 覆写 equals/hashCode 值语义**(harness 实证: 不覆写则 HashMap 引用比较缓存永不命中)。[模式: 复合键+值语义]
数据流: ms.id → offset → limit → sql → 每参数值 → environment.id → CacheKey。

### 4. 子类策略 + wrapper — Simple/Reuse/Batch 差异与最外层引用

场景: 三个 Executor 子类共享 95% 逻辑, 差异在哪?插件拦截后为什么用 wrapper 建 StatementHandler?
源码路径:
- `SimpleExecutor.java:57-93` — doQuery: `configuration.newStatementHandler(wrapper, ...)`→prepareStatement 三步(`getConnection(transaction)`→`handler.prepare(conn, timeout)`→`handler.parameterize(stmt)` L87-93)→handler.query; doFlushStatements 空实现
- `BaseExecutor.java:347-353` + `CachingExecutor.java:46` + `Plugin.java:52-58` — getConnection: transaction.getConnection+ConnectionLogger 包装; **`delegate.setExecutorWrapper(this)`** 装饰器设为被装饰者 wrapper; 插件未拦方法 invoke 透传
关键设计: 子类只覆写差异(q10): Simple 新建语句/Reuse 缓存语句/Batch 攒批; wrapper 语义(q6): BaseExecutor 用 wrapper(最外层包装)建 StatementHandler — 嵌套查询/拦截器看到完整装饰链, 非裸子类。[模式: 模板方法+装饰器+代理]
数据流: newExecutor(M-1) → [CachingExecutor(delegate=Simple)] → pluginAll 代理 → SimpleExecutor.doQuery 拿 this.wrapper(最外层) → newStatementHandler → prepareStatement → query。

### 5. StatementHandler 路由 + Prepared 执行 — SQL 落库的最后一公里

场景: statementType 三种值对应什么 JDBC 对象?主键什么时候回填?
源码路径:
- `RoutingStatementHandler.java:41-56` — 路由(q7): `switch ms.getStatementType()`: STATEMENT→SimpleStatementHandler/PREPARED→PreparedStatementHandler/CALLABLE→CallableStatementHandler — 纯委托无逻辑
- `PreparedStatementHandler.java:48-57` — update: ps.execute→`getUpdateCount`→**`keyGenerator.processAfter(executor, ms, ps, parameterObject)`**(主键回填时机); query: ps.execute→`resultSetHandler.handleResultSets(ps)`(M-5 导航)
- `PreparedStatementHandler.java:80-95` + `BaseStatementHandler.java:62-70` — instantiateStatement 三分支(q8): Jdbc3KeyGenerator 无 keyColumns→RETURN_GENERATED_KEYS/有→prepare(sql,keyColumns); ResultSetType.DEFAULT→普通/否则 prepare(sql,type,CONCUR_READ_ONLY); 构造生成 parameterHandler+resultSetHandler(过 4 工厂 pluginAll)
关键设计: 路由是策略模式(q7) — 三种 JDBC 执行方式零侵入切换; 回填时机固定(q8): 执行后 processAfter, 预编译方式与 keyGenerator 联动(RETURN_GENERATED_KEYS 仅 Jdbc3 用); 参数化 setter 委托 parameterHandler(M-5 导航)。[模式: 策略+委托]
数据流: prepareStatement → handler.prepare(instantiateStatement 三分支→timeout→fetchSize) → handler.parameterize(M-5) → update: execute→getUpdateCount→processAfter 回填 / query: execute→handleResultSets(M-5)。

### 6. 二级缓存装饰 — CachingExecutor 查询流程 (导航 M-7)

场景: 二级缓存和一级缓存谁先查?commit 时二级缓存怎么刷?
源码路径:
- `CachingExecutor.java:96-140` — query(q9): `ms.getCache()!=null`→flushCacheIfRequired→useCache&&handler==null→ensureNoOutParams→`tcm.getObject` 命中返回/否则 delegate.query→tcm.putObject; commit→delegate.commit→`tcm.commit`(延迟提交, rollback 丢弃)
关键设计: 装饰器两级缓存(q9): 先查二级(CachingExecutor)→未命中下钻一级(BaseExecutor.localCache)→未命中查库; **tcm 事务性延迟提交**(commit 才真正写入二级, 回滚丢弃); useCache=false 或自定义 resultHandler 时直通 delegate。[模式: 装饰器+事务性缓存]
数据流: query → ms.getCache? → tcm.getObject 命中返回 : delegate.query(一级缓存→库) → tcm.putObject → commit 时 tcm.commit 落库。
### 负面空间 — 执行链刻意不做的事
- **不做自动重试/连接池管理**: 失败直接抛、连接只 transaction.getConnection 借用 — 池语义归属连接池层(对照 D-1)
- **不做 SQL 改写**: 语句拼装发生在 MappedStatement 构建期(M-6), 执行期只按 BoundSql 原样预编译(分页等改写属 MP-5 插件)
- **不做线程安全**: DefaultSqlSession/BaseExecutor 零 synchronized — queryStack/dirty 均非同步字段, 会话单线程使用; 多线程共享需每线程独立会话

→ 引出: 模板 query 第一步 `ms.getBoundSql(parameter)` 是 M-6 动态 SQL 求值点; `resultSetHandler.handleResultSets` 是 M-5 查询收尾 → [[M-6-scripting]] [[M-5-mapping]] [[M-7-cache]]
