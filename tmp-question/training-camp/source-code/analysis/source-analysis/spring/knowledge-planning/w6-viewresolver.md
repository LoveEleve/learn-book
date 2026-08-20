# W-6 ViewResolver — 逻辑视图解析 (Resolver 链 → View 渲染)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | DispatcherServlet.render(316行前段)+ViewResolver(58行)+View(100行)+AbstractCachingViewResolver(316行)+UrlBasedViewResolver(626行)+InternalResourceViewResolver(117行)+InternalResourceView(250行)+AbstractView(501行)+ContentNegotiatingViewResolver(380行)+ViewNameMethodReturnValueHandler(107行)
> 基线: W-5 结尾桥 — @ResponseBody 走消息转换器直写响应; 本域展开另一条分岔: 无 @ResponseBody 的 String 返回值 → 逻辑视图名 → ViewResolver 链 → View.render — 与 W-5 的内容协商机制对照

---

## §0.8

- 🟡 Working，1篇 — 入口(DispatcherServlet.render: 视图名→resolveViewNameInternal 遍历链) → 缓存模板(AbstractCachingViewResolver: 双缓存+UNRESOLVED) → URL 构建(UrlBasedViewResolver: prefix/suffix+redirect:/forward: 特殊前缀+checkResource) → JSP(InternalResourceViewResolver: requiredViewClass+InternalResourceView forward/include) → 协商视图(ContentNegotiatingViewResolver: Accept→候选视图→最匹配) → 返回值入口(ViewNameMethodReturnValueHandler: String→viewName)
- 设计模式: [模式: 模板方法]—AbstractCachingViewResolver 固定缓存骨架, createView/loadView 子类实现; [模式: 责任链]—viewResolvers 逐个尝试首个非 null; [模式: 内容协商]—Accept 头→候选视图匹配

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DispatcherServlet.java:1400 | render() | **渲染入口**: locale→response.setLocale → viewName 非 null→resolveViewName(链) / 非 null→直接用 mv.getView() → SmartView.resolveNestedViews → mv.getStatus→状态码 → view.render(model, request, response)(L1438) | High |
| DispatcherServlet.java:1481 | resolveViewNameInternal() | **Resolver 链**: 遍历 this.viewResolvers → viewResolver.resolveViewName(viewName, locale) → 首个非 null View 立即返回 — 前一个返回 null 才轮到下一个 | High |
| ViewResolver.java:38 | 接口 | **接口契约**: 单方法 resolveViewName(viewName, locale) → View 或 null — 返回 null 表示"我不管这个视图名" | High |
| View.java:47,97 | 接口 | **View 接口**: render(model, request, response) — 逻辑视图名→可渲染 View 的两步: 解析(Resolver)+渲染(View) | High |
| AbstractCachingViewResolver.java:172 | resolveViewName() | **双缓存模板**: isCache()→L215 getCacheKey(viewName+locale) → viewAccessCache 读 → miss→synchronized(viewCreationCache) 双重检查 → createView(子类) → 失败且 cacheUnresolved→UNRESOLVED_VIEW 哨兵 → cacheFilter → 双缓存写入 → 返回(UNRESOLVED→null) | High |
| AbstractCachingViewResolver.java:274,292 | createView()/loadView() | **模板两钩子**: createView(默认直接调 loadView, 可覆写拦截) → loadView 抽象(子类真正构建 View) — 缓存/并发逻辑全在基类 | High |
| UrlBasedViewResolver.java:466 | createView() | **三分支**: L471 canHandle(viewNames 匹配, 不匹配→null 传链) → redirect: 前缀→L475-482 RedirectView(302) → forward: 前缀→L488-489 InternalResourceView → 否则 super.createView→loadView | High |
| UrlBasedViewResolver.java:551,571 | loadView()/buildView() | **视图构建**: loadView: buildView→applyLifecycleMethods→checkResource(资源存在?)→false 返回 null; buildView: instantiateView→setUrl(prefix+viewName+suffix)(L573)→attributes→contentType→exposePathVariables | High |
| InternalResourceViewResolver.java:97,108 | requiredViewClass()/buildView() | **JSP 专用**: requiredViewClass=InternalResourceView; instantiateView: jstl 在 classpath→JstlView; buildView: super→setPreventDispatchLoop(true) | High |
| InternalResourceView.java:138 | renderMergedOutputModel() | **JSP 渲染**: L142 exposeModelAsRequestAttributes(model→request attribute) → L151 RequestDispatcher=getRequestDispatcher(url) → 已提交/include→L163 rd.include / 否则→L171 rd.forward — 转发而非输出 | High |
| ViewNameMethodReturnValueHandler.java:72,78 | supportsReturnType()/handleReturnValue() | **String 返回值入口**: supportsReturnType=void 或 CharSequence → L83 mavContainer.setViewName → L103 isRedirectViewName("redirect:"/redirectPatterns)→setRedirectModelScenario | High |
| ContentNegotiatingViewResolver.java:224 | resolveViewName() | **协商视图**: L227 getMediaTypes(ContentNegotiationManager 解析 Accept) → L229 getCandidateViews(遍历委托 resolvers+扩展名尝试+defaultViews) → L230 getBestView(redirect 优先→content-type 兼容→SELECTED_CONTENT_TYPE) → 无匹配: useNotAcceptableStatusCode→NOT_ACCEPTABLE_VIEW(406)/null | High |
| ContentNegotiatingViewResolver.java:106 | order | **顺序要求**: order=HIGHEST_PRECEDENCE — 必须在其他 resolver 之前(否则协商无意义) | High |
| AbstractView.java:303 | render() | **视图模板**: L312 createMergedOutputModel(model+静态属性合并) → L313 prepareResponse(缓存头) → L314 renderMergedOutputModel(抽象, 子类覆写) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+4个解析器+2个视图+入口 handler 约 2500 行 — 但结构是单链: "render 入口 → Resolver 链 → 缓存模板 → URL 构建 → JSP 渲染", 协商视图是分支。1篇 (~48行) 按入口→链→模板→构建→渲染线性展开; 若分 2 篇则"缓存模板"与"URL 构建"的模板方法关联被割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | DispatcherServlet.render→resolveViewNameInternal 链 (视图名→View 两步 + 多 resolver 责任链) | 🔴 | **为什么🔴**: 视图解析的入口骨架 — "逻辑视图名≠物理资源, 由链解析"是 MVC 视图层的核心模型 |
| P1-2 | AbstractCachingViewResolver 模板 (双缓存+UNRESOLVED 哨兵+createView/loadView 钩子) | 🔴 | **为什么🔴**: 所有内置 resolver 的基类 — 缓存/并发/失败缓存全部收敛, 子类只写构建逻辑 |
| P1-3 | UrlBasedViewResolver 三分支 (redirect:/forward: 前缀 + prefix/suffix 构建 + checkResource) | 🔴 | **为什么🔴**: "逻辑视图名→URL 资源"的默认机制 — redirect:/forward: 是表单流程和转发跳转的标配 |
| P2-1 | ContentNegotiatingViewResolver (Accept→候选视图→最匹配) | 🟡 | **为什么🟡**: 与 W-5 §2 内容协商机制同源对照 — 视图层的协商是"视图返回 JSON/HTML 双格式"的关键 |
| P2-2 | ViewNameMethodReturnValueHandler (String→viewName + redirect 识别) | 🟡 | **为什么🟡**: Controller 返回 String 的语义 — 最常用的视图入口 |
| P3-1 | InternalResourceViewResolver+InternalResourceView (JSP: model→request attribute→forward/include) | 🟢 | **为什么🟢**: JSP 是经典但不再是主流 — 机制(转发+属性暴露)值得一提 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **解析链与缓存** (render 入口 + ViewResolver 链 + AbstractCachingViewResolver 模板) | 🔴 | 入口到模板的骨架 — 责任链 + 模板方法 + 双缓存是视图层 80% 的机制 |
| B | **URL 视图构建** (UrlBasedViewResolver + InternalResourceViewResolver + InternalResourceView 渲染) | 🔴 | 默认 JSP 场景的完整落点 — prefix/suffix/redirect/forward/转发 |
| C | **协商与返回值入口** (ContentNegotiatingViewResolver + ViewNameMethodReturnValueHandler) | 🟡 | 两个"入口/出口"变体 — 与 W-5 对照的协商机制 |

> **Cluster A (§1)**: render 入口(viewName→resolveViewName) + ViewResolver 链(首个非 null) + AbstractCachingViewResolver 双缓存模板
> **Cluster B (§2)**: UrlBasedViewResolver createView 三分支(redirect/forward/loadView) + buildView(prefix/suffix) + InternalResourceViewResolver(JSP) + InternalResourceView(forward/include)
> **Cluster C (§3)**: ViewNameMethodReturnValueHandler(String→viewName) + ContentNegotiatingViewResolver(Accept→候选→最匹配) — 与 W-5 写链路协商对照

→ 引出 W-7: 静态资源 (ResourceHttpRequestHandler/DefaultServletHttpRequestHandler — addResourceHandlers) — "URL→资源"的另一分支; W-8 异常处理(@ControllerAdvice/HandlerExceptionResolver — 视图解析异常后的兜底渲染)

(End of file - total 62 lines)
