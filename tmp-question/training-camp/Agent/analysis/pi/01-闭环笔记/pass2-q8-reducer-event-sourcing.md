# q8 事件溯源正确性层(reducer + writer-leases + seq 全序)— 产品④重放损坏检测补漏

> 项目:Pi(agent/src/harness/reducer.ts + session-backends/sqlite-node/src/sqlite/storage/)
> 背景:Pi 域发现 v1-v6 假收敛遗漏(59 域),v7/v8 复测补漏。本笔记是 HANDOVER 待办的闭环——**Pi 参考架构声称"事件溯源可重放"却没引用损坏检测层**。
> 假设:reducer.ts 的 RecordLogCorruption 是产品④"重放即真相"的损坏检测蓝本;writer-leases 是 Fencing Token 的完整实现。
> 结论:✅ 成立——Pi 有完整的"事件日志 → 归约 → 损坏检测"三层,这是知识库重放正确性的直接证据。

---

## 一、架构位置:事件溯源三层

```
┌────────────────────────────────────────────────────────────┐
│ 第 1 层:事件日志(records/lanes 表)                         │
│   (session_id, lane, run_id, op_kind, seq, timestamp, payload) │
└──────────────┬─────────────────────────────────────────────┘
               ▼ 重放(按 seq 排序)
┌────────────────────────────────────────────────────────────┐
│ 第 2 层:归约器 reducer.ts:506 reduceLaneState              │
│   输入:RecordLogSlice + ownEntries + 配置条目 + 默认值      │
│   输出:LaneState(操作/工具批/steer/followUp/挂起写/溢出)   │
│   前置:validateRecordLog(损坏检测)                         │
└──────────────┬─────────────────────────────────────────────┘
               ▼
┌────────────────────────────────────────────────────────────┐
│ 第 3 层:状态(applyMutation 不变量,state.ts)                │
│   每次变更验证:seq 连续/id 唯一/lane 存在                   │
└────────────────────────────────────────────────────────────┘
```

**关键设计**:归约器是**纯函数**(确定性 + 不突变输入——测试断言 "is deterministic and does not mutate or alias its inputs")。损坏检测在归约**之前**执行,restore 拒绝损坏态。

---

## 二、设计 1:RecordLogCorruption 12 种损坏原因(损坏分类学)

**位置**:`reducer.ts:12-31`(类型定义)+ `312-392`(validateRecordLog)

| # | 损坏原因 | 检测逻辑 | 防止的 bug |
|---|---------|---------|-----------|
| 1 | multiple_open_operations | openOperations > 1 | 并发操作写同一 lane(单写者协议违反) |
| 2 | unknown_operation | record 引用不存在的 runId | 孤儿记录(半写崩溃) |
| 3 | record_after_finish | record.seq > 操作的 finish seq | 完成后追加记录(写入乱序) |
| 4 | non_consecutive_attempt | attempt 编号不连续(预期 = 前一 attempt+1) | 尝试序列断裂(丢失重试记录) |
| 5 | invalid_compaction_reason | 压缩尝试 reason 不是 manual/threshold/overflow;非压缩尝试带 reason | 压缩意图伪造 |
| 6 | inconsistent_step | 同系列尝试的 resultEntryId 不一致 | 尝试结果错配 |
| 7 | tool_call_mismatch | 工具批解析与记录不一致 | 工具调用与结果错配 |
| 8 | duplicate_tool_invocation | toolInvocations Set 重复 | 工具重复调用(重放翻倍) |
| 9 | queue_after_abort | abort 后仍 enqueue(nextRun 除外) | abort 后继续排队(意图失效) |
| 10 | invalid_queue_cancellation | 取消无对应 pending enqueue | 幽灵取消 |
| 11 | provisioned_entry_mismatch | 落盘条目内容 ≠ 意图声明的 provisioned 内容 | 内容被篡改(与意图不一致) |
| 12 | invalid_deferred_handle | deferred 句柄未在操作尾赎回 | 挂起写丢失 |

**核心哲学**:
```
corrupt() → throw RecordLogCorruption(reason, message)
restore 必须 REJECT 损坏态而非修复或继续
"这些是单写者协议无法产生的状态,不是普通操作失败或可恢复的不完整前缀"
```

**产品④映射**:知识库重放时的损坏检测——**重放不等于真相,重放+验证才是**。12 种损坏原因枚举 = "什么能算损坏"的完整定义,直接可抄为知识库日志的校验器。

## 设计 2:归约不变量(行为契约,测试 1,127 行佐证)

**位置**:`reducer.test.ts:442-1115`

测试断言的核心不变量(部分):
1. 不突变有界恢复输入(纯函数)
2. 空闲 lane 归约 → pending next-run 输入 + 默认配置
3. 持久化配置按序列折叠到默认值之上
4. 提交的操作自有配置在锚点后生效
5. **abort 杀 steer/followUp 队列,但保留写入和 next-run 输入**
6. 关闭最新 attempt 仅当其 provisioned 结果存在时
7. 未兑现的旧 attempt 结果 id 被忽略
8. 工具批**不**从 deferred-write 工具结果解析
9. 匹配被阻止的结果(无 tool-start 记录)并保持源顺序
10. 长度停止的工具批标记为截断,不解析
11. **未赎回的 deferred handle 仅在操作尾检测**
12. 错误形状的 deferred 写不分类为终端失败
13. **溢出守卫仅在新对话输入被消费后重置**
14. 确定性且不突变输入

