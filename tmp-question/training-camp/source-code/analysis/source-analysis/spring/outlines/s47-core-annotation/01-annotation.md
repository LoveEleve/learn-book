# C-5 注解元数据 — MergedAnnotation/@AliasFor/AnnotatedElementUtils

> 依赖 C-4 Ordered | 🟡 Working | 6 KP | [模式: 合成视图 + 链式映射]

**读者处境**: `@RestController` 上没写 @Component, 但组件扫描能扫到它 — 因为 @RestController 是 @Controller 的元注解, @Controller 又带 @Component。这套"元注解链+属性合并"怎么实现的？@AliasFor 有什么用？

### 1. MergedAnnotation/MergedAnnotations — 注解的"合并视图"

场景: 读一个注解时, 需要看到的不只是它自己 — 还有它的元注解链 (@RestController→@Controller→@Component)。Java 反射 getAnnotation 只能看一层, Spring 需要"合并视图"。

源码路径:
- `MergedAnnotation.java:61,91,100` — **接口**: isDirectlyPresent(直接声明在目标上) / isMetaPresent(通过元注解链可达) — 一个"合成视图", 属性值跨链解析
- `MergedAnnotation.java:148,156,392` — **链定位**: getMetaSource(直接上级元注解) / getRoot(链根, 即 @Component) / getValue(name)(按合并语义取属性值)
- `MergedAnnotations.java:155` — **集合**: from(AnnotatedElement, SearchStrategy) — 搜索策略: DIRECT(仅目标上)/INHERITED_ANNOTATIONS(继承)/TYPE_HIERARCHY(类+接口+父类+元注解)
- `AnnotationUtils.java:1369(总行)` — **旧体系**: 5.3 前的 API — getAnnotation/findAnnotation — 新代码统一走 MergedAnnotation 系(语义更严谨: 合并/注解不存在区分)

关键设计: **Why 不直接用 Java 反射 getAnnotation？** 反射只能拿到"直接标注的注解" — @RestController 上读不到 @Component, @ComponentScan 就扫不到; MergedAnnotation 把"目标类+元注解链"折叠成一个可查询的视图 — **读一个注解=读整条链**。[模式: 合成视图 — 链折叠为单视图]

数据流: MergedAnnotations.from(RestControllerClass, TYPE_HIERARCHY) → 内部构建映射图: RestController(直接) → meta: Controller → meta: Component → 对任意 query "isPresent(Component)" → 链上可达 → true。距离: RestController=distance0, Controller=1, Component=2。

### 2. @AliasFor — 注解内别名 + 元注解属性映射

场景: `@Service("userService")` — 这个 value 最终要落到 @Component 的 value 上(组件名)。@Service 只有 value 属性, 怎么把值传给元注解？答案: @AliasFor。

源码路径:
- `AliasFor.java:181,197,204` — **注解**: attribute(同注解内另一属性)/ annotation+attribute(指向元注解的属性) — 两种用法
- `Service.java`(spring-context/stereotype) — **经典例**: `@AliasFor(annotation = Component.class) String value()` — @Service.value 是 @Component.value 的别名
- `AnnotationTypeMapping.java:52,110,115` — **映射链**: source(上一级 AnnotationTypeMapping)+distance(离根距离)+aliasMappings(属性别名索引, L121) — 沿链逐级映射属性
- `AnnotationTypeMappings.java:49,65` — **构建**: forAnnotationType → addAllMappings(递归收集元注解) → afterAllMappingsSet(别名解析) — 每注解类型缓存一份映射(AnnotationTypeCache)

关键设计: **Why 用注解声明别名而非硬编码映射？** 声明式: @Service 的作者写一行 @AliasFor 就建立了"属性继承"关系 — 框架读取时无需知道每个组合注解的特例。**Why 属性值合并沿链传递？** @Service("x") → @AliasFor → @Component.value="x" — 组件扫描读 @Component 的 value 即得 bean 名; 若 @Service 未声明 value, 用 @Component 默认值空。[模式: 声明式映射]

数据流: @Service("userService") class UserService → 组件扫描 isAnnotated(Component)→true → getMergedAnnotation(Component) → 链: Service→(AliasFor)→Component → value 合并: @Service 的 value="userService" 通过 aliasMappings 映射到 Component.value → beanName="userService"。

### 3. AnnotatedElementUtils — 便捷入口与框架使用

场景: 框架内部到处要"判断有没有注解+读属性" — 每个调用方都自己构建 MergedAnnotations 太啰嗦 — 静态工具类收口。

源码路径:
- `AnnotatedElementUtils.java:208,335` — **isAnnotated(element, type)**(内部 getAnnotations(element).isPresent) / **getMergedAnnotation(element, type)** — 语义: 含元注解链; find 系列加 TYPE_HIERARCHY(父类/接口)
- `AnnotatedElementUtils.java:252` — getMergedAnnotationAttributes(读属性 Map, @Transactional 事务属性合并入口)
- 使用场景: `ConfigurationClassParser`(组件扫描/@Import 判定); `TransactionalAnnotationParser`(@Transactional 从类继承到方法); `ScheduledAnnotationBeanPostProcessor`(@Scheduled 探测); `AsyncAnnotationBeanPostProcessor`(@Async)

关键设计: **Why 需要 getMergedAnnotationAttributes(属性 Map)？** 有些注解(如 @Transactional)的属性需要"类级默认+方法级覆盖"的合并 — 单值 getValue 不够, 要整组属性 + 标注来源(distance)合并。[模式: 门面 — 静态收口]

数据流: @Transactional(rollbackFor=X) 在类上 + 方法无注解 → TransactionalAnnotationParser → AnnotatedElementUtils.findMergedAnnotationAttributes(method, Transactional.class) → TYPE_HIERARCHY 找到类级注解 → 合并属性(rollbackFor=X, propagation 默认) → 事务属性对象。@RestController → isAnnotated(Component) → 组件扫描注册。

→ 引出 0-6: Profile — @Profile 判定: 注解读取走本域 MergedAnnotation, 判定逻辑在 ConditionEvaluator — 条件装配的第一课。
