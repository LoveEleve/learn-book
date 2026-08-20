# S-5 事务传播 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: Propagation 枚举 6 值 (Javadoc 伪代码, 0.9 锚) + TransactionalTemplate switch 消费 + suspend/resume + SuspendedResourcesHolder |
| 1.x | RootContext 键扩展 (KEY_TIMEOUT/KEY_GLOBAL_LOCK_FLAG); MDC 同步 |
| 2.x | **ContextCore SPI 重构**: ContextCoreLoader (EnhancedServiceLoader) + FastThreadLocalContextCore (Netty 场景); GlobalLockConfigHolder (S-1) |
| 2.5.0 | TransactionInfo 扩展 (lockRetryInterval/Times/lockStrategyMode) |

## 痕迹证据

- Propagation.java:57-176: 每值 Javadoc 伪代码 (0.9 锚)
- DefaultGlobalTransaction.java:213: "In order to associate the following logs with XID, first get and then unbind" — 挂起顺序 (1.x 锚)
- RootContext.java:127: "xid is blank, switch to unbind operation!" — bind 空值 (1.x 锚)
- ContextCoreLoader.java:31-41: SPI Optional 加载 — 存储可替换 (2.x 锚)
- FastThreadLocalContextCore.java:32: Netty FastThreadLocal — 场景专用 (2.x 锚)

## 推断标注

- "0.9~1.x 骨架" — Fescar 起 (公知版本线) (标注)
- "2.x ContextCore SPI" — FastThreadLocal 存在性推断 (标注)
- "2.5.0 TransactionInfo" — 配置键实证 (实证)
- git 多 commit 可考古 — 本域以注释锚为主

## 对照线 (阶段 3 已交付)

- Spring 事务: 7 传播 (含 NESTED savepoint) vs Seata 6 — 挂起语义同构 (Spring suspend/resume vs Seata unbind/bind)
- MyBatis (3.4): 无传播概念 (单事务) — 对照复杂度
