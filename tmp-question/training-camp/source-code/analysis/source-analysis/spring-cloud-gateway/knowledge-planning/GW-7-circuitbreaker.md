# GW-7 熔断与重试 — 知识规划 (KP)

> 域级: 🟡 B | 模块: filter/factory/SpringCloudCircuitBreakerFilterFactory (55-165) + Resilience4J 版 + RetryGatewayFilterFactory (535)
> 日期: 2026-08-16 | 版本: 4.3.2 | Pass 2 闭环: q1(熔断编排) q3(SCC 交叉) q4(组合) — **3 闭环覆盖 4 问**

## 一、机制提取 (逐源)

### M1 熔断编排 (q1)
- SpringCloudCircuitBreakerFilterFactory (55-143): factory.create(id) (L95) → **cb.run(链 + 状态码熔断)** (L100-109: 成功但状态码在配置集 → CircuitBreakerStatusCodeException) → 失败: fallbackUri 重构 (L112-135) / resumeWithoutError (L143)
- Config (L169-177): name/fallbackUri/statusCodes/routeId

### M2 SCC 交叉 (q3)
- ReactiveCircuitBreaker (SCC: run(Mono) L31-32/run(Mono, fallback) L37)
- Resilience4J 版 (L32-37): 继承 + SCC 工厂

### M3 组合 (q4)
- Retry (535): RetryConfig (L73-74) + Backoff.exponential (L221-222) + exceedsMaxIterations (L229)
- 链顺序即语义: CircuitBreaker 外层 (兜底含重试调用)


## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 状态码熔断 / fallback 重构 |
| P2 | M2 SCC 消费 / M3 组合语义 |

## 三、叙事线

场景: 下游故障 — 熔断怎么介入?场景: 配置 `CircuitBreaker` 一行, 故障时怎么办?读者疑问链: 熔断怎么包装链 (M1) → 断路器谁提供 (M2) → 和重试怎么排 (M3)。
