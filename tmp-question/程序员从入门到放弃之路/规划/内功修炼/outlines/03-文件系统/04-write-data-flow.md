# 写数据流程 — `write()` 为什么成功了却还没落盘

> Cluster A: 3 KPs | 依赖: 03-file-creation-touch | 读者基线: 理解 inode/页缓存/bitmap 分配
> 读者处境: 03 篇已经把 0 字节文件创建出来了；本篇回答"第一个字节怎么写进去——为什么 `write()` 返回成功，磁盘却还没动"
> 打开新视角: `write()` 成功通常只意味着"数据进了 Page Cache 并被标脏"，不是"磁头已经落盘"；ext2 写路径本质是"先映射块，再改页缓存，最后异步回写"

---

### 概念依赖链

```
03-file-creation-touch(空 inode + 目录项) → 本篇: 写路径与回写
  ├─ §1 write_iter 入口(file → VFS → generic_perform_write)
  ├─ §2 ext2_write_begin(逻辑块 → 物理块映射, 需要时分配 block)
  ├─ §3 copy + dirty(数据进入页缓存, folio/buffer_head/inode 同时变脏)
  └─ §4 throttling + writeback(dirty_ratio 限速, wb_workfn 异步落盘)
先讲: 入口 → 块映射 → 脏页 → 回写
后续依赖: 05-读流程(页缓存未命中如何读回) / 06-文件删除(脏页与回收的关系)
```

### 叙事顺序

1. 问题引入——`write(fd, "x", 1)` 返回 1，为什么拔电后数据仍可能丢？（**Aha: `write()` 成功只代表内核接管了数据，不代表磁盘完成了持久化**）
   - 过渡: 这 1 个字节先流到哪？
2. `sys_write` → `generic_perform_write`——入口到逐页写循环
   - 过渡: 真正要写页前，内核先要知道这个字节属于哪个磁盘块——怎么映射？
3. `ext2_write_begin` + `ext2_get_block`——块映射与按需分配
   - 过渡: 块有了，字节怎么进入内核内存？
4. copy + dirty——页缓存中的"已写入"
   - 过渡: 页变脏了，什么时候才会真的去写盘？
5. 回写与限速——`balance_dirty_pages` + `wb_workfn`
   - 过渡: ext2 讲清了——ext4 为什么还要做延迟分配？
6. ext4 延迟分配 tradeoff——性能更好，崩溃窗口更大
   - 过渡: 写链路已齐——收束
7. 收束——`write` 成功 ≠ 落盘 + Aha Moment

### 1. `sys_write` → `generic_perform_write` — 从系统调用到页级循环

场景提示: 你在用户态执行 `write(fd, buf, 8192)`——这 8KB 是一次性直接打到磁盘，还是先被拆成页？ [写作时展开]

关键设计: ext2 最终复用通用写路径，按页/folio 迭代写入（fs/read_write.c + fs/ext2/file.c + mm/filemap.c）：

```[pseudocode]
sys_write(fd, buf, count)
  → ksys_write → vfs_write
  → file->f_op->write_iter = ext2_file_write_iter (fs/ext2/file.c)
  → generic_file_write_iter → __generic_file_write_iter
  → generic_perform_write(iocb, from)
      while (还有数据没写完):
        a_ops->write_begin(...)
        copy_page_from_iter_atomic / copy_folio_from_iter_atomic
        a_ops->write_end(...)
```

Why: 为什么 ext2 不自己写一套 `write()` 主循环？——**绝大多数本地文件系统共享同一条缓存写路径**：边界检查、分段写、多页循环、脏页记账都能复用 `generic_perform_write`；具体文件系统只需实现 `write_begin/write_end/get_block` 这几个磁盘相关钩子。**这就是 VFS 的价值**：把"写语义"和"磁盘映射"拆开。 [内核: 通用写路径与阶段2-06 篇页缓存/XArray 直接相连——文件系统只管块映射, 页缓存负责承接脏页]

比喻锚点: `generic_perform_write` 像流水线装配工——不关心你是 ext2 还是 ext4，只负责把大包裹拆成一页一页的小箱子，逐箱处理；每处理一箱前，先问文件系统"这箱该放哪"。 [写作时展开]

### 2. `ext2_write_begin` — 先把逻辑块翻译成物理块

场景提示: 文件原本 0 字节，第一次 `write()` 时连数据块都还没有——内核怎么知道应该把字节放到磁盘哪一块？ [写作时展开]

