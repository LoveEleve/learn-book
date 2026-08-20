# 页缓存 + 脏页回写 + LRU 回收 + Swap + OOM Killer

> Cluster A: 12 KPs | 依赖: 03-mmap + VMA + 缺页异常 | 读者基线: 了解 VMA 和缺页异常分支
> 读者处境: 已读完 03 篇，知道文件映射缺页（Major Fault）从磁盘读；本篇回答"读到的数据先进哪、写的数据何时落盘、内存不够时怎么逐级应对"
> 打开新视角: 文件 IO 为何"慢读快写"、free 命令 cached/buffered 的含义、内存压力下内核的完整应对阶梯、OOM 时杀谁的判定

---

### 概念依赖链

```
03-缺页(文件映射 Major Fault) → 本篇: 页缓存(文件 IO 中间层)
  ├─ §1 页缓存(address_space 缓存文件页 — 依赖 03 缺页路径)
  │    └─ §2 脏页回写(缓存页何时落盘 — 依赖 §1 的脏标记)
  │         └─ §3 LRU 回收(内存压力时驱逐哪页 — 依赖 §1 缓存页 + 02 页结构)
  │              ├─ §4 KSM(匿名页去重 — 依赖 03 COW)
  │              ├─ §5 Swap(匿名页换出到后备存储 — 依赖 03 缺页 + §3 回收)
  │              └─ §6 OOM Killer(回收无效时的最后一刀 — 依赖 §3)
先讲: 缓存(加速) → 落盘(持久) → 驱逐(腾挪) → 去重/换出/击杀(压力递进)
后续依赖: 05-MESI(伪共享背景)、13-VFS(文件层)
```

### 叙事顺序

1. 问题引入——读文件"第一次慢、之后快"，写文件"秒返回但断电丢数据"（**Aha: 页缓存让文件 IO 从'每次都访问磁盘'变成'只缺页一次'**）
   - 过渡: 缓存放哪？按什么结构索引？
2. 页缓存——address_space：host/i_pages(xarray)/a_ops，读预读/写脏标记，folio 批量
   - 过渡: 脏页不会永远留在内存——什么时候落盘？
3. 脏页回写——flusher 线程 + dirty 比例阈值 + 过期时间
   - 过渡: 内存总是不够——该赶走哪页？
4. LRU 回收——四链表升降 + 水位线 + kswapd/direct reclaim + refault distance
   - 过渡: 除了被动驱逐，还有一类主动腾挪——内容相同的匿名页
5. KSM——相同匿名页合并去重（依赖 COW 机制）
   - 过渡: 文件页有后备存储可以丢；匿名页丢了就没了——怎么办？
6. Swap——匿名页换出到 swap 分区，swap entry 标记 PTE
   - 过渡: 换出也救不回来时——最后一刀
7. OOM Killer——badness 评分决定杀谁
   - 过渡: 压力应对阶梯已齐——回到"文件 IO 快"的本质
8. 收束——页缓存/回写/回收/KSM/swap/OOM = 内存生命周期六环节

### 1. 页缓存 — 文件 IO 的必经缓存层

场景提示: 读文件第一次几十 ms、第二次几 µs；`free` 的 cached 列——页缓存让"读过的文件页留在内存"。 [写作时展开]

关键设计: 每个 inode 一个 `struct address_space`——文件页缓存的索引 (include/linux/fs.h)：

| 字段 | 含义 |
|------|------|
| host | 所属 inode |
| i_pages | 页索引——Linux 4.20+ 用 **xarray**（替代 radix tree），按文件偏移索引缓存页 |
| a_ops | address_space_operations——readpage/writepages 等回调（各文件系统实现） |

读流程: `do_generic_file_read → find_get_page`（查 xarray）→ 未命中 → `page_cache_sync_readahead`（预读）→ `readpage`（磁盘 IO）→ 加入 address_space → 再次访问命中（零 IO）。预读把顺序读的后续页提前加载，把 N 次磁盘 IO 折叠成 1 次。

写流程: `generic_perform_write → __block_write_begin`（读块到页）→ 用户写入修改 → `mark_buffer_dirty`（标记块脏）→ `set_page_dirty`（标记页脏）→ 返回（无磁盘 IO）→ 异步回写。

