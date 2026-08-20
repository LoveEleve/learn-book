# S2-4 MessageSource 国际化 — 多语言消息解析

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 5文件/1242行
> 基线: S2-3 事件 — refresh() Step 7 initMessageSource() 初始化消息源

---

## §0.8

- 🟡 Working，1篇 — MessageSource 接口 → Template Method → ResourceBundle → 缓存 → Parent 委托
- 设计模式: [模式: Template Method] AbstractMessageSource 骨架固定 → 子类实现 resolveCode; [模式: 责任链] Parent 委托链路: self→parent→...→DelegatingMessageSource(空)
- refresh() Step 7 `initMessageSource()` — 用户未设 → `DelegatingMessageSource`(空实现) → 用户设了 → 用户 bean

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MessageSource.java:getMessage(code,args,locale) | 接口 | 标准三参数: 无默认消息 → 无对应code → 抛NoSuchMessageException | High |
| MessageSource.java:getMessage(code,args,default,locale) | 接口 | 带默认消息: 无对应code → 返回 default 而非抛异常(渲染后) | High |
| MessageSource.java:getMessage(MessageSourceResolvable,locale) | 接口 | Resolvable对象: 含 codes[] 数组+defaultMessage+arguments → 按 codes[] 顺序尝试 | High |
| AbstractMessageSource.java:141-149 | getMessage(code,args,default,locale) | **核心入口(带默认)**: getMessageInternal→null→renderDefaultMessage(default,args,locale)→MessageFormat | High |
| AbstractMessageSource.java:153-157 | getMessage(code,args,locale) | **核心入口(无默认)**: getMessageInternal→null→throw NoSuchMessageException | High |
| AbstractMessageSource.java:210-258 | getMessageInternal() | **Template Method 核心**: ①locale=null→Locale.getDefault()兜底 ②code非null→messageCache检查(code,locale) →未缓存→resolveCodeWithoutArguments(无参)→MessageFormat / 带参→resolveCode→MessageFormat→缓存 ③formatMessage(commonMessage,args,locale)→返回 ④null→getMessageFromParent父级委托 | High |
| AbstractMessageSource.java:267-278 | getMessageFromParent() | **责任链**: parent instanceof AbstractMessageSource→getMessageInternal(内部直调,性能) / parent非null→parent.getMessage(标准重走) / parent null→return null(责任链终点) | High |
| AbstractMessageSource.java:resolveCode/resolveCodeWithoutArguments | 抽象方法 | **子类实现**: resolveCodeWithoutArguments→纯文本(无参数占位符); resolveCode→MessageFormat(带{0}{1}) | High |
| ResourceBundleMessageSource.java:setBasenames() | 配置 | basenames("messages","errors") → 对应 classpath:messages.properties, errors.properties → 按顺序合并 | High |
| ResourceBundleMessageSource.java:resolveCodeWithoutArguments() | 实现 | ResourceBundle.getBundle(basename, locale)→getString(code)→多个basename按序尝试→null→下一个basename | High |
| ResourceBundleMessageSource.java:resolveCode() | 实现 | resolveCodeWithoutArguments 获取原始文本 → new MessageFormat(text, locale)→applyCommonMessages() | High |

---

## 02-04 聚合+分类+聚类

### 聚合 (P1≥5 / P2 2-4 / P3 1)

**P1 核心机制 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | AbstractMessageSource.getMessageInternal — Template Method: locale兜底→缓存检查→resolveCode→formatMessage→父级委托 | 🔴 | **为什么🔴**: MessageSource的"发动机" — 三条getMessage重载→全部经getMessageInternal→这是Template Method模式的样板 — resolveCode给子类实现，formatMessage/MF缓存在父类 |
| P1-2 | AbstractMessageSource.getMessageFromParent — 父级责任链 | 🔴 | **为什么🔴**: 为什么容器级别的messageSource能兜底Controller级别的？parent委托链是HierarchicalMessageSource的实现 — self→parent→...→DelegatingMessageSource(返回默认消息) |
| P1-3 | ResourceBundleMessageSource.resolveCode/resolveCodeWithoutArguments — JDK ResourceBundle集成 | 🔴 | **为什么🔴**: 连接Spring和JDK标准 — basenames→ResourceBundle.getBundle→ISO-8859-1/UTF-8编码→NoSuchMessageException→try next basename — 多basename的容错和合并策略 |

**P2 支持机制 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | messageCache ConcurrentHashMap — MessageFormat 缓存 | 🟡 | **为什么🟡**: 性能优化 — MessageFormat创建成本高→按(code,locale)键缓存 → alwaysUseFullMessageFormat开关控制是否缓存空参MessageFormat |
| P2-2 | DelegatingMessageSource — 默认空实现 | 🟡 | **为什么🟡**: refresh() Step 7 默认创建的占位消息源 — 返回默认消息或原样返回code → 非关键路径但形成责任链终点 |

**P3 辅助 (1):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P3-1 | MessageSourceSupport.getCommonMessages — 公共消息覆盖 | 🟢 | **为什么🟢**: ReloadableResourceBundleMessageSource 支持动态刷新→公共消息更新 → 标准ResourceBundleMessageSource不受影响 — 扩展功能 |

### 聚类决策 (1篇)

**1篇理由**: 1242行/5文件 ≈ S1-4 循环依赖(823行/1篇)的规模 — 1篇(~42-49行)足够覆盖全部核心机制。不拆2篇 — MessageSource 的理念比实现重要(Template Method + Parent 委托 + JDK ResourceBundle 三个设计决策是同一故事线)。

**单篇结构**: §1 MessageSource 接口+getMessageInternal Template Method → §2 getMessageFromParent 父级责任链 → §3 ResourceBundleMessageSource JDK 集成+编码+缓存 → §4 refresh() Step 7 initMessageSource
