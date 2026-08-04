# 04-04 Environment 扩展与配置元数据

## 问题：Spring Boot 的默认属性和配置元数据管理不够灵活

Spring Boot 的 `SpringApplication.setDefaultProperties()` 提供了默认属性机制，但存在局限：

| 痛点 | Spring Boot 的现状 | 问题 |
|------|------------------|------|
| **默认属性来源单一** | `SpringApplication.setDefaultProperties()` 只接受一个 `Properties` | 无法从多个来源（classpath 资源、SPI、框架默认值）合并 |
| **默认属性不可扩展** | 无 SPI | 框架开发者无法注入自己的默认属性 |
| **属性来源不可追踪** | `OriginTrackedMapPropertySource` 只对 `application.properties` 生效 | 通过 `@PropertySource` 或编程式添加的属性源没有 Origin 信息 |
| **配置元数据不可查询** | `spring-configuration-metadata.json` 只在 IDE 插件中使用 | 运行时无法查询"有哪些配置项、默认值是什么、类型是什么" |

microsphere-spring-boot 通过 **DefaultPropertiesPostProcessor SPI + PropertySourceLoaders 复合加载器 + OriginTracked 初始化器 + 配置元数据仓库** 四层设计解决这些问题。

---

## 设计：四个扩展组件

### 整体架构

```
SpringApplication.run()
    │
    │  ─── environmentPrepared() ───
    │
    ├── DefaultPropertiesApplicationListener (ApplicationListener)
    │       ├── 从 Environment 读取现有 defaultProperties
    │       ├── 加载 DefaultPropertiesPostProcessor SPI（spring.factories）
    │       │       └── SpringApplicationDefaultPropertiesPostProcessor
    │       │               └── 添加 classpath*:/META-INF/config/default/*.* 资源模式
    │       ├── 用 PropertySourceLoaders 加载所有资源
    │       └── 合并到 SpringApplication.setDefaultProperties()
    │
    │  ─── freezeConfiguration() ─── (03-spring BeanFactoryListener)
    │
    ├── OriginTrackedConfigurationPropertyInitializer
    │       ├── 实现 ConfigurableApplicationContextInitializer + BeanFactoryListenerAdapter
    │       ├── onBeanFactoryConfigurationFrozen:
    │       │       遍历所有 PropertySource
    │       │       将非 OriginTracked 的替换为 OriginTrackedMapPropertySource
    │       └── 为每个属性值附加 Origin（来源 PropertySource 名称）
    │
    │  ─── CommandLineRunner ───
    │
    └── ConfigurationMetadataRepository
            ├── ConfigurationMetadataReader 读取 classpath:
            │       META-INF/spring-configuration-metadata.json
            │       META-INF/additional-spring-configuration-metadata.json
            ├── 解析为 ItemMetadata（group/property/hint）
            └── 暴露为 Bean，供 Actuator 端点查询
```

---

### DefaultPropertiesApplicationListener：多来源默认属性合并

#### 触发时机

`ApplicationEnvironmentPreparedEvent`--Environment 已准备好但 ApplicationContext 未创建。此时可以修改 `SpringApplication` 的默认属性，影响后续所有 Bean 的属性解析。

#### 处理流程

```java
public class DefaultPropertiesApplicationListener
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        SpringApplication springApplication = event.getSpringApplication();
        processDefaultProperties(environment, springApplication);
    }

    private void processDefaultProperties(ConfigurableEnvironment environment,
                                          SpringApplication springApplication) {
        // 1. 从 Environment 读取现有 defaultProperties
        Map<String, Object> defaultProperties = getDefaultProperties(environment);

        // 2. 通过 SPI 后处理
        postProcessDefaultProperties(springApplication, defaultProperties);

        // 3. 日志记录
        logDefaultProperties(springApplication, defaultProperties);
    }
}
```

