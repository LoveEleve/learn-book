# Java 分布式架构师 · 完整课程体系

> 覆盖：Java 核心（Java深 199文件）+ Spring Boot（20课）+ Spring Cloud（16课）+ 分布式架构（训练营120+课）
> 每章以概念讲解和动手练习为主，原始训练营文档作为课后参考阅读。

---

## 总览：三大支柱，六段递进

```
支柱一：Java 核心能力     支柱二：框架工程能力       支柱三：分布式架构能力
(JavaDeep 8期)           (Boot 20课 + Cloud 16课)   (训练营 4期 + 云原生)

阶段1: Java 基础 (4周)    阶段3: Spring Boot (4周)   阶段5: 分布式理论 (4周)
阶段2: Java 进阶 (4周)    阶段4: 微服务基础 (4周)    阶段6: 架构实战 (6周)
```

---

# 阶段 1：Java 核心基础（4 周）

## 第 1 周：面向对象与泛型（JavaDeep 第1-2期）

### 1.1 枚举的字节码本质
- **概念**：`enum` 是语法糖，编译后继承 `Enum<E>`，`public static final` 常量
- **参考代码**：`stage-1/lesson-2/EnumClassDemo.java`（207行，演示手写枚举 vs Java enum）
- **动手**：用 `javap -c` 看 enum 的字节码，标注编译器自动生成的方法（values(), valueOf()）

### 1.2 泛型擦除与 PECS
- **概念**：编译器在编译期检查泛型，运行时所有泛型信息被擦除为 Object 或上界类型
- PECS：Producer-Extends（读用 `? extends T`），Consumer-Super（写用 `? super T`）
- **参考代码**：`stage-1/lesson-3/GenericTypeErasureDemo.java`，`GenericWildcardsTypeDemo.java`
- **动手**：写一段代码证明泛型在运行时被擦除（用反射往 `List<String>` 里塞一个 Integer）

### 1.3 函数式四大接口
- **概念**：Consumer(消费)、Function(转换)、Predicate(断言)、Supplier(供给) 是一切函数式编程的基础
- **参考代码**：`stage-1/lesson-4/` 下全部文件
- **动手**：用 Function.andThen() 组合两个函数实现"先转大写再加前缀"，用 Predicate.and() 组合两个判断条件

### 1.4 Integer 缓存池
- **概念**：`Integer.valueOf(127)` 和 `Integer.valueOf(128)` 的行为不同——前者命中缓存，后者 new 新对象
- **参考代码**：`stage-1/lesson-2/IntegerDemo.java`
- **动手**：用 `==` 比较 `Integer.valueOf(127)` 两次 vs `Integer.valueOf(128)` 两次，解释结果

### 1.5 集合框架核心
- **Fail-Fast vs Fail-Safe**：ArrayList 遍历时修改 → ConcurrentModificationException（modCount 机制），CopyOnWriteArrayList 基于快照，安全删除
- **IdentityHashMap**：`==` 比较而非 `equals()` 比较，适用场景是区分不同实例的 key
- **WeakHashMap**：GC 回收 Key 后自动清理 Entry，防止内存泄漏
- **参考代码**：`stage-2/` 下各文件
- **动手**：用 ArrayList 遍历时 remove，看抛什么异常；换成 CopyOnWriteArrayList 再试

---

## 第 2 周：并发编程（JavaDeep 第3期）

### 2.1 synchronized 底层
- **对象头 Mark Word**：32/64位中存储 hashCode、GC 分代年龄、锁状态标志
- **锁升级**：无锁 → 偏向锁 → 轻量级锁（CAS） → 重量级锁（Monitor，OS mutex）
- **参考代码**：`stage-3/lesson-2/` 下 Synchronized 系列文件
- **动手**：写两段代码——同一对象锁 vs 不同对象锁，测试并发性能差异

### 2.2 ReentrantLock vs synchronized
- **区别**：Lock 可中断(tryLock)、可超时、可公平、可多条件(Condition)
- **重入**：`getHoldCount()` 查看重入次数，递归调用时递增
- **参考代码**：`stage-3/lesson-3/ReentrantLockDemo.java`
- **动手**：写一个递归方法，用 ReentrantLock 观察 `getHoldCount()` 变化

