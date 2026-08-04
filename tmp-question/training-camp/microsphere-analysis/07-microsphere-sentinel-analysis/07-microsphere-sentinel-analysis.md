# Microsphere-Sentinel 深度分析

> **核心命题**：Microsphere 的真正价值不是 middleware 集成，而是定义在底层模块的一组通用扩展点。
> **本文回答**：5 个扩展点定义在 5 个底层模块，Sentinel 和 Resilience4j 都是它们的消费者。同一个扩展点可以被不同模块复用于不同目的--限流、熔断、事件发布、日志审计。

---

## 背景：微服务限流的实现路径

### Sentinel 是什么

Alibaba Sentinel 是一个限流熔断库。核心 API 是 `SphU.entry("resourceName")` / `SphU.exit()`--在业务代码前后包裹这两个调用，Sentinel 就能统计 QPS、判断是否触发流控规则、在被限流时抛出 `BlockException`。

### 官方 Spring Cloud Alibaba Sentinel 的方案

官方集成通过 `@SentinelResource` 注解实现：

```java
@SentinelResource(value = "getUserById", fallback = "getUserByIdFallback")
public User getUserById(Long id) {
    return userMapper.selectById(id);
}
```

官方支持的适配器：

| 中间件 | 官方支持 | 机制 |
|--------|---------|------|
| Spring MVC `@Controller` | ✓ | `SentinelWebInterceptor` 拦截 `HandlerExecutionChain` |
| OpenFeign 接口 | ✓ | `SentinelFeign` 代理 Feign 调用 |
| RestTemplate | ✓ | `@SentinelRestTemplate` 拦截 HTTP 调用 |
| Spring Cloud Gateway | ✓ | `SentinelGatewayFilter` |
| **MyBatis** | ✗ | 不支持 |
| **Redis 命令** | ✗ | 不支持 |
| **Druid 连接池** | ✗ | 不支持 |
| **Hibernate** | ✗ | 不支持 |
| **JDBC/P6Spy** | ✗ | 不支持 |

官方方案的核心特征：**opt-in（开发者主动标注）**。你想保护哪个方法，就在那个方法上加 `@SentinelResource`。忘记标注 = 不保护。

### Microsphere 的方案

不需要注解，通过拦截器**自动保护所有调用**。保护范围不是由"开发者标记了什么"决定，而是由**框架有拦截器的地方**决定。

但这不是 sentinel 模块自己写的拦截器--它复用了其他 microsphere 模块已经定义好的扩展点。下面逐个分析。

---

## 第一部分：SentinelTemplate--Sentinel API 的模板方法包装

### Sentinel 原生 API 的问题

Sentinel 的 API 是一个四步配对操作：

```java
ContextUtil.enter("contextName", "origin");     // 第 1 步：进入上下文
Entry entry = SphU.entry("resourceName", ...);  // 第 2 步：创建资源入口
try {
    // 业务代码
} catch (BlockException e) {
    // 被流控了（不算业务异常）
} finally {
    entry.exit();                                // 第 3 步：退出入口（必须！）
    ContextUtil.exit();                          // 第 4 步：退出上下文（必须！）
}
// 如果业务抛了非 BlockException 异常，还要调：
Tracer.traceEntry(failure, entry);               // 第 5 步：记录异常（让熔断规则统计）
```

问题：
- 忘记 `entry.exit()` -> 资源计数永不释放 -> 永久流控
- 忘记 `ContextUtil.exit()` -> 上下文泄漏 -> 后续请求拿到错误的上下文
- 忘记 `Tracer.traceEntry()` -> 异常不统计 -> 熔断规则不生效
- `BlockException` 是流控异常，不应该 trace；业务异常才应该 trace

### SentinelOperations 接口

microsphere 定义了一个 `SentinelOperations` 接口，把四步操作压缩为两种模式：

```java
public interface SentinelOperations {
    // 两阶段模式（适配分离的 pre/post 钩子）
    SentinelContext begin(String resourceName, String contextName, String origin);
    void end(SentinelContext context);

    // 一次性模式（适配单次回调）
    default void execute(String resourceName, Runnable callback) { ... }
    default <R> R execute(String resourceName, Function<SentinelContext, R> callback) { ... }
    default <R> R call(String resourceName, ThrowableFunction<SentinelContext, R> callback) throws Throwable { ... }
    default <R, TR extends Throwable> R call(String resourceName, ..., Class<TR> throwableClass) throws TR { ... }
}
```

`execute` 和 `call` 的内部实现都是 `begin -> callback -> end`：

```java
default <R> R execute(String resourceName, Function<SentinelContext, R> callback) {
    SentinelContext context = begin(resourceName, null, null);
    try {
        R result = callback.apply(context);
        context.setResult(result);
        return result;
    } catch (Throwable e) {
        if (!(e instanceof BlockException)) {
            context.setFailure(e);    // 业务异常标记为 failure
        }
        throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    } finally {
        end(context);                 // 保证执行
    }
}
```

关键细节：`BlockException` 不设为 `failure`--流控异常不是业务失败，不应该被熔断规则统计。只有非 `BlockException` 的业务异常才设为 `failure`。

### SentinelTemplate 实现

```java
public class SentinelTemplate implements SentinelOperations {
    private final int resourceType;      // 默认 COMMON
    private final EntryType trafficType;  // 默认 IN

    public SentinelContext begin(String resourceName, String contextName, String origin) {
        String actualContextName = isBlank(contextName) ? DEFAULT_CONTEXT_NAME : contextName;
        String actualOrigin = isBlank(origin) ? DEFAULT_ORIGIN : origin;
        ContextUtil.enter(actualContextName, actualOrigin);
        Entry entry = SphU.entry(resourceName, this.resourceType, this.trafficType);
        return new SentinelContext(resourceName, actualContextName, actualOrigin, entry);
    }

    public void end(SentinelContext context) {
        Entry entry = context.getEntry();
        Throwable failure = context.getFailure();
        if (failure != null) {
            Tracer.traceEntry(failure, entry);  // 只在有 failure 时 trace
        }
        entry.exit();
        ContextUtil.exit();
    }
}
```

`resourceType` 和 `trafficType` 在构造时设定，不可变。不同类型的资源用不同的 `SentinelTemplate` 实例：
- Web 接口：`resourceType = COMMON_WEB`
- 数据库操作：`resourceType = COMMON_DB_SQL`
- 通用资源：`resourceType = COMMON`

### SentinelContext--跨 begin/end 的状态传递

```java
public class SentinelContext {
    private final String resourceName;
    private final String contextName;
    private final String origin;
    private final Entry entry;
    private Object result;           // 业务结果
    private Throwable failure;       // 业务异常（非 BlockException）

    // 属性存储（拦截器之间的数据共享）
    private Map<String, Object> attributes;

    // ThreadLocal 机制
    private static final ThreadLocal<SentinelContext> contextHolder = new ThreadLocal<>();

    public SentinelContext withinContext() {
        contextHolder.set(this);     // 存入 ThreadLocal
        return this;
    }

    public static SentinelContext getContext() {
        return contextHolder.get();
    }

    public static <T> T doInContext(Function<SentinelContext, T> consumer, boolean removeAfter) {
        SentinelContext context = contextHolder.get();
        try {
            return consumer.apply(context);
        } finally {
            if (removeAfter) {
                contextHolder.remove();  // 用完即清
            }
        }
    }
}
```

`SentinelContext` 有两种传递方式：
1. **显式传递**：begin 返回 context，end 接收 context（如 Redis、Spring Web 适配器通过框架属性传递）
2. **ThreadLocal 传递**：begin 后调 `withinContext()` 存入 ThreadLocal，end 通过 `doInContext(_, true)` 取出（如 Druid、Hibernate、P6Spy 适配器）