**步骤 1**：`getDefaultProperties(environment)` 从 Environment 中名为 `"defaultProperties"` 的 PropertySource 提取属性。这是 Spring Boot 标准的默认属性源名称。

**步骤 2**：`postProcessDefaultProperties` 加载所有 `DefaultPropertiesPostProcessor` 实现（通过 `spring.factories`），按 `Ordered` 排序后依次调用：

```java
private void postProcessDefaultProperties(SpringApplication springApplication,
                                           Map<String, Object> defaultProperties) {
    ResourceLoader resourceLoader = getResourceLoader(springApplication);
    PropertySourceLoaders propertySourceLoaders = new PropertySourceLoaders(resourceLoader);

    // 加载所有 DefaultPropertiesPostProcessor
    List<DefaultPropertiesPostProcessor> postProcessors =
        loadFactories(DefaultPropertiesPostProcessor.class, classLoader);

    // 收集资源路径
    Set<String> defaultPropertiesResources = SpringApplicationUtils.getDefaultPropertiesResources();
    for (DefaultPropertiesPostProcessor postProcessor : postProcessors) {
        postProcessor.initializeResources(defaultPropertiesResources);
    }

    // 加载资源文件，合并到 defaultProperties
    for (String resource : defaultPropertiesResources) {
        PropertySource<?> propertySource = propertySourceLoaders.load(resource);
        // 合并...
    }

    // 后处理
    for (DefaultPropertiesPostProcessor postProcessor : postProcessors) {
        postProcessor.postProcess(defaultProperties);
    }

    // 设置回 SpringApplication
    springApplication.setDefaultProperties(defaultProperties);
}
```

#### DefaultPropertiesPostProcessor SPI

```java
public interface DefaultPropertiesPostProcessor extends Ordered {
    void initializeResources(Set<String> defaultPropertiesResources);
    default void postProcess(Map<String, Object> defaultProperties) { }
    default int getOrder() { return LOWEST_PRECEDENCE; }
}
```

两个扩展点：
- `initializeResources`：添加资源路径（如 `classpath*:/META-INF/config/default/*.*`）
- `postProcess`：直接修改属性 Map（如添加框架级默认值）

#### SpringApplicationDefaultPropertiesPostProcessor

默认实现，注册在主 `spring.factories` 中：

```java
public class SpringApplicationDefaultPropertiesPostProcessor implements DefaultPropertiesPostProcessor {

    public static final String DEFAULT_PROPERTIES_RESOURCES_PATTERN =
        CLASSPATH_ALL_URL_PREFIX + "/META-INF/config/default/*.*";

    @Override
    public void initializeResources(Set<String> defaultPropertiesResources) {
        defaultPropertiesResources.add(DEFAULT_PROPERTIES_RESOURCES_PATTERN);
    }
}
```

添加 `classpath*:/META-INF/config/default/*.*` 通配符，自动加载所有 Jar 中 `META-INF/config/default/` 目录下的配置文件。这意味着**框架开发者可以在自己的 Jar 中打包默认配置**，无需用户手动声明。

**与 `spring.factories` 的配合**：`SpringApplicationUtils` 维护一个 static `Set<String> defaultPropertiesResources`，允许其他组件在启动早期通过 `SpringApplicationUtils.addDefaultPropertiesResource()` 注册额外的资源路径。`DefaultPropertiesApplicationListener` 在处理时合并 SPI 注册的路径和 static 注册的路径。

---

### PropertySourceLoaders：复合属性源加载器

#### 设计

Spring Boot 的 `PropertySourceLoader` 接口支持特定格式（`.properties`/`.yml`/`.yaml`）。`PropertySourceLoaders` 是**复合加载器**，聚合所有 `PropertySourceLoader` 实现：

