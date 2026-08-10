# stage-1 · 第 17 节：基于 Java 应用层追踪服务链路 — 知识点提取

> 课程：stage-1 服务治理 第 17 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/17. 第十七节：基于 Java 应用层追踪服务链路.md`
> 提取时间：2026-08-09 | 权重：核心（链路追踪是可观测性核心）
> **参考实现说明**：docs 用 Spring Cloud Sleuth，按方法论 04，**Sleuth 已并入 Micrometer Tracing**（`code/spring/micrometer-tracing` 有源码，本地无 spring-cloud-sleuth），参考实现以 Micrometer Tracing 为主。

---

## 一、本节概览

- **技术域**：服务链路追踪（Tracer/Span/Trace ID 上下文传递/框架整合）
- **维度**：`[分布式问题]`（可观测性/链路）+ `[工程问题]`（框架整合扩展点）
- **核心命题**：如何追踪一次请求跨多个服务的完整调用链路（Trace ID 传播、Span 生命周期、框架自动埋点）
- **知识点数**：7 个
- **前置**：Micrometer(第13节)、扩展点机制(第9节)、分布式概念

## 前置条件清单
读者需先掌握：
1. **分布式调用概念**（服务间调用、HTTP/RPC）
2. **Micrometer 门面**（第 13 节，Tracing 是它的一部分）
3. **扩展点机制**（第 9 节，框架整合）
4. **可观测性**（第 13/15/16 节指标）
未达前置者，先补：链路追踪概念 + 第 13 节 Micrometer

## 掌握度
目标读者：**本人（读源码多，Spring/监控熟悉）** — 已确认
讲解策略：Tracer/Span + 框架整合直接讲（你熟悉）；补 Trace ID 传播机制

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 服务链路追踪的需求与本质（Trace ID/Span）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：分布式调用概念
- **需求**：一次请求跨多个服务，如何串联全部调用链，定位问题
- **自主实现**：用 Trace ID(整条链唯一) + Span(每个调用) 串联，Trace ID 跨服务传递
- **参考实现**：Sleuth/Micrometer Tracing 用 **Trace ID(链路)** + **Span(单次调用)** 模型；Tracer.nextSpan() 创建 Span
- **对比取舍**：Trace ID 是整条链的"身份证"，Span 是链上每一跳；通过父子 Span 形成调用树
- **测试佐证**：`micrometer-tracing` 的 `Tracer.nextSpan()` / `Span`

### KP-02 核心 API（Tracer/Span 生命周期）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **需求**：用 Tracer/Span API 手动创建、标记、结束 Span
- **自主实现**：`tracer.nextSpan().name(...)` 创建 → `tag()/event()` 标记 → `end()` 结束（try/finally）
- **参考实现**（已源码验证）：micrometer-tracing `Tracer`（nextSpan/withSpanInScope/currentSpan）+ `Span`（name/event/tag/end）；docs 代码示例 try(Tracer.SpanInScope ws=tracer.withSpan(initialSpan)) { newSpan=nextSpan().name(...); newSpan.tag(...); newSpan.event(...); } finally { newSpan.end(); }
- **对比取舍**：Span 生命周期=创建→标记→结束；用 try-with-resources/finally 保证结束
- **测试佐证**：`code/spring/micrometer-tracing` 的 Tracer.java / Span.java

### KP-03 Trace ID 上下文传递（HTTP/RPC 头）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、HTTP/RPC
- **需求**：Trace ID 跨服务传播，串联调用链
- **自主实现**：Trace ID 作为元数据，通过调用头传递（HTTP header / RPC 上下文）
- **参考实现**（docs）：Trace ID 通过上下文传递——HTTP 用请求头、RPC 用 RPC 上下文（Dubbo RpcContext）；标准如 W3C traceparent
- **对比取舍**：**传递机制**——HTTP header(标准)、RPC 上下文(Dubbo RpcContext)；Trace ID 传播是串联全链的关键
- **测试佐证**：docs 明确 HTTP 请求头 / Dubbo RpcContext

### KP-04 日志扩展（Trace ID 关联日志）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：日志、链路
- **需求**：日志里带上 Trace ID，定位问题时按链路查日志
- **自主实现**：把 Trace ID 注入日志（MDC），日志带 traceId
- **参考实现**：Sleuth/Micrometer Tracing 集成日志（Logback/Log4j2 MDC 注入 traceId/spanId）
- **对比取舍**：Trace ID 关联日志是"链路 + 日志"联动的关键
- **待验证**：具体 MDC 注入实现

### KP-05 框架自动埋点（HTTP Client/Server/Feign/WebMVC）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：扩展点机制（第 9 节）
- **需求**：自动为常见框架埋点，无需手写
- **自主实现**：用框架扩展点（HandlerInterceptor/Filter/Feign Client 包装）自动创建/传播 Span
- **参考实现**（docs）：
  - HTTP Client：`HttpClientHandler`
  - HTTP Server：`HttpServerHandler`
  - Feign：`TracingFeignClient`（实现 feign.Client，Wrapper 拦截）
  - WebMVC：`SpanCustomizingHandlerInterceptor`（HandlerInterceptor）
  - Servlet：`TracingFilter`（Filter）
  - Tomcat：`TraceValve`（Valve）
- **对比取舍**：用各框架扩展点自动埋点（第 9 节扩展点机制的应用）；Feign 用 Client Wrapper
- **测试佐证**：docs 给出各整合类名（HttpClientHandler/HttpServerHandler/TracingFeignClient/SpanCustomizingHandlerInterceptor/TracingFilter/TraceValve）

### KP-06 TraceValve vs TracingFilter（Span 创建时机）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-05、Servlet 容器
- **需求**：理解 Span 创建时机（Tomcat Valve 早于 Filter）及两者关系
- **自主实现**：选合适的埋点层（Valve 最早/Filter 次之/Interceptor 方法级）
- **参考实现**（docs）：**TraceValve 存在意义**——提早(早于任何 Filter)创建 Span，避免自定义 Filter 在 TracingFilter 前独立创建 Span 破坏逻辑；TracingFilter 用 Servlet Filter API(容器透明)；TraceValve+TracingFilter 同时存在时 TracingFilter 用 TraceValve 的 TraceContext；非 Tomcat 容器 TracingFilter 独立创建
- **对比取舍**：**埋点层级**——Valve(Tomcat 最早) vs Filter(容器透明) vs Interceptor(方法级)；层级影响 Span 创建时机
- **测试佐证**：docs 详细讲 TraceValve 与 TracingFilter 的关系

### KP-07 第三方整合（Span API 接入 MyBatis/Redis）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：扩展点、Span API
- **需求**：把链路追踪接入 MyBatis/Redis 等
- **自主实现**：用 Span API + 框架扩展点（MyBatis Interceptor/Redis 拦截）埋点
- **参考实现**（docs）：用 Span API 整合第三方——MyBatis(Interceptor/Plugin)、Redis(RedisConnection)、JDBC(P6Spy)；可拦截框架：WebMVC/WebFlux/OpenFeign/JDBC(Wrappe r-P6Spy)/MyBatis/Redis
- **对比取舍**：与第 9 节扩展点整合一致——用框架扩展点织入 Span；可拦截框架列表是埋点覆盖面
- **关联 microsphere**：`[待验证]` microsphere-observability 是否有 tracing 整合

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| 链路需求与本质 | 分布式 | 核心 | P1 | 🔴 | High |
| Tracer/Span API | 工程 | 核心 | P1 | 🔴 | High |
| Trace ID 传递 | 分布式 | 核心 | P1 | 🔴 | High |
| 日志扩展 | 工程 | 核心 | P1 | 🟡 | Medium |
| 框架自动埋点 | 工程 | 核心 | P1 | 🔴 | High |
| TraceValve vs Filter | 工程 | 核心 | P1 | 🔴 | High |
| 第三方整合 | 工程 | 核心 | P1 | 🟡 | High |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **Tracer/Span**：`code/spring/micrometer-tracing` 的 Tracer.java / Span.java（已源码验证，nextSpan/withSpanInScope/name/event/tag/end）
- **Sleuth 过时**：本地无 spring-cloud-sleuth（已并入 micrometer-tracing，04 标过时）
- **microsphere-observability**：有 Micrometer Metrics binder，无专门 Tracing（`[待验证]`）
- **Tracing 是 Micrometer(第13节门面)的一部分**

---

## 五、本节小结（三层次视角）

**需求**：追踪一次请求跨多个服务的完整链路（Trace ID 传播、Span 生命周期、框架自动埋点），串联日志/指标定位问题。

**自主实现核心**：若我设计——
1. Trace ID + Span 模型（Trace ID 整链唯一，Span 每跳）
2. Trace ID 跨服务传播（HTTP header / RPC 上下文）
3. Tracer/Span API 手动埋点 + 框架扩展点自动埋点
4. 埋点层级选择（Valve/Filter/Interceptor）
5. 与日志(MDC)/指标(第 13 节)联动

**参考实现**：Micrometer Tracing（code/spring/micrometer-tracing 有源码）——Tracer/Span 已源码验证。**Sleuth 已并入 Micrometer Tracing（04 标过时）**。框架整合：HttpClientHandler/TracingFilter/TracingFeignClient/SpanCustomizingHandlerInterceptor/TraceValve。

**对比取舍**：知识本体是"**服务链路追踪**"（Trace ID/Span/传播/埋点）。核心洞察：**Trace ID 跨服务传播 + Span 父子树 + 框架自动埋点**；埋点层级(Valve/Filter/Interceptor)影响 Span 创建时机。

**待验证汇总**：
- 日志 MDC 注入具体实现
- microsphere-observability 是否有 tracing 整合

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：本节大部分为架构师发散；与 docs/前篇重复处已交叉引用。

### 完整认知：服务链路追踪在真实架构中完整该讲什么

docs 覆盖了"Sleuth Tracer/Span + 框架整合"。作为架构师，这个主题完整还该包含：

1. **链路追踪的完整模型**：不只 Trace ID/Span，而是**完整模型**——Trace（整条链）、Span（每一跳）、SpanContext、父子关系、Baggage（跨链传递的业务元数据）、采样（Sampler）——OpenTelemetry/W3C 标准（traceparent）
2. **Trace ID 传播协议**：不只"HTTP 头"，而是**标准协议**——W3C traceparent/tracestate、B3（Zipkin）——跨框架/跨语言互操作
3. **采样策略**：全量采样(开销大) vs 概率采样 vs 动态采样——链路追踪的**成本控制**（高流量下全量采样存储爆炸）
4. **埋点覆盖与边界**：不只常见框架，而是**覆盖全链路**（HTTP/RPC/MQ/DB/缓存）——埋点不全则链路断裂
5. **与指标/日志的联动（可观测三支柱）**：链路(Trace) + 指标(Metrics，第 13 节) + 日志(Logs，KP-04 MDC)——三支柱关联定位
6. **追踪后端**：Zipkin/Jaeger/OpenTelemetry Collector——Span 上报到哪、如何存储/查询
7. **性能影响**：埋点/传播/上报本身有开销——采样 + 低开销传播

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| Sleuth vs Micrometer Tracing | Sleuth(旧，并入 Micrometer Tracing)；Micrometer Tracing(现代，与 Metrics 统一门面)——用现代 |
| 全量 vs 采样 | 全量准但存储/开销大；采样省但丢部分链路——按流量/排查需求 |
| 埋点层级 | Valve(最早)/Filter(容器透明)/Interceptor(方法级)——层级影响 Span 创建时机(见 KP-06) |
| 单后端 vs 多后端 | 直接 Zipkin(简单)；OpenTelemetry Collector(多后端标准，灵活) |
| 传播协议 | W3C traceparent(标准)/B3(Zipkin)——按生态 |

### 常见坑/反模式

1. **Trace ID 断裂**：某个框架没埋点/没传播，链路断——要覆盖全链路 + 验证传播
2. **Span 未结束**：忘调用 end()/异常未 finally，Span 泄漏——用 try-with-resources/finally（见 KP-02）
3. **全量采样撑爆存储**：高流量全量采样，追踪存储爆炸——要采样
4. **埋点层级破坏 Span**：自定义 Filter 在 TracingFilter 前独立创建 Span 破坏逻辑（docs KP-06 的 TraceValve 场景）——注意埋点顺序
5. **跨线程传播丢失**：异步/线程池里没传播 Trace Context，Span 丢失父子关系——要手动传播
6. **只追踪不查**：采集了但没接后端(Zipkin)/没查询，追踪没价值——要完整闭环(采集+存储+查询)
7. **性能开销**：埋点/上报拖累业务——低开销传播 + 采样

### 生态位置

- **可观测三支柱之一（链路）**：与指标(Metrics，第 13 节)、日志(Logs)并列；第 18 节(Sleuth 重构到 Micrometer Tracing)承接
- **衔接**：第 13 节(Micrometer 门面，Tracing 是其一部分)、第 18 节(Java Instrument 重构链路)、第 9 节(扩展点埋点)
- **Micrometer Tracing**：`code/spring/micrometer-tracing` 有源码；Sleuth 已并入(过时)
- **与指标/日志联动**：可观测三支柱协同定位问题

**架构师视角结论**：本篇不只是"用 Sleuth 创建 Span"，而是"**设计完整的链路追踪体系**"——Trace/Span 模型、Trace ID 传播协议、采样、埋点覆盖、三支柱联动、追踪后端，是分布式系统排障的核心能力。
