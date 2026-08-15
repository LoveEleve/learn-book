# q6 — Compaction(深度版:双触发 + 摘要模板 + 序列化安全 + 测试契约)

> 域:②执行契约(上下文压缩) | 文件:core/src/session/compaction.ts(247)+ core/test/session-compaction.test.ts(47,3 契约)+ session-runner.test.ts(压缩契约段 1085-1360)+ specs/v2/session.md
> review 轮次:2 轮(源码全文 + 全部测试契约)

---

## 假设

压缩 = 把完整持久化会话换成"一个隐藏 checkpoint(结构化滚动摘要 + token 受限近期上下文)",模型表示替换,持久化不动。双触发(预估 + 溢出),失败绝不产生中间态。

## 验证

### 1. 双触发(设计 1:compactIfNeeded 预估 + compactAfterOverflow 溢出)

```ts
// compaction.ts:231-242 compactIfNeeded
预算 = estimate({system, messages, tools}) > context - max(output, buffer)  → 压缩
// compaction.ts:178-230 compactAfterOverflow:独立压缩调用
//  - 校验 summaryPrompt 能放进 context - summaryOutput
//  - 发布 Compaction.Started → 流式摘要 → Compaction.Ended(text, recent)
//  - 失败(provider error/空摘要)→ 返回 false(不中断主流程)
```

**默认值**(compaction.ts:12-15):buffer 20000 / keep tokens 8000 / 摘要输出 4096 / 工具输出截断 2000。

### 2. 摘要模板(设计 2:7 段 + 合并规则)

```ts
// compaction.ts:16-46 SUMMARY_TEMPLATE
## Objective / ## Important Details / ## Work State(Completed/Active/Blocked)/ ## Next Move(1. 2.)/ ## Relevant Files
// 规则:保留全部段(空段也保留)/ terse bullets / 保留精确路径/命令/错误串 / 不提压缩过程
// 更新指令(compaction.ts:47-55):prior-summary + conversation 合并——冲突 conversation 胜;Active→Completed;丢弃过期声明
```

**测试证据**(session-compaction.test.ts:4-31):
- 首次摘要:含 "Create a new anchored summary" + Work State 三段 + Relevant Files
- prior-summary 更新:顺序 conversation → prior-summary → 更新指令;含 "Carry forward objectives, constraints, user directives..."、"Move completed work from Active to Completed"

### 3. head/recent 切割(设计 3)

```ts
// compaction.ts:137-158 select:从最新往回累加 token 直到超 keepTokens(8000)
// head = 摘要输入;recent = 保留原文(序列化进 checkpoint)
```

**runner 契约证据**(session-runner.test.ts):
- 1148 "retains only complete serialized messages during compaction":EARLIER 进摘要,RECENT 保留原文;continuation 含 "<recent-context>\n[Assistant]: Earlier answer"
- 1180 "summarizes an oversized newest message without retaining a fragment":超大新消息全进摘要,recent-context 为空
- 1085 "automatically compacts into a completed summary and retained recent turn":第二次压缩带 <prior-summary>;上下文 = [compaction, assistant]

### 4. 序列化安全(设计 4:媒体不嵌入 base64)

```ts
// compaction.ts:88-93 serializeToolContent:text 保留 / file → `[Attached mime: name]`
// session-compaction.test.ts:33-47:"compaction describes tool media without embedding base64"
```

**设计要点**:工具结果里的 base64 媒体只描述不嵌入——压缩 prompt 体积防爆(旧版没挖到的点)。

### 5. 消息序列化(设计 5:serialize 五类型)

```ts
// compaction.ts:95-121
user:[User]: text + [Attached mime: name]
assistant:[Assistant]: text / [Assistant reasoning]: / [Assistant tool call]: name(input) / [Tool result]: truncate(2000)
system:[System update]: / synthetic / shell:[Shell]: command\noutput
```

### 6. 事件契约(设计 6:started=attempt / ended=生效)

specs/v2/session.md:117 + session-event.ts:398-432:
- Compaction.Started(durable, reason: auto|manual)= attempt
- Compaction.Delta(live-only)= 进度
- Compaction.Ended(durable, text, recent)= 完成;仅此事件投影模型可见压缩消息

**失败/中断 → 旧历史边界保持生效**(无损坏中间态)。

### 7. 溢出恢复(设计 7:一次二次,绝不循环)

**runner 契约证据**:
- 1209 "forces one compaction and retries after provider context overflow":3 请求(overflow → summary → final),上下文 = [compaction, assistant finish: stop],重放一致
- 1238 "persists a second context overflow after one recovery":二次溢出 → 终止失败
- 1261 "recovers once from a raw context overflow failure"
- 1289 "publishes the original overflow when recovery summarization fails"
- 1309 "interrupts overflow recovery while the summary provider is running"
- 1334 "preserves effective System updates while compaction rebaseline is blocked"

### 8. Epoch 联动(设计 8:压缩完成 = 新 Context Epoch)

- 1046 "rebuilds the baseline directly after completed compaction":Compaction.Ended(seq > baseline_seq)→ 下个 turn 全新 baseline
- context-epoch.ts:59-62:replacementSeq = compaction.seq > baseline_seq ? compaction.seq : undefined

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 双触发(预估 + 溢出) | compaction.ts:231-242,178-230 | ②上下文压力管理 |
| 2 | 7 段摘要模板 + 合并规则 | compaction.ts:16-55 | ④跨 session 交接摘要模板 |
| 3 | head/recent 切割(token 预算 8000) | compaction.ts:137-158 | ②压缩保留策略 |
| 4 | 序列化安全(媒体只描述不嵌入) | compaction.ts:88-93 + 测试 | ②上下文体积控制 |
| 5 | 消息序列化五类型 + 工具结果截断 | compaction.ts:95-121 | ②输出边界 |
| 6 | started=attempt / ended=生效 | session-event.ts:398-432 | ④事件契约 |
| 7 | 溢出一次恢复,绝不循环 | 测试 1209-1334 | ②失败循环(D17) |
| 8 | 压缩完成 → 新 Epoch | context-epoch.ts:59-62 | ②基线重建 |

## 面试弹药

- "压缩 = 替换模型表示,不动持久化":checkpoint 只是模型视图,完整转写永远保留
- "7 段摘要模板":Objective/Details/Work State(Completed/Active/Blocked)/Next Move/Relevant Files——给另一个 agent 续跑的规格书
- "媒体只描述不嵌入":base64 图片在压缩里变 `[Attached image/png: name]`——上下文体积控制
- "溢出压缩只一次":二次 overflow 就是终止失败——防循环边界
- "合并规则:conversation 胜 + Active→Completed":摘要更新像版本合并

## 待深挖

- [ ] Token.estimate 的实现(util/token.ts)——与 provider tokenizer 的关系
- [ ] V1 compaction.ts(608 行)对比 V2(新 checkpoint 设计)的演进
