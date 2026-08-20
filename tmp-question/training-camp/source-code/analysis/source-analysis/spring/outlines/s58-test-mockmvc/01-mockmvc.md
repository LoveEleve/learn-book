# C-16 MockMvc — 控制器测试 (装配 → perform → andExpect)

> 依赖 W-1 DispatcherServlet | 🟡 Working | 6 KP | [模式: 建造者 + 门面 + 复用]

**读者处境**: `mockMvc.perform(get("/users")).andExpect(status().isOk())` — 没启动 Tomcat 怎么测 Controller？MockMvc 和真实请求什么关系？为什么要用 webAppContextSetup/standaloneSetup 两种？

### 1. MockMvcBuilders — 两种装配模式

场景: 测试 Controller 有两条路: 加载完整 Spring 容器(测真实配置/依赖) vs 只测单个控制器(快、隔离)。MockMvcBuilders 提供两个入口。

源码路径:
- `MockMvcBuilders.java:37,51,76` — **两种构建**: `webAppContextSetup(ctx)`(L51, 传完整 WebApplicationContext — 走真实配置/Bean) → DefaultMockMvcBuilder; `standaloneSetup(controllers...)`(L76, 只注册指定控制器+最小配置) → StandaloneMockMvcBuilder — 默认装配 ArgumentResolver/MessageConverter 等
- MockMvc 构造(L71): 持 TestDispatcherServlet + filters + 默认结果处理器

关键设计: **Why 两种模式？** webAppContextSetup 是"集成测试"(真实容器配置, 能测到配置错误/Bean 依赖, 慢); standaloneSetup 是"控制器单元测试"(只测方法逻辑, 不启动全容器, 快且隔离) — 对应测试金字塔的两层。[模式: 建造者 — 双入口]

数据流: webAppContextSetup(ctx) → 用 MockServletContext 初始化 ctx → DefaultMockMvcBuilder → 创建 TestDispatcherServlet(注入 ctx) → MockMvc(servlet)。standaloneSetup(ctrl) → 建一个含该控制器的"最小 MVC 配置"(HandlerMapping/Adapter/Resolver) → MockMvc。

### 2. MockMvc.perform — 走真实 MVC 全链路

场景: perform(get("/users")) 内部怎么变成一次"真实"的请求处理？关键: 复用真实 DispatcherServlet + HandlerMapping + Adapter, 只是 mock 了 Servlet 容器。

源码路径:
- `MockMvc.java:167` — **perform()**: requestBuilder.buildRequest(构建 MockHttpServletRequest) → 默认请求合并(Mergeable) → 建 MockHttpServletResponse → `MockFilterChain.doFilter(request, response)`(L203)
- `MockMvc.java:200` — **MockFilterChain**: 调用链上的 Filter, 终点是 this.servlet(TestDispatcherServlet) — 触发完整的 doDispatch
- `TestDispatcherServlet.java:53,67` — **真实 DispatcherServlet**: extends DispatcherServlet — service 覆写处理 Mock 请求 — W-1 的 doDispatch/HandlerMapping/Adapter/渲染全复用

关键设计: **Why 复用真实 DispatcherServlet 而非模拟？** MockMvc 的价值正是"真实 MVC 逻辑可测" — 控制器、@RequestBody、消息转换、异常解析、视图渲染全走真实代码, 只有"容器/网络/Servlet API"被 mock — 这样测的是真实行为而非替身。[模式: 复用 — mock 环境, 真逻辑]

数据流: perform(get("/users")) → MockHttpServletRequestBuilder.buildRequest → MockHttpServletRequest(GET /users) → MockFilterChain.doFilter → TestDispatcherServlet.service → doDispatch(W-1): getHandler→HandlerMapping → getHandlerAdapter → invokeHandlerMethod → 控制器返回 → 消息转换/渲染 → MockHttpServletResponse 填充 → applyDefaultResultActions → ResultActions。

### 3. ResultActions 断言 — andExpect + MockMvcResultMatchers

场景: 结果怎么校验？状态码、响应内容、JSON 字段、重定向、视图名 — ResultMatcher 断言 MvcResult。

源码路径:
- `MockMvc.java:214,220,224` — **ResultActions**: andExpect(matcher)→matcher.match(mvcResult)(L214) / andDo(handler→handle)(L220, 打印/自定义) / andReturn(L224→MvcResult 拿响应/模型)
- `MockMvcResultMatchers.java:45,166,180` — **断言工厂**: status()(L166, StatusResultMatchers: isOk/is4xxClientError)、content()(L180: json/contentType/string)、view()(L74)、redirectedUrl(L129)、jsonPath(L193, JSONPath 表达式)
- `MockHttpServletRequestBuilder.java:54,73,153` — **请求构建**: uri(L73)/param(L153)/content(L113)/header(L143) — get/post 等动词方法

关键设计: **Why 断言也链式(andExpect 返回 this)？** 一个响应可断言多项(状态+内容+头) — 链式累积校验; ResultMatcher 是"校验 MvcResult 的断言函数", 与 ResultHandler("有副作用的处理")分离 — 区分"验证"与"动作"。[模式: 门面 + 断言收集]

数据流: mockMvc.perform(get("/users")) → .andExpect(status().isOk()) → StatusResultMatchers.match: 校验 mvcResult.getResponse().getStatus()==200 → .andExpect(content().json("[...]")) → json 校验 → .andExpect(jsonPath("$[0].id").value(1)) → JSONPath 定位 → 全部通过→测试绿。失败→断言异常→测试红。

→ 引出 8-2: TestContext — webAppContextSetup 背后是 TestContext 框架: 测试类怎么找到并缓存 ApplicationContext(@SpringBootTest/@ContextConfiguration/TestExecutionListener)。
