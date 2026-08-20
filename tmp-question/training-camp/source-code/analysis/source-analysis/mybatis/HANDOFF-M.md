# MyBatis 源码分析 — 交接文档 (阶段3.4)

> **日期**: 2026-08-13 | 阶段3.4 MyBatis
> **⚠️ 总入口**: 阶段3 总交接见 `../HANDOFF-STAGE3.md`(M+MP 全部状态/方法论/下一步) — 本文为 MyBatis 分域细节。
> **给新 AI**: 本文是 MyBatis 分析唯一入口。规划权威 = `M-PLAN.md` (7 域 v1)。方法论权威 = `talk-method/source-code-analysis/methodology/zh/` (01-09, **09 对既有规划保持怀疑必读**)。MP 阶段见 `../mybatis-plus/HANDOFF-MP.md` (已按顺序变更调整)。
> **顺序背景**: 原执行计划 3.3 MP 先于 3.4 M — 09 重审实测 **MP→M 单向依赖** (474 import / 反向 0) 反拓扑 → **M 提前执行**, MP 暂停待回流。
> **任务**: 7 域执行序 **M-1 → M-2 → M-6 → M-7 → M-3 → M-5 → M-4** (拓扑: M-1 Hub → M-2 执行链 → M-6 动态 SQL → M-7 缓存 → M-3 代理 → M-5 映射 → M-4 插件最后拦截 4 处理器)。严格一个域一个域, 问题驱动, 禁止批量写。

---

## §零 当前状态速查

| 域 | 目录 | 类型 | 行数 | 锚点 | 闭环 | questions | 状态 |
|:--:|---|:--:|:--:|:--:|:--:|:--:|:--:|
| M-1 Configuration 核心+装配 | outlines/m1-configuration | 🔴 | 66 | 18 | 11 | 21 | ✅ 完成 (深审+时空溯源+harness 通过) |
| M-2 SqlSession/Executor 链 | outlines/m2-executor | 🔴 | 69 | 15 | 11 | 21 | ✅ 完成 (深审+时空溯源+harness 通过) |
| M-6 动态 SQL | outlines/m6-scripting | 🔴 | 69 | 14 | 9 | 21 | ✅ 完成 (深审+时空溯源+harness 通过) |
| M-7 缓存体系 | outlines/m7-cache | 🟡 | 76 | 22 | 8 | 21 | ✅ 完成 (深审+REVIEW, 内容完整版) |
| M-3 Mapper 代理 | outlines/m3-mapper | 🔴 | 84 | 20 | 9 | 21 | ✅ 完成 (深审+时空溯源+harness 通过) |
| M-5 参数/结果映射 | outlines/m5-mapping | 🔴 | 71 | 14 | 11 | 21 | ✅ 完成 (深审+时空溯源+harness 通过) |
| M-4 插件机制 | outlines/m4-plugin | 🔴 | 62 | 11 | 7 | 20 | ✅ 完成 (深审+时空溯源+harness 通过) |

**执行序**: M-1 ✅ → M-2 ✅ → M-6 ✅ → M-7 ✅ → M-3 ✅ → M-5 ✅ → M-4 ✅ — **M 阶段 7/7 完成** → 回流 MP (MP-1~MP-8)

---

## §一 M-1 已交付内容 (Configuration 核心+装配)

### 产出物

