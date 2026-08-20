# W-4 HandlerMethodArgumentResolver — 参数绑定三机制

> 依赖 W-3 RequestMappingHandlerAdapter | 🟡 Working | 3 KP | [模式: 模板方法 + 策略模式]

**读者处境**: W-3 中 getMethodArgumentValues 调了 `resolvers.supportsParameter` → `resolveArgument` — 但每个 resolver 内部怎么工作？@PathVariable 怎么从 URL 提取值？@RequestParam 怎么读 query 参数？@RequestBody 怎么反序列化 JSON？三种机制完全不同。

### 1. 接口契约 + AbstractNamedValueMethodArgumentResolver 模板 — @PathVariable/@RequestParam 的共用骨架

场景: `@GetMapping("/users/{id}") public User getUser(@PathVariable Long id, @RequestParam(defaultValue="0") int page)` — 两个参数注解不同, 但都走 AbstractNamedValueMethodArgumentResolver 的同一模板: 名字解析 → 取原始值 → 缺省值/必填判断 → 类型转换。

源码路径:
- `HandlerMethodArgumentResolver.java:42,60` — **接口双方法**: supportsParameter 判断"这个参数归我管吗"(true 才轮到 resolveArgument) → resolveArgument 四参签名(parameter/mavContainer/webRequest/binderFactory)返回解析值
- `AbstractNamedValueMethodArgumentResolver.java:104` — **resolveArgument() 模板**: L107 getNamedValueInfo(注解属性, 缓存) → L113 resolveEmbeddedValuesAndExpressions(name, ${} 解析) → L119 resolveName(子类实现取原始值) → L121-133 null→defaultValue/required→handleMissingValue + handleNullValue → L136-144 convertIfNecessary(WebDataBinder 类型转换) → L149 handleResolvedValue
- `AbstractNamedValueMethodArgumentResolver.java:157,173,220` — **三个协作方法**: getNamedValueInfo(ConcurrentHashMap 256 缓存, 每参数只解析一次) → createNamedValueInfo(抽象: 子类读注解→NamedValueInfo{name,required,defaultValue}) → resolveName(抽象: 按名字取原始 String)

关键设计: **Why 抽象出 AbstractNamedValue 基类？** @PathVariable 和 @RequestParam 的差异只在"从哪取值"(URL 模板变量 vs query 参数)— 但"取到值之后的处理"完全一样: 名字解析/缺省值/必填校验/类型转换/缺失异常。模板方法把共性固定, 两个抽象方法(createNamedValueInfo + resolveName)留变化点 — 新增一个"命名值"注解只需覆写这两个方法。**参数解析的 80% 逻辑在基类, 子类只写 20% 差异。** [模式: 模板方法]

数据流: getMethodArgumentValues 遍历参数 → 参数0 `@PathVariable Long id` → resolvers.supportsParameter: RequestParamMethodArgumentResolver.supportsParameter(L127) 无 @RequestParam 注解 → false → ... → PathVariableMethodArgumentResolver.supportsParameter(L72) 有 @PathVariable → true → resolveArgument(L104) → L107 getNamedValueInfo: 缓存未命中 → createNamedValueInfo → 读 @PathVariable → NamedValueInfo{name="id", required=true, defaultValue=null} → L113 resolveEmbeddedValuesAndExpressions("id") → L119 resolveName("id", ...): request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST) → Map{id:1} → get("id") → "1" → L136 convertIfNecessary: WebDataBinder.convertIfNecessary("1", Long) → 1L → L149 handleResolvedValue → 返回 1L → 参数1 `@RequestParam(defaultValue="0") int page` → RequestParamMethodArgumentResolver.supportsParameter(L127) → true → 同一模板: resolveName → request.getParameterValues("page") → null(URL 无 page) → L121 defaultValue="0" → arg="0" → convertIfNecessary("0", int) → 0 → 返回。

### 2. @PathVariable / @RequestParam 的具体取值 — 两个子类的差异点

场景: 参数值到底从哪来？@PathVariable 从 URL 路径 `/users/1` 的 `{id}` 段 — 这个值在 W-2 的 RequestMappingInfoHandlerMapping 匹配时写入 request attribute。@RequestParam 从 query string `?page=0` 或 form body — 由 Servlet 容器解析。

源码路径:
- `PathVariableMethodArgumentResolver.java:72` — **supportsParameter()**: 有 @PathVariable → true; Map 类型需显式 value()(否则歧义)
- `PathVariableMethodArgumentResolver.java:93,100` — **resolveName()**: `request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)` → Map<String,String> → get(name) — attribute 由 RequestMappingInfoHandlerMapping.handleMatch 写入(L182); 缺失 → handleMissingValue 抛 `MissingPathVariableException(name, parameter)`
- `RequestParamMethodArgumentResolver.java:127` — **supportsParameter()**: @RequestParam → true; 无注解 → multipart 参数 true / useDefaultResolution(兜底版)且简单类型 true
- `RequestParamMethodArgumentResolver.java:162` — **resolveName()**: multipart 优先 → request.getParameterValues(name) → null 时 name+"[]" 兜底 → 单值取 [0], 多值返回数组

