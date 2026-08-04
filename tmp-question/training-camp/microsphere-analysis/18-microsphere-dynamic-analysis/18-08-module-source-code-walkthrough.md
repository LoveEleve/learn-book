# 18-08 18-dynamic 逐模块源码解析

## 目录

- [从 main() 到 DataSource 就绪的完整路径](#从-main-到-datasource-就绪的完整路径)
- [DynamicJdbcContextApplicationListener 入口分析](#dynamicjdbccontextapplicationlistener-入口分析)
- [DynamicJdbcChildContext 创建与初始化](#dynamicjdbcchildcontext-创建与初始化)
- [DynamicJdbcContextProcessor 6 步管道逐行解析](#dynamicjdbccontextprocessor-6-步管道逐行解析)
- [DynamicDataSource 完整生命周期](#dynamicdatasource-完整生命周期)
- [DynamicJdbcChildContextRefreshedListener Bean 升迁](#dynamicjdbcchildcontextrefreshedlistener-bean-升迁)
- [DynamicJdbcConfigBeanDefinitionRegistrar 注册](#dynamicjdbcconfigbeandefinitionregistrar-注册)
- [4 个 SPI 的实现逐模块对比](#4-个-spi-的实现逐模块对比)

---

## 从 main() 到 DataSource 就绪的完整路径

```
SpringApplication.run()
  │
  ├─ 1. 构造 SpringApplication
  │     ├─ 从 spring.factories 加载 ApplicationListener
  │     │   → DynamicJdbcContextApplicationListener（order=200）
  │     └─ 从 spring.factories 加载 DefaultPropertiesPostProcessor
  │         → DynamicJdbcDefaultPropertiesPostProcessor
  │
  ├─ 2. prepareEnvironment()
  │     └─ DefaultPropertiesApplicationListener 执行
  │         → 调用所有 DefaultPropertiesPostProcessor
  │         → DynamicJdbcDefaultPropertiesPostProcessor
  │           → 注册 META-INF/dynamic-jdbc/default.properties
  │
  ├─ 3. createApplicationContext()
  │     └─ new AnnotationConfigApplicationContext()
  │
  ├─ 4. prepareContext()
  │     ├─ 设置 Environment、注册 BeanFactoryPostProcessor、加载 sources
  │     └─ ApplicationPreparedEvent 发布 ← 18-dynamic 的入口！
  │         ← 注意：ApplicationPreparedEvent 不是在 refresh 中发布的！
  │         ← 是在 prepareContext 阶段（refresh 之前）发布的
  │         │
  │         └─ DynamicJdbcContextApplicationListener.onApplicationEvent()
  │               │
  │               ├─ 步骤 4.1：检查是否启用
  │               │   isDynamicJdbcEnabled(environment) → true（默认）
  │               │
  │               ├─ 步骤 4.2：读取配置
  │               │   getDynamicJdbcConfigs(environment)
  │               │   → 扫描 microsphere.dynamic.jdbc.configs.*
  │               │   → Jackson 反序列化为 DynamicJdbcConfig
  │               │
  │               ├─ 步骤 4.3：注册事件监听器
  │               │   registerPropagatingDynamicJdbcConfigChangedListener(...)
  │               │   → 监听 ZoneContextChangedEvent 和 PropertySourcesChangedEvent
  │               │
  │               ├─ 步骤 4.4：注册 ShardingSphere shutdown hook 监听器
  │               │   → 如果任何 config 有 ShardingSphere
  │               │
  │               ├─ 步骤 4.5：判断单/多 config 模式
  │               │   │
  │               │   ├─ 单 config:
  │               │   │   → DynamicJdbcContextProcessor.process() 在当前上下文执行 6 步管道
  │               │   │   → DynamicDataSource BeanDefinition 注册（refresh 之前）
  │               │   │
  │               │   └─ 多 config:
  │               │       → ThreadPoolExecutor 并行创建 N 个 DynamicJdbcChildContext
  │               │         → 每个子上下文执行 6 步管道（refresh 期间）
  │               │       → 等待所有完成 + 检查 InitializeErrors
  │               │       → 排除父上下文中的模块 Auto-Configuration
  │               │
  │               └─ DynamicDataSource（每个 config 一个）
  │                     ├─ 单 config：在父上下文 refresh 中实例化
  │                     │   → afterPropertiesSet() → 创建子上下文（启动时）
  │                     └─ 多 config：在子上下文 refresh 中实例化
  │                         → afterPropertiesSet() → 创建嵌套子上下文（启动时）
  │
  ├─ 5. refreshContext()
  │     └─ AbstractApplicationContext.refresh()
  │         │
  │         ├─ 5a. prepareRefresh()
  │         │
  │         ├─ 5b. obtainFreshBeanFactory()
  │         │
  │         ├─ 5c. prepareBeanFactory()
  │         │
  │         ├─ 5d. postProcessBeanFactory()
  │         │     └─ ServletWebServerApplicationContext 扩展（Web 环境）
  │         │
  │         ├─ 5e. invokeBeanFactoryPostProcessors()
  │         │     └─ ConfigurationClassPostProcessor 处理 @SpringBootApplication
  │         │         → @EnableAutoConfiguration → @Import(AutoConfigurationImportSelector.class)
  │         │           → ConfigurationClassParser 处理 @Import
  │         │             → AutoConfigurationImportSelector.selectImports()
  │         │               → 加载 AutoConfiguration.imports
  │         │               → DynamicJdbcAutoConfigurationImportFilter 过滤（首次扫描，缓存未命中）
  │         │               → DynamicJdbcAutoConfigurationImportListener 缓存
  │         │               → 返回过滤后的 Auto-Configuration 类名
  │         │           → 对每个 Auto-Configuration 类递归处理 @Bean、@Import
  │         │
  │         ├─ 5f. registerBeanPostProcessors()
  │         │
  │         ├─ 5g. initMessageSource()
  │         │
  │         ├─ 5h. initApplicationEventMulticaster()
  │         │
  │         ├─ 5i. onRefresh()
  │         │     └─ 内嵌 Web 服务器启动
  │         │
  │         ├─ 5j. registerListeners()
  │         │     └─ 注册 ApplicationListener → DynamicJdbcContextApplicationListener 在此注册
  │         │
  │         ├─ 5k. finishBeanFactoryInitialization()
  │         │     └─ 初始化所有非懒加载 singleton Bean
  │         │     └─ 单 config：DynamicDataSource 在此实例化
  │         │         → afterPropertiesSet() → 创建子上下文（启动时）
  │         │
  │         └─ 5l. finishRefresh()
  │               └─ 发布 ContextRefreshedEvent
  │
  └─ 6. afterRefresh()
        → ApplicationStartedEvent → ApplicationReadyEvent
```

### 关键时间点

```
T=0:    SpringApplication.run()
T=0.5:  prepareEnvironment → default.properties 加载
T=1.0:  prepareContext → ApplicationPreparedEvent
        → DynamicJdbcContextApplicationListener
        → 读取配置

        单 config 模式：
        → 6 步管道在当前上下文执行
        → DynamicDataSource BeanDefinition 注册（refresh 之前）

        多 config 模式：
        → 线程池并行创建业务子上下文
        → 每个子上下文 refresh 内执行 6 步管道

T=1.5:  refreshContext
        → 单 config：finishBeanFactoryInitialization 实例化 DynamicDataSource
          → afterPropertiesSet() → 创建 DynamicDataSource 子上下文（1-3 秒）
        → 多 config：子上下文 refresh 完成，DynamicDataSource 随子上下文初始化

T=2.0:  refresh → finishRefresh → ContextRefreshedEvent + ApplicationStartedEvent

T=3+:   第一个 HTTP 请求 → getConnection() → 连接池首次建连
```

注意：**ApplicationPreparedEvent 在 prepareContext 阶段发布（refresh 之前）**。因此单 config 的 DynamicDataSource BeanDefinition 在 refresh 前已注册，会被 `finishBeanFactoryInitialization` 实例化——两种模式的子上下文都在**启动时**创建。真正延迟到首次请求的只有 HikariCP 连接池的实际建连。

---

## DynamicJdbcContextApplicationListener 入口分析

### 类层次

```
ApplicationListener<ApplicationPreparedEvent>
  └─ OnceApplicationPreparedEventListener
       └─ OnceMainApplicationPreparedEventListener
            └─ DynamicJdbcContextApplicationListener
```

| 类 | 职责 |
|---|------|
| `ApplicationListener` | Spring 标准事件监听接口 |
| `OnceApplicationPreparedEventListener` | 防重复处理（ConcurrentSkipListSet） |
| `OnceMainApplicationPreparedEventListener` | 过滤 bootstrap 上下文 |
| `DynamicJdbcContextApplicationListener` | 真正的逻辑：创建子上下文 |

### onApplicationEvent 方法

```java
@Override
protected void onApplicationEvent(SpringApplication springApplication,
        String[] args, ConfigurableApplicationContext context) {

    if (isDisable(context)) {
        logger.debug("Current ApplicationContext[id : {}] disable Dynamic JDBC", context.getId());
        return;
    }

    processDynamicJdbcContext(context);
}
```

### isDisable 检查

```java
private boolean isDisable(ConfigurableApplicationContext context) {
    return !isDynamicJdbcEnabled(context.getEnvironment());
}

// DynamicJdbcPropertyUtils.isDynamicJdbcEnabled
public static boolean isDynamicJdbcEnabled(Environment environment) {
    return environment.getProperty(
        DYNAMIC_JDBC_ENABLED_PROPERTY_NAME,  // "microsphere.dynamic.jdbc.enabled"
        Boolean.TYPE,
        DEFAULT_DYNAMIC_JDBC_ENABLED_PROPERTY_VALUE  // true
    );
}
```

### processDynamicJdbcContext 方法

```java
private void processDynamicJdbcContext(ConfigurableApplicationContext context) {
    ConfigurableEnvironment environment = context.getEnvironment();

    // 1. 读取所有 DynamicJdbcConfig 配置
    Map<String, DynamicJdbcConfig> dynamicJdbcConfigs = getDynamicJdbcConfigs(environment);

    int dynamicJdbcConfigSize = dynamicJdbcConfigs.size();

    if (dynamicJdbcConfigSize == 0) {
        logger.info("No DynamicJdbcConfig was configured ...");
        return;
    }

    // 2. 注册 Zone 变更监听器
    registerPropagatingDynamicJdbcConfigChangedEventListener(dynamicJdbcConfigs, context);

    // 3. 注册 ShardingSphere shutdown hook 同步执行监听器
    registerSyncExecutionShutdownHookApplicationListener(dynamicJdbcConfigs, context);

    // 4. 判断单 config 还是多 config
    boolean multiple = dynamicJdbcConfigSize > 1;

    if (multiple) {
        // 多 config → 线程池并行创建子上下文
        processDynamicJdbcChildContexts(dynamicJdbcConfigs.entrySet(), context);
    } else {
        // 单 config → 在当前上下文执行 6 步管道
        Map.Entry<String, DynamicJdbcConfig> entry =
                dynamicJdbcConfigs.entrySet().iterator().next();
        processDynamicJdbcContext(entry, context);
    }
}
```

### 单 config vs 多 config 的关键区别

**单 config**：
- 在 `ApplicationPreparedEvent`（prepareContext 阶段，refresh 之前）执行 6 步管道
- `DynamicJdbcContextProcessor.process()` 在根上下文中注册 Bean
- 不创建业务子上下文，不涉及 Bean 升迁
- DynamicDataSource BeanDefinition 在 refresh 前注册 → `finishBeanFactoryInitialization` 实例化 → `afterPropertiesSet()` 创建自己的子上下文（启动时完成）

**多 config**：
- 每个 config 创建一个 `DynamicJdbcChildContext`
- 线程池并行初始化
- 每个子上下文独立执行 6 步管道
- 子上下文 Bean 升迁到父上下文
- 父上下文排除模块 Auto-Configuration

### 多 config 的错误处理

```java
private void processDynamicJdbcChildContexts(
        Set<Map.Entry<String, DynamicJdbcConfig>> configEntrySet,
        ConfigurableApplicationContext context) {

    int parallelism = configEntrySet.size();
    ThreadPoolExecutor executorService = (ThreadPoolExecutor) newFixedThreadPool(parallelism);
    InitializeErrors initializeErrors = new InitializeErrors();

    // 提交所有任务
    for (Map.Entry<String, DynamicJdbcConfig> entry : configEntrySet) {
        executorService.execute(() -> {
            try {
                initializeDynamicJdbcChildContext(entry, context);
            } catch (Throwable t) {
                initializeErrors.addError(entry.getKey(), t);
                logger.error("Initialize Dynamic-JDBC failed. DynamicJdbcConfig:[{}]",
                        entry.getKey(), t);
            }
        });
    }

    // 等待所有完成
    boolean terminated = false;
    long completedTaskCount = 0;
    while (!terminated) {
        try {
            terminated = executorService.awaitTermination(1, TimeUnit.SECONDS);
            completedTaskCount = executorService.getCompletedTaskCount();
            if (completedTaskCount == parallelism) {
                break;
            }
        } catch (InterruptedException e) {
            terminated = true;
        }
    }

    // 如果有任何错误，一次性抛出
    if (initializeErrors.hasError()) {
        throw new DynamicJdbcInitializeException(initializeErrors.toString());
    }

    // 排除父上下文中的模块 Auto-Configuration
    appendExclusionAutoConfigurationProperty(context);

    executorService.shutdownNow();
}
```

### initializeDynamicJdbcChildContext

```java
private void initializeDynamicJdbcChildContext(
        Map.Entry<String, DynamicJdbcConfig> entry,
        ConfigurableApplicationContext parentContext) {

    DynamicJdbcConfig config = entry.getValue();
    String propertyName = entry.getKey();

    // 创建子上下文
    DynamicJdbcChildContext childContext = new DynamicJdbcChildContext(
            config, propertyName, parentContext);

    // 注册父上下文 Bean（子上下文需要）
    childContext.registerParentBeans();    // 设标志：升迁时注册到父上下文
    childContext.mergeParentEnvironment(); // 合并父 Environment
    childContext.refresh();                // 执行完整生命周期 → 6 步管道在这里运行
}
```

---

## DynamicJdbcChildContext 创建与初始化

### 构造函数

```java
public class DynamicJdbcChildContext extends AnnotationConfigApplicationContext {

    protected final DynamicJdbcConfig dynamicJdbcConfig;
    protected final String dynamicJdbcConfigPropertyName;
    protected final ConfigurableApplicationContext parentContext;
    private boolean registerParentBeans = false;

    // 构造函数 A：自定义 IdGenerator
    public DynamicJdbcChildContext(DynamicJdbcConfig config, String propertyName,
            ConfigurableApplicationContext parentContext,
            DynamicJdbcChildContextIdGenerator generator) {
        this.dynamicJdbcConfig = config;
        this.dynamicJdbcConfigPropertyName = propertyName;
        this.parentContext = parentContext;
        String id = generator.generate(config, propertyName, parentContext);
        this.setId(id);
    }

    // 构造函数 B：默认 IdGenerator
    protected DynamicJdbcChildContext(DynamicJdbcConfig config, String propertyName,
            ConfigurableApplicationContext parentContext) {
        this(config, propertyName, parentContext, DynamicJdbcChildContextIdGenerator.DEFAULT);
    }
}
```

`AnnotationConfigApplicationContext` 的无参构造在隐式调用 `super()` 时会创建：
1. `AnnotatedBeanDefinitionReader ` — 用于注册 `@Configuration` 类和 `@Bean` 方法
2. `ClassPathBeanDefinitionScanner` — 用于扫描 `@ComponentScan` 路径

但 18-dynamic 的子上下文不使用默认的 Scanner（不使用组件扫描），只使用 Reader。

### context ID 的生成

```java
// DynamicJdbcChildContextIdGenerator.DEFAULT
public interface DynamicJdbcChildContextIdGenerator {
    DynamicJdbcChildContextIdGenerator DEFAULT = new DynamicJdbcChildContextIdGenerator() {};

    default String generate(DynamicJdbcConfig config, String propertyName,
            ConfigurableApplicationContext parentContext) {
        return generateDynamicJdbcChildContextId(config);
    }
}

// DynamicJdbcConfigUtils
public static String generateDynamicJdbcChildContextId(DynamicJdbcConfig config) {
    return DYNAMIC_JDBC_CHILD_CONTEXT_ID_PREFIX    // "DynamicJdbcChildContext["
            + config.getName()
            + DYNAMIC_JDBC_CHILD_CONTEXT_ID_SUFFIX;  // "]"
}
```

ID 示例：`DynamicJdbcChildContext[orders]`

DynamicDataSource 创建子上下文时使用自定义 IdGenerator：

```java
private static final DynamicJdbcChildContextIdGenerator idGenerator = (config, propertyName, parentContext) -> {
    return DynamicJdbcConfigUtils.generateDynamicDataSourceDynamicJdbcChildContextId(config);
    // → "Dynamic#DynamicJdbcChildContext[orders]#datasource"
};
```

### postProcessBeanFactory

```java
@Override
public final void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);

    // 1. 设置 ClassLoader（继承父上下文）
    setClassLoader(parentContext.getClassLoader());

    // 2. 准备 Environment
    prepareEnvironment(getEnvironment());

    // 3. 注册子上下文刷新监听器（Bean 升迁）
    addApplicationListener(new DynamicJdbcChildContextRefreshedListener(
            dynamicJdbcConfig, parentContext, beanFactory, registerParentBeans));

    // 4. 注册配置类
    registerConfigurationClasses();

    // 5. 定制 BeanFactory（子类可重写，当前为空）
    customizeBeanFactory(beanFactory);

    // 6. 执行 6 步管道
    processDynamicJdbcChildContext();
}
```

### prepareEnvironment

```java
protected void prepareEnvironment(ConfigurableEnvironment environment) {
    // For Spring Boot 2 Binder
    ConfigurationPropertySources.attach(getEnvironment());
}
```

这一行代码很关键。`ConfigurationPropertySources.attach()` 是 Spring Boot 2.x 引入的机制，它将 `ConfigurableEnvironment` 中的所有 `PropertySource` 包装为一个新的 `ConfigurationPropertySourcesPropertySource`，附加到 Environment 中。这样可以使 Spring Boot 的 `Binder` API 正常工作。

### registerConfigurationClasses

```java
private void registerConfigurationClasses() {
    List<Class<?>> configurationClasses = new LinkedList<>();
    setupConfigurationClasses(configurationClasses);
    configurationClasses.forEach(this::register);
}

protected void setupConfigurationClasses(List<Class<?>> configurationClasses) {
    configurationClasses.add(DynamicJdbcChildContextConfiguration.class);
}
```

只注册一个配置类：

```java
@EnableDynamicJdbcAutoConfiguration
public class DynamicJdbcChildContextConfiguration {}
```

`@EnableDynamicJdbcAutoConfiguration` 通过 `@Import(DynamicJdbcAutoConfigurationImportSelector.class)` 引入 Auto-Configuration 导入逻辑。

---

## DynamicJdbcContextProcessor 6 步管道逐行解析

### 入口

```java
void process(DynamicJdbcConfig dynamicJdbcConfig,
             String dynamicJdbcConfigPropertyName,
             ConfigurableApplicationContext context) {

    // Step 1: 注册注解配置处理器
    registerAnnotationConfigProcessors(context);

    // Step 2: Post-Process DynamicJdbcConfig（配置后处理）
    postProcessDynamicJdbcConfig(dynamicJdbcConfig, dynamicJdbcConfigPropertyName, context);

    // Step 3: 验证配置
    validateDynamicJdbcConfig(dynamicJdbcConfig, dynamicJdbcConfigPropertyName, context);

    // Step 4: 如果启用了动态，处理动态模块
    if (dynamicJdbcConfig.isDynamic()) {
        processDynamic(dynamicJdbcConfig, dynamicJdbcConfigPropertyName, context);
    }

    // Step 5: 合成配置属性到 Environment
    processDynamicJdbcConfigurationProperties(dynamicJdbcConfig, dynamicJdbcConfigPropertyName, context);

    // Step 6: 注册 Bean 定义
    registerDynamicJdbcConfigBeanDefinitions(dynamicJdbcConfig, dynamicJdbcConfigPropertyName, context);
}
```

### 第 1 步：registerAnnotationConfigProcessors

```java
private void registerAnnotationConfigProcessors(ConfigurableApplicationContext context) {
    BeanDefinitionRegistry registry = resolveBeanDefinitionRegistry(context);
    AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);
}
```

注册的处理器：

```java
AnnotationConfigUtils.registerAnnotationConfigProcessors(registry)
  → ConfigurationClassPostProcessor（处理 @Configuration, @Import, @Bean）
  → AutowiredAnnotationBeanPostProcessor（处理 @Autowired, @Value）
  → CommonAnnotationBeanPostProcessor（处理 @Resource, @PostConstruct, @PreDestroy）
  → EventListenerMethodProcessor（处理 @EventListener）
  → DefaultEventListenerFactory
```

这些处理器在子上下文中是必需的——因为子上下文是全新的 BeanFactory，没有任何基础设施 Bean。

### 第 2 步：postProcessDynamicJdbcConfig

```java
private void postProcessDynamicJdbcConfig(DynamicJdbcConfig config,
        String propertyName, ConfigurableApplicationContext context) {
    // 通过 SpringFactoriesLoader 加载所有 ConfigPostProcessor 实现
    List<ConfigPostProcessor> processors = getConfigPostProcessors(context);

    // 遍历执行
    processors.forEach(processor ->
        processor.postProcess(config, propertyName));
}
```

6 个实现会在第 4 步之前修改 DynamicJdbcConfig 对象：

| 实现 | 顺序 | 作用 |
|------|------|------|
| `DynamicJdbcConfigPostProcessor` | 1 | 设置 name（如果未设，从 propertyName 派生），调用 `invokeAwareInterfaces` |
| `DataSourcePropertiesConfigPostProcessor` | 2 | 填充每个 DataSource 的属性默认值（name, type, url, driver, username, password） |
| `TransactionConfigPostProcessor` | 3 | 空操作 |
| `ShardingSphereConfigPostProcessor` | 4 | 空操作 |
| `MybatisConfigPostProcessor` | 5 | 空操作 |
| `MybatisPlusConfigPostProcessor` | 6 | 空操作 |

### 第 3 步：validateDynamicJdbcConfig

```java
private void validateDynamicJdbcConfig(DynamicJdbcConfig config,
        String propertyName, ConfigurableApplicationContext context)
        throws ConfigValidationException {

    List<ConfigValidator> validators = getConfigValidators(context);
    ValidationErrors errors = new ValidationErrors(config.getName());

    // 所有验证器遍历执行，收集所有错误
    validators.forEach(validator ->
        validator.validate(config, propertyName, errors));

    // 如果有任何错误，统一抛出
    if (!errors.isValid()) {
        throw new ConfigValidationException(errors.toString());
    }
}
```

### 第 4 步：processDynamic

```java
private void processDynamic(DynamicJdbcConfig config,
        String propertyName, ConfigurableApplicationContext context) {
    processDynamicDataSource(config, propertyName, context);
}

private void processDynamicDataSource(DynamicJdbcConfig config,
        String propertyName, ConfigurableApplicationContext context) {

    String beanName = "DynamicJdbcDynamicDataSource";

    // 注册 DynamicDataSource 的 BeanDefinition
    registerDynamicDataSourceBeanDefinition(config, propertyName, beanName, context);

    // 移除原始的 DataSource 配置
    removeDataSourceConfigs(config);
}

private void registerDynamicDataSourceBeanDefinition(DynamicJdbcConfig config,
        String propertyName, String beanName, ConfigurableApplicationContext context) {

    // 创建 DynamicDataSource 的 BeanDefinition
    BeanDefinition bd = genericBeanDefinition(DynamicDataSource.class)
            .addConstructorArgValue(cloneDynamicJdbcConfig(config))   // 深克隆
            .addConstructorArgValue(propertyName)                     // 属性名
            .addConstructorArgValue(context)                          // 当前上下文
            .getBeanDefinition();

    BeanDefinitionRegistry registry = resolveBeanDefinitionRegistry(context);
    registry.registerBeanDefinition(beanName, bd);
}

private void removeDataSourceConfigs(DynamicJdbcConfig config) {
    config.setDataSource(emptyList());              // 清空 DataSource
    config.setHighAvailabilityDataSource(emptyMap()); // 清空 HA DataSource
    config.setShardingSphere(null);                 // 清空 ShardingSphere
}
```

**为什么移除 DataSource 配置？**

因为第 5 步 Synthesizer 会将数据源属性合成为 `spring.datasource.*`，而 `DataSourceAutoConfiguration` 在看到这些属性后会创建一个 `HikariDataSource` Bean。但 DynamicDataSource 本身就是一个 DataSource（已在第 4 步注册），如果 Auto-Configuration 再创建一个 HikariDataSource，容器中会有两个 DataSource（一个 DynamicDataSource 壳，一个真正的 HikariDataSource）。移除原始配置后，Synthesizer 不会合成 `spring.datasource.*` 属性（因为 DataSource 配置已清空），`DataSourceAutoConfiguration` 不会激活，容器中只有 DynamicDataSource 一个 DataSource Bean。

**为什么包括 ShardingSphere？**

因为 ShardingSphere 也会创建 `ShardingSphereDataSource`，和 DynamicDataSource 的"动态"机制冲突。DynamicDataSource 会在自己的子上下文中使用 ShardingSphere 配置（通过复制配置文件），不需要父上下文再创建一次。

### 第 5 步：processDynamicJdbcConfigurationProperties

```java
private void processDynamicJdbcConfigurationProperties(DynamicJdbcConfig config,
        String propertyName, ConfigurableApplicationContext context) {

    // 1. 合成 DynamicJdbcConfig 的 PropertySource
    MapPropertySource source = addDynamicJdbcConfigPropertySource(
            config, propertyName, context);

    // 2. 如果是子上下文，添加 Auto-Configuration 排除属性
    if (context instanceof DynamicJdbcChildContext) {
        addExclusionAutoConfigurationPropertySource(context, source);
    }
}
```

**addDynamicJdbcConfigPropertySource**：

```java
private MapPropertySource addDynamicJdbcConfigPropertySource(DynamicJdbcConfig config,
        String propertyName, ConfigurableApplicationContext context) {

    ConfigurableEnvironment env = context.getEnvironment();
    MutablePropertySources sources = env.getPropertySources();

    // 找到配置属性名称所在的 PropertySource
    String currentSourceName = findConfiguredPropertySourceName(env, propertyName);

    // 通过 Synthesizer SPI 合成属性
    List<ConfigConfigurationPropertiesSynthesizer> synthesizers =
            getDynamicJdbcConfigurationPropertiesSynthesizers(context);
    MapPropertySource synthesizedSource = buildDynamicJdbcPropertySource(
            config, propertyName, synthesizers);

    // 合成的 PropertySource 插入到当前配置之后
    if (currentSourceName != null) {
        sources.addAfter(currentSourceName, synthesizedSource);
    } else {
        sources.addFirst(synthesizedSource);
    }

    return synthesizedSource;
}
```

**addExclusionAutoConfigurationPropertySource**：

```java
private void addExclusionAutoConfigurationPropertySource(
        ConfigurableApplicationContext context, MapPropertySource source) {

    ConfigurableEnvironment env = context.getEnvironment();
    MutablePropertySources sources = env.getPropertySources();

    // 从已合成的 PropertySource 中获取 spring.autoconfigure.exclude 的值
    Object excludeValue = source.getProperty(AUTO_CONFIGURE_EXCLUDE_PROPERTY_NAME);

    // 创建一个单独的 PropertySource，放到第一位
    String propertySourceName = generateSynthesizedPropertySourceName(
            SPRING_AUTO_CONFIGURE_EXCLUDE_PROPERTY_NAME);
    sources.addFirst(new MapPropertySource(propertySourceName,
            Collections.singletonMap(SPRING_AUTO_CONFIGURE_EXCLUDE_PROPERTY_NAME, excludeValue)));
}
```

将 `spring.autoconfigure.exclude` 放到 Environment 的第一位，确保它的优先级最高，Spring Boot 的标准排除逻辑能正确读取。

### 第 6 步：registerDynamicJdbcConfigBeanDefinitions

```java
private void registerDynamicJdbcConfigBeanDefinitions(DynamicJdbcConfig config,
        String propertyName, ConfigurableApplicationContext context) {

    List<ConfigBeanDefinitionRegistrar> registrars =
            getDynamicJdbcConfigBeanDefinitionRegistrars(context);

    BeanDefinitionRegistry registry = resolveBeanDefinitionRegistry(context);

    // 遍历所有 Registrar，注册 Bean
    registrars.forEach(registrar ->
        registrar.register(config, propertyName, registry));
}
```

5 个 Registrar 各自注册不同类型的 Bean：

| Registrar | 注册的 Bean |
|-----------|------------|
| `DynamicJdbcConfigBeanDefinitionRegistrar` | DynamicJdbcConfig 本身（Factory Bean） |
| `TransactionConfigurationConfigBeanDefinitionRegistrar` | PlatformTransactionManager alias + customizer |
| `ShardingSphereConfigurationConfigBeanDefinitionRegistrar` | ModeConfiguration + RuleConfiguration（从 YAML 解析） |
| `MybatisConfigurationConfigBeanDefinitionRegistrar` | MybatisMapperScanConfiguration |
| `MybatisPlusConfigurationConfigBeanDefinitionRegistrar` | MybatisPlusMapperScanConfiguration |

---

## DynamicDataSource 完整生命周期

### 类声明

```java
public class DynamicDataSource
        implements DataSource, InitializingBean, DisposableBean, BeanFactoryAware {
    // DataSource：对外提供 Connection
    // InitializingBean：初始化回调
    // DisposableBean：销毁回调
    // BeanFactoryAware：获取 BeanFactory（用于 ZoneContext 注入）
}
```

### afterPropertiesSet

```java
@Override
public void afterPropertiesSet() {
    if (initialized) {
        logger.debug("已经初始化过了");
        return;
    }

    // 注册监听器
    initializeApplicationListeners();

    // 懒加载：只在 delegate 为 null 时初始化
    if (null == this.delegate) {
        initializeDataSource();
    }

    initialized = true;
}
```

`afterPropertiesSet` 在 singleton Bean 实例化时被 Spring 调用。DynamicDataSource 的 BeanDefinition 在 `ApplicationPreparedEvent`（prepareContext 阶段，refresh 之前）注册，因此会在本次 refresh 的 `finishBeanFactoryInitialization` 中实例化，`afterPropertiesSet()` 在**启动时**执行并创建子上下文——单 config 和多 config 模式都是如此。

子上下文内的 HikariCP 连接池才是真正延迟到首次 `getConnection()` 才建连。

如果 `delegate != null`（例如另一线程已抢先初始化），则跳过重复初始化。

### RefreshingDynamicDataSourceListener

```java
private void initializeApplicationListeners() {
    initializeRefreshingDynamicDataSourceListener();
}

private void initializeRefreshingDynamicDataSourceListener() {
    ConfigurableApplicationContext ctx = this.context;

    if (ctx instanceof DynamicJdbcChildContext) {
        // 如果是子上下文，在父上下文注册 Listener
        DynamicJdbcChildContext childCtx = (DynamicJdbcChildContext) ctx;
        childCtx.getParentContext().addApplicationListener(
                new RefreshingDynamicDataSourceListener());
    } else {
        // 如果是根上下文（单 config 模式），在根上下文注册
        ctx.addApplicationListener(new RefreshingDynamicDataSourceListener());
    }
}
```

为什么要在父上下文注册？因为 `DynamicJdbcConfigChangedEvent` 是在父上下文中发布的（`PropagatingDynamicJdbcConfigChangedEventListener` 在父上下文中注册，事件也在父上下文中发布）。如果 DynamicDataSource 在子上下文中，而监听器注册在子上下文中，就收不到父上下文的事件。

### createDynamicDataSourceConfig

```java
protected DynamicJdbcConfig createDynamicDataSourceConfig(DynamicJdbcConfig config) {
    DynamicJdbcConfig newConfig = DynamicJdbcConfigUtils.cloneDynamicJdbcConfig(config);
    initDynamicDynamicJdbcConfig(newConfig);
    return newConfig;
}

private void initDynamicDynamicJdbcConfig(DynamicJdbcConfig config) {
    // 1. 关闭动态（防止递归）
    config.setDynamic(false);

    // 2. 移除其他模块（子上下文只包含 DataSource）
    config.setTransaction(null);
    config.setMybatis(null);
    config.setMybatisPlus(null);

    // 3. 重命名
    config.setName(DynamicJdbcConfigUtils
            .generateDynamicDataSourceDynamicJdbcConfigName(config));
    // → "Dynamic#{originalName}#datasource"

    // 4. 设置 BeanFactory（继承自父 DataSource）
    config.setBeanFactory(beanFactory);
}
```

因为 `cloneDynamicJdbcConfig` 通过 Jackson 序列化再反序列化实现深克隆，所以 `setDynamic(false)` 和 `setTransaction(null)` 等修改不会影响原始配置。

### initializeDataSource

```java
private DataSource initializeDataSource(DynamicJdbcConfig config,
        String propertyName, ConfigurableApplicationContext context) {

    // 1. 创建仅包含 DataSource 配置的子上下文配置
    DynamicJdbcConfig dsConfig = createDynamicDataSourceConfig(config);

    // 2. 创建子上下文
    DynamicJdbcChildContext childContext = new DynamicJdbcChildContext(
            dsConfig, propertyName, context, idGenerator);

    // 3. 合并父上下文 Environment
    childContext.mergeParentEnvironment();

    // 4. 刷新子上下文（执行完整 Spring 生命周期 + 6 步管道）
    childContext.refresh();

    // 5. 从子上下文获取唯一的 DataSource Bean
    DataSource latestDataSource = getDataSource(childContext);

    // 6. 原子切换
    synchronized (mutex) {
        DataSource previous = DynamicDataSource.this.delegate;
        DynamicDataSource.this.delegate = latestDataSource;

        ConfigurableApplicationContext previousChildContext =
                DynamicDataSource.this.dynamicDataSourceChildContext;
        DynamicDataSource.this.dynamicDataSourceChildContext = childContext;

        logger.info("DataSource Previous : {} , Current : {}", previous, latestDataSource);

        // 7. 异步关闭旧上下文
        closeDynamicDataSourceChildContext(previousChildContext, true);
    }

    return latestDataSource;
}
```

### getDataSource

```java
private DataSource getDataSource(ApplicationContext childContext) {
    Map<String, DataSource> dataSources = childContext.getBeansOfType(DataSource.class);

    int size = dataSources.size();
    if (size > 1) {
        throw new IllegalStateException(...);
    }

    // 获取唯一的 DataSource Bean
    return dataSources.values().iterator().next();
}
```

从子上下文获取 DataSource 时，期望只有一个 DataSource Bean。如果子上下文中出现了多个 DataSource Bean（例如配置了多个数据源模块），抛异常。

### findParentContext（三級匹配）

```java
private ConfigurableApplicationContext findParentContext(
        ConfigurableApplicationContext eventSourceContext) {

    DynamicJdbcChildContext current = this.dynamicDataSourceChildContext;
    ConfigurableApplicationContext parent = current.getParentContext();

    // Level 1: 事件源 == 父上下文
    if (Objects.equals(parent, eventSourceContext)) {
        return parent;
    }

    // Level 2: 父上下文不是 DynamicJdbcChildContext → 不匹配
    if (!(parent instanceof DynamicJdbcChildContext)) {
        return null;
    }

    // Level 3: 父上下文的父上下文 == 事件源
    if (Objects.equals(((DynamicJdbcChildContext) parent).getParentContext(), eventSourceContext)) {
        return parent;
    }

    return null;
}
```

为什么需要三级匹配？因为可能有这样的层次：

```
根上下文（main）
  └─ DynamicJdbcChildContext[configA]（子上下文 1）
       └─ DynamicJdbcChildContext[Dynamic#configA#datasource]（DynamicDataSource 创建）
```

事件源是根上下文时：
- Level 1: `parent` 是 `DynamicJdbcChildContext[configA]`，`eventSourceContext` 是根上下文 → 不匹配
- Level 3: `((DynamicJdbcChildContext) parent)` 的 `parentContext` 是根上下文 → 匹配

### destroy

```java
@Override
public void destroy() {
    // 立即关闭当前上下文
    closeDynamicDataSourceChildContext(dynamicDataSourceChildContext, false);
    // 关闭调度器
    shutdownScheduler(closeScheduler);
}
```

`destroy` 在 BeanFactory 关闭时被调用。此时应该立即关闭所有资源，所以 `async=false`。

---

## DynamicJdbcChildContextRefreshedListener Bean 升迁

### 构造函数

```java
public DynamicJdbcChildContextRefreshedListener(
        DynamicJdbcConfig config,
        ConfigurableApplicationContext parentContext,
        ConfigurableListableBeanFactory childContextBeanFactory,
        boolean registerParentBeans) {

    this.dynamicJdbcConfig = config;
    this.parentContext = parentContext;
    this.parentBeanDefinitionRegistry =
            (BeanDefinitionRegistry) parentContext.getBeanFactory();

    // 收集子上下文中的基础设施 Bean 名称
    this.infrastructureBeanNames =
            findInfrastructureBeanNames(childContextBeanFactory);

    // 加载 ParentContextBeanNameGenerator SPI
    this.parentContextBeanNameGenerators =
            loadFactories(parentContext, ParentContextBeanNameGenerator.class);

    this.registerParentBeans = registerParentBeans;

    // 读取 exposed 和 primary 配置
    this.multipleContextExposedBeanClasses =
            getMultipleContextExposedBeanClasses(parentContext);
    this.multipleContextPrimaryBeanClasses =
            getMultipleContextPrimaryBeanClasses(parentContext);
}
```

### onApplicationEvent

```java
@Override
public void onApplicationEvent(ContextRefreshedEvent event) {
    ConfigurableApplicationContext childContext =
            (ConfigurableApplicationContext) event.getApplicationContext();

    // 1. 注册父上下文关闭时的子上下文自动关闭
    registerParentContextClosedEventListener(childContext);

    // 2. 升迁 Bean 到父上下文
    registerParentBeansFromChildContext(childContext);
}

private void registerParentContextClosedEventListener(
        ConfigurableApplicationContext childContext) {
    parentContext.addApplicationListener(
            (ApplicationListener<ContextClosedEvent>) e -> {
                childContext.close();
            });
}
```

### registerParentBeansFromChildContext

```java
private void registerParentBeansFromChildContext(
        ConfigurableApplicationContext childContext) {
    if (!registerParentBeans) {
        return;
    }

    ConfigurableListableBeanFactory bf = childContext.getBeanFactory();
    DynamicJdbcConfig config = childContext.getBean(DynamicJdbcConfig.class);
    String[] beanNames = bf.getBeanDefinitionNames();

    for (String beanName : beanNames) {
        // 跳过基础设施 Bean
        if (isInfrastructureBean(beanName)) {
            continue;
        }

        Object bean = bf.getBean(beanName);
        String parentBeanName = generateParentBeanName(
                beanName, bean, config, childContext);
        registerParentBean(parentBeanName, bean);
    }
}
```

### generateParentBeanName

```java
private String generateParentBeanName(String childBeanName, Object childBean,
        DynamicJdbcConfig config, ConfigurableApplicationContext childContext) {

    String parentBeanName = null;

    // 先通过 SPI 查找
    ParentContextBeanNameGenerator generator =
            findParentContextBeanNameGenerator(childBean);

    if (generator != null) {
        parentBeanName = generator.generate(
                childBeanName, childBean, config, childContext);
    }

    // SPI 没有找到或没生成，使用默认名称
    return StringUtils.hasText(parentBeanName)
            ? parentBeanName
            : generateDefaultParentBeanName(childBeanName, childContext);
}

private String generateDefaultParentBeanName(String childBeanName,
        ConfigurableApplicationContext childContext) {
    return childContext.getId() + "$" + childBeanName;
    // → "DynamicJdbcChildContext[orders]$dataSource"
}
```

### registerParentBean

```java
private void registerParentBean(String parentBeanName, Object childBean) {
    if (isExposedBeanClass(childBean)) {
        // 直接暴露为普通 Bean
        boolean primary = isPrimaryBean(childBean);
        registerBean(parentBeanDefinitionRegistry, parentBeanName, childBean, primary);
    } else {
        // 包装为 FactoryBean 注册
        registerFactoryBean(parentBeanDefinitionRegistry, parentBeanName, childBean);
    }
}
```

**直接暴露**：DataSource、PlatformTransactionManager、SqlSessionFactory、TransactionManagerCustomizers
**FactoryBean 注册**：其他所有非基础设施 Bean

---

## DynamicJdbcConfigBeanDefinitionRegistrar 注册

```java
public class DynamicJdbcConfigBeanDefinitionRegistrar
        extends AbstractConfigBeanDefinitionRegistrar {

    @Override
    public void register(DynamicJdbcConfig config, String propertyName,
            BeanDefinitionRegistry registry) {

        String beanName = generateDynamicJdbcConfigBeanName(config, propertyName);
        registerFactoryBean(registry, beanName, config);
    }
}
```

Bean 名称生成：`DynamicJdbcConfigBean[{propertySuffix}]` 或 `DynamicJdbcConfigBean[{propertySuffix}].{name}`

---

## 4 个 SPI 的实现逐模块对比

### ConfigPostProcessor

| 模块 | 类 | 实际逻辑 |
|------|----|---------|
| 根配置 | `DynamicJdbcConfigPostProcessor` | 设置 name，调用 `invokeAwareInterfaces` |
| DataSource | `DataSourcePropertiesConfigPostProcessor` | 填充 DataSource 默认值（186 行，唯一有实际逻辑的） |
| Transaction | `TransactionConfigPostProcessor` | 空 |
| ShardingSphere | `ShardingSphereConfigPostProcessor` | 空 |
| MyBatis | `MybatisConfigPostProcessor` | 空 |
| MyBatis-Plus | `MybatisPlusConfigPostProcessor` | 空 |

### ConfigValidator

| 模块 | 类 | 验证逻辑 |
|------|----|---------|
| 根配置 | `DynamicJdbcConfigValidator` | name 非空，至少一个模块 |
| DataSource | `DataSourcePropertiesModuleValidator` | datasource/ha-datasource 互斥；HA ≥ 2 zones 含 defaultZone；name/type/driverClassName/username/password 非空；class 可加载；无重复 name/URL |
| Transaction | `TransactionConfigValidator` | bean name 不重复，customizer class 可加载 |
| ShardingSphere | `ShardingSphereConfigValidator` | config-resource 可解析 |
| MyBatis | `MybatisConfigValidator` | mybatis 和 mybatis-plus 互斥 |
| MyBatis-Plus | `MybatisPlusConfigValidator` | 同上 |

### ConfigConfigurationPropertiesSynthesizer

| 模块 | 类 | 合成的属性 |
|------|----|-----------|
| DataSource | `DataSourceConfigurationPropertiesSynthesizer` | `spring.datasource.*` + `spring.datasource.hikari.*`/`tomcat.*`/`dbcp2.*`（仅非 ShardingSphere + 单数据源） |
| Transaction | `TransactionConfigConfigurationPropertiesSynthesizer` | 无实际合成（仅处理排除） |
| ShardingSphere | `ShardingSphereConfigConfigurationPropertiesSynthesizer` | `spring.shardingsphere.datasource.*` + `spring.shardingsphere.props.*`（从 YAML 提取） |
| MyBatis | `MybatisConfigConfigurationPropertiesSynthesizer` | `mybatis.base-packages` |
| MyBatis-Plus | `MybatisPlusConfigConfigurationPropertiesSynthesizer` | `mybatis-plus.base-packages` |

### ConfigBeanDefinitionRegistrar

| 模块 | 类 | 注册的 Bean |
|------|----|------------|
| 根配置 | `DynamicJdbcConfigBeanDefinitionRegistrar` | DynamicJdbcConfig 自身 |
| Transaction | `TransactionConfigurationConfigBeanDefinitionRegistrar` | PlatformTransactionManager alias + customizer |
| ShardingSphere | `ShardingSphereConfigurationConfigBeanDefinitionRegistrar` | ModeConfiguration + RuleConfiguration（YAML 解析） |
| MyBatis | `MybatisConfigurationConfigBeanDefinitionRegistrar` | MybatisMapperScanConfiguration（base-packages 扫描） |
| MyBatis-Plus | `MybatisPlusConfigurationConfigBeanDefinitionRegistrar` | MybatisPlusMapperScanConfiguration（base-packages 扫描） |
