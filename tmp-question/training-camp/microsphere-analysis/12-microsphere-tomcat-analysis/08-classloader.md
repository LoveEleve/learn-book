# 第 7 篇：类加载机制

> 第 4 篇中，`loadServlet()` 用 `InstanceManager.newInstance(servletClass)` 创建 Servlet 实例，
> 反射用的类加载器是 **WebappClassLoader**（`loadClassMaybePrivileged(className, classLoader)`）。
> 本篇回答核心问题：**Tomcat 的类加载器体系是什么？Webapp 类加载器如何打破双亲委派？
> Spring Boot 嵌入式为什么又把它"反过来"了？**
>
> **源码范围**：
> - `org.apache.catalina.loader.WebappClassLoaderBase`（2660 行，核心）
> - `org.apache.catalina.loader.ParallelWebappClassLoader`（61 行）
> - `org.apache.catalina.loader.WebappLoader`（591 行，类加载器的装配器）
> - Spring Boot：`TomcatEmbeddedWebappClassLoader`（129 行）
> - Spring Boot：`LaunchedURLClassLoader`（Fat Jar 类加载器）
> - `org.apache.catalina.startup.Bootstrap`（Standalone 类加载器层次）
>
> **本篇定位**：Tomcat 核心层 + Spring Boot 特有的反向设计。

---

## 目录

