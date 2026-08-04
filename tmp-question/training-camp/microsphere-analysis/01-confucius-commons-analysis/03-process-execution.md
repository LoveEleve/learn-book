# 进程执行 —— JDK 6 Process API 的三个设计缺陷与 workaround

> 源码对应：`ProcessExecutor.java`（138行）、`ProcessManager.java`（49行）
> JDK 11 对比：`ProcessHandle`、`Process.onExit()`、`ProcessBuilder.redirectErrorStream()`

---

## 一、JDK 6 的 Process API —— 你只有一个选择：无限等待

### 1.1 最简调用是什么样的

```java
// JDK 6 最简方式执行外部命令
Process process = Runtime.getRuntime().exec("ls -la");

// 读输出
InputStream stdout = process.getInputStream();

// 等待结束 —— 无超时参数！进程挂死则线程永远阻塞
int exitCode = process.waitFor();  // 阻塞，直到进程结束
```

你需要的是一个带超时的执行，但 JDK 6 的 `waitFor()` 没有超时重载。在 JDK 8 之前，任何需要超时控制的进程执行都必须**自己实现轮询**。

### 1.2 JDK 6 缺失的 API 清单

| 你需要的功能 | JDK 6 | JDK 版本 |
|---|---|---|
| 带超时等待 | ❌ | JDK 8: `waitFor(long timeout, TimeUnit)` |
| 检查是否存活 | ❌ | JDK 8: `isAlive()` |
| 异步事件驱动 | ❌ | JDK 9: `onExit().thenAccept(p -> {...})` |
| 获取 PID | ❌ （需反射 hack `RuntimeMXBean`） | JDK 9: `process.pid()` |
| stdout/stderr 合并 | ❌ | JDK 5: `ProcessBuilder.redirectErrorStream(true)`（但很多人不知道） |
| 高效流传输 | ❌ | JDK 9: `transferTo(OutputStream)` |

---

## 二、永恒原理:轮询 vs 阻塞--进程等待的古老难题

JDK 6 的 `Process.waitFor()` 是无限阻塞的--进程不结束就永远等。这逼迫框架作者用「轮询」解决--`Thread.sleep` + 循环检查 `process.exitValue()`。这是个经典的并发设计取舍，值得拔高一层讲。

### 2.1 轮询 vs 阻塞:时间效率的取舍

**阻塞模式**（理想）：线程睡眠等待事件，事件来时 OS 唤醒线程。零 CPU 开销、毫秒级响应。但 JDK 6 的 `waitFor()` 不支持超时--一旦阻塞就无法超时返回。

**轮询模式**（JDK 6 的无奈之举）：`Thread.sleep(N)` 睡眠 N 毫秒 -> 检查 `process.exitValue()` -> 未结束则再睡 -> 已结束则返回。有超时能力，但代价是 N 毫秒的响应延迟 + 每次 sleep 的上下文切换开销。

| 维度 | 阻塞（`waitFor`） | 轮询（`sleep` + `exitValue`） |
|---|---|---|
| 超时支持 | ❌（JDK 6） | ✅ |
| 响应延迟 | 毫秒级 | 取决于 sleep 间隔（默认 1s） |
| CPU 开销 | 零（睡眠） | 低（每秒一次上下文切换） |
| 异常控制流 | `exitValue` 不抛异常 | `exitValue` 抛 `IllegalThreadStateException`（未结束）用于控制流 |

**这个取舍在并发编程里反复出现**：[§9 文件监听](../02-microsphere-java-analysis/09-file-watch-service.md) 的「轮询 vs 事件驱动」是同一个取舍。JDK 6 时代的限制迫使开发者用轮询绕过；JDK 7+ 给了 `waitFor(long, TimeUnit)` 和 `Process.onExit()`（CompletableFuture），轮询就不再必要。

### 2.2 ProcessExecutor 的轮询设计--机制与参数

