# MySQL 死锁、MDL 与锁优化 — 从等待图到可回滚的并发策略

> Cluster A | 覆盖知识元: 4.5-4.8 | 依赖: 07-acid-mvcc-locks、05-index-maintenance | 读者基线: MVCC、Record/Gap/Next-Key Lock、事务
> 读者处境: 07 篇已经知道锁的种类；本篇回答线上最难的部分：锁为什么互相等待，DDL 为什么被一个“空闲事务”卡住，怎样观测和降低锁等待而不是盲目加超时
> 打开新视角: 锁问题分成三层——**行/范围锁保护数据，MDL 保护对象定义，InnoDB 内部 mutex/rw-lock 保护引擎结构**；死锁检测只能处理其中一部分

---

### 概念依赖链

```
07 MVCC/行锁 + 05 索引 → 本篇: MDL/死锁/锁优化
  ├─ §1 MDL(对象定义与DDL阻塞)
  ├─ §2 wait-for graph(事务死锁)
  ├─ §3 死锁日志/案例(诊断证据)
  ├─ §4 锁优化(粒度/索引/隔离/NOWAIT)
  └─ §5 InnoDB内部锁/显式锁(引擎内部并发)
先讲: MDL → 等待图 → 诊断 → 优化 → 内部锁
后续依赖: 阶段5 复制(锁等待如何放大复制延迟)
```

### 叙事顺序

1. 问题引入——一条 ALTER 被卡住，业务查询也变慢；究竟是行锁、MDL 还是引擎内部锁？
2. MDL——为什么空闲事务能阻塞 DDL
3. 死锁与 wait-for graph——为什么不是所有锁等待都能等下去
4. 诊断案例——日志、metadata_locks、事务和索引路径
5. 锁优化——减少持有、缩小范围、改变等待策略
6. InnoDB 内部锁——数据库内部并发瓶颈
7. 收束

### 1. MDL — DDL 为什么会被读事务卡住

场景提示: 一个事务只执行了 SELECT 却迟迟不提交，另一个会话 ALTER TABLE 为什么可能一直等待？ [写作时展开]

关键设计: Metadata Lock 保护表/对象定义在并发访问和 DDL 变更期间的一致性：

```[pseudocode]
普通读/写:
  获取对应 MDL shared 类型
  → 访问表定义/执行语句
  → 事务结束或语句边界释放(具体类型/时机需核对)

DDL:
  申请更强 MDL exclusive
  → 等待冲突的 shared MDL 释放
  → 执行/切换定义
  → 释放 MDL

阻塞链:
  长事务持有 shared MDL
  → DDL 排队
  → 后续请求可能继续受到队列/优先级影响
```

Why: 为什么“事务空闲”仍可能阻塞 DDL？——**事务是否执行 CPU 不代表它是否仍持有对象级锁**：事务打开后不提交，MDL/行锁/快照等生命周期可能继续存在。DDL 排查要看持有者、等待者、事务开始时间和应用连接池，而不是只看当前 SQL。 [MySQL: `performance_schema.metadata_locks`、processlist 和事务视图按版本联合排查]

比喻锚点: MDL 像装修前的楼宇产权锁：住户只是坐在屋里不动，也可能持有钥匙，装修队不能拆墙。 [写作时展开]

### 2. 死锁与 wait-for graph — 等待为什么会形成环

场景提示: 两个事务都在等待，为什么有时只是慢，有时 MySQL 会主动回滚其中一个？ [写作时展开]

关键设计: 死锁是等待图出现环，InnoDB 检测后选择牺牲者回滚：

```[pseudocode]
T1:
  lock row A
  → request row B → wait

T2:
  lock row B
  → request row A → wait

wait-for graph:
  T1 → T2
  T2 → T1
  → cycle detected
  → choose victim
  → rollback victim
  → release locks
  → other transaction continues
```

Why: 为什么“设置更长 lock wait timeout”不能解决死锁？——**timeout 只是让等待最终失败，不能消除循环依赖，甚至延长资源占用**；死锁检测与回滚代价、事务大小、锁持有对象有关。应用必须把 deadlock 当作可重试错误，并保证操作幂等/重试边界。 [MySQL/InnoDB: `LATEST DETECTED DEADLOCK` 提供等待图线索，具体 victim 选择由实现决定]

比喻锚点: 两辆车各自占着一条窄路、车头互相顶住；加长等待时间不会让道路自动变宽，必须让一辆车倒车。 [写作时展开]

### 3. 死锁/MDL诊断 — 从一条日志还原多条等待路径

场景提示: 线上出现 deadlock 或 ALTER 卡住，应该收集哪些证据？ [写作时展开]

关键设计: 诊断要同时看事务、锁对象、SQL、索引访问和时间线：

