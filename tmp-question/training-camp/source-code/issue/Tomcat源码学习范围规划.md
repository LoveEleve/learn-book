# Tomcat 源码学习范围规划

> **版本**: v10.1.34
> **仓库**: `/data/workspace/source-code/code/spring/tomcat/`
> **规模**: 大项目，核心在 catalina + coyote
> **日期**: 2026-08-03
> **R4**: 2026-08-04 — MCP 深度审查: 发现 catalina 下 session/11文件 + mapper/ 未覆盖; 新增 T-5(Session管理)+T-6(Mapper URL路由); 4→6域

---

## 一、仓库概况

Apache Tomcat 是 Java Servlet 容器，Spring Boot 默认嵌入。核心三层：**Connector(coyote—HTTP/AJP 协议) → Container(catalina—Engine→Host→Context→Wrapper 四层容器) → Servlet**。Spring Boot 启动时通过 `TomcatServletWebServerFactory` 创建嵌入式 Tomcat 实例。

**核心模块**：

| 模块 | 职责 | 关键类 |
|---|---|---|
| `catalina/` | Servlet 容器：Server→Service→Engine→Host→Context→Wrapper | StandardServer, StandardEngine, StandardHost, StandardContext, StandardWrapper |
| `coyote/` | HTTP 协议：Connector→ProtocolHandler→Processor→Adapter | Http11NioProtocol, Http11Processor, CoyoteAdapter |
| `el/` | EL 表达式引擎 | —（淘汰）|
| `jasper/` | JSP 编译器 | —（淘汰）|
| `juli/` | 日志系统 | —（淘汰）|
| `naming/` | JNDI | —（淘汰）|

---

## 二、知识域规划（机制闭环版）

> 这一版不再把目录边界直接当知识域边界，而是按“读者真实困惑 + 跨包主线 + 可独立成篇的机制闭环”重组。

### 🔴 核心域（3 个）

| 编号 | 域 | 核心类 | 核心问题 |
|:---:|---|---|---|
| T-1 | **启动与装配闭环** | Tomcat, StandardServer, StandardService, StandardEngine, StandardHost, StandardContext, StandardWrapper, Connector, MapperListener | 一个嵌入式 Tomcat 为什么从 `Tomcat.start()` 出发，就能把容器树、Connector、Mapper 监听和请求入口全部接起来？这一域不是只讲容器层次，而是讲“配置对象如何变成可收请求的运行时装配”。 |
| T-2 | **请求进入与协议处理闭环** | Connector, ProtocolHandler, Http11NioProtocol, NioEndpoint, Http11Processor, CoyoteAdapter | 一个 HTTP 请求进入 Tomcat 时，连接如何被接收、事件如何被分发、协议如何被解析、又是在哪里从 Coyote 世界切到 Catalina 世界？这一域吸收原 T-4 的线程模型，不再把线程与协议处理拆开。 |
| T-3 | **容器执行闭环：Mapper + Pipeline-Valve + FilterChain + Servlet** | Mapper, MapperListener, MappingData, Pipeline, Valve, StandardEngineValve, StandardHostValve, StandardContextValve, StandardWrapperValve, ApplicationFilterChain, ApplicationFilterFactory | 一个请求在适配成 Catalina Request/Response 后，如何完成 Host/Context/Wrapper 路由，如何穿过 Engine→Host→Context→Wrapper Valve 链，再进入 FilterChain，最终到 `Servlet.service()`？这一域把原 T-3 与 T-6 的主线合并看待。 |

### 🟡 扩展域（3 个）

| 编号 | 域 | 核心类 | 核心问题 |
|:---:|---|---|---|
| T-4 | **异步、超时与错误处理闭环** | AsyncStateMachine, AsyncContextImpl, AbstractProcessor, StandardHostValve, ErrorReportValve, ApplicationFilterChain | 异步请求为什么不只是 `startAsync()` 一个 API，而是会深入协议层状态机、容器线程、错误出口和重新分派流程？超时、dispatch、complete、onError、异常页是如何串起来的？ |
| T-5 | **Session 生命周期闭环** | StandardManager, PersistentManager, ManagerBase, StandardSession, FileStore, JDBCStore, DeltaManager, BackupManager | Session 从创建、访问、属性更新、失效、过期清理，到持久化和集群复制是怎么走完一生的？容器 Session 与 Spring Session 替换边界又在哪里？ |
| T-6 | **Mapper 路由与动态更新专题** | Mapper, MapperListener, MappingData | 路由树本身如何维护？`Exact / Prefix / Extension / Default` 四级匹配如何落地？Context/Wrapper 启停后 Mapper 为什么能热更新？这一域保留为专题，但它在正文里必须挂回 T-1/T-3 主线，而不是孤立成目录知识。 |

