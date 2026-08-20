# S-4 熔断降级域 — 大纲

## 上篇: 一条规则如何变成一次熔断 — 01-degrade-check.md

1. 两个槽的边界：`DegradeSlot`(旧式规则)与 `DefaultCircuitBreakerSlot`(默认规则)
2. `DegradeRule` 的 grade 三分类与规则校验
3. 规则加载：property/listener 与断路器复用
4. `tryPass` 与 `DegradeException`

## 中篇: 状态机与探测 — 02-circuit-breaker.md

1. CLOSED / OPEN / HALF_OPEN 三态与合法转换
2. 恢复时间窗口与探测请求的唯一性(CAS)
3. HALF_OPEN 探测成功/失败的回退
4. 观察者通知与边界不一致

## 下篇: 两种统计策略 — 03-strategies.md

1. ExceptionCircuitBreaker：异常比例与异常数
2. ResponseTimeCircuitBreaker：慢调用比例
3. 单 bucket 滑窗与 `minRequestAmount`
4. HALF_OPEN 下的单请求判定 vs CLOSED 下的窗口聚合
