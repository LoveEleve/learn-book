# q19 — LLM 适配器细节(深度版:DeepSeek 协议 + 错误映射 + Pi-AI 回放)

> 域:②底层(协议适配) | 文件:packages/llm/(llm-deepseek/src:adapter 158+/translate 53+/serialize 151+/sse)+(llm-pi-ai/src:adapter/catalog/config/context/discovery/provider/replay/stream)
> review 轮次:2 轮(源码全文核心)

---

## 假设

DeepSeek 适配器 = 第一个真实 LlmAdapter:错误码映射(HTTP→LlmError code)、空闲看门狗、推理努力档位。Pi-AI 适配器 = 复用 @earendil-works/pi-ai(跨项目依赖)+ 回放状态。

## 验证

### 1. DeepSeek 默认值(设计 1:预算常量)

```ts
// adapter.ts:89-93:
DEFAULT_STREAM_IDLE_TIMEOUT_MS = 300_000(流空闲看门狗)
DEFAULT_CONTEXT_WINDOW = 1_000_000(上下文容量)
DEFAULT_MAX_TOKENS = 256_000(输出上限)
STREAM_IDLE_TIMEOUT_CODE = 'LLM_STREAM_IDLE_TIMEOUT'
// 推理努力:off/high/max 三档(REASONING_EFFORTS 元数据)
// "One instance serves every model name it was registered under
//  (the harness model name IS the wire model name)"
```

### 2. 错误映射(设计 2:HTTP → 稳定码)

```ts
// adapter.ts:138-155:
httpErrorCode(status, error):
  401/403 → 'AUTH'
  配额 → QUOTA_EXCEEDED_CODE
  429 → 'RATE_LIMIT'
  400 → CONTEXT_WINDOW_EXCEEDED_CODE(上下文超)或 'INVALID_REQUEST'
  >=500 → 'SERVER'
  其他 → `HTTP_${status}`
// providerRetryAfterMs:retry-after 头(秒数或日期格式)
// requestId:x-request-id / x-deepseek-request-id
// 空闲看门狗:"One stable signal reaches both initial fetch and body reads.
//   Caller aborts map to ABORTED;the configured per-read idle watchdog maps to TIMEOUT"
```

### 3. 翻译/序列化(设计 3:wire 面)

```ts
// translate.ts:mapFinishReason(reason)/ mapUsage(usage)→ TokenUsage
// serialize.ts:serializeMessages(Message → WireMessage)/ serializeRequest(请求序列化)
// sse.ts:SSE 解析(流式)
```

### 4. Pi-AI 适配器(设计 4:跨项目 + 回放)

```ts
// llm-pi-ai:adapter/catalog/config/context/discovery/provider/replay/stream
// PiAiReplayState(toPiReplayState(AssistantMessage)):回放状态(录制重放)
// toPiAssistant(Message):消息转换
// ——与 pi 项目共享 @earendil-works/pi-ai(生态互操作)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 预算常量(空闲 300s/窗口 1M/输出 256k) | adapter.ts:89-93 | ②底层 |
| 2 | HTTP→稳定错误码映射 | adapter.ts:138-155 | ③错误契约 |
| 3 | 翻译/序列化/SSE | translate/serialize/sse | ②协议适配 |
| 4 | Pi-AI 回放状态 | llm-pi-ai/replay | ②录制重放 |

## 面试弹药

- "错误码是稳定契约":AUTH/RATE_LIMIT/CONTEXT_WINDOW_EXCEEDED/SERVER——重试策略按码决策
- "空闲看门狗是每读":单信号到初始 fetch + body 读——abort → ABORTED,空闲 → TIMEOUT
- "推理努力三档元数据":off/high/max——模型能力由目录描述
- "一个实例服务所有模型名":harness 模型名 = wire 模型名——注册简单

## 待深挖

- [ ] pi-ai 的 discovery/provider(模型发现)
- [ ] sse.ts 的流解析细节
- [ ] catalog 的模型目录
