# M-5 参数/结果映射 — 取参四路 + 结果对象创建 + 自动/显式双映射 + 嵌套聚合

> 前置: [[M-1-configuration]] (TypeHandlerRegistry/ReflectorFactory) | 复用: [[M-2-executor]] (ResultSetHandler 工厂/StatementHandler) | 对照: [[MP-2-metadata]] (TableInfo 列名映射 vs ResultMap) | 引出: [[M-4-plugin]]
> 🔴 Deep | 9 KP | [模式: 双路径分流+反射工厂+缓存+延迟加载+策略]
> Pass 2 闭环: q1(取参四路) q2(null 绑定兜底) q3(多结果集) q4(分流守卫) q5(对象创建链) q6(三级自动判定) q7(显式映射) q8(自动映射+缓存) q9(嵌套聚合) q10(构造器自动映射) q11(列元数据)

**读者处境**: `selectList` 返回的 List<User> 里, 每一行怎么变成 User 对象?为什么数据库列 user_name 能自动映射到 userName?为什么嵌套的 one-to-many 结果能聚合成一个父对象?为什么无默认构造函数的类也能映射?为什么 #{id} 为 null 时预编译不报错?这篇拆结果映射的两段: 参数绑定(入)与结果映射(出)。

### 1. 参数绑定 — DefaultParameterHandler 取参四路 + null 兜底

