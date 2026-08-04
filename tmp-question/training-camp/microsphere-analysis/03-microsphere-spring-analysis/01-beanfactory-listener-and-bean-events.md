# 03-01 BeanFactory 生命周期监听与 Bean 事件化

## 问题：Spring 的容器级生命周期是"黑盒"

Spring 提供了两类生命周期扩展点：

- **BeanPostProcessor**：Bean 级别，拦截初始化前后（`postProcessBeforeInitialization`/`postProcessAfterInitialization`）
- **BeanFactoryPostProcessor**：容器级别，拦截 BeanFactory 准备完成（`postProcessBeanFactory`）

但这两者之间存在大量"盲区"：

| 时机 | Spring 是否暴露回调 | 问题 |
|------|-------------------|------|
| BeanDefinition 注册完成 | `BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry` | 有，但无法区分"注册完成"和"BeanFactory 准备完成"两个阶段 |
| BeanFactory 准备完成 | `BeanFactoryPostProcessor#postProcessBeanFactory` | 有，但所有 PostProcessor 混在同一批调用中，无法统一排序 |
| **配置冻结（freezeConfiguration）** | **无** | 这是 `preInstantiateSingletons` 前的最后时机，Spring 没有任何回调 |
| Bean 实例化前（构造器级别） | `InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation` | 有，但只能返回代理对象跳过实例化，**无法观察构造器参数和工厂方法参数** |
| Bean 实例化后、属性填充前 | `InstantiationAwareBeanPostProcessor#postProcessAfterInstantiation` | 有，但无法获取 `PropertyValues` 的最终值 |
| Bean 属性填充完成 | `InstantiationAwareBeanPostProcessor#postProcessProperties` | 有，但返回值是修改后的 pvs，不是"通知" |
| Bean 初始化完成 | `BeanPostProcessor#postProcessAfterInitialization` | 有 |
| **所有 Bean 就绪（ContextRefreshed）** | `ContextRefreshedEvent` | 有，但事件可能在父子容器中重复触发 |
| Bean 销毁前/后 | `DestructionAwareBeanPostProcessor#postProcessBeforeDestruction` | 有，但只能"处理"不能"观察" |

核心矛盾在于：**Spring 的扩展点是"处理"语义（Processor），不是"观察"语义（Listener）**。Processor 需要返回可能修改的对象，而开发者经常只需要"知道发生了什么"，不需要修改任何东西。Processor 的另一个问题是：每个 Processor 都会参与所有 Bean 的创建链路（N×M 调用），而 Listener 可以通过 `supports()` 过滤。

microsphere-spring 的 `context/event` 包用一套 **BeanFactoryListener + BeanListener 双层监听体系** 填补了这个空白。

---

## 设计：双层监听体系

### 整体架构

