# §3.3 Selector 的工程陷阱 — 文章大纲

## 读者基线

已从 §3.1 理解 Selector 模型、从 §3.2 写出完整的单线程 select 循环。现在需要理解**写出能用的 select 循环后，JDK Selector 本身还有哪些让你崩溃的陷阱**。

## Pass 0: 设计上下文

- JDK 的 `EPollSelectorImpl` 是 NIO Selector 在 Linux 上的实现——底层委托给 epoll
- epoll 有一个著名 bug：即使没有任何 Channel 就绪，`epoll_wait` 也可能立即返回——这就是"空轮询 bug"
- Netty 4.2 在 `NioIoHandler` 中有专门的空轮询检测和重建 Selector 的逻辑——这些工程化处理是 Netty"比裸 NIO 好用"的关键原因
- `selectedKeys` 的底层实现是 Selector 内部的一个 Set——`select()` 返回后它包含就绪的 key，但**不会自动清除**

## Pass 1: 扫描结果

- 空轮询: `epoll_wait` 返回 0（无就绪事件）但 CPU 被唤醒 → 无限循环 → CPU 100%
- selectedKeys 陷阱: 每次 select 后 Set 不清空 → 旧事件堆积 → 重复处理
- 中断: `Thread.interrupt()` 让 select 返回 → 不确定是事件就绪还是被中断
- 标记问题 ≥5:
  1. 空轮询 bug 的根本原因是什么？为什么 epoll_wait 会返回 0？
  2. Netty 是怎么检测空轮询的？检测到后怎么处理？
  3. `selectedKeys` 为什么 JDK 不帮你自动清？设计意图是什么？
  4. select 返回后怎么区分"有事件"和"被 interrupt 了"？
  5. `select(long timeout)` 和 `select()` 有什么陷阱？timeout 设置多少合适？

## 概念依赖链

```
§3.2 的 select 循环 → 空轮询 bug(epoll_wait false wakeup) → selectedKeys 陷阱 → 中断处理 → Netty 的解决方案
```

## 叙事顺序

**开篇场景：select 循环写好了，上线后 CPU 100%**

§3.2 写好的 select 循环在本地测试完美——连接少、事件规律。部署到生产环境后偶尔出现 CPU 飙升到 100%。查代码——所有线程都在 `selector.select()` 之后执行——但 `select()` 本应该是在内核中休眠的。为什么被唤醒了？

```java
while (true) {
    int readyChannels = selector.select();  // 预期: 阻塞。实际: 偶尔空返回
    // readyChannels = 0 — 但 select() 没有阻塞！
    // 下一轮循环立即再执行 select() → 又空返回 → 死循环 → CPU 100%
}
```

**第一层：空轮询 bug — epoll 的 false wakeup**

根本原因：在 Linux 上，JDK 的 `EPollSelectorImpl` 调用 `epoll_wait(epfd, events, timeout)`。正常情况下 epoll_wait 在无事件时阻塞线程。但在某些内核版本（2.6.x~某些 3.x），存在一个 bug：即使没有任何 Channel 就绪，`epoll_wait` 偶尔会返回 0——看起来像是"有事件被处理了但其实是空返回"。

```
正常的 select() → epoll_wait → 内核休眠 → 有数据 → 唤醒 → 返回 N > 0
空轮询的 select() → epoll_wait → 内核休眠 → ??? → 唤醒 → 返回 0 → 无人处理 → 再来一轮
```

这个 bug 导致 JVM 进程的 CPU 被一个空 select 循环吃掉。只影响 Linux 上的 epoll 实现——其他平台的 Selector 不会触发。

**第二层：Netty 的空轮询检测与修复**

Netty 4.2 在 `NioIoHandler`（`NioIoHandler.java:667-704`）中实现空轮询检测。思想分三步：

1. 记录每次 `select()` 的耗时，区分**正常超时**和**假唤醒**——如果 select 返回 0 但时间差不多等于 timeout 时长，重置计数（正常情况，不是 bug）
2. 如果 select 返回 0 但时间明显短于 timeout（提前返回）→ 累积 `selectCnt++`
3. `selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD`（默认 512）→ **销毁当前 Selector，创建新 Selector** → 全部 Channel 重新注册 → `selectCnt = 1`（新 Selector 起始值）

