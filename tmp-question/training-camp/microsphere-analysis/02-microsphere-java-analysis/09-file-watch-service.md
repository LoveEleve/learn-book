# 文件监听 -- 一个被 OS 异构性拖累的跨平台地狱

> 主题：为什么「监听文件变化」这个看似简单的需求在 Java 里这么难，OS 原生通知机制有哪些根本约束（目录粒度、不递归、队列上限），以及「事件循环/处理分离」「文件原子性」「轮询 vs 事件驱动」三个永恒原理。microsphere 的 `StandardFileWatchService` 只是这条脉络上一个轻量实例。
> 关联：复用 [§6 事件系统](./06-event-system.md) 的 `EventDispatcher.parallel`，复用 [§4 Prioritized](./04-spi-prioritized.md) 的监听器排序。

---

## 一、文件监听为什么是个跨平台地狱

「监听某配置文件，变化时热刷新」--这需求听起来 trivial，实际写出来处处是坑。根源不在 Java，在 OS--**主流 OS 的文件通知机制四种各不相同，行为差异极大**。

### 1.1 四种 OS 原生通知机制

| OS | 机制 | 监听粒度 | 是否递归 | 队列上限 | 已知问题 |
|---|---|---|---|---|---|
| Linux | **inotify**（2005，kernel 2.6.13） | 路径（目录或文件） | ❌ 不递归 | 16384 events / 实例 | 队列满后 `IN_Q_OVERFLOW` 丢事件 |
| macOS | **FSEvents**（2007，10.5） | 目录（自动递归） | ✅ 递归 | 无硬上限 | 有 lag（事件合并延迟）、rename 后丢事件 |
| Windows | **ReadDirectoryChangesW** | 目录 | ✅ 可选递归 | 缓冲区 | 重叠 IO 复杂但行为最可靠 |
| BSD | **kqueue**（`EVFILT_VNODE`） | 文件描述符（每文件一个 fd） | ❌ | fd 上限 | 大量文件时 fd 耗尽 |

**四种机制的行为差异**：

- **递归监听**：FSEvents/ReadDirectoryChangesW 支持递归（注册一个目录自动监听所有子目录），inotify/kqueue 不支持（每个子目录要单独注册）。这意味着「监听整个目录树」在 Linux/BSD 上要自己遍历注册所有子目录，且新增子目录时要补注册。
- **事件粒度**：inotify 是「路径粒度」（注册哪个路径收哪个路径的事件），FSEvents 是「目录粒度」（注册目录收目录及子孙的事件），kqueue 是「fd 粒度」（每个 fd 一个 watch）。
- **队列上限**：inotify 有硬上限（默认 16384 events，可调 `/proc/sys/fs/inotify/max_queued_events`），溢出后 `IN_Q_OVERFLOW` 通知但丢具体事件。其他机制无硬上限或用缓冲区。
- **延迟**：FSEvents 有意做「事件合并 + 延迟」（省电考虑），可能几百毫秒后才通知。inotify 是实时。

#### 为什么每种机制这么设计

这四种机制的差异不是随意的，各自反映了 OS 的资源模型和设计目标：

**inotify 为什么不递归**：Linux inotify 的设计是「每个被监听路径占一个 watch descriptor（wd）」，wd 是 kernel 资源，有全系统上限（默认 `fs.inotify.max_user_watches`，常见值 8192-65536）。如果支持递归，一个百万文件目录树要占百万个 wd--内核承受不住。所以 inotify 选择「显式注册每个目录」，让用户自己控制 wd 消耗。**这是「kernel 资源有限」倒逼的设计**。

**FSEvents 为什么有 lag**：macOS FSEvents 是为笔记本省电设计的--它把事件批量合并、延迟几百毫秒再通知，减少唤醒 CPU 的次数（每次唤醒都耗电）。FSEvents 在内核里维护一个「事件流」日志文件，监听者从日志读事件。**这是「移动设备省电优先」倒逼的设计**，代价是牺牲实时性。

**kqueue 为什么是 fd 粒度**：BSD 的 kqueue 是通用事件机制（不止文件，还有 socket、signal、timer），用 `EVFILT_VNODE` filter 监听文件。它要求你 `open()` 每个要监听的文件拿到 fd，再注册 fd 到 kqueue。每个 fd 占一个进程文件描述符（进程 fd 上限通常 1024-4096）。**这是「复用 fd 抽象」的设计**，代价是大量文件时 fd 耗尽。

