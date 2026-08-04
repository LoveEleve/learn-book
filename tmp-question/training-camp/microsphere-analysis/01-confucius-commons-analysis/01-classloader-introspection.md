# ClassLoader 内省 —— 框架作者如何"撬开"JDK 锁住的门

> confucius-commons 源码对应：`ClassLoaderUtils.java`、`ManagementUtils.java`
> 核心问题：JDK 为什么把 ClassLoader 内部状态锁住？框架作者需要哪些"不该知道"的信息？怎么在安全和诊断之间选择？

---

## 一、你能看到什么，JDK 不让你看到什么

### 1.1 公开的 ClassLoader API（你能用的）

```java
ClassLoader cl = MyClass.class.getClassLoader();

// 加载类（公开）
cl.loadClass("com.example.Foo");     // 标准路径：parent delegation → findClass

// 获取父 ClassLoader（公开）
ClassLoader parent = cl.getParent();  // AppCL → ExtCL → null(Bootstrap)

// 获取资源（公开）
URL url = cl.getResource("config.properties");
Enumeration<URL> urls = cl.getResources("META-INF/services/com.example.Spi");
```

### 1.2 被隐藏的 ClassLoader API（你想用但用不了的）

```java
// 这些都是真实存在的 API，但不是 public/protected 就是 private：

// ❌ protected：只有 ClassLoader 的子类能调用
cl.findLoadedClass("com.example.Foo");  // 这个方法存在，但你调不了

// ❌ private：连子类也调不了，只有 ClassLoader 自己能调
cl.classes;  // HotSpot 内部维护的 List<Class<?>>，保存所有已加载的类

// ❌ 不存在：JDK 压根没提供的
cl.listAllLoadedClasses();  // 这个方法不存在
cl.isClassLoaded("com.example.Foo");  // 这个方法也不存在
```

**这里有一个重要的区分**：`findLoadedClass` 是 `protected` 的——这代表 JDK 设计者承认「子类可能需要知道加载状态」，但不希望「任何代码都能查」。`classes` 字段是 `private` 的——这代表 JDK 设计者认为「外部代码不应该知道 ClassLoader 内部维护了哪些类」。

### 1.3 类加载的三个入口——loadClass / forName / findLoadedClass 的区别

这是很多人困惑的地方：Java 有三种方式获取一个 Class 对象，但它们的语义完全不同。

```java
// 方式1：ClassLoader.loadClass(name) —— 只加载，不初始化
Class<?> clazz = classLoader.loadClass("com.example.Foo");
// 内部流程：
//   1. findLoadedClass(name) → 如果已加载，直接返回
//   2. parent.loadClass(name) → 委托父 ClassLoader
//   3. findClass(name) → 自己从字节码定义类
//   4. resolveClass(clazz) → 链接（验证+准备+解析），可选步骤
// 注意：loadClass 不执行 <clinit>（静态代码块）！
//       类的初始化被延迟到第一次使用时。

// 方式2：Class.forName(name) —— 加载 + 链接 + 初始化
Class<?> clazz = Class.forName("com.example.Foo");
// 内部流程：
//   1-4. 同上（加载+链接）
//   5. 立即执行 <clinit>（静态代码块和静态字段初始化）
// 这是 JDBC 驱动的注册机制：Class.forName("com.mysql.jdbc.Driver")
// 触发 Driver 的静态代码块向 DriverManager 注册自己。

// 方式3：ClassLoader.findLoadedClass(name) —— 只查询，不加载
Class<?> clazz = classLoader.findLoadedClass("com.example.Foo");
// 内部流程：只在内部的 classes 列表中查，找到就返回，找不到返回 null
// 不触发任何加载、链接、初始化行为
// 这就是 confucius-commons 需要反射访问的方法——它是唯一「只查不加载」的 API
```

**三者的核心区别**：

| | `loadClass` | `Class.forName` | `findLoadedClass` |
|---|---|---|---|
| 触发加载 | ✅ | ✅ | ❌ |
| 触发初始化 | ❌（延迟） | ✅（立即） | ❌ |
| 查找父链 | ✅ | ❌（用调用者的 CL） | ❌（只查当前 CL） |
| 需要反射 | ❌ | ❌ | ✅（protected） |
| 用途 | 框架内的类加载控制 | JDBC 驱动等需要静态初始化的场景 | 诊断：类是否已被加载 |

**为什么这个区别对框架作者很重要？**

想象你的框架在扫描 classpath 时发现了 1000 个类。如果你用 `Class.forName` 或 `LoadClass`，所有 1000 个类的静态初始化块都会被触发——数据库连接可能被打开、日志框架可能被初始化、单例对象可能被创建——在你还没决定是否使用这些类之前。