### 当前顺序说明

- **全书顺序**仍服从总执行计划，Tomcat 是否先写由全局排期决定。
- **Tomcat 内部顺序**建议优先：`T-1 -> T-2 -> T-3 -> T-4 -> T-5 -> T-6`
- 原因：先建立启动与请求主线，再进入异步/错误和生命周期专题，最后再单独深挖 Mapper 专题。
---

## 三、排除与暂缓边界

### 当前主线
- `catalina/core/`
- `catalina/connector/`
- `catalina/mapper/`
- `catalina/session/`
- `coyote/`
- `coyote/http11/`
- `org/apache/tomcat/util/net/`

### 已扫描但暂缓专题化
| 模块 | 说明 |
|---|---|
| `coyote/http2/` | 当前不是第一主线，但测试已显示它和 async/request lifecycle 有交叉，不能简单标“淘汰” |
| `catalina/valves/` | 不能整体排除；至少 `ErrorReportValve` 已进入异步/错误处理主线 |
| `catalina/realm/` | 安全认证是重要主题，但不进入第一批主线 |
| `tribes/`、`ha/` | 集群复制能力重要，但放在 Session 主线之后专题化 |

### 当前确认不进入第一批主线
| 模块 | 理由 |
|---|---|
| `jasper/` | JSP 编译器，不属于当前核心主线 |
| `el/` | EL 表达式引擎，独立性强 |
| `juli/` | 日志实现，不是容器/协议主线 |
| `naming/` | JNDI，对嵌入式 Spring Boot 主线不关键 |
| `coyote/ajp/` | 协议专项，可后续独立处理 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 3 |
| 🟡 扩展域 | 3 |
| **总域** | **6** |

以上规划当前共 **3🔴 + 3🟡 = 6 域**。

---

## 五、本轮源码扫描补充发现

> 这部分基于对 `java/` 和 `test/` 的实际扫描，不是只看目录名推断。

### 1. 当前 6 域更像“目录分块”，还不是“机制闭环”

本轮实扫可以确认，Tomcat 至少存在几条明显跨包主线：

- **启动主线**：`org.apache.catalina.startup.Tomcat` → `StandardServer` → `StandardService` → `Connector` → `MapperListener`
  - 证据：`java/org/apache/catalina/startup/Tomcat.java:137`
  - 证据：`java/org/apache/catalina/core/StandardService.java:103`
  - 证据：`java/org/apache/catalina/connector/Connector.java:999`

- **请求主线**：`NioEndpoint` → `Http11Processor` → `CoyoteAdapter` → `Mapper` / Container Pipeline → `ApplicationFilterChain` → `Servlet.service()`
  - 证据：`java/org/apache/tomcat/util/net/NioEndpoint.java:71`
  - 证据：`java/org/apache/coyote/http11/Http11Processor.java:70`
  - 证据：`java/org/apache/catalina/connector/CoyoteAdapter.java:64`
  - 证据：`java/org/apache/catalina/mapper/Mapper.java:47`
  - 证据：`java/org/apache/catalina/core/ApplicationFilterChain.java:46`

- **容器责任链主线**：`StandardEngineValve` → `StandardHostValve` → `StandardContextValve` → `StandardWrapperValve`
  - 证据：`java/org/apache/catalina/core/StandardEngineValve.java:35`
  - 证据：`java/org/apache/catalina/core/StandardHostValve.java:50`
  - 证据：`java/org/apache/catalina/core/StandardContextValve.java:40`
  - 证据：`java/org/apache/catalina/core/StandardWrapperValve.java:50`

- **异步与错误处理主线**：`AsyncStateMachine`、`AsyncContextImpl`、`ApplicationFilterChain`、`ErrorReportValve`、`StandardHostValve`
  - 证据：`java/org/apache/coyote/AsyncStateMachine.java:129`
  - 证据：`java/org/apache/catalina/core/AsyncContextImpl.java:441`
  - 证据：`java/org/apache/catalina/valves/ErrorReportValve.java:61`

这说明当前规划虽然列了 Container / Connector / Pipeline / Session / Mapper，但还没有把“启动”“请求处理”“异步错误”这些读者真正关心的机制闭环明确升格。

### 2. 启动主线比当前“T-1 嵌入式启动”描述更跨层

当前 `T-1` 已经意识到嵌入式启动重要，但本轮实扫确认：启动链不是单纯的容器层次说明，而是至少横跨 `startup/core/connector/mapper` 四层。

