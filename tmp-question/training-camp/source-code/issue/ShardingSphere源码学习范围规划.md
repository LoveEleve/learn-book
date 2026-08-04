# ShardingSphere 源码学习范围规划

> **版本**: v5.5.1
> **仓库**: `/data/workspace/source-code/code/spring/shardingsphere/`
> **规模**: 5966 个源文件，15+ 子模块，parser 占 1584 文件(27%)
> **日期**: 2026-08-03

---

## 一、仓库概况

Apache ShardingSphere 是分布式数据库生态系统，提供数据分片（Sharding）、读写分离、数据加密、影子库等能力。核心是 **JDBC 层面的 SQL 解析→路由→改写→执行→结果归并 五阶段引擎**——应用层无感知，JDBC URL 切换即可实现分库分表。

**子模块清单**（仅列核心，全部审计）：

| 子模块 | 文件数 | 职责 | 状态 |
|---|---|---|---|
| `kernel/` | 928 | 核心引擎：SQL 路由(24)、改写(17)、执行(44)、归并(46)、元数据、事务、DistSQL | ✅ 已探索 |
| `features/sharding/` | 352 | 分片功能：算法(24)、策略(14)、路由(9)、重写(8) | ✅ 已探索 |
| `jdbc/` | 83 | JDBC 适配器 + ShardingSphereDataSource + Spring Boot Starter | ✅ 已探索 |
| `infra/` | 830 | 基础设施：公共工具、表达式、重写引擎、合并引擎 | ✅ 部分 |
| `parser/` | 1584 | SQL 解析器（ANTLR 生成 + 多方言 MySQL/PG/Oracle/SQLServer） | 淘汰 |
| `features/readwrite-splitting/` | 若干 | 读写分离功能 | 🟡 |
| `features/encrypt/` | 若干 | 数据加密 | 淘汰 |
| `features/shadow/` | 若干 | 影子库压测 | 淘汰 |
| `features/broadcast/` | 若干 | 广播表 | 淘汰 |
| `mode/` | 191 | 运行模式：Standalone/Cluster | 淘汰 |
| `proxy/` | 若干 | 代理模式（独立进程） | 淘汰 |
| `agent/` `db-protocol/` `distribution/` `examples/` `test/` | — | 构建/示例/测试 | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| S-1 | **ShardingSphereDataSource + 引擎入口** | ShardingSphereDataSource, ContextManager, KernelProcessor, ShardingSphereConnection | **JDBC 代理**：`ShardingSphereDataSource` 包装 `ContextManager`（管理元数据+规则+连接上下文）→ `getConnection()` 创建 `ShardingSphereConnection`；**五阶段引擎入口**：`KernelProcessor.generateExecutionContext()` 协调前三个阶段——④ `check()` SQL 兼容性检查 → ② `route()` SQLRouteEngine.route() → ③ `rewrite()` SQLRewriteEntry.rewrite() → ④ `createExecutionContext()` 构建包含 `RouteUnit` 列表的 `ExecutionContext` → ⑤ JDBC 层 ExecutorEngine 并发执行 + MergeEngine 归并；**Spring Boot**：`ShardingSphereAutoConfiguration` + YAML/Java 配置物理数据源 Map
| S-2 | **SQL 路由（Route）** | ShardingRouteEngineFactory(233行), ShardingRouteEngine, ShardingStandardRoutingEngine, ShardingComplexRoutingEngine, ShardingConditions | **8 种路由引擎类型**（非 4 种策略）：① `ShardingStandardRoutingEngine`——单表精确/范围路由（DML/DQL，binding table 场景）；② `ShardingComplexRoutingEngine`——多表笛卡尔积路由；③ `ShardingUnicastRoutingEngine`——随机单点路由（无分片条件时取第一个分片）；④ `ShardingIgnoreRoutingEngine`——非分片表跳过路由；⑤ `ShardingDatabaseBroadcastRoutingEngine`——广播到所有数据库（TCL、SET 等全局语句）；⑥ `ShardingTableBroadcastRoutingEngine`——广播到所有物理表（DDL、OPTIMIZE TABLE）；⑦ `ShardingInstanceBroadcastRoutingEngine`——广播到所有实例（CREATE TABLESPACE 等）；⑧ `ShardingDataSourceGroupBroadcastRoutingEngine`——广播到数据源组；**SQL 类型分发**：Factory 按语句类型（TCL/DDL/DAL/DCL/DML+DQL）→子类型判断→选择路由引擎；**分片条件提取**：`ShardingConditions` 从 WHERE 子句提取分片键值→`StandardShardingAlgorithm.doSharding()` 计算目标物理表/库；**Binding Table**：关联表通过一个表的分片键路由到相同数据源，避免跨库 JOIN
| S-3 | **SQL 改写（Rewrite）** | SQLRewriteEntry, ShardingSQLRewriteContextDecorator, SQLRewriteContextDecorator(SPI), RouteSQLRewriteEngine, GenericSQLRewriteEngine | **SPI 装饰器模式**：每个 Rule 通过 `SQLRewriteContextDecorator` SPI 注册改写逻辑——`decorate()` 生成 `SQLToken`（表名 Token、分页 Token、分片键 Token）→ `generateSQLTokens()` →② `GenericSQLRewriteEngine`（无路由场景，直接替换 Token）或③ `RouteSQLRewriteEngine`（有路由，每个 RouteUnit 生成独立 SQL）；**分片改写**：`ShardingSQLRewriteContextDecorator` 添加表名 Token（`t_order`→`t_order_0`）、分片键值 Token、生成键 Token（雪花 ID）；**分页改写**：`ShardingPaginationParameterRewriter` 修正跨分片 `LIMIT offset,limit` 参数；**Hint 跳过**：`hintValueContext.isSkipSQLRewrite()` 可绕过所有改写
| S-4 | **执行与归并（Execute+Merge）** | ExecutorEngine(139行), MergeEngine, ResultMergerEngine(SPI), ResultDecoratorEngine(SPI), TransparentMergedResult | **多线程执行**：`ExecutorEngine` 对每个 `RouteUnit` 提交到线程池并发执行→收集 `QueryResult` 列表→根据 `isSerialExecute` 开关决定串行/并行；**SPI 归并体系**：`MergeEngine` 按 Rule 加载 `ResultMergerEngine` SPI → `ResultMerger.merge()` 合并多分片结果集；**四种归并策略**：① `IteratorStreamMergedResult`（流式排序归并——各分片结果有序→多路归并算法 `PriorityQueue` 逐行输出）；② `GroupByStreamMergedResult`（流式分组归并）；③ `OrderByStreamMergedResult`（流式排序）；④ `TransparentMergedResult`（透传——单分片直接返回）；⑤ `DecoratorResultMergedResult`（装饰归并——`ResultDecoratorEngine` SPI 对结果集做后处理，如读写分离查询结果、加密解密）

