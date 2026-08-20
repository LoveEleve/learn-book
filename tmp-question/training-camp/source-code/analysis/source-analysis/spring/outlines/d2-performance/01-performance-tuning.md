# D-2 Spring 性能调优 — 启动加速、运行时优化、内存优化

> 依赖全部主干层 + D-1 运行时诊断 | 🟡 Working | 3 KP | [模式: 实践分析]

**读者处境**: 应用启动慢怎么办？Bean 创建耗时长怎么优化？`@Cacheable` 缓存命中率低怎么调？内存占用高怎么排查？

### 1. 启动加速

场景: Spring Boot 应用启动从 30 秒优化到 5 秒。

优化手段:
- `spring.main.lazy-initialization=true`：全局懒加载，启动时不创建非必需 Bean
- `@Lazy`：单个 Bean 延迟初始化
- AOT / Native Image：编译时预处理，运行时跳过解析（Spring Boot 3 + GraalVM）
- `spring.context.index`：组件索引，跳过 classpath 扫描
- 减少 `@ComponentScan` 范围：只扫描必要的包

关键设计: **Why 懒加载能加速启动？** 懒加载把 Bean 的创建推迟到首次使用时，启动时只创建必需的 Bean（如 DataSource、WebServer）。但代价是首次请求会更慢。

### 2. 运行时优化

场景: 高并发下响应延迟高，吞吐量低。

优化手段:
- `@Cacheable`：缓存热点数据，减少 DB 查询
- `@Async`：把耗时操作异步化，释放请求线程
- `@Transactional` 传播行为选择：避免不必要的事务嵌套
- 连接池调优：`HikariCP` 的 `maximumPoolSize`、`minimumIdle`、`connectionTimeout`
- 线程池调优：`ThreadPoolTaskExecutor` 的 `corePoolSize`、`maxPoolSize`、`queueCapacity`

关键设计: **Why `@Cacheable` 不是万能的？** 缓存命中率低时，缓存反而增加开销（序列化 / 反序列化、内存占用）。`@Cacheable` 适合读多写少、数据变化不频繁的场景。

### 3. 内存优化

场景: 应用运行一段时间后，内存持续增长。

优化手段:
- Bean 作用域选择：默认 singleton 适合无状态 Bean；有状态 Bean 考虑 prototype / request / session
- `@Scope("prototype")` + `@Lookup`：避免 singleton 持有 prototype Bean 的引用
- Session / Request Scope：Web 场景下按需创建，请求结束后自动释放
- 连接池调优：`minimumIdle` 太大会浪费连接资源
- 缓存调优：`@Cacheable` 的 TTL / 最大条目数

关键设计: **Why singleton Bean 持有 prototype Bean 会出问题？** singleton Bean 在容器启动时创建，持有的 prototype Bean 引用在创建时就固定了。后续每次调用都返回同一个 prototype 实例，违背了 prototype 的"每次新建"语义。解决：`@Lookup` 方法注入或 `ObjectFactory<T>`。

### 4. 诊断与监控

场景: 线上问题排查。

工具:
- Spring Boot Actuator：`/actuator/health`、`/actuator/metrics`、`/actuator/beans`
- `ConditionEvaluationReport`：`/actuator/conditions`
- JVM 工具：`jstack`、`jmap`、`jstat`
- APM：Micrometer + Prometheus + Grafana

→ Spring 完整卷主干层 + 规范层 + 集成层 + 机制补深层 + 生产层全部完成。
