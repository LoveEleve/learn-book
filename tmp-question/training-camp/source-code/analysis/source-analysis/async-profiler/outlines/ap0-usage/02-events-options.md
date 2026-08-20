# 02. 四类事件,四类问题 — 事件与采样参数

> 🟢 使用域 | 覆盖: -e 事件 / -d / -f / -i / 高级参数
> 读者处境: CPU 高、内存涨、锁竞争、阻塞——四类问题各有一个对应的事件。

### 1. "按问题选事件" — -e 四件套

场景: 排查对象不同,采样事件不同。

- `-e cpu`(默认): CPU 热点——perf_events 硬件计数器(AP-2 源码域)
- `-e alloc`: **分配采样**——谁在 new 对象(内存增长/GC 压力)
- `-e lock`: Java 锁竞争——谁在等锁(与 Arthas `thread -b` 的锁视角互补,AR-3)
- `-e wall`: 墙钟——线程阻塞在哪(IO 等待/睡眠)
- 高级: `-e tlab`/`-e nativemem`/`-e trace`(方法调用追踪)/`-e proc`(进程级)
- 语法: `asprof -e alloc -d 30 -f alloc.html 8983`
- [Linux: cpu 事件走 perf_event_open 硬件计数器;wall 走信号定时器;alloc 靠字节码插桩(AP-3 instrument.cpp)——**不同事件,完全不同的采集机制**,这将在 AP-2/AP-3 深挖]

关键设计: **一个采样器,多引擎**: cpu/itimer/wallClock/alloc 是四种独立引擎(AP-2 的 Engine 抽象)——用户只选事件,引擎自动切换。与 Arthas 对照: Arthas `--event cpu|alloc|lock|wall` 参数直接透传 async-profiler(AR-6 篇 1 的 executeArgs 拼串)。

### 2. "采样三参数" — -d / -f / -i

场景: 采多久、存哪、多密。

- `-d <seconds>`: 采样时长(超时自动 stop)
- `-f <file>`: 输出文件(按扩展名推断格式,如 .html/.jfr/.txt)
- `-i <interval>`: 采样间隔(默认 10ms;**越短越精确,开销越大**)
- 组合: `asprof -e wall -d 60 -i 5ms -f block.html 8983`

关键设计: interval 是精度/开销的旋钮——10ms 对 99% 场景足够;高负载生产用 20-50ms 降开销,热点不明显才往下调。这与 Arthas `-i 200ms`(AR-3 的采样间隔)是同一哲学: **采样是"统计"而非"实测"**(AR-6 篇 2 的伯努利采样语义)。

### 3. "自动化与过滤" — 高级参数

场景: CI 里跑、只关心业务代码。

- `--timeout`/`--loop`: 定时自动停/循环采样(CI 集成: `asprof start --loop 300s -f out-%t.html` 每 5 分钟一张图)
- `--include 'com/demo/*'`/`--exclude '*Unsafe.park*'`: 栈过滤(聚焦热点,去掉噪声帧)
- `--signal <N>`: 自定义信号(默认 SIGPROF;容器限制时换信号)
- `-t`/`--threads`: 按线程输出
- `--memlimit`: 采样内存上限

生产注意: 过滤表达式用栈帧匹配,`--exclude '*Unsafe.park*'` 能去掉"等待"类噪声——这是火焰图"宽而不热"问题的常规解法。

---

跨域桥: 事件→引擎映射 = AP-2(engine.cpp + cpuEngine + wallClock + allocTracer);interval 语义 = AP-2(采样循环);Arthas 参数透传 = Arthas AR-6 篇 1(executeArgs)。

**OpenJDK 关联**: [域 32 JFR — outlines/32-jfr/] — JFR 同为采样框架,事件选择/引擎概念可对照。
