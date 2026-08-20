# 闭环笔记 q1: 搬移原语 — je_get_defrag_hint 与 no_tcache

## 假设
逐指针判定"值得搬"由 jemalloc 补丁 API 完成; 搬移用 no_tcache 分配防拿回同一块。

## 验证过程
- **判定 API** (defrag.c:30-32): `je_get_defrag_hint` — "this method was added to jemalloc in order to help us understand which pointers are worthwhile moving"
- **jemalloc 实现** (jemalloc_internal_inlines_c.h:341-400, iget_defrag_hint):
  - 仅 **小分配** (alloc_ctx.slab) 判定; 大分配 → defrag=0 (不搬 — 大对象不产生小 bin 碎片)
  - **跳过 slabcur** (L362): "Don't bother moving allocations from the slab currently used for new allocations" — 当前写入 slab 的对象搬走无意义
  - slab 有 free 位才考虑: 统计该 bin 全部 shard 的 non-full slabs 与有效 regs (扣除 full slabs 和 slabcur)
  - **判定式** (L389): `(nregs - free_in_slab) * curslabs <= curregs + curregs/8` — 本 slab 占用率 ≤ 平均 + **12.5% 防停滞权重** (L383-385: "avoid stagnation when all slabs have the same utilization")
  - 每次判定加 bin 锁 (L366/396-397) — 单线程但代码防御性加锁
- **搬移流程** (defrag.c:39-55 activeDefragAlloc): hint=0 → misses++; hint=1 → `zmalloc_usable_size` (原大小) → `zmalloc_no_tcache(size)` → memcpy → `zfree_no_tcache` → hits++
- **no_tcache** (zmalloc.c:199-213): `mallocx(size+PREFIX_SIZE, MALLOCX_TCACHE_NONE)` / `dallocx(ptr, MALLOCX_TCACHE_NONE)` — 注释: "bypass the thread cache ... so that we don't get back the same pointers we try to free" (defrag.c:47-48)
- **指针家族** (偏移保留模式):
  - activeDefragSds (L62-71): sdsAllocPtr 取分配起点, 搬后 offset 保留 — sds 头部在分配内
  - activeDefragHfield (L78-87): 同款 (hfield 对齐嵌入)
  - activeDefragStringObEx (L96-127): **expected_refcount 检查** (共享对象不搬, L98-99) + **EMBSTR 特例** (L115-121): sds 在 robj 分配内嵌, 搬 robj 后 `ret->ptr = (intptr_t)ret + ofs` 重算偏移; INT 编码跳过
  - dictDefragTables (L165-183): dict 结构 + ht_table[0]/[1] 双表指针
  - zslDefrag (L209-242): update[] 数组重链 + score 引用更新 (dict val 指向 zsl 内 score)

## 代码类型
Implementation (搬移原语 + 判定算法)

## 跨域关联
- R-33 (zmalloc): mallocx/dallocx with_flags / zmalloc_usable_size
- R-1 (object): EMBSTR 同 chunk / refcount 共享
- R-10a (rax): 大键 rax 搬移 (q4)

## 结论
判定 = jemalloc 补丁 iget_defrag_hint: 小分配 + 非 slabcur + slab 利用率 ≤ 平均+12.5% → 值得搬; 搬移 = no_tcache 分配-拷贝-释放 (防拿回同块); 指针家族按类型保留偏移/重算内嵌指针。
源码位置: jemalloc_internal_inlines_c.h:341-400; defrag.c:30-55,62-127,165-183,209-242; zmalloc.c:199-213
