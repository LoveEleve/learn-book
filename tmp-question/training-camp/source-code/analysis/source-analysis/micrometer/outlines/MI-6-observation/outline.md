# MI-6 Observation 域 — outline 收敛版

> 核心首批: `Observation.java` / `ObservationRegistry.java` / `ObservationHandler.java` / `SimpleObservation.java` / `SimpleObservationRegistry.java` / `NoopButScopeHandlingObservation.java` / `NullObservation.java` / `ObservationFilter.java`
> harness: `io.micrometer.observation.MiniMI6` **20/20 PASS** | 日期: 2026-08-17

## 一、域定位
- Observation 是高层“生命周期总线”，不是单一 metrics API
- 一次 observation 可同时驱动 metrics / tracing / logging / baggage 等多个 handler
- 默认运行时核心：`SimpleObservation` + `SimpleObservationRegistry`

## 二、工厂层与 no-op 分支
- `Observation.start(...)` / `createNotStarted(...)` 统一走静态工厂
- 在下列场景直接 fast-return no-op：
  - `registry == null`
  - `registry.isNoop()`
  - 任一 predicate 判 false
- no-op 返回的不是纯 `Observation.NOOP`，而是 **`NoopButScopeHandlingObservation`**，原因是即便禁用埋点，也要保留 scope/context propagation 语义

## 三、Registry / Config
- `ObservationRegistry.ObservationConfig` 维护 4 个 CopyOnWriteArrayList：
  - handlers
  - predicates
  - conventions
  - filters
- predicate 语义：**AND**，任一 false → disabled
- convention 选择：取第一个 supports 的全局 convention，否则 fallback 到 default convention
- `SimpleObservationRegistry.isNoop()`：
  - registry 本身是 `NOOP`
  - 或者 handler 列表为空
  都视为 no-op

## 四、SimpleObservation 状态机
- 构造阶段就完成：
  - `context.setParentFromCurrentObservation(registry)`
  - 从 registry 中筛出 **当前 context 支持的 handlers**（过滤后固化成 deque）
  - 捕获 filters
- `start()`：
  - 先让 convention 写 low/high cardinality key values，并可覆盖 technical name
  - 再正序 `onStart`
- `error(Throwable)`：立即 `context.setError()` + 正序 `onError`
- `event(Event)`：立即正序 `onEvent`
- `openScope()`：创建 `SimpleScope`，再正序 `onScopeOpened`
- `stop()`：
  - convention 再次补 key values，并可能设置 contextual name
  - 顺序应用所有 `ObservationFilter.map(...)`
  - 最后 **倒序** `onStop(context)`

## 五、顺序语义
- 正序：`onStart / onError / onEvent / onScopeOpened`
- 倒序：`onScopeClosed / onStop`
- 这是标准的“入栈正序 / 出栈逆序”模型

## 六、Scope 语义
- `SimpleScope` 构造时保存 `previousObservationScope`，并立刻设为当前 scope
- `close()` 时：
  - 若 current 是 `SimpleObservation` → 倒序 `onScopeClosed`
  - 然后恢复 `previousObservationScope`
- deprecated:
  - `Scope.reset()` no-op + warn
  - `Scope.makeCurrent()` no-op + warn
  - `Observation.getEnclosingScope()` 恒 `Scope.NOOP` + warn

## 七、Filter / Convention / Parent 细节
- `ObservationFilter` **只在 stop 前生效**，不会影响 earlier callbacks (`onError`/`onEvent`/`onScopeClosed`)
- parent 来源两种：
  - 当前 scope 自动继承
  - 手动 `parentObservation(...)`
- convention 优先级：`custom > global > default`

## 八、Handler 组合
- `FirstMatchingCompositeObservationHandler`
  - 每个生命周期只选第一个 supports 的 handler
- `AllMatchingCompositeObservationHandler`
  - 对所有 supports 的 handler 全量回调
- 两者 `supportsContext` 都是“只要存在任一支持者即返回 true”

## 九、Noop/Null 变体
- `NoopButScopeHandlingObservation`
  - 不做记录
  - 但 `openScope()` 仍创建 `SimpleScope(ObservationRegistry.NOOP, this)`
- `NullObservation`
  - 特殊 observation，用于 scope 清理/传播场景
  - 禁止 handler 生命周期回调，仅保留 scope 语义
- `ObservationRegistry.NOOP` 仍复用 `SimpleObservationRegistry` 的 thread-local current scope 读写逻辑

## 十、当前关键结论
- Observation 域的关键不是“是否记一个 timer”，而是：**生命周期时序、scope 恢复、parent 自动继承、以及 no-op 仍保留传播语义**
- `handlers` 在 observation 创建时按 context 过滤并固化，后续 filter/convention 不会重新做 handler 匹配
- stop 前 filter map 只影响 `onStop` 视图，不影响已发生的回调