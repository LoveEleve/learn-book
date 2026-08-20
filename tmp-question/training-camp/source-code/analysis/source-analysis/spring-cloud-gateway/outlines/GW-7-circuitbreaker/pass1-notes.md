# GW-7 Pass 1 扫描笔记 — 熔断与重试

> 日期: 2026-08-16 | 版本: 4.3.2 | 🟡 B | 模块: filter/factory/SpringCloudCircuitBreakerFilterFactory + Resilience4J 版 + RetryGatewayFilterFactory (535)

## 继承树/调用图

```
SpringCloudCircuitBreakerFilterFactory (55, 抽象):
  ├── ReactiveCircuitBreakerFactory.create(config.getId()) (L95) — SCC 熔断工厂
  ├── cb.run(chain.filter...) (L100): 成功但状态码在配置集 → CircuitBreakerStatusCodeException (L104-109)
  ├── 失败 → fallbackUri 处理 (L112-135): 重构 URI → GATEWAY_REQUEST_URL_ATTR → handle(DispatcherHandler)
  ├── 无 fallback → handleErrorWithoutFallback (L143, resumeWithoutError 可配)
  └── Resilience4J 版 (L32): 继承 + SCC Resilience4JCircuitBreakerFactory
RetryGatewayFilterFactory (54, 535): RetryConfig (retries/statuses/backoff 嵌套) + Reactor Retry (q5 已备)
```

## 基本元素分解

1. **熔断工厂**: SCC ReactiveCircuitBreaker 消费 + fallback 路由
2. **fallback 机制**: fallbackUri → DispatcherHandler (内部转发)
3. **状态码熔断**: 成功但响应码在配置集 → 视为熔断
4. **Retry**: 重试 (q5 已备, GW-7 组合)

## 标记问题 (4)

1. **Q1 熔断编排**: cb.run 包装链 + 状态码熔断 (成功但 5xx 算失败)
2. **Q2 fallback**: fallbackUri → DispatcherHandler 内部转发 (与 GW-5 ForwardRoutingFilter 关联)
3. **Q3 SCC 交叉**: ReactiveCircuitBreakerFactory (SCC 域)
4. **Q4 熔断+重试组合**: 链内组合语义 (限流→熔断→重试顺序)

## 已读测试

- SpringCloudCircuitBreakerFilterFactoryTests: cbFilterWorks (L35)/cbFilterTimesout (L47)/filterFallback (L77)
