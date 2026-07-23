# 10-JVM 云原生架构

> 训练营方向：JVM 在容器化/K8s 环境下的运维与诊断。
> 语雀子页面为空，基于技术专家知识发散补充。

---

## 核心命题：JVM 不是为容器设计的

JVM 诞生于 1995 年——远早于 Docker（2013）和 K8s（2015）。它的默认行为基于一个假设：**"这台机器所有资源都是我的"**。容器打破了这假设。本文列出 JVM 在云原生环境下的 12 个挑战及应对方案。

---

## 一、内存限制：JVM 默认用宿主机内存

**问题**：容器分配 512MB，JVM 认为能用 32GB。

```
K8s 配置：memory: 512Mi
JVM 实际看到：MaxHeapSize = 32GB / 4 = 8GB

结果：JVM 的 -Xmx 默认取的是宿主机物理内存，不是容器限制。
  → Heap 膨胀超过 512MB
    → OS 层面 OOM Killer 杀死 Pod
      → K8s 以为 Pod 挂了，重启
        → 再 OOM，再重启
          → CrashLoopBackOff
```

**解决方案**：

```dockerfile
# JDK 10+（推荐）
-XX:+UseContainerSupport        # 默认开启，JVM 感知容器内存限制
-XX:MaxRAMPercentage=75.0       # 非 Heap（线程栈+Meta+NMT）留 25%

# JDK 8（生产上还有大量 JDK 8 的容器）
-XX:+UnlockExperimentalVMOptions
-XX:+UseCGroupMemoryLimitForHeap
```

**验证**：`java -XX:+PrintFlagsFinal | grep MaxHeapSize` 确认 Heap 大小 ≈ 容器限制 × 75%。

---

## 二、CPU 限制：JVM 默认用宿主机核心数

**问题**：容器分配 2 核，JVM 认为能用 32 核。

**影响链**：
```
ParallelGCThreads = CPU 核心数 × 5/8 = 32 × 5/8 = 20 个 GC 线程
  → 实际只有 2 个核，20 个 GC 线程争抢
    → CPU Throttling：线程竞争 + 上下文切换
      → GC 更慢，STW 更长
```

**影响的不止 GC**：JIT 编译线程数、ForkJoinPool 并行度、ConcurrentHashMap 分区数——全部基于假的核心数计算。

**解决方案**：

```yaml
# K8s Pod spec
resources:
  limits:
    cpu: "2"
  requests:
    cpu: "2"
```

```dockerfile
# JDK 10+
-XX:ActiveProcessorCount=2      # 显式告诉 JVM：你只有 2 个核

# 或让 JVM 自动感知（JDK 10+ 默认，但推荐显式声明）
```

---

## 三、Metaspace：不在 -Xmx 内的隐形杀手

**问题**：`-Xmx` 只限制 Heap。Metaspace 不在 Heap 内——不受 `-Xmx` 控制。

```
容器内存 = Heap（-Xmx） + Metaspace + 线程栈 + JIT Code Cache + GC Overhead + Native
```

**生产场景**：应用使用 CGLIB/动态代理生成大量类（Spring AOP、Hibernate、MyBatis Mapper 代理）。Metaspace 可能膨胀到 200-500MB——不在 `-Xmx` 内，也不受限。

结果：Heap 还远没满（100MB/512MB），但 Metaspace 把总内存撑破了 → OOM Killer。

**解决方案**：

```dockerfile
-XX:MaxMetaspaceSize=256m       # 必须限制！不是可选

# 完整配置
-XX:MaxMetaspaceSize=256m
-XX:CompressedClassSpaceSize=64m # Class 指针压缩空间（Meta 的子集）
```

---

## 四、Native Memory Tracking 的真相：为什么 top 显示的内存远大于 -Xmx

NMT 公式：`Java进程内存 = Heap + Class(metaSpace) + Thread + Code(JIT) + GC + Compiler + Internal + Symbol`

**最容易忽视的是线程栈**：

```
应用有 200 个线程（Tomcat 200 + 连接池 50 + 异步 50 = 300）
-Xss 默认 1MB/线程 → 线程栈占用 = 300 × 1MB = 300MB
```

