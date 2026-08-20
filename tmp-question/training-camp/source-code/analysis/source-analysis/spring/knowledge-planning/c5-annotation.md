# C-5 注解元数据 — MergedAnnotation/@AliasFor/AnnotatedElementUtils

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | MergedAnnotation(663行)+MergedAnnotations(684行)+AnnotatedElementUtils(886行)+AliasFor(206行)+AnnotationTypeMapping(820行)+AnnotationTypeMappings(292行)+AnnotationUtils(1369行)
> 基线: C-4 Ordered — @Order 的 TYPE_HIERARCHY 搜索、父类注解生效, 靠的就是本域体系; 原始执行计划 0-5

---

## §0.8

- 🟡 Working，1篇 — 新体系(MergedAnnotation 视图: 直接/元注解/距离/合并取值; MergedAnnotations 集合+SearchStrategy) → @AliasFor 双语义(注解内别名+元注解属性映射: @Service.value→@Component.value) → 映射链(AnnotationTypeMapping: source/distance/aliasMappings) → 便捷 API(AnnotatedElementUtils: isAnnotated/getMergedAnnotation + TYPE_HIERARCHY) → 与老 AnnotationUtils 的取舍
- 设计模式: [模式: 代理/视图]—MergedAnnotation 是合成视图 (SynthesizedMergedAnnotationInvocationHandler); [模式: 链式映射]—AnnotationTypeMapping 沿元注解链传递

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MergedAnnotation.java:61,91,100 | 接口 | **注解视图**: isDirectlyPresent(直接声明) / isMetaPresent(元注解上出现) — 一个"合并视角"的注解, 属性值跨元注解链解析 | High |
| MergedAnnotation.java:148,156,392 | getMetaSource/getRoot/getValue | **链定位**: getMetaSource(直接元注解) / getRoot(最外层) / getValue(name)(按合并语义取值) — 取值会沿 @AliasFor 映射回源 | High |
| MergedAnnotations.java:155 | 集合接口 | **集合视图**: from(AnnotatedElement, SearchStrategy) — TYPE_HIERARCHY/INHERITED_ANNOTATIONS/DIRECT — 类/方法/字段统一入口 | High |
| AliasFor.java:181,197,204 | 注解 | **@AliasFor 双语义**: attribute=同注解内别名; annotation+attribute=元注解属性映射(@Service.value→@Component.value) | High |
| AnnotationTypeMapping.java:52,110,115 | 映射链 | **元注解映射**: source(上一级映射)+distance(离根距离)+aliasMappings(别名索引) — 构建 @Service→@Component 的"合成注解" | High |
| AnnotationTypeMappings.java:49,65 | 构建器 | **映射构建**: forAnnotationType → addAllMappings(递归收集元注解) → afterAllMappingsSet(解析别名) — 每个注解类型缓存一份映射 | High |
| AnnotatedElementUtils.java:208,335 | 便捷 API | **静态工具**: isAnnotated(element, type) / getMergedAnnotation(element, type) — 内部走 MergedAnnotations; find 系列(TYPE_HIERARCHY 含父类/接口) | High |
| AnnotationUtils.java(1369行, 旧体系) | 兼容 | **新旧并存**: AnnotationUtils 是老 API(5.3 前), 新代码用 MergedAnnotation 系 — 两者语义差异(合并 vs 单层)是兼容层存在原因 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+实现+工具约 4900 行 — 但概念只有一条链: "注解→MergedAnnotation 视图→沿元注解链合并取值". 1篇 (~46行) 按"概念→别名→链→工具"展开; 若分 2 篇则 @AliasFor 与映射链割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | MergedAnnotation 合并视图 (直接/元注解/距离 + getValue 合并取值) | 🔴 | **为什么🔴**: 整个注解体系的核心心智 — "读注解=读合并后的合成视图"而非单个注解 |
| P1-2 | @AliasFor 双语义 (注解内别名 + 元注解属性映射) | 🔴 | **为什么🔴**: @Service/@RestController/@RequestMapping 等组合注解全部靠它 — 属性继承的机制 |
| P1-3 | AnnotationTypeMapping 映射链 (source/distance/aliasMappings 构建) | 🔴 | **为什么🔴**: 合并取值的底层数据 — "注解+元注解链"如何表达为映射图 |
| P2-1 | AnnotatedElementUtils 便捷 API (isAnnotated/getMergedAnnotation/TYPE_HIERARCHY) | 🟡 | **为什么🟡**: 日常代码接触最多的入口 — 框架内部也用(feign/validation/async 探测) |
| P2-2 | MergedAnnotations 与 SearchStrategy (DIRECT/TYPE_HIERARCHY 差异) | 🟡 | **为什么🟡**: 搜索策略决定"哪层注解可见" — 排查"注解不生效"的第一步 |
| P3-1 | 新旧体系并存 (AnnotationUtils vs MergedAnnotation) | 🟢 | **为什么🟢**: 兼容历史 — 知道为什么有两个 API |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **合并视图** (MergedAnnotation/MergedAnnotations/搜索策略) | 🔴 | 读注解的新心智模型 |
| B | **别名与映射链** (@AliasFor + AnnotationTypeMapping/AnnotationTypeMappings) | 🔴 | 属性合并的底层机制 — 组合注解的基础 |
| C | **工具入口** (AnnotatedElementUtils + 新旧 API) | 🟡 | 日常使用与框架内部调用点 |

> **Cluster A (§1)**: MergedAnnotation 接口(直接/元注解/距离/取值) + MergedAnnotations 集合与搜索策略
> **Cluster B (§2)**: @AliasFor 双语义 + @Service→@Component 例子 + AnnotationTypeMapping 链构建
> **Cluster C (§3)**: AnnotatedElementUtils 静态 API + 框架使用场景(组件扫描/@Transactional 继承) + 新旧体系对照

→ 引出 0-6: Profile — @Profile 注解本身依赖 MergedAnnotation 体系读取 + ConditionEvaluator 判定 — 注解元数据的第一个实际消费者

(End of file - total 61 lines)
