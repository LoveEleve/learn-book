# S-5 @ConfigurationProperties — 属性绑定与松弛绑定 (Binder 核心)

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | ConfigurationProperties(约120行)+ConfigurationPropertiesBindingPostProcessor(约130行)+ConfigurationPropertiesBinder+Binder(约600行)+ConfigurationPropertyName(约300行)
> 基线: BOOT-PLAN-v2 S-5 — 配置属性的绑定核心; 前置: **C-2 类型转换(复用)/C-3 Environment(属性源复用)** — 展开 Binder/松弛绑定/绑定时机 (下层无, 全展开)

---

## §0.8

- 🔴 Deep，1篇 — 声明(@ConfigurationProperties: prefix/ignore 选项 + @EnableConfigurationProperties/@ConfigurationPropertiesScan 注册) → 时机(ConfigurationPropertiesBindingPostProcessor: postProcessBeforeInitialization 在 Bean 初始化前绑定) → 绑定(Binder: 属性源→Bindable 目标, bind/bindOrCreate, 类型转换走 C-2) → 松弛绑定(ConfigurationPropertyName: 规范化 kebab-case, 驼峰/下划线/连字符统一匹配)
- 设计模式: [模式: BPP 绑定]—初始化前注入; [模式: 绑定器]—Binder 属性源到对象; [模式: 名称规范化]—松弛绑定

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ConfigurationProperties.java:52,70,85 | 注解 | **@ConfigurationProperties**: prefix(L70)/ignoreInvalidFields(L78)/ignoreUnknownFields(L85, 默认 true) | High |
| ConfigurationPropertiesBindingPostProcessor.java:44,77 | 绑定时机 | **postProcessBeforeInitialization L77**: Bean 初始化前 → bind(L88)→ConfigurationPropertiesBinder.bind — 属性先于 @PostConstruct 注入 | High |
| Binder.java:248,274 | 绑定 | **bind(name, Bindable, handler) L274**: 属性源查找→internalBind(bean/property/集合)→BindResult; bindOrCreate L336(无则创建) | High |
| Binder.java:531 | 获取 | **Binder.get(Environment) L531**: 从环境构造绑定器(属性源+转换服务) | High |
| ConfigurationPropertyName.java:55,64 | 规范化 | **ConfigurationPropertyName**: 属性名元素化(Elements L64) — 统一为规范形式(小写 kebab-case) | High |
| ConfigurationPropertiesBinder | 桥 | **ConfigurationPropertiesBinder**: 持 Binder+校验器(Validator) — 绑定后校验 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 注解+后处理器+绑定器+名称规范化约 1200 行 — 知识主线: "@ConfigurationProperties 声明 → BPP 时机 → Binder 绑定 → 松弛匹配". 1篇 (🔴 ~50行) 按"声明→时机→绑定→松弛"展开; C-2 转换/C-3 环境复用(机制), Binder 全展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | ConfigurationPropertiesBindingPostProcessor (初始化前绑定时机) | 🔴 | **为什么🔴**: 属性什么时候注入 — 早于 @PostConstruct/初始化回调 |
| P1-2 | Binder 绑定 (属性源→Bindable→对象, bind/bindOrCreate) | 🔴 | **为什么🔴**: 绑定的执行引擎 — 类型转换(C-2)/嵌套/集合 |
| P1-3 | 松弛绑定 (ConfigurationPropertyName 规范化) | 🔴 | **为什么🔴**: Boot 属性匹配的核心 — server.port 与 SERVER_PORT/ServerPort 统一 |
| P2-1 | @ConfigurationProperties 声明与注册 (prefix/ignore + @EnableConfigurationProperties) | 🟡 | **为什么🟡**: 使用侧声明与注册方式 |
| P2-2 | 绑定后校验 (Validator/validation) | 🟡 | **为什么🟡**: 配置合法性检查 — 与 C-22 衔接 |
| P3-1 | bindOrCreate vs bind (无则创建语义) | 🟢 | **为什么🟢**: 两种绑定结果语义 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **声明与时机** (@ConfigurationProperties + BPP) | 🔴 | 怎么声明、何时绑定 |
| B | **绑定引擎** (Binder: 属性源→对象) | 🔴 | 绑定的执行 |
| C | **松弛与校验** (ConfigurationPropertyName + 校验) | 🔴 | 匹配规则与合法性 |

> **Cluster A (§1)**: @ConfigurationProperties(prefix/ignore) + 注册方式(@EnableConfigurationProperties/@ConfigurationPropertiesScan)
> **Cluster B (§2)**: ConfigurationPropertiesBindingPostProcessor(初始化前) + ConfigurationPropertiesBinder + Binder(bind/bindOrCreate)
> **Cluster C (§3)**: 松弛绑定(ConfigurationPropertyName 元素化/大小写/连字符) + 绑定后校验 + C-2 转换衔接

→ 引出 S-6: Starter 机制 — 配置类用 @ConfigurationProperties 接收属性, starter 把"依赖+自动装配+配置"打包 — 依赖传递与自动装配入口

(End of file - total 61 lines)
