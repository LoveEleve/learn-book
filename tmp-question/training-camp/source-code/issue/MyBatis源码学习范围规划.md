# MyBatis 源码学习范围规划

> **版本**: v3.5.16
> **仓库**: `/data/workspace/source-code/code/spring/mybatis/`
> **规模**: 386 个源文件，单模块
> **日期**: 2026-08-03

---

## 一、仓库概况

MyBatis 是 Java 持久层 ORM 框架。核心是 **SqlSession → Executor → StatementHandler → ResultSetHandler** 四层执行链 + **MapperProxy 动态代理** + **Configuration 全局配置**。Mybatis-Plus 在此之上替换了 Configuration/MapperRegistry/MapperAnnotationBuilder。

**核心包结构**：

| 包 | 职责 | 关键类 |
|---|---|---|
| `session/` | SqlSession/SqlSessionFactory——对外 API | SqlSession, SqlSessionFactory, Configuration |
| `binding/` | Mapper 代理——MapperProxy/MapperMethod | MapperProxy, MapperMethod, MapperRegistry |
| `builder/` | XML/注解解析——XMLConfigBuilder/XMLMapperBuilder | XMLConfigBuilder, XMLMapperBuilder, MapperAnnotationBuilder |
| `executor/` | 执行器——BaseExecutor/Simple/Reuse/Batch/Caching | BaseExecutor, SimpleExecutor, CachingExecutor |
| `mapping/` | 映射元数据——MappedStatement/ResultMap | MappedStatement, ResultMap, ParameterMap |
| `scripting/` | 脚本引擎——XMLLanguageDriver/dynamic SQL | XMLLanguageDriver, SqlNode(IfNode/ForEachNode) |
| `plugin/` | 插件拦截器——Interceptor/InterceptorChain | Interceptor, Plugin |
| `cache/` | 缓存——LRU/FIFO/Transactional | LruCache, TransactionalCache, CacheKey |
| `type/` | 类型处理——TypeHandler | TypeHandler, TypeHandlerRegistry |
| `reflection/` | 反射——MetaObject/Reflector | MetaObject, Reflector |
| `transaction/` | 事务——JdbcTransaction/ManagedTransaction | JdbcTransaction, SpringManagedTransaction |

---

## 二、知识域规划

### 🔴 核心域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| M-1 | **启动与 Configuration** | XMLConfigBuilder, XMLMapperBuilder, Configuration, SqlSessionFactoryBuilder | **XML 启动**：`SqlSessionFactoryBuilder.build(reader)` → `XMLConfigBuilder.parse()` 解析 `mybatis-config.xml` → 填充 `Configuration`(environments/mappers/settings/plugins/typeHandlers/mappedStatements)；**Mapper 加载**：`XMLMapperBuilder.parse()` → 解析每个 `XXXMapper.xml` → `MappedStatement`(id/sqlCommandType/parameterMap/resultMap/SqlSource)→存入 `Configuration.mappedStatements`(StrictMap 防重)；**Configuration**：全局唯一——持有所有 MappedStatement、缓存、类型处理器、拦截器链 |
| M-2 | **MapperProxy 动态代理** | MapperProxy, MapperMethod, MapperRegistry, MapperProxyFactory | **Mapper 接口代理**：`sqlSession.getMapper(UserMapper.class)` → `MapperRegistry.getMapper()` → `MapperProxyFactory.newInstance()` → `Proxy.newProxyInstance()` → `MapperProxy.invoke()` -> 区分 default 方法/MapperMethod；**MapperMethod**：封装 `SqlCommand`(type+id) + `MethodSignature`(返回类型/参数)→`execute(args)` → 根据 `SqlCommandType`(INSERT/UPDATE/DELETE/SELECT) 分发→`sqlSession.insert/update/delete/selectOne/selectList` |
| M-3 | **四层执行链** | BaseExecutor, SimpleExecutor, CachingExecutor, RoutingStatementHandler, DefaultResultSetHandler | **执行链**：`SqlSession.selectOne()` → `Executor.query(ms, parameter, ...)` → `CachingExecutor`(二级缓存)→`BaseExecutor`(一级缓存+事务管理)→`StatementHandler.prepare()`(创建 Statement+参数设置)→`ParameterHandler.setParameters()`(类型转换)→`StatementHandler.query()`(执行 SQL)→`ResultSetHandler.handleResultSets()`(ResultSet→POJO)；**三种 Executor**：`SIMPLE`(每次新建 Statement)、`REUSE`(复用 Statement)、`BATCH`(JDBC 批处理) |
| M-4 | **动态 SQL 与插件拦截** | SqlNode(IfNode/ForEachNode/ChooseNode/TrimNode), Interceptor, Plugin | **动态 SQL**：`<if test="...">` `→IfNode`、`<foreach>`→`ForEachNode`、`<trim>/<where>/<set>`→`TrimNode`——递归 `apply()` 生成 DynamicSqlSource→BoundSql(最终 SQL+参数列表)；**Plugin 链**：`@Intercepts(@Signature(type=Executor.class, method="query", args={...}))` → `InterceptorChain.pluginAll(target)` → `Plugin.wrap()` JDK 动态代理包装——可拦截 Executor/StatementHandler/ParameterHandler/ResultSetHandler 4 个层级——分页插件(PageHelper)在此层实现 |

### 🟡 扩展域（1 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| M-5 | **一级/二级缓存** | PerpetualCache, LruCache, TransactionalCache, CachingExecutor, CacheKey | **一级缓存**(默认开启)：`BaseExecutor.localCache`——同一个 SqlSession 内相同的 stmt+params→返回缓存结果→update 方法清空；**二级缓存**(namespace 级)：`CachingExecutor` 装饰——SQL 执行前查 `TransactionalCacheManager`→执行后入库→`commit()` 时刷新到真实 Cache；**CacheKey**：`stmtId + rowBounds + boundSql + params + environmentId` 五元组计算唯一键 |

---

## 三、淘汰清单

| 包/功能 | 理由 |
|---|---|
| `annotations/` | 注解定义（@Select/@Insert/@Update/@Delete）|
| `datasource/` | 数据源工厂（JNDI/Pooled/Unpooled—Spring Boot 用 HikariCP）|
| `io/` `lang/` `parsing/` | 工具类 |
| `logging/` | 日志适配 |
| `jdbc/` | JDBC 工具 |
| `cursor/` | Cursor 游标 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 4 |
| 🟡 扩展域 | 1 |
| **总域** | **5** |

---

## 五、学习顺序建议

```
M-1 启动与Configuration（理解 XML→Configuration 构建）
  → M-2 MapperProxy 动态代理（理解接口如何变成数据库操作）
    → M-3 四层执行链（理解 Executor→StatementHandler→ResultSetHandler）
      → M-4 动态SQL+Plugin（理解 <if>/<foreach> 和分页插件原理）
        → M-5 缓存体系
```

以上规划完成，共 **4🔴+1🟡=5 域**。MyBatis 是单模块 386 文件——与 MyBatis-Plus 互补（MP 替换 MyBatis Configuration/MapperRegistry/MapperAnnotationBuilder）。
