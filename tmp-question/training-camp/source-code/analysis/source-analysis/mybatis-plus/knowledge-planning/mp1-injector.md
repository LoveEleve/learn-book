# MP-1 SQL 自动注入 — DefaultSqlInjector 方法面 + AbstractMethod 模板 → MappedStatement

> 项目: MyBatis-Plus | 🔴 Deep / 1 篇 | DefaultSqlInjector(61)+AbstractSqlInjector(96)+AbstractMethod(450)+methods/ 18 类+SqlMethod(25 枚举)
> 基线: MP-PLAN MP-1 — 前置: **M-1 (Configuration/builderAssistant/StrictMap) + MP-2 (TableInfo, 已交付)** — 展开 注入器方法面→方法类模板→SQL 片段组装→MappedStatement 注册

---

## §0.8

- 🔴 Deep，1篇 — 注入器(**DefaultSqlInjector.getMethodList L39-60: 12 默认方法[7 无条件: Insert/Delete/Update/SelectCount/SelectMaps/SelectObjs/SelectList L42-48 + 5 havePK 条件: DeleteById/DeleteByIds/UpdateById/SelectById/SelectBatchByIds L49-54]; 无主键 warn "Cannot use Mybatis-Plus 'xxById' Method" L55-58**) → 装配(**AbstractSqlInjector.inspectInject L44-65: mapperRegistryCache 防重复→TableInfoHelper.initTableInfo(MP-2)→getMethodList→循环 m.inject→mapperRegistryCache.add**) → 模板(**AbstractMethod: inject L82-88[配置 configuration/builderAssistant/languageDriver[默认 XML 驱动 M-1]→injectMappedStatement]; addMappedStatement L252-273[statementName=接口名.方法名; **hasMappedStatement 已存在→warn "Has been loaded by XML or SqlProvider or Mybatis's Annotation, so ignoring this injection" 返回 null[用户自定义优先 L256-260]**; builderAssistant.addMappedStatement[PREPARED/flushCache=!isSelect/useCache=isSelect L261-267]]; SQL 片段族[sqlSelectColumns L162-173[自定义 resultMap 用 * 否则 getAllSqlSelect MP-2]/sqlSet L120-130[逻辑删除+ew wrapper]/sqlWhereEntityWrapper L228+/optlockVersion/convertChooseEwSelect]**) → 方法类(**methods/ 18 类: SelectById[String.format(sqlMethod.getSql(), 列, 表名, 主键列, 主键属性, 逻辑删除 SQL)→createSqlSource→addSelectMappedStatementForTable]; Insert[keyGenerator 三分支[默认 NoKeyGenerator→IdType.AUTO→Jdbc3KeyGenerator+keyColumn 去转义符 SqlInjectionUtils.removeEscapeCharacter→keySequence→TableInfoHelper.genKeyGenerator]; columnScript=convertTrim(getAllInsertSqlColumnMaybeIf)[MP-2 片段]**) → SQL 模板(**SqlMethod 枚举 25 值[core/enums/SqlMethod.java: method 名+描述+SQL 模板 %s 占位, 如 INSERT_ONE("insert","插入一条数据(选择字段插入)","<script>INSERT INTO %s %s VALUES %s</script>")]**)
- 设计模式: [模式: 模板方法+策略注册表(方法类)+枚举模板]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DefaultSqlInjector.java:39-60 | 方法面 | **12 默认方法**: 7 无条件(Insert/Delete/Update/SelectCount/SelectMaps/SelectObjs/SelectList)+5 havePK 条件(xxById 族); 无主键 warn | High |
| AbstractSqlInjector.java:44-65 | 装配 | mapperRegistryCache 防重复 → initTableInfo(MP-2) → getMethodList → 循环 inject → cache.add | High |
| AbstractMethod.java:82-88 | 模板入口 | inject: 配置三件套(configuration/builderAssistant/languageDriver 默认 XML)→injectMappedStatement 抽象 | High |
| AbstractMethod.java:252-273 | 注册 | **hasMappedStatement 已存在→warn 忽略(用户自定义优先)**; builderAssistant.addMappedStatement(PREPARED/flushCache=!isSelect/useCache=isSelect) | High |
| AbstractMethod.java:162-173 | 列片段 | sqlSelectColumns: 自定义 resultMap→*; 否则 getAllSqlSelect(MP-2) | High |
| methods/SelectById.java | 方法类 | String.format(sqlMethod.getSql(), 列, 表名, keyColumn, keyProperty, 逻辑删除)→createSqlSource→addSelectMappedStatementForTable | High |
| methods/Insert.java | 方法类 | keyGenerator 三分支(NoKey/AUTO→Jdbc3+去转义符/Sequence→genKeyGenerator); columnScript=convertTrim(getAllInsertSqlColumnMaybeIf) | High |
| SqlMethod.java | 枚举模板 | 25 值: method 名+描述+SQL 模板(%s 占位); INSERT_ONE/UPDATE_BY_ID/DELETE_BY_ID/SELECT_BY_ID/... | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 注入是单管线 — 1篇 (~70行) 按"注入器方法面→装配循环→模板注册→方法类示例→SqlMethod 模板"展开; TableInfo 片段来源衔接 MP-2(引用), 注册目标衔接 M-1(引用)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 12 默认方法面 + 无主键降级 | 🔴 | **为什么🔴**: 方法可用性 |
| P1-2 | inspectInject 装配循环 + 防重复 | 🔴 | **为什么🔴**: 注入时机 |
| P1-3 | addMappedStatement 注册 + 用户自定义优先 | 🔴 | **为什么🔴**: 冲突语义 |
| P1-4 | 模板方法: inject/injectMappedStatement 抽象 | 🔴 | **为什么🔴**: 扩展骨架 |
| P1-5 | 方法类组装 (SelectById/Insert 示例) | 🔴 | **为什么🔴**: 模板用法 |
| P2-1 | SqlMethod 枚举模板 (25 值) | 🟡 | **为什么🟡**: 记忆型 |
| P2-2 | keyGenerator 三分支 | 🟡 | **为什么🟡**: 主键策略 |
| P3-1 | 与 M-1 StrictMap/MP-2 片段协作 (引用) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **方法面与装配** | 🔴 | 注入主流程 |
| B | **模板与注册** | 🔴 | 机制核心 |
| C | **方法类与枚举** | 🟡 | 具体实现 |
| D | **协作边界** | 🟢 | 衔接 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 12 默认方法面 | getMethodList: 7 无条件(Insert/Delete/Update/SelectCount/SelectMaps/SelectObjs/SelectList)+5 havePK 条件(xxById 族) — 无主键时 xxById 不注入且 warn(测试/生产常见: 无 @TableId 实体调 selectById 报 statement 不存在) | DefaultSqlInjector.java:39-60 |
| q2 | 装配循环与时机 | inspectInject: mapperRegistryCache(ConcurrentSkipListSet) 防重复 → initTableInfo(MP-2)→getMethodList→逐方法 inject→cache.add; **触发: MybatisMapperAnnotationBuilder.parse 注解解析后 + isSupperMapperChildren(superMapperClass 默认 Mapper.class 可赋值才注入, GlobalConfigUtils L122-124) + 失败走 InjectorResolver 延迟重试** | AbstractSqlInjector.java:44-65; MybatisMapperAnnotationBuilder.java:114-126; InjectorResolver.java:26-38 |
| q3 | 用户自定义优先 | addMappedStatement: statementName=接口名.方法名; **hasMappedStatement(statementName, false)(L96-98, false=不触发 buildAllStatements 防注入期 pending 解析)→已存在→warn "Has been loaded by XML or SqlProvider or Mybatis's Annotation, so ignoring this injection" 返回 null** — XML/注解显式定义覆盖 MP 注入 | AbstractMethod.java:252-273,96-98 |
| q4 | 注册参数 | builderAssistant.addMappedStatement: PREPARED/flushCache=!isSelect/useCache=isSelect/keyGenerator 透传 — 与 M-1 的 MappedStatement.Builder 对齐 | AbstractMethod.java:261-267 |
| q5 | 列片段来源 | sqlSelectColumns: 自定义 resultMap(非自动)→"*"; 否则 table.getAllSqlSelect(MP-2 的列名+AS 决策) — 结果映射交给用户 | AbstractMethod.java:162-173 |
| q6 | 方法类模板 | SelectById: String.format(SqlMethod.SELECT_BY_ID.sql, 列, 表名, keyColumn, keyProperty, 逻辑删除)→createSqlSource(XML 脚本)→addSelectMappedStatementForTable — 方法类=模板实例化 | methods/SelectById.java |
| q7 | keyGenerator 三分支 | Insert: 默认 NoKeyGenerator; **IdType.AUTO→Jdbc3KeyGenerator+keyColumn 去转义符(SqlInjectionUtils.removeEscapeCharacter)**; keySequence→TableInfoHelper.genKeyGenerator(M-1 KeyGenerator 机制) | methods/Insert.java |
| q8 | SqlMethod 枚举 | 25 值: method 名+描述+SQL 模板(%s 占位) — INSERT_ONE/UPDATE_BY_ID/DELETE_BY_ID/SELECT_BY_ID 等; 模板统一 <script> 包裹(动态 SQL 兼容) | core/enums/SqlMethod.java |

→ 引出 MP-9: BaseMapper 17 方法面 = 注入的 xxById/selectList 等方法名的用户侧镜像; M-3 的 "Invalid bound statement" 与此注入时机关联。