两种方式的选择取决于中间件的拦截机制是否提供属性容器。

### 和其他 microsphere 模块的模板方法对比

| 模块 | 模板类 | 上下文类 | 解决的问题 |
|------|--------|---------|-----------|
| 03-spring | `BeanFactoryListener` | `BeanFactoryEvent` | Spring Bean 生命周期不可观察 |
| 04-spring-boot | `BindListener` | `ConfigurationPropertiesBindingEvent` | Binder 绑定过程不可观察 |
| **07-sentinel** | **`SentinelTemplate`** | **`SentinelContext`** | **SphU entry/exit 配对繁琐** |

每个模板类解决的都是同一个问题：**框架的 API 是过程的、分散的、容易遗漏的，包装成声明式的 begin/end 回调。**

---

## 第二部分：SentinelPlugin--插件 SPI 和 JMX 管理

### 问题

Sentinel 的资源定义是隐式的--你在代码里写 `SphU.entry("myResource")` 就创建了一个资源。但项目中到底有哪些资源被保护了？哪个 MyBatis Mapper 被限流了？哪个 Redis 命令被熔断了？没有任何清单。

### SentinelPlugin 接口

```java
public interface SentinelPlugin {
    boolean isAutoInstalled();          // 是否自动安装
    default boolean isEnabled() {       // 是否启用（读系统属性）
        return SentinelUtils.isPluginEnabled(getName());
    }
    void setEnabled(boolean enabled);   // 运行时启用/禁用
    String getName();                   // 唯一名称，如 "mybatis"、"redis"
    default String getContextName() {   // Sentinel Context 名称
        return SentinelUtils.getDefaultContextName(getName());
    }
    String getOrigin();                 // 调用来源
    int getResourceType();              // 资源类型
    EntryType getTrafficType();         // 流量类型（IN/OUT）

    static void install(SentinelPlugin plugin) {
        SentinelPluginRepository.INSTANCE.install(plugin);
        addShutdownHookCallback(() -> INSTANCE.uninstall(plugin));  // JVM 关闭时卸载
    }
}
```

每个 `SentinelPlugin` 实例 = 一条 Sentinel 资源的元数据声明。microsphere 定义了 6 个插件：

```
"mybatis"        -> 保护 MyBatis Mapper 的 update/query 调用
"redis"          -> 保护 Redis 的 SET/GET/DEL... 命令
"alibaba-druid"  -> 保护 Druid 连接池的 Statement 操作
"hibernate"      -> 保护 Hibernate 实体增删改查
"p6spy"          -> 保护 JDBC 语句执行
"spring-web"     -> 保护 Spring MVC/WebFlux 控制器方法
```

### AbstractSentinelPlugin--自动安装

```java
public abstract class AbstractSentinelPlugin implements SentinelPlugin {
    // 不可变元数据（构造时确定）
    private final String name;
    private final String contextName;
    private final String origin;
    private final int resourceType;
    private final EntryType trafficType;
    private final boolean autoInstalled;

    // 可变状态（运行时可改）
    private volatile boolean enabled;

    public AbstractSentinelPlugin(String name, ..., boolean autoInstalled) {
        this.name = name;
        // ...
        this.enabled = SentinelPlugin.super.isEnabled();  // 读系统属性
        if (autoInstalled) {
            SentinelPlugin.install(this);  // 构造时自动注册到全局仓库
        }
    }
}
```

关键设计：
- `name`、`contextName`、`resourceType` 等是 `final` -- 构造后不可变
- `enabled` 是 `volatile` -- 运行时可跨线程修改（如通过 JMX）
- `autoInstalled=true` 时构造即注册 -- Spring Bean 创建时自动触发

### SimpleSentinelPlugin--委托用的简单实现

```java
public class SimpleSentinelPlugin extends AbstractSentinelPlugin {
    // 5 个构造器，参数从少到多
    public SimpleSentinelPlugin(String name) { ... }
    public SimpleSentinelPlugin(String name, String contextName, String origin, ...) { ... }
}
```

当一个适配器类已经继承了其他类（如 `SentinelAlibabaDruidFilter extends AbstractStatementFilter`），不能再继承 `AbstractSentinelPlugin`，就用 `SimpleSentinelPlugin` 作为委托：

```java
public class SentinelAlibabaDruidFilter extends AbstractStatementFilter
        implements SentinelPlugin {
    private final SimpleSentinelPlugin delegate;  // 委托

    public SentinelAlibabaDruidFilter() {
        this.delegate = new SimpleSentinelPlugin(PLUGIN_NAME, ...);
    }

    // SentinelPlugin 方法全部委托
    public String getName() { return delegate.getName(); }
    public boolean isEnabled() { return delegate.isEnabled(); }
    // ...
}
```

### SentinelPluginRepository--插件仓库

```java
public interface SentinelPluginRepository {
    static SentinelPluginRepository INSTANCE = ServiceLoaderUtils.loadFirstService(...);

    boolean install(SentinelPlugin plugin);
    boolean isInstalled(String pluginName);
    SentinelPlugin get(String pluginName);
    Collection<SentinelPlugin> getAll();
    boolean uninstall(String pluginName);
    void clear();
}
```

`INSTANCE` 通过 Java SPI 加载。默认实现是 `JMXSentinelPluginRepository`。

### SimpleSentinelPluginRepository--内存实现

```java
public class SimpleSentinelPluginRepository implements SentinelPluginRepository {
    private final Map<String, SentinelPlugin> pluginsMap = newConcurrentHashMap();

    public boolean install(SentinelPlugin plugin) {
        String name = plugin.getName();
        boolean installed = pluginsMap.putIfAbsent(name, plugin) == null;
        if (!installed) {
            logger.warn("SentinelPlugin[name : '{}'] has been installed, ...", name);
        }
        return installed;
    }
}
```

`putIfAbsent` 保证幂等--同名插件只安装一次。

### JMXSentinelPluginRepository--JMX 管理实现（默认）

```java
public class JMXSentinelPluginRepository implements SentinelPluginRepository, Prioritized {
    private final SimpleSentinelPluginRepository delegate = new SimpleSentinelPluginRepository();

    public boolean install(SentinelPlugin plugin) {
        ObjectName objectName = format("io.microsphere.sentinel:type=SentinelPlugin,name={}", plugin.getName());
        return doInMBeanServer(() -> {
            if (mBeanServer.isRegistered(objectName)) return false;
            StandardMBean mBean = new StandardMBean(plugin, SentinelPlugin.class);
            mBeanServer.registerMBean(mBean, objectName);
            return delegate.install(plugin);
        });
    }

    public boolean uninstall(String pluginName) {
        return doInMBeanServer(() -> {
            mBeanServer.unregisterMBean(objectName);
            return delegate.uninstall(pluginName);
        });
    }
}
```

每个插件注册为一个 `StandardMBean`，暴露 `SentinelPlugin` 接口的所有方法。在 JConsole 中可以：
- 查看所有已安装的插件
- 运行时调用 `setEnabled(false)` 禁用某个插件
- 运行时调用 `setEnabled(true)` 重新启用

这是生产运维的关键能力--不需要重启应用就能动态调整限流保护范围。

### 两个级别的启用开关

| 开关 | 属性 | 范围 | 检查时机 |
|------|------|------|---------|
| 主开关 | `microsphere.sentinel.enabled` | 全局限流开/关 | Spring Boot 条件注解（启动时） |
| 插件开关 | `microsphere.sentinel.{pluginName}.enabled` | 单个插件的限流开/关 | 运行时 `isEnabled()` 调用 |

```bash
# 全局关闭限流
-Dmicrosphere.sentinel.enabled=false

# 只关闭 Redis 限流，其他保持
-Dmicrosphere.sentinel.redis.enabled=false
```