```[pseudocode]
死锁:
  SHOW ENGINE INNODB STATUS
  → LATEST DETECTED DEADLOCK
  → 事务/线程/SQL/锁类型/等待关系

MDL:
  performance_schema.metadata_locks
  + threads/processlist
  → 持有者/等待者/对象/开始时间

事务:
  活跃事务、开始时间、锁数量、Undo/history

访问路径:
  EXPLAIN/实际 SQL/索引
  → 是否扫描了比预期更大的范围
```

Why: 为什么只看一行“Deadlock found”不够？——**死锁根因在资源获取顺序、范围、索引和事务边界，错误日志只是结果**；MDL 与 InnoDB 行锁也不是同一张锁表，必须区分对象定义锁、记录/间隙锁和内部 mutex。 [MySQL: 5.x/8.0 锁观测视图不同，按目标版本选择 metadata_locks/事务与 InnoDB 状态]

比喻锚点: 排查像看交通事故重播：不仅要知道两车撞了，还要知道谁先占哪条车道、信号灯何时变化、后车为何继续驶入。 [写作时展开]

### 4. 锁优化 — 减少等待不是单纯加并发

场景提示: 订单队列被锁住、事务吞吐下降，应该拆事务、加索引、换隔离级别还是使用 SKIP LOCKED？ [写作时展开]

关键设计: 优化动作要对应锁等待来源：

```[pseudocode]
缩短锁生命周期:
  小事务/尽快提交
  → 减少锁持有时间与死锁窗口

缩小锁范围:
  合适索引/更精确谓词
  → 少扫描/少锁记录和 gap

统一顺序:
  多事务按相同键顺序获取资源
  → 减少循环等待

改变等待语义:
  NOWAIT → 立即报错
  SKIP LOCKED → 跳过已锁行(适合特定队列)

隔离级别:
  RC/RR 选择要结合幻读、当前读和业务正确性
```

Why: 为什么“加索引”可能既改善锁又改变业务结果？——**索引改变扫描路径与锁范围，可能减少锁，也可能让执行计划/访问顺序变化**；RC、SKIP LOCKED、NOWAIT 都改变并发可见性或任务语义，不能只按吞吐率选择。应用仍需处理重试、遗漏、顺序和幂等。 [MySQL: `NOWAIT/SKIP LOCKED` 支持和锁定读语义依赖版本/语句类型]

比喻锚点: 锁优化像仓库发货：缩短办单、按统一货架顺序取货、精准定位货架，或明确“没货就跳过”；每种策略都会改变业务等待和结果。 [写作时展开]

### 5. InnoDB 内部锁与显式锁 — 数据锁之外还有引擎共享结构

场景提示: SQL 行锁不多，但所有线程仍在等待 InnoDB 内部资源，应该看什么？ [写作时展开]

关键设计: 引擎内部 mutex/rw-lock 保护 Buffer Pool、索引、字典等共享结构；显式表锁/用户锁又是另一层：

```[pseudocode]
内部:
  mutex → 临界区互斥
  rw-lock → 并发读/独占写
  spin → 短等待先自旋
  os_wait → 长等待转入操作系统等待

诊断:
  SHOW ENGINE INNODB STATUS
  → SEMAPHORES/内部等待线索
  perf/ftrace → CPU、自旋、调度和调用路径

外部/显式:
  LOCK TABLES
  GET_LOCK
  FLUSH TABLES WITH READ LOCK
  → 与行锁/MDL/内部锁分开理解
```

Why: 为什么 mutex/rw-lock 等待不能用 SQL 行锁监控完全解释？——**它们保护的是引擎内部数据结构，不一定对应某条记录**；高并发下短自旋可能消耗 CPU，长等待才进入 sleep。优化要结合 Buffer Pool、I/O、线程数和内部状态，不能只增加数据库连接。 [MySQL/InnoDB: 内部锁名、状态输出和实现随版本变化]

### 6. 收束

锁问题闭环：

```[pseudocode]
症状: SQL慢/DDL卡/死锁
  → 区分 MDL / 行锁范围 / 内部锁
  → 收集持有者、等待者、SQL、索引、事务时间线
  → 找循环依赖/长事务/大范围扫描/内部热点
  → 缩短事务、统一顺序、缩小范围或改变等待语义
  → 复测吞吐、尾延迟、死锁率和业务正确性
```

**Aha Moment**: "锁排障的第一步不是“调大 timeout”，而是**判断是哪一层锁、谁持有、谁等待、资源获取顺序是什么**；死锁要打破环，MDL 要找到长事务，内部锁要回到引擎结构和调度证据。"
**回答读者三问**: ①DDL 为什么被卡=MDL 等待对象定义锁；②死锁怎么处理=wait-for graph 检测环并回滚 victim，应用重试；③锁优化怎么做=缩短生命周期、缩小范围、统一顺序或明确跳过/立即失败语义。

---

### 核心悬念

**"事务并发已经能诊断；多个 MySQL 实例之间如何把 binlog 变成可靠复制，GTID、半同步和并行回放又如何决定一致性与延迟？"**

→ 引出阶段 5 / MySQL 复制 — binlog、GTID、半同步、并行复制与延迟。