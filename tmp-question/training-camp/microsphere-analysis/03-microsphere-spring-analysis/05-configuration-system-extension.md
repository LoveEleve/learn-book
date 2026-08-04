# 03-05 配置系统扩展

## 问题：Spring 的配置体系缺乏可观察性和灵活性

Spring 的 `@PropertySource` 和 `Environment` 提供了配置管理的基础，但存在多个空白：

| 场景 | Spring 能力 | 问题 |
|------|-----------|------|
| YAML/JSON 属性源 | `@PropertySource` 只支持 `.properties` | 需要手动实现 `PropertySourceFactory` |
| 属性源排序 | `MutablePropertySources#addFirst/addLast/addBefore/addAfter` | `@PropertySource` 无法声明排序，只能按注册顺序 |
| 属性源变更通知 | 无 | 配置文件修改后无法感知，需要重启 |
| Environment 操作监听 | 无 | `getProperty`、`getActiveProfiles` 等操作不可观察 |
| 配置属性仓库 | 无 | 无法统一记录"哪些配置被读取了、值是什么" |
| 内联默认属性 | `@PropertySource` 需要外部文件 | 无法在注解中直接声明 `key=value` |

microsphere-spring 的 `config/` 和 `core/env/` 包通过**元注解组合 + 装饰器 + 监听器 + 事件**四层设计填补这些空白。

---

## 设计：四个扩展维度

### 整体架构

```
ConfigurableEnvironment
    │
    │  ─── 维度一：属性源扩展 ───
    │
    ├── @PropertySourceExtension (元注解)
    │       ├── @ResourcePropertySource (通用资源属性源)
    │       │       ├── @YamlPropertySource (YAML)
    │       │       └── @JsonPropertySource (JSON)
    │       └── @DefaultPropertiesPropertySource (内联属性)
    │
    │  ─── 维度二：可监听 Environment ───
    │
    ├── ListenableConfigurableEnvironment (装饰器)
    │       ├── EnvironmentListener (综合监听器)
    │       │       ├── ProfileListener (Profile 操作)
    │       │       └── PropertyResolverListener (属性解析)
    │       └── before/after 回调模式
    │
    │  ─── 维度三：属性变更事件 ───
    │
    ├── PropertySourceChangedEvent (单个变更)
    │       └── Kind: ADDED / REMOVED / REPLACED
    └── PropertySourcesChangedEvent (批量变更)
    │
    │  ─── 维度四：配置属性仓库 ───
    │
    └── ConfigurationPropertyRepository
            └── CollectingConfigurationPropertyListener (通过 PropertyResolverListener 收集)
```

---

### @PropertySourceExtension：可排序的属性源元注解

#### 元注解设计

`@PropertySourceExtension` 是一个标注在注解上的元注解（`@Target(ANNOTATION_TYPE)`），为 `@PropertySource` 增加排序和自动刷新能力：

```java
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
public @interface PropertySourceExtension {
    String name() default "";
    boolean autoRefreshed() default false;  // 配置变更时自动刷新
    boolean first() default false;           // 添加为最高优先级
    String before() default "";              // 在指定 PropertySource 之前
    String after() default "";               // 在指定 PropertySource 之后
    Class<? extends PropertySourceFactory> factory() default DefaultPropertySourceFactory.class;
    Class<? extends Comparator<Resource>> resourceComparator() default DefaultResourceComparator.class;
}
```

具体属性源注解通过元注解组合实现：

```java
@PropertySourceExtension
@Repeatable(ResourcePropertySources.class)
@Import(ResourcePropertySourceLoader.class)
public @interface ResourcePropertySource {
    @AliasFor(annotation = PropertySourceExtension.class) String name() default "";
    @AliasFor(annotation = PropertySourceExtension.class) boolean autoRefreshed() default false;
    @AliasFor(annotation = PropertySourceExtension.class) boolean first() default false;
    // ...
}
```

`@AliasFor(annotation = PropertySourceExtension.class)` 将 `@ResourcePropertySource` 的属性映射到元注解上，实现属性传递。`@Import(ResourcePropertySourceLoader.class)` 触发加载逻辑。

