# S2-4 MessageSource — Spring 国际化的 Template Method 链

> 依赖 S2-1 refresh() | 🟡 Working | 3 KP | [模式: Template Method + 责任链]

**读者处境**: refresh() 完成到 Step 7 `initMessageSource()` — 容器里有了一个 MessageSource bean — 但 `messageSource.getMessage("user.notfound", new Object[]{userId}, Locale.CHINA)` 具体是怎么找到 `用户{0}未找到` 这个文本的？

### 1. MessageSource 接口 + getMessageInternal — Template Method 的经典样板

场景: Controller 中 `messageSource.getMessage("error.timeout", null, Locale.getDefault())` → AbstractApplicationContext.getMessage(L1529-1540) → 委托给 messageSource bean → 进入 AbstractMessageSource.getMessage 三个重载之一 → 全部汇聚到 getMessageInternal — 这是 Template Method 的展示窗口。

源码路径:
- `AbstractMessageSource.java:141-149` — **getMessage(code, args, default, locale)**: 带默认消息版本 — getMessageInternal→null→renderDefaultMessage(default, args, locale) — MessageFormat 渲染默认消息后返回
- `AbstractMessageSource.java:153-166` — **getMessage(code, args, locale)**: 无默认消息版本 — getMessageInternal→null→getDefaultMessage(code) 兜底(L158-160)→仍为null→throw NoSuchMessageException(L163/166)
- `AbstractMessageSource.java:210-254` — **getMessageInternal()**: Template Method 核心骨架 — ①locale=null→Locale.getDefault() 兜底(L214-215) ②无参且非alwaysUseMessageFormat→resolveCodeWithoutArguments→返回纯文本(L219-225) / 有参→resolveArguments→resolveCode(L236)→synchronized(messageFormat).format 渲染 {0}{1}(L238-239) ③commonMessages 兜底→formatMessage(L245-249) ④返回null→getMessageFromParent 父级委托(L254)

关键设计: **Why 解析方法有两个而非一个？** 只有 resolveCode 是 abstract(L403, 返回 MessageFormat 对象) — resolveCodeWithoutArguments 是具体方法(L379-386): 默认实现仍委托 resolveCode 并 synchronized format(new Object[0]) — 子类可覆写为"无参直接返回纯文本"的快速路径。**性能优化**: 如果消息无参数({0})，跳过 MessageFormat 对象创建 — 直接返回 String。如果只有一个 resolveCode，每次都要 new MessageFormat — 即使最终不需要格式化。这是 Template Method 的精细化: 骨架在父类 — **两个方法让子类可以选择提供纯文本(fast)或MessageFormat(slow) — 父类formatter不关心子类的选择**。[模式: Template Method + 策略模式]

数据流: getMessage("user.notfound", [userId], Locale.CHINA) → L142 getMessageInternal("user.notfound", [userId], Locale.CHINA) → L214 locale非null→跳过兜底→有参→走else分支→L236 resolveCode("user.notfound", locale)→ResourceBundleMessageSource: ResourceBundle.getBundle("messages_zh_CN")→getStringOrNull("user.notfound")→"用户{0}未找到"→new MessageFormat("用户{0}未找到", locale)→L238-240 synchronized(messageFormat).format([userId])→"用户12345未找到"→返回

### 2. getMessageFromParent — 父级责任链的递进委托

场景: Controller 的 messageSource 没找到 "error.db.connection" — 但有 parent messageSource(容器全局的) — parent 找到了。Spring 通过 HierarchicalMessageSource 实现这种"局部→全局"的递进查找 — 类似于 ClassLoader 的双亲委派。

源码路径:
- `AbstractMessageSource.java:267-282` — **getMessageFromParent()**: ①parent instanceof AbstractMessageSource→parent.getMessageInternal(code, args, locale) **直调内部方法**(L270-273) ②parent非null但不是AbstractMessageSource(自定义MessageSource实现/DelegatingMessageSource)→parent.getMessage(code, args, null, locale)(L277-278)→走标准路径 ③parent==null→return null(责任链终点, 交给调用方处理)(L281-282)
- `AbstractMessageSource.java:254` — **getMessageInternal 中的调用**: resolveCode与commonMessages都返回null→getMessageFromParent→如果parent也返回null→回到调用方的throw/return default分支

