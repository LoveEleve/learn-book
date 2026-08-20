# 闭环笔记 q7: 迭代器与压缩节点访问

## 假设
quicklistIter 在节点间游走: 遇压缩节点先解压 (recompress 标记), 操作完由调用方触发重压; 迭代器在结构变更后失效 (resetIterator)。

## 验证过程
- quicklistIter (quicklist.h:114+): current 节点 + offset/zi (listpack 内偏移) + direction
- 遍历逻辑: _quicklistNext (L720+): 遇 encoding==LZF → `quicklistDecompressNode` (解压) — **遍历自动解压**
- recompress 处理 (L380-390 区域): 遍历完成后节点带 recompress 标志 → 统一重压
- resetIterator (L142-148): 插入/替换等结构变更后迭代器作废 (current=NULL) — 防止失效指针
- 头尾守卫 (L311-312): 头尾永不压缩/重压
- 消费者: t_list 的 LINDEX/LRANGE/LINSERT 全走迭代器
- 快慢指针: direction (HEAD/TAIL) 双向游走 — ZREVRANGE 式反向遍历 (list 的 RPOP 面)

## 代码类型
Interface (迭代器契约) + Implementation

## 跨域关联
- R-26 (LINDEX/LRANGE) → 消费者
- q4 (recompress) → 遍历解压面
- R-19 (lpNext 节点内游走) → 包内偏移

## 结论
迭代器 = 跨节点游走 + 自动解压 (recompress 延迟重压) + 结构变更失效 (resetIterator)。双向 (direction) 支撑正向/反向遍历。压缩对迭代透明 — 解压发生在需要时。
源码位置: quicklist.h:114+; quicklist.c:720+,380-390
