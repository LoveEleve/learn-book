# 事件总线 -- 不依赖容器的观察者模式，以及同步/异步分发的取舍

> 主题：观察者模式从回调到事件总线的演化，为什么 Spring `ApplicationEvent` 在无容器场景不可用，同步 vs 异步事件分发的取舍，读写分离的线程安全设计。microsphere 的 `EventDispatcher` 是无容器事件总线的一个实例。
> 关联：泛型自识别与 [§8 序列化](./08-serialization-spi.md) §3、[§3 转换](./03-convert-framework.md) §4 同构；事件循环/处理分离的 Reactor 模式在 [§9 文件监听](./09-file-watch-service.md) §3 深入讨论。

---

## 一、事件驱动:从回调和事件总线的演化

### 1.1 观察者模式的三代演化

事件驱动是个经典设计模式，它的演化经历了三代：

**第一代:回调函数（Callback）**

```java
button.setOnClickListener(new OnClickListener() {
    public void onClick(View v) { /* 处理点击 */ }
});
```

一个事件源（button）持有一个回调（OnClickListener）。**特点**：一对一、强类型、直接调用。**局限**：一个事件源只能通知一个回调；要通知多个观察者得自己维护列表。

**第二代:监听器列表（Listener List）**

```java
button.addListener(listener1);
button.addListener(listener2);
// 事件来时:for (listener : listeners) listener.onEvent(event)
```

事件源维护一个监听器列表，事件来时遍历通知。**特点**：一对多、解耦（事件源不认识具体监听器）。**局限**：每个事件源自己维护列表，跨事件源没有统一管理。

**第三代:事件总线（Event Bus）**

```java
eventDispatcher.addEventListener(listener);
eventDispatcher.dispatch(event);  // 总线负责分发给所有注册的监听器
```

一个中心化的「总线」管理所有监听器和事件分发。事件源只管 `dispatch(event)`，不关心谁在监听。**特点**：完全解耦（事件源和监听器互不认识）、多对多（多个事件源 -> 多个监听器）。**这是观察者模式的终极形态**--Guava `EventBus`、Spring `ApplicationEvent`、microsphere `EventDispatcher` 都是这一代。

### 1.2 Spring `ApplicationEvent` 的容器依赖

Spring `ApplicationEvent` 是 Java 生态最常用的事件总线，但它绑定在容器上：

```java
@Component
public class MyListener {
    @EventListener
    public void handle(MyEvent event) { ... }
}
// 监听器注册:Spring 容器扫描 @Component -> 发现 @EventListener -> 自动注册
// 事件发布:applicationContext.publishEvent(new MyEvent())
// 都在容器管理下--没有容器就不能用
```

**和 §3 类型转换、§10 日志一样的困境**：容器没启动就用不了。`main()` 第一行、命令行工具、嵌入式 SDK、框架启动早期事件（`SpringApplicationRunListener.starting()` 阶段）--这些场景没有 `ApplicationContext`，`publishEvent` 不可用。

microsphere 的 `EventDispatcher` 解决的就是这个「**无容器场景下的事件驱动**」--通过 SPI + 手动注册工作，不依赖任何容器。

### 1.3 无容器事件总线的需求场景

- **框架启动早期事件**：`SpringApplicationRunListener.starting()` 阶段，ApplicationContext 还没创建，但框架需要发布「启动中」事件通知监听器。
- **框架内部解耦**：microsphere 的子模块（如 [§9 文件监听](./09-file-watch-service.md)）需要事件分发，但不能依赖 Spring。
- **命令行工具**：`main()` 启动，没有容器，但需要事件驱动的工作流。
- **嵌入式 SDK**：给第三方用的 jar 包，不能要求对方启动 Spring。

这些场景和 [§3 类型转换](./03-convert-framework.md) §3 的「无容器困境」清单一致--microsphere 的整体定位就是「无容器基础设施」。


## 二、永恒原理一:同步 vs 异步事件分发的取舍

事件总线有个根本设计选择：**事件分发是同步还是异步？** 这决定了监听器的执行方式和适用场景。

### 2.1 两种分发模式

