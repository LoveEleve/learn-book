# SCC-11 BlockingLoadBalancer 重试 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照 + 跨域一致性)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 6 | **语义重大** | **registerThrowable 换服触发** (L66-83): 不能同服但可重试 → 重置 sameServerCount + nextServerCount++ + setExhaustedOnly/setServiceInstance(null) — 大纲完全没提这个核心方法 | 已修 §5 |
| 7 | **推断修正** | 换服 `<=` 的原因不是"容忍": 注释明示 **"increment first and then check, hence the equality check"** — 先递增后检查 | 已修 §5 |
| 8 | **canRetry 方法级安全** | **只有 GET 或 retryOnAllOperations=true 才可重试** (L43-47) — POST 默认不重试 | 已修 §5 |
| 9 | 语义补全 | createRetryTemplate: BackOffPolicy factory (L147-148) + setThrowLastExceptionOnExhausted (L150) + **RetryPolicy 条件选择** (L156-160: 禁用 → NeverRetryPolicy / InterceptorRetryPolicy) | 已修 §1 |
| 10 | **负面空间矛盾修正** | "不做幂等保证" 与 canRetry GET-only 矛盾 — 精确为"不做**非 GET** 幂等保证" (GET-only 是框架防线, retryOnAllOperations 时业务负责) | 已修 §负面 |
| 11 | 跨域确认 | InterceptorRetryPolicy implements RetryPolicy (L30) + canRetry 委托 canRetryNextServer (L66); 装配 LoadBalancerAutoConfiguration:182-191 | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | intercept L69-76 ✅ (createRetryTemplate 定义 L144); 上下文复用 **L79-102** (写 L80-111) + getServiceInstance L79 + previousServiceInstance L95-97 + choose L102 | 已修 |
| 2 | 锚点漂移 | 状态码 **L122-130** (写 L122-129): retryableStatusCode L124 + bodyCopy L128 + close L129 + throw L130 | 已修 |
| 3 | 锚点漂移 | LoadBalancedRetryPolicy **L25-55** (写 L26-56) + 四方法 L33/42/48/55 | 已修 |
| 4 | **计数不对称** | 同服 `<` (L52) vs 换服 `<=` (L58) — maxRetriesOnSameServiceInstance 严格小于, maxRetriesOnNextServiceInstance 小于等于 (换服多一次容忍) | 已修 §5 |
| 5 | 验证通过 | BlockingLoadBalancedRetryPolicy L33/37/39 / 四参构造 L58-66 | 通过 |

## 锚点密度统计

- file:line 锚点数: **20+** (🟡B 标准 ≥4 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 5 项检查 4 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (重试算法内建/分布式重试/幂等保证/响应体重放/超时控制内建/重试指标内建)
- [x] 每条有对照物 (Spring Retry/Micrometer)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (重试机制/同服换服/RetryTemplate 配合/状态码触发)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (拦截 → RetryTemplate → 上下文 → 选择/执行 → 状态码 → 异常驱动重试)
- [x] 边界交代: 同服/换服双判定/bodyCopy/计数不对称

## 方法论教训

- **`<` vs `<=` 的计数不对称是真实细节** — 同服严格小于, 换服小于等于 (换服多一次容忍) — 手写大纲会忽略这种边界
- **"抛异常进 RetryTemplate"是异常驱动重试** — 不是显式 retry 循环, ClientHttpResponseStatusCodeException 是触发点
- **bodyCopy 防流消费后重试无 body** — 重试前的数据保全
