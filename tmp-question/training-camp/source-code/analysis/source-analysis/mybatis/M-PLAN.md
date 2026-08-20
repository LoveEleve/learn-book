# MyBatis — 知识网络化规划 (M-1~M-7)

> **日期**: 2026-08-13 | **依据**: issue/源码分析执行计划.md 阶段3.4 (5域) + **09 对既有规划保持怀疑** 重审 (→ 7 域, 详见 §八)
> **源码**: `/data/workspace/source-code/code/spring/mybatis` (386 文件; org.apache.ibatis 22 顶层包)
> **定位**: 阶段3.4 — ORM 内核. 核心 = **Configuration 装配(Hub) + Executor 执行链 + 动态 SQL + 插件拦截 + 结果映射** — MyBatis 之所以是 MyBatis
> **执行顺序调整**: 原执行计划 3.3 MP 先于 3.4 M — 实测 **MP→M 单向依赖 (474 import / 反向 0)**, 反拓扑 → **本阶段提前至 MP 之前** (MP 阶段暂停, M 完成后回流)
> **知识网络**: 本文含 前置/复用/引出 双链; MP 阶段(3.3)后续正文对 MyBatis 的"导航指针"将改为真实引用

---

## 一、入口点与主线

`SqlSessionFactoryBuilder.build(InputStream)`(SqlSessionFactoryBuilder.java:47-63) → `XMLConfigBuilder.parse()`(XMLConfigBuilder.java:444 行, XML→Configuration) → `DefaultSqlSessionFactory.openSession`(L88-113: Environment→Transaction→`configuration.newExecutor(tx, execType)`→DefaultSqlSession) → 执行: `Executor.query/update`(Simple/Reuse/Batch + CachingExecutor 装饰) → `StatementHandler`(Routing 按类型路由) → `ParameterHandler`/`ResultSetHandler` → MappedStatement/BoundSql 贯穿. 用户侧: `MapperRegistry.getMapper` → `MapperProxyFactory`(JDK 动态代理) → `MapperMethod` → SqlSession. 旁路: scripting(动态 SQL)/cache(缓存)/type/reflection/plugin.

---

## 二、入口展开追踪 (00 §2) + 旁路扫描 (00 §2.5)

### 入口展开 (Level-1/2, 已读源码验证)

| 候选 | 源码位置 | 设计决策测试 | 结论 |
|---|---|---|---|
| `SqlSessionFactoryBuilder.build` | SqlSessionFactoryBuilder.java:47-63 | 门面: Reader/InputStream/Properties 多入口统一 → XMLConfigBuilder | **M-1 入口** |
| `XMLConfigBuilder.parse` | builder/xml/XMLConfigBuilder.java(444) | XML→Configuration 装配管线(settings/typeAliases/plugins/typeHandlers/mappers 分区解析) | **M-1** |
| `Configuration` | session/Configuration.java(1204) | **Hub**: 全部对象注册表 + 4 处理器工厂 `newExecutor/newStatementHandler/newParameterHandler/newResultSetHandler` 均过 `interceptorChain.pluginAll`(L707/714/721/741) | **M-1** |
| `MappedStatement` | mapping/MappedStatement.java | 单 SQL 的全量元数据(SqlSource/SqlCommandType/ResultMap/KeyGenerator/Cache) | **M-1** |
| `DefaultSqlSessionFactory.openSession` | session/defaults/L88-113 | Environment→TransactionFactory→newExecutor→DefaultSqlSession — 装配链 | **M-2** |
| `Executor` 族 | executor/(BaseExecutor 396/CachingExecutor 180/Simple/Reuse/Batch) | 执行核心: 一级缓存 localCache(BaseExecutor.java:58)/StatementHandler 路由 | **M-2** |
| `StatementHandler` 族 | executor/statement/(Routing/Prepared/Simple/Callable) | 按 MappedStatement 类型路由到 JDBC 操作 | **M-2** |
| `MapperRegistry.getMapper` | binding/MapperRegistry.java:67 | knownMappers→MapperProxyFactory→MapperProxy(InvocationHandler, MapperProxy.java:37)+Proxy.newProxyInstance(MapperProxyFactory.java:48)+methodCache | **M-3** |
| `InterceptorChain.pluginAll` | plugin/(InterceptorChain+Plugin 102) | 4 拦截点(Executor/StatementHandler/ParameterHandler/ResultSetHandler)+@Intercepts/Signature+Plugin.wrap JDK 代理 | **M-4** |
| `XMLScriptBuilder` | scripting/xmltags/XMLScriptBuilder.java(251) | **9 标签节点映射表**(trim/where/set/foreach/if/choose/when/otherwise/bind, L53-64)→SqlNode 树; parseScriptNode 动态/静态分流(DynamicSqlSource vs RawSqlSource) | **M-6** |
| `DynamicSqlSource` | scripting/xmltags/DynamicSqlSource.java | getBoundSql: DynamicContext.apply→SqlSourceBuilder.parse→BoundSql — 运行时求值 | **M-6** |
| `BaseExecutor.localCache` | executor/BaseExecutor.java:58,68 | 一级缓存: PerpetualCache+queryStack(L62)+createCacheKey(L136)+flush(L148) | **M-7** |
| `CachingExecutor` | executor/CachingExecutor.java:39,42,96 | 二级缓存: 装饰 Executor+TransactionalCacheManager(commit/rollback)+ms.getCache() | **M-7** |
| `DefaultResultSetHandler` | executor/resultset/DefaultResultSetHandler.java(1267) | 结果映射核心: 自动映射/嵌套结果/构造器/typeHandler | **M-5** |
| `TypeHandlerRegistry`/`Reflector`/`MetaObject` | type/(494)/reflection/(488/143) | 类型转换注册表+类元数据缓存+嵌套属性导航 — M-5 执行基础 | 并入 **M-5** |

