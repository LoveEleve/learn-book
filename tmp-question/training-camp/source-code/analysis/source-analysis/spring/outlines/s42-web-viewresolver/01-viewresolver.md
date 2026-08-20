# W-6 ViewResolver — 逻辑视图解析 (Resolver 链 → View 渲染)

> 依赖 W-5 HttpMessageConverter | 🟡 Working | 6 KP | [模式: 模板方法 + 责任链 + 内容协商]

**读者处境**: W-5 讲了 @ResponseBody 直写 JSON — 但 Controller 返回 `"userList"` 这种 String 时, Spring 怎么知道渲染哪个 JSP？逻辑视图名怎么变成 `/WEB-INF/views/userList.jsp`？多个 ViewResolver 谁先谁后？缓存放在哪？

### 1. render 入口 + Resolver 链 + AbstractCachingViewResolver 缓存模板

场景: `@Controller` 方法 `return "userList"` — 从字符串到 JSP 响应要经过: ①视图名→View 对象(解析) ②View→HTTP 输出(渲染)。"userList" 只是**逻辑名**, 物理资源由 resolver 决定。

源码路径:
- `DispatcherServlet.java:1400` — **render()**: locale→response.setLocale → viewName 非 null→resolveViewName(链); 非 null→直接用 mv.getView() → SmartView.resolveNestedViews → mv.getStatus→状态码 → L1438 view.render(model, request, response)
- `DispatcherServlet.java:1481` — **resolveViewNameInternal()**: 遍历 this.viewResolvers → viewResolver.resolveViewName(viewName, locale) → **首个非 null View 立即返回** — 前一个返回 null 才轮到下一个
- `ViewResolver.java:38` — **接口单方法**: resolveViewName(viewName, locale) → View 或 null("不归我管"); `View.java:47,97` — render(model, request, response)
- `AbstractCachingViewResolver.java:172` — **resolveViewName()**: 缓存关→直接 createView; 开→getCacheKey(viewName+locale)(L215) → viewAccessCache 读 → miss→synchronized(viewCreationCache) 双重检查 → createView(子类) → 构建失败且 cacheUnresolved→UNRESOLVED_VIEW 哨兵 → cacheFilter 通过→写入双缓存 → 返回(UNRESOLVED→null)
- `AbstractCachingViewResolver.java:274,292` — **模板两钩子**: createView(默认调 loadView, 可覆写) → loadView 抽象(子类真构建) — 缓存/并发逻辑全在基类

关键设计: **Why 缓存与并发收敛在基类？** 所有内置 resolver(InternalResourceView/UrlBased/BeanName…)都需"视图名→View 实例"的缓存 — 每个 View 是轻量配置对象, 重复创建浪费; 双缓存(L77 无锁读镜像 viewAccessCache + L80 主缓存 viewCreationCache, LinkedHashMap accessOrder 实现 LRU, 超限 removeEldestEntry 同步清镜像) — 读无锁、创建加锁, 避免并发下重复创建。**Why resolveViewName 返回 null 而非抛异常？** 责任链语义 — "我不管"让下一个 resolver 尝试, 链尾仍无→render L1412 抛 ServletException(500)。[模式: 责任链 + 模板方法]

数据流: return "userList" → 处理器返回 → ModelAndView("userList") → DispatcherServlet.render(L1400) → viewName="userList" 非 null → resolveViewName → L1481 遍历 [InternalResourceViewResolver] → resolveViewName("userList", zh) → L172 缓存 miss → createView → loadView → InternalResourceView → 缓存写入 → 返回 View → view.render → JSP 渲染。

### 2. UrlBasedViewResolver 三分支 + prefix/suffix + InternalResourceViewResolver(JSP)

场景: 配置 prefix=/WEB-INF/views/ suffix=.jsp — "userList" → /WEB-INF/views/userList.jsp; 表单提交后 `redirect:/users` 要 302; 内部跳转 `forward:/login` 要转发 — 三种视图名语义完全不同。

源码路径:
- `UrlBasedViewResolver.java:466` — **createView() 三分支**: L469-470 canHandle(viewNames 不匹配→null 传链) → `redirect:` 前缀→L475-482 new RedirectView(url, contextRelative, redirectHttp10Compatible)(默认 sendRedirect→302; 显式状态码或关 1.0 兼容→303) → `forward:` 前缀→L488-489 new InternalResourceView(url) → 否则 super.createView→loadView
- `UrlBasedViewResolver.java:551,571` — **loadView()/buildView()**: loadView: buildView→applyLifecycleMethods→checkResource(locale)(资源不存在→null); buildView: instantiateView(反射) → L573 `setUrl(getPrefix() + viewName + getSuffix())` → attributes/contentType/exposePathVariables
- `InternalResourceViewResolver.java:97,108` — **JSP 专用**: requiredViewClass=InternalResourceView; instantiateView: jstl 在 classpath→JstlView; buildView: super.buildView→setPreventDispatchLoop(true)(防无限转发)
- `InternalResourceView.java:138` — **renderMergedOutputModel**: L142 exposeModelAsRequestAttributes(model→request attribute, JSP 用 ${name} 取) → L151 getRequestDispatcher(url) → 响应已提交或 include→L163 rd.include / 否则→L171 rd.forward — **服务端转发, 非重定向**

