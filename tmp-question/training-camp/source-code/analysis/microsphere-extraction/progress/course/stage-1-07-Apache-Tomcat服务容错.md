# stage-1 · 第 7 节：基于 Apache Tomcat 实现 Web 服务容错性 — 知识点提取

> 课程：stage-1 服务治理 第 7 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/07. 第七节：基于 Apache Tomcat 实现 Web 服务容错性(Fault Tolerance).md`
> 提取时间：2026-08-09 | 权重：核心（Tomcat 是 Web 容器基石）

---

## 一、本节概览

- **技术域**：Tomcat（Servlet 容器核心 + 线程模型 + Spring Boot 整合 + 限流）
- **维度**：`[规范]`（Servlet 规范实现）+ `[性能优化]`（线程模型）+ `[工程问题]`（Spring Boot 整合）+ `[分布式问题]`（限流）
- **核心命题**：理解 Tomcat 作为 Servlet 容器参考实现的核心组件、线程模型，以及 Spring Boot 如何整合、如何限流
- **知识点数**：9 个
- **前置**：Servlet 规范、Java 线程池/AQS、Spring Boot 自动装配

## 前置条件清单
读者需先掌握：
1. **Servlet 规范**（Context/Connector/Engine/Host 层级）
2. **Java 线程池/AQS**（ThreadPoolExecutor 行为）
3. **Spring Boot 自动装配**（WebServerFactory/Customizer）
4. **JMX**（管理/限流用）
未达前置者，先补：Servlet 规范入门 + Java 并发(线程池) + Spring Boot 自动装配

## 掌握度
目标读者：**本人（读源码多，Spring/并发熟悉）** — 已确认
讲解策略：Spring Boot 整合 + 线程池直接讲（你熟悉）；Tomcat 核心组件/目录结构补基础

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Tomcat 设计意图与目录结构
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Servlet 规范
- **需求**：Tomcat 是 Servlet 规范参考实现，快速高效的 Web 容器
- **自主实现**：若实现 Servlet 容器，核心是"解析请求 → 找 Context/应用 → 调 Servlet → 返回"
- **参考实现**：Tomcat 是 Servlet 规范参考实现，也提供 JSP/EL/WebSocket；目录 bin/conf/lib/logs/temp/webapps/work
- **对比取舍**：作为参考实现严格遵守规范；同时注重性能、可管理性(JMX)
- **测试佐证**：Tomcat 源码在 `/data/workspace/source-code/code/spring/tomcat`

### KP-02 Tomcat 核心组件层级（Server/Service/Engine/Host/Connector/Context）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、Servlet 规范
- **需求**：理解 Tomcat 的组件层级模型
- **自主实现**：若设计容器，分层次——容器(Server) → 服务(Service) → 引擎(Engine) → 主机(Host) → 上下文(Context)
- **参考实现**：
  - **Server**：整个容器
  - **Service**：中间组件，连接 N 个 Connector 到 1 个 Engine
  - **Engine**：特定 Service 的请求处理管道，接收所有 Connector 请求
  - **Host**：网络名称关联（虚拟主机），Engine 可含多个 Host
  - **Connector**：处理与客户端通信（HTTP/AJP）
  - **Context**：Web 应用，Host 可含多个 Context
- **对比取舍**：层级模型是"职责嵌套"——Server>Service>Engine>Host>Context，Connector 横向接入
- **关联 microsphere**：`[待验证]` microsphere-tomcat(空壳)无实现
- **测试佐证**：Tomcat 源码 org.apache.catalina 下的 Server/Engine/Host/Connector/Context 接口

### KP-03 Tomcat 启动过程（Bootstrap）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：类加载
- **需求**：Tomcat 如何启动
- **自主实现**：设 ClassLoader → 反射加载启动类 → 处理参数
- **参考实现**：`org.apache.catalina.startup.Bootstrap` 设置 ClassLoader → 反射加载 `Catalina` → 处理命令行参数(如 start)
- **对比取舍**：Bootstrap 是启动入口，用反射解耦

### KP-04 Tomcat 线程模型（线程池行为）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Java 线程池/AQS
- **需求**：理解 Tomcat 请求处理线程模型
- **自主实现**：预创建核心线程 → 创建 Max 线程 → 入队
- **参考实现**（docs 分析）：
  - **Tomcat 行为**：预创建核心线程，再创建 Max 线程，最后入队（core:1,max:3 → task-1 入队/created...）
  - **传统 ThreadPoolExecutor**：核心线程 → 队列 → 创建 Max 线程
  - **区别**：Tomcat 优先扩线程再入队（避免队列堆积）
- **对比取舍**：Tomcat 与 JDK ThreadPoolExecutor 的线程扩展顺序不同——Tomcat 更激进扩线程
- **关联 microsphere**：`[待验证]`
- **待验证**：可用 Tomcat 源码 `org.apache.tomcat.util.threads.ThreadPoolExecutor` 验证

### KP-05 Tomcat 限流（全局 Web 服务限流）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：JMX、线程池
- **需求**：利用 JMX 和 Tomcat API 实现全局限流
- **自主实现**：限制线程池 min/max + 限制连接数
- **参考实现**：
  - 限制 ThreadPoolExecutor min/max 线程数
  - 限制 Connector `AbstractEndpoint#setMaxConnections`
  - 同步 vs 异步 Servlet：同步=Boss/Worker 同线程(100ms)；异步=兄弟线程(适合长轮询/配置中心)
- **对比取舍**：限流粒度——线程数 + 连接数双限制
- **关联 microsphere**：`[待验证]`

