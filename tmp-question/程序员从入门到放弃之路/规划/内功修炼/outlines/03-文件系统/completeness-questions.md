# 文件系统 域 — 全视角完备性验证

> Generated from 12 Per-Article Outlines covering 20 KPs
> Date: 2026-08-08

---

## 一、开发者视角 (Developer) — 代码级完备性

1. `ext2_super_block` 的 `s_log_block_size` 如何从磁盘值映射到内核块大小？`ext2_fill_super` 中的 `sb_set_blocksize` 对这个值的调用顺序是什么？
2. `ext2_get_block` 中 `create=1` 与 `create=0` 的分支差异——未命中时 `bh_result->b_blocknr` 被设为什么值？
3. `ext2_block_to_path` 返回的 `offsets[]` 数组的各元素分别代表第几级偏移？`boundary` 参数在什么条件下为 1？
4. `ext2_add_link` 中的空洞合并算法：`pde->rec_len += de->rec_len` 后如何处理边界对齐（rec_len 必须是 4 的倍数）？
5. `__block_commit_write` 中 `SetPageUptodate` 和 `mark_buffer_dirty` 的调用顺序是否重要？反过来会怎样？
6. `block_read_full_folio` 创建的 `buffer_head` 数组中, 每个 bh 的 `b_blocknr` 如何通过 `ext2_get_block` 映射到物理块？
7. `ext2_free_branches` 三重递归中, 如果某一层的 `sb_bread` 失败, 已释放的 bitmap 位能回滚吗？
8. JBD2 `jbd2_journal_get_write_access` 对 `bh->b_data` 的 copy 存在哪？为什么需要 copy 而非直接用原 bh？
9. Btrfs `csum_tree_block` 计算的校验和存在哪个元数据区域？读时 `csum_verify` 失败后如何定位 mirror 副本？
10. FUSE `fuse_dev_read` → `copy_to_user` 的请求结构 `fuse_in_header` + `fuse_write_in` 在用户态如何解析回包？
11. NFS v4 COMPOUND 中 `SEQUENCE→PUTFH→READ` 的 XDR 编码——`SEQUENCE` 操作的 slot table 和 session 如何关联？
12. Ceph CRUSH 的 `PG::acting` 和 `PG::up` 的区别——OSD 故障时两者何时分离？`peering` 过程如何收敛？

## 二、性能视角 (Performance) — 瓶颈与优化

1. ext2 间接块解析的 IO 次数: 4KB block, 读取文件的第 4GB 偏移, 需要多少次 `sb_bread`？
2. `balance_dirty_pages_ratelimited` 的触发阈值: `dirty_ratio` 和 `dirty_background_ratio` 分别在哪个百分比触发阻塞 vs 后台 writeback？
3. ext4 延迟分配的大 extent 合并: 连续 10 次 4KB write 在 ext2(10 个间接块新区块) 和 ext4(1 个 40KB extent) 的 `bio` 数量差异？
4. `page_cache_sync_readahead` vs `page_cache_async_readahead` 的区别: 什么条件下同步预读会阻塞当前 read？
5. Btrfs COW 的写放大: 修改一个 4KB 文件的数据块——从 leaf block 到 root 一共产生了多少 KB 的新写入？
6. ZFS `zfs send -i` 增量传输的块粒度——是否比 `rsync` 更高效？在什么场景下 ZFS send 的优势消失？
7. NFS v4 write delegation 延迟: 客户端获取 delegation → 本地写 → return delegation → 服务端 commit, 这个周期的最坏延迟是多少个 RTT？
8. FUSE 的上下文切换开销: `read(fuse_fd) → daemon → write(fuse_fd)` 的最少切换次数？与内核原生 `ext2_read_folio` 的对比？

## 三、SRE/运维视角 (SRE) — 故障与恢复

