# MP-5 分页插件 — willDoQuery count 预检 + beforeQuery 物理改写 + 方言策略

> 前置: [[MP-4-plugin]] (回调宿主) | 复用: [[M-2-executor]] (query 重发) | 对照: [[M-5-mapping]] (RowBounds 逻辑分页 vs 物理分页) | 引出: [[MP-6-optimistic]]
> 🔴 Deep | 7 KP | [模式: 策略(方言)+模板+缓存]
> Pass 2 闭环: q1(count 预检流程) q2(count MappedStatement) q3(count 优化) q4(SQL 改写) q5(方言策略) q6(参数消费) q7(页对象定位) q8(与宿主协作)

**读者处境**: 配一个 PaginationInnerInterceptor, selectPage 就自动分页 — 一次分页查询实际执行几次 SQL?count 查询怎么算的?MySQL 的 LIMIT 和 Oracle 的 ROWNUM 怎么统一?为什么页码越界返回空列表?这篇拆分页插件的三步: count 预检、SQL 物理改写、方言参数消费。

### 1. count 预检 — willDoQuery 内部执行 count + continuePage 短路

场景: 一次分页查询执行几次 SQL?什么时候直接短路不查数据?
源码路径:
- `PaginationInnerInterceptor.java:116-145` — willDoQuery(q1): `ParameterUtils.findPage(parameter)`→**4 条件不接管**(page null/size<0/!searchCount/自定义 resultHandler, L120-122)→**count 查询在此执行**: countMs 构建(自定义 countId 或自动 L124-136)→`executor.query(countMs, ..., countSql)`(L139)→`page.setTotal(total)`(L145)→`continuePage(page)`(L146)
- `PaginationInnerInterceptor.java:432-450` — continuePage: **总数 0 或页码越界(current>pages)且未开 overflow→false**(宿主短路返回空列表, MP-4 语义)
关键设计: count 与 data 两段式(q1): willDoQuery 内先 count(决定 total/是否短路), beforeQuery 再改写数据 SQL — **count 是额外一次查询**; 短路场景(总数为 0/页码越界)省掉数据查询。[模式: 预检+短路]
数据流: query 拦截 → findPage → count 条件? → count 查询 → setTotal → continuePage? → 短路 emptyList : beforeQuery 改写。

### 2. count 基础设施 — _mpCount MappedStatement + autoCountSql 优化

场景: count 的 MappedStatement 从哪来?count SQL 怎么优化?
源码路径:
- `PaginationInnerInterceptor.java:230-253` — buildAutoCountMappedStatement(q2): **ms.id+"_mpCount" + computeIfAbsent 缓存**, 复用原 sqlSource/parameterMap/cache, **resultMap 换 Long 版** — 运行时构造 MappedStatement 不新增 XML
- `PaginationInnerInterceptor.java:207-226` — buildCountMappedStatement: 自定义 countId(带 namespace 补全)优先
- `PaginationInnerInterceptor.java:260-275` — autoCountSql(q3): **去 orderBy/优化 select 列**; 测试: `select * from order_info left join(...)`→`SELECT COUNT(*) AS total ...`; optimizeCountSql 可关(page.optimizeCountSql())
关键设计: 运行期构造(q2): 复用原语句的 SqlSource 造 count 版 MappedStatement(只换 resultMap)— 零配置; count 优化(q3): 去 orderBy/简化列提升 count 性能, 复杂 SQL(left join 子查询)保留语义。[模式: 运行期构造+缓存+优化]
数据流: count 需要 → countId? getMappedStatement : buildAutoCount("_mpCount" 缓存) → autoCountSql(优化) → query。

### 3. SQL 改写 — beforeQuery 三步: orderBy → size 特例 → limit

