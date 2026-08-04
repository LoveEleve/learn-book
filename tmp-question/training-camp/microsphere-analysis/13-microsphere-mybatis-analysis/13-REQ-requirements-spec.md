# 13-REQ：MyBatis-Plus 后端增强需求规格（v2，MP 源码验证）

> v2 更新(Explore-15)：REQ 4项发散全部MCP确认有效 + 新增6个bug(D02-D06) + MP @Intercepts 5签名修正
>
> 需求分三类：
> 1. **已实现需求**（REQ-001~008，8 项）——MP 已有能力
> 2. **待改进+bug**（REQ-D01~D06，6 项）
> 3. **全新发散**（REQ-N01~N04，4 项）——MP源码+grep确认全部无
>
> **⚠️ MP @Intercepts 声明的是 5 个签名**（不是之前写的 8 个）：StatementHandler.prepare + getBoundSql + Executor.update + query×2
>
> **基准环境**：Java 17+，MyBatis 3.5.x+，MyBatis-Plus 3.5.x+

---

## 项目定位

**MyBatis-Plus 是 MyBatis 的主流后处理增强框架**——覆盖了从 CRUD 生成、Lambda 查询、分页、乐观锁到 SQL 拦截的全部模块。其中与 microsphere-mybatis 直接对应的部分是 **`@Intercepts` + `MybatisPlusInterceptor` 的多层拦截体系**——覆盖 `Executor` 和 `StatementHandler` 两级，比 microsphere 的纯 Executor 级 Filter Chain 范围更广。

**与 microsphere-mybatis ExecutorFilter 的同一位置对比**：

| | microsphere ExecutorFilter | MyBatis-Plus InnerInterceptor |
|---|---|---|
| 拦截层级 | 仅 Executor（10 方法） | Executor + StatementHandler（8 方法） |
| 模式 | Filter Chain（`chain.xxx()` 显式传递） | Plugin 链（MyBatis 原生 `Interceptor`） |
| 能改 SQL | 不能——`BoundSql` 是只读的 | 能——`beforePrepare/beforeGetBoundSql` 可替换 SQL |
| 内置实现 | Logging 一个 | **10 个**（分页/乐观锁/防全表/数据权限/租户/动态表名/非法SQL拦截） |
| 生态 | microsphere 内部 | GitHub 16k+ Star，生产广泛使用 |

**源码信息**：`/data/workspace/source-code/code/spring/mybatis-plus/`（399 主源文件）

---

## 一、多层拦截框架

### REQ-001：Executor + StatementHandler 两级拦截

**问题**：MyBatis 原生 `@Intercepts + Plugin.wrap()` 只能拦截单个类型（Executor/StatementHandler/ParameterHandler/ResultSetHandler）的特定方法。像"分页"这种操作需要先拦截 `Executor.query()` 知道要分页，然后拦截 `StatementHandler.prepare()` 改写 SQL 加上 LIMIT——需要两级拦截。单一级别做不到。

**产出**：
- `MybatisPlusInterceptor`：`@Intercepts` 声明覆盖 Executor（3 个 `@Signature`）+ StatementHandler（2 个 `@Signature`）共 5 个拦截点——一个拦截器同时挂载两级
- `InnerInterceptor` SPI：6 个回调——`willDoQuery/beforeQuery/willDoUpdate/beforeUpdate/beforePrepare/beforeGetBoundSql`
- 10 个内置实现（见 REQ-002~004）

**状态**：[已验证实现] 📝 **待编写原理文档**

---

### REQ-002：分页拦截器

**产出**：`PaginationInnerInterceptor` —— `willDoQuery` 读取 `IPage` 参数 → 拦截 `beforeQuery` 统计总行数（发 COUNT SQL）→ `beforePrepare` 改写 SQL 加 LIMIT/OFFSET → 返回分页结果。自动检测数据库方言（MySQL/Oracle/PostgreSQL/达梦等 12 种）生成对应语法。

**状态**：[已验证实现] 📝 **待编写原理文档**

---

### REQ-003：数据安全与审计拦截

**产出**：

