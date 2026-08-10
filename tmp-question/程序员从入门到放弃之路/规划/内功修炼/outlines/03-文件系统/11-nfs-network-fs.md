# 11 — 网络文件系统: NFS 协议栈与 FUSE 用户态文件系统

> Cluster B: 3 KPs | 依赖: 10-mount-permissions | 读者基线: 理解 VFS/挂载/文件操作接口

---

### 1. NFS v3 — 无状态 RPC 协议
  - 协议栈: XDR 编码 → portmap/rpcbind → `NFS_PROGRAM=100003` (`fs/nfs/nfs3xdr.c → nfs3_xdr_enc_read3args`)
  - 核心操作: `NFSPROC3_NULL/GETATTR(fh)/SETATTR(fh,attr)/LOOKUP(dirfh,name)/READ(fh,offset,count)/WRITE(fh,offset,count,stable)` (`include/linux/nfs3.h → nfs3_procedures`)
  - `NFSPROC3_CREATE(dirfh,name,mode)/MKDIR/REMOVE/RMDIR/RENAME/LINK/READDIR/FSSTAT/COMMIT`
  - file handle (fh): 服务端返回, 客户端 opaque, 唯一标识文件 (`fs/nfs/nfs3proc.c → nfs3_proc_read`)

### 2. NFS v4 — 有状态 + COMPOUND 复合操作
  - COMPOUND: `SEQUENCE→PUTFH→OPEN→READ→CLOSE` — 一次 RPC 完成多个操作 (`fs/nfs/nfs4xdr.c → nfs4_xdr_enc_compound`)
  - 状态管理: `stateid`, `clientid`, `OPEN_CONFIRM` — 有状态协议 (`fs/nfs/nfs4state.c → nfs4_alloc_stateid`)
  - Delegation: 客户端获得文件独占权 → 本地读写无需 RPC → return delegation 时提交结果 (`fs/nfs/delegation.c → nfs4_open_delegation`)
  - pNFS: `layout` 机制 — 元数据服务器告知客户端数据在哪个存储服务器 → 客户端直接读写存储 (`fs/nfs/pnfs.c → pnfs_read_done`)
  - 客户端挂载: `mount -t nfs server:/export /mnt → nfs4_proc_get_root(fh) → nfs_fhget(sb, fh, fattr) → inode` (`fs/nfs/nfs4proc.c → nfs4_proc_get_root`)

### 3. 属性缓存与 write delegation — 一致性权衡
  - `nfs_inode->cache_validity = NFS_INO_INVALID_ATTR|NFS_INO_INVALID_DATA` (`include/linux/nfs_fs.h → NFS_INO_INVALID_ATTR`)
  - 缓存参数: `acregmin=3s`(文件属性)→`acregmax=60s`→`acdirmin/acdirmax`(目录属性) — 通过 mount 选项调整 (`fs/nfs/super.c → nfs_parse_mount_options`)
  - write delegation: 客户端独占写 → 服务端 grant → 本地写 → return 提交, 零 RPC
  - async 模式: write 不等待服务端 commit → 数据可能丢失 → sync 模式: 每个 write 等 COMMIT → 安全但慢

### 4. FUSE 内核模块 — `/dev/fuse` 字符设备
  - VFS 请求→`/dev/fuse`→`fuse_ioctl`→用户态 daemon (`fs/fuse/dev.c → fuse_do_ioctl`)
  - 内核发送: `fuse_dev_read(fc, file, buf, count, ppos)` → `copy_to_user` → 用户态 daemon 收到请求 (`fs/fuse/dev.c → fuse_dev_read`)
  - 用户态 daemon: `fuse_session_loop` → 处理 → `fuse_session_reply(req, buf, len)` → `write(fc->pipe, iov, 2)` (`lib/fuse_lowlevel.c → fuse_session_loop`)
  - 内核接收: `fuse_dev_write(fc, file, write_buf, count, ppos) → fuse_request_end` → 唤醒等待 (`fs/fuse/dev.c → fuse_dev_write`)

### 5. `libfuse` ops 全集 — 40+ 操作接口
  - `struct fuse_operations { .init, .destroy, .getattr, .readlink, .mknod, .mkdir, .unlink, .rmdir, .symlink, .rename, .link, .chmod, .chown, .truncate, .open, .read, .write, .statfs, .flush, .release, .fsync, .setxattr, .getxattr, .listxattr, .removexattr, .opendir, .readdir, .releasedir, .fsyncdir, .access, .create, .lock, .utimens, .bmap, .ioctl, .poll, .write_buf, .read_buf, .flock, .fallocate, .copy_file_range, .lseek }` (`include/fuse.h → struct fuse_operations`)
  - 示例 — 内存文件系统: `create=malloc → read=memcpy → write=memcpy_to_fuse → unlink=free` → `fuse_main(argc, argv, &memfs_oper, NULL)`
  - sshfs: `sshfs user@host:/remote /mnt` → SSH → SFTP → fuse 桥接到远程 (`sshfs.c`)
  - 性能代价: 每次 IO 2×上下文切换 + 网络 RTT, 但有极端灵活性

### 6. 收束
  - NFS v4 COMPOUND 把多次 RPC 合并为单次往返, delegation 把一致性维护交给客户端
  - FUSE 把 VFS 请求通过 `/dev/fuse` 字符设备泵给用户态——极致灵活性换取上下文切换开销

---

### 核心悬念
**"NFS 和 FUSE 都是单点 —— 当文件数据分散在几十台服务器上时, CephFS 的 CRUSH 算法如何通过哈希而非元数据服务器定位每个 object 的 OSD？Facebook 的 Haystack 又如何用 append-only log 替代 inode 树来存几十亿张照片？"**

→ 引出 12-object-distributed-fs
