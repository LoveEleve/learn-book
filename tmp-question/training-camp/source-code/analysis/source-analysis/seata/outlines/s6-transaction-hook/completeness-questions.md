# S-6 TransactionHook — completeness-questions (全视角提问验证)

## 开发者视角

1. 钩子有几个? (7)
2. 各是什么? (beforeBegin~afterCompletion)
3. 怎么注册? (TransactionHookManager.registerHook)
4. 怎么清空? (Launcher cleanUp 后 clear)
5. 触发顺序? (注册序)
6. 钩子异常? (吞掉只 log)
7. afterCompletion 时机? (finally 全路径)
8. 存哪? (ThreadLocal)

## 架构师视角

9. 为什么观察者而非过滤器? (无链式上下文 — 广播语义)
10. 为什么 ThreadLocal? (事务级隔离 — 不跨线程)
11. 为什么 afterCompletion 仅 Launcher? (Participant 完成是子阶段 — 收尾归发起者)
12. 为什么异常不中断? (钩子是附加逻辑 — 不破坏主流程)
13. 为什么 Launcher 才 clear? (Participant 共享外层钩子)
14. 对照 Spring TransactionSynchronization? (同构同步器)
15. 为什么 Saga 复用? (模板机制模式无关)
16. 为什么 TCC 独立? (门面拦截 vs 生命周期观察 — 不同语义)

## 学生视角

17. 什么是钩子? (事务各阶段的自定义回调)
18. 什么是观察者? (事件发生时通知订阅者)
19. 什么是 ThreadLocal? (线程私有存储)
20. 什么是 afterCompletion? (事务最终完成的收尾回调)
