# H-5 生命周期/配置 — HikariConfig 校验→seal→PoolBase.newConnection + PropertyElf

> 项目: HikariCP | 🔴 Deep / 1 篇 | HikariConfig.java(1269行)+PoolBase.java(793行)+PropertyElf.java(60行)
> 基线: HIKARICP-PLAN H-5 (第1层池核心, 🔴) — 配置不可变+连接创建; 前置: **C-11 jdbc DataSource + H-13 DriverDataSource + s34-s36 jdbc** — 展开校验/seal/建连

---

## §0.8

- 🔴 Deep，1篇 — 配置校验与封印(HikariConfig.validate[L1045]: 数据源一致性+数值校验[maxLifetime<30s 回默认/keepaliveTime 相关/leakDetectionThreshold<2s 禁用]; seal[L1010] 置 sealed → 后续 setter 经 checkIfSealed 拒绝, 配置不可变) → 属性绑定(PropertyElf.setTargetFromProperties[L43]: 反射 setXXX 注入外部 Properties) → 连接创建(PoolBase.newConnection[L360]: dataSource.getConnection[L373]→setupConnection[L378/416] 重置 networkTimeout/readOnly/autoCommit; initializeDataSource[L321] 初始化 DataSource)
- 设计模式: [模式: 不可变守卫]—seal+checkIfSealed; [模式: 反射绑定]—PropertyElf; [模式: 模板方法]—setupConnection 连接初始化

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HikariConfig.java:1045,1010 | 校验封印 | **validate(L1045)+seal(L1010)**: 配置校验后封印, setter 拒绝 | High |
| PropertyElf.java:43 | 属性绑定 | **setTargetFromProperties(L43)**: 反射 setXXX | High |
| PoolBase.java:360,373,378 | 建连 | **newConnection(L360)**: dataSource.getConnection(L373)→setupConnection(L378) | High |
| PoolBase.java:416,321 | 初始化 | **setupConnection(L416)**: 重置 networkTimeout/readOnly/autoCommit; initializeDataSource(L321) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 生命周期是一条线(配置→封印→建连), 3 块耦合 — 1篇 (~50行) 按"配置校验封印 → 属性绑定 → 连接创建"展开; C-11/H-13 复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 配置校验与封印 (validate + seal) | 🔴 | **为什么🔴**: 配置如何不可变 |
| P1-2 | newConnection 连接创建 (getConnection + setupConnection) | 🔴 | **为什么🔴**: 连接怎么初始化 |
| P1-3 | PropertyElf 属性绑定 (反射 setXXX) | 🔴 | **为什么🔴**: 外部配置怎么注入 |
| P2-1 | initializeDataSource (DataSource 初始化/JNDI) | 🟡 | **为什么🟡**: 数据源来源 |
| P2-2 | 配置数值校验 (maxLifetime/keepaliveTime 等) | 🟡 | **为什么🟡**: 非法值处理 |
| P3-1 | 与 C-11/H-13 边界 | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **配置封印** | 🔴 | 不可变 |
| B | **连接创建** | 🔴 | 初始化 |
| C | **绑定与边界** | 🟡 | 外部配置 |

> **Cluster A (§1)**: HikariConfig.validate + seal + checkIfSealed
> **Cluster B (§2)**: PoolBase.newConnection(dataSource.getConnection + setupConnection)
> **Cluster C (§3)**: PropertyElf 绑定 + initializeDataSource + 数值校验 + 边界

→ 引出 H-4: 归还流程 — 生命周期之后: ProxyConnection.close→HikariPool.recycle→ConcurrentBag.requite(前置 H-2/H-12)
