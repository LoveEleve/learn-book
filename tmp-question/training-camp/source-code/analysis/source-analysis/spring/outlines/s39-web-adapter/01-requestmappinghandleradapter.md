# W-3 RequestMappingHandlerAdapter — HandlerMethod 如何被反射调用

> 依赖 W-1 doDispatch + W-2 HandlerMapping | 🟡 Working | 3 KP | [模式: 适配器模式 + 策略模式]

**读者处境**: W-2 中 RequestMappingHandlerMapping 返回了 HandlerMethod — 但 doDispatch 的 `ha.handle()` 到底做了什么？HandlerMethod 只是一个"方法元数据"包装 — 谁把它变成真实的反射调用？参数从哪来？返回值怎么处理？

### 1. invokeHandlerMethod → doInvoke — HandlerMethod 的完整调用链

场景: `ha.handle(request, response, handlerMethod)` → RequestMappingHandlerAdapter.handleInternal → invokeHandlerMethod: 创建 ServletWebRequest → 获取 WebDataBinderFactory(参数绑定) + ModelFactory(Model 初始化) → 把 HandlerMethod 包装为 ServletInvocableHandlerMethod → 注入 26 个参数解析器 + 返回值处理器 + 校验器 → modelFactory.initModel 初始化 @ModelAttribute/@SessionAttributes → invokeAndHandle → 真正反射调用。

源码路径:
- `RequestMappingHandlerAdapter.java:874` — **handleInternal()**: 入口 — checkRequest → synchronizeOnSession 可选同步 → invokeHandlerMethod → Cache-Control 头处理
- `RequestMappingHandlerAdapter.java:941` — **invokeHandlerMethod()**: 装配链 — L962 createInvocableHandlerMethod + L964-971 注入 resolvers/returnValueHandlers/binderFactory/parameterNameDiscoverer/methodValidator → L975 modelFactory.initModel → L991 invokeAndHandle → L996 getModelAndView
- `RequestMappingHandlerAdapter.java:1005` — **createInvocableHandlerMethod()**: 工厂方法 — new ServletInvocableHandlerMethod(handlerMethod) — 子类(自定义 MVC 配置)可覆写; 异步结果的包装走 ServletInvocableHandlerMethod.wrapConcurrentResult(L203)→内部类 ConcurrentResultHandlerMethod(L214)
- `InvocableHandlerMethod.java:178` — **invokeForRequest()**: 参数解析 → 参数校验 → doInvoke → 返回值校验
- `InvocableHandlerMethod.java:207` — **getMethodArgumentValues()**: L219 findProvidedArgument(预提供参数优先) → L223 resolvers.supportsParameter → L227 resolvers.resolveArgument — 每个参数独立解析
- `InvocableHandlerMethod.java:247` — **doInvoke()**: L258 method.invoke(getBean(), args) — Kotlin 分支先行(L250-255)— 最终就是一句反射调用

关键设计: **Why 参数解析在 invoke 之前独立成 getMethodArgumentValues？** 反射调用要求参数数组**完全确定**才能执行 — 但每个参数的类型/来源都不同(@PathVariable 从 URL / @RequestBody 从请求体 / @ModelAttribute 从 form)。分离后: getMethodArgumentValues 只"生产参数数组"(策略链可插拔) — doInvoke 只"消费参数数组"(纯反射) — Object[] args 解耦。[模式: 策略模式 — resolver 链生产参数]

数据流: ha.handle(request, response, handlerMethod) → handleInternal(L874) → checkRequest → invokeHandlerMethod(L941) → L944-957 创建 WebAsyncManager+AsyncWebRequest(异步支持) → L959 getDataBinderFactory(HandlerMethod→DataBinderFactory) → L960 getModelFactory → L962 createInvocableHandlerMethod → new ServletInvocableHandlerMethod(handlerMethod) → L964-971 注入 argumentResolvers/returnValueHandlers/dataBinderFactory/parameterNameDiscoverer/methodValidator → L973 new ModelAndViewContainer + L974 添加 FlashMap 属性 → L975 modelFactory.initModel(处理 @ModelAttribute/@SessionAttributes) → L991 invokeAndHandle(webRequest, mavContainer) → invokeForRequest(L178) → getMethodArgumentValues(L207): 遍历 methodParameters → 参数0: findProvidedArgument=null → resolvers.supportsParameter(@PathVariable resolver)=true → resolveArgument → "1"(从 URL 提取) → 参数1: 同理 → "Alice" → 返回 Object[]{1L, "Alice"} → 参数校验(无 @Validated 跳过) → doInvoke(L247) → method.invoke(userController, 1L, "Alice") → UserController.getUser(1L) → 返回 User{1,"Alice"} → 返回值校验跳过 → returnValue → invokeAndHandle 继续 → 返回值处理。

### 2. ServletInvocableHandlerMethod.invokeAndHandle — 返回值与响应装配

场景: 业务方法返回 User 对象 — invokeAndHandle 需要决定: 这个 User 是写进 Model 让 ViewResolver 渲染? 还是用 @ResponseBody 序列化为 JSON? @ResponseStatus 注解的响应码怎么设置?

源码路径:
- `ServletInvocableHandlerMethod.java:115` — **invokeAndHandle()**: L118 invokeForRequest(拿到返回值) → L119 setResponseStatus(@ResponseStatus 处理) → L121-133 返回值特判(null 分支 L121-127: requestNotModified/responseStatus已设/mavContainer.requestHandled → 短路; else-if 分支 L128-131: reason 有文本 → 短路) → L136-137 returnValueHandlers.handleReturnValue(核心分发)
- `ServletInvocableHandlerMethod.java:150` — **setResponseStatus()**: 读取 @ResponseStatus 注解 → L158-159 有 reason → response.sendError(status, reason) / 无 reason → L163 response.setStatus(status) → L168 设置 View.RESPONSE_STATUS_ATTRIBUTE(供 RedirectView 使用) — 短路在 invokeAndHandle 的 L124-133(responseStatus != null → setRequestHandled(true))
- `RequestMappingHandlerAdapter.java:1095` — **getModelAndView()**: mavContainer.isRequestHandled → null(直接响应) / 否则 new ModelAndView(mavContainer.getModel()...) — invokeHandlerMethod 中调用点在 L996

