# R-12 HyperLogLog — Pass 1 探索笔记

> 域: R-12 HyperLogLog (hyperloglog.c) | 🟡 B 方案 | 2026-08-13
> 源码: src/hyperloglog.c (1597) | Redis 7.4.2

## 调用图

```
哈希与定位:
MurmurHash64A (L376-426): 64bit, 字节序无关改造 (endian neutral), 固定 seed 0xadc83b19 (L446)
hllPatLen (L431-459): index = hash & 0x3FFF (低 14 位) + 前导零计数 (高 50 位, max Q+1=51)

稠密编码 (HLL_DENSE):
HLL_DENSE_GET/SET_REGISTER 宏 (L318-340): 6bit 打包, LSB-first
hllDenseSet (L473-483): count > oldcount 才写
hllDenseAdd (L491-496) / hllDenseRegHisto (L499-554): 16 寄存器/轮全展开 ×1024

稀疏编码 (HLL_SPARSE):
三 opcode 宏 (L344-367): ZERO(1B,1-64) / XZERO(2B,1-16384) / VAL(1B,值1-32×长1-4)
hllSparseSet (L634-886): 更新状态机 (A/B/C/D 分裂+合并) + promote
hllSparseAdd (L894-899) / hllSparseToDense (L564-617) / hllSparseRegHisto (L902-926)

计数:
hllCount (L1004-1039): reghisto[64] → Ertl 估计器 (sigma L962-974 / tau L979-991) → alpha_inf·m²/z
hllRawRegHisto (L936-957): HLL_RAW 内部 1B/寄存器, 8B/轮全零字跳过
hllMerge (L1059-1099): MAX 归并

命令面:
createHLLObject (L1105-1135): 空稀疏 = XZERO:16384, 总 18 字节
isHLLObjectOrReply (L1140-1168): magic/encoding≤1/dense 长度精确 12304
pfaddCommand (L1171-1208) / pfcountCommand (L1211-1304, 多键 L1220-1246) / pfmergeCommand (L1307-1378)
pfselftestCommand (L1386-1486) / pfdebugCommand (L1495-1596, 四子命令)

外部接入 (grep 实证): 仅 commands.def L11056-11060 (5 命令) + acl.c:56 类别 — 自包含文件, 无内部 API 被其他模块调用
```

## 基本元素分解

1. 哈希分域: MurmurHash64A 固定 seed + 低 14 位索引 + 高 50 位前导零计数
2. 稠密编码: 6bit 打包 12288B + 16B 头, 展开直方图
3. 稀疏编码: ZERO/XZERO/VAL 三 opcode 游程 + 原地更新状态机 + 提升
4. 估计器: Ertl sigma/tau (arXiv:1702.01284), alpha_inf = 1/(2·ln2)
5. 基数缓存: card[8] LE + MSB 有效位, PFCOUNT O(1) 读
6. 命令面: PFADD/PFCOUNT(多键 RAW 归并)/PFMERGE/PFDEBUG/PFSELFTEST

## 标记问题 (20 问)

1. MurmurHash64A 与 R-3 SipHash 的差异 (固定 seed 无随机化)?
2. hllPatLen 的 count 为什么从 1 起? max 为什么是 51?
3. 6bit 打包的 LSB-first 位序与 R-11 MSB-first 对照?
4. HLL_DENSE_SIZE 12304 怎么来的?
5. 空 HLL 为什么只有 18 字节?
6. 稀疏三 opcode 的位域设计 (00/01/1 前缀)?
7. hllSparseSet 的 A/B/C/D 四分支?
8. XZERO 分裂为什么最坏 5 字节?
9. 稀疏→稠密提升的两触发条件?
10. hllCount 的 Ertl 估计器公式与经典 Σ2^-M 的差异?
11. alpha_inf 0.721347520444481703680 的来源?
12. reghisto 为什么 64 格?
13. 基数缓存的 MSB 有效位设计?
14. PFCOUNT 为什么 CMD_READONLY|CMD_MAY_REPLICATE (只读改值)?
15. 多键 PFCOUNT 的 16KB 栈数组 + HLL_RAW?
16. hll_sparse_max_bytes 默认 3000 的开关点?
17. PFSELFTEST 的 6×relerr 误差界?
18. PFMERGE 的 use_dense 决策?
19. 0.81% 误差 = 1.04/√16384 的来源?
20. HLL 为什么不做降级/删除/哈希随机化?

## 时空溯源 (代码内痕迹)

- 2014 (2.8.9): 初版 PFADD/PFCOUNT/PFMERGE (版权 2014-Present; commands.def 全标 2.8.9)
- 2017 (4.0): Ertl 估计器引入 (arXiv:1702.01284 = 2017-02; 代码注释 L959-979) — 版本推断
- 7.x: hll-sparse-max-bytes MODIFIABLE_CONFIG (config.c:3224) — 版本推断
- 演进: 稠密初版 → 稀疏 (2.8.9/3.0 间) → Ertl 估计器 (4.0) → 配置动态化

## 大域拆分判断

1597 行单文件 — **不拆** (🟡 B, 6 闭环足够, 命令面占 ~30%)