#### YAML 和 JSON 支持

`@YamlPropertySource` 和 `@JsonPropertySource` 通过指定 `factory` 元注解属性实现格式支持：

```java
@ResourcePropertySource(factory = YamlPropertySourceFactory.class)
public @interface YamlPropertySource { ... }

@ResourcePropertySource(factory = JsonPropertySourceFactory.class)
public @interface JsonPropertySource { ... }
```

**YamlPropertySourceFactory** 使用 `ResourceYamlProcessor`（继承 Spring 的 `YamlProcessor`），配置 SnakeYAML：

```java
public class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
        ResourceYamlProcessor processor = new ResourceYamlProcessor(resource.getResource());
        return new ImmutableMapPropertySource(name, processor.process());
    }
}
```

`ResourceYamlProcessor` 的 SnakeYAML 配置：
- `allowDuplicateKeys(false)` - 禁止重复键
- `processComments(true)` - 处理注释
- `allowRecursiveKeys(true)` - 允许递归键
- `maxAliasesForCollections(Integer.MAX_VALUE)` - 不限制别名数量

**JsonPropertySourceFactory** 使用 Jackson `ObjectMapper` 读取 JSON 为 `LinkedHashMap`，包装为 `MapPropertySource`。

**ImmutableMapPropertySource** 继承 `MapPropertySource`，在构造时用 `Collections.unmodifiableMap` 包装源 Map，防止外部修改。它还根据源 Map 类型选择合适的副本类型（`TreeMap` for `SortedMap`、`LinkedHashMap` for `LinkedHashMap` 等），保留原始顺序。

#### 内联默认属性

`@DefaultPropertiesPropertySource` 允许在注解中直接声明 `key=value` 属性，无需外部文件：

```java
@DefaultPropertiesPropertySource(properties = {
    "spring.application.name=my-app",
    "server.port=8080"
})
@Configuration
public class MyConfig { }
```

属性被添加到名为 `defaultProperties` 的 PropertySource 中（Spring 标准的默认属性源名称），优先级最低。

#### 排序机制

`PropertySourceExtensionLoader` 在加载属性源时根据 `first`/`before`/`after` 属性决定插入位置：

- `first=true`：`MutablePropertySources#addFirst`（最高优先级）
- `before="xxx"`：`addBefore("xxx", propertySource)`
- `after="xxx"`：`addAfter("xxx", propertySource)`
- 均未指定：`addLast`（最低优先级）

Spring 原生的 `@PropertySource` 只支持 `addLast`，无法控制优先级。microsphere 的排序机制解决了多个属性源之间的优先级冲突问题。

#### 自动刷新与变更事件

当 `autoRefreshed=true` 时，`PropertySourceExtensionLoader` 会创建 `ResourcePropertySourcesRefresher`--一个函数式接口，负责重新加载资源、替换属性源、发布变更事件：

```
autoRefreshed=true 时的流程：
  1. 加载阶段：createResourcePropertySourcesRefresher() 创建 refresher（lambda）
  2. 加载阶段：configureResourcePropertySourcesRefresher() 设置触发机制
     └── 基类实现：DO NOTHING（空方法，扩展点）
     └── 子类可覆盖：接入文件监听、配置中心推送等触发源
  3. 运行时（外部触发）：refresher.refresh(resourceValue, resource)
     ├── 重新解析 Resource
     ├── 创建新的 PropertySource
     ├── 替换 CompositePropertySource 中的旧属性源
     └── 发布 PropertySourcesChangedEvent
```

**关键设计**：`configureResourcePropertySourcesRefresher` 是一个 `protected` 空方法（扩展点）。基类 `PropertySourceExtensionLoader` 不提供任何自动触发机制（没有文件监听、没有定时轮询）。子类（如 `ResourcePropertySourceLoader`）可以覆盖此方法，接入文件监听（`WatchService`）、配置中心推送（如 Nacos/Apollo）或其他触发源。

变更事件类型：
- `PropertySourceChangedEvent`：单个属性源变更，包含 `Kind`（ADDED/REMOVED/REPLACED）、`newPropertySource`、`oldPropertySource`
- `PropertySourcesChangedEvent`：批量变更，包含 `List<PropertySourceChangedEvent>`