场景: #{id} 的值从哪取?foreach 动态参数和普通参数有什么不同?null 值怎么绑定?
源码路径:
- `DefaultParameterHandler.java:62-99` — setParameters 逐 ParameterMapping(q1): 四路取值 ①`boundSql.hasAdditionalParameter(propertyName)`(issue#448, **foreach 的 __frch_ 附加参数优先**) ②parameterObject==null→null ③`hasTypeHandler(parameterObject.getClass())`→裸值(单参直接绑定) ④`metaObject.getValue(propertyName)`(对象属性)
- `DefaultParameterHandler.java:84-93` — 绑定(q2): `typeHandler.setParameter(ps, i, value, jdbcType)`; **value null 且 jdbcType null → `configuration.getJdbcTypeForNull()`**(默认 OTHER)
关键设计: 取参四路(q1): 与 M-2 createCacheKey 四路同构+additional 首查 — 动态 SQL 产生的附加参数(foreach item)优先级最高; null 兜底(q2): 驱动对 null 需明确 JDBC 类型(OTHER 兼容多数库)。[模式: 取值回退链+类型兜底]
数据流: PreparedStatementHandler.parameterize → setParameters → 逐 mapping: 四路取 value → typeHandler.setParameter。

### 2. 结果集入口 — handleResultSets 多结果集双循环 + 分流

场景: 一个存储过程返回多个 ResultSet 怎么处理?Cursor 结果有限制吗?
源码路径:
- `DefaultResultSetHandler.java:188-224` — handleResultSets(q3): 双循环 ①`resultMapCount` 顺序(每 resultMap 对应一个 ResultSet, L200-206) ②`resultSets` 名循环(**nextResultMaps 按名找父映射**, 嵌套关联 L208-221); `collapseSingleResultList`(单结果简化 L223)
- `DefaultResultSetHandler.java:226-242` — handleCursorResultSets: **单 resultMap 限制**(多映射抛 "Cursor results cannot be mapped to multiple resultMaps")
- `DefaultResultSetHandler.java:330-356` — handleRowValues 分流(q4): hasNestedResultMaps→嵌套路径(**ensureNoRowBounds+checkResultHandler 安全守卫**: 嵌套映射配 RowBounds/自定义 ResultHandler 破坏聚合, safeXxxEnabled 时抛 L335-356)/否则简单路径
关键设计: 多结果集(q3): resultMap 顺序对应 + 名称关联两机制; 分流守卫(q4): 嵌套聚合的完整性依赖全量行, 分页/自定义 handler 会打破 — 默认安全拒绝(可配置绕过)。[模式: 双循环+安全守卫]
数据流: handleResultSets → 循环 handleResultSet → handleRowValues → 嵌套? 嵌套行处理 : 简单行处理。

### 3. 简单行遍历 — skipRows + discriminator + getRowValue

场景: RowBounds 分页在 ResultSet 层怎么实现?discriminator 什么时候生效?
源码路径:
- `DefaultResultSetHandler.java:359-376` — 简单循环: `skipRows`(L387-402: **FORWARD_ONLY 逐行跳过 / 否则 absolute 定位**)→`shouldProcessMoreRows`(resultCount < limit)→`resolveDiscriminatedResultMap`(discriminator 动态选 resultMap)→`getRowValue`→`storeObject`(parentMapping→`linkToParents` 多结果集父关联 L601-620)
- `DefaultResultSetHandler.java:971` — resolveDiscriminatedResultMap: 按 discriminator 列值选结果映射
关键设计: 行级分页(q3 补充): 非 FORWARD_ONLY 用 JDBC absolute 跳行(高效), FORWARD_ONLY 逐行 next 跳过; discriminator 是运行期 resultMap 切换 — 同一列值决定不同映射结构。[模式: 游标分页+运行期选择]
数据流: while(next) → skipRows(仅首行前) → 每行: discriminator 选 map → getRowValue 建对象 → storeObject。

### 4. 对象创建 — createResultObject 四分支 + 延迟代理

场景: 无默认构造函数的类怎么实例化?带 lazy 的嵌套查询返回什么?
源码路径:
- `DefaultResultSetHandler.java:654-700` — createResultObject(q5): useConstructorMappings 重置→构造器映射参数收集(L656-662)→**nestedQuery && lazy → `proxyFactory.createProxy` 延迟代理**(L665-674, issue#109/#149); 分支链(L675-700): ①`hasTypeHandlerForResultObject`→`createPrimitiveResultObject`(L849, Map/List/标量专用创建) ②constructorMappings→`createParameterizedResultObject` ③接口/默认构造→`objectFactory.create` ④无默认构造→`createByConstructorSignature`(自动构造器); 全无→抛 "Do not know how to create an instance"
- `DefaultResultSetHandler.java:729-757` — 构造器四选一(q10): 单构造器直接/`@AutomapConstructor`(多个抛)/argNameBased 开启必须 @AutomapConstructor/按 JDBC 类型匹配(findUsableConstructorByArgTypes)
关键设计: 对象创建链(q5): 从"最显式"到"最自动" — 类型处理器(标量)→构造器映射→默认构造→自动构造器匹配; 延迟加载: lazy 嵌套查询不立即执行, 返回代理对象, 首次访问属性触发子查询(ResultLoaderMap)。[模式: 反射工厂+降级链+延迟代理]
数据流: createResultObject → 原始值? 直返 : (构造器映射? 参数化创建 : (默认构造? create : 自动构造器)) → 延迟代理包装。

### 5. 双映射路径 — 自动 vs 显式 + 三级自动判定

场景: resultMap 里没写的列能自动映射吗?自动映射的开关层级是什么?列名与属性名驼峰怎么对齐?
源码路径:
- `DefaultResultSetHandler.java:466-478` — shouldApplyAutomaticMappings(q6): ①`resultMap.getAutoMapping()` 显式属性 ②嵌套→仅 `AutoMappingBehavior.FULL` ③简单→非 NONE(PARTIAL/FULL)
- `DefaultResultSetHandler.java:481-515` — applyPropertyMappings(q7): 显式 ResultMapping → **mappedColumnNames 存在性检查**(column 不在结果集则跳过)→三路取值(nestedQuery 子查询/lazy 代理 / resultSet DEFERRED 挂起 / typeHandler.getResult)→**callSettersOnNulls && 非 primitive 才 set null**(issue#377, 默认 false 不调 setter)
- `DefaultResultSetHandler.java:580-645` — createAutomaticMappings(q8): 未映射列→columnPrefix 过滤→`findProperty`(**mapUnderscoreToCamelCase 时 name.replace("_","")+忽略大小写匹配**, MetaClass.java:56-64)→hasSetter→hasTypeHandler 校验→`UnMappedColumnAutoMapping` **按 (resultMapId:columnPrefix) 缓存**(autoMappingsCache L607)→无对应属性→`AutoMappingUnknownColumnBehavior` 三态(NONE 忽略/WARNING 日志/FAILING 抛)
关键设计: 双路径(q6/q7/q8): 显式映射(ResultMapping 声明)+自动映射(列名→属性名推断)并存互补; 三级自动判定: 显式属性 > 嵌套场景 FULL > 简单场景非 NONE; 自动映射缓存避免每行重复推断(列名→属性解析一次)。[模式: 声明式+约定式双轨+缓存]
数据流: getRowValue → shouldApplyAutomaticMappings? applyAutomaticMappings(缓存+逐列 set) → applyPropertyMappings(显式逐映射) → foundValues 汇总。

### 6. 嵌套映射 — partialObject 聚合 + 循环引用保护 + 列元数据

场景: one-to-many 的一父多子怎么聚合成一个对象?自引用嵌套会不会死循环?
源码路径:
- `DefaultResultSetHandler.java:440-462` — 嵌套行 getRowValue(q9): 同 combinedKey 命中→**partialObject 复用**(一父多子聚合); 新建→createResultObject→**putAncestor/ancestorObjects 循环引用保护**→applyNestedResultMappings→nestedResultObjects.put(combinedKey)
- `ResultSetWrapper.java:46-196` — 列元数据(q11): 构造读 ResultSetMetaData: **useColumnLabel ? label : name**(L59); mapped/unmapped 列名按 resultMap+columnPrefix 缓存; typeHandler 按 (propertyType, jdbcType) 查注册表
关键设计: 嵌套聚合(q9): combinedKey(父行主键)作为聚合键, 同 key 行合并到 partialObject — 一父多子只建一个父; ancestorObjects 记录祖先(自引用场景: 子节点的父已存在则直接关联, 不死循环)。[模式: 分组聚合+祖先栈]
数据流: 嵌套行 → createRowKey(combinedKey) → 命中? partialObject 复用 : 新建+putAncestor → 应用嵌套映射 → 返回。

### 负面空间 — 映射层刻意不做的事

- **不做 N+1 优化**: 嵌套查询默认逐条子查询(lazy 可延迟但仍是 N+1), 批量加载需 ResultHandler 或 join 嵌套结果 — 与 MP-5 分页等插件职责分离
- **不做自动刷新**: autoMapping 缓存/ResultSetWrapper 均构建期一次性, 结果集结构不变
- **不做跨库方言**: 类型映射依赖 TypeHandlerRegistry(M-1 注册), 方言差异由驱动处理

→ 引出: 4 处理器工厂(newParameterHandler/newResultSetHandler)是 M-4 插件机制的拦截点 — Parameter/ResultSet 处理器全部可被 @Intercepts 拦截 → [[M-4-plugin]]
