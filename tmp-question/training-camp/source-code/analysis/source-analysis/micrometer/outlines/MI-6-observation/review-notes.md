# MI-6 Observation 域 — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2 + harness 收敛)
- [x] 深读 8 个核心文件：`Observation` / `ObservationRegistry` / `ObservationHandler` / `SimpleObservation` / `SimpleObservationRegistry` / `NoopButScopeHandlingObservation` / `NullObservation` / `ObservationFilter`
- [x] no-op 分支闭环：null registry / noop registry / predicate=false → `NoopButScopeHandlingObservation`
- [x] scope 语义闭环：openScope 设置 current，close 恢复 previous scope
- [x] handler 顺序闭环：start 正序、scopeClosed/stop 倒序
- [x] filter 语义闭环：只影响 stop 前最终 context，不影响 earlier callbacks
- [x] convention 优先级闭环：custom > global > default
- [x] composite handler 语义闭环：first-match 与 all-match
- [x] harness `MiniMI6` **20/20 PASS**

## 关键打脸 / 易错点
- 打脸1：用于断言顺序的两个 handler 一开始没有共享同一个 log list，导致“start forward”假失败；修正后 20/20 通过
- 易错点1：registry 没 handler 时 `isNoop()==true`，这会让很多“非空 registry 应该有 observation” 的直觉失效
- 易错点2：`ObservationFilter` 不是全生命周期拦截器，只在 stop 前触发
- 易错点3：no-op 观察并不等于“没有 scope”；这里特意保留 scope 传播语义

## 收敛判定
- 第一轮收敛；下一轮需要做官方测试交叉（ObservationTests / ObservationNoopTests / CurrentObservationTest / CompositeObservationHandler tests）与残留扫描。

## 审查轮次: 第二轮 (2026-08-17, 官方测试交叉/残留清零)
- [x] `ObservationTests` 对照确认：
  - no handlers / null registry / predicate=false → noop
  - filter 可修改 context/key values
  - `parentObservation(...)` 与 scope 自动 parent 两条链路都被官方测试覆盖
  - convention 优先级 (`custom/global/default`) 有完整矩阵测试
- [x] `ObservationNoopTests` 对照确认：禁用 observation 仍保留 scope 栈与 previousScope 恢复 —— 与 `NoopButScopeHandlingObservation` 结论完全一致
- [x] `CurrentObservationTest` 对照确认：current observation 的同线程嵌套恢复、跨线程传播、即便 observation 被 predicate 禁用也仍可传播
- [x] `FirstMatchingCompositeObservationHandlerTests` / `AllMatchingCompositeObservationHandlerTests` 对照确认：first-match / all-match 两类 composite 语义与 harness 结论一致
- [x] 关键残留清零：Q1~Q6 均已被源码+harness+官方测试覆盖；无未闭合问题

## 收敛判定 (第二轮终)
MI-6 当前无已知问题。后续若继续深审，可转向 `ObservationThreadLocalAccessor` / `ObservedAspect` / `Propagator` 等扩展桥接层。
