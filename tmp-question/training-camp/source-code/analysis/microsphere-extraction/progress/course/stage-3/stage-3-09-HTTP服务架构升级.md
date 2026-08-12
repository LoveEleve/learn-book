# stage-3 · 第 09 节：第七节："高性能" HTTP 服务架构升级 — 知识点提取

> 课程：stage-3 三高架构 第 09 节（容器/服务组 09/10）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/09. 第七节："高性能" HTTP 服务架构升级.md`
> 提取时间：2026-08-12 | 权重：核心（Servlet 规范机制 + HTTP 性能升级三路径——异步/非阻塞/HTTP2）
> 案例载体：my-xhs（决策 B）

---

## 一、本节概览

- **技术域**：Servlet 规范（版本演进/核心 API/生命周期/注册/异步）、Spring MVC 集成（DispatcherServlet/异步支持）、Spring Boot 嵌入式容器、HTTP 性能升级三路径
- **维度**：`[规范]`（Servlet——本篇主体）+ `[工程问题]`（Spring 集成/注册）+ `[性能优化]`（HTTP 升级）
- **核心命题**：**HTTP 服务性能升级的三条路径**（docs 主要内容）——①异步（Servlet 3.0 AsyncContext）②非阻塞（Servlet 3.1 NIO）③HTTP/2（Servlet 4.0）——docs 前半是 Servlet 规范深度教程（机制本体），命名空间 javax→jakarta 迁移 ≠ 机制
- **知识点数**：8 个
- **前置**：stage-1 03/04（REST/Spring MVC）、05 篇（Tomcat）、07/08 篇（注册中心——HTTP 服务架构上下文）

## 前置条件清单
读者需先掌握：
1. **Spring MVC 请求处理链**（stage-1 03/04）
2. **Tomcat 容器架构**（05 篇 KP-09）
3. **HTTP 协议基础**（1.1/2.0 差异）
未达前置者，先补：stage-1 03/04

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **规范机制优先**：Servlet 版本演进/生命周期/异步机制（时间无关），javax→jakarta 标注迁移
- **实例锚定**：my-xhs jakarta（Boot 3.x）+ http2 实证 + WebFlux 网关对照
- **docs 场景**：docs 用 Servlet 3.0-4.0（2017）；现代是 Servlet 6.0（Tomcat 10.1/jakarta）——版本迁移≠机制

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Servlet 规范演进与核心 API（版本表/核心组件/命名空间迁移）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[过时→Servlet 6.0/jakarta（javax→jakarta 迁移≠机制）]` | **置信度**：High
- **前置**：无
- **来源**：docs §Servlet 简介 + §Servlet 版本 + §Servlet 核心 API
- **需求**：掌握 Servlet 规范的**演进主线**——异步（3.0）→ 非阻塞（3.1）→ HTTP/2（4.0）→ 现代 6.0（jakarta）
- **自主实现**：若我设计——理解"规范能力 = 容器能力"：升级 Servlet 版本 = 容器升级 + 命名空间迁移
- **参考实现**（docs 版本表 + 架构师）：**Servlet 定位**（docs §什么是 Servlet）——基于 Java 的 Web 组件、容器管理生命周期、平台无关；**功能位置**——介于 CGI 与服务扩展（NSAPI/Apache 模块）之间；**体系归属**——Java EE 规范的一部分（非一开始就是 J2EE）；**docs 版本演进**——2.2（J2EE 1.2，WAR 独立应用）→ 2.3（Filter/Listener/Wrapper）→ 2.4（XML Schema）→ 2.5（Annotation）→ **3.0（Java EE 6：可插拔/简化部署/异步 Servlet/文件上传）**→ **3.1（Java EE 7：非阻塞 IO/WebSocket）**→ **4.0（Java EE 8：HTTP/2）**；**现代（架构师）**——Servlet 5.0（Java EE 9，**jakarta.servlet 命名空间**）→ 6.0（Jakarta EE 10，Tomcat 10.1）；**核心 API 表**（docs）——Servlet/Filter/ServletContext/AsyncContext（3.0）/ServletContextListener/ServletRequestListener/HttpSessionListener/AsyncListener（3.0）/ServletContainerInitializer（3.0）+ Spring 代表实现（DispatcherServlet/CharacterEncodingFilter/ContextLoaderListener/RequestContextListener/HttpSessionMutexListener/StandardServletAsyncWebRequest/SpringServletContainerInitializer）；**命名空间迁移（04 §3.2.1）**——`javax.servlet.*` → `jakarta.servlet.*`（Servlet 5.0 起）——**迁移≠机制**；**my-xhs 实证**——Boot 3.2.5（Servlet 6.0/jakarta）：`AuthController.java:10` `import jakarta.validation.Valid`
- **对比取舍**：**规范升级节奏**——异步/非阻塞/HTTP2 是 Servlet 的三个里程碑（docs 主要内容三路径的来源）；现代基线 = Servlet 6.0 + jakarta
- **测试佐证**：docs §Servlet 版本表 + §核心 API 表 + my-xhs `AuthController.java:10`（jakarta 实证）

