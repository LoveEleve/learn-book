# Phase 2：企业开发内功（第 7-12 周）

## 目标
从"会用 Spring Boot"到"理解 Spring 内核"，建立数据库/RPC 底层认知。

---

## Week 7-8：Spring 内核（两周合并）

### 理论知识
- IOC 容器：`BeanDefinition` → `BeanFactory` → `ApplicationContext` 链路
- Bean 生命周期：实例化 → 属性填充 → `BeanNameAware` → `BeanFactoryAware` → `BeanPostProcessor.postProcessBeforeInitialization` → `@PostConstruct` → `InitializingBean` → `BeanPostProcessor.postProcessAfterInitialization` → 就绪 → `@PreDestroy` → `DisposableBean`
- `refresh()` 13 步源码阅读（`AbstractApplicationContext.refresh()`）
- AOP 原理：JDK 动态代理（`Proxy.newProxyInstance`）vs CGLIB（`Enhancer`）
- 切面执行链：`ReflectiveMethodInvocation` → 环绕通知 → 前置通知 → 目标方法 → 后置通知 → 返回/异常通知
- 事务管理：`@Transactional` 失效场景（非 public/自调用/异常类型不匹配/多线程）
- 事务传播行为：REQUIRED/REQUIRES_NEW/NESTED 源码实现

### 源码阅读清单
- `org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean()`
- `org.springframework.context.support.AbstractApplicationContext.refresh()`
- `org.springframework.aop.framework.JdkDynamicAopProxy.invoke()`
- `org.springframework.transaction.interceptor.TransactionInterceptor.invoke()`
- `org.springframework.transaction.support.AbstractPlatformTransactionManager`

### 实践任务
1. 手写 Mini-Spring（~2000 行，分模块）：
   - `MiniApplicationContext.java` — IOC 容器
   - `MiniBeanFactory.java` — Bean 工厂
   - `MiniJdkDynamicAopProxy.java` — JDK 动态代理 AOP
   - `MiniTransactionManager.java` — 事务管理
2. 写一个 BeanPostProcessor 记录所有 Bean 的初始化耗时（`BeanTimingPostProcessor.java`）
3. 分析 10 个 `@Transactional` 失效场景并写单元测试验证

### 输出
- 博客：《Spring Bean 的 13 个生命周期节点》
- 速查表：`@Transactional` 失效场景速查表

### 必背流程图
- Spring `refresh()` 13 步时序图
- Bean 生命周期完整链路
- AOP 切面执行链
- 事务提交/回滚时序图

---

## Week 9：Spring Boot 3.x 自动装配

