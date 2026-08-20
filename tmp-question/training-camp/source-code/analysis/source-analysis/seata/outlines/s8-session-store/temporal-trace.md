# S-8 Session 存储 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: SessionHolder + FileSessionManager (root.data) + FileTransactionStoreManager; ROOT_SESSION_MANAGER_NAME 注释锚 |
| 1.x | **DB 模式**: DataBaseSessionManager + LogStoreDataBaseDAO + 方言 SQL; reload 恢复; onClose setActive(false) |
| 2.x | **REDIS 模式**: RedisSessionManager + RedisLuaLocker/RedisLuaTransactionStoreManager (Lua 原子); **RAFT 模式**: RaftSessionManager + 分布式锁; SessionMode 4 值完整; FlushDiskMode |
| 2.5.0 | 方言扩展至 13 (polardbx/h2/oscar/kingbase...); GlobalTransactionDO 稳定 |

## 痕迹证据

- SessionHolder.java:71: ROOT_SESSION_MANAGER_NAME = "root.data" (0.9 锚)
- AbstractSessionManager.java:85-87: Rollbacking → LockStatus.Rollbacking (1.x 锚)
- AbstractSessionManager.java:154-156: onClose → setActive(false) (1.x 锚)
- storage/redis/: Lua 脚本族 (2.x 锚)
- storage/raft/: RaftSessionManager + RaftDistributedLocker (2.x 锚)
- GlobalSession.java:831-850: GlobalSessionLock tryLock 2s (1.x 锚)

## 推断标注

- "0.9~1.x 骨架" — Fescar 起 (公知版本线) (标注)
- "2.x REDIS/RAFT" — 存储目录存在性推断 (标注)
- "2.5.0 13 方言" — sql 目录实证 (实证)
- git 多 commit 可考古 — 本域以目录结构 + 注释锚为主

## 对照线 (阶段 4.3 已交付)

- ZK (4.3): 会话在服务端内存+持久化 vs Seata 会话在 TC 存储 — 会话生命周期管理对照
- Z-9 (4.3): FileTxnLog forceSync vs Seata FlushDiskMode — 刷盘语义对照