**ReadDirectoryChangesW 为什么最可靠**：Windows 用重叠 IO（overlapped I/O）+ 缓冲区，注册一个目录后内核异步把变化写进缓冲区，应用读取。它支持递归（一个标志位控制），缓冲区可配置大小，无明显硬上限。**这是「后发优势」**--Windows 在 2000 年代设计时吸取了前人的坑，做得最完善。

#### 跨平台地狱的根源

四种机制的设计目标不同（kernel 资源 / 省电 / fd 复用 / 后发优势），导致**它们的语义不在同一个抽象层**：

- inotify 的「wd」和 FSEvents 的「事件流日志」是两种完全不同的数据结构。
- inotify 的「不递归」和 FSEvents 的「递归」不是「一个特性的两个取值」，而是「两种监听模型」。
- 队列上限：inotify 是硬上限（丢事件），FSEvents 是日志文件（不丢但延迟），kqueue 是 fd 上限（注册失败）。

**JDK `WatchService` 试图用一套 API 统一这四种**，但「统一只在 API 层，行为差异保留」--这就是下一节要讲的跨平台不一致的根源。这不是 JDK 偷懒，而是四种机制的语义差异太大，无法在不损失信息的前提下统一。

### 1.2 JDK `WatchService` 的跨平台不一致

JDK 7 的 `WatchService` 试图统一这四种机制，但**统一只在 API 层，行为差异保留**：

- 在 Linux 上 `WatchService` 后端是 inotify，**继承不递归 + 16384 队列上限**。
- 在 macOS 上后端是 FSEvents，**支持递归但有 lag**。
- 在 Windows 上后端是 ReadDirectoryChangesW，**支持递归且可靠**。

**结果**：同一个 Java 程序在三个平台上行为不一致。你在 macOS 开发测试正常（FSEvents 递归），部署到 Linux 生产环境发现子目录监听失效（inotify 不递归）--这种「开发/生产行为不一致」的坑极难排查。

**`WatchService` 还继承了 OS 机制的「目录粒度」约束**：`Path.register` 实际是注册路径的父目录，监听目录内条目变化。你给一个文件路径，底层要么报错要么静默不工作。监听 `/etc/app/config.yml` 得注册 `/etc/app` 目录，然后在事件回调里判断 `event.context()` 是不是 `config.yml`。这个「目录 + 上下文过滤」的样板每次都要重写。

### 1.3 microsphere `StandardFileWatchService` 的定位

JDK `WatchService` 给了原生能力但不解决上述任何一个坑：跨平台不一致、目录粒度、队列溢出、样板代码、事件分发、生命周期管理全都要自己写。microsphere 的 `StandardFileWatchService` 把它包装成一个**带事件总线、线程分离、生命周期管理**的可用组件。下面三个小节讲透背后的永恒原理。


## 二、永恒原理一:OS 通知的「目录粒度」约束

文件监听的第一个根本约束来自 OS--**所有原生通知机制都是「目录粒度」或更粗**，没有「文件粒度」的递归监听。

### 2.1 为什么是目录粒度

OS 内核维护「被监听的路径」列表，每个条目消耗一个 kernel 资源（inotify watch descriptor、kqueue fd）。如果允许「文件粒度 + 递归」，一个百万文件的目录树要消耗百万个 kernel 资源--内核承受不住。所以 OS 选择「目录粒度」--注册一个目录，目录内任何条目变化都通知，由用户态过滤。

**这导致所有「文件粒度监听」都得做同一套样板**：

```
1. 注册文件所在目录(不是文件本身)
2. 收到事件时,从 event.context() 拿到变化文件的相对路径
3. 判断这个路径是不是我要监听的文件
4. 是 -> 派发;不是 -> 忽略
```

microsphere 的 `FileChangedMetadata` 就是这个样板的封装：按目录聚合监听器，每个目录维护一个 `filePaths` 集合，事件来时 `metadata.filePaths.contains(filePath)` 过滤。

### 2.2 inotify 不递归:Linux 上的根本约束

更严重的约束是 **inotify 不支持递归监听子目录**。注册 `/etc/app` 目录，**只收 `/etc/app` 直接子条目的事件，不收 `/etc/app/subdir/` 内的事件**。要监听整个目录树，必须自己遍历注册所有子目录，且**新增子目录时要补注册**（否则新增子目录内的变化收不到）。