### 旁路扫描

| 包 | 文件数 | 设计决策测试 | 结论 |
|---|---|---|---|
| `scripting/xmltags` | 18 | SqlNode 族 9 标签+OGNL — **定义特征级** | **M-6** (新增, 执行计划遗漏) |
| `cache` | 19 | Cache 装饰器 10 种(Blocking/Fifo/Lru/Scheduled/Serialized/Soft/Synchronized/Transactional/Weak/Logging)+CacheKey — 装饰器模式设计决策 | **M-7** (新增, 执行计划遗漏) |
| `builder` | 23 | XMLConfigBuilder/XMLMapperBuilder/MapperAnnotationBuilder(706) 装配管线 — 属 M-1 配置解析 | 并入 **M-1** |
| `type` | 56 | TypeHandlerRegistry 机制 + 大量具体类型实现 | 并入 **M-5** |
| `reflection` | 36 | Reflector/MetaObject — M-5 执行基础(面试低频) | 并入 **M-5** (密度不足可再议) |
| `annotations` | 30 | @Select/@Insert 等注解定义 — 载体, MapperAnnotationBuilder 消费 | 并入 **M-3** |
| `datasource`+`transaction` | 13+10 | 连接池/事务 — **已分析**(HikariCP H-13/Druid D-7, spring-tx) | **排除复用** (不重复成域) |
| `io`/`logging`/`parsing`/`util`/`exceptions`/`lang`/`cursor`/`jdbc` | 9+28+7+1+5+3+4+9 | 工具/基础设施 — 无框架级设计决策 | **排除** |

---

## 三、域清单 (7 域 / 6🔴 + 1🟡, 含信号置信度 + 方案预告)

### 核心域 (🔴 ×6)

| # | 域 | 包 | 核心主题 (设计决策) | 依赖 | 信号/置信度 | 方案 |
|:--:|---|---|---|---|:--:|:--:|
| M-1 | Configuration 核心+装配 | session/mapping/builder | **Hub**: XMLConfigBuilder 分区装配(settings/typeAliases/mappers)→Configuration(1204) 全对象注册表; MappedStatement 模型; **4 处理器工厂统一过 interceptorChain.pluginAll(L707-741)** | (叶子/Hub) | 面试高频/生产主流/Hub ✅ | 高 | A |
| M-2 | SqlSession/Executor 链 | session/executor | openSession→Transaction→newExecutor→DefaultSqlSession; **Executor 族 Simple/Reuse/Batch + CachingExecutor 装饰**; StatementHandler 族 Routing 路由; 一级缓存内嵌(导航 M-7) | M-1 | 面试高频/生产主流/Hub ✅ | 高 | A |
| M-3 | Mapper 代理 | binding | MapperRegistry→MapperProxyFactory→**MapperProxy(InvocationHandler)+methodCache 方法缓存**→MapperMethod(方法签名解析/参数映射); annotations 载体 | M-1,M-2 | 面试高频/生产主流/依赖弱 | 中 | A |
| M-4 | 插件机制 | plugin | **InterceptorChain.pluginAll 4 拦截点**(Executor/StatementHandler/ParameterHandler/ResultSetHandler L707-741)+@Intercepts/Signature 匹配+Plugin.wrap JDK 代理链 | M-2,M-5 | 面试高频/生产主流/MP-4 依赖 ✅ | 高 | A |
| M-5 | 参数/结果映射 | executor/resultset+parameter, type, reflection | DefaultResultSetHandler(1267) 自动/嵌套结果映射; TypeHandlerRegistry(494); Reflector(488)/MetaObject(143) 执行基础; DefaultParameterHandler(scripting/defaults/) | M-1,M-2 | 面试中频/生产主流/依赖弱 | 中 | A |
| M-6 | 动态 SQL | scripting | **XMLScriptBuilder 9 标签节点映射表(L53-64)→SqlNode 树**(If/ForEach/Where/Trim/Set/Choose); DynamicSqlSource vs RawSqlSource 分流; DynamicContext+OGNL 求值; LanguageDriverRegistry 扩展 | M-1 | 面试高频/生产主流/依赖弱 | 中 | A |

