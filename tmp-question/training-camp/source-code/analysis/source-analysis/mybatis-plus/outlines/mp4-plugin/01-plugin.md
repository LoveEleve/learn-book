# MP-4 插件体系 — MybatisPlusInterceptor 宿主 + InnerInterceptor 回调链

> 前置: [[MP-3-lambda-wrapper]] (Wrapper 联动) | 复用: [[M-4-plugin]] (Interceptor 契约/pluginAll) | 对照: [[M-4-plugin]] (Plugin 双条件匹配 vs 宿主内分派) | 引出: [[MP-5-pagination]]
> 🔴 Deep | 7 KP | [模式: 拦截器链(宿主)+回调模板+配置驱动装配]
> Pass 2 闭环: q1(5 签名挂载) q2(query 分发与重发) q3(update 短路) q4(StatementHandler 两路) q5(回调契约) q6(配置驱动) q7(M-4 对接)

**读者处境**: `mybatis-plus` 配置里加一个 `PaginationInnerInterceptor` 就自动分页 — 它挂在 M-4 的哪个拦截点上?分页是怎么"改写 SQL"的?为什么有的插件配置用 `@page` 前缀?这篇拆 MP 的插件宿主: 5 个签名挂载点、query 重发机制、InnerInterceptor 回调链与配置驱动装配。

### 1. 挂载 — 5 @Signature 覆盖 2 类处理器

场景: MybatisPlusInterceptor 拦了哪些方法?为什么 ParameterHandler/ResultSetHandler 不拦?
源码路径:
- `MybatisPlusInterceptor.java:41-48` — **5 @Signature**(q1): StatementHandler.prepare/getBoundSql + Executor.update/query(4 参)/query(6 参带 CacheKey)
- `MybatisPlusInterceptor.java:110-115` — plugin(): **只包装 Executor/StatementHandler**(与签名对齐, 其他类型原样返回)
- M-4 白名单 (4 类) 中选 2 类 — 参数/结果映射处理器不拦(无改写需求)
关键设计: 最小拦截面(q1): 5 方法覆盖 SQL 执行的所有"可改写点" — prepare(连接/语句时机)/getBoundSql(动态 SQL)/update/query(核心); plugin 过滤与签名严格对齐(避免无谓代理)。[模式: 声明式签名]
数据流: 4 工厂 pluginAll → MybatisPlusInterceptor.plugin → Executor/StatementHandler? Plugin.wrap : 原样。

### 2. 分发 — intercept 三路 + query 重发机制

场景: 分页插件改写的 SQL 怎么生效?willDoQuery 返回 false 会怎样?
源码路径:
- `MybatisPlusInterceptor.java:56-89` — intercept(q2/q3): Executor 判定(isUpdate=args.length==2 L62); SELECT→逐 InnerInterceptor: **willDoQuery false→返回 Collections.emptyList() 短路**(L75-77)+**beforeQuery(改 boundSql)**(L78)→**executor.createCacheKey+executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql) 重新发起**(L80-81); update→willDoUpdate false→**返回 -1**+beforeUpdate(L82-89)
- `MybatisPlusInterceptor.java:90-104` — StatementHandler 两路(q4): **args null→beforeGetBoundSql**(动态 SQL/Batch/Reuse 场景); 否则 beforePrepare
- `MybatisPlusInterceptor.java:68-73` — boundSql 来源: 4 参→`ms.getBoundSql(parameter)`; 6 参→直接取
关键设计: 重发机制(q2): 拦截器不直接改 Executor 内部, 而是 **beforeQuery 改 BoundSql 后重新调 executor.query 带改写结果** — 一级/二级缓存/插件链自然生效; **willDoQuery=false 是"宿主短路"** — 分页插件的真实用例: count 后 continuePage false(总数为 0 或页码越界且未开 overflow, PaginationInnerInterceptor.java:432-450)→直接空结果不查数据; 插件也可在 willDoQuery 内自行执行 count 查询(page.setTotal, L124-145)再决定是否短路。[模式: 改写+重发]
数据流: executor.query 拦截 → willDoQuery? → beforeQuery(改 boundSql) → createCacheKey → executor.query(重发) → M-2 执行。

