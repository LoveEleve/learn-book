# 18-07 线程安全与并发设计

## 目录

- [18-dynamic 中的并发问题汇总](#18-dynamic-中的并发问题汇总)
- [volatile 在 DataSource 热替换中的作用](#volatile-在-datasource-热替换中的作用)
- [synchronized(mutex) 原子切换的临界区保护](#synchronizedmutex-原子切换的临界区保护)
- [ConcurrentHashMap 在 InitializeErrors 中的应用](#concurrenthashmap-在-initializeerrors-中的应用)
- [ScheduledExecutorService 延迟关闭的资源管理](#scheduledexecutorservice-延迟关闭的资源管理)
- [ThreadPoolExecutor 并行创建子上下文](#threadpoolexecutor-并行创建子上下文)
- [事件广播中的并发保护](#事件广播中的并发保护)
- [ConcurrentSkipListSet 的防重复处理](#concurrentskiplistset-的防重复处理)

---

## 18-dynamic 中的并发问题汇总

先列出整个模块中涉及并发的所有场景，然后再逐个分析：

| 场景 | 涉及的类 | 并发的挑战 |
|------|---------|-----------|
| DynamicDataSource 热替换 | `DynamicDataSource` | 多个线程同时访问 delegate，切换时保证原子性和可见性 |
| 子上下文并行创建 | `DynamicJdbcContextApplicationListener` | 多个 config 由线程池并行初始化，失败收集要线程安全 |
| 延迟关闭旧上下文 | `DynamicDataSource` | 切换后延迟关闭的调度需要与正常关闭协调 |
| 事件传播 | `PropagatingDynamicJdbcConfigChangedEventListener` | Zone 变更事件可能并发到达 |
| Auto-Configuration 缓存 | `DynamicJdbcAutoConfigurationRepository` | 多子上下文同时访问缓存 |
| 防重复处理 | `OnceApplicationPreparedEventListener` | ApplicationPreparedEvent 可能并发触发 |
| Bean 升迁 | `DynamicJdbcChildContextRefreshedListener` | 父上下文可能有多个子上下文同时升迁 Bean |
| 错误收集 | `InitializeErrors` | 多个线程同时添加错误信息 |

---

## volatile 在 DataSource 热替换中的作用

### 问题

```java
public class DynamicDataSource implements DataSource {
    private DataSource delegate;  // 没有 volatile

    public Connection getConnection() throws SQLException {
        return delegate.getConnection();  // 可能读到过期的 delegate
    }
}
```

如果没有 volatile，线程 A 调用 `initializeDataSource()` 修改 `delegate` 后，线程 B 调用 `getConnection()` 可能**看不到**这个修改。

### Java 内存模型（JMM）的可见性问题

```
线程 A（切换线程）                      线程 B（读取线程）
  │                                      │
  ├─ delegate = newDataSource            │
  │  （写入 CPU 缓存）                    │
  │                                      ├─ delegate.getConnection()
  │                                      │  （从 CPU 缓存读取）
  │                                      │  可能读到旧的 delegate！
  │                                      │  因为 A 的写入可能还在缓存中
  │                                      │  没有刷入主内存
```

### volatile 的语义

```java
private volatile DataSource delegate;

// volatile 保证了：
// 1. 可见性：对 delegate 的写入立即刷入主内存，其他线程读取时从主内存读
// 2. 禁止指令重排：delegate 的写入不会与之前的操作重排序
```

**在 DynamicDataSource 中的使用**：

```java
public class DynamicDataSource implements DataSource {
    private final Object mutex = new Object();

    private volatile DataSource delegate;        // 热替换的数据源
    private volatile DynamicJdbcChildContext dynamicDataSourceChildContext;  // 热替换的子上下文
    private volatile boolean initialized;         // 初始化标志

    @Override
    public Connection getConnection() throws SQLException {
        return getDelegate().getConnection();  // 读 volatile
    }

    protected DataSource getDelegate() {
        DataSource ds = this.delegate;  // volatile 读
        if (ds == null) {
            ds = initializeDataSource();  // 懒加载
        }
        return ds;
    }

    private DataSource initializeDataSource(...) {
        // ...
        synchronized (mutex) {
            // volatile 写（在 synchronized 块内）
            DynamicDataSource.this.delegate = latestDataSource;
            DynamicDataSource.this.dynamicDataSourceChildContext = dynamicDataSourceChildContext;
        }
        // ...
    }
}
```

### volatile + synchronized 的配合

```java
// volatile：保证 delegate 对所有线程可见
// synchronized(mutex)：保证切换操作的原子性

private DataSource initializeDataSource(...) {
    DataSource latestDataSource = buildNewDataSource();

    synchronized (mutex) {
        // 在锁的保护下，原子地切换 delegate 和 childContext
        DynamicDataSource.this.delegate = latestDataSource;              // volatile 写
        DynamicDataSource.this.dynamicDataSourceChildContext = newCtx;  // volatile 写

        // 异步关闭旧上下文（previousChildContext 是切换前的引用）
        closeDynamicDataSourceChildContext(previousChildContext, true);
    }

    return latestDataSource;
}
```

**为什么 synchronized 内还要 volatile？**

`synchronized` 块在退出时会释放锁，触发内存屏障，确保块内的写入被推送到主内存。所以 `synchronized` 本身就保证了可见性。那为什么要加 `volatile`？

因为 `getConnection()` 的 `getDelegate()` 方法**不在 synchronized 块内**：

```java
public Connection getConnection() throws SQLException {
    // 这里没有 synchronized！
    // 如果没有 volatile，可能读到过期的 delegate
    return getDelegate().getConnection();
}
```

`getConnection()` 是高频调用路径，不能在每次调用时都加锁。所以用 `volatile` 保证无锁读取的可见性，用 `synchronized` 保证写入的原子性。

### volatile 的延迟关闭标志

```java
private volatile boolean initialized;

@Override
public void afterPropertiesSet() {
    if (initialized) {
        logger.debug("已经初始化过了");
        return;
    }
    // ...
    initializeApplicationListeners();
    if (null == this.delegate) {
        initializeDataSource();
    }
    initialized = true;  // volatile 写
}
```

这里用 `volatile` 确保多线程环境下 `afterPropertiesSet` 不会被重复执行。但注意这段代码本身不是原子的——两个线程可能同时进入 `afterPropertiesSet()`，都判断 `initialized == false`，都调用 `initializeDataSource()`。这在目前的设计中不会造成问题（`initializeDataSource` 内部有 synchronized 保护），但这是一个可能的并发 bug。

---

## synchronized(mutex) 原子切换的临界区保护

### 临界区内容

```java
synchronized (mutex) {
    DataSource previousDataSource = DynamicDataSource.this.delegate;   // 读旧
    DynamicDataSource.this.delegate = latestDataSource;                 // 写新
    DynamicDataSource.this.dynamicDataSourceChildContext = newCtx;     // 更新上下文

    closeDynamicDataSourceChildContext(previousChildContext, true);    // 关闭旧
}
```

这段临界区保护了三个操作的一致性：
1. **读取旧 delegate**：确保关闭的是切换前的那个
2. **写入新 delegate**：确保写入后所有线程都看到新的
3. **引用替换**：确保 delegate 和 childContext 的引用始终一致（不会出现 delegate 是新的但 childContext 是旧的）

### mutex 对象的选择

```java
private final Object mutex = new Object();
```

为什么不直接 `synchronized(this)`？

1. **避免外部锁**：如果外部代码也 `synchronized(dynamicDataSource)`，可能造成死锁
2. **锁的语义明确**：`mutex` 只保护这个对象的内部状态切换，不保护其他东西
3. **代码可读性**：看到 `mutex` 就知道保护的是什么

### 锁的范围控制

```java
private DataSource initializeDataSource(...) {
    DataSource latestDataSource = null;

    // 1. 不在锁内：创建上下文（重操作，秒级）
    DynamicJdbcChildContext newCtx = new DynamicJdbcChildContext(...);
    newCtx.mergeParentEnvironment();
    newCtx.refresh();  // 完整 Spring 启动

    // 2. 从新上下文获取 DataSource（轻操作）
    latestDataSource = getDataSource(newCtx);

    // 3. 在锁内：原子切换（轻操作，微秒级）
    synchronized (mutex) {
        DataSource previous = DynamicDataSource.this.delegate;
        DynamicDataSource.this.delegate = latestDataSource;
        DynamicDataSource.this.dynamicDataSourceChildContext = newCtx;
        closeDynamicDataSourceChildContext(previous, true);
    }

    return latestDataSource;
}
```

关键设计：**锁的粒度尽量小**。创建子上下文（refresh）需要 1-3 秒，这期间不应该持有锁——否则所有 `getConnection()` 都被阻塞。只有最后的指针切换才需要锁，这个操作只需几微秒。

---

## ConcurrentHashMap 在 InitializeErrors 中的应用

### 问题

```java
// DynamicJdbcContextApplicationListener
// 多个线程并行初始化子上下文
// 某个子上下文初始化失败时，需要收集所有错误
// 所有完成后再统一抛异常
```

### InitializeErrors 的设计

```java
class InitializeErrors {

    // ConcurrentHashMap：线程安全的错误收集
    private final ConcurrentHashMap<String, Throwable> errors = new ConcurrentHashMap<>();

    public void addError(String configPropertyName, Throwable error) {
        // 线程安全的 put
        errors.put(configPropertyName, error);
    }

    public boolean hasError() {
        return !errors.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        errors.forEach((name, error) -> {
            sb.append("Config[").append(name).append("]: ");
            sb.append(error.getMessage()).append("\n");
        });
        return sb.toString();
    }
}
```

### 为什么不直接用 HashMap

如果使用普通 `HashMap`，多个线程同时 `put` 时可能会导致：
1. 数据丢失：两个线程同时写入不同的 key，一个覆盖另一个
2. 死循环：JDK 1.7 的 `HashMap` 在并发 `resize()` 时可能形成环形链表，导致后续 `get()` 死循环。JDK 1.8+ 修复了这个问题，但并发 `put` 仍可能丢失数据

### ConcurrentHashMap 的适用性

`ConcurrentHashMap` 在这里的选择是合适的，因为：
1. **写多读少**：每个失败子上下文写一次，最后读一次
2. **不需要排序**：`ConcurrentHashMap` 不保证顺序，但这里不需要
3. **没有复合操作**：没有 `putIfAbsent`、`computeIfAbsent` 等复合操作的原子性需求

### 使用方式

```java
InitializeErrors initializeErrors = new InitializeErrors();

for (Map.Entry<String, DynamicJdbcConfig> entry : configEntrySet) {
    executorService.execute(() -> {
        try {
            initializeDynamicJdbcChildContext(entry, context);
        } catch (Throwable t) {
            // 多个线程并发 addError，线程安全
            initializeErrors.addError(entry.getKey(), t);
        }
    });
}

// 等待所有线程完成
// ...

// 所有完成后，检查是否有错误
if (initializeErrors.hasError()) {
    throw new DynamicJdbcInitializeException(initializeErrors.toString());
}
```

---

## ScheduledExecutorService 延迟关闭的资源管理

DynamicDataSource 切换后，旧的子上下文需要延迟关闭（默认 60s），用到 `ScheduledExecutorService`。

### 延迟关闭的调度

```java
public class DynamicDataSource implements DataSource, ... {
    private final ScheduledExecutorService closeScheduler;

    private final Duration dynamicDataSourceChildContextCloseDelay;

    public DynamicDataSource(...) {
        this.closeScheduler = Executors.newSingleThreadScheduledExecutor();
        this.dynamicDataSourceChildContextCloseDelay = getDynamicDataSourceChildContextCloseDelay(...);
    }

    private void closeDynamicDataSourceChildContext(
            ConfigurableApplicationContext oldContext, boolean async) {
        if (oldContext != null) {
            if (async) {
                long delay = dynamicDataSourceChildContextCloseDelay.toMillis();
                closeScheduler.schedule(
                    () -> closeContext(oldContext),
                    delay, TimeUnit.MILLISECONDS
                );
            } else {
                closeContext(oldContext);
            }
        }
    }

    private void closeContext(ConfigurableApplicationContext context) {
        context.close();
    }

    @Override
    public void destroy() {
        // 立即关闭当前上下文（async=false）
        closeDynamicDataSourceChildContext(dynamicDataSourceChildContext, false);
        shutdownScheduler(closeScheduler);
    }

    private void shutdownScheduler(ScheduledExecutorService scheduler) {
        if (scheduler != null && !scheduler.isShutdown() && !scheduler.isTerminated()) {
            scheduler.shutdown();
        }
    }
}
```

### 延迟关闭的安全隐患

```
时间轴：
T0: 切换发生
    delegate = newDataSource (new HikariCP pool)
    oldContext → scheduler.schedule(() → close(), 60s)

T0 ~ T60: 过渡期
    新请求 → getConnection() → newDataSource (新连接池)
    旧请求 → 在用 oldDataSource (旧连接池) → continue
    
T60: 调度执行
    closeContext(oldContext)
    → oldContext.close()
      → 所有旧连接池关闭
      → 如果还有未完成的旧请求，报错

T > 60: 完全切换完成
    只有新连接池在运行
```

**风险**：如果某个事务执行超过 60s，在 T60 时会被强制中断。数据库层的长事务（如批量数据迁移、大报表）可能受影响。

**改进思路**（已在 Q3 中提出）：ConnectionWrapper + 活跃连接计数，在有活跃连接时延迟关闭。

### 单线程调度器的选择

`Executors.newSingleThreadScheduledExecutor()` —— 只用一个线程处理所有延迟关闭任务。为什么？

1. 避免线程泄漏——每次切换都可能产生一个延迟任务，如果用多线程调度器，可能积累大量空闲线程
2. 单个 DynamicDataSource 实例的切换是串行的（`synchronized` 保护），不会同时产生多个需要并发关闭的旧上下文

注意：`newSingleThreadScheduledExecutor()` 默认创建非守护线程。如果应用未正确调用 `destroy()` 就退出，这个线程会阻止 JVM 关闭。在 `destroy()` 中调用 `shutdownScheduler()` 可以解决这个问题。

注意：如果多个 DynamicDataSource 实例同时切换（多 config 场景），每个实例有自己的调度器，此时可能有多个旧上下文在不同调度器中同时到达关闭时间——这没有问题，因为每个调度器只处理自己的旧上下文。

### 关闭时的 pending 任务风险

当 `destroy()` 被调用时，`shutdown()` 会取消所有尚未执行的延迟任务（pending ScheduledFuture）。这意味着：如果 `destroy()` 触发时，调度队列中有一个已提交但未执行的旧上下文关闭任务，该任务会被取消，对应的旧上下文永远不会被关闭。这在实际中概率较低（`destroy()` 通常只在 JVM 关闭时调用，此时资源泄漏没有影响），但在动态销毁重建场景中需要注意。

---

## ThreadPoolExecutor 并行创建子上下文

多 config 模式下，多个子上下文并行创建。

### 线程池配置

```java
// DynamicJdbcContextApplicationListener
private void processDynamicJdbcChildContexts(
        Set<Map.Entry<String, DynamicJdbcConfig>> configs,
        ConfigurableApplicationContext context) {

    int parallelism = configs.size();
    // 创建固定大小的线程池（等于 config 数量）
    ThreadPoolExecutor executor = (ThreadPoolExecutor) newFixedThreadPool(parallelism);

    InitializeErrors initializeErrors = new InitializeErrors();

    for (Map.Entry<String, DynamicJdbcConfig> entry : configs) {
        executor.execute(() -> {
            try {
                initializeDynamicJdbcChildContext(entry, context);
            } catch (Throwable t) {
                initializeErrors.addError(entry.getKey(), t);
            }
        });
    }

    // 等待所有线程完成
    boolean terminated = false;
    long completedTaskCount = 0;
    while (!terminated) {
        try {
            terminated = executor.awaitTermination(1, TimeUnit.SECONDS);
            completedTaskCount = executor.getCompletedTaskCount();
            if (completedTaskCount == parallelism) {
                break;
            }
        } catch (InterruptedException e) {
            terminated = true;
        }
    }

    if (initializeErrors.hasError()) {
        throw new DynamicJdbcInitializeException(initializeErrors.toString());
    }

    executor.shutdownNow();  // 所有任务已完成，立即关闭
}
```

### 设计细节

**1. `parallelism = configs.size()`**

为什么不固定为 `Runtime.getRuntime().availableProcessors()`？因为每个子上下文的创建是 IO 密集型（Spring 容器 refresh 涉及类加载、资源扫描、Auto-Configuration 处理），不是 CPU 密集型。IO 密集型的线程池大小可以更大。

但 `size()` 也有问题——如果 config 数量很大（比如 50 个），会创建 50 个线程，可能导致 CPU 上下文切换过多。实际项目中应该设一个上限（比如 `Math.min(configs.size(), 10)`）。

**2. `awaitTermination` 轮询**

```java
while (!terminated) {
    terminated = executor.awaitTermination(1, TimeUnit.SECONDS);
    completedTaskCount = executor.getCompletedTaskCount();
    if (completedTaskCount == parallelism) {
        break;
    }
}
```

为什么不直接用 `executor.awaitTermination(Long.MAX_VALUE, ...)`？因为 `awaitTermination` 在任务抛出未捕获异常时可能无限阻塞——线程池中的线程虽然失败了，但不会自动终止。轮询方式即使遇到异常线程，也能在每秒的 `completedTaskCount` 检查中发现任务已完成（异常任务也算完成），从而退出等待循环。

**3. `shutdownNow()` 的使用**

```java
executor.shutdownNow();
```

所有任务已经完成（`completedTaskCount == parallelism`），所以 `shutdownNow()` 不会中断任何任务。它只是为了释放线程池占用的资源。但使用 `shutdownNow()` 而不是 `shutdown()` 可能有风险——如果某个任务还没完成（理论上不应该，因为 `completedTaskCount` 检查保证了），`shutdownNow()` 会发送中断信号。

### 错误收集的时序

```
线程 1（子上下文 A 初始化）         线程 2（子上下文 B 初始化）
  │                                    │
  ├─ 开始 refresh                      ├─ 开始 refresh
  │                                    │
  ├─ 失败！                            ├─ 成功！
  │   catch(Throwable)                  │
  │   → addError("A", exception)       │
  │                                    │
  │  ConcurrentHashMap.put             │
  │  （线程安全）                       │
  │                                    │
  ├─ 线程结束                          ├─ 线程结束
  │                                    │
  │ 主线程（等待循环发现完成数==并行数）  │
  │ → hasError() == true               │
  │ → throw DynamicJdbcInitializeException
```

如果多个 config 失败，所有错误都被收集在 `ConcurrentHashMap` 中，最终一次性抛出。这使得用户能看到所有失败的 config，而不会在修复第一个后重启又遇到第二个。

---

## 事件广播中的并发保护

18-dynamic 大量依赖 Spring 事件机制，事件广播过程中的并发保护由 Spring 内部处理。

### Spring 的事件广播实现

Spring 的 `SimpleApplicationEventMulticaster` 在广播事件时，会先对 Listener 集合做一个快照（复制），然后在快照上遍历。这意味着广播期间如果有其他线程添加/移除 Listener，不会影响当前正在进行的广播。

```java
// SimpleApplicationEventMulticaster.multicastEvent() 的逻辑：
// 1. 获取所有 Listener（返回一个快照，不是原始引用）
// 2. 在快照上遍历并逐个调用
```

这个机制的核心是：**读取时加锁复制，写入时直接修改原始集合**。虽然 Spring 没有直接使用 `java.util.concurrent.CopyOnWriteArrayList`，但实现思路一致——读操作不阻塞写操作，写操作也不阻塞正在进行的读。

### CopyOnWriteArrayList 的特性（类比）

```java
// 写入：每次修改都创建新数组，写入成本高
// 读取：无锁，直接读数组引用
```

这个模式的关键收益：
- **读 >> 写**：Listener 注册/注销是初始化时的操作，运行时基本都是广播事件（读）
- **避免事件广播时的并发修改**：广播时遍历的是快照，不是原始集合

### 在 18-dynamic 中的影响

`DynamicJdbcChildContextRefreshedListener` 在父上下文中注册 `ContextClosedEvent` 监听器：

```java
parentContext.addApplicationListener((ApplicationListener<ContextClosedEvent>) e -> {
    childContext.close();
});
```

这个注册操作可能发生在父上下文中其他事件正在广播的同时。Spring 的快照机制保证了不会抛出 `ConcurrentModificationException`，并且新注册的 Listener 会在下一次广播时生效。

---

## ConcurrentSkipListSet 的防重复处理

`OnceApplicationPreparedEventListener` 用 `ConcurrentSkipListSet` 来追踪已处理过的上下文 ID。

### 为什么用 ConcurrentSkipListSet

```java
public abstract class OnceApplicationPreparedEventListener
        implements ApplicationListener<ApplicationPreparedEvent>, Ordered {

    private final Set<String> processedContextIds = new ConcurrentSkipListSet<>();

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        if (!isMainApplicationContext(context)) {
            return;
        }

        String contextId = context.getId();
        // 原子操作：如果不存在则添加，返回 true 表示第一次添加
        if (!processedContextIds.add(contextId)) {
            // 已经处理过了，跳过
            return;
        }

        onApplicationEvent(event.getSpringApplication(), event.getArgs(), context);
    }
}
```

对比几种选择的区别：

```java
// 方案 A：普通 HashSet（线程不安全）
// ❌ 两个线程可能同时通过 contains() 和 add() 之间的判断
Set<String> set = new HashSet<>();
if (!processedContextIds.contains(contextId)) {  // 线程 A 和 B 都返回 false
    processedContextIds.add(contextId);           // 都 add，但只有一个生效
    // 两个线程都执行了处理逻辑 → 重复处理！
}

// 方案 B：synchronized 同步（正确但代码冗余）
Set<String> set = new HashSet<>();
synchronized (this) {
    if (!processedContextIds.contains(contextId)) {
        processedContextIds.add(contextId);
        // 处理...
    }
}

// 方案 C：ConcurrentSkipListSet（正确且简洁）
Set<String> set = new ConcurrentSkipListSet<>();
if (!set.add(contextId)) {  // 原子检查+添加
    // 已经处理过，跳过
    return;
}
// 处理...
```

`ConcurrentSkipListSet.add()` 的语义：
- 返回 `true`：这个 key 之前不存在，添加成功（当前调用者是第一个）
- 返回 `false`：这个 key 已经存在（有另一个线程先添加了），当前调用者应该跳过

这个"检查+添加"的原子性避免了 `contains()` 和 `add()` 之间的竞态条件。

### 为什么不用 ConcurrentHashMap

`ConcurrentHashMap` 也可以做 `putIfAbsent`：

```java
ConcurrentHashMap<String, Boolean> map = new ConcurrentHashMap<>();
if (map.putIfAbsent(contextId, Boolean.TRUE) == null) {
    // 第一次处理
}
```

但 `ConcurrentSkipListSet` 的语义更清晰——`Set` 而不是 `Map`，表示"已处理的 ID 集合"，读代码时意图更明确。

---

## 并发设计的整体模式

把以上所有并发工具串起来，18-dynamic 的并发设计遵循以下模式：

| 并发问题 | 解决方案 | 选择原因 |
|---------|---------|---------|
| 高频读取 + 低频率写入 | `volatile` + `synchronized` | 读无锁，写有锁 |
| 多线程收集错误 | `ConcurrentHashMap` | 写多读少，不需要排序 |
| 延迟关闭 | `ScheduledExecutorService` | 单线程调度，避免泄漏 |
| 并行初始化 | `ThreadPoolExecutor` | 固定大小，有超时等待 |
| 事件广播 | Spring 内部快照机制（读时复制） | 读远大于写，广播不阻塞注册 |
| 防重复处理 | `ConcurrentSkipListSet` | 原子检查+添加 |

核心原则：
1. **尽可能无锁**：高频路径（`getConnection()`）不用锁
2. **锁的粒度尽量小**：创建子上下文（秒级）不在锁内，只有指针切换（微秒级）在锁内
3. **优先使用并发容器**：不自己实现同步逻辑
4. **延迟关闭缓解**：用时间换取正在执行的事务的完成机会
