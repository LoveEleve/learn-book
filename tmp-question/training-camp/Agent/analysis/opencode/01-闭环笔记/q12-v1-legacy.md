# q12 — V1 Legacy SessionPrompt(深度版:处理器链 + 重试 + 溢出)

> 域:②参考(过渡架构) | 文件:opencode/src/session/(prompt.ts 1631/processor.ts 718/retry.ts/overflow.ts/llm.ts 404)+ opencode/test/session/*.test.ts
> review 轮次:2 轮(核心循环 + 处理器链 + 重试/溢出策略)

---

## 假设

V1 = "内存循环 + 事件发布"的单体,处理器链(processor/retry/overflow/compaction)三态驱动主循环。它正在被 V2 替换(AGENTS.md 禁止 bridge legacy loop),但 V1 的重试策略/溢出策略/结构化输出仍有学习价值,且是"渐进迁移"的实证。

## 验证

### 1. 循环结构(设计 1:while(true) + processor 三态)

```ts
// prompt.ts:1092-1096 runLoop
while (true) {
  status.set(busy) → 组装 → llm → processor 处理 → 按 Result 决策
}
// processor.ts:677-683 返回三态:
needsCompaction → "compact" / blocked 或 error → "stop" / 否则 "continue"
```

### 2. 处理器链(设计 2:process 管线)

```ts
// processor.ts:628-677
process(streamInput):
1. llm.stream(streamInput)
2. Stream.tap(handleEvent) + takeUntil(needsCompaction) + runDrain
3. onInterrupt → aborted + halt(AbortError)
4. catchCauseIf(非纯中断)→ fail
5. Effect.retry(SessionRetry.policy)(重试策略)
6. catch(halt) + ensuring(cleanup)
```

### 3. 重试策略(设计 3:指数退避 + jitter)

```ts
// retry.ts:26-31
RETRY_INITIAL_DELAY 2000ms / BACKOFF_FACTOR 2 / JITTER_FACTOR 0.25 / MAX_DELAY_NO_HEADERS 30s / MAX_RETRIES 5
// delay(attempt, error, random):按 responseHeaders 的 retry-after 优先,否则退避 + 抖动
// RETRYABLE_MESSAGE_PATTERNS:可重试错误模式匹配
// GO_UPSELL:免费额度用尽 → 引导订阅
```

### 4. 溢出策略(设计 4:usable 预算)

```ts
// overflow.ts:8-31
COMPACTION_BUFFER 20000
usable = limit.input ? input - reserved : context - maxOutputTokens
reserved = compaction.reserved ?? min(20000, maxOutputTokens)
isOverflow:tokens.total >= usable 且 auto 开启
```

### 5. 压缩联动(设计 5:overflow → needsCompaction)

```ts
// processor.ts:605-617
ContextOverflowError:
- auto=false 且无 summary → 直接 error(不压缩)
- 否则 needsCompaction = true → 主循环走 compact 分支
```

### 6. 结构化输出(设计 6:强制工具)

```ts
// prompt.ts:74-82
STRUCTURED_OUTPUT_DESCRIPTION:
"Use this tool to return your final response in the requested structured format.
 - You MUST call this tool exactly once at the end of your response
 - Complete all necessary research and tool calls BEFORE calling this tool"
// 模型用工具调用表达最终答案,执行后循环停止——工具调用即结构化协议
```

### 7. deny 处理(设计 7:blocked 语义)

```ts
// processor.ts:201,632-633
ctx.blocked = ctx.shouldBreak(权限 deny 时)
shouldBreak = config.experimental.continue_loop_on_deny !== true
// blocked → "stop"(除非实验开关继续循环)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | while(true) + processor 三态 | prompt.ts:1092-1096 + processor.ts:677-683 | ②反模式(对比 V2) |
| 2 | process 管线(tap/takeUntil/retry/halt/cleanup) | processor.ts:628-677 | ②失败循环参考 |
| 3 | 指数退避 + jitter + retry-after | retry.ts:26-31 | ②重试策略(产品可抄) |
| 4 | usable 预算(预留输出/压缩 buffer) | overflow.ts:8-31 | ②上下文预算 |
| 5 | overflow → needsCompaction 联动 | processor.ts:605-617 | ②溢出恢复 |
| 6 | 结构化输出 = 强制工具 | prompt.ts:74-82 | ③验收器结构化输出蓝本 |
| 7 | deny → blocked → stop(实验开关) | processor.ts:201,632-633 | ②权限失败语义 |

## 面试弹药

- "处理器三态":compact/stop/continue——循环控制的最小协议(概念保留到 V2 的 needsContinuation)
- "重试策略参数化":初始 2s × 2 退避 + 25% 抖动 + retry-after 头优先 + 最多 5 次——生产级重试(产品③可直接抄)
- "usable = limit - 预留":上下文预算考虑输出预留 + 压缩 buffer——V2 的 compactIfNeeded 继承此思想
- "结构化输出 = 工具调用约束":不用 constrained sampling,强制工具调用一次——工具调用协议本身就是结构化
- "渐进迁移":V1 保留 + V2 shadow bridge——AGENTS.md 明确禁止 bridge legacy loop(防双真相)

## 待深挖

- [ ] V1 session.ts(1018 行)消息模型(message/part 两表)
- [ ] V1 压缩(compaction.ts 608)的分支摘要 vs V2 checkpoint
- [ ] message-v2.ts 的 V1→V2 消息转换
