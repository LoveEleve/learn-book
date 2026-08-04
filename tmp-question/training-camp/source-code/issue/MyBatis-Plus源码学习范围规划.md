# MyBatis-Plus 源码学习范围规划

> **版本**: v3.5.7
> **仓库**: `/data/workspace/source-code/code/spring/mybatis-plus/`
> **规模**: core(158) + annotation(16) + extension(111) + generator(100) = 385 个源文件，8 个子模块
> **日期**: 2026-08-03

---

## 一、仓库概况

MyBatis-Plus 是 MyBatis 的增强工具，在不改变 MyBatis 原生能力的前提下，提供开箱即用的 CRUD 接口、Lambda 条件构造器、分页插件、乐观锁等功能。核心设计模式是**通过替换 MyBatis Configuration 来"注入"能力**——不是 AOP 代理，而是直接替换 MyBatis 的核心组件（Configuration / MapperRegistry / MapperAnnotationBuilder）。

**子模块清单**（8 个，全部审计）：

| 子模块 | 文件数 | 职责 | 状态 |
|---|---|---|---|
| `mybatis-plus-core/` | 158 | 核心：Configuration 替换 + SQL 注入器 + Wrapper 条件构造 + 表元数据 + 注解处理器 | ✅ 已探索 |
| `mybatis-plus-annotation/` | 16 | 注解定义：@TableName/@TableId/@TableField/@TableLogic/@Version/@KeySequence/@InterceptorIgnore | ✅ 已探索 |
| `mybatis-plus-extension/` | 111 | 扩展：分页/乐观锁/多租户/防全表/自动填充/代码生成引擎/DDL/ActiveRecord | ✅ 已探索 |
| `mybatis-plus-generator/` | 100 | 代码生成器：config/engine/query/jdbc/type 等 | 淘汰 |
| `spring-boot-starter/` | 0 | Spring Boot 自动装配（纯 resources，无 Java 代码） | 淘汰 |
| `mybatis-plus/` | 0 | 根聚合模块 | 淘汰 |
| `mybatis-plus-bom/` | — | BOM 依赖管理 | 淘汰 |
| `libs/` `gradle/` | — | 构建系统 | 淘汰 |

**core 包结构**：

| 包 | 职责 | 关键类 |
|---|---|---|
| `core/` root | MyBatis 组件替换 | MybatisConfiguration, MybatisMapperRegistry, MybatisMapperAnnotationBuilder, MybatisSqlSessionFactoryBuilder |
| `core/injector/` | SQL 自动注入 | AbstractSqlInjector, DefaultSqlInjector, AbstractMethod + methods/（17 种内置方法） |
| `core/metadata/` | 表元数据解析 | TableInfo, TableInfoHelper, TableFieldInfo |
| `core/conditions/` | Wrapper 条件构造 | AbstractWrapper, QueryWrapper, UpdateWrapper, LambdaQueryWrapper |
| `core/plugins/` | 拦截忽略策略 | InterceptorIgnoreHelper |
| `core/mapper/` | BaseMapper 接口 | BaseMapper<T> 泛型接口 |
| `core/handlers/` | 类型处理器 | CompositeEnumTypeHandler, MybatisMapWrapper |
| `core/toolkit/` | 工具集 | SQL 脚本生成、反射、Lambda 序列化解析 |

---

## 二、知识域规划