关键连接点：
- `Tomcat.start()` 是外层入口
  - 证据：`java/org/apache/catalina/startup/Tomcat.java:435`
- `StandardService` 内部直接持有 `Mapper` 和 `MapperListener`
  - 证据：`java/org/apache/catalina/core/StandardService.java:97`
  - 证据：`java/org/apache/catalina/core/StandardService.java:103`
- 切换 Engine 时，`MapperListener` 还需要 stop/start 以重新接线
  - 证据：`java/org/apache/catalina/core/StandardService.java:151`
- `Connector.initInternal()` 里会创建 `CoyoteAdapter` 并挂到 `ProtocolHandler`
  - 证据：`java/org/apache/catalina/connector/Connector.java:999`
  - 证据：`java/org/apache/catalina/connector/Connector.java:1000`

这说明启动域后续不能只停留在 `Server -> Service -> Engine -> Host -> Context -> Wrapper` 的静态层次，而必须把 `Connector`、`Adapter`、`MapperListener` 这些“从配置走向可收请求”的桥接角色一起纳入。

### 3. 请求主线比当前 T-2/T-3/T-6 的拆法更一体化

当前规划把 `Connector + HTTP/1.1`、`Pipeline-Valve`、`Mapper` 分成三个域，这对盘点有帮助，但对读者理解“一个请求到底怎么走”是不够的。

本轮源码扫描确认的真实主线是：

- `NioEndpoint` 接收连接
  - 证据：`java/org/apache/tomcat/util/net/NioEndpoint.java:71`
- `Http11Processor` 处理 HTTP/1.1 协议
  - 证据：`java/org/apache/coyote/http11/Http11Processor.java:70`
- `CoyoteAdapter` 把协议层请求转给 Catalina 容器
  - 证据：`java/org/apache/catalina/connector/CoyoteAdapter.java:64`
- `Mapper` / `MapperListener` 提供 Host / Context / Wrapper 路由
  - 证据：`java/org/apache/catalina/mapper/Mapper.java:47`
  - 证据：`java/org/apache/catalina/mapper/MapperListener.java:47`
- `StandardEngineValve -> StandardHostValve -> StandardContextValve -> StandardWrapperValve` 形成容器责任链
  - 证据：`java/org/apache/catalina/core/StandardEngineValve.java:35`
  - 证据：`java/org/apache/catalina/core/StandardHostValve.java:50`
  - 证据：`java/org/apache/catalina/core/StandardContextValve.java:40`
  - 证据：`java/org/apache/catalina/core/StandardWrapperValve.java:50`
- `StandardWrapperValve` 再创建 `ApplicationFilterChain`，最后调用 `filterChain.doFilter()` 进入 Servlet
  - 证据：`java/org/apache/catalina/core/StandardWrapperValve.java:141`
  - 证据：`java/org/apache/catalina/core/StandardWrapperValve.java:155`
- `ApplicationFilterFactory.createFilterChain(...)` 是 Filter 链装配点
  - 证据：`java/org/apache/catalina/core/ApplicationFilterFactory.java:57`

这意味着后续正文不能把 `Mapper`、`Pipeline-Valve`、`FilterChain` 分散写成彼此孤立的章节，否则读者会知道每个零件，但仍然讲不出完整请求主线。

### 4. 异步与错误处理主线当前明显被低估

本轮实扫确认，Tomcat 的 async 不是 Servlet API 附属说明，而是一条深入协议层、容器层和错误出口的主线：

- `AbstractProcessor` 构造时直接创建 `AsyncStateMachine`
  - 证据：`java/org/apache/coyote/AbstractProcessor.java:85`
- `AsyncContextImpl` 在异常分支里会显式回调 `StandardHostValve.throwable(...)`
  - 证据：`java/org/apache/catalina/core/AsyncContextImpl.java:442`
- `StandardHostValve` 负责错误出口处理
  - 证据：`java/org/apache/catalina/core/StandardHostValve.java:231`
- `ErrorReportValve` 则提供默认错误页实现
  - 证据：`java/org/apache/catalina/valves/ErrorReportValve.java:61`
- `StandardWrapperValve` 里还要区分普通 `doFilter` 和 `request.isAsyncDispatching()` 的异步分派
  - 证据：`java/org/apache/catalina/core/StandardWrapperValve.java:152`
  - 证据：`java/org/apache/catalina/core/StandardWrapperValve.java:164`

这说明 async 至少不该只是 `Connector` 或 `Servlet` 里的一句补充，它已经足够形成独立域，甚至是 Tomcat 很值得优先写的主题之一。

### 5. 测试目录暴露出的边界，当前规划没有吸收

