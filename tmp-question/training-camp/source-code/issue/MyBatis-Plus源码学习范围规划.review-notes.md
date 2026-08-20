# MyBatis-Plus 源码学习范围规划复盘与修订路线图

> 复盘基线：MyBatis-Plus 3.5.7，源码 `/data/workspace/source-code/code/spring/mybatis-plus/`。
> 本文不是正文大纲，而是按“先模块扫描，再按机制重组；补齐失败路径、测试证据、集成边界”的方法论，对现有 9 域规划做卷级复盘。

## 一、复盘结论

现有 9 域规划抓住了 MyBatis-Plus 的几个高价值主题：

- SQL 自动注入
- 表元数据解析
- Lambda Wrapper
- 插件体系
- 分页插件
- 乐观锁 / 自动填充 / 逻辑删除 / `BaseMapper + IService`

但从方法论看，原规划还有三个风险：

1. **把“Configuration 替换 + SQL 注入 + 运行时参数处理”写成一个巨型域**，容易让真正的入口、状态中心和运行时回调混在一起。
2. **低估了 Spring Boot 自动装配层**。旧规划把 `spring-boot-starter/` 直接淘汰，但当前仓库中真正的 Java 自动装配逻辑在 `spring-boot-starter/mybatis-plus-spring-boot-autoconfigure/`，它不是空壳。
3. **把 `IService/ServiceImpl`、生成器、DDL、样例和测试设施的边界混在一起**。它们并不是同一层：有些是核心增强面，有些只是生态或开发工具。

因此，旧“5 核心 + 4 扩展”的 9 域框架可以保留骨架，但需要按主干 / 机制补深 / 集成装配 / 生产层候选重新收束。

## 二、已验证的仓库事实

| 项目 | 已验证结果 | 证据 |
|---|---:|---|
| 版本 | 3.5.7 | 规划文档头部 + 仓库目录 |
| 主源码 | core 158 + extension 111 + annotation 16 | 本地递归计数 |
| 索引状态 | MCP 已就绪 | `data-workspace-source-code-code-spring-mybatis-plus` |
| 架构主入口 | `MybatisSqlSessionFactoryBuilder.build(...)` | `mybatis-plus-core/src/main/java/com/baomidou/mybatisplus/core/MybatisSqlSessionFactoryBuilder.java:80` |
| 注册主桥 | `MybatisMapperRegistry.addMapper(...)` | `mybatis-plus-core/src/main/java/com/baomidou/mybatisplus/core/MybatisMapperRegistry.java:76` |
| 注入主桥 | `AbstractSqlInjector.inspectInject(...)` | `mybatis-plus-core/src/main/java/com/baomidou/mybatisplus/core/injector/AbstractSqlInjector.java:43` |
| 元数据中心 | `TableInfoHelper.initTableInfo(...)` | `mybatis-plus-core/src/main/java/com/baomidou/mybatisplus/core/metadata/TableInfoHelper.java:185` |
| Lambda 入口 | `LambdaUtils.extract(...)` | `mybatis-plus-core/src/main/java/com/baomidou/mybatisplus/core/toolkit/LambdaUtils.java:50` |
| 插件主入口 | `MybatisPlusInterceptor.intercept(...)` | `mybatis-plus-extension/src/main/java/com/baomidou/mybatisplus/extension/plugins/MybatisPlusInterceptor.java:55` |
| 分页主入口 | `PaginationInnerInterceptor.beforeQuery(...)` | `mybatis-plus-extension/src/main/java/com/baomidou/mybatisplus/extension/plugins/inner/PaginationInnerInterceptor.java:148` |
| 参数增强入口 | `MybatisParameterHandler.process(...)` | `mybatis-plus-core/src/main/java/com/baomidou/mybatisplus/core/MybatisParameterHandler.java:81` |
| Boot 自动装配层 | `mybatis-plus-spring-boot-autoconfigure` 存在真实 Java 自动装配类 | 架构扫描与目录扫描 |

## 三、按机制重组后的建议结构

### A. 核心主干层

#### MP-1 Configuration 替换与 Mapper 注册桥

