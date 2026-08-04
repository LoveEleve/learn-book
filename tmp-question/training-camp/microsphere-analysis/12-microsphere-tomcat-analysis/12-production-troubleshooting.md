# 第 12 篇：故障排查实战

> 前 11 篇建立了从架构到安全运维的完整认知体系。本篇是系列的**收官篇**，
> 回答生产值班时最直接的问题：**服务出问题了，怎么在 5 分钟内定位到根因？**
>
> 与第 10 篇（性能调优）的区别：第 10 篇是"配置怎么定"的**事前决策**；
> 本篇是"线上已经出问题"的**事后定位**——给出每种故障的症状、定位手段、和能直接复现验证的源码依据。
>
> **源码范围**：
> - `PortInUseException` / `PortInUseFailureAnalyzer`（Spring Boot 启动失败分析）
> - `TomcatStarter` / `TomcatWebServer.rethrowDeferredStartupExceptions`（第 8 篇已见，故障视角复习）
> - `LimitLatch` / `Acceptor`（第 6 篇已见，连接打满视角）
> - `TaskQueue` / `ThreadPoolExecutor`（第 6 篇已见，线程池耗尽视角）
> - `WebappClassLoaderBase`（第 7 篇已见，内存泄漏视角）
> - `AbstractAccessLogValve`（访问日志耗时定位）
>
> **本篇定位**：实战篇（故障驱动），结构为"故障 → 症状 → 定位手段 → 回扣源码 → 处置建议"。
> 与第 10/11 篇合称"实战三部曲"：调优（事前）→ 安全运维（防护）→ 故障排查（事后）。

---

## 目录