### 2.3 同步三件套
- **CountDownLatch**：一次性计数器，await() 等待 countDown() 到零（启动等待依赖服务就绪）
- **CyclicBarrier**：可重用屏障，所有参与者到齐才触发（批量任务并行执行后汇总）
- **Semaphore**：控制并发数（连接池限流）
- **参考代码**：`stage-3/lesson-3/` 下三个 Demo
- **动手**：用 CountDownLatch 实现"等待三个微服务全部启动后才开始处理请求"

### 2.4 死锁四条件
- 互斥 + 持有并等待 + 不可剥夺 + 循环等待 = 死锁
- **参考代码**：`stage-3/lesson-2/DeadLockDemo.java`
- **动手**：写一段死锁代码，用 jstack 定位死锁线程

---

## 第 3 周：并发进阶（JavaDeep 第3-4期）

### 3.1 CAS 与 Atomic 类
- **CAS**：CPU 指令级 compare-and-swap，Unsafe 类是入口（Java 9+ 换 VarHandle）
- **ABA 问题**：A→B→A，CAS 检测不出中间变化 → AtomicStampedReference 解决
- **参考代码**：`stage-3/lesson-3/AtomicDemo.java`
- **动手**：用反射获取 Unsafe 实例，手动 CAS 修改一个 volatile 字段

### 3.2 AQS 原理
- **CLH 队列**：每个线程封装成 Node，前驱节点 SIGNAL → 后继节点 park → 前驱释放时唤醒后继
- **参考代码**：`stage-4/lesson-3/AbstractQueuedSynchronizerDemo.java`
- **动手**：手写一个简易 ReentrantLock（基于 AQS 的 tryAcquire/tryRelease）

### 3.3 Java 内存模型
- **8 条 Happens-Before 规则**：程序次序、volatile、锁、线程启动/终止、中断、终结器、传递性
- **参考代码**：`stage-4/lesson-2/HappensBeforeRelationshipDemo.java`
- **动手**：写一段代码证明 volatile 变量的写入对后续读可见（不加 volatile 看不到变化）

### 3.4 CompletableFuture
- **异步编排**：thenApply(转换) / thenCompose(扁平化) / thenCombine(合并) / allOf(全部完成)
- **参考代码**：`stage-3/lesson-4/CompletableFutureDemo.java`
- **动手**：用 CompletableFuture 实现"同时查 3 个微服务，汇总结果后返回"

---

## 第 4 周：JVM 核心（JavaDeep 第5-6期 + 训练营 Stage 3）

### 4.1 双亲委派
- **三层 ClassLoader**：Bootstrap(rt.jar) → Extension(ext/) → Application(classpath)
- **委派流程**：子 Loader 先问父 Loader 是否已加载 → 没加载就问父能不能加载 → 父不行才自己加载
- **打破双亲委派**：Tomcat（WebAppClassLoader）、JDBC SPI（ThreadContextClassLoader）
- **参考代码**：`stage-5/ClassLoadingDemo.java`（MyClassLoader 实现 + defineClass vs loadClass 对比）
- **动手**：写一个 ClassLoader 从指定目录加载 .class 文件，证明同一类由不同 ClassLoader 加载时 `==` 返回 false

### 4.2 GC 算法与调优
- **G1 GC**：JDK 17+ 默认，Region 分代模型，停顿预测（-XX:MaxGCPauseMillis）
- **ZGC**：JDK 21+ 推荐，亚毫秒暂停，8MB-16TB 堆，彩色指针 + 负载屏障
- **参考数据**：训练营 Stage 3 实测 ZGC 100 TPS vs G1 11.2 TPS
- **参考代码**：`stage-3/docs/05.`（395 行完整讲解），`stage-3/papers/Deep Dive into ZGC.pdf`
- **动手**：用 jstat 监控你的应用 GC 频率和耗时，设置不同的 GC 算法对比