```
ApplicationContext
    │
    ├── EventPublishingBeanInitializer (ApplicationContextInitializer, HIGHEST_PRECEDENCE)
    │       │
    │       └── 注册 EventPublishingBeanBeforeProcessor
    │
    │   ─── invokeBeanFactoryPostProcessors ───
    │
    ├── EventPublishingBeanBeforeProcessor
    │       │  (BeanDefinitionRegistryPostProcessor)
    │       ├── prepareBeanDefinitions: 将 Initializer 插入为第一个 BeanDefinition
    │       ├── onBeanDefinitionRegistryReady()         ← BeanFactoryListener
    │       │
    │       │  (BeanFactoryPostProcessor)
    │       ├── onBeanFactoryReady()                    ← BeanFactoryListener
    │       ├── 注册 BeanListeners
    │       └── 装饰 InstantiationStrategy（替换为自身，委托原始策略）
    │
    │   ─── freezeConfiguration() ─── （无回调）
    │
    │   ─── preInstantiateSingletons ───
    │
    ├── Initializer（第一个被实例化的 Bean）
    │       ├── onBeanFactoryConfigurationFrozen()      ← BeanFactoryListener
    │       ├── onBeanDefinitionReady() × N             ← BeanListener
    │       ├── 更新 readyBeanNames
    │       └── 注册 EventPublishingBeanAfterProcessor
    │
    │   ─── 实例化业务 Bean ───
    │
    ├── EventPublishingBeanBeforeProcessor（作为 InstantiationStrategy 装饰器）
    │       ├── onBeforeBeanInstantiate()               ← BeanListener
    │       ├── delegate.instantiate()
    │       └── onAfterBeanInstantiated()               ← BeanListener
    │
    ├── EventPublishingBeanBeforeProcessor（作为 BeanPostProcessor）
    │       ├── onBeanPropertyValuesReady()             ← BeanListener
    │       └── onBeforeBeanInitialize()                ← BeanListener
    │
    ├── EventPublishingBeanAfterProcessor (BeanPostProcessor + ApplicationListener)
    │       ├── onAfterBeanInitialized()                ← BeanListener
    │       ├── ContextRefreshedEvent -> onBeanReady()  ← BeanListener
    │       └── ContextClosedEvent -> 装饰 DisposableBean
    │               ├── onBeforeBeanDestroy()           ← BeanListener
    │               └── onAfterBeanDestroy()            ← BeanListener（通过 DecoratingDisposableBean）
```

**设计原则：观察者模式 vs 处理者模式**

Spring 的 PostProcessor 是"处理者"（Processor）——它参与 Bean 创建链路，可以修改、替换、甚至阻止 Bean 创建。这带来了灵活性，但也意味着每个 Processor 都会增加创建链路的开销，且 Processor 之间的顺序敏感。

microsphere 的 Listener 是"观察者"（Observer）——它在 Processor 链路之外，通过事件通知机制工作。Listener 不能修改 Bean，只能"知道发生了什么"。这种分离带来了三个好处：

1. **零干扰**：Listener 异常不会影响 Bean 创建（`BeanFactoryListeners` 对每个 Listener 做 try-catch）
2. **可过滤**：`BeanListener#supports(String beanName)` 在调用前过滤，避免不必要的回调
3. **可排序**：Listener 通过 `Ordered`/`@Order` 排序，不参与 Bean 创建链路，顺序问题更简单

---

### BeanFactoryListener：容器级三个锚点

```java
public interface BeanFactoryListener extends EventListener {

    void onBeanDefinitionRegistryReady(BeanDefinitionRegistry registry);

    void onBeanFactoryReady(ConfigurableListableBeanFactory beanFactory);

    void onBeanFactoryConfigurationFrozen(ConfigurableListableBeanFactory beanFactory);
}
```

三个回调对应 Spring IoC 容器启动的三个关键阶段：

| 回调 | Spring 原生时序 | 此时容器状态 |
|------|----------------|-------------|
| `onBeanDefinitionRegistryReady` | `BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry` 之后 | 所有 BeanDefinition 已注册，但 BeanFactory 未处理 |
| `onBeanFactoryReady` | `BeanFactoryPostProcessor#postProcessBeanFactory` 之后 | BeanFactory 已完成后置处理，但配置未冻结 |
| `onBeanFactoryConfigurationFrozen` | `ConfigurableListableBeanFactory#freezeConfiguration()` 之后 | 配置已冻结，BeanDefinition 只读，**即将开始 `preInstantiateSingletons`** |

第三个回调 `onBeanFactoryConfigurationFrozen` 是整个体系最有价值的锚点。`freezeConfiguration()` 是 Spring 内部方法，它将 BeanDefinition 标记为只读，此后任何修改都会抛出异常。这个时机意味着：

- 所有 BeanDefinition 已经合并完毕（`MergedBeanDefinition` 已就绪）
- 配置已锁定，可以安全地并发读取 BeanDefinition
- Bean 实例化尚未开始

