# 09-01：Logging 增强——启动期日志缓冲、过滤器桥接与国际化

> **核心命题**：`InMemoryAppender` 的"启动期缓冲、真正目标 Appender 就绪后才转移"是一个好的设计思路，但因为用 `@EventListener(ApplicationPreparedEvent)` 而不是同项目里另外三个类（`ApplicationLoggingAutoConfiguration` 等）那种正确的晚期事件监听方式，整条 Kafka 日志投递链路**从设计上就永远不会被触发**——日志缓存被静默清空丢弃。这是理解"Spring Boot 事件时序陷阱"的完整案例。顺带展开 microsphere 自己的框架无关 `Filter` 抽象如何桥接到 Log4j2，以及基于 `microsphere-i18n` 的日志国际化（`I18nLogger` 只完成了 `trace()` 一个重载，其余全部是空方法）。

---

## 一、项目定位（需求出发点，从代码反推）

09-observability 是 microsphere 生态的"可观测性补全层"，logging 子系统试图解决四个问题：

| 痛点 | 框架现状 | 答案 | 实现状态 |
|------|---------|------|---------|
| 启动期日志丢失 | Log4j2 远程 Appender（如 Kafka）依赖网络，启动早期未就绪 | `InMemoryAppender` 缓冲 + `AddingInMemoryAppenderListener`/`RemovingInMemoryAppenderListener` 生命周期 | **❌ 完全失效**（P1 时序 bug：`@EventListener(ApplicationPreparedEvent)` 永不触发→Kafka Appender 从未挂载→日志静默丢弃） |
| 跨日志框架统一动态调级 | Spring Boot Actuator `/loggers` 端点基于 Logback，若用 Log4j2 则缺统一接口 | `Logging` 接口对标 JDK `LoggingMXBean` | **⚠️ 只实现了 JDK 版本**（`StandardLogging`，无 Log4j2 实现，实际用途有限） |
| 标记日志跨实现一致性过滤 | Log4j2/Logback/Log4j1 各自的 Filter 接口不兼容 | 框架无关 `io.microsphere.logging.filter.Filter` + `Log4j2FilterAdapter` 桥接 | **⚠️ 可用但有未完成部分**（`CompositeFilter` OR/XOR 未实现，只有 Log4j2 桥接，无 Logback/Log4j1 版） |
| 跨项目 i18n 日志延伸 | `microsphere-i18n` 解决了业务消息国际化，但日志仍走硬编码 | `I18nLogger` + `I18nLog4j2Filter` | **❌ 基本不可用**（`I18nLogger` 仅 `trace()` 一个重载实现，`debug/info/warn/error` 全部空方法体） |

本文只展开前三个（第四个因 `I18nLogger` 基本未完成，只简述现状）。

---

## 二、`Logging` 接口：跨日志框架的统一动态调级抽象

```java
// Logging.java:31（项目源码）
public interface Logging {

    List<String> getLoggerNames();                          // 获取所有 logger 名

    String getLoggerLevel(String loggerName);               // 获取指定 logger 的日志级别

    void setLoggerLevel(String loggerName, String levelName);  // 运行时修改日志级别

    String getParentLoggerName(String loggerName);          // 获取父 logger 名
}
```

这是对标 JDK 官方 `java.util.logging.LoggingMXBean` 的抽象——Spring Boot Actuator 的 `/loggers` 端点背后是 `LoggingSystem` 抽象（`LogbackLoggingSystem`/`Log4J2LoggingSystem` 等），但 microsphere 选择**不依赖 Spring Boot**、直接用一组更轻量的接口表达同一个语义（4 个方法 vs 官方 `LoggingMXBean` 的 4 个同名方法——完全一一对应）。

目前唯一实现是 `StandardLogging`（`StandardLogging.java:33-56`），把全部操作委托给 JDK 系 `java.util.logging` 的 `LoggingMXBean`：

```java
public class StandardLogging implements Logging {
    private final static LoggingMXBean loggingMXBean = getLoggingMXBean();

    @Override
    public void setLoggerLevel(String loggerName, String levelName) {
        loggingMXBean.setLoggerLevel(loggerName, levelName);
    }
    // getLoggerNames/getLoggerLevel/getParentLoggerName 同理委托
}
```

