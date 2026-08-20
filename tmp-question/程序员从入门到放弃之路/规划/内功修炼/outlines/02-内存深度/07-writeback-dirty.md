# Writeback — 脏页如何安全落盘

> Cluster D: 4 KPs | 依赖: 06-page-cache-xarray | 读者基线: address_space + xarray 索引
> 读者处境: 已读 06 篇（页缓存怎么"进"）；本篇回答"脏页怎么'出'——谁标记、谁写回、写太快怎么办"
> 打开新视角: 脏页标记的 xarray tag、双阈值触发链、Flusher 的 per-backing-device 循环、Dirty Throttling 的 PID 控制数学

---

### 概念依赖链

```
06-页缓存(进) → 本篇: Writeback(出)
  ├─ §1 脏页标记与触发链(谁标脏/何时触发 — 依赖 06 xarray tag)
  │    ├─ §2 Flusher 线程(写回循环 — 依赖 §1 标记)
  │    │    └─ §3 Dirty Throttling(限流数学 — 依赖 §1 阈值)
  │    └─ §4 memfd(匿名文件 — 依赖 06 页缓存)
先讲: 标记(脏) → 触发(阈值) → 写回(flusher) → 限流(数学) → 特例(memfd)
后续依赖: 08-反向映射(回收)
```

### 叙事顺序

1. 问题引入——write() 返回了但数据还在内存——谁负责"落盘"？什么时机？（**Aha: 写回不是'立即做'，是'标记+攒批+后台写'——两级阈值决定谁等谁**）
   - 过渡: 脏页怎么"标记"？
2. 脏页标记与触发链——xarray DIRTY tag；10% 后台/20% 阻塞
   - 过渡: 触发后谁写？——Flusher
3. Flusher 线程——per-backing-device 循环；WB_SYNC_NONE/ALL
   - 过渡: 写太快（磁盘跟不上）怎么办？——限流
4. Dirty Throttling——PID 控制器：setpoint/pos_ratio/task_ratelimit
   - 过渡: 有个特殊"文件"没磁盘后备——memfd
5. memfd——匿名文件共享内存 + seal
   - 过渡: 完整图景已齐——收束
6. 收束——标记→触发→写回→限流的完整链条

### 1. 脏页标记与限流触发链 — 从 folio_mark_dirty 到进度睡眠

场景提示: write() 把数据写进页缓存——怎么让内核"知道"这页需要落盘？ [写作时展开]

关键设计: 标记 + 双阈值触发（mm/page-writeback.c）： [内核: 阶段1-04 篇脏页回写概念→本篇源码——双阈值是 04 篇的落地]

```[pseudocode]
folio_mark_dirty → folio_account_dirtied → __folio_mark_dirty
  → __xa_set_mark(&i_pages, index, PAGECACHE_TAG_DIRTY) — xarray 标脏
  → inc_node_page_state(NR_FILE_DIRTY) — 全局脏页计数
第一层: wb_dirty_exceeded(wb, bg_thresh) 超 10% → wb_start_background_writeback
第二层: balance_dirty_pages_ratelimited → balance_dirty_pages
  → global_dirty_limits(&bg_thresh, &dirty_thresh)
  bg_thresh = total * dirty_background_ratio/100(10%) — 后台唤醒
  dirty_thresh = total * dirty_ratio/100(20%) — 阻塞写入者
  → 超 20% → current->nr_dirtied + dirty_pause + io_schedule_timeout(进度睡眠)
```

Why: 为什么"两级阈值"？——**两级对应两种等待者**：10%（background）唤醒 Flusher 后台写（**不阻塞任何进程**）；20%（thresh）阻塞**写进程本身**（进度睡眠，磁盘跟不上了就让你等）。**10% 是"开始干活"，20% 是"必须停下等"**——中间的 10% 区间是 Flusher 的"安全作业区"（异步写回追上脏页生成的速度）。

比喻锚点: 两级阈值=洗碗池两级警戒——水位到 10%（bg_thresh）叫洗碗工（Flusher）来洗（不耽误做饭=不阻塞写入）；到 20%（dirty_thresh）做饭的人（写进程）得停手等（进度睡眠）——中间 10% 是洗碗工的追赶空间。 [写作时展开]

### 2. Flusher 线程工作机制 — per-backing-device 的写回循环

场景提示: 谁真正把脏页写到磁盘？一个全局线程？ [写作时展开]

关键设计: per-backing-device 写回循环（fs/fs-writeback.c）：

```[pseudocode]
struct bdi_writeback → wb_workfn → wb_do_writeback → wb_check_background_flush
wb_writeback(wb, work) → writeback_sb_inodes(遍历 sb 脏 inode)
  → __writeback_single_inode → do_writepages → a_ops->writepages → ext4_writepages
  → mpage_map_and_submit_extent → mpage_submit_bio → bio_alloc → bio_add_folio → submit_bio
写完 → folio_end_writeback → folio_clear_dirty → __xa_clear_mark(DIRTY tag)
sync 模式: WB_SYNC_NONE(后台, 不等待) vs WB_SYNC_ALL(sync 系统调用, 等待每个 inode)
```

