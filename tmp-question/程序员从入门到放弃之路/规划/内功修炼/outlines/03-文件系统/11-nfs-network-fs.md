# 网络文件系统 — NFS 如何把远端树投影成本地 VFS，FUSE 又如何把文件系统搬到用户态

> Cluster B: 3 KPs | 依赖: 10-mount-permissions | 读者基线: 理解 VFS/挂载/root dentry/权限判定
> 读者处境: 10 篇刚讲完本地块设备如何通过 `mount` 接进 VFS；本篇回答另一种情况——如果文件根本不在本地磁盘，而在另一台机器上，VFS 还怎么保持同一套 `open/read/write` 接口？
> 打开新视角: NFS 是"把远端文件树伪装成本地 inode/dentry"，FUSE 是"把文件系统实现搬到用户态"；前者解决跨机器共享，后者解决开发灵活性

---

### 概念依赖链

```
10-mount-permissions(VFS 挂载树) → 本篇: 远端文件系统 / 用户态文件系统
  ├─ §1 NFS 挂载(root file handle → inode/dentry)
  ├─ §2 NFSv3 vs NFSv4(无状态 RPC → 有状态 COMPOUND)
  ├─ §3 cache / delegation(一致性与性能的权衡)
  └─ §4 FUSE(/dev/fuse 把 VFS 请求转交用户态 daemon)
先讲: 远端根如何挂进 VFS → 协议怎么传操作 → 性能/一致性权衡 → 用户态文件系统另一条路
后续依赖: 12-object-distributed-fs(当元数据/数据分散到几十上百台机器)
```

### 叙事顺序

1. 问题引入——`ls /mnt/nfs` 看起来和 `ls /mnt/ext2` 一样，但数据明明在远端服务器磁盘上：VFS 是怎么被“骗”成同一套 inode/dentry 语义的？（**Aha: 网络文件系统的核心不是把块搬过来，而是把远端对象包装成本地 VFS 能理解的 file handle / inode / dentry**）
   - 过渡: 先看挂载那一刻远端根是怎么接进本地命名空间的
2. NFS 挂载——把远端根 file handle 接成 `sb->s_root`
   - 过渡: 接进来之后，读写操作是怎么在网络上传的？
3. NFSv3 vs NFSv4——无状态 RPC 到有状态 COMPOUND
   - 过渡: 远端每次都 RPC 太慢，客户端靠什么减少往返？
4. 属性缓存 / delegation——一致性与性能的交换
   - 过渡: 还有一种更激进的路——文件系统根本不在内核里实现，行不行？
5. FUSE——通过 `/dev/fuse` 把 VFS 请求转给用户态 daemon
   - 过渡: 收束
6. 收束——远端树投影 vs 用户态实现 + Aha Moment

### 1. NFS 挂载 — 先把远端根目录变成本地 `sb->s_root`

场景提示: `mount -t nfs server:/export /mnt` 之后，为什么本地 VFS 立刻就能从 `/mnt` 往下 `lookup`？ [写作时展开]