**microsphere 没解决递归监听**：`StandardFileWatchService` 注册的是 `watch(file, listener, kinds)` 里的 `file`（或它的父目录），不遍历子目录。监听 `/etc/app` 时，`/etc/app/subdir/foo.yml` 变化收不到。这是 microsphere 的一个能力缺失，根源是 inotify 约束。

**业界解法**：

- **Apache Commons IO** 用轮询（`listFiles` 递归 diff），绕开 inotify 不递归。
- **Node.js `chokidar`** 在 inotify 后端上自己维护「子目录递归注册」--新增子目录时自动补注册，删除时清理。
- **macOS FSEvents** 天然递归，所以 chokidar 在 macOS 上不需要这层逻辑。

**这是「OS 约束倒逼上层复杂度」的典型**：OS 不提供递归，上层框架要么自己补（chokidar），要么用轮询绕开（Commons IO），要么不支持（microsphere）。没有银弹。

### 2.3 队列上限与 OVERFLOW

inotify 有 16384 events 的硬上限。当文件变化极频繁（如日志文件被高频写入、批量构建生成大量文件），队列满后内核丢弃事件并发出 `IN_Q_OVERFLOW`（对应 JDK 的 `StandardWatchEventKinds.OVERFLOW`）。

**OVERFLOW 是个「信息丢失」事件**--它告诉你「丢了些事件」，但不告诉你丢了什么。监听器收到 OVERFLOW 后无法知道哪些文件变了，唯一安全的做法是「全量重新扫描」（用 Scanner，§6.2）。这是事件驱动方案相对轮询方案的固有弱点--轮询每次都全量，不会丢；事件驱动快但可能丢。

**microsphere 对 OVERFLOW 的处理有缺陷**（§7.1）：它的 `toKind` 对 OVERFLOW 抛 `IllegalArgumentException`，会导致事件循环线程异常退出。正确做法是识别 OVERFLOW 后跳过（或触发全量重扫）。


## 三、永恒原理二:事件循环 / 处理分离的并发模式

文件监听的第二个永恒原理是**「事件循环线程」与「处理线程」必须分离**。这不是 microsphere 的发明，是个经典并发设计模式。

### 3.1 Reactor 模式

```
[OS 事件源] -> [事件循环线程](单线程,只取事件+派发) -> [处理线程池](并发执行业务逻辑)
```

事件循环线程职责单一：从事件源取事件（`ws.take()`）、读事件（`pollEvents()`）、重置（`reset()`）、派发到处理线程池。**不执行任何业务逻辑**。业务逻辑（如重新加载配置）在处理线程池并发执行。

**为什么必须分离？** 业务逻辑可能耗时长（重新加载配置涉及 IO）。如果业务逻辑和事件循环同线程，慢处理会阻塞 `take()`，期间发生的文件变化事件堆积在 OS 内核队列--队列满后 `OVERFLOW` 丢事件。分离后，事件循环线程只负责「取事件 + 派发」，监听器在另一线程池跑，互不阻塞。

### 3.2 业界的同构实现

这个模式在并发框架里反复出现：

| 框架 | 事件循环 | 处理线程池 |
|---|---|---|
| microsphere FileWatchService | `eventLoopExecutor`（单线程 `ws.take()`） | `eventHandlerExecutor`（用户传入） |
| Netty | boss group（accept 事件） | worker group（IO + 业务） |
| Disruptor | `BatchEventHandler` 消费 ring buffer | 业务逻辑在 handler 内或转发 |
| Nginx | master 进程（accept） | worker 进程（请求处理） |
| Reactor 模式（POSA2） | single-threaded event loop | thread pool for handlers |

**核心都是「单线程取事件 + 多线程处理」**。microsphere 是这个模式的极简版--单线程事件循环 + 用户传入的处理 Executor。

#### 同构点:这些框架到底「同」在哪里

光看表格只知道「都分了两个角色」，但同构的精髓在于**两个角色各自不变的职责边界**：

**事件循环角色的不变职责**：

1. **独占一个线程**（或极少线程）：事件循环是单线程的，或像 Netty boss group 那样只有少数线程。**绝不并发取事件**--并发会破坏事件顺序、引入锁竞争。
2. **只做「取 + 派发」，不做业务**：事件循环线程绝不执行耗时操作。它的生命周期里只有 `取事件 -> 派发到处理线程池 -> 回去取下一个`。一旦它执行了业务逻辑（如重新加载配置），就破坏了这个模式。
3. **阻塞在「取事件」上**：事件循环线程大部分时间阻塞在 `take()`/`epoll_wait`/`kevent` 上，等事件来。事件来时立刻唤醒派发。

