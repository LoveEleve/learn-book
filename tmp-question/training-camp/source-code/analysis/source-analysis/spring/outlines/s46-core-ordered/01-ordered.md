# C-4 Ordered — 排序体系 (三要素 → 比较算法 → 注解感知)

> 依赖 C-3 Environment | 🟡 Working | 6 KP | [模式: 优先级分层 + 策略]

**读者处境**: 多个拦截器、多个 @ControllerAdvice、多个 BeanPostProcessor — 执行顺序谁定？@Order(1) 和 @Order(-1) 谁先？PriorityOrdered 和 Ordered 什么关系？

### 1. Ordered / PriorityOrdered / @Order — 声明优先级的三种方式

场景: 框架里到处是"顺序": 拦截器 preHandle 顺序、转换器注册顺序、BPP 执行顺序 — 声明方式三种: 实现接口、实现标记接口、打注解。

源码路径:
- `Ordered.java:43,49,55,69` — **接口**: getOrder() 返回 int — **值越小优先级越高** — HIGHEST_PRECEDENCE=Integer.MIN_VALUE(L49) / LOWEST_PRECEDENCE=Integer.MAX_VALUE(L55)
- `PriorityOrdered.java:46` — **标记接口**: extends Ordered, 无新方法 — 语义: "我是最高优先级的组成部分, 恒排在其他 Ordered 之前"(不看 order 值)
- `Order.java:66,73` — **@Order 注解**: value() 默认 LOWEST_PRECEDENCE — 给无接口的对象(Bean/组件)标优先级, 类/方法/元注解都可用

关键设计: **Why 数值语义"越小越前"且负数合法？** 数值化让"插入新组件到最前"无需改已有值 — 默认组件用 0/大数, 扩展组件用负数即可插队; HIGHEST=MIN_VALUE 保证"想多前有多前"。**Why PriorityOrdered 独立接口？** 框架内部组件(如基础设施 BPP)必须最先跑 — 若靠数值, 用户 @Order(-100000) 可能反超 — 接口标记让"基础设施恒优先"不可覆盖。[模式: 优先级分层]

数据流: 无。声明侧: `class A implements Ordered { getOrder() { return 0; } }` / `class B implements PriorityOrdered { getOrder(){ return 999; } }` / `@Order(1) class C {}` — 排序时 A、C 按数值, B 无论数值恒先。

### 2. OrderComparator — 三段排序算法

场景: 把上面三类对象混在一个 List 里排序 — 算法: Priority 恒前 → 其余按 order 数值 → 没实现的兜底最后。

源码路径:
- `OrderComparator.java:53,76` — **doCompare()**: L78-79 p1=o1 instanceof PriorityOrdered / p2=o2 instanceof — L80-84: p1&&!p2→-1, p2&&!p1→1(类型决定, 不看数值) → L86-88: getOrder(o1)/getOrder(o2) → Integer.compare — 结果: 三段排序
- `OrderComparator.java:126,143` — **取值链**: getOrder(): findOrder(obj) → null 则兜底 LOWEST_PRECEDENCE; findOrder(): instanceof Ordered → getOrder() — **未实现接口的对象排最后**
- `OrderComparator.java:172` — **sort()**: 静态工具, size>1 才排序(1 个元素不浪费)

关键设计: **Why "未实现兜底 LOWEST" 而非抛错？** 容器里的对象常混有"不知道顺序"的第三方组件 — 兜底让它们天然排最后, 既不破坏排序也不强制实现接口。**Why Priority 特判在数值比较之前？** 三段语义: 基础设施组(必须最先) → 业务组(数值排序) → 无声明组(最后) — 若先比数值, PriorityOrdered 的 order=999 会被普通 @Order(-1) 反超, 破坏分层。[模式: 三段分层]

数据流: sort([C@1, A(Ordered,0), B(Priority,999), X(无接口)]) → doCompare 两两比较 → B 恒在最前 → A vs C: 0 vs 1 → A 前 → X 兜底最后 → [B, A, C, X]。

### 3. AnnotationAwareOrderComparator — 注解感知版 + 框架使用场景

场景: @ControllerAdvice 上写 @Order(1)、@Order(-1) — 框架怎么读到注解值？为什么继承父类的 @Order 也有效？

源码路径:
- `AnnotationAwareOrderComparator.java:47,63` — **findOrder() 覆写**: super.findOrder(仅 Ordered 接口)→无→findOrderFromAnnotation(L72): MergedAnnotations.from(element, **SearchStrategy.TYPE_HIERARCHY**) → OrderUtils.getOrderFromAnnotations(L108: 读 @Order 与 jakarta @Priority) → 仍无且是 DecoratingProxy→代理类再查
- `OrderComparator.java:160(父类 getPriority 返回 null)` — AnnotationAware 覆写 getPriority 提供 @Priority 语义(与 CDI 对齐)
- 使用场景: `DispatcherServlet.java:611,657,696,761` — 排序 handlerMappings/handlerAdapters/handlerExceptionResolvers/viewResolvers; `AbstractAdvisorAutoProxyCreator.java:163` — 排序 advisors; `ContentNegotiatingViewResolver.java:207` — 排序委托 viewResolvers

关键设计: **Why TYPE_HIERARCHY 搜索？** @Order 可能写在父类/接口/元注解上 — 单看目标类会漏; TYPE_HIERARCHY 沿继承链+接口+元注解找(基于 C-5 的 MergedAnnotation 体系)。**Why DecoratingProxy 特判？** AOP 代理对象 getClass() 是代理类, 注解在目标类上 — 需要还原被代理类再查。[模式: 搜索策略]

数据流: DispatcherServlet 启动 → initHandlerMappings(L611): 收集容器里所有 HandlerMapping bean → AnnotationAwareOrderComparator.sort → @Order/Ordered 值小的排前 → handlerMapping 责任链顺序确定(自定义 mapping 想先匹配→@Order 负数)。同样机制排序 handlerAdapters(L657)/异常解析器(L696)/viewResolvers(L761) 与 advisors(L163)。

→ 引出 0-5: 注解元数据 — 上面 TYPE_HIERARCHY 搜索、@Order 读值, 背后是 MergedAnnotation/@AliasFor/AnnotatedElementUtils 元数据体系。
