# 04-01 ConfigurationProperties 可监听绑定

## 问题：`Binder.bind()` 是黑盒

Spring Boot 的 `@ConfigurationProperties` 将外部配置属性绑定到 Bean。核心是 `Binder.bind()`：

```java
Binder binder = new Binder(configurationPropertySources, placeholderResolver, conversionService);
BindResult<AppProperties> result = binder.bind("app", Bindable.of(AppProperties.class), bindHandler);
```

这个过程对开发者完全不可见：哪些属性绑定了、绑定到了哪个字段、值有没有变化、绑定是否失败--全在黑盒内。Spring Boot 提供了 `BindHandler` 接口（`onStart`/`onSuccess`/`onFailure`/`onFinish`），但它被设计为"内部拦截器"，不是"外部观察者"：

| 维度 | `BindHandler` | `BindListener`（microsphere） |
|------|-------------|---------------------------|
| 语义 | 参与绑定流程，可以修改结果 | 观察绑定流程，不能修改 |
| 注册 | 每次 `Binder.bind()` 时传入 | 全局注册为 Spring Bean，自动发现 |
| 调用链 | 替换原始的 `BindHandler`，形成装饰链 | 不替换，附加在 `BindHandler` 旁边 |
| 异常处理 | 异常终止整个绑定流程 | 异常被隔离，不影响绑定 |
| 组合 | 只能有一个（传入哪个用哪个） | 多个 Listener 组合，按 @Ordered 排序 |

微服务架构中一个常见的需求是**属性变更通知**：当 `@ConfigurationProperties` Bean 的属性被配置中心更新后，需要通知相关组件刷新。这要求绑定过程可观察、属性变更可检测。Spring Boot 的 `Binder` 不提供这种能力。

microsphere-spring-boot 通过 **`BindListener` 体系 + `ConfigurationPropertiesBindHandlerAdvisor` 装饰器** 填补了这个空白。

---

## 设计：BindListener + 装饰器链 + 变更检测

### 整体架构

```
@EnableConfigurationPropertiesExtension
    │
    └── EnableConfigurationPropertiesExtensionRegistrar
            ├── 注册 ListenableConfigurationPropertiesBindHandlerAdvisor
            └── 注册 EventPublishingConfigurationPropertiesBeanPropertyChangedListener
                    │
                    ├── 作为 BindListener（被 Advisor 发现）
                    ├── 作为 ApplicationContextAware（获取上下文）
                    ├── 作为 InitializingBean（构建 BeanContext 映射）
                    └── 作为 SmartInitializingSingleton（标记绑定完成）
```

**运行时流程**：

```
Binder.bind("app", AppProperties.class, handler)
    │
    ├── ListenableConfigurationPropertiesBindHandlerAdvisor.apply(handler)
    │       └── 从 BeanFactory 获取所有 @Ordered 排序的 BindListener Bean
    │       └── 返回 ListenableBindHandlerAdapter(handler, bindListeners)
    │
    ├── ListenableBindHandlerAdapter.onStart(name, target, context)
    │       ├── super.onStart(name, target, context)  ← 委托原始 BindHandler
    │       └── bindListeners.onStart(name, target, context)  ← 通知所有 BindListener
    │
    ├── ... Spring Boot 内部逐个绑定属性 ...
    │
    ├── ListenableBindHandlerAdapter.onSuccess(name, target, context, result)
    │       ├── super.onSuccess(name, target, context, result)
    │       └── bindListeners.onSuccess(name, target, context, result)
    │            └── EventPublishingConfigurationPropertiesBeanPropertyChangedListener
    │                    └── 对比属性快照
    │                    └── 发现变更 -> 发布 ConfigurationPropertiesBeanPropertyChangedEvent
    │
    └── ... 类似 onFailure, onFinish ...
```

**设计原则：不替换，附加。** Spring Boot 的 `ConfigurationPropertiesBindHandlerAdvisor` 是 Spring Boot 提供的扩展点，在每次 `Binder.bind()` 时被调用，可以返回一个新的 `BindHandler` 替换原始的。microsphere 利用这个扩展点，返回一个 `ListenableBindHandlerAdapter` 作为装饰器--它持有原始 `BindHandler` 作为委托，在每个回调末尾通知 `BindListener`。

这与 03-spring 的 `BeanListener` 体系（第 1 篇）设计理念一致：**观察者不参与核心流程，在流程外通过事件机制工作**。

---