**同步模式（DirectEventDispatcher）**：

```java
public final class DirectEventDispatcher extends AbstractEventDispatcher {
    public DirectEventDispatcher() {
        super(DIRECT_EXECUTOR);  // DIRECT_EXECUTOR = Runnable::run(调用方线程直接执行)
    }
}
```

`dispatch(event)` 时，**调用方线程同步执行所有监听器**。事件分发是阻塞的--所有监听器执行完才返回。

**异步模式（ParallelEventDispatcher）**：

```java
EventDispatcher parallel = EventDispatcher.parallel(Executors.newFixedThreadPool(4));
// dispatch(event) 时,executor.execute(() -> {...}) 在独立线程执行监听器链
```

`dispatch(event)` 时，监听器在**独立线程池中执行**。调用方线程不阻塞--事件分发立即返回，监听器异步执行。

### 2.2 取舍:什么时候用哪个

| 维度 | 同步（Direct） | 异步（Parallel） |
|---|---|---|
| 执行顺序 | 可预测（调用方线程顺序执行） | 不确定（线程池调度） |
| 异常传播 | 监听器异常直接抛给调用方 | 监听器异常被线程池吞（需自己处理） |
| 阻塞 | 调用方阻塞（等所有监听器完成） | 调用方不阻塞 |
| 适用场景 | 框架启动阶段（需顺序保证）、单元测试 | 运行时事件（不能阻塞主线程） |

**关键区别：异常传播**：

- **同步模式**：一个监听器抛异常，异常直接传播给 `dispatch(event)` 的调用方。调用方可以 try-catch 处理。**但一个监听器异常会中止整个分发链**--后面的监听器不会被通知。
- **异步模式**：监听器在线程池中执行，异常不会传播给调用方--但也不会被其他监听器看到。**异常被线程池的 `UncaughtExceptionHandler` 处理**（默认打印到 stderr）。一个监听器异常不影响其他监听器（它们在独立线程执行）。

### 2.3 同步模式的"异常中止"问题

同步模式有个设计缺陷：`dispatch()` 的监听器遍历没有 try-catch：

```java
// AbstractEventDispatcher.dispatch 的核心逻辑(简化)
sortedListeners(entry -> entry.getKey().isAssignableFrom(event.getClass()))
    .forEach(listener -> {
        if (listener instanceof ConditionalEventListener) {
            if (!((ConditionalEventListener) listener).accept(event)) return;
        }
        listener.onEvent(event);  // ← 没有 try-catch! 抛异常会中止 forEach
    });
```

**一个监听器抛异常 -> `forEach` 中止 -> 后面的监听器不被通知**。Spring 的 `SimpleApplicationEventMulticaster` 有同样行为（除非配置了 `ErrorHandler`）。microsphere 没有提供 `ErrorHandler` 机制。

**这是「快速失败 vs 容错」的取舍**：同步模式选快速失败（一个出错全部停止），简单但脆弱。如果需要容错（一个监听器出错不影响其他），应该在监听器内部自己 try-catch，或用异步模式。

### 2.4 异步模式的"线程安全"与 Reactor 模式

异步模式把监听器执行放到独立线程池--这和 [§9 文件监听](./09-file-watch-service.md) §3 的「事件循环/处理分离」Reactor 模式同构：

```
[事件源] -> dispatch(event) -> [线程池] -> 并发执行监听器
```

**但 microsphere 的 EventDispatcher 比 FileWatchService 更简单**--它没有「事件循环线程」（不需要从 OS 取事件），`dispatch` 直接把监听器执行提交到线程池。这是「无事件源」的事件总线--事件由调用方显式 `dispatch`，不需要监听事件源。

**异步模式的线程安全**：监听器在多线程并发执行，必须保证监听器内部状态线程安全。microsphere 的 `EventDispatcher` 本身线程安全（注册用锁、分发用快照读），但**监听器的线程安全由监听器自己负责**--框架不提供同步保障。


## 三、永恒原理二:读写分离的线程安全

事件总线有个经典的并发问题：**监听器注册/注销（写）和事件分发（读）可能并发**。怎么保证线程安全又不影响性能？

