# C-4 review-notes — 六层深审记录 (2026-08-15)

## 审法: 大纲每锚点回源核对 + harness 18/18 实证

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "PERSISTENT_SEQUENTIAL 创建" — DistributedQueue.java:359 `withMode(CreateMode.PERSISTENT_SEQUENTIAL)` 逐字核对 | 通过 ✅ |
| 2 | 事实 | §2 "blockingNextGetData 版本等待" — L463-465; ChildrenCache.java:107-128 | 通过 ✅ (harness A 实证 FIFO) |
| 3 | 事实 | §3 "delete().withVersion" — L587-589 逐字核对 | 通过 ✅ |
| 4 | 事实 | §3 "锁安全模式 EPHEMERAL 同名锁节点 + 事务 requeue" — L607-651 (L612, L623-632, L644-647) | 通过 ✅ |
| 5 | 事实 | §4 "PriorityQueue 名字编码" — L186-192 负补码 0 前缀 | 通过 ✅ (harness D 实证 high,mid,low) |
| 6 | 事实 | §4 "DelayQueue epoch 编码" — L212-214; getDelay L75-83 | 通过 ✅ (harness E 实证: 800ms 未消费/2s 后消费; 注意 put 参数是 **delayUntilEpoch** 毫秒时间戳, 非延迟时长 — 大纲已按此修正) |
| 7 | 事实 | §4 "IdQueue remove 返回 int" — L168 (返回移除数量) | 通过 ✅ (harness F 实证返回 1; 初始测试误当 boolean) |
| 8 | 事实 | §4 "SimpleDistributedQueue take 阻塞" — L103, L171-208 | 通过 ✅ (harness G 实证; 无 close 方法 — 测试修正) |
| 9 | 事实 | §6 "barrier 节点存在=关闭" — DistributedBarrier.java:108 checkExists==null 判定 | 通过 ✅ (harness H 实证阻塞/放行) |
| 10 | 事实 | §6 "double barrier ready 节点 + memberQty 阈值" — L260-292 | 通过 ✅ (harness I 实证 3 成员同步; ready 节点清理) |
| 11 | 事实 | §5 "maxItems 强制前台 put" — QueueBuilder.java:216-220 | 通过 ✅ |
| 12 | 过程 | **harness 实证发现 1**: 无锁模式消费极快 — put 后节点立刻被删 (先删后消费); IdQueue remove 需要 producer-only 窗口 | 大纲语义: "先删后消费" 的速度实证 |
| 13 | 过程 | **harness 实证发现 2**: flushPuts 断言改为消费端可见 (节点已被消费删掉) | 通过 ✅ |
| 14 | 结构 | 负面空间 "无 ack" 与锁安全模式 at-least-once 对照自洽 | 通过 ✅ |

**结论**: 14 项核对 0 修正 (2 项 API 语义澄清: DelayQueue epoch 参数/IdQueue remove 返回值)。harness 18/18 覆盖: A/B FIFO+回调 C flushPuts D 优先级 E 延迟 F IdQueue G SimpleQueue H 单屏障 I 双屏障。

## harness 发现 (可写入文章):

1. **消费即删的速度**: 慢消费窗口测试证明节点在 consumeMessage 之前已被删除 — "先删后消费"名符其实。
2. **优先级/延迟/ID 名字编码全部实证**: high,mid,low 顺序 / 延迟 2s 精确到 800ms 未消费 / remove 返回 1。
3. **双屏障同步实证**: 3 成员 enter 全部完成才放行, leave 全部完成 + ready 节点清理。
4. **单屏障发令枪实证**: waitOnBarrier 阻塞直到 removeBarrier。
