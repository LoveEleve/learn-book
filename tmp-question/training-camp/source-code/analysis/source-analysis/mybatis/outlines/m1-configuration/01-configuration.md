# M-1 Configuration 核心+装配 — XML→Configuration 管线与 Hub 注册表

> 前置: [[spring-jdbc-C-11]] (JDBC 内核) | 复用: [[H-13-hikaricp]] [[D-7-druid]] (datasource 复用) | 对照: [[S-2-auto-config]] (Boot 条件装配 vs XML 分区装配) | 引出: [[M-2-sqlsession-executor]] [[M-4-plugin]] [[MP-1-sql-injector]]
> 🔴 Deep | 9 KP | [模式: 门面+Builder+反射校验+注册表+延迟解析+装饰链]
> Pass 2 闭环: q1(11 元素分区定序) q2(反射白名单) q3(三级合并) q4(parse 一次性) q5(StrictMap) q6(incomplete 延迟解析) q7(4 工厂汇聚点) q8(newExecutor 装饰链) q9(MappedStatement 默认值) q10(Mapper XML 装配) q11(mappers 四路)

**读者处境**: 一个 `mybatis-config.xml` 几行配置, 启动后 `selectList()` 就能执行 — XML 里的 `<settings>`/`<typeAliases>`/`<mappers>` 按什么顺序、什么机制变成注册表和 MappedStatement?配错 setting 名为何启动即报错?这篇拆装配管线与全局注册表 — 一切 SQL 执行的元数据来源。

### 1. 入口与装配管线 — 一次性 Builder + 11 元素分区定序

