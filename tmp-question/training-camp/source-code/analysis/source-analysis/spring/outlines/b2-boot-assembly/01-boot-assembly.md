# B-2 Spring Boot 装配 Spring Framework — `@SpringBootApplication` → `AutoConfigurationImportSelector` → `refresh()` 全链路

> 依赖 S2-1 refresh + B-1 DispatcherServlet-Tomcat | 🟡 Working | 3 KP | [模式: 模板方法 + 策略]

**读者处境**: `SpringApplication.run(App.class)` 到底做了什么？为什么加了 `@SpringBootApplication` 就能自动配置 DataSource、WebServer、Actuator？自动装配的底层机制到底是什么？

### 1. `SpringApplication.run()` — 创建 ApplicationContext + refresh

场景: `SpringApplication.run(App.class)` → 创建 `ApplicationContext` → `refresh()` → 容器启动。

源码路径:
- `SpringApplication.java` — `run()` 方法
- 创建 `ApplicationContext`（根据 classpath 判断是 `AnnotationConfigServletWebServerApplicationContext` 还是其他）
- `refreshContext(context)` → 触发 `AbstractApplicationContext.refresh()`

关键设计: **Why `SpringApplication` 不直接调 `refresh()`？** `SpringApplication` 在 `refresh()` 之前还要做：环境准备（`prepareEnvironment`）、ApplicationContext 创建、`ApplicationContextInitializer` 回调、Banner 打印等。`refresh()` 只是启动链的后半段，前半段由 `SpringApplication` 自己组织。

### 2. `@SpringBootApplication` — 三合一注解

场景: `@SpringBootApplication` = `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`。

关键拆解:
- `@SpringBootConfiguration`：本质是 `@Configuration`，标记配置类
- `@EnableAutoConfiguration`：触发自动装配
- `@ComponentScan`：组件扫描

### 3. `AutoConfigurationImportSelector` — 自动装配的核心

场景: `@EnableAutoConfiguration` → `@Import(AutoConfigurationImportSelector.class)` → 在 `ConfigurationClassParser` 处理 `@Import` 时调用 `selectImports()` → 读取 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` → 返回自动配置类列表。

源码路径:
- `AutoConfigurationImportSelector.java` — `selectImports()` → 读取自动配置类列表
- `AutoConfigurationLoader` — 加载 `AutoConfiguration.imports` 文件
- 条件过滤：`@ConditionalOnClass` / `@ConditionalOnBean` / `@ConditionalOnProperty` 等

关键设计: **Why `AutoConfigurationImportSelector` 是 `DeferredImportSelector`？** 它需要等所有用户配置类解析完之后，再决定哪些自动配置类应该生效。如果用普通 `ImportSelector`，用户配置还没解析完就开始自动装配，`@ConditionalOnMissingBean` 等条件判断会不准确。

数据流: `@SpringBootApplication` → `@EnableAutoConfiguration` → `@Import(AutoConfigurationImportSelector)` → `ConfigurationClassParser` 处理 `@Import` → `DeferredImportSelector` 延迟到所有用户配置解析完 → `selectImports()` 读取 `AutoConfiguration.imports` → 条件过滤 → 返回生效的自动配置类 → 注册为 BeanDefinition → `refresh()` 创建 Bean。

→ 引出 C-1: ApplicationContext 完整生命周期总图（`refresh()` 12 步的更深层理解）。