**线程栈不受任何 -X 参数限制**！这是容器 OOM 的第二大头号杀手（仅次于 Metaspace）。

**排查命令**：
```bash
jcmd <pid> VM.native_memory summary
# 输出 8 个内存区域的实际占用
```

---

## 五、K8s 探针与 GC 暂停的致命冲突

```
Readiness Probe：每 5s GET /actuator/health
Full GC：暂停 3s
→ 碰上了：Probe 在 GC 期间超时 → K8s 以为 Pod 挂了 → 重启 Pod
  → 重启需要 30s → 期间流量打到其他 Pod → 连锁过载
```

**解决方案**：

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 5       # > 最大 GC 暂停时间
  failureThreshold: 6     # 6 次失败 = 30s 后摘除
                           # 不因为一两次 GC 暂停就重启
```

---

## 六、GC 在小容器下的性能崩溃

不同 GC 在小容器下的表现天差地别：

| GC | 512MB 容器 | 2GB 容器 | 适用性 |
|---|---|---|---|
| **Serial** | ⭐⭐⭐ 最好 | ⭐ 不适合 | 小容器专用 |
| **G1** | ⭐ 开销大，暂停时间长 | ⭐⭐⭐ 默认选择 | 大容器 |
| **ZGC** | ⭐ 开销 15-20% | ⭐⭐⭐ 低延迟 | JDK 21+ 分代 ZGC |
| **Parallel** | ⭐⭐ 可用 | ⭐⭐ 可用 | 吞吐优先 |

**小容器为什么不推荐 G1**：G1 需要额外的记忆集（Remembered Set）存储跨 Region 引用——小 Heap 下记忆集的相对开销更大。

**小容器推荐**：512MB 以下用 Serial（简单高效），512MB-2GB 用 Parallel（吞吐优于 G1）。

---

## 七、启动时间：JVM 的致命劣势

```
JVM（Spring Boot）：20-60s
JVM + GraalVM Native：0.05s
Go：0.05s
```

K8s Pod 调度默认超时 30s——JVM 没启动完就被 K8s 判定失败。

**启动慢的三个阶段**：
1. 类加载（5-10s）：Spring 扫描 ClassPath，加载几百上千个类
2. Bean 初始化（10-30s）：创建 Bean、注入依赖、建立连接池
3. JIT 预热（10-30s）：前几千次调用是解释执行→C1 编译→C2 编译

**解决方案**：

```yaml
# K8s 配置
startupProbe:
  failureThreshold: 30     # 允许 30 次失败 = 150s 启动窗口
  periodSeconds: 5
```

```dockerfile
# JVM 配置
-XX:+TieredCompilation         # 先用 C1 快速编译，后台 C2 优化
-XX:TieredStopAtLevel=1        # 极端场景：只用 C1，牺牲峰值换启动速度
```

**终极方案**：CRaC（Coordinated Restore at Checkpoint）——启动后立刻做快照，重启时从快照恢复，启动时间降到毫秒级。Spring Boot 3.2+ 原生支持。

---

## 八、容器信号处理：SIGTERM 的延迟灾难

```
K8s 发送 SIGTERM → JVM 启动 ShutdownHook
  → 但 JVM 正在 Full GC（STW 暂停中）→ 信号处理线程也被暂停！
    → Full GC 结束 → 才开始处理 ShutdownHook
      → 但已经过了 5s → K8s 等了 30s（terminationGracePeriodSeconds）
        → 还没等到 → SIGKILL → 数据丢失
```

**解决方案**：

```yaml
# K8s
spec:
  terminationGracePeriodSeconds: 60  # 给 JVM 充足的关机时间
  containers:
  - preStop:
      exec:
        command:
        - curl
        - -X POST
        - http://localhost:8080/actuator/shutdown
```

```dockerfile
# JVM：提前触发 GC，减少停机时的 GC 风险
-XX:+ExplicitGCInvokesConcurrent     # System.gc() 用并发 GC
```

---

## 九、JVM 日志与容器 stdout 的割裂

**容器最佳实践**：日志写到 stdout/stderr——K8s 自动采集。

**JVM 默认行为**：GC 日志写文件、JFR 写文件、Heap Dump 写文件。

**问题**：
- GC 日志在 `/opt/app/gc.log`——容器炸了找不到
- JFR 录制了几百 MB 的数据在容器 rootfs——磁盘空间不够
- Heap Dump 文件在容器内部——怎么取出来分析？

**解决方案**：

```dockerfile
# GC 日志输出到 stdout
-Xlog:gc*:stdout:time,level,tags

