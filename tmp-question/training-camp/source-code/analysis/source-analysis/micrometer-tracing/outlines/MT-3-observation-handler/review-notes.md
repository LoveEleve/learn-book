# MT-3 Observation→Tracing 处理链 — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2 + harness 收敛)
- [x] 通读 6 个核心 handler 文件
- [x] `TracingObservationHandlerTests` 对照：未 start 即 event 抛 ISE、stacked scopes 倒序关闭、null scope 支持、event timestamp 分支
- [x] `TracingAwareMeterObservationHandlerTests` 对照：delegate 链与 onStop 临时 scope 可见性
- [x] `DefaultTracingObservationHandler` 主链已实证：创建 child/root span、设置 name、tag key values、转发 error、end span
- [x] sender/receiver handler 已实证：carrier inject/extract、remote service/address、name 在 stop 时设置
- [x] `RevertingScope` 语义已实读：scope 恢复 + baggage scopes 逆序关闭
- [x] harness `MiniMT3` **12/12 PASS**

## 关键易错点
- `TracingContext` 的 scope 是 per-thread map，不是单值；这意味着一个 observation 可能跨线程持有不同 trace scope
- `ERROR` key 会被转成 `RuntimeException(value)`，不等同于原始 Throwable
- sender/receiver 的 remote address 解析失败只 warn，不打断业务
- `TracingAwareMeterObservationHandler` 不是简单 delegate，而是在 `onStop` 临时把 tracing span 放回 scope

## 收敛判定
- 第一轮收敛；下一轮如果继续深审，应追 `baggageFields` 与 `RevertingScope` 的精确交互、manual current span 与 parent Observation 竞争关系。

## 审查轮次: 第二轮 (2026-08-17, parent 竞争与 baggage 白名单补强)
- [x] harness 从 12/12 补强到 **16/16**
- [x] 手动 current span vs parent Observation span：已实证 `getParentSpan(...)` 选择手动 current span 作为 parent
- [x] `RevertingScope` baggage 白名单：仅 `tracer.getBaggageFields()` 命中的 Observation key values 会转成 baggage，且匹配大小写不敏感
- [x] 非白名单 Observation key values 不会泄漏到 baggage scope

## 收敛判定 (第二轮终)
MT-3 当前无已知问题；parent 竞争与 baggage 白名单这两个最容易歧义的边界已实证闭环。