本轮扫描 `test/` 后，已确认测试不只是补充材料，而是直接暴露机制边界：

- `test/org/apache/coyote/http2/TestAsync.java`：async 行为与 HTTP/2 交互
- `test/org/apache/coyote/TestIoTimeouts.java`：连接超时边界
- `test/org/apache/catalina/mapper/`：路由映射边界
- `test/org/apache/catalina/session/`：session 生命周期与过期/持久化边界
- `test/org/apache/catalina/connector/`、`test/org/apache/coyote/http11/`：请求处理链与协议边界
- `test/org/apache/catalina/valves/`：Valve 责任链与错误出口边界

和 Netty 一样，Tomcat 后续规划不能只扫 `src/main`，必须把测试当设计证据一起纳入。

### 6. Async / FilterChain / ErrorReportValve 当前被低估

从本轮源码与测试联合扫描看，这三块都不是边角实现：

- `ApplicationFilterChain` 是进入 `Servlet.service()` 前的关键运行时桥梁
- `AsyncStateMachine` 直接连接请求生命周期、容器线程、协议层回调
- `ErrorReportValve` 和 `StandardHostValve` 共同构成错误出口处理链

如果后续继续只按当前 6 域推进，正文很容易把这几条主线塞进别的章节里一笔带过，最后出现“类名出现了，但读者问题没闭环”的问题。

---

## 六、旧规划问题复盘（供后续框架规划复用）

### 1. 按目录切域过强，跨包机制没有先画总图

当前 Tomcat 规划基本沿着 `catalina / coyote / session / mapper` 分块，但实际关键主线明显跨包：

- 启动：`startup + core + connector + mapper`
- 请求处理：`util/net + coyote + connector + mapper + core + servlet`
- 异步：`coyote + catalina/core + filter chain + error valve`

后续规划不能把目录边界直接当知识域边界，必须先画“启动主线图”“请求主线图”“异步错误主线图”，再决定域划分。

### 2. 用核心类列表替代读者真实困惑

现在每个域都列了很多核心类，但没有像 Netty 修订版那样，把域直接收束成读者问题：

- 嵌入式 Tomcat 为什么能从 `Tomcat.start()` 走到可接收请求？
- 一个 HTTP 请求是怎么从 Socket 走到 `Servlet.service()` 的？
- `Pipeline-Valve` 和 `ApplicationFilterChain` 到底是什么关系？
- 异步请求为什么会牵扯 `AsyncStateMachine`、`ErrorReportValve` 和容器线程？

后续每个域都应补：
- 读者困惑
- 入口与出口
- 状态/线程/对象边界
- 失败路径
- 至少一个独立成篇的理解闭环

### 3. 把运行时诊断/边界处理当附属细节

Tomcat 里并不只有“正常请求主线”：

- async 状态迁移
- timeout
- 错误页与异常出口
- session 过期清理
- mapper 热更新

这些不是正文末尾的“补充说明”，而是 Tomcat 运行时的重要组成部分。后续域规划应明确哪些边界必须升格为正文主线，避免只讲 happy path。

### 4. 没有把测试目录当作一等证据源

本轮扫描确认，Tomcat 的 async、http11/http2、mapper、session、connector 边界，在测试目录里都有直接证据。后续规划若只依据 `java/` 主码，很容易低估机制边界。

### 5. 排除清单过粗

当前把 `ajp/http2/realm/valves` 直接打成“非核心”，问题是：

- `http2/` 虽然不是当前第一主线，但它至少已经与 async、connector、request lifecycle 测试发生直接连接
- `valves/` 里至少 `ErrorReportValve` 已经和主请求/错误出口主线相连，不能用“内置 Valve 非核心”一笔抹掉

后续应区分：
- 当前主线
- 已扫描但暂缓
- 协议专项
- 平台/集群/扩展能力
- 确认淘汰

### 6. 统计层出现自洽错误

原文统计表写的是 6 域，但总结段还写成 `3🔴+1🟡=4 域`。这属于典型的跨层数字不同步问题，说明 Tomcat 规划在增补 Session / Mapper 后，没有做完最终收口检查。

---

## 七、当前建议的修订方向

基于本轮扫描，Tomcat 后续不建议直接进正文；建议先把域组织方式从“目录块”调整为“机制闭环”。

最低限度应补出的主线是：

1. **启动主线**
   - 嵌入式启动
   - 容器层次装配
   - Connector / MapperListener 挂接

2. **请求处理主线**
   - NioEndpoint / Processor / Adapter
   - Mapper 路由
   - Pipeline-Valve
   - FilterChain
   - Servlet

