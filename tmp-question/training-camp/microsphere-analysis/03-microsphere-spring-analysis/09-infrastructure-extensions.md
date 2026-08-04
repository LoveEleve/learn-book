# 03-09 基础设施扩展杂项

## 问题：Spring 生态的六个长尾空白

前八篇覆盖了 microsphere-spring 的核心扩展（生命周期、并行初始化、事件拦截、依赖注入、配置系统、注解扩展、Web 元数据、方法拦截）。本篇覆盖六个较小的扩展，它们各自解决一个独立的"长尾"问题：

| 扩展 | Spring 的空白 | 解决方式 |
|------|-------------|---------|
| URL Stream Handler | `java.net.URL` 不支持 `spring://` 协议 | 自定义 `URLStreamHandler` 桥接 Spring Resource/Environment/Bean |
| TTL 缓存 | `@Cacheable` 不支持 per-key TTL | `@TTLCacheable` + `TTLContext` ThreadLocal 传递 TTL |
| 类型转换桥接 | microsphere `Converter`（Java SPI）与 Spring `ConversionService` 不互通 | `SpringConverterAdapter` 适配器 |
| SmartLifecycle 扩展 | `SmartLifecycle` 缺少抽象基类和常量 | `AbstractSmartLifecycle` 模板方法 |
| P6Spy 集成 | P6Spy SQL 监控需手动包装 DataSource | `@EnableP6DataSource` 自动包装 |
| Guice 集成 | Google Guice `@Inject` 与 Spring 不互通 | `GuiceInjectAnnotationBeanPostProcessor` 桥接 |

---

## URL Stream Handler 桥接

### 设计

`SpringProtocolURLStreamHandler` 注册 `spring://` 协议，将 Spring 的 Resource、Environment、Bean 暴露为 URL 资源：

```
spring:resource:classpath:config.yml        -> Spring ResourceLoader.getResource()
spring:env:property-sources                 -> Environment.getPropertySources()
spring:env:profiles                         -> Environment.getActiveProfiles()
spring:bean:myDataSource                    -> BeanFactory.getBean("myDataSource")
```

继承 `ExtendableProtocolURLStreamHandler`（microsphere-java 的可扩展协议处理器），通过子协议工厂路由：

| 子协议 | 工厂 | 数据来源 |
|--------|------|---------|
| `resource` | `SpringResourceURLConnectionFactory` | `ResourceLoader.getResource(path)` |
| `env` | `SpringEnvironmentURLConnectionFactory` | `Environment.getPropertySources()` 或 `getActiveProfiles()` |
| `bean` | `SpringDelegatingBeanProtocolURLConnectionFactory` | `BeanFactory.getBean(name)` |

`SpringResourceURLConnection` 尝试通过 `Resource.getURL().openConnection()` 获取底层 `URLConnection`；如果 Resource 不支持 `getURL()`（如 `ClassPathResource` 在某些场景），退化为 `SpringResourceURLConnectionAdapter`（直接代理 Resource 的 InputStream）。

### 使用场景

- **配置导入**：`@PropertySource("spring:resource:classpath:config.yml")` 可以加载 Spring Resource 作为属性源
- **跨框架资源访问**：非 Spring 框架通过标准 `URL` API 访问 Spring 管理的资源
- **P6Spy 集成**：`@EnableP6DataSource` 同时注册 `SpringProtocolURLStreamHandler` 和 `SpringP6SpyURLConnectionFactory`，使 P6Spy 可以通过 `spring://` 协议访问 Spring 资源

---

## TTL 缓存扩展

### 设计

Spring 的 `@Cacheable` 不支持 per-key TTL--所有缓存的 Key 使用相同的过期策略。microsphere 通过 `@TTLCacheable` 扩展：

```java
@TTLCacheable(
    cacheNames = "users",
    key = "#id",
    ttl = 30,                    // TTL 值
    timeUnit = TimeUnit.MINUTES   // TTL 单位
)
public User getUser(Long id) { ... }
```

