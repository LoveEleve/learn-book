# Group Commit、Undo 与崩溃恢复 — MySQL 如何同时保证提交、回滚和 MVCC

> Cluster A | 覆盖知识元: 2.3 Group Commit + 2.4 Undo/MVCC + 2.5 Crash Recovery + 2.6 Purge | 依赖: 01 架构、02 Redo/LSN/Checkpoint | 读者基线: WAL、LSN、Buffer Pool
> 读者处境: 02 篇已经说明 Redo 能在崩溃后重放已记录变化；本篇回答事务更新到一半时如何回滚、binlog 与 InnoDB 如何协调，以及旧版本何时可以安全清理
> 打开新视角: 事务持久化有三条不同时间线——**Redo 保证引擎页可恢复，Undo 保证回滚/MVCC，Group Commit 协调 binlog 与引擎提交，Purge 决定历史版本何时消失**

---

### 概念依赖链

```
01 InnoDB架构 + 02 Redo/LSN/Checkpoint → 本篇: 提交/回滚/恢复
  ├─ §1 Group Commit(binlog与引擎提交)
  ├─ §2 Undo Log/回滚段/MVCC版本链
  ├─ §3 Crash Recovery(Redo→未提交事务处理)
  └─ §4 Purge(ReadView安全边界→物理清理)
先讲: 提交协调 → Undo写入 → 崩溃恢复 → 旧版本清理
后续依赖: 阶段3 索引/阶段4 事务并发(索引页与 ReadView 更深入)
```

### 叙事顺序

1. 问题引入——一个事务修改了 InnoDB 页、写了 binlog，恰好在提交中途断电，重启后如何判断它算成功还是失败？
2. Group Commit——FLUSH/SYNC/COMMIT 如何协调两套日志
3. Undo——未提交修改如何回滚，已提交旧版本如何服务 MVCC
4. Crash Recovery——从 Checkpoint 重放 Redo，再处理未完成事务
5. Purge——为什么不能立刻删除所有旧版本
6. 收束——提交、回滚、可见性和清理的时间线

### 1. Group Commit — Redo、binlog 与提交边界

场景提示: 多个事务同时提交，为什么 MySQL 不让每个事务独自 fsync 一次？ [写作时展开]

关键设计: Group Commit 把相邻提交的日志写入和同步阶段合并，降低 fsync 次数；具体内部阶段随 MySQL 版本、binlog/引擎配置变化：

```[pseudocode]
事务执行:
  InnoDB 修改页/生成 Redo
  → 事务 Event 写入 binlog cache

Group Commit(概念阶段):
  FLUSH:
    多个事务的 binlog cache → binlog 文件
  SYNC:
    按配置把 binlog 文件同步到持久化边界
  COMMIT:
    引擎侧完成事务提交/释放相关资源

结果:
  多事务共享一次或较少的同步成本
  → 提高吞吐, 但提交延迟/持久性取决于配置
```

Why: 为什么 binlog 和 InnoDB Redo 要协调，而不是只相信其中一个？——**binlog 服务复制/恢复逻辑，Redo 服务 InnoDB 页恢复**；两者提交顺序不一致可能造成“主库提交了但复制日志没有”或“复制看到了但引擎状态不一致”。内部 XA/两阶段协调的细节依赖 MySQL 版本与配置，不能把简化三阶段图当作所有实现的源码流程。 [MySQL: `sync_binlog`、`innodb_flush_log_at_trx_commit` 等参数共同影响提交边界]

比喻锚点: Group Commit 像一辆班车集中把多份快递送到银行盖章；每单单独跑一趟浪费时间，合并批次降低固定过路费。 [写作时展开]

### 2. Undo Log — 回滚、MVCC 与历史版本

场景提示: 事务把一行从旧值改成新值，但另一个一致性读还要看到旧值；旧版本保存在哪里？ [写作时展开]

关键设计: Undo 同时承担“未提交事务回滚”和“已提交历史版本供 MVCC 读取”两类职责：

```[pseudocode]
INSERT:
  Undo 记录如何撤销插入

DELETE:
  Undo 保留恢复/可见性所需的旧信息

UPDATE:
  Undo 保存前镜像/版本链必要字段

记录关系:
  聚簇记录中的 roll pointer
  → 指向 Undo 版本
  → 旧版本可能继续向前链接

读取:
  ReadView 判断当前版本是否可见
  → 不可见时沿 Undo 版本链构造历史视图
```

