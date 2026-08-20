# S-22 测试自动配置 — @WebMvcTest 切片 (关全量 + 类型过滤 + 选择性装配)

> 依赖 C-16~C-19 (test 层, 复用) + S-2 | 🟡 Working | 6 KP | [模式: 复合注解 + ContextCustomizer + 类型过滤]

**读者处境**: `@WebMvcTest(MyController.class)` 只加载 web 层, 启动飞快 — 为什么没加载数据库/全量配置?一个注解怎么做到"只测控制器"?切片和 @SpringBootTest 差在哪?

### 1. 切片注解组合 — @WebMvcTest 复合注解

场景: 一个 @WebMvcTest 注解, 背后叠了哪些元注解决定"这是个切片"?

源码路径:
- `web/servlet/WebMvcTest.java:101,103,104,107,108` — **复合注解**: `@BootstrapWith(WebMvcTestContextBootstrapper.class)`(L101, 自定义 context 启动器) + `@OverrideAutoConfiguration(enabled = false)`(L103, **关全量自动装配**) + `@TypeExcludeFilters(WebMvcTypeExcludeFilter.class)`(L104, **类型过滤**) + `@AutoConfigureMockMvc`(L107, 配 MockMvc) + `@ImportAutoConfiguration`(L108, 导入切片专属自动装配)
- 对照: `@SpringBootTest` 不关自动装配 — 全量加载; 切片(如 @WebMvcTest/@DataJpaTest)关闭全量, 只搭子集上下文

关键设计: **Why 复合注解？** 一个切片 = 多个关注点(关全量/过滤/导入/配 MockMvc)— 用元注解把这些打包进一个 @WebMvcTest, 用户只需一个注解即可声明"web 切片测试", 语义自解释。[模式: 复合注解]

数据流: 测试类标 @WebMvcTest → 元注解展开: @BootstrapWith 选自定义 bootstrapper → @OverrideAutoConfiguration(enabled=false) 交给 §2 → @TypeExcludeFilters 交给 §2 → @ImportAutoConfiguration/@AutoConfigureMockMvc 交给 §3。

### 2. 关闭全量 + 类型过滤 — 只留切片 Bean

场景: 切片如何确保"不加载全量自动装配 + 只留 Controller 相关 Bean"?

源码路径:
- `OverrideAutoConfigurationContextCustomizerFactory.java:40,47,54,58` — **关全量**: `createContextCustomizer`(L40): 读 @OverrideAutoConfiguration.enabled(L47) → 若 false 则 `DisableAutoConfigurationContextCustomizer`(L54) → `customizeContext`(L58): `TestPropertyValues.of("spring.boot.enableautoconfiguration=false").applyTo(context)` — 关闭 @EnableAutoConfiguration
- `filter/TypeExcludeFiltersContextCustomizer.java:84,87` — **注册过滤**: customizeContext(L84) → `context.getBeanFactory().registerSingleton(...TypeExcludeFilter)`(L87)
- `web/servlet/WebMvcTypeExcludeFilter.java:50,63,67,91` — **过滤内容**: extends Standard...(L50); includes `ControllerAdvice`(L63)/`Filter`(L67)/`Controller`(L91) — 只保留 web 切片需要的类型

关键设计: **Why 关全量？** 全量自动装配会拉入数据源/Redis/安全等 — 切片只要 web 层, 关掉 enableautoconfiguration 可避免无关 Bean/加速启动/隔离依赖; 再由 @ImportAutoConfiguration(§3)按需补回切片需要的。**Why TypeExcludeFilter？** 关全量后仍可能有用户 @Component 混入 — 用 TypeExcludeFilter 按注解类型过滤, 只留 @Controller/@ControllerAdvice/@Filter 等, 保证切片纯净。[模式: ContextCustomizer + 类型过滤]

数据流: @OverrideAutoConfiguration(enabled=false) → createContextCustomizer(L40) → DisableAutoConfigurationContextCustomizer(L54) → customizeContext(L58) 设 enableautoconfiguration=false → 全量自动装配被禁用。同时 TypeExcludeFiltersContextCustomizer(L87) 注册 WebMvcTypeExcludeFilter → 组件扫描时只留 Controller/ControllerAdvice/Filter 等(L63/67/91)。

### 3. 选择性自动装配导入 — @ImportAutoConfiguration

场景: 关掉全量后, MockMvc/Security 等切片需要的配置怎么补回来?

源码路径:
- `ImportAutoConfiguration.java:50,51` — **导入注解**: `@Import(ImportAutoConfigurationImportSelector.class)`(L50) + `@interface ImportAutoConfiguration`(L51) — 声明要导入的自动装配类(classes()(L70)); `web/servlet/AutoConfigureMockMvc.java:50` — @AutoConfigureMockMvc 也带 `@ImportAutoConfiguration`(L50), 借此导入 MockMvcAutoConfiguration(@see L43)
- 机制: ImportAutoConfigurationImportSelector 按注解上的 classes 精确导入切片专属自动装配(如 WebMvcTest 相关的 MockMvc/Jackson/Security 配置), 不触发全量
- 复用: 生成的 MockMvc 由 C-16 的 MockMvcBuilders/perform/andExpect 执行(机制在 C-16)

关键设计: **Why 选择性导入而非全量？** 切片"关了全量"但"要它自己的配置" — @ImportAutoConfiguration 精确列出需要的自动装配类, 既不全量加载又保证切片功能完整(如 MockMvc); 这是"关全量"与"按需补装"的平衡。**Why 与 C-16 边界？** MockMvc 的构建/执行/断言机制在 C-16 已深度覆盖, 本域只讲"切片怎么把 MockMvc 自动装配进来" — 复用内核, 展开装配。[模式: 选择性导入 + 复用边界]

数据流: @WebMvcTest → @ImportAutoConfiguration + @AutoConfigureMockMvc → ImportAutoConfigurationImportSelector 导入 MockMvc/Jackson 等切片自动装配 → 容器里注册 MockMvc + WebMvcTestContextBootstrapper 启动切片上下文 → 测试注入 MockMvc → 复用 C-16 perform/andExpect 断言。

→ 引出 S-23: Validation — 测试之后: ValidationAutoConfiguration 自动注册校验器 + MethodValidationPostProcessor(前置 C-22)。
