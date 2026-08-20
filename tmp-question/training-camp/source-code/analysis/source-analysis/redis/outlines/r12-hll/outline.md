# R-12 HyperLogLog — 概率基数估计

> 前置: [[R-33-zmalloc]] (内存面) + [[R-1-object]] (字符串承载) + [[R-24-string]] (命令面) + [[R-11-bitmap]] (精确 vs 近似) + [[R-21-db]] (键空间语义) | 引出: [[R-13-geo]] (空间编码) + [[R-17-client-caching]] | 对照: [[R-11-bitmap]] (精确 popcount vs 概率) + [[R-7-intset]] (升而不降) + [[R-3-dict]] (哈希 seed 策略)
> 🟡 B | 6 KP | [模式: 哈希分域 + 位打包 + 游程编码 + 统计估计 + 失效缓存 + 归并]
> Pass 2 闭环: q1(哈希分域) q2(稠密 6bit) q3(稀疏游程) q4(Ertl 估计器) q5(基数缓存) q6(命令面归并)

**读者处境**: 一个 1 亿独立访客的页面, 用 Set 存要 400MB, 用 HLL 只要 12KB? 误差 0.81% 是怎么保证的? PFADD 的 key 为什么能 GET 出来? 这篇拆概率基数估计: 哈希分域、6bit 打包、三 opcode 游程、Ertl 估计器、16B 头缓存、三命令面。

### 1. 哈希分域 — MurmurHash64A 定寄存器

场景: 一个元素怎么映射到 16384 个寄存器?
源码路径:
- MurmurHash64A (hyperloglog.c:376-426): 64bit MurmurHash2 变体 (m=0xc6a4a7935bd1e995, r=47); **字节序无关改造** (L372-374) — 跨架构确定性
- **固定 seed 0xadc83b19** (L446): 无随机化 — 对照 R-3 dict SipHash 随机 seed (跨实例确定性优先, 保证 PFMERGE/主从一致 — 动机推断标注)
- hllPatLen (L431-459): `index = hash & 0x3FFF` (低 14 位, L447) → `hash >>= 14` (L448) → `hash |= 1<<50` 保证终止 (L449-450) → 前导零计数
- count 语义 (L439-443): "000..1" 模式**含结尾 1** — 最小 1 (首 bit 即 1), 最大 Q+1=51 (L437); 64bit 哈希支撑基数 >10^9 (L18-20)
关键设计 (q1): **低 14 位定寄存器, 高 50 位定前导零长度** — 同一哈希值双用途; 确定性哈希 = HLL 跨实例语义前提。[模式: 哈希分域]
数据流: 元素 → MurmurHash64A(seed 固定) → index(低14位) + count(前导零+1, 1..51)。

### 2. 稠密编码 — 6bit 打包 12KB

场景: 16384 个 0-63 计数器怎么存得最省?
源码路径:
- 常量 (L173-181): HLL_P=14 / HLL_REGISTERS=16384 / HLL_BITS=6 / HLL_DENSE_SIZE = 16 + (16384×6+7)/8 = **12304**
- 打包宏 (L318-340): `_byte = regnum*6/8` + `_fb = regnum*6&7`; 读 `((b0>>fb)|(b1<<(8-fb)))&63`; **LSB-first 物理位序** (L76-77, L203-205)
- **越界免费**: 末寄存器读 b+1, sds 隐式 null 终结提供第 12305 字节 (L311-314) — 免条件分支
- hllDenseSet (L473-483): `count > oldcount` 才写 — 寄存器取 max
- hllDenseRegHisto (L499-554): **16 寄存器/轮 ×1024 轮全展开** (L509-546, r+=12), 无条件分支 (L194-197)
关键设计 (q2): 6bit 紧凑打包 + sds 尾零消灭越界分支; 直方图展开 = 计数的性能面。[模式: 位打包]
数据流: 寄存器 12288B → 宏读写 (count>old 才写) → 展开直方图。

### 3. 稀疏编码 — 三 opcode 游程

场景: 基数 100 时 12KB 太浪费 — 能不能按需分配?
源码路径:
- **三 opcode** (L344-367 + L86-108): ZERO `00xxxxxx` (1B, 1-64 个连续 0, L349) / XZERO `01xxxxxx yyyyyyyy` (2B, 1-16384, L350) / VAL `1vvvvvxx` (1B, 值 1-32 × 连续 1-4, L351-352)
- **空 HLL = 18B**: 16B 头 + XZERO:16384 (L110-111; createHLLObject L1105-1135, sparselen L1110-1112)
- 收益实测表 (L133-151): 100→267B / 1000→1882B / 10000→10591B — 对比稠密 12288B
- hllSparseSet 更新状态机 (L634-886): **A** 旧值够大→0 不动 (L733-737) / **B** VAL len1 直写 (L738-742) / **C** ZERO len1 替换 (L747-750) / **D** 通用分裂 seq[5] (L767-811: 最坏 XZERO-VAL-XZERO=5B, L758-761) + Step3 memmove+sdsIncrLen (L824-826) + **Step4 相邻 VAL 合并** (L835-865, scanlen=5)
- 预留扩容 (L653-659): avail<3 → greedy `+min(newlen,300)` 封顶 max_bytes
- **升稠不降稀**: 两触发 — count>32 (L642) / 超 hll_sparse_max_bytes=3000 (L822, config.c:3224 MODIFIABLE); hllSparseToDense 校验 idx==16384 (L608)
关键设计 (q3): **纯位置游程编码** (无显式索引, 更新=分裂+合并保持最简); 单向提升, 稠密永不降回 — 对照 R-7 intset 升而不降。[模式: 游程编码+状态机]
数据流: PFADD → 顺序定位 opcode (条件按频率排序 L675-677) → 分裂/合并 → 超限 promote 稠密。

