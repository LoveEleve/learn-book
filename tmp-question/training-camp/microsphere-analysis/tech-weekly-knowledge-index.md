# 「小马哥技术周报」知识点提取与发散

> 源仓库：https://github.com/mercyblitz/tech-weekly
> 共 84 期（2018.09 - 2026.07），本文件仅提取**永恒知识点**并发散补充，过滤过时内容和非技术期数。
> 标注说明：`[源:第N期]` 表示知识点来源期数；`[发散]` 表示补充的延伸知识点。

---

## 过滤说明

**已排除的期数（过时/非技术/无内容）：**

| 期数 | 排除原因 |
|------|---------|
| 1-7 | Spring Cloud Alibaba 早期介绍，Nacos/Sentinel 基础用法已众所周知 |
| 10-13 | Dubbo 基础使用/OPS/Arthas 介绍，工具已大幅演进 |
| 20 | Fescar 早期版本（已改名 Seata，架构重写） |
| 27, 41-45, 83-84 | 仅有录播链接，无内容材料 |
| 39-40 | Velocity 已被 Spring 废弃 |
| 47-48 | Java 培训/行业讨论，非技术 |
| 52 | 职场规划为主（部分技术点已提取到对应领域） |
| 61 | 行业现状，非技术 |
| 64 | 热门新闻事件 |
| 66 | 跨年直播，行业闲聊 |
| 68 | 期权维权，非技术 |
| 71 | 重写 Nacos API，过于具体 |
| 76 | 工作计划，非技术 |

**保留但需注意时效的期数：**

| 期数 | 时效说明 |
|------|---------|
| 8-9 | Spring Cloud Stream/Bus 概念仍有效，但实现已演进到 Spring Cloud Stream 4.x |
| 14-19 | Dubbo+Spring Cloud 整合的架构思想仍有效，但 API 已变化 |
| 22 | 注册中心对比原理仍有效，但各产品版本已更新 |
| 26-32 | Dubbo 外部化配置/Spring Boot 整合原理仍有效 |
| 37 | RSocket 部分已过时（Spring 已转向其他方向） |
| 67 | Dubbo 云原生挑战赛，HTTP/3 方向仍前沿 |

---

## 一、Spring IoC 容器核心原理

### 1.1 Bean 生命周期完整链路 `[源:第24,49,50,55,56,73,78期]`

**核心知识点：**

- BeanDefinition 解析 -> 注册 -> 合并 -> Class 加载 -> 实例化前（`InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation`）-> 实例化 -> 实例化后 -> 属性赋值（`postProcessProperties`）-> Aware 回调 -> 初始化前（`postProcessBeforeInitialization`）-> 初始化（`afterPropertiesSet`/`@PostConstruct`）-> 初始化后（`postProcessAfterInitialization`）-> 完成 -> 销毁前 -> 销毁 -> GC

- **BeanDefinition 合并机制**：`MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition` 在实例化前回调，用于缓存注入元信息（`InjectionMetadata`）。合并是指子 BeanDefinition 与父 BeanDefinition 的属性合并，生成 `RootBeanDefinition`

- **Aware 接口回调顺序**（由 `ApplicationContextAwareProcessor` 在 `postProcessBeforeInitialization` 中统一调用）：
  BeanNameAware -> BeanClassLoaderAware -> BeanFactoryAware -> ApplicationContextAware -> ApplicationEventPublisherAware -> EnvironmentAware -> ResourceLoaderAware -> ImportAware

- **Aware vs @Autowired**：大多数 Aware 接口可用 `@Autowired` 替代，但 Aware 是传统接口植入方式，在 BeanPostProcessor 注册阶段就可获取容器引用，而 `@Autowired` 依赖 `AutowiredAnnotationBeanPostProcessor`，时序更晚

- **`SmartInitializingSingleton`**：所有非 Lazy 单例 Bean 预实例化后的回调，`afterSingletonsInstantiated` 在 `preInstantiateSingletons` 末尾触发。与 `@PostConstruct` 的区别：后者在单个 Bean 初始化后触发，前者在所有 Bean 都 Ready 后触发

- **`@EventListener` 底层**：`DefaultEventListenerFactory` -> `ApplicationListenerMethodAdapter`，在 `afterSingletonsInstantiated` 时注册。这意味着事件监听器注册时机晚于 Bean 初始化——如果 Bean 初始化期间发布事件，`@EventListener` 可能接收不到 `[源:第73期]`

`[发散]` **小马哥未深入的点：**

- **BeanPostProcessor 注册时序陷阱**：`BeanPostProcessor` 本身也是 Bean，但它们必须在普通 Bean 初始化之前注册。Spring 通过 `registerBeanPostProcessors` 阶段单独处理，这意味着 BeanPostProcessor 中的 `@Autowired` 字段不会被同批次的其他 BeanPostProcessor 处理（因为它们还没注册完）

- **`BeanPostProcessor` 的 N×M 问题**：N 个 Bean × M 个 BeanPostProcessor = N×M 次回调。当 Bean 数量大时，这是启动性能瓶颈。`[发散]` Spring 5.x 引入了 `BeanPostProcessorChecker` 来检测未被任何 BeanPostProcessor 处理的 Bean，但这只是诊断手段而非优化

- **循环依赖的三级缓存**：`singletonObjects`（一级，完整 Bean）、`earlySingletonObjects`（二级，半成品 Bean）、`singletonFactories`（三级，ObjectFactory）。`[发散]` 三级缓存的存在是为了处理 AOP 代理的循环依赖——如果只有二级缓存，提前暴露的半成品 Bean 无法被代理。Spring 6.x 默认关闭了循环依赖（`spring.main.allow-circular-references=false`），因为循环依赖本身是设计缺陷

- **`FactoryBean` 的 `getObject` 时序**：`FactoryBean#getObject` 在 `postProcessAfterInitialization` 之后调用。`[发散]` 如果 BeanPostProcessor 在 `postProcessAfterInitialization` 中返回了代理对象，而 FactoryBean 也返回了对象，则 BeanPostProcessor 的代理会覆盖 FactoryBean 的结果——但实际上 Spring 对 FactoryBean 有特殊处理，`postProcessAfterInitialization` 对 FactoryBean 创建的 Bean 不生效

### 1.2 BeanDefinition 类型体系 `[源:第51期]`

- **GenericBeanDefinition**：通用定义，可设 parentName
- **RootBeanDefinition**：合并后的最终定义，不应再有 parentName
- **AnnotatedBeanDefinition**：注解驱动的子类型
  - `ScannedGenericBeanDefinition`：`@ComponentScan` 扫描结果
  - `AnnotatedGenericBeanDefinition`：`@Configuration` / `@Bean` 注册
  - `ConfigurationClassBeanDefinition`：`@Bean` 方法产生的定义

`[发散]` **小马哥未覆盖的点：**

- **BeanDefinition 的 `setAttribute` 黑科技**：可以临时存储与当前 Bean 定义相关的属性，不影响 Bean 实例化/初始化，但可辅助 Bean 初始化。这在框架开发中用于传递上下文信息，如 `ConfigurationClassPostProcessor` 用它记录 `@Configuration` 类的来源

