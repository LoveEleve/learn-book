# 03-02 并行 Bean 初始化

## 问题：`preInstantiateSingletons` 是串行的

Spring 的 `DefaultListableBeanFactory#preInstantiateSingletons` 按注册顺序逐个实例化非延迟单例 Bean：

```java
// Spring 源码（简化）
public void preInstantiateSingletons() {
    for (String beanName : beanDefinitionNames) {
        if (!isSingleton(beanName) || isLazyInit(beanName)) continue;
        getBean(beanName);  // 串行调用
    }
}
```

在大规模 Spring 应用中（数百甚至数千个 Bean），这个串行过程成为启动瓶颈。典型场景：

- **微服务应用**：200+ Bean，启动时间 30-60 秒，其中 Bean 实例化占 40-60%
- **Spring Cloud 应用**：注册中心客户端、配置中心客户端、熔断器等基础设施 Bean 初始化耗时叠加
- **测试环境**：每次启动应用等待数分钟，严重降低开发效率

直觉上，如果两个 Bean 之间没有依赖关系，它们可以并行实例化。但 Spring 没有做这件事，原因是：

1. **依赖分析成本**：Spring 在运行时通过 `AutowiredAnnotationBeanPostProcessor` 逐个 Bean 解析依赖，没有预先构建全局依赖图
2. **线程安全顾虑**：`getSingleton` 虽然是线程安全的，但 Bean 创建过程中的副作用（如 `BeanPostProcessor` 注册）可能不是
3. **设计哲学**：Spring 的设计倾向于"可预测的串行"而非"可能出问题的并行"

microsphere-spring 通过第 1 篇的 `BeanFactoryListener#onBeanFactoryConfigurationFrozen` 回调，在 `preInstantiateSingletons` 之前**预先创建**所有 Bean，让 Spring 的 `preInstantiateSingletons` 变为空操作（因为 Bean 已经存在）。

---

## 设计：依赖图 -> 连通分量 -> 并行执行

### 整体流程

```
onBeanFactoryConfigurationFrozen()
    │
    ├── 1. 创建线程池（默认 CPU 核心数线程）
    │
    ├── 2. 依赖分析（DefaultBeanDependencyResolver）
    │       ├── 过滤符合条件的 BeanDefinition（非抽象、单例、非延迟、无 InstanceSupplier）
    │       ├── 并行预加载 Bean Class
    │       ├── 并行解析每个 Bean 的依赖
    │       └── 展平传递依赖，移除被依赖的 Bean（只保留"根 Bean"）
    │
    ├── 3. 计算连通分量（resolveBeanNamesInDependencyPaths）
    │       ├── 每个 Bean + 其依赖集合 = 一个集合
    │       ├── 有交集的集合合并（连通分量）
    │       └── 每个连通分量 = 一条"依赖路径"
    │
    ├── 4. 并行实例化
    │       ├── 每条依赖路径提交为一个 Task
    │       ├── Task 内部按顺序 getBean()（保证依赖顺序）
    │       └── 多条路径并行执行
    │
    └── 5. 等待所有 Task 完成，关闭线程池
```

核心思想是：**将 Bean 依赖关系建模为无向图，找到连通分量，每个连通分量串行执行，分量之间并行执行**。

---

### 依赖分析：四个来源

`DefaultBeanDependencyResolver` 从四个来源解析每个 Bean 的依赖：

| 来源 | 解析方式 | 示例 |
|------|---------|------|
| **BeanDefinition 元数据** | `getDependsOn()` + `PropertyValues` 中的 `BeanReference` + `factoryBeanName` | `@DependsOn("b")`、XML `<ref bean="b"/>` |
| **构造器参数** | 通过 `SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors` 确定构造器，解析参数类型 | `class A { A(B b) {} }` |
| **@Bean 方法参数** | 从 `RootBeanDefinition#getResolvedFactoryMethod` 获取工厂方法，解析参数类型 | `@Bean A a(B b) { ... }` |
| **注入点（字段+方法）** | 通过 `InjectionPointDependencyResolvers` 解析 `@Autowired`/`@Resource`/`@Inject` | `@Autowired private B b;` |

