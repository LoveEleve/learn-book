# FULLTEXT、统计信息与 Online DDL — 索引的语义、估算和生命周期

> Cluster A | 覆盖知识元: 3.7-3.10 | 依赖: 05-index-maintenance-strategy | 读者基线: B+Tree、索引策略、EXPLAIN、事务/MDL
> 读者处境: 05 篇讲了普通 B+Tree 的读写成本；本篇回答三类更复杂问题：全文搜索为何不是普通 B+Tree、优化器如何估算选择性、在线 DDL 如何改变索引而不简单停机
> 打开新视角: 索引系统同时有**查询结构、统计模型和变更工程**三层；结构能查，不代表优化器会选，DDL 能执行，也不代表没有锁/复制/资源代价

---

### 概念依赖链

```
05 索引维护/策略 → 本篇: FULLTEXT/统计信息/Online DDL
  ├─ §1 FULLTEXT(倒排索引与分词)
  ├─ §2 统计信息/ANALYZE(基数/页数/行数)
  ├─ §3 Histogram(列分布与优化器估算)
  ├─ §4 Online DDL(INSTANT/INPLACE/COPY/MDL)
  └─ §5 外部工具与验证(pt-osc/EXPLAIN/监控)
先讲: 另一种索引 → 估算数据 → 列分布 → 结构变更 → 上线验证
后续依赖: 阶段4 事务与并发控制(锁、MVCC、DDL/事务边界)
```

### 叙事顺序

1. 问题引入——普通索引查不到“包含哪些词”，优化器又可能估错行数；索引结构、统计和 DDL 如何配合？
2. FULLTEXT——倒排索引而非 B+Tree 等值导航
3. 统计信息与 Histogram——优化器如何估算
4. Online DDL——索引变更如何在线完成
5. MDL/pt-online-schema-change——上线风险与验证
6. 收束

### 1. FULLTEXT — 从键查找转向词到文档集合

场景提示: `WHERE content LIKE '%database%'` 为什么难以利用普通 B+Tree，全文索引又解决了什么？ [写作时展开]

关键设计: FULLTEXT 使用分词/词项与倒排结构，把词映射到文档/记录集合，再计算匹配相关性：

```[pseudocode]
建立:
  文本列
  → tokenizer/ngram/语言规则
  → term → document/row postings

MATCH ... AGAINST
  → 查询词分解
  → 查倒排列表
  → 合并/过滤候选
  → natural language/boolean 等模式计算结果

中文/日文/韩文:
  → 分词或 ngram 策略影响索引大小与召回
```

Why: 为什么全文索引不能简单当作“更快的 LIKE”？——**它改变了查询语义和建索引方式**：分词、停用词、最小词长、相关性和布尔模式都会影响结果；更新文本还要维护倒排结构。全文功能、解析器和存储引擎支持要按 MySQL 版本/配置核对。 [MySQL: FULLTEXT parser、ngram/MeCab、natural language/boolean 模式具有版本和语言配置边界]

比喻锚点: B+Tree 像按身份证号查档案，倒排索引像书后的索引页：先找词，再列出出现它的章节。 [写作时展开]

### 2. 统计信息与 ANALYZE — 优化器为什么需要估算

场景提示: 两个索引都能用，优化器怎样判断哪个扫描行数更少、代价更低？ [写作时展开]

关键设计: 优化器依赖表/索引统计信息估算基数、页数、行数和选择性：

```[pseudocode]
统计信息可能包含:
  table rows / index pages / distinct values
  → 估算谓词选择性与访问代价

ANALYZE TABLE
  → 重新采样/计算统计信息
  → 更新优化器可见的统计

计划选择:
  候选索引/连接顺序/访问方式
  → 统计估算 + 成本模型
  → 选择执行计划
```

Why: 为什么同一条 SQL 偶尔会突然换计划？——**数据分布、统计信息陈旧、采样误差、参数值和成本模型都会改变估算**；`ANALYZE TABLE` 可能改善估算，也可能因采样变化导致计划变化。统计信息表不是用户可随意当作实时精确计数器的业务表。 [MySQL: `innodb_table_stats`/`innodb_index_stats` 与自动统计更新行为按版本和配置核对]

比喻锚点: 优化器像导航软件：统计信息是道路拥堵模型，模型过期时就可能选择看似更近、实际更堵的路线。 [写作时展开]

### 3. Histogram — 当索引统计只有平均值不够

场景提示: 一列 99% 的值相同、1% 的值稀有，平均基数为什么会误导优化器？ [写作时展开]

关键设计: Histogram 描述列值分布，补充传统索引统计对偏斜数据的表达能力：