### 4.3 JMX 监控
- **三种方式**：Spring @ManagedResource、标准 MBean（XxxMBean 接口）、DynamicMBean
- **参考代码**：`segfault-lessons/Boot lesson-17`（三种风格完整实现）
- **动手**：把一个 Spring Bean 注册到 JMX，用 JConsole 连接并修改属性值

---

# 阶段 2：Java 高级特性（2 周）

## 第 5 周：反射与字节码（JavaDeep 第7期）

### 5.1 JDK 动态代理 vs CGLib
- **JDK 代理**：只能代理接口，Proxy.newProxyInstance + InvocationHandler
- **CGLib**：代理类，Enhancer + MethodInterceptor，基于 ASM 生成子类
- **Spring AOP 选择**：如果目标实现了接口 → JDK 代理；否则 → CGLib（proxy-target-class=true 强制 CGLib）
- **参考代码**：`stage-7/lesson-4/CGLibDemo.java`（146行），`JavaDynamicProxyDemo.java`（143行）
- **动手**：写一个 InvocationHandler 统计方法调用耗时，对比 JDK 代理和 CGLib 代理的性能

### 5.2 APT 编译期注解处理
- **原理**：javac 编译时调用 AbstractProcessor.process()，可以读取源码结构、生成新文件
- **两阶段**：处理阶段（收集注解信息）→ 完成阶段（processingOver，写入生成的资源文件）
- **代表框架**：Lombok（生成 getter/setter）、MapStruct（生成 Mapper 实现）、Dagger2
- **参考代码**：`stage-7/lesson-3/RepositoryAnnotationProcessor.java`（149行完整实现）
- **动手**：写一个 APT 处理器，扫描所有 @ToString 注解的类，生成 META-INF/tostring.properties

### 5.3 Java Beans 内省
- **Introspector**：分析 Bean 的属性、事件、方法，获取 BeanInfo
- **PropertyEditor**：String ↔ 对象类型转换（Spring 的 TypeConverter 底层就是它）
- **参考代码**：`stage-7/lesson-2/PersonIntrospectionDemo.java`，`IdPropertyEditor.java`
- **动手**：写一个 PropertyEditor 将 "1,2,3" 字符串解析成 List<Integer>

---

## 第 6 周：NIO 与网络编程（JavaDeep 第8期）

### 6.1 装饰器 vs 代理
- **装饰器**：实现同一接口，持有被装饰对象，**透明增强**行为
- **代理**：不一定实现同一接口，**控制访问**，只暴露子集功能
- **参考代码**：`stage-8/lesson-1/DecoratingCharSequence.java`（装饰器：加 @ 前缀），`CharSequenceProxy.java`（代理：只代理 toString）
- **动手**：识别 Java IO 中的装饰器（BufferedReader → InputStreamReader → FileInputStream）

### 6.2 NIO 三件套：Buffer / Channel / Selector
- **Buffer**：position/limit/capacity 三元组。flip() = limit=position, position=0。clear() = 重置到写模式
- **Channel**：双向读写。FileChannel.transferTo() 零拷贝（sendfile 系统调用）
- **Selector**：单线程管理多连接。register(OP_ACCEPT/OP_READ/OP_WRITE) + selectedKeys()
- **参考代码**：`stage-8/lesson-4/SelectorServerDemo.java`（58行完整实现）
- **动手**：用 Selector 写一个单线程 Echo Server，能同时处理 100 个客户端连接

### 6.3 NIO → Netty
- **Selector** → EventLoopGroup（boss/worker 线程模型）
- **SocketChannel** → NioSocketChannel
- **ByteBuffer** → ByteBuf（自动扩容 + 池化 PooledByteBufAllocator）
- **手写 Pipeline** → ChannelPipeline + ChannelHandler 链
- **参考**：训练营 `rpc-project` 的 Netty 实现（MessageEncoder/Decoder + InvocationRequestHandler）

---

# 阶段 3：Spring Boot 工程能力（4 周）

## 第 7 周：从零搭建到上线

### 7.1 Spring Boot 自动装配原理
- **@EnableAutoConfiguration → spring.factories → AutoConfigurationImportSelector**
- **条件装配**：@ConditionalOnClass（类存在）、@ConditionalOnMissingBean（Bean 不存在）、@ConditionalOnProperty（配置开关）
- **参考代码**：`segfault-lessons/Boot lesson-20`（PersonAutoConfiguration 三件套完整实现）
- **动手**：写一个自动配置类，当项目中存在 RedisTemplate 时自动注册缓存管理器

