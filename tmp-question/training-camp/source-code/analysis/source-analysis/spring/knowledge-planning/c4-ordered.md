# C-4 Ordered — 排序体系 (Ordered/PriorityOrdered/@Order → OrderComparator)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | Ordered(71行)+PriorityOrdered(47行)+OrderComparator(229行)+AnnotationAwareOrderComparator(145行)+Order(75行)+OrderUtils(146行)
> 基线: C-3 Environment — 配置来源优先级靠 addFirst 手工排 — 本域展开框架级统一排序: Bean 后处理器/拦截器/转换器/异常解析器全部靠它定序; 原始执行计划 0-4

---

## §0.8

- 🟡 Working，1篇 — 三要素(Ordered 接口 getOrder / PriorityOrdered 标记接口 / @Order 注解 value) → 数值语义(越小越前, HIGHEST=MIN_VALUE) → 比较器(OrderComparator: PriorityOrdered 段特判→order 数值比较→未实现兜底 LOWEST) → 注解感知(AnnotationAwareOrderComparator: Ordered→@Order/@Priority→DecoratingProxy) → 场景(拦截器/转换器/异常解析器排序)
- 设计模式: [模式: 策略模式]—Comparator 可插拔; [模式: 优先级分层]—PriorityOrdered 组恒在普通组之前

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Ordered.java:43,49,55,69 | 接口与常量 | **数值契约**: getOrder() 返回 int — HIGHEST_PRECEDENCE=Integer.MIN_VALUE(L49) / LOWEST_PRECEDENCE=Integer.MAX_VALUE(L55) — **值越小优先级越高** | High |
| PriorityOrdered.java:46 | 标记接口 | **PriorityOrdered**: extends Ordered, 无新方法 — "此类对象恒排在非 Priority 之前"(独立于 order 值) | High |
| Order.java:66,73 | @Order 注解 | **注解声明**: value() 默认 LOWEST_PRECEDENCE — 给无接口的对象/Bean 标优先级(类/方法/元注解) | High |
| OrderComparator.java:53,76 | doCompare() | **核心比较**: L78-84 PriorityOrdered 特判(p1&&!p2→-1, 不看 order) → L86-88 getOrder 数值 Integer.compare — 三段排序: Priority 组→普通组→未实现组(LOWEST 兜底 L136) | High |
| OrderComparator.java:126,143 | getOrder()/findOrder() | **取值链**: getOrder: findOrder→null 兜底 LOWEST_PRECEDENCE; findOrder: instanceof Ordered→getOrder() | High |
| AnnotationAwareOrderComparator.java:47,63 | findOrder() 覆写 | **注解感知**: super.findOrder(Ordered 接口)→无→findOrderFromAnnotation(L72: MergedAnnotations.from(TYPE_HIERARCHY)→OrderUtils.getOrderFromAnnotations(L108, 读 @Order/@Priority)→DecoratingProxy 代理类特判) | High |
| OrderComparator.java:172 | sort() | **静态排序**: size>1 才 sort — 1 元素不浪费 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 两个接口+一个注解+两个比较器约 700 行 — 机制单线: "取 order 值 → 比较 → 排序". 1篇 (~44行) 按"声明→比较→感知→场景"展开; 若分 2 篇则"接口/注解声明"与"比较算法"割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | OrderComparator.doCompare 三段排序 (PriorityOrdered 特判→数值比较→LOWEST 兜底) | 🔴 | **为什么🔴**: 框架所有排序(拦截器/转换器/异常解析器/BP 后处理器)的算法核心 — "越小越前"与"Priority 恒前"是两个易错语义 |
| P1-2 | Ordered/@Order/PriorityOrdered 三要素与数值语义 (HIGHEST=MIN_VALUE) | 🔴 | **为什么🔴**: 声明侧的全部知识 — 用什么声明优先级、数值方向(负数=更优先) |
| P1-3 | AnnotationAwareOrderComparator.findOrder 链 (Ordered→@Order/@Priority→DecoratingProxy) | 🔴 | **为什么🔴**: 注解版比较器 — Boot/Web 场景默认用它 — 类继承链/代理上的 @Order 都能识别 |
| P2-1 | PriorityOrdered 为什么独立接口 (三段分组的必要性) | 🟡 | **为什么🟡**: 纯接口设计动机 — 不看数值只看类型 |
| P2-2 | OrderUtils.getOrderFromAnnotations (TYPE_HIERARCHY 搜索) | 🟡 | **为什么🟡**: 注解读取的搜索策略 — 元注解/父类上的 @Order 也可用 |
| P3-1 | sort() 静态工具 (size>1 优化) | 🟢 | **为什么🟢**: 工具细节 — 性能习惯 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **声明三要素** (Ordered/PriorityOrdered/@Order) | 🔴 | 怎么声明优先级 — 接口/标记接口/注解三选一 |
| B | **比较算法** (doCompare 三段 + 取值链) | 🔴 | 怎么排序 — Priority 段优先是核心 |
| C | **注解感知与场景** (AnnotationAware + 使用点) | 🔴 | 注解版比较器 + 框架内应用 |

> **Cluster A (§1)**: Ordered 接口 + 常量语义 + PriorityOrdered + @Order 注解
> **Cluster B (§2)**: OrderComparator.doCompare 三段排序 + getOrder 兜底链
> **Cluster C (§3)**: AnnotationAwareOrderComparator 注解读取链 + 框架使用场景(拦截器/转换器/异常解析器/BP 后处理器)

→ 引出 0-5: 注解元数据 — @Order 能被"继承链搜索"地读到, 靠的是 MergedAnnotation/AnnotatedElementUtils 元数据体系 (@AliasFor 别名)

(End of file - total 61 lines)