用户可以通过 `@EventListener` 监听这些事件，在配置变更后执行自定义逻辑（如重新初始化 Bean、刷新缓存）。

---

### ListenableConfigurableEnvironment：可监听的环境

#### 装饰器设计

`ListenableConfigurableEnvironment` 装饰 `ConfigurableEnvironment`，在所有操作前后通知 Listener：

```java
public class ListenableConfigurableEnvironment implements ConfigurableEnvironment {

    private final ConfigurableEnvironment delegate;
    private final List<EnvironmentListener> environmentListeners;
    private final List<ProfileListener> profileListeners;
    private final List<PropertyResolverListener> propertyResolverListeners;

    @Override
    public String getProperty(String key) {
        propertyResolverListeners.beforeGetProperty(...);
        String value = delegate.getProperty(key);
        propertyResolverListeners.afterGetProperty(..., value);
        return value;
    }

    @Override
    public String[] getActiveProfiles() {
        profileListeners.beforeGetActiveProfiles(...);
        String[] profiles = delegate.getActiveProfiles();
        profileListeners.afterGetActiveProfiles(..., profiles);
        return profiles;
    }
}
```

#### 三层监听器接口

```
EnvironmentListener (综合)
    ├── extends ProfileListener
    │       ├── beforeGetActiveProfiles / afterGetActiveProfiles
    │       ├── beforeGetDefaultProfiles / afterGetDefaultProfiles
    │       ├── beforeSetActiveProfiles / afterSetActiveProfiles
    │       └── beforeAddActiveProfile / afterAddActiveProfile
    │
    ├── extends PropertyResolverListener
    │       ├── beforeGetProperty / afterGetProperty
    │       ├── beforeGetRequiredProperty / afterGetRequiredProperty
    │       ├── beforeResolvePlaceholders / afterResolvePlaceholders
    │       ├── beforeSetConversionService / afterSetConversionService
    │       └── beforeGetConversionService / afterGetConversionService
    │
    └── Environment 自身操作
            ├── beforeGetPropertySources / afterGetPropertySources
            ├── beforeGetSystemProperties / afterGetSystemProperties
            ├── beforeGetSystemEnvironment / afterGetSystemEnvironment
            └── beforeMerge / afterMerge
```

`EnvironmentListener` 继承 `ProfileListener` 和 `PropertyResolverListener`，是一个"全能"监听器。用户可以只实现其中一个子接口，也可以实现 `EnvironmentListener` 覆盖所有。

所有方法都是 `default` 空实现，Listener 只需覆盖关心的方法。

#### 监听器发现

三层监听器分别通过 `SpringFactoriesLoader` 和 `BeanFactory.getBeansOfType` 加载：

- `EnvironmentListener`：从 `spring.factories` 加载
- `ProfileListener`：从 `spring.factories` 加载 + 所有 `EnvironmentListener` 实例（因为 `EnvironmentListener extends ProfileListener`）
- `PropertyResolverListener`：从 `spring.factories` 加载 + 所有 `EnvironmentListener` 实例

加载后按 `AnnotationAwareOrderComparator` 排序。这种设计意味着一个 `EnvironmentListener` 实例会同时出现在三个列表中，确保其所有回调都被触发。

#### 启用方式

通过 `ListenableConfigurableEnvironmentInitializer`（`ApplicationContextInitializer`），默认禁用：

```properties
microsphere.spring.listenable-environment.enabled=true
```

启用后，`context.setEnvironment(new ListenableConfigurableEnvironment(context))` 替换原始 Environment。由于是装饰器模式，原始 Environment 的所有功能完全保留。

#### MethodHandle 处理版本差异

`ListenableConfigurableEnvironment` 中有一段有趣的代码：

```java
private static final MethodHandle SET_ESCAPE_CHARACTER_METHOD_HANDLE =
    findVirtual(ConfigurablePropertyResolver.class, "setEscapeCharacter", Character.class);
```

`setEscapeCharacter` 方法在 Spring Framework 5.0 中添加，但在某些版本中可能不存在。microsphere 使用 `MethodHandle` 做防御性查找：如果方法存在则调用，不存在则跳过。这比反射 `try-catch` 更高效，且代码更清晰。