这正是**并行 Bean 初始化**的最佳启动点（第 2 篇的主题）。

**为什么 Spring 自己没有暴露这个回调？**

`freezeConfiguration()` 是 `DefaultListableBeanFactory` 的方法，不是接口方法。Spring 的设计哲学是：内部实现细节不暴露给用户。`BeanFactoryPostProcessor` 的 `postProcessBeanFactory` 回调在 `freezeConfiguration` 之前触发，用户无法在冻结后、实例化前插入逻辑。

**microsphere 如何捕获冻结时机：Initializer Bean 技巧**

这是整个设计中最精妙的部分。microsphere 没有用 PostProcessor 捕获 `freezeConfiguration`（因为它在 PostProcessor 之后执行），而是用了一个"**偷跑 Bean**"技巧：

1. `EventPublishingBeanBeforeProcessor` 在 `postProcessBeanDefinitionRegistry` 阶段调用 `prepareBeanDefinitions`：**移除所有已注册的 BeanDefinition，先注册 `EventPublishingBeanAfterProcessor.Initializer` 作为第一个 BeanDefinition，再重新注册其余 BeanDefinition**

2. Spring 执行 `invokeBeanFactoryPostProcessors` -> `freezeConfiguration()` -> `preInstantiateSingletons()`

3. `preInstantiateSingletons` 按注册顺序实例化 Bean，**第一个被实例化的就是 `Initializer`**

4. `Initializer` 的构造函数中触发 `onBeanFactoryConfigurationFrozen`，然后触发所有 Bean 的 `onBeanDefinitionReady`，最后注册 `EventPublishingBeanAfterProcessor` 作为 BeanPostProcessor

```
Spring refresh() 时序：
  invokeBeanFactoryPostProcessors()
    ├── EventPublishingBeanBeforeProcessor.postProcessBeanDefinitionRegistry()
    │       └── 将 Initializer 插入为第一个 BeanDefinition
    │       └── onBeanDefinitionRegistryReady()
    └── EventPublishingBeanBeforeProcessor.postProcessBeanFactory()
            └── onBeanFactoryReady()
            └── 装饰 InstantiationStrategy
  freezeConfiguration()                    ← 配置冻结，无回调
  preInstantiateSingletons()
    ├── 实例化 Initializer（第一个 Bean）   ← 此时 freezeConfiguration 已完成！
    │       └── onBeanFactoryConfigurationFrozen()
    │       └── onBeanDefinitionReady() × N
    │       └── 注册 EventPublishingBeanAfterProcessor
    ├── 实例化第二个 Bean...
    └── 实例化第 N 个 Bean...
```

这个技巧的本质是：**利用 BeanDefinition 注册顺序控制 Bean 实例化顺序，将"初始化逻辑"伪装成一个普通 Bean，在第一个被实例化时执行**。`Initializer` 不是 FactoryBean、不是 PostProcessor，就是一个普通的 `@Component`，但它通过"排在第一个"获得了"配置冻结后、业务 Bean 实例化前"的执行时机。

---

### BeanListener：Bean 级十二个回调

BeanListener 覆盖了 Bean 从定义到销毁的完整生命周期：

```java
public interface BeanListener extends EventListener {

    boolean supports(String beanName);

    // BeanDefinition 阶段
    void onBeanDefinitionReady(String beanName, RootBeanDefinition mergedBeanDefinition);

    // 实例化阶段
    void onBeforeBeanInstantiate(String beanName, RootBeanDefinition mergedBeanDefinition);
    void onBeforeBeanInstantiate(String beanName, RootBeanDefinition mergedBeanDefinition,
                                  Constructor<?> constructor, Object[] args);
    void onBeforeBeanInstantiate(String beanName, RootBeanDefinition mergedBeanDefinition,
                                  Object factoryBean, Method factoryMethod, Object[] args);
    void onAfterBeanInstantiated(String beanName, RootBeanDefinition mergedBeanDefinition, Object bean);

    // 属性填充阶段
    void onBeanPropertyValuesReady(String beanName, Object bean, PropertyValues pvs);

    // 初始化阶段
    void onBeforeBeanInitialize(String beanName, Object bean);
    void onAfterBeanInitialized(String beanName, Object bean);

    // 就绪阶段
    void onBeanReady(String beanName, Object bean);

    // 销毁阶段
    void onBeforeBeanDestroy(String beanName, Object bean);
    void onAfterBeanDestroy(String beanName, Object bean);
}
```

