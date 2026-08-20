# 闭环笔记 GW-1-q6 — 路由管理端点: actuator 动态增删路由 (v3.1 补充)

假设: actuator 不是可观测端点 — 它是路由的动态管理 API: POST/DELETE 运行时增删路由, refresh 手动刷新, GET 查询工厂。

验证过程:
- **save** (AbstractGatewayControllerEndpoint.java:238): `@PostMapping("/routes/{id}")` + `@RequestBody RouteDefinition route` — **运行时保存路由**; 类注释示例 (L232): "http POST :8080/admin/gateway/routes/apiaddreqhead uri=http://httpbin.org:80" — 一行 curl 加路由
- **refresh** (L178-179): `@PostMapping("/refresh")` — 手动触发 RefreshRoutesEvent (q3 事件面)
- **delete** (L327): `@DeleteMapping("/routes/{id}")` — 动态删除
- **查询面** (L202-213): `@GetMapping("/globalfilters")` (L202)/routefilters (L207)/routepredicates (L212) — 查看已注册工厂; combinedfilters (L335) — 路由的组合过滤器
- **实现**: GatewayControllerEndpoint (101) + GatewayLegacyControllerEndpoint (113, 旧版) extends AbstractGatewayControllerEndpoint (342)
- 管理通道: 走 RouteDefinitionWriter (动态写入 → RouteDefinitionRepository → 刷新)

代码类型: Implementation (管理 API 面)

结论: 管理端点 = **运行时路由治理**: 动态增删路由 (免重启/免改配置)/手动刷新/工厂查询 — 网关运维的定义特征。**被放弃的方案: 只靠配置重启** — 动态路由让流量调整零停机 (灰度/应急切换)。与 GW-1 的 Repository/Refresh 事件面闭环 (q3)。 [跨域: GW-1 路由模型; 运维面 (对照 Envoy admin API)] [HTTP: REST 管理面] (AbstractGatewayControllerEndpoint.java:151,202-213,238,327,335)
