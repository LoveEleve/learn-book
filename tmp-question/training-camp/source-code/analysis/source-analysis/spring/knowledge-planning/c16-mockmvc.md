# C-16 MockMvc — 控制器测试 (MockMvcBuilders → perform → andExpect)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | MockMvc(246行)+MockMvcBuilders(99行)+TestDispatcherServlet(156行)+MockHttpServletRequestBuilder(242行)+MockMvcResultMatchers(265行)
> 基线: W-1~W-6 — MockMvc 用真实 DispatcherServlet+HandlerMapping+Adapter 测试 Controller, 但不启动容器 — spring-test 层首域; 原始执行计划 8-1

---

## §0.8

- 🟡 Working，1篇 — 装配(MockMvcBuilders: webAppContextSetup 全容器 / standaloneSetup 轻量) → 执行(MockMvc.perform: MockHttpServletRequestBuilder.buildRequest→MockFilterChain.doFilter→TestDispatcherServlet 走真实 MVC 全链路→ResultActions) → 断言(ResultActions.andExpect: status/content/jsonPath/view/redirectedUrl) → 与真实容器对照
- 设计模式: [模式: 前端控制器复用]—MockMvc 复用真实 DispatcherServlet; [模式: 建造者]—MockMvcBuilders/MockHttpServletRequestBuilder 链式; [模式: 门面]—ResultActions 收口断言

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MockMvcBuilders.java:37,51,76 | 装配 | **两种构建**: webAppContextSetup(ctx)(L51, 全容器+配置) / standaloneSetup(controllers)(L76, 仅指定控制器, 轻量快速) | High |
| MockMvc.java:71,167 | 执行 | **perform(builder)**: L167: requestBuilder.buildRequest → 默认请求合并 → MockHttpServletResponse → MockFilterChain.doFilter(this.servlet, filters)(L203) | High |
| MockMvc.java:200 | 委托 | **真实链路**: MockFilterChain 调 TestDispatcherServlet(真实 DispatcherServlet 子类) — doGet/doPost → 完整 HandlerMapping+Adapter 流程 — 不启动 Servlet 容器 | High |
| TestDispatcherServlet.java:53,67 | 适配 | **TestDispatcherServlet**: extends DispatcherServlet — service 覆写支持测试(Mock request/response) | High |
| MockHttpServletRequestBuilder.java:54,73,153 | 请求构建 | **链式请求**: uri(L73)/content(L113)/header(L143)/param(L153)/get/post — 构建 MockHttpServletRequest | High |
| MockMvcResultMatchers.java:45,166,180 | 断言 | **断言工厂**: status()(L166)/content()(L180)/view()(L74)/redirectedUrl(L129)/jsonPath — ResultMatcher 校验 MvcResult | High |
| MockMvc.java:214,224 | ResultActions | **断言执行**: andExpect(matcher.match(mvcResult))(L214)/andDo(handler)/andReturn(L224 返回 MvcResult) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 建造者+执行器+断言约 1000 行 — 知识单线: "装配 MockMvc → perform 走真实 MVC → andExpect 断言". 1篇 (~46行) 按"装配→执行→断言"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | MockMvc.perform 全链路 (buildRequest→MockFilterChain→TestDispatcherServlet→ResultActions) | 🔴 | **为什么🔴**: 核心 — MockMvc 复用真实 MVC 全链路(HandlerMapping/Adapter/渲染), 只 mock Servlet 容器 |
| P1-2 | MockMvcBuilders 两种装配 (webAppContextSetup vs standaloneSetup) | 🔴 | **为什么🔴**: 全容器(集成) vs 轻量(单元)的选型 — 测试风格决策 |
| P1-3 | ResultActions/andExpect + MockMvcResultMatchers (status/content/view/jsonPath) | 🔴 | **为什么🔴**: 断言方式 — 校验 HTTP 语义(状态码/内容/视图/重定向) |
| P2-1 | MockHttpServletRequestBuilder (uri/param/content/header 链式请求) | 🟡 | **为什么🟡**: 请求构建 — 与真实请求参数映射 |
| P2-2 | TestDispatcherServlet (为何复用而非重写) | 🟡 | **为什么🟡**: 真实 MVC 逻辑可测的设计 — 与 W-1 DispatcherServlet 衔接 |
| P3-1 | MockMvcResultMatchers.jsonPath (JSON 断言) | 🟢 | **为什么🟢**: 最常用的内容断言 — JSONPath 表达式 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **装配** (MockMvcBuilders 双模式) | 🔴 | 集成 vs 单元测试风格 |
| B | **执行链路** (perform→真实 DispatcherServlet) | 🔴 | MockMvc 的核心价值 — 复用 MVC |
| C | **断言** (ResultActions/Matchers) | 🔴 | 校验请求结果 |

> **Cluster A (§1)**: MockMvcBuilders.webAppContextSetup/standaloneSetup + MockMvc 构造
> **Cluster B (§2)**: MockMvc.perform(请求构建→MockFilterChain→TestDispatcherServlet 全链路)
> **Cluster C (§3)**: ResultActions.andExpect + MockMvcResultMatchers(status/content/view/jsonPath)

→ 引出 8-2: TestContext — MockMvc 的 webAppContextSetup 需要 ApplicationContext — TestContext 框架(@SpringBootTest/@ContextConfiguration/TestExecutionListener)管理测试上下文缓存与生命周期

(End of file - total 61 lines)
