# q11 — Compaction + Skill + Subagent(深度版:三个能力缝样本)

> 域:②执行(能力缝) | 文件:packages/(compaction/compaction + compaction-basic + tool-result-pruner + command-compact)+(skill/skill + skill-filesystem + skill-badge + tool-skill)+(subagent/subagent + 7 providers + 3 tools)
> review 轮次:2 轮(源码结构 + 接口面)

---

## 假设

Compaction/Skill/Subagent = 三个能力缝样本,展示"缝"的三种复杂度:compaction(简单缝)、skill(带政策)、subagent(最复杂——7 providers + 生命周期事件)。

## 验证

### 1. Compaction 缝(设计 1:agent 上下文)

```ts
// compaction/compaction/src/index.ts:
CompactionAgentContext(60)/ ManualCompactAgentContext(70):压缩 agent 上下文(手动/自动)
Service(ctx.compaction)
// compaction-basic:basic provider;tool-result-pruner:工具结果修剪(压缩策略)
// command-compact:手动压缩命令
```

### 2. Skill 缝(设计 2:带政策)

```ts
// skill/skill/src/index.ts:
BUNDLED_SKILL_RANK = 600(排序)
SkillInvocationPolicy(48):调用政策
SkillDefinition(86)/ SkillCandidate(74):定义/候选
SkillLookupOptions/SkillViewOptions(104-117):查找/视图选项
SkillInvocationSource(147):调用来源
// skill-filesystem:文件系统技能;skill-badge:徽章;tool-skill:模型工具
// architecture.md:技能 provider 注册表 + 本地实现 + catalog/loader 工具
```

### 3. Subagent 缝(设计 3:7 providers + 生命周期)

```ts
// subagent/subagent/src/index.ts:140-166:
'subagent/provider-added'(emit)/ 'subagent/provider-removed'(emit)
'subagent/start'(Scoped<SubagentRuntime>, info)/ 'subagent/end'(info)
// 7 providers:acp(委托 ACP)/claude-code(委托 Claude Code)/codex(委托 Codex)/
//   dsh-sdk(JSON-RPC)/fork-in-process(进程内 fork)/in-process-driver(进程内驱动)/spawn-in-process(进程内 spawn)
// 3 Consumers:tool-subagent/tool-subagent-control/tool-subagent-report
// 相关:ChildComposition/DelegatedPolicyOverrides(child-agent.ts)
//   ContinuableSetupContribution(activation-setup-registry)
//   SubagentListEntry/SubagentDescendantListEntry(list-children)
//   SubagentIdentityProjection/SubagentTimingProjection(projection-types)
```

**产品启示**:②子章节执行(每章子 agent)= subagent 缝——从进程内 fork 到委托外部产品,同一接口。这是"章节并行/委托"的现成架构。

### 4. 缝复杂度光谱(设计 4:三样本对比)

| 缝 | 复杂度 | 特征 |
|----|:--:|------|
| compaction | 低 | agent 上下文 + basic provider |
| skill | 中 | 政策 + 排序 + 查找/视图选项 |
| subagent | 高 | 生命周期事件 + 7 providers + 委托/控制/报告 |

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Compaction 缝(手动/自动 + 结果修剪) | compaction/* | ②上下文管理 |
| 2 | Skill 缝(政策 + 排序 + 视图) | skill/* | ①骨架/技能 |
| 3 | Subagent 缝(7 providers + 生命周期) | subagent/* | ②子任务/委托 |
| 4 | 复杂度光谱(低/中/高) | 三样本 | ②缝设计尺度 |

## 面试弹药

- "subagent 7 providers":进程内 fork 到委托 Claude Code/Codex/ACP——同一接口覆盖从轻到重
- "skill 带政策":SkillInvocationPolicy + BUNDLED_SKILL_RANK——技能调用受控 + 排序
- "compaction 是低复杂度缝":agent 上下文 + basic provider——缝最小形态

## 待深挖

- [ ] tool-result-pruner 的修剪策略
- [ ] subagent 的 DelegatedPolicyOverrides(委托政策覆盖)
- [ ] in-process-driver vs spawn-in-process 差异
