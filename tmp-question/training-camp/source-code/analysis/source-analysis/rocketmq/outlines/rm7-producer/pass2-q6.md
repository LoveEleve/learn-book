# 闭环笔记 q6: 选择器家族与异步面 — selector + SendCallback

## 假设
选择器可插拔 (hash/机房间/随机); 异步回调面。

## 验证过程
- **选择器** (selector/ 3 实现):
  - SelectMessageQueueByHash (34): hashKey % size — **顺序消息 (RM-10 交叉)**
  - SelectMessageQueueByMachineRoom (40): 同机房优先 (MachineRoomResolver)
  - SelectMessageQueueByRandom (33): 随机
- **异步面**: SendCallback (onSuccess/onException) + sendCallBackExecutor (回调线程池) — invokeAsync 的 RM-1 回调 + 发送线程池
- **事务发送**: TransactionMQProducer (155) + TransactionListener (RM-10 交叉 — 半消息)
- **Request-Reply**: RequestResponseFuture/RequestFutureHolder (5.x 请求-响应模式, 对照 RM-5 SEND_REPLY)

## 代码类型
Interface (扩展面)

## 跨域关联
- RM-10 (事务): TransactionMQProducer
- RM-1 (remoting): 回调

## 结论
选择器三实现 (hash=顺序/机房间/随机); 异步回调池; 事务/请求-响应扩展面 (RM-10/5.x)。
源码位置: selector/ 3 文件; TransactionMQProducer.java; RequestResponseFuture.java
