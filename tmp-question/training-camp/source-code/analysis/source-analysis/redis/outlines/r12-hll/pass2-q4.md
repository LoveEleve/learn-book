# 闭环笔记 q4: hllCount — Ertl 估计器 (认知修正)

## 假设 (来自交接文档 §四)
"调和平均: alpha_m × m² / Σ 2^-M" — 经典 Flajolet 公式。

## 验证过程 — ⚠️ 假设被推翻
- 实测 7.4.2 主公式 (L1030-1038):
  ```
  z = m * hllTau((m - reghisto[HLL_Q+1]) / m);   // HLL_Q+1 = 51
  for (j = HLL_Q; j >= 1; --j) { z += reghisto[j]; z *= 0.5; }   // 递推折叠
  z += m * hllSigma(reghisto[0] / m);
  E = llroundl(HLL_ALPHA_INF * m * m / z);
  ```
- hllSigma (L962-974) / hllTau (L979-991): **Ertl 改进估计器**, 注释明确引用 `Otmar Ertl, arXiv:1702.01284` (L959-961, L976-978)
- HLL_ALPHA_INF = 0.721347520444481703680 (L368, 注释 "0.5/ln(2)") = 1/(2·ln2) — m≥128 时的渐近 alpha 常数 (经典公式按 m 分级: m=16→0.673 / 32→0.697 / 64→0.709 / ≥128→0.7213; 16384 恒取渐近值)
- reghisto[64] (L1013): count max=51, 64 格安全冗余 (L1008-1012 注释)
- 三编码直方图分派 (L1016-1025): DENSE 展开版 / SPARSE (附 invalid 检测: idx≠16384 → *invalid=1, L925) / RAW 1B/寄存器 (L936-957, 8B/轮全零字跳过)
- 误差: PFSELFTEST `relerr = 1.04/sqrt(HLL_REGISTERS)` (L1432) = 1.04/128 = 0.008125 = **0.8125%**; 1.04 为 Flajolet 论文经验常数 (推断标注)

## 代码类型
Mechanism (统计估计器) — **认知修正**

## 跨域关联
- 交接文档 §四 "调和平均 alpha_m×m²/Σ2^-M" — **表述过时**: 经典公式是 Flajolet 2010 原版; Redis 4.0 起换 Ertl (2017) 版本, 仅保留 alpha_inf 渐近常数与 E=alpha·m²/z 外壳
- R-11: 对照 精确 popcount vs 概率估计

## 结论
hllCount = 直方图 + Ertl sigma/tau 估计器 (非经典调和平均), E = round(alpha_inf·m²/z)。0.8125% 标准误差由 1.04/√16384 给出。
源码位置: hyperloglog.c:962-991,1004-1039,1432
