# J-6 review-notes — 六层深审记录 (2026-08-15)

## 审法: 锚点回源核对 + 极简复现 harness (MiniTimeWheel)

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "三态 workerState INIT/STARTED/SHUTDOWN" — HashedWheelTimer.java:66-70 实证 | 通过 ✅ |
| 2 | 事实 | §1 "wheel 桶数组 + mask + tickDuration" — L72-76 | 通过 ✅ |
| 3 | 事实 | §1 "论文引用" — L38-41 Varghese & Lauck | 通过 ✅ |
| 4 | 事实 | §2 "run() 自动重排" — RepeatedTimer.java:83-107 (timeout=null + schedule) | 通过 ✅ |
| 5 | 事实 | §2 "adjustTimeout 每次调度前" — L187 | 通过 ✅ |
| 6 | 事实 | §3 "三态 CAS + execute 三步" — MpscSingleThreadExecutor.java:46-48, L137-141 | 通过 ✅ |
| 7 | 事实 | §3 "shutdownHooks 经同一队列" — L149 | 通过 ✅ |
| 8 | 事实 | §4 "RwLock 子类 + 检测包装" — LongHeldDetectingReadWriteLock.java:69, L91 | 通过 ✅ |
| 9 | 事实 | §5 "ExecutorChooser 轮询" — DefaultFixedThreadsExecutorGroup.java:34-44 | 通过 ✅ |
| 10 | 事实 | §6 "SEGMENT_SIZE=128" — SegmentList.java:52 (SEGMENT_SHIFT=7, 2<<6=128) | 通过 ✅ |
| 11 | 事实 | §6 "firstOffset 首段缓存偏移" — L59 | 通过 ✅ |
| 12 | 事实 | §6 "estimatedBytes + Recyclers" — L63, L85 (16_382/SEGMENT_SIZE) | 通过 ✅ |
| 13 | 事实 | §6 "removeFromFirstWhen/LastWhen" — L187/L205 | 通过 ✅ |
| 14 | 事实 | §7 "timerPoolSize cpus*3 上限 20" — NodeOptions.java:131 | 通过 ✅ |
| 15 | 数字 | 锚点密度: J-6 outline 引用 .java:NNN ≥8 | 通过 ✅ (统计见下) |
| 16 | 结构 | 负面空间 "不做分层时间轮" 与论文 hierarchical 对照自洽 | 通过 ✅ |
| 17 | 过程 | **REVIEW 扩域自省**: 本域因 00 §3 定量预检补跑而新增 — 教训: ≥50 文件包不得凭"工具层"直觉排除 | 通过 ✅ (PLAN 审计表已记录) |

**结论**: 17 项核对 0 修正。harness 验证时间轮调度语义。

## harness 设计 (MiniTimeWheel — 时间轮极简复现)

- A. 桶定位: tick & mask 取模正确性 (2 的幂轮)
- B. 到期精度: 任务在 [due, due+tickDuration) 内触发
- C. 重复调度: RepeatedTimer 触发后自动重排
- D. 随机化窗口: adjustTimeout 在 [t, t+delay) 内变化
- E. 绕轮: 超长延迟任务跨多圈仍正确到期
