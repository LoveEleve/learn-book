# 03-03 事件拦截器链

## 问题：Spring 事件不可拦截

Spring 的事件机制由三个角色组成：

- **ApplicationEventPublisher**：发布事件
- **ApplicationEventMulticaster**：分发事件给所有匹配的 Listener
- **ApplicationListener**：处理事件

`SimpleApplicationEventMulticaster` 的 `multicastEvent` 流程是：解析事件类型 -> 遍历所有注册的 Listener -> 对每个匹配的 Listener 调用 `invokeListener` -> 最终调用 `listener.onApplicationEvent(event)`。

这个过程是**不可拦截的**。你无法在事件到达 Listener 之前插入逻辑，也无法在某个 Listener 处理完事件后执行清理。常见的痛点：

| 场景 | Spring 能力 | 问题 |
|------|-----------|------|
| 事件耗时统计 | 无 | 不知道哪个 Listener 处理慢 |
| 事件过滤/短路 | 无 | 无法阻止某个事件到达特定 Listener |
| 事件审计日志 | 实现 `ApplicationListener` | 需要为每种事件写一个 Listener，无法统一拦截 |
| 事件链路追踪（Trace ID） | 无 | 无法在事件分发前后注入/清理 Trace 上下文 |
| 异步事件异常处理 | `SimpleApplicationEventMulticaster#setErrorHandler` | 只能设一个全局 ErrorHandler，不能按 Listener 定制 |

microsphere-spring 的 `context/event` 包用 **两级拦截器链** 填补了这个空白。

---

## 设计：两级拦截器链

### 两级拦截的粒度差异

microsphere 区分了两个拦截层次，因为它们的关注点不同：

```
publishEvent(event)
    │
    │  ─── 第一级：事件级拦截（ApplicationEventInterceptor）───
    │       拦截的是"事件本身"，决定事件是否应该被分发
    │       可以短路：阻止事件到达任何 Listener
    │
    ├── interceptor1.intercept(event, eventType, chain)
    ├── interceptor2.intercept(event, eventType, chain)
    └── chain 终端 -> multicastEvent -> 遍历所有匹配 Listener
            │
            │  ─── 第二级：监听器级拦截（ApplicationListenerInterceptor）───
            │       拦截的是"Listener 处理事件"，决定该 Listener 是否应该处理
            │       可以短路：阻止事件到达该 Listener
            │
            ├── interceptor1.intercept(listener, event, chain)
            ├── interceptor2.intercept(listener, event, chain)
            └── chain 终端 -> listener.onApplicationEvent(event)
```

**为什么需要两级**：

- **事件级拦截器**关注事件本身：事件来源、事件类型、事件内容。典型用途是审计日志（记录每个事件被发布）、安全过滤（阻止敏感事件被分发）、Trace 上下文注入（在事件分发前设置 Trace ID）
- **监听器级拦截器**关注 Listener 与事件的组合：哪个 Listener 处理哪个事件、处理耗时多长。典型用途是性能监控（测量每个 Listener 的处理时间）、异常处理（按 Listener 定制错误处理策略）、条件过滤（根据运行时状态决定是否让该 Listener 处理）

一级拦截器在事件分发前只执行一次，二级拦截器对每个匹配的 Listener 各执行一次。如果有 5 个 Listener 匹配同一事件，一级拦截器执行 1 次，二级拦截器执行 5 次。

---

### 拦截器接口

**事件级拦截器**：

```java
public interface ApplicationEventInterceptor extends Ordered {

    void intercept(ApplicationEvent event, ResolvableType eventType,
                   ApplicationEventInterceptorChain chain);

    @Override
    default int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
```

**监听器级拦截器**：

```java
public interface ApplicationListenerInterceptor extends Ordered {

    void intercept(ApplicationListener<?> listener, ApplicationEvent event,
                   ApplicationListenerInterceptorChain chain);

    @Override
    default int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
```

两者都实现 `Ordered` 接口，支持按 `@Order`/`Ordered` 排序。默认优先级为 `LOWEST_PRECEDENCE`（最后执行）。