# JFR 流式输出（JDK 14+）
-XX:StartFlightRecording=disk=false,dumponexit=true,filename=/stdout

# Heap Dump：kubectl cp 或挂载 PersistentVolume
-XX:HeapDumpPath=/mnt/dumps/
```

---

## 十、跨容器 JVM 诊断

**以前**：SSH 进机器 → jstack/jcmd/jmap/jstat 直接连本地 JVM。

**现在**：Pod 隔离——不能 SSH 进去。怎么办？

```
kubectl exec -it <pod> -- jcmd 1 Thread.print    # jstack 替代
kubectl exec -it <pod> -- jcmd 1 GC.heap_dump /tmp/dump.hprof
kubectl cp <pod>:/tmp/dump.hprof ./dump.hprof     # 取文件
```

**更好的方案**：在应用镜像中打包诊断工具。

```dockerfile
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y jattach
COPY target/app.jar /app.jar
```

`jattach`：轻量级的 jcmd 替代（不依赖 JDK 环境，只要 JRE 就行）。

---

## 十一、类加载器泄漏：容器环境下的定时炸弹

```
K8s Pod 重启 → 新 JVM 实例 → 新 ClassLoader → 旧 ClassLoader 引用未清理
  → ThreadLocal 持有旧 ClassLoader 的引用（线程池复用了线程，线程上还挂着旧 ThreadLocal）
    → Metaspace 慢慢被填满
      → 几小时后 OOM
```

**不是一次就能重现的 bug**。通常在灰度发布后数小时才爆发——很难排查。

**常见泄漏源**：
- `ThreadLocal` 在线程池中未清理（Tomcat Worker 线程复用）
- Logback `LoggerContext` 在 reload 时没关闭旧的
- `Introspector` 缓存了 BeanInfo（Spring 频繁内省的副作用）

**排查**：
```bash
jcmd <pid> GC.class_stats | grep "ClassLoader"
# 看到同一个类的 ClassLoader 有几十个 → 泄漏
```

---

## 十二、JDK 版本在容器中的陷阱

| JDK | 容器感知 | 问题 |
|---|---|---|
| **JDK 8** | ❌ 默认不感知 | 必须 `-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap` |
| **JDK 9** | ❌ 不感知 | 有 `-XX:+UseContainerSupport` 但是实验性 |
| **JDK 10/11** | ✅ 默认支持 | `-XX:+UseContainerSupport` 默认开启 |
| **JDK 17** | ✅ 成熟 | 分代 ZGC 开始可用 |
| **JDK 21 LTS** | ✅ 最佳 | 分代 ZGC 正式 + 虚拟线程 + Generational Shenandoah |

**生产环境的常见事故**：用 JDK 8 base image 跑容器，没加参数——JVM 按宿主机内存分配 Heap → OOM Killer。解决：在 Dockerfile 中显式加参数，或升级到 JDK 21。

---

## 总结：12 个挑战 + 应对方案

| 挑战 | 应对方案 |
|---|---|
| 1. 内存限制 | `-XX:MaxRAMPercentage=75.0` |
| 2. CPU 限制 | `-XX:ActiveProcessorCount=N` |
| 3. Metaspace 无上限 | `-XX:MaxMetaspaceSize=256m` |
| 4. NMT 超限 | `jcmd <pid> VM.native_memory summary` |
| 5. 探针 vs GC | `timeoutSeconds > 最大 GC 暂停` |
| 6. 小容器 GC 崩溃 | 512MB→Serial, 512MB-2GB→Parallel |
| 7. 启动慢 | CRaC + `startupProbe` 150s 窗口 |
| 8. SIGTERM 延迟 | `terminationGracePeriodSeconds: 60` |
| 9. 日志割裂 | GC 日志→stdout, JFR Streaming |
| 10. 跨容器诊断 | `kubectl exec + jattach` |
| 11. ClassLoader 泄漏 | `jcmd <pid> GC.class_stats` |
| 12. JDK 版本陷阱 | 升级到 JDK 21 LTS |

## 对 sca-lab 的影响

- **performance 模块**：ZGC 调优 + NMT 验证 + GraalVM Native 启动时间对比
- **cloudnative 模块**：K8s 探针配置 + CRaC 快照恢复

---

## 附：查漏补缺

### 直接内存（ByteBuffer）：第三个不受 -Xmx 限制的区域

Metaspace 和 线程栈 之外，还有第三个"隐形杀手"：**Direct Memory**。

```java
ByteBuffer buffer = ByteBuffer.allocateDirect(100 * 1024 * 1024); // 100MB 堆外
```

这个 100MB 不在 Heap 内，不受 `-Xmx` 限制。谁在用？**Netty**——Spring Cloud Gateway 底层就是 Netty，默认使用堆外内存做网络缓冲区。

```
容器限制：512MB
  -Xmx: 256MB
  Metaspace: 128MB
  线程栈: 200MB
  堆外内存: 100MB           ← 没有限制！
  ─────────────────────
  总计: 684MB > 512MB → OOM Killer