关键设计: `ext2_write_begin` 调 `block_write_begin`，而真正的块映射由 `ext2_get_block` 完成；若块不存在，则走位图分配（fs/ext2/inode.c + fs/ext2/balloc.c）：

```[pseudocode]
a_ops->write_begin = ext2_write_begin
ext2_write_begin(file, mapping, pos, len, pagep, fsdata)
  → block_write_begin(mapping, pos, len, pagep, ext2_get_block)
      → ext2_get_block(inode, iblock, bh_result, create=1)
         → ext2_get_blocks: 解析 i_block[15] 间接链
         → 已有块? 把 bh_result->b_blocknr 指向现有物理块
         → 没有块 && create=1?
             ext2_new_blocks(...) 从 block bitmap 分配新块
             更新间接块 / inode 的 i_block 指针链
```

Why: 为什么 `write_begin` 阶段就要分配块，而不是等回写时再分配？——**ext2 是立即分配（allocate-on-write）**：用户一写，逻辑块就必须拿到真实物理块号，这样后面的页缓存/buffer_head 才知道自己脏的是哪块磁盘地址。**好处**：崩溃窗口小，名字在目录里、块号也已经决定；**代价**：连续小写可能拿到很多零散块，碎片更重——这正是 ext4 延迟分配要优化的点。 [内核: 03 篇改的是 inode bitmap, 这里第一次改 block bitmap——文件从"有名字"变成"有数据块"]

比喻锚点: `ext2_get_block` 像仓库管理员给快递分库位——先看这个包裹原来有没有货架号；没有就当场在空位表（block bitmap）里找一个货架，把编号记到账本上，再允许工人把东西放进去。 [写作时展开]

### 3. copy + dirty — 数据先进入 Page Cache，不是磁盘

场景提示: `write()` 把 1MB 数据写进文件时，CPU 真的是一字节一字节往磁盘控制器里推吗？ [写作时展开]

关键设计: 数据先拷入页缓存中的 folio/page，然后把 folio、buffer_head、inode 都标脏（mm/filemap.c + mm/page-writeback.c + fs/buffer.c）：

```[pseudocode]
generic_perform_write
  → copy_page_from_iter_atomic / copy_folio_from_iter_atomic
      把用户态 buf 拷进 folio(page cache)
  → folio_mark_accessed(folio)
  → ext2_write_end → generic_write_end
      → folio_mark_dirty(folio)
         → __folio_mark_dirty
            → __xa_set_mark(mapping->i_pages, folio_index, PAGECACHE_TAG_DIRTY)
            → __mark_inode_dirty(inode, I_DIRTY_PAGES)
      → block_write_end / __block_commit_write
      → mark_buffer_dirty_inode(bh, inode)
```

Why: 为什么要同时标脏 folio、buffer_head、inode 三层？——**三层负责三种视角**：folio 脏=这页缓存内容变了；buffer_head 脏=这页内哪些磁盘块片段变了；inode 脏=回写系统知道"这个文件有脏数据待刷"。**`write()` 成功点在 copy 完成，不在 submit_bio 完成**：只要字节已经进了页缓存且元数据记账完成，系统调用就可以返回，让后台异步刷盘。 [内核: `__xa_set_mark(PAGECACHE_TAG_DIRTY)` 把脏页挂进 XArray 脏集合, `__mark_inode_dirty(I_DIRTY_PAGES)` 把 inode 挂进 writeback 账本]

比喻锚点: 这像先把账单抄进会计的待办本——客户交材料（用户 buf）后，会计先把数字抄进内部账册（Page Cache），再贴上"待处理"红签（dirty 标记）；真正去银行转账（磁盘写）是财务批处理线程稍后干的事。 [写作时展开]

### 4. `balance_dirty_pages` + `wb_workfn` — 脏太多就限速，后台异步落盘

场景提示: 如果程序一直疯狂 `write()`，内核是不是会一直只堆脏页不刷盘？什么时候用户线程会被卡住？ [写作时展开]

关键设计: 前台写线程负责制造脏页，后台 writeback 线程负责刷盘；脏页太多时，前台会被主动限速（mm/page-writeback.c + fs/ext2/inode.c）：