### 支撑域 (🟡 ×1)

| # | 域 | 包 | 核心主题 (设计决策) | 依赖 | 信号/置信度 | 方案 |
|:--:|---|---|---|---|:--:|:--:|
| 🟡 M-7 | 缓存体系 | cache | **一级**(BaseExecutor.localCache: PerpetualCache+queryStack 递归守卫+createCacheKey) **vs 二级**(CachingExecutor 装饰+ms.getCache+TransactionalCacheManager 事务性延迟提交); Cache 装饰器 10 种(装饰器模式); CacheKey 结构 | M-2 | 面试高频/生产主流/依赖弱 | 中 | B |

---

## 四、已排除 (00 §3 — 防"存在=域")

| 类/包 | 原因 |
|-------|------|
| datasource (13) + transaction (10) | 连接池/事务 — HikariCP H-13 / Druid D-7 / spring-tx 已分析, 复用导航 |
| annotations (30) | 注解定义载体 — MapperAnnotationBuilder(M-1 装配/M-3 代理) 消费, 并入 M-3 |
| io/logging/parsing/util/exceptions/lang/cursor/jdbc | 工具/基础设施 — 无框架级设计决策 (parsing 的 XPathParser 是 XML 工具, jdbc 的 SqlRunner 是工具类) |
| reflection 独立成域 | Reflector/MetaObject 有设计决策但面试低频、无独立消费方 — 并入 M-5 执行基础 (M-5 大纲密度不足时再议) |

---

## 五、知识网络图 (Obsidian 双链)

```
← 复用/内核来源:
   spring-jdbc (C-11, 已分析) ──→ M-1/M-2 (JDBC 内核/事务)
   HikariCP H-13 / Druid D-7 (已分析) ──→ M-1 (datasource/transaction 复用, 不重复)
   spring-tx (s29-33, 已分析) ──→ M-2 (事务边界: autoCommit/Transaction 对比)
   spring-aop (s24-28, 已分析) ──→ M-4 (拦截链对照: AOP 切面 vs pluginAll 代理)
   S-2 自动装配管线 (已分析) ──→ M-1 (XMLConfigBuilder 分区装配对照)

→ 引出/消费者:
   M-1~M-7 ──→ 阶段3.3 MP (回流): MP-1 注入(MappedStatement)、MP-4 插件(InterceptorChain)、
              MP-5 分页(BoundSql/Executor)、MP-2 元数据(MapperBuilderAssistant/Reflector)
   M-7 缓存 ──→ 阶段3.5 Redis (缓存机制对照: 本地缓存 vs 分布式缓存)

   📌 双链格式 (每篇大纲 header 写):
   前置: [[M-1-core-configuration]] ...
   复用: [[H-13-hikaricp]] [[D-7-druid]] ...
   引出: [[MP-1-sql-injector]] ...
```

> **⚠️ 阶段关系变更**: 原执行计划 3.3(MP) 先于 3.4(M) — 实测反拓扑 (MP→M 474 import, 反向 0), 现调整: **M 阶段提前, MP 暂停**。M 完成后 MP 阶段正文的 MyBatis 导航指针全部升级为引用。

---

## 六、执行顺序 (拓扑: 被依赖先)

**M-1 → M-2 → M-6 → M-7 → M-3 → M-5 → M-4**

