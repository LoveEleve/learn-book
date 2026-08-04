# Stage 5：工程实践

> **本 stage 定位**：C 语言学习路线的第五阶段（最终阶段），工程化与生产落地。对应 `C-C++技术专家学习路线.md` 的 **Layer 8-11**（设计模式与工程化 + 调试与性能优化 + 真实系统源码阅读 + 生产化工程实践）。
>
> **预计时间**：3 周（每日 2-3 小时投入）+ 长期持续（源码阅读不设限）
>
> **完成本 stage 后**：能用 C 写出可维护的 1 万行项目（OOP / 模块化 / 测试 / 构建）、能用 C 写共享库被 Python/Java/Go 调用、能用 GDB / ASan / perf 定位内存泄漏与性能瓶颈、能读懂 Redis / Nginx 核心模块源码、能把 C 服务上到生产环境。

---

## 1. 学习目标

完成本 stage 后，应能达成以下 7 个目标：

1. **能用 OOP in C 写代码**（封装 = opaque pointer / 继承 = struct 嵌套 / 多态 = vtable）+ 用 `goto cleanup` 或 `cleanup` attribute 处理错误
2. **能用 C 写出可维护的 1 万行项目**（头文件设计 / 模块化 / 单元测试 / CMake 构建 / Unity 测试框架 / clang-tidy 静态分析）
3. **能用 C 写共享库被其他语言调用**（Python cffi / Java JNI / Go cgo / Rust FFI）+ 理解为什么 Rust 与 C 互操作最自然（ABI 兼容）
4. **能用 GDB 调 segfault**（断点 / watchpoint / 逆向调试 / Python 脚本化）+ 用 ASan / TSan / UBSan 定位内存与并发 bug + 用 perf + FlameGraph 找性能热点 + 理解 `-O0/-O1/-O2/-O3` / LTO / PGO 的差异
5. **能读懂 Redis / Nginx / Memcached 核心模块源码**（数据结构 `sds.c/dict.c/t_zset.c` + 事件循环 `ae.c/ae_epoll.c` + 内存池 `ngx_palloc.c` + 多线程模型 `thread.c`）
6. **能把 C 服务上到生产环境**（结构化日志 + statsd/Prometheus 指标 + 优雅关闭 + 热升级 + 安全加固 + CI/CD + core dump 配置）
7. **能写一个综合项目**：KV 存储（SDS 字符串 + 哈希表索引 + WAL 持久化 + epoll 网络层）或 HTTP 服务器（epoll + 线程池 + keepalive + chunked）

---

## 2. 对应 Layer

| Layer | 主题 | 本 stage 章节 |
|---|---|---|
| Layer 8 | 设计模式与工程化 + FFI 互操作 | `01-OOP-in-C.md` / `02-错误处理与模块化.md` / `03-单元测试与构建系统.md` / `04-FFI与其他语言互操作.md` |
| Layer 9 | 调试与性能优化 | `05-GDB进阶.md` / `06-内存调试ASan-Valgrind.md` / `07-性能分析perf-FlameGraph.md` / `08-编译优化LTO-PGO.md` / `09-静态分析cppcheck-clang-tidy.md` |
| Layer 10 | 真实系统源码阅读 | `09-Redis数据结构源码.md` / `10-Nginx事件循环与内存池.md` / `11-Memcached多线程模型.md` |
| Layer 11 | 生产化工程实践 | `12-可观测性与优雅关闭.md` / `13-热升级与安全加固.md` / `14-包管理与CI-CD.md` |

---

## 3. 学习内容（章节清单）

### 3.1 设计模式与工程化（对应 Layer 8）

#### 3.1.1 OOP in C

- **封装（opaque pointer / opaque type）**：前向声明不暴露 struct 实现，创建/销毁/操作函数分离到 .c。（注：opaque pointer 是 C 惯用法，Pimpl 是 C++ 惯用法依赖析构函数，不适用于纯 C）
- **继承（组合 + 嵌套 struct）**：子 struct 第一个成员是父 struct，可安全强转
- **多态（vtable）**：struct 嵌入函数指针表，运行时根据具体类型分发
- 实例：形状（circle / rectangle / triangle）的 vtable 多态实现

