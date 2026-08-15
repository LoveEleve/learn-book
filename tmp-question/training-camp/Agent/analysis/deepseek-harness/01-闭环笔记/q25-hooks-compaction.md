# q25 — Hook 协议 + Compaction-Basic(深度版:合并语义 + 摘要)

> 域:②执行(外部桥)+ ②上下文(压缩) | 文件:packages/(hooks/hook-protocol/src:codec/matcher/merge/detached/events)+(compaction/compaction-basic/src:config/region/summarizer/types)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Hook 协议 = 外部 hook 输出的解码/匹配/合并(权限决策合并是核心:deny>ask>allow)。Compaction-Basic = 压缩 provider(区域选择 + 摘要)。

## 验证

### 1. 合并语义(设计 1:deny > ask > allow)

```ts
// hooks/hook-protocol/src/merge.ts:15-62:
MergedHookOutcome = {
  decision: 最严格权限决策(deny > ask > allow;无 = none)
    block/deny 都折为 deny;approve/allow 都折为 allow
  reason: 所有 blocking/denying hook 的理由(joined \n\n)
  stop: 任何 hook 要求 halt(continue:false)
  stopReason: 第一个 halt hook 的
  additionalContext: 每 hook 的(按 hook 序,不 join——桥决定)
  systemMessages: 每 hook 的(按序)
}
rank:deny/block=3 > ask=2 > approve/allow=1 > none=0
mergeHookOutputs:折叠所有匹配 hook 的输出(按 hook 序);空列表 → 中性(决策 none,无 stop)
  ——"the caller treats that as no hook had anything to say"
```

**产品启示**:②验收器的多审查器合并 = 同一语义(最严格胜 + 理由聚合 + stop 传播)。与 Reasonix 的审查器合并思想一致。

### 2. 解码/匹配(设计 2:codec + matcher)

```ts
// codec.ts:parseHookOutput(exitCode, stdout, stderr, expectedEventName?):
//   expectedEventName 不同 → 畸形,丢弃事件作用域字段(q18 已提)
// matcher.ts:matchesMatcher(matcher, query, mode)/ matcherDiagnostic(匹配器诊断)
// events.ts:HookInvocation/HookResultRecord + DEFAULT_STDERR_SUMMARY_MAX_CHARS(500)
//   summarizeStderr(stderr, maxChars)(stderr 摘要)
// detached.ts:createDetachedRuns()(分离运行管理)
```

### 3. Compaction-Basic(设计 3:区域 + 摘要)

```ts
// compaction-basic:config/region/summarizer/types
// region.ts:压缩区域选择(哪些消息进摘要)
// summarizer.ts:摘要器(LLM 调用)
// ——basic provider(compaction 缝的最简实现)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 合并语义(deny>ask>allow + stop) | hooks/hook-protocol/merge.ts | ③多审查器合并 |
| 2 | 解码/匹配/摘要 | codec/matcher/events | ②外部协议 |
| 3 | Compaction-Basic(区域+摘要) | compaction-basic | ②压缩实现 |

## 面试弹药

- "最严格决策合并":deny > ask > allow——多 hook 的权限决策单一化
- "空列表 = 中性":无 hook 说话 = 决策 none——调用方按"没人有话"处理
- "additionalContext 不 join":桥决定如何用——合并层只聚合不解读
- "stderr 摘要上限 500":stdout/stderr 摘要化——事件记录紧凑

## 待深挖

- [ ] region.ts 的区域选择算法
- [ ] summarizer 的摘要调用
- [ ] matcher 的匹配模式(glob/正则?)