用 `findLoadedClass`（只查不加载）可以安全地扫描，不影响任何现有状态。

### 1.4 `defineClass` —— ClassLoader 的核心能力

讲了这么多「查类」，那「造类」呢？`defineClass` 是 ClassLoader 将字节码转换成 JVM 内部 `Class` 对象的唯一入口：

```java
// ClassLoader.java（JDK 源码，简化）
protected final Class<?> defineClass(String name, byte[] b, int off, int len) {
    // 1. 调用 JVM 内部 native 方法处理字节码
    // 2. 返回 Class<?> 对象
    // 这是 JVM 中从字节数组到类的唯一转换点——JVM 安全关键
}
```

**为什么 `defineClass` 是 `protected final` 的？**

`protected`：框架作者需要重写 `findClass`（从硬盘/网络读取字节码），然后在 `findClass` 中调用 `defineClass`（把字节码交给 JVM）。

`final`：不能被重写。如果框架可以篡改 `defineClass` 的逻辑，就绕过了 JVM 的字节码验证——类型安全就崩溃了。

**loadClass 的完整内部链路**：

```
loadClass(name)
  ↓
  ① findLoadedClass(name)   ← 今天的主角：查询类是否已经加载
  │  如果找到 → 直接返回
  ↓
  ② parent.loadClass(name)  ← 标准双亲委托
  │  如果父 CL 有 → 返回父的结果
  ↓
  ③ findClass(name)          ← 框架重写这个方法
  │  你的代码：byte[] bytes = readFromFileOrNetwork(name)
  ↓
  ④ defineClass(name, bytes) ← JVM 内部：字节码 → Class 对象
  │  如果字节码非法 → ClassFormatError
  ↓
  ⑤ resolveClass(clazz)      ← 链接：解析符号引用
  ↓
  返回 Class<?>
```

confucius-commons 的 `ClassLoaderUtils` 做的就是第一步——不需要加载、不需要定义，只需要快速回答「当前状态是什么」。

---

## 二、为什么 `findLoadedClass` 是 protected——Java 安全模型的命名空间隔离

### 2.1 先理解 ClassLoader 的命名空间

你大概率知道这个常识：「同一个全限定类名，被两个不同 ClassLoader 分别加载，JVM 认为它们是两个不同的类」：

```java
ClassLoader cl1 = new CustomClassLoader("v1/lib/");
ClassLoader cl2 = new CustomClassLoader("v2/lib/");

Class<?> foo1 = cl1.loadClass("com.example.Foo");
Class<?> foo2 = cl2.loadClass("com.example.Foo");

foo1 == foo2  // → false：两个 ClassLoader 各自有独立的 Foo 类
foo1.equals(foo2)  // → false
foo1.isAssignableFrom(foo2)  // → false
```

为什么 Java 这么设计？因为如果不做命名空间隔离，你可以通过伪造一个同名的恶意类，在 ClassLoader B 中覆盖 ClassLoader A 中的类——类型安全就崩溃了。

### 2.2 为什么不允许外部代码查「这个 ClassLoader 加载了哪些类」

如果任意代码可以查出「ClassLoader A 加载了 `com.example.Foo`」，你就可以：

**1. 推断应用使用的框架和库**

```java
// 攻击者代码
if (isClassLoaded(someClassLoader, "org.springframework.boot.autoconfigure.SpringBootApplication")) {
    // 这个 ClassLoader 里有 Spring Boot → 这个应用是 Spring Boot 项目
}
if (isClassLoaded(someClassLoader, "com.alibaba.nacos.api.NacosFactory")) {
    // 有 Nacos → 这是一个微服务应用，注册中心是 Nacos
}
```

这在安全审计中叫做**信息泄露**——攻击者可以通过反复探测来了解应用的内部架构。在 SecurityManager 环境下，这种探测应该被阻止。

**2. 探测「类是否已被加载」来做时序攻击**

```java
// 攻击者可以知道「用户刚刚是否执行了某个特定操作」
// 如果操作 X 会触发类 com.example.SensitiveClass 被加载
// 则攻击者可以探测这个类的加载状态来推断用户行为
```

这是**侧信道攻击**（side-channel attack）。虽然实际利用难度很高，但 JDK 的安全模型在设计时就考虑了这个风险。

**3. 在模块化环境中打破隔离**

在 OSGi 或 Java EE（Servlet 容器）环境中，不同 bundle 或 webapp 有独立的 ClassLoader。如果 bundle A 能查 bundle B 的 loaded classes，就能推断 bundle B 引入了哪些第三方库——如果有已知漏洞的版本，攻击者可以定向攻击。

