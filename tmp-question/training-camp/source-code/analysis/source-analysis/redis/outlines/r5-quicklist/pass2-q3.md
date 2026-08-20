# 闭环笔记 q3: 压缩策略 — 两端深度 + 收益阈值

## 假设
压缩只作用于"两端 compress 深度之外"的节点 (头尾常被 LPUSH/LPOP 访问, 不压); 且节点 <48B 不压、压缩收益 <8B 不压 — 三条件。

## 验证过程
- __quicklistCompress (quicklist.c:307-345): `quicklist->len < compress*2 → return` (长度不足无节点可压); 迭代两端跳过 compress 深度
- 配置 (config.c:3174): list-compress-depth 默认 0 (关闭)
- __quicklistCompressNode (L214-245):
  - L224-226: `node->sz < MIN_COMPRESS_BYTES (48) → return` — 小节点压缩不值
  - L234: `lzf->sz + MIN_COMPRESS_IMPROVE (8) >= node->sz → return` — **收益不足 8B 不压** (压缩后 +8 ≥ 原大 → 放弃)
  - lzf_compress 失败 (不可压缩数据) → return
- 压缩只压 RAW 节点 (quicklistCompressNode 宏 L247-252)
- #if 0 块 (L326-350): 旧版显式 depth=1/2 分支 — 被通用迭代替代 (演进痕迹)
- 语义: compress=N → 两端各 N 个节点保持解压 (LPUSH/LPOP/RPOP 高频面), 中间节点压缩

## 代码类型
Algorithmic (压缩调度) — 访问模式感知

## 跨域关联
- R-26 (t_list LPUSH/LPOP) → 两端访问面
- LZF (deps/lzf) → 压缩实现
- R-33 (zmalloc) → 压缩缓冲

## 结论
压缩三条件: 深度外 (访问低频) + ≥48B (有压缩价值) + 收益 ≥8B (压缩率足够)。头尾永远解压 (高频操作面) — 压缩是"冷中间区"的优化。默认关闭 (compress=0), 大列表才开。
源码位置: quicklist.c:78,83,214-252,307-345; config.c:3174
