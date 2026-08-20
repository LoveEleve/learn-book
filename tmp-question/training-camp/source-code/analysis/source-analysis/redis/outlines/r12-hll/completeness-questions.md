# R-12 HyperLogLog — completeness-questions

## 开发者视角

1. PFADD 一个元素怎么映射到寄存器? (哈希分域)
2. 稀疏三 opcode (ZERO/XZERO/VAL) 分别能表示什么?
3. hllSparseSet 的 A/B/C/D 四分支是什么?
4. 稀疏→稠密提升的触发条件?
5. 基数缓存的 MSB 有效位怎么工作?
6. PFCOUNT 多键怎么算? (RAW 归并)
7. 稠密 6bit 打包的位序? (LSB-first)
8. hllCount 的 Ertl 估计器公式?

## 架构师视角

9. 为什么固定 seed 无随机化? (对照 dict SipHash)
10. 空 HLL 18B vs 稠密 12304B — 稀疏的收益区间?
11. 只读命令改值 + MAY_REPLICATE 的传播设计?
12. 升稠不降稀的单向性设计意图?
13. 多键 PFCOUNT 栈上 16KB 数组的取舍?
14. 0.81% 误差的来源 (1.04/√16384)?
15. HLL 为什么复用字符串类型而非独立类型?
16. 校验分层 (头浅检 + 使用深检)?

## 学生视角

17. 前导零计数 count 为什么从 1 起, max 为什么 51?
18. XZERO 分裂为什么最坏 5 字节?
19. PFCOUNT 缓存命中与未命中的执行路径差异?
20. PFSELFTEST 怎么验证误差 (6σ 界)?