#### BeanDefinition 元数据解析

最直接的依赖来源。`RootBeanDefinition` 本身记录了三种依赖信息：

- **`depends-on`**：`@DependsOn("b")` 或 XML `depends-on="b"` 声明的显式依赖
- **`PropertyValues` 中的 `BeanReference`**：XML `<property name="x" ref="b"/>` 声明的属性引用
- **`factoryBeanName`**：`@Bean` 方法所在配置类的 Bean 名称

这三类依赖不需要反射分析 Bean Class，直接从 BeanDefinition 元数据读取。

#### 构造器参数解析

构造器参数的依赖解析需要确定 Bean 使用哪个构造器。Spring 在 `preInstantiateSingletons` 阶段通过 `SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors` 确定构造器，但依赖分析阶段不能调用这个方法（因为 Bean 还未实例化）。

microsphere 的处理方式：

1. 遍历 `BeanPostProcessors` 列表，找到所有 `SmartInstantiationAwareBeanPostProcessor`
2. 依次调用 `determineCandidateConstructors(beanClass, beanName)`
3. 第一个返回非 null 的 Processor 的结果被采用
4. 如果所有 Processor 都返回 null，使用 `beanClass.getConstructors()`（public 构造器）
5. 如果 public 构造器为空，使用 `getDeclaredConstructors()`（包括非 public）

**多构造器问题**：如果 Bean 有多个构造器且 `determineCandidateConstructors` 返回 null（没有 `@Autowired` 构造器），解析器无法确定使用哪个。此时 microsphere **跳过构造器参数依赖解析**并记录告警日志。这意味着多构造器 Bean 的依赖可能不完整。

#### @Bean 方法参数解析

`@Bean` 方法的依赖解析相对简单：`RootBeanDefinition#getResolvedFactoryMethod` 直接返回工厂方法（Spring 在 `ConfigurationClassParser` 阶段已解析）。拿到 `Method` 后，解析其参数类型。

`BeanMethodInjectionPointDependencyResolver` 只处理标注了 `@Bean` 的方法参数，其他方法参数被跳过。

#### 注入点解析：InjectionPointDependencyResolver 体系

这是最复杂的依赖来源。`@Autowired`、`@Resource` 等注解的依赖关系不会记录在 BeanDefinition 中，只有运行时通过 `AutowiredAnnotationBeanPostProcessor` 解析。microsphere 在 Bean 实例化前通过 `InjectionPointDependencyResolver` 体系预先解析这些依赖。

**体系结构**：

```
InjectionPointDependencyResolver (接口)
    │
    ├── InjectionPointDependencyResolvers (复合，通过 SpringFactoriesLoader 加载)
    │       │
    │       ├── ConstructionInjectionPointDependencyResolver  (构造器参数)
    │       ├── AutowiredInjectionPointDependencyResolver     (@Autowired)
    │       └── ResourceInjectionPointDependencyResolver      (@Resource)
    │
    └── AbstractInjectionPointDependencyResolver (抽象基类)
            │
            └── AnnotatedInjectionPointDependencyResolver<A> (注解驱动基类)
                    │
                    ├── AutowiredInjectionPointDependencyResolver
                    └── ResourceInjectionPointDependencyResolver
```

三个解析器通过 `META-INF/spring.factories` 注册：

```properties
io.microsphere.spring.beans.factory.InjectionPointDependencyResolver=\
io.microsphere.spring.beans.factory.ConstructionInjectionPointDependencyResolver,\
io.microsphere.spring.beans.factory.annotation.AutowiredInjectionPointDependencyResolver,\
io.microsphere.spring.beans.factory.annotation.ResourceInjectionPointDependencyResolver
```

