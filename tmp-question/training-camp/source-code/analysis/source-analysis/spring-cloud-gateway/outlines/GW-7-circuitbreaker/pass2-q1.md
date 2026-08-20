# 闭环笔记 GW-7-q1 — 熔断编排: cb.run 包装 + 状态码熔断

假设: 熔断过滤器把链包进 ReactiveCircuitBreaker.run: 成功但响应码在配置集 → 视为失败 (熔断); 失败 → fallback 或错误处理。

验证过程:
- **工厂** (SpringCloudCircuitBreakerFilterFactory.java:55-95): 抽象类; `reactiveCircuitBreakerFactory.create(config.getId())` (L95) — **SCC 熔断工厂按 id 建断路器**
- **包装** (L100-109): `cb.run(chain.filter(exchange).doOnSuccess(v -> { if (statuses.contains(statusCode)) throw new CircuitBreakerStatusCodeException(status); }), ...)` — **状态码熔断**: 链成功但响应码在配置集 (如 500/502) → 抛异常视为失败 — **成功语义扩展**
- **失败分支** (L110-135): fallbackUri 存在 → 重构请求 URI → GATEWAY_REQUEST_URL_ATTR → handle(DispatcherHandler) (L142); 无 fallback → `handleErrorWithoutFallback(t, resumeWithoutError)` (L143)
- **resumeWithoutError** (L143): 可配 — 失败时继续链 (放行) vs 抛错
- 状态码配置: Config.statusCodes (L96-102, HttpStatusHolder 解析)
- 测试: cbFilterWorks (Tests L35)/cbFilterTimesout (L47)

代码类型: Implementation (熔断编排)

结论: 熔断 = **链包装 + 状态码语义扩展**: 断路器管执行, 状态码白名单把"业务失败"纳入熔断判定; 失败路径: fallback 内部转发 / resumeWithoutError 放行 / 抛错。**被放弃的方案: 只看异常** — 网关场景下游返回 5xx 是常态, 状态码熔断是必需。 [跨域: SCC ReactiveCircuitBreakerFactory; GW-2 链] [模式: 包装器] (SpringCloudCircuitBreakerFilterFactory.java:55-143)
