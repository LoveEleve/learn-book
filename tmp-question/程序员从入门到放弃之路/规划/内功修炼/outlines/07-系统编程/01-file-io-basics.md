# 文件 I/O 基础 — open/read/write 全语义 + fsync/O_SYNC/Direct I/O + stdio 缓冲 + 文件描述符

> Cluster A: 6 KPs | 依赖: 无 | 读者基线: C 语言基础 + 会用 fopen/fread 但没理解到内核层

---

### 1. open/read/write — 系统调用的完整语义
  - `fd = open(path, O_RDWR | O_CREAT | O_TRUNC, 0644)` — 返回最小可用 fd，errno 精确区分 ENOENT/EACCES/EEXIST (man 2 open)
  - `ssize_t n = read(fd, buf, count)` — 关键坑: `n < count` 是 Partial Read（不是错误），pipe/socket 常见；`n == 0` 是 EOF (man 2 read)
  - `ssize_t n = write(fd, buf, count)` — Partial Write 同理: 磁盘满会导致 `n < count`，必须循环写；`O_NONBLOCK` 下 socket write 可返回 EAGAIN (man 2 write)
  - 内核路径: VFS → 页缓存(page cache)写入 → 标记 dirty → 后台回写(pdflush/writeback kthread) (B1 Ch2 §1-3)
  - `errno` 系统: `perror()` / `strerror()` / 线程安全(`_r` 版本)——系统调用返回 -1 时 `errno` 才有效 (man 3 errno)

### 2. 同步 I/O — 什么时候"写磁盘"真的写到了磁盘
  - `fsync(fd)`: 文件数据 + 元数据(大小/时间戳)→磁盘；`fdatasync(fd)`: 只刷数据 + 大小元数据(不刷时间戳)，性能更好 (man 2 fsync)
  - `open(..., O_SYNC)`: 每次 write 等价 write+fsync — 每字节都等磁盘确认，吞吐急剧下降 (man 2 open)
  - `O_DSYNC`: 同上但只同步数据——fdatasync 等效——中间态; `O_DIRECT`: 绕过页缓存直接 DMA → 磁盘，无缓冲 kernel buffer (man 2 open)
  - 内核路径: `__generic_file_fsync` → `sync_inode_metadata` → block layer → 磁盘控制器 (fs/fs-writeback.c)
  - 选择指南: 关键元数据 → fsync; 只关心数据不关心 mtime → fdatasync; 自做缓存(数据库)→ O_DIRECT

### 3. 用户态缓冲 — stdio 的三层缓冲机制
  - 全缓冲(block buffered): 默认 8192 字节，BUFSIZ → 填满才 flush → `fwrite` 后不 flush，crash 时数据丢失 (man 3 setbuf)
  - 行缓冲(line buffered): `\n` 触发 flush → 终端(stdout 连终端时)默认行缓冲 → `printf("hello")` 不输出等第一个 `\n` (man 3 setvbuf)
  - 无缓冲: stderr 默认无缓冲 → `fprintf(stderr, "error\n")` 立即输出 (man 3 setbuf)
  - `setvbuf(fp, buf, _IOFBF/_IOLBF/_IONBF, size)` — 手动设缓冲策略；`fflush(fp)` 强制刷新 (man 3 fflush)
  - 内核对比: stdio 缓冲在用户态 → write() 进入内核态 → 内核页缓存(第二次缓冲) → 磁盘——数据丢失有双重"窗口"

### 4. close/lseek/文件描述符 — 资源管理
  - `close(fd)`: 释放 fd + 递减打开文件计数 → 计数归零才真关闭 → 关键坑: fork 后父子共享 fd，两个 close 才真正关闭 (man 2 close)
  - `off_t lseek(fd, offset, SEEK_SET/SEEK_CUR/SEEK_END)` — 超过文件大小写入 → sparse file(空洞文件): `ls -l` 显示大但 `du` 小 (man 2 lseek)
  - `pread/pwrite`: 原子定位读写("lseek+read/write 的两步合并为一步")——多线程安全 (man 2 pread)
  - fd 泄漏检测: `lsof -p PID` → `/proc/PID/fd/` → `ulimit -n` 上限 1024(默认) → 到上限时 accept 返回 EMFILE (B1 Ch2 §6-8)

### 5. 收束
  - open/read/write 三个系统调用是所有 UNIX I/O 的基础——socket/pipe/fifo 全部复用这组接口和 fd 表
  - "同步"在操作系统里有三层: stdio fflush(用户态)→fsync(页缓存→磁盘)→O_DIRECT(绕过页缓存)——每层代价递增
  - 一句话记住 errno: 只在系统调用返回 -1 时有效，库调用不会设置 errno(除非是系统调用的 wrapper)

---

### 核心悬念
**"read/write 走到内核，但每次 1 字节太慢，每次 1MB 内存浪费——有没有办法让内核直接映射文件到进程地址空间，零拷贝读写？"**

→ 引出 02-mmap + epoll + inotify — 零拷贝 I/O 与文件监控
