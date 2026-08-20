# 闭环笔记 q4: lpInsert 三合一 — 插入/删除/替换的统一写路径

## 假设
lpInsert 一个函数处理插入/删除/替换: delete 强制 REPLACE (零长度元素); LP_AFTER 转 LP_BEFORE (跳下个); 内存操作"扩先 realloc 后 memmove / 缩先 memmove 后 realloc"。

## 验证过程
- lpInsert (listpack.c:821-968):
  - L828-833: `delete = (elestr==NULL && eleint==NULL)` → where 强制 LP_REPLACE (删除 = 替换为零长)
  - L835-843: `where == LP_AFTER → p = lpSkip(p); where = LP_BEFORE` — **统一为两个 case** (注释: "The function will actually deal with just two cases")
  - L845-847: poff = p-lp (realloc 后恢复地址)
  - L849-867: 编码准备 (字符串嗅探/整数直编/删除 0 长)
  - L872: backlen 编码 (delete 时为 0)
  - L875-879: REPLACE 时计算 replaced_len (被替换元素总大小)
  - L881-883: 新总长计算 + UINT32_MAX 上限检查
  - **realloc/memmove 时机 (L893-914)**: 变大且超容量 → 先 realloc (空间不足先扩, 再 memmove 腾位); 变小 → 先 memmove (压缩) 后 realloc (缩容) — **避免 realloc 后又要 memmove 的双倍复制?** 不 — 真正原因: 变大时若先 memmove 可能覆盖未分配区; 缩小时先 realloc 可能原地缩小破坏后续 memmove
  - L923-934: 写入新 entry (编码+backlen)
  - L937-946: 头部更新 (num_elements ±1, total_bytes)
- #if 0 调试块 (L948-965): 强制新指针 — 抓"忘记更新 lp 引用"的调用方 bug (生产禁用)

## 代码类型
Algorithmic (统一写路径) — 操作合并

## 跨域关联
- R-25 (t_hash HSET 用 lpInsert 替换) → 主消费
- 内存面: lp_realloc (listpack_malloc.h) → R-33 分配

## 结论
三合一写路径: 删除=替换零长, AFTER=跳转后 BEFORE — 一个函数覆盖全部写操作, 减少 API 面和 bug 面; realloc/memmove 顺序按"扩先缩后"编排避免越界; UINT32_MAX 上限防头部溢出 (LISTPACK_MAX_SAFETY_SIZE=1GB 提前拦)。
源码位置: listpack.c:821-968