**缺少 Log4j2 实现**：全项目没有 `Logging` 接口的 Log4j2 版本实现——这意味着如果要运行时修改 Log4j2 某包下的日志级别，目前只能走 Spring Boot Actuator 原生能力，microsphere 这套 `Logging` 抽象实际上只对接了 JDK 日志系统（Log4j2 侧的动态调级能力靠的是下面讲的那套 Filter/Appender 机制，与这个 `Logging` 接口无关）。读者注意区分"统一动态调级抽象"和"日志事件拦截/过滤"是两条平行线，本文后续讲的都是后者。

---

## 三、`InMemoryAppender` + 两个 Listener：启动期缓冲的完整链路

这是本文最核心的机制。先看完整设计意图，再看它为什么不生效。

### 3.1 `InMemoryAppender`：内存缓冲 + 转移

```java
// InMemoryAppender.java:39-105
public class InMemoryAppender extends AbstractLifeCycle implements Appender {
    public static final String NAME = "InMemory";
    private final Set<LogEvent> logEvents = new ConcurrentSkipListSet<>(LogEventComparator.INSTANCE);

    @Override
    public void append(LogEvent event) {
        logEvents.add(event);     // 所有日志事件缓存到内存有序集合
    }

    @Override
    public void stop() {
        super.stop();
        this.logEvents.clear();   // ⚠️ stop() 直接清空，缓存的日志事件被丢弃
    }

    public void transfer(Appender appender) {    // 核心方法：把缓存的事件转移到另一个 Appender
        Iterator<LogEvent> iterator = logEvents.iterator();
        while (iterator.hasNext()) {
            appender.append(iterator.next());
            iterator.remove();
        }
    }
}
```

设计意图：`stop()` 清空缓存是配合 `RemovingInMemoryAppenderListener` 使用的——在应用**完全启动后**，既然转移已经完毕，清空缓存释放内存——**但前提是 `transfer()` 已经被成功调用过**。如果转移从未发生，`stop()` → `clear()` 就是把缓存的内容**静默丢弃**。

### 3.2 `LogEventComparator`：一个潜在的日志丢失隐患

```java
// LogEventComparator.java:40-43
public int compare(LogEvent o1, LogEvent o2) {
    return Long.compare(o1.getTimeMillis(), o2.getTimeMillis());
}
```

`ConcurrentSkipListSet` 根据 `Comparator.compare()==0` 判断元素是否等同——**没有任何 tie-breaking**（次要排序键）。如果两条日志在同一毫秒内产生（高并发场景很常见），`compare()` 返回 0，第二条会被 `ConcurrentSkipListSet` 当作"重复元素"静默丢弃。这是一个**独立于核心时序 bug 之外的另一条日志丢失路径**——虽然丢日志的概率比主 bug 低很多（只影响同毫秒的高频日志），但值得在风险清单里标出。这里需要理解为什么选了 `ConcurrentSkipListSet` 而不是 `ConcurrentHashMap`+链表之类的方案：**`transfer()` 依靠有序迭代保证日志按时间顺序一致地转移到目标 Appender**——这是一个 trade-off：用数据结构的自然性质（有序集合的时间顺序）换取转移功能的时间一致性，代价是 Comparator 无法做 tie-breaking 时产生去重副作用。

### 3.3 添加与移除：两个 Listener 的生命周期分工

```java
// AddingInMemoryAppenderListener.java:36-48
public class AddingInMemoryAppenderListener extends OnceApplicationPreparedEventListener {
    @Override
    protected void onApplicationEvent(...) {
        InMemoryAppender inMemoryAppender = new InMemoryAppender();
        Log4j2Utils.addAppenderForAllLoggers(inMemoryAppender);   // ← 添加上
    }
}
```

```java
// RemovingInMemoryAppenderListener.java:36-48
public class RemovingInMemoryAppenderListener implements ApplicationListener<ApplicationStartedEvent> {
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        InMemoryAppender inMemoryAppender = findInMemoryAppender();
        Log4j2Utils.removeAppenderForAllLoggers(inMemoryAppender);  // ← 移除（触发 stop()→clear()）
    }
}
```

两个 Listener 的注册方式不同：

| Listener | 注册方式 | 监听事件 | 注册位置 |
|----------|---------|---------|---------|
| `AddingInMemoryAppenderListener` | `spring.factories` 里的 `ApplicationListener` key | `ApplicationPreparedEvent`（继承 `OnceApplicationPreparedEventListener`） | `logging-spring-boot/src/main/resources/META-INF/spring.factories` |
| `RemovingInMemoryAppenderListener` | 同上 | `ApplicationStartedEvent` | 同上 |

