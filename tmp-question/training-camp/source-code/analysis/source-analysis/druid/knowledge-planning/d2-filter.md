# D-2 Filter 拦截链 — 配置式 AOP: 递归链 + 代理对象模型 + 对象池复用

> 项目: Druid (JDBC 连接池) | 🔴 Deep / 1 篇 | Filter(1378)+FilterAdapter(2891)+FilterEventAdapter(541)+FilterChain(1192)+FilterChainImpl(5287)+proxy/jdbc(36 文件)
> 基线: DRUID-PLAN D-2 (拦截链) — 前置: **D-1(链尾执行池操作, 已分析) + s24-s28 spring-aop(对照)** — 展开递归链机制; proxy/jdbc 已定量预检(两处 ≥500 行类已读)并入本域

---

## §0.8

- 🔴 Deep，1篇 — 链结构(**Filter 接口[L35 extends Wrapper]** → FilterAdapter 空实现 → FilterEventAdapter 模板方法层 → FilterChain 契约接口 → FilterChainImpl 递归实现) → 递归机制(**pos[L37]+filterSize[L41] 控制进度, nextFilter()[L469] 逐 Filter 下推, 链尾执行 raw JDBC[statement_execute L3005: rawObject.execute]/池操作[dataSource_connect L5067→getConnectionDirect]/对象包装[connection_connect L123→ConnectionProxyImpl]) → 对象模型(proxy/jdbc 36 文件: ConnectionProxyImpl[createChain L75/recycleFilterChain L86]/StatementProxyImpl[sqlStat L514/executeType L551]/WrapperProxyImpl.getId 等, 链的载体) → 生命周期(FilterChainImpl **对象池复用**: DruidConnectionHolder.createChain L223/recycleFilterChain L234, per-connection 缓存; 借/还双路径: 有 filter 走链[getConnection L1345/DruidPooledConnection.close L274], 无 filter 直连) → 扩展(FilterManager aliasMap 静态加载[L35-45]+loadFilter[L99]+@AutoLoad 注解 SPI; 内置 5 类 Filter: Stat/Wall/Config/Encoding/Logging)
- 设计模式: [模式: 责任链]—pos+nextFilter 递归; [模式: 模板方法]—FilterEventAdapter Before/After 钩子; [模式: 对象池]—chain 复用

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Filter.java:35 | 契约 | **public interface Filter extends Wrapper** — 全量 JDBC 操作拦截方法签名 (1378 行) | High |
| FilterAdapter.java | 空实现 | **implements Filter 全方法空实现** — 用户继承它只覆写关心的方法 | High |
| FilterEventAdapter.java:178,30 | 模板层 | **statement_execute(chain,stmt,sql)(L178)**: executeBefore→super 执行→executeAfter 模板; connection_connect(L30-35): connectBefore→super→connectAfter | High |
| FilterChainImpl.java:37,41,469 | 递归机制 | **pos(L37)+filterSize(L41) 链指针; nextFilter()(L469): pos<size ? filters[pos++] 下推** | High |
| FilterChainImpl.java:3005,5067,123 | 链尾 | **statement_execute(L3005) 链尾=statement.getRawObject().execute(sql); dataSource_connect(L5067) 链尾=dataSource.getConnectionDirect(L5074); connection_connect(L123) 链尾=driver.connect→new ConnectionProxyImpl(L135)** | High |
| FilterChainImpl.java:62,67 | 复用支持 | **reset()(L62) pos=0; cloneChain()(L67) new FilterChainImpl(dataSource, pos)** | High |
| DruidConnectionHolder.java:223,234 | 对象池 | **createChain()(L223): filterChain 字段缓存, 无则 new; recycleFilterChain()(L234): chain.reset()+回存字段** — 每个连接持有一个链实例复用 | High |
| DruidPooledConnection.java:274,281 | 归还双路径 | **close(): filtersSize>0 → createChain().dataSource_recycle(L274); 否则 recycle()(L281)** | High |
| ConnectionProxyImpl.java:75,86 | 载体 | **createChain/recycleFilterChain(L75/86)** — 连接代理也持链 | High |
| FilterManager.java:36,53,99 | SPI | **aliasMap 静态加载 druid.filters.* 配置(L36/L38); getFilter(alias)(L53); loadFilter(filters,name)(L99); @AutoLoad 注解标记自动加载** | High |
| WallFilter.java:480 | 风格 A | **直接 override statement_execute(chain,stmt,sql)(L480)** — execute 前 check(sql)(L322) | High |
| StatFilter.java:378,383 | 风格 B | **override 模板钩子 statementExecuteBefore/After(L378/383)** — 埋数后置 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: Filter 链是单一机制(责任链) — 1篇 (~60行) 按"链结构→递归与链尾→对象模型与复用→扩展与风格"展开; 链尾的池操作引用 D-1(已分析), 拦截器实例(Stat/Wall)在 D-3/D-4 展开, 本域只讲链本身。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 五层类族 (接口→空实现→模板→契约→递归实现) | 🔴 | **为什么🔴**: 链的骨架 |
| P1-2 | 递归下推机制 (pos/nextFilter/链尾) | 🔴 | **为什么🔴**: 链怎么执行 |
| P1-3 | 链尾三类出口 (raw/池操作/包装) | 🔴 | **为什么🔴**: 链尾做什么 |
| P1-4 | proxy/jdbc 代理对象模型 | 🔴 | **为什么🔴**: 链的载体 |
| P1-5 | chain 对象池复用 (createChain/recycleFilterChain) | 🔴 | **为什么🔴**: 性能关键设计 |
| P2-1 | FilterEventAdapter 模板方法层 | 🟡 | **为什么🟡**: 钩子在哪定义 |
| P2-2 | 两种 Filter 实现风格 (override vs 模板钩子) | 🟡 | **为什么🟡**: 扩展两种写法 |
| P2-3 | FilterManager/AutoLoad SPI 加载 | 🟡 | **为什么🟡**: Filter 怎么装进来 |
| P3-1 | 与 spring-aop CGLIB 配置式拦截对照 | 🟢 | **为什么🟢**: 两种拦截哲学 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **链结构五层类族** | 🔴 | 骨架 |
| B | **递归下推+链尾出口** | 🔴 | 执行核心 |
| C | **对象模型+对象池复用** | 🔴 | 载体与性能 |
| D | **扩展与风格** | 🟡 | 使用侧 |