ProcessExecutor 用 `while(!finished)` 循环 + `Thread.sleep(waitForTimeInSecond)` 实现超时控制，核心逻辑：每轮检查超时 -> 读取 stdout/stderr -> 调 `exitValue()`（未结束抛 `IllegalThreadStateException` 继续循环）-> 已结束则返回。轮询间隔通过系统属性 `-Dprocess.executor.wait.for=0.5` 配置，默认 1 秒。

**系统属性 vs 构造参数的取舍**：系统属性给全局配置、不需改代码，但一个 JVM 只有一个值--同时执行快速命令（希望 50ms 轮询）和慢速命令（希望 5s 轮询）无法共存。这是「全局配置 vs 实例配置」的经典取舍，在很多框架里出现（如 log 级别的全局配置 vs 实例配置）。

**跨语言对比**：Go 的 `os/exec` 用 `context.Context` 提供超时 + 取消，语义清晰且无需轮询；Python 的 `subprocess.run(timeout=N)` 原生支持超时；Node.js 的 `child_process.exec` 用事件回调。**Java 是唯一一个在 JDK 6 时代迫使开发者用轮询解决超时的主流语言**--这是 JDK Process API 的历史欠账，直到 JDK 8 的 `waitFor(long, TimeUnit)` 才还清。

## 三、三个生产陷阱，从系统调用到设计取舍

### 3.1 陷阱1：单字节 I/O —— 为什么要缓冲

**源码位置**：`ProcessExecutor.java:93-98`

```java
while (processInputStream.available() > 0) {
    outputStream.write(processInputStream.read());  // ← 每字节：read() syscall + write() syscall
}
```

**系统调用的真正开销**：

一次 `read()` 在 Linux 上的执行路径：

```
1. 用户态 → 内核态切换（syscall 指令，保存寄存器，切换页表）：~50-100 CPU cycles
2. 内核处理：找 fd → 检查权限 → 从管道缓冲区拷贝 1 字节 → 返回：~100-500 cycles
3. 内核态 → 用户态切换（恢复寄存器，TLB flush）：~50-100 cycles

总计：~200-700 cycles / 一次 read()
```

对于 10KB 输出：10,240 次 read() + 10,240 次 write() = 20,480 次系统调用 = **~4M-14M CPU cycles**。

对比缓冲 I/O（8KB 缓冲区）：2 次 read(buf) + 2 次 write(buf, 0, len) = **~800-2,800 cycles**。

**性能差距**：缓冲方式快 **~10,000 倍**。

**为什么 confucius-commons 没做缓冲？**

这是实际代码的 tradeoff——在最简实现和性能优化之间，选择了最简实现。对于大多数调用子进程的场景（子进程输出几百字节以内），单字节 I/O 的性能影响可以忽略。

**JDK 9+ 的正确做法**：

```java
// JDK 9+: transferTo 内部自动使用 8KB 缓冲区
process.getInputStream().transferTo(outputStream);
```

### 3.2 陷阱2：Thread.sleep 轮询 —— 延迟与 CPU 的 classic tradeoff

**源码位置**：`ProcessExecutor.java:107`

```java
waitFor(waitForTimeInSecond);  // Thread.sleep(1000)
```

**延迟分析**：

假设子进程在 sleep(1000ms) 第 1ms 就结束了：

```
主线程: [开始] → ......... [sleep 1000ms] .......... → [醒来检查] → 发现进程已结束
子进程:            [1ms] → 结束了！
浪费:                                 999ms！
```

| sleep 时长 | 最坏延迟 | CPU 唤醒频率 |
|---|---|---|
| 1000ms（默认） | 1000ms | 1 次/秒 |
| 100ms | 100ms | 10 次/秒 |
| 10ms | 10ms | 100 次/秒 |
| 1ms | 1ms | 1000 次/秒（开销接近 busy-loop） |

**JDK 6 的另一可行方案——`Process.waitFor()` + 另一个线程超时杀死**：

```java
// 不用轮询的替代方案——但仍然是 workaround
Thread worker = new Thread(() -> process.waitFor());
worker.start();
worker.join(5000);  // 等 5 秒
if (worker.isAlive()) {
    process.destroy();       // 超时 → 杀死子进程
    worker.interrupt();      // 中断等待线程
    throw new TimeoutException();
}
```