`@TTLCacheable` 元注解 `@Cacheable(cacheResolver = "ttlCacheResolver")`，将缓存解析委托给 `TTLCacheResolver`。`TTLCacheResolver` 从注解中读取 `ttl` 和 `timeUnit`，通过 `TTLContext` 传递给底层缓存实现。

### TTLContext：ThreadLocal 传递

```java
public class TTLContext {
    private static final ThreadLocal<Duration> ttlThreadLocal = new ThreadLocal<>();

    public static void setTTL(Duration ttl) { ttlThreadLocal.set(ttl); }

    public static <R> R doWithTTL(Function<Duration, R> function, Duration defaultTTL) {
        Duration effectiveTTL = getEffectiveTTL(defaultTTL);
        try {
            return function.apply(effectiveTTL);
        } finally {
            ttlThreadLocal.remove();  // 清理 ThreadLocal
        }
    }
}
```

`TTLContext` 用 ThreadLocal 在缓存写入时传递 TTL 值。`doWithTTL` 方法在 `finally` 中清理 ThreadLocal，防止线程池复用导致的泄漏。

### Redis TTL 集成：未完成

`TTLRedisConfiguration` 和 `TTLRedisCacheWriterWrapper` 的**全部代码被 `//` 注释掉**（分别为 70 行和 98 行，每行都以 `//` 开头）。这意味着 Redis TTL 集成的代码写了一半但没有启用。

**功能链路断裂分析**：

```
@TTLCacheable(ttl=30, timeUnit=MINUTES)       ← 注解存在 ✅
    │
    ├── TTLCacheResolver                        ← 解析器存在 ✅，读取 ttl=30
    │       │
    │       └── TTLContext.setTTL(30min)         ← ThreadLocal 设值 ✅
    │
    ├── 缓存写入（Redis）
    │       └── TTLRedisCacheWriterWrapper       ← 被注释 ❌，无人读取 ThreadLocal
    │
    └── 实际行为：TTL 值设了但没人用
        └── 缓存按 Redis 全局默认 TTL 过期，per-key TTL 不生效
```

`@TTLCacheable` 退化为普通 `@Cacheable`--注解上的 `ttl` 和 `timeUnit` 参数被忽略。整个 TTL 缓存功能目前只有"注解 + ThreadLocal 传递"的基础设施，缺少"从 ThreadLocal 读取 TTL 并应用到缓存写入"的最后一环。相当于路标立了但没装摄像头。

**这个功能有价值吗？**

有价值，但需要理解它解决的具体问题。Spring Cache 的 `@Cacheable` 只支持**缓存级别**的 TTL（通过 `RedisCacheConfiguration.entryTtl(Duration)` 设置全局 TTL），不支持**方法级别**或**Key 级别**的 TTL。实际业务中不同数据的过期需求差异很大：

```java
// 用户信息：30 分钟过期（变化不频繁）
@TTLCacheable(cacheNames = "users", key = "#id", ttl = 30, timeUnit = TimeUnit.MINUTES)
public User getUser(Long id) { ... }

// 验证码：60 秒过期（短生命周期）
@TTLCacheable(cacheNames = "codes", key = "#phone", ttl = 60, timeUnit = TimeUnit.SECONDS)
public String getVerifyCode(String phone) { ... }

// 配置信息：永不过期（手动失效）
@TTLCacheable(cacheNames = "config", key = "#key", ttl = -1)
public String getConfig(String key) { ... }
```

如果不支持 per-key TTL，开发者要么为每种 TTL 创建不同的 `CacheManager`/`Cache` 配置，要么在业务代码中手动调用 `redis.expire(key, ttl)`--两种方案都侵入性强且容易遗漏。