### 理论知识
- `@SpringBootApplication` → `@EnableAutoConfiguration` → `AutoConfigurationImportSelector` → `spring.factories` / `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 条件装配：`@ConditionalOnClass` / `@ConditionalOnBean` / `@ConditionalOnMissingBean` / `@ConditionalOnProperty`
- Spring Boot 3.x 新特性：
  - **Jakarta EE 迁移**：`javax.*` → `jakarta.*`，影响 Servlet / JPA / Validation / Mail 等
  - **AOT 编译**：提前编译 Bean 定义，减少启动时间
  - **ProblemDetail**（RFC 7807）：标准化错误响应
  - **Observability API**（Micrometer 1.11+）：统一 Trace/Metrics/Log

### 源码阅读清单
- `org.springframework.boot.autoconfigure.AutoConfigurationImportSelector.selectImports()`
- `org.springframework.boot.autoconfigure.condition.OnClassCondition`
- `org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory`

### 实践任务
1. 写一个自定义 Starter（`metrics-spring-boot-starter`，~200 行）：自动上报接口耗时到 Prometheus
2. 将旧 Spring Boot 2.x 项目迁移到 3.3，记录 javax→jakarta 改动清单
3. 启用 AOT 编译，对比启动时间（JIT vs AOT）

### 输出
- 博客：《Spring Boot 2.x → 3.3 迁移实战：javax→jakarta 全记录》

---

## Week 10：MySQL 内核

### 理论知识
- **索引**：B+Tree 结构（页/槽/记录）、聚簇索引 vs 非聚簇索引、覆盖索引、索引下推（ICP）
- **最左前缀原则**：联合索引 `(a,b,c)` 的命中规则
- **MVCC**：undo log 版本链 + ReadView（`m_ids`/`min_trx_id`/`max_trx_id`/`creator_trx_id`）
- **Redo Log**：两阶段提交（prepare → binlog → commit）、WAL
- **锁**：行锁（Record Lock）/ 间隙锁（Gap Lock）/ 临键锁（Next-Key Lock）
- **Explain**：`id`/`select_type`/`type`（const→eq_ref→ref→range→index→ALL）/`key`/`rows`/`Extra`

### 源码阅读清单
- InnoDB B+Tree 插入/查找流程（了解即可，C 源码）
- `show engine innodb status` 输出解读

### 实践任务
1. 用 MySQL 官方 `employees` test_db 做 10 条 SQL 优化（从 >1s 降到 <50ms），记录 Explain 对比
2. 模拟死锁：两个事务交叉更新同一行，用 `show engine innodb status` 查看死锁日志
3. 验证 MVCC：在 RR 隔离级别下，两个事务交叉查询，验证快照读

### 输出
- 速查表：《Explain 输出字段速查表》
- 博客：《10 条慢 SQL 的优化之路：从 >1s 到 <50ms》

### 检验标准
- [ ] 能画出 MVCC 的 ReadView 判断逻辑
- [ ] 能用 Explain 分析任意 SQL 的执行计划

---

## Week 11：MyBatis 源码与插件机制

### 理论知识
- 核心执行链：`SqlSession` → `Executor` → `StatementHandler` → `ParameterHandler` → `ResultSetHandler`
- 一级缓存：SqlSession 级别，默认开启，失效场景（不同 SqlSession/清空/增删改）
- 二级缓存：Mapper 级别，跨 SqlSession，需显式配置 `<cache/>`
- 插件机制：4 个可拦截点（Executor/ParameterHandler/StatementHandler/ResultSetHandler）
- 插件链：`InterceptorChain.pluginAll()` 层层代理

### 源码阅读清单
- `org.apache.ibatis.session.defaults.DefaultSqlSession.selectList()`
- `org.apache.ibatis.executor.BaseExecutor.query()`
- `org.apache.ibatis.plugin.InterceptorChain.pluginAll()`
- `org.apache.ibatis.executor.statement.StatementHandler`

### 实践任务
1. 手写 MyBatis 慢 SQL 拦截插件（`SlowSqlInterceptor.java`，~80 行）：记录超过阈值的 SQL
2. 手写 MyBatis 分表插件（`TableShardInterceptor.java`，~120 行）：根据 Sharding 注解改写 SQL 表名

### 输出
- 博客：《MyBatis 插件机制：4 个可拦截点的实战应用》

---

## Week 12：Redis 内核

### 理论知识
- **核心数据结构**：
  - String：SDS（Simple Dynamic String），二进制安全，预分配
  - List：3.2 前 ziplist+linkedlist，3.2 后 quicklist
  - Hash：ziplist + dict。ziplist 紧凑存储，dict 为哈希表
  - Set：intset + dict。全整数时用 intset
  - ZSet：ziplist + skiplist + dict。跳表实现 O(logN) 范围查找
- **持久化**：
  - RDB：全量快照，`bgsave` fork 子进程，Copy-On-Write
  - AOF：追加写命令，AOF 重写（`bgrewriteaof`）
  - 混合持久化（4.0+）：RDB 头 + AOF 尾
- **高可用**：
  - 主从：全量同步（RDB）+ 增量同步（replication buffer）
  - Sentinel：主观下线→客观下线→Leader 选举→故障转移
  - Cluster：16384 槽位，MOVED/ASK 重定向，Gossip 协议
- **线程模型**：单线程 Reactor → 6.0 多线程 IO（读写）+ 单线程执行
- **过期策略**：惰性删除 + 定期删除

### 源码阅读清单
- Redis 源码（C，了解即可）：`sds.h`/`ziplist.c`/`quicklist.c`/`skiplist.c`

### 实践任务
1. 手写 Mini-Redis（`MiniRedis.java`，~500 行）：
   - 支持命令：GET/SET/EXPIRE/DEL/INCR/LPUSH/RPUSH/LRANGE
   - 数据结构：HashMap + 跳表 + LinkedList
   - 过期机制：惰性删除
2. 搭建 Redis Cluster（3 主 3 从），测试 MOVED/ASK 重定向
3. 用 `redis-benchmark` 对比单机 vs Cluster 的吞吐量

### 输出
- 博客：《Redis 持久化：RDB、AOF、混合持久化的实战对比》

### 检验标准
- [ ] 能手绘 skip list 的插入过程
- [ ] 能解释 Redis Cluster 的 MOVED vs ASK 区别
- [ ] 能手写 Mini-Redis 的核心数据结构

---

## Phase 2 总结

### 核心交付物
- [ ] Mini-Spring（IOC + AOP + 事务，~2000 行）
- [ ] 自定义 Starter（~200 行）
- [ ] MyBatis 插件 × 2（~200 行）
- [ ] Mini-Redis（~500 行）
- [ ] 4 篇深度博客 + 2 份速查表

### 与 my-xhs 的关联点
- Spring 源码 → 理解 my-xhs 的 Bean 配置和 AOP 切面
- MySQL 索引/锁 → 理解 my-xhs 订单表的分库分表设计
- MyBatis 插件 → 理解 my-xhs 的 SqlGuardInterceptor 实现
- Redis 源码 → 理解 my-xhs 库存分桶的 Lua 脚本和双数据源设计
