# hq27 洞察引擎(Insights)— 产品④"历史数据报告"蓝本

> 项目:Hermes(agent/insights.py 1,212 行 + hermes_cli/commands.py:323 /insights 命令)
> 假设:历史会话数据 → token/成本/工具模式/活动趋势报告——Hermes 的 InsightsEngine 是"知识库数据报告"的样本(Claude Code /insights 启发,多平台适配)。
> 结论:✅ 成立——报告结构/成本估算/工具技能拆分/平台分解/趋势/空报告语义全具备,产品④"历史分析"直接蓝本。

---

## 一、架构全景:会话数据 → 洞察报告

```
数据源:SessionDB(会话/messages/token 计数/工具使用/skill 使用)

┌────────────────────────────────────────────────────────────┐
│ generate(days=30, source=None):完整报告                   │
│   flush_token_counts 先(异步记账队列排空——报告反映精确计数)│
│   overview(总览)/models(模型)/platforms(平台分解)/         │
│   tools(工具分解)/skills(技能分解)/activity(活动趋势)/     │
│   top_sessions(最活跃会话)                                │
├────────────────────────────────────────────────────────────┤
│ 分解:                                                    │
│   _compute_platform_breakdown(平台)                        │
│   _compute_tool_breakdown(工具,自 _get_tool_usage)         │
│   _compute_skill_breakdown(技能,自 _get_skill_usage)       │
├────────────────────────────────────────────────────────────┤
│ 成本:_estimate_cost + _fmt_est_cost(共享成本标签)          │
│ 呈现:format_terminal(终端条形图 _bar_chart)                │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:报告结构(全景多维度)

**位置**:`insights.py:140-240`(generate)+ `200-240`(报告结构)

```
generate(days=30, source=None):
  - source 过滤(按来源平台)
  - ★ flush_token_counts 先——token/成本计数可能还在 SessionDB 异步
    记账队列;排空后报告反映精确计数(不靠估算)
  - 报告字段:overview/models/platforms/tools/skills/activity/
    top_sessions/empty 标志/generated_at
  - 空报告语义:无数据 → empty=True(可友好提示非错误)
```

**正确性价值**:报告前排空记账队列(精确而非估算);空报告友好(empty 标志);多维度全景。

**产品④映射**:知识库使用报告——精确计数前置(排空队列)+ 多维度分解 + 空语义。

## 设计 2:工具/技能/平台分解

**位置**:`insights.py:305`(_get_tool_usage)+ `385`(_get_skill_usage)+ `766-810`(compute 三分解)

```
工具使用(_get_tool_usage,cutoff+source 过滤)→ _compute_tool_breakdown
技能使用(_get_skill_usage)→ _compute_skill_breakdown
平台分解(_compute_platform_breakdown,会话统计)

——"哪些工具/技能被用"是可行动洞察(工具链优化/技能保鲜输入)
```

**正确性价值**:工具/技能/平台三分解——可行动洞察(什么常用/什么没用)。

**产品④映射**:知识库使用模式分解——工具/技能/平台三维度,驱动治理决策。

## 设计 3:成本估算

**位置**:`insights.py:48`(_estimate_cost)+ `36`(_fmt_est_cost)

```
_estimate_cost:token 用量 → 成本估算(按模型定价)
_fmt_est_cost:共享 cost-label 格式化(与全局一致)
  ——估算标签化,不假装精确
```

**正确性价值**:成本估算标签化(明确是估算);格式化共享(一致性)。

**产品④映射**:知识库成本可见——估算标签化 + 共享格式。

## 设计 4:终端呈现(条形图)

**位置**:`insights.py:89`(_bar_chart)+ `970`(format_terminal)

```
format_terminal:报告 → 终端文本(Claude Code /insights 风格)
_bar_chart:数值 → 条形图(max_width=20)
```

**正确性价值**:终端可读呈现(CLI 消费)——条形图直观。

**产品④映射**:报告呈现层——CLI 终端格式 + 可视化。

## 设计 5:多平台适配

**位置**:`insights.py:1-30`(模块头)+ commands.py:323

```
"adapted for Hermes Agent's multi-platform architecture with additional
cost estimation and platform breakdown"——Claude Code /insights 启发

消费:hermes_cli/commands.py:323 CommandDef("insights", "Show usage
insights and analytics", "Info")
```

**正确性价值**:跨平台适配(多来源)是 Hermes 特有增强(Claude Code 单平台)。

**产品④映射**:知识库报告多源适配——多平台分解。

---

## 三、与四项目对比(历史分析)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes insights |
|------|----|----------|----------|-----|-----------------|
| 历史报告 | — | — | — | insights | **InsightsEngine(Claude Code 启发)** |
| 精确性 | — | — | — | — | **flush_token_counts 先(不靠估算)** |
| 分解 | — | — | — | — | **平台/工具/技能三维** |
| 成本 | — | — | — | — | **估算标签化 + 共享格式** |
| 呈现 | — | — | — | — | **终端条形图 + CLI 命令** |

**结论**:产品④"历史分析"参考 = Hermes insights(报告结构 + 精确计数 + 三维分解 + 成本估算)。**与 hq2 用量追踪互补:usage 是每技能遥测(治理输入),insights 是聚合报告(用户可见)**。

---

## 四、面试弹药

1. **"排空记账队列再报告"**:token/成本计数还在异步队列——flush 后报告反映精确计数,不靠估算
2. **"空报告友好"**:无数据 → empty=True 可提示,非错误
3. **"三维分解"**:平台/工具/技能——什么常用/什么没用可行动
4. **"成本估算标签化"**:_estimate_cost 明确是估算,共享 cost-label 格式
5. **"Claude Code 启发 + 多平台适配"**:/insights 启发,Hermes 加成本估算 + 平台分解

---

## 五、产品映射汇总

| 设计 | 产品④用法 |
|------|---------|
| 报告结构 | 全景多维度(精确计数前置) |
| 三维分解 | 工具/技能/平台可行动洞察 |
| 成本估算 | 标签化 + 共享格式 |
| 终端呈现 | CLI 条形图 |
| 多平台适配 | 跨来源报告 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_insights.py(40 用例:报告结构/分解/成本/趋势/空语义/格式化)
> 位置:generate :140 / _get_tool_usage :305 / _compute_platform_breakdown :766 / format_terminal :970 / 消费 commands.py:323(/insights 命令)
