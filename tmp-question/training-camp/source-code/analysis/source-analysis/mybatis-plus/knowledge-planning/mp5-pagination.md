# MP-5 分页插件 — willDoQuery count 预检 + beforeQuery 物理分页改写 + 方言

> 项目: MyBatis-Plus | 🔴 Deep / 1 篇 | PaginationInnerInterceptor(478)+IDialect(13 方言)+DialectFactory+DialectModel+Page(183)+ParameterUtils
> 基线: MP-PLAN MP-5 — 前置: **MP-4 (回调链宿主) + M-2 (Executor 语义)** — 展开 willDoQuery count→beforeQuery 改写→方言→参数消费

---

## §0.8

- 🔴 Deep，1篇 — count 预检(**willDoQuery L116-145: ParameterUtils.findPage(parameter)→page 判定[null/size<0/!searchCount/handler≠NO_RESULT_HANDLER→return true 不接管 L120-122]→count 查询[自定义 countId[buildCountMappedStatement L207-226] 或自动[buildAutoCountMappedStatement "_mpCount" L230-253, 复用原 ms sqlSource+Long resultMap]→autoCountSql[count 优化: 去 orderBy/去列, 测试 `select * from order_info left join(...)`→`SELECT COUNT(*) AS total ...` L260-275]→executor.query count]→page.setTotal→**continuePage[总数 0/页码越界且未开 overflow→false 短路] L432-450**) → SQL 改写(**beforeQuery L149-195: findPage→**concatOrderBy[orderBy 拼接 L277+]→size<0 且无 maxLimit→只加 orderBy L166-171→handlerLimit[maxLimit 超限归位 L454-456]→`dialect.buildPaginationSql(buildSql, page.offset(), page.getSize())` L177→DialectModel[setConsumer 链: 参数消费 consumer[isFirstParam offset/limit 标记] L100-145]**) → 方言(**IDialect: buildPaginationSql(originalSql, offset, limit)→DialectModel; MySqlDialect: `LIMIT ?`(offset=0)/`LIMIT ?, ?`(双参数 setConsumerChain) L25-40; 13 方言[MySql/Oracle12c/Oracle/Postgre/SQLServer/SQLServer2005/DB2/Sybase/Informix/GBase8s/Trino/XCloud]**; DialectFactory L32-75: DbType→IDialect EnumMap 缓存+分支实例化; dialect 获取: 构造 dbType/IDialect/否则 `JdbcUtils.getDbType(executor)` 自动识别 L196-202) → 页对象(**IPage L183+Page: current/size/total/pages/orders/maxLimit/searchCount/optimizeCountSql; offset(): current<=1→0, 否则 (current-1)*size(IPage.java:71-77); findPage(core/toolkit/ParameterUtils L42): 参数里找 IPage 实现[IPage 参数或 Map 里 "page" 键]**)
- 设计模式: [模式: 策略(方言)+模板+缓存(countMsCache)]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| PaginationInnerInterceptor.java:116-145 | count 预检 | willDoQuery: page 判定 4 条件不接管→count 查询(自定义/自动)→setTotal→continuePage 短路 | High |
| PaginationInnerInterceptor.java:149-195 | SQL 改写 | beforeQuery: orderBy 拼接→size<0 特例→handlerLimit→dialect.buildPaginationSql(offset, size) | High |
| PaginationInnerInterceptor.java:230-253 | count MS | buildAutoCountMappedStatement: ms.id+"_mpCount" 缓存, 复用 sqlSource+Long resultMap | High |
| PaginationInnerInterceptor.java:260-275 | count 优化 | autoCountSql: 去 orderBy/去 select 列(测试断言 COUNT(*) AS total) | High |
| PaginationInnerInterceptor.java:432-450 | 短路 | continuePage: 总数 0 或页码越界未开 overflow→false | High |
| MySqlDialect.java:25-40 | 方言 | LIMIT ?(offset=0)/LIMIT ?, ?(setConsumerChain 双参数) | High |
| DialectFactory.java:32-75 | 工厂 | DbType→IDialect EnumMap+分支; 自动识别 JdbcUtils.getDbType | High |
| PaginationInnerInterceptor.java:196-202 | 方言选择 | 构造 dbType/IDialect 优先; 否则 JdbcUtils.getDbType(executor) | High |
| DialectModel.java:100-145 | 参数消费 | setConsumer(offset/limit 位置标记)+consumers 注入 ParameterMapping | High |
| IPage.java/Page.java | 页对象 | current/size/total/orders/maxLimit/offset(): current<=1→0, 否则 (current-1)*size(IPage.java:71-77) | High |
| PaginationInnerInterceptorTest.java:21-99 | 测试 | optimizeCount/notOptimizeCount/withAs/groupBy/leftJoin 的 count 优化断言 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 分页是单机制 — 1篇 (~70行) 按"count 预检→SQL 改写→方言策略→参数消费→页对象"展开; 回调宿主衔接 MP-4(引用), 执行衔接 M-2(引用)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | willDoQuery count 预检 + continuePage 短路 | 🔴 | **为什么🔴**: 分页核心 |
| P1-2 | beforeQuery 物理改写 (orderBy/size 特例/limit) | 🔴 | **为什么🔴**: SQL 改写 |
| P1-3 | count 优化 (autoCountSql/buildAutoCountMappedStatement) | 🔴 | **为什么🔴**: 性能 |
| P1-4 | 方言策略 (IDialect/DialectFactory/13 实现) | 🔴 | **为什么🔴**: 跨库 |
| P2-1 | DialectModel 参数消费 (offset/limit 标记) | 🟡 | **为什么🟡**: 参数注入 |
| P2-2 | 页对象 IPage/Page (findPage 定位) | 🟡 | **为什么🟡**: 参数面 |
| P3-1 | 与 MP-4 宿主/M-2 执行协作 (引用) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **count 预检** | 🔴 | 分页第一步 |
| B | **SQL 改写** | 🔴 | 核心 |
| C | **方言与参数** | 🔴 | 跨库面 |
| D | **页对象** | 🟡 | 参数面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | count 预检流程 | willDoQuery: findPage 定位页对象→4 条件不接管(page null/size<0/!searchCount/自定义 resultHandler)→**count 查询在 willDoQuery 内部执行**(自定义 countId 或自动 "_mpCount" MappedStatement)→page.setTotal→continuePage 短路(总数 0 或页码越界) | PaginationInnerInterceptor.java:116-145,432-450 |
| q2 | count MappedStatement | buildAutoCountMappedStatement: **ms.id+"_mpCount" + computeIfAbsent 缓存**, 复用原 sqlSource/parameterMap, resultMap 换 Long 版 — 不新增 SQL 定义 | PaginationInnerInterceptor.java:230-253 |
| q3 | count 优化 | autoCountSql: 去 orderBy/优化 select 列(测试: left join 复杂 SQL→`SELECT COUNT(*) AS total ...`) — optimizeCountSql 可关 | PaginationInnerInterceptor.java:260-275; 测试 |
| q4 | SQL 改写 | beforeQuery: concatOrderBy(页排序拼接)→**size<0 且无 maxLimit→只加 orderBy 不构造分页 SQL**→handlerLimit(超 maxLimit 归位)→`dialect.buildPaginationSql(buildSql, page.offset(), page.getSize())` | PaginationInnerInterceptor.java:149-195,454-456 |
| q5 | 方言策略 | IDialect.buildPaginationSql(originalSql, offset, limit)→DialectModel; MySql: LIMIT ?(offset=0)/LIMIT ?, ?(双参数); DialectFactory EnumMap 缓存; 构造未指定时 JdbcUtils.getDbType(executor) 自动识别 | MySqlDialect.java:25-40; DialectFactory.java:32-75; L196-202 |
| q6 | 参数消费 | DialectModel.setConsumer(isFirstParam) 标记 offset/limit 的占位位置, consumers() 把分页参数注入 ParameterMapping — 改写后 SQL 的 ? 与参数绑定一致 | DialectModel.java:100-145 |
| q7 | 页对象定位 | ParameterUtils.findPage: 直接 IPage 参数或 Map 中 "page" 键 — 与 MP-1 注入 SQL 的 #{ew...} 无关, 是独立参数通道 | IPage.java; ParameterUtils |
| q8 | 与宿主协作 | 分页接管仅当 willDoQuery 返回 true 且 beforeQuery 改写 — MP-4 宿主重发 query 时用改写后 boundSql(M-2 执行) | MP-4 交付 |

→ 引出 MP-6: 乐观锁 — OptimisticLockerInnerInterceptor 是 willDoUpdate/beforeUpdate 时机的 InnerInterceptor。
