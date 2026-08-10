# T-5 §2 MapperListener — 动态路由更新 + Tomcat 全链路收官

> 依赖 §1 | 🟡 Working | 2 KP

**读者处境**: 理解了四级匹配 — 但路由表是怎么构建的？什么时候更新的？如果 Tomcat 运行中通过 Manager 应用热部署了一个新的 WAR — Mapper 怎么知道有新 Context 需要路由？

### 1. MapperListener — ContainerEvent 的忠实监听者

场景: T-1 学过 — `host.addChild(context)` 触发 `ADD_CHILD_EVENT` — 谁在听这个事件？MapperListener。它订阅了 Container 树的所有增删事件 — 任何 Host/Context/Wrapper 的变化 — MapperListener 更新 Mapper 路由表。

源码路径:
- `MapperListener.java:147` — **containerEvent()**: `ContainerEvent` 分发 — `ADD_CHILD_EVENT`→`registerContext(child)` / `REMOVE_CHILD_EVENT`→`unregisterContext(child)` / `ADD_MAPPING_EVENT`→`mapper.addWrapper()` / `REMOVE_MAPPING_EVENT`→`mapper.removeWrapper()`
- `MapperListener.java:158` — **registerContext()**: 新 Context→`mapper.addContextVersion(hostName, host, path, version, context, welcomeFiles, resources)` — 将 Context 的所有 Wrapper 映射注册到 Mapper
- `MapperListener.java:392` — **unregisterContext()**: Context 移除→`mapper.removeContextVersion()` — 从路由表删除
- `MapperListener.java:174` — **ADD_ALIAS_EVENT**: Host 别名→`mapper.addHostAlias()` — 一个 Host 可以被多个域名访问

关键设计: **Why MapperListener 既要监听 ContainerEvent 又要监听 LifecycleEvent？** ContainerEvent 覆盖运行时变化(addContext/removeContext) — 但初始构建(start 时注册所有现有容器)通过 LifecycleEvent。`MapperListener.start()` 遍历 Engine 的所有 Host→每个 Host 的所有 Context→每个 Context 的所有 Wrapper→注册到 Mapper。这是 T-1 Container 树的完整遍历。

数据流: Tomcat 启动→`MapperListener.start()`→`engine.addContainerListener(mapperListener)` 注册监听→遍历 engine.findChildren()→每个 Host: `mapper.addHost(hostName, aliases, host)`→遍历 host.findChildren()→每个 Context: `registerContext(context)`→`mapper.addContextVersion(hostName, host, path, version, context, ...)`→遍历 context 的所有 ServletMappings→`mapper.addWrapper(hostName, path, version, mapping, wrapper, ...)`→路由表构建完成→运行时: Manager 部署新 Context→`host.addChild(newContext)`→`ContainerBase.fireContainerEvent(ADD_CHILD_EVENT)`→`MapperListener.containerEvent()`→`registerContext(newContext)`→Mapper 更新路由表→新 Context 立即可路由。

### 2. Tomcat Stage 1 全链路收官 — 从 TCP ACCEPT 到 Servlet.service()

场景: 学完了 T-1(容器树)→T-2(Connector+Adapter)→T-3(Pipeline+双链)→T-4(线程模型)→T-5(Mapper 路由)。现在可以从头到尾走完一个 HTTP 请求在 Tomcat 中的完整旅程。

**全链路数据流**:

```
1. TCP accept (T-4 §1)
   Acceptor.accept() → NioSocketWrapper → poller.register()

2. Poller 事件循环 (T-4 §2)
   selector.select() → selectedKeys → processKey() → processSocket()

3. Worker 线程执行 (T-4 §1)
   executor.execute(socketProcessor) → Http11Processor 读取 HTTP 报文

4. 协议解析 (T-2 §2)
   InputBuffer 读字节 → 解析 Method/URI/Headers → 创建 coyote.Request

5. Adapter 桥接 (T-2 §3)
   CoyoteAdapter.service(coyReq, coyRes) → ADAPTER_NOTES 复用 catalina Request
   → postParseRequest() → URI 解码+规范化 → mapper.map() ← T-5 §1

6. Mapper 路由 (T-5 §1)
   Exact→Prefix→Extension→Welcome 四级匹配 → 填充 mappingData
   (host=example.com, context=/app, wrapper=user)

7. Engine Pipeline (T-3 §2)
   engine.getPipeline().getFirst().invoke(request, response)
   → StandardEngineValve → host pipeline → StandardHostValve
   → context pipeline → StandardContextValve → wrapper pipeline

8. WrapperValve (T-3 §2)
   StandardWrapperValve → servlet.allocate() → FilterChain → servlet.service()

9. Filter 链 (T-3 §3)
   ApplicationFilterChain.internalDoFilter() → pos 迭代 Filter
   → filter.doFilter(req, res, chain) → chain.doFilter() → pos++
   → 全部 Filter 执行完 → servlet.service(req, res)

10. 响应返回 (T-2 §4)
    servlet 写入 response.getOutputStream() → OutputBuffer
    → coyote.Response → SocketOutputStream → TCP send
    → Processor 返回 SocketState.OPEN → keep-alive 复用
```

设计意图: **为什么 Tomcat 要用 Container 树+Mapper+Pipeline 三套独立结构？** Container 树管生命周期(T-1) — Mapper 管路由决策(T-5) — Pipeline 管请求处理(T-3)。路由 和 处理 分离: Mapper 只需要知道 "URI→Wrapper" 的映射 — 不需要知道 Wrapper 内部的 Pipeline 怎么处理。Container 新增/删除通过 MapperListener 自动同步到 Mapper — 不需要 Pipeline 感知。这是 **单写者(MapperListener) + 多读者(CoyoteAdapter 只读)** 的并发模型 — 无需读锁。

→ Tomcat 源码分析全部 5 域完成。Spring 生态 Stage 1(I/O 基础)学完: Netty 13 章 + Tomcat 5 域。Stage 2 即将: Spring Framework 核心容器。
