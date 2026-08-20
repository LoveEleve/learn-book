# 子查询优化、sys Schema 与 Performance Schema — 从 SQL 形态到运行期证据

> Cluster A | 覆盖知识元: 8.4-8.6 | 依赖: 13 优化器/EXPLAIN/慢日志 | 读者基线: 访问方法、执行计划、慢日志、锁/I/O 基础
> 文章定位: MySQL 阶段 8 的第二篇，并为阶段 9 可观测性开篇；回答子查询怎样被重写，sys/Performance Schema 又如何把计划、等待、I/O 和线程拼到一起
> 打开新视角: 查询优化不是只看 B+Tree 和 EXPLAIN，**子查询重写决定语义与可执行计划，sys Schema/Performance Schema 决定你能否在线验证这些计划在运行时的真实成本**

---

### 概念依赖链

```
13 优化器/EXPLAIN/慢日志 → 本篇: 子查询重写 + 运行期观测
  ├─ §1 子查询 → EXISTS/Semijoin/Materialization/Join
  ├─ §2 sys Schema(面向运维的摘要视图)
  ├─ §3 information_schema 与 metadata/lock 视角
  └─ §4 Performance Schema(采集模型桥接)
先讲: 计划重写 → 运维视图 → 元数据视图 → P_S 事件采集
后续依赖: 15-performance-schema-observability(运行期事件采集)
```

### 叙事顺序

1. 问题引入——慢 SQL 不一定只是缺索引，`IN`、子查询、锁等待和元数据争用可能一起把执行计划拖偏；怎样把“SQL 形态”和“运行期证据”接上？
2. 子查询重写——优化器如何把表达式变成更可执行的形态
3. sys Schema——把底层摘要表翻译成可读视图
4. information_schema / 锁元数据——对象和锁从哪看
5. Performance Schema——采集模型与阶段 9 的桥
6. 收束——从 SQL 结构到运行期证据链

### 1. 子查询优化 — 语法形态不是执行形态

场景提示: `WHERE x IN (SELECT ...)`、相关子查询和派生表为什么有时很慢，有时又会被优化成几乎等价的 JOIN？ [写作时展开]

关键设计: 优化器可能对语义等价的子查询做重写、半连接、物化或保留原形，具体取决于谓词、相关性和成本：

```[pseudocode]
子查询候选策略:
  IN → EXISTS / semi-join 变换
  materialization → 先执行内层并缓存结果
  derived/CTE merge or materialize
  rewrite to join/window/aggregation when semantically valid

目标:
  减少重复执行
  利用索引和 join 顺序
  控制中间结果大小
```

Why: 为什么“手工改成 JOIN”不一定总比子查询快？——**优化器可能已经做了等价重写，或者你的手工改写改变了重复值、NULL 语义、过滤位置和结果集大小**；materialization 也不是坏词，有时正是避免反复执行内层子查询的关键。调优前先看计划和实际行数，而不是只凭 SQL 形态判断。 [MySQL: semi-join、materialization、derived merge 的支持与行为依赖版本、开关和查询语义]

比喻锚点: 子查询像先问名单再筛人，也可能被重写成“边走边比对”的联查；表面写法不同，不代表后台一定按字面顺序执行。 [写作时展开]

### 2. sys Schema — 给 Performance Schema 和信息模式做“翻译”

场景提示: Performance Schema 表很多、字段很多，线上定位最耗时 SQL/锁等待时，为什么运维先看 sys Schema？ [写作时展开]

关键设计: sys Schema 把底层摘要表整理成更接近诊断任务的视图：

```[pseudocode]
sys Schema 视图(示意):
  statement_analysis
  → 归一化 SQL 的执行次数、总耗时、平均耗时、扫描行数等

innodb_lock_waits
  → 等待事务、阻塞事务、锁对象、等待时长

schema_unused_indexes / schema_redundant_indexes
  → 帮助发现未用或重叠索引
```

Why: 为什么 sys Schema 不是“新的监控系统”，而是“友好视图层”？——**它本质上建立在 Performance Schema、information_schema 和系统元数据之上**：如果底层采集没开、时间窗口不够、样本被清空或负载模型不匹配，sys 视图也不会自动变真。它适合诊断入口，不替代你理解底层表和采集开销。 [MySQL: sys 视图内容和列依赖版本与是否启用对应的 Performance Schema consumer]

