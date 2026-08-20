# MP-4 插件体系 — MybatisPlusInterceptor 宿主 + InnerInterceptor 回调链

> 项目: MyBatis-Plus | 🔴 Deep / 1 篇 | MybatisPlusInterceptor(155)+InnerInterceptor(126)+PropertyMapper
> 基线: MP-PLAN MP-4 — 前置: **M-4 (Interceptor 契约/4 工厂 pluginAll, 已交付) + M-2 (Executor 语义)** — 展开 5 @Signature→回调分发→InnerInterceptor 链→配置驱动

---

## §0.8

- 🔴 Deep，1篇 — 挂载(**@Intercepts 5 @Signature L41-48: StatementHandler.prepare/getBoundSql + Executor.update/query×2[6 参带 CacheKey 版] — 与 M-4 白名单内 2 类处理器**; plugin L110-115[只包装 Executor/StatementHandler, 与签名对齐]) → 分发(**intercept L56-107: Executor→query/update 两路[isUpdate=args.length==2 L62; SELECT 判定 L64; boundSql 来源[4 参 ms.getBoundSql / 6 参直接取 L68-73]; **willDoQuery false→返回空列表短路 L75-77; beforeQuery→executor.createCacheKey+executor.query 重新发起 L80-81[带改写后 boundSql]**]; StatementHandler→args null[getBoundSql L94-97]/prepare[beforePrepare L99-103]; finally invocation.proceed L106**) → 回调链(**InnerInterceptor L53-126: 6 回调+setProperties 全 default[willDoQuery/beforeQuery/willDoUpdate/beforeUpdate/beforePrepare/beforeGetBoundSql; willDoXxx 语义[false→emptyList/-1]]**) → 配置驱动(**setProperties L138-146: PropertyMapper.group("@")→别名+class 映射→ClassUtils.newInstance→setProperties 注入→addInnerInterceptor; 测试: @page→PaginationInnerInterceptor+page:limit/dbType 属性注入**)
- 设计模式: [模式: 拦截器链(宿主)+回调模板+配置驱动装配]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MybatisPlusInterceptor.java:41-48 | 签名 | **5 @Signature**: StatementHandler.prepare/getBoundSql + Executor.update/query×2 — 覆盖 2 类处理器 5 方法 | High |
| MybatisPlusInterceptor.java:56-107 | 分发 | Executor query/update 两路(isUpdate=args.length==2); willDoQuery false→emptyList; beforeQuery→createCacheKey+重发 query(带改写 boundSql) | High |
| MybatisPlusInterceptor.java:90-104 | StatementHandler | args null→beforeGetBoundSql; 否则 beforePrepare — 注解/XML 静态 SQL 走 prepare, 动态 SQL 走 getBoundSql | High |
| MybatisPlusInterceptor.java:110-115 | plugin | 只包装 Executor/StatementHandler(与签名对齐, 其他原样) | High |
| MybatisPlusInterceptor.java:138-146 | 配置 | PropertyMapper.group("@")→newInstance+setProperties→addInnerInterceptor — 配置驱动装配 | High |
| InnerInterceptor.java:53-126 | 回调 | 6 回调+setProperties 全 default; willDoXxx 短路语义(false→空列表/-1) | High |
| MybatisPlusInterceptorTest.java:20-38 | 测试 | @page→PaginationInnerInterceptor 实例化+maxLimit/dbType 属性注入 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 插件宿主是单机制 — 1篇 (~60行) 按"签名挂载→intercept 分发→回调链语义→配置驱动→与 M-4 对接"展开; M-4 Interceptor 契约引用(已交付)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 5 @Signature 挂载 (2 类处理器 5 方法) | 🔴 | **为什么🔴**: 拦截面 |
| P1-2 | intercept 分发 (query/update/StatementHandler 三路) | 🔴 | **为什么🔴**: 核心逻辑 |
| P1-3 | willDoQuery 短路 + 重发 query (boundSql 改写生效点) | 🔴 | **为什么🔴**: 分页等改写的机制 |
| P1-4 | InnerInterceptor 7 方法回调链 | 🔴 | **为什么🔴**: 扩展契约 |
| P2-1 | 配置驱动装配 (PropertyMapper @分组) | 🟡 | **为什么🟡**: 装配面 |
| P2-2 | plugin 过滤 (只包 Executor/StatementHandler) | 🟡 | **为什么🟡**: 性能 |
| P3-1 | 与 M-4 Interceptor/pluginAll 对接 (引用) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **签名与分发** | 🔴 | 宿主核心 |
| B | **回调链** | 🔴 | 扩展契约 |
| C | **装配与过滤** | 🟡 | 配置面 |
| D | **内核对接** | 🟢 | 衔接 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 5 签名挂载 | @Intercepts 声明 StatementHandler.prepare/getBoundSql + Executor.update/query(4 参)/query(6 参带 CacheKey) — **M-4 白名单 4 类中只拦 2 类 5 方法**(Parameter/ResultSet 处理器不拦), plugin() 同步只包装这两类(L110-115) | MybatisPlusInterceptor.java:41-48,110-115 |
| q2 | query 分发与重发 | Executor.query 拦截: SELECT 判定(L64)→逐 InnerInterceptor willDoQuery(**false→返回空列表短路** L75-77)+beforeQuery(**改 boundSql** L78)→**executor.createCacheKey+executor.query 重新发起**(L80-81); **willDoQuery 真实用例: 分页插件在 willDoQuery 内自查 page+执行 count 查询(page.setTotal L124-145), continuePage false(总数 0/页码越界未开 overflow, L432-450)时短路返回空** | MybatisPlusInterceptor.java:64-81; PaginationInnerInterceptor.java:116-145,432-450 |
| q3 | update 短路 | willDoUpdate false→**返回 -1**(影响行数语义); beforeUpdate 改写 | MybatisPlusInterceptor.java:82-89 |
| q4 | StatementHandler 两路 | args null→beforeGetBoundSql(动态 SQL 场景, Batch/Reuse 才会调用); args 非 null→beforePrepare(prepare 时机改连接/SQL) | MybatisPlusInterceptor.java:90-104 |
| q5 | 回调契约 | InnerInterceptor **6 回调+setProperties 全 default** — 实现者只覆写关心时机; willDoXxx 是"是否执行"决策点 | InnerInterceptor.java:53-126 |
| q6 | 配置驱动 | setProperties: PropertyMapper.group("@") 解析 `@page=类全名`+`page:limit=值` → ClassUtils.newInstance+属性注入+addInnerInterceptor — **纯配置装配插件**(测试: @page→PaginationInnerInterceptor+maxLimit=10+dbType=H2) | MybatisPlusInterceptor.java:138-146; 测试 L20-38 |
| q7 | M-4 对接 | MybatisPlusInterceptor implements Interceptor(M-4 契约): plugin 默认 Plugin.wrap 被覆写(只包 2 类); intercept 手动分发 InnerInterceptor 链(非 M-4 的 Plugin 双条件匹配, 而是签名 5 方法内再分派) | MybatisPlusInterceptor.java:55-107 |

→ 引出 MP-5: 分页插件 — PaginationInnerInterceptor 是 willDoQuery 判定+beforeQuery 改写的最典型 InnerInterceptor。