- **`BeanDefinition#setSource`**：区分不同 Bean 定义来源的手段。`[发散]` Spring 内部用 `source` 来判断 Bean 定义的来源（XML/注解/Groovy），在冲突检测和调试中有用

- **`BeanDefinitionBuilder` vs `BeanDefinitionReader`**：前者是编程式构建 BeanDefinition 的流畅 API，后者是从资源（XML/Properties）读取的解析器。`[发散]` Spring 6.x 引入了 `BeanDefinitionRegistrar` 编程式注册接口，比传统的 `BeanDefinitionRegistryPostProcessor` 更简洁

### 1.3 依赖注入机制 `[源:第24,49,55期]`

- **注入实现类对应关系**：
  - `@Autowired` / `@Inject` -> `AutowiredAnnotationBeanPostProcessor`
  - `@Resource` / `@EJB` / `@WebServicesRef` -> `CommonAnnotationBeanPostProcessor`
  - `@Bean` 方法参数 -> `ConfigurationClassParser`

- **注入方式与循环依赖**：
  - 构造器注入：不支持循环依赖（Bean 还未实例化无法提前暴露）
  - Setter/字段/方法注入：支持循环依赖（通过三级缓存提前暴露半成品）
  - 高版本 Spring Boot 默认关闭循环依赖处理

- **依赖筛选**：`@Autowired` 通过类型查找 -> `@Qualifier` 通过名称查找 -> 底层 `DefaultListableBeanFactory#doResolveDependency` -> `AutowireCandidateResolver#getSuggestedValue`

- **延迟注入**：`Optional<T>`、`ObjectFactory<T>`、`ObjectProvider<T>` 是延迟依赖注入；`@Inject`/`@Autowired` 直接注入是非延迟

`[发散]` **小马哥未深入的点：**

- **`@Autowired` vs `@Resource` 的冲突**：当字段同时标注 `@Autowired` 和 `@Resource` 时，`CommonAnnotationBeanPostProcessor`（处理 `@Resource`）的 Order 值低于 `AutowiredAnnotationBeanPostProcessor`，导致 `@Resource` 先处理 `[源:第78期]`。这不是 bug 而是设计——JSR-250 注解优先级高于 Spring 自定义注解

- **`@Autowired` 的 Collection/Map 注入**：`Map<String, T>` 注入所有 T 类型 Bean（key=bean name）；`List<T>` 注入所有 T 类型 Bean（按 `@Order`/`Ordered` 排序）。`[发散]` 这个特性是 Spring 实现 SPI 机制的基础——`microsphere-java` 的 `Prioritized` 排序就依赖于此

- **`@Autowired(required=false)` vs `Optional<T>`**：前者在找不到 Bean 时不报错但字段为 null；后者注入 `Optional.empty()`。`[发散]` `ObjectProvider<T>` 更灵活，支持 `getIfAvailable`/`getIfUnique`/`iterator`，是 Spring 4.3+ 推荐的延迟注入方式

- **构造器注入的优势**：不可变性（final 字段）、循环依赖显式失败（而非隐式三级缓存）、无需反射设置字段。`[发散]` Spring 4.3+ 如果类只有一个构造器，`@Autowired` 可省略——这是推荐构造器注入的信号

---

## 二、Spring AOP 与拦截器体系

### 2.1 BeanPostProcessor 体系 `[源:第34期]`

- **基本语义**：处理 Bean 初始化生命周期（before/after），Spring 5.0+ 提供默认无操作实现
- **与 BeanFactory 关系**：N:1，一个 BeanFactory 可关联 N 个 BeanPostProcessor
- **来源**：显式插入（`addBeanPostProcessor`）或定义为普通 Spring Bean（`registerBeanPostProcessors`）
- **N×M 性能问题**：N 个 Bean × M 个 BeanPostProcessor = N×M 次回调

`[发散]` **小马哥未覆盖的点：**

- **BeanPostProcessor 子接口层次**：
  - `InstantiationAwareBeanPostProcessor`：实例化前后 + 属性处理（`postProcessBeforeInstantiation`/`postProcessAfterInstantiation`/`postProcessProperties`）
  - `SmartInstantiationAwareBeanPostProcessor`：构造器确定（`determineCandidateConstructors`）+ 提前暴露引用（`getEarlyBeanReference`，用于循环依赖+AOP）
  - `MergedBeanDefinitionPostProcessor`：合并后定义处理（缓存注入元信息）
  - `DestructionAwareBeanPostProcessor`：销毁前回调（`postProcessBeforeDestruction`）

- **BeanPostProcessor 与 AOP 的关系**：`AbstractAutoProxyCreator`（`AspectJAwareAdvisorAutoProxyCreator`/`AnnotationAwareAspectJAutoProxyCreator`）实现 `SmartInstantiationAwareBeanPostProcessor`，在 `postProcessAfterInitialization` 中创建代理。`[发散]` 如果 Bean 实现了 `Advised` 接口，可以跳过代理创建——这是 Spring AOP 的一个优化

- **BeanFactoryPostProcessor vs BeanDefinitionRegistryPostProcessor**：后者优先于前者执行，且支持动态注册 BeanDefinition。`[发散]` `ConfigurationClassPostProcessor` 是最重要的 `BeanDefinitionRegistryPostProcessor`，它处理 `@Configuration`/`@ComponentScan`/`@Import`/`@Bean`，是注解驱动 Spring 的核心

### 2.2 HandlerInterceptor 体系 `[源:第35期]`

- **HandlerExecutionChain 执行流程**：`applyPreHandle` -> Handler -> `applyPostHandle` -> `triggerAfterCompletion`
- **HandlerInterceptor vs Filter**：Filter 拦截 Servlet（`DispatcherType`: Request/Forward/Include/Error/Async）；HandlerInterceptor 限于 DispatcherServlet
- **HandlerInterceptor vs WebRequestInterceptor**：后者是通用拦截器，支持 Servlet/Portlet/JSF
- **MappedInterceptor**：带 URL 模式的拦截器，内部适配到 HandlerInterceptor

`[发散]` **小马哥未覆盖的点：**

- **Filter 的 `DispatcherType`**：`ASYNC` 类型是 Servlet 3.0 异步请求的关键——`[发散]` 如果 Filter 只注册了 `REQUEST` 类型，异步请求中的 Forward 不会被拦截，这是异步处理中常见的安全漏洞

- **HandlerInterceptor 的 `excludePathPatterns`**：`MappedInterceptor` 支持 `includePathPatterns` 和 `excludePathPatterns`，但底层实现是 `AntPathMatcher`。`[发散]` `AntPathMatcher` 的性能在大规模路径匹配时是瓶颈，Spring 6.x 引入了 `PathPatternParser` 作为替代

- **拦截器与异步请求**：`AsyncHandlerInterceptor#afterConcurrentHandlingStarted` 在异步请求开始时调用，但 `postHandle` 和 `afterCompletion` 不会被调用（异步请求在独立线程中完成）。`[发散]` 这是 WebFlux 和 MVC 异步处理的核心差异——WebFlux 没有拦截器概念，只有 `WebFilter`

