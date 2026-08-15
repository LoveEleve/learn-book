# 闭环笔记 Q4:Agent Loop 本体

> 域:agent-loop.ts(796 行,100% 读完)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

agent-loop.ts 是 Pi 的循环引擎,采用"双层循环 + 队列消费"结构:内层处理工具调用,外层消费 follow-up。这是产品②"让 agent 一直跑"的核心机制。

## 验证过程

### 1. 双入口(31-143)

| 入口 | 用途 | 约束 |
|------|------|------|
| agentLoop(prompts) | 新 prompt 启动 | 无 |
| agentLoopContinue(context) | 续跑(重试/工具结果后) | **最后消息不能是 assistant**(:74-76),必须能转成 user/toolResult |

**关键设计**:
- **续跑校验**:LLM 不接受"assistant 结尾"的上下文,必须 user/toolResult 结尾。注释(:60-62)说明这个校验无法在 turn 内做,因为 convertToLlm 每轮只调一次
- **EventStream 模式**:loop 返回事件流,`agent_end` 事件携带最终 messages(:146-149)——**调用方订阅事件而非直接拿结果**

### 2. runLoop 双层循环(155-275)— 核心中的核心

```
外循环(while true):
  ├── 内循环(while hasMoreToolCalls || pendingMessages):
  │     1. 注入 pending(steering)消息(:182-190)
  │     2. streamAssistantResponse(LLM 调用,流式)(:193)
  │     3. error/aborted → agent_end 返回(:196-200)
  │     4. 提取 toolCalls(:203)
  │     5. length 截断 → failToolCallsFromTruncatedMessage(:211-214)
  │        否则 → executeToolCalls(:214)
  │     6. prepareNextTurn 钩子(可换模型/thinking)(:232-245)
  │     7. shouldStopAfterTurn 钩子(可终止)(:247-257)
  │     8. 重新取 steering(:259)
  ├── 内循环结束 = agent 想停
  └── 查 follow-up 队列(:263-268),有则继续,无则 break
```

**设计价值**:
- **steering 在每轮开始前注入,follow-up 在 agent 想停时注入**——两种队列的消费时机不同(对应 Q1 的 steer/followUp)
- **prepareNextTurn 钩子**:每轮后可以换模型/改 thinking 级别——"动态模型切换"机制
- **shouldStopAfterTurn 钩子**:外部可以决定"这轮后终止"——**任务完成判定点**,产品"章节完成检查"挂这里

### 3. streamAssistantResponse(281-372)— LLM 调用边界

```
transformContext(可插拔上下文变换)→ convertToLlm(AgentMessage→Message)
→ 组装 Context(systemPrompt/messages/tools)
→ 动态解析 API key(getApiKey,过期 token 刷新)
→ streamFunction 流式调用
→ 事件循环:start → text/thinking/toolcall delta → done/error
→ 流式过程中 partial 消息实时更新 context.messages(:337)
→ 最终消息替换 partial 或追加
```

**关键设计**:
- **转换只在 LLM 边界发生**(文件头注释 :1-4:"Transforms to Message[] only at the LLM call boundary")——内部全程 AgentMessage
- **API key 每轮动态解析**——支持过期 token 刷新(:305-306)

### 4. 工具执行:顺序 vs 并行(411-554)

```
executeToolCalls 决策:
- 有 sequential 模式的工具调用 或 配置 toolExecution="sequential" → 顺序执行
- 否则 → 并行执行

顺序执行(:433-487):逐个 prepare → execute → finalize → emit
并行执行(:489-554):
- 先全部 prepare(收集 immediate 或待执行函数)
- Promise.all 并发执行
- **结果仍按原始顺序输出**(orderedFinalizedCalls)
```

**设计价值**:并行执行 + 顺序输出——LLM 看到的工具结果顺序与调用顺序一致,不依赖完成时间。

### 5. 工具准备阶段 prepareToolCall(600-668)— 失败即返回错误结果

