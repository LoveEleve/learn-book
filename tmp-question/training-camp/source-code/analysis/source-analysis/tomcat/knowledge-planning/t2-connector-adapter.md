# T-2 Connector + Adapter — 知识规划

> 项目: Tomcat 10.1.x | 类型: **规范参考实现** (Servlet 6.0 / Jakarta EE)
> 核心文件: 7 文件 / ~7,700 行 | 预计 4 篇 v5 大纲
> 基线: T-1 容器+Lifecycle — T-2 回答 "请求如何从 TCP 字节流到达 Engine Pipeline 的 invoke()"

---

## §0.8 域审核前置

### 1. 域过载检查
- 核心类: 7 个 (AbstractProtocol/Http11NioProtocol/Processor/Adapter/CoyoteAdapter/Request/Response)
- 7 ≤ 10 → **不触发拆分阈值** ✅

### 2. 淘汰清单
- HTTP/1.1 Connector 在 Spring Boot 嵌入式和生产独立部署中都是核心入口 → 无淘汰项 ✅

### 3. 规范缺口
T-2 需要映射的 Servlet 规范:
- `jakarta.servlet.http.HttpServletRequest` → `RequestFacade` (包装 catalina.connector.Request)
- `jakarta.servlet.http.HttpServletResponse` → `ResponseFacade` (包装 catalina.connector.Response)
- `jakarta.servlet.ServletConnection` (6.0) → Connector 层提供连接信息
- `jakarta.servlet.AsyncContext` (3.0) → CoyoteAdapter.asyncDispatch()

### 4. 禁止过度加域
- 7 源结构合理 — Protocol/Processor/Adapter/Request/Response 是 T-2 的 5 个子主题，均在 Connector 域内
- T-4 线程模型(NioEndpoint)独立域 — 当前域只涉及 AbstractEndpoint 引用，不展开 Acceptor/Poller/Worker

### 项目类型判定
| 维度 | 值 |
|------|------|
| 类型 | 规范参考实现 |
| 规范 | Servlet 6.0 (Jakarta EE 10) |
| 额外维度 | 规范对应表 + 设计模式 + 架构意图 |
| 控制流 | 外部 TCP → Protocol → Adapter → Engine Pipeline — 请求从外向内流 |
| 关键桥接 | `connector.getService().getContainer().getPipeline().getFirst().invoke()` — T-2↔T-1 交汇点 |

---

## 01 提取 — 逐源映射

### Adapter.java (100 行 — Coyote↔Catalina 桥梁接口)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Adapter.java:46 | **service(Request, Response)**: Coyote 容器入口点 — 协议层处理器调用此方法将请求交给 Catalina。签名为 coyote.Request/coyote.Response | High |
| Adapter.java:60 | **prepare(Request, Response)**: 预处理—映射 coyote 层信息到 catalina 层，返回 false 表示已设置错误响应 | High |
| Adapter.java:73 | **asyncDispatch(Request, Response, SocketEvent)**: Servlet 3.0+ 异步事件分发 — SocketEvent(TIMEOUT/ERROR/OPEN_READ/OPEN_WRITE) | High |
| Adapter.java:82 | **log()**: 访问日志回调 — 在 service 执行路径外记录，用于 CheckRecycled 等安全场景 | High |
| Adapter.java:92 | **checkRecycled()**: 回收安全性检查 — Processor 归还池前断言 Request/Response 已 recycle | High |

### Http11NioProtocol.java (76 行 — NIO HTTP/1.1 协议定义)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Http11NioProtocol.java:28 | **extends AbstractHttp11JsseProtocol<NioChannel>** — 泛型固定 NioChannel，中间存在 JSSE 加密层 | High |
| Http11NioProtocol.java:33-35 | `new NioEndpoint()` — 构造器创建 Endpoint，决定底层使用 NIO 模型 | High |
| Http11NioProtocol.java:68-75 | `getNamePrefix()` — 返回 "http-nio" 或 "https-{ssl}-nio"，用于线程名和 JMX 注册 | High |
| Http11NioProtocol.java:51-65 | **setSelectorTimeout/getPollerThreadPriority** — NIO 特有的 Poller 配置，强制转型 `(NioEndpoint) getEndpoint()` | High |

