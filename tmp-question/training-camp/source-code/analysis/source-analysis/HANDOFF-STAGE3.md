# 阶段3 数据与存储 — 总交接文档 (STAGE3 v2 超详细版)

> **日期**: 2026-08-13 | 阶段3.2~3.4 执行中
> **给新 AI**: 本文是阶段3 的**唯一总入口**, 独立承载全部必要信息 (15 域交付详情内联)。分域细节如需回溯原始 REVIEW 上下文见 `mybatis/HANDOFF-M.md` + `mybatis-plus/HANDOFF-MP.md` (本文为超集)。
> **阅读顺序**: §零~§六 → §七 (MP-8 详案) → 开工。

---

## §零 状态速查

### 阶段3 全景

| 仓库 | 域数 | 状态 | 规划 | 分域 HANDOFF |
|:--|:--:|:--|:--|:--|
| HikariCP (3.1) | 13 | ✅ 100% (历史) | HIKARICP-PLAN.md | HANDOFF-HIKARICP-v2.md |
| Druid (3.2) | 9 | ✅ 100% (历史) | DRUID-PLAN.md | HANDOFF-DRUID-v2.md |
| **MyBatis (3.4 提前)** | **7** | ✅ **100%** | M-PLAN.md (7 域) | HANDOFF-M.md |
| **MyBatis-Plus (3.3 回流)** | **9** | ✅ **9/9** | MP-PLAN.md (9 域) | HANDOFF-MP.md |
| Redis (3.5) | **33** (09 重审 18→33, v2 补 zmalloc/functions) | ✅ **33/33 收官** (R-33+R-4+R-3+R-19+R-7+R-6+R-5+R-1+R-20+R-21+R-22+R-23+R-2+R-28+R-24+R-25+R-26+R-27+R-11+R-12+R-13+R-29+R-16+R-17+R-18+R-30+R-32+R-31+R-8+R-9+R-10+R-14+R-15 ✅) | REDIS-PLAN.md (含 §〇 怀疑审计表) | HANDOFF-REDIS-V4.md (超详细全量交接, 33 域固化, **2026-08-14 收官**) |
| **Redisson (3.6)** | **9** (1 导论 RD-0 + 5🔴+3🟡, 09 重审 7→8 +1 导论) | ✅ **9/9 完成** (RD-0~RD-8: 23 篇大纲 + 5 harness 65/65 + 400+ 问) | REDISSON-PLAN.md (v2.1 含三次 REVIEW) | HANDOFF-REDISSON.md (v3 超详细版, 350 行) |
| ES (3.7) | **12** (09 重审 11→12, +1 Aggregations) | ✅ **12/12 收官** (E-3/E-7/E-1/E-6/E-5/E-11/E-2/E-12/E-4/E-8/E-9/E-10 ✅) — 32 篇大纲 + 96 闭环 + 8 harness 127/127 | ES-PLAN.md (v1 含 §〇 审计表) | HANDOFF-ES.md (v2 超详细版 12 域, **2026-08-14 收官**) |

### ⚠️ 顺序变更历史 (必读)

原执行计划: 3.3 MP → 3.4 MyBatis。**2026-08-13 09 重审实测 MP→MyBatis 单向依赖** (MP 源码 import org.apache.ibatis **474 处**; MyBatis 反向引用 com.baomidou **零命中**) → 反拓扑 → **MyBatis 提前执行, MP 暂停后回流**。执行计划 L15/3.3/3.4 已加注记。

### 执行序现状

```
M: M-1 → M-2 → M-6 → M-7 → M-3 → M-5 → M-4  (7/7 ✅, 拓扑: Hub 先/插件最后)
MP: MP-2 → MP-1 → MP-9 → MP-3 → MP-4 → MP-5 → MP-6 → MP-7 → MP-8  (9/9 ✅ 收官)
```

---

## §一 方法论与铁律 (权威: talk-method/.../methodology/zh/ 01-09)

### 核心管线 (每域必走)

```
Pass 0 读上下文(README/测试地图) → Pass 1 扫轮廓(≥5 真问题/读 2 测试)
→ Pass 2 闭环(假设→grep 验证→结论, 内化 KP §05) → Pass 3 大纲(四要素)
→ 六层深审(必须真找问题, 零发现=不合格) → 方案 A 强制: 时空溯源+harness
→ 全量回归 → 更新 HANDOFF → 用户确认后再下一个域
```

### ⚠️ 2026-08-13 重大修正 — 禁止行号限制 (用户明确指令)

- 行号限制 (🔴 39-69 / 🟡 35-49) **废弃为软参考** — 内容完整 > 行数
- 09 文档新增反模式 **#7 机械执行历史格式规范牺牲内容** (M-7 被删空教训) + **#8 用"合规范"替代"讲清楚"** (REVIEW 标准 = 读者能否靠它写出文章)

### 09 对既有规划保持怀疑 (每阶段/每域开工前)

四类怀疑对象 + 验证方法:
1. **域清单完整性** — 顶层包扫描, ≥10 文件未覆盖包过设计决策测试 (MyBatis 漏 scripting/cache 教训)
2. **数字** — 穷举 (ls/grep; BaseMapper 17→19/VERSION 7→8/别名 22/方言 29 教训)
3. **依赖方向** — 正反双向 import 统计 (MP→M 474 实证)
4. **顺序** — 拓扑重跑 (教学序不得压拓扑)

**基线数字同样不可信** — MP-PLAN "17" 错, 每个数字独立穷举。

### 方案选择 (04)

🔴 → A (Pass 0-3 + 时空溯源 + 极简复现 harness) / 🟡 有设计决策 → B (Pass 0-2, Pass 3 可选) / 叶子 🟡 最低 B。

---

## §二 执行计划修正清单 (6 项, 全部落文档)

| 修正 | 原文 | 实测 | 落点 |
|:--|:--|:--|:--|
| 顺序 | 3.3 MP → 3.4 M | MP→M 单向依赖反拓扑 → M 提前 | 执行计划 L15/3.3/3.4 注记 |
| M 域数 | 5 | **7** (+M-6 动态 SQL +M-7 缓存) | 执行计划 3.4 + M-PLAN §八 |
| M-5 级别 | 🟡 | 🔴 (结果映射是 ORM 定义特征) | 执行计划 3.4 |
| MP 方法类 | 17 种内置 | 18 方法类/12 默认注入 | 执行计划 3.3 注记 |
| MP 回调 | 5 个 | 7 (InnerInterceptor) | 执行计划 3.3 注记 |
| BaseMapper 方法 | 17 CRUD | **19 抽象方法** (实测) | MP-PLAN + HANDOFF-MP |

