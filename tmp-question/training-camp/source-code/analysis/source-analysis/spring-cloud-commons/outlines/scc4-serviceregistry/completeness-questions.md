# SCC-4 服务注册抽象 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. ServiceRegistry 5 方法的分工? setStatus/getStatus 谁消费?
2. 触发点为什么是 WebServerInitializedEvent 而非应用启动?
3. start() 的"仪式"顺序为什么是 Pre 事件 → 前钩子 → register → 后钩子 → Registered 事件?
4. stop() 怎么与 start() 对称?
5. RegistrationLifecycle 4 钩子 + Ordered 的意义?

## B. 源码实证 (5)

6. onApplicationEvent 的 management namespace 跳过条件? (grep L114-117)
7. port.compareAndSet 的语义? (grep L118)
8. running 双检在哪? (grep start L151/L170)
9. registerManagement 的判空? (grep L268-273)
10. failFast 抛什么异常? (grep AutoServiceRegistrationAutoConfiguration L42)

## C. 推理深挖 (5)

11. 为什么管理服务要单独注册? management namespace 跳过的关系?
12. 注册失败 (register 抛) 会发生什么? failFast 与普通路径差异?
13. shouldRegisterManagement 由什么决定? 谁覆写?
14. 如果 WebServerInitializedEvent 事件丢失 (非 Web 应用), 服务还注册吗?
15. stop() 的 running CAS 与 isEnabled 顺序为什么这样?

## D. 跨域扩展 (5)

16. AbstractAutoServiceRegistration vs Nacos 的 NacosAutoServiceRegistration (5.8)?
17. RegistrationLifecycle vs Spring Boot 的 ApplicationListener 钩子设计?
18. 注册触发时机 vs SCC-8 的 ready 守卫 (ApplicationReadyEvent)?
19. failFast vs 配置中心启动失败的快速失败模式?
20. 如果给注册加"重试", 应该在哪层? 为什么当前不做?
