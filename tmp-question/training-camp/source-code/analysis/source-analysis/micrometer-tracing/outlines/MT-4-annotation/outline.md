# MT-4 注解切面域 — outline 收敛版

> 核心文件: `SpanAspect.java` / `AbstractMethodInvocationProcessor.java` / `ImperativeMethodInvocationProcessor.java` / `DefaultNewSpanParser.java` / `SpanTagAnnotationHandler.java` / `SpanTag.java` / `NewSpan.java` / `ContinueSpan.java`
> harness: `io.micrometer.tracing.annotation.MiniMT4` **14/14 PASS** | 日期: 2026-08-17

## 一、域职责
- 把 `@NewSpan` / `@ContinueSpan` 注解映射到 `Tracer/Span` 生命周期
- 用 `@SpanTag` 把参数值映射成 span tags
- 当前主实现是 `ImperativeMethodInvocationProcessor`，即命令式/同步路径

## 二、SpanAspect
- 两个切点:
  - `@annotation(ContinueSpan)` → `continueSpanMethod`
  - `@annotation(NewSpan)` → `newSpanMethod`
- 切面本身只做 method 解析和参数封装，真正语义都下沉给 `MethodInvocationProcessor.process(...)`
- `getMethod(pjp)` 回到 target class 重新取 method，避免只拿到接口方法

## 三、ImperativeMethodInvocationProcessor
- 核心判定：`startNewSpan = newSpan != null || currentSpan == null`
- 语义：
  - `@NewSpan` 一定新建 span
  - `@ContinueSpan` 若已有 current span，则复用它；若没有 current span，则退化为新建 span
- 新建 span 时：`nextSpan()` → `newSpanParser.parse(...)` → `span.start()`
- 整个方法调用在 `try (SpanInScope ignored = tracer.withSpan(span))` 中执行
- finally:
  - `after(...)`
  - 只有 `startNewSpan==true` 时才 `span.end()`
- 结论：`@ContinueSpan` 正常情况下不会关闭调用前已经存在的 span

## 四、AbstractMethodInvocationProcessor 公共骨架
- `before(...)`：
  - `ContinueSpan.log()` 非空时记录 BEFORE 事件
  - 若 invocation 是 `SpanAspectMethodInvocation` 且配置了 `SpanTagAnnotationHandler` 且 `currentSpanCustomizer()!=null`，则给当前 span 添加参数 tags
  - 统一补 `annotated.class` / `annotated.method` tags
- `after(...)`：
  - 有 log 时记录 AFTER 事件
  - 仅新建 span 时 `end()`
- `onFailure(...)`：
  - debug 打异常
  - 有 log 时记录 AFTER_FAILURE 事件
  - `span.error(e)`
- `logEvent(...)`：若 span 为 null，warn 提示“同类自调用时 aspect 不会正确解析”

## 五、DefaultNewSpanParser
- span 名优先级：
  1. `@NewSpan.name()`
  2. `@NewSpan.value()`
  3. 方法名
- 最终统一过 `SpanNameUtil.toLowerHyphen(...)`
- 例如 `camelCaseMethod` → `camel-case-method`

## 六、SpanTag / SpanTagAnnotationHandler
- `SpanTag` 只能放在参数上
- value 解析优先级：
  1. `resolver()`
  2. `expression()`
  3. `argument.toString()`
- key 优先级：`value()` 非空优先，否则 `key()`
- value 为 null 时统一回退空串
- `SpanTagAnnotationHandler` 类注释明确：接口方法与实现方法上的 tracing 注解信息都能合并处理

## 七、注解语义
### `@NewSpan`
- 创建新 span，若已有 trace 则 child，否则 root
- 默认 span 名来自方法名并 lower-hyphen
- `name()`/`value()` 均可指定自定义名称

### `@ContinueSpan`
- 优先继续当前 span
- 若当前无 span，则退化为新建 span，并在 finally 中结束
- `log()` 非空时记录 `before/after/afterFailure` 事件
- `log()` 为空时不会自动记录这些事件

## 八、关键边界
- 同类自调用时 Aspect 解析不可靠，源码只做 warn 提示，不做自动修复
- `onFailure(...)` catch 的是 `Exception` 而不是 `Throwable`，`Error` 不走该分支
- 参数 tags 走的是 `currentSpanCustomizer()`，不是直接修改 `Span`；因此依赖 current span scope 已经建立

## 九、验证
- 仓内官方：
  - `SpanCreatorAspectTests`：接口/实现方法注解、新旧 span、tag、log、error、handler 缺失、ContinueSpan fallback
  - `SpanCreatorAspectNegativeTests`：未注解不触发、实现方法注解仍可生效
  - `SpanTagAnnotationHandlerTests` / `NullSpanTagAnnotationHandlerTests`：resolver / expression / toString / null→""
- harness：**14/14 PASS**，覆盖 `@NewSpan` / `@ContinueSpan` / `@SpanTag` / fallback / failure / no-handler 路径

## 九、第二轮补强
- `SpanAspectMethodInvocation` 只做 PJP→AOP Alliance `MethodInvocation` 适配；其存在是 `SpanTagAnnotationHandler` 能访问原始 join point 参数/target 的关键。
- `AnnotationSpanDocumentation` 固化两个 tags：`annotated.class` / `annotated.method`，以及 `before/after/afterFailure` 三类事件模板。
- 官方 `SpanCreatorAspectTests` 已验证接口方法和实现方法注解都能生效；未注解 bean 不触发 advice。
- 当前实现边界：`ImperativeMethodInvocationProcessor` 的失败捕获是 `Exception`，对 `Error` 不走 `onFailure` 标记路径。
