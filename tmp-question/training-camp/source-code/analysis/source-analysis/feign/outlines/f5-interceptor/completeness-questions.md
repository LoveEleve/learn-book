# F-5 拦截器链 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. RequestInterceptor.apply 的签名? 什么时候执行?
2. RequestInterceptor 有顺序保证吗? 为什么?
3. MethodInterceptor.intercept 的签名? Chain 怎么用?
4. MethodInterceptor 和 RequestInterceptor 生命周期差在哪?
5. Invocation 里能看到什么? requestTemplate 可变吗?
6. ResponseInterceptor 挂在哪个环节? InvocationContext 分发逻辑?
7. BasicAuthRequestInterceptor 怎么加头? 编码是什么?

## 架构师视角

8. 三阶段拦截 (RI/MI/RI) 的设计意图? 各阶段能干什么?
9. MethodInterceptor 链末端是 runWithRetry 意味着什么?
10. RequestInterceptors 是列表, MethodInterceptor 是链 — 为什么?
11. 404/500 最终裁决为什么在 InvocationContext?
12. 拦截器共享实例的线程安全约定?
13. Retryer clone 每请求独立的设计? Retry-After 优先级?
14. validation/http-cache 模块怎么用 MethodInterceptor?

## SRE/运维视角

15. 拦截器抛异常怎么表现? 短路后调用方看到什么?
16. 重试日志特征? 5 次尝试怎么数?
17. 自定义认证拦截器要注意什么 (幂等/顺序)?
18. 拦截器性能影响? 每调用一次的开销?

## 研究者视角

19. vs Servlet Filter: 生命周期差异?
20. vs Spring HandlerInterceptor: 有序 vs 无序?
21. 责任链模式 (Chain of Responsibility) 的应用变体?
22. 13.x 为什么引入 MethodInterceptor/ResponseInterceptor?

## 学生视角

23. 什么是拦截器? 为什么要拦截?
24. 什么是责任链? 链和列表区别?
25. 什么是 Basic Auth? Base64 编码?
26. 什么是重试? 指数退避什么意思?
27. 什么是短路? 拦截器怎么"不放行"?