### 3.1 读写分离模式

microsphere 用「读写分离」模式：

```java
// 注册/注销(写):加锁
void doInListener(Class<? extends Event> eventType, Consumer<Collection<EventListener>> consumer) {
    synchronized (mutex) {  // ← 关键:注册/注销有锁保护
        List<EventListener> listeners = listenersCache.computeIfAbsent(eventType, e -> newLinkedList());
        consumer.accept(listeners);  // add/remove
        sort(listeners);              // 操作后重新排序
    }
}

// 分发(读):无锁
public void dispatch(Event event) {
    executor.execute(() -> {
        sortedListeners(entry -> entry.getKey().isAssignableFrom(event.getClass()))
            .forEach(listener -> listener.onEvent(event));  // 无锁读
    });
}
```

**写加锁、读不加锁**--这是 `CopyOnWriteArrayList` 模式的变体。`listenersCache` 是 `ConcurrentHashMap`（保证读不阻塞），写操作在 `synchronized(mutex)` 内（保证修改和排序原子）。

### 3.2 为什么不用 `CopyOnWriteArrayList`

`CopyOnWriteArrayList` 是 Java 并发包提供的「读写分离」列表--每次写都复制整个数组，读不加锁。为什么不直接用它？

**原因一:排序的原子性**。microsphere 注册监听器后要 `sort(listeners)`（按 `Prioritized` 排序）。`CopyOnWriteArrayList` 的 `add` 是原子的，但 `add + sort` 不是--如果线程 A 在 add 后 sort 前，线程 B 读到了未排序的列表。`synchronized(mutex)` 保证 `add + sort` 原子。

**原因二:写频率**。`CopyOnWriteArrayList` 适合「读多写少」--每次写都复制数组，写多了开销大。事件总线的监听器注册通常在启动时集中完成（写多），之后运行时只分发（读多）。但 microsphere 选了 `LinkedList` + `synchronized` 而非 `CopyOnWriteArrayList`，可能是为了「排序的原子性」而非性能。

**原因三:`ConcurrentHashMap` 的快照读**。`listenersCache` 是 `ConcurrentHashMap<Class, List<EventListener>>`。`computeIfAbsent` 和 `get` 都是线程安全的。分发时 `listenersCache.get(eventType)` 拿到 `List` 引用后遍历--如果此时有写操作（在 `synchronized` 内修改 `List`），遍历可能看到不一致状态。但 `LinkedList` 的遍历是弱一致的（不会抛 `ConcurrentModificationException`，但可能看到部分修改）--**这是「弱一致性」的取舍**，microsphere 接受它以换取读不加锁的性能。

### 3.3 读写分离的普适性

「读写分离」是并发设计的经典模式，不止事件总线：

| 场景 | 写 | 读 | 机制 |
|---|---|---|---|
| microsphere EventDispatcher | 注册监听器（`synchronized`） | 分发事件（无锁） | `ConcurrentHashMap` + `synchronized` |
| `CopyOnWriteArrayList` | 写时复制数组 | 读原数组 | 写复制、读无锁 |
| 数据库 MVCC | 写新版本 | 读旧版本快照 | 多版本并发控制 |
| Reactor `Flux` | 发射元素 | 订阅消费 | 背压 + 不可变序列 |

**共同本质**：读和写访问不同的数据版本，避免读写互锁。代价是「弱一致性」（读可能看到稍旧的数据）。在事件总线场景，监听器注册后短暂延迟才被分发看到--可接受。


## 四、永恒原理三:泛型自识别与「条件监听器」

### 4.1 泛型自识别:监听器怎么知道监听什么事件

```java
public interface EventListener<E extends Event> extends Prioritized {
    void onEvent(E event);

    static Class<? extends Event> findEventType(EventListener<?> listener) {
        return TypeUtils.resolveActualTypeArgumentClass(
            listener.getClass(), EventListener.class, 0
        );
    }
}
```

