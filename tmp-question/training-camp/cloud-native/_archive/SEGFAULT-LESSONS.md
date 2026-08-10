# segmentfault-lessons 完整资源整合

> 小马哥思否讲堂代码（500 Java 文件，1428 Star），含 3 大系列 40+ 子项目。
> 训练营主线的"实践补充包"——训练营讲架构和理论，这些是基本功练习。

---

## 一、Spring Boot 系列（20 课，118 文件）

### 已整合到 Phase 1（工程基础）

| lesson | 内容 | 关键代码 | 价值 |
|:------:|------|---------|:----:|
| 1 | Hello World + war 部署 | `SpringBootServletInitializer` | 入门 |
| 3 | REST 多格式（JSON/XML/HTML） | `HTMLRestController`, `JSONRestController`, `XMLRestController` | 内容协商 |
| 4 | Servlet/Filter/Listener 注册 | `MyServlet`, `MyFilter`, `MyServletRequestListener` | 原生 Servlet 整合 |
| 5 | 嵌入式 Tomcat | 自定义 `EmbeddedServletContainerCustomizer`（端口8888/NIO2协议）+ 独立 tomcat-test 工程 + Servers/ 目录 server.xml | 容器原理 |
| 6 | JDBC 原生访问 | `JdbcController` → `UserService` → `User` | 数据库基础 |
| 7 | MyBatis 整合 + 逆向工程 | `mybatis-generator-maven-plugin` | ORM 切换 |
| 12 | Bean Validation 自定义 | `@PersonNamePrefix` + `PersonNamePrefixConstraintValidator` | 校验扩展 |
| 13 | WebSocket 聊天室 + 异步 Servlet | `ChatRoomServerEndpoint` (@OnOpen/@OnMessage/@OnClose) + `AsyncServlet` | 实时通信 |
| 18 | 多源配置注入 ⭐ | `@Value` + `EnvironmentAware` + `@ConfigurationProperties` + `PropertySource` 优先级链，`ApplicationEnvironmentPreparedEvent` 监听器 + `spring.factories` | **配置优先级是 Spring Boot 最容易被忽视的核心概念** |
| 20 | 自定义 Starter ⭐⭐⭐ | `PersonAutoConfiguration` (@ConditionalOnWebApplication/@ConditionalOnProperty/@AutoConfigureAfter) | **理解 Spring Boot 自动装配的最佳入口** |

### 已整合到 Phase 5（数据架构）

| lesson | 内容 | 关键代码 | 价值 |
|:------:|------|---------|:----:|
| 8 | JPA 实体映射 ⭐ | `VipCustomer extends Customer` 继承策略 + `@EntityListeners(CustomerListener)` | JPA 关系映射 |
| 9 | Elasticsearch | `ElasticsearchConfiguration` (本地模式 + NodeBuilder)，`BookElasticsearchRepository` (程序化 Repository) 和 `BookRepository` (接口自动推导) 双模式 | NoSQL |
| 10 | Spring Cache | `SimpleCacheManager` + `ConcurrentMapCache` + `@Cacheable` 注解 + `RedisTemplate` 编程式双模式 | 缓存抽象 |
| 11 | Kafka 消息 | `KafkaController`, `ObjectSerializer/Deserializer`, `ConsumerListener` | 消息队列 |

### 已整合到 Phase 7/11（容错/安全/JVM）

| lesson | 内容 | 关键代码 | 价值 |
|:------:|------|---------|:----:|
| 15 | Spring Security ⭐ | `WebSecurityConfiguration` (CSRF/CSP/X-Frame-Options/XSS) + `AllowFromStrategy` 白名单 | 安全配置 |
| 16 | Logback/Log4j | `LogbackController` + `Log4jTest` (DOMConfigurator 热加载 + MDC) | 日志框架 |
| 17 | JMX 三种风格 ⭐⭐ | Spring @ManagedResource、标准 MBean (AttributeChangeNotification)、动态 MBean (getMBeanInfo 自描述) + `JMXController` REST 暴露 | **JVM 监控三种注册方式全覆盖** |
| 19 | Spring Boot 测试 ⭐ | **7 种测试风格**：纯单元/ @SpringBootTest/ @WebMvcTest/ MockMvc/ @TestPropertySource/ 自定义 TestExecutionListener/ MockEnvironment | **训练营体系唯一的测试代码** |
| 14 | SOAP Web Services | `@Endpoint` + `@PayloadRoot` + JAXB + XSD Schema + `WSDL11Definition` | SOAP 服务端/客户端 |

> ⚠️ lesson-2 无代码（仅课件），lesson-14 SOAP 过时但仍可学设计模式

---

## 二、Spring Cloud 系列（16 课，183 文件）

### 已整合到 Phase 3（手写中间件）

| lesson | 内容 | 关键代码 | 价值 |
|:------:|------|---------|:----:|
| **13** | **自定义 Stream Binder** ⭐⭐⭐ | `ActiveMQMessageChannelBinder implements Binder<MessageChannel, ConsumerProperties, ProducerProperties>` | **理解 SPI 扩展机制** |
| 1 | Spring Event ⭐⭐ | `ApplicationEvent` + `ApplicationListener<E>` 泛型监听 + `AnnotationConfigApplicationContext.publishEvent()` | **EventDispatcher 设计的原型** |
| 3 | Config Server | Git 后端 + `@ConfigurationProperties` + `ContextRefresher` 每秒轮询 | 配置中心 |

### 已整合到 Phase 6/7（可观测性/容错）

