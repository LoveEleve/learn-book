# stage-3 · 第 13 节：[公开课] 加餐一：Spring Web Reactive — 知识点提取

> 课程：stage-3 三高架构 第 13 节（加餐/事件组 13-17 第一篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/13. [公开课] 加餐一：Spring Web Reactive.md`
> 提取时间：2026-08-12 | 权重：核心（Reactive Web——**docs 完全空节**（19 行仅 3 标题），知识本体 = 架构师发散 + 源码实证（08 §2 极限案例））
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）

---

## 一、本节概览

- **技术域**：Spring Web 三模块架构（web/webmvc/webflux）、异步 Web、Reactive Web（WebFlux/Reactor/函数式端点）
- **维度**：`[工程问题]`（Spring Web 栈架构）
- **核心命题**：**docs 加餐一为完全空节（仅 3 个标题）**——知识本体全部来自架构师发散 + spring-framework 源码实证 + my-xhs WebFlux 网关实例；与 09 篇（HTTP 升级）互补：09 讲 Servlet 异步，本篇讲 Reactive 栈
- **知识点数**：4 个
- **前置**：09 篇（Servlet 异步/HTTP 升级）、stage-1 03/04（Spring MVC）

## 前置条件清单
读者需先掌握：
1. **Servlet 异步机制**（09 篇 KP-04/05：AsyncContext/DeferredResult——本篇异步部分交叉引用）
2. **Spring MVC 请求处理链**（stage-1 03/04：DispatcherServlet）
3. **HTTP/2 与性能升级**（09 篇 KP-08）
未达前置者，先补：09 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **源码实证**：spring-framework 三模块（web/webmvc/webflux）关键类
- **实例锚定**：my-xhs gateway 是 WebFlux（注释实证"基于 WebFlux 的响应式网关"）
- **docs 空节处理**：标题 → 发散补全（08 §2：内容越少补全越重要——本篇为极限案例）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Spring Web 三模块架构（spring-web/webmvc/webflux 分工）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：Spring 基础
- **来源**：docs 标题① + spring-framework 源码实证 + 架构师发散
- **需求**：理解 Spring Web 的**模块分层**——基础层（spring-web）与两栈（webmvc/webflux）的关系
- **自主实现**：若我设计——基础层放通用（消息转换/Web 客户端/HTTP 抽象），两栈分别实现阻塞（Servlet）与非阻塞（Reactive）请求处理
- **参考实现**（源码实证 + 发散）：**三模块（源码实证）**——`spring-web`（基础：WebHandler/HttpMessageConverter/ClientHttpRequestFactory 等通用抽象）+ `spring-webmvc`（Servlet 栈：DispatcherServlet——09 篇已提取）+ `spring-webflux`（Reactive 栈：DispatcherHandler/WebHandler）+ `spring-websocket`；**联系与区别（docs 标题①意图 + 发散）**——共同基础层（spring-web：消息转换/Web 客户端/URI 工具）；**webmvc vs webflux**——阻塞 Servlet 线程模型 vs 非阻塞事件循环（Netty）——API 同源（注解控制器共用），运行时不同；**底层 API（docs 意图）**——WebHandler（Reactive 请求处理链基础接口，源码实证 `spring-web/.../web/server/WebHandler.java`）+ DispatcherHandler（WebFlux 前端控制器，`spring-webflux/.../reactive/DispatcherHandler.java`）
- **对比取舍**：**两栈同 API 双运行时**——迁移成本低 vs 运行时语义差异（线程模型/背压）；**"Reactive 不是更快，是更省线程"**（09 篇三路径衔接）
- **测试佐证**：spring-framework `spring-web/.../WebHandler.java` + `spring-webflux/.../DispatcherHandler.java` + 模块目录（web/webmvc/webflux/websocket）

### KP-02 异步 Web（WebMVC 异步——交叉引用 09 篇）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：09 篇 KP-04/05
- **来源**：docs 标题② + 交叉引用
- **需求**：docs 标题②"Spring 异步 Web"——**与 09 篇重复内容，交叉引用不重复提取**
- **自主实现**：若我设计——WebMVC 异步 = Servlet 3.0 AsyncContext 的 Spring 封装（DeferredResult/Callable/CompletionStage）
- **参考实现**（交叉引用）：**09 篇 KP-04（Servlet 异步机制：AsyncContext/startAsync/AsyncListener）+ KP-05（Spring MVC 三形态：DeferredResult/Callable/CompletionStage + StandardServletAsyncWebRequest）**——已完整提取，不重复；**本篇衔接点**——WebMVC 异步是"Servlet 栈内优化"，WebFlux 是"换栈"——**两条异步路径的选型**（渐进 vs 重构）
- **对比取舍**：**WebMVC 异步（保持 Servlet 栈）vs WebFlux（换栈）**——渐进成本低 vs 全异步彻底——按存量/新服务选（my-xhs：网关新栈 WebFlux，业务存量 WebMVC）
- **测试佐证**：09 篇 KP-04/05（交叉引用）+ my-xhs（gateway WebFlux / 业务 WebMVC 混合实证）

### KP-03 Reactive Web 架构（WebFlux：DispatcherHandler/Reactor/Netty）【docs 标题③】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：Reactor/响应式概念
- **来源**：docs 标题③ + spring-framework 源码实证 + my-xhs gateway 实证
- **需求**：掌握 **Reactive Web 的处理模型**——非阻塞事件循环 + 响应式流（docs 标题③："WebMVC 与 WebFlux 在 Reactive Web 的设计"）
- **自主实现**：若我设计——请求处理链（WebHandler 链）→ 前端控制器（DispatcherHandler）→ 控制器返回 Publisher → 背压传播 → Netty 事件循环写回
- **参考实现**（源码实证 + my-xhs + 发散）：**处理模型**——`WebHandler`（基础链接口，spring-web 实证）+ `DispatcherHandler`（WebFlux 前端控制器，`spring-webflux/.../DispatcherHandler.java` 实证——Reactive 版的 DispatcherServlet，映射 handler/适配/结果处理全异步）；**运行时**——Netty（默认）+ Reactor（Mono/Flux 响应式流）；**与 WebMVC 对照（docs 标题③意图）**——注解控制器 API 相似，但**执行模型不同**：WebMVC 每请求一线程（Servlet 线程池），WebFlux 事件循环（少量线程处理海量连接——09 篇 KP-08"非阻塞=IO 层"的落地）；**my-xhs 实证**——`GatewayApplication.java:12` 注释"**Gateway 是基于 WebFlux 的响应式网关，不使用传统 WebMVC 和 JDBC**" + `CachingFilteringWebHandler implements WebHandler`（05 篇已证）——**网关层已采用 Reactive 栈**；**设计注意（发散）**——WebFlux 中阻塞调用（JDBC/同步 IO）会阻塞事件循环线程 → **阻塞操作必须换线程池/用响应式驱动**（my-xhs 注释"不使用 JDBC"即此纪律）
- **对比取舍**：**WebFlux vs WebMVC**——高连接/IO 密集优势 vs 阻塞生态兼容（JDBC 阻塞）——**网关/IO 密集选 WebFlux，DB 密集业务可留 WebMVC**（my-xhs 混合架构实证）
- **测试佐证**：spring-webflux `DispatcherHandler.java` + spring-web `WebHandler.java` + my-xhs `GatewayApplication.java:12`（注释实证）+ `CachingFilteringWebHandler.java`

### KP-04 函数式端点（RouterFunction/HandlerFunction）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：函数式编程、RouterFunction 概念
- **来源**：docs 标题③（函数式编程整合）+ spring-framework 源码实证
- **需求**：理解 **WebFlux 的函数式路由模型**（docs 标题③末句"函数式编程的整合"）——注解之外的第二形态
- **自主实现**：若我设计——RouterFunction（路由表：谓词 → 处理器）替代注解映射；HandlerFunction 处理请求返回 Mono/Flux
- **参考实现**（源码实证 + 发散）：**两个 RouterFunction（源码实证）**——`spring-webflux/.../function/server/RouterFunction.java`（Reactive 版：路由 → HandlerFunction）+ `spring-webmvc/.../function/RouterFunction.java`（**WebMVC 也有函数式**——同步版）——**函数式端点是两栈共有的第二形态**；**模型**——RouterFunction 匹配（路径/方法/谓词）→ HandlerFunction（ServerRequest → Mono<ServerResponse>）；**对比注解**——路由集中可组合（RouterFunctions.route().GET()...）vs 注解声明分散——**网关/API 组合场景友好**
- **对比取舍**：**注解 vs 函数式**——声明式分散 vs 组合式集中——函数式适合动态路由/网关场景（Spring Cloud Gateway 的 RouteLocator 即函数式风格 `[待验证：my-xhs gateway 路由方式]`）
- **测试佐证**：spring-webflux `function/server/RouterFunction.java` + spring-webmvc `function/RouterFunction.java`

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Spring Web 三模块架构 | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| 异步 Web（交叉引用 09 篇） | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| Reactive Web 架构（WebFlux） | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| 函数式端点（RouterFunction） | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：spring-framework（三模块）+ my-xhs（WebFlux 网关）
- **关键源码**（本次实证）：
  - `spring-web/.../web/server/WebHandler.java`（Reactive 请求处理基础接口）
  - `spring-webflux/.../DispatcherHandler.java`（WebFlux 前端控制器）+ `spring-webflux/.../function/server/RouterFunction.java`（Reactive 函数式）
  - `spring-webmvc/.../function/RouterFunction.java`（WebMVC 函数式——两栈共有）
  - my-xhs `GatewayApplication.java:12`（注释"基于 WebFlux 的响应式网关，不使用传统 WebMVC 和 JDBC"）+ `CachingFilteringWebHandler implements WebHandler`（05 篇）
- **诚实标注**：docs 为**完全空节**（19 行仅 3 标题）——知识本体全部为架构师发散 + 源码实证（08 §2 极限案例：内容为 0，补全即全部）；docs 标题②（异步 Web）与 09 篇重复 → 交叉引用不重复提取
- **关联标注**：09 篇（Servlet 异步/HTTP 三路径——本篇 Reactive 是"非阻塞"路径的栈级答案）；05 篇（gateway WebFlux/线程差异化）；12 篇（WebFlux 不配 JDBC 的架构纪律呼应）

---

## 五、本节小结（三层次视角）

**需求**：Spring Web 栈架构——三模块分工、异步 Web、Reactive Web、函数式端点（docs 空节，意图=理解 WebMVC 与 WebFlux 的关系）。

**自主实现核心**：若我设计——①基础层（spring-web）与两栈（webmvc 阻塞/webflux 非阻塞）②WebMVC 异步（渐进）vs WebFlux（换栈）选型 ③WebFlux：事件循环 + 阻塞操作换线程 ④函数式端点（RouterFunction）作为组合路由形态。

**参考实现**：docs（3 标题）+ **spring-framework 源码实证**（WebHandler/DispatcherHandler/双 RouterFunction）+ **my-xhs 实证**（gateway WebFlux 注释）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**Spring Web 两栈架构**"——同 API 双运行时、异步两条路径（渐进/换栈）、Reactive 的核心纪律（不阻塞事件循环）；my-xhs 的混合架构（WebFlux 网关 + WebMVC 业务）是 docs 意图的工程答案。

**待验证汇总**：
- my-xhs gateway 的路由方式（RouteLocator 函数式 vs 配置）——19 节网关展开
- WebFlux 阻塞调用治理（业务服务若转 Reactive 的评估）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 目标（知识主题） | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| Reactive Web（WebFlux） | ✅ **网关层已采用**：GatewayApplication:12 注释"基于 WebFlux 的响应式网关" + WebHandler 实现（05 篇） | 无（入口层 Reactive 已落地） |
| Spring Web 三模块 | ✅ spring-web 为基础（WebFlux/WebMVC 共用），my-xhs 两栈并存（gateway webflux / 业务 webmvc） | 无（混合架构合理） |
| 异步 Web（WebMVC 异步） | ⚠️ 业务服务无 DeferredResult/Callable（CompletableFuture 风格，09 篇核对） | 09 篇差距清单 P2（控制器层 MVC 异步出口评估） |
| 函数式端点 | ⚠️ gateway 路由方式未核（RouteLocator 函数式 vs 配置） | `[待验证]`：19 节网关核对 |

**结论**：13 篇（空节篇）核对——my-xhs 的 Reactive 落地在**网关层**（入口非阻塞），业务服务保留 WebMVC（阻塞）+ 异步任务（CompletableFuture）——与 09 篇差距清单一致（控制器层异步出口为 P2 待评估项）。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 完全空节（仅 3 标题）；知识本体 = 架构师发散 + spring-framework/my-xhs 实证；"docs 明确内容"vs"架构师发散"如下（本篇 100% 发散，08 §2 极限案例）。

### 完整认知：Spring Web Reactive 的完整认知该讲什么

docs 只有 3 个标题。完整还该包含：

1. **"Reactive 不是更快，是更省线程"**（核心纪律）：WebFlux 的事件循环模型——少量线程扛海量连接——**IO 密集/高连接场景收益大；CPU 密集场景无收益**；09 篇 KP-08 三路径（异步/非阻塞/HTTP2）中"非阻塞"的栈级答案
2. **阻塞调用是 WebFlux 的第一杀手**（发散 + my-xhs 注释实证）：JDBC/同步 IO 阻塞事件循环线程 → **整个网关卡死**——my-xhs"不使用传统 WebMVC 和 JDBC"注释即此纪律；**业务服务转 WebFlux 的前提 = 数据层响应式化**（R2DBC/Mongo 响应式驱动——12 篇 MyBatis 阻塞栈的对照）
3. **两栈并存是务实架构**（my-xhs 实证）：网关（入口，IO 密集）WebFlux + 业务（DB 密集）WebMVC + 异步任务（CompletableFuture）——**不搞"全栈 Reactive"运动**，按层按场景选
4. **函数式端点是"路由组合"的第二形态**（发散）：RouterFunction 集中可组合 vs 注解分散——**网关/API 聚合场景**（Spring Cloud Gateway RouteLocator 函数式风格）；两栈都有（源码实证双 RouterFunction）
5. **异步路径的选型树**（发散）：存量 WebMVC → WebMVC 异步（DeferredResult，渐进）；新网关/高连接服务 → WebFlux（换栈）；**DB 密集核心链路 → 谨慎**（阻塞驱动缺位）
6. **与 09 篇 HTTP 升级的关系**（衔接）：09 的三路径（线程/IO/协议）+ 本篇的栈选型——**HTTP 架构升级的完整决策面**

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| WebMVC vs WebFlux | 阻塞生态兼容 vs 高连接省线程 |
| WebMVC 异步 vs WebFlux | 渐进 vs 彻底（按存量/新服务） |
| 事件循环 vs 线程池 | 省线程 vs 阻塞调用会卡死 |
| 注解 vs 函数式路由 | 分散声明 vs 集中组合 |
| 全栈 Reactive vs 混合 | 一致性 vs 务实（my-xhs 混合实证） |

### 常见坑/反模式

1. **WebFlux 里调阻塞 API**（最大坑）：JDBC/Thread.sleep 阻塞事件循环——整站卡死（my-xhs"不使用 JDBC"纪律）
2. **DB 业务强转 WebFlux**：数据层仍阻塞 → 换栈无收益还复杂（12 篇对照）
3. **把 Reactive 当性能银弹**：CPU 密集场景无收益（省线程≠更快）
4. **背压忽略**：无限请求堆积内存（响应式流背压纪律）
5. **两栈混用同一事务模型**：WebFlux 无 ThreadLocal 事务（响应式事务不同语义）——跨栈知识迁移坑
6. **函数式与注解混用无纪律**：路由散两处——按团队约定统一

### 生态位置

- **stage-3 教学主线**：加餐/事件组（13-17）开篇——13 Reactive Web（栈认知）→ 14 分布式事件设计 → 15（缺失）→ 16 分布式事件 → 17 Reactive 异步服务；**13 为 16/17 篇的栈基础**
- **前后篇衔接**：09 篇（HTTP 三路径/Servlet 异步）→ 本篇（Reactive 栈）→ 16/17 篇（分布式事件/Reactive 异步服务深化）；05 篇（gateway WebFlux 实证）；12 篇（MyBatis 阻塞栈对照）
- **与源码提取的关系**：spring-framework（web/webmvc/webflux）为机制源；my-xhs 网关为实例

**架构师视角结论**：本篇为 **docs 空节极限案例**（19 行 3 标题）——知识本体 100% 来自发散 + 源码实证：Spring Web 两栈架构、Reactive 的"省线程"本质与"阻塞纪律"、异步两条路径选型、函数式端点形态；my-xhs 的混合架构（WebFlux 网关 + WebMVC 业务）是 docs 标题①③意图的工程答案，为 16/17 篇（分布式事件/Reactive 异步服务）铺路。
