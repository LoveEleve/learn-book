# MP-2 表元数据解析 — 注解→TableInfo 管线 (SQL 自动注入的地基)

> 前置: [[MyBatis 内核: MapperBuilderAssistant/Reflector (导航)]] | 复用: [[S-2-auto-config]] (Configuration 生命周期) | 对照: [[D-2-filter-chain]] (SPI 扩展点设计) | 引出: [[MP-1-sql-injector]] [[MP-3-lambda-wrapper]]
> 🔴 Deep | 8 KP | [模式: 缓存 + 模板方法 + 策略 + SPI]
> Pass 2 闭环: q1(双 Configuration 重初始化) q2(组合注解 SPI) q3(主键三路径) q4(表名五步管线) q5(列名推导+AS 兜底) q6(FieldStrategy+fill 豁免) q7(逻辑删除双路径) q8(元数据 fail-fast) q9(resultMap 三态) q10(读取路径+字段继承合并)

**读者处境**: 你只在实体上写 `@TableName("user")` 和 `private Long id`, 为什么 `selectList` 就能拼出正确的列名和 WHERE?实体从"Java 对象"变成"表模型"发生在哪一刻?这篇拆 TableInfoHelper 把 Class→TableInfo 的解析管线 — MP 一切 SQL 自动生成的元数据源头。

### 1. 入口与缓存 — initTableInfo 双缓存 + 多 Configuration 防御

场景: 多数据源项目里同一实体类为何解析两次?每次查询都反射解析吗?
源码路径:
- `TableInfoHelper.java:163` — `synchronized initTableInfo(builderAssistant, clazz)`(静态锁 Class 串行化); `TABLE_INFO_CACHE.get(clazz)`(L164) → **`oldConfiguration.equals(configuration)` 比较**(L168) → 不同则私有版重解析(L170)
- `TableInfoHelper.java:77,82,203-204` — 双 ConcurrentHashMap: Class→TableInfo + tableName→TableInfo, put 覆盖
- `TableInfoHelper.java:97-118` — 读取路径(q10): 过滤 primitive/SimpleType/interface→null(防 Mapper 接口误查); `getUserClass` 去代理类(L102); 未命中沿父类链查找并**缓存移动**到子类 key(L109-116)
关键设计: 缓存以 Class 为键, 但 TableInfo 持有 Configuration 引用(resultMap 注入/Reflector 均绑定)— 多 SqlSessionFactory/热重载须重建; **边界**: TABLE_NAME_INFO_CACHE 只 put 不清理, 表名变更旧条目残留。[模式: 缓存]
数据流: `AbstractSqlInjector.inspectInject`(AbstractSqlInjector.java:50, MP-1 导航) → initTableInfo → 命中且 Configuration 同 → 直接返回; 否则全量解析 → 双缓存 put → 返回。

### 2. 表名解析 — 注解优先 + 五步转换管线

场景: `@TableName("xxx")` + 全局 `table-prefix: ttt_`, 表名是 ttt_xxx 还是 xxx?
源码路径:
- `TableInfoHelper.java:221,225` — `initTableName`; `annotationHandler.getAnnotation(clazz, TableName.class)`(L225); value 非空→表名=value 且 `!keepGlobalPrefix` → 前缀豁免(L233-238); schema/resultMap/autoResultMap 落表, excludeProperty 返回(L242-250)
- `TableInfoHelper.java:255-270` — 装配序: `tablePrefix+name`(L257-259) → `String.format(tableFormat, name)`(L262-265) → `schema+"."+name`(L268-270)
- `TableInfoHelper.java:288-302` — 无注解: SimpleName → `camelToUnderline`(L291-293, tableUnderline 默认 true)→capitalMode 大写/否则 firstToLowerCase(L295-300); `MybatisConfiguration.java:92` mapUnderscoreToCamelCase=true(原生 MyBatis 默认 false)
关键设计: 显式表名跳过命名管线但默认不加全局前缀 — keepGlobalPrefix=true 才叠加(测试 testTableNamePrefix "xxx" vs testTableNamePrefix2 "ttt_xxx"); schema 在最外层。[模式: 策略链 + 显式豁免]
数据流: UserDetail → camelToUnderline "user_detail" → firstToLowerCase → 前缀 "mp_" → "mp_user_detail" → format/schema 可选 → setTableName。