> **Cluster A (§1)**: Filter 接口→Adapter→EventAdapter→Chain 契约→ChainImpl
> **Cluster B (§2)**: pos/nextFilter 递归 + 三类链尾
> **Cluster C (§3)**: proxy 对象模型 + per-connection chain 复用 + 借/还双路径
> **Cluster D (§4)**: FilterManager/SPI + StatFilter vs WallFilter 两种风格

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 链所有权协议 | 线程安全靠"一次一人持有": createChain 取字段置 null, 异步关闭 new 链不共享 | DruidConnectionHolder.java:223-237, DruidPooledConnection.java:252-257,316 |
| q2 | 责任链三态 | 空实现=放行 / 改参改果=过滤 / 抛异常=拦截 — "调不调 chain"是唯一分叉点 | WallFilter.java:480-502, FilterEventAdapter.java:178-198 |
| q4 | 双层代理 | ConnectionProxy=链载体(统计挂载点), DruidPooledConnection=池语义; 无 filter 退化为裸包装 | DruidAbstractDataSource.java:1687-1702, FilterChainImpl.java:123-140 |
| q5 | 模板 vs 覆写 | 覆写层级决定能力: EventAdapter 钩子=观察者, 方法本体=决策者 — 能力与需求匹配 | FilterEventAdapter.java:178-198, StatFilter.java:378-385, WallFilter.java:480-502 |
| q6 | 模板三路 catch | **SQLException/RuntimeException/Error 全捕获→统一 statement_executeErrorAfter→重抛** — 保证统计钩子对所有异常形态执行(Error 也不漏账), 重抛不吞 | FilterEventAdapter.java:178-197 |
| q7 | cloneChain 预留 API | 全仓库无内部调用点 — 是给 Filter 实现者的扩展 API(new FilterChainImpl(dataSource, pos) 从当前 pos 续链), 框架自身不用 | FilterChain.java:42, FilterChainImpl.java:67-69 |
| q8 | 链尾包装规则 | **返回 JDBC 可操作对象(Statement/ResultSet/Connection)→包 ProxyImpl(后续还能过链); 标量/元数据→rawObject 直通** | FilterChainImpl.java:246-260, 3024-3032 |

→ 引出 D-7: 连接验证 — 池核心的验证策略 (三时机, 链外直连; 执行序桥接 D-5 的 shrink keepAlive)
