# ALI-A6 Sentinel 三路限流 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. SentinelBeanPostProcessor 为什么选 MergedBeanDefinitionPostProcessor 而非普通 BeanPostProcessor?
2. blockHandler 方法签名为什么必须含 BlockException? 缺失会怎样?
3. 双 entry (host + host+path) 为什么都要? 只留一个行不行?
4. DegradeException 走 fallback, 其他 BlockException 走 blockHandler — 为什么分流?
5. SentinelFeign.Builder 为什么锁死 invocationHandlerFactory?

## B. 源码实证 (6)

6. 注解发现的双路径具体是哪两个? (grep L81-92)
7. 拦截器插入 RestTemplate 的位置? (grep L199)
8. 资源名格式? (grep SentinelProtectInterceptor:60-63)
9. SentinelInvocationHandler 只处理什么 target? (grep L94)
10. Web 路 BlockExceptionHandler 的三选逻辑? (grep SentinelWebAutoConfiguration L77-88)
11. SentinelConstants 的 PROPERTY_PREFIX 值? (grep L20)

## C. 推理深挖 (5)

12. urlCleaner 清理后资源名会怎样? 对限流统计的影响?
13. 动态注册拦截器 Bean 的命名为什么含配置摘要? 重复注册会怎样?
14. fallbackFactory.create(ex) 的 ex 参数给降级逻辑带来什么能力?
15. 双 entry 的 exit 顺序为什么是先 path 后 host? 反了会怎样?
16. Feign 路资源名没有 host 级, 与 RestTemplate 路的差异影响?

## D. 跨域扩展 (4)

17. fallback/blockHandler 与 SCC-10 CircuitBreaker 的 run+fallback 契约异同?
18. SentinelFeign 的 FeignClientFactory 与 SCC-13 NamedContextFactory 的关系?
19. 三路限流 vs 规划中 OpenFeign 5.6 的 FeignCircuitBreaker: 定位差异?
20. BlockException 处理面 vs Sentinel 5.9 的 ProcessorSlotChain: 集成层与内核分工?
