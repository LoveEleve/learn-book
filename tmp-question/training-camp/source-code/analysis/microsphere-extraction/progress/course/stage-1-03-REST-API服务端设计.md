# stage-1 · 第 3 节：REST API 服务端设计 — 知识点提取

> 课程：stage-1 服务治理 第 3 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/03. 第三节：REST API 服务端设计.md`
> 提取时间：2026-08-09 | 权重：核心（REST API 是服务治理主线）

---

## 一、本节概览

- **技术域**：REST API 服务端设计（模型/校验/异常/POJO通讯/幂等/多版本）
- **维度**：`[规范]`（Bean Validation/Jakarta Validation）+ `[工程问题]`（WebMVC 流程）+ `[分布式问题]`（幂等性）
- **核心命题**：如何设计统一、健壮的 REST API 服务端——统一模型、统一校验、统一异常、隐形包装 POJO
- **知识点数**：9 个
- **前置**：REST 理论、Spring WebMVC 基本使用、Bean Validation 基本使用（docs 预备技能）

## 前置条件清单
读者需先掌握：
1. **REST 理论**（HTTP 方法/状态码/资源）
2. **Spring WebMVC 基本使用**（@RestController/@RequestMapping/@ControllerAdvice）
3. **Bean Validation 基本使用**（@Valid/@NotNull 等注解）
4. **泛型**（Java 泛型，理解 ApiBase<T> 需要）
未达前置者，先补：Spring 官方 WebMVC 入门 + Bean Validation 入门

## 掌握度
目标读者：**本人（读源码多，Spring WebMVC 熟悉）** — 已确认
讲解策略：WebMVC 核心流程直接讲（你熟悉源码）；Bean Validation/REST 模型/幂等性补机制讲解

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 服务端 API 通用模型设计（ApiBase<T>）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Java 泛型
- **需求**：统一 REST 请求/响应模型，让所有 API 有一致的结构
- **自主实现**：定义泛型基类 `ApiBase<T>`，含 body(泛型) + 元数据(headers/metadata)
- **参考实现**：`ApiBase<T>` 有 headers(Map)/metadata(MultiValueMap)/body(T)；headers/metadata 标 `@Deprecated`
- **对比取舍**：泛型 body 承载业务数据；headers/metadata 已废弃(被 @RequestHeader/参数替代)
- **测试佐证**：biz-api 的 ApiRequest/ApiResponse 存在；`@Deprecated` headers/metadata

### KP-02 API 请求模型设计（ApiRequest<T>）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、Bean Validation
- **需求**：定义统一请求模型，支持校验
- **自主实现**：`ApiRequest<T>` = 泛型 body + headers/metadata；body 加 `@Valid` 触发校验
- **参考实现**：ApiRequest<T> 的 body 字段标 `@Valid`（进入 Spring 校验管道）；getHeaders() 空值返回 emptyMap 防御
- **对比取舍**：`@Valid` 让请求体在校验管道中被校验；空值防御避免 NPE

### KP-03 API 响应模型设计（ApiResponse<T>）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **需求**：定义统一响应模型，模拟 HTTP Response
- **自主实现**：`ApiResponse<T>` = 业务 code + message + body，模拟 HTTP 状态码
- **参考实现**：ApiResponse<T> 有 code(String 或 int)/message/body(T)；docs 中 extends ApiBase，biz-api 实际为独立类含 code:int
- **对比取舍**：**业务错误码 ≠ HTTP 状态码**——业务层用 code 表达业务语义，HTTP 状态码表达传输层
- **测试佐证**：biz-api ApiResponse.java 含 `code:int` + `@Valid body`

### KP-04 API 业务 Code 设计（StatusCode 枚举）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：枚举
- **需求**：业务 Code 模拟 HTTP Status Code，支持国际化
- **自主实现**：用枚举定义业务状态码(code + message)，message 支持国际化占位符
- **参考实现**：StatusCode 枚举 OK(0)/FAILED(-1)/CONTINUE(1)；CONTINUE 的 message 是占位符 `{status-code.continue}`；getMessage() → getLocalizedMessage()（FIXME 国际化未实现）
- **对比取舍**：占位符 + getLocalizedMessage 是国际化的预留设计
- **测试佐证**：biz-api/enums/StatusCode.java 存在

### KP-05 服务端 API 校验设计（Bean Validation）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Bean Validation 基本使用
- **需求**：服务端统一校验请求参数
- **自主实现**：用 Bean Validation 注解(@Valid/@NotNull)声明式校验，避免手写校验逻辑
- **参考实现**：
  - 实现：Bean Validator（Hibernate Validator）
  - 依赖：Expression Language（EL 2.0+，如 Tomcat EL）
  - Spring 适配：`LocalValidatorFactoryBean` / `OptionalValidatorFactoryBean`
  - 不兼容框架：Netty / Hibernate / JBoss Common Logger（注意隔离）
- **对比取舍**：**推荐 Bean Validation 扩展，不推荐 Spring WebMVC 自定义扩展**——声明式、标准、可复用
- **待验证**：EL 2.0 具体版本要求

### KP-06 服务端 API 异常处理（统一异常）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：@ControllerAdvice
- **需求**：统一处理服务端异常，返回统一格式，避免异常泄露
- **自主实现**：用 `@RestControllerAdvice`/`@ControllerAdvice` + `@ExceptionHandler` 统一捕获
- **参考实现**：推荐 @RestControllerAdvice/@ControllerAdvice；`RequestResponseBodyMethodProcessor` 处理 @RestController 参数/返回；`HandlerExceptionResolver` 解析异常
- **对比取舍**：统一异常处理保证响应格式一致，避免堆栈泄露
- **测试佐证**：biz-web `ExceptionHandlerConfiguration.java` 存在

### KP-07 服务端 API POJO 通讯（隐形包装）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：HandlerMethodReturnValueHandler
- **需求**：@RestController 返回 POJO 时自动包装成 ApiResponse<T>，业务代码无感知
- **自主实现**：实现 `HandlerMethodReturnValueHandler`，把方法返回的 POJO 自动包成 ApiResponse
- **参考实现**：`ApiResponseHandlerMethodReturnValueHandler` 统一封装返回值 POJO T → ApiResponse<T>；`ApiResponseBodyAdvice` 也实现
- **对比取舍**：隐形包装让业务方法只返回 POJO，框架自动加响应壳——统一格式且不侵入业务
- **测试佐证**：biz-web `ApiResponseHandlerMethodReturnValueHandler.java` + `ApiResponseBodyAdvice.java`

### KP-08 Spring WebMVC 核心流程（DispatcherServlet）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Servlet 规范、Spring WebMVC
- **需求**：理解 HTTP 请求在 WebMVC 中的完整处理链路
- **自主实现**：请求 → 前端控制器 → 找 Handler → 适配执行 → 渲染/写响应
- **参考实现**（docs 五步流程）：
  1. **DispatcherServlet** 处理 HTTP 请求（符合 Servlet Mapping）
  2. **HandlerMapping**（按优先级排序）找最匹配的 `HandlerExecutionChain`（= Handler + N 个 HandlerInterceptor）
  3. **HandlerAdapter** 找合适的适配器执行 Handler
  4. 执行返回 ModelAndView：@RestController 已在过程中写 HTTP Response（View 不渲染）；@Controller 执行 View 渲染
  5. ModelAndView → HTTP Response
- **对比取舍**：`DispatcherServlet → HandlerMapping → HandlerExecutionChain → HandlerAdapter → ModelAndView`；`@ResponseBody` 派生方式框架内部完成
- **待验证**：可用 spring-framework 源码进一步验证链路

### KP-09 服务端 API 幂等性 + 多版本
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：Redis、Web 幂等性
- **需求**：REST 幂等性校验 + 多版本 API 平滑升级
- **自主实现**：幂等——用 Redis 判断请求 Token 是否已处理，存在则拦截；多版本——@RequestMapping 支持版本并行
- **参考实现**：docs 提到"常规实现：Redis 判断请求 Token 是否存在"；"通用实现"未展开；多版本基于 WebMVC
- **对比取舍**：Redis 幂等是常规方案；多版本用 URI/版本 header 实现并行（docs 未展开，Medium 置信度）
- **待验证**：多版本 API 具体实现 docs 未展开

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| 通用模型 ApiBase | 工程 | 核心 | P1 | 🔴 | High |
| 请求模型 ApiRequest | 工程 | 核心 | P1 | 🔴 | High |
| 响应模型 ApiResponse | 工程 | 核心 | P1 | 🔴 | High |
| 业务 Code | 工程 | 核心 | P1 | 🟡 | High |
| Bean Validation 校验 | 规范 | 核心 | P1 | 🔴 | High |
| 统一异常处理 | 工程 | 核心 | P1 | 🔴 | High |
| POJO 隐形包装 | 工程 | 核心 | P1 | 🔴 | High |
| WebMVC 核心流程 | 工程 | 核心 | P1 | 🔴 | High |
| 幂等 + 多版本 | 分布式 | 支撑 | P2 | 🟡 | Medium |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **API 模型**：biz-api 的 ApiRequest/ApiResponse/StatusCode 存在 = 模型设计实证
- **隐形包装**：biz-web 的 ApiResponseHandlerMethodReturnValueHandler + ApiResponseBodyAdvice = POJO 包装实证
- **统一异常**：biz-web ExceptionHandlerConfiguration = 统一异常实证
- `[待验证]` microsphere-spring-web 是否有类似 HandlerMethodReturnValueHandler

---

## 五、本节小结（三层次视角）

**需求**：设计统一、健壮的 REST API 服务端——统一模型、统一校验、统一异常、隐形 POJO 包装、幂等、多版本。

**自主实现核心**：若我设计——
1. `ApiBase<T>/ApiRequest<T>/ApiResponse<T>` 泛型模型统一结构
2. StatusCode 枚举 + 国际化占位符
3. Bean Validation 声明式校验
4. @RestControllerAdvice 统一异常
5. HandlerMethodReturnValueHandler 隐形包装 POJO → ApiResponse
6. 基于 DispatcherServlet 核心流程理解链路

**参考实现**：小马哥 biz-api/biz-web 的 ApiResponse + ReturnValueHandler + ExceptionHandler 实证。

**对比取舍**：本篇技术含量高——**核心是理解 WebMVC 流程**(你熟悉源码) + **统一模型/校验/异常/包装的设计思想**。业务错误码 ≠ HTTP 状态码是关键认知。幂等/多版本 docs 未展开(Medium)。
