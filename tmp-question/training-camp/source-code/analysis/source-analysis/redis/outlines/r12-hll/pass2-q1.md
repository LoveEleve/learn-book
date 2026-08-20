# 闭环笔记 q1: MurmurHash64A + hllPatLen — 哈希分域

## 假设
64bit 哈希 + 低 14 位索引 + 高 50 位前导零计数; seed 固定。

## 验证过程
- MurmurHash64A (hyperloglog.c:376-426): 标准 MurmurHash2 64bit 变体, 常量 m=0xc6a4a7935bd1e995, r=47; 每 8B 一轮 + 尾字节 switch; **字节序无关改造** (L372-374 注释 "endian neutral" — 大端逐字节组装 L393-400)
- 调用点 hllPatLen (L431-459): **固定 seed 0xadc83b19** (L446) — 无随机化
- `index = hash & HLL_P_MASK` (L447, mask=0x3FFF 低 14 位) → `hash >>= HLL_P` (L448) → `hash |= 1<<HLL_Q` (L449-450, 保证循环终止) → 逐位找 0
- count 语义 (L439-443 注释): "000..1" 模式**含结尾 1**, 所以最小 count=1 (无前导零, 首 bit 即 1); max = Q+1 = 51 (L437)
- 设计动机 (L18-20 注释): 64bit 哈希支撑基数 >10^9, 代价仅每寄存器 1bit

## 代码类型
Mechanism (哈希分域)

## 跨域关联
- R-3 dict SipHash: **对照 — SipHash 16B 随机 seed (防 HashDoS), HLL 固定 seed 0xadc83b19 (跨实例确定, PFMERGE 一致性优先)** — 安全面权衡记录
- R-11: 对照 精确计数 (popcount) vs 概率计数 (前导零)

## 结论
哈希 = 固定 seed MurmurHash64A; 低 14 位定寄存器, 高 50 位定 count (1..51)。确定性哈希是 HLL 的跨实例语义前提 (主从/合并一致)。
源码位置: hyperloglog.c:376-459