```[pseudocode]
列分布:
  统计值域/频率
  → singleton/equi-height 等分桶方式

查询谓词:
  WHERE status = rare_value
  → histogram 估算更接近真实选择性

边界:
  统计有采样/更新成本
  只对适用谓词和优化器路径有帮助
  不是建立了索引, 也不是每条查询必然使用
```

Why: 为什么有 Histogram 仍不保证计划正确？——**它是估算模型，不是逐行实时统计**：采样、分桶、相关列、参数化查询和成本模型仍可能造成误差；Histogram 也不能替代合适的索引或解决所有 join 相关性。 [MySQL: Histogram 创建、更新和支持的谓词以目标版本 Optimizer 文档为准]

比喻锚点: 只知道全班平均身高无法判断“矮于 160cm 的人数”，Histogram 则把全班按身高区间分桶，但分桶仍是近似。 [写作时展开]

### 4. Online DDL — INSTANT、INPLACE、COPY 不是“无锁/无代价”

场景提示: 线上表要加列或建索引，如何避免长时间停写？ [写作时展开]

关键设计: MySQL DDL 算法决定是否重建数据、是否需要长时间元数据锁和资源成本：

```[pseudocode]
INSTANT:
  主要修改元数据
  → 通常不重写整表
  → 支持范围/版本/操作有限

INPLACE:
  尽量在引擎内完成
  → 可能后台重建/扫描/占用 I/O
  → 仍可能有 MDL 阻塞或短暂切换

COPY:
  创建临时/新表并复制数据
  → 成本高、空间/锁/复制影响明显

DDL 生命周期:
  获取 MDL
  → prepare/build/catch-up
  → commit metadata
  → 释放/升级锁
```

Why: 为什么 Online DDL 不等于“完全不阻塞”？——**元数据锁、长事务、最后切换、复制、磁盘空间和后台 I/O 仍会影响业务**；`INSTANT` 也只对支持的操作/版本成立，算法选择必须查看 `ALGORITHM/LOCK` 实际结果。 [MySQL: Online DDL 支持矩阵按版本、表结构和操作类型核对]

比喻锚点: INSTANT 像在目录上加一条说明，INPLACE 像营业中换货架，COPY 像开新店搬完整库存；都可能在交接时需要短暂封门。 [写作时展开]

### 5. MDL 与 pt-online-schema-change — 把上线风险变成可观测流程

场景提示: DDL 看起来在线，业务却突然卡住；怎样定位是 MDL、复制、空间还是触发器开销？ [写作时展开]

关键设计: 线上 schema 变更必须同时观测锁、进度、资源和回滚条件：

```[pseudocode]
MDL:
  DDL 等待已有事务释放元数据锁
  长事务/空闲事务可能成为阻塞源
  → performance_schema 等视图/锁信息定位

pt-online-schema-change(一种外部方案):
  创建影子表
  → 触发器同步增量变更
  → 分批复制历史数据
  → 短暂 rename 切换
  → 清理旧表

上线检查:
  空间/IO/锁/复制延迟/触发器失败
  → 小流量、限速、暂停和回滚策略
```

Why: 为什么 pt-online-schema-change 也不是免费在线？——**触发器会增加写路径，分批复制会争用 I/O，最终 rename 仍需要 MDL，复制拓扑和外键也增加复杂度**；它适合某些限制条件，不应替代原生 Online DDL 的能力评估。 [MySQL: `performance_schema.metadata_locks`、长事务和复制状态是 DDL 排障的重要证据]

比喻锚点: 影子表方案像营业中搭建新仓库：旧仓库继续接单，触发器同步新货，最后切换门牌；同步过程中任何一环都要有暂停和回滚。 [写作时展开]

### 6. 收束

索引生命周期闭环：

```[pseudocode]
查询结构:
  B+Tree/FULLTEXT

优化器:
  统计/Histogram → 估算计划

结构变更:
  INSTANT/INPLACE/COPY/外部影子表

上线验证:
  EXPLAIN + latency + I/O + MDL + replication + rollback
```

**Aha Moment**: "索引系统不止是“建一棵树”：**FULLTEXT 决定搜索语义，统计决定优化器估算，DDL 决定结构如何在线演进，MDL/复制/资源监控决定变更能否安全上线**。"
**回答读者三问**: ①全文索引为什么不同=词项到文档的倒排语义；②统计为什么会错=采样/分布/相关性/成本模型；③Online DDL 是否无锁=算法支持和最终 MDL/资源边界仍需验证。

---

### 核心悬念

**"架构、Redo、Undo、索引和 DDL 都准备好了；事务怎样用 MVCC、锁和隔离级别让并发读写既正确又不互相拖死？"**

→ 引出阶段 4 / 事务与并发控制 — ACID、MVCC、锁、隔离级别与死锁。