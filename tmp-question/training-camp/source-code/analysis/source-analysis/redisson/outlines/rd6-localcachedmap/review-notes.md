# RD-6 RLocalCachedMap 本地缓存 — 深审 REVIEW 记录 (六层深审 + 07 五维度 + 二次 REVIEW)

## 二次 REVIEW (07 换维度: 反写/事实/锚点/跨域/完整)

| 轮 | 维度 | 发现 | 修复 |
|:--:|:--|:--|:--|
| R1 | 反写覆盖 | 7 闭环机制全落入 3 篇大纲 ✅ | 通过 |
| R2 | **事实核验 (默认值/窗口)** | ① SyncStrategy 默认 INVALIDATE (L281) ② ReconnectionStrategy 默认 NONE (L267) ③ **LOAD 断线窗口 = 10 分钟** (cacheUpdateLogTime RedissonLocalCachedMap:55, 文档 L265-267) | 篇2/篇3 补默认值 + 10 分钟窗口 |
| R3 | **锚点密度 (第六次复发)** | 裸行号 14 处; file:line 01=4 低于 🔴≥8 | **全补 → 01=21/02=8/03=12** ✅ |
| R4 | 跨域补链 | 并行 AI 新产出 **r29-pubsub** (发布订阅) — RD-6 广播正好对应 | 篇2 header + 正文补 [[r29-pubsub]] |
| R5 | 完整性 | EvictionPolicy 枚举名未显式 (CacheProvider 节) | 篇1 补枚举 (NONE/LRU/LFU/SOFT/WEAK) |

> ⚠️ 铁律六连复发记录: 裸行号 RD-1/3/4/2/5/6。已列入完成检查单强制项 (grep 扫描)。

## 六层深审 (Pass 0-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **重点机制 (双层级读)** | getAsync 本地 hit / miss 按 StoreMode 回填 | 篇1-S2/3 |
| 2 | **SyncStrategy 双消息** | INVALIDATE hash vs UPDATE 全值 | 篇2-S1 |
| 3 | **excludedId 防循环** | 广播带 instanceId 排除发送者 | 篇2-S2 |
| 4 | **LOAD 增量补漏 (重大修正)** | 初稿"全量重载" → 实测增量 (更新日志 zset) | 篇3/pass2-q4/knowledge 三处 |
| 5 | 消息族 | Disable/Enable/Clear/DisableAck/DisabledKey | 篇3-S2 |
| 6 | CacheProvider | Caffeine vs 自研 LRU/LFU/Soft/Weak | 篇1-S4 |
| 7 | harness | MiniLocalCachedMap 12/12 PASS | harness |
| 8 | completeness ⚠️ 回填 | Q11/Q28/Q40 | 负面空间 |
| 9 | 通过项 | 全锚点 awk 验证 ✅ | 记录 |

## 07 五维度

### 维度1 功能正确性
- 双层级读 harness 12/12; SyncStrategy 两档; LOAD 增量补漏

### 维度2 性能
- 本地 hit 零网络; UPDATE 零回源; LOAD 增量高效

### 维度3 内存
- LocalCacheView 每 JVM 独立; CacheProvider 决定实现

### 维度4 一致性/并发
- 三通道一致; excludedId 防自处理; 最终一致窗口

### 维度5 边界/安全
- 重连 CLEAR/LOAD; Disable/Enable 运维; 断线 10min 窗口

## 完成状态

- [x] Pass 0-3 (🔴 A): 7 闭环 + 3 篇 + harness 12/12 + 50 问
- [x] **二次 REVIEW (R1-R5) 修复 4 项** (默认值/10min 窗口/锚点 14 处/r29 补链)
- [x] LOAD "全量"→"增量补漏" 重大修正
- [x] 铁律六连复发记录