| lesson | 内容 | 关键代码 | 价值 |
|:------:|------|---------|:----:|
| 12 | Stream 消息 | `@Output`/`@Input` 管道 + `ObjectSerializer` (Java 原生序列化) + `@StreamListener`/`@ServiceActivator`/`编程订阅` 三种接收方式 | 消息驱动 |
| 14 | Bus 远程事件 ⭐⭐ | `UserRemoteApplicationEvent extends RemoteApplicationEvent` + `@RemoteApplicationEventScan` + `BusEventController` REST 触发 | **分布式事件广播** |

### 过时但概念有价值

| lesson | 内容 | 概念价值 | API 状态 |
|:------:|------|:----:|:----:|
| 2 | Config Client | `PropertySourceLocator` + `BootstrapConfiguration` 机制 | ⚠️ 概念有用 |
| 4 | Eureka | Server+Client+Config 联动 | ❌ Eureka 停更 |
| 5 | Consul | `@EnableDiscoveryClient` + `/check` 自定义健康检查端点 vs Eureka 心跳 | ⚠️ 换 Nacos |
| 6-7 | Ribbon | 自定义 `MyPing` (/health 端点检测) + `MyRule` (永远选最后一台) | ❌ Ribbon 停更 |
| 8-10 | Hystrix | 自定义 `HystrixCommand` 继承类 + Hystrix Dashboard | ❌ Hystrix 停更 |
| 11 | Zuul | API 网关 | ❌ 换 Gateway |
| 15 | Zipkin/Sleuth | @EnableZipkinStreamServer + 10 子模块完整追踪体系 | ❌ 换 OpenTelemetry |

### ❌ 直接跳过
- lesson-16: 仅有回顾 PDF

---

## 三、「一入 Java 深似海」系列（8 期，199 文件）

### Session 1: 语言基础 + OOP + 函数式
**路径**: `deep-in-java/stage-1/`
- 泛型擦除、通配符、方法设计、`@FunctionalInterface` 接口 (Consumer/Function/Supplier/Predicate)、Stream API

### Session 2: 集合框架
- `FailFastVsFailSafeDemo`: ArrayList(modCount) vs CopyOnWriteArrayList(快照)
- `IdentityHashMapDemo`: == 比较 vs equals()
- `WeakReferenceMapDemo`: WeakReference + ReferenceQueue + GC
- `PriorityQueueDemo`: 小顶堆自然排序

### Session 3: 并发编程（31 文件）⭐⭐
- `DeadLockDemo`: 循环等待四个必要条件
- `ProducerConsumerProblemDemo`: wait()/notify() 协作，while 防虚假唤醒
- `ReentrantLockDemo`: getHoldCount() 递归重入，tryLock 超时，Condition
- `CountDownLatchDemo`: 一次性计数器 vs `CyclicBarrierDemo` 可重用屏障
- `AtomicDemo`: Unsafe/CAS/volatile 语义
- 还有 ReadWriteLock/Semaphore/ConcurrentHashMap/CopyOnWriteArrayList/CompletableFuture

### Session 4: JMM + AQS ⭐⭐
- Happens-Before 关系、AQS 原理 + **C 语言 POSIX Thread 代码**

### Session 5: 类加载
- `ClassLoadingDemo`: 自定义 MyClassLoader + `defineClass()` vs `loadClass()` 双亲委派对比
- 同名 Class 的 == 比较（解释了 Spring DevTools Class!=Class 问题）

### Session 6: GC 调优
- 传统 GC + G1 调优

### Session 7: 反射 + APT + 字节码 ⭐⭐⭐
- `RepositoryAnnotationProcessor` 完整编译期注解处理器
- CGLib/JDK 动态代理对比
- **这是 microsphere-annotation-processor 的蓝本**

### Session 8: I/O + NIO（26 文件）⭐⭐
- `SelectorServerDemo`: 完整 Selector 多路复用 (register OP_ACCEPT + selectedKeys + iterator.remove())
- `ChannelClientDemo/ServerDemo`: 非阻塞 (configureBlocking(false) + finishConnect())
- `BufferDemo`: Buffer position/limit/capacity 元数据
- `DecoratingCharSequence` (装饰器模式) vs `CharSequenceProxy` (代理模式) 对比

---

## 四、与训练营主线的整合策略

```
训练营主线（理论+架构）：              segfault-lessons（实践补充）：

Phase 1 工程基础                        Boot 1-7, 12, 13, 18, 20
Phase 2 分布式理论                       Java深 1, 2, 4, 5
Phase 3 手写中间件                       Cloud 1, 3, 13
Phase 4 事务体系                         —
Phase 5 数据架构                         Boot 8-11
Phase 6 可观测性                         Cloud 12, 15
Phase 7 容错模式                         Java深 3, Cloud 8-10(概念)
Phase 8 性能实战                         Java深 6
Phase 9 云原生部署                       —
Phase 10 多活架构                        —
Phase 11 平台工程                        Boot 14, 15, 16, 17, 19; Java深 7, 8
```

### 关键说明

| 资源 | 处理 |
|------|------|
| Boot lesson-20 自定义 Starter | **Phase 1 必学**——Microsphere AutoConfiguration 体系的前置知识 |
| Cloud lesson-13 自定义 Stream Binder | **Phase 3 自学**——SPI 扩展机制设计模式 |
| Java深 Session 7 APT 处理器 | **Phase 11 自学**——microsphere-annotation-processor 的原理 |
| Boot lesson-19 测试（7 风格） | **Phase 1 必补**——训练营完全缺失测试内容 |
| Boot lesson-18 配置优先级链 | **Phase 1 理解**——Spring Boot 最容易出错的核心概念 |
| Cloud lesson-1 Spring Event | **Phase 3 理解**——EventDispatcher 设计的原型 |
| Java深 Session 8 NIO | **Phase 3 理解**——Netty 的前置知识 |