两者都是 `spring.factories` 里的 `org.springframework.context.ApplicationListener` 注册——**原生** Spring Boot 级监听器，在 `SpringApplication` 构造阶段就被 `SpringFactoriesLoader` 加载，不需要等 Bean 实例化。所以这两个 Listener **肯定能收到各自监听的事件**——时序是否正确取决于它们监听的事件本身。

`OnceApplicationPreparedEventListener` 是 `ApplicationListener<ApplicationPreparedEvent>` 的子类（`OnceApplicationPreparedEventListener.java:46`），确认 `AddingInMemoryAppenderListener` 监听的确实是 `ApplicationPreparedEvent`。

**执行序设计意图**：

```
ApplicationPreparedEvent → 添加 InMemoryAppender（开始缓存日志）
    ↓（refresh 过程，真正的 Appender 建立）
ApplicationStartedEvent → 移除 InMemoryAppender（stop→clear 释放内存）
```

中间的 `refresh()` 过程应该由 `Log4j2AutoConfiguration.KafkaAppenderConfiguration` 完成"建 Kafka Appender + `transfer()` + **添加 Kafka Appender 代替** InMemory——这才是整个设计的完整闭环。

### 3.4 `KafkaAppenderConfiguration`：设计上承担"建 Kafka 替换内存缓冲"的环节

```java
// Log4j2AutoConfiguration.java:72-95（内部类 KafkaAppenderConfiguration）
@ConditionalOnClass(name = "org.apache.kafka.clients.KafkaClient")
@ConditionalOnProperty(prefix = PREFIX, name = "properties.bootstrap.servers")
class KafkaAppenderConfiguration {
    @EventListener(ApplicationPreparedEvent.class)       // ← 同样是 ApplicationPreparedEvent
    public void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
        this.context = event.getApplicationContext();
        initializeKafkaAppender();
    }

    private void initializeKafkaAppender() {
        KafkaAppender kafkaAppender = buildKafkaAppender();
        InMemoryAppender inMemoryAppender = findInMemoryAppender();  // 找到上一步添加的内存 Appender
        inMemoryAppender.transfer(kafkaAppender);                    // 转移缓存的日志
        Log4j2Utils.addAppenderForAllLoggers(kafkaAppender);         // 把 Kafka Appender 挂到所有 logger
    }
}
```

**这就是关键连接点**：`KafkaAppenderConfiguration` 承担了"构建真正的 Kafka Appender → 从 `InMemoryAppender` 转移缓存 → 把 Kafka Appender 挂上去"这一整步——如果这一步不执行，Kafka Appender 从未被挂载，`InMemoryAppender` 里的缓存日志等不到转移，只会等到 `RemovingInMemoryAppenderListener` 移除时被 `stop()`→`clear()` 清空丢弃。

---

## 四、核心 bug：为什么 `KafkaAppenderConfiguration.onApplicationPreparedEvent` 永远不会被触发

### 4.1 `@EventListener` 注解方法的注册时机

`@EventListener` 注解方法不是"声明了这个注解就自动生效"——它需要被专门扫描和注册。Spring 用 `EventListenerMethodProcessor` 做这件事（`EventListenerMethodProcessor.java:65-66`）：

```java
public class EventListenerMethodProcessor
        implements SmartInitializingSingleton, ApplicationContextAware, BeanFactoryPostProcessor {
```

关键方法 `afterSingletonsInstantiated()`（`EventListenerMethodProcessor.java:111-154`）：

```java
@Override
public void afterSingletonsInstantiated() {
    String[] beanNames = beanFactory.getBeanNamesForType(Object.class);
    for (String beanName : beanNames) {
        // 遍历所有 Bean，检查是否有 @EventListener 注解的方法
        // 有 → 创建 ApplicationListenerMethodAdapter 并注册到 context
        processBean(beanName, type);
    }
}
```

`SmartInitializingSingleton.afterSingletonsInstantiated()` 是在**容器所有单例 Bean 实例化完成之后**触发的回调。

### 4.2 `refresh()` 方法的执行顺序

用 `/data/workspace/source-code/code/spring/spring-framework`（v6.2.17）的 `AbstractApplicationContext.refresh()`（`AbstractApplicationContext.java:595-630`）验证：