### KP-06 同步 vs 异步 Servlet（线程模型影响）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：KP-05、Servlet 异步
- **需求**：理解同步/异步 Servlet 对线程占用和吞吐的影响
- **自主实现**：同步 Servlet 占线程直到完成；异步 Servlet 释放线程
- **参考实现**：同步=Boss/Worker 同线程(100ms 全占用)；异步=Boss/Worker 兄弟线程（适合 HTTP Long Poll/配置中心，增加吞吐）
- **对比取舍**：异步增加吞吐但客户端可忍受延迟；经典场景长轮询
- **待验证**：docs 的 Boss/Worker 描述可用 Tomcat NIO 源码验证

### KP-07 Spring Boot 整合 Tomcat（配置工厂/Customizer）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring Boot 自动装配
- **需求**：Tomcat 配置作为 Spring Bean 控制
- **自主实现**：若整合，用配置类(ServerProperties) + 工厂 + Customizer 链控制 Tomcat
- **参考实现**：
  - 配置类：`ServerProperties`（前缀 server.）+ 内部 Tomcat/Accesslog/Threads
  - 工厂：`ConfigurableServletWebServerFactory` / `ConfigurableTomcatWebServerFactory` / `TomcatServletWebServerFactory`
  - 自定义：`WebServerFactoryCustomizer` → `TomcatServletWebServerFactory` Bean → N 个 Customizer → 创建 WebServer
  - Customizer：`TomcatContextCustomizer` / `TomcatConnectorCustomizer` / `TomcatProtocolHandlerCustomizer`
- **对比取舍**：Spring Boot 把 Tomcat 配置抽象为 Bean + Customizer 链，声明式控制
- **测试佐证**：biz-web `MyTomcatWebServerFactoryCustomizer` + `MyTomcatProtocolHandlerCustomizer`
- **关联 microsphere**：`[待验证]` microsphere-spring-boot 是否有 Tomcat 扩展

### KP-08 JVM 进程关闭机制
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：JVM 生命周期
- **需求**：理解 JVM 关闭的几种方式
- **自主实现**：区分系统退出/信号退出/网络 Endpoint 退出
- **参考实现**：`System.exit(int)` → Runtime.exit；ShutdownHook(addShutdownHook)；SpringBoot shutdown Endpoint / Tomcat shutdown Endpoint
- **对比取舍**：优雅关闭 = ShutdownHook + Endpoint，避免强制退出丢状态
- **待验证**：可用 JDK17 源码验证 Runtime.exit/ShutdownHook

### KP-09 限流常见模式（4 层）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：限流概念
- **需求**：理解限流的 4 个层级
- **自主实现**：按流量入口/应用/框架/组件分层限流
- **参考实现**：
  1. **网关限流**：总流量入口，应用无关
  2. **Web Server 限流**：特定应用（Servlet Engine: Tomcat/Jetty/Undertow；Netty）
  3. **Web Framework 限流**：特定资源（Servlet/WebMVC/WebFlux/Vert.x）
  4. **组件限流**：特定组件
- **对比取舍**：限流分层——粒度从粗到细（网关→容器→框架→组件）
- **关联 microsphere**：`[待验证]` microsphere-alibaba-sentinel 属于框架/组件层

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| Tomcat 设计/目录 | 规范 | 核心 | P1 | 🟡 | High |
| 核心组件层级 | 规范 | 核心 | P1 | 🔴 | High |
| 启动过程 | 工程 | 核心 | P1 | 🟡 | High |
| 线程模型 | 性能 | 核心 | P1 | 🔴 | High |
| Tomcat 限流 | 分布式 | 核心 | P1 | 🟡 | High |
| 同步/异步 Servlet | 性能 | 核心 | P2 | 🟡 | Medium |
| Spring Boot 整合 | 工程 | 核心 | P1 | 🔴 | High |
| JVM 关闭 | 工程 | 支撑 | P2 | 🟡 | High |
| 限流 4 层模式 | 分布式 | 核心 | P1 | 🟡 | High |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **Spring Boot Tomcat 整合**：biz-web `MyTomcatWebServerFactoryCustomizer` + `MyTomcatProtocolHandlerCustomizer` = KP-07 实证
- **Tomcat 源码**：`/data/workspace/source-code/code/spring/tomcat`（可验证核心组件/线程模型）
- `[待验证]` microsphere-tomcat(空壳)无实现；microsphere-spring-boot 是否有 Tomcat 扩展

---

## 五、本节小结（三层次视角）

**需求**：理解 Tomcat（Servlet 容器基石）的核心组件、线程模型、Spring Boot 整合与限流——这是 Web 服务容错的基础。

**自主实现核心**：若我设计——
1. 组件层级 Server>Service>Engine>Host>Context + Connector 横向接入
2. 线程模型：预创建核心线程 → 扩 Max → 入队（比 JDK 线程池更激进）
3. Spring Boot 用 WebServerFactory + Customizer 链控制 Tomcat
4. 限流：线程数 + 连接数 + 4 层模式（网关→容器→框架→组件）

**参考实现**：biz-web 的 TomcatWebServerFactoryCustomizer/ProtocolHandlerCustomizer + Tomcat 源码实证。

**对比取舍**：本篇横跨 4 个维度——**规范**(Servlet 组件层级)、**性能**(线程模型)、**工程**(Spring Boot 整合)、**分布式**(限流)。你熟悉 Spring Boot 整合和线程池，重点补 Tomcat 组件层级(Server/Engine/Host/Context)。

**待验证汇总**：
- Tomcat ThreadPoolExecutor 扩展顺序（可用 code/spring/tomcat 验证）
- 同步/异步 Servlet Boss/Worker（可用 NIO 源码验证）
- Runtime.exit/ShutdownHook（可用 JDK17 验证）
- microsphere-spring-boot 是否有 Tomcat 扩展
