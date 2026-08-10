# T-5 Mapper 路由 — 知识规划

> 项目: Tomcat 10.1.x | 类型: Tomcat 自身设计(无Servlet规范对应) | 🟡B 域 / 预计 2 篇
> 核心文件: Mapper(1655行)/MappingData(62行)/MapperListener(512行) | ~2229 行
> 基线: T-4 线程模型 — 请求在 Worker 线程执行 Pipeline.invoke() — invoke() 内第一步: "这个 URI 对应哪个 Host/Context/Wrapper？" — 答案是 Mapper

---

## §0.8 域审核

- 核心类: 3 个, 2229 行 — 🟡B 域, 无需拆分
- 淘汰检查: 无 — Mapper 路由是 Tomcat 核心, Spring Boot 不做替代
- 规范对应: 无 — Servlet 规范未定义路由机制, Mapper 是 Tomcat 自身设计

---

## 01 提取

### Mapper.java (1655 行 — 四级匹配路由引擎)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Mapper.java:654 | **map(host, uri, version, mappingData)** — 对外入口, T-2 CoyoteAdapter 调用 | High |
| Mapper.java:834-836 | **Rule 1 Exact Match**: `exactWrappers` — 精确匹配 `/app/user` | High |
| Mapper.java:838-842 | **Rule 2 Prefix/Wildcard Match**: `wildcardWrappers` — 前缀匹配 `/api/*` | High |
| Mapper.java:871-874 | **Rule 3 Extension Match**: `extensionWrappers` — 后缀匹配 `*.jsp` | High |
| Mapper.java:877-890 | **Rule 4 Welcome Resources**: 无匹配→后缀 `/` → try welcome files (index.html) | High |
| Mapper.java:820 | **internalMapWrapper()** — 四级匹配的主方法, 按序执行 | High |
| Mapper.java:101 | **addHost()**: 注册 Host 到 MappedHost[] 数组 | High |

### MappingData.java (62 行 — 映射结果对象)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| MappingData.java:30-33 | **host/context/wrapper** — 三个容器引用, Mapper 映射的产物 | High |
| MappingData.java:55-62 | **recycle()** — 清零所有字段, 配合 Request.recycle() | High |

### MapperListener.java (512 行 — 动态路由更新)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| MapperListener.java:147 | **containerEvent()**: 监听 ADD_CHILD_EVENT/REMOVE_CHILD_EVENT → 动态更新 Mapper 路由表 | High |
| MapperListener.java:158 | **registerContext()**: 新 Context 添加→`mapper.addContextVersion()` | High |
| MapperListener.java:392 | **unregisterContext()**: Context 移除→`mapper.removeContextVersion()` | High |

---

## 02-03 聚合+分类

| KP 群 | 级别 | 理由 |
|------|:--:|------|
| **四级匹配算法 (Exact→Prefix→Extension→Welcome)** | 🟡 | Tomcat 自创, 概念清晰 — Servlet 规范无此定义, 优先级规则参考了 Servlet 2.3+ url-pattern 语义 |
| **MapperListener 动态更新** | 🟡 | ContainerEvent 观察者 — 与 T-1 的 addChild 事件和 T-3 的 pipeline 紧密关联 |

---

## 04 聚类

**Cluster A: 四级匹配算法 + 路由表结构** (→ §1)
- MappedHost[] → ContextVersion[] → MappedWrapper[] — 三层路由表
- Exact→Prefix→Extension→Welcome 四级匹配顺序
- map() 方法: host 查找→ContextPath 匹配→Wrapper 匹配→填充 mappingData

**Cluster B: MapperListener 动态路由 + 与其他域的连接** (→ §2)
- MapperListener 订阅 ContainerEvent(ADD_CHILD/REMOVE_CHILD)
- addHost/addContextVersion/addWrapper — 动态构建路由表
- 与 T-1 Container 树 + T-2 Adapter.map() + T-3 Pipeline 的完整回调链

> → Tomcat Stage 1 全部 5 域完成。从 TCP accept(T-4)→二进制帧解析(T-2)→Engine Pipeline(T-3)→四级 Mapper 路由(T-5)→Container 树(T-1)→FilterChain→Servlet。Spring 生态 Stage 1(I/O 基础)学完: Netty 13章 + Tomcat 5域。
