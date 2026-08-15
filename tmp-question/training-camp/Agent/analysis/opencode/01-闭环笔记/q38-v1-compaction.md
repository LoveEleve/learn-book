# q38 — V1 压缩(深度版:select/prune/process 三件套 + overflow 重放)

> 域:②参考(V1 压缩) | 文件:opencode/src/session/compaction.ts(608)+ overflow.ts(31)+ 对比 core/session/compaction.ts(V2,247)
> review 轮次:2 轮(源码全文)

---

## 假设

V1 压缩 = 三件套:select(尾部保留预算切割)、prune(旧工具输出擦除)、process(压缩 agent 生成摘要 + overflow 重放)。它直接写 V1 存储(updateMessage/updatePart),与 V2 的事件驱动 checkpoint 是两代设计。

## 验证

### 1. select(设计 1:tail 预算切割)

```ts
// compaction.ts:223-269
limit = cfg.compaction.tail_turns(>0 时限制保留轮数)
budget = preserveRecentBudget = cfg.preserve_recent_tokens ?? clamp(usable×0.25, 2k, 15k)
从最新轮往前累加 token,超预算:
  → splitTurn(当前轮内部切割:从后往前找能放进剩余预算的起点)
  → 失败且无 keep → tail fallback(日志)
keep.start === 0 → 全保留;否则 head/tail_start_id
// 惰性估算:只估 tail 相关的轮,成本与保留尾部成正比(compaction.ts:239 注释)
```

### 2. prune(设计 2:工具输出擦除)

```ts
// compaction.ts:271-317
prune = cfg.compaction.prune 开关
从最新往回扫描(跳过最近 2 轮用户消息):
- 遇 summary 消息 → 停(保护已压缩区)
- 已 compacted 工具 → 停
- 保护 skill 工具(PRUNE_PROTECTED_TOOLS)
- 累加工具输出 token;total > PRUNE_PROTECT(40k)→ 开始收集
- pruned > PRUNE_MINIMUM(20k)→ 擦除(time.compacted = now + updatePart)
// 效果:旧工具输出被清空(序列化时显示 "[Old tool result content cleared]")
```

**对比 V2**:V1 是"擦除已提交输出"(破坏性,但标记可审计);V2 checkpoint 是"替换模型视图,持久化不动"(非破坏性)。

### 3. process(设计 3:compaction agent + 插件钩子)

```ts
// compaction.ts:319-459
1. overflow 时找前一用户消息构造 replay(媒体降级为文本)
2. 专用 compaction agent(agents.get("compaction"))+ 独立模型
3. 隐藏历史摘要消息(hidden set,只留最后一次 summary)
4. select(head)→ serialize → buildPrompt(复用 core 的模板!compaction.ts:384)
5. 插件钩子:experimental.session.compacting(注入 context/prompt)+ chat.messages.transform(改消息)
6. 创建 summary assistant 消息(summary: true, mode: "compaction")+ processor 处理
7. result === "compact"(摘要也溢出)→ 错误终止
8. auto + continue + replay → 重放用户消息(媒体 → "[Attached mime: name]" 文本)
```

### 4. overflow(设计 4:预算)

```ts
// overflow.ts:8-31(与 q12 一致):usable = limit - reserved(compaction.buffer 20k vs maxOutputTokens)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | select(tail 预算 + splitTurn 轮内切割) | compaction.ts:223-269 | ②保留策略(产品可抄预算公式) |
| 2 | prune(工具输出擦除,40k 保护/20k 触发) | compaction.ts:271-317 | ②上下文压缩(破坏性,慎用) |
| 3 | process(compaction agent + 插件钩子) | compaction.ts:319-459 | ②摘要管线(插件扩展点) |
| 4 | overflow 重放(媒体降级) | compaction.ts:340-356 | ②溢出恢复 |
| 5 | 复用 core buildPrompt(模板单一来源) | compaction.ts:384 | ④模板共享 |

## 面试弹药

- "预算公式可抄":preserve_recent = clamp(usable×25%, 2k, 15k)——保留尾部预算的自适应
- "prune 是破坏性擦除":V2 用 checkpoint 替换视图,V1 直接清旧输出(带 time.compacted 标记)——两代哲学
- "compaction agent 独立模型":摘要用专用 agent 配置(可与会话模型不同)
- "插件可注入摘要上下文":experimental.session.compacting 钩子——摘要 prompt 可扩展

## 待深挖

- [ ] buildPrompt 的 V1/V2 共享(core 模板)
- [ ] completedCompactions 的摘要链(prior summary)
- [ ] serialize 的 V1/V2 差异(attachments vs content)
