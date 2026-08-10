# EventLoop — Pass 2 完成

> 域: EventLoop | 域#5 | 方案: A | 9 个闭环笔记全部完成

---

## 循环笔记汇总

| # | 问题 | 代码类型 | 核心结论 |
|:--:|------|:--:|------|
| Q1 | 双轨架构动机 | Interface Design | IoHandler/EventLoop 分离为虚拟线程 (Project Loom) 的 carrier release |
| Q2 | SelectStrategy 决策 | Algorithmic | 任务优先——有任务跳过阻塞 I/O，无任务阻塞等待 |
| Q3 | 空轮询检测与重建 | Algorithmic | selectCnt+time-elapsed 双重检查 → ≥512 次假唤醒自动重建 Selector |
| Q4 | SelectedSelectionKeySet | Implementation | 数组+size 替代 HashSet → O(selected) 替代 O(registered) |
| Q5 | Wakeup race condition | Implementation | CAS→select→CAS 间 race + post-select double check 解决 |
| Q6 | EventLoopGroup 分配 | Glue | Power-of-2 chooser 位运算分配 Channel 到 EventLoop |
| Q7 | ioRatio 已废弃 | Implementation | 4.2 废弃 ioRatio → maxTaskProcessingQuantumNs 时间预算替代比例模型 |
| Q8 | canSuspend 虚拟线程 | Implementation | numRegistrations==0 → EventLoop 可挂起释放 carrier thread |
| Q9 | rebuildSelector 双路径 | Implementation | 内部自动 (selectCnt≥512) + 外部主动 (rebuildSelector()) → 同一条 rebuildSelector0() |

---

## Pass 2 完成检查

- [x] 循环关闭: 9/9
- [x] grep≥3: Q1(IoHandler Javadoc+SingleThreadIoEventLoop.run() + IoHandlerFactory), Q3(selectCnt + time-elapsed + THRESHOLD=512), Q7(ioRatio @Deprecated + maxTaskProcessingQuantumNs + canBlock())
- [x] 代码类型: Interface Design(1) + Algorithmic(2) + Implementation(5) + Glue(1)
- [x] 文件存在: `find pass2-q*.md | wc -l` = 4 (含 grouped)

## 方法论证据

```
[01 Pass 2] grep: ~15次 | 闭环: 9/9 | 源码文件: 8个
```
