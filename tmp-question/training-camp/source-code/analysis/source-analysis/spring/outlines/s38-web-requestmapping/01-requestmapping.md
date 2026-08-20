# W-2 @RequestMapping → HandlerMethod 映射与匹配

> 依赖 W-1 DispatcherServlet | 🟡 Working | 1 KP | [模式: 模板方法]

**读者处境**: W-1 getHandler 调了 HandlerMapping.getHandler → RequestMappingHandlerMapping 如何在数百个 @RequestMapping 方法中找到匹配的那个？

### 1. initHandlerMethods — 启动时扫描所有 @RequestMapping 方法

场景: 启动时容器创建 RequestMappingHandlerMapping → afterPropertiesSet 触发扫描 — 遍历所有 Bean → 找 @Controller/@RequestMapping 方法 → 提取注解信息生成 RequestMappingInfo → 注册进 MappingRegistry — 此后每个请求都从这个注册表查找。

源码路径:
- `AbstractHandlerMethodMapping.java:221` — **initHandlerMethods()**(类声明在 :69): ①L211 afterPropertiesSet→L221 initHandlerMethods→L253 processCandidateBean→L264 `isHandler(beanType)`(@Controller/@RequestMapping) ②L274 `detectHandlerMethods(handler)`→反射 Method[]→过滤@GetMapping/@PostMapping等→`getMappingForMethod(method, userType)`→返回RequestMappingInfo ③L330 `registerHandlerMethod(handler, method, mapping)`→mappingRegistry.register
- `RequestMappingHandlerMapping.java:314` — **getMappingForMethod()**: 读取 @RequestMapping 注解 → `createRequestMappingInfo(mapping, condition)` → paths/methods(GET/POST)/params/headers/consumes/produces

数据流: DispatcherServlet init→initHandlerMappings→getBean(RequestMappingHandlerMapping)→afterPropertiesSet→initHandlerMethods→beanNames: "userController"→isHandler→@Controller→true→detectHandlerMethods→for method: getUser(@PathVariable Long id)→getMappingForMethod→@GetMapping("/users/{id}")→paths=["/users/{id}"]→methods=[GET]→produces=[application/json]→new RequestMappingInfo→registerHandlerMethod(userController, getUser, info)→mappingRegistry.register(info, new HandlerMethod(userController, getUser))→MappingRegistry(内部类 :573)四个字段: L575 registry: {T→MappingRegistration<T>} + L577 pathLookup: {字面路径→[T]}(含"/users/{id}") + L579 nameLookup: {mappingName→[HandlerMethod]} + L581 corsLookup: {HandlerMethod→CorsConfiguration}

关键设计: **Why 扫描在启动时一次性完成？** initHandlerMethods 是模板方法: afterPropertiesSet(L211) 触发 → initHandlerMethods(L221) 遍历 beanNames → isHandler 过滤 → detectHandlerMethods 反射扫描 → registerHandlerMethod 注册。扫描成本只付一次, 请求时只查注册表(零反射)— 这是"启动扫描 + 运行查找"的两阶段分离 — 与 W-4 的"匹配阶段写 attribute、解析阶段读 attribute"同理。[模式: 模板方法 — 扫描骨架固定, getMappingForMethod 子类覆写]

### 2. getHandler — 运行时 URL + Method 最佳匹配

场景: GET /users/1 → DispatcherServlet.getHandler → RequestMappingHandlerMapping.getHandlerInternal → lookupHandlerMethod(L400) → L402 pathLookup.get("/users/1")直接路径命中(无通配符查找) → candidates → 遍历: method匹配(GET) + params匹配 + headers匹配 + produces匹配 → 最佳得分 bestMatch → bestMatch.getHandlerMethod() → 返回 HandlerMethod。

源码路径:
- `AbstractHandlerMethodMapping.java:400` — **lookupHandlerMethod()**: 运行时匹配入口 — 字面路径优先 → 未命中回退全量匹配
- `AbstractHandlerMethodMapping.java:410` — **bestMatch 选择**: matches 按 score 排序取最优; :438 `bestMatch.getHandlerMethod()` 返回最终 HandlerMethod
- `AbstractHandlerMethodMapping.java:573` — **MappingRegistry 内部类**: 四个查找结构(L575-581)

数据流: GET /users/1 → DispatcherServlet.doDispatch→getHandler→handlerMappings遍历→RequestMappingHandlerMapping.getHandler(request)→getHandlerInternal→lookupHandlerMethod(L400)→lookupPath="/users/1"→L402 pathLookup.get("/users/1")→仅字面路径命中[rmInfo1(GET /users/{id})](通配符模式不在 pathLookup 中)→直接路径未命中时 L406-407 回退遍历 mappingRegistry.getRegistrations().keySet() 全部映射→遍历→rmInfo1.getMethodsCondition().getMatchingCondition(request)→GET==GET→match→rmInfo1.getParamsCondition().getMatchingCondition(request)→无附加参数→match→rmInfo1.getProducesCondition().getMatchingCondition(request)→Accept:application/json→match→bestMatch=rmInfo1(L410)→L438 bestMatch.getHandlerMethod()→HandlerMethod(UserController.getUser)→new HandlerExecutionChain(handlerMethod, interceptors)→返回→W-1继续

关键设计: **Why pathLookup 只存字面路径？** URL 模板 `/users/{id}` 与正则通配 `/users/*` 不同 — 模板路径需要运行时展开参数绑定, 无法作为字面 key 匹配 — 所以 pathLookup 只收录"无模板变量"的直接路径; 含 `{}` 的映射走 L406-407 的全量回退匹配。

回退匹配用条件组合(method/params/headers/produces)逐条求交集评分选出 bestMatch — 这是"模板路径 + 多条件评分"的匹配策略, 而非 URL 字符串哈希。

**评分机制**: 直接路径命中时 candidate 可能多个(如 `/users/1` 同时命中字面映射和 `/users/{id}` 模板)— 每个候选按 condition 求 `getMatchingCondition` — L410-434 用 MatchComparator(getMappingComparator, 子类 L118 委托 RequestMappingInfo.compareTo L449)排序 — compareTo 先比 methodsCondition(L453)再比 patternsCondition(L458, 字面路径更具体排前)— 取 bestMatch; 同分时抛 IllegalStateException("Ambiguous handler methods mapped" L423-431)。bestMatch 在 L410 选出后经 L438 取 HandlerMethod。

**两阶段设计**: 启动(initHandlerMethods)做"扫描+注册" — 把注解变成可查找的注册表(registry/pathLookup/nameLookup/corsLookup); 运行时(getHandler)做"纯查找" — 只读注册表不重新反射。

扫描成本只付一次, 匹配成本每次请求付 — 这是"编译期索引"思想的运行时版(与 S2-14 AOT 的静态化思想一致)。

→ spring-web 第二域完成。@RequestMapping→HandlerMethod 启动扫描+运行时匹配。引出 W-3: RequestMappingHandlerAdapter — HandlerMethod 如何被调用(invoke)。