```
1. 工具查找:找不到 → immediate 错误结果(:608-614)
2. prepareArguments 参数准备(:617)
3. validateToolArguments 参数验证(:618)——失败抛错 → catch → 错误结果
4. beforeToolCall 钩子(:619-647)——可 block(带 reason)+ terminate 标记
5. abort 检查(:629-635, 648-654)
```

**关键设计**:**"immediate 错误结果"模式**——工具准备阶段任何失败(找不到/参数错/被 block/abort)都立即生成错误 ToolResultMessage 返回给 LLM,而不是抛异常终止循环。**LLM 会看到错误并决定下一步**。

### 6. shouldTerminateToolBatch(582-584)

```ts
return finalizedCalls.length > 0 && finalizedCalls.every((f) => f.result.terminate === true);
```
所有工具结果都标记 terminate → 终止循环(配合 beforeToolCall 的 block+terminate)。

### 7. 截断安全:failToolCallsFromTruncatedMessage(374-406)— 安全设计

**length 截断时,该消息所有工具调用全部失败**:
> "Streamed tool-call arguments are finalized with a best-effort JSON salvage parser, so a truncated message can yield tool calls whose arguments parse and validate but are silently incomplete. None of them are safe to execute."

**关键洞察**:截断的参数可能"能解析、能通过验证、但静默不完整"——**执行它们比不执行更危险**。全部失败让模型重新发起。

### 8. 流式 partial 消息处理(314-371)

- start 事件:push partial 进 context.messages + emit message_start
- delta 事件:替换 context 里最后一条消息(:337)
- done/error:finalMessage 替换或追加 + emit message_end

**设计价值**:流式过程中 context 一直有"最新 partial"——即使中途 abort,context 状态也完整。

### 9. 工具执行细节三件套(670-796)— review 新增

**executePreparedToolCall**(670-711):
- 工具可发 **partial 结果更新**(tool_execution_update,683-696)——长工具执行中实时反馈
- 执行完 flush 所有 update 事件再返回(:698-699)
- 异常 → createErrorToolResult(不中断循环)

