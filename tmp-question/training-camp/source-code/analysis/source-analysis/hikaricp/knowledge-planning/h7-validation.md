# H-7 连接验证 — connectionTestQuery / validationTimeout / isValid

> 项目: HikariCP | 🟡 Working / 1 篇 | PoolBase.java(793行)+HikariConfig.java(1269行)
> 基线: HIKARICP-PLAN H-7 (第2层后台维护, 🟡) — 活连接校验; 前置: **H-5 PoolBase(建连)** — 展开 isConnectionDead/验证方式

---

## §0.8

- 🟡 Working，1篇 — 验证时机(PoolBase.isConnectionDead[L157]: 供 H-3 borrow 前/H-6 检查; setNetworkTimeout(validationTimeout)[L160] 限制验证时长) → 验证方式(isUseJdbc4Validation[L92, =getConnectionTestQuery()==null, L112]: 是→connection.isValid(validationSeconds)[L165]; 否→statement.execute(connectionTestQuery)[L173]) → 验证后清理(restore setNetworkTimeout(networkTimeout)[L177] + isIsolateInternalQueries rollback[L179])
- 设计模式: [模式: 双策略]—isValid vs connectionTestQuery; [模式: 超时保护]—validationTimeout; [模式: 状态还原]—finally 恢复

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| PoolBase.java:157,160 | 验证入口 | **isConnectionDead(L157)**: setNetworkTimeout(validationTimeout)(L160) | High |
| PoolBase.java:92,112,164,165 | 方式选择 | **isUseJdbc4Validation(L92, =testQuery==null L112)**: isValid(L165) | High |
| PoolBase.java:170,173 | 测试查询 | **setQueryTimeout(L170)+statement.execute(connectionTestQuery)(L173)** | High |
| PoolBase.java:177,179 | 清理 | **restore networkTimeout(L177)+isolate rollback(L179)** | High |
| HikariConfig.java:76,369 | 配置 | **connectionTestQuery(L76)/getConnectionTestQuery(L369)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 连接验证是一条线(时机→方式→清理), 3 块耦合但机制较薄 — 1篇 (~46行) 按"时机与超时 → 验证方式 → 清理与边界"展开; H-5 PoolBase 复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 验证时机 (isConnectionDead + validationTimeout) | 🔴 | **为什么🔴**: 何时/多快验证 |
| P1-2 | 验证方式选择 (isValid vs connectionTestQuery) | 🔴 | **为什么🔴**: 两种校验策略 |
| P1-3 | 验证后清理 (restore + rollback) | 🔴 | **为什么🔴**: 不污染连接 |
| P2-1 | connectionTestQuery 配置 | 🟡 | **为什么🟡**: 测试查询定制 |
| P2-2 | isUseJdbc4Validation 判定 | 🟡 | **为什么🟡**: 自动选 isValid |
| P3-1 | 与 H-3/H-5 边界 | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **时机与超时** | 🔴 | 何时验证 |
| B | **验证方式** | 🔴 | 怎么验证 |
| C | **清理与边界** | 🟡 | 不污染 |

> **Cluster A (§1)**: isConnectionDead + setNetworkTimeout(validationTimeout)
> **Cluster B (§2)**: isValid vs connectionTestQuery 双策略
> **Cluster C (§3)**: 清理(restore/rollback) + connectionTestQuery 配置 + 边界

→ 引出 H-12: 代理生成 — 验证之后: ProxyFactory/Javassist 生成 ProxyConnection 代理族(前置 s24-s28 aop)