microsphere 的设计思路是正确的（注解声明 TTL -> ThreadLocal 传递 -> CacheWriter 读取并应用），只是 Redis 的实现层尚未完成。用户可以参考注释掉的代码自行实现 `TTLRedisCacheWriterWrapper`，或等待后续版本。

### 如果自行实现 TTLRedisCacheWriterWrapper

已有的基础设施不需要改动（`@TTLCacheable` -> `TTLCacheResolver` -> `TTLContext.setTTL()` 都已就绪），只需实现一个类：

```java
// 核心逻辑（伪代码）
public class TTLRedisCacheWriterWrapper implements RedisCacheWriter {
    private final RedisCacheWriter delegate;

    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        Duration effectiveTTL = TTLContext.getTTL();  // 从 ThreadLocal 读取
        if (effectiveTTL != null && !effectiveTTL.isZero()) {
            ttl = effectiveTTL;  // 替换为注解声明的 TTL
        }
        delegate.put(name, key, value, ttl);  // 委托原始 Writer
    }
}
```

逻辑不复杂--`RedisCacheWriter.put` 方法本身就有 `Duration ttl` 参数，Wrapper 只需要在调用前替换为 ThreadLocal 中的值。

**真正的难点不在逻辑，而在反射安装**：`TTLRedisConfiguration` 需要反射修改 `RedisCacheManager` 内部的 `RedisCacheWriter` 字段。这个字段名和访问方式在 Spring Data Redis 的不同版本间可能变化：

- Spring Boot 2.x：`RedisCacheManager` 有 `cacheWriter` 字段
- Spring Boot 3.x：字段可能改名或封装方式变化
- `RedisCacheManager.RedisCacheManagerBuilder` 的构建方式也有差异

注释掉的代码正是用 `FieldUtils.getFieldValue` + `setFieldValue` 做反射替换，思路正确，可能是因为版本兼容性问题暂时注释掉了。

**实现评估**：锁定单个 Spring Boot 版本（如 3.2），半天内可实现并测试通过。兼容 Spring Boot 2.x ~ 3.x 多版本需要额外做版本检测和适配，约 1-2 天。

---

## 类型转换桥接

### SpringConverterAdapter

microsphere-java 定义了自己的 `Converter` 接口（通过 Java SPI 加载），与 Spring 的 `Converter`/`GenericConverter` 体系不互通。`SpringConverterAdapter` 桥接两者：

```java
public class SpringConverterAdapter implements ConditionalGenericConverter {

    private static final Map<ConvertiblePair, Converter> convertersMap;

    static {
        List<Converter> converters = loadServicesList(Converter.class);  // Java SPI
        // 按 sourceType -> targetType 映射
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return convertersMap.containsKey(buildConvertiblePair(sourceType, targetType));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        Converter converter = convertersMap.get(buildConvertiblePair(sourceType, targetType));
        return converter.convert(source);
    }
}
```

`SpringConverterAdapter` 实现 `ConditionalGenericConverter`，在 `matches` 中检查是否有匹配的 microsphere `Converter`，在 `convert` 中委托给 microsphere `Converter`。

### ConversionServiceResolver

`ConversionServiceResolver` 从多个来源解析 `ConversionService`，按优先级：

1. 已解析的单例（`resolved-conversionService` Bean）
2. `ConfigurableBeanFactory.getConversionService()`
3. `ConfigurableEnvironment.getConversionService()`
4. `CONVERSION_SERVICE_BEAN_NAME` Bean
5. 默认 `DefaultFormattingConversionService`

解析后注册为单例，避免重复解析。这个解析器被 `SpringProtocolURLStreamHandler` 和 `ConfigurationBeanBindingPostProcessor` 使用。

### 启用

`@EnableSpringConverterAdapter` 通过 `EnableSpringConverterAdapterRegistrar` 将 `SpringConverterAdapter` 注册到 `ConversionService`。

---

## SmartLifecycle 扩展

### AbstractSmartLifecycle

