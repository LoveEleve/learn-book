# Redo Log、WAL 与 LSN — MySQL 如何先记账，再安全地刷数据页

> Cluster A | 覆盖知识元: 2.1 Redo/WAL + 2.2 LSN/Checkpoint | 依赖: 01-server-innodb-architecture | 读者基线: Buffer Pool、脏页、InnoDB 后台线程
> 读者处境: 01 篇已经知道 UPDATE 先改 Buffer Pool 中的页；本篇回答“数据页还没落盘时，崩溃为什么不会让已提交事务消失？”
> 打开新视角: InnoDB 持久化不是“每次写都直接刷数据页”，而是**Redo 记录变化、LSN 标记进度、Checkpoint 控制安全回收窗口**

---

### 概念依赖链

```
01 Server/InnoDB/Buffer Pool → 本篇: Redo/WAL/LSN/Checkpoint
  ├─ §1 WAL 与 Redo Log(数据页先写日志)
  ├─ §2 Log Buffer/MTR/日志记录格式(变化如何编码)
  ├─ §3 LSN(日志位置与脏页进度)
  └─ §4 Checkpoint(刷脏、恢复起点、日志空间回收)
先讲: 为什么需要 WAL → 日志如何产生 → LSN 如何排序 → Checkpoint 如何推进
后续依赖: 03-group-commit-undo-recovery(组提交、Undo、崩溃恢复)
```

### 叙事顺序

1. 问题引入——UPDATE 已经返回，但数据页仍在 Buffer Pool；断电后 MySQL 靠什么重建这次修改？
2. WAL/Redo——先记录变化，再允许数据页晚刷
3. Log Buffer 与 MTR——一次页修改如何编码成日志
4. LSN——日志和脏页如何建立进度关系
5. Checkpoint——为什么日志不能无限增长
6. 收束——从修改到恢复起点

### 1. WAL 与 Redo — 数据页可以晚写，变化记录不能丢

场景提示: 修改一行数据时，为什么不用同步把整张 16KB 页写回磁盘？ [写作时展开]

关键设计: Write-Ahead Logging 要求相关日志先达到持久化边界，数据页可以之后再刷：

```[pseudocode]
事务修改页
  → Buffer Pool 中页变脏
  → 生成 Redo 记录“如何重建这次页修改”
  → Redo 进入 Log Buffer/日志文件
  → 达到提交/刷盘条件
  → 数据页后续由后台线程写入表空间

崩溃恢复:
  checkpoint 之后扫描 Redo
  → 把日志变化重新应用到数据页
```

Why: 为什么日志通常比数据页更适合先写？——**日志记录变化通常比随机刷完整数据页小，且可以顺序追加**；数据页继续留在 Buffer Pool，后台批量刷脏，兼顾提交延迟和 I/O 效率。WAL 也不等于每次 COMMIT 都保证硬件断电后的绝对持久化，具体由配置、文件系统和设备语义共同决定。 [内核: InnoDB Redo 是引擎层 WAL，和 Linux Page Cache/writeback 是两层不同日志/缓存机制]

比喻锚点: Redo 像先写施工变更单，数据页像现场墙面；停电后按变更单补做现场，而不是每改一颗螺丝就整面墙重刷。 [写作时展开]

### 2. Log Buffer、MTR 与 Redo 记录 — 一次修改怎样进入日志

场景提示: Buffer Pool 里改了页上的字段，Redo Log 究竟记录整页，还是记录更小的变化？ [写作时展开]

关键设计: InnoDB 用日志记录描述页内变化，Mini-Transaction 把相关的底层修改组织成一个原子日志组：

```[pseudocode]
事务/页修改
  → mtr_start()
  → 修改页/持有页 latch
  → 写入 MLOG_* 类型的 Redo 记录
      page identity + offset + operation/data
  → mtr_commit()
  → Redo bytes 进入 Log Buffer
  → 后续由日志线程/刷盘路径推进
```

Why: 为什么需要 MTR，而不是每个字段修改单独写一条无关系日志？——**底层页操作往往需要多条记录共同表达一个一致的内部变化**；MTR 把相关日志作为不可任意拆开的最小组，恢复时按组语义重放。Redo 格式可能包含物理/逻辑成分，不能简化成统一“完整页副本”。 [MySQL/InnoDB: MTR、MLOG 类型和日志格式依赖具体版本源码；本篇只建立机制地图]

