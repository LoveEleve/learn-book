# C-14 @InitBinder — WebDataBinder 定制 (绑定 → @InitBinder → 格式化注解)

> 依赖 W-4 参数绑定 + C-13 异常 | 🟡 Working | 6 KP | [模式: 模板方法 + 工厂 + 策略]

**读者处境**: 表单提交的对象、@RequestBody 校验、日期字段格式化 — 全靠 WebDataBinder。怎么自定义绑定(排除字段/加验证器/自定义格式化)？@InitBinder 和 @DateTimeFormat 各干什么？

### 1. DataBinder 绑定核心 — 属性填充 + 类型转换 + BindingResult

场景: 表单 POST user.name=Alice&user.age=25 → Spring 创建 User 对象并填充属性 — 这就是 DataBinder: 把参数值绑定到目标对象, 同时做类型转换(String→int)和校验。

源码路径:
- `WebDataBinder.java:65` — **绑定器**: extends DataBinder — Web 场景(字段前缀/标记)
- `DataBinder.java:1242` — **bind(pvs)**: L1242 签名 → 转 MutablePropertyValues → doBind(把属性值应用到 target: BeanWrapper 设置字段, 类型转换走 TypeConverter(C-2 衔接))
- `DataBinder.java:434` — **getBindingResult()**: 绑定+校验结果(Errors) — @RequestBody 校验失败时 bindingResult.hasErrors()
- `DataBinder.java:228` — **getTarget()**: 被绑定的目标对象

关键设计: **Why DataBinder 而非直接反射 setter？** 绑定要兼顾: 类型转换(字符串→属性类型)、嵌套属性(user.address.city)、集合绑定、忽略未知字段、校验收集 — 一套可配置的绑定引擎; 且转换失败不中断, 记录进 BindingResult 供校验层汇总报错。[模式: 模板方法 — 绑定骨架]

数据流: 表单提交 → getMethodArgumentValues → ModelAttributeMethodProcessor → binderFactory.createBinder(user, "user") → binder.bind(requestParams)(L1242): doBind → BeanWrapper.setPropertyValue("name", "Alice") + convertIfNecessary("25", Integer) → 25 → bindingResult 记录 → validateIfApplicable(@Valid 校验) → 有错抛 MethodArgumentNotValidException(C-13)。

### 2. @InitBinder — 定制 binder 的机制 (本地 + 全局)

场景: `@InitBinder void initBinder(WebDataBinder binder) { binder.setDisallowedFields("id"); binder.addValidators(custom); }` — 这个初始化方法怎么被调用？怎么对所有请求生效？

源码路径:
- `WebDataBinderFactory.java:42` — **createBinder(webRequest, target, objectName)**: 每请求每对象建一个 binder
- `InitBinderDataBinderFactory.java:39,49` — **定制工厂**: 构造器 L49 收 @InitBinder 方法列表; 创建 binder 后遍历调用这些方法(setDisallowedFields/addValidators/registerCustomEditor)
- `RequestMappingHandlerAdapter.java:209,211,592` — **双来源收集**: initBinderCache(本地控制器 L209)/initBinderAdviceCache(@ControllerAdvice L211) — initControllerAdviceCache L592 与 C-13 异常 advice 同机制收集; getDataBinderFactory 合并本地+全局

关键设计: **Why 用工厂+方法回调而非子类覆写？** 每个控制器/全局 advice 的 @InitBinder 不同 — 工厂把"创建 binder"+“应用该请求的 @InitBinder 们"组合, 实现声明式定制; 本地+advice 合并让全局规则(如统一日期格式)和局部规则(如某接口禁用字段)叠加。[模式: 工厂 + 回调]

数据流: 请求 handler → RequestMappingHandlerAdapter.getDataBinderFactory → InitBinderDataBinderFactory(本地 @InitBinder + 全局 advice @InitBinder) → 参数解析时 binderFactory.createBinder → 创建 WebDataBinder → 遍历调用各 @InitBinder: setDisallowedFields("id")/addValidators(...) → 返回定制好的 binder → 后续绑定/校验用。

### 3. @DateTimeFormat/@NumberFormat — 注解驱动的格式化

场景: `@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate date` — 字符串→LocalDate 转换不用手写, 注解声明即可。

源码路径:
- `DateTimeFormat.java:89` / `NumberFormat.java:50` — **注解**: pattern/style — 标在字段/方法参数上
- `FormattingConversionService.java:53,100` — **注解驱动**: addFormatterForFieldAnnotation(AnnotationFormatterFactory) L100 — 读到 @DateTimeFormat → 建 DateTimeFormatter → 注册为 String↔类型 转换器
- 衔接: WebMvcConfigurationSupport 用 FormattingConversionService 作为 conversionService → 注入 binder 的 typeConverter(C-2) → @DateTimeFormat 生效

关键设计: **Why 注解驱动而非硬编码格式化器？** 声明式: 字段上写注解即声明"这个字段用此格式" — 转换服务扫描注解自动注册, 比手写 Converter 简洁且可配(pattern 在注解上); 这是 C-2 转换体系在 Web 层的"注解化扩展"。[模式: 注解→格式化器工厂]

数据流: @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate birthDate → FormattingConversionService.addFormatterForFieldAnnotation 注册 DateTimeFormatter → 表单 "1999-01-01" → binder.convertIfNecessary("1999-01-01", LocalDate) → typeConverter 查转换器 → @DateTimeFormat 注解 → DateTimeFormatter → LocalDate(1999-01-01)。@NumberFormat(pattern="#,##0.00") BigDecimal → 同理。

→ 引出 7-B: WebFlux — 从 MVC(阻塞 Servlet)到响应式(RouterFunction/HandlerFunction/WebClient) — 响应式数据流如何改变 Web 请求处理模型。
