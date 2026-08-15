# q53 — V1 工具桥 + System + Reminders(深度版:执行接线)

> 域:②参考(V1 执行面) | 文件:opencode/src/session/(tools.ts 590/system.ts 152/reminders.ts 92)+ prompt/*.txt
> review 轮次:2 轮(源码全文核心)

---

## 假设

V1 执行接线三件套:SessionTools(工具解析/执行桥——处理器调用的工具解析器)、SystemPrompt(模型家族系统提示词分发 + 环境/技能/MCP 描述)、Reminders(plan/build 切换提醒注入)。

## 验证

### 1. SessionTools(设计 1:工具解析桥)

```ts
// tools.ts:41+ resolve({agent, model, session, processor, ...}):
- 工具集:registry 全量 + MCP 资源工具(list_mcp_resources/list_templates/read,MAX_BLOB 10MB + 附件 mime 白名单)
- context 构造:Tool.Context(sessionID/agent/messageID/callID)
- EffectBridge 桥接(工具执行回 Effect 世界)
- 处理器句柄:updateToolCall/completeToolCall(工具进度回写)
// 语义:每轮执行前 resolve 一次工具集(含 MCP 注入)
```

### 2. SystemPrompt(设计 2:模型家族分发)

```ts
// system.ts:27-47 provider(model):
muse → PROMPT_META;gpt-4/o1/o3 → BEAST;codex → CODEX;gpt → GPT;
gemini → GEMINI;claude → ANTHROPIC;trinity → TRINITY;kimi → KIMI;默认 → DEFAULT
// environment(model):环境描述;skills(agent):技能列表;mcp(agent, permission):MCP 服务器描述(权限过滤)
// ——V1 系统提示词组装(provider 差异在提示词层,q50 的 prompt/*.txt)
```

### 3. Reminders(设计 3:模式切换提醒)

```ts
// reminders.ts:apply({messages, agent, session}):
- plan agent → 用户消息注入 PROMPT_PLAN(synthetic part)
- 曾用 plan 且现在 build → 注入 BUILD_SWITCH(plan→build 切换提醒)
- experimentalPlanMode → PLAN_MODE
// 用途:模式切换时提醒模型当前模式(防误用旧模式指令)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 工具解析桥(MCP 注入 + 处理器回写) | tools.ts:41+ | ②执行接线 |
| 2 | 模型家族提示词分发 + 环境/技能/MCP 描述 | system.ts:27-47 | ②系统提示词组装 |
| 3 | 模式切换提醒(plan/build) | reminders.ts | ②意图漂移锚点 |

## 面试弹药

- "MCP 资源工具化":list/read MCP 资源作为标准工具(10MB 限制 + mime 白名单)——统一工具面
- "模型家族提示词":kimi/codex/gemini 各自模板——供应商差异在提示词层消化
- "模式提醒防漂移":plan→build 切换注入提醒——模型知道当前模式

## 待深挖

- [ ] tools.ts 的工具执行细节(截断/错误回写)
- [ ] 各模型家族提示词的内容差异
- [ ] reminders 的完整模式矩阵