**处理线程池角色的不变职责**：

1. **可并发**：处理线程池有多个线程，业务逻辑在这里并发执行。
2. **可阻塞**：业务逻辑可以慢、可以阻塞（IO、锁），不影响事件循环--因为它们在不同线程。
3. **可丢弃/可限流**：处理线程池满时，新事件可排队、可丢弃、可反压--策略由处理池决定，不影响事件循环的「取」。

**microsphere 的对应**：`eventLoopExecutor`（单线程，阻塞在 `ws.take()`，只派发）= 事件循环角色；`eventHandlerExecutor`（用户传入的线程池，跑监听器）= 处理线程池角色。职责边界和 Netty/Nginx 完全一致，只是规模小得多。

#### 为什么大家都收敛到这个模式

这不是巧合，而是**「阻塞操作」这个根本约束倒逼的收敛**。

**问题**：事件源（OS 文件通知、网络 socket、ring buffer）需要持续监听，监听者必须阻塞等待。同时，处理事件的业务逻辑可能阻塞（IO、锁、计算）。**如果监听和处理在同一线程，处理阻塞会让监听停滞，错过后续事件**。

**收敛逻辑**：

1. 监听必须持续 -> 需要一个 dedicated 线程阻塞监听。
2. 处理可能阻塞 -> 不能在监听线程做。
3. => 监听和处理必须分线程。
4. 监听是单源的（一个事件源），不需要并发 -> 监听线程单线程就够。
5. 处理是并发的（多个事件可同时处理）-> 处理用线程池。

**任何「持续监听事件源 + 处理可能阻塞」的系统都会被这五步推到同一个结构**。这就是为什么 Netty、Nginx、Disruptor、microsphere、Redis（6.0 前）、Node.js（libuv）都长这样--它们面对的是同一个根本约束。

**反例：为什么有些系统不用这个模式**。Redis 6.0 之前是「单线程事件循环 + 单线程处理」（不分线程处理），因为 Redis 的操作都是内存操作，不阻塞，没必要分线程。6.0 引入多线程 IO 是因为网络 IO 成了瓶颈。这说明这个模式的适用条件是「处理可能阻塞」--处理不阻塞时可以不分。

#### microsphere 的极简实现与边界

microsphere 的实现是这个模式的极简版，但有几个边界要注意：

**边界一：默认 `DIRECT_EXECUTOR` 破坏了分离**。无参构造用 `DIRECT_EXECUTOR`（`Runnable::run`，同步），处理在事件循环线程跑--**这等于退回了「不分线程」**。慢监听器会阻塞 `take()`，撑爆 OS 队列。生产环境必须传独立线程池。这是「默认值危险」的典型--模式本身是对的，默认配置破坏了模式。

**边界二：事件循环单线程的停止难题**。`ws.take()` 阻塞中如何停止？microsphere 的 `stop()` 用 `eventLoopFuture.cancel(true)` 中断线程，`take()` 抛 `InterruptedException` 跳出循环。但中断时机不确定--如果中断时正在 `pollEvents()` 派发，可能派发到一半。microsphere 用 `finally { watchKey.reset(); }` 保证 key 重置，但不保证派发的原子性。

**边界三：处理线程池的背压缺失**。如果监听器处理速度跟不上事件产生速度，事件堆积在 OS 队列（inotify 16384 上限）。microsphere 没有背压机制（不让事件源慢点产生）--因为 OS 文件通知不支持背压。只能靠「处理线程池够大」或「监听器够快」。这是事件驱动模式相对轮询模式的弱点--轮询可以主动控制节奏，事件驱动被动接收。

### 3.3 JDK `WatchService` 的三段式样板

事件循环线程内部是个固定三段式：

```java
while (running) {
    WatchKey key = ws.take();              // 1. 阻塞取事件
    for (WatchEvent event : key.pollEvents()) {  // 2. 读事件
        // 派发
    }
    key.reset();                            // 3. 重置(漏了就不再收事件)
}
```

**三段式每个环节都有坑**：

- `take()` 阻塞，停止时要中断线程（`cancel(true)`）。
- `pollEvents()` 返回的事件 `context()` 只含文件名不含路径，要自己 `dir.resolve(filename)` 拼绝对路径。
- `reset()` 必须在 `finally` 里调，否则该 key 不再收后续事件。新手最常漏。

microsphere 把这三段式封装进 `dispatchFileChangedEvents`，用 `finally { watchKey.reset(); }` 保证不漏。这是「样板下沉到框架」的典型价值。