folio (Linux 5.16+): 页缓存基本单位从单 page 升级为 folio——一个或多个连续物理页的容器（order>0 时为 compound page），减少 page→folio 反复转换、writepages 可批量操作连续页。

Why: 为什么文件 IO 必须经过页缓存？——①读复用：同一文件页被多次读只走一次磁盘；②写缓冲：写只需标记脏，落盘由后台批量合并；③共享：mmap 文件映射与 read 共享同一页缓存（03 篇 Major Fault 的 `filemap_fault` 正是从这里取页）。文件 IO 的性能本质 = 命中页缓存。

### 2. 脏页回写 — 从内存到磁盘的流水线

场景提示: 写文件秒返回，但 `poweroff` 与 `reboot` 差异——脏页落盘时机决定数据安全窗口。 [写作时展开]

关键设计: flusher 线程（per backing device，替代旧 pdflush）按阈值/周期异步回写：

| 触发 | 默认值 | 行为 |
|------|--------|------|
| dirty_background_ratio | 10% | 后台回写，不阻塞应用 |
| dirty_ratio | 20% | 前台阻塞等待（应用写被迫等落盘） |
| dirty_expire_interval | 30s | 页老化时间，超期必须写 |
| dirty_writeback_interval | 5s | flusher 检查周期 |
| dirty_bytes | — | 字节级阈值（替代百分比） |

回写流程: `bdi_writeback → wb_writeback → writeback_sb_inodes → __writeback_single_inode → do_writepages → mapping->a_ops->writepages`——从通用回写框架到具体文件系统回调。

Why: 为什么"先缓冲后批量落盘"而非写一次落一次？——磁盘随机写慢（寻道），批量合并顺序写快；100 次小写合并成 1 次大写的性能差距可到 10-100 倍。代价是断电丢失窗口（30s 内未落盘的脏页）——`fsync` 让应用在关键数据上强制落盘。

### 3. LRU 内存回收 — 四链表 + kswapd 水位线

场景提示: 内存不足时 `free` 的 available 下降——内核按什么顺序驱逐哪页？ [写作时展开]

关键设计: 每 zone 的 lruvec 维护**四条 LRU 链表**：

```[pseudocode]
LRU_INACTIVE_ANON / LRU_ACTIVE_ANON   — 匿名页（无文件后备）
LRU_INACTIVE_FILE / LRU_ACTIVE_FILE   — 文件页（有后备存储）
页在 inactive↔active 之间升降: 访问提升, 老化降级
```

水位线触发 (每 zone 独立, `/proc/zoneinfo` 可查):
- **high**: 安全
- **low**: 唤醒 kswapd 后台回收（异步，不阻塞分配）
- **min**: direct reclaim 同步回收（阻塞分配直到释放足够页）

回收路径: `shrink_node → shrink_lruvec → isolate_lru_pages → 回收或换出`——从 inactive 链表尾部取页：文件页直接丢弃（有后备），匿名页换出（§5）。

refault distance: 页被回收后再次访问——若距离近（min_seq − refault 小）说明**过早回收**→ 该页应升 active 防再次驱逐。防止"回收-缺页-再回收"的抖动循环。

Why: 为什么分 inactive/active 两代？——单链表"最久未用先驱逐"会把刚访问一次的冷页误杀（扫描冲击）。两代机制：新页进 inactive，被二次访问才升 active——需要"至少两次访问"才值得留在内存，过滤一次性扫描页（如启动读一堆配置后不再用）。

### 4. KSM — 相同匿名页的合并去重

场景提示: 一台宿主机跑 50 个同 OS 虚拟机——每个 VM 的内核/库页内容相同，却占 50 份内存。 [写作时展开]

关键设计: KSM（Kernel Same-page Merging）扫描匿名页 → memcmp 内容相同 → 合并为**写保护共享页** → 后续任一写触发 COW 裂开（复用 03 篇机制）：

```[pseudocode]
ksm_do_scan: 扫描匿名页 → 内容比较 → 相同页合并(写保护) → 写时 COW 裂开
```

配置: `echo 1 > /sys/kernel/mm/ksm/run` 启用；`pages_to_scan`（每次扫描页数，默认 100）/`sleep_millisecs`（间隔）/`pages_shared`（已合并数）。KVM 用 `MADV_MERGEABLE` 标记虚拟机内存。

