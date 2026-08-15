# q4 — SessionInput 持久化收件箱(深度版:admit/promote 语义 + 22 测试契约)

> 域:②执行(收件箱)+ ④知识库(可重放) | 文件:core/src/session/input.ts(288)+ schema/src/session-input.ts + core/test/session-prompt.test.ts(584 行,22 契约)
> review 轮次:2 轮(源码全文 + 全部测试契约)

---

## 假设

"提示词"不直接进模型:先写入持久化收件箱(session_input 表,durable admission),由串行 runner 在安全边界提升(promote)。收件箱支持幂等重试(exact reuse)、冲突检测、并发安全,并且事件溯源可重放。

## 验证

### 1. 表结构(设计 1:双游标)

```sql
-- schema.gen.ts:158-166
session_input: id PK, session_id FK, admitted_seq, promoted_seq(NULL=未提升), prompt, delivery, time_created
索引:session_input_session_pending_delivery_seq_idx
    UNIQUE(session_id, admitted_seq) / UNIQUE(session_id, promoted_seq)
```

**admitted_seq = PromptAdmitted 事件的 durable seq**(input.ts:55-75)——收件与事件日志同序。

### 2. Admit(设计 2:幂等 + 冲突 + 并发)

```ts
// input.ts:41-81
admit:
1. find(id) → 已存在直接返回(幂等)
2. events.publish(PromptAdmitted) → 返回 Admitted{admittedSeq: event.durable.seq}
3. catchDefect → 重读:并发已 admit 则成功(不重复写入)
```

**测试证据**:
- 252 "returns the original recorded message when the ID is retried"
- 237 "records distinct messages when the ID is omitted"(无 ID → 每个都是新消息)
- 342 "returns one recorded message to concurrent exact retries"(2 并发 → 1 个回执 + 1 个 admitted 事件)

### 3. 冲突检测(设计 3:LifecycleConflict → PromptConflictError)

```ts
// session.ts:360-386 prompt()
LifecycleConflict 或 !SessionInput.equivalent(admitted, expected) → PromptConflictError
```

**测试证据**:
- 292 "rejects reuse of one ID with a different prompt"
- 317 "rejects reuse of one ID with a different delivery mode"
- 488 "rejects reuse of one globally unique message ID across sessions"
- 518 "rejects a prompt ID already used by visible Session history"(跨 session/历史冲突)

### 4. Promotion(设计 4:steer 批量 + queue 单个 + cutoff)

```ts
// input.ts:245-266 promoteSteers:delivery='steer' AND promoted_seq IS NULL AND admitted_seq <= cutoff,批量
// input.ts:268-287 promoteNextQueued:delivery='queue' AND promoted_seq IS NULL,orderBy admitted_seq LIMIT 1
// publish(input.ts:216-243):events.publish(Prompted) → 投影器同事务标记 promoted + 写可见消息
```

**测试证据**:
- 386 "promotes steers only through the captured inbox cutoff"(cutoff 内提升,cutoff 外保留 pending)
- 362 "promotes one message once under concurrent promotion attempts"(2 并发 promote → 1 个 Prompted 事件,投影器同事务防重)
- 143 "durably admits one user message before transcript promotion"(admit 后 messages 为空 = 未提升)

### 5. 提升原子性(设计 5:事件 + 投影同事务)

promote → publish(Prompted) → projector.ts:353-366:
```
UPDATE session_input SET promoted_seq = seq(事件事务内)
INSERT session_message(可见 user 消息)(同事务)
```
失败 → 全回滚(EventV2 提交协议保证)。

### 6. 重放语义(设计 6:收件箱可重放,不调度执行)

**测试证据**:
- 403 "reprojects pending inbox input without scheduling execution":删除事件+表 → replayAll 重建 → admitted 恢复,promotedSeq 不存在,wakeCalls = []——重放只重建收件箱状态,不触发执行
- 186 "streams durable Session events after an aggregate sequence":事件流 = [prompt.admitted, prompt.admitted, prompted, prompted](admit 先于 promote)

### 7. Legacy 合成(设计 7:历史投影提示词的 exact retry)

**测试证据**:
- 446 "returns an exact retry of a legacy projected prompt":直接 publish(Prompted)(无 admitted 记录)→ retry 同 ID → 合成收件箱记录(promotedSeq 存在)
- 467 "returns an exact retry of a legacy projected queued prompt"(queue 版本)

**设计要点**(AGENTS.md):"Historical projected prompts lazily synthesize promoted inbox records during exact retry"——V1 时代的可见消息在 V2 收件箱里没有 admit 记录,重试时按投影合成。

### 8. resume 语义(设计 8)

| resume | 行为 | 测试 |
|--------|------|------|
| 省略 | admit + wake(默认执行) | 539 "starts execution by default" |
| true | admit + wake | 553 "starts execution when resume is explicitly true" |
| false | 只 admit,不 wake | 571 "only records the prompt when resume is false" |

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 双游标表结构(admitted_seq/promoted_seq) | schema.gen.ts:158-166 | ④知识库日志模型 |
| 2 | admit 幂等 + 并发 catchDefect 重读 | input.ts:41-81 | ④写入幂等 |
| 3 | 冲突检测(LifecycleConflict → PromptConflictError) | session.ts:360-386 | ④subject 冲突(持久化版) |
| 4 | steer 批量/queue 单个 + cutoff 语义 | input.ts:245-287 | ②delivery 语义 |
| 5 | 事件+投影同事务原子提升 | projector.ts:353-366 | ④一致性 |
| 6 | 重放只重建收件箱,不调度执行 | 测试 403 | ④重放安全 |
| 7 | Legacy 投影合成收件箱记录 | 测试 446/467 | ④迁移兼容 |
| 8 | resume 三态(省略/true/false) | session.ts:382 | ②admit-only 模式 |

## 面试弹药

- "先落盘再进模型":admit(收件箱)与 promote(可见历史)分离,模型永远只看已提升消息;resume:false = admit-only
- "幂等靠表 + 事件重放,不用分布式锁":同 ID 同内容返回同一回执;冲突 die;并发靠 onConflictDoNothing + 重读
- "cutoff 是 turn 边界":promoteSteers 只提升 ≤ 当前 seq 的输入——正在跑的 turn 不受新输入影响
- "重放不调度执行":重放重建状态(收件箱),不触发副作用——重放与执行严格分离
- "legacy 合成":老会话的可见消息没有 admit 记录,重试时按投影合成 promoted 记录——迁移兼容

## 待深挖

- [ ] Prompt 的 attachments/MIME 解析(resolvePrompt session.ts:460-472)
- [ ] schema/src/session-input.ts 的 Admitted/Delivery schema
