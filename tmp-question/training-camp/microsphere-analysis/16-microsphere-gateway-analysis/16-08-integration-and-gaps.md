# 16-08：集成关系、适用场景与缺口总表

> **核心命题**：16 在整个 microsphere 生态里到底站在哪？本文收口三件事：与 15/17/18 的集成关系（哪些通、哪些断、哪些是误解）、we:// 方案的适用边界与部署注意、以及**全局缺口总表**（统一 G 编号，映射散落在 16-03/16-05/16-07 的 P/风险/H 编号）。

---

## 一、与 15/17/18 的集成关系

### 1.1 与 15-configuration（配置中心）

| 链路 | 状态 | 说明 |
|------|------|------|
| 配置中心变更 → 网关路由刷新 | **通**（双通道） | `EnvironmentChangeEvent`（microsphere 补发，16-05 第四节）+ `RefreshScopeRefreshedEvent`（官方 reset，未被拦截）都通向 `RefreshRoutesEvent` |
| 15 的 `@PropertySourceExtension`/`PropertySourcesChangedEvent` → 网关 | **不通** | 16 不监听 15 的自有事件；两套配置体系独立（16-06 第六节）——**配置源归 15、绑定归 04、事件归 spring-cloud-context 标准** |
| excludes 热更新 | **通** | 配置刷新 → rebind（`onFinish` 重注入 WebEndpointConfig，16-06）→ `EnvironmentChangeEvent` → 端点缓存重建 |

### 1.2 与 17-multiactive（多活）—— Zone 真相

**结论（16-01 已纠正 HANDOVER 假设，此处给出完整机制）**：

- 16 **没有**网关层 Zone 实现（全历史仅 2 条 TODO 注释）。
- 网关的 Zone 优先由 **17 的全局 LoadBalancer 配置**提供：`CustomizedLoadBalancerAutoConfiguration`（`@LoadBalancerClients(defaultConfiguration=...)`）对**包括网关在内的所有 LoadBalancer 客户端**生效，条件是：
  1. 网关应用引入 17 的 multiactive-spring-cloud
  2. `microsphere.spring.cloud.loadbalancer.customized=true`
  3. `spring.cloud.loadbalancer.configurations=optimized-zone-preference`（`OptimizedZoneConfigurationCondition` 门控）
  4. `ZoneContext.preferenceEnabled=true`（默认 false，需动态开启——且开启链路在 17 侧断链，见 HANDOVER）
- **生效路径**：WebFlux 版 `choose()` 走 `LoadBalancerClientFactory`（16-03）→ 17 的 `ZonePreferenceServiceInstanceListSupplier` 在实例列表层过滤 ✓；MVC 版转发走官方 `lb()`（16-04）同样生效 ✓。
- **16 的 TODO 是什么**：`buildRequestMappingContexts` 里"样本实例选择"也想用 Zone 过滤——只影响"从哪台实例读端点元数据"，不影响转发目标，属次要优化；且原始设计的 `ServiceInstancePredicate` 插件点已被删除（16-02 L3）。

### 1.3 与 18-dynamic（多子上下文）

| 维度 | 结论 |
|------|------|
| 事件作用域 | 子上下文发布的事件父上下文监听器收不到（`this.context == event.getApplicationContext()` 守卫，16-05 第九节）——与 18-04 结论一致 |
| MVC static 缓存 | 网关跑在子上下文结构里时，`WebEndpointMappingHandlerSupplier` 的 static map 按 routeId 串上下文（16-04 问题①） |
| 端点元数据注册 | 多子上下文共享 ServiceRegistration 时 `attachMetadata` 后写覆盖（**推断**：`attachMetadata` 是 put 语义已证，但"多子上下文共享同一 Registration"场景无实证；参考 05-01 多注册中心机制） |
| 模式对比 | 18 的热替换有完整发布链（闭环）；16 的实例变更链断在起点（G1）——同主题、两种成熟度 |

---

## 二、适用场景与部署注意

### 2.1 什么时候该用 we://

| 场景 | 适合 | 不适合 |
|------|------|--------|
| 服务形态 | microsphere 全生态（服务都发布 `web.mappings` metadata） | 非生态/第三方服务（必须 `lb://` 混排） |
| 端点控制需求 | 需要 excludes（按 method/consumes 排除内部接口） | 只需要服务级转发（官方 discovery.locator 更简单） |
| 端点规模 | 中小（几十端点/服务） | 大端点集（G11 metadata 长度墙） |
| 变更频率 | 低频（端点/实例变化少） | 高频滚动发布（G1 断链，网关端点表不更新） |
| 错误语义 | 可接受"未命中即空响应" | 需要明确 5xx/404 语义（16-03 静默 200） |

**混排实践**：同一网关内 `we://all`（生态服务）与 `lb://`（非生态服务）路由共存——过滤器对非 `we` scheme 直接放行（16-03 第①步），互不干扰。**与官方过滤器共存已验证**：测试 yaml 里 we 路由与 `RequestSize`/`Retry` 官方过滤器路由（`request_size_route`/`retry_test`）同上下文运行——`CachingFilteringWebHandler` 按 routeId 缓存数组，官方 GatewayFilter 与 microsphere 全局过滤器在同一数组内正常共存（16-05 5.2）。

### 2.2 部署注意（呼应 17-03 的网关部署三问题）

