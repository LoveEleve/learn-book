# M-1 Configuration 核心+装配 — completeness-questions

## 开发者视角

1. XMLConfigBuilder 为什么只能 parse 一次?什么场景下会踩到 "Each XMLConfigBuilder can only be used once"?
2. `<properties>` 的 XML 内嵌、resource/url 文件、代码构造传入三个来源, 合并优先级是什么?resource 和 url 能同时写吗?
3. 写错 `<setting name="foo">` 为什么启动就报 "not known"?校验机制是什么?
4. 同一个 statement id 重复注册(如两个 XML 都定义 selectById)会发生什么?为什么?
5. `useGeneratedKeys` 不开时, insert 语句的主键回填是什么行为?keyGenerator 默认值是什么?
6. `<select>` 的 fetchSize/timeout/statementType 不配置时是什么默认值?在哪里定的?
7. mapper XML 被引用两次(两个 config 都加载同一 resource)会重复注册报错吗?为什么?
8. resultMap 互相引用(循环)时, 为什么第一遍解析不报错, 后面却能正常使用?
9. databaseId 双路径是什么意思?多数据库环境下一个 statement 能有几个版本?

## 架构师视角

10. 11 元素分区定序的依赖链是什么?为什么 typeAliases 在 plugins 之前、mappers 最后?vfsImpl 为什么必须提前加载?
11. settings 白名单用 MetaClass.hasSetter 反射校验而非硬编码枚举 — 这个设计好在哪?有什么代价?
12. StrictMap 相比普通 ConcurrentHashMap 改了什么语义?短名自动注册和 Ambiguity 占位解决什么问题?
13. incomplete 延迟解析(4 集合+removeIf 成功移除失败保留)如何解决循环引用?为什么 parsePendingResultMaps 要 do-while 多轮?
14. 4 个处理器工厂全部经过 interceptorChain.pluginAll — 这个"汇聚点设计"对插件机制意味着什么?插件开发者需要知道几个拦截点?
15. newExecutor 的装饰顺序为什么是 缓存在内、插件在外?反过来会有什么问题?
16. Configuration 构造注册的 22 个内置别名分成几类?哪些会被 XML 的 type 属性直接引用?

## 学生视角

17. SqlSessionFactoryBuilder.build → parse → DefaultSqlSessionFactory 的完整调用链是什么?Configuration 在哪一步创建?
18. MappedStatement 有 24 个字段, 哪些是执行必需的(sqlSource/parameterMap/resultMaps), 哪些是可选优化(fetchSize/timeout)?
19. buildAllStatements 的触发链顺序(resultMaps→cacheRefs→statements→methods)为什么这样排?
20. XMLMapperBuilder.configurationElement 的六段解析(cache-ref/cache/parameterMap/resultMap/sql/语句)与 MP-2 的 TableInfoHelper 管线有什么对应关系(导航)?
21. getMappedStatement 为什么默认会先 buildAllStatements?这和 MP 注入器的加载时序有什么关系(导航 MP-1)?