---

### ConfigurationPropertyRepository：配置属性仓库

#### 设计

`ConfigurationPropertyRepository` 是一个 `ConcurrentMap<String, ConfigurationProperty>` 仓库，记录每个配置属性的元信息：

- `name`：属性名
- `type`：目标类型
- `value`：解析后的值
- `defaultValue`：默认值
- `source`：来源（系统属性/环境变量/应用配置等）

通过 `CollectingConfigurationPropertyListener`（同时实现 `PropertyResolverListener` 和 `AutowireCandidateResolvingListener`）自动收集。该 Listener 在两个维度收集配置属性：

- **`afterGetProperty`**：当 `environment.getProperty(name)` 被调用时，记录属性名、类型、值、默认值
- **`afterGetRequiredProperty`**：当 `environment.getRequiredProperty(name)` 被调用时，同上且标记为 required
- **`afterSetPlaceholderPrefix/Suffix`**：跟踪占位符前缀/后缀变更

注意：虽然类声明了 `implements AutowireCandidateResolvingListener`，但并未覆盖 `suggestedValueResolved`/`lazyProxyResolved` 方法（使用默认空实现）。声明该接口可能是为了未来扩展或注册便利。当前配置属性收集仅通过 `PropertyResolverListener` 路径。

该 Listener 不在主 `spring.factories` 中，需要用户手动注册为 Bean 或添加到 `spring.factories`。

#### 与 ListenableConfigurableEnvironment 的配合

`CollectingConfigurationPropertyListener` 作为 `PropertyResolverListener` 注册。当 `ListenableConfigurableEnvironment` 启用时，每次 `getProperty` 调用都会通过 Listener 将属性信息收集到 Repository。

这意味着 Repository 中的数据是**惰性收集**的--只有被实际读取的属性才会出现在 Repository 中。未被读取的属性不会消耗内存。

#### 使用场景

- **配置审计**：哪些配置被读取了、值是什么、从哪里来
- **配置诊断**：哪些配置有默认值、哪些被覆盖了
- **配置文档**：自动生成配置属性清单

---

### 一个具体示例

```java
// 使用 YAML 属性源，声明最高优先级
@YamlPropertySource(
    name = "app-config",
    value = "classpath:config/app.yml",
    first = true,              // 最高优先级
    autoRefreshed = true        // 启用刷新基础设施（触发机制需子类提供）
)
// 使用内联默认属性
@DefaultPropertiesPropertySource(properties = {
    "app.name=my-app",
    "app.version=1.0.0"
})
// 启用可监听 Environment
// (通过 spring.factories 自动注册，需设置 microsphere.spring.listenable-environment.enabled=true)
@Configuration
public class AppConfig {

    @Bean
    public ApplicationListener<PropertySourcesChangedEvent> configChangeListener() {
        return event -> {
            event.getSubEvents().forEach(sub -> {
                System.out.printf("Config changed: %s %s%n",
                    sub.getKind(), sub.getNewPropertySource().getName());
            });
        };
    }
}
```

当外部触发刷新（如子类 `ResourcePropertySourceLoader` 通过文件监听或配置中心推送调用 `refresher.refresh()`）时：
1. `ResourcePropertySourcesRefresher` 重新解析 Resource
2. 创建新的 `ImmutableMapPropertySource`
3. 替换 `MutablePropertySources` 中的旧属性源
4. 发布 `PropertySourceChangedEvent(kind=REPLACED, new, old)`
5. 发布 `PropertySourcesChangedEvent` 包含上述子事件
6. 用户的 `@EventListener` 被触发

同时，每次 `environment.getProperty("app.name")` 被调用时：
1. `ListenableConfigurableEnvironment` 通知 `PropertyResolverListener.beforeGetProperty`
2. 委托 `delegate.getProperty("app.name")` 返回 `"my-app"`
3. 通知 `PropertyResolverListener.afterGetProperty(..., "my-app")`
4. `CollectingConfigurationPropertyListener` 将 `app.name` 记录到 `ConfigurationPropertyRepository`

---