场景: 同一个 XMLConfigBuilder 能 parse 两次吗?`<properties>` 为何最先、`<mappers>` 为何最后?
源码路径:
- `SqlSessionFactoryBuilder.java:47-63` — build(inputStream): new XMLConfigBuilder → parse() → new DefaultSqlSessionFactory(configuration) → finally ErrorContext.reset
- `XMLConfigBuilder.java:105-112` — parse(): `parsed` 标志 → 已解析抛 BuilderException("Each XMLConfigBuilder can only be used once.")(测试 parseIsTwice); `newConfig(configClass)` 反射实例化(子类化支持 L95-103)
- `XMLConfigBuilder.java:114-135` — 定序: properties(L117)→settings 读入→`loadCustomVfsImpl/loadCustomLogImpl`(L119-120)→typeAliases→plugins→objectFactory 族→settingsElement(L126)→environments(L128)→databaseIdProvider→typeHandlers→mappers(L131)
- `XMLConfigBuilder.java:153-196` — vfsImpl 提前: 逗号分隔多类→`configuration.setVfsImpl`(其内部调 VFS.addImplClass, Configuration.java:248-252); typeAliases 的 **package 扫描**(registerAliases 走 VFS) — 故 VFS 必须先就绪
关键设计: 一次性装配(q4): parse 一次防脏状态; 定序即依赖链(q1): properties 最先(issue#117 供 ${} 替换)→vfsImpl/logImpl 立即生效(包扫描用 VFS)→typeAliases(plugins resolveClass 需别名)→objectFactory 族(environments 工厂 #631)→**mappers 最后**(消费全部就绪注册表)。[模式: 门面+时序装配]
数据流: build(inputStream) → parse() → parseConfiguration(evalNode("/configuration")) → 11 子元素依次 → mappers 触发 XMLMapperBuilder 递归(§5) → Configuration → DefaultSqlSessionFactory 持有。

### 2. properties 三级合并 + settings 反射白名单

场景: 内嵌/文件/代码传入三个来源, 谁优先级高?写错 setting 名为何报错?
源码路径:
- `XMLConfigBuilder.java:237-259` — propertiesElement: 内嵌子属性(L241) → resource/url 文件 putAll(L248-252, **互斥校验 L244-247**) → `configuration.variables`(构造 props) putAll(L253-256) → parser+configuration 双 setVariables
- `XMLConfigBuilder.java:137-151` — settingsAsProperties: `MetaClass.forClass(Configuration.class).hasSetter(name)` 动态校验 → 未知抛 "not known"(测试 unknownSettings)
- `XMLConfigBuilder.java:261-296` — settingsElement: 29 项逐项 `getProperty(name, 默认值)` → setter(如 autoMappingBehavior=PARTIAL/cacheEnabled=true/localCacheScope=SESSION, 测试 Minimal 全量断言)
关键设计: 优先级(q3): **构造 props > 文件 > XML 内嵌**(putAll 后者覆盖); 白名单用反射非硬编码(q2) — 新增 setting 自动跟随字段演化; 默认值双写(settings 默认与字段默认互为镜像)。[模式: 属性覆盖链+反射校验]
数据流: props(高) → 文件(中) → 内嵌(低) 合入 defaults → parser.setVariables 支持 ${} → configuration.setVariables; settings 经 hasSetter 校验后逐项落字段。

### 3. Hub 注册表 — 内置别名 + StrictMap + incomplete 延迟解析

场景: 同名 statement 重复注册为何抛?getMappedStatement 为何可能触发整批解析?循环 resultMap 怎么解?
源码路径:
- `Configuration.java:191-223` — 构造注册 22 内置别名: 事务工厂 2/数据源 3/缓存 5/DB_VENDOR/语言 2/日志 7/代理 2
- `Configuration.java:1104-1202` — **StrictMap**: put 重复 key 抛(L1148-1152, 异于 ConcurrentHashMap 覆盖); key 含 "." 自动注册短名(namespace 去尾段 L1153-1160); 短名冲突→Ambiguity 占位(L1158); get 不存在抛(L1176); get Ambiguity 抛歧义提示(L1179-1182)
- `Configuration.java:170-178,966-1055` — **incomplete 延迟解析**: 4 集合(statements/cacheRefs/resultMaps/methods)+4 锁; `buildAllStatements`(L966-971) 按 resultMaps→cacheRefs→statements→methods 触发; parsePending* 用 removeIf(成功移除, IncompleteElementException 保留); **parsePendingResultMaps do-while 多轮迭代**(L1033-1047)
- `Configuration.java:908-917` — getMappedStatement(id, validateIncompleteStatements=true) → 先 buildAllStatements 再取
关键设计: StrictMap 保证唯一性(q5) — **MP 注入器同名冲突亦受此约束(导航 MP-1)**; 延迟解析解循环引用(q6): 先入集合, 读取时统一触发, 失败保留等下次, resultMap 多轮迭代直到无进展。[模式: 严格注册表+延迟解析]
数据流: 语句解析失败 → addIncompleteStatement → 后续 getMappedStatement → buildAllStatements → parsePendingStatements removeIf 重试成功移除。

### 4. 处理器工厂与插件汇聚点 — 4 newXxx + newExecutor 装饰链

场景: 插件能拦哪 4 种对象?在哪拦?缓存包装在插件内还是外?
源码路径:
- `Configuration.java:703-742` — 4 工厂: newParameterHandler(L703-708, 委托 lang.createParameterHandler)/newResultSetHandler(L710-715, new DefaultResultSetHandler)/newStatementHandler(L717-722, new RoutingStatementHandler)/newExecutor(L724-742) — **全部 `interceptorChain.pluginAll` 收尾**(L707/714/721/741)
- `Configuration.java:728-742` — newExecutor: BATCH/REUSE/SIMPLE 三分支(L731-737) → `cacheEnabled` 时 `new CachingExecutor(executor)` 装饰(L738-739) → pluginAll(L741)
关键设计: 拦截点集中(q7): 4 处理器是执行链全部扩展面; 装饰顺序固定(q8): **缓存(CachingExecutor)包最内, 插件包最外** — 插件看到的 Executor 已被缓存装饰。[模式: 工厂方法+装饰链+拦截器链]
数据流: newExecutor(tx, type) → 类型分支 → cacheEnabled? CachingExecutor(inner) → pluginAll(outer) → DefaultSqlSession 持有。

### 5. MappedStatement 模型 — 单 SQL 全量元数据 + Mapper XML 装配

场景: 一个 `<select>` 标签变成什么?不开 useGeneratedKeys 主键回填还工作吗?同一 statement 能有数据库专用版吗?
源码路径:
- `MappedStatement.java:34-63` — 24 字段: resource/id/statementType/sqlSource/cache/parameterMap/resultMaps/flushCacheRequired/useCache/sqlCommandType/keyGenerator/databaseId/lang/resultSets/dirtySelect...
- `MappedStatement.java:65-86` — Builder 默认(q9): statementType=PREPARED/resultSetType=DEFAULT/parameterMap="defaultParameterMap"/**keyGenerator: useGeneratedKeys&&INSERT→Jdbc3KeyGenerator 否则 NoKeyGenerator**(L78-79)/lang=默认驱动(L85); logId 拼 logPrefix(L80-84); resultMaps() 聚合 hasNestedResultMaps(L102-108)
- `XMLMapperBuilder.java:96-147` — Mapper 装配(q10): parse 幂等(isResourceLoaded L97)→configurationElement 六段(cache-ref/cache/parameterMap/resultMap/sql/语句 L111-118)→bindMapperForNamespace→parsePending*(false); **databaseId 双路径**(有配置 databaseId 先解析, 再 null 通用版 L116-124); 语句解析抛 IncompleteElementException→`addIncompleteStatement`(L145-147)
- `XMLConfigBuilder.java:389-424` — mappers 四路(q11): package/resource/url/class, 互斥校验(L419-420); XML 委托 XMLMapperBuilder 递归
关键设计: Builder 默认值面 — 空配置不 NPE(自增回填默认关需 useGeneratedKeys); 幂等加载防重复注册; databaseId 双路径: 特定版(优先)+通用版(兜底); 失败语句延迟重试。[模式: Builder+幂等+双版本语句]
数据流: mappers → resource → new XMLMapperBuilder(inputStream, configuration, resource, sqlFragments) → parse → 六段 → addMappedStatement 或 addIncompleteStatement。

### 负面空间 — MyBatis 配置层刻意不做的事

- **不做热更新**: 装配一次性(parsed 标志+启动期完成)— 运行期改 XML/注册表无效需重启; 与 Spring 运行时刷新不同
- **不做递归包扫描**: `<mappers><package>` 仅列单层包(VFS.list 非递归, ResolverUtil.java:246-254), 子包需显式声明; 与 Spring 组件扫描递归不同; 亦不做 XML 语法强校验(仅查已知 setting/互斥, 未知元素静默)

→ 引出 M-2: Configuration.newExecutor 产出的 Executor 链(§4)是 `DefaultSqlSessionFactory.openSession` 的执行主线起点 — 下一篇拆 Executor/StatementHandler 如何消费 MappedStatement; M-4 插件的全部拦截点即 §4 的 4 工厂 → [[M-2-sqlsession-executor]] [[M-4-plugin]] [[M-7-cache]]