### KP-02 Servlet 生命周期与组件注册（三生命周期 + 三注册方式 + Tomcat 实现链）
- **维度**：`[规范]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[过时→jakarta 命名空间]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §Servlet 生命周期 + §Filter/ServletContext 生命周期 + §Servlet 组件注册 + §Tomcat 实现
- **需求**：掌握 Servlet 组件的**生命周期契约与注册方式**——容器管理的本质
- **自主实现**：若我设计——init/service/destroy 三阶段；注册三通道（web.xml/注解/编程）
- **参考实现**（docs）：**生命周期**——Servlet（init(ServletConfig)/service/Request+Response/destroy）、Filter（init(FilterConfig)/doFilter+FilterChain/destroy）、ServletContext（contextInitialized/contextDestroyed）；**三注册方式**（docs 表）——传统 web.xml（`<servlet>+<servlet-mapping>`/`<filter>+<filter-mapping>`/`<listener>`）、注解（@WebServlet/@WebFilter/@WebListener）、编程（`ServletContext#addServlet/addFilter/addListener`）；**web.xml 示例**（docs）——`contextConfigLocation` 参数 + `ContextLoaderListener` 声明；**Tomcat 实现链**（docs 明确）——`ApplicationContextFacade`（门面）→ `ApplicationContext`（底层）→ `StandardContext`（决定者）→ 继承 `LifecycleBase`（init()/start() 生命周期回调）
- **对比取舍**：**注解/编程 vs web.xml**——声明式便利 vs 集中配置——现代 Boot 走编程（RegistrationBean，KP-06）
- **测试佐证**：docs §生命周期三节 + §注册三方式表 + §Tomcat 实现链

### KP-03 Spring MVC Servlet 集成（DispatcherServlet 初始化链）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：Spring MVC（stage-1 03/04）
- **来源**：docs §DispatcherServlet 初始化过程 + spring-framework 源码实证
- **需求**：理解 Spring MVC 与 Servlet 容器的**集成契约**——DispatcherServlet 初始化链（Spring 侧容器启动流程）
- **自主实现**：若我设计——Servlet 生命周期钩子内嵌 Spring 容器启动：init → 绑定配置 → 建 WebApplicationContext → 初始化策略组件
- **参考实现**（docs 链 + 源码实证）：**初始化链（docs 明确）**——`HttpServlet.init()` → `HttpServletBean.init()`（Servlet 初始化生命周期调用）→ `FrameworkServlet.initServletBean()`（ServletConfig 参绑定到字段）→ `FrameworkServlet.initWebApplicationContext()`（初始化 WebApplicationContext）→ `DispatcherServlet.onRefresh()` → `DispatcherServlet.initStrategies()`（初始化各种组件）；**源码实证**——spring-framework `spring-webmvc/.../DispatcherServlet.java` + `HttpServletBean.java`（存在实证）；**机制**——**"Servlet 容器启动 → Spring 容器启动"两级容器**：Servlet 生命周期是 Spring Web 容器的宿主钩子
- **对比取舍**：**两级容器模型**——Servlet 容器（Web 层）+ Spring 容器（业务层）——请求经 DispatcherServlet 分发（前端控制器模式）
- **测试佐证**：docs §DispatcherServlet 初始化过程（6 步链）+ spring-framework `spring-webmvc/.../DispatcherServlet.java`/`HttpServletBean.java`

