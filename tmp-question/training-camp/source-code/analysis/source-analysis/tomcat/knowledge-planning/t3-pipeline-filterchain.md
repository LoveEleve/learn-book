# T-3 Pipeline+双链 — 知识规划

> 项目: Tomcat 10.1.x | 类型: **规范参考实现** (Servlet 6.0 / Jakarta EE)
> 核心文件: 7 文件 / 1457 行 | 预计 4 篇 v5 大纲
> 基线: T-2 Adapter 把请求交给 `engine.getPipeline().getFirst().invoke()` — T-3 展开 Pipeline 内部全部流程

---

## §0.8 域审核前置

### 1. 域过载检查
- 核心类: 7 个 (Pipeline/Valve/StandardEngineValve/StandardHostValve/StandardContextValve/StandardWrapperValve/ApplicationFilterChain)
- 7 ≤ 10 + 1457 行 ≤ 5000 → **不触发拆分** ✅

### 2. 淘汰清单
- 全链在 Spring Boot 嵌入式 Tomcat 和生产部署中都是核心请求处理路径 → 无淘汰项 ✅

### 3. 规范缺口
T-3 映射的 Servlet 规范:
- `jakarta.servlet.Filter` → `ApplicationFilterConfig` + `ApplicationFilterChain`
- `jakarta.servlet.FilterChain` → `ApplicationFilterChain`
- `jakarta.servlet.FilterRegistration.Dynamic` → Context 层 addFilter
- `jakarta.servlet.Servlet` → StandardWrapperValve 中的 servlet.service()

### 4. 禁止过度加域
- T-3 = Valve 链 + Filter 链 — "双链" 命名准确
- T-1 ContainerBase 已预留 pipeline 字段 + basic Valve — T-3 展开其运行机制 ✅
- ApplicationDispatcher (请求转发) 不在 T-3 范围 — Servlet 规范维度非 Pipeline 架构

### 项目类型判定
| 维度 | 值 |
|------|------|
| 类型 | 规范参考实现 |
| 规范 | Servlet 6.0 (Jakarta EE 10) |
| 核心模式 | **Chain of Responsibility** — Pipeline 的标准教科书实现 |

---

## 01 提取 — 逐源映射

### Pipeline.java (124 行 — 请求处理流水线接口)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Pipeline.java:24-32 | **basic Valve + 附加 Valves**: basic 总是最后执行(容器自身的处理)，addValve 追加的 Valves 在 basic 之前。执行顺序: addValve 顺序 → basic | High |
| Pipeline.java:43 | **getBasic()**: 返回容器特定的基础 Valve — StandardEngineValve/HostValve 等 | High |
| Pipeline.java:79 | **addValve(Valve)**: 追加 Valve — 触发 Container.ADD_VALVE_EVENT，用于 MapperListener 感知 | High |
| Pipeline.java:86 | **getValves()**: 返回所有 Valve 集合(含 basic) | High |
| Pipeline.java:106 | **getFirst()**: 返回链中第一个 Valve — 即 T-2 的 `pipeline.getFirst().invoke()` 入口 | High |
| Pipeline.java:114 | **isAsyncSupported()**: 所有 Valve 支持异步? 任一不支持→整个 Pipeline 异步不可用 | High |

### Valve.java (118 行 — 请求处理单元接口)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Valve.java:50-58 | **getNext()/setNext()**: 单向链表 — Valve 持有 next 引用，形成 Chain of Responsibility 的物理结构 | High |
| Valve.java:64-118 | **invoke() 契约**: 检查/修改 Request+Response → 可选自生成 Response(短路) → 否则 `getNext().invoke()` → 检查响应。MUST NOT: 修改已使用的路由属性/已生成 Response 后继续传递/消费 InputStream/在 getNext() 返回后修改 Header/OutputStream | High |
| Valve.java:68 | **backgroundProcess()**: 周期性任务 — Valve 也可以有后台任务(重载/清理) | Medium |
| Valve.java:117 | **isAsyncSupported()**: Valve 级别异步支持声明 | Medium |

### StandardEngineValve.java (76 行 — Engine→Host 路由)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardEngineValve.java:56 | **invoke()**: 从 request 获取 Host(`request.getHost()`) — 未匹配→尝试 defaultHost — 调用 `host.getPipeline().getFirst().invoke()` — 进入下一层 Pipeline | High |

