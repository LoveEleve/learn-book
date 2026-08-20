# 闭环笔记 GW-2-q2 — 链执行: 反应式责任链 (递归 + Mono.defer)

假设: DefaultGatewayFilterChain 是不可变索引递归链 — 每次 filter 创建 index+1 的新链, Mono.defer 保证惰性执行。

验证过程:
- **DefaultGatewayFilterChain** (FilteringWebHandler.java:132-168): 字段 `index` + `filters` (L133-134); 构造 (L137-139, index=0) / 子链构造 (L141-144, index+1)
- **filter** (L153-166): `Mono.defer(() -> { if (this.index < filters.size()) { GatewayFilter filter = filters.get(this.index); DefaultGatewayFilterChain chain = new DefaultGatewayFilterChain(this, this.index + 1); return filter.filter(exchange, chain); } else { return Mono.empty(); // complete } })` — **索引推进 + 递归 + 完成条件 (index == size)**
- **Mono.defer 惰性**: 每次订阅才取下一个过滤器 — 反应式链天然支持异步过滤器 (返回 Mono 等待)
- **执行模型**: 请求 → 过滤器 0 (order 最小) → 链 1 → ... → 链 N (Mono.empty = 完成) → 转发 (NettyRoutingFilter 等终止过滤器)
- 与 GW-1 q2 的关系: route.getFilters 是已实例化 GatewayFilter 列表 (工厂 apply 产物)

代码类型: Algorithmic (递归链)

结论: 链执行 = **不可变索引递归**: 每级创建新链 (index+1), 过滤器决定何时调用链 (可中断 — 短路/错误); Mono.defer 让链惰性且反应式 (过滤器可返回 Mono 异步等待)。**被放弃的方案: 可变迭代器链** — 不可变链让过滤器安全异步 (无并发修改); 递归模型天然表达"过滤器可能不调用下游"。 [跨域: GW-1 装配产物消费; WebFlux 反应式] [模式: 责任链] (FilteringWebHandler.java:132-168)
