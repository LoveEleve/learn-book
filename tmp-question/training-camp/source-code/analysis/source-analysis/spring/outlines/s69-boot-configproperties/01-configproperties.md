# S-5 @ConfigurationProperties — 属性绑定与松弛绑定

> 依赖 C-2/C-3 | 🔴 Deep | 6 KP | [模式: BPP 绑定 + 绑定器 + 名称规范化]

**读者处境**: `server.port` 怎么变成 `ServerProperties.port`?为什么 `server.port`/`SERVER_PORT`/`server-port` 都能匹配?属性什么时候注入的?

### 1. @ConfigurationProperties 声明与注册

场景: `@ConfigurationProperties(prefix="server") class ServerProperties { int port; }` — 声明"这个类接收 server.* 属性"。

源码路径:
- `ConfigurationProperties.java:52,70,85` — **注解**: prefix(L70, 属性前缀)/ignoreInvalidFields(L78)/ignoreUnknownFields(L85, 默认 true — 未知属性不报错)
- 注册方式: `@EnableConfigurationProperties(ServerProperties.class)`(显式注册) / `@ConfigurationPropertiesScan`(扫描包) / @Component 直接注册 — Boot 自动装配类用 @EnableConfigurationProperties 注册自己的配置属性类
- 绑定产物: ConfigurationPropertiesBean — 包装配置类+前缀+绑定方式

关键设计: **Why 注解声明 + 注册分离？** 声明是"这个类是什么"(@ConfigurationProperties), 注册是"这个类在哪被启用"(@EnableConfigurationProperties) — 自动装配类里 @EnableConfigurationProperties(XxxProperties.class) 让配置属性类作为 Bean 绑定, 供自动装配类注入使用。[模式: 声明 + 注册]

数据流: `@ConfigurationProperties(prefix="server") ServerProperties` + `@EnableConfigurationProperties(ServerProperties.class)` → 容器注册 ServerProperties 为配置属性 Bean → 绑定时机(§2)注入 server.* 属性。

### 2. ConfigurationPropertiesBindingPostProcessor — 初始化前绑定

场景: 属性什么时候注入 Bean?关键: **Bean 初始化之前** — 这样 @PostConstruct/初始化方法里属性已可用。

源码路径:
- `ConfigurationPropertiesBindingPostProcessor.java:44,77` — **BPP**: postProcessBeforeInitialization L77 → bind(L88) → ConfigurationPropertiesBinder.bind — 在 Bean 属性填充后、@PostConstruct/InitializingBean 之前
- `ConfigurationPropertiesBinder` — **桥**: 持有 Binder + Validator(校验器) — bind 后校验(§3)
- `Binder.java:531` — **Binder.get(Environment)**: 从环境构造绑定器(取 C-3 的属性源 + C-2 转换服务)

关键设计: **Why postProcessBeforeInitialization？** 绑定是"把配置填进对象" — 必须在对象初始化逻辑(@PostConstruct/afterPropertiesSet)之前完成, 否则初始化里读到的属性是空的; 且 BPP 时机(C-6 BPP 体系)天然在 Bean 生命周期合适位置。[模式: 初始化前绑定]

数据流: 容器创建 ServerProperties → 属性填充(空) → ConfigurationPropertiesBindingPostProcessor.postProcessBeforeInitialization(L77) → binder.bind: Binder.get(environment)(L531) → 属性源查找 server.* → 绑定到 port 字段(类型转换 C-2) → @PostConstruct(此时属性已就绪) → 注入依赖方。

### 3. Binder 绑定 + 松弛绑定

场景: bind 怎么把 `server.port=8080` 变成 `ServerProperties.port`?为什么 `SERVER_PORT` 环境变量也能匹配 `server.port`?

源码路径:
- `Binder.java:248,274` — **bind(name, Bindable, handler) L274**: 属性源查找 → internalBind(按目标类型: bean 绑定/属性绑定/集合) → BindResult; bindOrCreate(L336: 无则先创建目标再绑定)
- `Binder.java:531` — **Binder.get(Environment)**: 包装 C-3 的属性源迭代器 + C-2 转换服务
- `ConfigurationPropertyName.java:55,64` — **松弛绑定核心**: 属性名"元素化"(Elements L64)— 任何写法(server.port/server-port/SERVER_PORT/ServerPort)先规范化成统一形式(小写元素, 连字符统一)再匹配 — **宽松命名规则**
- 校验: 绑定过程经 ValidationBindHandler(ConfigurationPropertiesBinder L97-101 绑定链)— Validator 校验 @Valid/@NotNull 等(C-22 衔接), 失败抛绑定异常

关键设计: **Why 松弛绑定？** 配置来源多样(application.yml 用 kebab-case, 环境变量用下划线大写, Java 属性用驼峰)— 若严格要求格式, 用户要记每种来源的写法; ConfigurationPropertyName 把所有写法归一化匹配, "一种属性, 任意来源统一书写"。**Why 元素化？** 属性名拆成点分隔的元素, 每元素独立规范化(连字符/大小写), 匹配时逐元素比对。[模式: 名称规范化]

数据流: `SERVER_PORT=8080`(环境变量) → Binder 查 "server.port" → 属性源转换: 环境变量名 SERVER_PORT → ConfigurationPropertyName 规范化 → 与查询的 server.port 逐元素匹配 → 命中 → C-2 转换 "8080"→int → ServerProperties.port=8080。`server.port: 8080`(yml)/`ServerPort`(驼峰)同样命中。

→ 引出 S-6: Starter 机制 — 配置属性类被自动装配类使用, 而自动装配类由 starter 打包提供 — starter 的依赖传递与自动装配入口。