```[pseudocode]
write_end 之后
  → balance_dirty_pages_ratelimited(mapping)
      → 脏页低于阈值? 直接返回
      → 脏页逼近 dirty_ratio / dirty_bytes?
          调 balance_dirty_pages(wb, ...)
          当前写线程 sleep / 降速, 等后台跟上

后台线程: wb_workfn
  → ext2_writepages(mapping, wbc)
  → mpage_writepages(..., ext2_get_block)
  → submit_bio / submit_bh(WRITE)
  → 磁盘完成后 folio/buffer_head 清脏
```

Why: 为什么脏页回写要"异步 + 限速"两件事一起做？——**只有异步，没有限速**：内存会被脏页吃光；**只有同步，没有异步**：每次 `write()` 都阻塞等磁盘，吞吐崩掉。Linux 选折中：先让用户线程快写进缓存，再用 `dirty_ratio/dirty_bytes` 把脏页水位控制在可回收范围内，后台 `wb_workfn` 持续排水。 [man 2 fsync: 需要持久化语义时, 必须显式等待回写完成]

比喻锚点: 脏页系统像水库——前台 `write()` 是上游来水，后台 writeback 是放水闸门；平时先蓄着提高吞吐，水位太高（dirty_ratio）就限流上游，不然大坝（内存）会顶满。 [写作时展开]

### 5. ext4 延迟分配 — 把"先拿块号"改成"先记欠条"

场景提示: ext2 第一次写就立刻分块，为什么 ext4 还要把分块推迟到回写时再做？ [写作时展开]

关键设计: ext4 的 delayed allocation 改掉了 ext2 的"写时立即分块"策略——`write_begin` 先占逻辑空间，不立刻决定物理块号（fs/ext4/inode.c）：

```[pseudocode]
ext2: write_begin 时 ext2_get_block(create=1) → 立刻拿到真实物理块
ext4: ext4_da_write_begin → ext4_da_get_block_prep
      → 先记 "这段逻辑块以后需要空间"，但暂不选物理块
回写时:
  ext4_da_writepages → ext4_map_blocks / ext4_mb_new_blocks
  → 一次性给连续脏页挑大块 extent
```

Why: 为什么 ext4 宁可扩大崩溃窗口，也要延迟分配？——**为了把很多零碎小写合并成连续 extent**：等到回写时再看全局脏页分布，能一次拿到大块连续空间，显著减少碎片和 I/O 次数。**代价**：`write()` 返回时块号还没落定，断电时新数据更可能丢——所以真正要持久化语义必须 `fsync()`，不能把 `write()` 当提交点。

比喻锚点: ext2 像顾客一来就现场分座位；ext4 延迟分配像先发排队号，等整批客人到齐再统一排座——更整齐、更省空间，但在正式入座前断电，名单可能还只在前台草稿里。 [写作时展开]

### 6. 收束

回到 `write()` 成功的真相：它通常只完成了三件事：
- 把用户态字节拷进 Page Cache
- 把 folio / buffer_head / inode 标脏
- 在 ext2 下，顺手把逻辑块映射到物理块（必要时从位图分配新块）

完整链路：

```[pseudocode]
write()
  → ext2_file_write_iter → generic_perform_write
  → ext2_write_begin / ext2_get_block / ext2_new_blocks(必要时)
  → copy_from_user 到 Page Cache
  → folio_mark_dirty + mark_buffer_dirty_inode
  → balance_dirty_pages_ratelimited
  → wb_workfn 后台 ext2_writepages
  → submit_bio → 真正落盘
```

**Aha Moment**: "`write()` 最常见的成功语义不是'数据已持久化'，而是**'数据已经安全地进入了内核的缓存与脏页账本'**。ext2 在 `write_begin` 时就把块号定下来，所以'地址'先稳定；真正慢的是回写。ext4 则连块号都可以先不定——这换来更少碎片，但把'成功'和'落盘'拉得更远。"
**回答读者三问**: ①`write()` 返回成功为什么还会丢数据=数据还在 Page Cache；②ext2 第一次写时做了什么=分块 + 脏页记账；③什么时候才真的落盘=后台 writeback 或 `fsync()`/内存压力触发。

---

### 核心悬念

**"`read()` 第一次打开一个从未命中过 Page Cache 的文件——缺页后，ext2 怎么沿着 inode 的间接块链把一个 4KB 页面从磁盘搬回内存，并塞进页缓存？"**

→ 引出 05-read-data-flow — 读路径与页缓存未命中——写完只是把页弄脏，下一篇看页第一次怎么被读进来。