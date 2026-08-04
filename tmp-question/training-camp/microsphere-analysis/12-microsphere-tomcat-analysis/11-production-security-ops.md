# 第 11 篇：生产安全与运维实战

> 第 10 篇解决了"性能怎么调"；本篇解决**上线前必须做完的两件事**：
> **安全加固**（别把内部信息暴露给攻击者）和**运维闭环**（服务要能被编排系统正确管理）。
>
> 这两件事很容易被忽视——因为不做也能跑起来，但生产事故往往就出在这些"顺手加一下就好"的配置上：
> 版本号泄露帮攻击者精确定位 CVE、反代场景下 IP 识别错误导致限流失效、
> K8s 滚动更新时没有优雅关闭导致请求丢失。
>
> **源码范围**：
> - `TomcatWebServerFactoryCustomizer` / `TomcatServletWebServerFactoryCustomizer`（第 8 篇已见）
> - `ErrorReportValve` / `RemoteIpValve` / `HealthCheckValve`（`org.apache.catalina.valves`）
> - `SslConnectorCustomizer` / `GracefulShutdown`（Spring Boot 嵌入式包）
> - `CoyoteAdapter`（X-Powered-By 写入点）
>
> **本篇定位**：实战篇（场景驱动），衔接第 8 篇配置映射表 + 第 10 篇性能调优。
> 结构延续"场景 → 决策 → 源码依据"。

---

## 目录

