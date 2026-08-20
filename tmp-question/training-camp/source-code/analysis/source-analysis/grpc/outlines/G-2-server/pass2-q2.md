# 闭环笔记 Q2 — InternalHandlerRegistry: 服务注册的扁平化快照

假设: 注册表在 build 时把 service 扁平化为 method 映射, 请求查找是 O(1) Map 读。

验证过程:
- grep `class InternalHandlerRegistry` → `extends HandlerRegistry` (InternalHandlerRegistry.java:30), 字段: services List + **methods Map (L33)**
- grep `addService` → Builder 按 service 名存 LinkedHashMap (L59-65, "services are added/replaced atomically" — 同名校覆盖)
- grep `build()` → **扁平化**: 遍历 service 全部 method, `map.put(method.getMethodDescriptor().getFullMethodName(), method)` (L67-78) — fullMethodName = "pkg.Service/Method" 作 key; 产出不可变 List/Map
- grep `lookupMethod` → `methods.get(methodName)` (L51-54) + TODO "honor authority header" — 单 key O(1); authority 参数暂未用
- ServerImpl.streamCreated 使用: `registry.lookupMethod(methodName, null)` (ServerImpl.java:501) → null 时 `fallbackRegistry.lookupMethod(methodName, stream.getAuthority())` (L545, 反射注册的 fallback) → 还 null → **UNIMPLEMENTED "Method not found: "** (L548-549) + NOOP_LISTENER + context.cancel (L555-558)

代码类型: Implementation (查找结构)

结论: 注册表 = **构建时快照** (不可变 Map, 无锁读); 服务注册是"名覆盖原子替换" (HashMap.put 语义); 请求路径 O(1) 查找 + 双注册表 (主 registry + fallbackRegistry) + UNIMPLEMENTED 闭环。**被放弃的方案: 请求时遍历 service 匹配** — 快照扁平化把 O(n) 匹配降为 O(1) 哈希; 不可变 Map 免并发控制。 [跨域: fullMethodName 来自 G-1 生成面] (InternalHandlerRegistry.java:30-78; ServerImpl.java:501,545-559)
