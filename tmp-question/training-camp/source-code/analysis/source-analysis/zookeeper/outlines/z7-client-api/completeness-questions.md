# Z-7 Client API — completeness-questions (全视角提问验证)

## 开发者视角

1. 同步异步怎么组织? (异步内核 + 同步薄封装 wait)
2. SendThread 干什么? (连接 + 收发 + ping)
3. EventThread 干什么? (事件 + 回调串行)
4. 三队列? (outgoing/pending/事件)
5. XID 特殊值? (PING=-2/AUTH=-4/SET_WATCHES=-8/NOTIFICATION=-1)
6. 重连恢复什么? (会话 sessionId + watch 重注册)
7. 请求超时? (会话超时 vs 连接超时)
8. watch 注册时机? (响应成功才注册)

## 架构师视角

9. 为什么异步内核? (并发请求 + 回调; 同步是特例)
10. 双线程分离意义? (网络不阻塞业务)
11. XID 匹配语义? (FIFO 顺序响应 — 单连接串行)
12. 重连 watch 恢复? (setWatches 批量 — 服务端状态重建)
13. 连接丢失结算? (conLossPacket 错误码按状态)
14. 对照 Jedis? (ZK 有会话状态机; Jedis 无状态)
15. 为什么不重试业务? (at-most-once 语义 — 幂等交业务)
16. ping 节奏? (readTimeout/2 + 10s 上限)

## 学生视角

17. ZooKeeper 类是什么? (客户端门面)
18. 什么是回调? (异步结果处理函数)
19. 什么是 XID? (请求编号, 匹配响应)
20. 重连会丢 watch 吗? (不会 — 自动重注册)
