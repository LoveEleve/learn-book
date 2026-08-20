# 读数据流程 — 页缓存命中、未命中与预读如何配合

> Cluster A: 2 KPs | 依赖: 04-write-data-flow | 读者基线: 理解页缓存/folio/buffer_head 概念
> 读者处境: 04 篇讲完了写路径——字节先写进 Page Cache；本篇回答反方向的问题：`read()` 第一次读文件时，页是怎么从磁盘被搬进内存的？
> 打开新视角: 读路径不是"每次 read 都打磁盘"，而是三态切换——页缓存命中(0 I/O)、未命中(同步读盘)、预读命中(上一次顺手替你读好了)

---

### 概念依赖链

```
04-write-data-flow(Page Cache/dirty/writeback) → 本篇: 读路径与预读
  ├─ §1 read_iter/filemap_read(统一读入口)
  ├─ §2 页缓存命中(0 I/O fast path)
  ├─ §3 页缓存未命中(ext2_read_folio → ext2_get_block → submit_bio)
  └─ §4 sync/async readahead(把下一页提前搬进缓存)
先讲: 入口 → 命中 → 未命中 → 预读
后续依赖: 06-文件删除(回收前页缓存与块回收如何衔接)
```

### 叙事顺序

1. 问题引入——第一次 `read(fd, buf, 4096)` 时，用户缓冲区明明是空的，内核怎么把磁盘上的 4KB 搬进来？（**Aha: `read()` 的核心不是"拷贝"，而是先确保目标 folio 在 Page Cache 里**）
   - 过渡: 统一入口先走到哪？
2. `sys_read` → `filemap_read`——读路径总入口
   - 过渡: 先看最便宜的情况——页缓存里已经有这页
3. 页缓存命中——0 I/O fast path
   - 过渡: 没命中才是第一次读文件的关键——谁负责把页读进来？
4. 页缓存未命中——`ext2_read_folio` + `ext2_get_block`
   - 过渡: 一次缺页只读当前页太亏——内核怎么顺手多读几页？
5. 同步/异步预读——顺序读的两级加速
   - 过渡: 读链路已经齐——收束
6. 收束——命中/未命中/预读三态 + Aha Moment

### 1. `sys_read` → `filemap_read` — 统一读入口

场景提示: 用户态执行 `read(fd, buf, 8192)`，这个 8KB 是直接从块设备拷到用户空间，还是先进入页缓存？ [写作时展开]

关键设计: ext2 和大多数本地文件系统共享通用读路径（fs/read_write.c + fs/ext2/file.c + mm/filemap.c）：

```[pseudocode]
sys_read(fd, buf, count)
  → ksys_read → vfs_read
  → file->f_op->read_iter = ext2_file_read_iter
  → generic_file_read_iter → filemap_read(iocb, iter, already_read)
      while (还有数据没读完):
        filemap_get_pages(...)   // 先确保目标 folio 在页缓存里
        copy_folio_to_iter(...)  // 再拷到用户 buf
```

Why: 为什么读路径也像写路径一样复用 `filemap_read`？——**读的通用性更强**：页缓存命中判断、copy_to_user、顺序读检测、同步/异步预读几乎对所有本地文件系统都一样；具体文件系统只需要提供 `read_folio/readahead/get_block`，回答"这页对应哪些磁盘块"。 [内核: 与 04 篇相反——写路径先 copy 再脏标记，读路径先确保 folio 存在再 copy 给用户]