`InjectionPointDependencyResolvers` 在构造时通过 `SpringFactoriesLoader.loadFactories` 加载所有实现，组成复合解析器。调用时，每个解析器依次处理注入点，将结果累加到 `Set<String> dependentBeanNames` 中。

#### 解析策略：先名后型

`AbstractInjectionPointDependencyResolver` 对每个注入点（Field/Parameter）采用两步解析：

**第一步：按名解析**

通过 `AutowireCandidateResolver#getSuggestedValue(DependencyDescriptor)` 获取建议值。这个方法返回 `@Qualifier("beanName")` 指定的名称，或 `@Resource(name="beanName")` 指定的名称。如果返回值是 `String`，直接作为依赖 Bean 名称。

**第二步：按型解析**

如果按名解析失败（返回 null），则按类型查找：

```java
String[] beanNames = beanFactory.getBeanNamesForType(dependentType, false, false);
addAll(dependentBeanNames, beanNames);
```

注意 `includeNonSingletons=false`（不包含非单例）和 `allowEagerInit=false`（不触发其他 Bean 的初始化）。这两个参数确保依赖分析不会产生副作用。

#### 参数化类型的解析

注入点的类型可能是参数化类型，如 `List<B>`、`Map<String, B>`、`Optional<B>`、`ObjectProvider<B>`。`AbstractInjectionPointDependencyResolver#resolveDependentType` 递归解析：

```
B               -> B.class
B[]             -> B.class（数组取组件类型）
List<B>         -> B.class（取最后一个类型参数）
Map<String, B>  -> B.class（取最后一个类型参数）
Optional<B>     -> B.class（取最后一个类型参数）
ObjectProvider<B> -> B.class（取最后一个类型参数）
List<Map<String, B>> -> B.class（递归取最后一个类型参数）
```

**为什么取最后一个类型参数**：Spring 的 `@Autowired List<B>` 注入所有 B 类型的 Bean，`@Autowired Map<String, B>` 注入所有 B 类型的 Bean（key 为 bean name）。两种情况的实际依赖类型都是 B（最后一个类型参数），而非第一个。

#### @Resource 的特殊处理

`ResourceInjectionPointDependencyResolver` 对 `@Resource` 的处理与 `@Autowired` 不同：

- **`@Resource(type=MyClass.class)`**（显式指定类型）：按 `MyClass.class` 查找 Bean 名称
- **`@Resource(name="myBean")`**（显式指定名称）：直接使用 `"myBean"` 作为依赖名称
- **`@Resource`（默认，name="" 且 type=Object.class）**：使用字段名（`Introspector.decapitalize(fieldName)`）作为依赖名称

这与 Spring 原生 `@Resource` 的语义一致：`@Resource` 默认按名称匹配，`@Autowired` 默认按类型匹配。

#### ResolvableDependencyTypeFilter

Spring 通过 `ConfigurableListableBeanFactory#registerResolvableDependency` 注册了非 Bean 的容器内对象：

```java
beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
beanFactory.registerResolvableDependency(ApplicationContext.class, context);
beanFactory.registerResolvableDependency(ResourceLoader.class, context);
beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, context);
```

这些类型不是常规 Bean，不应该出现在依赖图中。`ResolvableDependencyTypeFilter` 通过反射读取 `DefaultListableBeanFactory` 的 `resolvableDependencies` 字段，构建类型集合。解析注入点时，如果参数类型是这些类型之一（`isAssignableFrom`），跳过。

这个过滤至关重要：如果不过滤，一个 `@Autowired ApplicationContext` 的注入点会将所有 Bean 连接到 `ApplicationContext` 类型，导致整个依赖图变为一个连通分量，完全失去并行性。

---

### 展平传递依赖

依赖分析产生的是**直接依赖**：A 依赖 B，B 依赖 C。但并行执行需要的是**传递依赖**（展平后的完整依赖链）：A 依赖 {B, C}。

