# MP-1 SQL 自动注入 — completeness-questions

## 开发者视角

1. BaseMapper 里没有 XML 没有注解, 为什么 insert/selectList 能执行?注入发生在哪一步?
2. 哪些方法是无条件注入的?哪些依赖主键?无 @TableId 的实体调 selectById 会发生什么?
3. XML 里自己写了 selectById, MP 注入的同名方法会怎样?为什么是 warn 不是报错?
4. 自定义一个注入方法(继承 AbstractMethod)最少要写什么?injectMappedStatement 里做什么?
5. Insert 的主键策略怎么选?IdType.AUTO 和 Sequence 分别走什么?
6. 同一个 mapper 会被注入两次吗?mapperRegistryCache 起什么作用?
7. SQL 模板存在哪?%s 占位符怎么填充?
8. sqlSelectColumns 什么时候返回 *?什么时候返回列名列表?

## 架构师视角

9. 注入器方法面由元数据(havePK)驱动 — 这个设计与 MP-2 的依赖关系是什么?
10. "用户自定义优先"(hasMappedStatement 跳过)的冲突语义 — 与 M-1 StrictMap 的"重复抛"如何共存?(一个跳过一个是抛, 为什么?)
11. AbstractMethod 的 SQL 片段族(sqlSet/sqlWhereEntityWrapper/optlockVersion)为什么集中在模板基类而非方法类?
12. inspectInject 的防重复(mapperRegistryCache)与热重载: mapperRegistryCache 以什么为键?什么时候失效?
13. 12 个默认方法为什么是这 12 个?SelectPage/SelectMapsPage 为什么不在默认注入器里?
14. createSqlSource 统一 <script> 包裹 — 与 M-6 的动态 SQL 分流什么关系?

## 学生视角

15. mapper.insert(user) 从接口方法到 SQL 执行的完整链路(注入期+执行期)?
16. SqlMethod 枚举的三要素(method/描述/SQL 模板)分别被谁消费?
17. Insert 的 columnScript/valuesScript 怎么用 MP-2 的片段组装?convertTrim 干什么?
18. addSelectMappedStatementForTable 与 addMappedStatement 的关系?
19. 无主键实体的注入结果: 哪些方法有?哪些没有?调用没有的方法报什么错(导航 M-3)?
20. 注入的 MappedStatement 与 XML 解析的 MappedStatement 在 M-1 StrictMap 里如何共存?
