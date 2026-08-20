# H-5 生命周期/配置 — HikariConfig 校验→seal→PoolBase.newConnection + PropertyElf

> 依赖 C-11 + H-13 DriverDataSource + s34-s36 (复用) | 🔴 Deep | 6 KP | [模式: 不可变守卫 + 反射绑定 + 模板方法]

**读者处境**: 配了 spring.datasource.hikari.maximum-pool-size 等 — 配置怎么校验?为什么池启动后配置不能再改?新连接建立时怎么初始化?

### 1. 配置校验与封印 — validate + seal

场景: HikariDataSource 创建时配置先校验 — 校验什么?为什么之后不可改?

源码路径:
- `HikariConfig.java:1045` — **validate**: 数据源一致性(必须有 dataSource/dataSourceClassName/jdbcUrl 之一, L1047 区域) + 数值校验
- `HikariConfig.java:1010` — **seal**: `this.sealed = true`(L1010-1013) — 建池后封印
- 数值校验(validate 内): `maxLifetime<30s → 回默认`(L1103-1104) / `keepaliveTime<30s → 禁用`(L1109-1110) / `leakDetectionThreshold<2s → 禁用`(L1121-1122)
- `checkIfSealed`: 多数 setter 开头调(checkIfSealed 调用 28 次, setXxx 共 39 个) — 已封印则抛异常拒绝修改

关键设计: **Why seal 不可变？** 池启动后配置必须固定, 否则并发下池行为不一致 — seal 后所有 setter 经 checkIfSealed 拒绝, 保证配置在运行期不可变(只允许 JMX 白名单方法); 这避免"运行时改池配置导致不可预测"。**Why 数值校验回退？** 非法/过小值(maxLifetime<30s)用日志警告并回默认/禁用, 而非直接抛错 — 更宽容。[模式: 不可变守卫]

数据流: new HikariDataSource(config) → validate(L1045) 校验(数据源一致性 + 数值回退) → copyStateTo → new HikariPool → seal(L1010, sealed=true) → 后续 setter 调 checkIfSealed → 抛 IllegalStateException。

### 2. 连接创建 — newConnection + setupConnection

场景: 池需要一个新连接 — 怎么从数据源拿原始连接并初始化到可用状态?

源码路径:
- `PoolBase.java:360` — **newConnection(isEmptyPool)**: 取 credentials → `dataSource.getConnection()`(L373, 或带 user/pass) → `setupConnection(connection)`(L378)
- `PoolBase.java:416` — **setupConnection**: `setNetworkTimeout`(L418-424) → `setReadOnly`(L426) → `setAutoCommit`(L430) → `checkDriverSupport`(L433) — 重置连接为配置状态
- `PoolBase.java:321` — **initializeDataSource**: 从 config 初始化 DataSource(驱动/JNDI/DriverDataSource), 构造时调(L121)

关键设计: **Why setupConnection 重置？** 驱动返回的连接可能有脏状态(错误的 autoCommit/readOnly) — 统一重置为配置值, 保证每次借出的连接状态一致(避免上一位用户的设置泄漏)。**Why newConnection 委托 dataSource？** 连接来源(DataSource/DriverDataSource/JNDI)由 initializeDataSource 决定, newConnection 只负责"拿+初始化" — 解耦来源与创建。[模式: 模板方法 + 状态重置]

数据流: 需新连接(H-3 触发) → newConnection(L360) → dataSource.getConnection(L373, 来源=initializeDataSource 配的) → setupConnection(L378/416): setNetworkTimeout/readOnly/autoCommit → 返回可用连接 → 包 PoolEntry。

### 3. 属性绑定 + 边界 — PropertyElf + initializeDataSource

场景: 外部 Properties 怎么批量注入到配置对象?数据源从哪来?

源码路径:
- `PropertyElf.java:43` — **setTargetFromProperties(target, properties)**: 遍历属性, 按属性名找 `setXxx` 方法(`setProperty` L130 → `writeMethod` L136) 反射调用 — 把 Properties 映射到配置对象的 setter
- `PoolBase.java:321` — **initializeDataSource**: 优先 `config.getDataSource()`(用户实例)→ 否则 `dataSourceClassName` 实例化(L330-333)→ 否则 `jdbcUrl` 建 `DriverDataSource`(L334-336, H-13)→ 否则 `dataSourceJNDI` JNDI 查找(L337-341); 用 PropertyElf 绑定 dataSourceProperties
- 边界: 数据源内核(C-11/s34-s36)已覆盖 JDBC DataSource; 本域只讲 Hikari 的配置→连接生命周期

关键设计: **Why 反射绑定？** 用户可写任意 `com.zaxxer.hikari.*` 属性(HikariCP 或底层数据源属性) — 反射按 setter 动态注入, 无需为每个属性写 switch; 未匹配属性忽略。**Why initializeDataSource 多来源归一？** dataSource 实例/dataSourceClassName/jdbcUrl(DriverDataSource)/JNDI 按优先级取第一个非 null, 统一到 PoolBase 的一个 DataSource 字段, 后续 newConnection 只认这一个来源。[模式: 反射绑定 + 多来源归一]

数据流: 外部 Properties → PropertyElf.setTargetFromProperties(L43) → 反射调 setXxx 注入 → 配置对象就绪 → initializeDataSource(L321) 建/取 DataSource → newConnection(L360) 从它拿连接。

→ 引出 H-4: 归还流程 — 生命周期之后: ProxyConnection.close→HikariPool.recycle→ConcurrentBag.requite(前置 H-2/H-12)。
