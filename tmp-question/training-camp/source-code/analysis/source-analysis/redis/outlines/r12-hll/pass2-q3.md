# 闭环笔记 q3: 稀疏编码 — 三 opcode 与原地更新

## 假设
纯位置游程编码 (ZERO/XZERO/VAL); 空 HLL 极小; 更新 = 分裂+合并; 升稠不降稀。

## 验证过程
- 三 opcode 位域 (L344-367 + L86-108 注释):
  - **ZERO** `00xxxxxx` (1B): 6bit 长度+1 → 1-64 个连续 0 (L349)
  - **XZERO** `01xxxxxx yyyyyyyy` (2B): 14bit 长度+1 → 1-16384 个连续 0 (L350)
  - **VAL** `1vvvvvxx` (1B): 5bit 值+1 (1-32) × 2bit 长度+1 (1-4) (L351-352)
  - 前缀判定 00/01/1 互斥 (L346-348); 常量 L353-356
- **空 HLL = 18 字节** (L110-111 注释 "XZERO:16384"; createHLLObject L1105-1135): sparselen = 16 + ceil(16384/16384)*2 = 18 (L1110-1112); 循环写一个 XZERO:16384 (L1120-1126)
- 头注释平均表 (L133-151): 100 元素→267B / 1000→1882B / 10000→10591B — 对比稠密 12288B
- hllSparseSet 更新状态机 (L634-886):
  - count>32 → promote (L642)
  - 预留扩容 (L653-659): avail<3 且 alloc<上限 → greedy `newlen += min(newlen,300)` 封顶 max_bytes
  - 定位循环 (L670-692): 顺序扫 opcode 累加 span, **条件排序按频率** (L675-677: ZERO/VAL/XZERO)
  - 四分支: A 旧值够大→不动返回 0 (L733-737) / B VAL len1 直写 (L738-742) / C ZERO len1 替换 VAL (L747-750) / D 通用分裂 (L767-811): ZERO/XZERO 拆三段 (左零+VAL+右零, L771-794), VAL 拆三段 (左+新+右同值, L795-810); **seq[5] 上限** = XZERO(2)+VAL(1)+XZERO(2) (L758-761)
  - Step3 替换 (L813-827): memmove 右移 + sdsIncrLen + memcpy; deltalen 超 max_bytes → promote (L821-822)
  - Step4 合并 (L835-865): 从 prev 扫 ≤5 个 opcode (scanlen=5, L836); 相邻同值 VAL 且合长 ≤4 → 合并删一字节 (L847-863)
  - promote 路径 (L872-886): hllSparseToDense → hllDenseSet 必返回 1 (serverAssert L884)
- hllSparseToDense (L564-617): sdsnewlen(NULL, 12304) + 复制旧头 (L579) + 逐 opcode 写寄存器; **校验 idx==16384** (L608) 否则 C_ERR

## 代码类型
Mechanism (游程编码 + 原地更新状态机)

## 跨域关联
- R-7 intset: **对照 — 升而不降同哲学** (HLL 稀疏→稠密单向, 无降级)
- R-4: sdsIncrLen/sdsResize 复用; sdsnewlen(NULL) 零填充
- R-11: 对照 位图无 RLE vs HLL 稀疏游程 (R-11 大纲负面空间已预告)

## 结论
稀疏 = 纯位置游程: 最小 18B (空), 更新靠分裂+合并保持最简, 两触发升稠 (值>32 / 超 hll_sparse_max_bytes=3000)。**单向提升, 稠密永不降回**。
源码位置: hyperloglog.c:344-367,564-617,634-886