### AbstractProtocol.java (1240 行 — 协议处理器的通用骨架) [精简 — 聚焦架构]

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractProtocol.java:80 | **AbstractEndpoint\<S,?\> endpoint** — 底层 I/O 提供者，Protocol 与传输层解耦 | High |
| AbstractProtocol.java:147 | **Adapter adapter** — ProtocolHandler↔Connector 的桥接对象 | High |
| AbstractProtocol.java:532 | **abstract createProcessor()** — 工厂方法，子类创建具体 Processor(如 Http11Processor) | High |
| AbstractProtocol.java:608-634 | **init()**: JMX ObjectName 注册→GlobalRequestProcessor 注册→Endpoint 命名+domain 设置→`endpoint.init()` | High |
| AbstractProtocol.java:637-648 | **start()**: `endpoint.start()`→启动 monitorFuture 周期性调度 `startAsyncTimeout()` | High |
| AbstractProtocol.java:706-724 | **stop()**: 取消 monitor→`stopAsyncTimeout()`→所有 waitingProcessors 强制超时→`endpoint.stop()` | High |
| AbstractProtocol.java:812 | **ConnectionHandler.process(SocketWrapperBase, SocketEvent)** — 核心连接处理 | High |
| AbstractProtocol.java:887-899 | **Processor 获取三阶段**: 1)recycledProcessors.pop() → 2)createProcessor()→ 3)JMX register() | High |
| AbstractProtocol.java:905 | **processor.process(wrapper, status)** — 核心委托: Handler→Processor | High |
| AbstractProtocol.java:974-979 | **OPEN 状态**: release(processor)回收→wrapper.registerReadInterest() 注册读兴趣 — HTTP keep-alive 循环 | High |
| AbstractProtocol.java:1092-1116 | **release(Processor)**: processor.recycle()→upgrade 类型从 waitingProcessors 移除→非 upgrade push 到 recycledProcessors 池 | High |

### Processor.java (117 行 — 协议处理器接口)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Processor.java:43 | **process(SocketWrapperBase, SocketEvent) → SocketState** — 核心方法，返回 SocketState 决定连接后续调度 | High |
| Processor.java:84 | **recycle()** — 回收 Processor，为下一请求(可能同连接)准备 | High |
| Processor.java:74 | **timeoutAsync(long now)** — 异步超时检查，now<0 时强制触发(用于 stop) | High |
| Processor.java:62 | **isAsync()** — 是否处于异步处理状态 | High |

### CoyoteAdapter.java (1315 行 — 请求从协议层到容器层的转换核心)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| CoyoteAdapter.java:99 | **final Connector connector** — 每个 Adapter 绑定一个 Connector | High |
| CoyoteAdapter.java:303 | **service(coyote.Request, coyote.Response)** — Adapter 入口，协议处理器调用 | High |
| CoyoteAdapter.java:305-306 | **req.getNote(ADAPTER_NOTES)** — Notes 机制缓存 Catalina Request/Response，连接复用 | High |
| CoyoteAdapter.java:339 | **postParseRequest()** — 请求解析核心: Host 路由/URI 规范化/Mapper 映射 | High |
| CoyoteAdapter.java:344 | **connector.getService().getContainer().getPipeline().getFirst().invoke()** — T-2↔T-1 交汇: 请求进入 Engine Pipeline | High |
| CoyoteAdapter.java:622-667 | **URI 处理管线**: suspiciousURI→decodedURI→pathParameters 剥离→URLDecoder→normalize→convertURI | High |
| CoyoteAdapter.java:697 | **mapper.map(serverName, decodedURI, version, mappingData)** — Mapper 将 Host+URI 映射为 Host/Context/Wrapper | High |
| CoyoteAdapter.java:578-592 | **proxyName/proxyPort 代理代换** — Host header 可被 proxyName 覆盖(反向代理场景) | High |
| CoyoteAdapter.java:420-424 | **recycle Request+Response** — 非异步请求结束时归还对象池 | High |