关键设计: NFS 客户端并不是先把整个目录树拉下来，而是先拿到远端根的 file handle，再把它包装成 inode/dentry（fs/nfs/*）：

```[pseudocode]
mount -t nfs server:/export /mnt
  → nfs_fs_type.mount / nfs4_mount
  → 向服务端发 root/export 获取请求
  → 得到 root file handle + attributes
  → nfs_fhget(sb, fh, fattr)
      把远端对象包装成本地 inode
  → d_make_root(inode)
  → sb->s_root = root dentry
```

Why: 为什么 NFS 挂载时不需要先把整棵目录树同步下来？——**因为客户端真正需要的只是入口句柄**：只要有根 file handle，就能在后续 LOOKUP 时一步步向下解析；目录项、inode 属性、数据页都可以按需拉取。**所以 NFS 挂载的本质也是 10 篇的那套三件套：`super_block + root inode + root dentry`，只是 inode 的后端从本地磁盘换成了远端 RPC。** [内核: 10 篇 ext2 通过 `ext2_fill_super + ext2_iget` 构根；NFS 则通过 `nfs_fhget` 构根]

比喻锚点: NFS 挂载像拿到远端仓库的大门钥匙和第一张楼层平面图——不必先把整仓货物搬到本地，只要先能进门，后面缺哪件再远程查。 [写作时展开]

### 2. NFSv3 vs NFSv4 — 从无状态 RPC 到有状态 COMPOUND

场景提示: 远端打开一个文件，如果每一步都独立发网络请求，LOOKUP 一次、OPEN 一次、READ 一次，往返不是太多了吗？ [写作时展开]

关键设计: NFSv3 和 NFSv4 的核心分水岭，是"无状态逐操作 RPC"到"有状态复合操作"：

```[pseudocode]
NFSv3
  LOOKUP(dirfh, name)
  READ(fh, offset, count)
  WRITE(fh, offset, count, stable)
  ...
  特点: file handle 是核心, 协议尽量无状态

NFSv4
  COMPOUND [PUTFH, LOOKUP, OPEN, READ, CLOSE, ...]
  + clientid / stateid / delegation
  特点: 一次 RPC 可携带多步操作, 并显式维护客户端状态
```

Why: 为什么 NFSv4 要引入状态和 COMPOUND，看起来比 v3 复杂很多？——**因为网络 RTT 比本地函数调用贵几个数量级**：如果一次 `open+read` 要拆成 3~5 次 RPC，延迟会直接吞掉吞吐；COMPOUND 的目标就是把多步语义压缩成一次往返。**代价**：协议不再纯无状态，客户端/服务端都要维护 `clientid/stateid/delegation` 这套运行时状态机。 [内核: v3 的核心是 opaque file handle；v4 在 handle 之上再叠一层会话/状态语义]

比喻锚点: NFSv3 像每办一件事都重新排一次号；NFSv4 COMPOUND 像一次窗口受理把"查档→开单→付款→取件"一口气办完。 [写作时展开]

### 3. cache / delegation — 为什么网络文件系统既想快，又总怕不一致

场景提示: 远端文件如果每次 `stat/read` 都 RPC，NFS 会慢到不可用；但本地缓存多了，又会不会读到旧数据？ [写作时展开]

关键设计: NFS 客户端靠属性缓存、页缓存、delegation 这三层减少 RPC，但每层都在拿一致性换性能：

```[pseudocode]
属性缓存
  inode attr / dentry / readdir 结果在本地短暂缓存
  过期后再向服务端 revalidate

页缓存
  读过的数据页留在本地 Page Cache
  不是每次 read 都重新走网络

delegation (主要 NFSv4)
  服务端把某个文件一段时间的读/写主导权下放给客户端
  客户端可本地处理更多操作
  服务端 recall 时再交还
```

Why: 为什么 NFS 无法像本地 ext2 一样轻易承诺"看到的永远是最新"？——**因为本地缓存和远端共享天生冲突**：你想减少网络往返，就必须在客户端保留旧属性/旧页；一旦多客户端并发访问，就需要 revalidate、close-to-open、delegation recall 等机制修补一致性。**这就是网络文件系统的宿命：不是没有一致性，而是一致性总带着 RTT 成本。** [内核: 04/05 篇的页缓存到 NFS 这里仍然存在，只是数据来源从块设备变成了 RPC]

比喻锚点: NFS 缓存像多人共用的在线文档本地草稿——本地开缓存越多，编辑越流畅；但别人改了内容后，你的草稿就越容易过期，需要同步或被服务器召回。 [写作时展开]

### 4. FUSE — 不是把文件系统放到远端，而是把实现搬到用户态

场景提示: 如果你想做一个奇怪的文件系统（比如把 S3、微信聊天记录、加密容器挂成目录），为啥不直接写内核模块，而要用 FUSE？ [写作时展开]

关键设计: FUSE 不是一种远端协议，而是一条内核↔用户态的文件系统总线：

```[pseudocode]
VFS 请求(getattr/read/write/readdir/...)
  → fuse 内核模块
  → /dev/fuse
  → 用户态 daemon(libfuse/自定义服务)
  → daemon 处理后把结果写回 /dev/fuse
  → 内核把结果回填给 VFS
```

Why: 为什么 FUSE 极灵活，却总比内核文件系统慢？——**因为每次操作至少多了两次用户态/内核态往返**：VFS 进内核 → 转发到用户态 daemon → 再把结果写回内核；若后端还是网络/对象存储，就还要再叠一层 RTT。**它的价值不在极致性能，而在'把开发难度从内核级降到用户态级'。**

比喻锚点: FUSE 像给 VFS 装了一个翻译耳机——内核不会你的业务语言，就把请求通过 `/dev/fuse` 发给用户态翻译员；翻译员再把结果翻回来。方便是方便，但每句话都得来回转述。 [写作时展开]

### 5. 收束

回到"远端文件系统如何看起来像本地目录"：
- NFS：远端对象靠 file handle + RPC，被包装成本地 inode/dentry
- NFSv4：在 v3 的 handle 之上加入状态、COMPOUND、delegation，减少 RTT
- FUSE：不是远端协议，而是把文件系统实现从内核挪到用户态

完整对照：

```[pseudocode]
本地 ext2
  block device → ext2_fill_super → ext2_iget → sb->s_root

NFS
  remote handle → nfs_fhget → sb->s_root
  后续操作 = RPC + 本地缓存

FUSE
  VFS 请求 → /dev/fuse → 用户态 daemon → 回填结果
```

**Aha Moment**: "网络文件系统真正神奇的地方，不是把远端块设备搬到本地，而是**把远端对象伪装成了本地 VFS 能理解的 inode/dentry 树**；而 FUSE 则更进一步：它连文件系统的'解释器'都不放在内核里，而是放到了用户态。一个解决'文件在别处'，一个解决'逻辑在别处'。"
**回答读者三问**: ①NFS 为什么能像本地目录一样用=远端 file handle 被包装成本地 inode/dentry；②NFSv4 为什么更快= COMPOUND + delegation 减少 RTT；③FUSE 为什么灵活但慢=每次请求都要多走一圈用户态桥。

---

### 核心悬念

**"如果文件数据和元数据不只在一台远端服务器，而是分散到几十台机器上——CephFS、对象存储、Haystack 这类分布式文件/对象系统又如何放弃单一 inode 树，改用 CRUSH、append-only log、metadata server 分层来扩展到海量数据？"**

→ 引出 12-object-distributed-fs — 对象存储与分布式文件系统——远端单机树讲完，下一篇进入多机分布式世界。