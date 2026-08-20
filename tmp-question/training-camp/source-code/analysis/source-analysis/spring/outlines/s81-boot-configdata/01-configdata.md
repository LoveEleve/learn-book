# S-17 外部化配置深化 — ConfigData 加载与优先级 (application.yml 从哪来)

> 依赖 C-3 Environment (复用模型) + S-4 | 🔴 Deep | 6 KP | [模式: SPI/监听器 + 阶段化管线 + 树形贡献者/迭代器]

**读者处境**: `application.yml` 里的值怎么进到 Environment?为什么 `application-dev.yml` 能覆盖 `application.yml`?为什么 `./config/` 下的文件比 classpath 的优先?`spring.config.location` 怎么改搜索路径?— 这些是 Boot 配置加载的核心 (ConfigData)。

### 1. 入口与环境准备 — EnvironmentPostProcessor 触发

场景: run 走到 environmentPrepared 事件 → ConfigDataEnvironmentPostProcessor 被调用 → 开始加载配置文件。它是怎么被挂进启动流程的?

源码路径:
- `ConfigDataEnvironmentPostProcessor.java:46,51` — **角色**: implements EnvironmentPostProcessor, Ordered; `ORDER = HIGHEST_PRECEDENCE + 10`(L51) — 启动最早期的高优先级区段, 但**晚于** RandomValue(+1)/SystemEnvironment(+4)/SpringApplicationJson(+5) 等 postProcessor(Ordered 值小者先执行)
- `spring.factories:57` — **注册**: `EnvironmentPostProcessor=...ConfigDataEnvironmentPostProcessor`(SPI 列表)
- `EnvironmentPostProcessorApplicationListener.java:116,127` — **触发**: 监听 `ApplicationEnvironmentPreparedEvent`(L116) → `onApplicationEnvironmentPreparedEvent`(L127) → L134 遍历所有 EnvironmentPostProcessor 调 `postProcessEnvironment(environment, application)`
- `ConfigDataEnvironmentPostProcessor.java:88,96` — **执行**: postProcessEnvironment(L88) → L96 `getConfigDataEnvironment(environment, resourceLoader, additionalProfiles).processAndApply()` — 构造 ConfigDataEnvironment 并开始加载

关键设计: **Why EnvironmentPostProcessor + 事件？** 要在 Environment 可用但容器(Bean)未创建时修改它 — 事件时机保证"只改配置、不碰 Bean"; 通过 SPI 开放扩展(用户可加自定义 postProcessor)。**Why HIGHEST+10 而非更小？** ConfigData 需在启动最早期执行, 但刻意排在 Random/System/JSON 等 postProcessor(HIGHEST+1/4/5)之后 — 它们先注入 random.*/系统环境/SPRING_APPLICATION_JSON 等来源; ConfigData 读取 spring.config.* 的 Binder 依赖这些已存在的属性源, 若比它们更早, 这些来源(可能含 spring.config.import/location)尚不可见。[模式: SPI/监听器]

数据流: run → environmentPrepared 事件 → EnvironmentPostProcessorApplicationListener(L127) → 加载 spring.factories 的 EnvironmentPostProcessor 列表(含 ConfigData) → 按 ORDER 调用 → ConfigDataEnvironmentPostProcessor.postProcessEnvironment(L88) → new ConfigDataEnvironment → processAndApply(L96) → 进入 §2。

### 2. 编排 — processAndApply 分阶段加载

场景: 加载不是一次读完 — 为什么分阶段?profile 还没确定时怎么知道加载哪些文件?

源码路径:
- `ConfigDataEnvironment.java:234` — **processAndApply**: 顺序: `processInitial`(L238, 无激活上下文) → `processWithoutProfiles`(L241) → `withProfiles`(L242, **推断 profile**) → `processWithProfiles`(L243, 带 profile 再处理) → `applyToEnvironment`(L244)
- `ConfigDataEnvironmentContributors.java:104` — **withProcessedImports**: 每阶段递归解析导入(UNBOUND_IMPORT L117 → 绑定/加载子贡献者) — 贡献者是一棵树
- `ConfigDataEnvironment.java:277,321` — **withProfiles(L277)**: 从已加载源用 Binder 收集 `spring.profiles.include/active` 推断 profile → 返回带 profile 的 activationContext → processWithProfiles(L321) 据此加载 profile-specific 文件