3. **异步与错误主线**
   - AsyncStateMachine
   - AsyncContextImpl
   - ErrorReportValve / StandardHostValve
   - timeout / dispatch / complete / onError 边界

4. **Session / Mapper / 线程模型**
   - 可以作为专题，但必须挂回上面几条主线，而不是孤立存在

### 当前最值得优先修正的规划缺口顺序

1. **请求处理主线的统一建域**
   - 当前被拆在 T-2 / T-3 / T-6，读者很难从 Socket 一路讲到 Servlet
2. **Async / Error 主线升格**
   - 当前没有独立域，但源码与测试都显示它是高中心性机制
3. **FilterChain 明确升格**
   - 当前被埋在 Pipeline/Wrapper 之后，缺少独立解释
4. **启动主线补齐 Connector / MapperListener 接线关系**
   - 现在更像容器层次介绍，不足以支撑嵌入式 Tomcat 的真实启动闭环
5. **测试证据纳入规划依据**
   - 后续每个域都应至少列出一组测试支撑，避免只靠主码推断

---

## 八、T-1 ~ T-6 五要素卡片

> 对齐《源码范围规划复盘方法论》：每个候选知识域都显式给出“读者问题 / 入口类 / 状态核心 / 失败路径 / 连接点”。

### T-1 启动与装配闭环
- **读者问题**: 嵌入式 Tomcat 为什么从 `Tomcat.start()` 出发，就能把容器树、Connector、MapperListener 和请求入口全部接起来？
- **入口类**: `org.apache.catalina.startup.Tomcat`、`org.apache.catalina.core.StandardService`
- **状态核心**: `StandardService` 持有 `Engine`、`Mapper`、`MapperListener`，并在运行态切换 Engine 时重启 `MapperListener`
- **失败路径**: `engine.start()` / `mapperListener.start()` / `protocolHandler.init()` 启动失败时如何中断装配链
- **连接点**: 连接 T-2（Connector/CoyoteAdapter）、T-6（MapperListener/Mapper）、T-3（容器树最终执行链）

### T-2 请求进入与协议处理闭环
- **读者问题**: 一个 HTTP 请求进入 Tomcat 后，连接如何被接收、解析、切换到 Catalina 世界？
- **入口类**: `Connector`、`NioEndpoint`、`Http11Processor`
- **状态核心**: 连接接收、协议解析、Request/Response 构造、Adapter 切换
- **失败路径**: IO timeout、协议错误、bad request、close now、socket 关闭
- **连接点**: 连接 T-3（CoyoteAdapter -> Mapper/Pipeline/FilterChain）、T-4（AsyncStateMachine 在 Processor 中初始化）

### T-3 容器执行闭环
- **读者问题**: 请求进入 Catalina 后，为什么不是直接调 Servlet，而要经过 Mapper、Valve 链、FilterChain 多层转发？
- **入口类**: `CoyoteAdapter`、`Mapper`、`StandardEngineValve`、`StandardWrapperValve`
- **状态核心**: Host/Context/Wrapper 路由结果、Pipeline 基本 Valve、FilterChain 装配与执行
- **失败路径**: Host/Context/Wrapper 缺失、Filter/Servlet 抛异常、Wrapper 分配失败
- **连接点**: 承接 T-2，连接 T-4（异步 dispatch / throwable），连接 T-6（Mapper 专题）

### T-4 异步、超时与错误处理闭环
- **读者问题**: `startAsync()` 之后，请求为什么会牵扯协议层状态机、容器线程和错误出口？
- **入口类**: `AsyncStateMachine`、`AsyncContextImpl`、`AbstractProcessor`
- **状态核心**: async generation、dispatch / complete / timeout / error 转换、async dispatching 分支
- **失败路径**: timeout、dispatch 异常、onError、错误页转发、容器线程与非容器线程写回边界
- **连接点**: 连接 T-2（Processor 生命周期）、T-3（StandardWrapperValve / FilterChain）、`catalina/valves/ErrorReportValve`

### T-5 Session 生命周期闭环
- **读者问题**: Session 从创建到过期、持久化、集群复制，Tomcat 自己负责哪些阶段？
- **入口类**: `StandardManager`、`ManagerBase`、`StandardSession`
- **状态核心**: creation / access / attribute update / invalidate / expire / persist / replicate
- **失败路径**: 过期扫描、持久化失败、集群同步失败、替换为 Spring Session 后的边界变化
- **连接点**: 连接 T-3（请求执行阶段访问 Session）、`tribes/ha`（集群扩展能力）

