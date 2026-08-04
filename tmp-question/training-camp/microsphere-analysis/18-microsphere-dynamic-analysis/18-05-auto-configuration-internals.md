# 18-05 Auto-Configuration 内部机制

## 目录

- [Auto-Configuration 的三阶段处理流程](#auto-configuration-的三阶段处理流程)
- [AutoConfigurationImportSelector：选择器](#autoconfigurationimportselector选择器)
- [AutoConfigurationImportFilter：过滤器](#autoconfigurationimportfilter过滤器)
- [AutoConfigurationImportListener：监听器](#autoconfigurationimportlistener监听器)
- [ConfigurationClassPostProcessor 的完整处理链](#configurationclasspostprocessor-的完整处理链)
- [spring.factories 与 AutoConfiguration.imports](#springfactories-与-autoconfigurationimports)
- [@Conditional 体系](#conditional-体系)
- [18-dynamic 的 4 组件协作模式](#18-dynamic-的-4-组件协作模式)
- [模块互斥的实现：banned-modules](#模块互斥的实现banned-modules)

---

## Auto-Configuration 的三阶段处理流程

Spring Boot 的 Auto-Configuration 不是一个单一的动作，而是分三个阶段的处理流程。理解这三个阶段是理解 18-dynamic 的 Auto-Configuration 过滤机制的前提。

```
阶段 1: 候选加载（Candidate Loading）
  Goal: 确定所有可能被应用的 Auto-Configuration 类
  
  入口: AutoConfigurationImportSelector.selectImports()
  输入: 无（从 classpath 扫描）
  输出: String[] — 所有候选 Auto-Configuration 类的全限定名完整 列表
  数据源: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

阶段 2: 过滤（Filtering）
  Goal: 从候选中排除不满足条件的
  
  入口: AutoConfigurationImportFilter.match()
  输入: String[] — 阶段 1 输出的全量候选
  输出: boolean[] — 每个候选是否保留
  参与者: AutoConfigurationImportFilter 接口的实现链

阶段 3: 应用（Application）
  Goal: 将保留的 Auto-Configuration 类作为 @Configuration 处理
  
  入口: ConfigurationClassPostProcessor.processConfigBeanDefinitions()
  输入: String[] — 阶段 2 过滤后的保留类
  输出: 解析所有 @Bean @Import @Conditional，注册 BeanDefinition
  参与者: ConfigurationClassParser + BeanDefinitionReader
```

### 三个阶段的关系

```java
// AutoConfigurationImportSelector.selectImports() 的简化伪代码
public String[] selectImports(AnnotationMetadata annotationMetadata) {
    // 阶段 1: 加载候选（从 AutoConfiguration.imports）
    List<String> configurations = getCandidateConfigurations(annotationMetadata, excluded);

    // 去除重复
    configurations = removeDuplicates(configurations);

    // 阶段 2: 过滤（通过 AutoConfigurationImportFilter 链）
    configurations = filter(configurations, autoConfigurationMetadata);

    // 阶段 3: 触发 AutoConfigurationImportEvent（通知 Listener）
    fireAutoConfigurationImportEvents(configurations, excluded);

    // 返回结果给 ConfigurationClassPostProcessor
    return StringUtils.toStringArray(configurations);
}
```

注意阶段 3 在 18-dynamic 中也被利用了——`DynamicJdbcAutoConfigurationImportListener` 就是在 `fireAutoConfigurationImportEvents` 这一步被调用的。

---

## AutoConfigurationImportSelector：选择器

`AutoConfigurationImportSelector` 是 Auto-Configuration 的**入口**。它被 `@EnableAutoConfiguration` 注解通过 `@Import(AutoConfigurationImportSelector.class)` 引入，`ConfigurationClassPostProcessor` 在处理 `@Import` 时调用它的 `selectImports()` 方法。

### 继承与重写

```java
// 标准用法
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)  // 通过 @Import 引入 Selector
public @interface EnableAutoConfiguration { ... }

// 18-dynamic 的自定义用法
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(DynamicJdbcAutoConfigurationImportSelector.class)  // 自定义 Selector
public @interface EnableDynamicJdbcAutoConfiguration { ... }
```

`DynamicJdbcAutoConfigurationImportSelector` 继承自 `AutoConfigurationImportSelector`，重写了 `selectImports()` 和 `getAutoConfigurationImportFilters()`：

```java
class DynamicJdbcAutoConfigurationImportSelector
        extends AutoConfigurationImportSelector
        implements ImportSelector, DisposableBean {

    private static final List<AutoConfigurationImportFilter> FILTERS =
            Arrays.asList(DynamicJdbcAutoConfigurationImportFilter.INSTANCE);

    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        ClassLoader classLoader = getBeanClassLoader();
        String[] autoConfigurationClassNames = getAutoConfigurationClassNames(classLoader);

        if (ObjectUtils.isEmpty(autoConfigurationClassNames)) {
            // 缓存未命中 → 执行父类的 selectImports（标准三阶段）
            super.selectImports(annotationMetadata);
            // 再次尝试读取缓存
            autoConfigurationClassNames = getAutoConfigurationClassNames(classLoader);
        } else {
            // 缓存命中 → 直接返回
            return autoConfigurationClassNames;
        }

        return autoConfigurationClassNames;
    }

    @Override
    protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
        return FILTERS;  // 只使用自定义 Filter
    }
}
```

### 缓存优先策略

这是 18-dynamic 的关键优化。标准的 `AutoConfigurationImportSelector` 在每次 `selectImports()` 调用时都会重新执行三个阶段。18-dynamic 通过缓存避免了重复扫描：

```
第一次调用（如子上下文 1）:
  selectImports()
    → 缓存为空
    → super.selectImports()
      → 阶段 1: 加载候选 (10ms)
      → 阶段 2: Filter 过滤 (5ms)
      → 阶段 3: Listener 缓存结果
    → 再次读取缓存 → 命中
    → 返回结果

第二次调用（如子上下文 2）:
  selectImports()
    → 缓存命中（来自第一次的 Listener 写入）
    → 直接返回
    → 跳过阶段 1-3
```

**为什么需要缓存？**

子上下文的数量是不确定的（取决于 DynamicJdbcConfig 的数量）。每个子上下文都需要执行 Auto-Configuration 导入。如果没有缓存，N 个子上下文就会执行 N 次完整的候选加载和过滤，每次约 15-30ms。N=10 时就是 150-300ms 的重复工作。

缓存按 ClassLoader 分隔（`ConcurrentHashMap<ClassLoader, String[]>`），因为不同 ClassLoader 可能加载到不同的 `AutoConfiguration.imports` 文件（在 OSGi、自定义 ClassLoader 等场景下）。

### 自定义 Filter 的注入

```java
@Override
protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
    return FILTERS;
}
```

这个重写替换了默认的 Filter 列表。默认情况下，Spring Boot 会加载所有 `AutoConfigurationImportFilter` 接口的 `spring.factories` 实现，包括 `OnClassCondition`（它检查候选类上的 `@ConditionalOnClass` 注解）。18-dynamic 通过这个重写替换了默认的 Filter 链，改用基于包名前缀的匹配 + 显式 `ClassUtils.isPresent()` 检查。这意味着 `@ConditionalOnClass` 的检查没有被绕过——它在后面的 `@Conditional` 阶段仍然会执行。Filter 阶段只做粗粒度排除。

### DisposableBean.destroy()

```java
@Override
public void destroy() throws Exception {
    DynamicJdbcAutoConfigurationRepository.clear();
}
```

子上下文销毁时，清空缓存。为什么需要清空？在标准 JVM ClassLoader 中类一旦加载不会被卸载，所以清空缓存对标准部署没有影响。但在自定义 ClassLoader 场景下（如 OSGi、热部署、Web 应用重加载），ClassLoader 可能被 GC，其加载的类也会被卸载。此时清除缓存后，新子上下文使用新 ClassLoader 重新加载 Auto-Configuration 列表。

---

## AutoConfigurationImportFilter：过滤器

`AutoConfigurationImportFilter` 是 Auto-Configuration 的第二道关卡（第一道是 `spring.autoconfigure.exclude`）。

### 接口定义

```java
@FunctionalInterface
public interface AutoConfigurationImportFilter {

    /**
     * 对候选 Auto-Configuration 类进行批量过滤
     *
     * @param autoConfigurationClasses 候选类的全限定名数组
     * @param autoConfigurationMetadata 候选类的元数据（从 AutoConfiguration.imports 加载）
     * @return boolean[] — 每个候选类是否被保留（true = 保留，false = 排除）
     */
    boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata);
}
```

### DynamicJdbcAutoConfigurationImportFilter

```java
class DynamicJdbcAutoConfigurationImportFilter
        implements AutoConfigurationImportFilter, EnvironmentAware, BeanClassLoaderAware {

    public static final DynamicJdbcAutoConfigurationImportFilter INSTANCE = new DynamicJdbcAutoConfigurationImportFilter();

    private Set<String> basePackages;
    private ClassLoader classLoader;

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata metadata) {
        int length = autoConfigurationClasses.length;
        boolean[] result = new boolean[length];
        for (int i = 0; i < result.length; i++) {
            String className = autoConfigurationClasses[i];
            result[i] = match(className) && isClassPresent(className);
        }
        return result;
    }

    public boolean match(String autoConfigurationClassName) {
        for (String basePackage : basePackages) {
            if (autoConfigurationClassName.startsWith(basePackage)) {
                return true;
            }
        }
        return false;
    }

    private boolean isClassPresent(String className) {
        return ClassUtils.isPresent(className, classLoader);
    }
}
```

### 匹配逻辑详解

两个条件必须同时满足：

**条件 1：包名前缀匹配**

```java
autoConfigurationClassName.startsWith(basePackage)
```

例如，`basePackages` 中存放的是各模块配置的 `auto-configuration.base-packages` 值——它们是完整的 Auto-Configuration 类名或包前缀（如 `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` 或 `org.apache.shardingsphere.`），通过 `startsWith` 匹配。

以 `basePackages = {"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration", "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"}` 为例：
- `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` → 精确匹配 ✅
- `org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration` → 不匹配（名前缀不同）❌
- 但如果配置了 `org.springframework.boot.autoconfigure.jdbc.`，则 `DataSourceAutoConfiguration` 和 `DataSourceConfiguration` 都匹配

**条件 2：Class 必须在 ClassLoader 中存在**

```java
ClassUtils.isPresent(className, classLoader)
```

即使包名前缀匹配，如果 classpath 中不存在这个类，也会被排除。这处理了"依赖可选"的情况——比如项目中没引入 MyBatis-Plus 的依赖，它的 Auto-Configuration 即使被匹配到也要排除。

### basePackages 的来源

```java
@Override
public void setEnvironment(Environment environment) {
    this.basePackages = getAllModulesAutoConfigurationBasePackages((ConfigurableEnvironment) environment);
}
```

```java
// DynamicJdbcPropertyUtils.getAllModulesAutoConfigurationBasePackages()
public static Set<String> getAllModulesAutoConfigurationBasePackages(ConfigurableEnvironment environment) {
    Set<String> allClassPrefixes = new TreeSet<>();

    // 读取所有模块的 auto-configuration.base-packages 配置
    List<String> classPrefixesValues = getAllModulesPropertyValues(
            environment, AUTO_CONFIGURATION_BASE_PACKAGES_PROPERTY_NAME);

    for (String value : classPrefixesValues) {
        allClassPrefixes.addAll(resolveCommaDelimitedValueToList(environment, value));
    }

    return unmodifiableSet(allClassPrefixes);
}
```

这个 `getAllModulesPropertyValues` 方法遍历 `microsphere.dynamic.jdbc.modules.*` 下的所有模块，收集每个模块的 `auto-configuration.base-packages` 配置值。

---

## AutoConfigurationImportListener：监听器

`AutoConfigurationImportListener` 是 Auto-Configuration 的最終階段鉤子，在過濾完成後被調用。

### 接口定義

```java
@FunctionalInterface
public interface AutoConfigurationImportListener {

    /**
     * 在 Auto-Configuration 导入事件发生时调用
     * 
     * @param event 包含候选配置列表和排除列表
     */
    void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event);
}
```

### DynamicJdbcAutoConfigurationImportListener

```java
public class DynamicJdbcAutoConfigurationImportListener
        implements AutoConfigurationImportListener, EnvironmentAware, BeanClassLoaderAware {

    private static final DynamicJdbcAutoConfigurationImportFilter filter =
            DynamicJdbcAutoConfigurationImportFilter.INSTANCE;

    private ClassLoader classLoader;
    private ConfigurableEnvironment environment;

    @Override
    public void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event) {
        // 如果已经缓存，跳过
        if (DynamicJdbcAutoConfigurationRepository.isCached(classLoader)) {
            return;
        }

        // 获取所有候选配置
        List<String> configurationClassNames = event.getCandidateConfigurations();

        // 使用 Filter 匹配（第二次过滤）
        String[] matchedConfigurationClassNames = configurationClassNames.stream()
                .filter(filter::match)
                .toArray(String[]::new);

        // 缓存结果
        DynamicJdbcAutoConfigurationRepository.cache(classLoader, matchedConfigurationClassNames);
    }
}
```

### 为什么 Listener 中要再过滤一次

Listener 收到的 `AutoConfigurationImportEvent` 包含**经过 Filter 层过滤后**的候选列表（`event.getCandidateConfigurations()`）。但这里有一个关键点：这个列表是经过 Filter 和标准排除逻辑共同作用后的最终结果，18-dynamic 收到的不仅仅是 Filter 的输出。

18-dynamic 需要将匹配的类名缓存到 `DynamicJdbcAutoConfigurationRepository` 中，供后续子上下文复用和 banned-modules 查询。但 `AutoConfigurationImportEvent` 本身没有提供"哪些类是被 Filter 匹配的"这个信息（它只给了最终的 String[] 列表）。

所以 Listener 重新应用 `filter::match`——对 `event.getCandidateConfigurations()` 中每个类名做包名前缀匹配。匹配上的类名就是 18-dynamic 模块关心的 Auto-Configuration 类，被缓存到 Repository。这等同于第二次执行 Filter 的判断逻辑，但因为输入已经是过滤后的小列表，开销很小。

### 在 spring.factories 中的注册

```properties
# spring.factories（18-dynamic 的）
org.springframework.boot.autoconfigure.AutoConfigurationImportListener=\
  io.microsphere.dynamic.jdbc.spring.boot.autoconfigure.DynamicJdbcAutoConfigurationImportListener
```

Spring Boot 通过 `SpringFactoriesLoader` 加载所有 `AutoConfigurationImportListener`，在 `AutoConfigurationImportSelector.fireAutoConfigurationImportEvents()` 中按序调用：

```java
// AutoConfigurationImportSelector
private void fireAutoConfigurationImportEvents(List<String> configurations, Set<String> exclusions) {
    List<AutoConfigurationImportListener> listeners = getAutoConfigurationImportListeners();
    if (!listeners.isEmpty()) {
        AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, configurations, exclusions);
        for (AutoConfigurationImportListener listener : listeners) {
            listener.onAutoConfigurationImportEvent(event);
        }
    }
}
```

---

## ConfigurationClassPostProcessor 的完整处理链

`ConfigurationClassPostProcessor` 是 Spring 内部处理 `@Configuration` 类的核心 BeanFactoryPostProcessor。Auto-Configuration 的最终落地就是通过它完成的。

### 处理链

```
BeanFactoryPostProcessor 执行阶段
  │
  ├─ ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry()
  │   │
  │   ├─ 1. 构建 ConfigurationClassParser
  │   │
  │   ├─ 2. parse(Set<BeanDefinitionHolder> configCandidates)
  │   │   │
  │   │   ├─ 对每个候选的 @Configuration 类：
  │   │   │   ├─ 解析 @PropertySource
  │   │   │   ├─ 解析 @ComponentScan → 递归处理扫描到的类
  │   │   │   ├─ 解析 @Import → 递归处理 Import 的类
  │   │   │   │   └─ ImportSelector.selectImports()  → AutoConfiguration
  │   │   │   │   └─ ImportBeanDefinitionRegistrar  → 这里!
  │   │   │   ├─ 解析 @ImportResource
  │   │   │   └─ 解析 @Bean 方法
  │   │   │
  │   │   └─ 对所有递归解析的类重复上述过程
  │   │
  │   ├─ 3. validate() — 验证解析的配置类
  │   │
  │   └─ 4. loadBeanDefinitions()
  │       └─ ConfigurationClassBeanDefinitionReader
  │           ├─ 注册 @Bean 方法为 BeanDefinition
  │           ├─ 注册 @Import 的结果
  │           └─ 注册 ImportBeanDefinitionRegistrar 注册的 Bean
  │
  └─ ConfigurationClassPostProcessor.postProcessBeanFactory()
      └─ 增强 @Configuration 类（CGLIB 代理）
```

### @EnableAutoConfiguration 的 Import 链

```java
@SpringBootApplication
  → @EnableAutoConfiguration
    → @Import(AutoConfigurationImportSelector.class)
      → selectImports() 执行三个阶段
        → 返回过滤后的 Auto-Configuration 类名数组
      → ConfigurationClassParser 将这些类名作为 @Configuration 处理
        → 每个 Auto-Configuration 类：
          ├─ @ConditionalOnClass / @ConditionalOnMissingBean 等条件
          ├─ @Bean 方法 → 注册为 BeanDefinition
          └─ @Import → 递归处理
```

在 18-dynamic 中，这个链的入口是 `@EnableDynamicJdbcAutoConfiguration` 而不是 `@EnableAutoConfiguration`。两者的区别是：

| | @EnableAutoConfiguration | @EnableDynamicJdbcAutoConfiguration |
|---|---|---|
| Import 的 Selector | AutoConfigurationImportSelector | DynamicJdbcAutoConfigurationImportSelector |
| 是否走缓存 | 否（每次都扫描） | 是（优先走缓存） |
| 使用哪些 Filter | 所有 spring.factories 注册的 | 仅 DynamicJdbcAutoConfigurationImportFilter |
| 结果范围 | 所有匹配的 Auto-Configuration | 仅 18-dynamic 模块相关的 |

### 为什么需要单独的 @EnableDynamicJdbcAutoConfiguration

不能复用 `@EnableAutoConfiguration` 的原因：

1. **缓存策略不同**：标准 Selector 不缓存，每次调用都重新加载
2. **Filter 不同**：标准 Filter 集合是全量的（包括 `OnClassCondition`、`OnBeanCondition` 等），而子上下文只需要基于包名前缀的过滤
3. **作用域不同**：标准 Selector 的结果是整个应用的 Auto-Configuration，而子上下文只需要与其模块相关的部分

---

## spring.factories 与 AutoConfiguration.imports

### 两种配置文件的分工

| 文件 | 路径 | 用途 |
|------|------|------|
| `spring.factories` | `META-INF/spring.factories` | Spring Boot 的 SPI 配置文件，注册扩展点 |
| `AutoConfiguration.imports` | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Auto-Configuration 类列表 |

在 Spring Boot 3.0+ 中，`EnableAutoConfiguration` 的配置从 `spring.factories` 迁移到了 `AutoConfiguration.imports`，但其他 SPI（如 `AutoConfigurationImportFilter`、`AutoConfigurationImportListener`、`ApplicationListener`）的注册机制**没有变化**，仍然通过 `spring.factories`。

### spring.factories 的加载机制

```java
// SpringFactoriesLoader.loadFactoryNames(type, classLoader)
public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
    String factoryTypeName = factoryType.getName();
    // 扫描所有 JAR 中的 META-INF/spring.factories
    // 收集 key=factoryTypeName 的 value 列表
    return loadSpringFactories(classLoader).getOrDefault(factoryTypeName, Collections.emptyList());
}
```

18-dynamic 的 `spring.factories` 注册了 6 种 SPI：

```properties
# ApplicationListener
org.springframework.context.ApplicationListener=\
  io.microsphere.dynamic.jdbc.spring.boot.context.DynamicJdbcContextApplicationListener

# AutoConfigurationImportListener
org.springframework.boot.autoconfigure.AutoConfigurationImportListener=\
  io.microsphere.dynamic.jdbc.spring.boot.autoconfigure.DynamicJdbcAutoConfigurationImportListener

# DefaultPropertiesPostProcessor
io.microsphere.spring.boot.env.DefaultPropertiesPostProcessor=\
  io.microsphere.dynamic.jdbc.spring.boot.env.DynamicJdbcDefaultPropertiesPostProcessor

# 4 个模块化 SPI
io.microsphere.dynamic.jdbc.spring.boot.config.ConfigPostProcessor=\
  ..., ..., ..., ..., ..., ...  # 6 个实现

io.microsphere.dynamic.jdbc.spring.boot.config.validation.ConfigValidator=\
  ..., ..., ..., ..., ..., ...  # 6 个实现

io.microsphere.dynamic.jdbc.spring.boot.env.ConfigConfigurationPropertiesSynthesizer=\
  ..., ..., ..., ..., ...       # 5 个实现

io.microsphere.dynamic.jdbc.spring.boot.context.ConfigBeanDefinitionRegistrar=\
  ..., ..., ..., ..., ...       # 5 个实现
```

### 加载时机

| SPI | 加载时机 | 加载者 |
|-----|---------|--------|
| ApplicationListener | SpringApplication 构造时 | SpringFactoriesLoader |
| AutoConfigurationImportListener | AutoConfigurationImportSelector 初始化时 | SpringFactoriesLoader |
| DefaultPropertiesPostProcessor | `DefaultPropertiesApplicationListener` 中 | SpringFactoriesLoader |
| ConfigPostProcessor | `DynamicJdbcContextProcessor.getConfigPostProcessors()` | SpringFactoriesLoaderUtils |
| ConfigValidator | `DynamicJdbcContextProcessor.getConfigValidators()` | SpringFactoriesLoaderUtils |
| ConfigConfigurationPropertiesSynthesizer | `DynamicJdbcContextProcessor.get...()` | SpringFactoriesLoaderUtils |
| ConfigBeanDefinitionRegistrar | `DynamicJdbcContextProcessor.get...()` | SpringFactoriesLoaderUtils |

后四个 SPI 的加载通过 `SpringFactoriesLoaderUtils.loadFactories()`，它是 Spring 的 `SpringFactoriesLoader.loadFactories()` 的封装，额外支持：
- 排序（`Ordered` 接口）
- 过滤（按类型匹配）
- 缓存（`ConcurrentHashMap`）

---

## @Conditional 体系

Auto-Configuration 的过滤不仅有 `AutoConfigurationImportFilter`，还有 `@Conditional` 注解。两者的区别很重要：

| | AutoConfigurationImportFilter | @Conditional |
|---|---|---|
| **作用阶段** | Auto-Configuration 导入时 | BeanDefinition 注册时 |
| **触发时机** | `ConfigurationClassParser` 处理 @Import 之前 | `ConfigurationClassBeanDefinitionReader` 注册 @Bean 时 |
| **输出形式** | 排除整个 Auto-Configuration 类 | 排除该类中的特定 @Bean 方法 |
| **影响因素** | 类路径、Environment、包名匹配 | Spring 容器中的 Bean 定义、类型、属性等 |

### AutoConfigurationImportFilter 过滤的局限性

`AutoConfigurationImportFilter` 只能基于类名做粗略过滤。它不能做到：

- "如果有 DataSource Bean，才注册 DataSourceAutoConfiguration" —— 这是 `@ConditionalOnBean` 的工作
- "如果 classpath 中有 HikariCP，才注册 HikariCP 配置" —— 这是 `@ConditionalOnClass` 的工作

所以 `AutoConfigurationImportFilter` 是一个**粗过滤器**（提前排除明显不需要的类），`@Conditional` 是**精过滤器**（在 Bean 注册时做细粒度控制）。

### 在 18-dynamic 中的配合

```
AutoConfigurationImportFilter 粗过滤:
  排除所有包名不在 basePackages 中的 Auto-Configuration 类
  例：保留 DataSourceAutoConfiguration，排除 MybatisPlusAutoConfiguration（如果 MP 不在 basePackages 中）

@Conditional 细过滤（Spring Boot 标准机制）:
  保留的 Auto-Configuration 类中的 @Bean 方法再逐条检查条件
  例：DataSourceAutoConfiguration 中的 HikariDataSource 方法检查 classpath 中是否存在 HikariCP
```

---

## 18-dynamic 的 4 组件协作模式

把上面所有内容串联起来，18-dynamic 的 Auto-Configuration 过滤是通过 4 个组件协作完成的：

```
              第一阶段：标准 Auto-Configuration 扫描
              ┌──────────────────────────────────────────┐
              │  @EnableDynamicJdbcAutoConfiguration      │
              │    → @Import(ImportSelector)              │
              │      → selectImports()                    │
              │        → 检查缓存                         │
              │          ├─ 命中 → 直接返回               │
              │          └─ 未命中 → super.selectImports()│
              └──────────────────────────────────────────┘
                                │
                                ▼
              第二阶段：Filter 过滤（仅在首次）
              ┌──────────────────────────────────────────┐
              │  DynamicJdbcAutoConfigurationImportFilter  │
              │  match(className) → startsWith(basePackage)│
              │  + ClassUtils.isPresent()                  │
              └──────────────────────────────────────────┘
                                │
                                ▼
              第三阶段：Listener 缓存（仅在首次）
              ┌──────────────────────────────────────────┐
              │  DynamicJdbcAutoConfigurationImportListener│
              │  onAutoConfigurationImportEvent()          │
              │    → filter::match(className)              │
              │    → Repository.cache(ClassLoader, names)  │
              └──────────────────────────────────────────┘
                                │
                                ▼
              第四阶段：Repository 查询
              ┌──────────────────────────────────────────┐
              │  DynamicJdbcAutoConfigurationRepository    │
              │  cache: ConcurrentHashMap<ClassLoader,     │
              │         String[]>                          │
              │  getAutoConfigurationClassNames()          │
              │    → ImportSelector 读（命中）             │
              │    → getModuleExclusion 读（banned-modules）│
              └──────────────────────────────────────────┘
```

### 子上下文重复创建时的协作

```
子上下文 1 创建:
  Selector 发现缓存为空
  → 执行标准的 3 阶段流程
  → Filter 过滤，Listener 缓存
  → 返回结果

子上下文 2 创建:
  Selector 发现缓存命中（同一 ClassLoader）
  → 直接返回缓存结果
  → 跳过 Filter 和 Listener

子上下文销毁:
  Selector.destroy() → Repository.clear()
  → 下次创建子上下文 3 时重新加载
```

---

## 模块互斥的实现：banned-modules

banned-modules 机制不是 Auto-Configuration 的标准功能，而是 18-dynamic 在其之上实现的模块互斥。

### 配置与执行

已经在 `18-02-database-framework-coexistence.md` 的 banned-modules 节中详细讲过。这里补充与 Auto-Configuration 机制相关的部分。

### 在 Auto-Configuration 链中的插入点

banned-modules 的排除是在第 5 步属性合成时完成的：

```
第 5 步: ConfigConfigurationPropertiesSynthesizer
  → 每个模块的 Synthesizer 合成属性
    → 调用 synthesizeModuleExclusionAutoConfigurationProperty(module)
      → getModuleExclusionAutoConfigurationClassNames(context, module)
        → 读取 banned-modules
        → 获取被禁模块的 Auto-Configuration 类名
        → 添加到 Environment 的 spring.autoconfigure.exclude 属性
```

通过将排除属性注入到 `spring.autoconfigure.exclude`，利用 Spring Boot 标准的排除机制来实现模块互斥。

```java
// AbstractConfigConfigurationPropertiesSynthesizer
protected void synthesizeModuleExclusionAutoConfigurationProperty(
        String module, Map<String, Object> properties) {

    // 获取当前模块需要排除的 Auto-Configuration 类名
    Set<String> exclusionClassNames = DynamicJdbcPropertyUtils
            .getModuleExclusionAutoConfigurationClassNames(context, module);

    // 将它们追加到 spring.autoconfigure.exclude 中
    if (!exclusionClassNames.isEmpty()) {
        DynamicJdbcPropertyUtils.appendCommaDelimitedPropertyValues(
                properties,
                AUTO_CONFIGURE_EXCLUDE_PROPERTY_NAME,
                exclusionClassNames.toArray(new String[0]));
    }
}
```

### spring.autoconfigure.exclude 的生效机制

`ConfigurableAutoConfigurationImportFilter` 是 `microsphere-spring-boot` 提供的自定义 Filter，它的工作方式与标准 Spring Boot 的 exclude 机制不同：

- 标准 Spring Boot：`spring.autoconfigure.exclude` 在 `AutoConfigurationImportSelector.getCandidateConfigurations()` 之后、Filter 执行之前生效，是编译期/配置期静态的
- `ConfigurableAutoConfigurationImportFilter`：在 Filter 阶段动态读取 Environment 中的 `spring.autoconfigure.exclude`，支持运行时注入

```java
// 在 DynamicJdbcContextApplicationListener 中调用
addExcludedAutoConfigurationClasses(environment, classNames);
// 这个方法将排除项注入到 Environment 的 spring.autoconfigure.exclude 中
// 由 ConfigurableAutoConfigurationImportFilter 在运行时读取并生效
```

这意味着 18-dynamic 可以**在运行时动态决定排除哪些 Auto-Configuration**，不需要重启应用，也不需要修改配置文件。

### 多 config 情况下的排除

当有多个 DynamicJdbcConfig 时，还涉及父上下文的 Auto-Configuration 排除：

```java
// DynamicJdbcContextApplicationListener.appendExclusionAutoConfigurationProperty()
private void appendExclusionAutoConfigurationProperty(ConfigurableApplicationContext context) {
    // 1. 检查是否有显式配置的排除列表
    Set<String> exclusion = getMultipleContextExclusionAutoConfigurationClassNames(environment);

    // 2. 如果没有显式配置，自动收集所有模块的 Auto-Configuration 类
    if (exclusion.isEmpty()) {
        exclusion = getAllModulesAutoConfigurationClassNames(context);
    }

    // 3. 注入到父上下文的 Environment
    addExcludedAutoConfigurationClasses(environment, exclusion);
}
```

这确保了父上下文不会加载任何子上下文中的模块相关的 Auto-Configuration——它们应该只在子上下文中加载。