关键设计: **Why 分阶段？** profile 本身可能定义在配置文件里(application.yml 里写 active) — 所以先加载非 profile 文件(阶段1-2)推断出 profile, 再用它加载 profile-specific 文件(阶段3); 若 profile 依赖 config 里的 spring.profiles 则必须两轮。**Why 树形贡献者？** 一个位置可 import 更多位置(spring.config.import), 天然递归成树, 每阶段只处理"该激活且未处理"的节点。[模式: 阶段化管线 + 树形递归]

数据流: processInitial(加载初始 import, 无 profile) → processWithoutProfiles(withProcessedImports 展开非 profile 文件, 如 application.yml) → withProfiles(用 Binder 读 spring.profiles.active/include → 得到 profiles) → processWithProfiles(加载 application-dev.yml 等 profile-specific) → applyToEnvironment(把贡献者属性源写入 Environment)。

### 3. 优先级 — profile-specific 优先 + 位置语法 + addLast

场景: 为什么 application-dev.yml 覆盖 application.yml?为什么 ./config/ 比 classpath 优先?为什么命令行 -D 又能盖过所有 yml?— 优先级怎么定的?

源码路径:
- `ConfigDataEnvironmentContributor.java:547,579` — **优先级核心**: `ContributorIterator` 初始 phase=AFTER_PROFILE_ACTIVATION(L547) → 先遍历 AFTER 阶段(profile-specific)再 BEFORE 阶段(L579-587 切换) — 迭代顺序即优先级顺序
- `ConfigDataEnvironment.java:352,357,365` — **应用**: `applyContributor`(L352) 仅处理 `Kind.BOUND_IMPORT`(L357) → `propertySources.addLast`(L365) — 配置源追加到已存在源(命令行/系统属性/环境变量, 由 SpringApplication 先 addFirst)之后
- `ConfigDataEnvironmentContributor.java:276` — **moveProfileSpecific**: 把 profile-specific 子贡献者移到 AFTER 阶段(L287-288) — 实现"profile 覆盖非 profile"
- `ConfigDataLocation.java:41,163` — **位置语法**: `optional:` 前缀(L41, 找不到不报错)+ 分号分隔多位置; `ConfigDataEnvironment.java:89-94` — **DEFAULT_SEARCH_LOCATIONS**: `optional:classpath:/;classpath:/config/` + `optional:file:./;file:./config/;file:./config/*/` — 默认搜索路径; L203 作为 spring.config.location 默认值

关键设计: **Why AFTER 阶段优先？** 树迭代器先吐 AFTER(profile-specific) 节点 → applyContributor 先 addLast 它们 → 位于列表更前(索引更小)= 更高优先级; 非 profile 文件后 addLast → 被覆盖。**Why addLast 而非 addFirst？** 配置文件优先级天然低于命令行/系统属性/环境变量 — 用 addLast 把它们垫在已存在源之下(C-3: 索引 0 优先级最高); 文件位置(file:./)比 classpath 先 addLast, 故 file > classpath。[模式: 树形迭代器 + 有序插入]

数据流: applyContributor 迭代贡献者树 → **先 AFTER 阶段**(全部 profile-specific: application-dev.yml 等, file 先于 classpath)→ addLast → **再 BEFORE 阶段**(非 profile: application.yml, file 先于 classpath)→ addLast — 最终 MutablePropertySources 顺序: [命令行, 系统属性, 环境变量, ..., application-dev.yml, application.yml, ..., default] — 查找时(§2 C-3 责任链)从左到右, 前面的赢。

→ 引出 S-18: 日志 — 外部化配置之后: LoggingSystem/LogbackLoggingSystem 的日志系统抽象(进入启动运行时层)。
