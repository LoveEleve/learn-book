# SCC-2 @RefreshScope 热刷新 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. LockedScopedProxyFactoryBean 为什么"代理 + MethodInterceptor"双角色? 不能只用一个吗?
2. 双检锁 (synchronized name) 与 ReadWriteLock 的分工? 各防什么?
3. refresh() 为什么"先环境后 Scope"? 顺序反了会怎样?
4. refresh(String) 与 refreshAll() 的语义差异? 单 bean 刷新的 scoped target 转换?
5. ConfigDataContextRefresher 重跑全部 EnvironmentPostProcessor 的意义?

## B. 源码实证 (5)

6. GenericScope 的 cache 和 locks 分别是什么类型? (grep L86-88)
7. invoke 里哪些方法直通不拿锁? (grep GenericScope:462-463)
8. destroy() 的 errors 怎么收集? wrapIfNecessary 语义? (grep L127-142)
9. refresh(Class) 怎么找 bean 名? (grep RefreshScope:140-147)
10. RefreshAutoConfiguration 的装配条件? (grep L69-70)

## C. 推理深挖 (5)

11. 如果读锁内调 destroy (锁升级) 会怎样? 真实代码为什么不会发生?
12. destroy 后新 Bean 什么时候创建? "懒重建"的触发点?
13. 为什么用 ConcurrentMap\<String, ReadWriteLock\> 而非单一全局锁?
14. gh-349 的 UndeclaredThrowableException 为什么必须还原?
15. refresh 时正在执行的请求 (持读锁) 会怎样? 等还是失败?

## D. 跨域扩展 (5)

16. @RefreshScope 代理 vs Spring AOP @Transactional 代理的机制差异?
17. Spring Boot @ConfigurationProperties 重绑定 vs RefreshScope 重建的取舍?
18. Apollo 推送式刷新 vs Spring Cloud Config 拉取式刷新 (RefreshEndpoint)?
19. RefreshScope 的读写锁 vs ConcurrentHashMap 的并发控制的优劣?
20. 多上下文 (bootstrap + main + 子上下文) 下 refreshAll 的行为? 各自清缓存?
