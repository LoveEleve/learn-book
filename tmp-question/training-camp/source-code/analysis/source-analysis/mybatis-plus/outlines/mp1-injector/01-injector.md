# MP-1 SQL 自动注入 — 注入器方法面 + AbstractMethod 模板 → MappedStatement

> 前置: [[MP-2-metadata]] (TableInfo, 已交付) | 复用: [[M-1-configuration]] (Configuration/builderAssistant/StrictMap) | 对照: [[M-3-mapper]] (statementId 绑定/Invalid bound statement) | 引出: [[MP-9-mapper-service]]
> 🔴 Deep | 8 KP | [模式: 模板方法+策略注册表(方法类)+枚举模板]
> Pass 2 闭环: q1(12 默认方法面) q2(装配循环) q3(用户自定义优先) q4(注册参数) q5(列片段来源) q6(方法类模板) q7(keyGenerator 三分支) q8(SqlMethod 枚举)

**读者处境**: BaseMapper 里只声明 `int insert(T entity)` 接口方法, 没有 XML 没有注解 — 为什么 `mapper.insert(user)` 能执行?注入发生在什么时候?为什么 XML 里自己写了 `selectById` 就不会报重复?为什么无 @TableId 的实体调 selectById 会报 statement 不存在?这篇拆 MP 的 SQL 自动注入: 注入器决定"注入哪些方法", AbstractMethod 模板决定"方法怎么生成 SQL"。

### 1. 注入器方法面 — DefaultSqlInjector 12 默认方法 + 无主键降级

场景: 哪些方法无条件注入?为什么 xxById 族看主键脸色?
源码路径:
- `DefaultSqlInjector.java:39-60` — getMethodList(q1): **7 无条件**(Insert/Delete/Update/SelectCount/SelectMaps/SelectObjs/SelectList, L42-48 — **Insert 带 insertIgnoreAutoIncrementColumn 配置 L42**)+**5 havePK 条件**(DeleteById/DeleteByIds/UpdateById/SelectById/SelectBatchByIds, L49-54); 无主键 warn "Cannot use Mybatis-Plus 'xxById' Method"(L55-58)
- `DefaultSqlInjector.java:19` — `import ...injector.methods.*` 18 方法类
关键设计: 方法面决策(q1): 注入的可用性由**元数据**驱动 — havePK 决定 xxById 族注入与否(MP-2 的 TableInfo.havePK); 无主键不报错只 warn(实体仍可 CRUD, 只是 xxById 不可用 — 调用时报 M-3 的 statement 不存在)。[模式: 元数据驱动方法面]
数据流: getMethodList(configuration, mapperClass, tableInfo) → 7 无条件 + (havePK? 5 xxById : warn) → 列表。

### 2. 装配循环 — AbstractSqlInjector.inspectInject 防重复 + 注入时机

场景: 注入发生在什么时候?重复注入怎么防?
源码路径:
- `AbstractSqlInjector.java:44-65` — inspectInject(q2): `mapperRegistryCache`(GlobalConfig 的 ConcurrentSkipListSet, M-1 交付)contains 检查 → **TableInfoHelper.initTableInfo(builderAssistant, modelClass)**(MP-2 交付)→getMethodList→逐方法 `m.inject(builderAssistant, mapperClass, modelClass, tableInfo)`→cache.add
- `AbstractSqlInjector.java:45` — `ReflectionKit.getSuperClassGenericType(mapperClass, Mapper.class, 0)` — 泛型解析实体类
关键设计: 注入时机(q2): **MP 覆写 MybatisMapperAnnotationBuilder.parse — 注解语句解析完成后, isSupperMapperChildren 判断**(GlobalConfigUtils L122-124: superMapperClass 默认 Mapper.class 可赋值才注入, 普通接口跳过) → parserInjector → inspectInject; 注入失败(IncompleteElementException)→**InjectorResolver 延迟重试**(M-1 incomplete 机制, InjectorResolver.java:26-38); 防重复: mapperRegistryCache 以 mapperClass.toString 为键 — 同一 mapper 只注入一次。[模式: 覆写装配+注册表防重]
数据流: MybatisMapperRegistry.addMapper → MybatisMapperAnnotationBuilder.parse(注解语句) → isSupperMapperChildren? parserInjector→inspectInject : 跳过 → 泛型解析实体 → initTableInfo(MP-2) → 方法注入循环。

### 3. 模板与注册 — AbstractMethod 双抽象 + 用户自定义优先

