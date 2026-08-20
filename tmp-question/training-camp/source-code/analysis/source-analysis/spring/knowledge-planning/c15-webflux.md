# C-15 WebFlux — 响应式 Web (DispatcherHandler → RouterFunction → WebClient)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | DispatcherHandler(224行)+RouterFunctions(1433行)+RouterFunction(162行)+HandlerFunction(39行)+WebClient(990行)
> 基线: C-14 结尾桥 — 从 MVC(阻塞 Servlet)到响应式 — 同一套 Spring 的响应式 Web 栈; 原始执行计划 7-B

---

## §0.8

- 🟡 Working，1篇 — 响应式前端控制器(DispatcherHandler: Flux 组合 concatMap/next/switchIfEmpty→handleRequestWith) → 函数式路由(RouterFunction: route(predicate, handler) + andRoute 组合; HandlerFunction.handle→Mono<ServerResponse>) → 响应式客户端(WebClient: get/post/retrieve/exchangeToMono/bodyToMono) → 与 MVC 对照(DispatcherServlet vs DispatcherHandler, @RequestMapping vs RouterFunction, RestTemplate vs WebClient)
- 设计模式: [模式: 函数式路由]—RouterFunction 组合; [模式: 响应式流]—Mono/Flux 声明式组合; [模式: 前端控制器]—DispatcherHandler 同 DispatcherServlet 角色

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DispatcherHandler.java:72,142 | 响应式控制器 | **handle(exchange)**: L142: Flux.fromIterable(handlerMappings).concatMap(getHandler).next().switchIfEmpty(404).onErrorResume(handleResultMono).flatMap(handleRequestWith) — 全响应式链, 无阻塞 | High |
| DispatcherHandler.java:75,81 | 组件 | **handlerMappings**/handlerAdapters/resultHandlers — 与 MVC 同构(映射→适配→结果) | High |
| RouterFunctions.java:65,96,115 | 路由工厂 | **RouterFunctions.route()**: L115 route(predicate, handlerFunction)→DefaultRouterFunction; route() builder L96; 组合: andRoute(L132)/nest(嵌套) | High |
| HandlerFunction.java:30,37 | 处理函数 | **HandlerFunction**: handle(ServerRequest)→Mono<ServerResponse> — 纯函数, 无控制器类 | High |
| RouterFunction.java(162行) | 路由接口 | **route 判定**: matches(request)→Mono<HandlerFunction> — 谓词匹配+组合 | High |
| WebClient.java:80,86,145 | 响应式客户端 | **WebClient**: get() L86/post() L98; create() L145(DefaultWebClientBuilder); 调用: retrieve()(简单)/exchangeToMono(Function)(完整控制)/bodyToMono/bodyToFlux | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 控制器+路由+客户端约 2800 行 — 知识主线: "DispatcherHandler 响应式分派 → RouterFunction 函数路由 → WebClient 客户端". 1篇 (~46行) 按"分派→路由→客户端→对照"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | DispatcherHandler 响应式分派链 (Flux.concatMap/next/switchIfEmpty/flatMap) | 🔴 | **为什么🔴**: 响应式前端控制器核心 — 与 MVC DispatcherServlet 对照的骨架 |
| P1-2 | RouterFunction + HandlerFunction (函数式路由声明) | 🔴 | **为什么🔴**: WebFlux 函数式端点 — 路由即代码, 无注解 |
| P1-3 | WebClient 响应式客户端 (retrieve/exchangeToMono/bodyToMono) | 🔴 | **为什么🔴**: 非阻塞 HTTP 客户端 — 与 RestTemplate 对照 |
| P2-1 | 路由组合 (andRoute/nest/谓词) | 🟡 | **为什么🟡**: 复杂路由的组织方式 |
| P2-2 | 响应式 vs MVC 组件对照 (DispatcherHandler/Mapping/RestTemplate) | 🟡 | **为什么🟡**: 两套栈的映射关系, 迁移视角 |
| P3-1 | Mono/Flux 声明式组合 (响应式流) | 🟢 | **为什么🟢**: 响应式底层的抽象 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **响应式分派** (DispatcherHandler Flux 链) | 🔴 | 响应式请求处理核心 |
| B | **函数路由** (RouterFunction + HandlerFunction) | 🔴 | 无注解路由模型 |
| C | **客户端与对照** (WebClient + MVC 对比) | 🟡 | 使用与迁移 |

> **Cluster A (§1)**: DispatcherHandler(Flux 组合分派) + 组件(handlerMappings/adapters/resultHandlers)
> **Cluster B (§2)**: RouterFunction(route/andRoute) + HandlerFunction(handle→Mono)
> **Cluster C (§3)**: WebClient(get/post/retrieve/exchange) + 三对对照(MVC vs Flux)

→ 至此 spring-web/webmvc + spring-core + spring-expression + 补缺 15 域全部完成 — 后续 Stage 7: Spring Boot 自动装配 (24域)

(End of file - total 61 lines)