但 `ProcessExecutor` 没有选择这个方案——可能是因为 `waitFor()` 在进程正常结束前会一直阻塞，用 `join()` + `interrupt()` 来中断它不够优雅（`waitFor()` 不响应 interrupt）。

**JDK 9+ 的替代**：

```java
process.onExit()                       // CompletableFuture<Process>
    .orTimeout(5, TimeUnit.SECONDS)    // 5 秒超时
    .thenAccept(p -> System.out.println("Exit: " + p.exitValue()))
    .exceptionally(ex -> {
        process.destroyForcibly();
        return null;
    });
```

### 3.3 陷阱3：异常做控制流 —— 是 API 设计缺陷，但在 JDK 6 下是唯一选择

**源码位置**：`ProcessExecutor.java:99-104`

```java
try {
    exitValue = process.exitValue();   // 试图获取退出码
    if (exitValue != 0) throw new IOException();  // 非零退出 → 异常
    finished = true;                    // 正常结束
} catch (IllegalThreadStateException e) {
    // 进程还没结束 → 用异常作为「没结束」的信号 → 轮询
    waitFor(waitForTimeInSecond);
}
```

**这段代码的两个层面**：

| 层面 | 是谁的问题 | 代码是否有替代方案 |
|---|---|---|
| `exitValue()` 在进程没结束时抛异常 | **JDK 设计缺陷** | JDK 6 没有 `isAlive()`，没有办法。JDK 8 才修复。 |
| 把 catch 当作「检查是否结束」的机制 | **代码的务实选择** | 在 JDK 6 下没有更好的方案。但在 catch 中应该打印日志（至少 trace 级别），而不是完全静默。 |
| 如果进程真的崩溃（不是「还没结束」）时的异常？ | **JDK 和代码的共同盲区** | `IllegalThreadStateException` 只有一个原因：「进程还没结束」。但如果进程被 OS kill了（SIGKILL），`exitValue()` 会返回退出码而不是抛异常。所以这个 catch 覆盖的场景是完整且正确的。 |

**JDK 8 fix 之后的标准写法**：

```java
// JDK 8+: 不再需要异常做控制流
while (process.isAlive()) {
    Thread.sleep(1000);
}
int exitCode = process.waitFor();  // 此时进程肯定已结束，不阻塞
```

`isAlive()` 是一个纯粹的查询——返回 boolean，不抛异常。JDK 花了两大版本才提供了这个简单的方法。

---

## 四、ProcessManager —— 孤儿进程防御机制

4.1 机制:进程注册表 + ShutdownHook

ProcessManager 是个单例，内部维护一个 `ConcurrentHashMap<Process, String>`（Process -> 启动参数，用于日志诊断）。`ProcessExecutor` 每次启动子进程时调 `addUnfinishedProcess` 注册，进程结束时调 `removeUnfinishedProcess` 注销。JVM 退出时（ShutdownHook 触发），遍历注册表对每个未结束的 Process 调 `destroy()`（发送 SIGTERM）。

**这是「注册表 + 生命周期钩子」模式**--和 Spring 的 `DisposableBean` + `ApplicationContext.registerShutdownHook`、Netty 的 `EventLoopGroup.registerShutdownHook`、Guava 的 `ShutdownHookManager` 同构。核心都是：用注册表跟踪需要清理的资源，用 JVM 退出钩子统一清理。

### 4.2 为什么 JVM 退出不等于子进程退出

`Runtime.exec()` 在 Linux 上的底层是 `fork()` + `exec()`：

```
JVM 进程 (PID=1000)
    ↓ fork()
子进程 (PID=1001, PPID=1000)
    ↓ exec("command")
子进程独立运行

// 如果 JVM 退出（PID=1000 终止）
// 子进程 (PID=1001) 仍然在运行！
// 它的 PPID 从 1000 变成 1（init/systemd）
// → 孤儿进程（orphan process）
```

