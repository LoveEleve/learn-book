# Performance Schema 与锁监控 — MySQL 运行期到底在等什么、慢在哪里

> Cluster A | 覆盖知识元: 9.1 Performance Schema + 9.2 锁监控 | 依赖: 13 优化器/慢日志、14 sys/Performance Schema 桥接 | 读者基线: EXPLAIN、digest、锁等待、系统性能观测
> 文章定位: MySQL 阶段 9 第一篇；先建立 Performance Schema 采集模型、锁/复制等运行时视图，再把它们桥到 Exporter/Prometheus/Grafana
> 打开新视角: Performance Schema 不是“更多系统表”，而是**可配置的事件采集框架**；监控不是把所有 instrument 打开，而是只保留对当前故障和容量最有价值的事件层

---

### 概念依赖链

```
13 慢日志/EXPLAIN + 14 sys/P_S → 本篇: Performance Schema/锁监控/外部观测
  ├─ §1 instrument/consumer/event(采集模型)
  ├─ §2 digest/threads/复制表(运行期摘要)
  ├─ §3 data_locks/data_lock_waits/死锁(并发观测)
  ├─ §4 Exporter/Prometheus/Grafana(系统外部采集)
  └─ §5 指标选择与开销(监控设计)
先讲: 采集模型 → 摘要对象 → 锁等待 → 指标导出 → 监控策略
后续依赖: 16-connection-pool-proxysql(连接池与中间件指标联动)
```

### 叙事顺序

1. 问题引入——慢日志告诉你哪条 SQL 慢，但运行时到底慢在锁、I/O、临时表还是复制落后，去哪里看？
2. Performance Schema——instrument、consumer、event 三层模型
3. digest、threads、复制视图——把事件聚合成可读状态
4. data_locks/data_lock_waits——把锁等待关系显式化
5. Exporter、Prometheus、Grafana——怎样把这些视图变成持续监控
6. 收束——监控设计不是“全开”

### 1. Performance Schema — 不是日志，而是可配置事件工厂

场景提示: 为什么有些实例 `events_statements_summary_by_digest` 有数据，有些却几乎空白？ [写作时展开]

关键设计: Performance Schema 通过 instrument、consumer 和事件表定义“采什么、留多久、按什么粒度看”：

```[pseudocode]
instrument:
  具体可观测点
  例如语句、等待、锁、文件、socket、stage、transaction

consumer:
  是否把事件写入 current/history/history_long/summary 等表

event tables:
  current → 当前事件
  history → 每线程最近事件
  summary → 按 digest/thread/object 聚合
```

Why: 为什么 Performance Schema 不是“默认永远正确完整的黑盒录像机”？——**没开的 instrument/consumer 不会产生数据，开的越多开销和内存占用越高**；不同版本默认启用项和表结构也会变化。它是一个可配置观测平面，不是无成本审计总线。 [MySQL: Performance Schema 配置、内存占用和事件表语义需按版本与实例配置核对]

比喻锚点: Performance Schema 像一套可配置摄像头和计数器——摄像头没装就没有画面，装满全楼又会增加存储和管理成本。 [写作时展开]

### 2. digest、threads 与复制表 — 运行期对象怎样被聚合

场景提示: 同一类 SQL 执行了几百万次，为什么监控更关心 digest 模板，而不是每条原始语句？ [写作时展开]

关键设计: P_S 把高基数原始事件聚成 digest、线程和复制维度摘要：

```[pseudocode]
statement digest:
  SQL 归一化
  → DIGEST / DIGEST_TEXT
  → 统计 count、total time、rows、tmp table、sort 等

threads:
  连接线程/后台线程的运行时身份
  → 线程属性、当前事件、等待链路

replication tables:
  连接/applier/worker 维度状态
  → 复制线程、错误、延迟、队列、位点/GTID相关观测
```

Why: 为什么 digest 统计不能替代慢日志原文？——**digest 擅长发现高总耗时模板，但会抹平参数差异和个别极端慢请求的原始上下文**；复制视图也只是实例观察窗口，不能单独证明主从数据已完全一致。线程表帮助你把 SQL、锁、I/O 和复制状态连到具体执行实体。 [MySQL: digest 归一化规则、复制表数量和字段在不同版本里存在差异]

比喻锚点: digest 像把十万张相似订单归成一个模板统计总耗时，threads 像查是哪位操作员在处理，复制表像看分仓和运输队的当前状态。 [写作时展开]

### 3. 锁监控 — data_locks 与 data_lock_waits 把等待图显式化

场景提示: 事务在等锁，但 `SHOW PROCESSLIST` 只看到“Waiting for ...”；怎样真正知道谁堵了谁、锁的对象是什么？ [写作时展开]

关键设计: 锁观测要把持有锁对象和等待关系拆成两张图：