**短路能力**：拦截器可以选择不调用 `chain.intercept(...)`，从而短路整个链。事件级短路阻止事件到达任何 Listener；监听器级短路阻止事件到达当前 Listener。

---

### 责任链实现

两个链的实现结构完全一致，以 `DefaultApplicationListenerInterceptorChain` 为例：

```java
class DefaultApplicationListenerInterceptorChain implements ApplicationListenerInterceptorChain {

    private final Iterator<ApplicationListenerInterceptor> iterator;
    private final BiConsumer<ApplicationListener<?>, ApplicationEvent> listenerAndEventConsumer;

    @Override
    public void intercept(ApplicationListener<?> listener, ApplicationEvent event) {
        while (iterator.hasNext()) {
            ApplicationListenerInterceptor interceptor = iterator.next();
            interceptor.intercept(listener, event, this);
            return;  // 只执行一个拦截器，由拦截器决定是否继续链
        }
        // 链末尾：调用实际的 Listener
        listenerAndEventConsumer.accept(listener, event);
    }
}
```

**关键设计**：`intercept` 方法内部用 `while + return` 而非 `for` 循环。这是因为链的继续由拦截器内部决定--拦截器调用 `chain.intercept(...)` 时，`intercept` 方法被递归调用，`iterator.next()` 推进到下一个拦截器。如果拦截器不调用 `chain.intercept(...)`，链终止。

**Iterator 的线程安全**：`Iterator` 不是线程安全的，但每个链实例只在一次拦截过程中使用（事件级：每次 `multicastEvent` 创建新链；监听器级：每次 `invokeListener` 创建新链），不存在跨线程共享。

---

### 两种实现方案：直接替换 vs 代理包装

microsphere 提供了两种拦截器集成方式，取决于用户是否已有自定义 `ApplicationEventMulticaster`：

#### 方案一：直接替换（InterceptingApplicationEventMulticaster）

当 Spring 容器中没有自定义 `ApplicationEventMulticaster` 时，`EventExtensionRegistrar` 直接注册 `InterceptingApplicationEventMulticaster` 替换默认的 `SimpleApplicationEventMulticaster`：

```java
public class InterceptingApplicationEventMulticaster extends SimpleApplicationEventMulticaster {

    @Override
    public final void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
        execute(() -> {
            ResolvableType type = resolveEventType(event, eventType);
            // 创建事件级拦截器链，链末尾调用 super.multicastEvent
            DefaultApplicationEventInterceptorChain chain =
                new DefaultApplicationEventInterceptorChain(
                    this.applicationEventInterceptors, this::doMulticastEvent);
            chain.intercept(event, type);
        });
    }

    @Override
    protected final void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
        // 创建监听器级拦截器链，链末尾调用 super.invokeListener
        DefaultApplicationListenerInterceptorChain chain =
            new DefaultApplicationListenerInterceptorChain(
                this.applicationListenerInterceptors, this::doInvokeListener);
        chain.intercept(listener, event);
    }

    protected void doMulticastEvent(ApplicationEvent event, ResolvableType eventType) {
        super.multicastEvent(event, eventType);  // 委托父类分发
    }

    protected void doInvokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
        super.invokeListener(listener, event);  // 委托父类调用
    }
}
```

这个方案通过继承 `SimpleApplicationEventMulticaster` 并覆盖 `multicastEvent` 和 `invokeListener`（两者都标记为 `final`，防止子类绕过拦截器），在方法内部创建拦截器链。链的末尾通过 `doMulticastEvent`/`doInvokeListener` 委托父类执行实际逻辑。

**Executor 的默认行为**：`InterceptingApplicationEventMulticaster` 覆盖了 `getTaskExecutor`，当未配置 Executor 时返回 `Runnable::run`（同步执行）。这确保拦截器链在调用线程中同步执行，而不是被 Spring 的默认行为（无 Executor 时同步执行）所干扰。`execute` 方法包裹整个拦截器链，使得异步执行时整个链（包括所有拦截器和最终的 Listener 调用）都在异步线程中执行。