---

## 三、框架作者的困境——「我需要知道，但你不让我知道」

### 3.1 为什么框架需要诊断 ClassLoader 状态

想象你正在写一个模块化框架，需要回答以下问题：

**问题一：「类是否已经被加载？」**

场景：你的框架提供了一种「插件热加载」机制。用户把新版本 jar 放到 plugins 目录，框架自动重新加载。但在重新加载前，你需要确认旧版本的类有没有仍在被使用。

**问题二：「类是哪个 ClassLoader 加载的？��**

场景：应用中报 `NoClassDefFoundError`。你需要知道这个类到底被哪个 ClassLoader 加载了——是不是被不该加载它的 ClassLoader 加载了？

**问题三：「当前 ClassLoader 到底加载了多少类？」**

场景：排查内存泄漏。大量类被加载后没有卸载（ClassLoader 没有被 GC），你需要知道当前状态以便定位是哪个 ClassLoader 在泄漏。

这三个问题，JDK 都没有提供直接 API 来回答。所以框架作者只能绕道。

### 3.2 Spring 和 Tomcat 都绕了，但不是公开 API

Spring Framework 内部的 `ClassPathScanningCandidateComponentProvider` 和 Tomcat 的 `WebappClassLoader` 都在内部用到了 `findLoadedClass`（它们继承了 ClassLoader，所以可以调 protected 方法）。但它们的能力被锁在各自框架内部，不对外暴露。

confucius-commons 做的事情：把这些框架内部才能用的能力，提取出来放到一个独立的工具类中——**任何代码都能调**。

---

## 四、永恒原理:内省 vs 封装--软件设计的永恒张力

在深入具体机制前，先拔高一个层次。confucius-commons 做的事情本质上是**「内省」（introspection）**--在运行时查询系统内部状态。而 JDK 把这些状态锁起来本质上是**「封装」（encapsulation）**--隐藏内部实现，只暴露稳定接口。

这对矛盾不只存在于 ClassLoader，是软件设计的永恒张力：

| 领域 | 封装方（隐藏内部） | 内省方（查询内部） | 张力 |
|---|---|---|---|
| Java ClassLoader | JDK（protected/private） | 框架作者（反射） | 安全 vs 诊断 |
| Linux 内核 | syscall 边界 | `/proc`/`/sys` 虚拟文件系统 | 稳定性 vs 可观测性 |
| 数据库 | SQL 接口 | `EXPLAIN`/`SHOW STATUS` | 抽象 vs 调优 |
| Kubernetes | CRD/API 边界 | `kubectl describe`/events | 声明式 vs 调试 |
| Microprofile/Micrometer | 指标接口 | `MeterRegistry`/`HealthCheck` | 隔离 vs 监控 |

**共同本质**：封装方追求「接口稳定、内部可变」（JDK 改 ClassLoader 实现不影响你的代码），内省方追求「看到内部真相」（诊断需要）。两者冲突--内省依赖内部实现，但内部实现随时可变。

**解法谱系**：

1. **完全封装**（最安全，最不可诊断）：JDK 默认状态。外部代码看不到 ClassLoader 内部。
2. **官方内省 API**（最佳平衡）：JDK 提供 `ClassLoadingMXBean`（类数量统计）、`Instrumentation.getAllLoadedClasses()`（所有已加载类）。但功能有限，且 `Instrumentation` 要求 javaagent 启动。
3. **反射内省**（最强能力，最高风险）：confucius-commons 的做法。反射 `findLoadedClass`/`classes` 字段。能力最强但依赖内部实现，JDK 升级可能失效。
4. **模块化内省**（JDK 9+ 的未来）：`Module` API 提供官方的模块层内省能力。但要求应用模块化，当前覆盖率低。

**confucius-commons 选了路线 3**（反射），因为它是 JDK 6 时代的产物--那时路线 2（`Instrumentation`）不够成熟，路线 4（模块化）不存在。今天的最佳实践是「能用路线 2 就不用路线 3」（§9 的决策框架）。

**跨语言对比**：Python 的 `sys.modules` 直接暴露已加载模块字典（完全内省，无封装）；Go 没有运行时类加载（编译期固定，无需内省）；Node.js 的 `require.cache` 暴露已加载模块。**只有 Java 选择了「强封装 + 有限官方内省 API」**，迫使框架作者走反射。这是 Java 的设计哲学决定的--安全优先于可诊断性。


## 五、三个内省维度--机制与边界

