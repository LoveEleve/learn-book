# LRU 与回收 — kswapd 后台回收与 Direct Reclaim

> Cluster C: 4 KPs | 依赖: 08-reverse-mapping | 读者基线: 反向映射 anon_vma + try_to_unmap_one
> 读者处境: 已读 08 篇（反向映射找到映射者）；本篇回答"回收的决策层——怎么决定回收谁、谁来回收、回收得多激进"
> 打开新视角: LRU 四链表状态机、MGLRU 的多代革命、kswapd 的水位守护、Direct Reclaim 的优先级递进

---

### 概念依赖链

```
08-反向映射(解除映射) → 本篇: 回收决策
  ├─ §1 LRU 四链表(状态机 — 依赖 01 lru 字段)
  │    ├─ §2 kswapd 后台回收(异步守护 — 依赖 §1)
  │    ├─ §3 Direct Reclaim(同步回收 — 依赖 §1)
  │    └─ §4 回收策略(swappiness 等 — 依赖 §1/§3)
先讲: 状态机(LRU) → 异步(kswapd) → 同步(Direct) → 策略(swappiness)
后续依赖: 10-Swap(匿名页换出)
```

### 叙事顺序

1. 问题引入——内存不够了——"回收哪页"怎么选？"谁来收"？（**Aha: 回收是'两层分工'——LRU 决定'回收谁'，kswapd/Direct 决定'谁来收'**）
   - 过渡: "回收谁"怎么表达？——LRU 状态机
2. LRU 四链表——inactive/active × anon/file；激活/降级
   - 过渡: 谁监视内存水位？——kswapd
3. kswapd——per-NUMA-node 守护；low 唤醒/high 睡眠
   - 过渡: kswapd 不够用（分配等不及）？——Direct Reclaim
4. Direct Reclaim——分配路径同步回收；优先级递进
   - 过渡: 回收时"倾向回收谁"？——策略
5. 回收策略——swappiness/vfs_cache_pressure/pageout 分发
   - 过渡: 完整图景已齐——收束
6. 收束——状态机→异步→同步→策略的完整链条

### 1. LRU 四链表传统模型 — inactive/active anon/file 的状态机

场景提示: 内存里的页分四类（inactive/active × anon/file）——页怎么在这些链表间"升降"？ [写作时展开]

关键设计: 四链表状态机（include/linux/mmzone.h, mm/swap.c, mm/vmscan.c）： [内核: 阶段1-04 篇 LRU 概念→本篇源码——四链表是 04 篇的落地]

```[pseudocode]
struct lruvec: lists[NR_LRU_LISTS]
  LRU_INACTIVE_ANON=0 / LRU_ACTIVE_ANON=1 / LRU_INACTIVE_FILE=2 / LRU_ACTIVE_FILE=3 / LRU_UNEVICTABLE=4
激活: folio_mark_accessed → 首次访问 inactive → 二次访问 folio_referenced → folio_activate 升 active
降级: folio_deactivate → 长时间未访问 active → inactive
回收: 从 inactive tail 开始 → shrink_inactive_list → isolate_lru_folios
  → 回收失败 → lru_add_fn 移 active(二次机会)
MGLRU(6.1+): lru_gen_folio 多代模型 → 每代时间戳 → 回收最老代
  → /sys/kernel/mm/lru_gen/enabled 开关
```

设计动机（MGLRU 革命）: 传统 4-list 需显式 activate/deactivate + 频繁 list_move（lru_lock 争用）；**MGLRU 用代标识+时间戳**——无需频繁移动，减少锁竞争（多核大规模内存场景改进最明显）。

Why: 为什么分"两代"（inactive/active）？——**过滤一次性访问**：新页进 inactive，只访问一次（如启动读配置）永远到不了 active——回收时直接从 inactive 尾部拿走（不伤"常驻页"）；被二次访问才升 active（值得留）。**两代 = "试读期"与"常驻期"**——回收优先牺牲"只读了一次的页"，保护"被反复使用的页"（阶段1-04 篇概念 → 本篇源码）。

比喻锚点: LRU 两代=图书馆两区——新书放"新书区"（inactive），翻过一次的放"常借区"（active）；清理时先清"新书区没人翻的"（inactive 尾部），"常借区"的书（active）保留——没人看的先走，常看的留下。 [写作时展开]

### 2. kswapd 后台回收 — per-NUMA-node 的守护者

场景提示: 内存逐渐变少——谁"提前"发现并开始回收？（不等分配失败） [写作时展开]

关键设计: 每 NUMA node 一个 kswapd 守护线程（mm/vmscan.c）：

```[pseudocode]
kswapd_run(nid) → kthread_run(kswapd, pgdat, "kswapd%d", nid)
kswapd → balance_pgdat(pgdat, order, highest_zoneidx)
  检查每个 zone 水位 → 低于 low → kswapd_shrink_node → shrink_node
shrink_node → shrink_node_memcgs(每个 memory cgroup) → shrink_lruvec
get_scan_count: 确定 anon/file 扫描比例(受 swappiness) → shrink_inactive_list → shrink_folio_list
达标: pgdat_balanced(各 zone 达 high) → kswapd_try_to_sleep → schedule_timeout(睡眠)
```

