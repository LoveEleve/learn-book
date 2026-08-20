# SCC-11 BlockingLoadBalancer 重试 — RetryTemplate 包裹的"换服务器重试"

> 前置: [[SCC-7-ReactorLoadBalancer]] (choose 消费) + [[SCC-5-@LoadBalanced]] (拦截面) | 引出: [[SCC-12-扩展策略]] | 对照: Spring Retry + Ribbon 重试
> 🟡 B | 方案 B (标准) | 闭环: q1(RetryTemplate 包裹) q2(同服/换服) q3(状态码触发) q4(策略接口)
> Pass 2 闭环: q1(template.execute) q2(上下文实例复用) q3(retryableStatusCode) q4(四方法契约)

**读者处境**: 请求失败怎么重试? "同一服务器重试"和"换服务器重试"怎么区分? RetryTemplate 和 LoadBalancer 怎么配合? 状态码触发重试怎么实现?

### 1. RetryLoadBalancerInterceptor — RetryTemplate 包裹拦截

场景: 重试拦截器和普通拦截器差在哪?
源码路径:
- RetryLoadBalancerInterceptor (client/loadbalancer/RetryLoadBalancerInterceptor.java:47): implements BlockingLoadBalancerInterceptor
- **intercept** (L69-76): serviceName 提取 (L71-72) → **lbRetryFactory.createRetryPolicy** (L74) → **createRetryTemplate + template.execute** (L75-76, createRetryTemplate 定义 L144-166: BackOffPolicy factory 创建 L147-148 + setThrowLastExceptionOnExhausted(true) L150 + RetryListener L152-155 + **RetryPolicy 条件选择 L156-160: retry 禁用 → NeverRetryPolicy / 否则 InterceptorRetryPolicy**) — Spring Retry 包裹
- 四参构造 (L58-66): loadBalancer + requestFactory + lbRetryFactory + loadBalancerFactory
关键设计 (q1): **重试 = RetryTemplate.execute 包裹** — 拦截逻辑 (选实例/执行/判断) 作为 RetryCallback; RetryPolicy/BackOffPolicy 由 factory 创建; 与 LoadBalancerInterceptor 的唯一差异是 RetryTemplate 外层。 [模式: RetryTemplate 包裹]

### 2. 重试上下文 — 同实例复用 vs 重新选择

场景: 重试时用哪个实例?
源码路径:
- **上下文实例复用** (L79-84): `lbContext.getServiceInstance()` (L79) — 非空 → **同一实例重试** (canRetrySameServer 语义)
- **实例 null → 重新选择** (L93-102): previousServiceInstance (L95-97) + **RetryableRequestContext** (L99-100, 携带前实例+请求数据+hint) + lifecycle.onStart (L101) + **loadBalancer.choose(serviceName, lbRequest)** (L102)
- **DefaultResponse 包装** (L111) + 实例 null → onComplete(DISCARD) (L113-118)
- lbContext.setServiceInstance (L109) — 上下文记录选中实例供下次重试
关键设计 (q2): **"同服重试 vs 换服重试"由上下文实例决定** — 上下文有实例 = 同服 (canRetrySameServer); 无实例 = 重新 choose (canRetryNextServer); RetryableRequestContext 携带 previousServiceInstance 让新选择避开失败实例。 [模式: 上下文驱动重试选择]

### 3. 状态码触发 — retryableStatusCode 抛异常进 RetryTemplate

场景: HTTP 错误状态怎么触发重试?
源码路径:
- **执行 + 状态码检查** (L122-124): `loadBalancer.execute` → statusCode → **retryPolicy.retryableStatusCode(statusCode)** (L124)
- **触发重试** (L126-130): 可重试状态 → **bodyCopy (L128) + response.close() (L129) + throw ClientHttpResponseStatusCodeException** (L130) — 抛异常让 RetryTemplate 捕获重试
- 响应体复制 (L128): 重试前先复制 body (流被消费后不可重放)
关键设计 (q3): **"状态码可重试 → 抛异常进 RetryTemplate"** — 不是显式 retry 循环, 而是借 RetryTemplate 的异常驱动; bodyCopy 防流消费后重试无 body; response.close 释放连接。 [模式: 异常驱动重试]