**产品④映射**:知识库重放的"不变量集"——每个不变量都是"重放得到错误状态"的检查点。特别:abort 语义(中止操作保留已写入的数据)与"知识库中断恢复不丢已落库结论"直接对应。

## 设计 3:Fencing Token 完整实现(writer-leases)

**位置**:`storage/writer-leases.ts:19-48`

```
WriterLease = { ownerId, fence, expiresAtMs }

acquireWriterLease(db, sessionId, ownerId, now, expiresAtMs):
  INSERT ... ON CONFLICT(session_id) DO UPDATE
    SET owner_id=新, fence=fence+1, expires_at_ms=新
    WHERE expires_at_ms <= now            ← 仅过期才让位
  RETURNING owner_id, fence, expires_at_ms
  → undefined 表示他人持有未过期租约

renewWriterLease(db, sessionId, lease, now, expiresAtMs):
  UPDATE ... WHERE session_id=? AND owner_id=? AND fence=?
    AND expires_at_ms > now               ← 条件续租(防 stale 续租)
  → changes==1 才算成功
```

**正确性细节**:
- **fence 单调递增**:每次接管 +1,防"旧 owner 的续租/释放影响新 owner"
- **条件续租**:renew 必须带完整 fence 匹配——stale 持有者无法续租
- **过期让位**:仅在 expires_at <= now 时允许新 owner 接管(租约语义)
- 与 Hermes delivery_ledger/compression_lock 同族:跨进程协调的正确性靠"持 token + 版本 + 过期"

**产品④映射**:知识库多进程写的栅栏——"谁在写"必须带 fence,防止旧进程的延迟写覆盖新进程(与 #25 的"写来源 ContextVar"同思路,但 Pi 用 fence 更强)。

## 设计 4:seq 全序(session-sequences)

**位置**:`storage/session-sequences.ts:1-29`

```
session_sequences(session_id, next_seq)
createSequence(db, sessionId, nextSeq=1)
getNextSequence / setNextSequence / advanceSequence(seq+1)
```

单调递增 seq = 事件溯源的全序基础。**Pi 的事件日志重放 = 按 seq 排序归约**,seq 不连续 → 损坏。

## 设计 5:文件变异队列(file-mutation-queue)

**位置**:`core/tools/file-mutation-queue.ts:1-40`

```
fileMutationQueues: Map<realpath 规范化路径, Promise 链>
withFileMutationQueue(filePath, fn):
  key = realpath(filePath)(ENOENT 时用 resolve)
  同文件操作串行化(排队),不同文件并行
```

**与 Hermes file_state 同构**:跨代理/跨操作的同文件写安全。Pi 是"同文件 promise 串行",Hermes 是"read 戳 + 全局最后写者"。产品②并行工具执行的写安全两者择一。

---

## 三、产品④知识库"重放正确性"完整设计输入

| 层 | Pi 证据 | 产品④用法 |
|----|--------|-----------|
| 日志 | records/lanes 表 + seq 全序 | 章节结论事件日志(追加写 + 单调 seq) |
| 归约 | reduceLaneState(纯函数) | 重放 = 按 seq 归约出知识库状态 |
| 损坏检测 | RecordLogCorruption 12 种 | 重放校验器(损坏必须拒绝,不能静默修复) |
| 不变量 | reducer.test 14 组 | 重放自检断言 |
| 写栅栏 | writer-leases(fence 单调) | 多进程写章节的栅栏 |
| 并发写 | file-mutation-queue | 同文件写串行化 |

---

## 四、面试弹药

1. **"为什么损坏必须拒绝而不是修复"**:修复 = 猜测意图;单写者协议产生的状态是确定的,不确定的状态只能来自损坏——拒绝让操作者知道"数据不可信"(Pi 的 corrupt() 哲学)
2. **"Fencing Token 的 SQLite 实现"**:fence 单调递增 + 条件续租 WHERE fence=旧值,stale 持有者无法续租/释放影响新 owner
3. **"归约器纯函数的意义"**:确定性 + 不突变输入 → 重放可复现;测试断言"不突变或别名输入"是纯函数的契约
4. **"abort 保留写入"**:中止操作杀 steer/followUp 队列但保留已落库写入——中断不丢已确认的数据(与 #21 交接不丢结论同哲学)
5. **"工具批不从未决 deferred-write 解析"**:挂起写的工具结果不能解析工具批——防止把未落库的结果当真相

---

## 五、产品映射汇总

| 设计 | 产品④(书级知识库)用法 |
|------|----------------------|
| 12 种损坏原因 | 知识库日志校验器的完整分类 |
| 归约纯函数 | 重放可复现(确定性) |
| abort 保留写入 | 中断恢复不丢已落库结论 |
| fence 单调递增 | 多进程写栅栏(版本防 stale) |
| seq 全序 | 事件日志全序基础 |
| 文件变异队列 | 同文件写串行化 |

> 覆盖设计数:8(设计 1-5 + 2 子设计/3 子设计)