**读者问题**：MyBatis-Plus 是怎样在不推翻 MyBatis 核心的前提下，替换掉关键组件并把 mapper 接口接进增强体系的？

**入口**：`MybatisSqlSessionFactoryBuilder`、`MybatisConfiguration`、`MybatisMapperRegistry`、`MybatisMapperAnnotationBuilder`。

**状态核心**：替换后的 `Configuration`、自定义 `MapperRegistry`、`StrictMap`、mapper 注册缓存、默认 language driver / enum handler / 驼峰设置。

**失败路径**：mapper 重复注册、annotation builder 解析失败、与原生 MyBatis 配置冲突。

**跨域桥**：MP-2 的 SQL 注入、MP-3 的表元数据、MP-7 的 Spring Boot 自动装配。

#### MP-2 SQL 自动注入与 `MappedStatement` 批量生成

**读者问题**：为什么 `BaseMapper` 没有 XML 也能直接得到一批 CRUD statement？

**入口**：`AbstractSqlInjector.inspectInject(...)`、`DefaultSqlInjector.getMethodList(...)`、`AbstractMethod.inject(...)`。

**状态核心**：`GlobalConfigUtils.getSqlInjector()`、`mapperRegistryCache`、method list、`assistant.addMappedStatement(...)`。

**失败路径**：方法重复注入、父 Mapper 判断失败、自定义注入器冲突。

**跨域桥**：MP-1 注册桥、MP-3 元数据、MP-6 逻辑删除/乐观锁/自动填充。

#### MP-3 表元数据解析与 `GlobalConfig` 边界

**读者问题**：实体类上的 `@TableName/@TableId/@TableField/@TableLogic/@Version` 是怎样变成运行时表元数据的？

**入口**：`TableInfoHelper.initTableInfo(...)`、`initTableName(...)`、`initTableFields(...)`、`initTableIdWithAnnotation(...)`。

**状态核心**：`TableInfo`、`TableFieldInfo`、`GlobalConfig.DbConfig`、结果映射缓存、key generator。

**失败路径**：重复主键、缺省主键策略、逻辑删除与版本字段冲突。

**跨域桥**：MP-2 SQL 注入、MP-4 Lambda Wrapper、MP-6 运行时填充与逻辑删除。

#### MP-4 Wrapper / Lambda 条件构造器

**读者问题**：为什么 Lambda Wrapper 能避免字段名硬编码，而且还能持续生成一整条条件链？

**入口**：`AbstractWrapper`、`LambdaQueryWrapper`、`LambdaUpdateWrapper`、`LambdaUtils.extract(...)`。

**状态核心**：`SerializedLambda`、`ColumnCache`、`paramNameValuePairs`、`SharedString`、SQL segment 合并。

**失败路径**：Lambda 解析失败、字段缓存缺失、调试代理与正常模式双路径差异。

**跨域桥**：MP-3 表元数据、MP-5 插件与分页、MP-6 运行时参数处理。

#### MP-5 插件总线与 SQL 改写入口

**读者问题**：MyBatis-Plus 为什么没有做“一个分页插件”这么简单，而是先做了 `MybatisPlusInterceptor + InnerInterceptor` 总线？

**入口**：`MybatisPlusInterceptor.intercept(...)`、`plugin(...)`、`addInnerInterceptor(...)`。

**状态核心**：6 个回调时机、`Executor` / `StatementHandler` 切入点、BoundSql 改写、跳过执行约定。

**失败路径**：多插件顺序、跳过执行、占位符替换失败、SQL 改写副作用。

**跨域桥**：MP-6 具体内置插件、MP-7 Boot 自动装配自动挂载插件。

### B. 机制补深层

#### MP-6 内置运行时增强专题组

把当前旧规划的 M-5~M-8 拆成一个“增强专题组”，而不是互不相干的小域：

- **分页插件**：`PaginationInnerInterceptor.beforeQuery(...)`
- **乐观锁**：`OptimisticLockerInnerInterceptor`
- **自动填充与 ID 生成**：`MybatisParameterHandler.process()/populateKeys()/insertFill()/updateFill()`、`IdentifierGenerator`
- **逻辑删除**：`@TableLogic` 与注入 SQL 的改写
- **安全/隔离类插件**：`TenantLineInnerInterceptor`、`DataPermissionInterceptor`、`BlockAttackInnerInterceptor`、`IllegalSQLInnerInterceptor`、`DynamicTableNameInnerInterceptor`

