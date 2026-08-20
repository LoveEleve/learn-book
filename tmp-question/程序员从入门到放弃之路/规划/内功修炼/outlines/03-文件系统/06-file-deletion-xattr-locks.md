# 文件删除、xattr 与文件锁 — `unlink` 如何把名字、inode、数据块拆干净

> Cluster A: 4 KPs | 依赖: 02/03/04/05 | 读者基线: 理解 inode/间接块/位图/目录项/页缓存
> 读者处境: 01-05 已经讲完文件怎么被布局、创建、写入、读出；本篇回答闭环的最后一步：`rm a.txt` 之后，名字、inode、数据块分别怎么被回收？
> 打开新视角: 删除文件不是"立刻把内容抹掉"，而是按顺序拆三层关系——目录项解绑、链接计数归零、i_block 指针树递归回收；xattr/ACL 是 inode 之外的元数据外挂，文件锁则是并发访问的协调层

---

### 概念依赖链

```
02 inode/间接块 + 03 创建 + 04 写路径 + 05 读路径 → 本篇: 删除/扩展元数据/锁
  ├─ §1 unlink(先删目录项, 名字与 inode 脱钩)
  ├─ §2 i_links_count / i_dtime(判定是否真正释放)
  ├─ §3 ext2_truncate_blocks / ext2_free_branches(递归回收数据块树)
  ├─ §4 xattr/ACL(i_file_acl 指向外挂元数据块)
  └─ §5 flock/fcntl(文件内容之外的并发协调)
先讲: 名字解绑 → inode 生死 → 数据块递归释放 → 额外元数据 → 并发约束
后续依赖: 08-jbd2-journal(无日志崩溃恢复为什么慢)
```

### 叙事顺序

1. 问题引入——`rm a.txt` 后文件名立刻消失了，但磁盘上的数据块真的马上清零了吗？（**Aha: unlink 删除的是"名字→inode"关系，真正的数据块回收要等链接计数归零后再递归释放**）
   - 过渡: 第一步先删哪本账？——目录项
2. `sys_unlink` → `ext2_unlink`——先从父目录里摘名字
   - 过渡: 名字没了，但 inode 可能还有硬链接——谁决定它死没死？
3. `i_links_count` + `i_dtime`——inode 的生死线
   - 过渡: 真要死时，数据块树怎么拆？
4. `ext2_truncate_blocks` + `ext2_free_branches`——递归回收直接/间接块
   - 过渡: inode 数据面回收完了，额外元数据挂哪？
5. xattr / ACL——inode 之外的外挂元数据块
   - 过渡: 内容与元数据讲完了——并发访问靠什么协调？
6. flock / fcntl——文件锁不属于 ext2 磁盘格式，而属于 VFS 通用锁层
   - 过渡: 删除、元数据、锁三块都齐了——收束
7. 收束——unlink 只是第一刀，真正释放在后面 + Aha Moment

### 1. `sys_unlink` → `ext2_unlink` — 先删目录项，不是先删 inode

场景提示: 你执行 `rm a.txt`，终端马上就看不到名字了——这一瞬间内核最先改的是目录，还是 inode 表，还是 block bitmap？ [写作时展开]

关键设计: unlink 的第一步是把父目录里的目录项删掉（fs/namei.c + fs/ext2/namei.c + fs/ext2/dir.c）：

```[pseudocode]
sys_unlink(path)
  → do_unlinkat → vfs_unlink(dir, dentry)
  → dir->i_op->unlink = ext2_unlink
ext2_unlink(dir, dentry)
  → ext2_find_entry(...): 在父目录页里找到 ext2_dir_entry_2
  → ext2_delete_entry(de, page, page_addr)
      = 把目录项并入前一项的 rec_len / 形成空洞
  → mark_buffer_dirty_inode(..., dir)
```

Why: 为什么 unlink 先删目录项而不是先 free inode？——**用户看到的"文件存在"首先是名字存在**：shell/ls/路径解析都靠目录项找到 inode；所以名字必须先从目录树上摘掉，路径遍历才会立即失效。**这一步只断开名字，不碰数据块**：inode 还可能被别的硬链接引用，或者还被打开着。

比喻锚点: unlink 像先把门牌从楼栋名册上划掉——别人再按名字找不到你了，但房产证（inode）和屋里的家具（数据块）还没立刻被销毁。 [写作时展开]

### 2. `i_links_count` — inode 是否真正死亡，由链接计数决定

场景提示: 同一个 inode 有两个名字（硬链接）时，删掉一个名字为什么文件内容还在？ [写作时展开]

关键设计: unlink 删除目录项后，只是把链接计数减一；只有减到 0，inode 才进入真正回收路径（fs/ext2/namei.c + fs/ext2/inode.c）：

```[pseudocode]
ext2_unlink 成功后
  → inode->i_ctime = dir->i_ctime
  → inode_dec_link_count(inode)   // i_links_count--
  → inode->i_size / i_block 暂时不一定立刻动
inode 最终回收阶段
  → i_nlink == 0 && 无打开引用
  → ext2_evict_inode
      → truncate_inode_pages_final(...)
      → EXT2_I(inode)->i_dtime = now
      → __ext2_write_inode(...)
      → ext2_truncate_blocks(inode, 0)
      → ext2_xattr_delete_inode(...)
      → ext2_free_inode(inode)
```

Why: 为什么要把"名字消失"和"块释放"分成两步？——**因为 inode 是内容对象，目录项只是入口**：硬链接让多个目录项指向同一个 inode；只删一个入口不能杀死内容。**`i_dtime` 是尸检时间戳**：它告诉 fsck/恢复工具这个 inode 已进入删除流程，用于崩溃后的一致性校验。 [内核: 03 篇创建时是把名字绑定到 inode；这里正好反着做——先解绑名字，再等 inode 引用归零]