**这套机制和 [§3 转换](./03-convert-framework.md) §4.2、[§8 序列化](./08-serialization-spi.md) §3 完全同构**--通过 `Class.getGenericInterfaces()` 读取字节码 `Signature` 属性，恢复 `E` 的具体类型。`MyEventListener implements EventListener<MyEvent>` -> `findEventType` 返回 `MyEvent.class`。

注册监听器时，框架用 `findEventType` 自动识别它监听什么事件类型，按类型分桶存进 `listenersCache`。分发时 `entry.getKey().isAssignableFrom(event.getClass())` 做类型过滤--只通知声明了监听该事件类型（或父类型）的监听器。**不需要 `instanceof` 检查，由框架保证**。

### 4.2 类型过滤 + 条件过滤:两层分发

microsphere 的事件分发有两层过滤：

**第一层:类型过滤**

```java
entry.getKey().isAssignableFrom(event.getClass())
// 只通知那些声明了监听 event 类型(或父类型)的 listener
```

`MyEventListener implements EventListener<MyEvent>` 只接收 `MyEvent` 及其子类的事件。`GenericEventListener implements EventListener<Event>` 接收所有事件（`Event` 是基类）。

**第二层:条件过滤**

```java
if (listener instanceof ConditionalEventListener) {
    ConditionalEventListener cond = (ConditionalEventListener) listener;
    if (!cond.accept(event)) return;  // 跳过
}
```

`ConditionalEventListener` 接口允许监听器在 `accept(event)` 里做更细粒度的过滤--「我虽然监听 `MyEvent`，但只处理满足某条件的 `MyEvent`」。类似 Spring 的 `@EventListener(condition = "...")`。

**两层过滤的意义**：类型过滤是粗粒度（按类型分桶，避免遍历所有监听器），条件过滤是细粒度（在同一桶内按业务条件筛选）。这是「**粗粒度索引 + 细粒度过滤**」的经典模式--数据库的「索引 + WHERE」、Servlet 的「URL pattern + Filter 链」都是同构。


## 五、microsphere 作为「无容器事件总线」的一个实例

讲完三个原理，microsphere 的 `EventDispatcher` 就是这些原理的一次落地。

**实例一:同步/异步双模式（§2 原理的落地）**

`DirectEventDispatcher` 用 `DIRECT_EXECUTOR`（`Runnable::run`，同步），`ParallelEventDispatcher` 用用户传入的线程池（异步）。`EventDispatcher.parallel(executor)` 工厂方法创建并行实例。[§9 文件监听](./09-file-watch-service.md) 的 `StandardFileWatchService` 用 `EventDispatcher.parallel(handlerExecutor)` 做文件事件的异步分发。

**实例二:读写分离（§3 原理的落地）**

`listenersCache` 是 `ConcurrentHashMap<Class, List<EventListener>>`。`doInListener` 用 `synchronized(mutex)` 保护注册/注销 + 排序。`dispatch` 用 `sortedListeners` 快照读遍历，无锁。

**实例三:泛型自识别 + 两层过滤（§4 原理的落地）**

`findEventType` 从泛型签名恢复事件类型，按类型分桶。分发时 `isAssignableFrom` 做类型过滤，`ConditionalEventListener.accept` 做条件过滤。`EventListener extends Prioritized`，监听器按优先级排序通知（[§4](./04-spi-prioritized.md)）。

**事件类型体系**：`Event`（基类）-> `GenericEvent<T>`（带载荷的泛型事件）-> `FileChangedEvent`（[§9](./09-file-watch-service.md) 文件变化事件）等。`GenericEvent<T>` 用泛型携带事件载荷，和 `EventListener<E>` 的泛型自识别配合。


## 六、实例批判:这个实现的缺陷

