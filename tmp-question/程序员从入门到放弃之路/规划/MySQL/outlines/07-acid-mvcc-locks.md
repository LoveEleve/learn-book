# MySQL 事务、MVCC 与行锁 — 并发读写如何既正确又不互相拖死

> Cluster A | 覆盖知识元: 4.1-4.4 | 依赖: 03 Group Commit/Undo/Recovery、05 索引维护 | 读者基线: Redo/Undo、B+Tree、线程并发
> 文章定位: MySQL 阶段 4 第一篇；从 ACID 和隔离级别进入 ReadView/Undo 版本链，再落到 Record/Gap/Next-Key Lock
> 打开新视角: InnoDB 的并发控制不是“全表加锁”或“全靠 MVCC”，而是**一致性读看版本，当前读/写看锁，隔离级别决定两者如何组合**

---

### 概念依赖链

```
03 Undo/Redo/Recovery + 05 索引 → 本篇: 事务/MVCC/行锁
  ├─ §1 ACID/BEGIN/COMMIT/ROLLBACK/SAVEPOINT
  ├─ §2 隔离级别与并发异常
  ├─ §3 MVCC(Undo版本链/ReadView/二级索引回表)
  └─ §4 Record/Gap/Next-Key/Insert Intention Lock
先讲: 事务承诺 → 隔离异常 → 版本读 → 锁与范围
后续依赖: 08-deadlock-mdl-optimization(死锁、MDL与锁优化)
```

### 叙事顺序

1. 问题引入——两个事务同时读写同一行，怎样避免脏读、覆盖更新和幻读？
2. ACID 与事务控制
3. 四种隔离级别与并发异常
4. MVCC/Undo/ReadView
5. 行锁与间隙锁
6. 收束——一致性读与当前读的组合

### 1. ACID 与事务控制 — “提交成功”到底承诺什么

场景提示: 一个事务执行多条 SQL，中途报错后 `ROLLBACK`，哪些修改会一起消失？ [写作时展开]

关键设计: ACID 是四类约束，不是四个独立开关：

```[pseudocode]
BEGIN/autocommit=0
  → 事务开始

执行:
  约束/触发器/锁/MVCC/Undo/Redo 共同参与

COMMIT:
  → 原子提交边界与持久化配置共同决定结果

ROLLBACK:
  → 使用 Undo 撤销未提交逻辑修改

SAVEPOINT:
  → 回滚到事务内部标记, 不是结束整个事务

隐式提交:
  → 某些 DDL/控制语句可能结束当前事务
  → 需按 MySQL 版本和语句文档核对
```

Why: 为什么 Durability 不能简单理解成“COMMIT 返回就物理盘已写完”？——**提交持久性受 Redo、binlog、flush 配置、文件系统和设备边界共同影响**；Atomicity 也不是只靠 Undo，还涉及锁、日志和提交协议。应用必须明确自己的故障模型和持久化要求。 [MySQL: `innodb_flush_log_at_trx_commit`/`sync_binlog` 等配置会改变提交等待边界]

比喻锚点: 事务像一份合同：原子性是签约要么整份成立要么作废，一致性是条款不违反规则，隔离性是谈判过程互不偷看，持久性是签完后档案可恢复。 [写作时展开]

### 2. 隔离级别 — 脏读、不可重复读与幻读的取舍

场景提示: 同一事务两次查询同一条件，为什么可能得到不同结果或看到新插入行？ [写作时展开]

关键设计: 隔离级别控制并发事务的可见性和等待/锁代价：

```[pseudocode]
READ UNCOMMITTED
  允许读取未提交修改 → 脏读风险

READ COMMITTED
  一致性读通常按语句建立视图
  → 可能不可重复读/范围结果变化

REPEATABLE READ
  InnoDB 常见默认级别
  → 事务内一致性读使用相应 ReadView
  → 当前读/锁仍可能观察并发变化

SERIALIZABLE
  更强隔离, 更多阻塞/锁约束
```

Why: 为什么隔离级别不能只背“能否脏读/幻读”的表格？——**一致性读、当前读、锁定读和具体数据库实现共同决定观察结果**；InnoDB 用 MVCC 减少普通读阻塞，也用锁处理写与当前读。隔离更强通常意味着并发成本更高，具体幻读表现要用实际 SQL 和事务时序验证。 [MySQL: 隔离级别语义、consistent read 与 locking read 需要区分]