```java
public class PropertySourceLoaders implements PropertySourceLoader {

    private final List<PropertySourceLoader> loaders;

    public PropertySourceLoaders(ResourceLoader resourceLoader) {
        this.loaders = loadFactories(PropertySourceLoader.class, resourceLoader.getClassLoader());
    }

    @Override
    public String[] getFileExtensions() {
        // 聚合所有子加载器的扩展名
        Set<String> extensions = newLinkedHashSet();
        for (PropertySourceLoader loader : loaders) {
            extensions.addAll(Arrays.asList(loader.getFileExtensions()));
        }
        return extensions.toArray(EMPTY_STRING_ARRAY);
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        // 根据文件扩展名路由到子加载器
        String extension = getExtension(resource.getFilename());
        for (PropertySourceLoader loader : loaders) {
            if (supports(loader, extension)) {
                return loader.load(name, resource);
            }
        }
        return emptyList();
    }
}
```

**价值**：`DefaultPropertiesApplicationListener` 可以加载任意格式的默认属性文件（`.properties`、`.yml`、`.json`），无需关心具体格式。`PropertySourceLoaders` 根据文件扩展名自动路由到正确的子加载器。

---

### OriginTrackedConfigurationPropertyInitializer：属性来源追踪

#### 问题

Spring Boot 的 `OriginTrackedMapPropertySource` 为 `application.properties`/`.yml` 提供了 Origin 追踪--每个属性值都知道自己来自哪个文件的第几行。但通过 `@PropertySource` 或编程式添加的 `MapPropertySource`/`ResourcePropertySource` 没有 Origin 信息。

没有 Origin 信息意味着：Actuator 的 `/actuator/configprops` 端点无法显示属性来源，调试时无法追踪"这个属性值从哪里来"。

#### 设计

`OriginTrackedConfigurationPropertyInitializer` 同时实现 `ConfigurableApplicationContextInitializer`（03-spring 第 5 篇）和 `BeanFactoryListenerAdapter`（03-spring 第 1 篇）：

```java
public class OriginTrackedConfigurationPropertyInitializer
        extends ConfigurableApplicationContextInitializer implements BeanFactoryListenerAdapter {

    @Override
    protected void initialize(ConfigurableApplicationContext context, ConfigurableEnvironment environment) {
        this.applicationContext = context;
        this.propertySourceLoaders = new PropertySourceLoaders(context.getClassLoader());
    }

    @Override
    public void onBeanFactoryConfigurationFrozen(ConfigurableListableBeanFactory beanFactory) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        initializePropertySources(propertySources);
    }
}
```

**触发时机**：`onBeanFactoryConfigurationFrozen`--03-spring 第 1 篇定义的"配置冻结"回调。此时所有 BeanDefinition 已注册完毕，PropertySource 结构稳定，可以安全替换。

**替换逻辑**：

```java
void initializePropertySources(MutablePropertySources propertySources) {
    for (PropertySource propertySource : propertySources) {
        if (isPropertySourceCandidate(propertySource)) {
            String name = propertySource.getName();
            PropertySource originTracked = createOriginTrackedPropertySource(propertySource);
            propertySources.replace(name, originTracked);
        }
    }
}

private boolean isPropertySourceCandidate(PropertySource propertySource) {
    // 只处理 EnumerablePropertySource 且未实现 OriginLookup 的
    return (propertySource instanceof EnumerablePropertySource<?>)
            && !(propertySource instanceof OriginLookup);
}
```

**候选条件**：
- 必须是 `EnumerablePropertySource`（可遍历属性名，否则无法逐个包装）
- 不能已实现 `OriginLookup`（已支持 Origin 的不需要替换）

**替换策略**：

- **`ResourcePropertySource`**：通过 `PropertySourceLoaders.reloadAsOriginTracked()` 重新加载，保留文件名和行号作为 Origin
- **其他 `EnumerablePropertySource`**：创建 `OriginTrackedMapPropertySource`，每个属性值用 `OriginTrackedValue.of(value, new NamedOrigin(propertySource.getName()))` 包装，Origin 为 PropertySource 名称