### 🟡 扩展域（2 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| S-5 | **读写分离** | ReadwriteSplittingRule, ReadQueryLoadBalancer | 主库写、从库读自动路由——`writeAsync()`→master、`readAsync()`→slave；`DynamicReadwriteSplitting` 事务内强制主库、Hint 路由、负载均衡算法（轮询/随机/权重） |
| S-6 | **分布式事务** | ShardingSphereTransactionManager, XA/Seata/Saga 三种事务模式 | XA（2PC 两阶段提交）、Seata AT（自动补偿）、Saga（长事务补偿）——面试必问的分布式事务实现 |

---

## 三、淘汰清单

| 子模块/功能 | 文件数 | 理由 |
|---|---|---|
| `parser/` | 1584 | ANTLR 生成的 SQL AST 解析器，类似 Druid——学习引擎行为即可，不需要深入词法/语法解析 |
| `features/encrypt/` | 若干 | 数据加密脱敏，生产场景但非核心分片 |
| `features/shadow/` | 若干 | 影子库压力测试，非生产学习 |
| `features/broadcast/` | 若干 | 广播表，机制简单 |
| `mode/` | 191 | 运行模式配置（Standalone/Cluster），运维层面 |
| `proxy/` | 若干 | 代理模式（独立进程部署），my-xhs 用 JDBC 模式 |
| `agent/` `db-protocol/` `distribution/` | — | 构建/代理/分发基础设施 |
| `kernel/` 中的 authority/distsql/metadata 子模块 | 若干 | 权限管理/DistSQL 治理/元数据持久化，边缘功能 |
| `infra/` 中的大部分 | 830 | 公共工具类、SPI、配置绑定——涉及时查阅 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 4 |
| 🟡 扩展域 | 2 |
| **总域** | **6** |
| 淘汰子模块/功能 | 9+ 个 |

**Elasticsearch 确认**：`/data/workspace/source-code/code/spring/elasticsearch/` 是 ES 8.12.2 **Server 源码**（Gradle 构建），非 Java 客户端库——同 Redis 一样不纳入 Spring 学习规划。

---

## 五、学习顺序建议

```
S-1 JDBC 适配（理解 ShardingSphereDataSource 如何代理 DataSource）
  → S-2 SQL 路由（理解如何决定数据去哪个分片）
    → S-3 SQL 改写（理解逻辑表→物理表的变换）
      → S-4 执行与归并（理解跨分片结果合并）
        → S-5 读写分离 + S-6 分布式事务（按需）
```

以上规划完成，共 **4🔴+2🟡=6 域**。聚焦 ShardingSphere 的核心价值——五阶段查询引擎，5966 个文件中的 parser(1584)/infra(830)等为基础设施。
