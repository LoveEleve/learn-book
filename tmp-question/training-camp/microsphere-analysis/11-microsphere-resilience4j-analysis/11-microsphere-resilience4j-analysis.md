# Microsphere-Resilience4j 分析--4 个可迁移设计模式

> **核心命题**：Resilience4j 在国内不是主流（Sentinel 才是），但 microsphere-resilience4j 模块体现了 4 个可迁移的设计模式，适用于任何"多关注点包裹同一次调用"的框架。
> **本文回答**：哪 4 个模式？为什么值得学？在哪里用得上？

---

## 背景：为什么 Resilience4j 和 Sentinel 架构不同

Sentinel 只有一个入口 `SphU.entry()`，内部 slot chain 自动串联限流、熔断等所有模式。调用方不感知组合。

Resilience4j 有 6 个独立的容错模式（CircuitBreaker、RateLimiter、Retry、Bulkhead、ThreadPoolBulkhead、TimeLimiter），每种有自己的 Registry。调用方需要手动组合。这个"需要手动组合"的约束催生了以下 4 个设计模式。

---

## 模式 1：CallbackChain--递归式多关注点组合

### 问题

你想对同一次调用同时使用熔断 + 限流 + 重试。Resilience4j 原生写法是手动装饰：

```java
Supplier<String> s = () -> service.call();
s = Retry.decorateSupplier(retry, s);
s = RateLimiter.decorateSupplier(rateLimiter, s);
s = CircuitBreaker.decorateSupplier(circuitBreaker, s);
s.get();
```

每个调用点都要写 4 行装饰代码。如果换个地方用，又写一遍。

### 模式

microsphere 用一个 58 行的 `CallbackChain` 解决：

```java
class CallbackChain<T> implements ThrowableSupplier<T> {
    private final String entryName;
    private final ThrowableSupplier<T> delegate;       // 真实业务回调
    private final List<Resilience4jTemplate> templates; // 排序后的模板列表
    private int pos = 0;                                // 当前位置

    public T get() throws Throwable {
        if (pos < templates.size()) {
            // 还有模板没执行：取下一个，把"剩下的链"作为回调传入
            return templates.get(pos++).call(entryName, this::get);
        }
        // 所有模板都执行完了：执行真实业务回调
        return delegate.get();
    }
}
```

执行过程（3 个模板：Retry -> CircuitBreaker -> RateLimiter）：

```
CallbackChain.get()  pos=0
  -> RetryTemplate.call("name", this::get)
     Retry 开始重试逻辑
     -> CallbackChain.get()  pos=1
        -> CircuitBreakerTemplate.call("name", this::get)
           CircuitBreaker 获取许可
           -> CallbackChain.get()  pos=2
              -> RateLimiterTemplate.call("name", this::get)
                 RateLimiter 等待许可
                 -> CallbackChain.get()  pos=3
                    -> delegate.get()  真实业务调用
                 <- RateLimiter 记录结果
              <- CircuitBreaker 记录结果
           <- Retry 根据结果决定是否重试
```

**Retry 重试时的行为**：Retry 调用 `this::get()` 再次执行，但此时 `pos` 已经等于 `size`（第一次走完后递增到了 3）。所以重试时直接执行 `delegate.get()`，**跳过了 CircuitBreaker 和 RateLimiter**。

这意味着：
- CircuitBreaker 只看到第一次尝试的结果，不感知重试
- RateLimiter 只计一次许可，不计量重试产生的额外调用
- 只有业务代码被重新执行

这可能是有意设计（Retry 只负责重试业务逻辑，保护层看总体结果），也可能是一个遗漏。对比 Resilience4j 原生的 `decorateSupplier` 链式调用--原生方式下 Retry 重试时会重新走完整个装饰链（包括 CircuitBreaker 和 RateLimiter）。CallbackChain 的行为和原生方式不一致。

每个模板包裹下一个模板，最内层是真实业务。`pos` 是游标，`this::get` 是递归入口。

### 为什么值得学

这个模式不限于容错。任何"多个关注点按顺序包裹同一次调用"的场景都适用：

| 场景 | 模板列表 | 最内层 |
|------|---------|--------|
| 容错（本文） | Retry -> CircuitBreaker -> RateLimiter | 业务调用 |
| 链路追踪 | Trace -> Metrics -> Logging | 业务调用 |
| 认证授权 | Auth -> RateLimit -> Audit | 业务调用 |
| 事务管理 | Transaction -> Cache -> Business | 业务调用 |

