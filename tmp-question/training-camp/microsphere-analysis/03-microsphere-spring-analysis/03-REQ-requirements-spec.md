# 03-REQ：microsphere-spring 完整需求规格

> 本文是 microsphere-spring 的完整需求文档。所有需求分三类：
> 1. **已实现需求**（REQ-001~018，18 项）——现有代码已完成，每项均标记"📝 待编写原理文档"
> 2. **待实现需求**（REQ-D01~D09，9 项）——bug 修复
> 3. **全新发散**（REQ-N01~N08，8 项）——生产环境还需要但项目未覆盖的能力
>
> **基准环境**：Java 17+，Spring Framework 6.0~7.0，项目 CI 已测试 JDK 17/21/25

---

### 与 Spring Framework 6.2.17 重叠按包分布（v2，源码已验证）

> 经逐文件+逐方法对照 Spring Framework v6.2.17 `/data/workspace/source-code/code/spring/spring-framework/`。

| 包 | 文件数 | Spring 重叠 | 判定 |
|---|:---:|:---:|------|
| net | 10 | ~0% | spring: URL 协议 Spring 无 |
| context/event | 30 | ~5% | 拦截链/BeanFactoryListener/并行预实例化全无 |
| context/config+env | 66 | ~10% | PropertySource 通配符/排序/继承/自动刷新全新 |
| cache | 15 | ~15% | TTL 全新但复用 Cache 抽象，⚠️ Redis 集成是死代码 |
| context/core | 46 | ~25% | 工具类部分与 Spring 重叠 |
| web | 58 | ~15% | WebEndpointMapping 统一端点模型 Spring 无 |
| webmvc/webflux | 59 | ~20% | 基于 Spring 扩展点实现，增量增强 |
| beans/factory | 73 | ~35% | AnnotatedInjection 与 Autowired 逻辑重叠拉高均值 |
| jdbc/guice/test | 45 | ~10% | 集成层 |

**结论：~70% 能力 Spring Framework 6.2.17 完全没有，不是冗余实现。**

---

## 项目定位

**microsphere-spring 是 Spring Framework 的生产增强插件库**，解决的核心问题是：**Spring 给了你扩展点（BeanPostProcessor / ImportSelector / ApplicationListener），但很多"应该内置"的能力没有给——启动太慢、PropertySource 太弱、缓存没有 TTL、Bean 生命周期没有事件、端点没有统一注册表。microsphere-spring 不重写 Spring，它通过 Spring 自己的扩展机制把这些缺口补上。**

与前面项目的层次关系：
- **01-confucius-commons**（最底层）——JDK 锁住的内部能力
- **02-microsphere-java**（中间层）——JDK 给的太原始，加工成顺手工具
- **03-microsphere-spring**（应用层）——Spring 没给的生产必需能力，通过标准扩展点注入

**源码信息**：
- 路径：`/data/workspace/java-training-camp/cloud-native-code/share/microsphere-spring/`
- 版本：`0.2.37-SNAPSHOT`（Maven Central 已发布），Spring 6.0~7.0 兼容
- 模块：`microsphere-spring-context`（161 文件，核心）+ 7 个辅助模块（web/webflux/webmvc/test/jdbc/guice）
- CI 测试矩阵：JDK 17/21/25
- 自动装配：通过 `spring.factories` + `@Enable*` 注解体系，无 Spring Boot AutoConfiguration.imports

---

## 一、并行 Bean 实例化

### REQ-001：启动时依赖分析 + 并发单例初始化

**问题**：Spring 容器启动时按顺序创建单例 bean——有依赖关系的串行执行。一个大型应用如果有 500 个独立的单例（互相不依赖），Spring 仍然串行初始化，启动时间线性增长。而 JDK 服务器 CPU 核心数已经 16~64 个，串行初始化大量浪费计算资源。

**产出**：
- `DefaultBeanDependencyResolver`：解析 bean 之间的依赖图（通过 `@Autowired`/`@Resource`/构造函数参数/`@Bean` 方法参数推断），使用 `ExecutorService` 并行加载 bean class
- `ParallelPreInstantiationSingletonsBeanFactoryListener`：拦截 `BeanFactory.preInstantiateSingletons()`，按无交集的依赖路径分组，每组内并行 `getBean()`——无依赖关系的 bean 在独立线程中同时初始化
- 开关：`microsphere.spring.pre-instantiation.singletons.threads` 控制并行线程数
- 依赖解析器 SPI：`InjectionPointDependencyResolver`——4 个实现分别处理 `@Autowired`/`@Resource`/构造器/`@Bean` 方法
- `DependencyTreeWalker`：去重依赖树遍历，合并重复子节点

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- 依赖图不正确时（如隐式依赖未通过标准注解声明），并发 `getBean()` 可能导致 bean 初始化异常
- `resolveInjectionDependentBeanNames()` 中注入点依赖解析**未完成**（`// TODO`，`DependencyAnalysisBeanFactoryListener` 196-199 行直接 return 空列表）

