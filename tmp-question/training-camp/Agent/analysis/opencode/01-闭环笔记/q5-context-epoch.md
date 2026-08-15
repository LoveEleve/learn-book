# q5 — Context Epoch(深度版:基线生命周期 + 时间序更新 + 测试契约)

> 域:①对齐 + ②执行(上下文) | 文件:core/src/session/context-epoch.ts(174)+ system-context/(q2 已深挖)+ core/test/session-runner.test.ts(epoch 契约段 748-1083)+ session-compaction.test.ts
> review 轮次:2 轮(源码全文 + runner 测试契约)

---

## 假设

Epoch = "一次完整 Baseline 的生存期"。变化不进 baseline,而是追加为**持久化的时间序 System 消息**(Mid-Conversation System Message);只有压缩/搬家才重建 baseline。这样 provider prompt cache 前缀保持稳定。

## 验证

### 1. Epoch 生命周期(设计 1:四阶段)

```ts
// context-epoch.ts:40-78 prepareOnce
1. [context, stored, compaction] 并发加载
2. 无 stored → SystemContext.initialize → insert(baseline, snapshot, baseline_seq)
3. 有 stored + compaction.seq > baseline_seq → SystemContext.replace(新世代)
4. 有 stored → reconcile → 四态:
   - Unchanged/ReplacementBlocked → 保留原 baseline
   - ReplacementReady → replace + baselineSeq = latestSequence(或 compaction.seq)
   - Updated → publish(ContextUpdated, { commit: () => advance(snapshot) })   ← 事件+快照原子
```

### 2. 基线不可变(设计 2:变化 = 时间序消息,不碰 baseline)

**测试证据**(748 "reuses one durable baseline after the context producer changes"):
- system 请求两次都是 ["Initial context"]——**baseline 不变**
- 消息序列 = [user, user, system "Changed context"]——变化以时间序 System 消息追加
- `session.next.context.updated.1` 事件落库(1 条)
- 重放投影后消息数仍为 3(持久化)

**产品启示**:②上下文基线管理 = 变化增量进历史,基线稳定(provider 前缀缓存命中)。产品对齐模块改参数时,差异走"时间序更新"而不是重写系统提示词。

### 3. 移除语义(设计 3:removal 也是时间序消息)

**测试证据**(948 "admits removed context as a chronological System message"):
- 源消失 → 消息文本 = "System context source removed: test/context"
- 消息序列 = [user, user, system](移除也是一条 system 消息)

### 4. 模型/agent 切换保留基线(设计 4:切换 ≠ 重建)

**测试证据**(969 "keeps the baseline and chronological System updates after a model switch"):
- 模型切换 → baseline 仍 ["Initial context"] ×3
- 时间序 system 消息累积(2 条)+ "model-switched" 类型消息
- 上下文序列 = [user, user, system, model-switched, user, system]
- 重放后消息数 = 6(全部持久化)

**测试证据**(859 "updates selected-agent skill guidance after an agent switch"):
- agent 切换 → skill guidance 变化 → 时间序 system 消息(含 "Reviewer skills")
- baseline 不变("Initial context\n\nBuild skills" 保持)

### 5. Unavailable 语义(设计 5:保留基线,不发更新)

**测试证据**(1014 "preserves the baseline while context is temporarily unavailable"):
- 模型切换 + context unavailable → 请求 system 仍 ["Initial context"] ×3
- 恢复后也不重建(直到需要新世代)

### 6. 压缩 = 新世代(设计 6:replacement)

**测试证据**(1046 "rebuilds the baseline directly after completed compaction"):
- 手动 publish Compaction.Started + Compaction.Ended(seq > baseline_seq)
- 下个 turn → system = ["Replacement context"](全新 baseline)

### 7. 初始化阻塞(设计 7:首个 prompt 前必须完整 baseline)

**测试证据**(658 "retries the first provider turn after system context becomes available"):
- 初始上下文 unavailable → InitializationBlocked
- 无 epoch 行、无请求发出、**收件箱保留 pending**
- 恢复 → 同 messageID 重试成功

**specs/v2/session.md:58**: "The first complete observation initializes the epoch before any pending prompt becomes model-visible."

### 8. agent system 组合顺序(设计 8:agent system 在 durable context 之前)

**测试证据**(782/803/830):system = [agent.system, baseline]——agent 指令在前,durable 上下文在后。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Epoch 四阶段(initialize/reconcile/replace/advance) | context-epoch.ts:40-78 | ②上下文基线管理 |
| 2 | 基线不可变,变化 = 时间序 System 消息 | 测试 748 | ②prompt cache 稳定前缀 |
| 3 | 移除 = 时间序消息 | 测试 948 | ①配置变更处理 |
| 4 | 模型/agent 切换保留基线 | 测试 969/859 | ②切换不重建 |
| 5 | Unavailable 保留基线不发更新 | 测试 1014 | ①失败语义 |
| 6 | 压缩完成 = 新世代(ReplacementReady) | 测试 1046 | ②压缩联动 |
| 7 | 初始化阻塞 + 收件箱保留 | 测试 658 | ①对齐中断处理 |
| 8 | system = [agent.system, baseline] | 测试 782-857 | ②请求组装顺序 |

## 面试弹药

- "变化不改基线,只追加时间序消息":prompt cache 前缀稳定(成本)+ 变化可审计(事件)
- "切换 ≠ 重建":agent/model 切换保留 epoch,只有压缩/搬家重建——避免"每次切换丢缓存"
- "第一个 prompt 前必须完整基线":unavailable 就阻塞,绝不带不完整上下文跑——对齐先于执行
- "commit 回调原子推进 snapshot":ContextUpdated 事件与快照更新同事务(EventV2 协议)

## 待深挖

- [ ] move-session.test.ts 的搬家语义(epoch reset 细节)
- [ ] skill guidance 在 agent switch 时如何成为 Context Source(skill/guidance.ts,已读,可并入 q14)