### 3. 回调链 — InnerInterceptor 7 方法契约

场景: 实现一个 InnerInterceptor 要写什么?willDoXxx 和 beforeXxx 的分工?
源码路径:
- `InnerInterceptor.java:53-126` — **6 回调+setProperties 全 default**(q5): willDoQuery/beforeQuery/willDoUpdate/beforeUpdate/beforePrepare/beforeGetBoundSql
- willDoXxx 语义: 返回 false → query 短路 emptyList/update 短路 -1; beforeXxx 语义: 改写 SQL/参数
关键设计: 回调模板(q5): 全部 default — 实现者只覆写关心时机; willDo(决策)+before(执行)分离 — 决策点让插件能"拦截"操作而非仅"观察"; **配套 @InterceptorIgnore: 各插件内部自查(InterceptorIgnoreHelper.willIgnoreXxx(ms.id), 如 BlockAttack L57/DataPermission L69)跳过自身** — 宿主不感知, 注解字段按插件类型声明(tenantLine/blockAttack/dataPermission)。[模式: 回调模板+忽略注解]
数据流: intercept → 逐 InnerInterceptor: willDoXxx? → beforeXxx → 重发/继续。

### 4. 配置驱动 — PropertyMapper @分组装配

场景: 配置文件里的 @page 和 page:limit 怎么变成插件实例和属性?
源码路径:
- `MybatisPlusInterceptor.java:138-146` — setProperties(q6): `PropertyMapper.group("@")` 解析 `@page=类全名`+`page:limit=值` → `ClassUtils.newInstance`+`setProperties` 注入+`addInnerInterceptor`
- `MybatisPlusInterceptorTest.java:20-38` — 实证: @page→PaginationInnerInterceptor 实例化+**maxLimit=10+dbType=H2 属性注入**
关键设计: 配置驱动装配(q6): M-4 的 setProperties(NOP 默认)被 MP 重写为"插件工厂" — 别名+class 映射+属性注入全配置化; 插件无需代码装配。[模式: 配置驱动装配]
数据流: properties → PropertyMapper.group → @page 别名→newInstance → page:limit 等属性注入 → addInnerInterceptor。

### 5. 与 M-4 的对接 — 覆写 plugin + 手动分派

场景: MybatisPlusInterceptor 与普通 Interceptor 的实现差异?
源码路径:
- `MybatisPlusInterceptor.java:55-107` — intercept: 按 target 类型+args 长度**手动分派**到 InnerInterceptor 链(q7)
- `MybatisPlusInterceptor.java:109-115` — plugin: 覆写 M-4 默认 Plugin.wrap — **只包 Executor/StatementHandler**
关键设计: 宿主模式(q7): MybatisPlusInterceptor 是 M-4 契约的"总拦截器", 内部再分派给 InnerInterceptor 子链 — **M-4 的插件级 vs MP 的插件内级双层**; 与 M-4 Plugin 的双条件匹配不同, 这里签名 5 方法内按类型再分派。[模式: 宿主+子链]
数据流: M-4 pluginAll(MybatisPlusInterceptor) → 代理 → 方法命中 5 签名 → intercept → InnerInterceptor 链。

### 负面空间 — 插件宿主刻意不做的事

- **不拦 Parameter/ResultSet 处理器**: 无 SQL 改写需求(参数/结果由 M-5 语义处理)
- **不做插件间依赖管理**: InnerInterceptor 顺序=addInnerInterceptor 顺序, 无优先级注解(与 M-4 注册序一致)
- **不做异常重试**: willDoXxx false 直接短路返回, 无重试语义

→ 引出: PaginationInnerInterceptor 是 willDoQuery 判定+beforeQuery 改写的最典型 InnerInterceptor — 下一篇拆物理分页 SQL 改写与方言 → [[MP-5-pagination]]
