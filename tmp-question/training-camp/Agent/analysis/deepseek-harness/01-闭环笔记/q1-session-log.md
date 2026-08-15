# q1 — Session 日志(深度版:事件源 + Surface 投影 + 版本机制)

> 域:④知识库(事件源核心) | 文件:packages/core/session/src/(index.ts 1157/types.ts 436/surface.ts 460/chunk-rows.ts 346/invariant.ts 250/json.ts 190/repair.ts 133/request-header.ts 71)+ docs/architecture.md + docs/subsystems/session.md
> review 轮次:2 轮(源码全文核心 + 架构文档)

---

## 假设

Session 日志 = 追加写事件源(一切持久状态的唯一真相)。模型可见 ⟺ 已记录(运行时不变量)。Surface = 日志上的"模型可见视图"(3 种事件投影为 LLM 消息)。持久化/标题/遥测全是插件(订阅事件流)。

## 验证

### 1. 事件四类(设计 1:created/disposed/event/flush)

```ts
// index.ts:37-87 declare module '@deepseek-ai/cordis':
'session/created'(Scoped<Session>, session): 同步 throw veto 并回滚(配对 disposal);detach 请求推迟
'session/disposed': 离开 store(含发布回滚);监听失败记录并隔离
'session/event'(session, event): post-commit fire-and-forget append feed;快照在 log push 前解析,回调在 push 后
'session/flush'(session): 并行耐久性检查点——每个监听器运行,调用方等待全部,无 waterfall veto
// 关键:persistence 是插件关注点(订阅 session/event + drain 时 flush)——核心不依赖任何持久化实现
```

**产品启示**:④知识库的"存储插件化"——核心只发事件,持久化后端可换(JSONL/SQLite),验收器也可挂事件流。

### 2. SessionEventMap(设计 2:声明合并 + 可扩展)

```ts
// types.ts:236-335
interface SessionEventMap {
  'turn/start': { turn }
  'turn/end': { turn; reason: TurnEndReason }
  'step/start': { turn; step }
  'step/end': { turn; step }
  'user/message': UserMessage
  'assistant/chunk': { turn; step; chunk: StreamChunk }
  'assistant/message': { turn; step; message: AssistantMessage; usage? }
  'tool/call': { turn; step; callId; name; arguments }
  'tool/result': { ... }
}
// 插件用 declare module 合并扩展(typed events use declaration merging)
// SessionEventType = keyof SessionEventMap(合并后全集)
// ignorable: true 的枚举(未登记的构建拒绝写日志除非 ignorable)
```

**产品启示**:④知识库事件 schema = 声明合并扩展(插件加事件不改核心)——与 OpenCode 的 durable manifest 相比,类型级更强(构建期强制)。

### 3. Surface 投影(设计 3:3 事件 → LLM 消息)

```ts
// surface.ts:15-19,83-114
SURFACE_EVENT_TYPES = user/message | assistant/message | tool/result
deriveEventMessage(event):
  user/message → event.data(verbatim 直通;framing 是调用方责任,如 agent-instructions 的 <system-reminder>)
  assistant/message → data.message(空 content 跳过——只承载 usage 的 step)
  tool/result → data.message
  其他 → null(turn/step 边界、chunk、usage、error 只是 trace/replay 数据)
// surfaceOp: append | replace(替换阴影)——模型可见 surface 故意阴影被替换范围
// isAppendSurfaceEvent:人类转写的耐久来源;replacement 副本仅模型可见
//   ("a landed replacement would erase conversation the user already saw")
// deriveMessages() 折叠该函数:live surface 与 log 前缀重建的请求完全一致
```

**产品启示**:④"模型视图 vs 人类转写分离"——replace 阴影只影响模型,append-origin 保人类可见——这是 OpenCode 没有的设计(OpenCode 的 system 消息是追加,无替换语义)。

### 4. 版本机制(设计 4:writer 决定 bump)

```ts
// types.ts:51-91
SESSION_FORMAT_VERSION = 0(单调整数,无 major/minor)
bump 规则:由 WRITER 决定(不是 reader 能接受什么)
  - "parses without error" ≠ 正确——静默跳过影响重建的内容 = 错误读
  - 仅结构变化 bump:header 形状/SessionEvent 信封/核心事件语义/surface 机制(类型集+op 变体)
  - 普通新增事件不 bump(ignorable 守卫覆盖词汇增长)
  - 不确定就 bump(近身份升级近乎免费;漏 bump = 旧运行时静默读错新日志)
// 持久化后端拒绝其他版本(无迁移)
// Agent Note:session-log-version-mechanism(升级链/内存视图转换/migrate-on-continue)
```

**产品启示**:④知识库版本策略——"写入方决定何时升级"比 OpenCode 的 versionedType 更严格(结构级 bump + ignorable 词汇级)。

### 5. 运行时不变量(invariant.ts:250)

```ts
// invariant.ts:每个包拥有 ./invariant——检查"事件/数据关系"(非服务存在性)
// session 的不变量:模型可见 ⟺ 已记录(新模型可见输入 ⟹ 新 session 事件)
// verify-package-invariants 门禁强制
```

### 6. 存储行(chunk-rows.ts:346)

```ts
// decodeStorageRecord/packChunkRuns:chunk 运行打包(assistant/chunk 压缩存储)
// StorageRecord:持久化记录格式
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 事件四类(created veto/disposed/event feed/flush checkpoint) | index.ts:37-87 | ④存储插件化 |
| 2 | SessionEventMap 声明合并 + ignorable | types.ts:236-335 | ④事件 schema 扩展 |
| 3 | Surface 投影(3 事件 + append/replace 阴影) | surface.ts:15-114 | ④模型视图 vs 人类转写 |
| 4 | 版本机制(writer 决定 + 结构级 bump) | types.ts:51-91 | ④版本策略 |
| 5 | 运行时不变量(模型可见⟺已记录) | invariant.ts | ④正确性强制 |
| 6 | chunk 打包存储 | chunk-rows.ts | ④存储紧凑 |

## 面试弹药

- "持久化是插件,不是核心":核心只发事件(created veto/event feed/flush checkpoint),JSONL/SQLite/标题/遥测全是订阅者——可替换性即架构
- "模型可见 ⟺ 已记录":运行时不变量强制——比"应该记录"强一个量级
- "Surface 双视图":append-origin 保人类转写,replace 阴影只影响模型——避免"替换抹掉用户看过的对话"
- "writer 决定 bump":结构变化才 bump,词汇增长用 ignorable——版本策略精确到"谁该负责"

## 待深挖

- [ ] types.ts 的完整 SessionEventMap(TurnEndReason 变体)
- [ ] repair.ts(中断 turn 闭合器/interruptedTurnClosers)
- [ ] 持久化后端(jsonl vs sqlite)的差异
