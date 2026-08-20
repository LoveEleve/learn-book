# ALI-A9 Seata 分布式事务 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. 三路透传为什么共用 RootContext? 集成层的职责边界?
2. Feign 路 xid 空为什么直接 return 而非抛异常?
3. afterCompletion 的 unbind 校验为什么 warn 而不是直接覆盖?
4. Feign retryer 强制 NEVER_RETRY 的分布式事务语义?
5. RestTemplate 面为什么全量注入而非注解驱动?

## B. 源码实证 (6)

6. Feign 路 header 怎么写? (grep SeataFeignRequestInterceptor:32-37)
7. RestTemplate 路用什么包装器? (grep SeataRestTemplateInterceptor:43)
8. preHandle 的 bind 条件? (grep SeataHandlerInterceptor:45-52)
9. afterCompletion 的校验逻辑? (grep L59-76)
10. Retryer 改造的触发条件? (grep SeataFeignBuilderBeanPostProcessor:22-25)
11. RestTemplate 全量注入的实现? (grep AfterPropertiesSet:29-36)

## C. 推理深挖 (5)

12. 线程池复用下, 忘记 unbind 会怎样? 校验回绑防什么?
13. 嵌套事务 (A→B→C) 时 XID 怎么传递? 与单跳的差异?
14. 如果 B 服务自己发起新事务 (bind 新 xid), afterCompletion 的 warn 场景?
15. Feign retryer 被强制后, 普通非事务请求的重试能力会怎样?
16. RestTemplate 拦截器全量注入后用户自己的拦截器顺序?

## D. 跨域扩展 (4)

17. 三路透传 vs A6 的三路限流: 同构装配不同语义?
18. RootContext.bind/unbind vs SCC-2 的 ThreadLocal 缓存: 两种线程上下文?
19. 本域拦截器 vs OpenFeign 5.6 的 RequestInterceptor 体系: 扩展点?
20. Seata 集成层 vs Seata 内核 (GlobalTransactionScanner): XID 生命周期谁主导?