### 3. 主键与字段解析 — 三路径降级 + 组合注解 SPI + AS 兜底

场景: 实体没有 @TableId 也能 CRUD, 凭什么?`@MyTable` 元注解 MP 认吗?列 `name` 属性 `userName` 怎么办?
源码路径:
- `TableInfoHelper.java:335-347,529-558` — 主键三路径(q3): ①类级预检 existTableId(L321) 后有 @TableId → `initTableIdWithAnnotation`(L342, 多个抛 MybatisPlusException L338-340); ②无注解时 `initTableIdWithoutAnnotation` 仅 `"id".equalsIgnoreCase(property)`(L532) 猜中; ③全无 → warn + havePK=false(L374-377, TableInfo.java:220); type 策略: 注解 != NONE 优先否则全局 ASSIGN_ID(L472-517, GlobalConfig.java:108)
- `TableInfoHelper.java:607-615` + `ReflectionKit.java:127-155` — 字段发现(q10): `getAllFields` 过滤 `@TableField(exist=false)`; 子类 declaredFields+父类链合并, `excludeOverrideSuperField` 子类同名优先, 过滤 static/transient
- `AnnotationHandler.java:38-40,62-64` + `AnnotationUtils.java:33-49,61-72` — 注解读取 SPI(q2): 默认递归穿透元注解(`annotationType().getDeclaredAnnotations()` L65)+HashSet 防环(L61); 类级沿 superclass 上溯(L44-48); 用户可整体覆写
- `TableFieldInfo.java:265-300,354-380` + `TableInfoHelper.java:570-582` — 列名推导+AS 兜底(q5): column=注解 value 优先; 空则 property 按 camelToUnderline+capitalMode 大写+columnFormat; **AS 三条件**: 注解指定 property / resultMap==null 且 !autoInitResultMap 且 checkRelated=true("true=不合规", 列去下划线全大写==属性才免 AS)
关键设计: 主键三级降级(注解→猜 id→warn)不致命 — 无主键表仍可 CRUD, xxById 族不注入(MP-1 依 havePK, 导航); MyBatis 自动封装前提是"列名可转属性名", checkRelated 失败则显式 `AS 属性` 兜底, 有 resultMap 时互斥不再 AS。[模式: 降级链 + SPI + 约定优于配置]
数据流: `@TableId Long realId` → 注解解析 → type=NONE 回落全局 → key 落表; `@TableField("user_name") String userName` → 去下划线 USERNAME==USERNAME 免 AS; `@TableField("name") String userName` → `name AS userName`。

### 4. SQL 片段生成与特殊标记 — FieldStrategy + fill 豁免 + fail-fast

