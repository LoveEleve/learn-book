# q16 — Session 修复机制 + Terminal/LSP(深度版:崩溃恢复 + 执行面)

> 域:④知识库(恢复)+ ②执行(终端/LSP) | 文件:core/session/src/repair.ts(133)+ terminal/(terminal 119?/terminal-bash/sanitize/session/tool-terminal)+ lsp/(lsp/brand/types)
> review 轮次:2 轮(源码全文)

---

## 假设

修复机制 = 崩溃日志的确定性合成:扫描 → 未匹配调用收错误结果 → 合成 step/end + turn/end(interrupted)。这是"恢复语义定理"的 dsh 版(与 Pi 的 0/1/2 三态、OpenCode 的 failInterruptedTools 同族,但更完整——合成边界)。

## 验证

### 1. 修复算法(设计 1:interruptedTurnClosers)

```ts
// repair.ts:27-75 扫描:
跟踪 openTurn/openStep/pendingCalls(Map<CallId, {step, callSeq?}>)
  turn/start → 开 turn,清 pending;turn/end → 全清
  step/start → 开 step;step/end → 清 pending
  assistant/message → 工具调用块注册 pending
  tool/call → 记 callSeq(合成结果的 sourceEventSeqs)
  tool/result → 删除匹配调用
// 平衡日志(无崩溃尾)→ 返回 []——"already balanced"
// 合成序(repair.ts:89-131):
  1. 未匹配调用 → tool/result(isError: true)——"Close calls before their step:
     providers reject dangling assistant calls;Map insertion order preserves transcript order"
  2. 开着的 step → step/end(不合成会违反不变量:turn/end 而 step 开着)
  3. turn/end(reason: { kind: 'interrupted' })
// seq 从 last.seq + 1;time 复用 last 真实事件时间(确定性,不发明未来)
```

### 2. 双恢复码(设计 2:模型可见指导)

```ts
// repair.ts:12-16,93-123:
TOOL_NOT_STARTED:记录前中断 → "Retry it if it is still needed"
TOOL_OUTCOME_UNKNOWN:记录后无持久结果 →
  "Its outcome is unknown.Decide whether to retry from the tool semantics:
   retry only if the operation is read-only or idempotent;if it may have side effects,
   first verify external state or ask the user.Do not retry blindly."
// sourceEventSeqs:合成结果引用真实 tool/call 的 seq(可追踪)
```

**产品启示**:④崩溃恢复的"模型可见指导"——不是静默失败化,而是告诉模型"为什么/怎么重试"(只读/幂等才重试)。比 OpenCode 的 "Tool execution interrupted" 更有指导性。

### 3. Terminal(设计 3:持久终端)

```ts
// terminal/:terminal(Def)+ terminal-bash(bash 后端:config/sanitize/session)+ tool-terminal(Consumer + render)
// sanitize.ts:输出消毒(终端转义?)
// architecture.md:114:"Add persistent terminal execution:register a ctx.terminals backend plus dsh-tool-terminal"
```

### 4. LSP(设计 4:语言服务)

```ts
// lsp/:lsp(Service Def)+ brand/types——语言服务器能力(brand 品牌 ID)
// 与 OpenCode 的 LSP 类似(诊断/符号),但作为能力缝
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 修复算法(扫描→错误结果→合成边界) | repair.ts:27-131 | ④崩溃恢复 |
| 2 | 双恢复码 + 模型可见指导(只读/幂等才重试) | repair.ts:12-16,93-123 | ④恢复语义 |
| 3 | 确定性合成(seq 续 + 时间复用) | repair.ts:85-86 | ④重放确定性 |
| 4 | Terminal 缝(Def/backend/tool) | terminal/* | ②持久执行 |
| 5 | LSP 能力缝 | lsp/* | ②语言服务 |

## 面试弹药

- "恢复 = 确定性合成":崩溃尾扫描 → 错误结果 + step/end + turn/end(interrupted)——不发明未来时间,seq 续接
- "先关调用再关 step":providers 拒绝 dangling assistant calls——顺序是协议要求
- "模型可见的恢复指导":只读/幂等才重试;有副作用先验证外部状态或问用户——防盲目重放副作用
- "sourceEventSeqs 可追踪":合成结果引用真实 tool/call seq

## 待深挖

- [ ] terminal-bash 的 sanitize/session 细节
- [ ] lsp 的接口面
- [ ] coordinator 的撕裂恢复(与 repair 的关系)
