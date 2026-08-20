# A-5 Pointcut 表达式 — AspectJ 9种 Pointcut Primitive + PointcutParser

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | AspectJExpressionPointcut.java(738行, 9种primitive在L88-98)
> 基线: A-3 @AspectJ 解析 — Pointcut 表达式怎么被编译和匹配

---

## §0.8

- 🟡 Working，1篇 — Spring AOP支持的9种AspectJ Pointcut Primitive: execution/within/this/target/args/@annotation/@within/@args/@target + PointcutParser编译

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AspectJExpressionPointcut.java:88-98 | SUPPORTED_PRIMITIVES | Spring AOP支持9种: execution/within/this/target/args/@annotation/@within/@args/@target/reference | High |
| AspectJExpressionPointcut.java:228 | PointcutParser | `initializePointcutParser(classLoader)`→支持9种primitive→parsePointcutExpression→PointcutExpression→evaluate | High |

---

## 02-04 聚合 (1篇)

**P1 核心 (1)** 🔴: 9种Pointcut Primitive的语义和匹配规则 — **为什么** 🔴** 🔴: Pointcut是AOP的"筛选器"——决定advice应用到哪些方法——理解每种primitive的匹配对象(类/方法/参数/注解)是正确编写切面的基础。

**1篇理由**: 738行/1文件, 核心仅9种primitive声明(L88-98) — PointcutParser是AspectJ库。1篇(~35行)列出9种primitive+匹配示例。

| Primitive | 匹配对象 | 示例 |
|:--|------|------|
| execution | 方法签名 | `execution(* com.example.*Service.*(..))` |
| within | 类/包范围 | `within(com.example.service.*)` |
| this | 代理对象类型 | `this(com.example.IUserService)` |
| target | 目标对象类型 | `target(com.example.UserService)` |
| args | 方法参数类型 | `args(java.lang.String,..)` |
| @annotation | 方法注解 | `@annotation(org.springframework.transaction.annotation.Transactional)` |
| @within | 类注解 | `@within(org.springframework.stereotype.Service)` |
| @args | 参数注解 | `@args(jakarta.validation.Valid)` |
| @target | 目标类注解 | `@target(org.springframework.stereotype.Repository)` |