Why: 为什么 kswapd 是"异步"的？——**提前量**（阶段1-02 篇水位线）：kswapd 在内存**低水位（low）时就醒来回收**（异步，不阻塞任何进程），到高水位（high）才睡——**把"回收"从分配路径移到后台**：分配者永远不用等回收（正常情况）。没有 kswapd，内存只能"用光了才回收"（Direct Reclaim 阻塞）——kswapd 是"预防"，Direct 是"急救"。

比喻锚点: kswapd=水库管理员——水位到黄线（low）就开闸放水（后台回收），回到绿线（high）就休息（睡眠）；没有管理员，水满了才紧急泄洪（Direct Reclaim 阻塞）——管理员是"提前放"，泄洪是"紧急救"。 [写作时展开]

### 3. 直接回收 Direct Reclaim — 分配路径上的同步回收

场景提示: kswapd 没来得及——分配还是失败了——怎么办？ [写作时展开]

关键设计: 分配路径同步回收（mm/page_alloc.c, mm/vmscan.c）：

```[pseudocode]
触发: Buddy 分配失败 → __alloc_pages_direct_reclaim(gfp_mask, order)
条件: gfpflags_allow_blocking(gfp_mask) 允许阻塞
  → __perform_reclaim → try_to_free_pages → do_try_to_free_pages
  → shrink_zones → shrink_node → shrink_lruvec(阻塞当前任务)
优先级递进: sc.priority = DEF_PRIORITY(12) → 下降到 0
  → may_writepage(允许脏页写回) → may_unmap → may_swap(越来越激进)
成功 → 分配成功; 仍失败 → __alloc_pages_may_oom → out_of_memory → oom_kill_process
无 __GFP_DIRECT_RECLAIM → 直接返回 NULL → 上层处理
```

Why: 为什么 Direct Reclaim 要"优先级递进"？——**由温和到激进**：初始（priority 12）只回收"易得页"（干净文件页，直接丢）；不够再降 priority → 允许写回脏页（要 IO）→ 允许 unmap（要清 PTE）→ 允许 swap（要写盘）——**越到后面代价越大**（IO/锁/写盘），所以"先用便宜的"（priority 从 12 到 0 逐级放开）。OOM 是最后手段（全部回收手段耗尽才杀进程）。

比喻锚点: Direct Reclaim=搬家腾地方——先扔垃圾（干净页，priority 高代价小）→ 再卖旧家具（脏页要搬运=IO）→ 再拆墙（unmap 要清 PTE）→ 最后把东西存仓库（swap 要写盘）——从便宜到贵一步步来，实在不行把房东杀了（OOM）。 [写作时展开]

### 4. 回收策略与调优 — anon vs file 的权衡

场景提示: 回收时先牺牲"文件页"还是"匿名页"？凭什么决定？ [写作时展开]

关键设计: 三个调优旋钮（mm/vmscan.c, fs/dcache.c）：

```[pseudocode]
swappiness(0-200, 默认 60): 0=尽量回收文件页避 swap / 100=平等 / 200=积极 swap
vfs_cache_pressure(100): 越低越保留 dentry/inode 缓存
pageout 分发: 匿名页→swap_writepage / 文件页→writepage
shrink_folio_list 每批 SWAP_CLUSTER_MAX(32) 个 → 减少 lru_lock 获取次数
```

Why: 为什么"倾向回收文件页"？——**代价不对称**（阶段1-04 篇）：文件页丢弃后缺页重读（1 次磁盘读）；匿名页换出要"写盘+读回"（2 次 IO 且换入是随机读）。**默认 swappiness=60 偏向文件页**——文件页是"可再生的缓存"（丢了能重读），匿名页是"唯一副本"（丢了数据就没了）——除非匿名页本身不活跃（才值得换出腾内存）。

比喻锚点: swappiness=书架清理偏好——书架满了：文件页是"打印件"（丢了能重新打印=重读）；匿名页是"手写原稿"（丢了就没了）——清理时优先扔打印件（文件页），除非原稿确实很久没看（不活跃匿名页）才处理。 [写作时展开]

### 5. 收束

回到"回收谁/谁来收"：
- 状态机 = LRU 四链表（两代过滤一次性访问）
- 异步 = kswapd（low 唤醒/high 睡眠）
- 同步 = Direct Reclaim（优先级递进）
- 策略 = swappiness（文件页 vs 匿名页权衡）

**Aha Moment**: "回收是'两层分工'：LRU 状态机回答'回收谁'（两代过滤：只读一次的牺牲、反复使用的保留），kswapd/Direct 回答'谁来收'（异步预防 vs 同步急救）。而 swappiness 揭示了一个反直觉事实：**宁可回收文件页（可重读）也不动匿名页（唯一副本）**——'可再生'的优先牺牲。"
**回答读者三问**: ①回收谁=LRU 两代 inactive 尾部先牺牲；②谁回收=kswapd 异步/Direct 同步；③为何偏向文件页=可重读 vs 唯一副本。

---

### 核心悬念

**"匿名页回收时被写到哪里？swap entry 的 64 位是怎么编码的(类型+偏移)？换出后 PTE 变成什么样？换入时 do_swap_page 怎么找回？"**

→ 引出 10 Swap — add_to_swap + do_swap_page + swapin_readahead——回收决定"换出匿名页"，下一篇讲"换去哪、怎么换回"。
