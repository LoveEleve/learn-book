# MI-6 Observation 域 — Pass 0 发现

> 通读首批核心: `Observation`(1569, 前 420 行关键工厂与 Context 入口) / `ObservationRegistry`(226) / `ObservationHandler`(336) / `SimpleObservation`(330)
> 日期: 2026-08-17

## 1. 域职责
- Observation 是 **统一生命周期总线**：一次埋点可同时驱动 metrics / tracing / logging / baggage 等多个 handler
- 核心三件套:
  - `Observation`：API + 工厂 + Context/Scope 契约
  - `ObservationRegistry`：handler/predicate/filter/convention 注册与 current scope 管理
  - `ObservationHandler`：生命周期回调
- 真正运行时默认实现是 `SimpleObservation` + `SimpleObservationRegistry`

## 2. Observation 工厂层
- `start(name, registry)` / `createNotStarted(name, registry)`
- 所有工厂都有 **fast no-op** 分支:
  - `registry == null`
  - `registry.isNoop()`
  - predicate 判定禁用
  → 直接返回 `NoopButScopeHandlingObservation.INSTANCE`
- `Context` 创建前就做 no-op 短路，避免不必要分配
- 创建后 `context.setParentFromCurrentObservation(registry)`：自动捕获当前 observation 作为 parent

## 3. Convention 选择层
- `createNotStarted(customConvention, defaultConvention, contextSupplier, registry)` 的优先级：
  1. `customConvention`
  2. `ObservationRegistry.ObservationConfig` 里第一个 `supportsContext(context)` 的 `GlobalObservationConvention`
  3. `defaultConvention`
- 创建后若 predicate 对 `convention.getName()` 判 false → 返回 no-op
- `SimpleObservation` 构造时也会尝试从 config 里找 convention

## 4. ObservationRegistry
- `ObservationRegistry.NOOP` 仍委托 `SimpleObservationRegistry._getCurrentObservation()` / `_getCurrentObservationScope()` 维护 current scope 读取
- `ObservationConfig` 维护 4 个 CopyOnWriteArrayList:
  - handlers
  - predicates
  - conventions
  - filters
- `observationPredicate(...)` 是 **全量 AND 语义**：任一 predicate false → observation disabled
- `getObservationConvention(...)` 取第一个 supports 的 convention，否则 fallback 到 defaultConvention

## 5. ObservationHandler
- 生命周期钩子:
  - `onStart`
  - `onError`
  - `onEvent`
  - `onScopeOpened`
  - `onScopeClosed`
  - `onScopeReset` (deprecated)
  - `onStop`
- 两类 composite:
  - `FirstMatchingCompositeObservationHandler`
  - `AllMatchingCompositeObservationHandler`
- 前者每个生命周期都只找第一个 supports 的 handler
- 后者每个生命周期对所有 supports 的 handler 全量回调

## 6. SimpleObservation 状态机
- 构造时抓取:
  - `registry`
  - `context`
  - `convention`
  - `handlers`（**在创建时即按 supportsContext 过滤成 deque**）
  - `filters`
- `start()`:
  - 若有 convention，先把 low/high cardinality key values 写入 context，并可覆写 technical name
  - 然后 `notifyOnObservationStarted()`
- `stop()`:
  - 若有 convention，再次补 low/high key values，并可能设置 contextualName
  - 之后依次应用 `ObservationFilter.map(...)`
  - 最后 **倒序** `onStop(context)`
- `error(Throwable)`：`context.setError(error)` 后立即 `notifyOnError()`
- `event(Event)`：立即 `notifyOnEvent(event)`
- `openScope()`：创建 `SimpleScope`，随后 `notifyOnScopeOpened()`

## 7. 顺序语义 (关键)
- `onStart`：正序 handlers
- `onError`：正序
- `onEvent`：正序
- `onScopeOpened`：正序
- `onScopeClosed`：**倒序**
- `onStop`：**倒序**
- 这是典型“入栈正序 / 出栈逆序”模型

## 8. Scope 语义
- `SimpleScope` 保存:
  - `currentObservation`
  - `previousObservationScope`
- 构造时立即 `registry.setCurrentObservationScope(this)`
- `close()`:
  - 若 current 是 `SimpleObservation` → 调其 `notifyOnScopeClosed()`
  - 然后恢复 `previousObservationScope`
- deprecated:
  - `Scope.reset()` no-op + warn
  - `Scope.makeCurrent()` no-op + warn
  - `Observation.getEnclosingScope()` 恒 `Scope.NOOP` + warn

## 9. 当前已见风险点 / 待 Pass 1 验证 (Q)
- Q1: handler 集合在 `SimpleObservation` 构造时就按 `supportsContext(context)` 固化，若后续 filter/convention 改写 context，是否可能错过本应匹配的 handler？
- Q2: `stop()` 时 convention 再次追加 key values，是否可能产生重复 key；重复 key 的去重规则依赖 `KeyValues` 行为，需验证
- Q3: `ObservationFilter` 是 stop 前批量 map，是否只影响 `onStop`，不会影响 `onError/onEvent/onScopeClosed`
- Q4: `FirstMatchingCompositeObservationHandler` 与 `AllMatchingCompositeObservationHandler` 的匹配/顺序是否有官方测试覆盖
- Q5: no-op observation 虽然不记录，但 `NoopButScopeHandlingObservation` 名称暗示 scope 仍可能参与 thread-local 管理，需验证
- Q6: `ObservationRegistry.NOOP` 仍读写 current scope，这是否意味着“NOOP 也保留 scope 传播语义”，需要 harness 实证