#### 3.1.2 错误处理

- **返回码 + errno**：标准库模式，每个函数返回 0 表示成功、-1 表示错误
- **goto cleanup 模式**（Linux 内核主流）：所有错误跳转到统一的 cleanup 标签，按逆序释放资源
- **cleanup attribute（GCC/Clang）**：自动释放资源，类似 C++ 的 RAII 但在 C 中有限

#### 3.1.3 模块化与头文件设计

- 头文件原则：只放声明（除 `static inline`）/ 自给自足 / include 顺序（自身→标准库→第三方→项目内）
- API 隐藏：公共 API 在 `foo.h`，私有 API 在 `foo_internal.h` / `__attribute__((visibility("hidden")))` / `static` 限制到编译单元

#### 3.1.4 单元测试

- C 测试框架对比（Unity / cmocka / Criterion / Check）
- Unity 示例：`TEST_ASSERT_EQUAL` / `RUN_TEST` / `UNITY_BEGIN/END`
- 测试驱动开发（TDD）在 C 中的实践

#### 3.1.5 构建系统

- CMake 最小示例（`cmake_minimum_required` / `project` / `add_executable` / `target_link_libraries`）
- 多目标构建：可执行文件 + 静态库 + 动态库
- 编译选项管理：`-Wall -Wextra -Werror` / Debug vs Release / C 标准选择

#### 3.1.6 C 与其他语言互操作（FFI）

- C 端规范：`extern "C"` 导出 C ABI 符号；opaque pointer 隐藏实现
- **Python cffi**：声明式接口；ABI 模式（`ffi.dlopen()`）无需编译但性能较低、无类型检查；API 模式（`ffi.set_source()`）需编译为 `.so` 但性能接近原生（推荐）
- **Java JNI**：最常用但开销大，每次调用涉及 JNI 边界
- **Go cgo**：Go 编译器处理 C 代码，每次调用涉及 goroutine 切换 + 栈切换（性能差）
- **Rust FFI**：`extern "C"` 直接匹配 C ABI，无需中间层转换，开销最低
- **Node.js N-API**：ABI 稳定的原生模块接口（跨 Node.js 主要版本兼容，无需重新编译）

---

### 3.2 调试与性能优化（对应 Layer 9）

#### 3.2.1 GDB 进阶

- 断点（行断点 / 函数断点 / 条件断点）/ 数据断点（watchpoint/rwatchpoint）
- 逆向调试（`record` + `reverse-next` / `reverse-continue`）
- Python 脚本化（`source my_script.py`）
- 调试 core 文件（`gdb ./myapp core.dump`）+ attach 到运行中进程（`-p`）

#### 3.2.2 内存调试

- 工具对比表：Valgrind memcheck（10-20x）/ ASan（2x）/ MSan（3x）/ TSan（5-15x）/ UBSan（几乎免费）/ LeakSanitizer（几乎免费）
- ASan 示例：`-fsanitize=address` 编译 → 运行时自动报告越界 / UAF / double-free 的完整调用栈
- 生产环境不能用 ASan — 替代方案：tcmalloc heap profiler / jemalloc profiling

#### 3.2.3 性能分析

- **perf stat**：CPU 周期 / 缓存命中率 / 分支预测 / IPC
- **perf record + perf report**：采样找热点函数
- **FlameGraph**：`perf script` → `stackcollapse-perf.pl` → `flamegraph.pl`
- **gprof**：函数调用次数 + 耗时（需编译时插桩 `-pg`）
- **Intel VTune**：商业工具，全面硬件事件分析

#### 3.2.4 编译优化

- 编译优化级别对比：`-O0`（调试）/ `-O1`（保守优化，`-O2` 子集，编译速度与调试信息平衡）| `-O2`（生产）/ `-O3`（激进，可能变慢）/ `-Os`（体积）/ `-Ofast`（数学宽松）
- **LTO（Link-Time Optimization）**：跨编译单元优化，能内联跨文件函数
- **PGO（Profile-Guided Optimization）**：`-fprofile-generate` 收集 profile → `-fprofile-use` 基于 profile 编译优化分支预测 + 内联决策

