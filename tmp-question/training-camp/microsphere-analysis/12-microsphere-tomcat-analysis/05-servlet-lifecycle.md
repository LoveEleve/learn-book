# 第 4 篇：Servlet 生命周期管理（StandardWrapper 深度）

> 第 2 篇中，`StandardWrapperValve.invoke()` 第 3 步调用 `wrapper.allocate()` 获取 Servlet 实例。
> 本篇回答核心问题：**Tomcat 如何管理 Servlet 的整个生命周期——实例化、初始化、并发分配、卸载销毁？**
>
> 这是第 0 篇"Servlet 生命周期契约"（init → service → destroy）在 Tomcat 中的完整实现。
>
> **源码范围**：
> - `org.apache.catalina.core.StandardWrapper`（1340 行，核心）
> - `org.apache.catalina.core.StandardWrapperFacade`（102 行，ServletConfig 门面）
> - `org.apache.tomcat.InstanceManager` + `DefaultInstanceManager`（注解处理）
> - `org.apache.catalina.core.ApplicationContext` / `ApplicationContextFacade`（ServletContext 门面，顺带）
>
> **本篇定位**：Tomcat 核心层（Standalone 与嵌入式共享）。

---

## 目录

1. [StandardWrapper：Servlet 的家](#1-standardwrapperservlet-的家)
2. [单实例模型：一个 Servlet 只有一个对象](#2-单实例模型一个-servlet-只有一个对象)
3. [allocate()：并发分配的核心](#3-allocate并发分配的核心)
4. [loadServlet()：实例化与注解处理](#4-loadservlet实例化与注解处理)
5. [initServlet()：init 契约的实现](#5-initservletinit-契约的实现)
6. [deallocate() / unload()：归还与销毁](#6-deallocate--unload归还与销毁)
7. [Facade 门面：规范对象的保护壳](#7-facade-门面规范对象的保护壳)
8. [unavailable 机制：Servlet 的故障状态](#8-unavailable-机制servlet-的故障状态)
9. [本篇小结与面试要点](#9-本篇小结与面试要点)

---

## 1. StandardWrapper：Servlet 的家

### 1.1 定位

**StandardWrapper**（`org.apache.catalina.core.StandardWrapper`，1340 行）是 Servlet 的**管理容器**：

- 一个 Wrapper 对应一个 Servlet 定义（第 1 篇：Wrapper 1:1 Servlet）
- 持有 Servlet 的**唯一实例**、配置、映射、生命周期状态
- 同时实现 `ServletConfig` 接口（第 0 篇对照表：ServletConfig 的实现者）

```java
public class StandardWrapper extends ContainerBase implements ServletConfig, Wrapper, NotificationEmitter {
```

### 1.2 关键字段

```java
// ── Servlet 实例管理 ──
protected volatile Servlet instance = null;            // 唯一实例（懒加载）
protected volatile boolean instanceInitialized = false; // init 是否完成
protected final AtomicInteger countAllocated = new AtomicInteger(0);  // 并发分配计数

// ── 可用性 ──
protected long available = 0L;          // 可用时间戳：0=立即可用，>now=不可用到该时刻，MAX_VALUE=永久不可用
protected boolean unloading = false;    // 正在卸载

// ── 配置 ──
protected int loadOnStartup = -1;       // 启动加载优先级（-1=懒加载）
protected HashMap<String,String> parameters = new HashMap<>();  // init-param
protected ArrayList<String> mappings = new ArrayList<>();       // URL 映射

// ── 门面 ──
protected final StandardWrapperFacade facade = new StandardWrapperFacade(this);  // ServletConfig 门面

// ── 卸载等待 ──
protected long unloadDelay = 2000;      // 卸载时等待活跃请求的最长时间（毫秒）
```

### 1.3 构造：Pipeline + 广播器

```java
public StandardWrapper() {
    super();
    swValve = new StandardWrapperValve();   // 自己的 basic Valve（第 2 篇）
    pipeline.setBasic(swValve);
    broadcaster = new NotificationBroadcasterSupport();  // j2ee 状态通知（嵌入式中被禁用）
}
```

---

## 2. 单实例模型：一个 Servlet 只有一个对象

### 2.1 为什么是单例？（第 0 篇契约的回顾）

Servlet 规范规定：**容器为每个 Servlet 定义只创建一个实例**，多个请求线程**并发**调用
同一个实例的 `service()`。因此：

- Servlet 的实例字段**不是线程安全的**（多个请求共享）
- 开发者必须自己同步共享资源
- 容器需要精确管理"当前有多少个请求正在使用这个实例"——这就是 `countAllocated`

### 2.2 两个真实状态（加一个防御性检查）

| 状态 | 字段 | 含义 |
|---|---|---|
| 未加载 | `instance == null` | 从未实例化（懒加载等待中） |
| 就绪 | `instance != null && instanceInitialized` | 可服务请求 |

**注意"已加载未初始化"（`instance != null && !instanceInitialized`）在正常流程中不会出现**：
`instance = loadServlet()` 是原子赋值——`loadServlet()` 内部先实例化（局部变量）再 `initServlet()`，
若 `initServlet()` 抛异常，`loadServlet()` 抛异常，赋值不完成，`instance` 保持 `null`。
所以 `allocate()` 里的 `if (!instanceInitialized) initServlet(instance)` 是**历史防御代码**（`StandardWrapper.java:587-589`），
实际几乎不会执行。

---

## 3. allocate()：并发分配的核心

**源码**：`StandardWrapper.java:557-602`

```java
@Override
public Servlet allocate() throws ServletException {

    // 卸载中 → 拒绝
    if (unloading) {
        throw new ServletException(sm.getString("standardWrapper.unloading", getName()));
    }

    boolean newInstance = false;

    // ★ 双检查锁：instance 为空才加载
    if (instance == null || !instanceInitialized) {
        synchronized (this) {
            if (instance == null) {
                try {
                    instance = loadServlet();              // 实例化 + init
                    newInstance = true;
                    // ★ 计数在创建时就 +1，防止与 unload 竞态（Bug 43683）
                    countAllocated.incrementAndGet();
                } catch (ServletException e) {
                    throw e;
                } catch (Throwable e) {
                    ExceptionUtils.handleThrowable(e);
                    throw new ServletException(sm.getString("standardWrapper.allocate"), e);
                }
            }
            if (!instanceInitialized) {
                initServlet(instance);                    // 补 init（罕见路径）
            }
        }
    }

    // 非新建 → 计数 +1（并发请求共享同一实例）
    if (!newInstance) {
        countAllocated.incrementAndGet();
    }
    return instance;
}
```

**执行流程**：

```
请求 1 到达 → allocate()
  ├─ instance == null → synchronized 加锁
  │    ├─ loadServlet()（实例化 + init）→ newInstance=true
  │    └─ countAllocated = 1
  ├─ 返回 instance
请求 2 并发到达 → allocate()
  ├─ instance != null → 跳过锁（无锁快路径！）
  └─ countAllocated = 2
请求 N → countAllocated = N
```

**两个关键设计**：

1. **双检查锁（DCL）**：`instance == null` 先无锁检查，需要时才 `synchronized(this)`——
   绝大多数请求走**无锁快路径**，只有首次加载才竞争锁
2. **计数时机**：新建实例在**创建时**就 +1（而不是返回时）——防止 `unload()` 在
   "实例已创建但计数未加"的窗口误判"无活跃请求"而销毁实例（Bug 43683）

**与 load() 的差异（loadOnStartup 路径）**：

`StandardWrapper.load()`（`StandardWrapper.java:697-723`，第 1 篇 6.3 的 `wrapper.load()`）同样调用
`loadServlet()` 完成实例化+init，但**不递增 `countAllocated`**——因为启动预加载时没有活跃请求。
所以 loadOnStartup 预加载的 Servlet 在启动后 `countAllocated == 0`，第一个请求到来时
`allocate()` 走 `!newInstance` 分支才 +1。

> **面试点**：为什么用 `countAllocated` 而不是直接判断 `instance == null`？
> ——`instance` 非空不代表没有请求在用。`unload()` 需要等所有活跃请求结束才能销毁实例，
> 它通过 `countAllocated > 0` 来判断。计数是"正在使用的请求数"，与实例是否创建是两回事。

---

## 4. loadServlet()：实例化与注解处理

**源码**：`StandardWrapper.java:735-813`

```java
public synchronized Servlet loadServlet() throws ServletException {

    // 已有实例直接返回（幂等）
    if (instance != null) {
        return instance;
    }

    Servlet servlet;
    try {
        long t1 = System.currentTimeMillis();

        // 1. 必须配置了 servlet-class
        if (servletClass == null) {
            unavailable(null);    // 标记永久不可用
            throw new ServletException(sm.getString("standardWrapper.notClass", getName()));
        }

        // 2. ★ 通过 InstanceManager 创建实例（注解处理在这里！）
        InstanceManager instanceManager = ((StandardContext) getParent()).getInstanceManager();
        try {
            servlet = (Servlet) instanceManager.newInstance(servletClass);
        } catch (ClassCastException e) {
            unavailable(null);
            throw new ServletException(sm.getString("standardWrapper.notServlet", servletClass), e);
        } catch (Throwable e) {
            e = ExceptionUtils.unwrapInvocationTargetException(e);
            ExceptionUtils.handleThrowable(e);
            unavailable(null);
            throw new ServletException(sm.getString("standardWrapper.instantiate", servletClass), e);
        }

        // 3. @MultipartConfig 注解扫描（没有显式配置时）
        if (multipartConfigElement == null) {
            MultipartConfig annotation = servlet.getClass().getAnnotation(MultipartConfig.class);
            if (annotation != null) {
                multipartConfigElement = new MultipartConfigElement(annotation);
            }
        }

        // 4. ContainerServlet 特殊处理（容器内部 Servlet 可访问容器对象）
        if (servlet instanceof ContainerServlet) {
            ((ContainerServlet) servlet).setWrapper(this);
        }

        classLoadTime = (int) (System.currentTimeMillis() - t1);

        // 5. ★ init()（内部调用 initServlet）
        initServlet(servlet);

        fireContainerEvent("load", this);
        loadTime = System.currentTimeMillis() - t1;
    } finally {
        // swallowOutput 捕获 Servlet 的 System.out 输出
        ...
    }
    return servlet;
}
```

### 4.1 InstanceManager：实例化 + 注解的合体

**源码**：`org.apache.tomcat.InstanceManager` + `org.apache.catalina.core.DefaultInstanceManager`

```java
public interface InstanceManager {

    Object newInstance(String className) throws ...;
    void newInstance(Object o) throws ...;
    void destroyInstance(Object o) throws ...;   // @PreDestroy
    default void backgroundProcess() { }          // 周期维护
}
```

**DefaultInstanceManager.newInstance**（`DefaultInstanceManager.java:156-174`）：

```java
private Object newInstance(Object instance, Class<?> clazz) throws ... {
    if (!ignoreAnnotations) {
        // 1. 沿类继承链收集 @Resource 注入
        Map<String,String> injections = assembleInjectionsFromClassHierarchy(clazz);
        populateAnnotationsCache(clazz, injections);
        // 2. 处理注解：@Resource 注入、字段/方法注解
        processAnnotations(instance, injections);
        // 3. ★ 调用 @PostConstruct
        postConstruct(instance, clazz);
    }
    return instance;
}
```

**实例化时序**：

```
instanceManager.newInstance(servletClass)
  ├─ Class.forName(className)             ← 用 Webapp 类加载器（第 7 篇）
  ├─ clazz.getConstructor().newInstance() ← 反射调用无参构造
  ├─ @Resource 注入（按类继承链）
  ├─ processAnnotations（@WebServlet 等注解元数据收集）
  └─ postConstruct()                      ← 调用 @PostConstruct 方法
       │
       ▼
initServlet(servlet)                      ← 下一步：Servlet.init()
```

> **面试点**：`@PostConstruct` 和 `init()` 的执行顺序？
> ——`@PostConstruct` 在**实例化时**由 `InstanceManager` 调用（`DefaultInstanceManager.postConstruct`），
> 先于 `Servlet.init()`。所以：**构造 → @PostConstruct → init() → service() → destroy() → @PreDestroy**。
> 这是 Servlet 3.0 注解生命周期与规范生命周期的叠加。

---

## 5. initServlet()：init 契约的实现

**源码**：`StandardWrapper.java:816-855`

```java
private synchronized void initServlet(Servlet servlet) throws ServletException {

    if (instanceInitialized) {
        return;                    // 幂等
    }

    try {
        if (Globals.IS_SECURITY_ENABLED) {
            // 安全模式：doPrivileged 包装
            SecurityUtil.doAsPrivilege("init", servlet, classType, new Object[] { facade });
        } else {
            // ★ 普通模式：直接调用（注意传入的是门面！）
            servlet.init(facade);
        }

        instanceInitialized = true;      // init 成功标志
    } catch (UnavailableException f) {
        unavailable(f);                  // Servlet 主动声明不可用（如依赖缺失）
        throw f;
    } catch (ServletException f) {
        throw f;
    } catch (Throwable f) {
        ExceptionUtils.handleThrowable(f);
        getServletContext().log(...);
        throw new ServletException(sm.getString("standardWrapper.initException", getName()), f);
    }
}
```

**关键点**：

- `servlet.init(facade)` —— 传给 Servlet 的是 **`StandardWrapperFacade`**（门面），
  不是 `StandardWrapper` 本身（第 7 节详讲）
- `UnavailableException` 特殊处理：Servlet 在 `init()` 抛它 = "主动声明暂时不可用"
  （如数据库连不上），进入 `unavailable()` 流程（第 8 节）
- `instanceInitialized = true` 在 `init()` 成功后才置位——失败则下次请求重试

---

## 6. deallocate() / unload()：归还与销毁

### 6.1 deallocate()：归还

**源码**：`StandardWrapper.java:605-608`

```java
@Override
public void deallocate(Servlet servlet) throws ServletException {
    countAllocated.decrementAndGet();   // 活跃请求数 -1
}
```

由 `StandardWrapperValve.invoke()` finally 块调用（第 2 篇第 8 步）——请求结束即归还。

### 6.2 unload()：销毁（Context 停止/重载时）

**源码**：`StandardWrapper.java:920-1004`

```java
public synchronized void unload() throws ServletException {

    if (instance == null) {
        return;                        // 从未加载，无事可做
    }
    unloading = true;

    // ★ 等待活跃请求结束（unloadDelay=2000ms 分 20 段，最多 21 次检查）
    if (countAllocated.get() > 0) {
        int nRetries = 0;
        long delay = unloadDelay / 20;
        while ((nRetries < 21) && (countAllocated.get() > 0)) {
            if ((nRetries % 10) == 0) {
                log.info(sm.getString("standardWrapper.waiting", countAllocated.toString(), getName()));
            }
            Thread.sleep(delay);
            nRetries++;
        }
    }

    if (instanceInitialized) {
        try {
            // 1. ★ 调用 Servlet.destroy()（规范契约）
            instance.destroy();
        } catch (Throwable t) {
            t = ExceptionUtils.unwrapInvocationTargetException(t);
            ExceptionUtils.handleThrowable(t);
            unloading = false;
            throw new ServletException(sm.getString("standardWrapper.destroyException", getName()), t);
        } finally {
            // ★ finally：即使 destroy() 抛异常，@PreDestroy 也一定执行
            // 2. @PreDestroy 回调（通过 InstanceManager）
            if (!((Context) getParent()).getIgnoreAnnotations()) {
                ((Context) getParent()).getInstanceManager().destroyInstance(instance);
            }
            // 3. 清空引用
            instance = null;
            instanceInitialized = false;
        }
    }

    // 4. JSP 监控 MBean 注销（嵌入式中被禁用）
    if (isJspServlet && jspMonitorON != null) {
        Registry.getRegistry(null, null).unregisterComponent(jspMonitorON);
    }

    unloading = false;
    fireContainerEvent("unload", this);
}
```

**完整生命周期时序**：

```
创建（容器反射构造）
  ├─ @PostConstruct（InstanceManager）
  ├─ init(facade)                       ← allocate()/load() 触发
  ├─ service() × N（并发，countAllocated 计数）
  ├─ destroy()                          ← unload() 触发（Context 停止）
  └─ @PreDestroy（InstanceManager.destroyInstance）
```

> **面试点**：`destroy()` 和 `@PreDestroy` 的顺序？
> ——`destroy()` 在前（`StandardWrapper.unload()` 先调 `instance.destroy()`），
> `@PreDestroy` 在后（`InstanceManager.destroyInstance()` 内部调用）。
> **且 `@PreDestroy` 在 finally 中执行**——即使 `destroy()` 抛异常，`@PreDestroy` 也一定会被调用。

---

## 7. Facade 门面：规范对象的保护壳

### 7.1 为什么需要门面？

**门面模式**：Servlet 规范对象（`ServletConfig`/`ServletContext`/`HttpServletRequest` 等）对应用暴露时，
不能直接给内部实现对象——否则应用可以**向下转型**访问容器内部结构，破坏封装与安全。

**Tomcat 的门面体系**（第 0 篇对照表）：

| 规范接口 | 内部实现 | 门面（应用可见） |
|---|---|---|
| `ServletConfig` | `StandardWrapper`（自身实现） | `StandardWrapperFacade` |
| `ServletContext` | `ApplicationContext` | `ApplicationContextFacade` |
| `ServletRequest` | `Request` | `RequestFacade` |
| `ServletResponse` | `Response` | `ResponseFacade` |
| `HttpSession` | `StandardSession` | `StandardSessionFacade` |

### 7.2 StandardWrapperFacade

**源码**：`org/apache/catalina/core/StandardWrapperFacade.java`（102 行）

```java
public final class StandardWrapperFacade implements ServletConfig {

    private final ServletConfig config;        // 内部：StandardWrapper 本身

    public StandardWrapperFacade(StandardWrapper config) {
        this.config = config;
    }

    @Override
    public String getServletName() {
        return config.getServletName();
    }

    @Override
    public ServletContext getServletContext() {
        if (context == null) {
            context = config.getServletContext();
            if (context instanceof ApplicationContext) {
                // ★ 关键：ServletContext 也换成门面！
                context = ((ApplicationContext) context).getFacade();
            }
        }
        return context;
    }

    @Override
    public String getInitParameter(String name) {
        return config.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return config.getInitParameterNames();
    }
}
```

**要点**：

- **方法全部转发**：门面只做"转发"，不加逻辑
- **嵌套门面**：`getServletContext()` 返回的也不是 `ApplicationContext`，而是它的门面
  `ApplicationContextFacade`——**门面套门面**
- **final 类**：防止应用继承门面绕过保护
- **并发注意**：`getServletContext()` 中 `context` 字段非 volatile，但源码注释明确说明
  "可并发调用，但总是返回同一个对象，无并发问题"（`StandardWrapperFacade.java:76-79`）

### 7.3 门面如何"递进"？

`StandardWrapperFacade.getServletContext()` 中：

```java
if (context instanceof ApplicationContext) {
    context = ((ApplicationContext) context).getFacade();
}
```

`ApplicationContext.getFacade()`（`ApplicationContext.java:1238-1242`，`facade` 字段在 140 行构造时创建）：

```java
// 140 行：构造时创建（final）
private final ServletContext facade = new ApplicationContextFacade(this);

// 1238 行：直接返回
protected ServletContext getFacade() {
    return this.facade;
}
```

> **面试点**：为什么应用拿到的 `ServletContext` 不是 `ApplicationContext`？
> ——防向下转型。`ApplicationContext` 有很多容器内部方法（如 `getContext()` 访问父级），
> 直接暴露会被 `(ApplicationContext) ctx` 强转后滥用。门面只暴露规范定义的方法。

---

## 8. unavailable 机制：Servlet 的故障状态

### 8.1 unavailable()

**源码**：`StandardWrapper.java:901-916`

```java
@Override
public void unavailable(UnavailableException unavailable) {
    getServletContext().log(sm.getString("standardWrapper.unavailable", getName()));
    if (unavailable == null) {
        setAvailable(Long.MAX_VALUE);                    // 永久不可用
    } else if (unavailable.isPermanent()) {
        setAvailable(Long.MAX_VALUE);                    // 永久不可用
    } else {
        int unavailableSeconds = unavailable.getUnavailableSeconds();
        if (unavailableSeconds <= 0) {
            unavailableSeconds = 60;                     // 默认 60 秒
        }
        setAvailable(System.currentTimeMillis() + (unavailableSeconds * 1000L));  // 临时不可用
    }
}
```

**触发场景**：
- `init()` 抛 `UnavailableException`（第 5 节）
- `loadServlet()` 实例化失败（第 4 节：`servletClass == null` / `ClassCastException`）
- `service()` 抛 `UnavailableException`（第 2 篇 WrapperValve 第 6 步 catch）

### 8.2 isUnavailable() 与 available 时间戳

```java
public boolean isUnavailable() {

    if (!isEnabled()) {                    // ★ Servlet 被显式禁用（setEnabled(false)）
        return true;
    } else if (available == 0L) {
        return false;                      // 立即可用
    } else if (available <= System.currentTimeMillis()) {
        // 临时不可用期已过 → 自动恢复
        available = 0L;
        return false;
    } else {
        return true;                       // 不可用中
    }
}
```

**三态**：

| `available` 值 | 状态 |
|---|---|
| `0L` | 正常可用 |
| `now < available < MAX` | 临时不可用（倒计时自动恢复） |
| `Long.MAX_VALUE` | 永久不可用 |

**调用方**（第 2 篇）：`StandardWrapperValve` 第 2 步 `wrapper.isUnavailable()` 检查 →
不可用则 `checkWrapperAvailable()` 发 503（Service Unavailable）+ `Retry-After` 头。

---

## 9. 本篇小结与面试要点

### 9.1 本篇地图

```
第 0 篇：Servlet 生命周期契约（init/service/destroy）
第 2 篇：StandardWrapperValve（allocate 的调用方）
第 4 篇（本篇）：StandardWrapper（allocate 的实现方）
第 7 篇：类加载机制（loadServlet 的 Class.forName 用谁加载）
第 8 篇：Spring Boot 集成（DispatcherServlet 的注册与初始化时机）
```

### 9.2 面试要点速查

1. **单实例模型**：一个 Wrapper 一个 Servlet 实例，`countAllocated` 计数活跃请求
2. **双检查锁**：`allocate()` 无锁快路径 + `synchronized` 慢路径，首次加载才加锁
3. **计数时机**：新建实例创建时 +1（Bug 43683 竞态防御）
4. **实例化顺序**：构造 → `@PostConstruct` → `init()` → `service()` × N → `destroy()` → `@PreDestroy`
5. **InstanceManager**：`newInstance` 负责反射创建 + `@Resource` 注入 + `@PostConstruct`
6. **门面体系**：`StandardWrapperFacade`/`ApplicationContextFacade`/`RequestFacade` 等，
   防向下转型，嵌套门面（`getServletContext()` 返回门面的门面）
7. **unavailable 三态**：0=可用 / 时间戳=临时不可用（默认 60s）/ MAX=永久不可用
8. **unload 等待**：`countAllocated > 0` 时轮询等待，`unloadDelay=2000ms` 分 20 段（每段 100ms，最多 21 次检查 ≈ 2.1s）
9. **init 失败语义**：`UnavailableException` = 主动声明不可用；其他异常 = 下次请求重试
10. **ServletConfig 双重实现**：`StandardWrapper` 自身实现 + `StandardWrapperFacade` 门面暴露