### 🔴 核心域（5 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| M-1 | **SQL 自动注入机制** | AbstractSqlInjector(96行), DefaultSqlInjector(61行), AbstractMethod(450行) + methods/（17 种内置方法）, MybatisSqlSessionFactoryBuilder(111行), MybatisMapperAnnotationBuilder(704行), MybatisConfiguration(490行), MybatisParameterHandler(223行) | **完整初始化和注入链路**：① `MybatisSqlSessionFactoryBuilder.build(Configuration)`：初始化 `IdentifierGenerator` 雪花算法（网络 MAC 获取 workerId）→ `IdWorker` 设置→`SqlRunnerInjector` 注入→缓存 sqlSessionFactory；② `MybatisConfiguration` 构造：`mapUnderscoreToCamelCase=true`（自动驼峰）、`CompositeEnumTypeHandler` 枚举处理器、`MybatisXMLLanguageDriver`（支持注解 `<script>`）；③ **`MybatisMapperAnnotationBuilder.parse()`** 触发注入：加载 XML→解析 `@Select/@ResultMap`→校验 `isSupperMapperChildren(configuration, type)`（是否继承 BaseMapper）→是则调用 `parserInjector()`→`GlobalConfigUtils.getSqlInjector().inspectInject(assistant, type)` 批量注册 17 种 MappedStatement；④ **`MybatisParameterHandler.process()`** 运行时处理：INSERT 时 `populateKeys()` 雪花 ID 生成（`identifierGenerator.nextId(entity)`）+ `insertFill()` MetaObjectHandler 回调；UPDATE 时 `updateFill()` 回调；**SQL 优先级**：`addMappedStatement()` 已存在 ID 则忽略新注入→**XmlSql > sqlProvider > CurdSql**（自定义优先）；`AbstractMethod.inject()` 模板：构建 `SqlSource`（动态 SQL `<script>...<if test>...<where>...<trim>...</script>`）→ `builderAssistant.addMappedStatement`；**扩展点**：替换 `GlobalConfig.sqlInjector` + 注册自定义 AbstractMethod 子类 |
| M-2 | **表元数据解析与全局配置** | TableInfo(580行), TableInfoHelper(646行), TableFieldInfo, GlobalConfig(含DbConfig) | **三级注解解析**：`TableInfoHelper.initTableInfo()` 反射解析实体类→`initTableName()`（@TableName 表名映射+tablePrefix 拼接）→`initTableFields()`（遍历字段，解析 @TableId/@TableField/@TableLogic/@Version）→`initTableIdWithAnnotation()`/`initTableIdWithoutAnnotation()` 主键识别；**GlobalConfig.DbConfig 全局策略**：`idType`（默认 ASSIGN_ID 雪花）、`tablePrefix`（表前缀）、`tableUnderline`（驼峰转下划线）、`insertStrategy/updateStrategy/whereStrategy`（默认 FieldStrategy.NOT_NULL）、`capitalMode`、`keyGenerators`（Sequence 生成器）；**GlobalConfig 其他配置**：`sqlInjector`（可替换）、`metaObjectHandler`（自动填充）、`identifierGenerator`（ID 生成器）、`superMapperClass`（Mapper 父类检测）、`mapperRegistryCache`（已注入追踪） |
| M-3 | **Lambda 条件构造器** | AbstractWrapper(712行), LambdaQueryWrapper<T>, LambdaUpdateWrapper<T>, LambdaUtils(125行), SerializedLambda, SFunction | **Lambda 避免字段名硬编码**：`LambdaQueryWrapper<User>.eq(User::getName, "Tom")` → `SFunction<T,R>` 是 `Serializable` 的函数式接口 → `LambdaUtils.extract()` **双路径解析**：① IDEA 调试模式（Lambda 是 Proxy）→ `IdeaProxyLambdaMeta` ② 正常模式 → `writeReplace()` 方法返回 `java.lang.invoke.SerializedLambda` → `getImplMethodName()` 获取 "getName" → `methodToProperty()` 转换为属性名 → `ColumnCache` 缓存解析结果避免重复计算；`AbstractWrapper` 提供完整 SQL 条件链：`eq/ne/gt/ge/lt/le/like/between/in/isNull/orderBy/groupBy/having/exists/last/select/apply` + MySQL 特有 `jsonContains`/`jsonEq`/`inset`；**嵌套**：`and(Consumer)`/`or(Consumer)` 生成括号分组；**参数命名**：`SharedString` 持有 `paramAlias`（如 "ew"），`paramNameValuePairs` 生成 `#{ew.paramNameValuePairs.MPGENVAL1}` 占位符；**UpdateWrapper** 支持 `set(String column, Object val)` / `setSql` / `setIncrBy` |
| M-4 | **MybatisPlusInterceptor 插件体系** | MybatisPlusInterceptor(155行), InnerInterceptor（6回调接口）, InterceptorIgnoreHelper(238行) | **3.4.0+ 新版插件**：统一 `MybatisPlusInterceptor` 拦截 `Executor.update/query` + `StatementHandler.prepare/getBoundSql`；**InnerInterceptor 6 个回调**：① `willDoQuery`/`willDoUpdate`（返回 false 则跳过执行——查询返回空 List、更新返回 -1）② `beforeQuery`/`beforeUpdate`（SQL 改写时机）③ `beforePrepare`（参数修改）④ `beforeGetBoundSql`（BatchExecutor/ReuseExecutor 专用）；**内置 10 种 InnerInterceptor**：Pagination（物理分页）、OptimisticLocker（`@Version` CAS）、**TenantLine**（多租户 SQL 追加 `AND tenant_id=?`）、**DataPermission**（数据权限过滤）、**BlockAttack**（防全表删除/更新 `WHERE` 缺失拦截）、IllegalSQL（全表扫描检测）、DynamicTableName（动态表名替换 `sys_log_202408`）、ReplacePlaceholder（占位符替换）、**DataChangeRecorder**（数据变更记录 990行，最大实现）、**BaseMultiTableInnerInterceptor**（多表父类，TenantLine+DataPermission 继承自此）；**`@InterceptorIgnore`**：Two-level control — Mapper 注解（静态）+ ThreadLocal `InterceptorIgnoreHelper.handle()`（程序化，权限大于注解） |
| M-5 | **分页插件实现** | PaginationInnerInterceptor(478行), Page<T> | **物理分页 SQL 改写**：拦截 Executor.query → 判断参数含 `IPage` → 解析原始 SQL → 根据方言（MySQL/PG/Oracle 等）拼接 `LIMIT ?,?` / `OFFSET ? LIMIT ?` / `ROWNUM` → 执行 count 查询获取总数 → 执行分页查询；**多方言支持**：DbType 枚举识别（MySQL/MariaDB/Oracle/DB2/H2/SQLite/SQLServer/PG/Phoenix/DM/KingbaseES/ClickHouse 等 12+ 种）；`optimizeCountSql` 优化 count（去除 ORDER BY）；`join` 关联查询支持 |

