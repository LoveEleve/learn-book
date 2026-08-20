# C-12 拦截器 — HandlerInterceptor (三方法 → 执行链 → doDispatch 插入点)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | HandlerInterceptor(159行)+HandlerExecutionChain(212行)+WebMvcConfigurer(256行)+WebMvcConfigurationSupport(1234行)+InterceptorRegistry(85行)
> 基线: W-1 DispatcherServlet doDispatch — 处理前后有 pre/post 钩子 — 本域展开拦截器三方法与执行顺序; 原始执行计划 7-5

---

## §0.8

- 🟡 Working，1篇 — 接口(HandlerInterceptor 三方法: preHandle/postHandle/afterCompletion, 全部 default) → 执行链(HandlerExecutionChain: applyPreHandle 顺序→applyPostHandle 逆序→triggerAfterCompletion 逆序且仅 interceptorIndex 内) → doDispatch 插入点(L1084 前/L1089 处理/L1096 后/L1109 异常兜底) → 注册(WebMvcConfigurer.addInterceptors→InterceptorRegistry)
- 设计模式: [模式: 责任链]—拦截器链; [模式: 模板方法]—doDispatch 固定流程, 拦截器是插入钩子; [模式: 栈式清理]—preHandle 顺序进, postHandle/afterCompletion 逆序出

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HandlerInterceptor.java:81,103,130,155 | 三方法 | **接口契约**: preHandle L103(处理器前, 返回 false 中断链) / postHandle L130(处理器后、渲染前) / afterCompletion L155(请求结束后, 无论成败, 可做清理) — 全 default | High |
| HandlerExecutionChain.java:46,48 | 链状态 | **执行链**: interceptorList(有序) + interceptorIndex(已成功 pre 的个数) | High |
| HandlerExecutionChain.java:143 | applyPreHandle() | **正序执行**: L144 遍历 → interceptor.preHandle → false→triggerAfterCompletion+return false(中断); true→interceptorIndex=i — **顺序执行** | High |
| HandlerExecutionChain.java:158 | applyPostHandle() | **逆序执行**: L161 从 size-1 到 0 — 与 pre 相反 | High |
| HandlerExecutionChain.java:172 | triggerAfterCompletion() | **逆序清理**: L173 从 interceptorIndex 到 0 — 只清理 preHandle 成功的 | High |
| DispatcherServlet.java:1084,1089,1096,1106,1109 | doDispatch 插入点 | **生命周期**: L1084 applyPreHandle(false→return) → L1089 ha.handle → L1096 applyPostHandle → L1106 processDispatchResult(渲染) → 异常 L1109/1112 triggerAfterCompletion | High |
| WebMvcConfigurer.java:96 | 注册 | **扩展点**: addInterceptors(InterceptorRegistry) — 覆写注册拦截器; WebMvcConfigurationSupport L366-367 创建 registry 调 addInterceptors | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+执行链+注册约 2000 行 — 知识单线: "三方法 → 执行链正逆序 → doDispatch 插入 → 注册". 1篇 (~45行) 按"接口→执行链→doDispatch→注册"展开; 若分 2 篇则执行链与 doDispatch 割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | HandlerExecutionChain 执行顺序 (preHandle 正序→postHandle 逆序→afterCompletion 逆序) | 🔴 | **为什么🔴**: 拦截器最核心的心智 — "正进逆出"的栈式语义与 interceptorIndex 部分清理 |
| P1-2 | doDispatch 插入点与生命周期 (L1084/1089/1096/1106/1109) | 🔴 | **为什么🔴**: 拦截器在请求全流程中的位置 — 与 W-1 doDispatch 衔接 |
| P1-3 | HandlerInterceptor 三方法语义 (pre/post/after 各自时机) | 🔴 | **为什么🔴**: 三个钩子"什么时候能做什么" — 权限校验(pre)/改响应头(post)/资源清理(after) |
| P2-1 | applyPreHandle false 中断链 (triggerAfterCompletion + return) | 🟡 | **为什么🟡**: 拦截失败的短路语义 — 权限不足即返回 |
| P2-2 | WebMvcConfigurer.addInterceptors 注册与排序 | 🟡 | **为什么🟡**: 怎么注册 + 顺序控制(与 C-4 衔接) |
| P3-1 | 拦截器 vs Servlet Filter (时机差异) | 🟢 | **为什么🟢**: 概念区分 — Filter 在 Servlet 层更早, 拦截器在 HandlerMapping 后 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **执行链顺序** (HandlerExecutionChain 正逆序) | 🔴 | 核心心智 — 栈式清理 |
| B | **doDispatch 生命周期** (插入点) | 🔴 | 拦截器在请求中的位置 |
| C | **接口与注册** (三方法 + WebMvcConfigurer) | 🟡 | 使用侧 |

> **Cluster A (§1)**: HandlerInterceptor 三方法 + 执行链(preHandle/postHandle/afterCompletion 顺序)
> **Cluster B (§2)**: doDispatch 插入点(1084/1089/1096/1106/1109) + 中断链语义
> **Cluster C (§3)**: 注册(WebMvcConfigurer.addInterceptors) + 排序 + 与 Filter 对照

→ 引出 7-6: 异常处理 — 拦截器/处理器抛的异常最终进 processDispatchResult → HandlerExceptionResolver(@ExceptionHandler/@ControllerAdvice)

(End of file - total 61 lines)
