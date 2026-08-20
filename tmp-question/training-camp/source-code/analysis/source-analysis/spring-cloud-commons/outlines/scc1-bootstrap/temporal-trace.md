# SCC-1 Bootstrap 上下文 — 时空溯源 (代码注释 + 文档锚, git shallow)

> git shallow (单提交) 无法考古; 溯源以代码内 legacy/deprecated 痕迹 + docs/ 官方文档为锚。

## 演进链 (代码痕迹实证)

| 阶段 | 事件 | 证据 |
|:--:|:--|:--|
| 旧版 (1.x-2.3) | **bootstrap 是唯一配置加载方式**; PropertySourceLocator 是所有配置中心的入口; MARKER 类自动启用 | PropertyUtils.java:37-38 (MARKER_CLASS = org.springframework.cloud.bootstrap.marker.Marker, MARKER_CLASS_EXISTS 自动检测) |
| 2.4+ (Boot) | **spring.config.import 新机制取代 bootstrap**; use-legacy-processing 开关兼容旧应用 | PropertyUtils.java:52-54 (USE_LEGACY_PROCESSING_PROPERTY = spring.config.use-legacy-processing) |
| 4.3.2 (当前) | **双轨并存**: bootstrapEnabled (显式/marker) + useLegacyProcessing 两条件任一满足即启用 | BootstrapApplicationListener.java:99 (`!bootstrapEnabled && !useLegacyProcessing → return`) |

## 官方文档锚 (docs/)

- application-context-services.adoc:9 "The Bootstrap Application Context" — 官方章节
- application-context-services.adoc:14 "bootstrap properties are added with **high precedence**, so they cannot be overridden by local configuration" — 与 insertPropertySources addFirst 排序互证
- application-context-services.adoc:16-17 "The bootstrap context uses a **different convention** for locating external configuration" — 双轨制的文档面

## 版本相关性结论

- **bootstrap 是"被新机制取代但仍完整保留"的遗产** — 4.3.2 中三守卫 + 双条件启用 = 旧应用 (marker) + 新应用 (显式 enabled) + 迁移中 (legacy) 三态全兼容
- **排序仲裁 (insertPropertySources) 是长期稳定的核心价值** — allowOverride/overrideNone/overrideSystemProperties 三开关从 config server 时代沿用至今
- **PropertySourceLocator SPI 是所有配置中心的插槽** — Nacos/Apollo/Consul 都实现它; 新 spring.config.import 机制下 ConfigDataLocationResolver 是新一代对应物 (4.3.2 中并存)