### 2.3 内容协商机制 `[源:第36期]`

- **客户端**：`Accept` 头（`text/html`、`application/xml`）
- **服务端**：`content-type` 响应头
- **ViewResolver 共存策略**：按 Media Type / URL 后缀 / 请求参数

`[发散]` **小马哥未覆盖的点：**

- **内容协商的底层**：`ContentNegotiationManager` -> `ContentNegotiationStrategy` 链。`[发散]` Spring 5.x 之后默认禁用了 URL 后缀协商（`favorPathExtension=false`），因为存在安全风险（RFD - Reflected File Download）

- **`@ResponseBody` 与内容协商**：`@ResponseBody` 不走 ViewResolver，而是走 `HttpMessageConverter`。`[发散]` `RequestResponseBodyMethodProcessor` 根据 `Accept` 头选择合适的 Converter（Jackson/Protobuf/Xml），这是 REST API 的核心机制

- **内容协商与版本控制**：`[发散]` 可以通过自定义 `ContentNegotiationStrategy` 实现 API 版本控制，如 `Accept: application/vnd.api.v2+json`。也可以用 `Content-Type` 区分创建/更新操作

---

## 三、Spring Boot 自动装配与外部化配置

### 3.1 外部化配置抽象 `[源:第2,28期]`

- **Spring Cloud Context 抽象**：`EnvironmentManager`、`@RefreshScope`（Bean 动态刷新）、`EnvironmentChangeEvent`/`RefreshEvent`
- **Dubbo 外部化配置"绑定"设计**：将 Spring Boot relaxed binding 扩展到 Dubbo 配置项
- **Dubbo 外部化配置"激活"设计**：根据外部配置动态激活 Dubbo 组件

`[发散]` **小马哥未覆盖的点：**

- **`@ConfigurationProperties` binding 机制**：`[发散]` Spring Boot 2.x 使用 `Binder` API 替代了 1.x 的 `RelaxedDataBinder`，支持嵌套属性、集合、Map、Duration、DataSize 等复杂类型绑定。这是 microsphere-configuration 项目的核心基础

- **`Environment` 的 `PropertySource` 优先级**：命令行参数 > 系统属性 > 环境变量 > `application-{profile}.yml` > `application.yml` > 默认值。`[发散]` Spring Cloud 的 `bootstrap.yml` 优先级高于 `application.yml`，因为 Bootstrap Context 是 Parent Context

- **`@RefreshScope` 的实现原理**：`[发散]` `@RefreshScope` 的 Bean 被包装为 `ScopedProxyFactoryBean`，实际存储在 `ThreadScope`/`RefreshScope` 的缓存中。`RefreshEvent` 触发时，缓存被清空，下次访问时重新创建 Bean——这是"热更新"的本质

### 3.2 Spring Boot Auto-Configuration `[源:第29,58期]`

- **Dubbo Spring Boot 项目结构**：`autoconfigure`/`starter`/`actuator`/`compatible`
- **`DubboAutoConfiguration` 复用核心处理器**：`ServiceAnnotationBeanPostProcessor`（`@Service`）、`ReferenceAnnotationBeanPostProcessor`（`@Reference`）
- **版本兼容**：Dubbo >= 2.7 使用 `org.apache.dubbo`；< 2.7 使用 `com.alibaba`
- **Druid + Micrometer 整合**：实现 `MeterBinder` 接口，用 Gauge 绑定 Druid 指标

`[发散]` **小马哥未覆盖的点：**

- **`@AutoConfigureBefore`/`@AutoConfigureAfter`/`@AutoConfigureOrder`**：控制自动装配类的加载顺序。`[发散]` Spring Boot 2.7+ 用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 替代了 `spring.factories`，但顺序控制注解仍然有效

- **`@ConditionalOnClass` 的字节码分析**：`[发散]` `@ConditionalOnClass` 不通过 `Class.forName` 判断类是否存在（会触发类加载），而是通过 `ASM` 读取字节码——避免类加载失败导致的级联错误

- **`@ConditionalOnMissingBean` 的陷阱**：`[发散]` `@ConditionalOnMissingBean` 只检测当前 BeanFactory 中已注册的 Bean，不检测后续注册的 Bean。如果自动装配类 A 依赖 B 的 Bean，但 B 在 A 之后注册，A 的条件会误判。解决方案是使用 `@AutoConfigureAfter` 确保顺序

---

## 四、Java 并发与线程池

### 4.1 ThreadPoolExecutor 深度解析 `[源:第72,77,81期]`

- **7 个核心参数**：corePoolSize、maximumPoolSize、keepAliveTime、unit、workQueue、threadFactory、handler
- **动态参数设置**（均为线程安全）：`setCorePoolSize`/`setMaximumPoolSize`/`setKeepAliveTime`/`setThreadFactory`/`setRejectedExecutionHandler`/`allowCoreThreadTimeOut`
- **线程预热**：`prestartAllCoreThreads`/`prestartCoreThread`——Max 线程依赖 BlockingQueue 状态，预热只能针对核心线程
- **状态获取**：`getPoolSize`/`getActiveCount`/`getCompletedTaskCount`/`getTaskCount`
- **submit vs execute**：submit 包装为 FutureTask，catch Throwable 转为 Exception，看不到堆栈；execute 主动 catch Throwable 重新抛出

`[发散]` **小马哥未覆盖的点：**

- **线程池的状态机**：RUNNING(-1) -> SHUTDOWN(0) -> STOP(1) -> TIDYING(2) -> TERMINATED(3)。`[发散]` 状态用 AtomicInteger 的高 3 位表示，低 29 位表示工作线程数（`ctl` 字段）。`shutdown()` 不接受新任务但处理队列中的任务；`shutdownNow()` 不接受新任务且丢弃队列中的任务

- **`ThreadPoolExecutor` 的 workQueue 与拒绝策略的关系**：`[发散]` 任务提交流程：core 线程未满 -> 创建核心线程；core 线程已满 -> 入队列；队列已满 -> 创建非核心线程（到 max）；max 已满 -> 执行拒绝策略。`[发散]` 这意味着如果用 `LinkedBlockingQueue`（无界），max 永远不会生效——这是 `Executors.newFixedThreadPool` 的"坑"所在

- **`allowCoreThreadTimeOut` 的语义**：`[发散]` 默认核心线程不会超时回收。设置为 true 后，核心线程在 keepAliveTime 后也会回收。这在间歇性负载场景下节省资源，但会导致下次任务到来时重新创建线程的开销

- **`beforeExecute`/`afterExecute` 的钩子方法**：`[发散]` 这两个方法在 Worker 线程中同步调用。如果 `beforeExecute` 抛出异常，`afterExecute` 不会被调用，但 Worker 线程不会终止。这是监控单任务执行时间的基础——但要注意异常处理

- **Tomcat 线程池与 JUC 线程池的差异**：`[发散]` Tomcat 的 `org.apache.tomcat.util.threads.ThreadPoolExecutor` 提交任务时优先创建线程到 max，而不是先填满队列。这是因为 Web 服务器更关注响应延迟而非吞吐量——这是与 JUC 线程池的核心设计差异