```
invokeBeanFactoryPostProcessors(beanFactory);   // 610 行
registerBeanPostProcessors(beanFactory);         // 612 行
initApplicationEventMulticaster();                // 619 行
registerListeners();                             // 625 行 ← 早期事件在这里重放
finishBeanFactoryInitialization(beanFactory);    // 628 行 ← 单例在这里实例化，afterSingletonsInstantiated() 在这里触发
```

- **`registerListeners()`**（第 625 行）：把 spring.factories 里注册的原生 `ApplicationListener`（包括 `AddingInMemoryAppenderListener`/`RemovingInMemoryAppenderListener`）注册到 `applicationEventMulticaster`，并**重放**之前缓存在 `earlyApplicationEvents` 里的事件
- **`finishBeanFactoryInitialization()`**（第 628 行）：实例化所有非延迟单例 Bean，在这步**末尾**触发所有 `SmartInitializingSingleton.afterSingletonsInstantiated()`——**`EventListenerMethodProcessor` 在这一步才扫描 `@EventListener` 方法并注册**

**顺序不等式**：`registerListeners()` → `finishBeanFactoryInitialization()`。在 `registerListeners()` 重放 `ApplicationPreparedEvent` 的那一刻，`@EventListener` 方法还没有被 `EventListenerMethodProcessor` 扫描注册——**这些方法错过了早期事件的重放**。

### 4.3 为什么 `AddingInMemoryAppenderListener` 能收到而 `KafkaAppenderConfiguration` 收不到

两者的根本区别在于注册方式：

| | 注册方式 | 被 `ApplicationEventMulticaster` 感知的时间点 |
|---|---|---|
| `AddingInMemoryAppenderListener` | `spring.factories` 里的 `org.springframework.context.ApplicationListener` key —— **原生**监听器 | `SpringApplication` 构造阶段就被 `SpringFactoriesLoader` 加载，**天然感知所有生命周期事件**，包括 `ApplicationPreparedEvent` |
| `KafkaAppenderConfiguration.onApplicationPreparedEvent` | `@EventListener` 注解 —— **Bean 方法级**监听器 | 必须等 `EventListenerMethodProcessor.afterSingletonsInstantiated()` 扫描注册，而这个回调在 `finishBeanFactoryInitialization()` 末尾才触发——**晚于 `registerListeners()` 的早期事件重放** |

### 4.4 `earlyApplicationEvents` 机制为什么也没救到

Spring 有"早期事件缓存"机制（`AbstractApplicationContext.java:250,451-452,698`）：在 `applicationEventMulticaster` 初始化之前，`publishEvent` 发布的事件先缓存到 `earlyApplicationEvents` 这个 `LinkedHashSet` 里，等 `registerListeners()` 阶段统一重放。

但重放发生在 `registerListeners()`（第 625 行），目标接收者包括两类：
1. 所有 `getApplicationListeners()` 返回的原生监听器（`AddingInMemoryAppenderListener` 在这里）
2. 所有 `getBeanNamesForType(ApplicationListener.class, true, false)` 返回的监听器 Bean 名（通过 `addApplicationListenerBean` 注册，是**懒初始化**的代理引用）

第 2 类关键点：`addApplicationListenerBean` 注册的是一个 **`ApplicationListenerBean`** 懒引用——它不是立即初始化监听器 Bean，而是记录这个"bean name"，等真正的事件广播时才通过 `getBean(beanName)` 去获取。**如果那个 bean 还没有被实例化**（在 `registerListeners()` 时刻，`KafkaAppenderConfiguration` 内部的 Bean 还没创建），`getBean` 会触发创建——但 `ApplicationListenerBean.onApplicationEvent` 的实现里会从容器获取这个 bean 的 `ApplicationListener` 实例。而 `KafkaAppenderConfiguration` 本身不是 `ApplicationListener` 接口实现，它的 `@EventListener` 方法是由 `EventListenerMethodProcessor` 在之后生成的 `ApplicationListenerMethodAdapter`代理的——这个代理在`registerListeners()` 阶段还不存在，只有等到 `afterSingletonsInstantiated()` 才会被创建并注册。

**所以即使 `KafkaAppenderConfiguration` 作为 Bean 早期可以被发现，它的 `@EventListener` 方法级监听器在 `registerListeners()` 时刻仍不存在**——重放时找不到对应的 `ApplicationListener` 实例。

