# T-3 Pipeline + Filter 双链 20 问 (A 机制理解 5 / B 源码实证 6 / C 推理深挖 5 / D 跨域扩展 4)

> 格式升级: 旧多视角 11 问 → 新 A/B/C/D 四节 20 问 (2026-08-17 查漏补缺)

### A. 机制理解 (5)
1. Tomcat 为什么需要两套 Chain of Responsibility (Valve 链 + Filter 链)? 不能合并成一套吗?
2. pipeline.getFirst() 返回什么? 是 basic Valve 还是 addValve 添加的 Valve?
3. filterChain.doFilter() 内部怎么遍历 Filter? 是递归还是循环? 为什么这样实现?
4. Filter 的 chain.doFilter() 和 Valve 的 getNext().invoke() 有什么区别?
5. 如果在 Engine/Host/Context 三层各加 AccessLogValve — 一个请求会触发 3 条日志? 合理吗?

### B. 源码实证 (6)
6. StandardPipeline 的阀遍历逻辑 (getNext 循环)? (grep catalina/core/StandardPipeline.java:109/122)
7. ApplicationFilterChain 类声明与 doFilter 入口? (grep catalina/core/ApplicationFilterChain.java:46/117)
8. 4 个标准 Valve (Engine/Host/Context/Wrapper) 各叫什么类? (grep catalina/core/StandardEngineValve.java 等)
9. StandardContext 的过滤器映射怎么注册? (grep catalina/core/StandardContext.java filterMaps)
10. ApplicationFilterChain 的 internalDoFilter 与 pos 游标? (grep catalina/core/ApplicationFilterChain.java)
11. Valve 接口的 invoke 签名? (grep catalina/Valve.java)

### C. 推理深挖 (5)
12. Valve 链和 Filter 链的执行顺序 — Valve 先还是 Filter 先? 为什么?
13. Netty 的 ChannelPipeline 是双向的 — Tomcat 的 Pipeline 是单向的 — 为什么 Tomcat 不需要 Outbound?
14. 为什么 Tomcat Pipeline 用 getNext().invoke() 显式传递 — Netty 用 ctx 封装? 哪种设计更好?
15. 请求在 Filter 链中被拦截 (未调 doFilter) — 响应怎么返回? 链怎么终止?
16. Filter 的实例化时机与生命周期 — 是每请求 new 还是单例复用? 线程安全怎么保证?

### D. 跨域扩展 (4)
17. 本域 vs T-10 集群: tribes 的 ChannelInterceptor 链和 Valve 链是同构的吗?
18. 本域 vs Spring: SpringMVC 的 HandlerInterceptor/Filter 与 Tomcat Filter 链的分层?
19. 本域 vs Nacos: Nacos 的 Filter 链 (鉴权/限流) 与 Tomcat Filter 链的架构对照?
20. 本域 vs openjdk: JVM 的 Safepoint 处理 vs 责任链 — 两者都解决"横切关注点"问题, 实现方式差异?