# 文件系统对比 — ext4 / XFS / Btrfs / ZFS 四代进化各自解决了什么痛点

> Cluster A→B 过渡: 4 KPs | 依赖: 01-06 ext2 全链路 | 读者基线: 理解 ext2 的三大限制（间接块、位图、无日志）
> 读者处境: 前 01-06 篇把 ext2 全链路讲透了；本篇回答"为什么后来没人满足于 ext2——每一代新文件系统到底在修 ext2 的哪根短板"
> 打开新视角: 文件系统的演化不是版本号升级，而是三条主线的替换——索引结构（indirect→extent/B+tree）、一致性（fsck→journal→COW）、扩展性（单组位图→分配组/存储池）

---

### 概念依赖链

```
01-06 ext2 全链路 → 本篇: 四代文件系统对比
  ├─ §1 ext4(先修 ext2 三大痛点: 间接块/碎片/无日志)
  ├─ §2 XFS(把单机文件系统做成 64 位 B+tree 引擎)
  ├─ §3 Btrfs/ZFS(COW + checksum + snapshot 的另一条路线)
  └─ §4 选型决策(把技术差异落到场景)
先讲: ext2 的增量改良(ext4) → 另一派高并发引擎(XFS) → 第三派 COW 完整性(Btrfs/ZFS) → 选型
后续依赖: 08-jbd2-journal(ext4 日志原子性) / 09-cow-snapshot(COW 快照)
```

### 叙事顺序

1. 问题引入——如果 ext2 已经能创建、读写、删除文件，为什么还要有 ext4、XFS、Btrfs、ZFS？（**Aha: 新文件系统不是为了"能工作"，而是为了在大文件、崩溃恢复、快照校验、并发扩展上不再被 ext2 卡死**）
   - 过渡: 第一代改良最直接——先把 ext2 最痛的三根刺拔掉
2. ext4——在 ext2 之上补 extent、延迟分配、JBD2
   - 过渡: ext4 还是 inode+块组家族；如果目标是极限大文件和多核并发呢？
3. XFS——把文件系统做成 64 位 B+tree 引擎
   - 过渡: ext4/XFS 仍是 overwrite-in-place；如果追求快照、校验、自愈怎么办？
4. Btrfs / ZFS——COW + checksum + snapshot 路线
   - 过渡: 三条路线都看完了——真正落地时怎么选？
5. 选型决策——技术差异到场景映射
   - 过渡: 选型已明——收束
6. 收束——三条进化主线 + Aha Moment

### 1. ext4 — 先把 ext2 的三大痛点修掉

场景提示: ext2 在前 01-06 篇里暴露了三个硬伤：大文件靠三重间接块、随机小写易碎片、unlink 崩溃只能 fsck 全盘扫。ext4 第一刀砍向哪里？ [写作时展开]

关键设计: ext4 不是推翻 ext2，而是在同一家族内替换三块核心机制：

```[pseudocode]
ext2 的痛点
  1) i_block 三重间接块 → 大文件随机访问层级深
  2) 立即分配 → 小写碎片多
  3) 无日志 → 崩溃恢复靠全盘 fsck

ext4 的三把刀
  1) extent 树: ext4_extent { logical_start, len, physical_start }
  2) delayed allocation + multiblock allocator
  3) JBD2 journal: 元数据事务提交/重放
```

Why: 为什么 ext4 选择"渐进改良 ext2"而不是像 XFS/Btrfs 那样另起炉灶？——**兼容性红利巨大**：块组、inode、目录项家族都保留，工具链、迁移路径、运维经验几乎可以平滑继承；同时把最疼的三个点逐个替换，就能覆盖 80% 通用场景。**extent 是对 02 篇三重间接块的直接否定**：连续 1000 个块，不必记 1000 个指针，只记一条 `(起点, 长度)`。 [内核: 04 篇里 ext4 已经露过脸——delayed allocation 就是 ext2 立即分配的替代方案]