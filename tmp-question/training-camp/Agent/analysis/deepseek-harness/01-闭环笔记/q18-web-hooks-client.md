# q18 — Web 能力 + Hooks 桥 + Client 连接(深度版:外围执行面)

> 域:②执行(外围) | 文件:packages/(web/web + web-search-* + web-fetch-http/policy 98+provider)+(hooks/hook-protocol/runner.ts 150+ + claude-code + codex)+(client/connection:rpc/rpc-host/websocket-downlink/http-bridge/loopback-hostname/api-request-trust)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Web = 能力缝(search/fetch providers + 政策);Hooks = 外部工具 hook 执行器(超时/解码/永不 throw);Client = Web 客户端连接层(JSON-RPC 传输 + WebSocket + loopback 安全)。

## 验证

### 1. Web Fetch 政策(设计 1:URL 验证)

```ts
// web/web-fetch-http/src/policy.ts:24-98:
validateFetchUrl(input, maxUrlLength):URL 验证(长度上限)
isSameOrigin(a, b):同源检查
classifyContentType(contentType):FetchableKind 分类
parseCharset + decoderForCharset:字符集解码
// provider.ts:fetch provider 实现
// web 缝:search(deepseek/exa/perplexity)+ fetch(http)——Consumer = tool-web
```

### 2. Hook Runner(设计 2:外部 hook 执行)

```ts
// hooks/hook-protocol/src/runner.ts:20-80:
DEFAULT_HOOK_TIMEOUT_MS = 600_000(10 分钟)
runHook(bash, hook, options, now):
  timeoutMs = hook.timeoutSec ? sec*1000 : defaultTimeoutMs
  stdin = JSON.stringify(payload) + trailingNewline('?')(CC 要换行,Codex 不要)
  signal:拥有操作的信号;触发 → 取消 hook 运行
  expectedEventName:hookSpecificOutput 的 hookEventName 不同 → 视为畸形,丢弃事件作用域字段
  "Infrastructure rejection becomes an outcome with no exit code,
   so this function never throws or crashes the calling turn"
// RunHookResult = { output, durationMs(wall-clock,hook/result 事件 durable) }
// trusted env 在 executor scrub 后合并
```

**设计要点**:hook 失败 = outcome 不是 throw(不崩溃 turn);duration 持久化(hook/result 审计)。

### 3. Client 连接(设计 3:传输层)

```ts
// client/connection:
rpc.ts + rpc-host.ts:JSON-RPC 客户端/宿主
websocket-downlink.ts:WebSocket 下行
http-bridge.ts:HTTP 桥
loopback-hostname.ts:回环主机名(安全)
api-request-trust.ts:API 请求信任(防跨站?)
api-path.ts:API 路径
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Web Fetch 政策(URL/同源/内容类型) | web-fetch-http/policy.ts | ②网络边界 |
| 2 | Hook Runner(超时/解码/永不 throw) | hooks/hook-protocol/runner.ts | ②外部执行 |
| 3 | Client 连接(JSON-RPC/WS/HTTP) | client/connection | ②客户端传输 |

## 面试弹药

- "hook 失败 = outcome 不是 throw":基础设施拒绝 → 无退出码的 outcome——绝不崩溃调用 turn
- "duration 持久化":hook/result 事件带 wall-clock——外部工具审计
- "expectedEventName 校验":hook 输出声称不同事件 → 丢弃事件字段(防畸形输出污染)
- "fetch 政策前置":URL 长度/同源/内容类型分类——网络边界在政策层

## 待深挖

- [ ] web 的 search providers(deepseek/exa/perplexity)
- [ ] hook-protocol 的 codec/matcher/merge
- [ ] client 的 loopback 安全细节