### BindListener：五个绑定阶段

```java
public interface BindListener {
    default <T> void onStart(ConfigurationPropertyName name, Bindable<T> target,
                             BindContext context) { }

    default void onSuccess(ConfigurationPropertyName name, Bindable<?> target,
                           BindContext context, Object result) { }

    default void onCreate(ConfigurationPropertyName name, Bindable<?> target,
                          BindContext context, Object result) { }

    default void onFailure(ConfigurationPropertyName name, Bindable<?> target,
                           BindContext context, Exception error) { }

    default void onFinish(ConfigurationPropertyName name, Bindable<?> target,
                          BindContext context, Object result) { }
}
```

五个回调对应绑定的五个生命周期阶段：

| 回调 | 触发时机 | 典型用途 |
|------|---------|---------|
| `onStart` | 元素绑定开始，尚未确定结果 | 记录绑定上下文、初始化快照 |
| `onSuccess` | 绑定成功，已有最终结果 | 属性变更检测、审计日志 |
| `onCreate` | 属性未绑定，创建了新实例 | 跟踪默认值、新对象创建 |
| `onFailure` | 绑定失败 | 错误收集、告警 |
| `onFinish` | 绑定结束（成功或失败），**`onFailure` 触发后不触发 `onFinish`** | 资源清理、计时统计 |

所有方法都是 `default` 空实现，Listener 只需覆盖关心的回调。`onFinish` 的语义值得注意：如果 `onFailure` 触发了，`onFinish` 不会触发。这意味着 `onFinish` 不能依赖为"必定执行"，清理逻辑应放在 `try-finally` 或 `onSuccess`/`onFailure` 内部。

**`ConfigurationPropertyName` vs `PropertySource` 的路径差异**：`ConfigurationPropertyName` 使用虚线命名（dashed form），如 `app.user-name`，而普通 `PropertySource` 中的 key 可能是 `app.userName`（camelCase）或 `app.USER_NAME`（underscore）。Spring Boot 的 `ConfigurationPropertyName.of()` 做了规范化，所以 Listener 中收到的 `name` 是一致的小写虚线格式。

---

### ListenableBindHandlerAdapter：装饰器

`ListenableBindHandlerAdapter` 继承 `AbstractBindHandler`（Spring Boot 的 `BindHandler` 基类），在每个回调中先委托父类，再通知 `BindListener`：

```java
public class ListenableBindHandlerAdapter extends AbstractBindHandler {
    private final BindListeners bindHandlers;

    @Override
    public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target,
                                    BindContext context) {
        Bindable<T> result = super.onStart(name, target, context);  // 委托原始 BindHandler
        bindHandlers.onStart(name, target, context);                // 通知 Listener
        return result;
    }

    @Override
    public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target,
                            BindContext context, Object result) {
        Object returnValue = super.onSuccess(name, target, context, result);
        bindHandlers.onSuccess(name, target, context, result);
        return returnValue;
    }
    // onFailure / onFinish 类似...
}
```

`BindListeners` 是复合实现，持有 `List<BindListener>`，按 `Ordered` 排序后逐个调用。注意：与 03-spring 的 `BeanFactoryListeners`（有 try-catch 隔离）不同，`BindListeners` 直接使用 `listeners.forEach()`，**不做 try-catch 隔离**--一个 Listener 的异常会终止后续 Listener 的调用并向上传播。

---

### ListenableConfigurationPropertiesBindHandlerAdvisor：自动装饰

Spring Boot 的 `ConfigurationPropertiesBindHandlerAdvisor` 是 `Binder.bind()` 的扩展点。`ListenableConfigurationPropertiesBindHandlerAdvisor` 实现此接口：

```java
public class ListenableConfigurationPropertiesBindHandlerAdvisor
        implements ConfigurationPropertiesBindHandlerAdvisor, BeanFactoryAware {

    @Override
    public BindHandler apply(BindHandler originalHandler) {
        List<BindListener> listeners = getSortedBeans(beanFactory, BindListener.class);
        return new ListenableBindHandlerAdapter(originalHandler, listeners);
    }
}
```

每当 `Binder.bind()` 被调用，Spring Boot 会遍历所有 `ConfigurationPropertiesBindHandlerAdvisor` Bean，用 `apply()` 返回的 `BindHandler` 替换原始的。microsphere 的 Advisor 将原始 `BindHandler` 包裹在 `ListenableBindHandlerAdapter` 中，自动注入所有 `BindListener` Bean。

