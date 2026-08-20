# MP-4 插件体系 — completeness-questions

## 开发者视角

1. MybatisPlusInterceptor 拦了哪些方法?为什么是这 5 个?
2. 分页插件的 SQL 改写发生在哪个回调?改写后怎么生效?
3. willDoQuery 返回 false 会怎样?willDoUpdate false 呢?什么时候用这个能力?
4. beforeGetBoundSql 什么时候被调用?和 beforePrepare 的区别?
5. 实现一个自定义 InnerInterceptor 最少写什么?7 个方法都必写吗?
6. 配置文件里的 @page 和 page:limit 怎么变成插件?setProperties 怎么工作?
7. plugin() 覆写做了什么?为什么只包装 Executor/StatementHandler?

## 架构师视角

8. 5 个 @Signature 的选择依据 — 为什么覆盖 prepare/getBoundSql/update/query 就够 SQL 改写?
9. query 重发机制 (beforeQuery 改 boundSql→createCacheKey→executor.query) 的设计: 缓存/插件链如何自然生效?
10. willDoXxx(决策)+beforeXxx(执行)分离的回调模板 — 与 M-4 Interceptor 的单 intercept 对比?
11. 宿主模式: MybatisPlusInterceptor 是"总拦截器"再分派 InnerInterceptor 子链 — 与 M-4 的插件级拦截的层级关系?
12. 配置驱动装配 (PropertyMapper @分组) 的扩展性 — 新增插件需要改代码吗?
13. 为什么 ParameterHandler/ResultSetHandler 不拦?什么场景会需要拦它们(对照 M-4)?

## 学生视角

14. 一次带分页插件的 selectList 的完整调用链 (4 工厂→pluginAll→intercept→InnerInterceptor→重发 query)?
15. isUpdate 怎么判断?args.length==2 的含义?
16. boundSql 在 4 参/6 参签名下分别怎么拿?
17. willDoQuery false 返回 emptyList 和返回 null 的区别?
18. 配置驱动: @page 别名机制的设计意图?
19. 与 M-4 Plugin.wrap 的关系: MybatisPlusInterceptor 的 plugin() 为什么覆写?
20. 分页插件改写的 boundSql 重新走 executor.query 后, 一级缓存还会命中吗?