---

## §三 MyBatis 7 域全量交付 (M-PLAN 拓扑序)

### M-1 Configuration 核心+装配 (🔴 66 行/18 锚/11 闭环/21 问/harness 10/10)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| 入口一次性: SqlSessionFactoryBuilder.build → parse(parsed 标志) | SqlSessionFactoryBuilder.java:47-63; XMLConfigBuilder.java:95-112 |
| **11 元素分区定序**: properties(#117)→settings→vfsImpl/logImpl 提前→typeAliases→plugins→objectFactory 族→settings→environments(#631)→databaseIdProvider→typeHandlers→mappers (14 调用点, settings 拆三段) | XMLConfigBuilder.java:114-135,153-196 |
| properties 三级合并: 构造 props>文件>XML 内嵌; resource/url 互斥 | XMLConfigBuilder.java:237-259 |
| settings 反射白名单: MetaClass.hasSetter(Configuration) | XMLConfigBuilder.java:137-151,261-296 |
| **22 内置别名** (事务 2/数据源 3/缓存 5/DB_VENDOR/语言 2/日志 7/代理 2) | Configuration.java:191-223 |
| **StrictMap**: put 冲突抛/短名自动注册/Ambiguity 占位/get 缺失抛 | Configuration.java:1104-1202 |
| **incomplete 延迟解析**: 4 集合+4 锁+buildAllStatements 触发链+resultMaps do-while 多轮 | Configuration.java:170-178,966-1055 |
| **4 处理器工厂统一 pluginAll** (M-4 拦截点) | Configuration.java:703-742 |
| newExecutor 装饰链: SIMPLE/REUSE/BATCH→CachingExecutor(内)→pluginAll(外) | Configuration.java:728-742 |
| MappedStatement **24 字段** + Builder 默认 (PREPARED/keyGenerator 条件) | MappedStatement.java:34-86 |
| Mapper XML 六段装配 + databaseId 双路径 + 失败延迟 | XMLMapperBuilder.java:96-147 |

**时空溯源**: 2010 初始版 (StrictMap/Ambiguity 已定型) → 2010 bug179 短名 → 2022 并发化 (HashMap→ConcurrentHashMap, d64edc518) → 2023 synchronized→ReentrantLock → 2024 parsePending 重构。

**REVIEW 历史 (9 处)**: 密度 85→66 / header "10 KP"→8 / excludeProperty 语义 / "8 种标记"编造 / KP 猜主键表述 / settings "40+"→29 / vfsImpl 归属 (addImplClass 在 setVfsImpl 内) / parsePending 双模式 (statements 异常中断 vs cacheRefs 返回值) / 负面空间 0→3 条 (不热更新/不递归包扫描 ResolverUtil/不强 XML 校验)。

**负面空间**: 不热更新 / 不递归包扫描 / 不强 XML 校验。

### M-2 SqlSession/Executor 链 (🔴 69 行/15 锚/11 闭环/21 问/harness 8/8)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| 门面: selectOne 三态/dirty/commit 判定/close 回滚 (L263) | DefaultSqlSession.java:73-85,150-203,216-247,261-266 |
| **wrapCollection→wrapToMapIfCollection**: Collection→{"collection","list"}/数组→{"array"} (M-6 foreach 取值依据) | DefaultSqlSession.java:150-203; ParamNameResolver.java:158-177 |
| 模板方法: query/update + 4 doXxx 抽象 + **一级缓存 5 清点** (update/commit/rollback/STATEMENT scope/flushCacheRequired) | BaseExecutor.java:110-118,132-175,242-266,276-284 |
| **CacheKey 六段构成** (参数值参与, 值语义 equals/hashCode) | BaseExecutor.java:198-235 |
| queryStack+EXECUTION_PLACEHOLDER 防递归 + deferredLoads | BaseExecutor.java:148-173,331-355 |
| 子类策略 + wrapper 语义 (CachingExecutor 构造 setExecutorWrapper) | SimpleExecutor.java:57-93; CachingExecutor.java:46; BaseExecutor.java:347-357 |
| 路由三路 + Prepared 三分支 + 主键回填时机 (processAfter) | RoutingStatementHandler.java:41-56; PreparedStatementHandler.java:48-57,80-95 |
| 二级缓存查询流程 (tcm 延迟提交) | CachingExecutor.java:96-140 |

**时空溯源**: 2010 初始版 (模板+PLACEHOLDER+queryStack 已定型) → 2012 clearLocalCacheAfterEachStatement→**localCacheScope=STATEMENT** (b32ac15d1) → 2014 选择性延迟加载 → 2011 deferredLoad 优化。

**REVIEW 历史 (6 处)**: 密度 78→69 / close L265→L263 / **CacheKey 值语义 harness 实证** (3 FAIL) / "final 模板"编造 (无 final, CachingExecutor 覆写 12 方法) / 线程安全缺口 (零 synchronized 实证) / queryFromDatabase L331 精确化 + wrapCollection 补充。

**负面空间**: 不自动重试/不管理连接池/不改写 SQL/不做线程安全。

### M-6 动态 SQL (🔴 69 行/14 锚/9 闭环/21 问/harness 8/8)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| 入口双路径: XNode/`<script>`/String + ${} 变量替换 (issue#3/#127) | XMLLanguageDriver.java:30-48 |
| **9 标签 Handler 注册表** (trim/where/set/foreach/if/choose/when→IfHandler/otherwise/bind) | XMLScriptBuilder.java:53-63 |
| parseDynamicTags 递归 + **isDynamic 成员变量传播** + 未知标签抛 | XMLScriptBuilder.java:76-101 |
| 动态/静态分流: DynamicSqlSource vs RawSqlSource | XMLScriptBuilder.java:65-74 |
| **__frch_ 参数唯一化** (ITEM_PREFIX L28) + FilteredDynamicContext (appendSql 时替换) | ForEachSqlNode.java:28,69-137 |
| **Trim 家族**: Where=Trim("WHERE",[AND/OR])/Set=Trim("SET",[,],null,[,]) | WhereSqlNode.java:25-32; SetSqlNode.java:25-30; TrimSqlNode.java:56-60,89-99 |
| **ContextMap 四层回退** (_parameter/_databaseId) | DynamicContext.java:32-54,78-94 |
| ${} OGNL+injectionFilter(仅编程式构造可传)+null→"" vs #{} ParameterMapping+? | TextSqlNode.java:63-83; SqlSourceBuilder.java:42-52 |
| 运行时: getBoundSql apply→parse→setAdditionalParameter; Raw 构建期一次 | DynamicSqlSource.java:30-43; RawSqlSource.java:28-49 |
| **findProperty 去下划线+忽略大小写** (MetaClass.java:56-64, 非标准驼峰) | MetaClass.java:56-64 |
| OGNL 表达式编译缓存 + OgnlClassResolver | OgnlCache.java:44-58 |

**时空溯源**: 2012-04 **LanguageDriver 可插拔扩展点** (XMLScriptBuilder/OgnlCache/__frch_ 同期) → 2014 微调。

**REVIEW 历史 (9 处)**: 密度 78→69 / FilteredDynamicContext 必须 appendSql 时替换 (harness) / Trim 必须先 contents.apply (harness) / 单条件 Where 断言写反 / **Set 特化不完整** (suffixOverrides=[,]) / foreach first 语义 (首元素 ""非 separator) / **injectionFilter 仅编程式可传, XML 路径恒 null** / DynamicContext L65-73 / String 路径不经 parseScriptNode。

**负面空间**: 不做方言拼装 (归 MP-5) / ${} 不默认防护 / 不缓存 BoundSql。

### M-7 缓存体系 (🟡 76 行/22 锚/8 闭环/21 问/方案 B)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| Cache 接口 7 方法 + getReadWriteLock default null (3.2.6 起核心不调用) | Cache.java:60-97 |
| PerpetualCache 裸 HashMap 非同步 + equals 按 id | PerpetualCache.java:24-35 |
| **CacheBuilder 组装**: 默认 Perpetual+Lru + 标准链 Scheduled→Serialized→Logging→Synchronized→Blocking | CacheBuilder.java:92-127 |
| 构造契约: 基础 (String id)/装饰器 (Cache) + 属性反射注入 | CacheBuilder.java:137-177,199-218 |
| **readWrite=!readOnly 默认 true → SerializedCache 默认应用** | XMLMapperBuilder.java:169-172 |
| **CacheKey**: 37 乘子/17 初始/checksum/count + equals 快速失败 + NULL 键单例 | CacheKey.java:33-95 |
| **LruCache 双 map 同步** (accessOrder 触摸序+eldestKey 同步删, getObject 也 touch) | LruCache.java:31-93 |
| **TransactionalCache 三暂存** + getObject 三态 (含 clearOnCommit issue#146) + commit flush | TransactionalCache.java:43-95 |
| **releaseLock 时序: commit 的 flushPendingEntries 阶段** (非 putObject 时) | BlockingCache.java:59-63; TransactionalCache.java:85-95 |
| BlockingCache CountDownLatch 击穿防护 | BlockingCache.java:41-72 |
| TransactionalCacheManager per-Cache 包装 + 全量 commit/rollback | TransactionalCacheManager.java:44-57 |

**时空溯源**: 装饰器族自远古存在; 无重大演变 (本地二级缓存稳定)。

**REVIEW 历史 (5 处)**: 密度 78→69 (被行号禁令推翻, 恢复 76 内容完整版) / **clearOnCommit 分支遗漏** (issue#146) / removeObject javadoc L43-60→L60-76 / **releaseLock 时序** (commit flush 阶段, 非 putObject 时) / questions 文件从未创建 (交接 REVIEW 补齐)。

**负面空间**: 不做分布式 (对照 R-1) / 不做多级淘汰联动 / 不做缓存预热 / 不做写穿透。

### M-3 Mapper 代理 (🔴 84 行/20 锚/9 闭环/21 问/harness 15/15)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| 注册时序: **先 put 后 parse** + 失败回滚 (loadCompleted) | MapperRegistry.java:60-80 |
| 工厂: **methodCache 跨会话共享** + 每会话新代理 | MapperProxyFactory.java:35-56 |
| invoke 分派: **Object 方法直通** / cachedInvoker 缓存 | MapperProxy.java:81-112 |
| **default 方法 MethodHandle**: JDK9 privateLookupIn / JDK8 Lookup 反射 | MapperProxy.java:54-78,114-126 |
| execute 六分支 (INSERT/UPDATE/DELETE→rowCountResult; SELECT 五分支) + primitive null 检查 | MapperMethod.java:57-121 |
| SqlCommand: statementId=接口名.方法名 + "Invalid bound statement" + **父接口递归** | MapperMethod.java:222-268 |
| MethodSignature: TypeParameterResolver 泛型返回 + 特殊参唯一索引 | MapperMethod.java:284-302,355-368 |
| **ParamNameResolver 三态** + param1..N 通用名 + **ParamMap 严格 get** | ParamNameResolver.java:40-146; MapperMethod.java:203-215 |
| 注解装配: 同名 XML 优先 ("namespace:" 标志) + 延迟解析 | MapperAnnotationBuilder.java:101-127 |

**时空溯源**: 2010 初始版 → 2016 **参数命名规则重构** (d5fd43399, ParamNameResolver+param1..N) → 2019 **JDK9 default 方法兼容** (961b002b7)。

**REVIEW 历史 (6 处)**: harness 迭代 (try-catch/Exception) / **canHaveStatement 编造** (实为 !isBridge && !isDefault, L140-143 issue#237) / parse L101-127→**L114** / loadXmlResource "namespace:" 标志 / ParamNameUtil 实证 / 负面空间 4 条。

**负面空间**: 不做 AOP 增强 / 单接口代理 / 懒绑定。

### M-5 参数/结果映射 (🔴 71 行/14 锚/11 闭环/21 问/harness 12/12)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| **取参四路**: additional(issue#448)>null>裸值 hasTypeHandler>metaObject.getValue | DefaultParameterHandler.java:62-99 (L72-82) |
| **null 绑定兜底**: value null 且 jdbcType null→getJdbcTypeForNull (L87) | DefaultParameterHandler.java:84-93 |
| 多结果集双循环 + collapse + Cursor 单 map 限制 | DefaultResultSetHandler.java:188-242 |
| 行遍历分流 + 安全守卫 (ensureNoRowBounds/checkResultHandler) | DefaultResultSetHandler.java:330-356 |
| skipRows (FORWARD_ONLY 逐行/absolute) + discriminator | DefaultResultSetHandler.java:359-376,387-402,971 |
| **createResultObject 四分支** (createPrimitiveResultObject/构造器/默认构造/自动构造器) + 延迟代理 | DefaultResultSetHandler.java:654-700 |
| 构造器自动映射四选一 (单/@AutomapConstructor/argNameBased/类型匹配) | DefaultResultSetHandler.java:729-757 |
| **三级自动判定**: 显式>嵌套 FULL>简单非 NONE | DefaultResultSetHandler.java:466-478 |
| 显式映射: column 存在检查+三路取值+callSettersOnNulls (issue#377) | DefaultResultSetHandler.java:481-515 |
| 自动映射: findProperty+缓存 (resultMapId:columnPrefix) + UnknownColumnBehavior 三态 | DefaultResultSetHandler.java:580-645 |
| **嵌套聚合**: combinedKey partialObject+putAncestor 循环保护 | DefaultResultSetHandler.java:440-462 |
| ResultSetWrapper: useColumnLabel+mapped/unmapped 缓存 | ResultSetWrapper.java:46-196 |

**时空溯源**: 2015 **自动映射缓存** (340802b47) → 2017 AutomapConstructor 重命名 → 2021 argNameBased → 2022 构造器列排除修复。

**REVIEW 历史 (6 处)**: harness camel 简化 (真实机制是 findProperty replace("_","")+忽略大小写, 非标准驼峰) / **"三路"vs M-2 四路** (跨域引用没回验, 数字错误) / DefaultParameterHandler 行号 (L34-62→L62-99) / createPrimitiveResultObject 精确化 (L849) / ensureNoRowBounds·checkResultHandler 条件逐字对照 / 负面空间 4 条。

**负面空间**: 不做 N+1 优化 (lazy 仅延迟) / 不自动刷新 / 不跨库方言。

### M-4 插件机制 (🔴 62 行/12 锚/7 闭环/20 问/harness 6/6)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| **Invocation 4 类白名单** (2024-03 引入, 安全加固) | Invocation.java:30-36 |
| pluginAll **洋葱嵌套** (后注册包外层, 最外层 after 最后) | InterceptorChain.java:25-44 (pluginAll L29-34) |
| Plugin.wrap 三步骤 + **无匹配不包装** | Plugin.java:44-52,67-100 |
| invoke 双条件分派 + 透传 + unwrapThrowable | Plugin.java:55-65 |
| Interceptor 三方法契约 (L23-35) + Signature 精确匹配 | Interceptor.java:23-35; Signature.java:25-54 |
| 4 工厂汇聚点 (M-1 交付) | Configuration.java:703-742 |
| 多租户 schema 用例 + 非法目标实证 | PluginTest.java:75-100 |

**时空溯源**: 2010 初始版 → 2013 "Lock down collections" (unmodifiable) → **2024-03 白名单** (319da5811 "Prevent Invocation from invoking arbitrary method")。

**REVIEW 历史 (6 处)**: harness 迭代 (getMethod 参数/洋葱断言写反) / **行号超范围** (Interceptor 文件 35 行, 引用 L25-41) / InterceptorChain L32-41→L25-44 / Intercepts @Target(TYPE) / 时空溯源 2024-03 很新 / 负面空间 3 条。

**负面空间**: 不做任意对象拦截 (4 类白名单) / 不自动排序 (注册序) / 不执行期重载。

---

## §四 MyBatis-Plus 7 域全量交付 (MP 拓扑序)

### MP-2 表元数据解析 (🔴 66 行/20 锚/10 闭环/21 问)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| 入口双缓存 + **Configuration 重初始化** (多数据源, TABLE_NAME 缓存只 put 不清理边界) | TableInfoHelper.java:163-175,203-204 |
| 读取路径: primitive/SimpleType/interface 过滤 + getUserClass + 父类缓存移动 | TableInfoHelper.java:97-118 |
| **表名五步管线** + keepGlobalPrefix 豁免 (测试 testTableNamePrefix/2) | TableInfoHelper.java:221-302; MybatisConfiguration.java:92 |
| **主键三路径**: @TableId (多个抛)→属性名 id 猜→warn+havePK=false; type 注解>全局 ASSIGN_ID | TableInfoHelper.java:321-347,472-517,529-558; GlobalConfig.java:108 |
| **组合注解 SPI** (递归穿透元注解 L65+HashSet 防环 L61+父类上溯) | AnnotationHandler.java:38,62; AnnotationUtils.java:33-49,61-72 |
| 字段发现: 继承合并 (子类同名优先, 去 static/transient) | ReflectionKit.java:127-155 |
| 列名推导 + **checkRelated AS 兜底** (resultMap 互斥) | TableFieldInfo.java:265-300,354-380; TableInfoHelper.java:570-582 |
| **FieldStrategy convertIf** (NEVER→null/直拼/NOT_EMPTY/非空) + fill 豁免 + 注解>全局 | TableFieldInfo.java:596-608,302-304,318-320; GlobalConfig.java:194,200,217 |
| 逻辑删除双路径: @TableLogic 注解值优先, 全局 logicDeleteField 仅类中无注解兜底 | TableFieldInfo.java:406-430; GlobalConfig.java:184-188 |
| fail-fast: 多 @TableId 抛/多 @TableLogic·@Version Assert | TableInfoHelper.java:338-340; TableInfo.java:493-518 |
| **resultMap 三态**: 显式/autoResultMap 自动构建 (ResultFlag.ID+IJsonTypeHandler 每实例化)/AS 兜底 | TableInfoHelper.java:246-250,475-491; TableFieldInfo.java:556-576 |
| Lambda 列缓存联动 (formatKey 全大写) | TableInfoHelper.java:207; LambdaUtils.java:77-110 |

**REVIEW 历史 (10 处)**: 密度 145→66 / header KP 数 / excludeProperty 语义 / "8 种标记" / 猜主键表述 / **L70→L65** (AnnotationUtils 递归) / "注读取"错别字 / **formatKey 全大写** / FieldStrategy 5→6 (漏 DEFAULT) / PostInitTableInfoHandler L37/47/57。

### MP-1 SQL 自动注入 (🔴 62 行/9 锚/8 闭环/20 问/harness 8/8)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| **12 默认方法面**: 7 无条件+5 havePK (xxById) + 无主键 warn | DefaultSqlInjector.java:39-60 |
| **注入时机**: MybatisMapperAnnotationBuilder.parse 注解解析后 + **isSupperMapperChildren** (superMapperClass 默认 Mapper.class) + **InjectorResolver 延迟重试** | MybatisMapperAnnotationBuilder.java:114-126; GlobalConfigUtils.java:122-124; InjectorResolver.java:26-38 |
| inspectInject 装配循环 + mapperRegistryCache 防重复 | AbstractSqlInjector.java:44-65 |
| **用户自定义优先**: hasMappedStatement(statementName,false) 已存在→warn 跳过 | AbstractMethod.java:96-98,252-273 |
| 注册参数: PREPARED/flushCache=!isSelect/useCache=isSelect | AbstractMethod.java:261-267 |
| 列片段: 自定义 resultMap→*/否则 getAllSqlSelect | AbstractMethod.java:162-173 |
| 方法类模板: SelectById **5 占位** String.format; Insert keyGenerator 三分支 (外层 keyProperty 非空) | methods/SelectById.java; methods/Insert.java |
| SqlMethod 枚举 25 值 (method+描述+SQL 模板 %s) | core/enums/SqlMethod.java |

**时空溯源**: 2018-04 注入器重构初始版 → 3.5.4 insertIgnoreAutoIncrementColumn 等迭代。

**REVIEW 历史 (6 处)**: harness 迭代 (构造器遮蔽/5 占位) / **注入时机不精确** (覆写装配+isSupperMapperChildren+InjectorResolver 三层机制) / Insert 外层条件 / hasMappedStatement false 语义 / 负面空间 4 条。

**负面空间**: 不做 SQL 校验 (首执行才暴露) / 不运行时重注入 / 不做方法删除。

### MP-9 BaseMapper+IService (🟡 56 行/12 锚/8 闭环/20 问)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| **19 抽象方法面** (实测修正, 基线 17 错) + default 便捷族 | BaseMapper.java:108-423 |
| **default 直连**: getMybatisMapperProxy→sqlSession 直调 (绕过 MapperMethod) | BaseMapper.java:136-138,205-216 |
| selectOne 双态 (throwEx 抛/取首个) + exists | BaseMapper.java:305-329 |
| ServiceImpl: baseMapper 注入/工厂从代理提取 (volatile 懒加载) | ServiceImpl.java:58-93 |
| getSqlStatement: 复用 MP-1 注入 statementId | ServiceImpl.java:166-170 |
| saveBatch: @Transactional+executeBatch 分批提交 (DEFAULT_BATCH_SIZE=1000) | ServiceImpl.java:180-190 |
| retBool: 影响行数→boolean | SqlHelper.java:113-123 |
| 链式入口: queryChain/lambdaQuery → LambdaQueryChainWrapper (MP-3) | IService.java:596-655 |

**时空溯源**: 2018-02 3.0 分包初始版 → 2019 链式 rename → 2024-04 代理获取增强。

**REVIEW 历史 (2 处)**: **17→19 抽象方法** (含基线污染, 8 处同步修正) / default 直连/双态/批量逐字对照。

**负面空间**: 不做事务注解 / 不做 SQL 拼接 / 不做实体校验。

### MP-3 Lambda 条件构造器 (🔴 81 行/12 锚/8 闭环/20 问/harness 7/7)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| **SerializedLambda 反序列化黑魔法**: resolveClass 重写 JDK→MP 镜像 (JDK16 强封装) | SerializedLambda.java:42-60,47-52 |
| **LambdaUtils 三路提取**: IDEA 代理/反射 writeReplace/序列化兜底 | LambdaUtils.java:50-62 |
| **列名解析链**: methodToProperty→formatKey 全大写→ColumnCache (MP-2 产物) | AbstractLambdaWrapper.java:127-133 |
| addCondition 统一入口 + **MPGENVAL 参数命名** | AbstractWrapper.java:467-470; AbstractWrapperTest.java:18 |
| **MergeSegments 四段** + **NormalSegmentList 净化** (首段 and/or 丢弃/同类合并) | MergeSegments.java:33-71; NormalSegmentList.java:30-60 |
| 嵌套 and/or/nested (Consumer→APPLY 段) | AbstractWrapper.java:218-234 |
| select 族 (列集合/TableFieldInfo 谓词→chooseSelect) | LambdaQueryWrapper.java:50-145 |
| 链式终点: LambdaQueryChainWrapper→baseMapper.selectList | LambdaQueryChainWrapper.java |
| **apply 受控片段** (L243-248, {} 占位) | AbstractWrapper.java:243-248 |

**时空溯源**: 2018-05 初始版 ("完成简单 lambda 解析代码") → 2018-10 **反序列化安全性重构** (resolveClass) → **2021-07 JDK16 兼容** (162fc4358)。

**REVIEW 历史 (3 处)**: 负面空间与 apply 矛盾 / SerializedLambda 镜像原因表述 (模块/强封装) / 通过项 (maybeDo L544/columnToSqlSegment L671)。

**负面空间**: 不做任意字符串拼接 (仅 apply 受控片段) / 不做执行 / 不做多表 join。

### MP-4 插件体系 (🔴 62 行/11 锚/7 闭环/20 问/harness 5/5)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| **5 @Signature 挂载** (StatementHandler.prepare/getBoundSql + Executor.update/query×2) | MybatisPlusInterceptor.java:41-48 |
| plugin 过滤: 只包装 Executor/StatementHandler | MybatisPlusInterceptor.java:110-115 |
| intercept 三路分发 (query/update/StatementHandler) | MybatisPlusInterceptor.java:56-107 |
| **query 重发机制**: willDoQuery(false→emptyList 短路)+beforeQuery 改 boundSql→createCacheKey→重发 | MybatisPlusInterceptor.java:64-81 |
| **willDoQuery 真实用例: 分页 count+continuePage 短路** (非 SQL 审查) | PaginationInnerInterceptor.java:116-145,432-450 |
| willDoUpdate 短路 -1 | MybatisPlusInterceptor.java:82-89 |
| StatementHandler 两路 (args null→getBoundSql/否则 prepare) | MybatisPlusInterceptor.java:90-104 |
| InnerInterceptor 6 回调+setProperties 全 default | InnerInterceptor.java:53-126 |
| 配置驱动: PropertyMapper @分组→newInstance+属性注入 | MybatisPlusInterceptor.java:138-146 |
| **@InterceptorIgnore 各插件自查** (宿主不感知) | BlockAttackInnerInterceptor.java:57; DataPermissionInterceptor.java:69 |

**时空溯源**: 2020-06 3.4.0 引入 → 2020-06-30 setProperties 实现。

**REVIEW 历史 (3 处)**: **willDoQuery 用例编造** (实为 continuePage) / @InterceptorIgnore 补充 / 重发无递归+CacheKey 分离验证。

**负面空间**: 不拦 Parameter/ResultSet 处理器 / 不插件间依赖管理 / 不异常重试。

### MP-5 分页插件 (🔴 64 行/12 锚/8 闭环/20 问/harness 8/8)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| **willDoQuery count 预检**: 4 条件不接管 (page null/size<0/!searchCount/自定义 handler) + count 查询 + setTotal + **continuePage 短路** (总数 0/页码越界未开 overflow) | PaginationInnerInterceptor.java:116-145,432-450 |
| **buildAutoCountMappedStatement**: _mpCount+computeIfAbsent 缓存+复用 sqlSource+Long resultMap | PaginationInnerInterceptor.java:230-253 |
| autoCountSql 优化 (去 orderBy/简化列, 测试断言 COUNT(*) AS total) | PaginationInnerInterceptor.java:260-275 |
| **beforeQuery 改写**: concatOrderBy→size<0 特例 (只排序)→handlerLimit→dialect.buildPaginationSql | PaginationInnerInterceptor.java:149-195,454-456 |
| **offset: current≤1→0, 否则 (current-1)*size** (IPage.java:71-77) | IPage.java:71-77 |
| 方言: MySql LIMIT ?(offset=0)/LIMIT ?, ?(setConsumerChain) | MySqlDialect.java:25-40 |
| DialectFactory EnumMap + **JdbcUtils.getDbType 自动识别** | DialectFactory.java:32-75; L196-202 |
| DialectModel setConsumer 占位标记 + consumers 注入 | DialectModel.java:100-145 |
| findPage (core/toolkit/ParameterUtils L42): IPage 参数/Map "page" 键 | ParameterUtils.java:42 |

**时空溯源**: 2018 方言框架已有 → 2020-06 插件引入 → 2020-07 "新分页插件优化" (continuePage)。

**REVIEW 历史 (7 处)**: header 8→7 KP / harness 双参数用例 (offset=0 单参数) / **offset 算法错误** (current*size→(current-1)*size) / ParameterUtils 位置 (core/toolkit) / **改写后 mpBoundSql.sql() 写回** (L190-191) / count 流程逐字对照 / 负面空间 4 条。

**负面空间**: 不做逻辑分页 (物理改写, 对照 M-5 RowBounds) / 不缓存 count 结果 / 复杂 SQL 优化降级。

### MP-8 逻辑删除 (🟡 82 行/17 锚/8 闭环/20 问)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| **双值三级来源**: @TableLogic value/delval > 全局 DbConfig (logicDeleteValue="1"/logicNotDeleteValue="0") > 默认; 全局 logicDeleteField 属性名兜底 | TableFieldInfo.java:406-430; GlobalConfig.java:180-186 |
| **fail-fast**: 多 @TableLogic Assert 抛错 (同 @Version); logicDeleteFieldInfo 唯一聚合 | TableInfo.java:493-512 |
| **getLogicDeleteSql 双语义**: isWhere=true→未删除值 (查) / false→删除值 (SET); "NULL"→IS NULL/=NULL 特例; charSequence 引号 | TableInfo.java:437-470 |
| **方法类双路径**: isWithLogicDelete → UPDATE(逻辑)+addUpdateMappedStatement / 物理 DELETE — 命令切换是填充触发根源 | Delete.java:47-62; DeleteById.java:57-79 |
| **查询三路**: SelectById 尾缀 / wrapper convertWhere / getAllSqlWhere 排除 deleted 自身 | SelectById.java:50; AbstractMethod.java:243-246 |
| **更新双防护**: updateById AND deleted=0 (更新已删行数 0); SET 恒排除 deleted; BlockAttack 协同 | UpdateById.java:48-51; AbstractMethod.java:120-130 |
| **填充协同 (MP-7)**: withUpdateFill 直拼 SET (**`!isSimpleType(_parameter)` 守卫**: deleteById(id) 不填充/deleteById(et) 填充) + UPDATE 触发 updateFill — 删除人/时间自动填充 | DeleteById.java:57-67,64; LogicDelTest.java:83-94,127 |
| **批量双形态**: foreach SimpleType→#{item} / 实体→#{item.id}; 实体删除转换 (主键类型匹配) | DeleteByIds.java:84-100 |

**REVIEW 历史 (8 处)**: ①LogicDelTest 行号 58-64→83-94,127 ②Q16 全表更新拦截覆盖缺口补全 ③通过项锚点验证 ④二次 REVIEW: **源码 javadoc 矛盾实证** (formatLogicDeleteSql javadoc 与实现写反 L450-454) ⑤**SimpleType 守卫机制缺失** (`!isSimpleType(_parameter)` L64 — deleteById(id) 不填充) ⑥值直拼不参数化负面空间补 2 条 ⑦共存措辞澄清 (不同表可选/同表不混) ⑧additional 条件顺序 (optlock 前 deleted 后)。

**负面空间**: 不转换手写 SQL (682) / 不做物理删除共存 (同表不混) / 不处理 INSERT / **不填充 id 参数删除** / **值不参数化 (注入面限配置方)** / 不自动迁移清理 / 不做多表级联。

### MP-7 自动填充 (🟡 82 行/20 锚/8 闭环/20 问)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| **执行入口**: LanguageDriver 覆写 createParameterHandler → MybatisParameterHandler 构造 (super 存参后 L69 processParameter) — 每次 SQL 执行 new ParameterHandler 必触发 | MybatisXMLLanguageDriver.java:45-46; BaseStatementHandler.java:70; SimpleExecutor.java:48,62,75 |
| 命令过滤: 非 null + 非 SimpleType + INSERT/UPDATE | MybatisParameterHandler.java:72-79 |
| **提取链四分支**: Collection/数组/Map 值展开+objectSet 去重 (重入修复 ae5592621)/实体; Map 找 et 键 | MybatisParameterHandler.java:188-221,85-94 |
| **三重守门**: openInsertFill() 旧签名 + openInsertFill(ms) (3.5.6 按 ms.id 跳过, cd0238821) + isWithInsertFill (表级聚合) | MybatisParameterHandler.java:128-142; TableInfo.java:493-508 |
| **strictFill 三条件**: property 同名 + fieldType 精确 (Class.equals) + fill 标记, findFirst; 泛型 <T,E extends T> 子类值 | MetaObjectHandler.java:195-207; StrictFill.java:31-51 |
| **策略族**: setFieldValByName (值非 null)/fillStrategy (空才填)/strictFillStrategy (双保险) — 有值不覆盖 = 幂等 | MetaObjectHandler.java:101-106,218-223,234-242 |
| **SQL 直拼**: withInsertFill/withUpdateFill 无 if 标签 (必有值断言, 优先级压过 FieldStrategy: convertIf L596-608 NEVER→null 整列不进, fill 绕过; 对照 version if 守卫) | TableFieldInfo.java:455-462,486-493,512-530,596-608 |
| 主键先行: populateKeys (getKey>=3 即 ASSIGN_ID/UUID) 先, insertFill 后; AUTO/INPUT 不管 | MybatisParameterHandler.java:101-126 |
| 时序: 拦截器 beforeUpdate (乐观锁) 外层 → ParameterHandler updateFill 内层 | MybatisPlusInterceptor.java:84-89 |
| **逻辑删除触发面**: LOGIC_DELETE_BY_ID = UPDATE 命令 + withUpdateFill 直拼 SET (MP-8 连接) | DeleteById.java:57-67 |

**时空溯源**: 2016-08 接口初始 → 2017-06 FieldFill 枚举 → 2020-06 strictFill 族+泛型子类值 (44c421836) → 2023-09 重入修复 (ae5592621) → 2024-03 ms 级开关 (cd0238821)。

**REVIEW 历史 (12 处)**: ①构造器顺序写反 (super 先 L65→L69) ②负面空间"不做分布式 ID"错误 (populateKeys 即雪花) ③setFieldList 行号 503→493-508 ④M-2/M-6 双链目录名 ⑤SimpleExecutor 方法归属 (doUpdate/doQuery/queryCursor) ⑥二次 REVIEW: **FieldStrategy 优先级缺失** (fill 绕过 convertIf, NEVER→null 整列不进) ⑦**注册路径缺失** (MybatisConfiguration L94 setDefaultDriverClass) ⑧version-fill 表述 (无代码互斥, 乐观锁先写+有值不覆盖) ⑨基本类型 int 不匹配边界 (int.class vs Integer.class) ⑩updateById Map 措辞 ⑪MybatisUtils 澄清 (与填充无关, 属 MP-2/MP-9) ⑫setDefaultScriptingLanguage 兜底通过项。

**负面空间**: 不填充 update(wrapper) (官方) / 不处理 SELECT/DELETE / 不管 AUTO/INPUT 主键 / 不校验填充结果 (DB NOT NULL 兜底) / 不做并发控制 (幂等)。

### MP-6 乐观锁 (🟡 53 行/9 锚/6 闭环/20 问)

**机制速查**:

| 机制 | 关键行号锚点 |
|:--|:--|
| beforeUpdate 入口 (仅 UPDATE+Map 参数) | OptimisticLockerInnerInterceptor.java:106-116 |
| 旧值捕获: et=ENTITY→versionFieldInfo (MP-2)→versionField.get; null→exception/return | OptimisticLockerInnerInterceptor.java:118-136 |
| **双路径**: update→wrapper.apply({0} 旧值)/新建 UpdateWrapper.eq; updateById→MP_OPTLOCK_VERSION_ORIGINAL | OptimisticLockerInnerInterceptor.java:143-154 |
| 新值回写: versionField.set(et, 新值) | OptimisticLockerInnerInterceptor.java:156 |
| **VERSION_FUNCTION_MAP 8 类** (L290-297): long/Long/int/Integer→+1; Date/Timestamp/LocalDateTime/Instant→now; 不支持→原值 | OptimisticLockerInnerInterceptor.java:290-305 |
| **getUpdatedVersionVal protected 可覆写** (自定义策略扩展点) | OptimisticLockerInnerInterceptor.java:308-311 |
| SQL 联动: **getVersionOli** `AND version=#{MP_OPTLOCK_VERSION_ORIGINAL}` (MP-2) | TableFieldInfo.java:578-586 |
| wrapperMode: 无实体仅 wrapper→setVersionByWrapper | OptimisticLockerInnerInterceptor.java:158-163,198-203 |

**时空溯源**: 2020-06 引入 → 2022-01 wrapper 模式支持 (pull/3664)。

**REVIEW 历史 (3 处)**: **VERSION 7→8 类型** (漏 long.class) / getUpdatedVersionVal 覆写点补充 / 双路径自洽性验证。

**负面空间**: 不做重试 (冲突行数 0 归业务) / 不做悲观锁 (对照 spring-tx) / 不做版本回滚。

---

## §五 质量追踪

| 指标 | 数据 |
|:--|:--:|
| M 阶段 | 7/7 完成, 每域 KP+大纲+questions+六层深审+时空溯源+harness |
| MP 阶段 | 9/9 完成 (收官, 执行期注入三件套: 乐观锁+填充+逻辑删除) |
| harness | M 6 个 (6/6~15/15 PASS) + MP 5 个 (5/5~8/8 PASS), 全部 javac+java 验证 |
| REVIEW | 每域 2~10 处真实问题; 代表性: canHaveStatement 编造/willDoQuery 用例编造/offset 算法/17→19/VERSION 7→8/CacheKey 值语义 |
| 负面空间 | 每域 3-4 条 "不做" 声明 (07 维度5 必检) |
| 闭环 | M 66 条 + MP 71 条 = 137 条, 全部内化 KP §05 |
| 执行计划 | 6 项修正全部落文档 (§二) |
| 交接文档 REVIEW (2026-08-13) | M-7 questions 文件从未创建→已补 21 问; 锚点 5 处偏差→全修正; harness/KP/questions/路径/落点全验证 |

---

## §六 高频坑 (跨阶段 12 条)

1. **批量操作 = 被质疑** — 一个域一个域, 问题驱动, 汇报确认后再下一个
2. **行号硬限制 = 被质疑** — 内容优先 (09 反模式 7/8)
3. header 闭环列表与 KP §05 不同步 (累计 8+ 次被抓)
4. 条件写反/用例编造 — 机制描述必须配"真实实现者"验证 (updateCount/直方图/continuePage)
5. 默认值编造 — grep DEFAULT_* (testWhileIdle)
6. 数字穷举 — 目录/接口/类型逐项数 (spi 14→16/方言 28→29/BaseMapper 17→19/VERSION 7→8/别名 22/字段 24)
7. 行号必须 grep 原文件, 引用区间不得超文件行数 (Interceptor L25-41 超 35 行文件)
8. 内部矛盾 — 负面空间声明 vs 实际 API (apply 存在却说无字符串拼接)
9. 基线数字同样不可信 — MP-PLAN "17" 错, 每数字独立穷举 (09)
10. 前向引用: 未分析域用导航指针; 已分析域可真实引用 (M 完成后 MP 导航已升级)
11. harness 验证价值: 每次抓到真实机制偏差 (CacheKey 值语义/FilteredContext 时机/Trim 先 apply/5 占位模板)
12. 跨仓库: 引用他域机制必须回验 (DefaultParameterHandler "三路" vs M-2 createCacheKey 四路)

---

## §七 阶段3.3 收官 — MP 9/9 全量完成

**MP 全部 9 域完成** (MP-2 历史 + 本阶段 8 域)。执行期注入三件套: 乐观锁 (MP-6) + 自动填充 (MP-7) + 逻辑删除 (MP-8), 汇聚 BaseMapper 19 方法面 (MP-9)。无待办域。

**下一步 (阶段3.5)**: Redis 09 域重审已完成 (18→33, REDIS-PLAN.md v2 §〇 审计表)。数字断言 8 项全接受; **域清单遗漏 14 个定义特征级机制** (命令层 t_*×4/serverCron/db 键空间/expire/evict/networking 协议/pubsub/Lua/listpack/ACL/module) — 域清单错误率 78% (MyBatis 5→7 的放大版)。拓扑重排: R-2 事件驱动 2→12 位、R-1 object 1→7 位。开工首域 R-4 (SDS)。

---

## §八 后续阶段提醒

- **阶段3.5 Redis (33 域)**: ✅ **33/33 收官 (2026-08-14)** — 44 篇大纲/3691 行/18 harness/147 处问题; HANDOFF-REDIS-V4.md 唯一入口
- **阶段4.1 Kafka (12 域)**: 🔵 **10/12 (2026-08-15)** — KAFKA-PLAN.md (09 审计: 数字 8/8 接受, 淘汰表 1 笔误修正) + HANDOFF-KAFKA.md v11 唯一入口; K-3 ✅ + K-4 ✅ + K-12 ✅ + K-7 ✅ + K-1 ✅ + K-2 ✅ + K-6 ✅ + K-5 ✅ + K-8 ✅ + K-11 事务 ✅ (2 篇大纲 + 6 闭环 + 18 问: 幂等/两阶段/Marker); 下一步 K-10 Compaction (🟡 B)
- **阶段4.1 RocketMQ (13 域)**: ✅ **13/13 收官 (2026-08-14)** — ROCKETMQ-PLAN.md (09 审计: 10→13 域, v3 定级理由) + HANDOFF-ROCKETMQ.md v14 唯一入口; 全部 13 域交付 (大纲 974 行/发现 197/行号验证 ~521); 执行计划顺序问题已核查 (阶段 4 内部无强依赖链, 唯一真实依赖 Curator→ZK 已满足; Kafka 4.1.2 为 KRaft 时代, "Kafka 依赖 ZK"排序基于过时认知)
- **阶段4.3 ZooKeeper (9 域)**: ✅ **9/9 收官 (2026-08-15)** — ZOO-PLAN.md (09 审计: 4 修正 — Z-8 类名属 Curator/Java 客户端在 server 模块/WatchManager 双实现/sessionWatches 断言) + HANDOFF-ZOOKEEPER.md v10 唯一入口; 全部 9 域交付 (大纲 566 行/域文件 4173 行/REVIEW 136 处/harness 7/7 4/4); 收官亮点: Z-8 前驱消失悬挂竞态三处三处理 + Z-9 CRC 覆盖 Javadoc 不符 (len 无保护静默截断) + 快照重试残留面 (分类集合不清)
- **阶段4.5 Curator (8 域)**: ✅ **8/8 收官 (2026-08-15)** — CURATOR-PLAN.md (09 审计: 5→8 域 + RetryPolicy 5→6 种 + PathChildrenCache 枚举修正) + HANDOFF-CURATOR.md 唯一入口; Apache Curator 5.8.0; C-1 Framework / C-2 Leader / C-3 锁 / C-4 队列屏障 / C-5 缓存监听 / C-6 共享原子量 / C-7 持久节点 / C-8 服务发现 (48 节/147 harness 全过); 亮点: 旧三件套 @Deprecated → CuratorCache (ZK 3.6+ 持久 watcher)
- **阶段4.4 Seata (13 域)**: ✅ **13/13 收官 (2026-08-15)** — SEATA-PLAN.md (09 审计: 3 补充 — 21 态/SAGA_ANNOTATION/无 NESTED) + HANDOFF-SEATA.md v13 唯一入口; 全部 13 域交付 (大纲 857 行/域文件 5668 行/REVIEW 190 处/harness 11/11); 收官亮点: S-1 21 态状态机 + S-2 GlobalFinished #489 + S-11 子表片数数学 + S-12 7 锁实现修正 + S-13 双面多态收束
- **阶段5.2 Dubbo (7 域)**: 🔵 **2/7 (2026-08-15)** — DUBBO-PLAN.md (09 审计: 2 补充 — 3.x ExtensionDirector/3 目录 + AdaptiveLoadBalance 修正) + HANDOFF-DUBBO.md v2 唯一入口; D-1 ✅ (SPI 微内核 — 18 发现) + D-2 ✅ (服务导出 — harness 12/12, 11 发现: 6 层链/scope 三态/RegisterType/serverMap reset/回调导出); 下一步 D-3 服务引用 (🔴 A); 注: 阶段 4.6 SofaJRaft + 5.1 Feign 由其他 AI 并行
- 每仓库开工: 先建 PLAN (含怀疑审计表 §八) → 再逐域
- Obsidian 知识图谱: 全部域大纲转双链 vault (全局待办)

---

## §九 文件路径

```
analysis/source-analysis/
├── talk-method/.../methodology/zh/ (01-09 权威)   ← 09 含反模式 7/8
├── issue/源码分析执行计划.md                       ← 已加 3.3/3.4 注记 (2 处)
├── druid/  (HANDOFF-DRUID-v2.md 权威)
├── mybatis/
│   ├── M-PLAN.md (7 域, 含怀疑审计表 §八)
│   ├── HANDOFF-M.md (412 行, 分域 REVIEW 全量)
│   ├── knowledge-planning/ (m1~m7) + outlines/ (m1~m7 各 2 文件) + harness/ (6 个)
└── mybatis-plus/
    ├── MP-PLAN.md (9 域, 已修 19 抽象方法)
    ├── HANDOFF-MP.md (398 行, 分域 REVIEW 全量)
    ├── knowledge-planning/ (mp1~mp9) + outlines/ (9 域) + harness/ (4 个)
├── redis/
    └── REDIS-PLAN.md (32 域, 含 §〇 09 怀疑审计表 — 18→32)

源码:
├── /data/workspace/source-code/code/spring/mybatis/      (386 文件)
├── /data/workspace/source-code/code/spring/mybatis-plus/ (403 文件)
└── /data/workspace/source-code/code/spring/redis/        (175 文件, Redis 7.4.2)
```

---

## §十 完成检查单 (下次会话开始前)

- [x] 已读本文 §零~§九 (15 域交付详情内联)
- [x] 已读 09 方法论 (反模式 7/8 尤其)
- [x] 阶段3.3 MP 9/9 全量完成 (执行期注入三件套收官)
- [x] 阶段3.5 Redis 09 域重审完成 (18→33, REDIS-PLAN.md v2 §〇 审计表)
- [x] 每域完成后更新本文 §零/§四/§五
- [ ] 开工 Redis 首域 R-4 (SDS) 前: 读 REDIS-PLAN.md 拓扑序 + 对 R-4 数字断言复查
- [ ] 每域走 v5 全管线 (KP→大纲→questions→六层深审→更新 HANDOFF)