### StandardHostValve.java (396 行 — Host→Context 路由 + 错误处理)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardHostValve.java:80 | **invoke()**: 从 request 获取 Context(`request.getContext()`) — 调用 `context.getPipeline().getFirst().invoke()` — 进入下一层 Pipeline | High |
| StandardHostValve | **错误页面渲染**: Context 返回后检查 response.isError()→设置 Status 页面→生成 HTML 错误信息 | High |

### StandardContextValve.java (92 行 — Context→Wrapper 路由)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardContextValve.java:60 | **invoke()**: 从 request 获取 Wrapper(`request.getWrapper()`) — 拒绝未授权请求(WebSocket 路径) — 调用 `wrapper.getPipeline().getFirst().invoke()` | High |

### StandardWrapperValve.java (338 行 — Wrapper→FilterChain→Servlet)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardWrapperValve.java:86 | **invoke()**: 分配 Servlet 实例 — `ApplicationFilterFactory.createFilterChain()` 创建 Filter 链 — `filterChain.doFilter()` — 释放 Servlet 实例 | High |
| StandardWrapperValve.java:141 | **FilterChain 创建**: `ApplicationFilterFactory.createFilterChain(request, wrapper, servlet)` — 根据 URL pattern 匹配 Filters | High |

### ApplicationFilterChain.java (313 行 — Filter 链执行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ApplicationFilterChain.java:117 | **doFilter()**: 入口 — 调用 `internalDoFilter()` — 如果 request 是 HttpServletRequest 类型 | High |
| ApplicationFilterChain.java:144 | **internalDoFilter()**: 核心逻辑 — pos 按序取 Filter→`filter.doFilter()`→Filter 内部调 `chain.doFilter()` 回到此处→pos++→下一个 Filter→全部执行完→`servlet.service()` | High |
| ApplicationFilterChain.java:239 | **addFilter()**: 追加 Filter 到链 — 在 StandardWrapperValve 创建 FilterChain 时填充 | High |
| ApplicationFilterChain.java:259 | **release()**: 回收 FilterChain — 重置 pos/n，Filters 数组不清理(由 FilterFactory 管理) | High |
| ApplicationFilterChain.java:284 | **setServlet()**: 设置最终执行的 Servlet — Filter 全部执行完后调用 `servlet.service()` | High |

---

## 02 聚合 — P1/P2/P3

### P1 — 全系统共识（≥5 处引用）

| KP | 涉及文件 | 证据 |
|------|:--:|------|
| **Chain of Responsibility (getNext().invoke())** | 7 | Pipeline/Valve + 4 Valve 实现 — T-1 ContainerBase.pipeline + T-2 Adapter.invoke |
| **Pipeline.getFirst().invoke() 入口** | 5 | T-2 Adapter.service + T-2 asyncDispatch + T-2 log + 4 Valves (自调用) |
| **Valve 接口契约 (invoke→getNext→invoke)** | 7 | Valve 接口 + 4 实现 + ApplicationFilterChain(internalDoFilter — 同样模式) |

### P2 — 局部重要（2-4 处引用）

| KP | 涉及文件 | 证据 |
|------|:--:|------|
| **FilterChain + Filter 双链** | 3 | ApplicationFilterChain + StandardWrapperValve + ApplicationFilterFactory |
| **basic Valve 机制** | 4 | Pipeline.setBasic + 4 Container 各配置自己的 basic(Engine→EngineValve) |
| **错误页面渲染 (HostValve)** | 2 | StandardHostValve + ErrorReportValve |

### P3 — 独立知识点

| KP | 文件 |
|------|------|
| **WebSocket 路径拒绝 (ContextValve)** | StandardContextValve — 特定容器逻辑 |
| **Servlet 实例分配/释放 (WrapperValve)** | StandardWrapperValve — Servlet 生命周期管理 |
| **backgroundProcess (Valve)** | Valve 接口 — 周期性任务 |

---

## 03 深度分类 — 🔴🟡🟢 per KP

