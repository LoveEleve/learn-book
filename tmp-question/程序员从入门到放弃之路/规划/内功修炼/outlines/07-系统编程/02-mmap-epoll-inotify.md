# mmap、epoll 与 inotify — 映射、事件循环和文件变化如何接到一起

> Cluster A: 6 KPs | 依赖: 01-file-io-basics | 读者基线: open/read/write、fd、页缓存
> 读者处境: 01 篇讲了文件 I/O 的缓存与 fd 语义；本篇回答三个系统编程常见问题：如何映射大文件、如何等待大量 fd、如何在文件变化时收到通知
> 打开新视角: `mmap` 改变数据访问方式，`epoll` 改变等待方式，`inotify` 改变变更发现方式；三者都减少轮询/复制，但都有明确边界

---

### 概念依赖链

```
01 file I/O(fd/页缓存/同步) → 本篇: mmap/epoll/inotify + 链接/元数据
  ├─ §1 mmap(VMA/缺页/Page Cache/共享与私有)
  ├─ §2 epoll(ready event 与非阻塞 I/O)
  ├─ §3 stat/lstat/fstat(元数据与 TOCTOU)
  ├─ §4 link/unlink/symlink(名字与 inode)
  └─ §5 inotify(文件事件与队列)
先讲: 映射 → 事件等待 → 元数据 → 名字关系 → 文件通知
后续依赖: 03-fork-exec-daemon(进程、fd 表、地址空间和信号)
```

### 叙事顺序

1. 问题引入——大文件频繁读写、同时监听网络和文件变化，怎样避免反复复制和轮询？
2. mmap——文件页映射到地址空间
3. epoll——统一等待网络/管道/inotify fd
4. stat/link/unlink——名字与元数据的竞态
5. inotify——文件变化事件
6. 收束——映射、等待、通知三条线

### 1. mmap — 把文件映射成进程地址空间中的页

场景提示: `mmap` 返回指针后，第一次访问为什么可能触发缺页，而不是在 `mmap()` 调用时把整个文件读进内存？ [写作时展开]

关键设计: mmap 建立 VMA 和文件偏移的映射，实际页通常在首次访问时按需进入 Page Cache：

```[pseudocode]
p = mmap(NULL, len, PROT_READ|PROT_WRITE, MAP_SHARED, fd, offset)
  → 建立虚拟地址区间/VMA
  → 通常不立即读取整个文件

首次访问 p[i]
  → page fault
  → 文件页/页缓存查找或读盘
  → 建立页表映射

MAP_SHARED 写入:
  → 修改映射页
  → dirty/writeback
  → 需要持久化边界时配合 msync/fsync 等

MAP_PRIVATE 写入:
  → COW 私有页
  → 不把修改写回原文件
```

Why: 为什么 mmap 不是“零成本零拷贝”？——**它减少了显式 read→用户 buffer 的复制路径，但引入缺页、页表/TLB、缓存淘汰、脏页回写和地址空间管理成本**；顺序大读、随机访问、共享修改、文件大小变化等 workload 的最佳选择不同。`MAP_ANONYMOUS` 是匿名映射，不等同于“完全等价 malloc”。 [man 2 mmap/msync: 映射权限、共享/私有、同步和错误语义]

比喻锚点: mmap 像给仓库目录画一张虚拟地图，第一次走到某个货架时才把货搬到工作区；地图建立很快，但取货成本推迟到了访问时。 [写作时展开]

### 2. epoll — 把网络、管道和通知 fd 放进同一事件循环

场景提示: 一个进程既要等 socket，又要等 inotify 文件变化，能否统一用一个等待机制？ [写作时展开]

关键设计: epoll 等待 fd readiness，LT/ET 语义决定应用如何消费：

```[pseudocode]
epfd = epoll_create1(EPOLL_CLOEXEC)
epoll_ctl(epfd, EPOLL_CTL_ADD, fd, &event)

while (running):
  n = epoll_wait(epfd, events, maxevents, timeout)
  for event in events:
    if EPOLLIN:
      nonblocking read/recv until EAGAIN(ET)
    if EPOLLOUT:
      flush pending output
    if EPOLLERR/HUP:
      inspect error/close
```

Why: 为什么 ET 必须配非阻塞循环，而 LT 更容易正确？——**ET 只提醒状态变化，剩余数据未被读完时不保证再次产生边沿**；LT 会在条件仍满足时重复报告。epoll 也只管理 readiness，不保证一次事件对应完整应用消息。 [man 7 epoll: LT/ET、EPOLLONESHOT、EPOLLRDHUP 和线程唤醒语义]

比喻锚点: epoll 是总值班台，socket 和 inotify 都是不同报警器；值班台只说“有事”，处理人员还要把具体事件读干净。 [写作时展开]

### 3. stat、lstat、fstat — 元数据查询与 TOCTOU

场景提示: 程序先 `stat()` 判断文件权限，再 `open()` 使用它，攻击者能否在两步之间替换路径？ [写作时展开]

