# Z-7 Client API — Pass 1 探索笔记

> 域: Z-7 Client API | 🟡 B 方案 (无 harness) | 2026-08-15
> 源码: ZooKeeper (3118) + ClientCnxn (1751) + ClientCnxnSocket (NIO/Netty 双实现) + ClientWatchManager + ZooDefs | ZooKeeper 3.9.5
> 注: Java 客户端在 zookeeper-server 模块 (09 审计修正: client 模块仅 C)

## 调用图

```
ZooKeeper.getData (同步) → getData (异步 cb) → cnxn.submitRequest
  → Packet (RequestHeader + cxid) → outgoingQueue
  → SendThread: doTransport 发送 → 服务端响应 → readResponse
      → replyHeader.xid 匹配 pendingQueue → finishPacket
          → watchRegistration.register (成功才注册)
          → cb null → notifyAll (同步返回) / cb → eventThread.queuePacket
  → EventThread: Watcher.process + AsyncCallback.processResult

连接: startConnect → primeConnection (ConnectRequest + setWatches 重注册 + auth)
  → readConnectResult → onConnected
超时: 会话 (expirationTimeout - idleRecv) / 连接 (connectTimeout)
```

## 基本元素分解

1. **门面**: 同步/异步双 API + WatchRegistration 族 + 状态机
2. **双线程**: SendThread (连接+收发+ping) + EventThread (事件+回调)
3. **连接会话**: primeConnection (重连恢复+watch 重注册) + 超时面 + SASL
4. **响应回调**: XID 匹配 + finishPacket (注册/结算) + conLossPacket

## 标记问题 (20 问)

1. 同步异步关系? (异步内核)
2. WatchRegistration 族? (4 种)
3. 状态机? (CONNECTING/CONNECTED/CLOSED)
4. SendThread 主循环? (连接+收发)
5. EventThread? (事件+回调串行)
6. 三队列? (outgoing/pending/事件)
7. XID 特殊值? (-2/-4/-8/-1)
8. primeConnection? (ConnectRequest+setWatches+auth)
9. seenRwServerBefore? (重连恢复)
10. setWatches 分批? (SET_WATCHES_MAX_LENGTH)
11. ping 节奏? (readTimeout/2 + 10s)
12. 会话超时? (expirationTimeout - idleRecv)
13. 连接超时? (connectTimeout)
14. readConnectResult? (onConnected)
15. read-only? (找 RW server)
16. finishPacket? (注册+结算)
17. conLossPacket? (错误码按状态)
18. WatchRemoved? (deregistration)
19. SASL? (认证初始化)
20. readResponse 匹配? (FIFO XID)

## 时空溯源 (代码内注释锚)

- XID 预定义注释 (L120-127): PING/AUTH/SET_WATCHES — 3.4 协议锚
- DISABLE_AUTO_WATCH_RESET (L1015): 自动 watch 重置配置
- SetWatches vs SetWatches2 (L1066-1075): 持久 watch 兼容 (3.6+)
- CONNECTEDREADONLY + pingRwTimeout 指数 (L1278-1284): read-only 模式 (3.4+)

## 大域拆分判断

Z-7 = 客户端门面 + 协议线程; 单篇 🟡 B (8 闭环 q1-q4 + 验证); socket 传输细节 (NIO/Netty) 不展开

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划) | 验证 | 结论 |
|:--|:--|:--|
| "ZooKeeper/ClientCnxn/Packet/SendThread+EventThread" | 全实证 (ZooKeeper 3118 / ClientCnxn 1751 / Packet L267+ / SendThread/EventThread) | **接受** ✅ |
| 数字: XID 特殊值 | PING=-2 / AUTH=-4 / SET_WATCHES=-8 / NOTIFICATION=-1 (服务端) | **补充** ✅ |
| 数字: ping 间隔 | readTimeout/2 + MAX_SEND_PING_INTERVAL=10s | **补充** ✅ |
| 数字: setWatches 分批 | SET_WATCHES_MAX_LENGTH | **补充** ✅ |
