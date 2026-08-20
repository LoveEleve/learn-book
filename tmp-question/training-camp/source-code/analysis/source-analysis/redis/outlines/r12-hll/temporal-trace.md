# R-12 HyperLogLog — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2014 (2.8.9) | 初版 PFADD/PFCOUNT/PFMERGE (版权 "2014-Present"; commands.def 全部 since 2.8.9); 稠密编码为主; 文件头注释引用 Flajolet 原论文 [2] 与 Heule 等 [1] |
| 2017 (4.0) | **Ertl 改进估计器** (arXiv:1702.01284, 2017-02; 注释 L959-961/L976-978 引用) — 替代经典 Σ2^-M 调和平均, 保留 alpha_inf 外壳 (版本推断) |
| 7.x | hll-sparse-max-bytes MODIFIABLE_CONFIG (config.c:3224, 可 CONFIG SET 动态改) — 测试面 tests/unit/hyperloglog.tcl 有 "Change hll-sparse-max-bytes" 用例 (版本推断) |
| 2024 (7.4.2) | 现状: 稀疏起步/稠密终态双表示 + RAW 多键归并 + PFDEBUG 四子命令 |

## 痕迹证据

- L16-27: 文件头设计注记 — 64bit 哈希 (基数 >10^9)、16384×6bit=12k、复用字符串类型、无压缩 (对照 [1] 的压缩变体)、无 2^32 附近修正 (因 64bit)
- L86-108: 稀疏三 opcode 位域规范注释 (权威依据)
- L110-121: 稀疏纯位置示例 (3 非零寄存器 → 7 字节 opcode 序列)
- L128-158: 基数 vs 字节数实测表 (100→267B … 10000→10591B) — 稀疏优势区间 ~2000-3000 (L153-155)
- L372-374: MurmurHash64A "modified for Redis… endian neutral" — 跨架构确定性
- L959-991: Ertl 估计器引用 + sigma/tau 定义
- L1008-1012: reghisto 安全尺寸注释

## 推断标注

- "Ertl 估计器 4.0 引入" — 版本推断 (arXiv 2017-02 论文 + 4.0 发布时间 2017-07; 仓库浅克隆无法 git 验证)
- "1.04 为 Flajolet 论文经验常数" — 外部知识推断 (代码 L1432 只给出公式)
- "count≥33 稀疏阶段不可达 (概率 2^-33)" — 概率推断, 佐证 VAL 值上限 32 的设计安全
- "栈上 16KB 主线程安全" — 推断 (默认栈 8MB, 深度调用链有限)