### Request.java (3244 行 — Catalina 层 Request)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Request.java:153 | **protected coyote.Request coyoteRequest** — 委托模式: Catalina Request 包装 Coyote Request | High |
| Request.java:539 | **final Connector connector** — Request 持有 Connector 引用，可访问 Service/Mapper | High |
| Request.java:608-609 | **MappingData mappingData** — 持有 Mapper 映射结果(Host/Context/Wrapper) | High |
| Request.java:431-504 | **recycle()** — 全量状态重置: dispatcherType/authType/inputBuffer/session/mappingData/asyncContext | High |
| Request.java:160-163 | **setCoyoteRequest()** — 设置 Coyote Request 并通知 InputBuffer 关联 | High |

### Response.java (1674 行 — Catalina 层 Response)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Response.java:100 | **protected coyote.Response coyoteResponse** — 委托模式: 所有写操作最终到 Coyote 层 | High |
| Response.java:131 | **OutputBuffer outputBuffer** — 输出缓冲区，所有响应数据写入此处 | High |
| Response.java:202-230 | **recycle()** — cookies/outputBuffer/using*标志/outputStream/writer 全量重置 | High |
| Response.java:421-424 | **finishResponse() → outputBuffer.close()** — 刷出剩余缓冲区 | High |
| Response.java:501-512 | **getOutputStream()** — 互斥检查(usingWriter=false)→懒初始化 CoyoteOutputStream | High |

---

## 02 聚合 — P1/P2/P3 分级

### P1 — 全系统共识（≥5 处引用）

| KP | 涉及文件 | 证据 |
|------|:--:|------|
| **两层 Request/Response 设计** (coyote 层↔catalina 层) | 5 | Adapter 接口(coyote 签名)/CoyoteAdapter(转换)/Request(coyoteRequest 字段)/Response(coyoteResponse 字段)/Processor(持有 Request) |
| **Adapter.service() 桥接** | 5 | Adapter 接口/CoyoteAdapter.service()/AbstractProtocol(持有 adapter)/ConnectionHandler(触发)/Processor(调用方) |
| **Processor process→recycle 循环** | 4 | Processor 接口/ConnectionHandler.process/AbstractProtocol.release/RecycledProcessors 池 |
| **connector.getService().getContainer().getPipeline().getFirst().invoke()** | 3 | CoyoteAdapter.service/CoyoteAdapter.asyncDispatch/CoyoteAdapter.log — 全部通向 Engine Pipeline |
| **Mapper.map() 路由映射** | 3 | CoyoteAdapter.postParseRequest/Request.mappingData/MappingData.host+context+wrapper |

### P2 — 局部重要（2-4 处引用）

| KP | 涉及文件 | 证据 |
|------|:--:|------|
| **Endpoint 解耦** | 3 | AbstractProtocol(endpoint 字段)/Http11NioProtocol(NioEndpoint 构造)/AbstractEndpoint 接口 |
| **Protocol 泛型抽象** | 3 | AbstractProtocol\<S\>/Http11NioProtocol\<NioChannel\>/AbstractHttp11JsseProtocol |
| **Request/Response recycle 对象池** | 3 | Request.recycle/Response.recycle/ADAPTER_NOTES 缓存→CoyoteAdapter 复用 |
| **异步超时管理** | 3 | Processor.timeoutAsync/ConnectionHandler/waitingProcessors→monitorFuture |

### P3 — 独立知识点

| KP | 文件 | 原因 |
|------|------|------|
| **proxyName/proxyPort 反向代理** | CoyoteAdapter | 反向代理场景专用 |
| **URI 安全管线(suspicious/encode/normalize)** | CoyoteAdapter | URI 规范化独有 |
| **OPTIONS * / CONNECT 拒绝** | CoyoteAdapter | 特殊 HTTP 方法处理 |
| **OutputStream vs Writer 互斥** | Response | 响应输出流互斥 |
| **NIO Selector/Poller 配置** | Http11NioProtocol | NIO 特有配置 |

---

## 03 深度分类 — 🔴🟡🟢 per KP

