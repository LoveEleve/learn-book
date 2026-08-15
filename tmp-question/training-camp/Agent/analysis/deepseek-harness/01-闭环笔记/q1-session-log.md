# q1 — Session 日志(深度版:事件源 + Surface 投影 + 版本机制 + 测试契约)

> 域:④知识库(事件源核心) | 文件:packages/core/session/src/(index.ts 1157/types.ts 436/surface.ts 460/chunk-rows.ts 346/invariant.ts 250/json.ts 190/repair.ts 133/request-header.ts 71)+ tests/session.spec.ts(1730)+ docs/architecture.md
> review 轮次:3 轮(源码全文核心 + 架构文档 + 测试契约 60+)

---

## 假设

Session 日志 = 追加写事件源(一切持久状态的唯一真相)。模型可见 ⟺ 已记录(运行时不变量)。Surface = 日志上的"模型可见视图"(3 种事件投影为 LLM 消息)。持久化/标题/遥测全是插件(订阅事件流)。**测试契约(60+)揭示追加不变性/JSON 严格性/冻结语义**。

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

### 3. Surface 投影(设计 3:3 事件 → LLM 消息)

```ts
// surface.ts:15-19,83-114
SURFACE_EVENT_TYPES = user/message | assistant/message | tool/result
deriveEventMessage(event):
  user/message → event.data(verbatim 直通;framing 是调用方责任)
  assistant/message → data.message(空 content 跳过——只承载 usage 的 step)
  tool/result → data.message
  其他 → null(turn/step 边界、chunk、usage、error 只是 trace/replay 数据)
// surfaceOp: append | replace(替换阴影)——模型可见 surface 故意阴影被替换范围
// deriveMessages() 折叠该函数:live surface 与 log 前缀重建的请求完全一致
```

### 4. 版本机制(设计 4:writer 决定 bump)

```ts
// types.ts:51-91
SESSION_FORMAT_VERSION = 0(单调整数,无 major/minor)
bump 规则:由 WRITER 决定
  - 仅结构变化 bump:header 形状/SessionEvent 信封/核心事件语义/surface 机制
  - 普通新增事件不 bump(ignorable 守卫覆盖词汇增长)
  - 不确定就 bump
```

### 5. 测试契约(设计 5:追加不变性 + JSON 严格性 + 冻结)★ review 轮 3

```ts
// tests/session.spec.ts(60+ 契约,关键):
1. append-only 契约(spec:437):deriveMessages() 返回的消息 deep-frozen
   ——消费方突变 → TypeError("HACKED"/"injected"/"extra" 全抛)
   ——返回数组是调用方快照(可 reverse),但永不达缓存/日志;日志深等不变
2. 非 JSON 拒绝(spec:471):BigInt/函数/Symbol/Map/undefined/Infinity/稀疏数组(JSON.stringify 写 null 的洞)/
   密集数组含非序列化元素/嵌套非序列化/循环引用(seen-set 防爆栈)——全部拒绝且不入日志
3. surfaceOp 运行时守卫(spec:498):union 拓宽绕过重载条件要求 → 运行时仍拒
   ("surface-eligible and requires a surfaceOp marker")——防 union 拓宽漏洞
4. seed 校验(spec:518-665):非 JSON/非连续 seq/surface 缺标记 → 拒;
   getter 只读一次(验证与存储用同一事件);exotic 种子原型擦除防护
5. 快照语义(spec:737-828):seed/append 后突变原对象不影响;嵌套 getter 一次;
   非 JSON surface metadata 拒绝
6. 深层冻结(spec:903-957):seed/append 事件深冻结;迭代冻结嵌套恢复;缓存数组快照不随 append 增长
7. header 校验(spec:977-1060):exotic/非 JSON/不匹配/无效标量 → 拒
8. SessionStore(spec:1094+):created/event 事件序;重复 id 拒绝;seed 支持;
   enter() 拒绝过期准备(无覆盖)
```

**设计要点**:追加不变性 + JSON 严格性是"可重建性"的基础(任何后端可持久化);surfaceOp 守卫防类型拓宽绕过;深冻结防消费方破坏日志。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 事件四类(created veto/event feed/flush checkpoint) | index.ts:37-87 | ④存储插件化 |
| 2 | SessionEventMap 声明合并 + ignorable | types.ts:236-335 | ④事件 schema 扩展 |
| 3 | Surface 投影(3 事件 + append/replace 阴影) | surface.ts:15-114 | ④模型视图 vs 人类转写 |
| 4 | 版本机制(writer 决定 + 结构级 bump) | types.ts:51-91 | ④版本策略 |
| 5 | 追加不变性 + JSON 严格性 + 深冻结(测试契约) | session.spec.ts:437-957 | ④不可变性强制 |
| 6 | surfaceOp 运行时守卫(防 union 拓宽) | session.spec.ts:498 | ④类型安全兜底 |
| 7 | chunk 打包存储 + 不变量 | chunk-rows.ts + invariant.ts | ④存储紧凑 |

## 面试弹药

- "持久化是插件,不是核心":核心只发事件(created veto/event feed/flush checkpoint),JSONL/SQLite/标题/遥测全是订阅者
- "模型可见 ⟺ 已记录":运行时不变量强制
- "追加不变性有测试证明":deriveMessages 返回深冻结消息,突变抛 TypeError;数组可 reverse 但永不达日志
- "JSON 严格性含稀疏数组":every 跳过洞但 JSON.stringify 写 null——专门防
- "surfaceOp 运行时守卫":union 拓宽绕过类型层 → 运行时仍拒——双保险
- "Surface 双视图":append-origin 保人类转写,replace 阴影只影响模型
- "writer 决定 bump":结构变化才 bump,词汇增长用 ignorable

## 待深挖

- [ ] types.ts 的完整 SessionEventMap(TurnEndReason 变体)——loop.spec 963-1187 已示(completed/max-tokens/aborted/interrupted)
- [ ] chunk-rows 的存储格式
- [ ] 持久化后端契约(coordinator-contract.ts 1482 行)
