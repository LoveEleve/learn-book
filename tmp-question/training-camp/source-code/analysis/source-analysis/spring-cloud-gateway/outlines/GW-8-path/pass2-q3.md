# 闭环笔记 GW-8-q3 — 共同模式: addOriginalRequestUrl 保留链

假设: 4 种路径过滤器共用 addOriginalRequestUrl — 每次变换把原始 URL 追加进 GATEWAY_ORIGINAL_REQUEST_URL_ATTR (LinkedHashSet), 形成"原始→变换"链 (调试/回退)。

验证过程:
- **addOriginalRequestUrl** (ServerWebExchangeUtils.java:294-296): `computeIfAbsent(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, LinkedHashSet)` + **add** — **追加而非覆盖** (多次变换保留全链)
- **GATEWAY_ORIGINAL_REQUEST_URL_ATTR** (L111): "gatewayOriginalRequestUrl" — 原始 URL 集合
- **4 工厂消费**: RewritePath (L66)/SetPath (L64)/StripPrefix (L66)/PrefixPath (L72) — 全部在变换前调用
- **联动**: 变换后 GATEWAY_REQUEST_URL_ATTR 更新 → GW-4 (lb 解析) / GW-5 (转发) 消费新 URL; 原始链保留 (日志/指标/回退)
- 与 GW-4 的 addOriginalRequestUrl (L104) 是同一工具 — LB 解析前也保留

代码类型: Glue (保留链)

结论: 共同模式 = **变换前保留 + 变换后更新**: 原始 URL 链 (LinkedHashSet 追加) 供诊断; 新 URL 写 GATEWAY_REQUEST_URL_ATTR 供转发链。**被放弃的方案: 只保留最新** — 链式变换 (多个路径过滤器) 需要完整轨迹。 [跨域: GW-4/GW-5 同属性消费; 可观测] [模式: 保留链] (ServerWebExchangeUtils.java:111,294-296)