**与 03-spring 的跨模块呼应**：这是 03-spring 第 1 篇的 `BeanFactoryListener` 体系在 Spring Boot 中的实际应用。`onBeanFactoryConfigurationFrozen` 回调由 03-spring 的 `EventPublishingBeanBeforeProcessor` -> `Initializer` 触发，`OriginTrackedConfigurationPropertyInitializer` 利用这个时机替换 PropertySource。整个链路：03-spring 的 BeanFactory 生命周期监听 -> Spring Boot 的属性来源追踪。

**启用控制**：`OriginTrackedConfigurationPropertyInitializer` 继承 `ConfigurableApplicationContextInitializer`，默认启用（`getDefaultEnabled()` 返回 `true`）。可通过属性 `microsphere.spring.context-initializer.{beanName}.enabled=false` 禁用。这与 03-spring 的 `ListenableConfigurableEnvironmentInitializer`（默认禁用）不同--Origin 追踪被认为是安全的基础设施，默认启用。

---

### ConfigurationMetadataRepository：配置元数据仓库

#### 设计

Spring Boot 的配置元数据（`spring-configuration-metadata.json`）通常只在 IDE 中使用（提供自动补全和文档提示）。microsphere 将其加载到运行时，暴露为可查询的 Bean：

```java
public class ConfigurationMetadataRepository implements CommandLineRunner {

    private final ConfigurationMetadataReader configurationMetadataReader;
    private Map<String, ItemMetadata> namedGroups;      // 配置组
    private Map<String, ItemMetadata> namedProperties;   // 配置属性
    private Map<String, List<ItemHint>> namedHints;      // 属性提示

    @Override
    public void run(String... args) {
        ConfigurationMetadata metadata = configurationMetadataReader.read();
        // 解析 metadata 到三个 Map
    }
}
```

#### ConfigurationMetadataReader

```java
public class ConfigurationMetadataReader implements ResourceLoaderAware {

    public static final String METADATA_PATH =
        CLASSPATH_ALL_URL_PREFIX + "/META-INF/spring-configuration-metadata.json";
    public static final String ADDITIONAL_METADATA_PATH =
        CLASSPATH_ALL_URL_PREFIX + "/META-INF/additional-spring-configuration-metadata.json";

    public ConfigurationMetadata read() {
        ConfigurationMetadata metadata = new ConfigurationMetadata();
        readMetadata(metadata, METADATA_PATH);
        readMetadata(metadata, ADDITIONAL_METADATA_PATH);
        return metadata;
    }
}
```

读取 classpath 中所有 `spring-configuration-metadata.json` 和 `additional-spring-configuration-metadata.json`，用 Spring Boot 的 `JsonMarshaller` 反序列化，合并为一个 `ConfigurationMetadata`。

#### 使用场景

- **Actuator 端点**（第 5 篇）：`ConfigurationMetadataEndpoint` 和 `ConfigurationPropertiesEndpoint` 从 Repository 查询元数据，通过 `/actuator/configMetadata` 暴露
- **配置文档生成**：运行时查询所有配置项的名称、类型、默认值、描述
- **配置校验**：检查用户配置的属性名是否在元数据中存在（检测拼写错误）

**注册条件**：`ConfigurationMetadataRepository` 的类定义在 core 模块，但 Bean 注册在 Actuator 模块的 `ActuatorEndpointsAutoConfiguration.ConfigurationProcessorConfiguration` 中，且仅当 `@ConditionalOnConfigurationProcessorPresent`（classpath 中有 `ConfigurationMetadata` 类）时才注册。这意味着**只有引入 microsphere-spring-boot-actuator 且 classpath 中有 spring-boot-configuration-processor 时**，配置元数据才会在运行时加载。

---

## 永恒原理

### 1. 默认属性的优先级层次

Spring Boot 的属性优先级从高到低：

```
命令行参数 > 系统属性 > 环境变量 > application-{profile}.yml > application.yml > defaultProperties
```

