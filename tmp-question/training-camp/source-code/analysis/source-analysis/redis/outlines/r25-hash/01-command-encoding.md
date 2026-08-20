# R-25 上 — hash 命令与编码: listpack↔dict 三态

> 前置: [[R-19-listpack]] (编码) + [[R-3-Dict]] (dict/storedKey) + [[R-21-db]] (键空间) + [[R-24-string]] (命令层样板) | 引出: [[R-25-下]] (HFE 字段过期) | 对照: [[R-7-intset]] (单向升级同哲学)
> 🔴 A | 4 KP | [模式: 编码三态+单向升级+预扩迁移+迭代器遍历]
> Pass 2 闭环: q1(编码三态) q2(hashTypeSet) q3(转换) q4(命令面)

**读者处境**: HSET 一百万个字段会怎样?hash 的三种编码什么时候切换?为什么说"升级容易降级难"?HGETALL 怎么知道该返回 map 还是 array?这篇拆 hash 命令与编码: 三态选择、写入路径、转换迁移、命令面。

### 1. 编码三态 — 规模分级

场景: hash 内存怎么随规模变化?
源码路径:
- **三编码** (t_hash.c:74-130): LISTPACK (两元素组 field/value) / **LISTPACK_EX** (三元素组 + TTL, 7.4 HFE) / HT (hfield 键)
- 创建 (object.c:249): createHashObject 空 listpack 起步
- **阈值** (config.c:3215,3221): hash-max-listpack-entries 默认 512 / hash-max-listpack-value 默认 64 (旧名 ziplist 兼容)
- 两个 dictType: mstrHashDictType (L74) / **mstrHashDictTypeWithHFE** (L86, dictMetadata 扩展)
- hashTypeLookupWriteOrCreate (L1541): 不存在 → 创建 + dbAdd
关键设计 (q1): **规模分级**: 小 listpack (零指针), 中带 TTL listpack-EX, 大 dict; 升级单向 (R-7 intset 同哲学)。[模式: 编码三态]
数据流: HSET → 创建 listpack → 增长 → 超 512 → HT。

### 2. hashTypeSet — 三编码统一写入

场景: 一条 HSET 怎么落到三种编码?
源码路径:
- hashTypeSet (L855-977): **前置转换** (L861-865, 超 64 值转 HT)
- **LISTPACK**: lpFind → lpReplace (L880) / lpAppend ×2 (L887-888); 超 512 转 HT (L893-894)
- **LISTPACK_EX**: 三元素组; KEEP_TTL 保留旧 TTL (L918-919) / 否则清 TTL (L920-922); 新字段 listpackExAddNew (L928)
- **HT**: hfieldNew + dictUseStoredKeyApi (L941, R-3) + dictAddRaw; 已存在: KEEP_TTL 保留 hfield (L947-949) / 否则 hfieldPersist + 换键 (L951-956)
- TAKE_VALUE (L962-967): sds 零拷贝接管
关键设计 (q2): **三编码三分支 + 标志驱动** (KEEP_TTL/TAKE_VALUE) — 调用方只传标志不关心编码。[模式: 编码内写入]
数据流: HSET k f v → TryConversion → hashTypeSet → 编码分支 → 阈值复查。

### 3. 转换 — 单向升级与预扩

场景: 512 字段临界怎么转换?TTL 字段怎么迁移?
源码路径:
- **三触发** (hashTypeTryConversion L594-623): 批量字段数超 512 (L605-609, **dictExpand 预扩** L607) / 单值超 64 (L615-617) / **lpSafeToAdd 总量** (L621, R-19 1GB 安全线)
- Listpack→HT (L1553-1609): dictExpand 预扩 (L1585) + 迭代迁移 + 重复字段 panic (L1594-1600, 腐坏防御)
- Listpack→ListpackEX (L1559-1575): **每对后插 HASH_LP_NO_TTL** (L1568) — 两元素组扩三元素组
- **ListpackEx→HT** (L1611-1666): 全局摘除 (L1624-1625) → dictExpireMetadata 填充 (trash=1 L1634) → 私有 hfe 重建 (L1652-1653) → 全局重注 (L1661-1662)
- 无降级 (L1675: HT→X panic)
关键设计 (q3): 转换 = **单向 + 预扩免 rehash** + HFE 三阶段迁移 (摘/建/注)。[模式: 预扩迁移]
数据流: 超阈值 → dictExpand → 迭代迁移 → 元数据随迁 → 全局 HFE 重注。

### 4. 命令面 — 迭代与回复

场景: HGETALL 怎么返回?HRANDFIELD 怎么随机?
源码路径:
- hsetCommand (L2158): **HSET/HMSET 回复差异** (L2173-2178, 兼容历史)
- hincrbyCommand (L2187): hashTypeGetValue (HFE_LAZY_EXPIRE) + 溢出检查 (L2211, R-24 同款) + TAKE_VALUE|KEEP_TTL
- **hgetallCommand** (L2445): 先算 length (减过期字段, L2460) → **skipExpiredFields 优化** (L2462-2464: 全局无最小到期 → 迭代免逐字段查) → map/array 按 flags → **断言 count==length** (L2483)
- hrandfield (L2547): COUNT 正负语义 (L2556-2561) + 小 hash listpack 直接采样 (L2520, CASE 2.5 L2657) / 大 hash 加权 (hashTypeRandomElement L1789, dictGetFairRandomKey R-3)
- hscanCommand (L2509): scanGenericCommand 委托 (R-21)
关键设计 (q4): 命令面 = 查找 → 迭代 → 回复; skipExpiredFields 是**批量免查优化**。[模式: 迭代器遍历]
数据流: HGETALL → length → 迭代 (skip 优化) → map 回复 → 断言校验。

### 负面空间 — hash 命令刻意不做的事

- **不做编码降级**: HT 永不回 listpack (单向, R-7 同哲学)
- **不做字段级排序**: 迭代顺序 = 编码内部顺序 (listpack 插入序/dict 哈希序)
- **不做 HGETALL 分页**: 大 hash 用 HSCAN 显式遍历
- **不做 HRANDFIELD 保真**: COUNT 正负随机语义不同 (可重复 vs 不重复)
- **不做批量原子**: HSET 多字段非事务 (MULTI 才原子)

→ 引出: 字段怎么过期?→ [[R-25-下]]