**配置规格**：
```
microsphere.spring.pre-instantiation.singletons.threads = <线程数>
```

---

## 二、PropertySource 增强

### REQ-002：通配符/排序/继承/自动刷新的资源属性源

**问题**：Spring `@PropertySource` 有 5 个核心限制：(1) 不能作为 meta-annotation 被复用，(2) 不能控制多个 PropertySource 的加载顺序，(3) 不支持 `@Inherited` 继承，(4) `value` 不支持通配符（Ant 风格），(5) `encoding` 属性没有默认值。生产环境中，配置往往分布在多个文件，需要按优先级加载，且外部配置变更后应该自动刷新。

**产出**：
- `@ResourcePropertySource`：meta-annotation，解决上述全部 5 个限制
  - `value`：支持 Ant 通配符（`classpath*:META-INF/config/*.properties`）
  - `first`/`before`/`after`：控制 PropertySource 顺序
  - `autoRefreshed=true`：文件变更时自动刷新（通过 `StandardFileWatchService` 监听）
  - `encoding`：默认 UTF-8
- `@YamlPropertySource` / `@JsonPropertySource`：YAML/JSON 配置源
- `@DefaultPropertiesPropertySource`：向 Spring Boot 默认属性追加配置
- `PropertySourceExtensionLoader`：构建 `CompositePropertySource`，每个匹配的资源生成一个 `ResourcePropertySource`
- `PropertySourcesChangedEvent`：属性源变更事件（`ADDED/REPLACED/REMOVED`）

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- 自动刷新仅对 `file://` 协议生效（Jar 内资源无法监听）
- `PropertySourceExtensionLoader.handleSourceName()` 用 `lastIndexOf("@")` 截断，名字格式 `{name}#{value}@{resource}`——要求名字中必须含 `@`

**配置规格**：通过 `@ResourcePropertySource` 注解属性声明，无需额外配置文件。

---

### REQ-003：配置属性使用追踪

**问题**：Spring 容器启动时加载大量配置属性，但运维需要知道"哪些配置属性被实际使用了"——用于审计、清理无用配置、排查配置值来源。Spring 本身没有配置属性使用的收集机制。

**产出**：
- `ConfigurationPropertyRepository`：收集运行时被读取的所有配置属性名
- `CollectingConfigurationPropertyListener`：配合 `ListenableConfigurableEnvironment`，拦截每次 `getProperty()` 调用，记录被访问的属性名
- `max-size`：默认 99999，超出后停止收集

**状态**：[已实现] 📝 **待编写原理文档**

**配置规格**：
```
microsphere.spring.configuration-property-repository.max-size = 99999
```

---

## 三、TTL 缓存

### REQ-004：基于注解的 TTL 缓存

**问题**：Spring Cache 提供 `@Cacheable` 但**不支持每条缓存数据的 TTL（Time To Live）**。所有缓存条目共享同一个过期策略——无法让产品信息缓存 10 分钟、用户会话缓存 30 分钟。Spring Data Redis 2.x 的支持方案在 3.x 已废弃。

**产出**：
- `@EnableTTLCaching`：`@EnableCaching` 的 meta 注解扩展，一行启用
- `@TTLCacheable` / `@TTLCachePut`：带 `expire` + `timeUnit` 属性的 `@Cacheable`/`@CachePut` 变体
- `TTLCacheResolver`：解析注解中的 TTL 值 → 存入 `TTLContext` ThreadLocal → 调用原始 `CacheManager`
- `TTLContext`：`ThreadLocal<Duration>`，`doWithTTL()` 函数式执行，`finally` 确保 `clearTTL()`

**状态**：[已实现，不完整] 📝 **待编写原理文档**

- **❌ Redis TTL 注入逻辑已完全失效**：`TTLRedisCacheWriterWrapper` 和 `TTLRedisConfiguration` 整个文件被 `//` 注释掉（Spring Data Redis 3.x CacheWriter API 变更导致不兼容）——TTL 值只能通过 ThreadLocal 传递到 CacheManager 层面，实际写入 Redis 时无法注入 TTL
- 缓存过期时间存储在 ThreadLocal 中，线程池复用场景下 TTL 可能泄漏到其他任务

**配置规格**：通过 `@EnableTTLCaching` 注解启用，无需额外配置。

---

## 四、Bean 生命周期事件

### REQ-005：Bean 全生命周期事件发布

**问题**：Spring 容器初始化 bean 时会回调 `BeanPostProcessor`，但**不会发布任何 ApplicationEvent**。你想在某个 bean 初始化完成后做点什么——只能写 `ApplicationListener<ContextRefreshedEvent>`（太晚）或自定义 BPP（太底层）。生产环境需要知道"SQL 连接池什么时候初始化完成"、"Redis 连接什么时候就绪"——这些对应到 12 个 Bean 生命周期节点。

