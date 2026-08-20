# MT-4 注解切面域 — Pass 0 发现

> 通读首批核心: `SpanAspect` / `AbstractMethodInvocationProcessor` / `ImperativeMethodInvocationProcessor` / `DefaultNewSpanParser` / `SpanTagAnnotationHandler` / `SpanTag` / `NewSpan` / `ContinueSpan`
> 日期: 2026-08-17

## 1. 域职责
- 把 `@NewSpan` / `@ContinueSpan` 注解映射为 tracing 生命周期操作
- 与 Micrometer 核心 `@Timed/@Counted` 不同，这里消费的是 `Tracer/Span/SpanCustomizer`
- 参数级 `@SpanTag` 用于 span tag 动态提取

## 2. SpanAspect 主流程
- 两个切点:
  - `@annotation(ContinueSpan)` → `continueSpanMethod(...)`
  - `@annotation(NewSpan)` → `newSpanMethod(...)`
- 两条路径都会构造 `SpanAspectMethodInvocation(pjp, method)` 后交给 `MethodInvocationProcessor.process(...)`
- `getMethod(pjp)` 会回到 target class 上重新取 method，避免只拿到接口方法

## 3. AbstractMethodInvocationProcessor 公共骨架
- `before(...)`:
  - 若 `ContinueSpan.log()` 非空 → 发 BEFORE event
  - 若是 `SpanAspectMethodInvocation` 且配置了 `SpanTagAnnotationHandler` 且 `currentSpanCustomizer()!=null` → 追加参数 tags
  - 再统一补 class/method tags
- `after(...)`:
  - 若 `log` 非空 → 发 AFTER event
  - 仅当 `isNewSpan=true` 时 `span.end()`
- `onFailure(...)`:
  - debug 打异常
  - 若 `log` 非空 → 发 AFTER_FAILURE event
  - `span.error(e)`
- `logEvent(...)`：如果 span 为 null，会 warn 并提示“同类自调用时 aspect 不会正确解析”

## 4. ImperativeMethodInvocationProcessor 核心
- 只处理同步/命令式调用
- `startNewSpan = newSpan != null || currentSpan == null`
  - `@NewSpan` 一定新建 span
  - `@ContinueSpan` 只有在没有 current span 时才退化为新建 span
- 新建 span 时:
  - `tracer.nextSpan()`
  - `newSpanParser.parse(...)`
  - `span.start()`
- 整个调用在 `try (SpanInScope ignored = tracer.withSpan(span))` 中执行
- finally:
  - `after(span, startNewSpan, ...)`
  - 因此 `@ContinueSpan` 正常情况下不会 `end()` 既有 span

## 5. DefaultNewSpanParser
- span name 选择优先级:
  1. `@NewSpan.name()`
  2. `@NewSpan.value()`
  3. 方法名
- 最终会过 `SpanNameUtil.toLowerHyphen(...)`
- 也就是说 `camelCaseMethod` 最终默认会变成 `camel-case-method`

## 6. SpanTag / SpanTagAnnotationHandler
- `SpanTag` 只作用于 `PARAMETER`
- value 解析优先级:
  1. `resolver()`
  2. `expression()`
  3. `argument.toString()`
- key 解析优先级:
  - `value()` 非空优先
  - 否则 `key()`
- value 最终为 null 时回退空串
- `SpanTagAnnotationHandler` 会合并接口方法与实现方法上的 tracing 注解信息（类注释明确）

## 7. 注解语义
### `@NewSpan`
- 默认新建 child/root span
- `name()` 与 `value()` 都可指定 span 名
- 若两者都空，默认方法名并转 lower-hyphen

### `@ContinueSpan`
- 正常语义是继续当前 span
- 可选 `log()`，用于 BEFORE / AFTER / AFTER_FAILURE 事件名模板
- 但若当前没有 span，会退化为新建 span，并在 finally 中结束

## 8. 当前已见风险点 / 待 Pass 1 验证
- Q1: “同类自调用时 aspect 不会正确解析”的 warn 只是日志提示，是否在 tests 中有明确覆盖
- Q2: `SpanTagAnnotationHandler` 所说“接口和实现类注解合并”具体怎么落地，需看 `AnnotationHandler`/相关 tests
- Q3: `@ContinueSpan` 在“没有 current span”场景下退化为新建 span，这一语义是否有官方测试直证
- Q4: `DefaultNewSpanParser` 的 name/value 优先级和 lower-hyphen 是否有 tests 覆盖
- Q5: `onFailure` catch 的是 `Exception` 而不是 `Throwable`，对 `Error` 行为是否留空白
- Q6: 参数 tags 是通过 `currentSpanCustomizer()` 写入，而不是直接操作 `Span`，需要验证其与 scope/current span 的关系