# q17 — Session 投影/协调器/遥测(深度版:纯折叠 + 检查点非权威)

> 域:④知识库(投影/持久化) | 文件:packages/session/(session-projection/src/index.ts 428+session-persistence/src/coordinator.ts 1361+session-telemetry+session-telemetry-otel)+ session-stats
> review 轮次:2 轮(源码全文核心)

---

## 假设

投影 = 纯折叠(previous state + committed event → next state),带状态版本(缓存失效)与检查点(非权威捷径)。协调器 = 每会话写状态机(cursor 校验)。遥测/统计 = 会话级观测。

## 验证

### 1. 投影定义(设计 1:五元契约)

```ts
// session-projection/src/index.ts:40-108:
ProjectionDefinition = {
  key: 投影键(自己的 SessionProjectionMap 条目)
  schema: ZodType(wire 载荷离开 host 前验证)
  init(): 空日志初始状态
  apply(state, event): 纯转移——不关心的事件必须返回同引用(Object.is → 零下游工作)
  view(state): 状态 → wire 载荷(读侧投影)
  stateVersion: 持久化缓存失效版本——序列化字段/折叠语义变化时 bump
    (旧版本行丢弃,不 forward-apply 进垃圾)
}
```

**产品启示**:④章节投影(标题/进度/结论) = 纯折叠 + 版本失效——"不关心的返回同引用"是性能契约(Object.is 零成本跳过)。

### 2. 快照与检查点(设计 2:水印 + 非权威)

```ts
// session-projection/src/index.ts:93-127:
ProjectionSnapshot = { asOfSeq(共享水印——每个值反映的最后事件 seq;-1 = 空日志), values }
ProjectionCheckpointRow = { ver(stateVersion), seq(最后折叠事件), val(内部状态) }
  ——持久化缓存行(sessionId, key, ver, seq, val)
  "A row is never authoritative, only a fold shortcut:
   restore discards it on a version mismatch or when it claims events past the stored log end"
// ProjectionChangeListener:变化通知(session, key, value, seq——watermark)
```

### 3. 协调器(设计 3:每会话写状态机)

```ts
// session-persistence/src/coordinator.ts:590-700:
states = Map<SessionId, SessionState>({ meta, cursor, materialized })
append(id, events):adopt(接管未绑定状态)→ 校验 event.seq === state.cursor + i(seq 连续性)
  → 不匹配 throw("append seq mismatch: expected cursor+i at index i")
// preparations:SessionPreparations(准备缓存 DEFAULT_PREPARED_SESSION_CACHE_SIZE = 5)
// settledErrors:收集所有 promise 错误(不丢)
// dispose 顺序:quiescence drain 后(注释:coordinator's dispose effect AFTER the quiescence drain)
```

### 4. 遥测/统计(设计 4:会话级观测)

```ts
// session-telemetry(coordinator + index):会话遥测协调
// session-telemetry-otel:OTel 导出(与 OpenCode 的 OTLP 同思路)
// session-stats:会话统计
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 投影五元契约(纯折叠 + stateVersion) | session-projection:40-108 | ④章节投影 |
| 2 | 快照水印 + 检查点非权威 | session-projection:93-127 | ④缓存正确性 |
| 3 | 协调器 seq 连续性校验 | coordinator.ts:699-700 | ④写正确性 |
| 4 | 遥测/统计(OTel) | session-telemetry* | ③观测 |

## 面试弹药

- "不关心的返回同引用":Object.is 零下游工作——投影性能契约
- "检查点永非权威":只做折叠捷径,版本不匹配/越过日志尾 → 丢弃——缓存永远可重建
- "stateVersion 防 forward-apply 垃圾":折叠语义变化时旧行作废
- "append seq 校验":cursor + i 不匹配 throw——写序硬约束

## 待深挖

- [ ] coordinator 的 torn 恢复(撕裂标记)
- [ ] session-telemetry 的协调语义
- [ ] session-stats 的统计项
