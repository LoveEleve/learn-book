# M-5 参数/结果映射 — 取参四路 + 结果对象创建 + 自动/显式映射 + 嵌套

> 项目: MyBatis | 🔴 Deep / 1 篇 | DefaultResultSetHandler(1267)+ResultSetWrapper(196)+DefaultParameterHandler(99)+TypeHandlerRegistry(494)+Reflector(488)+MetaObject(143)+ResultMap(261)+ParameterMapping(224)
> 基线: M-PLAN M-5 — 前置: **M-1 (TypeHandlerRegistry/ReflectorFactory) + M-2 (ResultSetHandler 工厂) + M-3 (参数命名)** — 展开 参数绑定→结果集遍历→对象创建→双映射路径→嵌套

---

## §0.8

- 🔴 Deep，1篇 — 参数绑定(**DefaultParameterHandler.setParameters L62-99: 逐 ParameterMapping→四路取值[additionalParameter issue#448>parameterObject null>裸值 hasTypeHandler>metaObject.getValue L72-82]→typeHandler.setParameter+jdbcType 默认[value null 且 jdbcType null→configuration.getJdbcTypeForNull L87]**) → 结果集入口(**DefaultResultSetHandler.handleResultSets L188-224: 多结果集双循环[resultMapCount 顺序 L200-206 / resultSets 名+nextResultMaps 关联 L208-221]→collapseSingleResultList; handleCursorResultSets 单 resultMap 限制 L226-242**) → 行遍历(**handleRowValues 双路径 L330-337[hasNestedResultMaps→嵌套[ensureNoRowBounds L335-343+checkResultHandler L346-356 安全守卫] / 简单]; 简单循环 L359-376: skipRows[FORWARD_ONLY 逐行 vs absolute L387-402]→shouldProcessMoreRows[resultCount<limit]→resolveDiscriminatedResultMap→getRowValue→storeObject[parentMapping→linkToParents 多结果集父关联 L601-620]**) → 对象创建(**createResultObject L654-700: useConstructorMappings 重置→构造器映射参数收集→nestedQuery&&lazy→proxyFactory.createProxy 延迟代理[issue#109/#149 L665-674]; 分支链 L675-700[hasTypeHandlerForResultObject→原始值; constructorMappings→createParameterizedResultObject; 接口/默认构造→objectFactory.create; 否则 createByConstructorSignature 自动构造器[单构造器/@AutomapConstructor/按 JDBC 类型 findUsableConstructorByArgTypes L742-757]→抛 "Do not know how to create an instance" L700]**) → 双映射路径(**shouldApplyAutomaticMappings L466-478: 显式 autoMapping>嵌套 FULL>简单非 NONE; applyPropertyMappings L481-515[显式 ResultMapping→column 存在检查[mappedColumnNames]+getPropertyMappingValue[nestedQuery→getNestedQueryMappingValue/resultSet→addPendingChildRelation(DEFERRED)/普通 typeHandler.getResult]+callSettersOnNulls 判定 issue#377 L503-511]; createAutomaticMappings L580-645[未映射列→columnPrefix 过滤→findProperty 驼峰→hasSetter→hasTypeHandler→UnMappedColumnAutoMapping 缓存 autoMappingsCache→AutoMappingUnknownColumnBehavior 三态]→applyAutomaticMappings 遍历设置 L647-665**) → 嵌套映射(**getRowValue 嵌套版 L440-462: partialObject 复用[同 key 聚合]/新建+putAncestor/ancestorObjects[循环引用保护]→applyNestedResultMappings→nestedResultObjects.put(combinedKey); ResultSetWrapper L46-196[columnNames[useColumnLabel?label:name L59]+mapped/unmapped 列名缓存+typeHandler 按(propertyType,jdbcType)]**) → 构造器自动映射(**findConstructorForAutomapping L729-741: 单构造器/@AutomapConstructor[多个抛]/argNameBased 需 @AutomapConstructor/按类型匹配; applyArgNameBased 按参数名匹配列 L817+applyColumnOrderBased 按列序 L789-800**)
- 设计模式: [模式: 双路径分流+反射工厂+缓存+延迟加载]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DefaultParameterHandler.java:62-99 | 取参 | **四路取值**: additionalParameter(issue#448 优先)>parameterObject null>裸值(hasTypeHandler)>metaObject.getValue — 与 M-2 createCacheKey 一致 | High |
| DefaultParameterHandler.java:63-71 | 绑定 | typeHandler.setParameter; value null 且 jdbcType null→configuration.getJdbcTypeForNull 兜底 | High |
| DefaultResultSetHandler.java:188-224 | 多结果集 | resultMapCount 顺序循环+resultSets 名循环(nextResultMaps)+collapseSingleResultList | High |
| DefaultResultSetHandler.java:330-356 | 分流守卫 | hasNestedResultMaps→嵌套(ensureNoRowBounds/checkResultHandler 安全抛); 简单路径 | High |
| DefaultResultSetHandler.java:359-376 | 简单循环 | skipRows(FORWARD_ONLY 逐行/absolute)→shouldProcessMoreRows(resultCount<limit)→discriminator→getRowValue→storeObject | High |
| DefaultResultSetHandler.java:654-700 | 对象创建 | useConstructorMappings 重置; nestedQuery&&lazy→ProxyFactory 延迟代理; 分支链: 原始值>构造器>接口/默认构造>自动构造器>抛 | High |
| DefaultResultSetHandler.java:466-478 | 自动判定 | 显式 autoMapping>嵌套 FULL>简单非 NONE — 三级 | High |
| DefaultResultSetHandler.java:481-515 | 显式映射 | column 存在检查+三路取值(nestedQuery/resultSet DEFERRED/typeHandler)+callSettersOnNulls(issue#377) | High |
| DefaultResultSetHandler.java:580-645 | 自动映射 | 未映射列→columnPrefix 过滤→findProperty 驼峰→hasTypeHandler→缓存→UnknownColumnBehavior 三态 | High |
| DefaultResultSetHandler.java:440-462 | 嵌套行 | partialObject 复用(同 key 聚合)+putAncestor 循环引用保护+nestedResultObjects 缓存 | High |
| ResultSetWrapper.java:46-196 | 列元数据 | useColumnLabel 列名+映射/未映射列名缓存+typeHandler 按(propertyType,jdbcType) | High |
| DefaultResultSetHandler.java:729-757 | 构造器自动 | 单构造器/@AutomapConstructor(多抛)/argNameBased 约束/按 JDBC 类型匹配 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 参数/结果映射是执行链的收尾两段 — 1篇 (~75行) 按"参数绑定→结果集入口→行遍历分流→对象创建→双映射路径→嵌套映射→构造器自动映射"展开; TypeHandlerRegistry/Reflector 为执行基础(M-1 已建)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 取参四路 + jdbcTypeForNull 兜底 | 🔴 | **为什么🔴**: 参数绑定正确性 |
| P1-2 | 结果集入口双循环 + collapse | 🔴 | **为什么🔴**: 多结果集 |
| P1-3 | 行遍历双路径分流 + 安全守卫 | 🔴 | **为什么🔴**: 简单/嵌套核心 |
| P1-4 | createResultObject 分支链 + 延迟代理 | 🔴 | **为什么🔴**: 对象创建 |
| P1-5 | 显式/自动双映射 + 三级自动判定 | 🔴 | **为什么🔴**: 映射语义 |
| P2-1 | 嵌套映射 (partialObject/putAncestor/缓存) | 🟡 | **为什么🟡**: 复杂场景 |
| P2-2 | 构造器自动映射 (四选一) | 🟡 | **为什么🟡**: 无默认构造 |
| P2-3 | ResultSetWrapper 列元数据缓存 | 🟡 | **为什么🟡**: 性能 |
| P3-1 | 与 M-1 TypeHandlerRegistry/M-2 工厂协作 (导航) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **参数绑定** | 🔴 | 执行入口 |
| B | **结果遍历+对象创建** | 🔴 | 核心流程 |
| C | **映射路径** | 🔴 | 语义面 |
| D | **嵌套/构造器/元数据** | 🟡 | 复杂面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 取参四路 | DefaultParameterHandler.setParameters 逐 ParameterMapping 四路取值: ①boundSql.hasAdditionalParameter(issue#448, foreach 动态参数) ②parameterObject==null ③hasTypeHandler(裸值, 单参直接绑定) ④metaObject.getValue(对象属性) — 与 M-2 createCacheKey 四路一致+additional 首查 | DefaultParameterHandler.java:62-99 |
| q2 | null 绑定兜底 | value==null 且 jdbcType==null → `configuration.getJdbcTypeForNull()`(默认 OTHER) — 驱动对 null 需明确 JDBC 类型 | DefaultParameterHandler.java:63-71 |
| q3 | 多结果集 | handleResultSets 双循环: ①resultMapCount 顺序(每 resultMap 对应一个 ResultSet) ②resultSets 名循环(经 nextResultMaps 按名称找父映射, 嵌套关联); 单结果 collapseSingleResultList 简化 | DefaultResultSetHandler.java:188-224 |
| q4 | 分流守卫 | handleRowValues: hasNestedResultMaps→嵌套路径(**ensureNoRowBounds+checkResultHandler 安全守卫**: 嵌套映射配 RowBounds/自定义 ResultHandler 会破坏聚合, safeXxxEnabled 时抛); 否则简单路径 | DefaultResultSetHandler.java:330-356 |
| q5 | 对象创建链 | createResultObject 四分支: ①结果类型有 TypeHandler→原始值(Map/标量) ②构造器映射→createParameterizedResultObject ③接口/默认构造→objectFactory.create ④无默认构造→createByConstructorSignature(自动构造器); 全无→抛 "Do not know how to create an instance"; nestedQuery&&lazy→ProxyFactory 延迟代理(issue#109/#149) | DefaultResultSetHandler.java:654-700,729-757 |
| q6 | 三级自动判定 | shouldApplyAutomaticMappings: ①ResultMap.autoMapping 显式属性 ②嵌套→仅 FULL ③简单→非 NONE(PARTIAL/FULL 都自动) | DefaultResultSetHandler.java:466-478 |
| q7 | 显式映射 | applyPropertyMappings: 仅映射列存在的字段(mappedColumnNames); 三路取值 nestedQuery(子查询, 支持 lazy)/resultSet(DEFERRED 挂起关联)/typeHandler.getResult; **callSettersOnNulls && 非 primitive 才 set null**(issue#377 默认 false 不调 setter) | DefaultResultSetHandler.java:481-515 |
| q8 | 自动映射+缓存 | createAutomaticMappings: 未映射列→columnPrefix 过滤→findProperty(mapUnderscoreToCamelCase 时 replace("_","")+忽略大小写, MetaClass.java:56-64)→hasSetter→hasTypeHandler 校验→UnMappedColumnAutoMapping **按 (resultMapId:columnPrefix) 缓存**(autoMappingsCache L607); 无对应属性→AutoMappingUnknownColumnBehavior 三态(NONE 忽略/WARNING 日志/FAILING 抛) | DefaultResultSetHandler.java:580-645 |
| q9 | 嵌套聚合 | 嵌套行 getRowValue: 同 combinedKey 命中→partialObject 复用(一父多子聚合成一个); 新建对象→**putAncestor/ancestorObjects 循环引用保护**(自引用嵌套不死循环)→applyNestedResultMappings→nestedResultObjects.put | DefaultResultSetHandler.java:440-462 |
| q10 | 构造器自动映射 | findConstructorForAutomapping 四选一: 单构造器直接/@AutomapConstructor(多个抛)/argNameBased 开启则必须 @AutomapConstructor/按 JDBC 类型匹配(findUsableConstructorByArgTypes) | DefaultResultSetHandler.java:729-757 |
| q11 | 列元数据 | ResultSetWrapper 构造读 ResultSetMetaData: useColumnLabel? label : name(L59); mapped/unmapped 列名按 resultMap+columnPrefix 缓存; typeHandler 按 (propertyType, jdbcType) 查注册表 | ResultSetWrapper.java:46-196 |

→ 引出 M-4: 插件机制 — 4 处理器工厂(含 newParameterHandler/newResultSetHandler)是拦截点, Parameter/ResultSet 处理器全部可被 @Intercepts 拦截。
