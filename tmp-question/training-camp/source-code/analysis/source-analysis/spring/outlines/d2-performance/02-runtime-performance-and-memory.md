# D-2-2 运行时性能与内存优化 — 缓存、连接池、线程池、Bean 生命周期

> 依赖 D-2-1 + C-11 DataSource + S2-12 @Cacheable + S2-10 @Async | 🟡 Working | 2 KP | [模式: 实践分析]

**读者处境**: 启动已经够快了，但运行时吞吐低、延迟高、内存涨——这时该看缓存、连接池、线程池，还是 Bean 生命周期？

### 1. 缓存与方法执行路径

场景: `@Cacheable` 命中率不高，反而增加序列化与网络开销。

关键点:
- 热点数据、读多写少才适合缓存
- `condition` / `unless` 影响缓存写入路径
- 本地缓存 vs Redis 分布式缓存的取舍

### 2. 连接池与线程池

场景: 并发请求下，DB 连接不足或线程池排队。

关键点:
- HikariCP: `maximumPoolSize` / `minimumIdle` / `connectionTimeout`
- `ThreadPoolTaskExecutor`: `corePoolSize` / `maxPoolSize` / `queueCapacity`
- `@Async` 和 `@Scheduled` 的执行器策略对吞吐的影响

### 3. Bean 生命周期与内存

场景: 应用运行久了，内存持续增长。

关键点:
- singleton 持有大对象或 prototype 引用
- request/session scope 未及时释放
- 类加载器残留（与后续 Spring Boot / 容器集成篇呼应）

关键设计: **Why 性能调优不是孤立参数表？** 缓存、连接池、线程池、Bean 生命周期都已经在前面主线里讲过；性能调优只是把这些主线重新投影成性能问题的观察面。

→ Spring 生产层两篇（运行时诊断 + 性能）全部完成。
