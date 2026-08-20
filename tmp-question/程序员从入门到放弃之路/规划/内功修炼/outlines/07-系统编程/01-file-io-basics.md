# 文件 I/O 基础 — open、read、write、同步与文件描述符到底保证什么

> Cluster A: 6 KPs | 依赖: 无 | 读者基线: C 语言、会用 `fopen/fread` 但未理解内核层
> 读者处境: 阶段 7 开篇；本篇把用户态 stdio、文件描述符、VFS、页缓存和持久化边界串成一条链
> 打开新视角: `write()` 成功、`fflush()` 完成、`fsync()` 返回和数据真正持久化是不同语义；系统编程首先要分清**数据在哪一层、谁拥有偏移、谁负责错误**

---

### 概念依赖链

```
无前置(阶段7开篇) → 本篇: 文件描述符与 POSIX I/O
  ├─ §1 open/read/write(接口/返回值/partial I/O)
  ├─ §2 fsync/O_SYNC/O_DIRECT(持久化与缓存边界)
  ├─ §3 stdio(fflush/用户态缓冲)
  └─ §4 close/lseek/pread/pwrite(偏移/引用/资源)
先讲: 打开与读写 → 持久化 → 用户缓冲 → fd/偏移/关闭
后续依赖: 02-mmap-epoll-inotify(mmap/epoll/文件监控)
```

### 叙事顺序

1. 问题引入——`fwrite()` 返回成功，为什么机器断电后文件仍可能缺数据？（**Aha: I/O 成功、用户态 flush、内核回写和持久化是四个不同边界**）
2. open/read/write——文件描述符与 partial I/O
3. fsync/fdatasync/O_SYNC/O_DIRECT——同步语义
4. stdio——用户态缓冲叠加页缓存
5. close/lseek/pread/pwrite——偏移与资源生命周期
6. 收束——从用户 API 到持久化的分层账本

### 1. open、read、write — 返回值和 partial I/O 是第一道边界

场景提示: `read(fd, buf, 4096)` 返回 1000，应该当错误重试，还是这次读取已经合法完成？ [写作时展开]

关键设计: POSIX I/O 以文件描述符为统一入口，但每个调用都要严格解释返回值：

```[pseudocode]
fd = open(path, O_RDWR|O_CREAT|O_TRUNC, 0644)
  → 成功: 非负文件描述符
  → 失败: -1, 此时检查 errno

read(fd, buf, count)
  → n > 0: 本次读到 n 字节, n 可能小于 count
  → n == 0: EOF(普通文件)或对端关闭等语义
  → n == -1: 错误; 非阻塞场景可能 EAGAIN/EWOULDBLOCK

write(fd, buf, count)
  → n > 0: 本次写入 n 字节, 可能 partial write
  → n == -1: 错误
  → 非阻塞/管道/socket/信号/空间不足都可能影响结果
```

Why: 为什么 read/write 不承诺一次完成 `count` 字节？——**接口受文件类型、可用数据、信号、非阻塞状态、管道/socket 和资源压力影响**：普通文件常见完整读取，但代码不能把它当作所有 fd 的契约。可靠的“读满/写满”需要循环，并正确处理 EOF、EINTR 和 EAGAIN。 [man 2 read/write: 返回值、partial I/O、EINTR/EAGAIN 语义]

比喻锚点: `read`/`write` 像从仓库取货/交货，本次窗口只承诺处理实际能装下的数量；“请求 4096”不是“保证 4096”。 [写作时展开]

### 2. fsync、fdatasync、O_SYNC、O_DIRECT — 数据在哪一刻才更接近持久化

场景提示: 数据已经进入页缓存，怎样把“我希望现在持久化”的边界告诉内核？ [写作时展开]

关键设计: 同步 API 和 open 标志影响数据/元数据、每次写的等待以及是否使用页缓存：

```[pseudocode]
fflush(stream)
  → 只把 stdio 用户态缓冲交给 write

fsync(fd)
  → 请求相关文件数据/必要元数据写入稳定存储语义

fdatasync(fd)
  → 关注数据与影响后续读取的必要元数据
  → 不等同于“绝不更新任何时间戳”

O_SYNC/O_DSYNC
  → 影响 write 返回前的同步语义
  → 代价和具体文件系统/设备实现相关

O_DIRECT
  → 尝试绕过页缓存
  → buffer/offset/length 对齐、文件系统和设备约束必须满足
```

