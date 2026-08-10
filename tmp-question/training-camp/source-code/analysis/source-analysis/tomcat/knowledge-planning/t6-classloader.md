# T-6 ClassLoader — WebappClassLoader 打破双亲委派

> 项目: Tomcat 10.1.x | 🟡B 域 / 1-2 篇 | WebappClassLoaderBase(2660行)+ParallelWebappClassLoader(61行)
> 基线: T-1 Container 树 — Context 持有 WebappClassLoader，每个Web应用有独立的ClassLoader

---

## §0.8 域审核

- 核心类: WebappClassLoaderBase(2660行) + 变体(WebappClassLoader 57行 / ParallelWebappClassLoader 61行)
- 淘汰检查: 无 — ClassLoader 隔离是现代容器的基础，Spring Boot 容器隔离同样需要
- 之前分析覆盖: 08-classloader.md(24K字) — 确认此域不冗余

---

## 01 提取

### WebappClassLoaderBase.java (2660 行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| WebappClassLoaderBase.java:281 | **delegate=false** — 默认打破双亲委派: 先从 webapp 本地加载→找不到→委托 parent | High |
| WebappClassLoaderBase.java:1182 | **loadClass()**: delegateFirst 判断→双路径: delegate=true 先 parent / delegate=false 先本地 | High |
| WebappClassLoaderBase.java:989 | **delegateFirst = delegate \|\| filter(name, false)**: filter 强制某些类走 parent (Servlet API/JDK core) | High |
| WebappClassLoaderBase.java:768 | **findClass()**: 从 WEB-INF/classes + WEB-INF/lib/*.jar 加载 | High |

### WebappClassLoader.java (57 行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| WebappClassLoader.java:38-40 | **getClassLoadingLock()** 返回 `this` — 串行加载，非并行 | High |

### ParallelWebappClassLoader.java (61 行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ParallelWebappClassLoader.java:31 | **registerAsParallelCapable()** — 每类独立锁，并行加载 | High |

---

## 02-04 聚合+分类+聚类

| KP | 级别 | 文章 |
|------|:--:|:--:|
| **打破双亲委派(delegate+filter)** | 🟡 | §1.1 |
| **loadClass 双路径** | 🟡 | §1.2 |
| **串行 vs 并行加载** | 🟡 | §1.3 |
| **Spring Boot 中的 delegate=true** | 🟡 | §1.4 |

> 单篇大纲(§1): 打破双亲委派 + filter名单 + 串行vs并行 + Spring Boot delegate=true。引出 Spring Boot 集成(T-7)。
