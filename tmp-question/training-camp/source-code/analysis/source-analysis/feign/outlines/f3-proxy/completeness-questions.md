# F-3 代理与调用链 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. Feign.builder().target() 返回什么? 内部做了什么?
2. FeignInvocationHandler 怎么分发方法调用? Object 方法呢?
3. SynchronousMethodHandler.invoke 第一行做什么? retryer.clone 为什么?
4. executeAndDecode 的完整调用链?
5. targetRequest 里拦截器和 Target.apply 的顺序?
6. ResponseHandler 的 logAndRebuffer 为什么必要?
7. 异步接口怎么用? 与同步共用什么?

## 架构师视角

8. 方法→handler 映射在 newInstance 固化的设计? 动态性在哪?
9. MethodInterceptor 链末端是 runWithRetry 意味着什么?
10. retryer.clone 的并发语义? 状态隔离?
11. 日志→rebuffer→解码的顺序依赖? 流消费问题?
12. InvocationContext 裁决 404/500 与 F-4/F-5 的关系?
13. 异步性剥离 (MethodInfo) 让三客户端共用的价值?
14. Client 抽象与连接池/超时责任的切分?
15. Target 晚期绑定主机 vs 契约期绑定的取舍?

## SRE/运维视角

16. 重试 5 次总耗时多少? 怎么调?
17. 连接超时 vs 读超时怎么配? Options 在哪设?
18. 日志 FULL 级别输出什么? rebuffer 性能影响?
19. 调用失败 FeignException 的层级? 怎么 catch?

## 研究者视角

20. vs Dubbo 代理 (InvokerInvocationHandler): 异同?
21. vs Spring AOP 代理: 接口代理 vs CGLIB?
22. vs Retrofit OkHttpCall: 执行链对比?
23. 异步 CompletableFuture 链 vs 响应式 (reactive 模块)?

## 学生视角

24. 什么是动态代理? 接口怎么变成对象?
25. 什么是调用链? 请求经过哪些环节?
26. 什么是重试? 为什么要重试?
27. 什么是日志缓冲? 读过的流为什么不能再用?
28. 什么是同步/异步? CompletableFuture 是什么?
