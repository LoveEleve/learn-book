# Reasonix 域发现 v26 补充(续扫第十二轮:eventwire/event/trajectory)— 2026-08-14

> 承接:v25。本轮:internal/eventwire(770)/event(884)/trajectory(recorder)。
> 结论:eventwire 的 externalizable 标记与 event 的 typed 事件流确认——②执行事件层的高价值机制。

---

## 一、v26 深化确认(事件层)

| 设计 | 位置 | 要点 |
|------|------|------|
| **externalizable 标记** | eventwire/wire.go:14-40 | `externalizable:"true"` 标大字符串载荷(Text/Detail/Reasoning/Err)——**Remote 协议可卸载为内容引用,不改变 provider 可见语义** |
| **事件全模型** | :15-40 | 25+ 字段:Tool/Usage/Approval/Ask/Compaction/Maintenance/Guardian/DecisionReceipt/Extension/Readiness/Receipt/CheckpointTurn/**RetryAttempt/RetryMax/RetryScope(headers/stream)** |
| **typed 事件流** | event/event.go | Kind 枚举(TurnStarted/Reasoning/Text/Message/…);**Sink 解耦"发生了什么"与"怎么显示"**(TUI 渲染滚动/headless 渲染 ANSI/GUI 转发 webview——各前端实现一个 Sink);替代旧 io.Writer(写预格式化 ANSI,消费端靠行前缀猜结构——脆弱有损) |
| **trajectory 记录器** | trajectory/recorder.go | Record/MemoryRecall/DelegationAdmission/OutcomeProgress/**ContractShadowAudit**/CompletionReport/ReadinessAudit——全轨迹记录 |

---

## 二、关键设计(通用价值)

1. **"externalizable 标记"**:大字符串可卸载为内容引用——**事件载荷的可移植优化**(超大文本不阻塞事件流)
2. **"typed 事件流替代 io.Writer"**:结构化事件 vs 预格式化文本——**事件化的理由**(消费端不用猜结构)
3. **"重试字段入事件"**:RetryAttempt/RetryMax/RetryScope 在事件中——**失败可见性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v25 | — | 102 | 102 |
| v26 | eventwire/event/trajectory | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 appidentity/migration/i18n、sessioncatalog 细节、ablation——支撑层收尾。
