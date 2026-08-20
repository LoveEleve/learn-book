# R-11 位操作 — popcount 与 BITFIELD

> 前置: [[R-24-string]] (字符串承载) + [[R-1-object]] (编码) + [[R-4-SDS]] (零填充扩容) | 引出: [[R-12-hyperloglog]] (近似计数对照) + [[R-17-client-caching]] (位图内存面) | 对照: [[R-12-hyperloglog]] (精确 vs 近似计数)
> 🟡 B | 6 KP | [模式: SWAR 批处理+字对齐跳过+MSB 位序+三溢出模式]
> Pass 2 闭环: q1(popcount) q2(bitpos) q3(bitfield) q4(setbit) q5(bitop) q6(bitcount)

**读者处境**: BITCOUNT 怎么数 512MB 里的 1?BITPOS 在稀疏位图怎么秒回?BITFIELD 的 WRAP 和 SAT 差在哪?SETBIT 第 0 位是字节的哪一位?这篇拆位操作: popcount 算法、bitpos 跳跃、BITFIELD 位宽整数、四命令面。

### 1. redisPopcount — 查表 + SWAR

场景: 512MB 位图数 1 怎么快?
源码路径:
- redisPopcount (bitops.c:19-71): **256 查表** (L23, bitsinbyte) + **SWAR 28 字节批** (L33-66)
- **SWAR 三阶段** (L45-65): 每 2 位求和 (0x55555555) → 每 4 位 (0x33333333) → 每 8 位乘法累加 (0x0F0F0F0F × 0x01010101)
- 对齐处理 (L26-29) + 尾字节查表 (L68-69)
- 上限: proto_max_bulk_len 512MB (L17)
关键设计 (q1): **查表兜底 + SWAR 主体** — 28 字节/轮 ≈ 4 ops/字节, 比逐位快 ~10× (推断)。[模式: SWAR 批处理]
数据流: 位图 → 对齐前缀查表 → SWAR 批 → 尾查表 → 位数。

### 2. redisBitpos — 字对齐跳过

场景: 稀疏位图找第一个 1 怎么秒回?
源码路径:
- redisBitpos (L80-165): **字对齐跳过** (L98-121): 找 0 跳过全 1 字, 找 1 跳过全 0 字 — O(段数)
- 尾字大端组装 (L130-138) + **MSB 扫描** (L151-159)
- **特殊值** (L140-145): 全 0 找 1 → -1 (L145); 找 0 → 不失败 (零填充假设, L76-78)
关键设计 (q2): **"跳过不感兴趣的连续段"** — 稀疏场景 O(段数) 非 O(位数)。[模式: 字对齐跳过]
数据流: 位图 → 逐字跳过 → 尾字 MSB 扫描 → 位置/-1。

### 3. BITFIELD — 位宽整数与溢出

场景: 怎么在任意偏移读写任意位宽的整数?
源码路径:
- 类型 (L429-459): iN/uN (≤64)
- 存取 (L188-265): set/getUnsigned/Signed — 掩码清置位 + 符号扩展
- **溢出三模式** (L267-360): **WRAP 截断低 bits 位** (L295-300) / **SAT 钳 max/0** (L281,288) / **FAIL 报错**
  - 64 位特判 (L268: UINT64_MAX; L305: INT64_MAX) 防移位 UB
- bitfieldGeneric (L1032): GET/SET/INCRBY 子命令 + overflow 前缀 (默认 WRAP)
关键设计 (q3): BITFIELD = 位图的**结构化访问**: 跨字节位宽整数 + 三溢出模式; 64 位特判是安全面。[模式: 位宽整数]
数据流: BITFIELD k SET u8 0 100 → 类型解析 → 掩码存取 → 溢出检查。

### 4. SETBIT/GETBIT — MSB 位序与 dirty

场景: SETBIT key 0 1 是设哪个位?
源码路径:
- 位定位 (L535-537): `byte = offset >> 3` / `bit = 7 - (offset & 7)` — **MSB 优先** (第 0 位 = 字节最高位)
- **dirty 三条件** (L540): 新键/扩容/值变化才写 — 写相同值零开销
- 扩容零填充 (lookupStringForBitCommand L460: 新键 sdsnewlen L473-474 / 已存在 dbUnshare L484 + **sdsgrowzero L486**)
- 返回旧值 (L555) — 读改写合一
- 位序统一: SETBIT/BITFIELD/BITPOS 全 MSB-first (L184-186)
关键设计 (q4): **MSB 位序统一约定** + dirty 短路 (值相同零成本)。[模式: 位定位]
数据流: SETBIT k 0 1 → byte=0, bit=7 → 置最高位 → 返回旧值。

### 5. BITOP — 四运算与 maxlen

场景: BITOP 的键长度不一致怎么算?
源码路径:
- 操作解析 (L598-605) + **NOT 单键限制** (L611-612)
- 多键读取 + **maxlen 语义** (maxlen L592 / minlen L594): 结果取最长, **短键零填充**
- **字宽批量运算** (L672+: AND/OR/XOR 每轮 4×sizeof(ulong) 位运算, lp 指针 +4 — 减少循环次数)
- 传播原样 (结果确定性)
关键设计 (q5): **maxlen + 零填充** = 长度无关的键按 0 参与; 字宽批量运算。[模式: 位运算]
数据流: BITOP AND dst a b → 读键 → maxlen 循环 → 写入 dst。

### 6. BITCOUNT/BITPOS — 范围与掩码

场景: 范围计数怎么免子串拷贝?
源码路径:
- BITCOUNT (L775-866): start/end + **BIT/BYTE 单位** (L783-786) + 负索引归一 (R-24 同款)
- **首尾掩码** (声明 L782): 边界字节按位掩码 — 只数范围内位; 整字节 redisPopcount (L852) + 首尾调整 (L860)
- BITPOS (L867-1031): 范围 → redisBitpos + 位偏移调整
关键设计 (q6): **范围 + 掩码** 免子串分配; BIT/BYTE 双单位。[模式: 范围计数]
数据流: BITCOUNT k 0 -1 BYTE → 负索引 → 掩码 → popcount。

### 负面空间 — 位操作刻意不做的事

- **不做位压缩**: 位图按字节存储 (无 RLE, 对照 R-12 HLL 稀疏)
- **不做跨键位操作**: BITOP 结果物化新键 (无视图)
- **不做 BITFIELD 位宽 64+**: 上限 64 位 (uint64/int64 承载)
- **不做位图自动紧凑**: 高位全 0 不自动截断 (惰性)
- **不做原子多字段**: BITFIELD 多子命令非事务 (MULTI 才原子)

→ 引出: 近似计数怎么做到 12KB 存百万?→ [[R-12-hyperloglog]]