这些可以继续拆成多篇正文，但规划层应该先把它们视为“插件总线上的增强家族”，而不是互相独立的星散专题。

#### MP-7 Spring Boot 自动装配桥

**读者问题**：MyBatis-Plus 是怎样在 Boot 下自动创建增强版 `SqlSessionFactory`、挂载插件、回填 `GlobalConfig` 和扫描 mapper 的？

**入口**：`mybatis-plus-spring-boot-autoconfigure` 子模块中的 `MybatisPlusAutoConfiguration`、`MybatisPlusProperties`、相关 customizer。

**状态核心**：自动装配条件、properties -> configuration 回填、interceptor/identifierGenerator/metaObjectHandler 自动装配。

**失败路径**：多数据源、属性冲突、默认插件装配顺序、与原生 mybatis-spring-boot-starter 叠加时的边界。

**跨域桥**：MP-1~MP-6 全部依赖这一层进入 Boot 应用。

### C. 边界与生态层

#### MP-8 `BaseMapper` / `IService` / `ServiceImpl`

这个主题保留，但建议降级为“应用层增强边界”而不是卷内主干核心。原因：

- `BaseMapper` 与 SQL 注入、元数据解析直接相关，值得单开
- `IService/ServiceImpl` 更偏应用层抽象，不应抢占底层机制主线
- 其批量操作“并非真正 batch”是边界亮点，但不值得压过 SQL 注入与插件总线

#### MP-9 明确暂缓：生成器、DDL、样例与测试设施

- `mybatis-plus-generator/`：开发工具层，暂缓
- DDL 自动建表：暂缓
- 大量 sample / starter-test：作为测试与集成证据，不单开
- ActiveRecord：按需

## 四、需要修订的旧判断

### 1. `spring-boot-starter/` 不能再直接标记为“0 Java 代码”而淘汰

当前仓库里的 `spring-boot-starter/mybatis-plus-spring-boot-autoconfigure/` 有真实 Java 自动装配类，必须纳入 `MP-7`。

### 2. `M-1 SQL 自动注入机制` 过大，需要拆开“Configuration 替换 / 注册桥”和“SQL 批量注入”

否则会把入口、状态中心和运行时增强混成一团。

### 3. `M-9 BaseMapper + IService` 不宜继续当作和 SQL 注入、元数据解析同权重的核心主域

它更像应用层增强边界，应降权。

## 五、生产层候选

当前从源码与测试热点中已经能初步识别几类后续生产层候选：

- 多租户 / 数据权限 / 动态表名插件的 SQL 改写副作用
- 分页 count 优化与复杂 join 场景
- 逻辑删除与乐观锁组合边界
- 自动填充 / 雪花 ID 在批量与多线程场景下的行为
- IService 批量操作“非真正 batch”的性能误解

这些不应现在塞进主干，但应在卷级说明里明确仍缺。

## 六、建议的新阅读顺序

```text
MP-1 Configuration 替换与 Mapper 注册桥
  -> MP-2 SQL 自动注入
    -> MP-3 表元数据与 GlobalConfig
      -> MP-4 Wrapper / Lambda 条件构造
        -> MP-5 插件总线与 SQL 改写入口
          -> MP-6 具体内置增强专题（分页/乐观锁/自动填充/逻辑删除/租户/权限/安全）
            -> MP-7 Spring Boot 自动装配桥
              -> MP-8 BaseMapper / IService 应用层边界
```

## 七、方法论自检结果

- 没再把目录直接当知识域边界，而是按“替换桥、注入桥、元数据桥、插件桥、装配桥”重组。
- 已把 Spring Boot 自动装配层从淘汰项中拉回卷级结构。
- 已区分核心主干、机制补深、应用边界与生态暂缓。
- 当前仍未完成：逐域源码锚点表、Boot 自动装配类的逐文件深读、以及 MP 全卷的正式正文四件套。