### 4.2 ThreadLocal 与线程池 `[源:第81,82期]`

- **`InheritableThreadLocal` 的局限**：在 `Thread.init()` 时复制父线程 ThreadLocalMap，但线程池中线程创建者不确定（可能是 `prestartCoreThread` 调用者）
- **ThreadLocal 内存泄漏**：Entry extends `WeakReference<ThreadLocal<?>>`，Key 是弱引用，Value 是强引用；ThreadLocal 被 GC 后 Key 为 null，Value 成为"孤岛"
- **OOM 风险**：静态 ThreadLocal + 线程池环境 + 不调用 `remove` -> Map 膨胀

`[发散]` **小马哥未覆盖的点：**

- **`TransmittableThreadLocal`（阿里 TTL）**：`[发散]` 解决了 `InheritableThreadLocal` 在线程池场景的传递问题。核心思路：在 `Runnable` 提交时（`submit`/`execute`）捕获当前线程的 ThreadLocal 快照，在 Worker 线程执行前恢复，执行后清理。这与小马哥第 81 期提出的自定义 `ThreadPoolExecutor` 方案思路一致，但 TTL 是成熟的开源实现

- **ThreadLocal 的 hash 冲突处理**：`[发散]` ThreadLocalMap 使用开放寻址法（线性探测），而非 HashMap 的链表法。`threadLocalHashCode` 通过 AtomicInteger 的 `getAndAdd(0x61c88647)`（黄金分割数）来分散 hash 值，减少冲突

- **`expungeStaleEntry` 的清理机制**：`[发散]` ThreadLocalMap 在 `get`/`set`/`remove` 操作时会触发清理：探测到 Key 为 null 的 Entry 时，清理该 Entry 及其后连续的 stale Entry，并 rehash 后续 Entry。这是被动清理，不是主动 GC——因此如果 Map 长时间不被访问，泄漏的 Value 不会被回收

- **`ThreadLocal` 与 `synchronized` 的本质区别**：`[发散]` `synchronized` 是线程间共享同步（多线程竞争同一资源）；`ThreadLocal` 是线程间隔离（每个线程独立副本）。前者解决"竞争"问题，后者解决"共享"问题。Spring 事务用 ThreadLocal 绑定 Connection，确保同一事务中的所有操作使用同一 Connection

### 4.3 并发工具类的陷阱 `[源:第81期]`

- **CountDownLatch 滥用**：Boss/Worker 共用线程池 + `CountDownLatch#await()` -> 队列中任务无法执行 -> 死锁
- **解决方案**：Boss/Worker 分离线程池、使用 `CompletableFuture`/`CompletionService` 替代 `CountDownLatch`

`[发散]` **小马哥未覆盖的点：**

- **`CompletableFuture` 的默认线程池陷阱**：`[发散]` `CompletableFuture.supplyAsync` 不指定线程池时使用 `ForkJoinPool.commonPool()`，其并行度为 `CPU 核心数 - 1`。在 I/O 密集型场景下，commonPool 线程数不足会导致所有 CompletableFuture 串行执行

- **`CountDownLatch` vs `CyclicBarrier`**：`[发散]` CountDownLatch 一次性、不可重置；CyclicBarrier 可重置、可复用。CountDownLatch 是"一个等 N 个"或"N 个等一个"；CyclicBarrier 是"N 个互相等待"

- **`Phaser` 的灵活度**：`[发散]` Java 7 引入的 `Phaser` 支持动态注册/注销 party、分阶段等待、树形层次结构，是 CountDownLatch + CyclicBarrier 的超集。但 API 复杂，生产中很少使用

### 4.4 Integer 与字节码分析 `[源:第82期]`

- **Integer++ 底层**：`Integer#intValue()` -> int `++` -> `Integer.valueOf(int)`
- **`synchronized(count)` 锁失效**：`count++` 会创建新 Integer 对象，锁的是旧对象
- **Integer Cache**：`-128` 到 `127` 的 Integer 对象被缓存

`[发散]` **小马哥未覆盖的点：**

- **`Integer.valueOf` 的缓存机制**：`[发散]` 通过 `-XX:AutoBoxCacheMax` 可调整上限，但下限固定为 `-128`。这个缓存在 JVM 启动时初始化，是 Class 级别的共享状态。`[发散]` 这也是为什么 `==` 对 Integer 在缓存范围内为 true，范围外为 false——这是最常见的 Java 八股陷阱

- **自动装箱/拆箱的性能影响**：`[发散]` 在循环中频繁装箱/拆箱会产生大量临时对象，增加 GC 压力。`LongAdder`/`AtomicLong` 是更好的替代方案——前者在并发写场景下通过 Cell 分片减少竞争

- **`synchronized` 锁的 4 种状态**：`[发散]` 无锁 -> 偏向锁 -> 轻量级锁 -> 重量级锁。JDK 15+ 废弃了偏向锁（`-XX:-UseBiasedLocking`），因为现代 CAS 操作的开销已经很低，偏向锁的维护成本反而更高

---

## 五、JVM 与 GC

### 5.1 G1 GC 故障排查 `[源:第63期]`

- **G1 GC 日志关键字段**：`GC pause (young)`（YGC）、`Parallel Time`（STW 并行时间）、`Ext Root Scanning`、`Update RS`、`Object Copy`、`Humongous Register/Reclaim`
- **JVM 参数**：`-XX:+UseG1GC`、`-XX:MaxHeapSize=8G`、`-XX:InitiatingHeapOccupancyPercent=35`
- **YGC 分析**：Eden 408M -> 0, Survivors 0 -> 32M, Heap 408M -> 30.7M

`[发散]` **小马哥未覆盖的点：**

- **G1 的 Region 设计**：`[发散]` G1 将堆分为 2048 个等大 Region（1MB-32MB），每个 Region 可以动态切换为 Eden/Survivor/Old/Humongous。`[发散]` Humongous Region 用于存储大对象（> Region 的 50%），大对象直接分配在 Old Region，不经过 Young 区

- **`InitiatingHeapOccupancyPercent`（IHOP）**：`[发散]` 默认 45%（Java 8）/自适应（Java 9+）。当 Old 区占用达到 IHOP 时触发并发标记周期。`[发散]` 如果 Mixed GC 跟不上分配速度，会导致 Full GC（STW，整个堆）——这是 G1 最严重的问题

- **G1 vs ZGC vs Shenandoah**：`[发散]` 
  - G1：Region 分区，STW < 200ms（通常），并发标记但复制阶段 STW
  - ZGC（Java 15+ 生产可用）：染色指针，STW < 1ms，并发复制
  - Shenandoah（OpenJDK）：Brooks 转发指针，STW < 10ms，并发整理
  - `[发散]` ZGC 的染色指针利用了 64 位指针的高 4 位存储 GC 状态，是硬件级别的创新