比喻锚点: 隔离级别像会议室透明度：UNCOMMITTED 能看到草稿，RC 每轮拿新快照，RR 保持事务视图，SERIALIZABLE 则让更多人排队进入。 [写作时展开]

### 3. MVCC — Undo 版本链与 ReadView 如何完成一致性读

场景提示: 一个事务修改了记录，另一个事务不想被阻塞，还要读到符合隔离规则的旧值；旧值从哪里来？ [写作时展开]

关键设计: InnoDB 通过记录隐藏事务信息、Undo 版本链和 ReadView 判断可见性：

```[pseudocode]
当前聚簇记录:
  trx_id + roll_pointer
  → 指向 Undo 历史版本

ReadView:
  creator_trx_id
  m_low_limit_id / m_up_limit_id
  活跃事务列表

可见性:
  当前版本事务已提交且满足视图边界?
    是 → 读取当前版本
    否 → 沿 roll_pointer 查旧版本

二级索引:
  先定位 secondary entry
  → 必要时回表到 clustered record
  → 在聚簇记录/版本链上判断可见性
```

Why: 为什么 MVCC 不能完全替代锁？——**一致性读适合读取某个视图，写入、当前读、唯一性检查和范围保护仍需要锁**；版本链也不是无限保留，Purge 会在最老 ReadView 不再需要后清理。二级索引记录的可见性与回表路径也不能简单说成“二级索引自己判断全部版本”。 [MySQL/InnoDB: ReadView、roll pointer、Undo 和 Purge 共同决定版本生命周期]

比喻锚点: MVCC 像图书馆保存多个版本的书：读者拿到自己的版本视图，不必让作者停笔；但真正改书、预约唯一页码或封锁书架仍要锁。 [写作时展开]

### 4. Record、Gap、Next-Key 与 Insert Intention Lock

场景提示: `SELECT ... FOR UPDATE WHERE id BETWEEN 10 AND 20` 为什么可能阻塞另一个事务插入 15，即使 15 这行原本不存在？ [写作时展开]

关键设计: InnoDB 锁既保护已有索引记录，也可能保护索引间隙：

```[pseudocode]
Record Lock:
  锁定已有索引记录

Gap Lock:
  锁定索引记录之间的间隙
  → 防止范围内插入形成幻读

Next-Key Lock:
  record lock + 前方 gap 的组合
  → 保护索引范围

Insert Intention:
  插入前声明意图
  → 多个事务插入不同位置通常可并行
  → 仍会与冲突的 gap/record 约束协调

锁对象:
  以索引访问路径为基础
  → 索引选择、范围条件和隔离级别影响锁范围
```

Why: 为什么“查一行”也可能锁住一段范围？——**在 RR 等场景，锁定读不仅要保护当前记录，还要防止符合谓词的新记录插入**；锁范围由索引、扫描条件、匹配方式和隔离级别决定。没有合适索引时，扫描范围和锁影响可能扩大。 [MySQL/InnoDB: Record/Gap/Next-Key 具体兼容矩阵和 locking read 以目标版本手册/锁监控为准]

比喻锚点: Record Lock 是给已有房间上锁，Gap Lock 是把两栋房之间的空地也围起来，Next-Key 是房间和门前空地一起封锁；Insert Intention 是提前登记“我要在这块空地建房”。 [写作时展开]

### 5. 收束

并发控制闭环：

```[pseudocode]
事务控制
  → ACID/隔离级别
  → 一致性读: ReadView + Undo 版本链
  → 当前读/写: Record/Gap/Next-Key 等锁
  → 提交: Redo/binlog 协调
  → 老版本: Purge 等安全边界后清理
```

**Aha Moment**: "InnoDB 并发控制不是“MVCC 或锁”二选一，而是**普通读用版本视图减少阻塞，当前读/写用索引锁保护未来变化，隔离级别决定两者的边界和成本**。"
**回答读者三问**: ①MVCC 为什么不阻塞普通读=读取 Undo 历史版本；②Gap Lock 为什么锁不存在的行=保护范围不被插入改变；③索引为什么影响锁=锁范围沿实际索引访问路径建立。

---

### 核心悬念

**"事务能读写了，但两个事务按不同顺序拿锁就会互相等待；MDL、死锁检测和 SKIP LOCKED/NOWAIT 如何把锁等待变成可诊断、可控制的行为？"**

→ 引出 08-deadlock-mdl-optimization — 死锁、MDL、锁优化与内部并发。