### KP-04 Servlet 异步机制（AsyncContext/startAsync/AsyncListener）【docs 主要内容①】
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[过时→jakarta 命名空间（机制有效）]` | **置信度**：High
- **前置**：Servlet 3.0（KP-01）
- **来源**：docs §Servlet 异步支持 + §异步 Servlet 示例（代码全文）
- **需求**：掌握**异步 HTTP 升级的规范基础**——docs 主要内容第 1 条：释放 Servlet 线程（长任务不占容器线程）
- **自主实现**：若我设计——请求线程 startAsync 后立即归还容器线程池，长任务在业务线程池执行，完成后经 AsyncContext 写回
- **参考实现**（docs 示例代码全文）：**激活**——`@WebServlet(asyncSupported = true)`（Servlet 或 Filter 声明）；**入口**——`request.startAsync()` 创建 `AsyncContext`；**超时**——`asyncContext.setTimeout(50L)`；**监听**——`AsyncListener` 四回调（`onComplete`/`onTimeout`（可置 SC_SERVICE_UNAVAILABLE）/`onError`/`onStartAsync`，docs 示例完整实现）；**机制价值**——**容器线程不再被长任务阻塞**（Tomcat 线程池释放 → 提升并发吞吐——docs 主要内容"对比升级前后性能变化"的依据）；**现代形态**——Boot 3.x 内 `jakarta.servlet.AsyncContext`（命名空间迁移，机制不变）
- **对比取舍**：**异步 Servlet vs 阻塞 Servlet**——线程占用率 vs 代码复杂度（回调/超时处理）；**异步是"释放容器线程"，不是"加快单请求"**——吞吐提升的来源
- **测试佐证**：docs §异步 Servlet 示例（AsyncServlet 全文：asyncSupported/startAsync/setTimeout/AsyncListener 四回调）

### KP-05 Spring MVC 异步支持（DeferredResult/Callable/CompletionStage）【docs 主要内容①续】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-04、Spring MVC
- **来源**：docs §DeferredResult/§Callable/§CompletionStage（**空节标题**）+ 架构师发散 + spring-framework 源码实证
- **需求**：掌握 Spring 侧异步 MVC 的三形态——**docs 空节（仅标题），机制由架构师发散 + 源码实证补全（08 §2）**
- **自主实现**：若我设计——控制器返回三形态之一：Callable（线程池执行）、DeferredResult（外部事件驱动完成）、CompletionStage（链式异步）
- **参考实现**（docs 标题 + spring-framework 实证 + 发散）：**三形态（docs 标题）**——`DeferredResult`（异步结果容器：请求线程释放，**业务线程/事件**完成时 setResult）+ `Callable`（Spring 调度线程池执行）+ `CompletionStage`（CompletableFuture 链）；**源码实证**——spring-framework `spring-web/.../context/request/async/DeferredResult.java`（存在实证；Spring 异步支持的基础类型在 spring-web 模块，webmvc 依赖）；**MVC 异步原理（docs 关联 JSR）**——Spring MVC 把异步返回包装为 Servlet 3.0 的 AsyncContext 使用（`StandardServletAsyncWebRequest`——docs KP-01 核心 API 表实证）+ 超时/错误映射；**my-xhs 对照**——common/trace/`MdcAwareExecutorService`（异步线程池 MDC 透传，03 篇已证——**异步化必须配套 trace 上下文透传**，否则链路断裂）
- **对比取舍**：**三形态选型**——Callable（简单同步风格包装）/DeferredResult（事件驱动/长轮询）/CompletionStage（链式组合）——按业务异步形态选
- **测试佐证**：docs §异步支持 3 标题 + spring-web `DeferredResult.java` + my-xhs `MdcAwareExecutorService`（03 篇）

### KP-06 Spring Boot 嵌入式容器注册（限制表/RegistrationBean/@ServletComponentScan）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：Spring Boot（stage-1 23/24）
- **来源**：docs §Spring Boot Servlet Web（限制表 + 注册 + 参考资料）
- **需求**：理解**嵌入式容器的注册差异**——web.xml 传统方式不可用，编程注册为主
- **自主实现**：若我设计——RegistrationBean 体系替代 web.xml；@ServletComponentScan 兼容注解扫描
- **参考实现**（docs）：**嵌入式限制表（docs 明确）**——`web.xml`（不支持→`RegistrationBean`/`@Bean`）、`ServletContainerInitializer`（不支持→`ServletContextInitializer`——**Boot 刻意设计：防第三方 WAR 库破坏 Boot 应用**，docs 参考材料二原文）、`@WebServlet` 等（有限支持→`@ServletComponentScan`）；**注册体系**——`ServletContextInitializer` → `RegistrationBean` 族（`ServletListenerRegistrationBean`/`FilterRegistrationBean`/`ServletRegistrationBean`）+ **`@ServletComponentScan` 扫描链**（docs 明确：扫描 package → @Web* → RegistrationBean Bean 定义 → RegistrationBean Bean）；**参考材料一**（docs）——web.xml 元素到 Boot 的映射：`<servlet/>`→ServletRegistrationBean、`<filter/>`→FilterRegistrationBean、XML ApplicationContext→@ImportResource
- **对比取舍**：**编程注册 vs web.xml**——类型安全/可条件化 vs 集中声明——Boot 生态默认编程（一致性/自动装配友好）
- **测试佐证**：docs §Spring Boot Servlet Web（限制表 + 扫描链）+ 参考材料三（@ServletComponentScan 原文）

### KP-07 传统 Servlet 容器部署（SpringBootServletInitializer/Tomcat 插件）【过时→容器化】
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[过时→容器化部署（Docker）]` | **置信度**：High
- **前置**：WAR 部署概念
- **来源**：docs §Spring Boot 应用传统 Servlet 容器部署（SpringBootServletInitializer/Tomcat 7/8 插件）
- **需求**：了解 Boot 应用跑在外置容器的历史路径——**现代已被容器化取代**
- **自主实现**：若我设计——SpringBootServletInitializer 扩展入口 + WAR 打包 + 外置 Tomcat
- **参考实现**（docs + 发散）：**扩展 `SpringBootServletInitializer`**（WAR 入口，docs 标题）；**Tomcat 插件**——tomcat7-maven-plugin（Servlet 3.0）/tomcat8-maven-plugin（Servlet 3.1，`exec-war-only` 目标打包可执行 JAR，Alfresco 仓库）；**回顾 Spring Web 自动装配**（docs）——Servlet SPI `ServletContainerInitializer` + `@HandlesTypes` → Spring 适配 `SpringServletContainerInitializer` → Spring SPI `WebApplicationInitializer`（编程 `AbstractDispatcherServletInitializer` / 注解 `AbstractAnnotationConfigDispatcherServletInitializer`）；**过时标注**——Tomcat 7/8 插件与 WAR 外置部署 `[过时→Docker 容器化部署（08 篇双部署模式对照）]`，但 **SCI/WebApplicationInitializer 机制是 Boot 嵌入式容器初始化的基础（KP-06 ServletContextInitializer 前身）**
- **对比取舍**：**外置容器 vs 嵌入式/容器化**——容器级能力 vs 部署一致性——现代默认嵌入式 + Docker
- **测试佐证**：docs §传统部署（Tomcat 7/8 插件 pom 全文 + Spring SPI 链）