## 永恒原理

### 1. 元注解组合 vs 继承

Spring 的 `@PropertySource` 是一个不可扩展的注解--你无法给它添加 `first`/`before`/`after` 属性。microsphere 没有修改 `@PropertySource`，而是创建了 `@PropertySourceExtension` 元注解，通过 `@AliasFor` 实现属性传递。

这种"元注解组合"模式比继承更灵活：
- `@YamlPropertySource` 只需指定 `factory = YamlPropertySourceFactory.class`，其余属性自动从 `@ResourcePropertySource` -> `@PropertySourceExtension` 传递
- 用户可以创建自定义属性源注解，只需标注 `@PropertySourceExtension` 并通过 `@AliasFor` 映射属性
- 不修改 Spring 原生注解，兼容性完全保留

### 2. Before/After 回调模式

`ListenableConfigurableEnvironment` 的每个操作都拆分为 `before` 和 `after` 两个回调。这种模式比"仅 after"提供了更多可能性：

- **Before**：可以记录操作前的状态、修改输入参数（如修改请求的属性名）、甚至短路操作
- **After**：可以处理操作结果、记录耗时、触发后续逻辑

与第 1 篇的 `BeanListener`（也是 before/after 模式）一致，microsphere 在整个体系中统一使用这种双向回调模式。

### 3. 不可变属性源

`ImmutableMapPropertySource` 用 `Collections.unmodifiableMap` 包装源 Map，防止外部代码修改属性源内容。这在多线程环境下特别重要：

- 属性源可能被多个线程并发读取
- 如果 Map 可变，读取时可能看到不一致的状态
- 不可变 Map 保证读取操作的线程安全性

当属性源需要更新时（`autoRefreshed=true`），不是修改现有 Map，而是创建新的 `ImmutableMapPropertySource` 替换旧的。这是"Copy-on-Write"思想在配置管理中的应用。

### 4. 装饰器在 Environment 中的应用

`ListenableConfigurableEnvironment` 装饰 `ConfigurableEnvironment`，与第 4 篇的 `ListenableAutowireCandidateResolver` 装饰 `AutowireCandidateResolver` 模式完全一致。两者的共同点：

- 实现 Spring 原生接口
- 持有原始对象作为委托
- 在委托调用前后插入通知逻辑
- 通过 `ApplicationContextInitializer` 或 `BeanFactoryPostProcessor` 自动安装
- 默认禁用，按需启用

这种"装饰器 + 默认禁用"的模式是 microsphere 扩展 Spring 的标准范式。

---

## 边界与反例

### 1. autoRefreshed 的触发机制是扩展点而非内置功能

`autoRefreshed=true` 创建了 `ResourcePropertySourcesRefresher`（刷新基础设施），但基类 `PropertySourceExtensionLoader#configureResourcePropertySourcesRefresher` 是空方法（`DO NOTHING`）。microsphere-spring 本身不提供文件监听、定时轮询或配置中心推送等触发机制。

**实际使用**：需要子类覆盖 `configureResourcePropertySourcesRefresher`，在其中接入具体的触发源（如 Java `WatchService`、Nacos 配置监听、Apollo 推送）。或者通过外部代码获取 refresher 引用并手动调用 `refresh()`。

**边界**：如果只是设置 `autoRefreshed=true` 而不提供触发机制，刷新永远不会发生。`autoRefreshed` 的语义是"启用刷新能力"，不是"自动检测变更"。

### 2. ListenableConfigurableEnvironment 的性能开销

每次 `getProperty` 调用都会触发 `beforeGetProperty` 和 `afterGetProperty` 回调，遍历所有 `PropertyResolverListener`。在配置读取频繁的场景（如每次 HTTP 请求读取多个配置项），累积开销不可忽视。

**缓解**：默认禁用，仅在调试/审计场景启用。`CollectingConfigurationPropertyListener` 可以通过 `ConfigurationPropertyRepository.MAX_SIZE_PROPERTY_NAME` 限制最大收集数量（默认 99999）。

### 3. YAML 多文档支持

