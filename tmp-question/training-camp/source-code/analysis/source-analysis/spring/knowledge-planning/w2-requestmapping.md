# W-2 RequestMappingHandlerMapping — @RequestMapping→HandlerMethod 映射

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | AbstractHandlerMethodMapping(852行)+RequestMappingHandlerMapping
> 基线: W-1 DispatcherServlet — getHandler 遍历 HandlerMapping → RequestMappingHandlerMapping 返回匹配的 HandlerMethod

---

## §0.8

- 🟡 Working，1篇 — AbstractHandlerMethodMapping.initHandlerMethods(启动扫描) → detectHandlerMethods → registerHandlerMethod → MappingRegistry(registry/pathLookup/nameLookup/corsLookup) → getHandler(运行时匹配)
- 设计模式: [模式: 模板方法]—initHandlerMethods 骨架固定, getMappingForMethod 子类覆写

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AbstractHandlerMethodMapping.java:69 | initHandlerMethods() | **启动扫描**: afterPropertiesSet → initHandlerMethods → beanNames.forEach → detectHandlerMethods(bean) → 反射扫描 @RequestMapping/@GetMapping 等 → registerHandlerMethod | High |
| RequestMappingHandlerMapping.java:314 | getMappingForMethod() | **子类覆写**: 读取 @RequestMapping/@GetMapping/@PostMapping → 创建 RequestMappingInfo(paths/methods/params/headers/consumes/produces) → 返回T(映射key) | High |
| AbstractHandlerMethodMapping.java:MappingRegistry | 注册表 | registry(Map<T,MappingRegistration>)—按映射Key查找 + pathLookup(字面路径→[T])—直接路径快速查找 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 852行+子类 — 核心是启动扫描(initHandlerMethods)+请求匹配(getHandler→lookupHandlerMethod→urlLookup+最佳匹配)。1篇(~40行)。

**P1 核心** 🔴: AbstractHandlerMethodMapping 启动扫描 → 子类 getMappingForMethod → MappingRegistry 注册 → 运行时匹配 — **为什么** 🔴** 🔴: @Controller/@RequestMapping 注解如何被发现并注册为 HandlerMethod — 启动时扫描所有 Bean → @Controller → 找 @RequestMapping 方法 → 创建 HandlerMethod → 按 URL + HTTP method + params 等条件注册到 MappingRegistry → W-1 的 getHandler 从这个 Registry 匹配