> 依赖说明: M-1 Configuration 是 Hub(被全部依赖) → M-2 Executor 链依赖 M-1; M-6 动态 SQL 独立性强(依赖 M-1 即可, 放前); M-7 缓存依赖 M-2 的 Executor 结构(一级内嵌/二级装饰); M-3 代理依赖 M-1/2; M-5 参数/结果映射依赖 M-1(TypeHandlerRegistry)+M-2(StatementHandler); **M-4 插件最后** — pluginAll 拦截 4 种处理器, 需全部就位。每域走 v5 全管线 (KP→大纲→questions→六层深审→更新 HANDOFF)。

---

## 七、深度分类复核 (00 §3.5)

- **6🔴 / 1🟡** (86% 🔴 — 接近但未犯 80% 反模式, 因 MyBatis 内核 386 文件中定义特征机制占比极高)
- 🔴 = 定义性机制 (装配 Hub/执行链/动态 SQL/插件/结果映射/代理 — 没有它们 MyBatis 不再是 MyBatis)
- 🟡 = 支撑 (缓存 — 去掉缓存 MyBatis 仍是 MyBatis, 一级缓存默认开但属性能增强)
- **与执行计划差异**: ①M-5 由 🟡 升 🔴(结果映射是 ORM 定义特征, 无结果映射无法返回对象) ②新增 M-6/M-7(定义特征级包被遗漏) ③builder/type/reflection/annotations 并入策略

---

## 八、与原始执行计划 (issue/源码分析执行计划.md 阶段3.4) 的差异 — 怀疑审计表 (09 规范)

| 原始 (5 域) | 重审后 (7 域) | 验证动作 | 证据 | 理由 |
|:--|:--|:--|:--|:--|
| M-1~M-5 保留 | M-1~M-5 保留 | 入口展开+源码验证 | 4 处理器工厂 L707-741/Executor 族/Proxy.newProxyInstance L48 | 域划分方向正确 |
| — | **+M-6 动态 SQL** (🔴) | 顶层包扫描对照 | scripting/xmltags 18 文件: XMLScriptBuilder 9 标签映射表 L53-64/DynamicSqlSource 运行时求值 | 定义特征级包被遗漏 (<if>/<foreach> 是 MyBatis 招牌) |
| — | **+M-7 缓存** (🟡) | 同上 | cache 19 文件: BaseExecutor.localCache L58/CachingExecutor 装饰 L39/Cache 装饰器 10 种 | 一级/二级缓存面试高频, 装饰器设计决策 |
| M-5 标记 🟡 | M-5 升 🔴 | 定义特征测试重跑 | DefaultResultSetHandler(1267) 自动/嵌套映射 | 无结果映射无法 ORM — 定义特征 |
| M-1 "Configuration/Environment/TypeAliasRegistry/MappedStatement" | +builder 装配管线(23 文件) | 展开验证 | XMLConfigBuilder(444) 分区装配 | 装配是 M-1 应有之义 |
| M-5 未含 type/reflection | 并入 M-5 | 消费关系验证 | TypeHandlerRegistry(494)/Reflector(488) 是映射执行基础 | 避免碎片域 |
| 顺序 3.3 MP → 3.4 M | **M 提前, MP 暂停** | 正反双向 import 统计 | MP→M 474 处 / M→MP 零命中 | 单向依赖反拓扑, 教学序不得压拓扑 |

**覆盖率报告 (00 §第九步)**: 重审 7 域 vs 执行计划 5 域 = **140%**。差距解释: ①scripting/cache 为定义特征级包, 执行计划遗漏(方法 00 顶层包扫描+设计决策测试抓出) ②M-5 分类升级(定义特征测试重判)。执行计划无多余域 — 全部 5 域保留。

**注意**: MP-PLAN §八 曾报告"覆盖率 100%" — 该对照只比了**域数量**, 未做**包覆盖对照**, 正是 09 文档怀疑对象 #1 的教训。

---

## 九、完成检查单 (00 §8 + 09)

- [x] 入口点: SqlSessionFactoryBuilder.build() (已读方法体 L47-63)
- [x] 层级展开: Level-1/2 候选 15 项 + 旁路 9 项 (已读关键类)
- [x] 设计决策测试: 每候选含测试与结论
- [x] 依赖图: §三 每域依赖列 + §六 拓扑验证
- [x] 环形依赖: 无 (M-1 Hub 无入边环, 域间无环)
- [x] 排除清单: §四 5 项含原因
- [x] 对照验证: §八 覆盖率 140% + 3 处执行计划修正 (M-6/M-7 新增, M-5 升级)
- [x] Hub 检查: M-1/M-2 为 Hub (≤10 依赖, 无需 04 Hub 升级例外)
- [x] 怀疑审计: 09 规范四类对象全部验证 (域清单/数字/依赖方向/顺序)