主开关通过 `@ConditionalOnSentinelEnabled` 注解控制 AutoConfiguration 是否加载。插件开关通过 `SentinelPlugin.isEnabled()` 在运行时检查。

---

## 第三部分：5 个扩展点--microsphere 的核心架构资产

microsphere 的每个底层模块定义了自己的拦截钩子（扩展点）。这些扩展点不是为 sentinel 设计的--它们是通用机制，任何模块都可以实现。sentinel 只是其中一个消费者。

### 扩展点 1：HandlerMethodInterceptor--控制器方法拦截

**定义在：** `microsphere-spring-web`（03 模块）

```java
public interface HandlerMethodInterceptor {
    default void beforeExecute(HandlerMethod handlerMethod, Object[] args,
                               NativeWebRequest request) throws Exception {}
    default void afterExecute(HandlerMethod handlerMethod, Object[] args,
                              @Nullable Object returnValue, @Nullable Throwable error,
                              NativeWebRequest request) throws Exception {}
}
```

**兄弟接口** `HandlerMethodArgumentInterceptor`：拦截方法参数解析。

**发现机制：** Spring `@Bean` + `@EnableWebMvcExtension` / `@EnableWebFluxExtension` 扫描 `BeanSource`

`BeanSource` 是一个枚举，定义了三种发现途径：

```java
public enum BeanSource {
    BEAN_FACTORY,           // 从 Spring BeanFactory 中按类型查找
    SPRING_FACTORIES,       // 从 META-INF/spring.factories 加载
    JAVA_SERVICE_PROVIDER   // 从 META-INF/services/ 通过 ServiceLoader 加载
}
```

`@EnableWebExtension` 的 `sources()` 属性默认包含全部三种。这意味着扩展点实现可以通过任意一种方式被发现。

**激活链路：**

```
Spring Boot 启动
  -> WebMvcAutoConfiguration（@ConditionalOnWebMvcAvailable）
    -> @EnableWebMvcExtension
      -> WebExtensionBeanDefinitionRegistrar
        -> registerHandlerMethodInterceptors()：从 BeanSource 发现所有实现
        -> 注册 DelegatingHandlerMethodAdvice Bean
          -> ContextRefreshedEvent 时收集所有 HandlerMethodInterceptor
          -> 每次控制器调用时依次执行 beforeExecute / afterExecute
```

**WebMVC 的消费路径：**

`DelegatingHandlerMethodAdvice` -> `InterceptingHandlerMethodProcessor`（实现了 Spring 的 `HandlerMethodArgumentResolver` + `HandlerMethodReturnValueHandler` + `HandlerInterceptor`）-> 在 `RequestMappingHandlerAdapter` 中被注册 -> 每次控制器方法调用前后触发 `beforeExecute` / `afterExecute`。

**WebFlux 的消费路径：** 类似，通过 `InterceptingHandlerMethodProcessor` 适配 Reactive 路径。

**所有已知实现：**

| 实现 | 模块 | 用途 |
|------|------|------|
| `WebEventPublisher` | 03-spring-web | 发布 Web 请求事件（如 `WebEndpointMappingsReadyEvent`） |
| `StoringResponseBodyReturnValueInterceptor` | 03-spring-webflux | 缓存响应体（测试用） |
| `SentinelHandlerMethodInterceptor` | 07-sentinel | 限流 |

**Sentinel 实现的关键细节：**

```java
public class SentinelHandlerMethodInterceptor extends AbstractSentinelPlugin
        implements HandlerMethodInterceptor, ApplicationListener<WebEndpointMappingsReadyEvent>, Ordered {

    // 资源名缓存（启动时预计算）
    private Map<HandlerMethod, String> entryCache;

    public void onApplicationEvent(WebEndpointMappingsReadyEvent event) {
        initEntryCache(event);  // 监听端点映射就绪事件，预计算资源名
    }

    private void initEntryCache(WebEndpointMappingsReadyEvent event) {
        // 遍历所有 WebEndpointMapping，提取 HandlerMethod
        // 资源名格式："{URL模式}#{HandlerMethod}"
        // 例如："GET /api/users#UserController.getUser(Long)"
        for (WebEndpointMapping mapping : event.getMappings()) {
            HandlerMethod hm = (HandlerMethod) mapping.getEndpoint();
            String resourceName = mapping.toExpression() + "#" + hm;
            entryCache.put(hm, resourceName);
        }
    }

    public void beforeExecute(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request) {
        String resourceName = getResourceName(handlerMethod);  // 从缓存取
        SentinelContext ctx = sentinelOperations.begin(resourceName, getContextName(), getOrigin());
        setSentinelContext(request, ctx);  // 存入 NativeWebRequest 属性
    }

    public void afterExecute(HandlerMethod handlerMethod, Object[] args, Object returnValue,
                             Throwable error, NativeWebRequest request) {
        SentinelContext ctx = getSentinelContext(request);  // 从属性取出
        ctx.setResult(returnValue);
        ctx.setFailure(error);
        sentinelOperations.end(ctx);
    }
}
```

资源名有两级策略：
1. 如果 `WebEndpointMappingsReadyEvent` 已触发 -> 用 `{URL模式}#{HandlerMethod}`（如 `GET /api/users#UserController.getUser(Long)`）
2. 如果事件还没触发（首个请求在事件之前到达）-> 降级为 `"spring-web:" + handlerMethod`

`SentinelContext` 存入 `NativeWebRequest` 属性（不是 ThreadLocal）--因为 WebFlux 是反应式的，线程可能在请求处理过程中切换，ThreadLocal 不可靠。

**Spring Boot 自动装配：**

```java
@ConditionalOnWebApplication(type = ANY)
@ConditionalOnSentinelAvailable
@ConditionalOnProperty(name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
@ConditionalOnClass(name = {
    "org.springframework.web.method.HandlerMethod",
    "io.microsphere.spring.web.method.support.HandlerMethodInterceptor",
    "io.microsphere.alibaba.sentinel.spring.web.SentinelHandlerMethodInterceptor"
})
public class SentinelSpringWebAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    public SentinelHandlerMethodInterceptor sentinelHandlerMethodInterceptor() {
        return new SentinelHandlerMethodInterceptor();
    }
}
```

三层条件：
1. `@ConditionalOnWebApplication(ANY)` -- 必须是 Web 应用
2. `@ConditionalOnSentinelAvailable` -- Sentinel 启用且 classpath 有 SphU
3. `@ConditionalOnClass` -- classpath 有 HandlerMethod、HandlerMethodInterceptor、SentinelHandlerMethodInterceptor

只有三层条件都满足才创建 Bean。Bean 创建时 `AbstractSentinelPlugin` 构造函数自动调 `install(this)` 注册到 `SentinelPluginRepository`。

---

### 扩展点 2：ExecutorFilter--MyBatis 执行器拦截

**定义在：** `microsphere-mybatis-core`（13 模块）

```java
public interface ExecutorFilter extends Prioritized {
    default int update(MappedStatement ms, Object parameter,
                       ExecutorFilterChain chain) throws SQLException {
        return chain.update(ms, parameter);  // 默认：直接传递
    }
    default <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds,
                              ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql,
                              ExecutorFilterChain chain) throws SQLException {
        return chain.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
    }
    // ... commit, rollback, close, createCacheKey, deferLoad, getTransaction 等
}
```

**ExecutorFilterChain--责任链模式：**

```java
public class ExecutorFilterChain {
    private final ExecutorFilter[] filters;
    private final Executor delegate;      // 真实 Executor
    private int position = 0;

    public <E> List<E> query(MappedStatement ms, ...) throws SQLException {
        if (position < filters.length) {
            return filters[position++].query(ms, ..., this);
        }
        return delegate.query(ms, ...);  // 所有 filter 执行完，调真实 Executor
    }
}
```

