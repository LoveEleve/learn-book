# 连接池与 ProxySQL — 数据库外部的并发控制、路由与故障隔离

> Cluster A | 覆盖知识元: 9.3 连接池 + 9.4 ProxySQL + 9.5 指标桥接补充 | 依赖: 15 Performance Schema、13 优化器/慢日志、11 故障切换 | 读者基线: JDBC、连接数、主从复制、读写分离
> 文章定位: MySQL 监控主题的第二篇；把数据库外部的并发控制层补齐——应用为什么需要连接池，中间件又如何做读写分离、重写和镜像
> 打开新视角: 数据库性能不只发生在 mysqld 里，**连接池决定请求如何进入数据库，ProxySQL 决定请求被送到哪台数据库，以及是否被改写/镜像/隔离**

---

### 概念依赖链

```
15 P_S/监控 + 11 failover/backup + 09 replication → 本篇: 连接池/中间件
  ├─ §1 连接池的固定容量与借还生命周期
  ├─ §2 HikariCP 的并发数据结构与泄漏检测
  ├─ §3 JDBC/SPI 与应用接入层
  ├─ §4 ProxySQL(读写分离/重写/镜像)
  └─ §5 连接池/代理/数据库指标联动
先讲: 为什么池化 → 池内部如何快 → JDBC 接口层 → ProxySQL 路由层 → 指标联动
后续依赖: 阶段10 扩展专题、阶段11 理论/选型
```

### 叙事顺序

1. 问题引入——数据库 QPS 还没到极限，应用为什么先被连接创建、队列堆积和错误路由拖慢？
2. 连接池——固定容量、借还与等待
3. HikariCP——并发结构与泄漏检测
4. JDBC/SPI——应用如何真正拿到连接
5. ProxySQL——读写分离、重写和流量镜像
6. 指标联动——池、代理、数据库三层一起看
7. 收束

### 1. 连接池 — 固定容量不是保守，而是控制系统稳定性的边界

场景提示: Web 应用线程数不断增加时，为什么不应该让每个请求自己创建一条新 MySQL 连接？ [写作时展开]

关键设计: 连接池把昂贵的建连/认证/初始化变成可复用资源，并用等待队列表达背压：

```[pseudocode]
请求进入:
  borrow connection
    空闲连接可用? 直接取
    无空闲? 在等待队列排队/超时失败

请求完成:
  reset state if needed
  return connection to pool

池参数:
  max pool size
  minimum idle / keepalive / max lifetime
  connection timeout / validation
```

Why: 为什么固定大小池常比“按需无限扩容”更稳定？——**数据库能并行处理的连接数、锁竞争、Buffer Pool、线程调度和查询尾延迟都有上限**；池无限长大只会把排队从应用层挪到数据库内部。池大小应该来源于数据库承载能力和 workload 验证，而不是简单等于 CPU 核数或 Tomcat 线程数。 [系统性能: 连接池本质上是队列与背压装置，和 Little 定律/等待分析直接相关]

比喻锚点: 连接池像高速收费站车道数：车道不是越多越好，超过下游道路承载后只会把堵车推到更深处。 [写作时展开]

### 2. HikariCP — “快”来自减少共享与生命周期约束

场景提示: 为什么 HikariCP 常被视为高性能连接池，它快在哪里？ [写作时展开]

关键设计: HikariCP 通过较小的功能面和并发数据结构降低借还路径开销，但这些实现并不是唯一可行方案：

```[pseudocode]
核心生命周期:
  create → idle
  borrow → in-use
  return → idle
  retire/close → recreate

并发目标:
  借还路径尽量短
  减少中心锁竞争
  连接创建/销毁放到后台或边界路径

泄漏检测:
  borrow 超过阈值未归还
  → 记录借用栈/告警
```

Why: 为什么“池快”不等于“数据库就快”？——**连接池主要优化的是应用到数据库之间的等待和创建成本**；如果 SQL、锁、I/O 或复制已经是瓶颈，再快的池也只会更快地把请求送进拥堵点。泄漏检测也只是发现长借用，不自动证明业务忘记关闭连接——大事务、长流式读取和网络阻塞都可能触发。 [连接池: 具体内部结构、FastList/ConcurrentBag 等属于实现细节，版本和库实现不同]

比喻锚点: HikariCP 像高效的钥匙柜，拿钥匙和还钥匙很快；但钥匙背后的仓库如果本身拥堵，前台更快并不会让仓库处理更快。 [写作时展开]

### 3. JDBC 与 SPI — 连接池包裹的并不是“神秘黑盒”，而是 Driver/Connection 协议

场景提示: 应用代码只写 `DataSource.getConnection()`，底层怎样找到 MySQL 驱动并创建真实连接？ [写作时展开]

