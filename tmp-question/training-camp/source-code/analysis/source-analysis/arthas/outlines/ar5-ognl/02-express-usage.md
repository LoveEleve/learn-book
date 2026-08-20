# 02. params[0]>100 在哪一行被求值? — 表达式的生效点

> 🔴 Deep | 10 KP 中的 4 个(isConditionMet/getExpressionResult/ognl 命令/cost)
> 读者处境: 你知道表达式引擎长什么样了——现在定位它**在哪些命令的哪些代码行被触发**,以及输出表达式怎么"取回值"。

### 1. "条件与输出是两个方法" — isConditionMet / getExpressionResult

场景: `watch -e 'params[0] > 100' 'params[0]'`——一个做"判断",一个做"取值",实现上分开了。

- `AdviceListenerAdapter`(core/advisor/AdviceListenerAdapter.java:18):
  - `isConditionMet(String conditionExpress, Advice advice, double cost)`(:132-135): `StringUtils.isEmpty(expr) || ExpressFactory.threadLocalExpress(advice).bind(Constants.COST_VARIABLE, cost).is(expr)`——**空表达式 = 无条件(放行)**;否则求值判真
  - `getExpressionResult(String express, Advice advice, double cost)`(:137-139): `.get(express)`——取值(可为任意对象)
- 调用方(AR-2 篇 4 已述): WatchAdviceListener.watching(:80/:87)——**先 isConditionMet 判,再 getExpressionResult 取**;Stack/Trace/Monitor/TT 同理(全部继承 AdviceListenerAdapter)
- `Constants.COST_VARIABLE = "cost"`(Constants.java:40)——表达式里 `cost` 变量 = 该方法耗时(ms)
- [Java: `threadLocalExpress(advice).bind(...)` 链式——**同一个池里的对象被反复 reset+bind**;is 和 get 是两次独立求值(同一表达式可能被求两次: 条件一次、输出一次)]

关键设计: [模式: 守卫条件(空串短路)+ 池 vs 新建双策略(threadLocal/unpooled)] **空表达式短路**: 不带 `-e` 时 isConditionMet 直接 true,不进 OGNL——**零条件 = 零表达式开销**(watch 高频场景的性能底线,AR-2 篇 3 §5 的"无反射无锁"在此延续)。

### 2. "表达式环境" — Advice 即根对象

场景: 表达式里能写 `params[0]`/`target`/`returnObj`/`throwExp`/`clazz`/`method`——这些名字从哪来?

- 根对象 = `Advice`(AR-2 篇 3 §4 的数据模型)——OGNL 在根对象上取值,所以字段名即表达式变量名
- `method` 是 ArthasMethod(可 `.getName()`);`cost` 是 bind 进去的命名变量(不在 Advice 字段里,`bind(String, Object)` 的用法)
- 条件上下文小结(Constants.java:12-18 的 EXPRESS_DESCRIPTION 原文): target/clazz/method/params/params[0..n]/returnObj/throwExp/cost

关键设计: **"环境 = 数据模型"**: 表达式能力完全由 Advice 字段决定——新增变量 = 给 Advice 加字段,表达式立即可用。这就是为什么 tt -s/-w、monitor -c 共用同一套表达式: 它们的 Advice 是同一模型(AR-2 篇 4)。

### 3. "独立的表达式入口" — ognl 命令与 tt 搜索

场景: 不在 watch 里,也能直接执行表达式——`ognl` 命令、`tt -s`。

- `ognl` 命令(klass100/OgnlCommand.java:31-40): `@类@静态成员` / `@类@方法()` / `#var=..., {#var}` 语法;`-x` 展开深度 / `-c` classLoaderHash(用 `unpooledExpress`,按指定 CL 建一次性的 OgnlExpress)
- `tt -s`(TimeTunnelCommand.java:398-440): `ExpressFactory.threadLocalExpress(advice).is(searchExpress)`(:409)——对**每个录制的 TimeFragment** 的 advice 求值(筛选)
- `tt -w`(TimeTunnelCommand.java:370-395): `unpooledExpress(advice.getLoader()).bind(advice).get(watchExpress)`(:381)——对**单个** TimeFragment 取字段
- 差异: threadLocal(池,批量筛选)vs unpooled(一次性,单对象取详情)

关键设计: **池 vs 一次性,是按调用频次选**: 高频批量(tt -s 遍历所有碎片)用池复用;低频单次(tt -w/ognl)每次新建——成本与复杂度都匹配场景。

### 4. 表达式全链路

```
watch -e 'params[0] > 100' com.example.Service doBiz
  → 织入 SpyAPI(AR-2 篇 2)
  → 业务调用 → SpyImpl → AdviceListenerAdapter.before(AR-2 篇 3)
  → WatchAdviceListener.watching(AR-2 篇 4)
  → isConditionMet('params[0] > 100', advice, cost)
    → ExpressFactory.threadLocalExpress(advice).bind('cost', cost).is(expr)
      → OgnlExpress.get → Ognl.getValue(expr, context, advice)
      → instanceof Boolean && (Boolean)
  → 命中 → getExpressionResult → WatchModel → 输出
```

关键设计: 表达式是这条链的**最后一层闸门**——字节码已织入、回调已发生,表达式决定"这条记录要不要留下"。所以它的性能只影响"每次调用多一次 OGNL 求值",不影响增强本身。

---

跨域桥: watching 调用点 = AR-2 篇 4;Advice 模型 = AR-2 篇 3;tt -s/-w = AR-2 篇 4 §4;ognl -c 使用 = AR-0 篇 6;cost 变量与 AR-0 篇 4 的耗时解读对应。

---

**OpenJDK 关联**:  [OpenJDK 域 33 JMX — outlines/33-jmx-management/] — 表达式环境(Advice)与 JMX 的属性访问模型可对照(都是反射式读取运行时对象)。

### 核心悬念

**"表达式是'最后一层闸门'——那不带条件时它零开销吗?"** — 是: 空表达式直接短路,连 OGNL 都不进。但如果你要"全景"而不是"单点判断",还有另一种完全不插桩的观测手段——采样。

> → [AR-6 篇 2](../ar6-profiler/02-profiler-boundary.md)