# 第 6 篇：NIO 网络层与线程模型

> 第 5 篇讲完了 Connector 的两阶段启动，最终停在 `NioEndpoint.startInternal()`。
> 本篇回答核心问题：**Tomcat 的网络 IO 到底是怎么转起来的？三线程模型如何分工？
> 请求处理线程池和标准 ThreadPoolExecutor 有什么本质区别？连接数上限怎么实现的？**
>
> 这是全系列**性能核心**：Acceptor/Poller/Worker 三线程模型 + TaskQueue 反直觉设计 + LimitLatch 限流。
>
> **源码范围**：
> - `org.apache.tomcat.util.net.NioEndpoint`（1787 行，核心）
> - `org.apache.tomcat.util.net.Acceptor`（231 行）
> - `org.apache.tomcat.util.net.AbstractEndpoint`（1590 行）
> - `org.apache.tomcat.util.threads.TaskQueue`（119 行）/ `ThreadPoolExecutor` / `TaskThreadFactory`
> - `org.apache.tomcat.util.threads.LimitLatch`（174 行）
> - `org.apache.tomcat.util.collections.SynchronizedStack`（109 行）
> - `org.apache.coyote.AbstractProtocol$ConnectionHandler`（连接处理器）
> - `org.apache.coyote.http11.Http11Processor`（HTTP 解析与 adapter 调用）
>
> **本篇定位**：Tomcat 核心层（网络 IO 的心脏）。

---

## 目录