### 4.5 连锁失效的完整链路

```
Log4j2AutoConfiguration.KafkaAppenderConfiguration 的
  @EventListener(ApplicationPreparedEvent.class)
  → 永远不会被触发
  → initializeKafkaAppender() 永远不会执行
    → buildKafkaAppender() 从未执行
      → KafkaAppender 从未被挂载到 Log4j2 LoggerContext
        → 所有日志直接丢失（InMemoryAppender 缓存后等不到 transfer）
        → MicrometerAutoConfiguration.KafkaMetricsConfiguration
            .getKafkaProducer() 的 findAppender(loggerName)
            永远返回 null
            → KafkaClientMetrics 也永不绑定
```

---

## 五、正反对照：同一个项目里，正确的 `@EventListener` 用法

下面三个类都在同一个 `logging-spring-boot` 模块里，做的事情类似（监听某个事件并打日志），但选的事件类型完全不同：

| 类 | 监听的事件 | 事件触发时机 | `@EventListener` 是否生效 |
|---|---|---|---|
| `ApplicationLoggingAutoConfiguration` | `ApplicationStartedEvent` / `ApplicationFailedEvent` | `refresh()` **之后** | ✅ 生效（晚于 `finishBeanFactoryInitialization`） |
| `WebMvcLoggingAutoConfiguration` | `ServletRequestHandledEvent` | 第一个请求处理完成时 | ✅ 生效（请求处理阶段，容器已完全初始化） |
| `WebServerLoggingAutoConfiguration` | `WebServerInitializedEvent` | WebServer 启动完成时 | ✅ 生效（容器已完全初始化） |
| `KafkaAppenderConfiguration` | `ApplicationPreparedEvent` | `refresh()` **之前** | ❌ 永不触发（本文已证） |

**关键教训**：`@EventListener` 注解方法的介入时机从**物理上**被限制在 `finishBeanFactoryInitialization()`（单例实例化完成）之后——监听该时间点之前发布的事件（`ApplicationPreparedEvent` 及更早的事件）在语法上可以声明，但语义上永不可能接收。同一个项目里，作者对另外三个类选了正确的事件类型，唯独对 `KafkaAppenderConfiguration` 选了一个 "无法接收" 的事件——这不是 `@EventListener` 机制本身的缺陷，而是这个具体的事件类型选择错误。

---

## 六、microsphere 自定义 Filter 抽象与 Log4j2 桥接

这是与"启动期缓冲"平行的另一条日志增强链路——microsphere 自己定义了一套**框架无关**的 Filter 抽象，再桥接到 Log4j2。

### 6.1 `io.microsphere.logging.filter.Filter`：框架无关的三态过滤

```java
// Filter.java:25-76
public interface Filter {
    Result filter(String loggerName, String level, String message);

    enum Result {
        ACCEPT,   // 接受（记录此条日志）
        NEUTRAL,  // 中立（让后续过滤器决定）
        DENY;     // 拒绝（丢弃此条日志）
    }
}
```

只关心"logger名 / 日志级别 / 消息内容"三个通用维度——不依赖 Log4j2/Logback 任何特定类型。

### 6.2 `AbstractFilter`：模板方法

```java
// AbstractFilter.java:27-66
public abstract class AbstractFilter implements Filter {
    private Filter.Result onMatch = Filter.Result.NEUTRAL;      // 匹配成功时的返回值
    private Filter.Result onMismatch = Filter.Result.NEUTRAL;   // 匹配失败时的返回值

    @Override
    public final Result filter(String loggerName, String level, String message) {
        if (matches(loggerName, level, message)) {
            if (onMatch != null) { return onMatch; }    // ← final 方法，子类不可覆写
        }
        return onMismatch;
    }

    protected abstract boolean matches(String loggerName, String level, String message);  // ← 子类实现
}
```

与 13-mybatis 的 `InterceptorsExecutorFilterAdapter`、14-druid 的 `AbstractStatementFilter.execute`、本模块 micrometer 侧的 `AbstractMeterBinder.bindTo` 属于同一类设计——**final 收敛主流程 + 抽象子类实现具体判断**，是同一作者跨第 4 个项目复用的模式。细微差异：这里的 `onMatch != null` 检查——如果显式调用 `setOnMatch(null)` 把匹配成功结果置空，那即使 `matches()` 返回 true 也不走 `onMatch`，转而返回 `onMismatch`（默认 `NEUTRAL`）。这个设计让调用方可以动态调整"匹配成功的响应策略"（从"匹配即接受"改成"匹配时走中立，让后续过滤器决定"），相当于把判断逻辑拆成了两段：**是否匹配**（`matches`）+ **匹配后的行为**（`onMatch`/`onMismatch`），两段可以独立配置。

