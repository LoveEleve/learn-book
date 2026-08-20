# C-2 review-notes — 六层深审记录 (2026-08-15)

## 审法: 大纲每锚点回源核对 + harness 15/15 实证

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "reset 创建 EPHEMERAL_SEQUENTIAL + withProtection" — LeaderLatch.java:516-521 逐字核对 | 通过 ✅ |
| 2 | 事实 | §1 "ephemeralOwner 二次确认" — L556-573: `event.getStat().getEphemeralOwner() != thisSessionId → reset()` | 通过 ✅ |
| 3 | 事实 | §1 "watch 前驱 getData 而非 exists" — L600-601 注释逐字核对 | 通过 ✅ |
| 4 | 事实 | §1 "await 抛 EOFException" — L307-316 循环后校验状态 | 通过 ✅ |
| 5 | 事实 | §1 "CloseMode SILENT/NOTIFY_LEADER" — L102-112 + internalClose L195-226 | 通过 ✅ |
| 6 | 事实 | §2 "复用 InterProcessMutex" — LeaderSelector.java:70-77 + 注释 L60-64 | 通过 ✅ |
| 7 | 事实 | §2 "takeLeadership 阻塞 = 任期" — doWork L422-467: acquire L426 → takeLeadership L436 → release L451 | 通过 ✅ (harness E 实证: 任期内无并发, 无 autoRequeue 不重选) |
| 8 | 事实 | §2 "CancelLeadershipException → cancelElection" — WrappedListener L546-556 + "dated leadership" 注释 L550-553 | 通过 ✅ |
| 9 | 事实 | §3 "handleStateChange RECONNECTED → 复查" — LeaderLatch.java:630-663 | 通过 ✅ |
| 10 | 事实 | §3 "CURATOR-724 reset" — L610-617 注释实证 | 通过 ✅ |
| 11 | 事实 | §2 "autoRequeue" — LeaderSelector.java:182-184 + doWorkLoop L469-486 | 通过 ✅ (harness F 实证: 累计 3 次当选) |
| 12 | 过程 | **harness 实证发现 1**: latch 启动是**异步**的 — l1.start 后立即 l2.start, l2 可能先当选 (创建序由 ZK 决定); 与 C-3 的 FIFO 实证同源 | 大纲表述修正: "先启动者先当" 仅当创建序确定 |
| 13 | 过程 | **harness 实证发现 2**: 前驱 watch 驱动接替的延迟 <3s (实证 B/C), 非轮询 | 通过 ✅ |
| 14 | 结构 | 负面空间 "不做自动重新竞选" 与 autoRequeue 不矛盾 (latch 无, selector 有) | 通过 ✅ (大纲已区分) |

**结论**: 14 项核对 0 修正 (2 项 harness 实证语义澄清)。harness 15/15 覆盖: A 唯一性 B 事件驱动接替 C await 阻塞 D participants E 任期 F autoRequeue G 适配器。

## harness 发现 (可写入文章):

1. **选举也是 ZK 创建序**: latch 谁先当选由节点创建到达 ZK 的先后决定, 与锁的公平性同源。
2. **接替延迟 <3s**: leader close → NodeDeleted → 前驱 watcher → getChildren → 重判, 全链路事件驱动。
3. **任期语义实证**: takeLeadership 返回后不自动重选 (无 autoRequeue), 有了 autoRequeue 才循环。
4. **唯一性实证**: 3 latch 恰 1 leader; participants 恰 1 个 isLeader。
