# W-1 DispatcherServlet — doDispatch 核心调度链

> 依赖 Stage 2 spring-context | 🔴 Deep | 3 KP | [模式: 前端控制器 + 策略模式]

**读者处境**: Tomcat 接收 HTTP 请求 → 传给 Servlet → Spring MVC 的入口就是 DispatcherServlet。doDispatch 是 Spring MVC 全部请求的统一入口。

### 1. doDispatch 6步调度链

场景: GET /users/1 到达 Tomcat → Servlet 容器 → DispatcherServlet.service → doDispatch — 所有 Spring MVC 请求的统一调度入口 — 从"找到处理器"到"执行"到"渲染/异常处理"的 6 步管道。

源码路径:
- `DispatcherServlet.java:1049` — **doDispatch()**(L1051 为 mappedHandler 声明): ①L1061 `processedRequest = checkMultipart(request)`(文件上传) ②L1065 `mappedHandler = getHandler(processedRequest)`(HandlerMapping链) ③L1072 `getHandlerAdapter(mappedHandler.getHandler())` ④L1084 `mappedHandler.applyPreHandle`(拦截器preHandle) ⑤L1089 `mv = ha.handle(processedRequest, response, mappedHandler.getHandler())`(执行业务) ⑥L1096 `mappedHandler.applyPostHandle`(L1092 是 asyncManager 判断) ⑦L1106 `processDispatchResult(request, response, mappedHandler, mv, dispatchException)`(异常/视图渲染)

数据流: GET /users/1 → DispatcherServlet.service → doDispatch → checkMultipart(no)→getHandler→遍历handlerMappings: BeanNameUrlHandlerMapping→null→RequestMappingHandlerMapping.getHandler→匹配 @GetMapping("/users/{id}")→HandlerExecutionChain[UserController.getUser, HandlerInterceptor[]]→getHandlerAdapter→RequestMappingHandlerAdapter.supports(HandlerMethod)→true→applyPreHandle→Interceptor.preHandle→auth check→true→adapter.handle→invoke UserController.getUser(1)→返回 User{1,"Alice"}→ModelAndView→applyPostHandle→processDispatchResult→render→ViewResolver→Thymeleaf→HTML→response

关键设计: **doDispatch 的异常分支** — 任一环节抛异常 → dispatchException 被捕获 → 跳过 applyPostHandle → 直接 processDispatchResult(dispatchException, ...)→异常解析器链(HandlerExceptionResolver)处理 → 返回 500 或错误视图; 若 afterCompletion 存在 → 无论成败都执行(资源清理)。

### 2. HandlerMapping / HandlerAdapter 双链策略

场景: DispatcherServlet 初始化时从容器获取所有 HandlerMapping Bean — 有 RequestMappingHandlerMapping(注解) + SimpleUrlHandlerMapping(URL映射) + BeanNameUrlHandlerMapping(bean名映射) — doDispatch 遍历这个列表 — 找到第一个能处理请求的Mapping → 返回 HandlerExecutionChain(Handler + Interceptor列表)。

**HandlerAdapter**: supports(handler)匹配—返回true则调用handle—HandlerMethod(@RequestMapping注解)用 RequestMappingHandlerAdapter — HttpRequestHandler 用 HttpRequestHandlerAdapter — Servlet 用 SimpleServletHandlerAdapter。

源码路径:
- `DispatcherServlet.java:601` — **initHandlerMappings()**: 从容器收集全部 HandlerMapping Bean
- `DispatcherServlet.java:607` — **beansOfTypeIncludingAncestors**: 含祖先上下文的 HandlerMapping 收集 + L611 AnnotationAwareOrderComparator 排序
- `DispatcherServlet.java:1072` — **getHandlerAdapter()**: doDispatch 中按 supports 匹配选择适配器

关键设计: **Why HandlerMapping 和 HandlerAdapter 是两条独立链？** 映射(找 Handler)与执行(调 Handler)是两类关注点 — 新增一种 Handler 类型(如注解式 HandlerMethod vs 原生 Servlet)只需新增对应 Mapping/Adapter, 互不影响。Mapping 返回 HandlerExecutionChain(Handler+拦截器) — Adapter 消费 Handler 执行 — 中间通过 supports 匹配解耦。[模式: 策略模式 — 双链各自可扩展]

数据流: DispatcherServlet初始化→initStrategies→initHandlerMappings(DispatcherServlet.java:601)→L607 BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false)(含祖先上下文)→L611 AnnotationAwareOrderComparator.sort 排序→获取RequestMappingHandlerMapping/SimpleUrlHandlerMapping→存入handlerMappings List→initHandlerAdapters(同模式)→获取RequestMappingHandlerAdapter/HttpRequestHandlerAdapter→doDispatch→getHandler(request)→遍历handlerMappings→handlerMapping.getHandler(request)→matches→RequestMappingHandlerMapping内部: lookupHandlerMethod→匹配"/users/{id}"→UserController.getUser→new HandlerMethod(bean, method)→pack into HandlerExecutionChain→assign Interceptors→返回→getHandlerAdapter(handlerMethod)→遍历handlerAdapters→adapter.supports→RequestMappingHandlerAdapter.supports(true)→返回adapter→ha.handle

### 3. HandlerInterceptor 三阶段拦截

场景: preHandle(执行前—可返回false阻断整个请求)→postHandle(Controller执行后视图渲染前—可修改ModelAndView)→afterCompletion(视图渲染后—always调用即使异常—资源清理)。

源码路径:
- `HandlerExecutionChain.java:143` — **applyPreHandle()**: 正序遍历 interceptors → 任一返回 false → triggerAfterCompletion 已执行过的 → 返回 false 中断
- `HandlerExecutionChain.java:158` — **applyPostHandle()**: 逆序遍历 — 后注册的先执行
- `DispatcherServlet.java:1106` — **processDispatchResult()**: 异常处理 + render + afterCompletion 触发

关键设计: **Why preHandle 正序、postHandle/afterCompletion 逆序？** 拦截器是洋葱模型 — preHandle 按注册顺序进入, postHandle/afterCompletion 按相反顺序退出(后注册的先清理)— 与 try/finally 的嵌套语义一致: 先打开的资源后关闭, 保证清理顺序与资源依赖顺序匹配。

数据流: HandlerExecutionChain.applyPreHandle→interceptors[0].preHandle→auth interceptor→return true→continue→interceptors[1].preHandle→log interceptor→log→return true→all preHandle passed→adapter.handle→Controller.getUser→return ModelAndView→applyPostHandle→interceptors[1].postHandle(log)→interceptors[0].postHandle(auth)→视图渲染→processDispatchResult→render→afterCompletion→interceptors[1].afterCompletion(cleanup)→interceptors[0].afterCompletion(cleanup)

→ spring-web 第一域完成。DispatcherServlet 调度链 + HandlerMapping/Adapter + Interceptor。引出 W-2: RequestMappingHandlerMapping — @RequestMapping 注解如何处理为 HandlerMethod。