`flattenDependentBeanNamesMap` 通过递归展平：

```
直接依赖：A -> {B}, B -> {C}, C -> {}
展平后：  A -> {B, C}, B 被移除（因为 A 依赖它）, C 被移除（因为 A 和 B 依赖它）

最终只保留"根 Bean"（不被其他 Bean 依赖的 Bean）和它们的完整依赖集
```

**为什么移除被依赖的 Bean**：因为它们会在其依赖者的 `getBean()` 调用链中被创建。如果 A 依赖 B，调用 `getBean("A")` 时 Spring 会自动先创建 B。所以只需要对"根 Bean"调用 `getBean()`，被依赖的 Bean 会被级联创建。

**循环依赖处理**：展平过程通过 `flattenDependentBeanNames.add(dependentBeanName)` 的返回值（`Set.add` 返回 `false` 表示已存在）作为递归终止条件。如果 A -> B -> A，展平时 A 的依赖集中已经有 A，递归到 B -> A 时发现 A 已在集合中，跳过。

---

### 连通分量算法：resolveBeanNamesInDependencyPaths

这是并行执行的核心算法。给定展平后的依赖图（每个根 Bean -> 其完整依赖集），需要将 Bean 分组为可以并行执行的"路径"。

**算法步骤**：

```
输入：{A -> {B, C}, D -> {E}, F -> {}}
     （A 依赖 B 和 C，D 依赖 E，F 无依赖）

步骤 1：将 Bean 自身加入依赖集
     {A -> {A, B, C}, D -> {D, E}, F -> {F}}

步骤 2：合并有交集的集合
     A 的集合 {A,B,C} 与 D 的集合 {D,E} 无交集 -> 不合并
     A 的集合 {A,B,C} 与 F 的集合 {F} 无交集 -> 不合并
     D 的集合 {D,E} 与 F 的集合 {F} 无交集 -> 不合并

步骤 3：剩余非空集合即为依赖路径
     路径 1: {A, B, C}  -> 串行执行 getBean("A")（级联创建 B 和 C）
     路径 2: {D, E}      -> 串行执行 getBean("D")（级联创建 E）
     路径 3: {F}          -> 串行执行 getBean("F")

三条路径并行执行
```

**如果两个 Bean 的依赖集有交集**：

```
输入：{A -> {B}, C -> {B}}
步骤 1：{A -> {A, B}, C -> {C, B}}
步骤 2：{A, B} 与 {C, B} 有交集（B），合并为 {A, B, C}
结果：只有一条路径 {A, B, C}，串行执行
```

因为 A 和 C 都依赖 B，如果并行执行 `getBean("A")` 和 `getBean("C")`，两者都会触发 B 的创建，可能导致竞态条件。合并为同一条路径后，串行执行 `getBean("A")`（创建 B 和 A），然后 `getBean("C")`（B 已存在，直接创建 C）。

**这个算法的本质是无向图的连通分量**：将每个 Bean 视为顶点，依赖关系视为边（无向），找到所有连通子图。每个连通子图就是一条依赖路径。

---

### 并行实例化

```java
for (Set<String> beanNamesInDependencyPath : dependencyPaths) {
    executorService.submit(() -> {
        for (String beanName : beanNamesInDependencyPath) {
            beanFactory.getBean(beanName);  // 级联创建依赖
        }
    });
}
// 等待所有任务完成
while (executorService.awaitTermination(10, TimeUnit.MILLISECONDS)) {
}
```

**为什么直接调 `getBean` 而不是 `preInstantiateSingletons`**：

1. `getBean` 是线程安全的：`DefaultSingletonBeanRegistry#getSingleton` 使用 `synchronized` 保护单例创建
2. `getBean` 支持级联创建：调用 `getBean("A")` 时，如果 A 依赖 B，Spring 会自动递归创建 B
3. 当 Spring 的 `preInstantiateSingletons` 后续执行时，这些 Bean 已在 `singletonObjects` 中，`containsSingleton` 检查会跳过它们

