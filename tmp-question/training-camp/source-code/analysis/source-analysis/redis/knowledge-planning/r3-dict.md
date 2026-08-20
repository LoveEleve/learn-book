# R-3 Dict — 知识规划 (knowledge-planning)

> 项目: Redis 7.4.2 | 🔴 A / 1 篇 (+harness) | dict.c (2056)+dict.h (264)+siphash.c
> 基线: REDIS-PLAN R-3 — 前置: **R-33 (zmalloc) + R-4 (sds 键)** — 展开 双表渐进→触发阈值→联动迁移→掩码优化→单指针 entry→扫描算法→定制族→安全哈希

---

## §0.8

- 🔴 A，1篇 — 双表渐进(**ht_table[2]+rehashidx 游标 dict.h:96-102; dictRehash n 步+empty_visits=n*10 空桶上限 L385-414; 完成检测: 旧表释放+新表提升+rehashidx=-1 L362-374**) → 触发阈值(**1:1 扩 L1492-1515 / <1:8 缩 L1533-1550 / AVOID: 4:1 扩+1:32 缩 (dict_force_resize_ratio=4 × HASHTABLE_MIN_FILL=8); COW 三态: fork→FORBID / 子进程→AVOID / 正常 ENABLE (server.c:640-652 updateDictResizePolicy)**) → 联动迁移(**dictFind: 目标桶未迁且非空→_dictBucketRehash (缓存友好 L747-757), 否则游标步进; 双表查找 L758-770**) → 掩码优化(**2 幂表: 扩容重算 hash / 缩容直接 idx&掩码 (低 j 位不变, L320-327)**) → 单指针 entry(**低 3 位编码: key 直存 (entryIsKey &1) / normal / no-value; keys_are_odd: sdshdr 恒奇 (3/5/9/17B) + malloc 对齐 → sds 指针恒奇 (server.c:475); set dict no_value**) → 扫描(**dictScan 反向游标 rev+1+rev: rehash 期间不丢 (重复/中途新增可漏), 大表展开区 L1369-1470**) → 定制族(**dictType 函数指针: hash/cmp/dup/free + 7.x: no_value/keys_are_odd/storedKey API/rehashingCompleted/onDictRelease/metadata; 联合值 v{val,s64,u64,double} 整数免分配**) → 安全哈希(**SipHash+16B 随机 seed (server.c:6985-6987 getRandomBytes; dict.c:92-113); 2014 HashDoS 后引入; siphash_nocase 命令表用**)
- 设计模式: [模式: 双表渐进迁移+阈值双轨+访问感知调度+指针编码压缩+反转游标]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| dict.h:96-102; dict.c:385-414 | 双表渐进 | rehashidx 逐桶迁移; 空桶 n*10 上限; 完成提升 | High |
| dict.c:1492-1550; server.c:640-652 | 阈值 | 1:1/1:8 (ENABLE) vs 4:1/1:32 (AVOID); COW 三态 | High |
| dict.c:736-770,472-489 | 联动 | 查找触发桶迁移 (缓存友好) | High |
| dict.c:320-327 | 掩码 | 缩容 idx&掩码免重算哈希 | High |
| dict.c:128-171; server.c:474-475 | 单指针 | 低 3 位编码三态; sds 恒奇机制 (奇数头) | High |
| dict.c:1369-1470 | 扫描 | 反向游标 rev+1+rev 不丢 (重复/中途新增可漏) | High |
| dict.h:32-92; dict.c:850+ | 定制族 | dictType 注入; 整数联合值 | High |
| dict.c:92-113; server.c:6985-6987 | SipHash | 随机 seed; HashDoS 防护; nocase 变体 | High |

---

## 02-04 聚合+分类+聚类 (1篇+harness)

**1篇理由**: dict 是单机制闭环 (迁移→阈值→联动→优化), 1篇 (~80行) 按"双表渐进→阈值→联动→掩码→单指针→扫描→定制→安全"展开; harness 验证渐进迁移/阈值/掩码缩容/扫描。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 双表渐进 rehash 状态机 | 🔴 | **为什么🔴**: 核心机制 |
| P1-2 | 触发阈值 + COW 三态 | 🔴 | **为什么🔴**: 扩容策略 |
| P1-3 | 掩码优化 (缩容免重算) | 🔴 | **为什么🔴**: 2 幂数学 |
| P1-4 | 单指针 entry 优化 | 🔴 | **为什么🔴**: 7.x 内存压缩 |
| P2-1 | 查找联动迁移 | 🟡 | **为什么🟡**: 调度细节 |
| P2-2 | dictScan 反向游标 | 🟡 | **为什么🟡**: 遍历算法 |
| P2-3 | dictType 定制族 | 🟡 | **为什么🟡**: 接口面 |
| P3-1 | SipHash 安全 | 🟢 | **为什么🟢**: 安全权衡 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **渐进迁移** | 🔴 | 核心 |
| B | **阈值与掩码** | 🔴 | 策略/数学 |
| C | **编码与扫描** | 🟡 | 优化/算法 |
| D | **接口与安全** | 🟡 | 扩展/权衡 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 双表渐进 | ht[0]→ht[1] 逐桶迁移 (rehashidx 游标), 空桶 n*10 上限防阻塞, 完成时旧表释放/新表提升; 迁移期间读写双表兼容; **pauserehash 冻结面**: 两阶段删除 (L815,833)/安全迭代器 (L1034) 暂停迁移, 非安全迭代器 fingerprint 检测 | dict.c:312-414,815,833,1034; dict.h:96-102 |
| q2 | 阈值+COW | 1:1 扩/1:8 缩 (ENABLE) vs 4:1 扩/1:32 缩 (AVOID); fork→FORBID 全冻结 — 全为 COW 页保护 | dict.c:1492-1550; server.c:640-652 |
| q3 | 联动迁移 | 查找时目标桶未迁且非空 → 直接迁移 (缓存友好); 否则游标步进 — 热门桶自适应先迁 | dict.c:736-770 |
| q4 | 掩码优化 | 2 幂表: 缩容 idx&新掩码 (低 j 位不变) — 免重算哈希, 迁移 O(1)/元素 | dict.c:320-327 |
| q5 | 单指针 | 低 3 位编码 key/normal/no-value 三态; **桶空才直存 key** (无 next), 非空退回 no-value entry; sds 指针恒奇 (奇数头 3/5/9/17B + malloc 对齐) | dict.c:128-171,328-349; server.c:474-475 |
| q6 | 反向游标 | dictScan rev+1+rev: rehash 期间遍历不丢 (重复/中途新增可漏), 大表展开区全访问 | dict.c:1369-1470 |
| q7 | 定制族 | dictType 函数指针注入 (sds 键/robj 值/命令表 nocase); 联合值整数免分配 | dict.h:32-92; dict.c:850+ |
| q8 | SipHash | 16B 随机 seed 密钥化哈希 — 2014 HashDoS 后引入, 攻击者无法构造碰撞 | dict.c:92-113; server.c:6985-6987 |

→ 引出 R-19: listpack 是 dict 之外的另一条存储路线 (紧凑数组 vs 哈希链) → [[R-19-listpack]]