### 6.3 `LoggingNameFilter`：按 logger 名过滤

```java
// LoggingNameFilter.java:27-39
public class LoggingNameFilter extends AbstractFilter {
    private String loggerName;

    @Override
    public boolean matches(String loggerName, String level, String message) {
        return Objects.equals(this.loggerName, loggerName);    // ← 精确匹配
    }
}
```

配合 `setOnMatch(ACCEPT)` + `setOnMismatch(NEUTRAL)`，可以实现"只允许某个包的日志通过"的控制。

### 6.4 `Log4j2FilterAdapter`：桥接到 Log4j2 官方 `AbstractFilter`

```java
// Log4j2FilterAdapter.java:36-74
@Plugin(name = "Log4j2FilterAdapter", category = Node.CATEGORY, ...)
public class Log4j2FilterAdapter extends org.apache.logging.log4j.core.filter.AbstractFilter {
    private final Filter filter;  // ← 持有 microsphere 自定义 Filter

    public Log4j2FilterAdapter(Filter filter) {
        super(toResult(filter, AbstractFilter::getOnMatch),
              toResult(filter, AbstractFilter::getOnMismatch));
        this.filter = filter;
    }

    @Override
    public Result filter(LogEvent event) {
        String loggerName = event.getLoggerName();
        String level = event.getLevel().name();
        String message = event.getMessage().getFormattedMessage();
        return toResult(filter.filter(loggerName, level, message));  // ← 转换枚举
    }
}
```

桥接逻辑干净：microsphere `Filter.Result`（三值）→ Log4j2 `AbstractFilter.Result`（六值 `ACCEPT/NEUTRAL/DENY`），通过 `toResult()` switch 做一对一映射。这种"两段式"桥接的价值在于：**Filter 的实现逻辑（`matches`/`onMatch`/`onMismatch`）写在一次，就能同时跑在 Log4j2、Logback、Log4j1 等不同日志框架上**——只要写一个对应的桥接适配器（`XxxFilterAdapter`），不需要复制 Filter 逻辑。目前项目中只有 `Log4j2FilterAdapter` 一个桥接实现，但这个架构的设计方向是"写一次过滤逻辑，跨日志框架通用"。构造器里用 `AbstractFilter.getOnMatch()`/`getOnMismatch()` 做 Log4j2 侧的初始状态设定——这是一次性取值（`extends AbstractFilter` 后传给父类构造器），后续每次 `filter(LogEvent)` 直接从 LogEvent 提取三个通用维度转交内嵌 microsphere Filter，不再读这边的 `onMatch`/`onMismatch`。

### 6.5 `CompositeFilter`：已知未完成的组合器

```java
// CompositeFilter.java:27-63
public class CompositeFilter implements Filter {
    private List<Filter> filters;
    private Operator operator = Operator.OR;  // 默认 OR

    @Override
    public Result filter(String loggerName, String level, String message) {
        Result result = Result.NEUTRAL;
        for (Filter filter : filters) {
            if (Operator.AND.equals(operator)) {      // ← 只实现了 AND（且语义是错的）
                Result innerResult = filter.filter(loggerName, level, message);
                if (Result.ACCEPT.equals(innerResult)) {
                    result = Result.ACCEPT;           // ← 任意一个子 Filter 接受就算 AND 全部接受？
                }
            }
        }
        return result;
    }
}
```

**代码里自带 `// TODO`**——这是作者已知未完成：
- `Operator.OR`（默认操作符）**完全没有实现**（for 循环直接跳过 OR 分支），所以默认配置下永远返回 `NEUTRAL`
- `Operator.XOR` 完全未实现
- AND 分支的语义也是错的——变量名叫 AND，但循环内部"任一子 Filter 返回 ACCEPT 就把 result 设成 ACCEPT"是 OR 语义
- `DENY` 结果被完全忽略（子 Filter 返回 DENY 时，`CompositeFilter` 不可能返回 DENY，无法实现"任一 Filter 拒绝即整体拒绝"）