### 3.4 双 Executor 的默认陷阱

microsphere 的无参构造用 `DIRECT_EXECUTOR`（`Runnable::run`，同步执行）作为处理 Executor--**意味着监听器默认在事件循环线程同步执行**。慢监听器会阻塞事件循环，撑爆 OS 队列。

**这是「给你最大灵活性的同时把责任丢给你」的设计哲学**：microsphere 不替你决定处理线程池大小（场景不同需求不同），但默认值是「危险但简单」的同步执行。生产环境必须用带参构造传独立线程池：

```java
Executor handlerPool = Executors.newFixedThreadPool(4);
FileWatchService ws = new StandardFileWatchService(handlerPool);
```

**对比 Netty 的默认**：Netty 默认 boss/worker 都是 `NioEventLoopGroup`，安全但稍重。microsphere 选了「轻默认 + 用户负责」，要求用户懂这个权衡。


## 四、永恒原理三:文件原子性与「伪 MODIFY」问题

文件监听的第三个永恒原理来自文件系统本身--**文件写入不是原子的，但很多工具用「原子替换」保存文件，导致监听器看到的事件语义错乱**。

### 4.1 write / rename / fsync 的语义

- **`write`**：往文件描述符写数据。不保证原子性--其他进程可能读到「写了一半」的内容（读撕裂）。`O_APPEND` 模式下 write 是原子的，普通 write 不是。
- **`rename`**：原子地把一个路径重命名为另一个。在同一文件系统内 rename 是原子的（要么完成要么不完成，中间状态不可见）。
- **`fsync`**：把文件描述符的脏页刷到磁盘。保证持久性，不影响可见性（可见性由 OS page cache 保证）。

**「原子替换」保存模式**：很多编辑器保存文件时不是直接 `write` 原文件，而是：

```
1. 写临时文件 config.yml.bak
2. fsync 临时文件
3. rename config.yml.bak -> config.yml   ← 原子替换
```

这样即使保存过程中崩溃，原文件不受影响（要么旧版本要么新版本，不会出现半个版本）。

### 4.2 原子替换导致 DELETE + CREATE 而非 MODIFY

**`rename` 在 OS 通知机制里的语义是「原路径删除 + 新路径创建」**。所以 vim/IntelliJ/`sed -i` 等用原子替换保存的编辑器，监听器看到的是：

```
config.yml  ENTRY_DELETE   ← 原文件被删(rename 走了)
config.yml  ENTRY_CREATE   ← 新文件被创建(rename 完成)
```

而非 `ENTRY_MODIFY`。**如果你的监听器只注册了 `MODIFIED`，会收不到事件**--配置热刷新失效。

这是所有文件监听框架的共同坑，不是 microsphere 独有。**应对**：

- 注册 `CREATED` + `MODIFIED` + `DELETED` 全部，在 `onFileCreated`/`onFileDeleted` 里也触发刷新逻辑。
- 或检测「删后立即创建」合并成 MODIFY（但「立即」的阈值难定）。
- microsphere 不做这层合并，业务自己处理。

### 4.3 ENTRY_MODIFY 的重复触发

Linux inotify 对每次 `write()` 调用都发事件。一次文件保存可能触发 2-5 次 `ENTRY_MODIFY`（写内容 + 写元数据 + 关闭文件描述符）。microsphere 直接每次都派发 `FileChangedEvent`--监听器会被调用多次，重新加载配置多次。

**应对：防抖（debounce）**。监听器内部收到事件后等 200ms，期间再收到则忽略，200ms 无新事件再真正处理。这是处理「事件重复触发」的通用手法，跨领域适用（UI 事件、网络重连、搜索建议都用防抖）。

**microsphere 没内置防抖**--每个业务自己写一遍。这是「机制 vs 策略」的取舍：防抖阈值（200ms? 500ms?）因场景而异，框架不替你定。但代价是重复劳动。

### 4.4 读撕裂问题

监听器收到 `ENTRY_MODIFY` 后读文件，可能读到「写了一半」的内容（write 不原子）。处理半个配置文件会解析失败甚至数据错乱。

**应对**：

- 读时先 `FileChannel.lock()` 加文件锁（但写方也要配合加锁，跨进程协调难）。
- 或读到不完整内容时重试（等下次 `ENTRY_MODIFY` 再读）。
- 或要求写方用「原子替换」（§4.2）--读到的永远是完整版本（旧或新）。这是最干净的解法，但要求所有写方都遵守。

