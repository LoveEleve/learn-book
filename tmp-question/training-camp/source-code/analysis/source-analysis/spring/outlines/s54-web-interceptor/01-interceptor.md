# C-12 拦截器 — HandlerInterceptor (三方法 → 执行链 → doDispatch 插入点)

> 依赖 W-1 DispatcherServlet | 🟡 Working | 6 KP | [模式: 责任链 + 栈式清理]

**读者处境**: 登录校验、接口耗时统计、统一加响应头 — 拦截器三方法(pre/post/after)分别什么时候执行？多个拦截器顺序？为什么 postHandle/afterCompletion 是逆序？

### 1. HandlerInterceptor 三方法 + HandlerExecutionChain 执行顺序

场景: 配置了两个拦截器 A(preHandle→true) 和 B(preHandle→true) — 执行顺序: A.pre → B.pre → 处理器 → **B.post → A.post**(逆序) → **B.after → A.after**(逆序)。这就是"正进逆出"的栈式语义。

源码路径:
- `HandlerInterceptor.java:81,103,130,155` — **三方法**: preHandle L103(处理器前, 返回 false 中断) / postHandle L130(处理器后、渲染前) / afterCompletion L155(请求结束无论成败, 资源清理) — 全 default
- `HandlerExecutionChain.java:46,48` — **链状态**: interceptorList(有序 List) + interceptorIndex(已成功 pre 的个数, 初始 -1)
- `HandlerExecutionChain.java:143` — **applyPreHandle()**: L144 正序遍历 → preHandle false→L146 triggerAfterCompletion+return false(短路); true→L148 interceptorIndex=i
- `HandlerExecutionChain.java:158,172` — **applyPostHandle()**: L158 声明, L161 逆序(from size-1→0); **triggerAfterCompletion()**: L172 声明, L173 从 interceptorIndex 逆序到 0 — 只清理 preHandle 成功的

关键设计: **Why post/after 要逆序？** preHandle 是"注册资源"(如开启事务/设置线程变量), postHandle/afterCompletion 是对应"释放" — 先注册的后释放(栈/LIFO), 保证对称。**Why interceptorIndex 限定 after？** 若有拦截器 pre 返回 false 中断, 只有已成功的那些需要 after 清理 — 未执行的不能清理。[模式: 责任链 + 栈式(LIFO)]

数据流: applyPreHandle: [A.pre→true(i=0), B.pre→true(i=1)] → 处理器执行 → applyPostHandle: [B.post, A.post](逆序) → 渲染 → triggerAfterCompletion: [B.after, A.after](从 index=1 逆序)。若 B.pre 返回 false → A.pre=true(i=0), B.pre=false → triggerAfterCompletion 只清 [A.after] + return, 处理器不执行。

### 2. DispatcherServlet.doDispatch 插入点 — 拦截器在请求全流程的位置

场景: 拦截器钩子精确嵌在 doDispatch 哪几步之间？异常时 afterCompletion 还会执行吗？

源码路径:
- `DispatcherServlet.java:1084` — **applyPreHandle**: 失败→return(处理器不执行)
- `DispatcherServlet.java:1089` — **ha.handle**: 处理器(Controller 方法)执行
- `DispatcherServlet.java:1096` — **applyPostHandle**: 处理器后、视图渲染前 — 可改 ModelAndView
- `DispatcherServlet.java:1106` — **processDispatchResult**: 视图渲染(此处才真正输出)
- `DispatcherServlet.java:1109,1112` — **catch → triggerAfterCompletion**: 处理器/渲染抛异常也会进 afterCompletion(无论成败都清理) — 这是 after 与 post 的本质区别(post 只在正常流程)

关键设计: **Why afterCompletion 在 try/catch 外兜底？** after 的职责是"无条件清理"(释放锁/线程变量/日志) — 必须保证 handler 异常后也执行; postHandle 则只在"处理器正常返回"时调用(异常时渲染的是错误视图)。[模式: 模板方法 — 正常/异常双路径]

数据流: doDispatch: getHandler(L1065) → applyPreHandle(1084) → ha.handle(1089) → applyPostHandle(1096) → processDispatchResult 渲染(1106) → 正常结束 → finally/triggerAfterCompletion。若 handler 抛异常 → catch(L1107)→dispatchException → processDispatchResult(渲染错误页) → triggerAfterCompletion(1109)→afterCompletion 执行。

### 3. 注册与排序 — WebMvcConfigurer.addInterceptors

场景: `@Configuration class WebConfig implements WebMvcConfigurer { addInterceptors(registry) {...} }` — 拦截器怎么进链？顺序怎么定？

源码路径:
- `WebMvcConfigurer.java:96` — **addInterceptors(InterceptorRegistry)**: 覆写注册 — 与 Boot 的 WebMvcConfigurer 自动装配衔接
- `WebMvcConfigurationSupport.java:366-367` — **装配**: 创建 InterceptorRegistry → addInterceptors(registry) → registry.getInterceptors() 转为 HandlerInterceptor[] 放进 mapping 的拦截器链
- `InterceptorRegistry.java`(85行) — **注册器**: registry.addInterceptor(interceptor).addPathPatterns("/**").order(1) — 支持路径匹配与顺序(可覆写 C-4 排序默认值)

关键设计: **Why 用 InterceptorRegistry 而非直接 List？** 注册时还要配"拦截哪些路径"(addPathPatterns)与排除(排除路径) — registry 把"注册+路径+顺序"封装; 拦截器默认按注册顺序, 也可显式 order 调整。**vs Servlet Filter**: Filter 在 Servlet 容器层更早(HandlerMapping 之前), 拦截器在 Spring MVC 内(拿到 handler/ModelAndView) — 拦截器能做 view/处理器级操作, Filter 更底层。[模式: 注册器 + 建造]

数据流: @Configuration implements WebMvcConfigurer → addInterceptors: registry.addInterceptor(authInterceptor).addPathPatterns("/api/**").order(1) + .addInterceptor(logInterceptor).order(2) → WebMvcConfigurationSupport 收集 → 排序 → 装进 HandlerExecutionChain → 请求 /api/x 时进链执行(§1 顺序)。若 /public/x 不匹配 /api/** → 不经过 authInterceptor。

→ 引出 7-6: 异常处理 — 拦截器/处理器抛的异常汇聚到 processDispatchResult → HandlerExceptionResolver 链(@ExceptionHandler/@ControllerAdvice/默认兜底)。