### T-6 Mapper 路由与动态更新专题
- **读者问题**: 路由树是如何维护的？Context/Wrapper 变化后为什么能热更新？
- **入口类**: `Mapper`、`MapperListener`、`MappingData`
- **状态核心**: host/context/wrapper 树结构、exact/prefix/extension/default 匹配层次、监听器驱动更新
- **失败路径**: Context 未注册、Wrapper 缺失、动态变更后的路由失配
- **连接点**: 连接 T-1（Service 启动时挂 MapperListener）、T-3（请求进入容器链前的目标选择）

---

## 九、机制覆盖率对账表

| 机制 | 当前域 | 当前状态 | 仍缺什么 |
|---|---|---|---|
| 启动装配（Tomcat -> Service -> Connector -> MapperListener） | T-1 | 已形成闭环 | 可继续补 Spring Boot 嵌入式集成桥接 |
| 请求进入（NioEndpoint -> Processor -> Adapter） | T-2 | 已形成闭环 | 可继续补更多超时/异常测试证据 |
| 容器执行（Mapper -> Valve -> FilterChain -> Servlet） | T-3 | 已形成闭环 | 需要为 `T-2 -> T-3` 准备桥接总图 |
| 异步/错误处理 | T-4 | 已升格为独立域 | timeout / dispatch / complete / onError 测试与失败路径仍需补厚 |
| Session 生命周期 | T-5 | 已形成主题闭环 | 单机生命周期 vs 持久化/集群复制 边界还需继续收敛 |
| Mapper 路由与动态更新 | T-6 | 已形成专题候选 | 动态更新事件与测试证据仍偏薄 |
| 测试证据覆盖 | T-1 ~ T-6 | 已纳入规划依据 | 还没有逐域测试清单 |
| 排除/暂缓边界 | 全局 | 已分层记录 | 仍需随后续扫描动态更新 |

这张表的意义不是把规划“量化成完成率”，而是防止后续进入正文时把“已扫描”误当成“已闭环写清”。

---

## 十、深审复盘（按方法论再次检查后的剩余问题）

> 本节不是推翻前文，而是按《源码范围规划复盘方法论》与《源码分析深审缺陷档案》二次复盘后，确认当前规划仍然存在的剩余缺口。

### 1. T-2 与 T-3 仍然过于接近，存在“主线拆分过细”的风险

当前虽然已经把请求主线收束得比旧版更好，但 `T-2 请求进入与协议处理闭环` 和 `T-3 容器执行闭环` 的边界仍然很近：

- `T-2` 以 `Connector / NioEndpoint / Http11Processor / CoyoteAdapter` 为主
- `T-3` 以 `Mapper / Valve / FilterChain / Servlet` 为主

这在结构上是合理的，但要注意一个风险：

- 如果正文里先写 `T-2`，再写 `T-3`
- 却没有一张清晰的“CoyoteAdapter 之后如何切到 Mapper + Pipeline + FilterChain”的桥接总图

那么读者仍可能感觉“请求主线被人为截成两半”。

**结论**：
- 这不是要合并成一个域
- 但后续正文前，必须明确准备一张总图，把 `T-2 -> T-3` 的交界处讲透

### 2. T-4 仍然偏“高层结论”，失败路径证据还不够厚

`T-4` 已经升格为独立域，这是正确方向；但按方法论要求，它还缺更细的失败路径与测试证据：

- timeout 如何触发
- dispatch 如何转移
- complete 何时真正完成
- onError 如何回流到 `StandardHostValve` / `ErrorReportValve`
- 容器线程与非容器线程写回边界在哪

当前已有主码锚点，但测试层只点到了 `test/org/apache/coyote/http2/TestAsync.java` 和 `TestIoTimeouts.java`，还不足以支撑“已足够写正文”的判断。

**结论**：
- `T-4` 仍应保留在“接近可写，但建议先补失败路径/测试证据”
- 这个判断是正确的，不需要上调

### 3. T-5 Session 域还停留在主题级，尚未证明“集群复制”值得并入第一版卡片

当前 `T-5` 把：
- `StandardManager`
- `PersistentManager`
- `StandardSession`
- `FileStore`
- `JDBCStore`
- `DeltaManager`
- `BackupManager`

全部放进了同一域。这么做有覆盖面，但按方法论看，还存在一个问题：

- 前半段是单机 Session 生命周期
- 后半段已经进入持久化与集群复制

它们当然相关，但是否应该在第一版规划里放在同一主题，需要继续看：
- 集群复制是否真的在后续正文主线里优先级足够高
- 还是应先把“单机 Session 生命周期”写透，再把“持久化/集群复制”拆成后续专题

**结论**：
- 当前写法可作为“广义主题池”保留
- 但后续正文时很可能要拆成“单机 Session”与“持久化/集群复制”两层