confucius-commons 提供了三个维度的 ClassLoader 内省能力。每个维度有特定的机制和边界，理解它们才能正确使用。

### 5.1 维度一:查询单个类的加载状态（`findLoadedClass`）

**机制**：反射调用 `ClassLoader.findLoadedClass(String)`（protected 方法）。`getDeclaredMethod` 获取 Method 对象（不能用 `getMethod`，因为 `findLoadedClass` 不是 public），`setAccessible(true)` 绕过 protected 限制。Method 对象缓存为 `static final`--反射查找只执行一次，后续 `invoke` 调用经 JIT 优化后开销接近直接调用。

**为什么用 `getDeclaredMethod` 而非 `getMethod`**：`getMethod` 只返回 public 方法（含继承的）；`getDeclaredMethod` 返回当前类声明的所有方法（含 protected/private）。`findLoadedClass` 是 protected，必须用 `getDeclaredMethod`。

**边界**：`findLoadedClass` 的方法签名从 JDK 1.2 到 JDK 21 没变过，`setAccessible(true)` 绕过 protected 仍合法（`ClassLoader` 是 `java.lang` 公开类，不受 `--illegal-access` 限制）。这是三个维度中**唯一在 JDK 11+ 上完全可用**的。非 HotSpot JVM（IBM J9、Azul Zing）可能签名不同--`jvmUnsupportedOperationException` 处理这个降级。

### 5.2 维度二:查询所有已加载类（`classes` 字段）

**机制**：反射读取 `ClassLoader.classes` 私有字段（HotSpot JDK 6-8 中是 `Vector<Class<?>>`，因为类加载多线程，需要 synchronized 集合）。返回不可变 `Set<Class<?>>`。

**为什么 `classes` 是 `Vector`**：类加载是并发的（不同线程同时加载不同类），`Vector` 是 JDK 6 时代最简单的线程安全集合。JDK 9 后内部改用了更高效的并发结构。

**边界（JDK 11+ 彻底失效）**：JDK 9 Jigsaw 模块系统（JEP 261）完全重写了 ClassLoader。旧的 `URLClassLoader` + `Vector<Class<?>> classes` 被 `jdk.internal.loader.BuiltinClassLoader` 替代，后者用模块系统内部的 `ModuleLayer` + `Configuration` 跟踪类。**`classes` 字段在 JDK 11+ 不再存在**--反射读取抛 `IllegalArgumentException`，被 `jvmUnsupportedOperationException` 捕获，异常消息包含 JVM 厂商和版本信息便于排障。

**JDK 11+ 替代方案**：`Instrumentation.getAllLoadedClasses()`（官方 API，跨所有 ClassLoader，但要求 javaagent 启动）或 `ClassLoadingMXBean.getLoadedClassCount()`（JMX，只给数量不给类名）。

### 5.3 维度三:沿 ClassLoader 父链查询（`getInheritableClassLoaders`）

**机制**：从当前 ClassLoader 出发，沿 `getParent()` 一直走到 Bootstrap（`getParent()` 返回 null），收集整条父链。然后对链上每个 ClassLoader 调 `findLoadedClass`，第一个找到的胜出。

**为什么需要沿父链查**：在 Tomcat 中，`spring-core.jar` 在 `tomcat/lib`（AppClassLoader 加载），但 `WebAppClassLoader` 中的类也依赖它。只查 `WebAppClassLoader.findLoadedClass("org.springframework.SpringVersion")` 返回 null--Spring 不在 WebAppCL 中，在父 CL（AppCL）中。沿父链查才能得到正确答案。

**边界**：`getParent()` 遍历不受 JDK 版本影响（只是沿引用链走，不关心具体类名）。但 JDK 8 的 `ExtClassLoader` 在 JDK 11 变成了 `PlatformClassLoader`（不再加载 `jre/lib/ext`，改加载平台模块）--排查问题时需要知道中间层叫法的变迁。这个维度在 JDK 11+ 上完全可用。

### 5.4 三个维度的 JDK 版本兼容性总结

| 维度 | JDK 6-8 | JDK 11+ | 替代方案 |
|---|---|---|---|
| `findLoadedClass` 反射 | ✅ | ✅ | 不需要改 |
| `classes` 字段反射 | ✅ | ❌（字段消失） | `Instrumentation.getAllLoadedClasses()` |
| 父链遍历 | ✅ | ✅ | 不需要改 |


## 六、生产案例分析——三个真实场景

### 6.1 场景一：ClassLoader 泄漏诊断

**现象**：一个 Tomcat 应用运行数天后 OOM。heap dump 显示大量类被加载，Metaspace 耗尽。

