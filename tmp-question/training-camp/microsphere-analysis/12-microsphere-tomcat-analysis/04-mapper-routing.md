# 第 3 篇：Mapper 路由机制

> 第 2 篇中，四大 Valve 反复从 `request.getHost()`/`getContext()`/`getWrapper()` 取路由结果。
> 本篇回答核心问题：**这些路由结果是谁、在什么时候、用什么算法算出来的？**
>
> 答案是 **Mapper**：一个独立的"URL 路由器"，在 `CoyoteAdapter.postParseRequest()` 中被调用，
> 把 `(域名, URI)` 映射为 `(Host, Context, Wrapper)` 三元组。
>
> **源码范围**：
> - `org.apache.catalina.mapper.Mapper`（1655 行，核心）
> - `org.apache.catalina.mapper.MapperListener`（512 行，路由表的维护者）
> - `org.apache.catalina.mapper.MappingData`（62 行，映射结果）
> - `jakarta.servlet.http.MappingMatch`（Servlet 4.0 新增，映射类型）
> - `org.apache.catalina.connector.CoyoteAdapter`（postParseRequest 中的调用点）
>
> **本篇定位**：Tomcat 核心层（Standalone 与嵌入式共享）。是理解"URL 怎么找到 Servlet"的必经之路。

---

## 目录

