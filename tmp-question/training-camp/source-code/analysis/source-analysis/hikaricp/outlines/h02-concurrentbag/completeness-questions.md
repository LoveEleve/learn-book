# H-2 ConcurrentBag 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 连接存在哪些结构里? | §1 (sharedList+threadLocal+handoffQueue) |
| 2 | 连接条目有哪些状态? | §1 (NOT_IN_USE/IN_USE/REMOVED/RESERVED) |
| 3 | borrow 为什么快? | §2 (thread-local 优先 CAS) |
| 4 | 归还给谁? | §3 (先等待者, 否则本线程) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 CAS 而非锁? | §1 (无锁无阻塞) |
| 6 | 为什么三层查找? | §2 (快→兜底→直传) |
| 7 | 为什么归还先 handoff? | §3 (直传给等待者免中间步) |
| 8 | 为什么 FastList? | §3 (removeLast O(1) 微优化) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | ConcurrentBag 是什么? | §1 (无锁并发容器) |
| 10 | 线程亲和什么意思? | §3 (连接回本线程, 零竞争) |
| 11 | 状态怎么切换? | §1 (CAS via AtomicIntegerFieldUpdater) |

## 覆盖: 11 问 / 3 身份 / 100%