1. [NIO 模型概览：一个连接的一生](#1-nio-模型概览一个连接的一生)
2. [三线程模型：Acceptor → Poller → Worker](#2-三线程模型acceptor--poller--worker)
3. [请求处理线程池：TaskQueue 的反直觉设计](#3-请求处理线程池taskqueue-的反直觉设计)
4. [LimitLatch：连接数限流器](#4-limitlatch连接数限流器)
5. [SynchronizedStack：GC-free 对象池](#5-synchronizedstackgc-free-对象池)
6. [ConnectionHandler：连接与处理器的绑定](#6-connectionhandler连接与处理器的绑定)
7. [Http11Processor：HTTP 解析与 adapter 调用](#7-http11processorhttp-解析与-adapter-调用)
8. [本篇小结与面试要点](#8-本篇小结与面试要点)

---

## 1. NIO 模型概览：一个连接的一生

### 1.1 从 socket 到 Servlet 的完整旅程

```
客户端连接
  │
  ▼
★ Acceptor 线程（1 个）
  │  serverSocket.accept() 阻塞等待新连接
  │  每接到一个 → countUpOrAwaitConnection()（LimitLatch 限流）
  │            → setSocketOptions() → 注册到 Poller
  ▼
★ Poller 线程（1 个）
  │  selector.select() 阻塞等待 IO 事件（读/写就绪）
  │  processKey() → processSocket() → 提交到 Worker 线程池
  ▼
★ Worker 线程池（默认 maxThreads=200）
  │  SocketProcessor.doRun()
  │    → ConnectionHandler.process()
  │      → Http11Processor.service()
  │        → adapter.service()（第 2~5 篇的容器之旅）
  ▼
Servlet.service()
```

### 1.2 三线程分工（面试必背）

| 线程 | 数量 | 职责 | 阻塞点 |
|---|---|---|---|
| **Acceptor** | 1 | 接受新连接 | `serverSocket.accept()`（阻塞） |
| **Poller** | 1（默认） | 监听 IO 事件 | `selector.select()`（阻塞） |
| **Worker** | 线程池（默认 10~200） | 执行请求处理 | 无（业务代码自己阻塞） |

**核心思想**（与 Netty 的 Reactor 多线程模型对比）：

- Netty：**多个** EventLoop（每个绑定一组 Channel），`boss` 线程 accept + `worker` 线程 IO
- Tomcat NIO：**单** Acceptor + **单** Poller + **线程池** Worker

> **面试点**：Tomcat 的 NIO 与 Netty 的线程模型有什么异同？
> ——都基于 NIO Selector 事件驱动。Netty 是"**多 Reactor**"：boss 负责 accept，多个 worker EventLoop
> 各自持有一个 Selector 轮询自己的连接；Tomcat 是"**单 Reactor + 线程池**"：一个 Poller 持有一个
> Selector 处理所有连接的 IO 事件，就绪后把任务丢给 Worker 线程池（业务处理与 IO 事件分离）。
> Tomcat 的 Poller 不执行业务逻辑（只做事件分发），业务在 Worker 线程池；Netty 的 EventLoop
> 既做 IO 也执行 ChannelHandler（业务逻辑与 IO 同线程，除非显式切换线程）。

---

## 2. 三线程模型：Acceptor → Poller → Worker

### 2.1 Acceptor：接受连接的永动机

**源码**：`org/apache/tomcat/util/net/Acceptor.java`（231 行），`run()` 在 68-164

```java
@Override
public void run() {

    int errorDelay = 0;

    try {
        // 主循环：直到收到停止命令
        while (!stopCalled) {

            // 暂停状态：忙等 → 1ms sleep → 10ms sleep（避免空转烧 CPU）
            while (endpoint.isPaused() && !stopCalled) {
                if ((System.nanoTime() - pauseStart) > 1_000_000) {
                    Thread.sleep((System.nanoTime() - pauseStart) > 10_000_000 ? 10 : 1);
                }
            }

            if (stopCalled) break;
            state = AcceptorState.RUNNING;

            try {
                // ★ 连接数限流：超过 maxConnections 时阻塞在这里
                endpoint.countUpOrAwaitConnection();

                // Endpoint 可能在等待时被暂停
                if (endpoint.isPaused()) continue;

                U socket = null;
                try {
                    // ★ 阻塞接受新连接
                    socket = endpoint.serverSocketAccept();
                } catch (Exception ioe) {
                    endpoint.countDownConnection();
                    if (endpoint.isRunning()) {
                        // 失败退避：50ms → 100ms → ... → 1600ms（指数退避）
                        errorDelay = handleExceptionWithDelay(errorDelay);
                        throw ioe;
                    } else {
                        break;
                    }
                }
                errorDelay = 0;

                // ★ 配置 socket 并交给 Poller（注册到 Selector）
                if (!stopCalled && !endpoint.isPaused()) {
                    if (!endpoint.setSocketOptions(socket)) {
                        endpoint.closeSocket(socket);
                    }
                } else {
                    endpoint.destroySocket(socket);
                }
            } catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                log.error(sm.getString("endpoint.accept.fail"), t);
            }
        }
    } finally {
        stopLatch.countDown();
    }
    state = AcceptorState.ENDED;
}
```

**Acceptor 的三个关键细节**：

1. **限流在 accept 之前**：`countUpOrAwaitConnection()` 在 `serverSocketAccept()` **之前**——
   达到 `maxConnections` 时 Acceptor 阻塞，**不再 accept 新连接**（但已建立的连接继续处理）
2. **失败指数退避**：accept 异常（如 fd 耗尽）时 50ms→100ms→...→1600ms 递增等待，避免空转打日志
3. **暂停检测**：paused 时忙等→sleep 升级，兼顾"快速恢复"和"不烧 CPU"

### 2.2 Poller：Selector 事件循环

**源码**：`NioEndpoint.java:740-799`（`Poller.run()`）

```java
@Override
public void run() {
    // 事件循环：直到 destroy()
    while (true) {
        boolean hasEvents = false;

        try {
            if (!close) {
                hasEvents = events();                 // ★ 处理已注册的事件队列
                if (wakeupCounter.getAndSet(-1) > 0) {
                    // 有唤醒请求 → 非阻塞 select
                    keyCount = selector.selectNow();
                } else {
                    // ★ 阻塞等待 IO 事件（默认 1 秒超时）
                    keyCount = selector.select(selectorTimeout);
                }
                wakeupCounter.set(0);
            }
            if (close) {
                events();
                timeout(0, false);
                selector.close();
                break;
            }
            if (keyCount == 0) {
                hasEvents = (hasEvents | events());
            }
        } catch (Throwable x) {
            ExceptionUtils.handleThrowable(x);
            log.error(sm.getString("endpoint.nio.selectorLoopError"), x);
            continue;
        }

        // ★ 遍历就绪的 SelectionKey，分发到 Worker
        Iterator<SelectionKey> iterator = keyCount > 0 ? selector.selectedKeys().iterator() : null;
        while (iterator != null && iterator.hasNext()) {
            SelectionKey sk = iterator.next();
            iterator.remove();
            NioSocketWrapper socketWrapper = (NioSocketWrapper) sk.attachment();
            if (socketWrapper != null) {
                processKey(sk, socketWrapper);       // ★ 读/写事件 → processSocket
            }
        }

        timeout(keyCount, hasEvents);                 // 超时检测（keep-alive 超时等）
    }
    getStopLatch().countDown();
}
```

**processKey**（`NioEndpoint.java:801-856`）——IO 就绪分发：

```java
protected void processKey(SelectionKey sk, NioSocketWrapper socketWrapper) {
    try {
        if (close) {
            socketWrapper.close();
        } else if (sk.isValid()) {
            if (sk.isReadable() || sk.isWritable()) {
                unreg(sk, socketWrapper, sk.readyOps());   // 先取消注册（避免重复触发）
                boolean closeSocket = false;

                // ★ 可读 → 提交"读"任务到 Worker 线程池
                if (sk.isReadable()) {
                    if (socketWrapper.readOperation != null) {
                        // 非阻塞 IO（Servlet 3.1 ReadListener）
                        ...
                    } else if (!processSocket(socketWrapper, SocketEvent.OPEN_READ, true)) {
                        closeSocket = true;
                    }
                }
                // ★ 可写 → 提交"写"任务到 Worker 线程池
                if (!closeSocket && sk.isWritable()) {
                    ...
                    if (!processSocket(socketWrapper, SocketEvent.OPEN_WRITE, true)) {
                        closeSocket = true;
                    }
                }
                if (closeSocket) {
                    socketWrapper.close();
                }
            }
        } else {
            socketWrapper.close();    // 无效 key
        }
    } catch (CancelledKeyException ckx) {
        socketWrapper.close();
    } catch (Throwable t) { ... }
}
```

**processSocket**（`AbstractEndpoint.java:1247-1279`）——提交到 Worker 线程池：

```java
public boolean processSocket(SocketWrapperBase<S> socketWrapper, SocketEvent event, boolean dispatch) {
    try {
        SocketProcessorBase<S> sc = null;
        if (processorCache != null) {
            sc = processorCache.pop();            // ★ 从对象池取 SocketProcessor（复用）
        }
        if (sc == null) {
            sc = createSocketProcessor(socketWrapper, event);
        } else {
            sc.reset(socketWrapper, event);
        }
        Executor executor = getExecutor();
        if (dispatch && executor != null) {
            executor.execute(sc);                 // ★ 提交到 Worker 线程池！
        } else {
            sc.run();                             // 不调度（如同步/特殊场景）
        }
    } catch (RejectedExecutionException ree) {
        getLog().warn(sm.getString("endpoint.executor.fail", socketWrapper), ree);
        return false;                             // 线程池满 → 关闭连接
    } catch (Throwable t) {
        ...
    }
    return true;
}
```

**Poller 的要点**：
- **单线程**持有一个 Selector，处理**所有**连接的 IO 事件
- IO 事件（读就绪/写就绪）→ `processSocket` → 提交 `SocketProcessor` 到 Worker 线程池
- **Poller 本身不做业务**——它只做"事件 → 任务"的分发

### 2.3 Worker：SocketProcessor.doRun()

**源码**：`NioEndpoint.java:1681-1769`（`SocketProcessor.doRun()`）

```java
@Override
protected void doRun() {

    Poller poller = NioEndpoint.this.poller;
    if (poller == null) {
        socketWrapper.close();
        return;
    }

    try {
        int handshake = -1;
        try {
            if (socketWrapper.getSocket().isHandshakeComplete()) {
                handshake = 0;                          // 无 TLS 握手
            } else if (event == SocketEvent.STOP || event == SocketEvent.DISCONNECT ||
                    event == SocketEvent.ERROR) {
                handshake = -1;
            } else {
                handshake = socketWrapper.getSocket().handshake(  // ★ TLS 握手
                        event == SocketEvent.OPEN_READ, event == SocketEvent.OPEN_WRITE);
                event = SocketEvent.OPEN_READ;
            }
        } catch (IOException x) {
            handshake = -1;
        } catch (CancelledKeyException ckx) {
            handshake = -1;
        }

        if (handshake == 0) {
            SocketState state = SocketState.OPEN;
            // ★ 核心：交给 ConnectionHandler（→ Http11Processor → adapter.service）
            if (event == null) {
                state = getHandler().process(socketWrapper, SocketEvent.OPEN_READ);
            } else {
                state = getHandler().process(socketWrapper, event);
            }
            if (state == SocketState.CLOSED) {
                socketWrapper.close();
            }
        } else if (handshake == -1) {
            getHandler().process(socketWrapper, SocketEvent.CONNECT_FAIL);
            socketWrapper.close();
        } else if (handshake == SelectionKey.OP_READ) {
            socketWrapper.registerReadInterest();       // 握手未完，重新注册读兴趣
        } else if (handshake == SelectionKey.OP_WRITE) {
            socketWrapper.registerWriteInterest();
        }
    } catch (CancelledKeyException cx) {
        socketWrapper.close();
    } catch (Throwable t) { ... }
    finally {
        socketWrapper = null;
        event = null;
        // ★ 归还 SocketProcessor 到对象池（复用）
        if (running && processorCache != null) {
            processorCache.push(this);
        }
    }
}
```

**Worker 的完整链路**：

```
SocketProcessor.doRun()
  └─ ConnectionHandler.process(socketWrapper, OPEN_READ)   [AbstractProtocol.java:812]
       └─ 取/建 Http11Processor
            └─ processor.process(wrapper, status)          [Http11Processor]
                 └─ service() → adapter.service()          [第 2~5 篇]
```

---

## 3. 请求处理线程池：TaskQueue 的反直觉设计

### 3.1 标准 ThreadPoolExecutor 的"问题"

标准 `ThreadPoolExecutor` 的行为：

```
任务提交 → 线程数 < corePoolSize？→ 新建线程
        → 否则 → 放入队列
        → 队列满？→ 线程数 < maxPoolSize？→ 新建线程
        → 否则 → 拒绝
```

**问题**：如果队列是**无界**的（`LinkedBlockingQueue` 默认无界），
那么"线程数 < maxPoolSize"时**永远不会创建新线程**——因为队列永远不满！
结果：`corePoolSize=10, maxPoolSize=200` 的线程池，在**队列空**的情况下，
即使有 1000 个任务并发，也**只会有 10 个线程**在工作，其余 990 个在队列里排队。

对请求处理来说这是灾难——响应延迟会随队列深度线性增长。

### 3.2 Tomcat 的解法：TaskQueue 覆写 offer()

**源码**：`org/apache/tomcat/util/threads/TaskQueue.java`（119 行，核心 73-93）

```java
public class TaskQueue extends LinkedBlockingQueue<Runnable> {

    private transient volatile ThreadPoolExecutor parent = null;

    @Override
    public boolean offer(Runnable o) {
        // we can't do any checks
        if (parent == null) {
            return super.offer(o);
        }
        // ★ 1. 线程数已到上限：只能排队
        if (parent.getPoolSizeNoLock() == parent.getMaximumPoolSize()) {
            return super.offer(o);
        }
        // ★ 2. 有空闲线程：排队（不会创建新线程）
        if (parent.getSubmittedCount() <= parent.getPoolSizeNoLock()) {
            return super.offer(o);
        }
        // ★ 3. 提交数 > 线程数 且 线程数 < 上限：返回 false → 强制创建新线程！
        if (parent.getPoolSizeNoLock() < parent.getMaximumPoolSize()) {
            return false;
        }
        // ★ 4. 兜底：排队
        return super.offer(o);
    }
}
```

**关键机制**：`offer()` 返回 `false` 时，`ThreadPoolExecutor.executeInternal()` 会走
`else if (!addWorker(command, false)) { reject(command); }`——**创建新线程**（第 2 步失败才走到这）。

**与标准线程池的对比**：

| 场景 | 标准线程池 | Tomcat（TaskQueue） |
|---|---|---|
| 10 线程 + 1000 任务 | 10 线程跑，990 排队 | 逐步扩到 maxPoolSize=200 |
| 200 线程 + 更多任务 | 排队（200 线程跑） | 排队（200 线程跑） |
| 核心思想 | 先排队，队列满才扩线程 | **先扩线程，线程满才排队** |

**为什么 Tomcat 要这么做？**——HTTP 请求的响应延迟直接取决于"任务被处理的等待时间"。
标准线程池在队列空时不会扩线程，导致**高并发下延迟飙升**；
Tomcat 保证"任务多 → 线程多"，用线程数换取延迟。

**完整的 execute 链路**（`ThreadPoolExecutor.java:1374-1392`）：

```java
public void execute(Runnable command) {
    submittedCount.incrementAndGet();          // ★ 提交计数（TaskQueue.offer 判断用）
    try {
        executeInternal(command);              // 标准 3 步：addWorker → offer → addWorker
    } catch (RejectedExecutionException rx) {
        if (getQueue() instanceof TaskQueue) {
            // ★ 并发竞争下 offer 返回 false 但 addWorker 也失败 → force 入队兜底
            final TaskQueue queue = (TaskQueue) getQueue();
            if (!queue.force(command)) {
                submittedCount.decrementAndGet();
                throw new RejectedExecutionException(...);
            }
        }
        ...
    }
}
```

**TaskQueue.force()**（`TaskQueue.java:65-70`）——拒绝时的强制入队：

```java
public boolean force(Runnable o) {
    if (parent == null || parent.isShutdown()) {
        throw new RejectedExecutionException(...);
    }
    return super.offer(o);   // 无条件入队
}
```

> **面试点**：Tomcat 的线程池和标准 ThreadPoolExecutor 有什么区别？
> ——**Tomcat 不是继承 JDK 的 `java.util.concurrent.ThreadPoolExecutor`，而是继承
> `AbstractExecutorService` 自己实现了一套**（`ThreadPoolExecutor.java:330`）：
> `public class ThreadPoolExecutor extends AbstractExecutorService`。
> 它复刻了 JDK 的线程池三步执行逻辑（`executeInternal`：addWorker → workQueue.offer → addWorker），
> 但**必须自研**的原因：`TaskQueue.offer()` 需要 `getPoolSizeNoLock()`/`getSubmittedCount()` 这些
> **内部钩子**（JDK 未公开）。核心差异在 **TaskQueue 覆写 `offer()`**：当
> "提交的任务数 > 当前线程数 且 线程数 < maxPoolSize"时返回 false，**强制触发线程扩容**
> （标准线程池是"队列满才扩容"）。目的：**用线程数换请求延迟**——HTTP 请求等不得队列。
> 另外还有 `force()` 兜底（并发竞争下拒绝的任务强制入队）和
> `poll()`/`take()` 覆写（空闲线程回收时机优化）。

### 3.3 线程池的创建与默认值

**AbstractEndpoint.createExecutor()**（`AbstractEndpoint.java:1084-1095`）：

```java
public void createExecutor() {
    internalExecutor = true;
    if (getUseVirtualThreads()) {
        // ★ 虚拟线程（Spring Boot 3.2+，第 8 篇）
        executor = new VirtualThreadExecutor(getName() + "-virt-");
    } else {
        TaskQueue taskqueue = new TaskQueue(maxQueueSize);          // 无界队列（默认 MAX_VALUE）
        TaskThreadFactory tf = new TaskThreadFactory(getName() + "-exec-", daemon, getThreadPriority());
        executor = new ThreadPoolExecutor(getMinSpareThreads(), getMaxThreads(), getThreadsMaxIdleTime(),
                TimeUnit.MILLISECONDS, taskqueue, tf);
        taskqueue.setParent((ThreadPoolExecutor) executor);         // ★ 双向绑定（TaskQueue 需要 parent）
    }
}
```

**默认值**（`AbstractEndpoint.java`）：

| 参数 | 默认值 | 对应 Spring Boot 配置 |
|---|---|---|
| `minSpareThreads`（corePoolSize） | **10**（767 行） | `server.tomcat.threads.min-spare` |
| `maxThreads`（maximumPoolSize） | **200**（794 行） | `server.tomcat.threads.max` |
| `maxQueueSize` | **Integer.MAX_VALUE**（818 行，无界） | `server.tomcat.threads.max-queue-capacity` |
| `threadsMaxIdleTime` | **60000ms**（835 行） | 无直接配置项（需自定义 Customizer） |

**TaskThreadFactory**（71 行）：

```java
public Thread newThread(Runnable r) {
    Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
    t.setDaemon(daemon);          // 守护线程：主线程退出即结束
    t.setPriority(threadPriority);
    return t;
}
```

> **面试点**：Tomcat 的 Worker 线程是守护线程吗？
> ——是（`TaskThreadFactory` 默认 daemon=true）。所以嵌入式 Tomcat 需要
> `TomcatWebServer.startNonDaemonAwaitThread()` 创建**非守护**等待线程
> （`TomcatWebServer.java:214-226`），否则 main 线程退出后 JVM 直接结束（第 8 篇）。

---

## 4. LimitLatch：连接数限流器

### 4.1 为什么需要？

`maxConnections`（默认 8192）控制**同时打开的连接数上限**。实现不能简单用计数器——
需要**阻塞语义**：达到上限时，Acceptor 阻塞等待，而不是拒绝（拒绝会让客户端重连风暴）。

### 4.2 实现：基于 AQS 的共享锁

**源码**：`org/apache/tomcat/util/threads/LimitLatch.java`（174 行）

```java
public class LimitLatch {

    // ★ 内部 Sync 继承 AbstractQueuedSynchronizer（AQS）
    private class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        @Override
        protected int tryAcquireShared(int ignored) {
            long newCount = count.incrementAndGet();
            if (!released && newCount > limit) {
                // 超过上限：回退并返回 -1（进入等待队列）
                count.decrementAndGet();
                return -1;
            } else {
                return 1;      // 成功获取
            }
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            count.decrementAndGet();
            return true;
        }
    }

    private final Sync sync;
    private final AtomicLong count;
    private volatile long limit;
    private volatile boolean released = false;

    public LimitLatch(long limit) {
        this.limit = limit;
        this.count = new AtomicLong(0);
        this.sync = new Sync();
    }

    // ★ 获取：超限则阻塞（Acceptor 调用）
    public void countUpOrAwait() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    // ★ 释放（连接关闭时调用）
    public long countDown() {
        sync.releaseShared(0);
        return getCount();
    }

    // 优雅关闭：放行所有等待线程
    public boolean releaseAll() {
        released = true;
        return sync.releaseShared(0);
    }
}
```

### 4.3 与 Endpoint 的集成

**初始化**（`AbstractEndpoint.java:1459-1467`）：

```java
protected LimitLatch initializeConnectionLatch() {
    if (maxConnections == -1) {
        return null;                                  // -1 = 不限制
    }
    if (connectionLimitLatch == null) {
        connectionLimitLatch = new LimitLatch(getMaxConnections());
    }
    return connectionLimitLatch;
}
```

**Acceptor 中**（第 2.1 节已见）：

```java
endpoint.countUpOrAwaitConnection();   // 获取：超限阻塞
...
endpoint.countDownConnection();        // 释放（连接关闭时）
```

**优雅关闭**（`AbstractEndpoint.java:1469-1475`）：

```java
private void releaseConnectionLatch() {
    LimitLatch latch = connectionLimitLatch;
    if (latch != null) {
        latch.releaseAll();    // 放行所有等待线程（停止时）
    }
    connectionLimitLatch = null;
}
```

> **面试点**：`maxConnections` 和 `maxThreads` 是什么关系？
> ——完全不同的两个维度。`maxConnections`（默认 8192）= **连接的并发上限**
> （LimitLatch 计数，Acceptor 阻塞）；`maxThreads`（默认 200）= **请求处理线程上限**
> （线程池）。NIO 下一个连接不一定占用一个线程（keep-alive 空闲连接不占线程），
> 所以两者独立配置。达到 maxConnections 时新连接被阻塞；达到 maxThreads 时任务排队。

---

## 5. SynchronizedStack：GC-free 对象池

### 5.1 为什么需要？

请求处理中高频创建/销毁的对象（`SocketProcessor`、`PollerEvent`、`NioChannel`）会产生大量 GC 压力。
Tomcat 用**手写栈对象池**复用它们。

### 5.2 实现

**源码**：`org/apache/tomcat/util/collections/SynchronizedStack.java`（109 行）

```java
/**
 * This is intended as a (mostly) GC-free alternative to java.util.Stack
 * when the requirement is to create a pool of re-usable objects with no
 * requirement to shrink the pool.
 */
public class SynchronizedStack<T> {

    public static final int DEFAULT_SIZE = 128;

    private int size;
    private int limit;
    private int index = -1;        // 指向栈顶（下一个可用对象）
    private Object[] stack;

    public synchronized boolean push(T obj) {
        index++;
        if (index == size) {
            if (limit == -1 || size < limit) {
                expand();          // 扩容（唯一产生垃圾的地方）
            } else {
                index--;
                return false;      // 达上限：丢弃（不扩容）
            }
        }
        stack[index] = obj;
        return true;
    }

    @SuppressWarnings("unchecked")
    public synchronized T pop() {
        if (index == -1) {
            return null;           // 池空
        }
        T result = (T) stack[index];
        stack[index--] = null;     // ★ 置 null：防止对象滞留（内存泄漏防御）
        return result;
    }

    public synchronized void clear() { ... }
}
```

**设计要点**：
- **数组 + 索引**实现栈：比 `java.util.Stack`（Vector，有锁开销和扩容开销）更快
- **push 置引用、pop 置 null**：池中引用不滞留（防止"池化对象被 GC 根引用导致泄漏"）
- **上限控制**：`limit` 参数（如 `processorCache` 默认 500）——池满后不再缓存，直接丢弃（交给 GC）
- **"mostly GC-free"**：只有扩容时产生一次数组垃圾

**在 NioEndpoint 中的使用**（`NioEndpoint.java:255-268`）：

```java
// SocketProperties 默认 processorCache=0（禁用）、eventCache=0（禁用）、bufferPool=100
// 0 = 禁用缓存，-1 = 无上限，>0 = 上限
if (socketProperties.getProcessorCache() != 0) {
    processorCache = new SynchronizedStack<>(SynchronizedStack.DEFAULT_SIZE,
            socketProperties.getProcessorCache());      // SocketProcessor 池
}
if (socketProperties.getEventCache() != 0) {
    eventCache = new SynchronizedStack<>(SynchronizedStack.DEFAULT_SIZE,
            socketProperties.getEventCache());          // PollerEvent 池
}
int actualBufferPool = socketProperties.getActualBufferPool(...);
if (actualBufferPool != 0) {
    nioChannels = new SynchronizedStack<>(SynchronizedStack.DEFAULT_SIZE, actualBufferPool);  // NioChannel 池
}
```

> **注意**：Tomcat 裸机默认 `processorCache`/`eventCache` 为 **0（禁用）**（`SocketProperties.java:50/58`）——
> 但 **Spring Boot 默认配置了 200**（`ServerProperties.java:470`）：
> `TomcatWebServerFactoryCustomizer.customizeProcessorCache()` → `AbstractProtocol.setProcessorCache(200)`
> → `SocketProperties.setProcessorCache(200)`，所以 **Spring Boot 嵌入式下 SocketProcessor 缓存是启用的**。
> `bufferPool` 默认 **-2**（`SocketProperties.java:105`）= "用 `bufferPoolSize` 计算"：
> JVM 最大内存 < 1GB 时 0（禁用），否则按 `maxMemory/32` 折算可缓存通道数（`getActualBufferPool`）。

---

## 6. ConnectionHandler：连接与处理器的绑定

### 6.1 定位

**ConnectionHandler**（`AbstractProtocol` 的内部类，`AbstractProtocol.java:812` 起的 `process()`）
是**连接 ↔ 处理器（Processor）的映射表**：

- 每个连接对应一个 `SocketWrapper`（socket 包装）
- 每个连接需要一个 `Processor`（HTTP 解析器，如 `Http11Processor`）
- ConnectionHandler 维护 `Map<SocketWrapper, Processor>`，实现连接级处理器复用

### 6.2 process() 的核心流程

```java
public SocketState process(SocketWrapperBase<S> wrapper, SocketEvent status) {

    // 1. 从 wrapper 取当前绑定的 Processor（连接级复用）
    Processor processor = (Processor) wrapper.takeCurrentProcessor();

    // 2. 超时事件特殊处理（异步超时可能已过期）
    if (SocketEvent.TIMEOUT == status && ...) {
        return SocketState.OPEN;    // 无效超时，NO-OP
    }

    // 3. 无 Processor → 创建/复用
    if (processor == null) {
        // HTTP/2 升级协议协商（ALPN）
        String negotiatedProtocol = wrapper.getNegotiatedProtocol();
        if (negotiatedProtocol != null && negotiatedProtocol.length() > 0) {
            UpgradeProtocol upgradeProtocol = getProtocol().getNegotiatedProtocol(negotiatedProtocol);
            if (upgradeProtocol != null) {
                processor = upgradeProtocol.getProcessor(wrapper, getProtocol().getAdapter());
            }
            ...
        }
        if (processor == null) {
            processor = recycledProcessors.pop();       // ★ 从 Processor 池复用
        }
        if (processor == null) {
            processor = getProtocol().createProcessor();  // 新建（Http11Processor）
            register(processor);
        }
        processor.setSslSupport(wrapper.getSslSupport());
    }

    // 4. ★ 核心循环：处理直到需要等待 IO 或结束
    SocketState state = SocketState.CLOSED;
    do {
        state = processor.process(wrapper, status);     // Http11Processor.service()

        if (state == SocketState.UPGRADING) {
            // HTTP Upgrade（WebSocket/h2c）：切换处理器
            ...
        }
    } while (state == SocketState.OPEN && ...);

    // 5. 处理完释放
    ...
    return state;
}
```

**SocketState 状态机**（`AbstractEndpoint.java:83-90`，完整 9 态）：

```java
enum SocketState {
    OPEN, CLOSED, LONG, ASYNC_END, SENDFILE, UPGRADING, UPGRADED, ASYNC_IO, SUSPENDED
}
```

| 状态 | 含义 | 后续 |
|---|---|---|
| `OPEN` | 继续处理（keep-alive 下个请求） | 循环继续 |
| `CLOSED` | 连接关闭 | 清理 |
| `LONG` | 异步/非阻塞 IO 挂起 | 等 Poller 事件 |
| `UPGRADING` | HTTP 升级进行中（WebSocket/h2c） | 换处理器 |
| `UPGRADED` | 升级完成 | 新处理器接管 |
| `ASYNC_END` | 异步结束 | 收尾 |
| `SENDFILE` | 零拷贝发送文件 | 等写就绪 |
| `ASYNC_IO` | 升级处理器自己管理 IO | 不再经 Poller |
| `SUSPENDED` | 请求挂起 | 等恢复 |

---

## 7. Http11Processor：HTTP 解析与 adapter 调用

### 7.1 定位

**Http11Processor**（`org.apache.coyote.http11.Http11Processor`，继承 `AbstractProcessor`）
是 **HTTP/1.1 协议的解析器**：

- 解析请求行、请求头、请求体
- 处理 keep-alive、chunked 编码、Expect: 100-continue
- 解析完成后调用 `adapter.service()`（第 2 篇入口）

### 7.2 service() 与 adapter 的衔接

**源码**：`Http11Processor.java:251` 起（`service()` 方法），`adapter.service` 在 397 行

```java
public SocketState service(SocketWrapperBase<?> socketWrapper) throws IOException {
    ...
    // ★ 主循环：解析 HTTP 请求（状态机：请求行 → 头部 → 体）
    while (!getErrorState().isError() && keepAlive && !isAsync() && upgradeToken == null &&
            !endpoint.isPaused() && http11InputBuffer.parseRequestLine(keptAlive)) {

        if (!disableKeepAlive()) {
            ...
        }
        // 解析请求头
        if (!http09 && !inputBuffer.parseHeaders()) {
            // 不完整 → 等更多数据
            break;
        }
        ...
        // ★ 核心：调用适配器进入容器
        getAdapter().service(request, response);    // ← CoyoteAdapter.service()（第 2 篇）
        ...
    }
    ...
}
```

**一次请求的 Http11Processor 视角**：

```
Http11Processor.service()
  ├─ parseRequestLine()    ← 解析 "GET /path HTTP/1.1"
  ├─ parseHeaders()        ← 解析请求头（Host/Cookie/...）
  ├─ getAdapter().service() ← ★ 进入容器（第 2~5 篇）
  └─ 循环：keep-alive 继续下一个请求
```

---

## 8. 本篇小结与面试要点

### 8.1 本篇地图

```
第 5 篇：Connector 两阶段启动（NioEndpoint.startInternal 创建线程）
第 6 篇（本篇）：NIO 三线程模型 + 线程池 + 限流
第 2 篇：adapter.service()（Worker 线程中执行的容器之旅）
第 8 篇：虚拟线程（VirtualThreadExecutor 替代 Worker 线程池）
```

### 8.2 面试要点速查

1. **三线程模型**：Acceptor（accept）→ Poller（select）→ Worker（业务），各司其职
2. **Tomcat vs Netty 线程模型**：单 Reactor + 线程池 vs 多 Reactor（EventLoop）
3. **TaskQueue 反直觉**：`offer()` 在"提交数 > 线程数且未达上限"时返回 false → 强制扩线程
4. **扩线程的目的**：用线程数换请求延迟（HTTP 请求等不得队列）
5. **force() 兜底**：并发竞争下拒绝的任务强制入队
6. **线程池默认值**：minSpare=10、maxThreads=200、maxQueueSize=MAX（无界）
7. **Worker 是守护线程**：需要非守护 await 线程保 JVM（嵌入式）
8. **LimitLatch**：AQS 共享锁实现连接数限流，Acceptor 超限阻塞
9. **maxConnections vs maxThreads**：连接数（LimitLatch）vs 处理线程数（线程池），独立配置
10. **SynchronizedStack**：数组栈对象池，push/pop 置 null 防滞留，limit 控容量
11. **ConnectionHandler**：连接↔Processor 映射，Processor 池复用，UPGRADING 支持 WebSocket/h2c
12. **Http11Processor**：HTTP 解析状态机，解析完调 `adapter.service()` 进入容器
