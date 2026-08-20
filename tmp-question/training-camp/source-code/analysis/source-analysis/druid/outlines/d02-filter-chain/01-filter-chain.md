# D-2 Filter 拦截链 — 递归链 + 代理对象模型 + 对象池复用

> 前置: [[D-1-core-architecture]] (链尾池操作) | 复用: [[s24-aop-proxy]] (配置式拦截对照) | 引出: [[D-7-validation]]
> 🔴 Deep | 9 KP | [模式: 责任链 + 模板方法 + 对象池]
> Pass 2 闭环: q1(链所有权协议) q2(短路三态) q4(双层代理) q5(模板 vs 覆写) q6(Error 三路 catch) q7(cloneChain 预留) q8(链尾包装规则)

**读者处境**: `filters: stat,wall` 一行配置, 一次 `stmt.execute("select...")` 要穿过几层代码?StatFilter 和 WallFilter 明明都是 Filter, 为什么一个用钩子一个直接覆写?Druid 官网说"Filter 链是 Druid 的灵魂" — 这篇拆开链的骨架、递归语义和复用设计。

### 1. 链结构 — 五层类族各司其职

场景: 一个拦截体系为什么需要 5 个类?各自的边界在哪?

源码路径:
- `Filter.java:35` — **契约**: `public interface Filter extends Wrapper`(L35) — 全量 JDBC 操作拦截方法签名(1378 行)
- `FilterAdapter.java` — **空实现**: 全部方法纯放行(`return chain.xxx`) — 用户继承起点, 只覆写关心的
- `FilterEventAdapter.java:30,178` — **模板层**: `connection_connect`(L30): connectBefore→super→connectAfter; `statement_execute`(L178) 同构
- `FilterChain.java` — **链契约**: 链调用方法集(1192 行), 与 Filter 接口对称
- `FilterChainImpl.java:37,41` — **实现**: `protected int pos`(L37)+`filterSize`(L41) — 链进度指针

关键设计: **Why 五层？** 接口(契约)→Adapter(免实现)→EventAdapter(时机模板)→Chain(链调用契约)→ChainImpl(递归执行) — 每层解决一类问题: Adapter 解决"实现负担", EventAdapter 解决"时机获取", Chain 接口让 Filter 与实现解耦。[模式: 责任链]

数据流: `filters: stat,wall` → FilterManager 加载(§4) → filters 列表 → 一次操作从 ChainImpl.pos=0 开始逐 Filter 下推。

### 2. 递归与短路 — 责任链的三态语义

场景: 每个 Filter 在链上能干什么?怎么"拦住"一个操作?

源码路径:
- `FilterChainImpl.java:469` — **下推**: `nextFilter()`(L469): `getFilters().get(pos++)` — 每调一次 pos 前进
- `FilterChainImpl.java:3005,3010` — **链尾·裸执行**: `statement_execute`(L3005): pos<size → `nextFilter().statement_execute(...)`(L3007); 否则 `statement.getRawObject().execute(sql)`(L3010)
- `FilterChainImpl.java:246,260` — **链尾·对象包装**: `connection_createStatement`(L246): 链尾 `rawObject.createStatement()`(L250)→`new StatementProxyImpl(...)`(L259-260); `statement_getResultSet`(L3024) 同理包 ResultSetProxyImpl — **返回 JDBC 可操作对象必须包 Proxy, 标量/元数据 rawObject 直通**(闭环 q8)
- `FilterChainImpl.java:5067,5074` — **链尾·池操作**: `dataSource_connect`(L5067): 链尾 `dataSource.getConnectionDirect(maxWaitMillis)`(L5074, D-1)
- `WallFilter.java:485` — **拦截**: `sql = check(sql)`(L485) 违规抛异常 — **不调 chain 即拦截**(闭环 q2)
- `WallFilter.java:489` — **放行后改写**: 通过 check 的 sql 可能被改写后下推

关键设计: **Why 递归而非 for 循环？** (闭环 q2): 责任链三态 — 空实现=纯放行(Adapter), 改参/改结果=过滤(WallFilter check 改写 SQL), 抛异常=拦截。递归调用栈天然支持"执行前后都有钩子", for 循环做不到两侧时机。[模式: 责任链递归 + 短路]

