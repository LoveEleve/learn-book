# Ch14 HashedWheelTimer — 知识规划

> 来源: 4 文件 | ~1000 行 | common/src/main/java/io/netty/util/
> 基线: Ch11 HTTP 绑定网络协议 — Ch14 回答 "定时任务和超时怎么管理"

---

## 核心机制

- **时间轮算法**: wheel = HashedWheelBucket[tickPerWheel=512] — 哈希表代替优先级队列
- **100ms tick**: 默认 tickDuration, 近似 IO 超时(不需要精确)
- **remainingRounds 延迟执行**: deadline/tickDuration 计算→超出当前轮次→remainingRounds>0→每轮递减至 0 执行
- **MPSC 双队列**: timeouts(入队)+cancelledTimeouts(取消) — 非阻塞入队
- **Worker 三态**: workerState INIT→STARTED→SHUTDOWN — AtomicIntegerFieldUpdater
- **HashedWheelBucket 双向链表**: head/tail 链, expireTimeouts 遍历执行到期任务
- **transferTimeoutsToBuckets**: 每 tick 最多 100K timeout 转移→防线程饥饿
- **waitForNextTick**: System.nanoTime 精确等待 + Windows 特殊处理(#356)

## 聚类

### 时间轮结构 + 调度 (4 KPs)
1. wheel = HashedWheelBucket[512], mask = wheel.length-1, tick & mask 定位 bucket
2. remainingRounds 延迟 + deadline 精确判定
3. transferTimeoutsToBuckets: 100K 上限 + tickDuration 换算
4. waitForNextTick: nanoTime→sleep→deadline 精确对齐

### Worker + Bucket + Timeout (4 KPs)
1. Worker 三态 FSM: INIT→STARTED→SHUTDOWN + startTime 初始化
2. HashedWheelBucket: head/tail 双向链表 + addTimeout/expireTimeouts/remove
3. HashedWheelTimeout: 三态(INIT/CANCELLED/EXPIRED) + prev/next 指针自节点
4. newTimeout: start→pendingTimeouts 计数→timeouts 入队→CAS 启动 worker

### 教学顺序: 结构→调度→生命周期 (1 篇即可，🟡B)
