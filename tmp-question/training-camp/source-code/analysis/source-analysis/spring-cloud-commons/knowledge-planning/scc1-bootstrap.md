# SCC-1 Bootstrap 上下文 — 知识规划 (KP)

> 域: SCC-1 | 级别: 🔴 | 方案: A | 大纲: outlines/scc1-bootstrap/outline.md (6 节)

## §01 域定位

Bootstrap 上下文 = 主应用 Environment 定型前的"引路人"。bootstrap.yml 加载 + PropertySourceLocator SPI (配置中心插槽) + insertPropertySources 排序仲裁 (高优先级实现)。双轨制 (bootstrap vs spring.config.import) 兼容面。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| bootstrap/BootstrapApplicationListener.java | 512 | 事件入口/三守卫/子上下文构建 | 1,2 |
| bootstrap/config/PropertySourceLocator.java | 69 | locate SPI + locateCollection | 3 |
| bootstrap/config/PropertySourceBootstrapConfiguration.java | — | initialize + insertPropertySources 仲裁 | 4 |
| bootstrap/config/PropertySourceBootstrapProperties.java | — | allowOverride 三开关绑定 | 4 |
| util/PropertyUtils.java | — | bootstrapEnabled/useLegacyProcessing/marker | 1 |
| bootstrap/TextEncryptorBindHandler.java | — | {cipher} 解密 (SCC-9 前置) | 6 |
| bootstrap/encrypt/ (6 文件) | — | 解密后处理链 | 6 |

## §05 闭环要点 (Pass 2 内化)

### q1 双轨制
bootstrapEnabled = enabled 属性 || MARKER_CLASS_EXISTS (PropertyUtils:48-50); useLegacyProcessing 兼容; 三守卫 (L97-104)。

### q2 子上下文构建
bootstrapServiceContext: 空环境 + config.name 注入 (L151) + setId("bootstrap") (L203) + addAncestorInitializer 设 parent (L205)。

### q3 排序仲裁
insertPropertySources: 反转 addFirst 保序 (L188-194) + 三开关 (allowOverride/overrideNone/overrideSystemProperties) 三策略 (L197-227)。

### q4 Locator SPI
locate 单方法契约 + locateCollection 展开 Composite (L44-47); 配置中心生态插槽。

## §06 负面空间 (6 条)

不做应用 Bean 加载 / 不内建配置中心客户端 / 不缓存远程配置 / 不做运行时刷新 / 不保证 locator 顺序契约 / 不做 bootstrap 热更新

## §07 交叉引用

- → SCC-2 @RefreshScope (刷新链路) + SCC-9 配置加密 (encrypt 同源)
- ← Spring Boot ConfigData (spring.config.import 对照)
- 另见: Nacos NacosPropertySourceLocator (5.8) / Apollo