**排查方法论**（三步诊断：计数 -> 列表 -> 父链查询）：

```java
// 第1步：统计每个 WebAppClassLoader 加载了多少类
for (WebAppClassLoader cl : getAllWebAppClassLoaders()) {
    int count = ClassLoaderUtils.getLoadedClassCount();  // JMX 查询（公开 API）
    System.out.printf("WebApp[%s]: %d classes loaded%n", cl.getContextName(), count);
}

// 第2步：如果某个 WebApp 的类数量异常高，列出它加载的类（前 10 个）
if (count > EXPECTED_MAX) {
    Set<Class<?>> classes = ClassLoaderUtils.getLoadedClasses(cl);  // 反射查私有字段
    classes.stream().limit(10).forEach(c -> System.out.println(c.getName()));
}

// 第3步：确认关键类是否在正确的 ClassLoader 中
Class<?> springClass = ClassLoaderUtils.findLoadedClass(cl, "org.springframework.SpringVersion");
if (springClass == null) {
    // Spring 不在 WebAppClassLoader 中，在父 ClassLoader 中——正常
}
```

**如果没有内省工具**，你需要用 JMX 或 `-verbose:class` 参数重启应用来分析，定位耗时长得多。

### 6.2 场景二：类重复加载检测

**现象**：相同的类被两个不同的 ClassLoader 加载了两次（如 `guava-30.0.jar` 既在 `tomcat/lib` 又在 `webapp/WEB-INF/lib`）。

**检测方法**：

```java
Map<ClassLoader, Set<Class<?>>> map = ClassLoaderUtils.getAllLoadedClassesMap(cl);
// 输出：AppCL 有 Guava 的类，WebAppCL 也有 Guava 的类 → 重复加载！
```

### 6.3 场景三：跨 JVM 平台的兼容性

**场景**：你开发的应用在 Oracle JDK 8 上运行正常，迁移到 Amazon Corretto 11 后启动崩溃。

**调试**：confucius-commons 在反射失败时抛出的 `UnsupportedOperationException` 包含 `JAVA_VENDOR` 和 `JAVA_VERSION`，你不需要开 debug 就能在日志中看到：

```
UnsupportedOperationException: Current JVM[ Implementation : Amazon.com Inc. , 
Version : 11.0.15 ] does not supported !
```

立刻定位到「Corretto 11 上 ClassLoader.classes 字段不存在」——而不是花几个小时猜「是不是反射权限问题？是不是模块系统限制？」

---

## 七、方法论总结——框架作者如何看待「安全 vs 诊断」的 tradeoff

JDK 设计者选择了**安全优先**——他们默认假设外部代码是恶意的，你把内部状态锁住就行了。

框架作者选择了**诊断优先**——但在绕过安全限制时，必须遵守一个底线：**不要静默失败**。confucius-commons 的做法：

1. **反射失败 = 立即崩溃**（`jvmUnsupportedOperationException`），不让框架在不知道原因的状态下继续运行
2. **异常消息包含 JVM 元数据**（供应商 + 版本），让排障更高效
3. **降级路径是显式的**（不是静默的 `return null`，而是抛明确的 `UnsupportedOperationException`）

这三点是**所有框架代码在绕过 JDK 限制时应该遵守的准则**——不是 confucius-commons 独有的。

---

---
## 八、JDK 7+ 的并行类加载——`getClassLoadingLock`

### 8.1 历史问题

JDK 6 及之前，`loadClass` 方法是 `synchronized` 的——同一时间只有一个线程能加载类。在多线程应用中，这成为严重的性能瓶颈。

### 8.2 JDK 7 的解决方案

JDK 7 引入「并行类加载」机制。ClassLoader 子类可以声明自己是并行安全的：

```java
public class MyClassLoader extends ClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();  // ← 声明：我可以并行加载
    }

    protected Object getClassLoadingLock(String className) {
        // JDK 7+：并行 ClassLoader 中，每个类名有自己的锁
        // 不同类名互不影响，多个线程可同时加载不同的类
        return super.getClassLoadingLock(className);
    }
}
```

`getClassLoadingLock(className)` 为每个唯一的类名返回一个锁对象。线程 A 加载 `Foo.class` 和线程 B 加载 `Bar.class` 使用不同的锁——互不阻塞。

### 8.3 为什么框架作者需要知道这个

如果你手写了一个 ClassLoader，没有调用 `registerAsParallelCapable()`，JDK 7+ 会**默认降级为串行加载**——整个 ClassLoader 的所有 `loadClass` 调用被锁在同一个对象上。当一个线程在加载一个大 jar 包中的类时，所有其他线程都必须等待。