Why: 为什么"每设备一个 Flusher"而非"全局一个"？——**设备特性不同**：每个块设备有自己的脏页速率/IO 能力/队列深度；全局一个线程会被慢设备拖累（或对快设备调度不足）。**per-backing-device（bdi）模型让每个设备独立写回循环**——互不拖累，各自按自己的阈值（wb_dirty_limit）作业。WB_SYNC_NONE/ALL 区分"后台攒批"与"同步等待"（sync/fsync 语义）。

比喻锚点: Flusher=每栋楼一个保洁——每栋楼（块设备）有自己的保洁（bdi 线程）：楼 A 脏得快保洁多跑（独立循环），楼 B 干净保洁少跑——一个保洁扫全小区（全局单线程）会被最脏的楼拖死。 [写作时展开]

### 3. Dirty Throttling 限流数学 — pid 控制器思想

场景提示: 磁盘写 100MB/s，应用写 200MB/s——脏页暴涨，怎么"精准限流"不卡死？ [写作时展开]

关键设计: PID 控制器（mm/page-writeback.c）：

```[pseudocode]
目标: 脏页量稳定在 setpoint = (bg_thresh + dirty_thresh) / 2
dirty_ratelimit: 平滑长期写速率(页/秒) — 每 200ms 更新
pos_ratio: 当前脏页量偏离 setpoint 的比例 — bdi_position_ratio 返回 0-2 倍
task_ratelimit = dirty_ratelimit * pos_ratio >> SHIFT — 每任务实际速率
bdi_dirty_limit: 设备级脏页上限(5.9+ domain_dirty_limits 统一)
可调: /proc/sys/vm/dirty_ratio(20%)/dirty_background_ratio(10%)/dirty_bytes/dirty_expire_interval(30s)
```

Why: 为什么叫"PID 控制器"？——**比例（P）+ 积分（I）+ 微分（D）的工业控制思想**：`pos_ratio` 是比例项（脏页偏离 setpoint 越多，限流越狠）；`dirty_ratelimit` 平滑更新是积分项（长期速率自适应）；每 200ms 更新是采样周期。**目标不是"堵死写入"，是"让脏页量稳定在 setpoint"**——像巡航定速：偏离目标就轻踩刹车（限速），回到目标就松开。

比喻锚点: Dirty Throttling=高速巡航定速——目标速度是 setpoint（脏页平衡点）；车快超速（脏页偏多）就轻点刹车（pos_ratio 限速），慢了就松油门；长期坡度（磁盘快慢）靠巡航系统自适应（dirty_ratelimit 平滑更新）——不是"一脚刹死"，是"稳定在目标速度"。 [写作时展开]

### 4. memfd — 匿名文件的内存共享机制

场景提示: 两个进程共享一大块内存（图形 buffer/剪贴板）——不落盘、不可改，怎么做？ [写作时展开]

关键设计: memfd = 无磁盘后备的"匿名文件"（mm/memfd.c, mm/shmem.c）：

```[pseudocode]
memfd_create(name, MFD_CLOEXEC|MFD_ALLOW_SEALING) → anon_inode_getfile("[memfd]")
shmem_file_setup(name, size, VM_NORESERVE) — 匿名文件 → 无磁盘后备 → 完全在页缓存
ftruncate(fd, size) → write(fd, data) → shmem writepage 可 swap
F_ADD_SEALS(F_SEAL_SHRINK|GROW|WRITE) — 密封: 不可再改
用途: Android/Flatpak 图形 buffer → 高效 IPC 共享内存
```

Why: 为什么 memfd 比"普通文件+mmap"好？——**无磁盘后备**（数据只活在页缓存，不落盘——适合临时共享）+ **fd 语义**（通过 fd 传递，无需路径/权限管理）+ **seal 密封**（F_SEAL_WRITE 后不可修改——**共享只读保证**，多进程安全共享无需锁）。**"文件接口 + 内存语义"**——享受文件 API 的便利（fd/mmap/ftruncate）又不落盘。

比喻锚点: memfd=传阅的保密文件——文件只在会议室（内存）里传阅（不落盘）；文件加了"禁改章"（seal）后谁也不能涂改（共享只读）；传递靠"编号"（fd）而非复印（磁盘路径）。 [写作时展开]

### 5. 收束

回到"脏页怎么安全落盘"：
- 标记 = xarray DIRTY tag + 全局计数
- 触发 = 10% 后台唤醒 / 20% 阻塞写入
- 写回 = Flusher per-backing-device 循环
- 限流 = PID 控制器稳定 setpoint
- 特例 = memfd（无盘匿名文件）

**Aha Moment**: "写回不是'写一次落一次'，是'标记 + 攒批 + 后台写 + 精准限流'的流水线——两级阈值决定'谁等谁'（10% 后台干活、20% 写入者等），PID 控制器保证'脏页量稳定在 setpoint'（像巡航定速）。**写回的性能哲学：能异步就异步，异步追不上才同步**。"
**回答读者三问**: ①write 后数据何时落盘=10% 后台/20% 阻塞两级；②谁写盘=每设备一个 Flusher；③写太快怎么办=PID 限流稳定 setpoint。

---

### 核心悬念

**"回收换出匿名页时，内核怎么找到所有映射该页的进程的 PTE？反向映射 anon_vma + rmap_walk 怎么反向追到每个 VMA？"**

→ 引出 08 反向映射 — anon_vma + rmap_walk + try_to_unmap_one——写回是"正着找"（页→磁盘），回收要"反着找"（页→所有映射者）。