数据流: stmt.execute → chain.statement_execute(3005) → stat(模板, 只观察) → wall(485, 裁决) → 违规: 抛异常返回; 放行: rawObject.execute(3010) → 逐层返回。

### 3. 对象模型与复用 — 双层代理 + 所有权协议

场景: 链操作的对象是什么?per-connection 缓存链会不会并发打架?

源码路径:
- `DruidAbstractDataSource.java:1687,1689,1694` — **双层代理**: `createPhysicalConnection`(L1687): 无 filter → `new DruidStatementConnection(rawConn, stmt)`(L1692, 裸包装); 有 filter → `chain.connection_connect`(L1695) → 链尾 `new ConnectionProxyImpl(...)`(FilterChainImpl:140) — **物理连接被 ConnectionProxyImpl 包一层作为链载体**
- `DruidPooledConnection.java` — **池侧**: 用户持有的是 pool/ 的池连接(借还/事务语义), 内部转发到 ConnectionProxyImpl/DruidStatementConnection(闭环 q4)
- `DruidConnectionHolder.java:223,234` — **所有权协议**: `createChain()`(L223): 取出字段置 null(L224-229, 拿走); `recycleFilterChain()`(L234): reset(pos=0)+回存(L235-236) — **一次只被一个线程持有**
- `DruidPooledConnection.java:252,316` — **异步新建**: 跨线程关闭走 `syncClose`(L257): `new FilterChainImpl(dataSource)`(L316) — **不取缓存链, 宁可分配不共享**(闭环 q1)

关键设计: **Why 两层代理？** (闭环 q4): ConnectionProxyImpl 承载链语义(Filter 的统计/检查挂载点, 如 StatementProxyImpl.getSqlStat L514), DruidPooledConnection 承载池语义(借还/复位) — 职责分离。**Why 所有权转移而非锁？** (闭环 q1): 连接是单线程语义(ownerThread), 借出置 null 保证同刻只有一人持链; 异步场景新建绕开共享。[模式: 对象池 + 所有权转移]

数据流: getConnection(D-1) → holder.createChain(223, 字段置 null) → chain.dataSource_connect(5067) → 链尾 getConnectionDirect → recycleFilterChain(234, reset+回存) → 业务操作取链 → ... 异步归还: syncClose(257) → new FilterChainImpl(316) 不共享。

### 4. 扩展 — 加载机制与两种实现风格

场景: Filter 从哪来?写自定义 Filter 有几种写法?哪种能拦截?

源码路径:
- `FilterManager.java:36,38,42,53,99` — **加载**: 静态块读 `druid.filters.*` 建 aliasMap(L36/L38/L42, 别名→类名); `getFilter(alias)`(L53); `loadFilter(filters, name)`(L99)
- `AutoLoad.java` — **SPI 标记**: `@AutoLoad` 注解(RUNTIME/TYPE) 标记者自动装入
- `FilterEventAdapter.java:178` — **风格 B 模板**: `statement_execute`(L178): Before 钩子(L179)→super 放行(L182)→After 钩子(L184), 三路 catch 兜 Error 走 ErrorAfter(闭环 q6) — **覆写钩子=观察者**(StatFilter L378/383, 只记账无拦截力)
- `WallFilter.java:480` — **风格 A 覆写**: 覆写方法本体(L480), 自己决定是否调 chain(L489) — **决策者**(可拦截可改写)

关键设计: **Why 两种风格并存？** (闭环 q5): 同一接口方法, 覆写层级决定能力 — 钩子层=免费获得前后时机但无拦截能力, 方法本体层=完全控制。StatFilter 要时机(统计), WallFilter 要裁决(防火墙) — 能力与需求匹配, 这是链体系设计精妙处。[模式: 模板方法 + SPI]

数据流: 配置别名 → FilterManager 加载(36/99) → init 时 filter.init(D-1) → 执行时: 风格 A 拦截器(前置裁决) + 风格 B 监控器(后置记账) 按序穿过。

→ 引出 D-7: 连接验证 — 池核心的验证策略 (testOnBorrow/testWhileIdle/testOnReturn 三时机, 链外直连)。