关键设计: **Why @PathVariable 的值是"写进 request attribute"而非直接解析 URL？** URI 模板匹配在 W-2 的 HandlerMapping 阶段完成 — 解析出的变量 Map 写入 request attribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)— HandlerAdapter 阶段的 resolver 只是**读取**。两阶段分离: 匹配(HandlerMapping)与取值(Resolver)解耦 — 这也是 @PathVariable(读 attribute)和 @RequestParam(读 Servlet 参数)取值路径不同的根源。[模式: 责任链 — HandlerMapping 写, Resolver 读]

数据流: @RequestParam int page → RequestParamResolver.resolveName(L162) → request.getParameterValues("page") → ["0"] → 单值取 [0] → "0" → convertIfNecessary → 0。多值场景: `?tag=a&tag=b` → ["a","b"] → 返回数组。缺失场景: URL 无 page 参数且无 defaultValue → resolveName 返回 null → required=true → handleMissingValue → 抛 MissingServletRequestParameterException。若 URL 的 {id} 段缺失(@PathVariable) → resolveName 读 URI_TEMPLATE_VARIABLES_ATTRIBUTE 为 null → 同样 required 路径 → 抛 MissingPathVariableException → 500。

### 3. @RequestBody — HttpMessageConverter 链的请求体反序列化

场景: `@PostMapping("/users") public User createUser(@Valid @RequestBody UserDto dto)` — 请求体是 JSON `{"name":"Alice"}` — 需要: ①确定 Content-Type ②找到能反序列化 JSON→UserDto 的转换器 ③读 body 转对象 ④@Valid 校验。这条链完全不同于命名值解析 — 不走 AbstractNamedValue 模板。

源码路径:
- `RequestResponseBodyMethodProcessor.java:128` — **supportsParameter()**: 仅 `hasParameterAnnotation(RequestBody.class)`
- `RequestResponseBodyMethodProcessor.java:146` — **resolveArgument()**: L150 readWithMessageConverters(读 body) → L153-155 WebDataBinder 绑定(getVariableNameForParameter + createBinder) → L157 validateIfApplicable(@Valid 校验) → L159 校验失败 → 抛 MethodArgumentNotValidException → L167 adaptArgumentIfNecessary; required 检查在 checkRequired(L184): @RequestBody.required() && !isOptional → body 缺失抛 HttpMessageNotReadableException
- `AbstractMessageConverterMethodArgumentResolver.java:148` — **readWithMessageConverters()**: L161 contentType = inputMessage.getHeaders().getContentType() → L169 缺失 → APPLICATION_OCTET_STREAM → L179 遍历 messageConverters → L183 GenericHttpMessageConverter.canRead(targetType, contextClass, contentType) / L191 SmartHttpMessageConverter.canRead / L195 HttpMessageConverter.canRead(targetClass, contentType) → 第一个可读的转换器执行

关键设计: **Why @RequestBody 不继承 AbstractNamedValue 模板？** 命名值解析的模型是"**按名字取一个值**" — 但请求体是"**整个 body 转换成一个对象**" — 没有"名字"概念。且 body 需要 HttpMessageConverter 链(策略选择: JSON→MappingJackson2HttpMessageConverter / String→StringHttpMessageConverter / 字节→ByteArrayHttpMessageConverter)— 这是"目标类型+Content-Type 双维度匹配" — 与命名值的"名字查找"本质不同。所以 @RequestBody 走独立的 AbstractMessageConverterMethodArgumentResolver 链。[模式: 策略模式 — messageConverters 按 canRead 匹配]

数据流: POST /users Content-Type:application/json, body={"name":"Alice"} → getMethodArgumentValues → @Valid @RequestBody UserDto dto → RequestResponseBodyMethodProcessor.supportsParameter(L128) → true → resolveArgument(L146) → L150 readWithMessageConverters → createInputMessage(包装 ServletServerHttpRequest) → 父类 readWithMessageConverters(L148): contentType=application/json → L179 遍历 messageConverters: MappingJackson2HttpMessageConverter.canRead(UserDto, application/json) → Jackson 泛型匹配 → true → converter.read(UserDto, inputMessage) → ObjectMapper.readValue(body, UserDto) → UserDto{name:"Alice"} → L155 WebDataBinder.createBinder → L157 validateIfApplicable: dto 有 @Valid → validator.validate(dto) → 校验通过 → L167 adaptArgumentIfNecessary → 返回 UserDto。若校验失败 → L158-159 MethodArgumentNotValidException → 400 错误。若 body 缺失且 required=true → checkRequired(L184) → HttpMessageNotReadableException。

**扩展: 自定义 resolver** — 三种内置机制怎么选？@PathVariable(REST 路径段) / @RequestParam(query/form) / @RequestBody(整个 body)。自定义参数类型(如 Spring Security 的 `@AuthenticationPrincipal User currentUser`)只需: 实现接口双方法(supportsParameter 判断"何时启用" + resolveArgument 决定"如何解析"— 从 SecurityContextHolder 取 principal) + 注册到 setCustomArgumentResolvers(RequestMappingHandlerAdapter.java:223 / getDefaultArgumentResolvers 的 custom 组 L719-721, 顺序在默认 26 个之后、catch-all 之前) — 不改框架任何代码。三机制各自的差异点: 命名值模板(@PathVariable/@RequestParam) vs 消息体链(@RequestBody) — 同一个接口统一分派。[模式: 策略模式 + 模板方法]

→ 引出 W-5: HttpMessageConverter — messageConverters 链的 canRead/canWrite + MappingJackson2HttpMessageConverter 如何与 Jackson ObjectMapper 集成 — @RequestBody 读 / @ResponseBody 写。
