# 闭环笔记 q2: 稠密编码 — 6bit 打包

## 假设
16384×6bit 打包 12288B + 16B 头; LSB-first 物理位序; 直方图展开加速。

## 验证过程
- 常量 (L173-181): HLL_P=14 / HLL_Q=50 / HLL_REGISTERS=1<<14=16384 / HLL_BITS=6 / HLL_REGISTER_MAX=63 / HLL_DENSE_SIZE = 16 + (16384*6+7)/8 = **12304**
- 打包宏 (L318-340): `_byte = regnum*6/8`, `_fb = regnum*6&7`; 读 = `((b0>>fb)|(b1<<(8-fb)))&63`; 写 = 掩码清位+或 (L336-339); **LSB-first** (L76-77, L203-205: "starting from the LSB to the MSB")
- 越界安全 (L311-314 注释): 最后一个寄存器读 b+1 越界, 但 **sds 隐式 null 终结符免费提供第 12305 字节** — 免条件分支
- 寄存器值域: count max=51 < 63, 6bit 富余 (L178 注释 "count up to 63")
- hllDenseSet (L473-483): `count > oldcount` 才写, 返回是否变化
- hllDenseRegHisto (L499-554): **HLL_REGISTERS==16384 特化路径 — 16 寄存器/轮 ×1024 轮** (L509-546), r += 12 (16×6bit=96bit=12B); 无条件无分支 (L194-197 注释 "avoid conditionals"); 兜底通用循环 L548-553

## 代码类型
Mechanism (位打包)

## 跨域关联
- R-11: **对照 — MSB-first 文档位序 (SETBIT) vs LSB-first 物理打包 (HLL 6bit)**; 同是位级紧凑, 目的不同 (寻址位 vs 打包位)
- R-4: sds 隐式 null 终结的免费字节

## 结论
稠密 = 16B 头 + 12288B 寄存器 (6bit×16384, LSB-first 交织打包); 展开直方图 1024 轮/16 寄存器; sds 尾零消灭越界条件分支。
源码位置: hyperloglog.c:318-340,499-554