场景: 为什么 `updateById` 传 null 不进 SET?`fill=INSERT` 字段 null 也拼?@Version 标两个字段会怎样?
源码路径:
- `TableFieldInfo.java:596-608` — `convertIf`(q6): NEVER→null(字段整体消失, 下游 filter(Objects::nonNull)); isPrimitive||IGNORED||ALWAYS→直拼; NOT_EMPTY&&CharSequence→`!=null and !=''`; 默认→`!=null`; 策略选择 `chooseFieldStrategy`(L302-304,318-320): 注解 != DEFAULT 用注解否则全局(默认 NOT_NULL, GlobalConfig.java:194,200,217)
- `TableFieldInfo.java:455-462,486-493,512-530,542-548` + `TableInfo.java:393-410,419-428` — insert/set/where 片段: withInsertFill/withUpdateFill 跳过 if 直拼(必有值断言, MP-7 导航); set 支持 `update="%s+1"` 模板(L516-517); where 默认 `AND column=#{...}`; getAllSqlWhere/getAllSqlSet 逐字段拼接
- `TableFieldInfo.java:406-430` — 逻辑删除双路径(q7): @TableLogic value/delval 优先, 空则全局 "0"/"1"(GlobalConfig.java:184-188); **类中无任何 @TableLogic** 才用全局 logicDeleteField 属性名兜底
- `TableFieldInfo.java:215,341,389-398` + `TableInfo.java:493-518,437-470` — 标记与校验(q8): version=isAnnotationPresent(Version) 双构造检测; @OrderBy→ASC/DESC+sort 入 orderByFields; setFieldList AtomicInteger 计数→`Assert.isTrue(count<=1)` 多逻辑删除/多版本启动即抛; getLogicDeleteSql where 拼未删除值/`IS NULL`, 非 where 拼删除值, 字符型带引号(L461)
关键设计: 字段拼接是"策略驱动的 OGNL if 包裹" — null 安全性内建在生成期而非执行期; fill 豁免 if 是"必有值"断言; 特殊字段沉淀为 withLogicDelete/withVersion/withInsertFill 布尔面 — MP-6/7/8 据此介入(导航); 数量超限 fail-fast 防歧义。[模式: 策略 + 代码生成 + 启动校验]
数据流: updateById → getAllSqlSet → 逐字段 getSqlSet: `column=#{el},` → update 模板 format → withUpdateFill? 直拼 : convertIf(updateStrategy); @TableLogic deleted → 注解值(无则全局 0/1) → 计数=1 → DELETE 生成时 getLogicDeleteSql 拼 WHERE。

### 5. resultMap 三态 + 缓存联动 — 显式/自动构建/AS 兜底

场景: `@TableName(autoResultMap=true)` 有什么用?指定 typeHandler 为何不生效?
源码路径:
- `TableInfoHelper.java:246-250,475-491` — `initResultMapIfNeed`: autoInitResultMap && resultMap==null → 构建 id=`namespace.mp_EntitySimpleName`(L477), 主键带 `ResultFlag.ID`(L480-482), 逐字段 getResultMapping(L484-485), `configuration.addResultMap`(L488)
- `TableFieldInfo.java:556-576` — getResultMapping: jdbcType/typeHandler 落 ResultMapping; **IJsonTypeHandler 每次 new**(L565-567) 其余 registry 复用
- `TableInfo.java:263-273` — `chooseSelect`: 主键+字段 sqlSelect 逗号拼接, resultMap 决定是否 AS
- `TableInfoHelper.java:207` + `LambdaUtils.java:86-110` — 缓存联动: `installCache` 把 **property 全大写**(formatKey 大写转换 L77-79, 挂 key/字段 L101,107)→ColumnCache(column, sqlSelect, mapping) 装入 COLUMN_CACHE_MAP, 供 MP-3 Lambda 构造器取列名
关键设计: 三态: 显式(完全交给 XML)→自动构建(注解的 jdbcType/typeHandler 翻译成 ResultMapping 注入 MyBatis Configuration)→无(靠 checkRelated AS 兜底)。官方注释: typeHandler/jdbcType 只生效于 MP 自动注入方法且建议配合 autoResultMap — 自动注入 SQL 用 #{el} 引用参数需类型元数据。[模式: 三级降级 + 双缓存联动]
数据流: autoResultMap=true → initResultMapIfNeed: 主键 ID 映射+字段映射 → addResultMap → resultMap=新 id → chooseSelect 不加 AS; 同表解析后 LambdaUtils.installCache 预热列缓存。

### 结尾桥 — MP-1 SQL 自动注入

`AbstractSqlInjector.inspectInject`(AbstractSqlInjector.java:50) 拿到的 TableInfo(havePK/keyProperty/idType/fieldList/逻辑删除·版本·填充标记)正是 MP-1 方法生成的输入 — 下一篇拆 DefaultSqlInjector.getMethodList 如何按元数据决定注入哪些方法、AbstractMethod 模板如何产出 MappedStatement。

→ 引出: [[MP-1-sql-injector]] [[MP-3-lambda-wrapper]]