- `knowledge-planning/m1-configuration.md` — KP (§0.8+01 提取 13 行+P1P2P3 9 条+§05 闭环 11 条)
- `outlines/m1-configuration/01-configuration.md` — 大纲 (65 行, 5 节+结尾桥)
- `outlines/m1-configuration/completeness-questions.md` — 3 身份 21 问
- `harness/m1-configuration/MiniMyBatisConfig.java` — 极简复现 (编译运行 10/10 PASS)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| 入口一次性: SqlSessionFactoryBuilder.build → parse (parsed 标志) | SqlSessionFactoryBuilder.java:47-63; XMLConfigBuilder.java:95-112 |
| 11 元素分区定序: properties(#117)→settings→vfsImpl/logImpl 提前→typeAliases→plugins→objectFactory 族→settings→environments(#631)→databaseIdProvider→typeHandlers→mappers | XMLConfigBuilder.java:114-135,153-196 |
| properties 三级合并: 构造 props > 文件 > XML 内嵌; resource/url 互斥 | XMLConfigBuilder.java:237-259 |
| settings 反射白名单: MetaClass.hasSetter(Configuration) | XMLConfigBuilder.java:137-151,261-296 |
| 22 内置别名注册表 | Configuration.java:191-223 |
| StrictMap: put 冲突抛/短名自动注册/Ambiguity 占位/get 缺失抛 | Configuration.java:1104-1202 |
| incomplete 延迟解析: 4 集合+4 锁+buildAllStatements 触发链+resultMaps do-while 多轮 | Configuration.java:170-178,966-1055 |
| 4 处理器工厂统一 pluginAll (M-4 拦截点) | Configuration.java:703-742 |
| newExecutor 装饰链: SIMPLE/REUSE/BATCH→CachingExecutor(内)→pluginAll(外) | Configuration.java:728-742 |
| MappedStatement 24 字段 + Builder 默认值 (PREPARED/Jdbc3KeyGenerator 条件) | MappedStatement.java:34-86 |
| Mapper XML 六段装配 + databaseId 双路径 + IncompleteElementException 延迟 | XMLMapperBuilder.java:96-147 |

### 时空溯源结论 (04 方案 A 强制项)

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| e357f36a6 | 2010-05 | 初始版 (474 行): StrictMap+Ambiguity+内置别名已定型 — 注册表语义 14 年稳定 |
| c215f616b | 2010-10 | issue 133: sqlFragments 跨 mapper XML 引用 |
| 13d9aa89f | 2010-12 | bug 179: 非全限定 namespace 短名支持 |
| d64edc518 | 2022-10 | StrictMap HashMap→ConcurrentHashMap + containsKey null 保护 — 并发构造竞态修复 |
| 1e9eb69cd | 2023-11 | incomplete 集合 synchronized→ReentrantLock |
| fba61d2b7 | 2024-02 | parse pending elements 重构 |

### 本域 REVIEW 真实问题 (六层深审抓到)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 密度 | 首版大纲 85 行 (🔴 上限 69) | §1+§2 合并为 5 节, 65 行 |
| 2 | **数字** | "25 内置别名" — 实测 grep registerAlias **22 个** | KP+大纲+questions 三处修正 |
| 3 | **数字** | "MappedStatement 26 字段" — 实测实例字段 **24 个** (L36-59) | KP+大纲+questions 三处修正 |
| 4 | **数字+自相矛盾** | "12 分区" — 实际 **11 个 XML 元素** (parseConfiguration 14 调用点, settings 拆三段); 大纲数据流已写 11 但标题写 12 | 统一 11 元素分区 |

### 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 5 | **数字** | "settings 40+ 项" — 实测 L261-296 仅 **29 个 set 调用** | 大纲改 29 项 |
| 6 | **归属** | "vfsImpl→VFS.addImplClass" — 实为 loadCustomVfsImpl 只调 `configuration.setVfsImpl`, addImplClass 在 Configuration.setVfsImpl 内部 (L248-252) | 大纲 L16 归属修正 |
| 7 | **语义精确化** | "parsePending* removeIf 成功移除" 概括不精确 — **statements 靠异常中断保留**(return true), **cacheRefs 靠返回值条件移除**(resolveCacheRef()!=null), 两种模式 | KP q6 补两种模式 |
| 8 | **维度5 负面空间** | M-1 无任何"不做"声明 (0) — 07 维度5 对比型域必检 | 补 3 条: 不热更新/不递归包扫描 (ResolverUtil VFS.list 非递归 L246-254)/不强 XML 校验 |
| 9 | 通过项 | 维度1 桥 OUT→M-2 ✅; 维度2 锚点 17≥8 ✅; 维度3 前向引用均导航指针 ✅; 维度4 并发横切 (StrictMap/4 锁) 已覆盖 ✅; 维度5 开篇场景具体 ✅ | 记录备查 |

### 六层深审验证

- 层1 行号: 全部 grep 原文件 (含 StrictMap L1148-1160/L1176-1182、MappedStatement L78-85、XMLMapperBuilder L96-147)
- 层5 算法: keyGenerator 条件 (useGeneratedKeys&&INSERT)、装饰链顺序 (缓存在内/插件在外)、put 冲突/短名/Ambiguity 分支 — 逐字对照 + harness 实证
- 层6 归属: 4 工厂/StrictMap/incomplete 均属 Configuration; 六段装配属 XMLMapperBuilder

---

## §二 方法论速查 (每域必走)

```
Pass 0 读上下文 → Pass 1 扫轮廓(≥5 真问题/读 2 测试) → Pass 2 闭环(内化 KP §05)
→ Pass 3 大纲(四要素+header 闭环同步) → 六层深审(必须真找问题)
→ 04 方案 A 强制项: 时空溯源(git 历史) + 极简复现(harness) → 全量回归 → 更新本文
```

**格式**: header 必含 前置/复用/对照/引出 + `类型 | KP 数(P 条目数) | 模式` + `Pass 2 闭环`(与 KP §05 同步) + 读者处境; 每节四要素; 结尾桥独立段落。**⚠️ 2026-08-13 修正: 行号限制(🔴 39-69/🟡 35-49)废弃为软参考 — 内容完整 > 行数, 禁止为压行删除已验证机制内容 (09 反模式 7/8)。** 锚点 🔴≥8 / 🟡≥4。

**产出物**: KP + outlines/{域}/01-*.md + completeness-questions.md (≥3 身份 ≥5 问) + harness。

**检查命令**: 五要素 grep 全等 / wc -l 密度 / tail -1 → 引出 / KP P1P2P3 条数=色=为什么 / header qN=KP §05 条数 / 锚点密度。

---

## §三 高频坑

1. **数字穷举** — M-1 教训: "25 别名"实为 22、"26 字段"实为 24、"12 分区"实为 11 — 凡数字必须 grep/ls 实测
2. 批量操作 = 被质疑 — 一个域一个域
3. header 闭环与 KP §05 同步 (Druid 8 次被抓)
4. 条件写反 — 逐字对照源码
5. 默认值编造 — grep DEFAULT_*
6. 行号必须 grep 原文件
7. 前向引用: 未分析域用导航指针 (MP 各域正文禁展开 MyBatis 内部)
8. 自相矛盾: 同一文件标题与数据流数字不一致 (M-1 12 vs 11)

---

## §四 文件路径

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/mybatis/
├── M-PLAN.md                       ← 规划权威 (7 域 v1, 含怀疑审计表 §八)
├── HANDOFF-M.md (本文)             ← M 阶段权威
├── knowledge-planning/m1-configuration.md   ← M-1 KP ✅
├── outlines/m1-configuration/               ← M-1 大纲+questions ✅
└── harness/m1-configuration/                ← MiniMyBatisConfig ✅

源码:
/data/workspace/source-code/code/spring/mybatis/  (386 文件, org.apache.ibatis 22 包)
├── session/(Configuration 1204/SqlSession 380/defaults/)    builder/(xml 23/annotation)
├── mapping/(MappedStatement 347/ResultMap 261/ParameterMapping 224)
├── executor/(52 文件)  scripting/(28)  cache/(19)  binding/(6)  plugin/(8)
├── type/(56)  reflection/(36)  annotations/(30)  datasource/(13)  transaction/(10)
```

---

## §五 状态 — M 阶段 7/7 完成, 下一步回流 MP

**M 阶段全部交付**: M-1(66)/M-2(69)/M-6(69)/M-7(76)/M-3(84)/M-5(71)/M-4(62)。执行序 (拓扑): M-1→M-2→M-6→M-7→M-3→M-5→M-4 ✅。

**下一步**: 回流 MP 阶段 — `../mybatis-plus/HANDOFF-MP.md` (MP-2 已完成, 剩余 MP-1→MP-9→MP-3→MP-4→MP-5→MP-6→MP-7→MP-8)。届时 MyBatis 导航指针全部升级为真实引用 (MP-1 注入器↔M-1 StrictMap/MP-4 插件↔M-4 InterceptorChain/MP-5 分页↔M-2 Executor)。

## §六 M-4 已交付内容速查 (插件机制)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| Interceptor 三方法契约 + Signature 精确匹配 | Interceptor.java:25-41; Signature.java:25-54 |
| pluginAll 洋葱嵌套 (后注册包外层) | InterceptorChain.java:32-41 |
| Plugin.wrap 三步骤 + 无匹配不包装 | Plugin.java:44-52,67-100 |
| invoke 双条件分派 + 透传 + unwrapThrowable | Plugin.java:55-65 |
| **Invocation 4 类白名单 (2024-03 引入)** | Invocation.java:30-36 |
| proceed 显式继续链 | Invocation.java:52-56 |
| 4 工厂汇聚点 (M-1 交付) | Configuration.java:703-742 |
| 多租户 schema 用例 + 非法目标实证 | PluginTest.java:75-100 |

### M-4 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| e357f36a6 | 2010-05 | Plugin 初始版 — 插件机制远古定型 |
| 713ab8d74 | 2013-03 | **"Lock down collections"** — getInterceptors 改 unmodifiable |
| 319da5811 | 2024-03 | **"Prevent Invocation from invoking arbitrary method"** — 4 类白名单安全加固 (很新!) |

### M-4 深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | harness 迭代 | getMethod("query") 无参找不到 (接口方法带 String 参); 洋葱断言写反 (最外层 after 最后) | harness 修正 6/6 PASS |
| 2 | 通过项 | 白名单运行期校验 (wrap 不查 invoke 抛) 测试实证; 行号全部 grep; 负面空间 4 条 | 记录 |

### M-4 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 3 | **行号超范围** | Interceptor 接口写 L25-41 — 文件仅 **35 行**, 接口实为 **L23-35**(intercept L25/plugin L27/setProperties L31) | KP §0.8/01 表/q5 + 大纲 §1 修正 |
| 4 | **行号** | InterceptorChain 写 L32-41 — 类 **L25**, pluginAll **L29-34**, addInterceptor L36/getInterceptors L40 | KP §0.8/01 表 + 大纲 §2 修正 |
| 5 | 机制补充 | Intercepts 注解 @Target(TYPE) 未写 | 大纲 §1 补 "放类上" |
| 6 | 通过项 | 维度1 桥 OUT→MP-4 ✅; 维度3 对照 D-2 已分析/MP-4 引出 ✅; 维度4 无并发点(工厂期包装) ✅; 维度5 开篇场景具体 ✅; 洋葱语义 harness 实证与源码一致 ✅ | 记录备查 |

### M-4 负面空间 (07 维度5)

不做任意对象拦截 (4 类白名单) / 不做插件自动排序 (注册序决定) / 不做执行期重载 (工厂创建时包装一次)。

## §七 M-5 已交付内容速查 (参数/结果映射)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| 取参四路: additional>null>裸值>metaObject + jdbcTypeForNull 兜底 | DefaultParameterHandler.java:34-71 |
| 多结果集双循环 + collapse + Cursor 单 map 限制 | DefaultResultSetHandler.java:188-242 |
| 行遍历分流 + 安全守卫 (ensureNoRowBounds/checkResultHandler) | DefaultResultSetHandler.java:330-356 |
| skipRows (FORWARD_ONLY 逐行/absolute) + discriminator | DefaultResultSetHandler.java:359-376,387-402,971 |
| createResultObject 四分支 + 延迟代理 (issue#109/#149) | DefaultResultSetHandler.java:654-700 |
| 构造器自动映射四选一 (单/@AutomapConstructor/argNameBased/类型匹配) | DefaultResultSetHandler.java:729-757 |
| 三级自动判定 (显式>嵌套 FULL>简单非 NONE) | DefaultResultSetHandler.java:466-478 |
| 显式映射: column 存在检查+三路取值+callSettersOnNulls | DefaultResultSetHandler.java:481-515 |
| 自动映射: findProperty 去下划线+忽略大小写+缓存+UnknownColumnBehavior | DefaultResultSetHandler.java:580-645; MetaClass.java:56-64 |
| 嵌套聚合: combinedKey partialObject+putAncestor 循环保护 | DefaultResultSetHandler.java:440-462 |
| ResultSetWrapper: useColumnLabel+mapped/unmapped 缓存 | ResultSetWrapper.java:46-196 |

### M-5 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| 340802b47 | 2015-09 | **"Cached Automatic Mappings"** — autoMappingsCache 引入 (列→属性推断只做一次) |
| 048e474a0 | 2017-02 | Constructor → **AutomapConstructor 重命名** (避免与反射类混淆) |
| 395d36e76 | 2021-03 | argNameBasedConstructorAutoMapping 引入 |
| 66508f0e5 | 2022-04 | 构造器自动映射列排除于属性自动映射 (联动修复) |

### M-5 深审 + REVIEW 记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | harness 迭代 | camel 简化缺转小写 — 真实机制是 **MetaClass.findProperty: replace("_","")+忽略大小写匹配**(L56-64), 非标准驼峰转换 | harness 修正 12/12 PASS + 大纲/KP 机制精确化 |
| 2 | 通过项 | 行号全部 grep 验证 (四分支链/三级判定/四路取参/缓存键); 时空溯源 4 提交; 负面空间 4 条 | 记录 |

### M-5 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 3 | **数字错误** | KP q1/大纲 §1 "与 M-2 createCacheKey **三路**一致" — 实测 M-2 createCacheKey 也是**四路** (hasAdditionalParameter→parameterObject null→hasTypeHandler→metaObject.getValue, BaseExecutor L215-226) | KP+大纲双修正 "四路一致" |
| 4 | **行号** | DefaultParameterHandler: setParameters 写 L34-62 — 实为 **L62-99**(L34 是构造器); 四路取值 L72-82; null 兜底写 L63-71 — 实为 **L87** | KP §0.8/01 表/q1 + 大纲 §1 五处修正 |
| 5 | 精确化 | 大纲 §4 对象创建分支 ① 写"原始值" — 实为 **createPrimitiveResultObject**(L849, Map/List/标量专用创建) | 大纲精确化 |
| 6 | 通过项 | q7 显式映射"三路取值"(nestedQuery/resultSet/typeHandler) 与取参四路不同组, 表述正确; ensureNoRowBounds/checkResultHandler 条件逐字对照; 维度1 桥 OUT→M-4 ✅; 锚点 14≥8 ✅ | 记录备查 |

### M-5 负面空间 (07 维度5)

不做 N+1 优化 (lazy 仅延迟不批量) / 不做自动刷新 (缓存构建期一次) / 不做跨库方言 (归 TypeHandlerRegistry+驱动)。

## §八 M-7 已交付内容速查 (缓存体系)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| Cache 接口 7 方法 + getReadWriteLock default null (3.2.6 起核心不调用) | Cache.java:60-97 |
| PerpetualCache 裸 HashMap 非同步 + equals 按 id | PerpetualCache.java:24-35 |
| CacheBuilder 组装: 默认 Perpetual+Lru + 标准链 Scheduled→Serialized→Logging→Synchronized→Blocking | CacheBuilder.java:92-127 |
| 构造契约: 基础 (String id)/装饰器 (Cache) + 属性反射注入 | CacheBuilder.java:137-177,199-218 |
| readWrite=!readOnly 默认 true → SerializedCache 默认应用 | XMLMapperBuilder.java:169-172 |
| CacheKey: 37 乘子/17 初始/checksum/count + equals 快速失败 + NULL 键单例 | CacheKey.java:33-95 |
| LruCache 双 map 同步 (accessOrder 触摸序+eldestKey 同步删) | LruCache.java:31-93 |
| TransactionalCache 三暂存 + getObject 三态 (含 clearOnCommit issue#146) + commit flush | TransactionalCache.java:43-95 |
| BlockingCache CountDownLatch 击穿防护 (releaseLock 随 commit flush) | BlockingCache.java:41-72,59-63 |
| TransactionalCacheManager per-Cache 包装 + 全量 commit/rollback | TransactionalCacheManager.java:44-57 |

### M-7 负面空间 (07 维度5)

不做分布式 (纯本地, 对照 R-1) / 不做多级淘汰联动 / 不做缓存预热 / 不做写穿透。

---

## §九 M-6 已交付内容速查 (动态 SQL)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| 入口双路径: XNode/`<script>`/String + ${} 变量替换 | XMLLanguageDriver.java:30-48 |
| 9 标签 Handler 注册表 + parseDynamicTags 递归 + isDynamic 传播 | XMLScriptBuilder.java:53-63,76-101 |
| 动态/静态分流: DynamicSqlSource vs RawSqlSource | XMLScriptBuilder.java:65-74; XMLLanguageDriver.java:39-47 |
| __frch_ 参数唯一化 (ITEM_PREFIX L28) + FilteredDynamicContext | ForEachSqlNode.java:28,69-137 |
| Trim 家族: Where=Trim("WHERE",[AND/OR])/Set=Trim("SET",[,],[,]) | WhereSqlNode.java:25-32; SetSqlNode.java:25-30; TrimSqlNode.java:56-60,89-99 |
| ContextMap 四层取值回退 (_parameter/_databaseId) | DynamicContext.java:32-54,78-94 |
| ${} OGNL 求值+injectionFilter+null→"" vs #{} ParameterMapping+? 占位 | TextSqlNode.java:63-83; SqlSourceBuilder.java:42-52 |
| 运行时: getBoundSql apply→parse→setAdditionalParameter; Raw 构建期一次 | DynamicSqlSource.java:30-43; RawSqlSource.java:28-49 |
| OGNL 表达式编译缓存 | OgnlCache.java:44-58 |

### M-6 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| 616e65b1c | 2012-04 | **LanguageDriver 可插拔扩展点引入** (XMLScriptBuilder/OgnlCache/ForEachSqlNode/__frch_ 同期) |
| db88b7838 | 2014-09 | OgnlCache 私有构造微调 |
| 后续 | — | 9 标签映射/trim 家族/DynamicContext 主体自 2012 年稳定 |

### M-6 六层深审 + REVIEW 记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 密度 | 首版 78 行 (上限 69) | 合并 bullet 7 处+删 2 空行, 69 行 |
| 2 | harness 实证 1 | **FilteredDynamicContext 必须是 appendSql 时替换** — harness 构造时替换版 FAIL, 改为委托包装 PASS | 修复 harness (对照源码语义) |
| 3 | harness 实证 2 | **TrimSqlNode 必须先 contents.apply 再修剪** — 简化版漏 apply 致空输出 | 修复 harness |
| 4 | harness 断言反例 | 单条件 Where: 期望 "WHERE AND age..." 写反 — 正确为 AND 被吃+WHERE 前缀 "WHERE age > #{age}" | 修正断言, 8/8 PASS |

### M-6 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 5 | **语义不完整** | 大纲 Set 特化写 "Trim(\"SET\",[,])" — 实为 **Trim(\"SET\",[,],null,[,])**(suffixOverrides=[,] 去尾逗号是 Set 核心), 与 KP q4 不一致 | 大纲补全, KP/大纲统一 |
| 6 | **语义不精确** | "每元素 PrefixedContext(separator)" — 实为**首元素 ""/其余 separator**(first 判定 L90-94) | 大纲精确化 |
| 7 | **技术声明错误** | 负面空间 "injectionFilter 仅显式配置, 默认不开启" — 实为**仅编程式构造可传**, XML/注解路径 `new TextSqlNode(data)` 恒为 null (XMLScriptBuilder L83/XMLLanguageDriver L57 实证) | 负面空间改"编程式构造可传, XML/注解路径恒 null" |
| 8 | 行号 | DynamicContext 区间 L65-70 → appendSql L65/getSql L69/**getUniqueNumber L73** | 改 L65-73 |
| 9 | 数据流 | §1 数据流 "String→parseScriptNode" — String 路径实为 TextSqlNode.isDynamic 分流(不经 XMLScriptBuilder) | 分路径表述 |
| 10 | 通过项 | 维度1 桥 OUT→M-5 ✅; 维度2 锚点 14≥8 ✅; 维度3 对照 D-6 已分析/导航指针 ✅; 维度5 负面空间 3 条 ✅; OgnlCache 类加载防护实证 (OgnlClassResolver→Resources.classForName) ✅ | 记录备查 |

### M-6 负面空间 (07 维度5)

不做方言拼装 (归 MP-5 插件) / ${} 不默认开 injectionFilter (#{} 兜底) / 不缓存 BoundSql (仅 OGNL 编译结果缓存)。

---

## §十 M-2 已交付内容速查 (SqlSession/Executor 链)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| 门面: selectOne 三态/dirty/commit 判定/close 回滚 | DefaultSqlSession.java:73-85,150-203,216-247,261-266 |
| 模板方法: query/update + 4 doXxx 抽象 + 一级缓存 5 清点 | BaseExecutor.java:110-118,132-175,242-266,276-284 |
| CacheKey 六段构成 (参数值参与, 值语义 equals/hashCode) | BaseExecutor.java:198-235 |
| queryStack+PLACEHOLDER 防递归 + deferredLoads | BaseExecutor.java:148-173,331-355 |
| 子类策略 + wrapper 语义 | SimpleExecutor.java:57-93; CachingExecutor.java:46; BaseExecutor.java:347-357 |
| 路由三路 + Prepared 三分支 + 主键回填时机 | RoutingStatementHandler.java:41-56; PreparedStatementHandler.java:48-57,80-95 |
| 二级缓存查询流程 (tcm 延迟提交) | CachingExecutor.java:96-140 |

### M-2 时空溯源结论

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| e357f36a6 | 2010-05 | 初始版: 模板方法+一级缓存+EXECUTION_PLACEHOLDER+queryStack+CachingExecutor 已定型 |
| b32ac15d1 | 2012-02 | clearLocalCacheAfterEachStatement → **localCacheScope=STATEMENT** 枚举化 (issue #482) |
| 21076984d | 2014-03 | queryStack 相关: Selective Lazy Loading (#149) |
| 7f4cccec4 | 2011-04 | deferredLoad 立即加载优化 (issue #301) |

### M-2 六层深审 + REVIEW 记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 密度 | 首版大纲 78 行 (上限 69) | 压缩 bullet 合并/负面空间 3→2/删 2 空行, 69 行 |
| 2 | 行号 | close() 的 executor.close 写 L265 — 实为 **L263** | 大纲修正 (KP 区间写法无污染) |
| 3 | harness 实证 | **CacheKey 必须覆写 equals/hashCode 值语义** — harness 第一版 3 FAIL (HashMap 引用比较永不命中), 修复后 8/8 PASS | 修复 harness + 补入大纲 §3 关键设计 |

### M-2 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | **技术声明** | KP q1 "query/update 是 final 模板" — 实测 **无 final 修饰符** (BaseExecutor L111/L133,142), CachingExecutor 覆写 12 方法 (query/commit/rollback L49+) 实证 | KP q1 改为 "模板方法(无 final 约束)+CachingExecutor 覆写装饰" |
| 5 | **维度4 横切缺口** | SqlSession 线程安全未声明 — 实测 DefaultSqlSession/BaseExecutor **零 synchronized**, queryStack/dirty 均非同步字段 (面试高频点) | 负面空间补第 3 条 "不做线程安全" |
| 6 | 行号 | queryFromDatabase 区间 L332-355 → 方法签名在 **L331** | KP §0.8 精确化 |
| 7 | 通过项 | 维度1 桥 OUT→M-6 ✅; 维度2 锚点 15≥8 ✅; 维度3 前向引用均导航 (tcm 属 M-7 仅接口引用) ✅; 维度5 负面空间 4 条 ✅; 开篇场景具体 ✅ | 记录备查 |

### M-2 负面空间 (07 维度5)

不自动重试/不管理连接池/不改写 SQL — 执行层只按 BoundSql 原样预编译, 重试归池、改写归 MP-5 插件。

---

## §十一 M-3 已交付内容速查 (Mapper 代理)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| 注册时序: 先 put 后 parse + 失败回滚 | MapperRegistry.java:60-80 |
| 工厂: methodCache 跨会话共享 + 每会话新代理 | MapperProxyFactory.java:35-56 |
| invoke 分派: Object 方法直通 / cachedInvoker 缓存 | MapperProxy.java:81-112 |
| default 方法 MethodHandle: JDK9 privateLookupIn / JDK8 Lookup 反射 | MapperProxy.java:54-78,114-126 |
| execute 六分支 + rowCountResult + primitive null 检查 | MapperMethod.java:57-121 |
| SqlCommand: statementId 规则 + 经典报错 + 父接口递归 | MapperMethod.java:222-268 |
| MethodSignature: 泛型返回 + 特殊参唯一索引 | MapperMethod.java:284-302,355-368 |
| ParamNameResolver 三态 + param1..N + ParamMap 严格 | ParamNameResolver.java:40-146; MapperMethod.java:203-215 |
| 注解装配: 同名 XML 优先 + 延迟解析 | MapperAnnotationBuilder.java:101-127 |

### M-3 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| e357f36a6 | 2010-05 | MapperProxy 初始版 — 代理机制远古定型 |
| d5fd43399 | 2016-05 | **"Refactoring parameter naming rule"** — ParamNameResolver 独立成类 + param1..N 通用名引入 |
| 961b002b7 | 2019-08 | **JDK9 default 方法兼容** — privateLookupIn 避免非法反射访问 |

### M-3 六层深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | harness 迭代 | 重复注册测试未 try-catch 崩溃; Object 方法 invoke 需抛 Exception | 修复 harness, 15/15 PASS |
| 2 | 通过项 | 行号全部 grep 验证; SELECT 五分支顺序 (void+Handler 最先) 逐字对照; 命名三态/paramN 不覆盖逻辑/父接口递归条件 验证无矛盾 | 记录 |

### M-3 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 3 | **技术声明错误** | 大纲 §7 "canHaveStatement 过滤(非 default/static/Object 方法)" — 实为 **`!method.isBridge() && !method.isDefault()`**(L140-143, issue#237), static 未被排除(无注解 static 方法在 parseStatement 内自然跳过) | 大纲+KP 双修正 |
| 4 | **行号** | MapperAnnotationBuilder.parse 写 L101-127 — 实为 **L114**(parse L114/canHaveStatement L140-143/loadXmlResource L145) | KP §0.8/01 表+大纲三处修正 |
| 5 | 机制补充 | loadXmlResource 只提"同名 XML 优先" — 补 **"namespace:"+类型名标志防重复加载**(L145-148, Spring 场景)+**双路径资源查找**(#1347) | 大纲 §7 补强 |
| 6 | 通过项 | ParamNameUtil=Parameter::getName(-parameters 编译依赖) 实证; MapperMethodParamTest 三用例 (@Param 多参/HashMap 单参/@Param 单参) 与三态语义一致; 维度1 桥 OUT→M-5 ✅; 锚点 20≥8 ✅; 负面空间 4 条 ✅ | 记录备查 |

### M-3 负面空间 (07 维度5)

不做 AOP 增强 (拦截在 Executor 层 M-4) / 单接口代理 / 懒绑定 (错误仅使用时报)。

## §十二 逐篇深度 REVIEW (2026-08-13 第三轮 — 内容优先原则执行)

> **背景**: 用户指令"禁止限制自己的发挥" — 行号限制(🔴 39-69/🟡 35-49)废弃为软参考, 09 方法论新增反模式 7/8。M-7 恢复内容完整版 (76 行)。

| # | 域 | 类型 | 问题 | 修复 |
|:--:|:--|:--|:--|:--|
| 1 | M-7 | **机制遗漏** | TransactionalCache.getObject 的 **clearOnCommit 分支**(L73-75, issue#146 — 本事务 clear 后读不到旧缓存强制查库)未写 | 补入 §5 getObject 三态 |
| 2 | M-7 | **行号+时序** | ①Cache.removeObject javadoc 写 L43-60 — 实为 **L60-76**(方法 L76) ②数据流 "tcm.putObject(pending+releaseLock)" — **releaseLock 在 commit 的 flushPendingEntries 阶段**(BlockingCache.putObject finally L59-63), putObject 只入 pending 无锁释放 | 双修正 (行号+时序) |
| 3 | M-2 | **机制省略** | wrapCollection 只提名字未展开 — 实为 `ParamNameResolver.wrapToMapIfCollection`: Collection→{"collection","list"}, 数组→{"array"}, 这是 **M-6 foreach collection="list" 取值的依据** | §1 补机制+M-6 连接 |
| 4 | M-1 | 通过项 | 11 元素定序与 14 调用点表述一致; settings 双写默认值一致; 无新问题 | 记录 |
| 5 | M-6 | 通过项 | Handler 注册表/when 复用/OGNL 缓存/__frch_ 均验证无矛盾 | 记录 |

**M-7 KP 同步**: 上述 2 处修复对应 KP §05 q5 表述一致 (KP 未写 releaseLock 细节, 无污染)。

**全量状态**: M-1(66 行)/M-2(69)/M-6(69)/M-7(76) 全部内容优先, 五要素全等, 闭环 header 同步, 负面空间齐备。
