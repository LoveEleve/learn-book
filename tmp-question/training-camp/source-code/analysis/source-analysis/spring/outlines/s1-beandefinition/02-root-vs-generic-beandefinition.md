# S1-1 §2 RootBeanDefinition vs GenericBeanDefinition — 合并前 vs 合并后

> 依赖 §1 | 🔴 Deep | 2 KP | [模式: Decorator]

**读者处境**: 知道 BeanDefinition 接口的字段 — 但 Spring 内部处理 BeanDefinition 时用 `RootBeanDefinition` 而非接口。为什么需要两种实现？答案在**合并(Merge)**——parent BeanDefinition 可以被子 BeanDefinition 继承。

### 1. GenericBeanDefinition → RootBeanDefinition — 合并的完整流程

场景: `<bean id="parentDS" abstract="true"><property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/></bean>` + `<bean id="userDS" parent="parentDS"><property name="url" value="jdbc:mysql://localhost/user"/></bean>` — 子 Bean 不需要重复 driverClassName — 从 parent 继承。这在 Spring 1.x XML 时代是常用模式。

源码路径:
- `GenericBeanDefinition.java:46` — **parentName 字段**: "parent bean definition can be flexibly configured through the parentName property" — 标识"这个 BD 有父定义"(全文 102 行，超薄类)
- `RootBeanDefinition.java:259` — **构造器 `RootBeanDefinition(RootBeanDefinition original)`**: 深拷贝所有字段 → 用于 parent-to-child 的合并(全文 682 行)
- `RootBeanDefinition.java` — 持有 `targetType`(已解析的 ResolvableType) / `externallyManagedConfigMembers` / `decoratedDefinition`
- `AbstractBeanFactory.java:1408` — **getMergedBeanDefinition(beanName, bd, containingBd)**: 无 parent → 若已是 RootBeanDefinition 则 `cloneBeanDefinition()`(:1425-1427)，否则 `new RootBeanDefinition(bd)`(:1429)；有 parent → `new RootBeanDefinition(pbd)` 深拷贝 parent + `mbd.overrideFrom(bd)` 用 child 属性覆盖(:1456-1457)；最后若 scope 未配置 → 默认 SCOPE_SINGLETON(:1461-1462)。**targetType 不在合并时解析** — 它由 `RootBeanDefinition.getResolvableType()` (RootBeanDefinition.java:369) 惰性解析；**autowireMode 仅来自 XML autowire 属性**，合并不做任何推断

关键设计: **Why GenericBD 只有 102 行而 RootBD 有 682 行？** GenericBD 是"用户写的原始配置" — 只存 BeanDefinition 接口的基本字段 + parentName。RootBD 是"合并后 + post-process 后"的形态 — 多了: `targetType`(类型为 ResolvableType，RootBeanDefinition.java:79，由 getResolvableType() 在 RootBeanDefinition.java:369 惰性解析 — 优先 targetType → factoryMethodReturnType → 反射方法返回类型 → 最后超类实现按 beanClassName 懒解析)、`externallyManagedConfigMembers`(被外部管理的配置)、`decoratedDefinition`(装饰后的定义)。**GenericBD = 用户视角的 Bean 定义，RootBD = Spring 内部视角的处理后定义。** [模式: Decorator — RootBD 装饰 GenericBD, 添加内部属性]

数据流: XML 解析→`new GenericBeanDefinition()` 设 parentName="parentDS"→`beanFactory.registerBeanDefinition("userDS", genericBD)`→`beanDefinitionMap.put("userDS", genericBD)`→第一次 `getBean("userDS")`→`getMergedLocalBeanDefinition("userDS")`→`mergedBeanDefinitions` 缓存无→从 `beanDefinitionMap` 取 genericBD→genericBD.getParentName()="parentDS"→递归获取 parent 的 `RootBeanDefinition`(parent 无自己的 parent → 直接从其 GenericBD 构造 RootBD)→`new RootBeanDefinition(parentRootBD)` 深拷贝→覆盖 child 的 scope/lazyInit/propertyValues→存入 `mergedBeanDefinitions.put("userDS", mergedBD)`→返回 RootBD → 后续 `getBean()` 直接从缓存取合并后的 RootBD。