关键设计: 连接池通常包装 JDBC DataSource/Driver 体系，驱动发现和连接创建依赖 SPI/URL/配置：

```[pseudocode]
应用:
  DataSource.getConnection()
  → 连接池尝试借出已有连接
  → 若无且可扩容, 调驱动创建新连接

驱动层:
  DriverManager / DataSource / SPI
  → mysql driver connect(url, properties)
  → 协议握手/认证/session setup
```

Why: 为什么 JDBC 配置错误会在连接池层面表现为超时、借用失败或大量 pending？——**因为池的上游接口很简单，但底层仍要经历 DNS、TCP、TLS、认证、初始化和网络路径**；连接创建慢不一定是池实现慢，可能是驱动、证书、DNS、ProxySQL 或数据库端的握手瓶颈。 [JDBC: SPI/Driver/DataSource 接口与连接池是包装关系，不是替代关系]

比喻锚点: DataSource 像前台领号机，真正把你送进数据库大楼的仍是电梯、安检和门禁；前台卡顿不一定是领号机本身坏了。 [写作时展开]

### 4. ProxySQL — 数据库前的 L7/L4 决策层

场景提示: 应用既想读写分离、又想做 SQL 黑名单、流量镜像和路由切换，为什么不让应用自己硬编码主从地址？ [写作时展开]

关键设计: ProxySQL 在应用和 MySQL 实例之间维护用户、主机组、规则和连接复用：

```[pseudocode]
应用连接 ProxySQL
  → 认证/用户规则
  → 根据 SQL 类型/规则/事务状态
     选择 writer hostgroup / reader hostgroup
  → 可做查询重写、黑名单、镜像
  → 后端连接池/复用到 MySQL 实例

典型场景:
  读写分离
  故障切换后的主从角色切换
  风险 SQL 拦截
  流量镜像到测试环境
```

Why: 为什么 ProxySQL 不是“加一层就一定更稳”？——**代理自身会引入连接状态、配置一致性、额外延迟和运维复杂度**；读写分离还要处理事务内读一致性、延迟副本和路由错误。镜像流量也会放大下游负载，不能无边界开启。 [MySQL: hostgroup/规则/镜像语义按 ProxySQL 版本和配置核对]

比喻锚点: ProxySQL 像机场中转塔台：能把旅客分配到不同航线、拦截危险货物、复制训练流量，但塔台本身也可能成为新拥堵点。 [写作时展开]

### 5. 指标联动 — 连接池、代理和数据库要一起看

场景提示: 应用请求超时时，是连接池等待、ProxySQL 路由慢，还是数据库执行慢？ [写作时展开]

关键设计: 三层指标要按同一时间窗口对齐：

```[pseudocode]
连接池层:
  active/idle/pending/total
  borrow timeout / creation time / leak detect

代理层:
  后端连接数、hostgroup 路由命中、错误/重试/镜像量

数据库层:
  threads/connected/running
  digest/锁等待/复制延迟/Buffer Pool/Redo

方法:
  对齐同一时刻的 pending、路由延迟、SQL P99、锁等待
  → 判断瓶颈是在入库前、代理层还是数据库内部
```

Why: 为什么只看 Hikari 指标无法证明数据库需要扩容？——**pending 高可能是池太小、连接创建慢、代理不可达或数据库慢**；只看数据库 CPU 也可能漏掉应用 borrow timeout。监控必须跨层拼接，才能避免把背压误认为数据库算力不足。 [系统性能: Little 定律、等待分析和数据库内部 P_S 指标要一起解释]

### 6. 收束

数据库外部并发控制闭环：

```[pseudocode]
应用请求
  → 连接池 borrow/backpressure
  → JDBC/Driver/网络握手
  → ProxySQL 路由/重写/镜像
  → MySQL 执行/锁/复制
  → 指标与日志沿三层回传
```

**Aha Moment**: "数据库性能不只在存储引擎里发生：**连接池决定请求如何排队进入数据库，ProxySQL 决定请求去哪里、能不能被改写或隔离，MySQL 才负责真正执行**。三层任何一层都可能成为尾延迟入口。"
**回答读者三问**: ①为什么连接池不能无限扩=只会把排队推入数据库；②ProxySQL 解决什么=路由、隔离、镜像和连接复用；③故障时看哪里=池 pending、代理后端状态、数据库锁和 SQL P99 要一起对齐。

---

### 核心悬念

**"MySQL 主干已经从架构、事务、复制走到连接池和代理；还有哪些扩展路线（LSM、LevelDB、BoltDB、HTAP）能帮助你从单机行存走向更广义的数据系统？"**

→ 引出阶段 10 / 扩展专题 — LSM、LevelDB、BoltDB、大数据 SQL 与 CDC/HTAP。