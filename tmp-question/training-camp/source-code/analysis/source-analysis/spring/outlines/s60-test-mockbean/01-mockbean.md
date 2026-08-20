# C-18 @MockBean — 测试替身注入 (声明 → 替换 → 创建)

> 依赖 C-17 TestContext | 🟡 Working | 6 KP | [模式: BFPP + 代理替身]

**读者处境**: `@MockBean UserService userService` — 测试里用它替换真实 UserService, 让依赖它的 Bean 自动拿到 mock — 这是怎么做到的？为什么不需要手动 set 进去？

### 1. @MockBean 注解 + MockitoPostProcessor 收集

场景: 测试类字段标 @MockBean → 容器启动时, 一个后处理器扫描到该注解, 为该类型生成 mock bean 并替换真实 bean。

源码路径:
- `MockBean.java:103,119` — **注解**: value() 指定要 mock 的类型(按类型替换) — 标在测试类字段
- `MockitoPostProcessor.java:86,132` — **后处理器**: implements InstantiationAwareBeanPostProcessor + BeanDefinitionRegistryPostProcessor(可改 BeanDefinition) — postProcessBeanFactory L132: L138 注册 MockitoBeans 单例(收集所有 mock) → DefinitionsParser 扫描配置类里的 @MockBean/@SpyBean 字段 → 逐个 register

关键设计: **Why 用 BeanDefinitionRegistryPostProcessor？** 必须在容器实例化 Bean 之前替换定义 — BFPP 在 refresh 早期执行, 此时可增删改 BeanDefinition; 若用普通 BPP(实例化后)则真实 bean 已创建, 无法整体替换。[模式: BFPP — 启动期改定义]

数据流: 测试启动 → refresh → MockitoPostProcessor.postProcessBeanFactory → 扫描测试配置类的 @MockBean UserService 字段 → 生成 MockDefinition → register(定位要替换的 beanName)。

### 2. 替换机制 — 定位 beanName + 覆盖定义

场景: register 时怎么知道替换哪个 bean？接口多个实现怎么办？没有同类型 bean 呢？

源码路径:
- `MockitoPostProcessor.java:179` — **registerMock**: L190 definition.createMock(mock) → 建 RootBeanDefinition(mockType)
- `MockitoPostProcessor.java:209` — **定位 beanName**: ①@MockBean(name)→直接用该 name; ②getExistingBeans(同类型+qualifier): 空→新生成 beanName; 单个→**替换该 beanName**(覆盖其定义); 多个→取 primary; 无 primary→抛 IllegalStateException
- 替换后: 原 beanName 的 BeanDefinition 被替换为 mock 的 RootBeanDefinition → 所有 @Autowired/依赖注入拿到的都是 mock

关键设计: **Why 定位要区分多实现？** 接口有多个实现时无法确定"替换哪个" — 规则: 唯一→直接换; 多实现→必须 @Primary 或 @MockBean(name) 指定; 否则报错(避免注入错 mock)。这正是依赖注入"歧义"问题在测试替换上的重现。[模式: 定义替换 + 唯一性约束]

数据流: @MockBean UserService(只有一个实现)→ getExistingBeans(UserService)→["userService"]→ 返回该 beanName → 覆盖其 BeanDefinition 为 mock 定义。若 UserService 有两个实现→多个→需 @Primary 或 name 指定。注入 OrderService 里的 UserService → 容器给 mock。

### 3. MockDefinition 创建 + 对照与缓存

场景: mock 具体怎么生成？@SpyBean 什么区别？为什么换 mock 组合会导致容器重建？

源码路径:
- `MockDefinition.java:62,144,149` — **createMock**: L149 `Mockito.mock(mockType, settings)`; answer 默认 `RETURNS_DEFAULTS`(L62 — 未 stubbing 返回默认值: 对象 null/数字 0/集合空)
- `MockDefinition.java` 与 `SpyDefinition`: @MockBean(全替身, 方法返回默认) vs @SpyBean(部分真实 — 用真实实例包 Mockito spy, 未 stub 走真实逻辑)
- 与 TestContext 缓存: C-17 的 MergedContextConfiguration 键含 mock 定义 — **不同 @MockBean 组合→不同缓存键→容器重建**

关键设计: **Why 默认 RETURNS_DEFAULTS？** mock 未配置行为时返回"无害默认值"(null/0/空集合), 让测试聚焦于 stubbing 的行为, 也避免 NPE 中断; 需真实行为用 @SpyBean 或 when(...).thenReturn(...)。**Why mock 组合影响缓存？** 不同测试类的 @MockBean 集合不同 → 容器内容不同 → 不能共用, 缓存键必须区分(正确性)。[模式: 替身策略 + 缓存键]

数据流: @MockBean UserService → createMock → Mockito.mock(UserService) → 注入 OrderService.userService → 测试里 when(userService.find(1)).thenReturn(user) → 调用 find(1)→返回 user; 未 stub 的 method→null。@SpyBean UserService → Mockito.spy(真实) → 未 stub 走真实。两个测试 @MockBean 不同 → 各自容器。

→ 引出 8-B: @Sql — 替换 Bean 后要准备数据: @Sql 在测试方法前后执行 SQL 脚本(建表/插入/清理)。
