# T-3 §1 Pipeline + Valve — Chain of Responsibility 的标准实现

> 依赖 T-2 §4 | 🔴 Deep | 3 KP | [模式: Chain of Responsibility]

**读者处境**: T-2 结束 — `connector.getService().getContainer().getPipeline().getFirst().invoke(request, response)` 这行代码把请求交给了 Engine Pipeline 的第一个 Valve — 问题是: Pipeline 是什么？Valve 是什么？`getFirst()` 返回什么？这个 invoke 之后发生了什么？

### 1. Pipeline 接口 — basic Valve + 附加 Valves 的双层设计

场景: 需要对所有请求做访问日志记录 — 在 Spring Boot 中通过 `WebServerFactoryCustomizer` 程序化添加: `tomcat.getEngine().getPipeline().addValve(new AccessLogValve())` — 希望在进入容器核心逻辑前后都可以插入逻辑。独立 Tomcat 部署中通过 `server.xml` 的 `<Valve>` 标签配置同样的效果。Tomcat 的方案: **basic Valve = 容器核心逻辑(必须最后执行)**，**addValve = 可插入的扩展逻辑(在 basic 之前)**。

源码路径:
- `Pipeline.java:24-32` — Javadoc: "basic Valve always executed last — additional Valves added before it"
- `Pipeline.java:43` — `getBasic()` — 返回容器特定的基础 Valve(StandardEngineValve/HostValve 等)
- `Pipeline.java:79` — `addValve(Valve)` — 追加 Valve 并触发 `Container.ADD_VALVE_EVENT`
- `Pipeline.java:106` — `getFirst()` — 返回链中第一个 Valve(不是 basic — 是 addValve 的 first 或 basic)
- `Pipeline.java:114` — `isAsyncSupported()` — 检查所有 Valve 是否支持异步

关键设计: **Why basic 必须在最后？** 因为 basic Valve 是容器自身的处理逻辑(Engine 的 basic=StandardEngineValve — 负责选择 Host)。如果 AccessLogValve 在 basic 之后 — 则 basic 已经调了 `getNext().invoke()` — AccessLogValve 永远不可能被调用。addValve 放在 basic 之前 — 确保: AccessLog→basic(EngineValve)。addValve 可以: 1) 修改 request(如认证) 2) 短路返回(如权限拒绝) 3) 在 basic 返回后检查 response(如记录访问日志)。 [模式: Chain of Responsibility — Pipeline 是 CoR 的教科书实现: 链中每个处理器(Valve)决定是否处理+是否传递]

架构意图: **管道与容器同构** — T-1 学了 Container 树(每一层是一个 Container)。T-3 揭示了: 每个 Container 有一个 Pipeline — Pipeline 的第一个 Valve 被上一层的 Valve 调用 — 级联是: EngineValve→Host Pipeline→HostValve→Context Pipeline→ContextValve→Wrapper Pipeline→WrapperValve→FilterChain — **树结构递归进入管道结构**。

数据流: 程序化配置 `engine.getPipeline().addValve(new AccessLogValve())`→AccessLogValve 添加到链头→`engine.getPipeline().addValve(new RemoteAddrValve())`→RemoteAddr 插入到链头(AccessLog 之前)→链顺序: RemoteAddr→AccessLog→basic(EngineValve)。请求: `pipeline.getFirst().invoke()`(first=RemoteAddr)→RemoteAddr.invoke()→检查 RemoteAddr→`getNext().invoke()`→AccessLog.invoke()→记录开始时间→`getNext().invoke()`→EngineValve.invoke()→选择 Host→进入 Host Pipeline...

### 2. Valve 接口 — 单向链表 + invoke 契约

场景: `RemoteAddrValve.invoke()` 检查客户端 IP — 如果在黑名单 — 直接返回 403 — 不调用 `getNext().invoke()` — 后面的 Valve 全部跳过。这是 CoR 的标准行为: 每个处理器可以短路。

源码路径:
- `Valve.java:50-58` — `getNext()/setNext(Valve)` — 单向链表的 next 指针
- `Valve.java:71-113` — **invoke() 契约 10 条规则 (MAY 5 + MUST NOT 5)**: MAY: 检查/修改 Request+Response / 自生成 Response(短路) / 包装 Request+Response / 调用 getNext().invoke() / 检查返回后的 Response。MUST NOT: 修改已用的路由属性 / 已生成 Response 后继续传递 / 消费 InputStream / 在 getNext() 返回后修改 Header 或 OutputStream
- `Valve.java:68` — `backgroundProcess()` — 周期性任务(如重新加载)
- `Valve.java:117` — `isAsyncSupported()` — Valve 级异步声明

关键设计: **Why MUST NOT 在 getNext().invoke() 返回后修改 Response Header？** 因为后续 Valve 可能已经设置了 `Content-Type` 和 `Content-Length` — 如果 AccessLogValve 在返回后修改 — 响应头不一致 — 浏览器无法正确解析。CoR 的后处理只能是**只读检查**(如记录耗时=now-startTime、检查状态码、记录日志) — **不能修改**。

数据流: `pipeline.getFirst().invoke()`→Valve1.invoke()→日志→`valve1.getNext().invoke()`(即 Valve2)→Valve2.invoke()→权限→通过→`valve2.getNext().invoke()`(即 basic)→basic.invoke()→核心逻辑→返回→回到 Valve2 → Valve2 检查 response 无异常→返回→回到 Valve1 → Valve1 记录耗时+状态码→返回→回到 Pipeline → 回到 Adapter → finishResponse。

### 3. 容器 Pipeline 的创建与生命周期

场景: T-1 容器树中 — `ContainerBase` 持有 `Pipeline pipeline` 字段 — 这个字段什么时候创建的? 什么时候配置 basic Valve?

源码路径: 每个容器的构造器或 `initInternal()` 中设置 basic Valve: `StandardEngine` 设置 `StandardEngineValve`、`StandardHost` 设置 `StandardHostValve`、`StandardContext` 设置 `StandardContextValve`、`StandardWrapper` 设置 `StandardWrapperValve`。`Pipeline` 由 `ContainerBase` 在构造器中创建。

关键设计: **Why basic Valve 在容器创建时而非配置时设置？** basic Valve 是容器自身的核心逻辑 — 不需要外部配置 — 它在容器创建时就应该存在。额外的 Valves 通过程序化 API `addValve()` 添加 — Spring Boot 中通过 `WebServerFactoryCustomizer` 访问 Pipeline — 独立 Tomcat 中通过 `server.xml` 解析时调用。两种方式都不能删除 basic。

→ 引出 §2 4 Valve 级联 — 理解了 Pipeline+Valve 的 CoR 骨架 — 但 Engine/Host/Context/Wrapper 各自有一个 Pipeline — 每个 Pipeline 的 basic Valve 是什么? 每个 basic Valve 做了什么? 怎么级联?