1. **监听器抛异常会中止整个分发链**（§2.3）：同步模式下一个监听器异常，`forEach` 中止，后面的监听器不被通知。没有 `ErrorHandler` 机制。应提供可选的 `ErrorHandler` 让调用方决定「中止还是继续」。
2. **异步模式异常被吞**：监听器在线程池中执行，异常不传播给调用方，被线程池 `UncaughtExceptionHandler` 处理（默认打印 stderr）。应提供异步异常回调。
3. **没有事件总线命名空间**：`EventDispatcher` 是全局的，所有事件类型共享一个总线。复杂场景下可能需要多个隔离的总线（如「业务事件总线」和「系统事件总线」分离）。microsphere 不提供命名空间，调用方自己 new 多个 `EventDispatcher` 实例。
4. **弱一致性**（§3.2）：`LinkedList` 遍历是弱一致的，分发时可能看到注册操作的中间状态。可接受但不严格。
5. **`sortedListeners` 每次调用都排序**：分发时 `sortedListeners` 可能每次都排序（取决于实现），高频分发场景有开销。应缓存排序结果（注册时排好，分发时直接用）。

这些不是原理错误，而是原理在具体代码里的实现瑕疵。


## 七、与其他方案的原理对比

| 方案 | 容器依赖 | 同步/异步 | 条件过滤 | 线程安全 | 适合场景 |
|---|---|---|---|---|---|
| microsphere EventDispatcher | 无 | 两者都支持 | `ConditionalEventListener.accept` | 读写分离 | 无容器场景 |
| Spring ApplicationEvent | 需要 | 同步（`@Async` 异步） | `@EventListener(condition="...")` | 容器管理 | Spring 应用 |
| Guava EventBus | 无 | 同步（`AsyncEventBus` 异步） | `@Subscribe` + 方法参数类型 | 容器管理 | 无容器场景 |
| JDK Flow API（Reactive） | 无 | 异步（背压） | `Filter` 操作符 | 背压 + 不可变 | 响应式流 |
| Disruptor | 无 | 异步（ring buffer） | `EventHandler` | 无锁（CAS） | 极致性能 |

**原理层面的取舍**：

- **Spring ApplicationEvent** 假设「你在容器内」：`@EventListener` 由容器扫描注册，`publishEvent` 由容器分发。条件过滤用 SpEL 表达式（`condition = "#event.source == 'foo'"`），强大但需要 SpEL。
- **Guava EventBus** 也是无容器方案，用 `@Subscribe` 注解 + 方法参数类型做事件匹配（反射）。比 microsphere 的泛型自识别更动态（方法可以是任意签名），但反射开销更大。
- **JDK Flow API**（Reactive Streams）走另一条路--异步 + 背压 + 操作符链。不是简单的「事件总线」而是「响应式流」，更强大但更复杂。
- **Disruptor** 追求极致性能--无锁 ring buffer + 缓存行填充。适合高频事件（每秒百万级），但对简单场景过度设计。

microsphere 的定位：**无容器场景下的简单事件总线，同步/异步双模式，读写分离线程安全**。它不替代 Spring ApplicationEvent（Spring 在容器内更好用）、不替代 Reactive Streams（响应式流更强大）、不替代 Disruptor（极致性能场景）。它填补的是「无容器 + 简单事件驱动」这个空白。


## 八、面试要点

**Q1：「不用 Spring，怎么在纯 Java 中实现事件驱动的观察者模式？」**

答案：观察者模式三代演化--回调（一对一）-> 监听器列表（一对多）-> 事件总线（多对多解耦）。microsphere 的 `EventDispatcher` 是事件总线形态：`EventListener<E extends Event>` 接口 + 泛型自识别事件类型 + 注册/分发。同步模式（`DirectEventDispatcher`）在调用方线程执行，适合启动阶段；异步模式（`EventDispatcher.parallel(executor)`）在独立线程池执行，适合运行时。和 Spring `ApplicationEvent` 的区别是不需要容器--`main()` 第一行就能用。

**Q2：「事件分发同步和异步有什么区别？什么时候用哪个？」**

答案：同步（Direct）在调用方线程顺序执行所有监听器，异常直接传播（一个监听器异常中止整个链），适合需要顺序保证的场景（框架启动阶段、单元测试）。异步（Parallel）在独立线程池并发执行监听器，调用方不阻塞，异常被线程池吞（一个监听器异常不影响其他），适合不能阻塞主线程的运行时事件。关键区别是异常传播--同步模式异常可见但脆弱（一个崩全停），异步模式异常隔离但不可见（需自己处理）。microsphere 同步模式没有 ErrorHandler，需要容错应该监听器内部 try-catch 或用异步模式。