代价: CPU 扫描开销 + COW 裂开时一页复制——适合 VPS/虚拟化（多同源镜像）而非通用负载（扫描 CPU 换内存不值）。

Why: 为什么放在回收体系里？——KSM 是"主动腾挪"：不等待压力，周期性去重相同页。与 LRU 的被动驱逐互补：一个防患未然，一个事后应对。

### 5. Swap — 匿名页的后备存储

场景提示: 内存吃紧时匿名页（堆/栈数据）没有文件可丢——换到哪去？ [写作时展开]

关键设计: Swap 给匿名页提供后备存储：

- **swap entry**: PTE 低 12 位为 0（标识"此页在 swap"）→ 高位存 swap_type + swap_offset——页不在物理内存，在 swap 分区/文件
- **换出**: LRU 回收选中匿名页 → 写入 swap → PTE 改 swap entry
- **换入**: 访问触发缺页 → `do_swap_page` 检测 swap entry → 分配物理页 → 从 swap 读回 → 恢复 PTE——与 file-backed Major Fault 同路径

swappiness 控制倾向: `vm.swappiness`（0-100，默认 60）——>60 倾向换出匿名页，<60 倾向回收文件页。数据库建议 0-10：文件页有后备可安全丢弃，匿名页换出/换入开销大，宁可多回收文件页。

Why: 为什么不用 swap 而优先回收文件页？——文件页丢弃后缺页可重读（代价=磁盘读），匿名页换出要"写盘+读回"两步；且 swap 换入是随机读（比顺序文件读慢）。swap 是"最后的后备"，不是首选。

### 6. OOM Killer — 内存耗尽时的最后一刀

场景提示: dmesg 里 "Out of memory: Killed process"——为什么是它被杀？ [写作时展开]

关键设计: direct reclaim 仍无法满足分配 → `out_of_memory` → `select_bad_process`（badness 评分）→ `oom_kill_process` → SIGKILL。

badness 评分 (mm/oom_kill.c): 进程 RSS（越大越可能被杀）× oom_score_adj 调整 / 运行时间减分 / CAP_SYS_ADMIN 减分（守护进程豁免）。动态查看 `/proc/PID/oom_score`，设置 `/proc/PID/oom_score_adj`（-1000 = 永不杀，如数据库/监控）。

OOM 日志: `"Out of memory: Killed process XXXX (name) total-vm:XXXXkB, anon-rss:XXXXkB"` → `dmesg | tail -50`。

Why: 为什么 OOM 必须"杀进程"而不是拒绝分配？——overcommit 已承诺（03 篇），进程真访问时内核必须兑现；兑现不了又无法回收时，唯一选择是杀掉一个承诺占用者，释放物理页。这是 overcommit 模式的必然兜底——严格模式（overcommit_memory=2）减少 OOM 概率，但无法消除。

### 7. 收束

回到文件 IO 完整生命周期：
- 页缓存 = 加速（读缓存 + 写缓冲）→ 脏页回写 = 持久（批量落盘）
- LRU = 驱逐（两代+水位线）→ KSM = 去重（主动腾挪）→ Swap = 换出（匿名后备）→ OOM = 击杀（最后兜底）

**Aha Moment**: "读文件'慢一次快一生'靠页缓存，写文件'秒返回'靠脏页缓冲——文件 IO 的一切性能都来自'内存兜底'；而内存耗尽时的应对是递进阶梯：驱逐文件页 → 去重相同页 → 换出匿名页 → 杀进程。`free` 命令的 cached 不是'浪费的内存'，而是'随时可回收的后备库'——它同时是加速器（命中）和弹药库（可回收）。"
**回答读者三问**: ①文件 IO 快=页缓存命中；②cached/buffered=文件页缓存可回收（不是浪费）；③OOM 杀谁=badness 评分（RSS 大+adj 高）。

---

### 核心悬念

**"单核跑完了内存分配，多核同时分配页时 Buddy 分配器的 free_area 链表会被并发破坏——CPU 怎么保证缓存行一致性？什么是 MESI？"**

→ 引出 05-MESI 缓存一致性 + 伪共享 + Store Buffer + 内存模型——内存管理讲完了单核视角，多核的挑战从缓存一致性开始。