`AbstractSmartLifecycle` 是 `SmartLifecycle` 的模板方法基类：

```java
public abstract class AbstractSmartLifecycle implements SmartLifecycle {

    public static final int EARLIEST_PHASE = Integer.MIN_VALUE;
    public static final int LATEST_PHASE = Integer.MAX_VALUE;
    public static final int DEFAULT_PHASE = LATEST_PHASE;

    private int phase = DEFAULT_PHASE;
    private volatile boolean started = false;

    @Override
    public final void start() { doStart(); started = true; }
    @Override
    public final void stop() { doStop(); started = false; }
    @Override
    public final boolean isRunning() { return started; }

    protected abstract void doStart();
    protected abstract void doStop();
}
```

**设计要点**：
- `start`/`stop`/`isRunning` 标记为 `final`，子类只能覆盖 `doStart`/`doStop`，防止状态管理错误
- `started` 标记为 `volatile`，确保多线程可见性
- Phase 常量（`EARLIEST_PHASE`/`LATEST_PHASE`/`DEFAULT_PHASE`）提供标准化引用，被第 7 篇的 `WebEndpointMappingRegistrar` 和 `WebEventPublisher` 使用
- `stop(Runnable)` 默认实现调用 `stop()` 后执行回调，支持优雅停机

### LoggingSmartLifecycle

`LoggingSmartLifecycle` 是 `AbstractSmartLifecycle` 的简单实现，在 `doStart`/`doStop` 中打印日志。用于调试和验证 SmartLifecycle 的执行顺序。

---

## P6Spy JDBC 集成

### 设计

P6Spy 是一个 JDBC 代理工具，拦截 SQL 执行并记录日志/性能数据。传统使用方式需要手动将 `DataSource` 包装为 `P6DataSource`。microsphere 通过 `@EnableP6DataSource` 自动包装：

```java
@EnableP6DataSource
@Configuration
public class DataSourceConfig { }
```

`P6DataSourceBeanPostProcessor` 继承 `GenericBeanPostProcessorAdapter<DataSource>`（第 4 篇），在 `postProcessAfterInitialization` 中将每个 `DataSource` Bean 包装为 `P6DataSource`：

```java
protected DataSource doPostProcessAfterInitialization(DataSource bean, String beanName) {
    if (excludedDataSourceBeanNames.contains(beanName)) {
        return bean;  // 排除的 Bean 不包装
    }
    DataSource targetDataSource = bean.unwrap(DataSource.class);
    return new P6DataSource(targetDataSource);  // 包装为 P6Spy 代理
}
```

**排除机制**：通过 `microsphere.jdbc.p6spy.excluded-datasource-beans` 属性排除特定 DataSource（如监控数据源本身不需要被代理）。

**`unwrap` 处理**：如果 DataSource 是代理（如 HikariCP 的 `HikariProxyDataSource`），`unwrap(DataSource.class)` 获取底层真实 DataSource，避免双重代理。

### 集成 spring:// 协议

`@EnableP6DataSource` 同时注册 `SpringProtocolURLStreamHandler` 和 `SpringP6SpyURLConnectionFactory`，使 P6Spy 可以通过 `spring://` 协议访问 Spring 的 Resource、Environment、Bean。这实现了 P6Spy 配置与 Spring 配置的统一管理。

---

## Guice 集成

### 设计

Google Guice 是另一个 DI 框架，使用 `@Inject` 注解（JSR-330）。Spring 也支持 `@Inject`（通过 `AutowiredAnnotationBeanPostProcessor`），但 Guice 的 `@Inject` 有一个 `optional` 属性，Spring 不理解。

`@EnableGuice` 注册 `GuiceInjectAnnotationBeanPostProcessor`，它继承 `AnnotatedInjectionBeanPostProcessor`（第 4 篇），专门处理 Guice 的 `@Inject`：