由于作者明确标注了 `// TODO`（`CompositeFilter.java:36`），这不是隐藏 bug，而是明确标记的未完成状态——在文章里标注为"作者已知未完成"，而非当作漏洞批评。

---

## 七、国际化与死代码（简述）

### 7.1 `I18nLog4j2Filter`：变异型 Filter

```java
// I18nLog4j2Filter.java:58-89
@Override
public Result filter(LogEvent event) {
    MutableLogEvent mutableLogEvent = new MutableLogEvent();
    mutableLogEvent.initFrom(event);
    interpolate(mutableLogEvent);         // 替换 {code} 为本地化文本
    return super.filter(mutableLogEvent); // 传给下一个 Filter / Appender
}

private void interpolate(MutableLogEvent event) {
    String formattedMessage = event.getFormattedMessage();
    String resolvedCode = resolveCode(formattedMessage);          // 从 {xxx} 中提取 code
    String localizedMessage = serviceMessageSource.getMessage(resolvedCode, parameters); // i18n 查表
    event.setMessage(newMessage);                                // 替换消息
}
```

这不是传统意义上的"过滤器"——它不仅判断是否放行，还会**修改日志事件本身**（用国际化文本替换占位符）。依赖 `microsphere-i18n` 项目的 `ServiceMessageSource`。注意 `rebuildMessage` 里 `// TODO support more Message types`——只支持 `SimpleMessage`，`MapMessage` 等其他类型会被跳过不做替换。

### 7.2 `I18nLogger`：SLF4J 装饰器，仅完成一个重载

`I18nLogger.java:29-378` 实现了 `org.slf4j.Logger` 接口的全部方法，但**实际有实质代码的只有 `trace(String, Object...)` 这一个重载**（第 87-97 行）——调试/信息/警告/错误四个级别全部方法体都是空的，`isXxxEnabled()` 全部硬编码返回 `false`。类名上标注 `TODO : Finish`（第 29 行）。这不算 bug（作者明确说未完成），但说明国际化日志装饰器这条路**只开了个头就停了**。

### 7.3 `LoggingConfiguration` + `DefaultKafkaLayout`

- `LoggingConfiguration.java:30` javadoc 里 `@since TODO`——全项目无任何主代码引用 `getLoggingFiltersLocation()`，整套"通过配置指定 Filter 文件位置"的能力是死代码
- `DefaultKafkaLayout`（`DefaultKafkaLayout.java:42-141`）是一个精巧设计：为 Kafka Appender 的日志格式选择时，自动**复用**每个 logger 已有的 `RollingFileAppender`/`FileAppender` 的 layout，让 Kafka 里看到的日志格式和本地文件日志保持一致；且明确排除 `InMemoryAppender.NAME`（`DefaultKafkaLayout.java:79`），避免把缓冲用的临时 Appender 误判为"目标 layout 来源"。但这个精巧设计经由核心 bug 的连锁失效（Kafka Appender 从未挂载），实际上也从未被执行过。

---

## 八、问题清单（已证）

