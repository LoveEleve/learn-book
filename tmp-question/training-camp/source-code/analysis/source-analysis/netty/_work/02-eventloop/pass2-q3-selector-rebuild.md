## Loop Note: Q3 — Selector 空轮询检测与自动重建

**Hypothesis**: NioIoHandler.select() 使用 selectCnt 计数器 + time-elapsed 双重检查来区分正常的 select 超时和 JDK epoll bug 的假唤醒。假唤醒累计 >= 512 次时重建 Selector。

**Verification** (`NioIoHandler.java:630-709`):
- `selectCnt` 从 0 开始 (`line 633`)
- 每次 `selector.select(timeoutMillis)` → `selectCnt++` (`line 669`)
- select 返回后，检查 4 个条件决定 break 还是继续 (`line 671`):
  1. `selectedKeys != 0` → 有真实 I/O 事件 → break (正常)
  2. `oldWakenUp` → 用户调用了 wakeup → break
  3. `wakenUp.get()` → wakeup 被调用 → break
  4. `!runner.canBlock()` → 有积压任务 → break
- 如果 4 个条件全部 false → select 假唤醒（epoll bug）
- **time-elapsed 检查** (`lines 693-704`):
  ```java
  if (time - msToNanos(timeoutMillis) >= currentTimeNanos) {
      selectCnt = 1;  // 正常超时——select 真的等了超时时间
  } else if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
             selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
      selector = selectRebuildSelector(selectCnt);  // epoll bug——假唤醒累积
      selectCnt = 1;
      break;
  }
  ```
- `SELECTOR_AUTO_REBUILD_THRESHOLD = 512` (`line 83`)，可通过 `io.netty.selectorAutoRebuildThreshold` 覆盖
- `selectRebuildSelector()` (`line 255`) → 旧 selector 上的所有 key 迁移到新 selector → 关闭旧 selector
- `MIN_PREMATURE_SELECTOR_RETURNS = 3` (`line 66`) — 假唤醒超过 3 次记录日志警告

**Code type**: Algorithmic (error detection + self-healing)

**历史背景**: JDK 的 epoll wrapper 在某些 Linux 内核版本上存在 bug（JDK-6427854, JDK-6527572, Netty #203）——`Selector.select(timeout)` 在 epoll_wait 返回 0（事件到达但在应用层唤醒和 epoll_wait 之间被处理了）时，JDK 不分青红皂白地返回 0 并继续循环——造成 CPU 100% 自旋。

**关键设计细节**: selectCnt=1 不是 selectCnt=0。当 select 正常超时时设为 1（不是 0），因为 1 是一个"一次正常 select 刚刚完成"的标记。0 是"尚未执行任何 select"的初始状态。

**Conclusion**: 空轮询检测 = selectCnt 累加假唤醒 + 时间差验证正常超时 + 512 次触发重建 + 3 次触发警告。Netty 不只检测 epoll bug——它通过 Selector 重建来自动修复，避免了 JDK 升级前生产环境的 CPU 100% 崩溃。source: NioIoHandler.java:66-92,255,630-709