| KP 群 | 级别 | 判定理由 |
|------|:--:|------|
| **Pipeline + Valve Chain of Responsibility** | 🔴 Deep | T-3 的核心架构决策 — Pipeline 是 Tomcat 请求处理的骨架，双链(Pipeline+FilterChain)是 Tomcat 独有的请求处理模式 |
| **4 Valve invoke 级联** (Engine→Host→Context→Wrapper) | 🔴 Deep | 承载"请求如何从 Engine 路由到 Servlet"的完整路径 — 与 T-1 容器树直接关联，每个容器层一个 Valve |
| **FilterChain + internalDoFilter 嵌套调用** | 🔴 Deep | Filter 是 Servlet 规范的核心抽象 — `filter.doFilter()→chain.doFilter()` 的递归嵌套是 Filter 执行的基础 |
| **basic Valve 机制 vs 附加 Valve** | 🟡 Working | addValve/setBasic 的设计区别 — 概念重要但代码简单 |
| **错误页面 + WebSocket 拒绝** | 🟡 Working | 特定容器逻辑 — 不是 Pipeline 架构的核心 |

---

## 04 聚类 — 教学顺序

**Cluster A: Pipeline + Valve — Chain of Responsibility 的标准实现** (→ 对应 §1)
- Pipeline 接口: basic + 附加 Valves — getFirst()→invoke 链
- Valve 接口: getNext()/setNext() + invoke() + backgroundProcess()
- 与 Netty ChannelPipeline 对比: 同为 CoR — Tomcat 用 basic(终端)Valve，Netty 用 Head↔Tail Sentinel — 两种实现策略

**Cluster B: 4 Valve invoke 级联 — 从 Engine 到 Servlet**

- StandardEngineValve: 选择 Host → `host.getPipeline().getFirst().invoke()`
- StandardHostValve: 选择 Context + 错误页面渲染 → `context.getPipeline().getFirst().invoke()`
- StandardContextValve: 选择 Wrapper + WebSocket 拒绝 → `wrapper.getPipeline().getFirst().invoke()`
- StandardWrapperValve: 分配 Servlet → createFilterChain → `filterChain.doFilter()`
- 级联本质: 每层调用下一层的 Pipeline.getFirst().invoke() — 形成 4 层递归链

**Cluster C: Filter 双链 — ApplicationFilterChain 的嵌套执行**

- `doFilter()` → `internalDoFilter()` — pos 索引顺序取 Filter
- `filter.doFilter(request, response, chain)` — Filter 内部调 `chain.doFilter()` — 回到 internalDoFilter — pos++ — 下一个 Filter
- 递归 vs 循环: 实际上不是递归 — 是 `filter.doFilter()` 中的 `chain.doFilter()` 把控制权交还给 FilterChain — 然后 FilterChain 取下一个 Filter — 这是 "回调式" 迭代
- 最后一个 Filter 执行完 → `servlet.service()` → 逐层返回 (Filter 的 post-processing)
- 规范对应: `FilterChain` (2.3) → `ApplicationFilterChain`

**Cluster D: 与 Netty Pipeline 对比 — 同样是 CoR，两种实现哲学**

- Netty: `ChannelHandlerContext.fireChannelRead()` — 事件驱动 + Head↔Tail Sentinel — 用户 Handler 在中间 — 双向传播(inbound/outbound)
- Tomcat: `Valve.invoke()→getNext().invoke()` — 单向传播 — basic Valve 是终端 — 用户 Filter 在 WrapperValve 的 FilterChain 中
- 核心差异: Netty CoR 在 Channel 级别(TCP 连接) — Tomcat CoR 在 Container 级别(请求处理)
- 为什么 Tomcat 不直接用 Netty 的 Pipeline? Tomcat 诞生于 1999(Netty 2004) — Filter 是 Servlet 2.3(2000) 的标准 — 双链设计是历史产物

> → 引出 T-4 线程模型 — Pipeline 解决了"请求怎么处理" — 但"谁在执行"还没讲。Engine→Host→Context→Wrapper 这 4 个 Valve 的 invoke() 在哪个线程执行？`bind(8080)` 之后 —— Acceptor 线程 accept → Poller 线程读 → Worker 线程处理 → 这 3 个线程角色是怎么协作的？