1. [双亲委派模型回顾](#1-双亲委派模型回顾)
2. [Tomcat 的类加载器层次（Standalone）](#2-tomcat-的类加载器层次standalone)
3. [WebappClassLoaderBase：打破双亲委派](#3-webappclassloaderbase打破双亲委派)
4. [filter()：包级隔离的精细控制](#4-filter包级隔离的精细控制)
5. [WebappLoader：类加载器的装配器](#5-webapploader类加载器的装配器)
6. [Spring Boot 嵌入式：TomcatEmbeddedWebappClassLoader 反向设计](#6-spring-boot-嵌入式tomcatembeddedwebappclassloader-反向设计)
7. [Fat Jar 与 LaunchedURLClassLoader](#7-fat-jar-与-launchedurlclassloader)
8. [本篇小结与面试要点](#8-本篇小结与面试要点)

---

## 1. 双亲委派模型回顾

### 1.1 JDK 默认：父先加载

标准 `ClassLoader.loadClass()` 的逻辑（父优先）：

```
loadClass(name)
  ├─ findLoadedClass(name)       ← 已加载？直接返回
  ├─ parent.loadClass(name)      ← ★ 先问父加载器
  ├─ findClass(name)             ← 父加载不到才自己找
```

**好处**：核心类库（`java.lang.*`）永远不会被应用覆盖，保证一致性。

### 1.2 双亲委派在 Web 应用中的"问题"

Web 应用需要**隔离与覆盖**：

1. **隔离**：两个应用都带 `com.example.util.Utils`，各自用自己的版本，互不干扰
2. **覆盖**：应用想用自己的第三方库（如自己的 `log4j`），而不是容器的

双亲委派的"父先加载"会导致：父加载器有 `log4j` → 应用永远用自己的不行。

**Tomcat 的解法**：Webapp 类加载器**打破双亲委派**——"先自己找，找不到再给父"。

---

## 2. Tomcat 的类加载器层次（Standalone）

### 2.1 Bootstrap.initClassLoaders()

**源码**：`org/apache/catalina/startup/Bootstrap.java:140-156`

```java
private void initClassLoaders() {
    try {
        commonLoader = createClassLoader("common", null);     // 根
        if (commonLoader == null) {
            commonLoader = this.getClass().getClassLoader();
        }
        catalinaLoader = createClassLoader("server", commonLoader);  // 容器
        sharedLoader = createClassLoader("shared", commonLoader);    // 共享
    } catch (Throwable t) {
        ...
    }
}
```

### 2.2 完整层次（Standalone 运行时）

```
Bootstrap（JVM 启动）
  │
  ├─ Bootstrap ClassLoader        （JVM 内置：java.*）
  │     │
  │     └─ System/App ClassLoader （classpath：conf/logging.properties 等）
  │           │
  │           └─ Common ClassLoader   （conf/catalina.properties: common.loader）
  │                 │
  │                 ├─ Catalina ClassLoader （server.loader：容器自身）
  │                 │
  │                 └─ Shared ClassLoader （shared.loader：应用共享）
  │                       └─ Webapp ClassLoader（每个应用一个，★ 父 = Shared）
```

**关键**：
- **Catalina 与 Shared 是兄弟**（都继承 Common）——容器类库与应用共享类库**隔离**
- **每个 Web 应用一个 WebappClassLoader**——应用间**隔离**
- 但 Common 下的库（如 `tomcat-jdbc`）三者都可见

**★ Webapp 的父为什么是 Shared 而不是 Catalina？**（传播链，`Bootstrap.java:272`）：

```
Bootstrap.initClassLoaders()
  ├─ commonLoader = createClassLoader("common", null)        [Bootstrap.java:142]
  ├─ catalinaLoader = createClassLoader("server", commonLoader)  [147]
  └─ sharedLoader = createClassLoader("shared", commonLoader)    [148]
       │
       ▼
Catalina.setParentClassLoader(sharedLoader)          [Bootstrap.java:268-272]
       │
       ▼
解析 server.xml：SetParentClassLoaderRule 把 sharedLoader 设给 Engine
       │                                        [Catalina.java:488-489]
       ▼
StandardContext.getParentClassLoader()
  → Host.getParentClassLoader() → Engine.getParentClassLoader()
  → sharedLoader                                    [ContainerBase.java:463-469 向上传播]
       │
       ▼
WebappLoader.createClassLoader()
  → new ParallelWebappClassLoader(context.getParentClassLoader())   [WebappLoader.java:413]
```

所以：**容器（Catalina）用 catalinaLoader 加载，应用用 WebappClassLoader（父 = sharedLoader）加载**——
应用**看不到** Catalina 的类（兄弟隔离），但**看得到** Shared 和 Common 的类。

### 2.3 嵌入式：层次大幅简化

Spring Boot 嵌入式**没有 Bootstrap 的 initClassLoaders**——整个 Tomcat 由
`LaunchedURLClassLoader`（Fat Jar）加载，层次变为：

```
Bootstrap ClassLoader
  │
  └─ LaunchedURLClassLoader（Fat Jar：BOOT-INF/lib/*.jar，包括 Tomcat 本身）
        │
        └─ TomcatEmbeddedWebappClassLoader（每个应用一个，第 6 节）
```

**区别**：Tomcat 容器类库（catalina/coyote）与应用依赖**同处一个类加载器**（LaunchedURLClassLoader）——
嵌入式中不需要 Common/Server/Shared 分层（只有一个应用，分层失去意义）。

---

## 3. WebappClassLoaderBase：打破双亲委派

### 3.1 类声明

**源码**：`WebappClassLoaderBase.java:120-121`

```java
public abstract class WebappClassLoaderBase extends URLClassLoader
        implements Lifecycle, InstrumentableClassLoader, WebappProperties, PermissionCheck {
```

- **继承 `URLClassLoader`**（不是 `ClassLoader`）——自带 jar/目录资源查找能力
- **实现 `Lifecycle`**——类加载器也有生命周期（start/stop）
- **抽象类**：`ParallelWebappClassLoader` 是默认实现

### 3.2 loadClass() 的核心算法（1207-1375 行）

**注释中的加载顺序**（1187-1198 行）明确写着：

```
1. findLoadedClass()               ← 已加载缓存
2. delegate=true 时 → parent        ← 可选：父优先
3. findClass()                      ← 自己找（本地仓库）
4. parent                          ← 兜底：父加载
```

**源码实现**（1207-1375 行，逐步拆解）：

**步骤 0：检查已加载缓存**（1218-1240）：

```java
// (0) 本地缓存（findLoadedClass0：并发优化）
clazz = findLoadedClass0(name);
if (clazz != null) { ... return clazz; }

// (0.1) 标准缓存
clazz = findLoadedClass(name);
if (clazz != null) { ... return clazz; }
```

**步骤 0.2：Javase 优先（防覆盖核心类）**（1242-1291）：

```java
/*
 * (0.2) Try loading the class with the bootstrap class loader, to prevent the webapp from overriding Java SE classes.
 * This implements SRV.10.7.2
 */
String resourceName = binaryNameToPath(name, false);
ClassLoader javaseLoader = getJavaseClassLoader();
boolean tryLoadingFromJavaseLoader;
try {
    // ★ 用 getResource 试探（避免昂贵的 ClassNotFoundException）
    URL url = javaseLoader.getResource(resourceName);
    tryLoadingFromJavaseLoader = url != null;
} catch (Throwable t) { ... }

if (tryLoadingFromJavaseLoader) {
    clazz = javaseLoader.loadClass(name);   // ★ Java SE 类从 bootstrap 加载
    if (clazz != null) { ... return clazz; }
}
```

**关键设计**：**`java.*` 类永远由 Javase 类加载器加载**——即使应用想覆盖也不允许
（Servlet 规范 SRV.10.7.2 的要求）。

**步骤 1：delegate 时父优先**（1307-1328）：

```java
boolean delegateLoad = delegate || filter(name, true);   // ★ filter 见第 4 节

// (1) Delegate to our parent if requested
if (delegateLoad) {
    clazz = Class.forName(name, false, parent);
    if (clazz != null) { ... return clazz; }
}
```

**步骤 2：本地仓库**（1330-1347）——**打破双亲委派的核心**：

```java
// (2) Search local repositories
clazz = findClass(name);        // ★ 先自己找！（URLClassLoader.findClass → findClassInternal）
if (clazz != null) { ... return clazz; }
```

**findClass 的内部优化**（`WebappClassLoaderBase.java:768-813`）：

```java
public Class<?> findClass(String name) throws ClassNotFoundException {
    ...
    String path = binaryNameToPath(name, true);
    ...
    // ★ 找不到的类会被缓存（notFoundClassResources），避免重复扫描 jar
    if (!notFoundClassResources.contains(path)) {
        clazz = findClassInternal(name);        // 从 WebResourceRoot（WEB-INF/classes、lib）找
        ...
        // ★ 有外部仓库（addURL 添加的 URL）时才走 URLClassLoader 的标准查找
        if (clazz == null && hasExternalRepositories) {
            clazz = super.findClass(name);
            ...
        }
    }
    ...
}
```

两个性能优化：
- **`notFoundClassResources` 缓存**：加载失败的类名被缓存（`ConcurrentLruCache`），
  后续请求直接判定"没有"而不重复扫描 jar——避免 `ClassNotFoundException` 的高昂代价
- **`hasExternalRepositories` 标记**：只有调用过 `addURL()`（外部仓库）才走
  `URLClassLoader.findClass` 的标准 jar 查找——嵌入式下 `addURL` 被忽略（第 6 节），此路径不执行

**步骤 3：父兜底**（1349-1368）：

```java
// (3) Delegate to parent unconditionally
if (!delegateLoad) {
    clazz = Class.forName(name, false, parent);
    if (clazz != null) { ... return clazz; }
}
```

**完整决策图**：

```
loadClass(name)
  ├─ findLoadedClass0/findLoadedClass → 已加载直接返回
  ├─ javaseLoader.getResource 试探 → 是 Java SE 类 → javase 加载
  ├─ delegate || filter(name) ?
  │    ├─ 是 → parent.loadClass() → 找到返回
  │    └─ 否 ↓
  ├─ findClass(name)（本地仓库）→ 找到返回
  ├─ parent.loadClass()（兜底）
  └─ ClassNotFoundException
```

> **面试点**：Tomcat 如何打破双亲委派？
> ——`WebappClassLoaderBase.loadClass()` **先查本地仓库（findClass）再委托父加载器**，
> 与 JDK 的"父先加载"相反。这样应用可以**覆盖**父加载器中的同名类（用自己的版本）。
> 但有两个例外：① Java SE 类（`java.*`）永远从 Javase 加载器加载（SRV.10.7.2）；
> ② `delegate=true` 或 `filter()` 命中的包（servlet API 等）走父优先。

---

## 4. filter()：包级隔离的精细控制

### 4.1 作用

`filter(name, isClassName)`（`WebappClassLoaderBase.java:2474`）判断**哪些包必须父优先**——
防止 Web 应用覆盖容器/规范的类。

### 4.2 隔离名单（源码 2474-2555 行）

| 前缀 | 必须父优先（返回 true） | 例外（允许本地加载，返回 false） |
|---|---|---|
| `jakarta.*` | `annotation.` `el.` `servlet.` `websocket.` `security.auth.message.` | `servlet.jsp.jstl.` |
| `javax.*` | **仅 `websocket.`**（迁移后残留的旧包） | 无 |
| `org.apache.*` | `el.` `catalina.` `jasper.` `juli.` `tomcat.` `naming.` `coyote.` | `tomcat.jdbc.`（允许本地） |

**含义**：
- 应用的 `jakarta.servlet.*` 类**不会生效**——规范 API 永远用容器的
- 应用的 `org.apache.tomcat.*` 类**不会生效**——容器内部实现不暴露
- 但 `org.apache.tomcat.jdbc.*`（连接池）允许应用自带——因为它是应用可替换的组件
- **javax 分支只有 websocket**：Servlet 6.0 下 `javax.*` 已基本消失，仅旧的
  `javax.websocket` 需要防覆盖（其他如 `javax.el` 等已迁移为 `jakarta.*`）

### 4.3 与 delegate 的关系

```java
boolean delegateLoad = delegate || filter(name, true);
```

- `delegate=true`：**全部**父优先（Spring Boot 的设置，见第 6 节）
- `filter(name)` 命中：**该包**父优先
- 两者都否：**先自己找**

---

## 5. WebappLoader：类加载器的装配器

### 5.1 定位

**WebappLoader**（`org.apache.catalina.loader.WebappLoader`，591 行）是 **Context 与类加载器之间的装配器**：

- 实现 `Loader` 接口（`org.apache.catalina.Loader`）
- 持有 `WebappClassLoaderBase` 实例
- 负责创建、配置、启动类加载器

### 5.2 创建过程

**默认类**（`WebappLoader.java:98`）：

```java
private String loaderClass = ParallelWebappClassLoader.class.getName();
```

**createClassLoader()**（404-423 行）：

```java
private WebappClassLoaderBase createClassLoader() throws Exception {

    if (classLoader != null) {
        return classLoader;                          // 已有实例直接返回
    }

    if (ParallelWebappClassLoader.class.getName().equals(loaderClass)) {
        // ★ 默认：ParallelWebappClassLoader（并行加载能力）
        return new ParallelWebappClassLoader(context.getParentClassLoader());
    }

    // 自定义 loaderClass：反射创建
    Class<?> clazz = Class.forName(loaderClass);
    ...
    ClassLoader parentClassLoader = context.getParentClassLoader();
    Class<?>[] argTypes = { ClassLoader.class };
    Object[] args = { parentClassLoader };
    Constructor<?> constr = clazz.getConstructor(argTypes);
    classLoader = (WebappClassLoaderBase) constr.newInstance(args);
    return classLoader;
}
```

**启动配置**（316-330 行）：

```java
classLoader = createClassLoader();
classLoader.setResources(context.getResources());     // 绑定 WebResourceRoot（WEB-INF/classes、lib）
classLoader.setDelegate(this.delegate);                // delegate 标志
// 配置仓库（WEB-INF/classes + WEB-INF/lib/*.jar）
setClassPath();
classLoader.start();
```

### 5.3 ParallelWebappClassLoader

**源码**：`ParallelWebappClassLoader.java`（61 行）

```java
public class ParallelWebappClassLoader extends WebappClassLoaderBase {

    static {
        if (!JreCompat.isGraalAvailable()) {
            if (!registerAsParallelCapable()) {        // ★ 注册并行加载能力
                log.warn(...);
            }
        }
    }
    ...
}
```

**为什么需要并行**：传统 `ClassLoader.loadClass` 的 `synchronized (getClassLoadingLock(name))`——
JDK 7+ 的"类加载锁"机制允许**不同类名并行加载**（`registerAsParallelCapable()` 注册）。
`ParallelWebappClassLoader` 正是注册了这个能力，多个请求加载不同类时不互相阻塞。

> **面试点**：`WebappClassLoader` 与 `ParallelWebappClassLoader` 有什么区别？
> ——后者**注册了 `registerAsParallelCapable()`**（JDK 7+ 类加载锁优化），
> 不同类名的加载可并行执行。Tomcat 10.1 默认用 Parallel 版本（`WebappLoader.java:98`）。
> `WebappClassLoader`（57 行）是老的非并行版本，仅用于兼容。

---

## 6. Spring Boot 嵌入式：TomcatEmbeddedWebappClassLoader 反向设计

### 6.1 为什么需要"反向"？

传统 WebappClassLoader"先自己找"的逻辑，在 **Fat Jar 场景下会出问题**：

```
Fat Jar 结构：
  BOOT-INF/classes/          ← 应用类
  BOOT-INF/lib/*.jar         ← 所有依赖（包括 Tomcat 本身、Spring）
```

问题：`LaunchedURLClassLoader` 加载了**全部依赖**（包括应用自己的类），
如果 WebappClassLoader 先找自己——它自己的仓库（WEB-INF/classes、lib）**是空的**（嵌入式没有标准 war 结构），
但父加载器（LaunchedURLClassLoader）**什么都有**。此时"先自己找"完全多余且慢，
直接"父优先"才是正确的。

### 6.2 TomcatEmbeddedWebappClassLoader

**源码**：`TomcatEmbeddedWebappClassLoader.java`（129 行，Spring Boot）

```java
/**
 * Extension of Tomcat's {@link ParallelWebappClassLoader} that does not consider the
 * {@link ClassLoader#getSystemClassLoader() system classloader}. This is required to
 * ensure that any custom context class loader is always used (as is the case with some
 * executable archives).
 */
public class TomcatEmbeddedWebappClassLoader extends ParallelWebappClassLoader {

    static {
        if (!JreCompat.isGraalAvailable()) {
            ClassLoader.registerAsParallelCapable();
        }
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (JreCompat.isGraalAvailable() ? this : getClassLoadingLock(name)) {
            Class<?> result = findExistingLoadedClass(name);
            result = (result != null) ? result : doLoadClass(name);
            if (result == null) {
                throw new ClassNotFoundException(name);
            }
            return resolveIfNecessary(result, resolve);
        }
    }

    private Class<?> doLoadClass(String name) {
        if ((this.delegate || filter(name, true))) {
            // ★ 父优先路径
            Class<?> result = loadFromParent(name);
            return (result != null) ? result : findClassIgnoringNotFound(name);
        }
        // ★ 自己优先路径（与父类相同）
        Class<?> result = findClassIgnoringNotFound(name);
        return (result != null) ? result : loadFromParent(name);
    }

    @Override
    protected void addURL(URL url) {
        // ★ 忽略 Tomcat 添加的 URL（gh-919）——仓库路径由 LaunchedURLClassLoader 管理
    }

    private Class<?> loadFromParent(String name) {
        if (this.parent == null) {
            return null;
        }
        try {
            return Class.forName(name, false, this.parent);   // ★ 父 = LaunchedURLClassLoader
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
}
```

### 6.3 与标准 WebappClassLoader 的关键差异

| 特性 | 标准 WebappClassLoader | TomcatEmbeddedWebappClassLoader |
|---|---|---|
| 加载顺序 | 先本地 → 后父 | **先父 → 后本地**（`doLoadClass` 首行父优先） |
| `addURL()` | 正常添加仓库 URL | **忽略**（仓库由 LaunchedURLClassLoader 管理） |
| `findResource`/`findResources` | 正常查找 | **返回 null/空**（资源也交给父） |
| 系统类加载器 | 可能参与 | **不考虑**（注释明确说明） |
| 父加载器 | Common/Shared | **LaunchedURLClassLoader** |

**加载顺序**（嵌入式实际）：

```
TomcatEmbeddedWebappClassLoader.loadClass("com.example.MyService")
  ├─ findExistingLoadedClass → 已加载？
  ├─ doLoadClass
  │    ├─ delegate=true（Spring Boot 设置了！）→ loadFromParent
  │    │    └─ Class.forName(name, false, LaunchedURLClassLoader)
  │    │         └─ LaunchedURLClassLoader 从 BOOT-INF/lib/*.jar 找 ★
  │    └─ 父找不到 → findClassIgnoringNotFound（自己的空仓库）
  └─ 结果
```

**为什么 Spring Boot 还要 setDelegate(true)**（`TomcatServletWebServerFactory.java:255-257`）：

```java
WebappLoader loader = new WebappLoader();
loader.setLoaderInstance(new TomcatEmbeddedWebappClassLoader(parentClassLoader));
loader.setDelegate(true);          // ★ 强制全部父优先
```

即使 `TomcatEmbeddedWebappClassLoader.doLoadClass` 已实现"先父"，`setDelegate(true)`
再兜一层——保证**所有类**（包括 filter 名单外的）都先查父加载器。

> **面试点**：为什么嵌入式 Webapp 类加载器是"父优先"？
> ——因为 Fat Jar 的 `LaunchedURLClassLoader` 已经加载了**所有**依赖
> （包括应用类 `BOOT-INF/classes` 和依赖 `BOOT-INF/lib`）。
> Webapp 类加载器自己的仓库是空的（没有标准 war 的 WEB-INF 结构），
> "先自己找"纯属浪费；且**必须**用 LaunchedURLClassLoader 的类
> （否则同一 jar 出现两份 class，类型不兼容）。

---

## 7. Fat Jar 与 LaunchedURLClassLoader

### 7.1 Fat Jar 结构（可执行 jar）

```
app.jar
  ├─ META-INF/MANIFEST.MF        （Main-Class: JarLauncher）
  ├─ org/springframework/boot/loader/...   （Spring Boot Loader）
  ├─ BOOT-INF/classes/           （应用类）
  └─ BOOT-INF/lib/*.jar          （所有依赖）
```

### 7.2 LaunchedURLClassLoader

**源码**：`LaunchedURLClassLoader.java`（367 行，spring-boot-loader-classic）

```java
public class LaunchedURLClassLoader extends URLClassLoader {

    public LaunchedURLClassLoader(boolean exploded, Archive rootArchive, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        ...
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // jarmode 特殊处理
        if (name.startsWith("org.springframework.boot.loader.jarmode.")) { ... }

        if (this.exploded) {
            return super.loadClass(name, resolve);    // 展开目录模式：标准父优先
        }

        Handler.setUseFastConnectionExceptions(true);
        try {
            try {
                definePackageIfNecessary(name);       // ★ 自动 definePackage（jar: 协议需要）
            } catch (IllegalArgumentException ex) { ... }
            return super.loadClass(name, resolve);    // 标准 URLClassLoader：父优先
        } finally {
            Handler.setUseFastConnectionExceptions(false);
        }
    }
}
```

**关键点**：
- 本质是 **URLClassLoader**：URL 指向 `BOOT-INF/lib/*.jar`（`jar:` 协议，自定义 `Handler`）
- `definePackageIfNecessary`：`jar:` 协议下需要手动 definePackage（普通 `file:` 协议自动）
- **父优先**（标准行为）——与 `TomcatEmbeddedWebappClassLoader` 的父优先形成一致链

### 7.3 完整类加载链（嵌入式）

```
请求 com.example.MyController
  └─ TomcatEmbeddedWebappClassLoader.loadClass（delegate=true）
       └─ LaunchedURLClassLoader.loadClass（父优先）
            ├─ findLoadedClass
            ├─ parent（AppClassLoader）→ 没有
            └─ findClass → BOOT-INF/lib/spring-webmvc.jar 找到 ★
```

**请求 org.apache.catalina.startup.Tomcat（容器类）**：

```
  └─ TomcatEmbeddedWebappClassLoader（delegate=true）
       └─ LaunchedURLClassLoader → BOOT-INF/lib/tomcat-embed-core.jar 找到 ★
```

> **面试点**：为什么嵌入式 Tomcat 不需要 Common/Server/Shared 类加载器分层？
> ——分层的目的（隔离容器库与应用库）在嵌入式下不存在：**只有一个应用**，
> 所有库（包括 Tomcat 自己）都在 `BOOT-INF/lib` 由 `LaunchedURLClassLoader` 统一加载。
> 分层从 4 层简化为 2 层（LaunchedURLClassLoader → TomcatEmbeddedWebappClassLoader）。

---

## 8. 本篇小结与面试要点

### 8.1 本篇地图

```
第 1 篇：StandardContext.startInternal 第 ⑩ 步 Loader.start（类加载器创建）
第 4 篇：loadServlet → InstanceManager.newInstance（反射用 WebappClassLoader）
第 7 篇（本篇）：类加载器体系与加载顺序
第 8 篇：Spring Boot 集成（setDelegate(true) 的完整上下文）
```

### 8.2 面试要点速查

1. **双亲委派 vs 打破**：WebappClassLoader"先自己找后父"（覆盖能力），JDK"先父后自己"（一致性）
2. **Javase 例外**：`java.*` 永远从 Javase 加载器加载（SRV.10.7.2，防覆盖核心类）
3. **filter() 隔离名单**：`jakarta.servlet.*`/`org.apache.tomcat.*` 等必须父优先；`tomcat.jdbc` 例外
4. **Standalone 层次**：Bootstrap → Common →（Catalina | Shared）→ **Webapp（父 = Shared，不是 Catalina）**——容器与应用兄弟隔离
5. **嵌入式简化**：LaunchedURLClassLoader → TomcatEmbeddedWebappClassLoader（两层）
6. **ParallelWebappClassLoader**：注册 `registerAsParallelCapable()`，不同类并行加载
7. **WebappLoader**：Loader 接口实现，创建/配置/启动类加载器，`loaderClass` 可定制
8. **TomcatEmbeddedWebappClassLoader 三个覆写**：`loadClass`（父优先）、`addURL`（忽略）、`findResource`（null）
9. **setDelegate(true)**：Spring Boot 强制全部父优先（Fat Jar 场景）
10. **LaunchedURLClassLoader**：URLClassLoader 加载 `BOOT-INF/lib/*.jar`，`jar:` 协议 + definePackage
11. **类加载器双亲链**：TomcatEmbedded → LaunchedURLClassLoader → AppClassLoader → Bootstrap