**产出**：
- `BeanListener` 接口：12 个回调方法——`beforeInitialize`/`afterInitialize`、`beforeDestroy`/`afterDestroy`、`onAutoWire`、`onSetBeanName` 等
- `BeanFactoryListener` 接口：`beanDefinitionRegistered`/`beanDefinitionRemoved`
- `EventPublishingBeanBeforeProcessor`：`BeanDefinitionRegistryPostProcessor` + `InstantiationStrategy` 替换 + `DestructionAwareBeanPostProcessor`——在每个生命周期节点发布对应事件
- `EventPublishingBeanAfterProcessor`：反射替换 `DisposableBeanAdapter` 为装饰器，在 destroy 后回调
- 开关：`microsphere.spring.event-publishing-bean.enabled=true`（默认 **false**，因为 `prepareBeanDefinitions()` 会删除并重建全部 BeanDefinition——高风险 hack）

**状态**：[已实现，有高风险设计] 📝 **待编写原理文档**

- **❌ prepareBeanDefinitions() 重注册全部 bean definition**：删除所有 BeanDefinition 再重新注册——这会改变其他 BeanPostProcessor 的注册顺序，是一个"整个容器的观察顺序都会变"的操作
- 默认禁用——需要显式开 `enabled=true` 才生效

**配置规格**：
```
microsphere.spring.event-publishing-bean.enabled = false  # 默认关闭
```

---

## 五、Environment 可监听化

### REQ-006：拦截 Environment 属性解析和 Profile 激活

**问题**：Spring `Environment` 的 `getProperty()` 是静默的——你无法知道哪个配置属性在什么时候被谁取值。排查"这个值到底从哪里来的？"只能靠猜。生产环境需要一个"审计层"——每次属性解析和 Profile 激活都触发回调。

**产出**：
- `ListenableConfigurableEnvironment`：装饰 `ConfigurableEnvironment`——所有属性读写方法前后触发 `EnvironmentListener`/`PropertyResolverListener`/`ProfileListener` 回调
- `EnvironmentListener`：`onAddActiveProfile`/`onSetActiveProfiles`/`onGetProperty`
- `PropertyResolverListener`：`resolveRequiredPlaceholders`/`resolvePlaceholders`
- 开关：`microsphere.spring.listenable-environment.enabled=true`（默认 false）
- `ListenableAutowireCandidateResolver`：装饰 `AutowireCandidateResolver`，`getSuggestedValue()`（`@Value` 注解解析）后触发回调——追踪谁用了哪个 `@Value`

**状态**：[已实现] 📝 **待编写原理文档**

- **setEscapeCharacter() 版本兼容逻辑有 bug**：先通过 MethodHandle 反射调成功，紧接着抛 `UnsupportedOperationException`——Spring 6.2 之前必然抛异常

**配置规格**：
```
microsphere.spring.listenable-environment.enabled = false
microsphere.spring.listenable-autowire-candidate-resolver.enabled = false
```

---

## 六、自定义注解注入

### REQ-007：用任意自定义注解实现 @Autowired 等价注入

**问题**：Spring 只支持 `@Autowired`/`@Resource`/`@Inject` 三种注入注解。如果你的团队有自定义注解（如 `@ServiceDependency`、`@TenantId`），无法直接用注解标记注入点——必须借助额外的 BPP 或 AOP。每个团队都在重复实现"自定义注解 → 自动注入"的模式。

**产出**：
- `AnnotatedInjectionBeanPostProcessor`（949 行，context 模块最复杂类）：通用模板——指定一个注解类型，所有标记了该注解的字段/方法/构造器都会被自动注入
- 支持构造器注入、字段注入、方法注入、`ShortcutDependencyDescriptor` 缓存
- 顺序：`LOWEST_PRECEDENCE - 3`（显式声明"比 `AutowiredAnnotationBeanPostProcessor` 高 3 级优先级"）
- 所有 microsphere 生态的组件都可以通过这个模式实现自定义注入

**状态**：[已实现] 📝 **待编写原理文档**

**配置规格**：通过子类继承 + Spring bean 注册启用，无需外部配置。

---

## 七、配置 Bean 绑定

### REQ-008：注解驱动的配置属性到 Bean 绑定

**问题**：Spring Boot 有 `@ConfigurationProperties`，但纯 Spring Framework 没有等价的注解——需要手动从 Environment 取值并 set 到对象。microsphere 生态需要一套不依赖 Spring Boot 的配置绑定机制。

**产出**：
- `@EnableConfigurationBeanBinding` / `@EnableConfigurationBeanBindings`：声明式绑定——`prefix` 指定属性前缀，`type` 指定目标 Bean 类
- `ConfigurationBeanBindingPostProcessor`：`postProcessBeforeInitialization` 时识别 source 为 `@EnableConfigurationBeanBinding` 的 bean，自动从 PropertySources 取值并绑定
- `DefaultConfigurationBeanBinder`：通过 `DataBinder` 把 Properties 值转换为目标 Bean 字段
- `ConfigurationBeanAliasGenerator` 系列：自动生成配置 bean 的别名（`appConfigDataSource` / `user-config-xxx`）
- `ConfigurationBeanCustomizer`：绑定前后自定义

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P2 bug：DefaultConfigurationBeanBinder.bind() 参数交叉**——`dataBinder.setIgnoreInvalidFields(ignoreUnknownFields)` 和 `dataBinder.setIgnoreUnknownFields(ignoreInvalidFields)` 两个参数传反了

