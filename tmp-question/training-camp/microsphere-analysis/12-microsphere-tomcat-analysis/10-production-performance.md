# 第 10 篇：生产性能调优实战（场景驱动）

> 前 9 篇建立了完整的源码认知。本篇回答生产中最实际的问题：
> **我的服务该怎么配？为什么这么配？出问题时怎么定位？**
>
> 与第 8 篇的区别：第 8 篇是"配置项 → 源码"的**静态映射**；
> 本篇是"场景 → 决策 → 源码依据"的**动态分析**——先讲场景，再给结论，最后回扣机制。
>
> **阅读前提**：第 6 篇（NIO 线程模型）、第 8 篇（配置映射表）。

---

## 目录

1. [调优决策框架：先理解三个数字的关系](#1-调优决策框架先理解三个数字的关系)
2. [场景一：高并发 Web（IO 密集）](#2-场景一高并发-webio-密集)
3. [场景二：长连接（SSE / 轮询 / WebSocket）](#3-场景二长连接sse--轮询--websocket)
4. [场景三：压测与瓶颈定位](#4-场景三压测与瓶颈定位)
5. [建议配置模板](#5-建议配置模板)
6. [本篇小结与面试要点](#6-本篇小结与面试要点)

---

## 1. 调优决策框架：先理解三个数字的关系

### 1.1 三个核心数字

| 配置 | 默认值 | 控制什么 | 底层机制（第 6 篇） |
|---|---|---|---|
| `server.tomcat.threads.max` | 200 | **并发处理**的请求数上限 | `ThreadPoolExecutor.maximumPoolSize` |
| `server.tomcat.max-connections` | 8192 | **同时打开**的连接数上限 | `LimitLatch`（Acceptor 阻塞） |
| `server.tomcat.accept-count` | 100 | 等待队列长度 | `ServerSocket.bind(backlog)` |

### 1.2 关键认知：三者是独立的

**一个 keep-alive 空闲连接不占 Worker 线程**（第 6 篇）——这是理解一切的起点：

```
连接建立（Acceptor）→ 注册到 Poller（Selector 监听）
  → 请求到达（Poller 检测到读就绪）
    → 提交 Worker 线程池（此时才占用线程）
      → 响应完成 → 归还线程
        → 连接继续挂 Poller（keep-alive 空闲）
```

**推论**：
- `maxConnections=8192` 但 `maxThreads=200` 完全合理——**8192 个连接里绝大多数是空闲 keep-alive**
- 高并发下真正同时"在跑"的请求数 ≈ 活跃连接数（远小于总连接数）
- **瓶颈判断**：连接数打满 ≠ 线程数打满，症状完全不同（第 4 节）

### 1.3 决策树

```
服务类型？
├─ 短请求、高 QPS（REST API）→ 线程数够用就行，关注连接数
├─ 长请求、阻塞 IO（调用下游慢）→ 线程数要够大（IO 密集）
├─ CPU 密集（计算/加密）→ 线程数 ≈ 核数，超配无益
└─ 长连接（SSE/轮询/WebSocket）→ 连接数要够大，线程数看并发请求
```

---

## 2. 场景一：高并发 Web（IO 密集）

### 2.1 特征

- 典型 REST API：请求快（毫秒级），但内部要**调下游**（DB/Redis/其他服务）
- QPS 高，连接数大（大量 keep-alive 客户端）
- **线程大部分时间在等待**（IO 阻塞），而不是计算

### 2.2 为什么 IO 密集线程数可以大于核数？

**源码依据**：Worker 线程在 `Http11Processor.service()` → `Servlet.service()` 期间
**大部分时间阻塞在 IO**（数据库连接等待、下游 HTTP 调用）——阻塞期间线程不占 CPU。

```
线程 1: [解析请求][----等待 DB----][组装响应]
线程 2: [解析请求][--等待 Redis--][组装响应]
线程 3: [解析请求][----等待下游----][组装响应]
        ↑ 三个线程同时"等待"，但 CPU 利用率并不高
```

所以：**IO 密集场景，maxThreads 可以远超核数**（如 8 核机器配 200~400）。

**对比**：CPU 密集场景（纯计算、加密、压缩），线程数超过核数后
**上下文切换开销 > 并行收益**——`maxThreads ≈ 核数 × (1 + 等待/计算比)`。

### 2.3 实操建议

| 配置 | 建议 | 理由 |
|---|---|---|
| `server.tomcat.threads.max` | 200~400（IO 密集） | 等待不占 CPU，线程数换吞吐 |
| `server.tomcat.threads.min-spare` | 20~50 | 预热：避免突发流量时频繁创建线程 |
| `server.tomcat.threads.max-queue-capacity` | **保持默认（无界）** | 第 6 篇 TaskQueue 已保证"先扩线程"，队列只是最后兜底 |
| `server.tomcat.max-connections` | 保持 8192 或按需 | 连接数是上限，不是常态 |
| `server.tomcat.accept-count` | 保持 100 | 超过 maxConnections 后的排队，调大只是延缓拒绝 |

**为什么 `max-queue-capacity` 保持无界反而好**？——回扣第 6 篇 TaskQueue：

```
TaskQueue.offer() 的决策（第 6 篇 3.2）：
  ├─ 线程数 == maxPoolSize → 入队（无界队列永远能入）
  ├─ 提交数 <= 线程数 → 入队
  ├─ 提交数 > 线程数 且 线程数 < maxPoolSize → return false → ★ 扩线程
  └─ 兜底 → 入队
```

队列只是"线程已经全忙"时的**临时缓冲**——此时请求已经在排队了，调大 queue-capacity
只是把"拒绝"变成"更久地排队"。真正该做的是**调大 maxThreads 或优化业务耗时**。

> **面试点**：`max-queue-capacity` 调大有用吗？
> ——意义有限。TaskQueue 的设计是"**优先扩线程，队列只是兜底**"（第 6 篇）。
> 队列无界时，线程打满后请求无限排队——表现为延迟线性增长而不是拒绝。
> **注意**：`max-queue-capacity=0` 在 Spring Boot 中**不生效**——`TomcatWebServerFactoryCustomizer`
> 用 `.when(this::isPositive)` 过滤（`TomcatWebServerFactoryCustomizer.java:104-106`），
> 0 被跳过，实际仍是默认无界。要快速失败需自定义 `TomcatConnectorCustomizer` 直接
> `AbstractProtocol.setMaxQueueSize(0)`（届时线程池满 → `RejectedExecutionException` →
> `processSocket` 返回 false → **连接被关闭**，客户端表现为 connection reset）。

### 2.4 虚拟线程的影响（2026 生产现状）

`spring.threads.virtual.enabled=true` 后（第 8 篇 9.2）：

| | 平台线程 | 虚拟线程 |
|---|---|---|
| `maxThreads` | 生效（上限） | **失效**（每请求一个虚拟线程） |
| `min-spare` | 生效（预热） | 失效 |
| 适用 | 传统场景 | **IO 密集高并发**（阻塞调用不再占线程） |

**决策**：如果服务是典型的 IO 密集（大量下游调用），且 JDK 21+，**虚拟线程是首选**——
不需要再纠结 maxThreads 调多少。

---

## 3. 场景二：长连接（SSE / 轮询 / WebSocket）

### 3.1 特征

- 客户端**长时间保持连接**（SSE 推送、长轮询、WebSocket）
- 连接数可能巨大（万级），但每个连接的请求频率低
- 连接空闲时**不占线程**（挂 Poller 上），但**占连接数**（LimitLatch 计数）

### 3.2 maxConnections vs maxThreads 的博弈

**源码依据**（第 6 篇 4.3）：

```
Acceptor 每接到一个连接：
  countUpOrAwaitConnection()   ← LimitLatch 计数（maxConnections）
  → 超过 8192 → Acceptor 阻塞（新连接进不来）
```

**长连接场景的核心矛盾**：连接数被 maxConnections 卡住，但线程利用率很低。

**例**：10000 个 SSE 客户端（每个 1 条连接），如果 `maxConnections=8192`：
- 前 8192 个连接正常
- 第 8193 个连接**阻塞等待**（Acceptor 停在 `countUpOrAwaitConnection`）
- 客户端表现为"连接超时"

**调大 maxConnections 的代价**（源码依据，`AbstractEndpoint`）：

```java
// 每个连接在 Endpoint 层持有对象：
//   NioSocketWrapper（socket 包装）
//   Poller 的 SelectionKey
//   SocketBufferHandler（读写缓冲，appReadBufSize + appWriteBufSize = 16KB 默认）
```

万级连接 ≈ 每连接 **100~500KB** 内存开销（JVM 侧 16KB 缓冲 + 对象，加内核 socket 收发缓冲），
`maxConnections` 调大 = 内存换连接数。
`maxConnections` 调大 = 内存换连接数。

### 3.3 实操建议

| 配置 | 建议 | 理由 |
|---|---|---|
| `server.tomcat.max-connections` | 按内存估算（每连接约 100~500KB） | 万级连接 ≈ 8~16GB 内存预算 |
| `server.tomcat.threads.max` | 按**并发活跃请求**算，不按连接数 | 空闲连接不占线程（第 6 篇） |
| `server.tomcat.keep-alive-timeout` | 长连接场景调大（如 60s+） | 控制空闲连接何时释放 |
| `server.tomcat.max-keep-alive-requests` | 调大（如 1000）或 -1 | 长连接复用次数 |

**注意**：SSE/WebSocket 这类场景，**虚拟线程也是福音**——即使请求频繁，
虚拟线程不占用平台线程资源（第 8 篇 9.2）。

> **面试点**：1 万个长连接需要多少个线程？
> ——取决于**同时活跃的请求数**，不是连接数。10000 个 SSE 连接，若每秒只有 100 个
> 事件推送，那么活跃请求 ≈ 100，**线程池 100~200 就够**。空闲连接挂 Poller 上
> （Selector 事件驱动，第 6 篇），不消耗 Worker 线程。

---

## 4. 场景三：压测与瓶颈定位

### 4.1 四种典型瓶颈的症状差异（面试高频）

| 瓶颈 | 症状 | 判断方法 | 源码依据 |
|---|---|---|---|
| **线程池耗尽** | 延迟**线性增长**、CPU 不高、请求堆积 | 监控线程池活跃数 = maxThreads | TaskQueue 无界队列（第 6 篇） |
| **连接数打满** | 新连接**超时/被拒**、已有连接正常 | 监控连接数 = maxConnections | LimitLatch 阻塞 Acceptor（第 6 篇） |
| **CPU 打满** | 线程全忙、CPU 100% | top + jstack 看线程栈 | 业务计算瓶颈 |
| **下游慢** | 线程大量 WAITING（socketRead） | jstack 看线程状态 | IO 阻塞（第 2 篇） |

### 4.2 用 jstack 快速定位

```
# 线程池耗尽：大量 RUNNABLE 且执行时间长
jstack <pid> | grep -c "RUNNABLE"

# 下游慢：大量线程 WAITING 在 socketRead
jstack <pid> | grep -A 3 "java.net.SocketInputStream.socketRead0"

# 线程池打满确认：线程名 "http-nio-8080-exec-N" 的线程数
jstack <pid> | grep -c "http-nio-8080-exec"
```

**线程名规范**（第 6 篇 TaskThreadFactory）：`http-nio-8080-exec-1`——
`http-nio`（协议-IO 模型）+ `8080`（端口）+ `exec-N`（线程号）。

### 4.3 监控手段

| 手段 | 看什么 | 对应源码 |
|---|---|---|
| Actuator 线程 dump | `/actuator/threaddump` | 线程状态快照 |
| JMX（mbeanregistry.enabled=true） | `ThreadPool` 的 `currentThreadsBusy` | 第 1 篇 MBean 注册 |
| 访问日志（第 8 篇） | 响应时间分布 | AccessLogValve |
| 日志 | `"Tomcat started on port"` / 异常 | - |

**JMX 监控的关键属性**（开启 `server.tomcat.mbeanregistry.enabled=true` 后）：

```
Catalina:type=ThreadPool,name="http-nio-8080"
  ├─ currentThreadsBusy    ← 忙线程数（接近 maxThreads = 线程池耗尽）
  ├─ currentThreadCount    ← 当前线程数
  ├─ maxThreads
  └─ connectionCount       ← 连接数（接近 maxConnections = 连接打满）
```

> **面试点**：如何判断是"线程池耗尽"而不是"连接数打满"？
> ——看症状：线程池耗尽 = **已有请求变慢**（排队），连接数打满 = **新连接进不来**
> （老请求正常）。对应源码：前者是 `TaskQueue` 排队（延迟增长），
> 后者是 `LimitLatch.countUpOrAwait()` 阻塞 Acceptor（连接超时）。

---

## 5. 建议配置模板

### 5.1 常规高并发 Web（IO 密集，JDK 21 平台线程）

```yaml
server:
  port: 8080
  tomcat:
    threads:
      max: 300          # IO 密集：等待不占 CPU，线程数换吞吐
      min-spare: 30     # 预热，避免突发建线程
    max-connections: 10000   # 按内存估算：每连接约 100~500KB    accept-count: 200        # 排队缓冲（不是无脑调大）
    connection-timeout: 10s  # 等待请求数据的超时（SO_TIMEOUT，默认 20s）
    keep-alive-timeout: 30s  # 空闲连接释放
    max-keep-alive-requests: 1000
    accesslog:
      enabled: true
      pattern: "%h %l %u %t \"%r\" %s %b %D"   # %D = 处理耗时（微秒，AbstractAccessLogValve.java:1806 → ElapsedTimeElement(true,false)）
```

### 5.2 长连接场景（SSE / 轮询）

```yaml
server:
  tomcat:
    max-connections: 30000   # 长连接多，连接数优先
    threads:
      max: 200               # 按活跃请求算，不按连接数
    keep-alive-timeout: 60s   # 长连接保持
    max-keep-alive-requests: -1   # 不限复用次数
```

### 5.3 虚拟线程场景（JDK 21+，IO 密集首选）

```yaml
spring:
  threads:
    virtual:
      enabled: true    # maxThreads/min-spare 失效（第 8 篇 9.2）

server:
  tomcat:
    max-connections: 10000   # 连接数仍由 LimitLatch 控制
    threads:
      max: 200   # 虚拟线程下不生效，可留默认
```

### 5.4 常见坑

| 坑 | 说明 |
|---|---|
| 无脑调大 `maxThreads` | CPU 密集场景线程过多 = 上下文切换灾难 |
| 无脑调大 `accept-count` | 只是把"拒绝"变"排队"，客户端体验更差 |
| 忘记 `max-http-form-post-size` | 大文件上传被 **400** 拒绝（`FailReason.POST_TOO_LARGE`，不是 413） |
| 忽略 `connection-timeout` | 默认 20s（`SocketProperties.soTimeout`），弱网/慢客户端占连接 |
| 生产环境开 `mbeanregistry` | 有开销，按需开启 |

---

## 6. 本篇小结与面试要点

### 6.1 本篇地图

```
第 6 篇：NIO 线程模型（TaskQueue/LimitLatch 机制）
第 8 篇：配置映射表（配置项 → 源码）
第 10 篇（本篇）：场景驱动调优（为什么这么配）
第 11 篇：安全与运维（待写）
第 12 篇：故障排查（待写）
```

### 6.2 面试要点速查

1. **三个数字独立**：maxThreads（处理并发）/ maxConnections（连接并发）/ accept-count（排队）——互不替代
2. **空闲连接不占线程**：keep-alive 连接挂 Poller，只有活跃请求占 Worker——所以 8192 连接配 200 线程合理
3. **IO 密集线程 > 核数**：阻塞等待不占 CPU，线程数换吞吐；CPU 密集则 ≈ 核数
4. **TaskQueue 队列无界但"先扩线程"**：queue-capacity 调大意义有限（第 6 篇）
5. **长连接场景**：maxConnections 按内存估算，maxThreads 按活跃请求算
6. **瓶颈症状差异**：线程池耗尽=延迟增长（排队），连接打满=新连接超时（Acceptor 阻塞）
7. **jstack 线程名**：`http-nio-8080-exec-N`——协议-IO 模型-端口-线程号
8. **JMX 关键指标**：`currentThreadsBusy`/`connectionCount`
9. **虚拟线程**：maxThreads 失效，IO 密集高并发首选（第 8 篇）