- **G1 FullGC 的常见原因**：`[发散]` 
  1. TO-space exhaustion（Survivor 区不足以容纳存活对象）
  2. Humongous 分配失败（大对象无法找到连续 Region）
  3. Mixed GC 跟不上分配速度
  4. Metaspace 不足（触发 Full GC 而非 Concurrent Mark）

### 5.2 线上故障排查方法论 `[源:第62,75,86期]`

- **bilibili 713 故障**：`lua-resty-balancer` 的 `_gcd` 函数收到 weight="0"（字符串），触发 JIT 编译器 bug 死循环
- **支付宝国补故障**：营销活动配置错误
- **CPU/负载/内存排查方法论**：先看监控面板（而非 top），找正常->异常的趋势转折点

`[发散]` **小马哥未覆盖的点：**

- **JIT 编译器 Bug**：`[发散]` JIT 编译器（C1/C2/Graal）在极端情况下会生成错误的机器码。bilibili 故障是 C2 JIT 的类型推断 bug——`_gcd` 函数收到字符串 "0" 时，JIT 假设它总是数字类型，生成了死循环的机器码。`[发散]` 解决方案：`-XX:CompileCommand=exclude,...::_gcd` 排除特定方法的 JIT 编译

- **线上故障排查工具链**：`[发散]`
  - CPU 高：`top -Hp <pid>` -> `jstack <pid>` -> 找到 CPU 高的线程 nid
  - 内存高：`jmap -histo:live <pid>` -> `jmap -dump:format=b,file=heap.hprof <pid>` -> MAT 分析
  - GC 频繁：`jstat -gcutil <pid> 1000` -> 分析 GC 日志（`-Xlog:gc*`）
  - `[发散]` 生产环境应优先使用 Arthas/async-profiler 等低开销工具，`jstack` 会触发 Safepoint 导致 STW

- **故障应急预案**：`[发散]` 
  1. 止损优先于定位（回滚/降级/限流）
  2. 保留现场（thread dump/heap dump/GC log）后重启
  3. 多活架构是容灾止损最快方案
  4. 办公网与用户链路必须隔离（bilibili 故障教训）

---

## 六、数据库事务与一致性

### 6.1 Spring 事务机制 `[源:第53,80期]`

- **MySQL 事务**：Auto-Commit=1 时 DML 自动事务；默认隔离级别 RR，MVCC 机制
- **Spring 事务与 MyBatis 集成**：`SqlSessionFactory` 与 `DataSourceTransactionManager` 必须使用相同 DataSource
- **`SpringManagedTransaction`**：通过 `DataSourceUtils.getConnection()` 获取 Spring 管理的 Connection
- **多线程事务失效**：`@Transactional` + `@Async` 组合时，`SimpleAsyncTaskExecutor` 每次创建独立线程，无法绑定事务资源

`[发散]` **小马哥未覆盖的点：**

- **Spring 事务的 ThreadLocal 绑定**：`[发散]` `TransactionSynchronizationManager` 用 ThreadLocal 绑定 `ConnectionHolder`，确保同一事务中的所有操作使用同一 Connection。这就是为什么多线程场景下事务会失效——不同线程有不同的 ThreadLocal

- **事务传播行为**：`[发散]` 
  - `REQUIRED`（默认）：当前有事务则加入，无则新建
  - `REQUIRES_NEW`：总是新建事务，挂起当前事务
  - `NESTED`：创建嵌套事务（通过 Savepoint 实现）
  - `SUPPORTS`/`NOT_SUPPORTED`/`MANDATORY`/`NEVER`
  - `[发散]` `REQUIRES_NEW` 的"挂起"是通过 `SuspendedResourcesHolder` 保存当前事务资源，新事务完成后恢复——这意味着外层事务的 ThreadLocal 被临时替换

- **`@Transactional` 的 self-invocation 失效**：`[发散]` 同一类中方法 A 调用方法 B（`@Transactional`），B 的事务不生效——因为 AOP 代理只拦截外部调用，内部调用走的是 `this` 而非代理对象。解决方案：注入自身代理（`@Lazy` self-injection）/使用 `AopContext.currentProxy()`/将方法拆到不同类

- **Spring 事务与 JPA/Hibernate 的 `Flush` 时机**：`[发散]` Hibernate 的 Session 在事务提交前会自动 flush，但 flush 不等于 commit。如果 flush 时发生 ConstraintViolation，事务不会回滚（因为异常发生在 flush 而非 commit 阶段）——这是 JPA 的一个微妙陷阱

### 6.2 连接池原理 `[源:第53期]`

- **DBCP 连接池**：使用 Commons Pools 的 `wait()`/`notify()` 实现阻塞

`[发散]` **小马哥未覆盖的点：**

- **Druid vs HikariCP 的设计差异**：`[发散]`
  - HikariCP：`ConcurrentBag`（无锁并发队列），FastList（避免范围检查），Connection 在借出时状态校验。极致性能优先
  - Druid：StatFilter（SQL 统计）、WallFilter（SQL 防注入）、ProxyConnection（JDBC 代理）。功能丰富优先
  - `[发散]` HikariCP 的 `ConcurrentBag` 使用 `ThreadLocal` + `CopyOnWriteArrayList` 实现无锁借出——每个线程优先从自己的 ThreadLocal 列表中获取连接，减少竞争

- **连接泄漏检测**：`[发散]` Druid 通过 `removeAbandoned` 配置检测泄漏连接（借出超过 `removeAbandonedTimeout` 未归还）。HikariCP 通过 `leakDetectionThreshold` 配置，在连接借出超过阈值时打印堆栈日志

### 6.3 分布式锁 Code Review `[源:第80期]`

- **Redis 分布式锁的 reentrant（可重入）问题**：`tryLock` 实现中缺少重进入的逻辑判断

`[发散]` **小马哥未覆盖的点：**

- **Redis 分布式锁的正确实现**：`[发散]`
  - SET NX PX（原子加锁 + 过期时间）
  - Value 必须是唯一标识（UUID），释放锁时用 Lua 脚本校验+删除（原子操作）
  - 续期问题：Redisson 通过 Watchdog 自动续期（默认 10s 续期一次）
  - `[发散]` Redlock 算法（多节点加锁，多数成功才算成功）在时钟漂移场景下仍有争议（Martin Kleppmann vs antirez 之争）

- **分布式锁的本质**：`[发散]` 分布式锁不是"锁"，而是"共识"——让多个节点对"谁持有锁"达成一致。Redis 的 AP 特性决定了它在网络分区时可能出问题（两个节点同时持有锁），而 Zookeeper/etcd 的 CP 特性更可靠但性能更低

---

## 七、微服务架构与治理

### 7.1 服务注册与发现 `[源:第1,16,18,22,59期]`

- **注册中心对比**：Eureka（AP）、Zookeeper（CP）、Consul（AP）、Nacos（AP/CP 可选）
- **Spring Cloud Commons 抽象**：`Registration`/`ServiceRegistry`/`DiscoveryClient`
- **Dubbo Registry SPI**：`RegistryFactory`/`Registry`，通过桥接适配 Spring Cloud Commons API
- **跨区域注册中心设计**：同区域优先 + 故障转移；双注册双订阅