#### 3.2.5 静态分析（代码质量前置）

| 工具 | 特点 | 用途 |
|---|---|---|
| `-Wall -Wextra -Wpedantic -Werror` | 编译器警告=错误 | 基础防线 |
| cppcheck | 经典 C/C++ 静态分析 | CI 集成 |
| clang-tidy | Clang 现代静态分析，可自动修复 | CI 集成 |
| Coverity | 商业，业界最强 | 安全关键项目 |
| PVS-Studio | 商业，C/C++ | 深度分析 |

**深度目标**：能在 CI 中集成 clang-tidy + cppcheck + 编译警告三层静态防线，对任何新提交自动报告。

#### 3.2.6 崩溃恢复与 Core Dump

- 启用 core dump：`ulimit -c unlimited` + 配置 `/proc/sys/kernel/core_pattern`（如 `/var/core/core.%e.%p.%t`）
- 安装 SIGSEGV / SIGABRT handler，用 `backtrace(3)` 打印调用栈（**注：glibc GNU 扩展，`<execinfo.h>`，musl libc 和 macOS 不支持**；跨平台替代：libunwind）
- 事后用 `addr2line` 符号化：`addr2line -e ./myapp -f -C 0x401234`
- GDB 调试 core 文件：`gdb ./myapp core.dump` + `bt` 查看崩溃调用栈

---

### 3.3 真实系统源码阅读（对应 Layer 10）

#### 3.3.1 源码阅读清单

| 系统 | 行数 | 核心模块 | 重点学习 |
|---|---|---|---|
| **Redis** | ~10 万行 | `sds.c` / `dict.c` / `t_zset.c` / `ae.c` / `networking.c` | 数据结构 / 事件循环 / 单 Reactor 模型 |
| **Nginx** | ~20 万行 | `ngx_palloc.c` / `ngx_event.c` / `ngx_epoll_module.c` / `ngx_http_request.c` | 内存池 / 多进程 + Reactor / HTTP 解析 |
| **Memcached** | ~2 万行 | `memcached.c` / `thread.c`（~500 行有效代码）| 多线程 + 锁分片 / slab 分配器 |
| **SQLite** | ~15 万行 | `btree.c` / `wal.c` / `pager.c` | B 树 / WAL / 事务 |
| **LMDB** | ~1 万行 | `mdb.c` | mmap 持久化 / B+ 树 / COW |
| **redis-labs/redisraft** | ~3 万行 | `raft.c` / `raft_log.c` | Raft 算法 C 实现（**独立项目，非 Redis 核心；GitHub README 声明"not yet ready for production use"；最后更新 2023 年 7 月，已 3 年未维护**） |
| **Linux kernel** | 数千万行 | `kernel/sched/` / `mm/` / `fs/` / `net/` | 调度器 / 内存管理 / VFS / 网络栈 |
| **musl libc** | ~5 万行 | `src/string/` / `src/network/` / `src/thread/` | 极简 libc 实现 |

#### 3.3.2 阅读方法

1. 从入口追到核心：`main()` → 初始化 → 事件循环 → 处理流程
2. 先看数据结构，再看算法：先理解核心 struct，再看函数如何操作它
3. 结合文档与代码：先读 design doc，再读代码
4. 跑起来再读：能编译运行，加日志观察行为
5. 改一行试试：修改后重新运行，理解每行的作用

#### 3.3.3 推荐阅读顺序

1. **Redis 数据结构源码精读**：`sds.c` + `dict.c` + `t_zset.c`（约 5000 行，最容易上手）
2. **Redis 事件循环源码精读**：`ae.c` + `ae_epoll.c`（约 1000 行）
3. **Nginx 内存池源码精读**：`ngx_palloc.c`（约 1000 行）
4. **Memcached 多线程模型源码精读**：`thread.c`（~500 行，含注释）
5. **进阶**：SQLite btree.c（B 树实现）/ redis-labs/redisraft raft.c（Raft C 实现）

