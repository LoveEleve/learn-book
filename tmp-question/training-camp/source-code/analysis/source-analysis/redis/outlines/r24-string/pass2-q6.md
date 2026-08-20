# 闭环笔记 q6: LCS 与内存防护

## 假设
LCS 用 DP 表 (alen+1)×(blen+1) uint32; 三层内存防护 (UINT32 上限/512MB/ztrymalloc)。

## 验证过程
- lcsCommand (t_string.c:716-929):
  - 选项 (L741-760): LEN/IDX/WITHMATCHLEN/MINMATCHLEN; **LEN+IDX 互斥** (L763-767, "IDX 包含 LEN")
  - **字符串长度上限** (L770-773): `alen >= UINT32_MAX-1 || blen >= UINT32_MAX-1` → 拒 (DP 表索引防溢出)
  - **表大小计算** (L786-789): `lcssize = (alen+1)*(blen+1)` (注释: 上限内不可能溢出) + `lcsalloc = lcssize*4` + **双重检查** (L789: lcsalloc < SIZE_MAX && lcsalloc/lcssize==4 — 乘法溢出验证)
  - **内存上限** (L790-793): lcsalloc > proto_max_bulk_len → 拒 (512MB 瞬态内存上限)
  - **ztrymalloc 降级** (L794): 分配失败 → 错误而非 OOM 崩溃 (R-33 try 家族)
  - DP 主循环 (L802-823): LCS[i][j] 三态 (0/对角线+1/左右 max)
  - 回溯 (L848-905): 匹配/方向选择 + 区间聚合 (arange/brange) + minmatchlen 过滤
- 复杂度: O(alen×blen) 时间 + O(alen×blen×4) 空间 — 大串保护是必须

## 代码类型
Mechanism (DP + 防护)

## 跨域关联
- R-33 (ztrymalloc/OOM 降级) / R-20 (proto_max_bulk_len 配置)

## 结论
LCS = 经典 DP + **三层防护**: UINT32 长度限 (表索引安全) / 512MB 内存限 (proto_max_bulk_len) / ztrymalloc 降级 (分配失败给错误不崩溃)。表大小乘法双重检查 (溢出验证) 是安全面细节。
源码位置: t_string.c:716-929