#### 方案二：代理包装（InterceptingApplicationEventMulticasterProxy）

当用户已注册了自定义 `ApplicationEventMulticaster`（如配置了异步 Executor 或自定义 ErrorHandler），直接替换会丢失用户配置。`InterceptingApplicationEventMulticasterProxy` 通过**代理模式**保留原始 Multicaster：

```
原始 BeanDefinition：
  "applicationEventMulticaster" -> UserCustomMulticaster

代理后：
  "applicationEventMulticaster"          -> InterceptingApplicationEventMulticasterProxy
  "applicationEventMulticaster_ORIGINAL" -> UserCustomMulticaster（重命名保留）
```

Proxy 的工作方式：

1. **作为 `GenericBeanPostProcessorAdapter<ApplicationListener>`**：这是一个 microsphere 提供的泛型基类，通过 `GenericTypeResolver.resolveTypeArgument` 在构造时解析类型参数 `T`，只处理 `ApplicationListener` 类型的 Bean。`postProcessAfterInitialization` 中调用 `doPostProcessAfterInitialization`，后者用 `InterceptingApplicationListener` 包装原始 Listener
2. **作为 ApplicationEventMulticaster**：实现所有接口方法，委托给原始 Multicaster
3. **事件级拦截**：在 `multicastEvent` 中创建事件级拦截器链，链末尾调用 `delegate.multicastEvent`
4. **监听器级拦截**：由 `InterceptingApplicationListener` 在 `onApplicationEvent` 中创建监听器级拦截器链

```
Proxy.multicastEvent(event)
    └── 事件级拦截器链
            └── delegate.multicastEvent(event)          ← 委托原始 Multicaster
                    └── 遍历 Listener
                            └── InterceptingApplicationListener.onApplicationEvent(event)
                                    └── 监听器级拦截器链
                                            └── delegate.onApplicationEvent(event)  ← 委托原始 Listener
```

**为什么 Proxy 需要 BeanPostProcessor**：

方案一（直接替换）在 `invokeListener` 方法中创建监听器级链，因为 Multicaster 完全控制了 Listener 调用过程。方案二（Proxy）不能覆盖 `invokeListener`（因为 delegate 是用户的 Multicaster，其 `invokeListener` 行为可能被自定义），所以需要在 Listener 层面包装--将每个 Listener 替换为 `InterceptingApplicationListener`，后者在 `onApplicationEvent` 中创建监听器级链。

`InterceptingApplicationListener` 实现了 `GenericApplicationListenerAdapter`，支持 `supportsEventType(ResolvableType)` 方法，确保只处理匹配的事件类型。它还实现了 `DelegatingWrapper` 接口，`equals`/`hashCode` 基于原始 Listener（而非包装器），避免在 Set/Map 中出现重复。

---

### @EnableEventExtension 配置