microsphere 不解决读撕裂--它在事件层通知，不管读写协调。这是「文件监听框架」的能力边界。


## 五、永恒原理四:轮询 vs 事件驱动的架构权衡

文件监听有两种根本不同的架构--轮询和事件驱动。它们的取舍是永恒的架构命题。

### 5.1 两种架构

**轮询**：

```
每 N 秒:
  当前快照 = listFiles(目录)
  diff(上次快照, 当前快照) -> 变化事件
  上次快照 = 当前快照
```

定时全量扫描目录，对比前后快照找出变化。

**事件驱动**：

```
注册 OS 通知(inotify/FSEvents/...)
事件来时:
  派发事件
```

OS 内核在文件变化时主动通知，应用被动接收。

### 5.2 取舍

| 维度 | 轮询 | 事件驱动 |
|---|---|---|
| 延迟 | 秒级（取决于轮询间隔） | 毫秒级（OS 实时通知） |
| CPU 占用 | 较高（每次 listFiles + diff） | 极低（无事件时阻塞） |
| 跨平台一致性 | ✅ 完全一致（都是 listFiles） | ❌ 受 OS 机制影响（§1.2） |
| 递归监听 | ✅ 天然支持（递归 listFiles） | ❌ inotify 不递归（§2.2） |
| 事件丢失 | ❌ 不丢（每次全量） | ✅ 可能丢（OVERFLOW） |
| 实现复杂度 | 低 | 高（要处理 OS 差异、队列溢出、样板） |

**没有银弹**：

- **轮询赢在跨平台一致性和简单**，输在延迟和 CPU。
- **事件驱动赢在延迟和 CPU**，输在跨平台一致性和复杂度。

### 5.3 业界选择

- **Apache Commons IO `FileAlterationMonitor`**：轮询。跨平台一致、简单，但秒级延迟、CPU 占用高。适合文件量小、对延迟不敏感的场景。
- **JDK `WatchService` / microsphere**：事件驱动。毫秒延迟、低 CPU，但跨平台不一致、不递归、可能丢事件。适合文件量大、对延迟敏感、能接受平台差异的场景。
- **Node.js `chokidar`**：混合--默认事件驱动，inotify 不递归时自己补递归注册，FSEvents 不可靠时退回轮询。这是「两全」的工程努力，但代码复杂度极高。
- **Spring Cloud Config**：既不轮询也不事件驱动，而是「远程拉取」--配置中心主动推或客户端定时拉。本质是轮询，但拉的是远程配置而非本地文件。适合分布式配置同步。

**microsphere 选事件驱动**，定位是「单机本地文件低延迟监听」。分布式配置同步不在它的范畴。


## 六、microsphere 作为「文件监听框架」的一个实例

讲完四个原理，microsphere 的 `StandardFileWatchService` 就是这些原理的一次落地。

**实例一:目录粒度聚合（§2 原理的落地）**

`FileChangedMetadata` 按目录聚合--每个被监听目录一个 metadata，内含 `filePaths` 集合（该目录下被监听的具体文件）和 `EventDispatcher`。事件来时 `metadata.filePaths.contains(filePath)` 过滤，解决「目录粒度监听 + 文件粒度过滤」的样板（§2.1）。

**实例二:双 Executor 分离（§3 原理的落地）**

`eventLoopExecutor`（内部单线程）跑事件循环 `ws.take()` + `pollEvents()` + `reset()`，`eventHandlerExecutor`（用户传入）执行监听器。通过 `EventDispatcher.parallel(handlerExecutor)` 桥接--每个目录一个并行 EventDispatcher，文件事件异步分发到处理线程池（[§6 事件系统](./06-event-system.md) 的并行模式）。

**实例三:事件桥接（[§6](./06-event-system.md) 原理的落地）**

`FileChangedEvent` extends `Event`，`FileChangedListener extends EventListener<FileChangedEvent>`。`Kind` 枚举（CREATED/MODIFIED/DELETED）屏蔽 JDK 的 `ENTRY_CREATE` 等--翻译层让监听器只依赖 microsphere 自己的抽象，未来换底层（如换 Commons IO 轮询）上层不用改。`FileChangedListener` 的默认方法 `onEvent` 按 `Kind` 路由到 `onFileCreated`/`onFileModified`/`onFileDeleted`，子类只 override 关心的那个。

