# q5 — LLM 能力缝(深度版:llm/stream 瀑布 + 冻结请求 + 错误分类)

> 域:②底层(模型能力) | 文件:packages/llm/(llm/src 947+256/llm-deepseek/adapter.ts/llm-pi-ai/adapter.ts/llm-retry/history.ts)+ docs/subsystems/llm-streaming.md
> review 轮次:2 轮(源码全文核心)

---

## 假设

LLM = 能力缝:Service Definition(llm/src)+ Provider(deepseek/pi-ai 双适配器)+ Consumer(agent-loop)。核心 = llm/stream 瀑布(重试/重放/路由挂点)+ 冻结请求(loop 构建的请求是 session log 的纯函数)。

## 验证

### 1. llm/stream 瀑布(设计 1:每调用可拦截)

```ts
// llm/src/index.ts:56-70:
'llm/stream'(waterfall): 每个流式模型调用
  next() → 解析的 adapter 流;或 yield 自己的 chunk 短路
  LOOP 构建的请求:markAgentLoopRequest 进程本地身份 + deep-frozen(突变抛错)
    ——"its content is a pure function of the session log"(可重建性 Agent Note)
  hand-built 调用无标记;消息已遵守不可变创建契约
// stream() 实现:ctx.waterfall(this, 'llm/stream', options, () => this.adapterStream(options))
```

**产品启示**:①对齐模块的模型调用、②验收器的独立审查调用都可挂 llm/stream(重试/路由/录制)——能力缝的可拦截性是架构级的。

### 2. 错误分类(设计 2:LlmError)

```ts
// llm/src/index.ts:83-163:
LlmError extends HarnessError:code 字符串共享分类(AUTH/RATE_LIMIT/NO_ADAPTER...)
options:status(100-599 校验)/providerRetryAfterMs(正整数)/requestId(非空)
failure = Object.freeze({ message, code, ... })——可序列化事实,与 live Error 并存
// 适配器 throw → adapterFailureChunk:terminal finish chunk
//   reason: aborted(signal.aborted 或 code === 'ABORTED')| error
// 中间件/嵌套调用/清理/消费者失败保持 thrown(不被吞)
```

### 3. 适配器注册(设计 3:AdapterRegistration)

```ts
// llm/src/index.ts:239-283:
AdapterRegistration = { adapter: LlmAdapter, provider: LlmProviderInfo, retryPolicy: ResolvedRetryPolicy }
DirectoryRegistrationHandle:目录注册
// llm-deepseek:DeepSeek 适配器(adapter.ts + sse.ts + serialize.ts + translate.ts)
// llm-pi-ai:Pi-AI 适配器(adapter/catalog/config/context/discovery/provider/replay/stream)
//   ——与 pi 项目共享 @earendil-works/pi-ai(跨项目依赖)
// llm-retry:重试历史(history.ts,基于 LlmFailure 分类)
```

### 4. 消息与组装(设计 4:词汇)

```ts
// message.ts(261):UserMessage/AssistantMessage/ToolResultMessage
// assembler.ts(164):BlockAssembler(块组装)
// retry-policy.ts(191):重试策略
// call-config.ts(117):LlmCallConfig + AdapterDefaults
// attribution.ts(68):归因
// adapter-failure.ts(104):适配器失败归一化
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | llm/stream 瀑布(重试/重放/路由) | llm/src/index.ts:56-70 | ②模型调用拦截 |
| 2 | 冻结请求(session log 纯函数) | index.ts:56-70 | ④可重建性 |
| 3 | LlmError 分类(AUTH/RATE_LIMIT/NO_ADAPTER) | index.ts:83-163 | ③错误契约 |
| 4 | 双适配器(deepseek/pi-ai)+ 目录注册 | llm-deepseek + llm-pi-ai | ②供应商抽象 |
| 5 | 消息词汇 + 重试策略 | message/assembler/retry-policy | ②底层 |

## 面试弹药

- "loop 请求 deep-frozen":突变抛错——请求内容 = session log 纯函数(可重建性强制)
- "失败分类在错误码层":AUTH/RATE_LIMIT/NO_ADAPTER 共享分类 + failure 冻结事实——重试策略按码决策
- "适配器失败 → terminal finish chunk":不抛给消费者——流协议内表达终态
- "llm-pi-ai 与 pi 项目共享":跨项目依赖(@earendil-works/pi-ai)——harness 生态互操作性实证

## 待深挖

- [ ] llm-deepseek 的 sse/serialize(协议适配)
- [ ] llm-pi-ai 的 replay(录制重放)
- [ ] retry-policy 的具体策略
