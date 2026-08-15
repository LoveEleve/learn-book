# q21 — Inbox + Tool-Calls(深度版:可重放收件箱 + 调度器)

> 域:②执行引擎(核心细节) | 文件:packages/core/agent/src/inbox.ts(220)+ agent-loop/src/tool-calls.ts(289+)+ agent.ts
> review 轮次:2 轮(源码全文)

---

## 假设

Inbox = 双列表(next-turn/next-step)的**增量投影**(durable spliced 事件重放)。Tool-Calls = 调度器(exclusive 屏障/parallel 滚动池/abort 合成错误结果)。

## 验证

### 1. Inbox 投影(设计 1:replay-once 增量)

```ts
// inbox.ts:25-40:
class Inbox:state = { 'next-turn': [], 'next-step': [] }
构造:从 session.header.seedLength 起重放 agent/inbox/spliced 事件(持久化收件箱可重放!)
  无效 splice → throw("invalid persisted inbox splice at session seq N")
// getter:nextTurn/nextStep/hasPending
// 突变序(inbox.ts:158-193):validate → session.append('agent/inbox/spliced')(durable 先)
//   → live 投影后改 → 通知
//   "The durable event commits before the live projection mutates,so synchronous
//    session/event observers see the pre-splice lists and can reconstruct the
//    removed messages from the normalized coordinates"
```

**产品启示**:④收件箱可重放(与 OpenCode 的 session_input 同思想,但用"事件投影"而非表)——新 session 打开 = 重放 inbox 事件,无需单独表。

### 2. 突变操作(设计 2:六操作 + 归一化)

```ts
// inbox.ts:57-146:
claim(target, turn):next-step 全量 + (next-turn 时)next-turn 一个(0,1)——发布 claimed 通知
append/prepend:追加/前置(durable 记录)
replace(messageId, new):替换(旧 discarded + 新 inserted);非 pending → false
remove(messageId):移除(durable 取消)
clear():先清 next-step 再 next-turn(顺序)
splice:标准化(负索引/越界钳制/NaN 处理)+ validate(安全整数/范围/跨列表 ID 唯一)
// "next-step input followed by the queued turn,when requested"(claim 返回序)
```

### 3. Tool-Calls 调度器(设计 3:屏障 + 滚动池)

```ts
// tool-calls.ts:59+:
executeToolCalls:调度一个 assistant step 的工具调用
  "Exclusive calls form barriers;parallel calls use a bounded rolling pool
   and are reclassified before start.Dispatch may overlap,while policy,
   results,and result context remain model-ordered"
  完成/abort:已启动调用结果按序提交;abort 排干 + 未启动调用合成错误结果(重放有效)
  内部调度失败:停止新分派 + 排干已启动 + 首错拒绝(不伪造工具结果)
// PlannedCall{block, exec}:参数解析后待调度
// Slot{exec, result, needsPost}:等待模型序定稿
// GroupOutcome{consumed, aborted, concluded}:concludesTurn 检测
// appendSkippedToolCall:跳过调用合成(TOOL_ABORTED_BEFORE_DISPATCH)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Inbox 增量投影(replay-once) | inbox.ts:25-40 | ④收件箱可重放 |
| 2 | 六突变操作 + 归一化校验 | inbox.ts:57-146 | ②输入管理 |
| 3 | durable 先于 live(观察者见前状态) | inbox.ts:158-193 | ④一致性 |
| 4 | 调度器(屏障/滚动池/abort 合成) | tool-calls.ts:59+ | ②工具并行 |

## 面试弹药

- "收件箱 = 事件投影不是表":从 seedLength 重放 spliced 事件——无独立存储,重放即恢复
- "durable 先于 live":观察者见前状态可重建移除消息——事件溯源顺序纪律
- "claim 语义":next-step 全量 + next-turn 一个——步骤边界消费规则
- "abort 合成错误结果保重放有效":未启动调用 → TOOL_ABORTED_BEFORE_DISPATCH——日志不悬挂

## 待深挖

- [ ] tool-calls 的 runGroup(并行组)
- [ ] code-mode.ts(673,run_code 语义)
- [ ] agent-loop/index.ts 的 create/resume 生命周期
