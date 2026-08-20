# SCC-12 LoadBalancer 扩展策略 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 7 | **语义重大** | Weighted 核心不是"展开/排序": **LazyWeightedServiceInstanceList 是 GCD 归一化 + 懒展开** (L46-51: weights 求最大公约数 + total/GCD 展开数组防大权重爆炸; L57-66: get(index) 才 selector.next() 逐步展开) — 大纲原"按权重展开/排序"失准 | 已修 §1 |
| 8 | 语义补强 | 权重 **≤ 0 → DEFAULT_WEIGHT** (L91-95) + **异常 → DEFAULT_WEIGHT** (L99-103) — 双兜底 | 已修 §1 |
| 9 | 细节补全 | HintBased 实例元数据 **"hint" 键匹配** (L93) + hintHeaderName 配置 (L79-81) | 已修 §3 |
| 10 | 细节补全 | ZonePreference **callGetWithRequestOnDelegates 开关** (L73-75) — get(Request) 是否传 delegate | 已补 §2 |
| 11 | 验证通过 | 回退语义 (zone L94-96 / hint L88-90 / cookie L73) 三处一致 "绝不空手"; 负面空间"不做动态权重" (无响应时间采集) | 通过 |

## 深审发现 (一轮, 锚点维度)

| # | 类型 | 发现 | 处置 |
|:--:|:--:|:--|:--|
| 1 | 验证通过 | Weighted L37/41/50/60/91 (METADATA_WEIGHT_KEY/weightFunction.apply) | 通过 |
| 2 | 验证通过 | ZonePreference L40/42/44 (ZONE 键/zoneConfig) | 通过 |
| 3 | 验证通过 | HintBased L40/57/66/68 (getHint 双来源/filteredByHint) | 通过 |
| 4 | 验证通过 | StickySession L40/60/68/73 (cookie 匹配/回退) | 通过 |
| 5 | 验证通过 | Subset L42/46/53/64/71/78 (分桶算法) | 通过 |
| 6 | 验证通过 | SameInstancePreference L38-39/L94-95 (SelectedInstanceCallback) | 通过 |

> 本轮零锚点修正 — 大纲写作时锚点直接来自前一轮 grep 实证, 准确率高 (方法论迭代效果)

## 锚点密度统计

- file:line 锚点数: **25+** (🟡B 标准 ≥4 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 6 项检查 0 修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (动态权重/多区域感知/粘性失效清理/子集动态重算/同实例过载保护/组合策略编排)
- [x] 每条有对照物 (Ribbon WeightedResponseTimeRule/ZoneAwareLoadBalancer)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (权重/区域/hint/粘性/降采样)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (Supplier 链 → 各扩展策略过滤/排序 → SCC-7 消费)
- [x] 边界交代: 区域回退/cookie 回退/分桶算法/元数据 weight

## 方法论教训

- **元数据驱动是扩展策略的共同模式** — weight/zone 都从实例元数据取 (注册中心可注入); hint/cookie 从请求取 — "数据来源决定策略类型"
- **回退语义是安全底线** — zone/cookie 无匹配都回退全部, 绝不空手 (高可用取向)
- **Netflix 算法承袭** — Subset 分桶引用 Netflix subsetting (与 SCC-7 ocelli 同源)
