# 闭环笔记 q1: 门面 — 同步/异步双 API + WatchRegistration

## 假设
异步为内核, 同步为薄封装; watch 注册绑定响应。

## 验证过程
- **ZooKeeper** (3118 行): 每 API 双面 — 同步版 (getData 阻塞等) 与异步版 (getData(..., cb, ctx) 带 AsyncCallback); 同步内部调异步 + wait (finishPacket notifyAll, ClientCnxn:752-760)
- **WatchRegistration 族** (ZooKeeper:265-362): **ExistsWatchRegistration / DataWatchRegistration / ChildWatchRegistration / AddWatchRegistration** — getWatches(rc) **按响应码决定注册** (成功才注册); register(int rc) (L280-285)
- **ZooKeeperState** (ZooKeeper 内部): CONNECTING/CONNECTED/CLOSED/AUTH_FAILED + CONNECTEDREADONLY — 客户端侧状态机
- **构造** (L445+): connectString (hostProvider) + sessionTimeout + watcher → ClientCnxn 创建 → SendThread/EventThread 启动

## 代码类型
Architecture (门面 + 回调)

## 跨域关联
- Z-6: watch 注册入口 (getData 带 watcher)
- Z-5: 会话协商 (ConnectRequest)

## 结论
门面 = 异步内核 + 同步薄封装; WatchRegistration 响应驱动 (失败不注册); 状态机五态。
源码位置: ZooKeeper.java:265-362,445+; ClientCnxn.java:725-761