经典的 Chain of Responsibility：每个 Filter 决定是否继续传递（调 `chain.query(...)`）或中断。

**发现机制：** Spring `@Bean` + `@EnableMyBatisExtension` 扫描 `BeanSource`

```java
@Import(MyBatisExtensionBeanDefinitionRegistrar.class)
public @interface EnableMyBatisExtension {
    boolean interceptExecutor() default true;
    BeanSource[] sources() default {BEAN_FACTORY, SPRING_FACTORIES, JAVA_SERVICE_PROVIDER};
}
```

**激活链路：**

```
Spring Boot 启动
  -> MyBatisAutoConfiguration（@ConditionalOnMyBatisAvailable）
    -> @EnableMyBatisExtension
      -> MyBatisExtensionBeanDefinitionRegistrar
        -> 从 BeanSource 发现所有 ExecutorFilter
        -> 如果有 filter：注册 InterceptingExecutorInterceptor Bean
        -> 注册 SqlSessionFactoryBeanPostProcessor
          -> 把 InterceptingExecutorInterceptor 注入 MyBatis Configuration
            -> MyBatis 初始化时调用 plugin(Executor)
              -> InterceptingExecutorInterceptor.plugin() 把 Executor 替换为 InterceptingExecutor
                -> 每次 query/update 创建 ExecutorFilterChain，依次执行所有 filter
```

**关键桥接：** `InterceptingExecutorInterceptor` 是 MyBatis 的 `Interceptor`（MyBatis 原生 SPI），它的 `plugin()` 方法把原生 `Executor` 包装为 `InterceptingExecutor`。这是 microsphere 的 filter chain 和 MyBatis 原生插件系统之间的桥梁。

**所有已知实现：**

| 实现 | 模块 | 用途 |
|------|------|------|
| `LoggingExecutorFilter` | 13-microsphere-mybatis | SQL 执行日志 |
| `SentinelMyBatisExecutorFilter` | 07-sentinel | 限流 |

**Sentinel 实现的关键细节：**

```java
public class SentinelMyBatisExecutorFilter extends AbstractSentinelPlugin implements ExecutorFilter {

    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds,
                              ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql,
                              ExecutorFilterChain chain) throws SQLException {
        return doInSentinel(ms, () -> chain.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql));
    }

    public int update(MappedStatement ms, Object parameter, ExecutorFilterChain chain) throws SQLException {
        return doInSentinel(ms, () -> chain.update(ms, parameter));
    }

    private <T> T doInSentinel(MappedStatement ms, Callable<T> callable) throws SQLException {
        if (!isEnabled()) {
            return callable.call();  // 插件禁用时直接执行，不经过 Sentinel
        }
        String resourceName = ms.getId();  // "com.example.mapper.UserMapper.selectUser"
        return sentinelOperations.call(resourceName, context -> callable.call(), SQLException.class);
    }
}
```

这是唯一使用**一次性 `call()` 模式**（而非两阶段 begin/end）的适配器。因为 MyBatis 的 `ExecutorFilter` 把整个操作包裹在一个方法里，不需要分离的 pre/post 钩子。

`call(resourceName, callback, SQLException.class)` 的第三个参数是 checked exception 类型--`SQLException` 是 checked，`call` 方法会正确传播它。

资源命名：`ms.getId()` -> Mapper 接口方法的全限定名（如 `com.example.mapper.UserMapper.selectUser`）。稳定、低基数、一眼能看出是哪个查询。

插件禁用时直接跳过 Sentinel，零开销。

---

### 扩展点 3：RedisCommandInterceptor / RedisConnectionInterceptor--Redis 命令拦截

**定义在：** `microsphere-redis-spring`（08 模块）

```java
public interface RedisMethodInterceptor<T> extends Ordered {
    default void beforeExecute(RedisMethodContext<T> context) throws Throwable {}
    default void afterExecute(RedisMethodContext<T> context,
                              @Nullable Object result, @Nullable Throwable failure) throws Throwable {}
    default void handleError(RedisMethodContext<T> context, boolean before,
                             @Nullable Object result, @Nullable Throwable failure, Throwable error) {}
}

public interface RedisConnectionInterceptor extends RedisMethodInterceptor<RedisConnection> {}
public interface RedisCommandInterceptor extends RedisMethodInterceptor<RedisCommands> {}
```

两个层次的拦截器：
- `RedisConnectionInterceptor` -- 连接级操作（`getConnection`、`close`）
- `RedisCommandInterceptor` -- 命令级操作（每个 `SET`/`GET`/`DEL`/`HSET`...）

**发现机制：** `@EnableRedisInterceptor` -> `RedisInterceptorBeanDefinitionRegistrar` -> 从 `BeanSource` 发现

**激活链路：**

```
Spring Boot 启动
  -> RedisInitializer（ApplicationContextInitializer，spring.factories 注册）
    -> RedisModuleInitializer（SPI）
      -> RedisInterceptorModuleInitializer
        -> 检查 microsphere.redis.interceptor.enabled
        -> 注册 @EnableRedisInterceptor 配置
          -> RedisInterceptorBeanDefinitionRegistrar
            -> 从 BeanSource 发现所有 RedisCommandInterceptor 和 RedisConnectionInterceptor
            -> 注册 RedisConnectionFactoryProxyBeanPostProcessor
              -> 对每个 RedisConnectionFactory Bean 创建 AOP 代理
                -> 拦截 getConnection()，返回 JDK 动态代理的 RedisConnection
                  -> InterceptingRedisConnectionInvocationHandler
                    -> 每次 Redis 命令调用：beforeExecute(all interceptors) -> 真实调用 -> afterExecute(all interceptors)
```

**关键设计：双层代理。** 第一层 AOP 代理拦截 `RedisConnectionFactory.getConnection()`，把返回值替换为 JDK 动态代理的 `RedisConnection`。第二层 JDK 代理拦截 `RedisConnection` 的所有方法。这在 08-redis 文章中已详细分析。

**错误隔离：** 拦截器异常被 `catch (Throwable)` 吃掉，不阻塞 Redis 操作：

```java
private void beforeExecute(List<RedisMethodInterceptor> interceptors, RedisMethodContext context) {
    for (RedisMethodInterceptor interceptor : interceptors) {
        try {
            interceptor.beforeExecute(context);
        } catch (Throwable e) {
            interceptor.handleError(context, true, null, null, e);
            logger.error("...", e);
            // 不 throw，继续执行下一个拦截器
        }
    }
}
```

拦截器是"观察者"不是"守卫"--异常不会阻塞 Redis 操作。这个设计选择在 08-redis 文章中已讨论。

**所有已知实现：**

| 实现 | 类型 | 模块 | 用途 |
|------|------|------|------|
| `EventPublishingRedisCommandInterceptor` | RedisCommandInterceptor | 08-redis | 将 Redis 写命令发布为 `RedisCommandEvent`（Kafka 复制用） |
| `SentinelRedisCommandInterceptor` | RedisConnectionInterceptor | 07-sentinel | 限流 |

**Sentinel 实现的关键细节：**