### 7.2 REST API 多格式输出
- **内容协商**：同一 URL，Accept: application/json → 返回 JSON；Accept: application/xml → 返回 XML
- **参考代码**：`segfault-lessons/Boot lesson-3`（JSON/XML/HTML 三种格式 + RestTemplate 客户端调用）
- **动手**：实现同一个 Controller 方法同时支持 JSON 和 XML 输出

### 7.3 Servlet 三大组件注册
- **两种方式**：@WebServlet/@WebFilter/@WebListener 注解扫描 + ServletRegistrationBean 编程注册
- **参考代码**：`segfault-lessons/Boot lesson-4`
- **动手**：用编程方式注册一个 Filter，拦截所有请求并记录耗时

### 7.4 嵌入式 Tomcat 定制
- **WebServerFactoryCustomizer**：定制端口、协议、线程池、ContextPath
- **参考代码**：`segfault-lessons/Boot lesson-5`（端口 8888 + NIO2 协议）
- **动手**：把应用端口改为 9090，协议改为 HTTP/2

---

## 第 8 周：数据访问层

### 8.1 JDBC → MyBatis → JPA（三层递进）
- **JDBC**：原生 DataSource + PreparedStatement + ResultSet。最灵活，最繁琐
- **MyBatis**：SQL 写在 XML 或注解中。自定义 TypeHandler（JSON 字段 ↔ Java 对象），逆向工程生成代码
- **JPA**：@Entity + @OneToMany/@ManyToOne，EntityListener 生命周期回调，继承策略（TABLE_PER_CLASS）
- **参考代码**：`segfault-lessons/Boot lesson-6/7/8`
- **动手**：分别用 JDBC 和 MyBatis 写同一个查询，对比代码量和性能

### 8.2 缓存策略
- **@Cacheable**：声明式缓存，基于 AOP 自动缓存方法返回值
- **编程式**：CacheManager.getCache().put() 手动控制
- **多级缓存**：ConcurrentMap 一级 + Redis 二级（先查内存，miss 才查 Redis）
- **参考代码**：`segfault-lessons/Boot lesson-10`
- **动手**：实现一个方法，第一次调用查数据库（慢），第二次从缓存取（快）

### 8.3 Elasticsearch 集成
- **Spring Data ES**：@Document 注解映射索引，Repository 接口（findByName 自动推导查询）
- **参考代码**：`segfault-lessons/Boot lesson-9`
- **动手**：把 MySQL 中的商品数据同步到 ES，实现全文搜索

---

## 第 9 周：消息与安全

### 9.1 Kafka 集成
- **KafkaTemplate**：生产消息，支持 String 和自定义序列化对象
- **@KafkaListener**：消费消息，自动 ACK
- **序列化**：自定义 `Serializer<Object>` 用 Java 原生序列化（生产建议用 JSON/Protobuf）
- **参考代码**：`segfault-lessons/Boot lesson-11`
- **动手**：发一条"订单创建"消息，消费者异步更新库存

### 9.2 Spring Security 防护
- **CSRF**：CookieCsrfTokenRepository，POST 请求验证 Token
- **CSP**：contentSecurityPolicy 限制脚本来源
- **X-Frame-Options**：AllowFromStrategy 白名单防止点击劫持
- **XSS**：xssProtection 浏览器级防护
- **参考代码**：`segfault-lessons/Boot lesson-15`（完整 68 行配置）
- **动手**：配置 Spring Security，只允许同域 POST，阻止 iframe 嵌入

### 9.3 测试
- **7 种风格**：纯 JUnit → @SpringBootTest → @WebMvcTest → MockMvc → @TestPropertySource → TestExecutionListener → MockEnvironment
- **参考代码**：`segfault-lessons/Boot lesson-19`（7 种风格全部有代码）
- **动手**：用 @WebMvcTest + MockMvc 测试 Controller 的正确响应和错误处理