比喻锚点: sys Schema 像给复杂原始仪表盘做了“值班员面板”，把最常用的告警信息先整理出来，但背后仍是同一套传感器。 [写作时展开]

### 3. information_schema 与锁/元数据视角 — 不是所有信息都来自同一层

场景提示: 表结构、索引定义、元数据锁、事务状态和 InnoDB 内部信息，为什么分散在不同系统视图里？ [写作时展开]

关键设计: information_schema 暴露对象定义和部分状态，锁/事务信息又可能来自 InnoDB 视图或 Performance Schema：

```[pseudocode]
information_schema:
  TABLES / COLUMNS / STATISTICS / KEY_COLUMN_USAGE
  → 定义/结构类信息

锁/事务/元数据:
  metadata_locks / innodb_trx / lock waits 等
  → 具体来源和可用性随版本演进

用途:
  先确认对象定义/索引布局
  再定位元数据锁、事务等待和 DDL 阻塞
```

Why: 为什么不能把 information_schema 当成“实时系统状态总线”？——**很多视图是元数据/逻辑视图，不是为高频运行时监控设计的**；某些锁/事务视图还随版本从 information_schema 迁移到 performance_schema 或 sys。用对入口比背表名更重要。 [MySQL: 元数据、锁和事务视图在 5.6/5.7/8.0 的位置和字段有明显差异]

比喻锚点: information_schema 像建筑蓝图和产权册，Performance Schema 更像实时监控大屏；两者都重要，但不能用蓝图替代实时摄像头。 [写作时展开]

### 4. Performance Schema — 运行时观测模型，而不是“又一堆系统表”

场景提示: 想知道某条 SQL 的锁等待、文件 I/O、线程状态和阶段耗时，为什么最终都绕不开 Performance Schema？ [写作时展开]

关键设计: Performance Schema 通过 instrument、consumer 和 event 表决定采集什么、保留多久、如何聚合：

```[pseudocode]
instrument:
  定义可观测点(等待、阶段、语句、文件、socket、mutex...)

consumer:
  控制采集到当前、历史、汇总等表

event tables:
  current / history / summary by digest/thread/event name ...

线程/连接:
  threads 表连接到会话与采集配置
```

Why: 为什么开了 Performance Schema 还可能“什么都看不到”或“开销突然增大”？——**采集点和 consumer 没开启就没有数据，开启过多又会增加内存和运行时成本**；它适合带着目标打开，而不是全量长期无差别采集。Performance Schema 给的是运行时事件证据，不直接告诉你应该怎么改 SQL。 [MySQL: instrument/consumer 粒度、默认开启项和事件保留策略需按版本核对]

比喻锚点: Performance Schema 像一套可配置传感器系统——传感器没装就没有数据，装满全楼又会增加管理成本；应先决定想看哪类故障。 [写作时展开]

### 5. 收束

SQL 诊断闭环：

```[pseudocode]
慢 SQL/异常模板
  → 看子查询/CTE/派生表是否被重写或物化
  → EXPLAIN/ANALYZE 验证计划与行数
  → sys Schema 找高耗时/高锁等待模板
  → information_schema / metadata_locks 看对象与锁
  → Performance Schema 下钻线程、等待、I/O、阶段
```

**Aha Moment**: "优化器决定‘怎么跑’，Performance Schema/sysschema 决定‘跑的时候发生了什么’；**没有前者，你不知道为什么选这个计划；没有后者，你不知道这个计划在线上究竟慢在哪**。"
**回答读者三问**: ①子查询为什么不能只看语法=优化器可能重写/物化/半连接化；②sys Schema 做什么=把底层运行时摘要翻译成诊断视图；③Performance Schema 为什么重要=它是运行期等待、I/O、线程和语句事件的统一采集框架。

---

### 核心悬念

**"查询优化和运行期观测已经接上了；真正线上最常用的 Top SQL、锁等待、文件 I/O、Digest 汇总要怎么系统化采集与展示？"**

→ 引出阶段 9 / Performance Schema、慢日志与监控可观测性深化。