### 4. hllCount — Ertl 估计器 (认知修正)

场景: 直方图怎么变成近似基数?
源码路径:
- reghisto[64] (L1013, count max 51 的安全格); 三编码直方图分派 (L1016-1025; RAW 8B/轮全零字跳过 L936-957)
- hllSigma (L962-974) / hllTau (L979-991): **Ertl 改进估计器** (arXiv:1702.01284)
- 主公式 (L1030-1038): `z = m·tau((m-reghisto[51])/m) + Σ(递推折叠) + m·sigma(reghisto[0]/m)`; `E = llroundl(alpha_inf·m²/z)`
- HLL_ALPHA_INF = 0.721347520444481703680 (L368) = **1/(2·ln2)** — 经典论文中 alpha 按 m 分级 (m≥128 取渐近值, 16384 恒适用 — 推断标注)
- 标准误差: relerr = 1.04/√16384 = **0.8125%** (L1432, 1.04 为 Flajolet 经验常数 — 推断标注)
关键设计 (q4): **⚠ 非经典 Σ2^-M 调和平均** — Redis 4.0 起换 Ertl (2017) 估计器, 保留 alpha_inf·m²/z 外壳; invalid 检测 = sparse idx≠16384 (L925)。[模式: 统计估计]
数据流: 直方图 → tau/sigma 修正 → alpha_inf·m²/z → llroundl。

### 5. 16B 头与基数缓存 — 只读改值

场景: PFCOUNT 为什么 O(1)?
源码路径:
- hllhdr (L161-167): magic[4]="HYLL" + encoding + notused[3] + card[8] (LE); **card[7] MSB=1 失效** (L170-171)
- PFCOUNT 单键 (L1259-1304): 缓存有效 → 8B 组装直返零计算 (L1270-1279); 失效 → hllCount 重算 + **回写缓存** (L1283-1295) + signalModifiedKey + dirty++ (L1299-1300)
- **只读命令改值**: CMD_READONLY|CMD_MAY_REPLICATE — **commands.def 唯一该组合** (grep 实证: 6 个 MAY_REPLICATE 中仅 pfcount, 其余 publish/spublish/eval/evalsha/fcall); 因改值需 dbUnshareStringValue (L1266); lookupKeyRead 非 Write 的从库语义 (L1254-1258)
- 校验分层: isHLLObjectOrReply 头浅检 (magic/encoding≤1/dense 长度精确 12304, L1140-1168) + 使用期深检 (hllSparseRegHisto invalid)
关键设计 (q5): **缓存命中 = 纯读**; 计算是读, 缓存回写是"只读命令"的传播副作用 (R-28 面唯一案例)。[模式: 失效缓存]
数据流: PFCOUNT → valid? → 直返 : 重算+回写+传播。

### 6. 命令面 — PFADD/PFCOUNT 多键/PFMERGE

场景: 三个命令怎么协作? 多键合并怎么算?
源码路径:
- PFADD (L1171-1208): 缺键 **createHLLObject 稀疏起步** (L1180-1182, **createObject(OBJ_STRING) = RAW 编码承载** L1130 — 无独立类型/编码, magic 头辨识); 逐元素 hllAdd (L1188-1199); 更新→失活+notify pfadd+dirty+=updated (L1201-1206); 回复 :1/:0; **CMD_DENYOOM** (maxmemory 满被拒 — R-23 交叉)
- PFCOUNT 多键 (L1220-1246): **栈上 16400B max 数组** (L1221) + HLL_RAW 内部编码 (L1227) + hllMerge 逐寄存器 MAX (L1059-1099)
- PFMERGE (L1307-1378): 任一输入稠密 → use_dense 目标 (L1326, L1353); 稀疏写回走 hllSparseSet 可能中途 promote (L1365-1369); notify pfadd (L1375)
- hll_sparse_max_bytes 默认 3000 (config.c:3224); PFSELFTEST (L1386-1486): 10M 元素双编码一致性 (L1452) + 6σ 误差界 (L1460, j==10 特判 L1466)
关键设计 (q6): 多键 = **栈上 RAW 归并** (MAX 可交换, 缺键当空); 自检 = 双编码对照 + 误差界。[模式: 归并]
数据流: PFMERGE dest a b → max[] MAX 归并 → use_dense? → 写回 dest 寄存器。

### 负面空间 — HLL 刻意不做的事

- **不做精确计数**: 0.81% 近似 (对照 R-11 精确 popcount — 精度换内存的经典权衡)
- **不做稠密→稀疏降级**: 升稠不降稀单向 (对照 R-27 set 双向转换)
- **不做哈希随机化**: 固定 seed — 跨实例确定性优先于 HashDoS 防护 (对照 R-3 SipHash)
- **不做删除/减法**: 只增不改, 多键合并无差值
- **不做寄存器扩容**: P=14 固定, 精度不可配置 (对照 [1] 论文压缩变体)
- **不做独立类型**: 复用 OBJ_STRING + magic 头 (对照 R-1 编码体系)

→ 引出: 怎么把经纬度塞进 ZSet 的 score 做空间搜索? → [[R-13-geo]]