**配置规格**：通过 `@EnableConfigurationBeanBinding(prefix="app.datasource", type=DataSourceConfig.class)` 声明。

---

## 八、事件拦截链

### REQ-009：Spring ApplicationEvent 发布与监听拦截

**问题**：Spring 的 `ApplicationEventMulticaster` 发布事件后，没有标准方式在"事件到达监听器之前"做拦截——不能做事件的统一校验、日志、权限检查。生产环境需要"所有事件发布时自动记录来源和时间"、"某些敏感事件拒绝特定消费者"。

**产出**：
- `InterceptingApplicationEventMulticaster`：代理原有 `ApplicationEventMulticaster`——`multicastEvent()` 时先过 `ApplicationEventInterceptor` 链，`invokeListener()` 时过 `ApplicationListenerInterceptor` 链
- `ApplicationEventInterceptor(Chain)` / `ApplicationListenerInterceptor(Chain)`：拦截器接口 + 链式组合
- Proxy 模式：原 multicaster bean 改名 `applicationEventMulticaster_ORIGINAL`（可配置 `microsphere.spring.application-event-multicaster.reset-bean-name`）
- 拦截器来源：从 `BeanSource`（BeanFactory + spring.factories + ServiceLoader）三源收集

**状态**：[已实现] 📝 **待编写原理文档**

**配置规格**：`@EnableEventExtension` 注解启用。

---

## 九、Web 端点注册表

### REQ-010：启动时收集全部 Web 端点元数据

**问题**：一个 Spring Boot 应用有几十上百个 HTTP 端点——Controller 映射、Servlet 注册、Filter 链——但运行时没有一个地方可以**列出所有端点及其参数/请求方法/Content-Type**。运维需要这个信息做 API 文档、路由审计、安全扫描。Spring Boot Actuator 的 `/mappings` 只输出文本，不能程序化访问。

**产出**：
- `WebEndpointMapping`：端点元数据模型——`kind`（Servlet/Filter/Controller）、`patterns`、`methods`、`params`、`headers`、`consumes`、`produces`、`source`、`endpoint`
- `WebEndpointMappingResolver` SPI：从不同 Web 环境（MVC/WebFlux/Servlet 3）解析端点
- `@EnableWebExtension` / `@EnableWebMvcExtension` / `@EnableWebFluxExtension`：一行注解收集端点
- `WebEndpointMappingRegistrar`：启动时自动解析并注册全部端点
- `WebEndpointMappingRegistry`：端点注册表（Simple/Composite/Filtering）

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P1 bug：WebEndpointMappingRegistrar 重复 resolve**——`mappings=resolver.resolve(context)` 后，addAll 的却是**再次调用** `resolver.resolve(context)`，局部变量白赋值、resolver 被调两次（有状态 resolver 会丢数据）
- **P2 bug：WebEndpointMapping.id 跨 kind 哈希碰撞**——仅由 endpoint 哈希决定，Filter 和 Servlet 同名（String 相同）时覆盖
- **P2 bug：equals() 忽略 endpoint/source**——只比 kind+patterns+methods，不同 handler 相同配置视为相同

**配置规格**：`@EnableWebMvcExtension(registerWebEndpointMappings=true)`。

---

## 十、Handler 方法拦截

### REQ-011：AOP 风格的 Controller 方法拦截

**问题**：拦截 Spring MVC 的 Controller 方法通常用 `HandlerInterceptor`——但它只能拦截"请求到达"和"请求结束"，看不到方法参数、看不到返回值。要在"方法执行前"和"方法返回后"做 AOP 风格的操作，只能用 `@Aspect` 或自定义 `RequestBodyAdvice`/`ResponseBodyAdvice`——各自有不同的限制和兼容性问题。

**产出**：
- `HandlerMethodInterceptor`：`beforeExecute(HandlerMethod, args)` / `afterExecute(HandlerMethod, args, result, error)` ——AOP 风格的 around 钩子
- `HandlerMethodArgumentInterceptor`：`beforeResolveArgument` / `afterResolveArgument` ——参数级拦截
- `@EnableWebMvcExtension(interceptHandlerMethods=true)`：一行启用
- MVC 侧：`InterceptingHandlerMethodProcessor` 同时实现 `HandlerInterceptor` + `HandlerMethodArgumentResolver` + `HandlerMethodReturnValueHandler`，原地替换 `RequestMappingHandlerAdapter` 的 resolver 列表
- WebFlux 侧：`InterceptingHandlerMethodProcessor` 同时实现 `WebFilter` + `HandlerAdapter` + `ArgumentResolver` + `ResultHandler`
- `StoringRequestBodyArgumentInterceptor` / `StoringResponseBodyReturnValueInterceptor`：内置实现——存储请求体/响应体供后续使用

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- 实现用反射替换 Spring 内部私有字段（`argumentResolvers`/`returnValueHandlers`），跨 Spring 版本脆弱
- MVC 与 WebFlux 的实现是大段同构代码复制，接口方法签名不同