```

**解决**：
```dockerfile
-XX:MaxDirectMemorySize=128m     # 必须限制！和 Metaspace 一样重要
```

**排查**：`jcmd <pid> VM.native_memory summary` 中的 "Other" 区域包含 Direct Memory 用量。

### 线程栈的现代解决方案：虚拟线程

线程栈的公式 `300 线程 × 1MB = 300MB` 在 JDK 21 有了根本性的改变：

```java
// 传统线程
new Thread(() -> doWork()).start();           // ~1MB/线程

// 虚拟线程（JDK 21 Loom）
Thread.startVirtualThread(() -> doWork());    // ~1KB/线程
```

**虚拟线程的本质**：线程栈不是预分配的连续内存块，而是以 StackChunk 形式按需分配在 Heap 中。这意味着 10000 个虚拟线程的内存开销 = 10000 × ~1KB = 10MB，而不是 10000 × 1MB = 10GB。

**但不是所有场景都能用虚拟线程**：synchronized 块会 pin 住虚拟线程的载体线程（carrier thread）→ 退化到传统线程的阻塞模型→失去了"轻量级"的优势。IO 密集场景（HTTP 请求/DB 查询/消息消费）是虚拟线程的最佳场景。

### 容器镜像瘦身

JRE 基础镜像的大小直接影响 Pod 启动速度（需要从 Registry 拉取镜像）。

| 基础镜像 | 大小 | 适用 |
|---|---|---|
| `openjdk:21` | ~470MB | ❌ 太胖 |
| `eclipse-temurin:21-jre` | ~230MB | ⭐ 推荐 |
| `eclipse-temurin:21-jre-alpine` | ~180MB | ⭐⭐ 更轻（但 musl libc 有兼容性问题） |
| `jlink` 自定义运行时 | ~50-80MB | ⭐⭐⭐ 只带你的应用需要的模块 |

**jlink 制作最小 JRE**：
```dockerfile
FROM eclipse-temurin:21-jdk AS builder
RUN jlink --add-modules java.base,java.sql,java.naming,java.management,\
    java.net.http,jdk.unsupported --output /custom-jre

FROM alpine:3.20
COPY --from=builder /custom-jre /opt/jre
ENV JAVA_HOME=/opt/jre
```

### JDK 8 → 21 容器迁移检查清单

| 检查项 | JDK 8（危险） | JDK 21（安全） |
|---|---|---|
| 内存感知 | `-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap` | ✅ 默认，只需 `-XX:MaxRAMPercentage=75` |
| CPU 感知 | 手动 `-XX:ActiveProcessorCount=N` | ✅ 默认感知 |
| Metaspace | 无限制 | 必须显式 `-XX:MaxMetaspaceSize=256m` |
| GC | CMS/Parallel | G1（默认）/ ZGC（低延迟） |
| 线程 | OS 线程 1:1 | OS 线程 + 虚拟线程 |
| 字符串 | 重复字符串占用多 | 默认开启字符串去重 |
| 容器信号处理 | 非标准 ShutdownHook | ✅ 标准，但注意 Full GC 延迟信号 |

