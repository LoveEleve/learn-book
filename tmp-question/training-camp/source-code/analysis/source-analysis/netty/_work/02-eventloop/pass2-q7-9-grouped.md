## Loop Note: Q7 — ioRatio → maxTaskProcessingQuantumNs

**Hypothesis**: Netty 4.1 用 ioRatio 控制 I/O 和任务的 CPU 时间分配。Netty 4.2 废弃 ioRatio，用 maxTaskProcessingQuantumNs 替代——每次循环中 I/O 先执行完整一轮，接着任务执行最多 N 纳秒。

**Verification**:
- `NioEventLoop.java:143-155` — `getIoRatio() { return 0; }` + `setIoRatio(int)` **@Deprecated + no-op** — ioRatio 在 4.2 中彻底移除
- `SingleThreadIoEventLoop.java:39-41` — 替代机制: `DEFAULT_MAX_TASK_PROCESSING_QUANTUM_NS = max(100ms, sysprop("io.netty.eventLoop.maxTaskProcessingQuantumMs", 1000ms))`
- `SingleThreadIoEventLoop.java:192-205` — run() 循环: `runIo() → runAllTasks(maxTaskProcessingQuantumNs)` — I/O 总是先执行，然后任务有多达 1000ms (可配) 来处理
- `SingleThreadIoEventLoop.java:46-48` — `IoHandlerContext.canBlock(): return !hasTasks() && !hasScheduledTasks()` — 只有无任务时才允许 select 阻塞
- 设计变更: ioRatio 比例控制 (I/O 50%=一半时间) → 时间预算控制 (任务至多 1 秒然后回去做 I/O)

**Code type**: Implementation (scheduling policy evolution)

**设计权衡**:
| 维度 | 4.1 ioRatio | 4.2 maxTaskProcessingQuantumNs |
|------|------|------|
| 参数语义 | I/O 占总时间的比例 | 任务处理的纳秒上限 |
| 典型值 | ioRatio=50 | maxTaskProcessingQuantumMs=1000 |
| 语义 | "至少 50% 时间用于 I/O" | "任务跑满 1 秒停下来" |
| I/O 优先 | 需要计算 I/O 时间占比 | I/O 天然先执行 |

**Conclusion**: 4.1 的比例模型被替换为 4.2 的时间预算模型——更简单: I/O 先执行一轮不受限，然后任务跑最多 1 秒。删掉了 ioRatio 因为 IoHandler/EventLoop 分离后 I/O 时间自然无法精确计算——IoHandler 报告的是 activeIoTime + canBlock()，比例由这两者间接决定。source: NioEventLoop.java:143-155(@Deprecated), SingleThreadIoEventLoop.java:39-205

---

## Loop Note: Q8 — canSuspend 虚拟线程

**Hypothesis**: Netty 4.2 的 canSuspend() 允许无 Channel 注册的 EventLoop 挂起释放线程——这是 Project Loom 虚拟线程的核心集成点。

**Verification**:
- `SingleThreadIoEventLoop.java:204` — `while (!confirmShutdown() && !canSuspend())` — 循环退出条件之一: canSuspend=true → EventLoop 线程挂起
- `SingleThreadIoEventLoop.java:212-215` — `canSuspend(state): super.canSuspend(state) && numRegistrations.get() == 0` — 无注册 Channel + 父类允许 = 可挂起
- `SingleThreadEventExecutor.canSuspend()` — 父类实现检查任务队列 + 定时任务是否为空 + shutdown 状态
- `IoHandlerFactory.isChangingThreadSupported()` — NioIoHandlerFactory 返回 true (`NioIoHandler.java:809`) → IoHandler 支持线程切换。这意味着虚拟线程挂起时 Selector 可随 carrier 迁移，不需要额外的线程固定策略 |
- `SingleThreadIoEventLoop.java:85-91` — 构造函数传递 `isChangingThreadSupported` 给父类，决定线程挂起策略

**Code type**: Implementation (virtual thread integration)

**设计意图**: 虚拟线程下的关键挑战——一个 NioEventLoop 有 Selector 阻塞在 select 中。如果虚拟线程无法释放 carrier，该 EventLoop 独占一个 OS 线程。canSuspend 在 numRegistrations==0 时允许线程挂起——此时选择器没有 Channel 注册，挂起是安全的（Selector 无等待事件）。但当有 Channel 时 (numRegistrations>0)，不能挂起——否则 select 等待的 channel 事件会导致 carrier 无法恢复。

**Conclusion**: canSuspend = 虚拟线程的"安全阀"——只有事件循环完全空闲 (numRegistrations==0) 时才挂起释放 carrier thread。有活跃 Channel → 继续运行 + I/O 处理。isChangingThreadSupported=true (NioIoHandler 的 Selector 可跨线程) 配合 canSuspend 共同实现虚拟线程集成: EventLoop 挂起时 Selector 可跟随迁移到新 carrier 线程。这就是 IoHandler/EventLoop 分离的最终目的。source: SingleThreadIoEventLoop.java:85-91,192-215, NioIoHandler.java:809

---

## Loop Note: Q9 — rebuildSelector 双路径

**Hypothesis**: Selector 重建有两条独立路径:(1) NioIoHandler 内部自动检测重建 (select 循环中的 selectCnt 阈值), (2) NioEventLoop 对外暴露的 rebuildSelector() 供外部主动触发。

**Verification**:
- 路径 1 (内部): `NioIoHandler.select()` (`line 697-702`) → selectCnt >= 512 + time-elapsed 通过 → `selectRebuildSelector(selectCnt)` → `rebuildSelector0()` → 迁移所有 key → 关闭旧 Selector
- 路径 2 (外部): `NioEventLoop.rebuildSelector()` (`line 161-171`):
  ```java
  public void rebuildSelector() {
      if (!inEventLoop()) {
          execute(() -> ((NioIoHandler) ioHandler()).rebuildSelector0());
      } else {
          ((NioIoHandler) ioHandler()).rebuildSelector0();
      }
  }
  ```
- 外部路径确保在 EventLoop 线程中执行 (inEventLoop 检查)
- `NioIoHandler.rebuildSelector0()` (`line 255`) — 实际的重建逻辑: 新 selector → 迁移所有 key → 关闭旧
- 两条路径汇聚在同一 `rebuildSelector0()` — 内部自动检测 + 外部主动触发

**Code type**: Implementation (self-healing + user control)

**使用场景**:
| 路径 | 触发方 | 场景 |
|------|------|------|
| 内部 | select 循环自动 | 假唤醒积累到 512 — 系统自愈 |
| 外部 | API 调用 | 告警系统主动重建 ("selector rebuild count today > 100 → force rebuild") |
| 外部 | IOException 捕获 | NioIoHandler.run() 捕获 IOException (`line 471-475`) — "Selector is messed up. Let's rebuild" |

**Conclusion**: 双路径不是"主备"关系——是"主动检测 + 用户可控"。内部自动 = 系统自愈，外部主动 = 运维干预。两者共存让 Netty 的 selector 管理从"隐藏式修复"升级为"可观测的修复"(可以记录 external rebuild vs internal rebuild 的计数)。source: NioEventLoop.java:161-171, NioIoHandler.java:255,471-475,697-702