confucius-commons 的 `ClassLoaderUtils` 没有涉及 `getClassLoadingLock`——因为在写它的 JDK 6 时代，并行类加载还不存在。但如果你今天在 JDK 11+ 上写框架，你的 ClassLoader 必须声明为并行安全。

---

## 九、反向思考——什么时候**不应该**绕过 JDK 的限制

confucius-commons 展示了「怎么绕过」，但框架作者也应该知道「什么时候不该绕过」：

1. **标准 JMX API 够用时**：`ClassLoadingMXBean.getLoadedClassCount()` 给你类数量统计——如果你只需要「当前加载了多少类」，不需要反射访问 `ClassLoader.classes` 字段。

2. **`-verbose:class` 参数够用时**：如果你是在排查启动问题，JVM 的 `-verbose:class` 参数会打印每一行的类加载日志——不需要你自己写代码查。

3. **准备在生产用 `--illegal-access=deny` 时**：JDK 16+ 默认阻止对 JDK 内部的反射。如果你的框架是给别人用的，依赖 `ClassLoader.classes` 意味着用户在 JDK 17 上必须额外配置 JVM 参数——降低了框架的可用性。

4. **考虑升级到 `java.lang.instrument`**：`Instrumentation.getAllLoadedClasses()` 是官方 API，不需要反射——但它要求 javaagent 方式启动。这是功能与侵入性的 tradeoff：你可以选择「不需要反射，但需要用户在启动时加 -javaagent」。

**决策框架**：

```
你需要什么信息？
├── 类数量统计  → ClassLoadingMXBean（官方 API，无需反射）
├── 单个类的加载状态 → findLoadedClass 反射（当前唯一方案）
├── 所有已加载类的列表 → Instrumentation.getAllLoadedClasses()（JDK 9+ 官方 API）
└── 类文件来源 → ProtectionDomain.getCodeSource()（官方 API）
```

---

## 十、新增生产案例：排查「类已加载但找不到」

### 现象

```java
Exception in thread "main" java.lang.NoClassDefFoundError: com/example/Foo
```

应用启动时报这个错。你检查了 pom.xml，依赖确实存在。jar 包也在 classpath 上。但就是找不到。

### 排查方法论

```java
// 步骤1：沿 ClassLoader 链查这个类到底被谁加载了
Class<?> foo = ClassLoaderUtils.findLoadedClass(
    Thread.currentThread().getContextClassLoader(),
    "com.example.Foo"
);
System.out.println("Loaded by: " + (foo != null ? foo.getClassLoader() : "NOT LOADED"));

// 步骤2：如果没被加载——查这个类声称在哪个 jar 里
// ProtectionDomain.getCodeSource() 告诉你类来自哪个 jar——官方 API
URL url = ClassPathUtils.getRuntimeClassLocation("com.example.Foo");
System.out.println("Should be in: " + url);

// 步骤3：检查这个 jar 是不是真的在 classpath 上
Set<String> classNames = ClassUtils.getClassNamesInClassPath(url.getFile(), false);
System.out.println(classNames.contains("com.example.Foo") ? "YES in classpath" : "NO in classpath");

// 可能的根因：
// - jar 在 classpath 但 ClassLoader 的搜索路径不包含它
// - 类存在于 jar 但加载时抛了 Error（ClassLoader 缓存了失败的尝试）
// - 不同版本的类在父 ClassLoader 和子 ClassLoader 中冲突
```

### 如果没有内省工具

你只能加 `-verbose:class` 重启应用，在数万行日志中人工找 `com/example/Foo`。

confucius-commons 是 JDK 6 时代的产物（约 2012 年）。JDK 经历了 9（模块系统）、11（LTS）、17（LTS）、21（LTS）四个大版本，它的代码还靠谱吗？

### 7.1 `findLoadedClass` 反射——JDK 11 上完全可用

好消息：`ClassLoader.findLoadedClass(String)` 的方法签名从 JDK 6 到 JDK 21 都没有变。它仍然是 `protected`，`setAccessible(true)` 仍然能绕过。

```java
// confucius-commons 的写法在 JDK 11 上同样有效
Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
m.setAccessible(true);
// 在 JDK 11 上不会触发任何警告或错误
```

**为什么不会触发 `--illegal-access` 限制？**

JDK 9+ 的 `--illegal-access` 只限制对**JDK 内部类**（`sun.*`、`com.sun.*`、`jdk.internal.*`）的反射访问。`ClassLoader` 是 `java.lang` 包下的公开 API——即使对它的 `protected` 方法做 `setAccessible(true)` 也完全合法。