关键是：**每个关注点只实现 `call(name, callback)`，不关心链上有其他什么关注点。** 关注点之间通过递归调用自然组合，顺序由优先级排序决定。

### 和 Sentinel 的对比

Sentinel 不需要 CallbackChain--slot chain 内部自动串联。但如果你要设计一个"多个独立组件需要组合"的框架（不是 Sentinel 那种一体化设计），CallbackChain 是最简洁的方案。

### 代价

**Retry 重试时跳过其他保护层。** 上文已分析：CallbackChain 的 `pos` 不重置，Retry 重试时只执行业务代码，CircuitBreaker 和 RateLimiter 不参与重试。对比 Resilience4j 原生的 `decorateSupplier` 链式调用（重试时重新走完整个装饰链），CallbackChain 的行为不一致。

CallbackChain 也是**非线程安全**的（`pos` 是实例字段）。每次调用创建一个新的 CallbackChain 实例，不能复用。这在实践中没有问题（每次调用创建一个 58 行的对象，开销可忽略）。

---

## 模式 2：isBeginSupported--不是所有操作都能拆成两阶段

### 问题

microsphere 的扩展点体系有两类适配器：
- **两阶段适配器**（如 Hibernate 的 pre/post 回调）：begin -> 业务 -> end
- **一次性适配器**（如 MyBatis 的 ExecutorFilter）：call(callback) 内部完成

Sentinel 只有一个入口 `SphU.entry/exit`，天然支持两阶段。但 Resilience4j 的 6 个模式中，有 3 个**根本无法拆成两阶段**：

| 模式 | 能拆吗 | 原因 |
|------|--------|------|
| CircuitBreaker | ✓ | acquirePermission -> 业务 -> onError/onSuccess |
| RateLimiter | ✓ | waitForPermission -> 业务 -> onResult |
| Bulkhead | ✓ | acquirePermission -> 业务 -> onComplete |
| **Retry** | **✗** | 重试需要重新执行回调，两阶段假设回调只执行一次 |
| **ThreadPoolBulkhead** | **✗** | 异步执行，begin 返回时结果未就绪 |
| **TimeLimiter** | **✗** | 需要 Future 包装，两阶段无法表达 |

### 模式

`Resilience4jOperations` 接口用 `isBeginSupported()` / `isEndSupported()` 让每个模板声明自己是否支持两阶段：

```java
public interface Resilience4jOperations<E> {
    default boolean isBeginSupported() { return true; }   // 默认支持
    default boolean isEndSupported() { return true; }     // 默认支持
    Resilience4jContext<E> begin(String name);
    void end(Resilience4jContext<E> context);

    default <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable {
        // 默认实现：用 begin/end 包裹 callback
        Resilience4jContext<E> context = begin(name);
        try {
            T result = callback.get();
            context.setResult(result);
            return result;
        } catch (Throwable e) {
            context.setFailure(e);
            throw e;
        } finally {
            end(context);
        }
    }
}
```

不支持两阶段的模板重写为 `false`：

```java
// RetryTemplate
public boolean isBeginSupported() { return false; }
public boolean isEndSupported() { return false; }
// 只重写 call()，不重写 doBegin/doEnd

// RetryTemplate.call()：用原生 decorateCheckedSupplier
public <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable {
    Retry retry = getEntry(name);
    return Retry.decorateCheckedSupplier(retry, callback::get).get();
}
```

组合器 `ChainableResilience4jFacade` 在两阶段模式下跳过不支持的模板：

```java
public Resilience4jContext<Resilience4jContext[]> begin(String name) {
    for (int i = 0; i < size; i++) {
        if (templates.get(i).isBeginSupported()) {  // ← 只调支持的
            subContexts[i] = templates.get(i).begin(name);
        }
    }
}
```

### 为什么值得学

**设计两阶段 API 时，永远要考虑"有没有操作天然不能拆"。** 如果你假设所有实现都能拆，遇到不能拆的就无法接入。

实际例子：
- **事务管理**：`begin -> commit/rollback` 是两阶段。但分布式事务的 Saga 模式不能拆--每一步都是独立的补偿调用。
- **缓存**：`get -> put` 是两阶段。但多级缓存的查询不能拆--L1 miss 后查 L2，L2 miss 后查 DB，整个过程是一次性回调。
- **限流**：`acquire -> release` 是两阶段。但令牌桶预消费不能拆--预消费意味着一次性判断能不能通过。