### 4. T-6 Mapper 专题的“动态更新”证据还偏概念化

当前已经确认：
- `Mapper` 是请求路由核心
- `MapperListener` 在启动和变更时起更新作用

但“动态更新”这四个字仍偏结论，后续还需要补：
- 哪些容器事件会触发 `MapperListener`
- Context / Wrapper 增删改时，Mapper 树如何更新
- 对应测试目录里哪些用例实际锁定了这些行为

**结论**：
- `T-6` 作为专题保留没问题
- 但它目前更像“候选正文主题”，还不是第一优先级正文

### 5. 当前文档仍然缺少“机制覆盖率”显式对账

按方法论，不能只说“看过这些模块、补了这些主题”，还应该回答：

- 启动机制是否闭环
- 请求机制是否闭环
- async/error 是否闭环
- session 是否闭环
- mapper 是否闭环
- 失败路径覆盖到什么程度
- 测试证据覆盖到什么程度

现在文档已经有主线、有五要素、有正文优先级，但还没有一个简洁的“机制覆盖率对账表”。

**结论**：
- 这不影响当前进入正文排期判断
- 但如果还要继续完善规划文档，这会是下一步最值得补的一层

### 6. `HANDOVER.md` 中关于 Tomcat 域数的历史描述已经过期

当前 grep 结果显示：
- `HANDOVER.md` 里仍写着 `Tomcat (3🔴+1🟡=4域 ✅)`

这与当前文档实际的 6 域规划已不一致。

**结论**：
- 这是典型的跨层不一致问题
- 说明后续如果要收口整套规划，还需要同步更新上游 handover/索引文件

---

## 十、卷级完整路线图（主干 + 专题层）

> 这一节用于回答一个重要问题：为什么当前主干只有 6 条，但旧版 Tomcat 卷曾经写到 12 篇？
>
> 结论不是“旧版全对、新版变少了”，而是：
> - 当前 6 条是 **嵌入式 Tomcat 主干机制闭环**
> - 旧版额外覆盖的是 **规范 / 集成 / 类加载 / 生产实战** 等专题层
>
> 也就是说，Tomcat 完整卷不应只停留在主干闭环，而应显式拆成：
> 1. **主干层**：先立住系统骨架
> 2. **规范层**：补实现背后的外部契约
> 3. **集成层**：补上层装配桥
> 4. **机制补深层**：补主干里已点到但值得单独深挖的专题
> 5. **生产层**：补性能、安全运维与故障排查

### A. 主干层（当前已完成规划并已进入正文）

1. `T-1` 启动与装配闭环
2. `T-2` 请求进入与协议处理闭环
3. `T-3` 容器执行闭环
4. `T-4` 异步、超时与错误处理闭环
5. `T-5` Session 生命周期闭环
6. `T-6` Mapper 路由与动态更新专题

这一层回答的是：
- 嵌入式 Tomcat 怎么启动
- 请求怎么进来
- 请求怎么在 Catalina 里执行
- 偏离正常路径时怎么重新接住
- Session 如何被管理
- 路由树如何存在并更新

### B. 规范层（之前遗漏，应该纳入完整卷）

7. **Servlet 规范与 Tomcat 实现边界**
   - 为什么 Tomcat 的很多设计要长成这样
   - 生命周期、Filter、DispatcherType、Session 等规范契约如何压到实现上
   - 这一层不是附录，而是帮助读者分清“这是规范要求”还是“这是 Tomcat 自己的实现取舍”

### C. 机制补深层（主干的纵深补篇）

8. **Servlet 生命周期专题（StandardWrapper 深挖）**
   - `allocate()`、`loadServlet()`、`initServlet()`、`unload()`
   - 适合放在主干之后，作为 `T-3` 的执行末端纵深

9. **类加载与 WebApp 隔离专题**
   - `WebappClassLoaderBase`
   - 打破双亲委派、应用隔离、热部署/泄漏问题
   - 这是 Tomcat 与普通嵌入式 Web 框架差异很大的一条线，不应被漏掉

### D. 集成层（之前遗漏，应该纳入完整卷）

10. **Spring Boot 集成专题**
   - `TomcatServletWebServerFactory`
   - `TomcatWebServer`
   - `TomcatStarter`
   - 三层 customizer / 配置映射
   - 这一层非常重要，因为我们分析对象本来就是“嵌入式 Tomcat”，如果不把 Boot 集成补上，读者会知道 Tomcat 内核，却不知道 Spring Boot 实际怎么把它装起来

### E. 生产与排障层（之前遗漏，应该纳入完整卷）

