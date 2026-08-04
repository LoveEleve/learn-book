# SPI + Prioritized 排序 —— 从「声明顺序不可靠」到「显式权重排序」

> 源码对应：`ServiceLoaderUtils.java`(911行)、`Prioritized.java`
> 与前篇关系：confucius-commons 的 `loadFirst/last` 依赖 ClassLoader 层次 → microsphere-java 用 `Prioritized` 排序替代

---

## 一、confucius-commons 的方案为什么不够

confucius-commons 的 `ServiceLoaderUtils.loadFirstService()` 和 `loadLastService()` 依赖 ClassLoader parent-delegation 的返回顺序——父 CL 资源在前、子 CL 资源在后。

这个方案在三种场景下不可靠：
1. **不是所有 ClassLoader 都遵循 parent-delegation**（Tomcat WebAppCL 逆序、OSGi bundle CL）
2. **JDK 9 模块系统中 ModuleLayer 的顺序可能不同**
3. **你无法控制第三方代码的 SPI 声明顺序**——如果两个 jar 都在同一个 ClassLoader 中，`META-INF/services` 文件的顺序不确定（文件系统决定）

microsphere-java 的解法：让每个 SPI 实现**自己声明优先级**，框架按优先级排序后使用。

---

## 二、核心机制——`sort(services, COMPARATOR)`

### 2.1 一行关键代码

```java
// ServiceLoaderUtils.java:873-892
static <S> List<S> loadServicesAsList(Class<S> serviceType, ClassLoader classLoader) {
    ServiceLoader<S> serviceLoader = ServiceLoader.load(serviceType, classLoader);
    Iterator<S> iterator = serviceLoader.iterator();
    
    if (!iterator.hasNext()) {
        throw new IllegalArgumentException("No Service implementation was defined");
    }

    // 关键：将 ServiceLoader 的 Iterator 转为 LinkedList → 排序 → 返回
    LinkedList<S> serviceList = newLinkedList(iterator);
    sort(serviceList, COMPARATOR);   // ← 这就是全部魔法
    return serviceList;
}
```

### 2.2 COMPARATOR 的逻辑

```java
// Prioritized.java:61-76
Comparator<Object> COMPARATOR = (one, two) -> {
    boolean b1 = one instanceof Prioritized;
    boolean b2 = two instanceof Prioritized;

    if (b1 && b2) {
        // 两个都实现了 Prioritized → 按 priority 值升序排列
        // Priority 值越小 → 越排在前面
        return ((Prioritized) one).compareTo((Prioritized) two);
    } else if (b1) {
        // only one implements Prioritized → one 排在前面
        return -1;
    } else if (b2) {
        // only two implements Prioritized → two 排在前面
        return 1;
    }
    return 0;  // 都不实现 → 保持原始顺序
};
```

```java
// compareTo 的默认实现（Prioritized.java）
default int compareTo(Prioritized that) {
    return Integer.compare(this.getPriority(), that.getPriority());
    // this=MIN → that=MAX → Integer.compare(MIN, MAX) → 负数 → MIN 在前
    // 结论：Priority 值越小 → 排越前面
}
```

### 2.3 优先级值的约定

> **勘误说明**：本表曾把 `MIN_PRIORITY`/`MAX_PRIORITY` 的数值标反，与源码 `Prioritized.java:81-86` 不符。下表已按源码修正。具体为 `MAX_PRIORITY = MIN_VALUE`（最高优先级，数值最小，排最前）、`MIN_PRIORITY = MAX_VALUE`（最低优先级，数值最大，排最后）。

| 常量 | 源码定义 | 实际 int 值 | 排序位置 | 含义 |
|---|---|---|---|---|
| `MAX_PRIORITY` | `= MIN_VALUE` | `Integer.MIN_VALUE` | **最前** | 最高优先级--最先被遍历、最先被匹配 |
| `NORMAL_PRIORITY` | `= 0` | `0` | 中间 | 默认 |
| `MIN_PRIORITY` | `= MAX_VALUE` | `Integer.MAX_VALUE` | **最后** | 最低优先级--最后被遍历（兜底） |