1. ext2 无日志时, `ext2_truncate` 递归释放间接块中途崩溃——fsck 如何检测 bitmap 与 indirect block 的不一致？
2. ext2 orphan list 的 `ext2_orphan_cleanup` 在挂载时执行——如果一个 inode 在 orphan list 上但目录项已经不存在, 会怎么处理？
3. ext4 `data=ordered` 模式下, 数据 flush 与 journal commit 的顺序——如果数据 flush 成功但 journal commit 前崩溃, replay 后文件内容是否完整？
4. JBD2 checkpoint: journal 空间满时, `kjournald2` 如何选择性回收已 checkpoint 的事务块？`j_tail` 推进的触发条件？
5. Btrfs 快照的空间回收: 删除快照后, 共享的 extent 何时被真正释放？`btrfs-cleaner` 内核线程的角色？
6. LVM 快照的 COW 区溢出: 当 COW 区满载(原卷有超过 `-L 2G` 的修改量)时会发生什么？快照是否自动失效？
7. NFS v4 state 丢失: 服务端重启后 `clientid` 和 `stateid` 不匹配——客户端的 recovery 流程(`nfs4_state_manager`) 如何处理？
8. Ceph OSD 故障恢复: primary OSD down → PG `peering` → 新 primary 推选 → `backfill` 数据追赶——这个过程对客户端 IO 的延迟影响？

## 四、架构师视角 (Architect) — 设计决策

1. ext2 为什么选择 SuperBlock 只在 3^n BG 备份而非每个 BG 都备份？备份策略的设计依据是什么？
2. ext2 的 `i_block[15]` 设计: 为什么是 12 直接/1 单重/1 双重/1 三重而非 15 全直接？这个比例如何随 block size 变化？
3. ext4 为什么选择 extent 树而不直接替换为 B+tree？extent 树与 XFS B+tree 在操作复杂度上的 tradeoff？
4. JBD2 为什么用环形缓冲区而非 append-only log？环形缓冲区的 `j_head`/`j_tail` 回收策略有什么副作用？
5. Btrfs COW 与 ZFS COW 的根切换机制差异: Btrfs 的树根指针更新是单步原子操作, ZFS 的 uberblock 是双轮辋(ring)——为什么 ZFS 选双轮辋？
6. Haystack 为什么选择 append-only log + needle_index 而非一个扁平的 key-value store (如 LevelDB)？设计约束是什么？
7. Ceph CRUSH 用确定性哈希定位, GlusterFS DHT 用客户端哈希——为什么 Ceph 不选 DHT？MDS 的存在引入了什么 Ceph 独有的能力？
8. FUSE 的性能损失(2×上下文切换)是否值得其灵活性？在什么情况下应该用内核原生文件系统而非 FUSE？

## 五、研究者视角 (Researcher) — 深层追问

1. ext2 间接块链是否可以扩展为"间接块 B+tree"？技术上需要改哪些 struct？这与 extent tree 在什么维度上等价/不等价？
2. 页缓存脏页的 `dirty_expire_interval` 和 `dirty_writeback_interval` 在高 IO 负载下的交互——什么条件下 writeback 线程来不及回写导致 `balance_dirty_pages` 永久阻塞写进程？
3. Btrfs 的 CoW 和 ZFS 的 CoW 都声称 O(1) 快照——如果 1TB 文件的 1 个字节被修改, Btrfs 遍历到那个 leaf block 需要多少次 IO？这个"O(1)"是常数还是 O(log N)?
4. CRUSH 算法的 `crush_do_rule` 中 `choose` 步骤的 `r` (replica number) 参数——当 OSD 故障后 `r` 重试如何保证数据迁移量最小？
5. distributed filesystem 的 CAP 取舍: CephFS(CP) vs GlusterFS(AP, 无 MDS) vs NFS v4(CP, 单点 MDS)——它们各自在哪一类故障下不保证 linearizability？

## 六、编译器/底层视角 (Compiler/Low-Level)

1. `struct ext2_super_block` 在磁盘上的字节序是 little-endian (`__le32`), 挂载时 `le32_to_cpu` 转换——如果内核跑在大端平台(如某些 PowerPC), 这个转换在哪里发生？
2. `i_block[15]` 中 block 号从磁盘的 `__le32` 到内存 `sector_t` 的转换——`ext2_get_block` 如何保证 block 号不溢出 32 位？
3. `buffer_head.b_blocknr` 的类型是 `sector_t`(64 位)——ext2 的 32 位 `i_block` 存储 → 64 位 `sector_t` 的转换有溢出风险吗？

