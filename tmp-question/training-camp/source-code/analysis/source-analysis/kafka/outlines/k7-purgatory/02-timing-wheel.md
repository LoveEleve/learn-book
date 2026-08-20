# K-7 Purgatory 篇 2/2 — 时间轮: 分级计时与跨域对照

> 前置: [[K-7-purgatory-01]] (状态机+触发) | 复用: — | 对照: [[ch14-timer]] (Netty 时间轮同构) [[E-3-translog]] (等待机制) | 引出: — (K-1 Producer acks 语义交付后补链)
> 🟡 B | 来源: TimingWheel.java:97-184 + SystemTimer.java:30-61 + DelayedOperationPurgatory.java:58-70
> 定位: K-7 卷收尾 — 回答"超时怎么计时? 为什么不用 ScheduledThreadPoolExecutor?"

**读者处境**: 面试官问 "延迟操作的超时怎么实现? 为什么不用 Java 定时器?" 你答 "时间轮" — 但再问 "怎么分级? 溢出怎么办? 和 Netty 的什么关系?" 你答不上来。这篇是时间轮算法的完整答案, 收束 K-7 域。

### 1. 问题引入 — 超时也要高效

场景: 每请求一个延迟任务, broker 可能同时挂几万延迟操作 — 超时怎么计时才不成为瓶颈?
- 时间轮: O(1) 添加 + 桶批量过期 (TimingWheel.java:97)
- 本篇问题: 时间轮 (Q2) / 对照 (Q5/Q6)

### 2. 分级时间轮 — tickMs 递增的层级

场景: 时间轮内部结构?
- TimingWheel (TimingWheel.java:97): tickMs/wheelSize/interval=tickMs*wheelSize (TimingWheel.java:L98-103) + buckets[] (TimingWheel.java:L103)
- 默认参数: SystemTimer 无参构造 tickMs=1ms, wheelSize=20 (SystemTimer.java:40-42) → 层级 tickMs: 1ms→20ms (×20)→400ms (×20) — 规划断言 "1ms/20ms/400ms" 实证
- add 三分支 (TimingWheel.java:L143-175): 已过期拒绝 (TimingWheel.java:L149-151) / 本层桶 bucketId = virtualId % wheelSize (TimingWheel.java:L152-169) / **溢出 → overflowWheel 升层** (TimingWheel.java:L171-173)
- 分级创建: addOverflowWheel (TimingWheel.java:L131-141: 上层 tickMs=本层 interval) — 层间 tickMs 递增
- advanceClock (TimingWheel.java:L177-184): 推进 + overflowWheel 级联
- 驱动: SystemTimer (SystemTimer.java:58-61) + DelayQueue — 桶过期由队列头驱动, 无轮询浪费; 过期任务由单线程执行器提交 (SystemTimer.java:54,80: newFixedThreadPool(1) + taskExecutor.submit)

### 3. 与调度器对比 — 为什么时间轮

场景: ScheduledThreadPoolExecutor 不行吗?
- 时间轮: O(1) 添加 (桶内链表), 批量桶共享调度 (TimingWheel.java:143-169)
- 调度堆: O(log n) 添加, 逐任务调度
- Kafka 场景: 大量短延迟任务 (acks=all 等待 <1s) — 时间轮批量优势明显; 代价是 tick 粒度精度上限

### 4. 与 Netty 对照 — 同构时间轮

场景: Netty HashedWheelTimer 和这个一样吗?
- 同构: 都是分级时间轮 (tickMs/wheelSize/溢出) — Netty ch14-timer 已讲算法
- 差异: 驱动 (Kafka DelayQueue vs Netty worker 轮询) + 定位 (Kafka 是 Purgatory 超时兜底, 主路径是条件触发 tryComplete; Netty 是纯定时器)
- 面试记忆点: "Netty 定时器 = 纯时间轮; Kafka 时间轮 = 超时兜底, 条件触发优先"

### 核心悬念
"为什么 Kafka 和 Netty 都选时间轮?" — 共同场景: 海量短超时任务 (IO 超时/副本等待), 时间轮 O(1) 添加 + 批量过期, 对比调度堆 O(log n) 逐任务 — 同构选择是分布式系统"大量短等待"场景的最优解。

### 概念依赖链
Q2 时间轮 → Q5 对比 → Q6 Netty 对照 → (K-1 acks 语义交付后回补)

### 源码锚点清单
- TimingWheel.java:97 (类) / 98-103 (tickMs/wheelSize/interval) / 101 (DelayQueue) / 103 (buckets) / 106-108 (overflowWheel volatile) / 131-141 (addOverflowWheel) / 143-175 (add 三分支) / 149-151 (已过期) / 152-169 (本层桶) / 171-173 (溢出升层) / 177-184 (advanceClock)
- SystemTimer.java:30 (类) / 58-61 (TimingWheel 组装)
- DelayedOperationPurgatory.java:58-70 (构造: purgeInterval=1000)
- netty ch14-timer (对照)