关键设计: **Why parent instanceof AbstractMessageSource 走内部方法？** 源码注释(L271-272)给出的真实理由: **避免 useCodeAsDefaultMessage 被激活时把 code 字符串本身当作默认消息返回** — 若走外层 parent.getMessage(code, args, null, locale)，父级开启 useCodeAsDefaultMessage 时会直接把 code 当作消息返回，子级就无法继续尝试其它 codes 或抛出真正的 NoSuchMessageException；直调 getMessageInternal 返回 null 才保留这些语义。省一层方法调用只是附带效果。

数据流: childMessageSource.getMessageInternal("db.error", args, locale)→resolveCode→ResourceBundle没找到→return null→L254 getMessageFromParent("db.error", args, locale)→L270 parent instanceof AbstractMessageSource→true→L273 parent.getMessageInternal("db.error", args, locale)→parent.resolveCode→ResourceBundle找到"数据库连接失败: {0}"→formatMessage→返回→childMessageSource返回格式化消息→Controller收到

### 3. ResourceBundleMessageSource — JDK ResourceBundle 的 Spring 适配

场景: Spring Boot `spring.messages.basename=messages,errors` → ResourceBundleMessageSource.setBasenames("messages", "errors") → getMessage("order.created") → 先在 messages.properties 找 → 没找到 → 再在 errors.properties 找 — 两个 basename 按顺序"合并"为一个虚拟的 ResourceBundle。

源码路径:
- `ResourceBundleMessageSource.java:setBasenames()` — 可设多个 basename("messages","errors") → 按顺序尝试 — 找到即停止(非合并)
- `ResourceBundleMessageSource.java:resolveCodeWithoutArguments()` — 遍历basenames→ResourceBundle.getBundle(basename, locale)→getStringOrNull(bundle, code)(L361-371): containsKey 预检(L362)→getString→catch MissingResourceException 兜底(L366)→try next basename→所有basename穷尽→return null — 不涉及 NoSuchMessageException(那是 Spring 上层异常, 由 AbstractMessageSource 抛出)
- `ResourceBundleMessageSource.java:110-111` — 默认编码: 类中**无 DEFAULT_ENCODING 常量** — 构造器内 `setDefaultEncoding("ISO-8859-1")`(L111, JDK ResourceBundle 标准) → 可通过 setDefaultEncoding("UTF-8") 覆盖(Spring Boot 默认 UTF-8)

关键设计: **多 basename 是"按序查找"而非"合并"** — messages.properties 和 errors.properties 都是独立的 ResourceBundle — 如果 messages.properties 有 "greeting.hello" — errors.properties 中就算也有 — 永远不会用到 errors 中的版本。这与 Spring Boot 的 `spring.config.import`(合并)不同 — 这里是"优先"而非"合并"。

数据流: ResourceBundleMessageSource.resolveCode("order.created", zh_CN)→遍历basenames: ①"messages": getResourceBundle("messages_zh_CN")→getMessageFormat(bundle,"order.created",zh_CN)→getStringOrNull→null→ ②"errors": getResourceBundle("errors_zh_CN")→getMessageFormat(bundle,"order.created",zh_CN)(L315): cachedBundleMessageFormats 三层嵌套Map查询(bundle→code→locale)(L318-325)→未命中→getStringOrNull→"订单已创建"→createMessageFormat→put 缓存(L333-339)→返回MessageFormat — 缓存写入**无条件**触发(getMessageFormat 每次被调用都会查/写缓存, 与消息有无{0}占位符无关); 无参消息走 getMessageInternal 的 resolveCodeWithoutArguments 快速路径(L219-225)→返回纯文本String, 不经该缓存

→ spring-context 第四域完成。MessageSource 从接口到 Template Method 到 JDK 集成到 Parent 委托 — 4层架构。引出 S2-5: 父子容器 — 为什么 parent MessageSource 能跨容器共享？父容器的 Bean 怎么被子容器"继承"？
