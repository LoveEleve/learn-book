# SCC-11 BlockingLoadBalancer 重试 — 知识规划 (KP)

> 域: SCC-11 | 级别: 🟡 | 方案: B | 大纲: outlines/scc11-blocking-retry/outline.md (6 节)

## §01 域定位

BlockingLoadBalancer 重试 = RetryTemplate 包裹的"换服务器重试"。RetryLoadBalancerInterceptor (异常驱动) + LoadBalancedRetryPolicy 四方法 (同服/换服双判定) + BlockingLoadBalancedRetryPolicy (计数上限)。

## §02 源文件清单

| 文件 | 职责 | 归属节 |
|:--|:--|:--:|
| client/loadbalancer/RetryLoadBalancerInterceptor.java | RetryTemplate 包裹拦截 | 1,2,3 |
| client/loadbalancer/LoadBalancedRetryPolicy.java | 四方法契约 | 4 |
| loadbalancer/blocking/retry/BlockingLoadBalancedRetryPolicy.java | 计数上限实现 | 5 |
| loadbalancer/blocking/retry/BlockingLoadBalancedRetryFactory.java | 策略工厂 | 5 |
| client/loadbalancer/RetryableStatusCodeException + ClientHttpResponseStatusCodeException | 重试触发异常族 | 3 |

## §05 闭环要点 (Pass 2 内化)

### q1 RetryTemplate 包裹
intercept (L69-76): createRetryPolicy (L74) + createRetryTemplate (L144) + template.execute (L76)。

### q2 同服/换服
上下文实例复用 (L79) vs 重新选择 (L93-102, RetryableRequestContext + previousServiceInstance)。

### q3 状态码触发
retryableStatusCode (L124) → bodyCopy (L128) + close (L129) + throw ClientHttpResponseStatusCodeException (L130) — 异常驱动。

### q4 策略契约
四方法 (L33/42/48/55); 计数: 同服 `<` (L52) vs 换服 `<=` (L58) 不对称。

## §06 负面空间 (6 条)

不做重试算法内建 / 不做分布式重试 / 不做幂等保证 / 不做响应体重放 / 不做超时控制内建 / 不做重试指标内建

## §07 交叉引用

- ← SCC-7 ReactorLoadBalancer (choose 消费) + SCC-5 @LoadBalanced (拦截面)
- → SCC-12 扩展策略
- 另见: Spring Retry / Ribbon RetryRule