检测逻辑的实际源码（`NioIoHandler.java:667-704`）：

```java
int selectedKeys = selector.select(timeoutMillis);
selectCnt++;

if (selectedKeys != 0 || oldWakenUp || wakenUp.get() || !runner.canBlock()) {
    break;  // 有真实事件或用户唤醒——正常
}
// selectedKeys = 0 —— 需要区分: 正常超时 vs 假唤醒?
long time = System.nanoTime();
if (time - timeoutMillis_ns >= currentTimeNanos) {
    selectCnt = 1;  // 正常超时——时间过完了, 不是 bug
} else if (selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
    selector = selectRebuildSelector(selectCnt);  // 假唤醒累计超阈值 → 重建
    selectCnt = 1;                                 // 新 Selector, 重置为 1
}
```

关键设计点：不是每次 select 返回 0 就判定为 bug——只累计"时间没到就返回"的提前返回。正常超时返回不会累积计数，避免了误判。

这个逻辑不是"修复 epoll bug"（JDK 自己修不了内核），而是"绕过它"——通过重建 Selector（new epoll fd）来重置 epoll 内核状态。

**第三层：selectedKeys 的陷阱 — 为什么必须手动清**

`Selector.selectedKeys()` 返回的 Set **不是 snapshot**——它是 Selector 内部集合的**直接引用**：

```java
// JDK 的默认实现：selectedKeys 就是 Selector 内部的 Set 本身
// 不是复制品！
Set<SelectionKey> keys = selector.selectedKeys();
// keys 和 Selector 内部的 selectedKeys 是同一个对象
```

这意味着：
- `select()` 把新就绪的 key **添加**到 selectedKeys
- 如果你上一轮没清理，新就绪的 key 会**追加**到旧 key 后面
- 1000 轮不清理 = selectedKeys 包含全部历史 key → O(n) 遍历

JDK 没有自动清理的设计意图是"给你自己管理 event 处理进度的能力"——但几乎所有 NIO 代码都遵循同样的模式：**处理完一轮就全部清理**。

**第四层：select 中断 — 阻塞返回 ≠ 事件就绪**

`select()` 的返回可能是真的"有 Channel 就绪"，也可能只是"当前线程被 interrupt 了"。如果忽略中断信号，下一次 select 可能永远不再阻塞——因为线程的中断标志已被设置：

```java
while (true) {
    selector.select();  // 如果当前线程已被 interrupt，立即返回
    // readyChannels = 0，继续下一轮 → 空轮询
}
```

正确处理：select 返回后检查 `Thread.interrupted()`——如果是中断，选择退出循环或重新设置中断标志。但 select 的核心阻塞语义已经是"在 select 返回后和下一轮调用前检查"。

**结尾悬念**

JDK NIO 的旅程到这里结束了。你学会了 ByteBuffer、Channel、Selector——以及它们的真实陷阱。从下一章开始，将进入 Netty 的世界。Netty 的 ByteBuf、EventLoop、Pipeline——它们本质上是用更好的 API 封装了你在 NIO 三章中学到的所有基础，然后解决了 JDK 遗留的空轮询 bug、selectedKeys 陷阱和线程安全问题。NIO 是 Netty 的地基——现在地基已经打好了。

## 核心悬念

**读完能回答**：epoll 空轮询 bug 的触发条件是什么？Netty 怎么检测和绕过它？为什么 selectedKeys 不会自动清理——JDK 的设计意图是什么？select 被线程中断后应该怎么处理？

## 文章边界

- **在本篇内**: 空轮询 bug → Netty 的 SELECTOR_AUTO_REBUILD → selectedKeys 内部引用陷阱 → select 中断处理 → NIO 终结 → 引出 Netty
- **不属于本篇**: Netty NioEventLoop 完整实现（Ch5）、Netty ByteBuf（Ch4）
