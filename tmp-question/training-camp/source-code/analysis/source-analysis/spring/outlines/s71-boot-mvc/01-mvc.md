# S-7 MVC 自动装配 — WebMvcAutoConfiguration

> 依赖 W-1~W-5 (全部复用) | 🔴 Deep | 6 KP | [模式: 自动装配+条件 + 委托配置]

**读者处境**: 引了 starter-web, 没写任何配置就有 DispatcherServlet/HandlerMapping/消息转换器 — 谁装配的?条件是什么?用户自定义了 MVC 会怎样?

### 1. 装配条件与顺序 — 何时激活/跳过

场景: WebMvcAutoConfiguration 是 imports 里 156 个自动装配类之一 — 它什么时候生效?为什么 @EnableWebMvc 会"关闭"它?

源码路径:
- `WebMvcAutoConfiguration.java:144,147,148,151` — **声明**: @AutoConfiguration(after=DispatcherServletAutoConfiguration/TaskExecutionAutoConfiguration, L144 — 依赖先行) + @ConditionalOnClass(Servlet/DispatcherServlet/WebMvcConfigurer, L147 — MVC 类在 classpath) + @ConditionalOnMissingBean(WebMvcConfigurationSupport.class, L148 — **容器没有它才生效**) + L151 类声明
- 语义: 用户写 @EnableWebMvc(或自己定义 WebMvcConfigurationSupport) → @ConditionalOnMissingBean 不匹配 → 整个自动装配跳过 — **用户配置优先**

关键设计: **Why @ConditionalOnMissingBean(WebMvcConfigurationSupport)？** 自动装配的"默认"性质: 它提供默认 MVC 配置, 用户一旦显式接管(@EnableWebMvc 导入 WebMvcConfigurationSupport), 默认装配必须让位 — 否则两套 MVC Bean 冲突。这是 S-3 OnBeanCondition 的典型应用。[模式: 条件装配 + 用户优先]

数据流: S-2 读 imports → WebMvcAutoConfiguration 候选 → S-3 评估: @ConditionalOnClass(有 DispatcherServlet→true) + @ConditionalOnMissingBean(容器无 WebMvcConfigurationSupport→true, 用户没 @EnableWebMvc) → 激活。用户 @EnableWebMvc → WebMvcConfigurationSupport bean 存在 → 跳过。

### 2. EnableWebMvcConfiguration — W 系列 Bean 的装配核心

场景: W-1~W-4 讲的 DispatcherServlet 家族(HandlerMapping/HandlerAdapter/ArgumentResolver/消息转换器) — 这些 Bean 在 Boot 里由谁创建?答案: EnableWebMvcConfiguration。

源码路径:
- `WebMvcAutoConfiguration.java:182,185` — **适配器**: WebMvcAutoConfigurationAdapter L185: @Import(EnableWebMvcConfiguration.class)(L182) + @EnableConfigurationProperties(WebMvcProperties/WebProperties)(L183) — implements WebMvcConfigurer
- `WebMvcAutoConfiguration.java:386,412` — **EnableWebMvcConfiguration L386** extends DelegatingWebMvcConfiguration — createRequestMappingHandlerAdapter L412(覆写: 响应状态处理/同步异步)
- `DelegatingWebMvcConfiguration`(spring-webmvc) — **委托配置**: setConfigurers(L50) 收集容器全部 WebMvcConfigurer bean → 创建 RequestMappingHandlerMapping(W-2)/RequestMappingHandlerAdapter(W-3)/ArgumentResolvers(W-4)/MessageConverters(W-5)/HandlerExceptionResolver(C-13) 等核心 Bean

关键设计: **Why 委托 DelegatingWebMvcConfiguration？** 装配逻辑(建 Bean)在 spring-webmvc 的 DelegatingWebMvcConfiguration(纯 Spring 侧), Boot 只负责"激活它"(@Import) — 用户自定义 WebMvcConfigurer 被收集聚合, 与默认装配共存; 机制(W 系列)与装配(本域)职责分离 — 复用≠省略: 本域不讲机制细节, 只讲"Bean 怎么被建"。[模式: 委托配置 + 聚合]

数据流: WebMvcAutoConfigurationAdapter 激活 → @Import(EnableWebMvcConfiguration) → 继承 DelegatingWebMvcConfiguration → 收集所有 WebMvcConfigurer bean(含 Adapter 自身+用户实现) → 创建: requestMappingHandlerMapping(W-2 机制)/requestMappingHandlerAdapter(W-3, createRequestMappingHandlerAdapter L412)/argumentResolvers(W-4)/messageConverters(W-5, 见 §3)/exceptionResolver(C-13) → DispatcherServlet 注入使用。

### 3. Adapter 定制与 HttpMessageConverters

场景: spring.mvc.* 属性怎么生效?消息转换器怎么装配(用户定制+默认)?

源码路径:
- `WebMvcAutoConfiguration.java:195,225` — **转换器**: messageConvertersProvider L195(ObjectProvider<HttpMessageConverters>) → configureMessageConverters L225: 用户定制的 HttpMessageConverters(有则用) + 默认集合 — W-5 的转换器链装配
- `WebMvcProperties.java` — **属性**: spring.mvc.* (静态资源/路径匹配/格式化) — S-5 绑定后由 Adapter 应用
- 其他 Bean: L165/172/283/292/301/372(拦截器/静态资源/格式化等)

关键设计: **Why HttpMessageConverters 用 ObjectProvider？** 用户可能自定义转换器集合(Bean)— 有则优先生效, 无则用 Boot 默认; ObjectProvider 表达"可选依赖", 避免硬依赖。转换器机制是 W-5, 这里只做"装配+定制合并"。[模式: 可选注入 + 定制优先]

数据流: 用户 @Bean HttpMessageConverters(自定义集合) → configureMessageConverters: provider.getIfAvailable() → 有自定义→用自定义; 无→默认(ByteArray/String/Jackson 等, W-5) → 传给 requestMappingHandlerAdapter(W-3 使用)。spring.mvc.static-path-pattern → WebMvcProperties → 静态资源处理器配置。

→ 引出 S-8: 嵌入式容器 — WebMvcAutoConfiguration 在 DispatcherServletAutoConfiguration 之后, 而 DispatcherServlet 需要 Servlet 容器 — ServletWebServerFactory 自动装配与 refresh 衔接(容器组装复用 t7)。