**唯一需要注意的**：如果 `classLoader` 的实际类型是 `jdk.internal.loader.BuiltinClassLoader`（JDK 11 中 AppClassLoader 的父类），调用它的方法本身不会有问题——因为你的代码是顺着 `ClassLoader` 接口调的（多态），不是直接触及内部类。

### 7.2 `ClassLoader.classes` 字段——JDK 11 上彻底失效

这是 confucius-commons 在 JDK 11+ 上**真正坏掉**的部分。

```java
// JDK 8：HotSpot 的 ClassLoader 内部有 private Vector<Class<?>> classes
// JDK 11：BuiltinClassLoader 没有这个字段

// 启动时调用 getLoadedClasses(jdk11ClassLoader)：
// → FieldUtils.readField(classLoader, "classes", true)
// → IllegalArgumentException: Field 'classes' not found
// → catch → jvmUnsupportedOperationException
// → 日志：Current JVM[ Implementation : Oracle Corporation , Version : 11.0.15 ] 
//         does not supported !
```

**为什么字段消失了？**

JDK 9 的 Jigsaw 模块系统（JEP 261）完全重写了 ClassLoader。旧的 `java.net.URLClassLoader` + `Vector<Class<?>> classes` 被 `jdk.internal.loader.BuiltinClassLoader` 替代，后者用模块系统内部的 `ModuleLayer` + `Configuration` 来跟踪类。

**JDK 11 的替代方案——用 `java.lang.instrument`**：

```java
// 需要以 javaagent 方式启动
public class ClassCounterAgent {
    public static void premain(String args, Instrumentation inst) {
        // Instrumentation.getAllLoadedClasses() 返回 JVM 中所有已加载的类
        Class<?>[] allClasses = inst.getAllLoadedClasses();
        System.out.println("Total loaded classes: " + allClasses.length);
    }
}
// 启动方式：java -javaagent:class-counter-agent.jar -jar myapp.jar
```

这比 `getLoadedClasses(classLoader)` 更强——它能跨越所有 ClassLoader，不像旧版只查单个 ClassLoader。

**不需要 javaagent 的轻量级替代——JMX**：

```java
ClassLoadingMXBean mxBean = ManagementFactory.getClassLoadingMXBean();
int loaded = mxBean.getLoadedClassCount();      // 当前已加载类总数
long total = mxBean.getTotalLoadedClassCount(); // 历史累计加载
long unloaded = mxBean.getUnloadedClassCount(); // 累计卸载（JDK 7+）
```

JMX 给了统计数字，但不给类名列表。如果你只需要「数量」而不是「哪些类」，JMX 无需 javaagent。

### 7.3 `getCallerClassName()` 的替代——JDK 9 引入 `StackWalker`

```java
// confucius-commons 的旧写法（依赖 sun.reflect.Reflection）
Class<?> caller = sun.reflect.Reflection.getCallerClass(3);

// JDK 11 的正确写法（官方 API，不依赖内部类）
Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
    .walk(frames -> frames.skip(2)  // skip self + immediate caller
                         .findFirst()
                         .map(StackWalker.StackFrame::getDeclaringClass))
    .orElse(null);
```

`StackWalker` 的优势：惰性遍历栈帧（不一次性创建整个 `StackTraceElement[]`），只在需要时消费。劣势：API 比旧 `getCallerClass(int)` 更繁琐。

### 7.4 ClassLoader 层次的变化——ExtClassLoader 退休了

```
JDK 8:
AppClassLoader → ExtClassLoader → Bootstrap(null)

JDK 11:
AppClassLoader → PlatformClassLoader → Bootstrap(null)
                  ↑ 替代 ExtClassLoader
                  不再加载 jre/lib/ext
                  改为加载平台模块（java.sql, java.xml 等）
```

`getInheritableClassLoaders()` 的代码不受影响——它只是沿 `getParent()` 走，不关心具体类名。但你在排查问题时需要知道：JDK 11 上中间那层叫 `PlatformClassLoader`，不是 `ExtClassLoader`。

### 7.5 总结——confucius-commons 的 JDK 8 → JDK 11 迁移矩阵

| 类 | 方法 | JDK 8 | JDK 11 | 替代方案 |
|---|---|---|---|---|
| ClassLoaderUtils | `findLoadedClass()` 反射 | ✅ | ✅ | 不需要改 |
| ClassLoaderUtils | `getLoadedClasses()` | ✅ | ❌ | `Instrumentation.getAllLoadedClasses()` |
| ClassLoaderUtils | `getInheritableClassLoaders()` | ✅ | ✅ | 不需要改 |
| ManagementUtils | `getCurrentProcessId()` | ✅ | ⚠️ 警告 | `ProcessHandle.current().pid()` |
| ReflectionUtils | `getCallerClassName()` (sun.reflect) | ✅ | ⚠️ 废弃 | `StackWalker` |
| UnsafeUtils | `unsafe` 获取 | ✅ | ⚠️ 需 `--add-opens` | `VarHandle` |