```java
public class SentinelRedisCommandInterceptor extends AbstractSentinelPlugin
        implements RedisConnectionInterceptor, InitializingBean, BeanClassLoaderAware {

    // 启动时预计算所有 Redis 命令方法的资源名
    private Map<Method, String> methodResourceNamesCache;

    public void afterPropertiesSet() {
        initMethodResourceNamesCache();
    }

    private void initMethodResourceNamesCache() {
        // 遍历 RedisClusterConnection 的所有接口方法
        // 资源名格式：SentinelUtils.buildResourceName(method)
        // 例如："RedisClusterConnection.get(byte[])"
        for (Method method : RedisClusterConnection.class.getMethods()) {
            methodResourceNamesCache.put(method, SentinelUtils.buildResourceName(method));
        }
    }

    public void beforeExecute(RedisMethodContext<RedisConnection> context) {
        Method method = context.getMethod();
        String resourceName = methodResourceNamesCache.get(method);  // 从缓存取
        SentinelContext sc = sentinelOperations.begin(resourceName, getContextName(), getOrigin());
        setSentinelContext(context, sc);  // 存入 RedisMethodContext 的属性 map
    }

    public void afterExecute(RedisMethodContext<RedisConnection> context, Object result, Throwable failure) {
        SentinelContext sc = getSentinelContext(context);  // 从属性取出
        sc.setResult(result);
        sc.setFailure(failure);
        sentinelOperations.end(sc);
    }
}
```

资源名是**方法签名**（如 `RedisClusterConnection.get(byte[])`），不是具体的 key。这意味着所有 `GET` 命令共享同一个 Sentinel 资源--粒度在命令类型级别，不在 key 级别。

`SentinelContext` 存入 `RedisMethodContext` 的属性 map（不是 ThreadLocal）--因为 Redis 连接可能被多个线程复用（Lettuce 是异步的），ThreadLocal 不可靠。

**和 08-redis 的关系：** `SentinelRedisCommandInterceptor` 实现的是 `RedisConnectionInterceptor`，这是 08-redis 模块定义的扩展点。08-redis 的 `RedisConnectionFactoryProxyBeanPostProcessor` 负责发现所有 `RedisConnectionInterceptor` Bean 并注入代理链。sentinel 只需要声明自己是 `RedisConnectionInterceptor` 类型的 Bean，08-redis 的框架自动接管。

---

### 扩展点 4：AbstractStatementFilter--Druid 连接池拦截

**定义在：** `microsphere-alibaba-druid-core`（14 模块）

```java
public abstract class AbstractStatementFilter extends FilterAdapter {
    // 模板方法：子类只实现这两个
    protected abstract void beforeExecute(StatementProxy statement, String resourceName) throws Throwable;
    protected abstract void afterExecute(StatementProxy statement, String resourceName,
                                         Object result, Throwable failure);

    // 模板：把所有 Druid Filter 方法统一路由到 execute()
    protected <T> T execute(StatementProxy statement, ThrowableSupplier<T> callback) throws SQLException {
        String resourceName = getResourceName(statement);  // 从 SQL 提取资源名
        beforeExecute(statement, resourceName);
        try {
            return callback.get();
        } catch (Throwable e) {
            afterExecute(statement, resourceName, null, e);
            throw e;
        } finally {
            // ... afterExecute with result
        }
    }

    // 覆盖 Druid 的所有 statement_execute* 方法为 final
    @Override
    public final void statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql) {
        execute(statement, () -> chain.statement_executeUpdate(statement, sql));
    }
    // ... 其他 statement_execute* 方法
}
```

`AbstractStatementFilter` 继承 Druid 的 `FilterAdapter`，把 Druid 的十几个 `statement_execute*` 方法统一路由到 `execute()` 模板方法。子类只需要实现 `beforeExecute` 和 `afterExecute`。

**发现机制：** `@EnableAlibabaDruid` -> `AlibabaDruidRegistrar` -> 从 `BeanSource` 发现 `Filter` 类 -> 注册 `DruidDataSourceBeanPostProcessor`

**消费：** `DruidDataSourceBeanPostProcessor` 在 `DruidDataSource` 初始化前把收集到的 Filter 注入 `druidDataSource.getProxyFilters()`。Druid 初始化时自动按 Filter 链执行。

**资源命名策略：** `AbstractStatementFilter` 自带 SQL 解析，从 SQL 中提取表名和操作类型：
```
"SELECT * FROM users" -> "SELECT users"
"INSERT INTO orders ..." -> "INSERT orders"
"UPDATE users SET ..." -> "UPDATE users"
```

**所有已知实现：**

| 实现 | 模块 | 用途 |
|------|------|------|
| `LoggingStatementFilter` | 14-microsphere-druid | SQL 执行日志 |
| `SentinelAlibabaDruidFilter` | 07-sentinel | 限流 |

**Sentinel 实现的关键细节：**

```java
public class SentinelAlibabaDruidFilter extends AbstractStatementFilter
        implements SentinelPlugin {

    // 委托模式：因为已经继承了 AbstractStatementFilter，不能再继承 AbstractSentinelPlugin
    private final SentinelPlugin delegate;
    private final SentinelOperations sentinelOperations;

    public SentinelAlibabaDruidFilter() {
        this(DEFAULT_CONTEXT_NAME, DEFAULT_ORIGIN);
    }

    public SentinelAlibabaDruidFilter(String contextName, String origin) {
        this.delegate = new SimpleSentinelPlugin(
            PLUGIN_NAME,           // "alibaba-druid"
            contextName,           // "microsphere_sentinel_alibaba_druid_context"
            origin,                // "Filter"
            COMMON_DB_SQL,         // 资源类型
            IN,                    // 入口流量
            false                  // 不自动安装（手动调 install）
        );
        this.sentinelOperations = new SentinelTemplate(getResourceType(), getTrafficType());
        install(this);  // 手动注册到 SentinelPluginRepository
    }

    @Override
    protected void beforeExecute(StatementProxy statement, String resourceName) throws Throwable {
        if (isEnabled()) {
            // resourceName 由父类 AbstractStatementFilter 从 SQL 解析得到
            // 例如："SELECT users"、"INSERT orders"
            SentinelContext context = sentinelOperations.begin(resourceName, getContextName(), getOrigin());
            context.withinContext();  // 存入 ThreadLocal
        }
    }

    @Override
    protected void afterExecute(StatementProxy statement, String resourceName,
                                 Object result, Throwable failure) {
        if (isEnabled()) {
            doInContext(context -> {
                context.setResult(result);
                context.setFailure(failure);
                sentinelOperations.end(context);
            }, true);  // 从 ThreadLocal 取出，结束后清除
        }
    }

    // SentinelPlugin 方法全部委托给 delegate
    public String getName() { return delegate.getName(); }
    public boolean isEnabled() { return delegate.isEnabled(); }
    public void setEnabled(boolean enabled) { delegate.setEnabled(enabled); }
    // ...
}
```

**为什么用委托而不是继承？** `SentinelAlibabaDruidFilter` 已经继承了 `AbstractStatementFilter`（Druid 的要求），Java 单继承限制下不能再继承 `AbstractSentinelPlugin`。所以用 `SimpleSentinelPlugin` 作为委托，把 `SentinelPlugin` 接口的方法全部委托过去。

**资源命名策略：** 由父类 `AbstractStatementFilter` 提供，从 SQL 中解析表名和操作类型：
```
"SELECT * FROM users"     -> "SELECT users"
"INSERT INTO orders ..."  -> "INSERT orders"
"UPDATE users SET ..."    -> "UPDATE users"
```

**注册机制：** 双重注册：
1. Java SPI：`META-INF/services/com.alibaba.druid.filter.Filter` 注册为 Druid Filter（Druid 自动发现）
2. Spring Boot AutoConfiguration：`SentinelAlibabaDruidAutoConfiguration` 创建 `@Bean`

```java
@ConditionalOnSentinelAvailable
@ConditionalOnProperty(name = ENABLED_PROPERTY_NAME, matchIfMissing = true)
@ConditionalOnClass(name = {
    "io.microsphere.alibaba.druid.spring.boot.condition.ConditionalOnAlibabaDruidAvailable",
    "io.microsphere.alibaba.sentinel.alibaba.druid.SentinelAlibabaDruidFilter"
})
@AutoConfigureAfter(name = {
    "com.alibaba.cloud.sentinel.custom.SentinelAutoConfiguration",
    "io.microsphere.alibaba.druid.spring.boot.autoconfigure.AlibabaDruidAutoConfiguration"
})
public class SentinelAlibabaDruidAutoConfiguration {

    @ConditionalOnAlibabaDruidAvailable
    static class Config {
        @Bean @ConditionalOnMissingBean
        public SentinelAlibabaDruidFilter sentinelAlibabaDruidFilter() {
            return new SentinelAlibabaDruidFilter();
        }
    }
}
```

