# S1-6 BeanPostProcessor 全景 — doCreateBean 各阶段的 BPP 切入

> 依赖 S1-3 / S1-5 | 🟡 Working | 2 KP

**读者处境**: S1-5 学完 @Autowired(@AutowiredAnnotationBPP) 和 @Resource(CommonAnnotationBPP) — 都是 BPP。Spring 还内置了多少个 BPP？各自在 doCreateBean 的哪个位置切入？如果我想写一个自定义 BPP — 怎么选择接口和时机？

### 1. BPP 接口继承树 — 四个时机对应四个子接口

场景: 写一个 `@Timed` 注解 — 方法执行前后记录耗时。应该实现哪个 BPP 接口？切在初始化前？后？还是实例化前？

源码路径:
- `BeanPostProcessor.java:82,108` — **postProcessBeforeInit/AfterInit** — 最常用接口, 初始化前后切入(Spring 内置的 @PostConstruct 和 AOP 代理都在这些阶段)
- `InstantiationAwareBeanPostProcessor` — **postProcessBeforeInstantiation**(返回非null→跳过后续13步)/**postProcessAfterInstantiation**(返回false→跳过 populateBean)/**postProcessProperties**(@Autowired/@Resource 在此)
- `SmartInstantiationAwareBeanPostProcessor` — **predictBeanType**(预测最终类型, 用于 @Autowired 类型匹配)/**getEarlyBeanReference**(循环依赖提前暴露)
- `DestructionAwareBeanPostProcessor` — **postProcessBeforeDestruction**(@PreDestroy 在此)/**requiresDestruction**(判断是否需要销毁回调)

关键设计: **Why 4 个子接口而非一个大接口？** 每种切入时机有不同语义: BeforeInstantiation 需要返回对象(可替换 Bean), BeforeInit 需要返回对象(可包装 Bean), postProcessProperties 需要修改 PropertyValues。一个大接口会让实现者困惑"这个方法是干什么的"。4 个子接口让每个实现了单一职责 — 实现者只选择需要的。[模式: Interface Segregation]

数据流: `@Component class MyBean` 创建→`doCreateBean`→①`resolveBeforeInstantiation`(L518)→`AbstractAutoProxyCreator.postProcessBeforeInstantiation`→无 TargetSource→返回 null→②`createBeanInstance`→new MyBean→③`populateBean`→`AutowiredAnnotationBPP.postProcessProperties`(L506) 注入 @Autowired 字段→④`initializeBean`→`applyBeanPostProcessorsBeforeInitialization`(L424)→`InitDestroyAnnotationBPP.postProcessBeforeInitialization`(L216) 执行 @PostConstruct→⑤`invokeInitMethods`(L1865)→afterPropertiesSet+initMethod→⑥`applyBeanPostProcessorsAfterInitialization`(L440)→`AbstractAutoProxyCreator.postProcessAfterInitialization`→需要代理→生成 CGLIB 代理→⑦注册 DisposableBeanAdapter→销毁时→`InitDestroyAnnotationBPP.postProcessBeforeDestruction` 执行 @PreDestroy。

### 2. doCreateBean 中的 BPP 时间线 + 关键实现

场景: doCreateBean 13步 — 每步都有 BPP 的切入窗口。理解这个时间线才能知道"我的自定义 BPP 应该在哪个时机做事"。

源码路径:

| 时机 | doCreateBean 步骤 | 关键处理 | 注解 |
|------|:--:|------|------|
| 实例化前 | `resolveBeforeInstantiation()`(L518) | **AbstractAutoProxyCreator**(InstantiationAwareBPP子类) | TargetSource 提前代理 |
| 实例化后 | `populateBean` 中的 `postProcessProperties` | **AutowiredAnnotationBPP**(L506) | @Autowired/@Value |
| 初始化前 | `applyBeanPostProcessorsBeforeInitialization`(L424) | **InitDestroyAnnotationBPP**(L216) | @PostConstruct |
| 初始化中 | `invokeInitMethods`(L1865) | **— (Bean自身接口, 非BPP)** | `afterPropertiesSet`(InitializingBean) + `initMethod`(BD指定) |
| 初始化后 | `applyBeanPostProcessorsAfterInitialization`(L440) | **AbstractAutoProxyCreator.postProcessAfterInitialization** | AOP代理(CGLIB/JDK) |
| 销毁前 | `destroySingletons` → `DisposableBeanAdapter` | **InitDestroyAnnotationBPP.postProcessBeforeDestruction** | @PreDestroy |

关键设计: **Why AOP 和 @Autowired 不在同一个 BPP 中？** AOP 是"代理生成" — 关注 Bean 的切面。@Autowired 是"依赖注入" — 关注 Bean 的依赖。两类关注点完全正交 — 分离为不同 BPP 让各自独立演化和测试。

数据流: 写自定义 BPP→`@Component class TimedAnnotationBPP implements BeanPostProcessor`→容器刷新时被注册→`getBeanNamesForType(BeanPostProcessor.class)` 收集→排序(Ordered)→每次 `doCreateBean` 的 initializeBean→`applyBeanPostProcessorsBeforeInitialization` 遍历全部 BPP→调用 `TimedAnnotationBPP.postProcessBeforeInitialization(bean, beanName)`→扫描 @Timed 方法→缓存 metadata→返回 bean→继续下一个 BPP→全部执行完后 `invokeInitMethods`→`applyBeanPostProcessorsAfterInitialization` 再遍历→`TimedAnnotationBPP.postProcessAfterInitialization`→若需要→生成代理包裹 @Timed 方法→返回代理。

→ 引出 S1-7 FactoryBean — Spring 的另一种"特殊 Bean"机制。BPP 是"加工 Bean"的钩子 — FactoryBean 是"创建 Bean"的工厂。MyBatis Mapper / Dubbo Reference 都是 FactoryBean — 不是 BPP。