**关键理解**：`sort` 是**升序**排列。Priority 值越小，排越前面--即最先被遍历、最先被匹配。`MAX_PRIORITY`（最高优先级）的数值是 `Integer.MIN_VALUE`（最小整数），所以它排最前。这是「语义命名」（max = 优先级最高）与「数值约定」（小 = 排前）方向相反的妥协--名字说的是「优先级高低」，数值走的是「升序排序」，两者方向相反。Spring 的 `Ordered.HIGHEST_PRECEDENCE = Integer.MIN_VALUE` 是同样的妥协，microsphere 跟随 Spring 约定。

**命名陷阱**：第一次读源码极容易把 `MAX_PRIORITY` 当成「数值最大」。实际它是「优先级最高」，数值反而是最小的。记住「优先级高 = 数值小 = 排前」即可。

**实际案例**：

```java
// MavenArtifactResourceResolver: priority = 1（数值小 -> 排在前面 -> 最先被尝试）
// ManifestArtifactResourceResolver: priority = 5
// ArchiveFileArtifactResourceResolver: priority = 9（数值大 -> 排在后面 -> 最后被尝试）

// 遍历顺序：Maven(1) -> Manifest(5) -> ArchiveFile(9)
// Maven 最准 -> priority 数值最小 -> 第一个执行 ✓
```

### 2.4 缓存控制——默认关闭

```java
// ServiceLoaderUtils.java:132-140
@ConfigurationProperty(
    name = "microsphere.service-loader.cached",
    defaultValue = "false",           // 默认不缓存
    description = "Whether to cache the loaded services"
)
public static final boolean SERVICE_LOADER_CACHED = 
    parseBoolean(System.getProperty("microsphere.service-loader.cached", "false"));

private static final ConcurrentMap<Class<?>, List<?>> servicesCache = new ConcurrentHashMap<>();
```

**为什么默认不缓存？** SPI 实现可能在运行时变化（OSGi 热部署、模块化框架动态加载新 jar）。缓存会让框架感知不到新增的实现。如果你确认运行时 classpath 不会变化，可以开启缓存提升性能。

**`getServiceClassNames()` 的价值**：框架在扫描阶段需要知道「有哪些 SPI 实现」，但不想立即实例化所有实现。`getServiceClassNames()` 只读 `META-INF/services/` 文件中的类名字符串——零副作用。

**`failFast` 参数**：`getServiceClasses(type, cl, failFast)`——`failFast=true` 时实现类加载失败或类型不匹配立即抛 `IllegalStateException`。默认 true——启动时失败优于运行时失败。

**同一优先级风险**：`Collections.sort` 稳定排序——同值保持原始顺序。但原始顺序不可靠（文件系统决定）。应避免两个 SPI 实现使用相同的 priority 值。

**非确定性优先级**：`getPriority()` 每次返回不同值 → 排序结果不确定 → 同一请求可能命中不同实现。无防御机制——依赖开发者自律。


## 三、与 confucius-commons 的对比

| | confucius-commons | microsphere-java |
|---|---|---|
| 选择策略 | `loadFirstService`/`loadLastService`（依赖 CL 层次） | `Prioritized` 排序 |
| 可靠性 | 依赖 parent-delegation——OSGi/模块系统下不确定 | 开发者显式声明优先级——跨环境可靠 |
| 缓存 | 无 | 支持（默认关闭） |
| 可扩展性 | 不可扩展 | `Prioritized` 接口——任何实现都能声明优先级 |
| 获取 SPI 类名（不实例化） | 无 | `getServiceClassNames()`——只读配置不加载类 |


## 四、发散 —— JDK 内置的排序方案

### 4.1 `ServiceLoader.load().stream()` (JDK 9+)

JDK 9+ 的 ServiceLoader 提供了 `stream()` 方法，可以在排序后选择：