### 代价

两阶段模式下，不支持两阶段的模式被**静默跳过**。用户可能以为开启了 Retry + CircuitBreaker，但实际只有 CircuitBreaker 生效（因为 Hibernate 适配器用两阶段）。这是一个隐藏的行为差异，需要文档明确说明。

---

## 模式 3：双向事件桥接--第三方库事件 <-> Spring ApplicationEvent

### 问题

Resilience4j 有自己的事件系统：`CircuitBreaker.getEventPublisher().onSuccess(event -> ...)`。Spring 也有自己的事件系统：`@EventListener` + `ApplicationEvent`。两套系统不互通。

用户想在 Spring 中监听 CircuitBreaker 的状态转换事件，需要手动注册：

```java
// 不用 microsphere 的写法：手动注册每个 CircuitBreaker 的事件消费者
circuitBreaker.getEventPublisher().onStateTransition(event -> {
    applicationContext.publishEvent(new MyCircuitBreakerEvent(event));
});
// 每个 CircuitBreaker 都要写一遍
```

### 模式

microsphere 实现了**双向桥接**：

**方向一：Resilience4j 事件 -> Spring ApplicationEvent**

```java
public abstract class Resilience4jEventApplicationEventPublisher<E, ET>
        implements EventConsumer<ET>, RegistryEventConsumer<E>, ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    // 当新的 CircuitBreaker 被创建时，注册自己为事件消费者
    void onEntryAddedEvent(EntryAddedEvent<E> event) {
        E entry = event.getAddedEntry();
        EventPublisher publisher = entry.getEventPublisher();
        publisher.onEvent(this::consumeEvent);  // 注册到 Resilience4j
    }

    // Resilience4j 事件发生时，转发为 Spring ApplicationEvent
    void consumeEvent(ET event) {
        applicationEventPublisher.publishEvent(event);  // 转发到 Spring
    }
}
```

用户在 Spring 中用 `@EventListener` 监听：

```java
@EventListener
public void onCircuitBreakerStateTransition(CircuitBreakerOnStateTransitionEvent event) {
    logger.info("CircuitBreaker {} 状态从 {} 转为 {}",
        event.getCircuitBreakerName(),
        event.getStateTransition().getFromState(),
        event.getStateTransition().getToState());
}
```

**方向二：Spring Bean -> Resilience4j 事件消费者**

```java
public abstract class Resilience4jEventConsumerBeanRegistrar<E>
        implements RegistryEventConsumer<E>, BeanFactoryAware {

    void onEntryAddedEvent(EntryAddedEvent<E> event) {
        E entry = event.getAddedEntry();
        EventPublisher publisher = entry.getEventPublisher();

        // 反射查找 publisher 的所有 onXxxEvent(EventConsumer) 方法
        for (Method method : publisher.getClass().getMethods()) {
            if (method.getName().startsWith("on") && takesEventConsumer(method)) {
                // 查找 Spring BeanFactory 中对应类型的 Bean
                Class<?> eventType = getEventConsumerType(method);
                for (Object bean : beanFactory.getBeanProvider(eventType)) {
                    method.invoke(publisher, bean);  // 注册为 Resilience4j 事件消费者
                }
            }
        }
    }
}
```

用户在 Spring 中声明 `EventConsumer` Bean 即可被自动注册：

```java
@Bean
public EventConsumer<CircuitBreakerOnErrorEvent> onErrorLogger() {
    return event -> logger.error("CircuitBreaker {} 发生错误", event.getCircuitBreakerName());
}
```

### 为什么值得学

**任何有自己事件系统的第三方库**，集成到 Spring 时都需要这个模式：

| 第三方库 | 内部事件系统 | 桥接到 Spring |
|---------|------------|-------------|
| Resilience4j | EventPublisher.onEvent() | 本文 |
| Hibernate | EntityListener / Interceptor | microsphere-hibernate 的 EntityCallback |
| Druid | Filter 回调 | microsphere-druid 的 Filter 链 |
| Netty | ChannelHandler.pipeline() | Spring WebFlux 的适配 |
| Kafka | ConsumerRebalanceListener | Spring Kafka 的 ContainerProperties |

桥接的两个方向：
1. **库 -> Spring**：库事件发生时，转发为 Spring ApplicationEvent（用户用 `@EventListener` 监听）
2. **Spring -> 库**：Spring Bean 实现库的接口，自动注册为库的事件消费者