**为什么用 `awaitTermination` 轮询而不是 `shutdown + awaitTermination`**：

microsphere 使用 `while (awaitTermination(10ms))` 循环等待。这是因为 `executorService.shutdown()` 不会取消已提交的任务，而 `awaitTermination` 会阻塞到所有任务完成。轮询方式允许在等待期间做其他事情（虽然当前实现没有做）。

---

### 配置与注册

#### 事件系统注册

整个事件系统（包括 `BeanFactoryListener` 和 `BeanListener`）通过 `META-INF/spring.factories` 自动注册为 `ApplicationContextInitializer`：

```properties
# microsphere-spring-context/src/main/resources/META-INF/spring.factories
org.springframework.context.ApplicationContextInitializer=\
io.microsphere.spring.context.event.EventPublishingBeanInitializer,\
io.microsphere.spring.beans.factory.support.ListenableAutowireCandidateResolverInitializer,\
io.microsphere.spring.core.env.ListenableConfigurableEnvironmentInitializer,\
io.microsphere.spring.context.annotation.AutoRegistrationBeanInitializer
```

`EventPublishingBeanInitializer` 虽然自动注册，但默认**禁用**（`microsphere.spring.event-publishing-bean.enabled=false`）。

#### 并行初始化监听器注册

`ParallelPreInstantiationSingletonsBeanFactoryListener` **不在** `spring.factories` 中。它需要用户显式注册为 Spring Bean：

```java
@Configuration
public class MyConfig {
    @Bean
    public ParallelPreInstantiationSingletonsBeanFactoryListener parallelPreInstantiationListener() {
        return new ParallelPreInstantiationSingletonsBeanFactoryListener();
    }
}
```

`BeanFactoryListeners`（复合监听器）在构造时通过 `beanFactory.getBeansOfType(BeanFactoryListener.class)` 发现它。只要它注册为 Spring Bean，就会被自动纳入监听链。

#### 注入点解析器注册

`InjectionPointDependencyResolver` 的三个实现通过 `spring.factories` 自动注册：

```properties
io.microsphere.spring.beans.factory.InjectionPointDependencyResolver=\
io.microsphere.spring.beans.factory.ConstructionInjectionPointDependencyResolver,\
io.microsphere.spring.beans.factory.annotation.AutowiredInjectionPointDependencyResolver,\
io.microsphere.spring.beans.factory.annotation.ResourceInjectionPointDependencyResolver
```

`InjectionPointDependencyResolvers` 构造时通过 `SpringFactoriesLoader.loadFactories` 加载这三个实现。注意 `BeanMethodInjectionPointDependencyResolver` **不在**列表中--它用于 `DefaultBeanDependencyResolver` 的构造器/方法参数解析，而非注入点解析。

#### 配置属性

```properties
# 启用事件系统（前置条件）
microsphere.spring.event-publishing-bean.enabled=true

# 并行初始化线程数（默认 = CPU 核心数，设为 0 禁用）
microsphere.spring.pre-instantiation.singletons.threads=8

# 线程名前缀
microsphere.spring.pre-instantiation.singletons.thread.name-prefix=Parallel-Pre-Instantiation-Singletons-Thread-
```

默认线程数为 `Runtime.getRuntime().availableProcessors()`，即 CPU 核心数。设为 0 时 `newExecutorService()` 返回 null，跳过并行初始化。

#### Dependency 与 DependencyTreeWalker

microsphere 还提供了 `Dependency` 和 `DependencyTreeWalker` 两个类，用于构建和遍历依赖树。但它们**并未被并行初始化使用**--并行初始化直接操作 `Map<String, Set<String>>`。这两个类是独立的工具，可能用于未来的可视化或调试功能。

