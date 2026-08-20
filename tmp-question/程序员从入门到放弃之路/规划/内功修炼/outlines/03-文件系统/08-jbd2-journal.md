# JBD2 日志架构 — ext4 如何把三本账绑成一个原子事务

> Cluster B: 2 KPs | 依赖: 07-filesystem-comparison | 读者基线: 理解 ext2 无日志时目录项 / inode / bitmap 三本账可能中途失配
> 读者处境: 01-06 已把 ext2 的崩溃窗口暴露得很彻底；07 篇刚说明 ext4 的第一个系统性答案是 JBD2；本篇回答"日志到底怎样把半成品状态变成可恢复事务"
> 打开新视角: JBD2 不是"先写日志再写数据"这么一句话，而是完整的事务机——handle 记账、descriptor 列清单、commit 做原子锚点、replay 只重放已提交事务

---

### 概念依赖链

```
01-06 ext2 三本账 + 07 ext4 路线 → 本篇: JBD2 事务与恢复
  ├─ §1 journal/transaction/handle 三对象(谁持久, 谁运行时)
  ├─ §2 handle 生命周期(start → write_access → dirty_metadata → stop)
  ├─ §3 commit 结构(descriptor → data/metadata blocks → commit)
  └─ §4 recovery/replay(只重放完整事务)
先讲: 对象模型 → 事务写入 → 提交顺序 → 崩溃恢复
后续依赖: 09-cow-snapshot(COW 如何不靠 journal 也保证一致性)
```

### 叙事顺序

1. 问题引入——ext2 里一次 `unlink` 同时要改目录项、inode、bitmap，掉电时三本账可能只写成一半；JBD2 到底把哪一半变成了"要么全成、要么全不成"？（**Aha: JBD2 保证的不是"数据总已落盘"，而是"元数据修改以事务为单位可判定地提交或丢弃"**）
   - 过渡: 先看事务机里最基本的三个角色是谁
2. `journal_t` / `transaction_t` / `handle_t`——日志系统的三层对象
   - 过渡: 业务代码真正接触的入口是 handle——它一生经历什么？
3. `jbd2_journal_start` → `get_write_access` → `dirty_metadata` → `stop`
   - 过渡: handle 结束后，后台怎样把一个事务真正写到 journal 里？
4. commit——descriptor / metadata / commit block 的提交顺序
   - 过渡: 机器在 commit 写到一半时断电怎么办？
5. recovery / replay——为什么只有完整事务会被重放
   - 过渡: 事务已经讲清——不同日志模式又在保护什么边界？
6. ext4 三种 journal 模式——ordered / writeback / journal
   - 过渡: 收束
7. 收束——JBD2 解决的是"三本账原子提交" + Aha Moment

### 1. `journal_t` / `transaction_t` / `handle_t` — 三层对象各管一层职责

场景提示: 你看到 `jbd2_journal_start()`、`kjournald2`、journal inode，这些东西分别是"日志文件"、"一次事务"还是"一次 API 调用"？ [写作时展开]