`[发散]` **小马哥未覆盖的点：**

- **CAP 理论在注册中心的实践**：`[发散]` 
  - AP（Eureka/Nacos）：网络分区时各节点继续提供服务，可能返回过期数据。适合"可用性优先"的场景
  - CP（Zookeeper/etcd/Consul）：网络分区时少数派节点不可用，保证数据强一致。适合"一致性优先"的场景
  - `[发散]` Nacos 1.0+ 同时支持 AP（默认）和 CP 模式，通过 `@NacosValue` 配置切换。AP 模式使用 Distro 协议，CP 模式使用 Raft 协议

- **服务实例健康检查**：`[发散]`
  - Eureka：客户端心跳（默认 30s），服务端 90s 未收到心跳则剔除
  - Nacos：客户端心跳（临时实例）/服务端主动探测（永久实例）
  - `[发散]` 临时实例（默认）在客户端断连后自动注销；永久实例需要手动注销，适合数据库/消息队列等基础设施

- **服务发现的 Push vs Pull**：`[发散]`
  - Eureka：Pull（客户端定时拉取，默认 30s）
  - Nacos：Push+Pull（UDP 推送 + 定时拉取兜底）
  - `[发散]` 纯 Pull 模式的延迟 = 拉取间隔，不适合大规模实例场景。Nacos 的 UDP 推送解决了这个问题，但 UDP 不可靠——所以需要 Pull 兜底

### 7.2 服务调用模式 `[源:第16,18,19,31期]`

- **`@LoadBalanced` RestTemplate**：通过 `ClientHttpRequestInterceptor` -> `LoadBalancerInterceptor`
- **OpenFeign**：`@FeignClient` 动态代理 -> `FeignClientFactoryBean#getObject()` -> HTTP 调用
- **Feign Contract**：SpringMvcContract（识别 `@RequestMapping` 等）、JAX-RS Contract
- **Dubbo+Spring Cloud**：`@DubboTransported` 替换 Feign HTTP 为 Dubbo 协议

`[发散]` **小马哥未覆盖的点：**

- **Feign 的 ApplicationContext 隔离**：`[发散]` 每个 `@FeignClient` 创建独立的子 ApplicationContext，拥有自己的 Encoder/Decoder/Contract/Logger/Retryer/Options。这是 Feign 的"组件隔离"设计——不同 FeignClient 可以有不同的配置，互不影响

- **Spring Cloud LoadBalancer vs Netflix Ribbon**：`[发散]` Ribbon 已停止维护，Spring Cloud LoadBalancer 是替代方案。核心差异：Ribbon 通过 `IClientConfig` 配置，SCL 通过 `LoadBalancerProperties` 配置；Ribbon 支持 ServerListFilter 链，SCL 通过 `ServiceInstanceListSupplier` 链

- **Feign 的继承与接口共享**：`[发散]` Feign 支持接口继承（Provider 和 Consumer 共享 API 接口），但这引入了耦合——API 接口变更影响所有消费者。`[发散]` 最佳实践是 Provider 定义 API 模块（含 DTO + 接口），Consumer 依赖 API 模块而非 Provider 实现

### 7.3 分布式链路跟踪 `[源:第79期]`

- **Trace ID 与 Span**：一个 Trace ID 对应 N 个 Span Id
- **AOP 实现方式**：Agent-Based（字节码提升，黑盒）/ API-Based（动态代理）
- **生态拦截点**：HTTP Server/Client、JDBC、RPC、MQ、NoSQL
- **Trace ID 透传**：Servlet Filter/ServletRequestListener、Dubbo Filter、Feign Capacity

`[发散]` **小马哥未覆盖的点：**

- **OpenTelemetry 统一标准**：`[发散]` OpenTelemetry 是 OpenTracing + OpenCensus 的合并，已成为可观测性的事实标准。它统一了 Tracing/Logging/Metrics 的 API 和数据模型。Spring Boot 3.x 通过 `micrometer-tracing` 集成 OpenTelemetry

- **采样策略**：`[发散]` 
  - 概率采样（如 1%）：简单但可能丢失关键请求
  - 限流采样（如每秒 100 条）：保证不超量但可能丢失突发流量
  - 尾部采样：请求完成后根据延迟/错误率决定是否采样——最精确但需要缓冲所有请求
  - `[发散]` 生产环境通常组合使用：头部采样率 + 尾部采样（错误请求 100% 采样）

- **Baggage 与上下文传播**：`[发散]` OpenTelemetry 的 Baggage 允许在 Trace 中携带键值对（如 userId/tenantId），跨服务传播。`[发散]` Baggage 的传播会增加请求头大小，需要限制 Baggage 数量和大小

- **Span 的 Kind**：`[发散]` OpenTelemetry 定义了 5 种 Span Kind：INTERNAL（内部方法）、SERVER（服务端）、CLIENT（客户端）、PRODUCER（MQ 生产者）、CONSUMER（MQ 消费者）。INTERNAL Span 不跨进程，其他 4 种涉及跨进程传播

---

## 八、Java 21+ 与虚拟线程

### 8.1 虚拟线程 `[源:第60期]`

- **Java 21 正式特性**：虚拟线程
- **JDK 升级影响评估清单**：移除 API、字节码兼容性、JVM GC 变化、新 API 对老框架影响

`[发散]` **小马哥未覆盖的点：**

- **虚拟线程的实现原理**：`[发散]` 虚拟线程是用户态线程，由 JVM 调度而非 OS。底层通过 `ForkJoinPool`（carrier thread）运行，当虚拟线程执行 I/O 操作时，JVM 自动 unmount 虚拟线程并释放 carrier thread。`[发散]` 这意味着虚拟线程的数量不再受 OS 线程限制，可以创建数百万个虚拟线程

- **虚拟线程与 `synchronized` 的冲突**：`[发散]` 虚拟线程在 `synchronized` 块中执行 I/O 操作时会 pin carrier thread（无法 unmount），导致 carrier thread 被阻塞。Java 21 的解决方案是用 `ReentrantLock` 替代 `synchronized`

- **虚拟线程与 ThreadLocal 的代价**：`[发散]` 虚拟线程数量可达百万级，如果每个虚拟线程都有 ThreadLocal，内存消耗巨大。Java 21 引入了 `ScopedValue` 作为 ThreadLocal 的替代——它不可变、有界、自动清理

- **虚拟线程不适用场景**：`[发散]` 
  1. CPU 密集型任务（虚拟线程不提升 CPU 利用率）
  2. 需要 `synchronized` 的代码（pin 问题）
  3. 已有异步框架（WebFlux/Vert.x）的场景（收益不明显）
  4. `[发散]` 虚拟线程的最大价值是让"同步编程模型"获得"异步性能"——对存量同步代码零改造即可提升吞吐量

### 8.2 Java 版本升级评估 `[源:第60期]`

- **移除 API**：Unsafe、XML API
- **字节码兼容性**
- **JVM GC 变化**

`[发散]` **小马哥未覆盖的点：**