与 Spring `BeanPostProcessor` 相比，BeanListener 暴露了三个 Spring 原生无法观察的时机：

**1. 构造器参数和工厂方法参数**

Spring 的 `InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation` 只能返回一个代理对象来跳过实例化，无法观察"用了哪个构造器"和"传了什么参数"。microsphere 通过**装饰 `InstantiationStrategy`** 来解决这个问题。

`EventPublishingBeanBeforeProcessor` 实现了 `InstantiationStrategy` 接口，在 `postProcessBeanFactory` 阶段将自身替换为 `AbstractAutowireCapableBeanFactory` 的实例化策略，同时持有原始策略作为委托：

```
原始链路：AbstractAutowireCapableBeanFactory → SimpleInstantiationStrategy.instantiate()

装饰后：AbstractAutowireCapableBeanFactory → EventPublishingBeanBeforeProcessor.instantiate()
                                                    ├── onBeforeBeanInstantiate(...)  ← 通知 Listener
                                                    └── delegate.instantiate(...)     ← 委托原始策略
                                                            └── onAfterBeanInstantiated(...) ← 通知 Listener
```

这是整个设计中最高明的部分。`InstantiationStrategy` 是 Spring 内部接口，有两个 `instantiate` 重载分别对应构造器实例化和工厂方法实例化。通过装饰这个接口，microsphere 可以观察到：

- 构造器的 `Constructor<?>` 对象和 `Object[] args` 参数
- 工厂方法的 `Method` 对象、`Object factoryBean` 和 `Object[] args` 参数

这些信息在 `BeanPostProcessor` 中完全不可见。

**2. 属性填充完成**

`onBeanPropertyValuesReady` 在 `postProcessProperties` 返回后触发，此时 `PropertyValues` 已经是最终值（经过所有 Processor 处理），可以安全地读取而不影响后续流程。

**3. Bean 完全就绪**

`onBeanReady` 在 `ContextRefreshedEvent` 时触发，此时所有非延迟单例 Bean 都已创建完成。与直接监听 `ContextRefreshedEvent` 的区别是：`BeanListeners` 内部跟踪了"已就绪 Bean 名称集合"（`readyBeanNames`），避免对在 Listener 注册前已创建的基础设施 Bean 重复触发回调。

---

### BeanFactoryListeners：复合监听器与发现机制

`BeanFactoryListeners` 是 `BeanFactoryListener` 的复合实现，负责：

1. **发现**：从 BeanFactory 和 `SpringFactoriesLoader` 两处发现 Listener
2. **排序**：按 `Ordered`/`@Order` 排序
3. **容错**：每个 Listener 的回调包裹在 try-catch 中，一个 Listener 异常不影响其他 Listener

```java
class BeanFactoryListeners implements BeanFactoryListener {

    BeanFactoryListeners(ConfigurableListableBeanFactory beanFactory) {
        // 从 SpringFactoriesLoader 注册（META-INF/spring.factories）
        registerSpringFactoriesBeans(beanFactory, BeanFactoryListener.class);
        // 从 BeanFactory 获取所有 BeanFactoryListener Bean
        Map<String, BeanFactoryListener> map = beanFactory.getBeansOfType(BeanFactoryListener.class);
        // 按 Ordered 排序
        namedListeners.sort(NamedBeanHolderComparator.INSTANCE);
    }

    private void iterate(Consumer<BeanFactoryListener> action, String actionName) {
        for (int i = 0; i < listenerCount; i++) {
            try {
                action.accept(namedListeners.get(i).getBeanInstance());
            } catch (Throwable e) {
                logger.error("...", e);  // 一个失败不影响其他
            }
        }
    }
}
```

