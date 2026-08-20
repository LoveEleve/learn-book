# J-6 并发模型与定时器 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. HashedWheelTimer 的 wheel 大小怎么定? 为什么是 2 的幂?
2. RepeatedTimer.run() 结束后做了什么? 触发中再触发会怎样?
3. MpscSingleThreadExecutor.execute 的三步是什么? shutdown 后还能提交吗?
4. LongHeldDetectingReadWriteLock 什么时候报警? 阈值在哪配?
5. SegmentList 的头删为什么 O(1)? firstOffset 怎么用?
6. RaftTimerFactory 的 sharedTimerPool 有什么用?
7. ExecutorChooser 轮询和幂等选择区别?

## 架构师视角

8. 时间轮 vs ScheduledThreadPoolExecutor: 什么时候选哪个? 精度代价?
9. "触发完成才重排" 防什么? 重叠触发会怎样 (选举定时器)?
10. MPSC 为什么是 pipeline 的顺序保证基础? 与 Disruptor 的关系?
11. 锁检测的设计哲学: 为什么只告警不干预?
12. SegmentList 的内存预算怎么支撑 LogManager 背压?
13. 分段列表 vs LinkedList vs ArrayList: 头尾操作复杂度对比?
14. 定时器工厂的共享池在多 group 部署的价值?
15. 为什么这些都该是 Hub 域? 前 5 域各用了什么?

## SRE/运维视角

16. 时间轮 tickDuration 太小会怎样? CPU 空转?
17. 锁长持告警日志长什么样? 怎么定位线程?
18. 线程池参数 (core/max/queue) 怎么调? 快照 writeExecutor?
19. 内存日志背压触发时, 写入表现? 怎么排查?

## 研究者视角

20. vs Netty HashedWheelTimer: JRaft 移植改了哪些?
21. vs 论文 "Hashed and Hierarchical Timing Wheels": 为什么不做 hierarchical?
22. vs Disruptor: MPSC 单线程 vs RingBuffer 无锁, 取舍?
23. SegmentList vs Java ArrayDeque/ConcurrentLinkedQueue: 设计动机?
24. vs ForkJoinPool 工作窃取: 为什么 JRaft 不需要?

## 学生视角

25. 什么是时间轮? 为什么叫"轮"?
26. 什么是 MPSC? 和 MPMC 区别?
27. 什么是线程组? 轮询分发?
28. 什么是分段? 为什么删除不用搬?
29. 什么是定时器? 一次性和周期性的区别?