```java
class GuiceInjectAnnotationBeanPostProcessor extends AnnotatedInjectionBeanPostProcessor {

    GuiceInjectAnnotationBeanPostProcessor() {
        super(Inject.class);  // 处理 com.google.inject.Inject
    }

    @Override
    protected boolean determineRequiredStatus(AnnotationAttributes attributes) {
        // Guice @Inject(optional=true) -> 非必需
        // Guice @Inject(optional=false) -> 必需
        return Boolean.FALSE.equals(attributes.getBoolean("optional"));
    }
}
```

`determineRequiredStatus` 将 Guice 的 `optional` 属性映射为 Spring 的 `required` 语义：`optional=true` 对应 `required=false`（找不到 Bean 不报错）。

### 与 Spring 原生 @Inject 支持的区别

Spring 的 `AutowiredAnnotationBeanPostProcessor` 已经支持 `javax.inject.Inject`（JSR-330 标准），但不支持 Guice 扩展的 `optional` 属性。microsphere 的 `GuiceInjectAnnotationBeanPostProcessor` 补充了这个能力，使得 Guice 的 `@Inject(optional=true)` 在 Spring 中也能正确语义化。

---

## 永恒原理

### 1. 协议桥接与资源统一寻址

`SpringProtocolURLStreamHandler` 将 Spring 的内部资源（Resource、Environment、Bean）暴露为标准 URL，遵循了"统一寻址"原则。任何支持 `URL` API 的工具都可以访问 Spring 资源，无需依赖 Spring API。

这与 JDBC 的 `jdbc:` 协议、JNDI 的 `ldap://` 协议是同一思想--为特定领域的资源定义标准 URL 协议，使得通用工具可以访问。

### 2. ThreadLocal 上下文传递

`TTLContext` 使用 ThreadLocal 在方法调用链中传递 TTL 值，无需修改方法签名。这种模式在 Spring 生态中很常见：
- `TransactionSynchronizationManager` 用 ThreadLocal 绑定事务资源
- `RequestContextHolder` 用 ThreadLocal 绑定请求属性
- `LocaleContextHolder` 用 ThreadLocal 绑定 Locale

共同的风险是线程池复用导致的泄漏，共同的最佳实践是 `try-finally` 清理。

### 3. 适配器模式连接异质类型系统

`SpringConverterAdapter` 将 microsphere `Converter`（Java SPI）适配为 Spring `ConditionalGenericConverter`。`GuiceInjectAnnotationBeanPostProcessor` 将 Guice `@Inject` 适配为 Spring 注入语义。两者都是适配器模式的经典应用--不修改任一方的接口，通过中间适配器实现互操作。

适配器的核心约束是：**适配器必须完全实现目标接口的语义**。`SpringConverterAdapter` 必须正确实现 `matches` 和 `convert`；`GuiceInjectAnnotationBeanPostProcessor` 必须正确映射 `optional` 到 `required`。如果语义映射不完整（如遗漏边界情况），适配器会成为隐藏 bug 的来源。

### 4. 模板方法与 final 保护

`AbstractSmartLifecycle` 将 `start`/`stop`/`isRunning` 标记为 `final`，子类只能覆盖 `doStart`/`doStop`。这确保了状态管理（`started` 标志）不会被子类意外破坏。`GenericBeanPostProcessorAdapter`（第 4 篇）和 `BeanFactoryListeners`（第 1 篇）也使用了相同的 `final` 保护模式。

---

## 边界与反例

### 1. TTLRedisConfiguration 未实现

`TTLRedisConfiguration` 和 `TTLRedisCacheWriterWrapper` 的全部代码被注释掉（`//`）。这意味着 TTL 缓存的 Redis 集成尚未完成。用户如果依赖 `@TTLCacheable` + Redis，TTL 不会生效。

**缓解**：用户可以参考注释掉的代码自行实现 `TTLRedisCacheWriterWrapper`，或等待后续版本。

### 2. SpringProtocolURLStreamHandler 的类加载风险

