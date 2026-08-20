# MP-2 表元数据解析 — TableInfoHelper 注解→表模型

> 项目: MyBatis-Plus | 🔴 Deep / 1 篇 | TableInfoHelper(646行)+TableInfo+TableFieldInfo+注解族(@TableName/@TableId/@TableField/@TableLogic)
> 基线: MP-PLAN MP-2 (元数据) — 前置: **annotation 模块(薄载体) + MyBatis MapperBuilderAssistant(导航)** — 展开注解→TableInfo 解析管线

---

## §0.8

- 🔴 Deep，1篇 — 入口(**initTableInfo(L163) 两级缓存+Configuration 变更重初始化[L166-173: 缓存命中但 Configuration 不同→多数据源场景重解析]**) → 初始化管线(**initTableInfo 私有版 L185-209: PostInitTableInfoHandler.creteTableInfo[可插拔]→initTableName→initTableFields→initResultMapIfNeed→双缓存 TABLE_INFO_CACHE+TABLE_NAME_INFO_CACHE→LambdaUtils.installCache[MP-3 衔接]**) → 表名解析(**initTableName L221-302: @TableName 注解优先[value/keepGlobalPrefix/resultMap/autoResultMap/excludeProperty] → 无注解走 dbConfig[tablePrefix 前缀追加 L257-259/tableFormat 格式化/下划线转换 camelToUnderline L291-293/大小写] → schema 拼接 L268-270**) → 字段解析(**initTableFields L313-378: 主键 @TableId[多个抛异常 L338-340]/无注解按属性名 "id" 猜主键 initTableIdWithoutAnnotation[猜中后套全局 IdType] → 无主键 warn L375-377 → @TableField 有/无注解双构造分支→TableFieldInfo → OrderBy 排序字段**)
- 设计模式: [模式: 缓存]—两级+Configuration 键; [模式: 模板方法]—PostInitTableInfoHandler 钩子; [模式: 策略]—主键/表名解析可配

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| TableInfoHelper.java:163,166,170 | 入口 | **initTableInfo(L163): TABLE_INFO_CACHE.get(clazz)→缓存命中但 Configuration 不同→重初始化(L168-170)** — 多数据源隔离 | High |
| TableInfoHelper.java:185,189,202 | 管线 | **私有 initTableInfo(L185): PostInitTableInfoHandler.creteTableInfo(L189)→initTableName(L193)→initTableFields(L198)→initResultMapIfNeed(L201)→postTableInfo(L202)** | High |
| TableInfoHelper.java:203,204,207 | 缓存 | **双缓存: TABLE_INFO_CACHE(clazz→)(L203)+TABLE_NAME_INFO_CACHE(tableName→)(L204)+LambdaUtils.installCache(L207, MP-3 列缓存衔接)** | High |
| TableInfoHelper.java:221,225,233 | 表名 | **initTableName(L221): AnnotationHandler.getAnnotation(clazz, TableName)(L225)→注解 value 优先(L233-238)/无注解 initTableNameWithDbConfig(L240)** | High |
| TableInfoHelper.java:255,262,268 | 表名装配 | **前缀 tablePrefix 追加(L257-259)→tableFormat 格式化(L262-265)→schema 拼接(L268-270)** | High |
| TableInfoHelper.java:288,291,295 | 默认表名 | **initTableNameWithDbConfig(L288): 下划线 camelToUnderline(L291-293)→capitalMode 大写/首字母小写(L295-300)** | High |
| TableInfoHelper.java:313,335,346 | 主键 | **initTableFields(L313): @TableId 存在则注解解析(L336-343, 多个抛异常 L338-340); 否则 initTableIdWithoutAnnotation 猜主键(L346)** | High |
| TableInfoHelper.java:355,359,366 | 字段 | **@TableField 有注解→TableFieldInfo 构造 A(L359)/无注解→构造 B(L366)** — 双构造分支 | High |
| TableInfoHelper.java:374,375 | 降级 | **无主键 warn: "Can not find table primary key"(L375-377)** — 影响 MP-1 的 xxById 注入 | High |
| AnnotationHandler.java:38,62 + AnnotationUtils.java:29-46,62-70 | 注解 SPI | **组合注解递归解析**(@MyId→@TableId): 穿透元注解+HashSet 防环+父类上溯 — 可整体覆写 | High |
| TableFieldInfo.java:265-300,354-380 + TableInfoHelper.java:570-582 | 列名与 AS | **column 推导管线 + checkRelated 判定**: 列名不可转属性名时 sqlSelect 追加 `AS 属性` 兜底(有 resultMap 互斥) | High |
| TableFieldInfo.java:596-608 | 策略 | **FieldStrategy convertIf**: NEVER→null/IGNORED·ALWAYS·primitive→直拼/NOT_EMPTY→双非空/默认→非空; fill 字段豁免 if | High |
| TableInfoHelper.java:246-250,475-491 + TableFieldInfo.java:556-576 | resultMap 三态 | 显式 resultMap / autoResultMap 自动构建(ResultFlag.ID+jdbcType·typeHandler 翻译, IJsonTypeHandler 每次 new) / 无则 AS 兜底 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 元数据解析是单管线机制 — 1篇 (~60行) 按"入口缓存→管线→表名→字段"展开; 无主键降级衔接 MP-1(已按拓扑后写, 用导航), Lambda 缓存衔接 MP-3(导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 两级缓存+Configuration 变更重初始化 | 🔴 | **为什么🔴**: 多数据源隔离 |
| P1-2 | 初始化管线 (PostInitTableInfoHandler 钩子) | 🔴 | **为什么🔴**: 可插拔架构 |
| P1-3 | 表名解析优先级链 (注解→全局配置→转换) | 🔴 | **为什么🔴**: 命名策略 |
| P1-4 | 主键解析 (@TableId/猜主键/无主键降级) | 🔴 | **为什么🔴**: 影响 xxById 注入 |
| P1-5 | 字段解析 (@TableField 双构造分支) | 🔴 | **为什么🔴**: 字段模型 |
| P2-1 | 双缓存+Lambda 缓存衔接 | 🟡 | **为什么🟡**: 性能与 MP-3 衔接 |
| P2-2 | 可插拔 AnnotationHandler | 🟡 | **为什么🟡**: 扩展点 |
| P3-1 | 与 MyBatis MapperBuilderAssistant 协作 (导航) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **入口与缓存** | 🔴 | 性能与隔离 |
| B | **初始化管线** | 🔴 | 骨架 |
| C | **表名+主键+字段解析** | 🔴 | 核心产出 |
| D | **扩展点与衔接** | 🟡 | 可插拔 |

> **Cluster A (§1)**: initTableInfo 入口+两级缓存+Configuration 重初始化
> **Cluster B (§2)**: 私有 initTableInfo 管线+PostInitTableInfoHandler
> **Cluster C (§3)**: initTableName 优先级链 + initTableFields 主键/字段
> **Cluster D (§4)**: AnnotationHandler 扩展+Lambda 缓存衔接 MP-3

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 入口双 Configuration 重初始化 | 缓存命中后比较 `oldConfiguration.equals(configuration)`, 不同(多 SqlSessionFactory/热重载)则走私有 initTableInfo 全量重解析并 put 覆盖 — 缓存以 Class 为键但 TableInfo 绑定 Configuration, 故需重建; **边界**: TABLE_NAME_INFO_CACHE 只 put 不清理旧表名 | TableInfoHelper.java:163-175, 203-204 |
| q2 | 组合注解 SPI | AnnotationHandler 默认实现递归 `annotationType().getDeclaredAnnotations()` 穿透元注解(@MyTable→@TableName)+HashSet 防环; 类级沿 superclass 链上溯; 用户可覆写(如换 Spring 版注解语义) | AnnotationHandler.java:38-40; AnnotationUtils.java:33-49, 61-72 |
| q3 | 主键三路径 | ①@TableId 注解(多个抛 MybatisPlusException L338-340)→②无注解时属性名 `id`(equalsIgnoreCase) 猜主键→③无则 warn "Can not find table primary key" + havePK=false(MP-1 据此降级不注入 xxById); IdType 策略: 注解 type != NONE 优先, 否则回落全局 ASSIGN_ID | TableInfoHelper.java:335-347, 472-517, 529-558; 测试 testMoreTableId/testPriorityTableId |
| q4 | 表名五步管线 | 无 @TableName: SimpleName→(tableUnderline 默认 true)camelToUnderline→(!capitalMode)firstToLowerCase→(tablePrefix)前置→(tableFormat)String.format→(schema)schema.表; @TableName.value 显式时跳过转换管线但默认**不加全局前缀**(keepGlobalPrefix=true 才加) | TableInfoHelper.java:255-270, 288-302; 测试 testTableNamePrefix/testTableNamePrefix2 |
| q5 | 字段列名推导 + AS 兜底 | column 推导: 注解 value 优先→property 按 underCamel 驼峰转下划线→capitalMode 大写→columnFormat(有注解需 keepGlobalFormat); sqlSelect 追加 `AS 属性` 三条件: ①注解指定 property ②resultMap==null 且 !autoInitResultMap 且 checkRelated=true; checkRelated 语义 "true=不合规" — underCamel 时列去下划线后与属性全大写相等才免 AS | TableFieldInfo.java:265-300, 354-380; TableInfoHelper.java:570-582 |
| q6 | FieldStrategy 三策略 + fill 豁免 | convertIf: NEVER→null(字段整体从 SQL 消失, 下游 filter(Objects::nonNull)); isPrimitive/IGNORED/ALWAYS→直拼; NOT_EMPTY&&CharSequence→非空双判断; 默认→!=null; 注解>全局(DEFAULT 回落, 全局默认 NOT_NULL); withInsertFill/withUpdateFill 字段跳过 if 包裹直拼(断言必有值) | TableFieldInfo.java:455-462, 512-530, 596-608; GlobalConfig.java:194-217 |
| q7 | 逻辑删除双路径 | ①字段 @TableLogic: value/delval 注解优先, 否则全局 logicNotDeleteValue="0"/logicDeleteValue="1"; ②类中完全无 @TableLogic(existTableLogic=false) 时全局 logicDeleteField 属性名匹配兜底 — 有注解则全局字段名不再生效(防混淆) | TableFieldInfo.java:406-430; 测试 testLogic |
| q8 | 元数据合法性 fail-fast | 多 @TableId 解析期直接抛(initTableFields L338-340); @TableLogic>1 或 @Version>1 在 setFieldList 期 Assert 抛(AtomicInteger 计数) — 非法配置启动即失败 | TableInfoHelper.java:338-340; TableInfo.java:493-518; 测试 testVersion |
| q9 | resultMap 三态 | ①@TableName.resultMap 显式指定(优先级最高)→②autoResultMap=true 自动构建(initResultMapIfNeed: 主键 ResultFlag.ID + 字段 getResultMapping 含 jdbcType/typeHandler/IJsonTypeHandler 每实例化, 注入 configuration, 名=namespace.mp_EntitySimpleName)→③两者皆无则 null, 靠 checkRelated AS 兜底(二者互斥: 有 resultMap 不加 AS) | TableInfoHelper.java:246-250, 475-491; TableFieldInfo.java:556-576; 测试 testTableAutoResultMap |
| q10 | 读取路径与字段继承合并 | getTableInfo 过滤 primitive/SimpleType/interface→null(防 Mapper 接口误查); getUserClass 去代理类; 缓存未命中沿父类链查找并**缓存移动**到子类 key; 字段发现 ReflectionKit.getFieldList: 子类字段+父类链 declaredFields 合并, excludeOverrideSuperField 子类同名优先, 过滤 static/transient | TableInfoHelper.java:97-118, 607-615; ReflectionKit.java:127-155; 测试 testExcludeProperty |

→ 引出 MP-1: SQL 自动注入 — DefaultSqlInjector.getMethodList 依据 TableInfo(havePK/字段列表/IdType) 生成 12 种内置方法 MappedStatement
