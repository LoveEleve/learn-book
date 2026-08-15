# q16 — Session 修复机制 + Terminal/LSP(深度版:崩溃恢复 + 测试契约)

> 域:④知识库(恢复)+ ②执行(终端/LSP) | 文件:core/session/src/repair.ts(133)+ tests/repair.spec.ts+ terminal/(terminal/terminal-bash/sanitize/session/tool-terminal)+ lsp/(lsp/brand/types)
> review 轮次:3 轮(源码全文 + 修复测试契约)

---

## 假设

修复机制 = 崩溃日志的确定性合成:扫描 → 未匹配调用收错误结果 → 合成 step/end + turn/end(interrupted)。**测试契约(11+)验证边界:平衡/空/开 step/未记录调用/已结算/已闭 step/多调用**。

## 验证

### 1. 修复算法(设计 1:interruptedTurnClosers)

```ts
// repair.ts:27-75 扫描:
跟踪 openTurn/openStep/pendingCalls(Map<CallId, {step, callSeq?}>)
  turn/start → 开 turn,清 pending;turn/end → 全清
  step/start → 开 step;step/end → 清 pending
  assistant/message → 工具调用块注册 pending
  tool/call → 记 callSeq;tool/result → 删除匹配
// 平衡日志 → []("already balanced")
// 合成序:未匹配调用 → tool/result(isError)→ 开 step → step/end → turn/end(interrupted)
// seq 从 last.seq + 1;time 复用 last(确定性,不发明未来)
```

### 2. 测试契约(设计 2:边界全覆盖)★ review 轮 3

```ts
// tests/repair.spec.ts(11+ 契约):
1. 平衡日志 → 无事件(spec:19);空日志 → 无事件(spec:27)
2. 开 turn 无开 step → 仅 turn/end{interrupted}(spec:31)
3. 开 step → step/end 先于 turn/end(spec:40——不变量顺序)
4. 未记录调用的 assistant 请求 → TOOL_NOT_STARTED(spec:50)
5. 已有结果的 tool-call → 不合成(spec:88)
6. 所属 step 已闭 → 不合成(spec:119)
7. 只合仍然开着的 turn,不动已提交的早期 turn(spec:144)
8. 多未应答调用 → 每调用一个结果,按日志序(spec:195)
9. 已记录 tool/call 的合成结果带 surfaceOp + sourceEventSeqs(spec:229)
10. tool/call 无匹配 assistant/message → 优雅处理(spec:263)
```

**设计要点**:合成只针对"仍然开着的 turn"——已提交边界永不改动;surfaceOp/sourceEventSeqs 保重放与追踪。

### 3. Terminal(设计 3:持久终端)

```ts
// terminal/:terminal(Def)+ terminal-bash(bash 后端:config/sanitize/session)+ tool-terminal(Consumer + render)
```

### 4. LSP(设计 4:语言服务)

```ts
// lsp/:lsp(Service Def)+ brand/types——语言服务器能力(作为能力缝)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 修复算法(扫描→错误结果→合成边界) | repair.ts:27-131 | ④崩溃恢复 |
| 2 | 双恢复码 + 模型可见指导(只读/幂等才重试) | repair.ts:12-16,93-123 | ④恢复语义 |
| 3 | 边界契约(已结算/已闭 step/早期 turn 不动) | repair.spec:88-263 | ④恢复正确性 |
| 4 | 确定性合成(seq 续 + 时间复用) | repair.ts:85-86 | ④重放确定性 |
| 5 | Terminal 缝 + LSP 能力缝 | terminal/* + lsp/* | ②执行面 |

## 面试弹药

- "只合开着的 turn":已提交边界永不改动——恢复不重写历史
- "恢复 = 确定性合成":seq 续接 + 时间复用,不发明未来
- "模型可见的恢复指导":只读/幂等才重试;有副作用先验证或问用户——防盲目重放副作用
- "sourceEventSeqs 可追踪":合成结果引用真实 tool/call seq
- "边界契约全覆盖":平衡/空/未记录/已结算/已闭 step/多调用——恢复正确性有测试证明

## 待深挖

- [ ] terminal-bash 的 sanitize/session 细节
- [ ] lsp 的接口面
- [ ] coordinator 的撕裂恢复(与 repair 的关系)
