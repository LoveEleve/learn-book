# 闭环笔记 q5: 16B 头与基数缓存 — 只读改值

## 假设
PFCOUNT O(1) 靠缓存; 缓存失效用 MSB 位; 只读命令可能改值并传播。

## 验证过程
- hllhdr (L161-167): `magic[4]="HYLL" + encoding(1B) + notused[3] + card[8]` (LE 64bit) = 16B
- 缓存协议 (L169-171): **card[7] 的 MSB=1 表示失效** (HLL_INVALIDATE_CACHE), =0 有效 (HLL_VALID_CACHE) — 缓存语义: "自上次计算后未修改" (L57-61 注释)
- PFADD 失活 (L1202); hllSparseSet 失活 (L869); PFMERGE 失活 (L1370)
- PFCOUNT 单键 (L1259-1304):
  - `lookupKeyRead` 非 Write (L1259) — 注释解释从库语义: 逻辑过期键从库不删 (L1254-1258)
  - 缓存有效 → 8 字节组装直返 (L1270-1279) **零计算**
  - 失效 → hllCount 重算 + **回写缓存** (L1283-1295) + signalModifiedKey + dirty++ (L1299-1300) → 传播
  - 因会改值 → dbUnshareStringValue (L1266) 副本分离 (R-21)
- 命令标志 (commands.def L11057): **CMD_READONLY|CMD_MAY_REPLICATE** — "只读命令"语义放宽: 计算是只读, 缓存回写是副作用且需传播
- isHLLObjectOrReply (L1140-1168): 类型/编码 (sdsEncodedObject)/长度≥16/magic/encoding≤HLL_MAX_ENCODING=1/**稠密长度必须精确 12304** (L1158-1159)
- 校验深度: 稀疏内部结构仅在 hllSparseRegHisto/hllSparseToDense 时验证 (invalid 标志) — 头部浅校验 + 使用时深校验

## 代码类型
Mechanism (失效缓存) + 命令语义变体

## 跨域关联
- R-21: lookupKeyRead/Write 语义、dbUnshareStringValue、signalModifiedKey (已交付)
- R-28: 传播面 (MAY_REPLICATE 变体 — 只读命令触发传播的唯一案例)
- R-1: 共享回复 (cone/czero)

## 结论
缓存 = 16B 头内嵌 + MSB 有效位; PFCOUNT 命中零计算, 未命中重算回写并**以只读命令身份传播** (MAY_REPLICATE)。校验: 头浅检 + 使用深检。
源码位置: hyperloglog.c:161-171,1140-1168,1211-1304
