# q13 — LLM 包(深度版:Route 四轴 + generateObject + 工具调度)

> 域:②底层(供应商抽象) | 文件:packages/llm/src/(llm.ts 186/route/client.ts 436/executor 385/protocol.ts 84/endpoint.ts 53/auth.ts 156/framing.ts 27/transport/)+ schema/(messages/events/errors/options)+ tool.ts(253)+ tool-runtime.ts(78)+ AGENTS.md(架构权威)
> review 轮次:2 轮(源码 + 架构文档 + 测试结构)

---

## 假设

LLM 包 = "Effect Schema-first 的 LLM 核心",与 Core/Server 完全解耦。Route 是注册的四件套组合(Protocol/Endpoint/Auth/Framing)——供应商接入 = 5-15 行 Route.make,不是 300-400 行克隆。工具循环在包外(会话层负责持久化/续跑)。

## 验证

### 1. Route 四轴(设计 1:协议/端点/认证/帧)

```ts
// AGENTS.md(权威):Route = Protocol + Endpoint + Auth + Framing 组合
Protocol:body.from/body.schema/stream.event/stream.step(语义契约,含事件状态机)
Endpoint:baseURL/path/query(URL 构造)
Auth:bearer/header/签名(Bedrock SigV4,Azure AAD 函数式签名)
Framing:SSE 共享;Bedrock 保留 AWS event-stream 二进制帧
// 结论:DeepSeek/TogetherAI/Cerebras/Baseten/Fireworks/DeepInfra 全部复用 OpenAIChat.protocol
//       ——"bug fix in one protocol propagates to every consumer in a single commit"
```

**Transport 缝**(非 HTTP 传输):OpenAI WebSocket Responses 后端 = WebSocketTransport.jsonTransport(同一 protocol/endpoint,不同 transport)。

### 2. LLM 入口(设计 2:request/stream/generate/generateObject)

```ts
// llm.ts:53-75 request:归一化 RequestInput → LLMRequest(system/messages/tools/toolChoice/generation 构造)
// llm.ts:45-47 generate/stream = LLMClient 转发
// 执行流程(AGENTS.md):request.model.route → 构建 provider-native body → transport 取 HttpClientRequest
//   → RequestExecutor.Service 发送 → 解析 provider 流为 LLMEvent → LLMResponse
```

### 3. generateObject(设计 3:强制合成工具,provider-native JSON 模式刻意不用)

```ts
// llm.ts:80-144
GENERATE_OBJECT_TOOL_NAME = "generate_object"
runGenerateObject:tools=[generate_object] + toolChoice: named(generate_object)
→ 模型必须调用该工具 → tool._decode(call.input) → GenerateObjectResponse
// 两种输入:schema(Effect Schema,typed)或 jsonSchema(运行时 schema:MCP/插件清单,unknown + 调用方校验)
// 失败:模型没调用强制工具 → InvalidProviderOutputReason LLMError
```

**设计要点**:不用 provider-native JSON mode(行为不一致),强制工具调用 = 全协议统一。产品③验收器的结构化输出方案直接可抄(与 V1 的 StructuredOutput 工具同思想,更通用)。

### 4. 工具调度(设计 4:ToolRuntime 单次调用 + 错误三径)

```ts
// tool-runtime.ts:78 行
dispatch(tools, call):解码 input → execute → 编码 result → tool-result 事件
// 不:流式/会话事件/调度 fiber/追加历史/计步/续轮——持久化与续跑在包外(会话层)
// 错误三径(AGENTS.md):
//   1. 未知工具名 → tool-error
//   2. 输入 Schema 失败 → tool-error
//   3. handler 返回 ToolFailure → tool-error
//   4. 非 ToolFailure 缺陷 → 失败整个流
// provider 定义工具(Anthropic web_search 等):providerExecuted=true → 调用方跳过本地 dispatch
```

### 5. 时间序 system 消息(设计 5:权威降级)

```ts
// AGENTS.md "Chronological System Updates":
Message.system(...) = provider 中立的时间序操作符(历史中从该位置起生效)
// Anthropic 原生支持(claude-opus-4-8);其他 route 降级为 <system-update>...</system-update> 包装的用户文本
// ——"wrapped-user fallback preserves ordering while visibly lowering authority"
// 绝不静默透传 raw role: "system" 给可能拒绝的 route;不把检索文档/工具输出放进特权 system 更新
```

**产品启示**:产品①对齐的"时间序上下文变更"(Context Epoch 的 Mid-Conversation System Message 到 LLM 请求时)就是走这条降级通道。

### 6. 录制测试(设计 6:cassette + 回放)

```ts
// AGENTS.md "Recording Tests":
cassette = [request, response] 有序数组(多步流一个文件)
RECORD=true 录制(需真实 API key);默认回放
// 匹配:顺序游标 + method/URL/allow-list 头/JSON body 校验
// 二进制响应 base64(保 AWS event-stream 帧);RECORDED_PROVIDER/PREFIX/TAGS/TEST 过滤
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Route 四轴(Protocol/Endpoint/Auth/Framing) | route/* + AGENTS.md | ②供应商接入成本 |
| 2 | request/stream/generate 三入口 + 执行流程 | llm.ts:53-75 | ②单 provider turn |
| 3 | generateObject 强制合成工具(全协议统一) | llm.ts:80-144 | ③验收器结构化输出 |
| 4 | ToolRuntime 单次调用 + 错误三径 + providerExecuted | tool-runtime.ts | ②工具调度 |
| 5 | 时间序 system 降级(<system-update> 包装) | AGENTS.md | ①对齐上下文变更通道 |
| 6 | 录制测试(cassette + 回放 + 过滤) | test/recorded-* | ③测试基建 |

## 面试弹药

- "四轴解耦是供应商接入成本的答案":复用 OpenAIChat.protocol 的 provider 是 5-15 行 Route.make;协议 bug fix 一次传播全族
- "generateObject 不用 provider JSON mode":强制合成工具调用,行为全协议统一——结构化输出的通用方案
- "工具循环在包外":LLM 包只做单次 turn + dispatch,持久化/续跑/计步是会话层的职责——关注点分离
- "时间序 system 降级":不支持原生 chrono system 的 route 用 <system-update> 包装降权——不静默透传,降权可见
- "录制测试保二进制":AWS event-stream 帧 base64 存储,回放逐字节一致

## 待深挖

- [ ] route/client.ts 的 prepare/stream/generate 细节
- [ ] executor.ts 的传输错误映射
- [ ] MCP 工具 → LLM 工具定义的桥接(mcp 域)