**afterToolCall 钩子**(713-758):
- 可覆盖结果的 content/details/usage/isError/**terminate**
- 钩子自身异常 → 结果变成错误结果(:747-750)
- **"no deep merge"**:未提供的字段保留原值(:289 注释)

**createToolResultMessage**(777-791):
- **null content 归一化**(:782-784):工具返回无 content → `[]`,防 null 进 session/上下文
- **addedToolNames 传递**(:787):工具执行时可动态添加新工具

### 10. 动态工具注册(addedToolNames)— review 新增,重要发现

```
执行链:工具执行 → 结果带 addedToolNames(新激活的工具)
→ ToolResultMessage 携带 → 上下文 → 下一轮 LLM 可用新工具
来源:extensions/wrapper.ts:29-33(扩展工具激活时检测新增)
```

**这是"自进化"的机制**:agent 在执行中能给自己添加新工具能力。
**产品映射**:产品③验收器"分析中发现需要新工具"→ 动态注册,不用重启。

### 11. AgentLoopConfig 钩子契约全貌(types.ts:149-293)— review 新增

| 钩子 | 用途 | 契约约束 |
|------|------|---------|
| convertToLlm | AgentMessage→Message | **不能抛异常**(抛了打断低层循环:159-160) |
| transformContext | 上下文变换(剪枝/注入) | 不能抛异常;示例=上下文剪枝(192-198) |
| getApiKey | 动态 API key(过期 token) | 不能抛;无 key 返回 undefined |
| shouldStopAfterTurn | 本轮后停止 | 用途注释:上下文满前优雅停止(218) |
| prepareNextTurn | 换 model/thinking | 返回 undefined 保持现状 |
| getSteeringMessages | 轮后注入 steering | 不能抛;无消息返回 [] |
| getFollowUpMessages | agent 想停时注入 | 同上 |
| toolExecution | sequential/parallel | 默认 parallel |
| beforeToolCall | 执行前拦截(block+terminate) | 接收 abort signal |
| afterToolCall | 执行后覆盖结果 | no deep merge |

**关键设计洞察**:
- **所有钩子契约都是"不能抛异常,返回安全回退"**——低层循环的健壮性由钩子协议保证,不是 try/catch
- **transformContext 的官方示例就是上下文剪枝**——Pi 把"上下文窗口管理"设计成循环的可插拔钩子

### 11.5 钩子异常的双层防线(agent.ts:502-521)— review 第三轮修正

**验证**:runLoop 里所有钩子调用确实**无 try/catch 保护**(裸调用,已验证)。

**但 agent.ts 层有兜底**:
```
钩子抛异常 → Agent 的 try/catch(agent.ts:506)→ handleRunFailure:
  构造 stopReason="error" 的 assistant 消息
  → 发 message_start/message_end/turn_end/agent_end 完整事件序列
  → 错误消息进上下文(LLM 下一轮能看到)
```

**结论修正**:"不能抛异常"是**契约要求(第一防线)**,handleRunFailure 是**运行时兜底(第二防线)**——异常不挂死循环,被转成"错误消息事件"正常收尾。
**产品映射**:产品扩展点(验收器/投影器)用"契约 + 兜底"双保险——钩子挂了,主循环还能正常收尾并让 LLM 看到错误。

### 12. thinking level 7 档(types.ts:300)

`off / minimal / low / medium / high / xhigh / max`——xhigh/max 仅部分模型族支持(用模型元数据检测)。

### 13. Agent 类生命周期(agent.ts 592 行,100% 读完)— review 第四轮新增

**单例运行保护**(prompt/continue):
- `prompt()`:activeRun 时抛错 "already processing. Use steer() or followUp()"(:351-355)
- `continue()`:同上(:362-364)

**continue() 的真实逻辑(修正 Q4 笔记 #1)**(:361-388):
```
最后消息是 assistant 时:
  1. 先 drain steering 队列 → 有则当新 prompt 跑(skipInitialSteeringPoll)
  2. 再 drain follow-up → 有则跑
  3. 都没有 → 抛 "Cannot continue from message role: assistant"
```
**修正**:assistant 结尾不是立即报错——**先消费队列**,队列有消息就能继续。Q4 笔记 #1"续跑校验"不完整,应补充此逻辑。

**队列消费方式**(createLoopConfig,:475-482):
```ts
getSteeringMessages: async () => {
  if (skipInitialSteeringPoll) { skipInitialSteeringPoll = false; return []; }
  return this.steeringQueue.drain();   // drain = 一次性取空
}
getFollowUpMessages: async () => this.followUpQueue.drain(),
```
- **skipInitialSteeringPoll**:首轮不轮询 steering(新 prompt 本身已是消息,避免重复)
- **drain 而非 peek**:每轮取空队列——避免同一消息被多轮消费

**runWithLifecycle**(:486-509):
```
activeRun 单例保护 → AbortController → executor(signal)
→ 异常 → handleRunFailure(错误转消息,已记录)
→ finally → finishRun(清状态 + resolve)
```

**processEvents 事件归约(reducer)**(:544-591):
```
message_start/update → _state.streamingMessage
message_end → push 进 _state.messages(transcript)
tool_execution_start/end → pendingToolCalls 集合增删
turn_end(有 errorMessage)→ _state.errorMessage
agent_end → 清 streamingMessage
然后:广播给所有 listener(带 signal)
```

**设计价值**:**"事件→状态归约"模式**——所有内部状态通过事件流更新,监听者看到的是统一事件源。产品④知识库自动记录 = 监听同一事件流。

### 14. prepareNextTurnWithContext 与 prepareNextTurn 双版本(:463-471)

```
prepareNextTurn:无 context 参数(仅 signal)
prepareNextTurnWithContext:带完整 context
优先级:WithContext 优先
```
**双版本兼容设计**——老 API 兼容,新 API 增强。

## 代码类型

Algorithmic(循环引擎)

## 跨域关联

- → 被依赖:AgentSession._runAgentPrompt(agent.prompt/continue 最终到这里)
- ← 依赖:types.ts(AgentContext/AgentLoopConfig)、stream-fn.ts(默认流函数)、pi-ai(EventStream/validateToolArguments)

## 结论

核心可抄设计 14 个:
1. **双入口(启动/续跑)+ 续跑校验(非 assistant 结尾)** → 产品循环入口
2. **双层循环(内层工具/外层 follow-up)** → 产品执行引擎主干
3. **steering 轮前注入/follow-up 停前注入** → 队列消费时机
4. **prepareNextTurn 钩子(换模型/thinking)** → 动态模型切换
5. **shouldStopAfterTurn 钩子** → 任务完成判定点(产品章节完成检查)
6. **工具并行执行 + 顺序输出** → 结果一致性
7. **immediate 错误结果模式** → 失败不中断,LLM 决定下一步
8. **截断全失败安全设计** → 防"看似完整实则残缺"的参数
9. **工具 partial 更新流 + afterToolCall 覆盖** → 长工具实时反馈 + 结果可改
10. **动态工具注册(addedToolNames)** → 自进化机制
11. **钩子契约"不能抛异常"协议 + handleRunFailure 兜底** → 扩展点双层防线
12. **transformContext 官方示例=上下文剪枝** → 上下文管理是可插拔钩子
13. **continue 先消费队列再报错 + drain 消费 + skipInitialSteeringPoll** → 续跑与队列消费细节
14. **processEvents 事件归约(reducer)模式** → 统一事件源驱动状态

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| 双层循环 | ✅ 抄 | 产品执行引擎主干 |
| 续跑校验(先消费队列) | ✅ 抄 | 防止非法续跑 |
| prepareNextTurn 钩子 | ✅ 抄 | 分析中切换模型(如摘要用便宜模型) |
| shouldStopAfterTurn 钩子 | ✅ 抄 | **章节完成自动判定**(挂验收器) |
| 并行执行+顺序输出 | ✅ 抄 | 多工具(如多文件读取)效率 |
| immediate 错误结果 | ✅ 抄 | 失败结论进上下文(LLM 可自纠正) |
| 截断全失败 | ✅ 抄 | 安全边界 |
| EventStream 事件流 | ✅ 抄 | 知识库自动记录(订阅事件) |
| 动态工具注册 | ✅ 抄 | 验收器发现新需求时注册新工具 |
| 钩子双层防线 | ✅ 抄 | 产品扩展点契约 |
| processEvents 归约 | ✅ 抄 | 知识库自动记录(同一事件源) |
| drain 消费队列 | ✅ 抄 | 避免消息重复消费 |

## 面试问答弹药

- **Q**:agent 循环怎么组织?→ A:双层循环——内层跑工具调用直到没工具可调,外层消费 follow-up 队列直到没有新消息
- **Q**:用户执行中插话?→ A:steering 在每轮开始前注入,follow-up 在 agent 想停时注入
- **Q**:工具能并行执行吗?→ A:能,默认并行,但结果按原始顺序输出;sequential 工具强制顺序
- **Q**:截断的响应怎么处理?→ A:length 截断时所有工具调用全部失败——截断参数可能"验证通过但残缺",执行更危险
- **Q**:工具准备失败会中断循环吗?→ A:不会——immediate 错误结果返回给 LLM,LLM 决定下一步
- **Q**:agent 能自己加工具吗?→ A:能——工具结果带 addedToolNames,动态注册新工具(自进化)
- **Q**:钩子出异常怎么办?→ A:契约要求不抛;就算抛了,handleRunFailure 转成错误消息,循环正常收尾
- **Q**:assistant 结尾能继续吗?→ A:先消费 steering/follow-up 队列,队列有消息就能继续;没有才报错
- **Q**:内部状态怎么更新?→ A:事件归约(reducer)——processEvents 按事件类型更新状态,监听者看到统一事件源
