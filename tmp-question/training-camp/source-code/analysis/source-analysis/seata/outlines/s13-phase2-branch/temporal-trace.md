# S-13 Phase2 分支通知 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: AbstractRMHandler + RMHandlerAT (undo 回滚) + RmBranchCommitProcessor; 分支通知协议定型 |
| 1.x | DefaultRMHandler SPI 注册 (loadAll); RMHandlerTCC/XA 扩展; exceptionHandleTemplate |
| 2.x | **AsyncWorker 异步提交** (S-11 面); RMHandlerSagaAnnotation; issue #2226 (UndoLogDelete 默认空) |
| 2.5.0 | 处理器族稳定 (5 实现) |

## 痕迹证据

- AbstractRMHandler.java:84-87: "https://github.com/seata/seata/issues/2226" (2.x 锚)
- DefaultRMHandler.java:49-54: SPI loadAll 注册 (2.x 锚)
- RmBranchCommitProcessor.java:56: "rm client handle branch commit process" (1.x 锚)
- RMHandlerAT.java:39-128: AT 收束 (0.9 锚)

## 推断标注

- "0.9~1.x 骨架" — Fescar 起 (公知版本线) (标注)
- "2.x AsyncWorker" — S-11 交叉实证 (实证)
- "2.5.0 5 实现" — 类存在性实证 (实证)
- git 多 commit 可考古 — 本域以注释锚 + 模块结构为主

## 对照线 (阶段 4.1 已交付)

- RocketMQ RM-12 (事务消息): 半消息+确认 vs Seata 分支通知+状态回传 — 两阶段确认对照
