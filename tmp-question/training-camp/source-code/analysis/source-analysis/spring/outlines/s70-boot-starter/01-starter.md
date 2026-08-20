# S-6 Starter 机制 — 依赖聚合与自动装配入口

> 依赖 S-2 + S-4 | 🟡 Working | 6 KP | [模式: 聚合器 + 声明式装配 + BOM]

**读者处境**: 引入 spring-boot-starter-web 就自动有了 MVC 全家桶 — starter 到底是什么?为什么"引入依赖"就能自动装配?版本为什么不用写?

### 1. Starter 本质 — 空 jar + 依赖聚合

场景: starter 目录里只有 build.gradle 没有源码 — 它本质是"依赖打包器": 用 api 声明把一组依赖传递给你。

源码路径:
- `spring-boot-starter/build.gradle`(dependencies 段) — **基础 starter**: `api(spring-boot)` + `api(spring-boot-autoconfigure)` + `api(starter-logging)` + jakarta.annotation + spring-core + snakeyaml — 无代码, 纯聚合
- `spring-boot-starter-web/build.gradle`(dependencies 段) — **功能 starter**: `api(starter)` + `api(starter-json)` + `api(starter-tomcat)` + spring-web + spring-webmvc — 按"Web 功能"聚合
- api(而非 implementation): 依赖**传递**给使用者 — 引 starter-web 等于引上面全部

关键设计: **Why 空 jar + 聚合？** "依赖即配置": 引入 starter = 声明"我要这套功能", 依赖传递自动带入实现库; 无需自己组合依赖(Web 需要 webmvc+tomcat+json 三样, starter 一次给全)。starter 本身不写代码 — 自动装配在 autoconfigure 模块(S-2)。[模式: 聚合器]

数据流: 项目加 `spring-boot-starter-web` 依赖 → Maven/Gradle 解析: api 传递 → 获得 spring-boot/autoconfigure/starter-json/starter-tomcat/spring-web/webmvc → classpath 齐备 → 各 jar 的 imports 触发自动装配(S-2)。

### 2. 依赖链 → 自动装配映射

场景: starter 引入的依赖怎么变成"自动配好的 Bean"?关键: 每个依赖 jar 自带自动装配清单(imports), 引入即注册。

源码路径:
- `spring-boot-starter-json/build.gradle` — **JSON**: api(jackson-databind/jackson-datatype-*) → spring-boot-autoconfigure 的 imports 含 JacksonAutoConfiguration(有 jackson 时激活)
- `spring-boot-starter-tomcat/build.gradle` — **容器**: api(tomcat-embed-core/el/websocket) → 嵌入式 Tomcat 在 classpath → ServletWebServerFactoryAutoConfiguration 生效(工厂组装见 t7)
- `spring-boot-starter-web/build.gradle` — **MVC**: spring-webmvc → WebMvcAutoConfiguration(有 DispatcherServlet 时激活)
- 触发机制: S-2 的 AutoConfigurationImportSelector 读各 jar 的 META-INF/spring/*.imports — **starter 不写自动装配, 依赖 jar 提供**

关键设计: **Why 自动装配在依赖 jar 而非 starter？** 职责分离: starter 管"依赖集合", autoconfigure 管"自动装配逻辑"; 同一 autoconfigure jar(如 spring-boot-autoconfigure)被所有 starter 共享, 自动装配类按条件(S-3)只对存在依赖生效 — 引入什么依赖, 什么自动装配就激活。[模式: 依赖驱动装配]

数据流: 引 starter-web → classpath 有 spring-webmvc+tomcat-embed → S-2 读 imports: WebMvcAutoConfiguration(条件: 有 DispatcherServlet)→激活 → ServletWebServerFactoryAutoConfiguration(条件: 有 Tomcat)→激活 → 自动装配产生 DispatcherServlet/嵌入式 Tomcat。不引 starter-tomcat(换 undertow)→ Tomcat 条件不满足→用 Undertow 自动装配。

### 3. BOM 版本管理 + 自定义 starter

场景: 为什么依赖不用写版本号?团队怎么做一个内部 starter?

源码路径:
- `spring-boot-dependencies/build.gradle` — **BOM**: 所有依赖(spring 框架/jackson/tomcat 等)的版本集中声明 — 继承该 BOM 的工程免写版本, 版本由 Boot 发行版统一
- 自定义 starter 约定: 命名 `xxx-spring-boot-starter`(官方 `spring-boot-starter-xxx`); 内含: 自动装配类(autoconfigure 模块) + 自己的 `META-INF/spring/*.imports`(S-2 读取) + 依赖聚合

关键设计: **Why BOM？** 版本一致性: 各库版本必须互相兼容(Boot 测试过的组合)— BOM 把"经过验证的版本集"给出来, 开发者不纠结版本; 升级 Boot = 全家升版本。**Why 自定义 starter 要带 imports？** 你的自动装配要被人用, 必须注册进 S-2 的清单 — 约定文件位置是关键。[模式: BOM + 约定]

数据流: 项目继承 spring-boot-dependencies(BOM) → 依赖不写版本 → 构建解析 BOM 得版本。自定义 starter: 写自动装配类 + 建 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports(列自动装配类) → 使用者引入你的 starter → S-2 读到你的 imports → 自动装配生效。

→ 引出 S-7: MVC 自动装配 — starter-web 带来了 spring-webmvc, WebMvcAutoConfiguration 如何自动配好 DispatcherServlet/HandlerMapping/消息转换器(与 W-1/W-5 衔接)。
