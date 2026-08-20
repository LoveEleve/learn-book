# RD-5 RMap 分布式映射 — 深审 REVIEW 记录 (六层深审 + 07 五维度 + 二次 REVIEW)

## 二次 REVIEW (07 换维度: 跨域/事实/锚点/完整/负面)

| 轮 | 维度 | 发现 | 修复 |
|:--:|:--|:--|:--|
| R1 | 跨域引用核验 | h13/m7/r21/r22/rd3/rd4 全存在; rd6 前向合法 ✅ | 通过 |
| R2 | **事实核验 (五路清理)** | MapCacheEvictionTask 清理 = **zrem×3 辅助 zset + hdel 主 hash 五路** — 大纲/pass2 只写 hdel 漏 zrem | 篇2-S4 + pass2-q6 补五路删除 (L90-93) |
| R3 | **锚点密度 (第五次复发)** | 裸行号 18 处 (01=5/02=13); file:line 02=3 低于 🟡B ≥4 | **全补 → 01=11/02=18** ✅ (五连复发 — 根治铁律升级) |
| R4 | **完整性对照** | REDISSON-PLAN "Map 命令面" 主题大纲缺失 — 只讲双写未讲命令映射 | 篇1 新增 "4. 命令面" 节 (HSET/HGET/HMSET/HDEL...) |
| R5 | 负面空间 | 含对比 ✅ | 通过 |

> ⚠️ 铁律升级 (五连复发): 裸行号在 RD-1/3/4/2/5 每次都犯。**"写时自查"承诺不可靠 — 必须 REVIEW 强制 sed 扫描**: 每次域完成后跑 `grep -nE '\(L[0-9]+' *.md | grep -v java:` 全量扫裸行号, 作为完成检查单固定项。

## 六层深审 (Pass 0-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **重点机制 (mapWriterFuture 三路)** | 写操作按 WriteMode 三选一: BEHIND 缓冲 / THROUGH 同步 / THROUGH 异步; condition 门控 | 篇1-S1 |
| 2 | **五结构索引 (RMapCache)** | 主 hash + TTL/idle/LRU zset + options hash; 惰性检查 | 篇2-S1/2 |
| 3 | **自适应清理 (EvictionTask)** | sizeHistory 三态调 delay (5s~2h) | 篇2-S3 |
| 4 | Retryable 重试 | writerRetryAttempts 默认 0, interval 100ms | 篇1 负面空间 |
| 5 | completeness ⚠️ 回填 | Q15/Q32/Q35/Q16 | 两篇负面空间 |
| 6 | 通过项 | 全锚点行号 awk 验证 ✅ | 记录 |

## 07 五维度

### 维度1 功能正确性
- 写三路 + condition 门控实证; 五结构惰性检查; 五路清理 (zrem×3+hdel)

### 维度2 性能
- WriteBehind 50/1000 批量合并; zrangebyscore+hdel 一脚本; 惰性 zscore 开销

### 维度3 内存
- 五结构 = 主 hash + 4 辅助; TTL/idle zset 随条目增长

### 维度4 一致性/并发
- 双写无事务; 写后窗口滞后; 并发 miss 无去重; 每集合独立 EvictionTask

### 维度5 边界/安全
- Retryable 默认关; condition 门控; 自适应 delay 界 5s~2h

## 完成状态

- [x] Pass 0-3 (方案 B): 6 闭环 + 知识规划 + 2 篇大纲 + 50 问
- [x] **二次 REVIEW (R1-R5) 修复 3 项** (五路清理/锚点 18 处/命令面)
- [x] 铁律升级: 裸行号五连复发 → 域完成必跑 sed 全量扫描