**配置规格**：`@EnableWebMvcExtension(interceptHandlerMethods=true, storeRequestBody=true)`。

---

## 十一、P6Spy JDBC 监控

### REQ-012：零侵入 SQL 语句追踪

**问题**：想知道应用发送了什么 SQL 语句、每条耗时多少——不依赖 APM 工具、不改任何应用代码。配置 DataSource 时加一行配置即可。

**产出**：
- `@EnableP6DataSource`：一行注解包裹所有 DataSource bean 为 P6Spy DataSource
- `P6DataSourceBeanPostProcessor`：`postProcessAfterInitialization` 时用 `P6DataSource` 包裹每个 DataSource
- `CompoundJdbcEventListenerFactory`：收集容器中所有 `JdbcEventListener` bean，合成为 Compound 监听器
- `PropertySourcesP6LoadableOptionsAdapter`：P6Spy 选项从 Spring Environment 读取（前缀 `microsphere.jdbc.p6spy.options`）
- `spring:env:property-sources://` URL 适配：P6Spy 内部配置加载 URL 适配到 Spring Environment

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- 排除列表：`microsphere.jdbc.p6spy.excluded-datasource-beans` Set 绑定
- `unwrap` 失败仅 trace 日志，静默保留原 bean

**配置规格**：`@EnableP6DataSource` 注解。

---

## 十二、自定义 spring: URL 协议

### REQ-013：Spring Resource/Environment 的 URL 化访问

**问题**：Spring 有 `Resource` 和 `Environment` 两个核心抽象，但外部工具（P6Spy、JDK `URL.openConnection()`）只认识 URL。你无法用一个 URL 表示"从 Spring Environment 取 `server.port` 属性"——需要单独写 Spring 代码。

**产出**：
- `spring:` URL 协议：`spring:{sub-protocol}:{ext}:...://...`
- `spring:resource:classpath://config.properties`：访问 Spring Resource
- `spring:env:property-sources://server/`：访问 Spring Environment 属性
- `spring:env:profiles://active`：访问当前激活的 Profile 列表
- `spring:delegating-bean:xxx://...`：委托给容器中注册的 `SubProtocolURLConnectionFactory` bean
- 内置 3 个工厂：`SpringResourceURLConnectionFactory`、`SpringEnvironmentURLConnectionFactory`、`SpringDelegatingBeanProtocolURLConnectionFactory`

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- `SpringEnvironmentURLConnectionFactory.getJavaType()` 仅支持 `text/properties/map`，其他类型留 `// TODO`

**配置规格**：无配置项，协议通过 `SpringProtocolURLStreamHandler` 自动注册。

---

## 十三、注解属性占位符解析与配置覆盖

### REQ-014：任意注解属性支持 @Value 占位符

**问题**：Spring 的 `@Value("${server.port}")` 注解放置在字段/参数上——但它**不能用在自定义注解的属性上**。如果你定义了 `@Retry(maxAttempts=3)`，无法写成 `@Retry(maxAttempts="${retry.max:3}")`。microsphere-spring 让所有自定义注解都支持占位符解析。

**产出**：
- `ResolvablePlaceholderAnnotationAttributes`：自动解析注解属性中的 `${...}` 占位符（通过 `Environment.resolvePlaceholders()`）
- 用于所有 `@ResourcePropertySource`、`@EnableConfigurationBeanBinding`、`@EnableTTLCaching` 等注解

**状态**：[已实现] 📝 **待编写原理文档**

---

### REQ-015：通过配置属性覆盖任意注解属性值

**问题**：注解属性是编译时固定的——`@Retry(maxAttempts=3)` 无法在运行时通过配置文件改成 5。需要一种运行时配置覆盖注解默认值的机制。

**产出**：
- `@OverrideAnnotationAttributes`：声明一个注解属性可以被配置覆盖
- `ConfigurationPropertyOverrideAnnotationAttributesStrategy`：从 `microsphere.spring.{AnnotationSimpleName}.{propertyName}` 属性前缀读取覆盖值
- `ConversionService` 转换：String 来源的配置值经 Spring `ConversionService` 转为目标类型

**状态**：[已实现] 📝 **待编写原理文档**

**配置规格**：
```
microsphere.spring.Retry.maxAttempts = 5
```

---

## 十四、Guice 集成

### REQ-016：Google Guice @Inject 到 Spring 的注入桥接

**问题**：一些老项目或特定模块使用了 Google Guice 的 `@Inject` 注解。如果迁移到 Spring，需要把 `@Inject` 的注入点也纳入 Spring 的 DI 管理——不能要求开发者把所有 `@Inject` 改成 `@Autowired`。