Why: 为什么不能把 `O_DIRECT` 简化成“完全不经过内核”？——**系统调用、文件系统、块层和驱动仍然存在，只是数据缓存路径不同**；它还把缓存管理责任更多交给应用，并可能与对齐、并发、元数据和混合 buffered I/O 产生复杂交互。`fsync()` 返回也依赖底层存储对稳定写入的实现，不能只看函数名猜磁盘物理状态。 [man 2 fsync/open: 同步、直接 I/O、错误和对齐语义必须按目标系统核对]

比喻锚点: stdio/页缓存像两层待发货仓库，`fflush` 只是把货从手推车交给仓库，`fsync` 才请求仓库把货送到稳定库位；O_DIRECT 是绕过中间货架，但仍要经过装卸系统。 [写作时展开]

### 3. stdio 缓冲 — `fwrite` 与 `write` 之间还有一层

场景提示: `printf("hello")` 不一定立即出现在终端或文件里，stdio 究竟缓存了什么？ [写作时展开]

关键设计: C stdio 在用户态维护 FILE 缓冲，刷新条件取决于全缓冲、行缓冲、无缓冲和显式 fflush：

```[pseudocode]
全缓冲:
  累积到缓冲区/显式 fflush/关闭时再调用 write

行缓冲:
  常在遇到换行或特定刷新条件时提交

无缓冲:
  库层不保留常规输出缓冲

fflush(stream)
  → 刷新 stdio 缓冲
  → 不自动等价于 fsync(fd)

stdio → write → Page Cache → writeback → device
```

Why: 为什么 `fflush` 后仍不能断言断电不丢？——**fflush 只解决用户态 FILE 缓冲，数据仍可能在内核页缓存、文件系统日志、块设备队列或设备缓存中**；要讨论持久化还要明确 fsync、文件系统、设备和硬件保证。缓冲模式默认值也受 stream、终端/文件和 libc 实现影响，不应把 BUFSIZ/行缓冲行为写成全平台固定常数。 [man 3 fflush/setvbuf: stdio 缓冲与刷新语义]

比喻锚点: `fflush` 是把桌面上的草稿放进公司收发柜，不是把文件送进防火保险柜；两者都叫“交出去”，安全等级不同。 [写作时展开]

### 4. close、lseek、pread/pwrite — fd、打开文件对象和偏移

场景提示: fork 后父子进程各自 close，为什么一个进程的读写偏移可能影响另一个？多线程怎样避免共享 offset 竞争？ [写作时展开]

关键设计: fd 是进程表中的引用，指向打开文件对象；打开文件对象拥有 file status flags 和当前 offset：

```[pseudocode]
open
  fd table entry → open file description → inode/file operations

fork
  父子进程复制 fd 引用
  → 可能共享同一个 open file description/offset

close(fd)
  → 释放当前 fd 引用
  → 最后引用释放后才完成更深层关闭

lseek(fd, offset, whence)
  → 修改共享打开文件偏移

pread/pwrite(fd, buf, n, offset)
  → 使用显式 offset
  → 不改变共享当前文件偏移
  → 适合并发随机读写(仍需考虑数据竞态)
```

Why: 为什么 `pread/pwrite` 比“lseek + read/write”更适合并发？——**它把位置作为一次调用参数，避免多个线程争用共享 file offset 的两步竞态**；但它不自动解决同一数据区域的覆盖、持久化、锁和应用协议问题。lseek 超过 EOF 后写入还会产生稀疏文件，逻辑大小与实际占用不同。 [man 2 close/lseek/pread: fd 引用、共享 offset、稀疏文件和错误语义]

比喻锚点: 共享 offset 像多人共用一本书签，谁移动书签都会影响别人；pread/pwrite 像每个人都直接报页码，不抢那张共享书签。 [写作时展开]

### 5. 收束

文件 I/O 的分层边界：

```[pseudocode]
stdio buffer
  → write/read 系统调用
  → fd/open file description/VFS
  → Page Cache 或 O_DIRECT 路径
  → filesystem/block layer/device
  → fsync/O_SYNC 等持久化边界
```

**Aha Moment**: "文件 I/O 的难点不在记住 `open/read/write` 名字，而在**区分 partial、offset、用户缓冲、页缓存、回写和稳定存储**这几层语义；每一层都可能成功，但成功含义不同。"
**回答读者三问**: ①read/write 为什么要循环=partial I/O 是合法返回；②fflush 是否等于落盘=只刷新 stdio，不等于 fsync；③多线程怎样避免 offset 竞争=pread/pwrite 使用显式偏移，但仍需处理数据竞态。

---

### 核心悬念

**"read/write 每次都在用户态和内核间复制；mmap 如何把文件映射到地址空间，epoll 又如何让线程同时等待文件/网络事件，inotify 怎样通知目录变化？"**

→ 引出 02-mmap-epoll-inotify — mmap、epoll 与文件监控。