# 闭环笔记 q8: 阻塞语义对照 — Redisson 锁订阅 vs Redis 服务端阻塞

## 假设
Redisson 锁等待用"客户端订阅 + 信号量" (Redis PubSub 的 PUBLISH 唤醒), 与 Redis 服务端 BLPOP 等阻塞命令 (服务端队列 hold + 推送) 是两种阻塞模型。对照点: 谁等待、谁唤醒、唤醒有没有消息丢失风险。

## 验证过程
- **Redisson 锁** (RD-2): 客户端等待 → subscribe(channel) 建立订阅 → 释放者 PUBLISH 0 → LockPubSub onMessage → Semaphore.release → 客户端再抢 (tryLock 循环 L272-299). 阻塞在客户端, 唤醒靠发布订阅
- **Redis 服务端阻塞命令** (Redis R-26 blocked/list): BLPOP/BRPOP → 服务端持有连接等待, 数据就绪 push 回复. 阻塞在服务端 (连接挂起), 唤醒靠"键就绪信号" (signalKeyAsReady)
- 关键差异:
  - 挂载点: 客户端 (锁) vs 服务端 (BLPOP)
  - 唤醒源: PubSub 消息 (锁) vs 键就绪事件 (BLPOP)
  - 可靠性: 锁订阅掉线 → 永远等 (靠 latch 超时兜底); BLPOP 掉线 → 命令重发
  - 客户端连接占用: 锁等待复用订阅连接不占命令连接; BLPOP 占一条连接
- RLMq 场景 (RD-5 blocking queue): 用锁订阅做队列阻塞 (notifier), 文档在 RD-5
- Redis 锁的 channel (redisson_lock__channel) 是 PubSub, 非阻塞命令 — 所以客户端连接不被占死

## 代码类型
Glue (跨层对照) — 两种分布式阻塞语义

## 跨域关联
- Redis r26-list (阻塞) → 服务端 BLPOP 语义
- r22-expire (过期) → 锁 ttl 由服务端 PEPXPIRE 管
- RD-2 Q5 (订阅) → 客户端侧等待的实现
- 面试点: "分布式锁等待靠什么?跟 BLPOP 阻塞队列区别?"

## 结论
两种阻塞: Redisson 锁 = 客户端订阅+信号量 (阻塞在客户端, PubSub 唤醒, 复用订阅连接); Redis BLPOP = 服务端 hold 连接 (阻塞在服务端, 就绪推回, 占连接)。锁订阅掉线靠 latch 超时兜底, BLPOP 掉线靠命令重发。这是"公平 vs 连接占用"的两种取舍。
源码位置: Reflex RedissonLock.java:244-299, Redis r26 (BLPOP 服务端面)