# MP-5 分页插件 — completeness-questions

## 开发者视角

1. 一次 selectPage 分页查询实际执行几次 SQL?count 查询发生在哪个回调?
2. 什么时候不会执行 count?(page null/size<0/searchCount false/自定义 resultHandler 分别什么场景?)
3. 页码越界(如只有 3 页查第 5 页)会怎样?overflow 配置控制什么?
4. 自定义 countId 怎么用?不配置时 count 的 MappedStatement 哪来的?
5. autoCountSql 优化了什么?什么 SQL 不会被优化?
6. size<0 且不配置 maxLimit 时 SQL 会怎样?只排序不分页怎么实现?
7. 不配置 DbType 时方言怎么选?JdbcUtils.getDbType 从哪拿?
8. Page 对象怎么传给 mapper?IPage 参数或 Map 里 "page" 键的区别?

## 架构师视角

9. count 与 data 两段式设计: 为什么 count 放 willDoQuery 而不是 beforeQuery?短路收益?
10. buildAutoCountMappedStatement 运行期构造 MappedStatement(复用 sqlSource+换 Long resultMap)的设计意图?
11. autoCountSql 的优化策略(去 orderBy/简化列)与 optimizeCountSql 开关的取舍?
12. 方言策略: IDialect 收敛差异到 buildPaginationSql 一点 + DialectFactory 自动识别 — 新增数据库要做什么?
13. DialectModel 的 setConsumer 占位标记机制 — 与 M-5 ParameterMapping 的对接?
14. 物理分页 vs M-5 RowBounds 逻辑分页: 各自适用场景?为什么 MP 选物理?
15. 改写 SQL 后重发 query 的缓存语义(与未分页查询的 CacheKey 差异, 导航 M-2)?

## 学生视角

16. 一次分页查询的完整调用链(willDoQuery count→beforeQuery 改写→重发→M-2 执行)?
17. MySQL 的 LIMIT ?, ? 和 LIMIT ? 两种形态分别什么时候用?
18. page.offset() 怎么算?current=1, size=10 的 offset?
19. countMsCache 缓存了什么?为什么 count 结果不缓存?
20. 13 种方言的实现差异主要在哪?
