# 闭环笔记 q5: usable 联动 + jemalloc nallocx — 与 R-33 的接力

## 假设
SDS 是 R-33 usable 家族的最大消费者: 创建/扩容时 alloc 字段 = 分配器实际大小 (免费膨胀); sdsResize 缩容时用 je_nallocx 判断"分配大小是否已最优"避免无谓 realloc。

## 验证过程
- 创建联动: sds.c:93-105 (_sdsnewlen) — `s_trymalloc_usable(hdrlen+initlen+1, &usable)` → `usable-hdrlen-1` → **alloc = 分配器实际** (上限 sdsTypeMaxSize(type) 防类型溢出) — R-33 q4 的直接衔接
- 扩容联动: sds.c:248-266 (_sdsMakeRoomFor) — realloc 分支 `s_realloc_usable(sh, hdrlen+newlen+1, &usable)` → 同样 alloc=usable
- **缩容优化 (sdsResize, sds.c:332-338)**: `alloc_already_optimal = (je_nallocx(newlen, 0) == zmalloc_size(sh))` — **je_nallocx 预查询**目标大小的分配桶, 与当前实际分配大小相同 → 跳过 realloc (realloc 也有成本, 即使不移动)!注释: "we aim to avoid calling realloc()...it incurs a cost even if the allocation size stays the same"
- 配套: alloc 字段被设回精确 size (sds.c:354 sdssetalloc(s, size)) — 注释 L296-299: "set to the requested size regardless of the actual allocation size...to avoid repeated calls...when the caller detects that it has excess space"
- 免费膨胀的代价: alloc 语义 = "我承诺可写 alloc 字节" — 消费方 (sdscat 等) 据此免 realloc; 若分配器实际更大也不追认

## 代码类型
Glue (跨层优化接力) + Algorithmic (预查询优化)

## 跨域关联
- R-33 (zmalloc usable 家族/extend_to_usable) → 上游
- R-1 (object.c:601 sdsRemoveFreeSpace 字符串 shrink) → 缩容消费
- jemalloc API: je_nallocx (MALLOCX_* 查询族)

## 结论
usable 接力: 创建/扩容 alloc=分配器实际 (免费膨胀, 免 realloc); 缩容用 je_nallocx 预查询避免"无意义 realloc" (分配桶没变就不调)。alloc 字段是 SDS 内部"可写承诺", 与分配器实际解耦 (设精确值防重复 resize)。
源码位置: sds.c:93-105,248-266,332-338; object.c:601