---

### 3.4 生产化工程实践（对应 Layer 11）

#### 3.4.1 可观测性

- **结构化日志**：自写 JSON 日志器（带时间戳 / 级别 / 上下文），替代 `printf`
- **指标采集**：statsd / Prometheus client_c / 自写 stats（关键指标：QPS / 延迟分位数 P50/P95/P99 / 错误率 / 资源占用）
- **进程指标**：`/proc/[pid]/status` / `getrusage(2)`（内存 / CPU / fd 数 / 线程数）
- **分布式追踪**（进阶）：OpenTelemetry C++ SDK / Jaeger client

#### 3.4.2 优雅关闭

- `SIGTERM` 信号处理 → 设置全局标志 → 主循环检查 → 停止 accept 新连接 → 给进行中请求设置 deadline → 缩短超时（如从正常 30s 缩短为 drain 阶段 5s）→ 等待完成或超时 → 释放资源 → `_exit(0)`
- `SIGHUP` 常用于 reload 配置或优雅重启的备选信号（配合热升级方案）
- 忽略 `SIGPIPE`（写已关闭 socket 时避免进程被杀死）

#### 3.4.3 热升级（Zero-Downtime Restart）

- **SO_REUSEPORT**（Linux 3.9+）：多进程绑定同一端口，内核负载均衡，新进程启动后老进程退出
- **信号触发老进程让出 fd**：老进程收到 SIGUSR1 → 停止 accept → 通过 SCM_RIGHTS 传 listen fd 给新进程 → 等待进行中请求完成 → 退出

#### 3.4.4 限流与熔断

- 计数器 / 滑动窗口 / 令牌桶（允许突发）/ 漏桶（严格匀速）
- 令牌桶实现：定时填令牌 + 桶容量（允许一定突发）

#### 3.4.5 崩溃恢复（详见 §3.2.6）

core dump 配置 + backtrace 符号化 + GDB 调试 core 文件的前置介绍见 §3.2.6（调试阶段）。生产化在此基础上的额外关注点：

- 配置 `/proc/sys/kernel/core_pattern` 为统一路径（如 `/var/core/core.%e.%p.%t`），配合日志聚合系统自动收集
- 生产环境建议同时保留 core dump + 应用层 backtrace（双重保障）
- 监控 core dump 产生频率作为故障告警指标

#### 3.4.6 生产环境 Profiler（不能用 ASan）

- tcmalloc heap profiler / jemalloc profiling（内存分配，~5% 开销）
- perf record（CPU profiling，<1% 开销）
- eBPF / bpftrace（系统级追踪，<1% 开销）
- gperftools CPU profiler（~3% 开销）

#### 3.4.6 崩溃恢复

- 启用 core dump：`ulimit -c unlimited` + 配置 `/proc/sys/kernel/core_pattern`
- 安装 SIGSEGV / SIGABRT handler，用 `backtrace(3)` 打印调用栈（**注：glibc GNU 扩展，`<execinfo.h>`，musl libc 和 macOS 不支持**；跨平台替代：libunwind）
- 事后用 `addr2line` 符号化：`addr2line -e ./myapp -f -C 0x401234`

#### 3.4.7 安全加固（与 Stage 1 §1.2.9 配合）

生产编译选项（推荐，需确认目标平台支持）：

```bash
-O2                                      # 优化（-D_FORTIFY_SOURCE=2 需要至少 -O1 才生效）
-D_FORTIFY_SOURCE=2                      # libc 边界检查（需配合 -O1+ 使用，Debug 编译仅 -O0 时此选项无效）
-fstack-protector-strong                 # 栈金丝雀（比 -fstack-protector 覆盖更全，保护局部数组和栈地址引用）
-fPIE -pie                               # 位置无关可执行文件（ASLR）
-Wl,-z,relro,-z,now                      # GOT 只读
-Wl,-z,noexecstack                       # 栈不可执行（NX）
-fcf-protection                          # 控制流完整性（Intel CET，需 CPU 支持：Intel Tiger Lake 11th Gen+ / AMD Zen 3+；旧 CPU 上会触发 SIGILL 非法指令，须确认部署平台硬件）
```

