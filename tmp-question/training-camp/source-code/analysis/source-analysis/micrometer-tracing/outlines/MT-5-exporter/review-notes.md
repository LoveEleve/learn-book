# MT-5 Exporter 抽象与过滤 — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2 + harness 收敛)
- [x] 通读 6 个核心 exporter 文件
- [x] 官方 `SpanIgnoringSpanExportingPredicateTests` 对照：主 skip list、additional skip list、regex cache
- [x] FinishedSpan typed tags 行为实证：数字/布尔/List/null
- [x] duration、regex full-match/partial-match、empty name 语义实证
- [x] TestSpanReporter report/poll/spans copy/close 语义实证
- [x] harness `MiniMT5` **12/12 PASS**

## 关键易错点
- `SpanFilter` 是 mutation hook，不是 predicate
- regex 使用 `matches()`，不是 `find()` 或 contains
- `getTypedTags()` 的默认值仍是 String，不会恢复原始对象类型
- `FinishedSpan` 的 default links/local service 是兼容性兜底，不代表 bridge 一定实现

## 收敛判定
- MT-5 第一轮收敛；下一轮需交叉检查 Brave/Otel finished span 实现，以及 Wavefront reporter 对默认字段的依赖。

## 审查轮次: 第二轮 (2026-08-17, bridge/reporter 交叉)
- [x] `BraveFinishedSpan` 对照确认：
  - 直接包裹 `MutableSpan`
  - tags/events 是“清空后重写”策略
  - links 通过 tag 编码/解码（`LinkUtils`），并非原生字段
  - local/remote service 等字段都能落地
- [x] `OtelFinishedSpan` 对照确认：
  - typed tags 真正保留 OTel AttributeKey 类型信息
  - `getError()` 通过名为 `exception` 的 EventData 反推 `Throwable` 外壳
  - links 映射到 OTel `LinkData`，比 Brave 更原生
  - local service 名来自 `Resource`，不是普通 tags
- [x] `WavefrontSpanHandler` 对照确认：
  - 强依赖 `FinishedSpan` 的 name/timestamps/tags/events/context/service/address
  - 采用异步队列 + 发送线程 + heartbeat + span derived metrics
  - close 之后继续 end/report 会 drop 并计数，不是静默吞掉
- [x] 因此 MT-5 与 MT-8 的边界清晰：MT-5 定义 finished span 契约，MT-8 消费该契约并加入队列/发送策略

## 收敛判定 (第二轮终)
MT-5 当前无已知问题。已确认 bridge 对 `FinishedSpan` default 能力的依赖差异：Brave 借 tags 编码 links，OTel 用原生 LinkData，Wavefront 把 finished span 当作完整出口模型消费。