`mergedBeanDefinitions` 缓存保证每个 Bean 只合并一次 — 合并结果被并发 getBean 安全共享(ConcurrentHashMap, AbstractBeanFactory.java:170)。

parent 链可多级嵌套(AbstractBeanFactory.java:1438/1442 递归 getMergedBeanDefinition)— 每层递归都走同一模板。

### 2. 为什么 @Component Bean 也要走合并？

场景: Spring Boot `@Component class UserService` — 没有 XML parent。为什么 `getBean("userService")` 也是先获取 `RootBeanDefinition`？答案: BeanFactory 的 `getBean()` 只认 RootBD — 合并是统一入口，不管配置来源有没有 parent。这也解释了为什么注解 Bean 和 XML Bean 在容器内行为完全一致 — 它们最终都变成同一种 RootBD 形态。

源码路径:
- `ClassPathBeanDefinitionScanner` — 扫描 `@Component` → 创建 `ScannedGenericBeanDefinition`(GenericBD 子类) → `beanDefinitionRegistry.registerBeanDefinition(beanName, scannedBD)`
- `AbstractBeanFactory.getMergedLocalBeanDefinition(beanName)` — **即使没有 parent** — 也会合并: 从 ScannedGenericBD → `new RootBeanDefinition(scannedBD)` 拷贝为 RootBD(AbstractBeanFactory.java:1423-1430) → 若 scope 未配置则补默认 singleton(:1461-1462)。合并不解析 class、不推断 autowireMode
- `AbstractBeanFactory.java:1432-1452` — 有 parent 时递归 `getMergedBeanDefinition(parentName, ...)` 取 parent 合并结果，再深拷贝+overrideFrom — 父子链可以多级嵌套

关键设计: **Why 即使没有 parent 也要走合并？** 合并本身只做三件事 — 无 parent 直接 `new RootBeanDefinition(bd)`、有 parent 则深拷贝 + `overrideFrom(bd)` 覆盖、scope 缺省补 singleton。**每个 Bean 最终都变成一个 RootBD — 不管有没有 parent。** 至于 targetType/泛型的解析，不在合并时发生 — 那由 `getResolvableType()` (RootBeanDefinition.java:369) 在 `determineTargetType()` 被调用时才惰性执行；autowireMode 也不是合并推断的 — 只来自 XML 的 `autowire` 属性。合并后统一形态带来的好处: BeanFactory 的处理逻辑(实例化/属性填充/销毁)只面对一种 BD 类型，无需为 GenericBD/RootBD 写两套分支 — 这是"配置源多样性 → 内部处理统一"的经典收敛模式。

副作用: 每次 BeanDefinition 变更(如 BPP 修改属性)后需将 `stale` 标记为 true (RootBeanDefinition.java:72 字段, AbstractBeanFactory.java:1538 赋值) 使 `mergedBeanDefinitions` 缓存失效 — 合并缓存与配置一致性是这里的设计权衡。

数据流: `@Component class UserService`→ClassPathBeanDefinitionScanner→`new ScannedGenericBeanDefinition(metadata)`(ClassPathScanningCandidateComponentProvider.java:424)→registerBeanDefinition("userService", scannedBD)→存入 beanDefinitionMap→getBean("userService")→`getMergedLocalBeanDefinition("userService")`→mergedBeanDefinitions 缓存未命中→`getMergedBeanDefinition`(:1408)→scannedBD.getParentName()==null→scannedBD 不是 RootBD→`new RootBeanDefinition(scannedBD)`(:1429) 拷贝全部属性→scope 未配置→setScope(SCOPE_SINGLETON)(:1461-1462)→存入 mergedBeanDefinitions 缓存→返回 RootBD→后续 getBean 直接命中缓存，不再合并。

→ 引出 S1-2 BeanFactory — BeanDefinition 定义了什么 Bean — BeanFactory 通过 `registerBeanDefinition()` 将 BD 存入 `beanDefinitionMap` — `getBean()` 时从 Map 取出 BD→解析 class→实例化。但实例化不是一行 `new` — getBean 内部走了完整的 Bean 生命周期。
