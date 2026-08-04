# 第 2 篇：Pipeline-Valve 请求处理链

> 第 1 篇建立了容器树骨架（Server→Service→Engine→Host→Context→Wrapper）和 Lifecycle 生命周期。
> 本篇回答核心问题：**一个 HTTP 请求进来后，Tomcat 内部如何把它送到 Servlet 手里？**
>
> 答案是 Pipeline-Valve 责任链：四个容器各持有一条阀门链，请求逐级下沉，
> 最后在 Wrapper 层构建 Filter 链并调用 Servlet。
>
> **源码范围**：
> - `org.apache.catalina.Pipeline` / `Valve` / `org.apache.catalina.valves.ValveBase`
> - `org.apache.catalina.core.StandardPipeline`
> - `org.apache.catalina.core.StandardEngineValve` / `StandardHostValve` / `StandardContextValve` / `StandardWrapperValve`
> - `org.apache.catalina.connector.CoyoteAdapter`（service 方法）
> - `org.apache.catalina.core.ApplicationFilterFactory` / `ApplicationFilterChain`
>
> **本篇定位**：Tomcat 核心层（请求处理主路径）。嵌入式与 Standalone 完全一致。

---

## 目录

1. [Pipeline + Valve：Tomcat 的责任链模式](#1-pipeline--valvetomcat-的责任链模式)
2. [四大 StandardValve：请求逐级下沉](#2-四大-standardvalve请求逐级下沉)
3. [CoyoteAdapter.service()：请求进入容器的门户](#3-coyoteadapterservice请求进入容器的门户)
4. [ApplicationFilterChain：Servlet 规范 Filter 链的实现](#4-applicationfilterchainservlet-规范-filter-链的实现)
5. [一次请求的完整阀门之旅](#5-一次请求的完整阀门之旅)
6. [本篇小结与面试要点](#6-本篇小结与面试要点)

---

## 1. Pipeline + Valve：Tomcat 的责任链模式

### 1.1 为什么需要 Valve？

第 1 篇讲过容器树有六层。请求处理时，很多**横切逻辑**需要分层执行：

- Engine 层：取出映射好的 Host（虚拟主机），下沉到 Host 链
- Host 层：取出映射好的 Context（应用），触发请求事件，下沉到 Context 链
- Context 层：访问控制（WEB-INF 保护）、取出映射好的 Wrapper（Servlet），下沉到 Wrapper 链
- Wrapper 层：加载 Servlet、构建 Filter 链、调用 Servlet

> 注意：**路由（哪个 Host/Context/Wrapper）不是 Valve 决定的**，而是 `CoyoteAdapter.postParseRequest()`
> 通过 Mapper（第 3 篇）预先算好放进 Request 的——Valve 只是**取出并使用**（2.1 节详述）。

如果不分层，这些逻辑全堆在一个类里；如果每个容器自己写一套，风格各异。
Tomcat 用 **Pipeline + Valve** 统一：**每个容器一条阀门链，请求沿链逐级处理**。

### 1.2 两个接口 + 一个抽象类

**Valve 接口**（`org/apache/catalina/Valve.java`，118 行）：

```java
public interface Valve {

    // 链的下一个阀门（单向链表指针）
    Valve getNext();
    void setNext(Valve valve);

    // 周期任务（如 Session 过期扫描时也会调用 Valve）
    void backgroundProcess();

    // 核心：处理请求。可以：
    //   1. 直接生成响应并返回（不再往下传）
    //   2. 包装 request/response 后传给下一个阀门（getNext().invoke()）
    void invoke(Request request, Response response) throws IOException, ServletException;

    boolean isAsyncSupported();
}
```

**Pipeline 接口**（`org/apache/catalina/Pipeline.java`，124 行）：

```java
public interface Pipeline extends Contained {

    Valve getBasic();          // 基础阀门：永远在链尾
    void setBasic(Valve valve);
    void addValve(Valve valve); // 加到 basic 之前
    Valve[] getValves();
    void removeValve(Valve valve);
    Valve getFirst();          // 链头：请求的入口
    boolean isAsyncSupported();
}
```

**ValveBase 抽象类**（`org/apache/catalina/valves/ValveBase.java`：37 行）：

```java
public abstract class ValveBase extends LifecycleMBeanBase implements Contained, Valve {
    protected Valve next = null;     // 单向链表指针
    @Override public Valve getNext() { return next; }
    @Override public void setNext(Valve valve) { this.next = valve; }
    // 还提供了 getContainer()/setContainer()（Contained 接口）、MBean 注册
}
```

**关键设计**：
- Valve 是**单向链表**节点：每个 Valve 持有一个 `next` 指针
- Pipeline 只维护两个引用：`first`（链头）和 `basic`（链尾）
- **basic Valve 永远是最后一个**——它是容器自身核心处理逻辑的封装，用户添加的 Valve 只能插在它前面

### 1.3 StandardPipeline：链表的管理者

**源码**：`org/apache/catalina/core/StandardPipeline.java`（395 行）

```java
public class StandardPipeline extends LifecycleBase implements Pipeline {

    protected Valve basic = null;    // 基础阀门（链尾）
    protected Valve first = null;    // 第一个阀门（链头）
    protected Container container = null;
}
```

**addValve 的核心逻辑**（`StandardPipeline.java:268-303`）——插入到 basic 之前：

```java
@Override
public void addValve(Valve valve) {

    // 1. 绑定容器
    if (valve instanceof Contained) {
        ((Contained) valve).setContainer(this.container);
    }

    // 2. 若 Pipeline 已启动，立即启动新 Valve（Lifecycle 联动）
    if (getState().isAvailable()) {
        if (valve instanceof Lifecycle) {
            ((Lifecycle) valve).start();
        }
    }

    // 3. 插入链表：first → ... → 新 Valve → basic
    if (first == null) {
        first = valve;
        valve.setNext(basic);
    } else {
        Valve current = first;
        while (current != null) {
            if (current.getNext() == basic) {   // 找到 basic 前一个
                current.setNext(valve);
                valve.setNext(basic);           // 新阀门插到 basic 前面
                break;
            }
            current = current.getNext();
        }
    }

    container.fireContainerEvent(Container.ADD_VALVE_EVENT, valve);  // 通知监听器
}
```

**getFirst()**（`StandardPipeline.java:388-394`）：

```java
public Valve getFirst() {
    if (first != null) {
        return first;    // 有自定义阀门 → 从第一个开始
    }
    return basic;        // 没有自定义阀门 → 直接就是 basic
}
```

**isAsyncSupported()**（`StandardPipeline.java:103-112`）——遍历整条链：

```java
public boolean isAsyncSupported() {
    Valve valve = (first != null) ? first : basic;
    boolean supported = true;
    while (supported && valve != null) {
        supported = supported & valve.isAsyncSupported();
        valve = valve.getNext();
    }
    return supported;
}
```

### 1.4 四条链是怎么建立的？

每个容器构造时设置自己的 basic Valve（第 1 篇已见）：

```java
// StandardEngine.java:61-65
public StandardEngine() {
    pipeline.setBasic(new StandardEngineValve());
    backgroundProcessorDelay = 10;
}

// StandardHost.java:66-71
public StandardHost() {
    super();
    pipeline.setBasic(new StandardHostValve());
}

// StandardContext 构造（StandardContext.java:157 附近）
//   → pipeline.setBasic(new StandardContextValve())
// StandardWrapper 构造
//   → pipeline.setBasic(new StandardWrapperValve())
```

`ContainerBase` 持有 pipeline（`ContainerBase.java:217`）：

```java
protected final Pipeline pipeline = new StandardPipeline(this);
```

于是容器树中天然存在**四条阀门链**：

```
Engine.pipeline:   [用户 Valve...] → StandardEngineValve
Host.pipeline:     [用户 Valve...] → StandardHostValve
Context.pipeline:  [用户 Valve...] → StandardContextValve
Wrapper.pipeline:  [用户 Valve...] → StandardWrapperValve
```

> **面试点**：Valve 和 Filter 都是责任链模式，有什么区别？——**Valve 是容器内部机制**（`org.apache.catalina`，请求进入容器后的处理管线，可以访问容器内部对象）；**Filter 是 Servlet 规范机制**（`jakarta.servlet`，应用级别的拦截器，只面对 Servlet 规范 API）。Valve 在 Filter 之前执行，且每个容器一层 Valve 链，而 Filter 链只在 Wrapper 层构建一次。用户自定义 Valve 通常用于容器级横切（如 IP 黑名单 `RemoteAddrValve`、访问日志 `AccessLogValve`）。

---

## 2. 四大 StandardValve：请求逐级下沉

### 2.1 总体视图

```
CoyoteAdapter.service()
  └─ Engine.pipeline.getFirst().invoke()
       └─ StandardEngineValve.invoke()      ← 取出 Host（Mapper 已映射）
            └─ Host.pipeline.getFirst().invoke()
                 └─ StandardHostValve.invoke()   ← 取出 Context + 请求事件（Mapper 已映射）
                      └─ Context.pipeline.getFirst().invoke()
                           └─ StandardContextValve.invoke()  ← WEB-INF 保护 + 取出 Wrapper（Mapper 已映射）
                                └─ Wrapper.pipeline.getFirst().invoke()
                                     └─ StandardWrapperValve.invoke()  ← 加载 Servlet + Filter 链
                                          └─ ApplicationFilterChain.doFilter()
                                               └─ Servlet.service()
```

每个 Valve 的职责是**"取出下一层并下沉"**——因为路由信息（Host/Context/Wrapper）是
`CoyoteAdapter.postParseRequest()` 通过 **Mapper**（第 3 篇）预先算好放进 Request 的，
Valve 只是从 Request 里取出来，然后调用下一层 Pipeline。

### 2.2 StandardEngineValve：取出 Host 并下沉

**源码**：`StandardEngineValve.java`（76 行，`invoke` 在 55-75）

```java
@Override
public void invoke(Request request, Response response) throws IOException, ServletException {

    // 从 Request 中取出 Mapper 已映射好的 Host
    Host host = request.getHost();
    if (host == null) {
        // HTTP 1.0 无 Host 头且没有默认主机 → 404
        if (!response.isError()) {
            response.sendError(404);
        }
        return;
    }

    if (request.isAsyncSupported()) {
        request.setAsyncSupported(host.getPipeline().isAsyncSupported());
    }

    // 交给 Host 的阀门链
    host.getPipeline().getFirst().invoke(request, response);
}
```

要点：
- **它自己不处理业务**，只是把请求"递交给 Host 层"
- `request.getHost()` 是哪来的？→ `CoyoteAdapter.postParseRequest()` 用 Mapper 算好的（第 3 篇详讲）
- async 支持逐层传递：每层把下一层的 `isAsyncSupported()` 结果写回 request

### 2.3 StandardHostValve：取出 Context + 请求事件

**源码**：`StandardHostValve.java`（`invoke` 在 79-168）

```java
@Override
public void invoke(Request request, Response response) throws IOException, ServletException {

    // 取出 Mapper 映射好的 Context
    Context context = request.getContext();
    if (context == null) {
        if (!response.isError()) {
            response.sendError(404);
        }
        return;
    }

    if (request.isAsyncSupported()) {
        request.setAsyncSupported(context.getPipeline().isAsyncSupported());
    }

    boolean asyncAtStart = request.isAsync();

    try {
        // 绑定 Webapp 类加载器到当前线程（第 7 篇详讲）
        context.bind(Globals.IS_SECURITY_ENABLED, MY_CLASSLOADER);

        // ★ 触发 ServletRequestListener.requestInitialized()（第 0 篇的契约！）
        //    监听器抛异常 → 返回 false → 请求中止
        if (!asyncAtStart && !context.fireRequestInitEvent(request.getRequest())) {
            return;
        }

        try {
            // 交给 Context 的阀门链（已是错误状态的请求不再转发给应用，直接查错误页）
            if (!response.isErrorReportRequired()) {
                context.getPipeline().getFirst().invoke(request, response);
            }
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            // 记录异常，进入错误处理流程
            if (!response.isErrorReportRequired()) {
                request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, t);
                throwable(request, response, t);
            }
        }

        // 解除挂起：让错误处理能完成/容器能刷新剩余数据
        response.setSuspended(false);

        // ★ 从 request 属性重新取异常（不是 catch 块的局部变量！）
        Throwable t = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // 长请求期间 Context 可能被销毁，防止 NPE
        if (!context.getState().isAvailable()) {
            return;
        }

        // ★ 错误页面处理（若应用配置了 error-page）
        if (response.isErrorReportRequired()) {
            // 若错误已阻止 IO，不生成永远读不到的错误报告
            AtomicBoolean result = new AtomicBoolean(false);
            response.getCoyoteResponse().action(ActionCode.IS_IO_ALLOWED, result);
            if (result.get()) {
                if (t != null) {
                    throwable(request, response, t);   // 按异常匹配 error-page
                } else {
                    status(request, response);         // 按状态码匹配 error-page
                }
            }
        }

        // ★ 触发 ServletRequestListener.requestDestroyed()
        if (!request.isAsync() && !asyncAtStart) {
            context.fireRequestDestroyEvent(request.getRequest());
        }
    } finally {
        // 规范要求：请求结束时访问一次 Session，更新最后访问时间
        if (context.getAlwaysAccessSession()) {
            request.getSession(false);
        }
        // 解除类加载器绑定
        context.unbind(Globals.IS_SECURITY_ENABLED, MY_CLASSLOADER);
    }
}
```

要点（与第 0 篇契约的对应）：
- **`fireRequestInitEvent()` / `fireRequestDestroyEvent()`**：`ServletRequestListener` 接口的触发点——每次请求进来/出去都会回调；init 监听器抛异常会中止请求
- **错误页面处理**：应用配置了 `error-page` 时，错误响应在这里被路由到错误页——`throwable()` 按异常类型匹配、`status()` 按状态码匹配，内部用 `RequestDispatcher` 做 forward（第 0 篇的 FORWARD 分发）
- **`setSuspended(false)`**：请求/响应回到容器控制后解除挂起，让错误处理和剩余数据刷新能完成
- **类加载器绑定**：请求处理期间线程上下文类加载器切换为 Webapp 类加载器
- **Session 访问**：finally 中按规范要求访问一次 Session（`getSession(false)`）以更新最后访问时间

### 2.4 StandardContextValve：WEB-INF 保护 + 取出 Wrapper

**源码**：`StandardContextValve.java`（92 行，`invoke` 在 60-91）

```java
@Override
public void invoke(Request request, Response response) throws IOException, ServletException {

    // ★ 安全：禁止直接访问 WEB-INF 和 META-INF 下的资源
    MessageBytes requestPathMB = request.getRequestPathMB();
    if ((requestPathMB.startsWithIgnoreCase("/META-INF/", 0)) || (requestPathMB.equalsIgnoreCase("/META-INF")) ||
            (requestPathMB.startsWithIgnoreCase("/WEB-INF/", 0)) || (requestPathMB.equalsIgnoreCase("/WEB-INF"))) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }

    // 取出 Mapper 映射好的 Wrapper
    Wrapper wrapper = request.getWrapper();
    if (wrapper == null || wrapper.isUnavailable()) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }

    // 100-continue 确认
    try {
        response.sendAcknowledgement(ContinueResponseTiming.IMMEDIATELY);
    } catch (IOException ioe) {
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, ioe);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
    }

    if (request.isAsyncSupported()) {
        request.setAsyncSupported(wrapper.getPipeline().isAsyncSupported());
    }

    // 交给 Wrapper 的阀门链
    wrapper.getPipeline().getFirst().invoke(request, response);
}
```

要点：
- **WEB-INF/META-INF 保护**：直接返回 404（防止通过 URL 访问应用内部文件）
- 100-continue 确认（Expect: 100-continue 协议细节）
- 同样只是"取出下一层并下沉"

### 2.5 StandardWrapperValve：真正干活的阀门

**源码**：`StandardWrapperValve.java`（`invoke` 在 85-256，172 行，复杂度 35）

这是**四条链中最复杂的一个**，因为 Servlet 的加载和调用都发生在这一层：

```java
@Override
public void invoke(Request request, Response response) throws IOException, ServletException {

    boolean unavailable = false;
    Throwable throwable = null;
    long t1 = System.currentTimeMillis();
    requestCount.incrementAndGet();
    StandardWrapper wrapper = (StandardWrapper) getContainer();
    Servlet servlet = null;
    Context context = (Context) wrapper.getParent();

    // 1. Context 不可用检查
    if (!context.getState().isAvailable()) {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, ...);
        unavailable = true;
    }

    // 2. Wrapper 不可用检查（如 Servlet 初始化失败被标记 unavailable）
    if (!unavailable && wrapper.isUnavailable()) {
        container.getLogger().info(...);
        checkWrapperAvailable(response, wrapper);
        unavailable = true;
    }

    // 3. ★ 分配 Servlet 实例（懒加载：第一次请求才 loadServlet + init）
    try {
        if (!unavailable) {
            servlet = wrapper.allocate();   // 第 4 篇详讲
        }
    } catch (UnavailableException e) {
        checkWrapperAvailable(response, wrapper);
    } catch (ServletException | Throwable e) {
        throwable = e;
        exception(request, response, e);
        servlet = null;
    }

    // 4. 设置 DispatcherType 属性（REQUEST/ASYNC...）
    MessageBytes requestPathMB = request.getRequestPathMB();
    DispatcherType dispatcherType = DispatcherType.REQUEST;
    if (request.getDispatcherType() == DispatcherType.ASYNC) {
        dispatcherType = DispatcherType.ASYNC;
    }
    request.setAttribute(Globals.DISPATCHER_TYPE_ATTR, dispatcherType);
    request.setAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR, requestPathMB);

    // 5. ★ 构建 Filter 链（ApplicationFilterFactory）
    ApplicationFilterChain filterChain = ApplicationFilterFactory.createFilterChain(request, wrapper, servlet);

    // 6. ★ 调用 Filter 链（链尾就是 servlet.service()）
    try {
        if ((servlet != null) && (filterChain != null)) {
            if (request.isAsyncDispatching()) {
                request.getAsyncContextInternal().doInternalDispatch();   // 异步分发
            } else {
                filterChain.doFilter(request.getRequest(), response.getResponse());  // ← 常规路径
            }
        }
    } catch (BadRequestException e) {
        throwable = e;
        exception(request, response, e, HttpServletResponse.SC_BAD_REQUEST);
    } catch (IOException | ServletException | Throwable e) {
        throwable = e;
        exception(request, response, e);
    } finally {
        // 7. 释放 Filter 链（回收对象）
        if (filterChain != null) {
            filterChain.release();
        }

        // 8. 归还 Servlet 实例（递减并发计数）
        try {
            if (servlet != null) {
                wrapper.deallocate(servlet);
            }
        } catch (Throwable e) { ... }

        // 9. 统计：处理时间、最大/最小耗时
        long t2 = System.currentTimeMillis();
        long time = t2 - t1;
        processingTime.add(time);
        if (time > maxTime) maxTime = time;
        if (time < minTime) minTime = time;
    }
}
```

**StandardWrapperValve 的九步职责**：

| 步骤 | 职责 | 关键调用 |
|---|---|---|
| 1-2 | 可用性检查 | `context.getState().isAvailable()` / `wrapper.isUnavailable()` |
| 3 | **Servlet 懒加载** | `wrapper.allocate()`（首次请求才实例化+init） |
| 4 | DispatcherType 属性 | `request.setAttribute(DISPATCHER_TYPE_ATTR, ...)` |
| 5 | **构建 Filter 链** | `ApplicationFilterFactory.createFilterChain()` |
| 6 | **调用链** | `filterChain.doFilter()`（链尾 = `servlet.service()`） |
| 7 | 释放链 | `filterChain.release()` |
| 8 | 归还 Servlet | `wrapper.deallocate(servlet)`（`countAllocated` 递减） |
| 9 | 统计 | 处理时间、max/min 耗时 |

> **面试点**：Servlet 是在**启动时**还是**首次请求时**初始化？——取决于 `load-on-startup`（第 1 篇 6.3 节）：`>= 0` 启动时加载（`StandardContext.loadOnStartup()`），`-1` 首次请求时通过 `StandardWrapperValve.allocate()` 懒加载。Spring Boot 的 DispatcherServlet 默认 `-1`。

---

## 3. CoyoteAdapter.service()：请求进入容器的门户

### 3.1 定位

`CoyoteAdapter` 是 Catalina 与 Coyote 两个世界的桥梁（第 1 篇 1.1 节）：
Coyote 层（`Http11Processor`）解析完 HTTP 报文后，调用 `adapter.service(coyoteRequest, coyoteResponse)`，
进入 Catalina 世界。

**源码**：`org/apache/catalina/connector/CoyoteAdapter.java`（1315 行，`service` 在 302-426）

### 3.2 service() 主流程

```java
@Override
public void service(org.apache.coyote.Request req, org.apache.coyote.Response res) throws Exception {

    // ── 第一步：对象转换（coyote Request → catalina Request）──
    Request request = (Request) req.getNote(ADAPTER_NOTES);
    Response response = (Response) res.getNote(ADAPTER_NOTES);

    if (request == null) {
        // 首次：创建 catalina 层 Request/Response，并双向关联
        request = connector.createRequest();
        request.setCoyoteRequest(req);
        response = connector.createResponse();
        response.setCoyoteResponse(res);
        request.setResponse(response);
        response.setRequest(request);

        // 挂到 coyote Request 的 note 上（复用：下次不再创建）
        req.setNote(ADAPTER_NOTES, request);
        res.setNote(ADAPTER_NOTES, response);

        // 设置 query string 编码
        req.getParameters().setQueryStringCharset(connector.getURICharset());
    }

    // ── 第二步：X-Powered-By 头 ──
    if (connector.getXpoweredBy()) {
        response.addHeader("X-Powered-By", POWERED_BY);
    }

    boolean async = false;
    boolean postParseSuccess = false;
    req.setRequestThread();   // 标记当前线程正在处理请求

    try {
        // ── 第三步：请求解析与映射（核心）──
        // 1. 设置 scheme/secure（https 判断）
        // 2. URI 解码（%xx）、路径参数剥离、规范化（//、..、. 处理）
        // 3. ★ Mapper 映射：serverName+decodedURI → Host/Context/Wrapper
        // 4. Session ID 解析（Cookie/URL）
        postParseSuccess = postParseRequest(req, request, res, response);

        if (postParseSuccess) {
            // async 支持标记（沿 Valve 链逐层传递）
            request.setAsyncSupported(connector.getService().getContainer().getPipeline().isAsyncSupported());

            // ── 第四步：★ 进入容器（本篇核心）──
            // Engine.pipeline.getFirst().invoke() → 四大 Valve 逐级下沉 → Servlet
            connector.getService().getContainer().getPipeline().getFirst().invoke(request, response);
        }

        // ── 第五步：异步处理 ──
        if (request.isAsync()) {
            async = true;
            // 异步请求：不回收对象，等待异步完成
            ReadListener readListener = req.getReadListener();
            if (readListener != null && request.isFinished()) {
                // 非阻塞 IO：数据已全部读完，在 Webapp 类加载器下通知监听器
                ClassLoader oldCL = request.getContext().bind(false, null);
                try {
                    if (req.sendAllDataReadEvent()) {
                        req.getReadListener().onAllDataRead();
                    }
                } finally {
                    request.getContext().unbind(false, oldCL);
                }
            }
            // 异步中的错误处理
            Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            if (!request.isAsyncCompleting() && throwable != null) {
                request.getAsyncContextInternal().setErrorState(throwable, true);
            }
        } else {
            // 同步请求：完成收尾
            request.finishRequest();
            response.finishResponse();
        }
    } catch (IOException e) {
        // 忽略（客户端断开常见）
    } finally {
        // 异步完成时若连接将被强制关闭，先触发 onComplete() 回调
        AtomicBoolean error = new AtomicBoolean(false);
        res.action(ActionCode.IS_ERROR, error);
        if (request.isAsyncCompleting() && error.get()) {
            res.action(ActionCode.ASYNC_POST_PROCESS, null);
            async = false;
        }

        // ── 第六步：收尾 ──
        // 访问日志（AccessLog）
        if (!async && postParseSuccess) {
            Context context = request.getContext();
            long time = System.nanoTime() - req.getStartTimeNanos();
            if (context != null) {
                context.logAccess(request, response, time, false);
            } else if (response.isError()) {
                // 映射失败（无 Context）：退级到 Host/Engine 记录
                Host host = request.getHost();
                if (host != null) {
                    host.logAccess(request, response, time, false);
                } else {
                    connector.getService().getContainer().logAccess(request, response, time, false);
                }
            }
        }

        // 清理线程状态
        req.getRequestProcessor().setWorkerThreadName(null);
        req.clearRequestThread();

        // ── 第七步：★ 对象回收（非异步）──
        if (!async) {
            updateWrapperErrorCount(request, response);
            request.recycle();     // 清空 Request 状态，归还对象池复用
            response.recycle();
        }
    }
}
```

### 3.3 三个关键设计

**① 对象复用**：catalina `Request`/`Response` 不是每请求新建，而是创建一次后挂在
`coyote Request` 的 **note** 槽位上，后续请求直接取用；用完后 `recycle()` 清空状态。
（第 4 篇还会看到 `StandardWrapper` 对 Servlet 实例的类似复用哲学。）

**② 异步不回收**：`if (!async) { request.recycle(); response.recycle(); }`——
异步请求的 Request/Response 对象必须保留，等异步线程 `complete()` 后再回收。
（对应第 0 篇 AsyncContext 契约的 Tomcat 实现。）

**③ postParseRequest 是路由前置**：进入 Valve 链之前，Mapper 已经把
Host/Context/Wrapper 全部映射好塞进 request（`request.getHost()`/`getContext()`/`getWrapper()`），
所以四大 Valve 才能"只取不查"。

> **面试点**：`coyote.Request` 和 `catalina Request` 有什么区别？——前者是网络层协议对象（`org.apache.coyote.Request`，只含 HTTP 报文解析结果），后者是容器层规范对象（`org.apache.catalina.connector.Request`，实现 Servlet 规范、含上下文信息），两者通过 `setCoyoteRequest` 关联，`CoyoteAdapter` 负责转换。应用拿到的是**再包一层的门面** `RequestFacade`（第 0 篇对照表）。

---

## 4. ApplicationFilterChain：Servlet 规范 Filter 链的实现

### 4.1 构建：ApplicationFilterFactory

**源码**：`org/apache/catalina/core/ApplicationFilterFactory.java`（207 行）

`createFilterChain`（57-139 行）在 StandardWrapperValve 第 5 步被调用，**每次请求构建一次**：

```java
public static ApplicationFilterChain createFilterChain(ServletRequest request, Wrapper wrapper, Servlet servlet) {

    if (servlet == null) {
        return null;                       // 无 Servlet → 无链
    }

    // 对象复用：非安全模式下从 Request 里取旧链复用
    ApplicationFilterChain filterChain = null;
    if (request instanceof Request) {
        Request req = (Request) request;
        filterChain = (ApplicationFilterChain) req.getFilterChain();
        if (filterChain == null) {
            filterChain = new ApplicationFilterChain();
            req.setFilterChain(filterChain);
        }
    } else {
        filterChain = new ApplicationFilterChain();   // 分发场景新建
    }

    filterChain.setServlet(servlet);
    filterChain.setServletSupportsAsync(wrapper.isAsyncSupported());

    StandardContext context = (StandardContext) wrapper.getParent();
    filterChain.setDispatcherWrapsSameObject(context.getDispatcherWrapsSameObject());
    FilterMap filterMaps[] = context.findFilterMaps();   // 应用的 Filter 映射表

    if (filterMaps == null || filterMaps.length == 0) {
        return filterChain;                // 无 Filter → 直接到 Servlet
    }

    DispatcherType dispatcher = (DispatcherType) request.getAttribute(Globals.DISPATCHER_TYPE_ATTR);
    String requestPath = FilterUtil.getRequestPath(request);
    String servletName = wrapper.getName();

    // ★ 第一轮：URL 模式匹配的 Filter（按声明顺序）
    for (FilterMap filterMap : filterMaps) {
        if (!matchDispatcher(filterMap, dispatcher)) continue;      // DispatcherType 匹配
        if (!FilterUtil.matchFiltersURL(filterMap, requestPath)) continue;  // URL 匹配
        ApplicationFilterConfig filterConfig =
                (ApplicationFilterConfig) context.findFilterConfig(filterMap.getFilterName());
        if (filterConfig == null) continue;
        filterChain.addFilter(filterConfig);    // 加入链
    }

    // ★ 第二轮：Servlet 名匹配的 Filter
    for (FilterMap filterMap : filterMaps) {
        if (!matchDispatcher(filterMap, dispatcher)) continue;
        if (!matchFiltersServlet(filterMap, servletName)) continue;  // Servlet 名匹配
        ApplicationFilterConfig filterConfig =
                (ApplicationFilterConfig) context.findFilterConfig(filterMap.getFilterName());
        if (filterConfig == null) continue;
        filterChain.addFilter(filterConfig);
    }

    return filterChain;
}
```

**匹配规则（与第 0 篇 6 节的规范对应）**：
- **DispatcherType 匹配**（`matchDispatcher`，177-206 行）：`FilterMap` 用位掩码存储
  （`FORWARD=2/INCLUDE=4/REQUEST=8/ERROR=1/ASYNC=16`），`&` 运算判断
- **URL 匹配**（`FilterUtil.matchFiltersURL`）：精确路径、`/*` 前缀、`*.ext` 后缀
- **Servlet 名匹配**（`matchFiltersServlet`，152-170 行）：含 `*` 通配（匹配所有 Servlet）
- **顺序**：先 URL 匹配的 Filter（声明顺序），后 Servlet 名匹配的 Filter——规范规定
- **防重复**：`addFilter()`（`ApplicationFilterChain.java:239-253`）会检查同一 Filter 不重复入链（一个 Filter 可能同时匹配 URL 和 Servlet 名）

### 4.2 执行：ApplicationFilterChain

**源码**：`org/apache/catalina/core/ApplicationFilterChain.java`（313 行）

**内部状态**（60-93 行）：

```java
private ApplicationFilterConfig[] filters = new ApplicationFilterConfig[0];  // 链上的 Filter 数组
private int pos = 0;   // 当前执行位置
private int n = 0;     // Filter 总数
private Servlet servlet = null;              // 链尾的 Servlet
private boolean servletSupportsAsync = false;
```

**doFilter → internalDoFilter**（116-209 行）——**递归调用**：

```java
@Override
public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
    if (Globals.IS_SECURITY_ENABLED) {
        // 安全模式下用 doPrivileged 包装
        ...
    } else {
        internalDoFilter(request, response);
    }
}

private void internalDoFilter(ServletRequest request, ServletResponse response)
        throws IOException, ServletException {

    // ★ 核心：还有 Filter 就执行 Filter，否则执行 Servlet
    if (pos < n) {
        ApplicationFilterConfig filterConfig = filters[pos++];   // 取下一个并前进

        try {
            Filter filter = filterConfig.getFilter();

            // async 支持标记传递
            if (request.isAsyncSupported() && !(filterConfig.getFilterDef().getAsyncSupportedBoolean())) {
                request.setAttribute(Globals.ASYNC_SUPPORTED_ATTR, Boolean.FALSE);
            }

            // ★ 调用 Filter 的 doFilter（应用代码在这里执行！）
            filter.doFilter(request, response, this);   // this = FilterChain 自身
            // 注意：应用 Filter 内部调用 chain.doFilter() 会递归回到这里
        } catch (IOException | ServletException | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            e = ExceptionUtils.unwrapInvocationTargetException(e);
            ExceptionUtils.handleThrowable(e);
            throw new ServletException(sm.getString("filterChain.filter"), e);
        }
        return;
    }

    // ★ 链尾：调用 Servlet（第 0 篇 Filter 契约的终点）
    try {
        // 分发包装场景（forward/include 时 dispatcherWrapsSameObject=true）：
        // 记录"当前线程最后服务的请求/响应"，供 RequestDispatcher 包装器检查
        if (dispatcherWrapsSameObject) {
            lastServicedRequest.set(request);
            lastServicedResponse.set(response);
        }

        if (request.isAsyncSupported() && !servletSupportsAsync) {
            request.setAttribute(Globals.ASYNC_SUPPORTED_ATTR, Boolean.FALSE);
        }
        servlet.service(request, response);    // ← 业务代码入口！
    } catch (IOException | ServletException | RuntimeException e) {
        throw e;
    } catch (Throwable e) {
        e = ExceptionUtils.unwrapInvocationTargetException(e);
        ExceptionUtils.handleThrowable(e);
        throw new ServletException(sm.getString("filterChain.servlet"), e);
    } finally {
        if (dispatcherWrapsSameObject) {
            lastServicedRequest.set(null);
            lastServicedResponse.set(null);
        }
    }
}
```

**执行模型（与第 0 篇 6.2 的示意图完全对应）**：

```
internalDoFilter(pos=0)
  └─ filters[0].doFilter(req, res, chain)
       └─ [应用 Filter 前置逻辑]
            └─ chain.doFilter()  ──→  internalDoFilter(pos=1)   ← 递归
                 └─ filters[1].doFilter(...)
                      └─ chain.doFilter()  ──→  internalDoFilter(pos=2)
                           └─ pos==n → servlet.service()        ← 链尾
                      └─ [应用 Filter 后置逻辑]
            └─ [应用 Filter 后置逻辑]
```

**递归的关键**：Filter 的 `chain.doFilter()` 参数 `this` 就是链本身，
每次调用 `internalDoFilter` 的 `pos` 都 +1——**执行权在应用代码手中**：
Filter 可以不调 `chain.doFilter()` 直接拦截（第 0 篇 6.2 的"拦截"语义）。

**recycle()**（StandardWrapperValve 第 7 步调用，`ApplicationFilterChain.java:259-268`）：

```java
void release() {
    for (int i = 0; i < n; i++) {
        filters[i] = null;   // 清空引用，方便 GC
    }
    n = 0;                   // Filter 数归零
    pos = 0;                 // 位置归零
    servlet = null;
    servletSupportsAsync = false;
    dispatcherWrapsSameObject = false;
}
```

注意：`release()` **不重建数组**——`filters` 数组保留（已扩容的容量继续用），下次请求 `addFilter()` 时从 `n=0` 开始填。链对象存在 `Request.getFilterChain()` 里被复用。

> **面试点**：Filter 链是每次请求都重新构建吗？——`ApplicationFilterChain` 对象本身会复用
> （存在 `Request.getFilterChain()` 里），但**匹配过程（createFilterChain）每次请求都执行**，
> Filter 实例（`ApplicationFilterConfig` 内）是单例复用的。`StandardWrapperValve` 第 7 步
> `release()` 后链归零，下次请求重新匹配。

---

## 5. 一次请求的完整阀门之旅

结合第 1 篇的启动链路和第 0 篇的契约，把一次请求从 socket 到 Servlet 的完整路径画出来：

```
Coyote NioEndpoint（第 6 篇）
  └─ Http11Processor（解析 HTTP 报文）
       └─ org.apache.coyote.Request（网络层对象）
            └─ CoyoteAdapter.service()                    [CoyoteAdapter.java:302]
                 ├─ 创建/复用 catalina Request/Response    [connector.createRequest()]
                 ├─ postParseRequest()                    [CoyoteAdapter.java:559]
                 │    ├─ URI 解码 + 规范化 + 路径参数剥离
                 │    ├─ ★ Mapper.map() → Host/Context/Wrapper 映射（第 3 篇）
                 │    └─ Session ID 解析（Cookie/URL）
                 └─ Engine.pipeline.getFirst().invoke()    [CoyoteAdapter.java:344]
                      └─ StandardEngineValve.invoke()      [取出 Host]
                           └─ Host.pipeline.getFirst().invoke()
                                └─ StandardHostValve.invoke()   [取出 Context]
                                     ├─ fireRequestInitEvent()   [ServletRequestListener]
                                     ├─ context.pipeline.getFirst().invoke()
                                     │    └─ StandardContextValve.invoke()  [WEB-INF 保护 + 取出 Wrapper]
                                     │         └─ Wrapper.pipeline.getFirst().invoke()
                                     │              └─ StandardWrapperValve.invoke()
                                     │                   ├─ wrapper.allocate()      [Servlet 懒加载]
                                     │                   ├─ createFilterChain()     [匹配 Filter]
                                     │                   └─ filterChain.doFilter()
                                     │                        └─ ApplicationFilterChain.internalDoFilter()
                                     │                             ├─ filter.doFilter() × N（应用 Filter）
                                     │                             └─ servlet.service()  ← ★ 业务入口
                                     ├─ 错误页面处理（error-page）
                                     └─ fireRequestDestroyEvent()  [ServletRequestListener]
```

**一个请求中关键对象的关系**：

```
org.apache.coyote.Request（网络层）
    │ setCoyoteRequest / note(ADAPTER_NOTES)
    ▼
org.apache.catalina.connector.Request（容器层）
    │ setRequest()
    ▼
RequestFacade（门面，应用可见，第 0 篇对照表）
    │
    ├── getHost() → StandardHost
    ├── getContext() → StandardContext（→ ServletContext 门面）
    └── getWrapper() → StandardWrapper
```

**两个链的对比（面试高频）**：

| | Valve 链 | Filter 链 |
|---|---|---|
| 层次 | 容器层（Engine/Host/Context/Wrapper 各一条） | 应用层（Wrapper 内一条） |
| 接口 | `org.apache.catalina.Valve` | `jakarta.servlet.Filter` |
| 访问权限 | 容器内部对象 | 仅 Servlet 规范 API |
| 构建时机 | 容器启动时 | **每次请求**（createFilterChain） |
| 执行顺序 | 先执行 | 后执行（Valve 链尾构建它） |
| 典型实现 | `AccessLogValve`/`RemoteAddrValve`/`ErrorReportValve` | `CharacterEncodingFilter`/`FilterChainProxy` |

---

## 6. 本篇小结与面试要点

### 6.1 本篇地图

```
第 1 篇：容器树 + Lifecycle（本篇的执行骨架）
第 2 篇（本篇）：Pipeline-Valve 请求处理链   ← 请求如何进入容器
第 3 篇：Mapper 路由（Valve 中 request.getHost() 的来源）
第 4 篇：Servlet 生命周期（allocate/loadServlet 的详细实现）
第 5 篇：Connector（CoyoteAdapter.service 的调用者）
第 6 篇：NIO 网络层（请求从哪里来）
```

### 6.2 面试要点速查

1. **Pipeline-Valve 是责任链**：每个容器一条链（`first → ... → basic`），`basic` 是容器核心逻辑（四大 StandardValve），用户 Valve 插在 basic 前
2. **四大 StandardValve 的职责**：EngineValve 取 Host、HostValve 取 Context+请求事件、ContextValve 保护 WEB-INF+取 Wrapper、WrapperValve 加载 Servlet+构建并执行 Filter 链——**路由本身由 Mapper 决定，Valve 只负责取出并下沉**
3. **Valve vs Filter**：容器内部机制 vs 规范机制；执行顺序 Valve 先 Filter 后；Valve 每容器一层，Filter 只在 Wrapper 层
4. **CoyoteAdapter.service() 七步**：对象转换 → X-Powered-By → postParseRequest（路由前置）→ Pipeline 调用 → 异步处理 → 收尾（访问日志 + 异步完成回调）→ **非异步回收对象**
5. **两个 Request**：`coyote.Request`（网络层）→ `catalina Request`（容器层）→ `RequestFacade`（门面），note 槽位复用对象
6. **异步不回收**：`if (!async) request.recycle()`——异步请求保留对象直到 complete()
7. **Filter 链匹配规则**：DispatcherType 位掩码 → URL 匹配（精确/前缀/后缀）→ Servlet 名匹配；先 URL 后 Servlet 名
8. **FilterChain 执行模型**：`internalDoFilter` 递归，`pos` 指针推进，链尾 `servlet.service()`；Filter 不调 `chain.doFilter()` 即拦截
9. **Servlet 懒加载**：`load-on-startup=-1`（DispatcherServlet 默认）时首次请求 `allocate()` 才实例化+init
10. **请求事件**：`ServletRequestListener.requestInitialized()` 在 `StandardHostValve` 触发（每次请求），`contextInitialized()` 在 `StandardContext.listenerStart()` 触发（应用启动，第 1 篇 ⑳ 步）