**发现机制**：通过 `getSortedBeans` 从 BeanFactory 获取。BindListener 可以通过两种方式注册：
- Spring Bean（`@Component`）
- `spring.factories` 中的 `BindListener` 条目

---

### EventPublishingConfigurationPropertiesBeanPropertyChangedListener：变更检测

这是 `BindListener` 体系的核心应用。它实现了 `BindListener`、`ApplicationContextAware`、`InitializingBean`、`SmartInitializingSingleton` 四个接口：

```java
public class EventPublishingConfigurationPropertiesBeanPropertyChangedListener
        implements BindListener, ApplicationContextAware, InitializingBean, SmartInitializingSingleton {

    private Map<String, ConfigurationPropertiesBeanContext> beanContexts;
    private boolean bound = false;

    // InitializingBean: 构建所有 @ConfigurationProperties Bean 的 BeanContext
    @Override
    public void afterPropertiesSet() {
        this.beanContexts = buildConfigurationPropertiesBeanContexts(context);
    }

    // SmartInitializingSingleton: 所有 Bean 创建完成后，标记首次绑定完成
    //（首次绑定在 Bean 创建期间已完成，此后都是"重新绑定"）
    // 实际上此方法为空--标记逻辑在 onStart 中完成

    @Override
    public <T> void onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        if (!bound) {
            // 首次绑定：initializeBean 拍摄快照
            initConfigurationPropertiesBeanContext(name, target, context);
        }
    }

    @Override
    public void onSuccess(ConfigurationPropertyName name, Bindable<?> target,
                          BindContext context, Object result) {
        if (bound) {
            // 后续绑定（配置刷新）：对比快照，检测变更
            setConfigurationPropertiesBeanProperty(name, target, context, result);
        }
    }
}
```

**两阶段设计**：

```
阶段一（应用启动，bound = false）：
  onStart -> ConfigurationPropertiesBeanContext.initializeBean(bean) -> 拍摄快照
  onSuccess -> setConfigurationPropertiesBeanProperty -> 记录属性值

阶段二（配置刷新，bound = true）：
  onStart -> 跳过（已绑定）
  onSuccess -> 对比当前值与快照 -> 发现差异 -> 发布事件
```

`bound` 标志在 `SmartInitializingSingleton.afterSingletonsInstantiated()` 中设置为 `true`--此时所有单例 Bean 已创建完成，首次绑定也已结束。后续的任何 `Binder.bind()` 调用都被视为"重新绑定"（由配置中心推送触发），触发变更检测。

---

### ConfigurationPropertiesBeanContext：属性快照与差异检测

`ConfigurationPropertiesBeanContext`（583 行）是变更检测的核心。它的职责：

1. **维护属性快照**：`Map<ConfigurationPropertyName, ConfigurationPropertiesBeanProperty>`，记录每个属性的元信息（getter/setter/field/value）
2. **拍摄快照**：`initializeBean()` 通过 JavaBeans `PropertyDescriptor` 为每个属性创建 `ConfigurationPropertiesBeanProperty`，记录当前值
3. **检测变更**：`setConfigurationPropertiesBeanProperty()` 对比新值与快照中的旧值，用 `deepEquals` 判断是否变更
4. **发布事件**：发现变更后，创建 `ConfigurationPropertiesBeanPropertyChangedEvent`（继承 03-spring 的 `BeanPropertyChangedEvent`），通过 `ApplicationContext` 发布

`ConfigurationPropertiesBeanPropertyChangedEvent` 在 `BeanPropertyChangedEvent` 的基础上增加了：
- `propertyType: ResolvableType` -- 属性的泛型类型（如 `List<String>`）
- `configurationProperty: ConfigurationProperty` -- Spring Boot 的配置属性元数据（包含来源信息）

这使得监听者可以知道"哪个 `@ConfigurationProperties` Bean 的哪个属性从什么值变成了什么值，这个属性是什么类型，从哪个配置源来"。

**构造器绑定支持**：对于使用 Spring Boot 2.2+ 构造器绑定的 `@ConfigurationProperties`，`ConfigurationPropertiesBeanContext` 通过 `BindUtils.getBindConstructor` 获取绑定构造器，然后通过 `BeanWrapper.setPropertyValues` 做增量对比--不是创建新实例，而是复制当前 Bean 的属性到一个克隆实例，对克隆实例做绑定，再逐个属性对比差异。

