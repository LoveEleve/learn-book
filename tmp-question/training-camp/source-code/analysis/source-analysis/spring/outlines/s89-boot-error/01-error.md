# S-25 Error 处理自动装配 — ErrorMvcAutoConfiguration → BasicErrorController → DefaultErrorAttributes

> 依赖 S-7 MVC + C-13 异常 (复用) | 🟡 Working | 6 KP | [模式: 条件装配 + 内容协商 + 兜底视图]

**读者处境**: 请求抛异常或 404 → 浏览器出白底错误页、接口出 JSON 错误 — 这是谁处理的?`/error` 端点哪来的?错误信息里的 timestamp/status/path 怎么来的?为什么能自定义?

### 1. 装配入口与条件 — ErrorMvcAutoConfiguration

场景: 加了 spring-boot-starter-web, 错误处理自动生效 — 自动装配怎么知道要创建错误端点?

源码路径:
- `ErrorMvcAutoConfiguration.java:86,87,90` — **条件**: `@AutoConfiguration(before = WebMvcAutoConfiguration.class)`(L86, 先于 MVC 装配) + `@ConditionalOnWebApplication(type = Type.SERVLET)`(L87, 仅 Servlet Web) + `@ConditionalOnClass(Servlet, DispatcherServlet)`(L88)
- `ErrorMvcAutoConfiguration.java:98,99,100` — **errorAttributes**: `@Bean @ConditionalOnMissingBean(ErrorAttributes)`(L99) → `DefaultErrorAttributes`(L100)
- `ErrorMvcAutoConfiguration.java:104,105,106` — **basicErrorController**: `@Bean @ConditionalOnMissingBean(ErrorController)`(L105) → `BasicErrorController(errorAttributes, errorViewResolvers)`(L106)

关键设计: **Why before WebMvcAutoConfiguration？** 错误端点要先注册, 才能在 MVC 装配后接管 /error; @ConditionalOnWebApplication(SERVLET) 保证只在传统 Servlet Web(非 WebFlux)启用; 用户可覆盖(ErrorAttributes/ErrorController 均可自定义)。[模式: 条件装配]

数据流: starter-web → ErrorMvcAutoConfiguration 评估 → @ConditionalOnWebApplication(SERVLET) 命中 → @Bean DefaultErrorAttributes(L100) + @Bean BasicErrorController(L106) 注册。

### 2. 控制器与映射 — BasicErrorController + DefaultErrorAttributes

场景: `/error` 怎么映射?同样一个错误, 浏览器要 HTML、接口要 JSON — 怎么区分?

源码路径:
- `BasicErrorController.java:58` — **路径**: `@RequestMapping("${server.error.path:${error.path:/error}}")`(L58) — 默认 /error, 可用 server.error.path 改
- `BasicErrorController.java:85,86` — **HTML**: `@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)`(L85) → `errorHtml`(L86) 返回 `ModelAndView` — 浏览器走这
- `BasicErrorController.java:95,96` — **JSON**: `@RequestMapping`(L95) → `error`(L96) 返回 `ResponseEntity<Map>` — 接口走这(内容协商)
- `DefaultErrorAttributes.java:95,101,103,104,105,106` — **属性**: getErrorAttributes(L95) → `put("timestamp")`(L103)+`addStatus`(L104)+`addErrorDetails`(L105)+`addPath`(L106) — 组装错误响应体

关键设计: **Why 内容协商(TEXT_HTML vs JSON)？** 同一个 /error 端点按 Accept 头返回不同形式 — 浏览器 Accept text/html 得 ModelAndView 错误页, 接口 Accept application/json 得 ResponseEntity JSON; 一个端点两种响应, 无需两个端点。**Why server.error.path 可配？** 默认 /error 但允许用户改路径, 避免与应用自身路由冲突。[模式: 内容协商]

数据流: 请求异常 → C-13 解析链未处理 → 转发到 /error → BasicErrorController(L58) → 按 Accept: text/html → errorHtml(L86)→ModelAndView(错误页); application/json → error(L96)→ResponseEntity(JSON, 内容来自 DefaultErrorAttributes L101-106)。

### 3. 属性与兜底 — ErrorAttributeOptions + Whitelabel

场景: 错误响应里 trace/message 什么时候有?没配错误模板时那个白底错误页哪来的?

源码路径:
- `DefaultErrorAttributes.java:95,96,97` — **options 控制**: getErrorAttributes(options) → `options.isIncluded(STACK_TRACE)`(L96) + `options.retainIncluded`(L97) — 按 ErrorAttributeOptions 决定是否含 stackTrace/message/exception
- `ErrorMvcAutoConfiguration.java:145` — **whitelabel**: `@ConditionalOnBooleanProperty(name="server.error.whitelabel.enabled", matchIfMissing=true)`(L145) + `ErrorTemplateMissingCondition` — 无 error 模板时用 StaticView 兜底错误页
- `DefaultErrorViewResolver` — ErrorViewResolver: 按状态码解析错误视图(如 404→error/404), 无则 whitelabel

关键设计: **Why options 控制敏感字段？** stackTrace 含敏感栈信息, 默认不含 — 需 server.error.include-stacktrace 才输出; message/exception 同理按需开放, 避免泄露。**Why whitelabel 兜底？** 用户没写 error 模板时, 用 StaticView 输出白底错误页保证"有响应不白屏"; 写了模板则由 ErrorTemplateMissingCondition 判定让位。[模式: 兜底视图 + 按需开放]

数据流: /error 请求 → DefaultErrorAttributes.getErrorAttributes(options)(L95) → options 决定是否含 stackTrace → timestamp/status/error/path 组装 → 视图解析: 有 error 模板 → 自定义错误页; 无模板 + whitelabel.enabled → StaticView 白底页(ErrorMvcAutoConfiguration L145)。

→ 引出 S-26: Actuator Health 聚合 — 错误之后: StatusAggregator/HealthContributorRegistry 的健康状态聚合(前置 S-21)。
