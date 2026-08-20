# GW-7 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 6 处引用**内容**逐一验证 | **6/6 命中零修正**: factory.create L95/状态码熔断 L100-109 逐字 (CircuitBreakerStatusCodeException)/fallback 重构 L124-133 逐字/resumeWithoutError L143/Config L169-177/ReactiveCircuitBreaker (SCC) L29-37 逐字 |
| 2. 数字穷举 | statusCodes 白名单/backoff 5 参数 (GW-2 实证) | ✅ |
| 3. 代码块逐字 | "throw new CircuitBreakerStatusCodeException(status)" (L109)/"No fallback available" (SCC L33) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← GW-2+GW-6 / → GW-8 | ✅ (前置均前位) |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16)

**R1 全量回源 — 1 处修正**: handle(DispatcherHandler) 实际 **L142** (原 L133 为 .toUri()) — outline + pass2-q1 同步。
**R2**: 3 闭环全含被放弃+跨域 ✅
**R3**: 前置 GW-2(序3)+GW-6(序6) < GW-7(序7) 合规 ✅
**R5/R8/R9**: 3 节/负面 6 条/harness 覆盖 3 机制 ✅
**R10**: 残留 0 + harness 回归 3/3 → 收敛

## 第三轮: 追加 (新维度)

**R12/R13 配套机制发现**: **FallbackHeadersGatewayFilterFactory** (L33-54) — 熔断异常 → CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR (addExceptionDetails L162) → fallback 路由写异常头 (ExecutionExceptionType/Message L105-117) — **大纲零覆盖, 补强 §3 配套**
**R14**: 残留 0 + FallbackHeaders 同步 KP (**KP 重复行清理 1 处**) → 收敛确认

## 第四轮: 追加 (新维度)

**R16 SCC 域号核对 (重要)**: GW-7 的 SCC 交叉引用**未标域号** — SCC-PLAN 实际有 **SCC-10 断路器抽象** (ReactiveCircuitBreaker 8 类, SCC-PLAN L57) → outline + pass2-q3 补全 SCC-10 (上次 SCC-5 错位的变体: 不是错位而是缺失)
**R17 Retry 默认值穷举**: **retries=3 + series=[SERVER_ERROR] + methods=[GET] + exceptions=[IOException, TimeoutException]** (RetryGatewayFilterFactory L305-315) — **默认只重试幂等 GET + 5xx** (安全默认) — 补大纲 + 与 gRPC G-6 必填对照
**R18 fallback-body 联动**: fallbackUri 配置 → **enableBodyCaching(routeId)** (L92) — 内部转发需重放 body (GW-5 联动) — 补大纲
**R19 series 匹配**: statusCode.series() 比对 (L95-104) — 补大纲
**R20**: 残留 0 + harness 回归 3/3 → **收敛判定**

累计第四轮: SCC 域号 1 + 默认值 1 + 联动 2。

**R11 fallback 机制深挖** (3 处补强):
- **Config 完整字段**: name/fallbackUri/routeId/statusCodes/**resumeWithoutError 默认 false** (L183 区) — 默认失败抛错
- **reset(exchange) (L139)**: "Reset the exchange" — 清路由属性让 fallback 请求重新路由 — 大纲原只提重构
- **fallback 双路径**: fallback 走 **handle(DispatcherHandler) 直转** (L142, 不经转发过滤器链) vs GW-5 ForwardRoutingFilter 的链内 forward:// — 两条路径的关系补明

## 发现与修正 (第一轮)

1. **零修正轮**: 引用内容写作时逐字验证; 前置声明合规; SCC 交叉以接口契约为准 (ReactiveCircuitBreaker L29-37 实证)。
2. **completeness 20 问**: 3 处 ⚠️ (对照/API 细节) 写作展开。
3. **时空溯源**: 见 temporal-trace.md。

## 结论

大纲机制全部有源码实证; 零修正。**达到合格标准**。