四层条件确保只在 Sentinel + Druid + microsphere Druid 模块都可用时才加载。`@AutoConfigureAfter` 确保在 Spring Cloud Alibaba Sentinel 和 microsphere Druid 之后加载。

---

### 扩展点 5：EntityCallback--Hibernate 实体回调

**定义在：** `microsphere-hibernate-core`（13 模块的 hibernate 子模块）

```java
public interface EntityCallback extends Prioritized {
    // 实体加载
    default void onPreLoad(Object entity) {}
    default void onPostLoad(Object entity) {}

    // 实体插入
    default void onPreInsert(Object entity) {}
    default void onPostInsert(Object entity) {}

    // 实体更新
    default void onPreUpdate(Object entity) {}
    default void onPostUpdate(Object entity) {}

    // 实体删除
    default void onPreDelete(Object entity) {}
    default void onPostDelete(Object entity) {}

    // ... 持久化、合并、刷新、驱逐、锁定、刷新等，共约 20 个钩子
}
```

**发现机制：** Java `ServiceLoader.load(EntityCallback.class)` -- 纯 Java SPI，不经过 Spring

**激活链路：**

```
Hibernate 初始化 SessionFactory
  -> Java ServiceLoader 发现 EntittyCallbackIntegrator
    -> EntittyCallbackIntegrator.integrate()
      -> 获取 Hibernate 的 EventListenerRegistry
      -> 创建 EntityCallbackListener
        -> EntityCallbackListener 构造时调 ServiceLoader.load(EntityCallback.class)
          -> 发现所有 EntityCallback 实现
          -> 组合为 CompositeEntityCallback（按 Prioritized 排序）
      -> 为每个 Hibernate 事件类型注册 EntityCallbackListener
        -> LOAD -> listener
        -> PERSIST -> listener
        -> MERGE -> listener
        -> ... 约 23 个事件类型
```

**消费：** Hibernate 事件发生时调用 `EntityCallbackListener` 的对应方法 -> 委托给 `CompositeEntityCallback` -> 依次执行所有 `EntityCallback` 实现。

**所有已知实现：**

| 实现 | 模块 | 用途 |
|------|------|------|
| `SentinelHibernateEntityCallback` | 07-sentinel | 限流 |
| `Resilience4jHibernateEntityCallback` | 11-resilience4j | 熔断 |

**关键发现：** `Resilience4jHibernateEntityCallback` 也实现了 `EntityCallback`！这证明了扩展点的可复用性--同一个扩展点被两个不同的限流引擎实现。

**Sentinel 实现的关键细节：**

```java
public class SentinelHibernateEntityCallback extends AbstractSentinelPlugin
        implements EntityCallback {

    public void onPreInsert(Object entity) {
        begin(entity, "INSERT");
    }
    public void onPostInsert(Object entity) {
        end();
    }
    public void onPreUpdate(Object entity) {
        begin(entity, "UPDATE");
    }
    public void onPostUpdate(Object entity) {
        end();
    }
    // ... load, delete, persist, merge 等

    private Optional<SentinelContext> begin(Object entity, String action) {
        if (!isEnabled()) return empty();
        String resourceName = getSentinelResourceName(entity, action);
        // 资源名："Entity:INSERT:com.example.User"
        SentinelContext ctx = sentinelOperations.begin(resourceName, getContextName(), getOrigin());
        ctx.withinContext();  // 存入 ThreadLocal
        return of(ctx);
    }

    private void end() {
        doInContext(context -> {
            sentinelOperations.end(context);
            return null;
        }, true);  // 用完即清
    }
}
```

资源命名：`Entity:{action}:{className}`（如 `Entity:INSERT:com.example.User`）。

上下文传递：ThreadLocal（通过 `withinContext()` / `doInContext(_, true)`）。Hibernate 的事件回调是同步的，在同一个线程内完成 pre/post，ThreadLocal 安全。

---

## 第四部分：6 个适配器的三种交互模式

6 个 sentinel 适配器不是同一个模式，而是三种：

### 模式一：一次性 call（MyBatis）

```java
// MyBatis ExecutorFilter 把整个查询包裹在一个方法里
return sentinelOperations.call(resourceName, () -> chain.query(...), SQLException.class);
```

- 不需要 begin/end 分离
- 不需要 ThreadLocal 或属性传递
- `call()` 内部做了 begin -> callback -> end
- 适配器代码最简

### 模式二：双阶段 begin/end + ThreadLocal（Druid、Hibernate、P6Spy）

```java
// beforeExecute 钩子
SentinelContext ctx = sentinelOperations.begin(resourceName, ...);
ctx.withinContext();  // 存入 ThreadLocal

// afterExecute 钩子（可能在不同方法中）
doInContext(context -> {
    context.setFailure(failure);
    sentinelOperations.end(context);
}, true);  // 从 ThreadLocal 取出，结束后清除
```

- 依赖 ThreadLocal 在 pre/post 之间传递 SentinelContext
- 风险：异步执行时 ThreadLocal 丢失（和 08-redis 的 `DynamicRedisConnectionFactory` 同样的问题）
- 适用场景：中间件的拦截器提供了分离开的 pre/post 钩子，但在同一个线程内

### 模式三：双阶段 begin/end + 框架属性（Redis、Spring Web）

```java
// beforeExecute 钩子
SentinelContext ctx = sentinelOperations.begin(resourceName, ...);
setSentinelContext(frameworkContext, ctx);  // 存入框架属性

// afterExecute 钩子
SentinelContext ctx = getSentinelContext(frameworkContext);  // 从属性取出
sentinelOperations.end(ctx);
```

- 不依赖 ThreadLocal
- Redis 用 `RedisMethodContext` 的属性 map
- Spring Web 用 `NativeWebRequest` 的属性
- 更安全：不担心异步/虚拟线程问题

### 三种模式的对比

| | MyBatis | Druid/Hibernate/P6Spy | Redis/Spring Web |
|--|---------|----------------------|-----------------|
| 调用方式 | 一次性 `call()` | 双阶段 begin/end | 双阶段 begin/end |
| 上下文传递 | 不需要 | ThreadLocal | 框架属性 |
| 异步安全 | 天然安全 | 不安全 | 安全 |
| 资源命名 | `Mapper.methodName` | SQL / `Entity:action:Name` | 方法签名 / `{pattern}#{handler}` |
| 为什么不同 | MyBatis 一次调用 = 一个方法 | 框架提供了分离开的 pre/post 钩子 | 框架提供了属性容器 |

---

### 扩展点 6（第三方 SPI）：JdbcEventListener--P6Spy JDBC 拦截

**定义在：** P6Spy 库（`com.p6spy.engine.event.JdbcEventListener`，不是 microsphere）

P6Spy 是一个 JDBC 代理层，包装真实的 JDBC `Statement`，在每次 SQL 执行前后触发事件。microsphere 的 sentinel 模块通过实现 P6Spy 的 `JdbcEventListener` 接入。

**接口定义（P6Spy 原生）：**

```java
// P6Spy 的接口，不是 microsphere 定义的
public class SimpleJdbcEventListener {
    public void onBeforeAnyExecute(StatementInformation statementInformation) {}
    public void onAfterAnyExecute(StatementInformation statementInformation,
                                   long timeElapsedNanos, SQLException e) {}
    // ... onBeforeAnyCommit, onAfterAnyCommit, onBeforeAnyBatchAdd, etc.
}
```