场景: 分页 SQL 怎么拼?不想要 LIMIT 只排序怎么办?
源码路径:
- `PaginationInnerInterceptor.java:149-195` — beforeQuery(q4): `concatOrderBy`(页排序 OrderItem 拼接, L277+)→**size<0 且无 maxLimit→只加 orderBy 不构造分页 SQL**(L166-171)→`handlerLimit`(超 maxLimit 归位, L454-456)→**`dialect.buildPaginationSql(buildSql, page.offset(), page.getSize())`**(L177)
- offset 计算: **IPage.offset(): current<=1→0, 否则 (current-1)*size**(IPage.java:71-77)
关键设计: 分页参数化(q4): 改写只替换 SQL 文本(方言拼接), **offset/limit 以 ? 占位走 DialectModel 参数消费** — 预编译安全; size<0 特例是"只排序不分页"模式(limit 不拼)。[模式: 文本改写+参数占位]
数据流: beforeQuery → concatOrderBy → size<0? 只排序 : handlerLimit → dialect.buildPaginationSql → DialectModel。

### 4. 方言策略 — IDialect 13 实现 + DialectFactory + 自动识别

场景: MySQL/Oracle/PG 的分页语法差异怎么统一?不配 dbType 会怎样?
源码路径:
- `IDialect.java` — 契约: `buildPaginationSql(originalSql, offset, limit)` → DialectModel
- `MySqlDialect.java:25-40` — **LIMIT ?(offset=0 单参数)/LIMIT ?, ?(双参数 setConsumerChain)**(q5)
- `DialectFactory.java:32-75` — **DbType→IDialect EnumMap 缓存**(L32)+DbType 分支实例化(Oracle12c/SQLServer/DB2...)
- `PaginationInnerInterceptor.java:196-202` — 方言选择: 构造 dbType/IDialect 优先; **否则 `JdbcUtils.getDbType(executor)` 连接元数据自动识别**
关键设计: 策略模式(q5): 方言差异收敛到 buildPaginationSql 一点; 自动识别兜底(未配置时从连接拿 DbType)— 开箱即用。[模式: 策略+工厂+自动识别]
数据流: beforeQuery → dialect(构造指定? : JdbcUtils.getDbType) → buildPaginationSql → DialectModel。

### 5. 参数消费 — DialectModel setConsumer 链 + 页对象定位

场景: 改写后的 ? 怎么和 offset/limit 值绑定?Page 对象怎么传进来?
源码路径:
- `DialectModel.java:100-145` — **setConsumer(isFirstParam) 标记 offset/limit 占位位置**; setConsumerChain=双参数标记; consumers() 把分页参数注入 ParameterMapping(q6)
- `IPage.java:71-77`/`Page.java` — 页对象: current/size/total/pages/orders/maxLimit/searchCount/optimizeCountSql; **offset(): current<=1→0, 否则 (current-1)*size**
- `core/toolkit/ParameterUtils.findPage`(L42) — 定位(q7): 直接 IPage 参数或 Map 中 "page" 键
关键设计: 参数消费(q6): DialectModel 记录 offset/limit 在改写 SQL 中的占位顺序, consumers 时按位置补 ParameterMapping+参数值 — 与 M-5 DefaultParameterHandler 绑定兼容; findPage 双通道(q7): 独立参数位与 MP-1 的 ew 参数通道无关。[模式: 占位标记+消费]
数据流: buildPaginationSql → DialectModel(offset/limit 标记) → **mpBoundSql.sql(改写后 SQL) 写回**(L190-191) → consumers(补 ParameterMapping) → 重发 query → M-5 绑定。

### 负面空间 — 分页插件刻意不做的事

- **不做逻辑分页**: 物理分页改写(数据库层 LIMIT), 不内存分页 — 与 M-5 RowBounds 对照
- **不做 count 自定义缓存**: countMsCache 仅缓存 MappedStatement 构造, count 结果每次查(数据实时性)
- **不做多表复杂优化**: 极端复杂 SQL(嵌套子查询)的 count 优化可能降级为包裹 COUNT(*)

→ 引出: 乐观锁插件是 willDoUpdate/beforeUpdate 时机的 InnerInterceptor — 下一篇拆 @Version CAS 更新 → [[MP-6-optimistic]]
