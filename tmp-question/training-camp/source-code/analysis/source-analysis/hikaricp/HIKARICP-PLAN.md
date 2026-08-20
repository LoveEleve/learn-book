# HikariCP — 知识网络化规划 (H-1~H-13)

> **日期**: 2026-08-12 | **依据**: 原始执行计划 阶段3.1 (13域) + 00 域发现深度 REVIEW 修复
> **源码**: `/data/workspace/source-code/code/spring/hikaricp` (48 文件 / 9654 行, com.zaxxer.hikari)
> **定位**: 阶段3 数据与存储首站 — 连接池. 核心 = **ConcurrentBag 无锁并发** (HikariCP "快" 的秘密)
> **知识网络**: 本文含 前置/复用/引出 双链(Obsidian 06 §6), 与 Spring 层(S-10 Boot DataSource / C-11 jdbc DataSource / s29-33 tx)双向互联

---

## 入口点与主线

`HikariDataSource.getConnection()` → `HikariPool.getConnection()` → `ConcurrentBag.borrow()` → `PoolEntry`(三态) → `PoolBase`(建连/验证/超时) — 主路径. 旁路: metrics/ (指标), jmx, hibernate/ 集成等.

---

## 重构后域清单 (13 域 / 15 篇)

### 第 1 层: 池核心 (5 域 全部 🔴)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| H-1 | 核心架构 (总览) | HikariConfig→HikariDataSource→HikariPool→ConcurrentBag 全链路 | **S-10 Boot DataSource(引出: 本域是 S-10 池化的深入)、C-11 jdbc DataSource** | 展开: 池全链路+初始化; **引用: S-10 的 DataSourceAutoConfiguration→HikariDataSource 接线** |
| H-2 | ConcurrentBag + PoolEntry + FastList | borrow/requite/reserve + **PoolEntry 三态(CAS via AtomicIntegerFieldUpdater) + FastList(线程本地表/removeLast O(1))** | H-13 DriverDataSource | 展开: 无锁并发容器+条目状态机+微优化 |
| H-3 | 获取流程 | getConnection→borrow→createEntry→addConnectionExecutor | H-2、H-5 | 展开: 借出链+动态扩池 |
| H-4 | 归还流程 | ProxyConnection.close→recycle→requite | H-2、H-5、H-12 | 展开: 归还链+状态复位 |
| H-5 | 生命周期/配置 | HikariConfig 校验→seal 熔断→PoolBase.newConnection + **PropertyElf 属性绑定(setTargetFromProperties→反射 setXXX)** | C-11、s34-s36 jdbc | 展开: 配置校验/不可变/seal/建连 |

### 第 2 层: 后台维护 (2 域 🔴+🟡)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| H-6 | HouseKeeper | 30s 周期—idleTimeout 淘汰/fillPool 补到 minIdle/**ClockSource 时钟** | H-3、H-4 | 展开: 定时维护线程+时钟抽象 |
| H-7 | 连接验证 | connectionTestQuery/validationTimeout/isValid/ALIVE_BIT | H-5 PoolBase | 展开: 活连接校验策略 |

### 第 3 层: 代理与监控 (4 域 全部 🟡)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| H-8 | 泄漏检测 | ProxyLeakTask/leakDetectionThreshold | H-12 | 展开: 借用超时泄漏跟踪 |
| H-9 | 指标监控 | MetricsTracker/PoolStats/Micrometer/Dropwizard/Prometheus | H-1 | 展开: 指标桥接(薄) |
| H-10 | JMX | HikariPoolMXBean/HikariConfigMXBean | H-1 | 展开: MXBean 暴露(薄) |
| H-11 | SuspendResumeLock | 暂停/恢复连接池 | H-1 | 展开: 挂起语义 |

### 第 4 层: 支撑 (2 域 🔴+🟡)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| H-12 | 代理生成 | ProxyFactory/JavassistProxyFactory→**整个代理族**(ProxyConnection/Statement/PreparedStatement/CallableStatement/ResultSet/DatabaseMetaData + **FastList openStatements**) | s24-s28 aop 对照 | 展开: 字节码代理生成+方法级拦截; **对照: spring-aop 动态代理(CGLIB)** |
| H-13 | DriverDataSource | JDBC 驱动封装 | C-11 | 展开: driver→Connection 桥(薄, 基石) |