1. [Mapper 是什么：路由表 + 路由算法](#1-mapper-是什么路由表--路由算法)
2. [路由表的数据结构：四层索引](#2-路由表的数据结构四层索引)
3. [MapperListener：路由表的守护者](#3-mapperlistener路由表的守护者)
4. [map() 算法：Host → Context → Wrapper 三级匹配](#4-map-算法host--context--wrapper-三级匹配)
5. [Wrapper 的五条匹配规则（Servlet 规范映射）](#5-wrapper-的五条匹配规则servlet-规范映射)
6. [CoyoteAdapter 中的调用与二次映射](#6-coyoteadapter-中的调用与二次映射)
7. [本篇小结与面试要点](#7-本篇小结与面试要点)

---

## 1. Mapper 是什么：路由表 + 路由算法

### 1.1 定位

**Mapper**（`org.apache.catalina.mapper.Mapper`）是 Tomcat 的**路由核心**，承担两件事：

1. **维护路由表**：Host/Context/Wrapper 的注册信息（谁在哪个域名下、哪个路径映射到哪个 Servlet）
2. **执行路由**：给定 `(域名, URI)`，算出 `(Host, Context, Wrapper)`

它被 `StandardService` 持有（第 1 篇已见：`StandardService.java:97`）：

```java
protected final Mapper mapper = new Mapper();
```

一个 Service 一个 Mapper（一个 Tomcat 实例通常只有一个 Service）。

### 1.2 与 Valve 的关系（第 2 篇的衔接）

```
CoyoteAdapter.postParseRequest()          ← 请求解析
  └─ mapper.map(serverName, uri, ...)     ← ★ 路由计算（本篇）
       └─ request.mappingData 填结果       ← host/context/wrapper 写进 MappingData
            │
            ▼
StandardEngineValve.invoke()
  └─ request.getHost()      ← 从 MappingData 取（不是自己查！）
StandardHostValve.invoke()
  └─ request.getContext()   ← 从 MappingData 取
StandardContextValve.invoke()
  └─ request.getWrapper()   ← 从 MappingData 取
```

**关键结论（第 2 篇 2.1 的源码依据）**：`Request.getContext()`/`getHost()`/`getWrapper()`
全部直接返回 `mappingData` 字段（`Request.java:557/601/716`）：

```java
public Context getContext() { return mappingData.context; }
public Host getHost()     { return mappingData.host; }
public Wrapper getWrapper(){ return mappingData.wrapper; }
```

所以四大 Valve 拿到的是 **Mapper 预先算好的结果**，它们自己不做任何路由决策。

### 1.3 MappingData：路由结果载体

**源码**：`org/apache/catalina/mapper/MappingData.java`（62 行）

```java
public class MappingData {

    public Host host = null;
    public Context context = null;
    public int contextSlashCount = 0;
    public Context[] contexts = null;          // 多版本部署时所有 Context（Session 重映射用）
    public Wrapper wrapper = null;
    public boolean jspWildCard = false;

    public final MessageBytes requestPath = MessageBytes.newInstance();   // 完整请求路径
    public final MessageBytes wrapperPath = MessageBytes.newInstance();   // 映射到 Wrapper 的路径
    public final MessageBytes pathInfo = MessageBytes.newInstance();      // 剩余路径信息
    public final MessageBytes redirectPath = MessageBytes.newInstance();  // 目录重定向路径

    // Servlet 4.0 的 HttpServletMapping 支持
    public MappingMatch matchType = null;      // CONTEXT_ROOT/DEFAULT/EXACT/EXTENSION/PATH

    public void recycle() { ... }              // 清空复用
}
```

`Request` 持有它（`Request.java:608`）：`protected final MappingData mappingData = new MappingData();`

---

## 2. 路由表的数据结构：四层索引

Mapper 内部用**四层嵌套结构**组织路由信息。全部定义在 `Mapper.java` 内部类中：

```
Mapper
  ├── MappedHost[] hosts                  ← 第 1 层：域名 → Host
  │     └── MappedHost.contextList        ← 第 2 层：路径前缀 → Context
  │           └── ContextList.contexts
  │                 └── MappedContext     ← 第 3 层：Context 路径 → 多版本
  │                       └── MappedContext.versions
  │                             └── ContextVersion   ← 第 4 层：版本 → Context
  │                                   ├── exactWrappers      (精确匹配)
  │                                   ├── wildcardWrappers   (前缀匹配 /*)
  │                                   ├── extensionWrappers  (后缀匹配 *.ext)
  │                                   └── defaultWrapper     (默认 /)
  └── defaultHostName / defaultHost       ← 兜底域名
```

### 2.1 每层的类定义

**MappedHost**（`Mapper.java:1489-1540`）——域名层：

```java
protected static final class MappedHost extends MapElement<Host> {

    public volatile ContextList contextList;      // 该域名下的 Context 列表

    // 别名机制：alias 的 realHost 指向真身，共享同一个 contextList
    private final MappedHost realHost;
    private final List<MappedHost> aliases;       // 真身持有所有别名

    public MappedHost(String name, Host host) {           // 真身构造
        super(name, host);
        realHost = this;
        contextList = new ContextList();
        aliases = new CopyOnWriteArrayList<>();
    }

    public MappedHost(String alias, MappedHost realHost) {  // 别名构造
        super(alias, realHost.object);
        this.realHost = realHost;
        this.contextList = realHost.contextList;          // ← 别名共享真身的 Context 列表
        this.aliases = null;
    }

    public boolean isAlias() { return realHost != this; }
    public MappedHost getRealHost() { return realHost; }
}
```

**关键设计**：`<Host name="www.example.com"><Alias>example.com</Alias></Host>`
——两个域名指向同一个 Host 对象，**别名共享同一个 contextList**，路由结果一致。

**ContextList**（`Mapper.java:1563-1600`）——Context 列表层：

```java
protected static final class ContextList {

    public final MappedContext[] contexts;   // 按路径排序（insertMap 保持有序）
    public final int nesting;                // 最大嵌套层级（/a/b/c 的 slashCount）

    public ContextList addContext(MappedContext mappedContext, int slashCount) {
        // 插入并保持有序，同时更新 nesting = max(nesting, slashCount)
    }
}
```

**MappedContext**（`Mapper.java:1602-1609`）——路径层：

```java
protected static final class MappedContext extends MapElement<Void> {
    public volatile ContextVersion[] versions;   // 多版本部署支持（如 /app##v1）
    public MappedContext(String name, ContextVersion firstVersion) { ... }
}
```

**ContextVersion**（`Mapper.java:1611-1642`）——版本层（真正干活的）：

```java
protected static final class ContextVersion extends MapElement<Context> {

    public final String path;                    // Context 路径（"" 或 "/app"）
    public final int slashCount;                 // 路径中 / 的数量（嵌套深度）
    public final WebResourceRoot resources;      // 静态资源根（welcome file 检查用）
    public String[] welcomeResources;            // 欢迎文件列表
    public MappedWrapper defaultWrapper = null;          // / 映射
    public MappedWrapper[] exactWrappers = new MappedWrapper[0];     // 精确匹配
    public MappedWrapper[] wildcardWrappers = new MappedWrapper[0];  // /* 前缀匹配
    public MappedWrapper[] extensionWrappers = new MappedWrapper[0]; // *.ext 后缀匹配
    public int nesting = 0;                      // Wrapper 最大嵌套
    private volatile boolean paused;             // 暂停状态（重载时）
}
```

**MappedWrapper**（`Mapper.java:1644-1654`）——Servlet 层：

```java
protected static class MappedWrapper extends MapElement<Wrapper> {
    public final boolean jspWildCard;    // JSP Servlet 的 /* 通配（welcome file 特殊处理）
    public final boolean resourceOnly;   // 仅处理物理资源（如 JSP）
}
```

### 2.2 有序数组 + 二分查找：性能核心

**关键设计**：所有数组（`hosts`、`contexts`、`versions`、三种 `wrappers`）都是**按名称排序的有序数组**，
插入用 `insertMap`（保持有序），查找用**二分查找** `find()`：

```java
private static <T> int find(MapElement<T>[] map, CharChunk name, int start, int end) {
    int a = 0;
    int b = map.length - 1;
    if (b == -1) return -1;
    if (compare(name, start, end, map[0].name) < 0) return -1;
    if (b == 0) return 0;
    int i = 0;
    while (true) {
        i = (b + a) >>> 1;                 // 二分
        int result = compare(name, start, end, map[i].name);
        if (result == 1) a = i;
        else if (result == 0) return i;
        else b = i;
        if ((b - a) == 1) { ... return a 或 b; }   // 收敛
    }
}
```

**注意语义**：`find` 返回的是"**最接近的较小或相等的元素**"（closest inferior or equal）——
不是精确匹配！精确匹配由 `exactFind` 再校验一次：

```java
private static <T, E extends MapElement<T>> E exactFind(E[] map, CharChunk name) {
    int pos = find(map, name);              // 二分定位
    if (pos >= 0) {
        E result = map[pos];
        if (name.equals(result.name)) {     // 校验是否真的相等
            return result;
        }
    }
    return null;
}
```

> **面试点**：为什么用"有序数组 + 二分"而不是 HashMap？——**性能**。请求路由是每请求必执行的热路径，
> 二分查找 O(log n) 且**无哈希计算、无链表遍历、CPU 缓存友好**；同时数组不可变（每次修改创建新数组），
> 天然无锁并发读。这是 Tomcat 对"读多写少"场景的极致优化。

---

## 3. MapperListener：路由表的守护者

### 3.1 定位

**MapperListener**（`org.apache.catalina.mapper.MapperListener`，512 行）是 **Mapper 与容器树之间的桥梁**：

- **监听容器事件**：Host/Context/Wrapper 的增删改 → 同步更新 Mapper 路由表
- **监听生命周期事件**：`AFTER_START_EVENT` → 注册；`BEFORE_STOP_EVENT` → 注销

构造于 `StandardService`（`StandardService.java:103`）：

```java
protected final MapperListener mapperListener = new MapperListener(this);
```

### 3.2 双通道监听：ContainerListener + LifecycleListener

`addListeners()`（`MapperListener.java:491-497`）**递归**给整个容器树挂监听：

```java
private void addListeners(Container container) {
    container.addContainerListener(this);     // 容器事件（addChild/removeChild）
    container.addLifecycleListener(this);     // 生命周期事件（start/stop）
    for (Container child : container.findChildren()) {
        addListeners(child);                  // 递归
    }
}
```

**通道一：容器事件**（`containerEvent`，147-249 行）——动态增删：

```java
@Override
public void containerEvent(ContainerEvent event) {
    if (Container.ADD_CHILD_EVENT.equals(event.getType())) {
        Container child = (Container) event.getData();
        addListeners(child);
        // 子容器已启动 → 立即注册（此时生命周期事件已错过）
        if (child.getState().isAvailable()) {
            if (child instanceof Host)       registerHost((Host) child);
            else if (child instanceof Context) registerContext((Context) child);
            else if (child instanceof Wrapper) {
                if (child.getParent().getState().isAvailable()) {
                    registerWrapper((Wrapper) child);
                }
            }
        }
    } else if (Container.REMOVE_CHILD_EVENT.equals(event.getType())) {
        ...
    } else if (Wrapper.ADD_MAPPING_EVENT.equals(event.getType())) {
        // ★ Servlet 动态添加映射（addServletMappingDecoded 触发）
        Wrapper wrapper = (Wrapper) event.getSource();
        ...
        mapper.addWrapper(hostName, contextPath, version, mapping, wrapper, jspWildCard, ...);
    } else if (Wrapper.REMOVE_MAPPING_EVENT.equals(event.getType())) {
        mapper.removeWrapper(...);
    }
}
```

**通道二：生命周期事件**（`lifecycleEvent`，452-483 行）——启停注册：

```java
@Override
public void lifecycleEvent(LifecycleEvent event) {
    if (event.getType().equals(AFTER_START_EVENT)) {
        Object obj = event.getSource();
        if (obj instanceof Wrapper) {
            Wrapper w = (Wrapper) obj;
            if (w.getParent().getState().isAvailable()) registerWrapper(w);
        } else if (obj instanceof Context) {
            Context c = (Context) obj;
            if (c.getParent().getState().isAvailable()) registerContext(c);
        } else if (obj instanceof Host) {
            registerHost((Host) obj);
        }
    } else if (event.getType().equals(BEFORE_STOP_EVENT)) {
        // 反向注销
        ...
    }
}
```

### 3.3 注册流程：registerHost → registerContext → Wrapper

**registerHost**（292-309 行）：

```java
private void registerHost(Host host) {
    String[] aliases = host.findAliases();
    mapper.addHost(host.getName(), aliases, host);      // 域名 + 别名注册

    for (Container container : host.findChildren()) {
        if (container.getState().isAvailable()) {
            registerContext((Context) container);       // 递归注册 Context
        }
    }
    findDefaultHost();                                   // 重算默认域名
}
```

**registerContext**（360-386 行）：

```java
private void registerContext(Context context) {
    String contextPath = context.getPath();
    if ("/".equals(contextPath)) {
        contextPath = "";                                // ★ 根 Context 路径归一化为 ""
    }
    Host host = (Host) context.getParent();
    WebResourceRoot resources = context.getResources();
    String[] welcomeFiles = context.findWelcomeFiles();
    List<WrapperMappingInfo> wrappers = new ArrayList<>();
    for (Container container : context.findChildren()) {
        prepareWrapperMappingInfo(context, (Wrapper) container, wrappers);  // 收集所有 Servlet 映射
    }
    mapper.addContextVersion(host.getName(), host, contextPath, context.getWebappVersion(),
            context, welcomeFiles, resources, wrappers);
}
```

**Wrapper 映射收集**（`prepareWrapperMappingInfo`，442-450 行）：

```java
private void prepareWrapperMappingInfo(Context context, Wrapper wrapper, List<WrapperMappingInfo> wrappers) {
    String wrapperName = wrapper.getName();
    boolean resourceOnly = context.isResourceOnlyServlet(wrapperName);
    String[] mappings = wrapper.findMappings();
    for (String mapping : mappings) {
        boolean jspWildCard = (wrapperName.equals("jsp") && mapping.endsWith("/*"));
        wrappers.add(new WrapperMappingInfo(mapping, wrapper, jspWildCard, resourceOnly));
    }
}
```

### 3.4 Wrapper 映射的四分类（addWrapper）

**源码**：`Mapper.java:441-489`——按 Servlet 规范的四类 URL 模式分桶：

```java
protected void addWrapper(ContextVersion context, String path, Wrapper wrapper, boolean jspWildCard,
        boolean resourceOnly) {

    synchronized (context) {
        if (path.endsWith("/*")) {
            // ★ 前缀通配：/foo/* → wildcardWrappers（name 去掉尾部的 /*）
            String name = path.substring(0, path.length() - 2);
            MappedWrapper newWrapper = new MappedWrapper(name, wrapper, jspWildCard, resourceOnly);
            ... insertMap → context.wildcardWrappers
        } else if (path.startsWith("*.")) {
            // ★ 后缀通配：*.do → extensionWrappers（name 去掉 *.）
            String name = path.substring(2);
            ... context.extensionWrappers
        } else if (path.equals("/")) {
            // ★ 默认 Servlet：/ → defaultWrapper
            context.defaultWrapper = new MappedWrapper("", wrapper, jspWildCard, resourceOnly);
        } else {
            // ★ 精确匹配：/foo → exactWrappers
            final String name;
            if (path.length() == 0) {
                name = "/";      // Context 根映射（Servlet 4.0 的 CONTEXT_ROOT）
            } else {
                name = path;
            }
            ... context.exactWrappers
        }
    }
}
```

> **面试点**：Servlet 规范的四类 URL 模式如何存储？——**分桶存储**：
> `/*` 前缀 → `wildcardWrappers`，`*.ext` 后缀 → `extensionWrappers`，`/` → `defaultWrapper`，其余 → `exactWrappers`。
> 映射时按"精确 → 前缀 → 后缀 → 默认"顺序匹配（Servlet 规范规定）。

---

## 4. map() 算法：Host → Context → Wrapper 三级匹配

### 4.1 入口

**源码**：`Mapper.java:654-667`

```java
public void map(MessageBytes host, MessageBytes uri, String version, MappingData mappingData) throws IOException {

    if (host.isNull()) {
        // 无 Host 头（HTTP/1.0）→ 用默认域名
        String defaultHostName = this.defaultHostName;
        if (defaultHostName == null) return;
        host.setChars(MessageBytes.EMPTY_CHAR_ARRAY, 0, 0);
        host.getCharChunk().append(defaultHostName);
    }
    host.toChars();
    uri.toChars();
    internalMap(host.getCharChunk(), uri.getCharChunk(), version, mappingData);
}
```

### 4.2 第一级：Host 匹配（internalMap 前半段，696-731 行）

```java
// Virtual host mapping
MappedHost[] hosts = this.hosts;
MappedHost mappedHost = exactFindIgnoreCase(hosts, host);   // ★ 域名精确匹配（忽略大小写）
if (mappedHost == null) {
    // 通配域名支持：*.example.com 注册时去掉 * 存为 ".example.com"
    // 请求 example.com 的二级域名 xxx.example.com 时，去掉首段再查
    int firstDot = host.indexOf('.');
    if (firstDot > -1) {
        int start = host.getStart();
        host.setStart(firstDot + start);      // 临时去掉 "xxx."
        mappedHost = exactFindIgnoreCase(hosts, host);
        host.setStart(start);                 // 恢复
    }
    if (mappedHost == null) {
        mappedHost = defaultHost;             // ★ 兜底：默认域名
        if (mappedHost == null) return;
    }
}
mappingData.host = mappedHost.object;
```

**Host 匹配顺序**：精确域名（忽略大小写）→ 通配域名（去首段）→ 默认域名

### 4.3 第二级：Context 匹配（internalMap 中段，733-812 行）

这是 Mapper 最精巧的部分——**"最长前缀 + 逐级回退"**：

```java
ContextList contextList = mappedHost.contextList;
MappedContext[] contexts = contextList.contexts;
int pos = find(contexts, uri);          // ★ 二分找到"最接近的较小或相等"的 Context
if (pos == -1) return;

int lastSlash = -1;
int uriEnd = uri.getEnd();
boolean found = false;
MappedContext context = null;
while (pos >= 0) {
    context = contexts[pos];
    if (uri.startsWith(context.name)) {                  // URI 以 Context 路径开头
        length = context.name.length();
        if (uri.getLength() == length) {                 // 完全相等 → 命中
            found = true;
            break;
        } else if (uri.startsWithIgnoreCase("/", length)) {  // 后面紧跟 / → 命中
            found = true;
            break;
        }
    }
    // ★ 未命中：截断 URI 到上一层斜杠，回退再找
    if (lastSlash == -1) {
        lastSlash = nthSlash(uri, contextList.nesting + 1);   // 第一次：截到最深层
    } else {
        lastSlash = lastSlash(uri);                            // 之后：逐级向上
    }
    uri.setEnd(lastSlash);
    pos = find(contexts, uri);
}
uri.setEnd(uriEnd);   // 恢复

if (!found) {
    if (contexts[0].name.equals("")) {
        context = contexts[0];        // 兜底：根 Context（""）
    } else {
        context = null;
    }
}
if (context == null) return;

// 多版本选择：默认取最新版本
ContextVersion contextVersion = null;
ContextVersion[] contextVersions = context.versions;
if (contextVersions.length > 1) {
    // 记录所有版本（Session 重映射用，见第 6 节）
    mappingData.contexts = contextObjects;
    if (version != null) {
        contextVersion = exactFind(contextVersions, version);   // 按版本号找
    }
}
if (contextVersion == null) {
    contextVersion = contextVersions[contextVersions.length - 1];  // 最新版本
}
mappingData.context = contextVersion.object;
mappingData.contextSlashCount = contextVersion.slashCount;
```

**举例**（Context 路径：`""`、`/app`、`/app/api`，请求 URI `/app/api/users`）：

```
find(contexts, "/app/api/users") → 最接近的较小项是 "/app/api"
  ✓ uri.startsWith("/app/api") 且后面是 "/" → 命中 /app/api
```

**举例**（请求 URI `/app/users`，没有 `/app/users` Context）：

```
find → "/app/api"（最接近较小项）
  ✗ "/app/users" 不以 "/app/api" 开头
  → 截断 URI 到上一层斜杠 → "/app"
  find(contexts, "/app") → "/app"
  ✓ 命中 /app
```

**举例**（请求 URI `/other`，没有匹配 Context，有根 Context `""`）：

```
find → 回退到 "/" → 没有匹配 → contexts[0].name=="" → 用根 Context
```

> **面试点**：Context 匹配为什么是"最长前缀 + 回退"？——Servlet 规范要求 **最长的 Context 路径优先**
> （`/app/api` 和 `/app` 都部署时，`/app/api/xxx` 必须路由到 `/app/api`）。
> 二分查找先定位"最接近"的项，再逐级截断回退，保证 O(log n) 找到最长匹配。

### 4.4 第三级：Wrapper 匹配（internalMapWrapper，820-991 行）

```java
private void internalMapWrapper(ContextVersion contextVersion, CharChunk path, MappingData mappingData)
        throws IOException {

    int pathStart = path.getStart();
    int pathEnd = path.getEnd();
    boolean noServletPath = false;
    int length = contextVersion.path.length();
    if (length == (pathEnd - pathStart)) {
        noServletPath = true;              // URI 等于 Context 路径本身
    }
    int servletPath = pathStart + length;  // 去掉 Context 前缀
    path.setStart(servletPath);

    // Rule 1 -- Exact Match（精确匹配）
    internalMapExactWrapper(contextVersion.exactWrappers, path, mappingData);

    // Rule 2 -- Prefix Match（前缀匹配 /foo/*）
    if (mappingData.wrapper == null) {
        internalMapWildcardWrapper(contextVersion.wildcardWrappers, contextVersion.nesting, path, mappingData);
        ...
    }

    // Context 根路径重定向（/app → /app/）
    if (mappingData.wrapper == null && noServletPath && contextVersion.object.getMapperContextRootRedirectEnabled()) {
        path.append('/');
        mappingData.redirectPath.setChars(...);
        return;
    }

    // Rule 3 -- Extension Match（后缀匹配 *.ext）
    if (mappingData.wrapper == null && !checkJspWelcomeFiles) {
        internalMapExtensionWrapper(contextVersion.extensionWrappers, path, mappingData, true);
    }

    // Rule 4 -- Welcome resources（欢迎文件：/app/ → /app/index.html）
    if (mappingData.wrapper == null) {
        // 遍历 welcomeResources，尝试 exact → wildcard → extension 匹配
        ...
    }

    // Rule 7 -- Default servlet（默认 Servlet /）
    if (mappingData.wrapper == null) {
        if (contextVersion.defaultWrapper != null) {
            mappingData.wrapper = contextVersion.defaultWrapper.object;
            mappingData.matchType = MappingMatch.DEFAULT;
        }
        // 目录重定向：/app/foo（无尾斜杠，是目录）→ /app/foo/
        if (contextVersion.resources != null && buf[pathEnd - 1] != '/') {
            WebResource file = contextVersion.resources.getResource(pathStr);
            if (file != null && file.isDirectory()) {
                path.append('/');
                mappingData.redirectPath.setChars(...);
            }
        }
    }
    path.setStart(pathStart);
    path.setEnd(pathEnd);
}
```

**注意**：源码注释中规则编号是 **Rule 1/2/3/4/7**（没有 5/6）——因为历史上规则 5/6 是 JSP 相关的
（`jsp-property-group`），已合并进规则 2/4 的处理中（`checkJspWelcomeFiles`）。

---

## 5. Wrapper 的五条匹配规则（Servlet 规范映射）

### 5.1 三条核心匹配算法

**① 精确匹配 internalMapExactWrapper**（997-1012 行）：

```java
private void internalMapExactWrapper(MappedWrapper[] wrappers, CharChunk path, MappingData mappingData) {
    MappedWrapper wrapper = exactFind(wrappers, path);     // 二分精确查找
    if (wrapper != null) {
        mappingData.requestPath.setString(wrapper.name);
        mappingData.wrapper = wrapper.object;
        if (path.equals("/")) {
            // 根路径映射（Context 根）：Servlet 4.0 的 CONTEXT_ROOT
            mappingData.pathInfo.setString("/");
            mappingData.wrapperPath.setString("");
            mappingData.matchType = MappingMatch.CONTEXT_ROOT;
        } else {
            mappingData.wrapperPath.setString(wrapper.name);
            mappingData.matchType = MappingMatch.EXACT;
        }
    }
}
```

**② 前缀匹配 internalMapWildcardWrapper**（1018-1060 行）——与 Context 匹配同款"回退"技巧：

```java
private void internalMapWildcardWrapper(MappedWrapper[] wrappers, int nesting, CharChunk path,
        MappingData mappingData) {

    int pos = find(wrappers, path);
    if (pos != -1) {
        boolean found = false;
        while (pos >= 0) {
            if (path.startsWith(wrappers[pos].name)) {        // 前缀命中
                length = wrappers[pos].name.length();
                if (path.getLength() == length || path.startsWithIgnoreCase("/", length)) {
                    found = true;
                    break;
                }
            }
            // 回退：截到上一层斜杠
            path.setEnd(lastSlash);
            pos = find(wrappers, path);
        }
        path.setEnd(pathEnd);
        if (found) {
            mappingData.wrapperPath.setString(wrappers[pos].name);
            if (path.getLength() > length) {
                // ★ 剩余部分就是 pathInfo（Servlet 规范：/foo/* 中 * 的部分）
                mappingData.pathInfo.setChars(...);
            }
            mappingData.requestPath.setChars(...);
            mappingData.wrapper = wrappers[pos].object;
            mappingData.jspWildCard = wrappers[pos].jspWildCard;
            mappingData.matchType = MappingMatch.PATH;
        }
    }
}
```

**③ 后缀匹配 internalMapExtensionWrapper**（1071-1105 行）：

```java
private void internalMapExtensionWrapper(MappedWrapper[] wrappers, CharChunk path, MappingData mappingData,
        boolean resourceExpected) {
    // 从后往前找最后一个 '/'，再找 '.'，取扩展名
    for (int i = pathEnd - 1; i >= servletPath; i--) {
        if (buf[i] == '/') { slash = i; break; }
    }
    if (slash >= 0) {
        for (int i = pathEnd - 1; i > slash; i--) {
            if (buf[i] == '.') { period = i; break; }
        }
        if (period >= 0) {
            path.setStart(period + 1);      // 只比较扩展名部分
            MappedWrapper wrapper = exactFind(wrappers, path);
            if (wrapper != null && (resourceExpected || !wrapper.resourceOnly)) {
                mappingData.wrapper = wrapper.object;
                mappingData.matchType = MappingMatch.EXTENSION;
            }
        }
    }
}
```

### 5.2 完整匹配顺序（Servlet 规范）

```
请求 /app/foo/bar.do 到达（假设 /app Context、foo 是精确 Servlet、*.do 是扩展 Servlet）

1. Rule 1 Exact      /app/foo/bar.do 精确匹配？          → 不中
2. Rule 2 Prefix     /app/foo/* 前缀匹配？               → 不中（若命中，剩余为 pathInfo）
3. Context 根重定向  URI == Context 路径 → 重定向加 "/"
4. Rule 3 Extension  *.do 后缀匹配？                     → ★ 命中（matchType=EXTENSION）
5. Rule 4 Welcome    以 / 结尾 → 尝试欢迎文件（index.html 等）
6. Rule 7 Default    / 默认 Servlet 兜底                 → 命中（matchType=DEFAULT）
```

**MappingMatch 五种类型**（`jakarta.servlet.http.MappingMatch`，Servlet 4.0）：

| 类型 | 含义 | 触发规则 |
|---|---|---|
| `CONTEXT_ROOT` | Context 根映射 | Rule 1 且路径为 `/` |
| `EXACT` | 精确匹配 | Rule 1 |
| `PATH` | 路径前缀匹配 | Rule 2（`/foo/*`） |
| `EXTENSION` | 扩展名匹配 | Rule 3（`*.ext`） |
| `DEFAULT` | 默认 Servlet | Rule 7（`/`） |

> **面试点**：`getServletPath()` / `getPathInfo()` / `getRequestURI()` 的关系？
> ——`getServletPath()` = 映射到 Servlet 的路径（`mappingData.wrapperPath`，`Request.java:2187`），
> `getPathInfo()` = 前缀匹配的剩余部分（`mappingData.pathInfo`，`Request.java:2120`，仅 PATH 匹配时有值），
> `getRequestURI()` = **coyote 层的原始 URI**（`coyoteRequest.requestURI()`，`Request.java:2175`，含 Context 前缀、未解码）。
> 注意：`mappingData.requestPath` 不是 `getRequestURI()`，而是内部 `getRequestPathMB()`（`Request.java:2163`）。
> 三者来源不同，容易混淆。

---

## 6. CoyoteAdapter 中的调用与二次映射

### 6.1 调用点：postParseRequest

**源码**：`CoyoteAdapter.java:559-845`（287 行，复杂度 44）

```java
protected boolean postParseRequest(org.apache.coyote.Request req, Request request,
        org.apache.coyote.Response res, Response response) throws IOException, ServletException {

    // 1. scheme/secure 处理（https 判断）
    if (req.scheme().isNull()) {
        req.scheme().setString(connector.getScheme());
        request.setSecure(connector.getSecure());
    } else {
        request.setSecure(req.scheme().equals("https"));
    }

    // 2. proxyName/proxyPort（反向代理配置）
    ...

    // 3. URI 处理：path 参数剥离 → %xx 解码 → 规范化（//、..、.）
    ...
    parsePathParameters(req, request);        // 去掉 ;jsessionid=xxx 等路径参数
    req.getURLDecoder().convert(...);         // %xx 解码
    normalize(req.decodedURI(), ...);         // 规范化（// → /、/./ → /、/../ 处理）

    // 4. ★ 获取域名（虚拟主机）
    MessageBytes serverName;
    if (connector.getUseIPVHosts()) {
        serverName = req.localName();         // IP 虚拟主机
    } else {
        serverName = req.serverName();        // 常规域名
    }

    // 5. ★★ Mapper 映射（本篇核心）
    String version = null;
    Context versionContext = null;
    boolean mapRequired = true;
    while (mapRequired) {
        connector.getService().getMapper().map(serverName, decodedURI, version, request.getMappingData());

        // Context 为 null：404 或根 Context 未部署
        if (request.getContext() == null) {
            return true;      // 交给 Valve 层处理（EngineValve/HostValve 会发 404）
        }

        // Session ID 解析（URL → Cookie → SSL）
        parseSessionCookiesId(request);
        parseSessionSslId(request);
        sessionID = request.getRequestedSessionId();

        mapRequired = false;
        if (version != null && request.getContext() == versionContext) {
            // 已拿到想要的版本
        } else {
            // ★ 多版本二次映射：Session 属于哪个 Context 版本就用哪个
            Context[] contexts = request.getMappingData().contexts;
            if (contexts != null && sessionID != null) {
                for (int i = contexts.length; i > 0; i--) {
                    Context ctxt = contexts[i - 1];
                    if (ctxt.getManager().findSession(sessionID) != null) {
                        if (!ctxt.equals(request.getMappingData().context)) {
                            version = ctxt.getWebappVersion();
                            versionContext = ctxt;
                            request.getMappingData().recycle();   // 重置
                            mapRequired = true;                   // 二次映射
                        }
                        break;
                    }
                }
            }
        }

        // Context 处于暂停（重载中）→ 等待后重试
        if (!mapRequired && request.getContext().getPaused()) {
            Thread.sleep(1000);
            request.getMappingData().recycle();
            mapRequired = true;
        }
    }

    // 6. 目录重定向（/app/foo 是目录 → 302 到 /app/foo/）
    MessageBytes redirectPathMB = request.getMappingData().redirectPath;
    if (!redirectPathMB.isNull()) {
        response.sendRedirect(URLEncoder.DEFAULT.encode(redirectPathMB.toString(), StandardCharsets.UTF_8));
        return false;     // 不进入容器，直接返回
    }

    // 7. TRACE 过滤（Connector 未开启 allowTrace 时拒绝）
    if (!connector.getAllowTrace() && req.method().equals("TRACE")) {
        response.sendError(405, ...);
        return true;
    }

    // 8. 连接器级认证/授权（doConnectorAuthenticationAuthorization）
    ...
    return true;   // 映射成功，进入 Valve 链
}
```

### 6.2 二次映射（多版本部署）

**多版本部署**（`<Context version="v2">`）：同一 Context 路径可有多个版本，
`Mapper.map()` 默认返回**最新版本**，但 `mappingData.contexts` 记录了所有版本。

如果请求带着 Session ID，且该 Session 属于**旧版本**的 Context——`postParseRequest`
会检测到并**二次映射**到正确版本：

```java
Context[] contexts = request.getMappingData().contexts;   // 所有版本
if (contexts != null && sessionID != null) {
    for (int i = contexts.length; i > 0; i--) {
        Context ctxt = contexts[i - 1];
        if (ctxt.getManager().findSession(sessionID) != null) {   // Session 在哪个版本？
            if (!ctxt.equals(request.getMappingData().context)) {
                version = ctxt.getWebappVersion();     // 记住目标版本
                mapRequired = true;                    // 重新 map
            }
            break;
        }
    }
}
```

> **面试点**：为什么 `map()` 要用 while 循环？——多版本部署 + Session 亲和性需要**二次映射**：
> 第一次映射默认取最新版本，发现 Session 属于旧版本后重置映射数据重来一次。
> 嵌入式中无多版本部署，但代码路径一致。

---

## 7. 本篇小结与面试要点

### 7.1 本篇地图

```
第 1 篇：容器树 + Lifecycle
第 2 篇：Pipeline-Valve（request.getHost() 的消费方）
第 3 篇（本篇）：Mapper 路由（request.getHost() 的生产方）
第 4 篇：Servlet 生命周期（allocate 的调用方在 WrapperValve）
第 5 篇：Connector（CoyoteAdapter 的调用者）
第 6 篇：NIO 网络层（请求从哪里来）
```

### 7.2 面试要点速查

1. **Mapper 的职责**：维护路由表 + 执行 `(域名, URI) → (Host, Context, Wrapper)` 映射；每 Service 一个
2. **四层数据结构**：`MappedHost[]` → `ContextList` → `MappedContext` → `ContextVersion`（含四类 Wrapper 桶）
3. **有序数组 + 二分**：`find()` 返回"最接近的较小或相等"项，`exactFind()` 再校验；无锁并发读
4. **Host 匹配**：精确（忽略大小写）→ 通配（去首段）→ 默认域名
5. **Context 匹配**：最长前缀 + 逐级回退（`nthSlash`/`lastSlash` 截断 URI）
6. **Wrapper 匹配**：Rule 1 精确 → Rule 2 前缀 → Context 根重定向 → Rule 3 后缀 → Rule 4 欢迎文件 → Rule 7 默认 Servlet
7. **MappingMatch 五类型**：CONTEXT_ROOT/EXACT/PATH/EXTENSION/DEFAULT（Servlet 4.0）
8. **MapperListener 双通道**：ContainerListener（容器事件）+ LifecycleListener（AFTER_START/BEFORE_STOP）
9. **registerContext 归一化**：根 Context 路径 `/` → `""`
10. **二次映射**：多版本部署 + Session 亲和性 → while 循环重映射
11. **`/` 与 `""` 的区别**：`/` 是默认 Servlet 映射，`""` 是 Context 根路径（CONTEXT_ROOT）