`defaultProperties` 是优先级最低的属性源--只在没有其他来源提供值时才生效。microsphere 的 `DefaultPropertiesApplicationListener` 向 `defaultProperties` 合并多来源默认值，确保框架默认值**不覆盖用户配置**，只填充用户未配置的属性。

这是"约定优于配置"的底层实现：框架提供合理默认值，用户可以覆盖任何默认值。

### 2. 复合加载器与扩展名路由

`PropertySourceLoaders` 是复合模式的经典应用：聚合多个子加载器，按文件扩展名路由。这与 03-spring 第 7 篇的 `SmartWebEndpointMappingFactory`（按端点类型路由到子工厂）设计理念一致。

复合加载器的优势是**对调用方透明**：`DefaultPropertiesApplicationListener` 不需要知道资源是 `.properties` 还是 `.yml`，只需调用 `propertySourceLoaders.load(name, resource)`，由复合加载器自动路由。

### 3. Origin 追踪与 BeanFactoryListener 的跨模块复用

`OriginTrackedConfigurationPropertyInitializer` 是 03-spring 基础设施在 Spring Boot 中的直接应用：

```
03-spring: BeanFactoryListener.onBeanFactoryConfigurationFrozen()
    └── Spring Boot: OriginTrackedConfigurationPropertyInitializer
            └── 替换 PropertySource 为 OriginTrackedMapPropertySource
```

这验证了 03-spring 的设计目标：`BeanFactoryListener` 提供"配置冻结后、实例化前"的时机，让上层模块安全地修改容器状态。Origin 追踪需要在这个时机执行，因为：
- 此时所有 `@PropertySource` 已加载，PropertySource 结构稳定
- Bean 实例化尚未开始，替换 PropertySource 不会影响已创建的 Bean

### 4. 配置元数据的运行时利用

Spring Boot 的配置元数据原本是编译时产物（由 `spring-boot-configuration-processor` 生成），只在 IDE 中使用。microsphere 将其加载到运行时，使配置成为"一等公民"--可查询、可校验、可文档化。

这是"元数据驱动"思想的体现：不仅用元数据描述代码（如 Java 反射），还用元数据描述配置。配置元数据回答了"有哪些配置项、什么类型、默认值是什么"的问题，使得运行时配置审计和校验成为可能。

---

## 边界与反例

### 1. DefaultPropertiesApplicationListener 的执行顺序

`DEFAULT_ORDER = LOWEST_PRECEDENCE - 1`，接近最低优先级。如果其他 `ApplicationListener<ApplicationEnvironmentPreparedEvent>` 在更早的顺序中修改了 `defaultProperties`，microsphere 的合并可能覆盖之前的修改。

**缓解**：microsphere 的处理是"合并"而非"替换"--只添加新属性，不覆盖已有属性。但如果其他 Listener 设置了同名属性，microsphere 不会覆盖它。

### 2. PropertySourceLoaders 的扩展名冲突

如果两个 `PropertySourceLoader` 支持同一扩展名（如 `.yml`），`PropertySourceLoaders` 会使用第一个匹配的加载器（按 `spring.factories` 中的顺序）。第二个加载器被忽略。

**缓解**：Spring Boot 的 `spring.factories` 中通常只有一个 `.yml` 加载器（`YamlPropertySourceLoader`），不会冲突。但如果用户注册了自定义 YAML 加载器，可能导致行为不一致。

### 3. OriginTrackedConfigurationPropertyInitializer 的替换风险

替换 PropertySource 是**原地替换**（`propertySources.replace(name, newSource)`）。如果在替换期间有其他线程读取属性（如配置中心推送触发的异步刷新），可能读到不一致的中间状态。

**缓解**：`onBeanFactoryConfigurationFrozen` 在 `preInstantiateSingletons` 之前执行，此时只有单线程操作。但在配置刷新场景（Spring Cloud `@RefreshScope`）中，如果刷新触发了新的 `BeanFactoryPostProcessor`，可能在多线程环境中替换 PropertySource。