**双路发现的原理**：

`registerSpringFactoriesBeans` 会将 `META-INF/spring.factories` 中注册的 `BeanFactoryListener` 实现类注册为 BeanDefinition，然后 `getBeansOfType` 统一获取。这意味着 Listener 可以通过两种方式注册：

- **Spring Bean**：标注 `@Component` 或在 `@Configuration` 中 `@Bean`
- **SPI**：在 `META-INF/spring.factories` 中声明

后者适用于框架级 Listener（不需要用户显式启用），前者适用于用户自定义 Listener。

---

### BeanTimeStatistics：一个实用的 BeanListener

`BeanTimeStatistics` 是 `BeanListener` 的内置实现，用 `StopWatch` 测量每个 Bean 的实例化和初始化耗时：

```
Bean 创建时间线：
  onBeanDefinitionReady → start("ready.{beanName}")
  onBeforeBeanInstantiate → start("instantiation.{beanName}")
  onAfterBeanInstantiated → stop()
  onBeforeBeanInitialize → start("initialization.{beanName}")
  onAfterBeanInitialized → stop()
  onBeanReady → stop("ready.{beanName}")
```

这个实现的价值在于：**它暴露了 Spring 不愿意暴露的 Bean 级耗时数据**。Spring 的 `StopWatch` 只在 `AbstractApplicationContext#refresh` 级别计时，粒度太粗。`BeanTimeStatistics` 可以精确到每个 Bean 的实例化时间和初始化时间，帮助定位启动瓶颈。

它通过 `supports(String beanName)` 过滤掉基础设施 Bean（如 `environment`、`beanFactory` 等），避免噪音数据。

---

### BeanPropertyChangedEvent：JavaBeans 事件桥接

microsphere 还将 JavaBeans 的 `PropertyChangeListener` 机制桥接到 Spring 事件体系：

```java
public class JavaBeansPropertyChangeListenerAdapter implements PropertyChangeListener {

    private final ApplicationEventPublisher publisher;

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        BeanPropertyChangedEvent adapted = new BeanPropertyChangedEvent(
            event.getSource(), event.getPropertyName(),
            event.getOldValue(), event.getNewValue()
        );
        publisher.publishEvent(adapted);
    }
}
```

`BeanPropertyChangedEvent` 是一个 `ApplicationEvent`，携带 `propertyName`、`oldValue`、`newValue`。用户可以用标准的 `@EventListener` 监听 Bean 属性变更：

```java
@EventListener
public void onBeanPropertyChanged(BeanPropertyChangedEvent event) {
    log.info("Bean {} property {} changed: {} -> {}",
        event.getBean(), event.getPropertyName(),
        event.getOldValue(), event.getNewValue());
}
```

**原理与边界**：

JavaBeans 的 `PropertyChangeListener` 需要通过 `PropertyChangeSupport` 注册到目标 Bean。只有 Bean 内部主动调用 `firePropertyChange` 才会触发通知——这意味着它不是"拦截器"，不能自动捕获所有字段变更。它的价值在于：当 Bean 实现了 `PropertyChangeSupport` 模式（如 `java.beans.PropertyChangeSupport` 或 Lombok 的 `@BoundProperty`），microsphere 可以将这些变更统一路由到 Spring 事件总线，与其他 ApplicationEvent 一起处理。

---

### OnceApplicationContextEventListener：防止父子容器重复触发

Spring 的 hierarchical context（父子容器）会导致 `ApplicationContextEvent` 被多次触发。`OnceApplicationContextEventListener` 通过检查事件源是否是当前 ApplicationContext 来过滤：