**Q3：「事件总线的注册和分发改怎么做线程安全？为什么不用 CopyOnWriteArrayList？」**

答案：读写分离模式。注册/注销（写）用 `synchronized(mutex)` 保护修改 + 排序的原子性；分发（读）用 `ConcurrentHashMap` 快照读，无锁。不用 `CopyOnWriteArrayList` 三个原因：① 注册后要 `sort`，`CopyOnWriteArrayList` 的 `add` 原子但 `add + sort` 不原子，`synchronized` 保证原子。② `CopyOnWriteArrayList` 写时复制整个数组，写多了开销大。③ `ConcurrentHashMap` + `LinkedList` 的弱一致性读可接受（监听器注册后短暂延迟才被分发看到）。读写分离是并发设计经典模式--`CopyOnWriteArrayList`、数据库 MVCC、Reactor `Flux` 都是同构。

**Q4：「监听器怎么知道自己监听什么事件类型？泛型不是被擦除了吗？」**

答案：泛型自识别。和 [§3 转换](./03-convert-framework.md) §4.2、[§8 序列化](./08-serialization-spi.md) §3 完全同构--`EventListener<E extends Event>` 的 `E` 在运行时被擦除，但通过 `Class.getGenericInterfaces()` 读取字节码 `Signature` 属性可恢复。`MyEventListener implements EventListener<MyEvent>` -> `findEventType` 返回 `MyEvent.class`。注册时按类型分桶存进 `listenersCache`，分发时 `isAssignableFrom` 做类型过滤--只通知声明了监听该事件类型的监听器，不需要 `instanceof` 检查。这是 microsphere 「极简接口 + 富注册表」风格的统一体现--Converter、Serializer、EventListener 三个接口都用同一套泛型自识别机制。

**Q5：「microsphere EventDispatcher 和 Guava EventBus 有什么区别？」**

答案：两者都是无容器事件总线，但事件匹配机制不同。microsphere 用泛型自识别--`EventListener<MyEvent>` 的类型签名在编译期固定，运行时反射读出，类型安全。Guava 用 `@Subscribe` 注解 + 方法参数类型--方法可以是任意签名（`void handle(MyEvent event, long timestamp)`），反射匹配参数类型，更灵活但反射开销更大。microsphere 的条件过滤用 `ConditionalEventListener.accept(event)`（Java 代码），Guava 没有内建条件过滤（得自己在 `@Subscribe` 方法里 if-else）。microsphere 更类型安全、反射开销小；Guava 更灵活、API 更简洁。

**Q6：「如果让你改进 microsphere EventDispatcher，你会改什么？」**

答案：四个方向。① 提供可选的 `ErrorHandler`--同步模式下一个监听器异常中止整个链太脆弱，应该让调用方决定「中止还是继续」。② 异步模式异常回调--监听器在线程池中异常被吞，应该提供异步异常回调让调用方可见。③ 缓存排序结果--`sortedListeners` 每次调用可能都排序，高频分发有开销，应注册时排好、分发时直接用。④ 提供事件总线命名空间--复杂场景需要多个隔离总线，应该支持命名的事件总线实例（类似 Spring 的 `ApplicationEventPublisher` per context）。

---

> **与 §3/§8 的关联**：`EventListener<E>` 的泛型自识别与 `Converter<S,T>`（[§3](./03-convert-framework.md)）、`Serializer<S>`（[§8](./08-serialization-spi.md)）完全同构--microsphere 「极简接口 + 富注册表」风格的统一体现。
> **与 §9 的关联**：`EventDispatcher.parallel` 被 [§9 文件监听](./09-file-watch-service.md) 的 `StandardFileWatchService` 复用--文件事件通过事件总线异步分发。§9 的 Reactor 模式（事件循环/处理分离）是异步分发的更深一层讨论。
> **与 §4 的关联**：`EventListener extends Prioritized`，监听器按优先级排序通知，是 [§4 SPI+Prioritized](./04-spi-prioritized.md) 的直接应用。
