# 闭环笔记 q2: 双线程 — SendThread + EventThread + 三队列

## 假设
网络与业务分离; 三队列解耦。

## 验证过程
- **SendThread** (ClientCnxn:969+): 连接管理 + 收发主循环:
  - 未连接 → hostProvider.next + startConnect (L1188-1204)
  - **doTransport** (socket 收发)
  - **ping 节奏** (L1260-1274): `timeToNextPing = readTimeout/2 - idleSend - (idleSend>1000 ? 1000:0)`; **MAX_SEND_PING_INTERVAL=10000ms 上限** (L1184) — 双条件触发 (时间到 / 10s 无包)
- **EventThread** (L469+): **waitingEvents 统一队列** (事件 + 回调) — 串行处理 (Watcher.process + AsyncCallback.processResult, L533+)
- **三队列** (L148-153): **outgoingQueue (LinkedBlockingDeque — 发送)** + **pendingQueue (ArrayDeque — 等待响应)** + eventThread 队列
- **XID 预定义** (L120-127): **PING_XID=-2 / AUTHPACKET_XID=-4 / SET_WATCHES_XID=-8**; 服务端 NOTIFICATION_XID=-1 (Z-6 交叉)

## 代码类型
Implementation (双线程 + 队列)

## 跨域关联
- Z-6: 事件投递 (NOTIFICATION_XID)
- Z-4: 服务端处理器链 (对应面)

## 结论
双线程: Send (网络) vs Event (业务); 三队列 (outgoing/pending/事件); XID 特殊值区分控制包。
源码位置: ClientCnxn.java:120-127,148-153,469+,969+,1184,1260-1274