```java
@EnableEventExtension(
    intercepted = true,                    // 是否启用拦截器（默认 true）
    executorForListener = "taskExecutor",  // 异步执行器 Bean 名称（默认 "N/E" = 不使用）
    sources = {BeanSource.BEAN_FACTORY, BeanSource.SPRING_FACTORIES, BeanSource.JAVA_SERVICE_PROVIDER}
)
@Configuration
public class MyConfig {
}
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `intercepted` | `true` | 是否用 InterceptingApplicationEventMulticaster 替换默认 Multicaster |
| `executorForListener` | `"N/E"` | Listener 异步执行器的 Bean 名称；`"N/E"` 表示不使用异步 |
| `sources` | `{BEAN_FACTORY, SPRING_FACTORIES, JAVA_SERVICE_PROVIDER}` | 拦截器的发现来源 |

**`sources` 的三路发现**：

`BeanSource` 枚举定义了三种拦截器发现方式：

- **`BEAN_FACTORY`**：从 Spring 容器中查找 `ApplicationEventInterceptor`/`ApplicationListenerInterceptor` 类型的 Bean
- **`SPRING_FACTORIES`**：从 `META-INF/spring.factories` 加载
- **`JAVA_SERVICE_PROVIDER`**：从 `META-INF/services/` 加载（Java SPI）

三路发现的结果合并后通过 `getSortedBeans` 按 `Ordered` 排序。

**`EventExtensionRegistrar` 的注册逻辑**：

1. 检查是否已有 `applicationEventMulticaster` BeanDefinition
2. 如果没有：创建 `InterceptingApplicationEventMulticaster`（方案一）
3. 如果有且 `intercepted=true`：将原始 BeanDefinition 重命名为 `applicationEventMulticaster_ORIGINAL`，注册 `InterceptingApplicationEventMulticasterProxy`（方案二）
4. 如果 `intercepted=false` 但 `executorForListener` 有值：修改原始 BeanDefinition，注入 Executor
5. 如果两者都无值：不注册任何东西（`@EnableEventExtension` 无效果）

第 3 步是代理方案的关键：**先移除原始 BeanDefinition，重命名后重新注册，再注册 Proxy**。这样 Proxy 在初始化时可以通过 `beanFactory.getBean(delegateBeanName)` 获取原始 Multicaster 作为委托。

**`intercepted=false` 时的行为**：

当 `intercepted=false` 但 `executorForListener` 指定了 Executor Bean 时，`EventExtensionRegistrar` **不创建**拦截器 Multicaster，而是修改原始 BeanDefinition，通过 `MutablePropertyValues` 注入 `RuntimeBeanReference`（即 `propertyValues.addPropertyValue("taskExecutor", new RuntimeBeanReference(executorBeanName))`）。这要求 Multicaster 类必须有 `setTaskExecutor(Executor)` 方法（`SimpleApplicationEventMulticaster` 满足）。当 `intercepted=false` 且 `executorForListener="N/E"` 时，`@EnableEventExtension` 完全无效果。

**内置拦截器**：

microsphere 提供了两个示例拦截器实现（注意：只在测试的 `spring.factories` 中注册，生产环境需要用户手动注册为 Bean 或添加到 `spring.factories`）：

- `LoggingApplicationEventInterceptor`：在事件分发前后打印日志
- `LoggingApplicationListenerInterceptor`：在 Listener 调用前后打印日志

它们默认 `LOWEST_PRECEDENCE`，可以通过 `@Order` 覆盖优先级。

---

### 一个具体示例

```java
// 定义事件级拦截器：审计日志
@Component
@Order(1)
public class AuditLogEventInterceptor implements ApplicationEventInterceptor {
    @Override
    public void intercept(ApplicationEvent event, ResolvableType eventType,
                          ApplicationEventInterceptorChain chain) {
        log.info("Event published: {} at {}", event.getClass().getSimpleName(), Instant.now());
        chain.intercept(event, eventType);  // 继续链
        log.info("Event dispatched: {}", event.getClass().getSimpleName());
    }
}

// 定义监听器级拦截器：性能监控
@Component
@Order(1)
public class PerformanceListenerInterceptor implements ApplicationListenerInterceptor {
    @Override
    public void intercept(ApplicationListener<?> listener, ApplicationEvent event,
                          ApplicationListenerInterceptorChain chain) {
        long start = System.nanoTime();
        try {
            chain.intercept(listener, event);  // 继续链，最终调用 listener
        } finally {
            long elapsed = System.nanoTime() - start;
            log.info("Listener {} handled {} in {}ms",
                listener.getClass().getSimpleName(),
                event.getClass().getSimpleName(),
                elapsed / 1_000_000);
        }
    }
}