```java
ServiceLoader<MyService> loader = ServiceLoader.load(MyService.class);
Optional<MyService> first = loader.stream()
    .sorted((p1, p2) -> {
        // 自定义排序逻辑
    })
    .findFirst()
    .map(ServiceLoader.Provider::get);
```

优势：JDK 内置，不需要 microsphere-java 依赖。劣势：排序逻辑在调用方——每个调用方都要自己排序，不如 microsphere 的「SPI 实现自声明优先级」集中管理。

### 4.2 `@javax.annotation.Priority` (JSR 250)

Java EE 的 `@Priority` 注解可以标记优先级，但没有统一的排序实现——需要框架自己读取注解值并排序。

### 4.3 Spring `@Order` / `Ordered`

Spring 的 `@Order` 和 `Ordered` 接口是 microsphere `Prioritized` 的同类方案。但 Spring 的 `Ordered` 实现需要 Spring 容器——microsphere 的不需要。

### 4.4 OSGi ServiceRanking（历史参考）

OSGi 的 `Constants.SERVICE_RANKING` 是最早的「SPI 优先级排序」概念——比 JDK ServiceLoader 早 10 年。今天 OSGi 已极少用于新项目（被 JDK 9 模块系统和 Spring Boot 替代），但作为优先级排序的思想先驱值得提及。


## 五、面试要点

**Q1：「ServiceLoader 加载的顺序是确定的吗？怎么控制？」**

答案：不确定——取决于 `ClassLoader.getResources()` 的返回顺序。两种控制方式：① confucius-commons 的 loadFirst/last——利用 ClassLoader 层次（但 OSGi/模块系统下不可靠）；② microsphere-java 的 Prioritized 排序——每个 SPI 实现声明自己的优先级，`sort(services, COMPARATOR)` 排序后第一个匹配的胜出。

**Q2：「Prioritized.COMPARATOR 的实现中，Priority 值越小排越前还是越后？」**

答案：越前。`compareTo` 使用 `Integer.compare(this.priority, that.priority)`——负数表示 this 在 that 之前。`Collections.sort` 升序排列——Priority=1 排在 Priority=9 之前。所以如果你想「最先被执行」，给 `MAX_PRIORITY`（= `Integer.MIN_VALUE`，数值最小，排最前）；想「最后兜底」，给 `MIN_PRIORITY`（= `Integer.MAX_VALUE`，数值最大，排最后）。

**注意命名陷阱**：`MAX_PRIORITY` 是「优先级最高」而非「数值最大」--它的数值反而是 `Integer.MIN_VALUE`。记住「优先级高 = 数值小 = 排前」即可。

追问：「为什么不直接用 `Comparator.comparingInt(Prioritized::getPriority).reversed()` 让高数值代表高优先级？」-> 那样命名和数值方向一致，更直观。当前的升序约定 + 反向命名（max=小数值）跟随 Spring `Ordered` 约定--`Ordered.HIGHEST_PRECEDENCE = Integer.MIN_VALUE` 也是同样的妥协。这是 Java 生态历史遗留，两套约定（Thread 大=高 vs Spring 小=高）并存。

**Q3：「microsphere 的 ServiceLoaderUtils 的缓存对性能有什么影响？为什么默认关闭？」**

答案：缓存存在 `ConcurrentHashMap` 中——首次加载后从缓存返回，避免重复实例化 SPI 实现。默认关闭因为运行时可能新增 SPI 实现（动态加载 jar）——缓存会让框架感知不到变化。适合开启的场景：运行时 classpath 不变的独立应用。

追问：「如果开启了缓存但 SPI 实现后来变了（热部署），缓存怎么知道要刷新？」→ 当前实现没有失效机制——只能重启 JVM。可以改进的方向：① 加 `refresh(serviceType)` 方法让调用方手动刷新；② 基于文件时间戳自动检测 `META-INF/services` 文件变化。

---

> **与 confucius-commons 的关联**：[详见 confucius-commons §5 - SPI 覆盖策略](../../01-confucius-commons-analysis/04-spi-classloader-strategy.md)