```java
public abstract class OnceApplicationContextEventListener<E extends ApplicationContextEvent>
        implements ApplicationListener<E>, ApplicationContextAware {

    public final void onApplicationEvent(E event) {
        if (isOriginalEventSource(event)) {  // 只处理当前 Context 的事件
            onApplicationContextEvent(event);
        }
    }

    private boolean isOriginalEventSource(ApplicationEvent event) {
        return nullSafeEquals(getApplicationContext(), event.getSource());
    }
}
```

**为什么需要这个**：Spring MVC 中，`DispatcherServlet` 有自己的 `WebApplicationContext`，它是 `ContextLoaderListener` 创建的 Root `WebApplicationContext` 的子容器。当 `ContextRefreshedEvent` 触发时，子容器的 Listener 会收到两次事件（一次来自子容器，一次冒泡自父容器）。`OnceApplicationContextEventListener` 确保只处理来自当前容器的事件。

---

### 启用方式

整个体系默认**关闭**，通过 `microsphere.spring.event-publishing-bean.enabled=true` 启用：

```java
public class EventPublishingBeanInitializer extends ConfigurableApplicationContextInitializer
        implements PriorityOrdered {

    public static final String ENABLED_PROPERTY_NAME =
        "microsphere.spring.event-publishing-bean.enabled";

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;  // 最高优先级，确保在其他 Initializer 之前注册
    }
}
```

`EventPublishingBeanInitializer` 是 `ApplicationContextInitializer`，在 `ApplicationContext.refresh()` 之前执行。它以 `HIGHEST_PRECEDENCE` 优先级运行，确保 `EventPublishingBeanBeforeProcessor` 是第一个被注册的 `BeanFactoryPostProcessor`。

**为什么默认关闭**：Bean 事件化有性能开销。每个 Bean 的创建都会触发 5-8 次 Listener 回调，在 Bean 数量多时（数千个）累积开销不可忽视。默认关闭让用户按需启用，符合"防御性设计"原则。

---

## 永恒原理

### 1. 观察者与处理者的分离

"观察者"（Observer）和"处理者"（Processor/Interceptor）是两种根本不同的扩展模式：

| 维度 | 处理者（Processor） | 观察者（Listener） |
|------|--------------------|--------------------|
| 语义 | "我要参与并可能修改" | "我只需要知道" |
| 返回值 | 返回可能修改后的对象 | 无返回值（void） |
| 链路 | 在创建链路中，影响后续步骤 | 在创建链路外，不影响创建 |
| 异常 | 异常会中断创建 | 异常被隔离，不影响创建 |
| 排序 | 顺序敏感，可能影响结果 | 顺序不敏感（或仅影响日志顺序） |
| 过滤 | 通常无过滤，处理所有 Bean | 可通过 `supports()` 过滤 |

Spring 的 PostProcessor 体系是"处理者"设计，适合需要修改 Bean 行为的场景（如 AOP 代理、属性覆盖）。但大量场景只需要"观察"（如启动耗时统计、Bean 依赖分析、审计日志），这些场景用 Processor 是过度设计——你不得不返回原始对象，还要担心异常影响创建链路。

microsphere 的 Listener 体系将"观察"语义从"处理"中剥离出来，让扩展点的设计意图更清晰。

### 2. 装饰器模式与 InstantiationStrategy

`EventPublishingBeanBeforeProcessor` 装饰 `InstantiationStrategy` 的设计，是装饰器模式在框架扩展中的经典应用。装饰器模式的核心约束是：**装饰器必须实现与被装饰对象相同的接口**。`InstantiationStrategy` 是 Spring 内部接口，有明确的 `instantiate` 方法签名，microsphere 实现这个接口并将调用委托给原始策略，在委托前后插入通知逻辑。