```[pseudocode]
data_locks:
  当前持有/申请中的锁对象
  → engine / object / lock_type / lock_mode / lock_data 等

data_lock_waits:
  请求者锁ID ↔ 阻塞者锁ID 关系
  → 可拼成等待链/阻塞图

SHOW ENGINE INNODB STATUS:
  补充 deadlock、semaphores、事务上下文
```

Why: 为什么只看 `LATEST DETECTED DEADLOCK` 不能涵盖所有等待问题？——**死锁日志只记录最近一次被检测并回滚的环，普通长等待、MDL、内部锁热点和历史趋势并不都在那一段里**；`data_locks` 看到的是当前快照，结合线程、事务和 SQL 才能恢复因果。 [MySQL: 8.0 的 `performance_schema.data_locks` 与旧版本视图不同，按目标版本诊断]

比喻锚点: data_locks 像停车场里当前谁占着哪个车位，data_lock_waits 像哪辆车在等哪辆车挪车；死锁日志只是最近一次两车互堵的事故报告。 [写作时展开]

### 4. Exporter、Prometheus 与 Grafana — 把数据库内部视图变成外部时间序列

场景提示: DBA 平时不可能每分钟手动查一次 `performance_schema`；怎样把这些运行态数据持续采集和展示？ [写作时展开]

关键设计: Exporter/Agent 把 MySQL 状态、P_S 摘要和系统指标转成 Prometheus 时间序列，再由 Grafana 展示：

```[pseudocode]
mysqld exporter / agent
  → 周期性查询 status / P_S / replication / InnoDB 视图
  → 输出 metrics endpoint

Prometheus
  → scrape
  → 存储时序/规则/告警

Grafana
  → 仪表板
  → QPS/TPS/延迟/锁等待/Buffer Pool/Redo/复制延迟/连接数
```

Why: 为什么 Exporter 指标不能简单等同于 SQL 诊断真相？——**采样周期会错过瞬时尖峰，聚合会抹平个别事务，collector 还可能因权限/查询成本不完整**；Grafana 图表是长期趋势视图，不替代 EXPLAIN、慢日志和 tracing。指标设计要区分业务 SLI（P99、错误率）与数据库内部健康（锁等待、Redo、Buffer Pool）。 [系统性能: 指标采样、保留粒度和高基数标签都会影响成本与解释]

比喻锚点: Exporter 像把数据库里的多个仪表读数抄到监控平台，Grafana 是总控室大屏；大屏适合看趋势，不适合替代现场取证。 [写作时展开]

### 5. 监控策略 — 九类指标不是全开，而是围绕故障模型选

场景提示: 连接、语句、临时表、I/O、锁、Redo、复制……这么多指标，哪些应该长期保留，哪些在故障时临时打开？ [写作时展开]

关键设计: 监控指标应围绕“会坏什么”和“恢复靠什么”设计：

```[pseudocode]
长期保留(低成本/高价值):
  QPS/TPS/错误率/P95-P99
  连接数/活跃线程
  Buffer Pool 命中/脏页/Redo 速率
  复制健康/延迟
  锁等待聚合/死锁计数

按需打开(高成本/细粒度):
  statement digest 高频表
  wait/stage/file/socket 细表
  大量 history/history_long

原则:
  为故障模型服务
  为容量规划服务
  为回归验证服务
```

Why: 为什么“全量采集最安全”常常是错误选择？——**观测本身消耗 CPU、内存和表空间，海量高基数字段还会把监控系统拖垮**；好的监控设计不是最全，而是能在成本可控下缩短定位时间。 [MySQL: Performance Schema consumer、summary/history 策略要与实例负载和监控目标匹配]

### 6. 收束

监控与可观测性闭环：

```[pseudocode]
慢日志/告警
  → Performance Schema digest/threads/replication
  → data_locks/data_lock_waits/死锁日志
  → Exporter/Prometheus/Grafana 看趋势与告警
  → 必要时回到 EXPLAIN/trace/system metrics
```

**Aha Moment**: "Performance Schema 的价值不在于‘它记录了很多表’，而在于**它把 SQL、线程、锁、I/O、复制这些运行事件放进同一观测平面**；sys 和监控平台只是这张平面的翻译层。"
**回答读者三问**: ①为什么有时 P_S 没数据=instrument/consumer 没开或采样窗口不对；②锁等待看哪里=data_locks + data_lock_waits + 事务/线程；③监控如何设计=长期保留低成本核心指标，细粒度按需打开。

---

### 核心悬念

**"数据库内部的监控和复制都讲清了；连接池和中间件如何在数据库外部塑造并发、故障切换和读写分离？"**

→ 引出 16-connection-pool-proxysql — HikariCP、连接池与 ProxySQL。