---

### BindableConfigurationBeanBinder：Binder 替代 DataBinder

03-spring 第 5 篇的 `ConfigurationBeanBinder` 接口有两个实现：

| 实现 | 所在模块 | 底层绑定引擎 | 特性 |
|------|---------|------------|------|
| `DefaultConfigurationBeanBinder` | microsphere-spring-context | Spring Framework `DataBinder` | 需 setter 或 `initDirectFieldAccess`；不支持松散绑定 |
| `BindableConfigurationBeanBinder` | microsphere-spring-boot | Spring Boot `Binder` | 支持松散绑定（kebab-case -> camelCase）；支持构造器绑定 |

`BindableConfigurationBeanBinder` 的实现：

```java
public void bind(Map<String, Object> configurationProperties, boolean ignoreUnknownFields,
                 boolean ignoreInvalidFields, Object configurationBean) {
    // 1. 将 Map 转换为 Spring Boot 的 ConfigurationPropertySource
    Iterable<ConfigurationPropertySource> sources =
        ConfigurationPropertySources.from(new MapPropertySource("internal", configurationProperties));

    // 2. 创建 Binder（配置占位符解析器 + ConversionService）
    Binder binder = new Binder(sources, new PropertySourcesPlaceholdersResolver(sources), conversionService);

    // 3. 创建 BindHandler（控制 ignoreUnknownFields / ignoreInvalidFields）
    BindHandler bindHandler = BindHandlerUtils.createBindHandler(ignoreUnknownFields, ignoreInvalidFields);

    // 4. 绑定
    binder.bind("", Bindable.ofInstance(configurationBean), bindHandler);
}
```

`BindHandlerUtils.createBindHandler` 根据参数构建 `BindHandler` 链：
- `ignoreInvalidFields=true` -> 包装 `IgnoreErrorsBindHandler`（跳过类型转换错误）
- `ignoreUnknownFields=false` -> 包装 `NoUnboundElementsBindHandler`（不忽略未知字段时，报错）

这个实现比 `DefaultConfigurationBeanBinder` 更强大：松散绑定意味着 `app.user-name` 可以匹配 `app.userName` 字段，`app.USER_NAME` 也能匹配。这是 Spring Boot 配置绑定最显著的用户体验改进。

---

### @EnableConfigurationPropertiesExtension：启用注解

```java
@EnableConfigurationPropertiesExtension(
    adviseBindListener = true,       // 是否注入 BindListener 装饰（默认 true）
    publishEvents = true,            // 是否发布属性变更事件（默认 true）
    sources = {BEAN_FACTORY, SPRING_FACTORIES, JAVA_SERVICE_PROVIDER}  // BindListener 发现来源
)
```

- `adviseBindListener=true`：注册 `ListenableConfigurationPropertiesBindHandlerAdvisor`，自动装饰所有 `Binder.bind()` 调用
- `publishEvents=true`：注册 `EventPublishingConfigurationPropertiesBeanPropertyChangedListener`
- `sources`：控制 `BindListener` 的发现来源（与第 6 篇的 `BeanSource` 一致）

**自动启用**：`ConfigurationPropertiesAutoConfiguration` 是 Spring Boot `AutoConfiguration`：

```java
@AutoConfigureBefore(ConfigurationPropertiesAutoConfiguration.class)  // Spring Boot 的
@EnableConfigurationPropertiesExtension
public class ConfigurationPropertiesAutoConfiguration {
}
```

只要 classpath 中有 microsphere-spring-boot，此自动装配会自动激活。这意味着**所有 `@ConfigurationProperties` 的绑定过程自动获得可观察性**--无需用户添加任何注解或配置。

---

## 永恒原理

### 1. 观察者-装饰器-适配器三层分离

microsphere 的 BindListener 体系有三个角色：

- **观察者**（`BindListener`）：定义"观察到什么"（五个回调）
- **装饰器**（`ListenableBindHandlerAdapter`）：将观察者编织进绑定流程。它继承 `AbstractBindHandler`，在委托原始 `BindHandler` 后通知观察者
- **适配器**（`ListenableConfigurationPropertiesBindHandlerAdvisor`）：将装饰器注入 Spring Boot 的绑定入口。它实现 `ConfigurationPropertiesBindHandlerAdvisor`，在 `apply` 方法中创建装饰器