`URLStreamHandler` 通过 `URL.setURLStreamHandlerFactory` 或 `META-INF/services/java.net.spi.URLStreamHandlerProvider` 注册。如果 JVM 已有其他 `URLStreamHandlerFactory`（如某些应用服务器），注册可能失败。

### 3. SpringConverterAdapter 的静态初始化

`convertersMap` 在静态初始化块中通过 `loadServicesList(Converter.class)` 加载。如果 microsphere `Converter` 实现依赖了尚未初始化的资源（如 Spring Bean），加载会失败。静态初始化的时机不可控，可能在类加载的早期阶段触发。

### 4. P6DataSourceBeanPostProcessor 的 unwrap 失败

`bean.unwrap(DataSource.class)` 在某些 DataSource 实现（如自定义代理）中可能抛出 `SQLException`。microsphere 捕获异常并跳过包装（返回原始 Bean），但这意味着该 DataSource 不会被 P6Spy 监控。

### 5. Guice @Inject 与 JSR-330 @Inject 的冲突

如果 classpath 中同时存在 `com.google.inject.Inject` 和 `javax.inject.Inject`，Spring 的 `AutowiredAnnotationBeanPostProcessor` 会处理后者，`GuiceInjectAnnotationBeanPostProcessor` 处理前者。如果同一个字段同时标注了两者，会导致双重注入。

**缓解**：避免在同一字段上同时使用两种 `@Inject` 注解。

---

## 现代 Spring（6.x）是否已支持？

| microsphere 特性 | Spring 6.x 是否有等价物 | 说明 |
|------------------|----------------------|------|
| `spring://` URL 协议 | 无 | Spring 6.x 无 Spring 资源的 URL 协议 |
| `@TTLCacheable` per-key TTL | 部分 | Spring Cache 不支持 per-key TTL；Caffeine/Guava Cache 支持 but 非 Spring 标准 |
| `SpringConverterAdapter` | 无 | Spring 6.x 无 Java SPI Converter 桥接 |
| `AbstractSmartLifecycle` | 无 | Spring 6.x 的 SmartLifecycle 仍是裸接口 |
| `@EnableP6DataSource` | 无 | Spring 6.x 无 P6Spy 自动集成 |
| `@EnableGuice` | 部分 | Spring 6.x 支持 JSR-330 `@Inject`，但不支持 Guice 的 `optional` 属性 |

Spring 6.0 将 `javax.inject` 迁移到 `jakarta.inject`，Guice 的 `@Inject`（`com.google.inject` 包）不受影响，仍然需要单独的 `BeanPostProcessor` 处理。

---

## 小结

本篇覆盖了 microsphere-spring 的六个基础设施扩展：

1. **URL Stream Handler 桥接**：`spring://` 协议将 Spring Resource/Environment/Bean 暴露为标准 URL
2. **TTL 缓存扩展**：`@TTLCacheable` + `TTLContext` ThreadLocal 传递 per-key TTL（Redis 集成尚未完成）
3. **类型转换桥接**：`SpringConverterAdapter` 将 microsphere Java SPI Converter 适配为 Spring `ConditionalGenericConverter`
4. **SmartLifecycle 扩展**：`AbstractSmartLifecycle` 模板方法 + final 状态保护
5. **P6Spy 集成**：`@EnableP6DataSource` 自动包装 DataSource，排除机制 + `unwrap` 防双重代理
6. **Guice 集成**：`@EnableGuice` 桥接 Google Guice `@Inject` 的 `optional` 属性

这六个扩展的共同特点是**小而精**--每个只解决一个具体问题，依赖前八篇的基础设施（如 `GenericBeanPostProcessorAdapter`、`AnnotatedInjectionBeanPostProcessor`、`ConversionServiceResolver`），体现了 microsphere-spring 的模块化设计理念。

至此，microsphere-spring 的 9 篇深度分析全部完成。