| # | 问题 | 证据 |
|---|------|------|
| P1 | **`KafkaAppenderConfiguration.onApplicationPreparedEvent` 永不触发**：`@EventListener` 监听 `ApplicationPreparedEvent` 语义上不可能接收——`registerListeners()` 重放早期事件早于 `EventListenerMethodProcessor.afterSingletonsInstantiated()` 注册 `@EventListener` 监听器 | `AbstractApplicationContext.java:625/628` + `EventListenerMethodProcessor.java:111-154` |
| P1a | 连锁：Kafka Appender 从未挂载→日志静默丢弃 | `Log4j2AutoConfiguration.java:91-96` |
| P1b | 连锁：`KafkaClientMetrics` 永不绑定（`getKafkaProducer` → `findAppender` 返回 null） | `MicrometerAutoConfiguration.java:248-258` |
| P2 | `InMemoryAppender`+`LogEventComparator`：`ConcurrentSkipListSet` 只比较时间戳，同毫秒内多条日志被当重复元素静默丢弃 | `LogEventComparator.java:40-43` |
| P3 | `CompositeFilter.filter()`：`// TODO`，OR/XOR 完全未实现，AND 语义写错，DENY 被忽略（**作者已知未完成**） | `CompositeFilter.java:36` |
| P4 | `AddingInMemoryAppenderListener` 的父类 `OnceApplicationPreparedEventListener` 声明了抽象方法 `isIgnored`，但子类未覆写——受限于 `0.0.1-SNAPSHOT` 版本与本地 spring-boot 源码版本不一致无法验证，暂不作为结论 | `OnceApplicationPreparedEventListener.java:175` + `AddingInMemoryAppenderListener.java` 无覆写 |
| P5 | `I18nLogger`：`TODO : Finish`，仅 `trace(String,Object...)` 一个重载实现，其余全部空方法体 | `I18nLogger.java:29,140-378` |
| P6 | `LoggingConfiguration`：死代码，`@since TODO`，无主代码引用 | `LoggingConfiguration.java:30` + 无引用 |
| P7 | `DefaultKafkaLayout`：设计精巧但因 P1 连锁失效从未被执行 | 推理，非源码可证（因为 P1 已严格证明） |
| P8 | `ApplicationLoggingAutoConfigurationTest`：验证的是 `UncaughtExceptionHandler` 类型而非 `onApplicationStartedEvent` 的日志语义——测试覆盖点不精确 | `ApplicationLoggingAutoConfigurationTest.java:36-39` |
| P9 | `WebMvcLoggingAutoConfigurationTest`/`WebServerLoggingAutoConfigurationTest`：完全不验证事件监听器业务逻辑，一个只断言 HTTP 404，一个只 `assertNotNull` Bean | `WebMvcLoggingAutoConfigurationTest.java:39-43` + `WebServerLoggingAutoConfigurationTest.java:43-45` |
| P10 | `Log4j2AutoConfiguration`/`InMemoryAppender`/`KafkaAppenderConfiguration`：完全没有测试覆盖——P1 严重 bug 处于零测试保护状态 | 全文 grep 无对应 `*Test.java` |

---

## 九、测试佐证

- `ApplicationLoggingAutoConfigurationTest`：验证 `UncaughtExceptionHandler` 类型正确——**实际验证的是 `registerLoggingUncaughtExceptionHandlerAsDefault()` 的副作用，不是 `onApplicationStartedEvent` 的日志输出语义**，测试覆盖了行为但覆盖点不精确（P8）
- `WebMvcLoggingAutoConfigurationTest`：发一个 `GET /`，断言 404——**完全没有验证 `onServletRequestHandledEvent` 真的被调用**，测试名和实际验证内容分裂（P9）
- `WebServerLoggingAutoConfigurationTest`：只 `assertNotNull` Bean 存在——**完全不验证事件监听器业务逻辑**
- `InMemoryAppender`/`Log4j2AutoConfiguration`/`KafkaAppenderConfiguration`：**完全没有测试**——P1 这个严重 bug 没有测试保护

---

## 十、小结（引用要点）

- **P1** 是本文的核心发现：`@EventListener(ApplicationPreparedEvent.class)` 在 Spring Boot 时序上永不可能触发——`registerListeners()` 事件重放早于 `EventListenerMethodProcessor` 注册监听器——这个 bug 导致 Kafka Appender 从未挂载，启动期日志最终被 `RemovingInMemoryAppenderListener` 清空丢弃（`stop()`→`clear()`），连带 `KafkaClientMetrics` 也永不绑定
- **同项目正反对照**：`ApplicationLoggingAutoConfiguration`/`WebMvcLoggingAutoConfiguration`/`WebServerLoggingAutoConfiguration` 三个类选了正确的**晚期**事件（`Started`/`Failed`/`ServletRequestHandled`/`WebServerInitialized`），同项目里正反证例完备
- **Spring Boot 事件时序"红线"**：任何依赖 `@EventListener` 监听早于 `finishBeanFactoryInitialization()` 完成的事件，在设计上无效——这是一条可以推广到其他 microsphere 项目的验证规则
- **`InMemoryAppender` 设计本身没错**：缓冲-转移-移除的完整设计思路正确，只是实现上需要改用正确的事件触发方式（应该用 `ApplicationEnvironmentPreparedEvent` 或直接实现 `ApplicationListener<ApplicationPreparedEvent>` 接口而非 `@EventListener` 注解）
- **与作者惯用模式的关系**：`AbstractFilter.filter()` 的 final 收敛模式是同一作者在 microsphere 生态中的第 4 次使用（延续 13-mybatis/14-druid/本模块 micrometer 侧的模板方法设计），`Log4j2FilterAdapter` 桥接模式干净简洁