`Dependency` 是一个树节点：每个节点有 `beanName`、`parent`、`children` 列表。`DependencyTreeWalker#walk` 做的事情是**去重**：如果树中存在重复节点，合并其子节点并标记 `duplicated=true`，最后移除重复节点。

```
Before walk: A[B, C[D, E, B]]
After walk:  A[C[D, E, B]]     // B 被标记为重复并移除
```

这个去重逻辑与并行初始化中的连通分量合并（`resolveBeanNamesInDependencyPaths`）有相似之处，但实现方式不同--`DependencyTreeWalker` 操作树结构，`resolveBeanNamesInDependencyPaths` 操作集合。

---

### 一个具体示例

假设有以下 Bean 定义：

```java
@Configuration
class AppConfig {
    @Bean
    A a(B b, C c) { return new A(b, c); }   // A 依赖 B 和 C（构造器参数）

    @Bean
    B b() { return new B(); }                 // B 无依赖

    @Bean
    C c(D d) { return new C(d); }             // C 依赖 D（构造器参数）

    @Bean
    D d() { return new D(); }                 // D 无依赖

    @Bean
    E e() { return new E(); }                 // E 无依赖

    @Bean
    F f() { return new F(); }                 // F 无依赖
    // F 不被任何 Bean 依赖
}
```

**步骤 1：依赖分析**

| Bean | 直接依赖来源 | 直接依赖 |
|------|------------|---------|
| A | @Bean 方法 `a(B b, C c)` 参数 | {B, C} |
| B | 无 | {} |
| C | @Bean 方法 `c(D d)` 参数 | {D} |
| D | 无 | {} |
| E | 无 | {} |
| F | 无 | {} |

**步骤 2：展平传递依赖**

- A -> {B, C} -> C -> {D} => A -> {B, C, D}
- C -> {D}
- B/D/E/F -> {}

**步骤 3：移除被依赖的 Bean**

- B 被 A 依赖 -> 移除
- C 被 A 依赖 -> 移除
- D 被 A 和 C 依赖 -> 移除

剩余"根 Bean"：A -> {A, B, C, D}、E -> {E}、F -> {F}

**步骤 4：连通分量**

- {A, B, C, D} 与 {E} 无交集
- {A, B, C, D} 与 {F} 无交集
- {E} 与 {F} 无交集

三条依赖路径：`[A,B,C,D]`、`[E]`、`[F]`

**步骤 5：并行执行**

```
Thread-1: getBean("A") -> 级联创建 D -> C -> B -> A
Thread-2: getBean("E")
Thread-3: getBean("F")
```

三个线程并行执行，Thread-1 内部串行创建 A 的依赖链，Thread-2 和 Thread-3 独立创建无依赖的 E 和 F。

---

## 永恒原理

### 1. 依赖图与连通分量

并行化的核心前提是：**无依赖关系的任务可以并行执行**。在 Bean 容器中，"无依赖关系"不是简单的两两检查，而是需要构建完整的依赖图并找到连通分量。

这个问题在计算机科学中是经典的：给定一个无向图，找到所有连通子图。microsphere 的实现用集合合并（Union-Find 的简化版）来解决这个问题--两个集合有交集就合并，最终每个非空集合就是一个连通分量。

**为什么不用更高效的 Union-Find 算法**：microsphere 的实现是 O(N²) 的（N 个 Bean，每对检查交集），而 Union-Find 是 O(N·α(N))。但在实际应用中，Bean 数量通常在数百级别，O(N²) 的性能差异可忽略。而且集合合并的实现更直观，更容易理解正确性。

### 2. 展平传递依赖的必要性

直接依赖不足以确定并行安全性。A 依赖 B，B 依赖 C，如果只看直接依赖，A 和 C 似乎可以并行。但实际上 A 的创建会触发 B 的创建，B 的创建又触发 C 的创建--如果 C 也在另一个路径中被直接创建，就会产生竞态。

展平传递依赖后，A 的依赖集变为 {B, C}，任何与 C 有交集的 Bean 都会被合并到同一路径，消除了竞态。

