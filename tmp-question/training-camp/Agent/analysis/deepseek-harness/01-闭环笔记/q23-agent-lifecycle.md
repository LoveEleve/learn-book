# q23 — Agent 生命周期(深度版:工厂 + 配置校验 + 声明式 agent)

> 域:②执行引擎(生命周期) | 文件:packages/core/agent-loop/src/index.ts(713)+ agent.ts + constants.ts
> review 轮次:2 轮(源码全文核心)

---

## 假设

AgentLoop = 具体工厂 + 驱动服务:声明式 agent 配置(启动时创建/恢复)、身份冲突预校验、并行上限。FactoryOwnership 管理 live agents + 启动任务的有序 teardown。

## 验证

### 1. 配置校验(设计 1:加载时拒绝冲突)

```ts
// index.ts:274-294 validateConfiguredAgents:
sessionId 与 resumeSessionId 互斥(sessionId + hasResumeId → throw)
重复 exact session identity → throw("agents X and Y use duplicate exact session identity")
// Config:maxParallelToolCalls(1 = 串行;默认 DEFAULT_MAX_PARALLEL_TOOL_CALLS)
// agents:声明式 agent 数组(id/sessionId/provider/model/maxTokens/cwd/resumeSessionId)
// "misconfiguration fails loud"(dsh 惯例)
```

### 2. 工厂(设计 2:create/resume)

```ts
// index.ts:606-660:
createAgent(ownerCtx, options)→ AgentHandle:setupAndPublish(装配 + 发布)
resume(ownerCtx, options):恢复持久化会话
// restoreOrCreateConfigured(407):配置 agent 启动时恢复或创建
// waitForDrainingConfiguredIdentity(431):等待身份排空(防并发占用)
// raceAbort/raceAbortCall:创建/恢复的 abort 竞速
```

### 3. 所有权(设计 3:FactoryOwnership)

```ts
// index.ts:39-90:
FactoryOwnership:accepting + teardown AbortController + inactive promise
  liveAgents(Set<dispose>)/ startupTasks(Set<Promise>)
  track:登记 live agent teardown;trackStartup:登记配置启动工作;trackWrapper:公开 create/resume continuation
  waitWhileActive:task 或 teardown 竞速
  dispose:accepting=false + teardown.abort('agent loop is not active') + inactive.resolve()
    + await 全部 liveAgents dispose + startupTasks
// INACTIVE_STATES:UNLOADING/DISPOSED/FAILED(fiber 状态)——不能拥有/服务新生命周期
```

### 4. 每读配置(设计 4:动态上限)

```ts
// index.ts:326-330:
"Read through on every scheduler decision:tool-calls.ts destructures this at the start
 of each group,so a committed change caps the next group without disturbing the one in flight"
// ——maxParallelToolCalls 动态生效(当前组不受影响)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 加载时身份冲突预校验 | index.ts:274-294 | ②配置正确性 |
| 2 | create/resume 工厂 + abort 竞速 | index.ts:606-660 | ②生命周期 |
| 3 | FactoryOwnership 有序 teardown | index.ts:39-90 | ②插件卸载 |
| 4 | 每读并行上限(动态生效) | index.ts:326-330 | ②并发控制 |

## 面试弹药

- "misconfiguration fails loud":身份冲突加载时 throw(会话 ID 互斥/重复)——绝不静默
- "teardown 等待活体 + 启动任务":FactoryOwnership.dispose 等全部 settle——卸载完整
- "每读上限":commit 的变更封顶下一组,不打扰在飞组——并发决策动态
- "INACTIVE_STATES":UNLOADING/DISPOSED/FAILED 不能拥有新生命周期——fiber 状态驱动

## 待深挖

- [ ] setupAndPublish 的装配细节
- [ ] resume 的恢复语义(seedLength)
- [ ] launcher identities(configuredAgentIdentities)
