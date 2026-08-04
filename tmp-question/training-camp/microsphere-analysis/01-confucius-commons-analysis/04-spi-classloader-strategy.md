# SPI 覆盖策略 —— 利用 ClassLoader 层次控制「谁的实现生效」

> 源码对应：`ServiceLoaderUtils.java`（114行）
> JDK 11 对比：`module-info.java` 的 `provides...with` + microsphere-java 的 `Prioritized`

**SPI 的「谁先谁后」是个经典问题，有两种根本不同的解法**：依赖 ClassLoader 层次（confucius-commons 的 `loadFirst/lastService`）vs 显式优先级（microsphere-java 的 `Prioritized` 接口）。本文讲第一种--它的局限（JDK 9 模块系统下 CL 层次假设被打破）催生了第二种（[02-microsphere §4 SPI+Prioritized](../02-microsphere-java-analysis/04-spi-prioritized.md)）。理解本文的局限是理解 microsphere Prioritized 存在意义的前提。

---

## 一、SPI 加载顺序的本质——不是 JDK Bug，是 ClassLoader 委托模型的副产品

### 1.1 ServiceLoader 内部做了什么

```java
// ServiceLoader.load() 内部调用链（简化）：
ServiceLoader.load(MyService.class, classLoader)
  → LazyIterator.hasNext()
    → ClassLoader.getResources("META-INF/services/com.example.MyService")
      // ↑ 返回 ALL matching resources across the CL hierarchy
      //   顺序：父 CL 先，子 CL 后（parent-delegation）
    → 逐行读取每个 resource 文件
    → Class.forName(className).newInstance()  // 实例化
    → 返回 Iterator
```

关键步骤是 `ClassLoader.getResources(String)`。它在 JDK 中的实现**没有**对返回的资源做排序——返回顺序就是 ClassLoader 搜索路径的顺序。

### 1.2 `getResources()` 的返回顺序

`ClassLoader.getResources(String)` 的 Javadoc 原文：

> "Finds all the resources with the given name. A resource is some data that can be accessed by class code in a way that is independent of the location."

关于顺序，它指向 `getResource(String)`：

> "The search order is described in the documentation for **getResource(String)**."

而 `getResource(String)` 的 Javadoc：

> "**Delegates to the parent class loader first.** If the parent is not null, it searches the parent first."

所以顺序是确定的：**父 ClassLoader 的资源先被找到，子 ClassLoader 的资源后被追加**。

### 1.3 实际验证——Tomcat 中的 ClassLoader 层次

```
BootstrapClassLoader (加载 rt.jar)
    ↑ parent
PlatformClassLoader (JDK 11+) / ExtClassLoader (JDK 8) (加载 jre/lib/ext)
    ↑ parent
AppClassLoader (加载 classpath: tomcat/bin/bootstrap.jar, tomcat/lib/*.jar)
    ↑ parent
WebAppClassLoader-1 (加载 webapp-1/WEB-INF/classes + WEB-INF/lib/*.jar)
WebAppClassLoader-2 (加载 webapp-2/WEB-INF/classes + WEB-INF/lib/*.jar)
    （多个 WebAppClassLoader 之间没有父子关系，各自独立）
```

`getResources("META-INF/services/...")` 在 `WebAppClassLoader-1` 上的搜索路径：

```
1. BootstrapClassLoader 的搜索路径 → 找到 JDK 内置的 SPI 实现
2. PlatformClassLoader 的路径 → 找到 jre/lib/ext 中的实现
3. AppClassLoader 的路径 → 找到 tomcat/lib 中的实现
4. WebAppClassLoader-1 的路径 → 找到 webapp-1/WEB-INF/lib 中的实现
```

返回的 `Enumeration<URL>` 中，URL 的顺序 = 这个搜索顺序。所以 **BootstrapCL 的实现排在 `list.get(0)`，WebAppCL 的实现排在 `list.get(size-1)`**。


## 二、源码解析——两行代码实现两种策略

### 2.1 核心——loadFirstService 和 loadLastService