### 4.3 ProcessManager + ShutdownHook 的配合

```java
// JVM 启动时注册 ShutdownHook（在 ProcessManager 加载时做）
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    Map<Process, String> unfinished = ProcessManager.INSTANCE.unfinishedProcessesMap();
    if (unfinished.isEmpty()) return;
    
    System.err.printf("[ProcessManager] Destroying %d orphan processes:%n", unfinished.size());
    for (Map.Entry<Process, String> entry : unfinished.entrySet()) {
        Process p = entry.getKey();
        String args = entry.getValue();
        System.err.printf("  PID=%s, command=%s%n", 
            ManagementUtils.getCurrentProcessId(), args);  // confucius 的 PID 获取
        p.destroy();  // SIGTERM
        
        // 等 5 秒，如果还没死 → SIGKILL
        boolean exited = p.waitFor(5, TimeUnit.SECONDS);
        if (!exited) {
            System.err.printf("  PID=%s did not respond to SIGTERM, sending SIGKILL%n", 
                ManagementUtils.getCurrentProcessId());
            p.destroyForcibly();
        }
    }
}));
```

confucius-commons 的 `ProcessManager` 是一个**防御性设计**——它假设 `ProcessExecutor.execute()` 可能在 while 循环中崩溃（JVM OOM、栈溢出、System.exit），此时 finally 块不会执行，子进程留在 `unfinishedProcessesCache` 中。ShutdownHook 在 JVM 正常退出或 `System.exit()` 时都会执行——它是挽救孤儿进程的最后机会。

### 4.4 JDK 11 的替代——`ProcessHandle`

```java
// JDK 11: JVM 内部已维护父子进程关系
ProcessHandle.current()           // 当前 JVM 的 ProcessHandle
    .children()                   // Stream<ProcessHandle> — JVM 直接管理的子进程
    .forEach(child -> {
        System.out.printf("PID=%d, cmd=%s, CPU=%dms%n",
            child.pid(),
            child.info().command().orElse("unknown"),
            child.info().totalCpuDuration().orElse(Duration.ZERO).toMillis());
        
        child.destroy();          // 等价于旧 Process.destroy()
    });
```

JDK 11 不再需要 `ProcessManager` + `ShutdownHook`——JVM 在退出时自动回收它创建的所有子进程。

---

## 五、生产案例分析 —— 管道死锁导致子进程输出不完整

### 5.1 现象

线上服务偶尔调用子进程后拿到的输出不完整——stdout 在中间断开，最后几行缺失。

### 5.2 根因分析

JDK 的 `Process` 对象有三条独立的 OS 管道：stdin、stdout、stderr。每条管道的 OS 缓冲区是有限的（Linux 默认 64KB）：

```
子进程                         父进程（JVM）
                                    ProcessExecutor
┌─────────┐    stdout 管道(64KB)      ┌──────────────┐
│         │ ═════════════════════════>│  读 stdout    │
│  子进程  │                         │              │
│         │    stderr 管道(64KB)      │  读 stderr    │
└─────────┘ ═════════════════════════>│              │
                                     └──────────────┘
```

**死锁场景**：

1. 子进程写入 65KB 到 stderr（超过 64KB 管道缓冲区 → 子进程阻塞等父进程读取）
2. 父进程的 `ProcessExecutor` 先读 stdout（while 循环顺序读），还没读 stderr
3. 子进程在 stderr 写入上阻塞 → 无法继续写 stdout → stdout 管道不为空但子进程不再写
4. 父进程读完已有的 stdout → 以为「子进程输出完了」→ 等超时或等退出码
5. 子进程永远阻塞在 stderr 写入上 → 父进程超时 → 拿到不完整的 stdout

**关键**：`ProcessExecutor` 的顺序读取（`while(stdout...) {} while(stderr...) {}`）是一先一后——如果 stderr 先满了而 stdout 还没读完，死锁。

### 5.3 修复方案

**方案A：多线程同时读两个流**

