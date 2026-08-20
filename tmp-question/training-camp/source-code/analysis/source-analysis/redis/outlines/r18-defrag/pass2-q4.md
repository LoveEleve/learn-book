# 闭环笔记 q4: 大键延后 — defrag_later 列表与 scanLater 族

## 假设
大键 (字段数 > 阈值) 不进主扫描, 而是登记到 defrag_later 列表, 由 scanLater 族分批续扫 (防延迟尖峰)。

## 验证过程
- **动机** (defrag.c:383-385): "when the value has lots of elements, we want to handle it later and not as part of the main dictionary scan. this is needed in order to prevent latency spikes when handling large items"
- **登记** (L386-389): defragLater — `sdsdup(key)` 入 `db->defrag_later` 列表 (注意: 拷贝键名, 因为主键名可能随后被搬)
- **阈值** (L492/513/532/546/703): 各处 `> server.active_defrag_max_scan_fields` (默认 1000, config.c:3196) — quicklist.len / zset dictSize / hash dictSize / set dictSize / stream raxSize
- **主循环衔接** (L1185-1210): 每桶扫描前先 defragLaterStep (清积压); 桶扫完若列表非空 → defrag_later_item_in_progress=1 继续 (L1201-1204) — 大键延迟不影响游标推进
- **defragLaterStep** (L950-1010): 静态续扫状态 (defrag_later_current_key/cursor); 每键重查 dict (键可能已删 — L937-940 容错); 内循环每 **16 迭代/512 重分配/64 字段** 检时限 (L990-992); 完成后统计 key_hits/misses 并从列表摘除 (L962-967)
- **scanLater 三种续扫技术**:
  - **list** (L392-433): `quicklistBookmark "_AD"` 续扫 — 游标=bookmark (quicklist 7.x 功能); 128 节点/时限检; bookmark 创建失败 → bookmark_failed 下次从头 (L417-427); 注意 `ob->ptr = ql` (bookmark 创建可能重分配 quicklist, L422)
  - **set/zset/hash** (L446-484): dictScanDefrag 游标续扫 (q2 复用)
  - **stream** (L567-616): **static last[16] 保存最后 ID** + `raxSeek(">", last)` 续扫 (L586-590); rax 节点经 `ri.node_cb = defragRaxNode` 搬移 (L583) + entry data 搬 (L599-601); 128 迭代/时限检; serverAssert(ri.key_len==sizeof(last)) (L605 — 续扫 ID 必须完整 16B)
- **module 延后** (L918-935): defragLaterItem → moduleLateDefrag (module.c:13553) — 模块键自己的 cursor 续扫
- **关闭时清理** (L1074-1076): 禁用 defrag 中途 → listEmpty(defrag_later) + 静态状态复位

## 代码类型
Implementation (延迟任务队列 + 多技术续扫)

## 跨域关联
- R-5 (quicklist): Bookmark 功能
- R-10 (stream/rax): rax 迭代器 node_cb
- R-31 (module): moduleLateDefrag

## 结论
大键 = defrag_later 队列 + 三类续扫 (bookmark/游标/static-last); 主扫描与延后扫描交错 (每桶前清积压); 每键 16/512/64 三条件检时限 — 延迟尖峰防护闭环。
源码位置: defrag.c:383-389,392-484,567-616,918-1010,1185-1210
