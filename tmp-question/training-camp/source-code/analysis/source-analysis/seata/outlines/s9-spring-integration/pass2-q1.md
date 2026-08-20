# 闭环笔记 q1: 注解解析 — @GlobalTransactional 全参数

## 假设
注解参数覆盖超时/回滚规则/传播/锁配置; 参数桥到 TransactionInfo。

## 验证过程
- **注解参数** (GlobalTransactional, integration-tx-api): **timeoutMills** (默认 DEFAULT_GLOBAL_TRANSACTION_TIMEOUT=**60000ms**, DefaultValues:285) / **rollbackFor + rollbackForClassName** / **noRollbackFor + noRollbackForClassName** (Class/String 双形式) / **propagation 默认 REQUIRED** / **lockRetryInterval / lockRetryTimes / lockStrategyMode** (S-12 交叉)
- **参数桥** (GlobalTransactionalInterceptorHandler:207-244): getTransactionInfo → **超时重置语义**: aspectTransactional.timeout <= 0 **|| == 60000** → 用 defaultGlobalTransactionTimeout (L208-211) — 未配置时跟随全局默认 (动态)
- **rollbackRules 构建** (L216-241): rollbackFor → **RollbackRule** / noRollbackFor → **NoRollbackRule** (Class + String 双形式, LinkedHashSet) — **S-5 rollbackOn 规则引擎的注解源实证闭环**
- **name()** (L199-206): 注解 name 空 → **formatMethod** (方法签名格式化)
- **AspectTransactional**: 注解 DTO (解析结果)

## 代码类型
Data (注解面)

## 跨域关联
- S-1: TransactionalTemplate 消费 (TransactionInfo)
- S-5: Propagation 默认 REQUIRED + rollbackRules (规则引擎源)
- S-12: lockRetry* 参数

## 结论
注解 = 超时/回滚规则/传播/锁配置 8+ 参数; 超时重置语义 (未配置跟随全局默认); rollbackRules 注解源闭环。
源码位置: GlobalTransactional.java; GlobalTransactionalInterceptorHandler.java:199-244; DefaultValues.java:285