这是 03-spring 第 1 篇"观察者与处理者分离"原则在 Spring Boot 环境中的延伸。Spring Boot 的 `BindHandler` 是"处理者"（可以修改绑定结果），microsphere 的 `BindListener` 是"观察者"（只能观察），两者通过 `ListenableBindHandlerAdapter` 共存。

### 2. 两阶段绑定的生命周期

`EventPublishingConfigurationPropertiesBeanPropertyChangedListener` 的两阶段设计（`bound = false` 时拍摄快照，`bound = true` 时检测变更）源于 Spring Boot 的绑定生命周期：

```
应用启动时：Binder.bind() 被调用 → Bean 属性首次赋值 → SmartInitializingSingleton → bound = true
配置刷新时：Binder.bind() 再次被调用 → 与快照对比 → 发布变更事件
```

`bound` 标志的切换点是 `SmartInitializingSingleton.afterSingletonsInstantiated()`--在 03-spring 第 1 篇的 `BeanListener` 体系中也使用了相同的时机（`onBeanReady`）。这不是巧合：`afterSingletonsInstantiated` 是所有单例 Bean 创建完成的唯一可靠信号。

### 3. 属性快照与深度对比

`ConfigurationPropertiesBeanContext` 的属性变更检测使用"快照 + 对比"模式，而非"拦截 + 通知"模式。"拦截 + 通知"需要在每次属性赋值时触发，实现复杂且性能开销大。"快照 + 对比"在绑定完成后统一对比所有属性，发现差异后批量发布事件。

`deepEquals` 确保对数组、集合、嵌套对象进行深度比较。

### 4. BindHandler 装饰链与 BindHandlerUtils

Spring Boot 的 `BindHandler` 设计为装饰链模式--每个 `BindHandler` 可以持有下一个 `BindHandler` 作为委托。`BindHandlerUtils.createBindHandler` 利用这一点构建链：

```
IgnoreErrorsBindHandler  --> NoUnboundElementsBindHandler  --> DEFAULT
    (忽略无效字段)             (不忽略未知字段时校验)
```

microsphere 的 `ListenableBindHandlerAdapter` 插入在最外层，确保所有 Listener 在最终处理之前/之后被通知。

---

## 边界与反例

### 1. ConfigurationPropertiesBindHandlerAdvisor 的全局生效

`ListenableConfigurationPropertiesBindHandlerAdvisor` 是全局的--所有 `Binder.bind()` 调用都会被装饰。这意味着：

- 所有 `@ConfigurationProperties` 绑定都会触发 BindListener
- 非 `@ConfigurationProperties` 的 `Binder.bind()` 调用（如 `@ConfigurationBeanBinding`，第 5 篇）也会触发

如果你的 Listener 做了耗时操作（如数据库查询），所有绑定都会变慢。

**缓解**：在 `onStart` 中通过 `isConfigurationPropertiesBean(context)` 过滤，只处理 `@ConfigurationProperties` 的绑定。

### 2. 构造器绑定的差异检测开销

对于构造器绑定的 `@ConfigurationProperties`（不通过 setter，而是通过构造器参数），`ConfigurationPropertiesBeanContext` 需要创建克隆实例来计算差异。这个过程涉及 Bean 复制（`BeanUtils.copyProperties`），对于复杂嵌套对象可能开销较大。

**边界**：如果 `@ConfigurationProperties` Bean 的嵌套属性深度高（如多层嵌套 List/Map），克隆和差异检测的延迟可能不可忽视。建议大型配置对象使用 setter 绑定而非构造器绑定。

### 3. BindListener 的线程安全

`Binder.bind()` 通常在主线程中调用（启动阶段），但在配置刷新场景（Spring Cloud `@RefreshScope`）中可能被异步调用。`EventPublishingConfigurationPropertiesBeanPropertyChangedListener` 中对 `beanContexts` Map 的访问不是线程安全的。

**缓解**：配置刷新场景中应确保 `Binder.bind()` 的调用是串行的。Spring Cloud 的 `ContextRefresher` 在 `synchronized` 块中执行刷新，保证了这一点。

### 4. BindableConfigurationBeanBinder 的 BindHandler 与 ListenableBindHandlerAdapter 的交互

`BindableConfigurationBeanBinder.bind()` 中通过 `BindHandlerUtils.createBindHandler` 创建了 `BindHandler` 链（IgnoreErrors/NoUnboundElements）。但这个 `BindHandler` 在传入 `binder.bind()` 之前，会先经过 `ListenableConfigurationPropertiesBindHandlerAdvisor.apply()` 装饰。装饰后的 `ListenableBindHandlerAdapter` 持有原始 `BindHandler` 作为父级，所以 IgnoreErrors/NoUnboundElements 仍然生效。