### 3. `getBean` 的线程安全契约

Spring 的 `DefaultSingletonBeanRegistry#getSingleton(String, ObjectFactory)` 使用 `synchronized` 保护单例创建：

```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    synchronized (this.singletonObjects) {
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null) {
            singletonObject = singletonFactory.getObject();
            addSingleton(beanName, singletonObject);
        }
        return singletonObject;
    }
}
```

这意味着：即使两个线程同时调用 `getBean("A")`，A 只会被创建一次。这是并行 Bean 初始化的线程安全基础。

**但有一个微妙的问题**：`synchronized` 只保护单例注册，不保护 Bean 创建过程中的副作用。如果 `BeanPostProcessor` 在创建过程中修改了共享状态（如静态变量或 BeanFactory 的非线程安全字段），并行创建可能导致数据竞争。microsphere 假设 `BeanPostProcessor` 是线程安全的，这在 Spring 的设计中也是一个隐含契约。

### 4. 预加载 Class 的并行化

`DefaultBeanDependencyResolver#preProcessLoadBeanClasses` 在依赖分析之前并行加载 Bean Class。Class 加载是启动瓶颈之一（特别是有大量 `@ComponentScan` 的应用），并行加载可以显著减少时间。

`ClassLoader.loadClass` 在大多数实现中是线程安全的（使用 `synchronized` 或 `ConcurrentHashMap` 缓存已加载的类）。并行加载时，如果两个线程同时请求加载同一个 Class，只有一个线程会实际执行类加载，另一个等待。

---

## 边界与反例

### 1. 多构造器的 Bean

当 Bean 有多个构造器时，`SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors` 决定使用哪个。但这个方法在 `preInstantiateSingletons` 阶段才被调用，依赖分析阶段无法预知结果。

microsphere 的处理：如果只有一个构造器，解析其参数；如果有多个，**跳过构造器参数依赖解析**（日志告警）。这意味着多构造器 Bean 的依赖可能不完整，导致它被错误地分配到独立路径（实际有依赖但分析结果无依赖）。

**后果**：多构造器 Bean 可能与它的实际依赖并行创建。由于 `getBean` 是线程安全的，这不会导致重复创建，但可能导致创建顺序不确定（依赖 Bean 可能在 Bean 之后才创建，触发 Spring 的循环依赖处理或创建失败）。

### 2. `SmartInitializingSingleton` 的时序

Spring 的 `preInstantiateSingletons` 在所有单例创建完成后调用 `SmartInitializingSingleton#afterSingletonsInstantiated`。microsphere 在 `onBeanFactoryConfigurationFrozen` 阶段（`preInstantiateSingletons` 之前）预先创建了所有 Bean，但这些 Bean 的 `afterSingletonsInstantiated` 回调不会在此时触发。

当 Spring 的 `preInstantiateSingletons` 执行时，它发现所有 Bean 已在 `singletonObjects` 中，跳过创建，但仍然会遍历调用 `SmartInitializingSingleton#afterSingletonsInstantiated`。这意味着 `SmartInitializingSingleton` 的回调时序不受影响--它仍然在所有 Bean 创建完成后触发。

### 3. `BeanPostProcessor` 的并行安全

并行创建 Bean 时，多个线程同时调用 `BeanPostProcessor` 的回调方法。如果 `BeanPostProcessor` 实现有非线程安全的共享状态（如可变实例变量），会导致数据竞争。

**常见风险**：
- 自定义 `BeanPostProcessor` 中使用非线程安全的 `SimpleDateFormat`
- `BeanPostProcessor` 在 `postProcessBeforeInitialization` 中修改静态变量
- `BeanPostProcessor` 注册新的 BeanDefinition（`BeanDefinitionRegistry` 不是线程安全的）