| 拦截器 | 功能 | 触发钩子 |
|--------|------|---------|
| `BlockAttackInnerInterceptor` | 防全表 UPDATE/DELETE——不带 WHERE 条件拦截 | `beforeUpdate` |
| `IllegalSQLInnerInterceptor` | 检测 SQL 中的危险操作（DROP/TRUNCATE/ALTER） | `beforePrepare` |
| `DataChangeRecorderInnerInterceptor` | SQL 变更记录——改动前后值 diff | `beforeUpdate` + `after` |
| `DataPermissionInterceptor` | 数据权限过滤——自动给查询加 `WHERE org_id = ?` | `beforeQuery` |

**状态**：[已验证实现] 📝 **待编写原理文档**

---

### REQ-004：多租户与表路由

**产出**：
- `TenantLineInnerInterceptor`：自动给 SQL 加租户过滤 `WHERE tenant_id = ?`——支持忽略特定表/特定 MappedStatement
- `DynamicTableNameInnerInterceptor`：运行时动态替换表名——`user` → `user_202401` 按月分表
- `BaseMultiTableInnerInterceptor`：多拦截器基础抽象

**状态**：[已验证实现] 📝 **待编写原理文档**

---

### REQ-005：乐观锁

**产出**：`OptimisticLockerInnerInterceptor` —— `beforeUpdate` 检测实体类 `@Version` 字段 → 在 WHERE 子句中加 `version = #{version}` → 更新后检测 `rows == 1`，为 0 抛 `OptimisticLockerException`。

**状态**：[已验证实现] 📝 **待编写原理文档**

---

## 二、Core 层基础能力

### REQ-006：BaseMapper + LambdaQueryWrapper 自动 CRUD

**产出**：
- `BaseMapper<T>`：18 个 CRUD 方法——`insert/updateById/deleteBatchIds/selectById/selectList/selectPage`
- `LambdaQueryWrapper<T>` / `LambdaUpdateWrapper<T>`：类型安全的条件构造器——`lambdaQuery().eq(User::getName, "Tom").gt(User::getAge, 18)`
- `AbstractWrapper` 下 `select/orderBy/groupBy/having` 链式 API

**状态**：[已验证实现] 📝 **待编写原理文档**

---

### REQ-007：SQL Injector 自定义方法注入

**产出**：
- `AbstractMethod`：SQL 模板方法——`injectMappedStatement()` 在启动时向 MyBatis Configuration 中注册自定义 SQL
- 内置注入方法：`SelectById/SelectList/Insert/Update` 等 18 个
- 扩展机制：继承 `DefaultSqlInjector` + 重写 `getMethodList()` 添加自定义方法

**状态**：[已验证实现] 📝 **待编写原理文档**

---

### REQ-008：自动填充与主键策略

**产出**：
- `MetaObjectHandler`：`insertFill/updateFill` 自动填充 `createTime/updateTime/createBy` 等字段
- `IdType`：8 种主键策略（AUTO/ASSIGN_ID 雪花算法/ASSIGN_UUID/INPUT 手动）
- `@TableLogic`：逻辑删除——`deleteById` 自动转为 `update SET deleted=1`

**状态**：[已验证实现] 📝 **待编写原理文档**

---

## 三、对比 microsphere-mybatis 的差异

| 维度 | microsphere | MyBatis-Plus |
|------|:---:|:---:|
| 覆盖 Executor 全方法 | ✅ 10 个（含 commit/rollback/close） | ❌ 只挂 Executor.query 和 update |
| SQL 改写 | ❌ 不能 | ✅ beforePrepare 可改写 |
| 内置拦截器 | ❌ 无 | ✅ 10 个 |
| 分页/乐观锁/租户 | ❌ 无 | ✅ 有 |
| 模式灵活度 | ✅ Filter Chain 可控制链 | InnerInterceptor 链通过循环实现 |

**唯一微球更强的地方**：Executor 全方法覆盖（commit/rollback/close 可拦截）——可用于事务级审计。但 MP 的 InnerInterceptor 通过 `@Signature` 也可扩展拦截范围，这不是 MP 做不到而是没做。

---

## 四、待改进（REQ-DXX）

### REQ-D01：InnerInterceptor 缺少 commit/rollback 钩子

**方案**：在 `InnerInterceptor` 中增加 `afterCommit/afterRollback` 回调——与 `Executor.commit/rollback` 挂钩——用于事务级审计（如记录"这条 UPDATE 已提交"）。

