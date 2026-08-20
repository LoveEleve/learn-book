# 闭环笔记 q6: 命令面 — PFADD/PFCOUNT 多键/PFMERGE/自检

## 假设
多键 = 内存 MAX 归并; PFMERGE 按输入编码选目标; 自检验证误差界。

## 验证过程
- PFADD (L1171-1208): lookupKeyWrite → 缺键 createHLLObject (**稀疏起步** L1180-1182) / 有键 dbUnshare (L1185); 逐元素 hllAdd (L1188-1199, -1=损坏报 invalid_hll_err); 有更新才: 失活缓存+signalModifiedKey+notify "pfadd"+dirty+=updated (L1201-1206); 回复 :1/:0 (L1207)
- PFCOUNT 多键 (L1220-1246): **栈上 `uint8_t max[16+16384]=16400B`** (L1221) + `hdr->encoding=HLL_RAW` 内部编码 (L1227); 逐键 hllMerge (L1237, 缺键当空 L1232); 一次 hllCount (L1244)
- hllMerge (L1059-1099): 稠密逐寄存器 MAX (L1066-1069); 稀疏逐 opcode 展开 (L1076-1096), 校验 idx==16384 (L1096)
- PFMERGE (L1307-1378): **use_dense 决策** — 任一输入稠密 → 目标转稠密 (L1326, L1353); 栈 max[16384] (L1308); 写回: 稠密 hllDenseSet / 稀疏 hllSparseSet 逐寄存器 (L1360-1367, 稀疏写可能中途 promote → 需重取 hdr L1368-1369); 通知 pfadd (L1375, "semantical simplicity" L1373-1374)
- hll_sparse_max_bytes: 默认 **3000** (config.c:3224, createSizeTConfig MODIFIABLE_CONFIG) — 开关点: hllSparseSet 预留 (L653) + deltalen 检查 (L822)
- PFSELFTEST (L1386-1486): HLL_TEST_CYCLES=1000 寄存器随机读写对照 (L1397-1418); **10M 唯一元素双编码并行** (L1436-1478) — dense/sparse 计数必须一致 (L1452), 误差界 `maxerr = ceil(relerr*6*checkpoint)` (L1460, j==10 特判 maxerr=1 L1466), checkpoint 1→10→100… (L1433)
- PFDEBUG (L1495-1596): getreg (强制转稠密+GETREG 全量 L1511-1530) / decode (opcode 转文本 z/Z/v L1532-1566) / encoding (L1568-1573) / todense (L1575-1587)
- 命令标志 (commands.def L11056-11060): pfadd CMD_WRITE|CMD_DENYOOM|CMD_FAST / pfcount CMD_READONLY|CMD_MAY_REPLICATE / pfmerge CMD_WRITE|CMD_DENYOOM / pfdebug+pfselftest CMD_ADMIN

## 代码类型
Command + Mechanism (归并)

## 跨域关联
- R-23: **CMD_DENYOOM** — PFADD/PFMERGE 在 maxmemory 满时被拒 (对照淘汰面)
- R-1: createObject(OBJ_STRING) 承载 — HLL 无独立类型 (L1130)
- R-33: 内存面 — 稀疏最小 18B vs 稠密固定 12304B

## 结论
多键 = 栈上 RAW 归并 (MAX 可交换); PFMERGE 目标编码跟随输入; 自检 = 双编码一致性 + 6σ 误差界。HLL 命令族全部 2.8.9 引入。
源码位置: hyperloglog.c:1059-1099,1105-1135,1171-1378,1386-1486
