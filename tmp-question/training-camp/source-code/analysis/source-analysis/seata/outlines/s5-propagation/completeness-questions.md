# S-5 事务传播 — completeness-questions (全视角提问验证)

## 开发者视角

1. 传播有几种? (6)
2. 各语义? (REQUIRED/REQUIRES_NEW/NOT_SUPPORTED/SUPPORTS/NEVER/MANDATORY)
3. 挂起怎么实现? (unbind + holder)
4. 恢复? (bind)
5. clean 语义? (事务结束不返回 holder)
6. 嵌套 REQUIRES_NEW? (两段挂起栈)
7. 上下文存哪? (ContextCore SPI)
8. 配置载体? (TransactionInfo)

## 架构师视角

9. 为什么无 NESTED? (无 savepoint 内嵌语义 — 对照 Spring)
10. 为什么挂起用 unbind 而非栈? (单 xid 线程模型 — 挂起换出)
11. 为什么每层 finally resume? (挂起必恢复 — 栈式安全)
12. 为什么 ContextCore SPI? (响应式/Netty 场景可替换)
13. 为什么 NOT_SUPPORTED 内 MANDATORY 抛? (环境清空 — 无事务即无事务)
14. 为什么 MDC 同步? (日志关联 xid)
15. 对照 Spring? (挂起语义同构, NESTED 缺)
16. 为什么不继承子线程? (ThreadLocal 默认 — 需显式传递)

## 学生视角

17. 什么是传播? (事务进入时的参与策略)
18. 什么是挂起? (临时解绑现有事务)
19. 什么是 REQUIRES_NEW? (开新事务, 旧的挂起)
20. 什么是 MANDATORY? (强制要求已有事务)