// 定义监听器级拦截器：条件过滤（短路）
@Component
@Order(2)
public class ConditionalListenerInterceptor implements ApplicationListenerInterceptor {
    @Override
    public void intercept(ApplicationListener<?> listener, ApplicationEvent event,
                          ApplicationListenerInterceptorChain chain) {
        if (shouldSkip(listener, event)) {
            return;  // 短路：不调用 chain.intercept，事件不会到达该 Listener
        }
        chain.intercept(listener, event);
    }
}
```

执行流程：

```
publishEvent(UserCreatedEvent)
    │
    ├── AuditLogEventInterceptor.intercept(event, type, chain)
    │       log: "Event published: UserCreatedEvent"
    │       chain.intercept -> 继续
    │
    ├── multicastEvent -> 遍历匹配 Listener
    │       │
    │       ├── Listener: EmailNotificationListener
    │       │       ├── PerformanceListenerInterceptor.intercept(listener, event, chain)
    │       │       │       start = now()
    │       │       │       chain.intercept -> 继续
    │       │       │       ├── ConditionalListenerInterceptor.intercept(listener, event, chain)
    │       │       │       │       shouldSkip = false
    │       │       │       │       chain.intercept -> 继续
    │       │       │       │       └── listener.onApplicationEvent(event)  ← 实际处理
    │       │       │       log: "EmailNotificationListener handled UserCreatedEvent in 15ms"
    │       │
    │       ├── Listener: AuditLogListener
    │               ├── PerformanceListenerInterceptor.intercept(listener, event, chain)
    │               │       start = now()
    │               │       chain.intercept -> 继续
    │               │       ├── ConditionalListenerInterceptor.intercept(listener, event, chain)
    │               │       │       shouldSkip = true
    │               │       │       return  ← 短路！不调用 chain.intercept
    │               │       log: "AuditLogListener handled UserCreatedEvent in 0ms"
    │
    log: "Event dispatched: UserCreatedEvent"
```

注意 `ConditionalListenerInterceptor` 短路时，`PerformanceListenerInterceptor` 的 `finally` 块仍然执行（因为 `chain.intercept` 正常返回，只是链内部没有调用 listener）。这意味着性能监控数据仍然记录，只是 elapsed time 接近 0。

---

## 永恒原理

### 1. 责任链模式的 Iterator 实现

microsphere 的拦截器链使用 `Iterator` 而非传统的"拦截器列表 + 索引"方式实现责任链。两种方式的区别：

**索引方式**（如 Servlet FilterChain）：
```java
class FilterChain {
    private List<Filter> filters;
    private int index = 0;

    void doFilter(Request req, Response res) {
        if (index < filters.size()) {
            filters.get(index++).doFilter(req, res, this);
        } else {
            target.handle(req, res);
        }
    }
}
```

**Iterator 方式**（microsphere）：
```java
class Chain {
    private Iterator<Interceptor> iterator;

    void intercept(...) {
        if (iterator.hasNext()) {
            iterator.next().intercept(..., this);
            return;  // 关键：return 后由拦截器决定是否递归
        }
        target.accept(...);
    }
}
```

Iterator 方式的优势是**状态封装**：`Iterator` 内部维护当前位置，链对象本身不需要额外的 `index` 字段。每次 `intercept` 调用只执行一个拦截器，链的继续由拦截器调用 `chain.intercept(...)` 触发递归。

两种方式共同的风险是**递归深度**：如果有 N 个拦截器，递归深度为 N。在实际应用中 N 通常 < 10，不会栈溢出。

### 2. 装饰器 vs 代理的抉择

microsphere 提供两种方案，对应两种经典设计模式：

| 维度 | 装饰器（InterceptingApplicationEventMulticaster） | 代理（InterceptingApplicationEventMulticasterProxy） |
|------|------------------------------------------------|---------------------------------------------------|
| 关系 | 继承 `SimpleApplicationEventMulticaster` | 持有 `ApplicationEventMulticaster` 引用 |
| 控制 | 完全控制 `multicastEvent` 和 `invokeListener` | 只控制 `multicastEvent`，`invokeListener` 由 delegate 控制 |
| 前提 | 容器中无自定义 Multicaster | 容器中有自定义 Multicaster |
| 监听器拦截 | 在 `invokeListener` 中创建链 | 通过 `BeanPostProcessor` 包装 Listener |
| 复杂度 | 低 | 高 |

装饰器模式通过继承覆盖方法，代理模式通过组合委托方法。装饰器更简洁但耦合度高（依赖父类实现），代理更灵活但代码量大（需要实现所有接口方法）。

microsphere 的选择逻辑：**优先装饰器（简洁），当无法装饰时退化为代理（兼容）**。这是一个务实的设计决策。

### 3. 两级拦截的分层关注

事件级和监听器级拦截器的分离，体现了"关注点分离"原则。如果只有一级拦截器（在 Listener 调用前），事件级的逻辑（如审计日志、Trace 注入）需要在每个拦截器中重复执行 N 次（N = 匹配的 Listener 数量）。两级分离后，事件级逻辑只执行 1 次。

这种分层在 Web 框架中也有体现：Servlet Filter（请求级）和 Spring Interceptor（Handler 级），前者拦截所有请求，后者只拦截特定 Handler。

---

## 边界与反例

### 1. 异步执行的双层传播

`InterceptingApplicationEventMulticaster` 的 `multicastEvent` 用 `execute(() -> ...)` 包裹事件级拦截器链。同时，Spring 的 `SimpleApplicationEventMulticaster#multicastEvent` 内部也会用 `getTaskExecutor()` 为每个 Listener 调用 `invokeListener`。由于 `InterceptingApplicationEventMulticaster` 覆盖了 `getTaskExecutor()` 使其永不返回 null（默认 `Runnable::run`），两层执行使用的是**同一个 Executor**。

