# E-11 FieldData — 全视角提问验证 (completeness)

> 验证时机: 2 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生
> 🟡 B 域: 最少 30 问放宽至 20 问 (普通域 <10000 行按 30, 本域 B 方案按需)

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 加载 | loadDirect 和 load 区别? (缓存跳过?) | ⚠️ 01-L2 提了未展开 → 补一句 (loadDirect 绕过缓存) |
| 2 | 开发者 | global | OrdinalMap 怎么映射段内→全局? | ✅ 01-L3 (合并映射) |
| 3 | 开发者 | 断路器 | 超限抛什么? 请求会怎样? | ✅ 02-L2 (CircuitBreakingException 拒绝查询) |
| 4 | 架构师 | 设计 | 为什么 global ordinals 单独成层? | ✅ 01-L3 (跨段去重) |
| 5 | 架构师 | 历史 | 5.0 为什么迁移 docValues? | ✅ 02-L3 (OOM 事故) |
| 6 | 架构师 | 对照 | 服务端 vs 客户端缓存哲学? | ✅ 02-L4 (容量 vs 一致) |
| 7 | 性能工程师 | 内存 | 40% 限制怎么调? (overhead 含义?) | ✅ 02-L2 (1.03 开销系数) |
| 8 | 性能工程师 | 加载 | 长遍历的断路器检查频率? | ✅ 01-L3 (64K 次) |
| 9 | SRE | 断路器 | 线上 CircuitBreakingException 怎么处理? | ⚠️ 02 未提运维 → 补一句 (调 limit/降并发/查高基数) |
| 10 | SRE | 缓存 | 缓存什么时候清? | ✅ 01-L4 (clear) |
| 11 | 研究者 | 对照 | fielddata vs Redis 淘汰 (LRU)? | ✅ 02-L4 (断路器 vs LRU) |
| 12 | 研究者 | 演进 | 5.0 迁移的 commit 证据? | ✅ 02-L3 (7290b2dc916) |
| 13 | 子系统开发者 | 衔接 | terms agg 怎么消费 global? (E-12) | ✅ 01-L4 + 02-L5 |
| 14 | 子系统开发者 | 衔接 | 排序怎么消费? (E-2) | ✅ 01-L4 (comparator) |
| 15 | 学生 | 概念 | fielddata 是什么? | ✅ 01-L1 (docValues 堆缓存) |
| 16 | 学生 | 概念 | global ordinals 通俗解释? | ✅ 01-L5 (聚合加速索引) |
| 17 | 学生 | 历史 | 为什么叫"事故"? | ✅ 02-L3 (高基数 OOM) |
| 18 | 学生 | 断路器 | 40% 是什么的 40%? | ✅ 02-L2 (heap) |

**统计**: ✅ 15 / ⚠️ 3 / ❌ 0 — ⚠️ 全部"补一句"级
→ 回补 3 项: 01-L2 loadDirect 语义 / 02-L2 CircuitBreakingException 运维 / 02-L2 40% 是 heap

## 回补清单

1. 01-L2: 补 loadDirect 绕过缓存一句
2. 02-L2: 补 CircuitBreakingException 运维 (调 limit/降并发/查高基数字段)
3. 02-L2: 明确 40% 是 JVM heap 百分比