---

## 十一、面试要点

**Q1：「ClassLoader 的双亲委托模型具体是怎么实现的？如果你写了一个自定义 ClassLoader，不遵循 parent-delegation，会有什么风险？」**

答案：`loadClass(name)` 内部逻辑——①先调 `findLoadedClass(name)` 检查是否已加载；②如果没加载，调 `parent.loadClass(name)` 委托父 CL；③如果父 CL 抛出 `ClassNotFoundException`，调自己的 `findClass(name)` 从字节码加载。如果不遵循这个模型（如 Tomcat WebAppCL 先自己加载后委托父 CL）：可能加载到不同版本的同一个类 → `NoSuchMethodError`、`ClassCastException`；破坏类型安全隔离——恶意类可以伪装成 JDK 核心类被加载。

追问：「Tomcat 的 WebAppClassLoader 为什么不遵循标准 parent-delegation？它的委托顺序是什么？」
回答：Tomcat 用了「先自己、后父 CL」的逆序——先在自己的 `WEB-INF/classes` 和 `WEB-INF/lib` 中加载，找不到才委托父 CL。原因：servlet 规范要求每个 webapp 的类和 lib 是优先的——否则 tomcat/lib 中的 Spring jar 会优先于 webapp 中的版本，webapp 无法选择自己的框架版本。

**Q2：「同一个全限定类名被两个不同的 ClassLoader 分别加载，JVM 认为它们是同一个类吗？为什么？」**

答案：不是同一个类。JVM 用 `(ClassLoader, 全限定类名)` 二元组作为类的唯一标识。「同一 ClassLoader 同一类名」= 同一类。「不同 ClassLoader 同一类名」= 不同类型。如果它们被认为是同一个类，ClassLoader B 中的恶意类就可以访问 ClassLoader A 中受保护的数据——类型安全完全摧毁。

追问：「`Class.forName("java.lang.String")` 返回的是哪个 ClassLoader 加载的 String？`String.class` 呢？」
回答：`Class.forName(className)` 使用调用者的 ClassLoader。`String.class` 是类字面量——它在编译时就确定，等价于调用者 ClassLoader 的 `loadClass`。两者的结果通常是同一个——都是 BootstrapCL 加载的 String。但如果调用者在自定义 CL 中——可能不同。

**Q3：「你在写一个框架，需要在扫描 classpath 时知道有哪些类，但又不想触发任何类的静态初始化。怎么实现？」**

答案：遍历 jar 条目文件名（`com/example/Foo.class → com.example.Foo`），不调用 `Class.forName()` 或 `loadClass()`。只用 `findLoadedClass(className)` 查询是否已加载——这个方法只查 ClassLoader 内部的 `classes` 列表，不触发加载、链接、初始化。confucius-commons 的 `SimpleClassScanner` 的 `requireLoad=false` 选项就是这个用途。

追问：「如果 `findLoadedClass` 是 protected 的，外部代码怎么调？」
答案：反射。`ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class)` + `setAccessible(true)`。Spring Framework 内部也是同样做法。代价是依赖 HotSpot 内部实现——但 `findLoadedClass` 从 JDK 1.2 到 JDK 21 都没有变过。

**Q4：「NoClassDefFoundError 和 ClassNotFoundException 有什么区别？你怎么排查定位缺失的类在哪个 jar？」**

答案：`ClassNotFoundException`——类加载时找不到字节码（如 `Class.forName("com.mysql.Driver")` 拼写错误）。`NoClassDefFoundError`——类加载时找到了类，但初始化时依赖的另一个类找不到或版本不匹配（如 A 类编译时依赖 B 类，运行时 B 类不存在或版本不同）。

排查方法：① `ClassLoaderUtils.findLoadedClass(cl, className)` 检查类是否加载；② `ClassPathUtils.getRuntimeClassLocation(className)` 获取类文件位置（通过 `ProtectionDomain.getCodeSource()`）；③ 如果不是 HotSpot JVM，降级到 `loadClass(className)` 试加载并检查异常；④ confucius-commons 的 `jvmUnsupportedOperationException` 在异常消息中包含 JVM 厂商和版本——帮助确认是否是 JDK 迁移导致的兼容性问题。
