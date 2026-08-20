# SCC-5 @LoadBalanced 客户端 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. @LoadBalanced 的 @Qualifier 元注解意义? 收集方怎么用它?
2. "URL host 即服务名"约定怎么建立? 为什么设计成这样?
3. LoadBalancerClient execute 双形态的差异? 各自场景?
4. BlockingLoadBalancerClient 怎么"阻塞包装响应式"?
5. DeferringLoadBalancerInterceptor 解决什么时序问题?

## B. 源码实证 (5)

6. intercept 里 serviceName 从哪提取? (grep LoadBalancerInterceptor:53)
7. Assert.state 的校验信息? (grep L54)
8. reconstructURI 的委托实现? (grep BlockingLoadBalancerClient:148-149)
9. choose 的 Mono.from(...).block() 语义? (grep L163)
10. LoadBalancerAutoConfiguration 收集 @LoadBalanced RestTemplate 的方式? (grep L60-62)

## C. 推理深挖 (5)

11. 为什么用 SmartInitializingSingleton 而非普通 Bean 初始化?
12. 如果没有 LoadBalancerClient bean, 装配会发生什么?
13. reconstructURI 对 https 协议怎么处理? 实例 URI 影响?
14. transformers 的典型用途? (加 header/改路径)
15. 多个 @LoadBalanced RestTemplate 怎么分别定制? (customizer 机制)

## D. 跨域扩展 (5)

16. @LoadBalanced vs Ribbon 的 @LoadBalanced 兼容演进?
17. BlockingLoadBalancerClient.choose vs SCC-7 ReactorLoadBalancer.choose 的关系?
18. LoadBalancerRequestTransformer vs Feign 的 RequestInterceptor (5.1)?
19. DeferringLoadBalancerInterceptor vs SCC-8 的 ready 守卫 (延迟解析模式)?
20. WebClient 响应式场景怎么用 LoadBalancer? (对比阻塞式)