| KP 群 | 级别 | 判定理由 |
|------|:--:|------|
| **两层 Request/Response + Adapter.service() 桥接** | 🔴 Deep | T-2 的核心架构决策 — 为什么有 coyote 和 catalina 两层? 为什么需要 Adapter 转换? 不理解这个分离就理解不了 Tomcat 的协议无关性 |
| **connector.getService().getContainer().getPipeline().invoke()** | 🔴 Deep | T-2↔T-1 的唯一交汇点 — 也是当前读 Tomcat 源码时从 Connector 世界进入 Container 世界的入口。T-1 已理解容器树，T-2 解释"谁调了这行 invoke" |
| **Processor 生命周期 + RecycledProcessors 池** | 🔴 Deep | Protocol 层的核心性能机制 — Processor 是昂贵的(链接到 JMX/SSL/缓冲区)，回收复用是 keep-alive 高性能的基础 |
| **Protocol 抽象层(AbstractProtocol→Http11NioProtocol)** | 🟡 Working | 协议扩展性的架构基础 — Protocol 只决定创建什么 Processor，真正 I/O 在 Endpoint。理解层次关系但不需展开 T-4 |
| **URI 处理管线 + Mapper.map()** | 🟡 Working | 请求路由的关键链路 — Mapper 查询入口在 CoyoteAdapter，但 Mapper 路由表构建在 T-5 |
| **异步超时 + asyncDispatch** | 🟡 Working | Servlet 3.0+ 异步支持 — 不是 Connector 的核心职责，但连接现代特性 |
| **proxyName/反射代理** | 🟢 Surface | 生产环境配置 — 概念简单，反向代理场景专用 |

---

## 04 聚类 — 教学顺序

**Cluster A: Protocol 抽象层 — 协议如何与传输解耦** (→ 对应 §1)
- AbstractProtocol + Http11NioProtocol: 协议栈的通用骨架 → NIO 实例化
- Endpoint 引用 — 协议不关心底层是 NIO/NIO2/APR，只关心 createProcessor()
- 规范对应: `ProtocolHandler` → Servlet 规范未直接定义，但 `getAdapter()/setAdapter()` 是 Connector→Container 关键桥梁

**Cluster B: Processor — 请求处理单元的生命周期** (→ 对应 §2)
- Processor 接口: process()/recycle()/isAsync()
- ConnectionHandler.process(): 从 recycledProcessors 池获取→处理→回收
- SocketState 状态机: OPEN→LONG→UPGRADING→CLOSED，每个状态决定连接后续调度
- 模式: Processor 池化 → [模式: Object Pool]
- 架构意图: **池化 + 连接复用** — 一次 accept() 后的连接上可以处理 N 个 HTTP 请求(keep-alive)，Processor 每次回收而不是销毁

**Cluster C: CoyoteAdapter — 两次对象世界的转换器** (→ 对应 §3)
- Adapter 接口: service/prepare/asyncDispatch/log — 5 个契约方法
- CoyoteAdapter.service() 完整流程: Notes 缓存复用→创建 coyote↔catalina 映射→URI 解析→Mapper 路由→invoke Pipeline
- 关键桥接线: `connector.getService().getContainer().getPipeline().getFirst().invoke(request, response)`
- postParseRequest: URI 安全管线→Mapper.map()→Session ID 解析→TRACE 过滤器→认证
- 规范对应: `ServletRequest`/`ServletResponse` → Catalina Request/Response 经 Adapter 创建

**Cluster D: Request/Response 两层委托** (→ 对应 §4)
- 两层设计: coyote.Request(字节流层)↔catalina.connector.Request(Servlet 语义层)
- Request: coyoteRequest 委托 → Connector 引用 → MappingData(映射结果) → recycle()
- Response: coyoteResponse 委托 → OutputBuffer → OutputStream/Writer 互斥 → finishResponse()
- 规范对应: `HttpServletRequest`(1.0) → `RequestFacade` / `HttpServletResponse` → `ResponseFacade`
- 规范映射表: 方法与规范的精确对应

> → 引出 T-3 Pipeline+双链 — 请求经过 Adapter 进入了 `engine.getPipeline().getFirst().invoke()` — 之后发生了什么? Pipeline 内部的 Valve 链如何执行? StandardEngineValve→StandardHostValve→StandardContextValve→StandardWrapperValve 四个 Valves 各做什么? ApplicationFilterChain 在哪个环节被调用?