**产出**：
- `@EnableGuice`：一行注解启用 Guice→Spring 桥接
- `GuiceInjectAnnotationBeanPostProcessor`：拦截标有 `@Inject` 的字段/方法，从 Spring 容器获取 bean 注入

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P1 bug：裸 @Inject 被当可选注入**——`determineRequiredStatus` 在无 `optional` 属性时返回 false。Guice `@Inject` 默认语义是**必须注入**（`optional=false`），这里把最常见用法当成了可选注入——依赖缺失不报错，静默 null

**配置规格**：`@EnableGuice`。

---

## 十五、测试基础设施

### REQ-017：Spring Boot 测试增强工具

**问题**：写 Spring Boot 集成测试需要嵌入式数据库、内嵌 Tomcat、内嵌 ZooKeeper、WebTestClient——每种都需要样板配置。需要一个开箱即用的测试增强集合。

**产出**（`microsphere-spring-test`，35 文件）：
- `@EnableEmbeddedDatabase`：一行注解启用内嵌 SQLite/H2
- `EmbeddedTomcatContextLoader`：启动内嵌 Tomcat 运行测试（含 `Bootstrapper` 动态创建 `SpringApplication`）
- `EmbeddedZookeeperServer`：内嵌 ZooKeeper（`Curator TestingServer`）
- `AbstractWebFluxTest` / `AbstractWebMvcTest`：测试基类
- `SpringTestUtils.testInSpringContainer`：创建临时 ApplicationContext 执行回调

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **P2 bug：EmbeddedTomcatContextLoader 默认 contextPath=""**——Tomcat `addWebapp("", docBase)` 需要非空路径，常用 `"/"`，空串可能抛异常
- **P2 bug：registerResolvableDependency** 在 `customizeContext` 阶段取 parent（此时还未 setParent）→ 注册 null
- Tomcat 仅通过 JVM shutdown hook 停止，close 不停止；多测试类用同端口 8080 冲突
- `TestFilter.doFilter` 空实现——不调 `chain.doFilter()`，若真实接入会吞掉请求

**配置规格**：通过注解声明，Embedded Tomcat 在 `server.port` 属性配置。

---

## 十六、microsphere Converter → Spring ConversionService 桥接

### REQ-018：microsphere-java 类型转换器自动注册到 Spring

**问题**：microsphere-java（02）有 30+ 个 Converter（`String→Duration`、`String→DataSize` 等），但 Spring 的 `ConversionService` 完全不知道它们。你用 `@Value("${timeout}")` 注入 Duration 时走的仍是 Spring 自己的 `StringToDurationConverter`（只认 ISO-8601）。两套转换器体系互不兼容——你需要手动把 microsphere Converter 包装成 Spring `GenericConverter` 再注册。

**产出**（`core/convert/`，5 文件）：
- `@EnableSpringConverterAdapter`：一行注解，自动把所有 microsphere-java `Converter` 注册到 Spring `ConversionService`
- `EnableSpringConverterAdapterRegistrar`：`ImportBeanDefinitionRegistrar`——在 `BeanFactory` 初始化后注册 `SpringConverterAdapter` bean
- `SpringConverterAdapter`：microsphere `Converters.loadServicesList()` → 包装为 Spring `ConditionalGenericConverter` → 批量注册进 `ConversionService`
- `ConversionServiceResolver`：按优先级多源解析 `ConversionService`（BeanFactory→Environment→默认创建 `DefaultFormattingConversionService` 并注册为 bean）

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- `SpringConverterAdapter` 静态加载 `ServiceLoader`（`loadServicesList`），新增 Converter 服务文件需重启才生效

**配置规格**：`@EnableSpringConverterAdapter`。

---

## 十七、待实现需求（bug 修复）

### REQ-D01：DefaultConfigurationBeanBinder 参数交叉修复

**方案**：`dataBinder.setIgnoreInvalidFields(ignoreUnknownFields)` ↔ `dataBinder.setIgnoreUnknownFields(ignoreInvalidFields)` 互换。

**状态**：[待修复] — 两个参数传反，绑定时行为与预期相反。

---

### REQ-D02：TTL Redis 注入逻辑恢复

**方案**：适配 Spring Data Redis 3.x 的 `RedisCacheWriter` API，取消注释 `TTLRedisCacheWriterWrapper` 和 `TTLRedisConfiguration`。

**状态**：[待修复] — 当前 TTL 值无法注入 Redis 写入路径。

---

### REQ-D03：Guice @Inject 语义修复

**方案**：`determineRequiredStatus` 无 `optional` 属性时返回 true（必须注入）。

**状态**：[待修复] — 裸 `@Inject` 当前被当可选注入，缺依赖静默 null。

---

### REQ-D04：WebEndpointMappingRegistrar 双重 resolve 修复

**方案**：删除重复的 `resolver.resolve(context)` 调用，使用局部变量 `mappings`。

**状态**：[待修复] — resolver 被调用两次，有状态 resolver 数据可能不一致。

---

