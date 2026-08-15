# q20 — Session-Query + Workflow(深度版:语料检索 + 工作流)

> 域:④知识库(检索)+ ②执行(工作流) | 文件:packages/(session-query/session-query:corpus/cursor/documents/extraction/filters/sources/tracing + tool-session-query:operations 275+/presentation)+(workflow/workflow:types/runtime-types + workflow-worker-thread:host/meta/protocol/realm/runtime/session/worker)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Session-Query = 逻辑语料库(live 优先 + 持久化回退)+ 检索操作(搜索/跟踪/读取)。Workflow = 工作流能力(worker-thread provider,realm 物化)。

## 验证

### 1. 逻辑语料(设计 1:live 优先)

```ts
// session-query/corpus.ts:11-60:
LogicalSession = { header(克隆源头), events(克隆原始日志) }
LogicalSessionSource:借用的源(仅在一次同步批投影期间有效;调用方必须克隆保留输出)
SessionCorpus:
  _optionalPersistenceFiber:可选服务注入(ctx.inject(['sessionPersistence']))——持久化挂载时绑定
  listSessions(signal):完整逻辑语料,newest-first 确定性顺序
  "Resolves a live-preferred corpus against the persistence service mounted now"
// LogicalProjectionResult:批投影结果(fulfilled/rejected 按 sessionId)
```

**产品启示**:④全书检索 = 逻辑语料(live 内存优先 + 持久化回退)——检索不感知存储细节。

### 2. 操作面(设计 2:5 操作)

```ts
// tool-session-query/operations.ts:275:
operations = {
  executeSessionSearch,   // 会话搜索
  executeEventSearch,     // 事件搜索
  executeSessionTrace,    // 会话跟踪
  executeEventTrace,      // 事件跟踪
  executeEventRead,       // 事件读取
}
// ——模型工具层(workspace-access/service-boundary 分离)
```

### 3. Workflow(设计 3:worker-thread 工作流)

```ts
// workflow/workflow/src/types.ts:
WorkflowRunId(Branded)/ WorkflowPhase / WorkflowMeta
WorkflowStopReason = 'completed' | 'cancelled' | 'error'
WorkflowResult / WorkflowRunInfo / WorkflowAgentInfo / WorkflowAgentOutcome(completed/failed/cancelled)
// workflow-worker-thread:host/meta/protocol/realm/runtime/session/worker
// realm.ts:materializeFromRealm(value, root)(跨线程值物化)
// tool-workflow + tool-ralph:Consumers
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 逻辑语料(live 优先 + 借源契约) | session-query/corpus.ts | ④全书检索 |
| 2 | 5 检索操作 | tool-session-query/operations.ts | ④查询面 |
| 3 | Workflow(worker-thread + realm) | workflow/* | ②后台工作流 |
| 4 | 停止原因三态(completed/cancelled/error) | workflow/types.ts | ②终止语义 |

## 面试弹药

- "live 优先 + 持久化回退":语料检索不感知存储——可选服务注入(持久化挂载时绑定)
- "借源契约":LogicalSessionSource 仅一次批投影有效——克隆保留输出(借用安全)
- "realm 物化":跨 worker-thread 值安全传输
- "workflow 停止三态":completed/cancelled/error——agent 结局四态含 failed

## 待深挖

- [ ] cursor/filters 的分页过滤细节
- [ ] tracing 的跟踪语义
- [ ] workflow 的 realm 协议