**状态**：[待添加] — 当前只有 `beforeQuery/beforeUpdate/beforePrepare/beforeGetBoundSql`，缺失事务提交和回滚钩子。

---

### REQ-D02：适配器吞异常

`InterceptorsExecutorFilterAdapter` 的 `createCacheKey/deferLoad/getTransaction` 三处 `catch(Throwable){}` 吞异常不 rethrow——与 update/query 的 catch 后 rethrow 不一致。createCacheKey 失败返回 null → CachingExecutor 后续 NPE。

### REQ-D03：SQL 改写 + 二级缓存 = CacheKey 不一致

CachingExecutor 在 delegate.query 前用原始 BoundSql 算 CacheKey，filter 在其内层改 SQL 后 key 不更新 → 缓存碰撞/脏读。MP 先 `beforeQuery` 改写 SQL 再 `executor.createCacheKey(ms,parameter,rowBounds,boundSql)` 正确处理。

### REQ-D04：异常类型强制包装

`ExecutorFilterChain.applySQL` 把所有非 SQLException 异常经 `ExceptionUtils.wrap` 转成 SQLException——filter 抛 RuntimeException 类型被改变。

### REQ-D05：4 方法绕过拦截链

`flushStatements/isCached/clearLocalCache/isClosed` 直接 delegate——`flushStatements` 是 BATCH 执行器刷 SQL 入口，BATCH 模式 update 绕过过滤链。

### REQ-D06：AutoConfiguration 空壳

`MyBatisAutoConfiguration` 只挂 @EnableMyBatisExtension 无实际逻辑；`MyBatisCloudAutoConfiguration` 注释声称 HasFeatures 但零代码。

---

## 五、发散需求

### REQ-N01：InnerInterceptor 级 Micrometer 指标集成

**生产痛点**：运维想知道"PaginationInnerInterceptor 在多少条 SQL 上生效了？多租户拦截器改写了多少条 SQL？"——当前全部不可见。分页器/多租户/乐观锁的运行时状态是黑盒。

**产出**：`MybatisPlusMetrics` —— 自动统计每个 `InnerInterceptor` 的 `beforeQuery/beforeUpdate/beforePrepare` 执行次数 + SQL 改写计数 → Micrometer Counter。`tenant_line_applied_total/optimistic_lock_total/pagination_count_total`

**状态**：[待实现]

---

### REQ-N02：PaginationInnerInterceptor 分页慢查询告警

**生产痛点**：分页翻到 `page=1000` 时 MySQL `LIMIT 10000, 20` 扫了 10020 行——与 DBA 说这是全表扫描级的开销。运维不知道哪些分页查询正在扫描大偏移量。

**产出**：`PaginationSlowLimitDetector` —— `beforeQuery` 检测 `offset > threshold`（如 5000）→ 日志 WARN + Micrometer Counter `pagination_large_offset_total`——DBA 可据此建议前端改为游标分页。

**状态**：[待实现]

---

### REQ-N03：动态 SQL 限流

**生产痛点**：某个 `WHERE` 条件组合导致 SQL 瞬时慢查询、拖垮数据库——运维想"拦截传了这个参数的 SQL 请求，降级返回缓存数据"——MP 的拦截框架已有全部信息但缺少限流能力。

**产出**：`RateLimitInnerInterceptor` —— `willDoQuery` 中检测 SQL + 参数组合的 QPS → 超过阈值时 `return false`（跳过 SQL 执行，返回降级数据或空列表）。

**状态**：[待实现]

---

### REQ-N04：乐观锁冲突次数追踪

**生产痛点**：`OptimisticLockerInnerInterceptor` 检测到版本冲突抛了异常——但一个月发生了多少次冲突？哪些表/用户高频冲突？运维需要这个数据来评估并发设计是否合理。

**产出**：`OptimisticLockConflictMonitor` —— 拦截乐观锁冲突事件 → `Counter("optimistic_lock_conflict_total", "table", "method")` Micrometer 指标 + 冲突详情日志（含表名、方法名、参数）。

**状态**：[待实现]

---

## 六、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| — | 2026-08-03 | REQ 文档编写（MyBatis-Plus 视角 v1），替换原 microsphere-mybatis 版 |