---

## 已排除 (00 §3 thin-wrapper/边缘 — 防"存在=域")

| 类/包 | 原因 |
|-------|------|
| hibernate/HikariConnectionProvider | 第三方集成 glue, 无池决策 |
| HikariJNDIFactory | JNDI 注册 glue |
| SQLExceptionOverride | 接口, 薄 |
| IsolationLevel | 枚举, 薄 |
| HikariCredentialsProvider/Credentials | 凭证抽象, 薄 |
| UtilityElf | 工具, 薄 |

---

## 知识网络图 (跨大纲边 — Obsidian 双链)

```
← 复用/内核来源:
   spring-jdbc (C-11, s34-s36) ──→ H-5, H-13 (DataSource/JdbcTemplate 内核)
   spring-tx (s29-s33) ──→ H-5, H-4 (连接/事务边界)
   spring-aop (s24-s28) ──→ H-12 (代理机制对照)

→ 引出/消费者 (前向引用到 Spring Boot — 知识网络必须双向):
   HikariCP H-1~H-13 ──→ S-10 Boot DataSource (本域是 S-10 的池化内核深入)
   S-10 (s74-boot-datasource) 已标注 "池化深入在阶段3 HikariCP" ← 呼应
   HikariCP H-3/H-4 ──→ C-11 (s53-jdbc-datasource) 连接获取/归还
   HikariCP H-1 ──→ S-8 (server 内嵌容器无关, 但 DataSource 装配同 S-2 自动装配管线)

   📌 双链格式 (每篇大纲 header 写):
   前置: [[C-11]] [[s34-jdbc-template]] ...
   复用: [[s29-tx]] ...
   引出: [[s74-boot-datasource]] (S-10) ...
```

> **⚠️ 前向引用原则 (06 §2)**: 大纲正文禁止引用未分析域; 但 **HikariCP ↔ Spring Boot 的"引出/被消费"边**是**已分析域** (S-10/C-11 已完成) — 可放心双向写. 知识网络图里"引出→S-10"合法, 因为 S-10 已存在.

---

## 执行顺序 (拓扑: 叶子先)

H-13 → H-2 → H-3 → H-5 → H-4 → H-6 → H-7 → H-12 → H-8 → H-1 → H-9 → H-10 → H-11

> 教学顺序: H-1(总览) 可前置作导航, 但深度依赖按上述拓扑. 每域走 v5 全管线 (KP→大纲→questions→六层深审→更新 HANDOFF). ⚠️ 实际执行 H-3 先于 H-5(结尾桥链一致), 与初始"H-5 先于 H-3"有偏差 — H-3 对 H-5 用导航指针而非深依赖, 已记录.

---

## 深度分类复核

- **7🔴 / 6🟡** (54% 🔴, 未犯 80% 反模式)
- 🔴 = 池核心定义性机制 (ConcurrentBag/HouseKeeper/代理族/配置seal)
- 🟡 = 支撑/薄 (验证/泄漏/指标/JMX/挂起)
- **REVIEW 修复** (相对原始执行计划): ①H-2 补 FastList+PoolEntry ②H-12 补整个代理族+FastList ③H-5 补 PropertyElf 绑定 ④显式排除清单 ⑤H-1/S-10 双向知识网络边

---

## 与原始执行计划 (H-1~H-13) 的差异

| 原始 | 本规划 | 理由 |
|:--:|:--:|---|
| H-2 ConcurrentBag | + FastList + PoolEntry | REVIEW 发现三态状态机与微优化表未点名 |
| H-12 代理生成 | + 整个代理族 | 9 个 Proxy 类, 原计划只点 Connection/Statement |
| H-5 生命周期 | + PropertyElf 绑定 | 配置→setXXX 反射绑定机制 |
| — | + 排除清单 | 00 要求显式排除薄/边缘 |
| — | + 知识网络双链 | 与 S-10/C-11/tx/aop 双向互联, 供 Obsidian |
