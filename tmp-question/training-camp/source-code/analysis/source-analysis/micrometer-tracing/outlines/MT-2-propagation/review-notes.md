# MT-2 传播与上下文适配 — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2 + harness 收敛)
- [x] 通读 5 个核心实现文件
- [x] 官方 `BaggageToPropagateTests` 对照：奇数 varargs / map copy / varargs map
- [x] 官方 `ReactorBaggageTests` 对照：单值、多值、同名覆盖、旧 key 保留
- [x] integration tests 对照：Observation+manual span / pure Observation / pure span / thread pool / baggage
- [x] `Propagator.NOOP`、Getter 默认 `getAll()`、Setter/Getter 无状态契约已验证
- [x] span accessor 退让规则已实证：OTLA span → null；手动 child → child span
- [x] baggage accessor：无 span warn/no-op；有 span 建立可逆 baggage scope 链
- [x] Reactor Context append 合并/覆盖已实证
- [x] harness `MiniMT2` **17/17 PASS**

## 关键易错点
- `ObservationAwareSpanThreadLocalAccessor` 不是简单地“永远返回 tracer.currentSpan()”；Observation 已创建 tracing span 时必须退让
- `ObservationAwareBaggageThreadLocalAccessor` 无 current span 时不会抛出，而是 warn 后不设置
- `BaggageToPropagate` map 构造复制输入，但 `getBaggage()` 返回内部 map，应按快照使用
- accessor 的线程 map 若不 restore/close，会产生线程状态污染风险

## 收敛判定
- MT-2 第一轮收敛；后续需要做第二轮：生产代码/官方测试覆盖矩阵、生命周期异常路径、bridge Propagator 差异边界。

## 审查轮次: 第二轮 (2026-08-17, bridge 交叉/生命周期/大小写边界)
- [x] Brave/OTel `Propagator` 都显式实现 `fields/inject/extract`；OTel 额外覆盖 `Getter.getAll()`，不会误用默认单值读取
- [x] Brave extract 会回写已存在 baggage fields；OTel extract 通过 `TextMapGetter.getAll` 支持重复 propagation key
- [x] `BaggageTextMapPropagator` 应放在 OTel propagator chain 末端；inject 只导出 remoteFields 白名单，extract 将 carrier 值写入 baggage context
- [x] accessor integration tests 覆盖 Observation+manual spans、pure Observation、pure spans、thread pool、Reactor baggage、restore/close
- [x] 生命周期风险确认：spanActions/baggageInScope 都按 Thread 保存，正常 restore 会移除/恢复 previous；未关闭 scope 仍是明确的污染风险，不是隐藏自动清理
- [x] harness 从 Pass0 行为点补强到 **17/17 PASS**

## 收敛判定 (第二轮终)
MT-2 无新增源码缺陷。已知边界仅包括：`BaggageTextMapPropagator` 的 remoteFields 大小写策略由 OTel propagator 组合契约决定，不能把 inject 的大小写匹配直接推断成 extract 的大小写归一化。
