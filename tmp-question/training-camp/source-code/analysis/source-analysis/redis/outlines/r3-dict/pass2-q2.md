# 闭环笔记 q2: 触发阈值体系 — 1:1/4:1/1:8/1:32 + COW 三态

## 假设
扩容/缩容由 used/buckets 比例触发: 正常 1:1 扩、<1:8 缩; fork 场景降级 AVOID (4:1 才扩、1:32 才缩) — 目的是 COW 保护。

## 验证过程
- dictExpandIfNeeded (dict.c:1492-1515): rehash 中→返回; 空表→扩到 4 桶; **1:1 比例触发扩容** (注释 L1502: "If we reached the 1:1 ratio"); AVOID 模式: ratio 超"安全阈值"才扩
- dictShrinkIfNeeded (L1533-1550): `<= DICT_HT_INITIAL_SIZE (4 桶) 不缩`; **ENABLE: <1:8 缩** (`ht_used[0] * HASHTABLE_MIN_FILL <= size`); **非 FORBID (AVOID): <1:32 缩** (`* dict_force_resize_ratio`)
- 全局三态 (dict.c:41): DICT_RESIZE_ENABLE/AVOID/FORBID + dict_force_resize_ratio=4 + HASHTABLE_MIN_FILL=8 (dict.h:27)
- **COW 策略 (server.c:640-652 updateDictResizePolicy)**: `in_fork_child != NONE → FORBID` (子进程在, 完全禁 resize); `hasActiveChildProcess → AVOID` (RDB/AOF/复制子进程活跃 — 避免 COW 页复制); 否则 ENABLE
- dictRehash 的 AVOID 检查 (L393-398): 扩时 s1 < 4*s0 停下 / 缩时 s0 < 32*s1 停下 (阈值内暂停迁移)
- 头部注释 (dict.c:31-40): "we use copy-on-write and don't want to move too much memory around when there is a child performing saving operations"

## 代码类型
Algorithmic (阈值策略) + Glue (fork 生命周期联动)

## 跨域关联
- R-8 (RDB/AOF fork 子进程) → AVOID/FORBID 的触发方
- R-20 (updateDictResizePolicy 调用链) → 生命周期
- R-18 (defrag 时?) → 迁移暂停配合

## 结论
阈值双轨: 正常 ENABLE (1:1 扩 / 1:8 缩), 子进程活跃 AVOID (4:1 扩 / 1:32 缩 — 延迟到"必须"才动), fork 中 FORBID (完全冻结) — 全部为 COW 保护服务: 迁移会复制页, 子进程在读旧页时迁移 = 大量页复制。
源码位置: dict.c:41-42,1492-1550; server.c:640-652
