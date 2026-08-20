# 闭环笔记 q5: 引用计数 — 分派释放 + 特殊值守卫

## 假设
refcount==1 → 释放 (按类型分派 freeXxxObject + zfree 外壳); >1 → 递减; 特殊值 (共享/栈上) 不碰 — 三态语义。

## 验证过程
- incrRefCount (object.c:349-359): `refcount < OBJ_FIRST_SPECIAL_REFCOUNT → ++`; 共享 → 无操作 (不可变); 栈上 → serverPanic
- decrRefCount (L361-377):
  - `refcount == 1` → 按 type 分派释放: freeStringObject/freeListObject/freeSetObject/freeZsetObject/freeHashObject/freeModuleObject/freeStreamObject → zfree(o)
  - 否则: `refcount != OBJ_SHARED_REFCOUNT → --` (共享永不减)
  - 守卫: `refcount <= 0 → serverPanic`
- makeObjectShared (L56-60): refcount = OBJ_SHARED_REFCOUNT (断言原为 1)
- 生命周期: 命令参数 → 键空间 → 回复 → 各阶段 incr/decr 配对; 共享对象 (shared.integers 等) 全程不计数
- 与 RCU 对照: 单线程下引用计数无锁; 特殊值让共享对象"免维护"

## 代码类型
Implementation (内存管理) — 生命周期契约

## 跨域关联
- R-20 (命令参数/键值生命周期) → 主消费
- R-5/R-6/R-19 (freeXxxObject 分派) → 释放面
- R-33 (zfree) → 底层

## 结论
引用计数三态: 1→释放 (分派), >1→递减, 特殊值→不碰。共享对象 (OBJ_SHARED_REFCOUNT) 永久存活免维护; 栈上对象 (OBJ_STATIC_REFCOUNT) 禁止 incr。单线程下无锁配对。
源码位置: object.c:56-60,349-377
