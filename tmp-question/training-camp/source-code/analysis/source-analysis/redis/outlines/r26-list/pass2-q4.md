# 闭环笔记 q4: 范围与移动命令

## 假设
LRANGE/LTRIM/LSET/LINSERT/LREM + LMOVE/BLMOVE — 迭代器 + quicklist API; LMOVE 复用 pop+push。

## 验证过程
- lrangeCommand (L856+): 负索引归一 (同 R-24 GETRANGE) → addListRangeReply (L704: quicklist 段 L657 / listpack 段 L678)
- ltrimCommand: listTypeDelRange 保留段
- lsetCommand (L601-635): listTypeReplaceAtIndex (L362, quicklist 直改节点) + 共享对象保护 (L618-621)
- linsertCommand (L513-563): listTypeNext 找 pivot + listTypeInsert (L318, quicklist node 内插入)
- lremCommand: listTypeEqual + listTypeDelete (迭代器删除)
- **lmoveCommand** (L908+): pop src → push dst (listTypePop L1087 附近) — 原子 (单线程) + 空 src 不删 dst 语义
- BLMOVE: 复用 lmove 逻辑 + 阻塞 (blockForKeys with src+dst)
- 传播: LMOVE 原样重写 (无特殊归一)

## 代码类型
Glue (范围/移动命令)

## 跨域关联
- R-5 (quicklist 全部 API) / R-19 (listpack 遍历)

## 结论
范围/移动 = quicklist/listpack 迭代器封装 + 负索引归一 (与 R-24 同模式)。LMOVE 是 pop+push 原子组合 (单线程天然原子), BLMOVE 加阻塞面。
源码位置: t_list.c:513-635,704-736,856-1087