```java
// ServiceLoaderUtils.java:88-91
public static <T> T loadFirstService(ClassLoader cl, Class<T> type) {
    List<T> serviceList = loadServicesList0(cl, type);
    return serviceList.get(0);  // 父 CL 的排在前面 → 返回父 CL 的实现
}

// ServiceLoaderUtils.java:109-112
public static <T> T loadLastService(ClassLoader cl, Class<T> type) {
    List<T> serviceList = loadServicesList0(cl, type);
    return serviceList.get(serviceList.size() - 1);  // 子 CL 的排在后面 → 返回子 CL 的实现
}
```

### 2.2 底层——loadServicesList0

```java
// ServiceLoaderUtils.java:57-70
private static <T> List<T> loadServicesList0(ClassLoader cl, Class<T> type) {
    ServiceLoader<T> serviceLoader = ServiceLoader.load(type, cl);
    List<T> serviceList = new ArrayList<>();
    for (T service : serviceLoader) {
        serviceList.add(service);
    }
    
    if (serviceList.isEmpty()) {
        // 一个 SPI 实现都没有 → 抛明确异常
        throw new IllegalArgumentException(
            String.format("No Service interface[type : %s] implementation was defined in " +
                "service loader configuration file[/META-INF/services/%s] under ClassLoader[%s]",
                type.getName(), type.getName(), cl));
    }
    
    return serviceList;
}
```

**注意异常消息**：包含了三个关键信息——接口名、配置文件路径、ClassLoader。如果 SPI 加载失败，这个异常消息能让你直接定位「在哪个 ClassLoader 下找不到」，不需要额外排查。

### 2.3 设计意图

loadFirstService 的 Javadoc（第82-86行）：

> "Design Purpose: 利用 ClassLoader 的层次性，各层次的 ClassLoader 将会能访问其 class path 下的配置文件。那么，覆盖 ClassLoader 的 class path 下的配置文件的第一个实现类，从而提供覆盖实现类机制。"

loadLastService 的 Javadoc（第96-98行）：

> "设计目的：利用 ClassLoader 的层次性，一旦较高层次（这里最高层次为 Bootstrap ClassLoader）双亲的 ClassLoader 中加载了配置文件的最后一个实现类的话，低层次的 ClassLoader 将无法覆盖前定义。"

**这两个 Javadoc 就是完整的设计文档**。loadFirstService = 「内核不可被覆盖」（Bootstrap CL 的实现在最前面，list.get(0)永远是它）。loadLastService = 「应用可以覆盖内核」（WebAppCL 的实现在最后面，list.get(size-1)是它）。


## 三、两种策略的真实场景推演

### 3.1 loadFirstService —— 内核优先

```
场景：JDBC Driver 管理

JDK 6 之前，Class.forName("com.mysql.jdbc.Driver") 是唯一驱动注册方式。
JDK 6 引入 ServiceLoader 自动加载 Driver。

ClassLoader 层次：
  BootstrapCL:  java.sql.Driver 的 SPI 文件 → "sun.jdbc.odbc.JdbcOdbcDriver"（JDK内置）
  AppCL:        mysql-connector.jar 的 SPI 文件 → "com.mysql.cj.jdbc.Driver"（应用依赖）

loadFirstService(AppCL, Driver.class) → sun.jdbc.odbc.JdbcOdbcDriver
  → JDK 内置驱动优先，应用不能替换
  → 保证了兼容性——即使应用引入了更新的 MySQL 驱动，JDK 内置功能不受影响
```

### 3.2 loadLastService —— 应用优先

```
场景：SLF4J 日志绑定

ClassLoader 层次：
  AppCL (framework.jar):  SPI 文件 → "org.slf4j.simple.SimpleServiceProvider"（框架默认）
  WebAppCL (my-app.jar):  SPI 文件 → "com.myapp.CustomLoggerServiceProvider"（应用自定义）

loadLastService(WebAppCL, SLF4JServiceProvider.class) → CustomLoggerServiceProvider
  → 用户自定义覆盖框架默认
  → 不需要改任何配置文件——只要把自定义实现放在 WebAppCL 的加载路径中
```

### 3.3 什么时候不应该用这两种策略

| 场景 | 问题 | 应该用什么 |
|---|---|---|
| 需要**多个** SPI 实现同时生效 | first/last 只返回一个 | 循环遍历 ServiceLoader |
| 类路径不可控（给第三方用的 SDK） | 你无法控制用户把 jar 放哪个 CL | microsphere-java 的 `Prioritized` 排序 |
| 容器环境 ClassLoader 层次打破 parent-delegation | OSGi、模块系统的类加载顺序可能不同 | `@Priority` 或 Ordered 接口 |


