# H-12 代理生成 — ProxyFactory → JavassistProxyFactory → Proxy 代理族

> 项目: HikariCP | 🔴 Deep / 1 篇 | ProxyFactory.java(80行)+JavassistProxyFactory.java(160行)+ProxyConnection.java(340行)+ProxyStatement/PreparedStatement/CallableStatement/ResultSet/DatabaseMetaData
> 基线: HIKARICP-PLAN H-12 (第4层支撑, 🔴) — 代理生成; 前置: **s24-s28 spring-aop(代理对照) + H-3/H-4** — 展开字节码代理生成+方法级拦截

---

## §0.8

- 🔴 Deep，1篇 — 代理族结构(ProxyConnection[L37]/ProxyStatement/PreparedStatement/CallableStatement/ResultSet/DatabaseMetaData 6 个抽象类 implements 对应 JDBC 接口, 包 delegate[L51]+poolEntry[L53]+leakTask[L54]) → Javassist 生成(JavassistProxyFactory[L42]: generateProxyClass 生成 6 具体代理类[L64-72] + 把方法体注入 ProxyFactory 的静态工厂) → ProxyFactory 门面(静态工厂 getProxyConnection 等[L46], 方法体由 Javassist 注入[L48]; PoolEntry.createProxyConnection 调用→供 H-3 借出/H-4 归还)
- 设计模式: [模式: 字节码代理]—Javassist 生成; [模式: 门面工厂]—ProxyFactory; [模式: 委托]—delegate 转发+拦截

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ProxyConnection.java:37,51,53 | 代理族 | **abstract implements Connection(L37)**: delegate(L51)+poolEntry(L53)+leakTask(L54) | High |
| JavassistProxyFactory.java:42,64,72 | 生成 | **class(L42)**: generateProxyClass 6 类(L64-72) | High |
| ProxyFactory.java:46,48 | 门面 | **getProxyConnection(L46)**: 方法体由 Javassist 注入(L48) | High |
| JavassistProxyFactory.java:114 | 生成方法 | **generateProxyClass(L114)**: ClassPool 生成具体代理 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 代理生成是一条线(结构→生成→门面), 3 块耦合 — 1篇 (~50行) 按"代理族结构 → Javassist 生成 → 门面与衔接"展开; s24-s28 aop 对照。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 代理族结构 (6 抽象代理类) | 🔴 | **为什么🔴**: 代理怎么组织 |
| P1-2 | Javassist 字节码生成 (generateProxyClass + 注入) | 🔴 | **为什么🔴**: 具体代理怎么来 |
| P1-3 | ProxyFactory 门面 + 池衔接 | 🔴 | **为什么🔴**: 代理怎么被使用 |
| P2-1 | 与 spring-aop CGLIB 对照 | 🟡 | **为什么🟡**: 代理方式对比 |
| P2-2 | 委托/状态追踪 (delegate+poolEntry+leakTask) | 🟡 | **为什么🟡**: 代理内部 |
| P3-1 | 与 H-3/H-4/H-8 边界 | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **代理族结构** | 🔴 | 组织 |
| B | **Javassist 生成** | 🔴 | 生成 |
| C | **门面与边界** | 🟡 | 使用 |

> **Cluster A (§1)**: 6 抽象代理类(delegate/poolEntry/leakTask)
> **Cluster B (§2)**: JavassistProxyFactory(generateProxyClass + 注入)
> **Cluster C (§3)**: ProxyFactory 门面 + s24-s28 对照 + 池衔接

→ 引出 H-8: 泄漏检测 — 代理之后: ProxyLeakTask/leakDetectionThreshold 的借用超时泄漏跟踪(前置 H-12)