---

## 第 10 周：高级特性

### 10.1 WebSocket 实时通信
- **@ServerEndpoint**：基于 JSR 356，@OnOpen/@OnMessage/@OnClose，ConcurrentHashMap 管理 Session
- **异步 Servlet**：request.startAsync() + AsyncContext，长连接场景
- **参考代码**：`segfault-lessons/Boot lesson-13`
- **动手**：实现一个聊天室，所有人发的消息实时广播给所有人

### 10.2 自定义 Starter
- **三要素**：AutoConfiguration 类 + spring.factories 注册 + @ConfigurationProperties 绑定
- **条件控制**：@ConditionalOnWebApplication、@ConditionalOnProperty
- **参考代码**：`segfault-lessons/Boot lesson-20`
- **动手**：写一个 Starter，引入后自动注册全局请求日志拦截器

### 10.3 JMX 监控
- **Spring @ManagedResource**：注解驱动，最简单
- **标准 MBean**：XxxMBean 接口 + 实现类，类型安全
- **DynamicMBean**：运行时动态定义属性和操作
- **参考代码**：`segfault-lessons/Boot lesson-17`
- **动手**：把数据库连接池注册为 MBean，用 JConsole 查看活跃连接数

### 10.4 @ConfigurationProperties
- **四种注入方式**：@Value、@ConfigurationProperties、EnvironmentAware、编程式添加 PropertySource
- **优先级链**：命令行参数 > application-listener > java-code > application-prod.properties > application.properties
- **参考代码**：`segfault-lessons/Boot lesson-18`
- **动手**：用 @ConfigurationProperties 绑定一组配置，通过 EnvironmentChangeEvent 监听变更

---

# 阶段 4：微服务架构基础（4 周）

> ⚠️ segfault-lessons Cloud 系列基于 Spring Cloud Dalston（2017 年），Eureka/Ribbon/Hystrix/Zuul/Sleuth 全部已过时。
> 下面用现代等价方案讲解，原始代码仅作为概念参考。

## 第 11 周：服务发现与配置中心

### 11.1 服务发现：从 Eureka 到 Nacos
- **Eureka 教了什么**：AP 模型（自我保护）、心跳续约、多级缓存、Peer 同步
- **为什么换到 Nacos**：Eureka 2.0 跳票，Nacos 同时支持 CP（配置）和 AP（服务发现）
- **临时实例 vs 持久实例**：微服务 Pod → 临时（心跳保活），数据库 → 持久（手动管理）
- **核心 API 不变**：`@EnableDiscoveryClient` + `DiscoveryClient.getInstances()`
- **参考**：`segfault-lessons/Cloud lesson-4/5`（理解服务注册发现的概念），训练营 `microsphere-nacos`（130+ 文件，完整 Nacos 客户端实现）
- **动手**：启动一个 Nacos Server，注册两个服务实例，用 DiscoveryClient 查询

### 11.2 配置中心：从 Config Server 到 Nacos Config
- **Config Server 教了什么**：Git 后端、Bootstrap 上下文、ContextRefresher 轮询刷新
- **为什么换到 Nacos Config**：自动推送（长轮询/GRPC）、版本管理、灰度发布、一键回滚
- **参考**：`segfault-lessons/Cloud lesson-2/3`
- **动手**：在 Nacos 上修改数据库连接配置，观察服务是否自动刷新

---

## 第 12 周：负载均衡与容错

### 12.1 负载均衡：从 Ribbon 到 Spring Cloud LoadBalancer
- **Ribbon 教了什么**：IRule（负载规则）、IPing（健康检查）、ZoneAwareLoadBalancer（区域感知）
- **SCL 怎么替代**：`ServiceInstanceListSupplier` → Reactor `Flux<ServiceInstance>` → `RoundRobinLoadBalancer`
- **自定义规则**：从"继承 AbstractLoadBalancerRule"改为"实现 ServiceInstanceListSupplier.get()"
- **参考**：`segfault-lessons/Cloud lesson-6/7`（理解负载均衡的概念）
- **动手**：实现一个 LoadBalancer，永远选择 CPU 使用率最低的实例