### 4. ConfigurationMetadataRepository 的 CommandLineRunner 时序

`ConfigurationMetadataRepository` 实现 `CommandLineRunner`，在所有 Bean 初始化完成后执行。这意味着在 `CommandLineRunner.run()` 之前，Repository 的数据为空。如果其他组件在 `SmartInitializingSingleton` 阶段尝试查询元数据，会得到空结果。

**缓解**：改用 `InitializingBean.afterPropertiesSet()` 可以更早地加载元数据。但当前实现选择了 `CommandLineRunner`，可能是为了确保所有 `ConfigurationMetadataReader` 的依赖（如 `ResourceLoader`）已就绪。

### 5. SpringApplicationUtils 的 static 状态

`SpringApplicationUtils.defaultPropertiesResources` 是 static 字段，在多 `SpringApplication` 实例场景（如测试套件中创建多个 ApplicationContext）中共享状态。一个 Application 添加的资源路径会影响另一个 Application。

**缓解**：通过 `addShutdownHookCallback(defaultPropertiesResources::clear)` 在 JVM 关闭时清理。但在测试套件中，多个 Application 之间的状态可能互相干扰。

---

## 现代 Spring Boot（3.x）是否已支持？

| microsphere 特性 | Spring Boot 3.x 是否有等价物 | 说明 |
|------------------|------------------------|------|
| `DefaultPropertiesPostProcessor` SPI | 无 | Spring Boot 3.x 无默认属性后处理 SPI |
| `classpath*:/META-INF/config/default/*.*` | 无 | Spring Boot 3.x 无此约定路径 |
| `PropertySourceLoaders` 复合加载器 | 无 | Spring Boot 3.x 无复合 PropertySourceLoader |
| `OriginTrackedConfigurationPropertyInitializer` | 部分 | Spring Boot 3.x 对 `application.properties` 已有 Origin 追踪，但 `@PropertySource` 没有 |
| `ConfigurationMetadataRepository` 运行时查询 | 无 | Spring Boot 3.x 的配置元数据只在 IDE 中使用 |
| `ConfigurationMetadataReader` | 无 | Spring Boot 3.x 无运行时元数据读取器 |

Spring Boot 3.x 的 `ConfigDataEnvironmentPostProcessor` 支持导入 YAML/JSON 配置文件，但不支持 SPI 扩展默认属性来源。Spring Boot 3.x 的 `Origin` 机制只对 `ConfigData`（`application.properties`/`.yml`）生效，对 `@PropertySource` 和编程式添加的 PropertySource 不生效。

---

## 小结

microsphere-spring-boot 的 Environment 扩展与配置元数据，通过四个组件将 Spring Boot 的属性管理从"单一来源"扩展为"多来源可追踪"：

1. **`DefaultPropertiesApplicationListener` + `DefaultPropertiesPostProcessor` SPI**：多来源默认属性合并，框架开发者可通过 SPI 注入默认值或资源路径；`classpath*:/META-INF/config/default/*.*` 约定让框架 Jar 打包默认配置
2. **`PropertySourceLoaders`**：复合加载器，按扩展名路由，支持任意格式的属性文件
3. **`OriginTrackedConfigurationPropertyInitializer`**：利用 03-spring 的 `BeanFactoryListener.onBeanFactoryConfigurationFrozen` 回调，为非 OriginTracked 的 PropertySource 添加来源追踪
4. **`ConfigurationMetadataRepository` + `ConfigurationMetadataReader`**：将编译时配置元数据加载到运行时，暴露为可查询的 Bean，供 Actuator 端点和配置审计使用

这四个组件的共同设计理念是**让属性管理的每个环节都可扩展、可追踪、可查询**。默认属性可扩展（SPI）、属性来源可追踪（Origin）、配置元数据可查询（Repository），形成完整的属性生命周期管理。
