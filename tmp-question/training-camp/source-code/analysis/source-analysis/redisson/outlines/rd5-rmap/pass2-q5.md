# 闭环笔记 q5: EvictionTask 自适应 — 清理频率的智能浮动

## 假设
后台清理不是固定频率: EvictionTask 每次清理后按"清理量历史"调 delay (5s~2h 自适应)。清理多→加快, 清理少→放慢。

## 验证过程
- EvictionTask.run (eviction/EvictionTask.java:71-113):
  - L71 每次 execute() 清理 → whenComplete(size)
  - L81-83 错误 → schedule() (错误也续排, 同 Watchdog)
  - L88-105 **三态调 delay**:
    - sizeHistory 存最近 2 次清理量
    - **连续递减** (peekFirst > peekLast > size) → `delay = min(maxDelay, delay*1.5)` (L93-95, 清理越来越少→放慢)
    - **连续相同且大** (≥keysLimit) → `delay = max(minDelay, delay/4)` (L100-102, 一直很多→加快)
    - **连续相同且 0** → `delay = min(maxDelay, delay*1.5)` (L103-105, 清完了→放慢)
  - L107-108 sizeHistory 滚动; L111 schedule() 续排
- 注释掉的快进逻辑 (L97-98): `if (sizeHistory... < size) prevDelay = max(minDelay, prevDelay/2)` — 未启用的加快分支
- 界: minDelay=5s (maxCleanUpDelay 相关) / maxDelay=2h (EvictionScheduler 注释 L27)
- 每集合一个 task (EvictionScheduler tasks map L36) — 各自独立自适应

## 代码类型
Implementation (自适应调度) — 清理频率反馈控制

## 跨域关联
- Q4 (惰性检查) → 惰性处理读面, 此任务处理后台物理清理
- RD-1 (ServiceManager.newTimeout) → 调度宿主
- 面试点: "RMapCache 过期怎么清理?多久一次?"

## 结论
后台清理 = 自适应反馈: 清理量历史 (sizeHistory 2 样本) 三态调 delay: 递减*1.5 放慢 / 持续大 /4 加快 / 清零*1.5 放慢; 界 5s~2h。设计: 用清理量作为负载信号自动调频, 闲置时省资源, 堆积时勤清理。
源码位置: EvictionTask.java:71-113