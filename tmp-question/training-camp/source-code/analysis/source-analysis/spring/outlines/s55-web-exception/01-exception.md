# C-13 异常处理 — HandlerExceptionResolver (解析链 → @ControllerAdvice → 兜底)

> 依赖 C-12 拦截器 | 🟡 Working | 6 KP | [模式: 责任链 + 模板方法 + 策略]

**读者处境**: Controller 抛异常 → 返回 JSON 错误 / 错误页 — 谁处理？@ExceptionHandler 方法怎么被找到？@ControllerAdvice 全局异常为什么有效？标准异常(如 404)的默认状态码哪来的？

### 1. 解析链入口 — processHandlerException → resolver 链

场景: 处理器抛 NullPointerException → doDispatch 的 processDispatchResult 发现异常 → processHandlerException → 遍历所有 HandlerExceptionResolver, 第一个能处理的接管并返回错误 ModelAndView。

源码路径:
- `DispatcherServlet.java:694,696` — **链装配**: 收集容器中 HandlerExceptionResolver bean → `AnnotationAwareOrderComparator.sort`(C-4 排序) — 顺序即处理优先级
- `DispatcherServlet.java:1161` — **processHandlerException**: 处理器异常时调用 → 遍历 handlerExceptionResolvers → 首个返回非 null ModelAndView 的接管 → render 渲染错误视图
- `HandlerExceptionResolver.java:36,53` — **接口**: resolveException(request, response, handler, ex) → ModelAndView(返回 null = 不处理, 交给下一个)

关键设计: **Why 责任链 + 排序？** 不同异常交给不同 resolver: 自定义(@ExceptionHandler)优先, 标准异常兜底 — 排序(AnnotationAwareOrderComparator)保证"越具体的越先"; 返回 null 让链继续, 全部不处理则容器重抛(最终 500)。[模式: 责任链]

数据流: handler 抛 BusinessException → doDispatch catch → dispatchException → processDispatchResult(1106) → 有异常→L1161 processHandlerException → 遍历 [ExceptionHandlerExceptionResolver, DefaultHandlerExceptionResolver] → ExceptionHandlerExceptionResolver.shouldApplyTo→true→doResolveException → 找到 @ExceptionHandler 方法→渲染错误视图(mv 非 null)。

### 2. ExceptionHandlerExceptionResolver — @ControllerAdvice 缓存与方法匹配

场景: `@ControllerAdvice class GlobalHandler { @ExceptionHandler(BusinessException.class) ... }` — 这个 advice 的异常方法怎么被找到并调用？

源码路径:
- `ExceptionHandlerExceptionResolver.java:85,309` — **advice 缓存**: initExceptionHandlerAdviceCache L309: `ControllerAdviceBean.findAnnotatedBeans(ctx)`(L314, 找所有 @ControllerAdvice) → 每个 bean 建 `ExceptionHandlerMethodResolver`(L320) → 存 exceptionHandlerAdviceCache(按 ControllerAdviceBean 分组, 含本地 @ExceptionHandler)
- `ExceptionHandlerMethodResolver.java:60,164` — **异常→方法**: resolveMethod(exception) L164 → 按异常类型(含父类/Cause 链)在 mappedMethods(L80 缓存)中查最匹配的 @ExceptionHandler 方法 → 返回 Method
- 执行: 找到方法后 InvocableHandlerMethod 调用, 返回值走正常处理(可 @ResponseBody/返回视图)

关键设计: **Why 用"异常类型→方法"的映射缓存而非遍历？** 每次异常都遍历所有 @ExceptionHandler 匹配太慢 — 构建期(initExceptionHandlerAdviceCache)就把"异常类型→Method"建成 Map; 匹配还考虑异常 Cause 链和父类(处理 IllegalArgumentException 的方法也能接它子类)。[模式: 缓存 + 类型匹配]

数据流: 抛 BusinessException → ExceptionHandlerExceptionResolver.doResolveException → 遍历 adviceCache: 全局 advice 的 ExceptionHandlerMethodResolver.resolveMethod(BusinessException) → mappedMethods 命中 → Method=handleBusiness → InvocableHandlerMethod.invoke → @ResponseBody → 返回 JSON 错误 / 返回视图名 → mv → 渲染。

### 3. DefaultHandlerExceptionResolver — 标准异常兜底

场景: 没写 @ExceptionHandler 的 NoHandlerFoundException(404)/MethodArgumentNotValidException(400) — 谁设状态码？

源码路径:
- `DefaultHandlerExceptionResolver.java:155,181` — **标准异常**: doResolveException L181: 按异常类型 switch → 设状态码(NoHandlerFound→404, MethodArgumentNotValid→400, AsyncRequestTimeout→503 等) → 返回空 mv(仅设状态码)
- `DefaultHandlerExceptionResolver.java:174` — **兜底顺序**: order=LOWEST_PRECEDENCE — 排在 @ExceptionHandler 之后, 处理框架标准异常
- 全链优先级: `ExceptionHandlerExceptionResolver`(@ExceptionHandler, 默认 order 0) → `ResponseStatusExceptionResolver`(@ResponseStatus) → `DefaultHandlerExceptionResolver`(标准异常, LOWEST) — 用户自定义 resolver 可插在中间

关键设计: **Why Default 兜底且只设状态码？** 框架标准异常(404/400/415)有明确 HTTP 语义 — 不需要视图, 只设状态码即可; LOWEST 保证自定义处理优先, 只有未接管的标准异常才由它统一映射。**Why 三种 resolver 分工？** 注解驱动(@ExceptionHandler)、注解状态(@ResponseStatus)、标准兜底 — 覆盖从"完全自定义"到"框架默认"的谱系。[模式: 策略分层]

数据流: 请求 /nope 无匹配 → NoHandlerFoundException(需 enable 404) → processHandlerException → ExceptionHandlerExceptionResolver: 无 @ExceptionHandler(NoHandlerFound)→null → ResponseStatusExceptionResolver: 无 @ResponseStatus→null → DefaultHandlerExceptionResolver → handleNoHandlerFoundException → response.sendError(404) → 空 mv → 容器错误页。业务异常 @ExceptionHandler 命中 → 自定义 JSON/视图(不落兜底)。

→ 引出 7-A: @InitBinder — 从异常回到参数绑定: WebDataBinder 是 @RequestBody 校验与 @DateTimeFormat/@NumberFormat 转换的承载者。