| 17-03 问题 | we:// 场景下的表现 |
|------------|-------------------|
| 问题一：网关区域感知 | 需要 17 的配置生效（1.2 节 4 个条件），且 `ZoneContext` 需开启偏好 |
| 问题二：网关 zone 来源 | 与业务服务相同（ZoneLocator 自动检测或手动配置 `microsphere.availability.zone`）；we:// 不改变这一点 |
| 问题三：跨区域重试 | **启用 Zone 配置后，we:// 同样不 fallback**：WebFlux 版 `choose()` 走 Zone 过滤后的 LB 列表（17 的 ListSupplier），同区域耗尽即失败（放行 → 静默 200）；MVC 版 `lb()` 同区域耗尽抛 503——**"同区域优先"的容灾缺口对 we:// 路由同样适用**（与 17-03 问题三的结论一致） |

**滚动发布的现实问题（G1 的日常形态）**：服务滚动升级改了 API 路径后，网关端点缓存不更新——新接口请求"未命中 → 静默 200"，排障只能靠重启网关或手动触发配置刷新。**这是生产使用 16 前必须解决的第一个问题**（补 `ServiceInstancesChangedEvent` 发布者或调整心跳拦截）。

---

## 三、全局缺口总表（统一 G 编号）

> 编号映射：16-03 的 P1-P5、16-05 的风险 A-D、16-07 的 H1-H3 在此统一为 G1-G15；各文章内保留原编号以便追溯。

| # | 缺口 | 严重度 | 证据 | 影响 |
|---|------|--------|------|------|
| G1 | 实例变更断链：`ServiceInstancesChangedEvent` 无发布者 + 心跳被禁 | **高** | 16-05 八（测试手动 publish 佐证） | 实例上下线/滚动发布网关无感知，端点表永不更新 |
| G2 | WebFlux 阻塞选实例：事件循环 `toFuture().get()` | **高** | 16-03 P1 | 高并发下事件循环被堵，事故源 |
| G3 | context-path 丢失：`buildPath` 死代码（协议字段仍在，16-07 H3 同源） | **高** | 16-03 P2 / 16-02 L1 | context-path 部署服务网关转发 404 |
| G4 | WebFlux refresh NPE：无实例时 `getWebEndpointMappings(null)` | 中 | 16-03 P3 | 刷新静默失败、恢复后不自动补救（MVC 版有防御） |
| G5 | 样本实例：端点集只取一个实例 | 中 | 16-03 P4 | 实例间端点不一致（金丝雀）时路由表不完整 |
| G6 | MVC static 缓存跨上下文共享 | 中 | 16-04 问题① | 多上下文/多实例 routeId 串实例 |
| G7 | 空过滤器数组吞请求 | 中 | 16-05 风险 A | 缓存未建立窗口内请求无响应（窗口极小） |
| G8 | 反射读父类私有字段 `globalFilters` | 中 | 16-05 风险 B | SCG 升级改字段即静默失败 |
| G9 | `subscribe().dispose()` 阻塞消费路由源 | 低 | 16-05 风险 C | 异步路由仓库（Redis）时可能消费不完整 |
| G10 | 双缓存不一致窗口（两组件独立重建） | 低 | 16-05 风险 D | 毫秒级"新端点表+旧过滤器数组"，未原子化 |
| G11 | metadata 单条无分片 + URL 编码放大 | 中 | 16-07 H1 | 大端点集超注册中心长度限制，注册失败/截断 |
| G12 | 隐式 schema：字段硬约定、无版本号 | 中 | 16-07 H2 | 两端不同步时解析异常或伪端点 |
| G13 | 死代码：`GatewayUtils` 两个方法仅测试引用 | 低 | 16-03 P5 | 无功能影响，维护噪音 |
| G14 | 绑定失败静默：`onFailure` 未实现 | 低 | 16-06 | excludes 配置格式错误无日志、静默失效 |
| G15 | 静默 200 可观测性缺口：未命中端点无错误日志 | 中 | 16-03 | 生产"接口返回空"无法区分正常与被吞 |

**三条高危（G1-G3）是生产前置条件**：G1（发布者缺失）决定"要不要用"、G2（阻塞）决定"WebFlux 版能不能扛流量"、G3（context-path）决定"存量 context-path 服务能不能迁"。

---

## 四、验证边界（哪些是证据、哪些是推断）

**证据级**（源码/字节码/git 取证）：全部 G1-G15 的行为描述、过滤器序、事件链、绑定机制、协议字段、演进史提交归属。

**推断级**（逻辑推导或外部事实，未在本工作区实证）：

| 项 | 状态 |
|----|------|
| Nacos metadata 默认 4096 上限（G11 的触发条件） | 外部事实（Nacos 默认值），未在本环境验证 |
| G7/G10 的实际窗口时长 | 逻辑推导（毫秒级），未实测 |
| 多子上下文共享 Registration 的 metadata 竞态 | 推断（1.3 节已标注） |
| 2025-10-31 重构的深层动机 | 未知（16-02 已诚实标注） |

---

## 五、总结论（引用要点）

- **16 的生态位置**：纯消费者扩展——模型在 03、发现/协议在 05、绑定机制在 04、Zone 能力在 17；**16 自己只写"网关怎么消费"**，侵入面收敛在自动配置与事件拦截两处，可完全剥离（16-01）。
- **Zone 真相**：网关 Zone 优先 = 17 全局 LoadBalancer 配置（4 个条件），16 无 Zone 代码；TODO 只是样本选择优化。
- **两条断链同构**：16 的实例变更断链与 17 的配置改 zone 断链都是"事件接口预留、发布者缺失"——microsphere 各项目的共同模式。
- **生产判断**：学习/演示价值高（两代架构对比、双实现对比、与官方 SCG 九维对比）；生产使用前必须解决 G1-G3。