关键设计: JBD2 运行时至少要区分三层对象（fs/jbd2/*.c）：

```[pseudocode]
journal_t (journal_t / struct journal_s)
  = 整个日志系统实例
  = journal 设备/文件 + 环形空间头尾 + 后台线程 kjournald2

transaction_t
  = 当前正在收集 或 正在提交 的一批元数据修改
  = 把多个 handle 攒成同一个 commit 单位

handle_t
  = 一次上层操作拿到的"记账票据"
  = 业务代码通过它声明: 我准备改哪些 metadata buffer
```

Why: 为什么还要分 `transaction_t` 和 `handle_t` 两层，不能一个 `handle` 直接对应一次 commit？——**因为一个系统调用的修改粒度太细，而一次 commit 的 I/O 粒度要尽量大**：多个 handle 可以并到同一个 running transaction，一起提交，减少 journal I/O 次数；同时上层代码只需拿着小 handle 改自己的几个 buffer，不必直接操心整个 transaction 的生命周期。 [内核: ext2 的问题是每本账各写各的；JBD2 第一步就是先在内存里把它们收编进同一个 transaction]

比喻锚点: `handle_t` 像财务报销单，`transaction_t` 像一整批一起走审批流的报销包，`journal_t` 则是整套财务系统和保险柜。 [写作时展开]

### 2. `handle_t` 生命周期 — 先声明要改哪些块，再把它们挂进事务

场景提示: ext4 修改一个目录项时，为什么不能直接改 `bh->b_data`，还非得先 `get_write_access`？ [写作时展开]

关键设计: JBD2 对元数据写入有一套固定调用协议（fs/jbd2/transaction.c）：

```[pseudocode]
handle = jbd2_journal_start(journal, nblocks)
  → 预留本次操作预计要占用的 journal 空间

jbd2_journal_get_write_access(handle, bh)
  → 告诉 JBD2: 这个 metadata buffer 我要改
  → 必要时为旧内容建立可恢复副本/状态保护

... 业务代码真正修改 bh->b_data ...

jbd2_journal_dirty_metadata(handle, bh)
  → 把这个 bh 挂到当前 running transaction 的元数据脏链表

jbd2_journal_stop(handle)
  → 结束本次 handle
  → 若当前 transaction 满了/需要提交, 唤醒 commit 流程
```

Why: 为什么要把"拿写权限"和"标记脏"拆开？——**因为 JBD2 需要先看到旧状态，再接受新状态**：`get_write_access` 是"我要动它"的声明点，JBD2 可以在这里保证崩溃恢复所需的前置条件；真正改完之后，`dirty_metadata` 才表示"这个 buffer 的新版本属于当前事务"。这相当于把"申请修改资格"和"修改已完成"分成两个阶段。 [内核: 这与数据库 WAL 极像——先登记改动资格，再宣布脏页属于事务]

比喻锚点: 改 metadata 像改公章文件——先到管理员那登记"我要借这份文件修改"（get_write_access），改完再交回并标记"这份是本次事务的新版本"（dirty_metadata）。 [写作时展开]

### 3. commit — descriptor / metadata / commit block 三段式提交

场景提示: 一个事务里可能改了目录块、inode 表块、bitmap 块；这些块在 journal 里是怎么排布的？哪一块决定"事务真的成立了"？ [写作时展开]

关键设计: `kjournald2` 后台线程把 running transaction 写成一段可以重放的日志记录（fs/jbd2/commit.c）：

```[pseudocode]
kjournald2
  → jbd2_journal_commit_transaction(journal)

journal 中一次 commit 的逻辑结构
  [descriptor block]
    记录: 本事务有哪些 metadata block、它们最终对应哪个磁盘块号(tag)
  [metadata/data copies]
    依次写入本事务涉及的 buffer 副本
  [commit block]
    事务完成标志 = "看到它, 才说明前面的这批块整体有效"
```

Why: 为什么提交顺序必须是 descriptor → payload → commit，而不是先写 commit 再补数据？——**因为 commit block 是恢复时的"真伪锚点"**：只有当 descriptor 和所有 payload 都稳定写到 journal 后，最后那个 commit block 才能出现；恢复时只要没看到 commit，就当这批事务从未发生。**所以 commit block 不是普通尾块，而是原子性的判定点。**

比喻锚点: 这像快递打包——先写装箱单（descriptor），再把所有物品装箱（metadata copies），最后才贴封箱签（commit block）。恢复时看见半箱货但没封签，就整箱作废。 [写作时展开]

### 4. recovery / replay — 只重放完整事务，不碰半截事务

场景提示: 如果机器恰好在 descriptor 写完、metadata 写了一半时断电，重启后内核怎么知道这半截事务要不要认？ [写作时展开]

关键设计: 挂载恢复时，JBD2 不是盲目重放整个 journal，而是分阶段扫描并只重放完整提交的事务（fs/jbd2/recovery.c）：

```[pseudocode]
jbd2_journal_recover(journal)
  → do_one_pass(..., PASS_SCAN)
     找出日志里有哪些事务边界/commit 记录
  → do_one_pass(..., PASS_REVOKE)
     处理 revoke 记录(某些旧块即使出现在日志里也不要重放)
  → do_one_pass(..., PASS_REPLAY)
     只对"有完整 commit block"的事务进行重放

没有 commit block?
  → 这批事务视为未提交, 整批丢弃
```

Why: 为什么恢复时能做到 O(journal 大小) 而不是 O(整个文件系统大小)？——**因为 JBD2 把"崩溃前哪些地方可能不一致"压缩进 journal 了**：不再像 ext2 那样全盘扫描 inode/bitmap/目录项三本账，只需看最近这段事务日志。**journal 的价值不是减少正常写 I/O，而是把恢复范围从整盘缩到一小段环形日志。** [内核: 06 篇里 ext2 删除半路掉电会留下整棵树半拆状态；JBD2 的目标就是让恢复只需要看 journal, 不需要猜整盘哪里坏了]

比喻锚点: ext2 崩溃恢复像地震后全城普查；JBD2 像每次施工都先登记施工许可证，灾后只需检查最近那几张施工单，不必挨栋搜。 [写作时展开]

### 5. ext4 三种 journal 模式 — 保护边界不一样

场景提示: 有人说 ext4 默认最安全，也有人说只有 `fsync()` 才安全；到底 `data=ordered/writeback/journal` 差在哪？ [写作时展开]

关键设计: ext4 不是只有一种日志模式，而是三种保护边界（fs/ext4）：

```[pseudocode]
data=writeback
  只 journal 元数据
  数据块何时落盘不做先后保证

data=ordered (默认)
  只 journal 元数据
  但要求相关数据块先于 metadata commit 落盘
  避免新 inode/size 指向垃圾旧数据

data=journal
  数据 + 元数据都先进 journal
  最安全, 也最重
```

Why: 为什么默认是 `ordered`，而不是最安全的 `journal`？——**因为大多数 workload 更在意元数据一致性 + 合理性能**：`ordered` 已经能避免"文件长度更新了但内容还是垃圾块"这类最刺眼的问题；而 `data=journal` 要把数据也写两遍，代价太高。**所以 JBD2 默认解决的是"文件系统不乱账"，不是"每次 write 都天然持久化"。** [man 2 fsync: 需要真正持久化边界时, 仍需显式刷盘]

比喻锚点: 三种模式像快递保险等级——writeback 只保箱单，ordered 先确保货物装车再确认箱单，journal 则连货物本体都先放进保险柜再转运。 [写作时展开]

### 6. 收束

回到 JBD2 的本质：它解决的不是"让磁盘更快"，而是**把元数据更新组织成可判定的事务**：
- handle：上层操作的记账票据
- transaction：一批一起提交的元数据变更
- commit block：恢复时的原子锚点
- replay：只重放完整事务

整条链路：

```[pseudocode]
start(handle)
  → get_write_access(声明要改哪些 metadata buffer)
  → 修改 bh->b_data
  → dirty_metadata(挂进 running transaction)
  → stop(handle)
  → kjournald2 commit: descriptor → payload → commit block
  → 崩溃后 recovery: scan → revoke → replay(只认完整 commit)
```

**Aha Moment**: "JBD2 的关键不是'多写一份日志'，而是**把'目录项、inode、bitmap 这三本本来会分别落盘的账，强行绑成一个带封箱签的事务包'**。只要 commit block 没出现，这包就视为不存在；只要 commit block 出现，恢复时就能整包重放。ext2 最怕的'半写状态'，正是被这个封箱签消灭掉的。"
**回答读者三问**: ①JBD2 保证了什么=元数据事务的原子可恢复性；②为什么 commit block 最关键=它是整批事务生效的唯一锚点；③为什么 ext4 恢复比 ext2 快=只扫 journal，不扫整盘。

---

### 核心悬念

**"如果不想写 journal，还有没有另一条路让崩溃后天然只看到完整版本？Btrfs/ZFS 的 COW + checksum 又是怎样用'切换根指针'代替'重放日志'的？"**

→ 引出 09-cow-snapshot — COW 与快照——JBD2 是'先记日志再原地改写'，下一篇看'根本不原地改写'的另一派答案。