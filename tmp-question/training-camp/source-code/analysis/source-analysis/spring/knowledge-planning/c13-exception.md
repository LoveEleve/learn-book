# C-13 异常处理 — HandlerExceptionResolver (@ControllerAdvice → 解析链 → 兜底)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | HandlerExceptionResolver(56行)+AbstractHandlerExceptionResolver(312行)+ExceptionHandlerExceptionResolver(574行)+ExceptionHandlerMethodResolver(300行)+DefaultHandlerExceptionResolver(709行)+ControllerAdviceBean(274行)
> 基线: C-12 拦截器结尾桥 — 处理器/拦截器抛的异常进 processDispatchResult→processHandlerException — 本域展开异常解析链; 原始执行计划 7-6

---

## §0.8

- 🟡 Working，1篇 — 入口(processDispatchResult→processHandlerException→handlerExceptionResolvers 链, DispatcherServlet L696 排序) → 接口与模板(HandlerExceptionResolver.resolveException + AbstractHandlerExceptionResolver: shouldApplyTo→doResolveException) → 注解驱动(ExceptionHandlerExceptionResolver: @ControllerAdvice 缓存 + ExceptionHandlerMethodResolver 按异常类型匹配) → 兜底(DefaultHandlerExceptionResolver: 标准异常→状态码, LOWEST 顺序)
- 设计模式: [模式: 责任链]—handlerExceptionResolvers 逐个尝试; [模式: 模板方法]—shouldApplyTo/doResolveException; [模式: 策略]—各 resolver 负责一类异常

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HandlerExceptionResolver.java:36,53 | 接口 | **接口契约**: resolveException(request, response, handler, ex)→ModelAndView(可 null) | High |
| DispatcherServlet.java:694,696 | 链装配 | **排序链**: 收集容器中 HandlerExceptionResolver bean → AnnotationAwareOrderComparator.sort(C-4) — 顺序即优先级 | High |
| DispatcherServlet.java:1161 | 触发点 | **processHandlerException**: 处理器异常→遍历 resolver → 首个返回非 null mv 即用(渲染错误视图) | High |
| AbstractHandlerExceptionResolver.java:177,182,212,309 | 模板 | **模板骨架**: resolveException L177: shouldApplyTo(request, handler)(L212, 判断是否适用)→doResolveException(抽象 L309) — 子类只写"怎么解析" | High |
| ExceptionHandlerExceptionResolver.java:85,309,314 | @ControllerAdvice | **advice 缓存**: initExceptionHandlerAdviceCache L309: ControllerAdviceBean.findAnnotatedBeans(L314) → 每个 advice 建 ExceptionHandlerMethodResolver(L320) 存 exceptionHandlerAdviceCache | High |
| ExceptionHandlerMethodResolver.java:60,164 | 方法匹配 | **异常→方法**: resolveMethod(exception)(L164)→按异常类型匹配 @ExceptionHandler 方法; mappedMethods 缓存(L80, 含 Cause 链) | High |
| DefaultHandlerExceptionResolver.java:155,181,174 | 兜底 | **标准异常**: doResolveException L181: 按异常类型 switch→设置状态码(如 NoHandlerFound→404, MethodArgumentNotValid→400); order=LOWEST_PRECEDENCE(L174) 兜底最后 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+模板+注解解析器+兜底约 2200 行 — 知识单线: "processHandlerException → resolver 链(advice 优先→标准兜底) → 渲染错误视图". 1篇 (~46行) 按"入口→模板→注解→兜底"展开; 若分 2 篇则 advice 匹配与兜底割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 异常解析链 (processHandlerException→resolver 遍历→首个非 null) | 🔴 | **为什么🔴**: 异常处理的入口骨架 — 责任链+排序是"谁处理这个异常"的决定机制 |
| P1-2 | ExceptionHandlerExceptionResolver (@ControllerAdvice 缓存 + 按异常类型匹配方法) | 🔴 | **为什么🔴**: @ExceptionHandler/@ControllerAdvice 的实现核心 — 实际开发最高频 |
| P1-3 | AbstractHandlerExceptionResolver 模板 (shouldApplyTo→doResolveException) | 🔴 | **为什么🔴**: 所有 resolver 的骨架 — 适用性判断与解析分离 |
| P2-1 | DefaultHandlerExceptionResolver (标准异常→状态码, LOWEST 兜底) | 🟡 | **为什么🟡**: 未自定义异常时的默认行为 — 404/400/500 从哪来 |
| P2-2 | ExceptionHandlerMethodResolver 方法匹配 (异常类型/Cause 链/缓存) | 🟡 | **为什么🟡**: @ExceptionHandler 方法怎么定位 — 父类异常/多异常匹配 |
| P3-1 | ControllerAdviceBean.findAnnotatedBeans (@ControllerAdvice 扫描) | 🟢 | **为什么🟢**: 全局异常处理器的收集 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **解析链与模板** (processHandlerException + AbstractHandlerExceptionResolver) | 🔴 | 入口与骨架 |
| B | **注解驱动** (ExceptionHandlerExceptionResolver + 方法匹配) | 🔴 | @ControllerAdvice 核心 |
| C | **兜底与收集** (DefaultHandlerExceptionResolver + ControllerAdviceBean) | 🟡 | 默认行为与全局收集 |

> **Cluster A (§1)**: processHandlerException 链 + HandlerExceptionResolver 接口 + AbstractHandlerExceptionResolver 模板
> **Cluster B (§2)**: ExceptionHandlerExceptionResolver(@ControllerAdvice 缓存) + ExceptionHandlerMethodResolver(异常→方法)
> **Cluster C (§3)**: DefaultHandlerExceptionResolver(标准异常→状态码) + 全链优先级 + @ControllerAdvice 全局收集

→ 引出 7-A: @InitBinder — 异常处理后回到参数绑定: WebDataBinder 定制(@InitBinder/@DateTimeFormat/@NumberFormat 注册转换器)

(End of file - total 61 lines)
