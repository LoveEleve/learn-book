# hq24 学习图谱(Learning Graph)— 产品④"知识关联可视化"蓝本

> 项目:Hermes(agent/learning_graph.py 328 行 + hermes_cli/journey.py/web_server.py/tui_gateway 消费 + desktop 面板)
> 假设:"学习可见"图——用户学到的技能(agent 创建/使用)+ 记忆块(MEMORY.md/USER.md)作为一等节点,关联可答"我学的技能与记住的什么相连"。是"知识关联可视化"的样本。
> 结论:✅ 成立——学习信号过滤/技能-技能边/记忆-技能词法关联/密度统计/集群全具备,产品④"知识图谱可视化"直接蓝本。

---

## 一、架构全景:学习可见图

```
┌────────────────────────────────────────────────────────────┐
│ 节点:                                                    │
│   技能(非 base + 学习信号:agent 创建或使用过)             │
│      SkillNode(name/category/source/timestamp/use_count/  │
│                state/created_by/pinned/related)           │
│   记忆(MEMORY.md/USER.md 按 § 分隔的 prose 块)            │
│      memory:{source}:{idx} 卡片(标题/正文/时间戳)         │
├────────────────────────────────────────────────────────────┤
│ 边:                                                       │
│   技能-技能:声明 related_skills(两端都存在,去重无向)      │
│   记忆-技能:词法重叠(技能名命中 +6,词 token 交集计数,     │
│     每记忆块 top-4)——答"记住的什么与哪些技能相连"         │
├────────────────────────────────────────────────────────────┤
│ 消费:journey.py / web_server(API)/ desktop 学习面板       │
│ 统计:density_stats(节点/边/密度/孤立%/类别/agent 创建/使用)│
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:学习信号过滤(图只显示学到的)

**位置**:`learning_graph.py:262-267`(learned_skills 过滤)+ `125-153`(build_skill_nodes)

```
learned_skills = 非 base 源 + (agent 创建或 use_count > 0)
  ——base 安装的技能不算"学到的";agent 创建或使用过的才算学习信号

build_skill_nodes:
- 排除 .archive/.hub/node_modules/.git(归档/外来/依赖不显示)
- 用量合并(tools.skill_usage.load_usage,失败回退 .usage.json)
- 时间戳优先用量 last_activity_at → 文件 mtime
- related_skills 从 frontmatter 或 metadata.hermes(容错解析)
```

**正确性价值**:图聚焦"profile 学到且可行动的"——base 技能不算学习;学习信号(创建/使用)是过滤标准。

**产品④映射**:知识图谱聚焦学习信号——"学到的"是 agent 创建或使用过的,不是全部。

## 设计 2:技能-技能边(声明 related_skills)

**位置**:`learning_graph.py:156-168`(build_edges)

```
无向 related_skills 边,两端都存在才连(去重):
  target in nodes 且 != 自己;排序(a,b)去重
  ——悬挂引用不连(目标技能不存在则断边)
```

**正确性价值**:边只连真实存在的两端(悬挂引用断边);无向去重。

**产品④映射**:知识图谱边完整性——引用目标不存在则断边(不画幽灵边)。

## 设计 3:记忆-技能关联(词法重叠评分)

**位置**:`learning_graph.py:227-245`(_memory_skill_edges)+ `223-224`(_tokenize)

```
每记忆块 × 每技能评分:
  技能名出现在记忆文本 → +6(强信号)
  词 token 交集(>=3 字符)计数
  → 排序后取 top-4 技能连边

_tokenize:非字母数字切分,>=3 字符 token 集(去停用词噪声)
```

**正确性价值**:**"记住的什么与哪些技能相连"**——词法重叠答图查询;技能名命中 +6 强信号优先。

**产品④映射**:知识关联的推导——词法重叠(简单可解释)+ 强信号加权(名称命中)。

## 设计 4:密度统计(图健康度)

**位置**:`learning_graph.py:171-190`(density_stats)

```
nodes/related_edges/edges_per_node/linked_nodes/isolated_pct/
categories/agent_created/used/top_categories(前 8)
  ——孤立 %(n - linked)/n:图有多少技能没被连接
  ——agent_created:自我沉淀比例;used:实际使用比例
```

**正确性价值**:统计让图健康可诊断——孤立率(知识碎片化)/使用率(沉淀有用性)可量化。

**产品④映射**:知识图谱健康度指标——孤立率/使用率/自我沉淀率。

## 设计 5:记忆卡片(§ 分隔的 prose 块)

**位置**:`learning_graph.py:193-220`(_memory_cards)

```
MEMORY.md/USER.md 按裸 § 分隔 → 每块一卡片
  (memory:memory|profile:{idx} 节点 id)
- 标题 = 首行(80 字符截断);正文 = 1200 字符
- 时间戳 = 文件 mtime + chunk_idx(同文件内递增序)
- "Every chunk is surfaced — the graph shows everything"(不筛选记忆)
```

**正确性价值**:记忆全显(不筛选)——图谱显示一切;§ 分隔是结构化边界。

**产品④映射**:知识块结构化——prose 按分隔符分卡片,全显不筛选。

---

## 三、与四项目对比(知识可视化)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes learning_graph |
|------|----|----------|----------|-----|-----------------------|
| 知识节点 | facts | 结论模型 | — | — | **技能 + 记忆块双类** |
| 关联 | — | subject 冲突 | — | — | **related_skills + 词法重叠** |
| 过滤 | — | pinned/relevant | — | — | **学习信号(创建/使用)** |
| 统计 | — | — | — | — | **密度/孤立%/使用率** |
| 消费 | — | — | — | — | **desktop 学习面板 + API** |

**结论**:产品④"知识可视化"参考 = Hermes learning_graph(双类节点 + 词法关联 + 健康度统计)。**与 Reasonix subject 冲突互补:subject 管"一问题一答案"(正确性),learning_graph 管"知识怎么连"(可视性)**。

---

## 四、面试弹药

1. **"图只显示学到的"**:非 base + (agent 创建或使用)——base 技能不算学习信号
2. **"记忆-技能词法关联"**:技能名命中 +6 强信号 + token 交集——答"记住的与哪些技能相连"
3. **"悬挂引用断边"**:related_skills 目标不存在则断边——不画幽灵边
4. **"孤立率可诊断"**:isolated_pct 量化知识碎片化;使用率量化沉淀有用性
5. **"记忆全显"**:§ 分隔块全部显示,不筛选——图谱显示一切

---

## 五、产品映射汇总

| 设计 | 产品④用法 |
|------|---------|
| 学习信号过滤 | 图谱聚焦 agent 创建/使用的 |
| 技能-技能边 | 声明关联(悬挂断边) |
| 记忆-技能词法 | 知识关联推导(+6 强信号) |
| 密度统计 | 图健康度(孤立/使用/沉淀) |
| 记忆卡片 | § 分隔结构化 + 全显 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_learning_graph.py(3:密度统计孤立/记忆分隔卡片/全载荷形状+边完整性)+ test_learning_graph_render.py(5:渲染)
> 位置:build_learning_graph :254 / build_skill_nodes :125 / build_edges :156 / _memory_skill_edges :227 / density_stats :171 / _memory_cards :193
> 消费:hermes_cli/journey.py:24 / web_server.py:3621(API)/ desktop 学习面板