## 七、学生视角 (Student) — 学习路径完备性

1. 如果读者没有操作系统基础(不知道什么是内核/用户态), 01-ext2-disk-layout 的读者基线"理解磁盘概念"是否充分？
2. 从 02(inode)→03(touch)→04(write)→05(read)→06(unlink) 的链路是否遗漏了 `stat/fstat` 系统调用？它不修改文件但返回 inode 元数据——是否该加一篇？
3. 从 06 直接桥接到 08(JBD2) 跳过了 07(文件系统对比)——07 的选型知识先于日志细节是否合理？
4. 11(FUSE) 中 `fuse_main` 示例是内存文件系统——是否需要给一个实际的 sshfs 使用步骤？
5. 12(分布式) 桥接引出分布式专题——读者从这里进入分布式专题的知识断层是什么？需要在这里补一个"分布式文件系统与本地文件系统的接口对比表"吗？

---

## 跨篇承诺链验证 (Cross-Article Commitment Check)

| From | To | 承诺 | 是否兑现 |
|------|-----|------|:--:|
| 01: 核心悬念 | 02 | "i_block[15] 如何让 2 字节 inode 承载 4TB 文件" | ✅ 02 §2 四种寻址层级 |
| 02: 核心悬念 | 03 | "VFS 如何穿透 dentry cache→inode→bitmap" | ✅ 03 §1-§4 |
| 03: 核心悬念 | 04 | "write() 返回成功时数据在 Page Cache 哪里" | ✅ 04 §2-§4 |
| 04: 核心悬念 | 05 | "read 首次未命中时 ext2_get_block 如何捞 page" | ✅ 05 §4 |
| 05: 核心悬念 | 06 | "rm 后三重间接链如何递归释放" | ✅ 06 §3 |
| 06: 核心悬念 | 08 | "JBD2 如何把 O(n) 崩溃恢复变 O(1)" | ✅ 08 §1-§5 |
| 08: 核心悬念 | 09 | "Btrfs COW 如何不用 journal 实现崩溃一致性" | ✅ 09 §1-§2 |
| 09: 核心悬念 | 10 | "mount 全链路 SB→root inode→dentry" | ✅ 10 §1-§3 |
| 10: 核心悬念 | 11 | "NFS v4 COMPOUND 合并 RPC" | ✅ 11 §2 |
| 11: 核心悬念 | 12 | "Ceph CRUSH 哈希定位, Haystack append-only log" | ✅ 12 §2-§3 |
| 12: 核心悬念 | — | 引出分布式专题 | ✅ 12 §6 |

---

## 未覆盖 KP 检查

来源文件 `outline-03-文件系统.md` 20 KPs 对照:

| KP | 落地大纲 |
|----|:--:|
| Ext2 磁盘布局 (SuperBlock+GDT+Bitmap) | ✅ 01 |
| Inode 结构 + 间接块链 + 目录项 | ✅ 02 |
| 文件创建全链路 | ✅ 03 |
| 写数据流程 + ext4 延迟分配 | ✅ 04 |
| 读数据流程 | ✅ 05 |
| 文件删除 + xattr + 文件锁 | ✅ 06 |
| 文件系统对比 (ext4/XFS/Btrfs/ZFS) | ✅ 07 |
| JBD2 架构 + 三种日志模式 | ✅ 08 |
| Btrfs COW/快照/checksum + ZFS/LVM 快照 | ✅ 09 |
| 挂载全流程 + 权限管理 (RWX/ACL) | ✅ 10 |
| NFS v3/v4 协议栈 + file handle + delegation + pNFS | ✅ 11 |
| FUSE 用户态文件系统 | ✅ 11 |
| 对象存储 (S3/Haystack) | ✅ 12 |
| CephFS (CRUSH) + GlusterFS (DHT) | ✅ 12 |

**覆盖率: 20/20 = 100%** ✅
