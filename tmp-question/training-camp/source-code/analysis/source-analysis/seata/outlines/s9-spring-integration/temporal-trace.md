# S-9 Spring 集成 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: GlobalTransactionScanner (AbstractAutoProxyCreator) + GlobalTransactionalInterceptor + @GlobalTransactional; wrapIfNecessary 织入 |
| 1.x | ScannerChecker 族; PROXYED_SET/EXCLUDE 集合; failureHandler 钩子 |
| 2.x | **模块化重构**: 拦截器移至 integration-tx-api (GlobalTransactionalInterceptorHandler 双 handler TM/GlobalLock); AspectTransactional DTO; 超时重置语义; SeataAutoConfiguration (Boot starter) |
| 2.5.0 | DefaultInterfaceParser 接口解析; AdapterSpringSeataInterceptor; Scanner 注释锚 (L291-297) |

## 痕迹证据

- GlobalTransactionScanner.java:291-297: "Corresponding interceptor: ...GlobalTransactionalInterceptorHandler // TM handler" + "// GlobalLock handler" (2.x 锚)
- GlobalTransactionScanner.java:300-305: LocalTCC/TwoPhaseBusinessAction/RemotingParser 注释 (TCC 面)
- GlobalTransactionScanner.java:525-539: afterPropertiesSet disable/initClient
- GlobalTransactionalInterceptorHandler.java:208-211: 超时重置语义
- DefaultValues.java:285: DEFAULT_GLOBAL_TRANSACTION_TIMEOUT=60000

## 推断标注

- "0.9~1.x 骨架" — Fescar 起 (公知版本线) (标注)
- "2.x integration-tx-api" — 模块存在性实证 (实证)
- "2.5.0 DefaultInterfaceParser" — 类存在性推断 (标注)
- git 多 commit 可考古 — 本域以注释锚 + 模块结构为主

## 对照线 (阶段 3 已交付)

- Spring @Transactional: TransactionInterceptor vs Seata GlobalTransactionalInterceptorHandler — 注解语义/回滚规则同构 (RollbackRule 家族同源)
- MyBatis (3.4): 无事务注解 — 单事务面