### 代价

反射查找 `onXxxEvent(EventConsumer)` 方法有性能开销。但只在 entry 创建时执行一次（不是每次事件触发），开销可忽略。

---

## 模式 4：每特性独立 @Enable--比全局开关更细粒度

### 问题

Sentinel 用一个全局开关 `microsphere.sentinel.enabled=true` 控制所有限流。简单，但不灵活--你只想用熔断不想用限流时，没有独立开关。

### 模式

Resilience4j 为每种容错模式提供独立的 `@Enable` 注解：

```java
@EnableCircuitBreaker(publishEvents = true, plugins = {"mybatis", "spring-web"})
@EnableRateLimiter
@EnableRetry
// 不加 @EnableBulkhead = 不启用舱壁
public class MyApplication { }
```

6 个注解，每个有三个可选属性：

| 属性 | 作用 | 默认值 |
|------|------|--------|
| `publishEvents` | 把 Resilience4j 事件转发为 Spring ApplicationEvent | false |
| `consumeEvents` | 把 Spring Bean 注册为 Resilience4j 事件消费者 | false |
| `plugins` | 加载哪些适配器（如 "mybatis"、"spring-web"） | {}（空） |

每个 `@EnableX` 通过 `@Import(EnableXRegistrar.class)` 触发注册器，注册器做五件事：

```
1. 注册该模式的 Registry Bean（如 CircuitBreakerRegistry）
2. 如果 publishEvents=true：注册事件转发器
3. 如果 consumeEvents=true：注册事件消费者扫描器
4. 如果 plugins 非空：加载指定的适配器插件
5. 把该模式加入 LazyResilience4jFacade 的 modules 集合
```

多个 `@EnableX` 累积到同一个 `LazyResilience4jFacade`。当 Spring 上下文刷新时，Facade 从 ApplicationContext 查找所有 Registry Bean，构建 `ChainableResilience4jFacade`。

### 为什么值得学

**框架的启用粒度应该和功能的独立性对齐。** 如果两个功能完全独立（可以单独使用），就应该有独立的开关。如果两个功能必须一起使用（如 Sentinel 的限流和熔断共享 slot chain），一个开关就够了。

判断标准：

| 场景 | 启用方式 |
|------|---------|
| 功能之间有共享状态/依赖 | 全局开关（Sentinel 模式） |
| 功能完全独立，各有自己的 Registry | 每特性 @Enable（Resilience4j 模式） |
| 功能有依赖关系但有条件组合 | 层级 @Enable（如 @EnableBulkhead 同时配置 ThreadPoolBulkhead） |

### 代价

每特性 @Enable 意味着用户需要知道有哪些特性、每个特性叫什么。6 个注解比 1 个开关学习成本高。对于只用熔断的用户，看到 6 个 @Enable 注解可能感到困惑。

---

## 和 Sentinel 的对比总结

| 维度 | Sentinel（国内主流） | Resilience4j（国际主流） |
|------|-------------------|----------------------|
| 容错模式 | 1 个入口，内部 slot chain 自动链 | 6 个独立 Registry，CallbackChain 手动链 |
| 两阶段支持 | 全部支持 | 3 支持，3 不支持（isBeginSupported） |
| 启用方式 | 全局开关 | 每特性 @Enable |
| 事件系统 | JMX | 双向 Spring ApplicationEvent 桥接 |
| 适配器覆盖 | 6 个（Web/MyBatis/Redis/Druid/Hibernate/P6Spy） | 5 个（Web/MyBatis/Druid/Hibernate/OpenFeign） |
| 自举程度 | 完全（所有适配器用 microsphere 扩展点） | 部分（MyBatis/Druid 直接用原生 SPI） |
| 国内主流 | ✓ | ✗ |

**不需要掌握 Resilience4j 的 API**。需要掌握的是上面 4 个设计模式--它们在任何"多关注点组合"的框架设计中都用得上。

---

## 对 sca-lab 的启发

1. **多关注点组合用 CallbackChain**--58 行代码解决"N 个模板按顺序包裹一次调用"
2. **设计两阶段 API 时留 opt-out**--`isBeginSupported()` 让不能拆的操作声明自己
3. **集成第三方库时做双向事件桥接**--库事件 -> Spring ApplicationEvent + Spring Bean -> 库消费者
4. **功能独立时用每特性 @Enable**--比全局开关更灵活，但要控制注解数量
