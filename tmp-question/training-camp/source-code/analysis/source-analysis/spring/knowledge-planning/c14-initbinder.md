# C-14 @InitBinder — WebDataBinder 定制 (绑定 → @InitBinder → @DateTimeFormat)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | WebDataBinder(约300行)+DataBinder(约1300行)+InitBinderDataBinderFactory(约100行)+WebDataBinderFactory(52行)+@InitBinder(约80行)+@DateTimeFormat(约120行)+@NumberFormat(约110行)+FormattingConversionService(约130行)
> 基线: W-4 参数绑定 + C-13 异常 — @RequestBody 校验、表单绑定的承载者是 WebDataBinder — 本域展开 binder 定制与格式化注解; 原始执行计划 7-A

---

## §0.8

- 🟡 Working，1篇 — 绑定器(WebDataBinder extends DataBinder: bind→doBind 属性填充+类型转换+BindingResult) → 创建(WebDataBinderFactory.createBinder → InitBinderDataBinderFactory) → 定制(@InitBinder 方法: 本地+@ControllerAdvice 全局, 注册自定义编辑器/验证器) → 格式化注解(@DateTimeFormat/@NumberFormat → FormattingConversionService.addFormatterForFieldAnnotation → 注解驱动转换器)
- 设计模式: [模式: 模板方法]—DataBinder.bind 骨架; [模式: 工厂]—WebDataBinderFactory 创建 binder; [模式: 策略]—@InitBinder 注入定制

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| WebDataBinder.java:65 | 绑定器 | **WebDataBinder**: extends DataBinder — Web 场景的绑定器(属性前缀等) | High |
| DataBinder.java:1242,434,228 | 绑定核心 | **bind(pvs)**: L1242→doBind(属性值→target 填充, 含类型转换 via TypeConverter); getBindingResult L434(校验结果); getTarget L228 | High |
| WebDataBinderFactory.java:42 | 工厂接口 | **createBinder(webRequest, target, objectName)**: 每个请求参数/模型对象建一个 binder | High |
| InitBinderDataBinderFactory.java:39,49 | 创建+定制 | **InitBinderDataBinderFactory**: 构造器(L49, 收 @InitBinder 方法) — 创建 binder 后遍历调用 @InitBinder 方法(注册自定义 PropertyEditor/验证器/字段前缀) | High |
| RequestMappingHandlerAdapter.java:209,211,592 | 方法收集 | **initBinderCache**(本地 L209)/initBinderAdviceCache(@ControllerAdvice L211) — initControllerAdviceCache L592 收集; 与 C-13 异常 advice 同机制 | High |
| FormattingConversionService.java:53,100 | 格式化 | **addFormatterForFieldAnnotation**: 注解→格式化器工厂 — @DateTimeFormat/@NumberFormat 注解驱动的转换 | High |
| DateTimeFormat.java:89 / NumberFormat.java:50 | 注解 | **声明**: @DateTimeFormat(pattern/style) / @NumberFormat(pattern) — 标在字段/参数上, 由转换服务解析 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 绑定器+工厂+注解+格式化服务约 2200 行 — 知识主线: "WebDataBinder 是绑定的承载 → @InitBinder 定制它 → @DateTimeFormat 靠格式化注解驱动". 1篇 (~45行) 按"绑定器→创建定制→格式化"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | DataBinder 绑定核心 (bind→doBind 属性填充+类型转换+BindingResult) | 🔴 | **为什么🔴**: @RequestBody 校验/表单绑定都靠它 — 参数绑定(C-2 转换)在 binder 内完成 |
| P1-2 | InitBinderDataBinderFactory (@InitBinder 方法收集+创建后应用) | 🔴 | **为什么🔴**: 定制 binder 的机制 — 注册自定义编辑器/验证器的标准做法 |
| P1-3 | @InitBinder 双来源 (本地 + @ControllerAdvice 全局) | 🔴 | **为什么🔴**: 局部 vs 全局定制 — 与 C-13 advice 同收集机制 |
| P2-1 | WebDataBinderFactory.createBinder (工厂模式) | 🟡 | **为什么🟡**: binder 生命周期 — 每请求每对象一个 |
| P2-2 | @DateTimeFormat/@NumberFormat → FormattingConversionService | 🟡 | **为什么🟡**: 注解驱动格式化 — 声明式日期/数字转换 |
| P3-1 | WebDataBinder 前缀 (fieldMarkerPrefix "_" 等) | 🟢 | **为什么🟢**: Web 表单特殊语义 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **绑定器核心** (DataBinder bind/转换/BindingResult) | 🔴 | 绑定的执行引擎 |
| B | **定制机制** (@InitBinder + 工厂 + advice 全局) | 🔴 | 怎么定制 binder |
| C | **格式化注解** (@DateTimeFormat → 转换服务) | 🟡 | 声明式日期/数字转换 |

> **Cluster A (§1)**: WebDataBinder/DataBinder — bind/doBind 属性填充+类型转换+BindingResult
> **Cluster B (§2)**: WebDataBinderFactory + InitBinderDataBinderFactory(@InitBinder) + 本地/advice 双来源
> **Cluster C (§3)**: @DateTimeFormat/@NumberFormat → FormattingConversionService 注解驱动

→ 引出 7-B: WebFlux — 从 MVC(阻塞/Servlet)到响应式 — RouterFunction/HandlerFunction 与 WebClient — 参数绑定/异常处理的响应式对照

(End of file - total 61 lines)
