# A-3 @AspectJ 解析 — ReflectiveAspectJAdvisorFactory + AspectJExpressionPointcut

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 3文件/~1225行
> 基线: A-2 Advice 链 — @Aspect/@Before/@After 注解方法被转换为 Advisor 后进入 proceed 链

---

## §0.8

- 🟡 Working，1篇 — ReflectiveAspectJAdvisorFactory.getAdvisors + AspectJExpressionPointcut + AnnotationAwareAspectJAutoProxyCreator
- 设计模式: [模式: 适配器]—把@Aspect注解类适配为AOP Alliance Advisor链

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ReflectiveAspectJAdvisorFactory.java:125 | getAdvisors() | **核心**: 遍历@Aspect类的方法→对每个@Before/@After/@Around/@AfterReturning/@AfterThrowing→L229 getPointcut→AspectJExpressionPointcut→L238 setExpression(pointcutExpr)→new InstantiationModelAwarePointcutAdvisorImpl→返回Advisor列表 | High |
| AspectJExpressionPointcut.java:83 | setExpression() | Pointcut表达式: execution/within/this/target/args/@annotation → AspectJ PointcutParser编译 → PointcutExpression → matches(method, targetClass) | High |
| AnnotationAwareAspectJAutoProxyCreator.java:50 | 自动检测 | 继承AbstractAutoProxyCreator—findCandidateAdvisors→BeanFactoryAspectJAdvisorsBuilder.buildAspectJAdvisors—扫描所有@Aspect Bean→逐个getAdvisors→返回Advisor→createProxy | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**P1 核心 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | ReflectiveAspectJAdvisorFactory.getAdvisors — @Aspect类→Advisor链的完整转换流程(L125-L238) | 🔴 | **为什么**: @AspectJ的核心—通过反射读取每个注解方法→提取pointcut表达式→创建Advisor→Spring AOP与AspectJ集成的基础 |
| P1-2 | AspectJExpressionPointcut.setExpression — Pointcut Parser编译+matches匹配 | 🔴 | **为什么**: Pointcut的核心执行—AspectJ表达式被PointcutParser编译为PointcutExpression→matches(method, clazz)→判断方法是否被拦截 |

**1篇理由**: ~1225行/3文件—核心是getAdvisors转换(334行)+Pointcut匹配(738行)。1篇(~42行)。

**单篇结构**: §1 ReflectiveAspectJAdvisorFactory.getAdvisors 转换流程 → §2 AspectJExpressionPointcut Pointcut匹配 + AnnotationAwareAspectJAutoProxyCreator 自动检测
