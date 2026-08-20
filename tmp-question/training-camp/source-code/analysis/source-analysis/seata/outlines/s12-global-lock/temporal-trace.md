# S-12 全局锁体系 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: lock_table + DataBaseLocker + AbstractLockManager (collectRowLocks) + lockQuery 只查不锁 |
| 1.x | **LockStatus**: Locked/Rollbacking (S-8 联动); updateLockStatus; 冲突 failFast 面 |
| 2.x | **LockerManagerFactory SPI** (LockMode 4 值); Redis/File/Raft 族扩展 (RedisLuaLocker/RedisDistributedLocker/RaftDistributedLocker); 13 方言 LockStoreSql |
| 2.5.0 | DistributedLocker 族稳定 (DB/Redis/Raft 3 实现) |

## 痕迹证据

- LockerManagerFactory.java: "use lock store mode: {}" — 模式加载 (2.x 锚)
- LockStoreDataBaseDAO.java: "Global lock on [{}:{}] is holding by xid {} branchId {}" (1.x 锚)
- AbstractLockManager.java:122-165: collectRowLocks 解析 (1.x 锚)
- LockStatus.java:25-30: Locked(0)/Rollbacking(1) (1.x 锚)
- storage/{db,file,redis,raft}/lock/: 双族目录 (2.x 锚)

## 推断标注

- "0.9~1.x 骨架" — Fescar 起 (公知版本线) (标注)
- "2.x SPI/族扩展" — 目录结构实证 (实证)
- "2.5.0 分布式族" — 实现存在性推断 (标注)
- git 多 commit 可考古 — 本域以目录/注释锚为主

## 对照线 (阶段 4.3 已交付)

- ZK WriteLock (Z-8): 顺序节点前驱链 vs Seata lock_table 检查-插入 — 两种分布式锁思路 (事件驱动 vs 轮询重试)
- ZK ReadWriteLock: RW 锁语义 vs Seata 单一行锁