### 12.2 断路器：从 Hystrix 到 Resilience4j + Istio
- **Hystrix 教了什么**：断路器三态（CLOSED→OPEN→HALF_OPEN）、线程池隔离、Fallback
- **Resilience4j 怎么做**：`@CircuitBreaker(name="x", fallbackMethod="f")`，装饰器模式，不用线程池隔离
- **2026 年分层**：HTTP 调用层的熔断/重试/超时 → Istio DestinationRule；DB 连接池/业务降级 → Resilience4j
- **参考**：`segfault-lessons/Cloud lesson-8/9/10`
- **动手**：用 Resilience4j @CircuitBreaker 实现"调用第三方 API 3 次失败后熔断 10 秒"

---

## 第 13 周：网关与消息

### 13.1 网关：从 Zuul 到 Spring Cloud Gateway
- **Zuul 教了什么**：ZuulFilter 四阶段（pre/route/post/error）、路由转发
- **Gateway 怎么做**：Route = ID + URI + Predicate（匹配条件）+ Filter（拦截处理），基于 WebFlux 非阻塞
- **参考**：`segfault-lessons/Cloud lesson-11`
- **动手**：配置 Gateway 将 `/order/**` 路由到 order-service，`/user/**` 路由到 user-service

### 13.2 消息驱动：Stream + Bus
- **Stream 教了什么**：@Input/@Output 管道抽象、@StreamListener/@ServiceActivator 消费、自定义 Binder
- **2026 年**：Spring Cloud Stream 依然是最佳选择，Binder 支持 Kafka/RabbitMQ
- **Bus 教了什么**：RemoteApplicationEvent 分布式事件广播 → 2026 年可以用 Kafka + 自定义事件
- **参考**：`segfault-lessons/Cloud lesson-12/13/14`
- **动手**：用 Stream 实现"用户注册"事件，订单服务、积分服务分别消费

---

## 第 14 周：链路追踪与可观测性

### 14.1 链路追踪：从 Sleuth 到 OpenTelemetry
- **Sleuth 教了什么**：Span 模型（TraceId/SpanId/ParentSpanId）、Header 传播、MDC 日志注入
- **OTel 怎么做**：Java Agent 零代码注入、W3C traceparent 标准、OTLP 协议导出
- **参考**：`segfault-lessons/Cloud lesson-15`
- **动手**：用 OTel Java Agent 启动应用，在 Jaeger 上查看调用链

### 14.2 指标监控
- **Micrometer**：Timer/Counter/Gauge/DistributionSummary 四种指标类型
- **Prometheus**：Pull 模式（/actuator/prometheus）+ Grafana 面板
- **参考**：训练营 Stage 1 的 Micrometer 讲解

---

# 阶段 5：分布式理论基石（4 周）

> 内容来自训练营 Stage 2 的 Paxos/Raft/ZAB/ZK/Nacos 核心课程，已在 CURRICULUM.md 模块二中展开。

---

# 阶段 6：架构实战（6 周）

> 内容来自训练营 Stage 2-4 的 RPC 框架、事务体系、多活架构、Shopizer 优化，后续展开。

---

## 附录：已过时内容对照表

| segfault-lessons 课程 | 讲了什么概念（保留） | 用什么替代（2026） |
|----------------------|-------------------|------------------|
| Cloud L4-5 Eureka | 服务注册发现、心跳、AP 模型 | Nacos / K8s Service |
| Cloud L6-7 Ribbon | IRule 负载策略、IPing 健康检查 | Spring Cloud LoadBalancer |
| Cloud L8-10 Hystrix | 断路器状态机、隔离、Fallback | Resilience4j + Istio |
| Cloud L11 Zuul | 网关路由、Filter 链 | Spring Cloud Gateway |
| Cloud L15 Sleuth | Span 模型、TraceId 传播 | Micrometer Tracing + OTel |
| Boot L14 SOAP | XML Schema、WSDL、JAXB | REST / gRPC（业务上已不再使用 SOAP） |
| Boot 全部 | Spring Boot 1.5.8 API | Spring Boot 3.x（javax→jakarta、WebServerFactoryCustomizer 等） |
