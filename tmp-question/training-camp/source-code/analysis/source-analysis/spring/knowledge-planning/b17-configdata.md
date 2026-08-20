# S-17 外部化配置深化 — ConfigData 加载与优先级 (application.yml 的来源)

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | ConfigDataEnvironmentPostProcessor(172行)+ConfigDataEnvironment(413行)+ConfigDataEnvironmentContributors(349行)+ConfigDataEnvironmentContributor(594行)+ConfigDataLocation(169行)+EnvironmentPostProcessorApplicationListener(255行)
> 基线: BOOT-PLAN-v2 S-17 ⭐ — 配置数据加载与优先级; 前置: **C-3 Environment(PropertySource/MutablePropertySources/查找链 — 复用模型) + S-4 run 流程** — 展开 ConfigData 加载编排与优先级

---

## §0.8

- 🔴 Deep，1篇 — 入口(ConfigDataEnvironmentPostProcessor: EnvironmentPostProcessor, ORDER=HIGHEST_PRECEDENCE+10, 由 ApplicationEnvironmentPreparedEvent 触发, L96 processAndApply) → 编排(ConfigDataEnvironment.processAndApply 4 阶段: processInitial→processWithoutProfiles→withProfiles(推断 profile)→processWithProfiles→applyToEnvironment) → 位置语法(ConfigDataLocation: optional: 前缀/分号多位置/DEFAULT_SEARCH_LOCATIONS 默认搜索 classpath+file+config 目录) → **优先级核心**(ContributorIterator 先遍历 AFTER_PROFILE_ACTIVATION(profile-specific) 再 BEFORE → applyContributor 仅对 BOUND_IMPORT 做 addLast → profile-specific > 非 profile, 全部位于命令行/系统属性/环境变量之下)
- 设计模式: [模式: SPI/监听器]—EnvironmentPostProcessor; [模式: 阶段化管线]—4 阶段编排; [模式: 树形贡献者+迭代器]—ContributorIterator 决定优先级

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ConfigDataEnvironmentPostProcessor.java:46,51,88,96 | 入口 | **implements EnvironmentPostProcessor,Ordered**: ORDER=HIGHEST_PRECEDENCE+10(L51); postProcessEnvironment(L88)→L96 `getConfigDataEnvironment(...).processAndApply()` | High |
| spring.factories:57 | 注册 | **ConfigDataEnvironmentPostProcessor** 在 EnvironmentPostProcessor 列表 | High |
| EnvironmentPostProcessorApplicationListener.java:116,127 | 触发 | **onApplicationEnvironmentPreparedEvent(L127)**: 遍历所有 EnvironmentPostProcessor → postProcessEnvironment(L134) — run 的 environmentPrepared 阶段 | High |
| ConfigDataEnvironment.java:234,242,244 | 编排 | **processAndApply(L234)**: processInitial→processWithoutProfiles(L241)→withProfiles(L242 推断 profile)→processWithProfiles(L243)→applyToEnvironment(L244) | High |
| ConfigDataEnvironment.java:89-94,203 | 搜索位置 | **DEFAULT_SEARCH_LOCATIONS(L89)**: optional:classpath:/;config/ + optional:file:./;config/;config/*/ — L203 作为 spring.config.location 默认值 | High |
| ConfigDataEnvironmentContributor.java:547,579 | 优先级核心 | **ContributorIterator**: phase 初始 AFTER_PROFILE_ACTIVATION(L547) → 先遍历 AFTER(profile-specific) 再 BEFORE(L579-587) — profile-specific 优先 | High |
| ConfigDataEnvironment.java:352,357,365 | 应用 | **applyContributor(L352)**: 仅 Kind.BOUND_IMPORT(L357)→propertySources.addLast(L365) — 排在已存在源之后 | High |
| ConfigDataLocation.java:41,163 | 位置语法 | **optional: 前缀(L41)**: of(L163) 解析 optional/位置 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: ConfigData 是"配置加载"一个主题, 核心 3 块(入口/编排/优先级)耦合紧密, 且 🔴 需完整叙事 — 1篇 (~50行) 按"入口 → 编排 → 优先级"展开; C-3 的 PropertySource/MutablePropertySources/查找链机制全部复用(C-3 已讲), 本域只讲"谁加载、怎么编排、优先级怎么定"。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 入口触发 (EnvironmentPostProcessor + ORDER + environmentPrepared 事件) | 🔴 | **为什么🔴**: 配置加载的启动时机与 SPI |
| P1-2 | 编排 (processAndApply 4 阶段 + profile 分阶段推断) | 🔴 | **为什么🔴**: 为什么 profile 要分阶段加载 |
| P1-3 | **优先级模型** (ContributorIterator AFTER-first + applyContributor addLast) | 🔴 | **为什么🔴**: profile-specific>非profile、文件>classpath、全部低于命令行 — Boot 配置优先级本质 |
| P2-1 | 位置语法 (optional:/分号/spring.config.*) | 🟡 | **为什么🟡**: 位置怎么写/可选性 |
| P2-2 | DEFAULT_SEARCH_LOCATIONS 默认加载 application.yml | 🟡 | **为什么🟡**: 默认从哪些路径找 |
| P3-1 | 与 C-3 边界 (复用 property source 模型) | 🟢 | **为什么🟢**: 模型在 C-3, 本域讲加载 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **入口** (ConfigDataEnvironmentPostProcessor + 事件触发) | 🔴 | 配置何时/怎样被加载 |
| B | **编排** (processAndApply 4 阶段) | 🔴 | 为什么分阶段 |
| C | **优先级** (ContributorIterator + addLast + 位置语法) | 🔴 | 配置谁覆盖谁 |

> **Cluster A (§1)**: ConfigDataEnvironmentPostProcessor(EnvironmentPostProcessor, ORDER HIGHEST+10) + ApplicationEnvironmentPreparedEvent 触发
> **Cluster B (§2)**: ConfigDataEnvironment.processAndApply 4 阶段(initial→withoutProfiles→withProfiles→withProfiles→applyToEnvironment)
> **Cluster C (§3)**: ConfigDataLocation 语法 + DEFAULT_SEARCH_LOCATIONS + ContributorIterator AFTER-first + applyContributor addLast

→ 引出 S-18: 日志 — 外部化配置之后: LoggingSystem/LogbackLoggingSystem(进入启动运行时层)