### KP-08 HTTP 性能升级三路径（异步/非阻塞/HTTP2）【docs 主要内容③ + 实例锚定】
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]`（HTTP2 [有效]，Servlet 版本 [过时→6.0]） | **置信度**：High
- **前置**：HTTP 1.1/2.0
- **来源**：docs §主要内容（3 条）+ 架构师整合 + my-xhs 实证
- **需求**：掌握 **HTTP 服务性能升级的三条路径**——docs 主要内容全集：①异步（Servlet 3.0）②非阻塞（Servlet 3.1）③HTTP/2（Servlet 4.0），每条"对比升级前后性能"
- **自主实现**：若我设计——①长任务异步化（释放容器线程）②IO 读写非阻塞（NIO，大流量连接）③HTTP/2（多路复用/头部压缩/二进制帧）
- **参考实现**（docs 意图 + my-xhs 实证 + 发散）：**①异步（Servlet 3.0）**——AsyncContext 释放线程（KP-04/05）；**②非阻塞（Servlet 3.1）**——`ReadListener`/`WriteListener`（Servlet 3.1 非阻塞 IO，docs 版本表实证）——**现代形态：Reactive 栈（WebFlux/Netty）**——my-xhs gateway 即 WebFlux（05 篇已证：spring-cloud-starter-gateway + WebHandler）——**非阻塞的现代答案**；**③HTTP/2（Servlet 4.0+）**——多路复用/头部压缩/服务端推送；**my-xhs 实证**——`my-xhs-user/src/main/resources/application.yml:18-19`（`http2: enabled: true`——**HTTP/2 已启用**）+ compression（05 篇已证）；**评估方法**——docs 每条都"对比升级前后性能变化"（02 篇 6 步流程：基线 → 改 → 对比）
- **对比取舍**：**三路径的组合**——异步（线程）→ 非阻塞（IO）→ HTTP2（协议）——分层升级；**阻塞 Servlet 栈 vs Reactive 栈**——Servlet 3.1 非阻塞 vs WebFlux 全异步——现代高并发默认 Reactive（网关/IO 密集）
- **测试佐证**：docs §主要内容 3 条 + my-xhs `application.yml:18-19`（http2）+ 05 篇（gateway WebFlux/compression）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Servlet 规范演进与核心 API | 规范 | 核心 | P1 | 🟡 | 过时→6.0/jakarta | High |
| 生命周期与组件注册 | 规范 | 支撑 | P2 | 🟡 | 过时→jakarta | High |
| Spring MVC Servlet 集成 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| Servlet 异步机制（AsyncContext） | 规范 | 核心 | P1 | 🔴 | 过时→jakarta | High |
| Spring MVC 异步支持（三形态） | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| Boot 嵌入式容器注册 | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| 传统容器部署（WAR 路径） | 工程问题 | 支撑 | P3 | 🟢 | 过时→容器化 | High |
| HTTP 性能升级三路径 | 性能优化 | 核心 | P1 | 🔴 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：spring-framework（code/spring）+ my-xhs（实例）
- **关键源码**（本次实证）：
  - spring-framework `spring-webmvc/.../DispatcherServlet.java` + `HttpServletBean.java`（docs 初始化链的存在基础）
  - spring-framework `spring-web/.../context/request/async/DeferredResult.java`（Spring 异步支持基础类型）
  - my-xhs `AuthController.java:10`（jakarta.validation 实证——Servlet 6.0/jakarta 命名空间）
  - my-xhs `my-xhs-user/src/main/resources/application.yml:18-19`（http2.enabled=true）+ 05 篇（gateway WebFlux/compression）
- **诚实标注**：docs §DeferredResult/§Callable/§CompletionStage 为**空节标题** → KP-05 架构师发散 + 源码实证补全（08 §2）；docs 的 Servlet 4.0（2017）→ 现代 6.0（jakarta，Boot 3.x）；docs Spring Boot 2.0 参考文档链接（版本过时，机制有效）；Tomcat 7/8 Maven 插件 [过时→容器化]
- **关联标注**：stage-1 03/04（Spring MVC——docs 的 DispatcherServlet 是 stage-1 REST 的容器基础）；05 篇（Tomcat 参数/http2/WebFlux 网关）；03 篇（MdcAwareExecutorService——异步化配套）；08 篇（双部署模式对照）

---

## 五、本节小结（三层次视角）

**需求**：HTTP 服务性能升级三路径——异步（Servlet 3.0）、非阻塞（3.1）、HTTP/2（4.0）——docs 以 Servlet 深度教程铺垫机制。

**自主实现核心**：若我设计——①AsyncContext 释放容器线程（长任务业务池）②非阻塞 IO/Reactive（大连接）③HTTP/2 开启（多路复用）+ 压缩；每条"改前基线 → 改 → 对比"（02 篇 6 步）。

**参考实现**：docs（Servlet 规范教程 + 异步示例全文）+ spring-framework 源码（DispatcherServlet/DeferredResult）+ **my-xhs 实证**（jakarta/http2 enabled/WebFlux 网关）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**HTTP 性能升级的三层路径**"——线程层（异步）→ IO 层（非阻塞/Reactive）→ 协议层（HTTP2）；Servlet 规范是载体（3.0→6.0 迁移≠机制），现代高并发默认 Reactive 栈 + HTTP/2。

**待验证汇总**：
- Servlet 3.1 ReadListener/WriteListener 非阻塞 IO 的容器细节（docs 仅版本表提及）
- Spring MVC 异步三形态的完整实现链（DeferredResult 存在已证，请求处理映射细节待展）
- my-xhs 各服务 HTTP/2 实际生效面（配置已开，压测对比数据无）

---

## 六、现状核对（my-xhs 落地核对与差距清单）【重点篇】

### 现状核对表

| docs 目标（升级动作） | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| ① 异步 HTTP（Servlet 3.0/Spring 异步） | ⚠️ **部分异步化**：15 文件用 CompletableFuture + common/AsyncConfig（自定义线程池）+ **OrderService.java:77【O2修复】"异步补偿/联动任务线程池（MDC 感知，替代 CompletableFuture.runAsync 的 commonPool 丢 traceId）"** + :465-466（释放库存/退券异步）——**"异步化配套 trace 透传"已正确落地** | 差距：业务服务无 DeferredResult/Callable 风格（MVC 异步出口），控制器层仍阻塞等待；可按长任务路径评估（如订单创建） |
| ② 非阻塞 HTTP（Servlet 3.1/Reactive） | ⚠️ **网关层**：WebFlux（spring-cloud-starter-gateway + WebHandler 实证，05 篇）✅；**业务服务**：阻塞 Tomcat 栈（threads.max 差异化实证） | 现状：入口非阻塞 + 业务阻塞（混合架构合理）；评估项：高并发瓶颈服务（如 feed/搜索）是否转 Reactive |
| ③ HTTP/2 | ✅ **已启用**：`http2.enabled=true`（user yml:18-19 实证）+ compression + keep-alive 调优 | 无（配置面完成） |
| 压测对比（docs 每路径都要求） | ❌ 无升级前后对比数据 | 缺口：HTTP 三路径的实测对比未做（02 篇同一缺口） |

### 差距清单（HTTP 升级优先级建议）

1. **P0**：建立真实压测基线（JMH 真实化或 JMeter 端到端）——三路径的"对比"前提（docs 意图核心）
2. **P1**：核对 gateway（WebFlux）server.tomcat 配置块（不生效则改 server.netty）
3. **P2**：长任务路径（订单创建/Feed 聚合）评估 DeferredResult/WebFlux 化
4. **P2**：启用 JFR（jcmd 动态）为 HTTP 升级做进程内取证

**结论**：09 篇 HTTP 升级的**配置面已大部分完成**（HTTP2/压缩/keep-alive/异步配套 trace），**测量面缺失**（无压测对比）是最大差距——这正好是 docs"对比升级前后性能变化"的核心动作。

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Servlet 规范深度教程 + 三路径意图（主要内容）；spring-framework/my-xhs 实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：HTTP 服务架构升级的完整认知该讲什么

docs 覆盖 Servlet 机制与三路径。完整还该包含：

1. **三条路径解决三个不同瓶颈**（docs 意图 + 发散）：异步=**线程占用**（长任务不占容器线程）、非阻塞=**IO 等待**（大连接/慢客户端）、HTTP/2=**协议开销**（连接复用/头部压缩）——**按瓶颈选路径，不盲目全上**；docs 的"对比升级前后性能"就是验证哪个瓶颈被解决（02 篇 6 步）
2. **异步的隐性成本：线程上下文**（架构师发散）：异步化后请求跨线程执行——**trace/MDC 透传是异步化的配套工程**（my-xhs `MdcAwareExecutorService` 实证，03 篇）——"异步化一半=链路断裂一半"
3. **Reactive 是"非阻塞"的现代答案**（发散）：Servlet 3.1 非阻塞 API 使用门槛高（ReadListener/WriteListener 手动管理）——**WebFlux/Netty 全异步栈**（my-xhs gateway 实证）是 Spring 生态的现代形态；**阻塞栈 + 大线程池 vs Reactive 栈**是架构选型（05 篇 Tomcat 线程参数对照）
4. **HTTP/2 的收益面**（发散）：多路复用（一个 TCP 多请求）、头部压缩（HPACK）、服务端推送——**网关/入口先开**（my-xhs gateway + user 已开实证）；注意 TLS（HTTP/2 需 h2/ALPN，现代强制 TLS）
5. **规范升级 = 容器升级 + 命名空间迁移**（docs 主线 + 04 纪律）：Servlet 4.0→6.0 的"迁移"是 javax→jakarta + Tomcat 10.x——**机制（AsyncContext/Listener）不变**；升级项目时区分"迁移工作"与"机制理解"
6. **两级容器模型**（docs 明确）：Servlet 容器（Web 生命周期）→ Spring 容器（业务）——DispatcherServlet 是桥（KP-03 初始化链）；**嵌入式容器让"容器"成为依赖而非部署环境**（KP-06 RegistrationBean 编程注册）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 异步 vs 阻塞 | 线程吞吐 vs 代码复杂度（回调/超时） |
| 非阻塞 IO vs 阻塞 IO | 大连接吞吐 vs 编程复杂度 |
| Servlet 3.1 非阻塞 vs WebFlux | 渐进 vs 全异步（现代选 Reactive） |
| HTTP/1.1 vs HTTP/2 | 多路复用/压缩 vs TLS/兼容 |
| 编程注册 vs web.xml | 类型安全 vs 集中声明 |
| 嵌入式 vs 外置容器 | 一致性 vs 容器级能力 |

### 常见坑/反模式

1. **异步不设超时**：AsyncContext 无 timeout → 线程/连接泄漏（docs 示例 `setTimeout(50L)` 示范）
2. **异步化不配套 trace 透传**：请求跨线程链路断裂（my-xhs MdcAwareExecutorService 示范）
3. **全路径都上不验证**：三路径盲目全开——不按瓶颈选（02 篇控制变量纪律）
4. **HTTP/2 无 TLS**：h2 需 ALPN/TLS（现代强制）——配置错降级回 1.1 不自知
5. **javax 参数照搬**：Servlet 5.0 起 jakarta.* ——`javax.servlet` 包在 Boot 3.x 不存在（迁移≠机制）
6. **嵌入式容器当黑盒**：RegistrationBean/SCI 差异不理解——第三方 WAR 组件失效（docs 参考材料二：Boot 刻意不执行 SCI）

### 生态位置

- **stage-3 教学主线**：容器/服务组（05-10）——05 容器参数 → 06 微服务化 → 07/08 注册中心 → **09 HTTP 架构升级（本篇）** → 10 RPC 架构升级；**09 收 HTTP 栈，10 转 RPC 栈**
- **前后篇衔接**：stage-1 03/04（Spring MVC REST——本篇 DispatcherServlet 是其后端基础）→ 本篇（异步/非阻塞/HTTP2 升级）→ 05 篇（Tomcat 参数 + WebFlux 网关）；10 篇（RPC——HTTP 之外的调用面）
- **与源码提取的关系**：spring-framework（webmvc/web）为机制源；my-xhs 为实例（jakarta/http2/WebFlux）

**架构师视角结论**：本篇以 **Servlet 规范机制**为知识本体（版本演进/生命周期/异步 AsyncContext/注册体系）、三路径（异步/非阻塞/HTTP2）为升级主线、my-xhs 实证（jakarta/http2/WebFlux）为现代落地——"规范机制时间无关、工具版本迁移标注"的典型篇目；HTTP 栈升级的完整纪律 = 按瓶颈选路径 + 改前基线 + 配套 trace 透传。