关键设计: **Why 用 "redirect:"/"forward:" 前缀而非专门方法？** 视图名是纯字符串契约 — 前缀让"视图解析"一个机制覆盖三种跳转(转发/重定向/直接渲染), Controller 无需 import 任何 View 类; RedirectView 走浏览器 302(URL 变化、可刷新), forward/include 走服务端跳转(URL 不变)。**Why checkResource？** 若 JSP 不存在, loadView 返回 null → 链继续 → 最终 500 — 把"资源缺失"暴露为解析失败而非渲染期 404。[模式: 策略 — 前缀分派]

数据流: "userList" → createView(L466): canHandle→true → 无 redirect/forward 前缀 → super.createView→loadView(L551) → buildView(L571): /WEB-INF/views/userList.jsp → checkResource: 文件存在→true → applyLifecycleMethods → View → render → InternalResourceView.renderMergedOutputModel(L138): model→request attributes → RequestDispatcher.forward("/WEB-INF/views/userList.jsp") → JSP 输出 HTML。"redirect:/users" → L475 RedirectView → 302 Location:/users。

### 3. String 入口 + ContentNegotiatingViewResolver — 视图层的内容协商

场景: 同一个 URL, 浏览器 Accept:text/html 要 HTML 页面, 客户端 Accept:application/json 要 JSON 数据 — 逻辑视图名相同, 但"选中哪个 View"要按 Accept 头协商。

源码路径:
- `ViewNameMethodReturnValueHandler.java:72,78` — **String 返回值入口**: supportsReturnType=void 或 CharSequence → L83 mavContainer.setViewName(viewName) → L103 isRedirectViewName("redirect:" 前缀/redirectPatterns)→setRedirectModelScenario(true)
- `ContentNegotiatingViewResolver.java:224` — **resolveViewName()**: L227 getMediaTypes(定义 L257: ContentNegotiationManager 解析 Accept) → L229 getCandidateViews(定义 L309: 遍历委托 viewResolvers 解析 viewName + `viewName.extension` 尝试 + defaultViews 兜底) → L230 getBestView(定义 L339: redirect SmartView 优先 → view.getContentType 与 requestedMediaType isCompatibleWith → 命中→SELECTED_CONTENT_TYPE→返回) → 无匹配: useNotAcceptableStatusCode→NOT_ACCEPTABLE_VIEW(406)/返回 null
- `ContentNegotiatingViewResolver.java:106` — **顺序**: order=HIGHEST_PRECEDENCE — 必须最先(否则普通 resolver 已返回 View, 协商永远不执行)

关键设计: **Why 视图层也要内容协商？** 与 W-5 §2 的转换器协商**同源** — ContentNegotiationManager 解析 Accept 后: 转换器链选"**转换器**"(对象→字节), 视图协商选"**View**"(模型→HTML/JSON/PDF)— 两处共享 requestedMediaTypes 语义。差异: 转换器协商在 HandlerAdapter 写阶段, 视图协商在 render 阶段; 无匹配时转换器→406 异常, 视图→NOT_ACCEPTABLE_VIEW(仍 406, 但走视图机制)。[模式: 内容协商]

数据流: return "userList", Accept:application/json → ViewNameMethodReturnValueHandler.setViewName → ModelAndView("userList") → CNVR.resolveViewName(L224) → getMediaTypes=[application/json] → getCandidateViews(L229): 委托 InternalResourceViewResolver.resolveViewName("userList")→JSP View(text/html) + 扩展名尝试 "userList.json"→checkResource 失败→null + defaultViews 兜底→MappingJackson2JsonView(application/json) → getBestView(L230): JSP 的 text/html 与 json 不兼容→跳过 → MappingJackson2JsonView 兼容→SELECTED_CONTENT_TYPE=application/json → render: ObjectMapper 输出 JSON。浏览器 Accept:text/html → 反向选中 JSP。

→ 引出 W-7: 静态资源 — ResourceHttpRequestHandler/addResourceHandlers — "URL→静态文件"不经视图链的第三条路径; 之后 W-8 异常处理(HandlerExceptionResolver — 渲染失败后的兜底)。