**Scanner 作为补充**：文件监听是「动态」（变化时通知），`Scanner` 是「静态」（主动扫描一次性返回）。两者互补--监听器用于热更新，扫描器用于初始化全量发现。`SimpleFileScanner` 递归扫目录，`SimpleJarEntryScanner` 扫 jar 内条目，`SimpleClassScanner` 扫包下类（退化到 confucius-commons 的 `ClassDataRepository` 三重索引）。Scanner 是另一套独立抽象，本文不展开。


## 七、实例批判:这个实现的缺陷

作为原理的一个落地实例，microsphere 的实现也有瑕疵。

1. **OVERFLOW 静默杀事件循环**（§2.3）：`toKind` 对 OVERFLOW 抛 `IllegalArgumentException`，异常冒泡到 `eventLoopExecutor.submit(...)` 的 Future 被吞，事件循环线程静默退出。表面 WatchService 还在工作，但后续不再有事件分发。应识别 OVERFLOW 跳过或触发全量重扫。
2. **不支持递归监听子目录**（§2.2）：inotify 约束未解，监听 `/etc/app` 时 `/etc/app/subdir/` 内变化收不到。应像 chokidar 那样遍历注册子目录 + 新增子目录补注册。
3. **`watch()` 在 `start()` 之后静默失效**：`registerDirectoriesToWatchService` 在 `start()` 时遍历缓存注册目录，之后调 `watch()` 加的新文件，metadata 进了缓存但目录没 register--静默不工作。应检查 `started` 状态抛异常。
4. **没有内置防抖**（§4.3）：`ENTRY_MODIFY` 重复触发让监听器被多次调用。每个业务自己写防抖是重复劳动。
5. **`fileChangedMetadataCache` 非线程安全**：`TreeMap` + `watch()`/`getMetadata()` 无锁。多线程并发注册同一目录可能丢数据。应换 `ConcurrentHashMap` 或加锁。

这些不是原理错误，而是原理在具体代码里的实现瑕疵。


## 八、与其他方案的原理对比

| 方案 | 架构 | 延迟 | CPU | 跨平台一致 | 递归监听 | 多监听器分发 | 依赖 |
|---|---|---|---|---|---|---|---|
| microsphere FileWatchService | 事件驱动 | 毫秒 | 极低 | ❌（受 OS 影响） | ❌ | EventDispatcher 并行 | microsphere-java-core |
| Apache Commons IO | 轮询 | 秒级 | 较高 | ✅ | ✅ | 自己实现 | commons-io |
| JDK WatchService 原生 | 事件驱动 | 毫秒 | 极低 | ❌ | ❌ | 自己实现 | JDK |
| Node.js chokidar | 混合 | 毫秒 | 低 | ✅（混合兜底） | ✅ | 自己实现 | chokidar |
| Spring Cloud Config | 远程拉取 | 取决于触发 | 低 | ✅ | N/A | RefreshScope | spring-cloud |

**原理层面的取舍**：

- **Commons IO 轮询**赢在跨平台一致和简单，输在延迟和 CPU。
- **microsphere/JDK 原生**赢在低延迟低 CPU，输在跨平台不一致和不递归。
- **chokidar** 用混合架构两全，但代码复杂度极高（专门解决跨平台坑的库）。
- **Spring Cloud Config** 走另一条路（远程拉取），解决的是分布式配置同步而非本地文件监听。

microsphere 的定位：**单机本地文件低延迟监听**。它不解决跨平台不一致（继承 WatchService）、不解决递归监听（继承 inotify 约束）、不解决分布式同步。它是「JDK WatchService 的可用包装」，不是「终极文件监听方案」。


## 九、面试要点

**Q1：「监听文件变化用 JDK 的 WatchService 就行了，为什么还要封装？原生 API 有什么坑？」**

答案：WatchService 是裸 API，五个坑。① 跨平台不一致--Linux inotify 不递归 + 16384 队列上限，macOS FSEvents 递归但有 lag，Windows ReadDirectoryChangesW 递归可靠。同一程序三平台行为不同。② 目录粒度约束--`Path.register` 实际注册父目录，监听单文件要自己过滤 `event.context()`。③ 事件信息少--`context()` 只含文件名不含路径，要自己拼。④ 没分发机制--要自己开线程 `take()` 死循环，监听器并发通知、异常处理、优雅停止全要自己写。⑤ OVERFLOW 丢事件但不说丢了什么。microsphere 把这些封装成带事件总线、双 Executor 分离、生命周期管理的组件。

**Q2：「为什么文件监听在 Linux 上不递归？怎么解决？」**

