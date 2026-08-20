# 闭环笔记 q4: 响应与回调 — XID 匹配 + finishPacket

## 假设
FIFO XID 匹配; finishPacket 统一结算 (注册/回调/同步唤醒)。

## 验证过程
- **readResponse** (ClientCnxnSocket): replyHeader.xid → **pendingQueue 匹配** — **FIFO (服务端按序响应, 单连接串行)**
- **finishPacket** (ClientCnxn:725-761):
  1. **watchRegistration.register(err)** (L727-729) — 响应成功才注册 watch (Z-6 交叉)
  2. **watchDeregistration → WatchRemoved 事件** (L730-750): unregister → materializedWatchers → queueEvent (WatchRemoved 通知客户端)
  3. **cb null → 同步: p.notifyAll (同步 API 唤醒)** (L752-756) / **cb → eventThread.queuePacket (异步回调)** (L757-760)
- **conLossPacket** (L782-797): 连接丢失 → 挂起包全部结算 — 错误码按状态 (**AUTHFAILED/SESSIONEXPIRED/CONNECTIONLOSS**)
- **queueEvent** (L763-771): err SESSIONEXPIRED/CONNECTIONLOSS → **Disconnected 状态事件**
- **EventThread 处理** (L533+): Watcher.process + AsyncCallback.processResult 串行执行

## 代码类型
Implementation (匹配 + 结算)

## 跨域关联
- Z-6: watch 注册/移除事件
- Z-4: 服务端响应面

## 结论
响应 = FIFO XID 匹配 → finishPacket (注册/结算) → 同步唤醒或异步回调; 连接丢失全量结算 (错误码按状态)。
源码位置: ClientCnxn.java:725-797; ClientCnxnSocket.java:readResponse
