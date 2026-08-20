# M-1 Configuration 核心+装配 — XML→Configuration 管线与 Hub 注册表

> 项目: MyBatis | 🔴 Deep / 1 篇 | XMLConfigBuilder(444)+Configuration(1204)+MappedStatement(347)+BaseBuilder(134)+XMLMapperBuilder(402)+StrictMap
> 基线: M-PLAN M-1 (Hub 域) — 前置: **JDBC(spring-jdbc C-11) + 连接池(H-13/D-7)** — 展开 XML 装配管线→Hub 注册表→处理器工厂

---

## §0.8

- 🔴 Deep，1篇 — 入口(**SqlSessionFactoryBuilder.build L47-63 → XMLConfigBuilder.parse L105-112[parsed 一次性标志 L106-108]**) → 装配管线(**parseConfiguration L114-135: 11 元素分区定序[properties 最先 issue#117 L116→settings→vfsImpl/logImpl 提前 L119-120→typeAliases→plugins→objectFactory→objectWrapperFactory→reflectorFactory→settings→environments[objectFactory 后 issue#631 L127]→databaseIdProvider→typeHandlers→mappers 最后]**) → 配置机制(**properties 三级合并 L237-259[XML内嵌<文件<构造 props]; settingsAsProperties 反射白名单 L137-151[MetaClass.hasSetter]; vfsImpl 逗号分隔多类 L153-166**) → Hub 注册表(**Configuration 构造 22 内置别名 L191-223; StrictMap 语义 L1104-1202[put 冲突抛/短名自动注册/Ambiguity 歧义占位]; incomplete 延迟解析 4 集合 L170-178+parsePending* L973-1055**) → 处理器工厂(**4 newXxx 统一 pluginAll L703-742[Executor/StatementHandler/ParameterHandler/ResultSetHandler]; newExecutor 装饰链 L728-742[SIMPLE/REUSE/BATCH→CachingExecutor→pluginAll]**) → MappedStatement 模型(**Builder 默认值 L68-86[PREPARED/defaultParameterMap/keyGenerator 按 useGeneratedKeys&&INSERT]; 24 字段**) → Mapper XML 装配(**XMLMapperBuilder.parse L96-113[isResourceLoaded 幂等→configurationElement 六段→bindMapperForNamespace→parsePending* false]; databaseId 双路径 L116-124; IncompleteElementException→addIncomplete L145-147**)
- 设计模式: [模式: 门面+Builder+反射校验+注册表+延迟解析+装饰链]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| XMLConfigBuilder.java:105-112 | 一次性 | **parse(): parsed 标志防重复调用** — "Each XMLConfigBuilder can only be used once"(测试 parseIsTwice) | High |
| XMLConfigBuilder.java:114-135 | 分区定序 | **parseConfiguration 11 元素分区**: properties 最先(#117)→settings→vfsImpl/logImpl 提前→typeAliases→plugins→objectFactory→objectWrapperFactory→reflectorFactory→settings→environments(#631)→databaseIdProvider→typeHandlers→mappers | High |
| XMLConfigBuilder.java:137-151 | 白名单 | **settings 反射校验**: MetaClass.forClass(Configuration).hasSetter — 未知 setting 抛 BuilderException(测试 unknownSettings) | High |
| XMLConfigBuilder.java:237-259 | 合并 | **properties 三级合并**: XML 内嵌 < 文件(resource/url 互斥 L244-247) < 构造 props; parser+configuration 双 setVariables | High |
| Configuration.java:191-223 | 别名 | **构造注册 22 内置别名**: 事务工厂 2/数据源 3/缓存 5/DB_VENDOR/语言 2/日志 7/代理 2 | High |
| Configuration.java:1104-1202 | StrictMap | **put 重复 key 抛异常**(L1148-1152, 异于 ConcurrentHashMap 覆盖); 短名自动注册(L1153-1160); Ambiguity 占位(L1158); get 不存在抛(L1176-1178); get Ambiguity 抛(L1179-1182) | High |
| Configuration.java:170-178,966-1055 | 延迟解析 | **4 incomplete 集合+锁**(statements/cacheRefs/resultMaps/methods); buildAllStatements 触发链(L966-971); parsePending* removeIf 成功移除失败保留; parsePendingResultMaps do-while 多轮迭代(L1033-1047) | High |
| Configuration.java:703-742 | 工厂 | **4 处理器工厂统一 pluginAll**: ParameterHandler L707/ResultSetHandler L714/StatementHandler L721/Executor L741 — 插件拦截点全集中于此 | High |
| Configuration.java:728-742 | 装饰链 | **newExecutor**: BATCH/REUSE/SIMPLE 选择 → cacheEnabled 时 CachingExecutor 装饰(L738-739) → pluginAll(L741) | High |
| MappedStatement.java:65-86 | Builder | **Builder 默认值**: statementType=PREPARED/resultSetType=DEFAULT/parameterMap=defaultParameterMap/keyGenerator: useGeneratedKeys&&INSERT→Jdbc3KeyGenerator 否则 NoKeyGenerator(L78-79)/lang=默认语言驱动(L85) | High |
| XMLMapperBuilder.java:96-113,145-147 | Mapper | **parse 幂等**(isResourceLoaded L97); configurationElement 六段: cache-ref/cache/parameterMap/resultMap/sql/语句(L111-118); IncompleteElementException→addIncompleteStatement(L145-147) | High |
| XMLMapperBuilder.java:116-124 | 多库 | **databaseId 双路径**: 先 requiredDatabaseId 再 null — 语句可带 databaseId 特定版+通用版 | High |
| XMLConfigBuilder.java:389-424 | mappers | **mappersElement 四路**: package/resource/url/class, 互斥校验(L419-420); XML 委托 XMLMapperBuilder | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 配置装配是单管线机制 — 1篇 (~66行) 按"入口一次性→11 元素分区定序→properties/settings 机制→Hub 注册表(StrictMap/incomplete)→4 工厂汇聚点→MappedStatement 模型→Mapper XML 装配"展开; 插件汇聚点衔接 M-4(导航), 缓存衔接 M-7(导航), 注入器冲突语义衔接 MP-1(导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 11 元素分区定序 (properties 最先/vfsImpl 提前/mappers 最后) | 🔴 | **为什么🔴**: 装配时序依赖链 |
| P1-2 | properties 三级合并 + settings 反射白名单 | 🔴 | **为什么🔴**: 配置语义核心 |
| P1-3 | StrictMap 语义 (冲突抛/短名/Ambiguity) | 🔴 | **为什么🔴**: 全部注册表行为 |
| P1-4 | incomplete 延迟解析 (4 集合+触发链) | 🔴 | **为什么🔴**: 循环引用解法 |
| P1-5 | 4 处理器工厂 pluginAll 汇聚点 | 🔴 | **为什么🔴**: M-4 插件机制地基 |
| P1-6 | newExecutor 装饰链 + MappedStatement Builder 默认值 | 🔴 | **为什么🔴**: 执行链路起点 |
| P2-1 | Mapper XML 六段装配 + databaseId 双路径 | 🟡 | **为什么🟡**: 多库支持 |
| P2-2 | 22 内置别名表 | 🟡 | **为什么🟡**: 记忆型知识 |
| P3-1 | 与 MapperRegistry/TypeHandlerRegistry 协作 (导航 M-3/M-5) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **装配管线与配置机制** | 🔴 | 骨架+时序 |
| B | **Hub 注册表 (StrictMap+incomplete)** | 🔴 | 全局语义 |
| C | **处理器工厂+模型** | 🔴 | 执行起点 |
| D | **Mapper 装配与内置知识** | 🟡 | 多库/记忆 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 11 元素分区定序 | 顺序即依赖链: properties 最先(#117 供后续 ${} 替换) → settings 读入 → **vfsImpl/logImpl 立即加载**(typeAliases 的 package 扫描需 VFS, L180) → typeAliases(plugins 的 resolveClass 需别名) → objectFactory 族(environments 需工厂, #631) → mappers 最后消费全部就绪 | XMLConfigBuilder.java:114-135,153-171,180 |
| q2 | settings 反射白名单 | 用 MetaClass.forClass(Configuration.class).hasSetter 动态校验而非硬编码 — 新增 setting 自动跟随字段演化; 未知设置抛 "not known"(测试 unknownSettings); vfsImpl 特殊处理因支持逗号多类+需先于注册表 | XMLConfigBuilder.java:137-151; 测试 XmlConfigBuilderTest.unknownSettings |
| q3 | properties 三级合并 | 优先级: 构造 props > resource/url 文件 > XML 内嵌 (putAll 后者覆盖前者); resource/url 互斥校验; 测试 PropertiesUrl 文件无 prop1 故内嵌 bbbb 保留 | XMLConfigBuilder.java:237-259; jdbc.properties 无 prop1 |
| q4 | parse 一次性 | parsed 标志防重复调用 — Builder 线程不安全, 解析是单次装配操作(测试 parseIsTwice) | XMLConfigBuilder.java:105-112 |
| q5 | StrictMap 语义 | ①put 重复 key 抛异常(注册表唯一性 — **MP 注入器同名冲突亦受此约束, 导航 MP-1**) ②key 含 "." 自动注册短名(namespace 去尾段) ③短名冲突→Ambiguity 占位 ④get 不存在抛 ⑤get Ambiguity 抛歧义提示 | Configuration.java:1104-1202 |
| q6 | incomplete 延迟解析 | 循环引用(嵌套 resultMap/cache-ref/语句依赖)先入 incomplete 集合, buildAllStatements 按 resultMaps→cacheRefs→statements→methods 顺序触发; removeIf 两种模式: **statements 靠异常中断保留**(parse 抛 IncompleteElementException), **cacheRefs 靠返回值条件移除**(resolveCacheRef()!=null); **parsePendingResultMaps do-while 多轮迭代**直到一轮无进展 — 解决级联嵌套 | Configuration.java:170-178,966-1055 |
| q7 | 4 工厂汇聚点 | 4 newXxx 统一过 interceptorChain.pluginAll — 插件只需实现 Interceptor+@Intercepts 即可拦 4 处理器, 拦截点设计集中(导航 M-4) | Configuration.java:703-742 |
| q8 | newExecutor 装饰链 | SIMPLE/REUSE/BATCH → cacheEnabled 时 CachingExecutor 装饰 → pluginAll — 装饰器+插件两层的顺序固定: 缓存包在最内, 插件包在最外 | Configuration.java:728-742 |
| q9 | MappedStatement 默认值 | Builder 构造: PREPARED/defaultParameterMap/Jdbc3KeyGenerator(仅 useGeneratedKeys&&INSERT) 否则 NoKeyGenerator/lang=默认驱动 — 未配置时的行为面 | MappedStatement.java:65-86 |
| q10 | Mapper XML 装配 | parse 幂等(isResourceLoaded); 六段: cache-ref→cache→parameterMap→resultMap→sql 片段→select|insert|update|delete; 语句解析失败 IncompleteElementException → addIncompleteStatement 等下次; databaseId 双路径(特定版+通用版) | XMLMapperBuilder.java:96-124,145-147 |
| q11 | mappers 四路 | package(包扫描)/resource/url/class 四路注册, 互斥校验("may only specify a url, resource or class"); XML 委托 XMLMapperBuilder 递归 | XMLConfigBuilder.java:389-424 |

→ 引出 M-2: SqlSession/Executor — Configuration.newExecutor 产出的 Executor 链是执行主线的起点; MP-1 注入器在 Configuration.addMappedStatement 上注册(StrictMap 冲突语义生效)。