这个模式的边界在于：**如果 Spring 改变 `InstantiationStrategy` 接口**（如增加新方法），装饰器需要同步更新。microsphere 通过反射获取 `AbstractAutowireCapableBeanFactory#getInstantiationStrategy` 来设置装饰器，而非依赖固定的方法签名，降低了耦合度。

### 3. 配置冻结作为并行安全的分界点

`freezeConfiguration()` 将 BeanDefinition 标记为只读，此后任何修改抛出异常。这个"只读"保证是并行的前提条件：如果 BeanDefinition 可变，并行实例化时一个线程的修改会影响其他线程。

microsphere 选择 `onBeanFactoryConfigurationFrozen` 作为并行初始化的启动点，正是利用了这个"只读保证"。这不是巧合——**任何需要并行读取容器元数据的扩展，都应该在配置冻结后执行**。

### 4. BeanDefinition 注册顺序作为隐式优先级

`Initializer` 技巧揭示了 Spring 的一个隐含行为：**`preInstantiateSingletons` 按 BeanDefinition 注册顺序实例化 Bean**。虽然 Spring 文档没有保证这个顺序（`DefaultListableBeanFactory.getBeanDefinitionNames()` 返回注册顺序），但 microsphere 利用这个行为将 `Initializer` 排在第一位，获得了"配置冻结后、业务 Bean 实例化前"的执行时机。

这个模式的本质是：**当框架不提供显式的生命周期回调时，可以通过控制资源注册顺序来获得隐式的执行时机**。这是一种"灰色技巧"--它依赖 Spring 的实现细节而非公开 API，如果 Spring 未来改变实例化顺序（如改为并行实例化），这个技巧可能失效。

---

## 现代 Spring（6.x / Spring Boot 3.x）是否已支持？

**答案：仍然不支持。** 截至 Spring Framework 6.1 / Spring Boot 3.2，以下特性 Spring 原生均未提供：

| microsphere 特性 | Spring 6.x 是否有等价物 | 说明 |
|------------------|----------------------|------|
| `BeanFactoryListener` | 无 | Spring 6.x 仍只有 `BeanFactoryPostProcessor`，无 Listener 语义 |
| `onBeanFactoryConfigurationFrozen` | 无 | `freezeConfiguration()` 仍是内部方法，无回调 |
| `BeanListener`（12 回调） | 无 | `BeanPostProcessor` 仍是唯一的 Bean 级扩展点 |
| 构造器参数观察 | 无 | `InstantiationStrategy` 仍是内部接口，不暴露给用户 |
| Bean 销毁后回调 | 无 | `DestructionAwareBeanPostProcessor` 只有 `postProcessBeforeDestruction`，无 after |

Spring 5.3+ 引入了 `ApplicationStartup` / `StartupStep` API，Spring Boot 3.x 的 `BufferingApplicationStartup` 可以记录启动步骤。但这是**被动录制**（recording），不是**主动观察**（observing）--你能看到"Bean A 创建花了 50ms"，但不能在"Bean A 创建时"执行自定义逻辑。两者是互补关系，不是替代关系。

Spring 6.1 的 `SmartInitializingSingleton#afterSingletonsInstantiated` 是最接近的，但它只在**所有单例 Bean 都创建完成后**触发一次，不能观察单个 Bean 的创建过程。

## Bean 销毁后回调：DecoratingDisposableBean

`BeanListener` 有 `onBeforeBeanDestroy` 和 `onAfterBeanDestroy` 两个回调。`onBeforeBeanDestroy` 通过 `DestructionAwareBeanPostProcessor#postProcessBeforeDestruction` 实现，但 Spring **没有**"销毁后"的回调。

microsphere 的解决方案是：在 `ContextClosedEvent` 时，反射修改 `DefaultSingletonBeanRegistry` 的 `disposableBeans` Map，将 Spring 内部的 `DisposableBeanAdapter` 替换为 `DecoratingDisposableBean`。后者在调用原始 `destroy()` 后，触发 `onAfterBeanDestroy`：