### 🟡 扩展域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| M-6 | **乐观锁** | OptimisticLockerInnerInterceptor, @Version | 拦截 Executor.update → 检查 `@Version` 字段 → 原 SQL `WHERE id=? AND version=?` + `SET version=version+1` → 更新行数=0 则抛 OptimisticLockerInnerInterceptor 异常 |
| M-7 | **自动填充与 ID 生成** | MybatisParameterHandler(223行), MetaObjectHandler(243行), @TableField(fill=FieldFill), IdentifierGenerator(59行), DefaultIdentifierGenerator(69行) | **触发时机不在 SQL 注入，在 MybatisParameterHandler 运行时**：`processParameter()` → 区分 INSERT/UPDATE → `process()` 提取实体参数 → `populateKeys()`（ASSIGN_ID 雪花/ASSIGN_UUID）→ `insertFill()`/`updateFill()` 回调 `MetaObjectHandler`；**雪花算法**：`DefaultIdentifierGenerator` 使用 `workerId`（网络 MAC 地址）+ `datacenterId` + 毫秒时间戳 + 序列号；**MetaObjectHandler**：`insertFill(metaObject)`/`updateFill(metaObject)` + MappedStatement 级过滤（`openInsertFill(mappedStatement)`）；`StrictFill` 提供严格填充策略（targetField 非 null 才填充）；`@TableField(fill=FieldFill.INSERT/UPDATE/INSERT_UPDATE)` 标记哪些字段需要填充 |
| M-8 | **逻辑删除** | @TableLogic, SqlScriptUtils | `@TableLogic` 注解标记逻辑删除字段 → SQL 注入器自动将 `deleteById` → `UPDATE SET deleted=1 WHERE id=?`；`selectList` 自动追加 `AND deleted=0`；支持全局配置 `logic-delete-value`/`logic-not-delete-value` |
| M-9 | **BaseMapper 与 IService** | BaseMapper<T>, IService<T>, ServiceImpl | `BaseMapper<T>` 定义 20+ 通用 CRUD 方法（insert/delete/update/select 四大类）；`IService<T>` 是 Service 层接口；`ServiceImpl<M extends BaseMapper<T>, T>` 默认实现；**批量操作**：`saveBatch`/`updateBatchById`（内部逐条执行，非真正 batch） |

---

## 三、淘汰清单

| 子模块/功能 | 文件数 | 理由 |
|---|---|---|
| `mybatis-plus-generator/` | 100 | 代码生成器，开发工具非运行时，用一次就够 |
| `spring-boot-starter/` | 0 | 纯 resources 无 Java 代码，自动装配逻辑由 `mybatis-plus-spring-boot-autoconfigure` 提供（在 Spring Boot 启动器中） |
| `mybatis-plus/` `mybatis-plus-bom/` `libs/` `gradle/` | — | 构建基础设施 |
| `extension/activerecord/` | 若干 | ActiveRecord 模式，Spring 项目用 MyBatis Mapper 模式 |
| `extension/incrementer/` | 若干 | 分布式 ID 生成器（雪花算法），面试低频且独立于核心架构 |
| `extension/ddl/` | 若干 | DDL 自动建表，生产不推荐 |
| `extension/scripting/` | 若干 | 脚本语言支持，边缘功能 |
| `extension/p6spy/` | 若干 | p6spy SQL 性能监控，用 Druid StatFilter 替代 |
| `extension/service/` | 若干 | IService 接口定义，M-9 🟡 覆盖 |
| `core/batch/` | 若干 | 伪批量操作（逐条执行），不深入 |
| `core/override/` | 若干 | MyBatis 组件重载，不独立成域 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 5 |
| 🟡 扩展域 | 4 |
| **总域** | **9** |
| 淘汰子模块/功能 | 11 个 |

---

## 五、与现有规划的交叉覆盖

| 交叉点 | MyBatis-Plus 域 | Boot 域 | 关系 |
|---|---|---|---|
| mybatis-spring-boot-starter | — | B-9（自动装配目标列表） | Boot 域讲自动装配，MP 域讲核心机制 |
| HikariCP/Druid 连接池 | — | B-9 | MP 不涉及连接池，由底层 DataSource 提供 |

---

## 六、学习顺序建议

```
M-1 SQL 自动注入（理解 MP 如何"无中生有"生成 SQL）
  → M-2 表元数据（理解 @TableName/@TableId/@TableField 如何解析）
    → M-3 Lambda Wrapper（理解 Lambda 条件 API 如何避免字符串硬编码）
      → M-4 MybatisPlusInterceptor 插件体系（理解新版插件架构）
        → M-5 分页插件（理解物理分页 SQL 改写）
          → M-6~M-9 按需深入（乐观锁/自动填充/逻辑删除/IService）
```

以上规划完成，共 **5🔴+4🟡=9 域**，等你确认。