`ResourceYamlProcessor` 继承 Spring 的 `YamlProcessor`，后者支持多文档 YAML（`---` 分隔）。但 `process()` 方法将所有文档合并为一个 Map，后文档的键覆盖前文档。如果多文档 YAML 中有同名键但不同含义，可能导致意外的覆盖。

### 4. ListenableConfigurableEnvironment 的 MethodHandle 兼容性

`setEscapeCharacter` 方法使用 `MethodHandle` 防御性查找。如果 `MethodHandle` 查找失败（方法不存在），`SET_ESCAPE_CHARACTER_METHOD_HANDLE` 为 null。调用时需要 null 检查，否则 NPE。

**边界**：这是对 Spring 版本差异的妥协。如果 Spring 未来移除或重命名此方法，microsphere 的 `MethodHandle` 查找会静默失败，不会影响其他功能。

### 5. PropertySourceExtensionLoader 的排序冲突

如果两个属性源都声明 `before="sameSource"`，它们的相对顺序取决于加载顺序（即注解声明的顺序）。microsphere 不保证同优先级属性源的确定性顺序。

**缓解**：使用 `first=true` 或 `after` 精确控制优先级，避免多个属性源声明相同的 `before`/`after` 目标。

### 6. CollectingConfigurationPropertyListener 的惰性收集

Repository 只记录被实际读取的属性。如果应用启动后某个配置从未被 `getProperty` 调用，它不会出现在 Repository 中。这可能导致配置审计遗漏--某些"以防万一"的配置（声明了但未使用）不会被记录。

---

## 现代 Spring（6.x）是否已支持？

| microsphere 特性 | Spring 6.x 是否有等价物 | 说明 |
|------------------|----------------------|------|
| `@PropertySourceExtension` 排序 | 无 | Spring 6.x 的 `@PropertySource` 仍无排序属性 |
| `@YamlPropertySource` | 无 | Spring 6.x 仍不原生支持 YAML `@PropertySource` |
| `@JsonPropertySource` | 无 | Spring 6.x 仍不原生支持 JSON `@PropertySource` |
| `@DefaultPropertiesPropertySource` | 无 | Spring 6.x 无内联属性注解 |
| `ListenableConfigurableEnvironment` | 无 | Spring 6.x 的 Environment 仍不可监听 |
| `PropertySourceChangedEvent` | 无 | Spring 6.x 无属性源变更事件 |
| `ConfigurationPropertyRepository` | 无 | Spring 6.x 无配置属性仓库 |

Spring Boot 的 `@ConfigurationProperties` + `@RefreshScope`（Spring Cloud）提供了配置热更新能力，但这是 Spring Cloud 的功能，不是 Spring Framework 原生。且 `@RefreshScope` 通过销毁并重建 Bean 实现，不是属性源级别的刷新。

Spring 6.0 的 `ConfigurableEnvironment` 没有新增监听接口。Spring Boot 3.1 的 `ConfigDataEnvironmentPostProcessor` 支持导入 YAML/JSON 配置文件，但这是 Spring Boot 的功能，且不支持 `@PropertySource` 注解方式。

---

## 小结

microsphere-spring 的配置系统扩展，通过四个维度将 Spring 的"静态配置"变为"动态可观察"：

1. **属性源扩展**（`@PropertySourceExtension`）：元注解组合，支持排序（`first`/`before`/`after`）、自动刷新（`autoRefreshed`）、YAML/JSON 格式、内联属性
2. **可监听 Environment**（`ListenableConfigurableEnvironment`）：装饰器模式，三层监听器（Environment/Profile/PropertyResolver），before/after 回调覆盖所有 Environment 操作
3. **属性变更事件**（`PropertySourceChangedEvent`）：ADDED/REMOVED/REPLACED 三种变更类型，支持批量事件
4. **配置属性仓库**（`ConfigurationPropertyRepository`）：惰性收集，记录每个被读取的配置属性的元信息

这四个维度可以独立使用，也可以组合使用。例如，`autoRefreshed=true` 的 YAML 属性源 + `ListenableConfigurableEnvironment` + `@EventListener(PropertySourcesChangedEvent.class)` = 完整的配置热更新方案，且全部基于 Spring Framework（不依赖 Spring Boot/Cloud）。
