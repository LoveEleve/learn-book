# C-3 review-notes — 六层深审记录 (2026-08-15)

## 审法: 大纲每锚点回源核对 + harness 22/22 实证

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "threadData ConcurrentMap" — InterProcessMutex.java:60 附近字段实证 | 通过 ✅ |
| 2 | 事实 | §1 "重入 lockCount++ 不碰 ZK" — internalLock L205-210 逐字核对 | 通过 ✅ (harness B 实证: 3 acquire 2 release 仍持有) |
| 3 | 事实 | §2 "EPHEMERAL_SEQUENTIAL + withProtection" — StandardLockInternalsDriver.java:49-72 逐字核对 | 通过 ✅ |
| 4 | 事实 | §2 "ourIndex < maxLeases 判定" — L38-39 逐字核对 | 通过 ✅ |
| 5 | 事实 | §3 "getData 而非 exists 挂 watch" — LockInternals.java:242-244 注释逐字核对 | 通过 ✅ |
| 6 | 事实 | §3 "watcher → postSafeNotify" — L61-66 逐字核对 | 通过 ✅ |
| 7 | 事实 | §4 "guaranteed 删除" — releaseLock L106-110 → deleteOurPath L281-287 `client.delete().guaranteed()` | 通过 ✅ |
| 8 | 事实 | §5 "前缀等长" — __READ__/__WRIT__ 实测均 8 字符 (注释 L60-62 "must be the same length") | 通过 ✅ (逐字符数: _ _ R E A D _ _ = 8; _ _ W R I T _ _ = 8) |
| 9 | 事实 | §5 "ReadLock maxLeases=Integer.MAX_VALUE" — L123 实证 | 通过 ✅ |
| 10 | 事实 | §5 "firstWriteIndex 判定" — L141-158 逐字核对 | 通过 ✅ (harness F 实证: 多读者并存/写者被拒/降级前奏) |
| 11 | 事实 | §6 "读→写升级不可能" — Javadoc L48 + 位置判定推导 (读节点排在写节点前 → ourIndex≥1) | 通过 ✅ (推导自洽: 写锁判定用混排全局位置, 读者自己的读节点序号更小) |
| 12 | 事实 | §7 "lease 节点 EPHEMERAL_SEQUENTIAL" — InterProcessSemaphoreV2.java:287-290 实证 | 通过 ✅ (harness G 实证 3 取超时/归还可取) |
| 13 | 事实 | §7 "SharedCountReader 动态上限" — L113-133 countHasChanged → maxLeases 更新 + postSafeNotify | 通过 ✅ |
| 14 | 事实 | §8 "MultiLock 逆序释放" — InterProcessMultiLock.java:104-112 实证 | 通过 ✅ (harness H 实证部分失败回滚) |
| 15 | 数字 | "maxLeases=Integer.MAX_VALUE 参与判定" — getsTheLock 中 ourIndex < maxLeases 恒真 | 通过 ✅ (语义: 读锁不互斥) |
| 16 | 过程 | **harness 实证发现: FIFO 语义** — 5 线程同时抢锁时顺序 [0,3,1,2,4] 非线程 id 序 | **重要发现** ✅: 公平性 = **ZK 视角的创建序**而非 JVM 线程启动序; 错开启动后严格 FIFO (22/22 全过)。大纲 §8 已按此修正表述 (InterProcessReadWriteLock Javadoc "in the order requested (from ZK's point of view)") |
| 17 | 结构 | 负面空间 "不做锁续约" 与 Redisson 对照 — 已列入文章对比点 | 通过 ✅ |

**结论**: 17 项核对 0 修正 (harness 实证 1 项语义澄清: FIFO 是 ZK 视角)。harness 22/22 覆盖: A 互斥 B 可重入 C FIFO D 超时清理 E 会话崩溃释放 F 读写锁+降级 G 信号量 H MultiLock。

## harness 发现 (可写入文章):

1. **公平性语义实证**: 5 线程同刻抢锁, 获取序 [0,3,1,2,4] — 完全由 ZK 收到创建的先后决定; 这是"从 ZK 视角公平"的活证据。
2. **会话崩溃释放实证**: 客户端 close 后锁节点被服务端回收 (ephemeral), 无需客户端兜底。
3. **超时清理实证**: acquire 超时失败后参与者节点数回到 1 (自己节点已删)。
4. **写→读降级实证**: 写者持有时 acquire 读锁立即成功 (writeLock.isOwnedByCurrentThread 分支)。