```
原始链路：DefaultSingletonBeanRegistry.destroySingleton()
              -> DisposableBeanAdapter.destroy()

装饰后：DefaultSingletonBeanRegistry.destroySingleton()
            -> DecoratingDisposableBean.destroy()
                    ├── delegate.destroy()           ← 原始销毁逻辑
                    └── onAfterBeanDestroy(beanName)  ← 通知 Listener
```

这个设计用反射修改 Spring 内部数据结构（`disposableBeans` Map），侵入性较高。如果 Spring 未来重构 `DefaultSingletonBeanRegistry` 的字段名或结构，这段代码会失效。

---

## 边界与反例

### 1. Listener 不能修改 Bean

BeanListener 的所有回调返回 `void`，不能修改 Bean 对象、BeanDefinition 或 PropertyValues。如果需要修改，仍然必须使用 Spring 的 `BeanPostProcessor`。

**反例**：尝试在 `onAfterBeanInstantiated` 中修改 Bean 的字段——这是可能的（Bean 对象引用可以修改），但不推荐。因为 Listener 不在 Spring 的事务/安全上下文中，修改行为可能导致不可预测的副作用。

### 2. InstantiationStrategy 装饰器的侵入性

装饰 `InstantiationStrategy` 是一个侵入性较高的操作。`EventPublishingBeanBeforeProcessor` 通过反射替换 `AbstractAutowireCapableBeanFactory` 的 `instantiationStrategy` 字段。如果其他框架也替换了这个策略（如某些 AOP 框架），可能导致冲突。

**边界**：microsphere 的装饰器持有原始策略引用并委托调用，理论上可以链式装饰。但如果有两个框架都做装饰且不互相感知，调用顺序可能不可控。

### 3. readyBeanNames 的两阶段设置

`BeanListeners` 在构造时通过 `beanFactory.getSingletonNames()` 记录已创建的 Bean，这些 Bean 不会触发 `onBeanReady` 回调。但此时可能已有其他 `BeanFactoryPostProcessor` 创建了额外的 Bean。`Initializer` 在 `preInstantiateSingletons` 阶段（第一个被实例化时）会**重新设置** `readyBeanNames`，将窗口内新增的单例纳入排除范围。

两阶段设置将竞态窗口从"`postProcessBeanFactory` 到 `ContextRefreshedEvent`"缩小到"`postProcessBeanFactory` 到 `Initializer` 构造"，而 `Initializer` 是 `preInstantiateSingletons` 的第一个 Bean，窗口极小。

### 4. SpringFactoriesLoader 的类加载风险

`registerSpringFactoriesBeans` 会从 `META-INF/spring.factories` 加载 Listener 类并实例化。如果 `spring.factories` 中声明的类依赖了尚未初始化的 Bean，会导致 `NoClassDefFoundError` 或 `BeanCreationException`。

**边界**：Listener 应避免在构造函数中依赖其他 Bean。如果需要依赖注入，应作为 Spring Bean 注册（通过 `@Component`），而非通过 `spring.factories`。

---

## 小结

microsphere-spring 的 BeanFactory 生命周期监听体系，本质上是将 Spring 的"Processor"（处理者）范式补充了"Listener"（观察者）范式。通过双层监听（BeanFactory 级 + Bean 级）、`InstantiationStrategy` 装饰器、配置冻结锚点三个关键设计，它暴露了 Spring 原生不提供的容器生命周期可观察性。

这套体系的价值不在于"能做什么 Spring 做不到的事"（Spring 的 Processor 理论上能做一切），而在于**让"观察"这件事变得安全、零干扰、可过滤**。当你只需要统计 Bean 耗时、分析依赖关系、记录审计日志时，不需要写一个可能影响所有 Bean 创建的 Processor——只需要实现一个 `BeanListener`，用 `supports()` 过滤关注的 Bean，在回调中做你的事。