- **模块化（JPMS）的影响**：`[发散]` Java 9 模块化导致很多反射 API 受限（`--add-opens`/`--add-exports`）。Spring/Hibernate 等框架大量使用反射，升级时需要添加 JVM 参数。`[发散]` microsphere-java 中的 `Unsafe` 封装就是受此影响

- **Record/Pattern Matching/Sealed Classes**：`[发散]` 这些特性改变了 Java 的建模方式。Record 适用于 DTO/Value Object；Sealed Classes 适用于领域模型（限定子类型）；Pattern Matching 减少 instanceof 强转

- **GraalVM Native Image**：`[发散]` Spring Boot 3.x + GraalVM 可以编译为原生可执行文件，启动时间从秒级降到毫秒级。但 Native Image 的限制：不支持反射（需配置/注解）、不支持动态代理（需构建时生成）、不支持 CGLIB（Spring AOP 需改为 JDK 代理）

---

## 九、安全漏洞与防御性编程

### 9.1 Log4j2 / JNDI 注入 `[源:第46期]`

- **CVE-2021-44228（Log4Shell）**：JNDI lookup 允许任意代码执行
- **漏洞链**：Log4j2 -> JNDI Lookup -> LDAP/RMI -> 远程类加载 -> RCE
- **9 种修复方案**：网络隔离、网关过滤、移除 `JndiLookup`、禁用 Lookups、升级 JDK、升级 Log4j、Security Manager 等
- **JNDI 架构**：SPI 抽象层（LDAP/RMI/DNS/CORBA）

`[发散]` **小马哥未覆盖的点：**

- **JNDI 注入的根因**：`[发散]` JNDI 的 `Context.lookup()` 可以加载远程类（通过 `ObjectFactory`）。`[发散]` JDK 8u191+ 添加了 `com.sun.jndi.ldap.object.trustURLCodebase=false`，阻止了 LDAP/RMI 远程类加载，但仍然可以通过本地 Classpath 中的 `ObjectFactory` 实现攻击（Java Deserialization Gadget Chain）

- **Log4j2 漏洞的深层原因**：`[发散]` 
  1. Lookup 机制设计过于强大（允许在日志消息中执行 JNDI 查询）
  2. 默认开启 Lookup（`%msg{nolookups}` 需要显式关闭）
  3. 日志框架不应该有"执行"能力——这是职责越界
  - `[发散]` 这个漏洞的教训：框架的"便利性"和"安全性"是矛盾的。任何允许在配置/消息中执行代码的机制都是潜在漏洞

- **类似的注入漏洞**：`[发散]`
  - Spring SpEL 注入（`#{...}`）
  - Log4j Lookup 注入（`${...}`）
  - OGNL 注入（Struts2 漏洞）
  - `[发散]` 共同特征：用户输入被传递到表达式引擎执行。防御原则：用户输入永远不应该被当作表达式执行

### 9.2 防御性编程 `[源:第65期]`

- **定义**：保证程序对不可预见的使用不会造成功能损坏
- **核心原则**：提高工程质量、源码可读性、处理不可预期用户操作
- **警惕过度防御**：过多异常捕捉会导致结果不正确或被隐藏

`[发散]` **小马哥未覆盖的点：**

- **防御性编程的层次**：`[发散]`
  1. **输入验证**：前置防御（Precondition），拒绝非法输入
  2. **不变量维护**：过程中防御（Invariant），确保对象状态始终合法
  3. **异常处理**：后置防御（Postcondition），确保异常不破坏状态
  4. **Fail-Fast**：尽早暴露问题，而非吞掉异常
  - `[发散]` `Objects.requireNonNull` / `Preconditions.checkArgument`（Guava）/ `Assert`（Spring）是输入验证的标准工具

- **过度防御的反模式**：`[发散]`
  - `catch (Exception e) { }` 静默吞异常
  - `return null` 代替抛异常
  - 多层 try-catch 嵌套
  - `[发散]` 这些做法会导致问题被隐藏，最终在不可预测的地方爆发（"延迟爆炸"）

- **防御性编程 vs 健壮性编程**：`[发散]` 
  - 防御性编程：假设调用者会犯错，保护自己不被伤害
  - 健壮性编程：假设环境会出问题，保证系统持续运行
  - `[发散]` 前者是"不信任输入"，后者是"不信任环境"。两者结合才是生产级代码

---

## 十、Code Review 与工程实践

### 10.1 AOP 切面优化 `[源:第74期]`

- 避免 Bean 过早初始化（用 `SmartInitializingSingleton` 替代 `@PostConstruct`）
- 缩小切面搜索范围（仅 `@Controller`）
- 通过 BeanDefinition 获取 Class 而非 Bean 对象

`[发散]` **小马哥未覆盖的点：**

- **AOP 切面的性能影响**：`[发散]` 每个被切面匹配的 Bean 都会被创建代理（CGLIB/JDK Proxy）。如果切面表达式过宽（如 `execution(* com..*.*(..))`），几乎所有 Bean 都会被代理，增加启动时间和内存消耗。`[发散]` 最佳实践：尽可能缩小 pointcut 范围，使用注解标记（`@within(RestController)`）而非包路径

- **CGLIB vs JDK Proxy 的选择**：`[发散]`
  - JDK Proxy：基于接口，只能代理接口方法
  - CGLIB：基于继承，可以代理 public/protected 方法，但不能代理 final 类/方法
  - `[发散]` Spring Boot 2.0+ 默认使用 CGLIB（`spring.aop.proxy-target-class=true`），因为大多数类不实现接口。但 CGLIB 代理对象的类型与原始类不同（子类），在 `instanceof` 和类型转换时需要注意

### 10.2 hashCode 实现 `[源:第74期]`

- 避免 `String#intern()` 误用（JVM 常量池有限）
- 成员 hashCode 累加用 `* 31` 而非简单加法
- String hashCode 有缓存

`[发散]` **小马哥未覆盖的点：**

- **`* 31` 的数学原理**：`[发散]` 31 是奇素数，且 `31 * i == (i << 5) - i`，JVM 会自动优化为位移运算。`[发散]` Joshua Bloch 在《Effective Java》中推荐 `result = 31 * result + (field == null ? 0 : field.hashCode())`

- **`Objects.hash` 的陷阱**：`[发散]` `Objects.hash(a, b, c)` 内部使用 `Arrays.hashCode`，会对基本类型自动装箱。在性能敏感场景应手动计算

- **hashCode 与 HashMap 的关系**：`[发散]` HashMap 的桶位置 = `hash & (n-1)`（n 为桶数量，必须是 2 的幂次）。好的 hashCode 分布均匀可以减少 hash 冲突。`[发散]` 如果所有对象的 hashCode 都相同（如 `return 1`），HashMap 退化为链表（Java 8+ 链表长度 > 8 时转为红黑树，但仍然比正常 HashMap 慢）

### 10.3 反射性能优化 `[源:第74期]`

- **性能排序**：直接调用 > MethodHandle(static final) > Java Compiler > Lambda > Java 反射

`[发散]` **小马哥未覆盖的点：**

