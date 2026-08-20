# MI-4 AOP / 注解域 — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, 通读 + harness 收敛)
- [x] 通读 10 文件：2 aspect + 2 annotation + 4 tag support + 2 repeatable container
- [x] 主路径闭环：`@Timed/@Counted` → aspect → builder → register → MI-2/MI-3 meter
- [x] 异步闭环：`CompletionStage.whenComplete(...)` 才记录/停止
- [x] `@MeterTag` 三层优先级实证：resolver > expression > toString，null → empty string
- [x] `recordFailuresOnly` 实证：成功路径不记 counter
- [x] `longTask=true` 实证：走 `LongTaskTimer` 路径且结束后 `activeTasks()==0`
- [x] harness `MiniMI4` **20/20 PASS**

## 关键打脸 / 易错点
- 仓内 `TimedAspectTest` / `CountedAspectTest` / `MeterTagSupportTests` 已迁移到 samples module，当前模块几乎没有主体验证；因此本域必须依赖自建 harness
- `TimedAspect` 构造器 `(registry, tagsProvider)` 与 `(registry, shouldSkip)` 存在 lambda 二义性，调用 skip 版本时需要显式 cast 成 `Predicate<ProceedingJoinPoint>`
- `MeterTagSupport` 是 package-private，harness 需要放进 `io.micrometer.core.aop` 包内验证

## 收敛判定
- 第一轮收敛，待下一轮做“切点优先级/类注解vs方法注解去重 + 官方迁移缺口说明 + 残留扫描”。

## 审查轮次: 第二轮 (2026-08-17, 语义漏洞扫描/反射实证)
- [x] 类注解 vs 方法注解去重：`timedClass` pointcut 自带 `!@annotation(Timed)`，因此类上 `@Timed` + 方法上 `@Timed` 不会双记
- [x] `CountedAspect` 同理：`countedClass` pointcut 自带 `!@annotation(Counted)`，避免类/方法重复记数
- [x] **重复 `@Timed` 风险实证**：`Method.getAnnotation(Timed.class)` 在 `@Timed("a") @Timed("b")` 场景下返回 `null`，真正可见的是 `TimedSet` 容器；而 `TimedAspect` 未显式处理 `TimedSet`
- [x] 对照发现：`TimedFinder` (jersey binder) 会同时检查 `Timed` 与 `TimedSet`，AOP 路径未做同样处理
- [x] harness 补强到 **22/22**，新增重复注解元数据断言

## 收敛判定 (第二轮终)
MI-4 主要实现语义已闭环；唯一需要显式记录的风险点是 AOP 路径对重复 `@Timed` 的支持不明确。除该设计/实现风险外，无其他残留问题。


## 审查轮次: 第三轮 (2026-08-17, samples 模块交叉 + 重复注解风险加固)
- [x] 样例模块交叉确认 `class-level vs method-level` 去重:
  - `samples/.../TimedAspectTest.java:493` 调 `annotatedOnMethod()` 后 `registry.getMeters()).hasSize(1)`，说明方法级 `@Timed` 覆盖类级，不会双记
  - `samples/.../CountedAspectTest.java:450` `ignoreClassLevelAnnotationIfMethodLevelPresent()` 明确断言 `class.counted` 不存在、只记录 `method.counted`
- [x] 样例模块未发现任何“重复 `@Timed`”测试用例；AOP 主测试迁移后，该风险仍无人覆盖
- [x] 反证加强：同仓 `TimedFinder` 会同时处理 `Timed` 和 `TimedSet`，说明作者在其他路径已意识到 repeatable 容器问题；AOP 路径未跟进该处理
- [x] harness 补强到 **22/22**，把重复 `@Timed` 的反射可见性差异固化为断言

## 收敛判定 (第三轮终)
MI-4 现阶段唯一剩余的“问题”不是分析残缺，而是源码本身的实现风险：AOP 路径对重复 `@Timed` 缺少显式 `TimedSet` 支持，且样例模块也未覆盖。除此之外，其余语义已闭环。
