# S-25 Error 处理自动装配 — ErrorMvcAutoConfiguration → BasicErrorController → DefaultErrorAttributes

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | ErrorMvcAutoConfiguration.java(305行)+BasicErrorController.java(150行)+DefaultErrorAttributes.java(140行, boot/web/servlet/error)+ErrorViewResolver.java(40行)
> 基线: BOOT-PLAN-v2 S-25 (深探新增) — 错误响应机制; 前置: **S-7 MVC 自动装配 + C-13 异常(请求期异常解析)** — 展开错误响应/whitelabel//error 映射

---

## §0.8

- 🟡 Working，1篇 — 装配入口(ErrorMvcAutoConfiguration: @AutoConfiguration(before=WebMvcAutoConfiguration) + @ConditionalOnWebApplication(SERVLET) + @ConditionalOnClass → errorAttributes[DefaultErrorAttributes] + basicErrorController[BasicErrorController]) → 错误控制器与映射(BasicErrorController: @RequestMapping("${server.error.path:${error.path:/error}}") + errorHtml[TEXT_HTML→ModelAndView] + error[JSON→ResponseEntity]) → 错误属性与 whitelabel(DefaultErrorAttributes: timestamp/status/errorDetails/path + ErrorAttributeOptions 控制 stackTrace/message; WhitelabelErrorViewConfiguration: server.error.whitelabel.enabled + ErrorTemplateMissingCondition 回退)
- 设计模式: [模式: 条件装配]—@ConditionalOnWebApplication; [模式: 内容协商]—TEXT_HTML vs JSON; [模式: 兜底视图]—whitelabel

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ErrorMvcAutoConfiguration.java:86,87,90,98,104 | 入口 | **@AutoConfiguration(before=WebMvcAutoConfiguration)(L86)+@ConditionalOnWebApplication(SERVLET)(L87)**: errorAttributes Bean(L98/100, DefaultErrorAttributes)+basicErrorController(L104/106, BasicErrorController) | High |
| BasicErrorController.java:58,85,95 | 映射 | **@RequestMapping("${server.error.path:${error.path:/error}}")(L58)**: errorHtml(TEXT_HTML→ModelAndView)(L85-86)+error(JSON→ResponseEntity)(L95-96) | High |
| DefaultErrorAttributes.java:95,101,103 | 属性 | **getErrorAttributes(L95)→put timestamp(L103)+addStatus(L104)+addErrorDetails(L105)+addPath(L106)** — ErrorAttributeOptions 控制包含项 | High |
| ErrorMvcAutoConfiguration.java:145 | whitelabel | **@ConditionalOnBooleanProperty(server.error.whitelabel.enabled, matchIfMissing=true)(L145)+ErrorTemplateMissingCondition** — 无错误模板时兜底视图 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 错误处理是一条线(装配→控制器→属性), 3 块耦合 — 1篇 (~46行) 按"装配入口 → 控制器映射 → 属性与兜底"展开; C-13 的请求期异常解析链复用, 本域讲 Boot 的错误响应端点。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 装配入口与条件 (ErrorMvcAutoConfiguration) | 🔴 | **为什么🔴**: 错误处理何时装配 |
| P1-2 | BasicErrorController (/error 映射 + HTML/JSON) | 🔴 | **为什么🔴**: 统一错误端点 |
| P1-3 | DefaultErrorAttributes (错误属性集合 + options) | 🔴 | **为什么🔴**: 错误响应内容 |
| P2-1 | Whitelabel 错误页 (兜底视图) | 🟡 | **为什么🟡**: 无模板时回退 |
| P2-2 | ErrorViewResolver (错误视图解析) | 🟡 | **为什么🟡**: 错误页解析链 |
| P3-1 | 与 C-13/S-7 边界 (异常解析复用) | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **装配入口** | 🔴 | 何时装错误处理 |
| B | **控制器与属性** | 🔴 | /error 怎么响应 |
| C | **兜底与边界** | 🟡 | 无模板回退 |

> **Cluster A (§1)**: ErrorMvcAutoConfiguration 条件 + errorAttributes/basicErrorController Bean
> **Cluster B (§2)**: BasicErrorController(/error + HTML/JSON) + DefaultErrorAttributes(属性+options)
> **Cluster C (§3)**: Whitelabel 兜底 + ErrorViewResolver + C-13 边界

→ 引出 S-26: Actuator Health 聚合 — 错误之后: StatusAggregator/HealthContributorRegistry 的健康状态聚合(前置 S-21)
