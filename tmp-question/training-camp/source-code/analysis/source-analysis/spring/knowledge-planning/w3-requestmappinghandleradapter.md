# W-3 RequestMappingHandlerAdapter — HandlerMethod 的反射调用链

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | RequestMappingHandlerAdapter(1150行)+InvocableHandlerMethod(384行)+ServletInvocableHandlerMethod(330行)
> 基线: W-2 RequestMappingHandlerMapping — getHandler 返回 HandlerMethod → W-1 doDispatch 的 `ha.handle()` 调用本适配器

---

## §0.8

- 🟡 Working，1篇 — RequestMappingHandlerAdapter.handleInternal → invokeHandlerMethod → createInvocableHandlerMethod → ServletInvocableHandlerMethod.invokeAndHandle → InvocableHandlerMethod.invokeForRequest → getMethodArgumentValues(resolver链解析参数) → doInvoke(反射调用) → returnValueHandlers 处理返回值
- 设计模式: [模式: 适配器模式]—HandlerAdapter 接口统一不同 Handler 类型; [模式: 策略模式]—26 个 ArgumentResolver + 返回值处理器链
- 核心文件: RequestMappingHandlerAdapter(1150行)/InvocableHandlerMethod(384行)/ServletInvocableHandlerMethod(330行)

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| RequestMappingHandlerAdapter.java:868 | supportsInternal() | **支持判断**: 返回 true — 所有 HandlerMethod 都由本适配器处理 | High |
| RequestMappingHandlerAdapter.java:874 | handleInternal() | **入口**: checkRequest → (可选 synchronizeOnSession 同步) → invokeHandlerMethod → 缓存头处理 | High |
| RequestMappingHandlerAdapter.java:941 | invokeHandlerMethod() | **核心装配**: 创建 ServletWebRequest → getDataBinderFactory → getModelFactory → createInvocableHandlerMethod → 注入 argumentResolvers/returnValueHandlers → modelFactory.initModel → invokeAndHandle → getModelAndView | High |
| RequestMappingHandlerAdapter.java:1005 | createInvocableHandlerMethod() | **工厂方法**: new ServletInvocableHandlerMethod(handlerMethod) — 子类可覆写 | High |
| RequestMappingHandlerAdapter.java:590 | afterPropertiesSet() | **初始化**: initControllerAdviceCache → 默认 argumentResolvers(26个)/returnValueHandlers 装配 → messageConverters 设置 | High |
| RequestMappingHandlerAdapter.java:685 | getDefaultArgumentResolvers() | **26个默认解析器**: @RequestParam/@PathVariable/@RequestHeader/@RequestBody/@ModelAttribute/Map/Model/Errors + 类型匹配(WebRequest/HttpEntity) + catch-all | High |
| InvocableHandlerMethod.java:178 | invokeForRequest() | **参数解析+调用**: getMethodArgumentValues → (可选参数校验) → doInvoke → (可选返回值校验) | High |
| InvocableHandlerMethod.java:207 | getMethodArgumentValues() | **参数解析**: 遍历 methodParameters → findProvidedArgument → resolvers.supportsParameter → resolveArgument | High |
| InvocableHandlerMethod.java:247 | doInvoke() | **反射调用**: getBridgedMethod → Kotlin 分支 → method.invoke(getBean(), args) → IllegalArgumentException 包装 | High |
| ServletInvocableHandlerMethod.java:115 | invokeAndHandle() | **MVC 装配**: invokeForRequest → setResponseStatus(@ResponseStatus) → null返回值特判 → returnValueHandlers.handleReturnValue | High |
| ServletInvocableHandlerMethod.java:150 | setResponseStatus() | **@ResponseStatus**: 设置响应状态码 + reason → 短路请求处理 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 1150+384+330 行 — 核心是"HandlerMethod → 参数解析 → 反射调用 → 返回值处理"的完整调用链。1篇 (49行) 覆盖从 handleInternal 到 method.invoke 的全链路; W-4 域再展开 ArgumentResolver 细节。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | invokeHandlerMethod 装配链 (binderFactory/modelFactory/invocableMethod 注入) | 🔴 | **为什么🔴**: 这是"HandlerMethod 元数据→可调用方法"的装配枢纽 — 数据绑定/模型初始化/参数解析器全部在此注入, 缺它 HandlerMethod 无法执行 |
| P1-2 | InvocableHandlerMethod.invokeForRequest→getMethodArgumentValues→doInvoke 反射调用链 | 🔴 | **为什么🔴**: 从"参数数组生产"到"反射调用"的完整链 — 每个参数独立解析是 MVC 参数绑定的核心机制 |
| P1-3 | ServletInvocableHandlerMethod.invokeAndHandle (返回值处理 + @ResponseStatus 短路) | 🔴 | **为什么🔴**: 决定"渲染视图 vs 直接响应"的分界 — requestHandled 短路是 MVC 响应装配的关键开关 |
| P2-1 | HandlerAdapter 接口适配模式 (supports/handle 双方法契约) | 🟡 | **为什么🟡**: 三类 Handler 需要三类 Adapter — supports 匹配契约是策略选择的入口 |
| P2-2 | 26 个默认 ArgumentResolver 的分类体系 (注解/类型/catch-all) | 🟡 | **为什么🟡**: 理解"参数如何被解析"的全局视图 — W-4 逐类展开的基础 |
| P3-1 | afterPropertiesSet 初始化时机 (resolver 装配 + ControllerAdvice 缓存) | 🟢 | **为什么🟢**: 装配时机决定"何时可用" — 属于初始化细节, 非核心机制 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **HandlerMethod 装配与反射调用链** (invokeHandlerMethod→invokeAndHandle→doInvoke) | 🔴 | 这是 MVC 从"请求匹配"到"业务方法执行"的最后一段 — 理解参数如何变成反射调用是 MVC 核心 |
| B | **HandlerAdapter 适配模式** (supports/handle + resolver 策略链) | 🟡 | 理解"为什么要有 Adapter"和"为什么参数解析是策略链" — 架构层面的可扩展性设计 |

> **Cluster A (§1-§2)**: RequestMappingHandlerAdapter 装配链 + InvocableHandlerMethod 反射调用 + invokeAndHandle 返回值处理
> **Cluster B (§3)**: HandlerAdapter 接口契约 + ArgumentResolver 分类体系 + afterPropertiesSet 装配

→ 引出 W-4: HandlerMethodArgumentResolver — 26 个 resolver 中 @PathVariable/@RequestParam/@RequestBody 的具体解析逻辑 (本域只讲调用链, 不展开解析器内部)