**注意**：`-fcf-protection` 是平台相关选项，嵌入式 / IoT / 虚拟化环境可能不支持。确认目标部署平台的 CPU 型号后再决定是否开启。

#### 3.4.8 包管理与 CI/CD

- **Conan**：C/C++ 包管理器（两个主要选项之一，与 vcpkg 并列为两大生态；注意：大部分 C/C++ 项目仍使用系统包管理器或源码包含）
- **vcpkg**：Microsoft 出品，CMake 原生集成
- **CI/CD 关键检查项**：多平台编译（Linux + macOS + Windows）/ 多编译器编译（GCC + Clang）/ ASan + TSan + UBSan 运行测试 / 静态分析（clang-tidy / cppcheck）/ 测试覆盖率（lcov / gcov）/ 性能回归（benchmark vs baseline）
- **LLDB**：macOS 默认调试器，GDB 是 Linux 主流。macOS 用户需熟悉 LLDB（命令语法类似但不同：`breakpoint set` vs `b`，`thread backtrace` vs `bt`）

---

## 4. 实战项目

### 4.1 工程化部分

1. **用 OOP in C 实现形状类层次**（circle / rectangle / triangle）：vtable 多态
2. **写一个 C 库**：头文件设计 + CMakeLists.txt + 单元测试（Unity）
3. **重构一段历史代码**：用 opaque pointer 隐藏实现，用 `cleanup` attribute 简化错误处理
4. **写共享库被三种语言调用**：Python cffi / Java JNI / Go cgo

### 4.2 调试与性能部分

5. **用 GDB 调一个 segfault 程序**：找到崩溃位置 + 根因 + 逆向调试
6. **用 ASan / TSan / UBSan 定位内存 bug**：写有 UAF / 越界 / double-free / 数据竞争 的程序
7. **用 perf + FlameGraph 找出程序热点**：优化一个排序程序，对比 `-O2` vs `-O3` 性能差异
8. **配置 core dump + backtrace 符号化**：制造一个崩溃，从 core 文件还原调用栈

### 4.3 源码阅读与生产化部分

9. **Redis 数据结构源码精读**：`sds.c` + `dict.c` + `t_zset.c`，写注释 + 简化实现
10. **Redis 事件循环源码精读**：`ae.c` + `ae_epoll.c`，理解 epoll 在实战中的用法
11. **给一个 C 服务添加可观测性**：结构化日志 + statsd 指标
12. **实现优雅关闭**：SIGTERM 触发 → 完成进行中请求 → 5 秒超时 → 退出
13. **用 SO_REUSEPORT 实现热升级**：老进程 + 新进程同时监听，平滑切换
14. **搭建 C 项目的 CI/CD**：GitHub Actions + 多平台编译（GCC + Clang + ASan 测试）

### 4.4 综合项目（二选一或多选）

15. **写一个 KV 存储**（SDS 字符串 + 哈希表索引 + WAL 持久化 + epoll 网络层 + 协议解析）
16. **写一个生产级并发 HTTP 服务器**（epoll + 非阻塞 I/O + 线程池 + keepalive + chunked + 超时管理 + 请求头大小限制 + Host 头校验 + 慢连接检测 — 注：与 Stage 4 的基础 HTTP 服务器不同，本版本增加了完整生产级特性）

---

## 5. 参考书章节

| 主题 | 极致C | Fluent C | Practical | Mastering | 高效调试 | Memory Thinking |
|---|---|---|---|---|---|---|
| OOP in C | Ch6-8 | 全书 | — | — | — | — |
| 错误处理与模块化 | Ch22, 23 | 全书 | — | — | — | — |
| 单元测试与构建系统 | Ch22, 23 | — | — | — | — | — |
| FFI 互操作 | Ch21 | — | — | — | — | — |
| GDB 进阶 | Ch22 | — | — | — | 全书 | — |
| 内存调试 | Ch22 | — | — | Ch10 | 全书 | — |
| 性能分析 | — | — | — | — | 全书 | — |
| 编译优化 | Ch2 | — | — | — | Ch5 | — |
| 源码阅读（Redis/Nginx/Memcached） | 全书辅助 | — | — | — | — | — |
| 生产化工程实践 | — | — | — | — | — | — |

