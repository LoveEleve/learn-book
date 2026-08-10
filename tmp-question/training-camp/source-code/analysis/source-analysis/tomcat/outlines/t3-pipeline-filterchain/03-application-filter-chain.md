# T-3 §3 ApplicationFilterChain — Filter 链的嵌套执行

> 依赖 §2 | 🔴 Deep | 3 KP | [模式: Chain of Responsibility — Filter 变体]

**读者处境**: 知道 Valve 链用 `getNext().invoke()` 传递请求 — WrapperValve 把请求交给了 `filterChain.doFilter()` — Filter 链内部是怎么执行的？Filter 的 `chain.doFilter()` 和 Valve 的 `getNext().invoke()` 有什么本质区别？

### 1. internalDoFilter — pos 索引 + 回调式迭代

场景: web.xml 配置了 3 个 Filter: AuthFilter → LoggingFilter → EncodingFilter — 最后执行 Servlet。Filter 链的执行不是简单的 for 循环 — Filter 可以在 `chain.doFilter()` **之前**做预处理 — **之后**做后处理 — 这才是 Filter 的强大之处。

源码路径:
- `ApplicationFilterChain.java:117` — **doFilter()**: 入口 — 如果 request 是 HttpServletRequest→调 `internalDoFilter(req, res)` — 否则直接调
- `ApplicationFilterChain.java:144` — **internalDoFilter()**: `private void internalDoFilter(request, response)` — 拿 `filters[pos++]` → `filter.doFilter(request, response, this)` — Filter 内部调用 `chain.doFilter()` — 回到此方法(pos 已 +1) — 拿下一个 Filter — 全部执行完 → `servlet.service(request, response)`
- `ApplicationFilterChain.java:239` — **addFilter()**: 追加 FilterConfig 到 filters 数组
- `ApplicationFilterChain.java:284` — **setServlet()**: 设置最终的 Servlet 实例
- `ApplicationFilterChain.java:259` — **release()**: reset pos/n/request/response — filters 数组不清(由 FilterFactory 重用)

关键设计: **这不是递归 — 是回调式迭代**。`filter.doFilter(request, response, chain)` 中的 `chain` 就是 ApplicationFilterChain 自身 — Filter 调 `chain.doFilter()` 等价于调用 `ApplicationFilterChain.doFilter()` — 又回到 internalDoFilter — pos 已经从刚才的位置+1 — 取下一个 Filter。这就是"嵌套"的感觉但实际栈上只有 Filter.doFilter→chain.doFilter→internalDoFilter→下一个 Filter.doFilter — 是**尾调用式**的 — 不会造成真正的递归深度溢出。 [模式: Chain of Responsibility — Filter 变体: 回调而非递归]

→ 实现规范: `FilterChain` (2.3) → `ApplicationFilterChain` — 完美对应。Servlet 规范定义的 `Filter.doFilter(ServletRequest, ServletResponse, FilterChain)` 三个参数中 — 第三个 FilterChain 传给 Filter 让它继续调用 — 这是 "回调式 CoR" 的标准实现。

数据流: WrapperValve 创建 filterChain→`filterChain.doFilter(req, res)`→`internalDoFilter(req, res)`→`pos=0, n=3`→`filters[0] = AuthFilter`→`authFilter.doFilter(req, res, this)`→AuthFilter: 检查 token→`chain.doFilter(req, res)`→回到 ApplicationFilterChain→`internalDoFilter(req, res)`→`pos=1`→`filters[1] = LoggingFilter`→`loggingFilter.doFilter(req, res, this)`→LoggingFilter: 记录 startTime→`chain.doFilter(req, res)`→回到 ApplicationFilterChain→`internalDoFilter(req, res)`→`pos=2`→`filters[2] = EncodingFilter`→`encodingFilter.doFilter(req, res, this)`→EncodingFilter: `request.setCharacterEncoding("UTF-8")`→`chain.doFilter(req, res)`→回到 ApplicationFilterChain→`internalDoFilter(req, res)`→`pos=3 >= n`→`servlet.service(req, res)`→返回→EncodingFilter 后处理: 检查 response encoding→返回→LoggingFilter 后处理: 记录耗时→返回→AuthFilter 后处理: 无→返回→ApplicationFilterChain.doFilter() 返回→WrapperValve 收到。

### 2. Valve 链 vs Filter 链 — 两套 CoR 的差异

场景: 新人疑惑 "Tomcat 为什么有两套 Chain of Responsibility？Valve 链和 Filter 链难道不能合并？" 答案: 两套 CoR 服务于不同抽象层。

关键设计: 

| 维度 | Valve 链 | Filter 链 |
|------|------|------|
| 抽象层 | Tomcat 容器层级 (Engine/Host/Context/Wrapper) | Servlet 规范层 (Filter) |
| 谁定义 | Tomcat 源码 (Valve 接口) | Servlet 规范 (Filter 接口) |
| 配置方式 | server.xml `<Valve>` | web.xml `<filter>` 或 `@WebFilter` |
| 链结构 | 单向链表 getNext()/setNext() | 数组 filters[] + pos 索引 |
| 传递方式 | `getNext().invoke()` — 显式 | `chain.doFilter()` — 回调 |
| 终端 | basic Valve(StandardWrapperValve) | servlet.service() |
| 生命周期 | 容器启动时创建，停止时销毁 | 请求级别: FilterChain 每次请求新建 |

架构意图: **两链是 Tomcat 的 defense-in-depth 设计** — Valve 链处理容器级关注(路由/安全/集群)，Filter 链处理应用级关注(编码/认证/日志)。运维在 server.xml 加 Valve — 开发者在 web.xml 加 Filter — 两个角色不需要相互了解。 [模式: Layered Architecture — Valve 链在上层, Filter 链在下层]

### 3. ApplicationFilterFactory — URL Pattern 匹配

场景: web.xml 中定义 `<filter-mapping><url-pattern>/api/*</url-pattern></filter-mapping>` — Filter 只匹配 /api/ 开头的请求 — 不匹配 /static/ 请求。FilterFactory 根据 URL pattern 动态构建 Filter 链 — 不是所有 Filter 都对每个请求生效。

源码路径: `ApplicationFilterFactory.createFilterChain(request, wrapper, servlet)` — 检查 context 的所有 FilterMap → 匹配 URL pattern 或 Servlet name → 把匹配的 FilterConfig 加入 FilterChain → 返回。

关键设计: **Why Filter Chain 每次请求新建？** 因为不同 URL 匹配不同的 Filter — 预构建固定链不现实。但 `filters[]` 数组在同一 FilterChain 对象上重用(WTF!) — Tomcat 10 的优化: FilterChain 对象由 `ApplicationFilterFactory` 池化 — `filters[n++] = filterConfig` 在这种设计中是**可变的** — release() 重置 n 但不清数组。

→ 引出 §4 与 Netty Pipeline 对比 — 理解了两套 CoR 之后 — 把 Tomcat 的 Pipeline+FilterChain 与 Netty 的 ChannelPipeline 做并排对比 — 同样是 CoR — 两种实现哲学迥异(容器级 vs 连接级 / 单向 vs 双向 / 历史 vs 现代)。