比喻锚点: MTR 像一张装配工单里的连续步骤；只执行前半张工单，机器状态可能没有意义，所以日志要把这组步骤绑定起来。 [写作时展开]

### 3. LSN — 把日志位置、页修改和恢复进度放到同一坐标

场景提示: 日志文件不断追加，怎样知道某个脏页对应的日志是否已经写好，哪些日志可以安全回收？ [写作时展开]

关键设计: LSN 是 InnoDB 日志空间中的递增位置/进度坐标，多个组件用它描述先后：

```[pseudocode]
新的页修改
  → 获得对应 Redo LSN

页状态:
  page_lsn = 该页最近一次修改对应的日志位置

日志状态:
  current/available LSN
  flushed_to_disk_lsn
  checkpoint_lsn

约束:
  数据页刷出前, 相关 Redo 必须先达到要求的持久化位置
  checkpoint 推进后, 更早日志才可能回收
```

Why: 为什么 LSN 不是简单的“日志文件字节偏移”？——**它还承载日志循环空间、页修改顺序、刷盘和 Checkpoint 的关系**；不同版本会有不同字段和内部坐标转换，不能只用 `flushed_to_disk_lsn + offset` 之类简化公式替代完整语义。 [MySQL/InnoDB: 监控 LSN 字段时需按版本文档区分 current、flushed、checkpoint 等进度]

比喻锚点: LSN 像城市工程总账的流水号；每张变更单、每块未归档墙面和安全检查点都用同一号码标记先后。 [写作时展开]

### 4. Checkpoint — 推进安全边界并回收日志空间

场景提示: Redo 文件是环形空间，日志一直写下去迟早会绕回；Checkpoint 怎样保证旧日志可以被覆盖？ [写作时展开]

关键设计: Checkpoint 代表系统已经把足够早的脏页推进到可恢复位置，并移动恢复起点：

```[pseudocode]
脏页不断产生
  → Flush list 持有 page_lsn 信息

Checkpoint 推进
  → 选择并刷出足够早的脏页
  → 更新 checkpoint_lsn
  → 恢复时不必从更早日志开始
  → 旧日志空间可逐步复用

Redo 压力:
  checkpoint_age 过大/日志空间紧张
  → 后台加速刷脏
  → 必要时前台写入被迫等待
```

Why: 为什么 Checkpoint 不能只“把日志指针往前移动”？——**如果对应脏页还没安全刷出，覆盖旧日志就会失去恢复所需的信息**；Checkpoint 是数据页持久化进度与日志回收之间的安全边界。刷新策略、日志容量、I/O 速度和工作负载共同决定 checkpoint pressure。 [内核: Checkpoint 类似日志/脏页系统的回收水位，不等于一次性把所有脏页刷干净]

比喻锚点: Checkpoint 像工程验收线：验收线之前的施工变更都已落地，可以清理旧工单；验收线之后的变更仍必须保留。 [写作时展开]

### 5. 收束

Redo/WAL 的完整链路：

```[pseudocode]
修改 Buffer Pool 页
  → MTR 产生 Redo
  → Log Buffer/日志文件推进 LSN
  → 提交/刷盘边界确认日志
  → 后台刷脏页
  → Checkpoint 推进
  → 旧日志安全回收
  → 崩溃时从 checkpoint 后重放 Redo
```

**Aha Moment**: "InnoDB 的持久化不是‘数据页什么时候写’，而是**日志先把变化记住，LSN 把日志与页进度对齐，Checkpoint 再决定哪些旧信息可以安全忘掉**。"
**回答读者三问**: ①为什么提交不一定同步写数据页=WAL 允许数据页晚刷；②LSN 有什么用=统一表达日志、页修改和恢复进度；③日志为什么不会无限增长=Checkpoint 刷脏并推进回收边界。

---

### 核心悬念

**"Redo 保护了已提交修改，但一个事务更新到一半崩溃时，未提交内容如何回滚？多个事务又如何把 binlog fsync 与引擎提交绑在一起？"**

→ 引出 03-group-commit-undo-recovery — Group Commit、Undo、MVCC 与崩溃恢复。