**重点参考**：
- Fluent C 全书（C 设计模式 — OOP 与错误处理的最佳实践）
- 高效调试 全书（调试技术深度 — 从符号到优化代码全覆盖）
- 极致C Ch6-8（OOP 语法）+ Ch21（FFI 互操作）+ Ch22-23（测试 + 构建系统）

**注**：生产化工程实���（Layer 11）6 本书覆盖不足，需补充外部资料：
- 《Linux 系统编程》（Robert Love）— 系统级编程基础
- 《UNIX 网络编程 卷1》（W. Richard Stevens）— 网络编程圣经
- 《性能之巅》（Brendan Gregg）— eBPF / 火焰图 / 系统性能分析
- 《BPF Performance Tools》（Brendan Gregg）— eBPF 追踪工具实战

---

## 6. 完成标准

能回答以下 14 个问题即算完成：

1. OOP in C 的三段式是什么？封装 / 继承 / 多态各用什么机制实现？
2. C 的三种错误处理模式（返回码+errno / goto cleanup / cleanup attribute）的适用场景？
3. 头文件设计的核心原则是什么？include 顺序？
4. C 中用什么测试框架？写一个 Unity 测试的完整示例？
5. 为什么 Rust 与 C 互操作最自然？Java JNI 和 Go cgo 各有什么性能代价？
6. GDB 中如何设置条件断点？如何逆向调试？如何调试 core dump？
7. ASan / TSan / UBSan 各检测什么问题？哪些工具可以在生产环境使用？
8. perf stat 和 perf record 的区别？如何生成火焰图？
9. LTO 和 PGO 各是什么？什么时候使用？
10. Redis `ae.c` 的事件循环模型是什么？Nginx `ngx_palloc.c` 的内存池设计要点？
11. 优雅关闭的完整流程是什么？热升级的两种方案（SO_REUSEPORT / 信号+SCM_RIGHTS）的区别？
12. C 项目的生产安全加固编译选项有哪些？每项的作用和平台依赖是什么？
13. 生产环境 C 服务的可观测性需要哪 3 个维度（日志 / 指标 / 追踪）？每种维度用什么工具实现？关键监控指标有哪些（QPS / P50/P95/P99 / 错误率 / 资源占用）？
14. **（操作性验证）** 用一个 C 共享库，写出被 Python cffi / Java JNI 两种语言调用的完整示例代码。

---

## 7. 后续：进入源码阅读与实战

完成本 stage 后，C 语言学习路线的全部 5 个 stage 已完成。后续进入**长期持续阶段**：

- **源码阅读**：持续阅读 Redis / Nginx / Memcached / SQLite / Linux kernel 源码，不设时间上限
- **分布式衔接**：读 redis-labs/redisraft 的 `raft.c`（Raft 算法的 C 实现），对照 etcd/raft Go 实现理解算法本质
- **综合项目**：完成 KV 存储或并发 HTTP 服务器的综合实战

**推荐长期路径**：

```
Stage 5 完成
    ↓
Redis 数据结构源码（sds.c/dict.c/t_zset.c）→ 2-3 周精读
    ↓
Redis 事件循环（ae.c/ae_epoll.c）→ 1 周精读
    ↓
Nginx 内存池（ngx_palloc.c）→ 1 周精读
    ↓
Memcached 多线程（thread.c）→ 1 周精读
    ↓
综合项目：KV 存储 or HTTP 服务器 → 4-6 周从零实现
    ↓
redis-labs/redisraft raft.c → 3-4 周精读（分布式闭环）
```

**参考**：分布式系统主线（Raft / Paxos / etcd / ZooKeeper）学习见 `tmp-question/分布式/` 目录。

**C++ 进阶**：C++ 学习路线见 `tmp-question/语言/cpp/` 目录（待梳理）。
