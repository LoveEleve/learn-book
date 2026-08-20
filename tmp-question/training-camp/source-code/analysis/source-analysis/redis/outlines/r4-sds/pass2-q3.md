# 闭环笔记 q3: 扩容三路 — 头部不变才能 realloc

## 假设
扩容时头部类型相同 → realloc 原地 (allocator 可能零拷贝); 头部类型升级 (长度跨越阈值) → 不能 realloc, 必须 malloc+memcpy+free — 因为 sds 指针位置变了, realloc 会破坏 s[-1] 布局。

## 验证过程
- sds.c:248-262 (_sdsMakeRoomFor):
  - `if (oldtype==type)` → `s_realloc_usable(sh, hdrlen+newlen+1, &usable)` — **realloc 原地**, s 指针 = newsh+hdrlen (同头部偏移)
  - else → `s_malloc_usable + memcpy((char*)newsh+hdrlen, s, len+1) + s_free(sh)` — 注释 L253-254: "**Since the header size changes, need to move the string forward, and can't use realloc**"
- 为什么不能 realloc: realloc 保底"同指针或复制到新址", 但新头部 (如 8→16, 头 3→5 字节) 时 buf 起点偏移改变 — 若 realloc 原地扩展 (从 sh 起点), buf 位置 = newsh+hdrlen(new) ≠ 原 buf 地址, 但所有引用 s 的调用方期望 s 不变 (同一地址) — 矛盾; 若 realloc 挪到新址, memcpy 语义等价但裸 realloc 不会"平移内容到新偏移"
- 三路收益: 同型 realloc 最省 (allocator 原地膨胀, 无复制); 升级路径显式 memcpy (1 次复制 + 1 次 free)
- 降级缩容 (sdsResize) 同理: use_realloc = oldtype==type || (type<oldtype && type>SDS_TYPE_8) (sds.c:327) — 缩小到 8 以上 (头变小) 也可 realloc? 注意: 缩小到 16→8 头变小 — buf 偏移从 5→3, 同样有平移问题... 但 use_realloc 允许? 看 sdsResize 后续 (L344+) 处理 — 缩小时 data 往前挪? 需要验证: 缩型时 newsh = s_malloc + memcpy (sds.c:344 else 分支)。所以 use_realloc 只对同型/小缩 (>8) 成立 — 但 16→8 头变小还是不行? use_realloc 条件是 type>8 (8/16/32/64 族) — 16→8: type=8 > 8? 否!条件 `type > SDS_TYPE_8` = type>1 — 8 型不满足!所以 16→8 走 else (手动)。而 32→16: type=2 > 1 ✅ realloc。**等等 — 32→16 头从 9 变 5, buf 偏移变了, realloc 原地也不行啊!** 需要看 sdsResize 的 use_realloc 分支怎么处理偏移。

## 代码类型
Algorithmic (内存布局约束)

## 跨域关联
- R-33 (zmalloc s_realloc_usable) → 扩容底层
- R-28 (querybuf 增长) → 高频扩容消费者

## 结论
扩容三路由"头部是否变化"决定: 同型 → realloc (allocator 原地膨胀最优); 升级 → malloc+memcpy+free (buf 偏移变化, 必须平移内容)。sdsResize 的 use_realloc 是"伪降型" (见附)。
源码位置: sds.c:217-268,327

## 附: use_realloc 疑问闭环 (L327-343 验证)

- use_realloc 分支: `newsh = s_realloc(sh, newlen)` 中 **newlen = oldhdrlen+size+1 (用旧头大小!)**, 且 `s = (char*)newsh+oldhdrlen` — **buf 偏移保持旧头**
- **s[-1] 不更新** (else 分支才有 `s[-1] = type`, L349) — 头部类型保持旧值
- 真义: **"伪降型"** — 保留旧头 (len/alloc 字段位置不变, sdslen/sdssetlen 按旧 type 分派依旧正确), 只是把分配请求缩小到 oldhdrlen+size+1, alloc 字段缩到 size
- 为什么 type>8 条件: 目标降到 8 以下 (5) 时, 5 头无 len/alloc 字段语义且保留旧头不省空间 — 必须真换头 (走 else: malloc+memcpy+新头)
- 所以 use_realloc = "旧头继续用, 分配紧凑化" (纯缩 alloc); 真换头只在升级 (变大跨越阈值) 或降到 8/5 时发生 — 16→8 头变小走 else, 32→16 走 use_realloc (保留 32 头但分配紧凑)