### REQ-D05：CompositeWebFilter 请求重复执行修复（WebFlux）

**方案**：将 `for` 循环改为链式代理——每个 filter 的 chain 是包含后续 filter 的组合 chain，而非同一个 chain 被重复传入。

**状态**：[待修复] — 当前 N>1 时每个 filter 独立跑一遍完整下游，请求被重复处理 N 次。

---

### REQ-D06：DependencyAnalysisBeanFactoryListener TODO 补全

**方案**：实现 `resolveInjectionDependentBeanNames()`，复用 `DefaultBeanDependencyResolver` 的注入点解析逻辑。

**状态**：[待修复] — 当前直接 return 空列表，注入点层面依赖分析未完成。

---

### REQ-D07：EmbeddedTomcatContextLoader contextPath 默认值修正

**方案**：`contextPath=""` → `contextPath="/"`。

**状态**：[待修复] — Tomcat 不允许空 contextPath。

---

### REQ-D08：EventPublishingBeanBeforeProcessor.prepareBeanDefinitions 重构

**方案**：不删除全部 BeanDefinition 再重建——改为只追加缺失的 BeanDefinition 到首位，不改变已有 BeanDefinition 的注册顺序。

**状态**：[待改进] — 当前删除全量再重建会影响其他 BPP 的观察顺序。

---

### REQ-D09：WebRequestProducesRule.matches 逻辑反转修复

**方案**：`if (isNotEmpty(result)) { return false; }` → `return isNotEmpty(result)`——与 `WebRequestConsumesRule.matches()` 和 Spring `ProducesRequestCondition` 保持一致语义。同步修复测试 `FilterOperatorTest.testXOR`（测试已固化错误行为，与 02-REQ-D01 同模式）。

**状态**：[待修复] — 当前有匹配 produces 表达式时返回 false，语义完全反转。17 个文件的 `WebRequestRule` 框架独立可复用，REQ-010 间接触及但未显式列出。

---

## 十八、发散需求（生产环境需要的全新能力）

### REQ-N01：Bean 初始化时间统计仪表盘

**生产痛点**：应用启动后只能看到总时间，无法知道"哪个 bean 初始化最慢"。Spring Boot Actuator 的 `/startup` 需要 `BufferingApplicationStartup` 且不区分单个 bean 的耗时明细。

**产出**：`BeanTimeStatistics.report()` —— 利用已有的 `BeanTimeStatistics`（已有 StopWatch 记录），在 `ContextRefreshedEvent` 时输出 Top-N 最慢的 bean 初始化耗时列表，格式化输出或暴露 JMX。

**状态**：[待实现]

---

### REQ-N02：ConditionalOnMissingBean 的增强属性源条件

**生产痛点**：Spring 的 `@ConditionalOnProperty` 只是"有这个属性就加载"。生产环境需要"这个属性值等于 X 且 bean Y 不存在时加载"——组合条件。

**产出**：`@ConditionalOnConfigurationProperty(prefix="app.datasource", havingValue="mysql", matchIfMissing=true, conditionalOnMissingBean=DataSource.class)` —— 组合属性值匹配 + bean 存在性检查。

**状态**：[待实现]

---

### REQ-N03：配置属性变更事件与审计日志

**生产痛点**：配置文件被改了——谁改的？什么时候改的？改之前是什么值？运维需要审计追踪。

**产出**：`PropertySourceChangeAuditor` —— 基于已有的 `PropertySourcesChangedEvent` + `ConfigurationPropertyRepository`，在属性变更时记录 old→new 值到审计日志，暴露 JMX/HTTP endpoint 查询变更历史。

**状态**：[待实现]

---

### REQ-N04：RPM 式灰度配置发布

**生产痛点**：生产环境要逐步切换配置（先 10% 的 Pod 用新配置，观察 5 分钟，没问题再全量推）。Spring Cloud Config 不支持灰度。

**产出**：`ProgressivePropertySource` —— 基于 `@ResourcePropertySource(autoRefreshed=true)` 的刷新能力，叠加 `rollout-percentage` 属性控制灰度比例。`PropertySource.refresh()` 前检查滚动策略。

**状态**：[待实现]

---

### REQ-N05：ReversedProxyHandlerMapping 的统一实现

**生产痛点**：MVC 和 WebFlux 各有一份 `ReversedProxyHandlerMapping` 实现——同构代码，行为细节不一致。内部微服务间有 `microsphere_wem_id` 头可直接跳过路由匹配精准定位 handler，但两个版本的实现方式完全不同。

**产出**：抽取公用的 handler 查找逻辑到 `microsphere-spring-web` 共享层，MVC/WebFlux 各自只提供环境相关的 `getHandlerInternal`。

**状态**：[待实现]

---

### REQ-N06：配置属性类型安全绑定增强

**生产痛点**：`DefaultConfigurationBeanBinder` 用 `DataBinder` 绑定，类型校验能力有限——`Duration`、`DataSize`、Enum、List<自定义类型> 等复杂类型可能绑定失败且无明确错误消息。

