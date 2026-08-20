# R-7 intset — 整数集合: 有序数组 + 值域升级, 一档到底

> 前置: [[R-33-zmalloc]] (分配) | 对照: [[R-19-listpack]] (同族紧凑编码) + [[R-3-Dict]] (转换目标) | 引出: [[R-6-zset]] (下一层结构)
> 🟡 B | 6 KP | [模式: 编码分级+单向升级+有序数组二分]
> Pass 2 闭环: q1(升级) q2(编码) q3(二分) q4(升而不降) q5(校验) q6(消费)

**读者处境**: SADD 一堆整数, 它存在哪?为什么 100 万个 2 位数组成的 set 能省 75% 内存?为什么加了一个超大整数后, 整个 set 突然"变胖"了?为什么删掉它也不会瘦回来?这篇拆 intset: 2/4/8 字节三档编码、升级机制、以及"升而不降"的单向哲学。

### 1. 结构与编码 — 2/4/8 三档字节宽

场景: 小整数 set 为什么省内存?encoding 字段存什么?
源码路径:
- `intset.h:35-39` — `uint32_t encoding + uint32_t length + int8_t contents[]` — 柔性数组
- `intset.c:41-43` — **encoding = 元素字节宽**: INTSET_ENC_INT16=2 / INT32=4 / INT64=8
- `intset.c:46-53` — 值域判定: ±32767→16 档, ±2^31→32 档, 其他→64 档
- `intset.c:56-95` — 读写: **memcpy + memrevXXifbe** (统一小端存储; 大端平台转换)
关键设计: 编码分级 (q2): 集合内所有元素同一宽度 (有序数组的代价) — 全 int16 的 100 万整数集只要 2MB vs int64 的 8MB; 字节序统一让 RDB/跨平台可移植, memcpy 免未对齐访问。[模式: 分级编码 + 统一字节序]
数据流: intsetNew → encoding=16 → 元素按宽度存取。

### 2. 升级机制 — 值域超界时的"头尾特权"

场景: 加一个 2^40 的数, 整个 set 怎么"变胖"?
源码路径:
- `intset.c:214-216` (intsetAdd) — `valenc > encoding → intsetUpgradeAndAdd` — 注释: 新值 "should be either appended (if > 0) or prepended (if < 0), because it lies outside the range of existing values"
- `intset.c:159-182` — 升级:
  - L163: `prepend = value < 0` — **负数头插/正数尾插** (极值特权, 免二分)
  - L166-167: 改 encoding + resize (+1 元素)
  - L172-173: **从后往前搬**: `while(length--) _intsetSet(is, length+prepend, _intsetGetEncoded(is,length,curenc))` — 新宽度下从前往后会覆盖未读旧值
关键设计: 单调升级 (q1): 值域只升不降, 新值必在极值 → O(n) 搬移 + O(1) 定位; 从后往前搬是"防覆盖"的关键 (新宽度逐元素膨胀)。[模式: 极值特权 + 反向搬移]
数据流: SADD 2^40 → 值域超界 → 升级 32→64 → 从后往前搬 → 尾部插入。

### 3. 二分查找 — O(log n) 查询面

场景: SISMEMBER 怎么在有序数组里找?为什么两端查找更快?
源码路径:
- `intset.c:117-156` (intsetSearch) — 标准二分: `mid = (min+max)>>1` (无符号防溢出)
- `intset.c:128-134` — **首尾快速路径**: 值 > max → pos=length (尾插位); 值 < min → pos=0 (头插位) — 极值查找/插入免二分
- `intset.c:184-203` (intsetMoveTail) — 插入/删除的 memmove (按宽度)
关键设计: 有序二分 (q3): O(log n) 查找 (512 元素 ≤9 次比较) vs listpack 的 O(n) 线性 — intset 的查询优势; 首尾快速路径服务升级场景的连续极值插入。[模式: 二分 + 边界捷径]
数据流: SISMEMBER 5 → 编码检查 → 二分 → 命中/未命中。

### 4. 升而不降 — 删掉大数也不瘦身

场景: 删掉那个 2^40 后, set 为什么还是 8B/元素?
源码路径:
- `intset.c:236-253` (intsetRemove) — 二分 → MoveTail 覆盖 → `intsetResize(len-1)` — **只缩元素数, 无任何降级逻辑**
- 降级成本 (机制推断: 代码无降级路径是事实, 成本分析为合理推断): 需全量验证剩余值在新值域内 (O(n)) + 删除后可能又插大值 (回弹乒乓) — 收益 < 成本
- 同哲学: listpack→dict / 小 hash→dict 全单向 — **小→大便宜且少见, 大→小昂贵且可能回弹**
关键设计: 单向升级 (q4): 显式不做降级 — 一次性大值后的空间浪费是接受的代价 (业务可重建恢复); 与 Redis 全部编码转换同哲学。[模式: 单向转换]
数据流: SREM 2^40 → 移尾覆盖 → 缩 resize → encoding 不变 (64)。

### 5. 字节序与完整性 — RDB 防御

场景: RDB 加载时怎么知道 intset 没坏?
源码路径:
- `intset.c:302-343` (intsetValidateIntegrity) — encoding 必须 2/4/8 / **sizeof+count×宽度 == size 精确一致** (防伪造 length) / 非空; deep: **逐元素严格递增无重复**
关键设计: 双级校验 (q5): 浅层结构 + deep 语义 (有序无重复) — RDB 加载/DEBUG RELOAD 的畸形防御; 与 listpack 的 lpValidateIntegrity 同族。[模式: 双级完整性]
数据流: RDB 加载 → intsetValidateIntegrity → 校验通过才可用。

### 6. 消费场景 — set 的 512 阈值

场景: SADD 什么时候用 intset?什么时候转 dict?
源码路径:
- `config.c:3216` — **set-max-intset-entries = 512** (CONFIG SET 可调)
- `t_set.c:26` — 双条件: `isSdsRepresentableAsLongLong(value)` (整数性) **&&** `size_hint <= 512` (规模)
- `t_set.c:42` — intset 超阈值 → **单向转 dict** (hashtable 编码)
关键设计: 阈值转换 (q6): 整数性 + 规模双条件入 intset; 超限转 dict — 与 hash 的 listpack 策略同构 (512/128 阈值家族)。[模式: 双条件 + 阈值转换]
数据流: SADD 1000 个整数 → 超 512 → 转 dict (哈希链); ≤512 → intset (有序数组)。

### 负面空间 — intset 刻意不做的事

- **不做降级**: 升而不降 (q4 详述)
- **不做字符串**: 只存整数 (非整数 → dict)
- **不做 O(1) 插入**: 有序数组 memmove O(n) (小规模专用)
- **不做压缩**: 无编码内压缩 (对照 quicklist LZF)
- **不做删除缩容优化**: Remove 只缩元素数

→ 引出: ZSet 是下一层数据结构 — skiplist + dict 双结构 → [[R-6-zset]]