场景: 新增一个自定义注入方法要写什么?XML 里已有同名语句会怎样?
源码路径:
- `AbstractMethod.java:82-88` — inject(q4): 配置三件套(configuration/builderAssistant/`languageDriver=configuration.getDefaultScriptingLanguageInstance()` M-1)→`injectMappedStatement` 抽象
- `AbstractMethod.java:252-273` — addMappedStatement(q3): statementName=`mapperClass.getName() + "." + id`; **hasMappedStatement(已存在)→warn "Has been loaded by XML or SqlProvider or Mybatis's Annotation, so ignoring this injection" 返回 null**(L256-260); builderAssistant.addMappedStatement: PREPARED/flushCache=`!isSelect`/useCache=isSelect/keyGenerator 透传(L261-267)
- `AbstractMethod.java:162-173` — sqlSelectColumns(q5): 自定义 resultMap(非自动)→"*"; 否则 `table.getAllSqlSelect()`(MP-2 列名+AS)
关键设计: 用户优先(q3): 注入前查 hasStatement(M-1 StrictMap) — XML/注解显式定义的同名语句优先, MP 注入静默放弃(warn 不抛); 注册参数与 M-1 的 MappedStatement.Builder 对齐(q4): flushCache/useCache 按读写自动设置。[模式: 模板方法+冲突让位]
数据流: m.inject → injectMappedStatement(SQL 模板组装) → addMappedStatement → hasStatement? warn 返回 : builderAssistant.addMappedStatement → M-1 StrictMap。

### 4. 方法类示例 — SelectById 模板实例化 + Insert 主键三分支

场景: 一个方法类的完整逻辑?Insert 的主键生成策略怎么选?
源码路径:
- `methods/SelectById.java` — injectMappedStatement(q6): `String.format(SqlMethod.SELECT_BY_ID.getSql(), sqlSelectColumns(tableInfo, false), tableInfo.getTableName(), tableInfo.getKeyColumn(), tableInfo.getKeyProperty(), tableInfo.getLogicDeleteSql(true, true))` → `createSqlSource`(XML 脚本)→`addSelectMappedStatementForTable`
- `methods/Insert.java` — keyGenerator 三分支(q7): **外层条件 keyProperty 非空(无主键实体直接 NoKeyGenerator+null)**; 默认 `NoKeyGenerator.INSTANCE`; **`IdType.AUTO`→`Jdbc3KeyGenerator.INSTANCE`+keyColumn 去转义符(`SqlInjectionUtils.removeEscapeCharacter`)**; keySequence→`TableInfoHelper.genKeyGenerator`(M-1 KeyGenerator 机制); columnScript=convertTrim(`getAllInsertSqlColumnMaybeIf`) 等(MP-2 片段)
- `AbstractMethod.java:390+` — createSqlSource: 统一 `<script>` 包裹(LanguageDriver 动态 SQL 兼容)
关键设计: 方法类=模板实例化(q6): 每个方法类声明 SqlMethod 枚举 + String.format 填占位符 + 选注册变体(addSelect/addInsert/addDelete/addUpdate); keyGenerator 由 IdType 驱动(q7): AUTO 走 JDBC 自增回填, Sequence 走 KeyGenerator(MP-2 的 genKeyGenerator), 默认无回填。[模式: 策略选择+模板实例化]
数据流: Insert.injectMappedStatement → 选 keyGenerator(IdType) → 拼 columnScript/valuesScript(MP-2) → addInsertMappedStatement → 注册。

### 5. SqlMethod 枚举模板 — 25 值三要素

场景: SQL 模板存在哪?占位符怎么用?
源码路径:
- `core/enums/SqlMethod.java` — 25 枚举值(q8): `INSERT_ONE("insert", "插入一条数据(选择字段插入)", "<script>INSERT INTO %s %s VALUES %s</script>")` / DELETE_BY_ID / UPDATE_BY_ID / SELECT_BY_ID...
- 模板统一 `<script>` 包裹 — 与 M-6 动态 SQL 解析兼容(createSqlSource 走 XMLLanguageDriver)
关键设计: 枚举模板(q8): SQL 模板+方法名+描述三要素集中一处; %s 占位符由方法类 String.format 填充; <script> 包裹让模板可含动态标签(后续 MP-3/MP-8 的片段也进此类模板)。[模式: 枚举模板]
数据流: SqlMethod.SELECT_BY_ID → getSql() → String.format(列, 表, keyColumn, keyProperty, 逻辑删除) → createSqlSource → MappedStatement。

### 负面空间 — 注入层刻意不做的事

- **不做 SQL 校验**: 模板拼装后直接交 LanguageDriver 解析, 语法错误在首次执行才暴露(与 M-1 配置解析的启动校验不同)
- **不做运行时重注入**: mapperRegistryCache 一次注入, 热更新实体需重初始化(MP-2 Configuration 比较机制)
- **不做方法删除**: 注入的方法不可撤销, 用户优先只是"跳过"而非"移除"

→ 引出: BaseMapper 的 17 方法面(MP-9)正是注入方法名的用户侧镜像 — 注入的方法与接口方法一一对应; 调用链回到 M-3(statementId 绑定) → [[MP-9-mapper-service]]
