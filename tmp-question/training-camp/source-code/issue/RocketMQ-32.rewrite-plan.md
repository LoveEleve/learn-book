# RocketMQ-32 重写规划

> 题目：Ack、ck、revive 为什么必须同时存在——Pop 确认与恢复闭环
> 状态：5.x 特色拆深篇。承接 RocketMQ-31，把 Pop 的 ack/ck/checkpoint/invisibility/revive/死信闭环单独拆透。

## 1. 读者困惑
- 为什么 Pop 拿到消息后，不能只靠 ack？
- ck/checkpoint 到底记录什么？
- invisible time 到底是谁在维护？
- revive 和死信是怎么从“超时未 ack”走出来的？

## 2. 一句话顿悟
**Pop 要形成可靠消费闭环，至少要同时回答四个问题：谁拿走了消息、拿走多久不可见、成功时如何显式确认、失败/超时时如何恢复。ack、ck、invisibility、revive 正是这四个问题在服务端侧的对应落点。**

## 3. 失败方案推演
- 只有 ack，没有 checkpoint → 无法恢复超时未确认消息
- 只有 invisibility，没有 revive → 消息可能永久悬挂
- 只看 ack 回执，不看服务端状态持有 → 误判 Pop 只是增强 Pull

## 4. 章节问题
- ack 关闭的到底是什么状态？
- ck/checkpoint 为什么不是多余的一层？
- ChangeInvisibleTime 在闭环里扮演什么角色？
- revive 怎样决定重投递还是死信？

## 5. 至少要排除的误解
- ack 就够了
- ck 只是一个普通时间戳
- invisible time 只是客户端本地超时概念
- revive 只是定时重试线程

## 6. 关键证据清单
- `broker/.../processor/AckMessageProcessor`
- `broker/.../processor/ChangeInvisibleTimeProcessor`
- `broker/.../processor/PopMessageProcessor`
- `broker/.../pop/PopReviveService` or equivalent
- `broker/...` checkpoint/merge related classes

## 7. 版本与实现边界
- RocketMQ 5.x 为主
- 本篇聚焦 Pop 闭环，不展开 Proxy 总览与 Pull 对照

## 8. 字数预算
- 7000~10000 字