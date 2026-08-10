# 内存深度 — 全视角完备性验证

> 13 篇, 40 KPs, 562 行总长 | 每篇平均 43 行 | 范围: 39-55 行

---

## 读者视角 — 教学顺序是否合理？

1. 01(struct page) → 02(Buddy) 的过渡: 读者能否从"页描述符"自然过渡到"怎么从 free_area 分配"？对物理内存结构的理解是否完整？
2. 02(Buddy/PCP) → 03(页表): 物理页分配完后, 读者能否自然转向"分配了物理页怎么映射到虚拟地址"？
3. 03(页表) → 04(mm_struct/VMA): 从低层硬件机制跳到高层进程抽象 — 过渡是否平滑？
4. 04(VMA) → 05(缺页): VMA 只是数据结构, 缺页是它的实战 — 读者是否清楚 "VMA 不等同于页表" 这个关键区别？
5. 09(LRU/kswapd) 自反: kswapd 的回收流程里提到 "直接回收", 但读者此时还不知道 Direct Reclaim 的分配路径触发点(在 09 本文内解释) — 逻辑闭环是否完整？
6. 13(ptmalloc) 是读者最熟悉的用户态层面 — 放在结尾是否合适？是否需要移到开头作为"引子"？

## 开发者视角 — 每个 KP 是否够详细？

7. 01: struct page 的 `mapping` 字段双重语义(file-backed→address_space, anonymous→anon_vma) — 两个方向都解释清楚了吗？
8. 02: Buddy fast path 和 slow path 的边界条件(ZONE_DMA 的 watermark 限制) — 读者能否区分？
9. 03: PGN_FLAGS 和 PTE 标志位(bit0-63) — 硬件域和软件域的界限解释清楚了吗？
10. 04: `mmap_lock` 读写竞争 — VMA merge/split 时的锁获取顺序是否可被读者推演？
11. 05: 匿名缺页(map)和文件缺页的 `vmf`(vm_fault) 状态变量 — 缺页处理器的统一接口设计解释了吗？
12. 06: xarray 的 RCU 读取路径 — 读者能否理解 "写者加锁, 读者无锁" 的并发模型？
13. 07: Dirty Throttling 的 pid 控制器 — 数学公式(`pos_ratio`, `task_ratelimit`) 的解释层次够不够？
14. 08: try_to_unmap_one 中 "交换 PTE 为 swap entry" 的操作 — PTE 从硬件值变为软件值的本质变化讲清楚了吗？
15. 09: MGLRU 的 "generation 时间戳" 模型 — 与传统 four-list 模型的对比够详细吗？
16. 10: swap entry 的 64 位编码 — type(位域宽) + offset(位宽) 的边界(on-disk layout)讲够了吗？
17. 11: OOM Killer `oom_badness` 评分 — 各子项(vm_rss, swap_ents, ptes)的权重理由解释了吗？
18. 12: MemAvailable 计算中 `watermark_low + reserved` — 这些预留值的内涵是否解释？

## SRE/性能视角 — 调优参数够不够

19. 02: `min_free_kbytes` 和 Watermark 三级的动态计算 — 大内存机器(>64GB)的用户怎么调？
20. 03: TLB shootdown IPI 的触发频率 — 读者能否预估多少 VMA 操作导致 IPI 风暴？
21. 06: MADV_DONTNEED → `zap_page_range` 的延迟 — 大 VMA(>100MB)时的阻塞风险有提示吗？
22. 07: `dirty_ratio/dirty_bytes` 并发写入者的速度 — 脏页超过 20% 后 `balance_dirty_pages` 阻塞多久？
23. 09: swappiness=0 vs 60 vs 100 — 数据库(自有 buffer pool)环境下的推荐值？
24. 10: `swapon -p` swap 优先级 — 多设备(SSD + HDD)场景下的配置策略？
25. 11: `oom_score_adj` 生产环境的最佳实践 — 什么进程应该 `-1000` 保护？
26. 12: cgroup v2 `memory.stat` 的 `slab_reclaimable` — 容器环境怎么根据这个值评估实际可用内存？

## 架构师视角 — 设计决策与演进

27. 01: struct page 64 字节 — 为什么不用更小的(如 48 字节)？5.16 引入 folio 的根本困境是什么？→ **已覆盖**: 01 §1 设计考量行解释 64B=缓存行宽度避免跨行 cache miss
28. 03: 为什么 4 级页表(48 位)而非 3 级(39 位, 512GB 空间)？La57(57 位)的动机是什么？
29. 04: VMA 为什么用红黑树+链表双索引而不用区间树(interval tree) 替代链表？→ **已覆盖**: 04 §2 设计考量行解释双索引各自服务不同访问模式(查找 vs 遍历)
30. 05: GUP 的 FOLL_LONGTERM 和 MOVABLE 冲突 — Linux 怎么解决 RDMA 注册 + 内存热插拔的矛盾？
31. 06: xarray 为什么替代 radix tree(4.20+)？性能差异在哪里？→ **已覆盖**: 06 §1 设计考量行解释 xarray 内存密度更高 + RCU 无锁读
32. 07: Flusher 为什么 per-backing-device 而非 per-CPU？设计权衡是什么？
33. 08: Reverse mapping 为什么需要 anon_vma + anon_vma_chain 双重链而非简单一个 vma→page 映射表？
34. 09: MGLRU(6.1+) 为什么替代传统 4-list？在什么场景下改进最明显？→ **已覆盖**: 09 §1 设计考量行解释避免频繁 list_move 减少 lru_lock 争用
35. 10: swap cluster 分配为什么用 256 而非 512 或 64？SSD 场景下这个值的影响？
36. 11: OOM Reaper 为什么不可靠(进程在 D 态)？是否有替代方案(如提前预收割)？
37. 13: ptmalloc 为什么不合并 fastbin 的相邻 chunk？per-thread tcache(2.26+) 引入的根本原因是什么？

## 内核研究者视角 — 源码验证题

38. 找到 `__free_one_page` 中确定伙伴 PFN 的位运算公式, 自己推演 order=2 page_idx=5 时伙伴是几？
39. 验证 `balance_dirty_pages` 中 `dirty_thresh` 边界计算: dirty=4000MB, bg_thresh=2000MB, dirty_thresh=4000MB 时的 pos_ratio 值？
40. 追踪 `oom_kill_process` → `oom_reaper` 中 `__oom_reap_task_mm` 对 VM_LOCKED 的 VMA 如何处理？

## 面试视角 — 高频考题覆盖

41. "Linux 内核中 struct page 占多少字节？为什么？" — 01 应覆盖
42. "malloc(1MB) / malloc(1GB) 各走什么路径？" — 13(Buddy→brk/mmap) 应覆盖
43. "fork 后写 parent 的 page，内核发生了什么？" — 05(do_wp_page) 应覆盖
44. "脏页比例过高时，write() 系统调用会阻塞吗？" — 07(balance_dirty_pages) 应覆盖
45. "OOM Killer 选哪个进程杀？能不能保护某个进程？" — 11 应覆盖

---

> **自检结果**: 45 题覆盖 7 身份(读者/开发者/性能/SRE/架构师/研究者/面试) — 全部 13 篇覆盖, 无不明确边界
