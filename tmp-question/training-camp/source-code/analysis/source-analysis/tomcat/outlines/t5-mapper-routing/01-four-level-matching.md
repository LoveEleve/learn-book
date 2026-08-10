# T-5 §1 四级匹配算法 — Exact→Prefix→Extension→Welcome

> 依赖 T-3 §2 | 🟡 Working | 2 KP

**读者处境**: T-2 CoyoteAdapter 调了 `mapper.map(host, uri, version, mappingData)` — 这行代码怎么从 "/app/user" 找到 Context="/app" 和 Wrapper="user"？Mapper 不是数据库查询 — 是**数组遍历 + 字符匹配**的四级算法。

### 1. 路由表结构 — MappedHost[] → ContextVersion[] → MappedWrapper[]

场景: 一个 Tomcat 实例有 3 个 Host(example.com/test.com/localhost) — 每个 Host 有 5 个 Context(/app/admin/manager/docs/ROOT) — 每个 Context 有 10 个 Wrapper。Mapper 的路由表是三层数组 — 不是 HashMap — 因为 Mapper 需要做前缀匹配而非精确查找。

源码路径:
- `Mapper.java:101` — **addHost()**: 在 `MappedHost[] hosts` 数组中按名排序插入 Host
- `Mapper.java:255` — **addContextVersion()**: 在 Host 的 `ContextList.contexts[]` 中按 path 排序插入 Context
- `Mapper.java:820` — **internalMapWrapper()**: 从 `ContextVersion.exactWrappers[]` / `wildcardWrappers[]` / `extensionWrappers[]` 三个数组中查找

关键设计: **Why 数组而非 HashMap？** URI 匹配需要前缀匹配(如 /app/user 匹配前缀 /app/*) — HashMap 只能做精确匹配。数组 + 二分查找: `Arrays.binarySearch()` 定位到路径的字典序位置 — 前缀匹配从该位置向前遍历 — 找到最长前缀匹配。Extension 匹配(`*.jsp`)从后往前查后缀。

数据流: `mapper.map("example.com", "/app/user", "HTTP/1.1", mappingData)`→`MappedHost[] hosts` 二分查 "example.com"→找到 MappedHost→`ContextList.contexts[]` 遍历匹配 "/app"→找到 ContextVersion→`internalMapWrapper()`→Rule 1: `exactWrappers[]` 查 "/user"→未命中→Rule 2: `wildcardWrappers[]` 查最长前缀 "/user"→匹配 "/*"→mappingData.wrapper = "user" Wrapper→返回。

### 2. 四级匹配顺序 — 为什么要分四级？

场景: Servlet 规范 2.3 定义了 url-pattern 的优先级规则: `/app/user` 精确匹配高于 `/app/*` 前缀匹配 — 高于 `*.jsp` 扩展匹配 — 最后是 `/` 默认 Servlet。Mapper 的 internalMapWrapper 完全遵照这个优先级:

源码路径:
- `Mapper.java:834-836` — **Rule 1 Exact Match**: `internalMapExactWrapper(exactWrappers, path, mappingData)` — 遍历 exactWrappers 数组, 精确字符匹配
- `Mapper.java:838-858` — **Rule 2 Prefix/Wildcard Match**: `internalMapWildcardWrapper(wildcardWrappers, ...)` — 遍历 wildcardWrappers, 最长前缀优先
- `Mapper.java:871-874` — **Rule 3 Extension Match**: `internalMapExtensionWrapper(extensionWrappers, ...)` — 后缀匹配 `*.jsp`/`*.do`
- `Mapper.java:877-890` — **Rule 4 Welcome Resources**: 无匹配→路径以 `/` 结尾→`contextVersion.welcomeResources[]` 逐条尝试

关键设计: **Why Exact 先于 Wildcard？** 如果 `/app/user` 同时匹配 exact `/app/user` 和 wildcard `/app/*` — Servlet 规范要求 exact 优先(因为匹配更精确)。如果 wildcard 先执行 — exact 永远不会被命中 — 请求 `GET /app/user` 被映射到 `/*` 的 DefaultServlet — 等于所有的精确匹配都失效了。

数据流: `GET /app/user.jsp`→exactWrappers 查 "/user.jsp"→命中 JspServlet→直接返回。`GET /app/user`→exact 未命中→wildcard 查 "/user"→匹配 "/*"→DefaultServlet→返回。`GET /style.css`→exact/wildcard 未命中→extension 查 ".css"→未定义→welcome 未激活(路径不以/结尾)→mappingData.wrapper=null→404 Not Found。

→ 引出 §2 MapperListener — 路由表不是静态的 — Context 可以在运行时动态添加(热部署)。新 Context 添加后 — 如何同步更新 Mapper 的 hosts[]/contexts[]/wrappers[] 三层数组？
