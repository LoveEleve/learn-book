# 闭环笔记 q5: 单指针 entry 优化 — no_value + keys_are_odd

## 假设
7.x 对"无 value 字典" (如 set 内部 dict) 做了内存优化: key 是奇数指针 (sds) 时直接以指针本身作为 entry 存桶里 (省 16+ 字节/元素); 否则用无值 entry (省 value 字段)。

## 验证过程
- **三态编码** (dict.c:128-171): entry 指针的低 2 位 (ENTRY_PTR_MASK) 编码:
  - entryIsKey: `de & 1` — **key 指针直存** (无 entry 分配!)
  - entryIsNormal: ENTRY_PTR_NORMAL — 完整 entry (key+val+next)
  - entryIsNoValue: ENTRY_PTR_NO_VALUE — dictEntryNoValue (key+next, 无 value)
- encodeMaskedPtr (L152): 指针 OR 位标记; decodeMaskedPtr: 掩码清位 — 前提: 被编码指针低位为 0 (malloc 对齐)
- **keys_are_odd 前提** (server.c:475): `keys_are_odd = 1 /* an SDS string is always an odd pointer */` — **奇数指针保证机制**: sdsnewlen (sds.c:101) `s = (char*)sh+hdrlen` — malloc 返回 16 对齐 (偶数) + **hdrlen 恒为奇数 (sdshdr5=1/8=3/16=5/32=9/64=17B)** → sds buf 指针 = 偶数+奇数 = **恒奇** — entryIsKey 用 `&1` 即可识别
- no_value 消费: server.c:474/634 (db 键空间? set 内部?) — set 的 dict (t_set) 用 no_value

## 代码类型
Algorithmic (指针编码优化) — 内存压缩

## 跨域关联
- R-4 (sds 奇数指针约定) → 编码前提
- R-27 (t_set 内部 dict) → no_value 消费方
- R-33 (zmalloc 对齐) → encodeMaskedPtr 前提

## 结论
7.x 单指针优化: 无值字典的 key 在**桶空时**直存桶 (省整个 entry, 无 next 可链), 桶非空退回 no-value entry (带 next); 指针低 3 位编码三态。keys_are_odd 依赖 sds 指针恒奇 (奇数头+malloc 对齐)。
源码位置: dict.c:128-171; server.c:474-475
