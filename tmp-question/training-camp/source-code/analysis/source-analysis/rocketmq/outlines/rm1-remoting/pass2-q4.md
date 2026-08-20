# 闭环笔记 q4: 客户端调用面 — invokeSync/Async/Oneway + 长连接管理

## 假设
三调用模式共享 channel 缓存; 同步超时处理分级; 长连接定时扫描。

## 验证过程
- **invokeSync** (NettyRemotingClient.java:540-585): getAndCreateChannel (长连接缓存) → 剩余时间扣减 (getChannel 耗时计入 timeout) → invokeSyncImpl (Future 信号量等待) → 成功后 updateChannelLastResponseTime
  - **发送异常 → 立即关 channel** (L556-560)
  - **超时分级** (L561-572): `left > MIN_CLOSE_TIMEOUT_MILLIS || left > timeoutMillis/4` 才关 channel — "avoid close the success channel if left timeout is small" (取 channel 耗时大时左超时小, 关闭可惜); 受 clientCloseSocketIfTimeout 配置控制
- **invokeAsync/Oneway**: invokeAsyncImpl (responseTable 预注册 + 回调) / oneway 不注册 (fire-and-forget)
- **长连接缓存** (getAndCreateChannel L631-632): channelTables ConcurrentMap<addr, ChannelWrapper> — 复用连接
- **扫描清理**: 定时扫描 channel (空闲/超时响应) — ResponseFuture 超时清理 (responseTable)
- **interruptPullRequests** (L593-606): 关 broker 连接时**中断拉取请求** (code 11=拉消息, 361=长轮询?) — 5.x 长轮询唤醒面
- **namesrv 通道**: getAndCreateNameserverChannelAsync — 无 addr 时路由发现通道 (RM-11 交叉)

## 代码类型
Implementation (客户端调用)

## 跨域关联
- RM-7/8/9 (Producer/消费): 全部走这三调用模式
- RM-11 (Namesrv): 路由发现通道
- Netty (阶段1): channel 管理

## 结论
调用 = 共享长连接缓存 + 同步超时分级关闭 (取通道耗时折减) + 异步回调 + oneway 免注册; 5.x 中断拉取唤醒面 (code 11/361)。
源码位置: NettyRemotingClient.java:540-632,593-606