1. [故障排查的通用方法论](#1-故障排查的通用方法论)
2. [故障一：启动失败](#2-故障一启动失败)
3. [故障二：连接数打满](#3-故障二连接数打满)
4. [故障三：线程池耗尽](#4-故障三线程池耗尽)
5. [故障四：类加载器内存泄漏](#5-故障四类加载器内存泄漏)
6. [故障五：慢请求定位](#6-故障五慢请求定位)
7. [本篇小结与全系列收尾](#7-本篇小结与全系列收尾)

---

## 1. 故障排查的通用方法论

### 1.1 先分层，再深挖

Tomcat + Spring Boot 的故障可以按**发生阶段**分成两类，排查思路完全不同：

| 阶段 | 特征 | 排查起点 |
|---|---|---|
| **启动阶段**（进程还没跑起来） | 应用日志里能看到异常堆栈，进程直接退出 | 看异常类型 + `FailureAnalyzer` 输出的友好提示 |
| **运行阶段**（进程活着但请求异常） | 日志可能没有明显异常，需要主动探测 | jstack + 监控指标 + 访问日志，先分类症状再定位 |

**运行阶段故障的第一步永远是分类**：新连接建不上？已有连接变慢？CPU 飙高？内存持续增长？——这四类症状对应完全不同的瓶颈（本篇第 3~6 节逐一展开），**症状分类错了，后面的定位手段全部无效**。这是本篇要反复强调的核心方法论。

### 1.2 排查工具箱

生产环境常驻的排查手段，按侵入性从低到高排列：

| 工具 | 侵入性 | 用途 |
|---|---|---|
| 访问日志（`%D` 耗时/状态码分布） | 无 | 事后统计分析，发现耗时/错误的整体趋势 |
| Actuator `/actuator/metrics`、`/actuator/threaddump` | 低 | 实时指标快照，不阻塞业务线程 |
| `jstack <pid>` | 低（对 JVM 短暂停顿） | 线程栈快照，定位阻塞/死锁/池子耗尽 |
| `jmap -histo <pid>` / heap dump | 中～高（大堆会阻塞较久） | 内存分布分析，定位内存泄漏 |
| JMX（`jconsole`/VisualVM） | 低 | 实时线程池/连接数指标（需要 `mbeanregistry.enabled=true`，第 11 篇 4.4） |

**核心原则**：先用无侵入或低侵入手段（日志、Actuator）**缩小范围**，确认大概是哪一类问题，再用 `jstack`/heap dump 精确定位——不要一上来就 dump 堆，大堆 dump 期间应用会有明显停顿，本身可能造成二次故障。

---

## 2. 故障一：启动失败

启动失败是最容易排查的一类——因为**异常会直接打在日志里，进程也不会继续跑**，不需要"主动探测"。关键是**看懂异常类型**，别被表面的异常信息误导（比如一个端口占用问题，异常栈可能层层包裹到很深，第一眼看到的往往是外层的 `WebServerException`，而不是根因）。

### 2.1 症状：端口被占用

**典型错误日志**：

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Web server failed to start. Port 8080 was already in use.

Action:

Identify and stop the process that's listening on port 8080 or configure this application to
listen on another port (server.port).
```

这段"友好提示"格式是 Spring Boot 的 `FailureAnalyzer` 机制生成的，不是原始异常栈。**源码走读**（`PortInUseFailureAnalyzer.java`，完整类）：

```java
class PortInUseFailureAnalyzer extends AbstractFailureAnalyzer<PortInUseException> {
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, PortInUseException cause) {
        return new FailureAnalysis("Web server failed to start. Port " + cause.getPort() + " was already in use.",
                "Identify and stop the process that's listening on port " + cause.getPort() + " or configure this "
                        + "application to listen on another port.",
                cause);
    }
}
```

它通过 `META-INF/spring.factories` 文件里的 `FailureAnalyzer` 扫描机制注册（第 2.1 节末尾核实过具体的注册方式），专门识别 `PortInUseException` 类型的异常并生成这段结构化提示（"Description" + "Action" 两段式），比裸露的堆栈更适合直接展示给运维/开发。

**异常的产生链路**（`TomcatWebServer.start()`，`TomcatWebServer.java:228-258`）：

```java
@Override
public void start() throws WebServerException {
    synchronized (this.monitor) {
        if (this.started) {
            return;
        }
        try {
            addPreviouslyRemovedConnectors();
            Connector connector = this.tomcat.getConnector();
            if (connector != null && this.autoStart) {
                performDeferredLoadOnStartup();
            }
            checkThatConnectorsHaveStarted();
            this.started = true;
            logger.info(getStartedLogMessage());
        }
        catch (ConnectorStartFailedException ex) {
            stopSilently();
            throw ex;
        }
        catch (Exception ex) {
            PortInUseException.throwIfPortBindingException(ex, () -> this.tomcat.getConnector().getPort());   // ★ 关键识别点
            throw new WebServerException("Unable to start embedded Tomcat server", ex);
        }
        finally {
            ...
        }
    }
}
```

**`throwIfPortBindingException` 的判定逻辑**（`PortInUseException.java:69-103`，完整方法链）：

```java
public static void throwIfPortBindingException(Exception ex, IntSupplier port) {
    ifPortBindingException(ex, (bindException) -> {
        throw new PortInUseException(port.getAsInt(), ex);
    });
}

public static void ifPortBindingException(Exception ex, Consumer<BindException> action) {
    ifCausedBy(ex, BindException.class, (bindException) -> {
        // BindException 也可能是因为地址无法分配（不是端口占用）导致的
        if (bindException.getMessage().toLowerCase(Locale.ROOT).contains("in use")) {   // ★ 关键字匹配
            action.accept(bindException);
        }
    });
}

public static <E extends Exception> void ifCausedBy(Exception ex, Class<E> causedBy, Consumer<E> action) {
    Throwable candidate = ex;
    while (candidate != null) {
        if (causedBy.isInstance(candidate)) {    // ★ 沿着 cause 链一直向下找
            action.accept((E) candidate);
            return;
        }
        candidate = candidate.getCause();
    }
}
```

**关键设计细节**：这里做了两层判定，缺一不可——首先要在**异常的 cause 链**里找到 `java.net.BindException`（`ifCausedBy` 沿着 `getCause()` 一路往下找，不要求是最外层异常），其次要**检查异常消息包含 "in use"**（不是所有 `BindException` 都是端口占用，还可能是"地址无法分配"这种网络配置问题——比如配置了一个本机不存在的网卡地址）。只有两层都满足，才会包装成 `PortInUseException` 抛出，进而被 `PortInUseFailureAnalyzer` 识别并生成友好提示。

**定位手段**：

```bash
# 找出占用端口的进程
lsof -i :8080
# 或
netstat -tlnp | grep 8080
# 或（新系统）
ss -tlnp | grep 8080
```

**处置**：确认该端口是被别的服务合法占用（配置错误，改自己的端口）还是**同一个应用启动了两次**（常见于部署脚本没有先杀掉旧进程就直接启动新进程——生产环境这种情况比开发环境更隐蔽，因为可能是**上一次滚动更新失败留下的僵尸进程**）。

**补充（第三轮 review 补全的一个知识点）：还有一种更隐蔽的端口占用故障，发生在"第二阶段启动"而不是初次绑定端口时**——回顾第 5/8 篇的两阶段启动机制：Spring Boot 先在 `getWebServer()` 阶段创建 Connector 并**摘走**（`removeServiceConnectors`），真正的端口 `bind` 发生在 `TomcatWebServer.start()`（`finishRefresh()` 触发，`addPreviouslyRemovedConnectors()` 把 Connector 加回去）。如果这一步失败（比如端口在两阶段之间被别的进程抢先占用——虽然概率低但在高并发部署场景下并非不可能），走的是另一条异常路径：

```java
// TomcatWebServer.java:266-277
private void checkThatConnectorsHaveStarted() {
    checkConnectorHasStarted(this.tomcat.getConnector());
    for (Connector connector : this.tomcat.getService().findConnectors()) {
        checkConnectorHasStarted(connector);
    }
}

private void checkConnectorHasStarted(Connector connector) {
    if (LifecycleState.FAILED.equals(connector.getState())) {
        throw new ConnectorStartFailedException(connector.getPort());   // ★ 另一个专属异常类型
    }
}
```

`ConnectorStartFailedException` 同样有专属的 `FailureAnalyzer`（`ConnectorStartFailureAnalyzer`，完整类）：

```java
class ConnectorStartFailureAnalyzer extends AbstractFailureAnalyzer<ConnectorStartFailedException> {
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConnectorStartFailedException cause) {
        return new FailureAnalysis(
                "The Tomcat connector configured to listen on port " + cause.getPort()
                        + " failed to start. The port may already be in use or the connector may be misconfigured.",
                "Verify the connector's configuration, identify and stop any process that's listening on port "
                        + cause.getPort() + ", or configure this application to listen on another port.",
                cause);
    }
}
```

**这两个异常类型的区分点**：`PortInUseException` 对应**绑定阶段的 `BindException`**（且要求异常消息包含 "in use"），触发路径是 `TomcatWebServer.start()` 里 `catch (Exception ex)` 分支；`ConnectorStartFailedException` 对应**Connector 状态检查发现已经是 `FAILED`**（不要求具体是什么原因导致的失败，可能是端口占用，也可能是 SSL 配置错误等其他导致 Connector 启动失败的原因），触发路径是 `checkThatConnectorsHaveStarted()`，在 `catch (ConnectorStartFailedException ex)` 分支里直接 `stopSilently()` 后重新抛出，**不会**走 `PortInUseException` 的判定逻辑。两者的提示文案也有差异：`ConnectorStartFailureAnalyzer` 的描述用词更谨慎（"port may already be in use **or** the connector may be misconfigured"），因为它涵盖的失败原因范围比 `PortInUseException` 更广。

**注册机制说明**：所有这些 `FailureAnalyzer`（包括 `PortInUseFailureAnalyzer`、`ConnectorStartFailureAnalyzer`）统一通过 `META-INF/spring.factories` 文件里的 `org.springframework.boot.diagnostics.FailureAnalyzer` 键注册（不是走 `@AutoConfiguration` 的 `AutoConfiguration.imports` 机制——这是两套不同的 SPI 扫描体系，`FailureAnalyzer` 沿用的是 Spring Boot 更早期的 `spring.factories` 格式）。

### 2.2 症状：TomcatStarter 捕获的应用层异常

如果启动失败不是端口问题，而是应用自己的 Bean 初始化逻辑抛的异常（比如某个 `ServletContextInitializer` 里的代码有 bug），**堆栈会呈现出一个特殊现象**：异常最初发生在 Tomcat 的容器线程里，但最终看到的堆栈却是在**主线程**（`main` 方法所在线程）——这是 TomcatStarter 异常桥接机制的效果（第 8 篇 5 节已详细讲过机制，这里复习故障排查的应用）。

**回顾捕获点**（`TomcatStarter.onStartup()`，`TomcatStarter.java:49-64`）：

```java
@Override
public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
    try {
        for (ServletContextInitializer initializer : this.initializers) {
            initializer.onStartup(servletContext);   // ★ 这里可能抛出应用层异常（Bean 初始化失败等）
        }
    }
    catch (Exception ex) {
        this.startUpException = ex;   // ★ 存起来，不往外抛
        if (logger.isErrorEnabled()) {
            logger.error("Error starting Tomcat context. Exception: " + ex.getClass().getName() + ". Message: "
                    + ex.getMessage());
        }
    }
}
```

**重新抛出点**（`TomcatWebServer.rethrowDeferredStartupExceptions()`，`TomcatWebServer.java:196-212`）：

```java
private void rethrowDeferredStartupExceptions() throws Exception {
    Container[] children = this.tomcat.getHost().findChildren();
    for (Container container : children) {
        if (container instanceof TomcatEmbeddedContext embeddedContext) {
            TomcatStarter tomcatStarter = embeddedContext.getStarter();
            if (tomcatStarter != null) {
                Exception exception = tomcatStarter.getStartUpException();
                if (exception != null) {
                    throw exception;   // ★ 在主线程重新抛出，SpringApplication.run() 能捕获到
                }
            }
        }
        if (!LifecycleState.STARTED.equals(container.getState())) {
            throw new IllegalStateException(container + " failed to start");
        }
    }
}
```

**排查要点**：看到启动失败堆栈时，先确认**堆栈里是否有 `rethrowDeferredStartupExceptions` 这个方法名**（或者更常见的，堆栈顶部直接就是应用类的异常，`rethrowDeferredStartupExceptions` 这一帧可能因为异常重新抛出的方式而不出现在堆栈里，取决于是否保留 cause）——如果日志里**先出现一条 `logger.error("Error starting Tomcat context...")`**（`TomcatStarter.java:59-61` 打的日志，日志级别是 ERROR 但**不代表进程立即退出**，因为这里没有重新抛出），**随后才是**应用最终因为异常退出的堆栈——这就是 TomcatStarter 桥接机制在起作用：第一条日志是容器线程里的"原始记录"，第二条才是主线程重抛后 `SpringApplication.run()` 捕获到并打印的完整堆栈。**两条日志对应同一个根因**，不要误判成两个独立问题。

> **面试点**：为什么启动失败时有时候会看到两条看起来相关但格式不同的错误日志？
> ——因为 Tomcat 的 `StandardContext.startInternal()`（第 1 篇 6.2 节 ⑳ 步）在**容器线程**里调用 `TomcatStarter.onStartup()`，异常被 `TomcatStarter` 捕获后**先在容器线程记一条 ERROR 日志**（`TomcatStarter.java:59-61`），然后把异常存起来不往外抛，让 Context 启动流程"体面地"走完（避免 Tomcat 自身的日志机制把这次失败当成未处理异常再打一遍冗余堆栈）；随后 `TomcatWebServer.initialize()` 在**主线程**里调用 `rethrowDeferredStartupExceptions()`，把存好的异常重新抛出，这次会被 `SpringApplication.run()` 的顶层异常处理捕获，走完整套 `FailureAnalyzer` 分析流程并打印完整堆栈。这是**同一个异常在两个不同线程里被记录了两次**，本质是一个根因。

### 2.3 症状：临时目录 / docBase 权限问题

**典型表现**：应用启动**在打出"Tomcat initialized with..."日志之前**就直接失败（核实过时序：`createTempDir` 发生在 `TomcatServletWebServerFactory.getWebServer()` 内部，`TomcatWebServer` 的构造函数拿到已经构建好的 `Tomcat` 对象后才会触发 `initialize()` 打这行日志——所以临时目录创建失败时，连这行日志都看不到），异常信息里会出现 `Unable to create tempDir` 或者更底层的 `IOException`/权限拒绝提示。

**根因**（第 1 篇 3.2 节 `Tomcat.getServer()` 懒构建 + 本篇引用的 `getWebServer()` 具体位置）：嵌入式 Tomcat 会在 `TomcatServletWebServerFactory.getWebServer()` 里创建临时目录（`createTempDir("tomcat")`/`createTempDir("tomcat-docbase")`，`TomcatServletWebServerFactory.java:201`/`245`）。核实过 `createTempDir()` 的实现（`AbstractConfigurableWebServerFactory.java:213-223`）：

```java
protected final File createTempDir(String prefix) {
    try {
        File tempDir = Files.createTempDirectory(prefix + "." + getPort() + ".").toFile();
        tempDir.deleteOnExit();
        return tempDir;
    }
    catch (IOException ex) {
        throw new WebServerException(
                "Unable to create tempDir. java.io.tmpdir is set to " + System.getProperty("java.io.tmpdir"), ex);
    }
}
```

如果容器运行环境（比如某些强隔离的容器安全策略、只读文件系统）**禁止写入系统临时目录**，或者手动配置了 `server.tomcat.basedir` 指向一个没有写权限的路径，`Files.createTempDirectory()` 会抛 `IOException`，被包装成 `WebServerException` 直接向上抛出——**这个异常发生在 `getWebServer()` 方法内部，比 `TomcatWebServer` 对象创建、比"Tomcat initialized with..."日志打印都更早**，是启动异常里发生时间点最靠前的一类。

**排查手段**：确认 `java.io.tmpdir` 系统属性指向的路径是否可写；如果显式配置了 `server.tomcat.basedir`，检查该路径的权限。生产容器化部署中，这类问题常见于**只读根文件系统**（Security Context 配置 `readOnlyRootFilesystem: true`）却忘了显式挂载一个可写的 `emptyDir` 卷给临时目录用。

---

## 3. 故障二：连接数打满

### 3.1 症状

**表现**：新连接建立缓慢或超时；**已经建立的连接处理正常**（这是与"线程池耗尽"最核心的症状区别——第 4 节会详细对比）。客户端侧看到的是 TCP 连接超时，而不是"发出请求后响应慢"。

### 3.2 源码机制回顾（第 6 篇已详述，故障视角复习）

**核心限流点**：`LimitLatch.countUpOrAwait()`（`LimitLatch.java:118-123`）：

```java
public void countUpOrAwait() throws InterruptedException {
    if (log.isTraceEnabled()) {
        log.trace("Counting up["+Thread.currentThread().getName()+"] latch="+getCount());
    }
    sync.acquireSharedInterruptibly(1);   // ★ 基于 AQS 共享锁，达到上限时阻塞
}
```

**调用点**：`AbstractEndpoint.countUpOrAwaitConnection()`（`AbstractEndpoint.java:1477`起）被 `Acceptor` 每接受一个连接就调用一次：

```java
// Acceptor.java:116
endpoint.countUpOrAwaitConnection();
```

`maxConnections` 默认 **8192**（`AbstractEndpoint.java:548`）。当当前连接数达到这个值，`Acceptor` 线程会**阻塞在 `countUpOrAwaitConnection()` 里**，不再从操作系统的 accept 队列取新连接——这意味着**操作系统层面的 TCP 三次握手仍然可能完成**（连接已经进入内核的 accept 队列），但 Tomcat 应用层不会去 `accept()` 它，客户端感知为"连接建立后长时间无响应"或者操作系统层面的 `accept` 队列（对应 `accept-count` 配置的 backlog）也满了导致连接直接被拒绝/超时。

### 3.3 定位手段

**第一步：确认连接数是否真的接近上限**（需要 `server.tomcat.mbeanregistry.enabled=true`，第 11 篇 4.4 节）：

```
Catalina:type=ThreadPool,name="http-nio-8080"
  └─ connectionCount   ← 如果这个值持续贴近 maxConnections（默认 8192），确认是连接打满
```

或者用系统工具直接看 TCP 连接数（不依赖 JMX，生产环境更常用，无需改配置）：

```bash
# 统计当前 established 连接数
ss -s
# 具体看某个端口的连接数
ss -tan state established '( dport = :8080 or sport = :8080 )' | wc -l
```

**第二步：区分"连接数打满是否合理"**——回顾第 10 篇的关键认知：**大量 keep-alive 空闲连接是正常现象**，不占线程但占连接数。如果连接数接近上限的同时，`currentThreadsBusy` 远小于 `maxThreads`，说明是**纯连接数问题**（长连接场景常见，第 10 篇场景二），不是流量真的处理不过来，只是"同时挂着的客户端太多"。

**第三步：判断是被动打满还是攻击**——检查连接来源 IP 分布（结合第 11 篇的访问日志/RemoteIpValve 还原出的真实客户端 IP）：

```bash
# 从访问日志统计来源 IP 出现次数，看是否集中在少数 IP（可能是异常客户端或攻击）
awk '{print $1}' /var/log/app/access_log.*.log | sort | uniq -c | sort -rn | head -20
```

如果连接高度集中在少数 IP，可能是**客户端连接池配置错误**（没有正确复用连接，每次请求新建连接）或者**恶意连接耗尽攻击**（简单的 DoS，大量建连不发数据）。

### 3.4 处置

| 场景 | 处置方式 |
|---|---|
| 正常业务量增长，长连接场景 | 按第 10 篇 3.3 节公式调大 `maxConnections`，评估内存开销（每连接约 100~500KB） |
| 客户端连接池配置错误 | 联系客户端团队修复连接复用逻辑，而不是无脑调大服务端上限 |
| 恶意连接攻击 | 结合网络层（防火墙/云厂商 WAF）限流，Tomcat 应用层的 `maxConnections` 只是最后一道防线，不是防 DoS 的主要手段 |
| `accept-count`（排队队列）频繁打满 | 说明 `maxConnections` 本身已经不够用，扩大排队队列只是"延迟拒绝"（第 10 篇已强调），治标不治本 |

> **面试点**：连接数打满和线程池耗尽，从 `jstack` 里怎么快速区分？
> ——连接数打满时，`jstack` 里能看到**多个 `Acceptor` 线程阻塞在 `LimitLatch.countUpOrAwait()`**（体现为 `sync.acquireSharedInterruptibly` 相关的等待栈帧）；线程池耗尽时，看到的是**大量 `http-nio-8080-exec-N` 工作线程都在跑业务代码**（`RUNNABLE` 状态，栈顶是具体的业务方法），而不是等待在某个同步原语上。这是两种完全不同的线程栈画面，一眼就能分辨。

---

## 4. 故障三：线程池耗尽

### 4.1 症状

**表现**：**已经建立的请求处理变慢**（延迟持续增长），CPU 使用率往往**不高**（因为线程在等待，不是在算），**新连接仍然能建立**（这是与"连接数打满"的核心区别——连接层面没问题，卡在业务处理层面）。

### 4.2 源码机制回顾

**核心提交点**：`ThreadPoolExecutor.execute()`（`ThreadPoolExecutor.java:1374-1394`）：

```java
public void execute(Runnable command) {
    submittedCount.incrementAndGet();
    try {
        executeInternal(command);
    } catch (RejectedExecutionException rx) {
        if (getQueue() instanceof TaskQueue) {
            final TaskQueue queue = (TaskQueue) getQueue();
            if (!queue.force(command)) {   // ★ 兜底：强制入队
                submittedCount.decrementAndGet();
                throw new RejectedExecutionException(sm.getString("threadPoolExecutor.queueFull"));
            }
        } else {
            submittedCount.decrementAndGet();
            throw rx;
        }
    }
}
```

**关键决策点**：`TaskQueue.offer()`（`TaskQueue.java:74-93`）——第 6 篇已详述的"优先扩线程，队列只是兜底"逻辑：

```java
public boolean offer(Runnable o) {
    if (parent == null) {
        return super.offer(o);
    }
    // 线程数已达上限 → 只能入队
    if (parent.getPoolSizeNoLock() == parent.getMaximumPoolSize()) {
        return super.offer(o);   // ★ 线程池耗尽时，请求开始堆积在这里
    }
    // 有空闲线程 → 入队让空闲线程处理
    if (parent.getSubmittedCount() <= parent.getPoolSizeNoLock()) {
        return super.offer(o);
    }
    // 线程数未达上限，且提交数超过当前线程数 → 拒绝入队，逼迫 JDK ThreadPoolExecutor 创建新线程
    if (parent.getPoolSizeNoLock() < parent.getMaximumPoolSize()) {
        return false;
    }
    return super.offer(o);
}
```

**队列无界的后果**：`maxQueueCapacity` 默认无界（第 10 篇已述），当 `parent.getPoolSizeNoLock() == parent.getMaximumPoolSize()`（线程数已经达到 `maxThreads`）时，所有新请求全部走 `super.offer(o)` 入队——**队列会无限增长，表现为延迟持续上升而不是直接报错**。这正是"线程池耗尽"这类故障最典型也最隐蔽的地方：**服务表面上还在"正常"响应（没有直接拒绝），只是越来越慢**，很容易被忽视直到雪崩。

### 4.3 定位手段

**第一步：用 jstack 确认线程池状态**：

```bash
jstack <pid> > /tmp/threaddump.txt

# 统计 http-nio 线程总数（应该接近 maxThreads 才是耗尽）
grep -c "http-nio-8080-exec" /tmp/threaddump.txt

# 看这些线程具体卡在哪里（业务代码 or 下游 IO 等待）
grep -A 20 "http-nio-8080-exec-1\"" /tmp/threaddump.txt
```

如果发现**大量 `http-nio` 线程都停在同一段业务代码或同一个下游调用**（比如都卡在某个 Feign 调用、某个数据库查询），这不是"线程池配置不够大"的问题，而是**下游变慢拖垮了整个线程池**——这种情况盲目调大 `maxThreads` 只是延缓症状，下游继续变慢照样耗尽，真正需要做的是给下游调用加**超时 + 熔断**（比如 Resilience4j/Sentinel），阻止下游的慢直接拖累到 Tomcat 线程池。

**第二步：结合 Actuator 指标确认线程池当前状态**：

```bash
curl -s localhost:8080/actuator/metrics/tomcat.threads.busy | jq
curl -s localhost:8080/actuator/metrics/tomcat.threads.config.max | jq
```

**前提条件（容易被忽略的一个坑，第 11 篇 4.4 节已详细核实）**：这两个指标**必须**在 `server.tomcat.mbeanregistry.enabled=true` 的情况下才有数据——Micrometer 的 `tomcat.threads.*` 系列指标底层是通过 JMX 查询 `Catalina:type=ThreadPool,...` 这个 MBean 拿到的，而这个 MBean 默认（`mbeanregistry.enabled` 默认 `false`）根本不会注册。如果生产环境没有提前打开这个配置，上面两条 `curl` 会返回空指标或者查询不到该 metric name——**这个坑最好在故障发生前就确认好**，不要等到线程池已经耗尽了才发现监控数据是空的。

如果 `tomcat.threads.busy` 持续贴近 `tomcat.threads.config.max`（通常长时间维持在 90%+ 以上），确认是线程池即将或已经耗尽。

**第三步：查看队列积压情况**——Tomcat 的 `TaskQueue` 没有直接暴露"当前队列长度"的标准 Actuator 指标，但可以通过 JMX 的 `ThreadPool` MBean 观察 `currentThreadsBusy` 是否持续等于 `maxThreads`（意味着队列在积压，只是 JMX 看不到队列深度本身），或者更直接地——**观察请求排队导致的响应时间分布**（访问日志的 `%D`，如果 P99 显著大于 P50，且这个差值持续扩大，是队列积压的典型信号）。

### 4.4 处置

| 根因 | 处置方式 |
|---|---|
| 业务量增长超过原有 `maxThreads` 评估 | 按第 10 篇公式重新评估并调大 `maxThreads`，同时评估机器 CPU/内存余量 |
| 下游服务变慢拖累线程池 | **加超时+熔断**，防止个别下游故障传播到整个线程池；不要只调大线程数 |
| CPU 密集型代码误配了过大的 `maxThreads` | 按第 10 篇的经验公式回调到接近核数，过度并发反而因为上下文切换更慢 |
| 突发流量导致瞬时耗尽 | 结合 `min-spare` 预热 + 限流（网关层限流优先于容器层被动排队） |

> **面试点**：为什么线程池耗尽不会像连接数打满那样直接拒绝新请求？
> ——因为连接层和线程池层是两个独立的资源池（第 10 篇 1.2 节的核心认知）。连接数打满时，`Acceptor` 直接阻塞不再 accept 新连接，新连接**在网络层面**就建立不起来；线程池耗尽时，**连接已经建立**（`NioEndpoint` 的 `Poller` 层面能正常接收数据），只是提交给 `ThreadPoolExecutor` 处理时因为线程数已达上限，被塞进了**无界的 `TaskQueue`**——请求"进来了"，只是排队等着被处理，所以表现为延迟增长而不是直接被拒绝。这也是为什么"线程池耗尽"比"连接打满"更危险——它不会主动示警（不报错），只会悄悄变慢直到整体雪崩。

---

## 5. 故障四：类加载器内存泄漏

### 5.1 症状

**表现**：应用反复经历"热部署/reload"（Standalone 场景更常见，但嵌入式场景下的**开发环境热重启**、或者**同 JVM 里创建多个 Spring 子上下文**——比如第 11 篇 3.3 节的管理端口独立 `ApplicationContext`——某些情况下也会涉及类似的类加载器管理问题）后，老年代内存持续增长不释放，最终 `OutOfMemoryError: Metaspace` 或普通堆 OOM。

**核心机制**：每次 Web 应用重新加载，Tomcat 都会创建一个**全新的 `WebappClassLoader`** 实例（第 7 篇已详述类加载隔离机制）。如果旧的 `WebappClassLoader` 因为某些引用没有被正确清理，导致它本该被回收却一直被外部持有——**这个类加载器加载的所有类、所有静态字段引用的对象，全部无法被 GC 回收**，这就是经典的"类加载器泄漏"。

### 5.2 源码机制：Tomcat 的泄漏诊断与部分清理

**触发点**：`StandardContext.stopInternal()`（`StandardContext.java:4696-4703`）——Context 停止时会停止其 `Loader`：

```java
Loader loader = getLoader();
if (loader instanceof Lifecycle) {
    ClassLoader classLoader = loader.getClassLoader();
    ((Lifecycle) loader).stop();   // ★ 触发 WebappLoader.stopInternal()
    if (classLoader != null) {
        InstanceManagerBindings.unbind(classLoader);
    }
}
```

**`WebappLoader.stopInternal()`**（`WebappLoader.java:359-396`）直接调用类加载器自身的清理：

```java
protected void stopInternal() throws LifecycleException {
    ...
    if (classLoader != null) {
        try {
            classLoader.stop();   // ★ WebappClassLoaderBase.stop()
        } finally {
            classLoader.destroy();
        }
        ...
    }
    classLoader = null;
}
```

**`WebappClassLoaderBase.clearReferences()`**（`WebappClassLoaderBase.java:1574-1626`）是核心的清理逻辑，一次性处理多种泄漏来源：

```java
protected void clearReferences() {
    ...
    // 1. 注销遗留的 JDBC 驱动（第三方驱动常见的静态注册表泄漏）
    clearReferencesJdbc();

    // 2. 停止应用自己启动但没有停止的线程（★ 最常见的泄漏来源之一）
    clearReferencesThreads();

    // 3. 清理序列化缓存中的类引用
    if (clearReferencesObjectStreamClassCaches && !JreCompat.isGraalAvailable()) {
        clearReferencesObjectStreamClassCaches();
    }

    // 4. 检测由本类加载器加载的类触发的 ThreadLocal 泄漏（★ 另一大常见来源）
    if (clearReferencesThreadLocals && !JreCompat.isGraalAvailable()) {
        checkThreadLocalsForLeaks();
    }

    // 5. 清理 RMI Target 表
    if (clearReferencesRmiTargets) {
        clearReferencesRmiTargets();
    }

    // 6. 清理 IntrospectionUtils / commons-logging / java.beans.Introspector 的类引用缓存
    IntrospectionUtils.clear();
    if (clearReferencesLogFactoryRelease) {
        LogFactory.release(this);
    }
    java.beans.Introspector.flushCaches();

    // 7. 清理自定义 URLStreamHandler
    TomcatURLStreamHandlerFactory.release(this);
}
```

**这段代码的意义（核实源码后需要澄清一个常见误解）**：Tomcat 内置了一套**检测**常见内存泄漏源的机制，但**不代表所有检测到的问题都会被自动清理**——两个最关键的子方法在"检测到问题后具体怎么处理"上完全不同：

- **`clearReferencesThreads()`**（`WebappClassLoaderBase.java:1678`起）：默认只会**记录警告日志**（`log.warn(...stackTrace...)`，打出该线程的完整堆栈方便定位），**是否真正尝试停止线程**取决于 `clearReferencesStopThreads` 这个开关——它的**默认值是 `false`**（`WebappClassLoaderBase.java:332`）！也就是说**默认情况下 Tomcat 只是警告"这个线程可能造成泄漏"，并不会帮你停掉它**。原因也很直接：**主动 `Thread.stop()` 类操作本身是危险的**（可能导致共享资源处于不一致状态），Tomcat 默认选择"告警但不干预"，需要显式配置 `clearReferencesStopThreads=true` 才会尝试停止。
- **`checkThreadLocalsForLeaks()`**（`WebappClassLoaderBase.java:1872-1916`）：**只做检测和记录 `log.error`，代码里完全没有清除泄漏条目的动作**（`expungeStaleEntriesMethod.invoke(...)` 调用的是 JDK `ThreadLocalMap` 自带的"清理已经被 GC 掉的弱引用条目"逻辑，这是清理**已经失效**的旧条目，不是清理"仍然存活但导致泄漏"的条目）。它的价值纯粹是**诊断**——通过反射遍历所有线程的 `ThreadLocalMap`，把"key 或 value 由当前 WebappClassLoader 加载"的条目找出来打日志报警，**日志本身就是最重要的产出**（能看到具体是哪个类、哪个线程持有了泄漏引用），但不会替你清理。

所以更准确的说法是：Tomcat 提供的是一套**诊断+部分清理**的组合机制——`clearReferencesJdbc()`/`clearReferencesRmiTargets()`/`clearReferencesObjectStreamClassCaches()` 等确实会执行清理动作；但线程和 `ThreadLocal` 这两个最常见也最棘手的泄漏来源，**默认策略是"报警但不动手"**，需要运维/开发人员根据警告日志自行判断和修复代码，或者显式打开 `clearReferencesStopThreads` 承担"强制停止线程"的风险。这个设计上的取舍本身就是一个很好的面试点。

**`checkThreadLocalsForLeaks()` 的检测思路**（`WebappClassLoaderBase.java:1872`起，反射遍历所有线程的 `ThreadLocalMap`）——这是最值得理解的一部分，因为它揭示了 `ThreadLocal` 泄漏的本质：`ThreadLocal` 本身的值如果是**由当前 WebappClassLoader 加载的类的实例**（比如业务代码定义的一个 POJO 存进了某个 `ThreadLocal`），而这个线程本身是**容器级别的公共线程**（比如线程池里的工作线程，生命周期跨越多次请求、甚至跨越应用本身），那么即使这次 Web 应用被卸载了，`ThreadLocal` 里挂着的引用依然存在于这些长生命周期线程的 `ThreadLocalMap` 里，形成了"应用已经卸载，但它的对象还被 JVM 里其他地方间接持有"的经典泄漏模式。

### 5.3 定位手段

**第一步：确认是否真的是类加载器泄漏**——最直接的信号是**反复 reload/redeploy 后，Metaspace 或老年代持续增长，且增长量与 reload 次数大致成正比**：

```bash
# 观察 Metaspace 使用趋势
jstat -gc <pid> 5s   # 每 5 秒采样一次，看 MC/MU（Metaspace 容量/使用量）是否单调增长
```

**第二步：用 heap dump 找出"应该被回收但没被回收"的类加载器**：

```bash
jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>
```

用 Eclipse MAT（Memory Analyzer Tool）或 VisualVM 打开 dump，查询：

```
按 Class Loader 分组统计（MAT 的 "Class Loader Explorer"）
→ 如果看到多个 WebappClassLoader 实例同时存在（正常情况应该只有 1 个，或者 reload 过程中短暂存在 2 个）
→ 找到"多出来的"那些实例的 GC Root 路径（谁在引用它，导致它无法被回收）
```

MAT 的 "Leak Suspects" 报告通常能直接定位到具体的持有链——常见模式包括：**某个静态 Map/缓存持有了业务对象的引用**（该业务对象由旧的 WebappClassLoader 加载）、**线程池里的 `ThreadLocal` 没清理**（对应上面 `checkThreadLocalsForLeaks` 试图检测但可能检测不全的场景）、**定时任务/后台线程持有了旧 Context 相关的引用没有停止**。

**第三步（嵌入式场景的特殊性）**：嵌入式 Spring Boot 应用通常是**单进程单应用**，不像 Standalone Tomcat 那样频繁热部署多个不同的 Web 应用——所以这类问题在嵌入式场景下**出现频率远低于传统 Standalone 部署**，但并非完全不会遇到：常见触发场景是**测试环境反复通过 DevTools 热重启**、或者**应用内部动态创建了多个短生命周期的 `ApplicationContext`**（比如动态加载插件模块，每个插件对应一个独立类加载器，卸载插件时如果没清理干净会有同样的问题）。

### 5.4 处置

| 泄漏来源 | 处置方式 |
|---|---|
| 应用自己启动的线程未停止 | 应用 `@PreDestroy`/`ContextClosedEvent` 里显式关闭自定义线程池/后台线程 |
| `ThreadLocal` 未清理 | 使用 `ThreadLocal` 后务必在 `finally` 里调 `remove()`，尤其在线程池复用的线程里 |
| 第三方库的静态缓存持有类实例 | 排查依赖库版本，确认是否有已知的类加载器泄漏 issue（部分老版本 JDBC 驱动/日志框架有过这类问题） |
| 静态字段直接持有业务对象 | 代码层面避免把当前 Web 应用的对象塞进 JVM 全局单例；确实需要跨应用共享的数据应该放在应用外部（缓存/DB） |

> **面试点**：为什么 `ThreadLocal` 泄漏在嵌入式场景下影响相对较小？
> ——因为嵌入式场景通常**没有"热部署/reload 同一个 Web 应用"这个动作**（每次发版都是重启整个 JVM 进程，进程重启后所有内存自然清零）。传统 Standalone Tomcat 支持"不重启 JVM，只重新部署某个 war 包"，这种场景下旧的 `WebappClassLoader` 才有"应该被回收却没被回收，持续累积占用内存"的风险。但**不代表嵌入式场景完全没有类加载器泄漏问题**——只要应用内部存在动态创建/卸载类加载器的逻辑（插件化架构、动态字节码生成配合独立类加载器隔离），同样的泄漏模式依然会发生，只是没有"Web 应用整体 reload"这个最典型的诱因。

---

## 6. 故障五：慢请求定位

### 6.1 症状

**表现**：整体吞吐正常（没有明显的连接/线程池瓶颈），但**部分请求响应时间异常**——可能是全局性的（所有请求都慢一点）也可能是局部性的（只有某类接口慢）。这是最考验"定量分析"能力的一类故障，因为往往不是"坏了"，而是"慢了"，需要精确的耗时数据支撑判断。

### 6.2 定位手段：访问日志的 %D

**第一步：从访问日志统计耗时分布**，找出真正的慢请求特征（是否集中在特定 URI、特定时间段、特定状态码）：

```bash
# 提取 %D（微秒）和 URI，按耗时排序，找最慢的请求
awk '{print $NF, $7}' /var/log/app/access_log.log | sort -rn | head -50

# 统计某个 URI 的耗时分布（转换成毫秒方便阅读）
grep "/api/order" /var/log/app/access_log.log | awk '{print $NF/1000}' | sort -n | \
  awk '{a[NR]=$1} END {print "P50:", a[int(NR*0.5)], "P95:", a[int(NR*0.95)], "P99:", a[int(NR*0.99)]}'
```

**注意**（第 11 篇 4.3 节已核实）：`%D` 的单位是**微秒**（`AbstractAccessLogValve.java:1806` → `ElapsedTimeElement(true, false)` → `Style.MICROSECONDS`），做数据分析时务必先除以 1000 转换成毫秒，否则统计出的数字会离谱地大，容易误判。

**第二步：区分"慢在 Tomcat 侧排队"还是"慢在业务逻辑本身"**——`%D` 统计的是 `CoyoteAdapter.service()` 整个处理过程的耗时（第 1 篇桥接点），**包含了线程池排队等待的时间**（如果线程池已经繁忙，请求提交给 `ThreadPoolExecutor` 后在 `TaskQueue` 里排队的这段时间也会被计入 `%D`）。所以：

- 如果 `%D` 分布**整体性**变慢（所有接口都慢，不管业务逻辑多简单），优先怀疑**线程池排队**（第 4 节）或**下游依赖变慢**，而不是这个接口本身的业务逻辑变复杂了
- 如果只有**特定几个接口**慢，其他接口正常，大概率是这些接口自身的业务逻辑（数据库慢查询、下游调用超时）

**第三步：用 jstack 直接抓运行中的慢请求线程栈**——如果是持续性的慢（不是偶发），可以直接连续多次 dump 线程栈，观察同一个业务线程是否长时间停留在同一处：

```bash
for i in 1 2 3; do jstack <pid> > /tmp/stack_$i.txt; sleep 2; done
# 对比三次 dump 里 http-nio 线程的栈，如果同一个线程连续几次都停在同一行代码
# （比如某个 JDBC 查询、某个 Feign 调用），基本可以确认卡点
```

### 6.3 常见慢请求根因分类

| 根因类型 | 特征 | 定位方式 |
|---|---|---|
| 数据库慢查询 | 线程栈停留在 JDBC 相关方法（`java.sql.*`/连接池获取连接） | 数据库慢查询日志 + `EXPLAIN` 执行计划 |
| 下游服务调用慢/超时 | 线程栈停留在 HTTP 客户端方法（`socketRead0` 等） | 结合链路追踪（如接入了 SkyWalking/Zipkin），查下游服务本身的处理耗时 |
| GC 停顿 | 全局性的、周期性的慢（不是持续性） | GC 日志（`-Xlog:gc*`），观察 STW 停顿时长与业务慢请求时间点是否重合 |
| 线程池排队（本篇第 4 节） | `%D` 整体升高，jstack 里线程池线程都在跑业务但数量已达上限 | 结合第 4 节的线程池指标 |
| 序列化/反序列化耗时（大对象） | 特定接口慢，且响应体较大 | 抓取具体接口的 CPU profile（`async-profiler` 等），确认耗时在序列化框架内部 |

**GC 停顿这个根因容易被忽略**——如果慢请求呈现明显的**周期性**（比如每隔几分钟出现一波慢请求，且时间点接近），而不是持续性的慢，要优先排查 GC 日志。GC 的 Stop-The-World 阶段会让所有业务线程一起停顿，这种"全局同时变慢"的画面与"线程池排队变慢"（渐进式增长）在时序上有明显区别——GC 导致的慢是脉冲式的，线程池排队导致的慢是渐进式爬升的。

> **面试点**：`%D` 里包含哪些阶段的耗时？只是业务代码执行时间吗？
> ——不是。`%D`（`ElapsedTimeElement`）统计的是从 `CoyoteAdapter.service()` 开始处理这个请求，到响应写完为止的**全部耗时**——这包括：请求提交到线程池后在 `TaskQueue` 里的排队等待时间（如果线程池繁忙）、Filter 链执行时间、`Servlet.service()` 业务代码执行时间（含所有下游调用的阻塞等待）、响应写出时间。**排查慢请求时不能想当然地认为 `%D` 就是"业务代码跑了多久"**——如果线程池本身在排队，这段等待时间会被"错误地记在"这次请求的账上，看起来像是业务代码变慢了，实际是线程池资源不够。这也是为什么第 4 节强调要先排除线程池耗尽，再去看具体业务代码的耗时。

---

## 7. 本篇小结与全系列收尾

### 7.1 全系列地图（完整收官）

```
第 0 篇：Servlet 规范全景（契约）
第 1 篇：容器树 + Lifecycle（骨架）
第 2 篇：Pipeline-Valve（请求处理链）
第 3 篇：Mapper（路由）
第 4 篇：StandardWrapper（Servlet 生命周期）
第 5 篇：Connector + CoyoteAdapter（两层桥接）
第 6 篇：NIO 线程模型（网络心脏）
第 7 篇：类加载机制（隔离与覆盖）
第 8 篇：Spring Boot 集成 + 生产定制（汇合）
第 10 篇：性能调优实战（事前决策）
第 11 篇：安全加固与运维闭环（事中防护）
第 12 篇（本篇）：故障排查实战（事后定位）
```

**实战三部曲的关系**：第 10 篇解决"该怎么配"（决策），第 11 篇解决"配好之后怎么保证安全和能被正确运维"（防护/闭环），第 12 篇解决"万一出问题了怎么第一时间定位"（应急）。三篇覆盖了生产环境从上线前到运行中再到出故障的完整时间线，共同建立在第 0~8 篇的源码机制之上——**每一个故障排查手段，最终都能回扣到某个具体的源码机制**，这正是本系列"源码导向"的核心价值：不是死记硬背"遇到 XX 问题怎么办"，而是理解**为什么会有这个问题、源码层面在做什么、所以该往哪个方向排查**。

### 7.2 面试要点速查

1. **启动失败排查优先级**：先看异常类型（`PortInUseException` 等有专门 `FailureAnalyzer` 的类型，能直接给出结构化提示）；`TomcatStarter` 桥接机制会导致同一根因在日志里出现两次（容器线程记录 ERROR 日志 + 主线程重新抛出的完整堆栈），不要误判成两个问题
2. **`PortInUseException` 的判定逻辑**：沿着异常 cause 链查找 `BindException`，且消息必须包含 "in use" 关键字——不是所有 `BindException` 都是端口占用
3. **端口相关的启动失败有两种不同的异常类型**：`PortInUseException` 对应**初次绑定阶段**的 `BindException`；`ConnectorStartFailedException` 对应**两阶段启动的第二阶段**（`addPreviouslyRemovedConnectors()` 之后）Connector 状态检查发现 `FAILED`——两者各有专属 `FailureAnalyzer`，都通过 `spring.factories` 注册（不是 `@AutoConfiguration` 机制）
4. **连接数打满 vs 线程池耗尽的本质区别**：前者卡在网络层（`Acceptor` 阻塞在 `LimitLatch`，新连接建立不起来但已有连接正常）；后者卡在应用层（连接能建立，但请求提交给线程池后在无界 `TaskQueue` 里排队，表现为延迟渐进增长）
5. **`jstack` 快速区分两者**：连接打满看到 `Acceptor` 线程阻塞在 `acquireSharedInterruptibly`；线程池耗尽看到大量 `http-nio-exec` 线程都在跑业务代码（`RUNNABLE`）
6. **线程池耗尽最危险的地方**：因为队列默认无界，不会直接报错拒绝，只会悄悄变慢直到雪崩——必须靠主动监控（`tomcat.threads.busy` 贴近 `config.max`）提前发现，不能等用户投诉
7. **下游变慢拖累线程池时**，正确处置是加超时+熔断，不是无脑调大 `maxThreads`（治标不治本，下游继续变慢照样耗尽）
8. **Tomcat 类加载器泄漏机制是"诊断+部分清理"，不是全自动清理**：`clearReferencesJdbc`/`clearReferencesRmiTargets` 等确实执行清理；但 `clearReferencesThreads` 默认只警告不停止线程（`clearReferencesStopThreads` 默认 `false`），`checkThreadLocalsForLeaks` 只检测记录日志、从不清除泄漏的 `ThreadLocal` 条目——默认策略是"报警但不动手"，因为强制停止线程本身有风险
9. **嵌入式场景下类加载器泄漏影响较小**：因为没有"同 JVM 内热部署同一个 Web 应用"这个最典型的诱因，但插件化/动态类加载场景仍可能触发
10. **`%D` 的单位是微秒**，不是毫秒，且统计的是全链路耗时（含线程池排队时间），不能直接等同于"业务代码执行时间"
11. **GC 停顿导致的慢请求呈脉冲式**（周期性、全局同时发生），与线程池排队导致的慢（渐进式爬升）在时序特征上有明显区别，是快速甄别根因的关键信号
12. **排查前必须确认 `mbeanregistry.enabled=true`**（第 11 篇 4.4 节已核实）：`/actuator/metrics/tomcat.threads.*` 系列指标底层通过 JMX 查询 `Catalina:type=ThreadPool` MBean，这个 MBean 默认不注册——生产环境如果没提前打开这个配置，故障发生时会发现监控数据是空的，这个坑必须在故障排查体系搭建阶段就避开，不能等真出问题才发现
