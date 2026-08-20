# S-9 Spring 集成 — Pass 1 探索笔记

> 域: S-9 Spring 集成 | 🟡 B 方案 (无 harness) | 2026-08-15
> 源码: spring/annotation/GlobalTransactionScanner (676) + integration-tx-api/ (GlobalTransactionalInterceptorHandler + GlobalTransactional 注解 + AspectTransactional) + seata-spring-boot-starter/SeataAutoConfiguration | Seata 2.5.0

## 调用图

```
@GlobalTransactional (注解, integration-tx-api)
  → SeataAutoConfiguration → GlobalTransactionScanner bean (AbstractAutoProxyCreator)
    → afterPropertiesSet: initClient (TM/RM) + findBusinessBeanNamesNeededEnhancement (BeanDefinition 扫描)
    → wrapIfNecessary (每个 bean 初始化后):
        doCheckers (排除 FactoryBean/已代理) → NEED_ENHANCE 集合 → DefaultInterfaceParser 解析接口
        → AdapterSpringSeataInterceptor → super.wrapIfNecessary (Spring AOP) / 已代理: addAdvisor 按序插入
    → 方法调用 → GlobalTransactionalInterceptorHandler.handleGlobalTransaction:
        TransactionInfo 构建 (超时重置: <=0 或 ==60000 → 默认) + rollbackRules (rollbackFor/noRollbackFor)
        → TransactionalTemplate.execute (S-1)
        → ExecutionException → failureHandler.onBeginFailure/onCommitFailure/onRollbackFailure
```

## 基本元素分解

1. **注解**: @GlobalTransactional (timeoutMills/rollbackFor/noRollbackFor/propagation/lockRetry*)
2. **扫描器**: GlobalTransactionScanner (AbstractAutoProxyCreator + ScannerChecker 族 + 双集合)
3. **拦截器**: GlobalTransactionalInterceptorHandler (TM handler + GlobalLock handler)
4. **配置**: SeataAutoConfiguration (failureHandler + scanner bean)
5. **参数桥**: AspectTransactional → TransactionInfo (超时重置语义)

## 标记问题 (20 问)

1. 注解参数? (timeoutMills/rollbackFor/propagation/lockRetry*)
2. AbstractAutoProxyCreator? (Spring AOP 基础设施)
3. wrapIfNecessary? (代理包装)
4. ScannerChecker 族? (ConfigBeans/Package/Scope 3 个)
5. 双集合? (PROXYED_SET/NEED_ENHANCE_BEAN_NAME_SET)
6. 超时重置? (<=0 或 ==60000 → 默认)
7. rollbackRules? (注解 → RollbackRule/NoRollbackRule)
8. ExecutionException 处理? (Participant 抛原异常)
9. FailureHandler? (onBeginFailure/CommitFailure/RollbackFailure)
10. 已代理 bean? (addAdvisor 按序插入)
11. disableGlobalTransaction? (配置关闭)
12. initClient? (TM/RM 初始化)
13. SeataAutoConfiguration? (自动配置)
14. GlobalLock handler? (handleGlobalLock)
15. 默认传播? (REQUIRED)
16. 方法名格式化? (formatMethod)
17. 对照 Spring @Transactional? (注解语义)
18. ORDER_NUM=1024? (advisor 顺序)
19. AspectTransactional? (注解 DTO)
20. 数据源自动代理? (DataSourceAutoProxyCreator)

## 时空溯源 (代码内注释锚)

- GlobalTransactionScanner:300-305 "Corresponding interceptor: ...GlobalTransactionalInterceptorHandler // TM handler" + "// GlobalLock handler" (2.x 重构锚)
- integration-tx-api: 拦截器独立模块 (2.x 模块化)
- 超时重置语义注释 (aspectTransactional.getTimeoutMills 判断)

## 大域拆分判断

S-9 = Spring 装配面 (注解 → 扫描 → 代理 → 拦截 → 模板); 单篇 🟡 B (8 闭环 q1-q4 + 无 harness)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "@GlobalTransactional→GlobalTransactionScanner→TransactionalTemplate" | 全链实证 (注解 → 扫描器 → InterceptorHandler → Template) | **接受** ✅ |
| "扫描装配" | AbstractAutoProxyCreator + wrapIfNecessary + ScannerChecker 3 个 | **接受** ✅ |
| 数字: 注解参数 | timeoutMills/rollbackFor×2/noRollbackFor×2/propagation/lockRetry×3 | **补充** ✅ |
| 数字: 默认超时 | DEFAULT_GLOBAL_TRANSACTION_TIMEOUT=60000 (DefaultValues:285) | **补充** ✅ |
