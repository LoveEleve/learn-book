# K-4 Partition & ISR 篇 3/4 — 同步的纪律: 两阶段拉取与 epoch 截断

> 前置: [[K-4-isr-02]] (状态机) [[K-3-log-03]] (崩溃恢复) | 复用: — | 对照: [[rd2-rlock]] (fencing token) [[E-6-seqno]] (term 语义) | 引出: [[K-4-isr-04]]
> 🔴 A | 来源: AbstractFetcherThread.scala:58,115-182,211-232,276-315,318-373,604-655
> 定位: K-4 卷中篇 — 回答"follower 怎么同步? 截断规则是什么? 被 fence 怎么办?"

**读者处境**: 面试官问 "follower 怎么从 leader 同步? 日志不一致怎么办?" 你答 "拉取" — 但再问 "为什么先截断再拉? epoch 截断 4 规则是什么? FENCED_LEADER_EPOCH 什么意思?" 你答不上来。这篇是副本同步协议的完整答案。

### 1. 问题引入 — 先回退, 后前进

场景: follower 的日志可能比 leader 长 (旧 leader 写了多余数据) — 直接拉新数据会叠在错位日志上。怎么办?
- doWork 两阶段 (AbstractFetcherThread.scala:115-118): **maybeTruncate() → maybeFetch()**
- 本篇问题: 两阶段 (Q4) / 4 规则 (Q5) / fencing (Q6)

### 2. 两阶段拉取 — doWork 的纪律

场景: 每轮 doWork 做什么?
- maybeTruncate (AbstractFetcherThread.scala:174-182): 有 epoch 的分区 → truncateToEpochEndOffsets (AbstractFetcherThread.scala:L177); 无 epoch → truncateToHighWatermark (AbstractFetcherThread.scala:L180) — 双路径
- 截断锁: partitionMapLock 内 (AbstractFetcherThread.scala:L215) + epoch 校验 (AbstractFetcherThread.scala:L224-225) — "Ensure we hold a lock during truncation"
- maybeFetch (AbstractFetcherThread.scala:L120): buildFetch → 发送 → processFetchRequest (AbstractFetcherThread.scala:L318) → processPartitionData → appendAsFollower (K-3)
- 迭代截断: epoch 未知时 truncationCompleted=false → 再问 leader (AbstractFetcherThread.scala:L638)

### 3. epoch 截断 4 规则 — 回退到安全点

场景: follower 问 leader "我的 epoch N 该截到哪?" 返回 4 种情况怎么处理?
- 规则 1: leader 返回 undefined epoch offset → **截到 HW** (AbstractFetcherThread.scala:L606-613) — leader 旧格式或请求 epoch 太老
- 规则 2: valid offset + undefined epoch → min(leader offset, LEO) (AbstractFetcherThread.scala:L614-619) — 老协议 (< IBP 2.0)
- 规则 3: epoch 本地未知 → 截到最大已知小 epoch 的 endOffset + **再发请求** (AbstractFetcherThread.scala:L626-638)
- 规则 4: 正常 → min(leader offset, follower epoch endOffset, LEO) (AbstractFetcherThread.scala:L639-642)
- 源码注释 AbstractFetcherThread.scala:L585-599 即规则本身 — 面试可直接引用

### 4. FENCED_LEADER_EPOCH — 被新 epoch 抛弃

场景: leader 换了 epoch, 旧 follower 还在拉?
- 处理: FENCED_LEADER_EPOCH → onPartitionFenced (AbstractFetcherThread.scala:L276-280)
- onPartitionFenced (AbstractFetcherThread.scala:L302-315): 请求 epoch == 当前 → markPartitionFailed 等新 LeaderAndIsr (AbstractFetcherThread.scala:L308); 请求 epoch 旧 → 重试 (AbstractFetcherThread.scala:L312)
- 语义: epoch 即 fencing token — 与 rd2-rlock 同源: 旧 token 的请求必须被拒

### 核心悬念
"follower 比 leader 长, 为什么必须截断而不是直接拉?" — 旧 leader 的数据可能包含未提交的孤儿数据 (分区曾经在别处领先过); 不截断就叠数据, 日志永久错位。epoch 截断 4 规则 = 用 leader 的 epoch 知识安全定位回退点 — 本质是"以 leader 为唯一真相, 但用 epoch 防过度回退"。

### 概念依赖链
Q4 两阶段 → Q5 4 规则 → Q6 fencing → (04 篇: HW) → (K-3 appendAsFollower 衔接)

### 源码锚点清单
- AbstractFetcherThread.scala:58 (类) / 115-118 (doWork 两阶段) / 120 (maybeFetch) / 174-182 (maybeTruncate) / 211-232 (truncateToEpochEndOffsets) / 215 (锁) / 224-225 (epoch 校验) / 244-260 (truncateToHighWatermark) / 276-280 (FENCED 处理) / 302-315 (onPartitionFenced) / 318-373 (processFetchRequest) / 373 (processPartitionData) / 585-599 (4 规则注释) / 604-655 (getOffsetTruncationState) / 606-613 (规则1) / 614-619 (规则2) / 626-638 (规则3) / 639-642 (规则4)
