# q34 — V1 LLM 编排(深度版:双运行时 + 工具修复 + workflow 审批)

> 域:②参考(V1 请求层) | 文件:opencode/src/session/llm.ts(404)+ llm/(request 226/native-request 196/native-runtime 195/ai-sdk 288)+ 对比 V2 runner/llm
> review 轮次:2 轮(源码全文核心段)

---

## 假设

V1 LLM = "AI SDK 默认 + native 可选"的双运行时:experimentalNativeLlm 开启时先试 @opencode-ai/llm,不支持则回退 AI SDK。工具调用修复(repairToolCall)和 workflow 审批是 V1 的独特复杂度。V2 只用 native 单路径。

## 验证

### 1. 双运行时(设计 1:seam + 回退)

```ts
// llm.ts:226-278
flags.experimentalNativeLlm → LLMNativeRuntime.stream(...)
  native.type === "supported" → 返回 native 流
  否则 → logInfo("native runtime unavailable; falling back to ai-sdk", reason)
默认路径:streamText(AI SDK 全权:provider 执行 + 工具调度)
  + LLMAISDK.toLLMEvents(fullStream parts → LLMEvent 规范化)
// 对比 V2:AGENTS.md "Preserve one explicit llm.stream(request) call per provider turn"
//   ——V2 只走 native,无 AI SDK 路径
```

### 2. 工具修复(设计 2:repairToolCall)

```ts
// llm.ts:267-287
experimental_repairToolCall(failed):
- 工具名大小写不匹配 → 转小写重试(模型常犯)
- 其他失败 → 转为 invalid 工具调用(输入含 error 信息)——错误进模型上下文,自纠正
// 对比 V2:V2 的无效调用 → registry 结算层 error result(q8)
```

### 3. GitLab workflow 审批(设计 3:会话级预批 + 防循环)

```ts
// llm.ts:118-196
toolExecutor:workflow 模型工具调用 → opencode 工具系统执行 → 结果回 WebSocket
sessionPreapprovedTools:规则集非 ask 的工具列表
approvalHandler:
- 会话已批准的(approvedToolsForSession)→ 自动通过(防服务端 MCP 工具死循环)
- 否则 perm.ask(workflow_tool_approval, patterns=[name: title])→ 批准后加入会话集合
```

### 4. 遥测注入(设计 4:tracer Proxy)

```ts
// llm.ts:214-224
cfg.experimental.openTelemetry → OtelTracer serviceOption
Proxy:startSpan 包装 → 自动注入 session.id 属性
```

### 5. 请求准备(设计 5:LLMRequestPrep)

```ts
// llm/request.ts(226):provider/auth/plugin/flags → prepared{system/messages/tools/toolChoice/params/headers}
// 工具定义:经 tool.definition 钩子(q33)后进请求
// native-request.ts(196):AI SDK 形状 → LLMRequest 降级适配
// native-runtime.ts(195):LLMClient.stream + 工具调度桥接
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 双运行时 seam + 显式回退日志 | llm.ts:226-278 | ②运行时演进 |
| 2 | 工具调用修复(大小写 + invalid 转换) | llm.ts:267-287 | ②容错 |
| 3 | workflow 审批(会话预批 + 防循环) | llm.ts:118-196 | ②外部模型集成 |
| 4 | tracer Proxy 注入 session.id | llm.ts:214-224 | ③遥测 |
| 5 | 请求准备管线 + native 适配器 | llm/request.ts | ②请求组装 |

## 面试弹药

- "双运行时显式回退":native 不支持 → 记录 reason 回退 AI SDK——feature flag 驱动迁移
- "repairToolCall 转 invalid 工具":错误进上下文让模型自纠(不是失败终止)——容错设计
- "会话级预批防死循环":workflow 服务端 MCP 工具每次调用都要审批会死循环——会话内已批自动通过
- "V2 单路径":只保留 native llm.stream——迁移完成后删除双运行时复杂度

## 待深挖

- [ ] ai-sdk.ts 的 fullStream → LLMEvent 转换
- [ ] request.ts 的插件变换管线(参数/头)
- [ ] V1 的工具执行桥(toolCtx 结构)
