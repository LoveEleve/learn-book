# SCC-11 BlockingLoadBalancer 重试 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. RetryLoadBalancerInterceptor 与 LoadBalancerInterceptor 的核心差异?
2. "同服重试"与"换服重试"由什么决定?
3. 状态码触发重试的"异常驱动"机制怎么运作?
4. LoadBalancedRetryPolicy 四方法的分工?
5. 计数 `<` vs `<=` 的不对称语义?

## B. 源码实证 (5)

6. RetryTemplate 从哪创建? (grep RetryLoadBalancerInterceptor:144)
7. previousServiceInstance 怎么被使用? (grep L95-97)
8. RetryableRequestContext 携带什么? (grep L99-100)
9. retryableStatusCode 在哪判定? (grep L124)
10. ClientHttpResponseStatusCodeException 什么时候抛? (grep L130)

## C. 推理深挖 (5)

11. bodyCopy 为什么在抛异常前复制? 不复制会怎样?
12. canRetrySameServer 与 canRetryNextServer 的调用时机?
13. RetryTemplate 的 RetryCallback 里发生了什么? (选实例/执行/判断全在回调)
14. 重试时 hint 怎么保持? (RetryableRequestContext 携带)
15. registerThrowable 记录异常后怎么影响判定?

## D. 跨域扩展 (5)

16. Spring Retry RetryTemplate vs SCC-8 的刷新重试?
17. 重试拦截器 vs Feign 的 Retryer (5.1)?
18. 同服/换服策略 vs Ribbon 的 RetryRule?
19. RetryableStatusCodeException vs F-4 的 FeignException 状态码?
20. 如果加"重试指标", 应该在哪层? (RetryTemplate listener vs 拦截器)