**缓解**：Spring 内置的 `BeanPostProcessor` 通常是线程安全的（如 `AutowiredAnnotationBeanPostProcessor` 使用 `ConcurrentHashMap` 缓存注入元信息）。风险主要来自用户自定义的 Processor。

### 4. 循环依赖与展平递归

`flatDependentBeanNames` 通过 `Set.add` 返回值防止无限递归。但循环依赖（A -> B -> A）在展平后会产生：A 的依赖集 = {B, A}（自引用被移除后为 {B}），B 的依赖集 = {A, B}（自引用被移除后为 {A}）。最终 A 和 B 会合并到同一条路径，串行执行。

Spring 默认支持 Setter 循环依赖（通过三级缓存），并行初始化不影响这个机制--因为 `getBean("A")` 会触发 A 的创建，A 创建过程中注入 B 时触发 B 的创建，B 创建过程中注入 A 时从三级缓存获取 A 的早期引用。整个过程在一个线程内完成。

### 5. `DependencyAnalysisBeanFactoryListener` vs `DefaultBeanDependencyResolver`

microsphere 有两个依赖分析实现：

- **`DependencyAnalysisBeanFactoryListener`**：独立监听器，只分析构造器参数和 BeanDefinition 元数据，**注入点依赖分析标注了 `// TODO` 未实现**。用于日志/调试
- **`DefaultBeanDependencyResolver`**：完整的依赖解析器，分析所有四个来源（包括注入点），被 `ParallelPreInstantiationSingletonsBeanFactoryListener` 使用

这意味着 `DependencyAnalysisBeanFactoryListener` 的分析结果可能不完整，不应作为并行初始化的依据。

---

## 现代 Spring（6.x）是否已支持？

**Spring 6.x 仍然不支持并行 Bean 初始化。** `preInstantiateSingletons` 仍然是串行的。

Spring 6.0 引入了 `AbstractApplicationContext#setStartupOptions`，但这是一个"是否允许循环依赖"的开关，与并行初始化无关。

Spring 6.1 引入了 `BeanInstanceAdvice` 概念（通过 `RootBeanDefinition#setInstanceSupplier`），但这是对 Bean 实例化的抽象，不是并行化。

Spring Boot 3.2 的 `spring.main.lazy-initialization=true` 可以延迟 Bean 创建，但不减少总创建时间（只是推迟到首次使用时）。

**GraalVM Native Image** 是另一种思路：通过 AOT 编译将 Bean 创建提前到编译期，启动时无需实例化。但这牺牲了运行时动态性（不支持动态 Bean 注册、反射受限），与并行初始化是互补而非替代关系。

microsphere 的并行初始化是"**运行时优化**"：不改编程模型、不牺牲动态性、兼容现有 `BeanPostProcessor`，只是把串行变并行。代价是依赖分析的额外开销（通常在数百毫秒级别）和 `BeanPostProcessor` 线程安全的隐含要求。

---

## 小结

microsphere-spring 的并行 Bean 初始化，本质上是将 Spring 的"串行实例化"替换为"依赖图分析 -> 连通分量 -> 并行执行"。通过四个来源的依赖分析（BeanDefinition 元数据、构造器参数、@Bean 方法参数、注入点），构建完整的传递依赖图，用集合合并算法找到连通分量，每个分量串行执行、分量之间并行执行。

这个设计最精妙的地方不是并行本身，而是**依赖分析的完整性**。Spring 没有做并行初始化，不是不知道怎么做，而是无法在 `preInstantiateSingletons` 阶段安全地分析依赖（因为依赖解析依赖 `BeanPostProcessor`，而 `BeanPostProcessor` 依赖 Bean 创建）。microsphere 通过 `onBeanFactoryConfigurationFrozen`（配置冻结后、实例化前）这个时机，在 `BeanPostProcessor` 已注册但未执行的状态下，通过反射和 `InjectionPointDependencyResolver` 预先分析依赖，绕过了这个先有鸡还是先有蛋的问题。