**发现机制：** 纯 Java SPI（`META-INF/services/com.p6spy.engine.event.JdbcEventListener`）

P6Spy 在启动时通过 `ServiceLoader.load(JdbcEventListener.class)` 发现所有监听器。microsphere 在这个 SPI 文件中注册了 `SentinelJdbcEventListener`。

**和 microsphere 扩展点的区别：** 这是第三方库的 SPI，不是 microsphere 自己定义的扩展点。但 sentinel 对待它的方式和 microsphere 自己的扩展点完全一样--实现接口 + 委托 SentinelPlugin。

**没有 AutoConfiguration 类：** P6Spy 和 Hibernate 一样，不需要 Spring Boot AutoConfiguration。它们靠 Java SPI 被对应框架自动发现。

**Sentinel 实现的关键细节：**

```java
public class SentinelJdbcEventListener extends SimpleJdbcEventListener
        implements SentinelPlugin {

    // 同样用委托模式（已经继承了 SimpleJdbcEventListener）
    private final SentinelPlugin delegate;
    private final SentinelOperations sentinelOperations;

    public SentinelJdbcEventListener() {
        this(DEFAULT_CONTEXT_NAME, DEFAULT_ORIGIN);
    }

    public SentinelJdbcEventListener(String contextName, String origin) {
        this.delegate = new SimpleSentinelPlugin(
            PLUGIN_NAME,           // "p6spy"
            contextName,           // "microsphere_sentinel_p6spy_context"
            origin,                // "Statement"
            COMMON_DB_SQL,         // 资源类型
            IN,                    // 入口流量
            false                  // 不自动安装
        );
        this.sentinelOperations = new SentinelTemplate(getResourceType(), getTrafficType());
        install(this);  // 手动注册到 SentinelPluginRepository
    }

    @Override
    public void onBeforeAnyExecute(StatementInformation statementInformation) {
        if (isEnabled()) {
            if (isEligibleStatement(statementInformation)) {
                execute(() -> {  // execute() 吞掉异常（ThrowableAction.execute）
                    String resourceName = getResourceName(statementInformation);
                    SentinelContext context = sentinelOperations.begin(
                        resourceName, getContextName(), getOrigin());
                    context.withinContext();  // 存入 ThreadLocal
                });
            }
        }
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation,
                                   long timeElapsedNanos, SQLException e) {
        if (isEnabled()) {
            if (isEligibleStatement(statementInformation)) {
                doInContext(context -> {
                    context.setFailure(e);
                    sentinelOperations.end(context);
                }, true);  // 从 ThreadLocal 取出，结束后清除
            }
        }
    }

    // 资源命名：直接用 SQL 字符串作为 Sentinel 资源名
    protected String getResourceName(StatementInformation statementInformation) {
        return statementInformation.getSql();
    }

    // 只拦截 PreparedStatement（参数化查询），不拦截普通 Statement
    protected boolean isEligibleStatement(StatementInformation statementInformation) {
        return statementInformation instanceof PreparedStatementInformation;
    }

    // SentinelPlugin 方法全部委托
    public String getName() { return delegate.getName(); }
    public boolean isEnabled() { return delegate.isEnabled(); }
    // ...
}
```

**资源命名策略：** 直接用 `statementInformation.getSql()` --**裸 SQL 字符串**作为 Sentinel 资源名。

这是一个有争议的选择：
- **优点**：精确到每条 SQL，Sentinel Dashboard 中能看到具体哪条 SQL 被限流
- **风险**：高基数。如果 SQL 有动态拼接（而非 PreparedStatement 的 `?` 占位符），每条不同的 SQL 都是一个新资源，Sentinel 的资源统计会爆炸

`isEligibleStatement()` 只允许 `PreparedStatementInformation` 通过--PreparedStatement 的 SQL 有 `?` 占位符，参数不拼入 SQL，基数可控。但如果同一个查询出现在多个 Mapper 中，SQL 字符串相同 -> 共享一个 Sentinel 资源 -> 无法区分来源。

**对比 MyBatis 的资源命名：**

| | MyBatis（ExecutorFilter） | P6Spy（JdbcEventListener） |
|--|-------------------------|--------------------------|
| 资源名 | `com.example.mapper.UserMapper.selectUser` | `SELECT id, name FROM users WHERE id = ?` |
| 粒度 | Mapper 方法级 | SQL 文本级 |
| 基数 | 低（每个方法一个资源） | 中（每个 SQL 模板一个资源） |
| 可读性 | 高（一眼看出是哪个 Mapper） | 中（需要看 SQL 才知道做什么） |
| 区分来源 | 精确（不同 Mapper 不同资源） | 不精确（同 SQL 不同 Mapper 共享资源） |

两个适配器保护的是**同一条 SQL 的不同层次**：MyBatis 在 ORM 层拦截，P6Spy 在 JDBC 层拦截。如果同时启用，同一条查询会经过两次 Sentinel entry（先 MyBatis 再 P6Spy），形成嵌套的限流资源。

**onBeforeAnyExecute 中的 `execute()` 包装：**

```java
execute(() -> {  // ThrowableAction.execute，吞掉异常
    String resourceName = getResourceName(statementInformation);
    SentinelContext context = sentinelOperations.begin(...);
    context.withinContext();
});
```

`execute()` 是 `ThrowableAction.execute(Runnable)`，把 checked exception 包装为 RuntimeException。如果 `begin()` 抛异常（如 Sentinel 初始化失败），异常被吞掉，不影响 JDBC 执行。这和 microsphere-redis 的"拦截器是观察者不是守卫"理念一致。

---

## 第五部分：扩展点的共同模式

5 个扩展点共享同一个四步架构：

```
定义：接口/抽象类   ->  在底层模块中定义（不关心限流）
发现：BeanSource    ->  通过 SpringFactoriesLoader / ServiceLoader / BeanFactory 发现
激活：@Enable 注解  ->  由自动装配或 SPI 初始化器触发
消费：Collector 类  ->  在合适的时机收集所有实现，组合成链依次执行
```

```
        03-spring-web                08-redis                 13-mybatis
  定义 HandlerMethodInterceptor  定义 RedisCommandInterceptor  定义 ExecutorFilter
               │                          │                          │
   ┌───────────┼───────────┐     ┌────────┼────────┐         ┌──────┼──────┐
   │           │           │     │        │        │         │      │      │
   ▼           ▼           ▼     ▼        ▼        ▼         ▼      ▼      ▼
WebEvent    Sentinel    用户   Event   Sentinel   用户    Logging  Sentinel  用户
Publisher   限流        自定义  Publish 限流      自定义   Filter   限流      自定义
（内置）                审计   （Kafka）          缓存     （内置）          审计
                       日志                     穿透
```

**关键设计决策**：扩展点由底层模块定义，上层模块实现。底层模块不知道也不关心上层模块怎么用。`microsphere-spring-web` 不知道 `HandlerMethodInterceptor` 被用在了限流、事件发布还是日志--它只负责定义钩子和收集实现。

---

## 第六部分：和官方 Spring Cloud Alibaba Sentinel 的对比

### 哲学差异

| | 官方 Sentinel | Microsphere Sentinel |
|---|---|---|
| **控制方式** | opt-in（`@SentinelResource` 注解，开发者主动标注） | opt-out（拦截器自动保护，开发者选择不保护什么） |
| **保护范围** | 开发者记得加注解的地方 | 框架能拦截到的地方（全部） |
| **新加中间件** | 等官方出适配器 | microsphere 的模块自带拦截器，sentinel 实现它 |
| **代价** | 注解散落各处，容易被忘记 | 拦截器对全量调用生效，有性能开销 |

### 性能代价