- **Java 反射的性能瓶颈**：`[发散]` 
  1. 方法查找（`Class.getMethod`）需要遍历方法表
  2. 参数类型检查（每次调用都检查）
  3. 基本类型装箱/拆箱
  4. 安全检查（`SecurityManager`）
  - `[发散]` `setAccessible(true)` 可以跳过安全检查，但仍不如直接调用

- **`MethodHandle` 的优势**：`[发散]` `MethodHandle` 是 Java 7 引入的，性能接近直接调用（JIT 可以内联）。但 `MethodHandle` 的创建开销大于 `Method`，适合"创建一次、多次调用"的场景

- **Lambda Metafactory**：`[发散]` Java 8+ 可以通过 `LambdaMetafactory.metafactory` 将方法引用编译为 Lambda，性能接近直接调用。`[发散]` Spring 5.x 的 `ReflectionUtils` 使用 `MethodHandle` + `LambdaMetafactory` 优化反射性能

---

## 十一、优雅停机与生命周期管理

### 11.1 Spring 优雅停机 `[源:第57,78期]`

- **Bean 销毁顺序**：`@PreDestroy` -> `DisposableBean` -> 自定义 destroy 方法 -> `SmartLifecycle#stop` -> `ContextClosedEvent`
- **Spring Cloud 优雅停机**：注册中心 deregister + 负载均衡策略
- **`SmartLifecycle`**：Spring Boot 优雅关闭关键接口

`[发散]` **小马哥未覆盖的点：**

- **Spring Boot 2.3+ 优雅停机**：`[发散]` `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s`。Spring Boot 在关闭时拒绝新请求，等待正在处理的请求完成（最长 30s），然后关闭

- **`SmartLifecycle` 的 phase**：`[发散]` `SmartLifecycle#getPhase` 返回整数，phase 值小的先启动后关闭，phase 值大的后启动先关闭。默认 phase 为 `Integer.MAX_VALUE`（最后启动、最先关闭）。`[发散]` 这意味着 Web Server（phase=`Integer.MAX_VALUE`）会在所有业务组件关闭后才关闭——但如果有长连接/WebSocket，需要更长等待时间

- **Kubernetes 优雅停机的配合**：`[发散]`
  1. Pod 收到 SIGTERM 信号
  2. Kubernetes 等待 `terminationGracePeriodSeconds`（默认 30s）
  3. 如果超时，发送 SIGKILL
  - `[发散]` 最佳实践：preStop hook（sleep 5s 让 LB 更新）+ 应用优雅停机（deregister + drain）+ `terminationGracePeriodSeconds` > 优雅停机超时

- **`ContextClosedEvent` 的时序**：`[发散]` `ContextClosedEvent` 在 Bean 销毁之前发布，这意味着在事件监听器中仍然可以使用其他 Bean。但如果监听器依赖的 Bean 在监听器之前被销毁（由于 phase 顺序），会导致 NPE——这是优雅停机中最常见的 bug

---

## 十二、AI 时代软件工程

### 12.1 AI 在软件工程中的应用 `[源:第85期]`

- **知识库**：Wiki AI 工具，内部代码需内部构建模型
- **文档化**：AI Agent 写 JavaDoc，个人 Review
- **代码测试**：分支覆盖需 Repeat Test + AI + 基础设施
- **智能化 CI/CD**：GitHub Actions、GitLab Runners

`[发散]` **小马哥未覆盖的点：**

- **AI Coding 的能力边界**：`[发散]`
  - 擅长：模板代码生成、文档编写、单元测试、代码翻译（语言间迁移）
  - 不擅长：架构设计、领域建模、复杂调试、性能优化
  - `[发散]` AI 的本质是"基于已有知识的概率生成"，无法创造新的架构模式。真正的创新仍然需要人类

- **LLM 概率性问题**：`[发散]` LLM 的输出是非确定性的（相同输入可能产生不同输出）。这对代码生成是致命的——代码必须确定性。解决方案：温度参数设为 0、使用结构化输出（JSON Schema）、Human-in-the-loop Review

- **Agent Patterns**：`[发散]`
  - **ReAct**（Reasoning + Acting）：思考->行动->观察循环
  - **Plan-and-Execute**：先规划再执行
  - **Reflexion**：执行后自我反思并改进
  - **Multi-Agent**：多个 Agent 分工协作（如 CrewAI/AutoGen）
  - `[发散]` 这些 Pattern 的本质是软件设计中的"分治"思想在 AI 领域的应用

### 12.2 FDE 工程师面试 `[源:第86期]`

- **RPC 框架设计**：网络通讯、协议设计、序列化/反序列化；Netty 内存管理
- **容器云**：Jar vs Java Agent（Docker 分层构建）、Service Mesh
- **AI Agent 平台**：SDD、Agent Patterns、LLM、RAG、Embedding

`[发散]` **小马哥未覆盖的点：**

- **RPC 框架的核心设计决策**：`[发散]`
  - 协议：二进制（Dubbo/Protobuf）vs 文本（HTTP/JSON）
  - 序列化：JSON（可读、慢）vs Protobuf（不可读、快、需 Schema）vs Hessian2（Java 生态、跨语言差）
  - 线程模型：Boss/Worker（Netty Reactor 模式）vs 线程池（Servlet 模型）
  - `[发散]` Dubbo 3.x 的 Triple 协议基于 HTTP/2 + Protobuf，兼容 gRPC——这是云原生时代的 RPC 方向

- **Service Mesh 的本质**：`[发散]` 将服务治理（负载均衡/熔断/限流/链路跟踪）从应用层下沉到基础设施层（Sidecar Proxy）。`[发散]` 好处是语言无关；坏处是增加了一跳延迟和运维复杂度。`[发散]` 对于 Java 生态，Spring Cloud + Dubbo 已经提供了完善的服务治理，Service Mesh 的收益不明显——除非有多语言需求

---

## 附：知识点与 microsphere 项目模块的映射

| 知识领域 | 相关 microsphere 模块 | 核心知识点 |
|---------|---------------------|-----------|
| Spring IoC | microsphere-spring | Bean 生命周期监听、并行 Bean 初始化、`BeanListener` |
| Spring AOP | microsphere-spring | 拦截器链、`BeanPostProcessor` 扩展 |
| Spring Boot | microsphere-spring-boot | 自动装配、`@Conditional` 扩展、Actuator 端点 |
| Spring Cloud | microsphere-spring-cloud | 服务注册适配、配置推送、`@RefreshScope` 扩展 |
| 线程池 | microsphere-devops | `ThreadPoolExecutor` 可观测性、动态参数 |
| 事务 | microsphere-dynamic | 动态数据源、`@Transactional` 扩展 |
| 可观测性 | microsphere-observability | Tracing/Logging/Metrics 整合 |
| 容错 | microsphere-resilience4j | 熔断/重试/限流 |
| 配置 | microsphere-configuration | 多配置中心适配（Apollo/Nacos/etcd/ZK） |
| 网关 | microsphere-gateway | Spring Cloud Gateway 扩展 |
| 多活 | microsphere-multiactive | 跨区域注册、同区域优先 |
| 链路跟踪 | microsphere-observability | Trace ID 透传、Span 管理 |
