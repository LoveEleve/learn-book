# W-1 DispatcherServlet — doDispatch 核心调度链

> 项目: Spring Framework 6.x | 🔴 Deep / 1 篇 | DispatcherServlet.java(1544行)
> 基线: Stage 2 spring-context — DispatcherServlet 是 HttpServlet Bean(由容器管理)

---

## §0.8

- 🟡 Working，1篇 — DispatcherServlet.doDispatch 核心调度链
- 设计模式: [模式: 前端控制器]—DispatcherServlet 统一接收所有HTTP请求 → 分发给对应 Handler → 视图解析

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DispatcherServlet.java:1049 | doDispatch() | **调度链**: ①getHandler(遍历HandlerMapping链→返回HandlerExecutionChain) ②getHandlerAdapter(匹配Handler的Adapter) ③preHandle(拦截器) ④ha.handle(执行业务Controller) ⑤postHandle ⑥processDispatchResult(异常/视图) | High |
| DispatcherServlet.java:1065 | getHandler() | 遍历 `this.handlerMappings` → `mapping.getHandler(request)` → HandlerExecutionChain(含 HandlerMethod + Interceptors) | High |
| DispatcherServlet.java:1072 | getHandlerAdapter() | 遍历 `this.handlerAdapters` → `adapter.supports(handler)` → 返回匹配的 HandlerAdapter(如 RequestMappingHandlerAdapter→@RequestMapping方法) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 1544行/1文件 — 核心是 doDispatch 调度链(30行方法) + getHandler/getHandlerAdapter 辅助。1篇(~45行)覆盖完整调度链。

**P1 核心 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | doDispatch 6步调度链 — getHandler→preHandle→getHandlerAdapter→handle→postHandle→processDispatchResult—render | 🔴 | **为什么**: Spring MVC的"发动机"—所有HTTP请求经此管道:URL→Handler→执行→视图 |
| P1-2 | HandlerMapping 策略链—遍历返回第一个非null Mapping | 🔴 | **为什么**: 多个Mapping共存(注解式/XML式/静态资源)—遍历链找到第一个能处理请求的→返回HandlerMethod |
| P1-3 | HandlerAdapter 匹配—supports(handler)→处理不同类型Handler | 🔴 | **为什么**: HandlerMethod(注解) vs HttpRequestHandler(接口) vs Servlet(原生)—不同Handler需要不同Adapter→策略模式分离 |

**单篇结构**: §1 doDispatch 6步调度链 → §2 HandlerMapping/HandlerAdapter 双链策略 → §3 HandlerInterceptor 拦截器(pre/post/afterCompletion)