自动全量保护的代价是：即使 Sentinel 限流配置为空，每条 MyBatis SQL、每个 Redis 命令、每个控制器方法都经过：

```
beforeExecute(SentinelPlugin)
  -> SphU.entry() 创建 Sentinel Entry（即使没有规则也有开销）
    -> original call
  -> SphU.exit() 释放 Entry
afterExecute(SentinelPlugin)
```

官方 `@SentinelResource` 只在有注解的方法上运行，开销精确可控。Microsphere 的开销在所有被保护的调用点上。

但 Sentinel 的 `SphU.entry()` 在没有规则时开销很小（主要是 ThreadLocal 操作 + slot chain 空跑），实际影响通常 < 0.01ms。

### 为什么 microsphere 能做

因为它之前已经做了 03-spring-web（定义 `HandlerMethodInterceptor`）、08-redis（定义 `RedisCommandInterceptor`）、microsphere-mybatis（定义 `ExecutorFilter`）...

官方 Sentinel 想支持 MyBatis，需要从零写 MyBatis 拦截器。Microsphere 不需要--拦截器已经在 MyBatis 模块里了，sentinel 只是实现它。

**先有钩子，后有钩子的用途。** 这是基础设施先行设计--每个模块先定义通用拦截接口，后续模块再给这些接口注入具体功能。

---

## 第七部分：自举架构和可扩展性

### 自举：框架自己扩展自己

```
03-spring-web 定义了 HandlerMethodInterceptor
  -> 05-spring-cloud 用它做"Web 端点元数据收集"
  -> 07-sentinel 用它做"接口限流"
  -> 用户可以用它做"审计日志"
  -> 同一个钩子，不同用途

08-microsphere-redis 定义了 RedisConnectionInterceptor
  -> 08-redis 自己用它做"Kafka 复制事件发布"
  -> 07-sentinel 实现它，做 Redis 命令限流
  -> 同一个钩子，不同用途

microsphere-hibernate 定义了 EntityCallback
  -> 07-sentinel 实现它，做实体操作限流
  -> 11-resilience4j 实现它，做实体操作熔断
  -> 同一个钩子，不同限流引擎
```

### 可扩展性

**新增中间件（如 Elasticsearch）：**
1. 在 `microsphere-elasticsearch` 中定义 `EsQueryInterceptor` 扩展点
2. sentinel 实现它
3. 自动保护所有 ES 查询

**新增限流引擎（如 Hystrix 替代品）：**
1. 实现同样的 5 个扩展点
2. 不需要改中间件模块
3. Resilience4j 已经这样做了--它有 `Resilience4jHibernateEntityCallback`

**扩展点的定义者和实现者解耦。** 中间件模块不需要知道 sentinel 存在，限流模块不需要知道中间件的内部细节。

---

## 第八部分：Spring Cloud 集成

### SentinelCloudAutoConfiguration

```java
@ConditionalOnSentinelAvailable
@AutoConfigureAfter({
    SentinelAlibabaDruidAutoConfiguration.class,
    SentinelMyBatisAutoConfiguration.class,
    SentinelRedisAutoConfiguration.class,
    SentinelSpringWebAutoConfiguration.class,
    // ... 其他 microsphere cloud auto-configs
})
public class SentinelCloudAutoConfiguration {

    @Bean
    public HasFeatures features() {
        List<NamedFeature> features = new ArrayList<>();
        Collection<SentinelPlugin> plugins = SentinelPluginRepository.INSTANCE.getAll();
        for (SentinelPlugin plugin : plugins) {
            if (plugin.isEnabled()) {
                features.add(new NamedFeature(plugin.getName(), plugin.getClass()));
            }
        }
        return HasFeatures.abstractFeatures(features);
    }
}
```

这把所有已安装且启用的 Sentinel 插件暴露到 Spring Cloud Actuator 的 `/actuator/features` 端点。运维可以通过 HTTP 查看哪些中间件被 Sentinel 保护了。

---

## 总结

### 扩展点全景

| 扩展点 | 定义模块 | 发现机制 | Sentinel 实现 | 其他实现 |
|--------|---------|---------|-------------|---------|
| `HandlerMethodInterceptor` | 03-spring-web | Spring Bean + @EnableWebMvcExtension | `SentinelHandlerMethodInterceptor` | `WebEventPublisher`、`StoringResponseBody...` |
| `ExecutorFilter` | 13-microsphere-mybatis | Spring Bean + @EnableMyBatisExtension | `SentinelMyBatisExecutorFilter` | `LoggingExecutorFilter` |
| `RedisCommandInterceptor` | 08-redis | Spring Bean + @EnableRedisInterceptor | `SentinelRedisCommandInterceptor` | `EventPublishingRedisCommandInterceptor` |
| `AbstractStatementFilter` | 14-microsphere-druid | Spring Bean + @EnableAlibabaDruid + Java SPI | `SentinelAlibabaDruidFilter` | `LoggingStatementFilter` |
| `EntityCallback` | 13-hibernate | Java ServiceLoader | `SentinelHibernateEntityCallback` | `Resilience4jHibernateEntityCallback` |
| `JdbcEventListener`（P6Spy 第三方） | P6Spy 库 | Java ServiceLoader | `SentinelJdbcEventListener` |（无）|

### Sentinel 的 30 个文件到底是什么

| 类别 | 文件数 | 内容 |
|------|--------|------|
| `SentinelTemplate` + `SentinelContext` + `SentinelOperations` | 3 | 模板方法包装 SphU API |
| `SentinelPlugin` + `AbstractSentinelPlugin` + `SimpleSentinelPlugin` | 3 | 插件 SPI 定义 |
| `SentinelPluginRepository` + `SimpleSentinelPluginRepository` + `JMXSentinelPluginRepository` | 3 | 插件仓库 + JMX 管理 |
| `SentinelConstants` + `SentinelUtils` | 2 | 常量和工具 |
| 6 个适配器 + 6 个常量类 | 12 | 实现其他模块的扩展点 |
| 4 个 AutoConfiguration + 2 个条件注解 | 6 | Spring Boot 自动装配 |
| 1 个 CloudAutoConfiguration | 1 | Actuator 集成 |
| **合计** | **30** | |

### 和其他模块的对比

| 模块 | 本质 | 和扩展点的关系 |
|------|------|-------------|
| 06-nacos | 手写 HTTP 客户端 | 独立，不消费扩展点 |
| 08-redis | 定义 `RedisCommandInterceptor` 扩展点 | 扩展点的**定义者** |
| 03-spring-web | 定义 `HandlerMethodInterceptor` 扩展点 | 扩展点的**定义者** |
| 13-microsphere-mybatis | 定义 `ExecutorFilter` 扩展点 | 扩展点的**定义者** |
| 14-microsphere-druid | 定义 `AbstractStatementFilter` 扩展点 | 扩展点的**定义者** |
| 13-hibernate | 定义 `EntityCallback` 扩展点 | 扩展点的**定义者** |
| **07-sentinel** | **实现 6 个扩展点** | 扩展点的**消费者** |
| 11-resilience4j | **实现 1 个扩展点** | 扩展点的**消费者** |

**07-sentinel 的本质不是限流模块，是扩展点的消费者。** 限流只是它作为消费者选择提供的功能。Resilience4j 也是消费者，提供的是熔断功能。未来任何新的限流/熔断/降级引擎都可以通过实现同样的扩展点接入。

### 对 sca-lab 的启发

如果要在自己的微服务框架中统一接入限流：
1. **先在每个中间件模块中定义通用扩展点**（属于该模块的业务逻辑，不关心限流）
2. **再用一个 capstone 模块实现所有扩展点**（把钩子连到限流引擎）
3. **钩子的定义者和实现者分离**，各模块可以独立演进
4. **用 JMX 暴露插件状态**，运维可以运行时调整保护范围