这意味着异步行为取决于 Executor 类型：

**同步（`Runnable::run`，默认）**：
```
Thread-A: multicastEvent
    └── event-level chain (同步)
            └── super.multicastEvent (同步)
                    ├── invokeListener -> listener-level chain -> listener.onApplicationEvent
                    ├── invokeListener -> listener-level chain -> listener.onApplicationEvent
                    └── ...
            └── event-level "after" logic  ← 所有 Listener 执行完后
```

**异步（ThreadPoolExecutor）**：
```
Thread-A: multicastEvent
    └── pool.execute(() -> event-level chain)
            └── super.multicastEvent
                    ├── pool.execute(() -> invokeListener -> listener-level chain)  ← 非阻塞提交
                    ├── pool.execute(() -> invokeListener -> listener-level chain)  ← 非阻塞提交
                    └── ...                           ← 提交完，立即返回
            └── event-level "after" logic  ← Listener 可能还在执行！
                    
Thread-B: listener-level chain -> listener.onApplicationEvent  ← 异步执行
Thread-C: listener-level chain -> listener.onApplicationEvent  ← 异步执行
```

**关键差异**：使用线程池时，事件级拦截器的"after"逻辑（如 `finally` 块中的日志）在所有 Listener 任务**提交**后执行，但 Listener 可能**尚未完成**。如果需要等待所有 Listener 完成，应该在事件级拦截器中使用 `CountDownLatch` 或 `CompletableFuture.allOf`。

`InterceptingApplicationEventMulticasterProxy` 的行为不同：Proxy 的 `multicastEvent` 用 `execute` 包裹事件级链，但链末尾调用 `delegate.multicastEvent`，Listener 的调度由 delegate（原始 Multicaster）的 Executor 决定。监听器级链在 `InterceptingApplicationListener.onApplicationEvent` 中创建，运行在 delegate 调度的线程中。

### 2. 短路不影响上游拦截器的后置逻辑

当监听器级拦截器 B 短路（不调用 `chain.intercept`），上游拦截器 A 的 `chain.intercept` 调用正常返回，A 的后置逻辑（如 `finally` 块）仍然执行：

```java
// Interceptor A (Order=1)
void intercept(listener, event, chain) {
    long start = now();
    try {
        chain.intercept(listener, event);  // 内部 B 短路，但这里正常返回
    } finally {
        log.info("elapsed: {}", now() - start);  // 仍然执行
    }
}

// Interceptor B (Order=2) - 短路
void intercept(listener, event, chain) {
    if (shouldSkip) return;  // 不调用 chain.intercept
    // ...
}
```

A 的 `finally` 块执行是因为 `chain.intercept` 是同步方法调用，B 的 `return` 只是让 B 的 `intercept` 方法返回，不影响 A 的调用栈。