如果同时使用了自定义 `ConfigurationPropertiesBindHandlerAdvisor`，装饰顺序取决于 Advior 的 `@Order`。microsphere 的 Advisor 没有显式设置 Order（默认 `LOWEST_PRECEDENCE`），所以自定义 Advior 通常排在前面。

### 5. BindableConfigurationBeanBinder 不经过 Advisor 装饰

`BindableConfigurationBeanBinder.bind()` 内部直接调用 `binder.bind(..., bindHandler)`，创建了 `Binder` 实例并传入 `BindHandler`。但与 Spring Boot 的 `Binder.bind()`（通过 `ConfigurationPropertiesBinder` 调用）不同，这里没有经过 `ConfigurationPropertiesBindHandlerAdvisor` 的装饰。

**后果**：`BindableConfigurationBeanBinder` 触发的绑定**不会**通知 `BindListener`。属性变更事件不会因为 `BindableConfigurationBeanBinder` 的绑定而发布。只有 Spring Boot 原生的 `@ConfigurationProperties` 绑定（通过 `ConfigurationPropertiesBinder` 触发）才会经过 Advisor 装饰。

**缓解**：如果需要 `BindableConfigurationBeanBinder` 也触发 BindListener，需要在 `bind` 方法中手动获取 Advisor 并装饰 `BindHandler`，而非依赖全局 Advisor 机制。

---

## 现代 Spring Boot（3.x）是否已支持？

| microsphere 特性 | Spring Boot 3.x 是否有等价物 | 说明 |
|------------------|------------------------|------|
| `BindListener` 可观察绑定 | 无 | Spring Boot 3.x 的 `BindHandler` 仍是"处理者"，无全局注册的"观察者" |
| `ConfigurationPropertiesBindHandlerAdvisor` 全局装饰 | 部分 | Spring Boot 3.x 有此接口，但需要手动实现装饰逻辑 |
| `ConfigurationPropertiesBeanPropertyChangedEvent` | 无 | Spring Boot 3.x 无属性变更事件 |
| `BindableConfigurationBeanBinder`（Binder 替代 DataBinder） | 无 | Spring Boot 3.x 没有统一的 `ConfigurationBeanBinder` SPI |

Spring Boot 3.x 的 `@ConfigurationProperties` 绑定仍然不可观察。`BindHandler` 虽然提供了 `onSuccess`/`onFailure` 等回调，但每次绑定都需要显式传入，不支持全局注册和自动发现。

Spring Cloud 的 `@RefreshScope` 通过销毁并重建 Bean 实现刷新，不触发属性变更事件。microsphere 的 `ConfigurationPropertiesBeanPropertyChangedEvent` 提供了更细粒度的"属性级"变更通知，而非"Bean 级"的重建。

---

## 小结

microsphere-spring-boot 的 ConfigurationProperties 可监听绑定，通过 **`BindListener` 体系 + `ConfigurationPropertiesBindHandlerAdvisor` 装饰器 + 属性快照变更检测** 三层设计，将 Spring Boot 的 `Binder.bind()` 从黑盒变为白盒。

核心组件：

- **`BindListener`**（5 回调）：观察者接口，全局注册，自动发现
- **`ListenableBindHandlerAdapter`**（装饰器）：不替换原始 `BindHandler`，附加通知
- **`ListenableConfigurationPropertiesBindHandlerAdvisor`**（适配器）：在每次 `Binder.bind()` 时自动注入装饰器
- **`EventPublishingConfigurationPropertiesBeanPropertyChangedListener`**（变更检测）：两阶段设计（快照 + 对比），发布 `ConfigurationPropertiesBeanPropertyChangedEvent`
- **`ConfigurationPropertiesBeanContext`**（583 行）：属性快照管理、差异检测、事件发布的核心引擎
- **`BindableConfigurationBeanBinder`**：Spring Boot `Binder` 实现，替代 DataBinder，支持松散绑定

整套体系通过 `ConfigurationPropertiesAutoConfiguration` 自动激活，对应用完全透明。这是 microsphere 在 03-spring 的 `BeanListener` 体系之后，将"可观察性"原则从 Bean 容器延伸到配置绑定的自然延续。
