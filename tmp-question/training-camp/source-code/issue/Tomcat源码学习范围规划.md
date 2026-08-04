# Tomcat 源码学习范围规划

> **版本**: v10.1.34
> **仓库**: `/data/workspace/source-code/code/spring/tomcat/`
> **规模**: 大项目，核心在 catalina + coyote
> **日期**: 2026-08-03

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

## 二、知识域规划

### 🔴 核心域（3 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| T-1 | **嵌入式启动 + 容器层次** | StandardServer, StandardService, StandardEngine, StandardHost, StandardContext, StandardWrapper | **容器链**：`Server`→`Service`(多个 Connector+一个 Engine)→`Engine`(虚拟主机路由)→`Host`(域名匹配)→`Context`(Web 应用，对应一个 `webapps/ROOT` 或 Spring Boot 嵌入式)→`Wrapper`(单个 Servlet)；**嵌入式启动**：`Tomcat tomcat = new Tomcat()` → `tomcat.setPort(8080)` → `Context ctx = tomcat.addContext("", baseDir)` → `Tomcat.addServlet(ctx, "dispatcher", new DispatcherServlet())` → `ctx.addServletMappingDecoded("/", "dispatcher")` → `tomcat.start()` → `tomcat.getServer().await()`；**Spring Boot 集成**：`TomcatServletWebServerFactory.getWebServer()` → 创建 `Tomcat` → 配置 `Context` → `TomcatWebServer.start()` |
| T-2 | **Connector + HTTP/1.1 处理** | Connector, Http11NioProtocol, Http11Processor, CoyoteAdapter, NioEndpoint | **Connector 结构**：`Connector(protocol="HTTP/1.1")` → `ProtocolHandler(Http11NioProtocol)` → `Endpoint(NioEndpoint—Acceptor+Poller+Worker 三线程模型)`；**请求处理链**：Acceptor 接收 TCP 连接→`Poller` NIO 事件分发→`SocketProcessor` 提交 Worker 线程池→`Http11Processor.service()` 解析 HTTP→`CoyoteAdapter.service()` 转换为 `Request/Response` 对象→`Engine.getPipeline().getFirst().invoke()` 进入容器链 |
| T-3 | **Pipeline-Valve 责任链** | Pipeline, Valve, StandardEngineValve, StandardHostValve, StandardContextValve, StandardWrapperValve | **责任链驱动**：每个容器(Engine/Host/Context/Wrapper)都有一个 `Pipeline`，通过 `invoke(request, response)` 串联 Valves→`StandardEngineValve`→`StandardHostValve`→`StandardContextValve`→`StandardWrapperValve`→最终 `Servlet.service()`；**Filter 链**：`ApplicationFilterChain` 包装→`doFilter()` 链式调用→最后才到 Servlet |

### 🟡 扩展域（1 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| T-4 | **线程模型与连接池** | NioEndpoint, Acceptor, Poller, SocketProcessor, ThreadPoolExecutor | **三线程模型**：`Acceptor`(接受连接→设置 Socket)→`Poller`(NIO Selector 事件轮询→封装 `PollerEvent`→生成 `SocketProcessor`)→`SocketProcessor`(Worker 线程池执行—`executor.execute()`)；**maxConnections**：最大连接数(10000)—控制 Poller 队列深度；**maxThreads**：Worker 线程池最大线程(200)—控制并发处理数 |

---

## 三、淘汰清单

| 模块 | 理由 |
|---|---|
| `jasper/` (JSP) | JSP 淘汰——Spring Boot 不用 |
| `el/` | EL 表达式——独立引擎 |
| `juli/` | 日志——Spring Boot 用 Logback |
| `naming/` | JNDI——Spring Boot 不用 |
| `coyote/ajp/` `coyote/http2/` | AJP/HTTP2 协议——非核心 |
| `catalina/realm/` `catalina/valves/` | 安全/内置 Valve——非核心 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 3 |
| 🟡 扩展域 | 1 |
| **总域** | **4** |

以上规划完成，共 **3🔴+1🟡=4 域**。聚焦嵌入式 Tomcat 的三层（容器+Connector+Pipeline）。