## 四、JDK 9 模块系统——当 `loadFirstService` 的假设被打破

### 4.1 模块系统中的 SPI 声明方式变了

JDK 8 的唯一方式：`META-INF/services/com.example.PaymentService` 文本文件。

JDK 9 新增方式：`module-info.java` 的 `provides...with` 语法：

```java
// JDK 8: META-INF/services/ 文件
// 文件内容（一行一个实现类全限定名）：
//   com.example.impl.WechatPayService
//   com.example.impl.AliPayService

// JDK 9+: module-info.java（META-INF/services/ 文件仍然有效）
module com.example.provider {
    requires com.example.spi;
    provides com.example.spi.PaymentService with 
        com.example.impl.WechatPayService,   // ← 顺序：第1个
        com.example.impl.AliPayService;      // ← 顺序：第2个
}
```

`ServiceLoader.load()` 在 JDK 9+ 上**同时检查两种声明方式**——模块声明和 META-INF/services 文件。

### 4.2 provides...with 声明的顺序是否确定？

**是的，确定。** 但它的顺序不等于 ClassLoader.getResources() 的返回顺序。

```java
// 在模块系统下，ServiceLoader.load() 的行为：
// 1. 先加载当前 ModuleLayer 的 provides...with 声明
// 2. 再加载父 ModuleLayer 的声明
// 3. 最后加载未命名模块（classpath 上的 META-INF/services/）

// confucius-commons 的 loadFirst/last 依赖的是「父先子后」的 ClassLoader 顺序
// 但模块系统的顺序是「当前模块先、父模块后」——刚好反向！
```

**这意味着什么？** 如果你的应用从 JDK 8 迁移到 JDK 11 并启用了模块系统（`module-info.java`），`loadFirstService` 的行为可能反转——原来在父 CL 中的框架默认实现先被返回，现在可能变成子模块中的应用实现先被返回。

### 4.3 模块系统对 confucius-commons 的实际影响

```java
// 场景：框架使用 loadFirstService 确保「JDBC Driver 先用 JDK 内置的」
// JDK 8 (classpath 模式):
//   BootstrapCL: sun.jdbc.odbc.JdbcOdbcDriver → list[0] → loadFirstService 返回
//   AppCL: com.mysql.cj.jdbc.Driver → list[1]
//   ✓ 符合预期：JDK 内置优先

// JDK 11 (module-path 模式):
//   Module java.sql (Bootstrap layer): sun.jdbc.odbc.JdbcOdbcDriver  → 可能排在第2位
//   Module com.mysql (App layer): com.mysql.cj.jdbc.Driver → 可能排在第1位
//   ✗ 不符合预期！模块系统的顺序可能不同

// 结论：loadFirst/last 在模块系统下不一定可靠
```

### 4.4 推荐方案——用 microsphere-java 的 Prioritized 替代

```java
// confucius-commons: 依赖 ClassLoader parent-delegation 顺序（JDK 9+ 可能不成立）
MyService service = ServiceLoaderUtils.loadLastService(cl, MyService.class);

// microsphere-java: 依赖显式优先级——不依赖任何加载顺序
List<MyService> services = ServiceLoaderUtils.loadServicesList(MyService.class, cl);
services.sort(Comparator.comparingInt(Prioritized::getPriority).reversed());
MyService service = services.get(0);  // 优先级最高的

// 实现方：
public class CustomLoggerProvider implements SLF4JServiceProvider, Prioritized {
    public int getPriority() { return MAX_PRIORITY; }  // 显式声明"我最高优先级"
    // ...
}
```

**优先级排序的优势**：

1. **跨 JVM 可移植**：不依赖 HotSpot 的 `getResources()` 实现细节
2. **跨部署方式可移植**：classpath 模式 vs module-path 模式 vs ClassLoader 层次——都不影响
3. **显式契约**：每个实现声明自己的优先级——代码审查时一眼可见
4. **可独立开关**：`MIN_PRIORITY` = 禁用（但不是删除），`NORMAL_PRIORITY` = 默认，`MAX_PRIORITY` = 强制覆盖


