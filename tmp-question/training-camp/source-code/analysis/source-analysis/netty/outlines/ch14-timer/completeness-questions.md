# Ch14 HashedWheelTimer 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `tick & mask` 如何做到 O(1) bucket 定位? mask 为什么取 `wheel.length-1`? | §1 |
| 2 | `remainingRounds` 的作用是什么? 如果 deadline 超出 51.2 秒(512×100ms)会发生什么? | §2 |
| 3 | `expireTimeouts` 遍历时如何避免死循环? 链表节点移除是安全的吗? | §2 |
| 4 | `waitForNextTick()` 用绝对时间而非相对时间——两者差异在哪? 相对时间会累积什么误差? | §3 |
| 5 | `Timeout.cancel()` 为什么不直接修改 bucket 链表而是走 `cancelledTimeouts` 队列? | §4 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 时间轮的 tickDuration=100ms——为什么 IO 超时 30 秒不需要毫秒级精度? 什么场景需要更高精度? | §1 |
| 7 | Timeout 自身作为链表节点(prev/next 字段)——相比 `LinkedList<Node>` 的 GC 优势有多大? | §2 |
| 8 | Worker 三态(INIT/STARTED/SHUTDOWN)用 CAS 切换——不加锁的设计意图是什么? `start()` 反复调用怎么处理? | §3 |
| 9 | `INSTANCE_COUNT_LIMIT=64` 是计数的还是警告的? 为什么 64 个实例就提醒? 多实例 vs 单例共享的权衡? | §1 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 10 | `ScheduledThreadPoolExecutor` 用 PriorityQueue 插入 O(log N)——10000 个定时器多大开销? 时间轮怎么降到 O(1)? | §1 |
| 11 | 时间轮的"轮"是什么概念? 512 个 bucket 每个 tick 往前走一格——走完一圈后 bucket 里的 timer 还没到期怎么办? | §2 |
| 12 | `newTimeout()` 提交的 timer 什么时候执行? 不是立即执行的话——流程指向哪里? | §1 |

## 覆盖: 12 问 / 3 身份 / 100%