```java
// JDK 6 的正确但繁琐的写法
byte[] stdoutBytes;
byte[] stderrBytes;

Thread stdoutThread = new Thread(() -> {
    stdoutBytes = readAllBytes(stdout);  // 在独立线程中阻塞读
});
Thread stderrThread = new Thread(() -> {
    stderrBytes = readAllBytes(stderr);
});
stdoutThread.start();
stderrThread.start();
stdoutThread.join(5000);  // 5 秒超时
stderrThread.join(5000);

stdoutThread.interrupt();
stderrThread.interrupt();
```

**方案B：JDK 5 的 `redirectErrorStream(true)`**

```java
ProcessBuilder pb = new ProcessBuilder("command");
pb.redirectErrorStream(true);   // stderr → stdout，只有一条管道，不存在死锁
Process process = pb.start();
```

但 `ProcessExecutor` 用的是 `Runtime.exec(command)`（便捷方式），不是 `ProcessBuilder`——所以没有 `redirectErrorStream` 选项。这是 `Runtime.exec()` vs `ProcessBuilder` 的另一个差异——`Runtime.exec()` 更简洁但功能少。

### 5.4 验证方法

写一个测试子进程来重现问题：

```java
// 子进程：先写 65KB 到 stderr（填满管道），再写 stdout
public class StderrFlood {
    public static void main(String[] args) {
        byte[] data = new byte[65 * 1024];  // 65KB > 64KB 管道缓冲区
        System.err.write(data);             // 阻塞——管道满了
        System.err.flush();
        System.out.println("This line will never be printed!");
    }
}
```

用 `ProcessExecutor` 执行这个子进程 → 永远不会正常结束 → 超时或死锁。

---

## 六、面试要点

**Q1：「`Runtime.exec()` 和 `ProcessBuilder` 有什么区别？什么时候该用哪个？」**

答案：`Runtime.exec()` 是 JDK 1.0 的古老 API——便捷但功能少（命令字符串、不支持 redirectErrorStream、不支持修改环境变量）。`ProcessBuilder` 是 JDK 5 引入的替代——支持命令列表（避免 shell 注入）、支持环境变量修改、支持 redirectErrorStream、支持工作目录设置。**任何时候都应该用 `ProcessBuilder`——除非你在写 JDK 1.0 的代码。**

追问：「`Runtime.exec("cmd arg1 arg2")` 和 `new ProcessBuilder("cmd", "arg1", "arg2").start()` 在 OS 层面有什么不同？」
回答：`Runtime.exec(String)` 内部会用 `StringTokenizer` 按空格分割参数 → 如果你传的参数包含空格（如文件路径 `"C:\\Program Files\\app.exe"`），会被错误分割。`ProcessBuilder` 的数组形式不允许这种歧义。

**Q2：「为什么子进程的输出偶尔不完整？」**

答案：管道死锁。stdout 和 stderr 是两条独立的 OS 管道，各有 64KB 缓冲区。如果子进程在 stderr 管道写满了而父进程只读了 stdout，子进程会阻塞在 stderr 上——导致 stdout 也停止写入。解决方案：① JDK 5+：`redirectErrorStream(true)` 合并两个流；② 多线程同时消费两个流。

**Q3：「用 `Process.exitValue()` 检查进程是否结束有什么问题？」**

答案：`exitValue()` 的名字暗示「获取已退出进程的退出码」，不是「检查进程是否活着」。它的行为是：进程未结束 → 抛异常。这是 JDK 早期的 API 设计缺陷——在 JDK 8 引入 `isAlive()` 之前，检查进程存活状态只能依赖异常做控制流。不要在新代码中这样做。

**Q4：「JVM 退出后，`Runtime.exec()` 创建的子进程会怎样？」**

答案：变成孤儿进程——父进程（JVM）退出后子进程由 `init`（PID=1）接管，继续运行直到自然结束或被 kill。如果子进程是长时间运行的（如启动了一个后台服务），会一直占用资源。防御：`ProcessManager` + `ShutdownHook` 在 JVM 退出前 destroy 所有子进程。