答案：根源是 inotify 设计--每个被监听路径消耗一个 kernel watch descriptor，递归监听百万文件目录树会耗尽 kernel 资源。所以 inotify 只监听注册的目录的直接子条目，不递归子目录。WatchService 在 Linux 后端是 inotify，继承这个约束。解法三种：① 自己遍历注册所有子目录 + 新增子目录补注册（Node.js chokidar 的做法）。② 改用轮询（Commons IO 递归 listFiles + diff）。③ 不支持递归（microsphere，限制使用场景）。macOS FSEvents 和 Windows ReadDirectoryChangesW 天然支持递归，所以这个坑是 Linux/BSD 独有。

**Q3：「文件监听为什么要把事件循环和处理分开线程？不分行不行？」**

答案：是 Reactor 模式--事件循环线程只取事件+派发，处理线程池执行业务逻辑。不分行的风险：业务逻辑（如重新加载配置）可能耗时，阻塞事件循环线程的 `ws.take()`，期间文件变化事件堆积在 OS 内核队列（inotify 16384 上限），队列满后 OVERFLOW 丢事件。分离后事件循环只取事件，监听器在另一线程池跑，互不阻塞。这是经典并发模式，Netty boss/worker、Disruptor EventHandler、Nginx master/worker 都同构。microsphere 用 `eventLoopExecutor`（单线程）+ `eventHandlerExecutor`（用户传入）实现，默认 `DIRECT_EXECUTOR` 是陷阱--生产环境必须传独立线程池。

**Q4：「vim 保存文件为什么监听器收到的是 DELETE+CREATE 而非 MODIFY？怎么应对？」**

答案：vim/IntelliJ/sed -i 等编辑器用「原子替换」保存--写临时文件 + rename 替换原文件。rename 在 OS 通知里语义是「原路径删除 + 新路径创建」，所以监听器看到 DELETE+CREATE 而非 MODIFY。这是文件原子性的副作用--原子替换保证「要么旧版本要么新版本，不会半个版本」，但牺牲了 MODIFY 语义。应对：注册 CREATED+MODIFIED+DELETED 全部，在 onFileCreated/onFileDeleted 里也触发刷新；或检测「删后立即创建」合并成 MODIFY。microsphere 不做合并，业务自己处理。所有文件监听框架都有这个坑，不是 microsphere 独有。

**Q5：「轮询和事件驱动两种文件监听架构，怎么选？为什么没有银弹？」**

答案：轮询赢在跨平台一致（都是 listFiles）和简单、递归天然支持、不丢事件；输在秒级延迟和 CPU 占用高。事件驱动赢在毫秒延迟和极低 CPU；输在跨平台不一致（OS 机制差异）、inotify 不递归、OVERFLOW 丢事件。没有银弹因为四个维度（延迟/CPU/一致性/递归）互相冲突。Commons IO 选轮询（跨平台一致优先），JDK/microsphere 选事件驱动（低延迟优先），chokidar 用混合两全但代码复杂度极高。选型看场景：文件量大对延迟敏感选事件驱动，跨平台一致优先选轮询。

**Q6：「如果让你改进 microsphere 的 FileWatchService，你会怎么改？」**

答案：原理层照搬事件循环/处理分离 + 目录聚合。改进点：① 支持递归监听--遍历注册子目录 + 新增子目录补注册（chokidar 做法），解 inotify 不递归约束。② OVERFLOW 不应抛异常杀死事件循环，应识别后跳过或触发全量重扫。③ `watch()` 在 `start()` 之后应抛异常而非静默失效。④ 内置防抖机制应对 ENTRY_MODIFY 重复触发（阈值可配）。⑤ `fileChangedMetadataCache` 换 `ConcurrentHashMap` 或加锁，应对并发注册。核心思路：双 Executor 分离和事件桥接是正确原理，递归监听和防抖是缺失能力，OVERFLOW 处理是实现 bug。

---

> **与事件系统的关联**：`StandardFileWatchService` 是 `EventDispatcher`（[§6](./06-event-system.md)）并行模式的典型消费者--每个监听目录一个 `EventDispatcher.parallel(handlerExecutor)`，文件事件通过事件总线异步分发。
> **与 confucius-commons 的关联**：`SimpleClassScanner` 退化路径用 `ClassDataRepository`（confucius-commons 三重索引）--两个框架在类扫描上形成「运行时索引 + 全量扫描」互补，参见 [confucius-commons §1](../01-confucius-commons-analysis/01-classloader-introspection.md)。
