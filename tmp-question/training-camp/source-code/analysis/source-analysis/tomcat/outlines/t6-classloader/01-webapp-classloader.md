# T-6 ClassLoader — WebappClassLoader 打破双亲委派 + Filter 名单 + 并行加载

> 依赖 T-1 §4 | 🟡 Working | 2 KP

**读者处境**: Context 代表一个 Web 应用 — 每个应用有自己的 `WEB-INF/classes` 和 `WEB-INF/lib`。如果所有应用共享同一个 ClassLoader — 应用 A 的 `com.example.Service` 会被应用 B 的 `com.example.Service` 覆盖 — 类冲突。Tomcat 的方案: 每个 Web 应用分配一个 `WebappClassLoader` — 类隔离。

### 1. 打破双亲委派 — delegate=false + filter 名单

场景: JDK 的双亲委派: `Application ClassLoader → Extension ClassLoader → Bootstrap ClassLoader`，加载类时先从 parent 找 — 保证 JDK 核心类不被覆盖。但 Web 应用需要覆盖共享库的类 — 比如应用自带 `logback.xml` 使用特定版本的 `ch.qos.logback` — Tomcat 的 shared lib 里也有 `logback` — 如果 parent 先加载 — 应用永远用自己的版本。

源码路径:
- `WebappClassLoaderBase.java:281` — **delegate=false** — 默认打破双亲: 先本地→后 parent
- `WebappClassLoaderBase.java:989` — **delegateFirst = delegate || filter(name, false)** — filter 强制某些包走 parent
- `WebappClassLoaderBase.java:1182` — **loadClass()**: delegateFirst=true→try parent first; delegateFirst=false→try local first(调用 findClass:768 从 WEB-INF 加载)

关键设计: **Why delegate=false 默认？** Servlet 规范 2.3+ (J2EE) 要求 Web 应用类优先于容器类。从 parent 先加载 = 应用永远不能覆盖容器类 = 兼容性和灵活性折中。**但 Spring Boot 嵌入式设 `delegate=true`** — 因为 Spring Boot 的依赖全是应用自己通过 Maven/Gradle 管理的 — 没有"共享 lib vs 应用 lib"的冲突。delegate=false 只在独立 Tomcat(多个 Web 应用共享 catalina lib)时才有意义。

### 2. Filter 名单 — 什么类绝对不能从 Web 应用加载？

场景: `javax.sql.DataSource` — JDK 的一部分 — 如果 Web 应用的 `WEB-INF/lib` 里打包了一个 `javax.sql.DataSource` — 应用自身的类加载器会优先找到它 — 但这会破坏 JDK 标准库的类型一致性 — 可能导致 `ClassCastException`。Tomcat 维护了一份 **filter 名单** — 列出的包不会被 WebappClassLoader 从本地加载 — 强制走 parent 委托。

源码路径:
- `WebappClassLoaderBase` 的 filter 名单包含: `jakarta.servlet.*`, `org.apache.catalina.*`, `org.apache.tomcat.*`, `java.*`, `javax.*` — 这些包名匹配的类 → delegateFirst=true → 强制 parent 加载 → 即使 WEB-INF/lib 有同名类 → 也不被加载

关键设计: **Why 不允许覆盖 javax.* / jakarta.*？** 如果应用打包了自己的 `jakarta.servlet.Servlet` — Servlet Container 必须使用容器提供的版本 — 否则 Servlet 实例和 Container 期望的类型不一致 → `ClassCastException`。filter 名单是 **类型安全的底线** — 保证容器和 Web 应用看到的是同一套核心 API 类。

### 3. 串行 vs 并行加载 — WebappClassLoader vs ParallelWebappClassLoader

场景: 应用启动 — Tomcat 需要在 `WEB-INF/lib` 里扫描 50 个 JAR — 每个 JAR 加载 100 个类 → 5000 个类需要加载。Java 7+ 支持并行类加载 — 但需要 ClassLoader 声明自己支持并行。`ParallelWebappClassLoader` 在 static 块调用 `registerAsParallelCapable()`(L31) — Java 会对每个不同类名的加载使用不同的锁 → 50 个类可以同时被多个线程加载。非并行变体 `WebappClassLoader` — `getClassLoadingLock()` 返回 `this`(L38-40) → 所有类共享同一把锁 → 串行加载。

源码路径:
- `ParallelWebappClassLoader.java:31-35` — **static 块**: `registerAsParallelCapable()` 注册 → 每类独立锁
- `WebappClassLoader.java:38-40` — **getClassLoadingLock()**: 返回 `this` — 所有类串行

关键设计: **Spring Boot 用哪个？** `TomcatEmbeddedWebappClassLoader`(Spring Boot 自定义) — 继承自 `WebappClassLoaderBase` — 默认并行加载(L57-61 的 `WebappClassLoader` 是 Tomcat standalone 的默认 — 嵌入式走了自定义路径)。

→ 引出 T-7 Spring Boot 集成 — ClassLoader 是 Web 应用隔离的核心 — Spring Boot 嵌入式 Tomcat 如何配置 `TomcatEmbeddedWebappClassLoader`？`TomcatServletWebServerFactory` 如何把 Context/ClassLoader/Connector/Engine 全部组装起来？