关键设计: **Why null 返回值和 @ResponseStatus 都导致"短路"？** MVC 的契约: 若业务方法自己写入了 response(如 HttpServletResponse 参数或 @ResponseBody 处理器)— mavContainer.setRequestHandled(true) 标记"请求已处理" — 后续不再创建 ModelAndView — 避免 ViewResolver 二次渲染。@ResponseStatus 同理: 状态码+reason 已设置 → 无需视图。这两个短路点是"直接响应 vs 视图渲染"的分界 — requestHandled 是短路开关。[模式: 状态标记]

数据流: 业务方法返回 User{1,"Alice"} → invokeAndHandle(L115) → L118 invokeForRequest → 返回 User → L119 setResponseStatus: getUser 方法无 @ResponseStatus → 跳过 → L121 returnValue=User 非 null(不进 null 短路分支)→ L128 getResponseStatusReason 无文本 → 跳过 → L133 setRequestHandled(false) → L136 returnValueHandlers.handleReturnValue(User, 返回类型, mavContainer, webRequest) → 遍历 handlers: RequestResponseBodyMethodProcessor.supportsReturnType(@ResponseBody? 无 → false) → ViewNameMethodReturnValueHandler.supportsReturnType(User? 非 void/String → false) → ModelAndViewMethodReturnValueHandler.supportsReturnType(User? 非 ModelAndView → false) → ... → 若 Controller 方法标 @ResponseBody → RequestResponseBodyMethodProcessor.supportsReturnType=true → messageConverter(Jackson) → JSON 写入 response → mavContainer.setRequestHandled(true) → 返回 invokeHandlerMethod → L996 getModelAndView(声明 L1095): isRequestHandled=true → return null → handleInternal 返回 null → doDispatch 的 processDispatchResult 收到 null → 无视图渲染 → response 已含 JSON。

### 3. HandlerAdapter 适配模式 + 26 个 ArgumentResolver 的分类体系

场景: doDispatch 中 `getHandlerAdapter(handler)` 遍历所有 HandlerAdapter → supports(handler) 匹配 — HandlerMethod 用 RequestMappingHandlerAdapter, HttpRequestHandler 用 HttpRequestHandlerAdapter, Servlet 用 SimpleServletHandlerAdapter。

源码路径:
- `RequestMappingHandlerAdapter.java:868` — **supportsInternal()**: 返回 true — 所有 HandlerMethod 都由本适配器处理; 契约: AbstractHandlerMethodAdapter.supports(L68) 先检查 `handler instanceof HandlerMethod` 再调 supportsInternal — handle(L84) 强转为 HandlerMethod 后调 handleInternal
- `RequestMappingHandlerAdapter.java:590` — **afterPropertiesSet()**: L592 initControllerAdviceCache(@ControllerAdvice 的 @InitBinder/@ModelAttribute 缓存) → L596-598 默认 argumentResolvers 装配 → L603-605 默认 returnValueHandlers → L607-612 methodValidator 构建 → L616 initMessageConverters(默认 3 个转换器); custom resolvers 通过 setCustomArgumentResolvers(L223) 注入
- `RequestMappingHandlerAdapter.java:685` — **getDefaultArgumentResolvers()**: 26 个 resolver 三组: ①注解组(L688-703: @RequestParam/@PathVariable/@RequestHeader/@RequestBody/@RequestPart/@CookieValue/@SessionAttribute/@RequestAttribute/@ModelAttribute) ②类型组(L705-713: WebRequest/HttpEntity/Model/Map/Errors/SessionStatus) ③custom 组(L719-721) + catch-all 组(L724-727: Principal + 兜底 @RequestParam/ModelAttribute)

关键设计: **Why 26 个 resolver 而非一个巨型解析器？** 每个参数注解/类型对应一个 resolver — 单一职责 + 顺序可配置。supportsParameter 先问"这个参数归你管吗" → 归谁管谁解析。新增参数类型(如 Spring Security 的 @AuthenticationPrincipal)只需注册新 resolver — 不改框架代码。这是"开闭原则"的教科书实现 — 框架固定调用链, 解析策略开放扩展。[模式: 适配器模式 + 策略模式 — HandlerAdapter 适配 Handler 类型, ArgumentResolver 策略化参数解析]

数据流: DispatcherServlet.initStrategies→initHandlerAdapters→获取 [RequestMappingHandlerAdapter, HttpRequestHandlerAdapter, SimpleServletHandlerAdapter] → doDispatch: getHandlerAdapter(handlerMethod) → 遍历: RequestMappingHandlerAdapter.supports(HandlerMethod)→supportsInternal→true → 返回适配器 → ha.handle → (参数解析链见 §1) → 若某参数类型在 26 个 resolver 中全部 supportsParameter=false → 请求到达时抛 IllegalStateException("No suitable resolver") — 这是"参数类型无 resolver 时首个请求即暴露配置错误"的保护机制。

→ 引出 W-4: HandlerMethodArgumentResolver — 26 个 resolver 中 @PathVariable(URL 模板变量提取)、@RequestParam(Query 参数绑定)、@RequestBody(JSON 反序列化) 三个最常用的具体解析流程。
