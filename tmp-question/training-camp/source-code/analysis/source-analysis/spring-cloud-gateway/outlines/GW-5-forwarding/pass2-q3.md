# 闭环笔记 GW-5-q3 — 请求体缓存: AdaptCachedBodyGlobalFilter + 事件触发

假设: 请求体缓存是"按路由开关"的 — EnableBodyCachingEvent 标记要缓存的路由, AdaptCachedBodyGlobalFilter 只对这些路由缓存 body (供下游过滤器/谓词二次读取)。

验证过程:
- **AdaptCachedBodyGlobalFilter** (AdaptCachedBodyGlobalFilter.java:37-81): implements GlobalFilter + Ordered + **ApplicationListener<EnableBodyCachingEvent>** (L37)
- **触发** (L60-66): 路由的 body 已缓存或不在 routesToCache → 直接 next; 否则 **cacheRequestBody** (L66, ServerWebExchangeUtils) — 缓存后 mutate().request(替换请求) (L71-75)
- **order** (L80): `Ordered.HIGHEST_PRECEDENCE + 1000` — **链最前置** (在一切读 body 的过滤器之前)
- **EnableBodyCachingEvent** (event/): 由 SetRequestHostHeader 等过滤器工厂在配置时发布 — 声明"该路由要缓存 body"
- 消费端: ReadBody 谓词 (GW-3)/RewritePath 等读 body 的过滤器 — **body 只读一次, 缓存重放**
- 关联: RemoveCachedBodyFilter (链尾清理)

代码类型: Implementation (缓存编排)

结论: 请求体缓存 = **事件声明 + 前置过滤器**: 过滤器工厂发布 EnableBodyCachingEvent 声明需求, AdaptCachedBodyGlobalFilter (最高优先级前置) 按路由缓存 body; 谓词/过滤器二次读取不冲突 (GW-3 ReadBody 依赖此)。**被放弃的方案: 无条件缓存所有 body** — 内存成本; 按需声明 (routesToCache Map)。 [跨域: GW-3 ReadBody 谓词消费; GW-2 链 order 协作] [内存: 按路由缓存] (AdaptCachedBodyGlobalFilter.java:37-81)