### 4. LoadBalancedRetryPolicy — 四方法策略契约

场景: 重试策略接口承诺什么?
源码路径:
- LoadBalancedRetryPolicy (client/loadbalancer/LoadBalancedRetryPolicy.java:25): **四方法** — canRetrySameServer (L33) / canRetryNextServer (L42) / close (L48) / registerThrowable (L55)
- **canRetrySameServer**: "retry the failed request on the same server" — 同服判定
- **canRetryNextServer**: "retry on the next server from the load balancer" — 换服判定
- 实现: BlockingLoadBalancedRetryPolicy (loadbalancer/blocking/retry/) + InterceptorRetryPolicy
关键设计 (q4): **"同服/换服"双判定是重试策略的核心** — 每次重试前问策略"还能同服吗/还能换服吗"; close 收尾 (清理); registerThrowable 记录异常 (影响后续判定)。 [模式: 双判定策略]

### 5. BlockingLoadBalancedRetryPolicy — 具体实现

场景: 默认重试策略怎么实现?
源码路径:
- BlockingLoadBalancedRetryPolicy (loadbalancer/blocking/retry/BlockingLoadBalancedRetryPolicy.java): implements LoadBalancedRetryPolicy
- **canRetry 方法级安全** (L43-47): **只有 GET 或 retryOnAllOperations=true 才可重试** — POST 等非幂等默认不重试
- **同服计数** (L49-51): `sameServerCount < maxRetriesOnSameServiceInstance` — 严格小于
- **换服计数** (L53-55): `nextServerCount <= maxRetriesOnNextServiceInstance` — **注释明示 "After the failure, we increment first and then check, hence the equality check" — 先递增后检查所以用 <=, 非"容忍"**
- **registerThrowable 换服触发** (L66-83): 不能同服但可重试 → **重置 sameServerCount + nextServerCount++** (L71-72) + 超限 **setExhaustedOnly** (L74) / 否则 **setServiceInstance(null)** 让拦截器重新选择 (L80-82)
- retryableStatusCode (L85-87): **可重试状态码集合 contains** (非区间)
- retryableStatusCode: LoadBalancerProperties.retry.retryableStatusCodes 配置
- 装配: BlockingLoadBalancedRetryFactory (blocking/retry/)
关键设计 (q4): **"计数 vs 上限"双维度控制重试** — 同服次数/换服次数独立上限; 状态码可配; factory 负责创建 (策略实例化)。 [模式: 计数上限策略]

### 6. 装配面 — BlockingLoadBalancerClient 与重试的关系

场景: 阻塞式客户端和重试怎么衔接?
源码路径:
- BlockingLoadBalancerClient (blocking/client/): execute 双形态 + choose (SCC-5 已详)
- RetryLoadBalancerInterceptor vs LoadBalancerInterceptor: 重试版拦截器 (RetryTemplate 包裹) vs 普通版
- LoadBalancedRetryFactory/BlockingLoadBalancedRetryFactory: 策略工厂 (同服/换服上限配置)
- RetryableStatusCodeException + ClientHttpResponseStatusCodeException: 重试触发异常族
关键设计 (q1): **"阻塞执行 + 重试拦截"是两层** — BlockingLoadBalancerClient 只负责执行 (SCC-5), 重试在拦截器层 (RetryLoadBalancerInterceptor); 选重试拦截器 (retry-enabled) 决定是否启用重试。 [模式: 执行/重试分层]

## 代码类型
Architecture (重试面) + Integration (Spring Retry)

## 负面空间 — BlockingLoadBalancer 重试刻意不做的事

- **不做重试策略算法内建**: 用 Spring Retry 的 RetryTemplate (第三方)
- **不做分布式重试**: 本地重试, 无跨节点协调
- **不做非 GET 幂等保证**: 默认 GET-only 防线 (canRetry L47), 但 retryOnAllOperations=true 时非 GET 重试安全由业务保证 (重放风险)
- **不做响应体重放**: bodyCopy 只复制一次 (重试间不共享)
- **不做超时控制内建**: RetryTemplate 的 BackOffPolicy 配置面
- **不做重试指标内建**: 无专门指标采集 (对比 Micrometer)

→ 引出: 更多实例选择策略? → SCC-12 LoadBalancer 扩展策略