**但事件级短路不同**：如果事件级拦截器短路（不调用 `chain.intercept`），事件不会被分发，所有 Listener 都不会收到事件。上游事件级拦截器的后置逻辑仍然执行。

### 3. 拦截器发现的重复风险

`BeanSource` 三路发现可能导致同一个拦截器被注册多次：
- 一个 `@Component` 拦截器同时出现在 BeanFactory 和 `spring.factories` 中
- `getSortedBeans` 不去重

**后果**：同一拦截器执行两次，可能导致日志重复、计数翻倍。

**缓解**：拦截器应只通过一种方式注册。框架级拦截器用 `spring.factories`，应用级拦截器用 `@Component`。

### 4. Proxy 方案的 Listener 包装时序

`InterceptingApplicationEventMulticasterProxy` 作为 `BeanPostProcessor` 在 `postProcessAfterInitialization` 中包装 Listener。但 Proxy 本身是 `applicationEventMulticaster` Bean，它在 `setBeanFactory` 阶段获取 delegate（原始 Multicaster）。如果原始 Multicaster 在 Proxy 之前初始化，原始 Multicaster 可能已经注册了未包装的 Listener。

**缓解**：`EventExtensionRegistrar` 在 `BeanDefinitionRegistryPostProcessor` 阶段注册 Proxy 的 BeanDefinition，确保 Proxy 在 Bean 实例化前就替换了原始 BeanDefinition。原始 Multicaster 被重命名为 `_ORIGINAL`，在 Proxy 的 `setBeanFactory` 中通过 `getBean(delegateBeanName)` 延迟初始化。

### 5. `InterceptingApplicationListener` 的 equals/hashCode

`InterceptingApplicationListener` 的 `equals`/`hashCode` 基于原始 Listener（通过 `getDelegate()` 递归解包），而非包装器自身。这是为了确保在 `ApplicationEventMulticaster` 内部的 Listener 集合中，包装器和原始 Listener 不会同时存在。如果用户调用 `removeApplicationListener(originalListener)`，Proxy 能通过 `equals` 找到对应的包装器并移除。

---

## 现代 Spring（6.x）是否已支持？

**Spring 6.x 仍然不支持事件拦截器链。** `SimpleApplicationEventMulticaster` 仍然是最终实现，没有拦截器接口。

Spring 4.2 引入了 `@EventListener` 注解，允许将任意方法标记为事件监听器。但 `@EventListener` 方法不支持拦截--你只能通过 AOP 切面拦截 `@EventListener` 方法，但这绕过了 `ApplicationEventMulticaster`，无法控制事件分发本身。

Spring 6.0 的 `ApplicationEventPublisher` 增加了泛型支持（`publishEvent(T event)`），但分发机制没有变化。

如果需要"事件级拦截"的替代方案，Spring 生态中有：
- **Spring Integration**：通过 Message Channel + Interceptor 实现，但引入了完整的 EIP 模型，重量级
- **Axon Framework**：事件驱动架构框架，支持拦截器，但绑定了 CQRS/Event Sourcing 模型

microsphere 的方案是**轻量级**的：不改变 Spring 事件 API（`ApplicationEvent`/`ApplicationListener`），只替换 Multicaster 实现，对应用透明。

---

## 小结

microsphere-spring 的事件拦截器链，通过两级拦截（事件级 + 监听器级）和两种集成方式（装饰器 + 代理），在不改变 Spring 事件 API 的前提下，为事件分发过程增加了可拦截性。

事件级拦截器关注"事件是否应该被分发"，监听器级拦截器关注"该 Listener 是否应该处理"。两级分离避免了事件级逻辑的重复执行。装饰器方案简洁但要求替换 Multicaster，代理方案兼容用户自定义 Multicaster 但需要包装所有 Listener。

这套体系的核心价值是**将事件分发从黑盒变为白盒**。原本不可观察、不可控制的 `multicastEvent -> invokeListener -> onApplicationEvent` 链路，现在可以通过拦截器插入审计、监控、过滤、Trace 等横切逻辑，且不侵入应用代码。
