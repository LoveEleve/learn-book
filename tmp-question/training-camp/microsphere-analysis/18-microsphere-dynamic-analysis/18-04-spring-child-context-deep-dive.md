# 18-04 Spring 子上下文深度

## 目录

- [ApplicationContext 的生命周期](#applicationcontext-的生命周期)
- [AnnotationConfigApplicationContext 的构造过程](#annotationconfigapplicationcontext-的构造过程)
- [父子上下文的 Environment 合并](#父子上下文的-environment-合并)
- [子上下文的 Bean 升迁机制](#子上下文的-bean-升迁机制)
- [基础设施 Bean 的识别与过滤](#基础设施-bean-的识别与过滤)
- [子上下文的关闭传播](#子上下文的关闭传播)
- [OnceMainApplicationPreparedEventListener 的防重复机制](#oncemainapplicationpreparedeventlistener-的防重复机制)
- [18-dynamic 的上下文层次总结](#18-dynamic-的上下文层次总结)

---

## ApplicationContext 的生命周期

要理解 18-dynamic 的子上下文机制，必须先理解 Spring `ApplicationContext.refresh()` 的标准生命周期。无论父上下文还是子上下文，`refresh()` 都遵循同样的 13 步流程。

注意：`ApplicationContext` 的构造（设置 ID、创建 BeanFactory）发生在 `refresh()` 之前，不是 `refresh()` 的一部分。下面从 `refresh()` 的第一个内部步骤开始。

### refresh() 的 12 步生命周期

```
AbstractApplicationContext.refresh() 的 13 步：

1. prepareRefresh
   ├─ 设置启动时间
   ├─ 设置 active 标志
   └─ 初始化 PropertySource（留给子类扩展）

2. obtainFreshBeanFactory
   └─ 刷新 BeanFactory（GenericApplicationContext 中已经是 DefaultListableBeanFactory）

3. prepareBeanFactory
   ├─ 设置 ClassLoader
   ├─ 添加 ApplicationContextAwareProcessor
   ├─ 注册几个特殊的 Bean（environment, systemProperties 等）
   └─ 注册 LoadTimeWeaver（如果启用了）

4. postProcessBeanFactory (模板方法, 子类在此扩展)
   └─ DynamicJdbcChildContext 在这里插入：
       ├─ 设置 ClassLoader = parent.getClassLoader()
       ├─ prepareEnvironment() → 调用 ConfigurationPropertySources.attach()
       ├─ 注册 DynamicJdbcChildContextRefreshedListener
       ├─ 注册配置类（DynamicJdbcChildContextConfiguration）
       ├─ customizeBeanFactory() (子类可重写)
       └─ processDynamicJdbcChildContext() → 执行 6 步管道

5. invokeBeanFactoryPostProcessors
   ├─ 执行 BeanDefinitionRegistryPostProcessor（包括 ConfigurationClassPostProcessor）
   │   → 处理 @Configuration、@Import、@ComponentScan、@Bean
   │   → 这里 DynamicJdbcContextProcessor 第 1 步注册的
   │     AnnotationConfigUtils.registerAnnotationConfigProcessors() 开始生效
   └─ 执行 BeanFactoryPostProcessor

6. registerBeanPostProcessors
   ├─ 注册 BeanPostProcessor（按优先级排序）
   └─ 包括 AutowiredAnnotationBeanPostProcessor、CommonAnnotationBeanPostProcessor 等

7. initMessageSource
   └─ 初始化国际化资源

8. initApplicationEventMulticaster
   └─ 初始化事件广播器（SimpleApplicationEventMulticaster）

9. onRefresh (模板方法)
   └─ AnnotationConfigApplicationContext 中没有特殊实现
       DynamicJdbcChildContext 也没有重写

10. registerListeners
    └─ 注册 ApplicationListener（包括第 4 步注册的 DynamicJdbcChildContextRefreshedListener）
    └─ 广播早期事件

11. finishBeanFactoryInitialization
    └─ 实例化所有非懒加载 singleton Bean
    ├─ 包括 DynamicDataSource（第 5 步注册的 BeanDefinition）
    ├─ 包括 PlatformTransactionManager
    ├─ 包括 SqlSessionFactory
    └─ 包括 ShardingSphereDataSource（如果配置了）

12. finishRefresh
    └─ 初始化 LifecycleProcessor
    ├─ 发布 ContextRefreshedEvent
    ├─ 初始化 JMX
    └─ DynamicJdbcChildContext 重写了这个：
        logger.info("{} finishes refreshing", getId());

13. 就绪
    └─ ApplicationContext 可以被客户端使用了
```

### 18-dynamic 利用了哪些步骤

| 步骤 | 利用方式 |
|------|---------|
| 第 4 步 postProcessBeanFactory | DynamicJdbcChildContext 的重写——设置 ClassLoader、注册 Listener、注册配置类、执行 6 步管道 |
| 第 5 步 invokeBeanFactoryPostProcessors | DynamicJdbcContextProcessor 第 1 步注册的 AnnotationConfigUtils 在此生效，`@EnableDynamicJdbcAutoConfiguration` 中的 ImportSelector 被处理 |
| 第 11 步 finishBeanFactoryInitialization | DynamicDataSource 在此实例化（afterPropertiesSet 创建子上下文）；ShardingSphere 的 ModeConfiguration、MyBatis 的 SqlSessionFactory 也在此初始化 |
| 第 12 步 finishRefresh | DynamicJdbcChildContextRefreshedListener 收到 ContextRefreshedEvent，执行 Bean 升迁到父上下文 |

### 子上下文 vs 父上下文的关键差异

父上下文（主 Spring Boot 应用）：
- 由 `SpringApplication.run()` 创建
- 类型通常是 `AnnotationConfigApplicationContext` 或 `SpringBootApplicationContext`
- 有完整的 `@SpringBootApplication` 处理
- Environment 来自 `application.properties` + 各种 PropertySource

子上下文（18-dynamic 创建）：
- 手动 `new DynamicJdbcChildContext()`
- 类型是 `DynamicJdbcChildContext extends AnnotationConfigApplicationContext`
- 不扫描 `@SpringBootApplication`，只加载 `@EnableDynamicJdbcAutoConfiguration`
- Environment 继承自父上下文 + 合成的 PropertySource
- 关闭时由父上下文触发关闭

---

## AnnotationConfigApplicationContext 的构造过程

`DynamicJdbcChildContext` 继承了 `AnnotationConfigApplicationContext`，这个父类的构造过程值得深究——因为它不是简单的"new 一个对象"。

### 三层构造链

```java
// 调用链
new DynamicJdbcChildContext(dynamicJdbcConfig, propertyName, parentContext)
  → super(dynamicJdbcConfig, propertyName, parentContext, DynamicJdbcChildContextIdGenerator.DEFAULT)
    → this.dynamicJdbcConfig = dynamicJdbcConfig;
    → this.dynamicJdbcConfigPropertyName = dynamicJdbcConfigPropertyName;
    → this.parentContext = parentContext;
    → this.setId(idGenerator.generate(...));

// 调用 super(dynamicJdbcConfig, propertyName, parentContext, generator)
// 没有显式调用父类 AnnotationConfigApplicationContext() 构造函数
// 所以父类会走无参构造 → 调用 this() → this()
```

实际上，Java 中在子类构造函数中如果没有显式调用 `super(...)`，会隐式调用 `super()`。`DynamicJdbcChildContext` 的两个构造函数：

```java
// 构造函数 A：接受 IdGenerator
public DynamicJdbcChildContext(DynamicJdbcConfig config, String propertyName,
        ConfigurableApplicationContext parentContext, DynamicJdbcChildContextIdGenerator generator) {
    // 注意：这里没有显式调用 super()
    // Java 编译器会隐式插入 super() → AnnotationConfigApplicationContext()
    this.dynamicJdbcConfig = config;
    this.dynamicJdbcConfigPropertyName = propertyName;
    this.parentContext = parentContext;
    String id = generator.generate(config, propertyName, parentContext);
    this.setId(id);
}

// 构造函数 B：使用默认 IdGenerator
protected DynamicJdbcChildContext(DynamicJdbcConfig config, String propertyName,
        ConfigurableApplicationContext parentContext) {
    this(config, propertyName, parentContext, DynamicJdbcChildContextIdGenerator.DEFAULT);
}
```

关键点：构造函数 A **没有显式调用 `super()`**，所以 Java 编译器会隐式插入 `super()` 调用 `AnnotationConfigApplicationContext()` 的无参构造。这个无参构造会创建 `AnnotatedBeanDefinitionReader` 和 `ClassPathBeanDefinitionScanner`——此时 `BeanFactory` 已经存在（在 `GenericApplicationContext` 构造时已经创建）。

`AnnotationConfigApplicationContext` 的无参构造：

```java
public AnnotationConfigApplicationContext() {
    // 第一步：创建一个新的 Reader
    this.reader = new AnnotatedBeanDefinitionReader(this);
    // 第二步：创建一个新的 Scanner
    this.scanner = new ClassPathBeanDefinitionScanner(this);
}
```

### AnnotatedBeanDefinitionReader 做了什么

```java
public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
    this(registry, getOrCreateEnvironment(registry));
}

public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
    this.registry = registry;
    // 注册几个关键的 BeanPostProcessor
    registerAnnotationConfigProcessors(this.registry);
}

public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(
        BeanDefinitionRegistry registry) {

    Set<BeanDefinitionHolder> holders = new LinkedHashSet<>();

    // 注册 ConfigurationClassPostProcessor
    // 这是处理 @Configuration、@Import、@Bean 的核心
    if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
        // ...
        registry.registerBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME, def);
    }

    // 注册 AutowiredAnnotationBeanPostProcessor
    if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
        // ...
    }

    // 注册 CommonAnnotationBeanPostProcessor
    // 注册 EventListenerMethodProcessor
    // ...
}
```

此时 `BeanFactory` 已经存在（`GenericApplicationContext` 构造时已经创建了 `DefaultListableBeanFactory`）。`reader` 通过构造参数 `this`（即 `BeanDefinitionRegistry`，与 `BeanFactory` 是同一个对象）持有对 `BeanFactory` 的引用。构造完成后，`reader` 被用于注册配置类（`DynamicJdbcChildContextConfiguration`），而真正的 `@Bean` 和 `@Configuration` 处理由 `ConfigurationClassPostProcessor` 在 `refresh()` 的第 5 步执行。

### 为什么第 1 步管道要再次调用 registerAnnotationConfigProcessors

```java
// DynamicJdbcContextProcessor.java 第 1 步
private void registerAnnotationConfigProcessors(ConfigurableApplicationContext context) {
    BeanDefinitionRegistry registry = resolveBeanDefinitionRegistry(context);
    AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);
}
```

这个步骤看似重复（构造函数中已经通过 `AnnotatedBeanDefinitionReader` 注册过了），实际目的是：**确保 BeanFactory 中一定包含这些核心处理器**。

构造函数中的注册路径是 `AnnotatedBeanDefinitionReader` → `BeanDefinitionRegistry`（与 BeanFactory 是同一个对象）。所以实际上构造时这些处理器已经被注册到 BeanFactory 了。第 1 步的再次注册是防御性编程——如果由于某些原因（如自定义 BeanFactory 实现、子类覆盖了注册逻辑）构造时的注册没有生效，第 1 步可以补救。

由于 `AnnotationConfigUtils.registerAnnotationConfigProcessors` 内部有 `containsBeanDefinition` 检查，重复调用也不会重复注册。

---

## 父子上下文的 Environment 合并

子上下文要从父上下文继承 Environment，但不是简单的复制。

### mergeParentEnvironment 的实现

```java
// DynamicJdbcChildContext.java
public void mergeParentEnvironment() {
    ConfigurableEnvironment environment = getEnvironment();
    ConfigurableEnvironment parentEnvironment = parentContext.getEnvironment();
    if (parentEnvironment != null) {
        environment.merge(parentEnvironment);
    }
    removeSynthesizedPropertySources(environment);
    detachConfigurationPropertySources(environment);
}
```

### ConfigurableEnvironment.merge 的语义

`ConfigurableEnvironment.merge()` 是 Spring 5.x 引入的方法，它的合并规则是：

```java
// AbstractEnvironment.merge(ConfigurableEnvironment parent)
// 规则：父环境的 PropertySource 添加到子环境中，按照"父环境的顺序"插入到子环境的前面

public void merge(ConfigurableEnvironment parent) {
    // 1. 合并 activeProfiles 和 defaultProfiles
    // 2. 合并 PropertySource
    MutablePropertySources parentSources = parent.getPropertySources();
    MutablePropertySources mySources = getPropertySources();

    // 从父环境的最后一个 PropertySource 开始，逐个插入到子环境的最前面
    List<PropertySource<?>> list = new ArrayList<>();
    for (PropertySource<?> ps : parentSources) {
        list.add(ps);
    }
    // 反转顺序后插入
    for (int i = list.size() - 1; i >= 0; i--) {
        PropertySource<?> ps = list.get(i);
        if (!mySources.contains(ps.getName())) {
            mySources.addLast(ps);
        }
    }
}
```

合并后的 PropertySource 顺序：

```
子环境的 PropertySource（高优先级）
  ├─ 子环境的合成 PropertySource（来自 6 步管道第 5 步）
  ├─ 子环境的配置 PropertySource
  │
  ├─ 父环境的 PropertySource #1（原来在父环境最后）
  ├─ 父环境的 PropertySource #2
  ├─ ...
  └─ 父环境的 PropertySource #N（原来在父环境最前，现在在子环境最底层）
  父环境的 PropertySource（低优先级）
```

核心规则：**子环境自有的 PropertySource 优先级高于父环境的，父环境的 PropertySource 按原有顺序保留在子环境中**。

### removeSynthesizedPropertySources 的作用

合并后，父环境中的合成 PropertySource 也会出现在子环境中。但某些合成 PropertySource 是父上下文特有的（如父上下文的 `spring.autoconfigure.exclude`），不应该影响子上下文的 Auto-Configuration。

```java
private void removeSynthesizedPropertySources(ConfigurableEnvironment environment) {
    String[] propertyNames = {SPRING_AUTO_CONFIGURE_EXCLUDE_PROPERTY_NAME};
    for (String propertyName : propertyNames) {
        String propertySourceName = generateSynthesizedPropertySourceName(propertyName);
        if (propertySources.contains(propertySourceName)) {
            propertySources.remove(propertySourceName);
        }
    }
}
```

这确保了子上下文的 `spring.autoconfigure.exclude` 不受父上下文的排除策略影响——子上下文自己决定需要排除哪些 Auto-Configuration。

### detachConfigurationPropertySources 的作用

```java
private void detachConfigurationPropertySources(ConfigurableEnvironment environment) {
    MutablePropertySources sources = environment.getPropertySources();
    PropertySource<?> attached = sources.get(ATTACHED_PROPERTY_SOURCE_NAME);
    if (attached != null && attached.getSource() != sources) {
        sources.remove(ATTACHED_PROPERTY_SOURCE_NAME);
    }
}
```

`ConfigurationPropertySources.attach()` 在 Spring Boot 2.x 中用一个新的 PropertySource 包裹原有的 MutablePropertySources。合并后，子环境的 `ATTACHED_PROPERTY_SOURCE_NAME` 可能引用了父环境的 PropertySources。这段代码检测到这种"跨上下文引用"后移除它，让子上下文在 next `attach()` 时重新绑定到自己的 PropertySources。

### 子上下文 Environment 的最终结构

```
子环境的 PropertySources（从上到下优先级递减）：

1. SynthesizedPropertySource[spring.autoconfigure.exclude]  ← 第 5 步添加的排除属性
2. SynthesizedPropertySource[microsphere.dynamic.jdbc.configs.*]  ← 第 5 步合成的模块配置
3. 子环境的 application.properties（如果有）                ← 子环境自带
4. (来自父环境的) application.properties                    ← merge 后
5. (来自父环境的) bootstrap.properties（如果有 Spring Cloud）← merge 后
6. (来自父环境的) systemProperties                          ← merge 后
7. (来自父环境的) systemEnvironment                         ← merge 后
8. (来自父环境的) random                                    ← merge 后

越靠上优先级越高。
```

---

## 子上下文的 Bean 升迁机制

子上下文的 Bean 默认只在该子上下文内可见，对父上下文不可见。但父上下文中的服务（如 Controller、Service）可能需要注入子上下文中的 DataSource、TransactionManager、SqlSessionFactory。这就需要"Bean 升迁"——将子上下文的 Bean 注册到父上下文。

### 触发时机

```java
// DynamicJdbcChildContext.postProcessBeanFactory() 中
addApplicationListener(new DynamicJdbcChildContextRefreshedListener(
    dynamicJdbcConfig, parentContext, beanFactory, registerParentBeans));
```

Listener 在 `postProcessBeanFactory`（第 4 步）时注册，`finishRefresh`（第 12 步）发布 `ContextRefreshedEvent` 时被触发。

### 升迁的完整流程

```
子上下文 finishRefresh
  → ContextRefreshedEvent
    → DynamicJdbcChildContextRefreshedListener.onApplicationEvent()
      ├─ registerParentContextClosedEventListener()
      │   → 父上下文关闭时，关闭本子上下文
      │
      └─ registerParentBeansFromChildContext()
          └─ registerParentBeans == true 时才执行
              ├─ 遍历子上下文的所有 BeanDefinition 名称
              │   └─ 跳过基础设施 Bean
              ├─ childBean = context.getBean(beanName)
              ├─ parentBeanName = 生成父 Bean 名
              │   ├─ 遍历 ParentContextBeanNameGenerator SPI
              │   │   └─ PlatformTransactionManagerBeanNameGenerator
              │   │       → 如果是 PlatformTransactionManager，用 transaction.name
              │   └─ 默认: childContextId + "$" + beanName
              └─ registerParentBean(parentBeanName, childBean)
                  ├─ 如果是 exposed 类 → registerBean(registry, name, bean, primary)
                  │   → 直接注册为普通 Bean
                  └─ 如果不是 → registerFactoryBean(registry, name, bean)
                      → 包装为 FactoryBean 注册
```

### 为什么需要两种注册方式

```java
private void registerParentBean(String parentBeanName, Object childBean) {
    if (isExposedBeanClass(childBean)) {
        boolean primaryBean = isPrimaryBean(childBean);
        registerBean(parentBeanDefinitionRegistry, parentBeanName, childBean, primaryBean);
    } else {
        registerFactoryBean(parentBeanDefinitionRegistry, parentBeanName, childBean);
    }
}
```

**直接注册（普通 Bean）**：
- 适用于 DataSource、PlatformTransactionManager、SqlSessionFactory 等标准接口
- 直接用 `BeanDefinitionRegistry.registerBeanDefinition()` 注册已存在的实例
- 父上下文可以直接按类型注入（`@Autowired DataSource`）
- 支持 `@Primary` 标记

**FactoryBean 注册**：
- 适用于其他类型的 Bean
- 用 `GenericBeanDefinition` 包装，setBeanClass 为 `FactoryBean`
- 父上下文中按类型注入时需要指定具体的类名
- 不标记 `@Primary`

### exposed 和 primary 的控制

通过配置控制哪些类型的 Bean 可以被暴露、哪些可以标记为 primary：

```properties
# 哪些类型的 Bean 直接暴露
microsphere.dynamic.jdbc.multiple-context.bean-classes.expose=\
  javax.sql.DataSource,\
  org.springframework.transaction.PlatformTransactionManager,\
  org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers,\
  org.apache.ibatis.session.SqlSessionFactory

# 哪些类型的 Bean 在 primary 标记时生效
microsphere.dynamic.jdbc.multiple-context.bean-classes.primary=\
  javax.sql.DataSource,\
  org.springframework.transaction.PlatformTransactionManager,\
  org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers,\
  org.apache.ibatis.session.SqlSessionFactory
```

```java
private boolean isExposedBeanClass(Object childBean) {
    for (Class<?> clazz : multipleContextExposedBeanClasses) {
        if (clazz.isInstance(childBean)) {
            return true;
        }
    }
    return false;
}

private boolean isPrimaryBean(Object childBean) {
    if (!dynamicJdbcConfig.isPrimary()) {
        return false;
    }
    for (Class<?> clazz : multipleContextPrimaryBeanClasses) {
        if (clazz.isInstance(childBean)) {
            return true;
        }
    }
    return false;
}
```

### 多 config 下 Bean 名的冲突处理

如果有两个 config，都暴露了 `DataSource` 类型的 Bean，父上下文中会有两个 DataSource Bean。通过不同的 Bean 名区分：

```
dynamicJdbcConfigPropertyName = "microsphere.dynamic.jdbc.configs.orders"
childContextId = "DynamicJdbcChildContext[orders]"

→ 生成的父 Bean 名：
  DynamicJdbcChildContext[orders]$dataSource
  DynamicJdbcChildContext[orders]$transactionManager
```

但是 `@Autowired DataSource` 在注入时是按类型匹配的，如果存在多个 DataSource Bean 且没有 `@Primary`，会抛 `NoUniqueBeanDefinitionException`。

有两种解决方式：
1. 在配置中设置 `"primary": true`，该 config 暴露的 Bean 会被标记为 `@Primary`
2. 注入时使用 `@Qualifier` + 生成的 Bean 名（如 `@Qualifier("DynamicJdbcChildContext[orders]$dataSource")`）

注意方式 2 依赖框架生成的 Bean 名，这个名称可能随代码版本变化，建议在非必要场景下优先使用方式 1。

---

## 基础设施 Bean 的识别与过滤

子上下文中有许多 Spring 内部的基础设施 Bean（如 `environment`、`systemProperties`、`messageSource`、`applicationEventMulticaster` 等），这些 Bean 不应该被升迁到父上下文。

### 识别方式

```java
// DynamicJdbcChildContextRefreshedListener 构造函数
this.infrastructureBeanNames = findInfrastructureBeanNames(childContextBeanFactory);
```

`findInfrastructureBeanNames` 来自 `microsphere-spring-boot` 的 `BeanDefinitionUtils`：

```java
public static Set<String> findInfrastructureBeanNames(ConfigurableListableBeanFactory beanFactory) {
    Set<String> infrastructureBeanNames = new HashSet<>();

    // 1. 所有 BeanPostProcessor 都是基础设施
    String[] bppNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);
    infrastructureBeanNames.addAll(Arrays.asList(bppNames));

    // 2. 所有 BeanFactoryPostProcessor 都是基础设施
    String[] bfppNames = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);
    infrastructureBeanNames.addAll(Arrays.asList(bfppNames));

    // 3. 已知的 infrastructure bean names（常量列表）
    infrastructureBeanNames.addAll(Arrays.asList(
        "environment",
        "systemProperties",
        "systemEnvironment",
        "messageSource",
        "applicationEventMulticaster",
        "lifecycleProcessor",
        // ...
    ));

    // 4. 通过 InfrastructureRole 判断
    // 如果 BeanDefinition 的 role == ROLE_INFRASTRUCTURE，也是基础设施
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
        BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
        if (bd.getRole() == BeanDefinition.ROLE_INFRASTRUCTURE) {
            infrastructureBeanNames.add(beanName);
        }
    }

    return infrastructureBeanNames;
}
```

### 升迁时的过滤

```java
// 升迁循环
String[] childBeanDefinitionNames = childContextBeanFactory.getBeanDefinitionNames();
for (String beanName : childBeanDefinitionNames) {
    if (isInfrastructureBean(beanName)) {
        continue;  // 跳过基础设施
    }
    Object childBean = childContextBeanFactory.getBean(beanName);
    // ↑ 这行会实例化所有非基础设施 Bean（包括懒加载的）
    // 因为 getBean() 触发 Bean 的初始化，副作用是 Bean 中的 @PostConstruct、InitializingBean 等都会执行
    // 对于 DynamicDataSource：getBean() 会触发其 afterPropertiesSet() → 创建子上下文
    // 但真正建连（HikariCP）仍延迟到首次 getConnection()
    String parentBeanName = generateParentBeanName(...);
    registerParentBean(parentBeanName, childBean);
}
```

### 哪些 Bean 会被升迁

通过过滤后，被升迁的是**业务 Bean**——用户配置的模块所产生的 Bean：

| 模块 | 升迁的 Bean | 是否 exposed | 是否 primary |
|------|------------|-------------|-------------|
| DataSource（动态） | DynamicDataSource（本身） | 是 | 由 primary 配置决定 |
| Transaction | PlatformTransactionManager | 是 | 由 primary 配置决定 |
| MyBatis | SqlSessionFactory | 是 | 由 primary 配置决定 |
| Transaction | TransactionManagerCustomizers | 是 | 由 primary 配置决定 |
| 其他模块 Bean | 各模块的具体 Bean | 否（FactoryBean） | 否 |

---

## 子上下文的关闭传播

子上下文的 Bean 被升迁到父上下文后，存在一个生命周期问题：父上下文关闭时，子上下文使用的资源（连接池、线程池）需要同步关闭。

### 注册关闭监听器

```java
// DynamicJdbcChildContextRefreshedListener.onApplicationEvent()
private void registerParentContextClosedEventListener(ConfigurableApplicationContext childContext) {
    parentContext.addApplicationListener((ApplicationListener<ContextClosedEvent>) e -> {
        childContext.close();
    });
}
```

这段代码在父上下文中注册了一个 `ApplicationListener<ContextClosedEvent>`，当父上下文触发关闭事件时，依次关闭所有子上下文。

### Spring 的 ContextClosedEvent 广播顺序

```java
// AbstractApplicationContext.doClose()
protected void doClose() {
    // 1. 发布 ContextClosedEvent
    publishEvent(new ContextClosedEvent(this));

    // 2. 停止 LifecycleProcessor
    liveBeansView.clear();

    // 3. 销毁所有 singleton Bean
    destroyBeans();

    // 4. 关闭 BeanFactory
    closeBeanFactory();
}
```

子上下文的关闭发生在**第 1 步**（广播关闭事件时），在第 3 步（销毁 Bean）之前。这意味着子上下文的 Bean 先于父上下文 Bean 被销毁。这是正确的顺序——因为子上下文依赖于父上下文的基础设施（如父上下文的 ClassLoader、Environment），如果父上下文 Bean 先销毁，子上下文在关闭时可能会遇到 NullPointerException。

### DynamicDataSource 的延迟关闭

`DynamicDataSource` 的初始化方法（`initializeDataSource`）在完成 delegate 切换后，会异步关闭旧的子上下文：

```java
private DataSource initializeDataSource(...) {
    // ... 创建新子上下文、获取新 DataSource ...
    synchronized (mutex) {
        DataSource previousDataSource = DynamicDataSource.this.delegate;
        DynamicDataSource.this.delegate = latestDataSource;

        ConfigurableApplicationContext previousChildContext =
                DynamicDataSource.this.dynamicDataSourceChildContext;
        DynamicDataSource.this.dynamicDataSourceChildContext = dynamicDataSourceChildContext;

        // 异步关闭旧上下文（延迟 60s）
        closeDynamicDataSourceChildContext(previousChildContext, true);
    }
    return latestDataSource;
}
```

这个延迟关闭使得旧上下文中的连接池在 60s 内继续存活，正在使用旧连接的线程可以继续完成操作。60s 后，`ScheduledExecutorService` 执行 `closeContext()` —— → `ConfigurableApplicationContext.close()`。

---

## OnceMainApplicationPreparedEventListener 的防重复机制

在 18-dynamic 中，`DynamicJdbcContextApplicationListener` 继承了 `OnceMainApplicationPreparedEventListener`，这个父类处理了一个棘手的 Spring Cloud 问题。

### 问题：Bootstrap 上下文导致的双倍触发

Spring Cloud（传统模式，非 2020+ 的全新加载方式）在启动时会创建两个 ApplicationContext：

```
SpringApplication.run()
  ├─ 创建 Bootstrap Context (parent)
  │   ├─ ApplicationPreparedEvent #1 → Listener 触发一次
  │   └─ ContextRefreshedEvent #1
  │
  └─ 创建 Main Application Context (child)
      ├─ ApplicationPreparedEvent #2 → Listener 再触发一次
      └─ ContextRefreshedEvent #2
```

如果不加区分，`DynamicJdbcContextApplicationListener` 会处理两次 `ApplicationPreparedEvent`——一次在 bootstrap 上下文中，一次在主上下文中。bootstrap 上下文中的处理是无效的（甚至可能导致配置被污染）。

### OnceMainApplicationPreparedEventListener 的解决

```java
public abstract class OnceMainApplicationPreparedEventListener
        extends OnceApplicationPreparedEventListener {

    @Override
    protected boolean isMainApplicationContext(ConfigurableApplicationContext context) {
        // 只处理主应用上下文，跳过 bootstrap 上下文
        return !"bootstrap".equals(context.getId())
                && !context.getId().endsWith(":bootstrap");
    }
}

public abstract class OnceApplicationPreparedEventListener
        implements ApplicationListener<ApplicationPreparedEvent>, Ordered {

    // 使用 ConcurrentSkipListSet 追踪已处理过的上下文
    private final Set<String> processedContextIds = new ConcurrentSkipListSet<>();

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();

        // 1. 检查是否是主上下文
        if (!isMainApplicationContext(context)) {
            return;
        }

        // 2. 检查是否已经处理过
        String contextId = context.getId();
        if (!processedContextIds.add(contextId)) {
            // 已经处理过，跳过
            return;
        }

        // 3. 执行真正的处理逻辑
        onApplicationEvent(event.getSpringApplication(), event.getArgs(), context);
    }
}
```

### 多层防重复

这个防重复机制实际上是多层的：

| 层 | 机制 | 防止什么 |
|---|------|---------|
| `isMainApplicationContext` | 检查 context ID 不是 "bootstrap" | 防止 bootstrap 上下文的误触 |
| `processedContextIds` (ConcurrentSkipListSet) | `add()` 返回 false 表示已存在 | 防止同一上下文的二次处理 |
| `OnceMainApplicationPreparedEventListener` 子类 | 组合上述两层 | 既防 bootstrap 又防重复 |

`ConcurrentSkipListSet` 是线程安全的，如果在特殊情况下（如测试环境）同一上下文 ID 的两次事件几乎同时到达，只有一个线程能 `add()` 成功。

---

## 18-dynamic 的上下文层次总结

18-dynamic 运行时的完整上下文层次：

```
JVM 进程
  │
  ├─ Bootstrap Context（仅 Spring Cloud 传统模式）
  │   └─ ID: "bootstrap"
  │
  ├─ Root Context（AnnotationConfigApplicationContext）
  │   └─ ID: 由 SpringApplication 生成（或自定义）
  │
  │   ├─ DynamicJdbcChildContext[orders]（子上下文 1）
  │   │   ├─ DynamicJdbcChildContext[Dynamic#orders#datasource]
  │   │   │   (DynamicDataSource 热替换时创建，旧实体会被关闭)
  │   │   └─ 业务 Bean: DataSource × 1（由 Module Auto-Configuration 创建）
  │   │                TransactionManager × 1
  │   │                SqlSessionFactory × 1（MyBatis / MyBatis-Plus）
  │   │                Mapper × N
  │   │
  │   ├─ DynamicJdbcChildContext[users]（子上下文 2，多 config 时）
  │   │   └─ ...（同上结构）
  │   │
  │   └─ 父上下文自身的业务 Bean
  │       ├─ Controller
  │       ├─ Service
  │       └─ ...（通过 @Resource/@Autowired 注入子上下文升迁的 Bean）
  │
  └─ 所有上下文共享
      ├─ JVM ClassLoader
      ├─ System Properties
      └─ System Environment
```

### 上下文的 ID 规范

| 类型 | ID 模式 | 示例 |
|------|---------|------|
| Root Context | SpringApplication 生成 | `application-1` 或自定义 |
| 子上下文（config 驱动） | `DynamicJdbcChildContext[{name}]` | `DynamicJdbcChildContext[orders]` |
| 子上下文（DataSource 热替） | `Dynamic#DynamicJdbcChildContext[{name}]#datasource` | `Dynamic#DynamicJdbcChildContext[orders]#datasource` |

### 关键数字

| 指标 | 值 |
|------|-----|
| 每个子上下文的创建时间 | 1-3 秒（取决于 Auto-Configuration 数量） |
| 每个子上下文的额外内存 | 20-150 MB（取决于模块配置：仅 DataSource ~20MB，含 MyBatis+Transaction ~50-80MB，含 ShardingSphere ~80-150MB） |
| DynamicDataSource 切换时间 | 1-3 秒（子上下文重建） |
| DynamicDataSource 旧上下文延迟关闭 | 默认 60 秒 |
| 多 config 并行创建 | 每个 config 一个线程 |

### 3 级上下文层次的设计意图

```
Root Context（应用入口）
  │  职责：管理应用的生命周期、安全、全局配置
  │  持有的 Bean：Controller、全局 Service、配置属性
  │
  ├─ Child Context（数据库单元）
  │  职责：隔离数据库配置、ORM 框架版本、事务管理器
  │  持有的 Bean：DataSource、TransactionManager、SqlSessionFactory、Mapper
  │
  └─ Dynamic Child Context（热替单元）
     职责：每次配置变更或 Zone 切换时重建
     持有的 Bean：HikariDataSource、完整的 JDBC 相关 Bean
```

这三级上下文对应了三个抽象层次：
- **Root** = 应用（Application）
- **Child** = 数据库单元（Database Unit）
- **Dynamic** = 具体的数据库连接（Connection Pool Instance）

每个层次有自己独立的生命周期和关注点，互不干扰。