1. [安全加固：把内部信息藏起来](#1-安全加固把内部信息藏起来)
2. [反向代理场景：正确识别客户端身份](#2-反向代理场景正确识别客户端身份)
3. [自定义 Customizer 实战：三个完整可跑的例子](#3-自定义-customizer-实战三个完整可跑的例子)
4. [运维闭环：让编排系统看懂你的服务](#4-运维闭环让编排系统看懂你的服务)
5. [本篇小结与面试要点](#5-本篇小结与面试要点)

---

## 1. 安全加固：把内部信息藏起来

### 1.1 为什么要隐藏版本信息？

**攻击者的第一步是指纹识别**：知道你用的是 Tomcat 10.1.34 + 具体的 JDK 版本，就能去 CVE 数据库精确定位已知漏洞，省掉大量盲测时间。Tomcat 默认会通过三个渠道主动暴露这些信息：

1. **`Server` 响应头**：`Server: Apache Tomcat/10.1.34`
2. **`X-Powered-By` 响应头**：`X-Powered-By: Servlet/6.0 JSP/3.1 (Apache Tomcat/10.1.34...)`
3. **错误页脚注**：500 页面底部的 `<h3>Apache Tomcat/10.1.34</h3>`

这三者**互相独立**，必须分别关闭。这是本节的核心——很多人只关了一个就以为万事大吉。

### 1.2 渠道一：Server 响应头

**源码走读**：Spring Boot 定制入口在 `TomcatServletWebServerFactory.customizeConnector()`：

```java
// TomcatServletWebServerFactory.java:343-348
protected void customizeConnector(Connector connector) {
    int port = Math.max(getPort(), 0);
    connector.setPort(port);
    if (StringUtils.hasText(getServerHeader())) {
        connector.setProperty("server", getServerHeader());   // ★ 关键委托
    }
    ...
}
```

`getServerHeader()` 来自 `AbstractConfigurableWebServerFactory`（`AbstractConfigurableWebServerFactory.java:170-176`），由 `server.server-header` 配置项经 `ServletWebServerFactoryCustomizer.customize()` 注入：

```java
// ServletWebServerFactoryCustomizer.java:91
map.from(this.serverProperties::getServerHeader).to(factory::setServerHeader);
```

**`connector.setProperty("server", ...)` 到底做了什么**（第 5 篇 3.3 属性委托链的应用）：

```java
// Connector.java:314-319
public boolean setProperty(String name, String value) {
    ...
    return IntrospectionUtils.setProperty(protocolHandler, name, value);
}
```

反射调用到 `AbstractHttp11Protocol.setServer(String)`（`AbstractHttp11Protocol.java:457-458`），最终在响应阶段被 `Http11Processor` 读取并写入响应头：

```java
// Http11Processor.java:1046-1055
// Add server header
String server = protocol.getServer();
if (server == null) {
    if (protocol.getServerRemoveAppProvidedValues()) {
        headers.removeHeader("server");
    }
} else {
    // server always overrides anything the app might set
    headers.setValue("Server").setString(server);   // ★ 强制覆盖
}
```

**关键细节**：`server` 非空时会**强制覆盖**应用代码自己设置的 `Server` 头（"server always overrides anything the app might set"）——即使你的业务代码手动 `response.setHeader("Server", "xxx")`，Tomcat 层还会再覆盖一次。这是**协议层兜底**的设计，保证配置的一致性优先于业务代码的随意设置。

**两种做法对比**：

| 配置 | 效果 | 底层机制 |
|---|---|---|
| `server.server-header=` (留空/不配置) | 保留 Tomcat 默认 `Server` 头 | `getServerHeader()` 返回 null，`server` 字段为 null，`getServerRemoveAppProvidedValues()` 默认 false，不做任何处理 |
| `server.server-header=MyServer` | `Server` 头替换为自定义值 | `connector.setProperty("server", "MyServer")` |
| （无此配置项）完全移除 | 需要自定义 `TomcatConnectorCustomizer` 调 `setServerRemoveAppProvidedValues(true)` 且不设 `server` | 见下文 |

> **面试点**：`server.server-header=""` （空字符串）能移除 Server 头吗？
> ——不能，`StringUtils.hasText()` 判断空字符串为无文本，`customizeConnector` 里的 `if` 分支不执行，`server` 属性保持未设置，Tomcat **仍然发送默认的 `Apache Tomcat/xxx` 头**。真正要完全移除需要额外配置 `AbstractHttp11Protocol.setServerRemoveAppProvidedValues(true)`（这个 Spring Boot 没有暴露配置项，需要自定义 Customizer，见第 3 节示例）。

### 1.3 渠道二：X-Powered-By 响应头

**源码位置**：`CoyoteAdapter.service()` 入口处（第 1 篇桥接点）：

```java
// CoyoteAdapter.java:327-329
if (connector.getXpoweredBy()) {
    response.addHeader("X-Powered-By", POWERED_BY);
}
```

`getXpoweredBy()` 默认 **false**（Tomcat 裸机默认关闭），但需要注意：这个属性和 `Server` 头是**两条完全独立的开关**——`Connector.setXpoweredBy(boolean)` 走的是普通 setter，不经过 `setProperty` 反射委托链。Spring Boot **没有为它提供专门的配置项**，如果你在别处见到过 `X-Powered-By: Servlet/... JSP/... (Apache Tomcat/...)` 这种格式的响应头，通常是因为某些 Standalone 部署或旧版本显式开启了它；嵌入式 Spring Boot 默认不会主动打开，但也没有主动"加固"这一项——**它本来就是关的**，无需额外配置。

> **面试点**：为什么 X-Powered-By 和 Server 头要分开处理？——因为它们代表不同的暴露信息：`Server` 头暴露的是**容器**（Tomcat 版本），`X-Powered-By` 暴露的是**技术栈全貌**（Servlet 规范版本 + JSP 版本 + 容器版本拼接串）。历史上 `X-Powered-By` 常被 PHP/ASP.NET/Java 容器滥用来做技术炫耀，安全社区早早就建议默认关闭——Tomcat 索性把默认值设为 false。

### 1.4 渠道三：错误页脚注（ServerInfo）

**`ServerInfo` 的数据来源**：`org/apache/catalina/util/ServerInfo.properties`（构建时用 Maven/Ant 替换占位符）：

```properties
server.info=Apache Tomcat/@VERSION@
server.number=@VERSION_NUMBER@
server.built=@VERSION_BUILT@
server.built.iso=@VERSION_BUILT_ISO@
```

**消费点**：`ErrorReportValve.report()`（`ErrorReportValve.java:186-346`）——生成 500/404 等错误页 HTML 时：

```java
// ErrorReportValve.java:320-322
if (isShowServerInfo()) {
    sb.append("<h3>").append(ServerInfo.getServerInfo()).append("</h3>");   // ★ 版本号写入错误页
}
```

`showServerInfo` 字段默认 **true**（`ErrorReportValve.java:65`）——**Tomcat 裸机默认在每个错误页脚注暴露版本号**，这是最容易被忽视的一条泄露渠道（前两条大家都知道要关响应头，这条藏在错误页里）。

**Spring Boot 的加固**：`TomcatWebServerFactoryCustomizer.customizeErrorReportValve()`（`TomcatWebServerFactoryCustomizer.java:364-373`）：

```java
private void customizeErrorReportValve(ErrorProperties error, ConfigurableTomcatWebServerFactory factory) {
    if (error.getIncludeStacktrace() == IncludeAttribute.NEVER) {
        factory.addContextCustomizers((context) -> {
            ErrorReportValve valve = new ErrorReportValve();
            valve.setShowServerInfo(false);   // ★ 关闭版本号
            valve.setShowReport(false);       // ★ 关闭详细错误报告（异常类型/堆栈/root cause）
            context.getParent().getPipeline().addValve(valve);   // ★ 加到 Host 阀门链
        });
    }
}
```

**触发条件是 `server.error.include-stacktrace=NEVER`**（`ErrorProperties.java:46`，这也是**默认值**）——即**默认情况下**，只要没有手动把 `include-stacktrace` 调成 `ALWAYS`/`ON_TRACE_PARAM`，Spring Boot 就会自动加一个"精简版" `ErrorReportValve`，同时关掉 `showServerInfo` 和 `showReport` 两项。

**关键设计细节（核实源码后纠正）**：这个新 Valve 是加到 **`context.getParent()` 即 Host 层**，而不是 Context 层。为什么要这么做，且为什么不会造成"两个 ErrorReportValve 同时生效"的混乱？

这里的关键在于**时序**。`StandardHost` 有一段自己的默认逻辑（`StandardHost.startInternal()`，`StandardHost.java:747-770`）：

```java
protected void startInternal() throws LifecycleException {
    String errorValve = getErrorReportValveClass();   // 默认值："org.apache.catalina.valves.ErrorReportValve"
    if ((errorValve != null) && (!errorValve.equals(""))) {
        boolean found = false;
        Valve[] valves = getPipeline().getValves();
        for (Valve valve : valves) {
            if (errorValve.equals(valve.getClass().getName())) {   // ★ 按类名匹配
                found = true;
                break;
            }
        }
        if (!found) {
            Valve valve = ... new ErrorReportValve() ...;
            getPipeline().addValve(valve);   // 只有没找到同类名的 Valve 才会新增默认实例
        }
    }
    ...
}
```

即 **Host 启动时会检查 Pipeline 里是否已经存在一个类名为 `ErrorReportValve` 的 Valve，找到了就不再重复添加默认实例**。而 Spring Boot 的 `customizeErrorReportValve` 通过 `TomcatContextCustomizer` 在 `configureContext()` 阶段执行（`prepareContext()` 里先 `host.addChild(context)` 再 `configureContext(context, ...)`，`TomcatServletWebServerFactory.java:268-269`）——**这个时间点远早于 `tomcat.start()`**（第 1 篇：构建阶段完成后所有组件仍是 NEW 状态，start() 才真正启动）。

所以完整时序是：**Spring Boot 的 Customizer 先把自己配置好的 `ErrorReportValve`（`showServerInfo=false`/`showReport=false`）加进 Host 的 Pipeline → 之后 Host 真正 `start()` 时检查发现"已经有一个同类名的 Valve" → 跳过默认添加逻辑**。最终 Pipeline 里**只有一个** `ErrorReportValve` 实例，就是 Spring Boot 配置过的那个——不是"双重挂载后收敛"，而是**抢先占位，让 Tomcat 的默认添加逻辑天然失效**。这是一个设计上很巧妙的"按类名去重"机制，Spring Boot 恰好利用了这一点，不需要用任何"覆盖/替换已有 Valve"的显式操作。

> **面试点**：`showReport=false` 和 `showServerInfo=false` 分别关闭了什么？
> ——`showServerInfo` 只影响页面底部的版本号那一行；`showReport` 影响的范围更大：类型（异常报告/状态报告）、消息、描述、**完整堆栈**、root cause 链（最多 10 层）全部不显示，只留一个裸的状态码标题。生产环境两者都应该关闭——`showReport=false` 防止内部实现细节（包名、类名、调用链）泄露给客户端，这本身就是攻击者做代码结构侧写的信息源。

### 1.5 TLS 配置

**Spring Boot 侧入口**：`TomcatServletWebServerFactory.customizeSsl()`（`TomcatServletWebServerFactory.java:382-389`）：

```java
private void customizeSsl(Connector connector) {
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(logger, connector, getSsl().getClientAuth());
    customizer.customize(getSslBundle(), getServerNameSslBundles());
    addBundleUpdateHandler(null, getSsl().getBundle(), customizer);
    getSsl().getServerNameBundles()
        .forEach((serverNameSslBundle) -> addBundleUpdateHandler(serverNameSslBundle.serverName(),
                serverNameSslBundle.bundle(), customizer));
}
```

触发条件在 `customizeConnector()` 里：`if (Ssl.isEnabled(getSsl())) { customizeSsl(connector); }`——即配置了 `server.ssl.enabled=true`（或提供了 keystore）才会走这条路径。

**`SslConnectorCustomizer` 做的事**（完整类，`SslConnectorCustomizer.java`）：

```java
void customize(SslBundle sslBundle, Map<String, SslBundle> serverNameSslBundles) {
    ProtocolHandler handler = this.connector.getProtocolHandler();
    Assert.state(handler instanceof AbstractHttp11Protocol,
            "To use SSL, the connector's protocol handler must be an AbstractHttp11Protocol subclass");
    configureSsl((AbstractHttp11Protocol<?>) handler, sslBundle, serverNameSslBundles);
    this.connector.setScheme("https");   // ★ scheme 改为 https
    this.connector.setSecure(true);      // ★ secure 标志，影响 request.isSecure()
}

private void configureSsl(AbstractHttp11Protocol<?> protocol, SslBundle sslBundle,
        Map<String, SslBundle> serverNameSslBundles) {
    protocol.setSSLEnabled(true);
    if (sslBundle != null) {
        addSslHostConfig(protocol, protocol.getDefaultSSLHostConfigName(), sslBundle);   // ★ 主证书
    }
    serverNameSslBundles.forEach((serverName, bundle) -> addSslHostConfig(protocol, serverName, bundle));  // ★ SNI 多证书
}
```

**关键设计：`SslBundle` 抽象**——Spring Boot 3.1+ 引入的证书管理抽象层，`SslBundle` 封装了 keystore/truststore/密码/协议版本/密码套件（`SslStoreBundle` + `SslOptions` + `SslBundleKey`），支持**运行时更新**（`addBundleUpdateHandler`——证书轮换时无需重启应用，`update()` 方法重新走一次 `addSslHostConfig`）。

**多证书场景（SNI）**：`serverNameSslBundles` 支持给不同域名配置不同证书——`addSslHostConfig` 每次调用都以 `serverName` 为 key 创建一个 `SSLHostConfig`，底层对应 Tomcat 的 `AbstractHttp11Protocol.addSslHostConfig(SSLHostConfig, boolean)`——一个 Connector 可以承载多个 `SSLHostConfig`，请求到达时按 TLS SNI 扩展中的 hostname 匹配对应证书。

**证书验证模式**（`configureSslClientAuth`）：

```java
private void configureSslClientAuth(SSLHostConfig config) {
    config.setCertificateVerification(ClientAuth.map(this.clientAuth, "none", "optional", "required"));
}
```

对应 `server.ssl.client-auth`：`NONE`（默认，不验证客户端证书）/ `WANT`（可选双向认证）/ `NEED`（强制双向认证，即 mTLS）。

> **面试点**：`SslBundle` 和以前直接配 `server.ssl.key-store` 的区别是什么？
> ——`server.ssl.key-store` 系配置仍然可用，但内部会被转换成一个匿名 `SslBundle`。`SslBundle` 的价值在于**抽象出证书来源**（可以是文件、也可以是 PEM 内容、也可以来自 Vault/K8s Secret 等外部系统）和**支持热更新**——`addBundleUpdateHandler` 注册的回调让证书轮换时能不重启应用直接生效，这是老版本"改配置必须重启"做不到的。

---

## 2. 反向代理场景：正确识别客户端身份

### 2.1 问题的本质

生产环境几乎不会让客户端直连 Tomcat——前面通常有 K8s Ingress、云厂商 ELB/SLB、或自建 Nginx。这带来一个根本问题：**Tomcat 看到的 `request.getRemoteAddr()` 永远是反代的 IP，不是真实客户端 IP**。

这不只是"日志记录不准"的小问题——如果业务代码用 `remoteAddr` 做**限流**（每 IP 限流）或**风控**（IP 黑名单），反代场景下所有客户端会被识别成同一个 IP（反代自己），限流形同虚设。

**解决方案**：反代把真实客户端 IP 写入 `X-Forwarded-For` 头，Tomcat 侧用 `RemoteIpValve` 解析这个头，"还原"出真实的 `remoteAddr`。

### 2.2 RemoteIpValve 完整机制

**源码**：`org.apache.catalina.valves.RemoteIpValve`（完整类）——这是 Tomcat 对 Apache HTTPD `mod_remoteip` 的移植版本。

**核心处理逻辑**（`invoke()`，`RemoteIpValve.java:581-759`）：

```
1. 判断 originalRemoteAddr（当前连接对端 IP，即反代自己）是否匹配 internalProxies 或 trustedProxies
   ├─ 不匹配（既非内网也非信任代理）→ 完全跳过处理，remoteAddr 保持原样
   └─ 匹配 → 进入解析流程：

2. 从 X-Forwarded-For（或自定义 remoteIpHeader）取出 IP 链（可能多级代理，逗号分隔）
   例：X-Forwarded-For: 1.2.3.4, 10.0.0.5, 10.0.0.6
   （从左到右：客户端 → 第一层代理 → 第二层代理 → ... → 到达 Tomcat 前的最后一跳）

3. 从右向左遍历这条链：
   ├─ 是 internalProxies（内网 IP 段）→ 直接丢弃（不出现在结果里）
   ├─ 是 trustedProxies（信任代理）→ 记入 proxiesHeader（X-Forwarded-By），继续往左找
   └─ 遇到第一个既非内网也非信任的 IP → 这就是"真实客户端 IP"，停止遍历

4. request.setRemoteAddr(真实IP)，request.setRemoteHost(...)
   剩余（还没处理的）IP 段重新写回 remoteIpHeader；已处理的进入 proxiesHeader

5. 处理 protocolHeader（X-Forwarded-Proto）：
   ├─ 值匹配 protocolHeaderHttpsValue（默认 "https"）→ setSecure(true) + scheme=https + serverPort=443（或自定义 httpsServerPort）
   └─ 否则 → setSecure(false) + scheme=http

6. 处理完成后打上标记：request.setAttribute(Globals.REQUEST_FORWARDED_ATTRIBUTE, Boolean.TRUE)

7. 请求处理完毕后（finally 块），所有被修改的字段（remoteAddr/remoteHost/secure/scheme/serverName/port/两个header）
   全部还原为原始值 —— 这保证了该 Valve 的效果只在本次请求生命周期内有效，不污染连接级别的状态
```

**关键设计点**：`internalProxies` 与 `trustedProxies` 的**语义差异**——两者都会被"穿透"（不当作客户端 IP），但结果不同：`internalProxies` 匹配的 IP **直接丢弃**，不出现在任何输出头里；`trustedProxies` 匹配的 IP 会被**记录进 `X-Forwarded-By`**，留痕但不当客户端 IP。这个设计是为了区分"完全信任、无需审计的内部基础设施"（如容器网络内的 sidecar）和"外部但可信、需要留痕的代理"（如公司自建的反代集群）。

**默认 `internalProxies` 正则**（源码注释里列出）覆盖了常见私有地址段：`10/8`、`192.168/16`、`169.254/16`（链路本地）、`127/8`（loopback）、`100.64/10`（NAT64/CGN）、`172.16/12`、`::1`（IPv6 loopback）。**K8s 集群内部 IP（通常在 `10.x.x.x` 或 `172.x.x.x` 段）默认就落在这个范围内**——这也是为什么很多人在 K8s 环境里"什么都没配"却发现 RemoteIpValve 依然生效的原因（见 2.4 节自动探测）。

### 2.3 Spring Boot 侧的定制入口

**`TomcatWebServerFactoryCustomizer.customizeRemoteIpValve()`**（`TomcatWebServerFactoryCustomizer.java:230-257`）：

```java
private void customizeRemoteIpValve(ConfigurableTomcatWebServerFactory factory) {
    Remoteip remoteIpProperties = this.serverProperties.getTomcat().getRemoteip();
    String protocolHeader = remoteIpProperties.getProtocolHeader();
    String remoteIpHeader = remoteIpProperties.getRemoteIpHeader();
    // 为向后兼容，只要配了 protocol-header 也会启用这个 valve
    if (StringUtils.hasText(protocolHeader) || StringUtils.hasText(remoteIpHeader)
            || getOrDeduceUseForwardHeaders()) {
        RemoteIpValve valve = new RemoteIpValve();
        valve.setProtocolHeader(StringUtils.hasLength(protocolHeader) ? protocolHeader : "X-Forwarded-Proto");
        if (StringUtils.hasLength(remoteIpHeader)) {
            valve.setRemoteIpHeader(remoteIpHeader);
        }
        valve.setTrustedProxies(remoteIpProperties.getTrustedProxies());
        valve.setInternalProxies(remoteIpProperties.getInternalProxies());
        try {
            valve.setHostHeader(remoteIpProperties.getHostHeader());
        } catch (NoSuchMethodError ex) {
            // 兼容旧版本 Tomcat（8.5 < 8.5.44 / 9.0 < 9.0.23）没有这个方法
        }
        valve.setPortHeader(remoteIpProperties.getPortHeader());
        valve.setProtocolHeaderHttpsValue(remoteIpProperties.getProtocolHeaderHttpsValue());
        factory.addEngineValves(valve);   // ★ 加到 Engine 阀门链（第 2 篇知识：所有 Host/Context 共享）
    }
}
```

**触发这个 Valve 有三种方式**（`||` 三个条件任一满足）：
1. 显式配置 `server.tomcat.remoteip.protocol-header`（历史兼容，即使只配这一项也够）
2. 显式配置 `server.tomcat.remoteip.remote-ip-header`
3. **`getOrDeduceUseForwardHeaders()` 返回 true**——这是最容易被忽略但生产中最常触发的路径

### 2.4 三种策略：NATIVE / FRAMEWORK / NONE

**`getOrDeduceUseForwardHeaders()`**（`TomcatWebServerFactoryCustomizer.java:259-265`）：

```java
private boolean getOrDeduceUseForwardHeaders() {
    if (this.serverProperties.getForwardHeadersStrategy() == null) {
        CloudPlatform platform = CloudPlatform.getActive(this.environment);
        return platform != null && platform.isUsingForwardHeaders();
    }
    return this.serverProperties.getForwardHeadersStrategy() == ServerProperties.ForwardHeadersStrategy.NATIVE;
}
```

`ServerProperties.forwardHeadersStrategy`（`ServerProperties.java:94-97`）是一个可空的枚举，三个值定义在 `ServerProperties.ForwardHeadersStrategy`（`ServerProperties.java:1982-1998`）：

| 策略 | 语义 | 底层行为 |
|---|---|---|
| `NATIVE` | 用容器原生支持处理转发头 | 启用 `RemoteIpValve`（本节机制） |
| `FRAMEWORK` | 用 Spring 框架层处理转发头 | 走 `ForwardedHeaderFilter`（Servlet Filter，不涉及 Tomcat Valve） |
| `NONE` | 完全忽略 `X-Forwarded-*` | 两者都不启用 |
| **`null`（默认，不配置）** | **自动探测云平台** | 见下方 `CloudPlatform` 逻辑 |

**云平台自动探测的关键**（`CloudPlatform.java`）：

```java
// CloudPlatform.java:223-225，基类默认实现
public boolean isUsingForwardHeaders() {
    return true;   // ★ 除 NONE 外，所有平台默认认为"用了转发头"
}

// KUBERNETES 平台检测逻辑（CloudPlatform.java:108-140）
KUBERNETES {
    private static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
    private static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";
    @Override
    public boolean isDetected(Environment environment) {
        // 检测系统环境变量中是否存在 KUBERNETES_SERVICE_HOST / KUBERNETES_SERVICE_PORT
        // 这两个变量由 K8s 自动注入到每个 Pod（Service discovery 机制的一部分）
        ...
    }
}
```

**这意味着**：**只要你的应用跑在 K8s Pod 里（`KUBERNETES_SERVICE_HOST` 环境变量存在），什么都不配置，`forwardHeadersStrategy` 默认走自动探测分支，`CloudPlatform.getActive()` 返回 `KUBERNETES`，`isUsingForwardHeaders()` 继承基类默认实现返回 `true`——`RemoteIpValve` 自动启用**！这也解释了很多人"没配任何 remoteip 相关参数，K8s 环境下却发现 IP 识别是对的"的现象。

> **面试点**：K8s Ingress 场景下，`forward-headers-strategy` 应该配成什么？
> ——多数场景**不需要配置**，因为自动探测已经会在 K8s 环境下启用 `NATIVE` 等效行为。但**显式配置 `NATIVE` 是更好的实践**——避免依赖隐式的环境变量探测（万一某天 K8s 环境变量命名变化、或应用被迁移到非 K8s 但同样有反代的环境，隐式探测会失效）。如果团队用 Spring MVC 的 `ForwardedHeaderFilter` 做统一处理（比如同时要处理 reactive 网关），应该显式配 `FRAMEWORK` 并确保没有同时启用 `RemoteIpValve`——两者都开会导致 IP 被"还原"两次，语义混乱（第一次 Valve 处理后 `remoteAddr` 已经是"客户端 IP"，Filter 层再按 `X-Forwarded-For` 头解析会读到已经被 Valve 修改过的 header 状态，取决于处理时序容易出现count-off-by-one 或使用了本该被丢弃的中间代理 IP）。

### 2.5 生产配置示例

```yaml
server:
  forward-headers-strategy: native   # 显式声明，不依赖自动探测
  tomcat:
    remoteip:
      # 内网段：K8s 集群网络、公司内网 IP 段（按实际网络规划调整）
      internal-proxies: "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}"
      # 信任代理：云厂商负载均衡器的固定 IP 段（示例，按实际云厂商文档配置）
      trusted-proxies: "203\\.0\\.113\\.\\d{1,3}"
      remote-ip-header: X-Forwarded-For
      protocol-header: X-Forwarded-Proto
      protocol-header-https-value: https
```

**注意事项**：`internal-proxies`/`trusted-proxies` 是**正则表达式**，不是 CIDR，配置时要注意转义（YAML 里反斜杠要双写或用其他分隔符）。如果反代的真实 IP 落在这两个正则之外，`RemoteIpValve` 的"信任检查"（2.2 节步骤 1）会直接判定不匹配，整个解析流程被跳过——`X-Forwarded-For` 完全不被处理，`remoteAddr` 还是反代自己的 IP。**这是最常见的踩坑点**：以为配了 `remoteip.remote-ip-header` 就万事大吉，忘记检查反代自己的出口 IP 是否落在 `internal-proxies`/`trusted-proxies` 范围内。

---

## 3. 自定义 Customizer 实战：三个完整可跑的例子

Spring Boot 提供的三种 Customizer（第 8 篇 7.1 已讲职责分层）都可以自定义 Bean 注入，覆盖官方没有暴露配置项的场景。这里给三个生产常见的完整例子。

### 3.1 场景一：彻底移除 Server 响应头

1.2 节提到 `server.server-header=""` 不能真正移除头部，需要直接操作 `AbstractHttp11Protocol`：

```java
@Bean
public TomcatConnectorCustomizer removeServerHeaderCustomizer() {
    return (connector) -> {
        if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocol) {
            protocol.setServer(null);                          // 不设置固定值
            protocol.setServerRemoveAppProvidedValues(true);    // ★ 关键：移除应用层设置的值
        }
    };
}
```

`TomcatConnectorCustomizer` 由 `TomcatServletWebServerFactory.customizeConnector()` 末尾遍历执行（`TomcatServletWebServerFactory.java:364-366`）——**在 Spring Boot 自身的 `customizeConnector` 逻辑之后**，所以能覆盖框架默认行为。回顾 `Http11Processor` 的逻辑（1.2 节）：`server == null` 且 `getServerRemoveAppProvidedValues() == true` 时执行 `headers.removeHeader("server")`——彻底不发送这个头。

### 3.2 场景二：自定义 Valve 实现 IP 白名单

生产场景：内部管理接口只允许公司 IP 段访问，其他 IP 直接拒绝。Tomcat 自带的 `RemoteAddrValve`（继承 `RequestFilterValve`）已经能做这件事：

```java
@Bean
public TomcatContextCustomizer adminIpWhitelistCustomizer() {
    return (context) -> {
        RemoteAddrValve valve = new RemoteAddrValve();
        // allow 命中放行；若只配 deny 没配 allow，未命中 deny 的默认放行（源码 3.3 节已述）
        valve.setAllow("127\\.0\\.0\\.1|10\\.0\\.0\\.\\d{1,3}|192\\.168\\.1\\.\\d{1,3}");
        valve.setDenyStatus(403);   // 拒绝时返回的状态码，默认也是 403
        context.getPipeline().addValve(valve);   // ★ Context 级别，只影响这一个应用
    };
}
```

**源码依据**（`RemoteAddrValve.java`，逻辑非常薄的一个实现类）：

```java
public final class RemoteAddrValve extends RequestFilterValve {
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        String property;
        if (getUsePeerAddress()) {
            property = request.getPeerAddr();          // TCP 连接层面的对端地址（不经过 RemoteIpValve 篡改）
        } else {
            property = request.getRequest().getRemoteAddr();   // 可能已被 RemoteIpValve 还原成真实客户端 IP
        }
        if (getAddConnectorPort()) {
            property = property + ";" + request.getConnector().getPortWithOffset();
        }
        process(property, request, response);   // 交给父类 RequestFilterValve 做正则匹配
    }
}
```

**`RequestFilterValve.process()` 的判定逻辑**（`RequestFilterValve.java:352-365`）：

```java
protected void process(String property, Request request, Response response) throws IOException, ServletException {
    if (isAllowed(property)) {
        getNext().invoke(request, response);   // 命中 allow → 放行，继续 Pipeline
        return;
    }
    // 未命中 allow（或命中 deny）→ 拒绝
    denyRequest(request, response);
}
```

**allow/deny 的优先级规则**（源码类注释）：先检查 `deny`，命中则拒绝；否则检查 `allow`，命中则放行；**若只配置了 deny 没配置 allow，未命中 deny 的请求默认放行**——这意味着**白名单场景必须显式配置 `allow`**，只配 `deny` 做黑名单是另一种用法，不要混淆。

**如果需要自定义拒绝逻辑**（比如白名单外返回自定义 JSON 而不是默认错误页），继承 `RequestFilterValve` 覆写 `getLog()` 和实现 `invoke()`——`RemoteAddrValve` 本身逻辑很薄，完全可以照着写一个业务专属版本：

```java
public class CustomIpWhitelistValve extends RequestFilterValve {
    private static final Log log = LogFactory.getLog(CustomIpWhitelistValve.class);

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        process(request.getRemoteAddr(), request, response);
    }

    @Override
    protected void denyRequest(Request request, Response response) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(403);
        response.getWriter().write("{\"code\":403,\"message\":\"IP not allowed\"}");
        response.finishResponse();   // 显式结束响应，不再走 super 的 sendError
    }

    @Override
    protected Log getLog() {
        return log;
    }
}
```

> **面试点**：为什么 IP 白名单要用 Valve 而不是 Spring 的 Interceptor/Filter？
> ——性能与拦截时机。Valve 在**容器层**执行，比 Servlet Filter 更早（Filter 已经进入 Servlet 规范容器，走完 Wrapper 分配才轮到 Filter 链）；对于"直接拒绝、根本不需要进入应用逻辑"的场景，Valve 层拦截省去了 Filter 链、Servlet 分配等一系列容器开销。且 Valve 可以精确控制加在哪一层（Engine/Host/Context），比如把管理接口的白名单 Valve 只加在特定 Context 上，不影响其他应用。

### 3.3 场景三：双端口（业务端口 + 管理端口）

**注意**：这不是简单的"加两个 Connector"——**同一个应用暴露两个独立端口，Spring Boot 的做法是启动两个几乎完全独立的 `ApplicationContext`**（这是本篇写作过程中核实源码后纠正的一个常见误解，很多资料把它简化描述成"加一个 Connector"，源码上并非如此）。

**核实过的机制**（`ChildManagementContextInitializer.java`，完整类）：

```java
class ChildManagementContextInitializer implements BeanRegistrationAotProcessor, SmartLifecycle {
    @Override
    public void start() {
        if (this.managementContext == null) {
            ConfigurableApplicationContext managementContext = createManagementContext();   // ★ 创建独立子 ApplicationContext
            registerBeans(managementContext);
            managementContext.refresh();   // ★ 独立刷新，独立创建 WebServer
            this.managementContext = managementContext;
        }
    }
}
```

`createManagementContext()`（`ManagementContextFactory.java:60-72`）新建一个**独立的 Environment + ApplicationContext**，`setParent(parentContext)` 建立父子关系（用于 Bean 查找委托），但**这个子 Context 会独立完成一次 `refresh()`，独立触发 `onRefresh()` → `createWebServer()` → 独立的 `TomcatServletWebServerFactory.getWebServer()`**——也就是说，管理端口跑的是一个**独立的 Tomcat 实例**（独立的 `Tomcat` 对象、独立的容器树），只是共享同一个 JVM 进程和父容器里注册的 Bean（通过 `registerWebServerFactoryFromParent` 把父容器 WebServerFactory 的**类**继承过来，创建一个新实例）。

**配置方式**（应用层面只需要配置，不需要手写代码）：

```yaml
server:
  port: 8080
management:
  server:
    port: 9090            # ★ 触发独立 ManagementContext 创建
    address: 127.0.0.1    # 管理端口只监听本地，K8s 场景下常配合 sidecar 抓取
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

`ManagementServerProperties.port`（`ManagementServerProperties.java:40-44`）非空即代表"用不同端口"——`ManagementContextAutoConfiguration` 系列自动装配根据这个值决定是否走 `ChildManagementContextInitializer` 路径（同端口时管理端点直接注册进主 Context 的 Servlet 映射，不创建独立 WebServer）。

**如果需要真正意义上"同一个 Tomcat 实例内加第二个业务 Connector"**（比如既有 HTTP 又要监听一个内部专用端口，但共享同一套 Context/Servlet），才是本篇标题里"双端口"的另一种理解——用 `addAdditionalTomcatConnectors`（`TomcatServletWebServerFactory.java:742-744`）：

```java
@Bean
public WebServerFactoryCustomizer<TomcatServletWebServerFactory> additionalConnectorCustomizer() {
    return (factory) -> {
        Connector internalConnector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        internalConnector.setPort(8081);
        internalConnector.setScheme("http");
        factory.addAdditionalTomcatConnectors(internalConnector);   // ★ 同一个 Tomcat 实例的第二个 Connector
    };
}
```

**注意源码里的警告**（`TomcatServletWebServerFactory.java:738-742` javadoc）：`getTomcatConnectorCustomizers()` 注册的 Connector 定制器**不会**应用到通过这个方法添加的 Connector 上——这些附加 Connector 需要自己在 Lambda 里手动配置好所有属性，不会继承主 Connector 的定制链。

**两种"双端口"方案对比**：

| 方案 | 底层机制 | 适用场景 |
|---|---|---|
| `management.server.port` | 独立 `ApplicationContext` + 独立 `Tomcat` 实例 | Actuator 管理端点与业务端口物理隔离（常见于生产安全要求：管理端口只在内网可达） |
| `addAdditionalTomcatConnectors` | 同一个 `Tomcat` 实例的多个 `Connector`，共享同一套 Context/Servlet | 同一套业务逻辑需要多端口暴露（如同时监听 HTTP 明文端口和内部专用端口） |

### 3.4 场景四：自定义 Executor（线程池注册）

**注册链路**（`TomcatServletWebServerFactory.getWebServer()` 内部，`TomcatServletWebServerFactory.java:211`）：

```java
Connector connector = new Connector(this.protocol);
...
registerConnectorExecutor(tomcat, connector);   // ★ 见下方
```

```java
// TomcatServletWebServerFactory.java:222-226
private void registerConnectorExecutor(Tomcat tomcat, Connector connector) {
    if (connector.getProtocolHandler().getExecutor() instanceof Executor executor) {
        tomcat.getService().addExecutor(executor);   // ★ 注册到 Service 层，纳入生命周期管理
    }
}
```

这段代码的含义：**如果 Connector 的 `ProtocolHandler` 已经设置了自定义 `Executor`（Tomcat 的 `org.apache.catalina.Executor`，不是 `java.util.concurrent.Executor`），把它注册进 `Service`**——这样 `StandardService.startInternal()`（第 1 篇 6.1）里的 `executor.init()` / `executor.start()` 才会管理它的生命周期。

**自定义 Executor 的完整示例**（比如需要一个命名清晰、带监控埋点的自定义线程池，而不是用默认的 Tomcat 内部实现）：

```java
@Bean
public TomcatProtocolHandlerCustomizer<?> customExecutorCustomizer() {
    return (protocolHandler) -> {
        StandardThreadExecutor executor = new StandardThreadExecutor();
        executor.setName("biz-worker-pool");      // Service 里的引用名（Connector 可通过 executor 属性按此名引用）
        executor.setNamePrefix("biz-worker-");    // ★ 真正的线程名前缀，jstack 里可识别（回扣第 10 篇线程名分析）
        executor.setMaxThreads(300);
        executor.setMinSpareThreads(30);
        executor.setMaxQueueSize(Integer.MAX_VALUE);
        protocolHandler.setExecutor(executor);   // ★ 挂到 ProtocolHandler
    };
}
```

**两个"名字"不要混淆**：`setName()` 设置的是这个 Executor 在 `Service` 内的**引用名**（`StandardThreadExecutor.java:225-227`，供其他 Connector 通过 `executor` 属性按名字共享同一个线程池，`StandardService.getExecutor(String executorName)` 按这个名字查找）；真正决定 jstack 里线程显示名称前缀的是 `setNamePrefix()`（`StandardThreadExecutor.java:200-202`），默认值是 `"tomcat-exec-"`（`StandardThreadExecutor.java:57`），内部创建线程池时通过 `TaskThreadFactory(namePrefix, ...)`（`StandardThreadExecutor.java:116`）消费这个字段。

**为什么这里用 `TomcatProtocolHandlerCustomizer` 而不是 `TomcatConnectorCustomizer`**——`Connector.getProtocolHandler().setExecutor()` 在两者里都能调，选 `ProtocolHandlerCustomizer` 是因为它的语义更贴合"配置协议处理层"，而 Connector 定制器通常处理端口/URI 编码等连接器级配置，职责边界更清晰（第 8 篇 2.3 的分层原则）。

`StandardThreadExecutor` 实现了 Tomcat 的 `Executor` 接口（`org.apache.catalina.Executor extends java.util.concurrent.Executor, Lifecycle`），所以能被 `registerConnectorExecutor()` 正确识别并纳入生命周期管理——设置成功后，`jstack` 里看到的线程名会从默认的 `http-nio-8080-exec-N` 变成 `biz-worker-N`，配合第 10 篇的线程名定位法，可以在多线程池混用的场景下精确区分不同池子的线程。

---

## 4. 运维闭环：让编排系统看懂你的服务

### 4.1 优雅关闭与 K8s preStop 配合

**第 8 篇 9.1 已详细讲过 `GracefulShutdown` 的内部机制**（暂停 Connector → 关闭 socket → 轮询活跃请求）。本节聚焦**运维层面怎么配合 K8s 用好它**。

**核实过的完整时序**（结合 K8s Pod 生命周期）：

```
K8s 决定终止 Pod（滚动更新/缩容/节点驱逐）
  │
  ├─ 1. Pod 状态改为 Terminating，从 Service Endpoints 移除
  │      （但 kube-proxy 更新有延迟，仍可能有新流量短暂进入！这是常见事故点）
  │
  ├─ 2. K8s 执行 preStop Hook（如果配置了）
  │      建议：sleep 5~10 秒，给 Endpoints 摘除留出传播时间
  │
  ├─ 3. K8s 发送 SIGTERM 给容器主进程
  │      Spring Boot 收到 SIGTERM → JVM shutdown hook 触发
  │      → ConfigurableApplicationContext.close()
  │      → lifecycleProcessor.onClose() → 按 phase 反序停止 SmartLifecycle
  │      → WebServerGracefulShutdownLifecycle.stop(callback)   [第 8 篇 9.1]
  │           └─ webServer.shutDownGracefully(callback)
  │                └─ GracefulShutdown.shutDownGracefully()
  │                     ├─ 所有 Connector.pause() + closeServerSocketGraceful()（拒绝新连接）
  │                     └─ 轮询等待 getCountAllocated()/getInProgressAsyncCount() 归零
  │
  ├─ 4. K8s 等待 terminationGracePeriodSeconds（默认 30 秒）
  │      若优雅关闭在这个窗口内完成 → 进程自然退出
  │      若超时仍未完成 → K8s 发送 SIGKILL 强制杀死（未完成的请求直接中断！）
  │
  └─ 5. Pod 从 API Server 删除
```

**关键运维数字关系**：`terminationGracePeriodSeconds` 必须 **大于** 业务最长请求处理时间，否则优雅关闭会被 SIGKILL 抢跑。经验公式：

```
terminationGracePeriodSeconds ≥ preStop 等待时间 + 最长请求处理时间 + 安全余量（5~10s）
```

**K8s 配置示例**：

```yaml
spec:
  terminationGracePeriodSeconds: 60   # 留足够的窗口给优雅关闭完成
  containers:
    - name: app
      lifecycle:
        preStop:
          exec:
            command: ["sh", "-c", "sleep 8"]   # 等 Endpoints 摘除传播
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        periodSeconds: 5
```

> **面试点**：优雅关闭为什么不能保证 100% 不丢请求？
> ——两个原因。第一，**Endpoints 摘除的传播延迟**：K8s 把 Pod 从 Service 移除是异步的，kube-proxy/Ingress Controller 感知到变化需要时间，这个窗口期内可能仍有新流量打到即将关闭的 Pod（这也是为什么 `preStop` 建议加 sleep，用时间换安全）。第二，`GracefulShutdown` 只是**暂停新连接、等待已有活跃请求完成**——如果活跃请求本身有 bug（死循环、无限等待下游），会一直阻塞在 `awaitInactiveOrAborted()` 里，直到被 `terminationGracePeriodSeconds` 超时后 SIGKILL 强制中断，此时这个请求必然失败。

### 4.2 健康检查：HealthCheckValve

Tomcat 10.1 内置了一个轻量级健康检查 Valve（**注意**：这是 Tomcat 层面的健康检查，和 Spring Boot Actuator 的 `/actuator/health` 是**两套独立机制**，容易混淆）。

**完整源码**（`HealthCheckValve.java`，逻辑很简单的一个类）：

```java
public class HealthCheckValve extends ValveBase {
    private static final String UP = "{\n  \"status\": \"UP\",\n  \"checks\": []\n}";
    private static final String DOWN = "{\n  \"status\": \"DOWN\",\n  \"checks\": []\n}";
    private String path = "/health";
    protected boolean checkContainersAvailable = true;

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        MessageBytes urlMB = context ? request.getRequestPathMB() : request.getDecodedRequestURIMB();
        if (urlMB.equals(path)) {
            response.setContentType("application/json");
            if (!checkContainersAvailable || isAvailable(getContainer())) {
                response.getOutputStream().print(UP);
            } else {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.getOutputStream().print(DOWN);
            }
        } else {
            getNext().invoke(request, response);   // 不匹配路径，交给下一个 Valve（正常业务流程）
        }
    }

    protected boolean isAvailable(Container container) {
        for (Container child : container.findChildren()) {
            if (!isAvailable(child)) {   // ★ 递归检查所有子容器
                return false;
            }
        }
        if (container instanceof LifecycleBase) {
            return ((LifecycleBase) container).getState().isAvailable();   // 第 1 篇 available 标志
        }
        return true;
    }
}
```

**这个 Valve 的定位**：它工作在**容器层最外围**（Engine 或更高层挂载时），**在到达任何业务 Servlet 之前**就直接返回。`isAvailable()` 递归检查整棵容器树——只要有一个子容器（比如某个 Context 启动失败卡在非 available 状态）不可用，整体报 `DOWN`。这是一个**纯容器状态**的健康检查，**不涉及业务逻辑**（不检查数据库连接、不检查下游依赖）。

**与 Spring Boot Actuator 健康检查的对比**：

| | Tomcat `HealthCheckValve` | Spring Boot Actuator `/actuator/health` |
|---|---|---|
| 检查内容 | 容器树 Lifecycle 状态（`available` 标志） | 可插拔 `HealthIndicator`（DB/Redis/磁盘/自定义业务检查） |
| 触发层级 | Valve（容器层，Servlet 之前） | Servlet（DispatcherServlet 之后，走完整 MVC 栈） |
| 默认启用 | **默认不启用**，需要手动挂载 | Spring Boot Actuator 依赖引入后默认启用 |
| 典型用途 | 极端场景下的"容器进程是否还活着"探测（Servlet 容器本身瘫痪时 Actuator 端点可能也无法响应，此时这个更底层的 Valve 反而更可靠） | 业务级健康检查（K8s liveness/readiness 探针的常规选择） |

**挂载方式**（Spring Boot 没有自动装配，需要手动加）：

```java
@Bean
public WebServerFactoryCustomizer<TomcatServletWebServerFactory> healthCheckValveCustomizer() {
    return (factory) -> {
        HealthCheckValve valve = new HealthCheckValve();
        valve.setPath("/internal-health");
        factory.addEngineValves(valve);
    };
}
```

> **面试点**：K8s 的 liveness probe 应该用哪种健康检查？
> ——**通常用 Spring Boot Actuator 的 `/actuator/health/liveness`**，因为它能反映应用的实际存活状态（配合 `spring-boot-actuator` 的 `LivenessStateHealthIndicator`，与 `AvailabilityChangeEvent` 联动）。`HealthCheckValve` 更适合作为**兜底探测**——极端场景下（比如线程池被打满、Actuator 端点本身因为线程耗尽无法响应）,这个更底层、开销更小的 Valve 可能仍能返回结果。生产实践中很少两者都配，多数直接用 Actuator，但了解 `HealthCheckValve` 的存在是"容器层也有健康检查能力"这一知识点的体现。

### 4.3 访问日志规范

**第 8 篇 8.2 已讲过 `customizeAccessLog` 源码走读**，这里聚焦**生产环境该怎么配 pattern**。

**核实过的完整定制方法**（`TomcatWebServerFactoryCustomizer.java:327-349`）：

```java
private void customizeAccessLog(ConfigurableTomcatWebServerFactory factory) {
    ServerProperties.Tomcat tomcatProperties = this.serverProperties.getTomcat();
    AccessLogValve valve = new AccessLogValve();
    PropertyMapper map = PropertyMapper.get();
    Accesslog accessLogConfig = tomcatProperties.getAccesslog();
    map.from(accessLogConfig.getConditionIf()).to(valve::setConditionIf);
    map.from(accessLogConfig.getConditionUnless()).to(valve::setConditionUnless);
    map.from(accessLogConfig.getPattern()).to(valve::setPattern);
    map.from(accessLogConfig.getDirectory()).to(valve::setDirectory);
    map.from(accessLogConfig.getPrefix()).to(valve::setPrefix);
    map.from(accessLogConfig.getSuffix()).to(valve::setSuffix);
    map.from(accessLogConfig.getEncoding()).whenHasText().to(valve::setEncoding);
    map.from(accessLogConfig.getLocale()).whenHasText().to(valve::setLocale);
    map.from(accessLogConfig.isCheckExists()).to(valve::setCheckExists);
    map.from(accessLogConfig.isRotate()).to(valve::setRotatable);
    map.from(accessLogConfig.isRenameOnRotate()).to(valve::setRenameOnRotate);
    map.from(accessLogConfig.getMaxDays()).to(valve::setMaxDays);
    map.from(accessLogConfig.getFileDateFormat()).to(valve::setFileDateFormat);
    map.from(accessLogConfig.isIpv6Canonical()).to(valve::setIpv6Canonical);
    map.from(accessLogConfig.isRequestAttributesEnabled()).to(valve::setRequestAttributesEnabled);
    map.from(accessLogConfig.isBuffered()).to(valve::setBuffered);
    factory.addEngineValves(valve);
}
```

**生产推荐 pattern**（结合链路追踪场景）：

```yaml
server:
  tomcat:
    accesslog:
      enabled: true
      directory: /var/log/app
      prefix: access_log
      suffix: .log
      pattern: '%h %l %u %t "%r" %s %b %D "%{X-Request-Id}i" "%{Referer}i" "%{User-Agent}i"'
      rotate: true
      rename-on-rotate: true    # 滚动时先重命名再压缩，避免正在写入的文件被误操作
      max-days: 30               # 保留 30 天，配合外部日志采集（Filebeat/Fluentd）落盘后清理
      request-attributes-enabled: true   # 让 %{X-Request-Id}i 之类的模式能取到 RemoteIpValve 设置的属性
```

**pattern 占位符要点**（面试常问）：

| 占位符 | 含义 |
|---|---|
| `%h` | 远程主机名（若 `enableLookups=false` 就是 IP，默认关闭 DNS 反查以避免性能损耗） |
| `%l` | 远程逻辑用户名（RFC 1413，几乎总是 `-`，无实际价值但保留做格式兼容） |
| `%u` | 认证用户名（`REMOTE_USER`，未认证时是 `-`） |
| `%t` | 访问时间 |
| `%r` | 请求行（`METHOD /path HTTP/1.1`） |
| `%s` | 响应状态码 |
| `%b` | 响应字节数（不含 HTTP 头），`-` 表示 0 |
| `%D` | 处理耗时（**微秒**，`AbstractAccessLogValve.java:1806` → `new ElapsedTimeElement(true, false)` → `Style.MICROSECONDS`；`%T` 才是秒级，`AbstractAccessLogValve.java:1830-1831` → `new ElapsedTimeElement(false, false)` → `Style.SECONDS`） |
| `%{Header}i` | 请求头（`i` = incoming） |
| `%{Header}o` | 响应头（`o` = outgoing） |

**`request-attributes-enabled` 的意义**：这个开关控制 `AbstractAccessLogValve` 是否读取 `AccessLog.REMOTE_ADDR_ATTRIBUTE` 等**请求属性**而不是直接读 `request.getRemoteAddr()`——回顾 2.2 节 `RemoteIpValve` 的 `requestAttributesEnabled` 逻辑（`RemoteIpValve.java:722-729`），两者是**配套开关**：`RemoteIpValve` 写入属性，`AccessLogValve` 读取属性，只有两边都开启，访问日志里记录的 `%h` 才会正确反映**反代之后还原出的真实客户端 IP**，而不是反代自己的 IP。这是一个容易被忽略的**跨 Valve 配合点**——只开一边不会报错，但日志记录会悄悄地记录成反代 IP。

### 4.4 Actuator + JMX 监控

**JMX 现状回顾**（第 1 篇 7.2 已详述）：`Registry.disableRegistry()` 默认关闭了 Tomcat 内部组件的 MBean 自动注册。生产环境重新打开 JMX 需要：

```yaml
server:
  tomcat:
    mbeanregistry:
      enabled: true   # ★ 重新允许 Tomcat 内部组件注册 MBean
```

打开后可以通过标准 JMX 客户端（`jconsole`/VisualVM/Prometheus JMX Exporter）读取：

```
Catalina:type=ThreadPool,name="http-nio-8080"
  ├─ currentThreadsBusy    ← 忙线程数，接近 maxThreads = 线程池即将耗尽
  ├─ currentThreadCount    ← 当前实际线程数（可能小于 maxThreads，按需扩张）
  ├─ maxThreads
  └─ connectionCount       ← 当前连接数，接近 maxConnections = 连接即将打满

Catalina:type=GlobalRequestProcessor,name="http-nio-8080"
  ├─ requestCount          ← 累计请求数
  ├─ errorCount            ← 累计错误数
  ├─ processingTime        ← 累计处理时间
  └─ maxTime                ← 单次请求最大处理时间
```

（`mbeans-descriptors.xml` 中 `connectionCount`/`currentThreadCount`/`currentThreadsBusy` 分别定义在 38/48/52 行的 `ThreadPool` 描述块，以及 257/267/271 行的另一处描述——Tomcat 对不同 `ProtocolHandler` 类型的 MBean 描述符是分开定义的。）

**但生产环境更常见的做法是走 Actuator**——`/actuator/metrics/tomcat.threads.busy`、`tomcat.threads.current`、`tomcat.threads.config.max` 这些 Micrometer 指标，以及 Prometheus + Grafana（`micrometer-registry-prometheus` 依赖引入后 `/actuator/prometheus` 直接输出可被 Prometheus 抓取的指标格式）。

**关键澄清（第二轮 review 核实源码后纠正的一个重大误解）**：**Micrometer 的 Tomcat 线程池指标本质上仍然是通过 JMX 读取的，`mbeanregistry.enabled=false`（默认值）会导致这些指标完全拿不到数据**——这与直觉相反，值得仔细梳理清楚。

Spring Boot 侧的绑定入口是 `TomcatMetricsAutoConfiguration`（完整类）：

```java
@AutoConfiguration(after = CompositeMeterRegistryAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnClass({ TomcatMetrics.class, Manager.class })
public class TomcatMetricsAutoConfiguration {
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean({ TomcatMetrics.class, TomcatMetricsBinder.class })
    public TomcatMetricsBinder tomcatMetricsBinder(MeterRegistry meterRegistry) {
        return new TomcatMetricsBinder(meterRegistry);
    }
}
```

`TomcatMetricsBinder`（`spring-boot-actuator` 模块，完整类）在应用启动完成（`ApplicationStartedEvent`）后，只是**找到 Session 管理器 `Manager`**，然后创建 Micrometer 官方的 `io.micrometer.core.instrument.binder.tomcat.TomcatMetrics` 并绑定：

```java
@Override
public void onApplicationEvent(ApplicationStartedEvent event) {
    ApplicationContext applicationContext = event.getApplicationContext();
    Manager manager = findManager(applicationContext);
    this.tomcatMetrics = new TomcatMetrics(manager, this.tags);
    this.tomcatMetrics.bindTo(this.meterRegistry);
}
```

注意：这里传进 `TomcatMetrics` 的只是 `Manager`（Session 管理器），**不是 `ThreadPoolExecutor`**——真正采集线程池/连接数指标的逻辑在 Micrometer 库自己的 `TomcatMetrics.registerThreadPoolMetrics()`（`io.micrometer.core.instrument.binder.tomcat.TomcatMetrics`，Micrometer 源码）：

```java
public static MBeanServer getMBeanServer() {
    List<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
    if (!mBeanServers.isEmpty()) {
        return mBeanServers.get(0);
    }
    return ManagementFactory.getPlatformMBeanServer();   // ★ JVM 全局 MBeanServer
}

private void registerThreadPoolMetrics(MeterRegistry registry) {
    registerMetricsEventually(":type=ThreadPool,name=*", (name, allTags) -> {
        Gauge.builder("tomcat.threads.config.max", mBeanServer,
                s -> safeDouble(() -> s.getAttribute(name, "maxThreads")))   // ★ 通过 JMX ObjectName 查询属性
            .tags(allTags).baseUnit(BaseUnits.THREADS).register(registry);
        Gauge.builder("tomcat.threads.busy", mBeanServer,
                s -> safeDouble(() -> s.getAttribute(name, "currentThreadsBusy")))
            .tags(allTags).baseUnit(BaseUnits.THREADS).register(registry);
        ...
    });
}
```

**`registerMetricsEventually` 的实现更进一步印证了这一点**：它先查 `mBeanServer.queryNames(...)` 看目标 ObjectName（`*:type=ThreadPool,name=*`）**是否已经存在**，不存在就注册一个 `NotificationListener` 监听 MBean 注册事件，等 MBean **真正出现**在 MBeanServer 里之后才补注册对应的 Micrometer Gauge。

**这条链路完全建立在"Tomcat 的 ThreadPool MBean 已经注册到 JVM 全局 `MBeanServer`"这个前提之上**。回顾第 1 篇 7.2 节和本篇前面的机制：`disableMBeanRegistry` 默认值是 **`true`**（`TomcatServletWebServerFactory.java:158`），对应 `server.tomcat.mbeanregistry.enabled` 默认 **`false`**（`ServerProperties.java:1052`）——默认情况下 `Registry.disableRegistry()` 会把 Tomcat 内部的单例 `Registry` 替换成 `NoDescriptorRegistry`（第 1 篇已述），而 `NoDescriptorRegistry.registerComponent()` 是**彻底的 no-op**（`NoDescriptorRegistry.java:58-62`），且它内部持有的 `MBeanServer` 实现（`NoJmxMBeanServer`）与 JVM 全局 MBeanServer**完全无关**——`LifecycleMBeanBase.register()`（第 1 篇 7.1 节）调的 `Registry.getRegistry(null, null).registerComponent(...)` 会直接落进这个 no-op 分支。**也就是说：默认配置下，`Catalina:type=ThreadPool,...` 这个 MBean 根本不存在于任何 MBeanServer 里，Micrometer 的 `registerMetricsEventually` 查不到任何匹配的 ObjectName，`tomcat.threads.busy` 等指标全部处于"注册了 Gauge 但取不到值"（`safeDouble` 捕获异常返回 `NaN`）甚至"连 Gauge 都没注册"（如果走的是 `NotificationListener` 等待分支，且 MBean 永远不会出现）的状态。**

**正确结论**：`server.tomcat.mbeanregistry.enabled=true` 是让 `/actuator/metrics/tomcat.threads.*` 系列指标能拿到真实数据的**必要前提**，不是可有可无的选项。这与第 11 篇最初的直觉（"Micrometer 走进程内直接调用，不依赖 JMX"）恰恰相反——Micrometer 对 Tomcat 的这套 `MeterBinder` 设计**完全基于 JMX**，Session 相关指标（`tomcat.sessions.*`）例外——那部分确实是 `TomcatMetricsBinder` 直接传入 `Manager` 对象引用、用 `Gauge.builder(..., manager, Manager::getActiveSessions)` 直接调用方法（不经过 JMX），但线程池/连接数/全局请求处理这几组最常被关注的指标，**全部依赖 JMX MBean 已注册**这个前提。

**面试常考的对比结论（修正版）**：JMX 客户端（`jconsole`/VisualVM）与 Actuator + Micrometer 并不是两条互相独立的路径——**Micrometer 的 Tomcat 线程池指标本身就是"包装了一层的 JMX 客户端"**，都要求 `mbeanregistry.enabled=true`。两者的区别只在于**消费方式**：JMX 客户端需要专门连接 MBeanServer 手动查看；Micrometer 把同样的数据转换成标准化的 `/actuator/metrics` 或 Prometheus 格式，接入现代可观测性生态。**如果不开 `mbeanregistry.enabled`，两条路都拿不到 Tomcat 侧的线程池/连接数指标**，这也是生产环境一个常见的隐性坑——引入了 Micrometer + Prometheus，却忘了开这个开关，导致 Grafana 面板上 Tomcat 相关面板一直空白。

> **面试点**：`server.tomcat.mbeanregistry.enabled` 关闭时，Actuator 上还能看到哪些 Tomcat 相关指标？
> ——**Session 相关指标不受影响**（`tomcat.sessions.active.current`/`tomcat.sessions.created` 等，因为 `TomcatMetricsBinder` 直接拿 `Manager` 对象引用调方法，不查 JMX，`TomcatMetricsBinder.java:67-78` 的 `findManager()` 直接遍历 `tomcatWebServer.getTomcat().getHost().findChildren()` 拿到 `Context.getManager()`）；但**线程池/连接数/全局请求处理相关的所有指标全部拿不到**（`tomcat.threads.*`/`tomcat.connections.*`/`tomcat.global.*`/`tomcat.servlet.*`），因为它们全部通过 `MBeanServer.queryNames()`+`getAttribute()` 查询 JMX ObjectName，而这些 ObjectName 从未被注册。这是一个"部分依赖 JMX、部分不依赖"的混合设计，容易被简化误传为"完全不依赖 JMX"。

---

## 5. 本篇小结与面试要点

### 5.1 本篇地图

```
第 8 篇：配置映射表（配置项 → 源码，静态视角）
第 10 篇：场景驱动调优（性能相关决策）
第 11 篇（本篇）：安全加固 + 运维闭环
第 12 篇：故障排查实战
```

### 5.2 面试要点速查

1. **三条独立的版本泄露渠道**：`Server` 响应头（`connector.setProperty("server",...)` → `Http11Processor` 强制覆盖）、`X-Powered-By`（`CoyoteAdapter` 独立开关，默认关闭）、错误页脚注（`ErrorReportValve.showServerInfo`，默认 true，Spring Boot 用 `include-stacktrace=NEVER` 触发关闭）——**必须分别核实，关一个不代表全关**
2. **彻底移除 Server 头**需要 `AbstractHttp11Protocol.setServerRemoveAppProvidedValues(true)`，仅设置 `server.server-header=""` 无效
3. **`ErrorReportValve` 的两个开关不同粒度**：`showServerInfo` 只影响版本号那一行；`showReport` 影响整个详细报告块（类型/消息/堆栈/root cause）
4. **RemoteIpValve 三段式判定**：先检查连接对端 IP 是否在 `internalProxies`/`trustedProxies` 中（不在则整个流程跳过）→ 从右向左解析 `X-Forwarded-For` 链 → 找到第一个非信任 IP 作为真实客户端 IP
5. **`forward-headers-strategy` 默认自动探测**：K8s 环境下 `KUBERNETES_SERVICE_HOST` 环境变量存在即触发 `CloudPlatform.KUBERNETES` 检测，`isUsingForwardHeaders()` 默认返回 true，等效启用 NATIVE 策略——生产建议显式配置而非依赖隐式探测
6. **RemoteIpValve 与 AccessLogValve 的配合**：`requestAttributesEnabled` 是两个 Valve 之间的配套开关，只开一边访问日志会悄悄记录成反代 IP
7. **双端口的两种实现路径完全不同**：`management.server.port` 是**独立 ApplicationContext + 独立 Tomcat 实例**（`ChildManagementContextInitializer`）；`addAdditionalTomcatConnectors` 才是**同一个 Tomcat 实例的多 Connector**，两者不要混淆
8. **优雅关闭无法保证零丢失**：Endpoints 摘除传播延迟（用 `preStop` sleep 缓解）+ 活跃请求本身可能因 bug 无限阻塞（`terminationGracePeriodSeconds` 超时后仍会被 SIGKILL）
9. **HealthCheckValve 与 Actuator health 是两套机制**：前者是容器层 Lifecycle 状态检查（默认不启用），后者是可插拔的业务级健康检查（默认启用）——K8s liveness 探针通常用 Actuator，`HealthCheckValve` 更多是兜底
10. **Micrometer 的 Tomcat 线程池指标本质上仍依赖 JMX**（第二轮 review 纠正的重大误解）：`mbeanregistry.enabled=false`（默认值）会导致 `Registry.disableRegistry()` 把 MBean 注册变成 no-op，`Catalina:type=ThreadPool,...` 根本不存在，Micrometer 通过 `MBeanServer.queryNames()` 查询这个 ObjectName 必然查不到——`tomcat.threads.*`/`tomcat.connections.*`/`tomcat.global.*` 全部拿不到数据；只有 `tomcat.sessions.*` 是例外（`TomcatMetricsBinder` 直接持有 `Manager` 对象引用，不查 JMX）
