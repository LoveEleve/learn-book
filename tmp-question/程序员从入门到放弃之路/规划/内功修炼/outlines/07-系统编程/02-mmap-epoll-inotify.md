# mmap + epoll + inotify — 零拷贝文件映射 + 高性能事件驱动 + 文件监控

> Cluster A: 6 KPs | 依赖: 01 (fd/系统调用基础) | 读者基线: 理解 open/read/write 和页缓存概念

---

### 1. mmap — 文件映射到进程地址空间
  - `void *p = mmap(NULL, len, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0)` — 文件直接映射到虚拟内存，省去 read/write 的用户态拷贝 (man 2 mmap)
  - MAP_SHARED: 修改 p[i] → 脏页回写 → 磁盘同步；多进程间共享内存 (B1 Ch4 §4-5)
  - MAP_PRIVATE: COW(写时复制)——修改不反映到磁盘；MAP_ANONYMOUS: 无文件后备→等价 malloc 大块(详见 06) (B1 Ch9 §3)
  - `mlock(p, len)` / `munlock`: 锁物理页防换出——mlock 有上限(RLIMIT_MEMLOCK)，需 CAP_IPC_LOCK (man 2 mlock)
  - `msync(p, len, MS_SYNC/MS_ASYNC)`: 强制刷回映射页到磁盘 (man 2 msync)
  - 内核路径: `mm/mmap.c` → do_mmap → VMA 分配 → 缺页中断时 page cache 映射 → 修改后 mark dirty → writeback

### 2. epoll — 三种 I/O 多路复用中的高性能选择
  - `epoll_create1(EPOLL_CLOEXEC)` → `epoll_ctl(epfd, EPOLL_CTL_ADD/EPOLL_CTL_MOD/EPOLL_CTL_DEL, fd, &ev)` → `epoll_wait(epfd, events, max, timeout)` (man 7 epoll)
  - LT (Level-Triggered, 电平触发, 默认): fd 可读 → 一直通知直到读完 → 兼容 select/poll (man 2 epoll_wait)
  - ET (Edge-Triggered, 边缘触发): fd 不可读→可读一次通知 → 必须用非阻塞 I/O + 循环读到 EAGAIN (man 2 epoll_wait)
  - 选择指南: 简单场景(LT)+ 高性能必须(ET)—LT 多一次系统调用但不出 BUG; ET 少通知但难写对(漏事件=死锁) (B1 Ch4 §1-4)
  - 内核: eventpoll 红黑树(注册 fd) + ready list(就绪 fd) + 回调机制——`epoll_ctl` ADD 时在目标 fd 的 wait_queue 上注册回调

### 3. stat/lstat/fstat — 文件元数据查询
  - `struct stat { dev_t st_dev; ino_t st_ino; mode_t st_mode; nlink_t st_nlink; off_t st_size; time_t st_atim/st_mtim/st_ctim; ... }` (man 2 stat)
  - stat vs lstat: stat 跟随符号链接(拿到目标文件信息) vs lstat 不跟随(拿到符号链接自身信息) (man 2 lstat)
  - `S_ISREG(m)/S_ISDIR(m)/S_ISLNK(m)` 宏: 判断文件类型——`st_mode & S_IFMT` 取类型位 (man 7 inode)
  - 常见误用: `stat+open` 有 TOCTOU(time-of-check-time-of-use)竞态——用 `fstat(open(...))` 代替 (B1 Ch8 §1)

### 4. link/unlink/symlink — 硬链接与符号链接
  - `link(target, newname)`: 硬链接——两个目录项指向同一 inode，`nlink` 计数+1 → 文件数据在所有硬链接 unlink+无 fd 打开后才释放 (man 2 link)
  - `symlink(target, linkpath)`: 符号链接——独立文件(inode)存目标路径 → `readlink` 读路径文本 → 悬空链接(dangling)可能(目标不存在) (man 2 symlink)
  - `unlink(path)`: 删除目录项——无其他硬链接且无 fd 打开→真删; 有 fd 打开→等 close 时删(进程内的"临时文件"用法) (man 2 unlink)

### 5. inotify — 文件系统事件监控
  - `inotify_init1(IN_NONBLOCK|IN_CLOEXEC)` → `inotify_add_watch(fd, path, IN_CREATE|IN_DELETE|IN_MODIFY|IN_CLOSE_WRITE|...)` → read 返回 `struct inotify_event` (man 7 inotify)
  - 事件类型: IN_CREATE(创建)/IN_DELETE(删除)/IN_MODIFY(修改)/IN_CLOSE_WRITE(写完关闭)/IN_MOVED_FROM+IN_MOVED_TO(重命名) (man 7 inotify)
  - 关键限制: 只监控单层→递归需 re-add watch; `IN_Q_OVERFLOW` = 队列溢出事件丢失；不能监控/proc/sysfs (B1 Ch8 §5)
  - 与 epoll 集成: `epoll_ctl(epfd, ADD, inotify_fd, ...)` → 统一事件循环

### 6. 收束
  - mmap 省掉用户态↔内核态的拷贝——代价是缺页中断和 TLB 压力，适合大文件随机访问
  - epoll ET+非阻塞 I/O = 现代高并发服务器的标配——从 nginx 到 Redis 都是同一配方
  - inotify 是文件系统级别的"watch"——比轮询 stat 实时且不烧 CPU

---

### 核心悬念
**"fd 能打开文件、epoll 能监控 fd——但进程怎么创建的？fork 后的 fd 表、地址空间、信号处理发生了什么？"**

→ 引出 03-fork + exec + daemon — 进程创建与进程生命周期