11. **生产性能专题**
   - 线程、连接、队列、超时、吞吐与压测定位
   - 这是主干机制落到生产参数的桥接层

12. **生产安全与运维专题**
   - RemoteIpValve、优雅关闭、JMX/Micrometer、运维侧安全边界
   - 这是容器从源码走向真实环境的治理层

13. **故障排查专题**
   - 启动失败
   - 连接打满
   - 线程池耗尽
   - 类加载器泄漏
   - 慢请求定位
   - 这一层不是“额外实战”，而是对主干机制的一次逆向复盘：知道主线之后，才能看懂故障为什么会这样表现

### 当前卷级判断

因此，Tomcat 这一卷更准确的完整结构应理解为：

- **第一阶段：6 篇主干机制篇**（当前已完成）
- **第二阶段：规范层 / 集成层 / 机制补深层 / 生产层**（当前还没有纳入新卷写作）

如果目标是“嵌入式 Tomcat 主干卷”，现在已经阶段性完结。

如果目标是“Tomcat 完整卷”，当前已经补上：
- Servlet 规范
- Spring Boot 集成
- StandardWrapper 生命周期专题
- ClassLoader 专题
- 生产性能 / 安全运维 / 故障排查

### 当前仍值得补、且不过时的 Tomcat 专题

> 以下只保留 **嵌入式 Spring Boot 主线真正 relevant** 的内容；JSP / Jasper、AJP、WebResourceRoot 等过时或当前主线不走的内容，不再纳入。

1. **HTTP 处理纵深专题**
   - `Http11Processor`
   - `InputBuffer / OutputBuffer`
   - Coyote `Request / Response`
   - keep-alive / recycle / reuse 这类协议层对象生命史

2. **对象复用 / 对象生命周期复用专题**
   - 不先假设它一定是“显式对象池”
   - 先从 Processor、Coyote request/response、buffer/message 复用点中抽主线

3. **StandardContext 启停主线专题**
   - Context 内部组件装配
   - 启动 / 停止 / 销毁顺序
   - 这是嵌入式 Tomcat 很核心、但当前主干还没完全压透的一条线

4. **Servlet / Filter / Listener 注册体系纵深专题**
   - `ServletContextInitializerBeans`
   - `ServletRegistrationBean / FilterRegistrationBean`
   - `SCI / HandlesTypes` 到应用注册链的再压实

5. **线程池 / Executor 专题**
   - `Executor`
   - `TaskQueue`
   - `LimitLatch`
   - 线程增长、排队、阻塞传播

### 推荐的卷级后续顺序

如果继续扩 Tomcat 完整卷，我建议顺序是：

1. **HTTP 处理纵深专题**
2. **对象复用 / 对象生命周期复用专题**
3. **StandardContext 启停主线专题**
4. **Servlet / Filter / Listener 注册体系纵深专题**
5. **线程池 / Executor 专题**

原因：
- 它们都仍然属于嵌入式 Tomcat 当前主线真正 relevant 的补深方向
- 不会重新引入 JSP / AJP / 资源系统这类过时或当前主线不走的内容
- 其中 HTTP 处理和对象复用最容易直接增厚前面主干而不改主线顺序

## 十一、正文准备度与优先级

### A. 已具备进入正文写作条件

1. **T-1 启动与装配闭环**
   - 已有明确入口、桥接类、跨包主线和装配证据
2. **T-2 请求进入与协议处理闭环**
   - 主链最清楚，源码证据集中，最适合作为 Tomcat 正文第一批
3. **T-3 容器执行闭环**
   - Mapper + Valve + FilterChain + Servlet 已形成可讲清的闭环

### B. 接近可写，但建议先补失败路径/测试证据

4. **T-4 异步、超时与错误处理闭环**
   - 中心性已确认，但后续最好继续补 `test/org/apache/coyote/http2/TestAsync.java` 及更多 async/timeout/error 测试证据
5. **T-5 Session 生命周期闭环**
   - 主题清晰，但还应继续补 `session/manager/store/ha/tribes` 的失败与复制路径

### C. 适合作为专题后写

6. **T-6 Mapper 路由与动态更新专题**
   - 现在已经足够专题化，但更适合作为 T-1/T-3 写完后的深挖篇，而不是 Tomcat 第一篇正文

### 推荐的 Tomcat 正文起手顺序

1. `T-1 启动与装配闭环`
2. `T-2 请求进入与协议处理闭环`
3. `T-3 容器执行闭环`
4. `T-4 异步、超时与错误处理闭环`
5. `T-5 Session 生命周期闭环`
6. `T-6 Mapper 路由与动态更新专题`
