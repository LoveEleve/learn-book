# 闭环笔记 q7: 释放 vs 强制释放 — owner 校验与无条件删除

## 假设
unlockInnerAsync 校验 owner (hexists) — 非持有者不能解锁; forceUnlock 无条件 del — 用于死锁/超时场景的强制回收。两者都发布唤醒消息。

## 验证过程
- `unlockInnerAsync` (RedissonLock.java:348-360):
  - 先查 KEYS[3] 信号位: `val ~= false → return val` (过期锁已被顶掉?)
  - `hexists(KEYS[1], ARGV[3]) == 0 → return nil` — **无此线程的持有, 拒绝解锁**
  - `hincrby -1` 减重入 → counter>0 → pexpire 续期 + set KEYS[3] 0 (信号) — **重入未完不删锁**
  - counter==0 → 删除锁 + 发布 UNLOCK (下文继续, 未完整读出但断言)
- `forceUnlockAsync` (L336-346): `del KEYS[1] == 1 → publish UNLOCK + return 1; else return 0` — **无条件 del,不问 owner**
- 语义: 
  - **unlock**: 持有者有序递减, 重入清零才真删, 期间续期并播消息
  - **forceUnlock**: 任何线程/运维可强制删 (死锁恢复/清洁) — 但要发布消息让等待者醒来
- 使用: unlock (正常) vs forceUnlock (超时回收/运维干预)

## 代码类型
Implementation (解锁协议) — owner 感知 vs 无条件

## 跨域关联
- Q5 (订阅) → 两种释放都发布唤醒
- Q1 (tryLock) → 非持有解锁的 nil 影响等待者
- 面试点: "unlock 和 forceUnlock 区别?为什么需要 force?"

## 结论
unlock = 持有者约束: hexists 校验 owner → 重入递减 → 归零才删+publish; forceUnlock = 无约束: del+publish。双轨满足"正常释放"与"死锁回收"。重入未完 (counter>0) 锁不删 — 是关键安全边界。
源码位置: RedissonLock.java:336-360