**产出**：集成 microsphere-java 的 `Converters` 体系——`ConfigurationBeanBinder` 优先使用 `Converters.convert()` 进行类型转换，失败时给出明确的"期望类型 X，实际值 Y，转换错误 Z"消息。

**状态**：[待实现]

---

### REQ-N07：运行时管理统一出口（JMX/HTTP 端点）

**生产痛点**：运维想查"TTL 缓存命中率、PropertySource 加载了哪些、哪几个 bean 初始化最慢、配置属性被谁读取过"——项目已有 `BeanTimeStatistics` / `ConfigurationPropertyRepository` / `WebEndpointMappingRegistry` / `CacheManager` 等数据源，但没有任何统一的查询入口。需要 SSH 进容器看日志或 JMX 手动查询，无法集成到监控面板。

**产出**：`ManagementEndpoint` —— 基于项目现有的数据收集体系，暴露一个统一的 HTTP/JMX 管理端点。提供 `/management/beans/top-slow`（最慢 bean）、`/management/config/property-sources`（属性源列表）、`/management/config/used-properties`（已被读过的属性）、`/management/cache/stats`（缓存统计）。纯 Spring 项目（不依赖 Boot Actuator）可用。

**生态定位**：Spring Boot Actuator 已有类似能力，但 **纯 Spring Framework 应用没有 Actuator**——microsphere-spring 填补这个缺口。Actuator 不覆盖的数据（TTL 缓存统计、PropertySource 明细）由本项目补充。

**状态**：[待实现]

---

### REQ-N08：配置变更 → Bean 刷新传播链

**生产痛点**：`@ResourcePropertySource(autoRefreshed=true)` 已能检测文件变化并刷新 `PropertySource`——但依赖这些属性的 `@Value` 字段和 `@ConfigurationProperties` bean 不会自动更新。改完配置文件还得重启应用，autoRefreshed 只做了一半。

**产出**：`PropertySourceChangePropagator` —— 监听 `PropertySourcesChangedEvent`（已存在）→ 识别受影响的 bean（通过 `ConfigurationPropertyRepository` 记录的使用追踪）→ 对标记了 `@Refreshable` 的 bean 执行重新绑定（复用 `ConfigurationBeanBinder` 逻辑）。`@ConfigurationProperties` bean 的 Spring Boot 等价方案是 `@RefreshScope`（仅 Cloud），microsphere 在纯 Spring 场景实现同等能力。

**生态定位**：Spring Cloud `@RefreshScope` 需要 Cloud Context + `/actuator/refresh` 端点——纯 Spring 应用没有。本项基于已有的 PropertySourcesChangedEvent + ConfigurationPropertyRepository + ConfigurationBeanBinder 三个现有组件，衔接自然。

**状态**：[待实现]

---

> **注：以下方向经核实不属于 03 的职责范围**
> - **N-分布式锁/@Scheduled 协调**：已有 ShedLock/Spring Integration/Quartz 覆盖，且 microsphere 可作为"@Enable 式一行启用"的轻量实现路径，但不作为 03 独立需求
> - **Spring Boot AutoConfiguration 生成**：实际应由 **04-microsphere-spring-boot** 模块承担——03 以 `@Enable*` 注解驱动，04 将这些注解转换为 Boot `AutoConfiguration.imports`，是跨模块延伸而非 03 内部实现
> - **@Scheduled 增强**：并入 04（Spring Boot 部署环境），03 不涉及调度支持

---

## 十九、跨项目一致性校验

> 以下模式在 01/02/03 中持续出现——确认是作者共性代码模式。

| 模式 | 01 表现 | 02 表现 | 03 表现 |
|------|---------|---------|---------|
| SPI + Prioritized 自选择 | 无 | ServiceLoaderUtils | spring.factories 加载优先 |
| 反射读取 Spring/JDK 内部字段 | ClassLoader.classes | URLClassPath | DefaultListableBeanFactory.resolvableDependencies |
| WebFlux/MVC 同构代码复制 | — | — | ConsumingWebEndpointMappingAdapter × 2 |
| 单个 Bean 多接口角色 | — | — | InterceptingHandlerMethodProcessor (WebFilter + HandlerAdapter + Resolver + Handler) |
| 注释掉的死代码 | — | UnsafeUtils 全文件注释 | TTLRedisCacheWriterWrapper 全文件注释 |
| TODO 未完成 | — | — | DependencyAnalysisBeanFactoryListener 直接 return 空列表 |
| WebFlux/MVC 同构代码复制 #2 | — | — | ReversedProxyHandlerMapping (MVC MethodHandle vs Flux endpoint) |
| 测试固化错误行为 | — | FilterOperatorTest.testXOR | ProducesRule 匹配逻辑反转已固化 |

---

## 二十、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 0.2.37-SNAPSHOT | 2024~2025 | 持续开发（Maven Central 已发布），Spring 6~7 兼容，Java 17+ |
| — | 2026-08-02 | REQ 文档编写（第一版） |