关键设计: 三个接口的对象和符号链接语义不同：

```[pseudocode]
stat(path)
  → 跟随符号链接, 查询目标 inode

lstat(path)
  → 查询符号链接自身 inode

fstat(fd)
  → 查询已经打开的文件对象

危险模式:
  stat(path) → check
  open(path) → use
  中间 path 可能被替换(TOCTOU)

更稳妥:
  openat/flags/权限约束
  → fstat(fd) 对已打开对象检查
```

Why: 为什么 fstat 能减少路径竞态？——**它检查的是已经获得的 fd 对象，不再重新解析一遍可能变化的路径**；但权限、目录替换、符号链接、跨目录边界仍需配合 `openat2`/安全 flags 和正确的权限模型。 [man 2 stat/lstat/fstat/openat2: 路径解析与符号链接边界]

比喻锚点: stat(path) 像先看门牌再进屋，fstat(fd) 像进屋后检查手里拿到的房间钥匙；前者到后者之间门牌可能被换。 [写作时展开]

### 4. link、unlink、symlink — 文件名与 inode 的不同关系

场景提示: 两个文件名为什么能指向同一份数据，符号链接又为什么可以悬空？ [写作时展开]

关键设计: 硬链接和符号链接分别复用 inode 与保存路径文本：

```[pseudocode]
link(target, newname)
  → 新目录项指向同一 inode
  → i_nlink 增加

symlink(target_text, linkpath)
  → 创建独立符号链接 inode
  → 保存目标路径字符串
  → 目标可以暂时不存在

unlink(path)
  → 删除一个目录项/链接
  → i_nlink 归零且没有打开引用时才释放 inode/数据
```

Why: 为什么 unlink 一个打开文件后，进程仍能读写它？——**目录名引用和打开文件引用是两种不同生命周期**：unlink 只删除名字，内核打开文件对象仍持有 inode；最后一个引用关闭后，空间才可回收。硬链接不能跨文件系统，符号链接则可能因目标移动成为 dangling link。 [man 2 link/unlink/symlink: 链接计数、跨文件系统和符号链接语义]

比喻锚点: 硬链接像两块门牌共用一套房产证，symlink 像一张写着“请去某地址”的指路纸条；房产证没了门牌才会失效，纸条则可能指向不存在的地址。 [写作时展开]

### 5. inotify — 文件变化事件与队列边界

场景提示: 配置文件热加载程序怎样知道文件变了，而不是每秒 stat 一遍？ [写作时展开]

关键设计: inotify fd 把文件系统事件排成用户态可读事件，天然可以加入 epoll：

```[pseudocode]
fd = inotify_init1(IN_NONBLOCK|IN_CLOEXEC)
wd = inotify_add_watch(path, IN_CREATE|IN_DELETE|IN_MODIFY|IN_CLOSE_WRITE|IN_MOVED_FROM|IN_MOVED_TO)

epoll_ctl(epfd, ADD, fd, EPOLLIN)

事件循环触发:
  read(inotify_fd, buffer)
  → struct inotify_event{name, wd, mask, cookie}
  → 处理/重新扫描真实状态

边界:
  watch 通常针对目录/对象, 递归目录需自行管理子 watch
  IN_Q_OVERFLOW → 事件可能丢失
  事件不是完整事务日志, 需要以当前文件状态校正
```

Why: 为什么收到 IN_MODIFY 就不能直接假设“文件已经完整写完”？——**写入可能分多次完成，事件队列可能合并/溢出，rename 还有成对 cookie；inotify 是变化提示，不是可靠的内容同步协议**。生产 watcher 应在事件后重新 stat/read/校验，必要时处理 overflow 全量 rescan。 [man 7 inotify: watch 范围、事件队列、overflow 与 rename cookie]

比喻锚点: inotify 像仓库变更提醒单，告诉你哪排货架动过；提醒单丢了或多张合并时，仍要回仓库盘点真实库存。 [写作时展开]

### 6. 收束

三条系统编程线：

```[pseudocode]
mmap:
  文件 → VMA → 缺页/Page Cache → 应用内存访问

epoll:
  多个 fd → readiness → 非阻塞消费

inotify:
  文件变化 → event fd → epoll 事件循环 → 重新确认状态
```

**Aha Moment**: "mmap 减少显式复制，epoll 减少无效等待，inotify 减少轮询；但三者都不是魔法：**缺页、ready 语义、事件丢失和路径竞态仍需由程序正确处理**。"
**回答读者三问**: ①mmap 是否零开销=不是，缺页/TLB/回写仍存在；②epoll 是否返回完整消息=不是，只返回 fd readiness；③inotify 是否可靠日志=不是，溢出/合并后需重新扫描。

---

### 核心悬念

**"fd 能打开文件、epoll 能监控事件、mmap 能映射地址；进程通过 fork/exec 创建后，fd 表、地址空间和信号处理到底如何继承与重置？"**

→ 引出 03-fork-exec-daemon — 进程创建、exec、daemon 与生命周期。