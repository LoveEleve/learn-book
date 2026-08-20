# MT-4 注解切面域 — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2 + harness 收敛)
- [x] 通读 8 个核心文件：Aspect / processor / parser / tag handler / 3 注解
- [x] `SpanCreatorAspectTests` 对照确认：
  - 接口方法/实现方法注解都可生效
  - `@NewSpan` 默认名 lower-hyphen
  - `@ContinueSpan` 有 current span 时复用，无 current span 时 fallback 新建
  - `log()` 非空时 before/after/afterFailure 事件齐全
  - `SpanTagAnnotationHandler` 缺失时参数 tag 不写入
- [x] `SpanCreatorAspectNegativeTests` 对照确认：未注解方法不触发 aspect；实现类直接注解也能生效
- [x] `SpanTagAnnotationHandlerTests` / `NullSpanTagAnnotationHandlerTests` 对照确认：resolver / expression / toString / null→empty string
- [x] harness `MiniMT4` **14/14 PASS**

## 关键易错点
- `@ContinueSpan` 不是“永不新建 span”；没有 current span 时会退化为新建 span
- `SpanTag` 不会直接写 span，而是通过 `currentSpanCustomizer()`；因此依赖 current span scope 正确存在
- `log()` 为空时不会发 before/afterFailure 事件，不能把失败事件当成 ContinueSpan 的无条件语义
- 同类自调用问题源码只 warn，不做自动补救

## 收敛判定
- MT-4 第一轮收敛；后续若继续深审，应单独追 `SpanAspectMethodInvocation` / `AnnotationSpanDocumentation` / interface+impl 注解合并细节是否还有边界空白。

## 审查轮次: 第二轮 (2026-08-17, 适配器/文档契约/异常边界)
- [x] `SpanAspectMethodInvocation` 适配链复核：PJP args/target/proceed/static method 均正确桥接到 MethodInvocation
- [x] `AnnotationSpanDocumentation` 复核：`annotated.class`、`annotated.method`、before/after/afterFailure 模板与官方 SpanCreator 测试一致
- [x] interface/implementation 双路径由官方正负测试覆盖；未注解 bean 不触发 advice
- [x] 发现并记录异常边界：processor `catch (Exception)`，`Error` 不会进入 `onFailure`；这是当前实现契约/风险，不在 harness 中伪造修复
- [x] harness 14/14 全绿；官方测试补足真实 AspectJ/PJP tag 路径

## 收敛判定 (第二轮终)
MT-4 无新的实现缺陷；唯一边界是源码明确的 `Error` 不走失败标记路径，以及同类自调用受 AOP 代理限制。