## 五、生产案例——排查 SPI 实现没被加载

### 5.1 现象

```java
// 框架代码
PaymentService service = ServiceLoaderUtils.loadFirstService(cl, PaymentService.class);
// → IllegalArgumentException: No Service interface[type : com.example.PaymentService] 
//   implementation was defined...
```

明明 jar 里有 `META-INF/services/com.example.PaymentService` 文件，但 `loadFirstService` 返回「没找到实现」。

### 5.2 排查步骤

**第1步：确认 SPI 文件存在且正确**

```bash
jar tf my-app.jar | grep META-INF/services/com.example.PaymentService
```

**第2步：定位是哪个 CL 找不到**

```java
// 异常消息中包含了 ClassLoader：
// "under ClassLoader[sun.misc.Launcher$AppClassLoader@5c647e05]"
// 确认你传入的 CL 和实际加载 SPI 实现的 CL 是同一个
```

**第3步：列出所有 CL 层次中的 SPI 文件**

```java
// confucius-commons 的 ClassLoaderUtils.getResources 可以看到所有层次的 SPI 文件
Set<URL> urls = ClassLoaderUtils.getResources(cl, "META-INF/services/com.example.PaymentService");
// 依次打印每个 URL，确认哪个 CL 的 jar 中有 SPI 文件
for (URL url : urls) {
    System.out.println("Found at: " + url);
}
```

**第4步：找错**。最常见的问题：
- SPI 文件放错 jar（放在 B.jar 但实际用的是 A.jar 的 CL）
- CL 层次错误（SPI 文件在 WebAppCL 的 jar 中但代码用的是 AppCL）
- 类加载失败（SPI 实现类依赖的 jar 不在 CL 的搜索路径中）

### 5.3 终极陷阱——类加载失败不会报错

```java
// ServiceLoader 的行为：
// 如果 SPI 实现类在实例化时抛异常（如 ClassNotFoundException），
// ServiceLoader 不会中断——它默默地跳过这个实现，继续下一个。
// 只有等它返回的 Iterator 不包含任何元素时，你才知道 "实现没被加载"。
```

所以即使 `META-INF/services/...` 文件存在，`loadFirstService` 也可能因为 SPI 实现类的构造器抛异常而报「没找到实现」。


## 六、面试要点

**Q1：「ServiceLoader 加载的顺序是确定的吗？不是的话，怎么控制？」**

答案：不确定——取决于 `ClassLoader.getResources()` 的返回顺序（不同 JVM/文件系统可能不同）。三种控制方式：① confucius-commons 的 `loadFirstService/loadLastService`——利用 parent-delegation 的父先子后顺序；② `ServiceLoader.load().stream().sorted()`——运行时排序；③ microsphere-java 的 `Prioritized`——每个实现声明自己的优先级。

追问：「如果 loadFirstService 的假设（父先子后）不成立了，会发生什么？在什么情况下不成立？」
回答：OSGi 环境（bundle ClassLoader 不遵循 parent-delegation）、JDK 9+ 自定义 ModuleLayer、自定义 ClassLoader 覆盖了 `getResources()` 的返回顺序。

**Q2：「你设计了一个框架，SPI 默认实现由框架提供，但允许用户替换。怎么实现？」**

答案：用 `loadLastService`。框架的默认 SPI 文件放在 framework.jar（AppCL），用户的 SPI 文件放在 my-app.jar（WebAppCL/TCCL）。`loadLastService(tccl, type)` 返回用户的自定义实现。

追问：「如果用户没有提供自定义实现，loadLastService 返回什么？」
答案：返回框架的默认实现——因为默认在 AppCL 中，唯一可用的实现就是它。`loadLastService` = `list.get(size-1)`，当 size=1 时，`get(0)` 和 `get(size-1)` 是同一个。

**Q3：「有什么方式可以不修改代码、不修改配置文件，把框架默认实现替换成你自己的实现？」**

答案：利用 ClassLoader 层次。把自定义实现放在 WebAppCL/TCCL 的加载路径中，用 `loadLastService(tccl, type)` 加载——因为自定义实现排在 ClassLoader 搜索路径的最后，会被 `loadLastService` 返回。不需要删除框架的默认 SPI 文件。