Why: 为什么 Undo 不能在事务提交后立刻删除？——**因为仍有活跃 ReadView 可能需要它构造一致性读**；Undo 不是简单“失败事务临时文件”，而是事务回滚和 MVCC 历史的共同基础。回滚段、Undo 页、Undo 表空间的具体布局依赖版本和配置。 [MySQL/InnoDB: ReadView 与 Purge 的安全边界决定历史版本生命周期]

比喻锚点: Undo 像编辑文档的版本历史：未提交时可以撤销，提交后旧版本还可能供正在阅读旧快照的人查看。 [写作时展开]

### 3. Crash Recovery — 重放已记录变化，处理未完成事务

场景提示: 机器在事务提交或数据页刷盘中途断电，mysqld 重启时如何从日志恢复一个可用状态？ [写作时展开]

关键设计: 恢复以最后可靠 Checkpoint 为起点，读取后续 Redo，把引擎页恢复到日志描述的状态，再处理未完成事务；binlog 协调是更高层的事务一致性问题：

```[pseudocode]
启动
  → 读取检查点/日志元信息
  → 从 checkpoint 之后扫描有效 Redo
  → 重放需要的页修改
  → 确定恢复范围/结束位置
  → 处理崩溃时未完成事务
      使用 Undo/事务状态回滚其逻辑效果
  → 启动后台 Purge/刷脏等线程
  → 对外提供服务
```

Why: 为什么恢复不是简单“Redo 后把所有事务 Undo 一遍”？——**Redo 恢复的是引擎页到崩溃前可重建状态，哪些事务已提交/未提交还要结合事务状态、日志和提交协议判断**；已提交事务不能被错误回滚，未提交事务不能留下可见效果。复制/binlog/内部 XA 的恢复还需要单独核对对应日志和版本语义。 [MySQL: 崩溃恢复阶段、Redo 扫描范围和事务回滚细节随版本实现变化]

比喻锚点: 灾后重建先按施工记录恢复楼体，再查看哪些工程单已封签、哪些未验收；未验收工程要撤回，不能把所有新墙都拆掉。 [写作时展开]

### 4. Purge — 历史版本什么时候能真正消失

场景提示: 事务已经提交，为什么旧版本和 delete-marked 记录还可能存在？谁决定它们何时物理清理？ [写作时展开]

关键设计: Purge 只能清理不再被任何活跃 ReadView 需要的历史版本：

```[pseudocode]
事务 UPDATE/DELETE
  → 产生 Undo/旧版本或 delete mark

活跃 ReadView
  → 仍可能需要旧版本
  → Purge 必须等待

最老安全边界推进
  → purge 读取 Undo history
  → 清理过期版本
  → 将可物理删除的 delete-marked 记录处理掉
  → 回收 Undo 空间/相关索引资源
```

Why: 为什么长事务会导致 Undo/history 持续增长？——**长事务的 ReadView 可能让旧版本仍然可见，Purge 不能越过它清理**；因此“事务提交了”不等于“历史马上消失”。排查要看长事务、history list、Undo 表空间和 Purge 进度，而不是只看当前 TPS。 [内核: 这与阶段 6 eBPF/性能观测的“生命周期窗口”同构——资源是否能回收取决于最老仍需它的读者]

比喻锚点: Purge 像档案管理员：只要还有人拿着旧版文件在审核，就不能销毁历史副本；最后一个读者离开后才可碎纸。 [写作时展开]

### 5. 收束

事务持久化四条时间线：

```[pseudocode]
执行:
  修改 Buffer Pool + 生成 Redo + 生成 Undo + 写 binlog cache

提交:
  Group Commit 协调 binlog/Redo/引擎提交边界

崩溃:
  Checkpoint 后重放 Redo
  → 识别未完成事务并处理回滚

运行中:
  ReadView 保留旧版本需求
  → Purge 等安全边界推进后清理 Undo/旧记录
```

**Aha Moment**: "MySQL 的事务不是一个瞬间，而是四条时间线：**Redo 保证页可恢复，Undo 保证回滚和 MVCC，Group Commit 协调持久化日志，Purge 等最后一个旧版本读者离开后再清理**。"
**回答读者三问**: ①Group Commit 优化什么=合并日志同步固定成本；②Undo 为什么提交后仍存在=MVCC ReadView 可能需要旧版本；③恢复先做什么=从 Checkpoint 后重放有效 Redo，再处理未完成事务。

---

### 核心悬念

**"事务恢复和版本链已经建立；InnoDB 怎样把一行记录组织进 B+Tree 页，聚簇索引、二级索引和回表又如何决定查询成本？"**

→ 引出阶段 3 / B+Tree 索引与 InnoDB 数据页。